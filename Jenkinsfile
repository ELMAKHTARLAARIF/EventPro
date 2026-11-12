pipeline {
    agent any

    tools { jdk 'JDK17' }

    environment {
        IMAGE_NAME = 'yourdockerhubuser/eventpro'
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
    }

    triggers { githubPush() }  

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean compileJava'
            }
        }

        stage('Tests + Coverage') {
            steps {
                sh './gradlew test jacocoTestReport jacocoTestCoverageVerification'
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                    publishHTML(target: [
                        reportDir: 'build/reports/jacoco/test/html',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Report',
                        keepAll: true
                    ])
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh './gradlew sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Package') {
            steps {
                sh './gradlew bootJar -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                        def img = docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                        img.push()
                        img.push('latest')
                    }
                }
            }
        }
    }

    post {
        success {
            emailext subject: "✅ EventPro #${env.BUILD_NUMBER} SUCCESS",
                     body: "Build OK: ${env.BUILD_URL}", to: 'you@email.com'
        }
        failure {
            emailext subject: "❌ EventPro #${env.BUILD_NUMBER} FAILED",
                     body: "Check: ${env.BUILD_URL}", to: 'you@email.com'
        }
    }
}