<template>
  <main class="app-shell score-shell">
    <section class="score-top">
      <button class="back-link" type="button" @click="$router.push(`/scan-result/${uuid}`)">
        <span class="back-icon" aria-hidden="true"></span>
        <span>返回酒款</span>
      </button>
      <div class="score-meta-strip">
        <div class="code-panel">
          <span>编号</span>
          <strong>{{ displayPlainShortCode(entry) }}</strong>
        </div>
        <div>
          <span>备注字数</span>
          <strong class="sub-score">{{ notesLength }} / {{ minNoteLength }}</strong>
        </div>
      </div>
    </section>

    <div class="score-content">
      <section v-if="entry && config" class="score-form">
        <div class="entry-info">
          <div class="entry-summary">
            <div>
              <span>投递组别：{{ entry.categoryName }}</span>
              <strong>{{ styleDisplayName(entry) }}</strong>
            </div>
            <div class="entry-side">
              <em>编号： <strong>{{ displayPlainShortCode(entry) }}</strong></em>
              <span>ABV {{ entry.abv }}</span>
            </div>
          </div>

          <div v-if="entry.styleCategoryName || entry.styleDescription" class="style-reference">
            <div>
              <span>风格分类</span>
              <strong>{{ entry.styleCategoryName || entry.categoryName }}</strong>
            </div>
            <p v-if="entry.styleDescription">{{ entry.styleDescription }}</p>
          </div>
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
                    v-for="tick in tickValues(item).filter((tick) => tick.label !== '')"
                    :key="tick.value"
                    class="range-label"
                    :class="{ start: tick.value === 0, end: tick.value === Number(item.maxScore) }"
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
    </div>

    <div class="sticky-actions">
      <button class="button primary full" type="button" :disabled="!canSubmit" @click="submit">
        {{ submitButtonText }}
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
const submitting = ref(false)

const form = reactive({
  beerUuid: uuid,
  judgeRoleType: '',
  dimensions: [],
})

const totalScore = computed(() => (
  form.dimensions.reduce((sum, item) => sum + Number(item.score || 0), 0)
))
const notesLength = computed(() => (
  form.dimensions.reduce((sum, item) => sum + String(item.note || '').length, 0)
))

const minNoteLength = computed(() => Number(config.value?.commentMinLength || 0))
const commentReady = computed(() => notesLength.value >= minNoteLength.value)
const remainingNotes = computed(() => Math.max(0, minNoteLength.value - notesLength.value))
const canSubmit = computed(() => (
  !entry.value?.locked
  && !submitting.value
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
  if (submitting.value) return ''
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
const submitButtonText = computed(() => {
  if (entry.value?.locked) return '本桌结果已确认'
  if (submitting.value) return existingScore.value ? '保存中...' : '提交中...'
  return existingScore.value ? '保存修改' : '提交评分'
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
      label: value === 0 || value === maxScore || (maxScore >= 10 && value === Math.round(maxScore / 2)) ? value : '',
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

function displayPlainShortCode(source) {
  return source?.shortCode || '待生成'
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

function buildDimensionPayload(item) {
  return {
    key: item.key,
    label: item.label,
    score: Number(item.score || 0),
    maxScore: item.maxScore,
    notePrompt: item.notePrompt,
  }
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
  if (!canSubmit.value) return
  submitting.value = true
  const payload = {
    ...form,
    totalScore: totalScore.value,
    comments: buildComments(form.dimensions),
    dimensions: form.dimensions.map(buildDimensionPayload),
  }
  try {
    if (existingScore.value) {
      await updateScore(existingScore.value.id, payload)
      message.value = '修改已保存'
    } else {
      await createScore(payload)
      message.value = '评分已提交'
    }
    window.setTimeout(() => router.push('/competitions'), 360)
  } catch (error) {
    submitting.value = false
    throw error
  }
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

</script>

<style scoped>
.score-shell {
  --score-bg: #f4f3f0;
  --score-card: #ffffff;
  --score-ink: #0b1628;
  --score-muted: #5f6b7a;
  --score-border: #dedbd5;
  --score-primary: #c87434;
  --score-header: #3d4a3a;
  width: min(100%, 560px);
  min-height: 0;
  margin: 0 auto;
  padding: 0 0 116px;
  background: var(--score-bg);
  color: var(--score-ink);
}

.score-shell > * + * {
  margin-top: 0;
}

.score-top {
  padding: 22px 20px 28px;
  color: #fff;
  background: var(--score-header);
}

.back-link {
  display: inline-flex;
  gap: 12px;
  align-items: center;
  border: 0;
  margin: 0 0 26px;
  padding: 0;
  color: rgba(255, 255, 255, 0.92);
  background: transparent;
  font-size: 21px;
  font-weight: 800;
  line-height: 1;
}

.back-icon {
  width: 14px;
  height: 14px;
  border: solid currentColor;
  border-width: 0 0 2px 2px;
  transform: rotate(45deg);
}

.score-meta-strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: end;
}

.score-meta-strip div {
  display: grid;
  gap: 8px;
}

.score-meta-strip div:last-child {
  min-width: 112px;
  text-align: right;
}

.score-meta-strip span {
  color: rgba(255, 255, 255, 0.55);
  font-size: 16px;
  font-weight: 650;
}

.score-meta-strip strong {
  color: #fff;
  font-size: 35px;
  line-height: 1;
  font-weight: 900;
  letter-spacing: 0;
}

.score-meta-strip .sub-score {
  font-size: 35px;
}

.score-meta-strip .sub-score::first-letter {
  letter-spacing: 0;
}

.score-meta-strip .sub-score {
  color: rgba(255, 255, 255, 0.68);
}

.score-meta-strip .sub-score {
  white-space: nowrap;
}

.score-meta-strip .sub-score::first-line {
  color: #fff;
}

.score-content {
  background: var(--score-bg);
}

.score-form {
  display: grid;
  gap: 0;
}

.entry-info {
  border-bottom: 1px solid var(--score-border);
  background: #fff;
}

.entry-summary,
.style-reference {
  padding: 16px 20px 14px;
}

.entry-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: end;
  border-bottom: 1px solid var(--score-border);
}

.entry-summary span,
.style-reference span {
  display: block;
  color: #586271;
  font-size: 16px;
  line-height: 1.25;
}

.entry-summary strong {
  display: block;
  margin-top: 8px;
  color: #050b16;
  font-size: 23px;
  line-height: 1.15;
  font-weight: 900;
}

.entry-side {
  display: grid;
  gap: 10px;
  justify-items: end;
  text-align: right;
}

.entry-summary em {
  color: #5c4634;
  font-size: 16px;
  font-style: normal;
  white-space: nowrap;
}

.entry-summary em strong {
  display: inline;
  margin: 0 0 0 4px;
  color: #c87434;
  font-size: inherit;
  line-height: inherit;
  font-weight: 850;
}

.entry-side > span {
  color: #4b5260;
  font-size: 18px;
  white-space: nowrap;
}

.style-reference {
  display: grid;
  gap: 10px;
}

.style-reference div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.style-reference strong {
  color: #050b16;
  text-align: right;
  font-size: 18px;
  font-weight: 850;
}

.style-reference p {
  margin: 0;
  color: #586271;
  line-height: 1.55;
  font-size: 15px;
}

.locked-alert {
  margin: 14px 20px 0;
  border: 1px solid #d0d5dd;
  border-radius: 14px;
  padding: 10px 12px;
  color: #475467;
  background: #f2f4f7;
  font-size: 13px;
  font-weight: 750;
}

.dimension-list {
  display: grid;
  gap: 18px;
  padding: 18px 20px 32px;
}

.dimension-card {
  border: 1px solid var(--score-border);
  border-radius: 18px;
  padding: 18px 16px 20px;
  background: var(--score-card);
  box-shadow: none;
}

.dimension-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 131px;
  gap: 12px;
  align-items: start;
}

.dimension-card h2 {
  margin: 0;
  color: #050b16;
  font-size: 21px;
  line-height: 1.15;
  font-weight: 900;
}

.dimension-card p {
  margin: 6px 0 0;
  color: #4c5a6c;
  font-size: 16px;
  line-height: 1.45;
}

.score-stepper {
  display: grid;
  grid-template-columns: 38px 50px 38px;
  gap: 3px;
  align-items: center;
  justify-content: end;
  width: 131px;
}

.score-stepper button {
  width: 38px;
  min-height: 39px;
  border: 1px solid var(--score-border);
  border-radius: 11px;
  color: #4d4d4d;
  background: #fff;
  font-size: 25px;
  line-height: 1;
  font-weight: 450;
}

.score-input {
  width: 50px;
  min-height: 39px;
  border: 1px solid var(--score-border);
  border-radius: 11px;
  color: #06172d;
  background: #fff;
  text-align: center;
  font-size: 20px;
  font-weight: 900;
  appearance: textfield;
}

.score-input::-webkit-outer-spin-button,
.score-input::-webkit-inner-spin-button {
  margin: 0;
  appearance: none;
}

.range {
  position: relative;
  z-index: 3;
  display: block;
  height: 28px;
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
  height: 6px;
  border-radius: 999px;
  background: transparent;
}

.range::-webkit-slider-thumb {
  width: 19px;
  height: 19px;
  margin-top: -7px;
  border: 0;
  border-radius: 999px;
  appearance: none;
  background: var(--score-primary);
  box-shadow: none;
}

.range::-moz-range-track {
  height: 6px;
  border: 0;
  border-radius: 999px;
  background: transparent;
}

.range::-moz-range-thumb {
  width: 19px;
  height: 19px;
  border: 0;
  border-radius: 999px;
  background: var(--score-primary);
  box-shadow: none;
}

.range-wrap {
  position: relative;
  margin-top: 22px;
  padding: 0 0 23px;
}

.range-rail {
  position: absolute;
  top: 11px;
  right: 0;
  left: 0;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #dfddd8;
  box-shadow: none;
  pointer-events: none;
}

.range-fill {
  position: absolute;
  inset: 0 auto 0 0;
  width: var(--range-progress);
  border-radius: inherit;
  background: var(--score-primary);
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
  display: none;
}

.range-tick.major i {
  display: none;
}

.range-tick.active i {
  display: none;
}

.range-labels {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 14px;
  pointer-events: none;
}

.range-label {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  color: #5f6b7a;
  font-size: 15px;
  font-weight: 500;
  line-height: 1;
}

.range-label.start {
  transform: translateX(0);
}

.range-label.end {
  transform: translateX(-100%);
}

.dimension-note {
  margin-top: 15px;
}

.note-textarea {
  min-height: 78px;
  margin-top: 0;
  border-color: var(--score-border);
  border-radius: 14px;
  padding: 16px;
  color: #172033;
  background: #fbfbfb;
  font-size: 17px;
  line-height: 1.45;
  resize: none;
}

.note-textarea::placeholder {
  color: #9a9aa0;
  font-weight: 500;
}

.note-summary {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  margin: 0 20px 16px;
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
  position: fixed;
  right: max(0px, calc((100vw - 560px) / 2));
  bottom: 0;
  left: max(0px, calc((100vw - 560px) / 2));
  z-index: 20;
  display: grid;
  gap: 9px;
  width: min(100%, 560px);
  margin: 0 auto;
  padding: 16px 20px calc(16px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--score-border);
  background: #fff;
  box-shadow: none;
}

.sticky-actions .button {
  min-height: 70px;
  border-radius: 18px;
  font-size: 22px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(200, 116, 52, 0.26);
}

.sticky-actions .button.primary {
  color: #fff;
  background: #e6ba95;
}

.sticky-actions .button.primary:not(:disabled) {
  background: #c87434;
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
  color: var(--score-primary);
  text-align: center;
  font-size: 16px;
  font-weight: 500;
}

@media (max-width: 380px) {
  .score-top {
    padding-right: 16px;
    padding-left: 16px;
  }

  .entry-summary,
  .style-reference,
  .dimension-list {
    padding-right: 14px;
    padding-left: 14px;
  }

  .dimension-head {
    grid-template-columns: minmax(0, 1fr);
  }

  .score-stepper {
    justify-content: start;
  }

  .sticky-actions {
    padding-right: 14px;
    padding-left: 14px;
  }
}
</style>
