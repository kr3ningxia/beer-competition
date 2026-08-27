function normalizeApiBaseUrl(value) {
  const fallback = import.meta.env.PROD ? '' : 'http://localhost:8084'
  const normalized = (value ?? fallback).trim().replace(/\/+$/, '')
  return normalized.endsWith('/api') ? normalized.slice(0, -4) : normalized
}

export const BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL)
