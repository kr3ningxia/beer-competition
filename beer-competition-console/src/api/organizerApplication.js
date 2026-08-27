import request from './request'

const APPLICATION_BASE_PATH = '/api/public/organizer-applications'
const PORTAL_APPLICATION_BASE_PATH = '/api/portal/organizer-applications'

function createApplicationForm(payload, material) {
  const formData = new FormData()
  Object.entries(payload || {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      formData.append(key, value)
    }
  })
  if (material) {
    formData.append('material', material)
  }
  return formData
}

export function submitOrganizerApplication(payload, material) {
  return request.post(APPLICATION_BASE_PATH, createApplicationForm(payload, material))
}

export function fetchOrganizerApplicationStatus(applicationNo, contactPhone) {
  return request.get(`${APPLICATION_BASE_PATH}/${encodeURIComponent(applicationNo)}`, {
    params: { contactPhone },
  })
}

export function resubmitOrganizerApplication(applicationNo, contactPhone, payload, material) {
  return request.put(`${APPLICATION_BASE_PATH}/${encodeURIComponent(applicationNo)}`, createApplicationForm(payload, material), {
    params: { contactPhone },
  })
}

export function fetchMyOrganizerApplications() {
  return request.get(PORTAL_APPLICATION_BASE_PATH, { authScope: 'portal' })
}

export function submitMyOrganizerApplication(payload, material) {
  return request.post(PORTAL_APPLICATION_BASE_PATH, createApplicationForm(payload, material), {
    authScope: 'portal',
  })
}

export function resubmitMyOrganizerApplication(applicationNo, payload, material) {
  return request.put(`${PORTAL_APPLICATION_BASE_PATH}/${encodeURIComponent(applicationNo)}`, createApplicationForm(payload, material), {
    authScope: 'portal',
  })
}
