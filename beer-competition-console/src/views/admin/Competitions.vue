<template>
  <div class="competition-ledger">
    <section class="page-head">
      <div>
        <h1>比赛管理</h1>
      </div>

      <button v-if="focusCompetition" class="focus-brief" type="button" @click="openQuickView(focusCompetition)">
        <span>当前重点</span>
        <strong>{{ focusCompetition.name }}</strong>
        <em>{{ statusMeta[focusCompetition.status].label }}</em>
        <small>{{ focusCompetition.dataIntegrityIssues.length ? '数据需修正' : `${focusCompetition.alerts.length || '无'} 项待处理` }} · {{ getNextAction(focusCompetition) }}</small>
      </button>

      <div class="head-actions">
        <button class="tool-button" type="button" @click="exportLedger">
          <Download />
          导出筛选
        </button>
        <button class="tool-button primary" type="button" @click="router.push('/admin/competitions/new')">
          <Plus />
          新建比赛
        </button>
      </div>
    </section>

    <section class="filter-bar">
      <label class="search-field">
        <Search />
        <input v-model.trim="keyword" type="search" placeholder="搜索比赛名称、编号" />
      </label>

      <div class="status-tabs" aria-label="比赛状态筛选">
        <button
          v-for="option in statusOptions"
          :key="option.value"
          :class="{ active: selectedStatus === option.value }"
          type="button"
          @click="selectedStatus = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <select v-model="selectedYear" aria-label="年份筛选">
        <option value="ALL">全部年份</option>
        <option value="2026">2026</option>
        <option value="2025">2025</option>
      </select>
    </section>

    <section class="ledger-panel">
      <div class="ledger-header">
        <span>赛事</span>
        <span>状态</span>
        <span>比赛日</span>
        <span>报名</span>
        <span>入库</span>
        <span>评审配置</span>
        <span>配置</span>
        <span>下一步</span>
      </div>

      <button
        v-for="competition in filteredCompetitions"
        :key="competition.id"
        class="ledger-row"
        type="button"
        @click="router.push(`/admin/competitions/${competition.id}`)"
      >
        <span class="event-cell">
          <strong>{{ competition.name }}</strong>
          <small>{{ competition.code }}</small>
        </span>
        <span :class="['state-badge', statusMeta[competition.status].tone]">
          {{ statusMeta[competition.status].label }}
        </span>
        <span>{{ formatDate(competition.date) }}</span>
        <span>{{ competition.entriesSummary.registered }} / {{ competition.entriesSummary.total }}</span>
        <span>{{ competition.entriesSummary.stored }} / {{ competition.entriesSummary.registered }}</span>
        <span>{{ competition.judgeTableCount }} 桌 · {{ getJudgeCount(competition) }} 人</span>
        <span>
          <b>{{ getReadyCount(competition) }} / {{ checkItems.length }}</b>
          <small>{{ getConfigHint(competition) }}</small>
        </span>
        <span class="row-action">
          {{ getNextAction(competition) }}
          <Right />
        </span>
        <button class="quick-button" type="button" title="快速概览" @click.stop="openQuickView(competition)">
          <View />
        </button>
      </button>

      <div v-if="loading" class="empty-state">
        <Search />
        <strong>正在加载赛事</strong>
      </div>

      <div v-else-if="filteredCompetitions.length === 0" class="empty-state">
        <Search />
        <strong>没有符合条件的比赛</strong>
        <p>调整名称、年份或状态筛选后再查看赛事台账。</p>
      </div>
    </section>

    <el-drawer
      v-model="quickVisible"
      append-to-body
      :with-header="false"
      size="430px"
      class="competition-drawer"
      modal-class="dark-drawer-mask"
    >
      <section v-if="selectedCompetition" class="drawer-content">
        <header class="drawer-hero">
          <button class="drawer-close" type="button" title="关闭" @click="quickVisible = false">
            <Close />
          </button>
          <p>{{ selectedCompetition.code }}</p>
          <h2>{{ selectedCompetition.name }}</h2>
          <span :class="['state-badge', statusMeta[selectedCompetition.status].tone]">
            {{ statusMeta[selectedCompetition.status].label }}
          </span>
          <div class="drawer-meta">
            <span>{{ formatDate(selectedCompetition.date) }}</span>
            <span>{{ selectedCompetition.entryFee }} 元 / 款</span>
            <span v-if="earlyBirdText(selectedCompetition)">{{ earlyBirdText(selectedCompetition) }}</span>
            <span>{{ getDateDistance(selectedCompetition.date) }}</span>
          </div>
          <p v-if="selectedCompetition.description" class="drawer-description">{{ selectedCompetition.description }}</p>
        </header>

        <div class="drawer-block">
          <div class="drawer-heading">
            <h3>关键统计</h3>
            <span>{{ getReadyCount(selectedCompetition) }} / {{ checkItems.length }} 配置完成</span>
          </div>
          <div class="drawer-stats">
            <div>
              <small>已报名</small>
              <strong>{{ selectedCompetition.entriesSummary.registered }}</strong>
            </div>
            <div>
              <small>已入库</small>
              <strong>{{ selectedCompetition.entriesSummary.stored }}</strong>
            </div>
            <div>
              <small>评审桌</small>
              <strong>{{ selectedCompetition.judgeTableCount }}</strong>
            </div>
            <div>
              <small>已汇总</small>
              <strong>{{ selectedCompetition.progressSummary.finalized }}</strong>
            </div>
          </div>
        </div>

        <div class="drawer-block">
          <div class="drawer-heading">
            <h3>待关注事项</h3>
            <span>{{ selectedCompetition.dataIntegrityIssues.length || selectedCompetition.alerts.length }} 项</span>
          </div>
          <div class="drawer-alerts">
            <p v-if="selectedCompetition.dataIntegrityIssues.length" class="danger">
              <Warning />
              数据需修正：比赛已开放报名，但缺少开放报名必需配置。
            </p>
            <p v-else-if="selectedCompetition.alerts.length === 0" class="success">
              <CircleCheck />
              关键配置已确认，当前比赛可以继续推进。
            </p>
            <p v-for="alert in selectedCompetition.alerts" v-else :key="alert.text" :class="alert.level">
              <component :is="alert.level === 'danger' ? Warning : Clock" />
              {{ alert.text }}
            </p>
          </div>
        </div>

        <footer class="drawer-footer">
          <button class="tool-button" type="button" @click="quickVisible = false">稍后处理</button>
          <button class="tool-button primary" type="button" @click="goDetail(selectedCompetition)">
            进入详情工作台
            <Right />
          </button>
        </footer>
      </section>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  CircleCheck,
  Clock,
  Close,
  Download,
  Plus,
  Right,
  Search,
  View,
  Warning,
} from '@element-plus/icons-vue'
import {
  checkItems,
  formatDate,
  getDateDistance,
  statusMeta,
} from './competitionStore'
import { fetchCompetitions } from '@/api/admin'

const router = useRouter()
const competitions = ref([])
const loading = ref(false)
const keyword = ref('')
const selectedStatus = ref('ALL')
const selectedYear = ref('ALL')
const quickVisible = ref(false)
const selectedCompetition = ref(null)

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '报名中', value: 'REGISTRATION_OPEN' },
  { label: '评审准备', value: 'JUDGING_PREP' },
  { label: '评审中', value: 'JUDGING' },
  { label: '结果确认', value: 'RESULT_CONFIRMING' },
  { label: '已发布', value: 'PUBLISHED' },
]

const focusCompetition = computed(() => {
  return competitions.value.find((item) => item.status === 'JUDGING_PREP')
    || competitions.value.find((item) => item.status === 'REGISTRATION_OPEN')
    || competitions.value[0]
})

const filteredCompetitions = computed(() => {
  const normalizedKeyword = keyword.value.toLowerCase()
  return competitions.value.filter((item) => {
    const matchesKeyword = !normalizedKeyword
      || item.name.toLowerCase().includes(normalizedKeyword)
      || item.code.toLowerCase().includes(normalizedKeyword)
    const matchesStatus = selectedStatus.value === 'ALL' || item.status === selectedStatus.value
    const matchesYear = selectedYear.value === 'ALL' || item.date?.startsWith(selectedYear.value)
    return matchesKeyword && matchesStatus && matchesYear
  })
})

onMounted(loadCompetitions)

async function loadCompetitions() {
  loading.value = true
  try {
    const data = await fetchCompetitions()
    competitions.value = data.map(normalizeCompetition)
  } finally {
    loading.value = false
  }
}

function normalizeCompetition(item) {
  const alertCount = Number(item.alertCount || 0)
  return {
    ...item,
    date: item.competitionDate,
    entriesSummary: item.entriesSummary || {
      total: 0,
      pendingPayment: 0,
      registered: 0,
      stored: 0,
      canceled: 0,
      resultPublished: 0,
    },
    progressSummary: item.progressSummary || { finalized: 0, total: 0, advanced: 0, commentWarnings: 0 },
    judgeTableCount: Number(item.judgeTableCount || 0),
    judgeCount: Number(item.judgeCount || 0),
    dataIntegrityIssues: item.dataIntegrityIssues || [],
    primaryAction: item.primaryAction || null,
    alerts: item.dataIntegrityIssues?.length
      ? []
      : alertCount
        ? [{ level: 'warning', text: `${alertCount} 项配置或流程事项需要关注` }]
        : [],
  }
}

function openQuickView(competition) {
  selectedCompetition.value = competition
  quickVisible.value = true
}

function goDetail(competition) {
  quickVisible.value = false
  router.push(`/admin/competitions/${competition.id}`)
}

function getJudgeCount(competition) {
  return competition.judgeCount
}

function getConfigHint(competition) {
  if (competition.dataIntegrityIssues.length) {
    return '需修正'
  }
  if (getReadyCount(competition) === checkItems.length) {
    return '完整'
  }
  if (competition.alerts.some((alert) => alert.level === 'danger')) {
    return '阻塞'
  }
  return '待确认'
}

function exportLedger() {
  ElMessage.info('请进入比赛详情导出评分数据')
}

function getReadyCount(competition) {
  return Number(competition.readyCount || 0)
}

function getNextAction(competition) {
  return competition.dataIntegrityIssues.length
    ? '修正数据'
    : competition.primaryAction?.text || statusMeta[competition.status].next
}

function earlyBirdText(competition) {
  if (competition?.earlyBirdFee === null || competition?.earlyBirdFee === undefined || !competition?.earlyBirdDeadline) return ''
  return `早鸟 ${competition.earlyBirdFee} 元 / 款，截止 ${formatDateTime(competition.earlyBirdDeadline)}`
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16).replaceAll('-', '.') : '-'
}
</script>

<style scoped>
.competition-ledger {
  --panel: rgba(22, 32, 36, 0.9);
  --panel-strong: rgba(26, 39, 44, 0.96);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold: #d8a935;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --blue: #6fb4cf;
  --orange: #f2994a;
  --red: #e05252;
  position: relative;
  height: 100vh;
  padding: 26px 28px;
  color: var(--text);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(216, 169, 53, 0.12), transparent 18rem),
    radial-gradient(circle at 86% 12%, rgba(111, 180, 207, 0.08), transparent 19rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto, auto;
}

.competition-ledger::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: "";
  background: repeating-linear-gradient(110deg, transparent 0 18px, rgba(216, 169, 53, 0.018) 18px 19px, transparent 19px 42px);
}

.competition-ledger > * {
  position: relative;
  z-index: 1;
}

.page-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 22px;
  align-items: center;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
}

p,
small,
.ledger-row,
.drawer-hero p,
.drawer-meta,
.drawer-heading span {
  color: var(--muted);
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  font-size: 28px;
  line-height: 1.1;
}

button,
input,
select {
  font: inherit;
}

button {
  cursor: pointer;
}

svg {
  width: 1em;
  height: 1em;
}

.focus-brief,
.head-actions,
.tool-button,
.filter-bar,
.search-field,
.status-tabs,
.ledger-row,
.event-cell,
.row-action,
.drawer-meta,
.drawer-heading,
.drawer-alerts p,
.drawer-footer {
  display: flex;
  align-items: center;
}

.focus-brief {
  min-width: 0;
  gap: 10px;
  color: var(--muted);
  text-align: left;
  border: 0;
  background: transparent;
}

.focus-brief span {
  color: var(--gold-soft);
  font-weight: 800;
}

.focus-brief strong,
.focus-brief small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.focus-brief strong {
  color: var(--text);
}

.focus-brief em {
  flex: 0 0 auto;
  color: var(--green);
  font-style: normal;
  font-weight: 800;
}

.head-actions {
  justify-content: flex-end;
  gap: 10px;
}

.tool-button {
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 14px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.tool-button:hover {
  transform: translateY(-1px);
  border-color: rgba(224, 184, 74, 0.36);
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.filter-bar {
  flex: 0 0 auto;
  justify-content: space-between;
  gap: 14px;
  margin-top: 22px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  backdrop-filter: blur(14px);
}

.search-field {
  gap: 10px;
  width: min(340px, 100%);
  min-height: 42px;
  padding: 0 12px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.search-field input {
  width: 100%;
  min-width: 0;
  color: var(--text);
  border: 0;
  outline: 0;
  background: transparent;
}

.search-field input::placeholder {
  color: var(--faint);
}

.status-tabs {
  gap: 6px;
  flex-wrap: wrap;
}

.status-tabs button,
.filter-bar select {
  min-height: 38px;
  padding: 0 12px;
  color: #a9bbc2;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.status-tabs button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.ledger-panel {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: 18px;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.ledger-panel::-webkit-scrollbar {
  width: 10px;
}

.ledger-panel::-webkit-scrollbar-thumb {
  border: 2px solid rgba(14, 20, 24, 0.9);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.28);
}

.ledger-header,
.ledger-row {
  display: grid;
  grid-template-columns: minmax(250px, 1.5fr) 104px 112px 112px 112px 142px 112px minmax(150px, 0.9fr) 44px;
  gap: 14px;
  align-items: center;
}

.ledger-header {
  min-height: 42px;
  padding: 0 16px;
  color: var(--faint);
  font-size: 13px;
}

.ledger-row {
  position: relative;
  width: 100%;
  min-height: 82px;
  margin-bottom: 10px;
  padding: 12px 16px;
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(14px);
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.ledger-row:hover {
  transform: translateY(-1px);
  border-color: rgba(224, 184, 74, 0.32);
  background: var(--panel-strong);
}

.event-cell {
  align-items: flex-start;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.event-cell strong {
  color: var(--text);
  font-size: 17px;
}

.event-cell strong,
.event-cell small {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-badge {
  display: inline-flex;
  justify-content: center;
  width: fit-content;
  padding: 7px 10px;
  color: var(--green);
  font-weight: 800;
  border: 1px solid rgba(111, 207, 122, 0.2);
  border-radius: 8px;
  background: rgba(111, 207, 122, 0.1);
  white-space: nowrap;
}

.state-badge.gold,
.state-badge.warning {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.08);
}

.state-badge.neutral {
  color: #a9bbc2;
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.ledger-row b {
  display: block;
  color: var(--text);
}

.row-action {
  gap: 8px;
  color: var(--gold-soft);
  font-weight: 800;
}

.quick-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 300px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

:global(.competition-drawer) {
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --orange: #f2994a;
  --red: #e05252;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    #10191d;
  background-size: 42px 42px;
}

:global(.competition-drawer .el-drawer__body) {
  padding: 0;
}

:global(.dark-drawer-mask) {
  background: rgba(3, 7, 9, 0.55);
}

.drawer-content {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  color: var(--text);
}

.drawer-hero {
  position: relative;
  padding: 28px 24px 22px;
  border-bottom: 1px solid var(--line);
  background:
    radial-gradient(circle at 88% 12%, rgba(216, 169, 53, 0.16), transparent 8rem),
    rgba(255, 255, 255, 0.025);
}

.drawer-close {
  position: absolute;
  top: 18px;
  right: 18px;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.drawer-hero h2 {
  margin: 8px 44px 14px 0;
  font-size: 26px;
  line-height: 1.18;
}

.drawer-meta {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.drawer-meta span {
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.drawer-description {
  margin-top: 14px;
  color: var(--muted);
  line-height: 1.6;
}

.drawer-block {
  padding: 22px 24px 0;
}

.drawer-heading {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.drawer-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.drawer-stats div,
.drawer-alerts p {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.drawer-stats div {
  padding: 12px;
}

.drawer-stats strong {
  display: block;
  margin-top: 6px;
  color: var(--text);
  font-size: 22px;
}

.drawer-alerts {
  display: grid;
  gap: 8px;
}

.drawer-alerts p {
  gap: 8px;
  padding: 11px 12px;
}

.drawer-alerts p.success {
  color: var(--green);
}

.drawer-alerts p.warning {
  color: #f1bd79;
  background: rgba(242, 153, 74, 0.09);
}

.drawer-alerts p.danger {
  color: #ff9089;
  background: rgba(224, 82, 82, 0.09);
}

.drawer-footer {
  justify-content: flex-end;
  gap: 10px;
  margin-top: auto;
  padding: 22px 24px;
}

@media (max-width: 1320px) {
  .ledger-header,
  .ledger-row {
    grid-template-columns: minmax(260px, 1fr) 104px 100px 100px 126px 104px 44px;
  }

  .ledger-header span:nth-child(5),
  .ledger-header span:nth-child(8),
  .ledger-row > span:nth-child(5),
  .ledger-row > .row-action {
    display: none;
  }
}

@media (max-width: 980px) {
  .competition-ledger {
    height: auto;
    min-height: 100vh;
    padding: 18px;
    overflow: visible;
  }

  .page-head,
  .filter-bar {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .focus-brief {
    flex-wrap: wrap;
  }

  .ledger-panel {
    overflow: visible;
  }

  .ledger-header {
    display: none;
  }

  .ledger-row {
    grid-template-columns: 1fr;
  }

  .quick-button {
    position: absolute;
    top: 14px;
    right: 14px;
  }
}
</style>
