<template>
  <div class="results-page">
    <section class="result-hero">
      <div>
        <span class="label-chip tone-gold">RESULTS</span>
        <h2>结果反馈</h2>
        <p>结果发布后，厂商可查看奖项、桌长综合评语和评审原始反馈。</p>
      </div>
      <el-select v-model="selectedId">
        <el-option v-for="entry in results" :key="entry.entryId" :label="entry.entryName" :value="entry.entryId" />
      </el-select>
    </section>

    <template v-if="selectedEntry?.published">
      <section class="published-grid">
        <article class="award-card">
          <span>{{ selectedEntry.competitionName }}</span>
          <h3>{{ selectedEntry.awardName || selectedEntry.roundResult?.awardName || selectedEntry.roundResult?.slotLabel || selectedEntry.roundResult?.resultType || '结果已发布' }}</h3>
          <strong>{{ selectedEntry.entryName }}</strong>
          <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }}</p>
          <p v-if="advanceStatusLabel">{{ advanceStatusLabel }}</p>
          <p v-if="selectedEntry.categoryEntryCount">本组别共 {{ selectedEntry.categoryEntryCount }} 款参赛酒</p>
          <div class="score-medallion">
            <small>共识分</small>
            <b>{{ finalScore || '-' }}</b>
          </div>
        </article>

        <article class="captain-card brewer-card">
          <h3>桌长综合评语</h3>
          <p>{{ captainScore?.comments || '暂无桌长评语' }}</p>
        </article>
      </section>

      <section class="feedback-card brewer-card">
        <h3 class="portal-section-title">评审反馈明细</h3>
        <div class="feedback-list">
          <article v-for="score in judgeScores" :key="score.judgeLabel" class="feedback-row">
            <div>
              <strong>{{ score.judgeLabel }}</strong>
              <p>{{ score.comments || '暂无评语' }}</p>
            </div>
            <div class="score-grid">
              <span v-for="dimension in score.dimensions" :key="dimension.key || dimension.label">
                <small>{{ dimension.label || dimension.key }}</small>
                <b>{{ dimension.score ?? '-' }}</b>
                <em v-if="dimension.note">{{ dimension.note }}</em>
              </span>
              <span><small>总分</small><b>{{ score.totalScore }}</b></span>
            </div>
          </article>
          <p v-if="!judgeScores.length" class="empty-feedback">暂无评审原始反馈</p>
        </div>
      </section>

      <section v-if="resultDetail.roundResults.length" class="round-result-card brewer-card">
        <h3 class="portal-section-title">晋级与奖项记录</h3>
        <div class="round-result-list">
          <span v-for="(roundResult, index) in resultDetail.roundResults" :key="`${roundResult.resultType}-${roundResult.rankNo}-${index}`">
            <small>{{ roundResult.awardType || roundResult.resultType || '轮次结果' }}</small>
            <strong>{{ roundResult.awardName || roundResult.slotLabel || resultTypeLabel(roundResult.resultType) }}</strong>
            <em v-if="roundResult.rankNo">第 {{ roundResult.rankNo }} 名</em>
          </span>
        </div>
      </section>
    </template>

    <section v-else-if="selectedEntry" class="locked-card brewer-card">
      <span :class="['label-chip', `tone-${entryStatusMeta[selectedEntry.status].tone}`]">
        {{ entryStatusMeta[selectedEntry.status].label }}
      </span>
      <h3>{{ selectedEntry.entryName }}</h3>
      <p>该酒款结果暂未发布。比赛结束并由主办方确认结果后，这里会展示评分反馈、桌长评语和奖项信息。</p>
      <div class="locked-progress">
        <span class="done">提交</span>
        <span :class="{ done: selectedEntry.status !== 'PENDING_PAYMENT' }">付款确认</span>
        <span :class="{ done: selectedEntry.status === 'STORED' || selectedEntry.status === 'RESULT_PUBLISHED' }">入库</span>
        <span>评审</span>
        <span>结果发布</span>
      </div>
    </section>
    <section v-else class="locked-card brewer-card">
      <h3>暂无参赛结果</h3>
      <p>提交报名后，比赛结果会在发布后显示。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { fetchPortalResultDetail, fetchPortalResults } from '@/api/portal'
import { entryStatusMeta } from './portalViewModels'

const results = ref([])
const selectedId = ref(null)
const resultDetail = ref({ summary: null, scores: [], roundResults: [] })
const selectedEntry = computed(() => resultDetail.value.summary || results.value.find((entry) => entry.entryId === selectedId.value))
const captainScore = computed(() => resultDetail.value.scores.find((score) => score.finalScore) || null)
const judgeScores = computed(() => resultDetail.value.scores.filter((score) => !score.finalScore))
const finalScore = computed(() => captainScore.value?.consensusScore || captainScore.value?.totalScore || null)
const advanceStatusLabel = computed(() => {
  if (!resultDetail.value.roundResults.length) return ''
  const advancedTypes = new Set(['ADVANCE', 'RANK', 'MEDAL', 'CHAMPION'])
  return resultDetail.value.roundResults.some((item) => advancedTypes.has(item.resultType)) ? '已晋级' : ''
})

function resultTypeLabel(type) {
  const labels = {
    ADVANCE: '晋级',
    RANK: '排序结果',
    MEDAL: '组别奖项',
    CHAMPION: '总冠军',
  }
  return labels[type] || type || '轮次结果'
}

onMounted(async () => {
  results.value = await fetchPortalResults()
  selectedId.value = results.value.find((entry) => entry.published)?.entryId || results.value[0]?.entryId || null
})

watch(selectedId, async (id) => {
  if (!id) {
    resultDetail.value = { summary: null, scores: [], roundResults: [] }
    return
  }
  resultDetail.value = await fetchPortalResultDetail(id)
})
</script>

<style scoped>
.results-page {
  display: grid;
  gap: 22px;
}

.result-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
  padding: 26px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(33, 25, 18, 0.94), rgba(89, 57, 21, 0.9)),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 16px);
  border-radius: 8px;
}

.result-hero h2 {
  margin: 16px 0 8px;
  font-size: 42px;
}

.result-hero p {
  margin: 0;
  color: #d8c8a8;
}

.result-hero .el-select {
  width: 320px;
}

.published-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 20px;
}

.award-card {
  position: relative;
  min-height: 380px;
  padding: 28px;
  overflow: hidden;
  color: #2b1d10;
  text-align: center;
  background:
    radial-gradient(circle at 50% 24%, rgba(255, 255, 255, 0.58), transparent 78px),
    linear-gradient(180deg, #f8df8c, #c68522);
  border: 1px solid rgba(87, 58, 26, 0.18);
  border-radius: 8px;
  box-shadow: 0 22px 48px rgba(83, 51, 17, 0.15);
}

.award-card::before {
  content: "";
  position: absolute;
  inset: 18px;
  border: 2px solid rgba(43, 29, 16, 0.18);
  border-radius: 8px;
}

.award-card > * {
  position: relative;
  z-index: 1;
}

.award-card span {
  font-weight: 900;
  letter-spacing: 0.1em;
}

.award-card h3 {
  margin: 54px 0 16px;
  font-size: 56px;
}

.award-card strong {
  display: block;
  font-size: 22px;
  line-height: 1.3;
}

.award-card p {
  color: #6b4710;
}

.score-medallion {
  display: grid;
  place-items: center;
  width: 112px;
  height: 112px;
  margin: 30px auto 0;
  color: #fff6df;
  background: #2b1d10;
  border-radius: 50%;
}

.score-medallion small,
.score-medallion b {
  display: block;
}

.score-medallion b {
  font-size: 34px;
  line-height: 1;
}

.captain-card,
.feedback-card,
.round-result-card,
.locked-card {
  padding: 24px;
}

.captain-card h3 {
  margin: 0 0 16px;
  font-size: 24px;
}

.captain-card p {
  margin: 0;
  color: #574d41;
  font-size: 18px;
  line-height: 1.85;
}

.result-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.feedback-list {
  display: grid;
  gap: 14px;
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
  color: #2b1d10;
  font-size: 18px;
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

.feedback-row p,
.locked-card p {
  color: #746a5f;
  line-height: 1.7;
}

.empty-feedback {
  margin: 0;
  padding: 18px;
  color: #746a5f;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
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
  min-height: 72px;
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
}

.locked-card {
  max-width: 860px;
}

.locked-card h3 {
  margin: 20px 0 10px;
  font-size: 28px;
}

.locked-progress {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-top: 24px;
}

.locked-progress span {
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

@media (max-width: 1100px) {
  .published-grid,
  .feedback-row,
  .round-result-list {
    grid-template-columns: 1fr;
  }

  .result-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .result-hero .el-select {
    width: 100%;
  }
}

@media (max-width: 680px) {
  .score-grid,
  .locked-progress {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
