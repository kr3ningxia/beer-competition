<template>
  <main class="app-shell confirmation-shell">
    <section class="top-panel confirmation-top">
      <button class="back-link" type="button" @click="$router.push('/competitions')">
        <span class="back-icon" aria-hidden="true"></span>
        返回任务
      </button>
      <p class="eyebrow">本桌结果核对</p>
      <h1 class="page-title">{{ confirmation?.tableName || '评审桌' }}</h1>
      <div class="confirmation-progress">
        <span>同桌确认</span>
        <strong>{{ confirmedCount }} / {{ requiredCount }}</strong>
      </div>
    </section>

    <section class="card confirmation-card">
      <div class="split">
        <div>
          <h2 class="section-title compact">本桌最终结果</h2>
          <p class="confirmation-hint">{{ stateText }}</p>
        </div>
        <span :class="['pill', mineConfirmed ? 'status-ok' : 'status-warn']">{{ mineConfirmed ? '已确认' : '待确认' }}</span>
      </div>

      <div class="confirmation-list">
        <article v-for="entry in entries" :key="entry.beerEntryId" :class="['confirmation-entry', { advanced: !isFeedbackOnlyCompetition && entry.advanced }]">
          <div class="entry-main">
            <strong>{{ displayShortCode(entry) }}</strong>
            <span>{{ entry.categoryName || '-' }} · {{ entry.style || '-' }}</span>
          </div>
          <div class="entry-result">
            <span>共识分</span>
            <strong>{{ entry.consensusScore ?? '-' }} 分</strong>
          </div>
          <p v-if="entry.comments" class="entry-comment">{{ entry.comments }}</p>
          <em v-if="!isFeedbackOnlyCompetition">{{ entry.advanced ? '晋级' : '未晋级' }}</em>
        </article>
      </div>

      <button class="button primary full confirm-button" type="button" :disabled="!canConfirm" @click="submitConfirm">
        {{ confirmButtonText }}
      </button>
      <p v-if="message" class="message">{{ message }}</p>
    </section>

    <JudgeBottomNav :role="me?.role" />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { confirmScoreRoundTable, fetchMe, fetchScoreConfirmation } from '@/api/judge'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'

const route = useRoute()
const me = ref(null)
const confirmation = ref(null)
const submitting = ref(false)
const message = ref('')

const entries = computed(() => confirmation.value?.entries || [])
const confirmedCount = computed(() => Number(confirmation.value?.confirmedCount || 0))
const requiredCount = computed(() => Number(confirmation.value?.requiredCount || 0))
const mineConfirmed = computed(() => Boolean(confirmation.value?.mineConfirmed))
const readyForConfirmation = computed(() => Boolean(confirmation.value?.readyForConfirmation))
const isFeedbackOnlyCompetition = computed(() => confirmation.value?.competitionType === 'FEEDBACK_ONLY')
const canConfirm = computed(() => readyForConfirmation.value && !mineConfirmed.value && !submitting.value)
const stateText = computed(() => {
  if (!readyForConfirmation.value) return '桌长最终意见还未完成，请稍后再核对。'
  if (mineConfirmed.value) return '你已确认本桌结果，等待桌长最终提交。'
  return isFeedbackOnlyCompetition.value
    ? '请核对共识分和综合评语，确认后桌长才能最终提交。'
    : '请核对共识分、综合评语和晋级结果，确认后桌长才能最终提交。'
})
const confirmButtonText = computed(() => {
  if (submitting.value) return '确认中...'
  if (!readyForConfirmation.value) return '等待桌长完成结果'
  if (mineConfirmed.value) return '已确认本桌结果'
  return '确认同意本桌结果'
})

function displayShortCode(entry) {
  return entry?.shortCode ? `编号： ${entry.shortCode}` : '编号待生成'
}

async function loadConfirmation() {
  confirmation.value = await fetchScoreConfirmation(route.params.roundTableId)
}

async function submitConfirm() {
  if (!canConfirm.value) return
  submitting.value = true
  message.value = ''
  try {
    confirmation.value = await confirmScoreRoundTable(route.params.roundTableId)
    message.value = '已确认本桌结果'
  } catch {
    message.value = '确认失败，请刷新后再试。'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  me.value = await fetchMe()
  await loadConfirmation()
})
</script>

<style scoped>
.back-link {
  justify-self: start;
  display: inline-flex;
  gap: 7px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  margin: 0 0 14px;
  padding: 7px 11px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.08);
  font-size: 13px;
  font-weight: 750;
  line-height: 1;
}

.back-icon {
  width: 8px;
  height: 8px;
  border: solid currentColor;
  border-width: 0 0 2px 2px;
  transform: rotate(45deg);
}

.confirmation-top {
  display: grid;
  gap: 8px;
}

.confirmation-progress {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px;
  padding: 10px 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.confirmation-progress span,
.confirmation-progress strong {
  font-weight: 850;
}

.compact {
  margin-bottom: 0;
}

.confirmation-hint {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 700;
}

.confirmation-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.confirmation-entry {
  position: relative;
  display: grid;
  gap: 10px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  color: #18222f;
  background: #fff;
}

.confirmation-entry.advanced {
  border-color: #f3c04f;
  background: #fffaf0;
}

.entry-main,
.entry-result {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: flex-start;
}

.entry-main {
  display: grid;
  gap: 4px;
}

.entry-main strong {
  font-size: 15px;
}

.entry-main span,
.entry-result span {
  color: #667085;
  font-size: 13px;
  font-weight: 750;
}

.entry-result strong {
  color: #9a5b26;
}

.entry-comment {
  margin: 0;
  color: #475467;
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.confirmation-entry em {
  justify-self: start;
  border-radius: 999px;
  padding: 4px 8px;
  color: #7a4b12;
  background: #fff2cf;
  font-style: normal;
  font-size: 12px;
  font-weight: 850;
}

.confirm-button {
  margin-top: 16px;
}

.message {
  margin: 12px 0 0;
  color: #067647;
  text-align: center;
  font-size: 14px;
  font-weight: 750;
}
</style>
