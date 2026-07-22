package com.jicek.license.h5.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * H5 我的卡密详情 DTO
 * 作者: 极策k  日期: 2026-07-22
 */
@Data
public class H5CardDetailDTO {
    private Long cardKeyId;
    private String cardNoMasked;
    private Integer cardStatus;
    private String cardTypeName;
    private Integer cardType;
    private LocalDateTime expireTime;
    private LocalDateTime firstUseTime;
    private Integer remainingCount;
    private List<String> features;
    private String softwareName;
}
