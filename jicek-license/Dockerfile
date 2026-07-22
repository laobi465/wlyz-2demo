# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 配置阿里云 Maven 镜像源（解决国内服务器下载 Maven Central 依赖慢/超时问题）
# 配置多个 mirror 提高下载成功率（主用 aliyun public，备用 aliyun central）
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <name>Aliyun Public</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
    <mirror>
      <id>aliyun-spring</id>
      <name>Aliyun Spring</name>
      <url>https://maven.aliyun.com/repository/spring</url>
      <mirrorOf>spring</mirrorOf>
    </mirror>
    <mirror>
      <id>aliyun-gradle-plugin</id>
      <name>Aliyun Gradle Plugin</name>
      <url>https://maven.aliyun.com/repository/gradle-plugin</url>
      <mirrorOf>gradle-plugin</mirrorOf>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>aliyun-profile</id>
      <repositories>
        <repository>
          <id>aliyun-public</id>
          <url>https://maven.aliyun.com/repository/public</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
        <repository>
          <id>aliyun-central</id>
          <url>https://maven.aliyun.com/repository/central</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>false</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>aliyun-profile</activeProfile>
  </activeProfiles>
</settings>
EOF

# 先复制 pom.xml 利用缓存（仅下载 pom 中声明的依赖）
COPY pom.xml .
# 注意：不用 mvn dependency:go-offline（该命令是 Maven 已知不稳定命令，
# 需要下载所有插件包括未声明的，任何小依赖失败就整体失败）
# 改为仅解析 pom 依赖，失败不致命（后续 package 会再下载）
RUN mvn dependency:resolve -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    || echo "[WARN] dependency:resolve 部分失败，将在 package 阶段重试"

# 复制源码构建
COPY src ./src
RUN mvn clean package -DskipTests -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="极策k"
LABEL description="极策k网络验证 - 卡密验证核心模块"

WORKDIR /app

# 使用阿里云 Debian 镜像源加速 apt（解决国内服务器 apt 慢/超时）
RUN sed -i 's|http://archive.ubuntu.com|https://mirrors.aliyun.com|g' /etc/apt/sources.list 2>/dev/null || true \
    && sed -i 's|http://security.ubuntu.com|https://mirrors.aliyun.com|g' /etc/apt/sources.list 2>/dev/null || true \
    && apt-get update && apt-get install -y --no-install-recommends tzdata curl \
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
