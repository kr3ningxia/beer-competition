<template>
  <main :class="['live-board', densityClass]">
    <header class="board-header">
      <section class="title-zone">
        <div class="stage-line">
          <span>{{ board.roundName }}</span>
          <b>{{ board.statusText }}</b>
          <em>{{ board.roundTypeText }}</em>
        </div>
        <h1>{{ board.title }}</h1>
      </section>

      <section :class="['notice-panel', board.noticeTone]" aria-label="现场状态">
        <span class="notice-dot" />
        <strong>{{ board.noticeTitle }}</strong>
        <p>{{ board.noticeText }}</p>
      </section>
    </header>

    <section class="metric-strip" aria-label="本轮总览">
      <article v-for="metric in board.metrics" :key="metric.label" :class="['metric-card', metric.tone]">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.unit }}</small>
      </article>
    </section>

    <section class="table-grid" aria-label="评审桌状态">
      <article v-for="table in board.tables" :key="table.id" :class="['table-card', table.tone]">
        <header class="table-head">
          <div class="table-name">
            <h2>{{ table.displayName }}</h2>
            <span>{{ table.entryCount }} 款酒</span>
          </div>
          <b class="status-pill">{{ table.statusText }}</b>
        </header>

        <section class="progress-zone">
          <div class="progress-main">
            <span>{{ table.primaryLabel }}</span>
            <strong>{{ table.primaryValue }}</strong>
          </div>
          <div class="progress-track" aria-hidden="true">
            <i :style="{ width: `${table.visualPercent}%` }" />
          </div>
        </section>

        <section class="table-facts">
          <span v-for="stat in table.stats" :key="stat.label" :class="{ accent: stat.accent }">
            <small>{{ stat.label }}</small>
            <strong>{{ stat.value }}</strong>
          </span>
        </section>

        <p v-if="table.issueText" class="issue-text">{{ table.issueText }}</p>
      </article>

      <article v-if="!board.tables.length" class="empty-card">
        <strong>等待轮次发布</strong>
        <span>轮次发布后，评审桌状态会显示在这里。</span>
      </article>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCompetitionProgress, fetchCompetitions } from '@/api/admin'

const REFRESH_SECONDS = 10

const route = useRoute()
const loading = ref(false)
const detail = ref(null)
const selectedCompetitionId = ref(route.query.competitionId || '')
let refreshTimer = null

const board = computed(() => buildBoard(detail.value))
const densityClass = computed(() => `tables-${Math.min(Math.max(board.value.tables.length || 1, 1), 8)}`)

onMounted(async () => {
  await ensureCompetition()
  await refreshBoard()
  startTimers()
})

onUnmounted(() => {
  window.clearInterval(refreshTimer)
})

async function ensureCompetition() {
  if (selectedCompetitionId.value) return
  const competitions = await fetchCompetitions()
  const target = (competitions || []).find((item) => ['JUDGING', 'JUDGING_PREP'].includes(item.status))
    || (competitions || [])[0]
  selectedCompetitionId.value = target?.id || ''
}

function startTimers() {
  refreshTimer = window.setInterval(refreshBoard, REFRESH_SECONDS * 1000)
}

async function refreshBoard() {
  if (!selectedCompetitionId.value || loading.value) return
  loading.value = true
  try {
    detail.value = normalizeDetail(await fetchCompetitionProgress(selectedCompetitionId.value))
  } finally {
    loading.value = false
  }
}

function normalizeDetail(data) {
  return {
    ...data,
    date: data?.competitionDate || data?.date || '',
    entryPool: data?.entryPool || data?.entries || [],
    rounds: data?.rounds || [],
    progressSummary: data?.progressSummary || {},
  }
}

function buildBoard(data) {
  if (!data) return buildEmptyBoard()
  const rounds = data.rounds || []
  const currentRound = data.currentRound || rounds.find((round) => ['PUBLISHED', 'IN_PROGRESS', 'SUBMITTED'].includes(round.status))
    || rounds[rounds.length - 1]
    || rounds[0]
  const tables = (currentRound?.tables || []).slice(0, 8).map((table) => buildTableTile(currentRound, table))
  const notices = collectNotices(currentRound, tables)
  const primaryNotice = notices[0] || buildDefaultNotice(currentRound, tables)
  return {
    title: data.name || '现场比赛',
    roundName: currentRound?.name || '未创建轮次',
    roundTypeText: currentRound?.type === 'RANKING' ? '排序轮' : '评分轮',
    statusText: resolveStatusText(data.status, currentRound),
    metrics: buildMetrics(currentRound, tables),
    tables,
    noticeTone: primaryNotice.tone,
    noticeTitle: primaryNotice.title,
    noticeText: primaryNotice.text,
  }
}

function buildEmptyBoard() {
  return {
    title: '现场比赛',
    roundName: '未创建轮次',
    roundTypeText: '等待发布',
    statusText: '现场准备中',
    metrics: [
      { label: '本轮酒款', value: '-', unit: '款', tone: 'neutral' },
      { label: '评审桌', value: '-', unit: '桌', tone: 'neutral' },
      { label: '已提交', value: '-', unit: '份', tone: 'gold' },
      { label: '完成桌数', value: '-', unit: '桌', tone: 'success' },
    ],
    tables: [],
    noticeTone: 'warning',
    noticeTitle: '等待轮次',
    noticeText: '当前比赛还没有发布可投屏的评审轮次。',
  }
}

function buildMetrics(round, tables) {
  const entryCount = countRoundEntries(round)
  const doneTables = tables.filter((table) => table.done).length
  if (round?.type === 'RANKING') {
    const selected = tables.reduce((sum, table) => sum + table.selectedCount, 0)
    const target = tables.reduce((sum, table) => sum + table.targetCount, 0)
    return [
      { label: '候选酒款', value: entryCount, unit: '款', tone: 'neutral' },
      { label: '评审桌', value: tables.length, unit: '桌', tone: 'neutral' },
      { label: '已排序', value: `${selected} / ${target}`, unit: '席位', tone: selected >= target && target > 0 ? 'success' : 'gold' },
      { label: '完成桌数', value: `${doneTables} / ${tables.length}`, unit: '桌', tone: doneTables === tables.length && tables.length ? 'success' : 'warning' },
    ]
  }
  const submitted = tables.reduce((sum, table) => sum + table.submittedCount, 0)
  const total = tables.reduce((sum, table) => sum + table.submittedTotal, 0)
  return [
    { label: '本轮酒款', value: entryCount, unit: '款', tone: 'neutral' },
    { label: '评审桌', value: tables.length, unit: '桌', tone: 'neutral' },
    { label: '已提交', value: `${submitted} / ${total}`, unit: '份', tone: submitted >= total && total > 0 ? 'success' : 'gold' },
    { label: '完成桌数', value: `${doneTables} / ${tables.length}`, unit: '桌', tone: doneTables === tables.length && tables.length ? 'success' : 'warning' },
  ]
}

function buildTableTile(round, table) {
  const entryCount = table.entryUuids?.length || 0
  const targetCount = Number(table.targetCount || 0)
  const displayName = table.name || '未命名桌'
  if (round?.type === 'RANKING') {
    const selectedCount = (table.rankings || []).filter((slot) => slot.uuid).length
    const issueText = getTableIssue(table)
    const done = !issueText && selectedCount >= targetCount && targetCount > 0
    const statusText = issueText ? '需关注' : done ? '已完成' : selectedCount ? '排序中' : '待排序'
    return {
      id: table.id || displayName,
      displayName,
      entryCount,
      targetCount,
      selectedCount,
      submittedCount: selectedCount,
      submittedTotal: Math.max(targetCount, 0),
      primaryLabel: '排序进度',
      primaryValue: `${selectedCount} / ${targetCount}`,
      visualPercent: targetCount ? normalizePercent(selectedCount * 100 / targetCount) : 0,
      statusText,
      done,
      issueText,
      tone: issueText ? 'danger' : done ? 'success' : selectedCount ? 'active' : 'warning',
      stats: [
        { label: '候选', value: `${entryCount} 款` },
        { label: '桌长', value: selectedCount ? `${selectedCount} / ${targetCount}` : '未开始', accent: selectedCount > 0 },
        { label: '目标', value: `${targetCount} 款`, accent: true },
      ],
    }
  }

  const judgeProgress = normalizePercent(table.judgeProgress)
  const captainProgress = normalizePercent(table.captainProgress)
  const selectedCount = Number(table.advancedCount || 0)
  const submittedTotal = entryCount
  const submittedCount = estimateCount(submittedTotal, judgeProgress)
  const captainConfirmedCount = estimateCount(entryCount, captainProgress)
  const issueText = getTableIssue(table)
  const done = !issueText && captainProgress >= 100
  let statusText = '等待评分'
  let tone = 'idle'
  if (issueText) {
    statusText = '需关注'
    tone = 'danger'
  } else if (done) {
    statusText = '已完成'
    tone = 'success'
  } else if (captainProgress > 0) {
    statusText = '确认中'
    tone = 'warning'
  } else if (judgeProgress >= 100) {
    statusText = '待确认'
    tone = 'warning'
  } else if (judgeProgress > 0) {
    statusText = '评分中'
    tone = 'active'
  }

  return {
    id: table.id || displayName,
    displayName,
    entryCount,
    targetCount,
    selectedCount,
    submittedCount,
    submittedTotal,
    captainConfirmedCount,
    primaryLabel: '评分提交',
    primaryValue: `${submittedCount} / ${submittedTotal}`,
    visualPercent: judgeProgress,
    statusText,
    done,
    issueText,
    tone,
    stats: [
      { label: '桌长', value: resolveCaptainText(captainProgress, captainConfirmedCount, entryCount), accent: captainProgress > 0 || judgeProgress >= 100 },
      { label: '晋级', value: `${targetCount} 款`, accent: true },
      { label: '进度', value: `${judgeProgress}%` },
    ],
  }
}

function resolveCaptainText(progress, confirmedCount, entryCount) {
  if (progress >= 100) return '已完成'
  if (progress > 0) return `${confirmedCount} / ${entryCount}`
  return '未开始'
}

function getTableIssue(table) {
  if (!table.captainPublicId && !table.captainName && !table.captain?.name) return '桌长未指定'
  if (!(table.entryUuids || []).length) return '尚未分配酒款'
  if (!Number(table.targetCount || 0)) return '晋级名额未设置'
  return ''
}

function collectNotices(round, tables) {
  if (!round) return [{ title: '等待轮次', text: '当前比赛还没有发布可投屏的评审轮次。', tone: 'warning' }]
  const issue = tables.find((table) => table.issueText)
  if (issue) return [{ title: '需现场处理', text: `${issue.displayName}${issue.issueText}，请工作人员处理。`, tone: 'danger' }]

  const waitingCaptain = tables.find((table) => table.statusText === '待确认')
  const scoring = tables.find((table) => table.statusText === '评分中')
  const captainWorking = tables.find((table) => table.statusText === '确认中')
  if (scoring) {
    const pending = Math.max(scoring.submittedTotal - scoring.submittedCount, 0)
    return [{ title: '评审进行中', text: `${scoring.displayName}还有 ${pending} 份评分待提交。`, tone: 'gold' }]
  }
  if (waitingCaptain) {
    return [{ title: '等待桌长', text: `${waitingCaptain.displayName}评分已完成，等待桌长确认。`, tone: 'warning' }]
  }
  if (captainWorking) {
    return [{ title: '桌长确认中', text: `${captainWorking.displayName}桌长正在确认本桌结果。`, tone: 'warning' }]
  }
  if (tables.length && tables.every((table) => table.done)) {
    return [{ title: '本轮已完成', text: '所有评审桌已完成，请等待主办方确认本轮结果。', tone: 'success' }]
  }
  return []
}

function buildDefaultNotice(round, tables) {
  if (!round) return { title: '等待轮次', text: '当前比赛还没有发布可投屏的评审轮次。', tone: 'warning' }
  if (!tables.length) return { title: '等待分桌', text: '当前轮次还没有评审桌。', tone: 'warning' }
  return { title: '现场正常', text: '评审桌状态正常，暂无需要现场处理的事项。', tone: 'success' }
}

function resolveStatusText(status, round) {
  if (round?.status === 'PUBLISHED') return '评分进行中'
  if (round?.status === 'IN_PROGRESS') return '处理中'
  if (round?.status === 'SUBMITTED') return '等待确认'
  if (round?.status === 'LOCKED') return '本轮已锁定'
  if (status === 'JUDGING') return '评分进行中'
  return '现场准备中'
}

function countRoundEntries(round) {
  return new Set((round?.tables || []).flatMap((table) => table.entryUuids || [])).size
}

function estimateCount(total, percent) {
  if (!total) return 0
  return Math.min(total, Math.round(total * normalizePercent(percent) / 100))
}

function normalizePercent(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number)) return 0
  return Math.max(0, Math.min(100, Math.round(number)))
}
</script>

<style scoped>
.live-board {
  --bg: #0d0f0e;
  --panel: #171a17;
  --panel-soft: #20231e;
  --line: rgba(239, 191, 91, 0.22);
  --line-strong: rgba(239, 191, 91, 0.42);
  --text: #fff8e8;
  --muted: #d4c5a2;
  --dim: #92876f;
  --gold: #f2bf4f;
  --gold-soft: #ffe08d;
  --green: #a8d46f;
  --blue: #75b7ff;
  --orange: #ff9d45;
  --danger: #ff665a;
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 18px;
  padding: 32px 40px 34px;
  overflow: hidden;
  color: var(--text);
  background:
    linear-gradient(90deg, rgba(242, 191, 79, 0.07), transparent 42%),
    linear-gradient(180deg, #18140d 0%, var(--bg) 62%, #080908 100%);
  font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif;
  letter-spacing: 0;
}

.live-board::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.13;
  background-image:
    linear-gradient(rgba(255, 232, 178, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 232, 178, 0.12) 1px, transparent 1px);
  background-size: 56px 56px;
}

.live-board::after {
  content: "";
  position: absolute;
  inset: auto 40px 22px;
  height: 3px;
  pointer-events: none;
  background: linear-gradient(90deg, var(--gold), rgba(168, 212, 111, 0.75), transparent);
}

.live-board > * {
  position: relative;
  z-index: 1;
}

.board-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 33%);
  gap: 28px;
  align-items: stretch;
}

.title-zone {
  min-width: 0;
}

.stage-line {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.stage-line span,
.stage-line b,
.stage-line em {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.24);
  font-size: 18px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.stage-line span {
  color: var(--gold-soft);
  border-color: var(--line-strong);
  background: rgba(242, 191, 79, 0.12);
}

.stage-line b {
  color: var(--green);
}

.stage-line em {
  color: var(--muted);
}

.title-zone h1 {
  max-width: 1120px;
  margin: 14px 0 0;
  overflow: hidden;
  color: var(--text);
  font-size: 58px;
  line-height: 1.02;
  font-weight: 950;
  letter-spacing: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-shadow: 0 12px 30px rgba(0, 0, 0, 0.45);
}

.notice-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  grid-template-rows: auto 1fr;
  gap: 6px 14px;
  align-content: center;
  min-width: 0;
  min-height: 104px;
  padding: 18px 22px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(18, 20, 18, 0.82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.notice-dot {
  grid-row: 1 / 3;
  align-self: center;
  width: 16px;
  height: 16px;
  border-radius: 999px;
  background: var(--gold);
  box-shadow: 0 0 24px rgba(242, 191, 79, 0.45);
}

.notice-panel strong {
  color: var(--gold-soft);
  font-size: 22px;
  line-height: 1;
  font-weight: 950;
}

.notice-panel p {
  margin: 0;
  overflow: hidden;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.35;
  font-weight: 850;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notice-panel.success .notice-dot {
  background: var(--green);
  box-shadow: 0 0 24px rgba(168, 212, 111, 0.45);
}

.notice-panel.success strong {
  color: var(--green);
}

.notice-panel.warning .notice-dot {
  background: var(--orange);
  box-shadow: 0 0 24px rgba(255, 157, 69, 0.45);
}

.notice-panel.warning strong {
  color: var(--orange);
}

.notice-panel.danger {
  border-color: rgba(255, 102, 90, 0.48);
  background: rgba(42, 15, 12, 0.86);
}

.notice-panel.danger .notice-dot {
  background: var(--danger);
  box-shadow: 0 0 28px rgba(255, 102, 90, 0.55);
}

.notice-panel.danger strong {
  color: #ff9b92;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 10px;
  min-width: 0;
  min-height: 90px;
  padding: 16px 20px 15px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(23, 26, 23, 0.82);
}

.metric-card span {
  grid-column: 1 / 3;
  color: var(--muted);
  font-size: 18px;
  line-height: 1;
  font-weight: 900;
}

.metric-card strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-size: 42px;
  line-height: 1;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.metric-card small {
  align-self: end;
  color: var(--dim);
  font-size: 16px;
  font-weight: 900;
}

.metric-card.gold strong,
.metric-card.warning strong {
  color: var(--gold-soft);
}

.metric-card.success strong {
  color: var(--green);
}

.table-grid {
  display: grid;
  gap: 16px;
  min-height: 0;
}

.tables-1 .table-grid {
  grid-template-columns: minmax(0, 1fr);
}

.tables-2 .table-grid,
.tables-3 .table-grid,
.tables-4 .table-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.tables-5 .table-grid,
.tables-6 .table-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.tables-7 .table-grid,
.tables-8 .table-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.tables-1 .table-grid,
.tables-2 .table-grid {
  grid-auto-rows: minmax(0, 1fr);
  align-content: stretch;
}

.tables-3 .table-grid,
.tables-4 .table-grid,
.tables-5 .table-grid,
.tables-6 .table-grid,
.tables-7 .table-grid,
.tables-8 .table-grid {
  grid-auto-rows: minmax(0, 1fr);
}

.table-card {
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  gap: 16px;
  min-width: 0;
  min-height: 0;
  padding: 24px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(32, 35, 30, 0.94), rgba(15, 17, 15, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05), 0 22px 46px rgba(0, 0, 0, 0.22);
}

.table-card.danger {
  border-color: rgba(255, 102, 90, 0.62);
  background: linear-gradient(180deg, rgba(48, 20, 16, 0.94), rgba(18, 13, 12, 0.98));
}

.table-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  min-width: 0;
}

.table-name {
  min-width: 0;
}

.table-name h2 {
  margin: 0;
  overflow: hidden;
  color: var(--text);
  font-size: 68px;
  line-height: 0.96;
  font-weight: 950;
  letter-spacing: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.table-name span {
  display: block;
  margin-top: 7px;
  color: var(--muted);
  font-size: 20px;
  line-height: 1;
  font-weight: 900;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  min-width: 104px;
  height: 42px;
  padding: 0 14px;
  color: #11130f;
  border-radius: 999px;
  background: var(--gold);
  font-size: 18px;
  font-weight: 950;
  white-space: nowrap;
}

.table-card.success .status-pill {
  background: var(--green);
}

.table-card.active .status-pill {
  background: var(--blue);
}

.table-card.warning .status-pill {
  background: var(--orange);
}

.table-card.danger .status-pill {
  background: var(--danger);
  color: #fff8f0;
}

.progress-zone {
  display: grid;
  align-content: center;
  gap: 18px;
  min-width: 0;
  min-height: 0;
}

.progress-main {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
  min-width: 0;
}

.progress-main span {
  flex: 0 0 auto;
  color: var(--muted);
  font-size: 22px;
  line-height: 1.1;
  font-weight: 900;
}

.progress-main strong {
  min-width: 0;
  overflow: hidden;
  color: var(--gold-soft);
  font-size: 72px;
  line-height: 0.92;
  font-weight: 950;
  text-align: right;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.table-card.success .progress-main strong {
  color: var(--green);
}

.table-card.active .progress-main strong {
  color: #a9d5ff;
}

.table-card.warning .progress-main strong {
  color: #ffc07a;
}

.table-card.danger .progress-main strong {
  color: #ff9b92;
}

.progress-track {
  height: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}

.progress-track i {
  display: block;
  height: 100%;
  min-width: 6px;
  max-width: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--gold), var(--gold-soft));
  transition: width 0.28s ease;
}

.table-card.success .progress-track i {
  background: linear-gradient(90deg, #78b84e, var(--green));
}

.table-card.active .progress-track i {
  background: linear-gradient(90deg, #4f9dea, var(--blue));
}

.table-card.warning .progress-track i {
  background: linear-gradient(90deg, #ff7d34, var(--orange));
}

.table-card.danger .progress-track i {
  background: linear-gradient(90deg, #e7443d, var(--danger));
}

.table-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.table-facts span {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.22);
}

.table-facts small {
  color: var(--dim);
  font-size: 16px;
  line-height: 1;
  font-weight: 900;
}

.table-facts strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-size: 28px;
  line-height: 1;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.table-facts .accent strong {
  color: var(--gold-soft);
}

.table-card.success .table-facts .accent strong {
  color: var(--green);
}

.table-card.warning .table-facts .accent strong {
  color: #ffc07a;
}

.table-card.danger .table-facts .accent strong {
  color: #ff9b92;
}

.issue-text {
  margin: 0;
  color: #ffbbb4;
  font-size: 18px;
  font-weight: 900;
}

.empty-card {
  display: grid;
  place-content: center;
  gap: 12px;
  min-height: 340px;
  color: var(--muted);
  text-align: center;
  border: 1px dashed var(--line-strong);
  border-radius: 8px;
  background: rgba(23, 26, 23, 0.65);
}

.empty-card strong {
  color: var(--text);
  font-size: 42px;
}

.empty-card span {
  font-size: 20px;
  font-weight: 850;
}

.tables-1 .table-card {
  padding: 38px 42px;
}

.tables-1 .table-name h2 {
  font-size: 124px;
}

.tables-1 .table-name span {
  font-size: 28px;
}

.tables-1 .status-pill {
  min-width: 146px;
  height: 56px;
  font-size: 24px;
}

.tables-1 .progress-main span {
  font-size: 30px;
}

.tables-1 .progress-main strong {
  font-size: 126px;
}

.tables-1 .progress-track {
  height: 28px;
}

.tables-1 .table-facts strong {
  font-size: 46px;
}

.tables-1 .table-facts small {
  font-size: 20px;
}

.tables-3 .table-card,
.tables-4 .table-card {
  padding: 22px;
}

.tables-3 .table-name h2,
.tables-4 .table-name h2 {
  font-size: 58px;
}

.tables-3 .progress-main strong,
.tables-4 .progress-main strong {
  font-size: 58px;
}

.live-board.tables-5,
.live-board.tables-6,
.live-board.tables-7,
.live-board.tables-8 {
  gap: 14px;
}

.tables-5 .table-card,
.tables-6 .table-card {
  gap: 12px;
  padding: 18px;
}

.tables-5 .table-name h2,
.tables-6 .table-name h2 {
  font-size: 48px;
}

.tables-5 .table-name span,
.tables-6 .table-name span {
  font-size: 17px;
}

.tables-5 .status-pill,
.tables-6 .status-pill {
  min-width: 86px;
  height: 34px;
  font-size: 15px;
}

.tables-5 .progress-main,
.tables-6 .progress-main {
  display: grid;
  gap: 8px;
}

.tables-5 .progress-main span,
.tables-6 .progress-main span {
  font-size: 18px;
}

.tables-5 .progress-main strong,
.tables-6 .progress-main strong {
  font-size: 48px;
  text-align: left;
}

.tables-5 .progress-track,
.tables-6 .progress-track {
  height: 14px;
}

.tables-5 .table-facts,
.tables-6 .table-facts {
  gap: 8px;
}

.tables-5 .table-facts span,
.tables-6 .table-facts span {
  padding: 10px;
}

.tables-5 .table-facts small,
.tables-6 .table-facts small {
  font-size: 13px;
}

.tables-5 .table-facts strong,
.tables-6 .table-facts strong {
  font-size: 22px;
}

.tables-7 .table-card,
.tables-8 .table-card {
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 10px;
  padding: 16px;
}

.tables-7 .table-head,
.tables-8 .table-head {
  align-items: flex-start;
  gap: 8px;
}

.tables-7 .table-name h2,
.tables-8 .table-name h2 {
  font-size: 42px;
}

.tables-7 .table-name span,
.tables-8 .table-name span {
  font-size: 15px;
}

.tables-7 .status-pill,
.tables-8 .status-pill {
  min-width: 76px;
  height: 30px;
  padding: 0 10px;
  font-size: 14px;
}

.tables-7 .progress-zone,
.tables-8 .progress-zone {
  gap: 10px;
  align-content: start;
}

.tables-7 .progress-main,
.tables-8 .progress-main {
  display: grid;
  gap: 6px;
}

.tables-7 .progress-main span,
.tables-8 .progress-main span {
  font-size: 16px;
}

.tables-7 .progress-main strong,
.tables-8 .progress-main strong {
  font-size: 40px;
  text-align: left;
}

.tables-7 .progress-track,
.tables-8 .progress-track {
  height: 12px;
}

.tables-7 .table-facts,
.tables-8 .table-facts {
  grid-template-columns: 1fr;
  gap: 7px;
}

.tables-7 .table-facts span,
.tables-8 .table-facts span {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
  gap: 8px;
  padding: 8px 10px;
}

.tables-7 .table-facts small,
.tables-8 .table-facts small {
  font-size: 13px;
}

.tables-7 .table-facts strong,
.tables-8 .table-facts strong {
  font-size: 19px;
}

@media (max-width: 1500px) {
  .live-board {
    gap: 14px;
    padding: 24px 30px 28px;
  }

  .board-header {
    grid-template-columns: minmax(0, 1fr) minmax(340px, 32%);
    gap: 20px;
  }

  .title-zone h1 {
    font-size: 48px;
  }

  .stage-line span,
  .stage-line b,
  .stage-line em {
    min-height: 32px;
    font-size: 16px;
  }

  .notice-panel {
    min-height: 92px;
    padding: 15px 18px;
  }

  .notice-panel strong {
    font-size: 20px;
  }

  .notice-panel p {
    font-size: 15px;
  }

  .metric-card {
    min-height: 78px;
    padding: 13px 16px 12px;
  }

  .metric-card span {
    font-size: 16px;
  }

  .metric-card strong {
    font-size: 34px;
  }

  .table-card {
    padding: 18px;
  }

  .table-name h2 {
    font-size: 54px;
  }

  .progress-main strong {
    font-size: 56px;
  }
}
</style>
