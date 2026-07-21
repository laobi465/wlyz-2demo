package com.jicek.license.card.dto;

import lombok.Data;

import java.util.List;

/**
 * 卡密批量生成响应 DTO
 * 作者: 极策k  日期: 2026-07-21
 *
 * 安全说明：
 * - plainCards 为明文卡密列表，仅在生成时返回一次
 * - 后续查询接口不会再返回明文
 * - 客户端必须立即保存到本地
 */
@Data
public class CardKeyGenResponseDTO {

    /** 本次生成的卡号列表（脱敏，仅显示前后各 4 位） */
    private List<String> maskedCardNos;

    /** 明文卡密列表（仅本次返回，请立即保存） */
    private List<String> plainCards;

    /** 生成数量 */
    private Integer count;

    /** 生成时间戳 */
    private Long timestamp;
}
