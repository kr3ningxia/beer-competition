<template>
  <main class="app-shell">
    <section :class="['top-panel', 'captain-top', { 'detail-top': uuid }]">
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
            <h2 class="section-title compact">{{ captainSummaryTitle }}</h2>
            <p class="captain-check-text">{{ tableCheckText }}</p>
          </div>
          <span :class="['pill', tableReadyForReview ? 'status-ok' : 'status-warn']">{{ advancedUuids.length }} / {{ advanceTargetCount }}</span>
        </div>
        <div v-if="captainSummaryBadges.length" class="check-blockers">
          <span v-for="item in captainSummaryBadges" :key="item">{{ item }}</span>
        </div>
        <button
          v-if="captainSummaryState !== 'ready-review'"
          :class="['button', summaryActionPrimary ? 'primary' : 'secondary', 'full', 'check-action']"
          type="button"
          :disabled="!canOpenNextAction"
          @click="openNextAction"
        >
          {{ captainSummaryActionLabel }}
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
              <div class="member-score">
                <strong>{{ score.totalScore }} 分</strong>
              </div>
            </div>
            <div class="dimension-notes">
              <div v-for="dim in score.dimensions" :key="dim.key">
                <span>{{ dim.label }}</span>
                <strong>{{ dim.score }}/{{ dim.maxScore }}</strong>
              </div>
            </div>
            <div v-if="score.comments" class="member-comment-block">
              <button
                class="comment-toggle"
                type="button"
                :aria-expanded="isCommentExpanded(score.id)"
                @click="toggleComment(score.id)"
              >
                <span>{{ commentSummary(score.comments) }}</span>
                <strong>{{ isCommentExpanded(score.id) ? '收起' : '展开' }}</strong>
              </button>
              <div v-if="isCommentExpanded(score.id)" class="member-comment-list">
                <p v-for="(line, index) in commentLines(score.comments)" :key="`${index}-${line}`" class="member-comment">{{ line }}</p>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="card final-card">
        <div class="final-head">
          <h2 class="section-title compact">本桌最终意见</h2>
          <span class="final-count">晋级 {{ advancedUuids.length }} / {{ advanceTargetCount }}</span>
        </div>
        <div v-if="scoreStats" class="score-reference-grid">
          <button type="button" @click="fillConsensusScore(scoreStats.average)">
            <span>平均</span>
            <strong>{{ scoreStats.average }}</strong>
          </button>
          <button type="button" @click="fillConsensusScore(scoreStats.highest)">
            <span>最高</span>
            <strong>{{ scoreStats.highest }}</strong>
          </button>
          <button type="button" @click="fillConsensusScore(scoreStats.lowest)">
            <span>最低</span>
            <strong>{{ scoreStats.lowest }}</strong>
          </button>
        </div>
        <label class="field score-field">
          共识分
          <input v-model="form.consensusScore" class="input" type="number" inputmode="numeric" min="0" max="50" step="1" />
        </label>
        <label class="field">
          综合评语
          <span class="field-count">{{ finalCommentLength }} / {{ minFinalCommentLength }} 字</span>
          <textarea
            v-model.trim="form.comments"
            class="textarea"
            placeholder="请写下本桌讨论后的综合意见，作为参赛方最终可见反馈。"
          />
        </label>
        <label :class="['advance-line', 'final-advance', { active: form.advanced }]">
          <input v-model="form.advanced" type="checkbox" />
          <span>加入晋级名单</span>
        </label>
        <p class="caption">本桌需晋级 {{ advanceTargetCount }} 款，当前已选 {{ advancedUuids.length }} 款。</p>
        <button class="button primary full" type="button" :disabled="!canFinalize" @click="submitFinal">
          保存这款酒的桌长意见
        </button>
        <p v-if="finalizeHint && !message" class="submit-hint">{{ finalizeHint }}</p>
        <p v-if="message" class="message">{{ message }}</p>
      </section>
    </template>

    <JudgeBottomNav :role="me?.role || 'CAPTAIN'" />
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchCaptainBoard,
  fetchCompetitions,
  fetchEntry,
  fetchMe,
  fetchTableScores,
  finalizeTableScore,
} from '@/api/judge'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'
import { isRankingTaskType, selectCurrentTask } from '@/utils/judgeTasks'

const route = useRoute()
const router = useRouter()
const uuid = computed(() => route.params.uuid ? String(route.params.uuid).toUpperCase() : '')
const me = ref(null)
const board = ref(null)
const entry = ref(null)
const tableScores = ref([])
const advancedUuids = ref([])
const expandedCommentIds = ref(new Set())
const message = ref('')
const form = reactive({
  consensusScore: '',
  comments: '',
  advanced: false,
})

const boardEntries = computed(() => board.value?.entries || [])
const normalScores = computed(() => tableScores.value.filter((score) => !score.finalFlag))
const finalScore = computed(() => tableScores.value.find((score) => score.finalFlag) || null)
const myScoreSubmitted = computed(() => Boolean(entry.value?.scored))
const finalizedCount = computed(() => boardEntries.value.filter((item) => item.finalized).length)
const unfinalizedCount = computed(() => Math.max(0, boardEntries.value.length - finalizedCount.value))
const myPendingScoreCount = computed(() => boardEntries.value.filter((item) => !item.scored).length)
const readyFinalizeEntries = computed(() => boardEntries.value.filter((item) => readyForFinalize(item)))
const readyFinalizeCount = computed(() => readyFinalizeEntries.value.length)
const advanceTargetCount = computed(() => Number(board.value?.roundTable?.targetCount || 0) || '-')
const numericAdvanceTarget = computed(() => Number(board.value?.roundTable?.targetCount || 0))
const tableReadyForReview = computed(() => {
  if (!boardEntries.value.length) return false
  const targetOk = numericAdvanceTarget.value <= 0 || advancedUuids.value.length === numericAdvanceTarget.value
  return finalizedCount.value === boardEntries.value.length && targetOk
})
const captainSummaryState = computed(() => {
  if (!boardEntries.value.length) return 'empty'
  if (tableReadyForReview.value) return 'ready-review'
  if (finalizedCount.value === boardEntries.value.length) return 'advance-check'
  if (readyFinalizeCount.value > 0) return 'ready-finalize'
  if (myPendingScoreCount.value > 0) return 'need-score'
  return 'waiting-peer'
})
const captainSummaryTitle = computed(() => {
  if (captainSummaryState.value === 'ready-review') return '本桌结果可核对'
  if (captainSummaryState.value === 'advance-check') return '晋级名单待核对'
  if (captainSummaryState.value === 'ready-finalize') return '有酒款可以汇总'
  if (captainSummaryState.value === 'waiting-peer') return '等待同桌评分完成'
  return '本桌汇总未完成'
})
const captainSummaryBadges = computed(() => {
  if (!boardEntries.value.length) return ['本桌未分配酒款']
  const badges = [
    `未汇总 ${unfinalizedCount.value} 款`,
    `可汇总 ${readyFinalizeCount.value} 款`,
  ]
  if (myPendingScoreCount.value > 0) badges.push(`待我评分 ${myPendingScoreCount.value} 款`)
  if (numericAdvanceTarget.value > 0 && finalizedCount.value === boardEntries.value.length && advancedUuids.value.length !== numericAdvanceTarget.value) {
    badges.push(`晋级需 ${numericAdvanceTarget.value} 款`)
  }
  return badges
})
const tableCheckText = computed(() => {
  if (!boardEntries.value.length) return '请联系现场工作人员确认本轮评审桌和酒款配置。'
  if (tableReadyForReview.value) return '酒款意见和晋级名单已齐，请核对无误后提交本桌结果。'
  if (captainSummaryState.value === 'advance-check') return '酒款意见已完成，还需按本桌目标确认晋级名单。'
  if (captainSummaryState.value === 'ready-finalize') return '同桌评分已齐，可填写桌长意见。'
  if (captainSummaryState.value === 'need-score') return '先扫码完成自己的评分；同桌评分齐全后，再填写桌长意见。'
  return '你的评分已完成，等同桌提交后再填写桌长意见。'
})
const nextActionEntry = computed(() => (
  readyFinalizeEntries.value[0]
  || boardEntries.value.find((item) => !item.finalized)
  || boardEntries.value.find((item) => item.advanced)
  || boardEntries.value[0]
))
const captainSummaryActionLabel = computed(() => {
  if (captainSummaryState.value === 'ready-review') return '核对本桌结果'
  if (captainSummaryState.value === 'advance-check') return '核对晋级名单'
  if (captainSummaryState.value === 'ready-finalize') return '处理可汇总酒款'
  if (captainSummaryState.value === 'need-score') return '去扫码评分'
  if (captainSummaryState.value === 'waiting-peer') return '查看评分进度'
  return '返回扫码'
})
const summaryActionPrimary = computed(() => (
  captainSummaryState.value === 'ready-finalize'
  || captainSummaryState.value === 'need-score'
  || captainSummaryState.value === 'ready-review'
  || captainSummaryState.value === 'advance-check'
))
const canOpenNextAction = computed(() => (
  captainSummaryState.value === 'need-score'
  || captainSummaryState.value === 'empty'
  || Boolean(nextActionEntry.value?.uuid)
))
const currentBoardEntry = computed(() => boardEntries.value.find((item) => item.uuid === uuid.value) || null)
const currentSubmittedCount = computed(() => Number(currentBoardEntry.value?.submittedCount || normalScores.value.length || 0))
const currentExpectedCount = computed(() => Number(currentBoardEntry.value?.expectedCount || 0))
const tableScoresReady = computed(() => (
  currentExpectedCount.value > 0
  && currentSubmittedCount.value >= currentExpectedCount.value
))
const scoreStats = computed(() => {
  if (!normalScores.value.length) return null
  const totals = normalScores.value.map((score) => Number(score.totalScore || 0))
  const average = totals.reduce((sum, value) => sum + value, 0) / totals.length
  return {
    average: Math.round(average),
    highest: Math.max(...totals),
    lowest: Math.min(...totals),
  }
})
const scoreReference = computed(() => {
  if (!scoreStats.value) return ''
  return `平均 ${scoreStats.value.average} 分，最高 ${scoreStats.value.highest} 分，最低 ${scoreStats.value.lowest} 分`
})
const minFinalCommentLength = 20
const finalCommentLength = computed(() => form.comments.length)
const consensusScoreValid = computed(() => (
  Number.isInteger(Number(form.consensusScore))
  && Number(form.consensusScore) >= 0
  && Number(form.consensusScore) <= 50
))
const canFinalize = computed(() => (
  tableScoresReady.value
  && consensusScoreValid.value
  && finalCommentLength.value >= minFinalCommentLength
))
const finalizeHint = computed(() => {
  if (!tableScoresReady.value) {
    const expectedText = currentExpectedCount.value > 0 ? currentExpectedCount.value : '-'
    return `同桌评分未完成（${currentSubmittedCount.value}/${expectedText}），暂不能保存桌长意见。`
  }
  if (!consensusScoreValid.value) return '请填写 0-50 的共识分。'
  if (finalCommentLength.value < minFinalCommentLength) return `综合评语还差 ${minFinalCommentLength - finalCommentLength.value} 字。`
  return ''
})

async function loadBoard() {
  board.value = await fetchCaptainBoard()
  advancedUuids.value = board.value.entries.filter((item) => item.advanced).map((item) => item.uuid)
}

async function loadDetail() {
  if (!uuid.value) return
  entry.value = await fetchEntry(uuid.value)
  tableScores.value = await fetchTableScores(uuid.value)
  expandedCommentIds.value = new Set()
  form.consensusScore = finalScore.value?.consensusScore || finalScore.value?.totalScore || ''
  form.comments = finalScore.value?.comments || ''
  form.advanced = Boolean(finalScore.value?.advanced || entry.value.advanced)
}

async function redirectRankingTaskIfNeeded() {
  try {
    const currentTask = selectCurrentTask(await fetchCompetitions())
    if (!isRankingTaskType(currentTask?.taskType)) return false
    router.replace(currentTask.roundTableId ? `/ranking/${currentTask.roundTableId}` : '/competitions')
    return true
  } catch {
    return false
  }
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
  if (captainSummaryState.value === 'need-score' || captainSummaryState.value === 'empty') {
    router.push('/competitions')
    return
  }
  if (!nextActionEntry.value?.uuid) return
  router.push(`/captain/${nextActionEntry.value.uuid}`)
}

function readyForFinalize(item) {
  const expected = Number(item.expectedCount || 0)
  const submitted = Number(item.submittedCount || 0)
  return Boolean(item.scored && !item.finalized && expected > 0 && submitted >= expected)
}

function fillConsensusScore(value) {
  form.consensusScore = Math.round(Number(value || 0))
}

function isCommentExpanded(scoreId) {
  return expandedCommentIds.value.has(scoreId)
}

function toggleComment(scoreId) {
  const nextIds = new Set(expandedCommentIds.value)
  if (nextIds.has(scoreId)) nextIds.delete(scoreId)
  else nextIds.add(scoreId)
  expandedCommentIds.value = nextIds
}

function commentLines(comments = '') {
  return String(comments)
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
}

function commentSummary(comments = '') {
  const lines = commentLines(comments)
  const textLength = String(comments).replace(/\s/g, '').length
  return lines.length > 1 ? `评语 ${lines.length} 项 · ${textLength} 字` : `评语 ${textLength} 字`
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
  if (await redirectRankingTaskIfNeeded()) return
  await loadBoard()
  await loadDetail()
})
</script>

<style scoped>
.back-link {
  border: 0;
  margin: 0 0 10px;
  padding: 0;
  color: rgba(255, 255, 255, 0.74);
  background: transparent;
  font-weight: 750;
}

.captain-top {
  padding: 16px 18px;
}

.captain-top.detail-top {
  padding-bottom: 16px;
}

.captain-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.captain-stats div {
  border-radius: 8px;
  padding: 10px 12px;
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
  margin-top: 4px;
  font-size: 20px;
  font-variant-numeric: tabular-nums;
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
  min-width: 0;
  border-radius: 8px;
  padding: 12px;
  background: #f7f8f6;
}

.entry-summary span {
  color: #667085;
  font-size: 13px;
}

.entry-summary strong {
  overflow-wrap: anywhere;
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
  min-width: 0;
}

.style-reference span {
  color: #667085;
  font-size: 13px;
}

.style-reference strong {
  min-width: 0;
  color: #18222f;
  font-size: 14px;
  overflow-wrap: anywhere;
  text-align: right;
}

.style-reference p {
  margin: 0;
  color: #344054;
  line-height: 1.55;
  font-size: 14px;
  overflow-wrap: anywhere;
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
  min-width: 0;
}

.member-card h3 {
  margin: 0;
  font-size: 16px;
  overflow-wrap: anywhere;
}

.member-card p {
  margin: 5px 0 0;
  color: #667085;
  overflow-wrap: anywhere;
}

.member-score {
  display: grid;
  justify-items: end;
  gap: 2px;
  flex: 0 0 auto;
}

.member-score strong {
  color: #a75517;
  font-size: 22px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.member-card .split > div {
  min-width: 0;
}

.dimension-notes {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 10px;
  margin-top: 12px;
}

.dimension-notes div {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  border-bottom: 1px solid #eef0ee;
  padding: 0 0 6px;
}

.dimension-notes span {
  min-width: 0;
  overflow: hidden;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dimension-notes strong {
  flex: 0 0 auto;
  color: #18222f;
  font-size: 12px;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.member-comment-block {
  margin-top: 12px;
  border-top: 1px solid #eaecf0;
  padding-top: 10px;
}

.comment-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: 0;
  border-radius: 8px;
  padding: 9px 10px;
  color: #475467;
  background: #f7f8f6;
  text-align: left;
}

.comment-toggle span {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-toggle strong {
  flex: 0 0 auto;
  color: #a75517;
  font-size: 13px;
  font-weight: 850;
}

.member-comment-list {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.member-comment {
  margin: 0;
  border-radius: 8px;
  padding: 9px 10px;
  color: #344054;
  background: #fbfbfa;
  font-size: 14px;
  line-height: 1.55;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.final-card {
  padding-bottom: 22px;
}

.final-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.final-count {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 5px 9px;
  color: #92400e;
  background: #fffaeb;
  font-size: 12px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.score-reference-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.score-reference-grid button {
  display: grid;
  gap: 4px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 9px 10px;
  color: #344054;
  background: #f7f8f6;
  text-align: left;
}

.score-reference-grid span {
  color: #667085;
  font-size: 12px;
  font-weight: 750;
}

.score-reference-grid strong {
  color: #18222f;
  font-size: 19px;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
}

.score-field {
  margin-top: 12px;
}

.field-count {
  float: right;
  color: #667085;
  font-size: 12px;
  font-weight: 750;
}

.final-advance {
  margin-top: 12px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  padding: 11px 12px;
  background: #fff;
}

.final-advance.active {
  border-color: #fedf89;
  color: #92400e;
  background: #fffaeb;
}

.final-advance input {
  width: 18px;
  height: 18px;
  accent-color: #a75517;
}

.submit-hint {
  margin: 8px 0 0;
  color: #b54708;
  text-align: center;
  font-size: 13px;
  font-weight: 750;
}

@media (max-width: 380px) {
  .dimension-notes {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
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
