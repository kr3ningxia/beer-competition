<template>
  <Teleport to="body">
    <Transition name="wechat-notice">
      <div v-if="visible" class="wechat-notice" role="dialog" aria-modal="true" aria-labelledby="wechat-notice-title">
        <div class="wechat-notice-backdrop" aria-hidden="true"></div>

        <div class="menu-pointer" aria-hidden="true">
          <span class="menu-pointer-label">点击右上角</span>
          <span class="menu-dots"><i></i><i></i><i></i></span>
          <span class="menu-pointer-line"></span>
        </div>

        <section class="wechat-notice-sheet">
          <div class="notice-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <rect x="3.25" y="4.5" width="17.5" height="15" rx="2.25" />
              <path d="M3.75 8.5h16.5M7 6.5h.01M10 6.5h.01" />
              <path d="m8.5 13 2 2 4-4" />
            </svg>
          </div>

          <h2 id="wechat-notice-title">建议使用浏览器打开</h2>
          <p class="notice-description">微信内置浏览器可能影响摄像头扫码和评分提交。请点击右上角「···」，选择「在浏览器中打开」，再登录评审端。</p>

          <div class="notice-step" aria-label="操作步骤">
            <div class="notice-step-item">
              <span class="step-number">1</span>
              <span>点击右上角「···」</span>
            </div>
            <span class="step-arrow" aria-hidden="true">→</span>
            <div class="notice-step-item">
              <span class="step-number">2</span>
              <span>选择「在浏览器中打开」</span>
            </div>
          </div>

          <button type="button" class="continue-button" @click="continueInWechat">继续使用微信</button>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const DISMISSED_KEY = 'judge_wechat_browser_notice_dismissed'
const route = useRoute()
const isWechatBrowser = ref(false)
const dismissed = ref(false)

const visible = computed(() => isWechatBrowser.value && route.path === '/login' && !dismissed.value)

onMounted(() => {
  isWechatBrowser.value = /MicroMessenger/i.test(navigator.userAgent || '')
  dismissed.value = readDismissedState()
})

function continueInWechat() {
  dismissed.value = true
  try {
    sessionStorage.setItem(DISMISSED_KEY, '1')
  } catch {
    // Storage may be unavailable in privacy-restricted browser contexts.
  }
}

function readDismissedState() {
  try {
    return sessionStorage.getItem(DISMISSED_KEY) === '1'
  } catch {
    return false
  }
}
</script>

<style scoped>
.wechat-notice,
.wechat-notice-backdrop {
  position: fixed;
  inset: 0;
}

.wechat-notice {
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  overflow: hidden;
  padding: 0 14px max(14px, env(safe-area-inset-bottom));
  color: #18222f;
}

.wechat-notice-backdrop {
  background: rgba(12, 19, 26, 0.78);
  backdrop-filter: blur(3px);
}

.wechat-notice-sheet {
  position: relative;
  z-index: 1;
  width: min(100%, 492px);
  padding: 24px 20px 18px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  background: #fffdf9;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.34);
}

.notice-mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  margin-bottom: 16px;
  border-radius: 12px;
  color: #a75517;
  background: #f6e7d9;
}

.notice-mark svg {
  width: 23px;
  height: 23px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

h2 {
  margin: 0;
  color: #18222f;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0;
}

.notice-description {
  margin: 10px 0 0;
  color: #5c6673;
  font-size: 15px;
  line-height: 1.65;
}

.notice-step {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 18px;
  padding: 12px;
  border: 1px solid #eadbc9;
  border-radius: 8px;
  color: #573a23;
  background: #fff7ed;
  font-size: 13px;
  font-weight: 750;
  line-height: 1.35;
}

.notice-step-item {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
}

.step-number {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #fff;
  background: #a75517;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.step-arrow {
  flex: 0 0 auto;
  color: #c28654;
  font-size: 18px;
  font-weight: 400;
}

.continue-button {
  width: 100%;
  min-height: 46px;
  margin-top: 16px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: #1f2a37;
  font-size: 15px;
  font-weight: 800;
}

.continue-button:focus-visible {
  outline: 3px solid rgba(167, 85, 23, 0.3);
  outline-offset: 3px;
}

.continue-button:active {
  background: #101923;
}

.menu-pointer {
  position: absolute;
  top: max(18px, env(safe-area-inset-top));
  right: 20px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 9px;
  color: #fff;
  font-size: 14px;
  font-weight: 800;
}

.menu-pointer-label {
  padding: 7px 10px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 7px;
  background: rgba(24, 34, 47, 0.72);
}

.menu-dots {
  display: inline-flex;
  gap: 3px;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 2px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
}

.menu-dots i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #fff;
}

.menu-pointer-line {
  position: absolute;
  right: 13px;
  top: 36px;
  width: 2px;
  height: 43px;
  transform: rotate(-27deg);
  transform-origin: top center;
  background: #d17932;
}

.menu-pointer-line::after {
  position: absolute;
  right: -4px;
  bottom: -1px;
  width: 8px;
  height: 8px;
  border-right: 2px solid #d17932;
  border-bottom: 2px solid #d17932;
  transform: rotate(45deg);
  content: '';
}

.wechat-notice-enter-active,
.wechat-notice-leave-active {
  transition: opacity 180ms ease;
}

.wechat-notice-enter-active .wechat-notice-sheet,
.wechat-notice-leave-active .wechat-notice-sheet {
  transition: transform 220ms ease, opacity 180ms ease;
}

.wechat-notice-enter-from,
.wechat-notice-leave-to {
  opacity: 0;
}

.wechat-notice-enter-from .wechat-notice-sheet,
.wechat-notice-leave-to .wechat-notice-sheet {
  opacity: 0;
  transform: translateY(22px);
}

@media (prefers-reduced-motion: reduce) {
  .wechat-notice-enter-active,
  .wechat-notice-leave-active,
  .wechat-notice-enter-active .wechat-notice-sheet,
  .wechat-notice-leave-active .wechat-notice-sheet {
    transition: none;
  }
}

@media (max-width: 390px) {
  .wechat-notice {
    padding-right: 10px;
    padding-left: 10px;
  }

  .wechat-notice-sheet {
    padding: 20px 16px 14px;
  }

  .notice-step {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .step-arrow {
    display: none;
  }

  .menu-pointer {
    right: 12px;
  }

  .menu-pointer-label {
    font-size: 13px;
  }
}
</style>
