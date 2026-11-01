
pipeline {
    agent {
        label 'php-agent'
    }

    options {
        timestamps()
    }

    stages {

        stage('Environment Check') {
            steps {
                echo 'Checking PHP environment...'

                sh '''
                    set -eu

                    php -v

                    echo "Checking PDO SQLite..."

                    php -r '
                        if (!in_array("sqlite", PDO::getAvailableDrivers())) {
                            fwrite(STDERR, "PDO SQLite is not installed!\\n");
                            exit(1);
                        }

                        echo "PDO SQLite is available.\\n";
                    '

                    echo "Environment check completed."
                '''
            }
        }

        stage('Prepare Test Database') {
            steps {
                echo 'Preparing test environment...'

                sh '''
                    set -eu

                    echo "Checking project files..."

                    test -f site/config.php
                    test -f site/modules/database.php
                    test -f sql/schema.sql
                    test -f tests/tests.php

                    echo "Saving original configuration..."

                    git show HEAD:site/config.php > .jenkins-original-config.php

                    echo "Removing old test database..."

                    rm -f test_database.db

                    echo "Creating test configuration..."

                    printf '%s\\n' \
                        '<?php' \
                        '$config = [' \
                        '    "db" => [' \
                        '        "path" => __DIR__ . "/../test_database.db"' \
                        '    ]' \
                        '];' \
                        > site/config.php

                    echo "Checking PHP syntax..."

                    php -l site/config.php

                    echo "Initializing SQLite database..."

                    php -r '
                        $path = __DIR__ . "/test_database.db";

                        $pdo = new PDO("sqlite:" . $path);
                        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

                        $schema = file_get_contents(__DIR__ . "/sql/schema.sql");

                        if ($schema === false) {
                            throw new RuntimeException("Cannot read schema.sql");
                        }

                        $pdo->exec($schema);

                        echo "Database schema imported successfully!\\n";
                    '

                    echo "Verifying database..."

                    test -f test_database.db

                    ls -lh test_database.db

                    echo "Test database is ready."
                '''
            }
        }

        stage('Install Dependencies') {
            steps {
                echo 'Checking project dependencies...'

                sh '''
                    if [ -f composer.json ]; then
                        composer install --no-interaction
                    else
                        echo "No Composer dependencies found."
                    fi
                '''
            }
        }

        stage('Run Tests') {
            steps {
                echo 'Running application tests...'

                sh '''
                    set -eu

                    echo "Checking test files..."

                    php -l tests/testframework.php
                    php -l tests/tests.php

                    echo "Executing tests..."

                    set +e
                    test_output=$(php tests/tests.php 2>&1)
                    test_status=$?
                    set -e

                    printf '%s\\n' "$test_output"

                    if [ "$test_status" -ne 0 ]; then
                        echo "PHP tests execution failed!"
                        exit "$test_status"
                    fi

                    if ! printf '%s\\n' "$test_output" | grep -Eq '7[[:space:]]*/[[:space:]]*7'; then
                        echo "Not all tests passed!"
                        exit 1
                    fi

                    echo "All 7 tests passed successfully!"
                '''
            }
        }
    }

    post {
        always {
            echo 'Cleaning up test environment...'

            sh '''
                if [ -f .jenkins-original-config.php ]; then
                    mv .jenkins-original-config.php site/config.php
                    echo "Original configuration restored."
                fi

                rm -f test_database.db
            '''

            echo 'Pipeline finished.'
        }

        success {
            echo 'All tests passed! Pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Check the logs above.'
        }
    }
}
