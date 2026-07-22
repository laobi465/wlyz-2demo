#!/usr/bin/env bash
# 极策k Shell SDK 示例
# 作者: 极策k  日期: 2026-07-21
#
# 运行方式：
#   bash example.sh
#
# 环境准备：
#   - 准备 RSA 公钥文件 rsa_pub.pem
#   - 配置环境变量或直接修改下面的常量

set -e

# source SDK
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/jicek.sh"

# 重写心跳回调（可选）
jicek_on_heartbeat_success() {
    echo "[心跳] 成功，下次间隔 ${JICEK_HEARTBEAT_INTERVAL}s"
}
jicek_on_heartbeat_failure() {
    echo "[心跳] 失败（第 ${JICEK_FAIL_COUNT} 次）" >&2
}
jicek_on_heartbeat_disconnect() {
    echo "[心跳] 已断开（连续 5 次失败）" >&2
}

main() {
    # 检查参数
    if [[ $# -lt 4 ]]; then
        cat <<EOF
用法: $0 <server_url> <app_key> <sign_secret> <rsa_pub_pem_path> [card_key]

示例:
  $0 https://api.jicek.example.com ak_xxx sk_xxx ./rsa_pub.pem JK-DEMO-XXXX-XXXX-XXXX
EOF
        exit 1
    fi

    local server_url="$1"
    local app_key="$2"
    local sign_secret="$3"
    local rsa_pub_path="$4"
    local card_key="${5:-JK-DEMO-XXXX-XXXX-XXXX}"

    # 读取 RSA 公钥
    if [[ ! -f "$rsa_pub_path" ]]; then
        echo "RSA 公钥文件不存在: $rsa_pub_path" >&2
        exit 1
    fi
    local rsa_pub
    rsa_pub=$(<"$rsa_pub_path")

    # 初始化
    jicek_init "$server_url" "$app_key" "$sign_secret" "$rsa_pub"
    echo "SDK 初始化完成，服务端: $JICEK_SERVER_URL"

    # 1. 卡密验证
    echo ""
    echo "=== 卡密验证 ==="
    local result
    result=$(jicek_verify_card "$card_key")
    echo "响应: $result"
    echo "sessionId: $JICEK_SESSION_ID"

    # 2. 启动后台心跳
    echo ""
    echo "=== 启动心跳 ==="
    jicek_start_heartbeat
    echo "心跳进程 PID: $JICEK_HEARTBEAT_PID"

    # 3. 业务运行 30 秒
    echo ""
    echo "=== 业务运行 30s ==="
    for i in $(seq 1 30); do
        echo "运行中... $i"
        sleep 1
    done

    # 4. 退出
    echo ""
    echo "=== 退出 ==="
    jicek_logout
    echo "已退出"
}

main "$@"
