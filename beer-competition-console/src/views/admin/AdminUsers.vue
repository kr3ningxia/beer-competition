<template>
  <div class="admin-users-page">
    <AdminPageHeader title="管理员账号">
      <template #actions>
        <button class="tool-button primary" type="button" @click="openCreateEditor">
          <Plus />
          新增管理员
        </button>
      </template>
    </AdminPageHeader>

    <section class="toolbar">
      <label class="search-box">
        <Search />
        <input v-model.trim="keyword" placeholder="搜索登录账号、管理员姓名" @input="scheduleLoadUsers" @keyup.enter="loadFirstPage" />
      </label>
      <div class="filter-group">
        <el-select
          v-model="adminTypeFilter"
          class="filter-select"
          popper-class="admin-users-select-popper"
          @change="loadUsers"
        >
          <el-option label="全部类型" value="ALL" />
          <el-option
            v-for="option in typeFilterOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-select
          v-model="statusFilter"
          class="filter-select"
          popper-class="admin-users-select-popper"
          @change="loadUsers"
        >
          <el-option
            v-for="item in statusFilters"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
    </section>

    <section class="table-card">
      <div class="table-headline">
        <div>
          <h2>账号明细</h2>
          <span>{{ users.length }} 个账号</span>
        </div>
        <strong v-if="loading">加载中</strong>
        <strong v-else-if="keyword || statusFilter !== 'ACTIVE' || adminTypeFilter !== 'ALL'">已筛选</strong>
      </div>

      <div class="admin-table">
        <div class="table-head">
          <span>管理员</span>
          <span>管理员类型</span>
          <span>登录账号</span>
          <span>状态</span>
          <span>创建时间</span>
          <span>更新时间</span>
          <span>操作</span>
        </div>
        <div class="table-body">
          <div v-for="item in users" :key="item.id" :class="['table-row', { current: item.currentUser }]">
            <div class="admin-cell">
              <div>
                <strong>{{ item.name || '未命名管理员' }}</strong>
                <small v-if="item.currentUser">当前登录账号</small>
              </div>
            </div>
            <span class="type-cell">{{ getAdminTypeLabel(item.adminType) }}</span>
            <span class="code-cell">{{ item.username }}</span>
            <span :class="['status-badge', accountStatusClass(item)]">
              {{ accountStatusLabel(item) }}
            </span>
            <span class="time-cell">{{ formatTime(item.createTime) }}</span>
            <span class="time-cell">{{ formatTime(item.updateTime) }}</span>
            <div class="row-actions">
              <button class="row-action" type="button" @click="openNameEditor(item)">编辑</button>
              <button v-if="!item.currentUser" class="row-action" type="button" @click="openPasswordReset(item)">重置密码</button>
              <button v-else class="row-action" type="button" @click="goToMyAccount">修改密码</button>
              <button
                v-if="Number(item.status) === 1"
                class="row-action danger"
                type="button"
                :disabled="item.currentUser"
                :title="item.currentUser ? '不能停用当前登录账号' : '停用账号'"
                @click="changeStatus(item, 0)"
              >
                停用
              </button>
              <button
                v-else
                class="row-action success"
                type="button"
                @click="changeStatus(item, 1)"
              >
                启用
              </button>
            </div>
          </div>
          <div v-if="!loading && users.length === 0" class="empty-state">
            <h2>没有匹配的管理员</h2>
            <p>调整筛选条件或新增一个后台账号</p>
          </div>
        </div>
      </div>
    </section>

    <div v-if="editorOpen" class="modal-mask" @click.self="closeEditor">
      <section class="modal-card">
        <header>
          <h2>{{ editorMode === 'create' ? '新增管理员' : '编辑管理员' }}</h2>
          <button class="icon-close" type="button" @click="closeEditor">×</button>
        </header>
        <label v-if="editorMode === 'create'">
          <span>登录账号</span>
          <input v-model.trim="form.username" autocomplete="off" placeholder="例如 event_admin" />
        </label>
        <label>
          <span>管理员姓名</span>
          <input v-model.trim="form.name" autocomplete="off" placeholder="用于操作记录和后台显示" />
        </label>
        <label v-if="editorMode === 'create'">
          <span>初始密码</span>
          <input v-model.trim="form.password" type="password" autocomplete="new-password" placeholder="至少 6 位" />
        </label>
        <div v-if="typeOptions.length" class="type-field">
          <span>管理员类型</span>
          <div class="segmented" role="radiogroup" aria-label="管理员类型">
            <button
              v-for="option in typeOptions"
              :key="option.value"
              class="segmented-item"
              :class="{ active: form.adminType === option.value }"
              type="button"
              role="radio"
              :aria-checked="form.adminType === option.value"
              :disabled="typeLocked"
              @click="form.adminType = option.value"
            >
              {{ option.label }}
            </button>
          </div>
          <small v-if="typeLocked" class="field-note">不能修改自己的管理员类型</small>
        </div>
        <footer>
          <button class="tool-button" type="button" @click="closeEditor">取消</button>
          <button class="tool-button primary" type="button" :disabled="saving" @click="saveEditor">保存</button>
        </footer>
      </section>
    </div>

    <div v-if="passwordOpen" class="modal-mask" @click.self="closePasswordEditor">
      <section class="modal-card">
        <header>
          <h2>重置管理员密码</h2>
          <button class="icon-close" type="button" @click="closePasswordEditor">×</button>
        </header>
        <p class="hint-line">
          重置后「{{ passwordTarget?.name || passwordTarget?.username }}」下次登录将直接使用此密码。
        </p>
        <label>
          <span>新密码</span>
          <input v-model.trim="passwordForm.newPassword" type="password" autocomplete="new-password" placeholder="6 到 32 位" />
        </label>
        <label>
          <span>确认新密码</span>
          <input v-model.trim="passwordForm.confirmPassword" type="password" autocomplete="new-password" />
        </label>
        <footer>
          <button class="tool-button" type="button" @click="closePasswordEditor">取消</button>
          <button class="tool-button primary" type="button" :disabled="saving" @click="savePassword">保存</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import {
  createAdminUser,
  fetchAdminUsers,
  resetAdminUserPassword,
  updateAdminUser,
  updateAdminUserStatus,
} from '@/api/admin'
import { getAdminMe } from '@/api/auth'
import { getAdminType, setSession } from '@/utils/auth'
import { ADMIN_TYPES, getAdminTypeLabel } from '@/config/adminAccess'

const route = useRoute()
const router = useRouter()
const users = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('ACTIVE')
const adminTypeFilter = ref('ALL')
const searchTimer = ref(null)
const editorOpen = ref(false)
const editorMode = ref('create')
const editingUser = ref(null)
const passwordOpen = ref(false)
const passwordTarget = ref(null)

const PLATFORM_SIDE_TYPES = [ADMIN_TYPES.PLATFORM_SUPER_ADMIN, ADMIN_TYPES.PLATFORM_EVENT_ADMIN]
const ORGANIZER_SIDE_TYPES = [ADMIN_TYPES.ORGANIZER_ADMIN, ADMIN_TYPES.ORGANIZER_SUB_ADMIN]

function toTypeOptions(types) {
  return types.map((value) => ({ value, label: getAdminTypeLabel(value) }))
}

const typeOptions = computed(() => {
  const referenceType = editorMode.value === 'edit' ? editingUser.value?.adminType : getAdminType()
  const sideTypes = PLATFORM_SIDE_TYPES.includes(referenceType) ? PLATFORM_SIDE_TYPES : ORGANIZER_SIDE_TYPES
  return sideTypes.includes(referenceType) ? toTypeOptions(sideTypes) : []
})

const typeLocked = computed(() => editorMode.value === 'edit' && Boolean(editingUser.value?.currentUser))

const typeFilterOptions = computed(() => {
  const referenceType = getAdminType()
  const sideTypes = PLATFORM_SIDE_TYPES.includes(referenceType) ? PLATFORM_SIDE_TYPES : ORGANIZER_SIDE_TYPES
  return toTypeOptions(sideTypes)
})

const form = reactive({
  username: '',
  name: '',
  password: '',
  adminType: '',
})

const passwordForm = reactive({
  newPassword: '',
  confirmPassword: '',
})

const statusFilters = [
  { label: '全部', value: 'ALL' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' },
]

watch(() => route.query.keyword, (value) => {
  const nextKeyword = value ? String(value) : ''
  if (keyword.value === nextKeyword) return
  keyword.value = nextKeyword
  loadUsers()
})

if (route.query.keyword) {
  keyword.value = String(route.query.keyword)
}

onMounted(initializePage)

async function initializePage() {
  try {
    const me = await getAdminMe()
    setSession('admin', me)
    if (me?.mustChangePassword || me?.mustChangeUsername) {
      await router.replace('/admin/account-setup')
      return
    }
  } catch {
    return
  }
  await loadUsers()
}

async function loadUsers() {
  loading.value = true
  try {
    users.value = await fetchAdminUsers({
      status: statusApiValue(statusFilter.value),
      adminType: adminTypeFilter.value === 'ALL' ? undefined : adminTypeFilter.value,
      keyword: keyword.value || undefined,
    }) || []
  } finally {
    loading.value = false
  }
}

function scheduleLoadUsers() {
  if (searchTimer.value) window.clearTimeout(searchTimer.value)
  searchTimer.value = window.setTimeout(loadFirstPage, 260)
}

function loadFirstPage() {
  loadUsers()
}

function statusApiValue(value) {
  if (value === 'ACTIVE') return 1
  if (value === 'DISABLED') return 0
  return undefined
}

function openCreateEditor() {
  editorMode.value = 'create'
  editingUser.value = null
  form.username = ''
  form.name = ''
  form.password = ''
  form.adminType = defaultCreateType()
  editorOpen.value = true
}

/** 平台侧默认赛事管理员、主办方侧默认主管理员，避免默认授予最高权限。 */
function defaultCreateType() {
  const options = typeOptions.value
  if (!options.length) return ''
  const preferred = getAdminType() === ADMIN_TYPES.ORGANIZER_ADMIN
    ? ADMIN_TYPES.ORGANIZER_ADMIN
    : ADMIN_TYPES.PLATFORM_EVENT_ADMIN
  return options.some((option) => option.value === preferred) ? preferred : options[0].value
}

function openNameEditor(user) {
  editorMode.value = 'edit'
  editingUser.value = user
  form.username = user.username || ''
  form.name = user.name || ''
  form.password = ''
  form.adminType = user.adminType || ''
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
  editingUser.value = null
}

async function saveEditor() {
  if (!form.name) {
    ElMessage.warning('请输入管理员姓名')
    return
  }
  if (editorMode.value === 'create' && (!form.username || !form.password)) {
    ElMessage.warning('请填写登录账号和初始密码')
    return
  }
  saving.value = true
  try {
    if (editorMode.value === 'create') {
      await createAdminUser({
        username: form.username,
        name: form.name,
        password: form.password,
        ...(form.adminType ? { adminType: form.adminType } : {}),
      })
      ElMessage.success('管理员账号已新增')
    } else {
      await updateAdminUser(editingUser.value.id, {
        name: form.name,
        ...(form.adminType ? { adminType: form.adminType } : {}),
      })
      ElMessage.success('管理员信息已更新')
    }
    closeEditor()
    await loadUsers()
  } finally {
    saving.value = false
  }
}

async function changeStatus(user, status) {
  if (user.currentUser && status === 0) {
    ElMessage.warning('不能停用当前登录账号')
    return
  }
  const title = status === 1 ? '启用管理员账号' : '停用管理员账号'
  const actionText = status === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${actionText}「${user.name || user.username}」吗？`, title, {
      confirmButtonText: actionText,
      cancelButtonText: '取消',
      type: status === 1 ? 'info' : 'warning',
    })
  } catch {
    return
  }
  await updateAdminUserStatus(user.id, { status })
  ElMessage.success(`账号已${actionText}`)
  await loadUsers()
}

function goToMyAccount() {
  router.push('/admin/account')
}

function openPasswordReset(user) {
  passwordTarget.value = user
  resetPasswordForm()
  passwordOpen.value = true
}

function closePasswordEditor() {
  passwordOpen.value = false
  passwordTarget.value = null
}

function resetPasswordForm() {
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function savePassword() {
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 32) {
    ElMessage.warning('密码长度需为 6 到 32 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  const targetLabel = passwordTarget.value?.name || passwordTarget.value?.username
  try {
    await ElMessageBox.confirm(
      `重置后「${targetLabel}」下次登录将直接使用此密码。`,
      '重置管理员密码',
      { confirmButtonText: '重置密码', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  saving.value = true
  try {
    await resetAdminUserPassword(passwordTarget.value.id, {
      password: passwordForm.newPassword,
    })
    ElMessage.success('密码已重置')
    closePasswordEditor()
  } finally {
    saving.value = false
  }
}

function accountStatusClass(user) {
  if (Number(user.status) !== 1) return 'inactive'
  return user.mustChangeUsername || user.mustChangePassword ? 'pending' : 'active'
}

function accountStatusLabel(user) {
  if (Number(user.status) !== 1) return '停用'
  return user.mustChangeUsername || user.mustChangePassword ? '待首次设置' : '启用'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.admin-users-page {
  --panel: rgba(22, 32, 36, 0.9);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --danger: #ffb4a8;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 0 28px 18px;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(216, 169, 53, 0.13), transparent 18rem),
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

button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

svg {
  width: 1em;
  height: 1em;
}

.tool-button,
.toolbar,
.search-box,
.filter-group,
.table-headline > div,
.admin-cell,
.row-action {
  display: flex;
  align-items: center;
}

.tool-button,
.row-action {
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

.filter-group {
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.filter-select {
  width: 170px;
}

.filter-select :deep(.el-select__wrapper) {
  min-height: 46px;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
  box-shadow: none;
}

.filter-select :deep(.el-select__wrapper:hover),
.filter-select :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(216, 169, 53, 0.32);
}

.filter-select :deep(.el-select__selected-item) {
  color: var(--text);
}

.filter-select :deep(.el-select__placeholder) {
  color: var(--faint);
}

.filter-select :deep(.el-select__caret) {
  color: var(--muted);
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

.table-headline span,
.table-head,
.admin-cell small,
.type-cell,
.time-cell,
.empty-state p {
  color: var(--muted);
}

.table-headline strong {
  color: var(--gold-soft);
}

.admin-table,
.table-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.table-body {
  display: grid;
  align-content: start;
  gap: 8px;
  overflow-y: auto;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.table-body::-webkit-scrollbar {
  width: 10px;
}

.table-body::-webkit-scrollbar-thumb {
  border: 2px solid rgba(22, 32, 36, 0.95);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.28);
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(150px, 0.9fr) minmax(155px, 0.9fr) 96px minmax(160px, 0.9fr) minmax(160px, 0.9fr) minmax(286px, auto);
  gap: 12px;
  align-items: center;
}

.table-head {
  padding: 0 12px 8px;
  font-size: 13px;
}

.table-row {
  padding: 13px 12px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  transition: border-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
}

.table-row.current {
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.055);
}

.table-row:hover {
  border-color: rgba(216, 169, 53, 0.2);
  background: rgba(255, 255, 255, 0.04);
  transform: translateY(-1px);
}

.admin-cell {
  min-width: 0;
}

.admin-cell strong,
.admin-cell small,
.type-cell,
.code-cell,
.time-cell {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  justify-content: center;
  width: fit-content;
  min-width: 56px;
  padding: 7px 10px;
  font-weight: 850;
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
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.1);
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
  color: var(--danger);
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
  width: min(100%, 500px);
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
  font-weight: 850;
}

.modal-card input {
  width: 100%;
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  padding: 11px 12px;
  color: var(--text);
  background: rgba(255, 255, 255, 0.05);
  outline: none;
}

.type-field {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 850;
}

.segmented {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  gap: 4px;
  padding: 4px;
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
}

.segmented-item {
  min-height: 38px;
  padding: 0 10px;
  color: #a9bbc2;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: color 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.segmented-item:hover:not(:disabled) {
  color: var(--text);
  background: rgba(255, 255, 255, 0.04);
}

.segmented-item.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.segmented-item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.field-note {
  color: var(--faint);
  font-size: 12px;
  font-weight: 400;
}

.hint-line {
  margin-top: 12px;
  color: var(--muted);
  line-height: 1.5;
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

@media (max-width: 1280px) {
  .table-head,
  .table-row {
    grid-template-columns: 1fr;
  }

  .table-head {
    display: none;
  }

  .table-row {
    align-items: flex-start;
  }

  .row-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 980px) {
  .admin-users-page {
    padding: 0 16px 16px;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    max-width: none;
  }

  .filter-group {
    justify-content: flex-start;
  }
}
</style>

<style>
.admin-users-select-popper.el-popper {
  border: 1px solid rgba(218, 232, 237, 0.14);
  background: #111c20;
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.38);
}

.admin-users-select-popper .el-select-dropdown,
.admin-users-select-popper .el-select-dropdown__list {
  background: #111c20;
}

.admin-users-select-popper .el-select-dropdown__item {
  color: #c7d6dc;
}

.admin-users-select-popper .el-select-dropdown__item.hover,
.admin-users-select-popper .el-select-dropdown__item.is-hovering,
.admin-users-select-popper .el-select-dropdown__item:hover {
  color: #ffdc73;
  background: rgba(216, 169, 53, 0.1);
}

.admin-users-select-popper .el-select-dropdown__item.selected,
.admin-users-select-popper .el-select-dropdown__item.is-selected {
  color: #ffdc73;
  font-weight: 800;
}
</style>
