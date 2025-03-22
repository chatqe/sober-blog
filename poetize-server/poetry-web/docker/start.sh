#!/bin/bash
APP_NAME=poetize-server.jar
LONG_NAME=qingsi.out
usage(){
    echo "Usage: sh 执行脚本.sh [start|stop|restart|status]"
    exit 1
}
is_exist(){
  pid=`ps -ef|grep $APP_NAME|grep -v grep|awk '{print $2}' `
  if [ -z "${pid}" ]; then
   return 1
  else
    return 0
  fi
}
start(){
  is_exist
  if [ $? -eq "0" ]; then
    echo "${APP_NAME} is already running. pid=${pid} ."
  else
    
  # nohup java -Xmx1024m -Xms512m -Xss1024k  -jar $APP_NAME > $LONG_NAME 2>&1 &
   # 直接启动 Java 应用，而不是后台执行
    java -Xmx1024m -Xms512m -Xss1024k -jar $APP_NAME > $LONG_NAME 2>&1
    echo ">>> start $APP_NAME successed PID=$! <<<"
	tail -f $LONG_NAME
  fi
}
stop(){
  is_exist
  if [ $? -eq "0" ]; then
    kill -9 $pid
    echo ">>> $APP_NAME process stopped <<<"
  else
    echo "${APP_NAME} is not running"
  fi
}
log(){
  is_exist
  if [ $? -eq "0" ]; then
    tail -f $LONG_NAME
  else
    echo "${APP_NAME} is not find"
  fi
}
restart(){
  stop
  start
}
case "$1" in
  "start")
    start
    ;;
  "stop")
    stop
    ;;
  "log")
    log
    ;;
  *)
    restart
    ;;
esac
exit 0
