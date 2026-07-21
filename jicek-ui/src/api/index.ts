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
