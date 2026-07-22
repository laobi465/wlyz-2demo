package com.jicek.sdk.model;

/**
 * 心跳结果
 * 作者: 极策k  日期: 2026-07-21
 */
public class HeartbeatResult {

    private int nextInterval;   // 下次心跳间隔（秒）
    private long serverTime;    // 服务器时间戳（毫秒）

    public int getNextInterval() { return nextInterval; }
    public void setNextInterval(int nextInterval) { this.nextInterval = nextInterval; }
    public long getServerTime() { return serverTime; }
    public void setServerTime(long serverTime) { this.serverTime = serverTime; }
}
