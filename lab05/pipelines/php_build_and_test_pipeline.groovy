pipeline {
    agent {
        label 'php-agent'
    }

    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                // Получаем код из репозитория
                checkout scm
            }
        }

        stage('Install Dependencies') {
            steps {
                echo 'Установка зависимостей проекта...'
                sh '''
                    if [ -f composer.json ]; then
                        composer install --no-interaction --prefer-dist
                    else
                        echo "Файл composer.json не найден, пропускаем установку зависимостей."
                    fi
                '''
            }
        }

        stage('Run Unit Tests') {
            steps {
                echo 'Запуск тестов...'
                sh '''
                    set -eu
                    
                    # Если используется PHPUnit (как указано в задании)
                    if [ -f vendor/bin/phpunit ]; then
                        echo "Запуск PHPUnit..."
                        vendor/bin/phpunit --log-junit results.xml
                    
                    # Если используется кастомный фреймворк из 4-й лабораторной
                    elif [ -f tests/tests.php ]; then
                        echo "PHPUnit не найден. Запуск кастомных тестов..."
                        php tests/tests.php
                    else
                        echo "Тесты не найдены!"
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        always {
            // Если PHPUnit сгенерировал XML-отчет, Jenkins его распарсит
            junit testResults: 'results.xml', allowEmptyResults: true
            echo 'Очистка рабочего пространства...'
            cleanWs()
        }
        success {
            echo 'Сборка и тесты успешно завершены!'
        }
        failure {
            echo 'Ошибка при сборке или выполнении тестов.'
        }
    }
}