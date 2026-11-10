pipeline {
    agent {
        label 'ansible-agent'
    }

    options {
        timestamps()
    }

    environment {
        ANSIBLE_HOST_KEY_CHECKING = 'False'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Deploy Code') {
            steps {
                echo 'Копирование файлов проекта на Test Server...'
                
                // Используем Ansible ad-hoc команду для копирования папки site/ в /var/www/html/site/
                // Флаг -b (become) используется для выполнения от имени root (sudo) на целевом сервере
                sh '''
                    ansible test_servers -i ansible/hosts.ini -b -m copy -a "src=./site/ dest=/var/www/html/site/ owner=www-data group=www-data mode=0755"
                '''
            }
        }
    }

    post {
        success {
            echo 'Проект успешно развернут на тестовом сервере!'
        }
        failure {
            echo 'Ошибка деплоя проекта.'
        }
    }
}