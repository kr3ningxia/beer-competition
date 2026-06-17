<template>
  <main class="dashboard-shell">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">当前比赛</p>
        <h1>{{ currentCompetition?.name || '暂无比赛' }}</h1>
        <div class="meta-row">
          <span>{{ currentCompetition?.competitionDate || currentCompetition?.date || '-' }}</span>
          <span>{{ statusText(currentCompetition?.status) }}</span>
          <span>{{ currentRound?.name || '未创建轮次' }}</span>
        </div>
      </div>
      <div class="toolbar">
        <button class="toolbar-button" type="button" @click="loadDashboard">刷新数据</button>
        <button class="toolbar-button primary" type="button" :disabled="!currentCompetition" @click="openCompetition">
          进入比赛详情
        </button>
      </div>
    </section>

    <section class="summary-grid">
      <article v-for="item in summaryCards" :key="item.label" :class="['metric-card', item.tone]">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel-card">
        <header>
          <div>
            <small>当前轮次</small>
            <h2>桌次进度</h2>
          </div>
          <span>{{ roundTables.length }} 桌</span>
        </header>
        <div class="table-list">
          <div v-for="table in tableRows" :key="table.id || table.name" class="table-row">
            <strong>{{ table.name }}</strong>
            <span>{{ table.entryCount }} 款酒</span>
            <span>{{ table.primary }}</span>
            <em>{{ table.status }}</em>
          </div>
          <p v-if="!tableRows.length" class="empty-line">当前还没有可展示的轮次桌。</p>
        </div>
      </article>

      <article class="panel-card">
        <header>
          <div>
            <small>现场关注</small>
            <h2>待处理事项</h2>
          </div>
          <span>{{ issueRows.length }} 项</span>
        </header>
        <div class="issue-list">
          <div v-for="issue in issueRows" :key="issue" class="issue-row">{{ issue }}</div>
          <p v-if="!issueRows.length" class="empty-line">暂无需要处理的事项。</p>
        </div>
      </article>
    </section>

    <section class="panel-card">
      <header>
        <div>
          <small>比赛台账</small>
          <h2>近期比赛</h2>
        </div>
        <span>{{ competitions.length }} 场</span>
      </header>
      <div class="competition-list">
        <button v-for="item in competitions" :key="item.id" type="button" @click="selectCompetition(item)">
          <strong>{{ item.name }}</strong>
          <span>{{ statusText(item.status) }}</span>
          <em>查看</em>
        </button>
        <p v-if="!competitions.length" class="empty-line">暂无比赛。</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCompetitionProgress, fetchCompetitions } from '@/api/admin'

const router = useRouter()
const competitions = ref([])
const detail = ref(null)
const selectedId = ref(null)

const currentCompetition = computed(() => detail.value || competitions.value.find((item) => item.id === selectedId.value) || competitions.value[0] || null)
const rounds = computed(() => detail.value?.rounds || [])
const currentRound = computed(() => detail.value?.currentRound || rounds.value.find((round) => ['PUBLISHED', 'IN_PROGRESS', 'SUBMITTED'].includes(round.status)) || rounds.value[rounds.value.length - 1] || null)
const roundTables = computed(() => currentRound.value?.tables || [])

const summaryCards = computed(() => {
  const entries = detail.value?.entriesSummary || {}
  const progress = detail.value?.progressSummary || {}
  const rankingSubmitted = roundTables.value.filter((table) => ['SUBMITTED', 'LOCKED'].includes(table.status)).length
  const isRanking = currentRound.value?.type === 'RANKING'
  return [
    { label: '参赛酒款', value: entries.total || 0, hint: '本场全部酒款', tone: 'neutral' },
    { label: '已入库', value: entries.stored || 0, hint: '可进入评审', tone: 'success' },
    { label: isRanking ? '排序桌提交' : '桌长汇总', value: isRanking ? `${rankingSubmitted} / ${roundTables.value.length}` : `${progress.finalized || 0} / ${progress.total || 0}`, hint: isRanking ? '等待组委会确认' : '首轮评分制', tone: 'gold' },
    { label: '平均耗时', value: formatMinutes(progress.averageReviewMinutes), hint: '本轮提交', tone: 'neutral' },
    { label: '反馈异常', value: progress.commentWarnings || 0, hint: '复核兜底', tone: (progress.commentWarnings || 0) > 0 ? 'warning' : 'success' },
    { label: '结果发布', value: detail.value?.resultSetup?.published ? '已发布' : '未发布', hint: detail.value?.resultSetup?.canPublishResults ? '可发布' : '按流程推进', tone: detail.value?.resultSetup?.published ? 'success' : 'warning' },
  ]
})

const tableRows = computed(() => roundTables.value.map((table) => {
  const isRanking = currentRound.value?.type === 'RANKING'
  const selected = (table.rankings || []).filter((slot) => slot.uuid).length
  return {
    id: table.id,
    name: table.name,
    entryCount: table.entryUuids?.length || 0,
    primary: isRanking ? `排序 ${selected} / ${table.targetCount || 0}` : `桌长汇总 ${Math.round(Number(table.captainProgress || 0))}%`,
    status: roundStatusText(table.status),
  }
}))

const issueRows = computed(() => {
  const issues = [...(detail.value?.dataIntegrityIssues || [])]
  roundTables.value.forEach((table) => {
    if (!table.captainPublicId) issues.push(`${table.name}缺少桌长`)
    if (!(table.entryUuids || []).length) issues.push(`${table.name}尚未分配酒款`)
  })
  return issues.slice(0, 8)
})

onMounted(loadDashboard)

async function loadDashboard() {
  competitions.value = await fetchCompetitions()
  const target = competitions.value.find((item) => ['JUDGING', 'RESULT_CONFIRMING', 'JUDGING_PREP'].includes(item.status)) || competitions.value[0]
  if (target) await selectCompetition(target, false)
}

async function selectCompetition(item, navigate = false) {
  selectedId.value = item.id
  detail.value = await fetchCompetitionProgress(item.id)
  if (navigate) openCompetition()
}

function openCompetition() {
  if (!currentCompetition.value?.id) return
  router.push(`/admin/competitions/${currentCompetition.value.id}`)
}

function statusText(status) {
  const labels = {
    DRAFT: '草稿',
    REGISTRATION_OPEN: '报名中',
    REGISTRATION_CLOSED: '报名已截止',
    JUDGING_PREP: '评审准备中',
    JUDGING: '评审中',
    RESULT_CONFIRMING: '结果确认',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }
  return labels[status] || status || '-'
}

function roundStatusText(status) {
  const labels = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    IN_PROGRESS: '进行中',
    SUBMITTED: '待确认',
    LOCKED: '已锁定',
  }
  return labels[status] || status || '-'
}

function formatMinutes(value) {
  const minutes = Number(value || 0)
  if (minutes <= 0) return '-'
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}
</script>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
  padding: 28px;
  color: #e6edf0;
  background:
    radial-gradient(circle at 92% 0%, rgba(216, 169, 53, 0.12), transparent 24rem),
    linear-gradient(180deg, #0f171c 0%, #0a1014 100%);
}

.hero-panel,
.panel-card,
.metric-card {
  border: 1px solid rgba(218, 231, 236, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.84);
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 24px;
}

.eyebrow,
.panel-card small {
  margin: 0 0 8px;
  color: #d8a935;
  font-size: 12px;
  font-weight: 800;
}

h1,
h2 {
  margin: 0;
}

h1 {
  font-size: 34px;
}

h2 {
  font-size: 20px;
}

.meta-row,
.toolbar,
.panel-card header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-row {
  margin-top: 12px;
  color: #9fb1b9;
}

.toolbar {
  align-self: flex-start;
}

.toolbar-button {
  min-height: 42px;
  border: 1px solid rgba(218, 231, 236, 0.16);
  border-radius: 8px;
  padding: 0 16px;
  color: #dce9ed;
  background: rgba(255, 255, 255, 0.04);
  font-weight: 800;
}

.toolbar-button.primary {
  color: #1b1408;
  background: #d8a935;
}

.toolbar-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.metric-card {
  padding: 18px;
}

.metric-card span,
.metric-card small {
  color: #9fb1b9;
}

.metric-card strong {
  display: block;
  margin: 8px 0;
  font-size: 30px;
}

.metric-card.gold strong {
  color: #d8a935;
}

.metric-card.success strong {
  color: #6fcf7a;
}

.metric-card.warning strong {
  color: #f2994a;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.6fr);
  gap: 16px;
  margin-top: 16px;
}

.panel-card {
  padding: 18px;
}

.panel-card header {
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-card header > span {
  color: #9fb1b9;
}

.table-list,
.issue-list,
.competition-list {
  display: grid;
  gap: 10px;
}

.table-row,
.issue-row,
.competition-list button {
  display: grid;
  grid-template-columns: 1fr auto auto auto;
  gap: 12px;
  align-items: center;
  min-height: 46px;
  border: 1px solid rgba(218, 231, 236, 0.08);
  border-radius: 8px;
  padding: 0 12px;
  color: #dce9ed;
  background: rgba(255, 255, 255, 0.035);
}

.issue-row {
  display: block;
  min-height: 0;
  padding: 12px;
  color: #ffd08a;
}

.competition-list button {
  width: 100%;
  text-align: left;
}

.table-row span,
.competition-list span {
  color: #9fb1b9;
}

.table-row em,
.competition-list em {
  color: #d8a935;
  font-style: normal;
}

.panel-card + .panel-card {
  margin-top: 16px;
}

.empty-line {
  margin: 0;
  color: #7d9199;
}

@media (max-width: 1100px) {
  .summary-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    display: grid;
  }
}
</style>
