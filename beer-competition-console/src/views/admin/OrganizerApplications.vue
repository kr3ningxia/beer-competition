<template>
  <div class="applications-page">
    <AdminPageHeader title="主办方入驻">
      <template #actions>
        <button class="icon-button" type="button" aria-label="刷新申请列表" title="刷新申请列表" :disabled="loading" @click="loadApplications">
          <Refresh :class="{ spinning: loading }" />
        </button>
      </template>
    </AdminPageHeader>

    <section class="filter-bar">
      <label class="search-box">
        <Search />
        <input v-model.trim="keyword" type="search" placeholder="搜索主体名称、申请编号或联系人">
      </label>
      <nav class="filter-tabs" aria-label="申请状态筛选">
        <button
          v-for="item in statusFilters"
          :key="item.value"
          :class="{ active: statusFilter === item.value }"
          type="button"
          @click="setStatusFilter(item.value)"
        >
          {{ item.label }}
          <span v-if="item.value !== 'ALL'">{{ statusCounts[item.value] || 0 }}</span>
        </button>
      </nav>
    </section>

    <section class="table-panel">
      <div class="panel-toolbar">
        <span class="panel-title">申请列表</span>
        <span v-if="loading" class="panel-state">读取中</span>
        <span v-else-if="loadError" class="panel-state error">加载失败</span>
        <span v-else-if="accessDenied" class="panel-state error">无权访问</span>
        <span v-else class="panel-state">最近提交优先</span>
      </div>

      <div v-if="accessDenied" class="state-block">
        <Lock />
        <strong>当前账号无法查看入驻申请</strong>
      </div>
      <div v-else-if="loadError" class="state-block">
        <Warning />
        <strong>申请列表暂时不可用</strong>
        <button class="text-button" type="button" @click="loadApplications">重新加载</button>
      </div>
      <div v-else class="application-table">
        <div class="table-head">
          <span>主体</span>
          <span>联系人</span>
          <span>状态</span>
          <span>提交时间</span>
          <span>账号</span>
          <span>操作</span>
        </div>

        <div v-if="loading" class="skeleton-list" aria-label="加载中">
          <div v-for="item in 5" :key="item" class="skeleton-row">
            <i></i><i></i><i></i><i></i><i></i><i></i>
          </div>
        </div>
        <div v-else-if="!filteredApplications.length" class="state-block table-empty">
          <Document />
          <strong>{{ keyword || statusFilter !== 'ALL' ? '没有符合条件的申请' : '暂无入驻申请' }}</strong>
          <button v-if="keyword || statusFilter !== 'ALL'" class="text-button" type="button" @click="clearFilters">清除筛选</button>
        </div>
        <div v-else class="table-body">
          <button
            v-for="application in filteredApplications"
            :key="application.id"
            class="application-row"
            type="button"
            @click="openDetail(application)"
          >
            <span class="organization-cell">
              <strong>{{ application.organizationName || '未填写主体名称' }}</strong>
            </span>
            <span class="contact-cell">
              <strong>{{ application.contactName || '-' }}</strong>
              <small>{{ application.maskedContactPhone || '-' }}</small>
            </span>
            <span :class="['status-badge', statusTone(application.status)]">
              <i></i>
              {{ statusLabel(application.status) }}
            </span>
            <span class="time-cell">{{ formatTime(application.submittedTime) }}</span>
            <span class="account-cell">
              <template v-if="application.status === 'ACCOUNT_ISSUED'">
                <Check />
                已开通
              </template>
              <template v-else-if="application.status === 'APPROVED'">
                待发放
              </template>
              <template v-else>-</template>
            </span>
            <span class="row-action-cell">
              <span class="row-action">{{ actionLabel(application) }} <ArrowRight /></span>
            </span>
          </button>
        </div>
      </div>
    </section>

    <Transition name="drawer">
      <div v-if="drawerOpen" class="drawer-layer" @click.self="closeDetail">
        <aside class="detail-drawer" role="dialog" aria-modal="true" aria-labelledby="detail-title">
          <header class="drawer-head">
            <div>
              <h2 id="detail-title">{{ selectedApplication?.organizationName || '入驻申请' }}</h2>
              <code>{{ selectedApplication?.applicationNo }}</code>
            </div>
            <button class="icon-button" type="button" aria-label="关闭详情" title="关闭详情" @click="closeDetail">
              <Close />
            </button>
          </header>

          <div v-if="selectedApplication" class="drawer-body">
            <div class="drawer-status-line">
              <span :class="['status-badge', statusTone(selectedApplication.status)]">
                <i></i>
                {{ statusLabel(selectedApplication.status) }}
              </span>
              <span v-if="selectedApplication.reviewedTime" class="muted-time">审核于 {{ formatTime(selectedApplication.reviewedTime) }}</span>
            </div>

            <div class="detail-grid">
              <div class="detail-item detail-wide">
                <span>联系人</span>
                <strong>{{ selectedApplication.contactName || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>手机号</span>
                <strong>{{ selectedApplication.contactPhone || selectedApplication.maskedContactPhone || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>邮箱</span>
                <strong>{{ selectedApplication.contactEmail || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>微信号</span>
                <strong>{{ selectedApplication.wechat || selectedApplication.maskedWechat || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>预计规模</span>
                <strong>{{ selectedApplication.expectedScale || '-' }}</strong>
              </div>
            </div>

            <div v-if="selectedApplication.businessDescription" class="detail-copy">
              <span>办赛介绍</span>
              <p>{{ selectedApplication.businessDescription }}</p>
            </div>
            <div v-if="selectedApplication.supplementalNote" class="detail-copy">
              <span>补充说明</span>
              <p>{{ selectedApplication.supplementalNote }}</p>
            </div>

            <div class="detail-material">
              <span>主体证明材料</span>
              <div v-if="selectedApplication.materialAssetId" class="material-actions">
                <span class="material-name">{{ selectedApplication.materialFileName || '已上传材料' }}</span>
                <button class="material-link" type="button" :disabled="materialLoading" @click="downloadMaterial">
                  <Loading v-if="materialLoading" class="spinning" />
                  <Download v-else />
                  {{ materialLoading ? '读取中' : '下载材料' }}
                </button>
              </div>
              <em v-else>未上传</em>
            </div>

            <div v-if="selectedApplication.reviewRemark" class="review-remark">
              <span>审核备注</span>
              <p>{{ selectedApplication.reviewRemark }}</p>
            </div>

            <Transition name="review-editor">
              <div v-if="reviewAction" class="review-editor">
                <div class="review-editor-head">
                  <span>{{ reviewAction === 'NEED_MORE_INFO' ? '退回补充资料' : '拒绝申请' }}</span>
                  <button class="icon-button small" type="button" aria-label="取消操作" title="取消操作" @click="cancelReview"><Close /></button>
                </div>
                <textarea v-model.trim="reviewRemark" rows="4" :placeholder="reviewAction === 'NEED_MORE_INFO' ? '写明需要补充的资料' : '写明本次拒绝原因'"></textarea>
                <div class="review-editor-actions">
                  <button class="ghost-button" type="button" @click="cancelReview">取消</button>
                  <button class="danger-button" type="button" :disabled="reviewLoading || !reviewRemark" @click="submitReviewAction">
                    <Loading v-if="reviewLoading" class="spinning" />
                    {{ reviewLoading ? '提交中' : '确认' }}
                  </button>
                </div>
              </div>
            </Transition>
          </div>

          <footer v-if="selectedApplication" class="drawer-actions">
            <template v-if="!reviewAction">
              <template v-if="['SUBMITTED', 'NEED_MORE_INFO', 'UNDER_REVIEW'].includes(selectedApplication.status)">
                <button class="ghost-button warning" type="button" :disabled="actionLoading" @click="reviewAction = 'NEED_MORE_INFO'">退回补充</button>
                <button class="ghost-button danger" type="button" :disabled="actionLoading" @click="reviewAction = 'REJECTED'">拒绝申请</button>
                <button class="primary-button" type="button" :disabled="actionLoading" @click="approveApplication">审核通过</button>
              </template>
              <button v-if="selectedApplication.status === 'APPROVED'" class="primary-button full-action" type="button" :disabled="actionLoading" @click="provisionApplication">
                <Loading v-if="actionLoading" class="spinning" />
                <Key v-else />
                {{ actionLoading ? '发放中' : '发放初始账号' }}
              </button>
              <span v-if="selectedApplication.status === 'ACCOUNT_ISSUED'" class="completed-action"><Check /> 账号已发放</span>
              <span v-if="selectedApplication.status === 'REJECTED'" class="completed-action muted"><Close /> 本次申请已结束</span>
            </template>
          </footer>
        </aside>
      </div>
    </Transition>

    <Transition name="modal">
      <div v-if="credentialVisible" class="modal-layer" @click.self="closeCredential">
        <section class="credential-modal" role="dialog" aria-modal="true" aria-labelledby="credential-title">
          <header class="credential-head">
            <div class="credential-mark"><Key /></div>
            <button class="icon-button" type="button" aria-label="关闭账号凭据" title="关闭账号凭据" @click="closeCredential"><Close /></button>
          </header>
          <h2 id="credential-title">账号已发放</h2>
          <p class="credential-organization">{{ credentialData?.organizationName }}</p>
          <div v-if="credentialData?.initialPassword" class="credential-fields">
            <div class="credential-field">
              <span>登录账号</span>
              <code>{{ credentialData.username }}</code>
              <button type="button" aria-label="复制登录账号" title="复制登录账号" @click="copyCredential(credentialData.username)"><CopyDocument /></button>
            </div>
            <div class="credential-field">
              <span>初始密码</span>
              <code>{{ credentialData.initialPassword }}</code>
              <button type="button" aria-label="复制初始密码" title="复制初始密码" @click="copyCredential(credentialData.initialPassword)"><CopyDocument /></button>
            </div>
          </div>
          <div v-else class="credential-issued">
            <Check />
            <span>该申请已完成账号发放</span>
          </div>
          <button class="primary-button credential-close" type="button" @click="closeCredential">完成</button>
        </section>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import {
  ArrowRight,
  Check,
  Close,
  CopyDocument,
  Document,
  Download,
  Key,
  Loading,
  Lock,
  Refresh,
  Search,
  Warning,
} from '@element-plus/icons-vue'
import {
  downloadOrganizerApplicationMaterial,
  fetchOrganizerApplications,
  provisionOrganizerApplication,
  reviewOrganizerApplication,
} from '@/api/organizerApplicationAdmin'

const applications = ref([])
const loading = ref(false)
const loadError = ref(false)
const accessDenied = ref(false)
const keyword = ref('')
const statusFilter = ref('ALL')
const drawerOpen = ref(false)
const selectedApplication = ref(null)
const actionLoading = ref(false)
const materialLoading = ref(false)
const reviewLoading = ref(false)
const reviewAction = ref('')
const reviewRemark = ref('')
const credentialVisible = ref(false)
const credentialData = ref(null)

const statusFilters = [
  { label: '全部', value: 'ALL' },
  { label: '待处理', value: 'SUBMITTED' },
  { label: '待补资料', value: 'NEED_MORE_INFO' },
  { label: '待发放', value: 'APPROVED' },
  { label: '已开通', value: 'ACCOUNT_ISSUED' },
  { label: '已拒绝', value: 'REJECTED' },
]

const filteredApplications = computed(() => {
  const search = keyword.value.toLowerCase()
  return applications.value.filter((item) => {
    const matchesStatus = statusFilter.value === 'ALL' || item.status === statusFilter.value
    const matchesKeyword = !search || [item.organizationName, item.applicationNo, item.contactName, item.contactEmail]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(search))
    return matchesStatus && matchesKeyword
  })
})

const statusCounts = computed(() => applications.value.reduce((counts, item) => {
  counts[item.status] = (counts[item.status] || 0) + 1
  return counts
}, {}))

onMounted(loadApplications)

async function loadApplications() {
  loading.value = true
  loadError.value = false
  accessDenied.value = false
  try {
    applications.value = await fetchOrganizerApplications(statusFilter.value) || []
  } catch (error) {
    applications.value = []
    accessDenied.value = error?.response?.status === 403
    loadError.value = !accessDenied.value
  } finally {
    loading.value = false
  }
}

function setStatusFilter(value) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  loadApplications()
}

function clearFilters() {
  keyword.value = ''
  statusFilter.value = 'ALL'
  loadApplications()
}

function openDetail(application) {
  selectedApplication.value = application
  drawerOpen.value = true
  cancelReview()
}

function closeDetail() {
  if (actionLoading.value || reviewLoading.value) return
  drawerOpen.value = false
  selectedApplication.value = null
  cancelReview()
}

function cancelReview() {
  reviewAction.value = ''
  reviewRemark.value = ''
}

async function submitReviewAction(explicitStatus) {
  const status = explicitStatus || reviewAction.value
  if (!selectedApplication.value || !status) return
  if ((status === 'NEED_MORE_INFO' || status === 'REJECTED') && !reviewRemark.value.trim()) return
  if (status === 'REJECTED' && !explicitStatus) {
    const confirmed = await confirmDanger('拒绝此申请后，本次申请将结束，申请人需要重新提交。', '确认拒绝')
    if (!confirmed) return
  }
  actionLoading.value = true
  reviewLoading.value = Boolean(reviewAction.value)
  try {
    const updated = await reviewOrganizerApplication(selectedApplication.value.id, {
      status,
      remark: reviewRemark.value.trim() || undefined,
    })
    replaceApplication(updated)
    ElMessage.success(statusLabel(status))
    cancelReview()
  } finally {
    actionLoading.value = false
    reviewLoading.value = false
  }
}

async function approveApplication() {
  const confirmed = await confirmDanger('审核通过后将进入账号发放阶段。', '确认审核通过')
  if (!confirmed) return
  await submitReviewAction('APPROVED')
}

async function provisionApplication() {
  if (!selectedApplication.value) return
  actionLoading.value = true
  try {
    const result = await provisionOrganizerApplication(selectedApplication.value.id)
    replaceApplication({ ...selectedApplication.value, status: 'ACCOUNT_ISSUED', statusLabel: '账号已发放', accountIssuedTime: new Date().toISOString() })
    credentialData.value = {
      ...result,
      organizationName: selectedApplication.value.organizationName,
    }
    credentialVisible.value = true
  } finally {
    actionLoading.value = false
  }
}

function replaceApplication(updated) {
  if (!updated?.id) return
  const index = applications.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) applications.value.splice(index, 1, updated)
  selectedApplication.value = updated
}

async function downloadMaterial() {
  const assetId = selectedApplication.value?.materialAssetId
  if (!assetId || materialLoading.value) return
  materialLoading.value = true
  try {
    const blob = await downloadOrganizerApplicationMaterial(assetId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = selectedApplication.value.materialFileName
      || `${selectedApplication.value.applicationNo || 'organizer-application'}-material`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } finally {
    materialLoading.value = false
  }
}

async function confirmDanger(message, title) {
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    return true
  } catch {
    return false
  }
}

function copyCredential(value) {
  if (!value) return
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(value).then(() => ElMessage.success('已复制')).catch(() => {})
    return
  }
  const input = document.createElement('textarea')
  input.value = value
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  input.remove()
  ElMessage.success('已复制')
}

function closeCredential() {
  credentialVisible.value = false
  credentialData.value = null
}

function actionLabel(application) {
  if (application.status === 'SUBMITTED' || application.status === 'NEED_MORE_INFO' || application.status === 'UNDER_REVIEW') return '处理申请'
  if (application.status === 'APPROVED') return '发放账号'
  return '查看详情'
}

function statusLabel(status) {
  return {
    SUBMITTED: '待处理',
    UNDER_REVIEW: '待处理',
    NEED_MORE_INFO: '待补充资料',
    APPROVED: '待发放账号',
    ACCOUNT_ISSUED: '账号已发放',
    REJECTED: '已拒绝',
  }[status] || status || '-'
}

function statusTone(status) {
  return {
    SUBMITTED: 'pending',
    UNDER_REVIEW: 'pending',
    NEED_MORE_INFO: 'warning',
    APPROVED: 'approved',
    ACCOUNT_ISSUED: 'issued',
    REJECTED: 'rejected',
  }[status] || 'neutral'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.applications-page {
  --bg: #0d1519;
  --panel: rgba(22, 32, 36, 0.9);
  --panel-soft: rgba(255, 255, 255, 0.035);
  --line: rgba(219, 232, 237, 0.1);
  --line-strong: rgba(219, 232, 237, 0.16);
  --text: #e6edf0;
  --muted: #91a5ad;
  --faint: #60747d;
  --gold: #d8a935;
  --gold-soft: #e0b84a;
  --green: #70cf7c;
  --orange: #f1bd79;
  --red: #ffb4a8;
  position: relative;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0 28px 18px;
  overflow: hidden;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.024) 1px, transparent 1px),
    radial-gradient(circle at 18% 6%, rgba(216, 169, 53, 0.13), transparent 20rem),
    linear-gradient(135deg, #0d1519 0%, #111c20 52%, #0b1418 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
}

h1,
h2,
p {
  margin: 0;
}

button,
input,
textarea {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

svg {
  width: 1em;
  height: 1em;
}

.filter-bar,
.search-box,
.filter-tabs,
.panel-toolbar,
.application-row,
.organization-cell,
.contact-cell,
.account-cell,
.row-action,
.drawer-status-line,
.drawer-actions,
.detail-material,
.review-editor-head,
.review-editor-actions,
.credential-head,
.credential-field,
.credential-issued {
  display: flex;
  align-items: center;
}

.count-pill,
.total-count {
  display: inline-flex;
  align-items: center;
  min-height: 25px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.count-pill {
  color: var(--gold-soft);
  background: rgba(216, 169, 53, 0.1);
  border: 1px solid rgba(216, 169, 53, 0.24);
}

.total-count {
  color: var(--muted);
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid var(--line);
}

.icon-button {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 38px;
  height: 38px;
  color: var(--gold-soft);
  background: rgba(216, 169, 53, 0.07);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
}

.icon-button:hover,
.icon-button:focus-visible {
  color: #f4d475;
  background: rgba(216, 169, 53, 0.14);
  outline: 0;
}

.icon-button svg {
  width: 17px;
  height: 17px;
}

.icon-button.small {
  width: 30px;
  height: 30px;
  color: var(--muted);
  background: transparent;
  border-color: var(--line);
}

.filter-bar,
.table-panel {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.filter-bar {
  flex: 0 0 auto;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  padding: 12px 14px;
}

.search-box {
  flex: 0 1 360px;
  gap: 9px;
  min-height: 42px;
  padding: 0 12px;
  color: var(--muted);
  background: rgba(7, 14, 17, 0.68);
  border: 1px solid var(--line-strong);
  border-radius: 7px;
}

.search-box:focus-within {
  border-color: rgba(216, 169, 53, 0.44);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.07);
}

.search-box svg {
  flex: 0 0 auto;
  width: 16px;
  height: 16px;
}

.search-box input {
  width: 100%;
  min-width: 0;
  color: var(--text);
  background: transparent;
  border: 0;
  outline: 0;
  font-size: 13px;
}

.search-box input::placeholder {
  color: var(--faint);
}

.filter-tabs {
  justify-content: flex-end;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-tabs button {
  min-height: 36px;
  padding: 0 10px;
  color: #a9bbc2;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.filter-tabs button:hover,
.filter-tabs button:focus-visible {
  color: var(--text);
  background: rgba(255, 255, 255, 0.04);
  outline: 0;
}

.filter-tabs button.active {
  color: var(--gold-soft);
  background: rgba(216, 169, 53, 0.09);
  border-color: rgba(216, 169, 53, 0.3);
}

.filter-tabs button span {
  margin-left: 5px;
  color: var(--faint);
  font-size: 11px;
}

.filter-tabs button.active span {
  color: var(--gold-soft);
}

.table-panel {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 16px;
  padding: 14px;
  overflow: hidden;
}

.panel-toolbar {
  flex: 0 0 auto;
  justify-content: space-between;
  gap: 12px;
  min-height: 34px;
  margin-bottom: 12px;
}

.panel-title {
  color: #dce9ed;
  font-size: 14px;
  font-weight: 900;
}

.panel-state {
  color: var(--faint);
  font-size: 12px;
}

.panel-state.error {
  color: var(--red);
}

.application-table,
.table-body,
.skeleton-list {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-head,
.application-row,
.skeleton-row {
  display: grid;
  grid-template-columns: minmax(230px, 1.6fr) minmax(150px, 1fr) minmax(110px, 0.68fr) minmax(145px, 0.88fr) minmax(90px, 0.6fr) minmax(130px, 0.84fr);
  gap: 14px;
  align-items: center;
}

.table-head {
  flex: 0 0 auto;
  padding: 0 14px 9px;
  color: var(--muted);
  font-size: 12px;
}

.table-body {
  gap: 8px;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.table-body::-webkit-scrollbar {
  width: 9px;
}

.table-body::-webkit-scrollbar-thumb {
  border: 2px solid rgba(22, 32, 36, 0.95);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.24);
}

.application-row {
  width: 100%;
  min-height: 70px;
  padding: 11px 14px;
  color: var(--text);
  text-align: left;
  background: rgba(255, 255, 255, 0.026);
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  transition: background 160ms ease, border-color 160ms ease, transform 160ms ease;
}

.application-row:hover,
.application-row:focus-visible {
  background: rgba(216, 169, 53, 0.055);
  border-color: rgba(216, 169, 53, 0.25);
  outline: 0;
  transform: translateY(-1px);
}

.organization-cell,
.contact-cell {
  align-items: flex-start;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.organization-cell strong,
.contact-cell strong,
.organization-cell small,
.contact-cell small,
.time-cell,
.account-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.organization-cell strong,
.contact-cell strong {
  color: #e6edf0;
  font-size: 13px;
}

.organization-cell small,
.contact-cell small,
.time-cell {
  color: var(--muted);
  font-size: 11px;
}

.organization-cell small {
  font-family: 'SFMono-Regular', Consolas, monospace;
  letter-spacing: 0.02em;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  min-height: 28px;
  padding: 0 9px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.status-badge i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge.pending {
  color: var(--gold-soft);
  background: rgba(216, 169, 53, 0.1);
  border: 1px solid rgba(216, 169, 53, 0.2);
}

.status-badge.reviewing {
  color: #9bc9d8;
  background: rgba(111, 180, 207, 0.1);
  border: 1px solid rgba(111, 180, 207, 0.2);
}

.status-badge.warning {
  color: var(--orange);
  background: rgba(242, 153, 74, 0.1);
  border: 1px solid rgba(242, 153, 74, 0.2);
}

.status-badge.approved,
.status-badge.issued {
  color: var(--green);
  background: rgba(111, 207, 122, 0.1);
  border: 1px solid rgba(111, 207, 122, 0.2);
}

.status-badge.rejected {
  color: var(--red);
  background: rgba(255, 180, 168, 0.08);
  border: 1px solid rgba(255, 180, 168, 0.2);
}

.status-badge.neutral {
  color: var(--muted);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--line);
}

.account-cell {
  gap: 5px;
  color: var(--muted);
  font-size: 12px;
}

.account-cell svg {
  flex: 0 0 auto;
  width: 15px;
  height: 15px;
  color: var(--green);
}

.row-action-cell {
  display: flex;
  justify-content: flex-end;
}

.row-action {
  gap: 5px;
  color: var(--gold-soft);
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.row-action svg {
  width: 15px;
  height: 15px;
  transition: transform 160ms ease;
}

.application-row:hover .row-action svg,
.application-row:focus-visible .row-action svg {
  transform: translateX(3px);
}

.skeleton-list {
  gap: 8px;
}

.skeleton-row {
  min-height: 70px;
  padding: 11px 14px;
  border: 1px solid rgba(219, 232, 237, 0.06);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.018);
}

.skeleton-row i {
  display: block;
  height: 10px;
  border-radius: 5px;
  background: linear-gradient(90deg, rgba(255,255,255,0.04), rgba(255,255,255,0.12), rgba(255,255,255,0.04));
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-row i:first-child {
  width: 72%;
}

.skeleton-row i:nth-child(2) {
  width: 58%;
}

.skeleton-row i:nth-child(3) {
  width: 64px;
}

.skeleton-row i:nth-child(4) {
  width: 82%;
}

.skeleton-row i:nth-child(5) {
  width: 42px;
}

.skeleton-row i:nth-child(6) {
  width: 74px;
  justify-self: end;
}

.state-block {
  flex: 1 1 auto;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  min-height: 220px;
  color: var(--muted);
  text-align: center;
}

.state-block svg {
  width: 28px;
  height: 28px;
  color: var(--gold-soft);
}

.state-block strong {
  color: #c6d3d8;
  font-size: 14px;
}

.text-button {
  padding: 0;
  color: var(--gold-soft);
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 900;
}

.text-button:hover,
.text-button:focus-visible {
  color: #f5d778;
  text-decoration: underline;
  text-underline-offset: 4px;
  outline: 0;
}

.drawer-layer,
.modal-layer {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(3, 9, 12, 0.68);
  backdrop-filter: blur(8px);
}

.detail-drawer {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  flex-direction: column;
  width: min(570px, 100%);
  height: 100%;
  color: var(--text);
  background: #111b1f;
  border-left: 1px solid var(--line-strong);
  box-shadow: -22px 0 70px rgba(0, 0, 0, 0.3);
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 25px 20px;
  border-bottom: 1px solid var(--line);
}

.drawer-head h2 {
  overflow: hidden;
  color: #f1f6f7;
  font-size: 22px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-head code {
  display: block;
  margin-top: 7px;
  color: var(--muted);
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
}

.drawer-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 25px 28px;
}

.drawer-body::-webkit-scrollbar {
  width: 9px;
}

.drawer-body::-webkit-scrollbar-thumb {
  border: 2px solid #111b1f;
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.24);
}

.drawer-status-line {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
}

.muted-time {
  color: var(--faint);
  font-size: 11px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.detail-item {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 14px;
  background: rgba(255, 255, 255, 0.025);
}

.detail-item.detail-wide {
  grid-column: 1 / -1;
}

.detail-item span,
.detail-copy > span,
.detail-material > span,
.review-remark > span,
.credential-field > span {
  color: var(--faint);
  font-size: 11px;
}

.detail-item strong {
  overflow: hidden;
  color: #dce9ed;
  font-size: 13px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-copy,
.detail-material,
.review-remark {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px dashed var(--line);
}

.detail-copy p,
.review-remark p {
  margin-top: 8px;
  color: #c8d5d9;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.detail-material {
  justify-content: space-between;
  gap: 18px;
}

.material-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  min-width: 0;
}

.material-name {
  overflow: hidden;
  max-width: 220px;
  color: #c8d5d9;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-material em {
  color: var(--faint);
  font-size: 12px;
  font-style: normal;
}

.material-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  color: var(--gold-soft);
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 900;
}

.material-link:hover,
.material-link:focus-visible {
  color: #f5d778;
  text-decoration: underline;
  text-underline-offset: 4px;
  outline: 0;
}

.material-link svg {
  width: 15px;
  height: 15px;
}

.review-remark {
  padding: 14px 15px 0;
  background: rgba(216, 169, 53, 0.06);
  border-top: 1px solid rgba(216, 169, 53, 0.22);
}

.review-editor {
  margin-top: 21px;
  padding: 15px;
  background: rgba(6, 13, 16, 0.68);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
}

.review-editor-head {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 11px;
  color: var(--gold-soft);
  font-size: 13px;
  font-weight: 900;
}

.review-editor textarea {
  display: block;
  width: 100%;
  min-height: 92px;
  padding: 11px 12px;
  color: var(--text);
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid var(--line-strong);
  border-radius: 6px;
  outline: 0;
  resize: vertical;
  font-size: 13px;
  line-height: 1.6;
}

.review-editor textarea:focus {
  border-color: rgba(216, 169, 53, 0.5);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08);
}

.review-editor textarea::placeholder {
  color: var(--faint);
}

.review-editor-actions {
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.drawer-actions {
  flex: 0 0 auto;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 73px;
  padding: 14px 25px;
  border-top: 1px solid var(--line);
  background: rgba(13, 21, 25, 0.88);
}

.primary-button,
.ghost-button,
.danger-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 14px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 900;
}

.primary-button {
  color: #1b1408;
  background: var(--gold);
  border: 1px solid rgba(224, 184, 74, 0.56);
}

.primary-button:hover,
.primary-button:focus-visible {
  background: #ebc24f;
  outline: 0;
}

.ghost-button,
.danger-button {
  color: #bacbd0;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid var(--line-strong);
}

.ghost-button:hover,
.ghost-button:focus-visible {
  color: var(--text);
  background: rgba(255, 255, 255, 0.07);
  outline: 0;
}

.ghost-button.warning {
  color: var(--orange);
  border-color: rgba(242, 153, 74, 0.24);
  background: rgba(242, 153, 74, 0.07);
}

.ghost-button.danger,
.danger-button {
  color: var(--red);
  border-color: rgba(255, 180, 168, 0.24);
  background: rgba(255, 180, 168, 0.06);
}

.danger-button:hover,
.danger-button:focus-visible,
.ghost-button.danger:hover,
.ghost-button.danger:focus-visible {
  background: rgba(255, 180, 168, 0.12);
  outline: 0;
}

.full-action {
  flex: 1 1 auto;
}

.completed-action {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 5px;
  color: var(--green);
  font-size: 12px;
  font-weight: 900;
}

.completed-action.muted {
  color: var(--muted);
}

.completed-action svg {
  width: 16px;
  height: 16px;
}

.credential-modal {
  position: absolute;
  top: 50%;
  left: 50%;
  width: min(440px, calc(100% - 32px));
  padding: 23px;
  color: var(--text);
  background: #152125;
  border: 1px solid var(--line-strong);
  border-radius: 9px;
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.42);
  transform: translate(-50%, -50%);
}

.credential-head {
  justify-content: space-between;
  gap: 14px;
}

.credential-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  color: #1b1408;
  background: var(--gold);
  border-radius: 8px;
}

.credential-mark svg {
  width: 20px;
  height: 20px;
}

.credential-modal h2 {
  margin-top: 20px;
  font-size: 22px;
  line-height: 1.25;
}

.credential-organization {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.credential-fields {
  display: grid;
  gap: 9px;
  margin-top: 21px;
}

.credential-field {
  gap: 10px;
  min-height: 52px;
  padding: 0 11px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--line);
  border-radius: 7px;
}

.credential-field > span {
  flex: 0 0 58px;
}

.credential-field code {
  flex: 1 1 auto;
  overflow: hidden;
  color: #f1d978;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credential-field button {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 30px;
  height: 30px;
  color: var(--muted);
  background: transparent;
  border: 0;
  border-radius: 5px;
}

.credential-field button:hover,
.credential-field button:focus-visible {
  color: var(--gold-soft);
  background: rgba(216, 169, 53, 0.1);
  outline: 0;
}

.credential-field button svg {
  width: 15px;
  height: 15px;
}

.credential-issued {
  gap: 9px;
  margin-top: 22px;
  padding: 15px;
  color: var(--green);
  background: rgba(111, 207, 122, 0.08);
  border: 1px solid rgba(111, 207, 122, 0.18);
  border-radius: 7px;
  font-size: 13px;
  font-weight: 800;
}

.credential-issued svg {
  width: 17px;
  height: 17px;
}

.credential-close {
  width: 100%;
  margin-top: 22px;
}

.spinning {
  animation: spin 900ms linear infinite;
}

.drawer-enter-active,
.drawer-leave-active,
.modal-enter-active,
.modal-leave-active,
.review-editor-enter-active,
.review-editor-leave-active {
  transition: opacity 180ms ease, transform 220ms ease;
}

.drawer-enter-from,
.drawer-leave-to,
.modal-enter-from,
.modal-leave-to,
.review-editor-enter-from,
.review-editor-leave-to {
  opacity: 0;
}

.drawer-enter-from .detail-drawer,
.drawer-leave-to .detail-drawer {
  transform: translateX(24px);
}

.modal-enter-from .credential-modal,
.modal-leave-to .credential-modal {
  transform: translate(-50%, calc(-50% + 10px));
}

.review-editor-enter-from,
.review-editor-leave-to {
  transform: translateY(-5px);
}

@keyframes shimmer {
  from { background-position: 100% 0; }
  to { background-position: -100% 0; }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1260px) {
  .applications-page {
    padding: 22px 18px 22px;
  }

  .filter-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    flex-basis: auto;
    max-width: none;
  }

  .filter-tabs {
    justify-content: flex-start;
  }

  .table-head {
    display: none;
  }

  .application-row {
    grid-template-columns: minmax(220px, 1.5fr) minmax(140px, 1fr) minmax(110px, 0.8fr) minmax(135px, 0.9fr) minmax(80px, 0.6fr) minmax(120px, 0.8fr);
  }
}

@media (max-width: 850px) {
  .applications-page {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .table-panel {
    overflow: visible;
  }

  .application-table {
    overflow: visible;
  }

  .table-body {
    overflow: visible;
  }

  .application-row {
    grid-template-columns: 1fr 1fr;
    gap: 15px 12px;
    min-height: 0;
    padding: 15px;
  }

  .application-row > :nth-child(3),
  .application-row > :nth-child(4),
  .application-row > :nth-child(5),
  .application-row > :nth-child(6) {
    align-self: center;
  }

  .row-action-cell {
    justify-content: flex-start;
  }

  .detail-drawer {
    width: min(570px, calc(100% - 8px));
  }
}

@media (max-width: 560px) {
  .applications-page {
    padding: 0 12px 18px;
  }

  .filter-tabs {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
  }

  .filter-tabs button {
    padding: 0 5px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .table-panel {
    padding: 10px;
  }

  .panel-toolbar {
    padding-inline: 3px;
  }

  .application-row {
    grid-template-columns: 1fr;
    gap: 11px;
  }

  .application-row > :nth-child(3),
  .application-row > :nth-child(4),
  .application-row > :nth-child(5),
  .application-row > :nth-child(6) {
    align-self: start;
  }

  .row-action-cell {
    padding-top: 3px;
  }

  .drawer-head,
  .drawer-body {
    padding-inline: 18px;
  }

  .drawer-actions {
    padding-inline: 18px;
  }

  .drawer-actions > * {
    flex: 1 1 auto;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-item.detail-wide {
    grid-column: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .spinning,
  .skeleton-row i {
    animation: none;
  }

  .application-row,
  .row-action svg,
  .drawer-enter-active,
  .drawer-leave-active,
  .modal-enter-active,
  .modal-leave-active,
  .review-editor-enter-active,
  .review-editor-leave-active {
    transition: none;
  }
}
</style>
