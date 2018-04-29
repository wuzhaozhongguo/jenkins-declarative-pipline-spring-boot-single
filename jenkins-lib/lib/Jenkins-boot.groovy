/**参数*/
def PROJECT_NAME = 'jcpt'//项目名称
def SERVICE_NAME = 'service-account'//服务名称
def SERVICE_FOLDER_NAME = 'service-im'//发布服务文件夹名称,不配置默认使用服务名称作为服务文件夹名称
def SERVICE_ENV = 'test'//环境
def GIT_PATH = 'git@10.50.10.214:jcpt/caifubao-jcpt.git'//git地址
def USER_EMAIL = '765105646@qq.com'//邮件接收人
def NODES = ['beta_facade_mq']//跳板机(在Jenkins中配置配置的节点名称,目标服务器从跳板机拷贝文件也用的这个名称)
def TARGETS = [['infra02']]//目标服务器
//端口配置
def SHUTDOWN_PORT=8029
def HTTP_PORT=8104
def AJP_PORT=8033
def SLEEP_TIME = 0//等待服务启动时间,秒,如果只有一个服务，或者不需要等待设置为0

/**拼装的配置*/
//project
def PROJECT_PATH = "/home/jhd/web/${PROJECT_NAME}/"//项目所在目录
//service
//如果没有设置服务文件夹名称，则使用服务名称作为文件夹名称
def _service_folder = SERVICE_FOLDER_NAME.length() == 0?SERVICE_NAME:SERVICE_FOLDER_NAME
def SERVICE_PATH = "${PROJECT_PATH}${_service_folder}/"//服务目录
def CONTAINER_SH_NAME = 'tomcat.zip'//容器启动脚本名称
def CONTAINER_SH_FOLDER_NAME = 'tomcat'//启动脚本文件夹名称
//pageage
def PACKAGE_NAME = "${SERVICE_NAME}.war"//包名
def PACKAGE_FOLDER_NAME = "${SERVICE_NAME}-web"//包所在文件夹名称
//jenkins
def JENKINS_LOCAL_TOOLS_PATH='/home/jhd/.jenkins/mytools/'

def JENKINS_PATH = "${env.JENKINS_HOME}/"//跳板机Jenkins临时目录
def JENKINS_USER = 'jhd'
def JENKINS_TOOL_FOLDER_NAME='tools'//工具文件夹名称
def JENKINS_TOOLS_PATH = "${JENKINS_PATH}${JENKINS_TOOL_FOLDER_NAME}/"
def JENKINS_REMOTE_PACK_FOLDER_NAME='workspace'//node节点工作目录
//maven
def MAVEN_BIN_PATH = '/usr/local/maven/apache-maven-3.3.9/bin/'

/**配置详情*/
def _config = ['project': ['name':PROJECT_NAME,'path':PROJECT_PATH],
               'package':['name':PACKAGE_NAME,'folder_name':PACKAGE_FOLDER_NAME],
               'service': ['name':SERVICE_NAME,'path':SERVICE_PATH,"ports":["shut_down":SHUTDOWN_PORT,"http":HTTP_PORT,"ajp":AJP_PORT]],
               'node': ['nodes':NODES,'targets':TARGETS,'sleep_time':SLEEP_TIME],
               'jenkins':['path':JENKINS_PATH,'user':JENKINS_USER,'tools_path':JENKINS_TOOLS_PATH,'local_tools_folder':JENKINS_TOOL_FOLDER_NAME,
                          'local_tools_path':JENKINS_LOCAL_TOOLS_PATH,'remote_work_path':"${JENKINS_PATH}${JENKINS_REMOTE_PACK_FOLDER_NAME}/${env.JOB_NAME}"],
               'container':['container_name':CONTAINER_SH_NAME,'container_folder_name':CONTAINER_SH_FOLDER_NAME],
               'maven':['bin_path':MAVEN_BIN_PATH,'service_env':MAVEN_ENV],'user':['email':USER_EMAIL]]

def failMessage = "无错误信息"
def message = "构建成功"
try {
    /**更新代码，打包*/
    node ('master'){
        stage ('Checkout'){
            timeout(time: 60, unit: 'SECONDS') {
                git credentialsId: 'ssh', url: GIT_PATH
            }
        }
        stage('Build'){
            sh "${_config.maven.bin_path}/mvn  -Dmaven.test.skip=true clean package -P ${_config.maven.service_env} -U"
        }
        stage('Stash'){

            dir("${_config.jenkins.local_tools_path}") {
                stash includes: "${_config.container.container_name}", name:"${_config.container.container_name}"
            }
            stash includes: "${_config.package.folder_name}/target/${_config.package.name}", name:"${_config.package.name}"

        }

    }
    stage ('Publish'){

        def _package_path = "${_config.jenkins.remote_work_path}/"//jenkins工作目录

        for (def i = 0;i< _config.node.nodes.size();i++){
            def __node = _config.node.nodes[i]
            def __targets = _config.node.targets[i]
            node("${__node}") {
                stage ('Unstash'){
                    unstash "${_config.package.name}"
                }

                //拼接脚本(从跳板机下载容器)
                def __sh_target_cp_container = new StringBuffer()
                __sh_target_cp_container.append('#!/bin/bash \n')
                __sh_target_cp_container.append(' source /etc/profile;')
                __sh_target_cp_container.append(" if [ ! -f \"${_config.jenkins.tools_path}/${_config.container.container_folder_name}/bin/shutdown-f.sh\" ]; then")
                __sh_target_cp_container.append(" scp ${_config.jenkins.user}@${__node}:${_config.jenkins.tools_path}${_config.container.container_name} ${_config.jenkins.tools_path};")
                __sh_target_cp_container.append(" unzip -o ${_config.jenkins.tools_path}${_config.container.container_name} -d ${_config.jenkins.tools_path}/;")
                __sh_target_cp_container.append(' fi')

                //拼接脚本(拷贝容器到服务目录)
                def __sh_target_cp_container_to_service = new StringBuffer()
                __sh_target_cp_container_to_service.append('source /etc/profile;')
                __sh_target_cp_container_to_service.append(" if [ ! -f \"${_config.service.path}/bin/shutdown-f.sh\" ]; then")
                __sh_target_cp_container_to_service.append(" cp -r ${_config.jenkins.tools_path}${_config.container.container_folder_name} ${_config.service.path};")
                __sh_target_cp_container_to_service.append(" sed -r -i \"/<Server/{:a;/\\/>/!{N;ba};/shutdown=\\\"SHUTDOWN\\\"/s/(port=\\\")[^\\\"]*/\\1${SHUTDOWN_PORT}/}\" ${_config.service.path}/conf/server.xml;")
                __sh_target_cp_container_to_service.append(" sed -r -i \"/<Connector/{:a;/\\/>/!{N;ba};/protocol=\\\"HTTP\\/1.1\\\"/s/(port=\\\")[^\\\"]*/\\1${HTTP_PORT}/}\" ${_config.service.path}/conf/server.xml;")
                __sh_target_cp_container_to_service.append(" sed -r -i \"/<Connector/{:a;/\\/>/!{N;ba};/protocol=\\\"AJP\\/1.3\\\"/s/(port=\\\")[^\\\"]*/\\1${AJP_PORT}/}\" ${_config.service.path}/conf/server.xml;")
                __sh_target_cp_container_to_service.append(' fi;')

                //拼接脚本(拼接重启脚本)
                def __sh_target_restart_dubbo = new StringBuffer()
                __sh_target_restart_dubbo.append('source /etc/profile;')
                __sh_target_restart_dubbo.append(" rm -r ${_config.service.path}/webapps/*;")
                __sh_target_restart_dubbo.append(" cp ${_package_path}${_config.package.folder_name}/target/${_config.package.name} ${_config.service.path}webapps/;")
                __sh_target_restart_dubbo.append(" sh ${_config.service.path}/bin/shutdown-f.sh;")
                __sh_target_restart_dubbo.append(" sh ${_config.service.path}/bin/startup.sh;")

                //跳板机工具文件夹
                sh "mkdir -p ${_config.jenkins.tools_path}"//创建工具文件夹
                //判断容器存在不，如果没有就从jenkins所在机器传一份上来
                def fileExist = fileExists "${_config.jenkins.tools_path}/${_config.container.container_folder_name}/bin/shutdown-f.sh"
                if (!fileExist){
                    dir("${_config.jenkins.tools_path}/"){
                        unstash "${_config.container.container_name}"
                        sh "unzip -o ${_config.jenkins.tools_path}${_config.container.container_name} -d ${_config.jenkins.tools_path}/"
                    }
                }

                if (!__targets){//没有目标机器，发布在facade
                    echo '发布在跳板机上'

                }else{//循环发布到目标机器

                    def _target_index = 0
                    for (def __targetNode in __targets){

                    }
                }
            }
        }
    }
} catch (e) {
    message = "构建失败"
    failMessage = e
    throw e
}finally {
    stage ('Mail'){
        try {
            mail bcc: '', body: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) ${env.BUILD_URL} ${failMessage}", cc: '', from: '13991544720@139.com', replyTo: '', subject: message, to: "${_config.user.email}"
        } catch (e) {
            e.printStackTrace()
        }
    }
}