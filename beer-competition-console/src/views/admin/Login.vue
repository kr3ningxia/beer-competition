<template>
  <div class="login-page" :class="{ 'is-submitting': loading }">
    <section class="login-shell">
      <div class="brand-panel">
        <header class="brand-lockup">
          <span class="brand-mark">
            <img src="/brand-icon.png" alt="" aria-hidden="true">
          </span>
          <div class="brand-text">
            <p>啤酒事务局</p>
            <h1>赛事后台</h1>
          </div>
        </header>

        <div class="brew-visual" aria-hidden="true">
          <span class="brew-light" />
          <svg class="toast-illustration" viewBox="0 0 520 340" role="presentation">
            <defs>
              <linearGradient id="mug-glass-fill" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#f7fbf0" stop-opacity="0.22" />
                <stop offset="0.48" stop-color="#d8e8dd" stop-opacity="0.08" />
                <stop offset="1" stop-color="#8aa39c" stop-opacity="0.18" />
              </linearGradient>
              <linearGradient id="mug-beer-fill" x1="0" y1="0" x2="0.82" y2="1">
                <stop offset="0" stop-color="#ffe28a" />
                <stop offset="0.38" stop-color="#eab83f" />
                <stop offset="1" stop-color="#a96718" />
              </linearGradient>
              <linearGradient id="mug-beer-glow" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0" stop-color="#fff2ad" stop-opacity="0" />
                <stop offset="0.5" stop-color="#fff5c8" stop-opacity="0.8" />
                <stop offset="1" stop-color="#fff2ad" stop-opacity="0" />
              </linearGradient>
              <linearGradient id="mug-foam-fill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0" stop-color="#fffdf6" />
                <stop offset="1" stop-color="#e7e3d5" />
              </linearGradient>
              <filter id="mug-shadow" x="-20%" y="-20%" width="140%" height="160%">
                <feDropShadow dx="0" dy="12" stdDeviation="10" flood-color="#000000" flood-opacity="0.38" />
              </filter>
              <g id="mug-art">
                <path
                  class="mug-body"
                  d="M68 72 H234 L214 257 C211 280 196 292 176 295 H126 C105 292 90 280 87 257 Z"
                  fill="url(#mug-glass-fill)"
                  filter="url(#mug-shadow)"
                />
                <path
                  class="mug-beer"
                  d="M81 121 H221 L205 256 C203 273 190 283 175 285 H127 C111 283 99 273 97 256 Z"
                  fill="url(#mug-beer-fill)"
                />
                <path class="beer-wave" d="M81 122 C110 114 133 128 159 121 C180 115 198 119 221 122 V139 H81 Z" fill="#ffe493" />
                <path class="mug-liquid-shine" d="M101 142 L93 248" fill="none" stroke="url(#mug-beer-glow)" stroke-linecap="round" stroke-width="9" />
                <circle class="beer-bubble bubble-a" cx="128" cy="231" r="5" fill="rgba(255, 246, 198, 0.72)" />
                <circle class="beer-bubble bubble-b" cx="179" cy="207" r="4" fill="rgba(255, 246, 198, 0.72)" />
                <circle class="beer-bubble bubble-c" cx="151" cy="174" r="3" fill="rgba(255, 246, 198, 0.72)" />
                <circle class="beer-bubble bubble-d" cx="193" cy="253" r="3" fill="rgba(255, 246, 198, 0.72)" />
                <path
                  class="mug-outline"
                  d="M68 72 L87 257 C90 280 105 292 126 295 H176 C196 292 211 280 214 257 L234 72"
                  fill="none"
                  stroke="rgba(224, 239, 232, 0.44)"
                  stroke-width="3"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
                <path class="foam" d="M66 117 C64 107 74 99 87 102 C90 91 103 88 113 97 C121 84 143 87 148 101 C160 91 178 94 183 106 C195 98 213 105 214 118 C225 118 231 127 224 136 H76 C68 135 63 127 66 117 Z" fill="url(#mug-foam-fill)" />
                <path class="foam-drip foam-drip-a" d="M91 124 C91 140 92 151 99 156 C104 160 111 154 112 143 L113 124 Z" fill="url(#mug-foam-fill)" />
                <path class="foam-drip foam-drip-b" d="M181 124 C181 140 183 151 190 156 C196 160 202 153 203 141 L204 124 Z" fill="url(#mug-foam-fill)" />
                <ellipse class="foam-highlight foam-highlight-a" cx="105" cy="108" rx="17" ry="7" fill="rgba(255, 255, 255, 0.48)" />
                <ellipse class="foam-highlight foam-highlight-b" cx="173" cy="113" rx="20" ry="8" fill="rgba(255, 255, 255, 0.48)" />
                <path class="mug-glass-highlight" d="M104 143 L97 244 C97 251 101 255 106 256" fill="none" stroke="rgba(255, 255, 255, 0.46)" stroke-linecap="round" stroke-width="8" />
              </g>
            </defs>

            <ellipse class="ground-shadow" cx="260" cy="309" rx="192" ry="12" />
            <g class="celebration-rays">
              <path class="ray ray-a" d="M119 58 L95 12 L130 51 Z" />
              <path class="ray ray-b" d="M260 48 L260 5 L271 48 Z" />
              <path class="ray ray-c" d="M401 58 L425 12 L390 51 Z" />
              <path class="ray ray-d" d="M79 82 L45 60 L83 88 Z" />
              <path class="ray ray-e" d="M441 82 L475 60 L437 88 Z" />
            </g>
            <g class="celebration-bubbles">
              <circle class="celebration-bubble bubble-1" cx="82" cy="65" r="9" />
              <circle class="celebration-bubble bubble-2" cx="172" cy="30" r="7" />
              <circle class="celebration-bubble bubble-3" cx="215" cy="66" r="6" />
              <circle class="celebration-bubble bubble-4" cx="306" cy="40" r="8" />
              <circle class="celebration-bubble bubble-5" cx="358" cy="73" r="6" />
              <circle class="celebration-bubble bubble-6" cx="440" cy="64" r="9" />
            </g>
            <g class="mug mug-left">
              <g class="mug-motion mug-motion-left">
                <use class="mug-art-use" href="#mug-art" />
              </g>
            </g>
            <g class="mug mug-right">
              <g class="mug-motion mug-motion-right">
                <use class="mug-art-use" href="#mug-art" />
              </g>
            </g>
            <g class="toast-burst">
              <circle class="toast-ring" cx="260" cy="111" r="19" />
              <path class="toast-spark toast-spark-a" d="M260 78 V64" />
              <path class="toast-spark toast-spark-b" d="M238 91 L228 81" />
              <path class="toast-spark toast-spark-c" d="M282 91 L292 81" />
              <circle class="toast-dot toast-dot-a" cx="224" cy="112" r="3" />
              <circle class="toast-dot toast-dot-b" cx="296" cy="112" r="3" />
            </g>
          </svg>
        </div>
      </div>

      <section class="login-card">
        <div class="card-head">
          <h2>赛事后台登录</h2>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username">
              <template #prefix>
                <User />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password autocomplete="current-password">
              <template #prefix>
                <Lock />
              </template>
            </el-input>
          </el-form-item>

          <div class="form-actions">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
          </div>

          <el-button native-type="submit" class="full" :loading="loading">
            登录
            <el-icon v-if="!loading"><Right /></el-icon>
          </el-button>
        </el-form>
      </section>
    </section>
    <SiteFilingFooter tone="dark" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Right, User } from '@element-plus/icons-vue'
import { adminLogin } from '@/api/auth'
import SiteFilingFooter from '@/components/SiteFilingFooter.vue'
import { setSession } from '@/utils/auth'

const REMEMBER_KEY = 'admin_login_remember_account'
const TOAST_DURATION = 780

const router = useRouter()
const route = useRoute()
const formRef = ref()

const queryUsername = typeof route.query.username === 'string' ? route.query.username : ''
const savedUsername = localStorage.getItem(REMEMBER_KEY) || ''

const form = reactive({
  username: queryUsername || savedUsername,
  password: '',
})
const remember = ref(Boolean(savedUsername) && !queryUsername)
const loading = ref(false)

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const delay = (duration) => new Promise((resolve) => setTimeout(resolve, duration))

function persistRemember() {
  if (remember.value && form.username) {
    localStorage.setItem(REMEMBER_KEY, form.username)
  } else {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

async function submit() {
  if (loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const data = await adminLogin(form)
    await delay(TOAST_DURATION)
    setSession('admin', data)
    persistRemember()
    ElMessage.success('登录成功')
    if (data?.mustChangePassword || data?.mustChangeUsername) {
      router.replace('/admin/account-setup')
    } else {
      router.replace('/admin/dashboard')
    }
  } catch {
    // 错误提示已由请求拦截器统一展示，这里仅复位加载态
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  --panel: rgba(17, 26, 30, 0.9);
  --line: rgba(219, 232, 237, 0.11);
  --text: #eaf1f3;
  --muted: #93a6ae;
  --faint: #647982;
  --gold-soft: #e0b84a;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100dvh;
  padding: 24px clamp(20px, 4vw, 56px) 16px;
  overflow: auto;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    radial-gradient(circle at 18% 14%, rgba(216, 169, 53, 0.16), transparent 22rem),
    radial-gradient(circle at 84% 10%, rgba(111, 180, 207, 0.1), transparent 22rem),
    linear-gradient(135deg, #0a1013 0%, #111c20 48%, #0a1317 100%);
  background-size: 48px 48px, 48px 48px, auto, auto, auto;
}

.login-page::before,
.login-page::after {
  position: fixed;
  pointer-events: none;
  content: "";
}

.login-page::before {
  inset: 0;
  background: repeating-linear-gradient(112deg, transparent 0 22px, rgba(216, 169, 53, 0.018) 22px 23px, transparent 23px 50px);
}

.login-page::after {
  inset: auto 6% 0 auto;
  width: 40rem;
  height: 40rem;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(216, 169, 53, 0.07), transparent 64%);
  transform: translateY(46%);
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  width: min(960px, 100%);
  min-height: 540px;
  margin: auto;
  border: 1px solid var(--line);
  border-radius: 16px;
  overflow: hidden;
  background: rgba(12, 18, 21, 0.74);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(18px);
}

.brand-panel,
.login-card {
  position: relative;
  min-width: 0;
}

.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  padding: 34px 34px 30px;
  background:
    radial-gradient(circle at 24% 22%, rgba(216, 169, 53, 0.12), transparent 14rem),
    radial-gradient(circle at 70% 80%, rgba(111, 180, 207, 0.08), transparent 16rem);
  border-right: 1px solid var(--line);
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  display: grid;
  place-items: center;
  overflow: hidden;
  width: 50px;
  height: 50px;
  border: 1px solid rgba(240, 205, 96, 0.28);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.03);
  box-shadow: 0 0 0 6px rgba(216, 169, 53, 0.06);
}

.brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand-text p {
  margin: 0;
  color: var(--gold-soft);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 2px;
}

.brand-text h1 {
  margin: 6px 0 0;
  font-size: 28px;
  line-height: 1.05;
  letter-spacing: 1px;
}

.brew-visual {
  position: relative;
  display: grid;
  flex: 1;
  place-items: center;
  align-self: stretch;
  min-height: 0;
  width: 100%;
  isolation: isolate;
}

.brew-light {
  position: absolute;
  right: 14%;
  bottom: 18%;
  left: 14%;
  z-index: -1;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(216, 169, 53, 0.28), transparent);
  box-shadow: 0 0 30px 8px rgba(216, 169, 53, 0.08);
  animation: light-breathe 5s ease-in-out infinite;
}

.toast-illustration {
  display: block;
  width: min(540px, 100%);
  max-height: 100%;
  overflow: visible;
}

.ground-shadow {
  fill: rgba(0, 0, 0, 0.34);
  filter: blur(5px);
  transform-origin: center;
  animation: shadow-breathe 7s ease-in-out infinite;
}

.mug {
  transform-box: view-box;
  transform-origin: 130px 165px;
  will-change: transform;
}

.mug-motion {
  transform-box: view-box;
  transform-origin: 130px 165px;
  will-change: transform;
}

.mug-left {
  animation: mug-left-float 7s ease-in-out infinite;
}

.mug-right {
  animation: mug-right-float 7s ease-in-out -0.3s infinite;
}

.mug-body {
  fill: url(#mug-glass-fill);
  stroke: none;
}

.mug-beer {
  fill: url(#mug-beer-fill);
}

.beer-wave {
  fill: #ffe493;
  opacity: 0.84;
  transform-origin: 150px 126px;
  animation: liquid-wave 4s ease-in-out infinite;
}

.mug-liquid-shine {
  stroke: url(#mug-beer-glow);
  stroke-dasharray: 26 110;
  animation: liquid-shine 5s ease-in-out infinite;
}

.beer-bubble {
  fill: rgba(255, 246, 198, 0.72);
  transform-box: fill-box;
  transform-origin: center;
  animation: beer-bubble-rise 4.8s ease-in-out infinite;
}

.bubble-b {
  animation-delay: -1.7s;
}

.bubble-c {
  animation-delay: -3s;
}

.bubble-d {
  animation-delay: -3.8s;
}

.foam {
  fill: url(#mug-foam-fill);
  transform-box: fill-box;
  transform-origin: center bottom;
  animation: foam-settle 6s ease-in-out infinite;
}

.foam-highlight {
  fill: rgba(255, 255, 255, 0.48);
  transform-box: fill-box;
  transform-origin: center;
}

.mug-glass-highlight {
  stroke: rgba(255, 255, 255, 0.46);
  opacity: 0.68;
}

.foam-drip {
  fill: url(#mug-foam-fill);
}

.toast-burst {
  opacity: 0;
  transform-box: view-box;
  transform-origin: 260px 111px;
}

.toast-ring {
  fill: none;
  stroke: rgba(250, 211, 99, 0.82);
  stroke-width: 2;
}

.toast-spark {
  stroke: rgba(255, 235, 153, 0.9);
  stroke-linecap: round;
  stroke-width: 3;
}

.toast-dot {
  fill: #f6d36c;
}

.celebration-rays {
  opacity: 0.34;
  transform-box: view-box;
  transform-origin: 260px 55px;
  animation: rays-breathe 5.2s ease-in-out infinite;
}

.ray {
  fill: #f3c951;
  transform-box: fill-box;
  transform-origin: center;
  animation: ray-shimmer 5.2s ease-in-out infinite;
}

.ray-b {
  opacity: 0.7;
  animation-delay: -1.1s;
}

.ray-c,
.ray-e {
  animation-delay: -2.3s;
}

.celebration-bubbles {
  opacity: 0.6;
}

.celebration-bubble {
  fill: none;
  stroke: rgba(236, 244, 235, 0.62);
  stroke-width: 2.5;
  transform-box: fill-box;
  transform-origin: center;
  animation: celebration-bubble-float 6s ease-in-out infinite;
}

.celebration-bubble.bubble-2,
.celebration-bubble.bubble-5 {
  animation-delay: -2.4s;
}

.celebration-bubble.bubble-3,
.celebration-bubble.bubble-6 {
  animation-delay: -4.1s;
}

.login-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 44px 38px;
  background: var(--panel);
}

.login-card::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent 30%);
  content: "";
}

.login-card > * {
  position: relative;
}

.card-head {
  margin-bottom: 32px;
}

.card-head h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  letter-spacing: 0.5px;
}

.form-actions {
  display: flex;
  align-items: center;
  margin: -2px 0 16px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #c7d4d9;
  font-weight: 700;
  font-size: 13px;
}

:deep(.el-form-item__error) {
  color: #ff8a80;
}

:deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--line);
  box-shadow: none;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(224, 184, 74, 0.45);
  background: rgba(255, 255, 255, 0.055);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.1);
}

:deep(.el-input__inner) {
  color: var(--text);
  background: transparent;
  border: 0;
  box-shadow: none;
  caret-color: var(--gold-soft);
}

:deep(.el-input__inner::placeholder) {
  color: var(--faint);
}

:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus),
:deep(.el-input__inner:-webkit-autofill:active) {
  -webkit-text-fill-color: var(--text);
  caret-color: var(--gold-soft);
  box-shadow: 0 0 0 1000px #182226 inset;
  transition: background-color 9999s ease-out;
}

:deep(.el-input__prefix),
:deep(.el-input__suffix) {
  color: var(--muted);
}

:deep(.el-checkbox__label) {
  color: var(--muted);
  font-size: 13px;
}

:deep(.el-checkbox__inner) {
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--line);
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: var(--gold-soft);
  border-color: var(--gold-soft);
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner::after) {
  border-color: #131a1c;
}

.full {
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 48px;
  margin-top: 4px;
  color: #131a1c;
  font-weight: 800;
  font-size: 15px;
  border: 1px solid rgba(240, 205, 96, 0.6);
  border-radius: 10px;
  background: linear-gradient(180deg, #f0ca5c 0%, #d7a42d 100%);
  box-shadow: 0 14px 30px rgba(216, 169, 53, 0.22), inset 0 1px 0 rgba(255, 255, 255, 0.32);
}

.full:hover,
.full:focus-visible {
  color: #131a1c;
  border-color: rgba(244, 215, 120, 0.78);
  background: linear-gradient(180deg, #f6d76f 0%, #deb03a 100%);
}

.full .el-icon {
  width: 17px;
  height: 17px;
}

@keyframes light-breathe {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}

@keyframes shadow-breathe {
  0%,
  100% {
    opacity: 0.72;
    transform: scaleX(1);
  }
  50% {
    opacity: 0.5;
    transform: scaleX(0.94);
  }
}

@keyframes mug-left-float {
  0%,
  100% {
    transform: translate(6px, 31px) rotate(12deg);
  }
  50% {
    transform: translate(2px, 25px) rotate(10deg);
  }
}

@keyframes mug-right-float {
  0%,
  100% {
    transform: translate(200px, 31px) rotate(-12deg);
  }
  50% {
    transform: translate(204px, 25px) rotate(-10deg);
  }
}

@keyframes liquid-wave {
  0%,
  100% {
    transform: translateX(0) scaleX(1);
  }
  50% {
    transform: translateX(3px) scaleX(0.97);
  }
}

@keyframes liquid-shine {
  0%,
  100% {
    stroke-dashoffset: 120;
    opacity: 0.35;
  }
  45% {
    stroke-dashoffset: -40;
    opacity: 0.78;
  }
}

@keyframes beer-bubble-rise {
  0%,
  100% {
    opacity: 0.25;
    transform: translateY(4px) scale(0.82);
  }
  45% {
    opacity: 0.8;
    transform: translateY(-19px) scale(1);
  }
  75% {
    opacity: 0;
    transform: translateY(-32px) scale(0.65);
  }
}

@keyframes foam-settle {
  0%,
  100% {
    transform: scale(1) translateY(0);
  }
  50% {
    transform: scale(1.01, 1.03) translateY(-1px);
  }
}

@keyframes toast-left {
  0% {
    transform: translate(0, 0) rotate(0deg);
    animation-timing-function: cubic-bezier(0.22, 0.78, 0.28, 1);
  }
  44% {
    transform: translate(25px, -6px) rotate(-8deg);
    animation-timing-function: cubic-bezier(0.64, 0, 0.84, 0.2);
  }
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
}

@keyframes toast-right {
  0% {
    transform: translate(0, 0) rotate(0deg);
    animation-timing-function: cubic-bezier(0.22, 0.78, 0.28, 1);
  }
  44% {
    transform: translate(-31px, -6px) rotate(8deg);
    animation-timing-function: cubic-bezier(0.64, 0, 0.84, 0.2);
  }
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
}

@keyframes toast-burst {
  0% {
    opacity: 0;
    transform: scale(0.42);
  }
  24% {
    opacity: 1;
    transform: scale(0.82);
  }
  66% {
    opacity: 0.76;
    transform: scale(1.12);
  }
  100% {
    opacity: 0;
    transform: scale(1.56);
  }
}

@keyframes foam-pop {
  0%,
  100% {
    transform: scale(1) translateY(0);
  }
  38% {
    transform: scale(1.04, 1.12) translateY(-3px);
  }
  72% {
    transform: scale(0.99, 1.03) translateY(0);
  }
}

@keyframes shadow-toast {
  0%,
  100% {
    opacity: 0.72;
    transform: scaleX(1);
  }
  52% {
    opacity: 0.95;
    transform: scaleX(0.82);
  }
}

@keyframes rays-breathe {
  0%,
  100% {
    opacity: 0.25;
    transform: scale(0.98);
  }
  50% {
    opacity: 0.42;
    transform: scale(1.02);
  }
}

@keyframes ray-shimmer {
  0%,
  100% {
    opacity: 0.7;
    transform: scaleY(0.96);
  }
  50% {
    opacity: 1;
    transform: scaleY(1.04);
  }
}

@keyframes celebration-bubble-float {
  0%,
  100% {
    opacity: 0.35;
    transform: translateY(3px) scale(0.94);
  }
  50% {
    opacity: 0.9;
    transform: translateY(-6px) scale(1);
  }
}

.login-page.is-submitting .mug-motion-left {
  animation: toast-left 720ms linear both;
}

.login-page.is-submitting .mug-motion-right {
  animation: toast-right 720ms linear both;
}

.login-page.is-submitting .toast-burst {
  animation: toast-burst 520ms 130ms cubic-bezier(0.22, 0.78, 0.28, 1) both;
}

.login-page.is-submitting .celebration-rays {
  animation: rays-burst 520ms 80ms cubic-bezier(0.22, 0.78, 0.28, 1) both;
}

.login-page.is-submitting .celebration-bubbles {
  animation: bubbles-burst 600ms 90ms cubic-bezier(0.22, 0.78, 0.28, 1) both;
}

.login-page.is-submitting .foam {
  animation: foam-pop 540ms 120ms cubic-bezier(0.22, 0.78, 0.28, 1) both;
}

.login-page.is-submitting .ground-shadow {
  animation: shadow-toast 720ms cubic-bezier(0.22, 0.78, 0.28, 1) both;
}

@keyframes rays-burst {
  0% {
    opacity: 0.18;
    transform: scale(0.86);
  }
  35% {
    opacity: 0.96;
    transform: scale(1.06);
  }
  100% {
    opacity: 0;
    transform: scale(1.18);
  }
}

@keyframes bubbles-burst {
  0% {
    opacity: 0.3;
    transform: translateY(0) scale(0.86);
  }
  38% {
    opacity: 0.9;
    transform: translateY(-9px) scale(1.08);
  }
  100% {
    opacity: 0;
    transform: translateY(-20px) scale(0.72);
  }
}

@media (prefers-reduced-motion: reduce) {
  .brew-light,
  .ground-shadow,
  .mug,
  .mug-motion,
  .beer-wave,
  .mug-liquid-shine,
  .beer-bubble,
  .foam,
  .toast-burst,
  .celebration-rays,
  .ray,
  .celebration-bubble {
    animation: none !important;
  }

  .toast-burst {
    opacity: 0;
  }
}

@media (max-width: 900px) {
  .login-page {
    padding: 18px;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    gap: 22px;
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }

  .brew-visual {
    display: none;
  }
}

@media (max-width: 560px) {
  .login-page {
    align-items: stretch;
    padding: 0;
  }

  .login-shell {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
  }

  .brand-panel,
  .login-card {
    padding: 26px 22px;
  }

  .brand-text h1,
  .card-head h2 {
    font-size: 24px;
  }
}
</style>
