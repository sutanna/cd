#! groovy
import org.codehaus.groovy.control.messages.ExceptionMessage

pipeline {
    agent {
        docker {
            image 'nexus.xsio.cn/jenkins-taskrunner:latest'
            alwaysPull true
            args "-v /home/uowprod/.ssh:/root/.ssh"
        }
    }
    // environment {
    //     ANSIBLE_JINJA2_NATIVE = 'true'
    // }
    parameters {
        string(name: 'service_name', defaultValue: '', description: '服务名称比如是backend还是wechatjob')
        string(name: 'component_name', defaultValue: '', description: '服务名称和镜像有关')
        string(name: 'component_version', defaultValue: '', description: '服务版本')
        string(name: 'deploy_env', defaultValue: '', description: '部署的namespace')
        string(name: 'k8s_node', defaultValue: '', description: 'ansible要连的节点')
        string(name: 'args', defaultValue: '{}', description: '服务部署时需要的参数，如cpu核数，内存大小等')
        string(name: 'notify_url',defaultValue: '', description: '通知job状态到sre的地址')
    }
    stages {
        stage('Notify Start') {
            steps{
                script {
                    def build = currentBuild

                    def targetUrl = "${notify_url}"
                    def buildUrl = build.absoluteUrl
                    def buildNumber = build.number

                    httpRequest url: targetUrl, contentType: 'APPLICATION_JSON', httpMode: 'POST', responseHandle: 'NONE', timeout: 30, requestBody: """
                    {
                        "build":{
                            parameters:{
                                "service_name":"${service_name}",
                                "component_version":"${component_version}",
                                "deploy_env":"${deploy_env}"
                            },
                            "number":"${buildNumber}",
                            "full_url":"${buildUrl}",
                            "phase": "STARTED"
                            }
                    }
                    """
                }
            }
        }
        stage('deploy services') {
            steps {
                script {
                    def build = currentBuild
                    try{
                        // echo "deploy service: ${service_name}, tag: ${component_version}"

                        def vars = readJSON text: args.replace("\n", "")
                        def extraVars = [:]
                        vars.each { k, v ->
                            if (v && v != 'null') {
                                extraVars[k] = v
                            }
                        }


                        if (!extraVars.replicas) {
                            extraVars.replicas = 1
                        }

                        extraVars.host = k8s_node
                        extraVars.namespace = deploy_env
                        extraVars.service_name = service_name
                        extraVars.service_version = component_version.substring(0, component_version.lastIndexOf('.'))
                        extraVars.image_name = component_name
                        extraVars.image_tag = component_version

                        deployService(service_name, k8s_node, extraVars)
                    }finally{

                        def targetUrl = "${notify_url}"
                        def buildUrl = build.absoluteUrl
                        def buildNumber = build.number

                        httpRequest url: targetUrl, contentType: 'APPLICATION_JSON', httpMode: 'POST', responseHandle: 'NONE', timeout: 30, requestBody: """
                        {
                            "build":{
                                parameters:{
                                    "service_name":"${service_name}",
                                    "component_version":"${component_version}",
                                    "deploy_env":"${deploy_env}"
                                },
                                "number":"${buildNumber}",
                                "full_url":"${buildUrl}",
                                "phase": "FINALIZED"
                                }
                        }
                        """
                    }
                }
            }
        }
    }
}

def deployService(service, host, extraVars) {

    ansiblePlaybook(playbook: "deployments_uowdev/playbook_int.yaml",
            inventory: host+","  ,
            extraVars: extraVars)
}