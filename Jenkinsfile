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
    stage ('commit') {
        withEnv ([ "PROJECT_DIR=/${env.JENKINS_AGENT_WORKSPACE}/${env.JOB_NAME}",
                   "COMPOSE_PROJECT_NAME=cd.integration" ]) {
            sh "docker-compose -f src/main/docker/docker-compose-local-test.yml run --rm build"
            junit ('build/docsTest-results/*.xml')
        }
    }

    stage ('integration') {
        withEnv ([ "PROJECT_DIR=/${env.JENKINS_AGENT_WORKSPACE}/${env.JOB_NAME}",
                   "COMPOSE_PROJECT_NAME=cd.integration",
                   "SPRING_DATASOURCE_URL=jdbc:postgresql://postgresdb/app",
                   "SPRING_DATASOURCE_USERNAME=spring",
                   "SPRING_DATASOURCE_PASSWORD=boot",
                   "SPRING_JPA_GENERATE_DDL=true",
                   ]) {

            dir ('build/docker') {
                sh "docker-compose -f docker-compose.yml -f docker-compose-integration-test.yml build"

                try {
                    // dependent services are brought up by the integration test and have to be shut down afterwards
                    sh "docker-compose -f docker-compose.yml -f docker-compose-integration-test.yml run --rm integration"
                    junit ('../integrationTest-results/*.xml')
                } finally {
                    sh "docker-compose -f docker-compose.yml -f docker-compose-integration-test.yml down -v"
                }
            }
        }
    }
}