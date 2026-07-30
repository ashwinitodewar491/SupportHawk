pipeline {
    agent any

    parameters {
        choice(
            name: 'ENV',
            choices: ['prod', 'stage'],
            description: 'Target environment'
        )
        choice(
            name: 'SUITE',
            choices: ['smoke', 'regression'],
            description: 'Test suite to run'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Run Tests') {
            steps {
                bat "mvn clean test -DtestGroups=${params.SUITE} -Denv=${params.ENV}"
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/extent-report/index.html', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            publishHTML(target: [
                allowMissing         : true,
                alwaysLinkToLastBuild: true,
                keepAll              : true,
                reportDir            : 'target/extent-report',
                reportFiles          : 'index.html',
                reportName           : "Test Report — ${params.ENV} | ${params.SUITE}"
            ])
        }

        failure {
            echo "Tests FAILED on ${params.ENV} | suite=${params.SUITE}"
        }

        unstable {
            echo "Tests UNSTABLE (failures present) on ${params.ENV} | suite=${params.SUITE}"
        }

        success {
            echo "Tests PASSED on ${params.ENV} | suite=${params.SUITE}"
        }
    }
}
