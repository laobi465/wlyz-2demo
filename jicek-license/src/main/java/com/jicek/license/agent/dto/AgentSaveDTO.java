package com.jicek.license.agent.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 代理创建/更新 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 校验规则：
 * - username 4-32 字符
 * - password 至少 6 位（创建时必填，更新时可空表示不改）
 * - commissionRate 0-100
 * - maxSubLevel 0-10
 */
@Data
public class AgentSaveDTO {

    /** 更新时传入，创建时为 null */
    private Long id;

    @NotNull
    private Long tenantId;

    /** 上级代理 ID，0 为顶级代理 */
    private Long parentId = 0L;

    @NotBlank
    @Size(min = 4, max = 32)
    private String username;

    /** 创建必填，更新可空 */
    private String password;

    private String realName;

    @Size(max = 128)
    private String contact;

    /** 分润比例 0-100 */
    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal commissionRate;

    /** 允许发展的下级层级数，0 不可发展 */
    @NotNull
    @DecimalMin("0")
    @DecimalMax("10")
    private Integer maxSubLevel;

    private String remark;
}
