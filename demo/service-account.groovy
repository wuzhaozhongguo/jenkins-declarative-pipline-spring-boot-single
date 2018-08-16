SERVICE_NAME="service-account"//服务名称
SERVICE_PORT_DEBUG="0"//DEBUG端口 不需要设置为0
SERVICE_MAIN_CLASS="com.caifubao.jcpt.account.app.AccountApplication"//服务启动入口
SERVICE_ENV="dev"//环境配置
SERVICE_CONFIG_SERVICE_URL="http://localhost:8888/"//环境配置
SCM_URL="git@10.50.10.214:jcpt/caifubao-jcpt.git"//GIT地址
SCM_BRANCH="dev"//分支
BUILD_ROOT_PATH="caifubao-service/"//服务上级全目录
DEPLOY_PATH_ROOT="/data/jcpt/service/"//服务发布路径
DEPLOY_NODE="master"//服务发布节点
BUILD_MAIL="wuzmf@fansfinancial.com"//构建邮件通知

@Library('boot-deploy') _

deployBoot(SERVICE_NAME,SERVICE_PORT_DEBUG,SERVICE_MAIN_CLASS,SERVICE_ENV,
        SERVICE_CONFIG_SERVICE_URL,
        SCM_URL,SCM_BRANCH,BUILD_ROOT_PATH,DEPLOY_PATH_ROOT,DEPLOY_NODE,BUILD_MAIL)