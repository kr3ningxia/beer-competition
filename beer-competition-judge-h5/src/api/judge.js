import {
  competition,
  entries,
  getFallbackPerson,
  getRoleLabel,
  seedScores,
} from './mockJudgeData'
import request from './request'
import { selectCurrentTask } from '@/utils/judgeTasks'

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

export function saveRankingDraft(roundTableId, payload) {
  return request.post(`/api/judge/round-tables/${roundTableId}/ranking-draft`, payload)
}

export function fetchScoreConfirmation(roundTableId) {
  return request.get(`/api/judge/round-tables/${roundTableId}/score-confirmation`)
}

export function confirmScoreRoundTable(roundTableId) {
  return request.post(`/api/judge/round-tables/${roundTableId}/score-confirmation`)
}

export async function fetchEntry(uuid) {
  const entry = await request.get(`/api/judge/entries/${uuid}`)
  return normalizeEntry(entry)
}

export async function resolveScanEntry(code) {
  const entry = await request.get('/api/judge/scan/resolve', { params: { code } })
  return normalizeEntry(entry)
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

export async function fetchScoreConfig(role, competitionId) {
  const config = await request.get('/api/judge/score-configs/current', {
    params: {
      role,
      competitionId,
    },
  })
  return normalizeScoreConfig(config)
}

export function submitScoreRoundTable(roundTableId) {
  return request.post(`/api/judge/round-tables/${roundTableId}/score-submit`)
}

export async function fetchMyScores() {
  const scores = await request.get('/api/judge/my-scores')
  return scores.map(normalizeScoreRecord)
}

export async function fetchMyScore(uuid) {
  const score = await request.get(`/api/judge/my-scores/${uuid}`)
  return score ? normalizeScoreRecord(score) : null
}

export async function createScore(payload) {
  const saved = await request.post('/api/judge/scores', payload)
  if (saved) return normalizeScoreRecord(saved)
  throw new Error('评分提交失败：后端未返回有效评分记录')
}

export async function updateScore(id, payload) {
  const saved = await request.put(`/api/judge/scores/${id}`, payload)
  if (saved) return normalizeScoreRecord(saved)
  throw new Error('评分更新失败：后端未返回有效评分记录')
}

export async function fetchTableScores(uuid) {
  const scores = await request.get(`/api/judge/table-scores/${uuid}`)
  return scores.map(normalizeScoreRecord)
}

export async function fetchCaptainBoard(roundTableId) {
  const tasks = await fetchJudgeTasks()
  const task = roundTableId
    ? tasks.find((item) => item.roundTableId === Number(roundTableId))
    : selectCurrentTask(tasks.filter((item) => item.taskType === 'CAPTAIN_FINALIZE'))
      || selectCurrentTask(tasks.filter((item) => item.taskType === 'RANKING_ROUND'))
  if (!task) return { competition: null, entries: [] }
  const [table, myScores] = await Promise.all([
    fetchRoundTable(task.roundTableId),
    fetchMyScores(),
  ])
  const myScoredUuids = new Set(myScores.map((score) => score.beerUuid))
  const entriesWithScores = await Promise.all((table.entries || []).map(async (entry) => {
    const tableScores = task.taskType === 'CAPTAIN_FINALIZE'
      ? await fetchTableScores(entry.uuid)
      : []
    const finalScore = tableScores.find((score) => score.finalFlag)
    const normalScores = tableScores.filter((score) => !score.finalFlag)
    return {
      ...entry,
      scored: myScoredUuids.has(entry.uuid),
      submittedCount: normalScores.length,
      expectedCount: table.expectedJudgeCount || 0,
      finalized: Boolean(finalScore || entry.advanced),
      finalScore: finalScore?.totalScore,
      advanced: Boolean(finalScore?.advanced || entry.advanced),
    }
  }))
  return {
    competition: {
      name: task.competitionName,
      competitionId: task.competitionId,
      flightName: task.roundName,
      tableName: task.tableName,
      taskType: task.taskType,
      roundTableId: task.roundTableId,
    },
    entries: entriesWithScores,
    rankings: table.rankings || [],
    roundTable: table,
    scoreConfirmation: table.scoreConfirmation || null,
  }
}

export async function finalizeTableScore(uuid, payload) {
  const saved = await request.post(`/api/judge/table-scores/${uuid}/finalize`, payload)
  return normalizeScoreRecord(saved)
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

function normalizeScoreConfig(config) {
  return {
    ...config,
    roleLabel: config.roleLabel || getRoleLabel(config.judgeRoleType),
    commentMinLength: Number(config.commentMinLength ?? config.minCommentLength ?? 0),
    dimensions: (config.dimensions || []).map((item) => ({
      ...item,
      maxScore: Number(item.maxScore || 0),
      notePlaceholder: item.notePlaceholder || item.notePrompt || '',
      description: item.description || item.notePrompt || '',
    })),
  }
}

function normalizeEntry(entry) {
  const canScore = entry.canScore ?? (entry.action === 'SCORE' || entry.action === 'CAPTAIN')
  const canFinalize = entry.canFinalize ?? (entry.action === 'CAPTAIN' || entry.action === 'RANKING')
  return {
    ...entry,
    canScore,
    canFinalize,
    scoreRoleType: entry.scoreRoleType || (entry.judgeRoleType === 'CAPTAIN' ? 'PROFESSIONAL' : entry.judgeRoleType),
  }
}

function normalizeScoreRecord(score) {
  return {
    ...score,
    finalFlag: Boolean(score.isFinal ?? score.finalFlag),
    advanced: Boolean(score.isAdvanced ?? score.advanced),
    locked: Boolean(score.locked),
    submittedAt: formatSubmittedAt(score.submittedAt),
  }
}

function formatSubmittedAt(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}
