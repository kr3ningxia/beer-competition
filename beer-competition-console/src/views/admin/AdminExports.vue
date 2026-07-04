<template>
  <div class="admin-exports-page">
    <section class="page-head">
      <div>
        <span>运营文件</span>
        <h1>导出中心</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="resetFilters">
          <RefreshLeft />
          重置筛选
        </button>
        <button class="tool-button primary" type="button" :disabled="!selectedCompetitionId" @click="refreshExportCount">
          <Refresh />
          刷新数量
        </button>
      </div>
    </section>

    <section class="filter-panel">
      <label class="field competition-field">
        <span>比赛</span>
        <select v-model="selectedCompetitionId" @change="onCompetitionChange">
          <option value="">选择比赛</option>
          <option v-for="item in competitions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <div class="field archived-toggle">
        <span>历史数据</span>
        <label class="checkbox-line">
          <input v-model="includeArchived" type="checkbox" @change="reloadCompetitions" />
          包含已归档比赛
        </label>
      </div>
      <label class="field search-field">
        <span>关键词</span>
        <div>
          <Search />
          <input v-model.trim="filters.keyword" placeholder="搜索酒款、厂牌、短编号" @keyup.enter="refreshExportCount" />
        </div>
      </label>
      <label class="field">
        <span>投递组别</span>
        <select v-model="filters.categoryId" :disabled="!categoryOptions.length" @change="refreshExportCount">
          <option value="">全部组别</option>
          <option v-for="item in categoryOptions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>报名状态</span>
        <select v-model="filters.entryStatus" @change="refreshExportCount">
          <option value="">全部状态</option>
          <option v-for="item in entryStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label class="field">
        <span>支付</span>
        <select v-model="filters.paymentStatus" @change="refreshExportCount">
          <option value="">全部支付状态</option>
          <option value="UNPAID">待支付</option>
          <option value="PAID">已支付</option>
          <option value="REFUNDED">已退款</option>
          <option value="CANCELED">已取消</option>
        </select>
      </label>
      <label class="field">
        <span>入库</span>
        <select v-model="filters.deliveryStatus" @change="refreshExportCount">
          <option value="">全部入库</option>
          <option value="NOT_SUBMITTED">未提交</option>
          <option value="SUBMITTED">已提交</option>
          <option value="RECEIVED">已入库</option>
        </select>
      </label>
    </section>

    <section class="summary-strip">
      <article>
        <small>当前比赛</small>
        <strong>{{ selectedCompetition?.name || '未选择' }}</strong>
      </article>
      <article>
        <small>筛选范围</small>
        <strong>{{ exportCount }}</strong>
      </article>
      <article>
        <small>比赛状态</small>
        <strong>{{ statusLabel(selectedCompetition?.status) }}</strong>
      </article>
      <article>
        <small>瓶贴张数</small>
        <strong>{{ labelCopies }} / 款</strong>
      </article>
    </section>

    <section class="export-grid">
      <article v-for="template in exportTemplates" :key="template.key" class="export-card">
        <div class="card-head">
          <span :class="['template-icon', template.tone]">
            <component :is="template.icon" />
          </span>
          <div>
            <small>{{ template.stage }}</small>
            <h2>{{ template.title }}</h2>
          </div>
        </div>
        <p>{{ template.description }}</p>
        <dl class="template-facts">
          <div>
            <dt>文件</dt>
            <dd>{{ template.fileType }}</dd>
          </div>
          <div>
            <dt>范围</dt>
            <dd>{{ template.scope }}</dd>
          </div>
        </dl>
        <label v-if="template.key === 'labels'" class="copies-field">
          <span>每款标签张数</span>
          <input v-model.number="labelCopies" type="number" min="1" max="6" />
        </label>
        <button class="export-button" type="button" :disabled="!selectedCompetitionId || exportingKey === template.key" @click="runExport(template.key)">
          <Download />
          {{ exportingKey === template.key ? '正在导出' : template.action }}
        </button>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Box,
  DataAnalysis,
  Document,
  Download,
  Printer,
  Refresh,
  RefreshLeft,
  Search,
} from '@element-plus/icons-vue'
import {
  exportCompetitionDelivery,
  exportCompetitionEntries,
  exportCompetitionLabels,
  exportCompetitionScoringData,
  fetchAdminEntries,
  fetchCompetitionDetail,
  fetchCompetitions,
} from '@/api/admin'

const route = useRoute()
const competitions = ref([])
const categoryOptions = ref([])
const selectedCompetitionId = ref('')
const exportCount = ref(0)
const labelCopies = ref(2)
const exportingKey = ref('')
const includeArchived = ref(false)

const filters = reactive({
  categoryId: '',
  entryStatus: '',
  paymentStatus: '',
  deliveryStatus: '',
  keyword: '',
})

const entryStatusOptions = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'REGISTERED', label: '报名成功' },
  { value: 'STORED', label: '已入库' },
  { value: 'CANCELED', label: '已取消' },
  { value: 'RESULT_PUBLISHED', label: '结果已出' },
]

const statusLabels = {
  DRAFT: '草稿',
  REGISTRATION_OPEN: '报名中',
  REGISTRATION_CLOSED: '报名截止',
  JUDGING_PREP: '评审准备',
  JUDGING: '评审中',
  RESULT_CONFIRMING: '结果确认',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
}

const exportTemplates = [
  {
    key: 'entries',
    title: '报名酒款台账',
    stage: '报名核对',
    description: '导出酒款、厂牌、组别、支付状态和报名信息，用于报名截止后的资料复核',
    fileType: 'Excel',
    scope: '当前筛选范围',
    action: '导出台账',
    icon: Document,
    tone: 'blue',
  },
  {
    key: 'delivery',
    title: '样品入库清单',
    stage: '现场准备',
    description: '导出送样、快递单号、签收和入库状态，用于现场样品核对',
    fileType: 'Excel',
    scope: '当前筛选范围',
    action: '导出清单',
    icon: Box,
    tone: 'green',
  },
  {
    key: 'labels',
    title: '批量参赛标签',
    stage: '贴瓶打印',
    description: '导出 A4 打印版参赛标签，每页 4 张，标签包含二维码、参赛编号和组别',
    fileType: 'PDF',
    scope: '已支付且已生成参赛标签',
    action: '下载 PDF',
    icon: Printer,
    tone: 'gold',
  },
  {
    key: 'scoring',
    title: '评分数据',
    stage: '结果复盘',
    description: '导出评分、维度、桌长汇总、轮次和奖项数据，用于赛后归档',
    fileType: 'Excel',
    scope: '整场比赛',
    action: '导出评分',
    icon: DataAnalysis,
    tone: 'amber',
  },
]

const selectedCompetition = computed(() => competitions.value.find((item) => String(item.id) === selectedCompetitionId.value))

onMounted(async () => {
  await loadCompetitions()
  if (competitions.value.length) {
    const fromQuery = route.query.competitionId
      ? competitions.value.find((item) => String(item.id) === String(route.query.competitionId))
      : null
    const focus = fromQuery
      || competitions.value.find((item) => item.status === 'JUDGING_PREP')
      || competitions.value.find((item) => item.status === 'REGISTRATION_OPEN')
      || competitions.value[0]
    selectedCompetitionId.value = String(focus.id)
    await loadCategories()
    await refreshExportCount()
  }
})

async function loadCompetitions() {
  competitions.value = await fetchCompetitions({ includeArchived: includeArchived.value })
}

async function reloadCompetitions() {
  const currentId = selectedCompetitionId.value
  await loadCompetitions()
  const currentStillVisible = competitions.value.some((item) => String(item.id) === currentId)
  selectedCompetitionId.value = currentStillVisible ? currentId : ''
  if (!selectedCompetitionId.value && competitions.value.length) {
    const focus = competitions.value.find((item) => item.status === 'JUDGING_PREP')
      || competitions.value.find((item) => item.status === 'REGISTRATION_OPEN')
      || competitions.value[0]
    selectedCompetitionId.value = String(focus.id)
  }
  Object.assign(filters, { categoryId: '', entryStatus: '', paymentStatus: '', deliveryStatus: '', keyword: '' })
  await loadCategories()
  await refreshExportCount()
}

async function onCompetitionChange() {
  Object.assign(filters, { categoryId: '', entryStatus: '', paymentStatus: '', deliveryStatus: '', keyword: '' })
  await loadCategories()
  await refreshExportCount()
}

async function loadCategories() {
  categoryOptions.value = []
  if (!selectedCompetitionId.value) return
  const detail = await fetchCompetitionDetail(selectedCompetitionId.value)
  categoryOptions.value = detail.categories || []
}

async function refreshExportCount() {
  if (!selectedCompetitionId.value) {
    exportCount.value = 0
    return
  }
  const data = await fetchAdminEntries({
    competitionId: selectedCompetitionId.value,
    categoryId: filters.categoryId || undefined,
    status: filters.entryStatus || undefined,
    paymentStatus: filters.paymentStatus || undefined,
    deliveryStatus: filters.deliveryStatus || undefined,
    keyword: filters.keyword || undefined,
    page: 1,
    pageSize: 1,
  })
  exportCount.value = data.total || 0
}

function resetFilters() {
  Object.assign(filters, { categoryId: '', entryStatus: '', paymentStatus: '', deliveryStatus: '', keyword: '' })
  labelCopies.value = 2
  refreshExportCount()
}

async function runExport(type) {
  if (!selectedCompetitionId.value) return
  exportingKey.value = type
  try {
    const params = exportParams()
    let blob
    let fallbackName = `${selectedCompetition.value?.name || '比赛'}-导出文件`
    if (type === 'entries') {
      blob = await exportCompetitionEntries(selectedCompetitionId.value, params)
      fallbackName = `${selectedCompetition.value?.name || '比赛'}-报名酒款台账.xlsx`
    }
    if (type === 'delivery') {
      blob = await exportCompetitionDelivery(selectedCompetitionId.value, params)
      fallbackName = `${selectedCompetition.value?.name || '比赛'}-样品入库清单.xlsx`
    }
    if (type === 'labels') {
      blob = await exportCompetitionLabels(selectedCompetitionId.value, { ...params, copies: normalizedCopies(), format: 'pdf' })
      fallbackName = `${selectedCompetition.value?.name || '比赛'}-批量参赛标签.pdf`
    }
    if (type === 'scoring') {
      blob = await exportCompetitionScoringData(selectedCompetitionId.value)
      fallbackName = `${selectedCompetition.value?.name || '比赛'}-评分数据.xlsx`
    }
    downloadBlob(blob, fallbackName)
    ElMessage.success('导出文件已开始下载')
  } catch {
    ElMessage.error(exportErrorMessage(type))
  } finally {
    exportingKey.value = ''
  }
}

function exportParams() {
  return {
    categoryId: filters.categoryId || undefined,
    entryStatus: filters.entryStatus || undefined,
    paymentStatus: filters.paymentStatus || undefined,
    deliveryStatus: filters.deliveryStatus || undefined,
    keyword: filters.keyword || undefined,
  }
}

function normalizedCopies() {
  const value = Number(labelCopies.value || 2)
  return Math.min(Math.max(value, 1), 6)
}

function downloadBlob(blob, fileName) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

function exportErrorMessage(type) {
  if (!exportCount.value && type !== 'scoring') return '当前筛选范围没有可导出的酒款'
  if (type === 'labels') return '这场比赛还没有生成现场标签'
  return '导出失败，请稍后重试'
}

function statusLabel(status) {
  return statusLabels[status] || status || '-'
}
</script>

<style scoped>
.admin-exports-page {
  --panel: rgba(18, 29, 34, 0.9);
  --panel-soft: rgba(255, 255, 255, 0.035);
  --line: rgba(218, 232, 237, 0.1);
  --text: #e9f2f5;
  --muted: #8ea4ad;
  --gold: #d8a935;
  --green: #6fcf7a;
  --blue: #6fb4cf;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 28px;
  color: var(--text);
  background:
    linear-gradient(rgba(218, 232, 237, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(218, 232, 237, 0.032) 1px, transparent 1px),
    radial-gradient(circle at 80% 10%, rgba(216, 169, 53, 0.11), transparent 18rem),
    #0b1216;
  background-size: 68px 68px, 68px 68px, auto, auto;
}

.page-head,
.head-actions,
.filter-panel,
.summary-strip,
.card-head,
.export-button {
  display: flex;
  align-items: center;
}

.page-head {
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.page-head span,
.template-facts dt,
.field span,
.card-head small,
.summary-strip small {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.page-head > div:first-child span {
  color: var(--gold);
}

h1,
h2,
p,
dl {
  margin: 0;
}

h1 {
  margin-top: 5px;
  font-size: 30px;
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

button:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

svg {
  width: 1em;
  height: 1em;
}

.head-actions {
  gap: 10px;
}

.tool-button,
.export-button {
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  min-width: max-content;
  padding: 0 14px;
  color: var(--text);
  font-weight: 800;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel-soft);
}

.tool-button.primary,
.export-button {
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.1);
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(260px, 1.3fr) minmax(260px, 1.3fr) repeat(4, minmax(140px, 1fr));
  gap: 12px;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.field {
  display: grid;
  gap: 7px;
}

.checkbox-line {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  min-height: 38px;
  color: var(--text);
  font-weight: 700;
}

.field .checkbox-line input {
  width: 16px;
  min-height: 16px;
  padding: 0;
}

.field input,
.field select,
.copies-field input {
  width: 100%;
  min-height: 38px;
  padding: 0 10px;
  color: var(--text);
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
  color: #7f949d;
}

.search-field input {
  padding-left: 36px;
}

.summary-strip {
  display: grid;
  grid-template-columns: 2fr repeat(3, 1fr);
  gap: 12px;
  margin-top: 12px;
}

.summary-strip article {
  display: grid;
  gap: 6px;
  min-height: 78px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.summary-strip strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 20px;
}

.export-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.export-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 320px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(18, 29, 34, 0.92);
}

.card-head {
  gap: 12px;
}

.template-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.template-icon svg {
  width: 22px;
  height: 22px;
}

.template-icon.blue {
  color: var(--blue);
  background: rgba(111, 180, 207, 0.1);
}

.template-icon.green {
  color: var(--green);
  background: rgba(111, 207, 122, 0.1);
}

.template-icon.gold,
.template-icon.amber {
  color: #ffdc73;
  background: rgba(216, 169, 53, 0.1);
}

.card-head h2 {
  margin-top: 3px;
  font-size: 18px;
}

.export-card p {
  min-height: 66px;
  color: #a8bac2;
  line-height: 1.65;
}

.template-facts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.template-facts div {
  display: grid;
  gap: 5px;
  padding: 11px;
  border: 1px solid rgba(218, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.template-facts dd {
  margin: 0;
  font-weight: 800;
}

.copies-field {
  display: grid;
  gap: 7px;
  margin-top: auto;
}

.copies-field span {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.export-card:not(:has(.copies-field)) .export-button {
  margin-top: auto;
}

.export-button {
  width: 100%;
}

@media (max-width: 1320px) {
  .filter-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .export-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .admin-exports-page {
    padding: 18px;
  }

  .page-head,
  .summary-strip,
  .filter-panel,
  .export-grid {
    grid-template-columns: 1fr;
  }

  .page-head {
    align-items: stretch;
  }

  .head-actions {
    justify-content: flex-start;
  }
}
</style>
