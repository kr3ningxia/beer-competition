<template>
  <div class="live-dashboard">
    <section class="hero-panel">
      <div class="hero-main">
        <p class="eyebrow">当前比赛</p>
        <div class="title-row">
          <h1>{{ competition.name }}</h1>
          <span class="status-pill live">
            <span />
            当前阶段：{{ competition.currentStage }}
          </span>
        </div>
        <div class="meta-row">
          <span>{{ competition.date }}</span>
          <span>{{ competition.batch }}</span>
        </div>
      </div>

      <div class="toolbar" aria-label="现场操作">
        <button
          v-for="action in topActions"
          :key="action.label"
          :class="['toolbar-button', action.tone, { disabled: action.disabled }]"
          :disabled="action.disabled"
          :title="action.disabled ? action.disabledReason : action.label"
        >
          <component :is="action.icon" />
          <span>{{ action.label }}</span>
          <strong v-if="action.badge">{{ action.badge }}</strong>
        </button>
      </div>
    </section>

    <div class="scroll-fade" aria-hidden="true" />

    <div class="dashboard-scroll">
      <section class="section-block first-section">
        <div class="section-heading">
          <h2>核心总览</h2>
          <span>统计当前批次全部评审桌</span>
        </div>
        <div class="summary-grid">
          <article v-for="item in summaryCards" :key="item.label" :class="['metric-card', item.tone]">
            <div>
              <p>{{ item.label }}</p>
              <strong>{{ item.value }}</strong>
              <span>{{ item.hint }}</span>
            </div>
            <span class="metric-icon">
              <component :is="item.icon" />
            </span>
          </article>
        </div>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <h2>评审桌进度</h2>
          <span>评委评分、桌长确认、入围选择分项查看</span>
        </div>
        <div class="table-grid">
          <article v-for="table in judgeTables" :key="table.tableName" :class="['table-card', table.tone]">
            <header>
              <div class="table-title">
                <span class="table-letter">{{ table.tableName.slice(0, 1) }}</span>
                <div>
                  <h3>{{ table.tableName }}</h3>
                  <p>
                    <User />
                    桌长：{{ table.captainName }} · {{ table.memberCount }} 人
                  </p>
                </div>
              </div>
              <span class="state-badge">{{ table.statusText }}</span>
            </header>

            <div class="flight-row">
              <span>轮次进度：{{ table.flightCompletedCount }} / {{ table.flightTotalCount }}</span>
              <div class="flight-cups" aria-label="轮次进度">
                <span
                  v-for="index in table.flightTotalCount"
                  :key="index"
                  :class="{ active: index <= table.flightCompletedCount, current: index === table.flightCompletedCount + 1 }"
                />
              </div>
              <strong>当前轮次：{{ table.currentFlightName }}</strong>
            </div>

            <div class="current-flight">
              {{ table.currentFlightName }} 桌长已确认：{{ table.currentFlightFinalizedCount }} /
              {{ table.currentFlightEntryCount }}
            </div>

            <div class="progress-stack">
              <div class="progress-block">
                <div class="progress-head">
                  <span>评委评分</span>
                  <strong>{{ table.judgeSubmittedCount }} / {{ table.judgeSubmissionTotal }}</strong>
                </div>
                <div class="progress-track">
                  <span :style="{ width: `${table.judgeSubmittedPercent}%` }" />
                </div>
              </div>

              <div class="progress-block final">
                <div class="progress-head">
                  <span>桌长确认</span>
                  <strong>{{ table.captainFinalizedCount }} / {{ table.captainFinalizeTotal }}</strong>
                </div>
                <div class="progress-track">
                  <span :style="{ width: `${table.captainFinalizedPercent}%` }" />
                </div>
              </div>
            </div>

            <div class="table-meta">
              <span>
                <Medal />
                本轮入围：{{ table.advancedSelectedCount }} / 建议 {{ table.advancedSuggestedMin }}-{{ table.advancedSuggestedMax }}
              </span>
              <span>
                <Clock />
                最近桌长确认：{{ table.lastFinalizeText }}
              </span>
            </div>

            <p v-if="table.issueText" class="table-warning">
              <Warning />
              {{ table.issueText }}
            </p>
          </article>
        </div>
      </section>

      <section class="operations-grid">
        <article class="panel category-panel">
          <div class="panel-heading">
            <div>
              <h2>参赛组别进度</h2>
              <p>按桌长已确认酒款统计</p>
            </div>
            <span>共 {{ categories.length }} 个参赛组别</span>
          </div>

          <div class="category-list">
            <div v-for="category in categories" :key="category.name" class="category-item">
              <div class="category-line">
                <div>
                  <span class="category-code">{{ category.code }}</span>
                  <strong>{{ category.name }}</strong>
                </div>
                <p>
                  <b>{{ category.finalizedCount }}</b>
                  / {{ category.totalCount }} 已确认
                  <em>{{ category.percent }}%</em>
                </p>
              </div>
              <div class="progress-track slim">
                <span :style="{ width: `${category.percent}%` }" />
              </div>
              <div class="category-breakdown" aria-label="按评审桌查看组别进度">
                <span v-for="table in category.tables" :key="`${category.code}-${table.name}`">
                  {{ table.name }} {{ table.finalized }} / {{ table.total }}
                </span>
              </div>
            </div>
          </div>
        </article>

        <article class="panel issue-panel">
          <div class="panel-heading">
            <div>
              <h2>待处理问题</h2>
              <p>优先处理会影响评审进度和结果发布的问题</p>
            </div>
            <div class="alert-counts">
              <span class="danger">{{ issueCounts.danger }} 个阻塞项</span>
              <span class="warn">{{ issueCounts.warning }} 个需关注</span>
            </div>
          </div>

          <div class="alert-list">
            <div v-for="issue in issues" :key="issue.message" :class="['alert-item', issue.level]">
              <span class="alert-icon">
                <component :is="issue.icon" />
              </span>
              <div class="alert-copy">
                <span>{{ issue.tableName }} · {{ issue.flightName }} · {{ issue.targetText }}</span>
                <strong>{{ issue.message }}</strong>
              </div>
              <em>{{ issue.timeText }}</em>
            </div>
          </div>

          <button class="quiet-action">
            <Document />
            查看处理清单
          </button>
        </article>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <h2>已选入围酒款</h2>
          <span>按评审桌和轮次展示，仅显示匿名编号</span>
        </div>
        <div class="advanced-grid">
          <article v-for="group in advancedGroups" :key="`${group.tableName}-${group.flightName}`" class="advanced-group">
            <header>
              <div>
                <h3>{{ group.tableName }} / {{ group.flightName }}</h3>
                <p>
                  已选 {{ group.selectedCount }} / 建议 {{ group.suggestedMin }}-{{ group.suggestedMax }}
                </p>
              </div>
              <span :class="['range-badge', group.rangeTone]">{{ group.rangeText }}</span>
            </header>

            <div class="candidate-list">
              <article v-for="entry in group.entries" :key="entry.uuid" class="candidate-card">
                <div>
                  <strong>{{ entry.uuid }}</strong>
                  <span>{{ entry.categoryName }}</span>
                </div>
                <p>
                  <small>桌长最终分</small>
                  <b>{{ entry.consensusScore }}</b>
                  <CircleCheck v-if="entry.confirmed" />
                </p>
              </article>
            </div>
          </article>
        </div>
        <p class="privacy-note">为保护盲评，仅展示酒款匿名编号，不展示酒名和厂商。</p>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <h2>快捷操作</h2>
          <span>比赛当天常用入口</span>
        </div>
        <div class="quick-grid">
          <button
            v-for="action in quickActions"
            :key="action.title"
            :class="['quick-card', { disabled: action.disabled }]"
            :disabled="action.disabled"
            :title="action.disabled ? action.disabledReason : action.title"
          >
            <component :is="action.icon" />
            <strong>{{ action.title }}</strong>
            <span>{{ action.disabled ? action.disabledReason : action.description }}</span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import {
  Box,
  CircleCheck,
  Clock,
  DataBoard,
  Document,
  Download,
  Medal,
  Promotion,
  Refresh,
  Setting,
  User,
  Warning,
} from '@element-plus/icons-vue'

const competition = {
  name: '2026 中国精酿啤酒大赛',
  date: '2026年5月30日',
  batch: '当前批次：第三批',
  currentStage: '评审进行中',
}

const summary = {
  totalEntries: 268,
  judgeSubmittedCount: 188,
  judgeSubmissionTotal: 268,
  captainFinalizedCount: 156,
  captainFinalizeTotal: 268,
  pendingFinalizeCount: 112,
  advancedCount: 32,
  openIssueCount: 5,
}

const canPublishResult = summary.pendingFinalizeCount === 0 && summary.openIssueCount === 0
const publishDisabledReason = canPublishResult ? '' : '全部酒款完成桌长确认，且待处理问题为 0 后可进入结果确认'

const topActions = [
  { label: '刷新数据', badge: '15 秒', icon: Refresh, tone: 'neutral' },
  { label: '导出当前批次评分', icon: Download, tone: 'neutral' },
  {
    label: '进入结果确认',
    icon: Promotion,
    tone: 'gold',
    disabled: !canPublishResult,
    disabledReason: publishDisabledReason,
  },
]

const summaryCards = [
  { label: '本场酒款', value: summary.totalEntries, hint: '当前批次评审酒款', icon: Box, tone: 'neutral' },
  {
    label: '评委评分已提交',
    value: `${summary.judgeSubmittedCount} / ${summary.judgeSubmissionTotal}`,
    hint: '评委已提交评分表',
    icon: DataBoard,
    tone: 'success',
  },
  {
    label: '桌长已确认',
    value: `${summary.captainFinalizedCount} / ${summary.captainFinalizeTotal}`,
    hint: '已生成桌长最终分',
    icon: CircleCheck,
    tone: 'success',
  },
  {
    label: '待桌长确认',
    value: summary.pendingFinalizeCount,
    hint: '仍需桌长确认最终分',
    icon: Clock,
    tone: 'warning',
  },
  { label: '已选入围', value: summary.advancedCount, hint: '各桌已选择入围酒款', icon: Medal, tone: 'gold' },
  { label: '待处理问题', value: summary.openIssueCount, hint: '影响评审进度的问题', icon: Warning, tone: 'danger' },
]

const judgeTables = [
  {
    tableName: 'A桌',
    captainName: '张明',
    memberCount: 4,
    currentFlightName: 'F3',
    flightCompletedCount: 2,
    flightTotalCount: 5,
    currentFlightFinalizedCount: 8,
    currentFlightEntryCount: 12,
    judgeSubmittedCount: 58,
    judgeSubmissionTotal: 68,
    captainFinalizedCount: 42,
    captainFinalizeTotal: 68,
    advancedSelectedCount: 2,
    advancedSuggestedMin: 2,
    advancedSuggestedMax: 4,
    lastFinalizeText: '2 分钟前',
    statusText: '推进正常',
    tone: 'success',
  },
  {
    tableName: 'B桌',
    captainName: '李华',
    memberCount: 4,
    currentFlightName: 'F4',
    flightCompletedCount: 3,
    flightTotalCount: 5,
    currentFlightFinalizedCount: 10,
    currentFlightEntryCount: 12,
    judgeSubmittedCount: 60,
    judgeSubmissionTotal: 65,
    captainFinalizedCount: 48,
    captainFinalizeTotal: 65,
    advancedSelectedCount: 5,
    advancedSuggestedMin: 2,
    advancedSuggestedMax: 4,
    lastFinalizeText: '1 分钟前',
    statusText: '需要处理',
    issueText: 'B桌 F4 已选 5 款入围，超过建议范围',
    tone: 'danger',
  },
  {
    tableName: 'C桌',
    captainName: '王芳',
    memberCount: 3,
    currentFlightName: 'F2',
    flightCompletedCount: 1,
    flightTotalCount: 5,
    currentFlightFinalizedCount: 7,
    currentFlightEntryCount: 12,
    judgeSubmittedCount: 47,
    judgeSubmissionTotal: 70,
    captainFinalizedCount: 35,
    captainFinalizeTotal: 70,
    advancedSelectedCount: 2,
    advancedSuggestedMin: 2,
    advancedSuggestedMax: 4,
    lastFinalizeText: '8 分钟前',
    statusText: '进度偏慢',
    issueText: 'C桌 F2 还有 3 款未完成评委评分',
    tone: 'warning',
  },
  {
    tableName: 'D桌',
    captainName: '陈强',
    memberCount: 4,
    currentFlightName: 'F2',
    flightCompletedCount: 1,
    flightTotalCount: 5,
    currentFlightFinalizedCount: 5,
    currentFlightEntryCount: 12,
    judgeSubmittedCount: 42,
    judgeSubmissionTotal: 65,
    captainFinalizedCount: 31,
    captainFinalizeTotal: 65,
    advancedSelectedCount: 2,
    advancedSuggestedMin: 2,
    advancedSuggestedMax: 4,
    lastFinalizeText: '18 分钟前',
    statusText: '需要处理',
    issueText: 'D桌 18 分钟没有新的桌长确认',
    tone: 'danger',
  },
].map((table) => ({
  ...table,
  judgeSubmittedPercent: Math.round((table.judgeSubmittedCount / table.judgeSubmissionTotal) * 100),
  captainFinalizedPercent: Math.round((table.captainFinalizedCount / table.captainFinalizeTotal) * 100),
}))

const categories = [
  {
    code: 'IPA',
    name: 'IPA',
    finalizedCount: 38,
    totalCount: 52,
    tables: [
      { name: 'A桌', finalized: 12, total: 16 },
      { name: 'B桌', finalized: 14, total: 18 },
      { name: 'D桌', finalized: 12, total: 18 },
    ],
  },
  {
    code: 'LAGER',
    name: '拉格',
    finalizedCount: 24,
    totalCount: 45,
    tables: [
      { name: 'A桌', finalized: 8, total: 15 },
      { name: 'B桌', finalized: 9, total: 14 },
      { name: 'C桌', finalized: 7, total: 16 },
    ],
  },
  {
    code: 'STOUT',
    name: '世涛',
    finalizedCount: 32,
    totalCount: 48,
    tables: [
      { name: 'A桌', finalized: 10, total: 16 },
      { name: 'B桌', finalized: 12, total: 16 },
      { name: 'D桌', finalized: 10, total: 16 },
    ],
  },
  {
    code: 'SOUR',
    name: '酸啤',
    finalizedCount: 18,
    totalCount: 35,
    tables: [
      { name: 'B桌', finalized: 6, total: 12 },
      { name: 'C桌', finalized: 7, total: 11 },
      { name: 'D桌', finalized: 5, total: 12 },
    ],
  },
  {
    code: 'WHEAT',
    name: '小麦',
    finalizedCount: 28,
    totalCount: 42,
    tables: [
      { name: 'A桌', finalized: 9, total: 14 },
      { name: 'C桌', finalized: 10, total: 14 },
      { name: 'D桌', finalized: 9, total: 14 },
    ],
  },
  {
    code: 'EXP',
    name: '实验风格',
    finalizedCount: 16,
    totalCount: 46,
    tables: [
      { name: 'A桌', finalized: 5, total: 15 },
      { name: 'C桌', finalized: 6, total: 16 },
      { name: 'D桌', finalized: 5, total: 15 },
    ],
  },
].map((category) => ({
  ...category,
  percent: Math.round((category.finalizedCount / category.totalCount) * 100),
}))

const issues = [
  {
    level: 'danger',
    tableName: 'D桌',
    flightName: 'F2',
    targetText: '桌长确认',
    message: '18 分钟没有新的桌长确认',
    timeText: '刚刚',
    icon: Warning,
  },
  {
    level: 'warning',
    tableName: 'C桌',
    flightName: 'F2',
    targetText: '评委评分',
    message: '还有 3 款酒缺评委评分',
    timeText: '3 分钟前',
    icon: Clock,
  },
  {
    level: 'warning',
    tableName: 'A桌',
    flightName: '评委王芳',
    targetText: '评语',
    message: '2 条评语不符合提交要求',
    timeText: '5 分钟前',
    icon: Document,
  },
  {
    level: 'danger',
    tableName: 'D桌',
    flightName: 'F2',
    targetText: 'BC-2026-STOUT-0042',
    message: '缺少桌长最终分',
    timeText: '8 分钟前',
    icon: Warning,
  },
  {
    level: 'neutral',
    tableName: 'B桌',
    flightName: 'F4',
    targetText: '入围选择',
    message: '入围数量为 5 款，建议调整为 2-4 款',
    timeText: '12 分钟前',
    icon: Medal,
  },
]

const issueCounts = {
  danger: issues.filter((issue) => issue.level === 'danger').length,
  warning: issues.filter((issue) => issue.level === 'warning').length,
}

const advancedGroups = [
  {
    tableName: 'A桌',
    flightName: 'F3',
    selectedCount: 2,
    suggestedMin: 2,
    suggestedMax: 4,
    entries: [
      { uuid: 'BC-2026-IPA-0001', categoryName: 'IPA', consensusScore: 92, confirmed: true },
      { uuid: 'BC-2026-IPA-0017', categoryName: 'IPA', consensusScore: 89, confirmed: true },
    ],
  },
  {
    tableName: 'B桌',
    flightName: 'F4',
    selectedCount: 5,
    suggestedMin: 2,
    suggestedMax: 4,
    entries: [
      { uuid: 'BC-2026-STOUT-0008', categoryName: '世涛', consensusScore: 91, confirmed: true },
      { uuid: 'BC-2026-LAGER-0023', categoryName: '拉格', consensusScore: 88, confirmed: false },
      { uuid: 'BC-2026-IPA-0031', categoryName: 'IPA', consensusScore: 90, confirmed: true },
    ],
  },
  {
    tableName: 'C桌',
    flightName: 'F2',
    selectedCount: 2,
    suggestedMin: 2,
    suggestedMax: 4,
    entries: [
      { uuid: 'BC-2026-SOUR-0005', categoryName: '酸啤', consensusScore: 90, confirmed: true },
      { uuid: 'BC-2026-WHEAT-0012', categoryName: '小麦', consensusScore: 87, confirmed: false },
    ],
  },
  {
    tableName: 'D桌',
    flightName: 'F2',
    selectedCount: 2,
    suggestedMin: 2,
    suggestedMax: 4,
    entries: [
      { uuid: 'BC-2026-EXP-0003', categoryName: '实验风格', consensusScore: 93, confirmed: true },
      { uuid: 'BC-2026-IPA-0029', categoryName: 'IPA', consensusScore: 86, confirmed: false },
    ],
  },
].map((group) => {
  const inRange = group.selectedCount >= group.suggestedMin && group.selectedCount <= group.suggestedMax

  return {
    ...group,
    rangeTone: inRange ? 'success' : 'danger',
    rangeText: inRange ? '数量合规' : '需要调整',
  }
})

const quickActions = [
  { title: '查看评分明细', description: '查看评委评分与桌长最终分', icon: Document },
  { title: '调整评审桌人员', description: '处理现场人员变动', icon: User },
  { title: '查看评分规则', description: '查看本场评分项目与满分', icon: Setting },
  { title: '导出现场数据', description: '下载评委评分、桌长最终分和入围记录', icon: Download },
  {
    title: '进入结果确认',
    description: '确认奖项后发布比赛结果',
    icon: Promotion,
    disabled: !canPublishResult,
    disabledReason: `还有 ${summary.pendingFinalizeCount} 款酒等待桌长确认`,
  },
]
</script>

<style scoped>
.live-dashboard {
  --bg: #0e1418;
  --panel: rgba(22, 32, 36, 0.9);
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
    radial-gradient(circle at 16% 4%, rgba(216, 169, 53, 0.12), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 48%, #0d151a 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.live-dashboard::before,
.live-dashboard::after {
  position: absolute;
  content: "";
  pointer-events: none;
}

.live-dashboard::before {
  inset: 0;
  background: linear-gradient(90deg, rgba(216, 169, 53, 0.04), transparent 18%, transparent 82%, rgba(111, 207, 122, 0.035));
}

.live-dashboard::after {
  width: 240px;
  height: 240px;
  top: -120px;
  right: 18%;
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.06);
  filter: blur(22px);
}

.live-dashboard > * {
  position: relative;
  z-index: 1;
}

.hero-panel,
.panel,
.metric-card,
.table-card,
.candidate-card,
.advanced-group,
.quick-card {
  border: 1px solid var(--line);
  background: var(--panel);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(14px);
}

.hero-panel {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: flex-start;
  padding: 22px 24px;
  border-radius: 22px;
}

.hero-main {
  min-width: 0;
}

.scroll-fade {
  flex: 0 0 34px;
  height: 34px;
  margin: 0 -26px -34px;
  z-index: 3;
  background:
    linear-gradient(180deg, rgba(14, 20, 24, 0.96) 0%, rgba(14, 20, 24, 0.72) 42%, rgba(14, 20, 24, 0) 100%);
  pointer-events: none;
}

.dashboard-scroll {
  flex: 1 1 auto;
  min-height: 0;
  margin: 0 -26px;
  padding: 34px 26px 36px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.dashboard-scroll::-webkit-scrollbar {
  width: 10px;
}

.dashboard-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.dashboard-scroll::-webkit-scrollbar-thumb {
  border: 2px solid rgba(14, 20, 24, 0.9);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.28);
}

.dashboard-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(216, 169, 53, 0.42);
}

.eyebrow,
.section-heading span,
.panel-heading p,
.meta-row,
.metric-card p,
.metric-card span,
.table-title p,
.flight-row,
.table-meta,
.privacy-note,
.quick-card span,
.current-flight,
.category-breakdown,
.advanced-group header p,
.alert-copy span {
  color: var(--muted);
}

.eyebrow {
  margin: 0 0 8px;
  letter-spacing: 0.12em;
  font-size: 12px;
}

.title-row,
.meta-row,
.toolbar,
.section-heading,
.panel-heading,
.progress-head,
.table-meta,
.category-line,
.alert-item,
.candidate-card,
.quick-grid {
  display: flex;
  align-items: center;
}

.title-row {
  gap: 14px;
  flex-wrap: wrap;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  font-size: clamp(28px, 2.2vw, 40px);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

h2 {
  font-size: 18px;
  font-weight: 700;
}

h3 {
  font-size: 20px;
  letter-spacing: -0.02em;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  color: var(--green);
  background: rgba(111, 207, 122, 0.1);
  border: 1px solid rgba(111, 207, 122, 0.2);
  font-weight: 700;
}

.status-pill span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--green);
}

.meta-row {
  gap: 16px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.toolbar-button,
.state-badge,
.category-code,
.alert-counts span,
.range-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.035);
}

svg {
  width: 1em;
  height: 1em;
}

.toolbar {
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  max-width: 380px;
}

button {
  font: inherit;
}

.toolbar-button,
.quick-card,
.quiet-action {
  cursor: pointer;
  color: var(--text);
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease, opacity 0.18s ease;
}

.toolbar-button {
  min-height: 44px;
  padding: 0 14px;
}

.toolbar-button:hover:not(.disabled),
.quick-card:hover:not(.disabled),
.quiet-action:hover {
  transform: translateY(-1px);
  border-color: rgba(224, 184, 74, 0.36);
}

.toolbar-button strong {
  padding: 3px 8px;
  border-radius: 8px;
  color: var(--gold-soft);
  background: rgba(216, 169, 53, 0.12);
}

.toolbar-button.gold {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
}

.toolbar-button.disabled,
.quick-card.disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.section-block {
  margin-top: 26px;
}

.first-section {
  margin-top: 0;
}

.section-heading {
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 0 2px;
}

.section-heading::after,
.panel-heading::after {
  content: "";
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--line), transparent);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(160px, 1fr));
  gap: 14px;
}

.metric-card {
  display: flex;
  justify-content: space-between;
  min-height: 148px;
  padding: 22px;
  border-radius: 18px;
}

.metric-card strong {
  display: block;
  margin: 12px 0 10px;
  font-size: 34px;
  line-height: 1;
  letter-spacing: -0.04em;
}

.metric-card.success strong,
.metric-card.success .metric-icon {
  color: var(--green);
}

.metric-card.warning strong,
.metric-card.warning .metric-icon {
  color: var(--orange);
}

.metric-card.gold strong,
.metric-card.gold .metric-icon {
  color: var(--gold-soft);
}

.metric-card.danger strong,
.metric-card.danger .metric-icon {
  color: var(--red);
}

.metric-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 44px;
  height: 44px;
  color: #9caab1;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
}

.metric-icon svg {
  width: 22px;
  height: 22px;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(270px, 1fr));
  gap: 16px;
}

.table-card {
  min-height: 390px;
  padding: 22px;
  border-radius: 18px;
}

.table-card header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.table-card.success {
  border-color: rgba(111, 207, 122, 0.2);
}

.table-card.warning {
  border-color: rgba(242, 153, 74, 0.34);
}

.table-card.danger {
  border-color: rgba(224, 82, 82, 0.45);
}

.table-title {
  display: flex;
  gap: 14px;
}

.table-letter {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 42px;
  height: 42px;
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
}

.table-title p {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.state-badge {
  align-self: flex-start;
  padding: 6px 10px;
  font-weight: 700;
  white-space: nowrap;
}

.success .state-badge {
  color: var(--green);
  background: rgba(111, 207, 122, 0.1);
  border-color: rgba(111, 207, 122, 0.22);
}

.warning .state-badge {
  color: var(--orange);
  background: rgba(242, 153, 74, 0.11);
  border-color: rgba(242, 153, 74, 0.26);
}

.danger .state-badge {
  color: var(--red);
  background: rgba(224, 82, 82, 0.1);
  border-color: rgba(224, 82, 82, 0.24);
}

.flight-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  margin-top: 26px;
}

.flight-cups {
  display: flex;
  gap: 7px;
  min-width: 0;
}

.flight-cups span {
  width: 18px;
  height: 26px;
  border: 1px solid rgba(141, 161, 170, 0.14);
  border-radius: 4px 4px 7px 7px;
  background: rgba(255, 255, 255, 0.025);
}

.flight-cups span.active {
  border-color: rgba(111, 207, 122, 0.5);
  background: linear-gradient(180deg, rgba(111, 207, 122, 0.28), rgba(111, 207, 122, 0.12));
}

.flight-cups span.current {
  border-color: rgba(224, 184, 74, 0.58);
  background: linear-gradient(180deg, rgba(224, 184, 74, 0.32), rgba(216, 169, 53, 0.18));
}

.current-flight {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(224, 184, 74, 0.16);
  border-radius: 12px;
  background: rgba(216, 169, 53, 0.06);
}

.progress-stack {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}

.progress-head {
  justify-content: space-between;
  color: #b6c5cb;
}

.progress-head strong {
  font-size: 18px;
  color: #f1f7f8;
}

.progress-track {
  height: 10px;
  margin-top: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.055);
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #d2a247, #ebce6a);
}

.progress-block:first-child .progress-track span {
  background: linear-gradient(90deg, #6fcf7a, #a7dc87);
}

.progress-track.slim {
  height: 8px;
  margin-top: 10px;
}

.table-meta {
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.table-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.table-meta span:first-child {
  color: var(--gold-soft);
}

.table-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  padding: 12px;
  color: #ff9089;
  border-radius: 12px;
  background: rgba(224, 82, 82, 0.1);
}

.operations-grid {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(360px, 1fr);
  gap: 20px;
  margin-top: 28px;
}

.panel {
  border-radius: 18px;
  padding: 22px;
}

.panel-heading {
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.panel-heading > div:first-child {
  min-width: max-content;
}

.panel-heading p {
  margin-top: 6px;
}

.panel-heading > span {
  color: var(--muted);
  white-space: nowrap;
}

.category-list,
.alert-list {
  display: grid;
  gap: 15px;
}

.category-line {
  justify-content: space-between;
  gap: 18px;
}

.category-line > div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-code {
  min-width: 72px;
  justify-content: center;
  padding: 5px 8px;
  color: #93a8b0;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.category-line p {
  color: #a8bac1;
}

.category-line b {
  color: #eaf2f4;
}

.category-line em {
  margin-left: 18px;
  color: var(--gold-soft);
  font-style: normal;
  font-weight: 700;
}

.category-breakdown {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.category-breakdown span {
  padding: 5px 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.alert-counts {
  display: flex;
  gap: 8px;
}

.alert-counts span {
  padding: 5px 10px;
  font-size: 13px;
  font-weight: 800;
}

.alert-counts .danger {
  color: var(--red);
  background: rgba(224, 82, 82, 0.11);
}

.alert-counts .warn {
  color: var(--orange);
  background: rgba(242, 153, 74, 0.1);
}

.alert-item {
  min-height: 64px;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.025);
}

.alert-item::before {
  content: "";
  width: 4px;
  align-self: stretch;
  border-radius: 999px;
  background: #71858d;
}

.alert-item.danger::before {
  background: var(--red);
}

.alert-item.warning::before {
  background: var(--orange);
}

.alert-copy {
  display: grid;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.alert-copy strong {
  color: #edf5f7;
}

.alert-item em {
  color: var(--muted);
  font-style: normal;
  white-space: nowrap;
}

.alert-icon {
  color: #9eb0b7;
}

.alert-item.danger .alert-icon {
  color: var(--red);
}

.alert-item.warning .alert-icon {
  color: var(--orange);
}

.quiet-action {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  padding: 10px 14px;
  color: #bac8ce;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.035);
}

.advanced-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(280px, 1fr));
  gap: 14px;
}

.advanced-group {
  padding: 18px;
  border-color: rgba(216, 169, 53, 0.16);
  border-radius: 16px;
}

.advanced-group header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.advanced-group header p {
  margin-top: 6px;
}

.range-badge {
  padding: 6px 10px;
  white-space: nowrap;
  font-weight: 700;
}

.range-badge.success {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.1);
}

.range-badge.danger {
  color: var(--red);
  border-color: rgba(224, 82, 82, 0.24);
  background: rgba(224, 82, 82, 0.1);
}

.candidate-list {
  display: grid;
  gap: 10px;
}

.candidate-card {
  justify-content: space-between;
  gap: 14px;
  min-height: 62px;
  padding: 12px 14px;
  border-color: rgba(216, 169, 53, 0.12);
  border-radius: 14px;
  box-shadow: none;
}

.candidate-card > div {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.candidate-card strong {
  color: var(--gold);
  font-family: "Consolas", "SFMono-Regular", monospace;
  letter-spacing: 0.02em;
}

.candidate-card span {
  padding: 4px 9px;
  color: #9cb0b8;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.candidate-card p {
  display: flex;
  align-items: center;
  gap: 10px;
}

.candidate-card small {
  color: var(--muted);
}

.candidate-card b {
  font-size: 22px;
  color: #e9f1f3;
}

.candidate-card svg {
  color: var(--green);
}

.privacy-note {
  margin-top: 14px;
  text-align: center;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 12px;
}

.quick-card {
  display: grid;
  gap: 8px;
  min-height: 116px;
  padding: 18px;
  text-align: left;
  border-radius: 14px;
}

.quick-card svg {
  width: 24px;
  height: 24px;
  color: #acbac1;
}

.quick-card:last-child svg,
.quick-card:last-child strong {
  color: var(--gold-soft);
}

@media (max-width: 1500px) {
  .summary-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .table-grid {
    grid-template-columns: repeat(2, minmax(260px, 1fr));
  }
}

@media (max-width: 1100px) {
  .live-dashboard {
    padding: 18px;
  }

  .hero-panel {
    grid-template-columns: 1fr;
  }

  .toolbar {
    justify-content: flex-start;
    max-width: none;
  }

  .operations-grid,
  .quick-grid,
  .summary-grid,
  .table-grid,
  .advanced-grid {
    grid-template-columns: 1fr;
  }

  .panel-heading,
  .section-heading,
  .flight-row,
  .category-line,
  .alert-item,
  .candidate-card,
  .advanced-group header {
    align-items: flex-start;
  }

  .flight-row,
  .category-line,
  .candidate-card,
  .advanced-group header {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
