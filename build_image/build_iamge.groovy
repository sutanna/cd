pipeline {
    parameters {
      string(name: 'branch', defaultValue: '', description: '代码分支')
      string(name: 'version', defaultValue: '', description: '服务tag')
      string(name: 'name', defaultValue: '', description: '服务名称')
      string(name: 'repo', defaultValue: '', description: '仓库地址')
      string(name: 'repo_dir', defaultValue: '', description: '仓库下项目目录')
      string(name: 'build_tool', defaultValue: 'gradle', description: 'gradle or sbt')
      string(name: 'git_platform', defaultValue: 'github', description: 'gitlab or github')
    }
    agent any

    stages {
      stage('Cloning Git') {
        steps {
          script {
            if (repo_dir == 'null') {
                repo_dir == ""
            }
            if (build_tool == 'null') {
                build_tool = 'gradle'
            }
            registry = "nexus-release.xsio.cn/${branch}"
            dockerImage = "${registry}/${name}:${version}"
            imageLatest = "${registry}/${name}"
            acr = "registry-vpc.cn-hangzhou.aliyuncs.com/xsio"
            acrDockerImage = "${acr}/${name}:${version}"
            acrImageLatest = "${acr}/${name}:${branch}-latest"
            repo1 = repo.replace("%2F", "/")
            dir = repo1.split('/')[1]
            if (git_platform == 'gitlab') {
                sh """
                  rm -rf ${dir}
                  git clone -b ${branch} --depth 1 ssh://git@gitlab.cd.xsio.cn:22222/${repo}.git
                  cd ${dir}
                  git log
                """
              } else if (git_platform == 'github'){
                sh """
                  rm -rf ${dir}
                  git clone -b ${branch} --depth 1 git@github.com:${repo1}
                  cd ${dir}
                  git log
                """
              }
          }    
        }
      }
      stage('build') {
        steps{
          script {
            if (build_tool == 'gradle') {
                if(name=='alipay'||name=='extappendix'||name=='cpduiba'||name=='cpjinshuju') {
                    sh """
                        . /root/.bashrc
                        cd ${dir}/src/main/webapp
                        rm -rf dist
                        npm i
                        npm run build
                        cd -
                    """
                }
                if(name == 'extyouzan' ){
                    sh """
                        . /root/.bashrc
                        cd ${dir}
                        cd ./ui
                        npm i
                        npm i -f @convertlab/c-design @convertlab/uilib @convertlab/ui-common
                        npm run build
                    """
                }
                if(name == 'extwechatcorp' ){
                    sh """
                        . /root/.bashrc
                        cd ${dir}
                        rm -rf ./src/main/webapp/dist
                        cd ./src/main/webapp
                        npm install
                        npm run build
                        cd -
                    """
                }
                if(name == 'extwechatwork'){
                    sh """
                        . /root/.bashrc
                        cd ${dir}
                        cd ./src/main/webapp
                        npm install
                        npm run build
                        cd -
                    """
                }
                if(name == 'extwebinar'){
                    sh """
                        . /root/.bashrc
                        cd ${dir}/ui
                        npm install
                        NODE_ENV=production npm run build
                        cd -
            
                        cd ${dir}/ui-h5
                        npm install
                        NODE_ENV=production npm run build
                        cd -
                    """
                }
                if(name == 'extlandingpage' || name == 'extmms'|| name == 'extlandingrt' || name == 'extwenjuanxing'){
                    sh """
                        . /root/.bashrc
                        cd ${dir}
                        rm -rf ./src/main/webapp/dist
                        cd ./ui
                        npm install
                        npm run build
                        cd -
                    """
                }
                if(name in ["extbaidusem"]){
                    sh """
                        . /root/.bashrc
                        cd ${dir}/ui
                        npm install
                        npm run build
                        cd -
                    """
                }
              if (repo_dir) {
                sh """ 
                pwd
                cd ${dir}/${repo_dir}
                . /root/.bashrc
                . /root/add_gradle_build_info.sh
                rm -rf build
                export GRADLE_OPTS="-Dfile.encoding=utf-8"
                ./gradlew --no-daemon bootRepackage  -Dversion=${version} -Dbranch=${branch?:""}
                """
              } else {
                sh """
                cd ${dir}
                . /root/.bashrc
                . /root/add_gradle_build_info.sh
                rm -rf build
                export GRADLE_OPTS="-Dfile.encoding=utf-8"
                ./gradlew --no-daemon bootRepackage  -Dversion=${version} -Dbranch=${branch?:""} 
                """
              }
            sh """
                # archive war to nexus
                cd ${dir}/${repo_dir}
                # /var/jenkins_home/tools/maven/bin/mvn -B deploy:deploy-file -DgroupId=com.convertlab -DartifactId=${name} -Dversion=${version} -Dpackaging=jar -Dfile=`find build/libs -name "*.war" | head -1` -Durl=http://nexus.xsio.cn/repository/maven-archive/ -DrepositoryId=xsio-archive || true
            """
            } else if (build_tool == 'sbt') {
              if (repo_dir) {
                sh """ 
                cd ${dir}/${repo_dir}
                ./sbt clean "Test/compile" package 
                """
              } else {
                sh """
                cd ${dir}
                ./sbt clean "Test/compile" package
                """
              }
            } else if (build_tool == 'mvn') {
              sh """
              cd ${dir}
              . /root/.bashrc
              /var/jenkins_home/tools/maven/bin/mvn -V -B clean package
              """
            } 
          }
        }
      }
      stage('Building image') {
        steps{
          script {
            sh """
              cd ${dir}/${repo_dir}
              docker build -t ${dockerImage} . 
              docker tag ${dockerImage} ${imageLatest}  
            """                      
          }
        }
      }
      stage('Push Image') {
        steps{
          script {
            docker.withRegistry( 'http://nexus-release.xsio.cn', 'nexus' ) {
              sh """
                docker push ${dockerImage}
                docker push ${imageLatest}
              """
            }
            docker.withRegistry( 'https://registry-vpc.cn-hangzhou.aliyuncs.com', 'acr' ) {
              sh """
                docker tag ${dockerImage} ${acrDockerImage}
                docker push ${acrDockerImage} || echo true
                docker tag ${dockerImage} ${acrImageLatest}
                docker push ${acrImageLatest} || echo true
                docker rmi ${acrDockerImage} ${acrImageLatest} ${dockerImage} ${imageLatest}
              """
            }
          }
        }
      }
    }
}
