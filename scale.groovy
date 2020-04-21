#! groovy

pipeline {
    agent {
        docker {
            image 'nexus-release.xsio.cn/jenkins-taskrunner:latest'
            alwaysPull true
            args "-v /root/.ssh:/root/.ssh"
        }
    }

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: '', description: '扩容/缩容的服务名')
        string(name: 'REPLICAS', defaultValue: '1', description: '实例个数')
    }

    stages {
        stage('scale service') {
            steps {
                script {
                    def service = params.SERVICE_NAME
                    def replicas = params.REPLICAS

                    def map = [master: 'test', validation: 'validation', release: 'prod']
                    def name = params.ENV_NAME ?: env.BRANCH_NAME
                    def namespace = map[name] ?: name

                    def host = namespace

                    ansiblePlaybook(playbook: "scale/playbook.yml",
                            inventory: "scale/hosts.ini",
                            extraVars: [
                                    host        : host,
                                    namespace   : namespace,
                                    replicas    : replicas,
                                    service_name: service])
                }
            }
        }
    }
}
