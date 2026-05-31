<template>
  <div class="page">
    <section class="card">
      <h1>个人资料</h1>
      <p>姓名：{{ me?.displayName || '-' }}</p>
      <p>手机号：{{ me?.phone || '-' }}</p>
      <div class="actions">
        <button class="ghost" @click="$router.push('/competitions')">返回比赛列表</button>
        <button class="danger" @click="logout">退出登录</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMe } from '@/api/judge'
import { clearSession } from '@/utils/auth'

const router = useRouter()
const me = ref(null)

function logout() {
  clearSession()
  router.push('/login')
}

onMounted(async () => {
  me.value = await fetchMe()
})
</script>

<style scoped>
.page {
  padding: 20px;
}

.card {
  background: #fff;
  border-radius: 18px;
  padding: 20px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

button {
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
}

.ghost {
  background: #f1f5f9;
}

.danger {
  background: #ef4444;
  color: #fff;
}
</style>
