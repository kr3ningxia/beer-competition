import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { BASE_URL } from '@/config'
import { clearSession, getToken } from '@/utils/auth'

const service = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
})

service.interceptors.request.use((config) => {
  const scope = config.authScope
  if (scope) {
    const token = getToken(scope)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload.code === 1) {
      return payload.data
    }
    ElMessage.error(payload.msg || '操作失败')
    return Promise.reject(new Error(payload.msg || '操作失败'))
  },
  (error) => {
    const status = error.response?.status
    const scope = error.config?.authScope
    if (status === 401 && scope) {
      clearSession(scope)
      router.push(scope === 'admin' ? '/admin/login' : '/portal/login')
    }
    ElMessage.error(error.response?.data?.msg || '请求失败，请稍后重试')
    return Promise.reject(error)
  }
)

export default service
