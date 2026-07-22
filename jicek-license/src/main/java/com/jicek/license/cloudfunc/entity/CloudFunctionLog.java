package com.jicek.license.cloudfunc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 云函数执行日志实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全说明（铁律 09/13）：
 * 1. 审计表，仅 INSERT，禁 UPDATE/DELETE
 * 2. status 见 JicekConstants.CF_STATUS_*（0-6）
 * 3. errorMessage 截断至 CF_ERROR_MSG_MAX_BYTES（4KB）
 * 4. input/output 不入库（防敏感信息泄漏），仅存大小（字节）
 *
 * invokeSource：dev（开发者测试）/ sdk（客户端调用）
 */
@Data
@TableName("jicek_cloud_function_log")
public class CloudFunctionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long functionId;

    /** 冗余：函数名（便于审计查询，防函数改名后无法追溯） */
    private String functionName;

    /** 冗余：所属软件 */
    private Long softwareId;

    /** 调用来源：dev / sdk */
    private String invokeSource;

    private String callerIp;

    /** 输入字节数 */
    private Integer inputSize;

    /** 输出字节数 */
    private Integer outputSize;

    /** 实际执行耗时（毫秒） */
    private Integer durationMs;

    /** 0成功 1编译失败 2运行时错误 3超时 4内存超限 5输入超限 6输出超限 */
    private Integer status;

    /** 失败时记录错误信息（截断 4KB） */
    private String errorMessage;

    private LocalDateTime createTime;
}
