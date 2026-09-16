export const fieldLabels = {
  'competition.name': '赛事名称', 'competition.code': '赛事编号',
  'sample.arrivalStart': '收样开始时间', 'sample.arrivalDeadline': '收样截止时间',
  'delivery.recipient': '联系人', 'delivery.phone': '收样电话',
  'delivery.address': '收样地址', 'delivery.note': '寄送要求',
  entryCount: '参赛酒款数', pendingDeliveryCount: '待寄送酒款数',
  resultUrl: '结果页面链接', portalUrl: '赛事平台链接', resultTable: '比赛结果明细',
}

export function readableFields(text = '') {
  return text.replace(/\{\{\s*([\w.]+)\s*}}/g, (token, key) => fieldLabels[key] ? `〔${fieldLabels[key]}〕` : token)
}

export function encodedFields(text = '') {
  return Object.entries(fieldLabels).reduce((value, [key, label]) => value.replaceAll(`〔${label}〕`, `{{${key}}}`), text)
}

export function escapeHtml(value = '') {
  return String(value).replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char])
}

export function renderPreview(source, values) {
  return source.replace(/\{\{\s*([\w.]+)\s*}}/g, (_, key) => escapeHtml(values[key] ?? `〔${fieldLabels[key] || key}〕`))
}
