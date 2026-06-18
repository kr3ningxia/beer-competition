const TOKEN_KEY = 'judge_token'
const REFRESH_TOKEN_KEY = 'judge_refresh_token'
const NAME_KEY = 'judge_display_name'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setSession(session, displayName) {
  const accessToken = typeof session === 'string' ? session : session?.accessToken || session?.token
  const refreshToken = typeof session === 'string' ? null : session?.refreshToken
  const resolvedDisplayName = typeof session === 'string' ? displayName : session?.displayName ?? displayName
  if (accessToken) {
    localStorage.setItem(TOKEN_KEY, accessToken)
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  }
  localStorage.setItem(NAME_KEY, resolvedDisplayName || '')
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(NAME_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

export function getDisplayName() {
  return localStorage.getItem(NAME_KEY) || ''
}
