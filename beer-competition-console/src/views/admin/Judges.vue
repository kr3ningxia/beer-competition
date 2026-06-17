<template>
  <div class="judges-page">
    <section class="page-head">
      <div>
        <h1>评审列表</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="loadJudges">
          <Refresh />
          刷新名单
        </button>
        <button class="tool-button primary" type="button" @click="router.push('/admin/assignments')">
          <Connection />
          评审编排
        </button>
      </div>
    </section>

    <section class="toolbar">
      <label class="search-box">
        <Search />
        <input v-model.trim="keyword" placeholder="搜索姓名、手机号后四位、资质、回避酒厂" @input="scheduleLoadJudges" @keyup.enter="loadFirstPage" />
      </label>
      <div class="filter-tabs" aria-label="评审状态筛选">
        <button
          v-for="item in statusFilters"
          :key="item.value"
          :class="{ active: statusFilter === item.value }"
          type="button"
          @click="setStatusFilter(item.value)"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section class="table-card">
      <div class="table-headline">
        <div>
          <h2>账号明细</h2>
          <span>{{ judges.length }} / {{ totalCount }}</span>
        </div>
        <strong v-if="loading">加载中</strong>
        <strong v-else-if="keyword || statusFilter !== 'ALL'">已筛选</strong>
      </div>

      <div class="judge-table">
        <div class="table-head">
          <span>评审</span>
          <span>联系方式</span>
          <span>资质</span>
          <span>利益关系</span>
          <span>状态</span>
          <span>操作</span>
        </div>
        <div class="table-body">
          <div v-for="judge in judges" :key="judge.publicId" class="table-row">
            <div class="judge-cell">
              <span class="avatar">{{ getInitial(judge.name) }}</span>
              <div>
                <strong>{{ judge.name || '未填写姓名' }}</strong>
                <small>{{ judge.statusLabel || statusLabel(judge.status) }}</small>
              </div>
            </div>
            <div class="contact-cell">
              <span>{{ judge.maskedPhone || '-' }}</span>
              <small>{{ judge.maskedWechat || '未填写微信号' }}</small>
            </div>
            <span class="qualification">{{ judge.qualification || '未填写资质' }}</span>
            <div class="conflict-cell">
              <span :class="['conflict-badge', hasBreweryConflict(judge) ? 'warning' : 'clear']">
                {{ hasBreweryConflict(judge) ? '需回避' : '无' }}
              </span>
              <small v-if="hasBreweryConflict(judge)" :title="judge.breweryConflictText">
                {{ judge.breweryConflictText }}
              </small>
            </div>
            <span :class="['status-badge', statusTone(judge)]">
              {{ judge.statusLabel || statusLabel(judge.status) }}
            </span>
            <div class="row-actions">
              <button class="row-action" type="button" @click="openEditor(judge)">编辑</button>
              <button
                v-if="Number(judge.status) === 2"
                class="row-action success"
                type="button"
                @click="changeStatus(judge, 1)"
              >
                通过
              </button>
              <button
                v-if="Number(judge.status) !== 0"
                class="row-action danger"
                type="button"
                @click="changeStatus(judge, 0)"
              >
                停用
              </button>
              <button
                v-if="Number(judge.status) === 0"
                class="row-action success"
                type="button"
                @click="changeStatus(judge, 1)"
              >
                启用
              </button>
              <button
                v-if="isActive(judge)"
                class="row-action"
                type="button"
                @click="router.push('/admin/assignments')"
              >
                编排
                <Right />
              </button>
            </div>
          </div>
          <div v-if="!loading && judges.length === 0" class="empty-state">
            <h2>没有匹配的评审</h2>
            <p>调整搜索条件或状态筛选后再查看。</p>
          </div>
        </div>
      </div>
      <footer class="pagination-bar">
        <span>共 {{ totalCount }} 条</span>
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
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
      </footer>
    </section>

    <div v-if="editorOpen" class="modal-mask" @click.self="closeEditor">
      <section class="modal-card">
        <header>
          <h2>编辑评审资料</h2>
          <button class="icon-close" type="button" @click="closeEditor">×</button>
        </header>
        <p class="privacy-hint">完整联系方式仅供内部联络和资料维护使用，系统会记录查看和修改操作。</p>
        <label>
          <span>手机号</span>
          <div class="readonly-row">
            <input v-model.trim="editForm.phone" readonly />
            <button class="row-action" type="button" @click="openPhoneEditor">更正</button>
          </div>
        </label>
        <label>
          <span>姓名</span>
          <input v-model.trim="editForm.name" />
        </label>
        <label>
          <span>微信号</span>
          <input v-model.trim="editForm.wechat" />
        </label>
        <label>
          <span>资质信息</span>
          <textarea v-model.trim="editForm.qualification"></textarea>
        </label>
        <label>
          <span>是否与酒厂有利益关联</span>
          <div class="conflict-toggle">
            <button :class="{ active: !editForm.breweryConflictFlag }" type="button" @click="setEditorBreweryConflict(false)">无</button>
            <button :class="{ active: editForm.breweryConflictFlag }" type="button" @click="setEditorBreweryConflict(true)">有</button>
          </div>
        </label>
        <label v-if="editForm.breweryConflictFlag">
          <span>相关酒厂或品牌名称及关系说明</span>
          <textarea v-model.trim="editForm.breweryConflictText" maxlength="500"></textarea>
        </label>
        <footer>
          <button class="tool-button" type="button" @click="closeEditor">取消</button>
          <button class="tool-button primary" type="button" @click="saveEditor">保存</button>
        </footer>
      </section>
    </div>

    <div v-if="phoneEditorOpen" class="modal-mask" @click.self="closePhoneEditor">
      <section class="modal-card">
        <header>
          <h2>更正手机号</h2>
          <button class="icon-close" type="button" @click="closePhoneEditor">×</button>
        </header>
        <p class="privacy-hint">手机号是评审登录身份。仅在录入错误且评审尚未分配比赛时更正。</p>
        <label>
          <span>新手机号</span>
          <input v-model.trim="phoneForm.phone" inputmode="tel" />
        </label>
        <label>
          <span>更正原因</span>
          <textarea v-model.trim="phoneForm.reason"></textarea>
        </label>
        <footer>
          <button class="tool-button" type="button" @click="closePhoneEditor">取消</button>
          <button class="tool-button primary" type="button" @click="savePhoneEditor">保存</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Refresh, Right, Search } from '@element-plus/icons-vue'
import { fetchJudgeDetail, fetchJudgesPage, updateJudge, updateJudgePhone, updateJudgeStatus } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const judges = ref([])
const keyword = ref('')
const statusFilter = ref('ALL')
const loading = ref(false)
const totalCount = ref(0)
const searchTimer = ref(null)
const editorOpen = ref(false)
const phoneEditorOpen = ref(false)
const editingJudgePublicId = ref(null)
const editForm = reactive({
  phone: '',
  name: '',
  wechat: '',
  qualification: '',
  breweryConflictFlag: false,
  breweryConflictText: '',
  reviewRemark: '',
})
const phoneForm = reactive({
  phone: '',
  reason: '',
})

const statusFilters = [
  { label: '全部', value: 'ALL' },
  { label: '待审核', value: 'PENDING' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' },
]

const pagination = reactive({
  page: 1,
  pageSize: 20,
})

const totalPages = computed(() => Math.max(Math.ceil(totalCount.value / pagination.pageSize), 1))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, pagination.page - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})

watch(() => route.query.keyword, (value) => {
  const nextKeyword = value ? String(value) : ''
  if (keyword.value === nextKeyword) return
  keyword.value = nextKeyword
  pagination.page = 1
  loadJudges()
})

onMounted(loadJudges)

if (route.query.keyword) {
  keyword.value = String(route.query.keyword)
}

async function loadJudges() {
  loading.value = true
  try {
    const data = await fetchJudgesPage({
      status: statusApiValue(statusFilter.value),
      keyword: keyword.value || undefined,
      page: pagination.page,
      pageSize: pagination.pageSize,
    })
    judges.value = data.records || []
    totalCount.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function scheduleLoadJudges() {
  if (searchTimer.value) window.clearTimeout(searchTimer.value)
  searchTimer.value = window.setTimeout(() => {
    loadFirstPage()
  }, 320)
}

function loadFirstPage() {
  pagination.page = 1
  loadJudges()
}

function setStatusFilter(value) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  loadFirstPage()
}

function statusApiValue(value) {
  if (value === 'PENDING') return 2
  if (value === 'ACTIVE') return 1
  if (value === 'DISABLED') return 0
  return undefined
}

function changePage(page) {
  const next = Math.min(Math.max(page, 1), totalPages.value)
  if (next === pagination.page) return
  pagination.page = next
  loadJudges()
}

function changePageSize() {
  pagination.page = 1
  loadJudges()
}

function isActive(judge) {
  return Number(judge.status) === 1
}

function statusLabel(status) {
  const map = {
    0: '停用',
    1: '启用',
    2: '待审核',
    3: '资料未完善',
  }
  return map[Number(status)] || '未知'
}

function statusTone(judge) {
  const status = Number(judge.status)
  if (status === 1) return 'active'
  if (status === 2) return 'pending'
  return 'inactive'
}

async function openEditor(judge) {
  const detail = await fetchJudgeDetail(judge.publicId)
  editingJudgePublicId.value = detail.publicId
  editForm.phone = detail.phone || ''
  editForm.name = detail.name || ''
  editForm.wechat = detail.wechat || ''
  editForm.qualification = detail.qualification || ''
  editForm.breweryConflictFlag = Boolean(detail.breweryConflictFlag)
  editForm.breweryConflictText = detail.breweryConflictText || ''
  editForm.reviewRemark = detail.reviewRemark || ''
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
  editingJudgePublicId.value = null
}

async function saveEditor() {
  if (editForm.breweryConflictFlag && !editForm.breweryConflictText) {
    ElMessage.warning('请填写相关酒厂或品牌名称及关系说明')
    return
  }
  await updateJudge(editingJudgePublicId.value, {
    name: editForm.name,
    wechat: editForm.wechat,
    qualification: editForm.qualification,
    breweryConflictFlag: editForm.breweryConflictFlag,
    breweryConflictText: editForm.breweryConflictFlag ? editForm.breweryConflictText : '',
    reviewRemark: editForm.reviewRemark,
  })
  ElMessage.success('评审资料已更新')
  closeEditor()
  await loadJudges()
}

async function changeStatus(judge, status) {
  await updateJudgeStatus(judge.publicId, { status })
  ElMessage.success(status === 1 ? '评审已启用' : '评审已停用')
  await loadJudges()
}

function getInitial(name) {
  return name?.trim()?.slice(0, 1) || '评'
}

function hasBreweryConflict(judge) {
  return Boolean(judge?.breweryConflictFlag && judge?.breweryConflictText)
}

function setEditorBreweryConflict(value) {
  editForm.breweryConflictFlag = value
  if (!value) editForm.breweryConflictText = ''
}

function openPhoneEditor() {
  phoneForm.phone = editForm.phone || ''
  phoneForm.reason = ''
  phoneEditorOpen.value = true
}

function closePhoneEditor() {
  phoneEditorOpen.value = false
}

async function savePhoneEditor() {
  await ElMessageBox.confirm('更正后评审需使用新手机号登录。确认保存吗？', '确认更正手机号', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    type: 'warning',
  })
  const next = await updateJudgePhone(editingJudgePublicId.value, phoneForm)
  editForm.phone = next.phone || ''
  ElMessage.success('手机号已更正')
  closePhoneEditor()
  await loadJudges()
}
</script>

<style scoped>
.judges-page {
  --panel: rgba(22, 32, 36, 0.9);
  --panel-strong: rgba(26, 39, 44, 0.96);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --orange: #f2994a;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 28px;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 17% 7%, rgba(216, 169, 53, 0.13), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
  overflow: hidden;
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

svg {
  width: 1em;
  height: 1em;
}

.page-head,
.head-actions,
.tool-button,
.toolbar,
.search-box,
.filter-tabs,
.table-headline > div,
.judge-cell,
.row-action {
  display: flex;
  align-items: center;
}

.page-head {
  flex: 0 0 auto;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--line);
}

.page-head h1 {
  font-size: 30px;
  line-height: 1.1;
}

.head-actions {
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tool-button,
.row-action,
.filter-tabs button {
  min-height: 42px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.tool-button {
  gap: 8px;
  padding: 0 12px;
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.toolbar,
.table-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.table-head,
.judge-cell small,
.contact-cell small,
.conflict-cell small,
.empty-state p,
.table-headline span {
  color: var(--muted);
}

.toolbar {
  flex: 0 0 auto;
  justify-content: space-between;
  gap: 16px;
  margin-top: 22px;
  padding: 14px 16px;
}

.search-box {
  flex: 1;
  max-width: 480px;
  gap: 10px;
  min-height: 46px;
  padding: 0 12px;
  color: var(--muted);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
}

.search-box input {
  width: 100%;
  min-width: 0;
  color: var(--text);
  border: 0;
  outline: 0;
  background: transparent;
}

.search-box input::placeholder {
  color: var(--faint);
}

.filter-tabs {
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tabs button {
  padding: 0 14px;
  color: #a9bbc2;
}

.filter-tabs button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.table-card {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 18px;
  padding: 16px;
}

.table-headline {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.table-headline > div {
  gap: 10px;
}

.table-headline strong {
  color: var(--gold-soft);
}

.judge-table {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pagination-bar {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  min-height: 48px;
  margin-top: 12px;
  padding-top: 12px;
  color: var(--muted);
  border-top: 1px solid var(--line);
}

.pager-buttons {
  display: flex;
  gap: 6px;
  align-items: center;
}

.pagination-bar button,
.pagination-bar select {
  min-height: 32px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.035);
}

.pagination-bar button {
  min-width: 34px;
  padding: 0 10px;
}

.pagination-bar button.active {
  color: var(--gold-soft);
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

.table-body {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  align-content: start;
  gap: 8px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.table-body::-webkit-scrollbar {
  width: 10px;
}

.table-body::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.table-body::-webkit-scrollbar-thumb {
  border: 2px solid rgba(22, 32, 36, 0.95);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.28);
}

.table-body::-webkit-scrollbar-thumb:hover {
  background: rgba(216, 169, 53, 0.42);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(200px, 1.1fr) minmax(180px, 0.9fr) minmax(220px, 1.1fr) minmax(150px, 0.8fr) 86px minmax(250px, auto);
  gap: 12px;
  align-items: center;
}

.table-head {
  padding: 0 12px;
  font-size: 13px;
}

.table-row {
  min-width: 0;
  padding: 13px 12px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  transition: border-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
}

.table-row:hover {
  border-color: rgba(216, 169, 53, 0.2);
  background: rgba(255, 255, 255, 0.04);
  transform: translateY(-1px);
}

.judge-cell {
  gap: 12px;
  min-width: 0;
}

.judge-cell strong,
.contact-cell span,
.qualification,
.conflict-cell small {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.judge-cell small,
.contact-cell small,
.conflict-cell small {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 38px;
  height: 38px;
  color: var(--gold-soft);
  font-weight: 800;
  border: 1px solid rgba(216, 169, 53, 0.26);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.09);
}

.status-badge {
  display: inline-flex;
  justify-content: center;
  width: fit-content;
  min-width: 56px;
  padding: 7px 10px;
  font-weight: 800;
  border-radius: 8px;
}

.conflict-cell {
  min-width: 0;
}

.conflict-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  width: fit-content;
  padding: 0 9px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 850;
}

.conflict-badge.clear {
  color: #8da1aa;
  border: 1px solid rgba(219, 232, 237, 0.12);
  background: rgba(255, 255, 255, 0.035);
}

.conflict-badge.warning {
  color: #f1bd79;
  border: 1px solid rgba(242, 153, 74, 0.24);
  background: rgba(242, 153, 74, 0.1);
}

.status-badge.active {
  color: var(--green);
  border: 1px solid rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.1);
}

.status-badge.inactive {
  color: #f1bd79;
  border: 1px solid rgba(242, 153, 74, 0.24);
  background: rgba(242, 153, 74, 0.09);
}

.status-badge.pending {
  color: #f7d774;
  border: 1px solid rgba(247, 215, 116, 0.24);
  background: rgba(247, 215, 116, 0.09);
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.row-action {
  justify-content: center;
  gap: 7px;
  padding: 0 10px;
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.24);
  background: rgba(216, 169, 53, 0.06);
}

.row-action.success {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.24);
  background: rgba(111, 207, 122, 0.08);
}

.row-action.danger {
  color: #ffb4a8;
  border-color: rgba(255, 180, 168, 0.24);
  background: rgba(255, 180, 168, 0.08);
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(5, 12, 16, 0.72);
  backdrop-filter: blur(10px);
}

.modal-card {
  width: min(100%, 520px);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  padding: 20px;
  color: var(--text);
  background: rgba(22, 32, 36, 0.98);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.36);
}

.modal-card header,
.modal-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.modal-card h2 {
  margin: 0;
}

.privacy-hint {
  margin: 12px 0 0;
  color: var(--muted);
  line-height: 1.5;
  font-size: 13px;
}

.readonly-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.conflict-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.conflict-toggle button {
  min-height: 40px;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font-weight: 800;
}

.conflict-toggle button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.08);
}

.icon-close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text);
  background: rgba(255, 255, 255, 0.04);
  font-size: 22px;
}

.modal-card label {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.modal-card input,
.modal-card textarea {
  width: 100%;
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  padding: 11px 12px;
  color: var(--text);
  background: rgba(255, 255, 255, 0.05);
  outline: none;
}

.modal-card textarea {
  min-height: 104px;
  resize: vertical;
}

.modal-card footer {
  justify-content: flex-end;
  margin-top: 18px;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 220px;
  text-align: center;
}

@media (max-width: 1260px) {
  .table-head,
  .table-row {
    grid-template-columns: minmax(190px, 1fr) minmax(170px, 0.9fr) minmax(180px, 1fr) minmax(130px, 0.8fr) 80px minmax(220px, auto);
  }
}

@media (max-width: 980px) {
  .judges-page {
    padding: 22px 16px;
  }

  .page-head,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .search-box {
    max-width: none;
  }

  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
  }

  .row-action {
    width: fit-content;
  }

  .row-actions {
    justify-content: flex-start;
  }
}
</style>
