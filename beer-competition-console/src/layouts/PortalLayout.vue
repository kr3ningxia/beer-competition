<template>
  <div class="portal-shell">
    <header class="hero">
      <div>
        <h1>啤酒大赛厂商 Portal</h1>
        <p>报名、状态跟踪与结果查询会逐步都汇集在这里。</p>
      </div>
      <div class="actions">
        <span>{{ displayName || '厂商账号' }}</span>
        <el-button @click="$router.push('/portal/home')">首页</el-button>
        <el-button @click="$router.push('/portal/entries')">酒款列表</el-button>
        <el-button type="danger" plain @click="logout">退出</el-button>
      </div>
    </header>
    <main class="portal-page">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearSession, getDisplayName } from '@/utils/auth'

const router = useRouter()
const displayName = getDisplayName('portal')

function logout() {
  clearSession('portal')
  router.push('/portal/login')
}
</script>

<style scoped>
.portal-shell {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff7ed 0%, #f8fafc 48%);
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 28px 32px;
  background: #ffffffcc;
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #fed7aa;
}

.hero h1,
.hero p {
  margin: 0;
}

.hero p {
  margin-top: 8px;
  color: #92400e;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.portal-page {
  padding: 24px 32px;
}
</style>
