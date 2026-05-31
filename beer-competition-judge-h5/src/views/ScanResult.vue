<template>
  <div class="page">
    <button class="ghost" @click="$router.push('/competitions')">返回比赛列表</button>
    <section class="card" v-if="entry">
      <h1>{{ entry.uuid }}</h1>
      <p>组别：{{ entry.categoryName }}</p>
      <p>风格：{{ entry.style }}</p>
      <p>ABV：{{ entry.abv }}</p>
      <p>简介：{{ entry.description }}</p>
      <ul>
        <li v-for="field in entry.extraFields" :key="field.key">{{ field.label }}：{{ field.value }}</li>
      </ul>
      <div class="actions">
        <button class="primary" @click="$router.push(`/score/${entry.uuid}`)">去评分</button>
        <button class="ghost" @click="$router.push(`/captain/${entry.uuid}`)">桌长汇总</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchEntry } from '@/api/judge'

const route = useRoute()
const entry = ref(null)

onMounted(async () => {
  entry.value = await fetchEntry(route.params.uuid)
})
</script>

<style scoped>
.page {
  padding: 20px;
}

.card {
  margin-top: 16px;
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

.primary {
  background: #d97706;
  color: #fff;
}

.ghost {
  background: #f1f5f9;
}
</style>
