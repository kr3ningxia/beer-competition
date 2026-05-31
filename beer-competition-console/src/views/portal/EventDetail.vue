<template>
  <div class="event-detail-page">
    <section class="detail-hero">
      <div>
        <span :class="['label-chip', competition.status === 'PUBLISHED' ? 'tone-gold' : 'tone-green']">
          {{ competition.stage }}
        </span>
        <h2>{{ competition.name }}</h2>
        <p>{{ competition.city }} · {{ competition.venue }} · 比赛日期 {{ competition.date }}</p>
      </div>
      <div class="fee-ticket">
        <span>{{ competition.shortName }}</span>
        <strong>¥{{ competition.entryFee }}</strong>
        <small>每款报名费</small>
      </div>
    </section>

    <section class="detail-grid">
      <article class="brewer-card progress-card">
        <div class="section-head">
          <h3 class="portal-section-title">赛事进度</h3>
          <RouterLink to="/portal/events">返回全部赛事</RouterLink>
        </div>
        <div class="progress-lane">
          <div
            v-for="step in competition.timeline"
            :key="step.label"
            :class="['lane-step', { done: step.done, active: step.active }]"
          >
            <span />
            <div>
              <strong>{{ step.label }}</strong>
              <small>{{ step.date }}</small>
            </div>
          </div>
        </div>
      </article>

      <article class="brewer-card rule-card">
        <h3 class="portal-section-title">报名配置</h3>
        <dl>
          <div>
            <dt>报名截止</dt>
            <dd>{{ competition.registrationDeadline }}</dd>
          </div>
          <div>
            <dt>投递组别</dt>
            <dd>{{ competition.categories.join(' / ') }}</dd>
          </div>
          <div>
            <dt>基础风格</dt>
            <dd>{{ competition.styleOptions.join(' / ') }}</dd>
          </div>
          <div>
            <dt>额外字段</dt>
            <dd>{{ competition.entryFields.join(' / ') }}</dd>
          </div>
        </dl>
      </article>
    </section>

    <section class="entry-panel brewer-card">
      <div class="section-head">
        <div>
          <h3 class="portal-section-title">本场参赛信息</h3>
          <p>{{ eventEntries.length > 0 ? '以下为本厂牌在该赛事下提交的酒款。' : '本场赛事尚未提交酒款。' }}</p>
        </div>
        <div class="entry-actions">
          <RouterLink class="secondary-action" to="/portal/submit">提交酒款</RouterLink>
        </div>
      </div>

      <div v-if="eventEntries.length" class="entry-table">
        <article v-for="entry in eventEntries" :key="entry.id" class="entry-row">
          <div>
            <span :class="['label-chip', `tone-${statusMeta[entry.status].tone}`]">
              {{ statusMeta[entry.status].label }}
            </span>
            <h4>{{ entry.name }}</h4>
            <p>{{ entry.uuid }} · {{ entry.categoryName }} · {{ entry.style }} · {{ entry.abv }}</p>
          </div>
          <div class="entry-flow">
            <span class="done">提交</span>
            <span :class="{ done: entry.paymentStatus === 'PAID' }">付款</span>
            <span :class="{ done: entry.paymentStatus === 'PAID' }">标签</span>
            <span :class="{ done: Boolean(entry.storedAt) }">入库</span>
            <span :class="{ done: entry.status === 'RESULT_PUBLISHED' }">结果</span>
          </div>
          <div class="row-actions">
            <RouterLink v-if="entry.paymentStatus !== 'PAID'" to="/portal/payment">去付款</RouterLink>
            <RouterLink v-else-if="entry.status === 'RESULT_PUBLISHED'" to="/portal/results">查看反馈</RouterLink>
            <RouterLink v-else to="/portal/payment">下载标签</RouterLink>
            <RouterLink to="/portal/entries">查看资料</RouterLink>
          </div>
        </article>
      </div>

      <div v-else class="empty-state">
        <strong>还没有参赛酒款</strong>
        <p>选择该赛事提交酒款后，这里会显示付款、二维码、入库和结果进度。</p>
        <RouterLink class="primary-action" to="/portal/submit">立即提交</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { competitions, entries, statusMeta } from './mockData'

const route = useRoute()
const competition = computed(() => competitions.find((item) => item.id === route.params.id) || competitions[0])
const eventEntries = computed(() => entries.filter((entry) => entry.competitionId === competition.value.id))
</script>

<style scoped>
.event-detail-page {
  display: grid;
  gap: 22px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  min-height: 260px;
  padding: 28px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(33, 25, 18, 0.94), rgba(83, 50, 19, 0.88)),
    repeating-linear-gradient(135deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 14px);
  border-radius: 8px;
}

.detail-hero h2 {
  max-width: 940px;
  margin: 20px 0 10px;
  font-size: 48px;
  line-height: 1;
}

.detail-hero p {
  margin: 0;
  color: #d8c8a8;
}

.fee-ticket {
  flex: 0 0 220px;
  padding: 22px;
  color: #2b1d10;
  background: linear-gradient(180deg, #fffaf0, #eac06c);
  border-radius: 8px;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.2);
}

.fee-ticket span,
.fee-ticket strong,
.fee-ticket small {
  display: block;
}

.fee-ticket span {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.fee-ticket strong {
  margin-top: 24px;
  font-size: 44px;
  line-height: 1;
}

.fee-ticket small {
  margin-top: 8px;
  color: #6e5d45;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 18px;
}

.progress-card,
.rule-card,
.entry-panel {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.section-head a {
  color: #9a6218;
  font-weight: 800;
  text-decoration: none;
}

.progress-lane {
  display: grid;
  gap: 12px;
}

.lane-step {
  display: flex;
  gap: 14px;
  padding: 14px;
  background: #f1e4ca;
  border-radius: 8px;
}

.lane-step span {
  width: 12px;
  height: 12px;
  margin-top: 5px;
  background: #c8b99f;
  border-radius: 50%;
}

.lane-step.done,
.lane-step.active {
  color: #fff6df;
  background: #2b1d10;
}

.lane-step.done span,
.lane-step.active span {
  background: #e1a23d;
}

.lane-step small {
  display: block;
  margin-top: 4px;
  color: #746a5f;
}

.lane-step.done small,
.lane-step.active small {
  color: #d8c8a8;
}

dl {
  display: grid;
  gap: 12px;
  margin: 0;
}

dl div {
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(87, 58, 26, 0.16);
}

dt {
  color: #746a5f;
  font-size: 13px;
}

dd {
  margin: 6px 0 0;
  font-weight: 800;
  line-height: 1.55;
}

.entry-panel p {
  margin: 0;
  color: #746a5f;
}

.entry-actions {
  display: flex;
  gap: 10px;
}

.primary-action,
.secondary-action {
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
}

.secondary-action {
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.secondary-action.dark {
  color: #fff6df;
  background: #2b1d10;
}

.entry-table {
  display: grid;
  gap: 12px;
  margin-top: 20px;
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

.row-actions {
  display: grid;
  gap: 8px;
  min-width: 100px;
}

.row-actions a {
  min-height: 36px;
  padding: 0 12px;
  color: #2b1d10;
  background: #e1a23d;
  border: 0;
  border-radius: 8px;
  font: inherit;
  font-weight: 800;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
}

.entry-row h4 {
  margin: 14px 0 6px;
  font-size: 22px;
}

.entry-row p {
  line-height: 1.6;
}

.entry-flow {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.entry-flow span {
  min-height: 44px;
  display: grid;
  place-items: center;
  color: #7d705f;
  background: #f0e2c8;
  border-radius: 8px;
  font-weight: 700;
}

.entry-flow span.done {
  color: #fff6df;
  background: #3d7d50;
}

.empty-state {
  display: grid;
  justify-items: start;
  gap: 12px;
  margin-top: 20px;
  padding: 28px;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.18);
  border-radius: 8px;
}

.empty-state strong {
  font-size: 22px;
}

@media (max-width: 1120px) {
  .detail-grid,
  .entry-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .detail-hero,
  .section-head {
    display: grid;
  }

  .entry-actions {
    flex-wrap: wrap;
  }

  .entry-flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
