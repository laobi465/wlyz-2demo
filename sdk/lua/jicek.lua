--[[
极策k网络验证 Lua SDK
作者: 极策k  日期: 2026-07-21

零硬依赖设计：
- HTTP：优先 LuaSocket，回退到 curl 命令行
- 加密：优先 luaossl，回退到 openssl 命令行
- 指纹：io.popen 执行系统命令（Windows wmic / Linux /proc / macOS sysctl）

三件套：
1. 卡密验证（verify_card）
2. 心跳保活（heartbeat / start_heartbeat）
3. 设备绑定/换机（bind_device / unbind_device）

适用场景：游戏脚本（如 OpenWRT 路由器、嵌入式设备、自动化脚本）
]]

local M = {}
M._NAME = "jicek"
M._VERSION = "0.3.1"

-- ==================== 异常 ====================

local JicekException = {}
JicekException.__index = JicekException

function M.new_exception(code, msg)
    local self = setmetatable({}, JicekException)
    self.code = code
    self.msg = msg
    return self
end

function JicekException:__tostring()
    return string.format("[%d] %s", self.code, self.msg)
end

M.JicekException = JicekException

-- ==================== 工具函数 ====================

local function shell_exec(cmd)
    -- 执行 shell 命令，返回 stdout 字符串（失败返回空字符串）
    local handle = io.popen(cmd .. " 2>/dev/null", "r")
    if not handle then return "" end
    local out = handle:read("*a") or ""
    handle:close()
    -- 去 trailing 换行
    out = out:gsub("[\r\n]+$", "")
    return out
end

local function os_type()
    local package_config = package.config:sub(1, 1)
    if package_config == "\\" then return "windows" end
    local uname = shell_exec("uname -s 2>/dev/null")
    if uname:match("Darwin") then return "macos" end
    if uname:match("Linux") then return "linux" end
    return "linux"
end

local function uuid_v4()
    -- 生成 UUID v4（不依赖外部库）
    local template = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx"
    local res = template:gsub("[xy]", function(c)
        local v = (c == "x") and math.random(0, 0xf) or math.random(8, 0xb)
        return string.format("%x", v)
    end)
    return res
end

local function now_ms()
    return tostring(math.floor(os.time() * 1000))
end

-- ==================== 加密模块 ====================

local Crypto = {}

-- 探测可用的加密后端：luaossl 或 openssl 命令行
local crypto_backend = nil
local function detect_crypto_backend()
    if crypto_backend then return crypto_backend end
    local ok, openssl = pcall(require, "openssl")
    if ok and openssl then
        crypto_backend = "luaossl"
        Crypto._openssl = openssl
        return crypto_backend
    end
    -- 检查 openssl 命令行
    local ver = shell_exec("openssl version 2>/dev/null")
    if ver and ver:match("OpenSSL") then
        crypto_backend = "cli"
        return crypto_backend
    end
    return nil
end

-- SHA-256 十六进制（64 字符小写）
function Crypto.sha256_hex(data)
    detect_crypto_backend()
    if crypto_backend == "luaossl" then
        local digest = Crypto._openssl.digest.new("sha256")
        digest:update(data)
        return digest:final():lower():gsub("(.)", function(c)
            return string.format("%02x", c:byte())
        end)
    end
    -- CLI 回退
    local cmd = string.format("printf '%%s' %s | openssl dgst -sha256 -hex 2>/dev/null | awk '{print $NF}'",
        data:gsub("'", "'\\''"))
    return shell_exec(cmd)
end

-- HMAC-SHA256 Base64
function Crypto.hmac_sha256_base64(data, secret)
    detect_crypto_backend()
    if crypto_backend == "luaossl" then
        local hmac = Crypto._openssl.hmac.new(secret, "sha256")
        hmac:update(data)
        local raw = hmac:final()
        return Crypto._base64_encode(raw)
    end
    -- CLI 回退
    local cmd = string.format("printf '%%s' %s | openssl dgst -sha256 -hmac %s -binary 2>/dev/null | openssl base64 -A",
        data:gsub("'", "'\\''"), secret:gsub("'", "'\\''"))
    return shell_exec(cmd)
end

-- Base64 编码（luaossl 后端使用）
function Crypto._base64_encode(raw)
    if Crypto._openssl and Crypto._openssl.base64 then
        return Crypto._openssl.base64(raw, false, true) -- no_pad=false, url=false
    end
    -- 纯 Lua Base64
    local b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    return ((raw:gsub(".", function(x)
        local r, b = "", x:byte()
        for i = 8, 1, -1 do r = r .. (b % 2 ^ i - b % 2 ^ (i - 1) > 0 and "1" or "0") end
        return r
    end) .. "0000"):gsub("%d%d%d?%d?%d?%d?%d?%d?", function(x)
        if #x ~= 8 then return "" end
        local c = 0
        for i = 1, 8 do c = c + (x:sub(i, i) == "1" and 2 ^ (8 - i) or 0) end
        return b:sub(c + 1, c + 1)
    end)) .. ({ "", "==", "=" })[#raw % 3 + 1]
end

-- RSA-2048-OAEP 加密（卡密传输）
-- public_key_pem: PEM 格式公钥字符串
function Crypto.rsa_encrypt_oaep(plaintext, public_key_pem)
    detect_crypto_backend()
    if crypto_backend == "luaossl" then
        local pkey = Crypto._openssl.pkey.new({ public = public_key_pem })
        local cipher = pkey:encrypt(plaintext, "oaep", { md = "sha256", mgf1 = "sha256" })
        return Crypto._base64_encode(cipher)
    end
    -- CLI 回退：写入临时文件
    local tmp_pem = os.tmpname()
    local tmp_in = os.tmpname()
    local tmp_out = os.tmpname()
    local f = io.open(tmp_pem, "w")
    f:write(public_key_pem)
    f:close()
    f = io.open(tmp_in, "w")
    f:write(plaintext)
    f:close()
    local cmd = string.format(
        "openssl pkeyutl -encrypt -inkey %s -pubin -in %s -out %s -pkeyopt rsa_padding_mode:oaep -pkeyopt rsa_oaep_md:sha256 2>/dev/null && openssl base64 -A -in %s 2>/dev/null",
        tmp_pem, tmp_in, tmp_out, tmp_out)
    local result = shell_exec(cmd)
    os.remove(tmp_pem)
    os.remove(tmp_in)
    os.remove(tmp_out)
    return result
end

-- 构造签名原文：METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_SHA256
function Crypto.build_sign_payload(method, path, timestamp, nonce, body_sha)
    return string.format("%s\n%s\n%s\n%s\n%s", method, path, timestamp, nonce, body_sha or "")
end

M.Crypto = Crypto

-- ==================== JSON 最小实现 ====================

local Json = {}

-- 简单 escape
local function json_escape(s)
    s = tostring(s or "")
    s = s:gsub("\\", "\\\\")
    s = s:gsub('"', '\\"')
    s = s:gsub("\n", "\\n")
    s = s:gsub("\r", "\\r")
    s = s:gsub("\t", "\\t")
    return s
end

-- 编码 table 到 JSON 字符串
function Json.encode(t)
    if type(t) ~= "table" then return "null" end
    -- 检测数组 vs 对象
    local is_array = true
    local n = 0
    for k, _ in pairs(t) do
        n = n + 1
        if type(k) ~= "number" then is_array = false break end
    end
    if n == 0 then return "{}" end

    if is_array then
        local parts = {}
        for i = 1, n do
            parts[i] = Json.encode(t[i])
        end
        return "[" .. table.concat(parts, ",") .. "]"
    else
        local parts = {}
        for k, v in pairs(t) do
            local val
            if type(v) == "table" then
                val = Json.encode(v)
            elseif type(v) == "string" then
                val = '"' .. json_escape(v) .. '"'
            elseif type(v) == "boolean" then
                val = v and "true" or "false"
            elseif type(v) == "number" then
                val = tostring(v)
            else
                val = "null"
            end
            parts[#parts + 1] = '"' .. json_escape(tostring(k)) .. '":' .. val
        end
        return "{" .. table.concat(parts, ",") .. "}"
    end
end

-- 简单 JSON 解析（仅解析响应顶层结构：code/msg/data）
-- 由于服务端响应结构固定，这里实现一个简化的递归下降解析器
local function skip_ws(s, i)
    while i <= #s and (s:sub(i, i) == " " or s:sub(i, i) == "\t"
        or s:sub(i, i) == "\n" or s:sub(i, i) == "\r") do
        i = i + 1
    end
    return i
end

local function parse_value(s, i)
    i = skip_ws(s, i)
    local c = s:sub(i, i)
    if c == "{" then
        local obj = {}
        i = i + 1
        i = skip_ws(s, i)
        if s:sub(i, i) == "}" then return obj, i + 1 end
        while true do
            i = skip_ws(s, i)
            local key
            key, i = parse_value(s, i)
            i = skip_ws(s, i)
            if s:sub(i, i) ~= ":" then return nil, i end
            i = i + 1
            local val
            val, i = parse_value(s, i)
            obj[key] = val
            i = skip_ws(s, i)
            if s:sub(i, i) == "}" then return obj, i + 1 end
            if s:sub(i, i) ~= "," then return nil, i end
            i = i + 1
        end
    elseif c == "[" then
        local arr = {}
        i = i + 1
        i = skip_ws(s, i)
        if s:sub(i, i) == "]" then return arr, i + 1 end
        while true do
            local val
            val, i = parse_value(s, i)
            arr[#arr + 1] = val
            i = skip_ws(s, i)
            if s:sub(i, i) == "]" then return arr, i + 1 end
            if s:sub(i, i) ~= "," then return nil, i end
            i = i + 1
        end
    elseif c == '"' then
        i = i + 1
        local buf = {}
        while i <= #s do
            local ch = s:sub(i, i)
            if ch == "\\" then
                local next_ch = s:sub(i + 1, i + 1)
                if next_ch == "n" then buf[#buf + 1] = "\n"
                elseif next_ch == "r" then buf[#buf + 1] = "\r"
                elseif next_ch == "t" then buf[#buf + 1] = "\t"
                elseif next_ch == '"' then buf[#buf + 1] = '"'
                elseif next_ch == "\\" then buf[#buf + 1] = "\\"
                else buf[#buf + 1] = next_ch end
                i = i + 2
            elseif ch == '"' then
                return table.concat(buf), i + 1
            else
                buf[#buf + 1] = ch
                i = i + 1
            end
        end
        return nil, i
    elseif c == "t" then
        return true, i + 4 -- true
    elseif c == "f" then
        return false, i + 5 -- false
    elseif c == "n" then
        return nil, i + 4 -- null
    else
        -- number
        local start = i
        while i <= #s and s:sub(i, i):match("[-+0-9.eE]") do
            i = i + 1
        end
        return tonumber(s:sub(start, i - 1)), i
    end
end

function Json.decode(s)
    local v, _ = parse_value(s, 1)
    return v
end

M.Json = Json

-- ==================== 设备指纹采集 ====================

local FingerprintCollector = {}
FingerprintCollector.__index = FingerprintCollector

function M.new_fingerprint_collector()
    return setmetatable({}, FingerprintCollector)
end

function FingerprintCollector:collect_cpu()
    local t = os_type()
    if t == "windows" then
        return shell_exec("wmic cpu get ProcessorId /value"):gsub("ProcessorId=", ""):gsub("^%s+", ""):gsub("%s+$", "")
    elseif t == "macos" then
        return shell_exec("sysctl -n machdep.cpu.brand_string")
    else
        return shell_exec("cat /proc/cpuinfo 2>/dev/null | grep -i serial | head -1"):gsub("serial%s*:?", ""):gsub("^%s+", ""):gsub("%s+$", "")
    end
end

function FingerprintCollector:collect_mainboard()
    local t = os_type()
    if t == "windows" then
        return shell_exec("wmic baseboard get SerialNumber /value"):gsub("SerialNumber=", ""):gsub("^%s+", ""):gsub("%s+$", "")
    elseif t == "macos" then
        return shell_exec("ioreg -l 2>/dev/null | grep IOPlatformSerialNumber | head -1")
    else
        return shell_exec("cat /sys/class/dmi/id/board_serial 2>/dev/null || dmidecode -s baseboard-serial-number 2>/dev/null"):gsub("^%s+", ""):gsub("%s+$", "")
    end
end

function FingerprintCollector:collect_disk()
    local t = os_type()
    if t == "windows" then
        return shell_exec("wmic diskdrive get SerialNumber /value"):gsub("SerialNumber=", ""):gsub("^%s+", ""):gsub("%s+$", "")
    elseif t == "macos" then
        return shell_exec("diskutil info / 2>/dev/null | grep 'Volume UUID' | awk '{print $3}'")
    else
        return shell_exec("cat /sys/block/sda/device/serial 2>/dev/null || hdparm -I /dev/sda 2>/dev/null | grep 'Serial No' | awk -F: '{print $2}'"):gsub("^%s+", ""):gsub("%s+$", "")
    end
end

function FingerprintCollector:collect_mac()
    local t = os_type()
    if t == "windows" then
        local out = shell_exec("getmac /fo csv /nh | head -1")
        return out:match('^[^,]*'):gsub('"', ''):gsub("^%s+", ""):gsub("%s+$", "")
    elseif t == "macos" then
        return shell_exec("ifconfig en0 2>/dev/null | grep ether | awk '{print $2}'")
    else
        return shell_exec("cat /sys/class/net/$(ip route | grep default | awk '{print $5}' | head -1)/address 2>/dev/null"):gsub("^%s+", ""):gsub("%s+$", "")
    end
end

function FingerprintCollector:collect_bios()
    local t = os_type()
    if t == "windows" then
        return shell_exec("wmic bios get SerialNumber /value"):gsub("SerialNumber=", ""):gsub("^%s+", ""):gsub("%s+$", "")
    elseif t == "macos" then
        return shell_exec("ioreg -l 2>/dev/null | grep IOPlatformUUID | head -1")
    else
        return shell_exec("cat /sys/class/dmi/id/product_uuid 2>/dev/null || dmidecode -s system-uuid 2>/dev/null"):gsub("^%s+", ""):gsub("%s+$", "")
    end
end

function FingerprintCollector:detect_vm_extra()
    local t = os_type()
    if t == "linux" then
        local cgroup = shell_exec("cat /proc/self/cgroup 2>/dev/null | grep docker | head -1")
        if cgroup:match("docker") then
            local cid = cgroup:match("docker/([a-f0-9]+)")
            if cid then return "container:" .. cid end
        end
        local vm_uuid = shell_exec("dmidecode -s system-uuid 2>/dev/null")
        if vm_uuid and vm_uuid ~= "" and vm_uuid ~= "Not Settable" and vm_uuid ~= "Not Specified" then
            return "vm:" .. vm_uuid
        end
    elseif t == "windows" then
        local model = shell_exec("wmic computersystem get Model /value"):gsub("Model=", ""):gsub("^%s+", ""):gsub("%s+$", "")
        if model:match("Virtual") or model:match("VMware") or model:match("KVM") then
            local uuid_str = shell_exec("wmic csproduct get UUID /value"):gsub("UUID=", ""):gsub("^%s+", ""):gsub("%s+$", "")
            return "vm:" .. uuid_str
        end
    end
    return nil
end

function FingerprintCollector:collect(rsa_public_key)
    local cpu = self:collect_cpu()
    local mainboard = self:collect_mainboard()
    local disk = self:collect_disk()
    local mac = self:collect_mac()
    local bios = self:collect_bios()

    local hashed = {
        cpu = Crypto.sha256_hex(cpu),
        mainboard = Crypto.sha256_hex(mainboard),
        disk = Crypto.sha256_hex(disk),
        mac = Crypto.sha256_hex(mac),
        bios = Crypto.sha256_hex(bios),
    }

    local vm_extra = self:detect_vm_extra()
    local is_vm = vm_extra and 1 or 0

    local fp_input = hashed.cpu .. hashed.mainboard .. hashed.disk .. hashed.mac .. hashed.bios
    if vm_extra then fp_input = fp_input .. vm_extra end
    local fingerprint = Crypto.sha256_hex(fp_input)

    local detail_json = Json.encode(hashed)
    local encrypted_detail = Crypto.rsa_encrypt_oaep(detail_json, rsa_public_key)

    local t = os_type()
    return {
        fingerprint = fingerprint,
        encryptedDetail = encrypted_detail,
        isVm = is_vm,
        vmExtra = vm_extra or "",
        osType = t,
        osVersion = shell_exec("uname -r 2>/dev/null") or "",
        deviceName = shell_exec("hostname 2>/dev/null") or "",
        clientVersion = "jicek-sdk-lua-0.3.1",
    }
end

M.FingerprintCollector = FingerprintCollector

-- ==================== HTTP ====================

local Http = {}

-- 探测 HTTP 后端：LuaSocket 或 curl
local http_backend = nil
local function detect_http_backend()
    if http_backend then return http_backend end
    local ok, http = pcall(require, "socket.http")
    if ok and http then
        http_backend = "luasocket"
        Http._http = http
        local ok2, https = pcall(require, "ssl.https")
        if ok2 and https then Http._https = https end
        return http_backend
    end
    -- 检查 curl
    local ver = shell_exec("curl --version 2>/dev/null")
    if ver and ver:match("curl") then
        http_backend = "curl"
        return http_backend
    end
    return nil
end

function Http.request(method, url, body, headers, timeout)
    detect_http_backend()
    if http_backend == "luasocket" then
        local ltn12 = require("ltn12")
        local req_headers = headers or {}
        local req_body = body
        if req_body then
            req_headers["Content-Length"] = #req_body
        end
        local resp_chunks = {}
        local http_mod = url:match("^https://") and Http._https or Http._http
        if not http_mod then
            -- 没装 https 模块时回退 curl
            return Http._curl_request(method, url, body, headers, timeout)
        end
        local _, status = http_mod.request{
            url = url,
            method = method,
            headers = req_headers,
            source = req_body and ltn12.source.string(req_body) or nil,
            sink = ltn12.sink.table(resp_chunks),
            timeout = timeout or 10,
        }
        return table.concat(resp_chunks), status
    end
    -- curl 回退
    return Http._curl_request(method, url, body, headers, timeout)
end

function Http._curl_request(method, url, body, headers, timeout)
    local cmd_parts = { "curl -s -X " .. method .. " -m " .. tostring(timeout or 10) }
    if headers then
        for k, v in pairs(headers) do
            cmd_parts[#cmd_parts + 1] = string.format(" -H '%s: %s'", k, tostring(v):gsub("'", "'\\''"))
        end
    end
    if body then
        local tmp = os.tmpname()
        local f = io.open(tmp, "w")
        f:write(body)
        f:close()
        cmd_parts[#cmd_parts + 1] = string.format(" --data-binary @%s", tmp)
        local resp = shell_exec(table.concat(cmd_parts, " ") .. " '" .. url .. "'")
        os.remove(tmp)
        return resp, 200
    else
        return shell_exec(table.concat(cmd_parts, " ") .. " '" .. url .. "'"), 200
    end
end

M.Http = Http

-- ==================== 主类 JicekClient ====================

local JicekClient = {}
JicekClient.__index = JicekClient

--- 构造 JicekClient
-- @param config table: { server_url, app_key, sign_secret, rsa_public_key, timeout }
function M.new_client(config)
    local self = setmetatable({}, JicekClient)
    self.server_url = (config.server_url or ""):gsub("/+$", "")
    self.app_key = config.app_key
    self.sign_secret = config.sign_secret
    self.rsa_public_key = config.rsa_public_key
    self.timeout = config.timeout or 10
    self.fp_collector = M.new_fingerprint_collector()
    self.session_id = nil
    self.heartbeat_interval = 60
    self.heartbeat_thread = nil
    self.heartbeat_stop = false
    self.heartbeat_callback = nil
    self.fail_count = 0
    return self
end

function JicekClient:set_heartbeat_callback(cb)
    self.heartbeat_callback = cb
end

-- ---------- 核心接口 ----------

-- 卡密验证
function JicekClient:verify_card(card_key)
    local fp = self.fp_collector:collect(self.rsa_public_key)
    local card_cipher = Crypto.rsa_encrypt_oaep(card_key, self.rsa_public_key)
    local body = {
        fingerprint = fp.fingerprint,
        encryptedDetail = fp.encryptedDetail,
        cardCipher = card_cipher,
        deviceName = fp.deviceName,
        osType = fp.osType,
        osVersion = fp.osVersion,
        clientVersion = fp.clientVersion,
        isVm = fp.isVm,
        vmExtra = fp.vmExtra,
    }
    local data = self:_post("/api/sdk/card/verify", body, fp.fingerprint)
    self.session_id = data.sessionId
    return data
end

-- 单次心跳
function JicekClient:heartbeat()
    local fp = self.fp_collector:collect(self.rsa_public_key)
    local timestamp = now_ms()
    local nonce = uuid_v4()
    local body = {
        tenantId = 0,
        softwareId = 0,
        fingerprint = fp.fingerprint,
        timestamp = tonumber(timestamp),
        nonce = nonce,
    }
    local json_body = Json.encode(body)
    local headers = self:_build_signed_headers("POST", "/api/sdk/device/heartbeat", json_body, fp.fingerprint)
    headers["X-Heartbeat-Interval"] = tostring(self.heartbeat_interval)
    local url = self.server_url .. "/api/sdk/device/heartbeat"
    local resp, _ = Http.request("POST", url, json_body, headers, self.timeout)
    local data = self:_parse_response(resp)
    self.heartbeat_interval = tonumber(data.nextInterval or 60) or 60
    return data
end

-- 启动心跳（Lua 没有原生线程，使用协程 + 定时器由宿主调度）
-- 宿主需在主循环中调用 client:heartbeat_tick() 来推进
function JicekClient:start_heartbeat()
    self.heartbeat_stop = false
    self.heartbeat_last_ts = os.time()
end

function JicekClient:stop_heartbeat()
    self.heartbeat_stop = true
end

-- 心跳推进（宿主循环中调用，返回是否触发了一次心跳）
function JicekClient:heartbeat_tick()
    if self.heartbeat_stop then return false end
    local now = os.time()
    local interval = self.heartbeat_interval
    if self.fail_count > 0 then
        interval = math.min(2 ^ self.fail_count, 30)
    end
    if (now - (self.heartbeat_last_ts or 0)) >= interval then
        self.heartbeat_last_ts = now
        local ok, err = pcall(function() self:heartbeat() end)
        if ok then
            self.fail_count = 0
            if self.heartbeat_callback and self.heartbeat_callback.on_success then
                self.heartbeat_callback.on_success()
            end
        else
            self.fail_count = self.fail_count + 1
            if self.heartbeat_callback and self.heartbeat_callback.on_failure then
                self.heartbeat_callback.on_failure(err)
            end
            if self.fail_count >= 5 then
                if self.heartbeat_callback and self.heartbeat_callback.on_disconnect then
                    self.heartbeat_callback.on_disconnect()
                end
                self.heartbeat_stop = true
            end
        end
        return true
    end
    return false
end

-- 退出登录
function JicekClient:logout()
    if self.session_id then
        pcall(function()
            self:_post("/api/sdk/auth/logout", { sessionId = self.session_id }, nil)
        end)
    end
    self:stop_heartbeat()
    self.session_id = nil
end

-- ---------- 内部方法 ----------

function JicekClient:_post(path, body, device_id)
    local json_body = Json.encode(body)
    local fp = device_id
    if not fp then
        local fp_data = self.fp_collector:collect(self.rsa_public_key)
        fp = fp_data.fingerprint
    end
    local headers = self:_build_signed_headers("POST", path, json_body, fp)
    local url = self.server_url .. path
    local resp, _ = Http.request("POST", url, json_body, headers, self.timeout)
    return self:_parse_response(resp)
end

function JicekClient:_build_signed_headers(method, path, body, device_id)
    local timestamp = now_ms()
    local nonce = uuid_v4()
    local body_sha = body and Crypto.sha256_hex(body) or ""
    local payload = Crypto.build_sign_payload(method, path, timestamp, nonce, body_sha)
    local signature = Crypto.hmac_sha256_base64(payload, self.sign_secret)
    return {
        ["X-App-Key"] = self.app_key,
        ["X-Timestamp"] = timestamp,
        ["X-Nonce"] = nonce,
        ["X-Signature"] = signature,
        ["X-Device-Id"] = device_id,
        ["Content-Type"] = "application/json; charset=UTF-8",
    }
end

function JicekClient:_parse_response(resp_text)
    if not resp_text or resp_text == "" then
        error(M.new_exception(500, "空响应"), 0)
    end
    local root = Json.decode(resp_text)
    if not root then
        error(M.new_exception(500, "响应解析失败: " .. resp_text), 0)
    end
    local code = root.code or 0
    if code ~= 200 then
        error(M.new_exception(code, root.msg or "未知错误"), 0)
    end
    return root.data or root
end

M.JicekClient = JicekClient

-- ==================== 模块导出 ====================

-- 兼容 require("jicek") 和直接 dofile
return M
