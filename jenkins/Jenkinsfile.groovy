//
// Required external configuration
// environment:
//  - DOCKER_REGISTRY=docker-registry:15000/
//  - JENKINS_AGENT_WORKSPACE
// credentials:
//  - dockerRegistryDeployer
//  - scm
// shared-library
//  - docker:https://github.com/SoftwareCraftsmen/jenkins-docker-shared-library@v0.2
//  - gradle:https://github.com/SoftwareCraftsmen/jenkins-gradle-shared-library@master

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

library 'docker'
library 'gradle@master'

node('docker') {
    def tag = env.BUILD_NUMBER

    stage('prepare') {
        checkout scm
        dockerVolume.createIf 'dot_m2'
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

    stage('build') {
        tag = gradleContainer([returnStdout:true],
            './gradlew --no-daemon --quiet -Prelease.scope=patch -Prelease.stage=milestone releaseVersion'
        )
        .trim()

        releaseEnv = ["APP_TAG=${tag}"]

        gradleContainer(
            './gradlew --no-daemon --quiet -Prelease.scope=patch -Prelease.stage=milestone clean asciidoctor assemble buildDockerContext')
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
                    gradleContainer(
                        './gradlew --no-daemon --quiet -Prelease.scope=patch -Prelease.stage=milestone -Dorg.ajoberstar.grgit.auth.force=hardcoded -Dorg.ajoberstar.grgit.auth.username=$GRGIT_USER -Dorg.ajoberstar.grgit.auth.password=$GRGIT_PASS --stacktrace release')
                }
            }
            junit("build/integrationTest-results/*.xml")
        }
    }

    stage('deliver') {
        dir('build/docker') {
            withEnv(dockerComposeEnv + releaseEnv) {
                withDockerRegistry([credentialsId: 'dockerRegistryDeployer',
                                    url: "http://${env.DOCKER_REGISTRY}"]) {
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

