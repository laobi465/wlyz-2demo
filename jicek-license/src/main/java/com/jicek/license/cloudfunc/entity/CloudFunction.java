package com.jicek.license.cloudfunc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 云函数实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全说明（铁律 04/06/13）：
 * 1. code 为用户编写的 Lua 源代码，最长 64KB（CF_CODE_MAX_BYTES）
 * 2. timeoutMs 上限 CF_MAX_TIMEOUT_MS（30s），防恶意长占线程
 * 3. 每次执行必须落 jicek_cloud_function_log 审计表
 * 4. enabled=0 时禁止调用（CF_DISABLED 错误码）
 *
 * runtime：当前仅支持 'lua'（LuaJ 沙箱执行）
 * enabled：0 禁用 1 启用
 */
@Data
@TableName("jicek_cloud_function")
public class CloudFunction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long softwareId;

    /** 函数名（同一软件下唯一） */
    private String name;

    private String description;

    /** Lua 源代码 */
    private String code;

    /** 运行时（当前仅 lua） */
    private String runtime;

    /** 执行超时（毫秒） */
    private Integer timeoutMs;

    /** 内存上限（KB，提示用） */
    private Integer memoryLimitKb;

    /** 输入大小上限（KB） */
    private Integer maxInputKb;

    /** 输出大小上限（KB） */
    private Integer maxOutputKb;

    /** 0 禁用 1 启用 */
    private Integer enabled;

    /** 版本号（每次保存自增） */
    private Integer version;

    /** 累计调用次数 */
    private Long invokeCount;

    private LocalDateTime lastInvokeTime;

    private String lastInvokeIp;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
