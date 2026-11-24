pipeline {
    agent {
        label 'php-agent'
    }

    options {
        timestamps()
    }

    environment {
        PHP_REPO_URL = 'https://github.com/k1tetsssu/containers08.git'
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
                echo 'Preparing test database and running PHP unit tests...'
                dir("${PHP_APP_DIR}") {
                    sh '''
                        set -eu

                        if [ -f site/config.php ]; then
                            cp site/config.php .jenkins-original-config.php
                        fi

                        rm -f test_database.db

                        if [ -f site/config.php ]; then
                            printf '%s\n' \
                                '<?php' \
                                '$config = [' \
                                '    "db" => [' \
                                '        "path" => __DIR__ . "/test_database.db"' \
                                '    ]' \
                                '];' \
                                > site/config.php
                            php -l site/config.php
                        fi

                        if [ -f sql/schema.sql ]; then
                            php -r '
                                $path = __DIR__ . "/test_database.db";
                                $pdo = new PDO("sqlite:" . $path);
                                $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
                                $schema = file_get_contents(__DIR__ . "/sql/schema.sql");
                                if ($schema === false) {
                                    throw new RuntimeException("Cannot read sql/schema.sql");
                                }
                                $pdo->exec($schema);
                                echo "Test SQLite database initialized.\n";
                            '
                        fi

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
            dir("${PHP_APP_DIR}") {
                sh '''
                    if [ -f .jenkins-original-config.php ]; then
                        mv .jenkins-original-config.php site/config.php
                    fi
                    rm -f test_database.db
                '''
            }
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