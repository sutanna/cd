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
        text(name: 'SERVICE_NAMES', defaultValue: 'all', description: '部署的服务名, 用逗号隔开')
    }

    stages {
        stage('Create All ConfigMap') {
            steps {
                script {
                    def map = [master: 'test', validation: 'validation', release: 'prod', foton: 'foton', yili: 'yili']
                    def name = params.ENV_NAME ?: env.BRANCH_NAME

                    def folder = map[name] ?: name
                    def host = folder
                    def namespace = folder

                    if (SERVICE_NAMES == 'all') {
                        ansiblePlaybook(playbook: "configmap/main.yml", inventory: "configmap/hosts.ini",
                            extraVars: [
                                    namespace: namespace,
                                    host     : host,
                                    folder   : folder])
                    } else {
                        ansiblePlaybook(playbook: 'configmap/update_service_config.yml', inventory: 'configmap/hosts.ini',
                            extraVars: [
                                namespace: namespace,
                                host     : host,
                                folder   : folder,
                                services  : SERVICE_NAMES
                            ])
                    }


                }
            }
        }
    }
}
