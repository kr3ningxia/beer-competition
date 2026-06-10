import { ref } from 'vue'

const TOKEN_KEYS = {
  admin: 'admin_token',
  portal: 'portal_token',
}

const USERNAME_KEYS = {
  admin: 'admin_display_name',
  portal: 'portal_display_name',
}

const SESSION_EVENT = 'beer-competition-session-updated'
const sessionRevision = ref(0)

export function getToken(scope) {
  return localStorage.getItem(TOKEN_KEYS[scope])
}

export function setSession(scope, token, displayName) {
  localStorage.setItem(TOKEN_KEYS[scope], token)
  setDisplayName(scope, displayName)
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
  localStorage.removeItem(USERNAME_KEYS[scope])
  notifySessionUpdated(scope)
}

export function isLoggedIn(scope) {
  sessionRevision.value
  const token = getToken(scope)
  return Boolean(token && isTokenUsable(token, scope))
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
