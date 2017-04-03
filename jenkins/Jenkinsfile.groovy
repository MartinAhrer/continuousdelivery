//
// Required external configuration
// environment:
//  - DOCKER_REGISTRY=docker-registry:15000/
//  - JENKINS_AGENT_WORKSPACE
// credentials:
//  - dockerRegistryDeployer
//  - scm

/**
 * Run an action that requires a Docker registry authentication.
 * @param credentialsId the credentials for the registry authentication will be pulled from the Jenkins credentials store using this credentials id.
 * @param action the registry action to run authenticated.
 */
def withDockerRegistry(String credentialsId = 'dockerRegistryDeployer', Closure action) {
    def dockerCredentials = usernamePassword(credentialsId: credentialsId, passwordVariable: 'DOCKER_REGISTRY_PASSWORD', usernameVariable: 'DOCKER_REGISTRY_USER')
    withCredentials([dockerCredentials]) {
        def registry = env.DOCKER_REGISTRY
        sh 'docker login --username=${DOCKER_REGISTRY_USER} --password=${DOCKER_REGISTRY_PASSWORD} ' + registry
        try {
            action(registry)
        }
        finally {
            sh "docker logout ${registry}"
        }
    }
}

def setupVolume() {
    def volume = sh(returnStdout: true, script: 'docker volume ls --filter name=dot_gradle -q').trim()
    if (!volume) {
        sh 'docker volume create --name dot_gradle'
    }
}

def composeFile(List items, CharSequence prefix = ' -f ') {
    def joined = ""
    for (String item : items) {
        joined <<= prefix
        joined <<= item
    }
    joined
}
/**
 * Runs a container using docker-compose.
 * @param container the container (id) of the container to run.
 * @param composeArguments a closure for building the docker-compose arguments
 * @return
 */
def runWithDockerCompose(String container, Closure composeArguments) {
    def arguments = composeArguments()
    try {
        sh(script: "docker-compose $arguments run --rm $container")
    } finally {
        sh "docker-compose $arguments down -v --remove-orphans"
    }
}

/**
 * Run simple 'one-shot' build step commands (primarily Gradle commands) inside a docker container.
 * @param map arguments
 * @param commandBuilder a closure building the command to be executed inside a container.
 * @return result of {@code sh} execution.
 */
def runWithDocker(Map map = [:], Closure commandBuilder) {
    def args = [image: 'openjdk:8-jdk', workspace: "${env.JENKINS_AGENT_WORKSPACE}/${env.JOB_NAME}", returnStdout: false] << map

    def dockerRunOptions = "--rm -v ${args.workspace}:/workspace -v dot_gradle:/root/.gradle --workdir=/workspace"
    for (Map.Entry variable : args.environment) {
        dockerRunOptions <<= " -e ${variable.key}=\"${variable.value}\""
    }
    return sh(script: "docker run ${dockerRunOptions} ${args.image} ${commandBuilder()}", returnStdout: args.returnStdout)
}


node('docker') {
    def tag = env.BUILD_NUMBER

    stage('prepare') {
        checkout scm
        setupVolume()
    }
    /**
     * As we are using DooD, we have to calculate an absolute host path for the job workspace.
     */
    final workspace = "${env.JENKINS_AGENT_WORKSPACE}/${env.JOB_NAME}"
    /**
     * We are building the compose project name in order to avoid conflicting builds with concurrent jobs or jobs for different environments.
     */
    final projectName = "${env.JOB_NAME}_${env.BUILD_NUMBER}"

    /**
     * dockerComposeEnv is used for configuring the docker-compose CLI
     */
    final dockerComposeEnv = ["PROJECT_DIR=/${workspace}",
                              "COMPOSE_PROJECT_NAME=${projectName}"]
    /**
     * applicationEnv is used for starting up the Spring boot application
     */
    final applicationEnv = ["SPRING_DATASOURCE_URL=jdbc:postgresql://postgresdb/app",
                            "SPRING_DATASOURCE_USERNAME=spring",
                            "SPRING_DATASOURCE_PASSWORD=boot",
                            "SPRING_JPA_GENERATE_DDL=true",
                            "APP_HOST=app",
                            "APP_PORT=8080"]
    def releaseEnv = []
    final gradleOptions = env.GRADLE_OPTS ? ['GRADLE_OPTS': env.GRADLE_OPTS] : [:]

    stage('build') {
        tag = runWithDocker(environment: gradleOptions, returnStdout: true) {
            './gradlew --no-daemon --quiet -Prelease.scope=patch -Prelease.stage=milestone releaseVersion'
        }
        .trim()

        releaseEnv = ["APP_TAG=${tag}"]

        runWithDocker(environment: gradleOptions) {
            './gradlew --no-daemon --quiet -Prelease.scope=patch -Prelease.stage=milestone clean asciidoctor assemble buildDockerContext'
        }
        junit("build/docsTest-results/*.xml")

        withEnv(dockerComposeEnv + releaseEnv) {
            // The task buildDockerContext has updated the docker-compose and Dockerfile scripts to reflect the built release version in the build directory.
            dir('build/docker') {
                sh "docker-compose build"
            }
        }
    }

    stage('integration') {
        withEnv(dockerComposeEnv + applicationEnv + releaseEnv) {

            dir('build/docker') {
                runWithDockerCompose('integration-test') {
                    composeFile(['docker-compose.yml', 'pipeline-integration-test.yml'])
                }

                def scmCredentials = usernamePassword(credentialsId: 'scm', usernameVariable: 'GRGIT_USER', passwordVariable: 'GRGIT_PASS')
                withCredentials([scmCredentials]) {
                    runWithDocker(environment: gradleOptions) {
                        './gradlew --no-daemon --quiet -Prelease.scope=patch -Prelease.stage=milestone -Dorg.ajoberstar.grgit.auth.force=hardcoded -Dorg.ajoberstar.grgit.auth.username=$GRGIT_USER -Dorg.ajoberstar.grgit.auth.password=$GRGIT_PASS --stacktrace release'
                    }
                }
            }
            junit("build/integrationTest-results/*.xml")
        }
    }

    stage('deliver') {
        dir('build/docker') {
            withEnv(dockerComposeEnv + releaseEnv) {
                withDockerRegistry { def registry ->
                    try {
                        sh "docker-compose push"
                    }
                    finally {
                        // Remove local docker objects (networks, containers, images, ...)
                        // This is essential as we are using the host's docker engine and would flood the host with temporary docker objects.
                        sh "docker-compose down -v --remove-orphans --rmi all"
                    }
                }
            }
        }
    }
}

