const STORAGE_PREFIX = 'judge_form_draft'

export function draftKey(...parts) {
  const suffix = parts
    .filter((part) => part !== null && part !== undefined && part !== '')
    .join(':')
  return `${STORAGE_PREFIX}:${suffix}`
}

export function readDraft(key) {
  try {
    const raw = window.localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function writeDraft(key, value) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // 存储不可用时忽略，不影响评分流程。
  }
}

export function clearDraft(key) {
  try {
    window.localStorage.removeItem(key)
  } catch {
    // 忽略存储异常。
  }
}
