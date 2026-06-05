<template>
  <main class="app-shell">
    <section class="top-panel">
      <button class="back-link" type="button" @click="$router.push('/competitions')">返回任务</button>
      <p class="eyebrow">{{ table?.roundName || '排序轮次' }}</p>
      <h1 class="page-title">{{ table?.tableName || '排序任务' }}</h1>
      <p class="ranking-mode">{{ rankingModeText }}</p>
      <div class="ranking-stats">
        <div>
          <span>候选</span>
          <strong>{{ entries.length }} 款</strong>
        </div>
        <div>
          <span>已排序</span>
          <strong>{{ filledCount }} / {{ slots.length }}</strong>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="split">
        <h2 class="section-title compact">排序结果</h2>
        <span class="pill">{{ table?.targetMode || 'TOP_N' }}</span>
      </div>
      <div class="slot-list">
        <label v-for="slot in slots" :key="slot.rank" class="slot-row">
          <span>{{ slot.label }}</span>
          <select v-model="slot.beerEntryId" :disabled="!canEdit">
            <option :value="null">待选择</option>
            <option v-for="entry in selectableEntries(slot.beerEntryId)" :key="entry.id" :value="entry.id">
              {{ displayShortCode(entry) }} · {{ entry.categoryName }} · {{ entry.style }}
            </option>
          </select>
        </label>
      </div>
      <button v-if="canEdit" class="button primary full" type="button" :disabled="!canSubmit" @click="submit">
        提交排序
      </button>
      <p v-else class="readonly-note">本轮由桌长提交排序结果，你可以查看本桌候选和已提交结果。</p>
      <p v-if="message" class="message">{{ message }}</p>
    </section>

    <section class="card">
      <div class="split">
        <h2 class="section-title compact">候选任务</h2>
        <span class="pill">{{ entries.length }} 款</span>
      </div>
      <div class="entry-list">
        <article v-for="entry in entries" :key="entry.id" class="entry-row">
          <strong>{{ displayShortCode(entry) }}</strong>
          <small>{{ entry.categoryName }} · {{ entry.style }}</small>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchRoundTable, submitRanking } from '@/api/judge'

const route = useRoute()
const router = useRouter()
const table = ref(null)
const entries = ref([])
const slots = ref([])
const message = ref('')

const filledCount = computed(() => slots.value.filter((slot) => slot.beerEntryId).length)
const canEdit = computed(() => Boolean(table.value?.canSubmitRanking))
const canSubmit = computed(() => canEdit.value && slots.value.length > 0 && filledCount.value === slots.value.length)
const rankingModeText = computed(() => {
  const count = table.value?.targetCount || slots.value.length || 0
  if (table.value?.targetMode === 'MEDALS') return '组别决战：确认金奖、银奖、铜奖'
  if (table.value?.targetMode === 'CHAMPION') return '总冠军轮：确认全场总冠军'
  return `继续筛选：选择并排序前 ${count} 款`
})

function selectableEntries(currentId) {
  const selected = new Set(slots.value.map((slot) => slot.beerEntryId).filter((id) => id && id !== currentId))
  return entries.value.filter((entry) => !selected.has(entry.id))
}

function displayShortCode(entry) {
  return entry?.shortCode ? `编号： ${entry.shortCode}` : '编号'
}

async function submit() {
  if (!window.confirm('提交后将进入后台确认，确认锁定前如需调整请联系现场工作人员。')) return
  await submitRanking(route.params.roundTableId, {
    results: slots.value.map((slot) => ({
      beerEntryId: slot.beerEntryId,
      rankNo: slot.rank,
      slotLabel: slot.label,
    })),
  })
  message.value = '排序已提交'
  window.setTimeout(() => router.push('/competitions'), 420)
}

onMounted(async () => {
  table.value = await fetchRoundTable(route.params.roundTableId)
  entries.value = table.value.entries || []
  slots.value = (table.value.rankings || []).map((slot) => ({
    ...slot,
    beerEntryId: slot.beerEntryId || null,
  }))
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

.ranking-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 16px;
}

.ranking-stats div {
  border-radius: 8px;
  padding: 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.ranking-stats span,
.ranking-stats strong,
.entry-row strong,
.entry-row small {
  display: block;
}

.ranking-stats span {
  color: rgba(248, 250, 252, 0.7);
  font-size: 12px;
}

.ranking-stats strong {
  margin-top: 5px;
  font-size: 21px;
}

.compact {
  margin-bottom: 0;
}

.slot-list,
.entry-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.slot-row {
  display: grid;
  gap: 7px;
  color: #344054;
  font-weight: 800;
}

.slot-row select {
  width: 100%;
  min-height: 44px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  padding: 0 10px;
  color: #18222f;
  background: #fff;
}

.slot-row select:disabled {
  color: #667085;
  background: #f2f4f7;
}

.ranking-mode,
.readonly-note {
  margin: 8px 0 0;
  color: rgba(248, 250, 252, 0.76);
  font-size: 13px;
  font-weight: 750;
}

.readonly-note {
  color: #667085;
}

.entry-row {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  color: #18222f;
  background: #fff;
}

.entry-row small {
  margin-top: 5px;
  color: #667085;
}

.message {
  margin: 12px 0 0;
  color: #067647;
  text-align: center;
  font-size: 14px;
  font-weight: 750;
}
</style>
