<template>
  <div class="submit-page">
    <section class="form-card brewer-card">
      <div class="form-header">
        <h2 class="portal-section-title">提交参赛酒款</h2>
        <p>先完成酒款资料，提交后支付报名费。</p>
      </div>

      <section v-if="selectedCompetition" class="competition-banner">
        <div>
          <span>正在报名：</span>
          <strong>{{ selectedCompetition.name }}</strong>
        </div>
        <RouterLink :to="`/portal/events/${selectedCompetition.id}`">赛事详情</RouterLink>
      </section>

      <section v-else class="competition-empty">
        <strong>未找到赛事</strong>
        <p>请从赛事详情页进入报名，或先返回赛事列表重新选择。</p>
        <RouterLink class="secondary-link" to="/portal/events">返回赛事列表</RouterLink>
      </section>

      <el-form
        v-if="selectedCompetition"
        ref="entryFormRef"
        :model="form"
        :rules="formRules"
        :disabled="Boolean(submittedEntry)"
        :validate-on-rule-change="false"
        label-position="top"
        class="entry-form"
      >
        <section class="form-section">
          <div class="section-title-row">
            <h3>酒款信息</h3>
            <span v-if="selectedCompetition?.entryFee">报名费 ¥{{ selectedCompetition.entryFee }} / 款</span>
          </div>
          <el-form-item label="酒款名称" prop="name" class="full-field">
            <el-input v-model.trim="form.name" placeholder="例如：浅色拉格" />
          </el-form-item>
        </section>

        <section class="form-section">
          <div class="section-title-row">
            <h3>分类与风格</h3>
          </div>
          <div class="form-grid">
            <el-form-item label="投递组别" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择投递组别">
                <el-option
                  v-for="item in selectedCompetition?.categories || []"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="基础风格" prop="style">
              <el-select v-model="form.style" filterable placeholder="请选择或搜索基础风格">
                <el-option
                  v-for="item in selectedCompetition?.styles || []"
                  :key="item.id || item.name"
                  :label="styleLabel(item)"
                  :value="item.name"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="ABV" prop="abv">
              <div class="number-field">
                <el-input-number v-model="form.abv" :min="0" :max="20" :precision="1" :step="0.1" controls-position="right" />
                <span>%</span>
              </div>
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
              class="dynamic-field"
            >
              <template #label>
                <span class="field-label">
                  <span>{{ field.fieldLabel }}</span>
                  <em v-if="field.required">必填</em>
                </span>
              </template>

              <el-input
                v-if="field.fieldType === 'textarea'"
                v-model.trim="form.extraFields[field.fieldKey]"
                type="textarea"
                :rows="4"
                maxlength="300"
                show-word-limit
                placeholder="选填"
              />
              <el-input-number
                v-else-if="field.fieldType === 'number'"
                v-model="form.extraFields[field.fieldKey]"
                :min="0"
                controls-position="right"
                placeholder="请输入数字"
              />
              <el-select
                v-else-if="field.fieldType === 'select'"
                v-model="form.extraFields[field.fieldKey]"
                placeholder="请选择"
              >
                <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
              </el-select>
              <el-select
                v-else-if="field.fieldType === 'multi_select'"
                v-model="form.extraFields[field.fieldKey]"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择，可多选"
              >
                <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
              </el-select>
              <el-input
                v-else
                v-model.trim="form.extraFields[field.fieldKey]"
                :placeholder="field.helpText || '请填写'"
              />
              <p v-if="field.helpText" class="field-help">{{ field.helpText }}</p>
            </el-form-item>
          </div>
        </section>

        <el-form-item prop="confirmed" class="confirm-item">
          <div class="confirm-box">
            <el-checkbox v-model="form.confirmed">
              我确认评审可见内容不含厂牌或联系方式
            </el-checkbox>
          </div>
        </el-form-item>

        <div v-if="!submittedEntry" class="form-actions">
          <el-button type="primary" :loading="submittingEntry" @click="submitEntry">
            提交报名并支付 {{ entryFeeLabel }}
          </el-button>
        </div>
      </el-form>

      <section v-if="submittedEntry" class="inline-payment">
        <div class="payment-head">
          <div>
            <span>报名支付</span>
            <h3>{{ paymentPaid ? '支付成功' : '支付报名费' }}</h3>
            <p>{{ paymentPaid ? '这一款酒已经报名成功，可以继续下载标签并填写送样信息。' : '当前使用模拟收款，后续接入微信支付后会在这里拉起微信支付。' }}</p>
          </div>
          <strong>{{ formatCurrency(paymentAmount) }}</strong>
        </div>

        <dl class="payment-facts">
          <div>
            <dt>酒款</dt>
            <dd>{{ submittedEntry.name }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ paymentPaid ? '报名成功' : '待支付' }}</dd>
          </div>
        </dl>

        <div class="payment-actions">
          <el-button
            v-if="!paymentPaid"
            type="primary"
            size="large"
            :loading="simulatingPayment"
            @click="paySubmittedEntry"
          >
            模拟支付成功
          </el-button>
          <RouterLink
            v-else
            class="primary-link"
            :to="`/portal/payment?entryId=${submittedEntry.id}`"
          >
            下载标签并填写送样信息
          </RouterLink>
          <el-button :disabled="simulatingPayment" @click="resetForNextEntry">继续提交另一款</el-button>
        </div>
      </section>
    </section>

    <aside v-if="selectedCompetition" class="preview-card">
      <div class="preview-label">
        <span>报名核对</span>
        <h3>报名核对单</h3>
        <template v-if="previewReady">
          <div class="receipt-line" />
          <dl>
            <div>
              <dt>赛事</dt>
              <dd>{{ selectedCompetition.name }}</dd>
            </div>
            <div>
              <dt>酒款</dt>
              <dd>{{ form.name || '-' }}</dd>
            </div>
            <div>
              <dt>组别</dt>
              <dd>{{ selectedCategoryName }}</dd>
            </div>
            <div>
              <dt>基础风格</dt>
              <dd>{{ form.style }}</dd>
            </div>
            <div>
              <dt>ABV</dt>
              <dd>{{ form.abv }}%</dd>
            </div>
            <div v-for="field in configuredFields" :key="field.fieldKey">
              <dt>{{ field.fieldLabel }}</dt>
              <dd>{{ formatFieldValue(form.extraFields[field.fieldKey]) || '-' }}</dd>
            </div>
          </dl>
          <div class="foam-line" />
          <p class="receipt-status">{{ submittedEntry ? (paymentPaid ? '已支付，报名成功' : '待支付报名费') : '提交后支付报名费' }}</p>
        </template>
        <p v-else class="preview-empty">填写组别、风格和 ABV 后显示核对单。</p>
      </div>

    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchPortalCompetitionDetail, simulatePortalEntryPayment, submitPortalEntry } from '@/api/portal'

const route = useRoute()
const entryFormRef = ref(null)
const selectedDetail = ref(null)
const submittedEntry = ref(null)
const submittingEntry = ref(false)
const simulatingPayment = ref(false)
const queryCompetitionId = Number(route.query.competitionId || 0)
const currencyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  maximumFractionDigits: 0,
})

const form = reactive({
  name: '',
  categoryId: null,
  style: '',
  abv: null,
  extraFields: {},
  confirmed: false,
})

const selectedCompetition = computed(() => selectedDetail.value || null)
const configuredFields = computed(() => normalizeEntryFields(selectedCompetition.value?.entryFields || []))
const selectedCategoryName = computed(() => selectedCompetition.value?.categories?.find((item) => item.id === form.categoryId)?.name || '')
const previewReady = computed(() => Boolean(selectedCategoryName.value && form.style && form.abv !== null && form.abv !== undefined))
const paymentAmount = computed(() => submittedEntry.value?.payment?.amount ?? submittedEntry.value?.entryFee ?? selectedCompetition.value?.entryFee ?? 0)
const paymentPaid = computed(() => submittedEntry.value?.paymentStatus === 'PAID' || submittedEntry.value?.canDownloadLabel)
const entryFeeLabel = computed(() => formatCurrency(selectedCompetition.value?.entryFee))
const formRules = computed(() => {
  const rules = {
    name: [{ required: true, message: '请填写酒款名称', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择投递组别', trigger: 'change' }],
    style: [{ required: true, message: '请选择基础风格', trigger: 'change' }],
    abv: [{ required: true, message: '请填写 ABV', trigger: 'change' }],
    confirmed: [
      {
        validator: (_rule, value, callback) => {
          if (value) {
            callback()
            return
          }
          callback(new Error('请确认评审可见内容不包含身份信息'))
        },
        trigger: 'change',
      },
    ],
  }

  configuredFields.value.forEach((field) => {
    if (!field.required) {
      return
    }
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

onMounted(async () => {
  if (!queryCompetitionId) {
    selectedDetail.value = null
    syncCompetitionDefaults()
    return
  }
  selectedDetail.value = await fetchPortalCompetitionDetail(queryCompetitionId)
  syncCompetitionDefaults()
})

function syncCompetitionDefaults() {
  if (!selectedCompetition.value) {
    form.categoryId = null
    form.style = ''
    form.abv = null
    form.confirmed = false
    form.extraFields = {}
    return
  }
  form.categoryId = null
  form.style = ''
  form.abv = null
  form.confirmed = false
  form.extraFields = Object.fromEntries(configuredFields.value.map((field) => [field.fieldKey, getEmptyFieldValue(field)]))
}

async function submitEntry() {
  if (submittedEntry.value || submittingEntry.value) return
  const valid = await entryFormRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先补全报名表中的必要信息')
    return
  }
  if (!selectedCompetition.value) {
    ElMessage.warning('请先从赛事详情进入报名')
    return
  }
  submittingEntry.value = true
  try {
    const entry = await submitPortalEntry(selectedCompetition.value.id, {
      name: form.name,
      categoryId: form.categoryId,
      style: form.style,
      abv: form.abv,
      extraFields: form.extraFields,
    })
    submittedEntry.value = entry
    ElMessage.success('报名已提交，请完成支付')
  } catch (error) {
    ElMessage.warning(error?.message || '报名提交失败，请稍后重试')
  } finally {
    submittingEntry.value = false
  }
}

async function paySubmittedEntry() {
  if (!submittedEntry.value || paymentPaid.value) return

  simulatingPayment.value = true
  try {
    submittedEntry.value = await simulatePortalEntryPayment(submittedEntry.value.id)
    ElMessage.success('支付成功，报名已完成')
  } catch (error) {
    ElMessage.warning(error?.message || '支付失败，请稍后重试')
  } finally {
    simulatingPayment.value = false
  }
}

function resetForNextEntry() {
  submittedEntry.value = null
  syncCompetitionDefaults()
  entryFormRef.value?.clearValidate()
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

function getEmptyFieldValue(field) {
  if (field.fieldType === 'multi_select') {
    return []
  }
  if (field.fieldType === 'number') {
    return null
  }
  return ''
}

function hasFieldValue(value) {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function formatFieldValue(value) {
  return Array.isArray(value) ? value.join('、') : value
}

function formatCurrency(value) {
  if (value === null || value === undefined || value === '') return '¥0'
  return currencyFormatter.format(Number(value))
}

function styleLabel(item) {
  return item.styleCode ? `${item.styleCode} ${item.name}` : item.name
}
</script>

<style scoped>
.submit-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.form-card {
  padding: 22px 24px;
}

.form-header p {
  margin: 8px 0 16px;
  color: #746a5f;
  line-height: 1.65;
}

.competition-banner,
.competition-empty {
  margin-bottom: 14px;
  padding: 12px 14px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.competition-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.competition-banner span {
  display: inline;
  color: #8b5c19;
  font-size: 13px;
  font-weight: 900;
}

.competition-banner strong,
.competition-empty strong {
  display: inline;
  margin-top: 0;
  color: #2b1d10;
  font-size: 18px;
  line-height: 1.25;
}

.competition-banner a {
  color: #8b5c19;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.form-section {
  padding: 15px 0;
  border-top: 1px solid rgba(87, 58, 26, 0.1);
}

.form-section:first-of-type {
  padding-top: 0;
  border-top: 0;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-title-row h3 {
  margin: 0;
  color: #2b1d10;
  font-size: 16px;
}

.section-title-row span {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 800;
}

.entry-form :deep(.el-input__wrapper),
.entry-form :deep(.el-textarea__inner),
.entry-form :deep(.el-select__wrapper),
.entry-form :deep(.el-input-number) {
  background: #fffdf7;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(87, 58, 26, 0.14) inset;
}

.entry-form :deep(.el-input-number) {
  width: 100%;
}

.entry-form :deep(.el-form-item__label) {
  color: #514338;
  font-weight: 800;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 18px;
}

.full-field {
  max-width: none;
}

.number-field {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  gap: 8px;
  align-items: center;
  max-width: 360px;
}

.number-field span {
  color: #746a5f;
  font-weight: 800;
}

.dynamic-field-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 18px;
}

.dynamic-field:has(textarea) {
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
  border-radius: 999px;
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
}

.field-label em {
  color: #8f5100;
  background: #ffe3a6;
}

.field-help {
  margin: 7px 0 0;
  color: #746a5f;
  font-size: 12px;
  line-height: 1.5;
}

.confirm-item {
  margin-bottom: 0;
}

.confirm-box {
  width: 100%;
  padding: 12px 14px;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.2);
  border-radius: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 14px;
}

.inline-payment {
  margin-top: 18px;
  padding: 18px;
  background: linear-gradient(180deg, #fffaf0, #fff5df);
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 8px;
}

.payment-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.payment-head span {
  display: block;
  color: #8b5c19;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.payment-head h3 {
  margin: 8px 0 6px;
  color: #2b1d10;
  font-size: 22px;
  line-height: 1.2;
}

.payment-head p {
  margin: 0;
  color: #746a5f;
  line-height: 1.6;
}

.payment-head strong {
  color: #8b5c19;
  font-size: 24px;
  white-space: nowrap;
}

.payment-facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0 0;
}

.payment-facts div {
  padding: 12px;
  background: #fffdf7;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.payment-facts dt,
.payment-facts dd {
  margin: 0;
}

.payment-facts dt {
  color: #907c66;
  font-size: 12px;
  font-weight: 800;
}

.payment-facts dd {
  margin-top: 6px;
  color: #2b1d10;
  font-weight: 800;
  line-height: 1.5;
}

.payment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 16px;
}

.primary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 14px;
  color: #fffaf0;
  background: #b9781f;
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.secondary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 14px;
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.preview-card {
  position: sticky;
  top: 116px;
  display: grid;
  gap: 16px;
}

.preview-label {
  min-height: 260px;
  padding: 22px;
  color: #2b1d10;
  background:
    linear-gradient(180deg, #fffaf0, #eac06c),
    repeating-linear-gradient(135deg, rgba(45, 29, 14, 0.07) 0 1px, transparent 1px 10px);
  border: 1px solid rgba(87, 58, 26, 0.18);
  border-radius: 8px;
  box-shadow: 0 24px 48px rgba(83, 51, 17, 0.14);
}

.preview-label > span {
  display: block;
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.preview-label h3 {
  margin: 18px 0 0;
  color: #2b1d10;
  font-size: 28px;
  line-height: 1.15;
}

.preview-label p,
.preview-empty {
  color: #675b4a;
  line-height: 1.7;
}

.preview-empty {
  margin-top: 28px;
}

.receipt-line {
  height: 1px;
  margin: 22px 0 18px;
  background: repeating-linear-gradient(90deg, rgba(43, 29, 16, 0.45) 0 8px, transparent 8px 14px);
}

.foam-line {
  height: 9px;
  margin: 24px 0 14px;
  background:
    radial-gradient(circle, #fffaf0 0 45%, transparent 48%) 0 0 / 16px 9px repeat-x;
}

.preview-label dl {
  display: grid;
  gap: 10px;
  margin: 0;
}

.preview-label dt {
  color: #806f5b;
  font-size: 13px;
}

.preview-label dd {
  margin: 4px 0 0;
  font-weight: 800;
  line-height: 1.5;
}

.receipt-status {
  margin: 0;
  color: #6b4710;
  font-weight: 800;
}

@media (max-width: 1080px) {
  .submit-page {
    grid-template-columns: 1fr;
  }

  .competition-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .preview-card {
    position: static;
  }
}

@media (max-width: 680px) {
  .form-grid,
  .dynamic-field-list,
  .form-actions,
  .payment-facts {
    grid-template-columns: 1fr;
  }

  .dynamic-field:has(textarea) {
    grid-column: auto;
  }

  .form-actions {
    display: grid;
  }

  .payment-head {
    display: grid;
  }

  .number-field {
    max-width: none;
  }
}
</style>
