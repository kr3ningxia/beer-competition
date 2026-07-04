<template>
  <main class="app-shell confirmation-shell">
    <section class="top-panel confirmation-top">
      <button class="back-link" type="button" @click="$router.push('/competitions')">
        <span class="back-icon" aria-hidden="true"></span>
        返回任务
      </button>
      <p class="eyebrow">本桌排序核对</p>
      <h1 class="page-title">{{ confirmation?.tableName || '评审桌' }}</h1>
      <div class="confirmation-progress">
        <span>同桌确认</span>
        <strong>{{ confirmedCount }} / {{ requiredCount }}</strong>
      </div>
    </section>

    <section class="card confirmation-card">
      <div class="split">
        <div>
          <h2 class="section-title compact">本桌排序结果</h2>
          <p class="confirmation-hint">{{ stateText }}</p>
        </div>
        <span :class="['pill', mineConfirmed ? 'status-ok' : 'status-warn']">{{ mineConfirmed ? '已确认' : '待确认' }}</span>
      </div>

      <div class="confirmation-list">
        <article v-for="slot in slots" :key="slot.rank" class="confirmation-entry">
          <div class="entry-main">
            <strong>{{ displaySlotLabel(slot) }}</strong>
            <span>{{ displayShortCode(slot) }}</span>
          </div>
          <p class="entry-meta">{{ [slot.categoryName, slot.style].filter(Boolean).join(' · ') || '-' }}</p>
        </article>
      </div>

      <button class="button primary full confirm-button" type="button" :disabled="!canConfirm" @click="submitConfirm">
        {{ confirmButtonText }}
      </button>
      <p v-if="message" class="message">{{ message }}</p>
    </section>

    <JudgeBottomNav :role="me?.role" :hide-table="true" />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { confirmRankingRoundTable, fetchMe, fetchRankingConfirmation } from '@/api/judge'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'

const route = useRoute()
const me = ref(null)
const confirmation = ref(null)
const submitting = ref(false)
const message = ref('')

const slots = computed(() => confirmation.value?.slots || [])
const confirmedCount = computed(() => Number(confirmation.value?.confirmedCount || 0))
const requiredCount = computed(() => Number(confirmation.value?.requiredCount || 0))
const mineConfirmed = computed(() => Boolean(confirmation.value?.mineConfirmed))
const readyForConfirmation = computed(() => Boolean(confirmation.value?.readyForConfirmation))
const tableSubmitted = computed(() => ['SUBMITTED', 'LOCKED'].includes(confirmation.value?.status))
const canConfirm = computed(() => readyForConfirmation.value && !mineConfirmed.value && !tableSubmitted.value && !submitting.value)
const isMedalMode = computed(() => confirmation.value?.targetMode === 'MEDALS')
const stateText = computed(() => {
  if (tableSubmitted.value) return '本桌排序已提交，等待主办方确认轮次。'
  if (!readyForConfirmation.value) return '桌长还未提交排序结果，请稍后再核对。'
  if (mineConfirmed.value) return '你已确认本桌排序，等待同桌确认完成。'
  return '请核对本桌排序，确认完成后系统将自动提交本桌排序。'
})
const confirmButtonText = computed(() => {
  if (submitting.value) return '确认中...'
  if (tableSubmitted.value) return '本桌排序已提交'
  if (!readyForConfirmation.value) return '等待桌长提交排序'
  if (mineConfirmed.value) return '已确认本桌排序'
  return '确认同意本桌排序'
})

function displaySlotLabel(slot) {
  if (isMedalMode.value) {
    return ['第一名', '第二名', '第三名'][Number(slot?.rank) - 1] || slot?.label || '名次'
  }
  return slot?.label || `第 ${slot?.rank || ''} 名`
}

function displayShortCode(slot) {
  return slot?.shortCode || slot?.uuid || '编号待生成'
}

async function loadConfirmation() {
  confirmation.value = await fetchRankingConfirmation(route.params.roundTableId)
}

async function submitConfirm() {
  if (!canConfirm.value) return
  submitting.value = true
  message.value = ''
  try {
    confirmation.value = await confirmRankingRoundTable(route.params.roundTableId, {
      resultVersion: confirmation.value?.resultVersion,
    })
    message.value = confirmation.value?.status === 'SUBMITTED' ? '本桌排序已自动提交' : '已确认本桌排序'
  } catch (error) {
    await loadConfirmation()
    message.value = error?.response?.data?.message || error?.message || '确认失败，请重新核对后再试。'
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
  display: grid;
  gap: 8px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  color: #18222f;
  background: #fff;
}

.entry-main {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: flex-start;
}

.entry-main strong {
  flex: 0 0 auto;
  color: #9a5b26;
  font-size: 14px;
  font-weight: 900;
}

.entry-main span {
  min-width: 0;
  text-align: right;
  font-size: 15px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.entry-meta {
  margin: 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 750;
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
