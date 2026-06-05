<template>
  <main class="login-shell">
    <section class="login-card">
      <p class="eyebrow dark-text">评审注册</p>
      <h1>创建评审账号</h1>
      <p class="login-note">先验证手机号，下一步填写姓名和资质信息。</p>

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

      <button class="button primary full submit-button" :disabled="!canSubmit" @click="submit">继续填写资料</button>
      <button class="button secondary full login-link" type="button" @click="router.push('/login')">已有账号，去登录</button>
      <p v-if="message" class="form-message">{{ message }}</p>
    </section>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, sendSmsCode } from '@/api/judge'
import { setSession } from '@/utils/auth'

const router = useRouter()
const message = ref('')
const form = reactive({ phone: '', code: '' })
const canSubmit = computed(() => form.phone.length >= 11 && form.code.length >= 4)

async function send() {
  await sendSmsCode({ phone: form.phone, bizType: 'JUDGE_REGISTER' })
  message.value = '验证码已发送，请查看手机短信'
}

async function submit() {
  const data = await register(form)
  setSession(data.token, data.displayName)
  router.push('/profile/edit')
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 18px;
  background:
    linear-gradient(160deg, rgba(31, 42, 55, 0.95), rgba(70, 75, 55, 0.92)),
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

.dark-text {
  color: #a75517;
  font-weight: 800;
}

h1 {
  margin: 0;
  color: #18222f;
  font-size: 28px;
  letter-spacing: 0;
}

.login-note {
  margin: 8px 0 18px;
  color: #667085;
  line-height: 1.5;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.submit-button {
  margin-top: 18px;
}

.login-link {
  margin-top: 10px;
}

.form-message {
  margin: 12px 0 0;
  color: #067647;
  font-size: 14px;
}
</style>
