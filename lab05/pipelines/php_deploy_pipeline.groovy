pipeline {
    agent {
        label 'ansible-agent'
    }

    options {
        timestamps()
    }

    environment {
        PHP_REPO_URL = 'https://github.com/k1tetsssu/containers08'
        PHP_REPO_BRANCH = 'main'
        PHP_APP_DIR = 'php-app'
        REMOTE_APP_DIR = '/var/www/html/app'
    }

    stages {
        stage('Clone Repository') {
            steps {
                echo 'Cloning automation repository and PHP project repository...'
                checkout scm
                dir("${PHP_APP_DIR}") {
                    git branch: "${PHP_REPO_BRANCH}", url: "${PHP_REPO_URL}"
                }
            }
        }

        stage('Copy Project Files') {
            steps {
                echo 'Copying PHP project files to test-server via Ansible...'
                withCredentials([sshUserPrivateKey(credentialsId: 'test-server-ssh-key', keyFileVariable: 'TEST_SERVER_KEY', usernameVariable: 'SSH_USER')]) {
                    sh '''
                        set -eu
                        mkdir -p /home/jenkins/.ssh
                        cp "$TEST_SERVER_KEY" /home/jenkins/.ssh/test_server_ssh_key
                        chmod 600 /home/jenkins/.ssh/test_server_ssh_key
                        chown -R jenkins:jenkins /home/jenkins/.ssh

                        ansible test_servers -i lab05/ansible/hosts.ini -b -m ansible.builtin.file \
                            -a "path=${REMOTE_APP_DIR} state=directory owner=www-data group=www-data mode=0755"

                        ansible test_servers -i lab05/ansible/hosts.ini -b -m ansible.builtin.copy \
                            -a "src=${PHP_APP_DIR}/ dest=${REMOTE_APP_DIR}/ owner=www-data group=www-data mode=preserve"
                    '''
                }
            }
        }

        stage('Configure Application') {
            steps {
                echo 'Applying permissions and reloading Apache2 on test-server...'
                withCredentials([sshUserPrivateKey(credentialsId: 'test-server-ssh-key', keyFileVariable: 'TEST_SERVER_KEY', usernameVariable: 'SSH_USER')]) {
                    sh '''
                        set -eu
                        ansible test_servers -i lab05/ansible/hosts.ini -b -m ansible.builtin.file \
                            -a "path=${REMOTE_APP_DIR} recurse=yes owner=www-data group=www-data"

                        ansible test_servers -i lab05/ansible/hosts.ini -b -m ansible.builtin.service \
                            -a "name=apache2 state=reloaded"
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f /home/jenkins/.ssh/test_server_ssh_key'
            cleanWs()
        }
        success {
            echo 'PHP application deployed to test-server successfully!'
        }
        failure {
            echo 'Deployment to test-server failed.'
        }
    }
}