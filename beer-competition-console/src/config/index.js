function normalizeApiBaseUrl(value) {
  const fallback = import.meta.env.PROD ? '' : 'http://localhost:8080'
  const normalized = (value ?? fallback).trim().replace(/\/+$/, '')
  return normalized.endsWith('/api') ? normalized.slice(0, -4) : normalized
}

function defaultJudgeH5BaseUrl() {
  if (import.meta.env.PROD && typeof window !== 'undefined') {
    return `${window.location.origin}/judge`
  }
  return 'http://localhost:5174'
}

export const BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL)
export const JUDGE_H5_BASE_URL = import.meta.env.VITE_JUDGE_H5_BASE_URL || defaultJudgeH5BaseUrl()
export const WECHAT_PAY_APP_ID = import.meta.env.VITE_WECHAT_PAY_APP_ID || ''
