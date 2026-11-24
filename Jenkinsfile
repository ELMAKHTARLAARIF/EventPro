pipeline {
    agent any

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))   // keep the last 20 builds
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
            steps {
                // JUnit 5 + Mockito, JaCoCo report is generated right after
                sh './gradlew test jacocoTestReport --no-daemon'
            }
        }

        stage('Coverage >= 80%') {
            steps {
                sh './gradlew jacocoTestCoverageVerification --no-daemon'
            }
        }

        stage('Package') {
            steps {
                sh './gradlew bootJar -x test --no-daemon'
            }
        }
    }

    post {
        always {
            // JUnit results (tab "Test Result" on the build page)
            junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
            // JaCoCo reports + the JAR are kept with the build
            archiveArtifacts artifacts: 'build/reports/jacoco/test/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: true, fingerprint: true
        }
        success { echo 'Pipeline OK' }
        failure { echo 'Pipeline FAILED: see the stage in red' }
    }
}