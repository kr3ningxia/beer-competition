<template>
  <div class="live-dashboard">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">现场评审控制台</p>
        <div class="title-row">
          <h1>{{ competition.name }}</h1>
          <span class="status-pill live">
            <span />
            {{ competition.status }}
          </span>
        </div>
        <div class="meta-row">
          <span>{{ competition.date }}</span>
          <span>{{ competition.batch }}</span>
          <span class="time-chip">
            <Clock />
            最后更新：{{ competition.updatedAt }}
          </span>
        </div>
      </div>

      <div class="toolbar" aria-label="现场操作">
        <button v-for="action in topActions" :key="action.label" :class="['toolbar-button', action.tone]">
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
          <span>比赛现场关键数据</span>
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
          <span>已评审 = 桌长已汇总</span>
        </div>
        <div class="table-grid">
          <article v-for="table in judgeTables" :key="table.name" :class="['table-card', table.tone]">
            <header>
              <div class="table-title">
                <span class="table-letter">{{ table.letter }}</span>
                <div>
                  <h3>{{ table.name }}</h3>
                  <p>
                    <User />
                    桌长：{{ table.captain }} · {{ table.members }} 人
                  </p>
                </div>
              </div>
              <span class="state-badge">{{ table.status }}</span>
            </header>

            <div class="flight-row">
              <span>Flight 进度</span>
              <div class="flight-cups" aria-label="Flight 进度">
                <span
                  v-for="index in table.flightTotal"
                  :key="index"
                  :class="{ active: index <= table.flightCurrent }"
                />
              </div>
              <strong>当前 F{{ table.flightCurrent }}</strong>
            </div>

            <div class="progress-head">
              <span>评审进度</span>
              <strong>{{ table.completed }} / {{ table.total }}</strong>
            </div>
            <div class="progress-track">
              <span :style="{ width: `${table.percent}%` }" />
            </div>
            <div class="table-meta">
              <span>{{ table.percent }}%</span>
              <span>
                <Medal />
                已勾选 {{ table.advanced }} 款
              </span>
              <span>
                <Clock />
                {{ table.lastSubmit }}
              </span>
            </div>

            <p v-if="table.warning" class="table-warning">
              <Warning />
              {{ table.warning }}
            </p>
          </article>
        </div>
      </section>

      <section class="operations-grid">
        <article class="panel category-panel">
          <div class="panel-heading">
            <div>
              <h2>投报组别进度</h2>
              <p>按桌长最终汇总统计</p>
            </div>
            <span>共 {{ categories.length }} 个投报组别</span>
          </div>

          <div class="category-list">
            <div v-for="category in categories" :key="category.name" class="category-item">
              <div class="category-line">
                <div>
                  <span class="category-code">{{ category.code }}</span>
                  <strong>{{ category.name }}</strong>
                </div>
                <p>
                  <b>{{ category.completed }}</b>
                  / {{ category.total }}
                  <em>{{ category.percent }}%</em>
                </p>
              </div>
              <div class="progress-track slim">
                <span :style="{ width: `${category.percent}%` }" />
              </div>
            </div>
          </div>
        </article>

        <article class="panel alert-panel">
          <div class="panel-heading">
            <div>
              <h2>异常提醒</h2>
              <p>优先处理影响现场推进的问题</p>
            </div>
            <div class="alert-counts">
              <span class="danger">2 严重</span>
              <span class="warn">2 警告</span>
            </div>
          </div>

          <div class="alert-list">
            <div v-for="alert in alerts" :key="alert.text" :class="['alert-item', alert.level]">
              <span class="alert-icon">
                <component :is="alert.icon" />
              </span>
              <strong>{{ alert.text }}</strong>
              <em>{{ alert.time }}</em>
            </div>
          </div>

          <button class="quiet-action">
            <CircleCheck />
            全部标记已处理
          </button>
        </article>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <h2>晋级候选</h2>
          <span>匿名评审，仅显示酒款 UUID</span>
        </div>
        <div class="candidate-list">
          <article v-for="candidate in candidates" :key="candidate.uuid" class="candidate-card">
            <div>
              <strong>{{ candidate.uuid }}</strong>
              <span>{{ candidate.category }}</span>
              <span>{{ candidate.table }}</span>
            </div>
            <p>
              <small>共识分</small>
              <b>{{ candidate.score }}</b>
              <CircleCheck v-if="candidate.confirmed" />
            </p>
          </article>
        </div>
        <p class="privacy-note">现场匿名评审：仅展示酒款 UUID，不展示酒名和厂商。</p>
      </section>

      <section class="section-block">
        <div class="section-heading">
          <h2>快捷操作</h2>
          <span>比赛当天常用入口</span>
        </div>
        <div class="quick-grid">
          <button v-for="action in quickActions" :key="action.title" class="quick-card">
            <component :is="action.icon" />
            <strong>{{ action.title }}</strong>
            <span>{{ action.description }}</span>
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
  CoffeeCup,
  DataBoard,
  Document,
  Download,
  Medal,
  Promotion,
  Refresh,
  Setting,
  Trophy,
  User,
  Warning,
} from '@element-plus/icons-vue'

const competition = {
  name: '2026 中国精酿啤酒大赛',
  date: '2026年5月30日',
  batch: '第三批次',
  status: '评审进行中',
  updatedAt: '12:19:53',
}

const topActions = [
  { label: '刷新数据', badge: '15s', icon: Refresh, tone: 'neutral' },
  { label: '导出 Excel', icon: Download, tone: 'neutral' },
  { label: '评分配置', icon: Setting, tone: 'neutral' },
  { label: '发布结果', icon: Promotion, tone: 'gold' },
]

const summaryCards = [
  { label: '总酒款数', value: '268', hint: '本场次投报', icon: Box, tone: 'neutral' },
  { label: '已评审', value: '156', hint: '+18 较 10 分钟前', icon: CircleCheck, tone: 'success' },
  { label: '待评审', value: '112', hint: '等待桌长汇总', icon: Clock, tone: 'warning' },
  { label: '晋级候选', value: '32', hint: '各桌已勾选', icon: Medal, tone: 'gold' },
  { label: '异常提醒', value: '5', hint: '需现场确认', icon: Warning, tone: 'danger' },
  { label: '当前完成率', value: '58%', hint: '+5% 较 10 分钟前', icon: DataBoard, tone: 'gold' },
]

const judgeTables = [
  {
    letter: 'A',
    name: 'A桌',
    captain: '张明',
    members: 4,
    status: '正常',
    tone: 'success',
    flightCurrent: 3,
    flightTotal: 5,
    completed: 42,
    total: 68,
    percent: 62,
    advanced: 8,
    lastSubmit: '2 分钟前',
  },
  {
    letter: 'B',
    name: 'B桌',
    captain: '李华',
    members: 4,
    status: '正常',
    tone: 'success',
    flightCurrent: 4,
    flightTotal: 5,
    completed: 48,
    total: 65,
    percent: 74,
    advanced: 11,
    lastSubmit: '1 分钟前',
  },
  {
    letter: 'C',
    name: 'C桌',
    captain: '王芳',
    members: 3,
    status: '稍慢',
    tone: 'warning',
    flightCurrent: 2,
    flightTotal: 5,
    completed: 35,
    total: 70,
    percent: 50,
    advanced: 6,
    lastSubmit: '8 分钟前',
  },
  {
    letter: 'D',
    name: 'D桌',
    captain: '陈强',
    members: 4,
    status: '异常',
    tone: 'danger',
    flightCurrent: 2,
    flightTotal: 5,
    completed: 31,
    total: 65,
    percent: 48,
    advanced: 7,
    lastSubmit: '18 分钟前',
    warning: '超过 18 分钟无桌长汇总',
  },
]

const categories = [
  { code: 'IPA', name: 'IPA', completed: 38, total: 52, percent: 73 },
  { code: 'LAGER', name: '拉格', completed: 24, total: 45, percent: 53 },
  { code: 'STOUT', name: '世涛', completed: 32, total: 48, percent: 67 },
  { code: 'SOUR', name: '酸啤', completed: 18, total: 35, percent: 51 },
  { code: 'WHEAT', name: '小麦', completed: 28, total: 42, percent: 67 },
  { code: 'EXP', name: '实验风格', completed: 16, total: 46, percent: 35 },
]

const alerts = [
  { text: 'D桌超过 18 分钟无桌长汇总', time: '刚刚', icon: Warning, level: 'danger' },
  { text: 'C桌 Flight 02 仍有 3 款待评审', time: '3 分钟前', icon: Clock, level: 'warning' },
  { text: '评审王某有 2 条评语字数不足', time: '5 分钟前', icon: Document, level: 'warning' },
  { text: 'BC-2026-STOUT-0042 缺少桌长最终分', time: '8 分钟前', icon: Warning, level: 'danger' },
  { text: 'B桌晋级数量超过建议范围', time: '12 分钟前', icon: Medal, level: 'neutral' },
]

const candidates = [
  { uuid: 'BC-2026-IPA-0001', category: 'IPA', table: 'A桌', score: 92, confirmed: true },
  { uuid: 'BC-2026-IPA-0017', category: 'IPA', table: 'A桌', score: 89, confirmed: true },
  { uuid: 'BC-2026-STOUT-0008', category: '世涛', table: 'B桌', score: 91, confirmed: true },
  { uuid: 'BC-2026-LAGER-0023', category: '拉格', table: 'B桌', score: 88, confirmed: false },
  { uuid: 'BC-2026-SOUR-0005', category: '酸啤', table: 'C桌', score: 90, confirmed: true },
  { uuid: 'BC-2026-WHEAT-0012', category: '小麦', table: 'C桌', score: 87, confirmed: false },
  { uuid: 'BC-2026-EXP-0003', category: '实验风格', table: 'D桌', score: 93, confirmed: true },
  { uuid: 'BC-2026-IPA-0029', category: 'IPA', table: 'D桌', score: 86, confirmed: false },
]

const quickActions = [
  { title: '查看全部评分记录', description: '浏览所有已提交的评分', icon: Document },
  { title: '管理评审分配', description: '调整评审桌人员配置', icon: User },
  { title: '调整评分维度', description: '修改评分项目与权重', icon: Setting },
  { title: '导出原始分与最终分', description: '下载完整评分数据', icon: Download },
  { title: '进入结果发布', description: '准备发布比赛结果', icon: Promotion },
]
</script>

<style scoped>
.live-dashboard {
  --bg: #0e1418;
  --panel: rgba(22, 32, 36, 0.9);
  --panel-soft: rgba(19, 29, 33, 0.82);
  --line: rgba(219, 232, 237, 0.1);
  --line-strong: rgba(224, 184, 74, 0.28);
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
.quick-card {
  border: 1px solid var(--line);
  background: var(--panel);
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(14px);
}

.hero-panel {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  padding: 22px 24px;
  border-radius: 22px;
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
.quick-card span {
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
  font-size: 24px;
  letter-spacing: -0.03em;
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

.time-chip,
.toolbar-button,
.state-badge,
.category-code,
.alert-counts span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.035);
}

.time-chip {
  padding: 8px 12px;
  color: #adbdc4;
}

svg {
  width: 1em;
  height: 1em;
}

.toolbar {
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

button {
  font: inherit;
}

.toolbar-button,
.quick-card,
.quiet-action {
  cursor: pointer;
  color: var(--text);
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.toolbar-button {
  min-height: 44px;
  padding: 0 14px;
}

.toolbar-button:hover,
.quick-card:hover,
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
  font-size: 38px;
  line-height: 1;
  letter-spacing: -0.06em;
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
  grid-template-columns: repeat(4, minmax(250px, 1fr));
  gap: 16px;
}

.table-card {
  min-height: 332px;
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
  gap: 14px;
  margin-top: 28px;
}

.flight-cups {
  display: flex;
  gap: 7px;
}

.flight-cups span {
  width: 18px;
  height: 26px;
  border: 1px solid rgba(141, 161, 170, 0.14);
  border-radius: 4px 4px 7px 7px;
  background: rgba(255, 255, 255, 0.025);
}

.flight-cups span.active {
  border-color: rgba(224, 184, 74, 0.58);
  background: linear-gradient(180deg, rgba(224, 184, 74, 0.32), rgba(216, 169, 53, 0.18));
}

.progress-head {
  justify-content: space-between;
  margin-top: 24px;
  color: #b6c5cb;
}

.progress-head strong {
  font-size: 20px;
  color: #f1f7f8;
}

.progress-track {
  height: 10px;
  margin-top: 12px;
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

.progress-track.slim {
  height: 8px;
  margin-top: 10px;
}

.table-meta {
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.table-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.table-meta span:nth-child(2) {
  color: var(--gold-soft);
}

.table-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 22px;
  padding: 12px;
  color: #ff7b74;
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
  min-height: 60px;
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

.alert-item strong {
  flex: 1;
}

.alert-item em {
  color: var(--muted);
  font-style: normal;
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

.candidate-list {
  display: grid;
  gap: 10px;
}

.candidate-card {
  justify-content: space-between;
  gap: 14px;
  min-height: 70px;
  padding: 14px 18px;
  border-color: rgba(216, 169, 53, 0.16);
  border-radius: 14px;
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
  font-size: 26px;
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

  .hero-panel,
  .operations-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    display: grid;
  }

  .toolbar {
    justify-content: flex-start;
  }

  .operations-grid,
  .quick-grid,
  .summary-grid,
  .table-grid {
    grid-template-columns: 1fr;
  }
}
</style>
