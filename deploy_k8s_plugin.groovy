#! groovy

pipeline {
    agent {
        docker {
            image 'nexus-release.xsio.cn/jenkins-taskrunner:latest'
            alwaysPull true
            args "-v /root/.ssh:/root/.ssh"
        }
    }
    // environment {
    //     ANSIBLE_JINJA2_NATIVE = 'true'
    // }
    parameters {
        text(name: 'SERVICE_NAMES', defaultValue: '', description: '部署的服务名, 用逗号隔开')
        string(name: 'IMAGE_TAG', defaultValue: '', description: '部署的服务的镜像标签')
    }
    stages {
        stage('deploy services') {
            steps {
                script {
                    def imageTag = params.IMAGE_TAG
                    def map = [master: 'test', validation: 'validation', release: 'prod']
                    def name = params.ENV_NAME ?: env.BRANCH_NAME
                    def envFolder = map[name] ?: name

                    def services = params.SERVICE_NAMES.trim().split("\\s*,\\s*") as Set
                    def basics = ['appdbmigration', 'customerdbmigration', 'loyaltydbmigration', 'mqmigration'] as Set
                    def basicServices = []
                    def normalServices = []

                    for (service in services) {
                        if (basics.contains(service)) {
                            basicServices.add(service)
                        } else {
                            normalServices.add(service)
                        }
                    }

                    echo "basicServices: $basicServices"
                    echo "normalServices: $normalServices"

                    // deploy basic services
                    for (service in basicServices) {
                        deployService(service, envFolder, imageTag)
                    }

                    def parallelTasks = [:]
                    for (service in normalServices) {
                        def serviceName = service
                        if (env.BRANCH_NAME != 'release') {
                            deployService(serviceName, envFolder, imageTag)
                        } else {
                            // parallel deploy normal services in production
                            parallelTasks[serviceName] = {
                                stage("deploy $serviceName") {
                                    deployService(serviceName, envFolder, imageTag)
                                }
                            }
                        }
                    }

                    if (parallelTasks) {
                        parallel parallelTasks
                    }
                }
            }
        }
    }
}

def deployService(service, envFolder, imageTag) {
    echo "deploy $service"

    def confs = readYaml(file: "deployments_k8s/${envFolder}/services.yml").deploy_configs

    ansiblePlaybook(playbook: "deployments_k8s/playbook.yml",
            inventory: "deployments_k8s/hosts.ini",
            extraVars: [
                    host        : "plugin",
                    namespace   : "test",
                    folder      : "test",
                    image_tag   : imageTag,
                    service_name: service])
}
