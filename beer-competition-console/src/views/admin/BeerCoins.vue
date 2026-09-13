<template>
  <main class="beer-coins-page">
    <AdminPageHeader title="啤酒币">
      <template #actions>
        <button class="icon-button" type="button" title="刷新啤酒币数据" aria-label="刷新啤酒币数据" :disabled="loading" @click="refreshOverview">
          <Refresh :class="{ spinning: loading }" />
        </button>
      </template>
    </AdminPageHeader>

    <nav v-if="isSuperAdmin" class="admin-view-tabs" aria-label="啤酒币管理视图">
      <button type="button" :class="{ active: adminView === 'pricing' }" @click="setAdminView('pricing')">定价设置</button>
      <button type="button" :class="{ active: adminView === 'accounts' }" @click="setAdminView('accounts')">账户与账务</button>
    </nav>

    <section v-if="loading && !overview" class="empty-panel">正在读取啤酒币数据</section>

    <template v-else>
      <template v-if="isSuperAdmin">
        <section v-if="adminView === 'pricing'" class="pricing-workspace">
          <article class="panel pricing-settings-panel">
            <header class="panel-header compact-header">
              <h2>价格设置</h2>
              <span class="pricing-save-state">
                <template v-if="pricingDirty">未保存</template>
                <template v-else-if="activePricing?.effectiveTime">更新于 {{ formatDateTime(activePricing.effectiveTime) }}</template>
                <template v-else>尚未保存</template>
              </span>
            </header>

            <div class="editor-tier-title">
              <span>阶梯价格</span>
              <button class="text-action" type="button" @click="addTier">
                <Plus />
                添加阶梯
              </button>
            </div>
            <div class="editor-tier-list">
              <div class="editor-tier-head" aria-hidden="true">
                <span>数量范围</span>
                <span>单枚价格</span>
                <span>操作</span>
              </div>
              <div v-for="(tier, index) in pricingEditor.tiers" :key="tier.key" class="editor-tier-row">
                <div class="tier-range-editor">
                  <template v-if="index < pricingEditor.tiers.length - 1">
                    <strong>{{ formatRangeStart(tier.startQuantity) }} 枚</strong>
                    <span>至</span>
                    <label>
                      <span class="sr-only">第 {{ index + 1 }} 档结束数量</span>
                      <input
                        v-model.number="tier.endQuantity"
                        :name="`tierEnd${index}`"
                        autocomplete="off"
                        type="number"
                        :min="tier.startQuantity"
                        step="1"
                        @blur="resequenceEditorTiers"
                      />
                    </label>
                    <span>枚</span>
                  </template>
                  <strong v-else>{{ formatRangeStart(tier.startQuantity) }} 枚及以上</strong>
                </div>
                <label class="tier-price-input">
                  <span class="sr-only">第 {{ index + 1 }} 档单枚价格</span>
                  <em>¥</em>
                  <input ref="tierPriceInputs" v-model="tier.unitPrice" :name="`tierPrice${index}`" autocomplete="off" inputmode="decimal" placeholder="0.00" @blur="normalizeTierPrice(tier)" />
                  <em>/枚</em>
                </label>
                <button class="icon-button danger-icon" type="button" title="删除这一档" :aria-label="`删除第 ${index + 1} 档`" :disabled="pricingEditor.tiers.length <= 1" @click="removeTier(index)">
                  <Delete />
                </button>
                <span v-if="tierRowError(index)" class="tier-row-error">{{ tierRowError(index) }}</span>
              </div>
            </div>

            <div class="pricing-trial editor-pricing-trial">
              <label class="form-field compact-field">
                <span>试算数量</span>
                <input v-model.number="pricingTrialQuantity" name="pricingTrialQuantity" autocomplete="off" type="number" min="1" max="1000000000" step="1" />
              </label>
              <div class="trial-result">
                <span>试算金额</span>
                <strong>{{ draftPricingPreview.valid ? formatCurrency(draftPricingPreview.amount) : '-' }}</strong>
              </div>
            </div>

            <div class="editor-footer">
              <span v-if="pricingEditorError" class="form-error" aria-live="polite">{{ pricingEditorError }}</span>
              <div class="pricing-actions">
                <button v-if="pricingDirty" class="secondary-button" type="button" :disabled="savingPricing" @click="resetPricingEditor">恢复已保存</button>
                <button class="primary-button" type="button" :disabled="savingPricing || !pricingDirty" @click="savePricing">
                  <Check />
                  {{ savingPricing ? '保存中' : '保存价格设置' }}
                </button>
              </div>
            </div>
          </article>
        </section>

        <section v-else class="accounts-workspace">
          <div class="accounts-toolbar">
            <label class="account-filter">
              <span>第三方账户</span>
              <select v-model="selectedAccountId" name="accountFilter">
                <option value="">全部第三方账户</option>
                <option v-for="account in accountOptions" :key="account.id" :value="String(account.id)">{{ account.name }}</option>
              </select>
            </label>
            <button class="primary-button" type="button" @click="openAdjustmentDialog">
              <Plus />
              人工调整
            </button>
          </div>

          <section class="account-facts">
            <div>
              <span>{{ selectedAccountId ? '可用余额' : '第三方账户' }}</span>
              <strong>{{ selectedAccountId ? formatInteger(selectedAccountStats.available) : formatInteger(accountOptions.length) }}</strong>
              <small>{{ selectedAccountId ? '枚' : '个' }}</small>
            </div>
            <div>
              <span>{{ selectedAccountId ? '30 天内到期' : '已到账订单' }}</span>
              <strong>{{ selectedAccountId ? formatInteger(selectedAccountStats.expiring) : formatInteger(filteredPurchaseOrders.filter((item) => item.status === 'PAID').length) }}</strong>
              <small>{{ selectedAccountId ? '枚' : '笔' }}</small>
            </div>
            <div>
              <span>{{ selectedAccountId ? '下一批到期' : '账务流水' }}</span>
              <strong class="fact-date">{{ selectedAccountId ? (selectedAccountStats.nextExpiry ? formatDate(selectedAccountStats.nextExpiry) : '暂无') : formatInteger(filteredLedger.length) }}</strong>
              <small>{{ selectedAccountId ? '' : '条' }}</small>
            </div>
          </section>

          <nav class="record-tabs" aria-label="账务记录类型">
            <button type="button" :class="{ active: recordView === 'orders' }" @click="setRecordView('orders')">购买订单 <span>{{ filteredPurchaseOrders.length }}</span></button>
            <button type="button" :class="{ active: recordView === 'lots' }" @click="setRecordView('lots')">到账批次 <span>{{ filteredLots.length }}</span></button>
            <button type="button" :class="{ active: recordView === 'ledger' }" @click="setRecordView('ledger')">账务流水 <span>{{ filteredLedger.length }}</span></button>
          </nav>

          <article class="panel record-panel account-record-panel">
            <div v-if="recordView === 'orders'" class="table-scroll">
              <table>
                <thead><tr><th>订单</th><th>企业</th><th>数量</th><th>金额</th><th>状态</th><th>时间</th></tr></thead>
                <tbody>
                  <tr v-for="order in filteredPurchaseOrders" :key="order.id">
                    <td><strong>{{ order.orderNo }}</strong><small>{{ order.productName || '-' }}</small></td>
                    <td><strong>{{ order.enterpriseAccountName || `账户 ${order.enterpriseAccountId}` }}</strong></td>
                    <td>{{ formatInteger(order.quantity) }}</td><td>{{ formatCurrency(order.amount) }}</td>
                    <td><span :class="['state-pill', orderTone(order.status)]">{{ orderStatusLabel(order.status, order.wechatTradeState, order.wechatTradeStateDesc) }}</span></td>
                    <td>{{ formatDateTime(order.createTime) }}</td>
                  </tr>
                  <tr v-if="!filteredPurchaseOrders.length"><td colspan="6" class="table-empty">暂无购买订单</td></tr>
                </tbody>
              </table>
            </div>
            <div v-else-if="recordView === 'lots'" class="table-scroll">
              <table>
                <thead><tr><th>批次</th><th>企业</th><th>来源</th><th>剩余</th><th>有效期</th></tr></thead>
                <tbody>
                  <tr v-for="lot in filteredLots" :key="lot.id">
                    <td><strong>{{ lot.lotNo }}</strong></td><td>{{ lot.enterpriseAccountName || `账户 ${lot.enterpriseAccountId}` }}</td>
                    <td>{{ lot.sourceType === 'PURCHASE' ? '购买到账' : '人工调整' }}</td>
                    <td>{{ formatInteger(lot.remainingQuantity) }} / {{ formatInteger(lot.totalQuantity) }}</td><td>{{ formatDate(lot.expiresAt) }}</td>
                  </tr>
                  <tr v-if="!filteredLots.length"><td colspan="5" class="table-empty">暂无到账批次</td></tr>
                </tbody>
              </table>
            </div>
            <div v-else class="table-scroll">
              <table>
                <thead><tr><th>流水</th><th>企业</th><th>方向</th><th>数量</th><th>业务</th><th>时间</th></tr></thead>
                <tbody>
                  <tr v-for="item in filteredLedger" :key="item.id">
                    <td><strong>{{ item.ledgerNo }}</strong><small>{{ item.reason || '-' }}</small></td><td>{{ item.enterpriseAccountName || `账户 ${item.enterpriseAccountId}` }}</td>
                    <td><span :class="['direction-pill', item.direction === 'CREDIT' ? 'credit' : 'debit']">{{ directionLabel(item.direction) }}</span></td>
                    <td>{{ formatInteger(item.quantity) }}</td><td>{{ businessLabel(item.businessType) }}</td><td>{{ formatDateTime(item.createTime) }}</td>
                  </tr>
                  <tr v-if="!filteredLedger.length"><td colspan="6" class="table-empty">暂无账务流水</td></tr>
                </tbody>
              </table>
            </div>
          </article>
        </section>
      </template>

      <template v-else>
        <section class="wallet-panel panel">
          <div class="wallet-main">
            <span class="eyebrow">可用余额</span>
            <strong>{{ formatInteger(wallet?.availableQuantity) }}</strong>
            <span>枚啤酒币</span>
          </div>
          <div class="wallet-fact">
            <span>30 天内到期</span>
            <strong>{{ formatInteger(wallet?.expiringQuantity) }}</strong>
          </div>
          <div class="wallet-fact">
            <span>下一批到期</span>
            <strong>{{ wallet?.nextExpiryTime ? formatDateTime(wallet.nextExpiryTime) : '暂无' }}</strong>
          </div>
        </section>

        <nav class="organizer-section-tabs" aria-label="啤酒币业务视图">
          <button type="button" :class="{ active: organizerSection === 'purchase' }" @click="setOrganizerSection('purchase')">购买啤酒币</button>
          <button type="button" :class="{ active: organizerSection === 'orders' }" @click="setOrganizerSection('orders')">购买订单 <span>{{ purchaseOrders.length }}</span></button>
          <button type="button" :class="{ active: organizerSection === 'lots' }" @click="setOrganizerSection('lots')">到账批次 <span>{{ lots.length }}</span></button>
          <button type="button" :class="{ active: organizerSection === 'ledger' }" @click="setOrganizerSection('ledger')">账户流水 <span>{{ ledger.length }}</span></button>
        </nav>

        <section v-if="organizerSection === 'purchase'" class="purchase-layout">
          <article class="panel purchase-panel">
            <header class="panel-header">
              <h2>{{ activePricing ? '购买啤酒币' : '暂未开放购买' }}</h2>
            </header>
            <div v-if="activePricing?.tiers?.length" class="purchase-form">
              <label class="quantity-field">
                <span>购买数量</span>
                <div class="quantity-input">
                  <input v-model.number="purchaseQuantity" name="purchaseQuantity" autocomplete="off" type="number" min="1" max="1000000000" step="1" inputmode="numeric" />
                  <em>枚</em>
                </div>
              </label>
              <div v-if="purchasePreview.error" class="form-error">{{ purchasePreview.error }}</div>
              <button class="primary-button purchase-button" type="button" :disabled="!purchasePreview.valid || purchasing" @click="createPurchase">
                <ShoppingCart />
                 {{ purchasing ? '创建订单中' : purchaseButtonLabel }}
              </button>
            </div>
            <div v-else class="inline-empty">平台尚未设置啤酒币价格。</div>
          </article>

          <article class="panel calculation-panel">
            <header class="panel-header compact-header">
              <h2>应付金额</h2>
              <strong class="total-amount">{{ formatCurrency(purchasePreview.amount) }}</strong>
            </header>
            <div v-if="activePricing?.tiers?.length" class="pricing-tier-overview">
              <div class="pricing-tier-overview-head"><span>购买区间</span><span>单价</span></div>
              <div
                v-for="(tier, index) in activePricing.tiers"
                :key="`purchase-tier-${tier.id || index}`"
                :class="['pricing-tier-row', { active: purchaseTierIndex === index }]"
              >
                <span>{{ formatTierRange(tier, index, activePricing.tiers.length) }}</span>
                <strong>{{ formatCurrency(tier.unitPrice) }} / 枚</strong>
              </div>
            </div>
            <div v-if="purchasePreview.segments.length" class="segment-list purchase-segment-list">
              <div class="segment-list-label">本次计价</div>
              <div v-for="segment in purchasePreview.segments" :key="`${segment.startQuantity}-${segment.endQuantity}`" class="segment-row">
                <span>{{ segment.startQuantity }} - {{ segment.endQuantity == null ? purchaseQuantity : segment.endQuantity }} 枚</span>
                <span>{{ formatInteger(segment.quantity) }} × {{ formatCurrency(segment.unitPrice) }}</span>
                <strong>{{ formatCurrency(segment.amount) }}</strong>
              </div>
            </div>
            <div v-else-if="!activePricing?.tiers?.length" class="inline-empty">平台尚未设置啤酒币价格。</div>
          </article>
        </section>
      </template>

      <section v-if="!isSuperAdmin && organizerSection === 'orders'" class="organizer-record-layout">
        <article class="panel record-panel orders-panel">
          <header class="panel-header compact-header">
            <h2>购买订单</h2>
            <span>{{ purchaseOrders.length }} 条</span>
          </header>
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>订单</th>
                  <th>数量</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>时间</th>
                  <th v-if="!isSuperAdmin">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in purchaseOrders" :key="order.id">
                  <td><strong>{{ order.orderNo }}</strong><small>啤酒币购买</small></td>
                  <td>{{ formatInteger(order.quantity) }}</td>
                  <td>{{ formatCurrency(order.amount) }}</td>
                  <td><span :class="['state-pill', orderTone(order.status)]">{{ orderStatusLabel(order.status, order.wechatTradeState, order.wechatTradeStateDesc) }}</span></td>
                  <td>{{ formatDateTime(order.createTime) }}</td>
                  <td v-if="!isSuperAdmin">
                    <button v-if="orderCanPay(order)" class="row-action" type="button" @click="openOrderPayment(order)">支付</button>
                    <button v-else-if="order.status === 'PAID'" class="row-action muted-action" type="button" @click="openOrderPayment(order)">查看</button>
                    <span v-else class="muted-text">-</span>
                  </td>
                </tr>
                <tr v-if="!purchaseOrders.length"><td colspan="6" class="table-empty">暂无购买订单</td></tr>
              </tbody>
            </table>
          </div>
        </article>

      </section>

      <section v-if="!isSuperAdmin && organizerSection === 'lots'" class="organizer-record-layout">
        <article class="panel record-panel lots-panel">
          <header class="panel-header compact-header">
            <h2>到账批次</h2>
            <span>{{ lots.length }} 批</span>
          </header>
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>批次</th>
                  <th>剩余</th>
                  <th>有效期</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="lot in lots" :key="lot.id">
                  <td><strong>{{ lot.lotNo }}</strong><small>{{ lot.sourceType === 'PURCHASE' ? '购买到账' : '人工调整' }}</small></td>
                  <td>{{ formatInteger(lot.remainingQuantity) }} / {{ formatInteger(lot.totalQuantity) }}</td>
                  <td>{{ formatDate(lot.expiresAt) }}</td>
                </tr>
                <tr v-if="!lots.length"><td colspan="3" class="table-empty">暂无到账批次</td></tr>
              </tbody>
            </table>
          </div>
        </article>

      </section>

      <section v-if="!isSuperAdmin && organizerSection === 'ledger'" class="organizer-record-layout">
        <article class="panel record-panel ledger-panel">
          <header class="panel-header compact-header">
            <h2>账户流水</h2>
            <span>{{ ledger.length }} 条</span>
          </header>
          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>流水</th>
                  <th>方向</th>
                  <th>数量</th>
                  <th>业务</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in ledger" :key="item.id">
                  <td><strong>{{ item.ledgerNo }}</strong><small>{{ item.reason || '-' }}</small></td>
                  <td><span :class="['direction-pill', item.direction === 'CREDIT' ? 'credit' : 'debit']">{{ directionLabel(item.direction) }}</span></td>
                  <td>{{ formatInteger(item.quantity) }}</td>
                  <td>{{ businessLabel(item.businessType) }}</td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                </tr>
                <tr v-if="!ledger.length"><td colspan="5" class="table-empty">暂无账户流水</td></tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>
    </template>

    <div v-if="adjustmentDialogOpen" class="modal-mask" @click.self="closeAdjustmentDialog">
      <section class="adjustment-modal" role="dialog" aria-modal="true" aria-labelledby="beer-coin-adjustment-title">
        <header class="modal-header">
          <h2 id="beer-coin-adjustment-title">人工调整</h2>
          <button class="icon-button" type="button" title="关闭" aria-label="关闭人工调整窗口" :disabled="adjusting" @click="closeAdjustmentDialog"><Close /></button>
        </header>

        <form class="adjustment-dialog-form" @submit.prevent="submitAdjustment">
          <label class="form-field">
            <span>第三方账户</span>
            <select v-model="adjustmentForm.enterpriseAccountId" name="adjustmentAccount" :disabled="adjusting">
              <option :value="null" disabled>请选择第三方账户</option>
              <option v-for="account in accountOptions" :key="account.id" :value="Number(account.id)">{{ account.name }}</option>
            </select>
          </label>

          <fieldset class="adjustment-direction">
            <legend>调整类型</legend>
            <div class="direction-control">
              <button
                v-for="item in adjustmentDirections"
                :key="item.value"
                type="button"
                :class="{ active: adjustmentForm.direction === item.value }"
                :disabled="adjusting"
                @click="adjustmentForm.direction = item.value"
              >{{ item.label }}</button>
            </div>
          </fieldset>

          <label class="form-field">
            <span>数量</span>
            <input v-model.number="adjustmentForm.quantity" name="adjustmentQuantity" autocomplete="off" type="number" min="1" step="1" :disabled="adjusting" placeholder="请输入调整数量" />
          </label>

          <label class="form-field">
            <span>原因</span>
            <textarea v-model.trim="adjustmentForm.reason" name="adjustmentReason" maxlength="255" rows="3" :disabled="adjusting" placeholder="请输入调整原因"></textarea>
          </label>

          <div class="adjustment-dialog-actions">
            <button class="secondary-button" type="button" :disabled="adjusting" @click="closeAdjustmentDialog">取消</button>
            <button class="primary-button" type="submit" :disabled="!canSubmitAdjustment || adjusting">
              <Check />
              {{ adjusting ? '处理中' : '确认调整' }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="paymentDialog.open" class="modal-mask" @click.self="closePayment">
      <section class="payment-modal" role="dialog" aria-modal="true" aria-labelledby="beer-coin-payment-title">
        <header class="modal-header">
          <div>
            <span class="eyebrow">啤酒币购买</span>
            <h2 id="beer-coin-payment-title">{{ formatInteger(paymentDialog.order?.quantity) }} 枚</h2>
          </div>
          <button class="icon-button" type="button" title="关闭" aria-label="关闭支付窗口" @click="closePayment"><Close /></button>
        </header>

        <div class="payment-summary">
          <span>应付金额</span>
          <strong>{{ formatCurrency(paymentDialog.order?.amount || paymentDialog.payment?.amount) }}</strong>
          <small>{{ paymentDialog.order?.orderNo || paymentDialog.payment?.orderNo }}</small>
        </div>

        <template v-if="paymentStatus === 'PAID'">
          <div class="payment-result success-result">
            <CircleCheckFilled />
            <strong>支付成功，啤酒币已到账</strong>
            <span>{{ formatInteger(paymentDialog.order?.quantity) }} 枚已加入可用余额</span>
          </div>
        </template>
        <template v-else>
          <div v-if="paymentDialog.error" class="payment-error">{{ paymentDialog.error }}</div>
          <div v-if="paymentDialog.qrDataUrl" class="payment-qr">
            <img :src="paymentDialog.qrDataUrl" width="236" height="236" alt="啤酒币微信支付二维码" />
            <span>请使用微信扫码支付</span>
          </div>
          <div v-else-if="paymentDialog.loading" class="payment-loading"><Loading class="spinning" />正在生成支付信息</div>
          <div v-else class="payment-result pending-result">
            <Clock />
            <strong>{{ paymentStatusLabel }}</strong>
            <span>{{ paymentDialog.payment?.mode === 'MOCK' ? '本地支付可直接确认到账' : '完成微信支付后将自动到账' }}</span>
          </div>
          <div class="payment-actions">
            <button v-if="paymentDialog.payment?.mode === 'MOCK' && paymentStatus !== 'PAID'" class="primary-button" type="button" :disabled="paymentDialog.checking" @click="simulatePayment">
              <Check />
              模拟到账
            </button>
            <button v-if="paymentStatus !== 'EXPIRED' && paymentStatus !== 'CLOSED'" class="secondary-button" type="button" :disabled="paymentDialog.checking" @click="checkPayment">
              <Refresh />
              {{ paymentDialog.checking ? '查询中' : '查询支付结果' }}
            </button>
            <button v-if="paymentStatus === 'EXPIRED' || paymentStatus === 'CLOSED'" class="primary-button" type="button" @click="retryPayment">
              <Refresh />
              重新发起支付
            </button>
          </div>
        </template>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Check,
  CircleCheckFilled,
  Clock,
  Close,
  Delete,
  Loading,
  Plus,
  Refresh,
  ShoppingCart,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import {
  createBeerCoinJsapiPayment,
  createBeerCoinNativePayment,
  createBeerCoinPurchaseOrder,
  adjustBeerCoin,
  fetchBeerCoinOverview,
  fetchBeerCoinPurchaseOrder,
  fetchBeerCoinPurchaseOrderStatus,
  fetchBeerCoinWechatClientConfig,
  saveBeerCoinPricing,
  simulateBeerCoinPayment,
} from '@/api/beerCoin'
import { ADMIN_TYPES } from '@/config/adminAccess'
import { getAdminType } from '@/utils/auth'
import { buildWechatOauthUrl, currentUrlWithoutWechatCode, invokeWechatPay, isWechatBrowser } from '@/utils/wechatPay'

const PAYMENT_ORDER_KEY = 'beer-coin-payment-order-id'

const route = useRoute()
const router = useRouter()
const overview = ref(null)
const loading = ref(false)
const savingPricing = ref(false)
const purchasing = ref(false)
const adjusting = ref(false)
const purchaseQuantity = ref(1)
const pricingTrialQuantity = ref(10)
const pricingEditorError = ref('')
const pricingValidationAttempted = ref(false)
const savedPricingSignature = ref('')
const editorKey = ref(0)
const tierPriceInputs = ref([])
const paymentTimer = ref(null)
const adjustmentDialogOpen = ref(false)
const wechatCode = ref(String(route.query.code || ''))
const pricingEditor = reactive({ tiers: [] })
const adjustmentDirections = [
  { value: 'CREDIT', label: '补发' },
  { value: 'DEBIT', label: '扣减' },
  { value: 'REVERSAL', label: '冲正' },
]
const adjustmentForm = reactive({
  enterpriseAccountId: null,
  direction: 'CREDIT',
  quantity: null,
  reason: '',
})
const paymentDialog = reactive({
  open: false,
  loading: false,
  checking: false,
  error: '',
  order: null,
  payment: null,
  qrDataUrl: '',
})
const wechatPayConfig = ref(null)

const adminType = computed(() => getAdminType())
const isSuperAdmin = computed(() => adminType.value === ADMIN_TYPES.PLATFORM_SUPER_ADMIN)
const activePricing = computed(() => overview.value?.activePricing || null)
const wallet = computed(() => overview.value?.wallet || null)
const purchaseOrders = computed(() => overview.value?.purchaseOrders || [])
const lots = computed(() => overview.value?.lots || [])
const ledger = computed(() => overview.value?.ledger || [])
const accountOptions = computed(() => overview.value?.accountOptions || [])
const pricingDirty = computed(() => pricingEditorSignature() !== savedPricingSignature.value)
const adminView = computed(() => route.query.view === 'accounts' ? 'accounts' : 'pricing')
const organizerSection = computed(() => ['purchase', 'orders', 'lots', 'ledger'].includes(String(route.query.section || ''))
  ? String(route.query.section)
  : 'purchase')
const recordView = computed(() => ['orders', 'lots', 'ledger'].includes(String(route.query.record || ''))
  ? String(route.query.record)
  : 'orders')
const selectedAccountId = computed({
  get: () => String(route.query.accountId || ''),
  set: (value) => replaceQuery({ accountId: value || undefined }),
})
const filteredPurchaseOrders = computed(() => filterBySelectedAccount(purchaseOrders.value))
const filteredLots = computed(() => filterBySelectedAccount(lots.value))
const filteredLedger = computed(() => filterBySelectedAccount(ledger.value))
const selectedAccountStats = computed(() => {
  if (!selectedAccountId.value) return { available: 0, expiring: 0, nextExpiry: null }
  const now = new Date()
  const expiryThreshold = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
  const activeLots = filteredLots.value.filter((lot) => {
    const availableFrom = parseDate(lot.availableFrom)
    const expiresAt = parseDate(lot.expiresAt)
    return Number(lot.remainingQuantity) > 0
      && (!availableFrom || availableFrom <= now)
      && (!expiresAt || expiresAt > now)
  })
  const expiryTimes = activeLots.map((lot) => parseDate(lot.expiresAt)).filter(Boolean)
  return {
    available: activeLots.reduce((sum, lot) => sum + Number(lot.remainingQuantity || 0), 0),
    expiring: activeLots
      .filter((lot) => {
        const expiresAt = parseDate(lot.expiresAt)
        return expiresAt && expiresAt <= expiryThreshold
      })
      .reduce((sum, lot) => sum + Number(lot.remainingQuantity || 0), 0),
    nextExpiry: expiryTimes.length ? new Date(Math.min(...expiryTimes.map((date) => date.getTime()))) : null,
  }
})
const paymentStatus = computed(() => {
  const orderStatus = paymentDialog.order?.status
  if (['PAID', 'EXPIRED', 'CLOSED'].includes(orderStatus)) return orderStatus
  return paymentDialog.payment?.paymentStatus || orderStatus || ''
})
const paymentStatusLabel = computed(() => orderStatusLabel(
  paymentStatus.value,
  paymentDialog.order?.wechatTradeState,
  paymentDialog.order?.wechatTradeStateDesc,
))

const purchasePreview = computed(() => calculatePreview(purchaseQuantity.value, activePricing.value?.tiers || []))
const purchaseTierIndex = computed(() => {
  const quantity = Number(purchaseQuantity.value)
  if (!Number.isSafeInteger(quantity) || quantity <= 0) return -1
  return (activePricing.value?.tiers || []).findIndex((tier) => {
    const start = Number(tier.startQuantity)
    const end = tier.endQuantity == null ? Number.MAX_SAFE_INTEGER : Number(tier.endQuantity)
    return quantity >= start && quantity <= end
  })
})
const draftPricingPreview = computed(() => calculatePreview(pricingTrialQuantity.value, pricingEditorTiersForPreview()))
const canSubmitAdjustment = computed(() => (
  Number.isSafeInteger(Number(adjustmentForm.enterpriseAccountId))
  && Number.isSafeInteger(Number(adjustmentForm.quantity))
  && Number(adjustmentForm.quantity) > 0
  && adjustmentForm.reason.trim().length > 0
))
const purchaseButtonLabel = computed(() => '生成支付订单')

function replaceQuery(changes) {
  const query = { ...route.query }
  Object.entries(changes).forEach(([key, value]) => {
    if (value == null || value === '') delete query[key]
    else query[key] = String(value)
  })
  router.replace({ query })
}

function setAdminView(view) {
  replaceQuery({
    view: view === 'accounts' ? 'accounts' : undefined,
    record: view === 'accounts' ? recordView.value : undefined,
    accountId: view === 'accounts' ? selectedAccountId.value : undefined,
  })
}

function setRecordView(view) {
  if (!['orders', 'lots', 'ledger'].includes(view)) return
  replaceQuery({ view: 'accounts', record: view === 'orders' ? undefined : view })
}

function setOrganizerSection(section) {
  if (!['purchase', 'orders', 'lots', 'ledger'].includes(section)) return
  replaceQuery({ section: section === 'purchase' ? undefined : section })
}

function filterBySelectedAccount(items) {
  if (!selectedAccountId.value) return items
  return items.filter((item) => String(item.enterpriseAccountId) === selectedAccountId.value)
}

function parseDate(value) {
  if (!value) return null
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

function openAdjustmentDialog() {
  if (!accountOptions.value.length) {
    ElMessage.warning('暂无可调整的第三方账户')
    return
  }
  const selectedId = Number(selectedAccountId.value)
  adjustmentForm.enterpriseAccountId = Number.isSafeInteger(selectedId) && selectedId > 0 ? selectedId : null
  adjustmentForm.direction = 'CREDIT'
  adjustmentForm.quantity = null
  adjustmentForm.reason = ''
  adjustmentDialogOpen.value = true
}

function closeAdjustmentDialog() {
  if (adjusting.value) return
  adjustmentDialogOpen.value = false
}

onMounted(async () => {
  await loadOverview()
  try {
    wechatPayConfig.value = await fetchBeerCoinWechatClientConfig()
  } catch {
    // 创建订单时仍由后端返回最终支付模式。
  }
  await resumeWechatPayment()
})

onBeforeUnmount(stopPolling)

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await fetchBeerCoinOverview()
    if (isSuperAdmin.value && !pricingEditor.tiers.length) {
      resetPricingEditor()
    }
  } finally {
    loading.value = false
  }
}

async function refreshOverview() {
  if (isSuperAdmin.value && pricingDirty.value) {
    try {
      await ElMessageBox.confirm(
        '当前价格设置尚未保存，刷新后将恢复为已保存内容。',
        '刷新啤酒币数据',
        { confirmButtonText: '刷新', cancelButtonText: '取消', type: 'warning' },
      )
    } catch {
      return
    }
  }
  await loadOverview()
  if (isSuperAdmin.value) resetPricingEditor()
}

function resetPricingEditor() {
  editorKey.value += 1
  const tiers = activePricing.value?.tiers || []
  pricingEditor.tiers = tiers.length
    ? tiers.map((tier, index) => ({
      key: `${editorKey.value}-${tier.id || tier.startQuantity}`,
      startQuantity: tier.startQuantity,
      endQuantity: index === tiers.length - 1
        ? null
        : (tier.endQuantity ?? Number(tiers[index + 1]?.startQuantity) - 1),
      unitPrice: tier.unitPrice == null ? '' : String(tier.unitPrice),
    }))
    : [{ key: `${editorKey.value}-1`, startQuantity: 1, endQuantity: null, unitPrice: '' }]
  pricingValidationAttempted.value = false
  pricingEditorError.value = ''
  resequenceEditorTiers()
  savedPricingSignature.value = pricingEditorSignature()
}

function pricingEditorSignature() {
  return JSON.stringify(pricingEditor.tiers.map((tier, index) => ({
    startQuantity: Number(tier.startQuantity),
    endQuantity: index === pricingEditor.tiers.length - 1 ? null : Number(tier.endQuantity),
    unitPrice: isValidUnitPrice(tier.unitPrice)
      ? Number(tier.unitPrice).toFixed(2)
      : String(tier.unitPrice ?? '').trim(),
  })))
}

async function addTier() {
  const last = pricingEditor.tiers[pricingEditor.tiers.length - 1]
  const lastStart = Number(last?.startQuantity)
  if (!last || !Number.isSafeInteger(lastStart) || lastStart <= 0) {
    pricingEditorError.value = '请先修正当前数量区间。'
    return
  }
  const newStart = lastStart + 1
  if (newStart > 1000000000) {
    pricingEditorError.value = '数量区间已达上限。'
    return
  }
  // 新档从上一档的下一个数量开始，上一档同步收口，区间保持连续。
  last.endQuantity = lastStart
  editorKey.value += 1
  pricingEditor.tiers.push({
    key: `${editorKey.value}-${pricingEditor.tiers.length + 1}`,
    startQuantity: newStart,
    endQuantity: null,
    unitPrice: '',
  })
  resequenceEditorTiers()
  pricingEditorError.value = ''
  await nextTick()
  const inputs = tierPriceInputs.value
  inputs[inputs.length - 1]?.focus()
}

async function removeTier(index) {
  if (pricingEditor.tiers.length <= 1) return
  const tier = pricingEditor.tiers[index]
  try {
    await ElMessageBox.confirm(
      `确认删除“${describeTierRange(tier, index)}”这一档？删除后数量范围会自动衔接。`,
      '删除阶梯',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  pricingEditor.tiers.splice(index, 1)
  resequenceEditorTiers()
  pricingEditorError.value = ''
}

function resequenceEditorTiers() {
  let nextStart = 1
  pricingEditor.tiers.forEach((tier, index) => {
    tier.startQuantity = nextStart
    const isLast = index === pricingEditor.tiers.length - 1
    if (isLast) {
      tier.endQuantity = null
      return
    }
    const end = Number(tier.endQuantity)
    nextStart = Number.isSafeInteger(end) && Number.isSafeInteger(nextStart) && end >= nextStart && end < 1000000000
      ? end + 1
      : null
  })
  pricingEditorError.value = ''
}

function normalizeTierPrice(tier) {
  if (isValidUnitPrice(tier.unitPrice)) {
    tier.unitPrice = Number(tier.unitPrice).toFixed(2)
  }
  pricingEditorError.value = ''
}

function isValidUnitPrice(value) {
  const text = String(value ?? '').trim()
  const amount = Number(text)
  return Number.isFinite(amount) && amount > 0 && /^\d+(\.\d{1,2})?$/.test(text)
}

function describeTierRange(tier, index) {
  const start = formatRangeStart(tier?.startQuantity)
  return index === pricingEditor.tiers.length - 1
    ? `${start} 枚及以上`
    : `${start} 至 ${formatInteger(tier?.endQuantity)} 枚`
}

function formatRangeStart(value) {
  const start = Number(value)
  return Number.isSafeInteger(start) && start > 0 ? formatInteger(start) : '待确定'
}

function tierRowError(index, force = pricingValidationAttempted.value) {
  const tier = pricingEditor.tiers[index]
  if (!tier) return ''
  const start = Number(tier.startQuantity)
  if (!Number.isSafeInteger(start) || start <= 0) return '请先修正上一档的结束数量。'
  if (index < pricingEditor.tiers.length - 1) {
    const rawEnd = String(tier.endQuantity ?? '').trim()
    const end = Number(rawEnd)
    if ((force || rawEnd) && (!Number.isSafeInteger(end) || end < start || end >= 1000000000)) {
      return `结束数量需要是 ${formatInteger(start)} 至 999,999,999 之间的整数。`
    }
  }
  const rawPrice = String(tier.unitPrice ?? '').trim()
  if ((force || rawPrice) && !isValidUnitPrice(rawPrice)) {
    return '单枚价格必须大于 0，最多保留两位小数。'
  }
  return ''
}

async function savePricing() {
  const payload = validatePricingEditor()
  if (!payload) return
  try {
    await ElMessageBox.confirm(
      `本次设置包含 ${payload.tiers.length} 档价格，保存后用于新创建的啤酒币订单，已有订单金额保持不变。`,
      '确认保存价格',
      { confirmButtonText: '保存', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  savingPricing.value = true
  try {
    await saveBeerCoinPricing(payload)
    ElMessage.success('价格设置已保存')
    await loadOverview()
    resetPricingEditor()
  } catch (error) {
    if (!error?.userNotified) ElMessage.warning(error?.message || '价格设置保存失败')
  } finally {
    savingPricing.value = false
  }
}

function validatePricingEditor() {
  resequenceEditorTiers()
  pricingValidationAttempted.value = true
  pricingEditorError.value = ''
  if (!pricingEditor.tiers.length) {
    pricingEditorError.value = '至少配置一档价格。'
    return null
  }
  let expectedStart = 1
  const tiers = []
  for (let index = 0; index < pricingEditor.tiers.length; index += 1) {
    const item = pricingEditor.tiers[index]
    const rowError = tierRowError(index, true)
    if (rowError) {
      pricingEditorError.value = `第 ${index + 1} 档：${rowError}`
      return null
    }
    const start = Number(item.startQuantity)
    const unitPrice = Number(item.unitPrice)
    if (!Number.isSafeInteger(start) || start !== expectedStart || start <= 0) {
      pricingEditorError.value = '数量区间必须从 1 开始并保持连续。'
      return null
    }
    const isLast = index === pricingEditor.tiers.length - 1
    const end = isLast ? null : Number(item.endQuantity)
    tiers.push({ startQuantity: start, endQuantity: end, unitPrice: unitPrice.toFixed(2) })
    expectedStart = end == null ? Number.MAX_SAFE_INTEGER : end + 1
  }
  return { tiers }
}

function pricingEditorTiersForPreview() {
  let expectedStart = 1
  const tiers = []
  for (let index = 0; index < pricingEditor.tiers.length; index += 1) {
    const item = pricingEditor.tiers[index]
    const start = Number(item.startQuantity)
    const unitPrice = Number(item.unitPrice)
    const isLast = index === pricingEditor.tiers.length - 1
    const end = isLast ? null : Number(item.endQuantity)
    if (!Number.isSafeInteger(start) || start !== expectedStart || start <= 0) return []
    if (!isValidUnitPrice(item.unitPrice)) return []
    if (end != null && (!Number.isSafeInteger(end) || end < start || end >= 1000000000)) return []
    tiers.push({ startQuantity: start, endQuantity: end, unitPrice })
    expectedStart = end == null ? Number.MAX_SAFE_INTEGER : end + 1
  }
  return tiers
}

async function submitAdjustment() {
  if (!canSubmitAdjustment.value || adjusting.value) return
  const direction = adjustmentDirections.find((item) => item.value === adjustmentForm.direction)?.label || '调整'
  try {
    await ElMessageBox.confirm(
      `确认对该第三方账户${direction} ${formatInteger(adjustmentForm.quantity)} 枚啤酒币？`,
      '确认人工调整',
      { confirmButtonText: '确认调整', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  adjusting.value = true
  try {
    await adjustBeerCoin({
      enterpriseAccountId: Number(adjustmentForm.enterpriseAccountId),
      direction: adjustmentForm.direction,
      quantity: Number(adjustmentForm.quantity),
      reason: adjustmentForm.reason.trim(),
    })
    adjustmentForm.quantity = null
    adjustmentForm.reason = ''
    await loadOverview()
    adjustmentDialogOpen.value = false
    ElMessage.success('啤酒币调整已完成')
  } catch (error) {
    if (!error?.userNotified) ElMessage.warning(error?.message || '啤酒币调整失败')
  } finally {
    adjusting.value = false
  }
}

async function createPurchase() {
  if (!purchasePreview.value.valid) return
  purchasing.value = true
  try {
    const order = await createBeerCoinPurchaseOrder({ quantity: Number(purchaseQuantity.value) })
    await loadOverview()
    await openOrderPayment(order)
  } catch (error) {
    if (!error?.userNotified) ElMessage.warning(error?.message || '购买订单创建失败')
  } finally {
    purchasing.value = false
  }
}

async function openOrderPayment(order) {
  paymentDialog.open = true
  paymentDialog.loading = false
  paymentDialog.checking = false
  paymentDialog.error = ''
  paymentDialog.order = order
  paymentDialog.payment = null
  paymentDialog.qrDataUrl = ''
  if (order?.status === 'PAID') return
  await startPayment(order)
}

async function startPayment(order, options = {}) {
  if (!order?.id || paymentDialog.loading) return
  paymentDialog.loading = true
  paymentDialog.error = ''
  try {
    const config = await fetchBeerCoinWechatClientConfig()
    wechatPayConfig.value = config
    if (config?.mode === 'WECHAT' && isWechatBrowser()) {
      if (!config.jsapiConfigured || !config.appId) {
        throw new Error('微信内支付暂不可用，请稍后再试')
      }
      if (!wechatCode.value) {
        sessionStorage.setItem(PAYMENT_ORDER_KEY, String(order.id))
        window.location.href = buildWechatOauthUrl(config.appId)
        return
      }
      paymentDialog.payment = await createBeerCoinJsapiPayment(order.id, wechatCode.value)
      clearWechatAuthCode()
      if (paymentDialog.payment?.paymentStatus === 'PAID') {
        paymentDialog.order = await fetchBeerCoinPurchaseOrderStatus(order.id)
        await loadOverview()
        return
      }
      if (!paymentDialog.payment?.payParams) throw new Error('微信支付参数缺失')
      await invokeWechatPay(paymentDialog.payment.payParams)
      startPolling()
      await checkPayment({ silent: true })
      return
    }

    paymentDialog.payment = await createBeerCoinNativePayment(order.id)
    if (paymentDialog.payment?.codeUrl) {
      paymentDialog.qrDataUrl = await QRCode.toDataURL(paymentDialog.payment.codeUrl, {
        errorCorrectionLevel: 'M',
        margin: 1,
        width: 236,
      })
    }
    if (paymentDialog.payment?.paymentStatus === 'PAID') {
      paymentDialog.order = await fetchBeerCoinPurchaseOrderStatus(order.id)
      await loadOverview()
      return
    }
    if (paymentDialog.payment?.mode !== 'MOCK' && !options.skipPolling) startPolling()
  } catch (error) {
    paymentDialog.error = error?.message || '支付信息生成失败，请稍后重试'
  } finally {
    paymentDialog.loading = false
  }
}

async function resumeWechatPayment() {
  const pendingId = Number(sessionStorage.getItem(PAYMENT_ORDER_KEY) || 0)
  if (!pendingId || !wechatCode.value) {
    if (!wechatCode.value) sessionStorage.removeItem(PAYMENT_ORDER_KEY)
    return
  }
  try {
    const order = await fetchBeerCoinPurchaseOrder(pendingId)
    await openOrderPayment(order)
  } catch {
    sessionStorage.removeItem(PAYMENT_ORDER_KEY)
  }
}

function clearWechatAuthCode() {
  sessionStorage.removeItem(PAYMENT_ORDER_KEY)
  if (!wechatCode.value) return
  wechatCode.value = ''
  window.history.replaceState(null, '', currentUrlWithoutWechatCode())
}

function startPolling() {
  stopPolling()
  paymentTimer.value = window.setInterval(() => checkPayment({ silent: true }), 3000)
}

function stopPolling() {
  if (paymentTimer.value) {
    window.clearInterval(paymentTimer.value)
    paymentTimer.value = null
  }
}

async function checkPayment(options = {}) {
  if (!paymentDialog.open || !paymentDialog.order?.id || paymentDialog.checking || paymentStatus.value === 'PAID') return
  paymentDialog.checking = true
  try {
    paymentDialog.order = await fetchBeerCoinPurchaseOrderStatus(paymentDialog.order.id)
    if (paymentStatus.value === 'PAID') {
      stopPolling()
      await loadOverview()
      if (!options.silent) ElMessage.success('支付成功，啤酒币已到账')
    } else if (paymentStatus.value === 'EXPIRED' || paymentStatus.value === 'CLOSED') {
      stopPolling()
    }
  } catch (error) {
    if (!options.silent && !error?.userNotified) ElMessage.warning(error?.message || '支付结果查询失败')
  } finally {
    paymentDialog.checking = false
  }
}

async function simulatePayment() {
  if (!paymentDialog.order?.id || paymentDialog.checking) return
  paymentDialog.checking = true
  try {
    paymentDialog.order = await simulateBeerCoinPayment(paymentDialog.order.id)
    stopPolling()
    await loadOverview()
    ElMessage.success('模拟支付成功，啤酒币已到账')
  } catch (error) {
    if (!error?.userNotified) ElMessage.warning(error?.message || '模拟支付失败')
  } finally {
    paymentDialog.checking = false
  }
}

async function retryPayment() {
  const previousOrder = paymentDialog.order
  if (!previousOrder || !['EXPIRED', 'CLOSED'].includes(paymentStatus.value)) return
  paymentDialog.loading = true
  paymentDialog.error = ''
  let newOrder = null
  try {
    newOrder = await createBeerCoinPurchaseOrder({ quantity: Number(previousOrder.quantity) })
    paymentDialog.order = newOrder
    paymentDialog.payment = null
    paymentDialog.qrDataUrl = ''
    await loadOverview()
  } catch (error) {
    paymentDialog.error = error?.message || '重新创建支付订单失败，请稍后重试'
  } finally {
    paymentDialog.loading = false
  }
  if (newOrder) await startPayment(newOrder)
}

async function closePayment() {
  stopPolling()
  paymentDialog.open = false
}

function orderCanPay(order) {
  if (!order || !['CREATED', 'WAITING_PAYMENT'].includes(order.status)) return false
  return !order.expireTime || new Date(order.expireTime.replace(' ', 'T')) > new Date()
}

function calculatePreview(quantity, tiers) {
  const value = Number(quantity)
  if (!Number.isSafeInteger(value) || value <= 0) {
    return { valid: false, amount: 0, segments: [], error: '购买数量必须是大于 0 的整数。' }
  }
  if (value > 1000000000) {
    return { valid: false, amount: 0, segments: [], error: '购买数量不能超过 1,000,000,000 枚。' }
  }
  if (!tiers.length) return { valid: false, amount: 0, segments: [], error: '当前尚未设置啤酒币价格。' }
  let cursor = 1
  let amount = 0
  const segments = []
  for (const tier of tiers) {
    const start = Math.max(cursor, Number(tier.startQuantity))
    if (start > value) break
    const end = tier.endQuantity == null ? value : Math.min(value, Number(tier.endQuantity))
    if (end < start) continue
    const count = end - start + 1
    const segmentAmount = count * Number(tier.unitPrice)
    amount += segmentAmount
    segments.push({ startQuantity: start, endQuantity: end, quantity: count, unitPrice: Number(tier.unitPrice), amount: segmentAmount })
    cursor = end + 1
  }
  if (cursor <= value) return { valid: false, amount: 0, segments: [], error: '购买数量超出当前计价范围。' }
  return { valid: true, amount, segments, error: '' }
}

function formatCurrency(value) {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '¥0.00'
  return `¥${amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatInteger(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '0'
}

function formatTierRange(tier, index, tierCount) {
  const start = formatInteger(tier?.startQuantity)
  if (index === tierCount - 1 || tier?.endQuantity == null) return `${start} 枚及以上`
  return `${start} - ${formatInteger(tier.endQuantity)} 枚`
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatDate(value) {
  if (!value) return '-'
  if (value instanceof Date) {
    return value.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  }
  return String(value).replace('T', ' ').slice(0, 10)
}

function orderStatusLabel(status, tradeState, tradeStateDesc) {
  if (status === 'CLOSED' && ['PAYERROR', 'REVOKED'].includes(tradeState)) {
    return tradeState === 'REVOKED' ? '支付已撤销' : (tradeStateDesc || '支付失败')
  }
  return ({ CREATED: '待发起支付', WAITING_PAYMENT: '待支付', PAID: '已到账', EXPIRED: '已过期', CLOSED: '已关闭' })[status] || status || '-'
}

function orderTone(status) {
  return status === 'PAID' ? 'success' : ['EXPIRED', 'CLOSED'].includes(status) ? 'muted' : 'warning'
}

function directionLabel(direction) {
  return ({ CREDIT: '入账', DEBIT: '消费', REVERSAL: '冲正' })[direction] || direction || '-'
}

function businessLabel(type) {
  return ({ PURCHASE: '购买到账', COMPETITION: '赛事消耗', MANUAL_ADJUSTMENT: '人工调整' })[type] || type || '-'
}
</script>

<style scoped>
.beer-coins-page {
  --panel: rgba(20, 31, 35, 0.94);
  --panel-soft: rgba(255, 255, 255, 0.035);
  --line: rgba(218, 231, 236, 0.11);
  --text: #e7eff1;
  --muted: #8fa4ad;
  --gold: #d8a935;
  --green: #71d595;
  min-height: 100vh;
  padding: 0 28px 24px;
  overflow-y: auto;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.024) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.018) 1px, transparent 1px),
    #0d1519;
  background-size: 48px 48px;
}

.purchase-layout,
.records-layout {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.purchase-layout { grid-template-columns: minmax(360px, 0.9fr) minmax(420px, 1.1fr); }
.records-layout { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.organizer-record-layout { display: grid; margin-top: 18px; }
.ledger-panel { grid-column: 1 / -1; }

.admin-view-tabs {
  display: flex;
  gap: 24px;
  margin-top: 4px;
  border-bottom: 1px solid rgba(218, 231, 236, 0.11);
}

.admin-view-tabs button,
.record-tabs button {
  position: relative;
  border: 0;
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.admin-view-tabs button {
  min-height: 46px;
  padding: 0 2px;
  color: #8fa4ad;
  font-size: 14px;
  font-weight: 750;
}

.admin-view-tabs button::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  content: '';
  background: transparent;
}

.admin-view-tabs button:hover,
.admin-view-tabs button.active { color: #f0f5f6; }
.admin-view-tabs button.active::after { background: var(--gold); }
.admin-view-tabs button:focus-visible,
.record-tabs button:focus-visible { outline: 2px solid rgba(255, 220, 115, 0.8); outline-offset: 2px; }

.organizer-section-tabs {
  display: flex;
  gap: 24px;
  margin-top: 18px;
  border-bottom: 1px solid rgba(218, 231, 236, 0.11);
}
.organizer-section-tabs button {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 44px;
  padding: 0 2px;
  color: #8fa4ad;
  border: 0;
  background: transparent;
  font: inherit;
  font-size: 13px;
  font-weight: 750;
  cursor: pointer;
}
.organizer-section-tabs button::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  content: '';
  background: transparent;
}
.organizer-section-tabs button:hover,
.organizer-section-tabs button.active { color: #f0f5f6; }
.organizer-section-tabs button.active::after { background: var(--gold); }
.organizer-section-tabs button:focus-visible { outline: 2px solid rgba(255, 220, 115, 0.8); outline-offset: 2px; }
.organizer-section-tabs button span { min-width: 18px; padding: 2px 5px; color: #82969e; border-radius: 4px; background: rgba(255, 255, 255, 0.06); font-size: 10px; text-align: center; }
.organizer-section-tabs button.active span { color: #171a0e; background: rgba(0, 0, 0, 0.14); }

.pricing-workspace,
.accounts-workspace {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.pricing-workspace { width: min(960px, 100%); }
.pricing-settings-panel { padding-bottom: 18px; }
.pricing-save-state { color: #82969e; font-size: 11px; font-variant-numeric: tabular-nums; }

.accounts-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(20, 31, 35, 0.72);
}

.account-filter { display: grid; gap: 7px; width: min(420px, 100%); color: #9bb0b8; font-size: 11px; font-weight: 700; }
.account-filter select {
  width: 100%;
  min-height: 38px;
  padding: 0 11px;
  color: #edf5f6;
  border: 1px solid rgba(218, 231, 236, 0.14);
  border-radius: 7px;
  outline: none;
  background: #0d171b;
  color-scheme: dark;
  font: inherit;
  font-size: 13px;
}
.account-filter select:focus-visible { border-color: rgba(216, 169, 53, 0.55); box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08); }

.account-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(218, 231, 236, 0.1);
}
.account-facts > div { min-width: 0; padding: 18px 20px; background: var(--panel); }
.account-facts > div + div { border-left: 1px solid rgba(218, 231, 236, 0.09); }
.account-facts span { display: block; margin-bottom: 8px; color: #82979f; font-size: 11px; }
.account-facts strong { color: #eef5f6; font-size: 24px; font-variant-numeric: tabular-nums; }
.account-facts small { margin-left: 5px; color: #8da2aa; font-size: 11px; }
.account-facts .fact-date { font-size: 18px; }

.record-tabs {
  display: flex;
  gap: 6px;
  padding: 4px;
  border: 1px solid rgba(218, 231, 236, 0.1);
  border-radius: 8px;
  background: rgba(20, 31, 35, 0.7);
}
.record-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  color: #8ea2aa;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 750;
}
.record-tabs button:hover { color: #dce7e9; background: rgba(255, 255, 255, 0.035); }
.record-tabs button.active { color: #171a0e; background: var(--gold); }
.record-tabs button span { min-width: 20px; padding: 2px 5px; border-radius: 4px; background: rgba(0, 0, 0, 0.16); font-size: 10px; text-align: center; }
.account-record-panel { min-height: 300px; box-shadow: none; }

.panel {
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.14);
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 20px 14px;
}

.compact-header { align-items: center; padding-bottom: 12px; }
.panel-header h2 { margin: 5px 0 0; color: #f1f6f7; font-size: 19px; letter-spacing: 0; }
.eyebrow { color: #879ca4; font-size: 11px; font-weight: 800; letter-spacing: 0.04em; }
.purchase-panel .panel-header h2 { margin-top: 0; }

.inline-empty, .empty-panel { padding: 38px 20px; color: var(--muted); text-align: center; font-size: 13px; }

.form-field, .quantity-field { display: grid; gap: 7px; min-width: 0; color: #9bb0b8; font-size: 11px; font-weight: 700; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.form-field input, .form-field select, .form-field textarea, .quantity-field input {
  box-sizing: border-box;
  width: 100%;
  min-height: 38px;
  padding: 0 11px;
  color: #edf5f6;
  border: 1px solid rgba(218, 231, 236, 0.14);
  border-radius: 7px;
  outline: none;
  background: #0d171b;
  font: inherit;
  font-size: 13px;
}
.form-field textarea { min-height: 84px; padding-top: 10px; resize: vertical; line-height: 1.5; }
.form-field select { appearance: none; color-scheme: dark; }
.form-field input:focus-visible, .form-field select:focus-visible, .form-field textarea:focus-visible, .quantity-field input:focus-visible { border-color: rgba(216, 169, 53, 0.55); box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08); }
.form-field input:disabled { color: #647981; cursor: not-allowed; background: rgba(255, 255, 255, 0.025); }
.form-field input::placeholder, .form-field textarea::placeholder, .quantity-field input::placeholder { color: #52676f; }
.editor-tier-title { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin: 0 20px 9px; color: #dbe7ea; font-size: 13px; font-weight: 800; }
.text-action, .row-action { display: inline-flex; align-items: center; gap: 5px; min-height: 30px; padding: 0 8px; color: #ffdc73; border: 1px solid rgba(216, 169, 53, 0.22); border-radius: 6px; background: rgba(216, 169, 53, 0.07); font: inherit; font-size: 11px; font-weight: 800; cursor: pointer; }
.text-action svg { width: 14px; height: 14px; }
.tier-range-editor input {
  box-sizing: border-box;
  width: 100%;
  min-height: 34px;
  padding: 0 9px;
  color: #edf5f6;
  border: 1px solid rgba(218, 231, 236, 0.14);
  border-radius: 6px;
  outline: none;
  background: #0d171b;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
}
.tier-range-editor input:focus-visible,
.tier-price-input:focus-within { border-color: rgba(216, 169, 53, 0.55); box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08); }
.tier-row-error { color: #ffaaa0; font-size: 11px; font-weight: 600; line-height: 1.4; }
.editor-tier-list { display: grid; margin: 0 20px; overflow: hidden; border: 1px solid rgba(218, 231, 236, 0.09); border-radius: 7px; }
.editor-tier-head,
.editor-tier-row {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(200px, 260px) 44px;
  gap: 12px;
  align-items: center;
}
.editor-tier-head {
  min-height: 34px;
  padding: 0 12px;
  color: #72868e;
  background: rgba(255, 255, 255, 0.025);
  font-size: 10px;
  font-weight: 800;
}
.editor-tier-row {
  min-height: 54px;
  padding: 9px 12px;
  border-top: 1px solid rgba(218, 231, 236, 0.08);
  background: rgba(255, 255, 255, 0.025);
}
.tier-range-editor {
  display: grid;
  grid-template-columns: minmax(88px, auto) auto minmax(120px, 180px) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #83979f;
  font-size: 11px;
}
.tier-range-editor strong { color: #dfeaec; font-size: 12px; font-weight: 750; white-space: nowrap; }
.tier-range-editor label { min-width: 0; }
.tier-price-input {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  min-height: 34px;
  overflow: hidden;
  color: #8499a1;
  border: 1px solid rgba(218, 231, 236, 0.14);
  border-radius: 6px;
  background: #0d171b;
  font-size: 11px;
  font-weight: 700;
}
.tier-price-input em { padding: 0 9px; font-style: normal; white-space: nowrap; }
.tier-price-input input {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  min-height: 32px;
  padding: 0 4px;
  color: #edf5f6;
  border: 0;
  outline: 0;
  background: transparent;
  font: inherit;
  font-size: 12px;
}
.tier-row-error { grid-column: 1 / -1; margin-top: -3px; }
.tier-range-editor input[type='number'] { appearance: textfield; }
.tier-range-editor input[type='number']::-webkit-inner-spin-button,
.tier-range-editor input[type='number']::-webkit-outer-spin-button { margin: 0; appearance: none; }
.compact-field input { min-height: 34px; padding: 0 8px; font-size: 12px; }
.icon-button { display: inline-grid; place-items: center; width: 38px; height: 38px; padding: 0; color: #b7c7cc; border: 1px solid rgba(218, 231, 236, 0.12); border-radius: 7px; background: rgba(255, 255, 255, 0.035); cursor: pointer; }
.icon-button:hover:not(:disabled) { color: #fff; border-color: rgba(216, 169, 53, 0.34); background: rgba(216, 169, 53, 0.08); }
.icon-button:disabled { opacity: 0.45; cursor: not-allowed; }
.icon-button svg { width: 17px; height: 17px; }
.danger-icon { color: #ff9b92; }
.editor-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 16px 20px 0; }
.pricing-actions { display: flex; justify-content: flex-end; gap: 8px; margin-left: auto; }
.pricing-trial { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin: 16px 20px 0; padding: 12px; border: 1px solid rgba(218, 231, 236, 0.08); border-radius: 7px; background: rgba(255, 255, 255, 0.025); }
.pricing-trial > .compact-field { display: flex; align-items: center; gap: 10px; }
.pricing-trial > .compact-field > span { white-space: nowrap; }
.pricing-trial > .compact-field > input { width: 160px; }
.pricing-trial > div { display: flex; align-items: center; justify-content: flex-end; gap: 12px; min-height: 34px; }
.pricing-trial > div span { color: #81969e; font-size: 10px; font-weight: 700; }
.pricing-trial > div strong { color: #ffdc73; font-size: 16px; font-variant-numeric: tabular-nums; }
.form-error { color: #ffaaa0; font-size: 12px; line-height: 1.45; }
.primary-button, .secondary-button { display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 38px; padding: 0 14px; border-radius: 7px; font: inherit; font-size: 12px; font-weight: 800; cursor: pointer; }
.primary-button { color: #171a0e; border: 1px solid #e2b942; background: #d8a935; }
.primary-button:hover:not(:disabled) { background: #e5bd51; }
.primary-button:disabled, .secondary-button:disabled { opacity: 0.5; cursor: not-allowed; }
.secondary-button { color: #dbe7ea; border: 1px solid rgba(218, 231, 236, 0.16); background: rgba(255, 255, 255, 0.05); }
.primary-button svg, .secondary-button svg { width: 15px; height: 15px; }

.wallet-panel { display: grid; grid-template-columns: minmax(240px, 1.25fr) repeat(2, minmax(180px, 0.75fr)); gap: 1px; margin-top: 18px; overflow: hidden; background: rgba(218, 231, 236, 0.1); }
.direction-control { display: inline-grid; grid-auto-flow: column; min-height: 38px; overflow: hidden; border: 1px solid rgba(218, 231, 236, 0.14); border-radius: 7px; }
.direction-control button { min-width: 58px; padding: 0 10px; color: #91a5ad; border: 0; border-left: 1px solid rgba(218, 231, 236, 0.1); background: #0d171b; font: inherit; font-size: 11px; font-weight: 750; cursor: pointer; }
.direction-control button:first-child { border-left: 0; }
.direction-control button.active { color: #171a0e; background: #d8a935; }
.direction-control button:disabled { cursor: not-allowed; opacity: 0.55; }
.direction-control button:focus-visible { position: relative; z-index: 1; outline: 2px solid #ffdc73; outline-offset: -2px; }
.wallet-main, .wallet-fact { min-width: 0; padding: 22px; background: var(--panel); }
.wallet-main { display: grid; align-content: center; gap: 3px; }
.wallet-main strong { color: #ffdc73; font-size: 40px; line-height: 1; letter-spacing: 0; }
.wallet-main > span:last-child { color: #9eb0b7; font-size: 12px; }
.wallet-fact { display: grid; align-content: center; gap: 8px; }
.wallet-fact span { color: #879ca4; font-size: 12px; }
.wallet-fact strong { color: #e6eff1; font-size: 18px; letter-spacing: 0; }
.purchase-panel, .calculation-panel { min-height: 238px; }
.purchase-form { display: flex; align-items: flex-end; gap: 14px; padding: 12px 20px 20px; }
.quantity-field { flex: 1; }
.quantity-input { position: relative; }
.quantity-input input { padding-right: 36px; font-size: 20px; font-weight: 800; }
.quantity-input em { position: absolute; top: 50%; right: 12px; color: #7e949c; font-size: 12px; font-style: normal; transform: translateY(-50%); }
.purchase-button { flex: 0 0 auto; min-width: 150px; }
.purchase-form > .form-error { align-self: center; }
.calculation-panel { background: rgba(18, 29, 33, 0.86); }
.total-amount { color: #ffdc73; font-size: 22px; letter-spacing: 0; }
.pricing-tier-overview { display: grid; gap: 0; margin: 0 20px 16px; overflow: hidden; border: 1px solid rgba(218, 231, 236, 0.09); border-radius: 7px; }
.pricing-tier-overview-head, .pricing-tier-row { display: grid; grid-template-columns: 1fr auto; gap: 16px; align-items: center; }
.pricing-tier-overview-head { min-height: 30px; padding: 0 11px; color: #718790; background: rgba(255, 255, 255, 0.025); font-size: 10px; font-weight: 800; }
.pricing-tier-row { min-height: 36px; padding: 0 11px; color: #b4c4c9; border-top: 1px solid rgba(218, 231, 236, 0.07); font-size: 11px; }
.pricing-tier-row strong { color: #dfeaec; font-size: 11px; font-variant-numeric: tabular-nums; }
.pricing-tier-row.active { color: #f4e0a0; background: rgba(216, 169, 53, 0.1); }
.pricing-tier-row.active strong { color: #ffdc73; }
.segment-list-label { margin-bottom: 7px; color: #81969e; font-size: 10px; font-weight: 800; }
.purchase-segment-list { margin-top: 0; }
.segment-list { display: grid; gap: 8px; margin: 0 20px 20px; }
.segment-row { display: grid; grid-template-columns: 1fr 1fr auto; gap: 12px; align-items: center; padding: 10px 11px; color: #a9bbc1; border: 1px solid rgba(218, 231, 236, 0.08); border-radius: 6px; background: rgba(255, 255, 255, 0.025); font-size: 12px; }
.segment-row strong { color: #dfeaec; font-size: 13px; }

.record-panel { overflow: hidden; }
.record-panel > .panel-header { border-bottom: 1px solid rgba(218, 231, 236, 0.08); }
.record-panel > .panel-header > span { color: #82969e; font-size: 12px; }
.table-scroll { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: 12px; }
th, td { padding: 11px 14px; text-align: left; white-space: nowrap; }
th { color: #718790; background: rgba(255, 255, 255, 0.025); font-size: 10px; font-weight: 800; }
td { color: #c8d7db; border-top: 1px solid rgba(218, 231, 236, 0.07); }
td strong, td small { display: block; max-width: 220px; overflow: hidden; text-overflow: ellipsis; }
td strong { color: #dfeaec; font-weight: 750; }
td small { margin-top: 3px; color: #7f959d; font-size: 10px; }
.state-pill, .direction-pill { display: inline-flex; align-items: center; min-height: 23px; padding: 0 7px; border-radius: 5px; font-size: 10px; font-weight: 800; }
.state-pill.success { color: #89e3a2; background: rgba(83, 201, 121, 0.14); }
.state-pill.warning { color: #ffdc73; background: rgba(216, 169, 53, 0.14); }
.state-pill.muted { color: #a5b7bd; background: rgba(255, 255, 255, 0.07); }
.direction-pill.credit { color: #8be3a2; background: rgba(83, 201, 121, 0.13); }
.direction-pill.debit { color: #ffb19d; background: rgba(255, 122, 107, 0.12); }
.row-action { min-height: 28px; color: #ffdc73; }
.muted-action { color: #b5c5ca; border-color: rgba(218, 231, 236, 0.12); background: rgba(255, 255, 255, 0.04); }
.muted-text, .table-empty { color: #71868e; }
.table-empty { padding: 28px 14px; text-align: center; }

.modal-mask { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 24px; background: rgba(3, 8, 10, 0.72); }
.payment-modal, .adjustment-modal { width: min(440px, 100%); overflow: hidden; border: 1px solid rgba(218, 231, 236, 0.16); border-radius: 8px; background: #111c20; box-shadow: 0 24px 80px rgba(0, 0, 0, 0.46); }
.adjustment-modal { width: min(520px, 100%); }
.modal-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; padding: 18px 20px; border-bottom: 1px solid rgba(218, 231, 236, 0.1); }
.modal-header h2 { margin: 0; color: #f0f6f7; font-size: 18px; letter-spacing: 0; }
.payment-modal .modal-header h2 { margin-top: 5px; font-size: 22px; }
.adjustment-dialog-form { display: grid; gap: 16px; padding: 20px; }
.adjustment-direction { min-width: 0; margin: 0; padding: 0; border: 0; }
.adjustment-direction legend { margin-bottom: 7px; padding: 0; color: #9bb0b8; font-size: 11px; font-weight: 700; }
.adjustment-direction .direction-control { display: grid; grid-template-columns: repeat(3, 1fr); width: 100%; }
.adjustment-dialog-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 2px; }
.payment-summary { display: grid; gap: 5px; padding: 18px 20px 12px; }
.payment-summary span { color: #8da2aa; font-size: 12px; }
.payment-summary strong { color: #ffdc73; font-size: 28px; letter-spacing: 0; }
.payment-summary small { color: #6f858d; font-size: 10px; }
.payment-qr { display: grid; justify-items: center; gap: 9px; padding: 8px 20px 16px; color: #8fa4ad; font-size: 11px; }
.payment-qr img { display: block; width: 236px; height: 236px; padding: 7px; border-radius: 7px; background: #fff; }
.payment-loading, .payment-result { display: grid; justify-items: center; gap: 9px; padding: 38px 20px; color: #9eb1b8; text-align: center; font-size: 12px; }
.payment-loading svg, .payment-result svg { width: 30px; height: 30px; color: #d8a935; }
.payment-result strong { color: #e6f0f2; font-size: 15px; }
.success-result svg { color: var(--green); }
.success-result strong { color: #96e7aa; }
.payment-error { margin: 0 20px 12px; padding: 10px 11px; color: #ffb2a8; border: 1px solid rgba(255, 122, 107, 0.2); border-radius: 6px; background: rgba(255, 122, 107, 0.08); font-size: 12px; line-height: 1.45; }
.payment-actions { display: flex; justify-content: center; gap: 8px; padding: 0 20px 20px; }
.spinning { animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1100px) {
  .purchase-layout { grid-template-columns: 1fr; }
}

@media (max-width: 820px) {
  .beer-coins-page { padding: 0 16px 18px; }
  .records-layout { grid-template-columns: 1fr; }
  .ledger-panel { grid-column: auto; }
  .wallet-panel { grid-template-columns: 1fr 1fr; }
  .wallet-main { grid-column: 1 / -1; }
  .purchase-form { align-items: stretch; flex-direction: column; }
  .purchase-button { width: 100%; }
  .editor-tier-head { display: none; }
  .editor-tier-row { grid-template-columns: 1fr 1fr 38px; }
  .pricing-trial { flex-wrap: wrap; }
  .pricing-trial > .compact-field { flex: 1 1 240px; }
  .accounts-toolbar { align-items: stretch; flex-direction: column; }
  .account-filter { width: 100%; }
  .account-facts { grid-template-columns: 1fr; }
  .account-facts > div + div { border-top: 1px solid rgba(218, 231, 236, 0.09); border-left: 0; }
  .record-tabs { overflow-x: auto; }
  .record-tabs button { flex: 0 0 auto; }
  .organizer-section-tabs { gap: 18px; overflow-x: auto; }
  .organizer-section-tabs button { flex: 0 0 auto; }
  .danger-icon { grid-column: 3; justify-self: end; }
}

@media (max-width: 540px) {
  .wallet-panel { grid-template-columns: 1fr; }
  .wallet-main { grid-column: auto; }
  .segment-row { grid-template-columns: 1fr auto; }
  .segment-row span:nth-child(2) { grid-column: 1 / -1; grid-row: 2; }
  .editor-tier-row { grid-template-columns: minmax(0, 1fr) 38px; }
  .tier-range-editor { grid-column: 1 / -1; }
  .editor-tier-row > .tier-price-input { grid-column: 1; }
  .pricing-trial { flex-direction: column; align-items: stretch; }
  .pricing-trial > .compact-field { flex: 0 0 auto; }
  .pricing-trial > div { justify-content: space-between; }
  .danger-icon { grid-column: 2; justify-self: end; }
}

@media (prefers-reduced-motion: reduce) {
  .spinning { animation: none; }
}
</style>
