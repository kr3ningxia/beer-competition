<template>
  <div class="analytics-workbench">
    <div v-if="loading" class="analytics-state">
      <strong>正在加载数据分析</strong>
    </div>

    <div v-else-if="!analytics" class="analytics-state empty">
      <strong>暂无分析数据</strong>
      <small>先完成报名、入库或评分后再查看</small>
    </div>

    <template v-else>
      <section class="metric-grid">
        <article v-for="item in summaryCards" :key="item.key" class="metric-card">
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <section class="analysis-grid">
        <article class="data-card data-card--wide">
          <div class="card-head">
            <div>
              <h3>报名结构</h3>
            </div>
            <small>{{ totalRegisteredText }}</small>
          </div>

          <div class="bucket-columns">
            <section class="bucket-panel">
              <h4>投递组别</h4>
              <div class="pie-layout">
                <div class="pie-visual" @mouseleave="clearPieHover">
                  <svg class="pie-chart" viewBox="0 0 200 200" role="img" aria-label="投递组别占比">
                    <path v-for="(bucket, index) in categoryPieBuckets" :key="bucket.key" class="pie-slice" :style="pieFillStyle(index)" :d="piePath(categoryPieBuckets, index)" @mouseenter="setPieHover('category', bucket)" />
                  </svg>
                  <div v-if="hoveredPie?.type === 'category'" class="pie-tooltip"><span>{{ hoveredPie.bucket.label }}</span><strong>{{ hoveredPie.bucket.count }} 款</strong><small>{{ bucketShare(hoveredPie.bucket) }}</small></div>
                </div>
                <div class="pie-legend">
                  <div v-for="(bucket, index) in categoryPieBuckets" :key="bucket.key" class="pie-legend-row"><i :style="pieColorStyle(index)"></i><span>{{ bucket.label }}</span><strong>{{ bucket.count }} 款</strong><small>{{ bucketShare(bucket) }}</small></div>
                </div>
              </div>
            </section>

            <section class="bucket-panel">
              <h4>报名风格</h4>
              <div class="pie-layout">
                <div class="pie-visual" @mouseleave="clearPieHover">
                  <svg class="pie-chart" viewBox="0 0 200 200" role="img" aria-label="报名风格占比">
                    <path v-for="(bucket, index) in stylePieBuckets" :key="bucket.key" class="pie-slice" :style="pieFillStyle(index)" :d="piePath(stylePieBuckets, index)" @mouseenter="setPieHover('style', bucket)" />
                  </svg>
                  <div v-if="hoveredPie?.type === 'style'" class="pie-tooltip"><span>{{ hoveredPie.bucket.label }}</span><strong>{{ hoveredPie.bucket.count }} 款</strong><small>{{ bucketShare(hoveredPie.bucket) }}</small></div>
                </div>
                <div class="pie-legend">
                  <div v-for="(bucket, index) in stylePieBuckets" :key="bucket.key" class="pie-legend-row"><i :style="pieColorStyle(index)"></i><span>{{ bucket.label }}</span><strong>{{ bucket.count }} 款</strong><small>{{ bucketShare(bucket) }}</small></div>
                </div>
              </div>
            </section>
          </div>

          <div class="bucket-columns">
            <section class="bucket-panel abv-panel">
              <h4>酒精度分布</h4>
              <div class="abv-chart"><div v-for="bucket in registration.abvBuckets || []" :key="bucket.key" class="abv-column"><span>{{ bucket.count }}</span><i :style="{ height: `${Math.max(5, Number(bucket.share || 0))}%` }"></i><small>{{ bucket.label }}</small></div></div>
            </section>

            <section class="bucket-panel brewery-panel">
              <h4>参赛厂家</h4>
              <div class="pie-layout">
                <div class="pie-visual" @mouseleave="clearPieHover">
                  <svg class="pie-chart" viewBox="0 0 200 200" role="img" aria-label="参赛厂家占比">
                    <path v-for="(bucket, index) in breweryPieBuckets" :key="bucket.key" class="pie-slice" :style="pieFillStyle(index)" :d="piePath(breweryPieBuckets, index)" @mouseenter="setPieHover('brewery', bucket)" />
                  </svg>
                  <div v-if="hoveredPie?.type === 'brewery'" class="pie-tooltip"><span>{{ hoveredPie.bucket.label }}</span><strong>{{ hoveredPie.bucket.count }} 款</strong><small>{{ bucketShare(hoveredPie.bucket) }}</small></div>
                </div>
                <div class="pie-legend">
                  <div v-for="(bucket, index) in breweryPieBuckets" :key="bucket.key" class="pie-legend-row"><i :style="pieColorStyle(index)"></i><span>{{ bucket.label }}</span><strong>{{ bucket.count }} 款</strong><small>{{ bucketShare(bucket) }}</small></div>
                </div>
              </div>
            </section>
          </div>
        </article>

        <article class="data-card">
          <div class="card-head">
            <div>
              <h3>付款构成</h3>
            </div>
          </div>

          <div class="channel-grid">
            <article v-for="bucket in paymentChannels" :key="bucket.key" class="channel-card">
              <small>{{ bucket.label }}</small>
              <strong>{{ bucket.count }} 笔</strong>
              <span>{{ formatMoney(bucket.amount) }}</span>
              <em>{{ bucketShare(bucket) }}</em>
            </article>
          </div>

          <div v-if="paymentStatuses.length" class="payment-status-layout">
            <div class="donut" :style="donutStyle(paymentStatuses)"><div><strong>{{ paymentCompletion }}%</strong><small>支付完成率</small></div></div>
            <div class="bucket-list compact">
            <div v-for="bucket in paymentStatuses" :key="bucket.key" class="bucket-row">
              <div class="bucket-top">
                <strong>{{ bucket.label }}</strong>
                <span>{{ bucket.count }} 笔</span>
              </div>
              <div class="bucket-track">
                <span :style="bucketBarStyle(bucket)"></span>
              </div>
              <div class="bucket-foot">
                <small v-if="bucket.detail">{{ bucket.detail }}</small>
                <small>{{ formatMoney(bucket.amount) }}</small>
              </div>
            </div>
            </div>
          </div>

          <div v-else class="empty-payment"><strong>暂无付款记录</strong><small>报名尚未产生付款数据</small></div>

          <div v-if="paymentNotes.length" class="note-list">
            <p v-for="note in paymentNotes" :key="note">{{ note }}</p>
          </div>
        </article>
      </section>

      <section class="analysis-grid secondary-grid">
        <article class="data-card">
          <div class="card-head">
            <div>
              <h3>送样进度</h3>
            </div>
          </div>

          <div v-if="deliveryStatuses.length" class="delivery-status-layout">
            <div class="donut" :style="donutStyle(deliveryStatuses)"><div><strong>{{ deliveryCompletion }}%</strong><small>入库完成率</small></div></div>
            <div class="bucket-list compact">
            <div v-for="bucket in deliveryStatuses" :key="bucket.key" class="bucket-row">
              <div class="bucket-top">
                <strong>{{ bucket.label }}</strong>
                <span>{{ bucket.count }} 笔</span>
              </div>
              <div class="bucket-track">
                <span :style="bucketBarStyle(bucket)"></span>
              </div>
              <div class="bucket-foot">
                <small v-if="bucket.detail">{{ bucket.detail }}</small>
                <small>{{ bucketShare(bucket) }}</small>
              </div>
            </div>
            </div>
          </div>

          <div v-else class="empty-payment"><strong>暂无送样记录</strong><small>报名酒款提交送样信息后显示</small></div>

          <div class="channel-grid delivery-grid">
            <article v-for="bucket in deliveryMethods" :key="bucket.key" class="channel-card">
              <small>{{ bucket.label }}</small>
              <strong>{{ bucket.count }} 笔</strong>
              <span>{{ bucketShare(bucket) }}</span>
            </article>
          </div>

          <div v-if="delivery.pendingEntries?.length" class="sample-list">
            <article v-for="sample in delivery.pendingEntries" :key="`${sample.title}-${sample.detail}`" class="sample-card warning">
              <strong>{{ sample.title }}</strong>
              <small>{{ sample.meta }}</small>
              <p>{{ sample.detail }}</p>
            </article>
          </div>
        </article>

        <article class="data-card data-card--wide">
          <div class="card-head">
            <div>
              <h3>评语文本</h3>
            </div>
            <small>{{ feedback.commentCount || 0 }} 条有效评语</small>
          </div>

          <div class="cloud-wrap">
            <span
              v-for="item in wordCloudItems"
              :key="item.text"
              :class="['cloud-item', item.tone]"
              :style="cloudItemStyle(item)"
              :title="item.sample || item.text"
            >
              {{ item.text }}
            </span>
            <div v-if="!wordCloudItems.length" class="cloud-empty">
              当前样本不足，暂不展示词云
            </div>
          </div>

          <div class="phrase-grid">
            <section class="phrase-panel">
              <h4>正向短语</h4>
              <div class="phrase-list">
                <article v-for="item in positivePhrases" :key="item.text" class="phrase-card positive">
                  <strong>{{ item.text }}</strong>
                  <small>{{ phraseMeta(item) }}</small>
                  <p>{{ item.sample || '暂无样例' }}</p>
                </article>
              </div>
            </section>

            <section class="phrase-panel">
              <h4>待关注短语</h4>
              <div class="phrase-list">
                <article v-for="item in negativePhrases" :key="item.text" class="phrase-card negative">
                  <strong>{{ item.text }}</strong>
                  <small>{{ phraseMeta(item) }}</small>
                  <p>{{ item.sample || '暂无样例' }}</p>
                </article>
              </div>
            </section>
          </div>

          <div class="sample-list">
            <article v-for="sample in feedback.samples" :key="`${sample.title}-${sample.detail}`" class="sample-card" :class="sample.tone">
              <strong>{{ sample.title }}</strong>
              <small>{{ sample.meta }}</small>
              <p>{{ sample.detail }}</p>
            </article>
          </div>
        </article>
      </section>

      <section v-if="warnings.length" class="warning-box">
        <p v-for="warning in warnings" :key="warning">{{ warning }}</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  analytics: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const hoveredPie = ref(null)

const summary = computed(() => props.analytics?.summary || {})
const registration = computed(() => props.analytics?.registration || {})
const payment = computed(() => props.analytics?.payment || {})
const delivery = computed(() => props.analytics?.delivery || {})
const feedback = computed(() => props.analytics?.feedback || {})
const warnings = computed(() => props.analytics?.warnings || [])

const paymentCompletion = computed(() => ratio(summary.value.paidEntries, summary.value.totalEntries))
const deliveryCompletion = computed(() => ratio(summary.value.receivedEntries, summary.value.totalEntries))

const summaryCards = computed(() => ([
  { key: 'total', label: '参赛酒款', value: formatCount(summary.value.totalEntries) },
  { key: 'reviewed', label: '已评审酒款', value: formatCount(summary.value.reviewedEntries) },
  { key: 'records', label: '评分记录', value: formatCount(summary.value.scoreRecords) },
  { key: 'commentChars', label: '平均评语字数', value: summary.value.averageCommentChars ? `${formatCount(summary.value.averageCommentChars)} 字` : '0 字' },
  { key: 'reviewDuration', label: '平均评语耗时', value: formatDuration(summary.value.averageReviewSeconds) },
]))

const paymentChannels = computed(() => payment.value.channels || [])
const categoryPieBuckets = computed(() => buildPieBuckets(registration.value.categories, 8))
const stylePieBuckets = computed(() => buildPieBuckets(registration.value.styles, 8))
const breweryPieBuckets = computed(() => buildPieBuckets(registration.value.breweries, 8))
const paymentStatuses = computed(() => payment.value.statuses || [])
const paymentNotes = computed(() => payment.value.notes || [])
const deliveryStatuses = computed(() => delivery.value.statuses || [])
const deliveryMethods = computed(() => delivery.value.methods || [])
const wordCloudItems = computed(() => (feedback.value.wordCloud || []).slice(0, 28))
const positivePhrases = computed(() => (feedback.value.positivePhrases || []).slice(0, 4))
const negativePhrases = computed(() => (feedback.value.negativePhrases || []).slice(0, 4))
const totalRegisteredText = computed(() => `${formatCount(summary.value.totalEntries)} 款参赛酒款`)

function topBuckets(list, limit) {
  return (list || []).slice(0, limit)
}

function bucketShare(bucket) {
  const share = Number(bucket?.share || 0)
  if (!Number.isFinite(share) || share <= 0) {
    return '0%'
  }
  return `${share.toFixed(1)}%`
}

function bucketBarStyle(bucket) {
  const share = Math.max(0, Number(bucket?.share || 0))
  return { width: `${Math.min(100, share || 4)}%` }
}

function buildPieBuckets(list, limit) {
  const source = (list || []).filter((item) => Number(item?.count || 0) > 0)
  if (source.length <= limit) return source
  const visible = source.slice(0, limit - 1)
  const remainder = source.slice(limit - 1).reduce((sum, item) => sum + Number(item.count || 0), 0)
  const total = source.reduce((sum, item) => sum + Number(item.count || 0), 0)
  return [...visible, { key: '__other__', label: '其他', count: remainder, share: total ? Number((remainder * 100 / total).toFixed(1)) : 0, tone: 'neutral' }]
}

function pieFillStyle(index) {
  const colors = ['#d8a935', '#6fcf7a', '#f2994a', '#5b9bd5', '#c76a9c', '#7f8c8d', '#b87935', '#56b4a9']
  return { fill: colors[index % colors.length] }
}

function piePath(list, index) {
  const items = list || []
  const total = items.reduce((sum, item) => sum + Number(item?.count || 0), 0)
  if (!total || !items[index]) return ''
  const start = items.slice(0, index).reduce((sum, item) => sum + Number(item?.count || 0), 0) / total * Math.PI * 2 - Math.PI / 2
  const end = start + Number(items[index].count || 0) / total * Math.PI * 2
  const radius = 92
  const x1 = 100 + radius * Math.cos(start)
  const y1 = 100 + radius * Math.sin(start)
  const x2 = 100 + radius * Math.cos(end)
  const y2 = 100 + radius * Math.sin(end)
  const largeArc = end - start > Math.PI ? 1 : 0
  if (items.length === 1) return `M 100 8 A 92 92 0 1 1 100 192 A 92 92 0 1 1 100 8 Z`
  return `M 100 100 L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2} Z`
}

function setPieHover(type, bucket) {
  hoveredPie.value = { type, bucket }
}

function clearPieHover() {
  hoveredPie.value = null
}

function pieColorStyle(index) {
  const colors = ['#d8a935', '#6fcf7a', '#f2994a', '#5b9bd5', '#c76a9c', '#7f8c8d', '#b87935', '#56b4a9']
  return { background: colors[index % colors.length] }
}

function cloudItemStyle(item) {
  const weight = Math.max(1, Number(item?.weight || 1))
  const size = Math.min(28, 12 + weight / 2.5)
  return {
    fontSize: `${size}px`,
    opacity: String(Math.min(1, 0.5 + weight / 40)),
  }
}

function phraseMeta(item) {
  return `${formatCount(item.count)} 次 · ${formatCount(item.entryCount)} 款 · ${formatCount(item.judgeCount)} 位`
}

function formatCount(value) {
  const count = Number(value || 0)
  return Number.isFinite(count) ? String(count) : '-'
}

function formatDuration(value) {
  const seconds = Number(value || 0)
  if (!seconds) return '0秒'
  if (seconds >= 3600) {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    return `${hours}小时${minutes}分`
  }
  if (seconds >= 60) {
    const minutes = Math.floor(seconds / 60)
    return `${minutes}分${seconds % 60}秒`
  }
  return `${seconds}秒`
}

function formatMoney(value) {
  const amount = Number(value || 0)
  if (!Number.isFinite(amount) || amount <= 0) {
    return '¥0'
  }
  return `¥${amount.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}
function ratio(a, b) {
  const x = Number(a || 0); const y = Number(b || 0)
  return y ? Math.round(x / y * 100) : 0
}

function donutStyle(list) {
  const colors = ['#d8a935', '#6fcf7a', '#f2994a', '#8899a6', '#d85b5b']
  const total = list.reduce((sum, item) => sum + Number(item.count || 0), 0) || 1
  let start = 0
  const stops = list.map((item, index) => { const end = start + Number(item.count || 0) / total * 360; const stop = `${colors[index % colors.length]} ${start}deg ${end}deg`; start = end; return stop })
  return { background: `conic-gradient(${stops.join(',')})` }
}
</script>

<style scoped>
.analytics-workbench {
  display: grid;
  gap: 14px;
  color: var(--text);
}

.analytics-state {
  display: grid;
  place-items: center;
  gap: 6px;
  min-height: 260px;
  padding: 24px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.01)),
    rgba(8, 14, 16, 0.92);
}

.analytics-state strong {
  color: var(--text);
  font-size: 18px;
}

.analytics-state small,
.card-head span,
.sample-card small,
.phrase-card small {
  color: var(--muted);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 13px 15px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.metric-card small {
  font-size: 12px;
  font-weight: 800;
}

.metric-card strong {
  color: #fff0c0;
  font-size: 24px;
  line-height: 1;
}

.analysis-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(350px, 0.72fr);
  gap: 14px;
}

.secondary-grid {
  grid-template-columns: minmax(0, 0.72fr) minmax(0, 1.28fr);
}

.data-card {
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 16px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
}

.data-card--wide {
  min-width: 0;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-head h3,
.bucket-panel h4,
.phrase-panel h4 {
  margin: 0;
  color: var(--text);
  font-size: 16px;
}

.card-head > div {
  display: grid;
  gap: 4px;
}

.bucket-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.bucket-panel {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.06);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.018);
}

.pie-layout {
  display: grid;
  grid-template-columns: minmax(132px, 160px) 1fr;
  align-items: center;
  gap: 14px;
  min-height: 190px;
}

.pie-visual {
  position: relative;
  display: grid;
  place-items: center;
  width: 152px;
  height: 176px;
}

.pie-chart {
  display: grid;
  place-items: center;
  width: 152px;
  height: 152px;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(255,255,255,.08), 0 10px 24px rgba(0,0,0,.18);
}

.pie-slice {
  cursor: pointer;
  stroke: rgba(13, 22, 25, .9);
  stroke-width: 1.5;
  transition: filter .16s ease, opacity .16s ease;
}

.pie-slice:hover {
  filter: brightness(1.14) saturate(1.08);
  stroke: #fff0c0;
  stroke-width: 2.5;
}

.pie-tooltip {
  position: absolute;
  z-index: 2;
  bottom: -2px;
  left: 50%;
  display: flex;
  align-items: baseline;
  gap: 6px;
  max-width: 240px;
  padding: 6px 9px;
  border: 1px solid rgba(216, 169, 53, .35);
  border-radius: 6px;
  color: var(--text);
  background: rgba(8, 14, 16, .96);
  box-shadow: 0 8px 18px rgba(0,0,0,.28);
  font-size: 11px;
  transform: translateX(-50%);
  white-space: nowrap;
  pointer-events: none;
}

.pie-tooltip span { overflow: hidden; text-overflow: ellipsis; }
.pie-tooltip strong { color: #fff0c0; font-size: 12px; }
.pie-tooltip small { color: var(--muted); }

.pie-chart span {
  color: #fff8df;
  font-size: 21px;
  font-weight: 900;
  line-height: 1;
  text-shadow: 0 1px 3px rgba(0,0,0,.65);
}

.pie-chart span small {
  margin-left: 3px;
  color: rgba(255,248,223,.9);
  font-size: 11px;
  font-weight: 700;
}

.pie-legend {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.pie-legend-row {
  display: grid;
  grid-template-columns: 9px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 7px;
  min-width: 0;
  font-size: 12px;
}

.pie-legend-row i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.pie-legend-row span {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pie-legend-row strong { color: #fff0c0; font-size: 12px; white-space: nowrap; }
.pie-legend-row small { color: var(--muted); font-size: 11px; white-space: nowrap; }

.bucket-list {
  display: grid;
  gap: 10px;
}

.bucket-list.compact {
  gap: 8px;
}

.bucket-row {
  display: grid;
  gap: 6px;
}

.bucket-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bucket-top strong {
  min-width: 0;
  color: var(--text);
  font-size: 13px;
  line-height: 1.4;
}

.bucket-top span,
.bucket-foot small {
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.bucket-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(219, 232, 237, 0.07);
}

.bucket-track span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(224, 184, 74, 0.85), rgba(242, 153, 74, 0.85));
}

.bucket-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.bucket-foot small:last-child {
  margin-left: auto;
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.channel-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.channel-card small {
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.channel-card strong {
  color: #fff0c0;
  font-size: 22px;
  line-height: 1.1;
}

.channel-card span,
.channel-card em {
  color: var(--muted);
  font-size: 12px;
  font-style: normal;
}

.note-list {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(242, 153, 74, 0.18);
  border-radius: 8px;
  background: rgba(242, 153, 74, 0.06);
}

.note-list p,
.warning-box p {
  margin: 0;
  color: #f1bd79;
  font-size: 12px;
  line-height: 1.6;
}

.delivery-grid {
  margin-top: 2px;
}

.cloud-wrap {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 10px;
  min-height: 176px;
  padding: 14px;
  border: 1px solid rgba(219, 232, 237, 0.06);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.018);
}

.cloud-item {
  display: inline-flex;
  align-items: center;
  padding: 8px 11px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.025);
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.cloud-item.positive {
  color: #dff5df;
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.07);
}

.cloud-item.negative {
  color: #ffcbcb;
  border-color: rgba(224, 82, 82, 0.22);
  background: rgba(224, 82, 82, 0.08);
}

.cloud-item.neutral {
  color: #e6edf0;
}

.cloud-empty {
  display: grid;
  place-items: center;
  min-height: 144px;
  width: 100%;
  color: var(--muted);
  font-size: 13px;
}

.phrase-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.phrase-panel {
  display: grid;
  gap: 10px;
}

.phrase-list {
  display: grid;
  gap: 8px;
}

.phrase-card,
.sample-card {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
}

.phrase-card strong,
.sample-card strong {
  color: var(--text);
  font-size: 14px;
}

.phrase-card p,
.sample-card p {
  margin: 0;
  color: rgba(230, 237, 240, 0.82);
  font-size: 12px;
  line-height: 1.6;
}

.phrase-card.positive,
.sample-card.positive {
  border-color: rgba(111, 207, 122, 0.18);
  background: rgba(111, 207, 122, 0.06);
}

.phrase-card.negative,
.sample-card.negative,
.sample-card.warning {
  border-color: rgba(242, 153, 74, 0.18);
  background: rgba(242, 153, 74, 0.06);
}

.sample-list {
  display: grid;
  gap: 10px;
}

.warning-box {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid rgba(242, 153, 74, 0.18);
  border-radius: 8px;
  background: rgba(242, 153, 74, 0.05);
}

.payment-status-layout,.delivery-status-layout { display:grid; grid-template-columns:145px 1fr; align-items:center; gap:14px; }
.donut { display:grid; place-items:center; width:138px; height:138px; border-radius:50%; }
.donut > div { display:grid; place-items:center; width:92px; height:92px; border-radius:50%; background:#131c20; }
.donut strong { color:#fff0c0; font-size:23px; }
.donut small { color:var(--muted); font-size:11px; }
.empty-payment { display:grid; place-items:center; align-content:center; min-height:150px; border:1px dashed rgba(219,232,237,.13); border-radius:8px; color:var(--muted); }
.empty-payment strong { color:var(--text); font-size:14px; }
.empty-payment small { margin-top:5px; font-size:12px; }
.abv-panel .bucket-list,.brewery-panel .bucket-list { min-height:150px; }
.abv-chart { display:flex; align-items:end; height:155px; padding:10px 6px 0; border-bottom:1px solid rgba(219,232,237,.09); }
.abv-column { display:flex; flex:1; flex-direction:column; align-items:center; justify-content:end; gap:5px; min-width:0; height:100%; color:var(--muted); font-size:11px; }
.abv-column i { width:24px; min-height:5px; border-radius:5px 5px 0 0; background:linear-gradient(180deg,#f0c85f,#b37930); }
.abv-column small { max-width:70px; overflow:hidden; color:var(--muted); font-size:10px; text-overflow:ellipsis; white-space:nowrap; }
@media (max-width:900px) { .payment-status-layout,.delivery-status-layout { grid-template-columns:1fr; justify-items:center; } }

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .analysis-grid,
  .secondary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .analytics-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-grid,
  .bucket-columns,
  .phrase-grid {
    grid-template-columns: 1fr;
  }

  .pie-layout {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .pie-legend {
    width: 100%;
  }
}
</style>
