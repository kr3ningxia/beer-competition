<template>
  <div class="results-page">
    <section class="result-hero">
      <div>
        <span class="label-chip tone-gold">结果中心</span>
        <h1>我的结果</h1>
        <p>查看已发布酒款的评分、评语、奖项和奖状。</p>
      </div>
      <div class="result-stats">
        <span><small>已提交</small><b>{{ results.length }}</b></span>
        <span><small>可查看</small><b>{{ publishedEntries.length }}</b></span>
        <span><small>已获奖</small><b>{{ awardedEntries.length }}</b></span>
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
              <small>{{ entry.categoryName || '未关联组别' }} · {{ entry.style || '未填写风格' }}</small>
              <em>{{ resultBrief(entry) }}</em>
            </button>
          </section>
        </div>

        <div v-else class="side-empty">
          <strong>没有匹配的酒款</strong>
          <p>换一个关键词或筛选条件。</p>
        </div>
      </aside>

      <main class="result-detail">
        <template v-if="selectedEntry?.published">
          <section class="overview-grid">
            <article class="award-card">
              <span>{{ selectedEntry.competitionName }}</span>
              <h2>{{ awardTitle }}</h2>
              <strong>{{ selectedEntry.entryName }}</strong>
              <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }}</p>
              <p v-if="selectedEntry.categoryEntryCount">本组别共 {{ selectedEntry.categoryEntryCount }} 款参赛酒</p>
              <button
                v-if="selectedEntry.certificateAvailable"
                class="certificate-button"
                type="button"
                @click="downloadCertificate"
              >
                下载 PDF 奖状
              </button>
            </article>

            <article class="overview-card brewer-card">
              <h2 class="portal-section-title">结果概览</h2>
              <div class="overview-facts">
                <span><small>共识分</small><b>{{ finalScore || '-' }}</b></span>
                <span><small>奖项</small><b>{{ awardTitle }}</b></span>
                <span><small>晋级状态</small><b>{{ advanceStatusLabel || '未晋级' }}</b></span>
                <span><small>奖状</small><b>{{ selectedEntry.certificateAvailable ? '可下载' : '暂未开放' }}</b></span>
              </div>
            </article>
          </section>

          <section class="captain-card brewer-card">
            <h2 class="portal-section-title">桌长总结</h2>
            <p>{{ captainScore?.comments || '主办方暂未发布桌长总结' }}</p>
          </section>

          <section class="feedback-card brewer-card">
            <h2 class="portal-section-title">评审反馈</h2>
            <div v-if="judgeScores.length" class="feedback-list">
              <article v-for="score in judgeScores" :key="score.judgeLabel" class="feedback-row">
                <div class="judge-comment">
                  <strong>{{ score.judgeLabel }}</strong>
                  <p>{{ score.comments || '暂无评语' }}</p>
                </div>
                <div class="score-grid">
                  <span v-for="dimension in score.dimensions" :key="dimension.key || dimension.label">
                    <small>{{ dimension.label || dimension.key }}</small>
                    <b>{{ dimension.score ?? '-' }}</b>
                    <em v-if="dimension.note">{{ dimension.note }}</em>
                  </span>
                  <span><small>总分</small><b>{{ score.totalScore ?? '-' }}</b></span>
                </div>
              </article>
            </div>
            <p v-else class="empty-feedback">主办方暂未发布评审反馈。</p>
          </section>

          <section v-if="resultDetail.roundResults.length" class="round-result-card brewer-card">
            <h2 class="portal-section-title">晋级与奖项记录</h2>
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
          <h2>结果暂未发布</h2>
          <p>主办方确认并发布结果后，这里会显示评分、评语和奖项信息。</p>
          <div class="locked-progress">
            <span class="done">提交资料</span>
            <span :class="{ done: selectedEntry.status !== 'PENDING_PAYMENT' }">付款确认</span>
            <span :class="{ done: selectedEntry.status === 'STORED' || selectedEntry.status === 'RESULT_PUBLISHED' }">酒样入库</span>
            <span :class="{ done: selectedEntry.status === 'RESULT_PUBLISHED' }">评审中</span>
            <span>结果发布</span>
          </div>
          <div class="empty-actions">
            <RouterLink to="/portal/my">返回我的参赛</RouterLink>
            <RouterLink to="/portal/entries">查看酒款资料</RouterLink>
          </div>
        </section>

        <section v-else class="locked-card brewer-card">
          <h2>当前暂无已发布结果</h2>
          <p>选择左侧酒款，可以查看结果状态和发布后的反馈。</p>
          <div class="empty-actions">
            <RouterLink to="/portal/my">查看我的参赛</RouterLink>
            <RouterLink to="/portal/events">浏览开放赛事</RouterLink>
          </div>
        </section>
      </main>
    </section>

    <section v-else class="locked-card brewer-card">
      <h2>还没有参赛酒款</h2>
      <p>提交报名后，比赛结果会在发布后显示。</p>
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

const filterOptions = [
  { label: '全部', value: 'all' },
  { label: '可查看', value: 'published' },
  { label: '待发布', value: 'locked' },
  { label: '已获奖', value: 'awarded' },
]

const selectedEntry = computed(() => resultDetail.value.summary || results.value.find((entry) => entry.entryId === selectedId.value))
const publishedEntries = computed(() => results.value.filter((entry) => isEntryResultPublished(entry)))
const awardedEntries = computed(() => results.value.filter((entry) => isEntryAwarded(entry)))
const captainScore = computed(() => resultDetail.value.scores.find((score) => score.finalScore) || null)
const judgeScores = computed(() => resultDetail.value.scores.filter((score) => !score.finalScore))
const finalScore = computed(() => captainScore.value?.consensusScore || captainScore.value?.totalScore || null)
const awardTitle = computed(() => selectedEntry.value?.awardName || selectedEntry.value?.roundResult?.awardName || selectedEntry.value?.roundResult?.slotLabel || '结果已发布')
const advanceStatusLabel = computed(() => {
  if (!resultDetail.value.roundResults.length) return ''
  const advancedTypes = new Set(['ADVANCE', 'RANK', 'MEDAL', 'CHAMPION'])
  return resultDetail.value.roundResults.some((item) => advancedTypes.has(item.resultType)) ? '已晋级' : ''
})
const filteredResults = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return results.value.filter((entry) => {
    const text = `${entry.entryName || ''} ${entry.competitionName || ''} ${entry.categoryName || ''} ${entry.style || ''} ${entry.entryId || ''}`.toLowerCase()
    const hitWord = !word || text.includes(word)
    const hitStatus = statusFilter.value === 'all'
      || (statusFilter.value === 'published' && isEntryResultPublished(entry))
      || (statusFilter.value === 'locked' && !isEntryResultPublished(entry))
      || (statusFilter.value === 'awarded' && isEntryAwarded(entry))
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
        competitionName: entry.competitionName || '未关联赛事',
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
  if (isEntryAwarded(entry)) return '已获奖'
  if (isEntryResultPublished(entry)) return '结果已发布'
  if (entry.status === 'STORED') return '结果待发布'
  return entryStatusMeta[entry.status]?.label || '结果待发布'
}

function resultTone(entry) {
  if (isEntryAwarded(entry) || isEntryResultPublished(entry)) return 'gold'
  if (entry.status === 'STORED') return 'blue'
  return entryStatusMeta[entry.status]?.tone || 'muted'
}

function resultBrief(entry) {
  if (isEntryAwarded(entry)) return entry.awardName || entry.roundResult?.awardName || '已获奖'
  if (isEntryResultPublished(entry)) return '评分和评语可查看'
  return entry.lockReason || '等待主办方发布结果'
}

function resultTypeLabel(type) {
  const labels = {
    ADVANCE: '晋级',
    RANK: '排序',
    MEDAL: '组别奖项',
    CHAMPION: '总冠军',
  }
  return labels[type] || '晋级与奖项'
}

async function downloadCertificate() {
  if (!selectedEntry.value?.entryId) return
  try {
    const blob = await downloadPortalResultCertificate(selectedEntry.value.entryId)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = selectedEntry.value.certificateFilename || `${selectedEntry.value.entryName || '奖状'}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('奖状暂未开放下载')
  }
}
</script>

<style scoped>
.results-page {
  display: grid;
  gap: 22px;
}

.result-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: end;
  padding: 28px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(32, 23, 15, 0.96), rgba(84, 48, 17, 0.9)),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 16px);
  border-radius: 8px;
}

.result-hero h1 {
  margin: 16px 0 8px;
  font-size: 44px;
  line-height: 1.05;
}

.result-hero p {
  margin: 0;
  color: #ead9b7;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(86px, 1fr));
  gap: 10px;
}

.result-stats span,
.overview-facts span {
  min-height: 72px;
  padding: 12px;
  background: rgba(255, 250, 240, 0.12);
  border: 1px solid rgba(255, 250, 240, 0.18);
  border-radius: 8px;
}

.result-stats small,
.result-stats b,
.overview-facts small,
.overview-facts b {
  display: block;
}

.result-stats small {
  color: #ead9b7;
}

.result-stats b {
  margin-top: 6px;
  font-size: 28px;
}

.result-workbench {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.result-sidebar,
.overview-card,
.captain-card,
.feedback-card,
.round-result-card,
.locked-card {
  padding: 22px;
}

.result-sidebar {
  position: sticky;
  top: 92px;
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
  background: #fff7e6;
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
.feedback-row p,
.captain-card p {
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

.overview-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 18px;
}

.award-card {
  position: relative;
  min-height: 340px;
  padding: 26px;
  overflow: hidden;
  color: #2b1d10;
  text-align: center;
  background:
    radial-gradient(circle at 50% 20%, rgba(255, 255, 255, 0.6), transparent 80px),
    linear-gradient(180deg, #f5db83, #c88222);
  border: 1px solid rgba(87, 58, 26, 0.18);
  border-radius: 8px;
  box-shadow: 0 22px 48px rgba(83, 51, 17, 0.15);
}

.award-card::before {
  position: absolute;
  inset: 18px;
  border: 2px solid rgba(43, 29, 16, 0.18);
  border-radius: 8px;
  content: "";
}

.award-card > * {
  position: relative;
  z-index: 1;
}

.award-card span {
  font-weight: 900;
  letter-spacing: 0.08em;
}

.award-card h2 {
  margin: 48px 0 16px;
  overflow-wrap: anywhere;
  font-size: 46px;
  line-height: 1.05;
}

.award-card strong {
  display: block;
  overflow-wrap: anywhere;
  font-size: 21px;
  line-height: 1.35;
}

.award-card p {
  color: #6b4710;
  line-height: 1.6;
}

.certificate-button {
  min-height: 38px;
  margin-top: 12px;
  padding: 0 16px;
  color: #fff6df;
  background: #2b1d10;
  border: 1px solid rgba(43, 29, 16, 0.2);
  border-radius: 8px;
  font-weight: 900;
}

.overview-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.overview-facts span {
  background: #fff7e6;
  border-color: rgba(87, 58, 26, 0.1);
}

.overview-facts small {
  color: #746a5f;
}

.overview-facts b {
  margin-top: 8px;
  overflow-wrap: anywhere;
  font-size: 22px;
  line-height: 1.25;
}

.captain-card p {
  margin: 0;
  font-size: 17px;
}

.feedback-list {
  display: grid;
  gap: 14px;
}

.feedback-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 460px;
  gap: 18px;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.judge-comment strong {
  font-size: 17px;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.score-grid span {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 4px;
  min-height: 74px;
  padding: 10px;
  text-align: center;
  background: #fffdf7;
  border-radius: 8px;
}

.score-grid small {
  color: #746a5f;
}

.score-grid b {
  font-size: 22px;
}

.score-grid em {
  max-width: 100%;
  color: #746a5f;
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
  overflow-wrap: anywhere;
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
  font-size: 30px;
}

.locked-progress {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 14px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.empty-actions a + a {
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

@media (max-width: 1120px) {
  .result-workbench,
  .overview-grid,
  .feedback-row,
  .round-result-list {
    grid-template-columns: 1fr;
  }

  .result-sidebar {
    position: static;
  }
}

@media (max-width: 720px) {
  .result-hero,
  .result-stats,
  .overview-facts,
  .score-grid,
  .locked-progress {
    grid-template-columns: 1fr;
  }

  .filter-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .result-hero h1 {
    font-size: 34px;
  }
}
</style>
