//
// Required external configuration
// environment:
//  - DOCKER_REGISTRY=docker-registry:15000/
//  - JENKINS_AGENT_WORKSPACE
// credentials:
//  - docker-registry


def setupVolume() {
    def volume = sh(returnStdout: true, script: 'docker volume ls --filter name=dot_gradle -q').trim()
    if (!volume) {
        sh 'docker volume create --name dot_gradle'
    }
}

node('docker') {
    def tag = env.BUILD_NUMBER

    stage('prepare') {
        checkout scm
        setupVolume()
    }
    def projectDir = "${env.JENKINS_AGENT_WORKSPACE}/${env.JOB_NAME}"
    def projectName = "${env.JOB_NAME}_${env.BUILD_NUMBER}"

    def projectSettings = ["PROJECT_DIR=/${projectDir}",
                         "COMPOSE_PROJECT_NAME=${projectName}"]

    def applicationSettings = ["SPRING_DATASOURCE_URL=jdbc:postgresql://postgresdb/app",
                               "SPRING_DATASOURCE_USERNAME=spring",
                               "SPRING_DATASOURCE_PASSWORD=boot",
                               "SPRING_JPA_GENERATE_DDL=true",
                               "APP_HOST=app",
                               "APP_PORT=8080"]
    def releaseSettings=[]

    stage('build') {
        withEnv(projectSettings) {
            try {
                tag = sh(returnStdout: true, script: 'docker-compose -f src/main/docker/pipeline-build.yml run --rm releaseInfo').trim()
                releaseSettings=["APP_TAG=${tag}"]

                withEnv(releaseSettings) {
                    sh "docker-compose -f src/main/docker/pipeline-build.yml run --rm build"

                    junit("build/docsTest-results/*.xml")

                    dir('build/docker') {
                        sh "docker-compose build"
                    }
                }
            }
            finally {
                sh "docker-compose -f src/main/docker/pipeline-build.yml down -v"
            }
        }
    }

    stage('integration') {
        withEnv(projectSettings + applicationSettings + releaseSettings) {

            dir('build/docker') {
                try {
                    // dependent services are brought up by the integration test and have to be shut down afterwards
                    sh "docker-compose -f docker-compose.yml -f pipeline-integration-test.yml run --rm integration-test"

                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'scm',
                                      usernameVariable: 'GRGIT_USER', passwordVariable: 'GRGIT_PASS']]) {
                        sh "docker-compose -f pipeline-build.yml run --rm release"
                    }
                }
                finally {
                    // take the containers down, remove volumes
                    sh "docker-compose down -v --remove-orphans"
                }
            }
            junit("build/integrationTest-results/*.xml")
        }
    }

    stage('deliver') {
        dir('build/docker') {
            withEnv(projectSettings + releaseSettings) {
                withCredentials([usernamePassword(
                        credentialsId: 'docker-registry',
                        passwordVariable: 'DOCKER_REGISTRY_PASSWORD',
                        usernameVariable: 'DOCKER_REGISTRY_USER')]) {

                    sh 'docker login --username=${DOCKER_REGISTRY_USER} --password=${DOCKER_REGISTRY_PASSWORD} ${DOCKER_REGISTRY}'
                    try {
                        sh "docker-compose push"
                    }
                    finally {
                        // remove local docker objects (networks, containers, images, ...)
                        sh "docker-compose down -v --remove-orphans --rmi all"
                    }
                }
            }
        }
    }
}

