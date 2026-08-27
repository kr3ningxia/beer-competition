import { ref } from 'vue'

const TOKEN_KEYS = {
  admin: 'admin_token',
  portal: 'portal_token',
}

const REFRESH_TOKEN_KEYS = {
  admin: 'admin_refresh_token',
  portal: 'portal_refresh_token',
}

const USERNAME_KEYS = {
  admin: 'admin_display_name',
  portal: 'portal_display_name',
}

const ADMIN_TYPE_KEY = 'admin_type'
const ADMIN_USERNAME_KEY = 'admin_username'
const ADMIN_MUST_CHANGE_PASSWORD_KEY = 'admin_must_change_password'
const ADMIN_MUST_CHANGE_USERNAME_KEY = 'admin_must_change_username'

const SESSION_EVENT = 'beer-competition-session-updated'
const sessionRevision = ref(0)

export function getToken(scope) {
  return localStorage.getItem(TOKEN_KEYS[scope])
}

export function getRefreshToken(scope) {
  return localStorage.getItem(REFRESH_TOKEN_KEYS[scope])
}

export function setSession(scope, session, displayName) {
  const accessToken = typeof session === 'string' ? session : session?.accessToken || session?.token
  const refreshToken = typeof session === 'string' ? null : session?.refreshToken
  const resolvedDisplayName = typeof session === 'string' ? displayName : session?.displayName ?? displayName
  if (accessToken) {
    localStorage.setItem(TOKEN_KEYS[scope], accessToken)
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEYS[scope], refreshToken)
  }
  if (scope === 'admin' && typeof session !== 'string' && session?.adminType) {
    localStorage.setItem(ADMIN_TYPE_KEY, session.adminType)
  }
  if (scope === 'admin' && typeof session !== 'string') {
    if (Object.prototype.hasOwnProperty.call(session, 'username') && session.username != null) {
      localStorage.setItem(ADMIN_USERNAME_KEY, session.username)
    }
    if (Object.prototype.hasOwnProperty.call(session, 'mustChangePassword')) {
      localStorage.setItem(ADMIN_MUST_CHANGE_PASSWORD_KEY, session.mustChangePassword ? '1' : '0')
    }
    if (Object.prototype.hasOwnProperty.call(session, 'mustChangeUsername')) {
      localStorage.setItem(ADMIN_MUST_CHANGE_USERNAME_KEY, session.mustChangeUsername ? '1' : '0')
    }
  }
  setDisplayName(scope, resolvedDisplayName)
}

export function setDisplayName(scope, displayName) {
  localStorage.setItem(USERNAME_KEYS[scope], displayName || '')
  notifySessionUpdated(scope)
}

export function createLocalSessionToken(scope, displayName) {
  const now = Math.floor(Date.now() / 1000)
  const payload = {
    scope,
    displayName,
    iat: now,
    exp: now + 24 * 60 * 60,
  }

  return [
    encodeBase64Url({ alg: 'none', typ: 'JWT' }),
    encodeBase64Url(payload),
    'local',
  ].join('.')
}

export function clearSession(scope) {
  localStorage.removeItem(TOKEN_KEYS[scope])
  localStorage.removeItem(REFRESH_TOKEN_KEYS[scope])
  localStorage.removeItem(USERNAME_KEYS[scope])
  if (scope === 'admin') {
    localStorage.removeItem(ADMIN_TYPE_KEY)
    localStorage.removeItem(ADMIN_USERNAME_KEY)
    localStorage.removeItem(ADMIN_MUST_CHANGE_PASSWORD_KEY)
    localStorage.removeItem(ADMIN_MUST_CHANGE_USERNAME_KEY)
  }
  notifySessionUpdated(scope)
}

export function isLoggedIn(scope) {
  sessionRevision.value
  const token = getToken(scope)
  return Boolean((token && isTokenUsable(token, scope)) || getRefreshToken(scope))
}

export function getDisplayName(scope) {
  return localStorage.getItem(USERNAME_KEYS[scope]) || ''
}

export function getAdminType() {
  return localStorage.getItem(ADMIN_TYPE_KEY) || ''
}

export function getAdminUsername() {
  return localStorage.getItem(ADMIN_USERNAME_KEY) || ''
}

export function isAdminCredentialSetupRequired() {
  return localStorage.getItem(ADMIN_MUST_CHANGE_PASSWORD_KEY) === '1'
    || localStorage.getItem(ADMIN_MUST_CHANGE_USERNAME_KEY) === '1'
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

function encodeBase64Url(value) {
  const json = JSON.stringify(value)
  const bytes = encodeURIComponent(json).replace(/%([0-9A-F]{2})/g, (_, hex) => {
    return String.fromCharCode(Number.parseInt(hex, 16))
  })
  return btoa(bytes).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function notifySessionUpdated(scope) {
  sessionRevision.value += 1
  window.dispatchEvent(new CustomEvent(SESSION_EVENT, { detail: { scope } }))
}
