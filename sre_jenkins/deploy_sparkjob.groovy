pipeline {
  parameters {
    string(name: "service_name", defaultValue: "", description: "组件名, 如: filter")
    string(name: "component_version", defaultValue: "", description: "组件版本, 如: 1.70.0")
    string(name: "deploy_env", defaultValue: "test", description: "要部署的环境")
    string(name: "hadoop_node", defaultValue: "", description: "hadoop节点")
    string(name: "args", defaultValue: "{}", description: "spark job所需要的变量")
  }
  agent any  

  stages {
    stage('build') {
      steps{
        script {
            name = service_name
            version = component_version
            mvnRepo = 'xsio-jar'
            if (version ==~ /1.0.*/) {
                mvnRepo = 'xsio-test'
            }

            sparkArgs = readJSON text: args.replace("\n", "")
        }

        // 删除本地tar包
        sh """ ssh ${hadoop_node} "rm -rf /root/.m2/repository/com/convertlab/${name}/${version}" """
        
        // 从maven下载tar包
        sh """ ssh ${hadoop_node} " source /etc/profile && mvn -q dependency:get -DgroupId=com.convertlab -Drepository=${mvnRepo} -DartifactId=${name} -Dversion=${version} -Dpackaging=tar -Dtransitive=false"
        """

        // 解压tar包
        sh """ 
           ssh ${hadoop_node} "sudo mkdir -p /opt/${name};sudo tar xvf /root/.m2/repository/com/convertlab/${name}/${version}/${name}-${version}.tar -C /opt/${name}/ && mkdir -p /opt/${name}/${name}-${version}/lib"

        """
        sh """
           ssh ${hadoop_node} "cd /opt/${name}/${name}-${version} && mv -f logback.xml spark* lib"
        """

        // 替换里面的变量
        script {
            sparkArgs.each { k, v ->
                v = v.replace('/', '\\/')
                sh """
                  ssh ${hadoop_node} "sudo sed -i 's/$k=.*/$k=$v/' /opt/${name}/${name}-${version}/lib/spark-driver.properties && sudo sed -i 's/<jar>.*<\\/jar>/<jar>${name}-${version}.jar<\\/jar>/' /opt/${name}/${name}-${version}/workflow.xml"
                """
            }
        }
        
        // 上传到HDFS上  
        sh """
            ssh ${hadoop_node} "sudo hdfs dfs -rm -r -skipTrash /user/oozie/${deploy_env}/workspace/${name}/lib/*.jar;sudo hdfs dfs -mkdir /user/oozie/${deploy_env}/workspace/${name};cd /opt/;sudo hdfs dfs -put -f /opt/${name}/${name}-${version}/* /user/oozie/${deploy_env}/workspace/${name}/"
        """  
      }
    }
  }
}

