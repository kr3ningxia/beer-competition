<template>
  <div class="payment-page">
    <section class="entry-panel brewer-card">
      <div class="panel-head">
        <div>
          <h2 class="portal-section-title">完成付款并提交寄样</h2>
          <p>按酒款逐一处理付款确认、标签下载和快递信息提交，避免漏寄或贴错标签。</p>
        </div>
        <RouterLink to="/portal/my">返回我的参赛</RouterLink>
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
              <b v-if="entry.paymentStatus !== 'PAID'">{{ formatCurrency(entry.entryFee) }}</b>
            </div>
            <p>{{ competitionLabel(entry) }}</p>
          </div>
          <div class="entry-row-side">
            <span :class="['task-chip', taskTone(entry)]">{{ entryTaskLabel(entry) }}</span>
          </div>
        </article>

        <div v-if="!payableEntries.length" class="empty-state">
          <strong>暂无报名酒款</strong>
          <p>提交报名后，这里会显示每款酒的付款、标签和寄样进度。</p>
        </div>
      </div>
    </section>

    <aside v-if="selectedEntry" class="workspace-panel">
      <section class="hero-card brewer-card">
        <div class="hero-copy">
          <span class="section-kicker">当前酒款</span>
          <h3>{{ selectedEntry.name }}</h3>
          <p>{{ entryMetaLine(selectedEntry) }}</p>
        </div>
        <div class="hero-side">
          <span :class="['task-chip', taskTone(selectedEntry)]">{{ entryTaskLabel(selectedEntry) }}</span>
          <strong v-if="selectedEntry.paymentStatus !== 'PAID'">{{ formatCurrency(selectedEntry.entryFee) }}</strong>
        </div>
      </section>

      <section class="next-card brewer-card">
        <div>
          <span class="section-kicker">当前待办</span>
          <h3>{{ currentAction.title }}</h3>
          <p>{{ currentAction.description }}</p>
        </div>
        <span :class="['task-chip', taskTone(selectedEntry)]">{{ entryTaskLabel(selectedEntry) }}</span>
      </section>

      <div class="workspace-grid">
        <section v-if="showPaymentCard" class="payment-card brewer-card full-span">
          <div class="card-head">
            <div>
              <span class="section-kicker">付款确认</span>
              <h3>先完成这款酒的付款确认</h3>
              <p>主办方确认到账后，这一款酒才会开放标签下载和寄样信息填写。</p>
            </div>
          </div>

          <dl class="summary-grid compact-grid">
            <div>
              <dt>报名费</dt>
              <dd>{{ formatCurrency(selectedEntry.payment?.amount || selectedEntry.entryFee) }}</dd>
            </div>
            <div>
              <dt>付款方式</dt>
              <dd>{{ paymentMethodText(selectedEntry.payment?.payMethod) }}</dd>
            </div>
          </dl>
        </section>

        <section v-if="showPreparationCards" class="label-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">下载并打印标签</span>
              <h3>寄样前请先贴好标签</h3>
              <p>建议优先使用打印版，贴在酒瓶或外箱醒目位置，便于主办方核对和入库。</p>
            </div>
            <span :class="['task-chip', selectedEntry.canDownloadLabel ? 'tone-green' : 'tone-amber']">
              {{ selectedEntry.canDownloadLabel ? '可下载' : '等待付款确认' }}
            </span>
          </div>

          <div class="label-layout">
            <div class="label-preview" v-html="labelSvg" />

            <div class="label-actions">
              <div class="label-action-copy">
                <strong>{{ selectedEntry.canDownloadLabel ? '标签已开放' : '付款确认后自动开放' }}</strong>
                <p>{{ selectedEntry.canDownloadLabel ? '打印后直接贴标，再回到下方填写寄样方式和快递单号。' : '如果你已经完成转账或现场付款，请等待主办方确认。' }}</p>
              </div>
              <div class="button-row">
                <el-button
                  type="primary"
                  :disabled="!selectedEntry.canDownloadLabel"
                  @click="downloadLabelPng"
                >
                  下载 PNG
                </el-button>
                <el-button
                  :disabled="!selectedEntry.canDownloadLabel"
                  @click="openPrintLabel"
                >
                  打印标签 / 保存 PDF
                </el-button>
              </div>
              <p class="label-tip">整箱寄送时，建议外箱也贴 1 张标签，收样时更容易核对。</p>
            </div>
          </div>
        </section>

        <section v-if="showPreparationCards" class="requirements-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">寄样要求</span>
              <h3>寄出前请核对这几项</h3>
            </div>
          </div>

          <dl class="requirement-list">
            <div>
              <dt>赛事信息</dt>
              <dd>{{ selectedEntry.competitionName || competitionName(selectedEntry.competitionId) || '当前赛事' }}</dd>
            </div>
            <div>
              <dt>建议送达时间</dt>
              <dd>{{ suggestedArrivalText }}</dd>
            </div>
            <div>
              <dt>标签粘贴</dt>
              <dd>每款酒至少贴 1 张，整箱寄送时建议外箱再贴 1 张。</dd>
            </div>
            <div>
              <dt>包装检查</dt>
              <dd>请确认防震、防漏和外箱加固，避免运输中破损。</dd>
            </div>
            <div>
              <dt>快递信息</dt>
              <dd>寄出后请尽快回填快递公司和单号，方便主办方签收。</dd>
            </div>
            <div>
              <dt>地址与联系人</dt>
              <dd>如页面外的赛事通知尚未提供收件信息，请先联系主办方确认后再寄送。</dd>
            </div>
          </dl>
        </section>

        <section v-if="showDeliveryCard" class="delivery-card brewer-card full-span">
          <div class="card-head">
            <div>
              <span class="section-kicker">填写快递信息</span>
              <h3>{{ deliveryCardTitle }}</h3>
              <p>{{ deliveryCardDescription }}</p>
            </div>
            <span :class="['task-chip', deliveryTone(selectedEntry.deliveryStatus)]">
              {{ deliveryStatusText(selectedEntry.deliveryStatus) }}
            </span>
          </div>

          <div v-if="showDeliverySummary" class="delivery-summary">
            <dl class="summary-grid">
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
                修改快递信息
              </el-button>
              <el-button @click="refreshEntries(selectedEntry.id)">刷新状态</el-button>
            </div>
          </div>

          <el-form v-else label-position="top" class="delivery-form">
            <div class="delivery-grid">
              <el-form-item label="送样方式">
                <el-radio-group v-model="deliveryForm.deliveryMethod">
                  <el-radio-button label="EXPRESS">快递寄送</el-radio-button>
                  <el-radio-button label="ONSITE">现场送样</el-radio-button>
                </el-radio-group>
                <p class="field-tip">如果你准备现场交样，也建议备注到场时间，方便主办方安排签收。</p>
              </el-form-item>

              <el-form-item v-if="deliveryForm.deliveryMethod === 'EXPRESS'" label="快递公司">
                <el-input
                  v-model.trim="deliveryForm.carrier"
                  placeholder="例如：顺丰、京东快递"
                />
              </el-form-item>

              <el-form-item v-if="deliveryForm.deliveryMethod === 'EXPRESS'" label="快递单号">
                <el-input
                  v-model.trim="deliveryForm.trackingNo"
                  placeholder="填写本次寄样的快递单号"
                />
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
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const editingDelivery = ref(false)
const savingDelivery = ref(false)
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

const hasSubmittedDelivery = computed(() => hasDeliveryProgress(selectedEntry.value))
const showPaymentCard = computed(() => selectedEntry.value?.paymentStatus !== 'PAID')
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
  shortCode: labelData.value?.shortCode || selectedEntry.value?.shortCode || '',
  categoryName: labelData.value?.categoryName || selectedEntry.value?.categoryName || '',
  style: labelData.value?.style || selectedEntry.value?.style || '',
  abv: labelData.value?.abv ?? selectedEntry.value?.abv ?? '',
}))

const labelSvg = computed(() => buildLabelSvg(labelRecord.value))

const suggestedArrivalText = computed(() => {
  if (!selectedEntry.value?.competitionDate) return '请至少预留 2 到 3 天运输时间，具体以主办方通知为准'
  return `${formatDate(selectedEntry.value.competitionDate)} 前送达更稳妥`
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
      title: '等待主办方确认付款',
      description: '你完成转账或现场付款后，主办方确认到账，这一款酒才会开放标签下载和寄样填写。',
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
  if (!selectedEntry.value?.canDownloadLabel) return '付款确认后可填写寄样信息'
  if (showDeliverySummary.value) return selectedEntry.value.deliveryStatus === 'RECEIVED' ? '样品已签收' : '你已提交寄样信息'
  return hasSubmittedDelivery.value ? '修改寄样信息' : '填写寄样信息'
})

const deliveryCardDescription = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return '当前阶段先等待主办方确认付款，确认完成后这里会自动开放。'
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
  if (value === 'MANUAL') return '线下付款确认'
  if (value === 'WECHAT') return '微信支付'
  return '待主办方确认'
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
  return '待付款确认'
}

function isStored(entry) {
  return entry?.stored || entry?.storedFlag === 1 || entry?.status === 'STORED' || entry?.status === 'RESULT_PUBLISHED'
}

function hasDeliveryProgress(entry) {
  if (!entry) return false
  return ['SUBMITTED', 'RECEIVED'].includes(entry.deliveryStatus) || Boolean(entry.deliverySubmittedAt || entry.trackingNo || entry.deliveryMethod)
}

function syncDeliveryForm(entry) {
  deliveryForm.deliveryMethod = entry?.deliveryMethod || 'EXPRESS'
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
    deliveryMethod: source.deliveryMethod || 'EXPRESS',
    carrier: source.deliveryMethod === 'EXPRESS' ? (source.carrier || '').trim() : '',
    trackingNo: source.deliveryMethod === 'EXPRESS' ? (source.trackingNo || '').trim() : '',
    deliveryNote: (source.deliveryNote || '').trim(),
  }
}

function entryDeliverySnapshot(entry) {
  return normalizeDeliveryPayload({
    deliveryMethod: entry?.deliveryMethod || 'EXPRESS',
    carrier: entry?.carrier || '',
    trackingNo: entry?.trackingNo || '',
    deliveryNote: entry?.deliveryNote || '',
  })
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
    ElMessage.warning('付款确认后才能填写寄样信息')
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
  const matrix = buildMatrix(`${label.uuid}-${label.shortCode}`)
  const cells = []
  const size = 10
  const offsetX = 34
  const offsetY = 80

  for (let row = 0; row < matrix.length; row += 1) {
    for (let col = 0; col < matrix[row].length; col += 1) {
      if (!matrix[row][col]) continue
      cells.push(`<rect x="${offsetX + col * size}" y="${offsetY + row * size}" width="${size}" height="${size}" rx="1.5" fill="#2e2014" />`)
    }
  }

  const category = escapeXml(label.categoryName || '待确认组别')
  const style = escapeXml(label.style || 'Style Pending')
  const abv = label.abv !== '' && label.abv !== null && label.abv !== undefined ? `${label.abv}%` : 'ABV Pending'
  const code = escapeXml(label.shortCode || 'PENDING')
  const uuid = escapeXml(label.uuid || '等待开放标签')

  return `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 260 360" role="img" aria-label="现场评审标签">
      <rect width="260" height="360" rx="24" fill="#fff8ec"/>
      <rect x="14" y="14" width="232" height="332" rx="18" fill="#fffdf9" stroke="#d4bf9f"/>
      <text x="130" y="44" text-anchor="middle" font-size="12" font-weight="700" letter-spacing="1.4" fill="#8c6330">现场评审标签</text>
      <text x="130" y="66" text-anchor="middle" font-size="22" font-weight="800" fill="#24170f">${uuid}</text>
      <rect x="28" y="74" width="204" height="204" rx="18" fill="#f7ecd8" stroke="#3a2818" stroke-width="10"/>
      ${cells.join('')}
      <text x="130" y="302" text-anchor="middle" font-size="12" font-weight="700" fill="#8c6330">现场短编号</text>
      <text x="130" y="326" text-anchor="middle" font-size="24" font-weight="900" fill="#24170f">${code}</text>
      <text x="130" y="344" text-anchor="middle" font-size="13" fill="#665647">${category} · ${style} · ${abv}</text>
    </svg>
  `
}

function buildMatrix(seed) {
  const size = 17
  const matrix = Array.from({ length: size }, () => Array(size).fill(false))

  drawFinder(matrix, 0, 0)
  drawFinder(matrix, 0, size - 5)
  drawFinder(matrix, size - 5, 0)

  let hash = 0
  for (const char of seed || 'label') {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  }

  for (let row = 0; row < size; row += 1) {
    for (let col = 0; col < size; col += 1) {
      if (inFinder(row, col, size)) continue
      hash = (hash * 1664525 + 1013904223) >>> 0
      matrix[row][col] = (hash & 3) === 0
    }
  }

  return matrix
}

function drawFinder(matrix, rowStart, colStart) {
  for (let row = rowStart; row < rowStart + 5; row += 1) {
    for (let col = colStart; col < colStart + 5; col += 1) {
      const onBorder = row === rowStart || row === rowStart + 4 || col === colStart || col === colStart + 4
      const center = row >= rowStart + 1 && row <= rowStart + 3 && col >= colStart + 1 && col <= colStart + 3
      matrix[row][col] = onBorder || center
    }
  }
}

function inFinder(row, col, size) {
  const topLeft = row < 5 && col < 5
  const topRight = row < 5 && col >= size - 5
  const bottomLeft = row >= size - 5 && col < 5
  return topLeft || topRight || bottomLeft
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
  --subtle-ink: #74624f;
  --line: rgba(103, 72, 39, 0.12);
  --cream: #fff9ef;
  --cream-deep: #f8ecd7;
  --paper: #fffdf9;
  --accent: #b9781f;
  display: grid;
  grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.entry-panel,
.hero-card,
.next-card,
.steps-card,
.label-card,
.requirements-card,
.delivery-card {
  padding: 24px;
}

.workspace-panel {
  display: grid;
  gap: 16px;
}

.panel-head,
.card-head,
.hero-card,
.next-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.panel-head p,
.card-head p,
.hero-copy p,
.next-card p,
.entry-row p,
.field-tip,
.label-tip {
  color: var(--subtle-ink);
  line-height: 1.65;
}

.panel-head a {
  color: #8b5c19;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.payment-card,
.entry-list-note,
.summary-grid div,
.requirement-list div {
  background: var(--cream);
  border: 1px solid var(--line);
  border-radius: 14px;
}

.payment-card,
.summary-grid dt,
.requirement-list dt {
  display: block;
  color: #8a755c;
  font-size: 12px;
}

.entry-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.entry-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 16px;
  cursor: pointer;
  background: linear-gradient(180deg, #fffaf1 0%, #fff6e7 100%);
  border: 1px solid var(--line);
  border-radius: 16px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.entry-row:hover {
  transform: translateY(-1px);
  border-color: rgba(121, 82, 40, 0.24);
  box-shadow: 0 12px 28px rgba(81, 56, 26, 0.08);
}

.entry-row.active {
  background: linear-gradient(180deg, #2f2116 0%, #43301f 100%);
  border-color: rgba(255, 230, 193, 0.18);
  box-shadow: 0 16px 34px rgba(47, 33, 22, 0.24);
}

.entry-row.active strong,
.entry-row.active b,
.hero-side strong,
.summary-grid dd,
.requirement-list dd {
  color: #fff8ee;
}

.entry-row.active p {
  color: #ddc8a8;
}

.entry-row-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
}

.entry-row-top strong,
.entry-row-top b {
  color: var(--ink);
}

.entry-row-top strong {
  font-size: 18px;
}

.entry-row p {
  margin: 8px 0 4px;
}

.entry-row small {
  display: block;
}

.entry-row-side {
  display: grid;
  justify-items: end;
  align-content: start;
}

.section-kicker {
  display: block;
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-card h3,
.next-card h3,
.card-head h3 {
  margin: 10px 0 8px;
  color: var(--ink);
  font-size: 28px;
  line-height: 1.25;
}

.hero-side {
  min-width: 160px;
  display: grid;
  gap: 8px;
  justify-items: end;
  align-content: start;
  text-align: right;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 16px;
}

.full-span {
  grid-column: 1 / -1;
}

.label-card,
.requirements-card,
.delivery-card {
  background:
    linear-gradient(180deg, rgba(255, 253, 249, 0.98) 0%, rgba(255, 248, 236, 0.98) 100%);
}

.label-layout {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
  margin-top: 18px;
}

.label-preview {
  min-height: 360px;
  padding: 18px;
  background: linear-gradient(180deg, #f6ebd7 0%, #fff7ea 100%);
  border: 1px solid rgba(102, 70, 36, 0.12);
  border-radius: 18px;
}

.label-preview :deep(svg) {
  display: block;
  width: 100%;
  height: auto;
}

.label-actions {
  display: grid;
  gap: 16px;
}

.label-action-copy strong {
  color: var(--ink);
  font-size: 18px;
}

.label-tip {
  margin: 0;
  font-size: 13px;
}

.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.requirement-list,
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0 0;
}

.requirement-list div,
.summary-grid div {
  padding: 16px;
}

.requirement-list dd,
.summary-grid dd {
  margin: 8px 0 0;
  font-weight: 700;
  line-height: 1.6;
  color: var(--ink);
}

.compact-grid {
  margin-top: 18px;
}

.delivery-form {
  margin-top: 18px;
}

.delivery-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.full-width {
  grid-column: 1 / -1;
}

.field-tip {
  margin: 8px 0 0;
  font-size: 12px;
}

.delivery-summary {
  display: grid;
  gap: 18px;
  margin-top: 18px;
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
  padding: 18px;
  color: var(--subtle-ink);
  background: var(--cream);
  border: 1px dashed rgba(87, 58, 26, 0.18);
  border-radius: 14px;
}

@media (max-width: 1280px) {
  .payment-page,
  .workspace-grid,
  .label-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .panel-head,
  .card-head,
  .hero-card,
  .next-card,
  .entry-row-top {
    display: grid;
  }

  .panel-brief,
  .requirement-list,
  .summary-grid,
  .delivery-grid {
    grid-template-columns: 1fr;
  }

  .entry-row {
    grid-template-columns: 1fr;
  }

  .entry-row-side,
  .hero-side {
    justify-items: start;
    text-align: left;
  }
}
</style>
