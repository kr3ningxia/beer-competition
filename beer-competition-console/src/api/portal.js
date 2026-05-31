import request from './request'

export function fetchPortalEntries() {
  return request.get('/api/portal/entries', { authScope: 'portal' })
}

export function fetchPortalEntryDetail(id) {
  return request.get(`/api/portal/entries/${id}`, { authScope: 'portal' })
}
