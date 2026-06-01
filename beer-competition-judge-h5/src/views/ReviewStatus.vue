<template>
  <main class="app-shell">
    <section class="top-panel">
      <p class="eyebrow">资料状态</p>
      <h1 class="page-title">{{ title }}</h1>
      <div class="status-line">
        <span>{{ me?.statusLabel || '-' }}</span>
      </div>
    </section>

    <section class="card">
      <div class="profile-list">
        <div>
          <span>姓名</span>
          <strong>{{ profile?.name || '-' }}</strong>
        </div>
        <div>
          <span>手机号</span>
          <strong>{{ profile?.phone || '-' }}</strong>
        </div>
        <div>
          <span>微信号</span>
          <strong>{{ profile?.wechat || '-' }}</strong>
        </div>
        <div>
          <span>资质信息</span>
          <strong>{{ profile?.qualification || '-' }}</strong>
        </div>
      </div>
      <p class="caption">{{ caption }}</p>
    </section>

    <section class="card stack">
      <button class="button secondary full" type="button" @click="router.push('/profile/edit')">修改资料</button>
      <button class="button danger full" type="button" @click="logout">退出登录</button>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMe, fetchProfile } from '@/api/judge'
import { clearSession } from '@/utils/auth'

const router = useRouter()
const me = ref(null)
const profile = ref(null)

const title = computed(() => {
  if (me.value?.profileRequired) return '请先完善资料'
  if (Number(me.value?.status) === 2) return '等待主办方确认'
  return '暂未分配比赛'
})

const caption = computed(() => {
  if (me.value?.profileRequired) return '填写完整资料后，主办方才能在评审池中看到你。'
  if (Number(me.value?.status) === 2) return '资料已进入评审池，审核通过后会被加入比赛编排。'
  return '账号已启用，请联系现场工作人员加入本场比赛。'
})

onMounted(async () => {
  me.value = await fetchMe()
  profile.value = await fetchProfile()
  if (me.value?.canScore) {
    router.replace('/competitions')
  }
})

function logout() {
  clearSession()
  localStorage.removeItem('judge_mock_user')
  router.push('/login')
}
</script>

<style scoped>
.status-line {
  display: inline-flex;
  margin-top: 14px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  padding: 7px 11px;
  color: #fff7ed;
  background: rgba(255, 255, 255, 0.1);
  font-weight: 800;
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
