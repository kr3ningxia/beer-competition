<template>
  <div class="operation-logs-page">
    <section class="page-head">
      <div>
        <span>后台审计</span>
        <h1>操作日志</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="resetFilters">
          <RefreshLeft />
          重置
        </button>
        <button class="tool-button primary" type="button" :disabled="loading" @click="loadLogs">
          <Refresh />
          刷新
        </button>
      </div>
    </section>

    <section class="filter-panel" aria-label="操作日志筛选">
      <label class="field time-field">
        <span>时间范围</span>
        <el-date-picker
          v-model="timeRange"
          class="audit-date-range"
          type="datetimerange"
          unlink-panels
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          popper-class="operation-log-date-popper"
          @change="applyFilters"
        />
      </label>

      <label class="field">
        <span>操作者</span>
        <el-select
          v-model="filters.adminUserId"
          class="audit-select"
          filterable
          clearable
          placeholder="全部操作者"
          popper-class="operation-log-select-popper"
          @change="applyFilters"
        >
          <el-option v-for="item in adminOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </label>

      <label class="field">
        <span>业务对象</span>
        <el-select
          v-model="filters.targetType"
          class="audit-select"
          clearable
          placeholder="全部对象"
          popper-class="operation-log-select-popper"
          @change="applyFilters"
        >
          <el-option v-for="item in targetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </label>

      <label class="field">
        <span>操作类型</span>
        <el-select
          v-model="filters.actionGroup"
          class="audit-select"
          clearable
          placeholder="全部类型"
          popper-class="operation-log-select-popper"
          @change="applyFilters"
        >
          <el-option v-for="item in actionGroupOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </label>

      <label class="field search-field">
        <span>关键词</span>
        <div>
          <Search />
          <input
            v-model.trim="filters.keyword"
            placeholder="搜索动作、对象编号、摘要、操作者"
            @keyup.enter="applyFilters"
          />
        </div>
      </label>

      <div class="filter-actions">
        <button class="tool-button subtle" type="button" @click="resetFilters">清空</button>
        <button class="tool-button primary" type="button" :disabled="loading" @click="applyFilters">查询</button>
      </div>
    </section>

    <section v-if="activeFilterTags.length" class="filter-tags" aria-label="当前筛选条件">
      <span v-for="tag in activeFilterTags" :key="tag.key">{{ tag.label }}</span>
    </section>

    <section class="table-card">
      <div class="table-title">
        <div>
          <h2>日志明细</h2>
          <span>当前 {{ logs.length }} 条，共 {{ total }} 条</span>
        </div>
        <div class="audit-summary">
          <strong v-if="loading">加载中</strong>
          <strong v-else>{{ riskSummaryText }}</strong>
        </div>
      </div>

      <div class="log-table">
        <div class="table-head">
          <span>时间</span>
          <span>操作者</span>
          <span>操作</span>
          <span>对象</span>
          <span>摘要</span>
          <span>风险</span>
          <span>操作</span>
        </div>

        <div class="table-body">
          <div v-for="item in logs" :key="item.id" :class="['table-row', riskTone(item.riskLevel)]">
            <div class="time-cell">
              <strong>{{ formatDate(item.createTime) }}</strong>
              <small>{{ formatClock(item.createTime) }}</small>
            </div>
            <div class="actor-cell">
              <strong>{{ displayAdminName(item) }}</strong>
              <small>{{ item.adminUsername || item.adminUserId || '-' }}</small>
            </div>
            <div class="action-cell">
              <span>{{ item.actionLabel || item.action }}</span>
              <small>{{ actionGroupLabel(item.actionGroup) }}</small>
            </div>
            <div class="target-cell">
              <strong>{{ item.targetLabel || item.targetType || '-' }}</strong>
              <small :title="item.targetName || item.targetPublicId || ''">{{ item.targetName || item.targetPublicId || '-' }}</small>
            </div>
            <div class="summary-cell">
              <strong :title="item.summaryText || ''">{{ item.summaryText || '-' }}</strong>
              <small v-if="item.detailText" :title="item.detailText">{{ item.detailText }}</small>
            </div>
            <span :class="['risk-badge', item.riskLevel || 'normal']">{{ riskLabel(item.riskLevel) }}</span>
            <div class="row-actions">
              <button class="row-action" type="button" @click="openDetail(item)">详情</button>
              <button v-if="item.targetRoute" class="row-action primary-action" type="button" @click="openTarget(item)">
                {{ targetActionLabel(item) }}
              </button>
            </div>
          </div>

          <div v-if="!loading && logs.length === 0" class="empty-state">
            <h2>{{ emptyTitle }}</h2>
            <p>{{ emptyDescription }}</p>
            <button v-if="hasActiveFilters" class="tool-button primary" type="button" @click="resetFilters">查看全部日志</button>
          </div>
        </div>
      </div>

      <footer class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          background
          :page-sizes="[20, 30, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="changePageSize"
          @current-change="changePage"
        />
      </footer>
    </section>

    <div v-if="detailOpen" class="drawer-mask" @click.self="closeDetail">
      <aside class="detail-drawer">
        <header>
          <div>
            <span>日志详情</span>
            <h2>{{ detailItem?.actionLabel || detailItem?.action || '操作记录' }}</h2>
            <small>{{ formatTime(detailItem?.createTime) }}</small>
          </div>
          <button type="button" @click="closeDetail">关闭</button>
        </header>

        <div class="drawer-grid">
          <div><span>操作者</span><strong>{{ displayAdminName(detailItem) }}</strong></div>
          <div><span>风险等级</span><strong>{{ riskLabel(detailItem?.riskLevel) }}</strong></div>
          <div><span>原始动作</span><strong>{{ detailItem?.action || '-' }}</strong></div>
          <div><span>业务对象</span><strong>{{ detailItem?.targetLabel || detailItem?.targetType || '-' }}</strong></div>
          <div class="wide"><span>对象编号</span><strong>{{ detailItem?.targetPublicId || '-' }}</strong></div>
        </div>

        <section class="drawer-block">
          <span>可读摘要</span>
          <strong>{{ detailItem?.summaryText || '-' }}</strong>
          <p v-if="detailItem?.detailText">{{ detailItem.detailText }}</p>
        </section>
        <section class="drawer-block">
          <span>原始摘要</span>
          <pre>{{ detailItem?.summary || '-' }}</pre>
        </section>
        <footer class="drawer-actions">
          <button v-if="detailItem?.targetRoute" class="tool-button primary" type="button" @click="openTarget(detailItem)">
            {{ targetActionLabel(detailItem) }}
          </button>
        </footer>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import { fetchAdminOperationLogs, fetchAdminUsers } from '@/api/admin'

const router = useRouter()
const logs = ref([])
const adminUsers = ref([])
const total = ref(0)
const loading = ref(false)
const detailOpen = ref(false)
const detailItem = ref(null)
const timeRange = ref([])

const filters = reactive({
  adminUserId: '',
  targetType: '',
  actionGroup: '',
  keyword: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 30,
})

const adminOptions = computed(() => adminUsers.value.map((item) => ({
  value: String(item.id),
  label: [item.username, normalizeAdminName(item.name)].filter(Boolean).join(' / '),
})))

const targetTypeOptions = [
  { value: 'BEER_ENTRY', label: '酒款' },
  { value: 'JUDGE', label: '评审' },
  { value: 'COMPETITION', label: '比赛' },
  { value: 'ADMIN_USER', label: '管理员' },
  { value: 'SCORE_RECORD', label: '评分记录' },
]

const actionGroupOptions = [
  { value: 'entry', label: '酒款' },
  { value: 'payment', label: '付款' },
  { value: 'refund', label: '退款' },
  { value: 'judge', label: '评审' },
  { value: 'competition', label: '比赛' },
  { value: 'export', label: '导出' },
  { value: 'account', label: '账号' },
  { value: 'score', label: '评分' },
]

const hasActiveFilters = computed(() => Boolean(
  timeRange.value?.length
  || filters.adminUserId
  || filters.targetType
  || filters.actionGroup
  || filters.keyword,
))

const activeFilterTags = computed(() => {
  const tags = []
  if (timeRange.value?.length === 2) tags.push({ key: 'time', label: `${timeRange.value[0]} 至 ${timeRange.value[1]}` })
  const admin = adminOptions.value.find((item) => item.value === filters.adminUserId)
  if (admin) tags.push({ key: 'admin', label: `操作者：${admin.label}` })
  const target = targetTypeOptions.find((item) => item.value === filters.targetType)
  if (target) tags.push({ key: 'target', label: `对象：${target.label}` })
  const action = actionGroupOptions.find((item) => item.value === filters.actionGroup)
  if (action) tags.push({ key: 'action', label: `类型：${action.label}` })
  if (filters.keyword) tags.push({ key: 'keyword', label: `关键词：${filters.keyword}` })
  return tags
})

const criticalCount = computed(() => logs.value.filter((item) => item.riskLevel === 'critical').length)
const warningCount = computed(() => logs.value.filter((item) => item.riskLevel === 'warning').length)
const riskSummaryText = computed(() => {
  if (!logs.value.length) return '当前页无记录'
  return `关键 ${criticalCount.value} · 关注 ${warningCount.value}`
})
const emptyTitle = computed(() => (hasActiveFilters.value ? '没有符合条件的操作记录' : '暂无操作日志'))
const emptyDescription = computed(() => (hasActiveFilters.value ? '调整时间、对象或关键词后再查询' : '关键业务操作发生后会显示在这里'))

onMounted(async () => {
  await Promise.all([loadAdminUsers(), loadLogs()])
})

async function loadAdminUsers() {
  adminUsers.value = await fetchAdminUsers() || []
}

async function loadLogs() {
  loading.value = true
  try {
    const data = await fetchAdminOperationLogs({
      startTime: timeRange.value?.[0] || undefined,
      endTime: timeRange.value?.[1] || undefined,
      adminUserId: filters.adminUserId || undefined,
      targetType: filters.targetType || undefined,
      actionGroup: filters.actionGroup || undefined,
      keyword: filters.keyword || undefined,
      page: pagination.page,
      pageSize: pagination.pageSize,
    })
    logs.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  pagination.page = 1
  loadLogs()
}

function resetFilters() {
  timeRange.value = []
  filters.adminUserId = ''
  filters.targetType = ''
  filters.actionGroup = ''
  filters.keyword = ''
  pagination.page = 1
  loadLogs()
}

function changePage(page) {
  pagination.page = page
  loadLogs()
}

function changePageSize(pageSize) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadLogs()
}

function openDetail(item) {
  detailItem.value = item
  detailOpen.value = true
}

function closeDetail() {
  detailOpen.value = false
  detailItem.value = null
}

function openTarget(item) {
  if (!item?.targetRoute) return
  router.push(item.targetRoute)
}

function displayAdminName(item) {
  if (!item) return '-'
  const adminName = normalizeAdminName(item.adminName)
  if (adminName) return adminName
  if (item.adminUsername) return item.adminUsername
  return item.adminUserId || '-'
}

function normalizeAdminName(value) {
  if (!value || /^\?+$/.test(String(value))) return ''
  return String(value)
}

function riskLabel(level) {
  const map = {
    critical: '关键',
    warning: '关注',
    normal: '普通',
  }
  return map[level] || '普通'
}

function riskTone(level) {
  if (level === 'critical') return 'is-critical'
  if (level === 'warning') return 'is-warning'
  return 'is-normal'
}

function actionGroupLabel(value) {
  return actionGroupOptions.find((item) => item.value === value)?.label || value || '-'
}

function targetActionLabel(item) {
  const map = {
    BEER_ENTRY: '打开酒款',
    JUDGE: '打开评审',
    COMPETITION: '打开比赛',
    ADMIN_USER: '打开账号',
    SCORE_RECORD: '查看评分',
  }
  return map[item?.targetType] || '打开对象'
}

function formatTime(value) {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  return text.slice(0, 19)
}

function formatDate(value) {
  const text = formatTime(value)
  if (text === '-') return '-'
  return text.slice(0, 10)
}

function formatClock(value) {
  const text = formatTime(value)
  if (text === '-') return '-'
  return text.slice(11, 19)
}
</script>

<style scoped>
.operation-logs-page {
  --panel: rgba(18, 29, 34, 0.91);
  --panel-strong: rgba(13, 22, 26, 0.95);
  --line: rgba(218, 232, 237, 0.1);
  --line-strong: rgba(218, 232, 237, 0.16);
  --text: #eaf3f6;
  --muted: #8ea4ad;
  --faint: #637984;
  --gold: #e0b84a;
  --gold-soft: rgba(216, 169, 53, 0.12);
  --danger: #ff9f98;
  --warning: #ffcf6b;
  --green: #83df95;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 28px 28px 18px;
  overflow: hidden;
  color: var(--text);
  background:
    linear-gradient(rgba(218, 232, 237, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(218, 232, 237, 0.035) 1px, transparent 1px),
    radial-gradient(circle at 18% 8%, rgba(216, 169, 53, 0.12), transparent 20rem),
    #0b1216;
  background-size: 68px 68px, 68px 68px, auto, auto;
}

h1,
h2,
p {
  margin: 0;
}

button,
input {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.page-head {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.page-head span {
  color: var(--gold);
  font-size: 13px;
  font-weight: 800;
}

.page-head h1 {
  margin-top: 5px;
  font-size: 30px;
  line-height: 1.12;
}

.head-actions,
.filter-actions,
.row-actions,
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tool-button,
.row-action,
.detail-drawer header button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: max-content;
  min-height: 38px;
  padding: 0 13px;
  color: #dce8ec;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font-weight: 800;
  white-space: nowrap;
}

.tool-button svg {
  width: 17px;
  height: 17px;
}

.tool-button.primary,
.row-action.primary-action {
  color: var(--gold);
  border-color: rgba(216, 169, 53, 0.34);
  background: var(--gold-soft);
}

.tool-button.subtle {
  color: #a8bac2;
}

.filter-panel {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(360px, 1.45fr) minmax(190px, 0.8fr) minmax(150px, 0.65fr) minmax(150px, 0.65fr) minmax(260px, 1fr) auto;
  gap: 12px;
  align-items: end;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.18);
}

.field {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.field > span,
.drawer-grid span,
.drawer-block span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.search-field > div {
  position: relative;
}

.search-field svg {
  position: absolute;
  left: 11px;
  top: 10px;
  width: 18px;
  height: 18px;
  color: #7f949d;
}

.search-field input {
  width: 100%;
  min-height: 40px;
  padding: 0 12px 0 38px;
  color: var(--text);
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  outline: none;
  background: #0d161a;
}

.search-field input::placeholder {
  color: var(--faint);
}

.filter-tags {
  flex: 0 0 auto;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.filter-tags span {
  max-width: min(420px, 100%);
  overflow: hidden;
  padding: 6px 10px;
  color: #cddbe0;
  border: 1px solid rgba(218, 232, 237, 0.11);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-card,
.detail-drawer {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.table-card {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  margin-top: 12px;
  padding: 16px;
  overflow: hidden;
}

.table-title {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  margin-bottom: 12px;
}

.table-title h2 {
  font-size: 18px;
}

.table-title span,
.audit-summary {
  color: var(--muted);
}

.audit-summary strong {
  color: var(--gold);
  font-size: 13px;
}

.log-table {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-gutter: stable;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 126px minmax(120px, 0.7fr) minmax(150px, 0.8fr) minmax(180px, 1fr) minmax(300px, 1.7fr) 74px 150px;
  gap: 12px;
  align-items: center;
  min-width: 1220px;
}

.table-head {
  flex: 0 0 auto;
  padding: 0 12px 10px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.table-body {
  flex: 1 1 auto;
  display: grid;
  align-content: start;
  gap: 8px;
  min-width: 1220px;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.log-table::-webkit-scrollbar,
.table-body::-webkit-scrollbar,
.detail-drawer::-webkit-scrollbar {
  width: 9px;
  height: 9px;
}

.log-table::-webkit-scrollbar-track,
.table-body::-webkit-scrollbar-track,
.detail-drawer::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.025);
}

.log-table::-webkit-scrollbar-thumb,
.table-body::-webkit-scrollbar-thumb,
.detail-drawer::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.32);
}

.table-row {
  min-height: 68px;
  padding: 10px 12px;
  border: 1px solid rgba(218, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  transition: border-color 0.16s ease, background 0.16s ease;
}

.table-row:hover {
  border-color: rgba(216, 169, 53, 0.2);
  background: rgba(255, 255, 255, 0.045);
}

.table-row.is-critical {
  border-color: rgba(255, 116, 108, 0.24);
  background: rgba(255, 116, 108, 0.045);
}

.table-row.is-warning {
  border-color: rgba(224, 184, 74, 0.2);
}

.time-cell,
.actor-cell,
.action-cell,
.target-cell,
.summary-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.time-cell strong,
.actor-cell strong,
.target-cell strong,
.summary-cell strong {
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-cell small,
.actor-cell small,
.action-cell small,
.target-cell small,
.summary-cell small {
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-cell span {
  overflow: hidden;
  width: fit-content;
  max-width: 100%;
  min-height: 26px;
  padding: 5px 9px;
  color: var(--gold);
  border-radius: 8px;
  background: var(--gold-soft);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-cell strong {
  line-height: 1.35;
}

.risk-badge {
  width: fit-content;
  min-width: 56px;
  padding: 6px 9px;
  border-radius: 8px;
  text-align: center;
  font-size: 12px;
  font-weight: 900;
}

.risk-badge.critical {
  color: var(--danger);
  background: rgba(255, 116, 108, 0.13);
}

.risk-badge.warning {
  color: var(--warning);
  background: rgba(224, 184, 74, 0.12);
}

.risk-badge.normal {
  color: var(--green);
  background: rgba(91, 191, 112, 0.11);
}

.row-actions {
  flex-wrap: nowrap;
}

.row-action {
  min-height: 32px;
  padding: 0 10px;
  color: #d5e2e7;
  font-size: 12px;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 240px;
  padding: 40px;
  color: var(--muted);
  text-align: center;
}

.empty-state h2 {
  color: var(--text);
  font-size: 20px;
}

.pagination-bar {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  min-height: 48px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-end;
  background: rgba(3, 8, 10, 0.58);
  backdrop-filter: blur(4px);
}

.detail-drawer {
  width: min(720px, 100vw);
  height: 100vh;
  padding: 22px;
  overflow-y: auto;
  border-radius: 0;
  border-right: 0;
}

.detail-drawer header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.detail-drawer header span,
.detail-drawer header small {
  color: var(--muted);
}

.detail-drawer h2 {
  margin: 4px 0;
  font-size: 26px;
}

.drawer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.drawer-grid div,
.drawer-block {
  display: grid;
  gap: 7px;
  padding: 13px;
  border: 1px solid rgba(218, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.drawer-grid .wide {
  grid-column: 1 / -1;
}

.drawer-grid strong,
.drawer-block strong,
.drawer-block p,
.drawer-block pre {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--text);
  line-height: 1.55;
  white-space: pre-wrap;
}

.drawer-block + .drawer-block {
  margin-top: 12px;
}

.drawer-actions {
  margin-top: 16px;
}

.audit-select,
.audit-date-range {
  width: 100%;
}

.audit-date-range:deep(.el-date-editor.el-input__wrapper),
.audit-date-range:deep(.el-range-editor.el-input__wrapper),
.audit-select :deep(.el-select__wrapper),
.audit-date-range :deep(.el-input__wrapper),
.audit-date-range :deep(.el-range-editor.el-input__wrapper) {
  min-height: 40px;
  border: 1px solid var(--line-strong);
  border-radius: 8px;
  background: #0d161a;
  box-shadow: none;
}

.audit-select :deep(.el-select__wrapper:hover),
.audit-select :deep(.el-select__wrapper.is-focused),
.audit-date-range :deep(.el-input__wrapper:hover),
.audit-date-range :deep(.el-range-editor.el-input__wrapper.is-active) {
  border-color: rgba(216, 169, 53, 0.36);
  box-shadow: none;
}

.audit-select :deep(.el-select__selected-item),
.audit-select :deep(.el-select__placeholder),
.audit-date-range :deep(.el-range-input),
.audit-date-range :deep(.el-range-separator) {
  color: var(--text);
}

.audit-select :deep(.el-select__placeholder.is-transparent),
.audit-date-range :deep(.el-range-input::placeholder) {
  color: var(--faint);
}

.audit-select :deep(.el-select__caret),
.audit-date-range :deep(.el-range__icon),
.audit-date-range :deep(.el-range__close-icon) {
  color: var(--muted);
}

.pagination-bar :deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 255, 255, 0.035);
  --el-pagination-button-color: #dce8ec;
  --el-pagination-button-disabled-bg-color: rgba(255, 255, 255, 0.02);
  --el-pagination-button-disabled-color: #617681;
  --el-pagination-hover-color: var(--gold);
  --el-color-primary: #d8a935;
  --el-text-color-regular: #dce8ec;
  --el-text-color-primary: #eaf3f6;
  --el-fill-color-blank: rgba(255, 255, 255, 0.035);
  --el-border-color: rgba(218, 232, 237, 0.14);
  --el-border-color-light: rgba(218, 232, 237, 0.1);
  --el-input-bg-color: rgba(255, 255, 255, 0.035);
  --el-input-text-color: #eaf3f6;
}

.pagination-bar :deep(.el-pager li),
.pagination-bar :deep(.btn-prev),
.pagination-bar :deep(.btn-next),
.pagination-bar :deep(.el-input__wrapper) {
  color: #dce8ec !important;
  border: 1px solid rgba(218, 232, 237, 0.14) !important;
  background: rgba(255, 255, 255, 0.035) !important;
  box-shadow: none !important;
}

.pagination-bar :deep(.el-pager li.is-active) {
  color: #11180c !important;
  border-color: rgba(216, 169, 53, 0.6) !important;
  background: #d8a935 !important;
}

.pagination-bar :deep(.el-pager li.is-disabled),
.pagination-bar :deep(.btn-prev.is-disabled),
.pagination-bar :deep(.btn-next.is-disabled) {
  color: #617681 !important;
  background: rgba(255, 255, 255, 0.025) !important;
}

.pagination-bar :deep(.el-input__inner) {
  color: #eaf3f6 !important;
}

@media (max-width: 1360px) {
  .filter-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .operation-logs-page {
    height: auto;
    min-height: 100%;
    padding: 22px 16px;
    overflow: visible;
  }

  .page-head,
  .table-title {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-panel,
  .drawer-grid {
    grid-template-columns: 1fr;
  }

  .table-card {
    overflow: visible;
  }

  .log-table,
  .table-body {
    overflow: visible;
  }

  .table-head {
    display: none;
  }

  .table-head,
  .table-row,
  .table-body {
    min-width: 0;
  }

  .table-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .pagination-bar {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>

<style>
.operation-logs-page .audit-date-range.el-date-editor.el-input__wrapper,
.operation-logs-page .audit-date-range.el-range-editor.el-input__wrapper {
  border: 1px solid rgba(218, 232, 237, 0.16) !important;
  border-radius: 8px !important;
  background: #0d161a !important;
  box-shadow: none !important;
}

.operation-logs-page .audit-date-range.el-date-editor.el-input__wrapper:hover,
.operation-logs-page .audit-date-range.el-range-editor.el-input__wrapper.is-active {
  border-color: rgba(216, 169, 53, 0.36) !important;
}

.operation-logs-page .audit-date-range .el-range-input,
.operation-logs-page .audit-date-range .el-range-separator {
  color: #eaf3f6 !important;
}

.operation-logs-page .audit-date-range .el-range-input::placeholder,
.operation-logs-page .audit-date-range .el-range__icon,
.operation-logs-page .audit-date-range .el-range__close-icon {
  color: #637984 !important;
}

.operation-log-select-popper,
.operation-log-date-popper {
  --el-bg-color-overlay: #111c20;
  --el-border-color-light: rgba(218, 232, 237, 0.14);
  --el-text-color-primary: #eaf3f6;
  --el-text-color-regular: #c7d6dc;
  --el-fill-color-light: rgba(216, 169, 53, 0.1);
  --el-color-primary: #d8a935;
  border: 1px solid rgba(218, 232, 237, 0.14) !important;
  background: #111c20 !important;
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.38) !important;
}

.operation-log-select-popper .el-select-dropdown,
.operation-log-select-popper .el-select-dropdown__list {
  background: #111c20;
}

.operation-log-select-popper .el-select-dropdown__item {
  color: #c7d6dc;
}

.operation-log-select-popper .el-select-dropdown__item.hover,
.operation-log-select-popper .el-select-dropdown__item.is-hovering,
.operation-log-select-popper .el-select-dropdown__item:hover {
  color: #ffdc73;
  background: rgba(216, 169, 53, 0.1);
}

.operation-log-select-popper .el-select-dropdown__item.selected,
.operation-log-select-popper .el-select-dropdown__item.is-selected {
  color: #ffdc73;
  font-weight: 800;
}

.operation-log-select-popper .el-select-dropdown__item.is-disabled {
  color: #5e737c;
}

.operation-log-date-popper .el-picker-panel,
.operation-log-date-popper .el-picker-panel__body,
.operation-log-date-popper .el-picker-panel__footer {
  color: #c7d6dc;
  border-color: rgba(218, 232, 237, 0.12);
  background: #111c20;
}

.operation-log-date-popper .el-date-table th,
.operation-log-date-popper .el-picker-panel__icon-btn,
.operation-log-date-popper .el-date-range-picker__time-header,
.operation-log-date-popper .el-date-range-picker__content.is-left {
  color: #8ea4ad;
  border-color: rgba(218, 232, 237, 0.12);
}

.operation-log-date-popper .el-date-table td.in-range .el-date-table-cell,
.operation-log-date-popper .el-date-table td.in-range .el-date-table-cell:hover {
  background: rgba(216, 169, 53, 0.1);
}

.operation-log-date-popper .el-input__wrapper {
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 0 0 1px rgba(218, 232, 237, 0.12) inset;
}

.operation-log-date-popper .el-input__inner {
  color: #eaf3f6;
}
</style>
