<template>
  <div class="competition-results-page">
    <section class="results-hero">
      <img src="/hero-beer.png" alt="" aria-hidden="true">
      <div class="hero-shade" aria-hidden="true"></div>
      <div class="hero-copy">
        <span class="label-chip tone-gold">赛事结果</span>
        <h1>赛事获奖结果</h1>
        <p>浏览各届赛事的完整获奖名单，了解全场总冠军与各组别金银铜奖。</p>
      </div>
    </section>

    <section class="results-list-block">
      <div class="list-toolbar">
        <div>
          <h2>赛事列表</h2>
          <p>{{ listSummary }}</p>
        </div>
        <label class="search-box">
          <Search aria-hidden="true" />
          <input v-model="keyword" type="search" placeholder="搜索赛事 / 酒款 / 厂牌 / 风格" aria-label="搜索赛事结果">
        </label>
      </div>

      <div v-if="loading" class="empty-state brewer-card">
        <strong>正在读取赛事结果</strong>
      </div>

      <div v-else-if="filteredCompetitions.length" class="competition-list">
        <article v-for="competition in filteredCompetitions" :key="competition.id" class="competition-card">
          <div class="competition-main">
            <div class="competition-copy">
              <h2>{{ competition.name }}</h2>
              <div class="facts">
                <span><Calendar aria-hidden="true" />比赛日期 {{ formatDate(competition.matchDate) }}</span>
                <span><Trophy aria-hidden="true" />获奖 {{ awardCount(competition) }} 项</span>
                <span><Files aria-hidden="true" />{{ groupCount(competition) }} 个投递组别</span>
              </div>
              <p>结果发布时间 {{ formatDateTime(competition.publishedAt) }}</p>
            </div>
            <RouterLink class="detail-button" :to="`/portal/competition-results/${competition.id}`">
              查看详情
              <ArrowRight aria-hidden="true" />
            </RouterLink>
          </div>

          <div v-if="previewEntries(competition).length" class="award-preview">
            <div class="preview-head">
              <strong>获奖预览</strong>
              <RouterLink
                v-if="awardCount(competition) > previewEntries(competition).length"
                :to="`/portal/competition-results/${competition.id}`"
              >
                查看全部 {{ awardCount(competition) }} 项
              </RouterLink>
            </div>
            <ul>
              <li v-for="entry in previewEntries(competition)" :key="entry.awardResultId || entry.id">
                <span :class="['award-badge', awardTone(entry)]">
                  <Medal aria-hidden="true" />
                  {{ awardLabel(entry) }}
                </span>
                <div>
                  <strong>{{ entry.beerName || '获奖酒款' }}</strong>
                  <p>{{ entry.breweryName || '获奖厂牌' }} · {{ entry.groupName || '投递组别' }} · {{ entry.style || '基础风格' }}</p>
                </div>
              </li>
            </ul>
          </div>
        </article>
      </div>

      <div v-else class="empty-state brewer-card">
        <strong>暂无已发布结果</strong>
        <p>结果发布后会在这里展示获奖名单。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowRight, Calendar, Files, Medal, Search, Trophy } from '@element-plus/icons-vue'
import { fetchPortalCompetitionResults } from '@/api/portal'

const competitions = ref([])
const keyword = ref('')
const loading = ref(true)

const filteredCompetitions = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  if (!query) return competitions.value
  return competitions.value.filter((competition) => {
    const haystack = [
      competition.name,
      competition.code,
      competition.edition,
      ...(competition.entries || []).flatMap((entry) => [
        entry.beerName,
        entry.breweryName,
        entry.groupName,
        entry.style,
        awardLabel(entry),
      ]),
    ].filter(Boolean).join(' ').toLowerCase()
    return haystack.includes(query)
  })
})

const listSummary = computed(() => {
  if (keyword.value.trim()) return `找到 ${filteredCompetitions.value.length} 场相关赛事`
  return `共 ${filteredCompetitions.value.length} 场赛事`
})

onMounted(async () => {
  try {
    competitions.value = await fetchPortalCompetitionResults()
  } finally {
    loading.value = false
  }
})

function previewEntries(competition) {
  return sortEntries((competition.entries || []).filter((entry) => !entry.champion)).slice(0, 4)
}

function awardCount(competition) {
  return (competition.entries || []).filter((entry) => !entry.champion).length
}

function groupCount(competition) {
  return competition.groups?.length || new Set((competition.entries || []).map((entry) => entry.groupId).filter(Boolean)).size
}

function sortEntries(entries) {
  return [...entries].sort((left, right) => {
    const leftOrder = awardOrder(left)
    const rightOrder = awardOrder(right)
    if (leftOrder !== rightOrder) return leftOrder - rightOrder
    return Number(left.rankNo || 99) - Number(right.rankNo || 99)
  })
}

function awardOrder(entry) {
  if (entry.champion || entry.awardType === 'CHAMPION') return 0
  if (awardLabel(entry) === '金奖') return 1
  if (awardLabel(entry) === '银奖') return 2
  if (awardLabel(entry) === '铜奖') return 3
  return 9
}

function awardLabel(entry) {
  if (entry?.champion || entry?.awardType === 'CHAMPION') return '总冠军'
  if (entry?.rankNo === 1) return '金奖'
  if (entry?.rankNo === 2) return '银奖'
  if (entry?.rankNo === 3) return '铜奖'
  if (entry?.awardName) return entry.awardName
  return '奖项'
}

function awardTone(entry) {
  const label = awardLabel(entry)
  if (label === '金奖') return 'gold'
  if (label === '银奖') return 'silver'
  if (label === '铜奖') return 'bronze'
  return 'champion'
}

function formatDate(value) {
  return value || '-'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}
</script>

<style scoped>
.competition-results-page {
  display: grid;
  gap: 24px;
  max-width: 1180px;
  margin: 0 auto;
}

.results-hero {
  position: relative;
  display: flex;
  align-items: flex-end;
  min-height: 300px;
  overflow: hidden;
  padding: 32px;
  color: #fffaf0;
  border-radius: 8px;
  background: #170d07;
  box-shadow: 0 24px 60px rgba(67, 43, 17, 0.14);
}

.results-hero img,
.hero-shade {
  position: absolute;
  inset: 0;
}

.results-hero img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 70% center;
}

.hero-shade {
  background:
    linear-gradient(135deg, rgba(32, 22, 15, 0.94), rgba(91, 50, 18, 0.78)),
    rgba(24, 13, 8, 0.26);
}

.hero-copy {
  position: relative;
  max-width: 620px;
}

.hero-copy h1 {
  margin: 18px 0 10px;
  font-size: 48px;
  line-height: 1.06;
  letter-spacing: 0;
}

.hero-copy p {
  margin: 14px 0 0;
  color: #ead9b7;
  line-height: 1.7;
  font-weight: 700;
}

.results-list-block {
  display: grid;
  gap: 22px;
}

.list-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.list-toolbar h2 {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
}

.list-toolbar p {
  margin: 8px 0 0;
  color: #746a5f;
  font-size: 14px;
}

.search-box {
  position: relative;
  display: block;
  width: 258px;
}

.search-box svg {
  position: absolute;
  left: 12px;
  top: 50%;
  width: 16px;
  height: 16px;
  color: #80766a;
  transform: translateY(-50%);
}

.search-box input {
  width: 100%;
  height: 40px;
  padding: 0 12px 0 36px;
  color: #241a10;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  outline: none;
}

.search-box input:focus {
  border-color: rgba(184, 117, 23, 0.45);
  box-shadow: 0 0 0 3px rgba(184, 117, 23, 0.12);
}

.competition-list {
  display: grid;
  gap: 20px;
}

.competition-card {
  overflow: hidden;
  background: rgba(255, 250, 240, 0.9);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(67, 43, 17, 0.08);
}

.competition-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 26px 24px;
}

.competition-copy h2 {
  margin: 0 0 16px;
  font-size: 24px;
  line-height: 1.2;
}

.competition-copy p {
  margin: 14px 0 0;
  color: #746a5f;
  font-size: 13px;
}

.facts {
  display: flex;
  flex-wrap: wrap;
  gap: 14px 18px;
  color: #746a5f;
  font-size: 14px;
}

.facts span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.facts svg,
.detail-button svg,
.award-badge svg {
  width: 16px;
  height: 16px;
}

.detail-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 16px;
  color: #fffaf0;
  background: #d89021;
  border: 1px solid rgba(130, 83, 25, 0.12);
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.award-preview {
  padding: 16px 26px 20px;
  background: rgba(245, 238, 222, 0.66);
  border-top: 1px solid rgba(87, 58, 26, 0.12);
}

.preview-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  color: #746a5f;
  font-size: 13px;
}

.preview-head a {
  color: #9a621e;
  font-weight: 800;
  text-decoration: none;
}

.award-preview ul {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.award-preview li {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 10px 12px;
  background: rgba(255, 253, 247, 0.86);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.award-preview li > div {
  min-width: 0;
}

.award-preview li strong,
.award-preview li p {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.award-preview li strong {
  font-size: 14px;
}

.award-preview li p {
  margin: 4px 0 0;
  color: #746a5f;
  font-size: 12px;
}

.award-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: 0 0 auto;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
}

.award-badge.champion,
.award-badge.gold {
  color: #704813;
  background: #f7dfaa;
}

.award-badge.silver {
  color: #4d4944;
  background: #ede8dd;
}

.award-badge.bronze {
  color: #6d4526;
  background: #ead6c0;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 180px;
  color: #746a5f;
  text-align: center;
}

.empty-state strong {
  color: #241a10;
}

.empty-state p {
  margin: 0;
}

@media (max-width: 760px) {
  .competition-results-page {
    gap: 22px;
  }

  .results-hero {
    min-height: 230px;
    padding: 34px 26px;
    border-radius: 8px;
  }

  .hero-copy h1 {
    font-size: 30px;
  }

  .list-toolbar,
  .competition-main {
    display: grid;
  }

  .search-box {
    width: 100%;
  }

  .detail-button {
    justify-self: start;
  }

  .award-preview ul {
    grid-template-columns: 1fr;
  }
}
</style>
