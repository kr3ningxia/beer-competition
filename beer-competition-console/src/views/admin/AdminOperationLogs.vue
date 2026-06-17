<template>
  <div class="operation-logs-page">
    <section class="page-head">
      <div>
        <span>后台审计</span>
        <h1>操作日志</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="loadLogs">
          <Refresh />
          刷新
        </button>
      </div>
    </section>

    <section class="filter-panel">
      <label class="field">
        <span>开始时间</span>
        <input v-model="filters.startTime" type="datetime-local" />
      </label>
      <label class="field">
        <span>结束时间</span>
        <input v-model="filters.endTime" type="datetime-local" />
      </label>
      <label class="field">
        <span>操作者</span>
        <select v-model="filters.adminUserId">
          <option value="">全部操作者</option>
          <option v-for="item in adminOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label class="field">
        <span>业务对象</span>
        <select v-model="filters.targetType">
          <option value="">全部对象</option>
          <option v-for="item in targetTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label class="field">
        <span>操作类型</span>
        <select v-model="filters.actionGroup">
          <option value="">全部类型</option>
          <option v-for="item in actionGroupOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
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
        <button class="tool-button" type="button" @click="resetFilters">重置筛选</button>
        <button class="tool-button primary" type="button" @click="applyFilters">查询</button>
      </div>
    </section>

    <section class="table-card">
      <div class="table-headline">
        <div>
          <h2>日志明细</h2>
          <span>{{ logs.length }} / {{ total }} 条</span>
        </div>
        <strong v-if="loading">加载中</strong>
        <strong v-else-if="hasActiveFilters">已筛选</strong>
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
          <div v-for="item in logs" :key="item.id" class="table-row">
            <div class="time-cell">{{ formatTime(item.createTime) }}</div>
            <div class="actor-cell">
              <strong>{{ displayAdminName(item) }}</strong>
              <small>{{ item.adminUsername || item.adminUserId || '-' }}</small>
            </div>
            <div class="action-cell">
              <span class="action-pill">{{ item.actionLabel || item.action }}</span>
              <small>{{ item.actionGroup || '-' }}</small>
            </div>
            <div class="target-cell">
              <strong>{{ item.targetLabel || item.targetType || '-' }}</strong>
              <small>{{ item.targetName || item.targetPublicId || '-' }}</small>
            </div>
            <div class="summary-cell">
              <strong>{{ item.summaryText || '-' }}</strong>
              <small v-if="item.detailText">{{ item.detailText }}</small>
            </div>
            <span :class="['risk-badge', item.riskLevel || 'normal']">{{ riskLabel(item.riskLevel) }}</span>
            <div class="row-actions">
              <button class="row-action" type="button" @click="openDetail(item)">查看详情</button>
              <button v-if="item.targetRoute" class="row-action" type="button" @click="openTarget(item)">打开对象</button>
            </div>
          </div>
          <div v-if="!loading && logs.length === 0" class="empty-state">
            <h2>没有符合条件的操作记录</h2>
            <p>调整筛选条件后再查看</p>
          </div>
        </div>
      </div>

      <footer class="pagination-bar">
        <span>共 {{ total }} 条</span>
        <div class="pager-buttons">
          <button type="button" :disabled="pagination.page <= 1" @click="changePage(pagination.page - 1)">上一页</button>
          <button
            v-for="page in visiblePages"
            :key="page"
            :class="{ active: pagination.page === page }"
            type="button"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          <button type="button" :disabled="pagination.page >= totalPages" @click="changePage(pagination.page + 1)">下一页</button>
        </div>
        <label>
          <span>每页</span>
          <select v-model.number="pagination.pageSize" @change="changePageSize">
            <option :value="30">30</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
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
          <div><span>原始动作</span><strong>{{ detailItem?.action || '-' }}</strong></div>
          <div><span>对象类型</span><strong>{{ detailItem?.targetLabel || detailItem?.targetType || '-' }}</strong></div>
          <div><span>对象编号</span><strong>{{ detailItem?.targetPublicId || '-' }}</strong></div>
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
          <button v-if="detailItem?.targetRoute" class="tool-button primary" type="button" @click="openTarget(detailItem)">打开对象</button>
        </footer>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import { fetchAdminOperationLogs, fetchAdminUsers } from '@/api/admin'

const router = useRouter()
const logs = ref([])
const adminUsers = ref([])
const total = ref(0)
const loading = ref(false)
const detailOpen = ref(false)
const detailItem = ref(null)

const filters = reactive({
  startTime: '',
  endTime: '',
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

const totalPages = computed(() => Math.max(Math.ceil(total.value / pagination.pageSize), 1))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, pagination.page - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})
const hasActiveFilters = computed(() => Boolean(filters.startTime || filters.endTime || filters.adminUserId || filters.targetType || filters.actionGroup || filters.keyword))

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
      startTime: normalizeInputTime(filters.startTime),
      endTime: normalizeInputTime(filters.endTime),
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
  filters.startTime = ''
  filters.endTime = ''
  filters.adminUserId = ''
  filters.targetType = ''
  filters.actionGroup = ''
  filters.keyword = ''
  applyFilters()
}

function changePage(page) {
  const next = Math.min(Math.max(page, 1), totalPages.value)
  if (next === pagination.page) return
  pagination.page = next
  loadLogs()
}

function changePageSize() {
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

function formatTime(value) {
  if (!value) return '-'
  const text = String(value)
  return text.includes('T') ? text.replace('T', ' ').slice(0, 19) : text.slice(0, 19)
}

function normalizeInputTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ')
}
</script>

<style scoped>
.operation-logs-page {
  display: grid;
  gap: 16px;
}

.page-head,
.filter-panel,
.table-card,
.detail-drawer {
  border: 1px solid rgba(218, 231, 236, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.84);
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  color: #e6edf0;
}

.page-head h1,
.page-head span,
.table-headline h2,
.table-headline span,
.table-headline strong,
.drawer-grid span,
.drawer-block span {
  margin: 0;
}

.page-head h1 {
  font-size: 32px;
}

.filter-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  padding: 18px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span,
.drawer-grid span,
.drawer-block span {
  color: #9fb1b9;
  font-size: 12px;
  font-weight: 700;
}

.field input,
.field select {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  color: #dce9ed;
  border: 1px solid rgba(218, 231, 236, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.search-field > div {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid rgba(218, 231, 236, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.search-field svg {
  width: 16px;
  height: 16px;
  color: #9fb1b9;
}

.search-field input {
  border: 0;
  background: transparent;
}

.filter-actions {
  display: flex;
  gap: 10px;
  align-items: end;
}

.head-actions,
.tool-button,
.row-action,
.pager-buttons button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(218, 231, 236, 0.16);
  border-radius: 8px;
  color: #dce9ed;
  background: rgba(255, 255, 255, 0.04);
  font-weight: 700;
}

.tool-button {
  min-height: 42px;
  padding: 0 14px;
}

.tool-button.primary {
  color: #1b1408;
  border-color: rgba(216, 169, 53, 0.38);
  background: #d8a935;
}

.tool-button svg {
  width: 16px;
  height: 16px;
}

.row-action {
  min-height: 32px;
  padding: 0 10px;
  color: #f3cf7b;
}

.pager-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pager-buttons button {
  min-width: 36px;
  min-height: 32px;
  padding: 0 10px;
}

.pager-buttons button.active {
  color: #1b1408;
  background: #d8a935;
}

.pagination-bar select {
  min-height: 32px;
  color: #dce9ed;
  border: 1px solid rgba(218, 231, 236, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.table-card {
  padding: 18px;
}

.table-headline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.log-table {
  display: grid;
  gap: 10px;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 140px 140px 160px 180px minmax(220px, 1fr) 96px 160px;
  gap: 12px;
  align-items: center;
}

.table-head {
  color: #738791;
  font-size: 12px;
}

.table-row {
  min-height: 58px;
  padding: 12px;
  border: 1px solid rgba(218, 231, 236, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.time-cell,
.actor-cell,
.action-cell,
.target-cell,
.summary-cell {
  min-width: 0;
}

.actor-cell,
.action-cell,
.target-cell,
.summary-cell {
  display: grid;
  gap: 4px;
}

.time-cell,
.actor-cell strong,
.action-cell span,
.target-cell strong,
.summary-cell strong {
  color: #dce9ed;
}

.actor-cell small,
.action-cell small,
.target-cell small,
.summary-cell small {
  color: #9fb1b9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.14);
  color: #f3cf7b;
}

.risk-badge {
  justify-self: start;
  min-width: 64px;
  padding: 6px 10px;
  border-radius: 999px;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
}

.risk-badge.critical {
  color: #ff8d8d;
  background: rgba(255, 98, 98, 0.12);
}

.risk-badge.warning {
  color: #f5b56e;
  background: rgba(245, 181, 110, 0.12);
}

.risk-badge.normal {
  color: #87d68d;
  background: rgba(92, 189, 102, 0.12);
}

.row-actions,
.drawer-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 28px 0 10px;
  text-align: center;
  color: #9fb1b9;
}

.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  color: #9fb1b9;
}

.drawer-mask {
  position: fixed;
  inset: 0;
  display: grid;
  justify-items: end;
  background: rgba(6, 10, 13, 0.5);
}

.detail-drawer {
  width: min(680px, 92vw);
  height: 100vh;
  padding: 20px;
  overflow: auto;
}

.detail-drawer header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.detail-drawer h2 {
  margin: 0;
  color: #e6edf0;
}

.drawer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.drawer-grid div,
.drawer-block {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid rgba(218, 231, 236, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.drawer-grid strong,
.drawer-block strong,
.drawer-block pre {
  margin: 0;
  color: #dce9ed;
  white-space: pre-wrap;
  word-break: break-word;
}

.drawer-block + .drawer-block {
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .filter-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .table-head,
  .table-row {
    grid-template-columns: 120px 120px 140px 140px minmax(180px, 1fr) 84px 140px;
  }
}

@media (max-width: 820px) {
  .filter-panel,
  .drawer-grid {
    grid-template-columns: 1fr;
  }

  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .pagination-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
