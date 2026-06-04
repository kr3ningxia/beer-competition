<template>
  <main class="app-shell">
    <section class="top-panel">
      <p class="eyebrow">我的评分</p>
      <h1 class="page-title">已评酒款</h1>
      <div class="summary-line">
        <span>{{ me?.roleLabel }}</span>
        <strong>{{ scores.length }} 款</strong>
      </div>
    </section>

    <section class="card">
      <div v-if="scores.length" class="score-list">
        <article v-for="score in scores" :key="score.id" class="score-row">
          <div class="split">
            <div>
              <h2>{{ score.shortCode ? `编号： ${score.shortCode}` : '编号' }}</h2>
              <p>{{ score.categoryName }} · {{ score.style }}</p>
            </div>
            <span :class="['pill', score.locked ? 'status-lock' : 'status-warn']">
              {{ score.locked ? '已锁定' : '可修改' }}
            </span>
          </div>
          <div class="score-meta">
            <span>总分 <strong>{{ score.totalScore }}</strong></span>
            <span>提交 {{ score.submittedAt }}</span>
          </div>
          <p class="comment">{{ score.comments }}</p>
          <button
            class="button secondary full"
            type="button"
            :disabled="score.locked"
            @click="$router.push(`/score/${score.beerUuid}`)"
          >
            {{ score.locked ? '桌长已确认' : '查看并修改' }}
          </button>
        </article>
      </div>

      <div v-else class="empty">
        <h2 class="section-title">还没有提交评分</h2>
        <p class="caption">扫码或输入编号后，就可以开始本轮评分。</p>
        <button class="button primary full" type="button" @click="$router.push('/competitions')">去扫码</button>
      </div>
    </section>

    <nav class="bottom-nav" :style="{ gridTemplateColumns: `repeat(${navItems.length}, minmax(0, 1fr))` }">
      <router-link v-for="item in navItems" :key="item.to" class="nav-item" :to="item.to">
        {{ item.label }}
      </router-link>
    </nav>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchMe, fetchMyScores } from '@/api/judge'

const me = ref(null)
const scores = ref([])

const navItems = computed(() => {
  const items = [
    { label: '扫码', to: '/competitions' },
    { label: '已评', to: '/judged' },
  ]
  if (me.value?.role === 'CAPTAIN') items.push({ label: '本桌', to: '/captain' })
  items.push({ label: '我的', to: '/profile' })
  return items
})

onMounted(async () => {
  me.value = await fetchMe()
  scores.value = await fetchMyScores()
})
</script>

<style scoped>
.summary-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  border-radius: 8px;
  padding: 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.score-list {
  display: grid;
  gap: 12px;
}

.score-row {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.score-row h2 {
  margin: 0;
  color: #18222f;
  font-size: 17px;
}

.score-row p {
  margin: 5px 0 0;
}

.score-row .split {
  align-items: flex-start;
}

.score-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  color: #667085;
  font-size: 14px;
}

.score-meta strong {
  color: #18222f;
  font-size: 20px;
}

.comment {
  display: -webkit-box;
  overflow: hidden;
  color: #344054;
  line-height: 1.55;
  white-space: pre-line;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.empty {
  display: grid;
  gap: 12px;
}
</style>
