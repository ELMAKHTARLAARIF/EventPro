pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        IMAGE_NAME = 'elmakhtarlaarif/eventpro'   // must be lowercase: <dockerhub-username>/<image>
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean assemble -x test --no-daemon'
            }
        }

        stage('Tests') {
            steps { sh './gradlew test jacocoTestReport --no-daemon' }
        }

        stage('Coverage >= 80%') {
            steps { sh './gradlew jacocoTestCoverageVerification --no-daemon' }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh './gradlew sonar --no-daemon'
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
            steps { sh './gradlew bootJar -x test --no-daemon' }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh '''
                        docker build -t $IMAGE_NAME:$IMAGE_TAG -t $IMAGE_NAME:latest .
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push $IMAGE_NAME:$IMAGE_TAG
                        docker push $IMAGE_NAME:latest
                        docker logout
                    '''
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
            archiveArtifacts artifacts: 'build/reports/jacoco/test/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: true, fingerprint: true
        }
        success {
            echo "Pipeline OK, image ${IMAGE_NAME}:${IMAGE_TAG} pushed"
        }
        failure {
            echo 'Pipeline FAILED: see the stage in red'
        }
    }
}
