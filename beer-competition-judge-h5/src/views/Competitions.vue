<template>
  <main class="app-shell">
    <section class="top-panel">
      <div class="split">
        <div>
          <p class="eyebrow">今日评审</p>
          <h1 class="page-title">{{ current?.name || '啤酒大赛' }}</h1>
        </div>
        <span class="role-badge">{{ me?.roleLabel }}</span>
      </div>
      <div class="meta-grid">
        <div>
          <span>评审桌</span>
          <strong>{{ me?.tableName || current?.tableName }}</strong>
        </div>
        <div>
          <span>当前轮次</span>
          <strong>{{ current?.flightName }}</strong>
        </div>
        <div>
          <span>我的进度</span>
          <strong>{{ current?.myScoredCount || 0 }} / {{ current?.totalEntries || 0 }}</strong>
        </div>
      </div>
    </section>

    <section v-if="!current" class="card action-card">
      <h2 class="section-title">暂未分配比赛</h2>
      <p class="caption">账号启用后，主办方会把你加入本场评审桌。</p>
      <button class="button secondary full empty-action" type="button" @click="router.push('/profile')">查看我的资料</button>
    </section>

    <template v-else>
    <section class="card action-card">
      <h2 class="section-title">开始评酒</h2>
      <button class="scan-button" type="button" @click="scannerOpen = !scannerOpen">
        扫码评酒
      </button>
      <div v-if="scannerOpen" class="scanner-box">
        <div class="scanner-frame">
          <span></span>
          <p>将二维码放入框内</p>
        </div>
        <div class="mock-list">
          <button
            v-for="entry in entries"
            :key="entry.uuid"
            type="button"
            @click="openEntry(entry.uuid)"
          >
            识别 {{ entry.uuid }}
          </button>
        </div>
      </div>

      <label class="field">
        输入酒款编号或短编号
        <div class="manual-row">
          <input v-model.trim="manualUuid" class="input" placeholder="例如 BC-2026-IPA-0001 或 IPA001" />
          <button class="button dark" type="button" @click="openEntry(manualUuid)">查看</button>
        </div>
      </label>
      <p class="caption">扫码异常时，可输入二维码标签上的短编号继续评分。</p>
    </section>

    <section class="card">
      <div class="split">
        <h2 class="section-title compact">本轮酒款</h2>
        <span class="pill">{{ entries.length }} 款</span>
      </div>
      <div class="entry-list">
        <button
          v-for="entry in entries"
          :key="entry.uuid"
          type="button"
          class="entry-row"
          @click="openEntry(entry.uuid)"
        >
          <span>
            <strong>{{ entry.uuid }}</strong>
            <small>{{ entry.shortCode }} · {{ entry.categoryName }} · {{ entry.style }}</small>
          </span>
          <em :class="['pill', entry.finalized ? 'status-lock' : entry.tableScoreCount ? 'status-warn' : '']">
            {{ entry.finalized ? '已确认' : entry.tableScoreCount ? `${entry.tableScoreCount} 人已评` : '待评分' }}
          </em>
        </button>
      </div>
    </section>
    </template>

    <nav class="bottom-nav" :style="{ gridTemplateColumns: `repeat(${navItems.length}, minmax(0, 1fr))` }">
      <router-link v-for="item in navItems" :key="item.to" class="nav-item" :to="item.to">
        {{ item.label }}
      </router-link>
    </nav>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCaptainBoard, fetchCompetitions, fetchMe } from '@/api/judge'

const router = useRouter()
const me = ref(null)
const current = ref(null)
const entries = ref([])
const scannerOpen = ref(false)
const manualUuid = ref('BC-2026-IPA-0001')

const navItems = computed(() => {
  const items = [
    { label: '扫码', to: '/competitions' },
    { label: '已评', to: '/judged' },
  ]
  if (me.value?.role === 'CAPTAIN') items.push({ label: '本桌', to: '/captain' })
  items.push({ label: '我的', to: '/profile' })
  return items
})

function openEntry(uuid) {
  if (!uuid) return
  router.push(`/scan-result/${uuid.toUpperCase()}`)
}

onMounted(async () => {
  me.value = await fetchMe()
  if (me.value?.profileRequired) {
    router.replace('/profile/edit')
    return
  }
  if (Number(me.value?.status) === 2) {
    router.replace('/review-status')
    return
  }
  const competitions = await fetchCompetitions()
  current.value = competitions[0]
  if (!current.value) {
    entries.value = []
    return
  }
  const board = await fetchCaptainBoard()
  entries.value = board.entries
})
</script>

<style scoped>
.role-badge {
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  padding: 7px 10px;
  color: #fff7ed;
  background: rgba(255, 255, 255, 0.1);
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 18px;
}

.meta-grid div {
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.08);
}

.meta-grid span,
.meta-grid strong {
  display: block;
}

.meta-grid span {
  color: rgba(248, 250, 252, 0.68);
  font-size: 12px;
}

.meta-grid strong {
  margin-top: 5px;
  color: #fff;
  font-size: 15px;
}

.action-card {
  margin-top: 12px;
}

.empty-action {
  margin-top: 14px;
}

.scan-button {
  width: 100%;
  min-height: 58px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: #a75517;
  font-size: 18px;
  font-weight: 850;
}

.scanner-box {
  margin-top: 12px;
  border-radius: 8px;
  padding: 12px;
  background: #121a24;
}

.scanner-frame {
  display: grid;
  place-items: center;
  min-height: 190px;
  border: 1px solid rgba(255, 255, 255, 0.38);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.72);
  background:
    linear-gradient(transparent 48%, rgba(167, 85, 23, 0.65) 50%, transparent 52%),
    rgba(255, 255, 255, 0.04);
}

.scanner-frame span {
  width: 86px;
  height: 86px;
  border: 2px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
}

.scanner-frame p {
  margin: 0;
  font-size: 13px;
}

.mock-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.mock-list button {
  min-height: 38px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.manual-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.compact {
  margin-bottom: 0;
}

.entry-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.entry-row {
  display: flex;
  gap: 10px;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  color: #18222f;
  background: #fff;
  text-align: left;
}

.entry-row strong,
.entry-row small {
  display: block;
}

.entry-row small {
  margin-top: 5px;
  color: #667085;
  line-height: 1.35;
}

.entry-row em {
  flex: 0 0 auto;
  font-style: normal;
}
</style>
