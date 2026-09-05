<template>
  <div ref="pageRoot" class="event-detail-page" :style="{ '--header-offset': headerHeight + 'px', '--section-offset': (headerHeight + navHeight + 16) + 'px' }">
    <RouterLink class="back-link" to="/portal/events"><el-icon><ArrowLeft /></el-icon> 返回赛事列表</RouterLink>
    <p v-if="loading" class="page-state" role="status">正在加载赛事…</p>
    <div v-else-if="loadError" class="page-state" role="alert">
      <p>{{ loadError }}</p><button class="text-link" @click="loadCompetition">重新加载</button>
    </div>
    <template v-else>
      <header class="detail-hero">
        <div class="hero-meta">
          <span :class="['label-chip', stageTone(competition.status)]">{{ stageLabel }}</span>
          <div v-if="competition.organizerType === 'TENANT'" class="event-source">
            <span class="source-label">第三方赛事</span>
            <span v-if="competition.organizerName" class="source-name">发起方：{{ competition.organizerName }}</span>
          </div>
          <span v-else-if="competition.organizerType === 'PLATFORM' && competition.organizerName" class="platform-organizer">主办方：{{ competition.organizerName }}</span>
        </div>
        <h1>{{ competition.name }}</h1>
        <div class="hero-actions">
          <RouterLink v-if="primaryAction" class="primary-action" :to="primaryAction.to">{{ primaryAction.label }}</RouterLink>
          <RouterLink v-if="hasEventEntries && canSubmitEntry(competition)" class="secondary-action" :to="submitPath">继续报名</RouterLink>
          <a v-if="competition.rulesUrl" class="secondary-action" :href="competition.rulesUrl" target="_blank" rel="noopener noreferrer">查看参赛细则</a>
        </div>
      </header>
      <dl class="date-summary">
        <div><dt>报名截止</dt><dd>{{ formatDateTime(competition.registrationDeadline) }}</dd></div>
        <div><dt>送样截止</dt><dd>{{ formatDateTime(logistics.sampleArrivalDeadline) }}</dd></div>
        <div><dt>比赛日期</dt><dd>{{ competition.competitionDate || '待公布' }}</dd></div>
      </dl>
      <section v-if="hasEventEntries || entriesError" class="participation">
        <h2>本场参赛</h2>
        <div v-if="!entriesError" class="participation-counts">
          <span v-for="item in participationCounts" :key="item.label">{{ item.label }} <b>{{ item.count }}</b> 款</span>
          <span v-if="!participationCounts.length">已有酒款记录</span>
        </div>
        <span v-else role="status">参赛记录暂时无法加载</span>
        <RouterLink class="text-link" :to="entriesPath">查看本场酒款 <el-icon><ArrowRight /></el-icon></RouterLink>
      </section>
      <nav ref="sectionNav" class="section-nav" aria-label="赛事详情分区">
        <div class="nav-links">
          <a v-for="section in navigationSections" :key="section.id" :href="'#' + section.id" :class="{ active: activeSection === section.id }" :aria-current="activeSection === section.id ? 'location' : undefined" @click="activeSection = section.id">{{ section.label }}</a>
        </div>
        <RouterLink v-if="primaryAction" class="primary-action compact" :to="primaryAction.to">{{ primaryAction.label }}</RouterLink>
      </nav>
      <section v-if="competition.description" id="event-description" class="detail-section">
        <h2 class="portal-section-title">赛事简介</h2>
        <p class="long-copy">{{ competition.description }}</p>
      </section>
      <section id="event-requirements" class="detail-section">
        <div class="category-heading">
          <h2 class="portal-section-title">投递组别</h2>
          <span>{{ competition.categories?.length || 0 }} 个组别</span>
        </div>
          <div class="category-main">
            <ul class="category-list">
              <li v-for="category in visibleCategories" :key="category.id">{{ category.name }}</li>
            </ul>
            <span v-if="!competition.categories?.length" class="muted">暂未配置</span>
            <button v-if="competition.categories?.length > 9" class="text-link expand-link" :aria-expanded="categoriesExpanded" @click="categoriesExpanded = !categoriesExpanded">
              {{ categoriesExpanded ? '收起组别' : '查看全部组别（共 ' + competition.categories.length + ' 个）' }}
            </button>
          </div>
        <div class="requirements-meta">
        <div class="field-row">
          <h3>基础风格</h3>
          <div class="inline-content"><span>{{ competition.styles?.length || 0 }} 种</span><button v-if="competition.styles?.length" class="text-link" @click="stylesOpen = true">查看基础风格 <el-icon><ArrowRight /></el-icon></button></div>
        </div>
        <div class="field-row">
          <h3>报名资料</h3>
          <div><p class="field-copy">酒款名称、投递组别、基础风格、ABV</p><p v-if="requiredFields.length" class="field-copy extra-fields">其他必填：{{ requiredFields.map(field => field.fieldLabel).join('、') }}</p></div>
        </div>
        </div>
      </section>
      <section id="event-fees" class="detail-section">
        <h2 class="portal-section-title">报名费用</h2>
        <div class="fee-summary">
          <strong>{{ feeText }}</strong>
          <span v-if="earlyBirdActive">早鸟截止 {{ formatDateTime(competition.earlyBirdDeadline) }}</span>
          <span v-if="earlyBirdActive">普通报名费 {{ formatMoney(competition.entryFee) }} / 款</span>
        </div>
        <table v-if="tierRows.length" class="fee-table">
          <caption class="sr-only">阶梯报名价格</caption>
          <colgroup><col class="quantity-column" /><col class="discount-column" /><col class="price-column" /></colgroup>
          <thead><tr><th scope="col">累计酒款序号</th><th scope="col">折扣</th><th scope="col">单价</th></tr></thead>
          <tbody><tr v-for="row in tierRows" :key="row.key"><td>{{ row.label }}</td><td>{{ row.rateText || '原价' }}</td><td><strong>{{ tierMoneyFormatter.format(row.amount) }}</strong><span class="price-unit"> / 款</span></td></tr></tbody>
        </table>
      </section>
      <section id="event-delivery" class="detail-section">
        <h2 class="portal-section-title">送样要求</h2>
        <dl class="delivery-summary">
          <div><dt>送样方式</dt><dd>{{ deliveryMethodText }}</dd></div>
          <div><dt>送达时间</dt><dd>{{ arrivalWindowText }}</dd></div>
        </dl>
        <div v-if="logistics.sampleQuantityNote" class="field-row"><h3>样品要求</h3><p class="long-copy">{{ logistics.sampleQuantityNote }}</p></div>
        <div v-if="logistics.deliveryAddress" class="field-row"><h3>送样地址</h3><p class="long-copy">{{ logistics.deliveryAddress }}</p></div>
        <div v-if="logistics.deliveryRecipient || logistics.deliveryPhone" class="field-row"><h3>收件信息</h3><p class="long-copy">{{ [logistics.deliveryRecipient, logistics.deliveryPhone].filter(Boolean).join(' · ') }}</p></div>
        <div v-if="logistics.deliveryNote" class="field-row"><h3>包装说明</h3><p class="long-copy">{{ logistics.deliveryNote }}</p></div>
      </section>
      <section class="detail-section process-section">
        <h2 class="portal-section-title">参赛流程</h2>
        <ol class="process-list"><li v-for="(step, index) in processSteps" :key="step"><span>{{ String(index + 1).padStart(2, '0') }}</span>{{ step }}</li></ol>
      </section>
      <el-drawer v-model="stylesOpen" title="基础风格" size="min(560px, 100vw)">
        <el-input v-model="styleSearch" placeholder="搜索风格名称或代码" clearable :prefix-icon="Search" aria-label="搜索基础风格" />
        <ul class="style-list"><li v-for="style in filteredStyles" :key="style.id || style.name"><h3>{{ [style.styleCode, style.name].filter(Boolean).join(' ') }}</h3><p v-if="style.description">{{ style.description }}</p></li></ul>
        <p v-if="!filteredStyles.length" role="status">未找到匹配的风格</p>
      </el-drawer>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onBeforeUnmount, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, Search } from '@element-plus/icons-vue'
import { isLoggedIn } from '@/utils/auth'
import { fetchPortalCompetitionDetail, fetchPortalEntries } from '@/api/portal'
import {
  buildTierLadder, canSubmitEntry, competitionResultPath, entrySummaryForCompetition,
  formatCompetitionFee, formatMoney, isCompetitionResultPublished, isEarlyBirdActive,
  isEntryPaymentPending, normalizeTiers,
} from './portalViewModels'

const route = useRoute()
const competition = ref({})
const entries = ref([])
const loading = ref(true)
const loadError = ref('')
const entriesError = ref(false)
const categoriesExpanded = ref(false)
const stylesOpen = ref(false)
const styleSearch = ref('')
const pageRoot = ref(null)
const sectionNav = ref(null)
const headerHeight = ref(0)
const navHeight = ref(64)
const activeSection = ref('event-description')
const navigationSections = computed(() => [
  ...(competition.value.description ? [{ id: 'event-description', label: '赛事简介' }] : []),
  { id: 'event-requirements', label: '投递组别' },
  { id: 'event-fees', label: '报名费用' },
  { id: 'event-delivery', label: '送样要求' },
])
let layoutObserver
let scrollFrame = 0
function updateActiveSection() {
  scrollFrame = 0
  const threshold = headerHeight.value + navHeight.value + 24
  let current = navigationSections.value[0]?.id
  for (const section of navigationSections.value) {
    const element = pageRoot.value?.querySelector('#' + section.id)
    if (element && element.getBoundingClientRect().top <= threshold) current = section.id
  }
  activeSection.value = current
}
function handleScroll() {
  if (!scrollFrame) scrollFrame = requestAnimationFrame(updateActiveSection)
}
async function observeLayout() {
  await nextTick()
  layoutObserver?.disconnect()
  const header = pageRoot.value?.closest('.portal-layout')?.querySelector('.portal-header') || document.querySelector('.portal-header')
  layoutObserver = new ResizeObserver(() => {
    headerHeight.value = header?.getBoundingClientRect().height || 0
    navHeight.value = sectionNav.value?.getBoundingClientRect().height || 64
    handleScroll()
  })
  for (const element of [header, sectionNav.value, pageRoot.value]) {
    if (element) layoutObserver.observe(element)
  }
}
const loggedIn = computed(() => isLoggedIn('portal'))
const logistics = computed(() => competition.value.logistics || {})
const eventEntries = computed(() => entries.value.filter(entry => entry.competitionId === competition.value.id))
const hasEventEntries = computed(() => eventEntries.value.length > 0)
const entriesPath = computed(() => '/portal/entries?competitionId=' + competition.value.id)
const submitPath = computed(() => '/portal/submit?competitionId=' + competition.value.id)
const stageLabel = computed(() => isCompetitionResultPublished(competition.value) ? '结果已发布' : competition.value.currentStageLabel)
const visibleCategories = computed(() => categoriesExpanded.value ? competition.value.categories : competition.value.categories?.slice(0, 9))
const requiredFields = computed(() => (competition.value.entryFields || []).filter(field => field.required))
const filteredStyles = computed(() => {
  const query = styleSearch.value.trim().toLowerCase()
  return (competition.value.styles || []).filter(style => [style.name, style.styleCode].filter(Boolean).join(' ').toLowerCase().includes(query))
})
const feeText = computed(() => formatCompetitionFee(competition.value))
const tierMoneyFormatter = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', minimumFractionDigits: 2, maximumFractionDigits: 2 })
const earlyBirdActive = computed(() => isEarlyBirdActive(competition.value))
const tierRows = computed(() => {
  const tiers = normalizeTiers(competition.value)
  const ladder = buildTierLadder(competition.value)
  return ladder.segments.map(segment => {
    if (segment.isBase) {
      const end = Number(tiers[0].startQuantity) - 1
      return { ...segment, label: end === 1 ? '第 1 款' : '第 1–' + end + ' 款' }
    }
    const index = tiers.findIndex(tier => 'tier-' + tier.startQuantity === segment.key)
    const start = Number(tiers[index].startQuantity)
    const end = tiers[index + 1] ? Number(tiers[index + 1].startQuantity) - 1 : null
    return { ...segment, label: end === null ? '第 ' + start + ' 款起' : end === start ? '第 ' + start + ' 款' : '第 ' + start + '–' + end + ' 款' }
  })
})
const summary = computed(() => entrySummaryForCompetition(competition.value.id, entries.value))
const participationCounts = computed(() => {
  const pending = eventEntries.value.filter(isEntryPaymentPending)
  return [
    { label: '待支付', count: pending.filter(entry => entry.paymentStatus !== 'PENDING_CONFIRM').length },
    { label: '转账待确认', count: pending.filter(entry => entry.paymentStatus === 'PENDING_CONFIRM').length },
    { label: '待办理送样', count: summary.value.deliveryActionPending },
    { label: '待确认入库', count: summary.value.deliverySubmitted },
    { label: '已入库', count: summary.value.stored },
    { label: '结果可查', count: summary.value.result },
  ].filter(item => item.count > 0)
})
const primaryAction = computed(() => {
  if (summary.value.result > 0) return { label: '查看我的结果', to: competitionResultPath(competition.value.id) }
  if (hasEventEntries.value) return { label: '查看本场酒款', to: entriesPath.value }
  if (isCompetitionResultPublished(competition.value)) return { label: '查看赛事结果', to: '/portal/competition-results/' + competition.value.id }
  if (canSubmitEntry(competition.value)) return { label: loggedIn.value ? '报名参赛' : '登录后报名', to: loggedIn.value ? submitPath.value : '/portal/login' }
  return null
})
const arrivalWindowText = computed(() => {
  const { sampleArrivalStart: start, sampleArrivalDeadline: end } = logistics.value
  if (start && end) return formatDateTime(start) + ' 至 ' + formatDateTime(end)
  if (end) return formatDateTime(end) + ' 前送达'
  if (start) return formatDateTime(start) + ' 起'
  return '待公布'
})
const deliveryMethodText = computed(() => logistics.value.deliveryMethod === 'EXPRESS' ? '快递寄送' : logistics.value.deliveryMethod === 'ONSITE' ? '现场送样' : '快递寄送 / 现场送样')
const processSteps = ['提交资料', '支付报名费', '下载标签', '送样入库', '查看结果']
async function loadCompetition() {
  loading.value = true
  loadError.value = ''
  entriesError.value = false
  try {
    competition.value = await fetchPortalCompetitionDetail(route.params.id)
    if (loggedIn.value) {
      try { entries.value = await fetchPortalEntries() } catch { entriesError.value = true }
    }
  } catch {
    loadError.value = '赛事加载失败，请稍后重试'
  } finally {
    loading.value = false
    observeLayout()
  }
}
onMounted(() => {
  loadCompetition()
  window.addEventListener('scroll', handleScroll, { passive: true })
})
onBeforeUnmount(() => {
  layoutObserver?.disconnect()
  window.removeEventListener('scroll', handleScroll)
  cancelAnimationFrame(scrollFrame)
})
function formatDateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '待公布' }
function stageTone(status) { return ({ PUBLISHED: 'tone-gold', REGISTRATION_OPEN: 'tone-green', JUDGING: 'tone-blue' })[status] || 'tone-amber' }
</script>

<style scoped>
:global(body:has(.event-detail-page)),
:global(body .portal-shell:has(.event-detail-page)) { overflow-x: clip; }
.event-detail-page { min-width: 0; }
.back-link { display: inline-flex; align-items: center; gap: 6px; margin-bottom: 18px; color: #746a5f; text-decoration: none; }
.detail-hero { padding: 28px 30px; min-height: 250px; box-sizing: border-box; color: #fff6df; background: linear-gradient(90deg, rgba(31,21,14,.92), rgba(60,40,20,.65)), url("https://images.unsplash.com/photo-1518099074172-2e47ee6cfdc0?auto=format&fit=crop&w=1200&q=72") center / cover; }
.detail-hero { display: flex; flex-direction: column; justify-content: space-between; gap: 24px; }
.detail-hero h1 { margin: 0; font-size: 36px; line-height: 1.3; overflow-wrap: anywhere; }
.hero-meta { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px 24px; }
.hero-meta > .label-chip { flex-shrink: 0; }
.event-source { display: flex; align-items: baseline; justify-content: flex-end; gap: 8px; min-width: 0; max-width: 60%; color: #ead9b7; font-size: 14px; line-height: 1.8; }
.source-label { flex-shrink: 0; padding: 1px 8px; border: 1px solid rgba(234,217,183,.55); border-radius: 4px; font-size: 12px; line-height: 22px; }
.source-name, .platform-organizer { overflow-wrap: anywhere; }
.source-name::before { content: '·'; margin-right: 8px; }
.platform-organizer { max-width: 60%; color: #ead9b7; font-size: 14px; line-height: 26px; }
.hero-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.hero-actions:empty { display: none; }
.primary-action, .secondary-action { display: inline-flex; justify-content: center; align-items: center; min-height: 40px; padding: 8px 16px; box-sizing: border-box; border-radius: 6px; text-decoration: none; font-size: 14px; font-weight: 700; }
.primary-action { background: #e1a23d; color: #2b1d10; }
.primary-action:hover { background: #efb453; }
.secondary-action { color: #fff6df; border: 1px solid #a89b86; }
.date-summary { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); margin: 0; padding: 22px 30px; gap: 24px; background: rgba(255,250,240,.9); border-bottom: 1px solid #ded6c7; }
dt { color: #746a5f; font-size: 14px; }
dd { margin: 7px 0 0; font-weight: 600; overflow-wrap: anywhere; line-height: 1.6; }
.participation { display: flex; align-items: center; flex-wrap: wrap; gap: 16px 24px; padding: 20px 30px; background: #f4f7ef; border-bottom: 1px solid #ded6c7; }
.participation h2 { font-size: 16px; margin: 0; }
.participation-counts { display: flex; flex-wrap: wrap; gap: 10px 20px; flex: 1; font-size: 14px; }
.participation-counts b { color: #1f5a34; }
.section-nav { position: sticky; top: var(--header-offset); z-index: 7; display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 0 30px; min-height: 66px; background: #fffaf0; border-bottom: 1px solid #ded6c7; box-shadow: 0 3px 6px rgba(67,43,17,.04); }
.nav-links { display: flex; align-self: stretch; gap: 32px; }
.nav-links a { display: flex; align-items: center; justify-content: center; min-height: 66px; color: #746a5f; text-decoration: none; font-size: 15px; font-weight: 600; padding: 0 4px; border-bottom: 3px solid transparent; box-sizing: border-box; }
.nav-links a:hover { color: #8f5100; }
.nav-links a.active { color: #7c4e0f; border-bottom-color: #ce912f; }
.detail-section { padding: 28px 30px; background: rgba(255,250,240,.9); border-bottom: 1px solid #ded6c7; scroll-margin-top: var(--section-offset); }
.long-copy { max-width: 60em; margin: 0; font-size: 15px; line-height: 1.85; white-space: pre-wrap; overflow-wrap: anywhere; color: #4f463b; }
.field-row { display: grid; grid-template-columns: 112px minmax(0,1fr); gap: 20px; margin-top: 20px; }
.field-row h3 { margin: 0; font-size: 14px; font-weight: 400; line-height: 1.8; color: #746a5f; }
.category-heading { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.category-heading .portal-section-title { margin: 0; }
.category-heading > span { font-size: 13px; color: #746a5f; }
.category-list { display: flex; flex-wrap: wrap; gap: 10px; list-style: none; margin: 0; padding: 0; }
.category-list li { display: flex; align-items: center; max-width: 100%; min-height: 36px; box-sizing: border-box; padding: 7px 16px; border: 1px solid #dfd3b9; border-radius: 4px; background: #fffdf7; font-size: 15px; font-weight: 600; line-height: 1.5; overflow-wrap: anywhere; }
.requirements-meta { margin-top: 24px; padding-top: 18px; border-top: 1px solid #e8e0d1; }
.requirements-meta .field-row { margin-top: 12px; grid-template-columns: 88px minmax(0,1fr); gap: 16px; }
.requirements-meta .field-row:first-child { margin-top: 0; }
.requirements-meta .field-copy, .requirements-meta .inline-content { font-size: 14px; color: #625646; }
.text-link { display: inline-flex; align-items: center; gap: 6px; border: 0; padding: 0; background: none; color: #875716; font: inherit; font-size: 14px; cursor: pointer; text-decoration: none; line-height: 1.8; }
.text-link:hover { text-decoration: underline; }
.expand-link { margin-top: 14px; }
.inline-content { display: flex; flex-wrap: wrap; gap: 16px; align-items: center; }
.field-copy { margin: 0; line-height: 1.8; overflow-wrap: anywhere; }
.extra-fields { margin-top: 8px; font-size: 14px; color: #746a5f; }
.fee-summary { display: flex; align-items: baseline; flex-wrap: wrap; gap: 12px 24px; }
.fee-summary strong { font-size: 24px; color: #795015; }
.fee-summary span, .rule-copy, .muted { font-size: 14px; color: #746a5f; }
.fee-table { width: 100%; max-width: 640px; border-collapse: collapse; table-layout: fixed; margin-top: 20px; font-size: 15px; }
.quantity-column { width: 42%; }
.discount-column { width: 22%; }
.price-column { width: 36%; }
.fee-table th, .fee-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #e6dece; overflow-wrap: anywhere; }
.fee-table th { font-size: 14px; background: #f7f0df; font-weight: 500; color: #746a5f; }
.fee-table td:last-child, .fee-table th:last-child { text-align: right; }
.fee-table td:last-child { font-variant-numeric: tabular-nums; white-space: nowrap; }
.fee-table td:first-child { font-weight: 500; }
.fee-table td:nth-child(2) { color: #746a5f; }
.price-unit { font-size: 13px; color: #746a5f; font-weight: 400; }
.rule-copy { margin: 14px 0 0; line-height: 1.8; }
.delivery-summary { display: grid; grid-template-columns: minmax(160px,1fr) minmax(0,2fr); margin: 0 0 24px; gap: 24px; }
.process-list { display: grid; grid-template-columns: repeat(5,minmax(0,1fr)); gap: 18px; list-style: none; padding: 0; margin: 24px 0 0; }
.process-list li { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; font-size: 15px; border-top: 2px solid #dac393; padding-top: 14px; }
.process-list span { color: #967024; font-size: 13px; }
.style-list { list-style: none; padding: 0; margin: 20px 0; }
.style-list li { padding: 16px 0; border-bottom: 1px solid #e6dece; }
.style-list h3 { font-size: 16px; margin: 0; overflow-wrap: anywhere; }
.style-list p { font-size: 14px; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; color: #746a5f; margin: 8px 0 0; }
.page-state { padding: 40px 0; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
a:focus-visible, button:focus-visible { outline: 2px solid #875716; outline-offset: 4px; }
@media (max-width: 720px) {
  .detail-hero { padding: 24px 20px; }
  .detail-hero h1 { font-size: 28px; }
  .hero-meta { flex-wrap: wrap; }
  .event-source { flex-basis: 100%; max-width: 100%; justify-content: flex-start; }
  .platform-organizer { flex-basis: 100%; max-width: 100%; }
  .date-summary { grid-template-columns: 1fr; gap: 14px; padding: 20px; }
  .date-summary div { display: flex; justify-content: space-between; gap: 12px; }
  .date-summary dd { margin: 0; text-align: right; }
  .date-summary dt { flex-shrink: 0; }
  .detail-section, .participation { padding: 24px 20px; }
  .section-nav { padding: 0 12px 10px; flex-wrap: wrap; gap: 0; }
  .nav-links { gap: 16px; width: 100%; }
  .nav-links a { min-height: 48px; font-size: 13px; padding: 0; }
  .compact { min-height: 34px; padding: 6px 10px; }
  .field-row { grid-template-columns: 1fr; gap: 8px; }
  .category-list li { font-size: 14px; padding: 7px 12px; }
  .requirements-meta .field-row { grid-template-columns: 1fr; gap: 4px; }
  .delivery-summary { grid-template-columns: 1fr; gap: 16px; }
  .fee-summary strong { font-size: 21px; }
  .fee-table th, .fee-table td { padding: 12px 6px; font-size: 13px; }
  .process-list { grid-template-columns: 1fr; gap: 12px; }
  .process-list li { border-top: 0; padding-top: 0; }
}
</style>
