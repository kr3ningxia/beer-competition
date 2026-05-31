import request from './request'

export function sendSmsCode(payload) {
  return request.post('/api/public/sms/send', payload)
}

export function login(payload) {
  return request.post('/api/public/judge/login', payload)
}

export function fetchMe() {
  return request.get('/api/judge/me')
}

export function fetchCompetitions() {
  return request.get('/api/judge/competitions')
}

export function fetchEntry(uuid) {
  return request.get(`/api/judge/entries/${uuid}`)
}

export function createScore(payload) {
  return request.post('/api/judge/scores', payload)
}

export function updateScore(id, payload) {
  return request.put(`/api/judge/scores/${id}`, payload)
}

export function fetchTableScores(uuid) {
  return request.get(`/api/judge/table-scores/${uuid}`)
}

export function finalizeTableScore(uuid, payload) {
  return request.post(`/api/judge/table-scores/${uuid}/finalize`, payload)
}
