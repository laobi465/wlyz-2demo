/**
 * 极策k API 接口定义
 * 作者: 极策k  日期: 2026-07-21
 */
import { api } from './request'

/* ============ 控制台 ============ */
export const dashboardApi = {
  summary: (tenantId: number) => api.get('/api/dev/dashboard/summary', { tenantId })
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
