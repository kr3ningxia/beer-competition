<template>
  <div class="batch-payment-page">
    <header class="payment-heading brewer-card">
      <div>
        <span class="section-kicker">报名付款</span>
        <h1>{{ paid ? '报名已完成' : refunded ? '订单已退款' : bankPending ? '等待到账确认' : '完成统一付款' }}</h1>
        <p v-if="batch">{{ batch.competitionName }} · {{ batch.entryCount }} 款酒</p>
      </div>
      <RouterLink to="/portal/my">返回我的参赛</RouterLink>
    </header>

    <template v-if="batch">
      <main class="payment-layout">
        <section class="payment-main brewer-card">
          <div v-if="paid" class="success-panel">
            <el-icon><CircleCheckFilled /></el-icon>
            <div>
              <span>付款成功</span>
              <h2>{{ batch.entryCount }} 款酒已完成报名</h2>
              <p v-if="!autoRedirectCancelled">{{ redirectSeconds }} 秒后前往下载标签并填写送样信息</p>
              <p v-else>已取消自动跳转，可随时前往办理送样</p>
              <div class="success-actions">
                <el-button type="primary" @click="goToFulfillment">立即前往</el-button>
                <el-button v-if="!autoRedirectCancelled" text @click="cancelAutoRedirect">留在本页</el-button>
              </div>
            </div>
          </div>

          <div v-else-if="refunded" class="pending-panel">
            <el-icon><Clock /></el-icon>
            <div>
              <span>订单状态已更新</span>
              <h2>{{ orderStatus === 'REFUNDED' ? '本批报名已全部退款' : '本批报名已有酒款退款' }}</h2>
              <p>请返回我的参赛查看各款酒的最新状态</p>
            </div>
          </div>

          <template v-else-if="bankPending && organizerQrPending">
            <div class="pending-panel organizer-qr-pending">
              <el-icon><Clock /></el-icon>
              <div>
                <span>付款信息已提交</span>
                <h2>等待主办方确认</h2>
                <div v-if="editingOrganizerRemark" class="organizer-remark-editor">
                  <el-input
                    v-model.trim="organizerRemarkDraft"
                    maxlength="255"
                    placeholder="付款备注（选填）"
                  />
                  <div class="organizer-remark-actions">
                    <el-button type="primary" :loading="savingOrganizerRemark" @click="saveOrganizerRemark">保存备注</el-button>
                    <el-button text @click="cancelOrganizerRemarkEdit">取消</el-button>
                  </div>
                </div>
                <div v-else class="organizer-remark-row">
                  <p v-if="organizerPaymentRemark" class="submitted-remark">付款备注：{{ organizerPaymentRemark }}</p>
                  <button type="button" class="organizer-remark-action" @click="startOrganizerRemarkEdit">
                    {{ organizerPaymentRemark ? '修改备注' : '填写付款备注' }}
                  </button>
                </div>
                <p v-if="collectionConfig?.collectionNote" class="collection-note">
                  收款备注：{{ collectionConfig.collectionNote }}
                </p>
              </div>
            </div>
          </template>
          <template v-else-if="bankPending && !editingBankTransfer">
            <div class="pending-panel">
              <el-icon><Clock /></el-icon>
              <div>
                <span>付款信息已提交</span>
                <h2>等待组委会核对到账</h2>
                <p>确认到账后，这批酒款会一起完成报名</p>
                <el-button v-if="!organizerQrPending" class="pending-edit-button" @click="editBankTransfer">修改转账信息</el-button>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="method-head">
              <div>
                <span class="section-kicker">当前付款方式</span>
                <h2>{{ currentPayModeLabel }}</h2>
              </div>
              <el-dropdown v-if="!editingBankTransfer && availablePaymentMethods.length > 1" placement="bottom-end" trigger="click" @command="switchPayMode">
                <button class="change-method-button" type="button">
                  <span>更换付款方式</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="method in alternativePaymentMethods"
                      :key="method.value"
                      :command="method.value"
                    >
                      改用{{ method.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <section v-if="!availablePaymentMethods.length" class="pending-panel unavailable-payment">
              <div>
                <h2>暂未开放付款</h2>
                <p>请联系赛事主办方确认收款方式</p>
              </div>
            </section>
            <section v-else-if="payMode === 'WECHAT_QR'" class="wechat-panel">
              <div class="qr-side">
                <div class="qr-frame">
                  <img v-if="organizerQrUrl" :src="organizerQrUrl" width="220" height="220" alt="赛事收款码" />
                  <span v-else class="qr-loading">收款码加载中…</span>
                </div>
                <span class="qr-caption">请使用微信扫码付款</span>
              </div>
              <div class="wechat-copy">
                <span>本批应付</span><strong>{{ formatCurrency(batch.totalAmount) }}</strong>
                <p>{{ collectionConfig?.collectionNote || '请扫码完成付款后提交确认' }}</p>
                <el-input
                  v-model.trim="bankForm.remark"
                  class="organizer-payment-remark"
                  maxlength="255"
                  placeholder="付款备注（选填）"
                />
                <el-button class="organizer-payment-submit" type="primary" :loading="paying" @click="submitOrganizerPayment">我已完成付款</el-button>
              </div>
            </section>
            <section v-else-if="payMode === 'WECHAT'" class="wechat-panel">
              <div v-if="!isWechatPayEnv" class="qr-frame">
                <img v-if="qrDataUrl" :src="qrDataUrl" width="220" height="220" alt="微信支付二维码" />
                <div v-else class="qr-loading">正在生成支付码…</div>
              </div>
              <div v-else class="jsapi-mark">
                <el-icon><Iphone /></el-icon>
                <strong>微信内支付</strong>
                <span>确认金额后完成付款</span>
              </div>
              <div class="wechat-copy">
                <span>本批应付</span>
                <strong>{{ formatCurrency(batch.totalAmount) }}</strong>
                <p>{{ expireText }}</p>
                <el-button v-if="paymentOrder?.mode === 'MOCK'" type="primary" :loading="paying" @click="simulatePayment">
                  模拟微信到账
                </el-button>
                <el-button v-else-if="isWechatPayEnv && wechatRetryAvailable" type="primary" :loading="creatingPayment" @click="startWechatPayment">
                  重新打开微信支付
                </el-button>
                <el-button v-else :loading="creatingPayment" @click="createQr">重新生成支付码</el-button>
                <el-button text :loading="checking" @click="checkPayment">查看支付结果</el-button>
              </div>
            </section>

            <section v-else class="bank-panel">
              <dl class="bank-account">
                <div><dt>收款户名</dt><dd>{{ bankAccount?.accountName || '-' }}</dd></div>
                <div><dt>开户银行</dt><dd>{{ bankAccount?.bankName || '-' }}</dd></div>
                <div><dt>银行账号</dt><dd>{{ bankAccount?.accountNo || '-' }}</dd></div>
                <div><dt>转账金额</dt><dd>{{ formatCurrency(batch.totalAmount) }}</dd></div>
                <div v-if="bankAccount?.remarkTip"><dt>收款提示</dt><dd>{{ bankAccount.remarkTip }}</dd></div>
                <div v-if="paymentContactText"><dt>付款咨询</dt><dd>{{ paymentContactText }}</dd></div>
              </dl>
              <div class="bank-account-actions">
                <el-button @click="copyBankAccount">复制账户信息</el-button>
              </div>
              <el-form label-position="top" class="bank-form">
                <el-form-item label="转账备注（选填）">
                  <el-input v-model.trim="bankForm.remark" maxlength="255" placeholder="如需补充核对信息，可填写转账备注" />
                </el-form-item>
                <el-form-item label="付款凭证" required>
                  <label class="voucher-picker">
                    <input type="file" accept="image/jpeg,image/png,image/webp,application/pdf" @change="selectVoucher" />
                    <el-icon><Upload /></el-icon>
                    <span>{{ voucherFile?.name || bankVoucherName || '上传图片或 PDF，单个文件不超过 10MB' }}</span>
                  </label>
                </el-form-item>
                <el-button class="bank-submit" type="primary" :loading="submittingBank" @click="submitBankTransfer">
                  {{ editingBankTransfer ? '保存转账信息' : '提交转账信息' }}
                </el-button>
                <el-button v-if="editingBankTransfer" class="bank-cancel-edit" @click="cancelBankTransferEdit">取消修改</el-button>
              </el-form>
            </section>
          </template>
        </section>

        <aside class="order-summary brewer-card">
          <header>
            <span>付款明细</span>
            <b>{{ batch.entryCount }} 款</b>
          </header>
          <div class="order-list">
            <div v-for="(entry, index) in batch.entries" :key="entry.id">
              <span>{{ String(index + 1).padStart(2, '0') }}</span>
              <span><strong>{{ entry.name }}</strong><small>{{ entry.categoryName }} · {{ entry.style }}</small></span>
              <b>{{ formatCurrency(entry.payment?.amount) }}</b>
            </div>
          </div>
          <div class="order-total"><span>合计</span><strong>{{ formatCurrency(batch.totalAmount) }}</strong></div>
          <div class="summary-actions">
            <RouterLink v-if="paid" :to="fulfillmentLocation">下载标签并填写送样信息</RouterLink>
            <RouterLink v-else-if="bankPending || refunded" to="/portal/my">返回我的参赛</RouterLink>
            <RouterLink v-else to="/portal/my">稍后处理</RouterLink>
          </div>
        </aside>
      </main>
    </template>

    <section v-else class="loading-state brewer-card">{{ loading ? '正在读取付款信息…' : '未找到付款订单' }}</section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowDown, CircleCheckFilled, Clock, Iphone, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import {
  createPortalBatchWechatJsapiPayment,
  createPortalBatchWechatNativePayment,
  fetchPortalBankTransferAccount,
  fetchPortalBankTransfer,
  fetchPortalBatchPaymentStatus,
  fetchPortalEntryBatch,
  fetchPortalWechatPayClientConfig,
  simulatePortalBatchPayment,
  submitPortalBatchBankTransfer,
  submitPortalOrganizerPayment,
  fetchPortalCompetitionCollection,
  updatePortalBatchBankTransfer,
  updatePortalOrganizerPayment,
  uploadPortalBankTransferVoucher,
} from '@/api/portal'
import { buildWechatOauthUrl, currentUrlWithoutWechatCode, invokeWechatPay, isWechatBrowser } from '@/utils/wechatPay'
import { BASE_URL } from '@/config'

const route = useRoute()
const router = useRouter()
const requestedBatchId = Number(route.query.batchId || 0)
const orderId = Number(route.query.orderId || 0)
const batchId = ref(0)
const batch = ref(null)
const paymentStatus = ref(null)
const paymentOrder = ref(null)
const bankAccount = ref(null)
const collectionConfig = ref(null)
const qrDataUrl = ref('')
const wechatPayConfig = ref(null)
const wechatAuthCode = ref(String(route.query.code || ''))
const voucherFile = ref(null)
const bankVoucherAssetId = ref(null)
const bankVoucherName = ref('')
const loading = ref(true)
const creatingPayment = ref(false)
const checking = ref(false)
const paying = ref(false)
const submittingBank = ref(false)
const editingBankTransfer = ref(false)
const payMode = ref(route.query.payMode === 'bank_transfer' || route.query.payMode === 'bank' ? 'BANK_TRANSFER' : 'WECHAT')
const redirectSeconds = ref(3)
const autoRedirectCancelled = ref(false)
const wechatRetryAvailable = ref(false)
const bankForm = reactive({ remark: '' })
let pollingTimer = null
let redirectTimer = null
const currencyFormatter = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', minimumFractionDigits: 0, maximumFractionDigits: 2 })

const orderStatus = computed(() => paymentStatus.value?.status || batch.value?.paymentStatus || '')
const paid = computed(() => orderStatus.value === 'PAID')
const refunded = computed(() => ['PARTIALLY_REFUNDED', 'REFUNDED'].includes(orderStatus.value))
const bankPending = computed(() => orderStatus.value === 'PENDING_CONFIRM')
const organizerQrPending = computed(() => bankPending.value && paymentStatus.value?.payMethod === 'WECHAT_QR')
const organizerPaymentRemark = ref('')
const organizerRemarkDraft = ref('')
const editingOrganizerRemark = ref(false)
const savingOrganizerRemark = ref(false)
const organizerManaged = computed(() => collectionConfig.value?.tenantCompetition === true)
const availablePaymentMethods = computed(() => {
  if (!organizerManaged.value) {
    return [
      { value: 'WECHAT', label: '微信支付' },
      { value: 'BANK_TRANSFER', label: '银行转账' },
    ]
  }
  const enabledMethods = collectionConfig.value?.enabledMethods || []
  return [
    ...(enabledMethods.includes('WECHAT_QR') ? [{ value: 'WECHAT_QR', label: '微信收款' }] : []),
    ...(enabledMethods.includes('BANK_TRANSFER') ? [{ value: 'BANK_TRANSFER', label: '银行转账' }] : []),
  ]
})
const alternativePaymentMethods = computed(() => availablePaymentMethods.value.filter((method) => method.value !== payMode.value))
const currentPayModeLabel = computed(() => availablePaymentMethods.value.find((method) => method.value === payMode.value)?.label || '付款方式')
const organizerQrUrl = computed(() => {
  const value = collectionConfig.value?.wechatQrUrl || ''
  if (!value || /^https?:\/\//i.test(value) || value.startsWith('data:')) return value
  return `${BASE_URL}${value.startsWith('/') ? value : `/${value}`}`
})
const isWechatPayEnv = computed(() => isWechatBrowser())
const paymentContactText = computed(() => {
  if (organizerManaged.value) return collectionConfig.value?.paymentContact || ''
  return bankAccount.value?.serviceWechat ? `小秘书微信 ${bankAccount.value.serviceWechat}` : ''
})
const wechatPayAppId = computed(() => wechatPayConfig.value?.appId || '')
const fulfillmentEntry = computed(() => batch.value?.entries?.find((entry) => !hasDeliveryProgress(entry)) || batch.value?.entries?.[0] || null)
const fulfillmentLocation = computed(() => ({
  path: '/portal/fulfillment',
  query: {
    batchId: batchId.value,
    entryId: fulfillmentEntry.value?.id,
  },
}))
const expireText = computed(() => {
  const value = paymentOrder.value?.expireTime
  if (!value) return '请在支付码有效期内完成付款'
  return `${new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value))} 前完成付款`
})

onMounted(async () => {
  try {
    if (!orderId) return
    const statusData = await fetchPortalBatchPaymentStatus(orderId)
    if (requestedBatchId && Number(statusData.batchId) !== requestedBatchId) {
      throw new Error('付款订单与报名批次不一致')
    }
    batchId.value = Number(statusData.batchId || requestedBatchId)
    if (!batchId.value) throw new Error('付款订单缺少报名批次')
    paymentStatus.value = statusData
    const loadedBatch = await fetchPortalEntryBatch(batchId.value)
    const loadedCollectionConfig = await fetchPortalCompetitionCollection(loadedBatch.competitionId)
    batch.value = loadedBatch
    collectionConfig.value = loadedCollectionConfig
    if (paymentStatus.value?.payMethod === 'WECHAT_QR' && paymentStatus.value?.bankTransferId) {
      try {
        const transfer = await fetchPortalBankTransfer(paymentStatus.value.bankTransferId)
        organizerPaymentRemark.value = transfer?.remark || ''
      } catch {
        organizerPaymentRemark.value = ''
      }
    }
    if (bankPending.value) {
      startPolling()
    } else if (!paid.value && !refunded.value) {
      const requestedMode = payMode.value === 'BANK_TRANSFER'
        ? 'BANK_TRANSFER'
        : (organizerManaged.value ? 'WECHAT_QR' : 'WECHAT')
      payMode.value = availablePaymentMethods.value.some((method) => method.value === requestedMode)
        ? requestedMode
        : (availablePaymentMethods.value[0]?.value || '')
      if (payMode.value === 'BANK_TRANSFER') {
        await loadBankAccount()
      } else if (payMode.value === 'WECHAT') {
        await startWechatPayment({ silent: true })
      }
    }
  } catch (error) {
    ElMessage.warning(error?.message || '付款信息读取失败')
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  stopPolling()
  stopAutoRedirect()
})

watch([paid, batch], ([paymentPaid, currentBatch]) => {
  if (paymentPaid && currentBatch) scheduleAutoRedirect()
})

async function switchPayMode(mode) {
  if (!availablePaymentMethods.value.some((method) => method.value === mode)) return
  payMode.value = mode
  if (mode === 'BANK_TRANSFER') {
    stopPolling()
    await loadBankAccount()
    return
  }
  if (mode === 'WECHAT') await startWechatPayment({ silent: true })
}

async function submitOrganizerPayment() {
  if (paying.value || paid.value) return
  paying.value = true
  try {
    const transfer = await submitPortalOrganizerPayment(orderId, { remark: bankForm.remark || '' })
    organizerPaymentRemark.value = transfer?.remark || ''
    paymentStatus.value = await fetchPortalBatchPaymentStatus(orderId)
    batch.value = await fetchPortalEntryBatch(batchId.value)
    startPolling()
    ElMessage.success('付款信息已提交，等待赛事主办方确认')
  } catch (error) {
    ElMessage.warning(error?.message || '付款信息提交失败')
  } finally {
    paying.value = false
  }
}

async function createQr() {
  if (creatingPayment.value || paid.value) return
  creatingPayment.value = true
  try {
    paymentOrder.value = await createPortalBatchWechatNativePayment(orderId)
    if (paymentOrder.value?.codeUrl) {
      qrDataUrl.value = await QRCode.toDataURL(paymentOrder.value.codeUrl, { errorCorrectionLevel: 'M', margin: 1, width: 220 })
      startPolling()
    }
  } catch (error) {
    ElMessage.warning(error?.message || '支付码生成失败')
  } finally {
    creatingPayment.value = false
  }
}

async function startWechatPayment(options = {}) {
  if (isWechatPayEnv.value) {
    await startJsapiPayment(options)
    return
  }
  await createQr()
}

async function startJsapiPayment(options = {}) {
  if (creatingPayment.value || paid.value) return
  wechatRetryAvailable.value = false
  await loadWechatPayConfig()
  if (wechatPayConfig.value?.jsapiConfigured === false || (!wechatPayAppId.value && wechatPayConfig.value?.mode !== 'MOCK')) {
    wechatRetryAvailable.value = true
    if (!options.silent) ElMessage.warning('微信支付暂不可用，请改用银行转账或稍后再试')
    return
  }
  if (!wechatAuthCode.value && wechatPayConfig.value?.mode !== 'MOCK') {
    window.location.href = buildWechatOauthUrl(wechatPayAppId.value)
    return
  }
  creatingPayment.value = true
  try {
    paymentOrder.value = await createPortalBatchWechatJsapiPayment(orderId, {
      code: wechatAuthCode.value || 'mock-code',
    })
    clearWechatAuthCode()
    if (paymentOrder.value?.mode === 'MOCK') return
    if (paymentOrder.value?.paymentStatus === 'PAID') {
      await checkPayment({ silent: true })
      return
    }
    if (!paymentOrder.value?.payParams) throw new Error('微信支付参数缺失')
    await invokeWechatPay(paymentOrder.value.payParams)
    startPolling()
    await checkPayment({ silent: true })
    wechatRetryAvailable.value = !paid.value
  } catch (error) {
    clearWechatAuthCode()
    wechatRetryAvailable.value = true
    if (!options.silent) ElMessage.warning(error?.message || '微信支付未完成，请稍后重试')
  } finally {
    creatingPayment.value = false
  }
}

async function loadWechatPayConfig() {
  if (wechatPayConfig.value) return wechatPayConfig.value
  wechatPayConfig.value = await fetchPortalWechatPayClientConfig()
  return wechatPayConfig.value
}

function clearWechatAuthCode() {
  if (!wechatAuthCode.value) return
  wechatAuthCode.value = ''
  window.history.replaceState(null, '', currentUrlWithoutWechatCode())
}

async function checkPayment(options = {}) {
  if (checking.value || paid.value) return
  checking.value = true
  try {
    paymentStatus.value = await fetchPortalBatchPaymentStatus(orderId)
    if (paid.value) {
      batch.value = await fetchPortalEntryBatch(batchId.value)
      stopPolling()
      if (!options.silent) ElMessage.success('付款成功，报名已完成')
    }
  } catch (error) {
    if (!options.silent) ElMessage.warning(error?.message || '支付结果查询失败')
  } finally {
    checking.value = false
  }
}

async function simulatePayment() {
  paying.value = true
  try {
    paymentStatus.value = await simulatePortalBatchPayment(orderId)
    batch.value = await fetchPortalEntryBatch(batchId.value)
    stopPolling()
    ElMessage.success('付款成功，报名已完成')
  } catch (error) {
    ElMessage.warning(error?.message || '模拟付款失败')
  } finally {
    paying.value = false
  }
}

async function loadBankAccount() {
  if (bankAccount.value) return
  if (organizerManaged.value && collectionConfig.value?.enabledMethods?.includes('BANK_TRANSFER')) {
    bankAccount.value = {
      accountName: collectionConfig.value.bankAccountName,
      bankName: collectionConfig.value.bankName,
      accountNo: collectionConfig.value.bankAccountNo,
      remarkTip: collectionConfig.value.collectionNote || '请在转账备注中填写厂牌名',
    }
    return
  }
  bankAccount.value = await fetchPortalBankTransferAccount()
}

function selectVoucher(event) {
  voucherFile.value = event.target.files?.[0] || null
}

async function copyBankAccount() {
  const account = bankAccount.value
  if (!account) return
  const text = [
    `收款户名：${account.accountName || ''}`,
    `开户银行：${account.bankName || ''}`,
    `银行账号：${account.accountNo || ''}`,
    account.remarkTip ? `收款提示：${account.remarkTip}` : '',
    paymentContactText.value ? `付款咨询：${paymentContactText.value}` : '',
  ].filter(Boolean).join('\n')
  await navigator.clipboard.writeText(text)
  ElMessage.success('收款账户信息已复制')
}

async function submitBankTransfer() {
  if (submittingBank.value) return
  if (!voucherFile.value && !bankVoucherAssetId.value) {
    ElMessage.warning('请上传付款凭证')
    return
  }
  submittingBank.value = true
  try {
    const wasEditing = editingBankTransfer.value
    let voucherAssetId = bankVoucherAssetId.value
    if (voucherFile.value) {
      const uploaded = await uploadPortalBankTransferVoucher(voucherFile.value)
      voucherAssetId = uploaded.fileAssetId
    }
    const payload = { ...bankForm, voucherAssetId }
    if (editingBankTransfer.value) await updatePortalBatchBankTransfer(orderId, payload)
    else await submitPortalBatchBankTransfer(orderId, payload)
    paymentStatus.value = await fetchPortalBatchPaymentStatus(orderId)
    batch.value = await fetchPortalEntryBatch(batchId.value)
    editingBankTransfer.value = false
    bankVoucherAssetId.value = null
    bankVoucherName.value = ''
    if (bankPending.value) startPolling()
    ElMessage.success(wasEditing ? '转账信息已更新' : '转账信息已提交')
  } catch (error) {
    paymentStatus.value = await fetchPortalBatchPaymentStatus(orderId).catch(() => paymentStatus.value)
    if (!error?.userNotified) ElMessage.warning(error?.message || '转账信息提交失败，请稍后重试')
  } finally {
    submittingBank.value = false
  }
}

async function editBankTransfer() {
  const transferId = paymentStatus.value?.bankTransferId
  if (!transferId) {
    ElMessage.warning('暂时无法读取转账信息')
    return
  }
  try {
    const transfer = await fetchPortalBankTransfer(transferId)
    await loadBankAccount()
    payMode.value = 'BANK_TRANSFER'
    bankForm.remark = transfer.remark || ''
    bankVoucherAssetId.value = transfer.voucherAssetId || null
    bankVoucherName.value = transfer.voucherFileName || ''
    voucherFile.value = null
    editingBankTransfer.value = true
  } catch (error) {
    ElMessage.warning(error?.message || '转账信息读取失败')
  }
}

function cancelBankTransferEdit() {
  editingBankTransfer.value = false
  bankVoucherAssetId.value = null
  bankVoucherName.value = ''
}

function startOrganizerRemarkEdit() {
  organizerRemarkDraft.value = organizerPaymentRemark.value
  editingOrganizerRemark.value = true
}

function cancelOrganizerRemarkEdit() {
  editingOrganizerRemark.value = false
  organizerRemarkDraft.value = ''
}

async function saveOrganizerRemark() {
  if (savingOrganizerRemark.value) return
  savingOrganizerRemark.value = true
  try {
    const transfer = await updatePortalOrganizerPayment(orderId, { remark: organizerRemarkDraft.value || '' })
    organizerPaymentRemark.value = transfer?.remark || organizerRemarkDraft.value || ''
    editingOrganizerRemark.value = false
    organizerRemarkDraft.value = ''
    ElMessage.success('付款备注已更新')
  } catch (error) {
    ElMessage.warning(error?.message || '付款备注更新失败')
  } finally {
    savingOrganizerRemark.value = false
  }
}

function startPolling() {
  stopPolling()
  pollingTimer = window.setInterval(() => checkPayment({ silent: true }), 3000)
}

function stopPolling() {
  if (pollingTimer) window.clearInterval(pollingTimer)
  pollingTimer = null
}

function scheduleAutoRedirect() {
  if (redirectTimer) return
  autoRedirectCancelled.value = false
  redirectSeconds.value = 3
  redirectTimer = window.setInterval(() => {
    redirectSeconds.value -= 1
    if (redirectSeconds.value <= 0) goToFulfillment()
  }, 1000)
}

function stopAutoRedirect() {
  if (redirectTimer) window.clearInterval(redirectTimer)
  redirectTimer = null
}

function cancelAutoRedirect() {
  stopAutoRedirect()
  autoRedirectCancelled.value = true
}

async function goToFulfillment() {
  stopAutoRedirect()
  await router.replace(fulfillmentLocation.value)
}

function hasDeliveryProgress(entry) {
  return ['SUBMITTED', 'RECEIVED'].includes(entry?.deliveryStatus)
    || Boolean(entry?.deliverySubmittedAt || entry?.trackingNo || entry?.deliveryMethod)
}

function formatCurrency(value) {
  return currencyFormatter.format(Number(value || 0))
}
</script>

<style scoped>
.batch-payment-page { display: grid; gap: 18px; }
.payment-heading { display: flex; justify-content: space-between; gap: 20px; align-items: center; padding: 22px 24px; }
.payment-heading h1 { margin: 4px 0; color: #2b1d10; font-size: 26px; }
.payment-heading p { margin: 0; color: #756552; }
.payment-heading a { color: #80500f; font-weight: 800; text-decoration: none; }
.section-kicker { color: #96611b; font-size: 11px; font-weight: 900; letter-spacing: .08em; }
.payment-layout { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 18px; align-items: start; }
.payment-main, .order-summary { padding: 24px; }
.method-head { display: flex; justify-content: space-between; gap: 18px; align-items: center; }
.method-head h2 { margin: 5px 0 0; color: #2b1d10; }
.change-method-button { display: inline-flex; gap: 5px; align-items: center; padding: 7px 2px; color: #806c55; background: transparent; border: 0; cursor: pointer; font-weight: 700; }
.change-method-button:hover { color: #80500f; }
.change-method-button:focus-visible { color: #80500f; outline: 2px solid rgba(174, 111, 25, .35); outline-offset: 3px; border-radius: 3px; }
.wechat-panel { display: flex; justify-content: center; gap: 42px; align-items: center; min-height: 330px; margin-top: 20px; padding: 30px 32px; background: #fff8e8; border: 1px solid rgba(87,58,26,.12); border-radius: 8px; }
.qr-side { display: grid; justify-items: center; gap: 10px; flex: 0 0 240px; }
.qr-frame { display: grid; width: 240px; aspect-ratio: 1; height: auto; min-width: 0; place-items: center; overflow: hidden; padding: 9px; background: #fff; border: 1px solid rgba(87,58,26,.14); border-radius: 8px; box-sizing: border-box; box-shadow: 0 8px 18px rgba(87,58,26,.08); }
.qr-frame img { display: block; width: 100%; height: 100%; object-fit: contain; }
.qr-caption { color: #806c55; font-size: 13px; font-weight: 700; }
.qr-loading { color: #8a7864; }
.jsapi-mark { display: grid; width: 210px; min-height: 210px; place-items: center; align-content: center; gap: 10px; color: #2f6f46; background: #fff; border: 1px solid rgba(87,58,26,.14); border-radius: 8px; text-align: center; }
.jsapi-mark .el-icon { font-size: 52px; }
.jsapi-mark span { color: #806c55; font-size: 13px; }
.wechat-copy { display: grid; gap: 10px; width: 100%; min-width: 0; box-sizing: border-box; }
.wechat-copy > span { color: #806c55; font-size: 13px; }
.wechat-copy > strong { color: #744709; font-size: 34px; font-variant-numeric: tabular-nums; }
.wechat-copy p { margin: 0 0 8px; color: #74624d; }
.organizer-payment-remark { max-width: 420px; }
.organizer-payment-submit { width: 100%; max-width: 420px; min-height: 46px; margin-top: 4px; background: #875515; border: 0; font-weight: 900; }
.bank-panel { margin-top: 20px; }
.bank-account { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 1px; overflow: hidden; background: rgba(87,58,26,.12); border: 1px solid rgba(87,58,26,.12); border-radius: 8px; }
.bank-account div { padding: 13px 15px; background: #fff8e8; }
.bank-account dt { color: #8a7761; font-size: 12px; }
.bank-account dd { margin: 5px 0 0; color: #342518; font-weight: 800; }
.bank-account-actions { margin-top: 12px; }
.bank-form { margin-top: 20px; }
.voucher-picker { display: flex; align-items: center; gap: 9px; width: 100%; padding: 12px 14px; color: #6f573e; background: #fffdf8; border: 1px dashed rgba(87,58,26,.25); border-radius: 7px; cursor: pointer; }
.voucher-picker input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.bank-submit { width: 100%; min-height: 46px; background: #875515; border: 0; font-weight: 900; }
.success-panel, .pending-panel { display: flex; gap: 22px; align-items: center; min-height: 330px; justify-content: center; }
.unavailable-payment { text-align: center; }
.success-panel > .el-icon, .pending-panel > .el-icon { width: 68px; height: 68px; font-size: 68px; }
.success-panel > .el-icon { color: #3b7a4f; }
.pending-panel > .el-icon { color: #ad721e; }
.success-panel h2, .pending-panel h2 { margin: 6px 0; color: #2b1d10; font-size: 25px; }
.success-panel p, .pending-panel p { margin: 0; color: #76644f; }
.success-actions { display: flex; gap: 8px; margin-top: 16px; }
.pending-edit-button { margin-top: 16px; }
.organizer-qr-pending .submitted-remark { color: #80500f; font-weight: 800; }
.organizer-qr-pending .collection-note { margin-top: 8px; font-size: 13px; }
.organizer-remark-row { display: flex; align-items: baseline; gap: 12px; margin-top: 12px; }
.organizer-remark-row .submitted-remark { min-width: 0; }
.organizer-remark-action { flex: none; padding: 0; color: #96611b; background: transparent; border: 0; cursor: pointer; font-size: 13px; font-weight: 800; white-space: nowrap; }
.organizer-remark-action:hover { color: #80500f; text-decoration: underline; text-underline-offset: 3px; }
.organizer-remark-action:focus-visible { color: #80500f; outline: 2px solid rgba(174, 111, 25, .35); outline-offset: 3px; border-radius: 3px; }
.organizer-remark-editor { display: grid; gap: 10px; max-width: 420px; margin-top: 14px; }
.organizer-remark-actions { display: flex; gap: 8px; }
.bank-cancel-edit { width: 100%; margin-top: 10px; }
.order-summary { position: sticky; top: 116px; }
.order-summary > header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 14px; border-bottom: 1px solid rgba(87,58,26,.12); }
.order-summary > header span { color: #2b1d10; font-size: 19px; font-weight: 900; }
.order-summary > header b { color: #86540f; }
.order-list { max-height: 360px; overflow-y: auto; }
.order-list > div { display: grid; grid-template-columns: 25px minmax(0,1fr) auto; gap: 9px; align-items: center; padding: 12px 0; border-bottom: 1px solid rgba(87,58,26,.1); }
.order-list > div > span:nth-child(2) { display: grid; gap: 3px; min-width: 0; }
.order-list strong, .order-list small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-list small { color: #887561; }
.order-total { display: flex; justify-content: space-between; align-items: baseline; padding: 18px 0; }
.order-total strong { color: #744709; font-size: 27px; font-variant-numeric: tabular-nums; }
.summary-actions a { display: flex; min-height: 42px; align-items: center; justify-content: center; color: #fff; background: #875515; border-radius: 7px; font-weight: 900; text-decoration: none; }
.loading-state { padding: 80px; text-align: center; }
@media (max-width: 1000px) { .payment-layout { grid-template-columns: 1fr; } .order-summary { position: static; } }
@media (max-width: 700px) { .payment-heading, .method-head, .wechat-panel { align-items: flex-start; flex-direction: column; } .wechat-panel { width: 100%; box-sizing: border-box; gap: 24px; } .qr-side { align-self: center; flex-basis: auto; width: min(100%, 240px); } .qr-frame { width: 100%; } .bank-account { grid-template-columns: 1fr; } }
</style>
