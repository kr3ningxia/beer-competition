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
        <p>{{ activeCompetition.description || '查看报名窗口、投递组别、基础风格和当前应付金额，确认后报名参赛' }}</p>
        <div class="hero-facts">
          <span>报名截止 {{ formatMonthDayTime(activeCompetition.registrationDeadline) || '-' }}</span>
          <span>{{ entryFeeText(activeCompetition) }}</span>
          <span>送样截止 {{ formatMonthDayTime(activeCompetition.logistics?.sampleArrivalDeadline) || '-' }}</span>
        </div>
        <div class="hero-actions">
          <RouterLink class="primary-action" :to="heroPrimaryAction.to">{{ heroPrimaryAction.label }}</RouterLink>
          <RouterLink
            class="secondary-action"
            :to="`/portal/events/${activeCompetition.id}`"
          >
            查看赛事详情
          </RouterLink>
          <a class="text-action" href="#entry-flow">参赛流程</a>
        </div>
      </div>
      <div v-else class="hero-copy">
        <span class="label-chip tone-amber">赛事报名</span>
        <h1>厂牌参赛入口</h1>
        <p>当前暂无开放展示赛事，请稍后再回来查看</p>
      </div>
      <aside class="hero-card">
        <span>{{ activeCompetition ? 'ENTRY INFO' : 'BEER AWARDS' }}</span>
        <strong>{{ activeCompetition ? '赛事信息' : '等待赛事开放' }}</strong>
        <dl>
          <div><dt>报名窗口</dt><dd>{{ registrationWindowText(activeCompetition) }}</dd></div>
          <div>
            <dt>参赛组别</dt>
            <dd class="category-summary-row">
              <span class="category-summary">{{ categorySummaryText(activeCompetition) }}</span>
              <button
                v-if="activeCategoryNames.length > summaryCategoryLimit"
                class="category-expand"
                type="button"
                @click="categoryDialogOpen = true"
              >
                查看全部
              </button>
            </dd>
          </div>
          <div><dt>参赛费用</dt><dd>{{ entryFeeText(activeCompetition) }}</dd></div>
        </dl>
      </aside>
    </section>

    <Teleport to="body">
      <div v-if="categoryDialogOpen" class="category-modal-backdrop" @click.self="categoryDialogOpen = false">
        <section class="category-modal" role="dialog" aria-modal="true" aria-labelledby="category-modal-title">
          <header class="category-modal-head">
            <div>
              <span>参赛组别</span>
              <h2 id="category-modal-title">全部参赛组别</h2>
            </div>
            <button class="category-modal-close" type="button" aria-label="关闭" @click="categoryDialogOpen = false">
              ×
            </button>
          </header>
          <div class="category-modal-meta">
            <strong>{{ activeCategoryNames.length }} 个组别</strong>
            <span>{{ activeCompetition?.name || '赛事信息' }}</span>
          </div>
          <ul class="category-modal-list">
            <li v-for="name in activeCategoryNames" :key="name">{{ name }}</li>
          </ul>
        </section>
      </div>
    </Teleport>

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
        <p>赛事开放后会在这里显示报名入口</p>
      </div>
    </section>

    <section class="reason-band">
      <div class="reason-lead">
        <span class="label-chip tone-gold">为什么参赛</span>
        <h2>让你的优秀酒款，<br />被更多人知道、喝到</h2>
      </div>
      <div class="reason-grid">
        <article v-for="(item, index) in reasons" :key="item.title">
          <div class="reason-title-row">
            <span class="reason-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <strong>{{ item.title }}</strong>
          </div>
          <p>{{ item.text }}</p>
        </article>
      </div>
    </section>

    <section id="entry-flow" class="section-block">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">参赛流程</h2>
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
          <div><dt>参赛编号</dt><dd>报名后生成，用于厂牌和组委会核对报名记录</dd></div>
          <div><dt>现场短编号</dt><dd>展示在标签下方，扫码失败时供现场人工输入</dd></div>
          <div><dt>标签用途</dt><dd>支付成功后下载并贴在酒瓶或外箱，组委会收样后确认入库</dd></div>
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
import { canSubmitEntry, formatCompetitionFee, isEarlyBirdActive, isEntryResultPublished, isEntryVendorActionPending } from './portalViewModels'

const loggedIn = computed(() => isLoggedIn('portal'))
const homeData = ref({ activeCompetition: null, openCompetitions: [], competitions: [] })
const entries = ref([])
const results = ref([])
const categoryDialogOpen = ref(false)
const summaryCategoryLimit = 3
const activeCompetition = computed(() => homeData.value.activeCompetition)
const openCompetitions = computed(() => homeData.value.openCompetitions || [])
const activeCategoryNames = computed(() => categoryNames(activeCompetition.value))
const pendingCount = computed(() => entries.value.filter((entry) => isEntryVendorActionPending(entry)).length)
const publishedResultCount = computed(() => results.value.filter((entry) => isEntryResultPublished(entry)).length)
const heroPrimaryAction = computed(() => {
  if (!activeCompetition.value || !canSubmitEntry(activeCompetition.value)) {
    return { label: '查看全部赛事', to: '/portal/events' }
  }
  return loggedIn.value
    ? { label: '报名参赛', to: `/portal/submit?competitionId=${activeCompetition.value.id}` }
    : { label: '登录后报名', to: '/portal/login' }
})

onMounted(async () => {
  homeData.value = await fetchPortalHome()
  if (loggedIn.value) {
    const [entryData, resultData] = await Promise.all([fetchPortalEntries(), fetchPortalResults()])
    entries.value = entryData
    results.value = resultData
  }
})

const reasons = [
  { title: '荣誉背书', text: '获得实体版和电子版奖状及奖牌，可用于瓶贴、海报、电商详情页、酒吧物料及品牌宣传' },
  { title: '媒体曝光', text: '获得啤酒事务局及媒体合作伙伴的报道，包括品牌故事、酒款风味、工艺亮点、评审反馈等' },
  { title: '专业“体检”', text: '无论是否获奖，所有参赛作品都将获得由专业啤酒评审和媒体/跨界评审撰写的反馈' },
  { title: '超赞奖品', text: '获得由赞助商提供的超赞奖品（具体奖品见赛事详情页）' },
]

const flowSteps = [
  { index: '01', title: '选择赛事', text: '查看正在报名中的赛事，确认自己是否有合适投递的酒款' },
  { index: '02', title: '报名参赛', text: '填写酒款信息、投报组别等' },
  { index: '03', title: '支付费用', text: '根据投递的酒款数量，支付报名费' },
  { index: '04', title: '下载标签', text: '支付成功后下载酒标，并按要求贴在样品上' },
  { index: '05', title: '送样入库', text: '在赛事开放的送样时间段内将酒款送达指定地点' },
  { index: '06', title: '查看结果', text: '查看比赛结果、裁判评语以及是否获奖' },
]

const faqs = [
  { question: '报名截止后还能报名参赛吗？', answer: '报名截止后不再开放报名入口，如需调整请联系组委会确认' },
  { question: '支付后多久开放标签下载？', answer: '支付成功后酒款状态变为报名成功，标签下载同步开放' },
  { question: '样品需要寄几瓶？', answer: '不同赛事要求可能不同，请以组委会发布的单场通知为准' },
  { question: '结果发布后能看到什么？', answer: '结果页展示评分明细、桌长综合评语和奖项' },
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

function categoryNames(competition) {
  return competition?.categories?.map((item) => item.name).filter(Boolean) || []
}

function categoryNamesText(competition) {
  const names = categoryNames(competition)
  return names.length ? names.join(' / ') : '待公布'
}

function categorySummaryText(competition) {
  const names = categoryNames(competition)
  if (!names.length) return '待公布'
  if (names.length <= summaryCategoryLimit) return names.join('、')
  return `${names.slice(0, summaryCategoryLimit).join('、')} 等 ${names.length} 个组别`
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

.category-summary-row {
  display: grid;
  gap: 7px;
}

.category-summary {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.category-expand {
  justify-self: start;
  padding: 0;
  color: #8b5c19;
  background: transparent;
  border: 0;
  font: inherit;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.4;
  cursor: pointer;
}

.category-expand:hover,
.category-expand:focus-visible {
  color: #2b1d10;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.category-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(31, 21, 14, 0.58);
}

.category-modal {
  width: min(560px, 100%);
  max-height: min(680px, calc(100vh - 48px));
  overflow: auto;
  padding: 24px;
  color: #2b1d10;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 8px;
  box-shadow: 0 28px 80px rgba(31, 21, 14, 0.32);
}

.category-modal-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.category-modal-head span {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.category-modal-head h2 {
  margin: 8px 0 0;
  font-size: 26px;
  line-height: 1.2;
}

.category-modal-close {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  color: #6b4710;
  background: #fff3d8;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 50%;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.category-modal-meta {
  display: grid;
  gap: 4px;
  margin-top: 18px;
  padding: 14px 0;
  border-top: 1px dashed rgba(87, 58, 26, 0.18);
  border-bottom: 1px dashed rgba(87, 58, 26, 0.18);
}

.category-modal-meta strong {
  font-size: 18px;
}

.category-modal-meta span {
  color: #746a5f;
  line-height: 1.5;
}

.category-modal-list {
  display: grid;
  gap: 10px;
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
}

.category-modal-list li {
  padding: 12px 14px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
  font-weight: 800;
  line-height: 1.45;
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
  grid-template-columns: minmax(360px, 0.74fr) minmax(0, 1.26fr);
  gap: 40px;
  align-items: stretch;
  min-height: 372px;
  padding: 34px 28px;
  color: #fff6df;
  background:
    linear-gradient(90deg, rgba(31, 20, 13, 0.98) 0%, rgba(38, 23, 14, 0.96) 40%, rgba(74, 40, 18, 0.9) 100%),
    url("https://images.unsplash.com/photo-1566633806327-68e152aaf26d?auto=format&fit=crop&w=1600&q=80");
  background-position: center;
  background-size: cover;
}

.reason-lead {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  min-width: 0;
}

.reason-lead .label-chip {
  width: fit-content;
}

.reason-band h2 {
  margin: 20px 0 0;
  max-width: 500px;
  font-size: 36px;
  line-height: 1.24;
}

.reason-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
  align-content: center;
}

.reason-grid article {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 12px;
  min-height: 142px;
  padding: 18px 19px 19px;
  background: rgba(255, 250, 240, 0.1);
  border: 1px solid rgba(255, 250, 240, 0.2);
  border-radius: 8px;
  box-shadow:
    inset 0 1px 0 rgba(255, 250, 240, 0.08),
    0 12px 28px rgba(12, 8, 5, 0.14);
}

.reason-title-row {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}

.reason-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 30px;
  height: 20px;
  color: #2b1d10;
  background: rgba(243, 217, 120, 0.92);
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  line-height: 20px;
  letter-spacing: 0;
}

.reason-grid strong {
  min-width: 0;
  color: #fff9e8;
  font-size: 18px;
  line-height: 1.3;
}

.reason-grid p {
  margin: 0;
  color: #e6d6b8;
  font-size: 15px;
  line-height: 1.7;
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

  .reason-band {
    min-height: auto;
  }

  .reason-band h2 {
    max-width: 760px;
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

  .category-modal-backdrop {
    place-items: end center;
    padding: 18px;
  }

  .category-modal {
    max-height: min(78vh, 620px);
  }

  .section-head,
  .flow-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .event-card dl {
    grid-template-columns: 1fr;
  }

  .reason-band {
    padding: 26px 20px;
  }

  .reason-band h2 {
    font-size: 30px;
  }

  .reason-grid {
    grid-template-columns: 1fr;
  }
}
</style>
