const PRICE_EPSILON = 1e-9

/**
 * 把折后单价换算成“X 折”，仅用于展示，不参与计价。
 */
export function tierPriceToZhe(price, entryFee) {
  const fee = Number(entryFee)
  const value = Number(price)
  if (!Number.isFinite(fee) || fee <= 0 || !Number.isFinite(value) || value <= 0) return ''
  const zhe = Math.round((value / fee) * 100) / 10
  const text = Number.isInteger(zhe) ? String(zhe) : zhe.toFixed(1)
  return `${text} 折`
}

export function tierPriceIssue(price, entryFee) {
  if (price === '' || price === null || price === undefined) return '请填写折后单价'
  const value = Number(price)
  if (!Number.isFinite(value)) return '请输入有效的折后单价'
  if (value <= 0) return '折后单价需大于 0'
  if (Math.abs(value * 100 - Math.round(value * 100)) > PRICE_EPSILON) return '折后单价最多保留 2 位小数'
  const fee = Number(entryFee)
  if (entryFee !== '' && entryFee !== null && entryFee !== undefined
    && Number.isFinite(fee) && value > fee) {
    return '折后单价不能高于普通报名费'
  }
  return ''
}

export function feeTierIssue(tier, index, tiers, entryFee) {
  const issue = tierPriceIssue(tier?.price, entryFee)
  if (issue) return issue
  if (index > 0) {
    const previous = tiers?.[index - 1]
    if (previous && Number(tier.price) > Number(previous.price)) {
      return '后面档位的折后单价不能高于前一档'
    }
  }
  return ''
}

/**
 * 列表内联提示用：空单价先不标红，保存时再由 feeTierIssue 拦截。
 */
export function feeTierDisplayIssue(tier, index, tiers, entryFee) {
  const price = tier?.price
  if (price === '' || price === null || price === undefined) return ''
  return feeTierIssue(tier, index, tiers, entryFee)
}
