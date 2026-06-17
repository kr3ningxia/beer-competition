<template>
  <div class="result-detail-page">
    <RouterLink class="back-link" to="/portal/competition-results">
      <ArrowLeft aria-hidden="true" />
      返回赛事结果列表
    </RouterLink>

    <template v-if="competition">
      <header class="summary-card">
        <h1>{{ competition.name }}</h1>
        <dl>
          <div>
            <dt>比赛日期</dt>
            <dd>{{ formatDate(competition.matchDate) }}</dd>
          </div>
          <div>
            <dt>结果发布时间</dt>
            <dd>{{ formatDateTime(competition.publishedAt) }}</dd>
          </div>
          <div>
            <dt>投递组别</dt>
            <dd>{{ groupOptions.length }} 个</dd>
          </div>
          <div>
            <dt>获奖</dt>
            <dd>{{ groupAwardCount }} 项</dd>
          </div>
        </dl>
      </header>

      <section v-if="championVisible && champion" class="champion-card" aria-label="全场总冠军">
        <div class="gold-line" aria-hidden="true"></div>
        <div class="champion-inner">
          <span class="champion-icon"><Trophy aria-hidden="true" /></span>
          <div>
            <p>全场总冠军 · 最高荣誉</p>
            <h2>{{ champion.beerName || '获奖酒款' }}</h2>
            <strong>{{ champion.breweryName || '获奖厂牌' }} · {{ champion.style || '基础风格' }}</strong>
            <em><Files aria-hidden="true" />跨组别评出 · 来自「{{ champion.groupName || '投递组别' }}」</em>
          </div>
        </div>
      </section>

      <section class="award-board">
        <div class="award-toolbar">
          <h2>组别获奖名单</h2>
          <div class="filters">
            <label class="search-box">
              <Search aria-hidden="true" />
              <input v-model="keyword" type="search" placeholder="搜索酒款 / 厂牌 / 风格" aria-label="搜索本场结果">
            </label>
            <select v-model="groupFilter" aria-label="按组别筛选">
              <option value="all">全部组别</option>
              <option v-for="group in groupOptions" :key="group.id" :value="String(group.id)">{{ group.name }}</option>
            </select>
            <select v-model="awardFilter" aria-label="按奖项筛选">
              <option value="all">全部奖项</option>
              <option value="金奖">金奖</option>
              <option value="银奖">银奖</option>
              <option value="铜奖">铜奖</option>
            </select>
          </div>
        </div>

        <div v-if="groupedEntries.length" class="group-list">
          <section v-for="group in groupedEntries" :key="group.id" class="group-card">
            <header>
              <h3>{{ group.name }}</h3>
              <span>获奖 {{ group.entries.length }} 项</span>
            </header>
            <ul>
              <li v-for="entry in group.entries" :key="entry.awardResultId || entry.id">
                <span :class="['award-badge', awardTone(entry)]">
                  <Medal aria-hidden="true" />
                  {{ awardLabel(entry) }}
                </span>
                <div>
                  <strong>{{ entry.beerName || '获奖酒款' }}</strong>
                  <p>{{ entry.breweryName || '获奖厂牌' }} · {{ entry.style || '基础风格' }}</p>
                </div>
              </li>
            </ul>
          </section>
        </div>

        <div v-else class="empty-state brewer-card">
          <strong>未找到匹配的获奖记录</strong>
          <p>调整关键词、组别或奖项筛选后再查看</p>
        </div>
      </section>
    </template>

    <div v-else class="empty-state brewer-card">
      <strong>{{ loading ? '正在读取赛事结果' : '暂无已发布赛事结果' }}</strong>
      <p v-if="!loading">返回列表查看其他赛事</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeft, Files, Medal, Search, Trophy } from '@element-plus/icons-vue'
import { fetchPortalCompetitionResultDetail } from '@/api/portal'

const route = useRoute()
const competition = ref(null)
const loading = ref(true)
const keyword = ref('')
const groupFilter = ref('all')
const awardFilter = ref('all')

const entries = computed(() => competition.value?.entries || [])
const champion = computed(() => entries.value.find((entry) => entry.champion || entry.awardType === 'CHAMPION') || null)
const groupAwardCount = computed(() => entries.value.filter((entry) => !entry.champion && entry.awardType !== 'CHAMPION').length)
const groupOptions = computed(() => {
  const groups = new Map()
  ;(competition.value?.groups || []).forEach((group) => {
    if (group?.id) groups.set(String(group.id), { id: group.id, name: group.name || '未分组' })
  })
  entries.value.forEach((entry) => {
    if (entry.groupId) groups.set(String(entry.groupId), { id: entry.groupId, name: entry.groupName || '未分组' })
  })
  return Array.from(groups.values())
})

const filteredEntries = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return entries.value.filter((entry) => {
    if (entry.champion || entry.awardType === 'CHAMPION') return false
    if (groupFilter.value !== 'all' && String(entry.groupId) !== groupFilter.value) return false
    if (awardFilter.value !== 'all' && awardLabel(entry) !== awardFilter.value) return false
    if (!query) return true
    return [entry.beerName, entry.breweryName, entry.style, entry.groupName]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
      .includes(query)
  })
})

const groupedEntries = computed(() => {
  const groups = new Map()
  filteredEntries.value.forEach((entry) => {
    const key = String(entry.groupId || 'ungrouped')
    if (!groups.has(key)) {
      groups.set(key, { id: key, name: entry.groupName || '未分组', entries: [] })
    }
    groups.get(key).entries.push(entry)
  })
  return Array.from(groups.values()).map((group) => ({
    ...group,
    entries: sortEntries(group.entries),
  }))
})

const championVisible = computed(() => {
  if (groupFilter.value !== 'all') return false
  if (awardFilter.value !== 'all') return false
  const query = keyword.value.trim().toLowerCase()
  if (!query) return true
  return [champion.value?.beerName, champion.value?.breweryName, champion.value?.style, champion.value?.groupName]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
    .includes(query)
})

onMounted(async () => {
  try {
    competition.value = await fetchPortalCompetitionResultDetail(route.params.id)
  } finally {
    loading.value = false
  }
})

function sortEntries(list) {
  return [...list].sort((left, right) => {
    const leftOrder = awardOrder(left)
    const rightOrder = awardOrder(right)
    if (leftOrder !== rightOrder) return leftOrder - rightOrder
    return Number(left.rankNo || 99) - Number(right.rankNo || 99)
  })
}

function awardOrder(entry) {
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
.result-detail-page {
  display: grid;
  gap: 24px;
  max-width: 900px;
  margin: 0 auto;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  justify-self: start;
  color: #746a5f;
  font-weight: 800;
  text-decoration: none;
}

.back-link svg,
.champion-icon svg,
.champion-inner em svg,
.search-box svg,
.award-badge svg {
  width: 16px;
  height: 16px;
}

.summary-card,
.champion-card,
.group-card {
  overflow: hidden;
  background: rgba(255, 250, 240, 0.92);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(67, 43, 17, 0.08);
}

.summary-card {
  padding: 26px;
}

.summary-card h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.18;
  letter-spacing: 0;
}

.summary-card dl {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 26px;
  margin: 24px 0 0;
}

.summary-card dt {
  color: #746a5f;
  font-size: 13px;
}

.summary-card dd {
  margin: 6px 0 0;
  font-size: 15px;
  font-weight: 900;
}

.champion-card {
  position: relative;
  border-color: rgba(216, 144, 33, 0.46);
  border-width: 2px;
  box-shadow: 0 18px 40px rgba(67, 43, 17, 0.08), 0 1px 0 rgba(216, 144, 33, 0.18);
}

.champion-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(216, 144, 33, 0.16), rgba(255, 246, 223, 0.72) 52%, transparent);
  pointer-events: none;
}

.gold-line {
  position: relative;
  height: 4px;
  background: linear-gradient(90deg, #d89021, rgba(216, 144, 33, 0.62), rgba(216, 144, 33, 0.18));
}

.champion-inner {
  position: relative;
  display: flex;
  gap: 18px;
  padding: 28px;
}

.champion-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 48px;
  height: 48px;
  color: #8f5100;
  background: #ffe3a6;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 50%;
}

.champion-icon svg {
  width: 24px;
  height: 24px;
}

.champion-inner p {
  margin: 2px 0 6px;
  color: #a46622;
  font-size: 13px;
  font-weight: 900;
}

.champion-inner h2 {
  margin: 0;
  font-size: 25px;
  line-height: 1.2;
}

.champion-inner strong {
  display: block;
  margin-top: 10px;
  color: #746a5f;
  font-size: 15px;
}

.champion-inner em {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  margin-top: 12px;
  padding: 0 10px;
  color: #6b5c48;
  background: rgba(232, 222, 204, 0.9);
  border-radius: 999px;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.award-board {
  display: grid;
  gap: 20px;
}

.award-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.award-toolbar h2 {
  margin: 0;
  font-size: 20px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box {
  position: relative;
  display: block;
  width: 226px;
}

.search-box svg {
  position: absolute;
  left: 12px;
  top: 50%;
  color: #80766a;
  transform: translateY(-50%);
}

.search-box input,
.filters select {
  height: 36px;
  color: #241a10;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  outline: none;
}

.search-box input {
  width: 100%;
  padding: 0 12px 0 36px;
}

.filters select {
  min-width: 128px;
  padding: 0 34px 0 12px;
}

.search-box input:focus,
.filters select:focus {
  border-color: rgba(184, 117, 23, 0.45);
  box-shadow: 0 0 0 3px rgba(184, 117, 23, 0.12);
}

.group-list {
  display: grid;
  gap: 18px;
}

.group-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(245, 238, 222, 0.68);
  border-bottom: 1px solid rgba(87, 58, 26, 0.12);
}

.group-card h3 {
  margin: 0;
  font-size: 15px;
}

.group-card header span {
  color: #746a5f;
  font-size: 13px;
}

.group-card ul {
  padding: 0;
  margin: 0;
  list-style: none;
}

.group-card li {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  padding: 18px 20px;
}

.group-card li + li {
  border-top: 1px solid rgba(87, 58, 26, 0.12);
}

.group-card li > div {
  min-width: 0;
}

.group-card li strong,
.group-card li p {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-card li strong {
  font-size: 15px;
}

.group-card li p {
  margin: 5px 0 0;
  color: #746a5f;
  font-size: 13px;
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

@media (max-width: 820px) {
  .summary-card dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .award-toolbar,
  .filters {
    display: grid;
  }

  .search-box,
  .filters select {
    width: 100%;
  }
}

@media (max-width: 560px) {
  .summary-card h1 {
    font-size: 26px;
  }

  .summary-card dl {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .champion-inner {
    display: grid;
    padding: 24px;
  }
}
</style>
