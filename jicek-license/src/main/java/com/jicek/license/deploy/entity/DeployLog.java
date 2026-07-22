package com.jicek.license.deploy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部署审计日志实体
 * 作者: 极策k  日期: 2026-07-22
 *
 * 安全说明（铁律 04/06/13）：
 * 1. 审计表，仅 INSERT + SELECT，禁 UPDATE/DELETE
 * 2. status 不可逆：0进行中 → 1成功 / 2失败 / 3已回滚
 * 3. errorMessage 截断至 4KB（DEPLOY_ERROR_MSG_MAX_BYTES）
 *
 * status：0 进行中 1 成功 2 失败 3 已回滚
 * triggerSource：webhook(自动) / manual(手动)
 */
@Data
@TableName("jicek_deploy_log")
public class DeployLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** webhook(自动) / manual(手动) */
    private String triggerSource;

    /** Git commit SHA */
    private String commitHash;

    /** 分支名，默认 main */
    private String branch;

    /** 0进行中 1成功 2失败 3已回滚 */
    private Integer status;

    /** 总耗时毫秒 */
    private Integer durationMs;

    /** 操作者 IP */
    private String operatorIp;

    /** 错误信息（截断至 4KB） */
    private String errorMessage;

    private LocalDateTime createTime;
}
