/**
 * 极策k网络验证 Node.js SDK
 * 作者: 极策k  日期: 2026-07-21
 *
 * 零第三方依赖（仅用 Node.js 内置模块：crypto, https, child_process）
 * 支持 Node.js 18+
 *
 * 三件套：
 * 1. 卡密验证（verifyCard）
 * 2. 心跳保活（heartbeat / startHeartbeat）
 * 3. 设备绑定/换机（bindDevice / unbindDevice）
 */
'use strict';

const crypto = require('crypto');
const https = require('https');
const http = require('http');
const { URL } = require('url');
const { execSync } = require('child_process');
const os = require('os');

class JicekException extends Error {
  constructor(code, msg) {
    super(`[${code}] ${msg}`);
    this.name = 'JicekException';
    this.code = code;
    this.msg = msg;
  }
}

// ==================== 加密工具 ====================

function hmacSha256Base64(data, secret) {
  return crypto.createHmac('sha256', secret).update(data, 'utf8').digest('base64');
}

function sha256Hex(data) {
  return crypto.createHash('sha256').update(data, 'utf8').digest('hex');
}

function rsaEncryptOaep(plaintext, publicKeyB64) {
  const pubKeyDer = Buffer.from(publicKeyB64, 'base64');
  const publicKey = crypto.createPublicKey({ key: pubKeyDer, format: 'der', type: 'spki' });
  const cipher = crypto.publicEncrypt(
    {
      key: publicKey,
      padding: crypto.constants.RSA_PKCS1_OAEP_PADDING,
      oaepHash: 'sha256',
    },
    Buffer.from(plaintext, 'utf8')
  );
  return cipher.toString('base64');
}

function buildSignPayload(method, path, timestamp, nonce, bodySha256) {
  return `${method}\n${path}\n${timestamp}\n${nonce}\n${bodySha256 || ''}`;
}

// ==================== 设备指纹采集 ====================

class FingerprintCollector {
  static exec(cmd) {
    try {
      return execSync(cmd, { timeout: 3000, encoding: 'utf8' }).trim();
    } catch (_) {
      return '';
    }
  }

  collect(rsaPublicKey) {
    const cpu = this._collectCpu();
    const mainboard = this._collectMainboard();
    const disk = this._collectDisk();
    const mac = this._collectMac();
    const bios = this._collectBios();

    const hashed = {
      cpu: sha256Hex(cpu),
      mainboard: sha256Hex(mainboard),
      disk: sha256Hex(disk),
      mac: sha256Hex(mac),
      bios: sha256Hex(bios),
    };

    const vmExtra = this._detectVmExtra();
    const isVm = vmExtra ? 1 : 0;

    let fpInput = hashed.cpu + hashed.mainboard + hashed.disk + hashed.mac + hashed.bios;
    if (vmExtra) fpInput += vmExtra;
    const fingerprint = sha256Hex(fpInput);

    const detailJson = JSON.stringify(hashed);
    const encryptedDetail = rsaEncryptOaep(detailJson, rsaPublicKey);

    return {
      fingerprint,
      encryptedDetail,
      isVm,
      vmExtra: vmExtra || '',
      osType: this._osType(),
      osVersion: os.release(),
      deviceName: os.hostname(),
      clientVersion: 'jicek-sdk-nodejs-0.3.1',
    };
  }

  _collectCpu() {
    if (process.platform === 'win32') {
      return FingerprintCollector.exec('wmic cpu get ProcessorId /value').replace('ProcessorId=', '').trim();
    }
    if (process.platform === 'darwin') {
      return FingerprintCollector.exec('sysctl -n machdep.cpu.brand_string');
    }
    return FingerprintCollector.exec('cat /proc/cpuinfo | grep -i serial | head -1').replace(/.*serial\s*:?/i, '').trim();
  }

  _collectMainboard() {
    if (process.platform === 'win32') {
      return FingerprintCollector.exec('wmic baseboard get SerialNumber /value').replace('SerialNumber=', '').trim();
    }
    if (process.platform === 'darwin') {
      return FingerprintCollector.exec('ioreg -l | grep IOPlatformSerialNumber | head -1');
    }
    return FingerprintCollector.exec('cat /sys/class/dmi/id/board_serial 2>/dev/null || dmidecode -s baseboard-serial-number 2>/dev/null');
  }

  _collectDisk() {
    if (process.platform === 'win32') {
      return FingerprintCollector.exec('wmic diskdrive get SerialNumber /value').replace('SerialNumber=', '').trim();
    }
    if (process.platform === 'darwin') {
      return FingerprintCollector.exec('diskutil info / | grep "Volume UUID" | awk \'{print $3}\'');
    }
    return FingerprintCollector.exec('cat /sys/block/sda/device/serial 2>/dev/null || hdparm -I /dev/sda 2>/dev/null | grep "Serial No" | awk -F: \'{print $2}\'').trim();
  }

  _collectMac() {
    const ifaces = os.networkInterfaces();
    for (const name of Object.keys(ifaces)) {
      if (name.toLowerCase().includes('docker') || name.toLowerCase().includes('veth')) continue;
      for (const iface of ifaces[name]) {
        if (!iface.internal && iface.mac && iface.mac !== '00:00:00:00:00:00') {
          return iface.mac;
        }
      }
    }
    return '';
  }

  _collectBios() {
    if (process.platform === 'win32') {
      return FingerprintCollector.exec('wmic bios get SerialNumber /value').replace('SerialNumber=', '').trim();
    }
    if (process.platform === 'darwin') {
      return FingerprintCollector.exec('ioreg -l | grep IOPlatformUUID | head -1');
    }
    return FingerprintCollector.exec('cat /sys/class/dmi/id/product_uuid 2>/dev/null || dmidecode -s system-uuid 2>/dev/null').trim();
  }

  _detectVmExtra() {
    if (process.platform === 'linux') {
      const cgroup = FingerprintCollector.exec('cat /proc/self/cgroup 2>/dev/null | grep docker | head -1');
      if (cgroup.includes('docker')) {
        const m = cgroup.match(/docker\/([a-f0-9]+)/);
        if (m) return `container:${m[1]}`;
      }
      const vmUuid = FingerprintCollector.exec('dmidecode -s system-uuid 2>/dev/null').trim();
      if (vmUuid && !['Not Settable', 'Not Specified'].includes(vmUuid)) {
        return `vm:${vmUuid}`;
      }
    }
    if (process.platform === 'win32') {
      const model = FingerprintCollector.exec('wmic computersystem get Model /value').replace('Model=', '').trim();
      if (/Virtual|VMware|KVM/.test(model)) {
        const uuid = FingerprintCollector.exec('wmic csproduct get UUID /value').replace('UUID=', '').trim();
        return `vm:${uuid}`;
      }
    }
    return null;
  }

  _osType() {
    if (process.platform === 'win32') return 'windows';
    if (process.platform === 'darwin') return 'macos';
    if (process.platform === 'linux') return 'linux';
    return process.platform;
  }
}

// ==================== 主类 ====================

class JicekClient {
  constructor(options) {
    if (!options.serverUrl) throw new Error('serverUrl 必填');
    if (!options.appKey) throw new Error('appKey 必填');
    if (!options.signSecret) throw new Error('signSecret 必填');
    if (!options.rsaPublicKey) throw new Error('rsaPublicKey 必填');

    this.serverUrl = options.serverUrl.replace(/\/$/, '');
    this.appKey = options.appKey;
    this.signSecret = options.signSecret;
    this.rsaPublicKey = options.rsaPublicKey;
    this.timeout = options.timeout || 10000;
    this._fpCollector = new FingerprintCollector();
    this._sessionId = null;
    this._heartbeatInterval = 60;
    this._heartbeatTimer = null;
    this._heartbeatCallback = null;
    this._failCount = 0;
  }

  setHeartbeatCallback(cb) {
    this._heartbeatCallback = cb;
  }

  // ---------- 核心接口 ----------

  async verifyCard(cardKey) {
    const fp = this._fpCollector.collect(this.rsaPublicKey);
    const cardCipher = rsaEncryptOaep(cardKey, this.rsaPublicKey);
    const body = {
      fingerprint: fp.fingerprint,
      encryptedDetail: fp.encryptedDetail,
      cardCipher,
      deviceName: fp.deviceName,
      osType: fp.osType,
      osVersion: fp.osVersion,
      clientVersion: fp.clientVersion,
      isVm: fp.isVm,
      vmExtra: fp.vmExtra,
    };
    const data = await this._post('/api/sdk/card/verify', body);
    this._sessionId = data.sessionId || null;
    return data;
  }

  async heartbeat() {
    const fp = this._fpCollector.collect(this.rsaPublicKey);
    const timestamp = Date.now();
    const nonce = crypto.randomUUID().replace(/-/g, '');
    const body = {
      tenantId: 0,
      softwareId: 0,
      fingerprint: fp.fingerprint,
      timestamp,
      nonce,
    };
    const jsonBody = JSON.stringify(body);
    const headers = this._buildSignedHeaders('POST', '/api/sdk/device/heartbeat', jsonBody, fp.fingerprint);
    headers['X-Sign-Secret'] = this.signSecret;
    headers['X-Heartbeat-Interval'] = String(this._heartbeatInterval);
    const resp = await this._httpRequest('POST', '/api/sdk/device/heartbeat', jsonBody, headers);
    const data = this._parseResponse(resp);
    this._heartbeatInterval = data.nextInterval || 60;
    return data;
  }

  startHeartbeat() {
    if (this._heartbeatTimer) return;
    this._scheduleNext(this._heartbeatInterval);
  }

  stopHeartbeat() {
    if (this._heartbeatTimer) {
      clearTimeout(this._heartbeatTimer);
      this._heartbeatTimer = null;
    }
  }

  async logout() {
    if (this._sessionId) {
      try {
        await this._post('/api/sdk/auth/logout', { sessionId: this._sessionId });
      } catch (_) { /* 退出失败不抛 */ }
    }
    this.stopHeartbeat();
    this._sessionId = null;
  }

  // ---------- 内部方法 ----------

  _scheduleNext(delaySec) {
    this._heartbeatTimer = setTimeout(async () => {
      try {
        const result = await this.heartbeat();
        this._failCount = 0;
        if (this._heartbeatCallback) this._heartbeatCallback.onSuccess(result);
        this._scheduleNext(this._heartbeatInterval);
      } catch (e) {
        this._failCount++;
        if (e.code === 3002) {
          if (this._heartbeatCallback) this._heartbeatCallback.onDeviceBanned();
          this.stopHeartbeat();
          return;
        }
        if (this._heartbeatCallback) this._heartbeatCallback.onFailure(e);
        if (this._failCount >= 5) {
          if (this._heartbeatCallback) this._heartbeatCallback.onDisconnect();
          this.stopHeartbeat();
          return;
        }
        const backoff = Math.min(Math.pow(2, this._failCount), 30);
        this._scheduleNext(backoff);
      }
    }, delaySec * 1000);
    if (this._heartbeatTimer.unref) this._heartbeatTimer.unref();
  }

  async _post(path, body) {
    const jsonBody = JSON.stringify(body);
    const fp = this._fpCollector.collect(this.rsaPublicKey);
    const headers = this._buildSignedHeaders('POST', path, jsonBody, fp.fingerprint);
    const resp = await this._httpRequest('POST', path, jsonBody, headers);
    return this._parseResponse(resp);
  }

  _buildSignedHeaders(method, path, body, deviceId) {
    const timestamp = String(Date.now());
    const nonce = crypto.randomUUID().replace(/-/g, '');
    const bodySha = body ? sha256Hex(body) : '';
    const payload = buildSignPayload(method, path, timestamp, nonce, bodySha);
    const signature = hmacSha256Base64(payload, this.signSecret);
    return {
      'X-App-Key': this.appKey,
      'X-Timestamp': timestamp,
      'X-Nonce': nonce,
      'X-Signature': signature,
      'X-Device-Id': deviceId,
      'Content-Type': 'application/json; charset=UTF-8',
    };
  }

  _httpRequest(method, path, body, headers) {
    return new Promise((resolve, reject) => {
      const url = new URL(this.serverUrl + path);
      const lib = url.protocol === 'https:' ? https : http;
      const opts = {
        method,
        hostname: url.hostname,
        port: url.port,
        path: url.pathname + url.search,
        headers,
        timeout: this.timeout,
      };
      const req = lib.request(opts, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          if (res.statusCode !== 200) {
            reject(new JicekException(res.statusCode, `HTTP 请求失败: ${data}`));
            return;
          }
          resolve(data);
        });
      });
      req.on('error', (e) => reject(new JicekException(500, `网络异常: ${e.message}`)));
      req.on('timeout', () => {
        req.destroy(new JicekException(500, '请求超时'));
      });
      if (body) req.write(body);
      req.end();
    });
  }

  _parseResponse(respText) {
    let root;
    try {
      root = JSON.parse(respText);
    } catch (e) {
      throw new JicekException(500, `响应解析失败: ${e.message}`);
    }
    const code = root.code || 0;
    if (code !== 200) {
      throw new JicekException(code, root.msg || '未知错误');
    }
    return root.data || root;
  }
}

module.exports = { JicekClient, JicekException };
