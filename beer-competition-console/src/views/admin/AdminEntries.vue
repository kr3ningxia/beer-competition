<template>
  <div class="admin-entries-page">
    <section class="page-head">
      <div>
        <span>酒款运营</span>
        <h1>酒款管理</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="resetFilters">
          <RefreshLeft />
          重置筛选
        </button>
        <button class="tool-button primary" type="button" @click="loadEntries">
          <Refresh />
          刷新
        </button>
      </div>
    </section>

    <section class="filter-panel">
      <label class="field search-field">
        <span>关键词</span>
        <div>
          <Search />
          <input v-model.trim="filters.keyword" placeholder="搜索酒款、厂牌、短编号" @keyup.enter="applyFilters" />
        </div>
      </label>
      <label class="field">
        <span>比赛</span>
        <select v-model="filters.competitionId" @change="onCompetitionChange">
          <option value="">全部比赛</option>
          <option v-for="item in competitions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>投递组别</span>
        <select v-model="filters.categoryId" :disabled="!categoryOptions.length" @change="applyFilters">
          <option value="">全部组别</option>
          <option v-for="item in categoryOptions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>报名状态</span>
        <select v-model="filters.status" @change="applyFilters">
          <option value="">全部状态</option>
          <option v-for="item in entryStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label class="field">
        <span>付款</span>
        <select v-model="filters.paymentStatus" @change="applyFilters">
          <option value="">全部付款</option>
          <option value="UNPAID">待付款</option>
          <option value="PAID">已付款</option>
          <option value="REFUNDED">已退款</option>
          <option value="CANCELED">已取消</option>
        </select>
      </label>
      <label class="field">
        <span>入库</span>
        <select v-model="filters.deliveryStatus" @change="applyFilters">
          <option value="">全部入库</option>
          <option value="NOT_SUBMITTED">未提交</option>
          <option value="SUBMITTED">已提交</option>
          <option value="RECEIVED">已入库</option>
        </select>
      </label>
      <label class="field">
        <span>分桌</span>
        <select v-model="filters.assigned" @change="applyFilters">
          <option value="">全部</option>
          <option value="true">已分桌</option>
          <option value="false">未分桌</option>
        </select>
      </label>
    </section>

    <section class="metric-strip">
      <article v-for="item in metrics" :key="item.label">
        <small>{{ item.label }}</small>
        <strong>{{ item.value }}</strong>
        <span>{{ item.hint }}</span>
      </article>
    </section>

    <section class="table-card">
      <div class="table-title">
        <h2>酒款明细</h2>
        <span>{{ entries.length }} / {{ total }} 款</span>
      </div>
      <div class="entries-table">
        <div class="table-head">
          <span>酒款</span>
          <span>比赛</span>
          <span>编号</span>
          <span>组别 / 风格</span>
          <span>付款</span>
          <span>入库</span>
          <span>分桌轨迹</span>
          <span>最近修改</span>
          <span>操作</span>
        </div>
        <div class="table-body">
          <div v-for="entry in entries" :key="entry.id" class="table-row">
            <div class="entry-cell">
              <strong>{{ entry.name || '未命名酒款' }}</strong>
              <small>{{ entry.breweryCompanyName || '未关联厂牌' }}</small>
            </div>
            <div class="soft-cell">
              <strong>{{ entry.competitionName || '-' }}</strong>
              <small>{{ entry.competitionCode || '-' }}</small>
            </div>
            <div class="code-cell">
              <strong>{{ entry.shortCode || '-' }}</strong>
              <small>{{ entry.uuid }}</small>
            </div>
            <div class="soft-cell">
              <strong>{{ entry.categoryName || '-' }}</strong>
              <small>{{ entry.style || '-' }}</small>
            </div>
            <span :class="['state-pill', paymentTone(entry.paymentStatus)]">{{ paymentLabel(entry.paymentStatus) }}</span>
            <span :class="['state-pill', deliveryTone(entry.deliveryStatus)]">{{ deliveryLabel(entry.deliveryStatus) }}</span>
            <span class="path-cell">{{ entry.pathText || '未分桌' }}</span>
            <span class="time-cell">{{ formatTime(entry.lastModifiedAt || entry.submittedAt) }}</span>
            <div class="row-actions">
              <button type="button" @click="openDetail(entry.id, 'profile')">查看</button>
              <button type="button" :disabled="!entry.canEdit" @click="openDetail(entry.id, 'profile', true)">编辑资料</button>
              <button type="button" @click="openDetail(entry.id, 'status')">状态处理</button>
            </div>
          </div>
          <div v-if="!loading && !entries.length" class="empty-state">
            <h2>没有符合条件的酒款</h2>
            <p>调整筛选条件后再查看。</p>
          </div>
        </div>
      </div>
      <footer class="pagination-bar">
        <span>共 {{ total }} 条</span>
        <div class="pager-buttons">
          <button type="button" :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">上一页</button>
          <button
            v-for="page in visiblePages"
            :key="page"
            :class="{ active: filters.page === page }"
            type="button"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          <button type="button" :disabled="filters.page >= totalPages" @click="changePage(filters.page + 1)">下一页</button>
        </div>
        <label>
          <span>每页</span>
          <select v-model.number="filters.pageSize" @change="changePageSize">
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
      </footer>
    </section>

    <div v-if="detailOpen" class="drawer-mask" @click.self="closeDetail">
      <aside class="entry-drawer">
        <header>
          <div>
            <span>{{ detail?.competitionName || '酒款详情' }}</span>
            <h2>{{ detail?.name || '未命名酒款' }}</h2>
            <small>{{ detail?.breweryCompanyName || '未关联厂牌' }} · {{ detail?.shortCode || detail?.uuid }}</small>
          </div>
          <button type="button" @click="closeDetail">关闭</button>
        </header>

        <nav class="drawer-tabs">
          <button v-for="tab in drawerTabs" :key="tab.key" :class="{ active: activeTab === tab.key }" type="button" @click="activeTab = tab.key">
            {{ tab.label }}
          </button>
        </nav>

        <section v-if="detailLoading" class="drawer-loading">加载中</section>

        <template v-else-if="detail">
          <section v-if="activeTab === 'profile'" class="drawer-panel profile-panel">
            <div class="risk-banner" v-if="detail.assigned || detail.resultPublished">
              <strong>{{ detail.resultPublished ? '结果已发布' : '酒款已分桌' }}</strong>
              <span>{{ detail.resultPublished ? '当前报名信息只读。' : '修改组别或评审可见信息后，需要核对分桌和现场展示。' }}</span>
            </div>
            <div class="form-grid">
              <label>
                <span>酒名</span>
                <input v-model.trim="editForm.name" :disabled="!detail.canEdit" />
              </label>
              <label>
                <span>投递组别</span>
                <select v-model="editForm.categoryId" :disabled="!detail.canEdit">
                  <option v-for="item in detailCategoryOptions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
                </select>
              </label>
              <label>
                <span>基础风格</span>
                <input v-model.trim="editForm.style" :disabled="!detail.canEdit" />
              </label>
              <label>
                <span>ABV</span>
                <input v-model.trim="editForm.abv" :disabled="!detail.canEdit" inputmode="decimal" />
              </label>
            </div>
            <section class="extra-fields">
              <label v-for="field in editForm.extraFields" :key="field.key">
                <span>{{ field.label }}</span>
                <input v-model.trim="field.value" :disabled="!detail.canEdit" />
              </label>
            </section>
            <label class="stack-field">
              <span>修改原因</span>
              <textarea v-model.trim="editForm.reason" :disabled="!detail.canEdit" placeholder="填写后才能保存资料修改"></textarea>
            </label>
            <footer>
              <button class="tool-button primary" type="button" :disabled="!detail.canEdit || saving" @click="saveProfile">保存报名信息</button>
            </footer>
          </section>

          <section v-if="activeTab === 'status'" class="drawer-panel status-panel">
            <div class="status-grid">
              <article>
                <small>报名状态</small>
                <strong>{{ entryStatusLabel(detail.status) }}</strong>
              </article>
              <article>
                <small>付款</small>
                <strong>{{ paymentLabel(detail.paymentStatus) }}</strong>
              </article>
              <article>
                <small>入库</small>
                <strong>{{ deliveryLabel(detail.deliveryStatus) }}</strong>
              </article>
            </div>
            <label class="stack-field">
              <span>处理原因</span>
              <textarea v-model.trim="statusReason" placeholder="可填写现场处理说明"></textarea>
            </label>
            <div class="status-actions">
              <button type="button" :disabled="!detail.canConfirmPayment" @click="runDetailStatusAction('payment')">确认付款</button>
              <button type="button" :disabled="!detail.canMarkStored" @click="runDetailStatusAction('stored')">确认入库</button>
              <button class="danger" type="button" :disabled="!detail.canCancel" @click="runDetailStatusAction('cancel')">取消报名</button>
            </div>
          </section>

          <section v-if="activeTab === 'label'" class="drawer-panel label-panel">
            <article><small>匿名编码</small><strong>{{ detail.uuid }}</strong></article>
            <article><small>短编号</small><strong>{{ detail.shortCode || '-' }}</strong></article>
            <article><small>标签编码</small><strong>{{ detail.labelCode || '-' }}</strong></article>
            <article><small>扫码令牌</small><strong>{{ detail.scanToken || '-' }}</strong></article>
          </section>

          <section v-if="activeTab === 'trace'" class="drawer-panel trace-panel">
            <div v-for="trace in detail.traces" :key="`${trace.type}-${trace.roundId}-${trace.roundTableId}-${trace.awardName}`" class="trace-row">
              <strong>{{ traceTitle(trace) }}</strong>
              <span>{{ traceText(trace) }}</span>
            </div>
            <p v-if="!detail.traces?.length" class="empty-line">还没有分桌或结果轨迹。</p>
          </section>

          <section v-if="activeTab === 'logs'" class="drawer-panel log-panel">
            <div v-for="log in detail.logs" :key="log.id" class="log-row">
              <strong>{{ actionLabel(log.action) }}</strong>
              <small>{{ formatTime(log.createTime) }} · 管理员 {{ log.adminUserId || '-' }}</small>
              <p>{{ formatLogSummary(log.summary) }}</p>
            </div>
            <p v-if="!detail.logs?.length" class="empty-line">暂无修改记录。</p>
          </section>
        </template>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  cancelEntry,
  confirmEntryPayment,
  fetchAdminEntries,
  fetchAdminEntryDetail,
  fetchCompetitionDetail,
  fetchCompetitions,
  markEntryStored,
  updateAdminEntry,
} from '@/api/admin'

const route = useRoute()
const router = useRouter()
const entries = ref([])
const total = ref(0)
const competitions = ref([])
const categoryOptions = ref([])
const detailCategoryOptions = ref([])
const loading = ref(false)
const detailLoading = ref(false)
const detailOpen = ref(false)
const detail = ref(null)
const activeTab = ref('profile')
const saving = ref(false)
const statusReason = ref('')

const filters = reactive({
  competitionId: '',
  status: '',
  paymentStatus: '',
  deliveryStatus: '',
  categoryId: '',
  assigned: '',
  keyword: '',
  page: 1,
  pageSize: 20,
})

const editForm = reactive({
  name: '',
  categoryId: '',
  style: '',
  abv: '',
  reason: '',
  extraFields: [],
})

const entryStatusOptions = [
  { value: 'PENDING_PAYMENT', label: '待付款' },
  { value: 'REGISTERED', label: '报名成功' },
  { value: 'STORED', label: '已入库' },
  { value: 'CANCELED', label: '已取消' },
  { value: 'RESULT_PUBLISHED', label: '结果已出' },
]

const drawerTabs = [
  { key: 'profile', label: '报名信息' },
  { key: 'status', label: '状态处理' },
  { key: 'label', label: '二维码' },
  { key: 'trace', label: '分桌轨迹' },
  { key: 'logs', label: '修改记录' },
]

const metrics = computed(() => [
  { label: '全部', value: total.value, hint: '当前筛选范围' },
  { label: '待付款', value: entries.value.filter((item) => item.paymentStatus === 'UNPAID').length, hint: '本页需跟进' },
  { label: '待入库', value: entries.value.filter((item) => item.status === 'REGISTERED').length, hint: '可确认到场' },
  { label: '资料待核对', value: entries.value.filter((item) => item.assigned && item.canEdit).length, hint: '已分桌仍可修正' },
  { label: '已分桌', value: entries.value.filter((item) => item.assigned).length, hint: '本页轨迹' },
  { label: '已取消', value: entries.value.filter((item) => item.status === 'CANCELED').length, hint: '不进分桌池' },
])

const totalPages = computed(() => Math.max(Math.ceil(total.value / filters.pageSize), 1))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, filters.page - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})

watch(() => route.query.entryId, (entryId) => {
  if (entryId) openDetail(Number(entryId), 'profile')
})

onMounted(async () => {
  await loadCompetitions()
  applyQuery()
  if (filters.competitionId) await loadCategoryOptions(filters.competitionId)
  await loadEntries()
  if (route.query.entryId) await openDetail(Number(route.query.entryId), 'profile')
})

async function loadCompetitions() {
  competitions.value = await fetchCompetitions()
}

function applyQuery() {
  if (route.query.competitionId) filters.competitionId = String(route.query.competitionId)
  if (route.query.keyword) filters.keyword = String(route.query.keyword)
}

async function onCompetitionChange() {
  filters.categoryId = ''
  await loadCategoryOptions(filters.competitionId)
  applyFilters()
}

async function loadCategoryOptions(competitionId) {
  if (!competitionId) {
    categoryOptions.value = []
    return
  }
  const data = await fetchCompetitionDetail(competitionId)
  categoryOptions.value = data.categories || []
}

function applyFilters() {
  filters.page = 1
  loadEntries()
}

function changePage(page) {
  const next = Math.min(Math.max(page, 1), totalPages.value)
  if (next === filters.page) return
  filters.page = next
  loadEntries()
}

function changePageSize() {
  filters.page = 1
  loadEntries()
}

function resetFilters() {
  Object.assign(filters, { competitionId: '', status: '', paymentStatus: '', deliveryStatus: '', categoryId: '', assigned: '', keyword: '', page: 1, pageSize: 20 })
  categoryOptions.value = []
  router.replace('/admin/entries')
  loadEntries()
}

async function loadEntries() {
  loading.value = true
  try {
    const params = {
      page: filters.page,
      pageSize: filters.pageSize,
      competitionId: filters.competitionId || undefined,
      status: filters.status || undefined,
      paymentStatus: filters.paymentStatus || undefined,
      deliveryStatus: filters.deliveryStatus || undefined,
      categoryId: filters.categoryId || undefined,
      assigned: filters.assigned === '' ? undefined : filters.assigned === 'true',
      keyword: filters.keyword || undefined,
    }
    const data = await fetchAdminEntries(params)
    entries.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(entryId, tab = 'profile', edit = false) {
  detailOpen.value = true
  activeTab.value = tab
  detailLoading.value = true
  try {
    detail.value = await fetchAdminEntryDetail(entryId)
    await loadDetailCategories(detail.value.competitionId)
    syncEditForm()
    statusReason.value = ''
    if (edit) activeTab.value = 'profile'
    router.replace({ path: '/admin/entries', query: { ...route.query, competitionId: filters.competitionId || detail.value.competitionId, entryId } })
  } finally {
    detailLoading.value = false
  }
}

async function loadDetailCategories(competitionId) {
  const data = await fetchCompetitionDetail(competitionId)
  detailCategoryOptions.value = data.categories || []
}

function closeDetail() {
  detailOpen.value = false
  detail.value = null
  const query = { ...route.query }
  delete query.entryId
  router.replace({ path: '/admin/entries', query })
}

function syncEditForm() {
  Object.assign(editForm, {
    name: detail.value?.name || '',
    categoryId: detail.value?.categoryId ? String(detail.value.categoryId) : '',
    style: detail.value?.style || '',
    abv: detail.value?.abv == null ? '' : String(detail.value.abv),
    reason: '',
    extraFields: (detail.value?.extraFields || []).map((item) => ({ ...item })),
  })
}

async function saveProfile() {
  if (!editForm.reason.trim()) {
    ElMessage.warning('请填写修改原因')
    return
  }
  if (detail.value.assigned) {
    try {
      await ElMessageBox.confirm('这款酒已经分桌，保存后请核对分桌、晋级和奖项归属。', '确认修改？', {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  saving.value = true
  try {
    const extraFields = {}
    editForm.extraFields.forEach((field) => {
      extraFields[field.key] = field.value
    })
    detail.value = await updateAdminEntry(detail.value.id, {
      name: editForm.name,
      categoryId: Number(editForm.categoryId),
      style: editForm.style,
      abv: Number(editForm.abv),
      extraFields,
      reason: editForm.reason,
    })
    syncEditForm()
    await loadEntries()
    ElMessage.success('报名信息已保存')
  } finally {
    saving.value = false
  }
}

async function runStatusAction(entry, type) {
  await openDetail(entry.id, 'status')
  await runDetailStatusAction(type)
}

async function runDetailStatusAction(type) {
  const current = detail.value
  if (!current) return
  if (type === 'cancel') {
    try {
      await ElMessageBox.confirm(`确认取消「${current.name || current.uuid}」的报名吗？`, '取消报名', {
        confirmButtonText: '确认取消',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  const payload = { reason: statusReason.value }
  if (type === 'payment') await confirmEntryPayment(current.id, payload)
  if (type === 'stored') await markEntryStored(current.id, payload)
  if (type === 'cancel') await cancelEntry(current.id, payload)
  detail.value = await fetchAdminEntryDetail(current.id)
  syncEditForm()
  await loadEntries()
  ElMessage.success(statusActionLabel(type))
}

function statusActionLabel(type) {
  return type === 'payment' ? '付款已确认' : type === 'stored' ? '入库已确认' : '报名已取消'
}

function entryStatusLabel(value) {
  return entryStatusOptions.find((item) => item.value === value)?.label || value || '-'
}

function paymentLabel(value) {
  return { UNPAID: '待付款', PAID: '已付款', REFUNDED: '已退款', CANCELED: '已取消' }[value] || value || '-'
}

function deliveryLabel(value) {
  return { NOT_SUBMITTED: '未提交', SUBMITTED: '已提交', RECEIVED: '已入库' }[value] || value || '-'
}

function paymentTone(value) {
  return value === 'PAID' ? 'success' : value === 'UNPAID' ? 'warning' : 'muted'
}

function deliveryTone(value) {
  return value === 'RECEIVED' ? 'success' : value === 'SUBMITTED' ? 'warning' : 'muted'
}

function traceTitle(trace) {
  if (trace.type === 'AWARD') return trace.awardName || '奖项'
  if (trace.type === 'RESULT') return trace.slotLabel || '轮次结果'
  return [trace.roundName, trace.tableName].filter(Boolean).join(' · ') || '分桌记录'
}

function traceText(trace) {
  if (trace.type === 'AWARD') return [trace.awardType, trace.rankNo ? `第 ${trace.rankNo}` : ''].filter(Boolean).join(' · ')
  if (trace.type === 'RESULT') return [trace.roundName, trace.tableName, trace.resultType].filter(Boolean).join(' · ')
  return trace.status || '已分配'
}

function actionLabel(action) {
  return {
    ENTRY_UPDATE: '编辑报名信息',
    ENTRY_CONFIRM_PAYMENT: '确认付款',
    ENTRY_MARK_STORED: '确认入库',
    ENTRY_CANCEL: '取消报名',
  }[action] || action
}

function formatLogSummary(summary) {
  if (!summary) return ''
  try {
    const data = JSON.parse(summary)
    if (Array.isArray(data.changes)) {
      const changed = data.changes.map((item) => item.field).join('、')
      return `${data.reason || '未填写原因'}${changed ? ` · 修改：${changed}` : ''}`
    }
    return data.reason || data.action || summary
  } catch {
    return summary
  }
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.admin-entries-page {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 28px 28px 18px;
  overflow: hidden;
  color: #e9f2f5;
  background:
    linear-gradient(rgba(218, 232, 237, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(218, 232, 237, 0.035) 1px, transparent 1px),
    #0b1216;
  background-size: 68px 68px;
}

.page-head,
.filter-panel,
.metric-strip,
.table-card {
  width: 100%;
  max-width: none;
  margin: 0 auto;
}

.page-head {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(218, 232, 237, 0.1);
}

.page-head span {
  color: #d8ae3f;
  font-size: 13px;
  font-weight: 700;
}

.page-head h1 {
  margin: 5px 0 0;
  font-size: 30px;
  line-height: 1.12;
}

.head-actions,
.row-actions,
.status-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tool-button,
.row-actions button,
.status-actions button,
.entry-drawer header button {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 13px;
  color: #d5e2e7;
  font-weight: 700;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
}

.tool-button.primary,
.status-actions button:not(.danger) {
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.12);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.danger,
.row-actions button.danger,
.status-actions button.danger {
  color: #ffaaa0;
  border-color: rgba(255, 122, 107, 0.25);
  background: rgba(255, 122, 107, 0.08);
}

.filter-panel {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(260px, 1.4fr) repeat(6, minmax(130px, 1fr));
  gap: 12px;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid rgba(218, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(18, 29, 34, 0.86);
}

.field {
  display: grid;
  gap: 7px;
}

.field span {
  color: #8ea4ad;
  font-size: 12px;
  font-weight: 700;
}

.field input,
.field select,
.stack-field textarea,
.form-grid input,
.form-grid select,
.extra-fields input {
  width: 100%;
  min-height: 38px;
  padding: 0 10px;
  color: #eaf3f6;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 8px;
  background: #0d161a;
}

.search-field div {
  position: relative;
}

.search-field svg {
  position: absolute;
  left: 10px;
  top: 10px;
  width: 18px;
  color: #7f949d;
}

.search-field input {
  padding-left: 36px;
}

.metric-strip {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.metric-strip article,
.table-card,
.entry-drawer {
  border: 1px solid rgba(218, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(18, 29, 34, 0.9);
}

.metric-strip article {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 3px 10px;
  min-height: 72px;
  padding: 10px 12px;
}

.metric-strip small,
.metric-strip span {
  color: #8ea4ad;
}

.metric-strip strong {
  grid-row: 1 / span 2;
  grid-column: 2;
  font-size: 24px;
  line-height: 1;
}

.table-card {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  margin-top: 12px;
  padding: 16px;
  overflow: hidden;
}

.table-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-title h2 {
  margin: 0;
}

.entries-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-gutter: stable;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(190px, 1.25fr) minmax(190px, 1fr) minmax(140px, 0.8fr) minmax(170px, 1fr) 84px 84px minmax(180px, 1fr) 120px 230px;
  gap: 12px;
  align-items: center;
  min-width: 1320px;
}

.table-head {
  flex: 0 0 auto;
  padding: 0 12px 10px;
  color: #8ea4ad;
  font-size: 13px;
  font-weight: 700;
}

.table-row {
  min-height: 72px;
  padding: 10px 12px;
  border: 1px solid rgba(218, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.table-body {
  display: grid;
  flex: 1 1 auto;
  gap: 8px;
  min-width: 1320px;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.entries-table::-webkit-scrollbar,
.table-body::-webkit-scrollbar,
.entry-drawer::-webkit-scrollbar {
  width: 9px;
  height: 9px;
}

.entries-table::-webkit-scrollbar-track,
.table-body::-webkit-scrollbar-track,
.entry-drawer::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.025);
  border-radius: 999px;
}

.entries-table::-webkit-scrollbar-thumb,
.table-body::-webkit-scrollbar-thumb,
.entry-drawer::-webkit-scrollbar-thumb {
  background: rgba(216, 169, 53, 0.34);
  border-radius: 999px;
}

.entry-cell,
.soft-cell,
.code-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.entry-cell strong,
.soft-cell strong,
.code-cell strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-cell small,
.soft-cell small,
.code-cell small,
.time-cell,
.path-cell {
  overflow: hidden;
  color: #8ea4ad;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-pill {
  width: fit-content;
  min-width: 58px;
  padding: 5px 9px;
  text-align: center;
  font-size: 12px;
  font-weight: 800;
  border-radius: 7px;
}

.state-pill.success {
  color: #88e49f;
  background: rgba(83, 201, 121, 0.16);
}

.state-pill.warning {
  color: #ffdc73;
  background: rgba(216, 169, 53, 0.15);
}

.state-pill.muted {
  color: #b4c3c8;
  background: rgba(255, 255, 255, 0.06);
}

.row-actions {
  flex-wrap: nowrap;
  justify-content: flex-end;
}

.row-actions button {
  min-height: 30px;
  padding: 0 9px;
  font-size: 12px;
  white-space: nowrap;
}

.pagination-bar {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  min-height: 46px;
  margin-top: 12px;
  padding-top: 12px;
  color: #8ea4ad;
  border-top: 1px solid rgba(218, 232, 237, 0.1);
}

.pager-buttons {
  display: flex;
  gap: 6px;
  align-items: center;
}

.pagination-bar button,
.pagination-bar select {
  min-height: 32px;
  color: #e9f2f5;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.035);
}

.pagination-bar button {
  min-width: 34px;
  padding: 0 10px;
}

.pagination-bar button.active {
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.1);
}

.pagination-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.pagination-bar label {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pagination-bar select {
  padding: 0 9px;
}

.empty-state,
.drawer-loading {
  padding: 42px;
  text-align: center;
  color: #8ea4ad;
}

.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-end;
  background: rgba(3, 8, 10, 0.58);
}

.entry-drawer {
  width: min(760px, 100vw);
  height: 100vh;
  padding: 22px;
  overflow-y: auto;
  border-radius: 0;
  border-right: 0;
}

.entry-drawer header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.entry-drawer header span,
.entry-drawer header small {
  color: #8ea4ad;
}

.entry-drawer h2 {
  margin: 4px 0;
  font-size: 26px;
}

.drawer-tabs {
  display: flex;
  gap: 8px;
  margin: 18px 0;
  border-bottom: 1px solid rgba(218, 232, 237, 0.1);
}

.drawer-tabs button {
  min-height: 38px;
  padding: 0 12px;
  color: #9eb1b8;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  cursor: pointer;
}

.drawer-tabs button.active {
  color: #ffdc73;
  border-color: #d8a935;
}

.drawer-panel {
  display: grid;
  gap: 14px;
}

.risk-banner {
  display: grid;
  gap: 4px;
  padding: 12px;
  color: #ffdc73;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.risk-banner span {
  color: #d5c99a;
}

.form-grid,
.extra-fields,
.status-grid,
.label-panel {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.form-grid label,
.extra-fields label,
.stack-field {
  display: grid;
  gap: 7px;
}

.form-grid span,
.extra-fields span,
.stack-field span {
  color: #8ea4ad;
  font-size: 12px;
  font-weight: 700;
}

.stack-field textarea {
  min-height: 84px;
  padding: 10px;
  resize: vertical;
}

.status-grid article,
.label-panel article,
.trace-row,
.log-row {
  padding: 13px;
  border: 1px solid rgba(218, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.status-grid small,
.label-panel small,
.trace-row span,
.log-row small,
.log-row p {
  color: #8ea4ad;
}

.status-grid strong,
.label-panel strong,
.trace-row strong,
.log-row strong {
  display: block;
  margin-top: 4px;
}

.trace-panel,
.log-panel {
  gap: 8px;
}

.log-row p {
  margin: 8px 0 0;
}

@media (max-width: 1280px) {
  .filter-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .metric-strip {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
