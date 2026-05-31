<template>
  <div class="payment-page">
    <section class="payment-list brewer-card">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">付款与标签</h2>
          <p>完成报名费付款后，下载 UUID 现场标签并贴在酒瓶或外箱醒目位置。</p>
        </div>
        <span class="label-chip tone-amber">报名费 ¥{{ activeCompetition.entryFee }} / 款</span>
      </div>

      <div class="pay-rows">
        <article
          v-for="entry in payableEntries"
          :key="entry.id"
          :class="['pay-row', { active: selectedEntry.id === entry.id }]"
          @click="selectedEntry = entry"
        >
          <span :class="['label-chip', entry.paymentStatus === 'PAID' ? 'tone-green' : 'tone-amber']">
            {{ entry.paymentStatus === 'PAID' ? '已付款' : '待付款' }}
          </span>
          <div>
            <strong>{{ entry.name }}</strong>
            <p>{{ entry.uuid }} · {{ entry.categoryName }}</p>
          </div>
          <b>¥{{ entry.fee }}</b>
        </article>
      </div>
    </section>

    <aside class="qr-panel">
      <section class="qr-label">
        <span>现场评审标签</span>
        <h2>{{ selectedEntry.uuid }}</h2>
        <div class="fake-qr" aria-label="二维码示意">
          <i v-for="n in 49" :key="n" :class="{ dark: qrPattern(n) }" />
        </div>
        <strong>{{ activeCompetition.shortName }}</strong>
        <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }} · {{ selectedEntry.abv }}</p>
      </section>

      <section class="action-card brewer-card">
        <h3>{{ selectedEntry.name }}</h3>
        <p>{{ selectedEntry.paymentStatus === 'PAID' ? '现场标签已可下载，用于现场张贴。' : '当前酒款仍待付款，完成支付后可下载现场标签。' }}</p>
        <div class="button-row">
          <el-button v-if="selectedEntry.paymentStatus !== 'PAID'" type="primary" @click="payMock">模拟付款</el-button>
          <el-button :disabled="selectedEntry.paymentStatus !== 'PAID'" @click="downloadMock">下载 PNG</el-button>
          <el-button :disabled="selectedEntry.paymentStatus !== 'PAID'">下载 PDF</el-button>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { activeCompetition, entries } from './mockData'

const payableEntries = computed(() => [...entries].sort((a, b) => {
  if (a.paymentStatus === b.paymentStatus) {
    return b.id - a.id
  }
  return a.paymentStatus === 'PAID' ? 1 : -1
}))

const selectedEntry = ref(payableEntries.value[0])

function qrPattern(n) {
  return [1, 2, 3, 7, 8, 9, 15, 17, 22, 24, 25, 27, 30, 32, 36, 37, 40, 43, 45, 46, 47].includes(n) || n % 6 === 0
}

function payMock() {
  ElMessage.success('Mock 支付成功，实际接入微信支付后由回调更新状态')
}

function downloadMock() {
  ElMessage.success('Mock 下载：二维码标签已生成')
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
.qr-label p {
  color: #746a5f;
}

.pay-rows {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}

.pay-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
  transition: background 0.16s ease, border-color 0.16s ease;
}

.pay-row.active {
  background: #2b1d10;
  border-color: rgba(216, 144, 33, 0.38);
  color: #fff6df;
}

.pay-row strong,
.pay-row p {
  display: block;
}

.pay-row p {
  margin: 5px 0 0;
}

.pay-row.active p {
  color: #d8c8a8;
}

.qr-panel {
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

.fake-qr i {
  background: transparent;
}

.fake-qr i.dark {
  background: #2b1d10;
}

.qr-label strong {
  display: block;
  font-size: 18px;
}

.action-card h3 {
  margin: 0 0 8px;
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

  .qr-panel {
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
