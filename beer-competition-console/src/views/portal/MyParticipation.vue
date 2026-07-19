<template>
  <div class="my-page">
    <section class="my-hero">
      <div>
        <span class="label-chip tone-green">我的参赛</span>
        <h1>{{ breweryProfile.breweryName || '完善厂牌资料' }}</h1>
        <p>{{ heroCopy }}</p>
      </div>
      <RouterLink class="hero-reminder" :to="heroReminder.to">
        {{ heroReminder.label }}
      </RouterLink>
      <RouterLink class="hero-action" :to="heroAction.to">{{ heroAction.label }}</RouterLink>
    </section>

    <section class="brewer-card progress-panel">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">我的赛事进度</h2>
        </div>
        <RouterLink to="/portal/events">浏览开放赛事</RouterLink>
      </div>

      <div class="competition-progress">
        <article v-for="competition in myCompetitions" :key="competition.id" class="progress-row">
          <div class="progress-main">
            <span :class="['label-chip', competition.status === 'PUBLISHED' ? 'tone-gold' : 'tone-green']">
              {{ competition.currentStageLabel }}
            </span>
            <h3>{{ competition.name }}</h3>
          </div>
          <div class="progress-status">
            <p>{{ competitionProgressText(competition) }}</p>
            <div class="progress-chips" aria-label="赛事进度摘要">
              <span v-for="chip in progressChips(competition)" :key="chip.label">
                <small>{{ chip.label }}</small>
                <b>{{ chip.value }}</b>
              </span>
            </div>
          </div>
          <div class="row-actions">
            <RouterLink
              v-for="action in competitionActions(competition)"
              :key="action.label"
              :class="{ 'is-primary': action.primary }"
              :to="action.to"
            >
              {{ action.label }}
            </RouterLink>
          </div>
        </article>
      </div>
    </section>

    <section class="brewer-card recent-panel">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">最近酒款</h2>
        </div>
        <RouterLink to="/portal/entries">全部酒款</RouterLink>
      </div>

      <div class="entry-table">
        <article v-for="entry in entries.slice(0, 4)" :key="entry.id" class="entry-row">
          <div class="entry-main">
            <span :class="['label-chip', `tone-${entryStatusMeta[entry.status].tone}`]">
              {{ entryStatusMeta[entry.status].label }}
            </span>
            <h3>{{ entry.name }}</h3>
            <p>{{ competitionName(entry.competitionId) }}</p>
          </div>
          <div class="entry-status">
            <p>{{ entryStatusText(entry) }}</p>
            <span v-if="showShortCode(entry)" class="short-code">
              <small>现场编号</small>
              <b>{{ entry.shortCode || '待生成' }}</b>
            </span>
          </div>
          <RouterLink :to="recentEntryAction(entry).to">{{ recentEntryAction(entry).label }}</RouterLink>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchMyParticipation } from '@/api/portal'
import {
  competitionResultPath,
  canSubmitEntry,
  entryPrimaryAction,
  entryStatusMeta,
  entrySummaryForCompetition,
  hasEntryDeliveryProgress,
  isEntryDeliveryActionPending,
  isEntryPaymentPending,
  isEntryStored,
  isEntryResultPublished,
  nextActionText,
  priorityEntry,
} from './portalViewModels'

const profile = ref({})
const entries = ref([])
const competitions = ref([])
const breweryProfile = computed(() => ({
  breweryName: profile.value.companyName || profile.value.displayName || '完善厂牌资料',
}))
const highestPriorityEntry = computed(() => priorityEntry(entries.value))
const heroAction = computed(() => entryPrimaryAction(highestPriorityEntry.value))
const heroCopy = computed(() => {
  if (unpaidEntries.value.length > 0) {
    return `有 ${unpaidEntries.value.length} 款酒需要支付报名费`
  }
  if (labelEntries.value.length > 0) {
    return `有 ${labelEntries.value.length} 款酒需要办理送样`
  }
  if (waitingDeliveryEntries.value.length > 0) {
    return `有 ${waitingDeliveryEntries.value.length} 款酒等待组委会确认入库`
  }
  if (resultEntries.value.length > 0) {
    return `有 ${resultEntries.value.length} 款酒的结果已经发布`
  }
  return '目前没有待处理的参赛事项，请浏览开放报名赛事'
})

const unpaidEntries = computed(() => entries.value.filter((entry) => isEntryPaymentPending(entry)))
const labelEntries = computed(() => entries.value.filter((entry) => isEntryDeliveryActionPending(entry)))
const waitingDeliveryEntries = computed(() => entries.value.filter((entry) => entry.status === 'REGISTERED' && hasEntryDeliveryProgress(entry) && !isEntryStored(entry)))
const resultEntries = computed(() => entries.value.filter((entry) => isEntryResultPublished(entry)))
const myCompetitions = computed(() => competitions.value)

const heroReminder = computed(() => {
  if (unpaidEntries.value.length > 0) {
    return { label: `${unpaidEntries.value.length} 款待支付`, to: entryPrimaryAction(unpaidEntries.value[0]).to }
  }
  if (labelEntries.value.length > 0) {
    return { label: `${labelEntries.value.length} 款待办理送样`, to: entryPrimaryAction(labelEntries.value[0]).to }
  }
  if (waitingDeliveryEntries.value.length > 0) {
    return { label: `${waitingDeliveryEntries.value.length} 款等待确认`, to: entryPrimaryAction(waitingDeliveryEntries.value[0]).to }
  }
  if (resultEntries.value.length > 0) {
    return { label: `${resultEntries.value.length} 款结果可看`, to: '/portal/results' }
  }
  return { label: '暂无待处理', to: '/portal/entries' }
})

function summary(competitionId) {
  return entrySummaryForCompetition(competitionId, entries.value)
}

function competitionProgressText(competition) {
  const item = summary(competition.id)
  if (!item.submitted) return '本场暂无酒款记录'
  if (item.pendingPayment > 0) {
    return '还有酒款待支付，处理后才能办理送样'
  }
  if (item.deliveryActionPending > 0) {
    return '请办理送样，并按赛事要求贴好现场标签'
  }
  if (item.deliverySubmitted > 0 || item.registered > 0) {
    return '送样信息已提交，等待组委会确认入库'
  }
  if (item.result > 0) {
    return isFeedbackOnlyCompetition(competition)
      ? '结果已发布，结果页展示本场评分、评语和诊断'
      : '结果已发布，结果页展示本场评分、评语和奖项'
  }
  if (item.stored > 0) {
    return '样品已入库，等待组委会发布评审结果'
  }
  return '报名记录已提交，请继续关注支付、标签和送样状态'
}

function progressChips(competition) {
  const item = summary(competition.id)
  const chips = [
    { label: '已提交', value: `${item.submitted} 款` },
  ]
  if (item.pendingPayment > 0) {
    chips.push({ label: '待支付', value: `${item.pendingPayment} 款` })
  } else if (item.deliveryActionPending > 0) {
    chips.push({ label: '需处理', value: `${item.deliveryActionPending} 款` })
  } else if (item.deliverySubmitted > 0 || item.registered > 0) {
    chips.push({ label: '待确认', value: `${item.deliverySubmitted || item.registered} 款` })
  } else if (item.stored > 0) {
    chips.push({ label: '已入库', value: `${item.stored} 款` })
  }
  chips.push(item.result > 0
    ? { label: '结果可查', value: `${item.result} 款` }
    : { label: '结果状态', value: '待发布' })
  return chips
}

function competitionActions(competition) {
  const item = summary(competition.id)
  const entriesPath = `/portal/entries?competitionId=${competition.id}`
  const entriesAction = { label: '查看酒款', to: entriesPath }
  const tertiaryAction = canSubmitEntry(competition)
    ? { label: '再报一款酒', to: `/portal/submit?competitionId=${competition.id}` }
    : { label: '赛事详情', to: `/portal/events/${competition.id}` }

  if (item.pendingPayment > 0) {
    const pendingEntries = entries.value.filter((entry) => entry.competitionId === competition.id && isEntryPaymentPending(entry))
    const pendingEntry = pendingEntries.find((entry) => entry.paymentStatus !== 'PENDING_CONFIRM') || pendingEntries[0]
    const paymentAction = entryPrimaryAction(pendingEntry)
    return [
      { ...paymentAction, primary: true },
      entriesAction,
      tertiaryAction,
    ]
  }
  if (item.deliveryActionPending > 0) {
    const deliveryEntry = entries.value.find((entry) => entry.competitionId === competition.id && isEntryDeliveryActionPending(entry))
    return [
      { label: '办理送样', to: entryPrimaryAction(deliveryEntry).to, primary: true },
      entriesAction,
      tertiaryAction,
    ]
  }
  if (item.deliverySubmitted > 0 || item.registered > 0) {
    const progressEntry = entries.value.find((entry) => entry.competitionId === competition.id && entry.status === 'REGISTERED')
    return [
      { label: '查看送样进度', to: entryPrimaryAction(progressEntry).to, primary: true },
      entriesAction,
      tertiaryAction,
    ]
  }
  if (item.result > 0) {
    return [
      { label: '查看结果', to: competitionResultPath(competition.id), primary: true },
      entriesAction,
      { label: '赛事详情', to: `/portal/events/${competition.id}` },
    ]
  }
  return [
    { ...entriesAction, primary: true },
    tertiaryAction,
  ]
}

function recentEntryAction(entry) {
  const action = entryPrimaryAction(entry)
  if (action.to === '/portal/entries' || action.label === '查看参赛进度' || action.label === '查看酒款资料') {
    return { label: action.label === '查看参赛进度' ? '查看参赛进度' : '查看酒款资料', to: `/portal/entries?competitionId=${entry.competitionId}&entryId=${entry.id}` }
  }
  return action
}

function entryStatusText(entry) {
  if (isEntryPaymentPending(entry)) {
    return '待支付报名费，支付后才能办理送样'
  }
  if (isEntryDeliveryActionPending(entry)) {
    return '请办理送样，并按赛事要求贴好现场标签'
  }
  if (entry.status === 'REGISTERED') {
    return hasEntryDeliveryProgress(entry) ? '送样信息已提交，等待组委会确认入库' : '请办理送样，并按赛事要求贴好现场标签'
  }
  if (isEntryResultPublished(entry)) {
    return isFeedbackOnlyEntry(entry)
      ? '结果已发布，结果页展示评分、评语和诊断'
      : '结果已发布，结果页展示评分、评语和奖项'
  }
  if (entry.status === 'STORED') {
    return '样品已入库，等待组委会发布评审结果'
  }
  return nextActionText(entry)
}

function showShortCode(entry) {
  return entry.status === 'REGISTERED' && !isEntryStored(entry) && !isEntryResultPublished(entry)
}

function competitionName(competitionId) {
  return competitions.value.find((competition) => competition.id === competitionId)?.name || '未关联赛事'
}

function isFeedbackOnlyCompetition(competition) {
  if (competition?.competitionType === 'FEEDBACK_ONLY') return true
  return entries.value.some((entry) => entry.competitionId === competition?.id && isFeedbackOnlyEntry(entry))
}

function isFeedbackOnlyEntry(entry) {
  return entry?.competitionType === 'FEEDBACK_ONLY'
}

onMounted(async () => {
  const data = await fetchMyParticipation()
  profile.value = data.profile || {}
  entries.value = data.entries || []
  competitions.value = data.competitions || []
})
</script>

<style scoped>
.my-page {
  display: grid;
  gap: 22px;
}

.my-hero {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 22px;
  min-height: 300px;
  padding: 32px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(31, 21, 14, 0.95), rgba(89, 49, 18, 0.88)),
    url("https://images.unsplash.com/photo-1505075106905-fb052892c116?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
}

.my-hero h1 {
  margin: 18px 0 10px;
  font-size: 48px;
  line-height: 1.05;
}

.my-hero p {
  max-width: 680px;
  margin: 0;
  color: #ead9b7;
  font-size: 17px;
  line-height: 1.7;
}

.hero-action,
.hero-reminder,
.row-actions a,
.entry-row > a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 15px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.hero-reminder {
  position: absolute;
  top: 24px;
  right: 24px;
  min-height: 32px;
  padding: 0 12px;
  color: #fff8e8;
  background: rgba(255, 250, 240, 0.12);
  border: 1px solid rgba(255, 250, 240, 0.22);
  border-radius: 999px;
  font-size: 13px;
  backdrop-filter: blur(8px);
}

.progress-panel,
.recent-panel {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.section-head p {
  margin: 0;
  color: #746a5f;
}

.section-head a {
  color: #8b5c19;
  font-weight: 800;
  text-decoration: none;
}

.competition-progress,
.entry-table {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.progress-row {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(340px, 1.2fr) auto;
  gap: 20px;
  align-items: center;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.progress-row h3,
.entry-row h3 {
  margin: 12px 0 6px;
  font-size: 22px;
}

.progress-main {
  min-width: 0;
}

.progress-status,
.entry-main,
.entry-status {
  min-width: 0;
}

.progress-row p,
.entry-row p {
  color: #746a5f;
}

.progress-row p,
.entry-status p {
  margin: 0;
  line-height: 1.65;
}

.progress-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.progress-chips span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 4px 10px;
  color: #5b4a35;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.08);
  border-radius: 999px;
}

.progress-chips small {
  color: #827563;
  font-size: 12px;
}

.progress-chips b {
  color: #2b1d10;
  font-size: 13px;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 220px;
}

.row-actions a {
  color: #6b4710;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.row-actions a.is-primary {
  color: #2b1d10;
  background: #e1a23d;
  border-color: transparent;
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(320px, 1.1fr) auto;
  gap: 20px;
  align-items: center;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.short-code {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  min-height: 30px;
  margin-top: 12px;
  padding: 4px 10px;
  color: #5b4a35;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.08);
  border-radius: 999px;
}

.short-code small {
  color: #827563;
  font-size: 12px;
}

.short-code b {
  color: #2b1d10;
  font-size: 13px;
}

@media (max-width: 1180px) {
  .progress-row,
  .entry-row {
    grid-template-columns: 1fr;
  }

  .progress-status {
    padding-top: 2px;
  }

  .row-actions {
    justify-content: flex-start;
    min-width: 0;
  }
}

@media (max-width: 720px) {
  .my-hero,
  .section-head {
    display: grid;
  }

  .my-hero h1 {
    font-size: 36px;
  }

  .hero-reminder {
    position: static;
    width: fit-content;
  }
}
</style>
