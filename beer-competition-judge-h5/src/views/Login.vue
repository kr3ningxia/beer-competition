<template>
  <main class="login-shell">
    <section class="login-card">
      <div class="brand-lockup">
        <img src="/brand-icon.png" alt="" aria-hidden="true">
        <span>啤酒事务局评审端</span>
      </div>
      <h1>评审登录</h1>

      <label class="field">
        手机号
        <input v-model="form.phone" class="input" inputmode="tel" placeholder="请输入手机号" />
      </label>

      <label class="field">
        验证码
        <div class="code-row">
          <input v-model="form.code" class="input" inputmode="numeric" placeholder="请输入验证码" />
          <button type="button" class="button secondary" @click="send">获取验证码</button>
        </div>
      </label>

      <button class="button primary full" :disabled="!canSubmit" @click="submit">进入评审</button>
      <button class="button secondary full register-link" type="button" @click="router.push('/register')">注册评审账号</button>
      <p v-if="message" class="form-message">{{ message }}</p>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, sendSmsCode } from '@/api/judge'
import { setSession } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const message = ref('')

const form = reactive({ phone: '', code: '' })
const canSubmit = computed(() => form.phone.length >= 11 && form.code.length >= 4)

async function send() {
  await sendSmsCode({ phone: form.phone, bizType: 'JUDGE_LOGIN' })
  message.value = '验证码已发送，请查看手机短信'
}

async function submit() {
  const data = await login(form)
  setSession(data.token, data.displayName)
  if (data.profileRequired) {
    router.push('/profile/edit')
    return
  }
  if (Number(data.status) === 2) {
    router.push('/review-status')
    return
  }
  router.push(String(route.query.redirect || '/competitions'))
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 18px;
  background:
    linear-gradient(160deg, rgba(31, 42, 55, 0.95), rgba(86, 65, 41, 0.9)),
    #1f2a37;
}

.login-card {
  width: min(100%, 430px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.28);
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #a75517;
  font-size: 14px;
  font-weight: 800;
}

.brand-lockup img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

h1 {
  margin: 0;
  color: #18222f;
  font-size: 28px;
  letter-spacing: 0;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) max-content;
  gap: 8px;
  margin-top: 8px;
}

.code-row .input {
  min-width: 0;
  margin-top: 0;
}

.code-row .button {
  white-space: nowrap;
}

.login-card > .button.full {
  margin-top: 10px;
}

.form-message {
  margin: 12px 0 0;
  color: #067647;
  font-size: 14px;
}

.register-link {
  margin-top: 10px;
}
</style>
