<template>
  <main class="app-shell">
    <section class="top-panel">
      <button class="back-link" type="button" @click="$router.push(`/scan-result/${uuid}`)">返回酒款</button>
      <p class="eyebrow">{{ config?.roleLabel || me?.roleLabel }}评分表</p>
      <h1 class="page-title">{{ uuid }}</h1>
      <div class="total-strip">
        <span>当前总分</span>
        <strong>{{ totalScore }} / 50</strong>
      </div>
    </section>

    <section v-if="entry && config" class="card">
      <div class="entry-summary">
        <span>{{ entry.categoryName }}</span>
        <strong>{{ entry.style }} · {{ entry.abv }}</strong>
      </div>

      <div class="dimension-list">
        <article v-for="item in form.dimensions" :key="item.key" class="dimension-card">
          <div class="split">
            <div>
              <h2>{{ item.label }}</h2>
              <p>满分 {{ item.maxScore }}</p>
            </div>
            <input
              v-model.number="item.score"
              class="score-input"
              type="number"
              inputmode="decimal"
              min="0"
              :max="item.maxScore"
              step="0.5"
              @blur="clampScore(item)"
            />
          </div>
          <input
            v-model.number="item.score"
            class="range"
            type="range"
            min="0"
            :max="item.maxScore"
            step="0.5"
          />
        </article>
      </div>

      <label class="field">
        评语
        <textarea
          v-model.trim="form.comments"
          class="textarea"
          :placeholder="commentPlaceholder"
        />
      </label>
      <div class="split">
        <p :class="['caption', commentReady ? 'ok-text' : 'warn-text']">
          {{ form.comments.length }} / {{ config.commentMinLength }} 字
        </p>
        <span v-if="existingScore" class="pill status-warn">已提交过</span>
      </div>
    </section>

    <div class="sticky-actions">
      <button class="button primary full" type="button" :disabled="!canSubmit" @click="submit">
        {{ existingScore ? '保存修改' : '提交评分' }}
      </button>
      <p v-if="message" class="message">{{ message }}</p>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createScore, fetchEntry, fetchMe, fetchMyScore, fetchScoreConfig, updateScore } from '@/api/judge'

const route = useRoute()
const router = useRouter()
const uuid = String(route.params.uuid || '').toUpperCase()
const me = ref(null)
const entry = ref(null)
const config = ref(null)
const existingScore = ref(null)
const message = ref('')

const form = reactive({
  beerUuid: uuid,
  judgeRoleType: '',
  dimensions: [],
  comments: '',
})

const totalScore = computed(() => (
  form.dimensions.reduce((sum, item) => sum + Number(item.score || 0), 0).toFixed(1)
))

const commentReady = computed(() => form.comments.length >= Number(config.value?.commentMinLength || 0))
const canSubmit = computed(() => (
  form.dimensions.length > 0
  && form.dimensions.every((item) => item.score !== '' && Number(item.score) >= 0 && Number(item.score) <= item.maxScore)
  && commentReady.value
))
const commentPlaceholder = computed(() => (
  me.value?.role === 'CROSS'
    ? '请写下第一印象、是否愿意推荐、适合什么饮用场景。'
    : '请写下香气、味道、口感和整体完成度，方便参赛方查看反馈。'
))

function clampScore(item) {
  const value = Number(item.score || 0)
  item.score = Math.min(item.maxScore, Math.max(0, value))
}

async function submit() {
  const payload = {
    ...form,
    totalScore: Number(totalScore.value),
    dimensions: form.dimensions.map((item) => ({ ...item, score: Number(item.score || 0) })),
  }
  if (existingScore.value) {
    await updateScore(existingScore.value.id, payload)
    message.value = '修改已保存'
  } else {
    await createScore(payload)
    message.value = '评分已提交'
  }
  window.setTimeout(() => router.push('/judged'), 360)
}

onMounted(async () => {
  me.value = await fetchMe()
  entry.value = await fetchEntry(uuid)
  config.value = await fetchScoreConfig(me.value.role)
  existingScore.value = await fetchMyScore(uuid)
  form.judgeRoleType = me.value.role

  if (existingScore.value) {
    form.dimensions = existingScore.value.dimensions.map((item) => ({ ...item }))
    form.comments = existingScore.value.comments
  } else {
    form.dimensions = config.value.dimensions.map((item) => ({ ...item, score: 0 }))
  }
})
</script>

<style scoped>
.back-link {
  border: 0;
  margin: 0 0 14px;
  padding: 0;
  color: rgba(255, 255, 255, 0.74);
  background: transparent;
  font-weight: 750;
}

.total-strip {
  display: flex;
  justify-content: space-between;
  align-items: end;
  margin-top: 16px;
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
}

.total-strip span {
  color: rgba(248, 250, 252, 0.7);
}

.total-strip strong {
  color: #fff;
  font-size: 24px;
}

.entry-summary {
  display: grid;
  gap: 5px;
  border-radius: 8px;
  padding: 12px;
  background: #f7f8f6;
}

.entry-summary span {
  color: #667085;
  font-size: 13px;
}

.dimension-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.dimension-card {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.dimension-card h2 {
  margin: 0;
  color: #18222f;
  font-size: 17px;
}

.dimension-card p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.score-input {
  width: 82px;
  min-height: 44px;
  border: 1px solid #cbd5d1;
  border-radius: 8px;
  color: #18222f;
  text-align: center;
  font-size: 18px;
  font-weight: 850;
}

.range {
  width: 100%;
  margin-top: 12px;
  accent-color: #a75517;
}

.ok-text {
  color: #067647;
}

.warn-text {
  color: #b54708;
}

.sticky-actions {
  position: sticky;
  bottom: 12px;
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 10px;
  border: 1px solid rgba(24, 34, 47, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(24, 34, 47, 0.12);
}

.message {
  margin: 0;
  color: #067647;
  text-align: center;
  font-size: 14px;
  font-weight: 750;
}
</style>
