#! groovy

pipeline {
    agent {
        docker {
            image 'nexus-release.xsio.cn/jenkins-taskrunner:test'
            alwaysPull true
            args "-u jenkins:jenkins -v /opt/hudson/.ssh:/home/jenkins/.ssh -v /opt/hudson/.kube:/home/jenkins/.kube -v /tmp/k8s:/tmp"
        }
    }
    
    parameters {
        text(name: 'SERVICE_NAMES', defaultValue: '', description: 'server name')
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'image tag')
        string(name: 'ENV', defaultValue: 'test', description: 'deploy env ')
    }
    stages {
        stage('deploy services') {
            steps {
                script {
                    def imageTag = params.IMAGE_TAG
                    def name = params.ENV_NAME ?: env.BRANCH_NAME
                    def envFolder = params.ENV

                    def services = params.SERVICE_NAMES.trim().split("\\s*,\\s*") as Set
                    def normalServices = []

                    normalServices.add(service)

                    echo "normalServices: $normalServices"


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

    def confs = readYaml(file: "deployments_k8s_cd/${envFolder}/services.yml").deploy_configs

    ansiblePlaybook(playbook: "deployments_k8s_cd/playbook.yml",
            inventory: "deployments_k8s_cd/hosts.ini",
            extraVars: [
                    namespace   : envFolder,
                    folder      : envFolder,
                    image_tag   : imageTag,
                    service_name: service])
}
