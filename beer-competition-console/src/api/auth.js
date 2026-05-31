import request from './request'

export function adminLogin(payload) {
  return request.post('/api/public/admin/login', payload)
}

export function sendSmsCode(payload) {
  return request.post('/api/public/sms/send', payload)
}

export function portalLogin(payload) {
  return request.post('/api/public/portal/login', payload)
}

export function getAdminMe() {
  return request.get('/api/admin/me', { authScope: 'admin' })
}

export function getPortalMe() {
  return request.get('/api/portal/me', { authScope: 'portal' })
}
