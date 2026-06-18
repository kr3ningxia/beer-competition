<template>
  <main :class="['live-board', densityClass]">
    <header class="board-header">
      <section class="title-zone">
        <div class="top-line">
          <button type="button" class="back-button" @click="goBack">返回</button>
          <span class="brand-mark">CHINESE LAGER AWARDS</span>
          <span class="screen-name">评审现场实时大屏</span>
        </div>

        <h1>{{ board.title }}</h1>
        <p class="subtitle">{{ board.subtitle }}</p>

        <div class="stage-line">
          <span>{{ board.roundName }}</span>
          <em>{{ board.roundTypeText }}</em>
          <b><i aria-hidden="true" />{{ board.statusText }}</b>
        </div>
      </section>

      <section class="partner-wall" aria-label="赛事合作方">
        <article v-for="group in board.partners" :key="group.label" :class="{ featured: group.featured }">
          <span>{{ group.label }}</span>
          <strong>{{ group.names.join(' / ') }}</strong>
        </article>
      </section>
    </header>

    <section class="scoreboard-stage" aria-label="评审进度总览">
      <section class="hero-panel">
        <div class="progress-copy">
          <div class="progress-kicker">
            <span class="eyebrow">{{ board.progress.eyebrow }}</span>
            <em>{{ board.statusText }}</em>
          </div>
          <strong>{{ board.progress.done }} / {{ board.progress.total }}</strong>
          <p>{{ board.progress.label }}</p>
        </div>

        <div class="beer-hero" aria-hidden="true">
          <div class="glass-visual">
            <div class="glass-halo" />
            <div class="beer-glass-large">
              <span class="glass-shine" />
              <span class="beer-liquid" :style="{ height: `${board.progress.liquidPercent}%` }">
                <i />
                <i />
                <i />
              </span>
              <span class="foam-line" :style="{ bottom: `${board.progress.foamBottom}%` }" />
            </div>
          </div>
          <strong class="glass-percent">{{ board.progress.percent }}%</strong>
        </div>
      </section>

      <aside class="metric-panel">
        <article v-for="metric in board.metrics" :key="metric.label" :class="['metric-card', metric.tone]">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>{{ metric.unit }}</small>
        </article>

        <div class="notice-panel" :class="board.notice.tone">
          <span class="notice-dot" />
          <p><strong>{{ board.notice.title }}</strong>{{ board.notice.text }}</p>
        </div>
      </aside>
    </section>

    <section class="desk-board" aria-label="评审桌汇总">
      <header class="desk-board-head">
        <span>{{ board.deskTitle }}</span>
        <strong>{{ board.deskHint }}</strong>
      </header>

      <div class="desk-table" role="table">
        <div class="desk-row desk-row-head" role="row">
          <span role="columnheader">评审桌</span>
          <span role="columnheader">{{ board.tableColumns.done }}</span>
          <span role="columnheader">{{ board.tableColumns.pending }}</span>
          <span role="columnheader">完成率</span>
          <span role="columnheader">平均用时</span>
          <span role="columnheader">平均评语</span>
          <span role="columnheader">状态</span>
        </div>

        <div v-for="table in board.tables" :key="table.id" :class="['desk-row', table.tone]" role="row">
          <strong role="cell">{{ table.displayName }}</strong>
          <span role="cell">{{ table.doneCount }}</span>
          <span role="cell">{{ table.pendingCount }}</span>
          <span role="cell">
            <b>{{ table.visualPercent }}%</b>
            <i class="mini-track" aria-hidden="true"><i :style="{ width: `${table.visualPercent}%` }" /></i>
          </span>
          <span role="cell">{{ table.averageTime }}</span>
          <span role="cell">{{ table.averageComment }}</span>
          <span role="cell"><em>{{ table.statusText }}</em></span>
        </div>

        <div v-if="!board.tables.length" class="desk-empty">
          <strong>等待轮次发布</strong>
          <span>轮次发布后，评审桌进度会显示在这里</span>
        </div>
      </div>
    </section>

    <footer class="round-footer" aria-label="现场进程">
      <span class="footer-label">现场进程</span>
      <ol>
        <li v-for="step in board.roundSteps" :key="step.label" :class="{ current: step.current, muted: step.muted }">
          <strong>{{ step.label }}</strong>
          <span>{{ step.status }}</span>
        </li>
      </ol>
      <small>CHINESE LAGER AWARDS · LIVE SCOREBOARD</small>
    </footer>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCompetitionProgress, fetchCompetitions } from '@/api/admin'

const REFRESH_SECONDS = 10
const STAT_PENDING_TEXT = '统计中'
const EVENT_TITLE = '首届中国拉格大赛'
const EVENT_SUBTITLE = 'The 1st Chinese Lager Awards'
const PARTNERS = [
  { label: '主办方', names: ['啤酒事务局'], featured: true },
  { label: '首席赞助', names: ['臻麦'], featured: true },
  { label: '行业赞助', names: ['迈拓设备', 'Fermentis', 'Kegland'] },
  { label: '协办', names: ['力波1987精酿'] },
]

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
  const competitions = await fetchCompetitions({ includeArchived: false })
  if (selectedCompetitionId.value && competitions.some((item) => String(item.id) === String(selectedCompetitionId.value))) return
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
  const progressSummary = data.progressSummary || {}
  const isRanking = currentRound?.type === 'RANKING'
  const tables = (currentRound?.tables || []).slice(0, 8).map((table) => buildTableTile(currentRound, table, progressSummary))
  const progress = buildProgress(currentRound, tables, progressSummary)
  const totalEntryCount = countCompetitionEntries(data, progress.total)

  return {
    title: EVENT_TITLE,
    subtitle: EVENT_SUBTITLE,
    roundName: currentRound?.name || '未创建轮次',
    roundTypeText: isRanking ? '排序轮' : '评分制',
    statusText: resolveStatusText(data.status, currentRound),
    partners: PARTNERS,
    progress,
    metrics: buildMetrics(currentRound, tables, progressSummary, totalEntryCount, progress),
    tables,
    notice: collectNotice(currentRound, tables),
    deskTitle: isRanking ? '排序轮桌次进度' : '首轮桌次评审进度',
    deskHint: isRanking ? '按桌次查看排序提交状态' : '按桌次查看已评审、待评审与反馈效率',
    tableColumns: isRanking
      ? { done: '已排序', pending: '待排序' }
      : { done: '已评审', pending: '待评审' },
    roundSteps: buildRoundSteps(rounds, currentRound),
  }
}

function buildEmptyBoard() {
  const progress = buildProgress(null, [], {})
  return {
    title: EVENT_TITLE,
    subtitle: EVENT_SUBTITLE,
    roundName: '未创建轮次',
    roundTypeText: '等待发布',
    statusText: '现场准备中',
    partners: PARTNERS,
    progress,
    metrics: buildMetrics(null, [], {}, 0, progress),
    tables: [],
    notice: { title: '等待轮次', text: '当前比赛还没有发布可投屏的评审轮次', tone: 'warning' },
    deskTitle: '首轮桌次评审进度',
    deskHint: '轮次发布后自动更新',
    tableColumns: { done: '已评审', pending: '待评审' },
    roundSteps: buildRoundSteps([], null),
  }
}

function buildProgress(round, tables, progress = {}) {
  if (round?.type === 'RANKING') {
    const done = normalizeCount(progress.finalized, tables.filter((table) => table.done).length)
    const total = normalizeCount(progress.total, tables.length)
    const percent = total ? normalizePercent(done * 100 / total) : 0
    return {
      eyebrow: '排序进度',
      done,
      total,
      percent,
      liquidPercent: 100 - percent,
      foamBottom: normalizeFoamBottom(percent),
      label: total ? `已提交 ${done} 桌 · 剩余 ${Math.max(total - done, 0)} 桌` : '等待排序轮发布',
    }
  }

  const fallbackTotal = tables.reduce((sum, table) => sum + table.submittedTotal, 0)
  const fallbackDone = tables.reduce((sum, table) => sum + table.doneCount, 0)
  const total = normalizeCount(progress.total, fallbackTotal)
  const done = Math.min(normalizeCount(progress.finalized, fallbackDone), total || fallbackDone)
  const percent = total ? normalizePercent(done * 100 / total) : 0
  return {
    eyebrow: '本轮已评',
    done,
    total,
    percent,
    liquidPercent: 100 - percent,
    foamBottom: normalizeFoamBottom(percent),
    label: total ? `已完成 ${done} 款 · 剩余 ${Math.max(total - done, 0)} 款` : '等待首轮酒款发布',
  }
}

function buildMetrics(round, tables, progress = {}, competitionEntryCount = 0, roundProgress = null) {
  const isRanking = round?.type === 'RANKING'
  const progressData = roundProgress || buildProgress(round, tables, progress)
  return [
    { label: '参赛酒款', value: competitionEntryCount || '-', unit: competitionEntryCount ? '款' : '', tone: 'neutral' },
    { label: isRanking ? '本轮桌数' : '本轮酒款', value: progressData.total || '-', unit: isRanking ? '桌' : '款', tone: 'neutral' },
    { label: isRanking ? '本轮已排' : '本轮已评', value: progressData.total ? `${progressData.done}/${progressData.total}` : '-', unit: '', tone: progressData.percent >= 100 ? 'success' : 'gold' },
    { label: '平均用时', value: formatAverageReviewTime(progress), unit: '', tone: 'neutral' },
    { label: '平均评语', value: formatCommentChars(progress.averageCommentChars || progress.siteAverageCommentChars), unit: '', tone: 'neutral' },
  ]
}

function buildTableTile(round, table, progress = {}) {
  const entryCount = table.entryUuids?.length || 0
  const targetCount = Number(table.targetCount || 0)
  const displayName = table.name || '未命名桌'
  const averageTime = resolveTableAverageTime(table)
  const averageComment = resolveTableAverageComment(table)

  if (round?.type === 'RANKING') {
    const selectedCount = (table.rankings || []).filter((slot) => slot.uuid).length
    const isChampion = table.targetMode === 'CHAMPION'
    const isMedals = table.targetMode === 'MEDALS'
    const issueText = getTableIssue(table)
    const submitted = ['SUBMITTED', 'LOCKED'].includes(table.status)
    const done = !issueText && (isMedals ? submitted : selectedCount >= targetCount && targetCount > 0)
    const visualPercent = targetCount ? normalizePercent(selectedCount * 100 / targetCount) : (done ? 100 : 0)
    const pendingCount = Math.max(targetCount - selectedCount, 0)
    const statusText = issueText ? '需处理' : done ? (isChampion ? '已提交' : '已完成') : selectedCount ? '排序中' : '待排序'
    return {
      id: table.id || displayName,
      displayName,
      entryCount,
      targetCount,
      selectedCount,
      submittedCount: selectedCount,
      submittedTotal: Math.max(targetCount, 0),
      doneCount: selectedCount,
      pendingCount,
      visualPercent,
      averageTime,
      averageComment,
      statusText,
      done,
      issueText,
      tone: issueText ? 'danger' : done ? 'success' : selectedCount ? 'active' : 'warning',
    }
  }

  const judgeProgress = normalizePercent(table.judgeProgress)
  const captainProgress = normalizePercent(table.captainProgress)
  const submittedTotal = entryCount
  const submittedCount = estimateCount(submittedTotal, judgeProgress)
  const captainConfirmedCount = estimateCount(entryCount, captainProgress)
  const issueText = getTableIssue(table)
  const done = !issueText && captainProgress >= 100
  let statusText = '等待评分'
  let tone = 'idle'
  if (issueText) {
    statusText = '需处理'
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
    selectedCount: Number(table.advancedCount || 0),
    submittedCount,
    submittedTotal,
    doneCount: submittedCount,
    pendingCount: Math.max(submittedTotal - submittedCount, 0),
    captainConfirmedCount,
    visualPercent: judgeProgress,
    averageTime,
    averageComment,
    statusText,
    done,
    issueText,
    tone,
  }
}

function getTableIssue(table) {
  if (!table.captainPublicId && !table.captainName && !table.captain?.name) return '桌长未指定'
  if (!(table.entryUuids || []).length) return '尚未分配酒款'
  if (!Number(table.targetCount || 0)) return table.targetMode === 'CHAMPION' ? '总冠军名额未设置' : '排序目标未设置'
  return ''
}

function collectNotice(round, tables) {
  if (!round) return { title: '等待轮次', text: '当前比赛还没有发布可投屏的评审轮次', tone: 'warning' }
  const issue = tables.find((table) => table.issueText)
  if (issue) return { title: '需现场处理', text: `${issue.displayName}${issue.issueText}，请工作人员处理`, tone: 'danger' }

  const active = tables.find((table) => ['评分中', '排序中', '确认中', '待确认'].includes(table.statusText))
  if (active) return { title: active.statusText, text: `${active.displayName}正在推进，请关注桌次进度`, tone: active.tone === 'active' ? 'gold' : 'warning' }
  if (tables.length && tables.every((table) => table.done)) {
    return { title: '本轮已完成', text: '所有评审桌已完成，请等待组委会确认本轮结果', tone: 'success' }
  }
  if (!tables.length) return { title: '等待分桌', text: '当前轮次还没有评审桌', tone: 'warning' }
  return { title: '现场正常', text: '评审桌状态正常，现场进度会自动刷新', tone: 'success' }
}

function resolveStatusText(status, round) {
  if (round?.status === 'PUBLISHED') return '评审进行中'
  if (round?.status === 'IN_PROGRESS') return '处理中'
  if (round?.status === 'SUBMITTED') return '等待确认'
  if (round?.status === 'LOCKED') return '本轮已锁定'
  if (status === 'JUDGING') return '评审进行中'
  return '现场准备中'
}

function countCompetitionEntries(data, fallback = 0) {
  const entryPoolCount = Array.isArray(data?.entryPool) ? data.entryPool.length : 0
  const entriesSummaryCount = Number(data?.entriesSummary?.registered || data?.entrySummary?.registered || 0)
  return entryPoolCount || entriesSummaryCount || fallback || 0
}

function formatMinutes(value) {
  const minutes = Number(value || 0)
  if (minutes <= 0) return STAT_PENDING_TEXT
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours}小时${rest}分钟` : `${hours}小时`
}

function formatCommentChars(value) {
  const chars = Number(value || 0)
  return chars > 0 ? `${chars}字` : STAT_PENDING_TEXT
}

function formatAverageReviewTime(progress = {}) {
  const secondsText = formatDurationSeconds(progress.averageReviewSeconds)
  return secondsText === STAT_PENDING_TEXT ? formatMinutes(progress.averageReviewMinutes) : secondsText
}

function resolveTableAverageTime(table) {
  const seconds = Number(table.averageDurationSeconds || table.reviewStats?.averageDurationSeconds || 0)
  if (seconds > 0) return formatDurationSeconds(seconds)
  return formatMinutes(table.averageReviewMinutes)
}

function resolveTableAverageComment(table) {
  return formatCommentChars(table.averageCommentChars || table.reviewStats?.averageCommentChars)
}

function formatDurationSeconds(secondsValue) {
  const seconds = Number(secondsValue || 0)
  if (seconds <= 0) return STAT_PENDING_TEXT
  const minutes = Math.floor(seconds / 60)
  const secondsRest = seconds % 60
  if (!minutes) return `${secondsRest}秒`
  return secondsRest ? `${minutes}分${secondsRest}秒` : `${minutes}分钟`
}

function buildRoundSteps(rounds, currentRound) {
  const currentIndex = Math.max(rounds.findIndex((round) => round.id === currentRound?.id), 0)
  const nextRound = rounds[currentIndex + 1]
  const terminal = currentRound?.tables?.some((table) => table.targetMode === 'CHAMPION')
  return [
    { label: currentRound?.name || '首轮', status: resolveShortRoundStatus(currentRound), current: true },
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

function normalizeCount(value, fallback = 0) {
  const number = Number(value)
  if (!Number.isFinite(number) || number < 0) return Number(fallback || 0)
  return Math.round(number)
}

function normalizePercent(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number)) return 0
  return Math.max(0, Math.min(100, Math.round(number)))
}

function normalizeFoamBottom(percent) {
  const liquidPercent = 100 - normalizePercent(percent)
  if (liquidPercent <= 4) return 5
  return Math.min(88, Math.max(11, liquidPercent))
}
</script>

<style scoped>
.live-board {
  --bg: #070807;
  --panel: rgba(18, 19, 17, 0.82);
  --panel-deep: rgba(9, 10, 9, 0.78);
  --line: rgba(225, 178, 91, 0.24);
  --line-strong: rgba(225, 178, 91, 0.52);
  --text: #fff8e8;
  --muted: #b5aa92;
  --dim: #7d7668;
  --gold: #dca64c;
  --gold-soft: #ffd682;
  --lager: #f2b33f;
  --teal: #69c5b9;
  --green: #b8d986;
  --orange: #f29c55;
  --danger: #ef6a55;
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-rows: auto minmax(220px, 27vh) minmax(300px, 1fr) auto;
  gap: 16px;
  padding: 28px 34px 18px;
  overflow: hidden;
  color: var(--text);
  background:
    linear-gradient(115deg, rgba(222, 167, 76, 0.13), transparent 38%),
    linear-gradient(180deg, #11120f 0%, #090a09 52%, #040504 100%);
  font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif;
  letter-spacing: 0;
}

.live-board::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.18;
  background-image:
    linear-gradient(rgba(255, 214, 132, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(105, 197, 185, 0.045) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(180deg, #000 0%, transparent 88%);
}

.live-board::after {
  content: "";
  position: absolute;
  inset: auto 34px 8px;
  height: 1px;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(220, 166, 76, 0.82), rgba(105, 197, 185, 0.62), transparent);
}

.live-board > * {
  position: relative;
  z-index: 1;
}

.board-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(480px, 38%);
  gap: 36px;
  align-items: end;
  min-height: 132px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(225, 178, 91, 0.18);
}

.title-zone {
  min-width: 0;
}

.top-line {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  color: var(--dim);
  font-size: 13px;
  font-weight: 900;
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  padding: 0 13px;
  color: #d7c8aa;
  border: 1px solid rgba(225, 178, 91, 0.22);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font: inherit;
  cursor: pointer;
}

.back-button::before {
  content: "←";
  margin-right: 6px;
}

.brand-mark {
  overflow: hidden;
  max-width: 300px;
  color: var(--gold);
  font-size: 12px;
  letter-spacing: 6px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.screen-name {
  color: #d8ccb4;
  white-space: nowrap;
}

.screen-name::before {
  content: "";
  display: inline-block;
  width: 1px;
  height: 14px;
  margin-right: 14px;
  vertical-align: -2px;
  background: rgba(225, 178, 91, 0.36);
}

.title-zone h1 {
  max-width: 920px;
  margin: 10px 0 0;
  overflow: hidden;
  color: var(--text);
  font-size: clamp(38px, 3.25vw, 58px);
  line-height: 1.02;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-shadow: 0 14px 28px rgba(0, 0, 0, 0.56);
}

.subtitle {
  margin: 5px 0 10px;
  overflow: hidden;
  color: #e5c68a;
  font-size: clamp(17px, 1.18vw, 23px);
  line-height: 1.1;
  font-weight: 800;
  white-space: nowrap;
  text-overflow: ellipsis;
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
  border: 1px solid rgba(225, 178, 91, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font-size: 15px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.stage-line span {
  color: var(--gold-soft);
  border-color: var(--line-strong);
  background: rgba(220, 166, 76, 0.13);
}

.stage-line b {
  gap: 8px;
  color: var(--green);
  background: rgba(184, 217, 134, 0.1);
}

.stage-line b i {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: currentColor;
  box-shadow: 0 0 16px currentColor;
}

.stage-line em {
  color: #d5c7ad;
}

.partner-wall {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.partner-wall article {
  display: grid;
  gap: 5px;
  min-width: 0;
  min-height: 50px;
  padding: 9px 13px;
  border: 1px solid rgba(225, 178, 91, 0.18);
  border-radius: 8px;
  background: rgba(18, 19, 17, 0.62);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.partner-wall article.featured {
  border-color: rgba(225, 178, 91, 0.36);
  background: rgba(220, 166, 76, 0.09);
}

.partner-wall span {
  color: #8f8878;
  font-size: 12px;
  line-height: 1;
  font-weight: 900;
}

.partner-wall strong {
  overflow: hidden;
  color: var(--text);
  font-size: clamp(15px, 1.12vw, 20px);
  line-height: 1.1;
  font-weight: 950;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.scoreboard-stage {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(430px, 28%);
  gap: 16px;
  min-height: 0;
}

.hero-panel {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 28%);
  align-items: center;
  gap: 26px;
  min-width: 0;
  min-height: 0;
  padding: 18px 34px 20px;
  overflow: hidden;
  border: 1px solid rgba(225, 178, 91, 0.24);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(220, 166, 76, 0.16), transparent 42%),
    linear-gradient(180deg, rgba(20, 21, 18, 0.92), rgba(7, 8, 7, 0.78));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04), 0 26px 54px rgba(0, 0, 0, 0.28);
}

.hero-panel::before {
  content: "";
  position: absolute;
  inset: 17px;
  pointer-events: none;
  border: 1px solid rgba(225, 178, 91, 0.09);
  border-radius: 6px;
}

.progress-copy {
  align-self: center;
  min-width: 0;
}

.progress-kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 31px;
  padding: 0 12px;
  color: #0c0d0b;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gold-soft), var(--teal));
  font-size: 14px;
  line-height: 1;
  font-weight: 950;
}

.progress-kicker em {
  display: inline-flex;
  align-items: center;
  min-height: 31px;
  padding: 0 12px;
  overflow: hidden;
  color: var(--green);
  border: 1px solid rgba(184, 217, 134, 0.22);
  border-radius: 999px;
  background: rgba(184, 217, 134, 0.08);
  font-size: 13px;
  font-style: normal;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.progress-kicker em::before {
  content: "";
  width: 8px;
  height: 8px;
  margin-right: 7px;
  border-radius: 999px;
  background: currentColor;
  box-shadow: 0 0 14px currentColor;
}

.progress-copy strong {
  display: block;
  margin-top: 14px;
  color: var(--text);
  font-size: clamp(62px, 7vw, 124px);
  line-height: 0.92;
  font-weight: 950;
  text-shadow: 0 18px 44px rgba(0, 0, 0, 0.58);
}

.progress-copy p {
  margin: 12px 0 0;
  color: #d7c8aa;
  font-size: clamp(19px, 1.75vw, 30px);
  line-height: 1.12;
  font-weight: 900;
}

.beer-hero {
  position: relative;
  display: grid;
  grid-template-columns: auto auto;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  grid-column: 2;
  gap: 18px;
  box-sizing: border-box;
  min-width: 0;
  height: auto;
  min-height: 0;
  padding: 0;
}

.glass-visual {
  position: relative;
  display: grid;
  place-items: center;
  min-width: 0;
  width: clamp(150px, 11vw, 210px);
  aspect-ratio: 1;
}

.glass-halo {
  position: absolute;
  inset: 0;
  width: 100%;
  aspect-ratio: 1;
  border: 1px solid rgba(225, 178, 91, 0.22);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(220, 166, 76, 0.22), transparent 62%);
  filter: blur(1px);
}

.glass-percent {
  color: var(--gold-soft);
  font-size: clamp(24px, 2.4vw, 38px);
  line-height: 1;
  font-weight: 950;
  text-align: left;
  text-shadow: 0 8px 22px rgba(0, 0, 0, 0.5);
}

.beer-glass-large {
  position: relative;
  width: clamp(108px, 7.6vw, 142px);
  height: clamp(136px, 17vh, 172px);
  overflow: hidden;
  border: 8px solid rgba(255, 248, 232, 0.7);
  border-top-width: 10px;
  border-radius: 18px 18px 28px 28px;
  background: rgba(255, 255, 255, 0.06);
  box-shadow:
    inset 0 0 0 3px rgba(34, 28, 18, 0.62),
    inset 20px 0 34px rgba(255, 255, 255, 0.05),
    0 24px 52px rgba(0, 0, 0, 0.42);
}

.beer-glass-large::after {
  content: "";
  position: absolute;
  top: 42px;
  right: -40px;
  width: 38px;
  height: 76px;
  border: 7px solid rgba(255, 248, 232, 0.42);
  border-left: 0;
  border-radius: 0 40px 40px 0;
}

.glass-shine {
  position: absolute;
  z-index: 3;
  left: 20px;
  top: 20px;
  width: 58%;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
}

.beer-liquid {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 8px;
  max-height: calc(100% - 18px);
  min-height: 0;
  overflow: hidden;
  border-radius: 7px 7px 18px 18px;
  background:
    linear-gradient(180deg, rgba(255, 224, 127, 0.96) 0%, rgba(242, 179, 63, 0.96) 42%, rgba(168, 105, 29, 0.98) 100%);
  box-shadow: inset 0 14px 18px rgba(255, 255, 255, 0.12);
  transition: height 0.45s ease;
}

.beer-liquid i {
  position: absolute;
  bottom: 9%;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  animation: bubble-rise 4.8s linear infinite;
}

.beer-liquid i:nth-child(1) {
  left: 26%;
}

.beer-liquid i:nth-child(2) {
  left: 48%;
  width: 4px;
  height: 4px;
  animation-delay: 1.1s;
}

.beer-liquid i:nth-child(3) {
  left: 68%;
  animation-delay: 2.4s;
}

.foam-line {
  position: absolute;
  z-index: 2;
  left: 13px;
  right: 13px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 250, 229, 0.88);
  box-shadow: 0 0 18px rgba(255, 240, 190, 0.3);
  transition: bottom 0.45s ease;
}

.metric-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
  min-height: 0;
}

.metric-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-rows: auto auto;
  align-content: center;
  gap: 7px 8px;
  min-width: 0;
  min-height: 0;
  padding: 11px 16px;
  overflow: hidden;
  border: 1px solid rgba(225, 178, 91, 0.18);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.metric-card span {
  grid-column: 1 / 3;
  overflow: hidden;
  color: #aaa18e;
  font-size: 14px;
  line-height: 1;
  font-weight: 900;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.metric-card strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-size: clamp(22px, 1.9vw, 32px);
  line-height: 1.05;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.metric-card small {
  align-self: center;
  color: #887f70;
  font-size: 14px;
  line-height: 1.1;
  font-weight: 900;
}

.metric-card.gold strong {
  color: var(--gold-soft);
}

.metric-card.success strong {
  color: var(--green);
}

.notice-panel {
  grid-column: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  padding: 10px 14px;
  border: 1px solid rgba(225, 178, 91, 0.2);
  border-radius: 8px;
  background: rgba(18, 19, 17, 0.72);
}

.notice-dot {
  flex: 0 0 auto;
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--gold);
  box-shadow: 0 0 18px rgba(220, 166, 76, 0.58);
}

.notice-panel p {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #d5c8ae;
  font-size: 14px;
  line-height: 1.25;
  font-weight: 850;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  white-space: normal;
  text-overflow: ellipsis;
}

.notice-panel strong {
  margin-right: 8px;
  color: var(--gold-soft);
  font-weight: 950;
}

.notice-panel.success .notice-dot {
  background: var(--green);
  box-shadow: 0 0 18px rgba(184, 217, 134, 0.52);
}

.notice-panel.success strong {
  color: var(--green);
}

.notice-panel.warning .notice-dot {
  background: var(--orange);
  box-shadow: 0 0 18px rgba(242, 156, 85, 0.56);
}

.notice-panel.warning strong {
  color: #ffb777;
}

.notice-panel.danger {
  border-color: rgba(239, 106, 85, 0.42);
  background: rgba(54, 18, 13, 0.72);
}

.notice-panel.danger .notice-dot {
  background: var(--danger);
  box-shadow: 0 0 20px rgba(239, 106, 85, 0.64);
}

.notice-panel.danger strong {
  color: #ff9f92;
}

.desk-board {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(225, 178, 91, 0.2);
  border-radius: 8px;
  background: rgba(12, 13, 12, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.live-board.tables-1 .desk-board,
.live-board.tables-2 .desk-board,
.live-board.tables-3 .desk-board,
.live-board.tables-4 .desk-board {
  align-self: start;
  min-height: 0;
}

.live-board.tables-1 .desk-table,
.live-board.tables-2 .desk-table,
.live-board.tables-3 .desk-table,
.live-board.tables-4 .desk-table {
  min-height: auto;
}

.desk-board-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  min-height: 44px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(225, 178, 91, 0.14);
}

.desk-board-head span {
  color: var(--gold-soft);
  font-size: 17px;
  font-weight: 950;
}

.desk-board-head strong {
  overflow: hidden;
  color: #8e8677;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.desk-table {
  display: grid;
  align-content: start;
  min-height: 0;
  overflow: hidden;
}

.desk-row {
  display: grid;
  grid-template-columns: minmax(130px, 1.1fr) 0.72fr 0.72fr 1.15fr 0.9fr 0.9fr 0.86fr;
  align-items: center;
  gap: 12px;
  min-height: 38px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.055);
  color: #d4c8ae;
}

.desk-row-head {
  min-height: 36px;
  color: #847d70;
  background: rgba(255, 255, 255, 0.025);
  font-size: 12px;
  font-weight: 950;
}

.desk-row strong {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-size: clamp(18px, 1.5vw, 28px);
  line-height: 1;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.desk-row span {
  min-width: 0;
  overflow: hidden;
  font-size: clamp(16px, 1.2vw, 21px);
  font-weight: 900;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.desk-row span b {
  display: inline-block;
  min-width: 46px;
  color: var(--gold-soft);
  font-size: clamp(16px, 1.2vw, 21px);
}

.desk-row em {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  max-width: 96px;
  min-height: 28px;
  padding: 0 10px;
  overflow: hidden;
  color: var(--gold-soft);
  border-radius: 999px;
  background: rgba(220, 166, 76, 0.13);
  font-size: 13px;
  font-style: normal;
  font-weight: 950;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.desk-row.success em {
  color: var(--green);
  background: rgba(184, 217, 134, 0.12);
}

.desk-row.warning em {
  color: #ffb777;
  background: rgba(242, 156, 85, 0.12);
}

.desk-row.danger em {
  color: #ff9f92;
  background: rgba(239, 106, 85, 0.14);
}

.mini-track {
  display: inline-block;
  width: min(120px, 44%);
  height: 7px;
  margin-left: 10px;
  overflow: hidden;
  border-radius: 999px;
  vertical-align: 2px;
  background: rgba(255, 255, 255, 0.08);
}

.mini-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--lager), var(--teal));
}

.desk-empty {
  display: grid;
  place-content: center;
  gap: 10px;
  min-height: 120px;
  color: var(--muted);
  text-align: center;
}

.desk-empty strong {
  color: var(--text);
  font-size: 30px;
}

.desk-empty span {
  font-size: 16px;
  font-weight: 850;
}

.round-footer {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 36px;
  color: #8f856f;
}

.footer-label {
  color: #bda76f;
  font-size: 14px;
  font-weight: 900;
}

.footer-label::before {
  content: "";
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 10px;
  transform: rotate(45deg);
  background: var(--gold);
}

.round-footer ol {
  display: flex;
  align-items: center;
  gap: 28px;
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
  min-height: 34px;
  padding: 0 13px;
  color: #9b917a;
  border: 1px solid rgba(225, 178, 91, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
  font-size: 14px;
  font-weight: 900;
}

.round-footer li + li::before {
  content: "→";
  position: absolute;
  left: -21px;
  color: rgba(225, 178, 91, 0.38);
}

.round-footer li.current {
  color: var(--gold-soft);
  border-color: var(--line-strong);
  background: rgba(220, 166, 76, 0.13);
}

.round-footer li.muted {
  opacity: 0.52;
}

.round-footer li span {
  color: #9e927a;
  font-weight: 850;
}

.round-footer small {
  overflow: hidden;
  color: rgba(189, 167, 111, 0.76);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 4px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.live-board.tables-5,
.live-board.tables-6,
.live-board.tables-7,
.live-board.tables-8 {
  gap: 12px;
  grid-template-rows: auto minmax(210px, 25vh) minmax(330px, 1fr) auto;
}

.tables-5 .desk-row,
.tables-6 .desk-row,
.tables-7 .desk-row,
.tables-8 .desk-row {
  min-height: 32px;
}

.tables-5 .desk-row strong,
.tables-6 .desk-row strong,
.tables-7 .desk-row strong,
.tables-8 .desk-row strong {
  font-size: 19px;
}

.tables-5 .desk-row span,
.tables-6 .desk-row span,
.tables-7 .desk-row span,
.tables-8 .desk-row span {
  font-size: 16px;
}

@keyframes bubble-rise {
  0% {
    transform: translateY(0) scale(0.8);
    opacity: 0;
  }

  18% {
    opacity: 0.8;
  }

  100% {
    transform: translateY(-170px) scale(1.1);
    opacity: 0;
  }
}

@media (max-width: 1500px) {
  .live-board {
    gap: 12px;
    padding: 20px 26px 14px;
    grid-template-rows: auto minmax(214px, 27vh) minmax(300px, 1fr) auto;
  }

  .board-header {
    grid-template-columns: minmax(0, 1fr) minmax(410px, 38%);
    gap: 26px;
    min-height: 128px;
    padding-bottom: 10px;
  }

  .partner-wall article {
    min-height: 50px;
    padding: 9px 12px;
  }

  .scoreboard-stage {
    grid-template-columns: minmax(0, 1fr) minmax(360px, 29%);
    gap: 12px;
  }

  .hero-panel {
    padding: 22px 30px;
  }

  .metric-panel {
    gap: 9px;
  }

  .metric-card {
    padding: 13px 14px;
  }

  .desk-row {
    min-height: 37px;
    gap: 9px;
  }
}
</style>
