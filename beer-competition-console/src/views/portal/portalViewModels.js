export const entryStatusMeta = {
  PENDING_PAYMENT: {
    label: '待支付',
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
  return competition?.status === 'PUBLISHED' || competition?.status === 'ARCHIVED'
}

export function isEntryResultPublished(entry) {
  return Boolean(entry?.published) || entry?.status === 'RESULT_PUBLISHED'
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
  return { label: '提交酒款', to: `/portal/submit?competitionId=${competition.id}` }
}

export function entryPrimaryAction(entry) {
  if (!entry) {
    return { label: '浏览开放赛事', to: '/portal/events' }
  }
  const paymentPath = `/portal/payment?entryId=${entry.id}`
  if (entry.status === 'PENDING_PAYMENT' || entry.paymentStatus === 'UNPAID') {
    return { label: '去支付', to: paymentPath }
  }
  if (isEntryResultPublished(entry)) {
    return { label: '查看结果', to: entryResultPath(entry) }
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
    || entries.find((entry) => isEntryResultPublished(entry))
    || entries.find((entry) => entry.status === 'STORED')
    || null
}

export function entryTimeline(entry) {
  const paid = entry?.paymentStatus === 'PAID' || entry?.canDownloadLabel
  const resultPublished = isEntryResultPublished(entry)
  const stored = Boolean(entry?.stored) || entry?.status === 'STORED' || resultPublished
  return [
    { label: '提交资料', done: true, hint: entry?.submittedAt || '已提交' },
    { label: '支付报名费', done: paid, hint: paid ? '已支付' : '待支付' },
    { label: '标签可用', done: paid, hint: paid ? '可下载并贴在酒瓶或外箱' : '支付成功后开放下载' },
    { label: '酒样入库', done: stored, hint: stored ? '已入库' : '等待主办方收样确认' },
    { label: '结果发布', done: resultPublished, hint: resultPublished ? '结果已发布' : '等待主办方发布结果' },
  ]
}

export function entrySummaryForCompetition(competitionId, entries) {
  const scoped = entries.filter((entry) => entry.competitionId === competitionId)
  return {
    submitted: scoped.length,
    pendingPayment: scoped.filter((entry) => entry.status === 'PENDING_PAYMENT').length,
    registered: scoped.filter((entry) => entry.status === 'REGISTERED').length,
    stored: scoped.filter((entry) => entry.status === 'STORED' || isEntryResultPublished(entry)).length,
    result: scoped.filter((entry) => isEntryResultPublished(entry)).length,
  }
}

export function nextActionText(entry) {
  if (entry.status === 'PENDING_PAYMENT' || entry.paymentStatus === 'UNPAID') {
    return '待支付报名费'
  }
  if (entry.status === 'REGISTERED') {
    return entry.stored ? '等待评审' : '下载标签并送样'
  }
  if (isEntryResultPublished(entry)) {
    return '查看结果'
  }
  if (entry.status === 'STORED') {
    return '等待结果发布'
  }
  return '查看详情'
}
