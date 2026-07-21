/**
 * 极策k API 客户端
 * 作者: 极策k  日期: 2026-07-21
 */
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const request: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截
request.interceptors.request.use(
  (config) => {
    // TODO: 从 Pinia 获取 token 注入
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截
request.interceptors.response.use(
  (response) => {
    const { code, msg, data } = response.data || {}
    if (code === 200) {
      return data
    }
    ElMessage.error(msg || '请求失败')
    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    const msg = error.response?.data?.msg || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

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
