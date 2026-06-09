<template>
  <div class="payment-page">
    <section class="page-head brewer-card">
      <div class="page-title">
        <span class="section-kicker">寄样办理</span>
        <h1>下载标签并提交寄样</h1>
        <p>{{ currentAction.description }}</p>
      </div>
      <RouterLink class="back-link" to="/portal/my">返回我的参赛</RouterLink>
      <div v-if="selectedEntry" class="flow-steps" aria-label="寄样进度">
        <span :class="{ done: selectedEntry.paymentStatus === 'PAID' || selectedEntry.canDownloadLabel, active: selectedEntry.paymentStatus !== 'PAID' }">
          <b>1</b>支付报名费
        </span>
        <span :class="{ done: selectedEntry.canDownloadLabel, active: selectedEntry.paymentStatus === 'PAID' && !selectedEntry.canDownloadLabel }">
          <b>2</b>标签下载
        </span>
        <span :class="{ done: hasSubmittedDelivery, active: selectedEntry.canDownloadLabel && !hasSubmittedDelivery }">
          <b>3</b>寄样信息
        </span>
        <span :class="{ done: selectedEntry.deliveryStatus === 'RECEIVED' || isStored(selectedEntry), active: hasSubmittedDelivery && selectedEntry.deliveryStatus !== 'RECEIVED' && !isStored(selectedEntry) }">
          <b>4</b>主办方签收
        </span>
      </div>
    </section>

    <section class="delivery-workbench">
      <aside class="entry-panel brewer-card">
        <div class="panel-head compact-head">
          <div>
            <span class="section-kicker">我的酒款</span>
            <h2>报名酒款</h2>
          </div>
        </div>

        <div class="entry-list">
          <article
            v-for="entry in payableEntries"
            :key="entry.id"
            :class="['entry-row', { active: selectedEntry?.id === entry.id }]"
            @click="selectEntry(entry)"
          >
            <div class="entry-row-main">
              <div class="entry-row-top">
                <strong>{{ entry.name }}</strong>
                <b v-if="entry.paymentStatus !== 'PAID'">{{ formatCurrency(entryPayAmount(entry)) }}</b>
              </div>
              <p>{{ competitionLabel(entry) }}</p>
              <small>{{ entryTaskLabel(entry) }}</small>
            </div>
          </article>

          <div v-if="!payableEntries.length" class="empty-state">
            <strong>暂无报名酒款</strong>
            <p>提交报名后，这里会显示每款酒的付款、标签和寄样进度。</p>
          </div>
        </div>
      </aside>

      <main v-if="selectedEntry" class="task-panel">
        <section class="current-card brewer-card">
          <div>
            <span class="section-kicker">当前酒款</span>
            <h2>{{ selectedEntry.name }}</h2>
            <p>{{ entryMetaLine(selectedEntry) }}</p>
          </div>
          <div class="current-side">
            <span :class="['task-chip', taskTone(selectedEntry)]">{{ entryTaskLabel(selectedEntry) }}</span>
            <el-button @click="refreshEntries(selectedEntry.id)">刷新状态</el-button>
          </div>
        </section>

        <section v-if="showPaymentCard" class="payment-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">报名支付</span>
              <h3>补缴报名费</h3>
              <p>这款酒还未完成支付，支付成功后开放标签下载和寄样信息填写。</p>
            </div>
            <strong>{{ formatCurrency(entryPayAmount(selectedEntry)) }}</strong>
          </div>
          <dl class="inline-facts">
            <div>
              <dt>付款方式</dt>
              <dd>{{ paymentMethodText(selectedEntry.payment?.payMethod) }}</dd>
            </div>
            <div>
              <dt>当前状态</dt>
              <dd>{{ entryTaskLabel(selectedEntry) }}</dd>
            </div>
          </dl>
          <div class="payment-actions">
            <el-button type="primary" size="large" :loading="simulatingPayment" @click="simulatePayment">
              模拟支付成功
            </el-button>
            <span>后续接入微信支付后，这里会拉起微信支付。</span>
          </div>
        </section>

        <section v-if="showLabelCard" class="label-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">LABEL</span>
              <h3>{{ labelCardTitle }}</h3>
              <p>{{ labelCardDescription }}</p>
            </div>
            <span :class="['task-chip', selectedEntry.canDownloadLabel ? 'tone-green' : 'tone-amber']">
              {{ selectedEntry.canDownloadLabel ? '可下载' : '待支付' }}
            </span>
          </div>

          <div class="label-preview-wrap">
            <div class="label-preview" v-html="labelSvg" />
          </div>
          <div class="label-actions">
            <el-button type="primary" :disabled="!selectedEntry.canDownloadLabel" @click="downloadLabelPng">
              下载 PNG
            </el-button>
            <el-button :disabled="!selectedEntry.canDownloadLabel" @click="openPrintLabel">
              打印标签 / 保存 PDF
            </el-button>
          </div>
          <p class="label-tip">瓶身至少贴 1 张；整箱寄送时，建议外箱再贴 1 张。</p>
        </section>

        <section v-if="showDeliveryCard" class="delivery-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">DELIVERY FORM</span>
              <h3>{{ deliveryCardTitle }}</h3>
              <p>{{ deliveryCardDescription }}</p>
            </div>
            <span :class="['task-chip', deliveryTone(selectedEntry.deliveryStatus)]">
              {{ deliveryStatusText(selectedEntry.deliveryStatus) }}
            </span>
          </div>

          <div v-if="showDeliverySummary" class="delivery-summary">
            <dl class="summary-list">
              <div v-for="item in deliverySummaryItems" :key="item.label">
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>

            <div class="button-row">
              <el-button
                v-if="selectedEntry.deliveryStatus !== 'RECEIVED'"
                type="primary"
                plain
                @click="editingDelivery = true"
              >
                修改寄样信息
              </el-button>
              <el-button @click="refreshEntries(selectedEntry.id)">刷新状态</el-button>
            </div>
          </div>

          <el-form v-else label-position="top" class="delivery-form">
            <div class="delivery-grid">
              <el-form-item label="送样方式">
                <el-radio-group v-model="deliveryForm.deliveryMethod">
                  <el-radio-button label="EXPRESS" :disabled="selectedLogistics.deliveryMethod === 'ONSITE'">快递寄送</el-radio-button>
                  <el-radio-button label="ONSITE" :disabled="selectedLogistics.deliveryMethod === 'EXPRESS'">现场送样</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item v-if="deliveryForm.deliveryMethod === 'EXPRESS'" label="快递公司">
                <el-input v-model.trim="deliveryForm.carrier" placeholder="例如：顺丰、京东快递" />
              </el-form-item>

              <el-form-item v-if="deliveryForm.deliveryMethod === 'EXPRESS'" label="快递单号">
                <el-input v-model.trim="deliveryForm.trackingNo" placeholder="填写本次寄样的快递单号" />
              </el-form-item>

              <el-form-item label="送样备注" class="full-width">
                <el-input
                  v-model.trim="deliveryForm.deliveryNote"
                  type="textarea"
                  :rows="4"
                  maxlength="500"
                  show-word-limit
                  placeholder="例如：本次寄送 2 箱，每箱 6 瓶；或约定在比赛前一天下午现场送样。"
                />
              </el-form-item>
            </div>

            <div class="button-row">
              <el-button type="primary" :loading="savingDelivery" @click="saveDelivery">
                保存并提交寄样信息
              </el-button>
              <el-button v-if="hasSubmittedDelivery" @click="cancelEditingDelivery">取消修改</el-button>
            </div>
          </el-form>
        </section>
      </main>

      <aside v-if="selectedEntry" class="requirement-aside brewer-card">
        <div class="aside-head">
          <span class="section-kicker">REQUIREMENTS</span>
          <h2>寄样要求</h2>
        </div>

        <dl class="aside-list">
          <div>
            <dt>建议送达</dt>
            <dd>{{ arrivalWindowText }}</dd>
          </div>
          <div>
            <dt>送样方式</dt>
            <dd>{{ competitionDeliveryMethodText }}</dd>
          </div>
          <div>
            <dt>酒样要求</dt>
            <dd>{{ selectedLogistics.sampleQuantityNote || '以主办方赛事通知为准。' }}</dd>
          </div>
          <div>
            <dt>标签粘贴</dt>
            <dd>每款酒瓶身至少贴 1 张，整箱寄送时外箱再贴 1 张。</dd>
          </div>
          <div>
            <dt>包装检查</dt>
            <dd>{{ selectedLogistics.deliveryNote || '请做好防震、防漏和外箱加固，避免运输中破损。' }}</dd>
          </div>
        </dl>

        <section class="address-block">
          <span class="section-kicker">ADDRESS</span>
          <h3>收件信息</h3>
          <p>{{ logisticsAddressText }}</p>
          <el-button v-if="canCopyLogisticsAddress" @click="copyLogisticsAddress">复制收件信息</el-button>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import {
  fetchPortalEntries,
  fetchPortalEntryDetail,
  fetchPortalEntryLabel,
  simulatePortalEntryPayment,
  submitPortalEntryDelivery,
} from '@/api/portal'
import { JUDGE_H5_BASE_URL } from '@/config'
import { entryPayAmount } from './portalViewModels'

const route = useRoute()

const entries = ref([])
const selectedEntry = ref(null)
const labelData = ref(null)
const editingDelivery = ref(false)
const savingDelivery = ref(false)
const simulatingPayment = ref(false)
const deliveryForm = reactive({
  deliveryMethod: 'EXPRESS',
  carrier: '',
  trackingNo: '',
  deliveryNote: '',
})

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  maximumFractionDigits: 0,
})

const payableEntries = computed(() => {
  return [...entries.value].sort((a, b) => entrySortWeight(a) - entrySortWeight(b) || b.id - a.id)
})
const selectedLogistics = computed(() => selectedEntry.value?.competitionLogistics || {})

const hasSubmittedDelivery = computed(() => hasDeliveryProgress(selectedEntry.value))
const showPaymentCard = computed(() => selectedEntry.value?.paymentStatus !== 'PAID')
const showLabelCard = computed(() => Boolean(selectedEntry.value?.canDownloadLabel))
const showPreparationCards = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return false
  return !hasSubmittedDelivery.value
})
const showDeliveryCard = computed(() => selectedEntry.value?.canDownloadLabel)

const showDeliverySummary = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return false
  if (!hasSubmittedDelivery.value) return false
  return !editingDelivery.value
})

const labelRecord = computed(() => ({
  uuid: labelData.value?.uuid || selectedEntry.value?.uuid || '',
  labelCode: labelData.value?.labelCode || selectedEntry.value?.labelCode || '',
  shortCode: labelData.value?.shortCode || selectedEntry.value?.shortCode || '',
  scanToken: labelData.value?.scanToken || selectedEntry.value?.scanToken || '',
  categoryName: labelData.value?.categoryName || selectedEntry.value?.categoryName || '',
  style: labelData.value?.style || selectedEntry.value?.style || '',
  abv: labelData.value?.abv ?? selectedEntry.value?.abv ?? '',
}))

const labelSvg = computed(() => buildLabelSvg(labelRecord.value))
const labelCardTitle = computed(() => {
  if (hasSubmittedDelivery.value) return '需要时可补打现场标签'
  return '寄样前请先贴好标签'
})
const labelCardDescription = computed(() => {
  if (hasSubmittedDelivery.value) return '标签会一直保留，后续补贴瓶身、外箱或现场核对时都可以重新下载。'
  return '建议优先使用打印版，贴在酒瓶或外箱醒目位置，便于主办方核对和入库。'
})
const labelActionDescription = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return '支付成功后即可下载标签。'
  if (hasSubmittedDelivery.value) return '如现场需要补贴、重贴或核对编号，可重新下载或打印这一张标签。'
  return '打印后直接贴标，再回到下方填写寄样方式和快递单号。'
})

const arrivalWindowText = computed(() => {
  const start = formatDateTime(selectedLogistics.value.sampleArrivalStart)
  const deadline = formatDateTime(selectedLogistics.value.sampleArrivalDeadline)
  if (start !== '待更新' && deadline !== '待更新') return `${start} 至 ${deadline}`
  if (deadline !== '待更新') return `${deadline} 前送达`
  if (!selectedEntry.value?.competitionDate) return '请至少预留 2 到 3 天运输时间，具体以主办方通知为准'
  return `${formatDate(selectedEntry.value.competitionDate)} 前送达更稳妥`
})
const competitionDeliveryMethodText = computed(() => {
  if (selectedLogistics.value.deliveryMethod === 'EXPRESS') return '快递寄送'
  if (selectedLogistics.value.deliveryMethod === 'ONSITE') return '现场送样'
  return '快递寄送 / 现场送样'
})
const logisticsAddressLines = computed(() => [
  selectedLogistics.value.deliveryRecipient,
  selectedLogistics.value.deliveryPhone,
  selectedLogistics.value.deliveryAddress,
].filter(Boolean))
const canCopyLogisticsAddress = computed(() => logisticsAddressLines.value.length > 0)
const logisticsAddressText = computed(() => {
  if (logisticsAddressLines.value.length) return logisticsAddressLines.value.join(' · ')
  if (selectedLogistics.value.logisticsVisibility === 'PAYMENT_CONFIRMED') return '支付成功后显示完整收件信息。'
  return '主办方暂未填写完整收件信息，寄出前请先联系确认。'
})
const currentAction = computed(() => {
  const entry = selectedEntry.value
  if (!entry) {
    return {
      title: '请选择酒款',
      description: '左侧列表会显示每款酒当前的付款与寄样进度。',
    }
  }

  if (entry.paymentStatus !== 'PAID') {
    return {
      title: '支付报名费',
      description: '核对金额并完成支付，支付成功后下载现场标签并填写寄样信息。',
    }
  }

  if (!entry.canDownloadLabel) {
    return {
      title: '付款已完成，等待标签开放',
      description: '当前酒款已经进入处理中，标签一开放就可以下载打印并继续填写寄样信息。',
    }
  }

  if (!hasDeliveryProgress(entry)) {
    return {
      title: '下载标签并提交快递信息',
      description: '先打印并贴好标签，再把送样方式、快递公司和快递单号填写完整。',
    }
  }

  if (entry.deliveryStatus === 'RECEIVED' || isStored(entry)) {
    return {
      title: '样品已签收，可等待入库或后续赛事进度',
      description: '主办方已经确认收到样品，这一款酒的寄样环节基本完成。',
    }
  }

  return {
    title: '等待主办方签收样品',
    description: '快递信息已经提交，接下来关注物流进度并等待主办方确认签收即可。',
  }
})

const deliveryCardTitle = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return '支付成功后可填写寄样信息'
  if (showDeliverySummary.value) return selectedEntry.value.deliveryStatus === 'RECEIVED' ? '样品已签收' : '你已提交寄样信息'
  return hasSubmittedDelivery.value ? '修改寄样信息' : '填写寄样信息'
})

const deliveryCardDescription = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return '支付成功后这里会自动开放。'
  if (showDeliverySummary.value) return selectedEntry.value.deliveryStatus === 'RECEIVED' ? '主办方已经确认收到样品，你可以在这里查看本次提交内容。' : '快递信息已提交，建议保留单号并关注签收进度。'
  return '寄出后请尽快提交快递信息，避免主办方无法及时核对来样。'
})

const deliverySummaryItems = computed(() => {
  const entry = selectedEntry.value
  if (!entry) return []

  return [
    {
      label: '送样方式',
      value: deliveryMethodText(entry.deliveryMethod),
    },
    {
      label: '快递公司',
      value: entry.carrier,
    },
    {
      label: '快递单号',
      value: entry.trackingNo,
    },
    {
      label: '提交时间',
      value: formatDateTime(entry.deliverySubmittedAt),
    },
    {
      label: '签收时间',
      value: formatDateTime(entry.deliveryReceivedAt, true),
    },
    {
      label: '送样备注',
      value: entry.deliveryNote,
    },
  ].filter((item) => item.value)
})

const isDirty = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return false
  return JSON.stringify(normalizeDeliveryPayload(deliveryForm)) !== JSON.stringify(entryDeliverySnapshot(selectedEntry.value))
})

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  entries.value = await fetchPortalEntries()
  const entryId = Number(route.query.entryId || 0)
  const target = entries.value.find((entry) => entry.id === entryId) || payableEntries.value[0] || null
  await selectEntry(target, { skipConfirm: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

function handleBeforeUnload(event) {
  if (!isDirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

function entrySortWeight(entry) {
  if (entry.paymentStatus !== 'PAID') return 0
  if (entry.deliveryStatus === 'RECEIVED' || isStored(entry)) return 3
  if (hasDeliveryProgress(entry)) return 2
  return 1
}

function competitionName(competitionId) {
  return entries.value.find((entry) => entry.competitionId === competitionId)?.competitionName || '未关联赛事'
}

function competitionLabel(entry) {
  return `${entry.competitionName || competitionName(entry.competitionId)} · ${entry.categoryName || '未关联组别'}`
}

function entryMetaLine(entry) {
  const parts = [entry.competitionName || competitionName(entry.competitionId), entry.categoryName]
  if (entry.style) parts.push(entry.style)
  if (entry.abv !== null && entry.abv !== undefined && entry.abv !== '') parts.push(`${entry.abv}%`)
  return parts.filter(Boolean).join(' · ')
}

function deliveryMethodText(value) {
  if (value === 'EXPRESS') return '快递寄送'
  if (value === 'ONSITE') return '现场送样'
  return '未填写'
}

function paymentMethodText(value) {
  if (value === 'MANUAL') return '人工确认'
  if (value === 'MOCK') return '模拟支付'
  if (value === 'WECHAT') return '微信支付'
  return '待支付'
}

function deliveryStatusText(value) {
  if (value === 'SUBMITTED') return '已提交寄样信息'
  if (value === 'RECEIVED') return '主办方已确认收到'
  return '待提交'
}

function deliveryTone(value) {
  if (value === 'RECEIVED') return 'tone-green'
  if (value === 'SUBMITTED') return 'tone-blue'
  return 'tone-amber'
}

function taskTone(entry) {
  if (!entry) return 'tone-amber'
  if (entry.deliveryStatus === 'RECEIVED' || isStored(entry)) return 'tone-green'
  if (hasDeliveryProgress(entry)) return 'tone-blue'
  if (entry.paymentStatus === 'PAID') return 'tone-brown'
  return 'tone-amber'
}

function entryTaskLabel(entry) {
  if (!entry) return '待处理'
  if (entry.deliveryStatus === 'RECEIVED' || isStored(entry)) return '已签收 / 入库'
  if (hasDeliveryProgress(entry)) return '等待主办方签收'
  if (entry.paymentStatus === 'PAID') return '待提交寄样'
  return '待支付'
}

function isStored(entry) {
  return entry?.stored || entry?.storedFlag === 1 || entry?.status === 'STORED' || entry?.status === 'RESULT_PUBLISHED'
}

function hasDeliveryProgress(entry) {
  if (!entry) return false
  return ['SUBMITTED', 'RECEIVED'].includes(entry.deliveryStatus) || Boolean(entry.deliverySubmittedAt || entry.trackingNo || entry.deliveryMethod)
}

function syncDeliveryForm(entry) {
  deliveryForm.deliveryMethod = entry?.deliveryMethod || defaultDeliveryMethod()
  deliveryForm.carrier = entry?.carrier || ''
  deliveryForm.trackingNo = entry?.trackingNo || ''
  deliveryForm.deliveryNote = entry?.deliveryNote || ''
  editingDelivery.value = entry?.canDownloadLabel && !hasDeliveryProgress(entry)
}

function cancelEditingDelivery() {
  syncDeliveryForm(selectedEntry.value)
}

function normalizeDeliveryPayload(source) {
  return {
    deliveryMethod: source.deliveryMethod || defaultDeliveryMethod(),
    carrier: source.deliveryMethod === 'EXPRESS' ? (source.carrier || '').trim() : '',
    trackingNo: source.deliveryMethod === 'EXPRESS' ? (source.trackingNo || '').trim() : '',
    deliveryNote: (source.deliveryNote || '').trim(),
  }
}

function entryDeliverySnapshot(entry) {
  return normalizeDeliveryPayload({
    deliveryMethod: entry?.deliveryMethod || defaultDeliveryMethod(),
    carrier: entry?.carrier || '',
    trackingNo: entry?.trackingNo || '',
    deliveryNote: entry?.deliveryNote || '',
  })
}

function defaultDeliveryMethod() {
  return selectedLogistics.value.deliveryMethod === 'ONSITE' ? 'ONSITE' : 'EXPRESS'
}

async function selectEntry(entry, options = {}) {
  if (!entry) {
    selectedEntry.value = null
    labelData.value = null
    syncDeliveryForm(null)
    return
  }

  if (!options.skipConfirm && selectedEntry.value?.id !== entry.id && isDirty.value) {
    try {
      await ElMessageBox.confirm('当前酒款还有未保存的寄样信息，切换后会丢失这些修改。是否继续切换？', '未保存的修改', {
        confirmButtonText: '继续切换',
        cancelButtonText: '留在当前酒款',
        type: 'warning',
      })
    } catch {
      return
    }
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
    await selectEntry(matched, { skipConfirm: true })
  }
}

async function saveDelivery() {
  if (!selectedEntry.value?.canDownloadLabel) {
    ElMessage.warning('支付成功后才能填写寄样信息')
    return
  }

  const payload = normalizeDeliveryPayload(deliveryForm)
  if (payload.deliveryMethod === 'EXPRESS' && (!payload.carrier || !payload.trackingNo)) {
    ElMessage.warning('请先填写快递公司和快递单号')
    return
  }

  savingDelivery.value = true
  try {
    await submitPortalEntryDelivery(selectedEntry.value.id, payload)
    ElMessage.success('寄样信息已提交')
    editingDelivery.value = false
    await refreshEntries(selectedEntry.value.id)
  } finally {
    savingDelivery.value = false
  }
}

async function simulatePayment() {
  if (!selectedEntry.value || selectedEntry.value.paymentStatus === 'PAID') return

  simulatingPayment.value = true
  try {
    selectedEntry.value = await simulatePortalEntryPayment(selectedEntry.value.id)
    ElMessage.success('支付成功，可以下载标签并填写寄样信息')
    await refreshEntries(selectedEntry.value.id)
  } catch (error) {
    ElMessage.warning(error?.message || '支付失败，请稍后重试')
  } finally {
    simulatingPayment.value = false
  }
}

async function copyLogisticsAddress() {
  const text = logisticsAddressLines.value.join('\n')
  if (!text) return
  await navigator.clipboard.writeText(text)
  ElMessage.success('收件信息已复制')
}

function formatCurrency(value) {
  if (value === null || value === undefined || value === '') return '¥0'
  return currencyFormatter.format(Number(value))
}

function formatDate(value) {
  if (!value) return '待公布'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatDateTime(value, emptyAsBlank = false) {
  if (!value) return emptyAsBlank ? '' : '待更新'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function buildLabelSvg(label) {
  const qr = QRCode.create(scanUrl(label), { errorCorrectionLevel: 'M', margin: 0 })
  const matrix = qr.modules
  const cells = []
  const qrSize = 176
  const cellSize = qrSize / matrix.size
  const offsetX = 42
  const offsetY = 88

  for (let row = 0; row < matrix.size; row += 1) {
    for (let col = 0; col < matrix.size; col += 1) {
      if (!matrix.data[row * matrix.size + col]) continue
      cells.push(`<rect x="${formatSvgNumber(offsetX + col * cellSize)}" y="${formatSvgNumber(offsetY + row * cellSize)}" width="${formatSvgNumber(cellSize)}" height="${formatSvgNumber(cellSize)}" fill="#2e2014" />`)
    }
  }

  const category = escapeXml(label.categoryName || '待确认组别')
  const style = escapeXml(label.style || 'Style Pending')
  const abv = label.abv !== '' && label.abv !== null && label.abv !== undefined ? `${label.abv}%` : 'ABV Pending'
  const code = escapeXml(label.shortCode || 'PENDING')
  const labelCode = escapeXml(label.labelCode || label.uuid || '等待开放标签')

  return `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 260 360" role="img" aria-label="现场评审标签">
      <rect width="260" height="360" rx="24" fill="#fff8ec"/>
      <rect x="14" y="14" width="232" height="332" rx="18" fill="#fffdf9" stroke="#d4bf9f"/>
      <text x="130" y="44" text-anchor="middle" font-size="12" font-weight="700" letter-spacing="1.4" fill="#8c6330">现场评审标签</text>
      <text x="130" y="66" text-anchor="middle" font-size="22" font-weight="800" fill="#24170f">${labelCode}</text>
      <rect x="28" y="74" width="204" height="204" rx="18" fill="#f7ecd8" stroke="#3a2818" stroke-width="10"/>
      ${cells.join('')}
      <text x="130" y="302" text-anchor="middle" font-size="12" font-weight="700" fill="#8c6330">现场短编号</text>
      <text x="130" y="326" text-anchor="middle" font-size="24" font-weight="900" fill="#24170f">${code}</text>
      <text x="130" y="344" text-anchor="middle" font-size="13" fill="#665647">${category} · ${style} · ${abv}</text>
    </svg>
  `
}

function scanUrl(label) {
  const token = encodeURIComponent(label.scanToken || label.shortCode || label.labelCode || label.uuid || '')
  return `${JUDGE_H5_BASE_URL.replace(/\/$/, '')}/q/${token}`
}

function formatSvgNumber(value) {
  return Number(value.toFixed(3))
}

function escapeXml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&apos;')
}

async function downloadLabelPng() {
  if (!selectedEntry.value?.canDownloadLabel) return

  try {
    const svgBlob = new Blob([labelSvg.value], { type: 'image/svg+xml;charset=utf-8' })
    const svgUrl = URL.createObjectURL(svgBlob)
    const image = new Image()

    await new Promise((resolve, reject) => {
      image.onload = resolve
      image.onerror = reject
      image.src = svgUrl
    })

    const canvas = document.createElement('canvas')
    canvas.width = 1040
    canvas.height = 1440
    const context = canvas.getContext('2d')
    context.fillStyle = '#fff8ec'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.drawImage(image, 0, 0, canvas.width, canvas.height)

    const pngBlob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
    URL.revokeObjectURL(svgUrl)

    if (!pngBlob) {
      throw new Error('标签图片生成失败')
    }

    triggerDownload(URL.createObjectURL(pngBlob), `${selectedEntry.value.shortCode || 'entry-label'}.png`)
    ElMessage.success('标签 PNG 已开始下载')
  } catch {
    ElMessage.error('标签下载失败，请稍后重试')
  }
}

function openPrintLabel() {
  if (!selectedEntry.value?.canDownloadLabel) return

  const printWindow = window.open('', '_blank', 'noopener,noreferrer,width=900,height=1200')
  if (!printWindow) {
    ElMessage.warning('浏览器拦截了新窗口，请允许弹窗后重试')
    return
  }

  printWindow.document.write(`
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="utf-8" />
        <title>打印标签</title>
        <style>
          body {
            margin: 0;
            padding: 24px;
            background: #f3ead8;
            font-family: "Microsoft YaHei", sans-serif;
          }
          .sheet {
            width: 420px;
            margin: 0 auto;
            padding: 24px;
            background: #fffdf9;
            border-radius: 16px;
          }
          .hint {
            margin-top: 16px;
            color: #6f5a44;
            font-size: 13px;
            line-height: 1.6;
            text-align: center;
          }
          @media print {
            body {
              padding: 0;
              background: #fff;
            }
            .sheet {
              width: auto;
              padding: 0;
              box-shadow: none;
            }
            .hint {
              display: none;
            }
          }
        </style>
      </head>
      <body>
        <div class="sheet">
          ${labelSvg.value}
          <p class="hint">可直接打印，也可在打印面板中选择“另存为 PDF”。</p>
        </div>
        <script>
          window.onload = function () {
            window.print();
          };
        <\/script>
      </body>
    </html>
  `)
  printWindow.document.close()
}

function triggerDownload(objectUrl, fileName) {
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(objectUrl), 500)
}
</script>

<style scoped>
.payment-page {
  --ink: #2f2116;
  --subtle-ink: #75624d;
  --muted-ink: #9a856a;
  --line: rgba(103, 72, 39, 0.13);
  --cream: #fff9ef;
  --paper: #fffdf8;
  --accent: #b9781f;
  --accent-deep: #8d5b18;
  display: grid;
  gap: 18px;
  width: min(100%, 1240px);
  margin: 0 auto;
  color: var(--ink);
}

.page-head,
.entry-panel,
.current-card,
.payment-card,
.label-card,
.delivery-card,
.requirement-aside {
  padding: 22px;
  border-radius: 8px;
}

.page-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px 24px;
  align-items: start;
}

.page-title h1,
.compact-head h2,
.current-card h2,
.aside-head h2 {
  margin: 8px 0 6px;
  line-height: 1.15;
}

.page-title h1 {
  font-size: 30px;
}

.page-title p,
.current-card p,
.card-head p,
.entry-row p,
.label-tip,
.address-block p {
  margin: 0;
  color: var(--subtle-ink);
  line-height: 1.65;
}

.section-kicker {
  display: block;
  color: var(--accent-deep);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.back-link {
  color: var(--accent-deep);
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.flow-steps {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding-top: 4px;
}

.flow-steps span {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  color: var(--muted-ink);
  background: #fff7e8;
  border: 1px solid var(--line);
  border-radius: 8px;
  font-weight: 800;
}

.flow-steps b {
  display: grid;
  place-items: center;
  flex: 0 0 22px;
  width: 22px;
  height: 22px;
  color: #8b5c19;
  background: #f3dfb9;
  border-radius: 50%;
  font-size: 12px;
}

.flow-steps span.done {
  color: #23603d;
  background: #e4f3e2;
}

.flow-steps span.active {
  color: #3a2615;
  background: #f4d995;
  border-color: rgba(185, 120, 31, 0.26);
}

.delivery-workbench {
  display: grid;
  grid-template-columns: 276px minmax(520px, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.entry-panel,
.requirement-aside {
  position: sticky;
  top: 76px;
}

.compact-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.entry-list,
.task-panel {
  display: grid;
  gap: 12px;
}

.entry-list {
  margin-top: 14px;
}

.entry-row {
  position: relative;
  padding: 14px 14px 14px 16px;
  cursor: pointer;
  background: linear-gradient(180deg, #fffaf1 0%, #fff6e7 100%);
  border: 1px solid var(--line);
  border-radius: 8px;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.entry-row::before {
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 0;
  width: 3px;
  border-radius: 999px;
  background: transparent;
  content: '';
}

.entry-row:hover {
  transform: translateY(-1px);
  border-color: rgba(185, 120, 31, 0.28);
  box-shadow: 0 10px 24px rgba(85, 56, 25, 0.08);
}

.entry-row.active {
  background: #fffdf8;
  border-color: rgba(185, 120, 31, 0.34);
}

.entry-row.active::before {
  background: var(--accent);
}

.entry-row-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.entry-row strong {
  color: var(--ink);
  line-height: 1.35;
}

.entry-row b {
  color: var(--accent-deep);
  font-size: 13px;
  white-space: nowrap;
}

.entry-row p {
  margin-top: 6px;
  font-size: 13px;
}

.entry-row small {
  display: block;
  margin-top: 8px;
  color: var(--accent-deep);
  font-weight: 800;
}

.current-card,
.card-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.current-card h2,
.card-head h3 {
  margin: 8px 0 6px;
  font-size: 28px;
  line-height: 1.2;
}

.current-side {
  display: grid;
  gap: 10px;
  justify-items: end;
}

.payment-card,
.label-card,
.delivery-card {
  background: linear-gradient(180deg, rgba(255, 253, 248, 0.98), rgba(255, 248, 236, 0.98));
}

.card-head strong {
  color: var(--accent-deep);
  font-size: 24px;
  white-space: nowrap;
}

.inline-facts,
.summary-list,
.aside-list {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
}

.inline-facts {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.payment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-top: 16px;
}

.payment-actions span {
  color: var(--subtle-ink);
  font-size: 13px;
  line-height: 1.5;
}

.inline-facts div,
.summary-list div,
.aside-list div {
  padding: 13px;
  background: var(--cream);
  border: 1px solid var(--line);
  border-radius: 8px;
}

dt,
dd {
  margin: 0;
}

dt {
  color: var(--muted-ink);
  font-size: 12px;
  font-weight: 800;
}

dd {
  margin-top: 6px;
  color: var(--ink);
  font-weight: 800;
  line-height: 1.55;
}

.label-preview-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.label-preview {
  width: min(100%, 320px);
  padding: 16px;
  background: linear-gradient(180deg, #f6ebd7 0%, #fff7ea 100%);
  border: 1px solid rgba(102, 70, 36, 0.13);
  border-radius: 8px;
}

.label-preview :deep(svg) {
  display: block;
  width: 100%;
  height: auto;
  max-height: 390px;
}

.label-actions,
.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.label-actions {
  justify-content: center;
  margin-top: 14px;
}

.label-tip {
  margin-top: 10px;
  text-align: center;
  font-size: 13px;
}

.delivery-form,
.delivery-summary {
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

.summary-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.requirement-aside {
  display: grid;
  gap: 16px;
}

.aside-list div {
  background: #fffaf1;
}

.address-block {
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.address-block h3 {
  margin: 7px 0 8px;
  font-size: 18px;
}

.address-block .el-button {
  margin-top: 12px;
}

.task-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.tone-green {
  color: #21613d;
  background: #dff2e1;
}

.tone-blue {
  color: #1f5877;
  background: #deefff;
}

.tone-amber {
  color: #8b5c19;
  background: #fbe8bc;
}

.tone-brown {
  color: #5b3710;
  background: #ead7bb;
}

.empty-state {
  padding: 16px;
  color: var(--subtle-ink);
  background: var(--cream);
  border: 1px dashed rgba(87, 58, 26, 0.18);
  border-radius: 8px;
}

.empty-state p {
  margin: 6px 0 0;
}

@media (max-width: 1180px) {
  .delivery-workbench {
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .requirement-aside {
    position: static;
    grid-column: 2;
  }
}

@media (max-width: 820px) {
  .page-head,
  .delivery-workbench,
  .inline-facts,
  .summary-list,
  .delivery-grid,
  .flow-steps {
    grid-template-columns: 1fr;
  }

  .entry-panel,
  .requirement-aside {
    position: static;
  }

  .requirement-aside {
    grid-column: auto;
  }

  .current-card,
  .card-head {
    display: grid;
  }

  .current-side {
    justify-items: start;
  }
}
</style>
