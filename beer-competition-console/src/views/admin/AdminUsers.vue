<template>
  <div :class="['admin-users-page', { 'setup-mode': setupMode, 'account-only': accountOnly }]">
    <AdminPageHeader v-if="!setupMode" :title="accountOnly ? '账号设置' : '管理员账号'">
      <template v-if="!accountOnly" #actions>
        <button class="tool-button" type="button" @click="toggleMyAccount">
          <Key />
          我的账号
        </button>
        <button class="tool-button primary" type="button" @click="openCreateEditor">
          <Plus />
          新增管理员
        </button>
      </template>
    </AdminPageHeader>

    <section :class="['my-account-panel', { expanded: myAccountExpanded, required: setupMode }]">
      <button class="my-account-toggle" type="button" :aria-expanded="myAccountExpanded" :disabled="setupMode || accountOnly" @click="toggleMyAccount">
        <span class="my-account-icon"><UserFilled /></span>
        <span class="my-account-copy">
          <strong>{{ setupMode ? '设置登录账号与密码' : '登录账号与密码' }}</strong>
          <small v-if="!setupMode">{{ `${currentUser.username || '当前账号'} · ${currentUser.displayName || '管理员'}` }}</small>
        </span>
        <ArrowDown v-if="!setupMode" :class="['toggle-arrow', { rotated: myAccountExpanded }]" />
      </button>
      <div v-if="myAccountExpanded" class="my-account-body">
        <div class="credential-grid">
          <label>
            <span>登录账号</span>
            <input v-model.trim="credentialForm.username" :disabled="!setupMode && credentialSaving" autocomplete="username" />
          </label>
          <label v-if="!setupMode">
            <span>当前密码</span>
            <input v-model.trim="credentialForm.oldPassword" type="password" autocomplete="current-password" />
          </label>
          <label>
            <span>新密码</span>
            <input v-model.trim="credentialForm.newPassword" type="password" autocomplete="new-password" />
          </label>
          <label>
            <span>确认新密码</span>
            <input v-model.trim="credentialForm.confirmPassword" type="password" autocomplete="new-password" />
          </label>
        </div>
        <footer class="my-account-footer">
          <button v-if="!setupMode" class="tool-button" type="button" @click="resetCredentialForm">取消</button>
          <button class="tool-button primary" type="button" :disabled="credentialSaving" @click="saveMyCredentials">{{ setupMode ? '完成设置' : '保存设置' }}</button>
        </footer>
      </div>
    </section>

    <template v-if="!setupMode && !accountOnly">
    <section class="toolbar">
      <label class="search-box">
        <Search />
        <input v-model.trim="keyword" placeholder="搜索登录账号、管理员姓名" @input="scheduleLoadUsers" @keyup.enter="loadFirstPage" />
      </label>
      <div class="filter-tabs" aria-label="管理员状态筛选">
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
          <span>{{ users.length }} 个账号</span>
        </div>
        <strong v-if="loading">加载中</strong>
        <strong v-else-if="keyword || statusFilter !== 'ALL'">已筛选</strong>
      </div>

      <div class="admin-table">
        <div class="table-head">
          <span>管理员</span>
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
                <small>{{ item.currentUser ? '当前登录账号' : getAdminTypeLabel(item.adminType) }}</small>
              </div>
            </div>
            <span class="code-cell">{{ item.username }}</span>
            <span :class="['status-badge', accountStatusClass(item)]">
              {{ accountStatusLabel(item) }}
            </span>
            <span class="time-cell">{{ formatTime(item.createTime) }}</span>
            <span class="time-cell">{{ formatTime(item.updateTime) }}</span>
            <div class="row-actions">
              <button class="row-action" type="button" @click="openNameEditor(item)">编辑</button>
              <button class="row-action" type="button" @click="openPasswordReset(item)">重置密码</button>
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
            <p>调整搜索条件或新增一个后台账号</p>
          </div>
        </div>
      </div>
    </section>
    </template>

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
        <label v-if="editorMode === 'create' && isOrganizerAdmin">
          <span>管理员类型</span>
          <select v-model="form.adminType">
            <option value="ORGANIZER_ADMIN">主办方主管理员</option>
            <option value="ORGANIZER_SUB_ADMIN">主办方子管理员</option>
          </select>
        </label>
        <footer>
          <button class="tool-button" type="button" @click="closeEditor">取消</button>
          <button class="tool-button primary" type="button" :disabled="saving" @click="saveEditor">保存</button>
        </footer>
      </section>
    </div>

    <div v-if="passwordOpen" class="modal-mask" @click.self="closePasswordEditor">
      <section class="modal-card">
        <header>
          <h2>{{ passwordMode === 'mine' ? '修改我的密码' : '重置管理员密码' }}</h2>
          <button class="icon-close" type="button" @click="closePasswordEditor">×</button>
        </header>
        <p v-if="passwordMode === 'reset'" class="hint-line">
          {{ passwordTarget?.name || passwordTarget?.username }} 下次登录需使用新密码
        </p>
        <label v-if="passwordMode === 'mine'">
          <span>当前密码</span>
          <input v-model.trim="passwordForm.oldPassword" type="password" autocomplete="current-password" />
        </label>
        <label>
          <span>新密码</span>
          <input v-model.trim="passwordForm.newPassword" type="password" autocomplete="new-password" placeholder="至少 6 位" />
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
import { ArrowDown, Key, Plus, Search, UserFilled } from '@element-plus/icons-vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import {
  createAdminUser,
  fetchAdminUsers,
  resetAdminUserPassword,
  updateAdminUser,
  updateAdminUserStatus,
  updateMyAdminPassword,
  updateMyAdminCredentials,
} from '@/api/admin'
import { getAdminMe } from '@/api/auth'
import { getAdminType, getAdminUsername, getDisplayName, isAdminCredentialSetupRequired, setSession } from '@/utils/auth'
import { ADMIN_TYPES, getAdminTypeLabel } from '@/config/adminAccess'

const route = useRoute()
const router = useRouter()
const users = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('ALL')
const searchTimer = ref(null)
const editorOpen = ref(false)
const editorMode = ref('create')
const editingUser = ref(null)
const passwordOpen = ref(false)
const passwordMode = ref('reset')
const passwordTarget = ref(null)
const myAccountExpanded = ref(false)
const credentialSaving = ref(false)
const setupMode = computed(() => route.path === '/admin/account-setup' && isAdminCredentialSetupRequired())
const accountOnly = computed(() => route.path === '/admin/account')
const currentUser = reactive({ username: getAdminUsername(), displayName: getDisplayName('admin') })
const isOrganizerAdmin = computed(() => getAdminType() === ADMIN_TYPES.ORGANIZER_ADMIN)

const credentialForm = reactive({
  username: currentUser.username,
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const form = reactive({
  username: '',
  name: '',
  password: '',
  adminType: 'ORGANIZER_ADMIN',
})

const passwordForm = reactive({
  oldPassword: '',
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

watch(setupMode, (required) => {
  myAccountExpanded.value = required || accountOnly.value || myAccountExpanded.value
  if (required) {
    users.value = []
  }
})

watch(() => route.path, () => {
  myAccountExpanded.value = setupMode.value || accountOnly.value
})

async function initializePage() {
  try {
    const me = await getAdminMe()
    setSession('admin', me)
    currentUser.username = me?.username || currentUser.username
    currentUser.displayName = me?.displayName || currentUser.displayName
    credentialForm.username = currentUser.username
  } catch {
    return
  }
  myAccountExpanded.value = setupMode.value || accountOnly.value
  if (setupMode.value || accountOnly.value) {
    return
  }
  await loadUsers()
}

async function loadUsers() {
  loading.value = true
  try {
    users.value = await fetchAdminUsers({
      status: statusApiValue(statusFilter.value),
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

function setStatusFilter(value) {
  if (statusFilter.value === value) return
  statusFilter.value = value
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
  form.adminType = isOrganizerAdmin.value ? 'ORGANIZER_ADMIN' : ''
  editorOpen.value = true
}

function openNameEditor(user) {
  editorMode.value = 'edit'
  editingUser.value = user
  form.username = user.username || ''
  form.name = user.name || ''
  form.password = ''
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
        ...(isOrganizerAdmin.value ? { adminType: form.adminType } : {}),
      })
      ElMessage.success('管理员账号已新增')
    } else {
      await updateAdminUser(editingUser.value.id, { name: form.name })
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
  await ElMessageBox.confirm(`确认${actionText}「${user.name || user.username}」吗？`, title, {
    confirmButtonText: actionText,
    cancelButtonText: '取消',
    type: status === 1 ? 'info' : 'warning',
  })
  await updateAdminUserStatus(user.id, { status })
  ElMessage.success(`账号已${actionText}`)
  await loadUsers()
}

function openPasswordReset(user) {
  passwordMode.value = 'reset'
  passwordTarget.value = user
  resetPasswordForm()
  passwordOpen.value = true
}

function openMyPasswordEditor() {
  passwordMode.value = 'mine'
  passwordTarget.value = null
  resetPasswordForm()
  passwordOpen.value = true
}

function toggleMyAccount() {
  if (setupMode.value || accountOnly.value) return
  myAccountExpanded.value = !myAccountExpanded.value
}

function resetCredentialForm() {
  credentialForm.username = currentUser.username
  credentialForm.oldPassword = ''
  credentialForm.newPassword = ''
  credentialForm.confirmPassword = ''
}

async function saveMyCredentials() {
  const initialSetup = setupMode.value
  if (initialSetup && !credentialForm.username) {
    ElMessage.warning('请输入登录账号')
    return
  }
  if (!initialSetup && !credentialForm.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!credentialForm.newPassword || credentialForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (credentialForm.newPassword !== credentialForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  credentialSaving.value = true
  try {
    await updateMyAdminCredentials({
      username: credentialForm.username,
      oldPassword: initialSetup ? undefined : credentialForm.oldPassword,
      newPassword: credentialForm.newPassword,
    })
    const updatedUser = await getAdminMe()
    setSession('admin', updatedUser)
    currentUser.username = updatedUser?.username || credentialForm.username
    currentUser.displayName = updatedUser?.displayName || currentUser.displayName
    myAccountExpanded.value = false
    ElMessage.success('账号设置已保存')
    if (initialSetup) {
      await router.replace('/admin/dashboard')
    } else {
      resetCredentialForm()
    }
  } finally {
    credentialSaving.value = false
  }
}

function closePasswordEditor() {
  passwordOpen.value = false
  passwordTarget.value = null
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function savePassword() {
  if (passwordMode.value === 'mine' && !passwordForm.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    if (passwordMode.value === 'mine') {
      await updateMyAdminPassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      })
      ElMessage.success('密码已更新')
    } else {
      await resetAdminUserPassword(passwordTarget.value.id, {
        password: passwordForm.newPassword,
      })
      ElMessage.success('密码已重置')
    }
    closePasswordEditor()
  } finally {
    saving.value = false
  }
}

function statusLabel(status) {
  return Number(status) === 1 ? '启用' : '停用'
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

.head-actions,
.tool-button,
.toolbar,
.search-box,
.filter-tabs,
.table-headline > div,
.admin-cell,
.row-action {
  display: flex;
  align-items: center;
}

.admin-users-page.setup-mode {
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
}

.admin-users-page.account-only {
  align-items: center;
  padding-top: 48px;
}

.account-only .admin-page-header,
.account-only .my-account-panel {
  width: min(100%, 720px);
}

.account-only .my-account-panel {
  margin-top: 18px;
}

.head-actions {
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.my-account-panel {
  flex: 0 0 auto;
  margin-top: 18px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.76);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.12);
}

.my-account-panel.required {
  border-color: rgba(216, 169, 53, 0.42);
  background: rgba(28, 38, 40, 0.94);
  box-shadow: 0 20px 60px rgba(216, 169, 53, 0.09);
}

.setup-mode .my-account-panel {
  width: min(100%, 480px);
  margin-top: 18px;
}

.setup-mode .my-account-toggle {
  min-height: 64px;
  padding-right: 20px;
  padding-left: 20px;
}

.my-account-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 70px;
  padding: 0 16px;
  color: var(--text);
  text-align: left;
  border: 0;
  background: transparent;
}

.my-account-toggle:not(:disabled) {
  cursor: pointer;
}

.my-account-toggle:disabled {
  opacity: 1;
}

.my-account-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 38px;
  height: 38px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.my-account-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.my-account-copy strong {
  color: var(--text);
  font-size: 15px;
}

.my-account-copy small {
  overflow: hidden;
  color: var(--muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toggle-arrow {
  width: 18px;
  height: 18px;
  margin-left: auto;
  color: var(--muted);
  transition: transform 0.18s ease;
}

.toggle-arrow.rotated {
  transform: rotate(180deg);
}

.my-account-body {
  padding: 0 16px 16px;
  border-top: 1px solid var(--line);
}

.setup-mode .my-account-body {
  padding: 0 20px 20px;
}

.credential-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding-top: 16px;
}

.setup-mode .credential-grid {
  grid-template-columns: 1fr;
  gap: 14px;
  padding-top: 18px;
}

.credential-grid label {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.credential-grid input {
  width: 100%;
  min-height: 42px;
  padding: 0 11px;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: none;
  background: rgba(7, 14, 17, 0.68);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.credential-grid input:focus {
  border-color: rgba(224, 184, 74, 0.5);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08);
}

.my-account-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.setup-mode .my-account-footer {
  margin-top: 18px;
}

.setup-mode .my-account-footer .tool-button {
  min-width: 96px;
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

.table-headline span,
.table-head,
.admin-cell small,
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
  grid-template-columns: minmax(190px, 1fr) minmax(155px, 0.9fr) 96px minmax(165px, 0.9fr) minmax(165px, 0.9fr) minmax(286px, auto);
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

.modal-card input,
.modal-card select {
  width: 100%;
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  padding: 11px 12px;
  color: var(--text);
  background: rgba(255, 255, 255, 0.05);
  outline: none;
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

  .head-actions {
    justify-content: flex-start;
  }

  .search-box {
    max-width: none;
  }

  .credential-grid {
    grid-template-columns: 1fr;
  }

}
</style>
