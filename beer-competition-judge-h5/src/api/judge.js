import {
  competition,
  entries,
  getFallbackPerson,
  getRoleLabel,
  scoreConfigs,
  seedScores,
} from './mockJudgeData'
import request from './request'

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

function buildComments(dimensions, fallback = '') {
  const lines = dimensions
    .map((item) => ({
      label: item.label,
      note: String(item.note || '').trim(),
    }))
    .filter((item) => item.note)
    .map((item) => `${item.label}：${item.note}`)
  return lines.length ? lines.join('\n') : fallback
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
    comments: buildComments(dimensions, payload.comments),
    submittedAt: nowLabel(),
    locked: false,
    finalFlag: Boolean(payload.finalFlag),
    advanced: Boolean(payload.advanced),
  }
}

export function sendSmsCode(payload) {
  return request.post('/api/public/sms/send', payload)
}

export async function login(payload) {
  const data = await request.post('/api/public/judge/login', payload)
  writeUser({
    ...getFallbackPerson(payload.phone),
    id: data.userId,
    phone: payload.phone,
    displayName: data.displayName,
    status: data.status,
    profileRequired: data.profileRequired,
  })
  return data
}

export async function register(payload) {
  const data = await request.post('/api/public/judge/register', payload)
  writeUser({
    ...getFallbackPerson(payload.phone),
    id: data.userId,
    phone: payload.phone,
    displayName: data.displayName,
    status: data.status,
    profileRequired: data.profileRequired,
  })
  return data
}

export async function fetchMe() {
  const data = await request.get('/api/judge/me')
  const user = {
    ...readUser(),
    ...data,
    id: data.userId,
    displayName: data.displayName,
    role: data.judgeRoleType,
    roleLabel: data.roleLabel || getRoleLabel(data.judgeRoleType),
    competitionId: data.currentCompetitionId,
    competitionName: data.currentCompetition,
  }
  writeUser(user)
  return user
}

export function fetchProfile() {
  return request.get('/api/judge/profile')
}

export function updateProfile(payload) {
  return request.put('/api/judge/profile', payload)
}

export async function fetchCompetitions() {
  const data = await request.get('/api/judge/tasks')
  return data.map((item) => ({
    ...item,
    role: item.judgeRoleType,
    roleLabel: item.roleLabel || getRoleLabel(item.judgeRoleType),
    name: item.competitionName,
    flightName: item.roundName,
    tableName: item.tableName,
    totalEntries: item.totalEntries || 0,
    myScoredCount: item.completedCount || 0,
    finalizedCount: item.completedCount || 0,
  }))
}

export function fetchJudgeTasks() {
  return request.get('/api/judge/tasks')
}

export function fetchRoundTable(roundTableId) {
  return request.get(`/api/judge/round-tables/${roundTableId}`)
}

export function submitRanking(roundTableId, payload) {
  return request.post(`/api/judge/round-tables/${roundTableId}/ranking`, payload)
}

export async function fetchEntry(uuid) {
  const entry = await request.get(`/api/judge/entries/${uuid}`)
  const scores = readScores()
  const finalScore = scores.find((item) => item.beerUuid === entry.uuid && item.finalFlag)
  return {
    ...entry,
    locked: Boolean(finalScore),
    finalScore: finalScore?.totalScore,
    advanced: Boolean(finalScore?.advanced),
  }
}

export async function resolveScanEntry(code) {
  const entry = await request.get('/api/judge/scan/resolve', { params: { code } })
  const scores = readScores()
  const finalScore = scores.find((item) => item.beerUuid === entry.uuid && item.finalFlag)
  return {
    ...entry,
    locked: Boolean(entry.locked || finalScore),
    finalScore: finalScore?.totalScore,
    advanced: Boolean(finalScore?.advanced),
  }
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

export async function createScore(payload) {
  const saved = await request.post('/api/judge/scores', payload)
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
  return saved || next
}

export async function updateScore(id, payload) {
  const saved = await request.put(`/api/judge/scores/${id}`, payload)
  const scores = readScores()
  const index = scores.findIndex((item) => item.id === id)
  if (index < 0) return createScore(payload)
  const next = normalizeScore(payload, scores[index])
  scores.splice(index, 1, next)
  writeScores(scores)
  return saved || next
}

export function fetchTableScores(uuid) {
  const scores = readScores()
  return wait(scores.filter((item) => item.beerUuid === uuid && !item.finalFlag))
}

export async function fetchCaptainBoard(roundTableId) {
  const tasks = await fetchJudgeTasks()
  const task = roundTableId
    ? tasks.find((item) => item.roundTableId === Number(roundTableId))
    : tasks.find((item) => item.taskType === 'CAPTAIN_FINALIZE') || tasks.find((item) => item.taskType === 'RANKING_ROUND')
  if (!task) return { competition: null, entries: [] }
  const table = await fetchRoundTable(task.roundTableId)
  return {
    competition: {
      name: task.competitionName,
      flightName: task.roundName,
      tableName: task.tableName,
      taskType: task.taskType,
      roundTableId: task.roundTableId,
    },
    entries: (table.entries || []).map((entry) => ({
      ...entry,
      submittedCount: 0,
      expectedCount: 0,
      finalized: Boolean(entry.advanced),
      advanced: Boolean(entry.advanced),
    })),
    rankings: table.rankings || [],
    roundTable: table,
  }
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
