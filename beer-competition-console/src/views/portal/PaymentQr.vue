<template>
  <div class="payment-page">
    <section class="payment-list brewer-card">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">付款与现场标签</h2>
          <p>本版本采用线下付款和主办方确认。付款确认后，可下载包含参赛编号和现场短编号的标签。</p>
        </div>
        <RouterLink to="/portal/my">返回我的参赛</RouterLink>
      </div>

      <div class="payment-guide">
        <strong>线下付款说明</strong>
        <p>请按所选赛事报名费完成转账或现场付款，并备注厂牌名称与酒款名称。主办方确认后，酒款状态会更新为报名成功。</p>
      </div>

      <div class="pay-rows">
        <article
          v-for="entry in payableEntries"
          :key="entry.id"
          :class="['pay-row', { active: selectedEntry.id === entry.id }]"
          @click="selectedEntry = entry"
        >
          <span :class="['label-chip', entry.paymentStatus === 'PAID' ? 'tone-green' : 'tone-amber']">
            {{ entry.paymentStatus === 'PAID' ? '已确认付款' : '等待付款确认' }}
          </span>
          <div>
            <strong>{{ entry.name }}</strong>
            <p>{{ competitionName(entry.competitionId) }} · {{ entry.categoryName }}</p>
          </div>
          <b>¥{{ entry.fee }}</b>
        </article>
      </div>
    </section>

    <aside class="label-panel">
      <section class="qr-label">
        <span>现场评审标签</span>
        <h2>{{ selectedEntry.uuid }}</h2>
        <div class="fake-qr" aria-label="二维码示意">
          <i v-for="n in 49" :key="n" :class="{ dark: qrPattern(n) }" />
        </div>
        <strong>现场短编号 {{ selectedEntry.shortCode }}</strong>
        <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }} · {{ selectedEntry.abv }}</p>
      </section>

      <section class="action-card brewer-card">
        <span :class="['label-chip', selectedEntry.paymentStatus === 'PAID' ? 'tone-green' : 'tone-amber']">
          {{ selectedEntry.paymentStatus === 'PAID' ? '标签可下载' : '等待付款确认' }}
        </span>
        <h3>{{ selectedEntry.name }}</h3>
        <p>{{ selectedEntry.paymentStatus === 'PAID' ? '请下载标签并贴在酒瓶或外箱，送样时便于主办方核对入库。' : '主办方确认付款后会开放标签下载。' }}</p>
        <div class="button-row">
          <el-button :disabled="selectedEntry.paymentStatus !== 'PAID'" type="primary" @click="downloadLabel">下载 PNG</el-button>
          <el-button :disabled="selectedEntry.paymentStatus !== 'PAID'">下载 PDF</el-button>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { competitions, entries } from './mockData'

const payableEntries = computed(() => [...entries].sort((a, b) => {
  if (a.paymentStatus === b.paymentStatus) return b.id - a.id
  return a.paymentStatus === 'PAID' ? 1 : -1
}))

const selectedEntry = ref(payableEntries.value[0])

function competitionName(competitionId) {
  return competitions.find((competition) => competition.id === competitionId)?.name || '未关联赛事'
}

function qrPattern(n) {
  return [1, 2, 3, 7, 8, 9, 15, 17, 22, 24, 25, 27, 30, 32, 36, 37, 40, 43, 45, 46, 47].includes(n) || n % 6 === 0
}

function downloadLabel() {
  ElMessage.success('标签已准备下载')
}
</script>

<style scoped>
.payment-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 390px;
  gap: 22px;
  align-items: start;
}

.payment-list,
.action-card {
  padding: 22px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.section-head p,
.pay-row p,
.action-card p,
.qr-label p,
.payment-guide p {
  color: #746a5f;
  line-height: 1.65;
}

.section-head a {
  color: #8b5c19;
  font-weight: 800;
  text-decoration: none;
}

.payment-guide {
  margin-top: 18px;
  padding: 16px;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.2);
  border-radius: 8px;
}

.payment-guide p {
  margin-bottom: 0;
}

.pay-rows {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.pay-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.pay-row.active {
  background: #2b1d10;
  color: #fff6df;
}

.pay-row.active p {
  color: #d8c8a8;
}

.pay-row strong,
.pay-row p {
  display: block;
}

.pay-row p {
  margin: 5px 0 0;
}

.label-panel {
  position: sticky;
  top: 116px;
  display: grid;
  gap: 16px;
}

.qr-label {
  padding: 24px;
  color: #2b1d10;
  text-align: center;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.18);
  border-radius: 8px;
  box-shadow: 0 22px 44px rgba(83, 51, 17, 0.14);
}

.qr-label > span {
  display: block;
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.qr-label h2 {
  margin: 22px 0;
  font-size: 24px;
}

.fake-qr {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 5px;
  width: 230px;
  height: 230px;
  padding: 16px;
  margin: 0 auto 22px;
  background: #f7ead0;
  border: 12px solid #2b1d10;
}

.fake-qr i.dark {
  background: #2b1d10;
}

.qr-label strong {
  display: block;
  font-size: 18px;
}

.action-card h3 {
  margin: 14px 0 8px;
  font-size: 22px;
}

.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

@media (max-width: 1080px) {
  .payment-page {
    grid-template-columns: 1fr;
  }

  .label-panel {
    position: static;
  }
}

@media (max-width: 680px) {
  .section-head,
  .pay-row {
    grid-template-columns: 1fr;
  }

  .section-head {
    display: grid;
  }
}
</style>
