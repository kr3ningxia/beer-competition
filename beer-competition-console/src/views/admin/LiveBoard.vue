<template>
  <main :class="['live-board', densityClass]">
    <header class="board-header">
      <div class="event-copy">
        <span class="eyebrow">现场投屏看板</span>
        <h1>{{ board.title }}</h1>
        <p>{{ board.dateText }} · {{ board.roundText }} · {{ board.statusText }}</p>
      </div>

      <div class="board-actions">
        <div class="refresh-pill">
          <Refresh />
          <span>{{ loading ? '刷新中' : `${refreshCountdown} 秒后刷新` }}</span>
          <strong>{{ currentTimeText }}</strong>
        </div>
        <button type="button" @click="manualRefresh">
          <Refresh />
          刷新
        </button>
        <button type="button" @click="returnToRounds">
          <Back />
          返回轮次编排
        </button>
      </div>
    </header>

    <section class="metric-strip" aria-label="核心进度">
      <article v-for="metric in board.metrics" :key="metric.label" :class="['metric-tile', metric.tone]">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.hint }}</small>
      </article>
    </section>

    <section class="board-body">
      <div class="table-panel">
        <div class="section-title">
          <h2>评审桌进度</h2>
          <span>{{ board.tables.length }} 桌 · {{ board.tableModeText }}</span>
        </div>

        <div class="table-board-grid">
          <article v-for="table in board.tables" :key="table.id" :class="['table-tile', table.tone]">
            <header>
              <div>
                <b>{{ table.shortName }}</b>
                <h3>{{ table.name }}</h3>
              </div>
              <em>{{ table.statusText }}</em>
            </header>

            <p class="captain-line">{{ table.entryCount }} 款 · 桌长 {{ table.captainName }}</p>
            <p class="state-line">{{ table.stateLine }}</p>

            <div class="progress-pair">
              <div>
                <span>{{ table.primaryLabel }}</span>
                <strong>{{ table.primaryText }}</strong>
                <i><b :style="{ width: `${table.primaryPercent}%` }" /></i>
                <small>{{ table.primaryHint }}</small>
              </div>
              <div>
                <span>{{ table.secondaryLabel }}</span>
                <strong>{{ table.secondaryText }}</strong>
                <i><b :style="{ width: `${table.secondaryPercent}%` }" /></i>
                <small>{{ table.secondaryHint }}</small>
              </div>
            </div>

            <footer>
              <span>{{ table.targetLabel }} {{ table.targetText }}</span>
              <small>{{ table.issueText || table.nextText }}</small>
            </footer>
          </article>
          <article v-if="!board.tables.length" class="table-empty">
            <strong>等待轮次发布</strong>
            <span>轮次创建并发布后，评审桌进度会显示在这里。</span>
          </article>
        </div>
      </div>

      <aside class="side-panel">
        <section>
          <div class="section-title compact">
            <h2>轮次路径</h2>
          </div>
          <div class="round-path-board">
            <span v-for="round in board.roundPath" :key="round.key" :class="round.state">
              <strong>{{ round.label }}</strong>
              <small>{{ round.detail }}</small>
            </span>
          </div>
        </section>

        <section>
          <div class="section-title compact">
            <h2>现场提示</h2>
            <span>{{ board.issueCount }} 项</span>
          </div>
          <div class="issue-list">
            <p v-if="!board.issues.length" class="empty-issue">暂无影响评审推进的问题</p>
            <p v-for="issue in board.issues" :key="issue" class="issue-item">{{ issue }}</p>
          </div>
        </section>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Refresh } from '@element-plus/icons-vue'
import { fetchCompetitionProgress, fetchCompetitions } from '@/api/admin'

const REFRESH_SECONDS = 10

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const selectedCompetitionId = ref(route.query.competitionId || '')
const refreshCountdown = ref(REFRESH_SECONDS)
const currentTime = ref(new Date())
let refreshTimer = null
let clockTimer = null

const board = computed(() => buildBoard(detail.value))
const densityClass = computed(() => {
  const count = board.value.tables.length
  if (count <= 2) return 'few-tables'
  if (count <= 4) return 'normal-tables'
  if (count <= 8) return 'many-tables'
  return 'dense-tables'
})
const currentTimeText = computed(() => formatTime(currentTime.value))

onMounted(async () => {
  await ensureCompetition()
  await refreshBoard()
  startTimers()
})

onUnmounted(() => {
  window.clearInterval(refreshTimer)
  window.clearInterval(clockTimer)
})

async function ensureCompetition() {
  if (selectedCompetitionId.value) return
  const competitions = await fetchCompetitions()
  const target = (competitions || []).find((item) => ['JUDGING', 'JUDGING_PREP'].includes(item.status))
    || (competitions || [])[0]
  selectedCompetitionId.value = target?.id || ''
}

function startTimers() {
  refreshTimer = window.setInterval(async () => {
    refreshCountdown.value -= 1
    if (refreshCountdown.value <= 0) await refreshBoard()
  }, 1000)
  clockTimer = window.setInterval(() => {
    currentTime.value = new Date()
  }, 1000)
}

async function refreshBoard() {
  if (!selectedCompetitionId.value) return
  loading.value = true
  try {
    detail.value = normalizeDetail(await fetchCompetitionProgress(selectedCompetitionId.value))
    refreshCountdown.value = REFRESH_SECONDS
  } finally {
    loading.value = false
  }
}

async function manualRefresh() {
  await refreshBoard()
}

function returnToRounds() {
  if (!selectedCompetitionId.value) {
    router.push('/admin/competitions')
    return
  }
  router.push(`/admin/competitions/${selectedCompetitionId.value}?tab=rounds`)
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
  const tables = (currentRound?.tables || []).map((table) => buildTableTile(currentRound, table))
  const issueTexts = collectIssues(currentRound, tables)
  const metrics = buildMetrics(data, currentRound, tables, issueTexts.length)
  return {
    title: data.name || '现场比赛',
    dateText: formatDate(data.date),
    roundText: currentRound ? `${currentRound.name} · ${currentRound.type === 'SCORE' ? '评分制' : '选择排序'}` : '未创建轮次',
    statusText: resolveStatusText(data.status, currentRound),
    tableModeText: currentRound?.type === 'SCORE' ? '评分与桌长汇总' : '桌长选择排序',
    metrics,
    tables,
    issueCount: issueTexts.length,
    issues: issueTexts.slice(0, 4),
    roundPath: buildRoundPath(rounds, currentRound),
  }
}

function buildEmptyBoard() {
  return {
    title: '现场比赛',
    dateText: '-',
    roundText: '加载中',
    statusText: '准备看板',
    tableModeText: '等待数据',
    metrics: [
      { label: '本轮酒款', value: '-', hint: '等待数据', tone: 'neutral' },
      { label: '普通评分', value: '-', hint: '等待数据', tone: 'neutral' },
      { label: '桌长汇总', value: '-', hint: '等待数据', tone: 'neutral' },
      { label: '已选晋级', value: '-', hint: '等待数据', tone: 'neutral' },
      { label: '待处理', value: '-', hint: '等待数据', tone: 'neutral' },
    ],
    tables: [],
    issues: [],
    issueCount: 0,
    roundPath: [],
  }
}

function buildMetrics(data, round, tables, issueCount) {
  const entryCount = countRoundEntries(round)
  if (round?.type === 'RANKING') {
    const selected = tables.reduce((sum, table) => sum + table.selectedCount, 0)
    const target = tables.reduce((sum, table) => sum + table.targetCount, 0)
    const doneTables = tables.filter((table) => table.tone === 'success').length
    return [
      { label: '候选酒款', value: entryCount, hint: '本轮候选', tone: 'neutral' },
      { label: '桌长排序', value: `${selected} / ${target}`, hint: '已填排序槽位', tone: selected >= target ? 'success' : 'warning' },
      { label: '完成桌数', value: `${doneTables} / ${tables.length}`, hint: '本轮已完成', tone: doneTables === tables.length ? 'success' : 'neutral' },
      { label: '晋级池', value: `${(data.entryPool || []).filter((entry) => entry.advanced).length}`, hint: '已标记晋级', tone: 'gold' },
      { label: '待处理', value: issueCount, hint: '影响推进的问题', tone: issueCount ? 'danger' : 'success' },
    ]
  }
  const judgeAverage = average(tables.map((table) => table.primaryPercent))
  const captainAverage = average(tables.map((table) => table.secondaryPercent))
  const advanced = tables.reduce((sum, table) => sum + table.selectedCount, 0)
  const target = tables.reduce((sum, table) => sum + table.targetCount, 0)
  return [
    { label: '本轮酒款', value: entryCount, hint: '当前轮次酒款', tone: 'neutral' },
    { label: '普通评分', value: `${judgeAverage}%`, hint: '评委评分进度', tone: judgeAverage >= 100 ? 'success' : 'neutral' },
    { label: '桌长汇总', value: `${captainAverage}%`, hint: '桌长确认进度', tone: captainAverage >= 100 ? 'success' : 'warning' },
    { label: '已选晋级', value: `${advanced} / ${target}`, hint: '桌长已选晋级', tone: advanced >= target && target > 0 ? 'success' : 'gold' },
    { label: '待处理', value: issueCount, hint: '影响推进的问题', tone: issueCount ? 'danger' : 'success' },
  ]
}

function buildTableTile(round, table) {
  const entryCount = table.entryUuids?.length || 0
  const captainName = table.captainName || table.captain?.name || '未指定'
  const targetCount = Number(table.targetCount || 0)
  if (round?.type === 'RANKING') {
    const selectedCount = (table.rankings || []).filter((slot) => slot.uuid).length
    const percent = targetCount ? Math.round(selectedCount / targetCount * 100) : 0
    const issueText = getTableIssue(table)
    return {
      id: table.id || table.name,
      name: table.name,
      shortName: table.name?.slice(0, 1) || '桌',
      captainName,
      entryCount,
      targetCount,
      selectedCount,
      primaryLabel: '候选',
      primaryText: `${entryCount} 款`,
      primaryPercent: 100,
      primaryHint: '本轮候选酒款',
      secondaryLabel: '已选',
      secondaryText: `${selectedCount} / ${targetCount}`,
      secondaryPercent: percent,
      secondaryHint: selectedCount ? '桌长已选择排序' : '等待桌长选择排序',
      targetLabel: '排序目标',
      targetText: `${targetCount} 款`,
      statusText: issueText ? '需要处理' : selectedCount >= targetCount && targetCount > 0 ? '已完成' : '排序中',
      stateLine: issueText || (selectedCount >= targetCount && targetCount > 0 ? '本桌排序已完成' : '等待桌长提交本桌排序'),
      nextText: selectedCount >= targetCount && targetCount > 0 ? '等待确认锁定' : '等待桌长提交排序',
      issueText,
      tone: issueText ? 'danger' : selectedCount >= targetCount && targetCount > 0 ? 'success' : 'warning',
    }
  }
  const judgeProgress = normalizePercent(table.judgeProgress)
  const captainProgress = normalizePercent(table.captainProgress)
  const selectedCount = Number(table.advancedCount || 0)
  const issueText = getTableIssue(table)
  let statusText = '评分中'
  let tone = 'neutral'
  if (issueText) {
    statusText = '需要处理'
    tone = 'danger'
  } else if (captainProgress >= 100) {
    statusText = '已完成'
    tone = 'success'
  } else if (judgeProgress >= 100) {
    statusText = '待桌长汇总'
    tone = 'warning'
  } else if (judgeProgress > 0) {
    statusText = '评分中'
    tone = 'neutral'
  }
  const stateLine = issueText
    || (captainProgress >= 100 ? '本桌结果已完成'
      : judgeProgress >= 100 ? '评分完成，等待桌长汇总'
        : judgeProgress > 0 ? '评委正在提交评分'
          : '等待评委提交评分')
  return {
    id: table.id || table.name,
    name: table.name,
    shortName: table.name?.slice(0, 1) || '桌',
    captainName,
    entryCount,
    targetCount,
    selectedCount,
    primaryLabel: '普通评分',
    primaryText: `${judgeProgress}%`,
    primaryPercent: judgeProgress,
    primaryHint: judgeProgress > 0 ? '评委评分正在推进' : '等待评委提交评分',
    secondaryLabel: '桌长汇总',
    secondaryText: `${captainProgress}%`,
    secondaryPercent: captainProgress,
    secondaryHint: captainProgress > 0 ? '桌长正在汇总结果' : '评分完成后由桌长汇总',
    targetLabel: '晋级',
    targetText: `${selectedCount} / ${targetCount}`,
    statusText,
    stateLine,
    nextText: captainProgress >= 100 ? '本桌结果已完成' : judgeProgress >= 100 ? '等待桌长汇总' : '等待评委评分',
    issueText,
    tone,
  }
}

function getTableIssue(table) {
  if (!table.captainPublicId && !table.captainName && !table.captain?.name) return '缺少桌长'
  if (!(table.entryUuids || []).length) return '尚未分配酒款'
  if (!Number(table.targetCount || 0)) return '目标数量未设置'
  return ''
}

function collectIssues(round, tables) {
  if (!round) return ['当前比赛还没有轮次']
  return tables
    .filter((table) => table.issueText)
    .map((table) => `${table.name}：${table.issueText}`)
}

function buildRoundPath(rounds, currentRound) {
  const items = rounds.map((round) => ({
    key: round.id,
    label: round.name,
    detail: `${round.tables?.length || 0} 桌 · ${roundStatusText(round.status)}`,
    state: round.id === currentRound?.id ? 'active' : round.status === 'LOCKED' ? 'done' : 'pending',
  }))
  items.push({
    key: 'result',
    label: '结果',
    detail: rounds.length ? '完成全部轮次后确认' : '等待轮次创建',
    state: 'pending',
  })
  return items
}

function resolveStatusText(status, round) {
  if (round?.status === 'PUBLISHED') return '评审进行中'
  if (round?.status === 'IN_PROGRESS') return '处理中'
  if (round?.status === 'SUBMITTED') return '等待确认'
  if (round?.status === 'LOCKED') return '本轮已锁定'
  if (status === 'JUDGING') return '评审进行中'
  return '现场准备中'
}

function roundStatusText(status) {
  const labels = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    IN_PROGRESS: '处理中',
    SUBMITTED: '已提交',
    LOCKED: '已锁定',
  }
  return labels[status] || status || '-'
}

function countRoundEntries(round) {
  return new Set((round?.tables || []).flatMap((table) => table.entryUuids || [])).size
}

function normalizePercent(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number)) return 0
  return Math.max(0, Math.min(100, Math.round(number)))
}

function average(values) {
  if (!values.length) return 0
  return Math.round(values.reduce((sum, value) => sum + normalizePercent(value), 0) / values.length)
}

function formatDate(value) {
  if (!value) return '比赛日未设置'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

function formatTime(value) {
  return `${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}:${String(value.getSeconds()).padStart(2, '0')}`
}
</script>

<style scoped>
.live-board {
  --board-bg: #081116;
  --panel: rgba(19, 31, 36, 0.9);
  --panel-soft: rgba(255, 255, 255, 0.035);
  --line: rgba(219, 232, 237, 0.12);
  --text: #ecf4f6;
  --muted: #91a7af;
  --gold: #e0b84a;
  --green: #75d783;
  --amber: #f1bd79;
  --red: #ff7d74;
  position: fixed;
  inset: 0;
  display: grid;
  grid-template-rows: 126px 138px minmax(0, 1fr);
  gap: 14px;
  padding: 28px 32px 24px;
  overflow: hidden;
  color: var(--text);
  background:
    radial-gradient(circle at 92% 14%, rgba(224, 184, 74, 0.16), transparent 15rem),
    radial-gradient(circle at 14% 10%, rgba(216, 169, 53, 0.14), transparent 22rem),
    radial-gradient(circle at 82% 82%, rgba(117, 215, 131, 0.08), transparent 18rem),
    linear-gradient(rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    var(--board-bg);
  background-size: auto, 96px 96px, 96px 96px, auto;
}

.live-board::before {
  content: "";
  position: absolute;
  top: 0;
  left: 8vw;
  right: 8vw;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(224, 184, 74, 0.88), transparent);
  box-shadow: 0 0 36px rgba(224, 184, 74, 0.38);
}

.live-board::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.26;
  background-image:
    radial-gradient(circle, rgba(255, 255, 255, 0.2) 0 1px, transparent 1.5px),
    radial-gradient(circle, rgba(224, 184, 74, 0.22) 0 1px, transparent 1.5px);
  background-position: 28px 34px, 92px 76px;
  background-size: 140px 120px, 190px 160px;
}

.live-board > * {
  position: relative;
  z-index: 1;
}

.board-header,
.metric-tile,
.table-tile,
.side-panel section {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.26);
}

.board-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 18px 24px;
}

.eyebrow,
.section-title span,
.metric-tile span,
.metric-tile small,
.captain-line,
.side-panel small,
.round-path-board small,
.issue-list p {
  color: var(--muted);
}

.event-copy h1,
.event-copy p,
.section-title h2,
.table-tile h3,
.captain-line,
.issue-list p {
  margin: 0;
}

.event-copy h1 {
  margin-top: 6px;
  font-size: clamp(34px, 3vw, 52px);
  line-height: 1.04;
}

.event-copy p {
  margin-top: 10px;
  font-size: 20px;
}

.board-actions {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 10px;
  align-items: center;
}

.board-actions button,
.refresh-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 13px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font: inherit;
  font-weight: 800;
  white-space: nowrap;
}

.board-actions button:last-child {
  opacity: 0.78;
}

.refresh-pill strong {
  color: var(--gold);
}

.board-actions svg {
  width: 17px;
  height: 17px;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.metric-tile {
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 14px 18px;
  border-left: 3px solid rgba(224, 184, 74, 0.3);
}

.metric-tile strong {
  font-size: clamp(32px, 3.1vw, 54px);
  line-height: 1;
}

.metric-tile.success strong {
  color: var(--green);
}

.metric-tile.warning strong,
.metric-tile.gold strong {
  color: var(--gold);
}

.metric-tile.danger strong {
  color: var(--red);
}

.board-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 16px;
  min-height: 0;
}

.few-tables .board-body,
.normal-tables .board-body {
  grid-template-columns: 1fr;
  grid-template-rows: minmax(0, 1fr) auto;
}

.table-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
  min-height: 0;
}

.section-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
}

.section-title h2 {
  font-size: 24px;
}

.table-board-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  align-content: start;
  min-height: 0;
}

.few-tables .table-board-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.normal-tables .table-board-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.many-tables .table-board-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dense-tables .table-board-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.table-tile {
  display: grid;
  align-content: start;
  gap: 14px;
  min-height: 0;
  padding: 18px;
}

.few-tables .table-tile,
.normal-tables .table-tile {
  min-height: 330px;
}

.table-tile.success {
  border-color: rgba(117, 215, 131, 0.32);
}

.table-tile.warning {
  border-color: rgba(224, 184, 74, 0.38);
}

.table-tile.danger {
  border-color: rgba(255, 125, 116, 0.46);
  background: linear-gradient(180deg, rgba(255, 125, 116, 0.08), rgba(19, 31, 36, 0.9));
}

.table-tile header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.table-tile header > div {
  display: flex;
  gap: 12px;
  align-items: center;
}

.table-tile b {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  color: var(--gold);
  border: 1px solid rgba(216, 169, 53, 0.28);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.1);
  font-size: 22px;
}

.table-tile h3 {
  font-size: 30px;
}

.table-tile em {
  padding: 7px 10px;
  color: var(--green);
  font-style: normal;
  font-weight: 900;
  border: 1px solid rgba(117, 215, 131, 0.24);
  border-radius: 8px;
  background: rgba(117, 215, 131, 0.09);
}

.table-tile.warning em {
  color: var(--gold);
  border-color: rgba(224, 184, 74, 0.28);
  background: rgba(224, 184, 74, 0.1);
}

.table-tile.danger em {
  color: var(--red);
  border-color: rgba(255, 125, 116, 0.32);
  background: rgba(255, 125, 116, 0.12);
}

.captain-line {
  font-size: 18px;
}

.state-line {
  margin: -4px 0 0;
  color: var(--text);
  font-size: 20px;
  font-weight: 900;
}

.progress-pair {
  display: grid;
  gap: 18px;
}

.progress-pair div {
  display: grid;
  grid-template-columns: minmax(80px, 1fr) auto;
  gap: 7px 8px;
  align-items: center;
}

.progress-pair span {
  color: var(--muted);
  font-size: 17px;
}

.progress-pair strong {
  font-size: 20px;
}

.progress-pair i {
  grid-column: 1 / -1;
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.progress-pair small {
  grid-column: 1 / -1;
  color: var(--muted);
  font-size: 14px;
}

.progress-pair i b {
  display: block;
  width: 0;
  height: 100%;
  border: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--green), #a9e48d);
}

.progress-pair div:nth-child(2) i b {
  background: linear-gradient(90deg, var(--gold), #f3d783);
}

.table-tile footer {
  display: grid;
  gap: 6px;
  color: var(--gold);
  font-size: 17px;
  font-weight: 900;
}

.table-tile footer small {
  color: var(--muted);
  font-size: 14px;
  font-weight: 700;
}

.table-tile.danger footer small {
  color: #ffb4ae;
}

.side-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.few-tables .side-panel,
.normal-tables .side-panel {
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.75fr);
  grid-template-rows: auto;
}

.side-panel section {
  display: grid;
  gap: 12px;
  padding: 13px 14px;
}

.section-title.compact h2 {
  font-size: 20px;
}

.round-path-board {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.round-path-board span {
  display: grid;
  gap: 4px;
  min-width: 132px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-soft);
}

.round-path-board span.active {
  color: var(--gold);
  border-color: rgba(224, 184, 74, 0.28);
  background: rgba(224, 184, 74, 0.08);
}

.round-path-board span.done {
  color: var(--green);
}

.issue-list {
  display: grid;
  gap: 8px;
  align-content: start;
  min-height: 0;
}

.few-tables .issue-list,
.normal-tables .issue-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.issue-item,
.empty-issue {
  grid-column: 1 / -1;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.issue-item {
  color: #ffb4ae;
  border-color: rgba(255, 125, 116, 0.24);
  background: rgba(255, 125, 116, 0.08);
}

.table-empty {
  display: grid;
  place-content: center;
  gap: 10px;
  min-height: 260px;
  color: var(--muted);
  text-align: center;
  border: 1px dashed rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.table-empty strong {
  color: var(--text);
  font-size: 30px;
}

@media (max-width: 1400px) {
  .live-board {
    padding: 20px;
    grid-template-rows: 108px 128px minmax(0, 1fr);
  }

  .board-body {
    grid-template-columns: minmax(0, 1fr) 300px;
  }

  .table-tile h3 {
    font-size: 24px;
  }
}
</style>
