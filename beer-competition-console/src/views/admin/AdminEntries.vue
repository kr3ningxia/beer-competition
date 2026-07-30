<template>
  <div class="admin-entries-page">
    <section class="page-head">
      <div>
        <span>酒款运营</span>
        <h1>酒款管理</h1>
      </div>
      <div class="head-actions">
        <button class="tool-button" type="button" @click="resetFilters">
          <RefreshLeft />
          重置筛选
        </button>
        <button class="tool-button primary" type="button" @click="loadEntries">
          <Refresh />
          刷新
        </button>
      </div>
    </section>

    <section class="filter-panel">
      <label class="field search-field">
        <span>关键词</span>
        <div>
          <Search />
          <input v-model.trim="filters.keyword" placeholder="搜索酒款、厂牌、短编号、运单号" @keyup.enter="applyFilters" />
        </div>
      </label>
      <label class="field">
        <span>比赛</span>
        <select v-model="filters.competitionId" @change="onCompetitionChange">
          <option value="">全部比赛</option>
          <option v-for="item in competitions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>投递组别</span>
        <select v-model="filters.categoryId" :disabled="!categoryOptions.length" @change="applyFilters">
          <option value="">全部组别</option>
          <option v-for="item in categoryOptions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
        </select>
      </label>
      <label class="field">
        <span>报名状态</span>
        <select v-model="filters.status" @change="applyFilters">
          <option value="">全部状态</option>
          <option v-for="item in entryStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <label class="field">
        <span>支付</span>
        <select v-model="filters.paymentStatus" @change="applyFilters">
          <option value="">全部支付状态</option>
          <option value="UNPAID">待支付</option>
          <option value="PENDING_CONFIRM">等待转账确认</option>
          <option value="PAID">已支付</option>
          <option value="REFUNDED">已退款</option>
        </select>
      </label>
      <label class="field">
        <span>入库</span>
        <select v-model="filters.deliveryStatus" @change="applyFilters">
          <option value="">全部入库</option>
          <option value="NOT_SUBMITTED">未提交</option>
          <option value="SUBMITTED">已寄出</option>
          <option value="RECEIVED">已入库</option>
        </select>
      </label>
      <label class="field">
        <span>退款</span>
        <select v-model="filters.refundStatus" @change="applyFilters">
          <option value="">全部退款</option>
          <option value="REQUESTED">待处理</option>
          <option value="APPROVED">待线下退款</option>
          <option value="PROCESSING">已登记打款</option>
          <option value="SUCCESS">已退款</option>
          <option value="REJECTED">已驳回</option>
          <option value="FAILED">退款失败</option>
        </select>
      </label>
      <label class="field">
        <span>分桌</span>
        <select v-model="filters.assigned" @change="applyFilters">
          <option value="">全部</option>
          <option value="true">已分桌</option>
          <option value="false">未分桌</option>
        </select>
      </label>
    </section>

    <section class="table-card">
      <div class="table-title">
        <h2>酒款明细</h2>
        <span>{{ entries.length }} / {{ total }} 款</span>
      </div>
      <div :class="['entries-table', { compact: entries.length < filters.pageSize }]">
        <div class="table-head">
          <span>酒款</span>
          <span>比赛</span>
          <span>组别 / 风格</span>
          <span>支付/退款</span>
          <span>入库</span>
          <span>最近修改</span>
          <span>操作</span>
        </div>
        <div class="table-body">
          <div v-for="entry in entries" :key="entry.id" :class="['table-row', { refunded: isRefundedEntry(entry), 'refund-priority': hasRefundPriority(entry) }]">
            <div class="entry-cell">
              <strong :data-tooltip="entry.name || '未命名酒款'" :title="entry.name || '未命名酒款'">{{ entry.name || '未命名酒款' }}</strong>
              <small :data-tooltip="entry.breweryCompanyName || '未关联厂牌'" :title="entry.breweryCompanyName || '未关联厂牌'">{{ entry.breweryCompanyName || '未关联厂牌' }}</small>
            </div>
            <div class="soft-cell">
              <strong :data-tooltip="entry.competitionName || '-'" :title="entry.competitionName || '-'">{{ entry.competitionName || '-' }}</strong>
              <small :data-tooltip="entry.competitionCode || '-'" :title="entry.competitionCode || '-'">{{ entry.competitionCode || '-' }}</small>
            </div>
            <div class="soft-cell">
              <strong :data-tooltip="entry.categoryName || '-'" :title="entry.categoryName || '-'">{{ entry.categoryName || '-' }}</strong>
              <small :data-tooltip="entry.style || '-'" :title="entry.style || '-'">{{ entry.style || '-' }}</small>
            </div>
            <div class="payment-refund-cell">
              <span :class="['state-pill', paymentRefundTone(entry)]">{{ paymentRefundLabel(entry) }}</span>
              <small v-if="paymentRefundMeta(entry)">{{ paymentRefundMeta(entry) }}</small>
            </div>
            <span :class="['state-pill', deliveryTone(entry.deliveryStatus)]">{{ deliveryLabel(entry.deliveryStatus) }}</span>
            <span class="time-cell">{{ formatTime(entry.lastModifiedAt || entry.submittedAt) }}</span>
            <div class="row-actions">
              <template v-if="hasRefundPriority(entry)">
                <button class="danger priority-action" type="button" @click="openRefundDetail(entry)">退款处理</button>
                <button type="button" @click="openDetail(entry.id, 'profile')">查看</button>
              </template>
              <template v-else-if="isRefundedEntry(entry)">
                <button type="button" @click="openDetail(entry.id, 'profile')">查看</button>
              </template>
              <template v-else>
                <button type="button" @click="openDetail(entry.id, 'profile')">查看</button>
                <button type="button" :disabled="!entry.canEdit" @click="openDetail(entry.id, 'profile', true)">编辑资料</button>
                <button type="button" @click="openDetail(entry.id, 'status')">状态处理</button>
              </template>
            </div>
          </div>
          <div v-if="!loading && !entries.length" class="empty-state">
            <h2>没有符合条件的酒款</h2>
            <p>调整筛选条件后再查看</p>
          </div>
        </div>
      </div>
      <footer class="pagination-bar">
        <span>共 {{ total }} 条</span>
        <div class="pager-buttons">
          <button type="button" :disabled="filters.page <= 1" @click="changePage(filters.page - 1)">上一页</button>
          <button
            v-for="page in visiblePages"
            :key="page"
            :class="{ active: filters.page === page }"
            type="button"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          <button type="button" :disabled="filters.page >= totalPages" @click="changePage(filters.page + 1)">下一页</button>
        </div>
        <label>
          <span>每页</span>
          <select v-model.number="filters.pageSize" @change="changePageSize">
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
      </footer>
    </section>

    <div v-if="detailOpen" class="drawer-mask" @click.self="closeDetail">
      <aside class="entry-drawer">
        <header>
          <div>
            <span>{{ detail?.competitionName || '酒款详情' }}</span>
            <h2>{{ detail?.name || '未命名酒款' }}</h2>
            <small>{{ detail?.breweryCompanyName || '未关联厂牌' }} · {{ detail?.shortCode || detail?.uuid }}</small>
          </div>
          <button type="button" @click="closeDetail">关闭</button>
        </header>

        <nav class="drawer-tabs">
          <button v-for="tab in drawerTabs" :key="tab.key" :class="{ active: activeTab === tab.key }" type="button" @click="activeTab = tab.key">
            {{ tab.label }}
          </button>
        </nav>

        <section v-if="detailLoading" class="drawer-loading">加载中</section>

        <template v-else-if="detail">
          <section v-if="activeTab === 'profile'" class="drawer-panel profile-panel">
            <div class="risk-banner" v-if="detail.assigned || detail.resultPublished">
              <strong>{{ detail.resultPublished ? '结果已发布' : '酒款已分桌' }}</strong>
              <span>{{ detail.resultPublished ? '当前报名信息暂不能修改' : '修改组别或评审可见信息后，需要核对分桌和现场展示' }}</span>
            </div>
            <section class="delivery-summary">
              <div class="section-title">
                <strong>送样信息</strong>
                <span :class="['state-pill', deliveryTone(detail.deliveryStatus)]">{{ deliveryLabel(detail.deliveryStatus) }}</span>
              </div>
              <dl v-if="deliveryInfoItems.length">
                <div v-for="item in deliveryInfoItems" :key="item.label">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                </div>
              </dl>
              <p v-else class="empty-line">厂牌尚未提交送样信息</p>
            </section>
            <div class="form-grid">
              <label>
                <span>酒名</span>
                <input v-model.trim="editForm.name" :disabled="!detail.canEdit" />
              </label>
              <label>
                <span>投递组别</span>
                <select v-model="editForm.categoryId" :disabled="!detail.canEdit">
                  <option v-for="item in detailCategoryOptions" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
                </select>
              </label>
              <label>
                <span>基础风格</span>
                <input v-model.trim="editForm.style" :disabled="!detail.canEdit" />
              </label>
              <label>
                <span>ABV</span>
                <input
                  v-model.trim="editForm.abv"
                  :disabled="!detail.canEdit"
                  inputmode="decimal"
                  placeholder="例如：5.35"
                  @blur="editForm.abv = normalizeAbvInput(editForm.abv)"
                />
              </label>
            </div>
            <section class="extra-fields">
              <label v-for="field in editForm.extraFields" :key="field.key">
                <span>{{ field.label }}</span>
                <input v-model.trim="field.value" :disabled="!detail.canEdit" maxlength="255" />
              </label>
            </section>
            <label class="stack-field">
              <span>修改原因</span>
              <textarea v-model.trim="editForm.reason" :disabled="!detail.canEdit" placeholder="填写后才能保存资料修改"></textarea>
            </label>
            <footer>
              <button class="tool-button primary" type="button" :disabled="!detail.canEdit || saving" @click="saveProfile">保存报名信息</button>
            </footer>
          </section>

          <section v-if="activeTab === 'status'" class="drawer-panel status-panel">
            <div class="status-grid">
              <article>
                <small>报名状态</small>
                <strong>{{ entryStatusLabel(detail.status) }}</strong>
              </article>
              <article>
                <small>支付</small>
                <strong>{{ paymentLabel(detail.paymentStatus) }}</strong>
              </article>
              <article>
                <small>入库</small>
                <strong>{{ deliveryLabel(detail.deliveryStatus) }}</strong>
              </article>
              <article>
                <small>退款</small>
                <strong>{{ refundStatusText(detail.refundStatus, detail) }}</strong>
              </article>
            </div>
            <label class="stack-field">
              <span>处理原因</span>
              <textarea v-model.trim="statusReason" placeholder="可填写现场处理说明"></textarea>
            </label>
            <div class="status-actions">
              <button type="button" :disabled="!detail.canConfirmPayment" @click="runDetailStatusAction('payment')">确认支付</button>
              <button type="button" :disabled="!detail.canMarkStored" @click="runDetailStatusAction('stored')">确认入库</button>
              <button v-if="detail.stored || detail.deliveryStatus === 'RECEIVED'" class="danger" type="button" :disabled="!detail.canUnmarkStored" @click="runDetailStatusAction('unmarkStored')">撤销入库</button>
              <button class="danger" type="button" :disabled="!detail.canCancel" @click="runDetailStatusAction('cancel')">取消报名</button>
            </div>
          </section>

          <section v-if="activeTab === 'refund'" class="drawer-panel refund-panel">
            <div :class="['refund-box', { urgent: hasRefundPriority(detail) }]">
              <div class="refund-box-heading">
                <strong>退款申请</strong>
              </div>
              <p>{{ detail.refundReason || detail.refund?.reason || '未填写退款原因' }}</p>
              <dl>
                <div><dt>退款状态</dt><dd>{{ refundStatusText(detail.refundStatus, detail) }}</dd></div>
                <div><dt>支付方式</dt><dd>{{ paymentMethodLabel(detail.payment?.payMethod) }}</dd></div>
                <div><dt>金额</dt><dd>{{ formatMoney(detail.refund?.amount || detail.payment?.amount) }}</dd></div>
                <div><dt>申请时间</dt><dd>{{ formatTime(detail.refundRequestedAt || detail.refund?.requestedTime) }}</dd></div>
                <div><dt>处理时间</dt><dd>{{ formatTime(detail.refundProcessedAt || detail.refund?.processedTime) }}</dd></div>
              </dl>
              <dl v-if="detail.bankTransfer" class="refund-source">
                <div><dt>原转账单号</dt><dd>{{ detail.bankTransfer.transferNo || '-' }}</dd></div>
                <div><dt>付款人</dt><dd>{{ detail.bankTransfer.payerName || '-' }}</dd></div>
                <div><dt>转账时间</dt><dd>{{ formatTime(detail.bankTransfer.transferTime) }}</dd></div>
                <div><dt>原转账金额</dt><dd>{{ formatMoney(detail.bankTransfer.amount) }}</dd></div>
                <div><dt>付款凭证</dt><dd><a v-if="detail.bankTransfer.voucherPublicUrl" :href="detail.bankTransfer.voucherPublicUrl" target="_blank" rel="noreferrer">查看凭证</a><span v-else>-</span></dd></div>
              </dl>
              <dl v-if="detail.bankTransfer?.entryCount > 1" class="refund-source">
                <div><dt>批量转账</dt><dd>共 {{ detail.bankTransfer.entryCount }} 款，本次退当前酒款 {{ formatMoney(detail.refund?.amount) }}</dd></div>
              </dl>
              <dl v-if="detail.offlineRefundTransferNo" class="refund-source">
                <div><dt>退款收款人</dt><dd>{{ detail.offlineRefundAccountName || '-' }}</dd></div>
                <div><dt>收款银行</dt><dd>{{ detail.offlineRefundBankName || '-' }}</dd></div>
                <div><dt>收款账号</dt><dd>尾号 {{ detail.offlineRefundAccountNoLast4 || '-' }}</dd></div>
                <div><dt>退款流水号</dt><dd>{{ detail.offlineRefundTransferNo }}</dd></div>
                <div><dt>实际打款时间</dt><dd>{{ formatTime(detail.offlineRefundTime) }}</dd></div>
              </dl>
            </div>
            <label class="stack-field">
              <span>处理原因</span>
              <textarea v-model.trim="statusReason" placeholder="可填写退款处理说明"></textarea>
            </label>
            <div class="status-actions">
              <button v-if="detail.canApproveRefund" type="button" @click="runRefundAction('approve')">通过申请</button>
              <button v-if="detail.canRejectRefund" type="button" @click="runRefundAction('reject')">驳回退款</button>
              <button v-if="detail.canRetryRefund" type="button" @click="runRefundAction('retry')">重试退款</button>
              <button v-if="detail.refundStatus === 'APPROVED' && isManualRefundPayment(detail)" type="button" @click="openOfflineRefundDialog">登记打款</button>
              <button v-if="detail.canConfirmOfflineRefund" type="button" @click="runRefundAction('completeOffline')">确认已完成线下退款</button>
            </div>
          </section>

          <section v-if="activeTab === 'label'" class="drawer-panel label-panel">
            <div class="label-preview-card">
              <div class="admin-label-preview" v-html="adminLabelSvg" />
              <button class="tool-button primary" type="button" @click="downloadAdminLabelPng">下载 PNG</button>
            </div>
            <div class="label-meta-grid">
              <article><small>作品编号</small><strong>{{ detail.shortCode || '-' }}</strong></article>
              <article><small>匿名编码</small><strong>{{ detail.uuid }}</strong></article>
              <article><small>投递组别</small><strong>{{ detail.categoryName || '-' }}</strong></article>
              <article><small>基础风格</small><strong>{{ detail.style || '-' }}</strong></article>
            </div>
          </section>

          <section v-if="activeTab === 'trace'" class="drawer-panel trace-panel">
            <div v-for="trace in detail.traces" :key="`${trace.type}-${trace.roundId}-${trace.roundTableId}-${trace.awardName}`" class="trace-row">
              <strong>{{ traceTitle(trace) }}</strong>
              <span>{{ traceText(trace) }}</span>
            </div>
            <p v-if="!detail.traces?.length" class="empty-line">还没有分桌或结果轨迹</p>
          </section>

          <section v-if="activeTab === 'logs'" class="drawer-panel log-panel">
            <div v-for="log in detail.logs" :key="log.id" class="log-row">
              <strong>{{ actionLabel(log.action) }}</strong>
              <small>{{ formatTime(log.createTime) }} · 管理员 {{ log.adminUserId || '-' }}</small>
              <p>{{ formatLogSummary(log.summary) }}</p>
            </div>
            <p v-if="!detail.logs?.length" class="empty-line">暂无修改记录</p>
          </section>
          <details class="danger-zone">
            <summary>危险操作</summary>
            <p>仅在确需移除酒款时使用，操作需要短编号确认并记录原因。</p>
            <button class="danger" type="button" @click="openDeleteDialog(detail)">删除酒款</button>
          </details>
        </template>
      </aside>
    </div>

    <div v-if="offlineRefundDialog.open" class="stage-confirm-backdrop" @click.self="closeOfflineRefundDialog">
      <section class="stage-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="offline-refund-title">
        <header><span class="confirm-kicker">线下退款</span><h2 id="offline-refund-title">登记打款</h2></header>
        <div class="confirm-summary">
          <span><small>酒款</small><strong>{{ detail?.name || '-' }}</strong></span>
          <span><small>退款金额</small><strong>{{ formatMoney(detail?.refund?.amount) }}</strong></span>
        </div>
        <div class="confirm-reason">
          <span>打款凭证</span>
          <div class="confirm-file-picker">
            <input
              id="offline-refund-voucher"
              class="confirm-file-input"
              type="file"
              accept="image/*,.pdf"
              @change="offlineRefundDialog.voucher = $event.target.files?.[0] || null"
            />
            <label class="confirm-file-button" for="offline-refund-voucher">选择凭证</label>
            <span class="confirm-file-name" :class="{ selected: offlineRefundDialog.voucher }">
              {{ offlineRefundDialog.voucher?.name || '未选择文件' }}
            </span>
          </div>
        </div>
        <label class="confirm-reason"><span>备注（选填）</span><textarea v-model.trim="offlineRefundDialog.reason" maxlength="300"></textarea></label>
        <footer><button class="confirm-button ghost" type="button" :disabled="offlineRefundDialog.loading" @click="closeOfflineRefundDialog">取消</button><button class="confirm-button primary" type="button" :disabled="offlineRefundDialog.loading || !canSubmitOfflineRefund" @click="submitOfflineRefund">{{ offlineRefundDialog.loading ? '保存中' : '保存并登记' }}</button></footer>
      </section>
    </div>

    <div v-if="entryConfirm.open" class="stage-confirm-backdrop" @click.self="closeEntryConfirm">
      <section class="stage-confirm-dialog warning" role="dialog" aria-modal="true" aria-labelledby="entry-confirm-title">
        <header>
          <span class="confirm-kicker">{{ entryConfirm.kicker }}</span>
          <h2 id="entry-confirm-title">{{ entryConfirm.title }}</h2>
        </header>
        <p class="confirm-copy">{{ entryConfirm.copy }}</p>
        <div class="confirm-summary">
          <span v-for="item in entryConfirm.summary" :key="item.label">
            <small>{{ item.label }}</small>
            <strong>{{ item.value }}</strong>
          </span>
        </div>
        <label v-if="entryConfirm.reasonLabel" class="confirm-reason">
          <span>{{ entryConfirm.reasonLabel }}</span>
          <textarea v-model.trim="entryConfirm.reason" :placeholder="entryConfirm.reasonPlaceholder" maxlength="255"></textarea>
        </label>
        <footer>
          <button class="confirm-button ghost" type="button" :disabled="entryConfirm.loading" @click="closeEntryConfirm">取消</button>
          <button class="confirm-button primary" type="button" :disabled="entryConfirm.loading || entryConfirmReasonInvalid" @click="confirmEntryAction">
            {{ entryConfirm.loading ? entryConfirm.loadingText : entryConfirm.confirmText }}
          </button>
        </footer>
      </section>
    </div>

    <div v-if="deleteDialog.open" class="stage-confirm-backdrop" @click.self="closeDeleteDialog">
      <section class="stage-confirm-dialog danger-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-entry-title">
        <header><span class="confirm-kicker">危险操作</span><h2 id="delete-entry-title">删除酒款</h2></header>
        <p class="confirm-copy">删除后酒款会退出报名、入库、分桌、评审和结果流程，财务与操作记录仍会保留。</p>
        <div v-if="deleteDialog.impact" class="confirm-summary">
          <span><small>酒款</small><strong>{{ deleteDialog.impact.entryName }}</strong></span>
          <span><small>支付</small><strong>{{ paymentLabel(deleteDialog.impact.paymentStatus) }}</strong></span>
          <span><small>分桌/评分</small><strong>{{ deleteDialog.impact.roundAssignmentCount }}/{{ deleteDialog.impact.scoreRecordCount }}</strong></span>
          <span><small>结果/奖项</small><strong>{{ deleteDialog.impact.roundResultCount }}/{{ deleteDialog.impact.awardResultCount }}</strong></span>
        </div>
        <label class="confirm-reason"><span>删除原因</span><textarea v-model.trim="deleteDialog.reason" maxlength="500" placeholder="请填写删除原因"></textarea></label>
        <label class="confirm-reason"><span>输入短编号：{{ deleteDialog.impact?.shortCode || '-' }}</span><input v-model.trim="deleteDialog.confirmationCode" placeholder="输入酒款短编号" /></label>
        <label v-if="deleteDialog.impact?.paymentStatus === 'PAID'" class="confirm-check"><input v-model="deleteDialog.manualRefunded" type="checkbox" /> 我已完成退款，并确认保留财务记录</label>
        <label v-if="deleteDialog.impact?.highRisk" class="confirm-check"><input v-model="deleteDialog.highRiskConfirmed" type="checkbox" /> 我确认这是一项高风险删除操作</label>
        <footer><button class="confirm-button ghost" type="button" :disabled="deleteDialog.loading" @click="closeDeleteDialog">取消</button><button class="confirm-button primary danger" type="button" :disabled="deleteDialog.loading || !canSubmitDelete" @click="confirmDeleteEntry">{{ deleteDialog.loading ? '删除中' : '确认删除' }}</button></footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import {
  approveEntryRefund,
  cancelEntry,
  confirmOfflineEntryRefund,
  registerOfflineEntryRefund,
  confirmEntryPayment,
  fetchAdminEntries,
  fetchAdminEntryDetail,
  fetchAdminEntryDeleteImpact,
  fetchCompetitionDetail,
  fetchCompetitions,
  markEntryStored,
  rejectEntryRefund,
  retryEntryRefund,
  unmarkEntryStored,
  updateAdminEntry,
  administrativelyDeleteEntry,
} from '@/api/admin'
import { JUDGE_H5_BASE_URL } from '@/config'
import { isValidAbvInput, normalizeAbvInput } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()
const entries = ref([])
const total = ref(0)
const competitions = ref([])
const categoryOptions = ref([])
const detailCategoryOptions = ref([])
const loading = ref(false)
const detailLoading = ref(false)
const detailOpen = ref(false)
const detail = ref(null)
const activeTab = ref('profile')
const saving = ref(false)
const statusReason = ref('')
const offlineRefundDialog = reactive({ open: false, loading: false, voucher: null, reason: '' })
const canSubmitOfflineRefund = computed(() => Boolean(offlineRefundDialog.voucher))
const entryConfirm = reactive({
  open: false,
  loading: false,
  action: '',
  kicker: '',
  title: '',
  copy: '',
  summary: [],
  confirmText: '确认',
  loadingText: '处理中',
  reasonLabel: '',
  reasonPlaceholder: '',
  reasonRequired: false,
  reason: '',
  payload: null,
})
const deleteDialog = reactive({ open: false, loading: false, impact: null, entryId: null, reason: '', confirmationCode: '', manualRefunded: false, highRiskConfirmed: false })
const canSubmitDelete = computed(() => Boolean(deleteDialog.reason.trim() && deleteDialog.confirmationCode.trim()
  && (!deleteDialog.impact?.highRisk || deleteDialog.highRiskConfirmed)
  && (deleteDialog.impact?.paymentStatus !== 'PAID' || deleteDialog.manualRefunded)))

const filters = reactive({
  competitionId: '',
  status: '',
  paymentStatus: '',
  deliveryStatus: '',
  refundStatus: '',
  categoryId: '',
  assigned: '',
  keyword: '',
  page: 1,
  pageSize: 20,
})

const editForm = reactive({
  name: '',
  categoryId: '',
  style: '',
  abv: '',
  reason: '',
  extraFields: [],
})

const entryStatusOptions = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'REGISTERED', label: '报名成功' },
  { value: 'STORED', label: '已入库' },
  { value: 'RESULT_PUBLISHED', label: '结果已出' },
]

const totalPages = computed(() => Math.max(Math.ceil(total.value / filters.pageSize), 1))
const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, filters.page - 2)
  const end = Math.min(totalPages.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})
const drawerTabs = computed(() => {
  const tabs = [
    { key: 'profile', label: '报名信息' },
    { key: 'status', label: '状态处理' },
  ]
  if (detail.value?.refundStatus) {
    tabs.push({ key: 'refund', label: '退款处理' })
  }
  tabs.push(
    { key: 'label', label: '二维码' },
    { key: 'trace', label: '分桌轨迹' },
    { key: 'logs', label: '修改记录' },
  )
  return tabs
})
const adminLabelRecord = computed(() => ({
  uuid: detail.value?.uuid || '',
  labelCode: detail.value?.labelCode || '',
  shortCode: detail.value?.shortCode || '',
  scanToken: detail.value?.scanToken || '',
  categoryName: detail.value?.categoryName || '',
  style: detail.value?.style || '',
  abv: detail.value?.abv ?? '',
}))
const entryConfirmReasonInvalid = computed(() => Boolean(entryConfirm.reasonRequired && !entryConfirm.reason.trim()))
const adminLabelSvg = computed(() => buildAdminLabelSvg(adminLabelRecord.value))
const deliveryInfoItems = computed(() => {
  const current = detail.value
  if (!current) return []
  const delivery = current.delivery || {}
  return [
    {
      label: '送样方式',
      value: deliveryMethodLabel(delivery.deliveryMethod || current.deliveryMethod),
    },
    {
      label: '快递公司',
      value: delivery.carrier || current.carrier,
    },
    {
      label: '快递单号',
      value: delivery.trackingNo || current.trackingNo,
    },
    {
      label: '提交时间',
      value: formatTime(delivery.submittedTime || current.deliverySubmittedAt),
    },
    {
      label: '签收时间',
      value: formatTime(delivery.receivedTime || current.deliveryReceivedAt),
    },
    {
      label: '送样备注',
      value: delivery.deliveryNote || current.deliveryNote,
    },
  ].filter((item) => item.value && item.value !== '-')
})

watch(() => route.query.entryId, (entryId) => {
  if (!entryId) return
  const tab = normalizeDetailTab(route.query.entryTab)
  if (detailOpen.value && detail.value?.id === Number(entryId)) {
    activeTab.value = tab
    return
  }
  openDetail(Number(entryId), tab)
})

onMounted(async () => {
  await loadCompetitions()
  applyQuery()
  if (filters.competitionId) await loadCategoryOptions(filters.competitionId)
  await loadEntries()
  if (route.query.entryId) await openDetail(Number(route.query.entryId), normalizeDetailTab(route.query.entryTab))
})

async function loadCompetitions() {
  competitions.value = await fetchCompetitions({ includeArchived: false })
}

function applyQuery() {
  if (route.query.competitionId && competitions.value.some((item) => String(item.id) === String(route.query.competitionId))) {
    filters.competitionId = String(route.query.competitionId)
  }
  if (route.query.keyword) filters.keyword = String(route.query.keyword)
}

async function onCompetitionChange() {
  filters.categoryId = ''
  await loadCategoryOptions(filters.competitionId)
  applyFilters()
}

async function loadCategoryOptions(competitionId) {
  if (!competitionId) {
    categoryOptions.value = []
    return
  }
  const data = await fetchCompetitionDetail(competitionId)
  categoryOptions.value = data.categories || []
}

function applyFilters() {
  filters.page = 1
  loadEntries()
}

function changePage(page) {
  const next = Math.min(Math.max(page, 1), totalPages.value)
  if (next === filters.page) return
  filters.page = next
  loadEntries()
}

function changePageSize() {
  filters.page = 1
  loadEntries()
}

function resetFilters() {
  Object.assign(filters, { competitionId: '', status: '', paymentStatus: '', deliveryStatus: '', refundStatus: '', categoryId: '', assigned: '', keyword: '', page: 1, pageSize: 20 })
  categoryOptions.value = []
  router.replace('/admin/entries')
  loadEntries()
}

async function loadEntries() {
  loading.value = true
  try {
    const params = {
      page: filters.page,
      pageSize: filters.pageSize,
      competitionId: filters.competitionId || undefined,
      status: filters.status || undefined,
      paymentStatus: filters.paymentStatus || undefined,
      deliveryStatus: filters.deliveryStatus || undefined,
      refundStatus: filters.refundStatus || undefined,
      categoryId: filters.categoryId || undefined,
      assigned: filters.assigned === '' ? undefined : filters.assigned === 'true',
      keyword: filters.keyword || undefined,
    }
    const data = await fetchAdminEntries(params)
    entries.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(entryId, tab = 'profile', edit = false) {
  detailOpen.value = true
  activeTab.value = tab
  detailLoading.value = true
  try {
    detail.value = await fetchAdminEntryDetail(entryId)
    await loadDetailCategories(detail.value.competitionId)
    syncEditForm()
    statusReason.value = ''
    if (edit) activeTab.value = 'profile'
    router.replace({
      path: '/admin/entries',
      query: {
        ...route.query,
        competitionId: filters.competitionId || detail.value.competitionId,
        entryId,
        entryTab: activeTab.value,
      },
    })
  } finally {
    detailLoading.value = false
  }
}

async function openDeleteDialog(entry) {
  deleteDialog.open = true
  deleteDialog.loading = true
  deleteDialog.entryId = entry.id
  deleteDialog.impact = null
  deleteDialog.reason = ''
  deleteDialog.confirmationCode = ''
  deleteDialog.manualRefunded = false
  deleteDialog.highRiskConfirmed = false
  try {
    deleteDialog.impact = await fetchAdminEntryDeleteImpact(entry.id)
  } finally {
    deleteDialog.loading = false
  }
}

function closeDeleteDialog() {
  if (deleteDialog.loading) return
  deleteDialog.open = false
}

async function confirmDeleteEntry() {
  if (!canSubmitDelete.value) return
  deleteDialog.loading = true
  try {
    await administrativelyDeleteEntry(deleteDialog.entryId, {
      reason: deleteDialog.reason,
      confirmationCode: deleteDialog.confirmationCode,
      paymentDisposition: deleteDialog.manualRefunded ? 'MANUAL_REFUNDED' : 'NONE',
      highRiskConfirmed: deleteDialog.highRiskConfirmed,
    })
    const deletedId = deleteDialog.entryId
    deleteDialog.open = false
    if (detail.value?.id === deletedId) closeDetail()
    await loadEntries()
    ElMessage.success('酒款已删除')
  } finally {
    deleteDialog.loading = false
  }
}

function normalizeDetailTab(tab) {
  return ['profile', 'status', 'refund', 'label', 'trace', 'logs'].includes(tab) ? tab : 'profile'
}

async function loadDetailCategories(competitionId) {
  const data = await fetchCompetitionDetail(competitionId)
  detailCategoryOptions.value = data.categories || []
}

function closeDetail() {
  detailOpen.value = false
  detail.value = null
  const query = { ...route.query }
  delete query.entryId
  delete query.entryTab
  router.replace({ path: '/admin/entries', query })
}

function syncEditForm() {
  Object.assign(editForm, {
    name: detail.value?.name || '',
    categoryId: detail.value?.categoryId ? String(detail.value.categoryId) : '',
    style: detail.value?.style || '',
    abv: detail.value?.abv == null ? '' : String(detail.value.abv),
    reason: '',
    extraFields: (detail.value?.extraFields || []).map((item) => ({ ...item })),
  })
}

async function saveProfile() {
  if (!editForm.reason.trim()) {
    ElMessage.warning('请填写修改原因')
    return
  }
  if (!isValidAbvInput(editForm.abv)) {
    ElMessage.warning('请填写 0-99.99 之间的 ABV，最多两位小数')
    return
  }
  if (detail.value.assigned) {
    try {
      await ElMessageBox.confirm('这款酒已经分桌，保存后请核对分桌、晋级和奖项归属', '确认修改？', {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      return
    }
  }
  saving.value = true
  try {
    const extraFields = {}
    editForm.extraFields.forEach((field) => {
      extraFields[field.key] = field.value
    })
    detail.value = await updateAdminEntry(detail.value.id, {
      name: editForm.name,
      categoryId: Number(editForm.categoryId),
      style: editForm.style,
      abv: Number(normalizeAbvInput(editForm.abv)),
      extraFields,
      reason: editForm.reason,
    })
    syncEditForm()
    await loadEntries()
    ElMessage.success('报名信息已保存')
  } finally {
    saving.value = false
  }
}

async function runStatusAction(entry, type) {
  await openDetail(entry.id, 'status')
  await runDetailStatusAction(type)
}

function hasRefundPriority(entry) {
  return ['REQUESTED', 'FAILED'].includes(entry?.refundStatus) || Boolean(entry?.canConfirmOfflineRefund)
}

function isRefundedEntry(entry) {
  return entry?.refundStatus === 'SUCCESS' || entry?.paymentStatus === 'REFUNDED'
}

async function openRefundDetail(entry) {
  await openDetail(entry.id, 'refund')
}

function openOfflineRefundDialog() {
  Object.assign(offlineRefundDialog, {
    open: true,
    loading: false,
    voucher: null,
    reason: '',
  })
}

function closeOfflineRefundDialog() {
  if (!offlineRefundDialog.loading) offlineRefundDialog.open = false
}

async function submitOfflineRefund() {
  if (!canSubmitOfflineRefund.value || !detail.value?.refund?.id) return
  offlineRefundDialog.loading = true
  try {
    await registerOfflineEntryRefund(detail.value.refund.id, {
      reason: offlineRefundDialog.reason,
      voucher: offlineRefundDialog.voucher,
    })
    offlineRefundDialog.open = false
    detail.value = await fetchAdminEntryDetail(detail.value.id)
    await loadEntries()
    ElMessage.success('线下打款已登记')
  } finally {
    offlineRefundDialog.loading = false
  }
}

function entrySummaryItems(current = detail.value, extra = []) {
  return [
    { label: '酒款', value: current?.name || current?.shortCode || current?.uuid || '-' },
    { label: '厂牌', value: current?.breweryCompanyName || '-' },
    { label: '当前状态', value: `${entryStatusLabel(current?.status)} / ${deliveryLabel(current?.deliveryStatus)}` },
    ...extra,
  ]
}

function openEntryConfirm(config) {
  Object.assign(entryConfirm, {
    open: true,
    loading: false,
    action: '',
    kicker: '',
    title: '',
    copy: '',
    summary: [],
    confirmText: '确认',
    loadingText: '处理中',
    reasonLabel: '',
    reasonPlaceholder: '',
    reasonRequired: false,
    reason: statusReason.value || '',
    payload: null,
    ...config,
  })
}

function closeEntryConfirm() {
  if (entryConfirm.loading) return
  entryConfirm.open = false
}

async function confirmEntryAction() {
  if (!entryConfirm.action || entryConfirmReasonInvalid.value) return
  entryConfirm.loading = true
  try {
    if (entryConfirm.action === 'status') await executeDetailStatusAction(entryConfirm.payload?.type, entryConfirm.reason)
    if (entryConfirm.action === 'refund') await executeRefundAction(entryConfirm.payload?.type, entryConfirm.reason)
    entryConfirm.open = false
  } finally {
    entryConfirm.loading = false
  }
}

async function runDetailStatusAction(type) {
  const current = detail.value
  if (!current) return
  if (type === 'unmarkStored' && !statusReason.value.trim()) {
    ElMessage.warning('请填写撤销入库原因')
    return
  }
  if (type === 'cancel') {
    openEntryConfirm({
      action: 'status',
      kicker: '报名处理',
      title: '确认取消报名？',
      copy: '取消后，该酒款会退出后续分桌、评审和结果流程；已产生的记录仍会保留用于追溯',
      summary: entrySummaryItems(current, [
        { label: '支付', value: paymentLabel(current.paymentStatus) },
        { label: '分桌', value: current.assigned ? '已分桌' : '未分桌' },
      ]),
      confirmText: '确认取消',
      loadingText: '取消中',
      reasonLabel: '处理原因',
      reasonPlaceholder: '可填写取消报名的现场说明',
      payload: { type },
    })
    return
  }
  if (type === 'unmarkStored') {
    openEntryConfirm({
      action: 'status',
      kicker: '入库撤销',
      title: '确认撤销入库？',
      copy: '撤销后，该酒款将回到已寄出状态，需要重新确认入库后才能继续现场流程',
      summary: entrySummaryItems(current, [
        { label: '入库', value: deliveryLabel(current.deliveryStatus) },
        { label: '短编号', value: current.shortCode || '-' },
      ]),
      confirmText: '撤销入库',
      loadingText: '撤销中',
      reasonLabel: '撤销原因',
      reasonPlaceholder: '请填写撤销入库原因',
      reasonRequired: true,
      payload: { type },
    })
    return
  }
  await executeDetailStatusAction(type, statusReason.value)
}

async function executeDetailStatusAction(type, reason = statusReason.value) {
  const current = detail.value
  if (!current) return
  const payload = { reason }
  if (type === 'payment') await confirmEntryPayment(current.id, payload)
  if (type === 'stored') await markEntryStored(current.id, payload)
  if (type === 'unmarkStored') await unmarkEntryStored(current.id, payload)
  if (type === 'cancel') await cancelEntry(current.id, payload)
  detail.value = await fetchAdminEntryDetail(current.id)
  syncEditForm()
  await loadEntries()
  ElMessage.success(statusActionLabel(type))
}

async function runRefundAction(type) {
  const current = detail.value
  const refundId = current?.refund?.id
  if (!current || !refundId) return
  if (['approve', 'retry', 'completeOffline'].includes(type)) {
    openEntryConfirm({
      action: 'refund',
      kicker: '退款处理',
      title: refundConfirmTitle(current, type),
      copy: refundConfirmCopy(current, type),
      summary: entrySummaryItems(current, [
        { label: '支付方式', value: paymentMethodLabel(current.payment?.payMethod) },
        { label: '退款状态', value: refundStatusText(current.refundStatus, current) },
        { label: '退款金额', value: formatMoney(current.refund?.amount || current.payment?.amount) },
      ]),
      confirmText: type === 'retry'
        ? '重试退款'
        : type === 'completeOffline'
          ? '确认已完成'
          : '通过申请',
      loadingText: type === 'retry' ? '重试中' : '确认中',
      reasonLabel: '处理原因',
      reasonPlaceholder: '可填写退款处理说明',
      payload: { type },
    })
    return
  }
  await executeRefundAction(type, statusReason.value)
}

async function executeRefundAction(type, reason = statusReason.value) {
  const current = detail.value
  const refundId = current?.refund?.id
  if (!current || !refundId) return
  const payload = { reason }
  if (type === 'approve') await approveEntryRefund(refundId, payload)
  if (type === 'reject') await rejectEntryRefund(refundId, payload)
  if (type === 'retry') await retryEntryRefund(refundId, payload)
  if (type === 'completeOffline') await confirmOfflineEntryRefund(refundId, payload)
  detail.value = await fetchAdminEntryDetail(current.id)
  syncEditForm()
  await loadEntries()
  ElMessage.success(refundActionLabel(type))
}

function refundActionLabel(type) {
  if (type === 'approve') return '退款申请已通过'
  if (type === 'completeOffline') return '线下退款已确认'
  return type === 'reject' ? '退款已驳回' : '退款已重试'
}

function statusActionLabel(type) {
  if (type === 'payment') return '支付已确认'
  if (type === 'stored') return '入库已确认'
  if (type === 'unmarkStored') return '入库已撤销'
  return '报名已取消'
}

function entryStatusLabel(value) {
  return entryStatusOptions.find((item) => item.value === value)?.label || value || '-'
}

function paymentLabel(value) {
  return { UNPAID: '待支付', PENDING_CONFIRM: '等待转账确认', PAID: '已支付', REFUNDED: '已退款', CANCELED: '已取消' }[value] || value || '-'
}

function paymentMethodLabel(value) {
  return { WECHAT: '微信支付', BANK_TRANSFER: '银行转账', MANUAL: '人工确认', MOCK: '测试支付' }[value] || '未记录'
}

function refundStatusText(value, entry) {
  if (value === 'APPROVED' && (entry?.canConfirmOfflineRefund || isManualRefundPayment(entry))) return '待线下退款'
  return {
    REQUESTED: '待处理',
    APPROVED: '处理中',
    PROCESSING: '已登记打款，待确认',
    SUCCESS: '已退款',
    FAILED: '退款失败',
    REJECTED: '已驳回',
  }[value] || '无退款'
}

function paymentRefundLabel(entry) {
  if (!entry?.refundStatus) return paymentLabel(entry?.paymentStatus)
  if (entry.refundStatus === 'APPROVED' && entry.canConfirmOfflineRefund) return '待线下退款'
  return {
    REQUESTED: '待退款审核',
    APPROVED: '退款处理中',
    PROCESSING: '已登记打款，待确认',
    SUCCESS: '已退款',
    FAILED: '退款失败',
    REJECTED: '退款已驳回',
  }[entry.refundStatus] || refundStatusText(entry.refundStatus, entry)
}

function paymentRefundMeta(entry) {
  if (!entry?.refundStatus) return ''
  if (entry.refundStatus === 'REQUESTED') return '等待处理'
  if (entry.refundStatus === 'APPROVED' && entry.canConfirmOfflineRefund) return '等待线下退款'
  if (entry.refundStatus === 'APPROVED') return '待线下退款'
  if (entry.refundStatus === 'PROCESSING') return '已登记打款'
  if (entry.refundStatus === 'SUCCESS') return '报名已取消'
  if (entry.refundStatus === 'FAILED') return '需要重试'
  if (entry.refundStatus === 'REJECTED') return '支付仍有效'
  return ''
}

function isManualRefundPayment(entry) {
  return ['BANK_TRANSFER', 'MANUAL'].includes(entry?.payment?.payMethod)
}

function refundConfirmTitle(entry, type) {
  if (type === 'retry') return '确认重试退款？'
  if (type === 'completeOffline') return '确认线下退款已完成？'
  return '确认通过退款申请？'
}

function refundConfirmCopy(entry, type) {
  if (type === 'retry') return '将重新提交微信退款，退款成功后，这款酒会取消报名并退出后续流程'
  if (type === 'completeOffline') return '确认后，这款酒会取消报名，支付记录将更新为已退款'
  if (isManualRefundPayment(entry)) return '通过后进入待线下退款，实际转账完成后还需再次确认'
  return '通过后将提交微信退款，退款成功后这款酒会取消报名'
}

function deliveryLabel(value) {
  return { NOT_SUBMITTED: '未提交', SUBMITTED: '已寄出', RECEIVED: '已入库' }[value] || value || '-'
}

function deliveryMethodLabel(value) {
  return { EXPRESS: '快递寄送', ONSITE: '现场送样' }[value] || ''
}

function paymentTone(value) {
  if (value === 'PENDING_CONFIRM') return 'warning'
  return value === 'PAID' ? 'success' : value === 'UNPAID' ? 'warning' : value === 'REFUNDED' ? 'success' : 'muted'
}

function refundTone(value) {
  if (value === 'SUCCESS') return 'success'
  if (['REQUESTED', 'APPROVED', 'PROCESSING'].includes(value)) return 'warning'
  if (value === 'FAILED') return 'danger'
  return 'muted'
}

function paymentRefundTone(entry) {
  if (!entry?.refundStatus) return paymentTone(entry?.paymentStatus)
  return refundTone(entry.refundStatus)
}

function deliveryTone(value) {
  return value === 'RECEIVED' ? 'success' : value === 'SUBMITTED' ? 'warning' : 'muted'
}

function traceTitle(trace) {
  if (trace.type === 'AWARD') return trace.awardName || '奖项'
  if (trace.type === 'RESULT') return trace.slotLabel || '轮次结果'
  return [trace.roundName, trace.tableName].filter(Boolean).join(' · ') || '分桌记录'
}

function traceText(trace) {
  if (trace.type === 'AWARD') return [trace.awardType, trace.rankNo ? `第 ${trace.rankNo}` : ''].filter(Boolean).join(' · ')
  if (trace.type === 'RESULT') return [trace.roundName, trace.tableName, trace.resultType].filter(Boolean).join(' · ')
  return trace.status || '已分配'
}

function actionLabel(action) {
  return {
    ENTRY_UPDATE: '编辑报名信息',
    ENTRY_CONFIRM_PAYMENT: '确认支付',
    ENTRY_BANK_TRANSFER_CONFIRM: '确认银行转账',
    ENTRY_BANK_TRANSFER_REJECT: '驳回银行转账',
    ENTRY_MARK_STORED: '确认入库',
    ENTRY_UNMARK_STORED: '撤销入库',
    ENTRY_CANCEL: '取消报名',
  }[action] || action
}

function formatLogSummary(summary) {
  if (!summary) return ''
  try {
    const data = JSON.parse(summary)
    if (Array.isArray(data.changes)) {
      const changed = data.changes.map((item) => item.field).join('、')
      return `${data.reason || '未填写原因'}${changed ? ` · 修改：${changed}` : ''}`
    }
    return data.reason || data.action || summary
  } catch {
    return summary
  }
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `¥${Number(value).toFixed(2)}`
}

function buildAdminLabelSvg(label) {
  const qr = QRCode.create(adminScanUrl(label), { errorCorrectionLevel: 'M', margin: 0 })
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

function adminScanUrl(label) {
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

async function downloadAdminLabelPng() {
  if (!detail.value) return

  try {
    const svgBlob = new Blob([adminLabelSvg.value], { type: 'image/svg+xml;charset=utf-8' })
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

    if (!pngBlob) throw new Error('现场标签生成失败')

    triggerDownload(URL.createObjectURL(pngBlob), `${detail.value.shortCode || 'entry-label'}.png`)
    ElMessage.success('现场标签已开始下载')
  } catch {
    ElMessage.error('标签下载失败，请稍后重试')
  }
}

function triggerDownload(objectUrl, fileName) {
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}
</script>

<style scoped>
.admin-entries-page {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 28px 28px 18px;
  overflow: hidden;
  color: #e9f2f5;
  background:
    linear-gradient(rgba(218, 232, 237, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(218, 232, 237, 0.035) 1px, transparent 1px),
    #0b1216;
  background-size: 68px 68px;
}

.page-head,
.filter-panel,
.table-card {
  width: 100%;
  max-width: none;
  margin: 0 auto;
}

.page-head {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(218, 232, 237, 0.1);
}

.page-head span {
  color: #d8ae3f;
  font-size: 13px;
  font-weight: 700;
}

.page-head h1 {
  margin: 5px 0 0;
  font-size: 30px;
  line-height: 1.12;
}

.head-actions,
.row-actions,
.status-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tool-button,
.row-actions button,
.status-actions button,
.entry-drawer header button {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 6px;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  min-width: max-content;
  padding: 0 13px;
  color: #d5e2e7;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
}

.tool-button svg {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
}

.tool-button.primary,
.status-actions button:not(.danger) {
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.12);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.danger,
.row-actions button.danger,
.status-actions button.danger {
  color: #ffaaa0;
  border-color: rgba(255, 122, 107, 0.25);
  background: rgba(255, 122, 107, 0.08);
}

.row-actions button.priority-action {
  color: #ffd4cf;
  border-color: rgba(255, 122, 107, 0.42);
  background: rgba(255, 122, 107, 0.16);
}

.stage-confirm-backdrop {
  --confirm-panel: #111b1f;
  --confirm-line: rgba(219, 232, 237, 0.13);
  --confirm-text: #edf4f6;
  --confirm-muted: #8ea1aa;
  --confirm-gold: #e0b84a;
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(1, 7, 9, 0.72);
  backdrop-filter: blur(10px);
}

.stage-confirm-dialog {
  box-sizing: border-box;
  display: grid;
  gap: 16px;
  width: min(520px, 100%);
  padding: 20px;
  color: var(--confirm-text);
  border: 1px solid rgba(224, 184, 74, 0.28);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(224, 184, 74, 0.07), rgba(255, 255, 255, 0.012)),
    var(--confirm-panel);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.48);
}

.stage-confirm-dialog header {
  display: grid;
  gap: 7px;
}

.confirm-kicker {
  color: var(--confirm-gold);
  font-size: 12px;
  font-weight: 800;
}

.stage-confirm-dialog h2 {
  font-size: 22px;
  line-height: 1.18;
}

.confirm-copy {
  color: #bfd0d7;
  line-height: 1.65;
}

.confirm-summary {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.58);
}

.confirm-summary span {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.confirm-summary small,
.confirm-reason span {
  color: var(--confirm-muted);
  font-size: 12px;
  font-weight: 800;
}

.confirm-summary strong {
  min-width: 0;
  overflow: hidden;
  color: var(--confirm-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.confirm-reason {
  display: grid;
  gap: 8px;
}

.confirm-reason textarea {
  width: 100%;
  min-height: 86px;
  resize: vertical;
  padding: 10px 12px;
  color: var(--confirm-text);
  border: 1px solid var(--confirm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font: inherit;
  line-height: 1.5;
}

.confirm-file-picker {
  display: flex;
  min-height: 40px;
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 5px;
  border: 1px solid var(--confirm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.confirm-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
}

.confirm-file-button {
  display: inline-flex;
  flex: 0 0 auto;
  min-height: 30px;
  align-items: center;
  padding: 0 11px;
  color: var(--confirm-text);
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.07);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.confirm-file-button:hover,
.confirm-file-button:focus-within {
  color: #ffdc73;
  border-color: rgba(224, 184, 74, 0.42);
  background: rgba(224, 184, 74, 0.1);
}

.confirm-file-name {
  min-width: 0;
  overflow: hidden;
  color: var(--confirm-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.confirm-file-name.selected {
  color: var(--confirm-text);
}

.stage-confirm-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  align-items: center;
}

.confirm-button {
  min-height: 40px;
  padding: 0 14px;
  color: var(--confirm-text);
  border: 1px solid var(--confirm-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font-weight: 800;
}

.confirm-button.primary {
  color: #141107;
  border-color: rgba(224, 184, 74, 0.4);
  background: linear-gradient(135deg, #f2cf67, #d6a52f);
}

.confirm-button.ghost:hover,
.confirm-button.primary:hover {
  transform: translateY(-1px);
}

.filter-panel {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(260px, 1.4fr) repeat(7, minmax(130px, 1fr));
  gap: 12px;
  margin-top: 18px;
  padding: 16px;
  border: 1px solid rgba(218, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(18, 29, 34, 0.86);
}

.field {
  display: grid;
  gap: 7px;
}

.field span {
  color: #8ea4ad;
  font-size: 12px;
  font-weight: 700;
}

.field input,
.field select,
.stack-field textarea,
.form-grid input,
.form-grid select,
.extra-fields input {
  width: 100%;
  min-height: 38px;
  padding: 0 10px;
  color: #eaf3f6;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 8px;
  background: #0d161a;
}

.search-field div {
  position: relative;
}

.search-field svg {
  position: absolute;
  left: 10px;
  top: 10px;
  width: 18px;
  color: #7f949d;
}

.search-field input {
  padding-left: 36px;
}

.table-card,
.entry-drawer {
  border: 1px solid rgba(218, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(18, 29, 34, 0.9);
}

.table-card {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  margin-top: 12px;
  padding: 16px;
  overflow: hidden;
}

.table-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-title h2 {
  margin: 0;
}

.entries-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: hidden;
  scrollbar-gutter: stable;
}

.entries-table.compact {
  flex: 0 0 auto;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: minmax(180px, 1.05fr) minmax(180px, 1fr) minmax(230px, 1.25fr) 132px 76px 114px minmax(208px, 1.05fr);
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.table-head {
  flex: 0 0 auto;
  padding: 0 12px 10px;
  color: #8ea4ad;
  font-size: 13px;
  font-weight: 700;
}

.table-row {
  min-height: 72px;
  padding: 10px 12px;
  border: 1px solid rgba(218, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.table-row.refund-priority {
  border-color: rgba(255, 122, 107, 0.24);
  background: rgba(255, 122, 107, 0.055);
}

.table-row.refunded {
  opacity: 0.52;
  background: rgba(255, 255, 255, 0.02);
}

.table-row.refunded .row-actions {
  opacity: 1;
}

.table-body {
  display: grid;
  flex: 1 1 auto;
  align-content: start;
  gap: 8px;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.entries-table.compact .table-body {
  flex: 0 0 auto;
  overflow-y: visible;
}

.entries-table::-webkit-scrollbar,
.table-body::-webkit-scrollbar,
.entry-drawer::-webkit-scrollbar {
  width: 9px;
  height: 9px;
}

.entries-table::-webkit-scrollbar-track,
.table-body::-webkit-scrollbar-track,
.entry-drawer::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.025);
  border-radius: 999px;
}

.entries-table::-webkit-scrollbar-thumb,
.table-body::-webkit-scrollbar-thumb,
.entry-drawer::-webkit-scrollbar-thumb {
  background: rgba(216, 169, 53, 0.34);
  border-radius: 999px;
}

.entry-cell,
.soft-cell,
.payment-refund-cell {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.entry-cell strong,
.soft-cell strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-cell strong[data-tooltip],
.soft-cell strong[data-tooltip],
.entry-cell small[data-tooltip],
.soft-cell small[data-tooltip] {
  position: relative;
  cursor: default;
}

.entry-cell strong[data-tooltip]:hover::after,
.soft-cell strong[data-tooltip]:hover::after,
.entry-cell small[data-tooltip]:hover::after,
.soft-cell small[data-tooltip]:hover::after {
  position: absolute;
  z-index: 12;
  top: calc(100% + 7px);
  left: 0;
  width: max-content;
  max-width: min(420px, calc(100vw - 72px));
  padding: 7px 9px;
  content: attr(data-tooltip);
  color: #e9f2f5;
  border: 1px solid rgba(218, 232, 237, 0.18);
  border-radius: 6px;
  background: #18252a;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.28);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
  white-space: normal;
  pointer-events: none;
}

.entry-cell small,
.soft-cell small,
.payment-refund-cell small,
.time-cell {
  overflow: hidden;
  color: #8ea4ad;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-pill {
  width: fit-content;
  min-width: 58px;
  padding: 5px 9px;
  text-align: center;
  font-size: 12px;
  font-weight: 800;
  border-radius: 7px;
}

.state-pill.success {
  color: #88e49f;
  background: rgba(83, 201, 121, 0.16);
}

.state-pill.warning {
  color: #ffdc73;
  background: rgba(216, 169, 53, 0.15);
}

.state-pill.muted {
  color: #b4c3c8;
  background: rgba(255, 255, 255, 0.06);
}

.state-pill.danger {
  color: #ffaaa0;
  background: rgba(255, 122, 107, 0.12);
}

.row-actions {
  flex-wrap: wrap;
  justify-content: flex-start;
}

.danger-zone {
  margin-top: 22px;
  padding: 12px;
  border: 1px solid rgba(239, 139, 126, .3);
  border-radius: 8px;
  color: var(--muted);
}

.danger-zone summary { cursor: pointer; color: #ef8b7e; font-weight: 700; }
.danger-zone button { margin-top: 8px; }
.confirm-reason input { width: 100%; min-height: 38px; }
.confirm-check { display: flex; gap: 8px; align-items: flex-start; margin-top: 10px; color: #f1d6d0; font-size: 13px; }
.danger-dialog .confirm-button.primary { background: #b94b40; }

.row-actions button {
  min-height: 30px;
  padding: 0 9px;
  font-size: 12px;
  white-space: nowrap;
}

.pagination-bar {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  min-height: 46px;
  margin-top: 12px;
  padding-top: 12px;
  color: #8ea4ad;
  border-top: 1px solid rgba(218, 232, 237, 0.1);
}

.pager-buttons {
  display: flex;
  gap: 6px;
  align-items: center;
}

.pagination-bar button,
.pagination-bar select {
  min-height: 32px;
  color: #e9f2f5;
  border: 1px solid rgba(218, 232, 237, 0.12);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.035);
}

.pagination-bar button {
  min-width: 34px;
  padding: 0 10px;
}

.pagination-bar button.active {
  color: #ffdc73;
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.1);
}

.pagination-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.pagination-bar label {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pagination-bar select {
  padding: 0 9px;
}

.empty-state,
.drawer-loading {
  padding: 42px;
  text-align: center;
  color: #8ea4ad;
}

.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-end;
  background: rgba(3, 8, 10, 0.58);
}

.entry-drawer {
  width: min(760px, 100vw);
  height: 100vh;
  padding: 22px;
  overflow-y: auto;
  border-radius: 0;
  border-right: 0;
}

.entry-drawer header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.entry-drawer header span,
.entry-drawer header small {
  color: #8ea4ad;
}

.entry-drawer h2 {
  margin: 4px 0;
  font-size: 26px;
}

.drawer-tabs {
  display: flex;
  gap: 8px;
  margin: 18px 0;
  border-bottom: 1px solid rgba(218, 232, 237, 0.1);
}

.drawer-tabs button {
  min-height: 38px;
  padding: 0 12px;
  color: #9eb1b8;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  cursor: pointer;
}

.drawer-tabs button.active {
  color: #ffdc73;
  border-color: #d8a935;
}

.drawer-panel {
  display: grid;
  gap: 14px;
}

.risk-banner {
  display: grid;
  gap: 4px;
  padding: 12px;
  color: #ffdc73;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.risk-banner span {
  color: #d5c99a;
}

.section-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.section-title strong {
  font-size: 15px;
}

.delivery-summary {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(218, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.delivery-summary dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 12px;
  margin: 0;
}

.delivery-summary div {
  min-width: 0;
}

.delivery-summary dt {
  color: #8ea4ad;
  font-size: 12px;
  font-weight: 700;
}

.delivery-summary dd {
  min-width: 0;
  margin: 4px 0 0;
  overflow-wrap: anywhere;
  color: #eaf3f6;
  font-weight: 800;
  line-height: 1.45;
}

.refund-box {
  display: grid;
  gap: 10px;
  padding: 14px;
  color: #eaf3f6;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.refund-box.urgent {
  border-color: rgba(255, 122, 107, 0.38);
  background: rgba(255, 122, 107, 0.1);
}

.refund-box p {
  margin: 0;
  color: #d5c99a;
}

.refund-box dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.refund-box div {
  display: grid;
  gap: 4px;
}

.refund-box dt {
  color: #8ea4ad;
  font-size: 12px;
}

.refund-box dd {
  margin: 0;
  font-weight: 800;
}

.form-grid,
.extra-fields,
.status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.form-grid label,
.extra-fields label,
.stack-field {
  display: grid;
  gap: 7px;
}

.form-grid span,
.extra-fields span,
.stack-field span {
  color: #8ea4ad;
  font-size: 12px;
  font-weight: 700;
}

.stack-field textarea {
  min-height: 84px;
  padding: 10px;
  resize: vertical;
}

.status-grid article,
.label-meta-grid article,
.trace-row,
.log-row {
  padding: 13px;
  border: 1px solid rgba(218, 232, 237, 0.09);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.refund-box-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hint-marker {
  display: inline-grid;
  width: 20px;
  height: 20px;
  padding: 0;
  place-items: center;
  color: var(--muted);
  border: 1px solid rgba(219, 232, 237, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  cursor: help;
  font-size: 12px;
  line-height: 1;
}

.hint-marker:hover,
.hint-marker:focus-visible {
  color: var(--gold-soft);
  border-color: rgba(224, 184, 74, 0.48);
  outline: none;
}

.status-grid small,
.label-meta-grid small,
.trace-row span,
.log-row small,
.log-row p {
  color: #8ea4ad;
}

.status-grid strong,
.label-meta-grid strong,
.trace-row strong,
.log-row strong {
  display: block;
  margin-top: 4px;
}

.label-panel {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.label-preview-card {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(216, 169, 53, 0.18);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.07);
}

.admin-label-preview {
  width: min(260px, 100%);
  padding: 10px;
  border-radius: 10px;
  background: #fff8ec;
}

.admin-label-preview :deep(svg) {
  display: block;
  width: 100%;
  height: auto;
}

.label-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.trace-panel,
.log-panel {
  gap: 8px;
}

.log-row p {
  margin: 8px 0 0;
}

@media (max-width: 1280px) {
  .filter-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

}
</style>
