<template>
  <main class="app-shell scan-result-page">
    <section class="scan-hero">
      <div class="hero-back-row">
        <button class="hero-back" type="button" @click="$router.push('/competitions')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M15 18 9 12l6-6" />
          </svg>
          <span>返回扫码</span>
        </button>
      </div>

      <div class="hero-identity">
        <div class="hero-copy">
          <p>{{ entry?.competitionName || '酒款信息' }}</p>
          <div class="code-lockup">
            <span>编号：</span>
            <strong>{{ shortCodeText(entry) }}</strong>
          </div>
        </div>
        <div v-if="entry" class="hero-actions">
          <div class="hero-metrics" aria-label="评审信息">
            <span>{{ entry.roundName || '-' }}</span>
            <span>{{ entry.tableName || '-' }}</span>
          </div>
        </div>
      </div>
    </section>

    <div v-if="entry" class="scan-content">
      <section class="card scan-card style-card">
        <h2 class="scan-section-title">风格信息</h2>

        <dl class="style-summary">
          <div>
            <dt>投递组别</dt>
            <dd>{{ entry.categoryName || '-' }}</dd>
          </div>
          <div>
            <dt>基础风格</dt>
            <dd>{{ styleDisplayName(entry) || '-' }}</dd>
          </div>
          <div>
            <dt>ABV</dt>
            <dd class="abv-value">{{ abvText(entry) }}</dd>
          </div>
        </dl>

        <button
          class="style-category-row"
          type="button"
          :aria-expanded="styleDetailOpen"
          @click="openStyleDetail"
        >
          <span>风格分类</span>
          <strong>{{ styleCategoryText(entry) }}</strong>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="m9 18 6-6-6-6" />
          </svg>
        </button>
      </section>

      <section v-if="entry.extraFields?.length" class="card scan-card">
        <h2 class="scan-section-title">补充信息</h2>
        <div class="extra-list">
          <div v-for="field in entry.extraFields" :key="field.key" class="extra-row">
            <span>{{ field.label }}</span>
            <strong>{{ field.value }}</strong>
          </div>
        </div>
      </section>

      <div v-if="(!entry.locked && entry.canScore) || entry?.action === 'RANKING'" class="primary-action">
        <button
          v-if="!entry.locked && entry.canScore"
          class="button primary full"
          type="button"
          @click="$router.push(`/score/${entry.uuid}`)"
        >
          <span>{{ entry.scored ? '修改我的专业评分' : scoreButtonLabel }}</span>
        </button>
        <button
          v-if="entry?.action === 'RANKING'"
          class="button primary full"
          type="button"
          @click="$router.push(`/ranking/${entry.roundTableId}`)"
        >
          进入本轮排序
        </button>
      </div>

      <div :class="['secondary-actions', { single: entry?.action !== 'CAPTAIN' }]">
        <button
          v-if="entry?.action === 'CAPTAIN'"
          class="button secondary quiet full"
          type="button"
          @click="$router.push(`/captain/${entry.uuid}`)"
        >
          查看本桌评分
        </button>
        <button class="button secondary quiet full" type="button" @click="$router.push('/competitions')">
          继续扫码
        </button>
      </div>
    </div>

    <section v-else-if="loading" class="card scan-card empty-card">
      <h2 class="scan-section-title">正在读取酒款</h2>
      <p class="caption">请稍候。</p>
    </section>

    <section v-else class="card scan-card empty-card">
      <h2 class="scan-section-title">没有找到这款酒</h2>
      <p class="caption">请确认编号是否完整，或联系现场工作人员。</p>
      <button class="button secondary full retry-button" type="button" @click="$router.push('/competitions')">
        返回扫码
      </button>
    </section>

    <Teleport to="body">
      <div
        v-if="styleDetailOpen"
        class="style-detail-backdrop"
        role="presentation"
        @click.self="closeStyleDetail"
      >
        <section
          class="style-detail-sheet"
          role="dialog"
          aria-modal="true"
          aria-labelledby="style-detail-title"
        >
          <header class="style-detail-head">
            <div>
              <span>风格分类</span>
              <h2 id="style-detail-title">{{ styleCategoryText(entry) }}</h2>
              <p>{{ styleDisplayName(entry) || '基础风格' }}</p>
            </div>
            <button class="style-detail-close" type="button" aria-label="关闭风格介绍" @click="closeStyleDetail">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 6l12 12M18 6 6 18" />
              </svg>
            </button>
          </header>

          <div class="style-detail-body">
            <p>{{ styleDetailText.main }}</p>
            <section v-if="styleDetailText.metrics" class="style-metrics-panel">
              <span>指标范围</span>
              <p>{{ styleDetailText.metrics }}</p>
            </section>
          </div>
        </section>
      </div>
    </Teleport>

    <JudgeBottomNav :role="me?.role" :active="scanResultActiveNav" :hide-table="hideTableNav" />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCompetitions, fetchEntry, fetchMe, resolveScanEntry } from '@/api/judge'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'
import { formatAbvWithUnit } from '@/utils/formatters'
import { isRankingTaskType, selectCurrentTask } from '@/utils/judgeTasks'

const route = useRoute()
const code = computed(() => String(route.params.code || route.params.uuid || '').toUpperCase())
const resolvingScan = computed(() => Boolean(route.params.code))
const entry = ref(null)
const me = ref(null)
const currentTask = ref(null)
const loading = ref(true)
const styleDetailOpen = ref(false)
const scoreButtonLabel = computed(() => (
  me.value?.role === 'CAPTAIN' ? '填写我的专业评分' : '开始评分'
))
const styleDetailText = computed(() => splitStyleDescription(entry.value?.styleDescription))

const hideTableNav = computed(() => isRankingTaskType(currentTask.value?.taskType))
const scanResultActiveNav = computed(() => (me.value?.role === 'CAPTAIN' && !hideTableNav.value ? 'table' : 'scan'))

onMounted(async () => {
  const [profile, tasks] = await Promise.all([fetchMe(), fetchCompetitions().catch(() => [])])
  me.value = profile
  currentTask.value = selectCurrentTask(tasks)
  try {
    entry.value = resolvingScan.value ? await resolveScanEntry(code.value) : await fetchEntry(code.value)
  } catch {
    entry.value = null
  } finally {
    loading.value = false
  }
})

function styleDisplayName(source) {
  return [source?.styleCode, source?.style].filter(Boolean).join(' ')
}

function styleCategoryText(source) {
  return source?.styleCategoryName || source?.categoryName || '-'
}

function abvText(source) {
  return formatAbvWithUnit(source?.abv)
}

function shortCodeText(source) {
  return source?.shortCode || '读取中'
}

function openStyleDetail() {
  if (!entry.value) return
  styleDetailOpen.value = true
}

function closeStyleDetail() {
  styleDetailOpen.value = false
}

function splitStyleDescription(description) {
  const text = String(description || '').trim()
  if (!text) return { main: '暂无风格介绍。', metrics: '' }
  const marker = '指标范围：'
  const markerIndex = text.indexOf(marker)
  if (markerIndex < 0) return { main: text, metrics: '' }
  return {
    main: text.slice(0, markerIndex).trim(),
    metrics: text.slice(markerIndex + marker.length).trim(),
  }
}

</script>

<style scoped>
.scan-result-page {
  width: min(100%, 568px);
  padding: 0 0 68px;
  background: #f7f6f2;
}

.scan-result-page > * + * {
  margin-top: 0;
}

.scan-hero {
  min-height: 176px;
  padding: 24px 24px 22px;
  color: #fff;
  background: #3a4737;
}

.hero-back-row {
  display: flex;
  align-items: center;
  min-height: 28px;
}

.hero-back {
  display: inline-flex;
  align-items: center;
  border: 0;
}

.hero-back {
  gap: 9px;
  padding: 0;
  color: #f4f7f0;
  background: transparent;
  font-size: 20px;
  font-weight: 850;
}

.hero-back svg {
  width: 27px;
  height: 27px;
  stroke: currentColor;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero-identity {
  display: flex;
  gap: 14px;
  justify-content: space-between;
  align-items: flex-start;
  margin-top: 18px;
}

.hero-copy {
  min-width: 0;
  padding-top: 4px;
}

.hero-copy p {
  margin: 0 0 10px;
  color: rgba(239, 244, 235, 0.78);
  font-size: 16px;
  line-height: 1.25;
}

.code-lockup {
  display: flex;
  align-items: baseline;
  min-width: 0;
}

.code-lockup span {
  flex: 0 0 auto;
  color: rgba(245, 247, 241, 0.82);
  font-size: 18px;
  font-weight: 850;
}

.code-lockup strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #fff;
  font-size: 34px;
  font-weight: 900;
  line-height: 0.95;
}

.hero-actions {
  display: grid;
  flex: 0 0 auto;
  justify-items: end;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  max-width: 188px;
}

.hero-metrics span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  max-width: 88px;
  border-radius: 999px;
  padding: 6px 12px;
  color: rgba(255, 255, 255, 0.96);
  background: rgba(255, 255, 255, 0.12);
  font-size: 14px;
  font-weight: 850;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scan-content {
  display: grid;
  gap: 22px;
  padding: 28px 24px 0;
}

.scan-card {
  border: 1px solid rgba(30, 34, 27, 0.1);
  border-radius: 22px;
  padding: 24px 22px;
  background: #fff;
  box-shadow: 0 2px 9px rgba(45, 45, 38, 0.14);
}

.scan-section-title {
  position: relative;
  margin: 0 0 17px;
  padding-left: 17px;
  color: #07111f;
  font-size: 21px;
  font-weight: 900;
  line-height: 1.2;
}

.scan-section-title::before {
  content: "";
  position: absolute;
  top: 2px;
  bottom: 1px;
  left: 0;
  width: 5px;
  border-radius: 999px;
  background: #d17932;
}

.style-summary {
  display: grid;
  grid-template-columns: 0.8fr 1.25fr 0.55fr;
  gap: 22px;
  margin: 0;
}

.style-summary div {
  min-width: 0;
}

dt {
  margin: 0 0 7px;
  color: #5f6873;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.25;
}

dd {
  margin: 0;
  color: #020817;
  font-size: 17px;
  font-weight: 900;
  line-height: 1.32;
  overflow-wrap: anywhere;
}

.abv-value {
  color: #d17932;
}

.style-category-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  width: calc(100% + 44px);
  margin: 25px -22px -24px;
  border-top: 1px solid #e6e1db;
  border-right: 0;
  border-bottom: 0;
  border-left: 0;
  padding: 18px 22px 19px;
  color: inherit;
  background: transparent;
  text-align: left;
  appearance: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.style-category-row:active {
  background: #fbfaf7;
}

.style-category-row span {
  color: #5f6873;
  font-size: 16px;
}

.style-category-row strong {
  min-width: 0;
  color: #020817;
  font-size: 19px;
  font-weight: 900;
  line-height: 1.25;
  text-align: right;
  overflow-wrap: anywhere;
}

.style-category-row svg {
  width: 21px;
  height: 21px;
  stroke: #5d5d57;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.style-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  align-items: end;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
  background: rgba(8, 12, 10, 0.42);
}

.style-detail-sheet {
  width: min(100%, 536px);
  max-height: min(78vh, 720px);
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid rgba(29, 32, 26, 0.12);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 -18px 42px rgba(15, 19, 13, 0.24);
}

.style-detail-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  gap: 12px;
  align-items: start;
  padding: 22px 22px 16px;
  border-bottom: 1px solid #ece7df;
}

.style-detail-head span {
  display: block;
  margin-bottom: 7px;
  color: #d17932;
  font-size: 14px;
  font-weight: 900;
  line-height: 1;
}

.style-detail-head h2 {
  margin: 0;
  color: #07111f;
  font-size: 21px;
  line-height: 1.22;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.style-detail-head p {
  margin: 7px 0 0;
  color: #687280;
  font-size: 14px;
  line-height: 1.45;
  font-weight: 650;
  overflow-wrap: anywhere;
}

.style-detail-close {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid #e4ded5;
  border-radius: 999px;
  color: #4e574c;
  background: #f8f7f3;
  appearance: none;
}

.style-detail-close svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
}

.style-detail-body {
  max-height: calc(min(78vh, 720px) - 126px);
  overflow-y: auto;
  padding: 18px 22px 24px;
  color: #263241;
  overscroll-behavior: contain;
}

.style-detail-body > p {
  margin: 0;
  color: #263241;
  font-size: 16px;
  line-height: 1.72;
  font-weight: 560;
  white-space: pre-wrap;
}

.style-metrics-panel {
  margin-top: 18px;
  border: 1px solid #eadfd3;
  border-radius: 18px;
  padding: 15px 16px;
  background: #fbf7f2;
}

.style-metrics-panel span {
  display: block;
  margin-bottom: 7px;
  color: #b76427;
  font-size: 14px;
  font-weight: 900;
  line-height: 1;
}

.style-metrics-panel p {
  margin: 0;
  color: #2d3541;
  font-size: 15px;
  line-height: 1.68;
  font-weight: 680;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.extra-list {
  display: grid;
  gap: 10px;
}

.extra-row {
  display: grid;
  gap: 7px;
  border-radius: 18px;
  padding: 16px 17px;
  background: #f8f7f5;
}

.extra-row span {
  color: #6b7280;
  font-size: 16px;
  line-height: 1.25;
}

.extra-row strong {
  color: #020817;
  font-size: 18px;
  font-weight: 900;
  line-height: 1.45;
}

.primary-action {
  display: grid;
  margin-top: 2px;
}

.primary-action .button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 64px;
  border-radius: 16px;
  background: #d17932;
  font-size: 19px;
  font-weight: 900;
}

.secondary-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: -2px;
}

.secondary-actions.single {
  grid-template-columns: minmax(0, 1fr);
}

.secondary-actions .button {
  min-height: 50px;
  border: 1px solid rgba(30, 34, 27, 0.13);
  border-radius: 13px;
  color: #161515;
  background: #fff;
  box-shadow: 0 1px 4px rgba(45, 45, 38, 0.1);
  font-size: 16px;
  font-weight: 850;
}

.empty-card {
  margin: 28px 24px 0;
}

.retry-button {
  margin-top: 16px;
}

@media (max-width: 420px) {
  .scan-hero {
    min-height: 164px;
    padding: 22px 19px 18px;
  }

  .hero-back {
    font-size: 19px;
  }

  .hero-identity {
    align-items: flex-start;
    margin-top: 16px;
  }

  .hero-copy p {
    font-size: 15px;
  }

  .code-lockup span {
    font-size: 17px;
  }

  .code-lockup strong {
    font-size: 32px;
  }

  .hero-metrics {
    gap: 6px;
    max-width: 166px;
  }

  .hero-metrics span {
    min-height: 28px;
    max-width: 78px;
    padding: 5px 10px;
    font-size: 13px;
  }

  .scan-content {
    gap: 22px;
    padding: 22px 18px 0;
  }

  .scan-card {
    border-radius: 18px;
    padding: 20px 16px;
  }

  .scan-section-title {
    font-size: 18px;
  }

  .style-summary {
    grid-template-columns: 0.85fr 1.15fr 0.5fr;
    gap: 15px;
  }

  dt {
    font-size: 13px;
  }

  dd {
    font-size: 15px;
  }

  .style-category-row {
    width: calc(100% + 32px);
    margin: 22px -16px -20px;
    padding: 16px;
  }

  .style-category-row span {
    font-size: 14px;
  }

  .style-category-row strong {
    font-size: 16px;
  }

  .extra-row {
    border-radius: 14px;
    padding: 14px 15px;
  }

  .extra-row span {
    font-size: 14px;
  }

  .extra-row strong {
    font-size: 16px;
  }

  .primary-action .button {
    min-height: 54px;
    border-radius: 14px;
    font-size: 17px;
  }

  .secondary-actions .button {
    min-height: 50px;
    font-size: 15px;
  }

  .empty-card {
    margin-inline: 18px;
  }

  .style-detail-backdrop {
    padding: 14px 10px calc(14px + env(safe-area-inset-bottom));
  }

  .style-detail-sheet {
    max-height: min(82vh, 720px);
    border-radius: 22px 22px 18px 18px;
  }

  .style-detail-head {
    padding: 20px 18px 14px;
  }

  .style-detail-head h2 {
    font-size: 19px;
  }

  .style-detail-body {
    max-height: calc(min(82vh, 720px) - 118px);
    padding: 16px 18px 22px;
  }

  .style-detail-body > p {
    font-size: 15px;
    line-height: 1.68;
  }

}

@media (max-width: 340px) {
  .style-summary {
    grid-template-columns: 1fr;
  }

  .secondary-actions {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
