export function formatAbvValue(value) {
  if (value === null || value === undefined || value === '') return ''
  const raw = String(value).trim()
  const normalized = raw.endsWith('%') ? raw.slice(0, -1).trim() : raw
  const match = normalized.match(/^(\d+)(?:\.(\d+))?$/)
  if (!match) return raw
  const integer = String(Number(match[1]))
  if (!Number.isFinite(Number(integer))) return raw
  if (!match[2]) return integer
  const decimal = match[2].replace(/0+$/, '')
  return decimal ? `${integer}.${decimal}` : integer
}

export function formatAbvWithUnit(value, emptyText = '-') {
  const formatted = formatAbvValue(value)
  return formatted ? `${formatted}%` : emptyText
}

export function normalizeAbvInput(value) {
  if (value === null || value === undefined) return ''
  const trimmed = String(value).trim()
  if (!trimmed) return ''
  const normalized = trimmed.endsWith('.') ? trimmed.slice(0, -1) : trimmed
  const match = normalized.match(/^(\d+)(?:\.(\d+))?$/)
  if (!match) return normalized
  const integer = String(Number(match[1]))
  if (!Number.isFinite(Number(integer))) return normalized
  if (!match[2]) return integer
  const decimal = match[2].replace(/0+$/, '')
  return decimal ? `${integer}.${decimal}` : integer
}

export function isValidAbvInput(value) {
  const normalized = normalizeAbvInput(value)
  if (!normalized) return false
  if (!/^(0|[1-9]\d?)(\.\d{1,2})?$/.test(normalized)) return false
  const number = Number(normalized)
  return number >= 0 && number <= 99.99
}
