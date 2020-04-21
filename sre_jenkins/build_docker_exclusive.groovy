  pipeline {
    parameters {
      string(name: 'branch', defaultValue: '', description: '代码分支')
      string(name: 'version', defaultValue: '', description: '服务tag')
      string(name: 'name', defaultValue: '', description: '服务名称')
      string(name: 'repo', defaultValue: '', description: '仓库地址')
      string(name: 'repo_dir', defaultValue: '', description: '仓库下项目目录')
      string(name: 'build_tool', defaultValue: 'gradle', description: 'gradle or sbt')
    }
    agent any

    stages {
      stage('Cloning Git') {
        steps {
          script {
            registry = "nexus-release.xsio.cn/${branch}"
            dockerImage = "${registry}/${name}:${version}"
            imageLatest = "${registry}/${name}"
            repo1 = repo.replace("%2F", "/")
            dir = repo1.split('/')[1]
            sh """
            rm -rf ${dir}
            git clone -b ${branch} --depth 1 git@github.com:${repo1}
            """
          }    
        }
      }
      stage('build') {
        steps{
          script {
            if (repo_dir) {
                sh """ 
                pwd
                . /root/.bashrc
                cd ${dir}/${repo_dir}
                rm -rf build
                export GRADLE_OPTS="-Dfile.encoding=utf-8"
                ./gradlew --no-daemon bootRepackage  -Dversion=${version} -Dbranch=${branch?:""} 
                """
            } else {
              if (build_tool == 'gradle') {
              sh """
              . /root/.bashrc
              cd ${dir}
              rm -rf build
              ./gradlew --no-daemon bootRepackage  -Dversion=${version} -Dbranch=${branch?:""} 
              """
            } else if (build_tool == 'sbt') {
              sh """
              cd ${dir}
              ./sbt clean "Test/compile" package
              """
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
      }
      stage('Building image') {
        steps{
          script {
            sh """
              cd ${dir}/${repo_dir}
              docker build -t ${dockerImage} . 
              docker build -t ${imageLatest} .
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
          }
        }
      }
    }
}