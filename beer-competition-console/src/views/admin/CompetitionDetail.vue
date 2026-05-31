<template>
  <div class="competition-detail">
    <section v-if="competition" class="detail-head">
      <button class="back-button" type="button" @click="router.push('/admin/competitions')">
        <Back />
        返回台账
      </button>
      <div class="title-block">
        <p>{{ competition.code }} · {{ competition.edition }}</p>
        <h1>{{ competition.name }}</h1>
        <div class="meta-line">
          <span :class="['state-badge', statusInfo.tone]">{{ statusInfo.label }}</span>
          <span>{{ formatDate(competition.date) }}</span>
          <span>报名截止 {{ formatDateTime(competition.registrationDeadline) }}</span>
          <span>{{ competition.entryFee }} 元 / 款</span>
        </div>
      </div>
      <button
        class="tool-button primary"
        type="button"
        :disabled="!primaryAction.enabled"
        @click="handlePrimaryAction"
      >
        {{ primaryAction.text }}
        <Right />
      </button>
    </section>

    <section v-if="competition" class="detail-shell">
      <section class="stage-summary" :class="{ danger: hasDataIssues }">
        <div>
          <small>当前阶段</small>
          <strong>{{ competition.currentStageLabel || statusInfo.label }}</strong>
        </div>
        <div>
          <small>当前可做</small>
          <strong>{{ primaryAction.text }}</strong>
        </div>
        <div>
          <small>当前阻塞</small>
          <strong>{{ stageBlockText }}</strong>
        </div>
      </section>

      <nav class="detail-tabs" aria-label="比赛详情导航">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="activeTab = tab.key"
        >
          <component :is="tab.icon" />
          {{ tab.label }}
        </button>
      </nav>

      <main class="tab-content">
        <section v-if="activeTab === 'overview'" class="tab-panel">
          <div class="overview-grid">
            <article class="metric-card">
              <small>比赛准备情况</small>
              <strong>{{ readyCount }} / {{ competition.checks.length }}</strong>
              <p>{{ hasDataIssues ? '系统数据需修正' : stageBlockText }}</p>
            </article>
            <article class="metric-card">
              <small>报名酒款</small>
              <strong>{{ competition.entriesSummary.registered }} / {{ competition.entriesSummary.total }}</strong>
              <p>待付款 {{ competition.entriesSummary.pendingPayment }} · 已取消 {{ competition.entriesSummary.canceled }}</p>
            </article>
            <article class="metric-card">
              <small>酒款入库</small>
              <strong>{{ competition.entriesSummary.stored }}</strong>
              <p>{{ Math.max(0, competition.entriesSummary.registered - competition.entriesSummary.stored) }} 款仍待确认</p>
            </article>
            <article class="metric-card">
              <small>已完成评审汇总</small>
              <strong>{{ competition.progressSummary.finalized }} / {{ competition.progressSummary.total }}</strong>
              <p>晋级 {{ competition.progressSummary.advanced }} 款</p>
            </article>
          </div>

          <article v-if="hasDataIssues" class="panel-card issue-card">
            <div class="panel-heading">
              <h2>系统数据需修正</h2>
              <span>{{ competition.dataIntegrityIssues.length }} 项</span>
            </div>
            <p class="issue-summary">当前比赛数据不一致：比赛已开放报名，但缺少开放报名必需配置。请管理员修正数据。</p>
            <div class="check-list">
              <div v-for="issue in competition.dataIntegrityIssues" :key="issue" class="check-item invalid">
                <Warning />
                <span>{{ issue }}</span>
                <strong>需修正</strong>
              </div>
            </div>
          </article>

          <div class="two-column">
            <article class="panel-card">
              <div class="panel-heading">
                <h2>比赛准备情况</h2>
                <span>{{ currentChecks.length }} 项当前相关</span>
              </div>
              <div class="check-list">
                <div v-for="check in visibleStageChecks" :key="check.key" :class="['check-item', check.state, check.group]">
                  <CircleCheck v-if="check.state === 'done'" />
                  <Warning v-else />
                  <span>{{ check.label }}</span>
                  <button v-if="check.targetTab && check.state !== 'done'" class="link-action" type="button" @click="activeTab = check.targetTab">
                    {{ check.group === 'data_issue' ? '查看' : '去处理' }}
                  </button>
                  <strong v-else>{{ check.message }}</strong>
                </div>
              </div>
            </article>

            <article class="panel-card">
              <div class="panel-heading">
                <h2>需要处理</h2>
                <span>{{ competition.alerts.length }} 项</span>
              </div>
              <div class="alert-list">
                <p v-if="competition.alerts.length === 0" class="success">
                  <CircleCheck />
                  当前阶段没有需要立即处理的事项。
                </p>
                <p v-for="alert in competition.alerts" v-else :key="alert.text" :class="alert.level">
                  <component :is="alert.level === 'danger' ? Warning : Clock" />
                  {{ alert.text }}
                </p>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'entryConfig'" class="tab-panel">
          <div class="edit-banner" :class="{ locked: !editable.entryStructure }">
            <Lock v-if="!editable.entryStructure" />
            <Edit v-else />
            {{ editable.entryStructure ? '草稿阶段可编辑报名结构' : '报名已开放，报名结构已锁定。若需修改组别、风格或报名补充信息，请先关闭报名或由管理员修正数据。' }}
          </div>

          <div class="two-column">
            <article class="panel-card">
              <div class="panel-heading">
                <h2>投递组别</h2>
                <button v-if="editable.entryStructure" class="icon-button" type="button" @click="categoryForm.push('')">
                  <Plus />
                </button>
              </div>
              <div v-if="editable.entryStructure" class="stack-list">
                <label v-for="(_, index) in categoryForm" :key="`category-${index}`">
                  <input v-model.trim="categoryForm[index]" placeholder="例如 IPA" />
                  <button class="icon-button" type="button" @click="removeItem(categoryForm, index)">
                    <Delete />
                  </button>
                </label>
              </div>
              <div v-else class="pill-list">
                <span v-for="category in categoryForm" :key="category">{{ category }}</span>
                <p v-if="categoryForm.length === 0" class="empty-line">当前没有投递组别数据，请管理员修正。</p>
              </div>
            </article>

            <article class="panel-card">
              <div class="panel-heading">
                <h2>基础风格</h2>
                <button v-if="editable.entryStructure" class="icon-button" type="button" @click="styleForm.push('')">
                  <Plus />
                </button>
              </div>
              <div v-if="editable.entryStructure" class="stack-list">
                <label v-for="(_, index) in styleForm" :key="`style-${index}`">
                  <input v-model.trim="styleForm[index]" placeholder="例如 American IPA" />
                  <button class="icon-button" type="button" @click="removeItem(styleForm, index)">
                    <Delete />
                  </button>
                </label>
              </div>
              <div v-else class="pill-list">
                <span v-for="style in styleForm" :key="style">{{ style }}</span>
                <p v-if="styleForm.length === 0" class="empty-line">当前没有基础风格数据，且报名结构已锁定，请管理员修正。</p>
              </div>
            </article>
          </div>

          <article class="panel-card">
            <div class="panel-heading">
              <h2>报名补充信息</h2>
              <button v-if="editable.entryStructure" class="tool-button" type="button" @click="addEntryField">
                <Plus />
                添加字段
              </button>
            </div>
            <div v-if="editable.entryStructure" class="data-table field-table">
              <div class="table-head">
                <span>字段 key</span>
                <span>字段名称</span>
                <span>类型</span>
                <span>必填</span>
                <span>评审可见</span>
                <span></span>
              </div>
              <div v-for="(field, index) in entryFieldForm" :key="field.localId" class="table-row">
                <input v-model.trim="field.fieldKey" :disabled="!editable.entryStructure" />
                <input v-model.trim="field.fieldLabel" :disabled="!editable.entryStructure" />
                <select v-model="field.fieldType" :disabled="!editable.entryStructure">
                  <option value="text">短文本</option>
                  <option value="textarea">长文本</option>
                  <option value="number">数字</option>
                  <option value="select">选项</option>
                </select>
                <label class="check-control"><input v-model="field.required" type="checkbox" :disabled="!editable.entryStructure" /> 必填</label>
                <label class="check-control"><input v-model="field.visibleToJudges" type="checkbox" :disabled="!editable.entryStructure" /> 展示</label>
                <button class="icon-button" type="button" :disabled="!editable.entryStructure" @click="removeItem(entryFieldForm, index)">
                  <Delete />
                </button>
              </div>
            </div>
            <div v-else class="data-table field-readonly-table">
              <div class="table-head">
                <span>字段 key</span>
                <span>字段名称</span>
                <span>类型</span>
                <span>必填</span>
                <span>评审可见</span>
              </div>
              <div v-for="field in entryFieldForm" :key="field.localId" class="table-row">
                <strong>{{ field.fieldKey }}</strong>
                <span>{{ field.fieldLabel }}</span>
                <span>{{ fieldTypeLabels[field.fieldType] || field.fieldType }}</span>
                <span>{{ field.required ? '必填' : '选填' }}</span>
                <span>{{ field.visibleToJudges ? '展示' : '隐藏' }}</span>
              </div>
              <p v-if="entryFieldForm.length === 0" class="empty-line">当前没有报名补充信息，且报名结构已锁定，请管理员修正。</p>
            </div>
            <footer v-if="editable.entryStructure" class="panel-actions">
              <button class="tool-button primary" type="button" @click="saveEntryConfig">
                <Check />
                保存报名配置
              </button>
            </footer>
          </article>
        </section>

        <section v-if="activeTab === 'entries'" class="tab-panel">
          <div class="overview-grid">
            <article class="metric-card"><small>全部</small><strong>{{ competition.entriesSummary.total }}</strong><p>已报名酒款</p></article>
            <article class="metric-card"><small>待付款</small><strong>{{ competition.entriesSummary.pendingPayment }}</strong><p>需跟进支付</p></article>
            <article class="metric-card"><small>已入库</small><strong>{{ competition.entriesSummary.stored }}</strong><p>现场已确认</p></article>
            <article class="metric-card"><small>结果已出</small><strong>{{ competition.entriesSummary.resultPublished }}</strong><p>发布后可见</p></article>
          </div>
          <article class="panel-card">
            <div class="panel-heading">
              <h2>参赛酒款</h2>
              <span>最近 20 条</span>
            </div>
            <div class="data-table entries-table">
              <div class="table-head">
                <span>UUID</span>
                <span>投递组别</span>
                <span>基础风格</span>
                <span>入库</span>
                <span>状态</span>
              </div>
              <div v-for="entry in competition.entries" :key="entry.uuid" class="table-row">
                <strong>{{ entry.uuid }}</strong>
                <span>{{ entry.categoryName }}</span>
                <span>{{ entry.style }}</span>
                <span>{{ entry.stored ? '已入库' : '待入库' }}</span>
                <span>{{ entryStatusLabels[entry.status] || entry.status }}</span>
              </div>
              <p v-if="competition.entries.length === 0" class="empty-line">暂无报名酒款。</p>
            </div>
          </article>
        </section>

        <section v-if="activeTab === 'judges'" class="tab-panel">
          <div class="edit-banner" :class="{ locked: !editable.judgeTables }">
            <Lock v-if="!editable.judgeTables" />
            <Edit v-else />
            {{ editable.judgeTables ? '评审开始前可调整评审桌' : '评审已开始，评审桌和评分表已锁定。' }}
          </div>
          <article class="panel-card">
            <div class="panel-heading">
              <h2>评审桌配置</h2>
              <button v-if="editable.judgeTables" class="tool-button" type="button" @click="addJudgeTable">
                <Plus />
                添加评审桌
              </button>
            </div>
            <div class="data-table judge-table">
              <div class="table-head">
                <span>桌号</span>
                <span>桌长</span>
                <span>专业评审</span>
                <span>跨界评审</span>
                <span></span>
              </div>
              <div v-for="(table, index) in judgeTableForm" :key="table.localId" class="table-row">
                <input v-if="editable.judgeTables" v-model.trim="table.tableName" />
                <strong v-else>{{ table.tableName }}</strong>
                <span :class="{ danger: table.captainCount !== 1, success: table.captainCount === 1 }">{{ table.captainCount }} 人</span>
                <span>{{ table.professionalCount }} 人</span>
                <span>{{ table.crossCount }} 人</span>
                <button v-if="editable.judgeTables" class="icon-button" type="button" @click="removeItem(judgeTableForm, index)">
                  <Delete />
                </button>
              </div>
            </div>
            <footer v-if="editable.judgeTables" class="panel-actions">
              <button class="tool-button primary" type="button" @click="saveJudgeTables">
                <Check />
                保存评审桌
              </button>
            </footer>
          </article>
        </section>

        <section v-if="activeTab === 'score'" class="tab-panel">
          <div class="edit-banner" :class="{ locked: !editable.scoreConfigs }">
            <Lock v-if="!editable.scoreConfigs" />
            <Edit v-else />
            {{ editable.scoreConfigs ? '评审开始前可调整三类评分表' : '评审已开始，评审桌和评分表已锁定。' }}
          </div>
          <div class="score-panels">
            <article v-for="config in scoreConfigForm" :key="config.role" class="panel-card">
              <div class="panel-heading">
                <h2>{{ roleLabels[config.role] }}</h2>
                <span :class="{ success: getScoreTotal(config) === 50, danger: getScoreTotal(config) !== 50 }">
                  {{ getScoreTotal(config) }} / 50
                </span>
              </div>
              <p v-if="getScoreTotal(config) !== 50" class="score-warning">
                当前总分 {{ getScoreTotal(config) }}，必须调整为 50 后才能保存。
              </p>
              <div class="dimension-list">
                <div class="dimension-head">
                  <span>key</span>
                  <span>维度</span>
                  <span>分值</span>
                </div>
                <div v-for="dimension in config.dimensions" :key="dimension.localId" class="dimension-row">
                  <input v-if="editable.scoreConfigs" v-model.trim="dimension.key" placeholder="key" />
                  <strong v-else>{{ dimension.key }}</strong>
                  <input v-if="editable.scoreConfigs" v-model.trim="dimension.label" placeholder="维度名称" />
                  <span v-else>{{ dimension.label }}</span>
                  <input v-if="editable.scoreConfigs" v-model.number="dimension.maxScore" min="1" type="number" />
                  <strong v-else>{{ dimension.maxScore }}</strong>
                </div>
              </div>
            </article>
          </div>
          <footer v-if="editable.scoreConfigs" class="panel-actions">
            <button class="tool-button primary" type="button" @click="saveScoreConfigs">
              <Check />
              保存评分表
            </button>
          </footer>
        </section>

        <section v-if="activeTab === 'progress'" class="tab-panel">
          <article v-if="isJudgingStage" class="panel-card">
            <div class="panel-heading">
              <h2>现场进度</h2>
              <span>按已完成评审汇总统计</span>
            </div>
            <div class="progress-list">
              <div v-for="table in competition.judgeTables" :key="table.id" class="progress-row">
                <strong>{{ table.tableName }}</strong>
                <div class="progress-bar"><span :style="{ width: getProgressWidth(table) }" /></div>
                <em>{{ table.finalized }} / {{ table.total }}</em>
              </div>
            </div>
          </article>
          <article v-else class="panel-card empty-panel">
            <h2>现场进度</h2>
            <p>比赛尚未进入评审阶段，评审开始后会按评审桌展示汇总进度。</p>
          </article>
        </section>

        <section v-if="activeTab === 'results'" class="tab-panel">
          <article v-if="!isResultStage" class="panel-card empty-panel">
            <h2>结果发布</h2>
            <p>比赛尚未进入结果确认阶段，评审完成后可配置奖项并发布结果。</p>
          </article>
          <div v-else class="two-column">
            <article class="panel-card">
              <div class="panel-heading">
                <h2>结果发布检查</h2>
                <span>{{ competition.resultSetup.published ? '已发布' : '未发布' }}</span>
              </div>
              <div class="check-list">
                <div :class="['check-item', competition.resultSetup.awardsReady ? 'done' : 'pending']">
                  <CircleCheck v-if="competition.resultSetup.awardsReady" />
                  <Warning v-else />
                  <span>奖项配置</span>
                  <strong>{{ competition.resultSetup.awardsReady ? '已设置' : '待设置' }}</strong>
                </div>
                <div :class="['check-item', competition.resultSetup.championReady ? 'done' : 'pending']">
                  <CircleCheck v-if="competition.resultSetup.championReady" />
                  <Warning v-else />
                  <span>全场总冠军</span>
                  <strong>{{ competition.resultSetup.championReady ? '已确认' : '待确认' }}</strong>
                </div>
              </div>
            </article>
            <article class="panel-card publish-card">
              <h2>发布与导出</h2>
              <p>结果发布功能暂未启用。请在评审完成并确认奖项后发布。</p>
              <div class="publish-actions">
                <button class="tool-button" type="button">导出评分数据</button>
                <button class="tool-button primary" type="button">发布结果</button>
              </div>
            </article>
          </div>
        </section>
      </main>
    </section>

    <section v-else class="not-found">
      <h1>{{ loading ? '正在加载比赛' : '没有找到比赛' }}</h1>
      <button class="tool-button primary" type="button" @click="router.push('/admin/competitions')">返回台账</button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  Calendar,
  Check,
  CircleCheck,
  Clock,
  DataAnalysis,
  Delete,
  Edit,
  Files,
  Finished,
  Lock,
  Medal,
  Plus,
  Right,
  Setting,
  Tickets,
  Warning,
} from '@element-plus/icons-vue'
import {
  defaultScoreConfigs,
  fieldTypeLabels,
  formatDate,
  formatDateTime,
  getScoreTotal,
  roleLabels,
  statusMeta,
} from './competitionStore'
import {
  fetchCompetitionDetail,
  openCompetitionRegistration,
  updateCompetitionCategories,
  updateCompetitionEntryFields,
  updateCompetitionJudgeTables,
  updateCompetitionStyles,
  updateScoreConfigs,
} from '@/api/admin'

const route = useRoute()
const router = useRouter()
const activeTab = ref('overview')
const loading = ref(false)
const competition = ref(null)
const categoryForm = reactive([])
const styleForm = reactive([])
const entryFieldForm = reactive([])
const judgeTableForm = reactive([])
const scoreConfigForm = reactive([])

const tabs = [
  { key: 'overview', label: '概览', icon: DataAnalysis },
  { key: 'entryConfig', label: '报名配置', icon: Setting },
  { key: 'entries', label: '参赛酒款', icon: Tickets },
  { key: 'judges', label: '评审配置', icon: Files },
  { key: 'score', label: '评分表', icon: Finished },
  { key: 'progress', label: '现场进度', icon: Calendar },
  { key: 'results', label: '结果发布', icon: Medal },
]

const entryStatusLabels = {
  PENDING_PAYMENT: '待付款',
  REGISTERED: '已报名',
  STORED: '已入库',
  CANCELED: '已取消',
  RESULT_PUBLISHED: '结果已出',
}

const statusInfo = computed(() => statusMeta[competition.value?.status] || statusMeta.DRAFT)
const editable = computed(() => competition.value?.editableScopes || {})
const readyCount = computed(() => competition.value?.checks.filter((check) => check.state === 'done').length || 0)
const primaryAction = computed(() => competition.value?.primaryAction || { text: statusInfo.value.next, targetTab: 'overview', enabled: true })
const hasDataIssues = computed(() => Boolean(competition.value?.dataIntegrityIssues?.length))
const currentChecks = computed(() => (competition.value?.stageChecks || []).filter((check) => ['current', 'required', 'data_issue'].includes(check.group)))
const visibleStageChecks = computed(() => {
  const checks = competition.value?.stageChecks || []
  const priority = ['data_issue', 'required', 'current', 'locked', 'future']
  return checks
    .filter((check) => check.group !== 'future' || check.state !== 'done')
    .sort((left, right) => priority.indexOf(left.group) - priority.indexOf(right.group))
})
const stageBlockText = computed(() => {
  if (hasDataIssues.value) {
    return '系统数据需修正'
  }
  const blockingCount = currentChecks.value.filter((check) => check.state !== 'done').length
  return blockingCount ? `${blockingCount} 项需要处理` : '无阻塞'
})
const isJudgingStage = computed(() => ['JUDGING', 'RESULT_CONFIRMING', 'PUBLISHED', 'ARCHIVED'].includes(competition.value?.status))
const isResultStage = computed(() => ['RESULT_CONFIRMING', 'PUBLISHED', 'ARCHIVED'].includes(competition.value?.status))
const canOpenRegistration = computed(() => {
  if (competition.value?.status !== 'DRAFT') {
    return false
  }
  const blocking = ['baseInfo', 'categories', 'styles', 'entryFields', 'judgeTables', 'scoreForms']
  return blocking.every((key) => competition.value.checks.find((check) => check.key === key)?.state === 'done')
})

onMounted(loadDetail)
watch(() => route.params.id, loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    const data = await fetchCompetitionDetail(route.params.id)
    competition.value = normalizeDetail(data)
    resetForms()
  } finally {
    loading.value = false
  }
}

function normalizeDetail(data) {
  return {
    ...data,
    date: data.competitionDate,
    categories: data.categories || [],
    styles: data.styles || [],
    entryFields: data.entryFields || [],
    judgeTables: data.judgeTables || [],
    scoreConfigs: data.scoreConfigs || [],
    checks: data.checks || [],
    stageChecks: data.stageChecks || [],
    alerts: data.alerts || [],
    dataIntegrityIssues: data.dataIntegrityIssues || [],
    currentStageLabel: data.currentStageLabel,
    primaryAction: data.primaryAction,
    entries: data.entries || [],
    editableScopes: data.editableScopes || {},
    entriesSummary: data.entriesSummary || { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0, resultPublished: 0 },
    progressSummary: data.progressSummary || { finalized: 0, total: 0, advanced: 0, commentWarnings: 0 },
    resultSetup: data.resultSetup || { awardsReady: false, published: false, championReady: false },
  }
}

function resetForms() {
  categoryForm.splice(0, categoryForm.length, ...competition.value.categories.map((item) => item.name))
  styleForm.splice(0, styleForm.length, ...competition.value.styles.map((item) => item.name))
  entryFieldForm.splice(0, entryFieldForm.length, ...competition.value.entryFields.map((item, index) => ({
    ...item,
    localId: item.id || `field-${index}`,
  })))
  judgeTableForm.splice(0, judgeTableForm.length, ...competition.value.judgeTables.map((item, index) => ({
    ...item,
    localId: item.id || `table-${index}`,
  })))
  const sourceScoreConfigs = competition.value.scoreConfigs.length ? competition.value.scoreConfigs : defaultScoreConfigs().map((item) => ({
    judgeRoleType: item.role,
    dimensions: item.dimensions,
  }))
  scoreConfigForm.splice(0, scoreConfigForm.length, ...sourceScoreConfigs.map((item) => ({
    role: item.judgeRoleType,
    dimensions: item.dimensions.map((dimension, index) => ({
      key: dimension.key || `${item.judgeRoleType.toLowerCase()}_${index + 1}`,
      label: dimension.label,
      maxScore: Number(dimension.maxScore || 0),
      localId: `${item.judgeRoleType}-${index}`,
    })),
  })))
}

function addEntryField() {
  entryFieldForm.push({
    localId: `new-field-${Date.now()}`,
    fieldKey: `field_${entryFieldForm.length + 1}`,
    fieldLabel: '',
    fieldType: 'text',
    required: false,
    visibleToJudges: true,
  })
}

function addJudgeTable() {
  const code = String.fromCharCode(65 + judgeTableForm.length)
  judgeTableForm.push({
    localId: `new-table-${Date.now()}`,
    tableName: `${code}桌`,
    captainCount: 0,
    professionalCount: 0,
    crossCount: 0,
    finalized: 0,
    total: 0,
  })
}

function removeItem(list, index) {
  list.splice(index, 1)
}

async function saveEntryConfig() {
  const categoryItems = categoryForm.filter(Boolean).map((name, index) => ({ name, sortOrder: index }))
  const styleItems = styleForm.filter(Boolean).map((name, index) => ({ name, sortOrder: index }))
  const fieldItems = entryFieldForm
    .filter((field) => field.fieldKey && field.fieldLabel)
    .map((field, index) => ({
      fieldKey: field.fieldKey,
      fieldLabel: field.fieldLabel,
      fieldType: field.fieldType,
      required: Boolean(field.required),
      visibleToJudges: Boolean(field.visibleToJudges),
      sortOrder: index,
    }))
  if (!categoryItems.length || !styleItems.length || !fieldItems.length) {
    ElMessage.warning('投递组别、基础风格和报名补充信息都至少保留 1 项')
    return
  }
  await updateCompetitionCategories(competition.value.id, { items: categoryItems })
  await updateCompetitionStyles(competition.value.id, { items: styleItems })
  const detail = await updateCompetitionEntryFields(competition.value.id, { items: fieldItems })
  competition.value = normalizeDetail(detail)
  resetForms()
  ElMessage.success('报名配置已保存')
}

async function saveJudgeTables() {
  const items = judgeTableForm.filter((table) => table.tableName).map((table) => ({ tableName: table.tableName }))
  if (!items.length) {
    ElMessage.warning('至少保留 1 张评审桌')
    return
  }
  const detail = await updateCompetitionJudgeTables(competition.value.id, { items })
  competition.value = normalizeDetail(detail)
  resetForms()
  ElMessage.success('评审桌已保存')
}

async function saveScoreConfigs() {
  if (!scoreConfigForm.every((config) => getScoreTotal(config) === 50)) {
    ElMessage.warning('三类评分表总分都需要为 50 分')
    return
  }
  await updateScoreConfigs(competition.value.id, {
    configs: scoreConfigForm.map((config) => ({
      judgeRoleType: config.role,
      dimensions: config.dimensions.map((dimension) => ({
        key: dimension.key,
        label: dimension.label,
        maxScore: Number(dimension.maxScore || 0),
      })),
    })),
  })
  await loadDetail()
  ElMessage.success('评分表已保存')
}

async function handleOpenRegistration() {
  const detail = await openCompetitionRegistration(competition.value.id)
  competition.value = normalizeDetail(detail)
  resetForms()
  ElMessage.success('比赛已开放报名')
}

async function handlePrimaryAction() {
  if (competition.value.status === 'DRAFT' && canOpenRegistration.value) {
    await handleOpenRegistration()
    return
  }
  if (primaryAction.value.targetTab) {
    activeTab.value = primaryAction.value.targetTab
  }
}

function getProgressWidth(table) {
  if (!table.total) {
    return '0%'
  }
  return `${Math.min(100, Math.round((table.finalized / table.total) * 100))}%`
}
</script>

<style scoped>
.competition-detail {
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

button:disabled,
input:disabled,
select:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

svg {
  width: 1em;
  height: 1em;
}

.detail-head,
.back-button,
.tool-button,
.detail-tabs,
.stage-summary,
.meta-line,
.panel-heading,
.alert-list p,
.check-item,
.stack-list label,
.panel-actions,
.progress-row,
.publish-actions,
.edit-banner {
  display: flex;
  align-items: center;
}

.detail-head {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
}

.title-block {
  min-width: 0;
}

.title-block p,
.meta-line,
small,
.panel-heading span,
.publish-card p,
.empty-line {
  color: var(--muted);
}

.title-block h1 {
  margin-top: 5px;
  font-size: 28px;
  line-height: 1.15;
}

.meta-line {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.meta-line > span:not(.state-badge) {
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.back-button,
.tool-button,
.icon-button {
  gap: 8px;
  min-height: 42px;
  padding: 0 12px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.icon-button {
  display: grid;
  place-items: center;
  width: 36px;
  min-height: 36px;
  padding: 0;
}

.state-badge {
  display: inline-flex;
  padding: 7px 10px;
  color: var(--green);
  font-weight: 800;
  border: 1px solid rgba(111, 207, 122, 0.2);
  border-radius: 8px;
  background: rgba(111, 207, 122, 0.1);
}

.state-badge.gold,
.state-badge.warning {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.08);
}

.state-badge.neutral {
  color: #a9bbc2;
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.detail-shell {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: 18px;
  display: flex;
  flex-direction: column;
}

.stage-summary {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid rgba(111, 207, 122, 0.18);
  border-radius: 8px;
  background: rgba(111, 207, 122, 0.055);
}

.stage-summary.danger {
  border-color: rgba(224, 82, 82, 0.3);
  background: rgba(224, 82, 82, 0.07);
}

.stage-summary div {
  min-width: 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.stage-summary small,
.stage-summary strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-summary strong {
  margin-top: 5px;
  color: var(--text);
}

.detail-tabs {
  flex: 0 0 auto;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 12px;
  color: #a9bbc2;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.detail-tabs button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.tab-content {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: 14px;
  overflow-y: auto;
}

.tab-panel {
  display: grid;
  gap: 14px;
  padding-bottom: 28px;
}

.overview-grid,
.score-panels {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-card,
.panel-card {
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.metric-card,
.panel-card {
  padding: 16px;
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  color: var(--text);
  font-size: 24px;
}

.issue-card {
  border-color: rgba(224, 82, 82, 0.28);
  background: rgba(224, 82, 82, 0.055);
}

.issue-summary,
.score-warning {
  margin-bottom: 12px;
  color: #ffb4ad;
}

.panel-heading {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.check-list,
.alert-list,
.stack-list,
.progress-list,
.dimension-list {
  display: grid;
  gap: 10px;
}

.check-item,
.alert-list p,
.table-row,
.edit-banner {
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.edit-banner {
  color: var(--green);
}

.edit-banner.locked {
  color: #f1bd79;
  background: rgba(242, 153, 74, 0.09);
}

.check-item span {
  flex: 1;
}

.check-item.done,
.alert-list p.success,
.success {
  color: var(--green);
}

.check-item.pending,
.check-item.locked,
.alert-list p.warning {
  color: #f1bd79;
  background: rgba(242, 153, 74, 0.09);
}

.check-item.invalid,
.check-item.data_issue,
.alert-list p.danger,
.danger {
  color: #ff9089;
}

.link-action {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 0 10px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.28);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.07);
}

.pill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill-list span {
  padding: 7px 10px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

input,
select {
  width: 100%;
  box-sizing: border-box;
  min-width: 0;
  min-height: 40px;
  padding: 0 10px;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: 0;
  background-color: rgba(7, 14, 17, 0.72);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.035);
  transition: border-color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}

input:hover,
select:hover {
  border-color: rgba(224, 184, 74, 0.24);
}

input:focus,
select:focus {
  border-color: rgba(224, 184, 74, 0.5);
  background-color: rgba(10, 20, 24, 0.92);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.09);
}

input::placeholder {
  color: var(--faint);
}

input:disabled,
select:disabled {
  color: #8fa0a8;
  -webkit-text-fill-color: #8fa0a8;
  border-color: rgba(219, 232, 237, 0.08);
  background-color: rgba(9, 16, 19, 0.78);
  opacity: 1;
}

input[type="number"] {
  text-align: right;
}

input[type="checkbox"] {
  display: inline-grid;
  place-content: center;
  width: 16px;
  min-width: 16px;
  height: 16px;
  min-height: 16px;
  margin: 0;
  padding: 0;
  accent-color: var(--gold-soft);
}

select {
  appearance: none;
  padding-right: 28px;
  background-image:
    linear-gradient(45deg, transparent 50%, #9eb0b7 50%),
    linear-gradient(135deg, #9eb0b7 50%, transparent 50%);
  background-position:
    calc(100% - 16px) 17px,
    calc(100% - 11px) 17px;
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}

.stack-list label {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.check-control {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: var(--muted);
  white-space: nowrap;
}

.data-table {
  display: grid;
  gap: 8px;
}

.table-head,
.table-row {
  display: grid;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.table-head {
  color: var(--muted);
  font-size: 13px;
}

.field-table .table-head,
.field-table .table-row {
  grid-template-columns: minmax(132px, 0.8fr) minmax(170px, 1fr) 120px 86px 106px 40px;
}

.field-readonly-table .table-head,
.field-readonly-table .table-row {
  grid-template-columns: minmax(150px, 0.9fr) minmax(200px, 1fr) 120px 92px 120px;
}

.entries-table .table-head,
.entries-table .table-row {
  grid-template-columns: minmax(180px, 1fr) 120px 180px 100px 110px;
}

.judge-table .table-head,
.judge-table .table-row {
  grid-template-columns: minmax(160px, 1fr) 100px 110px 110px 40px;
}

.table-row strong {
  color: var(--text);
}

.panel-actions {
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}

.score-panels {
  grid-template-columns: repeat(3, minmax(280px, 1fr));
}

.dimension-list {
  gap: 8px;
}

.dimension-head,
.dimension-row {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1fr) 72px;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.dimension-head {
  padding: 0 10px;
  color: var(--muted);
  font-size: 12px;
}

.dimension-row {
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.024);
}

.dimension-row strong,
.dimension-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-row {
  gap: 14px;
}

.progress-row strong {
  width: 90px;
}

.progress-row em {
  width: 90px;
  color: var(--muted);
  font-style: normal;
  text-align: right;
}

.progress-bar {
  flex: 1;
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
}

.progress-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--green), var(--gold-soft));
}

.publish-card {
  display: grid;
  gap: 14px;
  align-content: start;
}

.empty-panel {
  display: grid;
  gap: 10px;
  align-content: start;
  min-height: 170px;
}

.empty-panel p {
  color: var(--muted);
}

.publish-actions {
  gap: 10px;
  flex-wrap: wrap;
}

.not-found {
  display: grid;
  place-items: center;
  gap: 14px;
  min-height: 60vh;
}

@media (max-width: 1260px) {
  .overview-grid,
  .score-panels {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .competition-detail {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .detail-head,
  .two-column,
  .overview-grid,
  .score-panels {
    grid-template-columns: 1fr;
  }

  .tab-content {
    overflow: visible;
  }

  .table-head {
    display: none;
  }

  .field-table .table-row,
  .field-readonly-table .table-row,
  .entries-table .table-row,
  .judge-table .table-row,
  .dimension-row {
    grid-template-columns: 1fr;
  }
}
</style>
