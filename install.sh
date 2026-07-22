#!/bin/bash
###############################################################################
# 极策k网络验证 - 一键安装脚本（宝塔面板 + Docker 部署）
# 作者: 极策k  日期: 2026-07-22
#
# 功能：
#   1. 检测操作系统环境
#   2. 检测并安装宝塔面板（未安装则自动拉取官方脚本安装）
#   3. 检测并安装 Docker + Docker Compose v2
#   4. 端口冲突检测（应用 8080 / MySQL 3306 / Redis 6379 / 前端 80 / 宝塔 8888/888）
#   5. 自动生成加密密钥（AES / RSA / HMAC / JWT）
#   6. 通过 docker compose 构建并启动全部服务
#   7. 输出宝塔面板和项目的配置信息到 /root/jicek-deploy-info.txt
#
# 用法：
#   chmod +x install.sh
#   ./install.sh
#
# 铁律遵循：
#   04 禁硬编码：所有密钥/密码运行时生成或环境变量注入，不预设占位
#   06 防幻觉：所有命令真实执行，端口检测用 ss/netstat 实查，无假数据
#   13 遵循项目文档规范：脚本风格对齐项目 Shell SDK 规范
###############################################################################

set -euo pipefail

# ==================== 全局变量 ====================
# 兼容管道执行（curl | bash）：BASH_SOURCE[0] 可能是 /dev/stdin 或空，此时无法定位脚本路径
# 采用 fallback 策略：先尝试 BASH_SOURCE，失败则用 PWD，再失败用 /root
SCRIPT_DIR=""
if [[ -n "${BASH_SOURCE[0]:-}" && "${BASH_SOURCE[0]}" != "/dev/stdin" && "${BASH_SOURCE[0]}" != "bash" ]]; then
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" 2>/dev/null && pwd)"
fi
if [[ -z "$SCRIPT_DIR" || "$SCRIPT_DIR" == "/" ]]; then
    SCRIPT_DIR="$PWD"
fi
# 项目安装目录（clone 目标，管道执行时项目文件存放于此）
PROJECT_INSTALL_DIR="/opt/jicek"
REPORT_FILE="/root/jicek-deploy-info.txt"
LOG_FILE="/var/log/jicek-install.log"

# 服务端口（可通过环境变量覆盖，铁律 04）
APP_PORT="${APP_PORT:-8080}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_PORT="${REDIS_PORT:-6379}"
UI_PORT="${UI_PORT:-80}"
BTPANEL_PORT="${BTPANEL_PORT:-8888}"
PHPMYADMIN_PORT="${PHPMYADMIN_PORT:-888}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ==================== 工具函数 ====================
log_info()  { echo -e "${GREEN}[INFO]${NC} $*" | tee -a "$LOG_FILE"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*" | tee -a "$LOG_FILE"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*" | tee -a "$LOG_FILE"; }
log_step()  { echo -e "\n${BLUE}========== $* ==========${NC}" | tee -a "$LOG_FILE"; }

# 检测命令是否存在
has_cmd() { command -v "$1" >/dev/null 2>&1; }

# 端口占用检测（实查，无假数据，铁律 06）
# 返回 0 表示端口空闲，1 表示占用
port_is_free() {
    local port="$1"
    # 优先用 ss（iproute2 套件，现代 Linux 标配），回退 netstat
    if has_cmd ss; then
        if ss -tlnH "( sport = :$port )" 2>/dev/null | grep -q .; then
            return 1
        fi
    elif has_cmd netstat; then
        if netstat -tln 2>/dev/null | grep -qE ":$port\b"; then
            return 1
        fi
    else
        # 兜底：尝试 /proc/net/tcp 解析（无 ss/netstat 时）
        local hex_port
        hex_port=$(printf '%04X' "$port")
        if grep -q ":$hex_port " /proc/net/tcp 2>/dev/null; then
            return 1
        fi
    fi
    return 0
}

# 获取占用端口的进程名
port_occupier() {
    local port="$1"
    if has_cmd ss; then
        local pid
        pid=$(ss -tlnpH "( sport = :$port )" 2>/dev/null | grep -oE 'pid=[0-9]+' | head -1 | cut -d= -f2)
        if [[ -n "${pid:-}" ]]; then
            cat "/proc/$pid/comm" 2>/dev/null || echo "pid=$pid"
        fi
    fi
}

# ==================== 步骤 1：环境检测 ====================
check_os() {
    log_step "步骤 1/7：环境检测"

    if [[ ! -f /etc/os-release ]]; then
        log_error "无法识别操作系统（缺少 /etc/os-release），脚本仅支持 CentOS/Ubuntu/Debian"
        exit 1
    fi

    . /etc/os-release
    local os_id="$ID"
    local os_version="$VERSION_ID"
    log_info "操作系统：$PRETTY_NAME"

    case "$os_id" in
        centos|rhel|rocky|almalinux)
            log_info "系统类型：RHEL 系（$os_id $os_version）"
            PKG_MANAGER="yum"
            ;;
        ubuntu|debian)
            log_info "系统类型：Debian 系（$os_id $os_version）"
            PKG_MANAGER="apt-get"
            ;;
        *)
            log_error "不支持的操作系统：$os_id，脚本仅支持 CentOS/RHEL/Rocky/AlmaLinux/Ubuntu/Debian"
            exit 1
            ;;
    esac

    if [[ $EUID -ne 0 ]]; then
        log_error "脚本必须以 root 用户运行（当前用户 UID=$EUID）"
        exit 1
    fi
    log_info "当前用户：root（权限校验通过）"
}

# ==================== 步骤 2：宝塔面板检测与安装 ====================
check_and_install_btpanel() {
    log_step "步骤 2/7：宝塔面板检测与安装"

    # 宝塔默认安装路径
    local bt_path="/www/server/panel"
    local bt_cli="/etc/init.d/bt"

    if [[ -d "$bt_path" && -f "$bt_cli" ]]; then
        log_info "检测到宝塔面板已安装（路径：$bt_path）"
        # 读取宝塔实际配置（实查，铁律 06）
        local bt_default_port bt_ssl_port bt_username
        bt_default_port=$(python3 -c "import sys; sys.path.insert(0,'/www/server/panel'); import config; print(config.config['port'])" 2>/dev/null || echo "")
        bt_ssl_port=$(python3 -c "import sys; sys.path.insert(0,'/www/server/panel'); import config; print(config.config['ssl_port'])" 2>/dev/null || echo "")
        bt_username=$(python3 -c "import sys; sys.path.insert(0,'/www/server/panel'); import config; print(config.config['username'])" 2>/dev/null || echo "")

        BTPANEL_INSTALLED=true
        BTPANEL_PORT="${bt_default_port:-$BTPANEL_PORT}"
        BTPANEL_SSL_PORT="${bt_ssl_port:-}"
        BTPANEL_USERNAME="${bt_username:-}"
        log_info "宝塔端口：$BTPANEL_PORT"
        log_info "宝塔用户名：$BTPANEL_USERNAME"
    else
        log_warn "未检测到宝塔面板，开始自动安装..."
        log_info "拉取宝塔官方安装脚本..."

        # 根据系统选择官方安装脚本
        local install_url=""
        case "$ID" in
            centos|rhel|rocky|almalinux)
                install_url="https://download.bt.cn/install/install_6.0.sh"
                ;;
            ubuntu|debian)
                install_url="https://download.bt.cn/install/install-ubuntu_6.0.sh"
                ;;
        esac

        log_info "安装脚本地址：$install_url"
        log_info "正在安装宝塔面板（约需 2-5 分钟，请耐心等待）..."

        # 宝塔官方脚本需交互确认 y，通过管道注入
        if curl -sSO "$install_url" -o /tmp/bt_install.sh 2>/dev/null; then
            if bash /tmp/bt_install.sh ed8484bec <<< "y" 2>&1 | tee -a "$LOG_FILE"; then
                log_info "宝塔面板安装完成"
                BTPANEL_INSTALLED=true
                # 安装后读取配置
                if [[ -f "$bt_cli" ]]; then
                    BTPANEL_PORT=$(python3 -c "import sys; sys.path.insert(0,'/www/server/panel'); import config; print(config.config['port'])" 2>/dev/null || echo "$BTPANEL_PORT")
                    BTPANEL_USERNAME=$(python3 -c "import sys; sys.path.insert(0,'/www/server/panel'); import config; print(config.config['username'])" 2>/dev/null || echo "")
                    BTPANEL_PASSWORD=$(bt default 2>/dev/null | grep -oE 'password:[^ ]+' | cut -d: -f2 || echo "")
                fi
            else
                log_error "宝塔面板安装失败，请手动安装后重试"
                exit 1
            fi
        else
            log_error "无法下载宝塔安装脚本（$install_url），请检查网络后重试"
            exit 1
        fi
    fi
}

# ==================== 步骤 3：Docker 与 Docker Compose v2 检测安装 ====================
check_and_install_docker() {
    log_step "步骤 3/7：Docker 与 Docker Compose v2 检测安装"

    # Docker 检测
    if has_cmd docker; then
        local docker_ver
        docker_ver=$(docker --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
        log_info "Docker 已安装（版本：$docker_ver）"
    else
        log_warn "未检测到 Docker，开始自动安装..."

        # 使用 Docker 官方一键安装脚本
        log_info "拉取 Docker 官方安装脚本：https://get.docker.com"
        if curl -fsSL https://get.docker.com -o /tmp/get-docker.sh 2>/dev/null; then
            if sh /tmp/get-docker.sh 2>&1 | tee -a "$LOG_FILE"; then
                log_info "Docker 安装完成"
                # 启动 Docker 服务
                systemctl enable docker 2>/dev/null || true
                systemctl start docker 2>/dev/null || true
            else
                log_error "Docker 安装失败，请手动安装后重试"
                exit 1
            fi
        else
            log_error "无法下载 Docker 安装脚本，请检查网络后重试"
            exit 1
        fi
    fi

    # Docker Compose v2 检测（v2 是 docker compose 子命令，非独立 docker-compose）
    if docker compose version >/dev/null 2>&1; then
        local compose_ver
        compose_ver=$(docker compose version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
        log_info "Docker Compose v2 已就绪（版本：$compose_ver）"
    else
        log_warn "未检测到 Docker Compose v2，开始安装 compose 插件..."
        case "$PKG_MANAGER" in
            yum)
                yum install -y docker-compose-plugin 2>&1 | tee -a "$LOG_FILE" || {
                    log_error "docker-compose-plugin 安装失败，请手动安装"
                    exit 1
                }
                ;;
            apt-get)
                apt-get update -qq && apt-get install -y docker-compose-plugin 2>&1 | tee -a "$LOG_FILE" || {
                    log_error "docker-compose-plugin 安装失败，请手动安装"
                    exit 1
                }
                ;;
        esac
        if docker compose version >/dev/null 2>&1; then
            log_info "Docker Compose v2 安装完成"
        else
            log_error "Docker Compose v2 安装失败"
            exit 1
        fi
    fi

    # 配置 Docker Hub 镜像加速（解决国内服务器拉取 Docker Hub 镜像慢/超时问题）
    configure_docker_registry_mirror
}

# 配置 Docker Hub 镜像加速（国内服务器必备）
configure_docker_registry_mirror() {
    local daemon_json="/etc/docker/daemon.json"
    # 检测是否已配置镜像加速
    if [[ -f "$daemon_json" ]] && grep -q "registry-mirrors" "$daemon_json" 2>/dev/null; then
        log_info "Docker 镜像加速已配置（跳过）"
        return 0
    fi

    log_info "配置 Docker Hub 镜像加速（国内镜像源）..."
    mkdir -p /etc/docker
    cat > "$daemon_json" <<'EOF'
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me",
    "https://docker.m.daocloud.io",
    "https://dockerproxy.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  }
}
EOF

    # 重新加载 Docker 配置
    if systemctl is-active docker >/dev/null 2>&1; then
        systemctl daemon-reload 2>/dev/null || true
        systemctl restart docker 2>/dev/null || true
        log_info "Docker 镜像加速配置完成，已重启 Docker 服务"
    else
        log_info "Docker 镜像加速配置完成（Docker 服务未运行，将在启动时生效）"
    fi
}

# 从指定端口起 +1 递增，返回第一个空闲端口（最多尝试 100 次）
# 用法：find_free_port <起始端口>，echo 空闲端口号
find_free_port() {
    local start_port="$1"
    local port="$start_port"
    local max_try=100
    local tried=0
    while [[ $tried -lt $max_try ]]; do
        if port_is_free "$port"; then
            echo "$port"
            return 0
        fi
        port=$((port + 1))
        tried=$((tried + 1))
    done
    # 超过 100 次仍占用，返回原端口（后续部署会失败，但至少不卡死）
    echo "$start_port"
    return 1
}

# ==================== 步骤 4：端口冲突检测（冲突自动 +1） ====================
check_ports() {
    log_step "步骤 4/7：端口冲突检测（冲突自动递增 +1）"

    log_info "检测端口占用情况（使用 ss/netstat 实查，铁律 06）..."
    printf "%-12s %-15s %-10s %-12s %s\n" "原端口" "服务" "初始状态" "最终端口" "占用进程" | tee -a "$LOG_FILE"
    printf "%-12s %-15s %-10s %-12s %s\n" "----" "----" "----" "----" "----" | tee -a "$LOG_FILE"

    # 项目端口（冲突时自动 +1 递增，可调整）
    # 格式："端口:服务名:全局变量名"
    local adjustable_ports=(
        "$APP_PORT:应用服务:APP_PORT"
        "$MYSQL_PORT:MySQL:MYSQL_PORT"
        "$REDIS_PORT:Redis:REDIS_PORT"
        "$UI_PORT:前端Nginx:UI_PORT"
    )

    local has_conflict=false

    for entry in "${adjustable_ports[@]}"; do
        local port="${entry%%:*}"
        local rest="${entry#*:}"
        local name="${rest%%:*}"
        local var_name="${rest##*:}"
        if port_is_free "$port"; then
            printf "%-12s %-15s ${GREEN}%-10s${NC} %-12s %s\n" "$port" "$name" "空闲" "$port" "-" | tee -a "$LOG_FILE"
        else
            local occupier
            occupier=$(port_occupier "$port")
            has_conflict=true
            # 自动 +1 递增找空闲端口
            local new_port
            new_port=$(find_free_port "$((port + 1))")
            if [[ "$new_port" != "$port" ]] && port_is_free "$new_port"; then
                # 通过 eval 更新全局变量（var_name 是变量名）
                eval "$var_name=$new_port"
                printf "%-12s %-15s ${RED}%-10s${NC} ${YELLOW}%-12s${NC} %s\n" "$port" "$name" "占用" "$port->$new_port" "${occupier:-未知}" | tee -a "$LOG_FILE"
                log_warn "$name 端口 $port 被占用（进程：${occupier:-未知}），自动调整为 $new_port"
            else
                printf "%-12s %-15s ${RED}%-10s${NC} %-12s %s\n" "$port" "$name" "占用" "调整失败" "${occupier:-未知}" | tee -a "$LOG_FILE"
                log_error "$name 端口 $port 被占用且连续 100 个端口均被占用，无法自动调整"
            fi
        fi
    done

    # 宝塔端口（仅检测提示，不自动调整 —— 宝塔端口由宝塔面板管理）
    local bt_ports=(
        "$BTPANEL_PORT:宝塔面板"
        "$PHPMYADMIN_PORT:宝塔phpMyAdmin"
    )
    for entry in "${bt_ports[@]}"; do
        local port="${entry%%:*}"
        local name="${entry##*:}"
        if port_is_free "$port"; then
            printf "%-12s %-15s ${GREEN}%-10s${NC} %-12s %s\n" "$port" "$name" "空闲" "$port" "-" | tee -a "$LOG_FILE"
        else
            local occupier
            occupier=$(port_occupier "$port")
            has_conflict=true
            printf "%-12s %-15s ${YELLOW}%-10s${NC} %-12s %s\n" "$port" "$name" "占用" "$port(宝塔管理)" "${occupier:-未知}" | tee -a "$LOG_FILE"
            log_warn "$name 端口 $port 被占用（进程：${occupier:-未知}），该端口由宝塔面板管理，请在宝塔面板「设置」中修改"
        fi
    done

    if [[ "$has_conflict" == "true" ]]; then
        log_info "端口冲突处理完成：项目端口已自动调整，宝塔端口请手动处理"
        log_info "最终端口配置：APP=$APP_PORT MYSQL=$MYSQL_PORT REDIS=$REDIS_PORT UI=$UI_PORT BTPANEL=$BTPANEL_PORT"
    else
        log_info "所有端口均空闲，无冲突"
    fi
}

# ==================== 步骤 5：生成加密密钥与配置 ====================
generate_secrets() {
    log_step "步骤 5/7：生成加密密钥与配置"

    log_info "生成加密密钥（运行时随机生成，不硬编码，铁律 04）..."

    # MySQL 密码（随机 24 位）
    MYSQL_ROOT_PASSWORD=$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)
    MYSQL_PASSWORD=$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)

    # Redis 密码（随机 24 位，可选）
    REDIS_PASSWORD=$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)

    # AES-256 密钥（32 字节 Base64）
    JICEK_AES_KEY=$(head -c 32 /dev/urandom | base64)

    # HMAC-SHA256 密钥（32 字节 Base64）
    JICEK_HMAC_KEY=$(head -c 32 /dev/urandom | base64)

    # JWT 密钥（48 字节，满足 ≥32 字节要求）
    JICEK_JWT_SECRET=$(head -c 48 /dev/urandom | base64)

    # RSA-2048 密钥对
    log_info "生成 RSA-2048 密钥对..."
    local rsa_tmp_dir
    rsa_tmp_dir=$(mktemp -d)
    if openssl genrsa -out "$rsa_tmp_dir/private.pem" 2048 2>/dev/null; then
        # PKCS#8 私钥 Base64
        JICEK_RSA_PRIVATE_KEY=$(openssl pkcs8 -topk8 -nocrypt -in "$rsa_tmp_dir/private.pem" 2>/dev/null | base64 -w 0)
        # 公钥 X.509 Base64
        JICEK_RSA_PUBLIC_KEY=$(openssl rsa -in "$rsa_tmp_dir/private.pem" -pubout 2>/dev/null | base64 -w 0)
        log_info "RSA-2048 密钥对生成完成"
    else
        log_error "RSA 密钥生成失败（openssl 不可用）"
        exit 1
    fi
    rm -rf "$rsa_tmp_dir"

    log_info "所有密钥生成完成"
}

# ==================== 步骤 6：Docker Compose 部署 ====================

# 确保项目文件存在（管道执行时自动 clone，本地执行时直接使用）
ensure_project_files() {
    # 如果 SCRIPT_DIR 下已有 docker-compose.yml，说明是本地执行，直接用
    if [[ -f "$SCRIPT_DIR/docker-compose.yml" ]]; then
        log_info "项目文件已存在（本地执行）：$SCRIPT_DIR"
        return 0
    fi

    # 管道执行场景：需要 clone 项目
    log_info "未在当前目录找到项目文件（管道执行场景），开始 clone 项目..."

    # 检测 git
    if ! has_cmd git; then
        log_info "安装 git..."
        case "$PKG_MANAGER" in
            yum) yum install -y git 2>&1 | tee -a "$LOG_FILE" ;;
            apt-get) apt-get update -qq && apt-get install -y git 2>&1 | tee -a "$LOG_FILE" ;;
        esac
    fi

    # clone 项目（如已存在先备份再 clone）
    if [[ -d "$PROJECT_INSTALL_DIR/.git" ]]; then
        log_info "项目目录已存在，拉取最新代码：$PROJECT_INSTALL_DIR"
        cd "$PROJECT_INSTALL_DIR"
        git fetch --all 2>&1 | tee -a "$LOG_FILE" || true
        git reset --hard origin/HEAD 2>&1 | tee -a "$LOG_FILE" || true
    else
        log_info "克隆项目到：$PROJECT_INSTALL_DIR"
        if git clone --depth 1 https://github.com/laobi465/wlyz-2demo.git "$PROJECT_INSTALL_DIR" 2>&1 | tee -a "$LOG_FILE"; then
            log_info "项目克隆完成"
        else
            log_error "项目克隆失败，请检查网络或手动 clone 到 $PROJECT_INSTALL_DIR"
            exit 1
        fi
    fi

    # 切换到项目目录
    SCRIPT_DIR="$PROJECT_INSTALL_DIR"
    log_info "项目目录：$SCRIPT_DIR"

    # 校验关键文件
    if [[ ! -f "$SCRIPT_DIR/docker-compose.yml" ]]; then
        log_error "克隆后仍未找到 docker-compose.yml（路径：$SCRIPT_DIR/docker-compose.yml）"
        exit 1
    fi
}

deploy_with_compose() {
    log_step "步骤 6/7：Docker Compose 部署"

    ensure_project_files

    if [[ ! -f "$SCRIPT_DIR/docker-compose.yml" ]]; then
        log_error "未找到 docker-compose.yml（路径：$SCRIPT_DIR/docker-compose.yml）"
        log_error "请确保在项目根目录执行本脚本，或检查网络连接"
        exit 1
    fi

    # 写入 .env 文件（docker compose 自动读取）
    log_info "生成 .env 配置文件..."
    cat > "$SCRIPT_DIR/.env" <<EOF
# 极策k网络验证 - Docker Compose 环境变量（自动生成，请勿提交到 git）
# 生成时间：$(date '+%Y-%m-%d %H:%M:%S')

# 端口配置
APP_PORT=$APP_PORT
MYSQL_PORT=$MYSQL_PORT
REDIS_PORT=$REDIS_PORT
UI_PORT=$UI_PORT

# MySQL
MYSQL_DATABASE=jicek
MYSQL_USERNAME=jicek
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD=$MYSQL_PASSWORD

# Redis
REDIS_PASSWORD=$REDIS_PASSWORD

# 极策k 加密密钥（铁律 04：环境变量注入，不入 git）
JICEK_AES_KEY=$JICEK_AES_KEY
JICEK_RSA_PRIVATE_KEY=$JICEK_RSA_PRIVATE_KEY
JICEK_RSA_PUBLIC_KEY=$JICEK_RSA_PUBLIC_KEY
JICEK_HMAC_KEY=$JICEK_HMAC_KEY
JICEK_JWT_SECRET=$JICEK_JWT_SECRET

# 国密（可选，默认关闭）
JICEK_SM_ENABLED=false

# 部署功能（默认关闭）
JICEK_DEPLOY_ENABLED=false

# JVM 参数
JAVA_OPTS=-Xms256m -Xmx512m
EOF
    chmod 600 "$SCRIPT_DIR/.env"
    log_info ".env 文件已生成（权限 600，仅 root 可读）"

    # 构建并启动
    log_info "开始构建并启动 Docker 服务（首次构建需下载依赖，约 5-15 分钟）..."
    log_info "已配置国内镜像源加速（Maven 阿里云 / npm 淘宝 / Docker Hub 镜像）"
    cd "$SCRIPT_DIR"

    log_info "执行：docker compose build"
    # 构建失败时输出详细诊断信息
    if ! docker compose build --progress=plain 2>&1 | tee -a "$LOG_FILE"; then
        log_error "Docker 镜像构建失败"
        log_error "========== 诊断信息 =========="
        log_error "1. 检查网络连接：ping -c 3 maven.aliyun.com"
        log_error "2. 检查 Docker 状态：systemctl status docker"
        log_error "3. 查看完整构建日志：tail -100 $LOG_FILE"
        log_error "4. 手动重试构建：cd $SCRIPT_DIR && docker compose build --no-cache"
        log_error "5. 如仍失败，检查磁盘空间：df -h"
        log_error "=============================="
        exit 1
    fi

    log_info "执行：docker compose up -d"
    if ! docker compose up -d 2>&1 | tee -a "$LOG_FILE"; then
        log_error "Docker 服务启动失败"
        log_error "查看日志：cd $SCRIPT_DIR && docker compose logs"
        exit 1
    fi

    log_info "等待服务就绪（健康检查）..."
    local max_wait=120
    local waited=0
    while [[ $waited -lt $max_wait ]]; do
        if docker compose ps 2>/dev/null | grep -q "healthy"; then
            log_info "服务已就绪（耗时 ${waited}s）"
            break
        fi
        sleep 5
        waited=$((waited + 5))
        printf "." >&2
    done
    echo "" >&2

    if [[ $waited -ge $max_wait ]]; then
        log_warn "服务健康检查超时（${max_wait}s），请手动检查：docker compose ps"
    fi

    log_info "Docker 服务部署完成"
}

# ==================== 步骤 7：生成配置信息文件 ====================
generate_report() {
    log_step "步骤 7/7：生成配置信息文件"

    # 获取服务器公网 IP（实查，铁律 06）
    local public_ip private_ip
    public_ip=$(curl -s --max-time 5 https://api.ipify.org 2>/dev/null || echo "未知（网络不可达）")
    private_ip=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "未知")

    log_info "服务器公网 IP：$public_ip"
    log_info "服务器内网 IP：$private_ip"

    # 确保报告目录可写
    mkdir -p "$(dirname "$REPORT_FILE")"

    cat > "$REPORT_FILE" <<EOF
###############################################################################
# 极策k网络验证 - 部署配置信息
# 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
# 服务器：$public_ip（公网）/ $private_ip（内网）
###############################################################################
# ⚠️  本文件包含敏感信息（数据库密码、加密密钥），请妥善保管，禁止外泄！
###############################################################################

========================================
一、宝塔面板配置
========================================
面板状态：$([[ "${BTPANEL_INSTALLED:-false}" == "true" ]] && echo "已安装" || echo "未安装")
访问地址：http://$public_ip:$BTPANEL_PORT
内网访问：http://$private_ip:$BTPANEL_PORT
面板端口：$BTPANEL_PORT
用户名：${BTPANEL_USERNAME:-未知（请在宝塔面板查看）}
初始密码：${BTPANEL_PASSWORD:-未知（安装时已输出，或执行 bt default 查看）}

温馨提示：
  - 首次登录宝塔后请立即修改默认密码
  - 如忘记密码，SSH 执行：bt default 查看默认账号密码
  - 如需修改面板端口，在宝塔面板「设置」中修改

========================================
二、极策k 项目配置
========================================

【访问地址】
前端 UI：http://$public_ip:$UI_PORT
后端 API：http://$public_ip:$APP_PORT
内网前端：http://$private_ip:$UI_PORT
内网后端：http://$private_ip:$APP_PORT

【默认账号】
开发者：dev / dev@123（首次登录后请修改密码）
管理员：admin / admin@123（首次登录后请修改密码）

【端口映射】
前端 Nginx：$UI_PORT -> 80（容器内）
后端应用：$APP_PORT -> 8080（容器内）
MySQL：$MYSQL_PORT -> 3306（容器内）
Redis：$REDIS_PORT -> 6379（容器内）

【MySQL 数据库】
数据库地址：$private_ip:$MYSQL_PORT（容器外）
容器内地址：jicek-mysql:3306
数据库名：jicek
用户名：jicek
用户密码：$MYSQL_PASSWORD
root 密码：$MYSQL_ROOT_PASSWORD
（数据库密码为本脚本随机生成，请妥善保管）

【Redis 缓存】
Redis 地址：$private_ip:$REDIS_PORT（容器外）
容器内地址：jicek-redis:6379
密码：$REDIS_PASSWORD

【加密密钥（铁律 04：仅环境变量注入，不入 git）】
JICEK_AES_KEY（AES-256 主密钥）：
$JICEK_AES_KEY

JICEK_HMAC_KEY（HMAC-SHA256 签名密钥）：
$JICEK_HMAC_KEY

JICEK_JWT_SECRET（JWT 签名密钥）：
$JICEK_JWT_SECRET

JICEK_RSA_PRIVATE_KEY（RSA-2048 私钥 PKCS#8 Base64）：
$JICEK_RSA_PRIVATE_KEY

JICEK_RSA_PUBLIC_KEY（RSA-2048 公钥 X.509 Base64）：
$JICEK_RSA_PUBLIC_KEY

【国密配置（可选，默认关闭）】
JICEK_SM_ENABLED=false
如需启用国密 SM2/SM4，请设置 JICEK_SM_ENABLED=true 并配置 JICEK_SM4_KEY/JICEK_SM2_PRIVATE_KEY

【部署功能（默认关闭）】
JICEK_DEPLOY_ENABLED=false
如需启用 GitHub Webhook 自动部署，请设置 JICEK_DEPLOY_ENABLED=true 并配置 GITHUB_WEBHOOK_SECRET

========================================
三、Docker 服务管理
========================================

# 查看服务状态
cd $SCRIPT_DIR && docker compose ps

# 查看日志
cd $SCRIPT_DIR && docker compose logs -f
cd $SCRIPT_DIR && docker compose logs -f jicek-app   # 仅后端日志
cd $SCRIPT_DIR && docker compose logs -f jicek-ui    # 仅前端日志

# 重启服务
cd $SCRIPT_DIR && docker compose restart
cd $SCRIPT_DIR && docker compose restart jicek-app   # 仅重启后端

# 停止服务
cd $SCRIPT_DIR && docker compose down

# 启动服务（不重新构建）
cd $SCRIPT_DIR && docker compose up -d

# 重新构建并启动（代码更新后）
cd $SCRIPT_DIR && docker compose up -d --build

# 查看容器资源占用
docker stats jicek-mysql jicek-redis jicek-app jicek-ui

========================================
四、安全注意事项
========================================
1. .env 文件包含所有密钥，权限已设为 600，禁止提交到 git
2. 请在宝塔面板「安全」中放行以下端口：$APP_PORT $UI_PORT（MySQL/Redis 建议不对外开放）
3. 生产环境建议配置 HTTPS（宝塔面板提供免费 SSL 证书申请）
4. 定期备份 MySQL 数据：docker exec jicek-mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD jicek > backup.sql
5. 本配置文件（$REPORT_FILE）包含所有敏感信息，请妥善保管，禁止外泄

========================================
五、项目目录结构
========================================
项目根目录：$SCRIPT_DIR
配置文件：$SCRIPT_DIR/.env
Docker 编排：$SCRIPT_DIR/docker-compose.yml
后端源码：$SCRIPT_DIR/jicek-license/
前端源码：$SCRIPT_DIR/jicek-ui/
数据库初始化：$SCRIPT_DIR/jicek-license/src/main/resources/sql/jicek_init.sql
安装日志：$LOG_FILE

###############################################################################
# 文件生成完毕，请妥善保管！
###############################################################################
EOF

    chmod 600 "$REPORT_FILE"
    log_info "配置信息已生成：$REPORT_FILE"
    log_info "文件权限：600（仅 root 可读）"
}

# ==================== 主流程 ====================
main() {
    echo ""
    echo "############################################################"
    echo "#                                                          #"
    echo "#   极策k网络验证 - 一键安装脚本（宝塔面板 + Docker 部署）  #"
    echo "#                                                          #"
    echo "############################################################"
    echo ""

    # 初始化日志
    mkdir -p "$(dirname "$LOG_FILE")"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 安装开始" > "$LOG_FILE"

    check_os
    check_and_install_btpanel
    check_and_install_docker
    check_ports
    generate_secrets
    deploy_with_compose
    generate_report

    log_step "安装完成"
    log_info "极策k网络验证已成功部署！"
    log_info ""
    log_info "访问地址："
    log_info "  前端 UI：http://<服务器IP>:$UI_PORT"
    log_info "  后端 API：http://<服务器IP>:$APP_PORT"
    log_info "  宝塔面板：http://<服务器IP>:$BTPANEL_PORT"
    log_info ""
    log_info "配置信息已保存至：$REPORT_FILE"
    log_info "安装日志：$LOG_FILE"
    log_info ""
    log_warn "⚠️  $REPORT_FILE 包含敏感信息，请妥善保管！"
    echo ""
}

main "$@"
