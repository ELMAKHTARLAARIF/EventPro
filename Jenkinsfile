pipeline {
    agent any
    tools { jdk 'jdk17' }
    environment { IMAGE = "youruser/eventpro-api" }
    stages {
        stage('Checkout') { steps { checkout scm } }
        stage('Build')    { steps { sh 'chmod +x gradlew && ./gradlew clean assemble -x test' } }
        stage('Tests + Coverage') { steps { sh './gradlew test jacocoTestReport jacocoTestCoverageVerification' } }
        stage('SonarQube') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh './gradlew sonar -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONAR_AUTH_TOKEN'
                }
            }
        }
        stage('Quality Gate') {
            steps { timeout(time: 5, unit: 'MINUTES') { waitForQualityGate abortPipeline: true } }
        }
        stage('Package') { steps { sh './gradlew bootJar' } }
        stage('Docker') {
            steps {
                script {
                    def tag = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(7)}"
                    def img = docker.build("${IMAGE}:${tag}")
                    docker.withRegistry('', 'dockerhub-credentials') { img.push(); img.push('latest') }
                }
            }
        }
    }
    post {
        always {
            junit 'build/test-results/test/*.xml'
            publishHTML(target: [reportDir: 'build/reports/jacoco/test/html', reportFiles: 'index.html', reportName: 'JaCoCo'])
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
        }
        success { mail to: 'you@mail.com', subject: "SUCCESS ${env.JOB_NAME} #${env.BUILD_NUMBER}", body: "${env.BUILD_URL}" }
        failure { mail to: 'you@mail.com', subject: "FAILED ${env.JOB_NAME} #${env.BUILD_NUMBER}", body: "${env.BUILD_URL}" }
    }
}