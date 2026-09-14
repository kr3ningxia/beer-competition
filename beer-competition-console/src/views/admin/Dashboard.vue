<template>
  <main class="workspace-page">
    <AdminPageHeader title="工作台">
      <template #actions>
        <button class="icon-button" type="button" title="刷新工作台" aria-label="刷新工作台" :disabled="loading" @click="loadDashboard">
          <Refresh :class="{ spinning: loading }" />
        </button>
      </template>
    </AdminPageHeader>

    <section v-if="!loading && !competitions.length && !pendingApplications.length" class="empty-workspace">
      <span class="empty-icon"><Medal /></span>
      <strong>暂无比赛</strong>
      <button class="primary-button" type="button" @click="router.push('/admin/competitions/new')">
        <Plus />
        新建第一场比赛
      </button>
    </section>

    <template v-else-if="competitions.length || pendingApplications.length">
      <section class="todo-summary" aria-label="待办总览">
        <button v-for="card in summaryCards" :key="card.key" :class="['todo-card', card.tone]" type="button" @click="openSummaryCard(card)">
          <span class="todo-card-icon"><component :is="card.icon" /></span>
          <span class="todo-card-copy">
            <small>{{ card.label }}</small>
            <strong>{{ card.count }}</strong>
            <em v-if="card.unit">{{ card.unit }}</em>
          </span>
          <span v-if="card.hint" class="todo-card-hint"><ShoppingCart />{{ card.hint }}</span>
          <Right v-else />
        </button>
      </section>

      <section class="dashboard-grid">
        <article class="todo-panel">
          <header class="section-header">
            <div><h2>待处理</h2><span>{{ todoCountByFilter }} 项</span></div>
            <button v-if="todoFilter !== 'ALL'" class="text-button" type="button" @click="todoFilter = 'ALL'">查看全部</button>
          </header>
          <nav class="todo-tabs" aria-label="待办类型筛选">
            <button v-for="tab in todoTabs" :key="tab.key" :class="{ active: todoFilter === tab.key }" type="button" @click="todoFilter = tab.key">{{ tab.label }}<b v-if="tab.count">{{ tab.count }}</b></button>
          </nav>
          <div class="todo-list">
            <button v-for="todo in filteredTodos" :key="todo.id" class="todo-row" type="button" @click="openTodo(todo)">
              <span :class="['todo-type', todo.tone]">{{ todo.type }}</span>
              <span class="todo-main"><strong>{{ todo.title }}</strong><small>{{ todo.detail }}</small></span>
              <span class="todo-time">{{ todo.time }}</span>
              <Right />
            </button>
            <p v-if="!filteredTodos.length" class="todo-empty">当前没有待处理事项</p>
          </div>
        </article>

        <article class="active-panel">
          <header class="section-header"><div><h2>赛事</h2><span>{{ activeCompetitions.length }} 场</span></div><button class="text-button" type="button" @click="router.push('/admin/competitions')">全部比赛<Right /></button></header>
          <div class="active-list">
            <button v-for="item in activeCompetitions.slice(0, 6)" :key="item.id" class="active-row" type="button" @click="router.push(`/admin/competitions/${item.id}`)">
              <span class="active-main"><strong>{{ item.name }}</strong><small>{{ statusText(item.status) }} · {{ formatDate(item.competitionDate || item.date) }}</small></span>
              <span class="active-progress"><b>{{ item.entriesSummary?.registered || item.entriesSummary?.total || 0 }}</b><small>报名</small></span>
              <Right />
            </button>
            <p v-if="!activeCompetitions.length" class="todo-empty">暂无赛事</p>
          </div>
        </article>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Coin, Document, Medal, Money, OfficeBuilding, Plus, Refresh, Right, ShoppingCart, User } from '@element-plus/icons-vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import { fetchAdminBankTransfers, fetchAdminEntries, fetchCompetitions, fetchJudgeRecruitments } from '@/api/admin'
import { fetchBeerCoinWallet } from '@/api/beerCoin'
import { fetchOrganizerApplications } from '@/api/organizerApplicationAdmin'
import { ADMIN_TYPES, canAccessAdminTypes } from '@/config/adminAccess'
import { getAdminOrganizerType, getAdminType } from '@/utils/auth'

const router = useRouter()
const competitions = ref([])
const recruitments = ref([])
const transfers = ref([])
const transferTotal = ref(0)
const entryQueues = ref({ payment: null, storage: null, refund: null })
const applications = ref([])
const beerCoinBalance = ref(null)
const loading = ref(false)
const todoFilter = ref('ALL')
const canViewApplications = computed(() => canAccessAdminTypes(getAdminType(), [ADMIN_TYPES.PLATFORM_SUPER_ADMIN]))
const isThirdPartyOrganizer = computed(() => getAdminOrganizerType() === 'TENANT')
const pendingApplications = computed(() => applications.value.filter((item) => ['SUBMITTED', 'UNDER_REVIEW', 'APPROVED'].includes(item.status)))
const applicationTodos = computed(() => pendingApplications.value.slice(0, 5).map(toApplicationTodo))
const entryQueueCount = computed(() => Object.values(entryQueues.value)
  .filter((queue) => queue && queue !== entryQueues.value.payment)
  .reduce((sum, item) => sum + Number(item?.total || 0), 0))
const judgePendingTotal = computed(() => recruitments.value.reduce((sum, item) => sum + Number(item.pendingCount || 0), 0))
const summaryCards = computed(() => [
  ...(canViewApplications.value ? [{ key: 'APPLICATION', label: '入驻申请待处理', count: pendingApplications.value.length, tone: 'green', icon: OfficeBuilding }] : []),
  { key: 'JUDGE', label: '评委报名待审核', count: judgePendingTotal.value, tone: 'gold', icon: User },
  { key: 'TRANSFER', label: '转账待确认', count: transferTotal.value, tone: 'orange', icon: Money },
  { key: 'ENTRY', label: '酒款待处理', count: entryQueueCount.value, tone: 'blue', icon: Document },
  ...(beerCoinBalance.value === null ? [] : [{
    key: 'BEER_COIN',
    label: '啤酒币余额',
    count: formatQuantity(beerCoinBalance.value),
    unit: '枚',
    tone: 'gold',
    icon: Coin,
    path: '/admin/beer-coins',
    hint: '去购买',
  }]),
])
const todos = computed(() => [
  ...applicationTodos.value,
  ...recruitments.value.filter((item) => item.pendingCount).map((item) => ({ id: `judge-${item.id}`, type: '评委报名', tone: 'gold', todoKey: 'JUDGE', title: `${item.competitionName}待审核 ${item.pendingCount} 人`, detail: `招募${item.status === 'OPEN' ? '进行中' : '已截止'}`, time: formatTime(item.recruitmentDeadline), path: `/admin/judge-recruitments/${item.id}` })),
  ...transfers.value.slice(0, 6).map((item) => ({ id: `transfer-${item.id}`, type: '转账', tone: 'orange', todoKey: 'TRANSFER', title: `${item.breweryName || '厂牌'} · ${item.competitionName || '赛事'}`, detail: `${formatMoney(item.amount)} · ${item.voucherFileName ? '凭证已上传' : '缺少付款凭证'}`, time: formatTime(item.submittedTime), path: '/admin/bank-transfers' })),
  ...(entryQueues.value.storage?.records || []).slice(0, 4).map((item) => ({ id: `storage-${item.id}`, type: '酒款', tone: 'blue', todoKey: 'ENTRY', title: `${item.name || '未命名酒款'}待入库`, detail: `${item.breweryCompanyName || '未关联厂牌'} · ${item.competitionName || '赛事'}`, time: formatTime(item.submittedAt), path: '/admin/entries?deliveryStatus=SUBMITTED' })),
  ...(entryQueues.value.refund?.records || []).slice(0, 4).map((item) => ({ id: `refund-${item.id}`, type: '酒款', tone: 'red', todoKey: 'ENTRY', title: `${item.name || '未命名酒款'}退款待处理`, detail: `${item.breweryCompanyName || '未关联厂牌'} · ${item.competitionName || '赛事'}`, time: formatTime(item.refundRequestedAt), path: '/admin/entries?refundStatus=REQUESTED' })),
])
const todoTabs = computed(() => [
  { key: 'ALL', label: '全部', count: judgePendingTotal.value + transferTotal.value + entryQueueCount.value + pendingApplications.value.length },
  ...(canViewApplications.value ? [{ key: 'APPLICATION', label: '入驻申请', count: pendingApplications.value.length }] : []),
  { key: 'JUDGE', label: '评委报名', count: judgePendingTotal.value },
  { key: 'TRANSFER', label: '转账', count: transferTotal.value },
  { key: 'ENTRY', label: '酒款', count: entryQueueCount.value },
])
const todoCountByFilter = computed(() => todoTabs.value.find((tab) => tab.key === todoFilter.value)?.count || 0)
const filteredTodos = computed(() => todos.value.filter((item) => todoFilter.value === 'ALL' || item.todoKey === todoFilter.value).slice(0, 8))
const activeCompetitions = computed(() => competitions.value.filter((item) => !['PUBLISHED', 'ARCHIVED'].includes(item.status)))

function toApplicationTodo(item) {
  const name = item.organizationName || '未填写主体名称'
  const contact = [item.contactName, item.maskedContactPhone].filter(Boolean).join(' · ')
  const base = { id: `application-${item.id}`, type: '入驻申请', tone: 'green', todoKey: 'APPLICATION' }
  if (item.status === 'APPROVED') {
    return { ...base, title: `${name} · 待发放账号`, detail: `审核通过，等待开通后台${contact ? ` · ${contact}` : ''}`, time: formatTime(item.reviewedTime || item.submittedTime), path: '/admin/organizer-applications?status=APPROVED' }
  }
  if (item.status === 'UNDER_REVIEW') {
    return { ...base, title: `${name} · 审核中`, detail: contact || '等待审核结论', time: formatTime(item.submittedTime), path: '/admin/organizer-applications' }
  }
  return {
    ...base,
    title: `${name} · ${item.supplementalNote ? '补充资料后重新提交' : '提交入驻申请'}`,
    detail: `${contact ? `${contact} · ` : ''}等待审核`,
    time: formatTime(item.submittedTime),
    path: '/admin/organizer-applications?status=SUBMITTED',
  }
}

onMounted(loadDashboard)

// 主办方类型来自 /api/admin/me，进入工作台时可能尚未写入，登录态更新后需要补拉余额。
watch(isThirdPartyOrganizer, loadBeerCoinWallet)

async function loadBeerCoinWallet() {
  if (!isThirdPartyOrganizer.value) {
    beerCoinBalance.value = null
    return
  }
  try {
    beerCoinBalance.value = (await fetchBeerCoinWallet())?.availableQuantity ?? null
  } catch {
    // 账户不可用或组织不支持啤酒币时隐藏余额卡，不影响工作台其他数据。
    beerCoinBalance.value = null
  }
}

async function loadDashboard() {
  loading.value = true
  try {
    const [competitionData, recruitmentData, transferData, storageData, refundData, applicationData] = await Promise.all([
      fetchCompetitions({ includeArchived: false }),
      fetchJudgeRecruitments(),
      fetchAdminBankTransfers({ status: 'SUBMITTED', page: 1, pageSize: 20 }),
      fetchAdminEntries({ deliveryStatus: 'SUBMITTED', page: 1, pageSize: 8 }),
      fetchAdminEntries({ refundStatus: 'REQUESTED', page: 1, pageSize: 8 }),
      canViewApplications.value ? fetchOrganizerApplications('ALL') : Promise.resolve([]),
    ])
    competitions.value = competitionData || []
    recruitments.value = recruitmentData || []
    transfers.value = transferData?.records || []
    transferTotal.value = transferData?.total || 0
    entryQueues.value = { payment: null, storage: storageData, refund: refundData }
    applications.value = applicationData || []
    await loadBeerCoinWallet()
  } finally {
    loading.value = false
  }
}

function openSummaryCard(card) {
  if (card.path) {
    router.push(card.path)
    return
  }
  selectTodoType(card.key)
}

function selectTodoType(type) { todoFilter.value = type === 'ENTRY' ? 'ENTRY' : type }
function openTodo(todo) { router.push(todo.path) }

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

function formatTime(value) {
  return value ? new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) : '-'
}

function formatMoney(value) {
  return value === undefined || value === null ? '-' : `¥${Number(value).toFixed(2)}`
}

function formatQuantity(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '0'
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

.todo-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 12px; margin-top: 18px; }
.todo-card { display: flex; align-items: center; gap: 12px; min-height: 94px; padding: 15px; color: var(--text); text-align: left; border: 1px solid var(--line); border-radius: 7px; background: var(--panel); cursor: pointer; }
.todo-card:hover { border-color: rgba(216, 169, 53, 0.35); background: rgba(28, 40, 44, 0.96); }
.todo-card-icon { display: grid; flex: 0 0 auto; place-items: center; width: 36px; height: 36px; border-radius: 7px; background: rgba(216, 169, 53, 0.1); }
.todo-card-icon svg { width: 19px; height: 19px; }
.todo-card-copy { display: flex; flex: 1; min-width: 0; align-items: baseline; gap: 10px; }
.todo-card-copy small { overflow: hidden; color: var(--muted); text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.todo-card-copy strong { flex: 0 0 auto; font-size: 27px; line-height: 1; }
.todo-card-copy em { flex: 0 0 auto; color: var(--muted); font-size: 12px; font-style: normal; }
.todo-card-hint { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 4px; padding: 4px 8px; color: #e0b84a; border: 1px solid rgba(216, 169, 53, 0.28); border-radius: 6px; background: rgba(216, 169, 53, 0.08); font-size: 11px; font-weight: 800; white-space: nowrap; }
.todo-card-hint svg { width: 13px; height: 13px; }
.todo-card.orange .todo-card-icon { color: #f2a65a; background: rgba(242, 166, 90, 0.1); }
.todo-card.blue .todo-card-icon { color: #76b9d2; background: rgba(118, 185, 210, 0.1); }
.todo-card.green .todo-card-icon { color: #70cf7c; background: rgba(112, 207, 124, 0.1); }
.todo-card.orange strong { color: #f2a65a; }
.todo-card.blue strong { color: #76b9d2; }
.todo-card.green strong { color: #70cf7c; }
.todo-card > svg { flex: 0 0 auto; width: 15px; height: 15px; color: #71858d; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(330px, 0.65fr); gap: 14px; margin-top: 14px; }
.todo-panel, .active-panel { min-width: 0; padding: 18px; border: 1px solid var(--line); border-radius: 7px; background: var(--panel); }
.section-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-header > div { display: flex; align-items: baseline; gap: 10px; }
.section-header h2 { font-size: 18px; }
.section-header span { color: var(--muted); font-size: 12px; }
.todo-tabs { display: flex; gap: 6px; margin-top: 16px; border-bottom: 1px solid rgba(218, 231, 236, 0.08); }
.todo-tabs button { min-height: 34px; padding: 0 9px; color: var(--muted); border: 0; border-bottom: 2px solid transparent; background: transparent; cursor: pointer; font-size: 12px; }
.todo-tabs button.active { color: #e0b84a; border-bottom-color: #d8a935; }
.todo-tabs b { margin-left: 5px; color: inherit; font-weight: 700; }
.todo-list, .active-list { display: grid; gap: 7px; margin-top: 12px; }
.todo-row, .active-row { display: grid; align-items: center; gap: 10px; width: 100%; min-width: 0; padding: 10px; color: var(--text); text-align: left; border: 1px solid rgba(218, 231, 236, 0.07); border-radius: 6px; background: rgba(255, 255, 255, 0.022); cursor: pointer; }
.todo-row { grid-template-columns: 66px minmax(0, 1fr) 54px 15px; }
.todo-row:hover, .active-row:hover { border-color: rgba(216, 169, 53, 0.2); background: rgba(255, 255, 255, 0.04); }
.todo-type { width: fit-content; padding: 4px 6px; border-radius: 4px; color: #e0b84a; background: rgba(216, 169, 53, 0.1); font-size: 11px; }
.todo-type.orange { color: #f2a65a; background: rgba(242, 166, 90, 0.1); }
.todo-type.blue { color: #76b9d2; background: rgba(118, 185, 210, 0.1); }
.todo-type.red { color: #ffaaa0; background: rgba(255, 122, 107, 0.1); }
.todo-type.green { color: #70cf7c; background: rgba(112, 207, 124, 0.1); }
.todo-main, .active-main { display: grid; gap: 3px; min-width: 0; }
.todo-main strong, .todo-main small, .active-main strong, .active-main small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.todo-main strong, .active-main strong { font-size: 13px; }
.todo-main small, .active-main small { color: var(--muted); font-size: 11px; }
.todo-time { color: var(--muted); font-size: 11px; text-align: right; white-space: nowrap; }
.todo-row svg, .active-row svg { width: 15px; color: #71858d; }
.todo-empty { margin: 34px 0 26px; color: #70cf7c; text-align: center; font-size: 13px; }
.active-row { grid-template-columns: minmax(0, 1fr) 48px 15px; }
.active-progress { display: grid; justify-items: end; gap: 2px; }
.active-progress b { color: #e0b84a; font-size: 17px; }
.active-progress small { color: var(--muted); font-size: 10px; }

.empty-workspace { display: grid; place-items: center; align-content: center; gap: 16px; min-height: calc(100vh - 160px); margin-top: 16px; border: 1px solid var(--line); border-radius: 7px; background: rgba(21, 31, 35, 0.72); }
.empty-workspace > strong { font-size: 21px; }
.empty-icon { display: grid; place-items: center; width: 54px; height: 54px; color: #d8a935; border: 1px solid rgba(216, 169, 53, 0.22); border-radius: 8px; background: rgba(216, 169, 53, 0.07); }
.empty-icon svg { width: 25px; }

.status-badge { width: fit-content; padding: 5px 8px; color: #e0b84a; border: 1px solid rgba(216, 169, 53, 0.22); border-radius: 6px; background: rgba(216, 169, 53, 0.07); font-size: 12px; }
.text-button { padding: 0; color: #d8a935; border: 0; background: transparent; }

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1180px) {
  .todo-summary { grid-template-columns: 1fr; }
  .dashboard-grid { grid-template-columns: 1fr; }
}

@media (max-width: 760px) {
  .workspace-page { height: auto; padding: 0 16px 16px; }
  .todo-row { grid-template-columns: 58px minmax(0, 1fr) 15px; }
  .todo-time { display: none; }
}
</style>
