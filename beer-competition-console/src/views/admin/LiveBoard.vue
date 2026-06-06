<template>
  <main :class="['live-board', densityClass]">
    <header class="board-header">
      <section class="title-zone">
        <div class="top-line">
          <button type="button" class="back-button" @click="goBack">← 返回</button>
          <span class="brand-mark">CRAFT BEER AWARDS</span>
          <i aria-hidden="true" />
          <span class="screen-name">现场评审进度</span>
        </div>

        <h1>{{ board.title }}</h1>

        <div class="stage-line">
          <span>{{ board.roundName }}</span>
          <em>{{ board.roundTypeText }}</em>
          <b><i aria-hidden="true" />{{ board.statusText }}</b>
        </div>
      </section>

      <section class="notice-stack" aria-label="现场状态">
        <article v-for="notice in board.notices" :key="`${notice.title}-${notice.text}`" :class="['notice-panel', notice.tone]">
          <span class="notice-dot" />
          <p><strong>{{ notice.title }}</strong>{{ notice.text }}</p>
        </article>
      </section>
    </header>

    <section class="metric-strip" aria-label="本轮总览">
      <article v-for="metric in board.metrics" :key="metric.label" :class="['metric-card', metric.tone]">
        <i aria-hidden="true" />
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.unit }}</small>
      </article>
    </section>

    <section class="table-grid" aria-label="评审桌状态">
      <article v-for="table in board.tables" :key="table.id" :class="['table-card', table.tone]">
        <header class="table-head">
          <div class="table-name">
            <div class="name-row">
              <h2>{{ table.displayName }}</h2>
              <b class="status-pill"><i aria-hidden="true" />{{ table.statusText }}</b>
            </div>
            <span>{{ table.entryCount }} 款酒</span>
          </div>

          <div class="beer-meter" aria-hidden="true">
            <div class="beer-glass">
              <span :style="{ height: `${table.visualPercent}%` }" />
            </div>
            <strong>{{ table.visualPercent }}%</strong>
          </div>
        </header>

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

    <footer class="round-footer" aria-label="轮次路径">
      <span class="footer-label">轮次路径</span>
      <ol>
        <li v-for="step in board.roundSteps" :key="step.label" :class="{ current: step.current, muted: step.muted }">
          <strong>{{ step.label }}</strong>
          <span>{{ step.status }}</span>
        </li>
      </ol>
      <small>CRAFT BEER AWARDS · LIVE SCOREBOARD</small>
    </footer>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCompetitionProgress, fetchCompetitions } from '@/api/admin'

const REFRESH_SECONDS = 10

const route = useRoute()
const router = useRouter()
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

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/admin/competitions')
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
  const displayNotices = (notices.length ? notices : [buildDefaultNotice(currentRound, tables)]).slice(0, 2)
  return {
    title: data.name || '现场比赛',
    roundName: currentRound?.name || '未创建轮次',
    roundTypeText: currentRound?.type === 'RANKING' ? '排序轮' : '评分制',
    statusText: resolveStatusText(data.status, currentRound),
    metrics: buildMetrics(currentRound, tables, data.progressSummary || {}),
    tables,
    notices: displayNotices,
    roundSteps: buildRoundSteps(rounds, currentRound),
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
      { label: '评分提交', value: '-', unit: '', tone: 'gold' },
      { label: '完成桌数', value: '-', unit: '桌', tone: 'success' },
    ],
    tables: [],
    notices: [{ title: '等待轮次', text: '当前比赛还没有发布可投屏的评审轮次。', tone: 'warning' }],
    roundSteps: buildRoundSteps([], null),
  }
}

function buildMetrics(round, tables, progress = {}) {
  const entryCount = countRoundEntries(round)
  const doneTables = tables.filter((table) => table.done).length
  const averageReviewTime = formatMinutes(progress.averageReviewMinutes)
  if (round?.type === 'RANKING') {
    const selected = tables.reduce((sum, table) => sum + table.selectedCount, 0)
    const target = tables.reduce((sum, table) => sum + table.targetCount, 0)
    return [
      { label: '候选酒款', value: entryCount, unit: '款', tone: 'neutral' },
      { label: '评审桌', value: tables.length, unit: '桌', tone: 'neutral' },
      { label: '排序提交', value: `${selected} / ${target}`, unit: '', tone: doneTables === tables.length && tables.length ? 'success' : 'gold' },
      { label: '完成桌数', value: `${doneTables} / ${tables.length}`, unit: '桌', tone: doneTables === tables.length && tables.length ? 'success' : 'warning' },
      { label: '平均耗时', value: averageReviewTime, unit: '', tone: 'neutral' },
    ]
  }
  const submitted = tables.reduce((sum, table) => sum + table.submittedCount, 0)
  const total = tables.reduce((sum, table) => sum + table.submittedTotal, 0)
  const commentWarnings = Number(progress.commentWarnings || 0)
  return [
    { label: '本轮酒款', value: entryCount, unit: '款', tone: 'neutral' },
    { label: '评审桌', value: tables.length, unit: '桌', tone: 'neutral' },
    { label: '评分提交', value: `${submitted} / ${total}`, unit: '', tone: submitted >= total && total > 0 ? 'success' : 'gold' },
    { label: '完成桌数', value: `${doneTables} / ${tables.length}`, unit: '桌', tone: doneTables === tables.length && tables.length ? 'success' : 'warning' },
    { label: '平均耗时', value: averageReviewTime, unit: '', tone: 'neutral' },
    { label: '备注不足', value: commentWarnings, unit: '条', tone: commentWarnings > 0 ? 'warning' : 'success' },
  ]
}

function buildTableTile(round, table) {
  const entryCount = table.entryUuids?.length || 0
  const targetCount = Number(table.targetCount || 0)
  const displayName = table.name || '未命名桌'
  if (round?.type === 'RANKING') {
    const selectedCount = (table.rankings || []).filter((slot) => slot.uuid).length
    const isChampion = table.targetMode === 'CHAMPION'
    const isMedals = table.targetMode === 'MEDALS'
    const issueText = getTableIssue(table)
    const submitted = ['SUBMITTED', 'LOCKED'].includes(table.status)
    const done = !issueText && (isMedals ? submitted : selectedCount >= targetCount && targetCount > 0)
    const statusText = issueText ? '需关注' : done ? (isChampion ? '已提交' : '已完成') : selectedCount ? '排序中' : (isChampion ? '待提交' : '待排序')
    const targetLabel = isChampion ? '总冠军' : isMedals ? '奖项槽位' : '排序目标'
    const targetValue = isChampion ? (done ? '已选择' : '待选择') : `${targetCount} 款`
    return {
      id: table.id || displayName,
      displayName,
      entryCount,
      targetCount,
      selectedCount,
      submittedCount: selectedCount,
      submittedTotal: Math.max(targetCount, 0),
      visualPercent: targetCount ? normalizePercent(selectedCount * 100 / targetCount) : 0,
      statusText,
      done,
      issueText,
      tone: issueText ? 'danger' : done ? 'success' : selectedCount ? 'active' : 'warning',
      stats: [
        { label: '排序提交', value: `${selectedCount} / ${targetCount}`, accent: done || selectedCount > 0 },
        { label: '主办方确认', value: round.status === 'LOCKED' ? '已锁定' : done ? '待确认' : '未开始' },
        { label: targetLabel, value: targetValue, accent: true },
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
    visualPercent: judgeProgress,
    statusText,
    done,
    issueText,
    tone,
    stats: [
      { label: '评分表提交', value: `${submittedCount} / ${submittedTotal}`, accent: judgeProgress > 0 || done },
      { label: '桌长确认', value: resolveCaptainText(captainProgress, captainConfirmedCount, entryCount), accent: captainProgress > 0 || judgeProgress >= 100 },
      { label: '晋级名额', value: `${targetCount} 款`, accent: true },
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
  if (!Number(table.targetCount || 0)) return table.targetMode === 'CHAMPION' ? '总冠军名额未设置' : '排序目标未设置'
  return ''
}

function collectNotices(round, tables) {
  if (!round) return [{ title: '等待轮次', text: '当前比赛还没有发布可投屏的评审轮次。', tone: 'warning' }]
  const issue = tables.find((table) => table.issueText)
  if (issue) return [{ title: '需现场处理', text: `${issue.displayName}${issue.issueText}，请工作人员处理。`, tone: 'danger' }]

  const notices = []
  const waitingCaptain = tables.find((table) => table.statusText === '待确认')
  const scoring = tables.find((table) => table.statusText === '评分中')
  const captainWorking = tables.find((table) => table.statusText === '确认中')
  if (scoring) {
    const pending = Math.max(scoring.submittedTotal - scoring.submittedCount, 0)
    notices.push({ title: '评审进行中', text: `${scoring.displayName}还有 ${pending} 份评分待提交。`, tone: 'gold' })
  }
  if (waitingCaptain) {
    notices.push({ title: '等待桌长', text: `${waitingCaptain.displayName}评分已完成，等待桌长确认。`, tone: 'warning' })
  }
  if (captainWorking) {
    notices.push({ title: '桌长确认中', text: `${captainWorking.displayName}桌长正在确认本桌结果。`, tone: 'warning' })
  }
  if (tables.length && tables.every((table) => table.done)) {
    notices.push({ title: '本轮已完成', text: '所有评审桌已完成，请等待主办方确认本轮结果。', tone: 'success' })
  }
  return notices
}

function buildDefaultNotice(round, tables) {
  if (!round) return { title: '等待轮次', text: '当前比赛还没有发布可投屏的评审轮次。', tone: 'warning' }
  if (!tables.length) return { title: '等待分桌', text: '当前轮次还没有评审桌。', tone: 'warning' }
  return { title: '现场正常', text: '评审桌状态正常，暂无需要现场处理的事项。', tone: 'success' }
}

function resolveStatusText(status, round) {
  if (round?.status === 'PUBLISHED') return '评审进行中'
  if (round?.status === 'IN_PROGRESS') return '处理中'
  if (round?.status === 'SUBMITTED') return '等待确认'
  if (round?.status === 'LOCKED') return '本轮已锁定'
  if (status === 'JUDGING') return '评审进行中'
  return '现场准备中'
}

function countRoundEntries(round) {
  return new Set((round?.tables || []).flatMap((table) => table.entryUuids || [])).size
}

function formatMinutes(value) {
  const minutes = Number(value || 0)
  if (minutes <= 0) return '-'
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours}小时${rest}分钟` : `${hours}小时`
}

function buildRoundSteps(rounds, currentRound) {
  const currentIndex = Math.max(rounds.findIndex((round) => round.id === currentRound?.id), 0)
  const nextRound = rounds[currentIndex + 1]
  const terminal = currentRound?.tables?.some((table) => table.targetMode === 'CHAMPION')
  return [
    { label: currentRound?.name || '第一轮', status: resolveShortRoundStatus(currentRound), current: true },
    ...(terminal ? [] : [{ label: nextRound?.name || '下一轮', status: nextRound ? resolveShortRoundStatus(nextRound) : '待创建', muted: !nextRound }]),
    { label: '结果确认', status: currentRound?.status === 'LOCKED' && terminal ? '待发布' : '待确认', muted: true },
  ]
}

function resolveShortRoundStatus(round) {
  if (!round) return '待创建'
  if (round.status === 'LOCKED') return '已锁定'
  if (round.status === 'SUBMITTED') return '待确认'
  if (['PUBLISHED', 'IN_PROGRESS'].includes(round.status)) return '评审中'
  return '待发布'
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
  --bg: #100b07;
  --panel: #24170f;
  --panel-soft: #2d1d12;
  --panel-deep: #1a0f0a;
  --line: rgba(224, 162, 57, 0.2);
  --line-strong: rgba(224, 162, 57, 0.42);
  --text: #fff9eb;
  --muted: #b4a58a;
  --dim: #7c6e58;
  --gold: #e0a239;
  --gold-soft: #ffd069;
  --green: #bed894;
  --orange: #ff964b;
  --danger: #ff725e;
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 13px;
  padding: 24px 34px 17px;
  overflow: hidden;
  color: var(--text);
  background:
    radial-gradient(circle at 4% 0%, rgba(224, 162, 57, 0.16), transparent 26%),
    linear-gradient(180deg, #211508 0%, #120c07 48%, #0b0705 100%);
  font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif;
  letter-spacing: 0;
}

.live-board::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.22;
  background-image:
    linear-gradient(rgba(255, 214, 132, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 214, 132, 0.05) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: linear-gradient(180deg, #000, transparent 82%);
}

.live-board::after {
  content: "";
  position: absolute;
  inset: auto 34px 8px;
  height: 1px;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(224, 162, 57, 0.72), rgba(190, 216, 148, 0.42), transparent);
}

.live-board > * {
  position: relative;
  z-index: 1;
}

.board-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 34%);
  gap: 34px;
  align-items: start;
  padding-top: 0;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(224, 162, 57, 0.18);
}

.title-zone {
  min-width: 0;
}

.top-line {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  height: 25px;
  color: var(--dim);
  font-size: 13px;
  font-weight: 900;
}

.top-line i {
  width: 1px;
  height: 14px;
  background: rgba(224, 162, 57, 0.35);
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 25px;
  padding: 0 12px;
  color: #c6b79a;
  border: 1px solid rgba(224, 162, 57, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  font: inherit;
  cursor: pointer;
}

.brand-mark {
  overflow: hidden;
  max-width: 220px;
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 7px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.screen-name {
  color: #b8aa8e;
  white-space: nowrap;
}

.title-zone h1 {
  max-width: 960px;
  margin: 9px 0 4px;
  overflow: hidden;
  color: var(--text);
  font-size: 42px;
  line-height: 1.08;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-shadow: 0 8px 20px rgba(0, 0, 0, 0.55);
}

.stage-line {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.stage-line span,
.stage-line b,
.stage-line em {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 13px;
  border: 1px solid rgba(224, 162, 57, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  font-size: 15px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.stage-line span {
  color: var(--gold-soft);
  border-color: var(--line-strong);
  background: rgba(224, 162, 57, 0.12);
}

.stage-line b {
  gap: 8px;
  color: var(--green);
  background: rgba(190, 216, 148, 0.08);
}

.stage-line b i,
.status-pill i {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: currentColor;
}

.stage-line em {
  color: #ad9d81;
}

.notice-stack {
  display: grid;
  gap: 9px;
  align-content: center;
  padding-top: 14px;
}

.notice-panel {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(224, 162, 57, 0.16);
  border-radius: 8px;
  background: rgba(36, 23, 15, 0.64);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.notice-dot {
  flex: 0 0 auto;
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--gold);
  box-shadow: 0 0 18px rgba(224, 162, 57, 0.46);
}

.notice-panel p {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #d3c2a6;
  font-size: 14px;
  line-height: 1.25;
  font-weight: 850;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-panel strong {
  margin-right: 8px;
  color: var(--gold-soft);
  font-weight: 950;
}

.notice-panel.success .notice-dot {
  background: var(--green);
  box-shadow: 0 0 18px rgba(190, 216, 148, 0.48);
}

.notice-panel.success strong {
  color: var(--green);
}

.notice-panel.warning .notice-dot {
  background: var(--orange);
  box-shadow: 0 0 18px rgba(255, 150, 75, 0.48);
}

.notice-panel.warning strong {
  color: #ffb16e;
}

.notice-panel.danger {
  border-color: rgba(255, 114, 94, 0.36);
  background: rgba(54, 18, 13, 0.66);
}

.notice-panel.danger .notice-dot {
  background: var(--danger);
  box-shadow: 0 0 20px rgba(255, 114, 94, 0.56);
}

.notice-panel.danger strong {
  color: #ff9d91;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.metric-card {
  position: relative;
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr) auto;
  grid-template-rows: auto 1fr;
  gap: 6px 10px;
  min-width: 0;
  min-height: 91px;
  padding: 17px 22px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(36, 23, 15, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.metric-card i {
  grid-row: 1 / 3;
  align-self: center;
  width: 6px;
  height: 44px;
  border-radius: 999px;
  background: rgba(180, 165, 138, 0.48);
}

.metric-card span {
  grid-column: 2 / 4;
  color: #a99b81;
  font-size: 14px;
  line-height: 1;
  font-weight: 900;
}

.metric-card strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-size: 33px;
  line-height: 1;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.metric-card small {
  align-self: end;
  color: #8f8169;
  font-size: 14px;
  font-weight: 900;
}

.metric-card.gold i,
.metric-card.warning i {
  background: var(--gold);
}

.metric-card.gold strong,
.metric-card.warning strong {
  color: var(--gold-soft);
}

.metric-card.success i {
  background: var(--green);
}

.metric-card.success strong {
  color: var(--green);
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: clamp(260px, 32vh, 340px);
  align-content: start;
  gap: 16px;
  min-height: 0;
}

.table-card {
  position: relative;
  display: grid;
  grid-template-rows: minmax(136px, 1fr) auto auto;
  gap: 14px;
  min-width: 0;
  min-height: 0;
  padding: 20px 20px 12px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(36, 23, 15, 0.84);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03), 0 18px 40px rgba(0, 0, 0, 0.22);
}

.table-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.025), transparent 42%);
}

.table-card.danger {
  border-color: rgba(255, 114, 94, 0.52);
  background: rgba(52, 19, 14, 0.86);
}

.table-head {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  min-height: 0;
}

.table-name {
  align-self: start;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.table-name h2 {
  margin: 0;
  overflow: hidden;
  color: var(--text);
  font-size: 54px;
  line-height: 0.98;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-shadow: 0 8px 18px rgba(0, 0, 0, 0.42);
}

.table-name span {
  display: block;
  margin-top: 8px;
  color: #a9997e;
  font-size: 15px;
  line-height: 1;
  font-weight: 900;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  flex: 0 0 auto;
  max-width: 154px;
  height: 28px;
  padding: 0 12px;
  overflow: hidden;
  color: var(--gold-soft);
  border-radius: 999px;
  background: rgba(224, 162, 57, 0.18);
  font-size: 13px;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.table-card.success .status-pill {
  color: var(--green);
  background: rgba(190, 216, 148, 0.13);
}

.table-card.warning .status-pill {
  color: #ffa667;
  background: rgba(255, 150, 75, 0.13);
}

.table-card.danger .status-pill {
  color: #ff9d91;
  background: rgba(255, 114, 94, 0.16);
}

.beer-meter {
  display: grid;
  justify-items: center;
  gap: 6px;
  flex: 0 0 78px;
  padding-top: 1px;
}

.beer-glass {
  position: relative;
  width: 44px;
  height: 74px;
  border: 4px solid rgba(255, 248, 235, 0.44);
  border-top-width: 5px;
  border-radius: 6px 6px 10px 10px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 0 2px rgba(55, 36, 22, 0.45), 0 8px 18px rgba(0, 0, 0, 0.25);
}

.beer-glass::before {
  content: "";
  position: absolute;
  left: 7px;
  right: 7px;
  top: 6px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  z-index: 2;
}

.beer-glass::after {
  content: "";
  position: absolute;
  top: 18px;
  right: -18px;
  width: 18px;
  height: 29px;
  border: 4px solid rgba(255, 248, 235, 0.34);
  border-left: 0;
  border-radius: 0 16px 16px 0;
}

.beer-glass span {
  position: absolute;
  left: 4px;
  right: 4px;
  bottom: 4px;
  max-height: calc(100% - 14px);
  min-height: 5px;
  border-radius: 2px 2px 6px 6px;
  background:
    radial-gradient(circle at 25% 70%, rgba(255, 255, 255, 0.55) 0 1px, transparent 2px),
    radial-gradient(circle at 35% 52%, rgba(255, 255, 255, 0.5) 0 1px, transparent 2px),
    linear-gradient(180deg, #ffd46f 0%, #d99a2b 100%);
  transition: height 0.28s ease;
}

.beer-meter strong {
  color: var(--text);
  font-size: 13px;
  line-height: 1;
  font-weight: 950;
}

.table-facts {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.table-facts span {
  display: grid;
  gap: 8px;
  min-width: 0;
  min-height: 74px;
  padding: 12px 13px 11px;
  border: 1px solid rgba(224, 162, 57, 0.08);
  border-radius: 10px;
  background: rgba(26, 15, 10, 0.72);
}

.table-facts small {
  overflow: hidden;
  color: #a49378;
  font-size: 12px;
  line-height: 1;
  font-weight: 900;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.table-facts strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-size: 25px;
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
  color: #ffa667;
}

.table-card.danger .table-facts .accent strong {
  color: #ff9d91;
}

.issue-text {
  position: relative;
  margin: -4px 0 0;
  color: #ffb5aa;
  font-size: 14px;
  font-weight: 900;
}

.empty-card {
  display: grid;
  place-content: center;
  gap: 12px;
  min-height: 220px;
  color: var(--muted);
  text-align: center;
  border: 1px dashed var(--line-strong);
  border-radius: 14px;
  background: rgba(36, 23, 15, 0.6);
}

.empty-card strong {
  color: var(--text);
  font-size: 36px;
}

.empty-card span {
  font-size: 18px;
  font-weight: 850;
}

.round-footer {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 38px;
  color: #8f7c5b;
}

.footer-label {
  color: #a99162;
  font-size: 14px;
  font-weight: 900;
}

.footer-label::before {
  content: "♦";
  margin-right: 10px;
  color: var(--gold);
  font-size: 12px;
}

.round-footer ol {
  display: flex;
  align-items: center;
  gap: 30px;
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.round-footer li {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  color: #9b8d73;
  border: 1px solid rgba(224, 162, 57, 0.08);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.02);
  font-size: 14px;
  font-weight: 900;
}

.round-footer li + li::before {
  content: "→";
  position: absolute;
  left: -22px;
  color: rgba(224, 162, 57, 0.32);
}

.round-footer li.current {
  color: var(--gold-soft);
  border-color: var(--line-strong);
  background: rgba(224, 162, 57, 0.12);
}

.round-footer li.muted {
  opacity: 0.48;
}

.round-footer li span {
  color: #9f9278;
  font-weight: 850;
}

.round-footer small {
  overflow: hidden;
  color: rgba(169, 145, 98, 0.72);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 5px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.tables-1 .table-grid {
  grid-template-columns: minmax(0, 1fr);
  grid-auto-rows: clamp(360px, 48vh, 520px);
}

.tables-1 .table-card {
  padding: 30px;
}

.tables-1 .table-name h2 {
  font-size: 92px;
}

.tables-1 .table-name span {
  font-size: 24px;
}

.tables-1 .status-pill {
  height: 38px;
  padding: 0 18px;
  font-size: 18px;
}

.tables-1 .beer-meter {
  flex-basis: 132px;
}

.tables-1 .beer-glass {
  width: 68px;
  height: 112px;
}

.tables-1 .beer-meter strong {
  font-size: 18px;
}

.tables-1 .table-facts strong {
  font-size: 42px;
}

.tables-1 .table-facts small {
  font-size: 16px;
}

.tables-2 .table-grid {
  grid-auto-rows: clamp(300px, 42vh, 420px);
}

.tables-2 .table-card {
  min-height: 0;
}

.tables-5 .table-grid,
.tables-6 .table-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: clamp(210px, 27vh, 260px);
}

.tables-7 .table-grid,
.tables-8 .table-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-auto-rows: clamp(190px, 24vh, 230px);
}

.tables-5,
.tables-6,
.tables-7,
.tables-8 {
  gap: 10px;
  padding: 18px 26px 14px;
}

.tables-5 .board-header,
.tables-6 .board-header,
.tables-7 .board-header,
.tables-8 .board-header {
  padding-bottom: 10px;
}

.tables-5 .title-zone h1,
.tables-6 .title-zone h1,
.tables-7 .title-zone h1,
.tables-8 .title-zone h1 {
  font-size: 35px;
}

.tables-5 .metric-card,
.tables-6 .metric-card,
.tables-7 .metric-card,
.tables-8 .metric-card {
  min-height: 76px;
  padding: 13px 16px;
}

.tables-5 .metric-card strong,
.tables-6 .metric-card strong,
.tables-7 .metric-card strong,
.tables-8 .metric-card strong {
  font-size: 27px;
}

.tables-5 .table-card,
.tables-6 .table-card,
.tables-7 .table-card,
.tables-8 .table-card {
  gap: 10px;
  padding: 14px;
}

.tables-5 .table-name h2,
.tables-6 .table-name h2 {
  font-size: 38px;
}

.tables-7 .table-name h2,
.tables-8 .table-name h2 {
  font-size: 32px;
}

.tables-5 .name-row,
.tables-6 .name-row,
.tables-7 .name-row,
.tables-8 .name-row {
  display: grid;
  gap: 7px;
}

.tables-5 .status-pill,
.tables-6 .status-pill,
.tables-7 .status-pill,
.tables-8 .status-pill {
  justify-self: start;
  max-width: 120px;
}

.tables-5 .beer-meter,
.tables-6 .beer-meter,
.tables-7 .beer-meter,
.tables-8 .beer-meter {
  flex-basis: 56px;
}

.tables-5 .beer-glass,
.tables-6 .beer-glass,
.tables-7 .beer-glass,
.tables-8 .beer-glass {
  width: 34px;
  height: 58px;
}

.tables-5 .table-facts,
.tables-6 .table-facts,
.tables-7 .table-facts,
.tables-8 .table-facts {
  gap: 7px;
}

.tables-5 .table-facts span,
.tables-6 .table-facts span,
.tables-7 .table-facts span,
.tables-8 .table-facts span {
  min-height: 56px;
  padding: 9px;
}

.tables-5 .table-facts strong,
.tables-6 .table-facts strong {
  font-size: 19px;
}

.tables-7 .table-facts,
.tables-8 .table-facts {
  grid-template-columns: 1fr;
}

.tables-7 .table-facts span,
.tables-8 .table-facts span {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: baseline;
}

.tables-7 .table-facts strong,
.tables-8 .table-facts strong {
  font-size: 16px;
}

@media (max-width: 1500px) {
  .live-board {
    gap: 10px;
    padding: 18px 26px 14px;
  }

  .board-header {
    grid-template-columns: minmax(0, 1fr) minmax(340px, 34%);
    gap: 24px;
    padding-bottom: 10px;
  }

  .title-zone h1 {
    font-size: 40px;
  }

  .metric-card {
    min-height: 74px;
    padding: 12px 16px;
  }

  .metric-card strong {
    font-size: 29px;
  }

  .table-card {
    grid-template-rows: minmax(118px, 1fr) auto auto;
    padding: 16px 16px 11px;
  }

  .table-name h2 {
    font-size: 50px;
  }

  .beer-glass {
    width: 38px;
    height: 64px;
  }

  .table-facts span {
    min-height: 62px;
    padding: 10px;
  }

  .table-facts strong {
    font-size: 23px;
  }
}
</style>
