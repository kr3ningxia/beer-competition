<template>
  <div class="payment-page">
    <section class="payment-list brewer-card">
      <div class="section-head">
        <div>
          <h2 class="portal-section-title">支付与送样</h2>
          <p>先确认付款，再下载现场标签并提交送样信息。</p>
        </div>
        <RouterLink to="/portal/my">返回我的参赛</RouterLink>
      </div>

      <div class="payment-guide">
        <strong>当前采用线下付款确认</strong>
        <p>请按赛事报名费完成转账或现场付款，并备注厂牌名称与酒款名称。主办方确认后，标签下载和送样填写会自动开放。</p>
      </div>

      <div class="pay-rows">
        <article
          v-for="entry in payableEntries"
          :key="entry.id"
          :class="['pay-row', { active: selectedEntry?.id === entry.id }]"
          @click="selectEntry(entry)"
        >
          <span :class="['label-chip', entry.paymentStatus === 'PAID' ? 'tone-green' : 'tone-amber']">
            {{ entry.paymentStatus === 'PAID' ? '已确认付款' : '等待付款确认' }}
          </span>
          <div>
            <strong>{{ entry.name }}</strong>
            <p>{{ competitionName(entry.competitionId) }} · {{ entry.categoryName }}</p>
          </div>
          <b>¥{{ entry.entryFee }}</b>
        </article>
        <div v-if="!payableEntries.length" class="empty-state">
          <strong>暂无报名酒款</strong>
          <p>提交报名后，这里会显示付款、标签和送样进度。</p>
        </div>
      </div>
    </section>

    <aside v-if="selectedEntry" class="workspace-panel">
      <section class="summary-card brewer-card">
        <div class="summary-head">
          <div>
            <span class="eyebrow">BREWER ENTRY WORKSPACE</span>
            <h3>{{ selectedEntry.name }}</h3>
            <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }} · {{ selectedEntry.abv }}%</p>
          </div>
          <span :class="['label-chip', selectedEntry.paymentStatus === 'PAID' ? 'tone-green' : 'tone-amber']">
            {{ selectedEntry.paymentStatus === 'PAID' ? '已确认付款' : '待付款确认' }}
          </span>
        </div>

        <div class="status-grid">
          <article>
            <small>报名费</small>
            <strong>¥{{ selectedEntry.entryFee }}</strong>
          </article>
          <article>
            <small>付款状态</small>
            <strong>{{ selectedEntry.paymentStatus === 'PAID' ? '已确认' : '待确认' }}</strong>
          </article>
          <article>
            <small>送样状态</small>
            <strong>{{ deliveryStatusText(selectedEntry.deliveryStatus) }}</strong>
          </article>
          <article>
            <small>入库状态</small>
            <strong>{{ isStored(selectedEntry) ? '已入库' : '待入库' }}</strong>
          </article>
        </div>
      </section>

      <section class="label-workspace">
        <section class="qr-label">
          <span>现场评审标签</span>
          <h2>{{ labelData?.uuid || selectedEntry.uuid }}</h2>
          <div class="fake-qr" aria-label="二维码示意">
            <i v-for="n in 49" :key="n" :class="{ dark: qrPattern(n) }" />
          </div>
          <strong>现场短编号 {{ labelData?.shortCode || selectedEntry.shortCode }}</strong>
          <p>{{ selectedEntry.categoryName }} · {{ selectedEntry.style }} · {{ selectedEntry.abv }}%</p>
        </section>

        <section class="action-card brewer-card">
          <span :class="['label-chip', selectedEntry.canDownloadLabel ? 'tone-green' : 'tone-amber']">
            {{ selectedEntry.canDownloadLabel ? '标签可下载' : '等待付款确认' }}
          </span>
          <h3>标签下载</h3>
          <p>{{ selectedEntry.canDownloadLabel ? '请将标签贴在酒瓶或外箱，便于现场核对与入库。' : '主办方确认付款后，这里会开放标签下载。' }}</p>
          <div class="button-row">
            <el-button :disabled="!selectedEntry.canDownloadLabel" type="primary" @click="downloadLabel">下载 PNG</el-button>
            <el-button :disabled="!selectedEntry.canDownloadLabel">下载 PDF</el-button>
          </div>
        </section>
      </section>

      <section class="delivery-card brewer-card">
        <div class="section-title">
          <div>
            <h3>送样信息</h3>
            <p>{{ selectedEntry.paymentStatus === 'PAID' ? '付款确认后，填写送样方式和快递信息。' : '付款确认后可填写送样信息。' }}</p>
          </div>
          <span :class="['label-chip', deliveryTone(selectedEntry.deliveryStatus)]">
            {{ deliveryStatusText(selectedEntry.deliveryStatus) }}
          </span>
        </div>

        <el-form label-position="top" class="delivery-form">
          <div class="delivery-grid">
            <el-form-item label="送样方式">
              <el-radio-group v-model="deliveryForm.deliveryMethod" :disabled="!selectedEntry.canDownloadLabel">
                <el-radio-button label="EXPRESS">快递寄送</el-radio-button>
                <el-radio-button label="ONSITE">现场送样</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="快递公司">
              <el-input
                v-model.trim="deliveryForm.carrier"
                :disabled="!selectedEntry.canDownloadLabel || deliveryForm.deliveryMethod !== 'EXPRESS'"
                placeholder="例如：顺丰、京东快递"
              />
            </el-form-item>

            <el-form-item label="快递单号">
              <el-input
                v-model.trim="deliveryForm.trackingNo"
                :disabled="!selectedEntry.canDownloadLabel || deliveryForm.deliveryMethod !== 'EXPRESS'"
                placeholder="填写本次送样的快递单号"
              />
            </el-form-item>

            <el-form-item label="送样备注" class="full-width">
              <el-input
                v-model.trim="deliveryForm.deliveryNote"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
                :disabled="!selectedEntry.canDownloadLabel"
                placeholder="例如：本次寄送 2 箱，每箱 6 瓶；或约定在比赛前一天下午现场送样。"
              />
            </el-form-item>
          </div>

          <div class="delivery-actions">
            <el-button
              type="primary"
              :disabled="!selectedEntry.canDownloadLabel"
              @click="saveDelivery"
            >
              保存送样信息
            </el-button>
          </div>
        </el-form>

        <dl class="delivery-facts">
          <div><dt>当前方式</dt><dd>{{ deliveryMethodText(selectedEntry.deliveryMethod) }}</dd></div>
          <div><dt>快递公司</dt><dd>{{ selectedEntry.carrier || '-' }}</dd></div>
          <div><dt>快递单号</dt><dd>{{ selectedEntry.trackingNo || '-' }}</dd></div>
          <div><dt>送样时间</dt><dd>{{ selectedEntry.deliverySubmittedAt || '-' }}</dd></div>
          <div><dt>入库确认</dt><dd>{{ selectedEntry.deliveryReceivedAt || '-' }}</dd></div>
        </dl>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchPortalEntries,
  fetchPortalEntryDetail,
  fetchPortalEntryLabel,
  submitPortalEntryDelivery,
} from '@/api/portal'

const route = useRoute()
const entries = ref([])
const selectedEntry = ref(null)
const labelData = ref(null)
const deliveryForm = reactive({
  deliveryMethod: 'EXPRESS',
  carrier: '',
  trackingNo: '',
  deliveryNote: '',
})

const payableEntries = computed(() => [...entries.value].sort((a, b) => {
  if (a.paymentStatus === b.paymentStatus) return b.id - a.id
  return a.paymentStatus === 'PAID' ? 1 : -1
}))

onMounted(async () => {
  entries.value = await fetchPortalEntries()
  const entryId = Number(route.query.entryId || 0)
  await selectEntry(entries.value.find((entry) => entry.id === entryId) || payableEntries.value[0] || null)
})

function competitionName(competitionId) {
  return entries.value.find((entry) => entry.competitionId === competitionId)?.competitionName || '未关联赛事'
}

function qrPattern(n) {
  return [1, 2, 3, 7, 8, 9, 15, 17, 22, 24, 25, 27, 30, 32, 36, 37, 40, 43, 45, 46, 47].includes(n) || n % 6 === 0
}

function deliveryMethodText(value) {
  if (value === 'EXPRESS') return '快递寄送'
  if (value === 'ONSITE') return '现场送样'
  return '未填写'
}

function deliveryStatusText(value) {
  if (value === 'SUBMITTED') return '已提交送样'
  if (value === 'RECEIVED') return '主办方已确认收到'
  return '待提交'
}

function deliveryTone(value) {
  if (value === 'RECEIVED') return 'tone-green'
  if (value === 'SUBMITTED') return 'tone-blue'
  return 'tone-amber'
}

function isStored(entry) {
  return entry?.stored || entry?.status === 'STORED' || entry?.status === 'RESULT_PUBLISHED'
}

function syncDeliveryForm(entry) {
  deliveryForm.deliveryMethod = entry?.deliveryMethod || 'EXPRESS'
  deliveryForm.carrier = entry?.carrier || ''
  deliveryForm.trackingNo = entry?.trackingNo || ''
  deliveryForm.deliveryNote = entry?.deliveryNote || ''
}

function downloadLabel() {
  ElMessage.success('标签已准备下载')
}

async function selectEntry(entry) {
  if (!entry) {
    selectedEntry.value = null
    labelData.value = null
    syncDeliveryForm(null)
    return
  }
  selectedEntry.value = await fetchPortalEntryDetail(entry.id)
  labelData.value = null
  syncDeliveryForm(selectedEntry.value)
  if (selectedEntry.value?.canDownloadLabel) {
    labelData.value = await fetchPortalEntryLabel(entry.id)
  }
}

async function refreshEntries(targetId = selectedEntry.value?.id) {
  entries.value = await fetchPortalEntries()
  if (!targetId) return
  const matched = entries.value.find((entry) => entry.id === targetId)
  if (matched) {
    await selectEntry(matched)
  }
}

async function saveDelivery() {
  if (!selectedEntry.value?.canDownloadLabel) {
    ElMessage.warning('付款确认后再填写送样信息')
    return
  }
  if (deliveryForm.deliveryMethod === 'EXPRESS' && (!deliveryForm.carrier || !deliveryForm.trackingNo)) {
    ElMessage.warning('请先填写快递公司和快递单号')
    return
  }
  await submitPortalEntryDelivery(selectedEntry.value.id, { ...deliveryForm })
  ElMessage.success('送样信息已保存')
  await refreshEntries(selectedEntry.value.id)
}
</script>

<style scoped>
.payment-page {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.payment-list,
.summary-card,
.action-card,
.delivery-card {
  padding: 22px;
}

.section-head,
.summary-head,
.section-title {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.section-head p,
.payment-guide p,
.pay-row p,
.summary-head p,
.action-card p,
.section-title p,
.qr-label p {
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

.workspace-panel {
  position: sticky;
  top: 116px;
  display: grid;
  gap: 16px;
}

.summary-head h3,
.action-card h3,
.section-title h3 {
  margin: 8px 0 6px;
  color: #2b1d10;
  font-size: 22px;
}

.eyebrow,
.qr-label > span {
  display: block;
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.status-grid article {
  padding: 14px;
  background: #fff8ea;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.status-grid small {
  display: block;
  color: #8a7863;
}

.status-grid strong {
  display: block;
  margin-top: 6px;
  color: #2b1d10;
  font-size: 18px;
}

.label-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
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

.button-row,
.delivery-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.delivery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.full-width {
  grid-column: 1 / -1;
}

.delivery-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin: 20px 0 0;
}

.delivery-facts div {
  padding: 14px;
  background: #fff8ea;
  border: 1px solid rgba(87, 58, 26, 0.1);
  border-radius: 8px;
}

.delivery-facts dt {
  color: #8a7863;
  font-size: 12px;
}

.delivery-facts dd {
  margin: 8px 0 0;
  color: #2b1d10;
  font-weight: 800;
}

.empty-state {
  padding: 18px;
  color: #6f6152;
  background: #fff8ea;
  border: 1px dashed rgba(87, 58, 26, 0.18);
  border-radius: 8px;
}

@media (max-width: 1280px) {
  .payment-page,
  .label-workspace {
    grid-template-columns: 1fr;
  }

  .workspace-panel {
    position: static;
  }
}

@media (max-width: 720px) {
  .pay-row,
  .status-grid,
  .delivery-grid,
  .delivery-facts {
    grid-template-columns: 1fr;
  }

  .section-head,
  .summary-head,
  .section-title {
    display: grid;
  }
}
</style>
