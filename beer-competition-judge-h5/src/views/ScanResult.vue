<template>
  <main class="app-shell">
    <section class="top-panel">
      <button class="back-link" type="button" @click="$router.push('/competitions')">返回扫码</button>
      <p class="eyebrow">{{ entry?.competitionName || '酒款信息' }}</p>
      <h1 class="page-title">{{ displayShortCode(entry) }}</h1>
      <div class="scan-status">
        <span :class="['pill', entry?.locked ? 'status-lock' : 'status-ok']">
          {{ entry?.locked ? '本桌已确认' : actionLabel }}
        </span>
        <span v-if="entry?.scored" class="pill status-ok">已提交</span>
      </div>
    </section>

    <section v-if="entry" class="card info-card">
      <dl class="context-grid">
        <div>
          <dt>当前轮次</dt>
          <dd>{{ entry.roundName }}</dd>
        </div>
        <div>
          <dt>评审桌</dt>
          <dd>{{ entry.tableName }}</dd>
        </div>
      </dl>

      <dl class="info-grid">
        <div>
          <dt>投递组别</dt>
          <dd>{{ entry.categoryName }}</dd>
        </div>
        <div>
          <dt>基础风格</dt>
          <dd>{{ styleDisplayName(entry) }}</dd>
        </div>
        <div>
          <dt>ABV</dt>
          <dd>{{ entry.abv }}</dd>
        </div>
      </dl>

      <div v-if="entry.styleCategoryName || entry.styleDescription" class="style-reference">
        <div>
          <span>风格分类</span>
          <strong>{{ entry.styleCategoryName || entry.categoryName }}</strong>
        </div>
        <p v-if="entry.styleDescription">{{ entry.styleDescription }}</p>
      </div>

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
      <p class="caption">请确认编号是否完整，或联系现场工作人员。</p>
    </section>

    <div v-if="entry" class="sticky-actions">
      <button
        v-if="entry.canScore"
        class="button primary full"
        type="button"
        :disabled="entry.locked"
        @click="$router.push(`/score/${entry.uuid}`)"
      >
        {{ entry.locked ? '本桌结果已确认' : entry.scored ? '修改我的专业评分' : scoreButtonLabel }}
      </button>
      <button
        v-if="entry.action === 'CAPTAIN'"
        class="button secondary full"
        type="button"
        @click="$router.push(`/captain/${entry.uuid}`)"
      >
        查看本桌评分
      </button>
      <button
        v-if="entry.action === 'RANKING'"
        class="button primary full"
        type="button"
        @click="$router.push(`/ranking/${entry.roundTableId}`)"
      >
        进入本轮排序
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
import { fetchEntry, fetchMe, resolveScanEntry } from '@/api/judge'

const route = useRoute()
const code = computed(() => String(route.params.code || route.params.uuid || '').toUpperCase())
const resolvingScan = computed(() => Boolean(route.params.code))
const entry = ref(null)
const me = ref(null)
const actionLabel = computed(() => {
  if (entry.value?.action === 'CAPTAIN') return '桌长汇总'
  if (entry.value?.action === 'RANKING') return '本轮排序'
  return entry.value?.scored ? '可修改评分' : '可评分'
})
const scoreButtonLabel = computed(() => (
  me.value?.role === 'CAPTAIN' ? '填写我的专业评分' : '开始评分'
))

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
    entry.value = resolvingScan.value ? await resolveScanEntry(code.value) : await fetchEntry(code.value)
  } catch {
    entry.value = null
  }
})

function styleDisplayName(source) {
  return [source?.styleCode, source?.style].filter(Boolean).join(' ')
}

function displayShortCode(source) {
  if (source?.shortCode) return `编号： ${source.shortCode}`
  return '正在读取'
}
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

.context-grid,
.info-grid {
  display: grid;
  gap: 8px;
  margin: 0;
}

.context-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 8px;
}

.info-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.context-grid div,
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

.style-reference {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
}

.style-reference div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.style-reference span {
  color: #667085;
  font-size: 13px;
}

.style-reference strong {
  color: #18222f;
  font-size: 14px;
}

.style-reference p {
  margin: 0;
  color: #344054;
  line-height: 1.55;
  font-size: 14px;
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
