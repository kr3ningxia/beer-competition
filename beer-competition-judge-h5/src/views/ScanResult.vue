<template>
  <main class="app-shell">
    <section class="top-panel scan-hero">
      <div class="hero-topline">
        <button class="back-link" type="button" @click="$router.push('/competitions')">返回扫码</button>
        <span :class="['hero-status', entry?.locked ? 'locked' : 'active']">
          {{ entry?.locked ? '本桌已确认' : actionLabel }}
        </span>
      </div>
      <p class="eyebrow">{{ entry?.competitionName || '酒款信息' }}</p>
      <div class="code-lockup">
        <span>编号</span>
        <strong>{{ shortCodeText(entry) }}</strong>
      </div>
      <p v-if="actionHint" class="hero-hint">{{ actionHint }}</p>
      <div v-if="entry" class="hero-meta">
        <span>{{ entry.roundName || '未发布轮次' }}</span>
        <span>{{ entry.tableName || '未分桌' }}</span>
        <span v-if="entry.scored">我的评分已提交</span>
      </div>
    </section>

    <section v-if="entry" class="card info-card">
      <div class="card-head">
        <div>
          <h2 class="section-title compact">酒款信息</h2>
        </div>
        <span :class="['pill', entry.locked ? 'status-lock' : entry.scored ? 'status-ok' : 'status-warn']">
          {{ entry.locked ? '已锁定' : entry.scored ? '可修改' : '待评分' }}
        </span>
      </div>

      <dl class="review-grid">
        <div>
          <dt>当前轮次</dt>
          <dd>{{ entry.roundName || '-' }}</dd>
        </div>
        <div>
          <dt>评审桌</dt>
          <dd>{{ entry.tableName || '-' }}</dd>
        </div>
        <div>
          <dt>投递组别</dt>
          <dd>{{ entry.categoryName || '-' }}</dd>
        </div>
        <div>
          <dt>ABV</dt>
          <dd>{{ entry.abv || '-' }}</dd>
        </div>
        <div class="wide">
          <dt>基础风格</dt>
          <dd>{{ styleDisplayName(entry) || '-' }}</dd>
        </div>
      </dl>

      <section class="description">
        <h2 class="section-title">酒款简介</h2>
        <p>{{ entry.description }}</p>
      </section>

      <section v-if="entry.extraFields?.length" class="extra-fields">
        <h2 class="section-title">补充信息</h2>
        <div class="extra-list">
          <div v-for="field in entry.extraFields" :key="field.key" class="extra-row">
            <span>{{ field.label }}</span>
            <strong>{{ field.value }}</strong>
          </div>
        </div>
      </section>
    </section>

    <section v-else class="card empty-card">
      <h2 class="section-title">没有找到这款酒</h2>
      <p class="caption">请确认编号是否完整，或联系现场工作人员。</p>
      <button class="button secondary full retry-button" type="button" @click="$router.push('/competitions')">
        返回扫码
      </button>
    </section>

    <div v-if="entry" class="sticky-actions">
      <button
        v-if="entry.canScore"
        class="button primary full"
        type="button"
        :disabled="entry.locked"
        @click="$router.push(`/score/${entry.uuid}`)"
      >
        {{ entry.locked ? '本桌结果已确认' : entry.scored ? '修改我的专业评分' : scoreButtonLabel }}
      </button>
      <button
        v-if="entry.action === 'CAPTAIN'"
        class="button secondary full"
        type="button"
        @click="$router.push(`/captain/${entry.uuid}`)"
      >
        查看本桌评分
      </button>
      <button
        v-if="entry.action === 'RANKING'"
        class="button primary full"
        type="button"
        @click="$router.push(`/ranking/${entry.roundTableId}`)"
      >
        进入本轮排序
      </button>
      <button class="button secondary full" type="button" @click="$router.push('/competitions')">
        继续扫码
      </button>
    </div>

    <nav class="bottom-nav" :style="{ gridTemplateColumns: `repeat(${navItems.length}, minmax(0, 1fr))` }">
      <router-link v-for="item in navItems" :key="item.to" class="nav-item" :to="item.to">
        {{ item.label }}
      </router-link>
    </nav>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchEntry, fetchMe, resolveScanEntry } from '@/api/judge'

const route = useRoute()
const code = computed(() => String(route.params.code || route.params.uuid || '').toUpperCase())
const resolvingScan = computed(() => Boolean(route.params.code))
const entry = ref(null)
const me = ref(null)
const actionLabel = computed(() => {
  if (entry.value?.action === 'CAPTAIN') return '桌长汇总'
  if (entry.value?.action === 'RANKING') return '本轮排序'
  return entry.value?.scored ? '可修改评分' : '可评分'
})
const actionHint = computed(() => {
  if (!entry.value) return '正在读取扫码信息，请稍候。'
  if (entry.value.locked) return '本桌结果已经确认，评分内容仅供查看。'
  if (entry.value.action === 'CAPTAIN') return ''
  if (entry.value.action === 'RANKING') return '本轮需要从候选酒款中完成排序。'
  return entry.value.scored ? '你已经提交过本款评分，需要时可以继续修改。' : '请先核对酒款信息，再填写本人的评分。'
})
const scoreButtonLabel = computed(() => (
  me.value?.role === 'CAPTAIN' ? '填写我的专业评分' : '开始评分'
))

const navItems = computed(() => {
  const items = [
    { label: '扫码', to: '/competitions' },
  ]
  if (me.value?.role === 'CAPTAIN') items.push({ label: '本桌', to: '/captain' })
  items.push({ label: '我的', to: '/profile' })
  return items
})

onMounted(async () => {
  me.value = await fetchMe()
  try {
    entry.value = resolvingScan.value ? await resolveScanEntry(code.value) : await fetchEntry(code.value)
  } catch {
    entry.value = null
  }
})

function styleDisplayName(source) {
  return [source?.styleCode, source?.style].filter(Boolean).join(' ')
}

function shortCodeText(source) {
  return source?.shortCode || '读取中'
}
</script>

<style scoped>
.scan-hero {
  overflow: hidden;
  padding: 16px 18px 18px;
}

.scan-hero::after {
  content: "";
  display: block;
  height: 1px;
  margin: 16px -18px -18px;
  background: linear-gradient(90deg, rgba(241, 174, 103, 0.58), rgba(255, 255, 255, 0));
}

.hero-topline {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.back-link {
  border: 0;
  margin: 0;
  padding: 0;
  color: rgba(255, 255, 255, 0.74);
  background: transparent;
  font-weight: 750;
}

.hero-status {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 850;
}

.hero-status.active {
  color: #d7f7e6;
  background: rgba(6, 118, 71, 0.28);
}

.hero-status.locked {
  color: #e4e7ec;
  background: rgba(255, 255, 255, 0.12);
}

.code-lockup {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: baseline;
  margin-top: 10px;
}

.code-lockup span {
  color: rgba(248, 250, 252, 0.68);
  font-size: 14px;
  font-weight: 750;
}

.code-lockup strong {
  overflow-wrap: anywhere;
  color: #fff;
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
}

.hero-hint {
  max-width: 34em;
  margin: 12px 0 0;
  color: rgba(248, 250, 252, 0.78);
  font-size: 13px;
  line-height: 1.55;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 13px;
}

.hero-meta span {
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  padding: 4px 9px;
  color: rgba(248, 250, 252, 0.82);
  background: rgba(255, 255, 255, 0.075);
  font-size: 12px;
  font-weight: 750;
}

.info-card {
  display: grid;
  gap: 16px;
  margin-top: 12px;
}

.card-head {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf0ee;
}

.card-head > div {
  min-width: 0;
}

.card-head p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
}

.card-head .pill {
  flex: 0 0 auto;
}

.compact {
  margin-bottom: 0;
}

.review-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.review-grid div {
  min-width: 0;
  border: 1px solid #eef1ef;
  border-radius: 8px;
  padding: 11px 10px;
  background: #f8faf8;
}

.review-grid .wide {
  grid-column: 1 / -1;
  background: #fffaf3;
  border-color: #f4dfc6;
}

dt {
  margin: 0;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

dd {
  margin: 6px 0 0;
  color: #18222f;
  font-size: 16px;
  font-weight: 850;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.description,
.extra-fields {
  margin-top: 0;
}

.description p {
  margin: 0;
  color: #344054;
  line-height: 1.7;
}

.extra-list {
  display: grid;
  gap: 8px;
}

.extra-row {
  display: grid;
  gap: 6px;
  border: 1px solid #eef1ef;
  border-radius: 8px;
  padding: 10px 11px;
  background: #fbfcfb;
}

.extra-row span {
  color: #667085;
  font-size: 13px;
}

.extra-row strong {
  color: #18222f;
  line-height: 1.45;
}

.empty-card {
  margin-top: 12px;
}

.retry-button {
  margin-top: 14px;
}

.sticky-actions {
  position: sticky;
  bottom: 72px;
  display: grid;
  gap: 9px;
  margin-top: 12px;
  padding: 12px;
  border: 1px solid rgba(24, 34, 47, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(24, 34, 47, 0.12);
  backdrop-filter: blur(14px);
}

@media (max-width: 360px) {
  .scan-hero {
    padding-inline: 16px;
  }

  .code-lockup strong {
    font-size: 30px;
  }

  .card-head {
    display: grid;
  }

  .review-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .review-grid .wide {
    grid-column: auto;
  }
}
</style>
