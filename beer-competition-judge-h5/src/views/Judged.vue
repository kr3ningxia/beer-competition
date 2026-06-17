<template>
  <main class="app-shell">
    <section class="top-panel">
      <p class="eyebrow">我的评分</p>
      <h1 class="page-title">我的评分记录</h1>
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
            <span :class="['pill', score.locked ? 'status-lock' : 'status-ok']">
              {{ score.locked ? '已锁定' : '已评分' }}
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
            {{ score.locked ? '桌长已确认' : '查看/修改' }}
          </button>
        </article>
      </div>

      <div v-else class="empty">
        <h2 class="section-title">还没有提交评分</h2>
        <p class="caption">扫码或输入编号后，开始本轮评分。</p>
        <button class="button primary full" type="button" @click="$router.push('/competitions')">返回扫码</button>
      </div>
    </section>

    <JudgeBottomNav :role="me?.role" :hide-table="hideTableNav" active="profile" />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchCompetitions, fetchMe, fetchMyScores } from '@/api/judge'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'
import { isRankingTaskType, selectCurrentTask } from '@/utils/judgeTasks'

const me = ref(null)
const scores = ref([])
const currentTask = ref(null)
const hideTableNav = computed(() => isRankingTaskType(currentTask.value?.taskType))

onMounted(async () => {
  const [profile, tasks, myScores] = await Promise.all([fetchMe(), fetchCompetitions().catch(() => []), fetchMyScores()])
  me.value = profile
  currentTask.value = selectCurrentTask(tasks)
  scores.value = myScores
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
