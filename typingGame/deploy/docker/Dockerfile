# 通用服务镜像定义（8 个服务共用一份，按 SERVICE 构建不同模块）
#
# 构建前置：
#   mvn -q -DskipTests -pl user-svc -am package
# 构建镜像：
#   docker build -f deploy/docker/Dockerfile --build-arg SERVICE=user-svc -t typing-game/user-svc:local .
# 启动（推荐用编排）：
#   docker compose up -d user-svc
#
# 说明：旧版根目录 Dockerfile 复制的是不存在的单体包 typingGame-0.0.1-SNAPSHOT.jar，
# 已由本文件取代（见 docs/backend-structure-spec.md §9）。
FROM eclipse-temurin:22-jre-alpine

ARG SERVICE=user-svc

LABEL org.opencontainers.image.title="typing-game-${SERVICE}" \
      org.opencontainers.image.description="typing-game 微服务镜像（${SERVICE}）"

WORKDIR /app

RUN mkdir -p /app/uploads /app/logs

COPY ${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080-8087

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
