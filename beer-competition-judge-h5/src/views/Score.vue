<template>
  <main class="app-shell">
    <section class="top-panel">
      <button class="back-link" type="button" @click="$router.push(`/scan-result/${uuid}`)">返回酒款</button>
      <p class="eyebrow">{{ scoreRoleLabel }}评分表</p>
      <h1 class="page-title">{{ displayShortCode(entry) }}</h1>
      <div class="total-strip">
        <div>
          <span>当前总分</span>
          <strong>{{ totalScore }} / {{ totalMaxScore }}</strong>
        </div>
        <div>
          <span>备注字数</span>
          <strong class="sub-score">{{ notesLength }} / {{ minNoteLength }}</strong>
        </div>
      </div>
      <p class="score-brief">分数需填写整数；备注合计达到要求后可提交。</p>
    </section>

    <section v-if="entry && config" class="card">
      <div class="entry-summary">
        <div>
          <span>{{ entry.categoryName }}</span>
          <strong>{{ styleDisplayName(entry) }} · {{ entry.abv }}</strong>
        </div>
        <em>{{ displayShortCode(entry) }}</em>
      </div>

      <div v-if="entry.styleCategoryName || entry.styleDescription" class="style-reference">
        <div>
          <span>风格分类</span>
          <strong>{{ entry.styleCategoryName || entry.categoryName }}</strong>
        </div>
        <p v-if="entry.styleDescription">{{ entry.styleDescription }}</p>
      </div>

      <div v-if="entry.locked" class="locked-alert">
        桌长已确认本桌结果，此评分不可继续修改。
      </div>

      <div class="dimension-list">
        <article v-for="item in form.dimensions" :key="item.key" class="dimension-card">
          <div class="dimension-head">
            <div>
              <h2>{{ item.label }}（{{ item.maxScore }}分）</h2>
              <p v-if="item.description">{{ item.description }}</p>
            </div>
            <div class="score-stepper" :aria-label="`${item.label}分数`">
              <button type="button" :disabled="entry.locked || Number(item.score || 0) <= 0" @click="adjustScore(item, -1)">-</button>
              <input
                v-model.number="item.score"
                class="score-input"
                type="number"
                inputmode="numeric"
                min="0"
                :max="item.maxScore"
                step="1"
                :disabled="entry.locked"
                @blur="clampScore(item)"
              />
              <button type="button" :disabled="entry.locked || Number(item.score || 0) >= item.maxScore" @click="adjustScore(item, 1)">+</button>
            </div>
          </div>
          <div class="range-wrap" :style="{ '--range-progress': `${rangePercent(item)}%` }">
            <div class="range-rail" aria-hidden="true">
              <span class="range-fill"></span>
              <span
                v-for="tick in tickValues(item)"
                :key="tick.value"
                class="range-tick"
                :class="{ active: Number(item.score || 0) >= tick.value, major: tick.label }"
                :style="{ left: `${tick.percent}%` }"
              >
                <i></i>
              </span>
            </div>
            <input
              v-model.number="item.score"
              class="range"
              type="range"
              min="0"
              :max="item.maxScore"
              step="1"
              :disabled="entry.locked"
              @input="clampScore(item)"
            />
            <div class="range-labels" aria-hidden="true">
              <span
                v-for="tick in tickValues(item).filter((tick) => tick.label)"
                :key="tick.value"
                class="range-label"
                :style="{ left: `${tick.percent}%` }"
              >
                {{ tick.label }}
              </span>
            </div>
          </div>
          <div class="dimension-note">
            <textarea
              v-model="item.note"
              class="textarea note-textarea"
              :disabled="entry.locked"
              :aria-label="`${item.label}备注`"
              :placeholder="item.notePlaceholder || '记录本项依据'"
            />
          </div>
        </article>
      </div>

      <div class="note-summary">
        <p :class="['caption', commentReady ? 'ok-text' : 'warn-text']">
          备注合计 {{ notesLength }} / {{ minNoteLength }} 字
        </p>
        <span v-if="existingScore" class="pill status-warn">已提交过</span>
      </div>
    </section>

    <div class="sticky-actions">
      <button class="button primary full" type="button" :disabled="!canSubmit" @click="submit">
        {{ entry?.locked ? '本桌结果已确认' : existingScore ? '保存修改' : '提交评分' }}
      </button>
      <p v-if="submitHint" class="submit-hint">{{ submitHint }}</p>
      <p v-if="message" class="message">{{ message }}</p>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createScore, fetchEntry, fetchMe, fetchMyScore, fetchScoreConfig, updateScore } from '@/api/judge'

const route = useRoute()
const router = useRouter()
const uuid = String(route.params.uuid || '').toUpperCase()
const me = ref(null)
const entry = ref(null)
const config = ref(null)
const existingScore = ref(null)
const message = ref('')

const form = reactive({
  beerUuid: uuid,
  judgeRoleType: '',
  dimensions: [],
})

const totalScore = computed(() => (
  form.dimensions.reduce((sum, item) => sum + Number(item.score || 0), 0)
))
const totalMaxScore = computed(() => (
  form.dimensions.reduce((sum, item) => sum + Number(item.maxScore || 0), 0)
))
const notesLength = computed(() => (
  form.dimensions.reduce((sum, item) => sum + String(item.note || '').length, 0)
))

const minNoteLength = computed(() => Number(config.value?.commentMinLength || 0))
const commentReady = computed(() => notesLength.value >= minNoteLength.value)
const remainingNotes = computed(() => Math.max(0, minNoteLength.value - notesLength.value))
const canSubmit = computed(() => (
  !entry.value?.locked
  && form.dimensions.length > 0
  && form.dimensions.every((item) => (
    item.score !== ''
    && Number.isInteger(Number(item.score))
    && Number(item.score) >= 0
    && Number(item.score) <= item.maxScore
  ))
  && commentReady.value
))
const submitHint = computed(() => {
  if (entry.value?.locked) return '本桌结果已确认，评分不可修改。'
  if (remainingNotes.value > 0) return `备注还差 ${remainingNotes.value} 字`
  if (!form.dimensions.length) return ''
  const invalid = form.dimensions.some((item) => (
    item.score === ''
    || !Number.isInteger(Number(item.score))
    || Number(item.score) < 0
    || Number(item.score) > item.maxScore
  ))
  return invalid ? '请检查各项分数，必须为整数且不超过满分。' : ''
})

function clampScore(item) {
  const value = Number(item.score || 0)
  item.score = Math.min(Number(item.maxScore), Math.max(0, Math.round(value)))
}

function adjustScore(item, delta) {
  item.score = Number(item.score || 0) + delta
  clampScore(item)
}

function tickValues(item) {
  const maxScore = Number(item.maxScore || 0)
  if (!maxScore) return []

  const values = maxScore <= 5
    ? Array.from({ length: maxScore + 1 }, (_, index) => index)
    : [0, 0.25, 0.5, 0.75, 1].map((ratio) => Math.round(maxScore * ratio))

  return Array.from(new Set(values))
    .filter((value) => value >= 0 && value <= maxScore)
    .sort((a, b) => a - b)
    .map((value) => ({
      value,
      percent: (value / maxScore) * 100,
      label: value === maxScore || (maxScore >= 10 && value === Math.round(maxScore / 2)) ? value : '',
    }))
}

function rangePercent(item) {
  const maxScore = Number(item.maxScore || 0)
  if (!maxScore) return 0
  return Math.min(100, Math.max(0, (Number(item.score || 0) / maxScore) * 100))
}

function styleDisplayName(source) {
  return [source?.styleCode, source?.style].filter(Boolean).join(' ')
}

function displayShortCode(source) {
  return source?.shortCode ? `编号： ${source.shortCode}` : '编号'
}

function parseCommentNotes(comments = '') {
  return String(comments)
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .reduce((map, line) => {
      const match = line.match(/^(.+?)[：:]\s*(.+)$/)
      if (match) map.set(match[1], match[2])
      return map
    }, new Map())
}

function buildComments(dimensions) {
  return dimensions
    .map((item) => ({
      label: item.label,
      note: String(item.note || '').trim(),
    }))
    .filter((item) => item.note)
    .map((item) => `${item.label}：${item.note}`)
    .join('\n')
}

function buildDimensionForm(configDimensions, savedDimensions = [], savedComments = '') {
  const savedByKey = new Map(savedDimensions.map((item) => [item.key, item]))
  const savedByLabel = parseCommentNotes(savedComments)
  return configDimensions.map((item) => {
    const saved = savedByKey.get(item.key) || {}
    return {
      ...item,
      score: Number.isFinite(Number(saved.score)) ? Math.round(Number(saved.score)) : 0,
      note: saved.note || savedByLabel.get(item.label) || '',
    }
  })
}

async function submit() {
  const payload = {
    ...form,
    totalScore: totalScore.value,
    comments: buildComments(form.dimensions),
    dimensions: form.dimensions.map((item) => ({
      ...item,
      score: Number(item.score || 0),
      note: String(item.note || '').trim(),
    })),
  }
  if (existingScore.value) {
    await updateScore(existingScore.value.id, payload)
    message.value = '修改已保存'
  } else {
    await createScore(payload)
    message.value = '评分已提交'
  }
  window.setTimeout(() => router.push('/judged'), 360)
}

onMounted(async () => {
  me.value = await fetchMe()
  entry.value = await fetchEntry(uuid)
  const scoreRole = entry.value?.scoreRoleType || me.value.role
  config.value = await fetchScoreConfig(scoreRole)
  existingScore.value = await fetchMyScore(uuid)
  form.judgeRoleType = scoreRole

  if (existingScore.value) {
    form.dimensions = buildDimensionForm(config.value.dimensions, existingScore.value.dimensions, existingScore.value.comments)
  } else {
    form.dimensions = buildDimensionForm(config.value.dimensions)
  }
})

const scoreRoleLabel = computed(() => (
  entry.value?.scoreRoleType === 'PROFESSIONAL' && me.value?.role === 'CAPTAIN'
    ? '专业评委'
    : config.value?.roleLabel || me.value?.roleLabel
))
</script>

<style scoped>
.back-link {
  border: 0;
  margin: 0 0 10px;
  padding: 0;
  color: rgba(255, 255, 255, 0.74);
  background: transparent;
  font-weight: 750;
}

.top-panel {
  padding: 16px 18px;
}

.page-title {
  font-size: 23px;
}

.total-strip {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(92px, 0.7fr);
  gap: 10px;
  margin-top: 14px;
}

.total-strip div {
  display: grid;
  gap: 5px;
  border-radius: 8px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.1);
}

.total-strip span {
  color: rgba(248, 250, 252, 0.7);
  font-size: 13px;
}

.total-strip strong {
  color: #fff;
  font-size: 23px;
  line-height: 1;
}

.total-strip .sub-score {
  font-size: 18px;
}

.score-brief {
  margin: 10px 0 0;
  color: rgba(248, 250, 252, 0.72);
  font-size: 13px;
  line-height: 1.45;
}

.card {
  padding: 14px;
}

.entry-summary {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: flex-start;
  border-radius: 8px;
  padding: 12px;
  background: #f7f8f6;
}

.entry-summary span {
  display: block;
  color: #667085;
  font-size: 13px;
}

.entry-summary strong {
  display: block;
  margin-top: 5px;
}

.entry-summary em {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 5px 9px;
  color: #92400e;
  background: #fffaeb;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  white-space: nowrap;
}

.style-reference {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
}

.style-reference div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.style-reference span {
  color: #667085;
  font-size: 13px;
}

.style-reference strong {
  color: #18222f;
  font-size: 14px;
}

.style-reference p {
  margin: 0;
  color: #344054;
  line-height: 1.55;
  font-size: 14px;
}

.locked-alert {
  margin-top: 12px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  padding: 10px 12px;
  color: #475467;
  background: #f2f4f7;
  font-size: 13px;
  font-weight: 750;
}

.dimension-list {
  display: grid;
  gap: 9px;
  margin-top: 12px;
}

.dimension-card {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px 14px 14px;
  background: #fff;
}

.dimension-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 116px;
  gap: 12px;
  align-items: start;
}

.dimension-card h2 {
  margin: 0;
  color: #18222f;
  font-size: 16px;
  line-height: 1.25;
}

.dimension-card p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.score-stepper {
  display: grid;
  grid-template-columns: 28px 56px 28px;
  gap: 2px;
  align-items: center;
  justify-content: end;
  width: 116px;
}

.score-stepper button {
  min-height: 42px;
  border: 1px solid #cbd5d1;
  color: #18222f;
  background: #f7f8f6;
  font-size: 18px;
  font-weight: 850;
}

.score-stepper button:first-child {
  border-radius: 8px 0 0 8px;
}

.score-stepper button:last-child {
  border-radius: 0 8px 8px 0;
}

.score-input {
  width: 56px;
  min-height: 42px;
  border: 1px solid #cbd5d1;
  border-radius: 0;
  color: #18222f;
  text-align: center;
  font-size: 17px;
  font-weight: 850;
}

.range {
  position: relative;
  z-index: 3;
  display: block;
  height: 26px;
  width: 100%;
  margin: 0;
  appearance: none;
  background: transparent;
  cursor: pointer;
}

.range:disabled {
  cursor: not-allowed;
}

.range::-webkit-slider-runnable-track {
  height: 10px;
  border-radius: 999px;
  background: transparent;
}

.range::-webkit-slider-thumb {
  width: 22px;
  height: 22px;
  margin-top: -6px;
  border: 3px solid #fff;
  border-radius: 999px;
  appearance: none;
  background: #a75517;
  box-shadow: 0 2px 8px rgba(24, 34, 47, 0.24);
}

.range::-moz-range-track {
  height: 10px;
  border: 0;
  border-radius: 999px;
  background: transparent;
}

.range::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border: 3px solid #fff;
  border-radius: 999px;
  background: #a75517;
  box-shadow: 0 2px 8px rgba(24, 34, 47, 0.24);
}

.range-wrap {
  position: relative;
  margin-top: 10px;
  padding: 0 11px 18px;
}

.range-rail {
  position: absolute;
  top: 8px;
  right: 11px;
  left: 11px;
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #e4e7ec;
  box-shadow: inset 0 0 0 1px rgba(24, 34, 47, 0.14);
  pointer-events: none;
}

.range-fill {
  position: absolute;
  inset: 0 auto 0 0;
  width: var(--range-progress);
  border-radius: inherit;
  background: #a75517;
}

.range-tick {
  position: absolute;
  top: 0;
  z-index: 2;
  display: grid;
  justify-items: center;
  transform: translateX(-50%);
}

.range-tick i {
  width: 1px;
  height: 10px;
  border-radius: 999px;
  background: rgba(102, 112, 133, 0.46);
}

.range-tick.major i {
  width: 2px;
  background: rgba(102, 112, 133, 0.68);
}

.range-tick.active i {
  background: rgba(255, 255, 255, 0.76);
}

.range-labels {
  position: absolute;
  right: 11px;
  bottom: 0;
  left: 11px;
  height: 14px;
  pointer-events: none;
}

.range-label {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  color: #667085;
  font-size: 11px;
  font-weight: 750;
  line-height: 1;
}

.dimension-note {
  margin-top: 10px;
}

.note-textarea {
  min-height: 78px;
  margin-top: 0;
  font-size: 15px;
  line-height: 1.45;
}

.note-textarea::placeholder {
  color: #8a94a6;
  font-weight: 500;
}

.note-summary {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.note-summary .pill {
  justify-self: start;
}

.ok-text {
  color: #067647;
}

.warn-text {
  color: #b54708;
}

.sticky-actions {
  position: static;
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 10px;
  border: 1px solid rgba(24, 34, 47, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(24, 34, 47, 0.12);
}

.message {
  margin: 0;
  color: #067647;
  text-align: center;
  font-size: 14px;
  font-weight: 750;
}

.submit-hint {
  margin: 0;
  color: #b54708;
  text-align: center;
  font-size: 13px;
  font-weight: 750;
}

@media (max-width: 380px) {
  .dimension-head {
    grid-template-columns: minmax(0, 1fr);
  }

  .score-stepper {
    justify-content: start;
  }
}
</style>
