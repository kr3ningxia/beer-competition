<template>
  <div class="portal-login">
    <el-card class="login-card">
      <h2>厂商 Portal 登录</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="sms-row">
            <el-input v-model="form.code" />
            <el-button @click="send">发送验证码</el-button>
          </div>
        </el-form-item>
        <el-alert type="info" :closable="false" title="本地开发默认验证码为 123456" />
        <el-button type="primary" class="full" @click="submit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { portalLogin, sendSmsCode } from '@/api/auth'
import { setSession } from '@/utils/auth'

const router = useRouter()
const form = reactive({ phone: '13800000001', code: '123456' })

async function send() {
  await sendSmsCode({ phone: form.phone, bizType: 'PORTAL_LOGIN' })
  ElMessage.success('验证码已发送')
}

async function submit() {
  const data = await portalLogin(form)
  setSession('portal', data.token, data.displayName)
  ElMessage.success('登录成功')
  router.push('/portal/home')
}
</script>

<style scoped>
.portal-login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, #fffbeb, #fff7ed 40%, #f8fafc);
}

.login-card {
  width: 460px;
}

.sms-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.full {
  width: 100%;
  margin-top: 16px;
}
</style>
