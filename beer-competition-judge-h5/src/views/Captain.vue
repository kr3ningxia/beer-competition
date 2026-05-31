<template>
  <div class="page">
    <section class="card">
      <h1>桌长汇总</h1>
      <p>酒款 UUID：{{ uuid }}</p>
      <button class="ghost" @click="loadScores">刷新组内评分</button>
      <ul class="score-list">
        <li v-for="item in tableScores" :key="item.id">
          {{ item.judgeRoleType }} / {{ item.totalScore }} 分 / {{ item.comments }}
        </li>
      </ul>

      <label>
        共识评分
        <input v-model.number="form.consensusScore" type="number" step="0.1" />
      </label>
      <label>
        综合评语
        <textarea v-model="form.comments" rows="5" />
      </label>
      <label class="checkbox-row">
        <input v-model="form.advanced" type="checkbox" />
        是否晋级
      </label>
      <button class="primary" @click="submit">提交最终分</button>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchTableScores, finalizeTableScore } from '@/api/judge'

const route = useRoute()
const uuid = route.params.uuid
const tableScores = ref([])
const form = reactive({
  dimensions: [{ key: 'consensus', label: '共识评分', score: 42, maxScore: 50 }],
  consensusScore: 42,
  comments: '组内意见较一致，香气和风味完整度较好。',
  advanced: true,
})

async function loadScores() {
  tableScores.value = await fetchTableScores(uuid)
}

async function submit() {
  form.dimensions[0].score = form.consensusScore
  await finalizeTableScore(uuid, form)
  window.alert('桌长最终分已提交')
}

onMounted(loadScores)
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

.score-list {
  padding-left: 18px;
}

label {
  display: block;
  margin-top: 14px;
}

input,
textarea {
  width: 100%;
  margin-top: 8px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 12px;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.checkbox-row input {
  width: auto;
  margin-top: 0;
}

button {
  border: none;
  border-radius: 12px;
  padding: 12px 16px;
}

.primary {
  width: 100%;
  margin-top: 18px;
  background: #d97706;
  color: #fff;
}

.ghost {
  background: #f1f5f9;
}
</style>
