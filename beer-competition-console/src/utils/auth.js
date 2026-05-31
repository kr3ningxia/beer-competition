const TOKEN_KEYS = {
  admin: 'admin_token',
  portal: 'portal_token',
}

const USERNAME_KEYS = {
  admin: 'admin_display_name',
  portal: 'portal_display_name',
}

export function getToken(scope) {
  return localStorage.getItem(TOKEN_KEYS[scope])
}

export function setSession(scope, token, displayName) {
  localStorage.setItem(TOKEN_KEYS[scope], token)
  localStorage.setItem(USERNAME_KEYS[scope], displayName || '')
}

export function clearSession(scope) {
  localStorage.removeItem(TOKEN_KEYS[scope])
  localStorage.removeItem(USERNAME_KEYS[scope])
}

export function isLoggedIn(scope) {
  return !!getToken(scope)
}

export function getDisplayName(scope) {
  return localStorage.getItem(USERNAME_KEYS[scope]) || ''
}
