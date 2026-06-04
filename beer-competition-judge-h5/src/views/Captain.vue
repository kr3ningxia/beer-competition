<template>
  <main class="app-shell">
    <section class="top-panel">
      <button v-if="uuid" class="back-link" type="button" @click="$router.push('/captain')">返回本桌</button>
      <p class="eyebrow">桌长工作台</p>
      <h1 class="page-title">{{ uuid ? displayShortCode(entry) : `${me?.tableName || 'A 桌'}评分汇总` }}</h1>
      <div class="captain-stats">
        <div>
          <span>已确认</span>
          <strong>{{ finalizedCount }} / {{ boardEntries.length }}</strong>
        </div>
        <div>
          <span>已选晋级</span>
          <strong>{{ advancedUuids.length }} / {{ advanceTargetCount }}</strong>
        </div>
      </div>
    </section>

    <template v-if="!uuid">
      <section :class="['card', 'captain-check-card', { ready: tableReadyForReview }]">
        <div class="split">
          <div>
            <h2 class="section-title compact">{{ tableReadyForReview ? '本桌结果可核对' : '本桌结果未完成' }}</h2>
            <p class="captain-check-text">{{ tableCheckText }}</p>
          </div>
          <span :class="['pill', tableReadyForReview ? 'status-ok' : 'status-warn']">{{ advancedUuids.length }} / {{ advanceTargetCount }}</span>
        </div>
        <div v-if="captainBlockers.length" class="check-blockers">
          <span v-for="item in captainBlockers" :key="item">{{ item }}</span>
        </div>
        <button
          class="button primary full check-action"
          type="button"
          :disabled="!nextActionEntry"
          @click="openNextAction"
        >
          {{ tableReadyForReview ? '查看晋级名单' : '继续处理未完成酒款' }}
        </button>
      </section>

      <section class="card">
        <div class="split">
          <h2 class="section-title compact">本轮任务</h2>
          <span class="pill status-warn">{{ board?.competition?.flightName }}</span>
        </div>
        <div class="board-list">
          <article v-for="entry in boardEntries" :key="entry.uuid" class="board-row">
            <button type="button" @click="$router.push(`/captain/${entry.uuid}`)">
              <span>
                <strong>{{ displayShortCode(entry) }}</strong>
                <small>{{ entry.categoryName }} · {{ styleDisplayName(entry) }}</small>
                <small>我的评分：{{ entry.scored ? '已提交' : '待提交' }}</small>
              </span>
              <em :class="['pill', entry.finalized ? 'status-lock' : 'status-warn']">
                {{ entry.finalized ? `${entry.finalScore} 分` : `${entry.submittedCount}/${entry.expectedCount} 份评分` }}
              </em>
            </button>
            <span :class="['advance-check', { disabled: !entry.finalized }]">
              {{ entry.advanced ? '已加入晋级名单' : '晋级候选' }}
            </span>
          </article>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="card">
        <div v-if="entry" class="entry-summary">
          <span>{{ entry.categoryName }}</span>
          <strong>{{ styleDisplayName(entry) }} · {{ entry.abv }}</strong>
        </div>
        <div v-if="entry?.styleCategoryName || entry?.styleDescription" class="style-reference">
          <div>
            <span>风格分类</span>
            <strong>{{ entry.styleCategoryName || entry.categoryName }}</strong>
          </div>
          <p v-if="entry.styleDescription">{{ entry.styleDescription }}</p>
        </div>

        <div v-if="!myScoreSubmitted" class="captain-alert">
          你还没有提交这款酒的专业评分。
          <button type="button" @click="$router.push(`/score/${uuid}`)">去填写</button>
        </div>
        <div class="member-list">
          <article v-for="score in normalScores" :key="score.id" class="member-card">
            <div class="split">
              <div>
                <h3>{{ score.judgeName }}</h3>
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
          <input v-model="form.consensusScore" class="input" type="number" inputmode="numeric" min="0" max="50" step="1" />
        </label>
        <p v-if="scoreReference" class="caption">参考：{{ scoreReference }}</p>
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
        <p class="caption">本桌需晋级 {{ advanceTargetCount }} 款，当前已选 {{ advancedUuids.length }} 款。</p>
        <button class="button primary full" type="button" :disabled="!canFinalize" @click="submitFinal">
          保存这款酒的桌长意见
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
  consensusScore: '',
  comments: '',
  advanced: false,
})

const boardEntries = computed(() => board.value?.entries || [])
const normalScores = computed(() => tableScores.value.filter((score) => !score.finalFlag))
const finalScore = computed(() => tableScores.value.find((score) => score.finalFlag) || null)
const myScoreSubmitted = computed(() => normalScores.value.some((score) => score.judgeName === me.value?.displayName))
const finalizedCount = computed(() => boardEntries.value.filter((item) => item.finalized).length)
const advanceTargetCount = computed(() => Number(board.value?.roundTable?.targetCount || 0) || '-')
const numericAdvanceTarget = computed(() => Number(board.value?.roundTable?.targetCount || 0))
const tableReadyForReview = computed(() => {
  if (!boardEntries.value.length) return false
  const targetOk = numericAdvanceTarget.value <= 0 || advancedUuids.value.length === numericAdvanceTarget.value
  return finalizedCount.value === boardEntries.value.length && targetOk
})
const captainBlockers = computed(() => {
  const blockers = []
  if (!boardEntries.value.length) blockers.push('本桌未分配酒款')
  if (finalizedCount.value < boardEntries.value.length) blockers.push(`未汇总 ${boardEntries.value.length - finalizedCount.value} 款`)
  if (numericAdvanceTarget.value > 0 && advancedUuids.value.length !== numericAdvanceTarget.value) {
    blockers.push(`晋级需 ${numericAdvanceTarget.value} 款`)
  }
  return blockers
})
const tableCheckText = computed(() => {
  if (!boardEntries.value.length) return '请联系现场工作人员确认本轮评审桌和酒款配置。'
  if (tableReadyForReview.value) return '所有酒款已完成桌长意见，晋级数量符合本桌目标。'
  return '先完成每款酒的桌长意见，再核对晋级名单。'
})
const nextActionEntry = computed(() => (
  boardEntries.value.find((item) => !item.finalized)
  || boardEntries.value.find((item) => item.advanced)
  || boardEntries.value[0]
))
const scoreReference = computed(() => {
  if (!normalScores.value.length) return ''
  const totals = normalScores.value.map((score) => Number(score.totalScore || 0))
  const average = totals.reduce((sum, value) => sum + value, 0) / totals.length
  return `平均 ${Math.round(average)} 分，最高 ${Math.max(...totals)} 分，最低 ${Math.min(...totals)} 分`
})
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
  form.consensusScore = finalScore.value?.consensusScore || finalScore.value?.totalScore || ''
  form.comments = finalScore.value?.comments || ''
  form.advanced = Boolean(finalScore.value?.advanced || entry.value.advanced)
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

function openNextAction() {
  if (!nextActionEntry.value?.uuid) return
  router.push(`/captain/${nextActionEntry.value.uuid}`)
}

function styleDisplayName(source) {
  return [source?.styleCode, source?.style].filter(Boolean).join(' ')
}

function displayShortCode(source) {
  return source?.shortCode ? `编号： ${source.shortCode}` : '编号'
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

.captain-check-card {
  border-color: #fedf89;
  background: #fffaeb;
}

.captain-check-card.ready {
  border-color: #abefc6;
  background: #ecfdf3;
}

.captain-check-text {
  margin: 6px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
}

.check-blockers {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.check-blockers span {
  border-radius: 999px;
  padding: 5px 9px;
  color: #92400e;
  background: #fff;
  font-size: 12px;
  font-weight: 750;
}

.check-action {
  margin-top: 12px;
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

.style-reference {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
}

.style-reference div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.style-reference span {
  color: #667085;
  font-size: 13px;
}

.style-reference strong {
  color: #18222f;
  font-size: 14px;
}

.style-reference p {
  margin: 0;
  color: #344054;
  line-height: 1.55;
  font-size: 14px;
}

.score-title {
  margin-top: 18px;
}

.captain-alert {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  border: 1px solid #fedf89;
  border-radius: 8px;
  padding: 10px 12px;
  color: #92400e;
  background: #fffaeb;
  font-size: 13px;
  font-weight: 750;
}

.captain-alert button {
  flex: 0 0 auto;
  border: 0;
  border-radius: 8px;
  padding: 7px 10px;
  color: #fff;
  background: #a75517;
  font-weight: 850;
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
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.dimension-notes div {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  border-radius: 999px;
  padding: 4px 8px;
  background: #f7f8f6;
}

.dimension-notes span {
  display: inline-flex;
  border-radius: 999px;
  padding: 4px 8px;
  color: #344054;
  background: #e6ece8;
  font-size: 12px;
  font-weight: 750;
  white-space: nowrap;
}

.dimension-notes p {
  max-width: 220px;
  margin: 0;
  overflow: hidden;
  color: #344054;
  font-size: 12px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
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
