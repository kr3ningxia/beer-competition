<template>
  <div class="my-page">
    <section class="my-hero">
      <div>
        <span class="label-chip tone-green">我的参赛</span>
        <h1>{{ breweryProfile.breweryName || '完善厂牌资料' }}</h1>
        <p>{{ heroCopy }}</p>
      </div>
      <RouterLink class="hero-action" :to="heroAction.to">{{ heroAction.label }}</RouterLink>
    </section>

    <section class="todo-grid">
      <RouterLink v-for="item in todoCards" :key="item.label" :to="item.to" class="todo-card">
        <component :is="item.icon" />
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.hint }}</p>
      </RouterLink>
    </section>

    <section class="brewer-card progress-panel">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">我的赛事进度</h2>
          <p>按赛事查看已提交、付款确认、标签、入库和结果状态。</p>
        </div>
        <RouterLink to="/portal/events">浏览开放赛事</RouterLink>
      </div>

      <div class="competition-progress">
        <article v-for="competition in myCompetitions" :key="competition.id" class="progress-row">
          <div>
            <span :class="['label-chip', competition.status === 'PUBLISHED' ? 'tone-gold' : 'tone-green']">
              {{ competition.currentStageLabel }}
            </span>
            <h3>{{ competition.name }}</h3>
            <p>{{ competition.code }} · {{ competition.edition }}</p>
          </div>
          <div class="summary-strip">
            <span><small>已提交</small><b>{{ summary(competition.id).submitted }}</b></span>
            <span><small>待确认付款</small><b>{{ summary(competition.id).pendingPayment }}</b></span>
            <span><small>报名成功</small><b>{{ summary(competition.id).registered }}</b></span>
            <span><small>已入库</small><b>{{ summary(competition.id).stored }}</b></span>
            <span><small>结果可查</small><b>{{ summary(competition.id).result }}</b></span>
          </div>
          <div class="row-actions">
            <RouterLink :to="`/portal/events/${competition.id}`">赛事详情</RouterLink>
            <RouterLink to="/portal/entries">查看酒款</RouterLink>
          </div>
        </article>
      </div>
    </section>

    <section class="brewer-card recent-panel">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">最近酒款</h2>
          <p>只展示参赛处理中需要核对的信息。</p>
        </div>
        <RouterLink to="/portal/entries">全部酒款</RouterLink>
      </div>

      <div class="entry-table">
        <article v-for="entry in entries.slice(0, 4)" :key="entry.id" class="entry-row">
          <div>
            <span :class="['label-chip', `tone-${entryStatusMeta[entry.status].tone}`]">
              {{ entryStatusMeta[entry.status].label }}
            </span>
            <h3>{{ entry.name }}</h3>
            <p>{{ competitionName(entry.competitionId) }}</p>
          </div>
          <dl>
            <div><dt>参赛编号</dt><dd>{{ entry.uuid }}</dd></div>
            <div><dt>现场短编号</dt><dd>{{ entry.shortCode }}</dd></div>
            <div><dt>下一步</dt><dd>{{ nextActionText(entry) }}</dd></div>
          </dl>
          <RouterLink :to="entryPrimaryAction(entry).to">{{ entryPrimaryAction(entry).label }}</RouterLink>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { CircleCheck, Money, Tickets } from '@element-plus/icons-vue'
import { fetchMyParticipation } from '@/api/portal'
import {
  entryPrimaryAction,
  entryStatusMeta,
  entrySummaryForCompetition,
  nextActionText,
  priorityEntry,
} from './portalViewModels'

const profile = ref({})
const entries = ref([])
const competitions = ref([])
const breweryProfile = computed(() => ({
  breweryName: profile.value.companyName || profile.value.displayName || '完善厂牌资料',
}))
const highestPriorityEntry = computed(() => priorityEntry(entries.value))
const heroAction = computed(() => entryPrimaryAction(highestPriorityEntry.value))
const heroCopy = computed(() => {
  if (!highestPriorityEntry.value) {
    return '目前没有待处理的参赛事项，可以浏览开放报名赛事。'
  }
  return `${highestPriorityEntry.value.name} 当前需要你${nextActionText(highestPriorityEntry.value)}。`
})

const unpaidEntries = computed(() => entries.value.filter((entry) => entry.status === 'PENDING_PAYMENT'))
const labelEntries = computed(() => entries.value.filter((entry) => entry.status === 'REGISTERED'))
const storedEntries = computed(() => entries.value.filter((entry) => entry.status === 'STORED' || entry.status === 'RESULT_PUBLISHED'))
const resultEntries = computed(() => entries.value.filter((entry) => entry.status === 'RESULT_PUBLISHED'))
const myCompetitions = computed(() => competitions.value)

const todoCards = computed(() => [
  { label: '待确认付款', value: unpaidEntries.value.length, hint: '线下付款后等待主办方确认', icon: Money, to: '/portal/payment' },
  { label: '可下载标签', value: labelEntries.value.length, hint: '贴在酒瓶或外箱，便于现场核对', icon: Tickets, to: '/portal/payment' },
  { label: '已确认入库', value: storedEntries.value.length, hint: '主办方已收到酒样', icon: CircleCheck, to: '/portal/entries' },
  { label: '结果可查看', value: resultEntries.value.length, hint: '查看奖项、评分和评语', icon: CircleCheck, to: '/portal/results' },
])

function summary(competitionId) {
  return entrySummaryForCompetition(competitionId, entries.value)
}

function competitionName(competitionId) {
  return competitions.value.find((competition) => competition.id === competitionId)?.name || '未关联赛事'
}

onMounted(async () => {
  const data = await fetchMyParticipation()
  profile.value = data.profile || {}
  entries.value = data.entries || []
  competitions.value = data.competitions || []
})
</script>

<style scoped>
.my-page {
  display: grid;
  gap: 22px;
}

.my-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 22px;
  min-height: 300px;
  padding: 32px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(31, 21, 14, 0.95), rgba(89, 49, 18, 0.88)),
    url("https://images.unsplash.com/photo-1505075106905-fb052892c116?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
}

.my-hero h1 {
  margin: 18px 0 10px;
  font-size: 48px;
  line-height: 1.05;
}

.my-hero p {
  max-width: 680px;
  margin: 0;
  color: #ead9b7;
  font-size: 17px;
  line-height: 1.7;
}

.hero-action,
.row-actions a,
.entry-row > a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 15px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.todo-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  min-height: 130px;
  padding: 18px;
  color: #2b1d10;
  text-decoration: none;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.todo-card svg {
  width: 24px;
  height: 24px;
  color: #a76b18;
}

.todo-card span,
.todo-card p {
  color: #746a5f;
}

.todo-card strong {
  grid-column: 1 / -1;
  font-size: 38px;
  line-height: 1;
}

.todo-card p {
  grid-column: 1 / -1;
  margin: 0;
  line-height: 1.55;
}

.progress-panel,
.recent-panel {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.section-head p {
  margin: 0;
  color: #746a5f;
}

.section-head a {
  color: #8b5c19;
  font-weight: 800;
  text-decoration: none;
}

.competition-progress,
.entry-table {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.progress-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 560px auto;
  gap: 18px;
  align-items: center;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.progress-row h3,
.entry-row h3 {
  margin: 12px 0 6px;
  font-size: 22px;
}

.progress-row p,
.entry-row p,
dt {
  color: #746a5f;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.summary-strip span {
  min-height: 70px;
  padding: 10px;
  background: #fffdf7;
  border-radius: 8px;
}

.summary-strip small,
.summary-strip b {
  display: block;
}

.summary-strip b {
  margin-top: 6px;
  font-size: 24px;
}

.row-actions {
  display: grid;
  gap: 8px;
}

.row-actions a + a {
  color: #6b4710;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 520px auto;
  gap: 18px;
  align-items: center;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

dl div {
  padding: 10px;
  background: #fffdf7;
  border-radius: 8px;
}

dt,
dd {
  margin: 0;
}

dd {
  margin-top: 6px;
  font-weight: 800;
  line-height: 1.4;
}

@media (max-width: 1180px) {
  .todo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .progress-row,
  .entry-row {
    grid-template-columns: 1fr;
  }

  .summary-strip,
  dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .my-hero,
  .section-head {
    display: grid;
  }

  .my-hero h1 {
    font-size: 36px;
  }

  .todo-grid,
  .summary-strip,
  dl {
    grid-template-columns: 1fr;
  }
}
</style>
