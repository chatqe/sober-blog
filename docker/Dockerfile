FROM openjdk:8

# 配置环境
ENV APP_HOME /app
WORKDIR $APP_HOME

# 设置时区
ENV TZ=Asia/Shanghai \
    DEBIAN_FRONTEND=noninteractive
RUN ln -fs /usr/share/zoneinfo/${TZ} /etc/localtime && echo ${TZ} > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata && rm -rf /var/lib/apt/lists/*

# 复制项目文件
COPY docker.sh $APP_HOME
COPY poetize-server.jar $APP_HOME

# 指定需要暴露的端口,需要与 Java 服务的 HTTP 端口吻合
EXPOSE 18081

# 启动命令
CMD ["/bin/bash", "docker.sh", "start"]


