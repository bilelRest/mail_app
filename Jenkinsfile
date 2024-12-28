pipeline {
    agent any
    stages {
        stage("Build") {
            steps {
                script {
                    // Vérification si le conteneur 'mail_app0' existe avant de le supprimer
                    def containerExists = sh(script: "docker ps -a -q -f name=mail_app0", returnStdout: true).trim()
                    if (containerExists) {
                        sh "docker rm -f mail_app0"
                    } else {
                        echo "Le conteneur 'mail_app0' n'existe pas."
                    }

                    // Vérification si l'image 'mail_app' existe avant de la supprimer
                    def imageExists = sh(script: "docker images -q mail_app", returnStdout: true).trim()
                    if (imageExists) {
                        sh "docker rmi -f mail_app"
                    } else {
                        echo "L'image 'mail_app' n'existe pas."
                    }

                    // Compilation avec Maven
                    tool 'Maven 3.6.3'
                    def mvnStatus = sh(script: "mvn clean install -DskipTests", returnStatus: true)
                    if (mvnStatus != 0) {
                        error "La compilation Maven a échoué"
                    }
                }
            }
        }
        stage("Run") {
            steps {
                script {
                    // Construire l'image Docker
                    sh "docker build -t mail_app ."

                    // Démarrer un nouveau conteneur
                    sh """
                        docker run -d \
                        --name mail_app0 \
                        -p 8087:8087 \
                        mail_app
                    """
                }
            }
        }
    }
}
