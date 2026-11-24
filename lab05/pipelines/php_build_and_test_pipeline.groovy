pipeline {
    agent {
        label 'php-agent'
    }

    options {
        timestamps()
    }

    environment {
        PHP_REPO_URL = 'https://github.com/k1tetsssu/containers08'
        PHP_REPO_BRANCH = 'main'
        PHP_APP_DIR = 'php-app'
    }

    stages {
        stage('Clone Repository') {
            steps {
                echo "Cloning PHP project from ${PHP_REPO_URL} (${PHP_REPO_BRANCH})..."
                dir("${PHP_APP_DIR}") {
                    git branch: "${PHP_REPO_BRANCH}", url: "${PHP_REPO_URL}"
                }
            }
        }

        stage('Install Dependencies') {
            steps {
                echo 'Installing Composer dependencies if present...'
                dir("${PHP_APP_DIR}") {
                    sh '''
                        set -eu
                        if [ -f composer.json ]; then
                            composer install --no-interaction --prefer-dist
                        else
                            echo "No composer.json found; skipping composer install."
                        fi
                    '''
                }
            }
        }

        stage('Run Unit Tests') {
            steps {
                echo 'Running PHP unit tests...'
                dir("${PHP_APP_DIR}") {
                    sh '''
                        set -eu
                        if [ -x vendor/bin/phpunit ]; then
                            vendor/bin/phpunit
                        elif [ -f tests/tests.php ]; then
                            php tests/tests.php
                        else
                            echo "No PHPUnit or tests/tests.php found!"
                            exit 1
                        fi
                    '''
                }
            }
        }

        stage('Report Test Results') {
            steps {
                echo 'Reporting test execution status...'
                sh '''
                    set -eu
                    echo "PHP build and unit test stage finished successfully on agent: ${NODE_NAME}"
                '''
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo 'PHP build and unit tests succeeded!'
        }
        failure {
            echo 'PHP build or unit tests failed. Check console log.'
        }
    }
}