/**
 * 极策k API 客户端
 * 作者: 极策k  日期: 2026-07-21
 *
 * v0.7.0 增强：
 *  - 请求拦截器自动注入 Authorization: Bearer {token}（从 localStorage 读取）
 *  - 响应拦截器 401/9001/9002 自动跳转登录页并清理 token
 */
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/** localStorage key：JWT token */
export const TOKEN_KEY = 'jicek_token'
/** localStorage key：当前用户信息 */
export const USER_KEY = 'jicek_user'

const request: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截：注入 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：业务码处理 + 401 跳登录
request.interceptors.response.use(
  (response) => {
    const { code, msg, data } = response.data || {}
    if (code === 200) {
      return data
    }
    // 鉴权失败：token 缺失/无效/过期/角色不匹配
    if (code === 9001 || code === 9002 || code === 9003 || code === 401) {
      clearAuthAndRedirect()
      return Promise.reject(new Error(msg || '登录已过期'))
    }
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.msg || error.message || '网络异常'
    if (status === 401) {
      clearAuthAndRedirect()
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

/** 清理 token 并跳转登录页（防重复跳转） */
function clearAuthAndRedirect() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  // 避免在登录页重复跳转
  if (!window.location.pathname.startsWith('/login')) {
    ElMessage.warning('登录已过期，请重新登录')
    window.location.href = '/login'
  }
}

export const api = {
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig) {
    return request.get<any, T>(url, { ...config, params })
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig) {
    return request.post<any, T>(url, data, config)
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig) {
    return request.put<any, T>(url, data, config)
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig) {
    return request.delete<any, T>(url, config)
  }
}

export default request
