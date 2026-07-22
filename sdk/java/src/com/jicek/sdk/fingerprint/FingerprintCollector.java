package com.jicek.sdk.fingerprint;

import com.jicek.sdk.JicekException;
import com.jicek.sdk.crypto.CryptoUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备指纹采集器（5 维 SHA-256 融合）
 * 作者: 极策k  日期: 2026-07-21
 *
 * 5 维：CPU / 主板 / 硬盘 / 网卡 MAC / BIOS UUID
 * VM 场景：追加 VM UUID 或容器 ID 作为补充维度
 *
 * 平台支持：Windows / Linux / macOS
 * 采集失败时该维度返回空字符串（不禁整个流程）
 */
public class FingerprintCollector {

    private static final String OS = System.getProperty("os.name", "").toLowerCase();

    /**
     * 采集并计算最终指纹
     *
     * @return 指纹结果（fingerprint + encryptedDetail + isVm + vmExtra + 元信息）
     */
    public FingerprintResult collect(String rsaPublicKey) {
        Map<String, String> dimensions = new LinkedHashMap<>();
        dimensions.put("cpu", collectCpu());
        dimensions.put("mainboard", collectMainboard());
        dimensions.put("disk", collectDisk());
        dimensions.put("mac", collectMac());
        dimensions.put("bios", collectBios());

        // 5 维单独 SHA-256
        Map<String, String> hashedDims = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : dimensions.entrySet()) {
            hashedDims.put(e.getKey(), CryptoUtil.sha256Hex(e.getValue()));
        }

        // VM/容器检测
        String vmExtra = detectVmExtra();
        boolean isVm = vmExtra != null && !vmExtra.isBlank();

        // 最终指纹 = SHA-256(5维哈希拼接 [+ vmExtra])
        StringBuilder fpInput = new StringBuilder();
        fpInput.append(hashedDims.get("cpu"));
        fpInput.append(hashedDims.get("mainboard"));
        fpInput.append(hashedDims.get("disk"));
        fpInput.append(hashedDims.get("mac"));
        fpInput.append(hashedDims.get("bios"));
        if (isVm) {
            fpInput.append(vmExtra);
        }
        String fingerprint = CryptoUtil.sha256Hex(fpInput.toString());

        // 5 维哈希 JSON → RSA 加密
        String detailJson = toJson(hashedDims);
        String encryptedDetail = CryptoUtil.rsaEncrypt(detailJson, rsaPublicKey);

        return new FingerprintResult(
                fingerprint,
                encryptedDetail,
                isVm ? 1 : 0,
                vmExtra,
                detectOsType(),
                detectOsVersion(),
                detectHostName(),
                detectClientVersion()
        );
    }

    /* ============ 5 维采集 ============ */

    private String collectCpu() {
        if (OS.contains("win")) {
            return execRead("wmic cpu get ProcessorId /value").replace("ProcessorId=", "").trim();
        } else if (OS.contains("mac")) {
            return execRead("sysctl -n machdep.cpu.brand_string");
        } else {
            return execRead("cat /proc/cpuinfo | grep -i serial | head -1")
                    .replaceAll("(?i).*serial\\s*:", "").trim();
        }
    }

    private String collectMainboard() {
        if (OS.contains("win")) {
            return execRead("wmic baseboard get SerialNumber /value")
                    .replace("SerialNumber=", "").trim();
        } else if (OS.contains("mac")) {
            return execRead("ioreg -l | grep IOPlatformSerialNumber | head -1")
                    .replaceAll(".*\"(.+)\".*", "$1");
        } else {
            return execRead("cat /sys/class/dmi/id/board_serial 2>/dev/null || dmidecode -s baseboard-serial-number 2>/dev/null");
        }
    }

    private String collectDisk() {
        if (OS.contains("win")) {
            return execRead("wmic diskdrive get SerialNumber /value")
                    .replace("SerialNumber=", "").trim();
        } else if (OS.contains("mac")) {
            return execRead("diskutil info / | grep 'Volume UUID' | awk '{print $3}'");
        } else {
            return execRead("cat /sys/block/sda/device/serial 2>/dev/null || hdparm -I /dev/sda 2>/dev/null | grep 'Serial No' | awk -F: '{print $2}'")
                    .trim();
        }
    }

    private String collectMac() {
        if (OS.contains("win")) {
            return execRead("getmac /fo csv /nh | head -1 | awk -F',' '{print $1}'")
                    .replace("\"", "").trim();
        } else if (OS.contains("mac")) {
            return execRead("ifconfig en0 | grep ether | awk '{print $2}'");
        } else {
            return execRead("cat /sys/class/net/$(ip route | grep default | awk '{print $5}' | head -1)/address")
                    .trim();
        }
    }

    private String collectBios() {
        if (OS.contains("win")) {
            return execRead("wmic bios get SerialNumber /value")
                    .replace("SerialNumber=", "").trim();
        } else if (OS.contains("mac")) {
            return execRead("ioreg -l | grep IOPlatformUUID | head -1")
                    .replaceAll(".*\"(.+)\".*", "$1");
        } else {
            return execRead("cat /sys/class/dmi/id/product_uuid 2>/dev/null || dmidecode -s system-uuid 2>/dev/null")
                    .trim();
        }
    }

    /* ============ VM/容器检测 ============ */

    private String detectVmExtra() {
        // Linux 容器检测
        if (OS.contains("linux")) {
            String cgroup = execRead("cat /proc/self/cgroup 2>/dev/null | grep docker | head -1");
            if (cgroup.contains("docker")) {
                return "container:" + cgroup.replaceAll(".*docker/([a-f0-9]+).*", "$1");
            }
            // VM UUID
            String vmUuid = execRead("dmidecode -s system-uuid 2>/dev/null").trim();
            if (!vmUuid.isEmpty() && !vmUuid.equals("Not Settable") && !vmUuid.equals("Not Specified")) {
                return "vm:" + vmUuid;
            }
        }
        // Windows VM 检测
        if (OS.contains("win")) {
            String model = execRead("wmic computersystem get Model /value")
                    .replace("Model=", "").trim();
            if (model.contains("Virtual") || model.contains("VMware") || model.contains("KVM")) {
                String uuid = execRead("wmic csproduct get UUID /value")
                        .replace("UUID=", "").trim();
                return "vm:" + uuid;
            }
        }
        return null;
    }

    /* ============ 元信息 ============ */

    private String detectOsType() {
        if (OS.contains("win")) return "windows";
        if (OS.contains("mac")) return "macos";
        if (OS.contains("linux")) return "linux";
        return OS;
    }

    private String detectOsVersion() {
        return System.getProperty("os.version", "");
    }

    private String detectHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String detectClientVersion() {
        return "jicek-sdk-java-0.3.1";
    }

    /* ============ 工具方法 ============ */

    private String execRead(String cmd) {
        try {
            String[] shell;
            if (OS.contains("win")) {
                shell = new String[]{"cmd", "/c", cmd};
            } else {
                shell = new String[]{"/bin/sh", "-c", cmd};
            }
            Process p = new ProcessBuilder(shell)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            p.waitFor();
            return output == null ? "" : output;
        } catch (Exception e) {
            return "";
        }
    }

    private String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
