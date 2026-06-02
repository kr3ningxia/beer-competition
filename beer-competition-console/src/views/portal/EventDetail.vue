<template>
  <div class="event-detail-page">
    <section class="detail-hero">
      <div>
        <span :class="['label-chip', competition.status === 'PUBLISHED' ? 'tone-gold' : 'tone-green']">
          {{ competition.stage }}
        </span>
        <h1>{{ competition.name }}</h1>
        <p>{{ competition.description }}</p>
        <div class="hero-actions">
          <RouterLink
            v-if="canSubmitEntry(competition)"
            class="primary-action"
            :to="loggedIn ? `/portal/submit?competitionId=${competition.id}` : '/portal/login'"
          >
            {{ loggedIn ? '提交酒款' : '登录后报名' }}
          </RouterLink>
          <RouterLink class="secondary-action" to="/portal/events">返回全部赛事</RouterLink>
        </div>
      </div>
      <aside class="event-ticket">
        <span>{{ competition.shortName }}</span>
        <dl>
          <div><dt>比赛日期</dt><dd>{{ competition.date }}</dd></div>
          <div><dt>报名截止</dt><dd>{{ competition.registrationDeadline }}</dd></div>
          <div><dt>报名费</dt><dd>¥{{ competition.entryFee }} / 款</dd></div>
          <div><dt>比赛地点</dt><dd>{{ competition.city }} · {{ competition.venue }}</dd></div>
        </dl>
      </aside>
    </section>

    <section class="detail-grid">
      <article class="brewer-card info-card">
        <h2 class="portal-section-title">赛事简介</h2>
        <dl>
          <div><dt>主办方</dt><dd>{{ competition.organizer }}</dd></div>
          <div><dt>适合提交</dt><dd>{{ competition.audience }}</dd></div>
          <div><dt>赛事说明</dt><dd>{{ competition.description }}</dd></div>
        </dl>
      </article>

      <article class="brewer-card info-card">
        <h2 class="portal-section-title">报名要求</h2>
        <dl>
          <div><dt>投递组别</dt><dd>{{ competition.categories.join(' / ') }}</dd></div>
          <div><dt>基础风格</dt><dd>{{ competition.styleOptions.join(' / ') }}</dd></div>
          <div><dt>额外字段</dt><dd>{{ competition.entryFields.join(' / ') }}</dd></div>
          <div><dt>酒款简介</dt><dd>建议 300 字以内，请避免填写厂牌、联系人或其他可识别身份的信息。</dd></div>
        </dl>
      </article>
    </section>

    <section class="detail-grid">
      <article class="brewer-card info-card">
        <h2 class="portal-section-title">送样要求</h2>
        <dl>
          <div><dt>酒样数量</dt><dd>{{ competition.sampleRequirement }}</dd></div>
          <div><dt>送样截止</dt><dd>{{ competition.sampleDeadline }}</dd></div>
          <div><dt>送样地址</dt><dd>{{ competition.sampleAddress }}</dd></div>
          <div><dt>标签粘贴</dt><dd>付款确认后下载现场标签，贴在酒瓶或外箱便于收样核对。</dd></div>
        </dl>
      </article>

      <article class="brewer-card info-card">
        <h2 class="portal-section-title">奖项与反馈</h2>
        <dl>
          <div><dt>奖项说明</dt><dd>{{ competition.awardNote }}</dd></div>
          <div><dt>厂商可见反馈</dt><dd>结果发布后可查看评分明细、桌长综合评语和奖状相关信息。</dd></div>
          <div><dt>历史赛事</dt><dd>{{ competition.status === 'PUBLISHED' ? '该赛事已发布结果。' : '比赛结束并确认结果后开放查看。' }}</dd></div>
        </dl>
      </article>
    </section>

    <section class="brewer-card my-event-card">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">我的本场酒款</h2>
          <p>{{ loggedIn ? '查看本厂牌在该赛事下的提交和处理状态。' : '登录后可查看本厂牌在该赛事下的酒款。' }}</p>
        </div>
        <RouterLink
          v-if="loggedIn && canSubmitEntry(competition)"
          :to="`/portal/submit?competitionId=${competition.id}`"
        >
          继续提交
        </RouterLink>
      </div>

      <div v-if="!loggedIn" class="empty-state">
        <strong>登录后查看参赛记录</strong>
        <p>你可以先阅读赛事规则，确认参赛后再登录提交酒款。</p>
        <RouterLink class="primary-action" to="/portal/login">登录报名</RouterLink>
      </div>

      <div v-else-if="eventEntries.length" class="entry-list">
        <article v-for="entry in eventEntries" :key="entry.id" class="entry-row">
          <div>
            <span :class="['label-chip', `tone-${entryStatusMeta[entry.status].tone}`]">
              {{ entryStatusMeta[entry.status].label }}
            </span>
            <h3>{{ entry.name }}</h3>
            <p>{{ entry.categoryName }} · {{ entry.style }} · {{ entry.abv }}</p>
          </div>
          <dl>
            <div><dt>参赛编号</dt><dd>{{ entry.uuid }}</dd></div>
            <div><dt>现场短编号</dt><dd>{{ entry.shortCode }}</dd></div>
            <div><dt>下一步</dt><dd>{{ nextActionText(entry) }}</dd></div>
          </dl>
          <RouterLink :to="entryPrimaryAction(entry).to">{{ entryPrimaryAction(entry).label }}</RouterLink>
        </article>
      </div>

      <div v-else class="empty-state">
        <strong>本场赛事尚未提交酒款</strong>
        <p>确认组别、风格和送样要求后，可以提交适合本赛事的酒款。</p>
        <RouterLink v-if="canSubmitEntry(competition)" class="primary-action" :to="`/portal/submit?competitionId=${competition.id}`">
          提交酒款
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { competitions, entries } from './mockData'
import { canSubmitEntry, entryPrimaryAction, entryStatusMeta, nextActionText } from './portalViewModels'

const route = useRoute()
const loggedIn = computed(() => isLoggedIn('portal'))
const competition = computed(() => competitions.find((item) => item.id === route.params.id) || competitions[0])
const eventEntries = computed(() => entries.filter((entry) => entry.competitionId === competition.value.id))
</script>

<style scoped>
.event-detail-page {
  display: grid;
  gap: 22px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 28px;
  min-height: 420px;
  padding: 34px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(31, 21, 14, 0.94), rgba(86, 48, 18, 0.82)),
    url("https://images.unsplash.com/photo-1518099074172-2e47ee6cfdc0?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
}

.detail-hero h1 {
  max-width: 920px;
  margin: 18px 0 12px;
  font-size: 56px;
  line-height: 1;
}

.detail-hero p {
  max-width: 760px;
  color: #ead9b7;
  font-size: 17px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.primary-action,
.secondary-action,
.entry-row > a,
.section-head a {
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

.secondary-action,
.section-head a {
  color: #fff6df;
  background: rgba(255, 250, 240, 0.13);
  border: 1px solid rgba(255, 250, 240, 0.24);
}

.event-ticket {
  align-self: end;
  padding: 24px;
  color: #2b1d10;
  background: linear-gradient(180deg, rgba(255, 250, 240, 0.98), rgba(246, 217, 150, 0.95));
  border-radius: 8px;
}

.event-ticket > span {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.info-card,
.my-event-card {
  padding: 24px;
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

dt,
dd {
  margin: 0;
}

dt,
.section-head p,
.entry-row p,
.empty-state p {
  color: #746a5f;
}

dd {
  margin-top: 6px;
  font-weight: 800;
  line-height: 1.55;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.section-head a {
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.entry-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 460px auto;
  gap: 18px;
  align-items: center;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.entry-row h3 {
  margin: 12px 0 6px;
  font-size: 22px;
}

.entry-row dl {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.entry-row dl div {
  padding: 10px;
  background: #fffdf7;
  border: 0;
  border-radius: 8px;
}

.empty-state {
  display: grid;
  justify-items: start;
  gap: 12px;
  margin-top: 18px;
  padding: 24px;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.18);
  border-radius: 8px;
}

.empty-state strong {
  font-size: 22px;
}

@media (max-width: 1080px) {
  .detail-hero,
  .detail-grid,
  .entry-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .detail-hero {
    min-height: auto;
    padding: 26px;
  }

  .detail-hero h1 {
    font-size: 38px;
  }

  .section-head {
    display: grid;
  }

  .entry-row dl {
    grid-template-columns: 1fr;
  }
}
</style>
