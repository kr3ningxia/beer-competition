<template>
  <div class="analytics-workbench">
    <div v-if="loading" class="analytics-state">
      <strong>正在加载数据分析</strong>
      <small>请稍等</small>
    </div>

    <div v-else-if="!analytics" class="analytics-state empty">
      <strong>暂无分析数据</strong>
      <small>先完成报名、入库或评分后再查看</small>
    </div>

    <template v-else>
      <header class="analytics-hero">
        <div class="hero-copy">
          <p class="eyebrow">数据分析</p>
          <h2>{{ competitionName }}</h2>
          <p>围绕报名结构、付款构成、送样进度和评语文本做聚合展示，主办方直接看结果。</p>
        </div>
        <div class="hero-meta">
          <span>生成于 {{ formatDateTime(analytics.generatedAt) }}</span>
          <span>评分记录 {{ summary.scoreRecords || 0 }} 条</span>
          <span v-if="summary.testRecordCount">测试记录 {{ summary.testRecordCount }} 条</span>
        </div>
      </header>

      <section class="metric-grid">
        <article v-for="item in summaryCards" :key="item.key" class="metric-card">
          <small>{{ item.label }}</small>
          <strong>{{ item.value }}</strong>
          <p>{{ item.hint }}</p>
        </article>
      </section>

      <section class="analysis-grid">
        <article class="data-card data-card--wide">
          <div class="card-head">
            <div>
              <h3>报名结构</h3>
              <span>按投递组别、风格、酒精度和厂牌拆分</span>
            </div>
            <small>{{ totalRegisteredText }}</small>
          </div>

          <div class="bucket-columns">
            <section class="bucket-panel">
              <h4>投递组别</h4>
              <div class="bucket-list">
                <div v-for="bucket in topBuckets(registration.categories, 8)" :key="bucket.key" class="bucket-row">
                  <div class="bucket-top">
                    <strong>{{ bucket.label }}</strong>
                    <span>{{ bucket.count }} 款</span>
                  </div>
                  <div class="bucket-track">
                    <span :style="bucketBarStyle(bucket)"></span>
                  </div>
                  <div class="bucket-foot">
                    <small>{{ bucket.detail || '按报名分组统计' }}</small>
                    <small>{{ bucketShare(bucket) }}</small>
                  </div>
                </div>
              </div>
            </section>

            <section class="bucket-panel">
              <h4>报名风格</h4>
              <div class="bucket-list">
                <div v-for="bucket in topBuckets(registration.styles, 8)" :key="bucket.key" class="bucket-row">
                  <div class="bucket-top">
                    <strong>{{ bucket.label }}</strong>
                    <span>{{ bucket.count }} 款</span>
                  </div>
                  <div class="bucket-track">
                    <span :style="bucketBarStyle(bucket)"></span>
                  </div>
                  <div class="bucket-foot">
                    <small>{{ bucket.detail || '按风格配置统计' }}</small>
                    <small>{{ bucketShare(bucket) }}</small>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <div class="bucket-columns">
            <section class="bucket-panel">
              <h4>酒精度分布</h4>
              <div class="bucket-list compact">
                <div v-for="bucket in registration.abvBuckets || []" :key="bucket.key" class="bucket-row">
                  <div class="bucket-top">
                    <strong>{{ bucket.label }}</strong>
                    <span>{{ bucket.count }} 款</span>
                  </div>
                  <div class="bucket-track">
                    <span :style="bucketBarStyle(bucket)"></span>
                  </div>
                  <div class="bucket-foot">
                    <small>{{ bucket.detail || '按酒精度区间统计' }}</small>
                    <small>{{ bucketShare(bucket) }}</small>
                  </div>
                </div>
              </div>
            </section>

            <section class="bucket-panel">
              <h4>厂牌集中度</h4>
              <div class="bucket-list compact">
                <div v-for="bucket in topBuckets(registration.breweries, 6)" :key="bucket.key" class="bucket-row">
                  <div class="bucket-top">
                    <strong>{{ bucket.label }}</strong>
                    <span>{{ bucket.count }} 款</span>
                  </div>
                  <div class="bucket-track">
                    <span :style="bucketBarStyle(bucket)"></span>
                  </div>
                  <div class="bucket-foot">
                    <small>{{ bucket.detail || '按厂牌提交量统计' }}</small>
                    <small>{{ bucketShare(bucket) }}</small>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </article>

        <article class="data-card">
          <div class="card-head">
            <div>
              <h3>付款构成</h3>
              <span>只展示正式渠道，不把测试记录单列为付款结构</span>
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
                <small>{{ bucket.detail || '按付款状态统计' }}</small>
                <small>{{ formatMoney(bucket.amount) }}</small>
              </div>
            </div>
          </div>

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
              <span>关注未提交和待入库的酒款</span>
            </div>
          </div>

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
                <small>{{ bucket.detail || '按送样状态统计' }}</small>
                <small>{{ bucketShare(bucket) }}</small>
              </div>
            </div>
          </div>

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
              <span>词云为辅助阅读，重点看高频短语和样例语句</span>
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
import { computed } from 'vue'
import { formatDateTime } from '../../competitionStore'

const props = defineProps({
  analytics: {
    type: Object,
    default: null,
  },
  competitionName: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const summary = computed(() => props.analytics?.summary || {})
const registration = computed(() => props.analytics?.registration || {})
const payment = computed(() => props.analytics?.payment || {})
const delivery = computed(() => props.analytics?.delivery || {})
const feedback = computed(() => props.analytics?.feedback || {})
const warnings = computed(() => props.analytics?.warnings || [])

const summaryCards = computed(() => ([
  { key: 'total', label: '报名总数', value: formatCount(summary.value.totalEntries), hint: '全部提交的参赛酒款' },
  { key: 'registered', label: '有效报名', value: formatCount(summary.value.registeredEntries), hint: '剔除取消后的报名' },
  { key: 'stored', label: '已入库', value: formatCount(summary.value.storedEntries), hint: '完成收样确认的酒款' },
  { key: 'paid', label: '已支付', value: formatCount(summary.value.paidEntries), hint: '正式支付完成的记录' },
  { key: 'pending', label: '待支付', value: formatCount(summary.value.pendingPaymentEntries), hint: '需要继续跟进的报名' },
  { key: 'reviewed', label: '已评审酒款', value: formatCount(summary.value.reviewedEntries), hint: '至少有一条评分记录' },
  { key: 'records', label: '评分记录', value: formatCount(summary.value.scoreRecords), hint: '原始打分条数' },
  { key: 'awards', label: '奖项数量', value: formatCount(summary.value.awardCount), hint: '最终确认的奖项' },
  { key: 'comment', label: '平均评语字数', value: formatCount(summary.value.averageCommentChars), hint: '文本反馈的平均长度' },
  { key: 'duration', label: '平均用时', value: formatDuration(summary.value.averageReviewSeconds), hint: '单条评分平均耗时' },
]))

const paymentChannels = computed(() => payment.value.channels || [])
const paymentStatuses = computed(() => payment.value.statuses || [])
const paymentNotes = computed(() => payment.value.notes || [])
const deliveryStatuses = computed(() => delivery.value.statuses || [])
const deliveryMethods = computed(() => delivery.value.methods || [])
const wordCloudItems = computed(() => (feedback.value.wordCloud || []).slice(0, 28))
const positivePhrases = computed(() => (feedback.value.positivePhrases || []).slice(0, 4))
const negativePhrases = computed(() => (feedback.value.negativePhrases || []).slice(0, 4))
const totalRegisteredText = computed(() => `${formatCount(summary.value.registeredEntries)} 款有效报名`)

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
.hero-copy p,
.card-head span,
.sample-card small,
.phrase-card small {
  color: var(--muted);
}

.analytics-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(216, 169, 53, 0.2);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(216, 169, 53, 0.12), rgba(255, 255, 255, 0.025)),
    rgba(8, 14, 16, 0.94);
}

.hero-copy {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.hero-copy h2 {
  margin: 0;
  color: var(--gold-soft);
  font-size: 26px;
  line-height: 1.2;
}

.hero-copy p {
  margin: 0;
  line-height: 1.7;
}

.eyebrow {
  margin: 0;
  color: #f1bd79;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.hero-meta span {
  padding: 7px 10px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.18);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.06);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
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
  padding: 14px 15px;
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

.metric-card p {
  margin: 0;
  min-height: 34px;
  color: rgba(230, 237, 240, 0.72);
  font-size: 12px;
  line-height: 1.55;
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
}
</style>
