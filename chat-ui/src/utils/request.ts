import axios from 'axios'
import type { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import { safeParse } from '@/utils/json'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
  /** 使用 json-bigint 解析响应，防止 Long 精度丢失 */
  transformResponse: [
    (data: string) => {
      try {
        return safeParse(data) as Record<string, unknown>
      } catch {
        return data
      }
    },
  ],
})

// Request interceptor — attach Bearer token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

// Response interceptor — unwrap ApiResponse, handle 401
request.interceptors.response.use(
  (response) => {
    const res = response.data as { code: string | number; message?: string; data?: unknown }
    // json-bigint 将所有数字转为字符串，使用 == 宽松比较
    // eslint-disable-next-line eqeqeq
    if (res.code == 200) {
      return res.data
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.resetToken()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
