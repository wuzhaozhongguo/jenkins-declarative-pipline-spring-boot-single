#!/bin/sh

SHELL_ACTION=${1}
APP_NAME=${2}
APP_MAIN_CLASS=${3}
SERVICE_ENV=${4}
DEBUG_PORT=${5}
SERVICE_CONFIG_SERVICE_URL=${6}


APP_DIR=$(cd `dirname $0`; pwd)

#set java home
export JAVA_HOME_PATH=/usr/local/java/1.8/

#debug opts
JAVA_OPTS_DEBUG=""
if [ "${DEBUG_PORT}" != "0" ]; then
	JAVA_OPTS_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=${DEBUG_PORT},server=y,suspend=n"
else
	echo "D'T NEED DEBUG!"
fi
echo "${APP_NAME} ${APP_MAIN_CLASS} ${APP_NAME} ${APP_MAIN_CLASS} ${DEBUG_PORT} ${SERVICE_ENV} ${SERVICE_CONFIG_SERVICE_URL}"

#debug opts
if [ -n "$SERVICE_CONFIG_SERVICE_URL" ]; then
	SERVICE_OPTS_CONFIG="-Dspring.cloud.config.profile=${SERVICE_ENV} -Dspring.cloud.config.uri=${SERVICE_CONFIG_SERVICE_URL}"
else
	echo "D'T NEED CONFIG SERVICE!"
fi

#set java home
export JAVA_HOME=${JAVA_HOME_PATH}
#jenkins config
export JENKINS_NODE_COOKIE=dontKillMe


JAVA_OPTS=" ${SERVICE_OPTS_CONFIG} ${JAVA_OPTS_DEBUG}"

JAVA_MEM_OPTS=""
BITS=`java -version 2>&1 | grep -i 64-bit`
if [ -n "$BITS" ]; then
	JAVA_MEM_OPTS=" -server -Xmx2g -Xms2g -Xmn256m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
else
	JAVA_MEM_OPTS=" -server -Xms1g -Xmx1g -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi

usage() {
    echo "Usage: sh "service".sh [start|check|deploy|kill]"
    exit 1
}



kills(){
    tpid=`cat tpid`
    if [[ $tpid ]]; then
        echo 'Kill Process!'
        kill -9 $tpid
    fi
}

start(){
    rm -f $APP_DIR/tpid
    nohup $JAVA_HOME/bin/java $JAVA_OPTS $JAVA_MEM_OPTS -cp $APP_DIR/jar/.:$APP_DIR/jar/lib/* "$APP_MAIN_CLASS" >$APP_DIR/logs/"$APP_NAME".log &
    echo $! > $APP_DIR/tpid
    var1=1
    while [ $var1 -lt 10 ]
    do
     success=`grep 'APP Start ON SpringBoot Success' $APP_DIR/logs/"$APP_NAME".log`
     if [[ $success ]]; then
       echo $APP_NAME "APP Start ON SpringBoot Success"
       break
     fi
     fail=`grep 'APP Start ON SpringBoot Failed' $APP_DIR/logs/"$APP_NAME".log`
     if [[ $fail ]]; then
       echo $APP_NAME "APP Start ON SpringBoot Failed"
       break
     fi
    done

}

check(){
    tpid=`cat tpid`
    if [[ $tpid ]]; then
        echo 'App is running.'
    else
        echo 'App is NOT running.'
    fi
}

update(){
	kills
	deploy
	start
}

#----------部署包到目录-------

deploy() {
    cd $APP_DIR/
    rm -rf $APP_DIR/app.jar
    rm -rf $APP_DIR/jar/*
    
    cp $APP_DIR/backup/app.jar.update $APP_DIR/app.jar
	unzip $APP_DIR/app.jar -d $APP_DIR/jar/

    echo "file deploy is ok"
}

case "$1" in
    "start")
        start
        ;;
    "check")
        check
        ;;
    "kill")
        kills
        ;;
    "deploy")
        deploy
        ;;
	"update")
        update
        ;;
    *)
        usage
        ;;
esac
