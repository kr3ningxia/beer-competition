<template>
  <main class="workspace-page">
    <AdminPageHeader title="工作台">
      <template #actions>
        <button class="icon-button" type="button" title="刷新工作台" aria-label="刷新工作台" :disabled="loading" @click="loadDashboard">
          <Refresh :class="{ spinning: loading }" />
        </button>
        <button class="primary-button" type="button" @click="router.push('/admin/competitions/new')">
          <Plus />
          新建比赛
        </button>
      </template>
    </AdminPageHeader>

    <section class="summary-grid" aria-label="赛事概览">
      <article v-for="item in summaryCards" :key="item.label" :class="['metric-card', item.tone]">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section v-if="!loading && !competitions.length" class="empty-workspace">
      <span class="empty-icon"><Medal /></span>
      <strong>暂无比赛</strong>
      <button class="primary-button" type="button" @click="router.push('/admin/competitions/new')">
        <Plus />
        新建第一场比赛
      </button>
    </section>

    <template v-else-if="competitions.length">
      <section class="focus-layout">
        <article class="focus-panel">
          <header>
            <div>
              <span class="status-badge">{{ statusText(currentCompetition?.status) }}</span>
              <h2>{{ currentCompetition?.name }}</h2>
            </div>
            <button class="text-button" type="button" @click="openCompetition">
              进入比赛
              <Right />
            </button>
          </header>

          <div class="focus-stats">
            <div>
              <span>参赛酒款</span>
              <strong>{{ currentEntries.total || 0 }}</strong>
            </div>
            <div>
              <span>已入库</span>
              <strong>{{ currentEntries.stored || 0 }}</strong>
            </div>
            <div>
              <span>评审桌</span>
              <strong>{{ roundTables.length }}</strong>
            </div>
            <div>
              <span>结果发布</span>
              <strong class="state-value">{{ detail?.resultSetup?.published ? '已发布' : '未发布' }}</strong>
            </div>
          </div>
        </article>

        <article class="issues-panel">
          <header>
            <h2>待处理事项</h2>
            <span>{{ issueRows.length }} 项</span>
          </header>
          <div class="issue-list">
            <button v-for="issue in issueRows" :key="issue" type="button" @click="openCompetition">
              <span>{{ issue }}</span>
              <Right />
            </button>
            <p v-if="!issueRows.length">当前没有待处理事项</p>
          </div>
        </article>
      </section>

      <section class="competition-ledger">
        <header>
          <h2>近期比赛</h2>
          <button class="text-button" type="button" @click="router.push('/admin/competitions')">
            全部比赛
            <Right />
          </button>
        </header>
        <div class="ledger-head">
          <span>比赛</span>
          <span>状态</span>
          <span>比赛日期</span>
          <span>报名酒款</span>
          <span>操作</span>
        </div>
        <button
          v-for="item in competitions.slice(0, 6)"
          :key="item.id"
          class="ledger-row"
          type="button"
          @click="selectCompetition(item, true)"
        >
          <strong>{{ item.name }}</strong>
          <span>{{ statusText(item.status) }}</span>
          <span>{{ formatDate(item.competitionDate || item.date) }}</span>
          <span>{{ item.entriesSummary?.total || 0 }}</span>
          <em>查看</em>
        </button>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Medal, Plus, Refresh, Right } from '@element-plus/icons-vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import { fetchCompetitionProgress, fetchCompetitions } from '@/api/admin'
import { getAdminType } from '@/utils/auth'
import { ADMIN_TYPES } from '@/config/adminAccess'

const router = useRouter()
const competitions = ref([])
const detail = ref(null)
const selectedId = ref(null)
const loading = ref(false)

const currentCompetition = computed(() => detail.value
  || competitions.value.find((item) => item.id === selectedId.value)
  || competitions.value[0]
  || null)
const currentEntries = computed(() => detail.value?.entriesSummary || currentCompetition.value?.entriesSummary || {})
const currentRound = computed(() => {
  const rounds = detail.value?.rounds || []
  return detail.value?.currentRound
    || rounds.find((round) => ['PUBLISHED', 'IN_PROGRESS', 'SUBMITTED'].includes(round.status))
    || rounds[rounds.length - 1]
    || null
})
const roundTables = computed(() => currentRound.value?.tables || [])
const summaryCards = computed(() => [
  { label: '全部比赛', value: competitions.value.length, tone: 'gold' },
  { label: '报名中', value: competitions.value.filter((item) => item.status === 'REGISTRATION_OPEN').length, tone: 'green' },
  { label: '待确认收款', value: competitions.value.reduce((total, item) => total + Number(item.entriesSummary?.pendingPayment || 0), 0), tone: 'orange' },
  { label: '评审进行中', value: competitions.value.filter((item) => ['JUDGING_PREP', 'JUDGING', 'RESULT_CONFIRMING'].includes(item.status)).length, tone: 'blue' },
])
const issueRows = computed(() => {
  const issues = [...(detail.value?.dataIntegrityIssues || [])]
  roundTables.value.forEach((table) => {
    if (!table.captainPublicId) issues.push(`${table.name}缺少桌长`)
    if (!(table.entryUuids || []).length) issues.push(`${table.name}尚未分配酒款`)
  })
  return issues.slice(0, 6)
})

onMounted(loadDashboard)

async function loadDashboard() {
  loading.value = true
  try {
    competitions.value = await fetchCompetitions() || []
    const target = competitions.value.find((item) => ['JUDGING', 'RESULT_CONFIRMING', 'JUDGING_PREP'].includes(item.status))
      || competitions.value.find((item) => item.status === 'REGISTRATION_OPEN')
      || competitions.value[0]
    if (target) await selectCompetition(target, false)
    else detail.value = null
  } finally {
    loading.value = false
  }
}

async function selectCompetition(item, navigate = false) {
  selectedId.value = item.id
  detail.value = await fetchCompetitionProgress(item.id)
  if (navigate) openCompetition()
}

function openCompetition() {
  if (currentCompetition.value?.id) router.push(`/admin/competitions/${currentCompetition.value.id}`)
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

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '-'
}
</script>

<style scoped>
.workspace-page {
  --panel: rgba(21, 31, 35, 0.92);
  --line: rgba(218, 231, 236, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --gold: #d8a935;
  height: 100vh;
  padding: 0 28px 18px;
  overflow-y: auto;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    #0d1519;
  background-size: 48px 48px;
}

.head-actions, .focus-panel header, .issues-panel header, .competition-ledger header, .text-button, .primary-button, .icon-button {
  display: flex;
  align-items: center;
}

h1, h2 { margin: 0; letter-spacing: 0; }
h2 { font-size: 18px; }
.head-actions { gap: 9px; }
.primary-button, .icon-button, .text-button { justify-content: center; gap: 7px; min-height: 40px; border-radius: 7px; font-weight: 800; }
.primary-button { padding: 0 13px; color: #1a1509; border: 1px solid #d8a935; background: #d8a935; }
.primary-button svg, .icon-button svg, .text-button svg { width: 17px; height: 17px; }
.icon-button { width: 40px; padding: 0; color: #b6c6cc; border: 1px solid var(--line); background: rgba(255, 255, 255, 0.03); }
.icon-button:disabled { opacity: 0.55; }
.spinning { animation: spin 0.8s linear infinite; }

.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-top: 18px; }
.metric-card { min-height: 108px; padding: 17px; border: 1px solid var(--line); border-radius: 7px; background: var(--panel); }
.metric-card span { color: var(--muted); font-size: 13px; }
.metric-card strong { display: block; margin-top: 15px; font-size: 29px; }
.metric-card.gold strong { color: #e0b84a; }
.metric-card.green strong { color: #70cf7c; }
.metric-card.orange strong { color: #f2a65a; }
.metric-card.blue strong { color: #76b9d2; }

.empty-workspace { display: grid; place-items: center; align-content: center; gap: 16px; min-height: calc(100vh - 240px); margin-top: 16px; border: 1px solid var(--line); border-radius: 7px; background: rgba(21, 31, 35, 0.72); }
.empty-workspace > strong { font-size: 21px; }
.empty-icon { display: grid; place-items: center; width: 54px; height: 54px; color: #d8a935; border: 1px solid rgba(216, 169, 53, 0.22); border-radius: 8px; background: rgba(216, 169, 53, 0.07); }
.empty-icon svg { width: 25px; }

.focus-layout { display: grid; grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.55fr); gap: 14px; margin-top: 14px; }
.focus-panel, .issues-panel, .competition-ledger { border: 1px solid var(--line); border-radius: 7px; background: var(--panel); }
.focus-panel, .issues-panel { min-height: 220px; padding: 18px; }
.focus-panel header, .issues-panel header, .competition-ledger header { justify-content: space-between; gap: 16px; }
.focus-panel header > div { display: grid; gap: 10px; min-width: 0; }
.focus-panel h2 { overflow: hidden; font-size: 23px; text-overflow: ellipsis; white-space: nowrap; }
.status-badge { width: fit-content; padding: 5px 8px; color: #e0b84a; border: 1px solid rgba(216, 169, 53, 0.22); border-radius: 6px; background: rgba(216, 169, 53, 0.07); font-size: 12px; }
.text-button { padding: 0; color: #d8a935; border: 0; background: transparent; }
.focus-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-top: 30px; }
.focus-stats > div { display: grid; gap: 8px; padding: 12px; border-left: 2px solid rgba(216, 169, 53, 0.24); background: rgba(255, 255, 255, 0.025); }
.focus-stats span { color: var(--muted); font-size: 12px; }
.focus-stats strong { font-size: 23px; }
.focus-stats .state-value { font-size: 16px; }
.issues-panel header > span { color: var(--muted); font-size: 12px; }
.issue-list { display: grid; gap: 7px; margin-top: 16px; }
.issue-list button { display: grid; grid-template-columns: minmax(0, 1fr) 16px; align-items: center; gap: 8px; min-height: 38px; padding: 0 10px; color: #e7c381; text-align: left; border: 1px solid rgba(216, 169, 53, 0.1); border-radius: 6px; background: rgba(216, 169, 53, 0.04); }
.issue-list button span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.issue-list button svg { width: 15px; }
.issue-list p { margin: 26px 0 0; color: #6fcf7a; text-align: center; }

.competition-ledger { margin-top: 14px; padding: 18px; }
.competition-ledger header { margin-bottom: 14px; }
.ledger-head, .ledger-row { display: grid; grid-template-columns: minmax(220px, 1.4fr) 140px 150px 120px 70px; gap: 12px; align-items: center; }
.ledger-head { padding: 0 12px 8px; color: #69808a; font-size: 12px; }
.ledger-row { width: 100%; min-height: 48px; padding: 0 12px; color: #c7d5da; text-align: left; border: 1px solid rgba(218, 231, 236, 0.07); border-radius: 6px; background: rgba(255, 255, 255, 0.022); }
.ledger-row + .ledger-row { margin-top: 7px; }
.ledger-row:hover { border-color: rgba(216, 169, 53, 0.17); background: rgba(255, 255, 255, 0.035); }
.ledger-row strong, .ledger-row span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ledger-row em { color: #d8a935; font-style: normal; }

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1180px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .focus-layout { grid-template-columns: 1fr; }
  .focus-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ledger-head, .ledger-row { grid-template-columns: minmax(180px, 1fr) 120px 130px 100px 60px; }
}

@media (max-width: 760px) {
  .workspace-page { height: auto; padding: 0 16px 16px; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
  .ledger-head { display: none; }
  .ledger-row { grid-template-columns: 1fr; gap: 5px; padding: 11px; }
}
</style>
