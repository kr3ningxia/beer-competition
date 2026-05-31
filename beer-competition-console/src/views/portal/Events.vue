<template>
  <div class="events-page">
    <section class="events-hero">
      <div>
        <span class="label-chip tone-gold">EVENTS</span>
        <h2>全部赛事</h2>
        <p>查看开放报名与历史结果，进入单场赛事确认报名规则、付款标签和结果反馈。</p>
      </div>
      <RouterLink class="primary-action" to="/portal/submit">提交酒款</RouterLink>
    </section>

    <section class="event-grid">
      <article v-for="competition in competitions" :key="competition.id" class="event-card-large brewer-card">
        <div class="event-card-head">
          <div>
            <span :class="['label-chip', stageTone(competition.status)]">{{ competition.stage }}</span>
            <h3>{{ competition.name }}</h3>
            <p>{{ competition.city }} · {{ competition.venue }}</p>
          </div>
          <div class="date-block">
            <small>比赛日期</small>
            <strong>{{ formatDate(competition.date) }}</strong>
          </div>
        </div>

        <div class="event-meta">
          <span><small>报名截止</small><b>{{ competition.registrationDeadline }}</b></span>
          <span><small>报名费</small><b>¥{{ competition.entryFee }} / 款</b></span>
          <span><small>投递组别</small><b>{{ competition.categories.length }} 个</b></span>
        </div>

        <div class="progress-row" aria-label="赛事进度">
          <div
            v-for="step in competition.timeline"
            :key="step.label"
            :class="['progress-step', { done: step.done, active: step.active }]"
          >
            <span />
            <strong>{{ step.label }}</strong>
            <small>{{ step.date }}</small>
          </div>
        </div>

        <div class="entry-summary">
          <span><small>已提交</small><b>{{ competition.summary.submitted }}</b></span>
          <span><small>待付款</small><b>{{ competition.summary.pendingPayment }}</b></span>
          <span><small>已入库</small><b>{{ competition.summary.stored }}</b></span>
          <span><small>结果可查</small><b>{{ competition.summary.result }}</b></span>
        </div>

        <div class="event-card-foot">
          <p>{{ competition.description }}</p>
          <RouterLink class="event-link" :to="`/portal/events/${competition.id}`">
            {{ competition.actionLabel }}
          </RouterLink>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'
import { competitions } from './mockData'

function stageTone(status) {
  if (status === 'PUBLISHED') {
    return 'tone-gold'
  }
  if (status === 'REGISTRATION_OPEN') {
    return 'tone-green'
  }
  return 'tone-amber'
}

function formatDate(date) {
  return date.replaceAll('-', '.')
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
  min-height: 220px;
  padding: 28px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(33, 25, 18, 0.94), rgba(91, 58, 22, 0.88)),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 18px);
  border-radius: 8px;
}

.events-hero h2 {
  margin: 18px 0 10px;
  font-size: 46px;
  line-height: 1;
}

.events-hero p {
  max-width: 720px;
  margin: 0;
  color: #d8c8a8;
  line-height: 1.7;
}

.primary-action,
.event-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.event-grid {
  display: grid;
  gap: 18px;
}

.event-card-large {
  padding: 24px;
}

.event-card-head,
.event-card-foot {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.event-card-head h3 {
  max-width: 780px;
  margin: 16px 0 8px;
  font-size: 30px;
  line-height: 1.15;
}

.event-card-head p,
.event-card-foot p,
.event-meta small,
.entry-summary small,
.date-block small,
.progress-step small {
  color: #746a5f;
}

.date-block {
  flex: 0 0 150px;
  padding: 16px;
  text-align: right;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.date-block small,
.date-block strong {
  display: block;
}

.date-block strong {
  margin-top: 8px;
  font-size: 22px;
}

.event-meta,
.entry-summary {
  display: grid;
  gap: 10px;
  margin-top: 22px;
}

.event-meta {
  grid-template-columns: 1.25fr 0.8fr 0.65fr;
}

.entry-summary {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.event-meta span,
.entry-summary span {
  padding: 14px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.event-meta small,
.event-meta b,
.entry-summary small,
.entry-summary b {
  display: block;
}

.event-meta b,
.entry-summary b {
  margin-top: 6px;
}

.entry-summary b {
  font-size: 28px;
  line-height: 1;
}

.progress-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  margin-top: 22px;
}

.progress-step {
  position: relative;
  min-height: 86px;
  padding: 12px;
  background: #f1e4ca;
  border-radius: 8px;
}

.progress-step span {
  display: block;
  width: 12px;
  height: 12px;
  margin-bottom: 13px;
  background: #c8b99f;
  border-radius: 50%;
}

.progress-step.done,
.progress-step.active {
  color: #fff6df;
  background: #2b1d10;
}

.progress-step.done span,
.progress-step.active span {
  background: #e1a23d;
}

.progress-step.done small,
.progress-step.active small {
  color: #d8c8a8;
}

.progress-step strong,
.progress-step small {
  display: block;
}

.progress-step small {
  margin-top: 6px;
}

.event-card-foot {
  align-items: center;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed rgba(87, 58, 26, 0.18);
}

.event-card-foot p {
  max-width: 850px;
  margin: 0;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .events-hero,
  .event-card-head,
  .event-card-foot {
    display: grid;
  }

  .date-block {
    text-align: left;
  }

  .event-meta,
  .entry-summary,
  .progress-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .event-meta,
  .entry-summary,
  .progress-row {
    grid-template-columns: 1fr;
  }
}
</style>
