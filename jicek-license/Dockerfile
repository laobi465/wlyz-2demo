# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先复制 pom.xml 利用缓存
COPY pom.xml .
RUN mvn dependency:go-offline -B
# 复制源码构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="极策k"
LABEL description="极策k网络验证 - 卡密验证核心模块"

WORKDIR /app

# 安装 tzdata + curl（curl 用于 HEALTHCHECK）
RUN apt-get update && apt-get install -y --no-install-recommends tzdata curl \
    && ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# 复制构建产物（pom.xml finalName=${project.artifactId}=jicek-license，故产物为 jicek-license.jar）
COPY --from=builder /build/target/jicek-license.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 参数 + 启动
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xms256m -Xmx512m} -jar app.jar --spring.profiles.active=prod"]
