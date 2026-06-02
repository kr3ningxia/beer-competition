<template>
  <div class="events-page">
    <section class="events-hero">
      <div>
        <span class="label-chip tone-gold">赛事目录</span>
        <h1>选择适合你酒款的赛事</h1>
        <p>查看报名窗口、投递组别、送样要求和结果开放状态，再进入单场赛事提交酒款。</p>
      </div>
      <RouterLink class="primary-action" to="/portal/home">返回赛事首页</RouterLink>
    </section>

    <section class="event-list">
      <article v-for="competition in competitions" :key="competition.id" class="event-card brewer-card">
        <div class="event-main">
          <span :class="['label-chip', stageTone(competition.status)]">{{ competition.stage }}</span>
          <h2>{{ competition.name }}</h2>
          <p>{{ competition.description }}</p>
          <div class="fact-row">
            <span>比赛日期 {{ competition.date }}</span>
            <span>报名截止 {{ competition.registrationDeadline }}</span>
            <span>报名费 ¥{{ competition.entryFee }} / 款</span>
            <span>{{ competition.city }} · {{ competition.venue }}</span>
          </div>
        </div>

        <aside class="event-side">
          <strong>{{ competition.categories.length }} 个投递组别</strong>
          <p>{{ competition.categories.slice(0, 4).join(' / ') }}</p>
          <div class="event-actions">
            <RouterLink :to="`/portal/events/${competition.id}`">查看赛事详情</RouterLink>
            <RouterLink
              v-if="canSubmitEntry(competition)"
              class="event-primary"
              :to="loggedIn ? `/portal/submit?competitionId=${competition.id}` : '/portal/login'"
            >
              {{ loggedIn ? '提交酒款' : '登录报名' }}
            </RouterLink>
          </div>
        </aside>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { competitions } from './mockData'
import { canSubmitEntry } from './portalViewModels'

const loggedIn = computed(() => isLoggedIn('portal'))

function stageTone(status) {
  if (status === 'PUBLISHED') return 'tone-gold'
  if (status === 'REGISTRATION_OPEN') return 'tone-green'
  return 'tone-amber'
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
