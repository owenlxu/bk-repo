FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV MODULE auth

COPY ./ /data/workspace/
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/workspace/startup.sh