node ('docker') {
    stage ('prepare') {
        checkout scm

        // setup volume for directory .gradle
        try {
            volume=sh(returnStdout:true, script: 'docker volume ls --filter name=dot_gradle -q').trim()
            if (!volume) {
                sh 'docker volume create --name dot_gradle'
            }
        } catch (Exception e) {
            echo e
        }
    }
    def projectDir="${env.JENKINS_AGENT_WORKSPACE}/${env.JOB_NAME}"

    stage ('commit') {
        withEnv ([ "PROJECT_DIR=/${projectDir}",
                   "COMPOSE_PROJECT_NAME=continuousdelivery" ]) {
            sh "docker-compose -f src/main/docker/docker-compose-local-test.yml run --rm build"
            junit ("build/docsTest-results/*.xml")
            junit ("build/localIntegrationTest-results/*.xml")
        }
    }

    stage ('integration') {
        withEnv ([ "PROJECT_DIR=/${projectDir}",
                   "COMPOSE_PROJECT_NAME=continuousdelivery",
                   "DOCKER_REGISTRY=nexus:5000/",
                   "SPRING_DATASOURCE_URL=jdbc:postgresql://postgresdb/app",
                   "SPRING_DATASOURCE_USERNAME=spring",
                   "SPRING_DATASOURCE_PASSWORD=boot",
                   "SPRING_JPA_GENERATE_DDL=true",
                   ]) {

            dir ('build/docker') {
                try {
                    sh "docker-compose build"

                    // dependent services are brought up by the integration test and have to be shut down afterwards
                    sh "docker-compose -f docker-compose.yml -f docker-compose-integration-test.yml run --rm integration"
                }
                catch (Exception ex) {
                    currentBuild.result = 'FAILURE'
                }
                finally {
                    // take the containers down, remove volumes
                    sh "docker-compose down -v --remove-orphans"
                }
            }
            junit ("build/integrationTest-results/*.xml")
        }
    }
    stage ('deliver') {

        withEnv ([ "PROJECT_DIR=/${projectDir}",
                   "COMPOSE_PROJECT_NAME=continuousdelivery",
                   "DOCKER_REGISTRY=nexus:5000/",
                   "DOCKER_REGISTRY_USER=admin",
                   "DOCKER_REGISTRY_PASSWORD=admin123"
                   ]) {
            sh 'docker login --username=${DOCKER_REGISTRY_USER} --password=${DOCKER_REGISTRY_PASSWORD} ${DOCKER_REGISTRY}'
            dir ('build/docker') {
                try {
                    sh "docker-compose push"
                }
                catch (Exception ex) {
                    currentBuild.result = 'FAILURE'
                }
                finally {
                    // remove images
                    sh "docker-compose down --rmi all"
                }
            }
        }
    }
}