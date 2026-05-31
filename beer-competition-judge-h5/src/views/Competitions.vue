<template>
  <div class="page">
    <header class="topbar">
      <div>
        <h1>我的比赛</h1>
        <p>{{ displayName || '评审账号' }}</p>
      </div>
      <div class="top-actions">
        <button class="ghost" @click="$router.push('/profile')">个人资料</button>
      </div>
    </header>

    <section class="card uuid-card">
      <label>手动输入酒款 UUID</label>
      <div class="inline-row">
        <input v-model="manualUuid" placeholder="例如 BC-2026-IPA-0001" />
        <button class="primary" @click="goToScanResult">查看</button>
      </div>
    </section>

    <section class="list">
      <article v-for="item in competitions" :key="item.id" class="card competition-card">
        <h2>{{ item.name }}</h2>
        <p>比赛日期：{{ item.competitionDate }}</p>
        <p>状态：{{ item.status }}</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCompetitions } from '@/api/judge'
import { getDisplayName } from '@/utils/auth'

const router = useRouter()
const competitions = ref([])
const manualUuid = ref('BC-2026-IPA-0001')
const displayName = getDisplayName()

function goToScanResult() {
  if (!manualUuid.value) return
  router.push(`/scan-result/${manualUuid.value}`)
}

onMounted(async () => {
  competitions.value = await fetchCompetitions()
})
</script>

<style scoped>
.page {
  padding: 20px;
}

.topbar,
.inline-row {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: center;
}

.card {
  background: #fff;
  border-radius: 18px;
  padding: 18px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.uuid-card {
  margin: 18px 0;
}

label {
  display: block;
  color: #475569;
  margin-bottom: 8px;
}

input,
textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px;
}

button {
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
}

.primary {
  background: #d97706;
  color: #fff;
}

.ghost {
  background: #f1f5f9;
}

.list {
  display: grid;
  gap: 14px;
}
</style>
