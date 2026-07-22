// 极策k C# SDK 设备指纹采集
// 作者: 极策k  日期: 2026-07-21
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;

namespace Jicek.Sdk
{
    public class FingerprintResult
    {
        public string Fingerprint { get; set; } = "";
        public string EncryptedDetail { get; set; } = "";
        public int IsVm { get; set; }
        public string VmExtra { get; set; } = "";
        public string OsType { get; set; } = "";
        public string OsVersion { get; set; } = "";
        public string DeviceName { get; set; } = "";
        public string ClientVersion { get; set; } = "";
    }

    public class FingerprintCollector
    {
        public FingerprintResult Collect(string rsaPublicKey)
        {
            var cpu = CollectCpu();
            var mainboard = CollectMainboard();
            var disk = CollectDisk();
            var mac = CollectMac();
            var bios = CollectBios();

            var hashed = new Dictionary<string, string>
            {
                ["cpu"] = CryptoUtil.Sha256Hex(cpu),
                ["mainboard"] = CryptoUtil.Sha256Hex(mainboard),
                ["disk"] = CryptoUtil.Sha256Hex(disk),
                ["mac"] = CryptoUtil.Sha256Hex(mac),
                ["bios"] = CryptoUtil.Sha256Hex(bios),
            };

            var vmExtra = DetectVmExtra();
            var isVm = string.IsNullOrEmpty(vmExtra) ? 0 : 1;

            var fpInput = hashed["cpu"] + hashed["mainboard"] + hashed["disk"] + hashed["mac"] + hashed["bios"];
            if (!string.IsNullOrEmpty(vmExtra)) fpInput += vmExtra;
            var fingerprint = CryptoUtil.Sha256Hex(fpInput);

            var detailJson = JsonSerializer.Serialize(hashed);
            var encryptedDetail = CryptoUtil.RsaEncryptOaep(detailJson, rsaPublicKey);

            return new FingerprintResult
            {
                Fingerprint = fingerprint,
                EncryptedDetail = encryptedDetail,
                IsVm = isVm,
                VmExtra = vmExtra,
                OsType = OsType(),
                OsVersion = Environment.OSVersion.VersionString,
                DeviceName = Environment.MachineName,
                ClientVersion = "jicek-sdk-csharp-0.3.1",
            };
        }

        private static string Exec(string cmd)
        {
            try
            {
                var psi = new ProcessStartInfo
                {
                    FileName = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? "cmd" : "/bin/sh",
                    Arguments = (RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? "/c " : "-c ") + cmd,
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true,
                };
                using var p = Process.Start(psi);
                if (p == null) return "";
                var output = p.StandardOutput.ReadToEnd();
                p.WaitForExit(3000);
                return output.Trim();
            }
            catch { return ""; }
        }

        private static string CollectCpu()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return Exec("wmic cpu get ProcessorId /value").Replace("ProcessorId=", "").Trim();
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
                return Exec("sysctl -n machdep.cpu.brand_string");
            return Exec("cat /proc/cpuinfo | grep -i serial | head -1");
        }

        private static string CollectMainboard()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return Exec("wmic baseboard get SerialNumber /value").Replace("SerialNumber=", "").Trim();
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
                return Exec("ioreg -l | grep IOPlatformSerialNumber | head -1");
            return Exec("cat /sys/class/dmi/id/board_serial 2>/dev/null || dmidecode -s baseboard-serial-number 2>/dev/null");
        }

        private static string CollectDisk()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return Exec("wmic diskdrive get SerialNumber /value").Replace("SerialNumber=", "").Trim();
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
                return Exec("diskutil info / | grep 'Volume UUID' | awk '{print $3}'");
            return Exec("cat /sys/block/sda/device/serial 2>/dev/null || hdparm -I /dev/sda 2>/dev/null | grep 'Serial No' | awk -F: '{print $2}'").Trim();
        }

        private static string CollectMac()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                var out_ = Exec("getmac /fo csv /nh");
                var idx = out_.IndexOf(',');
                return idx > 0 ? out_.Substring(0, idx).Trim('"') : "";
            }
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
                return Exec("ifconfig en0 | grep ether | awk '{print $2}'");
            return Exec("cat /sys/class/net/$(ip route | grep default | awk '{print $5}' | head -1)/address").Trim();
        }

        private static string CollectBios()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
                return Exec("wmic bios get SerialNumber /value").Replace("SerialNumber=", "").Trim();
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
                return Exec("ioreg -l | grep IOPlatformUUID | head -1");
            return Exec("cat /sys/class/dmi/id/product_uuid 2>/dev/null || dmidecode -s system-uuid 2>/dev/null").Trim();
        }

        private static string DetectVmExtra()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
            {
                var cgroup = Exec("cat /proc/self/cgroup 2>/dev/null | grep docker | head -1");
                if (cgroup.Contains("docker"))
                {
                    var idx = cgroup.IndexOf("docker/");
                    if (idx >= 0) return "container:" + cgroup.Substring(idx + 7).Split('\n')[0];
                }
                var vmUuid = Exec("dmidecode -s system-uuid 2>/dev/null").Trim();
                if (!string.IsNullOrEmpty(vmUuid) && vmUuid != "Not Settable" && vmUuid != "Not Specified")
                    return "vm:" + vmUuid;
            }
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                var model = Exec("wmic computersystem get Model /value").Replace("Model=", "").Trim();
                if (model.Contains("Virtual") || model.Contains("VMware") || model.Contains("KVM"))
                {
                    var uuid = Exec("wmic csproduct get UUID /value").Replace("UUID=", "").Trim();
                    return "vm:" + uuid;
                }
            }
            return "";
        }

        private static string OsType()
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) return "windows";
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX)) return "macos";
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) return "linux";
            return Environment.OSVersion.Platform.ToString().ToLower();
        }
    }
}
