SERVICE_NAME="service-account"/**服务名称*/
SERVICE_VERSION="1.0.1"
SCM_URL="git@10.50.10.214:jcpt/caifubao-jcpt.git"/**GIT URL*/
SCM_BRANCH="test"/**版本分支*/
BUILD_ROOT_PATH="caifubao-service/"/**构建项目目录*/
PUBLISH_NODE_NAME="master"/**发布节点*/
PUBLISH_NODE_PATH="/data/jcpt/service/"/**发布路径*/
pipeline {
    agent none
    tools {
        jdk 'java1.8'
        maven 'maven3'
    }
        stages {
            stage('Package') {
                agent{node { label 'master' }}
                steps {
                    git branch: "${SCM_BRANCH}", credentialsId: 'wuzhao', url: "${SCM_URL}"
                    sh "mvn clean package install -Dmaven.test.skip=true -pl ${BUILD_ROOT_PATH}/${SERVICE_NAME}/"
                    stash includes: "${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/*.jar", name:"${SERVICE_NAME}"
                }
            }
            stage('UPLOAD') {
                agent{node { label "${PUBLISH_NODE_NAME}" }}
                steps {
                    unstash "${SERVICE_NAME}"
                    sh "pwd"
                }
            }
        }
    post {
        success {
            echo 'success!'
        }
        failure {
            echo 'fail!'
        }
        aborted {
            echo 'aborted!'
        }
    }
}