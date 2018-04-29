SERVICE_NAME="service-account"
SERVICE_VERSION="1.0.1"
SCM_URL="git@10.50.10.214:jcpt/caifubao-jcpt.git"
SCM_BRANCH="test"
BUILD_ROOT_PATH="caifubao-service/"
pipeline {
    agent any
    tools {
        jdk 'java1.8'
        maven 'maven3'
    }
    stages {
        stage('Checkout') {
            steps {
                git branch: "${SCM_BRANCH}", credentialsId: 'wuzhao', url: "${SCM_URL}"
            }
        }
        stage('Build') {
            steps {
                sh "mvn clean package install -Dmaven.test.skip=true -pl ${BUILD_ROOT_PATH}/${SERVICE_NAME}/"
                echo "${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/${SERVICE_NAME}-${SERVICE_VERSION}.jar"
                stash includes: "${BUILD_ROOT_PATH}/${SERVICE_NAME}/target/${SERVICE_NAME}-${SERVICE_VERSION}.jar", name:"${SERVICE_NAME}"
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