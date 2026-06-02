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
        <input v-model.trim="keyword" placeholder="搜索姓名、手机号后四位、资质" />
      </label>
      <div class="filter-tabs" aria-label="评审状态筛选">
        <button
          v-for="item in statusFilters"
          :key="item.value"
          :class="{ active: statusFilter === item.value }"
          type="button"
          @click="statusFilter = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section class="table-card">
      <div class="table-headline">
        <div>
          <h2>账号明细</h2>
          <span>{{ filteredJudges.length }} / {{ totalCount }}</span>
        </div>
        <strong v-if="loading">加载中</strong>
        <strong v-else-if="keyword || statusFilter !== 'ALL'">已筛选</strong>
      </div>

      <div class="judge-table">
        <div class="table-head">
          <span>评审</span>
          <span>联系方式</span>
          <span>资质</span>
          <span>状态</span>
          <span>操作</span>
        </div>
        <div v-for="judge in filteredJudges" :key="judge.publicId" class="table-row">
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
      </div>

      <div v-if="!loading && filteredJudges.length === 0" class="empty-state">
        <h2>没有匹配的评审</h2>
        <p>调整搜索条件或状态筛选后再查看。</p>
      </div>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Refresh, Right, Search } from '@element-plus/icons-vue'
import { fetchJudgeDetail, fetchJudges, updateJudge, updateJudgePhone, updateJudgeStatus } from '@/api/admin'

const router = useRouter()
const judges = ref([])
const keyword = ref('')
const statusFilter = ref('ALL')
const loading = ref(false)
const editorOpen = ref(false)
const phoneEditorOpen = ref(false)
const editingJudgePublicId = ref(null)
const editForm = reactive({
  phone: '',
  name: '',
  wechat: '',
  qualification: '',
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

const totalCount = computed(() => judges.value.length)
const filteredJudges = computed(() => {
  const query = keyword.value.toLowerCase()
  return judges.value.filter((judge) => {
    const matchesStatus =
      statusFilter.value === 'ALL' ||
      (statusFilter.value === 'PENDING' && Number(judge.status) === 2) ||
      (statusFilter.value === 'ACTIVE' && isActive(judge)) ||
      (statusFilter.value === 'DISABLED' && Number(judge.status) === 0)
    const matchesKeyword =
      !query ||
      [judge.name, judge.maskedPhone, judge.qualification]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query))
    return matchesStatus && matchesKeyword
  })
})

onMounted(loadJudges)

async function loadJudges() {
  loading.value = true
  try {
    judges.value = await fetchJudges()
  } finally {
    loading.value = false
  }
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
  editForm.reviewRemark = detail.reviewRemark || ''
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
  editingJudgePublicId.value = null
}

async function saveEditor() {
  await updateJudge(editingJudgePublicId.value, {
    name: editForm.name,
    wechat: editForm.wechat,
    qualification: editForm.qualification,
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
  min-height: 100vh;
  padding: 28px;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 17% 7%, rgba(216, 169, 53, 0.13), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
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
.empty-state p,
.table-headline span {
  color: var(--muted);
}

.toolbar {
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
  margin-top: 18px;
  padding: 16px;
}

.table-headline {
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
  display: grid;
  gap: 8px;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) minmax(200px, 1fr) minmax(260px, 1.25fr) 96px minmax(260px, auto);
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
.qualification {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.judge-cell small,
.contact-cell small {
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
    grid-template-columns: minmax(220px, 1.1fr) minmax(190px, 1fr) minmax(180px, 1fr) 88px minmax(220px, auto);
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
