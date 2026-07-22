"""
极策k网络验证 Python SDK
作者: 极策k  日期: 2026-07-21

零第三方依赖（除 cryptography 用于 RSA）。
支持 Python 3.9+，跨平台（Windows/Linux/macOS）。

三件套：
1. 卡密验证（verify_card）
2. 心跳保活（heartbeat / start_heartbeat）
3. 设备绑定/换机（bind_device / unbind_device）
"""
from __future__ import annotations

import hashlib
import hmac
import json
import os
import platform
import subprocess
import threading
import time
import uuid
from typing import Any, Dict, Optional
from urllib import request as urlreq
from urllib.error import HTTPError, URLError


class JicekException(Exception):
    """极策k SDK 异常"""

    def __init__(self, code: int, msg: str):
        super().__init__(f"[{code}] {msg}")
        self.code = code
        self.msg = msg


# ==================== 加密工具 ====================

def hmac_sha256_base64(data: str, secret: str) -> str:
    """HMAC-SHA256 签名，返回 Base64"""
    import base64
    sign = hmac.new(secret.encode("utf-8"), data.encode("utf-8"), hashlib.sha256).digest()
    return base64.b64encode(sign).decode("ascii")


def sha256_hex(data: str) -> str:
    """SHA-256，返回 64 字符小写十六进制"""
    return hashlib.sha256(data.encode("utf-8")).hexdigest()


def rsa_encrypt_oaep(plaintext: str, public_key_b64: str) -> str:
    """RSA-2048-OAEP 加密（卡密传输），返回 Base64"""
    import base64
    from cryptography.hazmat.primitives import serialization, hashes
    from cryptography.hazmat.primitives.asymmetric import padding

    pub_bytes = base64.b64decode(public_key_b64)
    public_key = serialization.load_der_public_key(pub_bytes)
    cipher = public_key.encrypt(
        plaintext.encode("utf-8"),
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None,
        ),
    )
    return base64.b64encode(cipher).decode("ascii")


def build_sign_payload(method: str, path: str, timestamp: str,
                        nonce: str, body_sha256: str) -> str:
    """构造签名原文：METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_SHA256"""
    return f"{method}\n{path}\n{timestamp}\n{nonce}\n{body_sha256 or ''}"


# ==================== 设备指纹采集 ====================

class FingerprintCollector:
    """5 维设备指纹采集（CPU/主板/硬盘/网卡/BIOS）"""

    @staticmethod
    def _exec(cmd: str) -> str:
        """执行 shell 命令并返回输出（失败返回空字符串）"""
        is_win = platform.system() == "Windows"
        shell = ["cmd", "/c", cmd] if is_win else ["/bin/sh", "-c", cmd]
        try:
            r = subprocess.run(shell, capture_output=True, text=True, timeout=3)
            return (r.stdout or "").strip()
        except Exception:
            return ""

    def collect(self, rsa_public_key: str) -> Dict[str, Any]:
        """采集并返回指纹结果"""
        # 5 维原始数据
        cpu = self._collect_cpu()
        mainboard = self._collect_mainboard()
        disk = self._collect_disk()
        mac = self._collect_mac()
        bios = self._collect_bios()

        # 5 维 SHA-256
        hashed = {
            "cpu": sha256_hex(cpu),
            "mainboard": sha256_hex(mainboard),
            "disk": sha256_hex(disk),
            "mac": sha256_hex(mac),
            "bios": sha256_hex(bios),
        }

        # VM/容器检测
        vm_extra = self._detect_vm_extra()
        is_vm = 1 if vm_extra else 0

        # 最终指纹
        fp_input = hashed["cpu"] + hashed["mainboard"] + hashed["disk"] + hashed["mac"] + hashed["bios"]
        if vm_extra:
            fp_input += vm_extra
        fingerprint = sha256_hex(fp_input)

        # 5 维哈希 JSON → RSA 加密
        detail_json = json.dumps(hashed, separators=(",", ":"))
        encrypted_detail = rsa_encrypt_oaep(detail_json, rsa_public_key)

        return {
            "fingerprint": fingerprint,
            "encryptedDetail": encrypted_detail,
            "isVm": is_vm,
            "vmExtra": vm_extra or "",
            "osType": self._os_type(),
            "osVersion": platform.version(),
            "deviceName": platform.node(),
            "clientVersion": "jicek-sdk-python-0.3.1",
        }

    def _collect_cpu(self) -> str:
        if platform.system() == "Windows":
            return self._exec("wmic cpu get ProcessorId /value").replace("ProcessorId=", "").strip()
        if platform.system() == "Darwin":
            return self._exec("sysctl -n machdep.cpu.brand_string")
        return self._exec("cat /proc/cpuinfo | grep -i serial | head -1").replace("serial\t:", "").strip()

    def _collect_mainboard(self) -> str:
        if platform.system() == "Windows":
            return self._exec("wmic baseboard get SerialNumber /value").replace("SerialNumber=", "").strip()
        if platform.system() == "Darwin":
            return self._exec("ioreg -l | grep IOPlatformSerialNumber | head -1")
        return self._exec("cat /sys/class/dmi/id/board_serial 2>/dev/null || dmidecode -s baseboard-serial-number 2>/dev/null")

    def _collect_disk(self) -> str:
        if platform.system() == "Windows":
            return self._exec("wmic diskdrive get SerialNumber /value").replace("SerialNumber=", "").strip()
        if platform.system() == "Darwin":
            return self._exec("diskutil info / | grep 'Volume UUID' | awk '{print $3}'")
        return self._exec("cat /sys/block/sda/device/serial 2>/dev/null || hdparm -I /dev/sda 2>/dev/null | grep 'Serial No' | awk -F: '{print $2}'").strip()

    def _collect_mac(self) -> str:
        if platform.system() == "Windows":
            return self._exec("getmac /fo csv /nh | head -1").split(",")[0].strip('"')
        if platform.system() == "Darwin":
            return self._exec("ifconfig en0 | grep ether | awk '{print $2}'")
        return self._exec("cat /sys/class/net/$(ip route | grep default | awk '{print $5}' | head -1)/address").strip()

    def _collect_bios(self) -> str:
        if platform.system() == "Windows":
            return self._exec("wmic bios get SerialNumber /value").replace("SerialNumber=", "").strip()
        if platform.system() == "Darwin":
            return self._exec("ioreg -l | grep IOPlatformUUID | head -1")
        return self._exec("cat /sys/class/dmi/id/product_uuid 2>/dev/null || dmidecode -s system-uuid 2>/dev/null").strip()

    def _detect_vm_extra(self) -> Optional[str]:
        if platform.system() == "Linux":
            cgroup = self._exec("cat /proc/self/cgroup 2>/dev/null | grep docker | head -1")
            if "docker" in cgroup:
                import re
                m = re.search(r"docker/([a-f0-9]+)", cgroup)
                if m:
                    return f"container:{m.group(1)}"
            vm_uuid = self._exec("dmidecode -s system-uuid 2>/dev/null").strip()
            if vm_uuid and vm_uuid not in ("Not Settable", "Not Specified"):
                return f"vm:{vm_uuid}"
        if platform.system() == "Windows":
            model = self._exec("wmic computersystem get Model /value").replace("Model=", "").strip()
            if any(k in model for k in ("Virtual", "VMware", "KVM")):
                uuid_str = self._exec("wmic csproduct get UUID /value").replace("UUID=", "").strip()
                return f"vm:{uuid_str}"
        return None

    def _os_type(self) -> str:
        s = platform.system()
        if s == "Windows":
            return "windows"
        if s == "Darwin":
            return "macos"
        if s == "Linux":
            return "linux"
        return s.lower()


# ==================== 主类 ====================

class JicekClient:
    """极策k 客户端主类"""

    def __init__(self, server_url: str, app_key: str, sign_secret: str,
                 rsa_public_key: str, timeout: int = 10):
        self.server_url = server_url.rstrip("/")
        self.app_key = app_key
        self.sign_secret = sign_secret
        self.rsa_public_key = rsa_public_key
        self.timeout = timeout
        self._fp_collector = FingerprintCollector()
        self._session_id: Optional[str] = None
        self._heartbeat_interval = 60
        self._heartbeat_thread: Optional[threading.Thread] = None
        self._heartbeat_stop = threading.Event()
        self._heartbeat_callback = None
        self._fail_count = 0

    def set_heartbeat_callback(self, callback):
        """设置心跳回调：on_success(result), on_failure(e), on_disconnect(), on_device_banned()"""
        self._heartbeat_callback = callback

    # ---------- 核心接口 ----------

    def verify_card(self, card_key: str) -> Dict[str, Any]:
        """卡密验证"""
        fp = self._fp_collector.collect(self.rsa_public_key)
        card_cipher = rsa_encrypt_oaep(card_key, self.rsa_public_key)
        body = {
            "fingerprint": fp["fingerprint"],
            "encryptedDetail": fp["encryptedDetail"],
            "cardCipher": card_cipher,
            "deviceName": fp["deviceName"],
            "osType": fp["osType"],
            "osVersion": fp["osVersion"],
            "clientVersion": fp["clientVersion"],
            "isVm": fp["isVm"],
            "vmExtra": fp["vmExtra"],
        }
        data = self._post("/api/sdk/card/verify", body)
        self._session_id = data.get("sessionId")
        return data

    def heartbeat(self) -> Dict[str, Any]:
        """单次心跳"""
        fp = self._fp_collector.collect(self.rsa_public_key)
        timestamp = str(int(time.time() * 1000))
        nonce = uuid.uuid4().hex
        body = {
            "tenantId": 0,
            "softwareId": 0,
            "fingerprint": fp["fingerprint"],
            "timestamp": int(timestamp),
            "nonce": nonce,
        }
        json_body = json.dumps(body, separators=(",", ":"))
        headers = self._build_signed_headers("POST", "/api/sdk/device/heartbeat", json_body, fp["fingerprint"])
        headers["X-Sign-Secret"] = self.sign_secret
        headers["X-Heartbeat-Interval"] = str(self._heartbeat_interval)
        resp = self._http_request("POST", "/api/sdk/device/heartbeat", json_body, headers)
        data = self._parse_response(resp)
        self._heartbeat_interval = int(data.get("nextInterval", 60))
        return data

    def start_heartbeat(self):
        """启动后台心跳守护线程"""
        if self._heartbeat_thread and self._heartbeat_thread.is_alive():
            return
        self._heartbeat_stop.clear()
        self._heartbeat_thread = threading.Thread(
            target=self._heartbeat_loop, name="jicek-heartbeat", daemon=True)
        self._heartbeat_thread.start()

    def stop_heartbeat(self):
        """停止心跳"""
        self._heartbeat_stop.set()
        if self._heartbeat_thread:
            self._heartbeat_thread.join(timeout=2)

    def logout(self):
        """退出登录"""
        if self._session_id:
            try:
                self._post("/api/sdk/auth/logout", {"sessionId": self._session_id})
            except Exception:
                pass
        self.stop_heartbeat()
        self._session_id = None

    # ---------- 内部方法 ----------

    def _heartbeat_loop(self):
        """心跳循环：动态间隔 + 指数退避"""
        while not self._heartbeat_stop.is_set():
            try:
                result = self.heartbeat()
                self._fail_count = 0
                if self._heartbeat_callback:
                    self._heartbeat_callback.on_success(result)
                interval = self._heartbeat_interval
            except JicekException as e:
                self._fail_count += 1
                if e.code == 3002:
                    if self._heartbeat_callback:
                        self._heartbeat_callback.on_device_banned()
                    return
                if self._heartbeat_callback:
                    self._heartbeat_callback.on_failure(e)
                if self._fail_count >= 5:
                    if self._heartbeat_callback:
                        self._heartbeat_callback.on_disconnect()
                    return
                interval = min(2 ** self._fail_count, 30)
            # 等待间隔（可被打断）
            self._heartbeat_stop.wait(interval)

    def _post(self, path: str, body: Dict[str, Any]) -> Dict[str, Any]:
        """POST 请求（自动签名）"""
        json_body = json.dumps(body, separators=(",", ":"))
        fp = self._fp_collector.collect(self.rsa_public_key)
        headers = self._build_signed_headers("POST", path, json_body, fp["fingerprint"])
        resp = self._http_request("POST", path, json_body, headers)
        return self._parse_response(resp)

    def _build_signed_headers(self, method: str, path: str,
                               body: str, device_id: str) -> Dict[str, str]:
        """构造带签名的请求头"""
        timestamp = str(int(time.time() * 1000))
        nonce = uuid.uuid4().hex
        body_sha = sha256_hex(body) if body else ""
        payload = build_sign_payload(method, path, timestamp, nonce, body_sha)
        signature = hmac_sha256_base64(payload, self.sign_secret)
        return {
            "X-App-Key": self.app_key,
            "X-Timestamp": timestamp,
            "X-Nonce": nonce,
            "X-Signature": signature,
            "X-Device-Id": device_id,
            "Content-Type": "application/json; charset=UTF-8",
        }

    def _http_request(self, method: str, path: str,
                       body: str, headers: Dict[str, str]) -> str:
        """HTTP 请求"""
        url = self.server_url + path
        data = body.encode("utf-8") if body else None
        req = urlreq.Request(url, data=data, method=method, headers=headers)
        try:
            with urlreq.urlopen(req, timeout=self.timeout) as resp:
                return resp.read().decode("utf-8")
        except HTTPError as e:
            body_text = e.read().decode("utf-8", errors="replace")
            raise JicekException(e.code, f"HTTP 请求失败: {body_text}")
        except URLError as e:
            raise JicekException(500, f"网络异常: {e.reason}")

    @staticmethod
    def _parse_response(resp_text: str) -> Dict[str, Any]:
        """解析响应"""
        try:
            root = json.loads(resp_text)
        except Exception as e:
            raise JicekException(500, f"响应解析失败: {e}")
        code = root.get("code", 0)
        if code != 200:
            raise JicekException(code, root.get("msg", "未知错误"))
        return root.get("data") or root
