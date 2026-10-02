function normalizeApiBaseUrl(value) {
  const fallback = import.meta.env.PROD ? '' : 'http://localhost:8084'
  const normalized = (value ?? fallback).trim().replace(/\/+$/, '')
  return normalized.endsWith('/api') ? normalized.slice(0, -4) : normalized
}

export const BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL)

// 赛事详情属于厂商端，生产环境与评委端使用不同的部署前缀。
export const PORTAL_BASE_URL = (
  import.meta.env.VITE_PORTAL_BASE_URL || (import.meta.env.PROD ? '/console' : 'http://localhost:5173')
).trim().replace(/\/+$/, '')
