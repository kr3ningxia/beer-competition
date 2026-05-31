<template>
  <div class="page">
    <section class="card">
      <h1>评分提交</h1>
      <p>当前酒款：{{ uuid }}</p>
      <label>
        角色
        <select v-model="form.judgeRoleType">
          <option value="PROFESSIONAL">专业评审</option>
          <option value="CROSS">跨界评审</option>
        </select>
      </label>
      <label v-for="item in form.dimensions" :key="item.key">
        {{ item.label }}（满分 {{ item.maxScore }}）
        <input v-model.number="item.score" type="number" step="0.1" />
      </label>
      <label>
        总分
        <input v-model.number="form.totalScore" type="number" step="0.1" />
      </label>
      <label>
        评语
        <textarea v-model="form.comments" rows="5" />
      </label>
      <div class="actions">
        <button class="primary" @click="submit">提交评分</button>
        <button class="ghost" @click="$router.push(`/scan-result/${uuid}`)">返回酒款</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRoute } from 'vue-router'
import { createScore } from '@/api/judge'

const route = useRoute()
const uuid = route.params.uuid

const form = reactive({
  beerUuid: uuid,
  judgeRoleType: 'PROFESSIONAL',
  dimensions: [
    { key: 'aroma', label: '香气', score: 10, maxScore: 12 },
    { key: 'appearance', label: '外观', score: 2, maxScore: 3 },
    { key: 'flavor', label: '味道', score: 16, maxScore: 20 },
    { key: 'mouthfeel', label: '口感', score: 4, maxScore: 5 },
    { key: 'overall', label: '整体印象', score: 8, maxScore: 10 },
  ],
  totalScore: 40,
  comments: '香气表现完整，入口干净，余味收束较好。',
})

async function submit() {
  await createScore(form)
  window.alert('评分已提交')
}
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

label {
  display: block;
  margin-top: 14px;
  color: #475569;
}

input,
textarea,
select {
  width: 100%;
  margin-top: 8px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px;
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
