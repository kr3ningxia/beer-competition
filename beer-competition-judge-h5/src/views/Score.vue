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
              <span>ABV {{ formatAbvWithUnit(entry.abv) }}</span>
            </div>
          </div>

          <div v-if="entry.styleCategoryName || entry.styleDescription" class="style-reference">
            <button
              class="style-reference-row"
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
          </div>
        </div>

          <div v-if="entry.locked" class="locked-alert">
            桌长已确认本桌结果，此评分已锁定。
          </div>

          <div class="dimension-list">
            <article v-for="item in form.dimensions" :key="item.key" class="dimension-card">
              <div class="dimension-head">
                <div>
                  <h2>{{ item.label }}（{{ item.maxScore }}分）</h2>
                  <p v-if="item.description">{{ item.description }}</p>
                </div>
                <div class="score-stepper" :aria-label="`${item.label}分数`">
                  <button type="button" :disabled="entry.locked || !hasScore(item) || Number(item.score) <= 1" @click="adjustScore(item, -1)">-</button>
                  <input
                    :value="scoreInputValue(item)"
                    class="score-input"
                    type="number"
                    inputmode="numeric"
                    min="1"
                    :max="item.maxScore"
                    step="1"
                    :disabled="entry.locked"
                    placeholder="未评"
                    @input="setScoreFromInput(item, $event.target.value)"
                    @blur="normalizeScoreInput(item)"
                  />
                  <button type="button" :disabled="entry.locked || Number(item.score || 0) >= item.maxScore" @click="adjustScore(item, 1)">+</button>
                </div>
              </div>
              <div v-if="isSegmentedScale(item)" class="score-options" :aria-label="`${item.label}分数选项`">
                <button
                  v-for="score in scoreOptions(item)"
                  :key="score"
                  type="button"
                  :class="{ active: Number(item.score) === score }"
                  :disabled="entry.locked"
                  @click="setScore(item, score)"
                >
                  {{ score }}
                </button>
              </div>
              <div v-else class="range-wrap" :class="{ unscored: !hasScore(item) }" :style="{ '--range-progress': `${rangePercent(item)}%` }">
                <div class="range-rail" aria-hidden="true">
                  <span class="range-fill"></span>
                  <span
                    v-for="tick in tickValues(item)"
                    :key="tick.value"
                    class="range-tick"
                    :class="{ active: hasScore(item) && Number(item.score) >= tick.value, major: tick.label }"
                    :style="{ left: `${tick.percent}%` }"
                  >
                    <i></i>
                  </span>
                </div>
                <input
                  :value="rangeInputValue(item)"
                  class="range"
                  type="range"
                  min="1"
                  :max="item.maxScore"
                  step="1"
                  :disabled="entry.locked"
                  @input="setScoreFromInput(item, $event.target.value)"
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
                  :placeholder="item.notePlaceholder || item.notePrompt || '记录本项依据'"
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

    <StyleDetailDialog :open="styleDetailOpen" :entry="entry" @close="closeStyleDetail" />
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createScore, fetchEntry, fetchMe, fetchMyScore, fetchScoreConfig, startScore, updateScore } from '@/api/judge'
import StyleDetailDialog from '@/components/StyleDetailDialog.vue'
import { formatAbvWithUnit } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()
const uuid = String(route.params.uuid || '').toUpperCase()
const me = ref(null)
const entry = ref(null)
const config = ref(null)
const existingScore = ref(null)
const message = ref('')
const submitting = ref(false)
const styleDetailOpen = ref(false)

const form = reactive({
  beerUuid: uuid,
  judgeRoleType: '',
  dimensions: [],
})

const totalScore = computed(() => (
  form.dimensions.reduce((sum, item) => sum + Number(item.score || 0), 0)
))
const notesLength = computed(() => (
  form.dimensions.reduce((sum, item) => sum + countEffectiveChars(item.note), 0)
))

const minNoteLength = computed(() => Number(config.value?.commentMinLength ?? config.value?.minCommentLength ?? 0))
const commentReady = computed(() => notesLength.value >= minNoteLength.value)
const remainingNotes = computed(() => Math.max(0, minNoteLength.value - notesLength.value))
const unscoredDimensions = computed(() => form.dimensions.filter((item) => !hasScore(item)))
const zeroScoreDimensions = computed(() => form.dimensions.filter((item) => Number(item.score) === 0))
const canSubmit = computed(() => (
  !entry.value?.locked
  && !submitting.value
  && form.dimensions.length > 0
  && form.dimensions.every(isScoreComplete)
  && commentReady.value
))
const submitHint = computed(() => {
  if (submitting.value) return ''
  if (entry.value?.locked) return '本桌结果已确认，评分不可修改。'
  if (!form.dimensions.length) return ''
  if (unscoredDimensions.value.length) return `还有 ${unscoredDimensions.value.length} 项未评分`
  if (zeroScoreDimensions.value.length) return `${zeroScoreDimensions.value.map((item) => item.label).join('、')}不能为 0 分`
  const invalid = form.dimensions.some((item) => (
    item.score === null
    || item.score === ''
    || !Number.isInteger(Number(item.score))
    || Number(item.score) < 1
    || Number(item.score) > item.maxScore
  ))
  if (invalid) return '请检查各项分数，必须为 1 到满分之间的整数。'
  if (remainingNotes.value > 0) return `备注还差 ${remainingNotes.value} 字`
  return ''
})
const submitButtonText = computed(() => {
  if (entry.value?.locked) return '本桌结果已确认'
  if (submitting.value) return existingScore.value ? '保存中...' : '提交中...'
  return existingScore.value ? '保存修改' : '提交评分'
})

function clampScore(item) {
  if (!hasScore(item)) return
  const value = Number(item.score)
  if (!Number.isFinite(value)) {
    item.score = null
    return
  }
  item.score = Math.min(Number(item.maxScore), Math.max(1, Math.round(value)))
}

function adjustScore(item, delta) {
  item.score = (hasScore(item) ? Number(item.score) : 0) + delta
  clampScore(item)
}

function hasScore(item) {
  return item.score !== null && item.score !== '' && item.score !== undefined
}

function isScoreComplete(item) {
  return hasScore(item)
    && Number.isInteger(Number(item.score))
    && Number(item.score) > 0
    && Number(item.score) <= Number(item.maxScore)
}

function scoreInputValue(item) {
  return hasScore(item) ? item.score : ''
}

function rangeInputValue(item) {
  return hasScore(item) ? item.score : 1
}

function setScore(item, score) {
  item.score = score
  clampScore(item)
}

function setScoreFromInput(item, rawValue) {
  item.score = rawValue === '' ? null : Number(rawValue)
}

function normalizeScoreInput(item) {
  if (!hasScore(item)) return
  const value = Number(item.score)
  if (!Number.isFinite(value)) {
    item.score = null
    return
  }
  const rounded = Math.round(value)
  if (rounded > Number(item.maxScore)) {
    item.score = Number(item.maxScore)
    return
  }
  item.score = Math.max(0, rounded)
}

function isSegmentedScale(item) {
  return Number(item.maxScore || 0) <= 5
}

function scoreOptions(item) {
  const maxScore = Number(item.maxScore || 0)
  return Array.from({ length: maxScore }, (_, index) => index + 1)
}

function tickValues(item) {
  const maxScore = Number(item.maxScore || 0)
  if (!maxScore) return []

  const majorValues = new Set([1, Math.round(maxScore * 0.25), Math.round(maxScore * 0.5), Math.round(maxScore * 0.75), maxScore])
  return Array.from({ length: maxScore }, (_, index) => index + 1)
    .sort((a, b) => a - b)
    .map((value) => ({
      value,
      percent: maxScore === 1 ? 0 : ((value - 1) / (maxScore - 1)) * 100,
      label: majorValues.has(value) ? value : '',
    }))
}

function rangePercent(item) {
  const maxScore = Number(item.maxScore || 0)
  if (!maxScore || !hasScore(item)) return 0
  if (maxScore === 1) return 100
  return Math.min(100, Math.max(0, ((Number(item.score) - 1) / (maxScore - 1)) * 100))
}

function styleDisplayName(source) {
  return [source?.styleCode, source?.style].filter(Boolean).join(' ')
}

function styleCategoryText(source) {
  return source?.styleCategoryName || source?.categoryName || '-'
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
    score: Number(item.score),
    maxScore: item.maxScore,
    notePrompt: item.notePrompt,
    note: String(item.note || '').trim(),
  }
}

function countEffectiveChars(text) {
  return String(text || '').replace(/\s+/g, '').length
}

function openStyleDetail() {
  if (!entry.value) return
  styleDetailOpen.value = true
}

function closeStyleDetail() {
  styleDetailOpen.value = false
}

function buildDimensionForm(configDimensions, savedDimensions = [], savedComments = '') {
  const savedByKey = new Map(savedDimensions.map((item) => [item.key, item]))
  const savedByLabel = parseCommentNotes(savedComments)
  return configDimensions.map((item) => {
    const saved = savedByKey.get(item.key) || {}
    const hasSavedScore = Object.prototype.hasOwnProperty.call(saved, 'score') && saved.score !== null && saved.score !== ''
    return {
      ...item,
      score: hasSavedScore && Number.isFinite(Number(saved.score)) ? Math.round(Number(saved.score)) : null,
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
  config.value = await fetchScoreConfig(scoreRole, entry.value?.competitionId || me.value?.competitionId)
  startScore({
    beerUuid: uuid,
    judgeRoleType: scoreRole,
  }).catch(() => {})
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
  padding: 0;
}

.style-reference-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  border: 0;
  padding: 16px 20px 14px;
  color: inherit;
  background: transparent;
  text-align: left;
  appearance: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.style-reference-row:active {
  background: #fbfaf7;
}

.style-reference strong {
  min-width: 0;
  color: #050b16;
  text-align: right;
  font-size: 18px;
  font-weight: 850;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.style-reference-row svg {
  width: 21px;
  height: 21px;
  stroke: #5d5d57;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
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

.score-input::placeholder {
  color: #98a2b3;
  font-size: 14px;
  font-weight: 750;
}

.score-input::-webkit-outer-spin-button,
.score-input::-webkit-inner-spin-button {
  margin: 0;
  appearance: none;
}

.score-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(52px, 1fr));
  gap: 8px;
  margin-top: 18px;
}

.score-options button {
  min-height: 44px;
  border: 1px solid var(--score-border);
  border-radius: 12px;
  color: #344054;
  background: #fff;
  font-size: 18px;
  font-weight: 850;
}

.score-options button.active {
  border-color: var(--score-primary);
  color: #fff;
  background: var(--score-primary);
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

.range-wrap.unscored .range::-webkit-slider-thumb {
  border: 2px solid #98a2b3;
  background: #fff;
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

.range-wrap.unscored .range::-moz-range-thumb {
  border: 2px solid #98a2b3;
  background: #fff;
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
  display: block;
  width: 1px;
  height: 6px;
  background: rgba(11, 22, 40, 0.18);
}

.range-tick.major i {
  width: 2px;
  background: rgba(11, 22, 40, 0.32);
}

.range-tick.active i {
  background: rgba(255, 255, 255, 0.62);
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
  .style-reference-row,
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
