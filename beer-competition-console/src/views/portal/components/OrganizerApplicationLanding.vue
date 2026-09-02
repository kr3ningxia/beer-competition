<template>
  <div class="organizer-landing">
    <section class="landing-hero" aria-labelledby="organizer-landing-title">
      <div class="hero-photo" aria-hidden="true"></div>
      <div class="hero-shade" aria-hidden="true"></div>
      <div class="hero-grain" aria-hidden="true"></div>

      <div class="hero-copy" data-reveal>
        <p class="hero-label">啤酒事务局赛事平台</p>
        <h1 id="organizer-landing-title">办一场比赛<br><em>从想法到落地</em></h1>
        <p class="hero-summary">从报名、收样、匿名评审到发布结果，一个流程走到底</p>
        <div class="hero-actions">
          <button class="action-primary" type="button" @click="$emit('apply')">
            申请成为主办方
            <Right />
          </button>
          <RouterLink class="action-quiet" to="/portal/organizer-application/status">
            <Search />
            查询申请
          </RouterLink>
        </div>
      </div>

      <div class="hero-index" aria-hidden="true">
        <span>01</span>
        <i></i>
        <span>06</span>
      </div>

      <a class="scroll-cue" href="#competition-journey">
        <span>看看一场赛事如何运转</span>
        <ArrowDown />
      </a>
    </section>

    <section id="competition-journey" ref="journeySection" class="journey-section">
      <div class="journey-heading" data-reveal>
        <h2>办一场赛，别再东一处西一处</h2>
        <p>从报名到发榜，一站式解决</p>
      </div>

      <div
        class="journey-stage"
        @mouseenter="journeyPaused = true"
        @mouseleave="journeyPaused = false"
      >
        <nav class="journey-nav" aria-label="办赛流程">
          <button
            v-for="(stage, index) in journeyStages"
            :key="stage.key"
            type="button"
            :class="{ active: activeJourney === index }"
            :aria-current="activeJourney === index ? 'step' : undefined"
            @click="selectJourney(index)"
          >
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <strong>{{ stage.label }}</strong>
          </button>
        </nav>

        <div class="journey-window">
          <div class="journey-copy">
            <Transition name="journey-copy" mode="out-in">
              <div :key="activeStage.key">
                <span class="journey-number">0{{ activeJourney + 1 }}</span>
                <h3>{{ activeStage.title }}</h3>
                <p>{{ activeStage.description }}</p>
                <ul>
                  <li v-for="item in activeStage.points" :key="item">
                    <Check />
                    {{ item }}
                  </li>
                </ul>
              </div>
            </Transition>
          </div>

          <div class="journey-visual" :class="`stage-${activeStage.key}`" aria-hidden="true">
            <div class="amber-orbit"></div>
            <Transition name="visual-shift" mode="out-in">
              <div :key="activeStage.key" class="visual-scene">
                <template v-if="activeStage.key === 'create'">
                  <div class="event-sheet">
                    <span class="sheet-mark">BC</span>
                    <i></i><i></i><i></i>
                    <strong>CRAFT BEER<br>AWARDS</strong>
                    <small>赛事草稿</small>
                  </div>
                  <div class="date-stamp">SEP<br><b>18</b></div>
                </template>

                <template v-else-if="activeStage.key === 'entry'">
                  <div v-for="(beer, index) in beerEntries" :key="beer.code" class="beer-entry" :style="{ '--index': index }">
                    <span>{{ beer.code }}</span>
                    <strong>{{ beer.style }}</strong>
                    <i></i>
                  </div>
                  <div class="entry-total"><b>128</b><span>参赛酒款</span></div>
                </template>

                <template v-else-if="activeStage.key === 'code'">
                  <div class="sample-glass"><i></i></div>
                  <div class="code-label">
                    <span>BLIND SAMPLE</span>
                    <strong>A-037</strong>
                    <div class="barcode"></div>
                  </div>
                  <div class="identity-seal"><Lock /></div>
                </template>

                <template v-else-if="activeStage.key === 'judge'">
                  <div class="judge-table">
                    <span v-for="index in 6" :key="index" :style="{ '--seat': index }"><User /></span>
                    <strong>T03</strong>
                    <small>IPA · 第二轮</small>
                  </div>
                  <div class="judge-note">6 位评委已就位</div>
                </template>

                <template v-else-if="activeStage.key === 'score'">
                  <div class="score-card">
                    <span>AROMA</span><b>18</b><i style="--score: 82%"></i>
                    <span>FLAVOR</span><b>17</b><i style="--score: 76%"></i>
                    <span>BALANCE</span><b>9</b><i style="--score: 88%"></i>
                  </div>
                  <div class="score-total"><span>TOTAL</span><b>44</b><small>/ 50</small></div>
                </template>

                <template v-else>
                  <div class="award-rings"><span></span><span></span><span></span></div>
                  <div class="award-medal">
                    <Trophy />
                    <strong>GOLD</strong>
                    <small>2026</small>
                  </div>
                  <div class="result-published"><CircleCheck /> 结果已发布</div>
                </template>
              </div>
            </Transition>
          </div>
        </div>

        <div class="journey-progress" aria-hidden="true">
          <i :style="{ width: `${((activeJourney + 1) / journeyStages.length) * 100}%` }"></i>
        </div>
      </div>
    </section>

    <section class="ownership-section">
      <div class="ownership-photo" data-reveal>
        <div class="photo-caption">
          <span>独立赛事</span>
          <strong>你的品牌<br>你的规则<br>你的收入</strong>
        </div>
      </div>
      <div class="ownership-copy" data-reveal>
        <h2>主办方掌握<br>自己的赛事</h2>
        <div class="ownership-list">
          <article>
            <span>01</span>
            <div>
              <strong>独立运营</strong>
              <p>自主设置赛事规则、报名方式和评审安排</p>
            </div>
          </article>
          <article>
            <span>02</span>
            <div>
              <strong>自主收款</strong>
              <p>报名费直接进您账户，平台不碰</p>
            </div>
          </article>
          <article>
            <span>03</span>
            <div>
              <strong>各管各的</strong>
              <p>只看得到自己的赛事和资料</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="application-bridge">
      <div class="bridge-line" aria-hidden="true">
        <i></i><i></i><i></i><i></i>
      </div>
      <div class="bridge-steps" data-reveal>
        <span>提交申请</span>
        <Right />
        <span>平台审核</span>
        <Right />
        <span>开通账号</span>
        <Right />
        <span>创建赛事</span>
      </div>
      <div class="bridge-content" data-reveal>
        <h2>准备好<br>就提交申请</h2>
        <p>先留下组织和联系人信息，审核通过后再进入后台完善赛事</p>
        <div class="bridge-actions">
          <button class="action-primary action-light" type="button" @click="$emit('apply')">
            开始申请
            <Right />
          </button>
          <RouterLink class="action-quiet action-quiet-light" to="/portal/organizer-application/status">
            已提交过申请
          </RouterLink>
        </div>
      </div>
      <div class="bridge-glass" aria-hidden="true">
        <span class="glass-foam"></span>
        <span class="glass-beer"></span>
        <span class="glass-shine"></span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  ArrowDown,
  Check,
  CircleCheck,
  Lock,
  Right,
  Search,
  Trophy,
  User,
} from '@element-plus/icons-vue'

defineEmits(['apply'])

const journeyStages = [
  {
    key: 'create',
    label: '创建赛事',
    title: '先把规则和日子定下来',
    description: '报名时间、比赛地点、组别和评分方式都能改，发布前先看一遍草稿',
    points: ['规则和日子都能改', '发布前先看一遍'],
  },
  {
    key: 'entry',
    label: '报名收样',
    title: '每款酒到哪、谁付了款，一查便知',
    description: '报名、付款、送样、入库的状态都随时能查，不用再翻群聊和表格',
    points: ['酒款资料一手建档', '收样状态随时可查'],
  },
  {
    key: 'code',
    label: '匿名编码',
    title: '评审只看得到编号，看不到厂牌',
    description: '入库酒款自动生成匿名编号，评审过程把厂牌和酒款身份藏起来',
    points: ['匿名样品编号', '标签与酒款可追溯'],
  },
  {
    key: 'judge',
    label: '评委安排',
    title: '评委、桌次和任务，在开赛前各就各位',
    description: '评委定了之后，日期、评审桌、任务一次排好，现场按表分工',
    points: ['评委分工一目了然', '评审桌次集中安排'],
  },
  {
    key: 'score',
    label: '现场评分',
    title: '评委扫码打分，分数当场汇总',
    description: '评委手机打分、写评语，桌长汇总，主办方盯着实时进度',
    points: ['匿名扫码评分', '评分评语同步汇总'],
  },
  {
    key: 'result',
    label: '发布结果',
    title: '最后一张评分表收尾，结果自动排好',
    description: '主办方确认奖项后发布结果，参赛者能看自己的评分、评语和获奖情况',
    points: ['奖项统一确认', '结果第一时间发布'],
  },
]

const beerEntries = [
  { code: 'A-031', style: 'American IPA' },
  { code: 'B-012', style: 'Belgian Ale' },
  { code: 'C-046', style: 'Imperial Stout' },
]

const activeJourney = ref(0)
const journeyPaused = ref(false)
const journeySection = ref(null)
const journeyVisible = ref(false)
const activeStage = computed(() => journeyStages[activeJourney.value])
let journeyTimer = null
let revealObserver = null
let journeyObserver = null

function selectJourney(index) {
  activeJourney.value = index
  journeyPaused.value = true
  window.setTimeout(() => { journeyPaused.value = false }, 4000)
}

onMounted(async () => {
  await nextTick()
  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) entry.target.classList.add('is-visible')
    })
  }, { threshold: 0.16 })
  document.querySelectorAll('.organizer-landing [data-reveal]').forEach((element) => revealObserver.observe(element))

  journeyObserver = new IntersectionObserver(([entry]) => {
    journeyVisible.value = entry.isIntersecting
  }, { threshold: 0.35 })
  if (journeySection.value) journeyObserver.observe(journeySection.value)

  journeyTimer = window.setInterval(() => {
    if (journeyVisible.value && !journeyPaused.value && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      activeJourney.value = (activeJourney.value + 1) % journeyStages.length
    }
  }, 3600)
})

onUnmounted(() => {
  revealObserver?.disconnect()
  journeyObserver?.disconnect()
  window.clearInterval(journeyTimer)
})
</script>

<style scoped>
.organizer-landing {
  --night: #090a08;
  --charcoal: #12130f;
  --bone: #eee9dc;
  --paper: #f6f1e6;
  --amber: #f3a712;
  --amber-soft: #ffc94a;
  --green: #78945b;
  overflow: hidden;
  color: var(--bone);
  background: var(--night);
  font-family: "Noto Sans SC", "Microsoft YaHei", sans-serif;
}

.landing-hero {
  position: relative;
  isolation: isolate;
  min-height: min(860px, calc(100svh - 68px));
  display: grid;
  align-items: center;
  padding: clamp(88px, 11vw, 152px) max(6vw, 28px) 92px;
  background: #0c0c09;
}

.hero-photo,
.hero-shade,
.hero-grain {
  position: absolute;
  inset: 0;
  z-index: -2;
  pointer-events: none;
}

.hero-photo {
  background:
    linear-gradient(90deg, #090a08 0%, rgba(9, 10, 8, 0.9) 31%, rgba(9, 10, 8, 0.18) 72%, rgba(9, 10, 8, 0.42) 100%),
    url("https://images.unsplash.com/photo-1535958636474-b021ee887b13?auto=format&fit=crop&w=2200&q=88") center 48% / cover no-repeat;
  animation: hero-breathe 16s ease-in-out infinite alternate;
}

.hero-shade {
  z-index: -1;
  background:
    radial-gradient(circle at 72% 48%, rgba(242, 151, 22, 0.2), transparent 29%),
    linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.52));
}

.hero-grain {
  z-index: -1;
  opacity: 0.1;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 180 180' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.9' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.55'/%3E%3C/svg%3E");
}

.hero-copy {
  width: min(720px, 66vw);
}

.hero-label {
  margin: 0 0 28px;
  color: var(--amber-soft);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
}

.hero-copy h1,
.journey-heading h2,
.ownership-copy h2,
.bridge-content h2 {
  margin: 0;
  font-family: "Noto Serif SC", "Songti SC", serif;
  font-weight: 800;
  letter-spacing: 0;
}

.hero-copy h1 {
  max-width: 720px;
  font-size: clamp(50px, 6.2vw, 100px);
  line-height: 1.08;
}

.hero-copy h1 em {
  color: var(--amber-soft);
  font-style: normal;
}

.hero-summary {
  max-width: 520px;
  margin: 34px 0 0;
  color: rgba(238, 233, 220, 0.78);
  font-size: clamp(16px, 1.4vw, 21px);
  line-height: 1.8;
}

.hero-actions,
.bridge-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 40px;
}

.action-primary,
.action-quiet {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 52px;
  padding: 0 24px;
  border-radius: 4px;
  font: inherit;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
  transition: transform 0.3s ease, background 0.3s ease, color 0.3s ease, border-color 0.3s ease;
}

.action-primary {
  color: #141208;
  background: var(--amber-soft);
  border: 1px solid var(--amber-soft);
}

.action-primary:hover {
  background: #ffd971;
  transform: translateY(-3px);
}

.action-primary svg,
.action-quiet svg {
  width: 18px;
  height: 18px;
}

.action-primary:hover svg {
  transform: translateX(4px);
}

.action-quiet {
  color: var(--bone);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.action-quiet:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.hero-index {
  position: absolute;
  right: max(3vw, 24px);
  top: 50%;
  display: grid;
  justify-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.52);
  font-size: 11px;
  transform: translateY(-50%);
}

.hero-index i {
  width: 1px;
  height: 120px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.2);
}

.hero-index i::after {
  content: "";
  display: block;
  width: 100%;
  height: 42%;
  background: var(--amber-soft);
  animation: index-flow 3s ease-in-out infinite;
}

.scroll-cue {
  position: absolute;
  left: max(6vw, 28px);
  bottom: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  text-decoration: none;
}

.scroll-cue svg {
  width: 17px;
  animation: scroll-cue 1.8s ease-in-out infinite;
}

.journey-section {
  min-height: 100vh;
  padding: clamp(88px, 10vw, 148px) max(6vw, 28px);
  color: #191a15;
  background: var(--paper);
}

.journey-heading {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.65fr);
  align-items: start;
  gap: 56px;
  max-width: 1440px;
  margin: 0 auto 24px;
}

.journey-heading h2 {
  font-size: clamp(36px, 3.8vw, 58px);
  line-height: 1.2;
  white-space: nowrap;
}

.journey-heading p {
  margin: 112px 0 0;
  color: #3f4438;
  font-family: "Noto Serif SC", "Songti SC", serif;
  font-size: clamp(20px, 2vw, 30px);
  font-weight: 600;
  line-height: 1.45;
}

.journey-stage {
  max-width: 1440px;
  margin: 0 auto;
}

.journey-nav {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  border-top: 1px solid #c8c2b4;
}

.journey-nav button {
  position: relative;
  display: grid;
  gap: 14px;
  min-height: 94px;
  padding: 19px 14px;
  color: #77746b;
  text-align: left;
  background: transparent;
  border: 0;
  border-right: 1px solid #d9d3c5;
  cursor: pointer;
  transition: color 0.28s ease, background 0.28s ease;
}

.journey-nav button::before {
  content: "";
  position: absolute;
  top: -2px;
  left: 0;
  width: 0;
  height: 3px;
  background: #191a15;
  transition: width 0.36s ease;
}

.journey-nav button.active {
  color: #151610;
  background: rgba(255, 255, 255, 0.5);
}

.journey-nav button.active::before {
  width: 100%;
}

.journey-nav span {
  font-size: 11px;
}

.journey-nav strong {
  font-size: 14px;
}

.journey-window {
  display: grid;
  grid-template-columns: minmax(320px, 0.86fr) minmax(460px, 1.4fr);
  min-height: 520px;
  overflow: hidden;
  background: #161711;
}

.journey-copy {
  display: grid;
  align-items: center;
  padding: clamp(38px, 5vw, 76px);
  color: var(--bone);
}

.journey-number {
  display: block;
  margin-bottom: 28px;
  color: var(--amber-soft);
  font-size: 13px;
  font-weight: 900;
}

.journey-copy h3 {
  margin: 0;
  font-family: "Noto Serif SC", "Songti SC", serif;
  font-size: clamp(28px, 3vw, 46px);
  line-height: 1.35;
}

.journey-copy p {
  margin: 24px 0 0;
  color: rgba(238, 233, 220, 0.68);
  font-size: 15px;
  line-height: 1.9;
}

.journey-copy ul {
  display: grid;
  gap: 11px;
  margin: 28px 0 0;
  padding: 0;
  list-style: none;
}

.journey-copy li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.journey-copy li svg {
  width: 15px;
  color: var(--amber-soft);
}

.journey-visual {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  background:
    linear-gradient(rgba(255,255,255,.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,.04) 1px, transparent 1px),
    radial-gradient(circle at 50% 45%, #4b2b0e 0%, #20180e 38%, #0d0e0b 72%);
  background-size: 44px 44px, 44px 44px, auto;
}

.amber-orbit {
  position: absolute;
  left: 50%;
  top: 50%;
  width: min(36vw, 500px);
  aspect-ratio: 1;
  border: 1px solid rgba(255, 188, 53, 0.24);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.amber-orbit::before,
.amber-orbit::after {
  content: "";
  position: absolute;
  inset: 11%;
  border: 1px solid rgba(255, 188, 53, 0.14);
  border-radius: inherit;
}

.amber-orbit::after {
  inset: 27%;
  background: rgba(255, 171, 35, 0.05);
}

.visual-scene {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
}

.event-sheet {
  position: relative;
  z-index: 2;
  width: min(300px, 48%);
  aspect-ratio: .76;
  padding: 34px;
  color: #18170f;
  background: #f2ead5;
  box-shadow: 30px 34px 70px rgba(0,0,0,.35);
  transform: rotate(-6deg);
}

.event-sheet::before {
  content: "";
  position: absolute;
  inset: 14px;
  border: 1px solid #ba9a52;
}

.sheet-mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  margin-bottom: 60px;
  color: #f6d16b;
  background: #1a281c;
  border-radius: 50%;
  font-weight: 900;
}

.event-sheet i {
  display: block;
  width: 54%;
  height: 2px;
  margin: 7px 0;
  background: #d2bd8d;
}

.event-sheet strong {
  display: block;
  margin-top: 22px;
  font-family: Georgia, serif;
  font-size: clamp(18px, 2vw, 29px);
  line-height: 1.05;
}

.event-sheet small {
  position: absolute;
  right: 30px;
  bottom: 30px;
}

.date-stamp {
  position: absolute;
  right: 16%;
  bottom: 16%;
  z-index: 3;
  width: 90px;
  padding: 14px;
  color: #17170f;
  text-align: center;
  background: var(--amber-soft);
  box-shadow: 12px 14px 40px rgba(0,0,0,.3);
  transform: rotate(8deg);
}

.date-stamp b {
  display: block;
  font-size: 34px;
}

.beer-entry {
  position: absolute;
  top: calc(28% + var(--index) * 84px);
  left: calc(19% + var(--index) * 6%);
  z-index: calc(4 - var(--index));
  display: grid;
  grid-template-columns: 68px 1fr 42px;
  align-items: center;
  width: min(440px, 64%);
  min-height: 70px;
  padding: 0 22px;
  color: #191a15;
  background: #f5eedf;
  box-shadow: 20px 24px 44px rgba(0,0,0,.25);
  animation: entry-arrive .65s both;
  animation-delay: calc(var(--index) * .1s);
}

.beer-entry span { color: #8a6a26; font-size: 12px; font-weight: 900; }
.beer-entry strong { font-size: 14px; }
.beer-entry i { height: 8px; background: #8ea36a; }

.entry-total {
  position: absolute;
  right: 9%;
  top: 13%;
  z-index: 6;
  display: grid;
  padding: 18px;
  color: #191a15;
  background: var(--amber-soft);
}

.entry-total b { font-size: 34px; line-height: 1; }
.entry-total span { margin-top: 6px; font-size: 11px; }

.sample-glass {
  position: absolute;
  left: 20%;
  top: 17%;
  width: 180px;
  height: 300px;
  border: 2px solid rgba(255,255,255,.3);
  border-radius: 20px 20px 72px 72px;
  box-shadow: inset -18px 0 30px rgba(255,255,255,.08), 18px 28px 60px rgba(0,0,0,.36);
  transform: rotate(-5deg);
}

.sample-glass i {
  position: absolute;
  inset: 30% 8px 8px;
  overflow: hidden;
  background: linear-gradient(90deg, #8f4c08, #e99b18 48%, #7b3e07);
  border-radius: 6px 6px 62px 62px;
}

.sample-glass i::before {
  content: "";
  position: absolute;
  left: 0;
  top: -10px;
  width: 100%;
  height: 20px;
  background: #f7e8be;
  border-radius: 50%;
}

.code-label {
  position: absolute;
  right: 12%;
  z-index: 3;
  width: min(320px, 42%);
  padding: 28px;
  color: #191a15;
  background: #f2ead6;
  box-shadow: 24px 28px 60px rgba(0,0,0,.4);
  transform: rotate(4deg);
}

.code-label span { font-size: 10px; font-weight: 900; }
.code-label strong { display: block; margin: 14px 0; font-size: clamp(34px, 5vw, 64px); }

.barcode {
  height: 38px;
  background: repeating-linear-gradient(90deg, #1a1a14 0 2px, transparent 2px 5px, #1a1a14 5px 6px, transparent 6px 10px);
}

.identity-seal {
  position: absolute;
  right: 8%;
  bottom: 12%;
  z-index: 5;
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  color: #191a15;
  background: var(--amber-soft);
  border-radius: 50%;
}

.identity-seal svg { width: 22px; }

.judge-table {
  position: relative;
  display: grid;
  place-items: center;
  width: min(430px, 62%);
  aspect-ratio: 1.5;
  color: #f0e9d9;
  background: #353123;
  border: 2px solid #73633d;
  border-radius: 50%;
  box-shadow: inset 0 0 0 18px #24251b, 22px 30px 70px rgba(0,0,0,.4);
}

.judge-table > span {
  --angle: calc(var(--seat) * 60deg);
  position: absolute;
  left: calc(50% + cos(var(--angle)) * 47%);
  top: calc(50% + sin(var(--angle)) * 70%);
  display: grid;
  place-items: center;
  width: 50px;
  height: 50px;
  color: #171812;
  background: var(--amber-soft);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.judge-table svg { width: 20px; }
.judge-table strong { font-size: 42px; }
.judge-table small { position: absolute; margin-top: 68px; color: #bcb5a4; }

.judge-note,
.result-published {
  position: absolute;
  right: 9%;
  bottom: 9%;
  padding: 12px 16px;
  color: #171812;
  background: #f2ead6;
  font-size: 12px;
  font-weight: 800;
}

.score-card {
  display: grid;
  grid-template-columns: 100px 40px 1fr;
  align-items: center;
  gap: 18px 12px;
  width: min(480px, 66%);
  padding: 40px;
  color: #ded7c8;
  background: rgba(13,14,11,.84);
  border: 1px solid #5b4c2b;
  box-shadow: 24px 30px 70px rgba(0,0,0,.35);
}

.score-card span { font-size: 11px; color: #9f988a; }
.score-card b { color: var(--amber-soft); font-size: 22px; }
.score-card i { position: relative; height: 4px; background: #3c3b32; }
.score-card i::after { content: ""; position: absolute; inset: 0 auto 0 0; width: var(--score); background: var(--amber-soft); animation: score-grow .8s both; }

.score-total {
  position: absolute;
  right: 8%;
  top: 12%;
  display: grid;
  padding: 18px;
  color: #171812;
  background: var(--amber-soft);
}
.score-total span { font-size: 9px; }
.score-total b { font-size: 42px; line-height: 1; }
.score-total small { text-align: right; }

.award-rings,
.award-rings span {
  position: absolute;
  left: 50%;
  top: 50%;
  border: 1px solid rgba(255,195,62,.26);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}
.award-rings { width: 390px; height: 390px; animation: rings-spin 18s linear infinite; }
.award-rings span:first-child { width: 290px; height: 290px; border-style: dashed; }
.award-rings span:nth-child(2) { width: 210px; height: 210px; }
.award-rings span:last-child { width: 120px; height: 120px; background: rgba(255,183,35,.08); }

.award-medal {
  position: relative;
  z-index: 3;
  display: grid;
  justify-items: center;
  gap: 8px;
  width: 190px;
  height: 190px;
  place-content: center;
  color: #171812;
  background: radial-gradient(circle at 35% 28%, #ffe38b, #f2ad22 45%, #a86608 100%);
  border: 10px double #6f4308;
  border-radius: 50%;
  box-shadow: 0 22px 70px rgba(241,164,21,.3);
}
.award-medal svg { width: 44px; height: 44px; }
.award-medal strong { font-size: 23px; }
.award-medal small { font-weight: 900; }
.result-published { display: flex; align-items: center; gap: 8px; }
.result-published svg { width: 16px; }

.journey-progress {
  height: 3px;
  background: #d4cdbd;
}

.journey-progress i {
  display: block;
  height: 100%;
  background: var(--amber);
  transition: width .5s cubic-bezier(.2,.8,.2,1);
}

.ownership-section {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(430px, .95fr);
  min-height: 780px;
  background: #d5dbc5;
}

.ownership-photo {
  position: relative;
  min-height: 680px;
  background:
    linear-gradient(180deg, rgba(0,0,0,.06), rgba(0,0,0,.66)),
    url("https://images.unsplash.com/photo-1505075106905-fb052892c116?auto=format&fit=crop&w=1500&q=88") center / cover no-repeat;
}

.photo-caption {
  position: absolute;
  left: 7%;
  bottom: 7%;
}

.photo-caption span {
  display: block;
  margin-bottom: 16px;
  color: var(--amber-soft);
  font-size: 12px;
  font-weight: 900;
}

.photo-caption strong {
  font-family: "Noto Serif SC", "Songti SC", serif;
  font-size: clamp(30px, 3.2vw, 52px);
  line-height: 1.25;
}

.ownership-copy {
  display: grid;
  align-content: center;
  padding: clamp(60px, 8vw, 126px);
  color: #1c2118;
}

.ownership-copy h2 {
  font-size: clamp(38px, 4.2vw, 66px);
  line-height: 1.2;
}

.ownership-list {
  display: grid;
  margin-top: 52px;
  border-top: 1px solid rgba(28,33,24,.24);
}

.ownership-list article {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 18px;
  padding: 22px 0;
  border-bottom: 1px solid rgba(28,33,24,.24);
}

.ownership-list article > span {
  color: #68704f;
  font-size: 11px;
  font-weight: 900;
}

.ownership-list strong { font-size: 17px; }
.ownership-list p { margin: 7px 0 0; color: #5d6356; font-size: 13px; line-height: 1.7; }

.application-bridge {
  position: relative;
  isolation: isolate;
  min-height: 720px;
  display: grid;
  align-items: center;
  padding: 120px max(8vw, 30px);
  overflow: hidden;
  background: #14150f;
}

.application-bridge::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -2;
  background: radial-gradient(circle at 78% 56%, rgba(231,137,12,.24), transparent 32%);
}

.bridge-line {
  position: absolute;
  top: 0;
  left: 8%;
  right: 8%;
  height: 1px;
  background: rgba(255,255,255,.13);
}

.bridge-line i {
  position: absolute;
  top: -4px;
  width: 9px;
  height: 9px;
  background: var(--amber-soft);
  border-radius: 50%;
}
.bridge-line i:nth-child(1) { left: 0; }
.bridge-line i:nth-child(2) { left: 33%; }
.bridge-line i:nth-child(3) { left: 66%; }
.bridge-line i:nth-child(4) { right: 0; }

.bridge-steps {
  position: absolute;
  left: 8%;
  right: 8%;
  top: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #7f806f;
  font-size: 11px;
}

.bridge-steps svg { width: 13px; }

.bridge-content {
  position: relative;
  z-index: 3;
  width: min(700px, 62vw);
}

.bridge-content h2 {
  font-size: clamp(44px, 5.5vw, 86px);
  line-height: 1.12;
}

.bridge-content p {
  max-width: 520px;
  margin: 30px 0 0;
  color: rgba(238,233,220,.62);
  font-size: 16px;
  line-height: 1.8;
}

.action-light { background: #f4eee0; border-color: #f4eee0; }
.action-quiet-light { border-color: transparent; background: transparent; }

.bridge-glass {
  position: absolute;
  right: clamp(5%, 9vw, 12%);
  bottom: -120px;
  width: min(26vw, 380px);
  height: 600px;
  overflow: hidden;
  border: 2px solid rgba(255,255,255,.18);
  border-radius: 44px 44px 120px 120px;
  box-shadow: inset -30px 0 50px rgba(255,255,255,.06), 0 40px 110px rgba(0,0,0,.45);
  transform: rotate(5deg);
}

.glass-beer {
  position: absolute;
  inset: 22% 10px 10px;
  background: linear-gradient(90deg, #6d3300, #df8310 42%, #f2ad27 57%, #633000);
  border-radius: 8px 8px 105px 105px;
}

.glass-foam {
  position: absolute;
  left: 10px;
  right: 10px;
  top: 16%;
  z-index: 2;
  height: 85px;
  background: #f3e5c3;
  border-radius: 50% 50% 22% 24%;
  animation: foam-float 5s ease-in-out infinite;
}

.glass-shine {
  position: absolute;
  left: 16%;
  top: 8%;
  z-index: 4;
  width: 12%;
  height: 62%;
  background: linear-gradient(180deg, rgba(255,255,255,.42), transparent);
  border-radius: 50%;
  filter: blur(5px);
}

[data-reveal] {
  opacity: 0;
  transform: translateY(34px);
  transition: opacity .9s ease, transform .9s cubic-bezier(.2,.8,.2,1);
}

[data-reveal].is-visible {
  opacity: 1;
  transform: none;
}

.journey-copy-enter-active,
.journey-copy-leave-active,
.visual-shift-enter-active,
.visual-shift-leave-active {
  transition: opacity .35s ease, transform .45s cubic-bezier(.2,.8,.2,1);
}
.journey-copy-enter-from { opacity: 0; transform: translateY(18px); }
.journey-copy-leave-to { opacity: 0; transform: translateY(-12px); }
.visual-shift-enter-from { opacity: 0; transform: scale(.96) rotate(-1deg); }
.visual-shift-leave-to { opacity: 0; transform: scale(1.03); }

@keyframes hero-breathe { to { transform: scale(1.045); } }
@keyframes index-flow { 0%,100% { transform: translateY(-100%); } 50% { transform: translateY(240%); } }
@keyframes scroll-cue { 0%,100% { transform: translateY(0); } 50% { transform: translateY(6px); } }
@keyframes entry-arrive { from { opacity: 0; transform: translateX(45px); } }
@keyframes score-grow { from { width: 0; } }
@keyframes rings-spin { to { transform: translate(-50%, -50%) rotate(360deg); } }
@keyframes foam-float { 50% { transform: translateY(6px) scaleX(1.02); } }

@media (max-width: 960px) {
  .landing-hero { min-height: 760px; }
  .hero-copy { width: min(760px, 84vw); }
  .journey-heading { grid-template-columns: 1fr; gap: 22px; }
  .journey-heading p { margin-top: 18px; }
  .journey-window { grid-template-columns: 1fr; }
  .journey-copy { min-height: 380px; }
  .journey-visual { min-height: 500px; }
  .ownership-section { grid-template-columns: 1fr; }
  .ownership-photo { min-height: 560px; }
  .bridge-content { width: min(620px, 70vw); }
  .bridge-glass { right: 1%; opacity: .72; }
}

@media (max-width: 720px) {
  .landing-hero { min-height: calc(100svh - 60px); padding: 84px 22px 88px; }
  .hero-photo { background-position: 62% center; }
  .hero-copy { width: 100%; }
  .hero-copy h1 { font-size: clamp(42px, 13vw, 66px); }
  .hero-summary { font-size: 15px; }
  .hero-actions, .bridge-actions { align-items: stretch; flex-direction: column; }
  .action-primary, .action-quiet { width: 100%; }
  .hero-index { display: none; }
  .scroll-cue { left: 22px; }
  .journey-section { padding: 72px 18px; }
   .journey-heading { margin-bottom: 34px; }
   .journey-heading h2 { font-size: clamp(18px, 6.5vw, 26px); }
   .journey-heading p { margin-top: 16px; font-size: 20px; }
  .journey-nav { display: flex; overflow-x: auto; scrollbar-width: none; }
  .journey-nav button { flex: 0 0 122px; min-height: 82px; }
  .journey-window { min-height: 0; }
  .journey-copy { min-height: 420px; padding: 34px 26px; }
  .journey-copy h3 { font-size: 28px; }
  .journey-visual { min-height: 390px; }
  .amber-orbit { width: 86vw; }
  .beer-entry { left: calc(8% + var(--index) * 4%); width: 72%; }
  .entry-total { right: 4%; }
  .event-sheet { width: 54%; padding: 22px; }
  .sheet-mark { margin-bottom: 38px; }
  .sample-glass { left: 9%; width: 130px; height: 230px; }
  .code-label { right: 5%; width: 52%; padding: 20px; }
  .judge-table { width: 64%; }
  .judge-table > span { width: 38px; height: 38px; }
  .score-card { width: 78%; padding: 26px; grid-template-columns: 72px 32px 1fr; }
  .award-rings { width: 300px; height: 300px; }
  .ownership-photo { min-height: 500px; }
  .ownership-copy { padding: 68px 24px; }
  .ownership-copy h2 { font-size: 40px; }
  .application-bridge { min-height: 680px; padding: 100px 24px 220px; align-items: start; }
  .bridge-steps { gap: 7px; overflow: hidden; }
  .bridge-steps span { white-space: nowrap; }
  .bridge-content { width: 100%; }
  .bridge-content h2 { font-size: 46px; }
  .bridge-glass { right: 50%; bottom: -330px; width: 240px; height: 520px; transform: translateX(50%) rotate(4deg); }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; scroll-behavior: auto !important; transition-duration: .01ms !important; }
  [data-reveal] { opacity: 1; transform: none; }
}
</style>
