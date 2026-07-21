package com.jicek.license.card.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 卡密批量生成请求 DTO
 * 作者: 极策k  日期: 2026-07-21
 */
@Data
public class CardKeyGenRequestDTO {

    @NotNull
    private Long tenantId;

    @NotNull
    private Long softwareId;

    @NotNull
    private Long cardTypeId;

    /** 生成数量，上限 1000 */
    @Min(1)
    private Integer quantity = 1;

    /** 卡密前缀（如 JC-） */
    private String prefix = "";

    /** 字符集：0大小写+数字 1纯数字 2自定义 */
    private Integer charset = 0;

    /** 自定义字符集（charset=2 时使用） */
    private String customCharset;

    /** 卡密长度（不含前缀） */
    @Min(8)
    private Integer length = 24;

    /** 备注 */
    private String remark;
}
