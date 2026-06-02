<template>
  <main class="app-shell">
    <section class="top-panel">
      <button v-if="uuid" class="back-link" type="button" @click="$router.push('/captain')">返回本桌</button>
      <p class="eyebrow">桌长工作台</p>
      <h1 class="page-title">{{ uuid || `${me?.tableName || 'A 桌'}评分汇总` }}</h1>
      <div class="captain-stats">
        <div>
          <span>已确认</span>
          <strong>{{ finalizedCount }} / {{ boardEntries.length }}</strong>
        </div>
        <div>
          <span>已选晋级</span>
          <strong>{{ advancedUuids.length }} 款</strong>
        </div>
      </div>
    </section>

    <template v-if="!uuid">
      <section class="card">
        <div class="split">
          <h2 class="section-title compact">本轮酒款</h2>
          <span class="pill status-warn">{{ board?.competition?.flightName }}</span>
        </div>
        <div class="board-list">
          <article v-for="entry in boardEntries" :key="entry.uuid" class="board-row">
            <button type="button" @click="$router.push(`/captain/${entry.uuid}`)">
              <span>
                <strong>{{ entry.uuid }}</strong>
                <small>{{ entry.categoryName }} · {{ entry.style }}</small>
              </span>
              <em :class="['pill', entry.finalized ? 'status-lock' : 'status-warn']">
                {{ entry.finalized ? `${entry.finalScore} 分` : `${entry.submittedCount}/${entry.expectedCount} 人已评` }}
              </em>
            </button>
            <label :class="['advance-check', { disabled: !entry.finalized }]">
              <input
                v-model="advancedUuids"
                type="checkbox"
                :value="entry.uuid"
                :disabled="!entry.finalized"
              />
              加入晋级名单
            </label>
          </article>
        </div>
      </section>

      <section class="card">
        <h2 class="section-title">确认晋级名单</h2>
        <p :class="['caption', advanceReady ? 'ok-text' : 'warn-text']">
          请选择 2-4 款进入下一轮，当前已选择 {{ advancedUuids.length }} 款。
        </p>
        <button class="button primary full" type="button" :disabled="!advanceReady" @click="saveAdvanced">
          确认晋级名单
        </button>
        <p v-if="message" class="message">{{ message }}</p>
      </section>
    </template>

    <template v-else>
      <section class="card">
        <div v-if="entry" class="entry-summary">
          <span>{{ entry.categoryName }}</span>
          <strong>{{ entry.style }} · {{ entry.abv }}</strong>
        </div>

        <h2 class="section-title score-title">同桌评分</h2>
        <div class="member-list">
          <article v-for="score in tableScores" :key="score.id" class="member-card">
            <div class="split">
              <div>
                <h3>{{ score.judgeName }}</h3>
                <p>{{ score.roleLabel }}</p>
              </div>
              <strong>{{ score.totalScore }} 分</strong>
            </div>
            <div class="dimension-notes">
              <div v-for="dim in score.dimensions" :key="dim.key">
                <span>{{ dim.label }} {{ dim.score }}/{{ dim.maxScore }}</span>
                <p v-if="dim.note">{{ dim.note }}</p>
              </div>
            </div>
            <p class="member-comment">{{ score.comments }}</p>
          </article>
        </div>
      </section>

      <section class="card">
        <h2 class="section-title">本桌最终意见</h2>
        <label class="field">
          共识分
          <input v-model.number="form.consensusScore" class="input" type="number" inputmode="numeric" min="0" max="50" step="1" />
        </label>
        <label class="field">
          综合评语
          <textarea
            v-model.trim="form.comments"
            class="textarea"
            placeholder="请写下本桌讨论后的综合意见，作为参赛方最终可见反馈。"
          />
        </label>
        <label class="advance-line">
          <input v-model="form.advanced" type="checkbox" />
          加入晋级名单
        </label>
        <button class="button primary full" type="button" :disabled="!canFinalize" @click="submitFinal">
          确认本桌评分
        </button>
        <p v-if="message" class="message">{{ message }}</p>
      </section>
    </template>

    <nav class="bottom-nav">
      <router-link class="nav-item" to="/competitions">扫码</router-link>
      <router-link class="nav-item" to="/judged">已评</router-link>
      <router-link class="nav-item" to="/captain">本桌</router-link>
      <router-link class="nav-item" to="/profile">我的</router-link>
    </nav>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchCaptainBoard,
  fetchEntry,
  fetchMe,
  fetchTableScores,
  finalizeTableScore,
} from '@/api/judge'

const route = useRoute()
const router = useRouter()
const uuid = computed(() => route.params.uuid ? String(route.params.uuid).toUpperCase() : '')
const me = ref(null)
const board = ref(null)
const entry = ref(null)
const tableScores = ref([])
const advancedUuids = ref([])
const message = ref('')
const form = reactive({
  consensusScore: 0,
  comments: '',
  advanced: false,
})

const boardEntries = computed(() => board.value?.entries || [])
const finalizedCount = computed(() => boardEntries.value.filter((item) => item.finalized).length)
const advanceReady = computed(() => advancedUuids.value.length >= 2 && advancedUuids.value.length <= 4)
const canFinalize = computed(() => (
  Number.isInteger(Number(form.consensusScore))
  && Number(form.consensusScore) >= 0
  && Number(form.consensusScore) <= 50
  && form.comments.length >= 20
))

async function loadBoard() {
  board.value = await fetchCaptainBoard()
  advancedUuids.value = board.value.entries.filter((item) => item.advanced).map((item) => item.uuid)
}

async function loadDetail() {
  if (!uuid.value) return
  entry.value = await fetchEntry(uuid.value)
  tableScores.value = await fetchTableScores(uuid.value)
  const average = tableScores.value.length
    ? tableScores.value.reduce((sum, item) => sum + Number(item.totalScore || 0), 0) / tableScores.value.length
    : 0
  form.consensusScore = Math.round(average)
  form.comments = ''
  form.advanced = Boolean(entry.value.advanced)
}

async function submitFinal() {
  await finalizeTableScore(uuid.value, {
    dimensions: [{ key: 'consensus', label: '共识分', score: Math.round(Number(form.consensusScore)), maxScore: 50 }],
    consensusScore: Math.round(Number(form.consensusScore)),
    comments: form.comments,
    advanced: form.advanced,
  })
  message.value = '本桌评分已确认'
  window.setTimeout(() => router.push('/captain'), 420)
}

async function saveAdvanced() {
  message.value = '请在每款酒的本桌最终意见中勾选晋级'
  await loadBoard()
}

watch(uuid, async () => {
  message.value = ''
  if (uuid.value) await loadDetail()
  else await loadBoard()
})

onMounted(async () => {
  me.value = await fetchMe()
  await loadBoard()
  await loadDetail()
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

.captain-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 16px;
}

.captain-stats div {
  border-radius: 8px;
  padding: 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.captain-stats span,
.captain-stats strong {
  display: block;
}

.captain-stats span {
  color: rgba(248, 250, 252, 0.7);
  font-size: 12px;
}

.captain-stats strong {
  margin-top: 5px;
  font-size: 21px;
}

.compact {
  margin-bottom: 0;
}

.board-list,
.member-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.board-row {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
}

.board-row button {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  border: 0;
  border-bottom: 1px solid #eaecf0;
  padding: 12px;
  color: #18222f;
  background: transparent;
  text-align: left;
}

.board-row strong,
.board-row small {
  display: block;
}

.board-row small {
  margin-top: 5px;
  color: #667085;
}

.board-row em {
  flex: 0 0 auto;
  font-style: normal;
}

.advance-check,
.advance-line {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px;
  color: #344054;
  font-weight: 750;
}

.advance-check.disabled {
  color: #98a2b3;
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

.score-title {
  margin-top: 18px;
}

.member-card {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.member-card h3 {
  margin: 0;
  font-size: 16px;
}

.member-card p {
  margin: 5px 0 0;
  color: #667085;
}

.member-card .split strong {
  color: #a75517;
  font-size: 21px;
}

.dimension-notes {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.dimension-notes div {
  border-radius: 8px;
  padding: 9px 10px;
  background: #f7f8f6;
}

.dimension-notes span {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 8px;
  color: #344054;
  background: #e6ece8;
  font-size: 12px;
  font-weight: 750;
}

.dimension-notes p {
  margin: 7px 0 0;
  color: #344054;
  line-height: 1.5;
}

.member-comment {
  color: #344054;
  line-height: 1.55;
}

.ok-text {
  color: #067647;
}

.warn-text {
  color: #b54708;
}

.message {
  margin: 12px 0 0;
  color: #067647;
  text-align: center;
  font-size: 14px;
  font-weight: 750;
}
</style>
