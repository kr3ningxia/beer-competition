import request from './request'

export function fetchCompetitions(params = {}) {
  return request.get('/api/admin/competitions', { params, authScope: 'admin' })
}

export function createCompetition(payload) {
  return request.post('/api/admin/competitions', payload, { authScope: 'admin' })
}

export function fetchCompetitionDetail(id) {
  return request.get(`/api/admin/competitions/${id}`, { authScope: 'admin' })
}

export function fetchCompetitionOverview(id) {
  return request.get(`/api/admin/competitions/${id}/overview`, { authScope: 'admin' })
}

export function fetchCompetitionEntryPool(id) {
  return request.get(`/api/admin/competitions/${id}/entry-pool`, { authScope: 'admin' })
}

export function fetchCompetitionCollection(id) {
  return request.get(`/api/admin/competitions/${id}/collection`, { authScope: 'admin' })
}

export function fetchCompetitionCollectionQr(assetId) {
  return request.get(`/api/admin/files/${assetId}`, { authScope: 'admin', responseType: 'blob' })
}

export function uploadCompetitionCollectionQr(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/api/admin/competitions/${id}/collection/wechat-qr`, formData, { authScope: 'admin' })
}

export function updateCompetitionCollection(id, payload) {
  return request.put(`/api/admin/competitions/${id}/collection`, payload, { authScope: 'admin' })
}

export function fetchCompetitionEntryPoolPage(id, params = {}) {
  return request.get(`/api/admin/competitions/${id}/entry-pool/page`, { params, authScope: 'admin' })
}

export function fetchCompetitionBoxNumbers(id) {
  return request.get(`/api/admin/competitions/${id}/box-numbers`, { authScope: 'admin' })
}

export function fetchCompetitionQuickSummary(id) {
  return request.get(`/api/admin/competitions/${id}/quick-summary`, { authScope: 'admin' })
}

export function fetchCompetitionNotifications(id) {
  return request.get(`/api/admin/competitions/${id}/notifications`, { authScope: 'admin' })
}

export function updateCompetitionNotificationRule(id, payload) {
  return request.put(`/api/admin/competitions/${id}/notifications/rule`, payload, { authScope: 'admin' })
}

export function updateCompetitionNotificationTemplate(id, eventCode, payload) {
  return request.put(`/api/admin/competitions/${id}/notifications/templates/${eventCode}`, payload, { authScope: 'admin' })
}

export function sendCompetitionNotificationTest(id, payload) {
  return request.post(`/api/admin/competitions/${id}/notifications/test`, payload, { authScope: 'admin' })
}

export function fetchCompetitionNotificationDeliveries(id, params = {}) {
  return request.get(`/api/admin/competitions/${id}/notifications/deliveries`, { params, authScope: 'admin' })
}

export function retryCompetitionNotification(id, deliveryId) {
  return request.post(`/api/admin/competitions/${id}/notifications/deliveries/${deliveryId}/retry`, {}, { authScope: 'admin' })
}

export function fetchCompetitionProgress(id) {
  return request.get(`/api/admin/competitions/${id}/progress`, { authScope: 'admin' })
}

export function fetchCompetitionResultDrafts(id) {
  return request.get(`/api/admin/competitions/${id}/results/draft`, { authScope: 'admin' })
}

export function fetchCompetitionAwardRules(id) {
  return request.get(`/api/admin/competitions/${id}/award-rules`, { authScope: 'admin' })
}

export function fetchCompetitionAwards(id) {
  return request.get(`/api/admin/competitions/${id}/awards`, { authScope: 'admin' })
}

export function fetchCompetitionLiveBoard(id) {
  return request.get(`/api/admin/competitions/${id}/live-board`, { authScope: 'admin' })
}

export function fetchCompetitionAnalytics(id) {
  return request.get(`/api/admin/competitions/${id}/analytics`, { authScope: 'admin' })
}

export function fetchCompetitionSponsors(id) {
  return request.get(`/api/admin/competitions/${id}/sponsors`, { authScope: 'admin' })
}

export function updateCompetitionSponsors(id, payload) {
  return request.put(`/api/admin/competitions/${id}/sponsors`, payload, { authScope: 'admin' })
}

export function uploadCompetitionSponsorLogo(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/api/admin/competitions/${id}/sponsors/logo`, formData, { authScope: 'admin' })
}

export function fetchCompetitionFeedbackReview(id) {
  return request.get(`/api/admin/competitions/${id}/feedback-review`, { authScope: 'admin' })
}

export function fetchCompetitionFeedbackReviewPage(id, params = {}) {
  return request.get(`/api/admin/competitions/${id}/feedback-review/page`, { params, authScope: 'admin' })
}

export function updateCompetitionFeedbackComment(competitionId, scoreRecordId, payload) {
  return request.patch(`/api/admin/competitions/${competitionId}/feedback-review/score-records/${scoreRecordId}/comments`, payload, { authScope: 'admin' })
}

export function fetchAdminOperationLogs(params = {}) {
  return request.get('/api/admin/operation-logs', { params, authScope: 'admin' })
}

export function fetchAdminEntries(params = {}) {
  return request.get('/api/admin/entries', { params, authScope: 'admin' })
}

export function fetchAdminBankTransfers(params = {}) {
  return request.get('/api/admin/bank-transfers', { params, authScope: 'admin' })
}

export function fetchAdminBankTransferDetail(id) {
  return request.get(`/api/admin/bank-transfers/${id}`, { authScope: 'admin' })
}

export function confirmAdminBankTransfer(id, payload = {}) {
  return request.post(`/api/admin/bank-transfers/${id}/confirm`, payload, { authScope: 'admin' })
}

export function rejectAdminBankTransfer(id, payload = {}) {
  return request.post(`/api/admin/bank-transfers/${id}/reject`, payload, { authScope: 'admin' })
}

export function downloadAdminBankTransferVoucher(id) {
  return request.get(`/api/admin/bank-transfers/${id}/voucher`, {
    authScope: 'admin',
    responseType: 'blob',
  })
}

export function fetchAdminEntryDetail(entryId) {
  return request.get(`/api/admin/entries/${entryId}`, { authScope: 'admin' })
}

export function updateAdminEntry(entryId, payload) {
  return request.put(`/api/admin/entries/${entryId}`, payload, { authScope: 'admin' })
}

export function fetchAdminEntryDeleteImpact(entryId) {
  return request.get(`/api/admin/entries/${entryId}/delete-impact`, { authScope: 'admin' })
}

export function administrativelyDeleteEntry(entryId, payload) {
  return request.post(`/api/admin/entries/${entryId}/administrative-delete`, payload, { authScope: 'admin' })
}

export function deleteCompetition(id) {
  return request.delete(`/api/admin/competitions/${id}`, { authScope: 'admin' })
}

export function fetchStyleLibraries() {
  return request.get('/api/admin/style-libraries', { authScope: 'admin' })
}

export function fetchEnabledStyleLibraries() {
  return request.get('/api/admin/style-libraries/enabled', { authScope: 'admin' })
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

export function updateStyleLibraryVisibility(code, visibility) {
  return request.patch(`/api/admin/style-libraries/${code}/visibility`, { visibility }, { authScope: 'admin' })
}

export function updateCompetitionBaseInfo(id, payload) {
  return request.put(`/api/admin/competitions/${id}/base-info`, payload, { authScope: 'admin' })
}

export function updateCompetitionRefundPolicy(id, payload) {
  return request.put(`/api/admin/competitions/${id}/refund-policy`, payload, { authScope: 'admin' })
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

export function reopenCompetitionRegistration(id, payload) {
  return request.post(`/api/admin/competitions/${id}/reopen-registration`, payload, { authScope: 'admin' })
}

export function returnCompetitionToSampleCheck(id, payload) {
  return request.post(`/api/admin/competitions/${id}/return-to-sample-check`, payload, { authScope: 'admin' })
}

export function fetchJudges(params = {}) {
  return request.get('/api/admin/judges', { params, authScope: 'admin' })
}

export function fetchJudgesPage(params = {}) {
  return request.get('/api/admin/judges/page', { params, authScope: 'admin' })
}

export function fetchJudgeDetail(publicId) {
  return request.get(`/api/admin/judges/${publicId}`, { authScope: 'admin' })
}

export function fetchCompetitionJudgePerformances(competitionId) {
  return request.get(`/api/admin/competitions/${competitionId}/judge-performances`, { authScope: 'admin' })
}

export function saveJudgePerformance(competitionId, judgePublicId, payload) {
  return request.put(`/api/admin/competitions/${competitionId}/judge-performances/${judgePublicId}`, payload, { authScope: 'admin' })
}

export function fetchJudgePerformanceOverview(publicIds = []) {
  return request.get('/api/admin/judge-performances/accounts', {
    params: { publicIds: publicIds.join(',') },
    authScope: 'admin',
  })
}

export function fetchJudgePerformanceHistory(publicId) {
  return request.get(`/api/admin/judges/${publicId}/performances`, { authScope: 'admin' })
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

export function deleteJudge(publicId) {
  return request.delete(`/api/admin/judges/${publicId}`, { authScope: 'admin' })
}

export function fetchJudgeRecruitments(params = {}) { return request.get('/api/admin/judge-recruitments', { params, authScope: 'admin' }) }
export function fetchJudgeRecruitment(id) { return request.get(`/api/admin/judge-recruitments/${id}`, { authScope: 'admin' }) }
export function createJudgeRecruitment(payload) { return request.post('/api/admin/judge-recruitments', payload, { authScope: 'admin' }) }
export function updateJudgeRecruitment(id, payload) { return request.put(`/api/admin/judge-recruitments/${id}`, payload, { authScope: 'admin' }) }
export function publishJudgeRecruitment(id) { return request.post(`/api/admin/judge-recruitments/${id}/publish`, {}, { authScope: 'admin' }) }
export function closeJudgeRecruitment(id) { return request.post(`/api/admin/judge-recruitments/${id}/close`, {}, { authScope: 'admin' }) }
export function reopenJudgeRecruitment(id) { return request.post(`/api/admin/judge-recruitments/${id}/reopen`, {}, { authScope: 'admin' }) }
export function fetchJudgeRecruitmentApplications(id, params = {}) { return request.get(`/api/admin/judge-recruitments/${id}/applications`, { params, authScope: 'admin' }) }
export function reviewJudgeRecruitmentApplication(id, payload) { return request.patch(`/api/admin/judge-recruitments/applications/${id}/status`, payload, { authScope: 'admin' }) }

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

export function changeRoundTableJudgeMembers(competitionId, roundId, roundTableId, payload) {
  return request.patch(`/api/admin/competitions/${competitionId}/rounds/${roundId}/tables/${roundTableId}/judge-members`, payload, { authScope: 'admin' })
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

export function deleteDraftRound(competitionId, roundId) {
  return request.delete(`/api/admin/competitions/${competitionId}/rounds/${roundId}`, { authScope: 'admin' })
}

export function lockRound(competitionId, roundId) {
  return request.post(`/api/admin/competitions/${competitionId}/rounds/${roundId}/lock`, {}, { authScope: 'admin' })
}

export function overrideRoundTableConfirmation(competitionId, roundTableId, payload) {
  return request.post(`/api/admin/competitions/${competitionId}/round-tables/${roundTableId}/confirmation-override`, payload, { authScope: 'admin' })
}

export function publishCompetitionResults(competitionId) {
  return request.post(`/api/admin/competitions/${competitionId}/results/publish`, {}, { authScope: 'admin' })
}

export function generateCompetitionAwards(competitionId) {
  return request.post(`/api/admin/competitions/${competitionId}/awards/generate`, {}, { authScope: 'admin' })
}

export function confirmCompetitionAwards(competitionId, payload) {
  return request.put(`/api/admin/competitions/${competitionId}/awards/confirm`, payload, { authScope: 'admin' })
}

export function uploadAwardCertificate(competitionId, awardId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/api/admin/competitions/${competitionId}/awards/${awardId}/certificate`, formData, { authScope: 'admin' })
}

export function deleteAwardCertificate(competitionId, awardId) {
  return request.delete(`/api/admin/competitions/${competitionId}/awards/${awardId}/certificate`, { authScope: 'admin' })
}

export function downloadAwardCertificate(competitionId, awardId) {
  return request.get(`/api/admin/competitions/${competitionId}/awards/${awardId}/certificate`, {
    authScope: 'admin',
    responseType: 'blob',
  })
}

export function exportCompetitionScoringData(competitionId) {
  return request.get(`/api/admin/competitions/${competitionId}/exports/scoring`, {
    authScope: 'admin',
    responseType: 'blob',
  })
}

export function exportCompetitionEntries(competitionId, params = {}) {
  return request.get(`/api/admin/competitions/${competitionId}/exports/entries`, {
    params,
    authScope: 'admin',
    responseType: 'blob',
  })
}

export function exportCompetitionDelivery(competitionId, params = {}) {
  return request.get(`/api/admin/competitions/${competitionId}/exports/delivery`, {
    params,
    authScope: 'admin',
    responseType: 'blob',
  })
}

export function exportCompetitionLabels(competitionId, params = {}) {
  return request.get(`/api/admin/competitions/${competitionId}/exports/labels`, {
    params,
    authScope: 'admin',
    responseType: 'blob',
  })
}

export function confirmEntryPayment(entryId, payload = {}) {
  return request.post(`/api/admin/entries/${entryId}/confirm-payment`, payload, { authScope: 'admin' })
}

export function markEntryStored(entryId, payload = {}) {
  return request.post(`/api/admin/entries/${entryId}/mark-stored`, payload, { authScope: 'admin' })
}

export function unmarkEntryStored(entryId, payload = {}) {
  return request.post(`/api/admin/entries/${entryId}/unmark-stored`, payload, { authScope: 'admin' })
}

export function cancelEntry(entryId, payload = {}) {
  return request.post(`/api/admin/entries/${entryId}/cancel`, payload, { authScope: 'admin' })
}

export function approveEntryRefund(refundId, payload = {}) {
  return request.post(`/api/admin/refunds/${refundId}/approve`, payload, { authScope: 'admin' })
}

export function rejectEntryRefund(refundId, payload = {}) {
  return request.post(`/api/admin/refunds/${refundId}/reject`, payload, { authScope: 'admin' })
}

export function retryEntryRefund(refundId, payload = {}) {
  return request.post(`/api/admin/refunds/${refundId}/retry`, payload, { authScope: 'admin' })
}

export function confirmOfflineEntryRefund(refundId, payload = {}) {
  return request.post(`/api/admin/refunds/${refundId}/confirm-offline`, payload, { authScope: 'admin' })
}

export function registerOfflineEntryRefund(refundId, payload = {}) {
  const formData = new FormData()
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') formData.append(key, value)
  })
  return request.post(`/api/admin/refunds/${refundId}/register-offline`, formData, { authScope: 'admin' })
}

export function fetchAdminUsers(params = {}) {
  return request.get('/api/admin/accounts', { params, authScope: 'admin' })
}

export function createAdminUser(payload) {
  return request.post('/api/admin/accounts', payload, { authScope: 'admin' })
}

export function updateAdminUser(id, payload) {
  return request.put(`/api/admin/accounts/${id}`, payload, { authScope: 'admin' })
}

export function updateAdminUserStatus(id, payload) {
  return request.patch(`/api/admin/accounts/${id}/status`, payload, { authScope: 'admin' })
}

export function resetAdminUserPassword(id, payload) {
  return request.patch(`/api/admin/accounts/${id}/password`, payload, { authScope: 'admin' })
}

export function updateMyAdminPassword(payload) {
  return request.patch('/api/admin/me/password', payload, { authScope: 'admin' })
}

export function updateMyAdminCredentials(payload) {
  return request.patch('/api/admin/me/credentials', payload, { authScope: 'admin' })
}
