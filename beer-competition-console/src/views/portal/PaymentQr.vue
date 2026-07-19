<template>
  <div class="payment-page">
    <section class="page-head brewer-card">
      <div class="page-title">
        <span class="section-kicker">{{ showPaymentCard ? '报名付款' : '送样办理' }}</span>
        <h1>{{ currentAction.title }}</h1>
        <p>{{ currentAction.description }}</p>
      </div>
      <RouterLink class="back-link" to="/portal/my">返回我的参赛</RouterLink>
      <div v-if="selectedEntry" class="flow-steps" aria-label="送样进度">
        <span :class="{ done: selectedEntry.paymentStatus === 'PAID' || selectedEntry.canDownloadLabel, active: selectedEntry.paymentStatus !== 'PAID' }">
          <b>1</b>报名付款
        </span>
        <span :class="{ done: selectedEntry.canDownloadLabel, active: selectedEntry.paymentStatus === 'PAID' && !selectedEntry.canDownloadLabel }">
          <b>2</b>标签下载
        </span>
        <span :class="{ done: hasSubmittedDelivery, active: selectedEntry.canDownloadLabel && !hasSubmittedDelivery }">
          <b>3</b>送样信息
        </span>
        <span :class="{ done: selectedEntry.deliveryStatus === 'RECEIVED' || isStored(selectedEntry), active: hasSubmittedDelivery && selectedEntry.deliveryStatus !== 'RECEIVED' && !isStored(selectedEntry) }">
          <b>4</b>组委会入库
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
          <button
            v-if="payableEntries.length"
            class="entry-picker-toggle"
            type="button"
            :aria-expanded="entryPickerOpen"
            @click="entryPickerOpen = !entryPickerOpen"
          >
            {{ entryPickerOpen ? '收起' : '切换' }}
          </button>
        </div>

        <div :class="['entry-list', { open: entryPickerOpen }]">
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
            <p>提交报名后，这里会显示每款酒的支付、标签和送样进度</p>
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
            <el-dropdown trigger="click" @command="handleMoreCommand">
              <el-button class="more-button" size="small" plain>
                更多
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit-entry" :disabled="!selectedEntry.canUpdateInfo">修改报名资料</el-dropdown-item>
                  <el-dropdown-item v-if="canCancelSelectedEntry" command="cancel-entry">取消报名</el-dropdown-item>
                  <el-dropdown-item v-if="selectedEntry.canRequestRefund" command="refund">申请退款</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </section>

        <section v-if="showRefundCard" class="refund-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">REFUND</span>
              <h3>{{ refundCardTitle }}</h3>
              <p>{{ refundCardDescription }}</p>
            </div>
            <span :class="['task-chip', refundTone(selectedEntry.refundStatus)]">
              {{ refundStatusText(selectedEntry.refundStatus) }}
            </span>
          </div>
          <dl class="inline-facts">
            <div>
              <dt>退款金额</dt>
              <dd>{{ formatCurrency(selectedEntry.refund?.amount || entryPayAmount(selectedEntry)) }}</dd>
            </div>
            <div v-if="selectedEntry.refundReason">
              <dt>退款原因</dt>
              <dd>{{ selectedEntry.refundReason }}</dd>
            </div>
            <div v-if="selectedEntry.refundRequestedAt">
              <dt>申请时间</dt>
              <dd>{{ formatDateTime(selectedEntry.refundRequestedAt) }}</dd>
            </div>
          </dl>
        </section>

        <section v-if="showPaymentCard" class="payment-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">报名支付</span>
              <h3>完成报名付款</h3>
              <p>请先核对应付金额，再选择本款酒的付款方式</p>
            </div>
            <strong>{{ formatCurrency(entryPayAmount(selectedEntry)) }}</strong>
          </div>
          <dl class="payment-entry-summary">
            <div>
              <dt>当前酒款</dt>
              <dd>{{ selectedEntry.name }}</dd>
            </div>
            <div>
              <dt>赛事与组别</dt>
              <dd>{{ entryMetaLine(selectedEntry) }}</dd>
            </div>
            <div>
              <dt>应付金额</dt>
              <dd>{{ formatCurrency(entryPayAmount(selectedEntry)) }}</dd>
            </div>
          </dl>
          <div v-if="showBankPendingPanel" class="bank-pending-box">
            <div class="bank-pending-head">
              <div>
                <strong>等待组委会核对到账</strong>
                <span>付款信息已提交，我们将在5个工作日内核对到账，确认后开放标签下载和送样信息填写</span>
              </div>
              <el-button
                v-if="selectedEntry.payment?.bankTransferId"
                :loading="cancelingBankTransfer"
                @click="editSelectedBankTransfer"
              >
                修改转账信息
              </el-button>
            </div>
            <dl class="bank-submitted-summary">
              <div>
                <dt>付款账户名</dt>
                <dd>{{ submittedBankTransfer?.payerName || '未填写' }}</dd>
              </div>
              <div>
                <dt>转账备注</dt>
                <dd>{{ submittedBankTransfer?.remark || '未填写' }}</dd>
              </div>
              <div>
                <dt>付款凭证</dt>
                <dd>{{ submittedBankTransfer?.voucherFileName || '未上传' }}</dd>
              </div>
            </dl>
            <div class="bank-service-contact">
              <span>加急到账、开票或付款问题</span>
              <b>小秘书微信 {{ bankServiceWechat }}</b>
            </div>
          </div>

          <template v-else>
            <div v-if="!editingBankTransferId" class="payment-method-options" role="radiogroup" aria-label="选择付款方式">
              <button
                :class="['payment-method-option', 'recommended', { active: payMode === 'WECHAT' }]"
                type="button"
                role="radio"
                :aria-checked="payMode === 'WECHAT'"
                @click="switchPayMode('WECHAT')"
              >
                <div class="payment-method-option-title">
                  <span>{{ wechatPayTitle }}</span>
                </div>
              </button>
              <button
                :class="['payment-method-option', { active: payMode === 'BANK_TRANSFER' }]"
                type="button"
                role="radio"
                :aria-checked="payMode === 'BANK_TRANSFER'"
                @click="switchPayMode('BANK_TRANSFER')"
              >
                <div class="payment-method-option-title">
                  <span>银行转账</span>
                </div>
              </button>
            </div>
            <p v-if="!editingBankTransferId" class="payment-method-note">{{ selectedPaymentModeNote }}</p>

            <div v-if="payMode === 'WECHAT'" class="payment-actions">
              <div v-if="paymentOrder?.mode === 'WECHAT' && !isWechatPayEnv" class="wechat-pay-box">
                <img v-if="paymentQrDataUrl" :src="paymentQrDataUrl" alt="微信支付二维码" />
                <div>
                  <strong>微信扫码支付</strong>
                  <span>{{ paymentExpireText }} 付款成功后可继续下载标签和填写送样信息</span>
                </div>
              </div>
              <el-button
                v-if="paymentOrder?.mode === 'MOCK'"
                type="primary"
                size="large"
                :loading="simulatingPayment"
                @click="simulatePayment"
              >
                模拟微信到账
              </el-button>
              <el-button v-else type="primary" size="large" :loading="creatingPayment" @click="startWechatPayment">
                {{ wechatPayButtonText }}
              </el-button>
              <el-button :loading="checkingPayment" @click="checkPaymentStatus">查看支付结果</el-button>
            </div>

            <div v-else class="bank-transfer-panel">
              <div class="bank-section-title">
                <span>1</span>
                <strong>收款账户</strong>
              </div>
              <section v-if="bankAccount" class="bank-account-box">
                <div><dt>账户名</dt><dd>{{ bankAccount.accountName }}</dd></div>
                <div><dt>开户行</dt><dd>{{ bankAccount.bankName }}</dd></div>
                <div><dt>账号</dt><dd>{{ bankAccount.accountNo }}</dd></div>
                <div><dt>备注</dt><dd>{{ bankAccount.remarkTip }}</dd></div>
              </section>
              <div class="bank-account-actions">
                <el-button @click="copyBankAccount">复制账户信息</el-button>
                <div class="bank-service-contact">
                  <span>加急到账、开票或付款问题</span>
                  <b>小秘书微信 {{ bankServiceWechat }}</b>
                </div>
              </div>

              <div class="bank-section-title">
                <span>2</span>
                <strong>提交付款信息</strong>
              </div>
              <el-form label-position="top" class="bank-transfer-form">
                <div class="bank-form-grid">
                  <el-form-item label="应付金额" class="bank-amount-field">
                    <div class="fixed-amount">{{ formatCurrency(entryPayAmount(selectedEntry)) }}</div>
                  </el-form-item>
                  <el-form-item label="付款账户名" class="bank-payer-field">
                    <el-input v-model.trim="bankTransferForm.payerName" placeholder="填写实际转账账户名，便于核对到账" />
                  </el-form-item>
                  <el-form-item label="转账备注（选填）" class="bank-remark-field">
                    <el-input v-model.trim="bankTransferForm.remark" placeholder="如银行转账时填写了备注，可在这里补充" />
                  </el-form-item>
                </div>
                <div class="voucher-row">
                  <input
                    ref="bankVoucherInputRef"
                    class="hidden-file"
                    type="file"
                    accept=".jpg,.jpeg,.png,.webp,.pdf"
                    @change="uploadBankVoucher"
                  />
                  <el-button :loading="uploadingBankVoucher" @click="bankVoucherInputRef?.click()">
                    {{ bankTransferForm.voucherFileName ? '重新上传付款凭证' : '上传付款凭证' }}
                  </el-button>
                  <span>{{ bankTransferForm.voucherFileName || '未上传付款凭证；支持图片或 PDF，单个文件不超过 10MB' }}</span>
                </div>
                <div class="payment-actions bank-submit-actions">
                  <el-button
                    type="primary"
                    size="large"
                    :disabled="!selectedEntry"
                    :loading="submittingBankTransfer"
                    @click="submitBankTransferForCurrentEntry"
                  >
                    {{ editingBankTransferId ? '保存转账信息' : '提交转账信息' }}
                  </el-button>
                  <el-button v-if="editingBankTransferId" size="large" @click="cancelEditingBankTransfer">
                    取消修改
                  </el-button>
                </div>
              </el-form>
            </div>
          </template>
        </section>

        <section v-if="showLabelCard" class="label-card brewer-card">
          <div class="card-head">
            <div>
              <span class="section-kicker">LABEL</span>
              <h3>{{ labelCardTitle }}</h3>
            </div>
            <span :class="['task-chip', selectedEntry.canDownloadLabel ? 'tone-green' : 'tone-amber']">
              {{ selectedEntry.canDownloadLabel ? '已开放下载' : '待支付' }}
            </span>
          </div>

          <div class="label-preview-wrap">
            <div class="label-preview" v-html="labelSvg" />
          </div>
          <div class="label-actions">
            <el-button type="primary" :disabled="!selectedEntry.canDownloadLabel" :loading="labelPdfDownloading" @click="downloadLabelPdf">
              下载 PDF
            </el-button>
            <el-button :disabled="!selectedEntry.canDownloadLabel" :loading="labelPngDownloading" @click="downloadLabelPng">
              下载 PNG
            </el-button>
          </div>
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

            <div class="button-row delivery-summary-actions">
              <el-button
                v-if="selectedEntry.deliveryStatus !== 'RECEIVED'"
                type="primary"
                plain
                @click="editingDelivery = true"
              >
                修改送样信息
              </el-button>
              <el-tooltip content="更新入库状态" placement="top">
                <el-button
                  class="delivery-refresh-button"
                  :icon="Refresh"
                  circle
                  aria-label="更新入库状态"
                  @click="refreshEntries(selectedEntry.id)"
                />
              </el-tooltip>
            </div>
          </div>

          <el-form v-else label-position="top" class="delivery-form">
            <div class="delivery-grid">
              <el-form-item label="送样方式" class="delivery-method-field">
                <div v-if="fixedDeliveryMethod" class="fixed-delivery-method">
                  {{ deliveryMethodText(fixedDeliveryMethod) }}
                </div>
                <el-radio-group v-else v-model="deliveryForm.deliveryMethod">
                  <el-radio-button label="EXPRESS" :disabled="selectedLogistics.deliveryMethod === 'ONSITE'">快递寄送</el-radio-button>
                  <el-radio-button label="ONSITE" :disabled="selectedLogistics.deliveryMethod === 'EXPRESS'">现场送样</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item v-if="deliveryForm.deliveryMethod === 'EXPRESS'" label="快递公司" class="delivery-carrier-field">
                <el-input v-model.trim="deliveryForm.carrier" placeholder="例如：顺丰、京东快递" />
              </el-form-item>

              <el-form-item v-if="deliveryForm.deliveryMethod === 'EXPRESS'" label="快递单号" class="delivery-tracking-field">
                <el-input v-model.trim="deliveryForm.trackingNo" placeholder="填写本次送样的快递单号" />
              </el-form-item>

              <el-form-item label="送样备注" class="full-width">
                <el-input
                  v-model.trim="deliveryForm.deliveryNote"
                  type="textarea"
                  :rows="4"
                  maxlength="500"
                  show-word-limit
                  placeholder="例如：本次寄送 2 箱，每箱 6 瓶；如有分箱、温控或破损风险说明，请写在这里"
                />
              </el-form-item>
            </div>

            <div class="button-row delivery-form-actions">
              <el-button type="primary" :loading="savingDelivery" @click="saveDelivery">
                保存并提交送样信息
              </el-button>
              <el-button v-if="hasSubmittedDelivery" @click="cancelEditingDelivery">取消修改</el-button>
            </div>
          </el-form>
        </section>
      </main>

      <aside v-if="selectedEntry" class="requirement-aside brewer-card">
        <template v-if="showPaymentCard">
          <div class="aside-head">
            <span class="section-kicker">NEXT</span>
            <h2>付款后办理</h2>
          </div>
          <dl class="aside-list payment-next-list">
            <div>
              <dt>标签下载</dt>
              <dd>付款确认后开放现场标签下载</dd>
            </div>
            <div>
              <dt>送样信息</dt>
              <dd>填写送样方式、快递公司和快递单号</dd>
            </div>
            <div>
              <dt>收件信息</dt>
              <dd>付款确认后显示完整收件信息</dd>
            </div>
          </dl>
        </template>
        <template v-else>
          <div class="aside-head">
            <span class="section-kicker">REQUIREMENTS</span>
            <h2>送样要求</h2>
          </div>

          <dl class="aside-list">
            <div>
              <dt>送达时间</dt>
              <dd>{{ arrivalWindowText }}</dd>
            </div>
            <div>
              <dt>送样方式</dt>
              <dd>{{ competitionDeliveryMethodText }}</dd>
            </div>
            <div>
              <dt>样品要求</dt>
              <dd>{{ selectedLogistics.sampleQuantityNote || '以组委会赛事通知为准' }}</dd>
            </div>
            <div>
              <dt>标签粘贴</dt>
              <dd>每款酒瓶身至少贴 1 张，整箱寄送时外箱再贴 1 张</dd>
            </div>
            <div>
              <dt>包装检查</dt>
              <dd>{{ selectedLogistics.deliveryNote || '请做好防震、防漏和外箱加固，避免运输中破损' }}</dd>
            </div>
          </dl>

          <section class="address-block">
            <span class="section-kicker">ADDRESS</span>
            <h3>收件信息</h3>
            <p>{{ logisticsAddressText }}</p>
            <el-button v-if="canCopyLogisticsAddress" @click="copyLogisticsAddress">复制收件信息</el-button>
          </section>
        </template>
      </aside>
    </section>

    <el-dialog
      v-model="editDialogVisible"
      title="修改报名资料"
      width="640px"
      class="entry-edit-dialog"
      :close-on-click-modal="!savingEdit"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editFormRules"
        :disabled="savingEdit"
        label-position="top"
        class="entry-edit-form"
      >
        <section class="locked-category">
          <span>投递组别</span>
          <strong>{{ selectedEntry?.categoryName || '-' }}</strong>
          <p>组别不支持修改。如需修改，请退款后重新报名。</p>
        </section>

        <el-form-item label="酒款名称" prop="name">
          <el-input v-model.trim="editForm.name" placeholder="请填写酒款名称" />
        </el-form-item>

        <div class="edit-form-grid">
          <el-form-item label="基础风格" prop="style">
            <el-select v-model="editForm.style" filterable placeholder="请选择或搜索基础风格">
              <el-option
                v-for="item in editCompetition?.styles || []"
                :key="item.id || item.name"
                :label="styleLabel(item)"
                :value="item.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="ABV" prop="abv">
            <el-input
              v-model.trim="editForm.abv"
              inputmode="decimal"
              placeholder="请填写酒精度"
              @blur="editForm.abv = normalizeAbvInput(editForm.abv)"
            >
              <template #suffix>
                <span class="abv-input-suffix">%</span>
              </template>
            </el-input>
          </el-form-item>
        </div>

        <section v-if="editConfiguredFields.length" class="edit-extra-section">
          <h4>补充信息</h4>
          <div class="edit-extra-grid">
            <el-form-item
              v-for="field in editConfiguredFields"
              :key="field.fieldKey"
              :label="field.fieldLabel"
              :prop="`extraFields.${field.fieldKey}`"
              class="edit-extra-field"
            >
              <template #label>
                <span class="field-label">
                  <span>{{ field.fieldLabel }}</span>
                  <em v-if="field.required">必填</em>
                </span>
              </template>

              <el-input
                v-if="field.fieldType === 'textarea'"
                v-model.trim="editForm.extraFields[field.fieldKey]"
                type="textarea"
                :rows="4"
                maxlength="255"
                show-word-limit
                placeholder="选填"
              />
              <el-input-number
                v-else-if="field.fieldType === 'number'"
                v-model="editForm.extraFields[field.fieldKey]"
                :min="0"
                controls-position="right"
                placeholder="请输入数字"
              />
              <el-select
                v-else-if="field.fieldType === 'select'"
                v-model="editForm.extraFields[field.fieldKey]"
                placeholder="请选择"
              >
                <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
              </el-select>
              <el-select
                v-else-if="field.fieldType === 'multi_select'"
                v-model="editForm.extraFields[field.fieldKey]"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择，可多选"
              >
                <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
              </el-select>
              <el-input
                v-else
                v-model.trim="editForm.extraFields[field.fieldKey]"
                maxlength="255"
                show-word-limit
                :placeholder="field.helpText || '请填写'"
              />
              <p v-if="field.helpText" class="field-help">{{ field.helpText }}</p>
            </el-form-item>
          </div>
        </section>
      </el-form>

      <template #footer>
        <div class="edit-dialog-actions">
          <el-button :disabled="savingEdit" @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingEdit" @click="saveEntryEdit">保存修改</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="refundDialogVisible"
      title="退赛退款"
      width="520px"
      class="refund-confirm-dialog"
      align-center
      :close-on-click-modal="!refunding"
    >
      <div class="refund-confirm-body">
        <p>
          点击确定退款按钮，这款酒将退出比赛，酒标同时作废。如需修改酒款信息，您可以直接点击“修改信息”。您确定要退款吗？
        </p>
        <el-input
          v-model.trim="refundForm.reason"
          class="refund-reason-input"
          placeholder="退款原因（选填）"
          maxlength="200"
          clearable
        />
      </div>
      <template #footer>
        <div class="refund-dialog-actions">
          <el-button class="refund-cancel-button" :disabled="refunding" @click="refundDialogVisible = false">取消退款</el-button>
          <el-button
            class="refund-edit-button"
            :disabled="refunding || !selectedEntry?.canUpdateInfo"
            @click="editEntryFromRefundDialog"
          >
            修改酒款信息
          </el-button>
          <el-button class="refund-confirm-button" :loading="refunding" @click="confirmRefundRequest">确定退款</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import {
  cancelPortalEntry,
  createPortalEntryWechatJsapiPayment,
  createPortalEntryWechatNativePayment,
  downloadPortalEntryLabelPdf,
  downloadPortalEntryLabelPng,
  fetchPortalCompetitionDetail,
  fetchPortalBankTransferAccount,
  fetchPortalBankTransfer,
  fetchPortalEntryBatch,
  fetchPortalEntryPaymentStatus,
  fetchPortalEntries,
  fetchPortalEntryDetail,
  fetchPortalEntryLabel,
  fetchPortalWechatPayClientConfig,
  requestPortalEntryRefund,
  simulatePortalEntryPayment,
  submitPortalBankTransfer,
  submitPortalEntryDelivery,
  updatePortalEntry,
  updatePortalBankTransfer,
  uploadPortalBankTransferVoucher,
} from '@/api/portal'
import { JUDGE_H5_BASE_URL, WECHAT_PAY_APP_ID } from '@/config'
import { formatAbvWithUnit, isValidAbvInput, normalizeAbvInput } from '@/utils/formatters'
import { buildWechatOauthUrl, currentUrlWithoutWechatCode, invokeWechatPay, isWechatBrowser } from '@/utils/wechatPay'
import { entryPayAmount, isEntryRefundActive, isEntryRefunded } from './portalViewModels'

const route = useRoute()
const router = useRouter()
const batchId = Number(route.query.batchId || 0)

const entries = ref([])
const selectedEntry = ref(null)
const labelData = ref(null)
const labelPdfDownloading = ref(false)
const labelPngDownloading = ref(false)
const editingDelivery = ref(false)
const savingDelivery = ref(false)
const simulatingPayment = ref(false)
const creatingPayment = ref(false)
const checkingPayment = ref(false)
const paymentOrder = ref(null)
const paymentQrDataUrl = ref('')
const wechatPayConfig = ref(null)
const payMode = ref('WECHAT')
const bankAccount = ref(null)
const submittedBankTransfer = ref(null)
const editingBankTransferId = ref(null)
const bankVoucherInputRef = ref(null)
const uploadingBankVoucher = ref(false)
const submittingBankTransfer = ref(false)
const cancelingBankTransfer = ref(false)
const entryPickerOpen = ref(false)
const editDialogVisible = ref(false)
const editFormRef = ref(null)
const editCompetition = ref(null)
const savingEdit = ref(false)
const refundDialogVisible = ref(false)
const refunding = ref(false)
let paymentPollingTimer = null
let paymentPollingCount = 0
const DEFAULT_REFUND_REASON = '退赛退款'
const deliveryForm = reactive({
  deliveryMethod: 'EXPRESS',
  carrier: '',
  trackingNo: '',
  deliveryNote: '',
})
const bankTransferForm = reactive({
  payerName: '',
  remark: '',
  voucherAssetId: null,
  voucherFileName: '',
})
const refundForm = reactive({
  reason: '',
})
const editForm = reactive({
  name: '',
  style: '',
  abv: '',
  extraFields: {},
})

const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

const payableEntries = computed(() => {
  return [...entries.value].sort((a, b) => entrySortWeight(a) - entrySortWeight(b) || b.id - a.id)
})
const selectedLogistics = computed(() => selectedEntry.value?.competitionLogistics || {})
const editConfiguredFields = computed(() => normalizeEntryFields(editCompetition.value?.entryFields || []))
const editFormRules = computed(() => {
  const rules = {
    name: [{ required: true, message: '请填写酒款名称', trigger: 'blur' }],
    style: [{ required: true, message: '请选择基础风格', trigger: 'change' }],
    abv: [
      {
        validator: (_rule, value, callback) => {
          if (isValidAbvInput(value)) {
            callback()
            return
          }
          callback(new Error('请填写 0-99.99 之间的 ABV，最多两位小数'))
        },
        trigger: ['blur', 'change'],
      },
    ],
  }
  editConfiguredFields.value.forEach((field) => {
    if (!field.required) return
    rules[`extraFields.${field.fieldKey}`] = [
      {
        validator: (_rule, value, callback) => {
          if (hasFieldValue(value)) {
            callback()
            return
          }
          callback(new Error(`请填写${field.fieldLabel}`))
        },
        trigger: field.fieldType === 'text' || field.fieldType === 'textarea' ? 'blur' : 'change',
      },
    ]
  })
  return rules
})
const fixedDeliveryMethod = computed(() => {
  if (selectedLogistics.value.deliveryMethod === 'EXPRESS') return 'EXPRESS'
  if (selectedLogistics.value.deliveryMethod === 'ONSITE') return 'ONSITE'
  return ''
})

const hasSubmittedDelivery = computed(() => hasDeliveryProgress(selectedEntry.value))
const showRefundCard = computed(() => Boolean(selectedEntry.value?.refundStatus))
const showPaymentCard = computed(() => {
  const entry = selectedEntry.value
  return Boolean(entry)
    && entry.status === 'PENDING_PAYMENT'
    && entry.paymentStatus !== 'PAID'
    && !isEntryRefunded(entry)
    && !isEntryRefundActive(entry)
})
const showBankPending = computed(() => selectedEntry.value?.paymentStatus === 'PENDING_CONFIRM')
const showBankPendingPanel = computed(() => showBankPending.value && !editingBankTransferId.value)
const canCancelSelectedEntry = computed(() => {
  const entry = selectedEntry.value
  if (!entry) return false
  return entry.status === 'PENDING_PAYMENT' && entry.paymentStatus === 'UNPAID'
})
const paymentExpireText = computed(() => {
  if (!paymentOrder.value?.expireTime) return '请在页面提示时间内完成付款'
  return `${formatDateTime(paymentOrder.value.expireTime)} 前完成支付`
})
const showLabelCard = computed(() => Boolean(selectedEntry.value?.canDownloadLabel))
const selectedPaymentModeNote = computed(() => {
  if (payMode.value === 'BANK_TRANSFER') {
    return '点击按钮即可查看收款账户信息。转账后请上传付款凭证，我们将在5个工作日内核对到账'
  }
  if (isWechatPayEnv.value) {
    return '支付成功后立即完成报名，并开放标签下载和送样信息填写'
  }
  return '请使用微信扫一扫支付，付款后回到页面查看结果'
})
const isWechatPayEnv = computed(() => isWechatBrowser())
const wechatAuthCode = ref(String(route.query.code || ''))
const wechatPayAppId = computed(() => wechatPayConfig.value?.appId || WECHAT_PAY_APP_ID)
const wechatPayTitle = computed(() => isWechatPayEnv.value ? '微信支付' : '微信扫码支付')
const wechatPayButtonText = computed(() => {
  if (isWechatPayEnv.value) {
    return paymentOrder.value?.mode === 'WECHAT' ? '重新发起微信支付' : '发起微信支付'
  }
  return paymentOrder.value?.mode === 'WECHAT' ? '重新生成支付码' : '生成微信支付码'
})
const bankServiceWechat = computed(() => bankAccount.value?.serviceWechat || 'beerxms')
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
  if (hasSubmittedDelivery.value) return '补打现场标签'
  return '送样前请先贴好标签'
})
const labelActionDescription = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return '支付成功后开放标签下载'
  if (hasSubmittedDelivery.value) return '如现场需要补贴、重贴或核对编号，可重新下载或打印这一张标签'
  return '打印后直接贴标，再回到下方填写送样方式和快递单号'
})

const arrivalWindowText = computed(() => {
  const start = formatDateTime(selectedLogistics.value.sampleArrivalStart)
  const deadline = formatDateTime(selectedLogistics.value.sampleArrivalDeadline)
  if (start !== '待更新' && deadline !== '待更新') return `${start} 至 ${deadline}`
  if (deadline !== '待更新') return `${deadline} 前送达`
  if (!selectedEntry.value?.competitionDate) return '请至少预留 2 到 3 天运输时间，并以组委会通知为准'
  return `${formatDate(selectedEntry.value.competitionDate)} 前完成送达`
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
  if (selectedLogistics.value.logisticsVisibility === 'PAYMENT_CONFIRMED') return '支付成功后显示完整收件信息'
  return '组委会暂未填写完整收件信息，寄出前请先联系确认'
})
const currentAction = computed(() => {
  const entry = selectedEntry.value
  if (!entry) {
    return {
      title: '请选择酒款',
      description: '左侧列表会显示每款酒当前的支付与送样进度',
    }
  }

  if (isEntryRefundActive(entry)) {
    return {
      title: '退款申请已提交',
      description: '微信支付将原路退回；银行转账由组委会联系处理',
    }
  }

  if (isEntryRefunded(entry)) {
    return {
      title: '退款已完成',
      description: '这款酒已退出本场比赛，标签和送样操作已停止使用',
    }
  }

  if (entry.status === 'CANCELED') {
    return {
      title: '报名已取消',
      description: '这款酒不会继续进入本场比赛流程',
    }
  }

  if (entry.paymentStatus !== 'PAID') {
    if (entry.paymentStatus === 'PENDING_CONFIRM') {
      return {
        title: '等待转账确认',
        description: '付款信息已提交，我们将在5个工作日内核对到账，确认后开放标签下载和送样信息填写',
      }
    }
    return {
      title: '完成报名付款',
      description: '核对金额并选择付款方式，付款确认后开放现场标签下载和送样信息填写',
    }
  }

  if (!entry.canDownloadLabel) {
    return {
      title: '支付已完成，等待标签开放',
      description: '当前酒款已经进入处理中，标签开放后请立即下载打印并填写送样信息',
    }
  }

  if (!hasDeliveryProgress(entry)) {
    return {
      title: '下载标签并提交快递信息',
      description: '先打印并贴好标签，再把送样方式、快递公司和快递单号填写完整',
    }
  }

  if (entry.deliveryStatus === 'RECEIVED' || isStored(entry)) {
    return {
      title: '样品已签收，可等待入库或后续赛事进度',
      description: '组委会已经确认样品入库，这一款酒的送样环节基本完成',
    }
  }

  return {
    title: '等待组委会确认入库',
    description: '快递信息已经提交，请持续关注物流进度，直至组委会确认签收入库',
  }
})

const deliveryCardTitle = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return '支付成功后开放送样信息填写'
  if (showDeliverySummary.value) return selectedEntry.value.deliveryStatus === 'RECEIVED' ? '样品已入库' : '你已提交送样信息'
  return hasSubmittedDelivery.value ? '修改送样信息' : '填写送样信息'
})

const deliveryCardDescription = computed(() => {
  if (!selectedEntry.value?.canDownloadLabel) return '支付成功后可下载标签'
  if (showDeliverySummary.value) return selectedEntry.value.deliveryStatus === 'RECEIVED' ? '组委会已经确认样品入库，这里保留本次提交内容' : '快递信息已提交，请保留单号并持续关注签收进度'
  return '寄出后请尽快提交快递信息，避免组委会无法及时核对来样'
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
  if (isWechatPayEnv.value) {
    await loadWechatPayConfig()
  }
  entries.value = await loadEntries()
  if (String(route.query.payMode || '').toLowerCase() === 'bank') {
    payMode.value = 'BANK_TRANSFER'
    await ensureBankAccount()
  }
  const entryId = Number(route.query.entryId || 0)
  const target = entries.value.find((entry) => entry.id === entryId) || payableEntries.value[0] || null
  await selectEntry(target, { skipConfirm: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  stopPaymentPolling()
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
  if (entry.abv !== null && entry.abv !== undefined && entry.abv !== '') parts.push(formatAbvWithUnit(entry.abv))
  return parts.filter(Boolean).join(' · ')
}

function deliveryMethodText(value) {
  if (value === 'EXPRESS') return '快递寄送'
  if (value === 'ONSITE') return '现场送样'
  return '未填写'
}

function deliveryStatusText(value) {
  if (value === 'SUBMITTED') return '已提交送样信息'
  if (value === 'RECEIVED') return '组委会已确认入库'
  return '待提交'
}

function deliveryTone(value) {
  if (value === 'RECEIVED') return 'tone-green'
  if (value === 'SUBMITTED') return 'tone-blue'
  return 'tone-amber'
}

function taskTone(entry) {
  if (!entry) return 'tone-amber'
  if (entry.status === 'CANCELED') return 'tone-muted'
  if (isEntryRefunded(entry)) return 'tone-green'
  if (isEntryRefundActive(entry)) return 'tone-blue'
  if (entry.refundStatus === 'FAILED' || entry.refundStatus === 'REJECTED') return 'tone-amber'
  if (entry.deliveryStatus === 'RECEIVED' || isStored(entry)) return 'tone-green'
  if (hasDeliveryProgress(entry)) return 'tone-blue'
  if (entry.paymentStatus === 'PAID') return 'tone-brown'
  return 'tone-amber'
}

function entryTaskLabel(entry) {
  if (!entry) return '待处理'
  if (entry.status === 'CANCELED') return '已取消'
  if (isEntryRefunded(entry)) return '已退款'
  if (isEntryRefundActive(entry)) return '退款处理中'
  if (entry.refundStatus === 'FAILED') return '退款失败'
  if (entry.refundStatus === 'REJECTED') return '退款已驳回'
  if (entry.paymentStatus === 'PENDING_CONFIRM') return '等待转账确认'
  if (entry.deliveryStatus === 'RECEIVED' || isStored(entry)) return '已签收 / 入库'
  if (hasDeliveryProgress(entry)) return '等待组委会确认入库'
  if (entry.paymentStatus === 'PAID') return '待提交送样信息'
  return '待支付'
}

function isStored(entry) {
  return entry?.stored || entry?.storedFlag === 1 || entry?.status === 'STORED' || entry?.status === 'RESULT_PUBLISHED'
}

function hasDeliveryProgress(entry) {
  if (!entry) return false
  return ['SUBMITTED', 'RECEIVED'].includes(entry.deliveryStatus) || Boolean(entry.deliverySubmittedAt || entry.trackingNo || entry.deliveryMethod)
}

function normalizeEntryFields(fields = []) {
  return fields
    .map((field, index) => {
      if (typeof field === 'string') {
        return {
          fieldKey: `legacy_${index}`,
          fieldLabel: field,
          fieldType: 'text',
          helpText: '',
          options: [],
          required: false,
          visibleToJudges: true,
          sortOrder: index,
        }
      }
      return {
        fieldKey: field.fieldKey || `custom_${index}`,
        fieldLabel: field.fieldLabel || field.label || `补充字段 ${index + 1}`,
        fieldType: field.fieldType || field.type || 'text',
        helpText: field.helpText || '',
        options: Array.isArray(field.options) ? field.options : [],
        required: Boolean(field.required),
        visibleToJudges: field.visibleToJudges !== false,
        sortOrder: Number(field.sortOrder ?? index),
      }
    })
    .sort((a, b) => a.sortOrder - b.sortOrder)
}

function hasFieldValue(value) {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function styleLabel(item) {
  return item?.styleCode ? `${item.styleCode} ${item.name}` : item?.name
}

const refundCardTitle = computed(() => {
  if (isEntryRefunded(selectedEntry.value)) return '退款已完成'
  if (selectedEntry.value?.refundStatus === 'REJECTED') return '退款申请已驳回'
  if (selectedEntry.value?.refundStatus === 'FAILED') return '退款暂未成功'
  return '退款申请已提交'
})

const refundCardDescription = computed(() => {
  if (isEntryRefunded(selectedEntry.value)) return '报名费已完成退款，这款酒不会继续进入本场比赛流程'
  if (selectedEntry.value?.refundStatus === 'REJECTED') return '组委会未通过本次退款申请，标签和送样操作已恢复'
  if (selectedEntry.value?.refundStatus === 'FAILED') return '退款处理失败，请等待组委会重新处理或联系确认'
  return '微信支付将原路退回；银行转账由组委会联系处理'
})

function refundStatusText(status) {
  return {
    REQUESTED: '待处理',
    APPROVED: '处理中',
    PROCESSING: '处理中',
    SUCCESS: '已退款',
    FAILED: '退款失败',
    REJECTED: '已驳回',
  }[status] || status || '-'
}

function refundTone(status) {
  if (status === 'SUCCESS') return 'tone-green'
  if (['REQUESTED', 'APPROVED', 'PROCESSING'].includes(status)) return 'tone-blue'
  return 'tone-amber'
}

function syncDeliveryForm(entry) {
  deliveryForm.deliveryMethod = fixedDeliveryMethod.value || entry?.deliveryMethod || defaultDeliveryMethod()
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
  return fixedDeliveryMethod.value || 'EXPRESS'
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
      await ElMessageBox.confirm('当前酒款还有未保存的送样信息，切换后会丢失这些修改，是否继续切换？', '未保存的修改', {
        confirmButtonText: '继续切换',
        cancelButtonText: '留在当前酒款',
        type: 'warning',
      })
    } catch {
      return
    }
  }

  selectedEntry.value = await fetchPortalEntryDetail(entry.id)
  if (showPaymentCard.value && selectedEntry.value?.paymentOrderId && selectedEntry.value?.registrationBatchId) {
    await router.replace({
      path: '/portal/batch-payment',
      query: {
        batchId: selectedEntry.value.registrationBatchId,
        orderId: selectedEntry.value.paymentOrderId,
        payMode: String(route.query.payMode || '').toLowerCase() === 'bank' ? 'bank_transfer' : 'wechat',
      },
    })
    return
  }
  labelData.value = null
  submittedBankTransfer.value = null
  editingBankTransferId.value = null
  paymentOrder.value = null
  paymentQrDataUrl.value = ''
  stopPaymentPolling()
  syncDeliveryForm(selectedEntry.value)
  syncBankTransferForm()
  if (showBankPending.value && selectedEntry.value?.payment?.bankTransferId) {
    await ensureBankAccount()
    submittedBankTransfer.value = await fetchPortalBankTransfer(selectedEntry.value.payment.bankTransferId)
  }
  if (selectedEntry.value?.canDownloadLabel) {
    labelData.value = await fetchPortalEntryLabel(entry.id)
  } else if (showPaymentCard.value && !showBankPending.value && payMode.value === 'WECHAT') {
    await startWechatPayment({ silent: true })
  }
  entryPickerOpen.value = false
}

async function refreshEntries(targetId = selectedEntry.value?.id) {
  entries.value = await loadEntries()
  if (!targetId) return
  const matched = entries.value.find((entry) => entry.id === targetId)
  if (matched) {
    await selectEntry(matched, { skipConfirm: true })
  }
}

function handleMoreCommand(command) {
  if (command === 'edit-entry') {
    openSelectedEntryEdit()
  }
  if (command === 'cancel-entry') {
    cancelSelectedEntry()
  }
  if (command === 'refund') {
    submitRefundRequest()
  }
}

async function loadEntries() {
  if (!batchId) return fetchPortalEntries()
  const batch = await fetchPortalEntryBatch(batchId)
  return batch.entries || []
}

async function openSelectedEntryEdit() {
  if (!selectedEntry.value?.canUpdateInfo) {
    ElMessage.warning(selectedEntry.value?.updateInfoDisabledReason || '当前酒款不能修改资料')
    return
  }
  try {
    editCompetition.value = await fetchPortalCompetitionDetail(selectedEntry.value.competitionId)
    resetEditForm(selectedEntry.value)
    editDialogVisible.value = true
    await nextTick()
    editFormRef.value?.clearValidate()
  } catch (error) {
    ElMessage.warning(error?.message || '报名配置加载失败')
  }
}

function resetEditForm(entry) {
  editForm.name = entry?.name || ''
  editForm.style = entry?.style || ''
  editForm.abv = entry?.abv === null || entry?.abv === undefined ? '' : String(entry.abv)
  const valueByKey = new Map((entry?.extraFields || []).map((field) => [field.key, field.value]))
  editForm.extraFields = Object.fromEntries(editConfiguredFields.value.map((field) => {
    const rawValue = valueByKey.get(field.fieldKey)
    return [field.fieldKey, normalizeExistingFieldValue(field, rawValue)]
  }))
}

function normalizeExistingFieldValue(field, value) {
  if (field.fieldType === 'multi_select') {
    if (Array.isArray(value)) return value
    if (!hasFieldValue(value)) return []
    return String(value).split('、').map((item) => item.trim()).filter(Boolean)
  }
  if (field.fieldType === 'number') {
    if (!hasFieldValue(value)) return null
    const number = Number(value)
    return Number.isNaN(number) ? null : number
  }
  return value ?? ''
}

async function saveEntryEdit() {
  if (!selectedEntry.value || savingEdit.value) return
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先补全报名资料')
    return
  }
  savingEdit.value = true
  try {
    await updatePortalEntry(selectedEntry.value.id, {
      name: editForm.name,
      style: editForm.style,
      abv: Number(normalizeAbvInput(editForm.abv)),
      extraFields: editForm.extraFields,
    })
    editDialogVisible.value = false
    ElMessage.success('报名资料已更新')
    await refreshEntries(selectedEntry.value.id)
  } catch (error) {
    ElMessage.warning(error?.message || '报名资料保存失败')
  } finally {
    savingEdit.value = false
  }
}

async function cancelSelectedEntry() {
  if (!canCancelSelectedEntry.value) return
  try {
    await ElMessageBox.confirm(
      '取消后，这款酒不会继续参赛；如需参赛，需要重新提交报名',
      '取消报名',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '暂不取消',
        type: 'warning',
      },
    )
    selectedEntry.value = await cancelPortalEntry(selectedEntry.value.id)
    ElMessage.success('报名已取消')
    await refreshEntries(selectedEntry.value.id)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.warning(error?.message || '取消报名失败')
  }
}

async function saveDelivery() {
  if (!selectedEntry.value?.canDownloadLabel) {
    ElMessage.warning('支付成功后才能填写送样信息')
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
    const completedEntryId = selectedEntry.value.id
    editingDelivery.value = false
    await refreshEntries(completedEntryId)
    const nextEntry = batchId
      ? payableEntries.value.find((entry) => entry.id !== completedEntryId && entry.canDownloadLabel && !hasDeliveryProgress(entry))
      : null
    if (nextEntry) {
      ElMessage.success('已保存，继续填写下一款酒')
      await selectEntry(nextEntry, { skipConfirm: true })
    } else {
      ElMessage.success(batchId ? '本批送样信息已全部填写' : '送样信息已提交')
    }
  } finally {
    savingDelivery.value = false
  }
}

async function submitRefundRequest() {
  if (!selectedEntry.value?.canRequestRefund) return
  refundForm.reason = ''
  refundDialogVisible.value = true
}

function editEntryFromRefundDialog() {
  refundDialogVisible.value = false
  openSelectedEntryEdit()
}

async function confirmRefundRequest() {
  if (!selectedEntry.value?.canRequestRefund || refunding.value) return
  refunding.value = true
  try {
    const reason = refundForm.reason.trim() || DEFAULT_REFUND_REASON
    selectedEntry.value = await requestPortalEntryRefund(selectedEntry.value.id, { reason })
    refundDialogVisible.value = false
    labelData.value = null
    ElMessage.success('退款申请已提交')
    await refreshEntries(selectedEntry.value.id)
  } catch (error) {
    ElMessage.warning(error?.message || '退款申请提交失败')
  } finally {
    refunding.value = false
  }
}

async function simulatePayment() {
  if (!selectedEntry.value || selectedEntry.value.paymentStatus === 'PAID') return

  simulatingPayment.value = true
  try {
    selectedEntry.value = await simulatePortalEntryPayment(selectedEntry.value.id)
    ElMessage.success('支付成功，标签下载和送样信息填写已开放')
    await refreshEntries(selectedEntry.value.id)
  } catch (error) {
    ElMessage.warning(error?.message || '支付失败，请稍后重试')
  } finally {
    simulatingPayment.value = false
  }
}

async function startWechatPayment(options = {}) {
  if (isWechatPayEnv.value) {
    await startJsapiPayment(options)
    return
  }
  await startNativePayment(options)
}

async function startNativePayment(options = {}) {
  if (!selectedEntry.value || selectedEntry.value.paymentStatus === 'PAID') return

  creatingPayment.value = true
  try {
    paymentOrder.value = await createPortalEntryWechatNativePayment(selectedEntry.value.id)
    if (paymentOrder.value?.mode === 'WECHAT' && paymentOrder.value.codeUrl) {
      paymentQrDataUrl.value = await QRCode.toDataURL(paymentOrder.value.codeUrl, {
        errorCorrectionLevel: 'M',
        margin: 1,
        width: 220,
      })
      startPaymentPolling()
    }
  } catch (error) {
    if (!options.silent) {
      ElMessage.warning(error?.message || '支付码生成失败，请稍后重试')
    }
  } finally {
    creatingPayment.value = false
  }
}

async function startJsapiPayment(options = {}) {
  if (!selectedEntry.value || selectedEntry.value.paymentStatus === 'PAID') return
  await loadWechatPayConfig()
  if (wechatPayConfig.value?.jsapiConfigured === false || !wechatPayAppId.value) {
    if (!options.silent) {
      ElMessage.warning('微信支付配置未完成，请改用银行转账或稍后再试')
    }
    return
  }
  if (!wechatAuthCode.value) {
    window.location.href = buildWechatOauthUrl(wechatPayAppId.value)
    return
  }

  creatingPayment.value = true
  try {
    paymentOrder.value = await createPortalEntryWechatJsapiPayment(selectedEntry.value.id, {
      code: wechatAuthCode.value,
    })
    clearWechatAuthCode()
    if (paymentOrder.value?.mode === 'MOCK') {
      return
    }
    if (!paymentOrder.value?.payParams) {
      throw new Error('微信支付参数缺失')
    }
    await invokeWechatPay(paymentOrder.value.payParams)
    ElMessage.success('微信支付已提交，请等待结果确认')
    startPaymentPolling()
    await checkPaymentStatus()
  } catch (error) {
    clearWechatAuthCode()
    if (!options.silent) {
      ElMessage.warning(error?.message || '微信支付未完成，请稍后重试')
    }
  } finally {
    creatingPayment.value = false
  }
}

async function loadWechatPayConfig() {
  if (wechatPayConfig.value) return wechatPayConfig.value
  try {
    wechatPayConfig.value = await fetchPortalWechatPayClientConfig()
  } catch {
    wechatPayConfig.value = {
      mode: 'WECHAT',
      appId: WECHAT_PAY_APP_ID,
      jsapiConfigured: Boolean(WECHAT_PAY_APP_ID),
    }
  }
  return wechatPayConfig.value
}

function clearWechatAuthCode() {
  if (!wechatAuthCode.value) return
  wechatAuthCode.value = ''
  window.history.replaceState(null, '', currentUrlWithoutWechatCode())
}

async function checkPaymentStatus() {
  if (!selectedEntry.value) return

  checkingPayment.value = true
  try {
    const status = await fetchPortalEntryPaymentStatus(selectedEntry.value.id)
    if (status.paymentStatus === 'PAID' || status.canDownloadLabel) {
      ElMessage.success('支付成功，标签下载和送样信息填写已开放')
      stopPaymentPolling()
      await refreshEntries(selectedEntry.value.id)
    }
  } catch (error) {
    ElMessage.warning(error?.message || '支付结果查询失败')
  } finally {
    checkingPayment.value = false
  }
}

function startPaymentPolling() {
  stopPaymentPolling()
  paymentPollingCount = 0
  paymentPollingTimer = window.setInterval(async () => {
    paymentPollingCount += 1
    if (paymentPollingCount > 40) {
      stopPaymentPolling()
      return
    }
    await checkPaymentStatus()
  }, 3000)
}

function stopPaymentPolling() {
  if (paymentPollingTimer) {
    window.clearInterval(paymentPollingTimer)
    paymentPollingTimer = null
  }
}

async function switchPayMode(mode) {
  payMode.value = mode
  if (mode === 'BANK_TRANSFER') {
    stopPaymentPolling()
    await ensureBankAccount()
    syncBankTransferForm()
    return
  }
  if (showPaymentCard.value && !showBankPending.value) {
    await startWechatPayment({ silent: true })
  }
}

async function ensureBankAccount() {
  if (bankAccount.value) return
  bankAccount.value = await fetchPortalBankTransferAccount()
}

function syncBankTransferForm() {
  if (!selectedEntry.value || showBankPending.value) return
  bankTransferForm.payerName = ''
  bankTransferForm.remark = ''
  bankTransferForm.voucherAssetId = null
  bankTransferForm.voucherFileName = ''
}

async function uploadBankVoucher(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploadingBankVoucher.value = true
  try {
    const result = await uploadPortalBankTransferVoucher(file)
    bankTransferForm.voucherAssetId = result.fileAssetId
    bankTransferForm.voucherFileName = result.fileName
    ElMessage.success('转账凭证已上传')
  } catch (error) {
    ElMessage.warning(error?.message || '凭证上传失败')
  } finally {
    uploadingBankVoucher.value = false
  }
}

async function submitBankTransferForCurrentEntry() {
  if (!selectedEntry.value) {
    ElMessage.warning('请先选择一款要支付的酒')
    return
  }
  submittingBankTransfer.value = true
  try {
    const payload = {
      entryId: selectedEntry.value.id,
      payerName: bankTransferForm.payerName || undefined,
      remark: bankTransferForm.remark || undefined,
      voucherAssetId: bankTransferForm.voucherAssetId || undefined,
    }
    if (editingBankTransferId.value) {
      await updatePortalBankTransfer(editingBankTransferId.value, payload)
      editingBankTransferId.value = null
      ElMessage.success('转账信息已更新，组委会将按最新内容核对到账')
    } else {
      await submitPortalBankTransfer(payload)
      ElMessage.success('转账信息已提交，我们将在5个工作日内核对到账')
    }
    await refreshEntries(selectedEntry.value?.id)
  } catch (error) {
    ElMessage.warning(error?.message || (editingBankTransferId.value ? '转账信息更新失败' : '转账信息提交失败'))
  } finally {
    submittingBankTransfer.value = false
  }
}

async function editSelectedBankTransfer() {
  const transferId = selectedEntry.value?.payment?.bankTransferId
  if (!transferId) return
  try {
    await ElMessageBox.confirm(
      '修改后将覆盖当前待确认的转账信息，组委会将按最新内容核对到账',
      '修改转账信息',
      {
        confirmButtonText: '继续修改',
        cancelButtonText: '暂不修改',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  cancelingBankTransfer.value = true
  try {
    const transfer = submittedBankTransfer.value || await fetchPortalBankTransfer(transferId)
    submittedBankTransfer.value = transfer
    editingBankTransferId.value = transferId
    payMode.value = 'BANK_TRANSFER'
    bankTransferForm.payerName = transfer.payerName || ''
    bankTransferForm.remark = transfer.remark || ''
    bankTransferForm.voucherAssetId = transfer.voucherAssetId || null
    bankTransferForm.voucherFileName = transfer.voucherFileName || ''
    await ensureBankAccount()
  } catch (error) {
    ElMessage.warning(error?.message || '暂时无法修改转账信息')
  } finally {
    cancelingBankTransfer.value = false
  }
}

function cancelEditingBankTransfer() {
  editingBankTransferId.value = null
  syncBankTransferForm()
}

async function copyBankAccount() {
  await ensureBankAccount()
  const account = bankAccount.value
  const text = [
    `账户名：${account.accountName}`,
    `开户行：${account.bankName}`,
    `账号：${account.accountNo}`,
    account.remarkTip,
    `小秘书微信：${bankServiceWechat.value}`,
  ].filter(Boolean).join('\n')
  await navigator.clipboard.writeText(text)
  ElMessage.success('银行账户信息已复制')
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
  const offsetY = 78

  for (let row = 0; row < matrix.size; row += 1) {
    for (let col = 0; col < matrix.size; col += 1) {
      if (!matrix.data[row * matrix.size + col]) continue
      cells.push(`<rect x="${formatSvgNumber(offsetX + col * cellSize)}" y="${formatSvgNumber(offsetY + row * cellSize)}" width="${formatSvgNumber(cellSize)}" height="${formatSvgNumber(cellSize)}" fill="#2e2014" />`)
    }
  }

  const category = escapeXml(label.categoryName || '组别待确认')
  const code = escapeXml(label.shortCode || 'PENDING')

  return `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 260 360" role="img" aria-label="现场评审标签">
      <rect width="260" height="360" rx="24" fill="#fff8ec"/>
      <rect x="14" y="14" width="232" height="332" rx="18" fill="#fffdf9" stroke="#d4bf9f"/>
      <text x="130" y="44" text-anchor="middle" font-size="12" font-weight="700" letter-spacing="1.4" fill="#8c6330">现场评审标签</text>
      <rect x="28" y="64" width="204" height="204" rx="18" fill="#f7ecd8" stroke="#3a2818" stroke-width="10"/>
      ${cells.join('')}
      <text x="130" y="292" text-anchor="middle" font-size="12" font-weight="700" fill="#8c6330">参赛编号</text>
      <text x="130" y="318" text-anchor="middle" font-size="28" font-weight="900" fill="#24170f">${code}</text>
      <text x="130" y="340" text-anchor="middle" font-size="13" fill="#665647">组别：${category}</text>
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

  labelPngDownloading.value = true
  try {
    const blob = await downloadPortalEntryLabelPng(selectedEntry.value.id)
    triggerDownload(URL.createObjectURL(blob), buildLabelPngFilename(selectedEntry.value))
    ElMessage.success('现场参赛标签 PNG 已开始下载')
  } catch {
    ElMessage.error('标签下载失败，请稍后重试')
  } finally {
    labelPngDownloading.value = false
  }
}

async function downloadLabelPdf() {
  if (!selectedEntry.value?.canDownloadLabel) return

  labelPdfDownloading.value = true
  try {
    const blob = await downloadPortalEntryLabelPdf(selectedEntry.value.id)
    triggerDownload(URL.createObjectURL(blob), buildLabelPdfFilename(selectedEntry.value))
    ElMessage.success('现场参赛标签 PDF 已开始下载')
  } catch {
    ElMessage.error('PDF 下载失败，请稍后重试')
  } finally {
    labelPdfDownloading.value = false
  }
}

function buildLabelPdfFilename(entry) {
  const entryName = safeDownloadFilename(entry?.name || entry?.uuid || 'entry-label')
  const shortCode = safeDownloadFilename(entry?.shortCode || entry?.uuid || 'entry-label')
  return `${entryName}-${shortCode}-现场参赛标签.pdf`
}

function buildLabelPngFilename(entry) {
  const entryName = safeDownloadFilename(entry?.name || entry?.uuid || 'entry-label')
  const shortCode = safeDownloadFilename(entry?.shortCode || entry?.uuid || 'entry-label')
  return `${entryName}-${shortCode}-现场参赛标签.png`
}

function safeDownloadFilename(value) {
  return String(value || 'entry-label').trim().replace(/[\\/:*?"<>|]/g, '_') || 'entry-label'
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
.refund-card,
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

.entry-picker-toggle {
  display: none;
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

.more-button {
  min-width: 56px;
}

.payment-card,
.refund-card,
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

.payment-entry-summary {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.6fr) auto;
  gap: 10px;
  margin: 16px 0 0;
  padding: 12px;
  background: #fffdf8;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.payment-entry-summary div {
  min-width: 0;
}

.payment-entry-summary dd {
  overflow-wrap: anywhere;
}

.payment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-top: 16px;
}

.payment-method-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.payment-method-option {
  position: relative;
  display: grid;
  min-height: 64px;
  padding: 18px 76px 18px 42px;
  text-align: left;
  color: var(--subtle-ink);
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 8px;
  cursor: pointer;
}

.payment-method-option::before {
  position: absolute;
  top: 18px;
  left: 16px;
  width: 15px;
  height: 15px;
  border: 2px solid #c4aa84;
  border-radius: 50%;
  content: '';
}

.payment-method-option.active {
  border-color: rgba(185, 120, 31, 0.45);
  box-shadow: 0 10px 24px rgba(141, 88, 18, 0.1);
}

.payment-method-option.active::before {
  border-color: var(--accent);
  background: radial-gradient(circle, var(--accent) 0 42%, transparent 46%);
}

.payment-method-option.recommended::after {
  position: absolute;
  top: 14px;
  right: 16px;
  padding: 2px 7px;
  color: #7d4c0e;
  background: #f9dfa4;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
  content: '推荐';
}

.payment-method-option-title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 22px;
}

.payment-method-option-title span {
  color: var(--ink);
  font-weight: 900;
  line-height: 1.35;
}

.payment-method-note {
  margin: 0;
  color: var(--subtle-ink);
  font-size: 13px;
  line-height: 1.6;
}

.payment-actions span {
  color: var(--subtle-ink);
  font-size: 13px;
  line-height: 1.5;
}

.wechat-pay-box {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1 1 260px;
  min-width: 0;
  padding: 12px;
  background: #f7f1e7;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.wechat-pay-box img {
  width: 112px;
  height: 112px;
  flex: 0 0 112px;
  background: #fff;
  border-radius: 6px;
}

.wechat-pay-box div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.wechat-pay-box strong {
  color: var(--ink);
  font-size: 16px;
}

.bank-pending-box,
.bank-transfer-panel {
  margin-top: 16px;
}

.bank-pending-box {
  display: grid;
  gap: 12px;
  padding: 14px;
  background: #fff7e6;
  border: 1px solid rgba(185, 120, 31, 0.22);
  border-radius: 8px;
}

.bank-pending-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
}

.bank-pending-head > div {
  display: grid;
  gap: 5px;
}

.bank-pending-box strong {
  color: var(--ink);
}

.bank-pending-box span,
.voucher-row span {
  color: var(--subtle-ink);
  font-size: 13px;
  line-height: 1.55;
}

.bank-submitted-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.bank-submitted-summary div {
  min-width: 0;
  padding: 12px;
  background: #fffdf8;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.bank-submitted-summary dd {
  overflow-wrap: anywhere;
}

.bank-transfer-panel {
  display: grid;
  gap: 12px;
}

.bank-section-title {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 2px;
  color: var(--ink);
  font-weight: 900;
}

.bank-section-title span {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  color: #7d4c0e;
  background: #f9dfa4;
  border-radius: 50%;
  font-size: 12px;
}

.bank-account-box {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.bank-account-box div {
  min-width: 0;
  padding: 12px;
  background: #fffaf1;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.bank-account-box dd {
  overflow-wrap: anywhere;
}

.voucher-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px 12px;
  align-items: center;
  padding-top: 2px;
}

.bank-account-actions {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.bank-account-actions .el-button {
  min-width: 124px;
  height: 40px;
  align-self: stretch;
}

.bank-service-contact {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 40px;
  padding: 10px 12px;
  background: #fffdf8;
  border: 1px dashed rgba(185, 120, 31, 0.28);
  border-radius: 8px;
}

.bank-service-contact span {
  color: var(--subtle-ink);
  font-size: 13px;
  line-height: 1.45;
}

.bank-service-contact b {
  color: var(--ink);
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  text-align: right;
}

.bank-transfer-form {
  display: grid;
  gap: 14px;
  padding-top: 2px;
}

.bank-form-grid {
  display: grid;
  grid-template-columns: minmax(132px, 0.42fr) minmax(260px, 1fr);
  gap: 12px 16px;
  align-items: start;
}

.bank-form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.bank-form-grid :deep(.el-input__wrapper),
.bank-form-grid :deep(.el-date-editor) {
  width: 100%;
}

.bank-payer-field {
  min-width: 0;
}

.bank-remark-field {
  grid-column: 1 / -1;
}

.fixed-amount {
  display: flex;
  align-items: center;
  min-height: 40px;
  padding: 0 14px;
  color: var(--accent-deep);
  background: #fff7e6;
  border: 1px solid rgba(185, 120, 31, 0.18);
  border-radius: 8px;
  font-weight: 900;
}

.bank-submit-actions {
  margin-top: 2px;
}

.hidden-file {
  display: none;
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

.delivery-summary-actions {
  align-items: center;
  margin-top: 16px;
}

.delivery-form-actions {
  margin-top: 18px;
}

.delivery-refresh-button {
  width: 34px;
  min-width: 34px;
  height: 34px;
  padding: 0;
}

.label-actions {
  justify-content: center;
  margin-top: 14px;
}

.fixed-delivery-method {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  color: #5b3710;
  font-weight: 800;
  border: 1px solid rgba(185, 120, 31, 0.18);
  border-radius: 8px;
  background: #fff7e6;
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
  grid-template-columns: minmax(180px, 1fr) minmax(300px, 1.75fr);
  align-items: start;
  gap: 18px 16px;
}

.delivery-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.delivery-method-field {
  grid-column: 1 / -1;
  min-width: 0;
}

.delivery-carrier-field {
  min-width: 0;
}

.delivery-tracking-field {
  min-width: 0;
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

.payment-next-list div {
  background: #fffdf8;
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

.tone-muted {
  color: #6f6252;
  background: #ece3d6;
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

.entry-edit-dialog :deep(.el-dialog) {
  background: #fffaf0;
  border-radius: 8px;
}

.entry-edit-dialog :deep(.el-dialog__header) {
  padding: 22px 24px 8px;
}

.entry-edit-dialog :deep(.el-dialog__title) {
  color: var(--ink);
  font-size: 21px;
  font-weight: 900;
}

.entry-edit-dialog :deep(.el-dialog__body) {
  padding: 12px 24px 6px;
}

.entry-edit-form {
  display: grid;
  gap: 14px;
}

.entry-edit-form :deep(.el-input__wrapper),
.entry-edit-form :deep(.el-textarea__inner),
.entry-edit-form :deep(.el-select__wrapper),
.entry-edit-form :deep(.el-input-number) {
  width: 100%;
  background: #fffdf7;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(87, 58, 26, 0.14) inset;
}

.entry-edit-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.entry-edit-form :deep(.el-form-item__label) {
  color: #514338;
  font-weight: 800;
}

.locked-category {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.13);
  border-radius: 8px;
}

.locked-category span {
  color: var(--accent-deep);
  font-size: 12px;
  font-weight: 900;
}

.locked-category strong {
  color: var(--ink);
  font-size: 18px;
}

.locked-category p,
.field-help {
  margin: 0;
  color: var(--subtle-ink);
  font-size: 13px;
  line-height: 1.55;
}

.edit-form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(150px, 0.65fr);
  gap: 14px;
}

.abv-input-suffix {
  color: #5f4d3d;
  font-weight: 800;
}

.edit-extra-section {
  display: grid;
  gap: 12px;
  padding-top: 4px;
}

.edit-extra-section h4 {
  margin: 0;
  color: var(--ink);
  font-size: 16px;
}

.edit-extra-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.edit-extra-field:has(textarea) {
  grid-column: 1 / -1;
}

.field-label {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 7px;
  align-items: center;
}

.field-label em {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 7px;
  color: #8f5100;
  background: #ffe3a6;
  border-radius: 999px;
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
}

.field-help {
  margin-top: 7px;
  font-size: 12px;
}

.edit-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.edit-dialog-actions :deep(.el-button--primary) {
  border: 0;
  background: linear-gradient(180deg, #d99a30, #a86514);
  font-weight: 900;
}

.refund-confirm-dialog :deep(.el-dialog) {
  background: #fffaf0;
  border-radius: 8px;
}

.refund-confirm-dialog :deep(.el-dialog__header) {
  padding: 22px 24px 8px;
}

.refund-confirm-dialog :deep(.el-dialog__title) {
  color: var(--ink);
  font-size: 21px;
  font-weight: 900;
}

.refund-confirm-dialog :deep(.el-dialog__body) {
  padding: 12px 24px 6px;
}

.refund-confirm-body {
  display: grid;
  gap: 14px;
}

.refund-confirm-body p {
  margin: 0;
  color: var(--ink);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.75;
}

.refund-reason-input :deep(.el-input__wrapper) {
  background: #fffdf7;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(87, 58, 26, 0.14) inset;
}

.refund-reason-input :deep(.el-input__inner::placeholder) {
  color: #a99b89;
}

.refund-dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.refund-cancel-button {
  color: #2b1d10;
  background: #e1a23d;
  border-color: #d69328;
  font-weight: 900;
}

.refund-edit-button,
.refund-confirm-button {
  color: #8b7b67;
  background: rgba(255, 253, 248, 0.72);
  border-color: rgba(87, 58, 26, 0.12);
  font-weight: 900;
}

.refund-confirm-button:hover,
.refund-confirm-button:focus {
  color: #6f6252;
  background: #fffdf8;
  border-color: rgba(87, 58, 26, 0.18);
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
  .payment-page {
    gap: 12px;
    max-width: 100%;
    overflow-x: hidden;
  }

  .page-head,
  .delivery-workbench,
  .inline-facts,
  .summary-list,
  .delivery-grid,
  .bank-account-box,
  .bank-form-grid,
  .payment-entry-summary,
  .bank-submitted-summary,
  .payment-method-options,
  .bank-account-actions,
  .flow-steps {
    grid-template-columns: 1fr;
  }

  .page-head,
  .entry-panel,
  .current-card,
  .refund-card,
  .payment-card,
  .label-card,
  .delivery-card,
  .requirement-aside {
    padding: 16px;
  }

  .page-head {
    gap: 12px;
  }

  .page-title h1 {
    margin-top: 6px;
    font-size: 24px;
  }

  .page-title p,
  .current-card p,
  .card-head p {
    font-size: 13px;
    line-height: 1.55;
  }

  .back-link {
    justify-self: start;
    min-height: 34px;
    padding: 0 10px;
    background: #fff7e6;
    border: 1px solid var(--line);
    border-radius: 8px;
    font-size: 13px;
  }

  .flow-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 7px;
    padding-top: 0;
  }

  .flow-steps span {
    min-height: 38px;
    padding: 8px 9px;
    font-size: 12px;
    line-height: 1.25;
  }

  .flow-steps b {
    flex-basis: 20px;
    width: 20px;
    height: 20px;
    font-size: 11px;
  }

  .delivery-workbench,
  .task-panel,
  .entry-list {
    gap: 10px;
  }

  .edit-form-grid,
  .edit-extra-grid {
    grid-template-columns: 1fr;
  }

  .compact-head {
    align-items: center;
  }

  .entry-picker-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 58px;
    min-height: 34px;
    padding: 0 12px;
    color: var(--accent-deep);
    background: #fff7e6;
    border: 1px solid rgba(185, 120, 31, 0.22);
    border-radius: 8px;
    font: inherit;
    font-size: 13px;
    font-weight: 900;
  }

  .entry-list {
    display: grid;
    overflow: hidden;
    margin-top: 10px;
    padding: 0;
    max-height: none;
  }

  .entry-list.open {
    max-height: 320px;
    overflow-y: auto;
    overscroll-behavior: contain;
    padding-right: 2px;
  }

  .entry-list:not(.open) .entry-row:not(.active) {
    display: none;
  }

  .entry-row {
    padding: 12px 12px 12px 14px;
    width: 100%;
  }

  .entry-row:hover {
    transform: none;
  }

  .entry-row p {
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    font-size: 12px;
  }

  .entry-row small {
    margin-top: 6px;
    font-size: 12px;
  }

  .current-card h2,
  .card-head h3 {
    font-size: 22px;
  }

  .current-card,
  .card-head {
    gap: 10px;
  }

  .payment-entry-summary,
  .bank-submitted-summary,
  .inline-facts,
  .summary-list {
    gap: 8px;
  }

  .payment-entry-summary {
    margin-top: 12px;
    padding: 10px;
  }

  .payment-method-options {
    gap: 8px;
    margin-top: 12px;
  }

  .payment-method-option {
    min-height: 52px;
    padding: 14px 48px 14px 40px;
  }

  .payment-method-option::before {
    top: 16px;
  }

  .payment-method-option.recommended::after {
    top: 12px;
    right: 12px;
  }

  .bank-transfer-panel {
    gap: 10px;
    margin-top: 12px;
  }

  .bank-service-contact {
    align-items: flex-start;
    flex-direction: column;
  }

  .bank-service-contact b {
    text-align: left;
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

@media (max-width: 560px) {
  .flow-steps {
    grid-template-columns: 1fr 1fr;
  }

  .section-kicker {
    font-size: 10px;
  }

  .compact-head h2,
  .aside-head h2 {
    margin-bottom: 0;
    font-size: 20px;
  }

  .card-head strong {
    font-size: 20px;
  }

  .payment-actions,
  .label-actions,
  .button-row,
  .voucher-row {
    display: grid;
    grid-template-columns: 1fr;
    width: 100%;
  }

  .payment-actions .el-button,
  .label-actions .el-button,
  .button-row .el-button,
  .voucher-row .el-button,
  .bank-account-actions .el-button {
    width: 100%;
  }

  .voucher-row span {
    font-size: 12px;
  }

  .bank-account-box div,
  .bank-submitted-summary div,
  .inline-facts div,
  .summary-list div,
  .aside-list div {
    padding: 11px;
  }

  .bank-section-title {
    margin-top: 0;
  }

  .fixed-amount {
    justify-content: center;
  }

  .wechat-pay-box {
    display: grid;
    justify-items: center;
    text-align: center;
  }
}
</style>
