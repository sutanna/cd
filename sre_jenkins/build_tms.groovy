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
            sh "rm -rf *"
            sh "rm -rf .git"
            registry = "nexus-release.xsio.cn/${branch}"
            repo1 = repo.replace("%2F", "/")
            git branch: "${branch}", credentialsId: 'xsio', url: "git@github.com:${repo1}.git"
          }    
        }
      }
      stage('build') {
        steps{
if (name == 'tms') {
 script {
            sh """
            rm -rf ./build
            . /root/.bashrc
            npm install
            npm run build
            ./gradlew --no-daemon bootRepackage  -Dversion=${version} -Dbranch=${branch?:""} 
            """
          }

          } else {
      script{
        sh """
        . /home/jenkins/.bashrc
        cd ./src/main/webapp
        npm install
        npm run build
        cd ../../../
        rm -rf build
        export GRADLE_OPTS="-Dfile.encoding=utf-8"
        ./gradlew --no-daemon bootRepackage  -Dversion=${version} -Dbranch=${branch?:""} 
"""
          }

          }
        }
      }
      stage('Building image') {
        steps{
          script {
            dockerImage = docker.build registry + "/${name}:${version}"
            imageLatest = docker.build registry + "/${name}"           
          }
        }
      }
      stage('Push Image') {
        steps{
          script {
            docker.withRegistry( 'http://nexus-release.xsio.cn', 'nexus' ) {
            dockerImage.push()
            imageLatest.push()
            }
          }
        }
      }
    }
}
