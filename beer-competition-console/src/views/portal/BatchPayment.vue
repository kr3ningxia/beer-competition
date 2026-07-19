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

          <template v-else-if="bankPending && !editingBankTransfer">
            <div class="pending-panel">
              <el-icon><Clock /></el-icon>
              <div>
                <span>付款信息已提交</span>
                <h2>等待组委会核对到账</h2>
                <p>确认到账后，这批酒款会一起完成报名</p>
                <el-button class="pending-edit-button" @click="editBankTransfer">修改转账信息</el-button>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="method-head">
              <div>
                <span class="section-kicker">当前付款方式</span>
                <h2>{{ payMode === 'BANK_TRANSFER' ? '银行转账' : isWechatPayEnv ? '微信支付' : '微信扫码支付' }}</h2>
              </div>
              <el-dropdown v-if="!editingBankTransfer" placement="bottom-end" trigger="click" @command="switchPayMode">
                <button class="change-method-button" type="button">
                  <span>更换付款方式</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="payMode === 'WECHAT' ? 'BANK_TRANSFER' : 'WECHAT'">
                      {{ payMode === 'WECHAT' ? '改用银行转账' : '改用微信支付' }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <section v-if="payMode === 'WECHAT'" class="wechat-panel">
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
              </dl>
              <el-form label-position="top" class="bank-form">
                <div class="bank-grid">
                  <el-form-item label="付款账户名">
                    <el-input v-model.trim="bankForm.payerName" maxlength="128" placeholder="填写付款账户名…" />
                  </el-form-item>
                  <el-form-item label="转账时间">
                    <el-date-picker
                      v-model="bankForm.transferTime"
                      type="datetime"
                      value-format="YYYY-MM-DDTHH:mm:ss"
                      placeholder="选择转账时间…"
                    />
                  </el-form-item>
                </div>
                <el-form-item label="转账备注">
                  <el-input v-model.trim="bankForm.remark" maxlength="255" placeholder="填写银行流水备注或其他核对信息…" />
                </el-form-item>
                <el-form-item label="付款凭证">
                  <label class="voucher-picker">
                    <input type="file" accept="image/jpeg,image/png,image/webp,application/pdf" @change="selectVoucher" />
                    <el-icon><Upload /></el-icon>
                    <span>{{ voucherFile?.name || bankVoucherName || '选择图片或 PDF' }}</span>
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
  updatePortalBatchBankTransfer,
  uploadPortalBankTransferVoucher,
} from '@/api/portal'
import { buildWechatOauthUrl, currentUrlWithoutWechatCode, invokeWechatPay, isWechatBrowser } from '@/utils/wechatPay'

const route = useRoute()
const router = useRouter()
const requestedBatchId = Number(route.query.batchId || 0)
const orderId = Number(route.query.orderId || 0)
const batchId = ref(0)
const batch = ref(null)
const paymentStatus = ref(null)
const paymentOrder = ref(null)
const bankAccount = ref(null)
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
const bankForm = reactive({ payerName: '', transferTime: '', remark: '' })
let pollingTimer = null
let redirectTimer = null
const currencyFormatter = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', minimumFractionDigits: 0, maximumFractionDigits: 2 })

const orderStatus = computed(() => paymentStatus.value?.status || batch.value?.paymentStatus || '')
const paid = computed(() => orderStatus.value === 'PAID')
const refunded = computed(() => ['PARTIALLY_REFUNDED', 'REFUNDED'].includes(orderStatus.value))
const bankPending = computed(() => orderStatus.value === 'PENDING_CONFIRM')
const isWechatPayEnv = computed(() => isWechatBrowser())
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
    if (isWechatPayEnv.value) await loadWechatPayConfig()
    const statusData = await fetchPortalBatchPaymentStatus(orderId)
    if (requestedBatchId && Number(statusData.batchId) !== requestedBatchId) {
      throw new Error('付款订单与报名批次不一致')
    }
    batchId.value = Number(statusData.batchId || requestedBatchId)
    if (!batchId.value) throw new Error('付款订单缺少报名批次')
    paymentStatus.value = statusData
    batch.value = await fetchPortalEntryBatch(batchId.value)
    if (bankPending.value) {
      startPolling()
    } else if (!paid.value && !refunded.value) {
      if (payMode.value === 'BANK_TRANSFER') await loadBankAccount()
      else await startWechatPayment({ silent: true })
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
  payMode.value = mode
  if (mode === 'BANK_TRANSFER') {
    stopPolling()
    await loadBankAccount()
    return
  }
  await startWechatPayment({ silent: true })
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
  bankAccount.value = await fetchPortalBankTransferAccount()
}

function selectVoucher(event) {
  voucherFile.value = event.target.files?.[0] || null
}

async function submitBankTransfer() {
  if (submittingBank.value) return
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
    ElMessage.warning(error?.message || '转账信息提交失败')
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
    bankForm.payerName = transfer.payerName || ''
    bankForm.transferTime = transfer.transferTime || ''
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
.wechat-panel { display: flex; justify-content: center; gap: 34px; align-items: center; min-height: 330px; margin-top: 20px; padding: 28px; background: #fff8e8; border: 1px solid rgba(87,58,26,.12); border-radius: 8px; }
.qr-frame { display: grid; width: 240px; height: 240px; place-items: center; background: #fff; border: 1px solid rgba(87,58,26,.14); border-radius: 8px; }
.qr-loading { color: #8a7864; }
.jsapi-mark { display: grid; width: 210px; min-height: 210px; place-items: center; align-content: center; gap: 10px; color: #2f6f46; background: #fff; border: 1px solid rgba(87,58,26,.14); border-radius: 8px; text-align: center; }
.jsapi-mark .el-icon { font-size: 52px; }
.jsapi-mark span { color: #806c55; font-size: 13px; }
.wechat-copy { display: grid; gap: 10px; min-width: 230px; }
.wechat-copy > span { color: #806c55; font-size: 13px; }
.wechat-copy > strong { color: #744709; font-size: 34px; font-variant-numeric: tabular-nums; }
.wechat-copy p { margin: 0 0 8px; color: #74624d; }
.bank-panel { margin-top: 20px; }
.bank-account { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 1px; overflow: hidden; background: rgba(87,58,26,.12); border: 1px solid rgba(87,58,26,.12); border-radius: 8px; }
.bank-account div { padding: 13px 15px; background: #fff8e8; }
.bank-account dt { color: #8a7761; font-size: 12px; }
.bank-account dd { margin: 5px 0 0; color: #342518; font-weight: 800; }
.bank-form { margin-top: 20px; }
.bank-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 16px; }
.bank-form :deep(.el-date-editor) { width: 100%; }
.voucher-picker { display: flex; align-items: center; gap: 9px; width: 100%; padding: 12px 14px; color: #6f573e; background: #fffdf8; border: 1px dashed rgba(87,58,26,.25); border-radius: 7px; cursor: pointer; }
.voucher-picker input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.bank-submit { width: 100%; min-height: 46px; background: #875515; border: 0; font-weight: 900; }
.success-panel, .pending-panel { display: flex; gap: 22px; align-items: center; min-height: 330px; justify-content: center; }
.success-panel > .el-icon, .pending-panel > .el-icon { width: 68px; height: 68px; font-size: 68px; }
.success-panel > .el-icon { color: #3b7a4f; }
.pending-panel > .el-icon { color: #ad721e; }
.success-panel h2, .pending-panel h2 { margin: 6px 0; color: #2b1d10; font-size: 25px; }
.success-panel p, .pending-panel p { margin: 0; color: #76644f; }
.success-actions { display: flex; gap: 8px; margin-top: 16px; }
.pending-edit-button { margin-top: 16px; }
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
@media (max-width: 700px) { .payment-heading, .method-head, .wechat-panel { align-items: flex-start; flex-direction: column; } .bank-account, .bank-grid { grid-template-columns: 1fr; } .qr-frame { align-self: center; } }
</style>
