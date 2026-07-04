<template>
  <main class="app-shell">
    <section class="top-panel review-header">
      <div class="review-header-main">
        <div class="review-title-block">
          <h1 class="page-title compact-title">{{ current?.name || '啤酒大赛' }}</h1>
          <p class="review-context">{{ me?.tableName || current?.tableName || '未分桌' }} · {{ current?.flightName || '未发布轮次' }}</p>
        </div>
        <div class="review-actions">
          <span class="role-badge">{{ current?.roleLabel || me?.roleLabel }}</span>
          <button
            class="refresh-status-button"
            type="button"
            :disabled="refreshingTasks"
            @click="refreshTaskStatus"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20 11a8 8 0 0 0-14.2-5" />
              <path d="M5 4v5h5" />
              <path d="M4 13a8 8 0 0 0 14.2 5" />
              <path d="M19 20v-5h-5" />
            </svg>
            {{ refreshingTasks ? '同步中' : '刷新状态' }}
          </button>
        </div>
      </div>
      <div :class="['progress-strip', { 'captain-progress': isScoreRoundCaptain }]">
        <template v-if="isScoreRoundCaptain">
        <span>我的评分 <strong>{{ myScoredCount }}/{{ entries.length }}</strong></span>
        <span>本桌汇总 <strong>{{ finalizedCount }}/{{ entries.length }}</strong></span>
        <span v-if="!isFeedbackOnlyCompetition && advanceTargetCount > 0">晋级 <strong>{{ advancedCount }}/{{ advanceTargetCount }}</strong></span>
        </template>
        <template v-else-if="isRankingRound">
        <span>候选 <strong>{{ entries.length }}款</strong></span>
        <span v-if="showRankingFilledProgress">已排序 <strong>{{ rankingFilledCount }}/{{ rankingSlots.length }}</strong></span>
        </template>
        <span v-else>{{ progressLabel }} <strong>{{ progressCount }}</strong></span>
        <span v-if="scoreConfirmationVisible"><strong>{{ scoreConfirmationProgressLabel }}</strong></span>
      </div>
      <div v-if="showReviewStats" :class="['review-stats-strip', { empty: !showDetailedReviewStats }]">
        <template v-if="showDetailedReviewStats">
          <article class="review-stat-card">
            <span>我的平均用时</span>
            <strong>{{ myReviewStatsText.duration }}</strong>
            <small>{{ myReviewStatsText.siteDuration }}</small>
          </article>
          <article class="review-stat-card">
            <span>我的平均评语字数</span>
            <strong>{{ myReviewStatsText.comment }}</strong>
            <small>{{ myReviewStatsText.siteComment }}</small>
          </article>
        </template>
        <article v-else class="review-stat-empty">
          <strong>首款提交后显示评分统计</strong>
          <small>包含平均用时、评语字数和现场参考。</small>
        </article>
      </div>
    </section>

    <section v-if="!current" class="card action-card">
      <h2 class="section-title">暂未分配比赛</h2>
      <p class="caption">账号启用后，主办方会把你加入本场评审桌。</p>
      <button class="button secondary full empty-action" type="button" @click="router.push('/profile')">查看我的资料</button>
    </section>

    <template v-else-if="isRankingRound">
    <section v-if="rankingConfirmationVisible" :class="['card', 'confirmation-task-card', { done: rankingConfirmation?.mineConfirmed }]">
      <div class="split">
        <div>
          <h2 class="section-title compact">{{ rankingConfirmation.mineConfirmed ? '已确认本桌排序' : '请确认本桌排序' }}</h2>
          <p class="task-hint">{{ rankingConfirmationHint }}</p>
        </div>
        <span :class="['pill', rankingConfirmation.mineConfirmed ? 'status-ok' : 'status-warn']">{{ rankingConfirmationProgressText }}</span>
      </div>
      <button class="scan-button confirmation-button" type="button" @click="router.push(`/ranking-confirmation/${current.roundTableId}`)">
        {{ rankingConfirmation.mineConfirmed ? '查看本桌排序' : '查看待确认排序' }}
      </button>
    </section>

    <section class="card ranking-action-card">
      <div class="split">
        <div>
          <h2 class="section-title compact">{{ canSubmitRanking ? '本桌排序' : '本桌排序任务' }}</h2>
          <p v-if="rankingTaskHint" class="task-hint">{{ rankingTaskHint }}</p>
        </div>
        <span v-if="showRankingFilledProgress" class="pill status-warn">{{ rankingFilledCount }}/{{ rankingSlots.length }}</span>
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
      <p v-if="!entries.length" class="empty-note">当前还没有排序酒款，请联系现场工作人员确认。</p>
    </section>
    </template>

    <template v-else>
    <section v-if="scoreConfirmationVisible" :class="['card', 'confirmation-task-card', { done: scoreConfirmation?.mineConfirmed }]">
      <div class="split">
        <div>
          <h2 class="section-title compact">{{ scoreConfirmation.mineConfirmed ? '已确认本桌结果' : '请确认本桌结果' }}</h2>
          <p class="task-hint">{{ scoreConfirmationHint }}</p>
        </div>
        <span :class="['pill', scoreConfirmation.mineConfirmed ? 'status-ok' : 'status-warn']">{{ confirmationProgressText }}</span>
      </div>
      <button class="scan-button confirmation-button" type="button" @click="router.push(`/score-confirmation/${current.roundTableId}`)">
        {{ scoreConfirmation.mineConfirmed ? '查看本桌结果' : '查看待确认结果' }}
      </button>
    </section>

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
          <strong>{{ tableCheckoutTitle }}</strong>
          <p>{{ tableCheckoutText }}</p>
        </div>
        <button
          class="button secondary checkout-button"
          type="button"
          :disabled="!canOpenTableWorkbench"
          @click="router.push('/captain')"
        >
          {{ tableCheckoutActionLabel }}
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
              <span :class="entry.finalized || readyForFinalize(entry) ? 'ok-text' : 'warn-text'">汇总：{{ entryFinalizeText(entry) }}</span>
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
const currentRoundTable = ref(null)
const rankingSlots = ref([])
const scoreConfirmation = ref(null)
const rankingConfirmation = ref(null)
const scannerOpen = ref(false)
const manualCode = ref('')
const scannerMessage = ref('将二维码放入取景框内')
const loadingTasks = ref(true)
const refreshingTasks = ref(false)
let qrReader = null
let scanLocked = false
let syncingTasks = false

const currentTaskRole = computed(() => current.value?.role || current.value?.judgeRoleType || me.value?.role)
const isCaptain = computed(() => currentTaskRole.value === 'CAPTAIN')
const isScoreRoundCaptain = computed(() => isCaptain.value && current.value?.taskType === 'CAPTAIN_FINALIZE')
const isRankingRound = computed(() => isRankingTaskType(current.value?.taskType))
const isFeedbackOnlyCompetition = computed(() => (
  current.value?.competitionType === 'FEEDBACK_ONLY'
  || captainBoard.value?.competition?.competitionType === 'FEEDBACK_ONLY'
  || currentRoundTable.value?.competitionType === 'FEEDBACK_ONLY'
))
const canSubmitRanking = computed(() => (
  current.value?.taskType === 'RANKING_ROUND'
  && currentRoundTable.value?.canSubmitRanking !== false
))
const currentRoundNo = computed(() => parseRoundNo(current.value?.roundName || current.value?.flightName))
const showRankingFilledProgress = computed(() => (
  canSubmitRanking.value
  || currentRoundNo.value <= 1
))
const rankingTaskHint = computed(() => (
  canSubmitRanking.value
    ? ''
    : '请查看本桌候选，排序由桌长提交。'
))
const myScoredCount = computed(() => entries.value.filter((entry) => entry.scored).length)
const finalizedCount = computed(() => entries.value.filter((entry) => entry.finalized).length)
const advancedCount = computed(() => entries.value.filter((entry) => entry.advanced).length)
const advanceTargetCount = computed(() => Number(captainBoard.value?.roundTable?.targetCount || current.value?.targetCount || 0))
const myPendingScoreCount = computed(() => entries.value.filter((entry) => !entry.scored).length)
const readyFinalizeCount = computed(() => entries.value.filter((entry) => readyForFinalize(entry)).length)
const pendingTableScoreCount = computed(() => entries.value.reduce((sum, entry) => sum + entryMissingScoreCount(entry), 0))
const peerReadyWaitingMeCount = computed(() => entries.value.filter((entry) => !entry.scored && tableScoresComplete(entry)).length)
const rankingFilledCount = computed(() => rankingSlots.value.filter((slot) => slot.beerEntryId).length)
const scoreConfirmationVisible = computed(() => (
  !isCaptain.value
  && !isRankingRound.value
  && Boolean(scoreConfirmation.value?.readyForConfirmation)
))
const rankingConfirmationVisible = computed(() => (
  !isCaptain.value
  && isRankingRound.value
  && Boolean(rankingConfirmation.value?.readyForConfirmation)
))
const confirmationProgressText = computed(() => `确认 ${scoreConfirmation.value?.confirmedCount || 0}/${scoreConfirmation.value?.requiredCount || 0}`)
const rankingConfirmationProgressText = computed(() => `确认 ${rankingConfirmation.value?.confirmedCount || 0}/${rankingConfirmation.value?.requiredCount || 0}`)
const scoreConfirmationProgressLabel = computed(() => (
  scoreConfirmation.value?.mineConfirmed ? '已确认结果' : '待确认结果'
))
const myReviewStats = computed(() => currentRoundTable.value?.myReviewStats || null)
const showReviewStats = computed(() => Boolean(myReviewStats.value) && !isRankingRound.value)
const hasSubmittedReviewStats = computed(() => Number(myReviewStats.value?.submittedCount || 0) > 0)
const hasSiteReviewStats = computed(() => (
  Number(myReviewStats.value?.siteAverageDurationSeconds || 0) > 0
  || Number(myReviewStats.value?.siteAverageCommentChars || 0) > 0
))
const showDetailedReviewStats = computed(() => hasSubmittedReviewStats.value || hasSiteReviewStats.value)
const myReviewStatsText = computed(() => ({
  duration: hasSubmittedReviewStats.value ? formatSeconds(myReviewStats.value?.averageDurationSeconds) : '待提交',
  siteDuration: hasSiteReviewStats.value ? `现场平均 ${formatSeconds(myReviewStats.value?.siteAverageDurationSeconds)}` : '现场暂无参考',
  comment: hasSubmittedReviewStats.value ? formatCount(myReviewStats.value?.averageCommentChars, '字') : '待提交',
  siteComment: hasSiteReviewStats.value ? `现场平均 ${formatCount(myReviewStats.value?.siteAverageCommentChars, '字')}` : '现场暂无参考',
}))
const rankingConfirmationHint = computed(() => {
  if (!rankingConfirmation.value?.mineConfirmed) {
    return '桌长已整理本桌排序，请核对顺序和候选酒款后确认。'
  }
  const confirmed = Number(rankingConfirmation.value?.confirmedCount || 0)
  const required = Number(rankingConfirmation.value?.requiredCount || 0)
  return required > 0 && confirmed >= required
    ? '本桌确认已齐，等待主办方确认轮次。'
    : '你已完成确认，等待其他评审确认。'
})
const scoreConfirmationHint = computed(() => {
  if (!scoreConfirmation.value?.mineConfirmed) {
    return isFeedbackOnlyCompetition.value
      ? '桌长已整理共识分和综合评语，请核对后确认。'
      : '桌长已整理共识分、综合评语和晋级结果，请核对后确认。'
  }
  const confirmed = Number(scoreConfirmation.value?.confirmedCount || 0)
  const required = Number(scoreConfirmation.value?.requiredCount || 0)
  return required > 0 && confirmed >= required
    ? '本桌确认已齐，等待主办方确认轮次。'
    : '你已完成确认，等待其他评审确认。'
})
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
    : '扫码或点酒款进入评分，已评分酒款在确认前开放查看和调整。'
))
const tableReadyForReview = computed(() => {
  if (!entries.value.length) return false
  if (isFeedbackOnlyCompetition.value) return finalizedCount.value === entries.value.length
  const targetOk = advanceTargetCount.value <= 0 || advancedCount.value === advanceTargetCount.value
  return finalizedCount.value === entries.value.length && targetOk
})
const tableSubmitted = computed(() => (
  captainBoard.value?.roundTable?.status === 'SUBMITTED'
  || currentRoundTable.value?.status === 'SUBMITTED'
))
const tableCheckoutTitle = computed(() => {
  if (!entries.value.length) return ''
  if (tableSubmitted.value) return '本桌已提交'
  if (tableReadyForReview.value) return '等待同桌确认'
  if (readyFinalizeCount.value > 0 && myPendingScoreCount.value > 0) return '处理本桌待办'
  if (readyFinalizeCount.value > 0) return '有酒款可汇总'
  if (myPendingScoreCount.value > 0) return '先完成个人评分'
  if (pendingTableScoreCount.value > 0) return '等待同桌评分'
  return '本桌结果未完成'
})
const tableCheckoutText = computed(() => {
  if (!entries.value.length) return ''
  if (tableSubmitted.value) return '等待主办方确认轮次。'
  if (readyFinalizeCount.value > 0 && myPendingScoreCount.value > 0) {
    return `${readyFinalizeCount.value} 款可汇总，另有 ${myPendingScoreCount.value} 款个人评分待提交。`
  }
  if (myPendingScoreCount.value > 0) {
    if (peerReadyWaitingMeCount.value > 0) {
      return `${peerReadyWaitingMeCount.value} 款同桌评分已齐，提交你的个人评分后即可汇总。`
    }
    return `还有 ${myPendingScoreCount.value} 款个人评分待提交，下方可查看同桌进度。`
  }
  if (readyFinalizeCount.value > 0) {
    return `${readyFinalizeCount.value} 款评分已齐，请填写桌长意见。`
  }
  if (pendingTableScoreCount.value > 0) {
    return `还差 ${pendingTableScoreCount.value} 份同桌评分，齐全后再填写桌长意见。`
  }
  if (isFeedbackOnlyCompetition.value) {
    return '酒款诊断意见已齐，等待同桌确认。'
  }
  if (advanceTargetCount.value > 0 && advancedCount.value !== advanceTargetCount.value) {
    return `酒款意见已完成，还需按本桌目标确认 ${advanceTargetCount.value} 款晋级酒。`
  }
  return '酒款意见和晋级名单已齐，等待同桌确认。'
})
const tableCheckoutActionLabel = computed(() => {
  if (tableSubmitted.value) return '查看'
  if (tableReadyForReview.value) return '核对'
  if (readyFinalizeCount.value > 0) return '去汇总'
  if (myPendingScoreCount.value > 0) return '去评分'
  return '看进度'
})
const canOpenTableWorkbench = computed(() => Boolean(entries.value.length))
const emptyStateTitle = computed(() => {
  if (loadingTasks.value) return '正在载入本桌酒款'
  if (!current.value?.roundTableId) return '你还没有加入本轮评审桌'
  if (current.value?.taskType !== 'CAPTAIN_FINALIZE') return '当前轮次不是第一轮汇总任务'
  return '本桌还没有酒款'
})
const emptyStateMessage = computed(() => {
  if (loadingTasks.value) return '正在同步本轮酒款和评分进度。'
  if (!current.value?.roundTableId) return '请现场工作人员先把你加入本轮评审桌。'
  if (current.value?.taskType !== 'CAPTAIN_FINALIZE') return '请联系现场工作人员确认第一轮评分任务。'
  return '请现场工作人员确认本轮、评审桌和酒款分配。'
})
const emptyStateChecks = computed(() => (
  loadingTasks.value
    ? []
    : [
      `评审桌：${me.value?.tableName || current.value?.tableName || '未分配'}`,
      `当前轮次：${current.value?.flightName || '未发布'}`,
    ]
))

function openEntry(uuid) {
  if (!uuid) return
  router.push(`/scan-result/${uuid.toUpperCase()}`)
}

function parseRoundNo(value) {
  const text = String(value || '').trim()
  const numeric = text.match(/\d+/)
  if (numeric) return Number(numeric[0])
  const chineseNumbers = {
    一: 1,
    二: 2,
    三: 3,
    四: 4,
    五: 5,
    六: 6,
    七: 7,
    八: 8,
    九: 9,
    十: 10,
  }
  const chinese = text.match(/第?\s*([一二三四五六七八九十])\s*轮/)
  return chineseNumbers[chinese?.[1]] || 1
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

function formatSeconds(value) {
  const seconds = Number(value || 0)
  if (!seconds) return '--'
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60)
    const remainMinutes = minutes % 60
    return remainMinutes ? `${hours}小时${remainMinutes}分` : `${hours}小时`
  }
  return `${minutes}分${rest}秒`
}

function formatCount(value, unit) {
  const count = Number(value || 0)
  return count > 0 ? `${count}${unit}` : `--`
}

function entryStatus(entry) {
  if (entry.finalized) {
    if (isFeedbackOnlyCompetition.value) return { label: '已诊断', className: 'status-lock' }
    return entry.advanced
      ? { label: '已晋级', className: 'status-ok' }
      : { label: '已确认', className: 'status-lock' }
  }
  if (isCaptain.value) {
    if (!entry.scored) return { label: tableScoresComplete(entry) ? '待我评' : '待我评分', className: 'status-warn' }
    if (readyForFinalize(entry)) return { label: '可汇总', className: 'status-ok' }
    if (entryMissingScoreCount(entry) > 0) return { label: '等同桌', className: 'status-warn' }
    return { label: '待汇总', className: 'status-warn' }
  }
  if (entry.scored) return { label: '已评分', className: 'status-ok' }
  return { label: '待评分', className: '' }
}

function entryMissingScoreCount(entry) {
  const expected = Number(entry.expectedCount || 0)
  const submitted = Number(entry.submittedCount || 0)
  return expected > 0 ? Math.max(0, expected - submitted) : 0
}

function entryFinalizeText(entry) {
  if (entry.finalized) return '已汇总'
  if (readyForFinalize(entry)) return '可汇总'
  if (!entry.scored) return '待本人评分'
  const missing = entryMissingScoreCount(entry)
  return missing > 0 ? '待同桌评分' : '待汇总'
}

function tableScoreProgress(entry) {
  const expected = Number(entry.expectedCount || 0)
  const submitted = Number(entry.submittedCount || 0)
  if (expected <= 0) return `${submitted} 份`
  return submitted >= expected ? `已齐 ${submitted} / ${expected}` : `${submitted} / ${expected}`
}

function readyForFinalize(entry) {
  const expected = Number(entry.expectedCount || 0)
  const submitted = Number(entry.submittedCount || 0)
  return Boolean(entry.scored && !entry.finalized && submitted >= expected)
}

function tableScoresComplete(entry) {
  const expected = Number(entry.expectedCount || 0)
  const submitted = Number(entry.submittedCount || 0)
  return expected > 0 && submitted >= expected
}

function captainAction(entry) {
  if (entry.finalized) return { label: '查看意见', primary: false }
  if (!entry.scored) return { label: '去评分', primary: true }
  if (readyForFinalize(entry)) return { label: '汇总意见', primary: true }
  if (entryMissingScoreCount(entry) > 0) return { label: '看进度', primary: false }
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

async function syncTaskDashboard(options = {}) {
  if (syncingTasks) return
  syncingTasks = true
  if (options.initial) {
    loadingTasks.value = true
  } else {
    refreshingTasks.value = true
  }
  try {
    const competitions = await fetchCompetitions()
    current.value = selectCurrentTask(competitions)
    captainBoard.value = null
    currentRoundTable.value = null
    rankingSlots.value = []
    scoreConfirmation.value = null
    rankingConfirmation.value = null
    if (!current.value || !current.value.roundTableId) {
      entries.value = []
      return
    }
    if (current.value.taskType === 'CAPTAIN_FINALIZE') {
      const board = await fetchCaptainBoard(current.value.roundTableId)
      captainBoard.value = board
      currentRoundTable.value = board.roundTable || null
      entries.value = board.entries || []
      return
    }
    const [table, myScores] = isRankingRound.value
      ? [await fetchRoundTable(current.value.roundTableId), []]
      : await Promise.all([
        fetchRoundTable(current.value.roundTableId),
        fetchMyScores(),
      ])
    currentRoundTable.value = table
    rankingSlots.value = table.rankings || []
    scoreConfirmation.value = table.scoreConfirmation || null
    rankingConfirmation.value = table.rankingConfirmation || null
    const myScoredUuids = new Set(myScores.map((score) => score.beerUuid))
    entries.value = (table.entries || []).map((entry) => ({
      ...entry,
      scored: myScoredUuids.has(entry.uuid),
      finalized: Boolean(entry.advanced),
    }))
  } finally {
    if (options.initial) {
      loadingTasks.value = false
    } else {
      refreshingTasks.value = false
    }
    syncingTasks = false
  }
}

function refreshTaskStatus() {
  return syncTaskDashboard()
}

function refreshWhenPageVisible() {
  if (document.visibilityState === 'visible') {
    refreshTaskStatus()
  }
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
  await syncTaskDashboard({ initial: true })
  document.addEventListener('visibilitychange', refreshWhenPageVisible)
})

onBeforeUnmount(() => {
  stopScanner()
  document.removeEventListener('visibilitychange', refreshWhenPageVisible)
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

.review-actions {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  gap: 7px;
  align-items: flex-end;
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

.refresh-status-button {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  min-height: 28px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 4px 9px;
  color: rgba(255, 255, 255, 0.86);
  background: rgba(255, 255, 255, 0.075);
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.refresh-status-button svg {
  width: 13px;
  height: 13px;
  stroke: currentColor;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
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

.review-stats-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.review-stats-strip.empty {
  grid-template-columns: minmax(0, 1fr);
}

.review-stat-card {
  display: grid;
  gap: 4px;
  min-width: 0;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.075);
}

.review-stat-card span,
.review-stat-card small {
  overflow: hidden;
  color: rgba(248, 250, 252, 0.68);
  font-size: 11px;
  font-weight: 750;
  line-height: 1.25;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.review-stat-empty {
  display: grid;
  gap: 3px;
  min-width: 0;
  border: 1px dashed rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.055);
}

.review-stat-empty strong,
.review-stat-empty small {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.review-stat-empty strong {
  color: #fff;
  font-size: 13px;
  line-height: 1.3;
  font-weight: 850;
}

.review-stat-empty small {
  color: rgba(248, 250, 252, 0.66);
  font-size: 11px;
  line-height: 1.25;
  font-weight: 750;
}

.review-stat-card strong {
  overflow: hidden;
  color: #fff;
  font-size: 17px;
  line-height: 1.1;
  font-weight: 900;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.action-card {
  margin-top: 12px;
}

.ranking-action-card {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.confirmation-task-card {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  border-color: #f3c04f;
  background: #fffaf0;
}

.confirmation-task-card.done {
  border-color: #abefc6;
  background: #f0fdf4;
}

.ranking-button {
  background: #1f2a37;
}

.confirmation-button {
  background: #9a5b26;
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
