<template>
  <main class="app-shell">
    <section class="top-panel review-header">
      <div class="review-header-main">
        <div class="review-title-block">
          <h1 class="page-title compact-title">{{ current?.name || '啤酒大赛' }}</h1>
          <p class="review-context">{{ me?.tableName || current?.tableName || '未分桌' }} · {{ current?.flightName || '未发布轮次' }}</p>
        </div>
        <span class="role-badge">{{ current?.roleLabel || me?.roleLabel }}</span>
      </div>
      <div :class="['progress-strip', { 'captain-progress': isScoreRoundCaptain }]">
        <template v-if="isScoreRoundCaptain">
        <span>我的评分 <strong>{{ myScoredCount }}/{{ entries.length }}</strong></span>
        <span>本桌汇总 <strong>{{ finalizedCount }}/{{ entries.length }}</strong></span>
        <span v-if="advanceTargetCount > 0">晋级 <strong>{{ advancedCount }}/{{ advanceTargetCount }}</strong></span>
        </template>
        <template v-else-if="isRankingRound">
        <span>候选 <strong>{{ entries.length }}款</strong></span>
        <span>已排序 <strong>{{ rankingFilledCount }}/{{ rankingSlots.length }}</strong></span>
        </template>
        <span v-else>{{ progressLabel }} <strong>{{ progressCount }}</strong></span>
      </div>
    </section>

    <section v-if="!current" class="card action-card">
      <h2 class="section-title">暂未分配比赛</h2>
      <p class="caption">账号启用后，主办方会把你加入本场评审桌。</p>
      <button class="button secondary full empty-action" type="button" @click="router.push('/profile')">查看我的资料</button>
    </section>

    <template v-else-if="isRankingRound">
    <section class="card ranking-action-card">
      <div class="split">
        <div>
          <h2 class="section-title compact">{{ canSubmitRanking ? '排序工作台' : '本桌排序任务' }}</h2>
          <p class="task-hint">{{ rankingTaskHint }}</p>
        </div>
        <span class="pill status-warn">{{ rankingFilledCount }}/{{ rankingSlots.length }}</span>
      </div>
      <button class="scan-button ranking-button" type="button" @click="router.push(`/ranking/${current.roundTableId}`)">
        {{ canSubmitRanking ? '进入选择排序' : '查看本桌候选' }}
      </button>
      <p v-if="!canSubmitRanking" class="caption">本轮由桌长提交排序结果。</p>
    </section>

    <section class="card">
      <div class="split">
        <h2 class="section-title compact">排序候选酒款</h2>
        <span class="pill">{{ entries.length }} 款</span>
      </div>
      <div class="entry-list">
        <article v-for="entry in entries" :key="entry.uuid" class="entry-row static-row">
          <span>
            <strong>{{ displayShortCode(entry) }}</strong>
            <small>组别：{{ entry.categoryName || '-' }}</small>
            <small>风格：{{ styleDisplayName(entry) || '-' }}</small>
          </span>
        </article>
      </div>
      <p v-if="!entries.length" class="empty-note">当前还没有排序候选酒款，请联系现场工作人员确认后续轮候选池。</p>
    </section>
    </template>

    <template v-else>
    <section class="card action-card">
      <div class="split action-head">
        <h2 class="section-title compact">{{ isScoreRoundCaptain ? '扫码评酒' : '扫码或输入编号' }}</h2>
      </div>
      <button class="scan-button" type="button" @click="toggleScanner">
        {{ scannerOpen ? '关闭扫码' : '扫描二维码' }}
      </button>
      <div v-if="scannerOpen" class="scanner-box">
        <div id="judge-qr-reader" class="scanner-reader"></div>
        <p class="scanner-message">{{ scannerMessage }}</p>
      </div>

      <div class="field manual-row">
        <input
          v-model.trim="manualCode"
          class="input"
          aria-label="输入酒款编号"
          placeholder="输入酒款编号，如 256Z"
          @keyup.enter="openScanCode(manualCode)"
        />
        <button class="button dark" type="button" @click="openScanCode(manualCode)">打开酒款</button>
      </div>
    </section>

    <section class="card table-task-card">
      <div class="split">
        <h2 class="section-title compact">{{ taskSectionTitle }}</h2>
        <span class="pill">{{ entries.length }} 款</span>
      </div>
      <p v-if="!isScoreRoundCaptain" class="task-hint">{{ taskSectionHint }}</p>
      <section v-if="isScoreRoundCaptain && entries.length" :class="['round-checkout', { ready: tableReadyForReview }]">
        <div>
          <strong>{{ tableReadyForReview ? '本桌可核对结果' : '本桌结果未完成' }}</strong>
          <p>{{ tableCheckoutText }}</p>
        </div>
        <button
          class="button secondary checkout-button"
          type="button"
          :disabled="!finalizedCount"
          @click="router.push('/captain')"
        >
          核对
        </button>
      </section>
      <div v-if="isScoreRoundCaptain && entries.length" class="captain-entry-list">
        <article
          v-for="entry in entries"
          :key="entry.uuid"
          :class="['captain-entry-card', { finalized: entry.finalized }]"
        >
          <button class="captain-entry-main" type="button" @click="openEntry(entry.uuid)">
            <span>
              <strong>{{ displayShortCode(entry) }}</strong>
              <small>{{ entry.categoryName || '-' }} · {{ styleDisplayName(entry) || '-' }}</small>
            </span>
            <em :class="['pill', entryStatus(entry).className]">{{ entryStatus(entry).label }}</em>
          </button>
          <div class="captain-entry-footer">
            <div class="captain-entry-meta">
              <span :class="entry.scored ? 'ok-text' : 'warn-text'">我：{{ entry.scored ? '已评' : '待评' }}</span>
              <span>同桌：{{ tableScoreProgress(entry) }}</span>
              <span :class="entry.finalized ? 'ok-text' : ''">汇总：{{ entry.finalized ? '已确认' : readyForFinalize(entry) ? '可汇总' : '等待' }}</span>
            </div>
            <button
              v-if="captainAction(entry)"
              type="button"
              :class="['entry-action', { primary: captainAction(entry).primary }]"
              @click="openCaptainAction(entry)"
            >
              {{ captainAction(entry).label }}
            </button>
          </div>
        </article>
      </div>
      <section v-else-if="isScoreRoundCaptain" class="empty-diagnostic">
        <h3>{{ emptyStateTitle }}</h3>
        <p>{{ emptyStateMessage }}</p>
        <ul>
          <li v-for="item in emptyStateChecks" :key="item">{{ item }}</li>
        </ul>
      </section>
      <div v-else class="entry-list">
        <button
          v-for="entry in entries"
          :key="entry.uuid"
          type="button"
          class="entry-row"
          @click="openEntry(entry.uuid)"
        >
          <span>
            <strong>{{ displayShortCode(entry) }}</strong>
            <small>组别：{{ entry.categoryName || '-' }}</small>
            <small>风格：{{ styleDisplayName(entry) || '-' }}</small>
            <small v-if="isCaptain">我的评分：{{ entry.scored ? '已提交' : '待提交' }}</small>
          </span>
          <em :class="['pill', entryStatus(entry).className]">
            {{ entryStatus(entry).label }}
          </em>
        </button>
      </div>
    </section>
    </template>

    <JudgeBottomNav :role="me?.role" :hide-table="isRankingRound" />
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Html5Qrcode } from 'html5-qrcode'
import { fetchCaptainBoard, fetchCompetitions, fetchMe, fetchMyScores, fetchRoundTable } from '@/api/judge'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'
import { isRankingTaskType, selectCurrentTask } from '@/utils/judgeTasks'

const router = useRouter()
const me = ref(null)
const current = ref(null)
const entries = ref([])
const captainBoard = ref(null)
const rankingSlots = ref([])
const scannerOpen = ref(false)
const manualCode = ref('')
const scannerMessage = ref('将二维码放入取景框内')
let qrReader = null
let scanLocked = false

const isCaptain = computed(() => me.value?.role === 'CAPTAIN')
const isScoreRoundCaptain = computed(() => isCaptain.value && current.value?.taskType === 'CAPTAIN_FINALIZE')
const isRankingRound = computed(() => isRankingTaskType(current.value?.taskType))
const canSubmitRanking = computed(() => current.value?.taskType === 'RANKING_ROUND')
const rankingTaskHint = computed(() => (
  canSubmitRanking.value
    ? '本轮从候选酒中选择并排序，提交后进入下一轮或奖项确认。'
    : '你已安排到本桌参与讨论，可查看候选酒款，排序结果由桌长提交。'
))
const myScoredCount = computed(() => entries.value.filter((entry) => entry.scored).length)
const finalizedCount = computed(() => entries.value.filter((entry) => entry.finalized).length)
const advancedCount = computed(() => entries.value.filter((entry) => entry.advanced).length)
const advanceTargetCount = computed(() => Number(captainBoard.value?.roundTable?.targetCount || current.value?.targetCount || 0))
const rankingFilledCount = computed(() => rankingSlots.value.filter((slot) => slot.beerEntryId).length)
const progressLabel = computed(() => (isCaptain.value ? '我的评分' : '我的进度'))
const progressCount = computed(() => (
  isCaptain.value
    ? `${myScoredCount.value} / ${entries.value.length}`
    : `${current.value?.myScoredCount || 0} / ${current.value?.totalEntries || 0}`
))
const taskSectionTitle = computed(() => (isCaptain.value ? '本桌酒款' : '我的本轮酒款'))
const taskSectionHint = computed(() => (
  isScoreRoundCaptain.value
    ? `扫码完成自己的专业评分；同桌评分齐全后，从下方进入本桌汇总。`
    : '扫码或点酒款进入评分，已评分酒款在确认前可查看或调整。'
))
const tableReadyForReview = computed(() => {
  if (!entries.value.length) return false
  const targetOk = advanceTargetCount.value <= 0 || advancedCount.value === advanceTargetCount.value
  return finalizedCount.value === entries.value.length && targetOk
})
const tableCheckoutText = computed(() => {
  if (!entries.value.length) return ''
  if (finalizedCount.value < entries.value.length) {
    return `还有 ${entries.value.length - finalizedCount.value} 款未填写桌长意见，完成后再核对本桌结果。`
  }
  if (advanceTargetCount.value > 0 && advancedCount.value !== advanceTargetCount.value) {
    return `酒款意见已完成，还需按本桌目标确认 ${advanceTargetCount.value} 款晋级酒。`
  }
  return '酒款意见和晋级名单已齐，请核对无误后提交本桌结果。'
})
const emptyStateTitle = computed(() => {
  if (!current.value?.roundTableId) return '当前账号还没有绑定评审桌'
  if (current.value?.taskType !== 'CAPTAIN_FINALIZE') return '当前轮次不是第一轮汇总任务'
  return '本桌还没有酒款'
})
const emptyStateMessage = computed(() => {
  if (!current.value?.roundTableId) return '请现场工作人员先把你加入本轮评审桌。'
  if (current.value?.taskType !== 'CAPTAIN_FINALIZE') return '请确认后台是否已经发布第一轮评分任务。'
  return '请现场工作人员确认本轮、评审桌和酒款分配。'
})
const emptyStateChecks = computed(() => [
  `评审桌：${me.value?.tableName || current.value?.tableName || '未分配'}`,
  `当前轮次：${current.value?.flightName || '未发布'}`,
])

function openEntry(uuid) {
  if (!uuid) return
  router.push(`/scan-result/${uuid.toUpperCase()}`)
}

function openCaptainAction(entry) {
  if (!entry?.uuid) return
  if (!entry.scored) {
    router.push(`/score/${entry.uuid.toUpperCase()}`)
    return
  }
  if (entry.finalized || readyForFinalize(entry)) {
    router.push(`/captain/${entry.uuid.toUpperCase()}`)
    return
  }
  router.push(`/captain/${entry.uuid.toUpperCase()}`)
}

function openScanCode(code) {
  const normalized = normalizeScanCode(code)
  if (!normalized) return
  router.push(`/q/${encodeURIComponent(normalized)}`)
}

function displayShortCode(entry) {
  return entry?.shortCode ? `编号： ${entry.shortCode}` : '编号待生成'
}

function styleDisplayName(entry) {
  return [entry?.styleCode, entry?.style].filter(Boolean).join(' ')
}

function entryStatus(entry) {
  if (entry.finalized) {
    return entry.advanced
      ? { label: '已晋级', className: 'status-ok' }
      : { label: '已确认', className: 'status-lock' }
  }
  if (isCaptain.value) {
    if (!entry.scored) return { label: '待我评分', className: 'status-warn' }
    return { label: '待汇总', className: 'status-warn' }
  }
  if (entry.scored) return { label: '已评分', className: 'status-ok' }
  return { label: '待评分', className: '' }
}

function tableScoreProgress(entry) {
  const expected = Number(entry.expectedCount || 0)
  const submitted = Number(entry.submittedCount || 0)
  return expected > 0 ? `${submitted} / ${expected}` : `${submitted} 份`
}

function readyForFinalize(entry) {
  const expected = Number(entry.expectedCount || 0)
  const submitted = Number(entry.submittedCount || 0)
  return Boolean(entry.scored && !entry.finalized && expected > 0 && submitted >= expected)
}

function captainAction(entry) {
  if (entry.finalized) return { label: '查看意见', primary: false }
  if (!entry.scored) return null
  if (readyForFinalize(entry)) return { label: '汇总意见', primary: true }
  return null
}

async function toggleScanner() {
  if (scannerOpen.value) {
    await stopScanner()
    return
  }
  scannerOpen.value = true
  scannerMessage.value = '将二维码放入取景框内'
  scanLocked = false
  await nextTick()
  await startScanner()
}

async function startScanner() {
  try {
    qrReader = new Html5Qrcode('judge-qr-reader')
    await qrReader.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 220, height: 220 } },
      (decodedText) => {
        if (scanLocked) return
        scanLocked = true
        const code = normalizeScanCode(decodedText)
        if (!code) {
          scanLocked = false
          return
        }
        scannerMessage.value = '已识别，正在打开酒款信息'
        stopScanner().finally(() => router.push(`/q/${encodeURIComponent(code)}`))
      },
    )
  } catch {
    scannerMessage.value = '无法打开摄像头，请检查浏览器权限，或输入标签下方编号'
  }
}

async function stopScanner() {
  scannerOpen.value = false
  if (!qrReader) return
  try {
    if (qrReader.isScanning) {
      await qrReader.stop()
    }
    await qrReader.clear()
  } catch {
    // 相机关闭失败不影响手动输入。
  } finally {
    qrReader = null
  }
}

function normalizeScanCode(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  try {
    const url = new URL(text)
    const parts = url.pathname.split('/').filter(Boolean)
    if (parts[0] === 'q' && parts[1]) {
      return decodeURIComponent(parts[1]).toUpperCase()
    }
  } catch {
    // 普通编号会进入这里，直接使用原文本。
  }
  return text.toUpperCase()
}

onMounted(async () => {
  me.value = await fetchMe()
  if (me.value?.profileRequired) {
    router.replace('/profile/edit')
    return
  }
  if (Number(me.value?.status) === 2) {
    router.replace('/review-status')
    return
  }
    const competitions = await fetchCompetitions()
    current.value = selectCurrentTask(competitions)
  captainBoard.value = null
  rankingSlots.value = []
  if (!current.value) {
    entries.value = []
    return
  }
  if (current.value.roundTableId) {
    if (current.value.taskType === 'CAPTAIN_FINALIZE') {
      const board = await fetchCaptainBoard(current.value.roundTableId)
      captainBoard.value = board
      entries.value = board.entries || []
      return
    }
    const [table, myScores] = isRankingRound.value
      ? [await fetchRoundTable(current.value.roundTableId), []]
      : await Promise.all([
        fetchRoundTable(current.value.roundTableId),
        fetchMyScores(),
      ])
    rankingSlots.value = table.rankings || []
    const myScoredUuids = new Set(myScores.map((score) => score.beerUuid))
    entries.value = (table.entries || []).map((entry) => ({
      ...entry,
      scored: myScoredUuids.has(entry.uuid),
      finalized: Boolean(entry.advanced),
    }))
  }
})

onBeforeUnmount(() => {
  stopScanner()
})

</script>

<style scoped>
.review-header {
  padding: 12px 14px;
}

.review-header-main {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
}

.review-title-block {
  min-width: 0;
}

.compact-title {
  overflow: hidden;
  max-width: 100%;
  font-size: 20px;
  line-height: 1.25;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.review-context {
  margin: 6px 0 0;
  color: rgba(248, 250, 252, 0.72);
  font-size: 13px;
  font-weight: 750;
}

.role-badge {
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  padding: 5px 9px;
  color: #fff7ed;
  background: rgba(255, 255, 255, 0.1);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.progress-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 10px;
}

.progress-strip span {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  min-height: 28px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  padding: 4px 9px;
  color: rgba(248, 250, 252, 0.74);
  background: rgba(255, 255, 255, 0.075);
  font-size: 12px;
  font-weight: 700;
}

.progress-strip strong {
  color: #fff;
  font-size: 13px;
}

.action-card {
  margin-top: 12px;
}

.ranking-action-card {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.ranking-button {
  background: #1f2a37;
}

.empty-action {
  margin-top: 14px;
}

.action-head {
  margin-bottom: 10px;
}

.scan-button {
  width: 100%;
  min-height: 52px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: #a75517;
  font-size: 17px;
  font-weight: 850;
}

.scanner-box {
  margin-top: 12px;
  border-radius: 8px;
  padding: 12px;
  background: #121a24;
}

.scanner-reader {
  overflow: hidden;
  min-height: 260px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 8px;
  background: #05070a;
}

.scanner-reader :deep(video) {
  width: 100%;
  border-radius: 8px;
}

.scanner-message {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  line-height: 1.45;
}

.manual-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.manual-row .button {
  min-width: 86px;
  min-height: 44px;
  padding: 10px 13px;
  white-space: nowrap;
}

.manual-row .input {
  min-height: 44px;
  margin-top: 0;
}

.field {
  margin-top: 12px;
}

.compact {
  margin-bottom: 0;
}

.table-task-card {
  margin-bottom: 92px;
}

.round-checkout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
  border: 1px solid #fedf89;
  border-radius: 8px;
  padding: 10px;
  background: #fffaeb;
}

.round-checkout.ready {
  border-color: #abefc6;
  background: #ecfdf3;
}

.round-checkout strong,
.round-checkout p {
  display: block;
  margin: 0;
}

.round-checkout strong {
  color: #26313d;
  font-size: 14px;
}

.round-checkout p {
  margin-top: 3px;
  color: #667085;
  font-size: 12px;
  line-height: 1.4;
}

.checkout-button {
  min-width: 64px;
  min-height: 38px;
  padding: 8px 12px;
}

.entry-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.task-hint {
  margin: 8px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
}

.captain-entry-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.captain-entry-card {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
}

.captain-entry-card.finalized {
  border-color: #c7e8d5;
  background: #fbfffc;
}

.captain-entry-main {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  border: 0;
  padding: 0;
  color: #18222f;
  background: transparent;
  text-align: left;
}

.captain-entry-main strong,
.captain-entry-main small {
  display: block;
}

.captain-entry-main strong {
  font-size: 15px;
}

.captain-entry-main small {
  margin-top: 3px;
  color: #667085;
  line-height: 1.35;
  font-size: 12px;
}

.captain-entry-main em {
  flex: 0 0 auto;
  font-style: normal;
}

.captain-entry-footer {
  display: flex;
  gap: 8px;
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 8px;
}

.captain-entry-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.captain-entry-meta span {
  border-radius: 999px;
  padding: 4px 8px;
  color: #475467;
  background: #f7f8f6;
  font-size: 12px;
  font-weight: 750;
  line-height: 1.35;
}

.captain-entry-meta .ok-text {
  color: #067647;
  background: #ecfdf3;
}

.captain-entry-meta .warn-text {
  color: #a75517;
  background: #fff7ed;
}

.entry-action {
  flex: 0 0 auto;
  min-height: 32px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 6px 10px;
  color: #344054;
  background: #fff;
  font-size: 12px;
  font-weight: 850;
  white-space: nowrap;
}

.entry-action.primary {
  border-color: #d17932;
  color: #fff;
  background: #a75517;
}

.empty-note {
  margin: 14px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.55;
}

.empty-diagnostic {
  margin-top: 12px;
  border: 1px dashed #d0d5dd;
  border-radius: 8px;
  padding: 12px;
  background: #f8faf9;
}

.empty-diagnostic h3,
.empty-diagnostic p {
  margin: 0;
}

.empty-diagnostic h3 {
  color: #26313d;
  font-size: 15px;
}

.empty-diagnostic p {
  margin-top: 6px;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
}

.empty-diagnostic ul {
  display: grid;
  gap: 5px;
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
}

.empty-diagnostic li {
  border-radius: 8px;
  padding: 7px 8px;
  color: #475467;
  background: #fff;
  font-size: 12px;
  font-weight: 750;
}

.entry-row {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  color: #18222f;
  background: #fff;
  text-align: left;
}

.static-row {
  cursor: default;
}

.entry-row strong,
.entry-row small {
  display: block;
}

.entry-row small {
  margin-top: 4px;
  color: #667085;
  line-height: 1.35;
}

.entry-row em {
  flex: 0 0 auto;
  font-style: normal;
}

@media (max-width: 380px) {
  .manual-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .entry-row {
    align-items: flex-start;
  }
}
</style>
