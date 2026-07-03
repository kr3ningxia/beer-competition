export const entryStatusMeta = {
  PENDING_PAYMENT: {
    label: '待支付',
    tone: 'amber',
    step: '待支付',
  },
  REGISTERED: {
    label: '报名成功',
    tone: 'green',
    step: '标签下载',
  },
  STORED: {
    label: '已确认入库',
    tone: 'blue',
    step: '等待评审',
  },
  RESULT_PUBLISHED: {
    label: '结果已发布',
    tone: 'gold',
    step: '结果发布',
  },
  CANCELED: {
    label: '报名已取消',
    tone: 'muted',
    step: '已取消',
  },
}

export function isRegistrationOpen(competition) {
  return competition?.status === 'REGISTRATION_OPEN'
}

export function canSubmitEntry(competition) {
  return isRegistrationOpen(competition)
}

export function isCompetitionResultPublished(competition) {
  return competition?.status === 'PUBLISHED'
}

export function isEntryResultPublished(entry) {
  return Boolean(entry?.published) || entry?.status === 'RESULT_PUBLISHED'
}

export function isEntryStored(entry) {
  return Boolean(entry?.stored) || entry?.storedFlag === 1 || entry?.status === 'STORED' || isEntryResultPublished(entry)
}

export function hasEntryDeliveryProgress(entry) {
  if (!entry) return false
  return ['SUBMITTED', 'RECEIVED'].includes(entry.deliveryStatus)
    || Boolean(entry.deliverySubmittedAt || entry.delivery?.submittedTime || entry.trackingNo || entry.deliveryMethod)
}

export function isEntryRefundActive(entry) {
  return ['REQUESTED', 'APPROVED', 'PROCESSING'].includes(entry?.refundStatus)
}

export function isEntryRefunded(entry) {
  return entry?.paymentStatus === 'REFUNDED' || entry?.refundStatus === 'SUCCESS'
}

export function isEntryPaymentPending(entry) {
  return !isEntryRefundActive(entry)
    && !isEntryRefunded(entry)
    && (entry?.status === 'PENDING_PAYMENT' || ['UNPAID', 'PENDING_CONFIRM'].includes(entry?.paymentStatus))
}

export function isEntryDeliveryActionPending(entry) {
  return entry?.status === 'REGISTERED'
    && !isEntryRefundActive(entry)
    && !isEntryStored(entry)
    && !hasEntryDeliveryProgress(entry)
}

export function isEntryVendorActionPending(entry) {
  return isEntryPaymentPending(entry) || isEntryDeliveryActionPending(entry)
}

export function isEarlyBirdActive(competition, now = new Date()) {
  if (competition?.earlyBirdFee === null || competition?.earlyBirdFee === undefined || !competition?.earlyBirdDeadline) {
    return false
  }
  const deadline = new Date(competition.earlyBirdDeadline)
  return !Number.isNaN(deadline.getTime()) && now.getTime() <= deadline.getTime()
}

export function currentEntryFee(competition) {
  if (!competition) return null
  return isEarlyBirdActive(competition) ? competition.earlyBirdFee : competition.entryFee
}

export function entryPayAmount(entry) {
  return entry?.payment?.amount ?? entry?.entryFee ?? 0
}

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '¥0'
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(Number(value))
}

export function formatCompetitionFee(competition) {
  if (!competition || competition.entryFee === null || competition.entryFee === undefined) return '待公布'
  if (isEarlyBirdActive(competition)) {
    return `早鸟价 ${formatMoney(competition.earlyBirdFee)} / 款`
  }
  return `报名费 ${formatMoney(competition.entryFee)} / 款`
}

export function isEntryAwarded(entry) {
  return isEntryResultPublished(entry) && Boolean(entry?.awardName || entry?.roundResult?.awardName || entry?.champion)
}

export function entryResultPath(entry) {
  return entry?.id || entry?.entryId ? `/portal/results?entryId=${entry.id || entry.entryId}` : '/portal/results'
}

export function competitionResultPath(competitionId) {
  return competitionId ? `/portal/results?competitionId=${competitionId}` : '/portal/results'
}

export function competitionAction(competition, loggedIn = false) {
  if (isCompetitionResultPublished(competition)) {
    return {
      label: loggedIn ? '查看本场结果' : '登录查看结果',
      to: loggedIn ? competitionResultPath(competition?.id) : '/portal/login',
    }
  }
  if (!canSubmitEntry(competition)) {
    return { label: '查看赛事详情', to: `/portal/events/${competition?.id}` }
  }
  if (!loggedIn) {
    return { label: '登录后报名', to: '/portal/login' }
  }
  return { label: '报名参赛', to: `/portal/submit?competitionId=${competition.id}` }
}

export function entryPrimaryAction(entry) {
  if (!entry) {
    return { label: '浏览开放赛事', to: '/portal/events' }
  }
  const paymentPath = `/portal/payment?entryId=${entry.id}`
  if (isEntryRefundActive(entry)) {
    return { label: '查看退款进度', to: paymentPath }
  }
  if (isEntryRefunded(entry)) {
    return { label: '查看酒款资料', to: '/portal/entries' }
  }
  if (isEntryPaymentPending(entry)) {
    if (entry.paymentStatus === 'PENDING_CONFIRM') {
      return { label: '查看转账进度', to: paymentPath }
    }
    return { label: '去支付', to: paymentPath }
  }
  if (isEntryResultPublished(entry)) {
    return { label: '查看结果', to: entryResultPath(entry) }
  }
  if (isEntryDeliveryActionPending(entry)) {
    return { label: '办理送样', to: paymentPath }
  }
  if (entry.status === 'REGISTERED') {
    return { label: '查看送样进度', to: paymentPath }
  }
  if (entry.status === 'STORED') {
    return { label: '查看参赛进度', to: '/portal/entries' }
  }
  return { label: '查看酒款资料', to: '/portal/entries' }
}

export function priorityEntry(entries) {
  return entries.find((entry) => isEntryPaymentPending(entry))
    || entries.find((entry) => isEntryDeliveryActionPending(entry))
    || entries.find((entry) => isEntryResultPublished(entry))
    || entries.find((entry) => entry.status === 'STORED')
    || entries.find((entry) => entry.status === 'REGISTERED')
    || null
}

export function entryTimeline(entry) {
  const paid = entry?.paymentStatus === 'PAID' || entry?.canDownloadLabel
  const pendingConfirm = entry?.paymentStatus === 'PENDING_CONFIRM'
  const resultPublished = isEntryResultPublished(entry)
  const stored = isEntryStored(entry)
  const items = [
    { label: '提交资料', done: true, hint: entry?.submittedAt || '已提交' },
    { label: '支付报名费', done: paid, hint: paid ? '已支付' : pendingConfirm ? '等待转账确认' : '待支付' },
    { label: '标签下载', done: paid, hint: paid ? '已开放下载，请贴在酒瓶或外箱' : '支付成功后开放下载' },
    { label: '样品入库', done: stored, hint: stored ? '已入库' : '等待组委会确认入库' },
    { label: '结果发布', done: resultPublished, hint: resultPublished ? '结果已发布' : '等待组委会发布结果' },
  ]
  if (entry?.refundStatus) {
    items.splice(2, 0, {
      label: isEntryRefunded(entry) ? '退款完成' : '退款申请',
      done: isEntryRefunded(entry),
      hint: isEntryRefunded(entry) ? '报名费已退款，报名已取消' : '退款申请已提交，等待退款结果',
    })
  }
  return items
}

export function entrySummaryForCompetition(competitionId, entries) {
  const scoped = entries.filter((entry) => entry.competitionId === competitionId)
  return {
    submitted: scoped.length,
    pendingPayment: scoped.filter((entry) => isEntryPaymentPending(entry)).length,
    deliveryActionPending: scoped.filter((entry) => isEntryDeliveryActionPending(entry)).length,
    deliverySubmitted: scoped.filter((entry) => entry.status === 'REGISTERED' && !isEntryStored(entry) && hasEntryDeliveryProgress(entry)).length,
    registered: scoped.filter((entry) => entry.status === 'REGISTERED').length,
    stored: scoped.filter((entry) => isEntryStored(entry)).length,
    result: scoped.filter((entry) => isEntryResultPublished(entry)).length,
  }
}

export function nextActionText(entry) {
  if (isEntryRefundActive(entry)) {
    return '退款处理中'
  }
  if (isEntryRefunded(entry)) {
    return '已退款'
  }
  if (isEntryPaymentPending(entry)) {
    if (entry.paymentStatus === 'PENDING_CONFIRM') return '等待转账确认'
    return '待支付报名费'
  }
  if (isEntryDeliveryActionPending(entry)) {
    return '办理送样'
  }
  if (entry.status === 'REGISTERED') {
    return hasEntryDeliveryProgress(entry) ? '等待组委会确认入库' : '办理送样'
  }
  if (isEntryResultPublished(entry)) {
    return '查看结果'
  }
  if (entry.status === 'STORED') {
    return '等待结果发布'
  }
  return '查看详情'
}
