const TOKEN_KEY = 'judge_token'
const NAME_KEY = 'judge_display_name'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setSession(token, displayName) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(NAME_KEY, displayName || '')
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(NAME_KEY)
}

export function isLoggedIn() {
  return !!getToken()
}

export function getDisplayName() {
  return localStorage.getItem(NAME_KEY) || ''
}
