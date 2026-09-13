<template>
  <div class="account-settings-page">
    <AdminPageHeader :title="setupMode ? '完成账号设置' : '账号设置'" />

    <div class="settings-body">
      <section class="form-card">
        <header class="card-head">
          <h2>账号信息</h2>
        </header>
        <dl class="info-grid">
          <div>
            <dt>登录账号</dt>
            <dd>{{ profile.username || '-' }}</dd>
          </div>
          <div>
            <dt>管理员姓名</dt>
            <dd>{{ profile.name || '未设置' }}</dd>
          </div>
          <div>
            <dt>角色</dt>
            <dd>{{ roleLabel }}</dd>
          </div>
          <div v-if="profile.organizerName">
            <dt>所属主办方</dt>
            <dd>{{ profile.organizerName }}</dd>
          </div>
        </dl>
      </section>

      <section v-if="setupMode" class="form-card setup">
        <header class="card-head">
          <div class="card-head-row">
            <h2>设置登录凭据</h2>
            <span class="head-flag">待完成</span>
          </div>
          <p>{{ setupGuidance }}</p>
        </header>

        <div class="form-grid">
          <label v-if="needsUsernameSetup" class="text-field span-all">
            <span class="field-label">新登录账号</span>
            <span :class="['field-control', { invalid: Boolean(setupUsernameError) }]">
              <input
                v-model.trim="setupForm.username"
                autocomplete="off"
                :placeholder="`当前账号：${profile.username || '-'}`"
              />
            </span>
            <small v-if="setupUsernameError" class="field-error">{{ setupUsernameError }}</small>
            <small v-else class="field-hint">4–32 位英文、数字或 . _ -</small>
          </label>

          <PasswordInput
            v-model="setupForm.newPassword"
            label="新密码"
            hint="6–32 位"
            autocomplete="new-password"
            :error="setupPasswordError"
          />
          <PasswordInput
            v-model="setupForm.confirmPassword"
            label="确认新密码"
            autocomplete="new-password"
            :error="setupConfirmError"
          />
        </div>

        <footer class="card-foot">
          <button class="tool-button primary" type="button" :disabled="saving || !setupValid" @click="submitSetup">
            {{ saving ? '提交中…' : '完成设置' }}
          </button>
        </footer>
      </section>

      <section v-else class="form-card">
        <header class="card-head">
          <h2>修改密码</h2>
        </header>

        <div class="form-grid">
          <div class="span-all">
            <PasswordInput
              v-model="passwordForm.oldPassword"
              label="当前密码"
              autocomplete="current-password"
            />
          </div>
          <PasswordInput
            v-model="passwordForm.newPassword"
            label="新密码"
            hint="6–32 位"
            autocomplete="new-password"
            :error="newPasswordError"
          />
          <PasswordInput
            v-model="passwordForm.confirmPassword"
            label="确认新密码"
            autocomplete="new-password"
            :error="confirmPasswordError"
          />
        </div>

        <footer class="card-foot">
          <button class="tool-button primary" type="button" :disabled="passwordSaving || !passwordValid" @click="submitPassword">
            {{ passwordSaving ? '提交中…' : '保存密码' }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import PasswordInput from '@/components/admin/PasswordInput.vue'
import { updateMyAdminCredentials } from '@/api/admin'
import { getAdminMe } from '@/api/auth'
import {
  getAdminOrganizerName,
  getAdminType,
  getAdminUsername,
  getDisplayName,
  isAdminUsernameSetupRequired,
  setSession,
} from '@/utils/auth'
import { getAdminTypeLabel } from '@/config/adminAccess'

const ADMIN_USERNAME_PATTERN = /^[A-Za-z0-9._-]{4,32}$/
const PASSWORD_MIN = 6
const PASSWORD_MAX = 32

const route = useRoute()
const router = useRouter()

const setupMode = computed(() => route.path === '/admin/account-setup')
const needsUsernameSetup = ref(false)

const profile = reactive({
  name: '',
  username: '',
  adminType: '',
  organizerName: '',
})

const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const setupForm = reactive({ username: '', newPassword: '', confirmPassword: '' })

const saving = ref(false)
const passwordSaving = ref(false)

const roleLabel = computed(() => getAdminTypeLabel(profile.adminType))

const setupUsernameError = computed(() => validateUsername(setupForm.username, profile.username))

const newPasswordError = computed(() => {
  const value = passwordForm.newPassword
  if (!value) return ''
  if (!isPasswordLengthValid(value)) return passwordLengthMessage()
  if (value === passwordForm.oldPassword) return '新密码不能与当前密码相同'
  return ''
})

const confirmPasswordError = computed(() => (
  passwordForm.confirmPassword && passwordForm.confirmPassword !== passwordForm.newPassword
    ? '两次输入的新密码不一致'
    : ''
))

const setupPasswordError = computed(() => (
  setupForm.newPassword && !isPasswordLengthValid(setupForm.newPassword) ? passwordLengthMessage() : ''
))

const setupConfirmError = computed(() => (
  setupForm.confirmPassword && setupForm.confirmPassword !== setupForm.newPassword
    ? '两次输入的新密码不一致'
    : ''
))

const passwordValid = computed(() => (
  Boolean(passwordForm.oldPassword)
  && isPasswordLengthValid(passwordForm.newPassword)
  && passwordForm.newPassword !== passwordForm.oldPassword
  && passwordForm.newPassword === passwordForm.confirmPassword
))

const setupValid = computed(() => {
  if (needsUsernameSetup.value
    && (!ADMIN_USERNAME_PATTERN.test(setupForm.username) || setupForm.username === profile.username)) {
    return false
  }
  return isPasswordLengthValid(setupForm.newPassword)
    && setupForm.newPassword === setupForm.confirmPassword
})

const setupGuidance = computed(() => (
  needsUsernameSetup.value
    ? `系统分配的登录账号为 ${profile.username || '-'}，请设置你自己的登录账号与密码，完成后即可进入工作台。`
    : '为了账号安全，请先设置新的登录密码，完成后即可进入工作台。'
))

onMounted(initializePage)

async function initializePage() {
  let me = null
  try {
    me = await getAdminMe()
    setSession('admin', me)
  } catch {
    // 请求拦截器已统一提示，这里退回本地缓存的身份信息。
  }
  applyProfile(me)
}

function applyProfile(me) {
  if (me) {
    profile.name = me.displayName || ''
    profile.username = me.username || ''
    profile.adminType = me.adminType || ''
    profile.organizerName = me.organizerName || ''
    needsUsernameSetup.value = Boolean(me.mustChangeUsername)
    return
  }
  profile.name = getDisplayName('admin')
  profile.username = getAdminUsername()
  profile.adminType = getAdminType()
  profile.organizerName = getAdminOrganizerName()
  needsUsernameSetup.value = isAdminUsernameSetupRequired()
}

function validateUsername(value, currentUsername) {
  if (!value) return ''
  if (!ADMIN_USERNAME_PATTERN.test(value)) return '需为 4–32 位英文、数字或 . _ -'
  if (currentUsername && value === currentUsername) return '新账号不能与当前账号相同'
  return ''
}

function isPasswordLengthValid(value) {
  return Boolean(value) && value.length >= PASSWORD_MIN && value.length <= PASSWORD_MAX
}

function passwordLengthMessage() {
  return `密码长度需为 ${PASSWORD_MIN} 到 ${PASSWORD_MAX} 位`
}

async function refreshSession() {
  const me = await getAdminMe()
  setSession('admin', me)
  applyProfile(me)
}

async function submitPassword() {
  if (!passwordValid.value) return
  passwordSaving.value = true
  try {
    await updateMyAdminCredentials({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    await refreshSession()
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码已更新')
  } finally {
    passwordSaving.value = false
  }
}

async function submitSetup() {
  if (!setupValid.value) return
  saving.value = true
  try {
    const payload = { newPassword: setupForm.newPassword }
    if (needsUsernameSetup.value) {
      payload.username = setupForm.username
    }
    await updateMyAdminCredentials(payload)
    await refreshSession()
    ElMessage.success('账号设置已完成')
    await router.replace('/admin/dashboard')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.account-settings-page {
  --panel: rgba(22, 32, 36, 0.9);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --danger: #ffb4a8;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  padding: 0 28px 40px;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(216, 169, 53, 0.13), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
}

.account-settings-page h2,
.account-settings-page p,
.account-settings-page dl,
.account-settings-page dt,
.account-settings-page dd {
  margin: 0;
}

.settings-body {
  display: grid;
  gap: 18px;
  width: min(100%, 880px);
  margin: 0 auto;
  padding-top: 22px;
}

.form-card {
  padding: 20px 22px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.form-card.setup {
  border-color: rgba(216, 169, 53, 0.42);
  background: rgba(28, 38, 40, 0.94);
  box-shadow: 0 20px 60px rgba(216, 169, 53, 0.09);
}

.card-head-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-head h2 {
  font-size: 16px;
  font-weight: 850;
}

.card-head p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.head-flag {
  padding: 3px 9px;
  color: var(--gold-soft);
  font-size: 11px;
  font-weight: 800;
  border: 1px solid rgba(216, 169, 53, 0.28);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.1);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px 22px;
  margin-top: 18px;
}

.info-grid dt {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.info-grid dd {
  margin-top: 6px;
  overflow: hidden;
  font-size: 15px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: start;
  gap: 18px;
  margin-top: 18px;
}

.span-all {
  grid-column: 1 / -1;
}

.text-field {
  display: grid;
  gap: 7px;
}

.field-label {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.field-control {
  display: flex;
  align-items: center;
  min-height: 42px;
  padding: 0 11px;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.field-control:focus-within {
  border-color: rgba(224, 184, 74, 0.5);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08);
}

.field-control.invalid {
  border-color: rgba(255, 180, 168, 0.5);
}

.field-control input {
  width: 100%;
  min-width: 0;
  color: var(--text);
  font: inherit;
  border: 0;
  outline: 0;
  background: transparent;
}

.field-control input::placeholder {
  color: var(--faint);
}

.field-hint {
  color: var(--faint);
  font-size: 12px;
}

.field-error {
  color: var(--danger);
  font-size: 12px;
}

.card-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.tool-button {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 18px;
  color: var(--text);
  font: inherit;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.tool-button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

@media (max-width: 720px) {
  .account-settings-page {
    padding: 0 16px 28px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
