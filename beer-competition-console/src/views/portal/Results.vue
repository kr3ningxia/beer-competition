<template>
  <div class="results-page">
    <section class="result-hero">
      <div>
        <span class="label-chip tone-gold">结果中心</span>
        <h1>我的结果</h1>
        <p>{{ resultHeroText }}</p>
      </div>
      <div class="result-stats">
        <span><small>参赛酒款</small><b>{{ results.length }}</b></span>
        <span><small>已发布</small><b>{{ publishedEntries.length }}</b></span>
        <span><small>{{ resultStatsThirdLabel }}</small><b>{{ resultStatsThirdValue }}</b></span>
      </div>
    </section>

    <section v-if="results.length" class="result-workbench">
      <aside class="result-sidebar brewer-card">
        <div class="filter-panel">
          <el-input v-model="keyword" placeholder="搜索酒名 / 编号 / 赛事" clearable />
          <div class="filter-tabs" aria-label="结果筛选">
            <button
              v-for="item in filterOptions"
              :key="item.value"
              :class="{ active: statusFilter === item.value }"
              type="button"
              @click="statusFilter = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div v-if="groupedResults.length" class="competition-groups">
          <section v-for="group in groupedResults" :key="group.competitionId || group.competitionName" class="competition-group">
            <div class="group-title">
              <strong>{{ group.competitionName }}</strong>
              <small>{{ group.entries.length }} 款</small>
            </div>
            <button
              v-for="entry in group.entries"
              :key="entry.entryId"
              :class="['result-entry', { active: entry.entryId === selectedId }]"
              type="button"
              @click="selectEntry(entry.entryId)"
            >
              <span :class="['label-chip', `tone-${resultTone(entry)}`]">{{ resultStatusLabel(entry) }}</span>
              <strong>{{ entry.entryName }}</strong>
              <small>{{ entry.categoryName || '组别待确认' }} · {{ entry.style || '风格待确认' }}</small>
              <em>{{ resultBrief(entry) }}</em>
            </button>
          </section>
        </div>

        <div v-else class="side-empty">
          <strong>没有匹配的酒款</strong>
          <p>换一个关键词或筛选条件</p>
        </div>
      </aside>

      <main class="result-detail">
        <template v-if="selectedEntry?.published">
          <section class="result-summary-card brewer-card">
            <div class="result-overview-head">
              <div class="summary-title">
                <span :class="['label-chip', `tone-${resultTone(selectedEntry)}`]">{{ resultStatusLabel(selectedEntry) }}</span>
                <h2>{{ selectedEntry.entryName }}</h2>
                <p>{{ selectedEntry.competitionName || '赛事信息待确认' }}</p>
              </div>

              <div class="result-actions">
                <button
                  v-if="!isFeedbackOnlySelectedEntry"
                  class="primary-action"
                  type="button"
                  :disabled="!selectedEntry.certificateAvailable"
                  @click="downloadCertificate"
                >
                  下载证书
                </button>
                <RouterLink class="secondary-action" to="/portal/my">查看参赛进度</RouterLink>
              </div>
            </div>

            <div :class="['result-outcome-grid', { 'feedback-only': isFeedbackOnlySelectedEntry }]">
              <section class="outcome-card">
                <small>{{ isFeedbackOnlySelectedEntry ? '诊断结果' : '最终结果' }}</small>
                <strong>{{ resultOutcome }}</strong>
              </section>
              <section>
                <small>综合得分</small>
                <strong>{{ finalScoreText }}</strong>
              </section>
              <section v-if="!isFeedbackOnlySelectedEntry">
                <small>证书状态</small>
                <strong>{{ certificateStatusText }}</strong>
              </section>
            </div>

            <dl class="entry-meta">
              <div>
                <dt>参赛组别</dt>
                <dd>{{ selectedEntry.categoryName || '组别待确认' }}</dd>
              </div>
              <div>
                <dt>基础风格</dt>
                <dd>{{ selectedEntry.style || '风格待确认' }}</dd>
              </div>
              <div>
                <dt>组内规模</dt>
                <dd>{{ categoryEntryCountText }}</dd>
              </div>
            </dl>
          </section>

          <section v-if="captainScore" class="captain-card brewer-card">
            <div class="section-heading">
              <h2 class="portal-section-title">综合评语</h2>
              <span>{{ scoreWithMax(captainScore, true) }}</span>
            </div>
            <p>{{ readableComment(captainScore.comments, '暂无综合文字反馈') }}</p>
          </section>

          <section class="feedback-card brewer-card">
            <div class="section-heading">
              <h2 class="portal-section-title">评审反馈</h2>
              <span>{{ judgeScores.length }} 条</span>
            </div>
            <div v-if="judgeScores.length" class="feedback-list">
              <article v-for="score in judgeScores" :key="score.judgeLabel" class="feedback-row">
                <header class="feedback-head">
                  <div>
                    <strong>{{ judgeDisplayName(score) }}</strong>
                    <small v-if="judgeDisplayName(score) !== scoreRoleLabel(score.judgeRoleType)">{{ scoreRoleLabel(score.judgeRoleType) }}</small>
                  </div>
                  <span>{{ scoreWithMax(score) }}</span>
                </header>

                <div v-if="score.dimensions?.length" class="dimension-table">
                  <div v-for="dimension in score.dimensions" :key="dimension.key || dimension.label" class="dimension-row">
                    <span>{{ dimension.label || dimension.key }}</span>
                    <strong>{{ dimensionScoreText(dimension) }}</strong>
                    <em>{{ readableComment(dimension.note, '') }}</em>
                  </div>
                </div>
              </article>
            </div>
            <p v-else class="empty-feedback">组委会暂未发布评审反馈</p>
          </section>

          <section v-if="resultDetail.roundResults.length" class="round-result-card brewer-card">
            <div class="section-heading">
              <h2 class="portal-section-title">{{ isFeedbackOnlySelectedEntry ? '诊断记录' : '晋级与奖项记录' }}</h2>
              <span>{{ resultDetail.roundResults.length }} 条</span>
            </div>
            <div class="round-result-list">
              <span v-for="(roundResult, index) in resultDetail.roundResults" :key="`${roundResult.resultType}-${roundResult.rankNo}-${index}`">
                <small>{{ resultTypeLabel(roundResult.resultType || roundResult.awardType) }}</small>
                <strong>{{ roundResult.awardName || roundResult.slotLabel || resultTypeLabel(roundResult.resultType) }}</strong>
                <em v-if="roundResult.rankNo">第 {{ roundResult.rankNo }} 名</em>
              </span>
            </div>
          </section>
        </template>

        <section v-else-if="selectedEntry" class="locked-card brewer-card">
          <span :class="['label-chip', `tone-${resultTone(selectedEntry)}`]">{{ resultStatusLabel(selectedEntry) }}</span>
          <h2>{{ selectedEntry.entryName }}</h2>
          <p>{{ selectedEntry.lockReason || lockedResultHint }}</p>
          <dl class="entry-meta locked-meta">
            <div>
              <dt>赛事</dt>
              <dd>{{ selectedEntry.competitionName || '赛事信息待确认' }}</dd>
            </div>
            <div>
              <dt>参赛组别</dt>
              <dd>{{ selectedEntry.categoryName || '组别待确认' }}</dd>
            </div>
            <div>
              <dt>基础风格</dt>
              <dd>{{ selectedEntry.style || '风格待确认' }}</dd>
            </div>
          </dl>
          <div class="locked-progress">
            <span class="done">提交资料</span>
            <span :class="{ done: selectedEntry.status !== 'PENDING_PAYMENT' }">支付报名费</span>
            <span :class="{ done: selectedEntry.status === 'STORED' || selectedEntry.status === 'RESULT_PUBLISHED' }">样品入库</span>
            <span :class="{ done: selectedEntry.status === 'RESULT_PUBLISHED' }">结果发布</span>
          </div>
          <div class="empty-actions">
            <RouterLink to="/portal/my">返回我的参赛</RouterLink>
            <RouterLink to="/portal/entries">查看酒款资料</RouterLink>
          </div>
        </section>

        <section v-else class="locked-card brewer-card">
          <h2>当前暂无已发布结果</h2>
          <p>选择左侧酒款，查看结果状态和发布后的反馈</p>
          <div class="empty-actions">
            <RouterLink to="/portal/my">查看我的参赛</RouterLink>
            <RouterLink to="/portal/events">浏览开放赛事</RouterLink>
          </div>
        </section>
      </main>
    </section>

    <section v-else class="locked-card brewer-card">
      <h2>还没有参赛酒款</h2>
      <p>提交报名后，比赛结果会在发布后显示</p>
      <div class="empty-actions">
        <RouterLink to="/portal/my">查看我的参赛</RouterLink>
        <RouterLink to="/portal/events">浏览开放赛事</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { downloadPortalResultCertificate, fetchPortalResultDetail, fetchPortalResults } from '@/api/portal'
import { entryStatusMeta, isEntryAwarded, isEntryResultPublished } from './portalViewModels'

const route = useRoute()
const router = useRouter()
const results = ref([])
const selectedId = ref(null)
const keyword = ref('')
const statusFilter = ref('all')
const resultDetail = ref({ summary: null, scores: [], roundResults: [] })

const selectedEntry = computed(() => resultDetail.value.summary || results.value.find((entry) => entry.entryId === selectedId.value))
const hasOnlyFeedbackResults = computed(() => results.value.length > 0 && results.value.every((entry) => isFeedbackOnlyEntry(entry)))
const filterOptions = computed(() => {
  const options = [
    { label: '全部', value: 'all' },
    { label: '已发布', value: 'published' },
    { label: '待发布', value: 'locked' },
  ]
  if (!hasOnlyFeedbackResults.value) options.push({ label: '已获奖', value: 'awarded' })
  return options
})
const publishedEntries = computed(() => results.value.filter((entry) => isEntryResultPublished(entry)))
const awardedEntries = computed(() => results.value.filter((entry) => isEntryAwarded(entry)))
const isFeedbackOnlySelectedEntry = computed(() => isFeedbackOnlyEntry(selectedEntry.value))
const captainScore = computed(() => resultDetail.value.scores.find((score) => score.finalScore) || null)
const judgeScores = computed(() => resultDetail.value.scores.filter((score) => !score.finalScore))
const finalScore = computed(() => captainScore.value?.consensusScore || captainScore.value?.totalScore || null)
const finalScoreMax = computed(() => scoreMax(captainScore.value) || 50)
const awardTitle = computed(() => selectedEntry.value?.awardName || selectedEntry.value?.roundResult?.awardName || selectedEntry.value?.roundResult?.slotLabel || '')
const resultOutcome = computed(() => {
  if (isFeedbackOnlySelectedEntry.value) return '诊断完成'
  return awardTitle.value || '未获奖'
})
const resultStatsThirdLabel = computed(() => (hasOnlyFeedbackResults.value ? '诊断发布' : '已获奖'))
const resultStatsThirdValue = computed(() => (hasOnlyFeedbackResults.value ? publishedEntries.value.length : awardedEntries.value.length))
const resultHeroText = computed(() => {
  if (!results.value.length) return '提交报名后，比赛结果会在发布后显示'
  if (hasOnlyFeedbackResults.value) return `已发布 ${publishedEntries.value.length} 款，待发布 ${results.value.length - publishedEntries.value.length} 款`
  return `已发布 ${publishedEntries.value.length} 款，已获奖 ${awardedEntries.value.length} 款，待发布 ${results.value.length - publishedEntries.value.length} 款`
})
const lockedResultHint = computed(() => (
  isFeedbackOnlySelectedEntry.value
    ? '组委会发布诊断结果后，这里会显示评分、评语和桌长综合意见'
    : '组委会确认并发布结果后，这里会显示评分、评语和奖项信息'
))
const categoryEntryCountText = computed(() => {
  const count = Number(selectedEntry.value?.categoryEntryCount || 0)
  return count ? `${count} 款参赛酒` : '待确认'
})
const finalScoreText = computed(() => {
  if (finalScore.value === null || finalScore.value === undefined || finalScore.value === '') return '待确认'
  return `${formatScore(finalScore.value)} / ${formatScore(finalScoreMax.value)}`
})
const certificateStatusText = computed(() => {
  if (!isEntryAwarded(selectedEntry.value)) return '无获奖证书'
  return selectedEntry.value?.certificateAvailable ? '下载开放' : '暂未开放'
})
const filteredResults = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return results.value.filter((entry) => {
    const text = `${entry.entryName || ''} ${entry.competitionName || ''} ${entry.categoryName || ''} ${entry.style || ''} ${entry.entryId || ''}`.toLowerCase()
    const hitWord = !word || text.includes(word)
    const published = isEntryResultPublished(entry)
    const awarded = isEntryAwarded(entry)
    const hitStatus = statusFilter.value === 'all'
      || (statusFilter.value === 'published' && published)
      || (statusFilter.value === 'locked' && !published)
      || (statusFilter.value === 'awarded' && awarded)
    return hitWord && hitStatus
  })
})
const groupedResults = computed(() => {
  const groups = new Map()
  filteredResults.value.forEach((entry) => {
    const key = entry.competitionId || entry.competitionName || 'unknown'
    if (!groups.has(key)) {
      groups.set(key, {
        competitionId: entry.competitionId,
        competitionName: entry.competitionName || '赛事信息待确认',
        entries: [],
      })
    }
    groups.get(key).entries.push(entry)
  })
  return Array.from(groups.values())
})

onMounted(async () => {
  results.value = await fetchPortalResults()
  selectedId.value = resolveInitialEntryId()
})

watch(selectedId, async (id) => {
  if (!id) {
    resultDetail.value = { summary: null, scores: [], roundResults: [] }
    return
  }
  resultDetail.value = await fetchPortalResultDetail(id)
  router.replace({ path: '/portal/results', query: { entryId: id } })
})

watch(() => route.query, () => {
  const nextId = resolveInitialEntryId()
  if (nextId && nextId !== selectedId.value) {
    selectedId.value = nextId
  }
})

watch(hasOnlyFeedbackResults, (onlyFeedback) => {
  if (onlyFeedback && statusFilter.value === 'awarded') {
    statusFilter.value = 'all'
  }
})

function resolveInitialEntryId() {
  const queryEntryId = Number(route.query.entryId || 0)
  if (queryEntryId && results.value.some((entry) => entry.entryId === queryEntryId)) {
    return queryEntryId
  }
  const queryCompetitionId = Number(route.query.competitionId || 0)
  if (queryCompetitionId) {
    const competitionEntry = results.value.find((entry) => entry.competitionId === queryCompetitionId && isEntryResultPublished(entry))
      || results.value.find((entry) => entry.competitionId === queryCompetitionId)
    if (competitionEntry) {
      return competitionEntry.entryId
    }
  }
  return publishedEntries.value[0]?.entryId || results.value[0]?.entryId || null
}

function selectEntry(entryId) {
  selectedId.value = entryId
}

function resultStatusLabel(entry) {
  if (isFeedbackOnlyEntry(entry)) {
    if (isEntryResultPublished(entry)) return '诊断已发布'
    if (entry.status === 'STORED') return '待发布'
    return entryStatusMeta[entry.status]?.label || '待发布'
  }
  if (isEntryAwarded(entry)) return '已获奖'
  if (isEntryResultPublished(entry)) return '未获奖'
  if (entry.status === 'STORED') return '待发布'
  return entryStatusMeta[entry.status]?.label || '待发布'
}

function resultTone(entry) {
  if (isFeedbackOnlyEntry(entry)) {
    if (isEntryResultPublished(entry)) return 'green'
    if (entry.status === 'STORED') return 'blue'
    return entryStatusMeta[entry.status]?.tone || 'muted'
  }
  if (isEntryAwarded(entry)) return 'gold'
  if (isEntryResultPublished(entry)) return 'green'
  if (entry.status === 'STORED') return 'blue'
  return entryStatusMeta[entry.status]?.tone || 'muted'
}

function resultBrief(entry) {
  if (isFeedbackOnlyEntry(entry)) {
    if (isEntryResultPublished(entry)) return '评分和诊断已开放'
    return entry.lockReason || '等待组委会发布诊断结果'
  }
  if (isEntryAwarded(entry)) return entry.awardName || entry.roundResult?.awardName || '获奖结果已发布'
  if (isEntryResultPublished(entry)) return '评分和评语已开放'
  return entry.lockReason || '等待组委会发布结果'
}

function resultTypeLabel(type) {
  const labels = {
    EVALUATED: '诊断完成',
    ADVANCE: '晋级',
    RANK: '排序',
    MEDAL: '组别奖项',
    CHAMPION: '总冠军',
  }
  return labels[type] || '晋级与奖项'
}

function isFeedbackOnlyEntry(entry) {
  return entry?.competitionType === 'FEEDBACK_ONLY'
}

function scoreRoleLabel(role) {
  const labels = {
    CROSS: '跨界评审',
    PROFESSIONAL: '专业评审',
    CAPTAIN: '桌长',
  }
  return labels[role] || '评审'
}

function judgeDisplayName(score) {
  const roleLabel = scoreRoleLabel(score.judgeRoleType)
  if (!score.judgeLabel || /#\s*\d+/.test(score.judgeLabel) || /^评审\s*\d+$/i.test(score.judgeLabel)) return roleLabel
  return score.judgeLabel
}

function scoreMax(score) {
  if (!score?.dimensions?.length) return null
  return score.dimensions.reduce((sum, dimension) => sum + Number(dimension.maxScore || 0), 0) || null
}

function scoreWithMax(score, consensus = false) {
  const value = consensus ? (score?.consensusScore || score?.totalScore) : score?.totalScore
  if (value === null || value === undefined || value === '') return '待确认'
  const max = scoreMax(score)
  return max ? `${formatScore(value)} / ${formatScore(max)}` : formatScore(value)
}

function dimensionScoreText(dimension) {
  if (dimension?.score === null || dimension?.score === undefined || dimension?.score === '') return '-'
  if (dimension.maxScore !== null && dimension.maxScore !== undefined && dimension.maxScore !== '') {
    return `${formatScore(dimension.score)} / ${formatScore(dimension.maxScore)}`
  }
  return formatScore(dimension.score)
}

function formatScore(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return value
  return Number.isInteger(number) ? String(number) : number.toFixed(1)
}

function readableComment(value, fallback) {
  const normalized = String(value || '').trim()
  if (!normalized) return fallback
  return normalized
}

async function downloadCertificate() {
  if (!selectedEntry.value?.entryId || !selectedEntry.value?.certificateAvailable) return
  try {
    const blob = await downloadPortalResultCertificate(selectedEntry.value.entryId)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = selectedEntry.value.certificateFilename || `${selectedEntry.value.entryName || '证书'}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('证书暂未开放下载')
  }
}
</script>

<style scoped>
.results-page {
  display: grid;
  gap: 18px;
}

.result-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: end;
  padding: 22px 24px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(34, 25, 17, 0.96), rgba(92, 55, 18, 0.9)),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 16px);
  border-radius: 8px;
}

.result-hero h1 {
  margin: 12px 0 6px;
  font-size: 34px;
  line-height: 1.1;
}

.result-hero p {
  margin: 0;
  color: #ead9b7;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(82px, 1fr));
  gap: 10px;
}

.result-stats span {
  min-height: 70px;
  padding: 12px;
  background: rgba(255, 250, 240, 0.12);
  border: 1px solid rgba(255, 250, 240, 0.18);
  border-radius: 8px;
}

.result-stats small,
.result-stats b {
  display: block;
}

.result-stats small {
  color: #ead9b7;
}

.result-stats b {
  margin-top: 5px;
  font-size: 27px;
}

.result-workbench {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.result-sidebar,
.result-summary-card,
.captain-card,
.feedback-card,
.round-result-card,
.locked-card {
  padding: 22px;
}

.result-sidebar {
  position: sticky;
  top: 92px;
  max-height: calc(100vh - 116px);
  overflow: auto;
  scrollbar-color: rgba(87, 58, 26, 0.24) rgba(255, 250, 240, 0.5);
  scrollbar-width: thin;
}

.result-sidebar::-webkit-scrollbar {
  width: 6px;
}

.result-sidebar::-webkit-scrollbar-track {
  background: rgba(255, 250, 240, 0.5);
  border-radius: 999px;
}

.result-sidebar::-webkit-scrollbar-thumb {
  background: rgba(87, 58, 26, 0.24);
  border-radius: 999px;
}

.result-sidebar::-webkit-scrollbar-thumb:hover {
  background: rgba(87, 58, 26, 0.34);
}

.filter-panel {
  display: grid;
  gap: 12px;
}

.filter-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}

.filter-tabs button {
  min-height: 34px;
  padding: 0 8px;
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  font-weight: 800;
}

.filter-tabs button.active {
  color: #fff6df;
  background: #2b1d10;
  border-color: #2b1d10;
}

.competition-groups {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.competition-group {
  display: grid;
  gap: 8px;
}

.group-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #6b4710;
}

.group-title strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-title small {
  color: #746a5f;
  white-space: nowrap;
}

.result-entry {
  display: grid;
  justify-items: start;
  gap: 7px;
  width: 100%;
  padding: 14px;
  text-align: left;
  color: #2b1d10;
  background: #fff8ec;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.result-entry.active {
  background: #fffdf7;
  border-color: rgba(184, 117, 23, 0.38);
  box-shadow: inset 3px 0 0 #3d7d50;
}

.result-entry strong {
  max-width: 100%;
  overflow-wrap: anywhere;
  font-size: 17px;
  line-height: 1.35;
}

.result-entry small,
.result-entry em,
.side-empty p,
.locked-card p,
.captain-card p,
.judge-comment,
.dimension-row em {
  color: #746a5f;
  line-height: 1.65;
}

.result-entry em {
  font-style: normal;
}

.result-detail {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.result-summary-card {
  display: grid;
  gap: 18px;
}

.summary-title {
  display: grid;
  justify-items: start;
  gap: 8px;
}

.summary-title h2 {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: 34px;
  line-height: 1.18;
}

.summary-title p {
  margin: 0;
  color: #746a5f;
}

.result-overview-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: start;
}

.result-outcome-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.25fr) repeat(2, minmax(170px, 0.75fr));
  gap: 12px;
}

.result-outcome-grid.feedback-only {
  grid-template-columns: minmax(220px, 1.25fr) minmax(170px, 0.75fr);
}

.result-outcome-grid section {
  min-width: 0;
  min-height: 112px;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.result-outcome-grid .outcome-card {
  background:
    linear-gradient(180deg, rgba(255, 248, 226, 0.96), rgba(246, 217, 150, 0.78));
  border-color: rgba(155, 99, 23, 0.22);
}

.result-outcome-grid small,
.result-outcome-grid strong {
  display: block;
}

.result-outcome-grid small {
  color: #746a5f;
  font-size: 13px;
  font-weight: 800;
}

.result-outcome-grid strong {
  margin-top: 10px;
  overflow-wrap: anywhere;
  color: #2b1d10;
  font-size: 30px;
  line-height: 1.18;
}

.result-outcome-grid section:not(.outcome-card) strong {
  font-size: 24px;
}

.entry-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.entry-meta div,
.locked-meta div {
  min-width: 0;
  padding: 12px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.entry-meta dt {
  margin: 0 0 7px;
  color: #746a5f;
  font-size: 13px;
}

.entry-meta dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: #2b1d10;
  font-weight: 800;
  line-height: 1.4;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 270px;
}

.primary-action,
.secondary-action,
.empty-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.primary-action {
  color: #fff6df;
  background: #2b1d10;
  border: 1px solid rgba(43, 29, 16, 0.2);
}

.primary-action:disabled {
  cursor: not-allowed;
  color: #877968;
  background: #eadbc1;
}

.secondary-action,
.empty-actions a + a {
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.section-heading .portal-section-title {
  margin: 0;
}

.section-heading > span {
  flex: 0 0 auto;
  color: #6b4710;
  font-weight: 800;
}

.captain-card p {
  margin: 0;
  overflow-wrap: anywhere;
  font-size: 17px;
}

.feedback-list {
  display: grid;
  gap: 14px;
}

.feedback-row {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: #fff8ec;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.feedback-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.feedback-head div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.feedback-head strong {
  overflow-wrap: anywhere;
  font-size: 18px;
}

.feedback-head small {
  color: #746a5f;
}

.feedback-head > span {
  flex: 0 0 auto;
  padding: 8px 12px;
  color: #2b1d10;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
  font-size: 18px;
  font-weight: 900;
}

.judge-comment {
  margin: 0;
  overflow-wrap: anywhere;
}

.dimension-table {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
}

.dimension-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 12px;
  align-items: start;
  min-height: 70px;
  padding: 12px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.08);
  border-radius: 8px;
}

.dimension-row span {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #2b1d10;
  font-weight: 800;
}

.dimension-row strong {
  color: #2b1d10;
  font-size: 18px;
  white-space: nowrap;
}

.dimension-row em {
  grid-column: 1 / -1;
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: 13px;
  font-style: normal;
}

.empty-feedback,
.side-empty {
  margin: 0;
  padding: 18px;
  color: #746a5f;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.18);
  border-radius: 8px;
}

.round-result-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.round-result-list span {
  display: grid;
  gap: 6px;
  min-height: 92px;
  padding: 14px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.round-result-list small,
.round-result-list em {
  color: #746a5f;
  font-style: normal;
}

.round-result-list strong {
  overflow-wrap: anywhere;
  color: #2b1d10;
  font-size: 18px;
}

.locked-card {
  max-width: none;
}

.locked-card h2 {
  margin: 18px 0 10px;
  overflow-wrap: anywhere;
  font-size: 30px;
}

.locked-meta {
  margin-top: 20px;
}

.locked-progress {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 24px;
}

.locked-progress span {
  min-height: 46px;
  padding: 12px;
  color: #7d705f;
  text-align: center;
  background: #f0e2c8;
  border-radius: 8px;
}

.locked-progress span.done {
  color: #fff6df;
  background: #3d7d50;
}

.empty-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.empty-actions a {
  color: #2b1d10;
  background: #e1a23d;
}

@media (max-width: 1120px) {
  .result-workbench,
  .result-overview-head,
  .result-outcome-grid,
  .round-result-list {
    grid-template-columns: 1fr;
  }

  .result-sidebar {
    position: static;
    max-height: none;
  }

  .result-actions {
    justify-content: flex-start;
    min-width: 0;
  }
}

@media (max-width: 720px) {
  .result-hero,
  .result-stats,
  .entry-meta,
  .filter-tabs,
  .locked-progress {
    grid-template-columns: 1fr;
  }

  .result-hero h1,
  .summary-title h2 {
    font-size: 30px;
  }

  .feedback-head,
  .section-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .feedback-head > span {
    align-self: flex-start;
  }

  .dimension-row {
    grid-template-columns: 1fr;
  }
}
</style>
