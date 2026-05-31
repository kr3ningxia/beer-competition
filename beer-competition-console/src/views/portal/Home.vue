<template>
  <div class="home-page">
    <section class="entry-hero">
      <div class="hero-copy">
        <span class="label-chip tone-green">{{ activeCompetition.stage }}</span>
        <h1>{{ activeCompetition.name }}</h1>
        <p>{{ activeCompetition.description }}</p>
        <div class="hero-facts" aria-label="赛事关键信息">
          <span>比赛日期 {{ activeCompetition.date }}</span>
          <span>{{ activeCompetition.categories.length }} 个投递组别</span>
          <span>报名费 ¥{{ activeCompetition.entryFee }} / 款</span>
        </div>
        <div class="hero-actions">
          <RouterLink class="primary-action" :to="heroAction.to">{{ heroAction.label }}</RouterLink>
          <RouterLink class="secondary-action" to="/portal/entries">查看我的酒款</RouterLink>
        </div>
      </div>

      <aside class="event-ticket" aria-label="赛事报名信息">
        <span>{{ activeCompetition.shortName }}</span>
        <dl>
          <div>
            <dt>比赛日期</dt>
            <dd>{{ activeCompetition.date }}</dd>
          </div>
          <div>
            <dt>报名截止</dt>
            <dd>{{ activeCompetition.registrationDeadline }}</dd>
          </div>
          <div>
            <dt>报名费</dt>
            <dd>¥{{ activeCompetition.entryFee }} / 款</dd>
          </div>
          <div>
            <dt>比赛地点</dt>
            <dd>{{ activeCompetition.city }} · {{ activeCompetition.venue }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <section class="events-panel brewer-card">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">开放报名赛事</h2>
          <p>先确认要参加的赛事，再进入提交、付款、标签和寄样流程。</p>
        </div>
        <RouterLink to="/portal/events">全部赛事</RouterLink>
      </div>

      <div class="event-grid">
        <article v-for="competition in openCompetitions" :key="competition.id" class="open-event-card">
          <div>
            <span :class="['label-chip', competition.id === activeCompetition.id ? 'tone-green' : 'tone-amber']">
              {{ competition.id === activeCompetition.id ? '推荐报名' : competition.stage }}
            </span>
            <h3>{{ competition.name }}</h3>
            <p>{{ competition.city }} · {{ competition.venue }}</p>
          </div>
          <div class="event-card-meta">
            <span><small>报名截止</small><b>{{ competition.registrationDeadline }}</b></span>
            <span><small>报名费</small><b>¥{{ competition.entryFee }} / 款</b></span>
            <span><small>我的报名</small><b>{{ entryCount(competition.id) }} 款</b></span>
          </div>
          <div class="event-card-actions">
            <RouterLink :to="`/portal/events/${competition.id}`">查看赛事</RouterLink>
            <RouterLink class="event-primary" to="/portal/submit">提交酒款</RouterLink>
          </div>
        </article>
      </div>
    </section>

    <section class="next-panel brewer-card">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">我的下一步</h2>
          <p>{{ breweryProfile.breweryName }} 当前有 {{ entries.length }} 款参赛酒，优先处理会影响参赛完成度的事项。</p>
        </div>
        <RouterLink to="/portal/entries">查看全部酒款</RouterLink>
      </div>

      <div class="next-grid">
        <RouterLink v-for="item in nextActions" :key="item.label" :to="item.to" class="next-card">
          <component :is="item.icon" />
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
          <p>{{ item.hint }}</p>
        </RouterLink>
      </div>
    </section>

    <section class="content-grid">
      <article class="flow-card brewer-card">
        <div class="section-head">
          <h2 class="portal-section-title">报名流程</h2>
          <RouterLink to="/portal/payment">付款与标签</RouterLink>
        </div>
        <div class="flow-line">
          <div
            v-for="step in flowSteps"
            :key="step.key"
            :class="['flow-step', { active: step.active, done: step.done }]"
          >
            <span>{{ step.index }}</span>
            <strong>{{ step.label }}</strong>
            <small>{{ step.hint }}</small>
          </div>
        </div>
      </article>

      <article class="rules-card brewer-card">
        <h2 class="portal-section-title">赛事规则摘要</h2>
        <dl>
          <div>
            <dt>投递组别</dt>
            <dd>{{ activeCompetition.categories.join(' / ') }}</dd>
          </div>
          <div>
            <dt>基础风格</dt>
            <dd>{{ activeCompetition.styleOptions.join(' / ') }}</dd>
          </div>
          <div>
            <dt>额外字段</dt>
            <dd>{{ activeCompetition.entryFields.join(' / ') }}</dd>
          </div>
          <div>
            <dt>现场标签</dt>
            <dd>付款后下载 UUID 标签，建议使用 70mm × 90mm 标签纸，贴在酒瓶或外箱醒目位置。</dd>
          </div>
        </dl>
      </article>
    </section>

    <section class="entry-strip">
      <div class="section-head">
        <h2 class="portal-section-title">最近报名记录</h2>
        <RouterLink class="submit-link" to="/portal/submit">继续提交</RouterLink>
      </div>
      <div class="entry-cards">
        <article v-for="entry in entries.slice(0, 3)" :key="entry.id" class="beer-label-card">
          <div>
            <span :class="['label-chip', `tone-${statusMeta[entry.status].tone}`]">
              {{ statusMeta[entry.status].label }}
            </span>
            <h3>{{ entry.name }}</h3>
            <p>{{ entry.categoryName }} · {{ entry.style }} · {{ entry.abv }}</p>
          </div>
          <div class="uuid-ticket">
            <span>UUID</span>
            <strong>{{ entry.uuid }}</strong>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { CircleCheck, Money, Tickets } from '@element-plus/icons-vue'
import { activeCompetition, breweryProfile, competitions, entries, statusMeta } from './mockData'

const paidEntries = computed(() => entries.filter((entry) => entry.paymentStatus === 'PAID'))
const unpaidEntries = computed(() => entries.filter((entry) => entry.status === 'PENDING_PAYMENT'))
const storedEntries = computed(() => entries.filter((entry) => entry.status === 'STORED' || entry.status === 'RESULT_PUBLISHED'))
const resultEntries = computed(() => entries.filter((entry) => entry.status === 'RESULT_PUBLISHED'))
const labelReadyEntries = computed(() => paidEntries.value.filter((entry) => !entry.storedAt))
const openCompetitions = computed(() => competitions.filter((competition) => competition.status === 'REGISTRATION_OPEN'))

const heroAction = computed(() => {
  if (unpaidEntries.value.length > 0) {
    return { label: '去付款', to: '/portal/payment' }
  }
  if (labelReadyEntries.value.length > 0) {
    return { label: '下载现场标签', to: '/portal/payment' }
  }
  if (resultEntries.value.length > 0) {
    return { label: '查看结果反馈', to: '/portal/results' }
  }
  return { label: '提交酒款', to: '/portal/submit' }
})

const nextActions = computed(() => [
  { label: '待付款', value: unpaidEntries.value.length, hint: '完成付款后生成现场标签', icon: Money, to: '/portal/payment' },
  { label: '待打印标签', value: labelReadyEntries.value.length, hint: '下载 UUID 标签并贴瓶或外箱', icon: Tickets, to: '/portal/payment' },
  { label: '结果可查', value: resultEntries.value.length, hint: '含奖项、评分和评语', icon: CircleCheck, to: '/portal/results' },
])

const flowSteps = computed(() => [
  { key: 'submit', index: '01', label: '提交资料', hint: `${entries.length} 款已提交`, done: entries.length > 0 },
  { key: 'pay', index: '02', label: '付款确认', hint: `${paidEntries.value.length} 款已付款`, active: unpaidEntries.value.length > 0, done: unpaidEntries.value.length === 0 },
  { key: 'qr', index: '03', label: '打印二维码', hint: '贴瓶或外箱', active: paidEntries.value.length > 0, done: storedEntries.value.length > 0 },
  { key: 'stored', index: '04', label: '酒样入库', hint: `${storedEntries.value.length} 款已入库`, active: storedEntries.value.length > 0 },
  { key: 'result', index: '05', label: '结果反馈', hint: `${resultEntries.value.length} 款可查看`, active: resultEntries.value.length > 0 },
])

function entryCount(competitionId) {
  return entries.filter((entry) => entry.competitionId === competitionId).length
}
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 22px;
}

.entry-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 28px;
  min-height: 430px;
  padding: 38px;
  overflow: hidden;
  color: #fff8e8;
  background:
    linear-gradient(135deg, rgba(35, 25, 16, 0.9), rgba(88, 57, 23, 0.76)),
    url("https://images.unsplash.com/photo-1532634786-c8f8c86a0062?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
  box-shadow: 0 24px 60px rgba(67, 43, 17, 0.14);
}

.entry-hero::after {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 120px;
  background: linear-gradient(180deg, transparent, rgba(33, 25, 18, 0.58));
  pointer-events: none;
}

.hero-copy,
.event-ticket {
  position: relative;
  z-index: 1;
}

.hero-copy {
  align-self: end;
  max-width: 760px;
}

.hero-copy h1 {
  margin: 18px 0 12px;
  font-size: 54px;
  line-height: 1.03;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 700px;
  margin: 0;
  color: #eadabd;
  font-size: 17px;
  line-height: 1.75;
}

.hero-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.hero-facts span {
  min-height: 32px;
  padding: 6px 10px;
  color: #fff8e8;
  background: rgba(255, 250, 240, 0.11);
  border: 1px solid rgba(255, 250, 240, 0.2);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.primary-action {
  color: #2b1d10;
  background: #e1a23d;
}

.secondary-action {
  color: #fff8e8;
  background: rgba(255, 250, 240, 0.12);
  border: 1px solid rgba(255, 250, 240, 0.28);
}

.event-ticket {
  align-self: end;
  padding: 22px;
  color: #2b1d10;
  background:
    linear-gradient(180deg, rgba(255, 250, 240, 0.96), rgba(247, 221, 162, 0.94)),
    repeating-linear-gradient(45deg, rgba(56, 36, 17, 0.06) 0 1px, transparent 1px 8px);
  border: 1px solid rgba(255, 246, 223, 0.6);
  border-radius: 8px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.24);
}

.event-ticket > span {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.event-ticket dl,
.rules-card dl {
  display: grid;
  gap: 14px;
  margin: 18px 0 0;
}

.event-ticket dl div,
.rules-card dl div {
  padding-bottom: 14px;
  border-bottom: 1px dashed rgba(87, 58, 26, 0.18);
}

.event-ticket dl div:last-child,
.rules-card dl div:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

dt {
  color: #746a5f;
  font-size: 13px;
}

dd {
  margin: 5px 0 0;
  font-weight: 800;
  line-height: 1.55;
}

.events-panel,
.next-panel,
.flow-card,
.rules-card {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.section-head p {
  margin: 0;
  color: #746a5f;
  line-height: 1.65;
}

.section-head a,
.submit-link {
  color: #9a6218;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.event-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.open-event-card {
  display: grid;
  gap: 18px;
  min-height: 246px;
  padding: 20px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.open-event-card h3 {
  margin: 16px 0 8px;
  font-size: 24px;
  line-height: 1.2;
}

.open-event-card p {
  margin: 0;
  color: #746a5f;
}

.event-card-meta {
  display: grid;
  grid-template-columns: 1.3fr 0.8fr 0.7fr;
  gap: 8px;
}

.event-card-meta span {
  padding: 12px;
  background: rgba(255, 250, 240, 0.7);
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.event-card-meta small,
.event-card-meta b {
  display: block;
}

.event-card-meta small {
  color: #746a5f;
}

.event-card-meta b {
  margin-top: 5px;
  line-height: 1.35;
}

.event-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-self: end;
}

.event-card-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 14px;
  color: #6b4710;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.event-card-actions .event-primary {
  color: #2b1d10;
  background: #e1a23d;
  border-color: rgba(87, 58, 26, 0.08);
}

.next-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.next-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  min-height: 112px;
  padding: 16px;
  color: #2b1d10;
  text-decoration: none;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.next-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 34px rgba(83, 51, 17, 0.12);
}

.next-card svg {
  width: 24px;
  height: 24px;
  margin-top: 3px;
  color: #b87517;
}

.next-card span,
.next-card p {
  color: #746a5f;
}

.next-card span {
  display: block;
  font-size: 13px;
}

.next-card strong {
  display: block;
  margin-top: 6px;
  font-size: 34px;
  line-height: 1;
}

.next-card p {
  grid-column: 1 / -1;
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.flow-line {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.flow-step {
  min-height: 132px;
  padding: 14px;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.18);
  border-radius: 8px;
}

.flow-step span {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #8a785f;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
}

.flow-step strong,
.flow-step small {
  display: block;
}

.flow-step strong {
  margin-top: 20px;
}

.flow-step small {
  margin-top: 8px;
  color: #807567;
  line-height: 1.5;
}

.flow-step.done,
.flow-step.active {
  background: #2b1d10;
  border-style: solid;
  color: #fff6df;
}

.flow-step.done span,
.flow-step.active span {
  color: #2b1d10;
  background: #e2a640;
}

.flow-step.done small,
.flow-step.active small {
  color: #d3bf99;
}

.entry-strip {
  display: grid;
  gap: 14px;
}

.entry-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.beer-label-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 210px;
  padding: 20px;
  color: #2b1d10;
  background:
    linear-gradient(180deg, #fffaf0, #f7dfab),
    repeating-linear-gradient(90deg, rgba(47, 31, 15, 0.06) 0 1px, transparent 1px 10px);
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 8px;
  box-shadow: 0 16px 36px rgba(83, 51, 17, 0.09);
}

.beer-label-card h3 {
  min-height: 58px;
  margin: 18px 0 8px;
  font-size: 22px;
  line-height: 1.25;
}

.beer-label-card p,
.uuid-ticket span {
  color: #746a5f;
}

.uuid-ticket {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed rgba(87, 58, 26, 0.24);
}

.uuid-ticket span,
.uuid-ticket strong {
  display: block;
}

.uuid-ticket strong {
  margin-top: 5px;
  font-size: 15px;
  letter-spacing: 0.04em;
}

@media (max-width: 1180px) {
  .entry-hero,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .event-grid,
  .next-grid,
  .entry-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .event-card-meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .entry-hero {
    min-height: auto;
    padding: 24px;
  }

  .hero-copy h1 {
    font-size: 38px;
  }

  .section-head {
    display: grid;
  }

  .event-grid,
  .next-grid,
  .entry-cards,
  .flow-line {
    grid-template-columns: 1fr;
  }
}
</style>
