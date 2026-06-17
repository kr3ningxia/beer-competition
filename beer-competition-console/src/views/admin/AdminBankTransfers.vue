<template>
  <div class="bank-transfer-page">
    <section class="page-head">
      <div>
        <span>财务确认</span>
        <h1>转账确认</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="resetFilters">
          <RefreshLeft />
          重置筛选
        </button>
        <button class="tool-button primary" type="button" @click="loadTransfers">
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
          <input v-model.trim="filters.keyword" placeholder="搜索厂牌、赛事、付款账户名、转账备注" @keyup.enter="applyFilters" />
        </div>
      </label>
      <label class="field">
        <span>状态</span>
        <select v-model="filters.status" @change="applyFilters">
          <option value="">全部状态</option>
          <option value="SUBMITTED">待确认</option>
          <option value="CONFIRMED">已确认</option>
          <option value="REJECTED">已驳回</option>
          <option value="CANCELED">已取消</option>
        </select>
      </label>
      <label class="field">
        <span>每页</span>
        <select v-model.number="filters.pageSize" @change="changePageSize">
          <option :value="20">20</option>
          <option :value="50">50</option>
          <option :value="100">100</option>
        </select>
      </label>
    </section>

    <section class="table-card">
      <div class="table-title">
        <h2>转账记录</h2>
        <span>{{ transfers.length }} / {{ total }} 笔</span>
      </div>
      <div class="transfer-table">
        <div class="table-head">
          <span>厂牌</span>
          <span>赛事</span>
          <span>金额 / 酒款</span>
          <span>付款信息</span>
          <span>状态</span>
          <span>提交时间</span>
          <span>操作</span>
        </div>
        <div class="table-body">
          <div v-for="item in transfers" :key="item.id" :class="['table-row', { urgent: item.status === 'SUBMITTED' }]">
            <div class="strong-cell">
              <strong>{{ item.breweryName || '-' }}</strong>
              <small>{{ item.payerName || '未填写付款账户名' }}</small>
            </div>
            <div class="soft-cell">
              <strong>{{ item.competitionName || '-' }}</strong>
              <small>{{ item.competitionCode || '-' }}</small>
            </div>
            <div class="soft-cell">
              <strong>{{ formatMoney(item.amount) }}</strong>
              <small>{{ item.entryName || '当前酒款' }}</small>
            </div>
            <div class="soft-cell">
              <strong class="remark-preview" :title="item.remark || ''">{{ item.remark || '-' }}</strong>
              <small>{{ formatTime(item.transferTime) }} · {{ item.voucherFileName ? '已上传凭证' : '未上传凭证' }}</small>
            </div>
            <span :class="['state-pill', statusTone(item.status)]">{{ statusLabel(item.status) }}</span>
            <span class="time-cell">{{ formatTime(item.submittedTime) }}</span>
            <div class="row-actions">
              <button type="button" @click="openDetail(item)">查看</button>
              <button v-if="item.status === 'SUBMITTED'" class="primary-action" type="button" @click="openDetail(item, 'confirm')">确认</button>
            </div>
          </div>
          <div v-if="!loading && !transfers.length" class="empty-state">
            <h2>{{ emptyStateTitle }}</h2>
            <p>{{ emptyStateDescription }}</p>
            <button v-if="emptyStateAction" type="button" @click="switchStatus(emptyStateAction.status)">
              {{ emptyStateAction.label }}
            </button>
          </div>
        </div>
      </div>
      <footer class="pagination-bar">
        <span>共 {{ total }} 条</span>
        <div class="pager-buttons">
          <button type="button" :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">上一页</button>
          <button type="button" :class="{ active: filters.page === page }" v-for="page in visiblePages" :key="page" @click="changePage(page)">
            {{ page }}
          </button>
          <button type="button" :disabled="filters.page >= totalPages" @click="changePage(filters.page + 1)">下一页</button>
        </div>
      </footer>
    </section>

    <div v-if="detailOpen" class="drawer-mask" @click.self="closeDetail">
      <aside class="transfer-drawer">
        <header>
          <div>
            <span>{{ detail?.competitionName || '转账详情' }}</span>
            <h2>{{ detail?.breweryName || '-' }}</h2>
            <small v-if="detail?.competitionCode">{{ detail.competitionCode }}</small>
          </div>
          <button type="button" @click="closeDetail">关闭</button>
        </header>

        <section v-if="detailLoading" class="drawer-loading">加载中</section>
        <template v-else-if="detail">
          <section class="summary-grid">
            <article><small>状态</small><strong>{{ statusLabel(detail.status) }}</strong></article>
            <article><small>金额</small><strong>{{ formatMoney(detail.amount) }}</strong></article>
            <article><small>付款时间</small><strong>{{ formatTime(detail.transferTime) }}</strong></article>
            <article><small>提交时间</small><strong>{{ formatTime(detail.submittedTime) }}</strong></article>
          </section>

          <section class="info-card">
            <div class="section-title">
              <strong>付款信息</strong>
              <div class="section-actions">
                <button type="button" :disabled="!detail.voucherAssetId || previewingVoucher" @click="previewVoucher">查看凭证</button>
                <button type="button" :disabled="!detail.voucherAssetId" @click="downloadVoucher">下载凭证</button>
              </div>
            </div>
            <dl>
              <div><dt>付款账户名</dt><dd>{{ detail.payerName || '-' }}</dd></div>
              <div><dt>凭证文件</dt><dd>{{ detail.voucherFileName || '未上传' }}</dd></div>
              <div class="full-info-row"><dt>转账备注</dt><dd>{{ detail.remark || '-' }}</dd></div>
              <div><dt>处理时间</dt><dd>{{ formatTime(detail.processedTime) }}</dd></div>
              <div><dt>处理说明</dt><dd>{{ detail.adminNote || '-' }}</dd></div>
            </dl>
          </section>

          <section class="info-card">
            <div class="section-title">
              <strong>当前酒款</strong>
            </div>
            <div class="entry-list">
              <article>
                <div>
                  <strong>{{ detail.entryName || '-' }}</strong>
                  <small>{{ [detail.shortCode, detail.categoryName, detail.style].filter(Boolean).join(' · ') }}</small>
                </div>
                <b>{{ formatMoney(detail.amount) }}</b>
              </article>
            </div>
          </section>

          <section v-if="detail.status === 'SUBMITTED'" class="process-card">
            <label>
              <span>处理说明</span>
              <textarea v-model.trim="processNote" placeholder="确认到账可不填；驳回时请填写原因"></textarea>
            </label>
            <div class="status-actions">
              <button type="button" :disabled="processing" @click="confirmTransfer">确认到账</button>
              <button class="danger" type="button" :disabled="processing" @click="rejectTransfer">驳回</button>
            </div>
          </section>
        </template>
      </aside>
    </div>

    <div v-if="voucherPreviewOpen" class="voucher-preview-mask" @click.self="closeVoucherPreview">
      <section class="voucher-preview-panel">
        <header>
          <strong>{{ voucherPreviewName }}</strong>
          <button type="button" @click="closeVoucherPreview">关闭</button>
        </header>
        <img v-if="voucherPreviewType === 'image'" :src="voucherPreviewUrl" alt="付款凭证" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  confirmAdminBankTransfer,
  downloadAdminBankTransferVoucher,
  fetchAdminBankTransferDetail,
  fetchAdminBankTransfers,
  rejectAdminBankTransfer,
} from '@/api/admin'

const transfers = ref([])
const total = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const detailOpen = ref(false)
const detail = ref(null)
const processNote = ref('')
const processing = ref(false)
const previewingVoucher = ref(false)
const voucherPreviewOpen = ref(false)
const voucherPreviewUrl = ref('')
const voucherPreviewName = ref('')
const voucherPreviewType = ref('')

const filters = reactive({
  status: 'SUBMITTED',
  keyword: '',
  page: 1,
  pageSize: 20,
})

const totalPages = computed(() => Math.max(Math.ceil(total.value / filters.pageSize), 1))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, filters.page - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})
const emptyStateTitle = computed(() => {
  if (filters.status === 'SUBMITTED') return '暂无待确认转账'
  if (filters.status === 'CONFIRMED') return '暂无已确认转账'
  if (filters.status === 'REJECTED') return '暂无已驳回转账'
  if (filters.status === 'CANCELED') return '暂无已取消转账'
  return '暂无转账记录'
})
const emptyStateDescription = computed(() => {
  if (filters.status === 'SUBMITTED') return '已确认或驳回的记录会移出待办，可切换状态继续查看'
  if (filters.status === 'CONFIRMED') return '确认到账后的记录会归档到这里'
  if (filters.status === 'REJECTED') return '驳回后的记录会归档到这里，厂商可重新提交付款信息'
  if (filters.status === 'CANCELED') return '厂商取消提交的记录会归档到这里'
  return '厂牌提交银行转账信息后会出现在这里'
})
const emptyStateAction = computed(() => {
  if (filters.status === 'SUBMITTED') return { label: '查看已确认记录', status: 'CONFIRMED' }
  if (filters.status === 'CONFIRMED' || filters.status === 'REJECTED' || filters.status === 'CANCELED') {
    return { label: '返回待确认', status: 'SUBMITTED' }
  }
  return null
})

onMounted(loadTransfers)
onBeforeUnmount(closeVoucherPreview)

async function loadTransfers() {
  loading.value = true
  try {
    const result = await fetchAdminBankTransfers(filters)
    transfers.value = result.records || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  filters.page = 1
  loadTransfers()
}

function resetFilters() {
  filters.status = 'SUBMITTED'
  filters.keyword = ''
  filters.page = 1
  loadTransfers()
}

function switchStatus(status) {
  filters.status = status
  filters.page = 1
  loadTransfers()
}

function changePage(page) {
  filters.page = page
  loadTransfers()
}

function changePageSize() {
  filters.page = 1
  loadTransfers()
}

async function openDetail(target, intent = '') {
  const id = typeof target === 'object' ? target.id : target
  const previousStatus = typeof target === 'object' ? target.status : ''
  detailOpen.value = true
  detailLoading.value = true
  processNote.value = ''
  try {
    detail.value = await fetchAdminBankTransferDetail(id)
    const row = transfers.value.find((item) => item.id === id)
    if (row) Object.assign(row, detail.value)
    if (previousStatus && detail.value?.status !== previousStatus) {
      await loadTransfers()
    }
    if (intent === 'confirm') {
      processNote.value = ''
    }
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailOpen.value = false
  detail.value = null
  closeVoucherPreview()
}

async function confirmTransfer() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      `确认这笔 ${formatMoney(detail.value.amount)} 转账已经到账？确认后该酒款将开放标签下载和送样信息填写`,
      '确认到账',
      {
        confirmButtonText: '确认到账',
        cancelButtonText: '再核对一下',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  processing.value = true
  try {
    detail.value = await confirmAdminBankTransfer(detail.value.id, { adminNote: processNote.value || undefined })
    ElMessage.success('已确认到账，记录已移至已确认')
    await loadTransfers()
  } catch (error) {
    ElMessage.warning(error?.message || '确认失败')
  } finally {
    processing.value = false
  }
}

async function rejectTransfer() {
  if (!detail.value) return
  if (!processNote.value) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await ElMessageBox.confirm(
      '驳回后厂商需要重新提交付款信息，请确认原因已经写清楚',
      '驳回转账',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '再核对一下',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  processing.value = true
  try {
    detail.value = await rejectAdminBankTransfer(detail.value.id, { adminNote: processNote.value })
    ElMessage.success('已驳回转账，记录已移至已驳回')
    await loadTransfers()
  } catch (error) {
    ElMessage.warning(error?.message || '驳回失败')
  } finally {
    processing.value = false
  }
}

async function downloadVoucher() {
  if (!detail.value?.voucherAssetId) return
  const blob = await downloadAdminBankTransferVoucher(detail.value.id)
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = detail.value.voucherFileName || `${detail.value.transferNo}-voucher`
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(objectUrl), 500)
}

async function previewVoucher() {
  if (!detail.value?.voucherAssetId) return
  previewingVoucher.value = true
  try {
    const blob = await downloadAdminBankTransferVoucher(detail.value.id)
    const fileName = detail.value.voucherFileName || '付款凭证'
    const objectUrl = URL.createObjectURL(blob)
    if (isImageVoucher(blob, fileName)) {
      closeVoucherPreview()
      voucherPreviewName.value = fileName
      voucherPreviewType.value = 'image'
      voucherPreviewUrl.value = objectUrl
      voucherPreviewOpen.value = true
      return
    }
    if (isPdfVoucher(blob, fileName)) {
      window.open(objectUrl, '_blank', 'noopener')
      setTimeout(() => URL.revokeObjectURL(objectUrl), 30000)
      return
    }
    URL.revokeObjectURL(objectUrl)
    ElMessage.warning('当前凭证格式不支持在线预览，请下载后查看')
  } catch (error) {
    ElMessage.warning(error?.message || '凭证预览失败')
  } finally {
    previewingVoucher.value = false
  }
}

function closeVoucherPreview() {
  if (voucherPreviewUrl.value) {
    URL.revokeObjectURL(voucherPreviewUrl.value)
  }
  voucherPreviewOpen.value = false
  voucherPreviewUrl.value = ''
  voucherPreviewName.value = ''
  voucherPreviewType.value = ''
}

function isImageVoucher(blob, fileName) {
  const type = blob?.type || ''
  if (type.startsWith('image/')) return true
  return /\.(jpe?g|png|webp|gif|bmp)$/i.test(fileName || '')
}

function isPdfVoucher(blob, fileName) {
  const type = blob?.type || ''
  if (type === 'application/pdf') return true
  return /\.pdf$/i.test(fileName || '')
}

function statusLabel(value) {
  return {
    SUBMITTED: '待确认',
    CONFIRMED: '已确认',
    REJECTED: '已驳回',
    CANCELED: '已取消',
  }[value] || value || '-'
}

function statusTone(value) {
  if (value === 'CONFIRMED') return 'success'
  if (value === 'SUBMITTED') return 'warning'
  if (value === 'REJECTED') return 'danger'
  return 'muted'
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `¥${Number(value).toFixed(2)}`
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.bank-transfer-page {
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
.table-card {
  width: 100%;
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
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tool-button,
.row-actions button,
.status-actions button,
.transfer-drawer header button,
.section-title button,
.empty-state button,
.pager-buttons button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  min-width: max-content;
  padding: 0 13px;
  color: #d5e2e7;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font-weight: 700;
  cursor: pointer;
}

.tool-button svg {
  width: 18px;
  height: 18px;
}

.tool-button.primary,
.row-actions .primary-action,
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
.status-actions button.danger {
  color: #ffaaa0;
  border-color: rgba(255, 122, 107, 0.25);
  background: rgba(255, 122, 107, 0.08);
}

.filter-panel {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) 160px 120px;
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

.field span,
.summary-grid small,
.info-card dt,
.process-card span {
  color: #8ea4ad;
  font-size: 12px;
  font-weight: 700;
}

.field input,
.field select,
.process-card textarea {
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

.table-card,
.transfer-drawer {
  border: 1px solid rgba(218, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(18, 29, 34, 0.9);
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

.table-title,
.section-title {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.table-title {
  margin-bottom: 12px;
}

.transfer-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow-x: auto;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(190px, 1.05fr) minmax(220px, 1.2fr) 130px minmax(240px, 1.35fr) 86px 130px 110px;
  gap: 12px;
  align-items: center;
  min-width: 1120px;
}

.table-head {
  flex: 0 0 auto;
  padding: 0 12px 10px;
  color: #8ea4ad;
  font-size: 13px;
  font-weight: 700;
}

.table-body {
  display: grid;
  flex: 1 1 auto;
  align-content: start;
  gap: 8px;
  min-width: 1120px;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.table-row {
  min-height: 72px;
  padding: 10px 12px;
  border: 1px solid rgba(218, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.table-row.urgent {
  border-color: rgba(216, 169, 53, 0.22);
  background: rgba(216, 169, 53, 0.055);
}

.strong-cell,
.soft-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.strong-cell strong,
.soft-cell strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.soft-cell .remark-preview {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.35;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.strong-cell small,
.soft-cell small,
.time-cell {
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

.state-pill.danger {
  color: #ffaaa0;
  background: rgba(255, 122, 107, 0.12);
}

.state-pill.muted {
  color: #b4c3c8;
  background: rgba(255, 255, 255, 0.06);
}

.pagination-bar {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.pager-buttons button.active {
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.1);
}

.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-end;
  background: rgba(3, 8, 10, 0.58);
}

.transfer-drawer {
  width: min(780px, 100vw);
  height: 100vh;
  padding: 22px;
  overflow-y: auto;
  border-radius: 0;
  border-right: 0;
}

.transfer-drawer header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.transfer-drawer header span,
.transfer-drawer header small {
  color: #8ea4ad;
}

.transfer-drawer h2 {
  margin: 4px 0;
  font-size: 26px;
}

.drawer-loading,
.empty-state {
  padding: 42px;
  text-align: center;
  color: #8ea4ad;
}

.empty-state button {
  margin-top: 14px;
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.12);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.summary-grid article,
.info-card,
.process-card {
  padding: 14px;
  border: 1px solid rgba(218, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.summary-grid strong {
  display: block;
  margin-top: 6px;
}

.info-card,
.process-card {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.info-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
  margin: 0;
}

.info-card .full-info-row {
  grid-column: 1 / -1;
}

.info-card dt,
.info-card dd {
  margin: 0;
}

.info-card dd {
  margin-top: 5px;
  overflow-wrap: anywhere;
  font-weight: 800;
}

.entry-list {
  display: grid;
  gap: 8px;
}

.entry-list article {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid rgba(218, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.46);
}

.entry-list article div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.entry-list small {
  color: #8ea4ad;
}

.entry-list b {
  color: #ffdc73;
  white-space: nowrap;
}

.process-card label {
  display: grid;
  gap: 8px;
}

.process-card textarea {
  min-height: 88px;
  padding: 10px 12px;
  resize: vertical;
  line-height: 1.5;
}

.voucher-preview-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 28px;
  background: rgba(3, 8, 10, 0.78);
}

.voucher-preview-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  width: min(980px, 96vw);
  max-height: 92vh;
  overflow: hidden;
  border: 1px solid rgba(218, 232, 237, 0.16);
  border-radius: 8px;
  background: #111b20;
  box-shadow: 0 26px 80px rgba(0, 0, 0, 0.42);
}

.voucher-preview-panel header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(218, 232, 237, 0.1);
}

.voucher-preview-panel header strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.voucher-preview-panel button {
  min-height: 34px;
  padding: 0 12px;
  color: #d5e2e7;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font-weight: 700;
  cursor: pointer;
}

.voucher-preview-panel img {
  display: block;
  max-width: 100%;
  max-height: calc(92vh - 60px);
  margin: auto;
  object-fit: contain;
}

@media (max-width: 980px) {
  .filter-panel,
  .summary-grid,
  .info-card dl {
    grid-template-columns: 1fr;
  }
}
</style>
