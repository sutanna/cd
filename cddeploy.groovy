node  {
  //  def TARGETS = [env.t04] as String[]
    parameters {
        string(name: 'APPNAME', defaultValue: '', description: 'pod name ')
        string(name: 'PORT', defaultValue: '', description: 'server port')
        string(name: 'TAG', defaultValue: '', description: 'image tag')
        string(name: 'ENV', defaultValue: '', description: 'deploy env ')
        string(name: 'IMAGE_PATH', defaultValue: '', description: 'IMAGE_PATH')
    }

    def IMAGE = "${env.REGISTRY_PUBLIC}/${IMAGE_PATH}:${TAG}"

    //stage 'checkout'
    //git branch: 'master', credentialsId: 'xsio', url: 'https://github.com/xsio/cd.git'

    stage ('deploy'){
        sshStr = """
        #cp ~/ystest/cdtemplate2.yaml  deploy.yaml
	mv cdtemplate.yaml deploy.yaml	
        sed -i '/NAME/s#NAME#${APPNAME}#' deploy.yaml
        sed -i '/SPACE/s#SPACE#${ENV}#' deploy.yaml
        sed -i '/PORT/s#PORT#${PORT}#' deploy.yaml
        sed -i '/IMAGE/s#DOCKERIMAGE#${IMAGE_PATH}#' deploy.yaml
        sed -i 's#TAG#${TAG}#' deploy.yaml
         kubectl apply -f deploy.yaml
        """.trim()
        sh sshStr
    }
}
