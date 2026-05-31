<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>主办方后台登录</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" class="full" @click="submit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { adminLogin } from '@/api/auth'
import { setSession } from '@/utils/auth'

const router = useRouter()
const form = reactive({ username: 'admin', password: '123456' })

async function submit() {
  const data = await adminLogin(form)
  setSession('admin', data.token, data.displayName)
  ElMessage.success('登录成功')
  router.push('/admin/dashboard')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at top, #dbeafe, #eff6ff 42%, #f8fafc);
}

.login-card {
  width: 420px;
}

.full {
  width: 100%;
}
</style>
