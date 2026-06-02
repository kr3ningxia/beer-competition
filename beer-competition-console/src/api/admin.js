import request from './request'

export function fetchCompetitions() {
  return request.get('/api/admin/competitions', { authScope: 'admin' })
}

export function createCompetition(payload) {
  return request.post('/api/admin/competitions', payload, { authScope: 'admin' })
}

export function fetchCompetitionDetail(id) {
  return request.get(`/api/admin/competitions/${id}`, { authScope: 'admin' })
}

export function deleteCompetition(id) {
  return request.delete(`/api/admin/competitions/${id}`, { authScope: 'admin' })
}

export function fetchStyleLibraries() {
  return request.get('/api/admin/style-libraries', { authScope: 'admin' })
}

export function fetchStyleLibraryDetail(code) {
  return request.get(`/api/admin/style-libraries/${code}`, { authScope: 'admin' })
}

export function saveStyleLibrary(payload) {
  return request.post('/api/admin/style-libraries', payload, { authScope: 'admin' })
}

export function updateStyleLibrary(code, payload) {
  return request.put(`/api/admin/style-libraries/${code}`, payload, { authScope: 'admin' })
}

export function updateCompetitionBaseInfo(id, payload) {
  return request.put(`/api/admin/competitions/${id}/base-info`, payload, { authScope: 'admin' })
}

export function updateCompetitionCategories(id, payload) {
  return request.put(`/api/admin/competitions/${id}/categories`, payload, { authScope: 'admin' })
}

export function updateCompetitionStyles(id, payload) {
  return request.put(`/api/admin/competitions/${id}/styles`, payload, { authScope: 'admin' })
}

export function updateCompetitionEntryFields(id, payload) {
  return request.put(`/api/admin/competitions/${id}/entry-fields`, payload, { authScope: 'admin' })
}

export function updateCompetitionJudgeTables(id, payload) {
  return request.put(`/api/admin/competitions/${id}/judge-tables`, payload, { authScope: 'admin' })
}

export function updateCompetitionJudgeAssignments(id, payload) {
  return request.put(`/api/admin/competitions/${id}/judge-assignments`, payload, { authScope: 'admin' })
}

export function openCompetitionRegistration(id) {
  return request.post(`/api/admin/competitions/${id}/open-registration`, {}, { authScope: 'admin' })
}

export function closeCompetitionRegistration(id) {
  return request.post(`/api/admin/competitions/${id}/close-registration`, {}, { authScope: 'admin' })
}

export function prepareCompetitionJudging(id) {
  return request.post(`/api/admin/competitions/${id}/prepare-judging`, {}, { authScope: 'admin' })
}

export function fetchJudges(params = {}) {
  return request.get('/api/admin/judges', { params, authScope: 'admin' })
}

export function fetchJudgeDetail(publicId) {
  return request.get(`/api/admin/judges/${publicId}`, { authScope: 'admin' })
}

export function updateJudge(publicId, payload) {
  return request.put(`/api/admin/judges/${publicId}`, payload, { authScope: 'admin' })
}

export function updateJudgePhone(publicId, payload) {
  return request.patch(`/api/admin/judges/${publicId}/phone`, payload, { authScope: 'admin' })
}

export function updateJudgeStatus(publicId, payload) {
  return request.patch(`/api/admin/judges/${publicId}/status`, payload, { authScope: 'admin' })
}

export function createAssignment(payload) {
  return request.post('/api/admin/judge-assignments', payload, { authScope: 'admin' })
}

export function fetchScoreConfigs(competitionId) {
  return request.get(`/api/admin/score-configs/${competitionId}`, { authScope: 'admin' })
}

export function updateScoreConfigs(competitionId, payload) {
  return request.put(`/api/admin/score-configs/${competitionId}`, payload, { authScope: 'admin' })
}

export function createFirstRound(competitionId, payload) {
  return request.post(`/api/admin/competitions/${competitionId}/rounds/first`, payload, { authScope: 'admin' })
}

export function saveRoundAllocation(competitionId, roundId, payload) {
  return request.put(`/api/admin/competitions/${competitionId}/rounds/${roundId}/allocation`, payload, { authScope: 'admin' })
}

export function publishRound(competitionId, roundId) {
  return request.post(`/api/admin/competitions/${competitionId}/rounds/${roundId}/publish`, {}, { authScope: 'admin' })
}

export function completeFirstRound(competitionId, roundId) {
  return request.post(`/api/admin/competitions/${competitionId}/rounds/${roundId}/complete-first-round`, {}, { authScope: 'admin' })
}

export function createNextRound(competitionId, payload) {
  return request.post(`/api/admin/competitions/${competitionId}/rounds/next`, payload, { authScope: 'admin' })
}

export function lockRound(competitionId, roundId) {
  return request.post(`/api/admin/competitions/${competitionId}/rounds/${roundId}/lock`, {}, { authScope: 'admin' })
}

export function publishCompetitionResults(competitionId) {
  return request.post(`/api/admin/competitions/${competitionId}/results/publish`, {}, { authScope: 'admin' })
}
