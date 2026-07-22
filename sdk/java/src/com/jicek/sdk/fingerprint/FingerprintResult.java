package com.jicek.sdk.fingerprint;

/**
 * 指纹采集结果
 * 作者: 极策k  日期: 2026-07-21
 */
public class FingerprintResult {

    private final String fingerprint;
    private final String encryptedDetail;
    private final int isVm;
    private final String vmExtra;
    private final String osType;
    private final String osVersion;
    private final String deviceName;
    private final String clientVersion;

    public FingerprintResult(String fingerprint, String encryptedDetail,
                              int isVm, String vmExtra,
                              String osType, String osVersion,
                              String deviceName, String clientVersion) {
        this.fingerprint = fingerprint;
        this.encryptedDetail = encryptedDetail;
        this.isVm = isVm;
        this.vmExtra = vmExtra;
        this.osType = osType;
        this.osVersion = osVersion;
        this.deviceName = deviceName;
        this.clientVersion = clientVersion;
    }

    public String getFingerprint() { return fingerprint; }
    public String getEncryptedDetail() { return encryptedDetail; }
    public int getIsVm() { return isVm; }
    public String getVmExtra() { return vmExtra; }
    public String getOsType() { return osType; }
    public String getOsVersion() { return osVersion; }
    public String getDeviceName() { return deviceName; }
    public String getClientVersion() { return clientVersion; }
}
