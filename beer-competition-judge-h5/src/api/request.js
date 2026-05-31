import axios from 'axios'
import router from '@/router'
import { BASE_URL } from '@/config'
import { clearSession, getToken } from '@/utils/auth'

const service = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload.code === 1) {
      return payload.data
    }
    return Promise.reject(new Error(payload.msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      clearSession()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default service
