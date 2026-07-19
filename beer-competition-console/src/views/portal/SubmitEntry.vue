<template>
  <div class="batch-submit-page">
    <main class="batch-workbench brewer-card">
      <header class="page-heading">
        <div>
          <span class="section-kicker">赛事报名</span>
          <h1>提交参赛酒款</h1>
        </div>
        <RouterLink v-if="competition" :to="`/portal/events/${competition.id}`">赛事详情</RouterLink>
      </header>

      <section v-if="competition" class="competition-strip">
        <span>正在报名</span>
        <strong>{{ competition.name }}</strong>
        <b>{{ feeText }}</b>
      </section>

      <section v-else-if="loading" class="page-state">正在读取赛事信息…</section>
      <section v-else class="page-state">
        <strong>未找到可报名赛事</strong>
        <RouterLink to="/portal/events">返回赛事列表</RouterLink>
      </section>

      <template v-if="competition">
        <section class="entry-switcher" aria-label="报名酒款列表">
          <div class="entry-tabs">
            <button
              v-for="(entry, index) in entries"
              :key="entry.clientId"
              :class="['entry-tab', { active: index === activeIndex }]"
              type="button"
              @click="activeIndex = index"
            >
              <span>{{ index + 1 }}</span>
              <span class="entry-tab-copy">
                <strong>{{ entry.name || `酒款 ${index + 1}` }}</strong>
                <small :class="{ complete: entryErrorCount(entry) === 0 }">
                  {{ entryErrorCount(entry) === 0 ? '资料完整' : `缺 ${entryErrorCount(entry)} 项` }}
                </small>
              </span>
            </button>
          </div>
          <el-button
            class="add-entry-button"
            :icon="Plus"
            :disabled="entries.length >= MAX_BATCH_ENTRIES"
            @click="addEntry"
          >
            添加酒款
          </el-button>
        </section>

        <div class="entry-editor-head">
          <div>
            <span>第 {{ activeIndex + 1 }} 款，共 {{ entries.length }} 款</span>
            <h2>{{ activeEntry.name || '填写酒款资料' }}</h2>
          </div>
          <div class="entry-tools">
            <el-button
              class="delete-entry-button"
              :icon="Delete"
              :disabled="entries.length === 1"
              @click="removeEntry"
            >
              删除本款
            </el-button>
          </div>
        </div>

        <el-form
          ref="activeFormRef"
          :key="activeEntry.clientId"
          :model="activeEntry"
          :rules="formRules"
          label-position="top"
          class="entry-form"
        >
          <section class="form-section">
            <div class="section-title-row">
              <h3>酒款信息</h3>
            </div>
            <el-form-item label="酒款名称" prop="name">
              <el-input v-model.trim="activeEntry.name" maxlength="128" placeholder="请输入酒款名称..." />
            </el-form-item>
          </section>

          <section class="form-section">
            <div class="section-title-row">
              <h3>分类与风格</h3>
            </div>
            <div class="form-grid">
              <el-form-item label="投递组别" prop="categoryId">
                <el-select v-model="activeEntry.categoryId" placeholder="请选择投递组别…">
                  <el-option
                    v-for="item in competition.categories || []"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="基础风格" prop="style">
                <el-select v-model="activeEntry.style" filterable placeholder="请选择或搜索基础风格…">
                  <el-option
                    v-for="item in competition.styles || []"
                    :key="item.id || item.name"
                    :label="styleLabel(item)"
                    :value="item.name"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="ABV" prop="abv">
                <el-input
                  v-model.trim="activeEntry.abv"
                  inputmode="decimal"
                  placeholder="请输入酒精度"
                  @blur="activeEntry.abv = normalizeAbvInput(activeEntry.abv)"
                >
                  <template #suffix><b class="input-suffix">%</b></template>
                </el-input>
              </el-form-item>
            </div>
          </section>

          <section v-if="configuredFields.length" class="form-section">
            <div class="section-title-row">
              <h3>补充信息</h3>
            </div>
            <div class="dynamic-field-list">
              <el-form-item
                v-for="field in configuredFields"
                :key="field.fieldKey"
                :label="field.fieldLabel"
                :prop="`extraFields.${field.fieldKey}`"
                :class="['dynamic-field', { wide: field.fieldType === 'textarea' }]"
              >
                <template #label>
                  <span class="field-label">
                    {{ field.fieldLabel }}
                    <em v-if="field.required">必填</em>
                  </span>
                </template>
                <el-input
                  v-if="field.fieldType === 'textarea'"
                  v-model.trim="activeEntry.extraFields[field.fieldKey]"
                  type="textarea"
                  :rows="4"
                  maxlength="255"
                  show-word-limit
                  placeholder="选填…"
                />
                <el-input-number
                  v-else-if="field.fieldType === 'number'"
                  v-model="activeEntry.extraFields[field.fieldKey]"
                  :min="0"
                  controls-position="right"
                />
                <el-select
                  v-else-if="field.fieldType === 'select'"
                  v-model="activeEntry.extraFields[field.fieldKey]"
                  placeholder="请选择…"
                >
                  <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
                </el-select>
                <el-select
                  v-else-if="field.fieldType === 'multi_select'"
                  v-model="activeEntry.extraFields[field.fieldKey]"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="请选择，可多选…"
                >
                  <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
                </el-select>
                <el-input
                  v-else
                  v-model.trim="activeEntry.extraFields[field.fieldKey]"
                  maxlength="255"
                  :placeholder="field.helpText || '请填写…'"
                />
                <p v-if="field.helpText" class="field-help">{{ field.helpText }}</p>
              </el-form-item>
            </div>
          </section>
        </el-form>

        <section v-if="hasRulesUrl" class="rules-panel">
          <el-checkbox v-model="rulesAccepted">
            我已阅读并同意本次大赛
            <a :href="competition.rulesUrl" target="_blank" rel="noopener noreferrer" @click.stop>参赛细则</a>
          </el-checkbox>
          <p v-if="showRulesError">请先阅读并同意本次大赛参赛细则</p>
        </section>

        <section class="payment-panel">
          <div>
            <h3>选择付款方式</h3>
          </div>
          <div class="payment-options" role="radiogroup" aria-label="选择付款方式">
            <button
              :class="['payment-option', { active: payMode === 'WECHAT' }]"
              type="button"
              role="radio"
              :aria-checked="payMode === 'WECHAT'"
              @click="payMode = 'WECHAT'"
            >
              <span>微信支付</span>
              <small>支付成功后完成报名</small>
            </button>
            <button
              :class="['payment-option', { active: payMode === 'BANK_TRANSFER' }]"
              type="button"
              role="radio"
              :aria-checked="payMode === 'BANK_TRANSFER'"
              @click="payMode = 'BANK_TRANSFER'"
            >
              <span>银行转账</span>
              <small>转账后等待到账确认</small>
            </button>
          </div>
        </section>
      </template>
    </main>

    <aside v-if="competition" class="receipt-column">
      <section class="receipt-card">
        <header>
          <span>报名确认</span>
          <b>{{ entries.length }} 款</b>
        </header>
        <div class="receipt-rule" />
        <dl class="event-fact">
          <dt>赛事</dt>
          <dd>{{ competition.name }}</dd>
        </dl>

        <div class="receipt-items">
          <button
            v-for="(entry, index) in entries"
            :key="entry.clientId"
            type="button"
            @click="activeIndex = index"
          >
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="receipt-entry-copy">
              <strong>{{ entry.name || `酒款 ${index + 1}` }}</strong>
              <small>{{ entryMeta(entry) }}</small>
            </span>
            <span :class="['entry-check', { complete: entryErrorCount(entry) === 0 }]">
              <CircleCheck v-if="entryErrorCount(entry) === 0" />
              <WarningFilled v-else />
            </span>
            <b>{{ formatCurrency(unitAmount) }}</b>
          </button>
        </div>

        <div class="receipt-totals">
          <div><span>报名费</span><b>{{ formatCurrency(unitAmount) }} × {{ entries.length }}</b></div>
          <div v-if="discountAmount > 0"><span>早鸟优惠</span><b>-{{ formatCurrency(discountAmount) }}</b></div>
          <div class="receipt-total"><span>应付总额</span><strong>{{ formatCurrency(totalAmount) }}</strong></div>
          <div><span>付款方式</span><b>{{ payMode === 'WECHAT' ? '微信支付' : '银行转账' }}</b></div>
        </div>

        <el-button class="submit-batch-button" type="primary" :loading="submitting" @click="submitBatch">
          {{ submitButtonLabel }}
          <el-icon><ArrowRight /></el-icon>
        </el-button>
        <p v-if="quote?.earlyBirdActive" class="price-note">
          当前按早鸟价结算，订单提交时锁定本批价格
        </p>
      </section>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, RouterLink, useRoute, useRouter } from 'vue-router'
import { ArrowRight, CircleCheck, Delete, Plus, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchPortalCompetitionDetail, quotePortalEntryBatch, submitPortalEntryBatch } from '@/api/portal'
import { isValidAbvInput, normalizeAbvInput } from '@/utils/formatters'

const MAX_BATCH_ENTRIES = 20
const route = useRoute()
const router = useRouter()
const activeFormRef = ref(null)
const competition = ref(null)
const entries = ref([])
const activeIndex = ref(0)
const rulesAccepted = ref(false)
const payMode = ref('WECHAT')
const quote = ref(null)
const loading = ref(true)
const submitting = ref(false)
const hydrated = ref(false)
const dirty = ref(false)
const showRulesError = ref(false)
const competitionId = Number(route.query.competitionId || 0)
const draftKey = `portal-entry-batch-draft:${competitionId}`
const idempotencyKey = ref(createRequestKey())
const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

const configuredFields = computed(() => normalizeEntryFields(competition.value?.entryFields || []))
const activeEntry = computed(() => entries.value[activeIndex.value] || entries.value[0])
const hasRulesUrl = computed(() => Boolean(competition.value?.rulesUrl))
const unitAmount = computed(() => Number(quote.value?.unitAmount ?? competition.value?.entryFee ?? 0))
const totalAmount = computed(() => Number(quote.value?.totalAmount ?? unitAmount.value * entries.value.length))
const discountAmount = computed(() => Number(quote.value?.discountAmount || 0))
const feeText = computed(() => `${formatCurrency(unitAmount.value)} / 款`)
const submitButtonLabel = computed(() => {
  if (totalAmount.value <= 0) return `提交 ${entries.value.length} 款报名`
  if (payMode.value === 'BANK_TRANSFER') return `提交 ${entries.value.length} 款，查看转账信息`
  return `提交 ${entries.value.length} 款，前往付款`
})

const formRules = computed(() => {
  const rules = {
    name: [{ required: true, message: '请填写酒款名称', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择投递组别', trigger: 'change' }],
    style: [{ required: true, message: '请选择基础风格', trigger: 'change' }],
    abv: [{
      validator: (_rule, value, callback) => {
        if (isValidAbvInput(value)) callback()
        else callback(new Error('请填写 0-99.99 之间的 ABV，最多两位小数'))
      },
      trigger: ['blur', 'change'],
    }],
  }
  configuredFields.value.filter((field) => field.required).forEach((field) => {
    rules[`extraFields.${field.fieldKey}`] = [{
      validator: (_rule, value, callback) => {
        if (hasFieldValue(value)) callback()
        else callback(new Error(`请填写${field.fieldLabel}`))
      },
      trigger: ['blur', 'change'],
    }]
  })
  return rules
})

onMounted(async () => {
  try {
    if (!competitionId) return
    competition.value = await fetchPortalCompetitionDetail(competitionId)
    restoreDraft()
    if (!entries.value.length) entries.value = [createEmptyEntry()]
    await refreshQuote()
  } catch (error) {
    ElMessage.warning(error?.message || '赛事信息读取失败')
  } finally {
    loading.value = false
    hydrated.value = true
    window.addEventListener('beforeunload', handleBeforeUnload)
  }
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload))

onBeforeRouteLeave(() => {
  if (!dirty.value || submitting.value) return true
  return window.confirm('报名资料尚未提交，确定离开当前页面吗？')
})

watch([entries, rulesAccepted, payMode], () => {
  if (!hydrated.value) return
  dirty.value = true
  saveDraft()
}, { deep: true })

watch(() => entries.value.length, () => {
  if (hydrated.value) refreshQuote()
})

function createEmptyEntry() {
  const extraFields = Object.fromEntries(configuredFields.value.map((field) => [
    field.fieldKey,
    emptyFieldValue(field),
  ]))
  return {
    clientId: globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`,
    name: '',
    categoryId: null,
    style: '',
    abv: '',
    extraFields,
  }
}

function addEntry() {
  if (entries.value.length >= MAX_BATCH_ENTRIES) return
  entries.value.push(createEmptyEntry())
  activeIndex.value = entries.value.length - 1
  nextTick(() => activeFormRef.value?.clearValidate())
}

async function removeEntry() {
  if (entries.value.length === 1) return
  const entryName = activeEntry.value.name || `酒款 ${activeIndex.value + 1}`
  const confirmed = await ElMessageBox.confirm(`“${entryName}”将从本次报名中移除，应付金额会同步更新。`, '确认删除这款酒？', {
    confirmButtonText: '确认删除',
    cancelButtonText: '暂不删除',
    type: 'warning',
    customClass: 'entry-delete-dialog',
    closeOnClickModal: false,
  }).catch(() => false)
  if (!confirmed) return
  entries.value.splice(activeIndex.value, 1)
  activeIndex.value = Math.min(activeIndex.value, entries.value.length - 1)
}

async function refreshQuote() {
  if (!competition.value || !entries.value.length) return
  try {
    quote.value = await quotePortalEntryBatch(competition.value.id, entries.value.length)
  } catch {
    quote.value = null
  }
}

async function submitBatch() {
  if (submitting.value) return
  const invalidIndex = entries.value.findIndex((entry) => entryErrorCount(entry) > 0)
  if (invalidIndex >= 0) {
    activeIndex.value = invalidIndex
    await nextTick()
    await activeFormRef.value?.validate().catch(() => false)
    ElMessage.warning(`请先补全第 ${invalidIndex + 1} 款酒的报名资料`)
    return
  }
  if (hasRulesUrl.value && !rulesAccepted.value) {
    showRulesError.value = true
    ElMessage.warning('请先阅读并同意本次大赛参赛细则')
    return
  }
  showRulesError.value = false
  submitting.value = true
  try {
    const batch = await submitPortalEntryBatch(competition.value.id, {
      idempotencyKey: idempotencyKey.value,
      expectedTotalAmount: totalAmount.value,
      rulesAccepted: hasRulesUrl.value ? rulesAccepted.value : undefined,
      entries: entries.value.map((entry) => ({
        name: entry.name.trim(),
        categoryId: entry.categoryId,
        style: entry.style,
        abv: Number(normalizeAbvInput(entry.abv)),
        extraFields: entry.extraFields,
      })),
    })
    dirty.value = false
    localStorage.removeItem(draftKey)
    ElMessage.success(`${batch.entryCount} 款酒已提交`)
    await router.push({
      path: '/portal/batch-payment',
      query: { batchId: batch.id, orderId: batch.paymentOrderId, payMode: payMode.value.toLowerCase() },
    })
  } catch (error) {
    await refreshQuote()
    ElMessage.warning(error?.message || '报名提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function entryErrorCount(entry) {
  if (!entry) return 0
  let count = 0
  if (!entry.name?.trim()) count += 1
  if (!entry.categoryId) count += 1
  if (!entry.style) count += 1
  if (!isValidAbvInput(entry.abv)) count += 1
  configuredFields.value.forEach((field) => {
    if (field.required && !hasFieldValue(entry.extraFields?.[field.fieldKey])) count += 1
  })
  return count
}

function entryMeta(entry) {
  const category = competition.value?.categories?.find((item) => item.id === entry.categoryId)?.name
  if (entryErrorCount(entry) > 0) return `还缺 ${entryErrorCount(entry)} 项`
  return [category, entry.style].filter(Boolean).join(' · ')
}

function saveDraft() {
  localStorage.setItem(draftKey, JSON.stringify({
    competitionId,
    entries: entries.value,
    activeIndex: activeIndex.value,
    rulesAccepted: rulesAccepted.value,
    payMode: payMode.value,
    idempotencyKey: idempotencyKey.value,
    savedAt: Date.now(),
  }))
}

function restoreDraft() {
  try {
    const draft = JSON.parse(localStorage.getItem(draftKey) || 'null')
    if (!draft || draft.competitionId !== competitionId || !Array.isArray(draft.entries) || !draft.entries.length) return
    entries.value = draft.entries.slice(0, MAX_BATCH_ENTRIES).map((entry) => ({
      ...createEmptyEntry(),
      ...entry,
      extraFields: Object.fromEntries(configuredFields.value.map((field) => [
        field.fieldKey,
        entry.extraFields?.[field.fieldKey] ?? emptyFieldValue(field),
      ])),
    }))
    activeIndex.value = Math.min(Number(draft.activeIndex || 0), entries.value.length - 1)
    rulesAccepted.value = Boolean(draft.rulesAccepted)
    payMode.value = draft.payMode === 'BANK_TRANSFER' ? 'BANK_TRANSFER' : 'WECHAT'
    idempotencyKey.value = draft.idempotencyKey || idempotencyKey.value
    ElMessage.success('已恢复上次未提交的报名资料')
  } catch {
    localStorage.removeItem(draftKey)
  }
}

function handleBeforeUnload(event) {
  if (!dirty.value || submitting.value) return
  event.preventDefault()
  event.returnValue = ''
}

function createRequestKey() {
  return globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`
}

function normalizeEntryFields(fields) {
  return fields.map((field, index) => ({
    fieldKey: field.fieldKey || `custom_${index}`,
    fieldLabel: field.fieldLabel || field.label || `补充字段 ${index + 1}`,
    fieldType: field.fieldType || field.type || 'text',
    helpText: field.helpText || '',
    options: Array.isArray(field.options) ? field.options : [],
    required: Boolean(field.required),
    sortOrder: Number(field.sortOrder ?? index),
  })).sort((a, b) => a.sortOrder - b.sortOrder)
}

function emptyFieldValue(field) {
  if (field.fieldType === 'multi_select') return []
  if (field.fieldType === 'number') return null
  return ''
}

function hasFieldValue(value) {
  if (Array.isArray(value)) return value.length > 0
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function styleLabel(item) {
  return item?.styleCode ? `${item.styleCode} ${item.name}` : item?.name
}

function formatCurrency(value) {
  return currencyFormatter.format(Number(value || 0))
}
</script>

<style scoped>
.batch-submit-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 350px;
  gap: 20px;
  align-items: start;
}

.batch-workbench { padding: 24px; }

.page-heading,
.competition-strip,
.entry-switcher,
.entry-editor-head,
.payment-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.page-heading h1,
.entry-editor-head h2,
.payment-panel h3 { margin: 4px 0 0; color: #2b1d10; }
.page-heading h1 { font-size: 25px; }
.page-heading a { color: #86540f; font-weight: 800; text-decoration: none; }
.section-kicker { color: #96611b; font-size: 11px; font-weight: 900; letter-spacing: .08em; }

.competition-strip {
  margin-top: 18px;
  padding: 13px 15px;
  justify-content: flex-start;
  color: #68451b;
  background: #fff6df;
  border: 1px solid rgba(87, 58, 26, .12);
  border-radius: 8px;
}
.competition-strip span { font-size: 12px; font-weight: 800; }
.competition-strip strong { color: #2b1d10; font-size: 17px; }
.competition-strip b { margin-left: auto; font-size: 13px; }

.entry-switcher {
  margin: 18px -24px 0;
  padding: 14px 24px;
  border-top: 1px solid rgba(87, 58, 26, .1);
  border-bottom: 1px solid rgba(87, 58, 26, .1);
}
.entry-tabs { display: flex; gap: 8px; min-width: 0; overflow-x: auto; padding: 2px; }
.entry-tab {
  display: flex;
  gap: 9px;
  align-items: center;
  min-width: 150px;
  padding: 9px 11px;
  color: #5e4b38;
  text-align: left;
  background: #fffdf8;
  border: 1px solid rgba(87, 58, 26, .13);
  border-radius: 7px;
  cursor: pointer;
}
.entry-tab:hover { border-color: rgba(166, 101, 20, .45); }
.entry-tab.active { background: #fff2ce; border-color: #b87820; box-shadow: 0 0 0 2px rgba(184, 120, 32, .1); }
.entry-tab > span:first-child {
  display: grid; width: 27px; height: 27px; place-items: center; color: #fff; background: #9b651d; border-radius: 50%; font-weight: 900;
}
.entry-tab-copy { display: grid; min-width: 0; gap: 2px; }
.entry-tab-copy strong { max-width: 100px; overflow: hidden; color: #2b1d10; text-overflow: ellipsis; white-space: nowrap; }
.entry-tab-copy small { color: #a55f20; }
.entry-tab-copy small.complete { color: #36754b; }

.entry-editor-head { padding: 22px 0 10px; align-items: flex-end; }
.entry-editor-head span { color: #8a765f; font-size: 12px; font-weight: 700; }
.entry-editor-head h2 { font-size: 20px; }
.entry-tools { display: flex; gap: 8px; }
.add-entry-button {
  min-width: 108px;
  height: 36px;
  color: #75470d;
  background: #fffaf0;
  border-color: rgba(117, 71, 13, .28);
  border-radius: 7px;
  font-weight: 800;
}
.add-entry-button:hover,
.add-entry-button:focus-visible {
  color: #5f3707;
  background: #fff1c9;
  border-color: #b87820;
}
.delete-entry-button {
  height: 34px;
  padding: 0 10px;
  color: #8a513a;
  background: transparent;
  border-color: transparent;
  border-radius: 6px;
  font-weight: 700;
}
.delete-entry-button:hover,
.delete-entry-button:focus-visible {
  color: #7c321f;
  background: #fff0e8;
  border-color: rgba(139, 61, 41, .24);
}
.delete-entry-button.is-disabled {
  color: #b6a99d;
  background: transparent;
  border-color: transparent;
}

.form-section { padding: 17px 0; border-top: 1px solid rgba(87, 58, 26, .1); }
.section-title-row { margin-bottom: 13px; }
.section-title-row h3 { margin: 0; color: #2b1d10; font-size: 16px; }
.form-grid { display: grid; grid-template-columns: minmax(180px, 1fr) minmax(220px, 1.2fr) minmax(140px, .65fr); gap: 18px; }
.dynamic-field-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 18px; }
.dynamic-field.wide { grid-column: 1 / -1; }
.entry-form :deep(.el-input__wrapper),
.entry-form :deep(.el-select__wrapper),
.entry-form :deep(.el-textarea__inner),
.entry-form :deep(.el-input-number) { width: 100%; background: #fffdf8; border-radius: 7px; box-shadow: 0 0 0 1px rgba(87, 58, 26, .15) inset; }
.entry-form :deep(.el-form-item__label) { color: #514338; font-weight: 800; }
.input-suffix { color: #5f4d3d; }
.field-label { display: inline-flex; gap: 7px; align-items: center; }
.field-label em { padding: 1px 6px; color: #8b5105; background: #ffe2a3; border-radius: 999px; font-size: 10px; font-style: normal; }
.field-help { margin: 6px 0 0; color: #7d6b58; font-size: 12px; }

.rules-panel { padding: 15px 17px; background: #fff7e6; border: 1px dashed rgba(87, 58, 26, .22); border-radius: 8px; }
.rules-panel a { color: #83520f; font-weight: 900; text-underline-offset: 3px; }
.rules-panel p { margin: 6px 0 0 24px; color: #c45656; font-size: 12px; }

.payment-panel { margin-top: 16px; padding-top: 18px; border-top: 1px solid rgba(87, 58, 26, .1); align-items: flex-start; }
.payment-panel h3 { font-size: 17px; }
.payment-options { display: grid; grid-template-columns: repeat(2, minmax(170px, 1fr)); gap: 10px; }
.payment-option { display: grid; gap: 3px; padding: 12px 14px; text-align: left; color: #493a2d; background: #fffdf8; border: 1px solid rgba(87, 58, 26, .16); border-radius: 7px; cursor: pointer; }
.payment-option:hover { border-color: rgba(166, 101, 20, .48); }
.payment-option.active { background: #fff1c7; border-color: #ae6f19; box-shadow: 0 0 0 2px rgba(174, 111, 25, .09); }
.payment-option span { font-weight: 900; }
.payment-option small { color: #7d6a56; }

.receipt-column { position: sticky; top: 116px; }
.receipt-card { overflow: hidden; padding: 22px; color: #2d2115; background: linear-gradient(180deg, #fffaf0 0%, #f3d58f 72%, #e8bd60 100%); border: 1px solid rgba(87, 58, 26, .2); border-radius: 8px; box-shadow: 0 22px 44px rgba(83, 51, 17, .14); }
.receipt-card > header { display: flex; justify-content: space-between; align-items: baseline; }
.receipt-card > header span { font-size: 27px; font-weight: 900; }
.receipt-card > header b { color: #795018; font-size: 13px; }
.receipt-rule { height: 1px; margin: 18px 0; background: repeating-linear-gradient(90deg, rgba(43, 29, 16, .42) 0 7px, transparent 7px 13px); }
.event-fact { margin: 0 0 14px; }
.event-fact dt { color: #86725d; font-size: 12px; }
.event-fact dd { margin: 5px 0 0; font-weight: 900; line-height: 1.45; }
.receipt-items { max-height: 285px; overflow-y: auto; border-top: 1px solid rgba(91, 61, 25, .13); }
.receipt-items button { display: grid; grid-template-columns: 24px minmax(0, 1fr) 20px auto; gap: 8px; align-items: center; width: 100%; padding: 12px 0; color: inherit; text-align: left; background: transparent; border: 0; border-bottom: 1px solid rgba(91, 61, 25, .13); cursor: pointer; }
.receipt-items button:hover strong { color: #81500d; }
.receipt-entry-copy { display: grid; gap: 3px; min-width: 0; }
.receipt-entry-copy strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.receipt-entry-copy small { overflow: hidden; color: #806d57; text-overflow: ellipsis; white-space: nowrap; }
.entry-check { width: 17px; height: 17px; color: #a55f20; }
.entry-check.complete { color: #3d7750; }
.entry-check svg { width: 100%; height: 100%; }
.receipt-items button > b { font-variant-numeric: tabular-nums; }
.receipt-totals { display: grid; gap: 10px; padding: 16px 0 4px; }
.receipt-totals div { display: flex; justify-content: space-between; gap: 15px; color: #725d44; font-size: 13px; }
.receipt-totals b { color: #372719; font-variant-numeric: tabular-nums; }
.receipt-totals .receipt-total { align-items: baseline; padding: 12px 0; color: #2b1d10; border-top: 1px dashed rgba(43, 29, 16, .3); border-bottom: 1px dashed rgba(43, 29, 16, .3); font-weight: 900; }
.receipt-total strong { color: #704408; font-size: 27px; font-variant-numeric: tabular-nums; }
.submit-batch-button { width: 100%; min-height: 50px; margin-top: 16px; border: 0; border-radius: 7px; background: #7d4d12; font-size: 15px; font-weight: 900; }
.submit-batch-button:hover { background: #925d19; }
.price-note { margin: 10px 0 0; color: #725b3d; text-align: center; font-size: 12px; }
.page-state { display: grid; gap: 12px; padding: 40px; text-align: center; }

:global(.entry-delete-dialog) {
  width: min(430px, calc(100vw - 32px));
  padding: 0;
  overflow: hidden;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, .22);
  border-radius: 8px;
  box-shadow: 0 24px 64px rgba(57, 36, 16, .24);
}
:global(.entry-delete-dialog .el-message-box__header) {
  padding: 20px 22px 10px;
}
:global(.entry-delete-dialog .el-message-box__title) {
  color: #2b1d10;
  font-size: 19px;
  font-weight: 900;
}
:global(.entry-delete-dialog .el-message-box__headerbtn) {
  top: 17px;
  right: 17px;
}
:global(.entry-delete-dialog .el-message-box__close) { color: #927c66; }
:global(.entry-delete-dialog .el-message-box__content) {
  padding: 8px 22px 20px;
  color: #66513e;
}
:global(.entry-delete-dialog .el-message-box__container) { align-items: flex-start; }
:global(.entry-delete-dialog .el-message-box__status) {
  margin-top: 2px;
  color: #b87820;
  font-size: 22px;
}
:global(.entry-delete-dialog .el-message-box__message) {
  padding-left: 12px;
  line-height: 1.7;
}
:global(.entry-delete-dialog .el-message-box__btns) {
  gap: 8px;
  padding: 14px 22px 18px;
  background: #fff4d9;
  border-top: 1px solid rgba(87, 58, 26, .12);
}
:global(.entry-delete-dialog .el-button) {
  min-width: 88px;
  height: 36px;
  border-radius: 6px;
  font-weight: 800;
}
:global(.entry-delete-dialog .el-button--default) {
  color: #684d34;
  background: #fffdf8;
  border-color: rgba(87, 58, 26, .2);
}
:global(.entry-delete-dialog .el-button--default:hover) {
  color: #4d321c;
  background: #fff8e8;
  border-color: rgba(87, 58, 26, .38);
}
:global(.entry-delete-dialog .el-button--primary) {
  color: #fff;
  background: #8a3f2a;
  border-color: #8a3f2a;
}
:global(.entry-delete-dialog .el-button--primary:hover) {
  background: #76311f;
  border-color: #76311f;
}

@media (max-width: 1160px) {
  .batch-submit-page { grid-template-columns: 1fr; }
  .receipt-column { position: static; }
}

@media (max-width: 760px) {
  .batch-workbench { padding: 18px; }
  .page-heading, .competition-strip, .entry-editor-head, .payment-panel { align-items: flex-start; flex-direction: column; }
  .competition-strip b { margin-left: 0; }
  .entry-switcher { margin-right: -18px; margin-left: -18px; padding: 12px 18px; align-items: flex-start; flex-direction: column; }
  .entry-tabs { width: 100%; }
  .form-grid, .dynamic-field-list, .payment-options { grid-template-columns: 1fr; width: 100%; }
  .dynamic-field.wide { grid-column: auto; }
  .entry-tools { width: 100%; }
}
</style>
