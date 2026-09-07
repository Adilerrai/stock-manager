pipeline {
    agent any

    stages {
        stage('Cleanup') {
            steps {
                sh '''
                    # Arrêter et supprimer l'ancien conteneur s'il existe
                    docker stop pointvente-app-api || true
                    docker rm -f pointvente-app-api || true
                '''
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean package -DskipTests=true'
            }
        }

        stage('Check Network') {
            steps {
                sh '''
                    # Vérifier si le réseau externe existe, sinon le créer
                    docker network inspect apps.prod >/dev/null 2>&1 || docker network create apps.prod

                    # Vérifier si le conteneur PostgreSQL existe
                    docker inspect pgsql.prod >/dev/null 2>&1 || echo "ATTENTION: Le conteneur PostgreSQL 'pgsql.prod' n'existe pas. Veuillez vous assurer qu'il est démarré."
                '''
            }
        }

        stage('Deploy Application') {
            steps {
                sh 'docker compose -f docker-compose.yml up -d --build web'
            }
        }
    }

    post {
        failure {
            sh 'docker compose -f docker-compose.yml down || true'
        }
    }
}
