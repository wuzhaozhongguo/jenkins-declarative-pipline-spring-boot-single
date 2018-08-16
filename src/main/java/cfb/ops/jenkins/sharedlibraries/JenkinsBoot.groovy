package cfb.ops.jenkins.sharedlibraries
//SERVICE_NAME="wi-colour-invest"//服务名称
//SERVICE_PORT_DEBUG="0"//DEBUG端口 不需要设置为0
//SERVICE_MAIN_CLASS="com.caifubao.jcpt.wi.colour.invest.app.WiColourInvestApplication"//服务启动入口
//SERVICE_ENV="dev"//环境配置
//SERVICE_CONFIG_SERVICE_URL="http://localhost:8888/"//环境配置
//SCM_URL="git@10.50.10.214:jcpt/caifubao-jcpt.git"//GIT地址
//SCM_BRANCH="dev"//分支
//BUILD_ROOT_PATH="caifubao-wi/"//服务上级全目录
//DEPLOY_PATH_ROOT="/data/jcpt/www/"//服务发布路径
//DEPLOY_NODE="master"//服务发布节点
//BUILD_MAIL="liuzcmf@fansfinancial.com"//构建邮件通知

def call(String SERVICE_NAME,String SERVICE_PORT_DEBUG,String SERVICE_MAIN_CLASS,String SERVICE_ENV,
         String SERVICE_CONFIG_SERVICE_URL,
         String SCM_URL,String SCM_BRANCH,String BUILD_ROOT_PATH,String DEPLOY_PATH_ROOT,String DEPLOY_NODE,String
                 BUILD_MAIL){

//固定配置
    JENKINS_TOOLS_PATH="/usr/local/jenkins/workspace/.jenkins/tools/"//JENKINS工具目录
    JENKINS_DEPLOY_SHELL_NAME="service.sh"//jenkins部署脚本
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
                    stash includes: "${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/${SERVICE_NAME}*.jar",name:"${SERVICE_NAME}"

                    dir("${JENKINS_TOOLS_PATH}"){
                        stash includes: '*', name: 'jenkins_tools'
                    }
                }
            }
            stage('UPLOAD') {
                agent{node { label "${DEPLOY_NODE}" }}
                steps {
                    //上传服务jar包
                    unstash "${SERVICE_NAME}"
                    unstash "jenkins_tools"
                    script {
                        def fileExist = fileExists "${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/${JENKINS_DEPLOY_SHELL_NAME}"
                        if (!fileExist){
                            sh "cp ${JENKINS_DEPLOY_SHELL_NAME} ${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/"
                        }
                        //获取jar包文件名
                        def SERVICE_FILE_NAME = sh(returnStdout: true, script: "ls " +
                                "${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/ | grep -E '^${SERVICE_NAME}.*.jar\$'").trim()
                        //生成服务、服务备份文件夹
                        sh "mkdir -p ${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/backup/"
                        sh "cp ${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/${SERVICE_FILE_NAME} " +
                                "${DEPLOY_PATH_ROOT}/${SERVICE_NAME}/backup/app.jar.update"
                        //切换到服务目录
                        dir("${DEPLOY_PATH_ROOT}/${SERVICE_NAME}"){
                            sh "sh ${JENKINS_DEPLOY_SHELL_NAME} update ${SERVICE_NAME} ${SERVICE_MAIN_CLASS} ${SERVICE_ENV} " +
                                    "${SERVICE_PORT_DEBUG} ${SERVICE_CONFIG_SERVICE_URL} "
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
