<template>
  <div class="home-page">
    <RouterLink v-if="loggedIn && publishedResultCount > 0" class="result-notice" to="/portal/results">
      有 {{ publishedResultCount }} 款酒的结果已发布
      <span>查看我的结果</span>
    </RouterLink>

    <RouterLink v-if="loggedIn && pendingCount > 0" class="my-notice" to="/portal/my">
      你有 {{ pendingCount }} 项参赛事项待处理，进入我的参赛查看
    </RouterLink>

    <section class="site-hero">
      <div v-if="activeCompetition" class="hero-copy">
        <span class="label-chip tone-green">{{ activeCompetition.currentStageLabel }}</span>
        <h1>{{ activeCompetition.name }}</h1>
        <p>{{ activeCompetition.description || '查看报名窗口、投递组别、基础风格和当前应付金额，确认后报名参赛。' }}</p>
        <div class="hero-facts">
          <span>报名截止 {{ formatMonthDayTime(activeCompetition.registrationDeadline) || '-' }}</span>
          <span>{{ entryFeeText(activeCompetition) }}</span>
          <span>送样截止 {{ formatMonthDayTime(activeCompetition.logistics?.sampleArrivalDeadline) || '-' }}</span>
        </div>
        <div class="hero-actions">
          <RouterLink class="primary-action" to="/portal/events">查看开放赛事</RouterLink>
          <a class="secondary-action" href="#entry-flow">了解参赛流程</a>
          <RouterLink
            v-if="canSubmitEntry(activeCompetition)"
            class="text-action"
            :to="`/portal/events/${activeCompetition.id}`"
          >
            查看详情
          </RouterLink>
        </div>
      </div>
      <div v-else class="hero-copy">
        <span class="label-chip tone-amber">赛事报名</span>
        <h1>厂牌参赛入口</h1>
        <p>当前暂无开放展示赛事，可以稍后再回来查看。</p>
      </div>
      <aside class="hero-card">
        <span>{{ activeCompetition ? 'ENTRY INFO' : 'BEER AWARDS' }}</span>
        <strong>{{ activeCompetition ? '赛事信息' : '等待赛事开放' }}</strong>
        <dl>
          <div><dt>报名窗口</dt><dd>{{ registrationWindowText(activeCompetition) }}</dd></div>
          <div><dt>参赛组别</dt><dd>{{ categoryNamesText(activeCompetition) }}</dd></div>
          <div><dt>参赛费用</dt><dd>{{ entryFeeText(activeCompetition) }}</dd></div>
        </dl>
      </aside>
    </section>

    <section class="section-block">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">开放报名赛事</h2>
        </div>
        <RouterLink to="/portal/events">全部赛事</RouterLink>
      </div>
      <div class="event-grid">
        <article v-for="competition in openCompetitions" :key="competition.id" class="event-card brewer-card">
          <span :class="['label-chip', competition.id === activeCompetition?.id ? 'tone-green' : 'tone-amber']">
            {{ competition.id === activeCompetition?.id ? '重点赛事' : competition.currentStageLabel }}
          </span>
          <h3>{{ competition.name }}</h3>
          <p>{{ competition.description || competition.code }}</p>
          <dl>
            <div><dt>报名截止</dt><dd>{{ formatDateTime(competition.registrationDeadline) }}</dd></div>
            <div><dt>当前应付</dt><dd>{{ entryFeeText(competition) }}</dd></div>
            <div v-if="earlyBirdDeadlineText(competition)"><dt>早鸟截止</dt><dd>{{ earlyBirdDeadlineText(competition) }}</dd></div>
          </dl>
          <div class="card-actions">
            <RouterLink :to="`/portal/events/${competition.id}`">查看赛事</RouterLink>
            <RouterLink
              v-if="canSubmitEntry(competition)"
              class="card-primary"
              :to="loggedIn ? `/portal/submit?competitionId=${competition.id}` : '/portal/login'"
            >
              {{ loggedIn ? '报名参赛' : '登录报名' }}
            </RouterLink>
          </div>
        </article>
      </div>
      <div v-if="!openCompetitions.length" class="empty-state">
        <strong>暂无开放报名赛事</strong>
        <p>赛事开放后会在这里显示报名入口。</p>
      </div>
    </section>

    <section class="reason-band">
      <div>
        <span class="label-chip tone-gold">为什么参赛</span>
        <h2>让酒款被专业评审认真品评，也让厂牌得到可复盘的反馈。</h2>
      </div>
      <div class="reason-grid">
        <article v-for="item in reasons" :key="item.title">
          <strong>{{ item.title }}</strong>
          <p>{{ item.text }}</p>
        </article>
      </div>
    </section>

    <section id="entry-flow" class="section-block">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">参赛流程</h2>
          <p>从报名到结果查看，每一步都围绕厂商需要完成的事项设计。</p>
        </div>
      </div>
      <div class="flow-grid">
        <article v-for="step in flowSteps" :key="step.index" class="flow-step">
          <span>{{ step.index }}</span>
          <strong>{{ step.title }}</strong>
          <p>{{ step.text }}</p>
        </article>
      </div>
    </section>

    <section class="guide-grid">
      <article class="brewer-card guide-card">
        <h2 class="portal-section-title">送样与标签</h2>
        <dl>
          <div><dt>参赛编号</dt><dd>报名后生成，用于厂商和主办方核对报名记录。</dd></div>
          <div><dt>现场短编号</dt><dd>展示在标签下方，扫码失败时供现场人工输入。</dd></div>
          <div><dt>标签用途</dt><dd>支付成功后下载并贴在酒瓶或外箱，主办方收样后确认入库。</dd></div>
        </dl>
      </article>
      <article class="brewer-card guide-card">
        <h2 class="portal-section-title">常见问题</h2>
        <div class="faq-list">
          <details v-for="item in faqs" :key="item.question">
            <summary>{{ item.question }}</summary>
            <p>{{ item.answer }}</p>
          </details>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { fetchPortalEntries, fetchPortalHome, fetchPortalResults } from '@/api/portal'
import { canSubmitEntry, formatCompetitionFee, isEarlyBirdActive, isEntryResultPublished } from './portalViewModels'

const loggedIn = computed(() => isLoggedIn('portal'))
const homeData = ref({ activeCompetition: null, openCompetitions: [], competitions: [] })
const entries = ref([])
const results = ref([])
const activeCompetition = computed(() => homeData.value.activeCompetition)
const openCompetitions = computed(() => homeData.value.openCompetitions || [])
const pendingCount = computed(() => entries.value.filter((entry) => entry.status === 'PENDING_PAYMENT' || entry.status === 'REGISTERED').length)
const publishedResultCount = computed(() => results.value.filter((entry) => isEntryResultPublished(entry)).length)

onMounted(async () => {
  homeData.value = await fetchPortalHome()
  if (loggedIn.value) {
    const [entryData, resultData] = await Promise.all([fetchPortalEntries(), fetchPortalResults()])
    entries.value = entryData
    results.value = resultData
  }
})

const reasons = [
  { title: '奖项背书', text: '通过组别奖项和结果发布，为年度代表作积累可传播的赛事证明。' },
  { title: '专业反馈', text: '结果发布后查看评分、桌长评语和评审建议，帮助复盘风格表达。' },
  { title: '厂牌曝光', text: '开放报名赛事面向精酿厂牌和行业观众，适合展示新品和稳定款。' },
]

const flowSteps = [
  { index: '01', title: '选择赛事', text: '查看日期、费用、投递组别和送样要求，确认适合报名的酒款。' },
  { index: '02', title: '报名参赛', text: '填写酒名、组别、基础风格、ABV 和赛事配置的额外字段。' },
  { index: '03', title: '支付报名费', text: '报名后支付报名费，支付成功即完成报名。' },
  { index: '04', title: '下载标签', text: '支付成功后下载现场标签，标签包含参赛编号和现场短编号。' },
  { index: '05', title: '送样入库', text: '按要求寄送或现场交付酒样，主办方收样后更新入库状态。' },
  { index: '06', title: '查看结果', text: '结果发布后查看评分、桌长评语和奖项。' },
]

const faqs = [
  { question: '报名截止后还能报名参赛吗？', answer: '报名截止后不再开放报名入口，如需调整请联系主办方确认。' },
  { question: '付款后多久可以下载标签？', answer: '支付成功后酒款状态变为报名成功，即可下载标签。' },
  { question: '酒样需要寄几瓶？', answer: '不同赛事要求可能不同，请以主办方发布的单场通知为准。' },
  { question: '结果发布后能看到什么？', answer: '可查看评分明细、桌长综合评语和奖项。' },
]

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function formatMonthDayTime(value) {
  if (!value) return ''
  const normalized = String(value).replace('T', ' ')
  const match = normalized.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/)
  if (!match) return formatDateTime(value)
  const [, month, day, hour, minute] = match
  return `${Number(month)}月${Number(day)}日 ${hour}:${minute}`
}

function registrationWindowText(competition) {
  if (!competition) return '待公布'
  const start = formatMonthDayTime(competition.registrationStart)
  const deadline = formatMonthDayTime(competition.registrationDeadline)
  if (start && deadline) return `${start} 开始，${deadline} 截止`
  if (deadline) return `${deadline} 截止`
  return '待公布'
}

function categoryNamesText(competition) {
  const names = competition?.categories?.map((item) => item.name).filter(Boolean) || []
  return names.length ? names.join(' / ') : '待公布'
}

function entryFeeText(competition) {
  return formatCompetitionFee(competition)
}

function earlyBirdDeadlineText(competition) {
  return isEarlyBirdActive(competition) ? formatDateTime(competition.earlyBirdDeadline) : ''
}
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
}

.my-notice {
  display: block;
  padding: 13px 16px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.result-notice {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  color: #fff6df;
  background: #2b1d10;
  border: 1px solid rgba(255, 250, 240, 0.16);
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.result-notice span {
  color: #f3d978;
  white-space: nowrap;
}

.site-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  min-height: 520px;
  padding: 42px;
  color: #fff8e8;
  background:
    linear-gradient(135deg, rgba(31, 21, 14, 0.92), rgba(78, 43, 16, 0.74)),
    url("https://images.unsplash.com/photo-1532634786-c8f8c86a0062?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
  box-shadow: 0 24px 60px rgba(67, 43, 17, 0.14);
}

.hero-copy {
  align-self: end;
  max-width: 860px;
}

.hero-copy h1 {
  margin: 18px 0 14px;
  font-size: 64px;
  line-height: 0.98;
  letter-spacing: 0;
}

.hero-copy p {
  display: -webkit-box;
  max-width: 760px;
  margin: 0;
  overflow: hidden;
  color: #eadabd;
  font-size: 18px;
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
.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.primary-action,
.secondary-action,
.text-action,
.card-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.primary-action,
.card-primary {
  color: #2b1d10;
  background: #e1a23d;
}

.secondary-action,
.text-action {
  color: #fff8e8;
  background: rgba(255, 250, 240, 0.13);
  border: 1px solid rgba(255, 250, 240, 0.26);
}

.hero-card {
  align-self: end;
  padding: 24px;
  color: #2b1d10;
  background: linear-gradient(180deg, rgba(255, 250, 240, 0.98), rgba(246, 217, 150, 0.95));
  border-radius: 8px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.24);
}

.hero-card > span {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.hero-card > strong {
  display: block;
  margin-top: 18px;
  font-size: 25px;
  line-height: 1.2;
}

.section-block,
.reason-band,
.guide-card {
  padding: 26px;
}

.section-block,
.reason-band {
  background: rgba(255, 250, 240, 0.9);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.section-head p {
  margin: 0;
  color: #746a5f;
  line-height: 1.6;
}

.section-head a {
  color: #8b5c19;
  font-weight: 800;
  text-decoration: none;
}

.event-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-top: 20px;
}

.event-card {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(280px, 0.42fr);
  column-gap: 34px;
  row-gap: 14px;
  align-items: start;
  padding: 22px 24px;
}

.event-card .label-chip,
.event-card h3,
.event-card > p,
.event-card .card-actions {
  grid-column: 1;
}

.event-card .label-chip {
  justify-self: start;
}

.event-card h3 {
  margin: 18px 0 8px;
  font-size: 26px;
  line-height: 1.2;
}

.event-card p,
.reason-grid p,
.flow-step p,
.faq-list p,
dt {
  color: #746a5f;
}

.event-card p {
  display: -webkit-box;
  max-width: 680px;
  margin: 0;
  overflow: hidden;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

dl {
  display: grid;
  gap: 12px;
  margin: 20px 0 0;
}

.event-card dl {
  grid-column: 2;
  grid-row: 1 / span 4;
  grid-template-columns: 1fr;
  align-self: center;
  gap: 13px;
  margin: 0;
}

dl div {
  padding-bottom: 11px;
  border-bottom: 1px dashed rgba(87, 58, 26, 0.18);
}

dt,
dd {
  margin: 0;
}

dd {
  margin-top: 5px;
  font-weight: 800;
  line-height: 1.45;
}

.card-actions a {
  color: #6b4710;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.reason-band {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: 24px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(35, 24, 16, 0.96), rgba(91, 49, 18, 0.9)),
    url("https://images.unsplash.com/photo-1566633806327-68e152aaf26d?auto=format&fit=crop&w=1600&q=80");
  background-position: center;
  background-size: cover;
}

.reason-band h2 {
  margin: 18px 0 0;
  font-size: 36px;
  line-height: 1.12;
}

.reason-grid {
  display: grid;
  gap: 12px;
}

.reason-grid article {
  padding: 18px;
  background: rgba(255, 250, 240, 0.1);
  border: 1px solid rgba(255, 250, 240, 0.18);
  border-radius: 8px;
}

.reason-grid p {
  margin-bottom: 0;
  color: #e6d6b8;
  line-height: 1.65;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.flow-step {
  min-height: 170px;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.flow-step span {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 50%;
  font-weight: 900;
}

.flow-step strong {
  display: block;
  margin-top: 18px;
  font-size: 18px;
}

.flow-step p {
  line-height: 1.65;
}

.guide-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 18px;
}

.faq-list {
  display: grid;
  gap: 10px;
}

details {
  padding: 14px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

summary {
  cursor: pointer;
  font-weight: 800;
}

details p {
  margin-bottom: 0;
  line-height: 1.65;
}

@media (max-width: 1120px) {
  .site-hero,
  .reason-band,
  .guide-grid {
    grid-template-columns: 1fr;
  }

  .flow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .event-card {
    grid-template-columns: 1fr;
  }

  .event-card dl {
    grid-column: 1;
    grid-row: auto;
  }
}

@media (max-width: 720px) {
  .site-hero {
    min-height: auto;
    padding: 28px;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .section-head,
  .flow-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .event-card dl {
    grid-template-columns: 1fr;
  }
}
</style>
