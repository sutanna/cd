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
        string(name: 'IMAGE_TAG', defaultValue: '', description: '部署的服务的镜像标签')
    }
    stages {
        stage('deploy services') {
            steps {
                script {
                    echo "deploy services with tag ${params.IMAGE_TAG}"
                    def imageTag = params.IMAGE_TAG
                    def map = [master:'test', validation: 'validation', release: 'prod']
                    def name = params.ENV_NAME ?: env.BRANCH_NAME
                    def envFolder = map[name] ?: name
                    ansiblePlaybook(playbook: "initial_deployments/deploy_all_services.yml", 
                        inventory: "initial_deployments/${envFolder}/hosts.ini",
                        hostKeyChecking: false,
                        extraVars: [landscape: envFolder, unified_image_tag: imageTag])
                }
            }
        }
    }
}
