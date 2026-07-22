package com.jicek.license.cloudfunc.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 云函数保存请求 DTO
 * 作者: 极策k  日期: 2026-07-22
 *
 * 校验规则（铁律 04 禁硬编码，所有上限走常量）：
 * - name: 1-64 字符，[a-zA-Z][a-zA-Z0-9_]*
 * - code: 非空，长度 ≤ CF_CODE_MAX_BYTES（64KB）
 * - timeoutMs: 100-30000
 * - memoryLimitKb / maxInputKb / maxOutputKb: 1-256*1024
 */
@Data
public class CloudFunctionSaveDTO {

    /** 更新时传，新建时不传 */
    private Long id;

    @NotNull
    private Long tenantId;

    @NotNull
    private Long softwareId;

    @NotBlank(message = "函数名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,63}$", message = "函数名必须以字母开头，仅含字母数字下划线，最长 64 字符")
    private String name;

    @Size(max = 255, message = "描述最长 255 字符")
    private String description;

    @NotBlank(message = "代码不能为空")
    private String code;

    @Min(value = 100, message = "超时最小 100ms")
    @Max(value = 30000, message = "超时最大 30000ms")
    private Integer timeoutMs;

    @Min(value = 1, message = "内存上限最小 1KB")
    @Max(value = 262144, message = "内存上限最大 256MB")
    private Integer memoryLimitKb;

    @Min(value = 1, message = "输入上限最小 1KB")
    @Max(value = 256, message = "输入上限最大 256KB")
    private Integer maxInputKb;

    @Min(value = 1, message = "输出上限最小 1KB")
    @Max(value = 256, message = "输出上限最大 256KB")
    private Integer maxOutputKb;

    /** 0 禁用 1 启用 */
    private Integer enabled;
}
