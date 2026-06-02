<template>
  <div class="entries-page">
    <section class="toolbar brewer-card">
      <div>
        <h2 class="portal-section-title">我的酒款</h2>
        <p>按酒款查看报名记录，并处理付款、现场标签、酒样入库和结果反馈。</p>
      </div>
      <div class="toolbar-actions">
        <el-input v-model="keyword" placeholder="搜索酒名 / 参赛编号 / 现场短编号" clearable />
        <el-select v-model="statusFilter" placeholder="状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </section>

    <section class="entry-grid">
      <article
        v-for="entry in filteredEntries"
        :key="entry.id"
        class="entry-card"
        @click="selectEntry(entry)"
      >
        <div class="label-top">
          <span :class="['label-chip', `tone-${entryStatusMeta[entry.status].tone}`]">
            {{ entryStatusMeta[entry.status].label }}
          </span>
          <span class="abv">{{ entry.abv }}</span>
        </div>
        <div class="competition-line">{{ competitionName(entry.competitionId) }}</div>
        <h3>{{ entry.name }}</h3>
        <p>{{ entry.categoryName }} · {{ entry.style }}</p>
        <div class="meta-grid">
          <span>
            <small>IBU</small>
            <strong>{{ entry.ibu }}</strong>
          </span>
          <span>
            <small>报名费</small>
            <strong>¥{{ entry.fee }}</strong>
          </span>
          <span>
            <small>支付</small>
            <strong>{{ entry.paymentStatus === 'PAID' ? '已付款' : '待付款' }}</strong>
          </span>
        </div>
        <div class="uuid-band">
          <span>参赛编号 {{ entry.uuid }}</span>
          <el-button size="small" text @click.stop="selectEntry(entry)">详情</el-button>
        </div>
        <div class="card-actions" @click.stop>
          <RouterLink :to="primaryAction(entry).to">{{ primaryAction(entry).label }}</RouterLink>
          <RouterLink :to="`/portal/events/${entry.competitionId}`">赛事详情</RouterLink>
        </div>
      </article>
    </section>

    <el-drawer v-model="drawerVisible" size="520px" class="entry-drawer" :with-header="false">
      <div v-if="selectedEntry" class="drawer-body">
        <div class="drawer-label">
          <span :class="['label-chip', `tone-${entryStatusMeta[selectedEntry.status].tone}`]">
            {{ entryStatusMeta[selectedEntry.status].label }}
          </span>
          <h2>{{ selectedEntry.name }}</h2>
          <p>参赛编号 {{ selectedEntry.uuid }} · 现场短编号 {{ selectedEntry.shortCode }}</p>
        </div>

        <section class="drawer-section">
          <h3>关联赛事</h3>
          <div class="linked-event">
            <strong>{{ competitionName(selectedEntry.competitionId) }}</strong>
            <RouterLink :to="`/portal/events/${selectedEntry.competitionId}`">进入赛事详情</RouterLink>
          </div>
        </section>

        <section class="drawer-section">
          <h3>报名信息</h3>
          <dl>
            <div><dt>投递组别</dt><dd>{{ selectedEntry.categoryName }}</dd></div>
            <div><dt>基础风格</dt><dd>{{ selectedEntry.style }}</dd></div>
            <div><dt>ABV / IBU</dt><dd>{{ selectedEntry.abv }} / {{ selectedEntry.ibu }}</dd></div>
            <div><dt>现场短编号</dt><dd>{{ selectedEntry.shortCode }}</dd></div>
            <div><dt>提交时间</dt><dd>{{ selectedEntry.submittedAt }}</dd></div>
            <div><dt>付款时间</dt><dd>{{ selectedEntry.paidAt || '-' }}</dd></div>
            <div><dt>入库时间</dt><dd>{{ selectedEntry.storedAt || '-' }}</dd></div>
          </dl>
        </section>

        <section class="drawer-section">
          <h3>评审可见信息预览</h3>
          <div class="anonymous-preview">
            <strong>参赛编号 {{ selectedEntry.uuid }}</strong>
            <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }} · {{ selectedEntry.abv }}</p>
            <p>{{ selectedEntry.description }}</p>
            <ul>
              <li v-for="field in selectedEntry.extraFields" :key="field.label">
                <span>{{ field.label }}</span>
                <b>{{ field.value }}</b>
              </li>
            </ul>
          </div>
          <p class="privacy-note">评审扫码时隐藏酒名、厂牌和联系人信息。</p>
        </section>

        <section class="drawer-section">
          <h3>状态时间线</h3>
          <div class="timeline">
            <div v-for="item in timeline(selectedEntry)" :key="item.label" :class="{ done: item.done }">
              <span />
              <div>
                <strong>{{ item.label }}</strong>
                <p>{{ item.time || item.hint }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="drawer-section">
          <h3>下一步操作</h3>
          <div class="drawer-actions">
            <RouterLink :to="primaryAction(selectedEntry).to">{{ primaryAction(selectedEntry).label }}</RouterLink>
            <RouterLink to="/portal/payment">付款与标签</RouterLink>
            <RouterLink to="/portal/results">结果反馈</RouterLink>
          </div>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { competitions, entries } from './mockData'
import { entryPrimaryAction, entryStatusMeta } from './portalViewModels'

const keyword = ref('')
const statusFilter = ref('')
const drawerVisible = ref(false)
const selectedEntry = ref(null)

const statusOptions = Object.entries(entryStatusMeta).map(([value, meta]) => ({ value, label: meta.label }))

const filteredEntries = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return entries.filter((entry) => {
    const hitStatus = !statusFilter.value || entry.status === statusFilter.value
    const hitWord = !word || `${entry.name} ${entry.uuid} ${entry.shortCode}`.toLowerCase().includes(word)
    return hitStatus && hitWord
  })
})

function selectEntry(entry) {
  selectedEntry.value = entry
  drawerVisible.value = true
}

function competitionName(competitionId) {
  return competitions.find((competition) => competition.id === competitionId)?.name || '未关联赛事'
}

function primaryAction(entry) {
  return entryPrimaryAction(entry)
}

function timeline(entry) {
  return [
    { label: '提交资料', time: entry.submittedAt, done: true },
    { label: '付款确认', time: entry.paidAt, hint: '等待支付报名费', done: Boolean(entry.paidAt) },
            { label: '标签可下载', hint: entry.paidAt ? '已生成现场标签' : '付款确认后开放下载', done: Boolean(entry.paidAt) },
    { label: '酒样入库', time: entry.storedAt, hint: '等待主办方确认收样', done: Boolean(entry.storedAt) },
    { label: '结果发布', hint: entry.status === 'RESULT_PUBLISHED' ? '评分反馈可查看' : '等待比赛结束', done: entry.status === 'RESULT_PUBLISHED' },
  ]
}
</script>

<style scoped>
.entries-page {
  display: grid;
  gap: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 22px;
}

.toolbar p {
  margin: 0;
  color: #746a5f;
}

.toolbar-actions {
  display: grid;
  grid-template-columns: 260px 150px;
  gap: 12px;
  align-items: center;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.entry-card {
  display: flex;
  flex-direction: column;
  min-height: 290px;
  padding: 20px;
  cursor: pointer;
  color: #2b1d10;
  background:
    linear-gradient(180deg, #fffaf0 0%, #f4dca5 100%),
    repeating-linear-gradient(135deg, rgba(84, 52, 19, 0.05) 0 1px, transparent 1px 9px);
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 8px;
  box-shadow: 0 16px 34px rgba(83, 51, 17, 0.09);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.entry-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 42px rgba(83, 51, 17, 0.14);
}

.label-top,
.uuid-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.abv {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  color: #fff6df;
  background: #2b1d10;
  border-radius: 50%;
  font-weight: 900;
}

.entry-card h3 {
  min-height: 68px;
  margin: 24px 0 8px;
  font-size: 25px;
  line-height: 1.2;
}

.entry-card p {
  margin: 0;
  color: #746a5f;
}

.competition-line {
  margin-top: 16px;
  color: #8b5c19;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.4;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 22px 0;
}

.meta-grid span {
  padding: 10px;
  background: rgba(255, 250, 240, 0.52);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.meta-grid small,
.meta-grid strong {
  display: block;
}

.meta-grid small {
  color: #837565;
}

.meta-grid strong {
  margin-top: 5px;
}

.uuid-band {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px dashed rgba(87, 58, 26, 0.22);
  font-weight: 800;
}

.card-actions,
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.card-actions a,
.drawer-actions a,
.linked-event a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.card-actions a + a,
.drawer-actions a + a,
.linked-event a {
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
}

.drawer-body {
  min-height: 100%;
  padding: 28px;
  color: #2b1d10;
  background: #fff7e6;
}

.drawer-label {
  padding: 22px;
  color: #fff6df;
  background:
    linear-gradient(135deg, #2b1d10, #5a3414),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.07) 0 1px, transparent 1px 12px);
  border-radius: 8px;
}

.drawer-label h2 {
  margin: 18px 0 8px;
  font-size: 30px;
  line-height: 1.15;
}

.drawer-label p {
  margin: 0;
  color: #d8c9ac;
}

.drawer-section {
  margin-top: 22px;
}

.drawer-section h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

dl {
  display: grid;
  gap: 10px;
  margin: 0;
}

.linked-event {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.13);
  border-radius: 8px;
}

.linked-event strong {
  line-height: 1.4;
}

dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(87, 58, 26, 0.1);
}

dt {
  color: #746a5f;
}

dd {
  margin: 0;
  font-weight: 700;
  text-align: right;
}

.anonymous-preview {
  padding: 18px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.13);
  border-radius: 8px;
}

.anonymous-preview > strong {
  display: block;
  font-size: 20px;
}

.anonymous-preview ul {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 14px 0 0;
  list-style: none;
}

.anonymous-preview li {
  display: grid;
  gap: 4px;
}

.anonymous-preview li span,
.privacy-note,
.timeline p {
  color: #746a5f;
}

.anonymous-preview li b {
  line-height: 1.5;
}

.privacy-note {
  margin: 10px 0 0;
  font-size: 13px;
}

.timeline {
  display: grid;
  gap: 12px;
}

.timeline > div {
  display: flex;
  gap: 12px;
}

.timeline > div > span {
  width: 12px;
  height: 12px;
  margin-top: 5px;
  background: #d5c6ad;
  border-radius: 50%;
}

.timeline > div.done > span {
  background: #3d7d50;
}

.timeline strong,
.timeline p {
  display: block;
}

.timeline p {
  margin: 4px 0 0;
}

@media (max-width: 1180px) {
  .entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .toolbar,
  .toolbar-actions {
    grid-template-columns: 1fr;
  }

  .toolbar {
    display: grid;
  }

  .entry-grid {
    grid-template-columns: 1fr;
  }
}
</style>
