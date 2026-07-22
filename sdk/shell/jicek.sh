#!/usr/bin/env bash
# 极策k网络验证 Shell SDK
# 作者: 极策k  日期: 2026-07-21
#
# 依赖：bash 4+ / curl / openssl / 标准Unix工具（grep/sed/awk/cat）
# 可选：jq（用于JSON解析，缺失时使用内置最小解析器）
#
# 用法：
#   source jicek.sh
#   jicek_init "https://api.jicek.example.com" "ak_xxx" "sk_xxx" "$(<rsa_pub.pem)"
#   jicek_verify_card "JK-DEMO-XXXX-XXXX-XXXX"
#   echo "sessionId=$JICEK_SESSION_ID"
#   jicek_start_heartbeat
#   # ...业务...
#   jicek_logout
#
# 文档：见 README.md

set -o pipefail

# ==================== 全局状态 ====================

JICEK_VERSION="0.3.1"
JICEK_SERVER_URL=""
JICEK_APP_KEY=""
JICEK_SIGN_SECRET=""
JICEK_RSA_PUBLIC_KEY=""
JICEK_TIMEOUT=10
JICEK_SESSION_ID=""
JICEK_HEARTBEAT_INTERVAL=60
JICEK_HEARTBEAT_PID=""
JICEK_FAIL_COUNT=0
JICEK_HEARTBEAT_STOP=0

# 心跳回调（用户可重写）
jicek_on_heartbeat_success() { :; }
jicek_on_heartbeat_failure() { :; }
jicek_on_heartbeat_disconnect() { :; }
jicek_on_device_banned() { :; }

# ==================== 工具函数 ====================

# 输出错误到 stderr
jicek_log_error() {
    echo "[Jicek Error] $*" >&2
}

# 抛出异常（退出码 = 错误码）
jicek_throw() {
    local code="$1"
    local msg="$2"
    jicek_log_error "[$code] $msg"
    exit "$code"
}

# 当前时间戳（13 位毫秒）
jicek_now_ms() {
    # date +%s%3N 在 GNU date 可用，BSD date 需回退
    local ts
    ts=$(date +%s%3N 2>/dev/null)
    if [[ ! "$ts" =~ ^[0-9]{13}$ ]]; then
        ts=$(date +%s)000
    fi
    echo "$ts"
}

# UUID v4（用 uuidgen 或 /proc/sys/kernel/random/uuid）
jicek_uuid_v4() {
    if command -v uuidgen >/dev/null 2>&1; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    elif [[ -r /proc/sys/kernel/random/uuid ]]; then
        cat /proc/sys/kernel/random/uuid
    else
        # 退化方案：用 $RANDOM 拼凑
        printf '%04x%04x-%04x-4%03x-%04x-%04x%04x%04x\n' \
            $RANDOM $RANDOM $RANDOM $((RANDOM % 4096)) \
            $((RANDOM % 65536)) $RANDOM $RANDOM $RANDOM
    fi
}

# 探测 jq 是否可用
jicek_have_jq() {
    command -v jq >/dev/null 2>&1
}

# ==================== 加密函数 ====================

# SHA-256 十六进制（小写）
# 用法：jicek_sha256_hex "string"
jicek_sha256_hex() {
    printf '%s' "$1" | openssl dgst -sha256 -hex 2>/dev/null | awk '{print $NF}'
}

# HMAC-SHA256 Base64
# 用法：jicek_hmac_sha256_b64 "data" "secret"
jicek_hmac_sha256_b64() {
    local data="$1"
    local secret="$2"
    printf '%s' "$data" | openssl dgst -sha256 -hmac "$secret" -binary 2>/dev/null | openssl base64 -A 2>/dev/null
}

# RSA-2048-OAEP 加密（输出 Base64）
# 用法：jicek_rsa_encrypt_oaep "plaintext" "public_key_pem"
jicek_rsa_encrypt_oaep() {
    local plaintext="$1"
    local pubkey="$2"
    local tmp_pem tmp_in tmp_out
    tmp_pem=$(mktemp)
    tmp_in=$(mktemp)
    tmp_out=$(mktemp)
    printf '%s' "$pubkey" > "$tmp_pem"
    printf '%s' "$plaintext" > "$tmp_in"
    openssl pkeyutl -encrypt -inkey "$tmp_pem" -pubin \
        -in "$tmp_in" -out "$tmp_out" \
        -pkeyopt rsa_padding_mode:oaep \
        -pkeyopt rsa_oaep_md:sha256 2>/dev/null
    if [[ $? -ne 0 ]]; then
        rm -f "$tmp_pem" "$tmp_in" "$tmp_out"
        return 1
    fi
    openssl base64 -A -in "$tmp_out" 2>/dev/null
    rm -f "$tmp_pem" "$tmp_in" "$tmp_out"
}

# 构造签名原文
# 用法：jicek_build_sign_payload "POST" "/path" "ts" "nonce" "body_sha"
jicek_build_sign_payload() {
    printf '%s\n%s\n%s\n%s\n%s' "$1" "$2" "$3" "$4" "$5"
}

# ==================== JSON 工具 ====================

# JSON 字符串转义
jicek_json_escape() {
    local s="$1"
    s="${s//\\/\\\\}"
    s="${s//\"/\\\"}"
    s="${s//$'\n'/\\n}"
    s="${s//$'\r'/\\r}"
    s="${s//$'\t'/\\t}"
    printf '%s' "$s"
}

# 从 JSON 字符串提取字段值（简单实现，仅适用于无嵌套的字符串/数字字段）
# 不依赖 jq 的回退方案
jicek_json_get() {
    local json="$1"
    local key="$2"
    if jicek_have_jq; then
        echo "$json" | jq -r "$key" 2>/dev/null
        return
    fi
    # 简化提取：匹配 "key":"value" 或 "key":number
    # 注：此实现不支持嵌套对象，复杂响应建议安装 jq
    local val
    val=$(echo "$json" | sed -nE 's/.*"'"$key"'"\s*:\s*"([^"]*)".*/\1/p')
    if [[ -z "$val" ]]; then
        val=$(echo "$json" | sed -nE 's/.*"'"$key"'"\s*:\s*([0-9]+).*/\1/p')
    fi
    echo "$val"
}

# 获取顶层 code 字段
jicek_json_code() {
    jicek_json_get "$1" "code"
}

# 获取顶层 msg 字段
jicek_json_msg() {
    jicek_json_get "$1" "msg"
}

# 获取 data.* 字段
jicek_json_data() {
    local json="$1"
    local field="$2"
    if jicek_have_jq; then
        echo "$json" | jq -r ".data.$field // empty" 2>/dev/null
        return
    fi
    # 回退：从 data 段中提取
    local data_section
    data_section=$(echo "$json" | sed -nE 's/.*"data"\s*:\s*(\{.*\}).*/\1/p')
    if [[ -n "$data_section" ]]; then
        jicek_json_get "$data_section" "$field"
    fi
}

# ==================== 设备指纹采集 ====================

jicek_os_type() {
    local uname_s
    uname_s=$(uname -s 2>/dev/null)
    case "$uname_s" in
        Linux*) echo "linux" ;;
        Darwin*) echo "macos" ;;
        MINGW*|MSYS*|CYGWIN*) echo "windows" ;;
        *) echo "linux" ;;
    esac
}

jicek_collect_cpu() {
    local t
    t=$(jicek_os_type)
    case "$t" in
        macos)
            sysctl -n machdep.cpu.brand_string 2>/dev/null
            ;;
        windows)
            # Git Bash / MSYS 环境下调用 wmic
            wmic cpu get ProcessorId /value 2>/dev/null | tr -d '\r' | grep -i ProcessorId | cut -d= -f2 | head -1
            ;;
        *)
            cat /proc/cpuinfo 2>/dev/null | grep -i serial | head -1 | sed -E 's/serial\s*:?//' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
            ;;
    esac
}

jicek_collect_mainboard() {
    local t
    t=$(jicek_os_type)
    case "$t" in
        macos)
            ioreg -l 2>/dev/null | grep IOPlatformSerialNumber | head -1
            ;;
        windows)
            wmic baseboard get SerialNumber /value 2>/dev/null | tr -d '\r' | grep -i SerialNumber | cut -d= -f2 | head -1
            ;;
        *)
            cat /sys/class/dmi/id/board_serial 2>/dev/null || dmidecode -s baseboard-serial-number 2>/dev/null
            ;;
    esac
}

jicek_collect_disk() {
    local t
    t=$(jicek_os_type)
    case "$t" in
        macos)
            diskutil info / 2>/dev/null | grep 'Volume UUID' | awk '{print $3}'
            ;;
        windows)
            wmic diskdrive get SerialNumber /value 2>/dev/null | tr -d '\r' | grep -i SerialNumber | cut -d= -f2 | head -1
            ;;
        *)
            cat /sys/block/sda/device/serial 2>/dev/null || hdparm -I /dev/sda 2>/dev/null | grep 'Serial No' | awk -F: '{print $2}' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
            ;;
    esac
}

jicek_collect_mac() {
    local t
    t=$(jicek_os_type)
    case "$t" in
        macos)
            ifconfig en0 2>/dev/null | grep ether | awk '{print $2}'
            ;;
        windows)
            getmac /fo csv /nh 2>/dev/null | head -1 | cut -d, -f1 | tr -d '"'
            ;;
        *)
            local iface
            iface=$(ip route 2>/dev/null | grep default | awk '{print $5}' | head -1)
            if [[ -n "$iface" ]]; then
                cat "/sys/class/net/$iface/address" 2>/dev/null
            fi
            ;;
    esac
}

jicek_collect_bios() {
    local t
    t=$(jicek_os_type)
    case "$t" in
        macos)
            ioreg -l 2>/dev/null | grep IOPlatformUUID | head -1
            ;;
        windows)
            wmic bios get SerialNumber /value 2>/dev/null | tr -d '\r' | grep -i SerialNumber | cut -d= -f2 | head -1
            ;;
        *)
            cat /sys/class/dmi/id/product_uuid 2>/dev/null || dmidecode -s system-uuid 2>/dev/null
            ;;
    esac
}

jicek_detect_vm_extra() {
    local t
    t=$(jicek_os_type)
    case "$t" in
        linux)
            local cgroup
            cgroup=$(cat /proc/self/cgroup 2>/dev/null | grep docker | head -1)
            if [[ "$cgroup" == *docker* ]]; then
                local cid
                cid=$(echo "$cgroup" | sed -nE 's/.*docker\/([a-f0-9]+).*/\1/p')
                if [[ -n "$cid" ]]; then
                    echo "container:$cid"
                    return
                fi
            fi
            local vm_uuid
            vm_uuid=$(dmidecode -s system-uuid 2>/dev/null)
            if [[ -n "$vm_uuid" && "$vm_uuid" != "Not Settable" && "$vm_uuid" != "Not Specified" ]]; then
                echo "vm:$vm_uuid"
            fi
            ;;
        windows)
            local model
            model=$(wmic computersystem get Model /value 2>/dev/null | tr -d '\r' | grep -i Model | cut -d= -f2)
            if [[ "$model" == *Virtual* || "$model" == *VMware* || "$model" == *KVM* ]]; then
                local uuid_str
                uuid_str=$(wmic csproduct get UUID /value 2>/dev/null | tr -d '\r' | grep -i UUID | cut -d= -f2)
                echo "vm:$uuid_str"
            fi
            ;;
    esac
}

# 采集设备指纹，输出 JSON 字符串
# 输出格式：{"fingerprint":"...","encryptedDetail":"...","isVm":0,"vmExtra":"","osType":"...","osVersion":"...","deviceName":"...","clientVersion":"..."}
jicek_collect_fingerprint() {
    local cpu mainboard disk mac bios
    cpu=$(jicek_collect_cpu)
    mainboard=$(jicek_collect_mainboard)
    disk=$(jicek_collect_disk)
    mac=$(jicek_collect_mac)
    bios=$(jicek_collect_bios)

    local cpu_h mb_h disk_h mac_h bios_h
    cpu_h=$(jicek_sha256_hex "$cpu")
    mb_h=$(jicek_sha256_hex "$mainboard")
    disk_h=$(jicek_sha256_hex "$disk")
    mac_h=$(jicek_sha256_hex "$mac")
    bios_h=$(jicek_sha256_hex "$bios")

    local vm_extra
    vm_extra=$(jicek_detect_vm_extra)
    local is_vm=0
    if [[ -n "$vm_extra" ]]; then
        is_vm=1
    fi

    local fp_input="$cpu_h$mb_h$disk_h$mac_h$bios_h"
    if [[ -n "$vm_extra" ]]; then
        fp_input="${fp_input}${vm_extra}"
    fi
    local fingerprint
    fingerprint=$(jicek_sha256_hex "$fp_input")

    # 5 维哈希 JSON → RSA 加密
    local detail_json
    detail_json=$(printf '{"cpu":"%s","mainboard":"%s","disk":"%s","mac":"%s","bios":"%s"}' \
        "$cpu_h" "$mb_h" "$disk_h" "$mac_h" "$bios_h")
    local encrypted_detail
    encrypted_detail=$(jicek_rsa_encrypt_oaep "$detail_json" "$JICEK_RSA_PUBLIC_KEY")
    if [[ $? -ne 0 ]]; then
        jicek_throw 500 "RSA 加密失败"
    fi

    local os_type os_ver device_name
    os_type=$(jicek_os_type)
    os_ver=$(uname -r 2>/dev/null)
    device_name=$(hostname 2>/dev/null)

    # 输出 JSON
    printf '{"fingerprint":"%s","encryptedDetail":"%s","isVm":%d,"vmExtra":"%s","osType":"%s","osVersion":"%s","deviceName":"%s","clientVersion":"jicek-sdk-shell-%s"}' \
        "$fingerprint" "$encrypted_detail" "$is_vm" \
        "$(jicek_json_escape "$vm_extra")" \
        "$os_type" "$os_ver" "$(jicek_json_escape "$device_name")" \
        "$JICEK_VERSION"
}

# ==================== HTTP 请求 ====================

# 构造带签名的请求头（输出 associative array 通过 eval 设置）
# 用法：jicek_build_signed_headers "POST" "/path" "body" "device_id"
# 输出：直接 echo 多行 "Key: Value" 格式供 curl -H 使用
jicek_build_signed_headers() {
    local method="$1"
    local path="$2"
    local body="$3"
    local device_id="$4"

    local timestamp nonce body_sha payload signature
    timestamp=$(jicek_now_ms)
    nonce=$(jicek_uuid_v4)
    body_sha=""
    if [[ -n "$body" ]]; then
        body_sha=$(jicek_sha256_hex "$body")
    fi
    payload=$(jicek_build_sign_payload "$method" "$path" "$timestamp" "$nonce" "$body_sha")
    signature=$(jicek_hmac_sha256_b64 "$payload" "$JICEK_SIGN_SECRET")

    echo "X-App-Key: $JICEK_APP_KEY"
    echo "X-Timestamp: $timestamp"
    echo "X-Nonce: $nonce"
    echo "X-Signature: $signature"
    echo "X-Device-Id: $device_id"
    echo "Content-Type: application/json; charset=UTF-8"
}

# 执行 HTTP 请求
# 用法：jicek_http_request "POST" "/path" "json_body" "device_id"
# 输出：响应体（写到 stdout）
jicek_http_request() {
    local method="$1"
    local path="$2"
    local body="$3"
    local device_id="$4"

    local headers
    headers=$(jicek_build_signed_headers "$method" "$path" "$body" "$device_id")

    local url="${JICEK_SERVER_URL}${path}"
    local -a curl_args=(
        -s -S
        -X "$method"
        -m "$JICEK_TIMEOUT"
        --connect-timeout 5
    )
    # 添加 headers
    while IFS= read -r line; do
        curl_args+=(-H "$line")
    done <<< "$headers"
    if [[ -n "$body" ]]; then
        curl_args+=(--data-binary "@-")
    fi
    curl_args+=("$url")

    if [[ -n "$body" ]]; then
        printf '%s' "$body" | curl "${curl_args[@]}"
    else
        curl "${curl_args[@]}"
    fi
}

# 解析响应，失败时抛出异常
# 用法：jicek_parse_response "resp_text"
# 成功：返回 data 部分的 JSON
jicek_parse_response() {
    local resp="$1"
    if [[ -z "$resp" ]]; then
        jicek_throw 500 "空响应"
    fi
    local code msg
    code=$(jicek_json_code "$resp")
    msg=$(jicek_json_msg "$resp")
    if [[ "$code" != "200" ]]; then
        jicek_throw "${code:-500}" "${msg:-未知错误}"
    fi
    # 返回 data 部分（如果有 jq，输出 data 对象；否则原样返回）
    if jicek_have_jq; then
        echo "$resp" | jq -c '.data // empty' 2>/dev/null
    else
        echo "$resp"
    fi
}

# ==================== 核心接口 ====================

# 初始化客户端
# 用法：jicek_init "server_url" "app_key" "sign_secret" "rsa_public_key_pem" [timeout]
jicek_init() {
    JICEK_SERVER_URL="${1%/}"
    JICEK_APP_KEY="$2"
    JICEK_SIGN_SECRET="$3"
    JICEK_RSA_PUBLIC_KEY="$4"
    if [[ -n "$5" ]]; then
        JICEK_TIMEOUT="$5"
    fi
    JICEK_SESSION_ID=""
    JICEK_HEARTBEAT_INTERVAL=60
    JICEK_FAIL_COUNT=0

    # 校验依赖
    if ! command -v curl >/dev/null 2>&1; then
        jicek_throw 500 "缺少依赖：curl"
    fi
    if ! command -v openssl >/dev/null 2>&1; then
        jicek_throw 500 "缺少依赖：openssl"
    fi
    if [[ -z "$JICEK_SERVER_URL" || -z "$JICEK_APP_KEY" || -z "$JICEK_SIGN_SECRET" || -z "$JICEK_RSA_PUBLIC_KEY" ]]; then
        jicek_throw 500 "初始化参数不完整"
    fi
}

# 卡密验证
# 用法：jicek_verify_card "card_key"
# 成功后 JICEK_SESSION_ID 被设置
jicek_verify_card() {
    local card_key="$1"
    if [[ -z "$card_key" ]]; then
        jicek_throw 1001 "卡密不能为空"
    fi

    local fp_json card_cipher
    fp_json=$(jicek_collect_fingerprint)
    local fingerprint
    fingerprint=$(jicek_json_get "$fp_json" "fingerprint")

    card_cipher=$(jicek_rsa_encrypt_oaep "$card_key" "$JICEK_RSA_PUBLIC_KEY")
    if [[ $? -ne 0 ]]; then
        jicek_throw 1008 "卡密加密失败"
    fi

    local device_name os_type os_ver client_version is_vm vm_extra encrypted_detail
    fingerprint=$(jicek_json_get "$fp_json" "fingerprint")
    encrypted_detail=$(jicek_json_get "$fp_json" "encryptedDetail")
    device_name=$(jicek_json_get "$fp_json" "deviceName")
    os_type=$(jicek_json_get "$fp_json" "osType")
    os_ver=$(jicek_json_get "$fp_json" "osVersion")
    client_version=$(jicek_json_get "$fp_json" "clientVersion")
    is_vm=$(jicek_json_get "$fp_json" "isVm")
    vm_extra=$(jicek_json_get "$fp_json" "vmExtra")

    local body
    body=$(printf '{"fingerprint":"%s","encryptedDetail":"%s","cardCipher":"%s","deviceName":"%s","osType":"%s","osVersion":"%s","clientVersion":"%s","isVm":%s,"vmExtra":"%s"}' \
        "$fingerprint" "$encrypted_detail" "$card_cipher" \
        "$(jicek_json_escape "$device_name")" "$os_type" "$(jicek_json_escape "$os_ver")" \
        "$client_version" "$is_vm" "$(jicek_json_escape "$vm_extra")")

    local resp
    resp=$(jicek_http_request "POST" "/api/sdk/card/verify" "$body" "$fingerprint")
    local data
    data=$(jicek_parse_response "$resp")

    # 提取 sessionId
    if jicek_have_jq; then
        JICEK_SESSION_ID=$(echo "$data" | jq -r '.sessionId // empty' 2>/dev/null)
    else
        JICEK_SESSION_ID=$(jicek_json_get "$resp" "sessionId")
    fi
    echo "$data"
}

# 单次心跳
# 用法：jicek_heartbeat
jicek_heartbeat() {
    local fp_json
    fp_json=$(jicek_collect_fingerprint)
    local fingerprint
    fingerprint=$(jicek_json_get "$fp_json" "fingerprint")

    local timestamp nonce
    timestamp=$(jicek_now_ms)
    nonce=$(jicek_uuid_v4)

    local body
    body=$(printf '{"tenantId":0,"softwareId":0,"fingerprint":"%s","timestamp":%s,"nonce":"%s"}' \
        "$fingerprint" "$timestamp" "$nonce")

    local resp
    resp=$(jicek_http_request "POST" "/api/sdk/device/heartbeat" "$body" "$fingerprint")
    local data
    data=$(jicek_parse_response "$resp")

    # 更新心跳间隔
    local next_interval
    if jicek_have_jq; then
        next_interval=$(echo "$data" | jq -r '.nextInterval // 60' 2>/dev/null)
    else
        next_interval=$(jicek_json_get "$resp" "nextInterval")
    fi
    if [[ "$next_interval" =~ ^[0-9]+$ ]]; then
        JICEK_HEARTBEAT_INTERVAL="$next_interval"
    fi
}

# 心跳循环（在后台子 shell 中运行）
jicek_heartbeat_loop() {
    JICEK_FAIL_COUNT=0
    while [[ "$JICEK_HEARTBEAT_STOP" -eq 0 ]]; do
        local interval="$JICEK_HEARTBEAT_INTERVAL"
        if [[ "$JICEK_FAIL_COUNT" -gt 0 ]]; then
            interval=$(( 2 ** JICEK_FAIL_COUNT ))
            if [[ "$interval" -gt 30 ]]; then
                interval=30
            fi
        fi

        if jicek_heartbeat; then
            JICEK_FAIL_COUNT=0
            jicek_on_heartbeat_success
        else
            JICEK_FAIL_COUNT=$(( JICEK_FAIL_COUNT + 1 ))
            jicek_on_heartbeat_failure
            if [[ "$JICEK_FAIL_COUNT" -ge 5 ]]; then
                jicek_on_heartbeat_disconnect
                return 1
            fi
        fi

        sleep "$interval"
    done
}

# 启动后台心跳
jicek_start_heartbeat() {
    if [[ -n "$JICEK_HEARTBEAT_PID" ]] && kill -0 "$JICEK_HEARTBEAT_PID" 2>/dev/null; then
        return 0
    fi
    JICEK_HEARTBEAT_STOP=0
    jicek_heartbeat_loop &
    JICEK_HEARTBEAT_PID=$!
}

# 停止心跳
jicek_stop_heartbeat() {
    JICEK_HEARTBEAT_STOP=1
    if [[ -n "$JICEK_HEARTBEAT_PID" ]]; then
        kill "$JICEK_HEARTBEAT_PID" 2>/dev/null
        wait "$JICEK_HEARTBEAT_PID" 2>/dev/null
        JICEK_HEARTBEAT_PID=""
    fi
}

# 退出登录
jicek_logout() {
    jicek_stop_heartbeat
    if [[ -n "$JICEK_SESSION_ID" ]]; then
        local body
        body=$(printf '{"sessionId":"%s"}' "$JICEK_SESSION_ID")
        # 忽略错误
        jicek_http_request "POST" "/api/sdk/auth/logout" "$body" "" >/dev/null 2>&1 || true
        JICEK_SESSION_ID=""
    fi
}

# ==================== 模块就绪 ====================

# 仅在被 source 时输出就绪信息（直接执行时打印帮助）
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    echo "极策k网络验证 Shell SDK v$JICEK_VERSION"
    echo "作者: 极策k  日期: 2026-07-21"
    echo ""
    echo "用法：source jicek.sh"
    echo ""
    echo "示例："
    echo "  jicek_init \"https://api.jicek.example.com\" \"ak_xxx\" \"sk_xxx\" \"\$(<rsa_pub.pem)\""
    echo "  jicek_verify_card \"JK-DEMO-XXXX-XXXX-XXXX\""
    echo "  jicek_start_heartbeat"
    echo "  jicek_logout"
fi
