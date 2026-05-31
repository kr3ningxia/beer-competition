<template>
  <div class="competition-console">
    <section class="hero-panel">
      <div class="title-row">
        <h1>比赛管理</h1>
      </div>

      <button class="focus-brief" aria-label="打开当前重点赛事" @click="openDetailDrawer(focusCompetition)">
        <span class="focus-label">当前重点</span>
        <strong>{{ focusCompetition.name }}</strong>
        <span class="focus-status">{{ statusMeta[focusCompetition.status].label }}</span>
        <span class="focus-summary">{{ focusActionSummary }}</span>
        <span class="focus-date">
          <Clock />
          {{ getDateDistance(focusCompetition.date) }}
        </span>
        <Right />
      </button>

      <div class="hero-actions">
        <button class="toolbar-button neutral" @click="exportLedger">
          <Download />
          导出筛选
        </button>
        <button class="toolbar-button gold" @click="openCreateDrawer">
          <Plus />
          新建比赛
        </button>
      </div>
    </section>

    <div class="workspace-scroll">
      <section class="filter-panel">
        <div class="search-box">
          <Search />
          <input v-model.trim="keyword" type="search" placeholder="搜索比赛名称、编号" />
        </div>

        <div class="filter-group" aria-label="比赛状态筛选">
          <button
            v-for="option in statusOptions"
            :key="option.value"
            :class="{ active: selectedStatus === option.value }"
            @click="selectedStatus = option.value"
          >
            {{ option.label }}
          </button>
        </div>

        <div class="compact-controls">
          <select v-model="selectedYear" aria-label="年份筛选">
            <option value="ALL">全部年份</option>
            <option value="2026">2026</option>
            <option value="2025">2025</option>
          </select>

          <div class="view-switch" aria-label="视图切换">
            <button :class="{ active: viewMode === 'card' }" title="赛事卡片" @click="viewMode = 'card'">
              <Grid />
              <span>赛事卡片</span>
            </button>
            <button :class="{ active: viewMode === 'list' }" title="紧凑列表" @click="viewMode = 'list'">
              <List />
              <span>紧凑列表</span>
            </button>
          </div>
        </div>
      </section>

      <section v-if="filteredCompetitions.length" class="competition-section">
        <div v-if="viewMode === 'card'" class="competition-grid">
          <article
            v-for="competition in filteredCompetitions"
            :key="competition.id"
            :class="['competition-card', statusMeta[competition.status].tone]"
            @click="openDetailDrawer(competition)"
          >
            <header>
              <div class="competition-title">
                <span class="beer-mark">
                  <GobletFull />
                </span>
                <div>
                  <p>{{ competition.code }} · {{ competition.edition }}</p>
                  <h3>{{ competition.name }}</h3>
                </div>
              </div>
              <span class="state-badge">{{ statusMeta[competition.status].label }}</span>
            </header>

            <div class="meta-strip">
              <span>
                <Calendar />
                {{ formatDate(competition.date) }}
              </span>
              <span>
                <Timer />
                报名截止 {{ formatDateTime(competition.registrationDeadline) }}
              </span>
              <span>
                <Money />
                {{ competition.entryFee }} 元 / 款
              </span>
            </div>

            <div class="stage-track" aria-label="比赛流程进度">
              <div
                v-for="(stage, index) in stages"
                :key="stage.label"
                :class="['stage-node', { done: index <= getStageIndex(competition.status) }]"
              >
                <span />
                <em>{{ stage.label }}</em>
              </div>
            </div>

            <div class="card-stats">
              <div>
                <small>报名酒款</small>
                <strong>{{ competition.entries.registered }} / {{ competition.entries.total }}</strong>
                <span>待付款 {{ competition.entries.pendingPayment }} · 已入库 {{ competition.entries.stored }}</span>
              </div>
              <div>
                <small>评审准备</small>
                <strong>{{ competition.judgeSetup.tableCount }} 桌 · {{ competition.judgeSetup.judgeCount }} 人</strong>
                <span>{{ competition.judgeSetup.captainReady ? '桌长已配齐' : '桌长待补齐' }}</span>
              </div>
              <div>
                <small>配置状态</small>
                <strong>{{ getReadyCount(competition) }} / {{ checkItems.length }}</strong>
                <span>{{ getConfigHint(competition) }}</span>
              </div>
            </div>

            <div class="alert-list">
              <p v-if="competition.alerts.length === 0" class="alert-item success">
                <CircleCheck />
                关键配置已就绪，可按计划推进
              </p>
              <p
                v-for="alert in competition.alerts.slice(0, 2)"
                v-else
                :key="alert.text"
                :class="['alert-item', alert.level]"
              >
                <component :is="alert.level === 'danger' ? Warning : Clock" />
                {{ alert.text }}
              </p>
            </div>

            <footer>
              <button class="plain-action" @click.stop="openDetailDrawer(competition)">
                进入管理
                <Right />
              </button>
              <button class="next-action" @click.stop="confirmAdvance(competition)">
                {{ competition.nextAction }}
              </button>
            </footer>
          </article>
        </div>

        <div v-else class="competition-list">
          <button
            v-for="competition in filteredCompetitions"
            :key="competition.id"
            class="list-row"
            @click="openDetailDrawer(competition)"
          >
            <span class="list-main">
              <strong>{{ competition.name }}</strong>
              <small>{{ competition.code }} · {{ competition.edition }}</small>
            </span>
            <span class="list-state">{{ statusMeta[competition.status].label }}</span>
            <span>{{ formatDate(competition.date) }}</span>
            <span>{{ competition.entries.registered }} 款已报名</span>
            <span>{{ competition.judgeSetup.tableCount }} 桌 / {{ competition.judgeSetup.judgeCount }} 位评审</span>
            <b>{{ competition.nextAction }}</b>
          </button>
        </div>
      </section>

      <section v-else class="empty-panel">
        <Search />
        <strong>没有符合条件的比赛</strong>
        <p>调整名称、年份或状态筛选后再查看赛事台账。</p>
      </section>
    </div>

    <el-drawer
      v-model="detailVisible"
      :with-header="false"
      size="430px"
      class="competition-drawer"
      modal-class="dark-drawer-mask"
    >
      <section v-if="selectedCompetition" class="drawer-content">
        <header class="drawer-hero">
          <button class="drawer-close" title="关闭" @click="detailVisible = false">
            <Close />
          </button>
          <p>{{ selectedCompetition.code }} · {{ selectedCompetition.edition }}</p>
          <h2>{{ selectedCompetition.name }}</h2>
          <span class="state-badge">{{ statusMeta[selectedCompetition.status].label }}</span>
          <div class="drawer-meta">
            <span>{{ formatDate(selectedCompetition.date) }}</span>
            <span>{{ selectedCompetition.entryFee }} 元 / 款</span>
            <span>{{ selectedCompetition.stage }}</span>
          </div>
        </header>

        <div class="drawer-block">
          <div class="drawer-heading">
            <h3>配置检查</h3>
            <span>{{ getReadyCount(selectedCompetition) }} / {{ checkItems.length }} 已完成</span>
          </div>

          <div class="check-list">
            <article v-for="item in getChecks(selectedCompetition)" :key="item.label" :class="['check-item', item.state]">
              <span class="check-icon">
                <component :is="checkStateMeta[item.state].icon" />
              </span>
              <div>
                <strong>{{ item.label }}</strong>
                <p>{{ item.description }}</p>
              </div>
              <em>{{ checkStateMeta[item.state].label }}</em>
            </article>
          </div>
        </div>

        <div class="drawer-block">
          <div class="drawer-heading">
            <h3>报名与评审</h3>
            <span>当前进展</span>
          </div>
          <div class="drawer-stats">
            <div>
              <small>已报名</small>
              <strong>{{ selectedCompetition.entries.registered }}</strong>
            </div>
            <div>
              <small>已入库</small>
              <strong>{{ selectedCompetition.entries.stored }}</strong>
            </div>
            <div>
              <small>评审桌</small>
              <strong>{{ selectedCompetition.judgeSetup.tableCount }}</strong>
            </div>
            <div>
              <small>评审人数</small>
              <strong>{{ selectedCompetition.judgeSetup.judgeCount }}</strong>
            </div>
          </div>
        </div>

        <div class="drawer-block">
          <div class="drawer-heading">
            <h3>待关注事项</h3>
            <span>{{ selectedCompetition.alerts.length }} 项</span>
          </div>
          <div class="drawer-alerts">
            <p v-if="selectedCompetition.alerts.length === 0">
              <CircleCheck />
              关键事项已确认，当前比赛可以继续推进。
            </p>
            <p
              v-for="alert in selectedCompetition.alerts"
              v-else
              :key="alert.text"
              :class="alert.level"
            >
              <component :is="alert.level === 'danger' ? Warning : Clock" />
              {{ alert.text }}
            </p>
          </div>
        </div>

        <footer class="drawer-footer">
          <button class="toolbar-button neutral" @click="detailVisible = false">稍后处理</button>
          <button class="toolbar-button gold" @click="confirmAdvance(selectedCompetition)">
            {{ selectedCompetition.nextAction }}
          </button>
        </footer>
      </section>
    </el-drawer>

    <el-drawer
      v-model="createVisible"
      :with-header="false"
      size="430px"
      class="competition-drawer"
      modal-class="dark-drawer-mask"
    >
      <section class="drawer-content">
        <header class="drawer-hero">
          <button class="drawer-close" title="关闭" @click="createVisible = false">
            <Close />
          </button>
          <p>创建赛事台账</p>
          <h2>新建比赛</h2>
          <span class="state-badge">草稿</span>
        </header>

        <el-form class="create-form" label-position="top">
          <el-form-item label="比赛名称">
            <el-input v-model="createForm.name" placeholder="例如 2026 中国精酿啤酒大赛" />
          </el-form-item>
          <el-form-item label="比赛编号">
            <el-input v-model="createForm.code" placeholder="例如 BC-2026" />
          </el-form-item>
          <el-form-item label="届次">
            <el-input v-model="createForm.edition" placeholder="例如 第三批次" />
          </el-form-item>
          <el-form-item label="比赛日期">
            <el-input v-model="createForm.date" placeholder="例如 2026-08-20" />
          </el-form-item>
          <el-form-item label="报名截止">
            <el-input v-model="createForm.registrationDeadline" placeholder="例如 2026-07-30T18:00:00" />
          </el-form-item>
          <el-form-item label="报名费">
            <el-input-number v-model="createForm.entryFee" :min="0" :step="20" />
          </el-form-item>
        </el-form>

        <footer class="drawer-footer">
          <button class="toolbar-button neutral" @click="createVisible = false">取消</button>
          <button class="toolbar-button gold" @click="createCompetition">
            <Plus />
            加入台账
          </button>
        </footer>
      </section>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  CircleCheck,
  Clock,
  Close,
  Download,
  GobletFull,
  Grid,
  List,
  Money,
  Plus,
  Right,
  Search,
  Timer,
  Warning,
} from '@element-plus/icons-vue'

const stages = [
  { label: '草稿', status: 'DRAFT' },
  { label: '报名', status: 'REGISTRATION_OPEN' },
  { label: '收样', status: 'REGISTRATION_CLOSED' },
  { label: '评审准备', status: 'JUDGING_PREP' },
  { label: '评审中', status: 'JUDGING' },
  { label: '结果发布', status: 'PUBLISHED' },
]

const statusMeta = {
  DRAFT: { label: '草稿', tone: 'neutral', next: '完善报名配置', confirm: '确认完善报名配置？建议先检查组别、风格和报名字段。' },
  REGISTRATION_OPEN: { label: '报名中', tone: 'success', next: '查看报名酒款', confirm: '确认查看报名酒款？可重点处理待付款和入库提醒。' },
  REGISTRATION_CLOSED: { label: '报名截止', tone: 'warning', next: '检查入库情况', confirm: '确认进入入库检查？请核对现场收到的酒款。' },
  JUDGING_PREP: { label: '评审准备', tone: 'gold', next: '检查评审桌', confirm: '确认检查评审桌？请确保每桌桌长和评分表已就绪。' },
  JUDGING: { label: '评审中', tone: 'success', next: '查看现场进度', confirm: '确认进入现场进度？现场看板将按桌长汇总口径统计。' },
  RESULT_CONFIRMING: { label: '结果确认', tone: 'gold', next: '确认结果发布', confirm: '确认发布结果？发布后厂商将可以查看奖项和反馈。' },
  PUBLISHED: { label: '已发布', tone: 'neutral', next: '查看归档数据', confirm: '确认查看归档数据？可以导出最终比赛台账。' },
  ARCHIVED: { label: '已归档', tone: 'neutral', next: '查看归档数据', confirm: '确认查看归档数据？' },
}

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '报名中', value: 'REGISTRATION_OPEN' },
  { label: '评审准备', value: 'JUDGING_PREP' },
  { label: '评审中', value: 'JUDGING' },
  { label: '结果确认', value: 'RESULT_CONFIRMING' },
  { label: '已发布', value: 'PUBLISHED' },
]

const checkItems = [
  { key: 'baseInfo', label: '基础信息' },
  { key: 'categories', label: '报名组别' },
  { key: 'styles', label: '基础风格' },
  { key: 'entryFields', label: '报名字段' },
  { key: 'judgeTables', label: '评审桌' },
  { key: 'scoreForms', label: '评分表' },
  { key: 'storedEntries', label: '酒款入库' },
  { key: 'resultSetup', label: '结果发布' },
]

const checkStateMeta = {
  done: { label: '已完成', icon: CircleCheck },
  pending: { label: '待补充', icon: Clock },
  confirm: { label: '需确认', icon: Warning },
}

const competitions = ref([
  {
    id: 1,
    code: 'BC-2026',
    name: '2026 中国精酿啤酒大赛',
    edition: '第三批次',
    date: '2026-08-20',
    registrationDeadline: '2026-07-30T18:00:00',
    entryFee: 199,
    status: 'JUDGING_PREP',
    stage: '评审准备',
    entries: { total: 286, pendingPayment: 14, registered: 268, stored: 238, canceled: 4 },
    judgeSetup: { tableCount: 4, judgeCount: 20, captainReady: true, scoreConfigReady: true },
    scoreSetup: { professionalReady: true, crossReady: true, captainReady: true },
    resultSetup: { awardsReady: false, published: false },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'done',
      entryFields: 'done',
      judgeTables: 'done',
      scoreForms: 'done',
      storedEntries: 'confirm',
      resultSetup: 'pending',
    },
    alerts: [
      { level: 'warning', text: '还有 30 款酒等待入库确认' },
      { level: 'warning', text: '结果奖项尚未设置，评审结束前可先准备' },
    ],
  },
  {
    id: 2,
    code: 'SOUR-SC-2026',
    name: '华南酸啤邀请赛',
    edition: '春季专题',
    date: '2026-06-18',
    registrationDeadline: '2026-06-01T20:00:00',
    entryFee: 149,
    status: 'REGISTRATION_OPEN',
    stage: '报名中',
    entries: { total: 96, pendingPayment: 12, registered: 72, stored: 0, canceled: 3 },
    judgeSetup: { tableCount: 2, judgeCount: 8, captainReady: false, scoreConfigReady: true },
    scoreSetup: { professionalReady: true, crossReady: false, captainReady: true },
    resultSetup: { awardsReady: false, published: false },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'confirm',
      entryFields: 'done',
      judgeTables: 'pending',
      scoreForms: 'confirm',
      storedEntries: 'pending',
      resultSetup: 'pending',
    },
    alerts: [
      { level: 'danger', text: '缺少 1 张评审桌桌长' },
      { level: 'warning', text: '酸啤组尚未确认基础风格展示口径' },
    ],
  },
  {
    id: 3,
    code: 'MAKER-CUP-2026',
    name: '城市小酿造者杯',
    edition: '夏季城市赛',
    date: '2026-07-12',
    registrationDeadline: '2026-06-28T18:00:00',
    entryFee: 99,
    status: 'DRAFT',
    stage: '草稿',
    entries: { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0 },
    judgeSetup: { tableCount: 0, judgeCount: 0, captainReady: false, scoreConfigReady: false },
    scoreSetup: { professionalReady: false, crossReady: false, captainReady: false },
    resultSetup: { awardsReady: false, published: false },
    checks: {
      baseInfo: 'done',
      categories: 'pending',
      styles: 'pending',
      entryFields: 'pending',
      judgeTables: 'pending',
      scoreForms: 'pending',
      storedEntries: 'pending',
      resultSetup: 'pending',
    },
    alerts: [
      { level: 'danger', text: '报名组别尚未配置，开放报名会受阻' },
      { level: 'warning', text: '评分表尚未准备' },
    ],
  },
  {
    id: 4,
    code: 'STOUT-WINTER-2025',
    name: '冬季世涛专题赛',
    edition: '年度收官',
    date: '2025-12-16',
    registrationDeadline: '2025-11-26T18:00:00',
    entryFee: 179,
    status: 'RESULT_CONFIRMING',
    stage: '结果确认',
    entries: { total: 128, pendingPayment: 0, registered: 121, stored: 121, canceled: 7 },
    judgeSetup: { tableCount: 3, judgeCount: 15, captainReady: true, scoreConfigReady: true },
    scoreSetup: { professionalReady: true, crossReady: true, captainReady: true },
    resultSetup: { awardsReady: true, published: false },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'done',
      entryFields: 'done',
      judgeTables: 'done',
      scoreForms: 'done',
      storedEntries: 'done',
      resultSetup: 'confirm',
    },
    alerts: [{ level: 'warning', text: '总冠军候选需要主办方最终确认' }],
  },
  {
    id: 5,
    code: 'LAGER-OPEN-2025',
    name: '清爽拉格公开赛',
    edition: '秋季开放赛',
    date: '2025-10-10',
    registrationDeadline: '2025-09-18T18:00:00',
    entryFee: 129,
    status: 'PUBLISHED',
    stage: '已发布',
    entries: { total: 168, pendingPayment: 0, registered: 160, stored: 160, canceled: 8 },
    judgeSetup: { tableCount: 3, judgeCount: 16, captainReady: true, scoreConfigReady: true },
    scoreSetup: { professionalReady: true, crossReady: true, captainReady: true },
    resultSetup: { awardsReady: true, published: true },
    checks: {
      baseInfo: 'done',
      categories: 'done',
      styles: 'done',
      entryFields: 'done',
      judgeTables: 'done',
      scoreForms: 'done',
      storedEntries: 'done',
      resultSetup: 'done',
    },
    alerts: [],
  },
])

const keyword = ref('')
const selectedStatus = ref('ALL')
const selectedYear = ref('ALL')
const viewMode = ref('card')
const detailVisible = ref(false)
const createVisible = ref(false)
const selectedCompetition = ref(null)
const createForm = reactive(getDefaultForm())

const focusCompetition = computed(() => {
  return competitions.value.find((item) => item.status === 'JUDGING_PREP')
    || competitions.value.find((item) => item.status === 'REGISTRATION_OPEN')
    || competitions.value[0]
})

const filteredCompetitions = computed(() => {
  const normalizedKeyword = keyword.value.toLowerCase()

  return competitions.value.filter((item) => {
    const matchesKeyword = !normalizedKeyword
      || item.name.toLowerCase().includes(normalizedKeyword)
      || item.code.toLowerCase().includes(normalizedKeyword)
      || item.edition.toLowerCase().includes(normalizedKeyword)
    const matchesStatus = selectedStatus.value === 'ALL' || item.status === selectedStatus.value
    const matchesYear = selectedYear.value === 'ALL' || item.date.startsWith(selectedYear.value)

    return matchesKeyword && matchesStatus && matchesYear
  })
})

const focusActionSummary = computed(() => {
  const alertCount = focusCompetition.value.alerts.length
  const prefix = alertCount > 0 ? `${alertCount} 项待处理` : '关键配置已就绪'

  return `${prefix} · ${focusCompetition.value.nextAction}`
})

function getDefaultForm() {
  return {
    name: '2026 新建精酿啤酒赛',
    code: 'BC-NEW-2026',
    edition: '第一批次',
    date: '2026-08-20',
    registrationDeadline: '2026-07-30T18:00:00',
    entryFee: 199,
  }
}

function openCreateDrawer() {
  Object.assign(createForm, getDefaultForm())
  detailVisible.value = false
  createVisible.value = true
}

function createCompetition() {
  if (!createForm.name || !createForm.code || !createForm.date) {
    ElMessage.warning('请先填写比赛名称、编号和比赛日期')
    return
  }

  competitions.value.unshift({
    id: Date.now(),
    code: createForm.code,
    name: createForm.name,
    edition: createForm.edition,
    date: createForm.date,
    registrationDeadline: createForm.registrationDeadline,
    entryFee: createForm.entryFee,
    status: 'DRAFT',
    stage: '草稿',
    entries: { total: 0, pendingPayment: 0, registered: 0, stored: 0, canceled: 0 },
    judgeSetup: { tableCount: 0, judgeCount: 0, captainReady: false, scoreConfigReady: false },
    scoreSetup: { professionalReady: false, crossReady: false, captainReady: false },
    resultSetup: { awardsReady: false, published: false },
    checks: {
      baseInfo: 'done',
      categories: 'pending',
      styles: 'pending',
      entryFields: 'pending',
      judgeTables: 'pending',
      scoreForms: 'pending',
      storedEntries: 'pending',
      resultSetup: 'pending',
    },
    alerts: [
      { level: 'warning', text: '请先完善报名组别和基础风格' },
      { level: 'warning', text: '评审桌与评分表可在赛前继续配置' },
    ],
  })

  createVisible.value = false
  ElMessage.success('比赛已加入台账，可以继续完善报名配置')
}

function openDetailDrawer(competition) {
  selectedCompetition.value = competition
  detailVisible.value = true
}

async function confirmAdvance(competition) {
  const meta = statusMeta[competition.status]

  try {
    await ElMessageBox.confirm(meta.confirm, competition.nextAction, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: meta.tone === 'danger' ? 'warning' : 'info',
    })
    advanceCompetition(competition)
  } catch {
    // User canceled.
  }
}

function advanceCompetition(competition) {
  const nextStatusMap = {
    DRAFT: 'REGISTRATION_OPEN',
    REGISTRATION_OPEN: 'REGISTRATION_CLOSED',
    REGISTRATION_CLOSED: 'JUDGING_PREP',
    JUDGING_PREP: 'JUDGING',
    JUDGING: 'RESULT_CONFIRMING',
    RESULT_CONFIRMING: 'PUBLISHED',
    PUBLISHED: 'ARCHIVED',
    ARCHIVED: 'ARCHIVED',
  }

  const nextStatus = nextStatusMap[competition.status]
  competition.status = nextStatus
  competition.stage = statusMeta[nextStatus].label
  competition.nextAction = statusMeta[nextStatus].next

  if (nextStatus === 'PUBLISHED') {
    competition.resultSetup.published = true
    competition.checks.resultSetup = 'done'
    competition.alerts = []
  }

  ElMessage.success(`${competition.name} 已更新为${statusMeta[nextStatus].label}`)
}

function exportLedger() {
  ElMessage.success(`已准备导出 ${filteredCompetitions.value.length} 场比赛的筛选结果`)
}

function getDateDistance(value) {
  const today = new Date()
  const target = new Date(`${value}T00:00:00`)
  const distance = Math.ceil((target - today) / 86400000)

  if (distance > 0) {
    return `距比赛 ${distance} 天`
  }
  if (distance === 0) {
    return '比赛日'
  }
  return `已结束 ${Math.abs(distance)} 天`
}

function getStageIndex(status) {
  if (status === 'RESULT_CONFIRMING') {
    return 4
  }
  if (status === 'ARCHIVED') {
    return stages.length - 1
  }
  const index = stages.findIndex((stage) => stage.status === status)
  return Math.max(index, 0)
}

function getReadyCount(competition) {
  return Object.values(competition.checks).filter((state) => state === 'done').length
}

function getConfigHint(competition) {
  const readyCount = getReadyCount(competition)

  if (readyCount === checkItems.length) {
    return '关键配置已完成'
  }
  if (competition.alerts.some((alert) => alert.level === 'danger')) {
    return '存在阻塞项'
  }
  return '仍有事项待确认'
}

function getChecks(competition) {
  const descriptions = {
    baseInfo: `${formatDate(competition.date)} · ${competition.entryFee} 元 / 款`,
    categories: '投报组别用于报名和结果归档',
    styles: '评审扫码时展示基础风格',
    entryFields: '厂商报名时填写酒款说明',
    judgeTables: `${competition.judgeSetup.tableCount} 张评审桌 · ${competition.judgeSetup.judgeCount} 位评审`,
    scoreForms: competition.judgeSetup.scoreConfigReady ? '专业、跨界、桌长评分表已准备' : '评分维度需要补齐',
    storedEntries: `${competition.entries.stored} / ${competition.entries.registered} 款已入库`,
    resultSetup: competition.resultSetup.published ? '结果已开放给厂商查看' : '奖项确认后发布结果',
  }

  return checkItems.map((item) => ({
    ...item,
    state: competition.checks[item.key],
    description: descriptions[item.key],
  }))
}

function formatDate(value) {
  return value.replaceAll('-', '.')
}

function formatDateTime(value) {
  return value.replace('T', ' ').slice(0, 16).replaceAll('-', '.')
}

competitions.value.forEach((competition) => {
  competition.nextAction = statusMeta[competition.status].next
})
</script>

<style scoped>
.competition-console {
  --bg: #0e1418;
  --panel: rgba(22, 32, 36, 0.9);
  --panel-strong: rgba(25, 38, 43, 0.96);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold: #d8a935;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --orange: #f2994a;
  --red: #e05252;
  position: relative;
  height: 100vh;
  padding: 24px 26px 0;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 15% 6%, rgba(216, 169, 53, 0.13), transparent 18rem),
    radial-gradient(circle at 82% 10%, rgba(111, 207, 122, 0.08), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 48%, #0d151a 100%);
  background-size: 48px 48px, 48px 48px, auto, auto, auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.competition-console::before {
  position: absolute;
  inset: 0;
  content: "";
  pointer-events: none;
  background:
    repeating-linear-gradient(
      110deg,
      transparent 0 18px,
      rgba(216, 169, 53, 0.018) 18px 19px,
      transparent 19px 42px
    );
}

.competition-console > * {
  position: relative;
  z-index: 1;
}

.filter-panel,
.competition-card,
.empty-panel,
.list-row {
  border: 1px solid var(--line);
  background: var(--panel);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(14px);
}

.hero-panel {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 2px 0 18px;
  border-bottom: 1px solid var(--line);
}

.workspace-scroll {
  flex: 1 1 auto;
  min-height: 0;
  margin: 0 -26px;
  padding: 24px 26px 36px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.workspace-scroll::-webkit-scrollbar {
  width: 10px;
}

.workspace-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.workspace-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(14, 20, 24, 0.9);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.28);
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

svg {
  width: 1em;
  height: 1em;
}

.eyebrow,
.subtitle,
.competition-title p,
.meta-strip,
.card-stats small,
.card-stats span,
.alert-item,
.list-row,
.drawer-hero p,
.drawer-meta,
.check-item p,
.drawer-heading span,
.drawer-stats small {
  color: var(--muted);
}

.eyebrow {
  margin-bottom: 8px;
  letter-spacing: 0.12em;
  font-size: 12px;
}

.title-row,
.focus-brief,
.hero-actions,
.toolbar-button,
.filter-panel,
.filter-group,
.compact-controls,
.view-switch,
.competition-card header,
.competition-title,
.meta-strip,
.stage-track,
.card-stats,
.alert-item,
.competition-card footer,
.plain-action,
.next-action,
.list-row,
.drawer-meta,
.drawer-heading,
.check-item,
.drawer-alerts p,
.drawer-footer {
  display: flex;
  align-items: center;
}

.title-row {
  gap: 10px;
  flex-wrap: wrap;
  min-width: 150px;
}

.title-row span {
  color: var(--muted);
  font-size: 14px;
}

h1 {
  font-size: clamp(24px, 1.45vw, 28px);
  line-height: 1.08;
  letter-spacing: 0;
}

.subtitle {
  margin-top: 8px;
}

.status-pill,
.state-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.1);
  color: var(--green);
  font-weight: 800;
}

.status-pill {
  padding: 8px 12px;
  border-radius: 999px;
}

.status-pill span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--green);
}

.focus-brief {
  justify-content: flex-start;
  gap: 10px;
  min-width: 0;
  min-height: 38px;
  padding: 0;
  color: var(--muted);
  text-align: left;
  border: 0;
  background: transparent;
  transition: color 0.18s ease;
}

.focus-brief:hover {
  color: var(--text);
}

.focus-brief strong,
.focus-summary {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.focus-brief strong {
  color: var(--text);
  font-size: 16px;
}

.focus-label {
  flex: 0 0 auto;
  padding-left: 14px;
  color: var(--gold-soft);
  font-weight: 800;
  border-left: 1px solid rgba(216, 169, 53, 0.25);
}

.focus-status {
  flex: 0 0 auto;
  color: var(--green);
  font-weight: 800;
}

.focus-status::before {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 7px;
  border-radius: 999px;
  background: var(--green);
  box-shadow: 0 0 0 3px rgba(111, 207, 122, 0.12);
  content: "";
  vertical-align: 1px;
}

.focus-brief > span:not(.focus-label) {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.focus-date {
  flex: 0 0 auto;
}

.hero-actions {
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  max-width: 340px;
}

.toolbar-button,
.plain-action,
.next-action {
  min-height: 42px;
  padding: 0 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text);
  background: rgba(255, 255, 255, 0.035);
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.toolbar-button,
.plain-action,
.next-action {
  gap: 8px;
}

.toolbar-button:hover,
.plain-action:hover,
.next-action:hover {
  transform: translateY(-1px);
  border-color: rgba(224, 184, 74, 0.36);
}

.toolbar-button.gold,
.next-action {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.08);
}

.filter-panel {
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border-radius: 8px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 260px;
  height: 42px;
  padding: 0 12px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.search-box input {
  width: 100%;
  min-width: 0;
  color: var(--text);
  border: 0;
  outline: 0;
  background: transparent;
}

.search-box input::placeholder {
  color: var(--faint);
}

.filter-group {
  gap: 6px;
  flex-wrap: wrap;
}

.filter-group button,
.view-switch button,
.compact-controls select {
  min-height: 38px;
  padding: 0 12px;
  color: #9fb1b8;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.filter-group button.active,
.view-switch button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.compact-controls {
  gap: 10px;
}

.compact-controls select {
  outline: 0;
}

.view-switch {
  gap: 6px;
}

.view-switch button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.competition-section {
  margin-top: 24px;
}

.competition-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(420px, 1fr));
  gap: 16px;
}

.competition-card {
  min-height: 382px;
  padding: 20px;
  border-radius: 8px;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.competition-card:hover {
  transform: translateY(-2px);
  border-color: rgba(224, 184, 74, 0.34);
  background: rgba(25, 38, 43, 0.95);
}

.competition-card.success {
  border-color: rgba(111, 207, 122, 0.2);
}

.competition-card.warning {
  border-color: rgba(242, 153, 74, 0.32);
}

.competition-card.gold {
  border-color: rgba(216, 169, 53, 0.32);
}

.competition-card.danger {
  border-color: rgba(224, 82, 82, 0.4);
}

.competition-card header {
  justify-content: space-between;
  gap: 14px;
}

.competition-title {
  gap: 12px;
  min-width: 0;
}

.beer-mark {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 44px;
  height: 44px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.22);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.competition-title h3 {
  margin-top: 4px;
  font-size: 22px;
  line-height: 1.25;
}

.state-badge {
  flex: 0 0 auto;
  padding: 7px 10px;
  border-radius: 8px;
  white-space: nowrap;
}

.meta-strip {
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.meta-strip span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 9px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.stage-track {
  justify-content: space-between;
  gap: 8px;
  margin-top: 22px;
}

.stage-node {
  position: relative;
  flex: 1;
  display: grid;
  gap: 7px;
  color: var(--faint);
  font-size: 12px;
  text-align: center;
}

.stage-node:not(:last-child)::after {
  position: absolute;
  top: 5px;
  left: calc(50% + 13px);
  right: calc(-50% + 13px);
  height: 2px;
  content: "";
  background: rgba(255, 255, 255, 0.08);
}

.stage-node span {
  justify-self: center;
  width: 12px;
  height: 12px;
  border: 1px solid rgba(141, 161, 170, 0.28);
  border-radius: 999px;
  background: #18252a;
}

.stage-node.done {
  color: #c6d4d9;
}

.stage-node.done span,
.stage-node.done:not(:last-child)::after {
  border-color: rgba(224, 184, 74, 0.55);
  background: linear-gradient(90deg, var(--green), var(--gold-soft));
}

.card-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 22px;
}

.card-stats > div {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.card-stats strong {
  display: block;
  margin: 8px 0 6px;
  color: var(--text);
  font-size: 18px;
}

.alert-list {
  display: grid;
  gap: 8px;
  min-height: 76px;
  margin-top: 16px;
}

.alert-item {
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.028);
}

.alert-item.success {
  color: var(--green);
  background: rgba(111, 207, 122, 0.08);
}

.alert-item.warning {
  color: #f1bd79;
  background: rgba(242, 153, 74, 0.09);
}

.alert-item.danger {
  color: #ff9089;
  background: rgba(224, 82, 82, 0.09);
}

.competition-card footer {
  justify-content: space-between;
  gap: 10px;
  margin-top: 18px;
}

.competition-list {
  display: grid;
  gap: 10px;
}

.list-row {
  display: grid;
  grid-template-columns: minmax(240px, 1.5fr) 100px 110px 130px 180px 150px;
  gap: 14px;
  width: 100%;
  min-height: 70px;
  padding: 12px 14px;
  text-align: left;
  border-radius: 8px;
}

.list-main {
  display: grid;
  gap: 5px;
}

.list-main strong {
  color: var(--text);
}

.list-state {
  color: var(--gold-soft);
}

.list-row b {
  color: var(--gold-soft);
}

.empty-panel {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 280px;
  margin-top: 24px;
  border-radius: 8px;
  text-align: center;
}

.empty-panel svg {
  width: 34px;
  height: 34px;
  color: var(--gold-soft);
}

:deep(.competition-drawer) {
  background:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    #10191d;
  background-size: 42px 42px;
  color: var(--text);
}

:deep(.competition-drawer .el-drawer__body) {
  padding: 0;
}

:global(.dark-drawer-mask) {
  background: rgba(3, 7, 9, 0.55);
}

.drawer-content {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  color: var(--text);
}

.drawer-hero {
  position: relative;
  padding: 28px 24px 22px;
  border-bottom: 1px solid var(--line);
  background:
    radial-gradient(circle at 88% 12%, rgba(216, 169, 53, 0.16), transparent 8rem),
    rgba(255, 255, 255, 0.025);
}

.drawer-close {
  position: absolute;
  top: 18px;
  right: 18px;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.drawer-hero h2 {
  margin: 8px 44px 14px 0;
  font-size: 26px;
  line-height: 1.18;
}

.drawer-meta {
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.drawer-meta span {
  padding: 6px 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.drawer-block {
  padding: 22px 24px 0;
}

.drawer-heading {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.check-list {
  display: grid;
  gap: 10px;
}

.check-item {
  gap: 12px;
  padding: 13px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.check-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.check-item > div {
  flex: 1;
  min-width: 0;
}

.check-item p {
  margin-top: 4px;
  font-size: 13px;
}

.check-item em {
  flex: 0 0 auto;
  font-style: normal;
  font-weight: 800;
}

.check-item.done .check-icon,
.check-item.done em {
  color: var(--green);
}

.check-item.pending .check-icon,
.check-item.pending em {
  color: var(--orange);
}

.check-item.confirm .check-icon,
.check-item.confirm em {
  color: var(--gold-soft);
}

.drawer-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.drawer-stats div {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.drawer-stats strong {
  display: block;
  margin-top: 6px;
  color: var(--text);
  font-size: 22px;
}

.drawer-alerts {
  display: grid;
  gap: 8px;
}

.drawer-alerts p {
  gap: 8px;
  padding: 11px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.drawer-alerts p.warning {
  color: #f1bd79;
  background: rgba(242, 153, 74, 0.09);
}

.drawer-alerts p.danger {
  color: #ff9089;
  background: rgba(224, 82, 82, 0.09);
}

.drawer-footer {
  justify-content: flex-end;
  gap: 10px;
  margin-top: auto;
  padding: 22px 24px;
}

.create-form {
  padding: 22px 24px 0;
}

.create-form :deep(.el-form-item__label) {
  color: #b8c8ce;
}

.create-form :deep(.el-input__wrapper),
.create-form :deep(.el-input-number),
.create-form :deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
}

@media (max-width: 1500px) {
  .competition-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .competition-console {
    height: auto;
    min-height: 100vh;
    padding: 18px;
    overflow: visible;
  }

  .hero-panel,
  .filter-panel,
  .hero-actions,
  .compact-controls,
  .competition-card header,
  .competition-card footer {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-panel {
    display: flex;
  }

  .focus-brief {
    width: 100%;
    flex-wrap: wrap;
    padding: 2px 0;
  }

  .focus-brief strong,
  .focus-summary {
    flex: 1 1 100%;
  }

  .focus-status {
    flex: 0 0 auto;
  }

  .focus-label {
    padding-left: 0;
    border-left: 0;
  }

  .hero-actions {
    max-width: none;
  }

  .workspace-scroll {
    overflow: visible;
    margin: 0 -18px;
    padding: 18px;
  }

  .card-stats,
  .competition-grid {
    grid-template-columns: 1fr;
  }

  .list-row {
    grid-template-columns: 1fr;
  }
}
</style>
