<template>
  <div class="events-page">
    <section class="events-hero">
      <div>
        <span class="label-chip tone-gold">赛事甄选</span>
        <h1>为你的酒款匹配更合适的赛场</h1>
        <p>查看报名窗口、投递组别、风格要求与结果状态，进入单场赛事完成报名</p>
      </div>
      <RouterLink class="primary-action" to="/portal/home">返回赛事首页</RouterLink>
    </section>

    <section class="event-list">
      <article v-for="competition in competitions" :key="competition.id" class="event-card brewer-card">
        <div class="event-main">
          <span :class="['label-chip', stageTone(competition.status)]">{{ stageLabel(competition) }}</span>
          <h2>{{ competition.name }}</h2>
          <p>{{ competition.description || competition.code }}</p>
          <div class="fact-row">
            <span>报名截止 {{ formatMonthDayTime(competition.registrationDeadline) }}</span>
            <span>{{ entryFeeText(competition) }}</span>
          </div>
        </div>

        <aside class="event-side">
          <strong>{{ competition.categories.length }} 个投递组别</strong>
          <p>{{ competition.categories.slice(0, 4).map((item) => item.name).join(' / ') || '暂未配置' }}</p>
          <div class="event-actions">
            <RouterLink :to="`/portal/events/${competition.id}`">查看赛事详情</RouterLink>
            <RouterLink
              v-if="showPrimaryAction(competition)"
              class="event-primary"
              :to="primaryAction(competition).to"
            >
              {{ primaryAction(competition).label }}
            </RouterLink>
          </div>
        </aside>
      </article>
      <div v-if="!competitions.length" class="empty-state brewer-card">
        <strong>暂无可展示赛事</strong>
        <p>赛事开放后会在这里显示</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { fetchPortalCompetitions } from '@/api/portal'
import {
  canSubmitEntry,
  competitionAction,
  formatCompetitionFee,
  isCompetitionResultPublished,
} from './portalViewModels'

const loggedIn = computed(() => isLoggedIn('portal'))
const competitions = ref([])

onMounted(async () => {
  competitions.value = await fetchPortalCompetitions()
})

function stageTone(status) {
  if (status === 'PUBLISHED') return 'tone-gold'
  if (status === 'REGISTRATION_OPEN') return 'tone-green'
  return 'tone-amber'
}

function stageLabel(competition) {
  return isCompetitionResultPublished(competition) ? '结果已发布' : competition.currentStageLabel
}

function primaryAction(competition) {
  return competitionAction(competition, loggedIn.value)
}

function showPrimaryAction(competition) {
  return isCompetitionResultPublished(competition) || canSubmitEntry(competition)
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function formatMonthDayTime(value) {
  if (!value) return '-'
  const normalized = String(value).replace('T', ' ')
  const match = normalized.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/)
  if (!match) return formatDateTime(value)
  const [, month, day, hour, minute] = match
  return `${Number(month)}月${Number(day)}日 ${hour}:${minute}`
}

function entryFeeText(competition) {
  return formatCompetitionFee(competition)
}
</script>

<style scoped>
.events-page {
  display: grid;
  gap: 22px;
}

.events-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  min-height: 300px;
  padding: 32px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(32, 22, 15, 0.94), rgba(91, 50, 18, 0.86)),
    url("https://images.unsplash.com/photo-1518099074172-2e47ee6cfdc0?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
}

.events-hero h1 {
  max-width: 780px;
  margin: 18px 0 10px;
  font-size: 50px;
  line-height: 1.05;
}

.events-hero p {
  max-width: 720px;
  margin: 0;
  color: #ead9b7;
  line-height: 1.7;
}

.primary-action,
.event-actions a {
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

.event-list {
  display: grid;
  gap: 18px;
}

.event-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 22px;
  padding: 24px;
}

.event-main h2 {
  max-width: 850px;
  margin: 16px 0 10px;
  font-size: 34px;
  line-height: 1.12;
}

.event-main p,
.event-side p {
  color: #746a5f;
  line-height: 1.7;
}

.event-main p {
  display: -webkit-box;
  max-width: 780px;
  margin: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.event-side p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fact-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.fact-row span {
  padding: 8px 10px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
}

.event-side {
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.event-side strong {
  display: block;
  font-size: 20px;
}

.event-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.event-actions a {
  color: #6b4710;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.event-actions .event-primary {
  color: #2b1d10;
  background: #e1a23d;
  border-color: transparent;
}

@media (max-width: 980px) {
  .events-hero,
  .event-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .events-hero {
    display: grid;
    min-height: auto;
    padding: 26px;
  }

  .events-hero h1 {
    font-size: 36px;
  }
}
</style>
