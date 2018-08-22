
def call(String SERVICE_NAME,Integer SERVICE_PORT_DEBUG,String SERVICE_MAIN_CLASS,String SERVICE_ENV,String
        SERVICE_CONFIG_SERVICE_URL,
                      String SCM_URL,String SCM_BRANCH,String BUILD_ROOT_PATH,String DEPLOY_PATH_ROOT,String
                 DEPLOY_NODE,String BUILD_MAIL,Integer SERVICE_TRACE){
//固定配置
    JENKINS_TOOLS_PATH="/usr/local/jenkins/workspace/.jenkins/tools/"//JENKINS工具目录
    JENKINS_TOOLS_SHELL_PATH='shell/'//JENKINS工具shell脚本目录
    JENKINS_TOOLS_TOOLS_PATH='tools/'//JENKINS工具 其他工具 脚本目录
    JENKINS_DEPLOY_SHELL_NAME="service.sh"//jenkins部署脚本
    JENKINS_DEPLOY_TRACE_NAME="trace.tar.gz"//调用链跟踪agent
    JENKINS_TRACE_SHELL_NAME="trace.sh"//jenkins部署调用链跟踪脚本
    SERVICE_TRACE_ENV=["dev":['collector_servers':'10.52.10.146:10800'],
                       "test":['collector_servers':'10.52.10.183:10800']]//调用链跟踪收集数据地址
    SERVICE_TRACE_AGENT_BASE_PATH="/path/to/skywalking-agent/"//服务调用链跟踪agent根路径

    //临时变量
    _SERVICE_STASH_DEPLOY_FILE_NAME='service_deploy_file_name'
    _JENKINS_STASH_TOOLS_SHELL_NAME='jenkins_shell_tools'
    _JENKINS_STASH_TOOLS_TOOLS_NAME='jenkins_tools_tools'
    _SERVICE_TRACE_AGENT_PATH=""//trace agent目录
    pipeline {
        agent none
        tools {
            jdk 'JDK8'
            maven 'maven3'
        }
        stages {
            stage('Package') {
                agent{node { label 'master' }}
                steps {
                    sh "java -version"
                    git branch: "${SCM_BRANCH}", credentialsId: 'USERNAME-WUZHAO', url: "${SCM_URL}"
                    sh "mvn clean package install -Dmaven.test.skip=true -pl ${BUILD_ROOT_PATH}/${SERVICE_NAME}/"
                    stash includes: "${BUILD_ROOT_PATH}${SERVICE_NAME}/target/${SERVICE_NAME}*.jar",
                            name:"${_SERVICE_STASH_DEPLOY_FILE_NAME}"

                    dir("${JENKINS_TOOLS_PATH}"){
                        stash includes: "${JENKINS_TOOLS_SHELL_PATH}/*", name: "${_JENKINS_STASH_TOOLS_SHELL_NAME}"
                        stash includes: "${JENKINS_TOOLS_TOOLS_PATH}/*", name: "${_JENKINS_STASH_TOOLS_TOOLS_NAME}"
                    }

                }
            }
            stage('UPLOAD') {
                agent{node { label "${DEPLOY_NODE}" }}
                steps {
                    //上传服务jar包
                    unstash "${_SERVICE_STASH_DEPLOY_FILE_NAME}"
                    //上传shell脚本
                    unstash "${_JENKINS_STASH_TOOLS_SHELL_NAME}"
                    //上传所有工具
                    unstash "${_JENKINS_STASH_TOOLS_TOOLS_NAME}"
                    script {
                        def fileExist = fileExists "${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/${JENKINS_DEPLOY_SHELL_NAME}"
                        if (!fileExist){
                            sh "cp ${JENKINS_TOOLS_SHELL_PATH}/${JENKINS_DEPLOY_SHELL_NAME} " +
                                    "${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/"
                        }
                        //获取jar包文件名
                        def SERVICE_FILE_NAME = sh(returnStdout: true, script: "ls " +
                                "${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/ | grep -E '^${SERVICE_NAME}.*.jar\$'")
                                .trim()
                        //生成服务、服务备份文件夹
                        sh "mkdir -p ${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/backup/"
                        sh "cp ${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/${SERVICE_FILE_NAME} " +
                                "${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/backup/app.jar.update"
                        //调用链跟踪
                        if (SERVICE_TRACE == 1){
                            _SERVICE_TRACE_AGENT_PATH = "${SERVICE_TRACE_AGENT_BASE_PATH}/${SERVICE_NAME}/"
                            def traceFileExist = fileExists "${SERVICE_TRACE_AGENT_BASE_PATH}/${SERVICE_NAME}/skywalking-agent.jar"
                            if (!traceFileExist){
                                sh "mkdir -p ${SERVICE_TRACE_AGENT_BASE_PATH}/${SERVICE_NAME}/"
                                sh "cp ${JENKINS_TOOLS_TOOLS_PATH}/${JENKINS_DEPLOY_TRACE_NAME} " +
                                        "${_SERVICE_TRACE_AGENT_PATH}"
                                sh "tar zxvf ${_SERVICE_TRACE_AGENT_PATH}/${JENKINS_DEPLOY_TRACE_NAME} -C" +
                                        "${_SERVICE_TRACE_AGENT_PATH}"
                                def _SERVICE_AGENT_NAME="trace-agent-${SERVICE_NAME}"
                                sh "sed -i \"s/\\(agent\\.application_code=\\).*\\\$/\\1${_SERVICE_AGENT_NAME}/\" " +
                                        "${_SERVICE_TRACE_AGENT_PATH}/config/agent.config"
                            }
                        }
                        //切换到服务目录
                        dir("${DEPLOY_PATH_ROOT}/${SERVICE_NAME}"){
                            sh "sh ${JENKINS_DEPLOY_SHELL_NAME} update ${SERVICE_NAME} ${SERVICE_MAIN_CLASS} ${SERVICE_ENV} " +
                                    "${SERVICE_PORT_DEBUG} ${SERVICE_CONFIG_SERVICE_URL} ${_SERVICE_TRACE_AGENT_PATH}"
                        }
                    }
                }
            }
        }
        post {
            success {
                emailext (
                        to: "${BUILD_MAIL}",
                        subject: "构建成功:Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                        body: "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'Check console output at ${env.BUILD_URL}'>",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                )
                echo 'success!'
            }
            failure {
                emailext (
                        to: "${BUILD_MAIL}",
                        subject: "构建失败:Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                        body: "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'Check console output at ${env.BUILD_URL}'>",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                )
                echo 'fail!'
            }
            aborted {
                emailext (
                        to: "${BUILD_MAIL}",
                        subject: "构建终止:Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                        body: "Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'Check console output at ${env.BUILD_URL}'>",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                )
                echo 'aborted!'
            }
        }
    }
}
