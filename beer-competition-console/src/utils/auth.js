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
  const token = getToken(scope)
  if (!token || !isTokenUsable(token, scope)) {
    clearSession(scope)
    return false
  }
  return true
}

export function getDisplayName(scope) {
  return localStorage.getItem(USERNAME_KEYS[scope]) || ''
}

function isTokenUsable(token, scope) {
  const payload = parseJwtPayload(token)
  if (!payload) {
    return false
  }
  if (payload.scope && payload.scope !== scope) {
    return false
  }
  if (payload.exp && payload.exp * 1000 <= Date.now()) {
    return false
  }
  return true
}

function parseJwtPayload(token) {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(Array.from(atob(normalized), (char) => {
      return `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`
    }).join(''))
    return JSON.parse(json)
  } catch {
    return null
  }
}
