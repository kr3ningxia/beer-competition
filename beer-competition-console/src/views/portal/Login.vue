<template>
  <div class="portal-login">
    <main class="login-shell">
      <section class="login-panel">
        <h1>参赛入口</h1>
        <p class="login-lead">手机号验证后即可进入赛事平台</p>

        <el-form :model="form" label-position="top" class="login-form" @submit.prevent="submit">
          <el-form-item label="手机号">
            <el-input
              v-model="form.phone"
              clearable
              type="tel"
              name="phone"
              autocomplete="tel"
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
                name="code"
                autocomplete="one-time-code"
                spellcheck="false"
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
          <transition name="captcha-reveal">
            <el-form-item v-if="showCaptcha" label="图形验证码" class="captcha-form-item">
              <div class="captcha-row">
                <el-input
                  v-model="form.captchaCode"
                  name="captchaCode"
                  autocomplete="off"
                  spellcheck="false"
                  maxlength="4"
                  placeholder="请输入图形验证码"
                  @input="normalizeCaptchaCode"
                />
                <button
                  type="button"
                  class="captcha-image-button"
                  :disabled="captchaLoading"
                  aria-label="刷新图形验证码"
                  title="换一张"
                  @click="refreshCaptcha"
                >
                  <img v-if="captchaImage" :src="captchaImage" alt="图形验证码，点击刷新" />
                  <span v-else class="captcha-loading">加载中</span>
                  <el-icon class="captcha-refresh-icon" aria-hidden="true"><Refresh /></el-icon>
                </button>
              </div>
            </el-form-item>
          </transition>
          <div v-if="message" class="form-note">{{ message }}</div>
          <el-button type="primary" native-type="submit" class="full" :loading="submitting">
            验证手机号，进入报名
          </el-button>
        </el-form>
      </section>
    </main>
    <SiteFilingFooter />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { getLoginCaptcha, portalLogin, sendSmsCode } from '@/api/auth'
import SiteFilingFooter from '@/components/SiteFilingFooter.vue'
import { setSession } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const form = reactive({ phone: '', code: '', captchaId: '', captchaCode: '' })
const sending = ref(false)
const submitting = ref(false)
const captchaLoading = ref(false)
const showCaptcha = ref(false)
const captchaImage = ref('')
const countdown = ref(0)
const message = ref('')
let timer = null

const phoneValid = computed(() => /^1\d{10}$/.test(form.phone))
const codeValid = computed(() => /^\d{4,6}$/.test(form.code))
const captchaValid = computed(() => !showCaptcha.value || /^[A-Z0-9]{4}$/.test(form.captchaCode))
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
  if (!captchaValid.value) {
    ElMessage.warning('请输入图形验证码')
    return
  }

  submitting.value = true
  try {
    const data = await portalLogin({
      phone: form.phone,
      code: form.code,
      captchaId: form.captchaId,
      captchaCode: form.captchaCode,
    })
    setSession('portal', data)
    ElMessage.success(data.newAccount ? '账号已创建' : '已登录')
    if (data.newAccount || data.profileRequired || data.profileComplete === false) {
      router.push({ path: '/portal/profile', query: { next: postProfilePath.value } })
      return
    }
    router.push(postLoginPath.value)
  } catch (error) {
    const errorMessage = error?.userMessage || error?.message || ''
    if (errorMessage === '验证码错误或已过期' || errorMessage.includes('图形验证码')) {
      showCaptcha.value = true
      message.value = '请完成图形验证码'
      await loadCaptcha()
    }
  } finally {
    submitting.value = false
  }
}

function normalizePhone(value) {
  const normalized = String(value || '').replace(/\D/g, '').slice(0, 11)
  if (normalized !== form.phone && showCaptcha.value) {
    resetCaptcha()
  }
  form.phone = normalized
}

function normalizeCode(value) {
  form.code = String(value || '').replace(/\D/g, '').slice(0, 6)
}

function normalizeCaptchaCode(value) {
  form.captchaCode = String(value || '').replace(/[^a-z\d]/gi, '').toUpperCase().slice(0, 4)
}

async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const data = await getLoginCaptcha()
    captchaImage.value = data.image
    form.captchaId = data.captchaId
    form.captchaCode = ''
  } finally {
    captchaLoading.value = false
  }
}

async function refreshCaptcha() {
  if (captchaLoading.value) return
  await loadCaptcha()
}

function resetCaptcha() {
  showCaptcha.value = false
  captchaImage.value = ''
  form.captchaId = ''
  form.captchaCode = ''
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
  display: flex;
  flex-direction: column;
  align-items: stretch;
  color: #2b1d10;
  background:
    linear-gradient(90deg, rgba(80, 51, 22, 0.055) 1px, transparent 1px),
    linear-gradient(180deg, #fff8e8 0%, #f2dfb6 100%);
  background-size: 24px 24px, auto;
}

.login-shell {
  display: grid;
  flex: 1;
  align-items: center;
  width: min(100%, 540px);
  margin: 0 auto;
  padding: 28px 0;
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
  grid-template-columns: minmax(0, 2fr) minmax(112px, 1fr);
  gap: 12px;
  width: 100%;
}

.sms-row :deep(.el-input) {
  min-width: 0;
}

.sms-row :deep(.el-button) {
  width: 100%;
  padding-inline: 12px;
  white-space: nowrap;
}

.captcha-reveal-enter-active,
.captcha-reveal-leave-active {
  overflow: hidden;
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.captcha-reveal-enter-from,
.captcha-reveal-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 12px;
  width: 100%;
}

.captcha-image-button {
  position: relative;
  display: grid;
  place-items: center;
  width: 132px;
  min-height: 42px;
  padding: 0;
  overflow: hidden;
  color: #7a6448;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 8px;
  background: #fffdf7;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.captcha-image-button:hover,
.captcha-image-button:focus-visible {
  border-color: #b87517;
  outline: none;
  box-shadow: 0 0 0 3px rgba(184, 117, 23, 0.12);
}

.captcha-image-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.captcha-image-button img {
  display: block;
  width: 100%;
  height: 42px;
  object-fit: cover;
}

.captcha-loading {
  font-size: 13px;
}

.captcha-refresh-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  color: #fff;
  border-radius: 50%;
  background: rgba(43, 29, 16, 0.52);
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
  .login-shell {
    width: min(100%, 520px);
    padding: 20px 0;
  }
}

@media (max-width: 640px) {
  .login-panel {
    padding: 24px;
    min-height: 430px;
  }

  .login-shell {
    width: calc(100% - 28px);
    padding: 14px 0;
  }

  .login-panel h1 {
    font-size: 30px;
  }

  .sms-row {
    grid-template-columns: minmax(0, 2fr) minmax(108px, 1fr);
    gap: 10px;
  }

  .captcha-row {
    grid-template-columns: minmax(0, 1fr) 124px;
    gap: 10px;
  }

  .captcha-image-button {
    width: 124px;
  }
}

@media (max-width: 720px) {
  .portal-login :deep(.el-input__inner) {
    font-size: 16px;
  }
}
</style>
