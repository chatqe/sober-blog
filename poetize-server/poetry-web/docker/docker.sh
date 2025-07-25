#!/bin/bash

# 设置镜像名称和标签
IMAGE_NAME="poetize:latest"
CONTAINER_NAME="poetize-container"
PORT=18081

# 1. 构建 Docker 镜像
echo "正在构建 Docker 镜像..."
docker build -t $IMAGE_NAME .

# 检查构建是否成功
if [ $? -ne 0 ]; then
  echo "Docker 镜像构建失败，请检查 Dockerfile 和 JAR 文件的配置！"
  exit 1
fi
echo "Docker 镜像构建成功！"

# 2. 检查是否有同名容器在运行
echo "检查是否有同名容器在运行..."

# 检查容器是否存在
if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
  # 如果容器在运行，则停止它
  echo "容器 $CONTAINER_NAME 正在运行，正在停止..."
  docker stop $CONTAINER_NAME

  # 删除已停止的容器
  echo "正在删除已停止的容器 $CONTAINER_NAME..."
  docker rm $CONTAINER_NAME
else
  echo "容器 $CONTAINER_NAME 没有运行，跳过停止和删除。"
fi

# 3. 运行 Docker 容器
echo "正在启动 Docker 容器..."
docker run -d --name $CONTAINER_NAME -p $PORT:$PORT $IMAGE_NAME

# 检查容器是否启动成功
if [ $? -eq 0 ]; then
  echo "Docker 容器已成功启动！"
#  echo "你可以通过 http://localhost:$PORT 访问你的 Java 应用。"
else
  echo "容器启动失败，请检查日志获取更多信息。"
  exit 1
fi
