<template>
  <div class="portal-login">
    <main class="login-shell">
      <section class="login-panel">
        <h1>参赛入口</h1>
        <p class="login-lead">手机号验证后即可进入参赛平台</p>

        <el-form :model="form" label-position="top" class="login-form" @submit.prevent="submit">
          <el-form-item label="手机号">
            <el-input
              v-model="form.phone"
              clearable
              inputmode="numeric"
              maxlength="11"
              placeholder="请输入联系人手机号"
              @input="normalizePhone"
            />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="sms-row">
              <el-input
                v-model="form.code"
                inputmode="numeric"
                maxlength="6"
                placeholder="请输入验证码"
                @input="normalizeCode"
              />
              <el-button :loading="sending" :disabled="sendDisabled" @click="send">
                {{ sendButtonText }}
              </el-button>
            </div>
          </el-form-item>
          <div v-if="message" class="form-note">{{ message }}</div>
          <el-button type="primary" native-type="submit" class="full" :loading="submitting">
            验证手机号，进入报名
          </el-button>
        </el-form>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { portalLogin, sendSmsCode } from '@/api/auth'
import { setSession } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const form = reactive({ phone: '', code: '' })
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
const message = ref('')
let timer = null

const phoneValid = computed(() => /^1\d{10}$/.test(form.phone))
const codeValid = computed(() => /^\d{4,6}$/.test(form.code))
const sendDisabled = computed(() => sending.value || countdown.value > 0 || !phoneValid.value)
const sendButtonText = computed(() => (countdown.value > 0 ? `${countdown.value}s` : '发送验证码'))
const postLoginPath = computed(() => normalizePortalPath(route.query.redirect || route.query.next, '/portal/my'))
const postProfilePath = computed(() => (
  postLoginPath.value === '/portal/profile' ? '/portal/my' : postLoginPath.value
))

async function send() {
  if (!phoneValid.value) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  sending.value = true
  try {
    await sendSmsCode({ phone: form.phone, bizType: 'PORTAL_LOGIN' })
    message.value = '验证码已发送，请查看手机短信'
    startCountdown()
  } finally {
    sending.value = false
  }
}

async function submit() {
  if (!phoneValid.value) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  if (!codeValid.value) {
    ElMessage.warning('请输入验证码')
    return
  }

  submitting.value = true
  try {
    const data = await portalLogin({ phone: form.phone, code: form.code })
    setSession('portal', data)
    ElMessage.success(data.newAccount ? '账号已创建' : '已登录')
    if (data.newAccount || data.profileRequired || data.profileComplete === false) {
      router.push({ path: '/portal/profile', query: { next: postProfilePath.value } })
      return
    }
    router.push(postLoginPath.value)
  } finally {
    submitting.value = false
  }
}

function normalizePhone(value) {
  form.phone = String(value || '').replace(/\D/g, '').slice(0, 11)
}

function normalizeCode(value) {
  form.code = String(value || '').replace(/\D/g, '').slice(0, 6)
}

function normalizePortalPath(value, fallback) {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (!rawValue || typeof rawValue !== 'string') {
    return fallback
  }
  if (!rawValue.startsWith('/portal') || rawValue.startsWith('/portal/login')) {
    return fallback
  }
  return rawValue
}

function startCountdown() {
  countdown.value = 60
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
.portal-login {
  --el-color-primary: #b87517;
  --el-color-primary-light-3: #d99d3d;
  --el-color-primary-light-5: #e8bc6b;
  --el-color-primary-light-7: #f1d394;
  --el-color-primary-light-9: #fff0c2;
  --el-color-primary-dark-2: #8b5c19;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  color: #2b1d10;
  background:
    linear-gradient(90deg, rgba(80, 51, 22, 0.055) 1px, transparent 1px),
    linear-gradient(180deg, #fff8e8 0%, #f2dfb6 100%);
  background-size: 24px 24px, auto;
}

.login-shell {
  display: grid;
  width: min(100%, 540px);
  margin: 0 auto;
}

.login-panel {
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  box-shadow: 0 24px 56px rgba(83, 51, 17, 0.14);
}

.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 470px;
  padding: 46px 42px;
  background:
    linear-gradient(180deg, rgba(255, 253, 247, 0.98), rgba(255, 250, 238, 0.96));
  box-shadow:
    0 24px 56px rgba(83, 51, 17, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.login-panel h1 {
  margin: 0 0 12px;
  font-size: 40px;
  line-height: 1.1;
  letter-spacing: 0;
}

.login-lead {
  margin: 0;
  color: #6f6252;
  font-size: 16px;
  line-height: 1.7;
}

.login-form {
  margin-top: 28px;
}

.login-form :deep(.el-form-item__label) {
  color: #5c5045;
  font-weight: 800;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 42px;
  background: #fffdf7;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(87, 58, 26, 0.14) inset;
}

.login-form :deep(.el-button) {
  min-height: 42px;
  border-radius: 8px;
  font-weight: 800;
}

.sms-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 116px;
  gap: 12px;
}

.form-note {
  margin: 8px 0 18px;
  padding: 12px;
  color: #725018;
  background: #fff0c2;
  border-radius: 8px;
}

.full {
  width: 100%;
}

@media (max-width: 980px) {
  .portal-login {
    padding: 20px;
  }

  .login-shell {
    width: min(100%, 520px);
  }
}

@media (max-width: 640px) {
  .portal-login {
    padding: 14px;
  }

  .login-panel {
    padding: 24px;
    min-height: 430px;
  }

  .login-panel h1 {
    font-size: 30px;
  }

  .sms-row {
    grid-template-columns: 1fr;
  }
}
</style>
