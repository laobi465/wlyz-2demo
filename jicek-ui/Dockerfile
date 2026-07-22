# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /build

# 配置淘宝 npm 镜像源（解决国内服务器下载 npm registry 慢/超时问题）
RUN npm config set registry https://registry.npmmirror.com

COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Runtime (Nginx)
FROM nginx:stable-alpine
LABEL maintainer="极策k"
LABEL description="极策k网络验证 - 前端 UI"

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
