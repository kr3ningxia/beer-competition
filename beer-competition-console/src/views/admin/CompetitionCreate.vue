<template>
  <div class="competition-create">
    <section class="create-head">
      <button class="back-button" type="button" @click="router.push('/admin/competitions')">
        <Back />
        返回台账
      </button>
      <div>
        <p class="eyebrow">Create Competition</p>
        <h1>新建比赛</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="router.push('/admin/competitions')">取消</button>
        <button class="tool-button primary" type="button" @click="submitDraft">
          <Plus />
          保存草稿
        </button>
      </div>
    </section>

    <section class="step-shell">
      <aside class="step-rail">
        <button
          v-for="(step, index) in steps"
          :key="step.key"
          :class="['step-item', { active: activeStep === index, done: index < activeStep }]"
          type="button"
          @click="activeStep = index"
        >
          <span>{{ index + 1 }}</span>
          <strong>{{ step.title }}</strong>
          <small>{{ step.caption }}</small>
        </button>
      </aside>

      <main class="step-content">
        <section v-if="activeStep === 0" class="form-section">
          <header>
            <h2>基础信息</h2>
            <p>比赛草稿先确定唯一编号、关键日期和报名费，后续配置完成后再开放报名。</p>
          </header>
          <div class="form-grid two">
            <label>
              <span>比赛名称</span>
              <input v-model.trim="draft.name" placeholder="例如 2026 中国精酿啤酒大赛" />
            </label>
            <label>
              <span>比赛编号</span>
              <input v-model.trim="draft.code" placeholder="例如 BC-2026" />
            </label>
            <label>
              <span>届次</span>
              <input v-model.trim="draft.edition" placeholder="例如 第三批次" />
            </label>
            <label>
              <span>比赛日期</span>
              <input v-model="draft.date" type="date" />
            </label>
            <label>
              <span>报名开始时间</span>
              <input v-model="draft.registrationStart" type="datetime-local" />
            </label>
            <label>
              <span>报名截止时间</span>
              <input v-model="draft.registrationDeadline" type="datetime-local" />
            </label>
            <label>
              <span>报名费</span>
              <input v-model.number="draft.entryFee" min="0" type="number" />
            </label>
            <label>
              <span>初始状态</span>
              <select v-model="draft.status" disabled>
                <option value="DRAFT">草稿</option>
              </select>
            </label>
          </div>
        </section>

        <section v-if="activeStep === 1" class="form-section">
          <header>
            <h2>报名配置</h2>
            <p>投递组别和基础风格按比赛独立维护，报名补充信息可控制是否匿名展示给评审。</p>
          </header>
          <div class="config-grid">
            <div class="config-card">
              <div class="card-title">
                <h3>投递组别</h3>
                <button class="icon-button" type="button" @click="draft.categories.push('')"><Plus /></button>
              </div>
              <div class="stack-list">
                <label v-for="(_, index) in draft.categories" :key="`category-${index}`">
                  <input v-model.trim="draft.categories[index]" placeholder="例如 IPA" />
                  <button class="icon-button" type="button" @click="removeItem(draft.categories, index)"><Delete /></button>
                </label>
              </div>
            </div>

            <div class="config-card">
              <div class="card-title">
                <h3>基础风格</h3>
                <button class="icon-button" type="button" @click="draft.baseStyles.push('')"><Plus /></button>
              </div>
              <div class="stack-list">
                <label v-for="(_, index) in draft.baseStyles" :key="`style-${index}`">
                  <input v-model.trim="draft.baseStyles[index]" placeholder="例如 American IPA" />
                  <button class="icon-button" type="button" @click="removeItem(draft.baseStyles, index)"><Delete /></button>
                </label>
              </div>
            </div>
          </div>

          <div class="config-card wide">
            <div class="card-title">
              <h3>报名补充信息</h3>
              <button class="tool-button" type="button" @click="addEntryField">
                <Plus />
                添加字段
              </button>
            </div>
            <div class="field-table">
              <div class="field-row field-head">
                <span>字段 key</span>
                <span>字段名称</span>
                <span>类型</span>
                <span>必填</span>
                <span>评审可见</span>
                <span></span>
              </div>
              <div v-for="(field, index) in draft.entryFields" :key="field.key" class="field-row">
                <input v-model.trim="field.key" placeholder="specialIngredients" />
                <input v-model.trim="field.label" placeholder="例如 增味原料或特殊工艺" />
                <select v-model="field.type">
                  <option value="text">短文本</option>
                  <option value="textarea">长文本</option>
                  <option value="number">数字</option>
                  <option value="select">选项</option>
                </select>
                <label class="switch-line"><input v-model="field.required" type="checkbox" /> 必填</label>
                <label class="switch-line"><input v-model="field.visibleToJudges" type="checkbox" /> 展示</label>
                <button class="icon-button" type="button" @click="removeItem(draft.entryFields, index)"><Delete /></button>
              </div>
            </div>
          </div>
        </section>

        <section v-if="activeStep === 2" class="form-section">
          <header>
            <h2>评审配置</h2>
            <p>每张评审桌必须有且只有 1 名桌长；跨界、专业、桌长评分表总分均固定为 50。</p>
          </header>
          <div class="config-card wide">
            <div class="card-title">
              <h3>评审桌</h3>
              <button class="tool-button" type="button" @click="addJudgeTable">
                <Plus />
                添加评审桌
              </button>
            </div>
            <div class="judge-table">
              <div class="judge-row judge-head">
                <span>桌号</span>
                <span>桌长</span>
                <span>专业评审</span>
                <span>跨界评审</span>
                <span>校验</span>
                <span></span>
              </div>
              <div v-for="(table, index) in draft.judgeTables" :key="table.id" class="judge-row">
                <input v-model.trim="table.name" />
                <input v-model.number="table.captain" min="0" type="number" />
                <input v-model.number="table.professional" min="0" type="number" />
                <input v-model.number="table.cross" min="0" type="number" />
                <strong :class="{ danger: Number(table.captain) !== 1 }">
                  {{ Number(table.captain) === 1 ? '桌长已确认' : '需 1 名桌长' }}
                </strong>
                <button class="icon-button" type="button" @click="removeItem(draft.judgeTables, index)"><Delete /></button>
              </div>
            </div>
          </div>

          <div class="score-grid">
            <article v-for="config in draft.scoreConfigs" :key="config.role" class="config-card">
              <div class="card-title">
                <h3>{{ roleLabels[config.role] }}</h3>
                <span :class="['score-total', { ok: getScoreTotal(config) === 50 }]">{{ getScoreTotal(config) }} / 50</span>
              </div>
              <div class="dimension-list">
                <label v-for="dimension in config.dimensions" :key="dimension.label">
                  <span>{{ dimension.label }}</span>
                  <input v-model.number="dimension.maxScore" min="0" type="number" />
                </label>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeStep === 3" class="form-section">
          <header>
            <h2>检查并创建</h2>
            <p>当前只保存为草稿。开放报名前，需要完成投递组别、基础风格、评审桌和评分表。</p>
          </header>
          <div class="review-layout">
            <article class="review-card">
              <h3>{{ draft.name || '未命名比赛' }}</h3>
              <p>{{ draft.code || '-' }} · {{ draft.edition || '-' }}</p>
              <div class="summary-grid">
                <span>比赛日 <strong>{{ draft.date || '-' }}</strong></span>
                <span>报名截止 <strong>{{ formatDateTime(draft.registrationDeadline) }}</strong></span>
                <span>报名费 <strong>{{ draft.entryFee }} 元 / 款</strong></span>
                <span>评审桌 <strong>{{ draft.judgeTables.length }} 张</strong></span>
              </div>
            </article>
            <article class="review-card">
              <h3>配置检查</h3>
              <div class="check-list">
                <div v-for="item in checkItems" :key="item.key" :class="['check-item', checks[item.key]]">
                  <CircleCheck v-if="checks[item.key] === 'done'" />
                  <Warning v-else />
                  <span>{{ item.label }}</span>
                  <strong>{{ checkLabels[checks[item.key]] }}</strong>
                </div>
              </div>
            </article>
          </div>
        </section>

        <footer class="step-actions">
          <button class="tool-button" type="button" :disabled="activeStep === 0" @click="activeStep -= 1">上一步</button>
          <button v-if="activeStep < steps.length - 1" class="tool-button primary" type="button" @click="activeStep += 1">
            下一步
            <Right />
          </button>
          <button v-else class="tool-button primary" type="button" @click="submitDraft">
            <Plus />
            保存草稿
          </button>
        </footer>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, CircleCheck, Delete, Plus, Right, Warning } from '@element-plus/icons-vue'
import {
  buildChecks,
  checkItems,
  defaultScoreConfigs,
  formatDateTime,
  getScoreTotal,
  roleLabels,
} from './competitionStore'
import {
  createCompetition,
  updateCompetitionCategories,
  updateCompetitionEntryFields,
  updateCompetitionJudgeTables,
  updateCompetitionStyles,
  updateScoreConfigs,
} from '@/api/admin'

const router = useRouter()
const activeStep = ref(0)

const steps = [
  { key: 'base', title: '基础信息', caption: '名称、编号、关键日期' },
  { key: 'entry', title: '报名配置', caption: '组别、风格、补充信息' },
  { key: 'judge', title: '评审配置', caption: '评审桌、评分表' },
  { key: 'review', title: '检查创建', caption: '保存草稿并进入工作台' },
]

const draft = reactive({
  name: '2026 新建精酿啤酒赛',
  code: 'BC-NEW-2026',
  edition: '第一批次',
  date: '2026-08-20',
  registrationStart: '2026-06-10T10:00',
  registrationDeadline: '2026-07-30T18:00',
  entryFee: 199,
  status: 'DRAFT',
  categories: ['IPA', '拉格'],
  baseStyles: ['American IPA', 'Pilsner'],
  entryFields: [
    { key: 'specialIngredients', label: '增味原料或特殊工艺', type: 'textarea', required: false, visibleToJudges: true },
  ],
  judgeTables: [
    { id: 'A', name: 'A桌', captain: 1, professional: 3, cross: 1 },
    { id: 'B', name: 'B桌', captain: 1, professional: 3, cross: 1 },
  ],
  scoreConfigs: defaultScoreConfigs(),
})

const checkLabels = {
  done: '已完成',
  confirm: '需确认',
  pending: '待补充',
}

const checks = computed(() => buildChecks(draft))

function removeItem(list, index) {
  list.splice(index, 1)
}

function addEntryField() {
  draft.entryFields.push({
    key: `field_${draft.entryFields.length + 1}`,
    label: '',
    type: 'text',
    required: false,
    visibleToJudges: true,
  })
}

function addJudgeTable() {
  const code = String.fromCharCode(65 + draft.judgeTables.length)
  draft.judgeTables.push({ id: code, name: `${code}桌`, captain: 1, professional: 3, cross: 1 })
}

async function submitDraft() {
  if (!draft.name || !draft.code || !draft.date || !draft.registrationDeadline || draft.entryFee === '' || draft.entryFee === null) {
    ElMessage.warning('请先填写比赛名称、编号、比赛日期、报名截止和报名费')
    activeStep.value = 0
    return
  }
  if (!draft.scoreConfigs.every((config) => getScoreTotal(config) === 50)) {
    ElMessage.warning('三类评分表总分都需要为 50 分')
    activeStep.value = 2
    return
  }
  try {
    const created = await createCompetition({
      name: draft.name,
      code: draft.code,
      edition: draft.edition,
      competitionDate: draft.date,
      registrationStart: toBackendDateTime(draft.registrationStart),
      registrationDeadline: toBackendDateTime(draft.registrationDeadline),
      entryFee: Number(draft.entryFee || 0),
    })
    const competitionId = created.id
    const categoryItems = draft.categories.filter(Boolean).map((name, index) => ({ name, sortOrder: index }))
    const styleItems = draft.baseStyles.filter(Boolean).map((name, index) => ({ name, sortOrder: index }))
    const entryFieldItems = draft.entryFields
      .filter((field) => field.key && field.label)
      .map((field, index) => ({
        fieldKey: field.key,
        fieldLabel: field.label,
        fieldType: field.type,
        required: Boolean(field.required),
        visibleToJudges: Boolean(field.visibleToJudges),
        sortOrder: index,
      }))
    const judgeTableItems = draft.judgeTables.filter((table) => table.name).map((table) => ({ tableName: table.name }))
    if (categoryItems.length) {
      await updateCompetitionCategories(competitionId, { items: categoryItems })
    }
    if (styleItems.length) {
      await updateCompetitionStyles(competitionId, { items: styleItems })
    }
    if (entryFieldItems.length) {
      await updateCompetitionEntryFields(competitionId, { items: entryFieldItems })
    }
    if (judgeTableItems.length) {
      await updateCompetitionJudgeTables(competitionId, { items: judgeTableItems })
    }
    await updateScoreConfigs(competitionId, {
      configs: draft.scoreConfigs.map((config) => ({
        judgeRoleType: config.role,
        dimensions: config.dimensions.map((dimension, index) => ({
          key: dimension.key || `${config.role.toLowerCase()}_${index + 1}`,
          label: dimension.label,
          maxScore: Number(dimension.maxScore || 0),
        })),
      })),
    })
    ElMessage.success('比赛草稿已创建，配置已保存')
    router.push(`/admin/competitions/${competitionId}`)
  } catch {
    ElMessage.warning('草稿创建或配置保存未全部完成，请在详情工作台继续检查')
  }
}

function toBackendDateTime(value) {
  if (!value) {
    return null
  }
  return value.length === 16 ? `${value}:00` : value
}
</script>

<style scoped>
.competition-create {
  --panel: rgba(22, 32, 36, 0.9);
  --panel-strong: rgba(26, 39, 44, 0.96);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --orange: #f2994a;
  --red: #e05252;
  height: 100vh;
  padding: 26px 28px;
  color: var(--text);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(216, 169, 53, 0.12), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
}

h1,
h2,
h3,
p {
  margin: 0;
}

button,
input,
select {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

svg {
  width: 1em;
  height: 1em;
}

.create-head,
.back-button,
.head-actions,
.tool-button,
.step-shell,
.step-item,
.card-title,
.stack-list label,
.field-row,
.judge-row,
.dimension-list label,
.step-actions,
.check-item {
  display: flex;
  align-items: center;
}

.create-head {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
}

.eyebrow,
p,
small,
label span,
.step-item small {
  color: var(--muted);
}

.eyebrow {
  margin-bottom: 6px;
  color: var(--gold-soft);
  font-size: 12px;
  font-weight: 800;
}

h1 {
  font-size: 28px;
}

.back-button,
.tool-button,
.icon-button {
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.back-button {
  gap: 8px;
  min-height: 42px;
  padding: 0 12px;
}

.head-actions,
.step-actions {
  gap: 10px;
}

.tool-button {
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 14px;
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.step-shell {
  flex: 1 1 auto;
  min-height: 0;
  gap: 18px;
  margin-top: 22px;
  align-items: stretch;
}

.step-rail {
  flex: 0 0 260px;
  display: grid;
  align-content: start;
  gap: 10px;
}

.step-item {
  gap: 12px;
  min-height: 76px;
  padding: 14px;
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.step-item span {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.28);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.step-item strong,
.step-item small {
  display: block;
}

.step-item.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: var(--panel-strong);
}

.step-content {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  overflow: hidden;
}

.form-section {
  flex: 1 1 auto;
  min-height: 0;
  padding: 24px;
  overflow-y: auto;
}

.form-section header {
  margin-bottom: 22px;
}

.form-section header p {
  margin-top: 8px;
}

.form-grid {
  display: grid;
  gap: 14px;
}

.form-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

label {
  display: grid;
  gap: 8px;
}

input,
select {
  min-width: 0;
  min-height: 42px;
  padding: 0 12px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  outline: 0;
  background: rgba(255, 255, 255, 0.04);
}

input::placeholder {
  color: var(--faint);
}

.config-grid,
.score-grid,
.review-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.config-card,
.review-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.config-card.wide {
  margin-top: 14px;
}

.card-title {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
}

.stack-list {
  display: grid;
  gap: 10px;
}

.stack-list label {
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.field-table,
.judge-table {
  display: grid;
  gap: 10px;
}

.field-row {
  display: grid;
  grid-template-columns: minmax(150px, 0.9fr) minmax(180px, 1fr) 130px 92px 112px 40px;
  gap: 10px;
}

.judge-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 90px 110px 110px 130px 40px;
  gap: 10px;
}

.field-head,
.judge-head {
  color: var(--muted);
  font-size: 13px;
}

.switch-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--muted);
}

.switch-line input {
  min-height: auto;
}

.judge-row strong {
  align-self: center;
  color: var(--green);
}

.judge-row strong.danger,
.score-total {
  color: var(--orange);
}

.score-total.ok {
  color: var(--green);
}

.dimension-list {
  display: grid;
  gap: 10px;
}

.dimension-list label {
  grid-template-columns: 1fr 90px;
  gap: 10px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.summary-grid span {
  padding: 12px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.summary-grid strong {
  display: block;
  margin-top: 6px;
  color: var(--text);
}

.check-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.check-item {
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.check-item.done {
  color: var(--green);
}

.check-item.pending,
.check-item.confirm {
  color: var(--orange);
}

.check-item span {
  flex: 1;
}

.step-actions {
  justify-content: flex-end;
  padding: 18px 24px;
  border-top: 1px solid var(--line);
}

@media (max-width: 1180px) {
  .step-shell,
  .create-head {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .step-rail {
    flex: 0 0 auto;
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 820px) {
  .competition-create {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .form-grid.two,
  .config-grid,
  .score-grid,
  .review-layout,
  .step-rail {
    grid-template-columns: 1fr;
  }

  .field-row,
  .judge-row {
    grid-template-columns: 1fr;
  }
}
</style>
