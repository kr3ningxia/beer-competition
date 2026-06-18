import axios from 'axios'
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

let toastTimer = null
let refreshPromise = null

function showRequestError(message) {
  if (typeof document === 'undefined') return
  const text = String(message || '操作失败，请稍后重试')
  let toast = document.querySelector('.h5-request-toast')
  if (!toast) {
    toast = document.createElement('div')
    toast.className = 'h5-request-toast'
    toast.style.cssText = [
      'position:fixed',
      'left:50%',
      'bottom:86px',
      'z-index:9999',
      'max-width:calc(100vw - 40px)',
      'transform:translateX(-50%)',
      'border-radius:8px',
      'padding:10px 14px',
      'color:#fff',
      'background:rgba(24,34,47,.92)',
      'font-size:14px',
      'font-weight:700',
      'line-height:1.45',
      'box-shadow:0 12px 30px rgba(0,0,0,.24)',
    ].join(';')
    document.body.appendChild(toast)
  }
  toast.textContent = text
  toast.style.display = 'block'
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.style.display = 'none'
  }, 2600)
}

function extractErrorMessage(error) {
  return error.response?.data?.msg || error.message || '操作失败，请稍后重试'
}

async function refreshSession() {
  if (refreshPromise) {
    return refreshPromise
  }
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('登录状态已失效')
  }
  refreshPromise = refreshClient
    .post('/api/public/auth/refresh', { refreshToken })
    .then((response) => {
      const payload = response.data
      if (payload.code !== 1 || !payload.data?.accessToken) {
        throw new Error(payload.msg || '登录状态已失效')
      }
      setSession(payload.data)
      return payload.data
    })
    .finally(() => {
      refreshPromise = null
    })
  return refreshPromise
}

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
    const error = new Error(payload.msg || '请求失败')
    showRequestError(error.message)
    return Promise.reject(error)
  },
  async (error) => {
    const config = error.config || {}
    if (error.response?.status === 401 && !config._retry) {
      try {
        const session = await refreshSession()
        config._retry = true
        config.headers = {
          ...(config.headers || {}),
          Authorization: `Bearer ${session.accessToken || session.token}`,
        }
        return service(config)
      } catch (refreshError) {
        clearSession()
        router.push('/login')
      }
    } else if (error.response?.status === 401) {
      clearSession()
      router.push('/login')
    }
    showRequestError(extractErrorMessage(error))
    return Promise.reject(error)
  }
)

export default service
