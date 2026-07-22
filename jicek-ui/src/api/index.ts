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
