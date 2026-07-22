/**
 * 极策k 管理员端 API 客户端（v0.15.0）
 * 作者: 极策k  日期: 2026-07-22
 *
 * 独立的 adminAxios 实例，注入 jicek_admin_token（与开发者 jicek_token 隔离）。
 * 401/9001/9002/9003 自动清理管理员 token 并跳转 /admin/login。
 */
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/** localStorage key：管理员 JWT token（独立于开发者 jicek_token） */
export const ADMIN_TOKEN_KEY = 'jicek_admin_token'
/** localStorage key：管理员用户信息 */
export const ADMIN_USER_KEY = 'jicek_admin_user'

const adminRequest: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截：注入管理员 token
adminRequest.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(ADMIN_TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：业务码处理 + 401 跳管理员登录页
adminRequest.interceptors.response.use(
  (response) => {
    const { code, msg, data } = response.data || {}
    if (code === 200) {
      return data
    }
    // 鉴权失败：token 缺失/无效/过期/角色不匹配
    if (code === 9001 || code === 9002 || code === 9003 || code === 401) {
      clearAdminAuthAndRedirect()
      return Promise.reject(new Error(msg || '登录已过期'))
    }
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.msg || error.message || '网络异常'
    if (status === 401) {
      clearAdminAuthAndRedirect()
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

/** 清理管理员 token 并跳转管理员登录页（防重复跳转） */
function clearAdminAuthAndRedirect() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_USER_KEY)
  if (!window.location.pathname.startsWith('/admin/login')) {
    ElMessage.warning('登录已过期，请重新登录')
    window.location.href = '/admin/login'
  }
}

const adminHttp = {
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig) {
    return adminRequest.get<any, T>(url, { ...config, params })
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig) {
    return adminRequest.post<any, T>(url, data, config)
  }
}

/* ============ 管理员鉴权 ============ */
export const adminAuthApi = {
  // 管理员登录（无需 tenantId）
  login: (data: { username: string; password: string }) =>
    adminHttp.post('/api/auth/admin/login', data),
  // 获取当前登录用户信息
  me: () => adminHttp.get('/api/auth/me')
}

/* ============ 管理员端 API（v0.15.0） ============ */
export const adminApi = {
  /* 工单管理 */
  ticketPage: (params: {
    current?: number
    size?: number
    tenantId?: number
    category?: number
    status?: number
  }) =>
    adminHttp.get('/api/admin/ticket/page', {
      current: params.current || 1,
      size: params.size || 20,
      tenantId: params.tenantId,
      category: params.category,
      status: params.status
    }),
  ticketGet: (id: number) => adminHttp.get(`/api/admin/ticket/${id}`),
  ticketReply: (id: number, content: string) =>
    adminHttp.post(`/api/admin/ticket/${id}/reply`, { content }),
  ticketClose: (id: number) => adminHttp.post(`/api/admin/ticket/${id}/close`),

  /* 开发者管理 */
  devUserPage: (params: {
    current?: number
    size?: number
    tenantId?: number
    username?: string
    status?: number
  }) =>
    adminHttp.get('/api/admin/dev-user/page', {
      current: params.current || 1,
      size: params.size || 20,
      tenantId: params.tenantId,
      username: params.username,
      status: params.status
    }),
  devUserGet: (id: number) => adminHttp.get(`/api/admin/dev-user/${id}`),
  devUserBan: (id: number) => adminHttp.post(`/api/admin/dev-user/${id}/ban`),
  devUserUnban: (id: number) => adminHttp.post(`/api/admin/dev-user/${id}/unban`),
  devUserResetPassword: (data: { id: number; newPassword: string }) =>
    adminHttp.post('/api/admin/dev-user/reset-password', data)
}

