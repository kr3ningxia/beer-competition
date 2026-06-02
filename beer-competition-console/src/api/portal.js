import request from './request'

export function fetchPortalHome() {
  return request.get('/api/portal/public/home')
}

export function fetchPortalCompetitions() {
  return request.get('/api/portal/public/competitions')
}

export function fetchPortalCompetitionDetail(id) {
  return request.get(`/api/portal/public/competitions/${id}`)
}

export function fetchMyParticipation() {
  return request.get('/api/portal/my', { authScope: 'portal' })
}

export function fetchPortalEntries() {
  return request.get('/api/portal/entries', { authScope: 'portal' })
}

export function fetchPortalEntryDetail(id) {
  return request.get(`/api/portal/entries/${id}`, { authScope: 'portal' })
}

export function submitPortalEntry(competitionId, data) {
  return request.post(`/api/portal/competitions/${competitionId}/entries`, data, { authScope: 'portal' })
}

export function fetchPortalEntryLabel(id) {
  return request.get(`/api/portal/entries/${id}/label`, { authScope: 'portal' })
}

export function fetchPortalResults() {
  return request.get('/api/portal/results', { authScope: 'portal' })
}

export function fetchPortalResultDetail(entryId) {
  return request.get(`/api/portal/results/${entryId}`, { authScope: 'portal' })
}

export function updatePortalProfile(data) {
  return request.put('/api/portal/profile', data, { authScope: 'portal' })
}

export function updatePortalAwardAddress(data) {
  return request.put('/api/portal/award-address', data, { authScope: 'portal' })
}
