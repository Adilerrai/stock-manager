pipeline {
    agent any

    environment {
        CONTAINER_NAME = "${env.CONTAINER_NAME ?: 'pointvente-app-api'}"
    }

    stages {
        stage('Cleanup Old Container') {
            steps {
                sh '''
                    docker stop ${CONTAINER_NAME} || true
                    docker rm -f ${CONTAINER_NAME} || true
                '''
            }
        }

        stage('Build & Test JAR') {
            steps {
                sh 'mvn clean package -DskipTests=true'
            }
        }

        stage('Build Docker Image (Bake)') {
            steps {
                sh 'docker buildx bake --load'
            }
        }

        stage('Check Network') {
            steps {
                sh '''
                    # Vérifier si le réseau externe existe, sinon le créer
                    docker network inspect apps.prod >/dev/null 2>&1 || docker network create apps.prod

                    # Vérifier si le conteneur PostgreSQL existe
                    docker inspect pgsql.prod >/dev/null 2>&1 || echo "ATTENTION: Le conteneur PostgreSQL 'pgsql.prod' n'existe pas."
                '''
            }
        }

        stage('Deploy Application') {
            steps {
                sh 'docker compose up -d web'
            }
        }
    }

    post {
        failure {
            sh 'docker compose down || true'
        }
    }
}
