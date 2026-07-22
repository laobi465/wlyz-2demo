package com.jicek.sdk.model;

import java.util.List;

/**
 * 卡密验证结果
 * 作者: 极策k  日期: 2026-07-21
 */
public class VerifyResult {

    private boolean valid;
    private String sessionId;
    private String expireTime;       // ISO-8601
    private Integer remainCount;     // 次数卡：剩余次数；时长卡：null
    private List<String> features;   // 功能卡：功能列表
    private String msg;

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getExpireTime() { return expireTime; }
    public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
    public Integer getRemainCount() { return remainCount; }
    public void setRemainCount(Integer remainCount) { this.remainCount = remainCount; }
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
}
