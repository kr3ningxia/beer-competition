import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { BASE_URL } from '@/config'
import { clearSession, getRefreshToken, getToken, setSession } from '@/utils/auth'

const service = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
})

const refreshClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
})

const refreshPromises = {}

async function refreshSession(scope) {
  if (refreshPromises[scope]) {
    return refreshPromises[scope]
  }
  const refreshToken = getRefreshToken(scope)
  if (!refreshToken) {
    throw new Error('登录状态已失效')
  }
  refreshPromises[scope] = refreshClient
    .post('/api/public/auth/refresh', { refreshToken })
    .then((response) => {
      const payload = response.data
      if (payload.code !== 1 || !payload.data?.accessToken) {
        throw new Error(payload.msg || '登录状态已失效')
      }
      setSession(scope, payload.data)
      return payload.data
    })
    .finally(() => {
      delete refreshPromises[scope]
    })
  return refreshPromises[scope]
}

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
    if (response.config?.responseType === 'blob') {
      return response.data
    }
    const payload = response.data
    if (payload.code === 1) {
      return payload.data
    }
    ElMessage.error(payload.msg || '操作失败')
    return Promise.reject(new Error(payload.msg || '操作失败'))
  },
  async (error) => {
    const status = error.response?.status
    const config = error.config || {}
    const scope = config.authScope
    if (status === 401 && scope && !config._retry) {
      try {
        const session = await refreshSession(scope)
        config._retry = true
        config.headers = {
          ...(config.headers || {}),
          Authorization: `Bearer ${session.accessToken || session.token}`,
        }
        return service(config)
      } catch (refreshError) {
        clearSession(scope)
        router.push(scope === 'admin' ? '/admin/login' : '/portal/login')
      }
    } else if (status === 401 && scope) {
      clearSession(scope)
      router.push(scope === 'admin' ? '/admin/login' : '/portal/login')
    }
    ElMessage.error(error.response?.data?.msg || '请求失败，请稍后重试')
    return Promise.reject(error)
  }
)

export default service
