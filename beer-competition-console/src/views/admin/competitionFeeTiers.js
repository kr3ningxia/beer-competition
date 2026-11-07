export const DISCOUNT_RATE_MAX_DECIMALS = 4

const PERCENT_EPSILON = 1e-6

export function discountRateToPercent(rate) {
  return Number((Number(rate) * 100).toFixed(DISCOUNT_RATE_MAX_DECIMALS))
}

export function discountPercentToRate(percent) {
  return Number((Number(percent) / 100).toFixed(6))
}

export function discountPercentIssue(value) {
  if (value === '' || value === null || value === undefined) return '请填写折扣率'
  const percent = Number(value)
  if (!Number.isFinite(percent)) return '请输入有效的折扣率'
  if (percent < 1 - PERCENT_EPSILON || percent > 100 + PERCENT_EPSILON) return '折扣率需在 1% 到 100% 之间'
  const rounded = Math.round(percent * 10 ** DISCOUNT_RATE_MAX_DECIMALS) / 10 ** DISCOUNT_RATE_MAX_DECIMALS
  if (Math.abs(percent - rounded) > PERCENT_EPSILON) return `折扣率最多保留 ${DISCOUNT_RATE_MAX_DECIMALS} 位小数`
  return ''
}

export function feeTierIssue(tier, index, tiers) {
  const issue = discountPercentIssue(tier?.discountRatePercent)
  if (issue) return issue
  if (index > 0) {
    const previous = tiers?.[index - 1]
    if (previous && Number(tier.discountRatePercent) > Number(previous.discountRatePercent)) {
      return '折扣率不能高于上一档'
    }
  }
  return ''
}
