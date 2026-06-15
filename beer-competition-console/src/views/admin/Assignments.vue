<template>
  <div class="assignment-entry-page">
    <section class="page-head">
      <div>
        <small>评审编排入口</small>
        <h1>选择比赛后编排评审</h1>
        <p>进入比赛后，把评审和酒款安排到对应桌。</p>
      </div>
      <button class="tool-button" type="button" @click="loadCompetitions">
        <Refresh />
        刷新比赛
      </button>
    </section>

    <section class="toolbar">
      <label class="search-box">
        <Search />
        <input v-model.trim="keyword" placeholder="搜索比赛名称、编号" />
      </label>
      <div class="filter-tabs" aria-label="比赛状态筛选">
        <button
          v-for="item in statusFilters"
          :key="item.value"
          :class="{ active: statusFilter === item.value }"
          type="button"
          @click="statusFilter = item.value"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <section class="competition-grid">
      <article v-for="competition in filteredCompetitions" :key="competition.id" class="competition-card">
        <div>
          <span :class="['state-badge', statusMeta[competition.status]?.tone]">
            {{ statusMeta[competition.status]?.label || competition.status }}
          </span>
          <h2>{{ competition.name }}</h2>
          <p>{{ competition.code || '未设置编号' }}</p>
        </div>
        <dl>
          <div>
            <dt>比赛日期</dt>
            <dd>{{ formatDate(competition.competitionDate) }}</dd>
          </div>
          <div>
            <dt>评审桌</dt>
            <dd>{{ competition.judgeTableCount || 0 }} 桌</dd>
          </div>
          <div>
            <dt>已分配评审</dt>
            <dd>{{ competition.judgeCount || 0 }} 人</dd>
          </div>
        </dl>
        <button class="tool-button primary" type="button" @click="openWorkbench(competition.id)">
          <Connection />
          进入分桌分配
        </button>
      </article>
    </section>

    <section v-if="!loading && filteredCompetitions.length === 0" class="empty-state">
      <h2>没有匹配的比赛</h2>
      <p>调整搜索条件或状态筛选后再进入评审编排。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Connection, Refresh, Search } from '@element-plus/icons-vue'
import { fetchCompetitions } from '@/api/admin'
import { formatDate, statusMeta } from './competitionStore'

const router = useRouter()
const competitions = ref([])
const keyword = ref('')
const statusFilter = ref('ALL')
const loading = ref(false)

const statusFilters = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '报名中', value: 'REGISTRATION_OPEN' },
  { label: '评审中', value: 'JUDGING' },
]

const filteredCompetitions = computed(() => {
  const query = keyword.value.toLowerCase()
  return competitions.value.filter((competition) => {
    const matchesStatus = statusFilter.value === 'ALL' || competition.status === statusFilter.value
    const matchesKeyword =
      !query ||
      [competition.name, competition.code]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query))
    return matchesStatus && matchesKeyword
  })
})

onMounted(loadCompetitions)

async function loadCompetitions() {
  loading.value = true
  try {
    competitions.value = await fetchCompetitions({ includeArchived: false })
  } finally {
    loading.value = false
  }
}

function openWorkbench(id) {
  router.push({ path: `/admin/competitions/${id}`, query: { tab: 'judges' } })
}
</script>

<style scoped>
.assignment-entry-page {
  --panel: rgba(22, 32, 36, 0.9);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  min-height: 100vh;
  padding: 28px;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 17% 7%, rgba(216, 169, 53, 0.13), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
}

h1,
h2,
p,
dl,
dd {
  margin: 0;
}

button,
input {
  font: inherit;
}

button {
  cursor: pointer;
}

svg {
  width: 1em;
  height: 1em;
}

.page-head,
.toolbar,
.search-box,
.filter-tabs,
.tool-button {
  display: flex;
  align-items: center;
}

.page-head {
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--line);
}

.page-head small {
  color: var(--gold-soft);
  font-size: 12px;
  font-weight: 800;
}

.page-head h1 {
  margin-top: 7px;
  font-size: 30px;
  line-height: 1.1;
}

.page-head p,
.competition-card p,
dt,
.empty-state p {
  color: var(--muted);
}

.tool-button,
.filter-tabs button {
  min-height: 42px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.tool-button {
  justify-content: center;
  gap: 8px;
  padding: 0 12px;
}

.tool-button.primary {
  width: 100%;
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.toolbar {
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.search-box {
  flex: 1;
  max-width: 520px;
  gap: 10px;
  min-height: 46px;
  padding: 0 12px;
  color: var(--muted);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
}

.search-box input {
  width: 100%;
  min-width: 0;
  color: var(--text);
  border: 0;
  outline: 0;
  background: transparent;
}

.filter-tabs {
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tabs button {
  padding: 0 14px;
  color: #a9bbc2;
}

.filter-tabs button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.competition-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.competition-card {
  display: grid;
  gap: 18px;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.competition-card h2 {
  margin-top: 12px;
  font-size: 20px;
  line-height: 1.22;
}

.competition-card p {
  margin-top: 7px;
}

.competition-card dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.competition-card dl div {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

dt {
  font-size: 12px;
}

dd {
  margin-top: 6px;
  overflow: hidden;
  color: var(--text);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-badge {
  display: inline-flex;
  width: fit-content;
  padding: 7px 10px;
  color: var(--green);
  font-weight: 800;
  border: 1px solid rgba(111, 207, 122, 0.2);
  border-radius: 8px;
  background: rgba(111, 207, 122, 0.1);
}

.state-badge.gold,
.state-badge.warning {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.08);
}

.state-badge.neutral {
  color: #a9bbc2;
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.03);
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 240px;
  margin-top: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  text-align: center;
}

@media (max-width: 1260px) {
  .competition-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .assignment-entry-page {
    padding: 22px 16px;
  }

  .page-head,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    max-width: none;
  }

  .competition-grid,
  .competition-card dl {
    grid-template-columns: 1fr;
  }
}
</style>
