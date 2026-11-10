pipeline {
    agent {
        label 'ansible-agent'
    }

    options {
        timestamps()
    }

    environment {
        // Отключаем интерактивный запрос на подтверждение SSH fingerprint
        ANSIBLE_HOST_KEY_CHECKING = 'False'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Configure Test Server') {
            steps {
                echo 'Запуск Ansible Playbook для настройки сервера...'
                // Запускаем плейбук, используя инвентарь из папки ansible/
                sh 'ansible-playbook -i ansible/hosts.ini ansible/setup_test_server.yml'
            }
        }
    }

    post {
        success {
            echo 'Тестовый сервер успешно сконфигурирован!'
        }
        failure {
            echo 'Произошла ошибка при настройке тестового сервера.'
        }
    }
}