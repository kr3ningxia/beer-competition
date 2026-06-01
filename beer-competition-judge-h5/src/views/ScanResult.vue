<template>
  <main class="app-shell">
    <section class="top-panel">
      <button class="back-link" type="button" @click="$router.push('/competitions')">返回扫码</button>
      <p class="eyebrow">酒款信息</p>
      <h1 class="page-title">{{ entry?.uuid || uuid }}</h1>
      <div class="scan-status">
        <span :class="['pill', entry?.locked ? 'status-lock' : 'status-ok']">
          {{ entry?.locked ? '本桌已确认' : '可评分' }}
        </span>
        <span v-if="entry?.shortCode" class="pill status-warn">短编号 {{ entry.shortCode }}</span>
        <span v-if="entry?.advanced" class="pill status-ok">已晋级</span>
      </div>
    </section>

    <section v-if="entry" class="card info-card">
      <dl class="info-grid">
        <div>
          <dt>投递组别</dt>
          <dd>{{ entry.categoryName }}</dd>
        </div>
        <div>
          <dt>基础风格</dt>
          <dd>{{ entry.style }}</dd>
        </div>
        <div>
          <dt>ABV</dt>
          <dd>{{ entry.abv }}</dd>
        </div>
      </dl>

      <div class="description">
        <h2 class="section-title">酒款简介</h2>
        <p>{{ entry.description }}</p>
      </div>

      <div v-if="entry.extraFields?.length" class="extra-fields">
        <h2 class="section-title">补充信息</h2>
        <div v-for="field in entry.extraFields" :key="field.key" class="extra-row">
          <span>{{ field.label }}</span>
          <strong>{{ field.value }}</strong>
        </div>
      </div>
    </section>

    <section v-else class="card">
      <h2 class="section-title">没有找到这款酒</h2>
      <p class="caption">请确认酒款编号是否完整，或联系现场工作人员。</p>
    </section>

    <div v-if="entry" class="sticky-actions">
      <button
        v-if="me?.role !== 'CAPTAIN'"
        class="button primary full"
        type="button"
        :disabled="entry.locked"
        @click="$router.push(`/score/${entry.uuid}`)"
      >
        {{ entry.locked ? '本桌结果已确认' : '开始评分' }}
      </button>
      <button
        v-else
        class="button primary full"
        type="button"
        @click="$router.push(`/captain/${entry.uuid}`)"
      >
        查看本桌评分
      </button>
      <button class="button secondary full" type="button" @click="$router.push('/competitions')">
        继续扫码
      </button>
    </div>

    <nav class="bottom-nav" :style="{ gridTemplateColumns: `repeat(${navItems.length}, minmax(0, 1fr))` }">
      <router-link v-for="item in navItems" :key="item.to" class="nav-item" :to="item.to">
        {{ item.label }}
      </router-link>
    </nav>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchEntry, fetchMe } from '@/api/judge'

const route = useRoute()
const uuid = String(route.params.uuid || '').toUpperCase()
const entry = ref(null)
const me = ref(null)

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
  try {
    entry.value = await fetchEntry(uuid)
  } catch {
    entry.value = null
  }
})
</script>

<style scoped>
.back-link {
  border: 0;
  margin: 0 0 14px;
  padding: 0;
  color: rgba(255, 255, 255, 0.74);
  background: transparent;
  font-weight: 750;
}

.scan-status {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.info-card {
  margin-top: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.info-grid div {
  border-radius: 8px;
  padding: 10px;
  background: #f7f8f6;
}

dt {
  margin: 0;
  color: #667085;
  font-size: 12px;
}

dd {
  margin: 5px 0 0;
  color: #18222f;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.description,
.extra-fields {
  margin-top: 18px;
}

.description p {
  margin: 0;
  color: #344054;
  line-height: 1.7;
}

.extra-row {
  display: grid;
  gap: 5px;
  border-top: 1px solid #eaecf0;
  padding: 12px 0;
}

.extra-row span {
  color: #667085;
  font-size: 13px;
}

.extra-row strong {
  color: #18222f;
  line-height: 1.45;
}

.sticky-actions {
  position: sticky;
  bottom: 72px;
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 10px;
  border: 1px solid rgba(24, 34, 47, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 30px rgba(24, 34, 47, 0.12);
}
</style>
