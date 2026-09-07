pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    parameters {
        string(
            name: 'GHCR_USERNAME',
            defaultValue: 'techey-solution',
            description: 'GitHub username/organization used to login to GHCR'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: false,
            description: 'Run Maven tests before deployment'
        )
    }

    environment {
        REGISTRY = 'ghcr.io'
        IMAGE_NAME = 'ghcr.io/techey-solution/roomkh-backend'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "📦 Checking out code..."
                    cleanWs()
                    checkout scm
                    
                    // Retrieve commit hash and short SHA
                    env.GIT_COMMIT_HASH = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    env.GIT_COMMIT_SHORT = env.GIT_COMMIT_HASH.take(7)
                    echo "Commit: ${env.GIT_COMMIT_HASH} (Short: ${env.GIT_COMMIT_SHORT})"
                }
            }
        }

        stage('Test') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    echo "🧪 Running Maven tests..."
                    sh '''
                        if command -v java >/dev/null 2>&1; then
                            ./mvnw test -B
                        else
                            echo "⚠️ Java not installed on Jenkins agent host. Skipping host tests."
                        fi
                    '''
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    echo "🔨 Building and pushing Docker image to GHCR..."
                    withCredentials([
                        usernamePassword(credentialsId: 'techey-ghcr', usernameVariable: 'GHCR_CRED_USER', passwordVariable: 'GHCR_TOKEN')
                    ]) {
                        def ghcrLoginUser = params.GHCR_USERNAME?.trim() ? params.GHCR_USERNAME.trim() : GHCR_CRED_USER
                        withEnv(["GHCR_LOGIN_USER=${ghcrLoginUser}"]) {
                            sh '''
                                set -e
                                export DOCKER_BUILDKIT=1
                                
                                echo "🔐 Logging in to ${REGISTRY}..."
                                echo "${GHCR_TOKEN}" | docker login "${REGISTRY}" -u "${GHCR_LOGIN_USER}" --password-stdin
                                
                                echo "🏗️ Building Docker image..."
                                docker build \
                                    --network=host \
                                    -t "${IMAGE_NAME}:sha-${GIT_COMMIT_SHORT}" \
                                    -t "${IMAGE_NAME}:latest" \
                                    -f Dockerfile \
                                    .
                                    
                                echo "📤 Pushing Docker image..."
                                docker push "${IMAGE_NAME}:sha-${GIT_COMMIT_SHORT}"
                                docker push "${IMAGE_NAME}:latest"
                                
                                echo "🔓 Logging out..."
                                docker logout "${REGISTRY}" || true
                            '''
                        }
                    }
                }
            }
        }

        stage('Deploy with Ansible') {
            steps {
                script {
                    echo "🚀 Running Ansible deployment..."
                    
                    // Check out the techey-infra repository
                    dir('techey-infra') {
                        checkout([$class: 'GitSCM', 
                            branches: [[name: '*/main']], 
                            userRemoteConfigs: [[url: 'git@github.com:techey-solution/techey-infra.git', credentialsId: 'app-server-ssh']]
                        ])
                    }
                    
                    withCredentials([
                        sshUserPrivateKey(credentialsId: 'app-server-ssh', keyFileVariable: 'SSH_KEY_FILE', usernameVariable: 'SSH_USER'),
                        string(credentialsId: 'techey-vault-password', variable: 'ANSIBLE_VAULT_PASSWORD')
                    ]) {
                        sh '''
                            set -e
                            
                            # Verify Ansible is installed
                            if ! command -v ansible-playbook >/dev/null 2>&1; then
                                echo "⚠️ ansible-playbook not found. Attempting to install..."
                                sudo apt-get update && sudo apt-get install -y ansible || {
                                    echo "❌ Ansible installation failed. Please ensure Ansible is installed on the Jenkins agent."
                                    exit 1
                                }
                            fi
                            
                            # Create directory for keys and files
                            mkdir -p /tmp/jenkins-ansible-keys
                            chmod 700 /tmp/jenkins-ansible-keys
                            
                            # Copy private key to a temporary file with safe permissions
                            cp "${SSH_KEY_FILE}" /tmp/jenkins-ansible-keys/ansible_ssh_key
                            chmod 600 /tmp/jenkins-ansible-keys/ansible_ssh_key
                            
                            # Extract public key from the private key
                            ssh-keygen -y -f /tmp/jenkins-ansible-keys/ansible_ssh_key > /tmp/jenkins-ansible-keys/nanwork.pub
                            chmod 644 /tmp/jenkins-ansible-keys/nanwork.pub
                            
                            # Write vault password file
                            echo "${ANSIBLE_VAULT_PASSWORD}" > /tmp/jenkins-ansible-keys/vault_password
                            chmod 600 /tmp/jenkins-ansible-keys/vault_password
                            
                            # Add target server to known_hosts to prevent interactive prompt
                            mkdir -p ~/.ssh
                            chmod 700 ~/.ssh
                            
                            # Navigate to the ansible folder to load ansible.cfg and roles correctly
                            cd techey-infra/ansible
                            
                            SERVER_IP=$(awk '/roomkh_prod:/ {getline; print}' inventories/deploy/hosts.yml | grep -oP 'ansible_host:\\s*\\K[\\d.]+')
                            if [ -n "${SERVER_IP}" ]; then
                                echo "Adding ${SERVER_IP} to known_hosts..."
                                ssh-keyscan -H "${SERVER_IP}" >> ~/.ssh/known_hosts
                            fi
                            
                            # Run Ansible playbook targeting the roomkh server and setting app_name
                            export ANSIBLE_STDOUT_CALLBACK=default
                            ansible-playbook -i inventories/deploy/hosts.yml \
                                playbooks/redeploy.yml \
                                --limit roomkh \
                                --private-key=/tmp/jenkins-ansible-keys/ansible_ssh_key \
                                --vault-password-file /tmp/jenkins-ansible-keys/vault_password \
                                -e "app_version=sha-${GIT_COMMIT_SHORT}" \
                                -e "app_name=roomkh" \
                                --tags app
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧹 Cleaning up sensitive files..."
                sh '''
                    rm -f /tmp/jenkins-ansible-keys/ansible_ssh_key
                    rm -f /tmp/jenkins-ansible-keys/vault_password
                    rm -f /tmp/jenkins-ansible-keys/nanwork.pub
                    
                    if [ -f techey-infra/ansible/inventories/deploy/hosts.yml ]; then
                        SERVER_IP=$(awk '/roomkh_prod:/ {getline; print}' techey-infra/ansible/inventories/deploy/hosts.yml | grep -oP 'ansible_host:\\s*\\K[\\d.]+' || true)
                        if [ -n "${SERVER_IP}" ]; then
                            ssh-keygen -R "${SERVER_IP}" || true
                        fi
                    fi
                '''
            }
        }
    }
}
