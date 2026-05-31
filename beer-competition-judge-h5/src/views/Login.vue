<template>
  <div class="page login-page">
    <section class="card">
      <h1>评审登录</h1>
      <p>比赛当天用手机号和验证码登录，开发环境验证码固定为 123456。</p>
      <label>
        手机号
        <input v-model="form.phone" />
      </label>
      <label>
        验证码
        <div class="inline-row">
          <input v-model="form.code" />
          <button type="button" class="ghost" @click="send">发送</button>
        </div>
      </label>
      <button class="primary" @click="submit">进入评审端</button>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, sendSmsCode } from '@/api/judge'
import { setSession } from '@/utils/auth'

const router = useRouter()
const form = reactive({ phone: '13800000011', code: '123456' })

async function send() {
  await sendSmsCode({ phone: form.phone, bizType: 'JUDGE_LOGIN' })
  window.alert('验证码已发送')
}

async function submit() {
  const data = await login(form)
  setSession(data.token, data.displayName)
  router.push('/competitions')
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 20px;
}

.card {
  width: min(100%, 420px);
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
}

label {
  display: block;
  margin-top: 16px;
  color: #475569;
}

input,
textarea {
  width: 100%;
  margin-top: 8px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px 14px;
}

.inline-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

button {
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
}

.primary {
  width: 100%;
  margin-top: 20px;
  background: #d97706;
  color: #fff;
}

.ghost {
  background: #fef3c7;
  color: #92400e;
}
</style>
