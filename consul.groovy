#! groovy

pipeline {
    agent {
        docker {
            image 'nexus-release.xsio.cn/jenkins-taskrunner:latest'
            alwaysPull true
            args "-v /root/.ssh:/root/.ssh"
        }
    }
    
    stages {
        stage('set env variables to consul') {
            steps {
                script {
                    def map = [master:'test', validation: 'validation', release: 'prod']
                    def name = params.ENV_NAME ?: env.BRANCH_NAME
                    def folder = map[name] ?: name
                    def host = folder
                    ansiblePlaybook(playbook: "consul/main.yml", inventory: "consul/hosts.ini",
                        extraVars: [host: host, folder:folder])
                }
            }
        }
    }
}
