pipeline {
  parameters {
    string(name: "name", defaultValue: "", description: "组件名, 如: filter")
    string(name: "version", defaultValue: "", description: "组件版本, 如: 1.70.0")
    string(name: "repo", defaultValue: "", description: "仓库, 如: xsio/java-projects")
    string(name: "repo_dir", defaultValue: "", description: "所在仓库子目录, 如filter")
    string(name: "branch", defaultValue: "", description: "分支版本, 如1.70")
  }
  agent any  

  stages {
    stage('Cloning Git') {
      steps {
        script {
          sh "rm -rf *"
          sh "rm -rf .git"

          git_repo = repo.replace("%2F", "/")
          sub_project = repo_dir
          git_branch = branch ?: master
        }
        git branch: "${git_branch}", credentialsId: 'xsio', url: "git@github.com:${git_repo}.git"
      }
    }
    stage('build') {
      steps{
        sh """
           rm -rf ./${sub_project}/build
           export GRADLE_OPTS="-Dfile.encoding=utf-8"
           ./gradlew ${sub_project}:publish  -Dcomponent_version=${version} -Dbranch=${git_branch}
        """
      }
    }
  }
}