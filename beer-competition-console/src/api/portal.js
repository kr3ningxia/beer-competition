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

export function fetchPortalCompetitionResults() {
  return request.get('/api/portal/public/results')
}

export function fetchPortalCompetitionResultDetail(id) {
  return request.get(`/api/portal/public/results/${id}`)
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

export function updatePortalEntry(entryId, data) {
  return request.put(`/api/portal/entries/${entryId}`, data, { authScope: 'portal' })
}

export function submitPortalEntryDelivery(entryId, data) {
  return request.post(`/api/portal/entries/${entryId}/delivery`, data, { authScope: 'portal' })
}

export function cancelPortalEntry(entryId) {
  return request.post(`/api/portal/entries/${entryId}/cancel`, {}, { authScope: 'portal' })
}

export function simulatePortalEntryPayment(entryId) {
  return request.post(`/api/portal/entries/${entryId}/payment/simulate`, {}, { authScope: 'portal' })
}

export function createPortalEntryWechatNativePayment(entryId) {
  return request.post(`/api/portal/entries/${entryId}/payment/wechat/native`, {}, { authScope: 'portal' })
}

export function createPortalEntryWechatJsapiPayment(entryId, data) {
  return request.post(`/api/portal/entries/${entryId}/payment/wechat/jsapi`, data, { authScope: 'portal' })
}

export function fetchPortalWechatPayClientConfig() {
  return request.get('/api/portal/payment/wechat/client-config', { authScope: 'portal' })
}

export function fetchPortalEntryPaymentStatus(entryId) {
  return request.get(`/api/portal/entries/${entryId}/payment/status`, { authScope: 'portal' })
}

export function fetchPortalBankTransferAccount() {
  return request.get('/api/portal/payment/bank-transfer/account', { authScope: 'portal' })
}

export function uploadPortalBankTransferVoucher(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/portal/payment/bank-transfer/voucher', formData, { authScope: 'portal' })
}

export function submitPortalBankTransfer(data) {
  return request.post('/api/portal/payment/bank-transfer', data, { authScope: 'portal' })
}

export function fetchPortalBankTransfer(id) {
  return request.get(`/api/portal/payment/bank-transfer/${id}`, { authScope: 'portal' })
}

export function updatePortalBankTransfer(id, data) {
  return request.put(`/api/portal/payment/bank-transfer/${id}`, data, { authScope: 'portal' })
}

export function cancelPortalBankTransfer(id) {
  return request.post(`/api/portal/payment/bank-transfer/${id}/cancel`, {}, { authScope: 'portal' })
}

export function requestPortalEntryRefund(entryId, data) {
  return request.post(`/api/portal/entries/${entryId}/refund`, data, { authScope: 'portal' })
}

export function fetchPortalEntryLabel(id) {
  return request.get(`/api/portal/entries/${id}/label`, { authScope: 'portal' })
}

export function downloadPortalEntryLabelPdf(id) {
  return request.get(`/api/portal/entries/${id}/label/pdf`, {
    authScope: 'portal',
    responseType: 'blob',
  })
}

export function downloadPortalEntryLabelPng(id) {
  return request.get(`/api/portal/entries/${id}/label/png`, {
    authScope: 'portal',
    responseType: 'blob',
  })
}

export function fetchPortalProfile() {
  return request.get('/api/portal/profile', { authScope: 'portal' })
}

export function fetchPortalResults() {
  return request.get('/api/portal/results', { authScope: 'portal' })
}

export function fetchPortalResultDetail(entryId) {
  return request.get(`/api/portal/results/${entryId}`, { authScope: 'portal' })
}

export function downloadPortalResultCertificate(entryId) {
  return request.get(`/api/portal/results/${entryId}/certificate`, {
    authScope: 'portal',
    responseType: 'blob',
  })
}

export function updatePortalProfile(data) {
  return request.put('/api/portal/profile', data, { authScope: 'portal' })
}

export function uploadPortalAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/portal/profile/avatar', formData, { authScope: 'portal' })
}
