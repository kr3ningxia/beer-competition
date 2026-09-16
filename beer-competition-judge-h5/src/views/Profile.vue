<template>
  <main class="app-shell">
    <section class="top-panel">
      <h1 class="page-title">评审信息</h1>
      <div class="profile-head">
        <div class="profile-avatar" aria-hidden="true">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" @error="avatarUrl = ''" />
          <span v-else>{{ avatarInitial }}</span>
        </div>
        <div class="profile-head-copy">
          <strong>{{ me?.displayName || '-' }}</strong>
          <span>{{ me?.roleLabel || me?.statusLabel }} · {{ me?.tableName || '未分配评审桌' }}</span>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="profile-list">
        <div>
          <span>手机号</span>
          <strong>{{ me?.phone || '-' }}</strong>
        </div>
        <div>
          <span>微信号</span>
          <strong>{{ me?.wechat || '-' }}</strong>
        </div>
        <div>
          <span>当前比赛</span>
          <strong>{{ me?.currentCompetition || '-' }}</strong>
        </div>
        <div>
          <span>资质信息</span>
          <strong>{{ me?.qualification || '-' }}</strong>
        </div>
      </div>
      <p class="caption">如信息有误，请联系现场工作人员。</p>
    </section>

    <section class="card stack">
      <button class="button secondary full" type="button" @click="$router.push('/judged')">我的评分记录</button>
      <button class="button primary full" type="button" @click="$router.push('/profile/edit')">修改资料</button>
      <button class="button danger full" type="button" @click="logout">退出登录</button>
    </section>

    <JudgeBottomNav :role="me?.role" :hide-table="hideTableNav" />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCompetitions, fetchJudgeAvatar, fetchMe, fetchProfile } from '@/api/judge'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'
import { clearSession } from '@/utils/auth'
import { isRankingTaskType, selectCurrentTask } from '@/utils/judgeTasks'

const router = useRouter()
const me = ref(null)
const currentTask = ref(null)
const avatarUrl = ref('')
const hideTableNav = computed(() => isRankingTaskType(currentTask.value?.taskType))
const avatarInitial = computed(() => String(me.value?.displayName || '评').trim().slice(0, 1) || '评')

function logout() {
  clearSession()
  localStorage.removeItem('judge_mock_user')
  router.push('/login')
}

onMounted(async () => {
  const [profile, tasks, detail] = await Promise.all([
    fetchMe(),
    fetchCompetitions().catch(() => []),
    fetchProfile().catch(() => null),
  ])
  me.value = profile
  currentTask.value = selectCurrentTask(tasks)
  if (detail?.avatarAssetId) {
    try {
      avatarUrl.value = URL.createObjectURL(await fetchJudgeAvatar())
    } catch {
      avatarUrl.value = ''
    }
  }
})

onBeforeUnmount(() => {
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value)
})
</script>

<style scoped>
.profile-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
}

.profile-avatar {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 56px;
  height: 56px;
  overflow: hidden;
  color: #fff4dc;
  background: #a75517;
  border: 2px solid rgba(255, 255, 255, 0.42);
  border-radius: 50%;
  font-size: 24px;
  font-weight: 850;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-head-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.profile-head strong {
  color: #fff;
  font-size: 22px;
}

.profile-head span {
  color: rgba(248, 250, 252, 0.72);
}

.profile-list {
  display: grid;
  gap: 0;
}

.profile-list div {
  display: grid;
  gap: 5px;
  border-bottom: 1px solid #eaecf0;
  padding: 13px 0;
}

.profile-list div:last-child {
  border-bottom: 0;
}

.profile-list span {
  color: #667085;
  font-size: 13px;
}

.profile-list strong {
  color: #18222f;
  line-height: 1.5;
}
</style>
