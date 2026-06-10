<template>
  <div class="event-detail-page">
    <RouterLink class="back-link" to="/portal/events">&lt; 返回</RouterLink>

    <section class="detail-hero">
      <div class="hero-copy">
        <span :class="['label-chip', stageTone(competition.status)]">
          {{ stageLabel }}
        </span>
        <h1>{{ competition.name || '赛事详情' }}</h1>
        <p>{{ competition.description || competition.code || '-' }}</p>
        <div class="hero-facts" aria-label="赛事关键时间和费用">
          <span>报名截止 {{ formatMonthDayTime(competition.registrationDeadline) }}</span>
          <span>{{ feeText }}</span>
          <span>送样截止 {{ formatMonthDayTime(logistics.sampleArrivalDeadline) }}</span>
        </div>
        <div class="hero-actions">
          <RouterLink
            v-if="heroPrimaryAction"
            class="primary-action"
            :to="heroPrimaryAction.to"
          >
            {{ heroPrimaryAction.label }}
          </RouterLink>
        </div>
      </div>
    </section>

    <section class="detail-section brewer-card">
      <div class="section-heading">
        <div>
          <h2 class="portal-section-title">赛事简介</h2>
        </div>
        <a
          v-if="competition.rulesUrl"
          class="rules-link"
          :href="competition.rulesUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          查看参赛细则
        </a>
      </div>
      <article class="description-panel">
        <p>{{ fullDescription }}</p>
      </article>
    </section>

    <section class="content-grid">
      <article class="brewer-card info-panel">
        <h2 class="portal-section-title">赛事关键信息</h2>
        <dl class="key-info-list">
          <div><dt>投递组别</dt><dd>{{ categoryNamesText }}</dd></div>
          <div><dt>比赛日期</dt><dd>{{ formatDate(competition.competitionDate) }}</dd></div>
          <div><dt>报名截止</dt><dd>{{ formatDateTime(competition.registrationDeadline) }}</dd></div>
          <div><dt>当前应付</dt><dd>{{ feeText }}</dd></div>
          <div><dt>送样截止</dt><dd>{{ formatDateTime(logistics.sampleArrivalDeadline) }}</dd></div>
          <div v-if="earlyBirdDeadlineText"><dt>早鸟截止</dt><dd>{{ earlyBirdDeadlineText }}</dd></div>
          <div v-if="showNormalFee"><dt>普通报名费</dt><dd>¥{{ competition.entryFee }} / 款</dd></div>
        </dl>
      </article>

      <article class="brewer-card info-panel">
        <h2 class="portal-section-title">送样与标签</h2>
        <dl>
          <div><dt>送样方式</dt><dd>{{ deliveryMethodText(logistics.deliveryMethod) }}</dd></div>
          <div><dt>建议送达</dt><dd>{{ arrivalWindowText }}</dd></div>
          <div><dt>酒样要求</dt><dd>{{ logistics.sampleQuantityNote || '以主办方后续通知为准' }}</dd></div>
          <div v-if="logistics.deliveryNote"><dt>包装说明</dt><dd>{{ logistics.deliveryNote }}</dd></div>
        </dl>
      </article>
    </section>

    <section class="detail-section brewer-card">
      <div class="section-heading">
        <div>
          <h2 class="portal-section-title">参赛流程</h2>
          <p>从报名资料到结果发布，厂商端主要关注这些节点。</p>
        </div>
      </div>
      <div class="process-strip">
        <span v-for="(step, index) in processSteps" :key="step.title">
          <small>{{ String(index + 1).padStart(2, '0') }}</small>
          <b>{{ step.title }}</b>
          <em>{{ step.text }}</em>
        </span>
      </div>
    </section>

    <section class="brewer-card participation-brief">
      <template v-if="loggedIn && hasEventEntries">
        <div>
          <span class="label-chip tone-blue">你已参加本赛事</span>
          <h2>本场参赛状态</h2>
          <p>具体酒款、标签和送样处理请进入“我的参赛”。</p>
        </div>
        <div class="brief-stats">
          <span><small>已提交</small><b>{{ eventSummary.submitted }}</b></span>
          <span><small>待支付</small><b>{{ eventSummary.pendingPayment }}</b></span>
          <span><small>已入库</small><b>{{ eventSummary.stored }}</b></span>
          <span><small>结果可查</small><b>{{ eventSummary.result }}</b></span>
        </div>
        <div class="brief-actions">
          <RouterLink to="/portal/my">进入我的参赛</RouterLink>
          <RouterLink v-if="canSubmitEntry(competition)" :to="`/portal/submit?competitionId=${competition.id}`">再报一款酒</RouterLink>
          <RouterLink v-if="eventSummary.result > 0" :to="competitionResultPath(competition.id)">查看我的结果</RouterLink>
        </div>
      </template>

      <template v-else-if="loggedIn">
        <div>
          <span class="label-chip tone-amber">尚未参加本赛事</span>
          <h2>确认规则后报名参赛</h2>
          <p>先核对组别、基础风格、费用和送样要求。</p>
        </div>
        <div class="brief-actions">
          <RouterLink v-if="canSubmitEntry(competition)" :to="`/portal/submit?competitionId=${competition.id}`">报名参赛</RouterLink>
          <RouterLink to="/portal/my">查看我的参赛</RouterLink>
        </div>
      </template>

      <template v-else>
        <div>
          <span class="label-chip tone-amber">公开赛事详情</span>
          <h2>登录后报名并追踪参赛酒款</h2>
          <p>未登录时可以先查看赛事规则、费用和送样要求。</p>
        </div>
        <div class="brief-actions">
          <RouterLink to="/portal/login">登录报名</RouterLink>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { fetchPortalCompetitionDetail, fetchPortalEntries } from '@/api/portal'
import {
  canSubmitEntry,
  competitionResultPath,
  entrySummaryForCompetition,
  formatCompetitionFee,
  isCompetitionResultPublished,
  isEarlyBirdActive,
} from './portalViewModels'

const route = useRoute()
const loggedIn = computed(() => isLoggedIn('portal'))
const competition = ref({ categories: [], styles: [], entryFields: [] })
const entries = ref([])

const logistics = computed(() => competition.value.logistics || {})
const eventEntries = computed(() => entries.value.filter((entry) => entry.competitionId === competition.value.id))
const hasEventEntries = computed(() => eventEntries.value.length > 0)
const eventSummary = computed(() => entrySummaryForCompetition(competition.value.id, entries.value))
const feeText = computed(() => formatCompetitionFee(competition.value))
const earlyBirdDeadlineText = computed(() => (isEarlyBirdActive(competition.value) ? formatDateTime(competition.value.earlyBirdDeadline) : ''))
const showNormalFee = computed(() => isEarlyBirdActive(competition.value) && competition.value.entryFee !== undefined && competition.value.entryFee !== null)
const stageLabel = computed(() => (isCompetitionResultPublished(competition.value) ? '结果已发布' : competition.value.currentStageLabel) || '赛事详情')
const categoryNamesText = computed(() => {
  const names = competition.value.categories?.map((item) => item.name).filter(Boolean) || []
  return names.length ? names.join(' / ') : '暂未配置'
})
const fullDescription = computed(() => {
  return competition.value.description || '主办方暂未填写赛事简介，请以参赛细则和后续通知为准。'
})
const heroPrimaryAction = computed(() => {
  if (isCompetitionResultPublished(competition.value)) {
    return {
      label: loggedIn.value ? '查看我的结果' : '登录查看结果',
      to: loggedIn.value ? competitionResultPath(competition.value.id) : '/portal/login',
    }
  }
  if (canSubmitEntry(competition.value)) {
    return {
      label: loggedIn.value ? '报名参赛' : '登录后报名',
      to: loggedIn.value ? `/portal/submit?competitionId=${competition.value.id}` : '/portal/login',
    }
  }
  return null
})
const arrivalWindowText = computed(() => {
  const start = formatDateTime(logistics.value.sampleArrivalStart)
  const deadline = formatDateTime(logistics.value.sampleArrivalDeadline)
  if (start !== '-' && deadline !== '-') return `${start} 至 ${deadline}`
  if (deadline !== '-') return `${deadline} 前送达`
  return '以主办方通知为准'
})
const processSteps = [
  { title: '提交资料', text: '填写酒名、组别、基础风格、ABV 和补充字段。' },
  { title: '支付报名费', text: '支付成功后报名完成，并开放标签相关操作。' },
  { title: '下载标签', text: '将现场标签贴在酒瓶或外箱，便于主办方核对。' },
  { title: '送样入库', text: '按送样窗口寄送或现场交样，等待主办方确认。' },
  { title: '查看结果', text: '结果发布后查看评分、评语、奖项和证书。' },
]

onMounted(async () => {
  competition.value = await fetchPortalCompetitionDetail(route.params.id)
  if (loggedIn.value) {
    entries.value = await fetchPortalEntries()
  }
})

function stageTone(status) {
  if (status === 'PUBLISHED' || status === 'ARCHIVED') return 'tone-gold'
  if (status === 'REGISTRATION_OPEN') return 'tone-green'
  if (status === 'JUDGING') return 'tone-blue'
  return 'tone-amber'
}

function formatDate(value) {
  return value || '-'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function formatMonthDayTime(value) {
  if (!value) return '-'
  const normalized = String(value).replace('T', ' ')
  const match = normalized.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/)
  if (!match) return formatDateTime(value)
  const [, month, day, hour, minute] = match
  return `${Number(month)}月${Number(day)}日 ${hour}:${minute}`
}

function deliveryMethodText(value) {
  if (value === 'EXPRESS') return '快递寄送'
  if (value === 'ONSITE') return '现场送样'
  return '快递寄送 / 现场送样'
}
</script>

<style scoped>
.event-detail-page {
  display: grid;
  gap: 14px;
}

.back-link {
  justify-self: start;
  color: #7d705f;
  font-weight: 800;
  text-decoration: none;
}

.back-link:hover {
  color: #2b1d10;
}

.detail-hero {
  display: grid;
  min-height: 360px;
  padding: 30px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(31, 21, 14, 0.94), rgba(86, 48, 18, 0.82)),
    url("https://images.unsplash.com/photo-1518099074172-2e47ee6cfdc0?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
}

.hero-copy {
  align-self: end;
}

.detail-hero h1 {
  max-width: 880px;
  margin: 18px 0 10px;
  overflow-wrap: anywhere;
  font-size: 48px;
  line-height: 1.05;
}

.detail-hero p {
  display: -webkit-box;
  max-width: 760px;
  margin: 0;
  overflow: hidden;
  color: #ead9b7;
  font-size: 16px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.hero-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-facts span {
  min-height: 34px;
  padding: 7px 12px;
  color: #fff8e8;
  background: rgba(255, 250, 240, 0.12);
  border: 1px solid rgba(255, 250, 240, 0.22);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
  line-height: 18px;
  white-space: nowrap;
}

.hero-actions,
.brief-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.primary-action,
.brief-actions a,
.card-link {
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

.participation-brief {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 22px;
}

.participation-brief h2 {
  margin: 12px 0 6px;
  font-size: 26px;
  line-height: 1.18;
}

.participation-brief p,
.section-heading p,
.requirement-block p,
dt,
.process-strip em,
.requirement-list small {
  color: #746a5f;
  line-height: 1.65;
}

.brief-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(82px, 1fr));
  gap: 8px;
}

.brief-stats span {
  min-height: 72px;
  padding: 11px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.brief-stats small,
.brief-stats b {
  display: block;
}

.brief-stats small {
  color: #746a5f;
}

.brief-stats b {
  margin-top: 7px;
  font-size: 25px;
  line-height: 1;
}

.brief-actions {
  grid-column: 1 / -1;
  margin-top: 0;
}

.brief-actions a + a {
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.detail-section,
.info-panel {
  padding: 24px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.section-heading .portal-section-title {
  margin: 0 0 8px;
}

.section-heading p {
  margin: 0;
}

.rules-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  min-height: 38px;
  padding: 0 13px;
  color: #6b4710;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 8px;
  background: #fff7e6;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.description-panel {
  max-width: 920px;
  padding: 22px 24px;
  background:
    linear-gradient(180deg, rgba(255, 253, 247, 0.96), rgba(255, 247, 230, 0.92));
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.description-panel p {
  max-width: 820px;
  margin: 0;
  color: #4f463b;
  font-size: 16px;
  line-height: 1.9;
  text-align: justify;
}

.requirement-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.6fr;
  gap: 14px;
}

.requirement-block {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 180px;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.requirement-block > span {
  color: #746a5f;
  font-size: 13px;
}

.requirement-block strong {
  overflow-wrap: anywhere;
  color: #2b1d10;
  font-size: 23px;
  line-height: 1.25;
}

.entry-requirements {
  gap: 14px;
}

.requirement-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.requirement-list span {
  display: grid;
  gap: 4px;
  min-height: 82px;
  padding: 12px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.08);
  border-radius: 8px;
}

.requirement-list b {
  color: #2b1d10;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip-list em {
  padding: 6px 9px;
  color: #6b4710;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
  font-style: normal;
  font-weight: 800;
}

.style-details {
  margin-top: 2px;
}

.style-details summary {
  cursor: pointer;
  color: #8b5c19;
  font-weight: 900;
}

.style-groups {
  display: grid;
  gap: 10px;
  max-height: 360px;
  margin-top: 12px;
  overflow: auto;
}

.style-group {
  padding: 12px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.08);
  border-radius: 8px;
}

.style-group h3 {
  margin: 0 0 6px;
  font-size: 15px;
}

.style-group p,
.requirement-block p {
  margin: 0;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

dl {
  display: grid;
  gap: 12px;
  margin: 0;
}

.key-info-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

dl div {
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(87, 58, 26, 0.16);
}

dt,
dd {
  margin: 0;
}

dd {
  margin-top: 6px;
  overflow-wrap: anywhere;
  font-weight: 800;
  line-height: 1.55;
}

.card-link {
  margin-top: 16px;
}

.process-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.process-strip span {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 132px;
  padding: 16px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.process-strip small {
  color: #a76b18;
  font-weight: 900;
}

.process-strip b {
  color: #2b1d10;
  font-size: 18px;
}

.process-strip em {
  font-style: normal;
}

@media (max-width: 1120px) {
  .participation-brief,
  .requirement-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .brief-stats,
  .requirement-list,
  .process-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .detail-hero {
    min-height: auto;
    padding: 24px;
  }

  .detail-hero h1 {
    font-size: 34px;
  }

  .brief-stats,
  .key-info-list,
  .requirement-list,
  .process-strip {
    grid-template-columns: 1fr;
  }
}
</style>
