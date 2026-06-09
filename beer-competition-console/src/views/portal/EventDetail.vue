<template>
  <div class="event-detail-page">
    <RouterLink class="back-link" to="/portal/events">&lt; 返回</RouterLink>

    <section class="detail-hero">
      <div class="hero-copy">
        <span :class="['label-chip', stageTone(competition.status)]">
          {{ stageLabel }}
        </span>
        <h1>{{ competition.name || '赛事详情' }}</h1>
        <p>{{ competition.code || '-' }} · {{ competition.edition || '-' }}</p>
        <div class="hero-facts" aria-label="赛事关键时间和费用">
          <span>
            <small>比赛日期</small>
            <b>{{ formatDate(competition.competitionDate) }}</b>
          </span>
          <span>
            <small>报名截止</small>
            <b>{{ formatDateTime(competition.registrationDeadline) }}</b>
          </span>
          <span>
            <small>送样截止</small>
            <b>{{ formatDateTime(logistics.sampleArrivalDeadline) }}</b>
          </span>
          <span>
            <small>报名费</small>
            <b>{{ feeText }}</b>
          </span>
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
          <h2 class="portal-section-title">参赛要求</h2>
          <p>判断这场赛事是否适合你的酒款，报名时按这些配置填写。</p>
        </div>
      </div>

      <div class="requirement-grid">
        <article class="requirement-block">
          <span>投递组别</span>
          <strong>{{ categorySummary }}</strong>
          <div class="chip-list">
            <em v-for="category in competition.categories" :key="category.id || category.name">{{ category.name }}</em>
          </div>
        </article>

        <article class="requirement-block">
          <span>基础风格</span>
          <strong>{{ styleSummary }}</strong>
          <p>{{ styleCategorySummary }}</p>
          <details v-if="competition.styles?.length" class="style-details">
            <summary>查看全部基础风格</summary>
            <div class="style-groups">
              <section v-for="group in styleGroups" :key="group.name" class="style-group">
                <h3>{{ group.name }}</h3>
                <p>{{ group.items.map((item) => item.name).join(' / ') }}</p>
              </section>
            </div>
          </details>
        </article>

        <article class="requirement-block">
          <span>额外字段</span>
          <strong>{{ entryFieldHeadline }}</strong>
          <div v-if="entryFields.length" class="field-list">
            <span v-for="field in entryFields" :key="field.fieldKey || field.fieldLabel">
              <b>{{ field.fieldLabel }}</b>
              <small>{{ field.required ? '必填' : '选填' }} · {{ field.visibleToJudges ? '评审可见' : '仅主办方可见' }}</small>
            </span>
          </div>
          <p v-else>本场没有配置补充字段。</p>
        </article>
      </div>
    </section>

    <section class="content-grid">
      <article class="brewer-card info-panel">
        <h2 class="portal-section-title">送样与标签</h2>
        <dl>
          <div><dt>送样方式</dt><dd>{{ deliveryMethodText(logistics.deliveryMethod) }}</dd></div>
          <div><dt>建议送达</dt><dd>{{ arrivalWindowText }}</dd></div>
          <div><dt>酒样要求</dt><dd>{{ logistics.sampleQuantityNote || '以主办方后续通知为准' }}</dd></div>
          <div><dt>收件信息</dt><dd>{{ deliveryAddressText }}</dd></div>
          <div v-if="logistics.deliveryNote"><dt>包装说明</dt><dd>{{ logistics.deliveryNote }}</dd></div>
        </dl>
      </article>

      <article class="brewer-card info-panel">
        <h2 class="portal-section-title">结果与奖项</h2>
        <dl>
          <div><dt>结果状态</dt><dd>{{ resultStatusText }}</dd></div>
          <div><dt>可查看内容</dt><dd>评分、评语、奖项和证书。</dd></div>
          <div><dt>查看入口</dt><dd>{{ resultEntryText }}</dd></div>
        </dl>
        <RouterLink
          v-if="resultCardAction"
          class="card-link"
          :to="resultCardAction.to"
        >
          {{ resultCardAction.label }}
        </RouterLink>
      </article>
    </section>

    <section class="detail-section brewer-card">
      <div class="section-heading">
        <div>
          <h2 class="portal-section-title">参赛流程</h2>
          <p>从提交资料到结果发布，厂商端主要关注这些节点。</p>
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
          <RouterLink v-if="eventSummary.result > 0" :to="competitionResultPath(competition.id)">查看我的结果</RouterLink>
        </div>
      </template>

      <template v-else-if="loggedIn">
        <div>
          <span class="label-chip tone-amber">尚未参加本赛事</span>
          <h2>确认规则后再提交酒款</h2>
          <p>先核对组别、基础风格、费用和送样要求。</p>
        </div>
        <div class="brief-actions">
          <RouterLink v-if="canSubmitEntry(competition)" :to="`/portal/submit?competitionId=${competition.id}`">提交酒款</RouterLink>
          <RouterLink to="/portal/my">查看我的参赛</RouterLink>
        </div>
      </template>

      <template v-else>
        <div>
          <span class="label-chip tone-amber">公开赛事详情</span>
          <h2>登录后提交和追踪参赛酒款</h2>
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
  isCompetitionResultPublished,
} from './portalViewModels'

const route = useRoute()
const loggedIn = computed(() => isLoggedIn('portal'))
const competition = ref({ categories: [], styles: [], entryFields: [] })
const entries = ref([])

const logistics = computed(() => competition.value.logistics || {})
const entryFields = computed(() => competition.value.entryFields || [])
const eventEntries = computed(() => entries.value.filter((entry) => entry.competitionId === competition.value.id))
const hasEventEntries = computed(() => eventEntries.value.length > 0)
const eventSummary = computed(() => entrySummaryForCompetition(competition.value.id, entries.value))
const feeText = computed(() => competition.value.entryFee === undefined || competition.value.entryFee === null ? '-' : `¥${competition.value.entryFee} / 款`)
const stageLabel = computed(() => (isCompetitionResultPublished(competition.value) ? '结果已发布' : competition.value.currentStageLabel) || '赛事详情')
const categorySummary = computed(() => {
  const count = competition.value.categories?.length || 0
  return count ? `${count} 个投递组别` : '暂未配置'
})
const styleSummary = computed(() => {
  const count = competition.value.styles?.length || 0
  return count ? `${count} 个可选基础风格` : '暂未配置'
})
const styleCategorySummary = computed(() => {
  const count = styleGroups.value.length
  if (!competition.value.styles?.length) return '主办方暂未配置基础风格库。'
  return count ? `按 ${count} 个风格大类组织，报名时单选一个基础风格。` : '报名时单选一个基础风格。'
})
const entryFieldHeadline = computed(() => {
  const count = entryFields.value.length
  if (!count) return '无补充字段'
  const judgeVisibleCount = entryFields.value.filter((field) => field.visibleToJudges).length
  return `${count} 个补充字段，${judgeVisibleCount} 个评审可见`
})
const styleGroups = computed(() => {
  const groups = new Map()
  ;(competition.value.styles || []).forEach((style) => {
    const key = style.categoryName || '未分组'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(style)
  })
  return Array.from(groups.entries()).map(([name, items]) => ({ name, items }))
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
      label: loggedIn.value ? '提交酒款' : '登录后报名',
      to: loggedIn.value ? `/portal/submit?competitionId=${competition.value.id}` : '/portal/login',
    }
  }
  return null
})
const resultCardAction = computed(() => {
  if (!isCompetitionResultPublished(competition.value)) {
    return null
  }
  if (!loggedIn.value) {
    return { label: '登录查看结果', to: '/portal/login' }
  }
  if (!hasEventEntries.value) {
    return null
  }
  return { label: '查看我的结果', to: competitionResultPath(competition.value.id) }
})
const resultStatusText = computed(() => (isCompetitionResultPublished(competition.value) ? '结果已发布' : '结果待发布'))
const resultEntryText = computed(() => {
  if (!loggedIn.value) return '登录后查看自己的评分、评语和奖项。'
  if (!hasEventEntries.value) return '本账号尚未参加该赛事。'
  return isCompetitionResultPublished(competition.value) ? '进入“我的结果”查看本场反馈。' : '主办方发布后可在“我的结果”查看。'
})
const arrivalWindowText = computed(() => {
  const start = formatDateTime(logistics.value.sampleArrivalStart)
  const deadline = formatDateTime(logistics.value.sampleArrivalDeadline)
  if (start !== '-' && deadline !== '-') return `${start} 至 ${deadline}`
  if (deadline !== '-') return `${deadline} 前送达`
  return '以主办方通知为准'
})
const deliveryAddressText = computed(() => {
  if (logistics.value.deliveryAddress) {
    return [
      logistics.value.deliveryRecipient,
      logistics.value.deliveryPhone,
      logistics.value.deliveryAddress,
    ].filter(Boolean).join(' · ')
  }
  if (logistics.value.logisticsVisibility === 'LOGIN_REQUIRED') {
    return '登录后在付款与寄样页查看完整收件信息'
  }
  if (logistics.value.logisticsVisibility === 'PUBLIC') {
    return '主办方暂未填写完整地址'
  }
  return '支付成功后显示完整收件信息'
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
  max-width: 760px;
  margin: 0;
  color: #ead9b7;
  font-size: 16px;
  line-height: 1.7;
}

.hero-facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  max-width: 980px;
  margin-top: 24px;
}

.hero-facts span {
  min-height: 82px;
  padding: 14px 16px;
  color: #2b1d10;
  background: linear-gradient(180deg, rgba(255, 250, 240, 0.98), rgba(246, 217, 150, 0.94));
  border: 1px solid rgba(255, 250, 240, 0.22);
  border-radius: 8px;
}

.hero-facts small,
.hero-facts b {
  display: block;
}

.hero-facts small {
  color: #7d705f;
}

.hero-facts b {
  margin-top: 8px;
  overflow-wrap: anywhere;
  font-size: 18px;
  line-height: 1.28;
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
.field-list small {
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

.requirement-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.3fr 1fr;
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

.field-list {
  display: grid;
  gap: 8px;
}

.field-list span {
  display: grid;
  gap: 3px;
  padding: 10px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.08);
  border-radius: 8px;
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

  .hero-facts,
  .brief-stats,
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

  .hero-facts,
  .brief-stats,
  .process-strip {
    grid-template-columns: 1fr;
  }
}
</style>
