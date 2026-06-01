<template>
  <main class="login-shell">
    <section class="login-card">
      <p class="eyebrow dark-text">啤酒大赛现场评审</p>
      <h1>评审登录</h1>
      <p class="login-note">已提交资料的评审可用手机号验证码进入。</p>

      <div class="quick-roles" aria-label="选择体验身份">
        <button
          v-for="item in demoAccounts"
          :key="item.phone"
          type="button"
          :class="['role-chip', { active: form.phone === item.phone }]"
          @click="pickDemo(item)"
        >
          <strong>{{ item.role }}</strong>
          <span>{{ item.name }}</span>
        </button>
      </div>

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
import { useRouter } from 'vue-router'
import { login, sendSmsCode } from '@/api/judge'
import { setSession } from '@/utils/auth'

const router = useRouter()
const message = ref('')

const demoAccounts = [
  { role: '专业评委', name: '林晓峰', phone: '13800000011' },
  { role: '大众评委', name: '陈可', phone: '13800000012' },
  { role: '桌长', name: '周明', phone: '13800000013' },
]

const form = reactive({ phone: demoAccounts[0].phone, code: '123456' })
const canSubmit = computed(() => form.phone.length >= 11 && form.code.length >= 4)

function pickDemo(item) {
  form.phone = item.phone
  form.code = '123456'
  message.value = `已切换为${item.role}账号`
}

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
  router.push('/competitions')
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

.quick-roles {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.role-chip {
  min-height: 66px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  padding: 9px 6px;
  color: #344054;
  background: #f7f8f6;
  text-align: left;
}

.role-chip strong,
.role-chip span {
  display: block;
}

.role-chip strong {
  font-size: 14px;
}

.role-chip span {
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
}

.role-chip.active {
  border-color: #a75517;
  color: #8a3f10;
  background: #fff3e8;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
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
