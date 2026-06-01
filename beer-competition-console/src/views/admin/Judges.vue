<template>
  <div class="judges-page">
    <section class="page-head">
      <div>
        <small>评审账号管理</small>
        <h1>评审列表</h1>
        <p>维护赛前可用评审名单，分桌与角色在评审分配中处理。</p>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="loadJudges">
          <Refresh />
          刷新名单
        </button>
        <button class="tool-button primary" type="button" @click="router.push('/admin/assignments')">
          <Connection />
          评审分配
        </button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <small>评审账号</small>
        <strong>{{ totalCount }}</strong>
        <p>名单总数</p>
      </article>
      <article class="metric-card">
        <small>可参与评审</small>
        <strong>{{ activeCount }}</strong>
        <p>启用账号</p>
      </article>
      <article class="metric-card">
        <small>停用账号</small>
        <strong>{{ inactiveCount }}</strong>
        <p>暂不参与分配</p>
      </article>
      <article class="metric-card">
        <small>资质已填</small>
        <strong>{{ qualifiedCount }}</strong>
        <p>便于赛前分组</p>
      </article>
    </section>

    <section class="toolbar">
      <label class="search-box">
        <Search />
        <input v-model.trim="keyword" placeholder="搜索姓名、手机号、微信号、资质" />
      </label>
      <div class="filter-tabs" aria-label="评审状态筛选">
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

    <section class="table-card">
      <div class="table-headline">
        <div>
          <h2>账号明细</h2>
          <span>{{ filteredJudges.length }} / {{ totalCount }}</span>
        </div>
        <strong v-if="loading">加载中</strong>
        <strong v-else-if="keyword || statusFilter !== 'ALL'">已筛选</strong>
      </div>

      <div class="judge-table">
        <div class="table-head">
          <span>评审</span>
          <span>联系方式</span>
          <span>资质</span>
          <span>状态</span>
          <span>操作</span>
        </div>
        <div v-for="judge in filteredJudges" :key="judge.id" class="table-row">
          <div class="judge-cell">
            <span class="avatar">{{ getInitial(judge.name) }}</span>
            <div>
              <strong>{{ judge.name || '未填写姓名' }}</strong>
              <small>ID {{ judge.id }}</small>
            </div>
          </div>
          <div class="contact-cell">
            <span>{{ judge.phone || '-' }}</span>
            <small>{{ judge.wechat || '未填写微信号' }}</small>
          </div>
          <span class="qualification">{{ judge.qualification || '未填写资质' }}</span>
          <span :class="['status-badge', isActive(judge) ? 'active' : 'inactive']">
            {{ isActive(judge) ? '启用' : '停用' }}
          </span>
          <button class="row-action" type="button" @click="router.push('/admin/assignments')">
            分配
            <Right />
          </button>
        </div>
      </div>

      <div v-if="!loading && filteredJudges.length === 0" class="empty-state">
        <h2>没有匹配的评审</h2>
        <p>调整搜索条件或状态筛选后再查看。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Connection, Refresh, Right, Search } from '@element-plus/icons-vue'
import { fetchJudges } from '@/api/admin'

const router = useRouter()
const judges = ref([])
const keyword = ref('')
const statusFilter = ref('ALL')
const loading = ref(false)

const statusFilters = [
  { label: '全部', value: 'ALL' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
]

const totalCount = computed(() => judges.value.length)
const activeCount = computed(() => judges.value.filter(isActive).length)
const inactiveCount = computed(() => totalCount.value - activeCount.value)
const qualifiedCount = computed(() => judges.value.filter((judge) => Boolean(judge.qualification?.trim())).length)
const filteredJudges = computed(() => {
  const query = keyword.value.toLowerCase()
  return judges.value.filter((judge) => {
    const matchesStatus =
      statusFilter.value === 'ALL' ||
      (statusFilter.value === 'ACTIVE' && isActive(judge)) ||
      (statusFilter.value === 'INACTIVE' && !isActive(judge))
    const matchesKeyword =
      !query ||
      [judge.name, judge.phone, judge.wechat, judge.qualification]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(query))
    return matchesStatus && matchesKeyword
  })
})

onMounted(loadJudges)

async function loadJudges() {
  loading.value = true
  try {
    judges.value = await fetchJudges()
  } finally {
    loading.value = false
  }
}

function isActive(judge) {
  return Number(judge.status) === 1
}

function getInitial(name) {
  return name?.trim()?.slice(0, 1) || '评'
}
</script>

<style scoped>
.judges-page {
  --panel: rgba(22, 32, 36, 0.9);
  --panel-strong: rgba(26, 39, 44, 0.96);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --orange: #f2994a;
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
p {
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
.head-actions,
.tool-button,
.toolbar,
.search-box,
.filter-tabs,
.table-headline > div,
.judge-cell,
.row-action {
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
  letter-spacing: 0;
}

.page-head h1 {
  margin-top: 7px;
  font-size: 30px;
  line-height: 1.1;
}

.page-head p {
  margin-top: 10px;
  color: var(--muted);
}

.head-actions {
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.tool-button,
.row-action,
.filter-tabs button {
  min-height: 42px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.tool-button {
  gap: 8px;
  padding: 0 12px;
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.metric-card,
.toolbar,
.table-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.16);
}

.metric-card {
  min-width: 0;
  padding: 16px;
}

.metric-card small,
.table-head,
.judge-cell small,
.contact-cell small,
.empty-state p,
.table-headline span {
  color: var(--muted);
}

.metric-card strong {
  display: block;
  margin: 8px 0 6px;
  color: var(--text);
  font-size: 25px;
}

.metric-card p {
  color: var(--faint);
}

.toolbar {
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
  padding: 14px 16px;
}

.search-box {
  flex: 1;
  max-width: 480px;
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

.search-box input::placeholder {
  color: var(--faint);
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

.table-card {
  margin-top: 18px;
  padding: 16px;
}

.table-headline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.table-headline > div {
  gap: 10px;
}

.table-headline strong {
  color: var(--gold-soft);
}

.judge-table {
  display: grid;
  gap: 8px;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) minmax(200px, 1fr) minmax(260px, 1.25fr) 96px 100px;
  gap: 12px;
  align-items: center;
}

.table-head {
  padding: 0 12px;
  font-size: 13px;
}

.table-row {
  min-width: 0;
  padding: 13px 12px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  transition: border-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
}

.table-row:hover {
  border-color: rgba(216, 169, 53, 0.2);
  background: rgba(255, 255, 255, 0.04);
  transform: translateY(-1px);
}

.judge-cell {
  gap: 12px;
  min-width: 0;
}

.judge-cell strong,
.contact-cell span,
.qualification {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.judge-cell small,
.contact-cell small {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 38px;
  height: 38px;
  color: var(--gold-soft);
  font-weight: 800;
  border: 1px solid rgba(216, 169, 53, 0.26);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.09);
}

.status-badge {
  display: inline-flex;
  justify-content: center;
  width: fit-content;
  min-width: 56px;
  padding: 7px 10px;
  font-weight: 800;
  border-radius: 8px;
}

.status-badge.active {
  color: var(--green);
  border: 1px solid rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.1);
}

.status-badge.inactive {
  color: #f1bd79;
  border: 1px solid rgba(242, 153, 74, 0.24);
  background: rgba(242, 153, 74, 0.09);
}

.row-action {
  justify-content: center;
  gap: 7px;
  padding: 0 10px;
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.24);
  background: rgba(216, 169, 53, 0.06);
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 220px;
  text-align: center;
}

@media (max-width: 1260px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .table-head,
  .table-row {
    grid-template-columns: minmax(220px, 1.1fr) minmax(190px, 1fr) minmax(180px, 1fr) 88px 96px;
  }
}

@media (max-width: 980px) {
  .judges-page {
    padding: 22px 16px;
  }

  .page-head,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .search-box {
    max-width: none;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr;
  }

  .row-action {
    width: fit-content;
  }
}
</style>
