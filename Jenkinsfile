pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        IMAGE_NAME = 'elmakhtarlaarif/eventpro'   // must be lowercase: <dockerhub-username>/<image>
        IMAGE_TAG  = "${env.BUILD_NUMBER}"
    }

    stages {
//Download source code
        stage('Checkout') {
            steps { checkout scm }
        }
//Compile the application
        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean assemble -x test --no-daemon'
            }
        }
// Execute unit tests
        stage('Tests') {
            steps { sh './gradlew test jacocoTestReport --no-daemon' }
        }
// Verify minimum 80% coverage
        stage('Coverage >= 80%') {
            steps { sh './gradlew jacocoTestCoverageVerification --no-daemon' }
        }
//Analyze code quality
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
//Generate executable JAR
        stage('Package') {
            steps { sh './gradlew bootJar -x test --no-daemon' }
        }
//Build and upload Docker image
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
