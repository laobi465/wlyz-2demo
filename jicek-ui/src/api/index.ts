/**
 * 极策k API 接口定义
 * 作者: 极策k  日期: 2026-07-21
 */
import { api } from './request'

/* ============ 鉴权 ============ */
export const authApi = {
  // 开发者登录（需提交 tenantId + username + password）
  devLogin: (data: { tenantId: number; username: string; password: string }) =>
    api.post('/api/auth/dev/login', data),
  // 管理员登录（无需 tenantId）
  adminLogin: (data: { username: string; password: string }) =>
    api.post('/api/auth/admin/login', data),
  // 获取当前登录用户信息
  me: () => api.get('/api/auth/me'),
  // 修改密码
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    api.post('/api/auth/change-password', data)
}

/* ============ 控制台 ============ */
export const dashboardApi = {
  summary: (tenantId: number) => api.get('/api/dev/dashboard/summary', { tenantId })
}

/* ============ 软件管理（v0.8.0，tenantId 由后端从 AuthContext 获取） ============ */
export const softwareApi = {
  // 分页查询（name 模糊匹配，enabled 状态过滤）
  page: (params: { current?: number; size?: number; name?: string; enabled?: number }) =>
    api.get('/api/dev/software/page', {
      current: params.current || 1,
      size: params.size || 20,
      name: params.name,
      enabled: params.enabled
    }),
  // 详情（signSecret 脱敏，无 rsaPrivateKey）
  get: (id: number) => api.get(`/api/dev/software/${id}`),
  // 创建（返回含 signSecret + rsaPrivateKey 明文，仅此一次）
  create: (data: {
    name: string
    version?: string
    minVersion?: string
    heartbeatInterval?: number
    maxConcurrent?: number
    enabled?: number
  }) => api.post('/api/dev/software', data),
  // 更新（仅非敏感字段，id 必填）
  update: (data: {
    id: number
    name: string
    version?: string
    minVersion?: string
    heartbeatInterval?: number
    maxConcurrent?: number
    enabled?: number
  }) => api.put('/api/dev/software', data),
  // 删除（关联卡类/设备/云函数时拒绝）
  delete: (id: number) => api.delete(`/api/dev/software/${id}`),
  // 轮换签名密钥（返回新明文，仅此一次）
  regenerateSignSecret: (id: number) =>
    api.post(`/api/dev/software/${id}/regenerate-sign-secret`),
  // 轮换 RSA 密钥对（返回新公钥 + 私钥明文，仅此一次）
  regenerateRsaKey: (id: number) =>
    api.post(`/api/dev/software/${id}/regenerate-rsa-key`)
}

/* ============ 卡密 ============ */
export const cardKeyApi = {
  generate: (data: any) => api.post('/api/dev/card/generate', data),
  query: (tenantId: number, cardNo: string) =>
    api.get('/api/dev/card/query', { tenantId, cardNo }),
  ban: (tenantId: number, cardKeyId: number, reason?: string) =>
    api.post('/api/dev/card/ban', null, { params: { tenantId, cardKeyId, reason } }),
  refund: (tenantId: number, cardKeyId: number) =>
    api.post('/api/dev/card/refund', null, { params: { tenantId, cardKeyId } })
}

/* ============ 卡类 ============ */
export const cardTypeApi = {
  save: (data: any) => api.post('/api/dev/card-type', data),
  page: (params: any) => api.get('/api/dev/card-type/page', params),
  get: (id: number) => api.get(`/api/dev/card-type/${id}`),
  delete: (id: number) => api.delete(`/api/dev/card-type/${id}`)
}

/* ============ 设备 ============ */
export const deviceApi = {
  page: (params: any) =>
    api.get('/api/dev/device/page', {
      tenantId: params.tenantId,
      softwareId: params.softwareId,
      status: params.status,
      onlineStatus: params.onlineStatus,
      page: params.current || 1,
      size: params.size || 20
    }),
  get: (tenantId: number, deviceId: number) =>
    api.get(`/api/dev/device/${tenantId}/${deviceId}`),
  ban: (tenantId: number, deviceId: number) =>
    api.post('/api/dev/device/ban', null, { params: { tenantId, deviceId } }),
  unban: (tenantId: number, deviceId: number) =>
    api.post('/api/dev/device/unban', null, { params: { tenantId, deviceId } })
}

/* ============ 支付 ============ */
export const payApi = {
  getConfig: (tenantId: number) => api.get(`/api/dev/pay/config/${tenantId}`),
  saveConfig: (data: any) => api.post('/api/dev/pay/config', data),
  createPay: (data: any) => api.post('/api/dev/pay/create', data),
  pageOrder: (params: any) => api.get('/api/dev/pay/order/page', params),
  refund: (outTradeNo: string, reason?: string) =>
    api.post('/api/dev/pay/refund', null, { params: { outTradeNo, reason } })
}

/* ============ 代理 ============ */
export const agentApi = {
  create: (data: any) => api.post('/api/dev/agent', data),
  update: (data: any) => api.put('/api/dev/agent', data),
  page: (params: any) =>
    api.get('/api/dev/agent/page', {
      tenantId: params.tenantId,
      parentId: params.parentId,
      status: params.status,
      page: params.current || 1,
      size: params.size || 20
    }),
  tree: (tenantId: number, rootParentId: number = 0) =>
    api.get('/api/dev/agent/tree', { tenantId, rootParentId }),
  get: (tenantId: number, agentId: number) =>
    api.get(`/api/dev/agent/${tenantId}/${agentId}`),
  ban: (tenantId: number, agentId: number) =>
    api.post('/api/dev/agent/ban', null, { params: { tenantId, agentId } }),
  unban: (tenantId: number, agentId: number) =>
    api.post('/api/dev/agent/unban', null, { params: { tenantId, agentId } }),
  recharge: (tenantId: number, agentId: number, amount: number, remark?: string) =>
    api.post('/api/dev/agent/recharge', null, {
      params: { tenantId, agentId, amount, remark }
    }),
  commissionPage: (params: any) =>
    api.get('/api/dev/agent/commission/page', {
      tenantId: params.tenantId,
      agentId: params.agentId,
      sourceAgentId: params.sourceAgentId,
      type: params.type,
      status: params.status,
      page: params.current || 1,
      size: params.size || 20
    })
}

/* ============ 提现 ============ */
export const withdrawApi = {
  apply: (data: any) => api.post('/api/dev/withdraw/apply', data),
  audit: (data: any) => api.post('/api/dev/withdraw/audit', data),
  page: (params: any) =>
    api.get('/api/dev/withdraw/page', {
      tenantId: params.tenantId,
      agentId: params.agentId,
      status: params.status,
      page: params.current || 1,
      size: params.size || 20
    }),
  get: (tenantId: number, withdrawId: number) =>
    api.get(`/api/dev/withdraw/${tenantId}/${withdrawId}`),
  pendingAmount: (tenantId: number, agentId: number) =>
    api.get('/api/dev/withdraw/pending-amount', { tenantId, agentId })
}

/* ============ 云函数 ============ */
export const cloudFuncApi = {
  save: (data: any) => api.post('/api/dev/cloud-func', data),
  page: (params: any) =>
    api.get('/api/dev/cloud-func/page', {
      tenantId: params.tenantId,
      softwareId: params.softwareId,
      name: params.name,
      enabled: params.enabled,
      current: params.current || 1,
      size: params.size || 20
    }),
  get: (tenantId: number, functionId: number) =>
    api.get(`/api/dev/cloud-func/${tenantId}/${functionId}`),
  delete: (tenantId: number, functionId: number) =>
    api.delete(`/api/dev/cloud-func/${tenantId}/${functionId}`),
  toggleEnabled: (tenantId: number, functionId: number, enabled: number) =>
    api.post('/api/dev/cloud-func/toggle-enabled', null, {
      params: { tenantId, functionId, enabled }
    }),
  invoke: (data: any) => api.post('/api/dev/cloud-func/invoke', data),
  logPage: (params: any) =>
    api.get('/api/dev/cloud-func/log/page', {
      tenantId: params.tenantId,
      functionId: params.functionId,
      softwareId: params.softwareId,
      status: params.status,
      invokeSource: params.invokeSource,
      current: params.current || 1,
      size: params.size || 20
    })
}

/* ============ 数据统计 ============ */
export const statsApi = {
  verifyTrend: (params: any) =>
    api.get('/api/dev/stats/verify-trend', {
      tenantId: params.tenantId,
      softwareId: params.softwareId,
      granularity: params.granularity || 'day',
      days: params.days
    }),
  deviceHeatmap: (params: any) =>
    api.get('/api/dev/stats/device-heatmap', {
      tenantId: params.tenantId,
      softwareId: params.softwareId,
      days: params.days
    }),
  income: (params: any) =>
    api.get('/api/dev/stats/income', {
      tenantId: params.tenantId,
      softwareId: params.softwareId,
      dimension: params.dimension || 'channel',
      days: params.days
    }),
  antiCrack: (params: any) =>
    api.get('/api/dev/stats/anti-crack', {
      tenantId: params.tenantId,
      softwareId: params.softwareId,
      days: params.days
    })
}

/* ============ 部署管理 ============ */
export const deployApi = {
  status: () => api.get('/api/dev/deploy/status'),
  manual: (data: any) => api.post('/api/dev/deploy/manual', data),
  logPage: (params: any) =>
    api.get('/api/dev/deploy/log/page', {
      tenantId: params.tenantId,
      status: params.status,
      triggerSource: params.triggerSource,
      current: params.current || 1,
      size: params.size || 20
    })
}

/* ============ 工单管理（开发者后台，单向：开发者→管理员） ============ */
export const ticketApi = {
  // 开发者向管理员提交工单
  submit: (data: any, devUserId: number) =>
    api.post('/api/dev/ticket/submit', data, { params: { devUserId } }),
  // 查询自己提交给管理员的工单
  submitPage: (params: any) =>
    api.get('/api/dev/ticket/submit/page', {
      tenantId: params.tenantId,
      devUserId: params.devUserId,
      category: params.category,
      status: params.status,
      current: params.current || 1,
      size: params.size || 20
    }),
  // 提交工单详情（含回复列表）
  submitDetail: (tenantId: number, ticketId: number) =>
    api.get(`/api/dev/ticket/submit/${tenantId}/${ticketId}`),
  // 开发者补充回复
  submitReply: (data: any, tenantId: number, devUserId: number) =>
    api.post('/api/dev/ticket/submit/reply', data, { params: { tenantId, devUserId } })
}

/* ============ 公告管理（v0.10.0，开发者按软件/版本下发） ============ */
export const announcementApi = {
  // 分页查询
  page: (params: {
    current?: number
    size?: number
    softwareId?: number
    status?: number
    type?: number
    title?: string
  }) =>
    api.get('/api/dev/announcement/page', {
      current: params.current || 1,
      size: params.size || 20,
      softwareId: params.softwareId,
      status: params.status,
      type: params.type,
      title: params.title
    }),
  // 详情
  get: (id: number) => api.get(`/api/dev/announcement/${id}`),
  // 创建（初始为草稿）
  create: (data: {
    softwareId: number
    title: string
    content: string
    type: number
    minVersion?: string
    maxVersion?: string
    sortOrder?: number
    pinned?: number
  }) => api.post('/api/dev/announcement', data),
  // 编辑（仅草稿可编辑）
  update: (data: {
    id: number
    softwareId: number
    title: string
    content: string
    type: number
    minVersion?: string
    maxVersion?: string
    sortOrder?: number
    pinned?: number
  }) => api.put('/api/dev/announcement', data),
  // 删除
  delete: (id: number) => api.delete(`/api/dev/announcement/${id}`),
  // 发布（草稿 → 已发布）
  publish: (id: number) => api.post(`/api/dev/announcement/${id}/publish`),
  // 下线（已发布 → 已下线）
  offline: (id: number) => api.post(`/api/dev/announcement/${id}/offline`)
}

/* ============ 更新包管理（v0.11.0，多格式 exe/sh/win/lua，SDK 检查更新） ============ */
export const updatePackageApi = {
  // 上传文件（multipart）
  upload: (file: File, onProgress?: (percent: number) => void) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/dev/update-package/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      }
    })
  },
  // 分页查询
  page: (params: {
    current?: number
    size?: number
    softwareId?: number
    status?: number
    channel?: number
    version?: string
  }) =>
    api.get('/api/dev/update-package/page', {
      current: params.current || 1,
      size: params.size || 20,
      softwareId: params.softwareId,
      status: params.status,
      channel: params.channel,
      version: params.version
    }),
  // 详情
  get: (id: number) => api.get(`/api/dev/update-package/${id}`),
  // 创建（草稿）
  create: (data: {
    softwareId: number
    version: string
    channel: number
    filePath: string
    fileName: string
    fileSize: number
    fileSha256: string
    fileType: string
    releaseNotes?: string
    minClientVersion?: string
    maxClientVersion?: string
    forceUpdate?: number
  }) => api.post('/api/dev/update-package', data),
  // 编辑（仅草稿，仅改 releaseNotes/版本范围/强制更新）
  update: (data: {
    id: number
    softwareId: number
    version: string
    channel: number
    filePath: string
    fileName: string
    fileSize: number
    fileSha256: string
    fileType: string
    releaseNotes?: string
    minClientVersion?: string
    maxClientVersion?: string
    forceUpdate?: number
  }) => api.put('/api/dev/update-package', data),
  // 删除（同时删物理文件）
  delete: (id: number) => api.delete(`/api/dev/update-package/${id}`),
  // 发布（草稿 → 已发布）
  publish: (id: number) => api.post(`/api/dev/update-package/${id}/publish`),
  // 下线（已发布 → 已下线）
  offline: (id: number) => api.post(`/api/dev/update-package/${id}/offline`)
}
