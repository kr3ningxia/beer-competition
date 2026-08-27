import request from './request'

const APPLICATION_BASE_PATH = '/api/admin/organizer-applications'

export function fetchOrganizerApplications(status) {
  return request.get(APPLICATION_BASE_PATH, {
    params: status && status !== 'ALL' ? { status } : undefined,
    authScope: 'admin',
  })
}

export function reviewOrganizerApplication(id, payload) {
  return request.put(`${APPLICATION_BASE_PATH}/${id}/review`, payload, { authScope: 'admin' })
}

export function provisionOrganizerApplication(id) {
  return request.post(`${APPLICATION_BASE_PATH}/${id}/provision`, {}, { authScope: 'admin' })
}

export function downloadOrganizerApplicationMaterial(fileAssetId) {
  return request.get(`/api/admin/files/${fileAssetId}`, {
    authScope: 'admin',
    responseType: 'blob',
  })
}
