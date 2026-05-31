import {
  competition,
  entries,
  getFallbackPerson,
  getPersonByPhone,
  getRoleLabel,
  scoreConfigs,
  seedScores,
} from './mockJudgeData'

const MOCK_USER_KEY = 'judge_mock_user'
const MOCK_SCORES_KEY = 'judge_mock_scores'

function wait(data) {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(structuredClone(data)), 140)
  })
}

function readUser() {
  const raw = localStorage.getItem(MOCK_USER_KEY)
  return raw ? JSON.parse(raw) : null
}

function writeUser(user) {
  localStorage.setItem(MOCK_USER_KEY, JSON.stringify(user))
}

function readScores() {
  const raw = localStorage.getItem(MOCK_SCORES_KEY)
  if (raw) return JSON.parse(raw)
  const seeded = seedScores()
  localStorage.setItem(MOCK_SCORES_KEY, JSON.stringify(seeded))
  return seeded
}

function writeScores(scores) {
  localStorage.setItem(MOCK_SCORES_KEY, JSON.stringify(scores))
}

function nowLabel() {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date())
}

function normalizeScore(payload, existing = {}) {
  const user = readUser()
  const dimensions = payload.dimensions || []
  const totalScore = Number(
    (payload.totalScore ?? dimensions.reduce((sum, item) => sum + Number(item.score || 0), 0)).toFixed(1),
  )

  return {
    ...existing,
    id: existing.id || `score-${Date.now()}`,
    beerUuid: payload.beerUuid,
    judgeName: user?.displayName || '现场评审',
    judgeRoleType: payload.judgeRoleType || user?.role || 'CROSS',
    roleLabel: getRoleLabel(payload.judgeRoleType || user?.role || 'CROSS'),
    dimensions,
    totalScore,
    comments: payload.comments,
    submittedAt: nowLabel(),
    locked: false,
    finalFlag: Boolean(payload.finalFlag),
    advanced: Boolean(payload.advanced),
  }
}

export function sendSmsCode() {
  return wait({ ok: true })
}

export function login(payload) {
  const person = getPersonByPhone(payload.phone) || getFallbackPerson(payload.phone)
  const user = {
    ...person,
    roleLabel: getRoleLabel(person.role),
    competitionId: competition.id,
    competitionName: competition.name,
  }
  writeUser(user)

  return wait({
    token: `mock-token-${person.phone}`,
    displayName: person.displayName,
    role: person.role,
  })
}

export function fetchMe() {
  const user = readUser()
  return wait({
    ...user,
    currentCompetition: competition.name,
    roleLabel: getRoleLabel(user?.role),
  })
}

export function fetchCompetitions() {
  const user = readUser()
  const scores = readScores()
  const mine = scores.filter((item) => item.judgeName === user?.displayName && !item.finalFlag)
  const finalized = scores.filter((item) => item.finalFlag)
  return wait([
    {
      ...competition,
      role: user?.role,
      roleLabel: getRoleLabel(user?.role),
      tableName: user?.tableName || competition.tableName,
      totalEntries: entries.length,
      myScoredCount: mine.length,
      finalizedCount: finalized.length,
    },
  ])
}

export function fetchEntry(uuid) {
  const entry = entries.find((item) => item.uuid.toUpperCase() === String(uuid).toUpperCase())
  if (!entry) {
    return Promise.reject(new Error('没有找到这个酒款编号'))
  }
  const scores = readScores()
  const finalScore = scores.find((item) => item.beerUuid === entry.uuid && item.finalFlag)
  return wait({
    ...entry,
    locked: Boolean(finalScore),
    finalScore: finalScore?.totalScore,
    advanced: Boolean(finalScore?.advanced),
  })
}

export function fetchEntries() {
  const scores = readScores()
  return wait(entries.map((entry) => {
    const finalScore = scores.find((item) => item.beerUuid === entry.uuid && item.finalFlag)
    const tableCount = scores.filter((item) => item.beerUuid === entry.uuid && !item.finalFlag).length
    return {
      ...entry,
      finalScore: finalScore?.totalScore,
      advanced: Boolean(finalScore?.advanced),
      finalized: Boolean(finalScore),
      tableScoreCount: tableCount,
    }
  }))
}

export function fetchScoreConfig(role) {
  return wait(scoreConfigs[role] || scoreConfigs.CROSS)
}

export function fetchMyScores() {
  const user = readUser()
  const scores = readScores()
  return wait(scores
    .filter((item) => item.judgeName === user?.displayName && !item.finalFlag)
    .map((score) => {
      const entry = entries.find((item) => item.uuid === score.beerUuid)
      const finalScore = scores.find((item) => item.beerUuid === score.beerUuid && item.finalFlag)
      return {
        ...score,
        categoryName: entry?.categoryName,
        style: entry?.style,
        locked: Boolean(finalScore),
      }
    }))
}

export function fetchMyScore(uuid) {
  const user = readUser()
  const scores = readScores()
  return wait(scores.find((item) => (
    item.beerUuid === uuid
    && item.judgeName === user?.displayName
    && !item.finalFlag
  )) || null)
}

export function createScore(payload) {
  const scores = readScores()
  const user = readUser()
  const existingIndex = scores.findIndex((item) => (
    item.beerUuid === payload.beerUuid
    && item.judgeName === user?.displayName
    && !item.finalFlag
  ))
  const next = normalizeScore(payload, existingIndex >= 0 ? scores[existingIndex] : {})
  if (existingIndex >= 0) {
    scores.splice(existingIndex, 1, next)
  } else {
    scores.push(next)
  }
  writeScores(scores)
  return wait(next)
}

export function updateScore(id, payload) {
  const scores = readScores()
  const index = scores.findIndex((item) => item.id === id)
  if (index < 0) return createScore(payload)
  const next = normalizeScore(payload, scores[index])
  scores.splice(index, 1, next)
  writeScores(scores)
  return wait(next)
}

export function fetchTableScores(uuid) {
  const scores = readScores()
  return wait(scores.filter((item) => item.beerUuid === uuid && !item.finalFlag))
}

export function fetchCaptainBoard() {
  const scores = readScores()
  return wait({
    competition,
    entries: entries.map((entry) => {
      const memberScores = scores.filter((item) => item.beerUuid === entry.uuid && !item.finalFlag)
      const finalScore = scores.find((item) => item.beerUuid === entry.uuid && item.finalFlag)
      return {
        ...entry,
        memberScores,
        submittedCount: memberScores.length,
        expectedCount: 2,
        finalized: Boolean(finalScore),
        finalScore: finalScore?.totalScore,
        advanced: Boolean(finalScore?.advanced),
      }
    }),
  })
}

export function finalizeTableScore(uuid, payload) {
  const scores = readScores()
  const existingIndex = scores.findIndex((item) => item.beerUuid === uuid && item.finalFlag)
  const finalPayload = {
    beerUuid: uuid,
    judgeRoleType: 'CAPTAIN',
    dimensions: payload.dimensions,
    totalScore: payload.consensusScore,
    comments: payload.comments,
    finalFlag: true,
    advanced: payload.advanced,
  }
  const next = normalizeScore(finalPayload, existingIndex >= 0 ? scores[existingIndex] : {})
  next.locked = true
  if (existingIndex >= 0) {
    scores.splice(existingIndex, 1, next)
  } else {
    scores.push(next)
  }
  writeScores(scores)
  return wait(next)
}

export function updateAdvancedList(uuids) {
  const scores = readScores().map((item) => (
    item.finalFlag
      ? { ...item, advanced: uuids.includes(item.beerUuid) }
      : item
  ))
  writeScores(scores)
  return wait({ advancedUuids: uuids })
}
