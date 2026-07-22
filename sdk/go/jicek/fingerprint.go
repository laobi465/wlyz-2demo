// 极策k Go SDK 设备指纹采集
// 作者: 极策k  日期: 2026-07-21
package jicek

import (
	"encoding/json"
	"os"
	"os/exec"
	"runtime"
	"strings"
	"time"
)

// collectFingerprint 采集并计算最终指纹
func collectFingerprint(rsaPublicKey string) *FingerprintResult {
	cpu := collectCpu()
	mainboard := collectMainboard()
	disk := collectDisk()
	mac := collectMac()
	bios := collectBios()

	hashed := map[string]string{
		"cpu":       sha256Hex(cpu),
		"mainboard": sha256Hex(mainboard),
		"disk":      sha256Hex(disk),
		"mac":       sha256Hex(mac),
		"bios":      sha256Hex(bios),
	}

	vmExtra := detectVmExtra()
	isVm := 0
	if vmExtra != "" {
		isVm = 1
	}

	fpInput := hashed["cpu"] + hashed["mainboard"] + hashed["disk"] + hashed["mac"] + hashed["bios"]
	if vmExtra != "" {
		fpInput += vmExtra
	}
	fingerprint := sha256Hex(fpInput)

	detailJson, _ := json.Marshal(hashed)
	encryptedDetail, err := rsaEncryptOAEP(string(detailJson), rsaPublicKey)
	if err != nil {
		encryptedDetail = ""
	}

	return &FingerprintResult{
		Fingerprint:     fingerprint,
		EncryptedDetail: encryptedDetail,
		IsVm:            isVm,
		VmExtra:         vmExtra,
		OsType:          osType(),
		OsVersion:       runtime.GOOS + "-" + os.Getenv("OS_VERSION"),
		DeviceName:      hostname(),
		ClientVersion:   "jicek-sdk-go-0.3.1",
	}
}

func execCmd(cmd string) string {
	var shell []string
	if runtime.GOOS == "windows" {
		shell = []string{"cmd", "/c", cmd}
	} else {
		shell = []string{"/bin/sh", "-c", cmd}
	}
	c := exec.Command(shell[0], shell[1:]...)
	c.Env = append(os.Environ())
	out, err := c.CombinedOutput()
	if err != nil {
		return ""
	}
	return strings.TrimSpace(string(out))
}

func collectCpu() string {
	switch runtime.GOOS {
	case "windows":
		return strings.ReplaceAll(strings.ReplaceAll(execCmd("wmic cpu get ProcessorId /value"), "ProcessorId=", ""), "\r", "")
	case "darwin":
		return execCmd("sysctl -n machdep.cpu.brand_string")
	default:
		return strings.TrimSpace(strings.TrimPrefix(execCmd("cat /proc/cpuinfo | grep -i serial | head -1"), "serial\t:"))
	}
}

func collectMainboard() string {
	switch runtime.GOOS {
	case "windows":
		return strings.ReplaceAll(strings.ReplaceAll(execCmd("wmic baseboard get SerialNumber /value"), "SerialNumber=", ""), "\r", "")
	case "darwin":
		return execCmd("ioreg -l | grep IOPlatformSerialNumber | head -1")
	default:
		return execCmd("cat /sys/class/dmi/id/board_serial 2>/dev/null || dmidecode -s baseboard-serial-number 2>/dev/null")
	}
}

func collectDisk() string {
	switch runtime.GOOS {
	case "windows":
		return strings.ReplaceAll(strings.ReplaceAll(execCmd("wmic diskdrive get SerialNumber /value"), "SerialNumber=", ""), "\r", "")
	case "darwin":
		return execCmd("diskutil info / | grep 'Volume UUID' | awk '{print $3}'")
	default:
		return strings.TrimSpace(execCmd("cat /sys/block/sda/device/serial 2>/dev/null || hdparm -I /dev/sda 2>/dev/null | grep 'Serial No' | awk -F: '{print $2}'"))
	}
}

func collectMac() string {
	// 简单方案：从 /sys/class/net 读取（Linux）或 ifconfig（macOS）
	switch runtime.GOOS {
	case "windows":
		out := execCmd("getmac /fo csv /nh")
		if idx := strings.Index(out, ","); idx > 0 {
			return strings.Trim(out[:idx], "\"")
		}
		return ""
	case "darwin":
		return execCmd("ifconfig en0 | grep ether | awk '{print $2}'")
	default:
		return execCmd("cat /sys/class/net/$(ip route | grep default | awk '{print $5}' | head -1)/address")
	}
}

func collectBios() string {
	switch runtime.GOOS {
	case "windows":
		return strings.ReplaceAll(strings.ReplaceAll(execCmd("wmic bios get SerialNumber /value"), "SerialNumber=", ""), "\r", "")
	case "darwin":
		return execCmd("ioreg -l | grep IOPlatformUUID | head -1")
	default:
		return execCmd("cat /sys/class/dmi/id/product_uuid 2>/dev/null || dmidecode -s system-uuid 2>/dev/null")
	}
}

func detectVmExtra() string {
	if runtime.GOOS == "linux" {
		cgroup := execCmd("cat /proc/self/cgroup 2>/dev/null | grep docker | head -1")
		if strings.Contains(cgroup, "docker") {
			parts := strings.Split(cgroup, "docker/")
			if len(parts) > 1 {
				id := strings.Split(parts[1], "\n")[0]
				return "container:" + id
			}
		}
		vmUuid := strings.TrimSpace(execCmd("dmidecode -s system-uuid 2>/dev/null"))
		if vmUuid != "" && vmUuid != "Not Settable" && vmUuid != "Not Specified" {
			return "vm:" + vmUuid
		}
	}
	if runtime.GOOS == "windows" {
		model := strings.TrimSpace(strings.ReplaceAll(execCmd("wmic computersystem get Model /value"), "Model=", ""))
		if strings.Contains(model, "Virtual") || strings.Contains(model, "VMware") || strings.Contains(model, "KVM") {
			uuid := strings.TrimSpace(strings.ReplaceAll(execCmd("wmic csproduct get UUID /value"), "UUID=", ""))
			return "vm:" + uuid
		}
	}
	return ""
}

func osType() string {
	switch runtime.GOOS {
	case "windows":
		return "windows"
	case "darwin":
		return "macos"
	case "linux":
		return "linux"
	default:
		return runtime.GOOS
	}
}

func hostname() string {
	h, err := os.Hostname()
	if err != nil {
		return "unknown"
	}
	return h
}

// 防止 time 包未使用（占位）
var _ = time.Now
