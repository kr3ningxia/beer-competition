<template>
  <div class="home-page">
    <RouterLink v-if="loggedIn && pendingCount > 0" class="my-notice" to="/portal/my">
      你有 {{ pendingCount }} 项参赛事项待处理，进入我的参赛查看
    </RouterLink>

    <section class="site-hero">
      <div class="hero-copy">
        <span class="label-chip tone-green">{{ activeCompetition.stage }}</span>
        <h1>Greater Bay Craft Beer Awards</h1>
        <p>{{ activeCompetition.description }}</p>
        <div class="hero-facts">
          <span>比赛日期 {{ activeCompetition.date }}</span>
          <span>报名截止 {{ activeCompetition.registrationDeadline }}</span>
          <span>报名费 ¥{{ activeCompetition.entryFee }} / 款</span>
          <span>{{ activeCompetition.city }} · {{ activeCompetition.venue }}</span>
        </div>
        <div class="hero-actions">
          <RouterLink class="primary-action" to="/portal/events">查看开放赛事</RouterLink>
          <a class="secondary-action" href="#entry-flow">了解参赛流程</a>
          <RouterLink
            v-if="canSubmitEntry(activeCompetition)"
            class="text-action"
            :to="loggedIn ? `/portal/submit?competitionId=${activeCompetition.id}` : '/portal/login'"
          >
            {{ loggedIn ? '立即提交酒款' : '登录后报名' }}
          </RouterLink>
        </div>
      </div>
      <aside class="hero-card">
        <span>{{ activeCompetition.shortName }}</span>
        <strong>精酿厂牌年度征集</strong>
        <dl>
          <div><dt>主办方</dt><dd>{{ activeCompetition.organizer }}</dd></div>
          <div><dt>投递组别</dt><dd>{{ activeCompetition.categories.length }} 个组别</dd></div>
          <div><dt>结果反馈</dt><dd>奖项、评分、桌长评语</dd></div>
        </dl>
      </aside>
    </section>

    <section class="section-block">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">开放报名赛事</h2>
          <p>先查看赛事规则和送样要求，再决定提交哪一款酒。</p>
        </div>
        <RouterLink to="/portal/events">全部赛事</RouterLink>
      </div>
      <div class="event-grid">
        <article v-for="competition in openCompetitions" :key="competition.id" class="event-card brewer-card">
          <span :class="['label-chip', competition.id === activeCompetition.id ? 'tone-green' : 'tone-amber']">
            {{ competition.id === activeCompetition.id ? '重点赛事' : competition.stage }}
          </span>
          <h3>{{ competition.name }}</h3>
          <p>{{ competition.city }} · {{ competition.venue }}</p>
          <dl>
            <div><dt>报名截止</dt><dd>{{ competition.registrationDeadline }}</dd></div>
            <div><dt>报名费</dt><dd>¥{{ competition.entryFee }} / 款</dd></div>
            <div><dt>比赛日期</dt><dd>{{ competition.date }}</dd></div>
          </dl>
          <div class="card-actions">
            <RouterLink :to="`/portal/events/${competition.id}`">查看赛事</RouterLink>
            <RouterLink
              v-if="canSubmitEntry(competition)"
              class="card-primary"
              :to="loggedIn ? `/portal/submit?competitionId=${competition.id}` : '/portal/login'"
            >
              {{ loggedIn ? '提交酒款' : '登录报名' }}
            </RouterLink>
          </div>
        </article>
      </div>
    </section>

    <section class="reason-band">
      <div>
        <span class="label-chip tone-gold">为什么参赛</span>
        <h2>让酒款被专业评审认真品评，也让厂牌得到可复盘的反馈。</h2>
      </div>
      <div class="reason-grid">
        <article v-for="item in reasons" :key="item.title">
          <strong>{{ item.title }}</strong>
          <p>{{ item.text }}</p>
        </article>
      </div>
    </section>

    <section id="entry-flow" class="section-block">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">参赛流程</h2>
          <p>从报名到结果查看，每一步都围绕厂商需要完成的事项设计。</p>
        </div>
      </div>
      <div class="flow-grid">
        <article v-for="step in flowSteps" :key="step.index" class="flow-step">
          <span>{{ step.index }}</span>
          <strong>{{ step.title }}</strong>
          <p>{{ step.text }}</p>
        </article>
      </div>
    </section>

    <section class="guide-grid">
      <article class="brewer-card guide-card">
        <h2 class="portal-section-title">送样与标签</h2>
        <dl>
          <div><dt>参赛编号</dt><dd>提交酒款后生成，用于厂商和主办方核对报名记录。</dd></div>
          <div><dt>现场短编号</dt><dd>展示在标签下方，扫码失败时供现场人工输入。</dd></div>
          <div><dt>标签用途</dt><dd>付款确认后下载并贴在酒瓶或外箱，主办方收样后确认入库。</dd></div>
        </dl>
      </article>
      <article class="brewer-card guide-card">
        <h2 class="portal-section-title">常见问题</h2>
        <div class="faq-list">
          <details v-for="item in faqs" :key="item.question">
            <summary>{{ item.question }}</summary>
            <p>{{ item.answer }}</p>
          </details>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'
import { activeCompetition, competitions, entries } from './mockData'
import { canSubmitEntry } from './portalViewModels'

const loggedIn = computed(() => isLoggedIn('portal'))
const openCompetitions = computed(() => competitions.filter((competition) => competition.status === 'REGISTRATION_OPEN'))
const pendingCount = computed(() => entries.filter((entry) => entry.status === 'PENDING_PAYMENT' || entry.status === 'REGISTERED').length)

const reasons = [
  { title: '奖项背书', text: '通过组别奖项和结果发布，为年度代表作积累可传播的赛事证明。' },
  { title: '专业反馈', text: '结果发布后查看评分、桌长评语和评审建议，帮助复盘风格表达。' },
  { title: '厂牌曝光', text: '开放报名赛事面向精酿厂牌和行业观众，适合展示新品和稳定款。' },
]

const flowSteps = [
  { index: '01', title: '选择赛事', text: '查看日期、费用、投递组别和送样要求，确认适合提交的酒款。' },
  { index: '02', title: '提交酒款', text: '填写酒名、组别、基础风格、ABV、简介和赛事配置的额外字段。' },
  { index: '03', title: '付款确认', text: '按赛事说明完成线下付款，等待主办方确认报名状态。' },
  { index: '04', title: '下载标签', text: '付款确认后下载现场标签，标签包含参赛编号和现场短编号。' },
  { index: '05', title: '送样入库', text: '按要求寄送或现场交付酒样，主办方收样后更新入库状态。' },
  { index: '06', title: '查看结果', text: '结果发布后查看奖项、评分、桌长评语和奖状相关信息。' },
]

const faqs = [
  { question: '报名截止后还能提交酒款吗？', answer: '报名截止后不再开放提交入口，如需调整请联系主办方确认。' },
  { question: '付款后多久可以下载标签？', answer: '本版本采用线下付款确认，主办方确认后酒款状态变为报名成功，即可下载标签。' },
  { question: '酒样需要寄几瓶？', answer: '不同赛事要求可能不同，请以单场赛事详情页的送样要求为准。' },
  { question: '结果发布后能看到什么？', answer: '可查看奖项、评分明细、桌长综合评语；获奖酒款可处理奖状和收件地址。' },
]
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
}

.my-notice {
  display: block;
  padding: 13px 16px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.site-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  min-height: 520px;
  padding: 42px;
  color: #fff8e8;
  background:
    linear-gradient(135deg, rgba(31, 21, 14, 0.92), rgba(78, 43, 16, 0.74)),
    url("https://images.unsplash.com/photo-1532634786-c8f8c86a0062?auto=format&fit=crop&w=1800&q=80");
  background-position: center;
  background-size: cover;
  border-radius: 8px;
  box-shadow: 0 24px 60px rgba(67, 43, 17, 0.14);
}

.hero-copy {
  align-self: end;
  max-width: 860px;
}

.hero-copy h1 {
  margin: 18px 0 14px;
  font-size: 64px;
  line-height: 0.98;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 760px;
  margin: 0;
  color: #eadabd;
  font-size: 18px;
  line-height: 1.7;
}

.hero-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-facts span {
  padding: 7px 11px;
  background: rgba(255, 250, 240, 0.12);
  border: 1px solid rgba(255, 250, 240, 0.22);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
}

.hero-actions,
.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.primary-action,
.secondary-action,
.text-action,
.card-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.primary-action,
.card-primary {
  color: #2b1d10;
  background: #e1a23d;
}

.secondary-action,
.text-action {
  color: #fff8e8;
  background: rgba(255, 250, 240, 0.13);
  border: 1px solid rgba(255, 250, 240, 0.26);
}

.hero-card {
  align-self: end;
  padding: 24px;
  color: #2b1d10;
  background: linear-gradient(180deg, rgba(255, 250, 240, 0.98), rgba(246, 217, 150, 0.95));
  border-radius: 8px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.24);
}

.hero-card > span {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.hero-card > strong {
  display: block;
  margin-top: 18px;
  font-size: 25px;
  line-height: 1.2;
}

.section-block,
.reason-band,
.guide-card {
  padding: 26px;
}

.section-block,
.reason-band {
  background: rgba(255, 250, 240, 0.9);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.section-head p {
  margin: 0;
  color: #746a5f;
  line-height: 1.6;
}

.section-head a {
  color: #8b5c19;
  font-weight: 800;
  text-decoration: none;
}

.event-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.event-card {
  padding: 22px;
}

.event-card h3 {
  margin: 18px 0 8px;
  font-size: 26px;
  line-height: 1.2;
}

.event-card p,
.reason-grid p,
.flow-step p,
.faq-list p,
dt {
  color: #746a5f;
}

dl {
  display: grid;
  gap: 12px;
  margin: 20px 0 0;
}

dl div {
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(87, 58, 26, 0.18);
}

dt,
dd {
  margin: 0;
}

dd {
  margin-top: 5px;
  font-weight: 800;
  line-height: 1.45;
}

.card-actions a {
  color: #6b4710;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.reason-band {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: 24px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(35, 24, 16, 0.96), rgba(91, 49, 18, 0.9)),
    url("https://images.unsplash.com/photo-1566633806327-68e152aaf26d?auto=format&fit=crop&w=1600&q=80");
  background-position: center;
  background-size: cover;
}

.reason-band h2 {
  margin: 18px 0 0;
  font-size: 36px;
  line-height: 1.12;
}

.reason-grid {
  display: grid;
  gap: 12px;
}

.reason-grid article {
  padding: 18px;
  background: rgba(255, 250, 240, 0.1);
  border: 1px solid rgba(255, 250, 240, 0.18);
  border-radius: 8px;
}

.reason-grid p {
  margin-bottom: 0;
  color: #e6d6b8;
  line-height: 1.65;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.flow-step {
  min-height: 170px;
  padding: 18px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.flow-step span {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 50%;
  font-weight: 900;
}

.flow-step strong {
  display: block;
  margin-top: 18px;
  font-size: 18px;
}

.flow-step p {
  line-height: 1.65;
}

.guide-grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 18px;
}

.faq-list {
  display: grid;
  gap: 10px;
}

details {
  padding: 14px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

summary {
  cursor: pointer;
  font-weight: 800;
}

details p {
  margin-bottom: 0;
  line-height: 1.65;
}

@media (max-width: 1120px) {
  .site-hero,
  .reason-band,
  .guide-grid {
    grid-template-columns: 1fr;
  }

  .event-grid,
  .flow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .site-hero {
    min-height: auto;
    padding: 28px;
  }

  .hero-copy h1 {
    font-size: 42px;
  }

  .section-head,
  .event-grid,
  .flow-grid {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
