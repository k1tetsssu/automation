pipeline {
    agent {
        label 'ansible-agent'
    }

    options {
        timestamps()
    }

    stages {
        stage('Clone Repository') {
            steps {
                echo 'Cloning automation repository with Ansible playbook...'
                checkout scm
            }
        }

        stage('Run Ansible Playbook') {
            steps {
                echo 'Running setup_test_server.yml against test-server...'
                withCredentials([sshUserPrivateKey(credentialsId: 'test-server-ssh-key', keyFileVariable: 'TEST_SERVER_KEY', usernameVariable: 'SSH_USER')]) {
                    sh '''
                        set -eu
                        mkdir -p /home/jenkins/.ssh
                        cp "$TEST_SERVER_KEY" /home/jenkins/.ssh/test_server_ssh_key
                        chmod 600 /home/jenkins/.ssh/test_server_ssh_key
                        chown -R jenkins:jenkins /home/jenkins/.ssh

                        ansible-playbook -i lab05/ansible/hosts.ini lab05/ansible/setup_test_server.yml
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
            echo 'Test server configured with Apache2 and PHP successfully!'
        }
        failure {
            echo 'Ansible test server configuration failed.'
        }
    }
}