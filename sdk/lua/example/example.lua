--[[
极策k Lua SDK 示例
作者: 极策k  日期: 2026-07-21

运行方式：
  lua example.lua
或
  luajit example.lua
]]

-- 假设 jicek.lua 在父目录
package.path = package.path .. ";../?.lua"
local jicek = require("jicek")

local function main()
    -- 配置（实际从配置文件/环境变量读取）
    local client = jicek.new_client({
        server_url = "https://api.jicek.example.com",
        app_key = "ak_demo_000000000001",
        sign_secret = "sk_demo_000000000001_secret",
        rsa_public_key = [[
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...（替换为真实公钥）
-----END PUBLIC KEY-----
]],
        timeout = 10,
    })

    -- 心跳回调
    client:set_heartbeat_callback({
        on_success = function()
            print("[心跳] 成功")
        end,
        on_failure = function(err)
            print("[心跳] 失败: " .. tostring(err))
        end,
        on_disconnect = function()
            print("[心跳] 已断开（连续 5 次失败）")
        end,
        on_device_banned = function()
            print("[心跳] 设备已封禁")
        end,
    })

    -- 1. 卡密验证
    local ok, result = pcall(function()
        return client:verify_card("JK-DEMO-XXXX-XXXX-XXXX")
    end)
    if not ok then
        print("卡密验证失败: " .. tostring(result))
        os.exit(1)
    end
    print("卡密验证成功: sessionId=" .. tostring(result.sessionId))
    print("  到期时间: " .. tostring(result.expireTime))
    print("  剩余次数: " .. tostring(result.remainCount))

    -- 2. 启动心跳（Lua 没有原生线程，需宿主循环推进）
    client:start_heartbeat()
    print("心跳已启动，主循环中调用 client:heartbeat_tick() 推进")

    -- 3. 主循环（示例：每秒推进一次心跳）
    for i = 1, 60 do
        client:heartbeat_tick()
        os.execute("sleep 1")  -- 实际项目中应使用宿主事件循环
    end

    -- 4. 退出
    client:logout()
    print("已退出")
end

main()
