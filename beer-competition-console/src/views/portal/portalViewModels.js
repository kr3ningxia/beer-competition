export const entryStatusMeta = {
  PENDING_PAYMENT: {
    label: '等待付款确认',
    tone: 'amber',
    step: '待付款',
  },
  REGISTERED: {
    label: '报名成功',
    tone: 'green',
    step: '标签可用',
  },
  STORED: {
    label: '已确认入库',
    tone: 'blue',
    step: '等待评审',
  },
  RESULT_PUBLISHED: {
    label: '结果可查看',
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

export function competitionAction(competition, loggedIn = false) {
  if (!canSubmitEntry(competition)) {
    return { label: competition?.status === 'PUBLISHED' ? '查看结果状态' : '查看赛事详情', to: `/portal/events/${competition?.id}` }
  }
  if (!loggedIn) {
    return { label: '登录后报名', to: '/portal/login' }
  }
  return { label: '提交酒款', to: `/portal/submit?competitionId=${competition.id}` }
}

export function entryPrimaryAction(entry) {
  if (!entry) {
    return { label: '浏览开放赛事', to: '/portal/events' }
  }
  const paymentPath = `/portal/payment?entryId=${entry.id}`
  if (entry.status === 'PENDING_PAYMENT' || entry.paymentStatus === 'UNPAID') {
    return { label: '查看付款说明', to: paymentPath }
  }
  if (entry.status === 'RESULT_PUBLISHED') {
    return { label: '查看结果反馈', to: '/portal/results' }
  }
  if (entry.status === 'REGISTERED') {
    return { label: '下载现场标签', to: paymentPath }
  }
  if (entry.status === 'STORED') {
    return { label: '查看参赛进度', to: '/portal/entries' }
  }
  return { label: '查看酒款资料', to: '/portal/entries' }
}

export function priorityEntry(entries) {
  return entries.find((entry) => entry.status === 'PENDING_PAYMENT')
    || entries.find((entry) => entry.status === 'REGISTERED')
    || entries.find((entry) => entry.status === 'RESULT_PUBLISHED')
    || entries.find((entry) => entry.status === 'STORED')
    || null
}

export function entryTimeline(entry) {
  const paid = entry?.paymentStatus === 'PAID' || entry?.canDownloadLabel
  const stored = Boolean(entry?.stored) || entry?.status === 'STORED' || entry?.status === 'RESULT_PUBLISHED'
  return [
    { label: '提交资料', done: true, hint: entry?.submittedAt || '已提交' },
    { label: '付款确认', done: paid, hint: paid ? '已确认' : '等待主办方确认付款' },
    { label: '标签可用', done: paid, hint: paid ? '可下载并贴在酒瓶或外箱' : '付款确认后开放下载' },
    { label: '酒样入库', done: stored, hint: stored ? '已入库' : '等待主办方收样确认' },
    { label: '结果发布', done: entry?.status === 'RESULT_PUBLISHED', hint: entry?.status === 'RESULT_PUBLISHED' ? '反馈可查看' : '等待比赛结束后发布' },
  ]
}

export function entrySummaryForCompetition(competitionId, entries) {
  const scoped = entries.filter((entry) => entry.competitionId === competitionId)
  return {
    submitted: scoped.length,
    pendingPayment: scoped.filter((entry) => entry.status === 'PENDING_PAYMENT').length,
    registered: scoped.filter((entry) => entry.status === 'REGISTERED').length,
    stored: scoped.filter((entry) => entry.status === 'STORED' || entry.status === 'RESULT_PUBLISHED').length,
    result: scoped.filter((entry) => entry.status === 'RESULT_PUBLISHED').length,
  }
}

export function nextActionText(entry) {
  if (entry.status === 'PENDING_PAYMENT' || entry.paymentStatus === 'UNPAID') {
    return '等待付款确认'
  }
  if (entry.status === 'REGISTERED') {
    return entry.stored ? '等待评审' : '下载标签并送样'
  }
  if (entry.status === 'STORED') {
    return '等待结果发布'
  }
  if (entry.status === 'RESULT_PUBLISHED') {
    return '查看评分反馈'
  }
  return '查看详情'
}
