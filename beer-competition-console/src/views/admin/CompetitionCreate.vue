<template>
  <div class="competition-create">
    <AdminPageHeader title="新建比赛">
      <template #leading>
        <button class="breadcrumb-link" type="button" @click="leaveCreatePage">
          <Back />
          比赛管理
        </button>
      </template>
      <template #actions>
        <div class="head-actions">
          <button class="tool-button" type="button" @click="leaveCreatePage">取消</button>
          <button class="tool-button primary" type="button" @click="submitDraft">
            <CircleCheck />
            保存草稿
          </button>
        </div>
      </template>
    </AdminPageHeader>

    <nav class="anchor-bar" aria-label="新建比赛分区导航">
      <button :class="{ active: activeSection === 'base-info', issue: sectionIssueMap['base-info'] }" type="button" @click="scrollToSection('base-info')">基础信息</button>
      <button :class="{ active: activeSection === 'logistics-info', issue: sectionIssueMap['logistics-info'] }" type="button" @click="scrollToSection('logistics-info')">送样信息</button>
      <button :class="{ active: activeSection === 'entry-config', issue: sectionIssueMap['entry-config'] }" type="button" @click="scrollToSection('entry-config')">报名表</button>
      <button v-if="isTenantOrganizer" :class="{ active: activeSection === 'collection-config' }" type="button" @click="scrollToSection('collection-config')">报名收款</button>
      <button :class="{ active: activeSection === 'score-config', issue: sectionIssueMap['score-config'] }" type="button" @click="scrollToSection('score-config')">评分表</button>
      <button :class="{ active: activeSection === 'review-draft' }" type="button" @click="scrollToSection('review-draft')">确认</button>
    </nav>

    <main class="create-panel">
        <section id="base-info" class="form-section">
          <header class="section-head">
            <h2>基础信息</h2>
          </header>
          <div class="base-info-groups">
            <label class="wide-field">
              <span>比赛名称</span>
              <input v-model.trim="draft.name" placeholder="请输入赛事名称" />
            </label>

            <section class="form-subgroup">
              <div class="subgroup-heading competition-type-heading">
                <h3>赛制选择</h3>
                <button
                  class="hint-marker info-hint"
                  type="button"
                  aria-label="赛制选择说明：风格对齐会在第一轮评选后发布结果，不产生奖项"
                  data-tooltip="风格对齐会在第一轮评选后发布结果，不产生奖项"
                >
                  <InfoFilled aria-hidden="true" />
                </button>
              </div>
              <div class="competition-type-switch">
                <button
                  v-for="option in competitionTypeOptions"
                  :key="option.value"
                  type="button"
                  :class="{ active: draft.competitionType === option.value }"
                  @click="draft.competitionType = option.value"
                >
                  <strong>{{ option.label }}</strong>
                </button>
              </div>
              <div class="form-grid two">
                <label>
                  <span>比赛日期</span>
                  <input v-model="draft.date" type="date" />
                </label>
              </div>
            </section>

            <section class="form-subgroup">
              <h3>报名时间</h3>
              <div class="form-grid two">
                <label>
                  <span>报名开始时间</span>
                  <input v-model="draft.registrationStart" type="datetime-local" />
                </label>
                <label>
                  <span>报名截止时间</span>
                  <input v-model="draft.registrationDeadline" type="datetime-local" />
                </label>
              </div>
            </section>

            <section class="form-subgroup">
              <h3>费用规则</h3>
              <div class="form-grid two">
                <label>
                  <span>普通报名费</span>
                  <div class="fee-field">
                    <input v-model.number="draft.entryFee" min="0" type="number" />
                    <span>元 / 款</span>
                  </div>
                </label>
                <label>
                  <span>早鸟价</span>
                  <div class="fee-field">
                    <input v-model.number="draft.earlyBirdFee" min="0" type="number" />
                    <span>元 / 款</span>
                  </div>
                </label>
                <label>
                  <span>早鸟截止时间</span>
                  <input v-model="draft.earlyBirdDeadline" type="datetime-local" />
                </label>
              </div>
              <label class="tier-toggle" :class="{ enabled: draft.tierPricingEnabled }">
                <span>启用阶梯报名费</span>
                <el-switch v-model="draft.tierPricingEnabled" aria-label="启用阶梯报名费" />
              </label>
              <div v-if="draft.tierPricingEnabled" class="tier-editor">
                <div class="tier-editor-head">
                  <span aria-hidden="true">起始款数</span>
                  <span aria-hidden="true">折扣率</span>
                  <span aria-hidden="true">示例单价</span>
                  <button type="button" class="text-button tier-add-button" @click="addFeeTier">添加阶梯</button>
                </div>
                <div v-for="(tier, index) in draft.feeTiers" :key="tier.id" class="tier-editor-row">
                  <div class="fee-field">
                    <input v-model.number="tier.startQuantity" :aria-label="`第 ${index + 1} 档起始款数`" min="2" step="1" type="number" />
                    <span>款起</span>
                  </div>
                  <div class="fee-field">
                    <input v-model.number="tier.discountRatePercent" :aria-label="`第 ${index + 1} 档折扣率`" min="1" max="100" step="0.01" type="number" />
                    <span>%</span>
                  </div>
                  <span class="tier-example-price">{{ formatTierAmount(tier.discountRatePercent) }}<small>元 / 款</small></span>
                  <button class="icon-button tier-remove-button" title="删除阶梯" :aria-label="`删除第 ${index + 1} 档阶梯`" type="button" @click="draft.feeTiers.splice(index, 1)"><Delete /></button>
                </div>
              </div>
            </section>

            <section class="form-subgroup">
              <h3>赛事展示</h3>
              <label class="wide-field">
                <span>赛事简介</span>
                <textarea v-model.trim="draft.description" rows="3" maxlength="1000" placeholder="面向厂牌说明本场赛事定位、适合提交的酒款或报名重点" />
              </label>
              <label class="wide-field">
                <span>参赛细则链接</span>
                <input v-model.trim="draft.rulesUrl" placeholder="例如 https://mp.weixin.qq.com/s/..." />
              </label>
            </section>
          </div>
        </section>

        <section id="logistics-info" class="form-section">
          <header class="section-head">
            <h2>送样信息</h2>
          </header>
          <div class="logistics-layout">
            <section class="entry-section logistics-section">
              <div class="form-grid two">
                <label>
                  <span>送样方式</span>
                  <el-select
                    v-model="draft.deliveryMethod"
                    class="form-select"
                    popper-class="competition-create-select-popper"
                    aria-label="送样方式"
                  >
                    <el-option
                      v-for="option in deliveryMethodOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
                <label>
                  <span>地址展示</span>
                  <el-select
                    v-model="draft.logisticsVisibility"
                    class="form-select"
                    popper-class="competition-create-select-popper"
                    aria-label="地址展示"
                  >
                    <el-option
                      v-for="option in logisticsVisibilityOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </label>
                <label>
                  <span>送达开始</span>
                  <input v-model="draft.sampleArrivalStart" type="datetime-local" />
                </label>
                <label>
                  <span>送达截止</span>
                  <input v-model="draft.sampleArrivalDeadline" type="datetime-local" />
                </label>
                <label>
                  <span>收件人</span>
                  <input v-model.trim="draft.deliveryRecipient" placeholder="例如 赛事收样组" />
                </label>
                <label>
                  <span>收件联系电话</span>
                  <input v-model.trim="draft.deliveryPhone" placeholder="用于快递面单和异常联系" />
                </label>
                <label class="wide-field">
                  <span>收件地址</span>
                  <input v-model.trim="draft.deliveryAddress" placeholder="省市区、详细地址、收样点名称" />
                </label>
                <label>
                  <span>样品数量要求</span>
                  <input v-model.trim="draft.sampleQuantityNote" placeholder="例如 每款 6 瓶，单瓶不低于 330ml" />
                </label>
                <label class="wide-field">
                  <span>粘贴标签说明</span>
                  <textarea v-model.trim="draft.deliveryNote" rows="3" placeholder="例如：用 A4 纸打印标签并裁剪，贴在每瓶酒款上，建议覆膜防水" />
                </label>
              </div>
            </section>
          </div>
        </section>

        <section id="entry-config" class="form-section">
          <header class="section-head">
            <h2>报名表配置</h2>
          </header>
          <div class="built-in-strip">
            <h3>内置报名信息</h3>
            <div class="built-in-fields">
              <span>酒名</span>
              <span>投递组别</span>
              <span>基础风格</span>
              <span>ABV</span>
            </div>
          </div>
          <section class="entry-section library-section">
            <div class="library-row">
              <h3>基础风格库</h3>
              <el-select
                v-model="draft.styleLibraryVersion"
                class="library-select"
                popper-class="competition-create-library-popper"
                aria-label="风格库版本"
              >
                <el-option
                  v-for="library in styleLibraryOptions"
                  :key="library.value"
                  :label="library.label"
                  :value="library.value"
                />
              </el-select>
              <div class="library-tags">
                <span v-for="tag in selectedStyleLibrary.tags.slice(0, 2)" :key="tag">{{ tag }}</span>
              </div>
            </div>
          </section>

          <section class="entry-section category-section">
            <div class="card-title">
              <div class="title-with-count">
                <h3>投递组别</h3>
                <span class="count-pill">{{ categoryCount }} 个</span>
              </div>
              <div class="title-actions">
                <label class="style-sync-switch">
                  <span>同步风格库</span>
                  <el-switch
                    v-model="syncCategoriesWithStyleLibrary"
                    aria-label="同步风格库投递组别"
                    @change="toggleCategoryStyleSync"
                  />
                </label>
                <button class="tool-button" type="button" @click="addCategory">
                  <Plus />
                  添加组别
                </button>
              </div>
            </div>
            <div class="category-list">
              <div v-for="(category, index) in draft.categories" :key="category.id" class="category-row">
                <input v-model.trim="category.name" :aria-label="`投递组别 ${index + 1}`" :disabled="syncCategoriesWithStyleLibrary" placeholder="例如 浅色拉格" />
                <button class="icon-button" :disabled="syncCategoriesWithStyleLibrary" title="删除投递组别" type="button" @click="removeCategory(index)"><Delete /></button>
              </div>
            </div>
          </section>

          <section class="entry-section field-section">
            <div class="card-title">
              <div class="title-with-count">
                <h3>补充字段</h3>
              </div>
              <button class="tool-button" type="button" @click="addEntryField">
                <Plus />
                添加字段
              </button>
            </div>
            <div class="field-list">
              <div v-for="(field, index) in draft.entryFields" :key="field.key" class="field-card">
                <div class="field-main-row">
                  <label>
                    <span>字段名称</span>
                    <input v-model.trim="field.label" placeholder="例如 增味原料或特殊工艺" />
                  </label>
                  <label>
                    <span>类型</span>
                    <el-select
                      v-model="field.type"
                      class="form-select"
                      popper-class="competition-create-select-popper"
                      :aria-label="`补充字段 ${index + 1} 类型`"
                    >
                      <el-option
                        v-for="option in fieldTypeOptions"
                        :key="option.value"
                        :label="option.label"
                        :value="option.value"
                      />
                    </el-select>
                  </label>
                  <button class="icon-button" title="删除补充字段" type="button" @click="removeItem(draft.entryFields, index)"><Delete /></button>
                </div>
                <div class="field-extra-row">
                  <label>
                    <span>提示文案</span>
                    <input v-model.trim="field.helpText" placeholder="给厂牌看的填写说明" />
                  </label>
                  <label class="switch-line"><input v-model="field.required" type="checkbox" /> 必填</label>
                  <label
                    class="switch-line visibility-switch"
                    data-tooltip="评审可见字段会进入匿名扫码页，只勾选评酒时需要判断的信息"
                    :class="{ warning: field.visibleToJudges }"
                  >
                    <input v-model="field.visibleToJudges" type="checkbox" />
                    评审可见
                  </label>
                </div>
                <div v-if="['select', 'multi_select'].includes(field.type)" class="field-options-editor">
                  <div class="option-editor-head">
                    <span>候选项</span>
                    <button type="button" @click="addFieldOption(field)">添加选项</button>
                  </div>
                  <div v-for="(_, optionIndex) in field.options" :key="`${field.key}-option-${optionIndex}`" class="option-editor-row">
                    <input v-model.trim="field.options[optionIndex]" :placeholder="`选项 ${optionIndex + 1}`" />
                    <button type="button" :disabled="optionIndex === 0" @click="moveFieldOption(field, optionIndex, -1)">上移</button>
                    <button type="button" :disabled="optionIndex === field.options.length - 1" @click="moveFieldOption(field, optionIndex, 1)">下移</button>
                    <button class="danger-text" type="button" @click="removeItem(field.options, optionIndex)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </section>

        <section v-if="isTenantOrganizer" id="collection-config" class="form-section">
          <header class="section-head"><h2>报名收款</h2></header>
          <div class="collection-config-grid">
            <div class="collection-method-field wide-field">
              <span class="field-label">支持的收款方式</span>
              <div class="collection-method-segmented" role="radiogroup" aria-label="支持的收款方式">
                <button
                  v-for="option in collectionMethodOptions"
                  :key="option.value"
                  class="collection-method-option"
                  :class="{ active: collectionDraft.methodMode === option.value }"
                  type="button"
                  role="radio"
                  :aria-checked="collectionDraft.methodMode === option.value"
                  @click="collectionDraft.methodMode = option.value"
                >
                  {{ option.label }}
                </button>
              </div>
            </div>
            <div v-if="wechatCollectionEnabled" class="collection-upload wide-field">
              <span class="field-label">微信收款码 <b>*</b></span>
              <input ref="collectionQrInput" class="collection-file-input" type="file" accept="image/jpeg,image/png,image/webp" @change="selectCollectionQr" />
              <button v-if="!collectionDraft.qrFile" class="upload-dropzone" type="button" @click="collectionQrInput?.click()"><span class="upload-icon">↑</span><strong>上传微信收款码</strong><small>支持 JPG、PNG、WEBP，请上传清晰完整的收款码</small></button>
              <div v-else class="qr-preview-card"><img :src="collectionDraft.qrPreviewUrl" alt="微信收款码预览" /><div><strong>{{ collectionDraft.qrFile.name }}</strong><small>厂商付款时将看到此收款码</small><button type="button" @click="collectionQrInput?.click()">更换图片</button></div></div>
            </div>
            <template v-if="bankCollectionEnabled">
              <label><span>收款户名 <b>*</b></span><input v-model.trim="collectionDraft.bankAccountName" placeholder="请输入银行卡开户名称" /></label>
              <label><span>银行账号 <b>*</b></span><input v-model.trim="collectionDraft.bankAccountNo" placeholder="请输入收款银行账号" /></label>
              <label class="wide-field"><span>开户银行 <b>*</b></span><input v-model.trim="collectionDraft.bankName" placeholder="请输入开户银行及支行" /></label>
            </template>
            <label class="wide-field"><span>付款备注要求</span><input v-model.trim="collectionDraft.collectionNote" maxlength="255" placeholder="例如：付款时请备注厂牌名称" /></label>
            <label class="wide-field"><span>付款咨询联系</span><input v-model.trim="collectionDraft.paymentContact" maxlength="128" placeholder="微信号或手机号，付款的厂商遇到问题时联系" /></label>
          </div>
          <div v-if="beerCoinOverview" class="beer-coin-create-hint" :class="{ insufficient: beerCoinInsufficient }">
            <div>
              <span>啤酒币</span>
              <strong>开放报名最低扣除 1 枚，每款有效酒款 1 枚，当前余额 {{ formatInteger(beerCoinWallet?.availableQuantity) }} 枚</strong>
            </div>
            <button v-if="beerCoinInsufficient" class="text-button" type="button" @click="router.push('/admin/beer-coins')">购买啤酒币</button>
          </div>
        </section>

        <section id="score-config" class="form-section">
          <header class="section-head">
            <h2>评分表配置</h2>
          </header>

          <div class="score-stack">
            <article v-for="config in draft.scoreConfigs" :key="config.role" class="config-card score-card">
              <div class="card-title">
                <div class="score-title">
                  <h3>{{ roleLabels[config.role] }}</h3>
                  <button
                    class="hint-marker info-hint score-info-hint"
                    type="button"
                    :aria-label="`${roleLabels[config.role]}说明：${scoreDescriptions[config.role]}`"
                    :data-tooltip="scoreDescriptions[config.role]"
                  >
                    <InfoFilled aria-hidden="true" />
                  </button>
                </div>
                <span :class="['score-total', { ok: getScoreTotal(config) === 50 }]">{{ getScoreTotal(config) }} / 50</span>
              </div>
              <div class="dimension-list">
                <div class="dimension-row dimension-head">
                  <span>维度</span>
                  <span>满分</span>
                  <span>备注提示</span>
                  <span></span>
                </div>
                <div v-for="(dimension, index) in config.dimensions" :key="dimension.key || dimension.label" class="dimension-row">
                  <input v-model.trim="dimension.label" :disabled="config.locked" placeholder="维度名称" />
                  <input v-model.number="dimension.maxScore" :disabled="config.locked" min="1" step="1" type="number" />
                  <input v-model.trim="dimension.notePrompt" placeholder="例如 描述香气表现" />
                  <button
                    class="icon-button"
                    type="button"
                    :disabled="config.locked || config.dimensions.length <= 2"
                    @click="removeDimension(config, index)"
                  >
                    <Delete />
                  </button>
                </div>
                <div class="score-foot">
                  <label>
                    <span>备注合计字数下限</span>
                    <input v-model.number="config.minCommentLength" min="0" type="number" />
                  </label>
                  <button v-if="config.role === 'CROSS'" class="tool-button" type="button" :disabled="config.dimensions.length >= 3" @click="addCrossDimension(config)">
                    <Plus />
                    添加维度
                  </button>
                  <span v-else class="status-pill">{{ config.locked ? '维度固定' : '共识分' }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section id="review-draft" class="form-section review-section">
          <header class="section-head review-head">
            <h2>确认草稿</h2>
          </header>
          <div :class="['review-banner', { blocked: reviewBlockingItems.length }]">
            <div class="review-banner-copy">
              <span class="review-banner-icon">
                <Warning v-if="reviewBlockingItems.length" />
                <CircleCheck v-else />
              </span>
              <div>
                <h3>{{ reviewBlockingItems.length ? `还有 ${reviewBlockingItems.length} 项需要处理` : '可以保存草稿' }}</h3>
                <p>{{ reviewBlockingItems.length ? reviewBlockingText : reviewReadyText }}</p>
              </div>
            </div>
            <div class="section-actions inline-actions">
              <button class="tool-button primary" type="button" @click="submitDraft">
                <CircleCheck />
                保存草稿，进入工作台
              </button>
            </div>
          </div>

          <div class="review-layout">
            <section class="review-block">
              <h3>保存前检查</h3>
              <div class="review-check-list">
                <div v-for="item in reviewItems" :key="item.key" :class="['review-check-item', item.status]">
                  <CircleCheck v-if="item.status === 'done'" />
                  <Warning v-else />
                  <div>
                    <strong>{{ item.label }}</strong>
                    <span>{{ item.detail }}</span>
                  </div>
                  <button v-if="item.status !== 'done'" class="text-button" type="button" @click="scrollToSection(item.target)">去修改</button>
                </div>
              </div>
            </section>

            <section class="review-block summary-block">
              <h3>关键摘要</h3>
              <dl class="summary-list">
                <div>
                  <dt>比赛名称</dt>
                  <dd>{{ draft.name || '-' }}</dd>
                </div>
                <div>
                  <dt>比赛类型</dt>
                  <dd>{{ competitionTypeLabel }}</dd>
                </div>
                <div>
                  <dt>比赛日期</dt>
                  <dd>{{ draft.date || '-' }}</dd>
                </div>
                <div>
                  <dt>报名截止</dt>
                  <dd>{{ formatDateTime(draft.registrationDeadline) }}</dd>
                </div>
                <div>
                  <dt>报名费</dt>
                  <dd>{{ draft.entryFee === '' || draft.entryFee === null ? '-' : `${draft.entryFee} 元 / 款` }}</dd>
                </div>
                <div>
                  <dt>早鸟价</dt>
                  <dd>{{ draft.earlyBirdFee ? `${draft.earlyBirdFee} 元 / 款` : '-' }}</dd>
                </div>
                <div>
                  <dt>早鸟截止</dt>
                  <dd>{{ formatDateTime(draft.earlyBirdDeadline) }}</dd>
                </div>
                <div>
                  <dt>赛事简介</dt>
                  <dd>{{ draft.description || '-' }}</dd>
                </div>
                <div>
                  <dt>参赛细则</dt>
                  <dd>{{ draft.rulesUrl || '-' }}</dd>
                </div>
                <div>
                  <dt>送样方式</dt>
                  <dd>{{ deliveryMethodText(draft.deliveryMethod) }}</dd>
                </div>
                <div>
                  <dt>送样截止</dt>
                  <dd>{{ formatDateTime(draft.sampleArrivalDeadline) }}</dd>
                </div>
                <div>
                  <dt>投递组别</dt>
                  <dd>{{ categorySummary }}</dd>
                </div>
                <div>
                  <dt>评审可见字段</dt>
                  <dd>{{ judgeVisibleFieldSummary }}</dd>
                </div>
              </dl>
            </section>
          </div>
        </section>

      </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminMe } from '@/api/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, CircleCheck, Delete, InfoFilled, Plus, Warning } from '@element-plus/icons-vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import {
  defaultScoreConfigs,
  formatDateTime,
  getScoreTotal,
  roleLabels,
} from './competitionStore'
import {
  createCompetition,
  fetchEnabledStyleLibraries,
  uploadCompetitionCollectionQr,
  updateCompetitionCollection,
} from '@/api/admin'
import { fetchBeerCoinOverview } from '@/api/beerCoin'
import { defaultStyleLibraryValue, fallbackStyleLibraries, formatStyleItemName, getStyleLibrary, normalizeStyleLibraries } from './styleLibraries'

const router = useRouter()
const activeSection = ref('base-info')
const collectionQrInput = ref(null)
const isTenantOrganizer = ref(false)
const beerCoinOverview = ref(null)
const beerCoinWalletLoading = ref(false)

const deliveryMethodOptions = [
  { label: '快递寄送 / 现场送样', value: 'BOTH' },
  { label: '仅快递寄送', value: 'EXPRESS' },
  { label: '仅现场送样', value: 'ONSITE' },
]

const logisticsVisibilityOptions = [
  { label: '支付成功后显示完整地址', value: 'PAYMENT_CONFIRMED' },
  { label: '登录后显示完整地址', value: 'LOGIN_REQUIRED' },
  { label: '公开显示完整地址', value: 'PUBLIC' },
]

const competitionTypeOptions = [
  { label: '正式评奖比赛', value: 'AWARD' },
  { label: '风格对齐会', value: 'FEEDBACK_ONLY' },
]

const fieldTypeOptions = [
  { label: '短文本', value: 'text' },
  { label: '长文本', value: 'textarea' },
  { label: '数字', value: 'number' },
  { label: '单选', value: 'select' },
  { label: '多选', value: 'multi_select' },
]

const collectionMethodOptions = [
  { label: '微信收款', value: 'WECHAT' },
  { label: '银行转账', value: 'BANK' },
  { label: '微信支付+银行转账', value: 'BOTH' },
]

function pad2(value) {
  return String(value).padStart(2, '0')
}

function formatDateOnly(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

function formatDateTimeLocal(date) {
  return `${formatDateOnly(date)}T${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

function createDefaultSchedule() {
  const now = new Date()
  const registrationStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 9, 0)
  const registrationDeadline = new Date(registrationStart)
  registrationDeadline.setDate(registrationDeadline.getDate() + 30)
  registrationDeadline.setHours(18, 0, 0, 0)
  const competitionDate = new Date(registrationDeadline)
  competitionDate.setDate(competitionDate.getDate() + 30)
  const sampleArrivalStart = new Date(registrationDeadline)
  sampleArrivalStart.setDate(sampleArrivalStart.getDate() + 5)
  sampleArrivalStart.setHours(9, 0, 0, 0)
  const sampleArrivalDeadline = new Date(competitionDate)
  sampleArrivalDeadline.setDate(sampleArrivalDeadline.getDate() - 5)
  sampleArrivalDeadline.setHours(18, 0, 0, 0)
  return {
    competitionDate: formatDateOnly(competitionDate),
    registrationStart: formatDateTimeLocal(registrationStart),
    registrationDeadline: formatDateTimeLocal(registrationDeadline),
    sampleArrivalStart: formatDateTimeLocal(sampleArrivalStart),
    sampleArrivalDeadline: formatDateTimeLocal(sampleArrivalDeadline),
  }
}

const defaultSchedule = createDefaultSchedule()

const draft = reactive({
  name: '',
  competitionType: 'AWARD',
  date: defaultSchedule.competitionDate,
  registrationStart: defaultSchedule.registrationStart,
  registrationDeadline: defaultSchedule.registrationDeadline,
  entryFee: '',
  earlyBirdFee: '',
  earlyBirdDeadline: '',
  tierPricingEnabled: false,
  feeTiers: [{ id: 'tier-initial', startQuantity: 3, discountRatePercent: 80 }],
  description: '',
  rulesUrl: '',
  deliveryMethod: 'EXPRESS',
  sampleArrivalStart: defaultSchedule.sampleArrivalStart,
  sampleArrivalDeadline: defaultSchedule.sampleArrivalDeadline,
  sampleQuantityNote: '',
  deliveryRecipient: '',
  deliveryPhone: '',
  deliveryAddress: '',
  deliveryNote: '',
  logisticsVisibility: 'PAYMENT_CONFIRMED',
  categories: [{ id: 'cat-1', name: '' }],
  styleLibraryVersion: defaultStyleLibraryValue,
  entryFields: [],
  scoreConfigs: createScoreConfigs(),
})
const collectionDraft = reactive({ methodMode: 'BOTH', qrFile: null, qrPreviewUrl: '', qrAssetId: null, bankAccountName: '', bankAccountNo: '', bankName: '', collectionNote: '', paymentContact: '' })
const initialDraftSnapshot = JSON.stringify(toDraftSnapshot(draft))
const initialCollectionSnapshot = JSON.stringify(toCollectionSnapshot(collectionDraft))

const scoreDescriptions = {
  CROSS: '每场比赛可配置 2-3 个维度，总分 50',
  PROFESSIONAL: '固定 BJCP 口径，允许调整备注提示',
  CAPTAIN: '桌长填写独立共识分和综合评语',
}

const categoryCount = computed(() => draft.categories.filter((category) => category.name).length)
const styleLibraryOptions = ref(normalizeStyleLibraries(fallbackStyleLibraries))
const syncCategoriesWithStyleLibrary = ref(false)
let manualCategoriesBeforeStyleSync = []
const selectedStyleLibrary = computed(() => getStyleLibrary(draft.styleLibraryVersion, styleLibraryOptions.value))
const reviewItems = computed(() => buildReviewItems(draft))
const reviewBlockingItems = computed(() => reviewItems.value.filter((item) => item.status !== 'done'))
const reviewBlockingText = computed(() => `请先处理：${reviewBlockingItems.value.map((item) => item.label).join('、')}`)
const competitionTypeLabel = computed(() => competitionTypeOptions.find((item) => item.value === draft.competitionType)?.label || '正式评奖比赛')
const reviewReadyText = computed(() => (
  draft.competitionType === 'FEEDBACK_ONLY'
    ? '保存后进入工作台配置评审桌、首轮评审和诊断发布'
    : '保存后进入工作台继续配置评审桌、轮次和奖项'
))
const sectionIssueMap = computed(() => reviewBlockingItems.value.reduce((map, item) => ({ ...map, [item.target]: true }), {}))
const categorySummary = computed(() => summarizeList(getCategoryNames(draft), '未配置'))
const judgeVisibleFieldSummary = computed(() => summarizeList(getJudgeVisibleFields(draft), '无'))
const beerCoinWallet = computed(() => beerCoinOverview.value?.wallet || null)
const beerCoinInsufficient = computed(() => Number(beerCoinWallet.value?.availableQuantity || 0) < 1)
const wechatCollectionEnabled = computed(() => ['WECHAT', 'BOTH'].includes(collectionDraft.methodMode))
const bankCollectionEnabled = computed(() => ['BANK', 'BOTH'].includes(collectionDraft.methodMode))
const collectionEnabledMethods = computed(() => [
  ...(wechatCollectionEnabled.value ? ['WECHAT_QR'] : []),
  ...(bankCollectionEnabled.value ? ['BANK_TRANSFER'] : []),
])
const isDraftDirty = computed(() => JSON.stringify(toDraftSnapshot(draft)) !== initialDraftSnapshot
  || (isTenantOrganizer.value && JSON.stringify(toCollectionSnapshot(collectionDraft)) !== initialCollectionSnapshot))

function toCollectionSnapshot(collection) {
  return {
    methodMode: collection.methodMode,
    qrFileName: collection.qrFile?.name || '',
    qrAssetId: collection.qrAssetId,
    bankAccountName: collection.bankAccountName,
    bankAccountNo: collection.bankAccountNo,
    bankName: collection.bankName,
    collectionNote: collection.collectionNote,
    paymentContact: collection.paymentContact,
  }
}

function removeItem(list, index) {
  list.splice(index, 1)
}

function addFeeTier() {
  const last = draft.feeTiers[draft.feeTiers.length - 1]
  draft.feeTiers.push({ id: `tier-${Date.now()}`, startQuantity: (last?.startQuantity || 1) + 2, discountRatePercent: 80 })
}

function formatTierAmount(ratePercent) {
  return (Number(draft.entryFee || 0) * Number(ratePercent ?? 100) / 100).toFixed(2)
}

function selectCollectionQr(event) {
  const file = event.target.files?.[0] || null
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('收款码仅支持 JPG、PNG 或 WEBP 图片')
    event.target.value = ''
    return
  }
  if (collectionDraft.qrPreviewUrl) URL.revokeObjectURL(collectionDraft.qrPreviewUrl)
  collectionDraft.qrFile = file
  collectionDraft.qrPreviewUrl = URL.createObjectURL(file)
}

function addCategory() {
  if (syncCategoriesWithStyleLibrary.value) return
  draft.categories.push({ id: `cat-${Date.now()}`, name: '' })
}

function removeCategory(index) {
  if (syncCategoriesWithStyleLibrary.value) return
  removeItem(draft.categories, index)
}

function addEntryField() {
  draft.entryFields.push({
    key: createEntryFieldKey(),
    label: '',
    type: 'text',
    helpText: '',
    required: false,
    visibleToJudges: true,
    options: [],
  })
}

function addFieldOption(field) {
  field.options ||= []
  field.options.push('')
}

function moveFieldOption(field, index, offset) {
  const target = index + offset
  if (target < 0 || target >= field.options.length) return
  const [item] = field.options.splice(index, 1)
  field.options.splice(target, 0, item)
}

function addCrossDimension(config) {
  if (config.dimensions.length >= 3) {
    ElMessage.warning('跨界评审需配置 2-3 个维度')
    return
  }
  const next = config.dimensions.length + 1
  config.dimensions.push({
    key: `cross_${next}`,
    label: '',
    maxScore: 10,
    notePrompt: '请补充这个维度的具体反馈',
  })
}

function removeDimension(config, index) {
  config.dimensions.splice(index, 1)
}

async function submitDraft() {
  const firstIssue = reviewBlockingItems.value[0]
  if (firstIssue) {
    ElMessage.warning(firstIssue.detail)
    scrollToSection(firstIssue.target)
    return
  }
  const invalidOptionField = draft.entryFields.find((field) => ['select', 'multi_select'].includes(field.type)
    && [...new Set((field.options || []).map((item) => item.trim()).filter(Boolean))].length < 2)
  if (invalidOptionField) {
    ElMessage.warning(`“${invalidOptionField.label || '未命名字段'}”至少需要 2 个不重复候选项`)
    return
  }
  if (isTenantOrganizer.value && wechatCollectionEnabled.value && !collectionDraft.qrFile && !collectionDraft.qrAssetId) {
    ElMessage.warning('请上传微信收款码')
    scrollToSection('collection-config')
    return
  }
  if (isTenantOrganizer.value && bankCollectionEnabled.value && (!collectionDraft.bankAccountName || !collectionDraft.bankAccountNo || !collectionDraft.bankName)) {
    ElMessage.warning('请完整填写收款户名、银行账号和开户银行')
    scrollToSection('collection-config')
    return
  }
  try {
    const created = await createCompetition({
      name: draft.name,
      competitionType: draft.competitionType,
      competitionDate: draft.date,
      registrationStart: toBackendDateTime(draft.registrationStart),
      registrationDeadline: toBackendDateTime(draft.registrationDeadline),
      entryFee: Number(draft.entryFee || 0),
      earlyBirdFee: draft.earlyBirdFee === '' || draft.earlyBirdFee === null ? null : Number(draft.earlyBirdFee),
      earlyBirdDeadline: toBackendDateTime(draft.earlyBirdDeadline),
      tierPricingEnabled: draft.tierPricingEnabled,
      feeTiers: draft.tierPricingEnabled ? draft.feeTiers.map(({ startQuantity, discountRatePercent }) => ({ startQuantity: Number(startQuantity), discountRate: Number(discountRatePercent) / 100 })) : [],
      description: draft.description,
      rulesUrl: draft.rulesUrl || null,
      deliveryMethod: draft.deliveryMethod,
      sampleArrivalStart: toBackendDateTime(draft.sampleArrivalStart),
      sampleArrivalDeadline: toBackendDateTime(draft.sampleArrivalDeadline),
      sampleQuantityNote: draft.sampleQuantityNote,
      deliveryRecipient: draft.deliveryRecipient,
      deliveryPhone: draft.deliveryPhone,
      deliveryAddress: draft.deliveryAddress,
      deliveryNote: draft.deliveryNote,
      logisticsVisibility: draft.logisticsVisibility,
      styleLibraryVersion: draft.styleLibraryVersion,
      categories: draft.categories
        .filter((category) => category.name)
        .map((category, index) => ({ name: category.name, sortOrder: index })),
      entryFields: draft.entryFields
        .filter((field) => field.label)
        .map((field, index) => ({
        fieldKey: field.key,
        fieldLabel: field.label,
        fieldType: field.type,
        helpText: field.helpText,
        options: ['select', 'multi_select'].includes(field.type) ? (field.options || []).filter(Boolean) : [],
        required: Boolean(field.required),
        visibleToJudges: Boolean(field.visibleToJudges),
        sortOrder: index,
      })),
      scoreConfigs: draft.scoreConfigs.map((config) => ({
        judgeRoleType: config.role,
        minCommentLength: Number(config.minCommentLength || 0),
        dimensions: config.dimensions.map((dimension, index) => ({
          key: dimension.key || `${config.role.toLowerCase()}_${index + 1}`,
          label: dimension.label,
          maxScore: Number(dimension.maxScore || 0),
          notePrompt: dimension.notePrompt,
        })),
      })),
    })
    const competitionId = created.id
    let qrAssetId = collectionDraft.qrAssetId
    if (isTenantOrganizer.value && wechatCollectionEnabled.value && collectionDraft.qrFile) qrAssetId = (await uploadCompetitionCollectionQr(competitionId, collectionDraft.qrFile)).fileAssetId
    if (isTenantOrganizer.value) {
      await updateCompetitionCollection(competitionId, {
        enabledMethods: collectionEnabledMethods.value,
        wechatQrAssetId: wechatCollectionEnabled.value ? qrAssetId : null,
        bankAccountName: bankCollectionEnabled.value ? collectionDraft.bankAccountName : null,
        bankAccountNo: bankCollectionEnabled.value ? collectionDraft.bankAccountNo : null,
        bankName: bankCollectionEnabled.value ? collectionDraft.bankName : null,
        collectionNote: collectionDraft.collectionNote,
        paymentContact: collectionDraft.paymentContact,
      })
    }
    ElMessage.success('比赛草稿已创建，下一步进入工作台完成评审编排')
    router.push(`/admin/competitions/${competitionId}`)
  } catch {
    ElMessage.warning('草稿创建失败，请检查页面配置后重试')
  }
}

function toBackendDateTime(value) {
  if (!value) {
    return null
  }
  return value.length === 16 ? `${value}:00` : value
}

async function leaveCreatePage() {
  if (!isDraftDirty.value) {
    router.push('/admin/competitions')
    return
  }
  try {
    await ElMessageBox.confirm('当前新建比赛内容尚未保存，离开后本页填写内容会丢失', '离开新建比赛？', {
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
      type: 'warning',
    })
    router.push('/admin/competitions')
  } catch {
    // User chose to keep editing.
  }
}

function scrollToSection(id) {
  activeSection.value = id
  const panel = document.querySelector('.create-panel')
  const section = document.getElementById(id)
  if (!panel || !section) return
  panel.scrollTo({ top: Math.max(0, section.offsetTop - 4), behavior: 'smooth' })
}

function syncActiveSection() {
  const sections = ['base-info', 'logistics-info', 'entry-config', 'collection-config', 'score-config', 'review-draft']
  const current = sections
    .map((id) => ({ id, top: Math.abs(document.getElementById(id)?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY) }))
    .sort((a, b) => a.top - b.top)[0]
  if (current?.id) {
    activeSection.value = current.id
  }
}

onMounted(() => {
  loadStyleLibraries()
  getAdminMe().then((user) => {
    isTenantOrganizer.value = user?.organizerType === 'TENANT'
    if (isTenantOrganizer.value) loadBeerCoinOverview()
  }).catch(() => {})
  document.querySelector('.create-panel')?.addEventListener('scroll', syncActiveSection, { passive: true })
})

onBeforeUnmount(() => {
  if (collectionDraft.qrPreviewUrl) URL.revokeObjectURL(collectionDraft.qrPreviewUrl)
  document.querySelector('.create-panel')?.removeEventListener('scroll', syncActiveSection)
})

async function loadStyleLibraries() {
  try {
    const data = await fetchEnabledStyleLibraries()
    styleLibraryOptions.value = normalizeStyleLibraries(data)
    if (!styleLibraryOptions.value.some((library) => library.value === draft.styleLibraryVersion)) {
      draft.styleLibraryVersion = styleLibraryOptions.value[0]?.value || defaultStyleLibraryValue
    }
  } catch {
    styleLibraryOptions.value = normalizeStyleLibraries(fallbackStyleLibraries)
  }
}

async function loadBeerCoinOverview() {
  beerCoinWalletLoading.value = true
  try {
    beerCoinOverview.value = await fetchBeerCoinOverview()
  } catch {
    beerCoinOverview.value = null
  } finally {
    beerCoinWalletLoading.value = false
  }
}

function formatInteger(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '0'
}

function getStyleLibraryCategoryNames(library) {
  const sourceItems = library?.styleItems?.length ? library.styleItems : (library?.styles || []).map((name) => ({ name }))
  const names = []
  const seen = new Set()
  sourceItems
    .filter((item) => item && item.status !== 0)
    .forEach((item) => {
      const name = formatStyleItemName(item)
      if (!name || seen.has(name)) return
      seen.add(name)
      names.push(name)
    })
  return names
}

function replaceDraftCategories(names) {
  draft.categories.splice(
    0,
    draft.categories.length,
    ...names.map((name, index) => ({ id: `cat-sync-${Date.now()}-${index}`, name })),
  )
}

function syncCategoriesFromSelectedLibrary() {
  const names = getStyleLibraryCategoryNames(selectedStyleLibrary.value)
  if (!names.length) {
    ElMessage.warning('当前风格库没有可同步的风格')
    return false
  }
  replaceDraftCategories(names)
  return true
}

function cloneCategories(categories) {
  return categories.map((category) => ({ ...category }))
}

function toggleCategoryStyleSync(enabled) {
  if (enabled) {
    manualCategoriesBeforeStyleSync = cloneCategories(draft.categories)
    if (syncCategoriesFromSelectedLibrary()) {
      ElMessage.success('已按当前风格库同步投递组别')
      return
    }
    syncCategoriesWithStyleLibrary.value = false
    return
  }
  draft.categories.splice(0, draft.categories.length, ...cloneCategories(manualCategoriesBeforeStyleSync))
}

watch(() => draft.styleLibraryVersion, (nextValue, previousValue) => {
  if (!nextValue || nextValue === previousValue) return
  if (syncCategoriesWithStyleLibrary.value && !syncCategoriesFromSelectedLibrary()) {
    syncCategoriesWithStyleLibrary.value = false
    draft.categories.splice(0, draft.categories.length, ...cloneCategories(manualCategoriesBeforeStyleSync))
  }
})

function getCategoryNames(source) {
  return source.categories.map((category) => category.name.trim()).filter(Boolean)
}

function getJudgeVisibleFields(source) {
  return source.entryFields
    .filter((field) => field.label.trim() && field.visibleToJudges)
    .map((field) => field.label.trim())
}

function hasEntryFieldContent(field) {
  return Boolean(
    field.label.trim()
    || (field.helpText || '').trim()
    || field.required
    || field.visibleToJudges
    || field.type !== 'text',
  )
}

function getIncompleteEntryFields(source) {
  return source.entryFields.filter((field) => !field.label.trim() && hasEntryFieldContent(field))
}

function toDraftSnapshot(source) {
  return {
    name: source.name,
    competitionType: source.competitionType,
    date: source.date,
    registrationStart: source.registrationStart,
    registrationDeadline: source.registrationDeadline,
    entryFee: source.entryFee,
    earlyBirdFee: source.earlyBirdFee,
    earlyBirdDeadline: source.earlyBirdDeadline,
    tierPricingEnabled: Boolean(source.tierPricingEnabled),
    feeTiers: (source.feeTiers || []).map((tier) => ({ ...tier })),
    description: source.description,
    rulesUrl: source.rulesUrl,
    deliveryMethod: source.deliveryMethod,
    sampleArrivalStart: source.sampleArrivalStart,
    sampleArrivalDeadline: source.sampleArrivalDeadline,
    sampleQuantityNote: source.sampleQuantityNote,
    deliveryRecipient: source.deliveryRecipient,
    deliveryPhone: source.deliveryPhone,
    deliveryAddress: source.deliveryAddress,
    deliveryNote: source.deliveryNote,
    logisticsVisibility: source.logisticsVisibility,
    categories: source.categories.map((category) => ({ name: category.name })),
    styleLibraryVersion: source.styleLibraryVersion,
    entryFields: source.entryFields.map((field) => ({
      label: field.label,
      type: field.type,
      helpText: field.helpText,
      required: field.required,
      visibleToJudges: field.visibleToJudges,
      options: [...(field.options || [])],
    })),
    scoreConfigs: source.scoreConfigs.map((config) => ({
      role: config.role,
      minCommentLength: config.minCommentLength,
      dimensions: config.dimensions.map((dimension) => ({
        label: dimension.label,
        maxScore: dimension.maxScore,
        notePrompt: dimension.notePrompt,
      })),
    })),
  }
}

function summarizeList(items, emptyText) {
  if (!items.length) {
    return emptyText
  }
  if (items.length <= 3) {
    return items.join('、')
  }
  return `${items.slice(0, 3).join('、')} 等 ${items.length} 项`
}

function getDuplicateItems(items) {
  const seen = new Set()
  const duplicated = new Set()
  items.forEach((item) => {
    if (seen.has(item)) {
      duplicated.add(item)
    }
    seen.add(item)
  })
  return Array.from(duplicated)
}

function isDeadlineAfterStart(start, deadline) {
  if (!start || !deadline) {
    return false
  }
  return new Date(deadline).getTime() > new Date(start).getTime()
}

function isOptionalDeadlineAfterStart(start, deadline) {
  if (!start || !deadline) {
    return true
  }
  return new Date(deadline).getTime() > new Date(start).getTime()
}

function isFutureDateTime(value) {
  if (!value) {
    return false
  }
  const time = new Date(value).getTime()
  return Number.isFinite(time) && time > Date.now()
}

function isTodayOrLater(dateValue) {
  if (!dateValue) {
    return false
  }
  const time = new Date(`${dateValue}T23:59:59`).getTime()
  return Number.isFinite(time) && time >= Date.now()
}

function isDateOnOrAfter(dateValue, dateTimeValue) {
  if (!dateValue || !dateTimeValue) {
    return true
  }
  return new Date(`${dateValue}T00:00:00`).getTime() >= new Date(dateTimeValue).getTime()
}

function isValidHttpUrl(value) {
  return /^https?:\/\//i.test(String(value || '').trim())
}

function deliveryMethodText(value) {
  if (value === 'EXPRESS') return '仅快递寄送'
  if (value === 'ONSITE') return '仅现场送样'
  return '快递寄送 / 现场送样'
}

function createEntryFieldKey() {
  return `custom_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
}

function createScoreConfigs() {
  const promptMap = {
    香气: '描述香气强度、干净度和主要香气来源',
    外观: '描述颜色、清澈度、泡沫和持久性',
    味道: '描述甜苦酸平衡、风味层次和缺陷',
    口感: '描述酒体、杀口感、顺滑度和余味',
    整体印象: '汇总整体表现、完成度和改进方向',
    整体感受: '描述第一印象、愉悦度和整体完成度',
    适饮性: '描述是否易饮、是否愿意继续饮用',
    记忆点: '描述最容易被记住的风味或体验',
    共识分: '桌长讨论后填写独立共识分',
  }
  return defaultScoreConfigs().map((config) => ({
    ...config,
    locked: config.role === 'PROFESSIONAL',
    minCommentLength: config.role === 'CROSS' ? 50 : 30,
    dimensions: config.dimensions.map((dimension, index) => ({
      key: dimension.key || `${config.role.toLowerCase()}_${index + 1}`,
      label: dimension.label,
      maxScore: dimension.maxScore,
      notePrompt: promptMap[dimension.label] || '请输入该维度备注提示',
    })),
  }))
}

function buildReviewItems(source) {
  const missingBaseFields = []
  if (!source.name) missingBaseFields.push('比赛名称')
  if (!source.date) missingBaseFields.push('比赛日期')
  if (!source.registrationStart) missingBaseFields.push('报名开始')
  if (!source.registrationDeadline) missingBaseFields.push('报名截止')
  if (source.entryFee === '' || source.entryFee === null || Number(source.entryFee) < 0) missingBaseFields.push('报名费')
  if (!source.description) missingBaseFields.push('赛事简介')

  const earlyBirdIncomplete = Boolean(source.earlyBirdFee || source.earlyBirdFee === 0) !== Boolean(source.earlyBirdDeadline)
  const earlyBirdInvalid = !earlyBirdIncomplete && source.earlyBirdDeadline
    && (!isDeadlineAfterStart(source.registrationStart, source.earlyBirdDeadline)
      || !isOptionalDeadlineAfterStart(source.earlyBirdDeadline, source.registrationDeadline)
      || !isFutureDateTime(source.earlyBirdDeadline)
      || Number(source.earlyBirdFee) > Number(source.entryFee)
      || Number(source.earlyBirdFee) < 0)
  const tierInvalid = source.tierPricingEnabled && (!source.feeTiers?.length
    || source.feeTiers.some((tier, index) => Number(tier.startQuantity) <= 1
      || (index > 0 && Number(tier.startQuantity) <= Number(source.feeTiers[index - 1].startQuantity))
      || Number(tier.discountRatePercent) <= 0 || Number(tier.discountRatePercent) > 100
      || (index > 0 && Number(tier.discountRatePercent) > Number(source.feeTiers[index - 1].discountRatePercent))))
  const categoryNames = getCategoryNames(source)
  const duplicatedCategories = getDuplicateItems(categoryNames)
  const scoreErrors = source.scoreConfigs
    .filter((config) => getScoreTotal(config) !== 50)
    .map((config) => `${roleLabels[config.role]} ${getScoreTotal(config)} 分`)
  const crossConfig = source.scoreConfigs.find((config) => config.role === 'CROSS')
  const crossDimensionValid = crossConfig && crossConfig.dimensions.length >= 2 && crossConfig.dimensions.length <= 3
  const visibleFields = getJudgeVisibleFields(source)
  const incompleteEntryFields = getIncompleteEntryFields(source)
  const logisticsSummary = [
    source.sampleArrivalDeadline ? `送达截止 ${formatDateTime(source.sampleArrivalDeadline)}` : '',
    source.deliveryAddress ? '已填收件地址' : '',
  ].filter(Boolean)

  return [
    {
      key: 'baseInfo',
      label: '基础信息',
      target: 'base-info',
      status: missingBaseFields.length ? 'pending' : 'done',
      detail: missingBaseFields.length ? `缺少${missingBaseFields.join('、')}` : `${source.name}，${source.date}`,
    },
    {
      key: 'competitionDate',
      label: '比赛日期',
      target: 'base-info',
      status: isTodayOrLater(source.date) && isDateOnOrAfter(source.date, source.registrationDeadline) ? 'done' : 'pending',
      detail: !source.date
        ? '请填写比赛日期'
        : !isTodayOrLater(source.date)
          ? '比赛日期不能早于今天'
          : !isDateOnOrAfter(source.date, source.registrationDeadline)
            ? '比赛日期需晚于报名截止时间'
            : source.date,
    },
    {
      key: 'time',
      label: '报名时间',
      target: 'base-info',
      status: isDeadlineAfterStart(source.registrationStart, source.registrationDeadline) && isFutureDateTime(source.registrationDeadline) ? 'done' : 'pending',
      detail: !isDeadlineAfterStart(source.registrationStart, source.registrationDeadline)
        ? '报名截止需晚于报名开始'
        : !isFutureDateTime(source.registrationDeadline)
          ? '报名截止时间已过期，请改为未来时间'
          : `截止 ${formatDateTime(source.registrationDeadline)}`,
    },
    {
      key: 'earlyBird',
      label: '早鸟价',
      target: 'base-info',
      status: !earlyBirdIncomplete && !earlyBirdInvalid ? 'done' : 'pending',
      detail: earlyBirdIncomplete
        ? '早鸟价和截止时间需要同时填写'
        : earlyBirdInvalid
          ? '早鸟价需不高于报名费，截止时间需晚于当前时间且在报名窗口内'
          : source.earlyBirdFee ? `早鸟 ${source.earlyBirdFee} 元 / 款，截止 ${formatDateTime(source.earlyBirdDeadline)}` : '未设置早鸟价',
    },
    {
      key: 'rulesUrl',
      label: '参赛细则',
      target: 'base-info',
      status: !source.rulesUrl || isValidHttpUrl(source.rulesUrl) ? 'done' : 'pending',
      detail: !source.rulesUrl
        ? '未设置参赛细则链接'
        : isValidHttpUrl(source.rulesUrl)
          ? '已设置参赛细则链接'
          : '参赛细则链接需以 http:// 或 https:// 开头',
    },
    {
      key: 'tierPricing',
      label: '阶梯报名费',
      target: 'base-info',
      status: !tierInvalid ? 'done' : 'pending',
      detail: !source.tierPricingEnabled ? '未启用阶梯报名费' : (tierInvalid ? '请按起始款数升序配置折扣率' : `已配置 ${source.feeTiers.length} 档`),
    },
    {
      key: 'logistics',
      label: '送样信息',
      target: 'logistics-info',
      status: isOptionalDeadlineAfterStart(source.sampleArrivalStart, source.sampleArrivalDeadline) ? 'done' : 'pending',
      detail: isOptionalDeadlineAfterStart(source.sampleArrivalStart, source.sampleArrivalDeadline)
        ? logisticsSummary.join('，') || '可保存草稿，开放报名前请补齐送样信息'
        : '送达截止需晚于送达开始',
    },
    {
      key: 'categories',
      label: '投递组别',
      target: 'entry-config',
      status: categoryNames.length && !duplicatedCategories.length ? 'done' : 'pending',
      detail: !categoryNames.length
        ? '请至少配置一个投递组别'
        : duplicatedCategories.length
          ? `组别名称重复：${duplicatedCategories.join('、')}`
          : summarizeList(categoryNames, '未配置'),
    },
    {
      key: 'entryFields',
      label: '补充字段',
      target: 'entry-config',
      status: incompleteEntryFields.length ? 'pending' : 'done',
      detail: incompleteEntryFields.length
        ? `有 ${incompleteEntryFields.length} 个补充字段缺少字段名称`
        : visibleFields.length ? `${summarizeList(visibleFields, '无')} 对评审可见` : '无评审可见字段',
    },
    {
      key: 'scoreForms',
      label: '评分表',
      target: 'score-config',
      status: scoreErrors.length ? 'pending' : 'done',
      detail: scoreErrors.length ? `总分需为 50：${scoreErrors.join('、')}` : '三类评分表均为 50 分',
    },
    {
      key: 'crossDimensions',
      label: '跨界评审',
      target: 'score-config',
      status: crossDimensionValid ? 'done' : 'pending',
      detail: crossDimensionValid ? `${crossConfig.dimensions.length} 个评分维度` : '跨界评审需配置 2-3 个维度',
    },
  ]
}
</script>

<style scoped>
.competition-create {
  --panel: rgba(22, 32, 36, 0.9);
  --panel-strong: rgba(26, 39, 44, 0.96);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #5f737d;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  --orange: #f2994a;
  --red: #e05252;
  height: 100vh;
  box-sizing: border-box;
  padding: 0 28px 18px;
  color: var(--text);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(216, 169, 53, 0.12), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
}

.competition-create > .admin-page-header {
  width: min(100%, 1180px);
}

h1,
h2,
h3,
p {
  margin: 0;
}

button,
input,
select,
textarea {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

svg {
  width: 1em;
  height: 1em;
}

.breadcrumb-link,
.head-actions,
.tool-button,
.card-title,
.category-row,
.dimension-row,
.section-actions {
  display: flex;
  align-items: center;
}

p,
small,
label span {
  color: var(--muted);
}

.tool-button,
.icon-button {
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.breadcrumb-link {
  justify-self: start;
  gap: 7px;
  min-height: 28px;
  padding: 0;
  color: var(--muted);
  border: 0;
  background: transparent;
  font-size: 13px;
  font-weight: 800;
}

.breadcrumb-link:hover {
  color: var(--gold-soft);
}

.head-actions,
.section-actions {
  gap: 10px;
}

.tool-button {
  justify-content: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
}

.tool-button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.anchor-bar {
  flex: 0 0 auto;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  width: min(100%, 1180px);
  min-height: 30px;
  margin-top: 8px;
  align-items: center;
}

.anchor-bar button {
  position: relative;
  min-height: 28px;
  padding: 0;
  color: var(--muted);
  border: 0;
  border-radius: 0;
  background: transparent;
  font-size: 13px;
  font-weight: 700;
}

.anchor-bar button::after {
  position: absolute;
  right: 0;
  bottom: -4px;
  left: 0;
  height: 2px;
  border-radius: 999px;
  background: transparent;
  content: '';
}

.anchor-bar button:hover,
.anchor-bar button.active {
  color: var(--gold-soft);
}

.anchor-bar button.active::after {
  background: rgba(224, 184, 74, 0.78);
}

.anchor-bar button.issue::before {
  position: absolute;
  top: 3px;
  right: -9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--orange);
  box-shadow: 0 0 0 3px rgba(242, 153, 74, 0.12);
  content: '';
}

.create-panel {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  width: min(100%, 1180px);
  margin-top: 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  overflow-y: auto;
}

.form-section {
  padding: 28px;
  border-bottom: 1px solid var(--line);
  scroll-margin-top: 12px;
}

.form-section:last-child {
  border-bottom: 0;
}

.form-section,
.create-panel,
.category-list {
  scrollbar-color: rgba(255, 255, 255, 0.16) rgba(255, 255, 255, 0.035);
  scrollbar-width: thin;
}

.form-section::-webkit-scrollbar,
.create-panel::-webkit-scrollbar,
.category-list::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.form-section::-webkit-scrollbar-track,
.create-panel::-webkit-scrollbar-track,
.category-list::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.045);
}

.form-section::-webkit-scrollbar-thumb,
.create-panel::-webkit-scrollbar-thumb,
.category-list::-webkit-scrollbar-thumb {
  border: 1px solid rgba(16, 25, 29, 0.95);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
}

.form-section::-webkit-scrollbar-thumb:hover,
.create-panel::-webkit-scrollbar-thumb:hover,
.category-list::-webkit-scrollbar-thumb:hover {
  background: rgba(224, 184, 74, 0.45);
}

.form-section header {
  margin-bottom: 22px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.form-grid {
  display: grid;
  gap: 14px;
}

.base-info-groups {
  display: grid;
  gap: 18px;
  max-width: 1000px;
}

.form-subgroup {
  display: grid;
  gap: 12px;
  padding-top: 2px;
}

.form-subgroup + .form-subgroup {
  padding-top: 16px;
  border-top: 1px solid rgba(219, 232, 237, 0.08);
}

.form-subgroup h3 {
  color: #c7d5db;
  font-size: 13px;
  line-height: 1.2;
}

.subgroup-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.competition-type-heading {
  justify-content: flex-start;
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

.info-hint {
  position: relative;
}

.info-hint svg {
  width: 13px;
  height: 13px;
}

.info-hint::after {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  z-index: 20;
  width: min(280px, calc(100vw - 80px));
  padding: 10px 12px;
  color: var(--text);
  border: 1px solid rgba(216, 169, 53, 0.26);
  border-radius: 8px;
  background: rgba(18, 27, 31, 0.98);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.26);
  content: attr(data-tooltip);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.55;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-4px);
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.info-hint:hover::after,
.info-hint:focus::after {
  opacity: 1;
  transform: translateY(0);
}

.refund-mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 8px;
  max-width: 420px;
}

.refund-mode-switch button {
  min-height: 40px;
  padding: 9px 14px;
  color: #c7d5db;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.refund-mode-switch button.active {
  color: #10191d;
  border-color: rgba(224, 184, 74, 0.78);
  background: var(--gold-soft);
}

.competition-type-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 10px;
  max-width: 620px;
}

.competition-type-switch button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: 0 16px;
  text-align: left;
  color: #c7d5db;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.competition-type-switch button.active {
  color: #10191d;
  border-color: rgba(224, 184, 74, 0.78);
  background: #e0b84a;
  box-shadow: 0 4px 14px rgba(224, 184, 74, 0.12);
}

.competition-type-switch button:hover:not(.active) {
  border-color: rgba(224, 184, 74, 0.42);
  background: rgba(224, 184, 74, 0.06);
}

.competition-type-switch button:focus-visible {
  outline: 2px solid rgba(224, 184, 74, 0.8);
  outline-offset: 2px;
}

.competition-type-switch strong {
  overflow-wrap: anywhere;
  font-size: 14px;
  line-height: 1.2;
}

.form-grid.two {
  max-width: 1000px;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  justify-content: start;
}

label {
  display: grid;
  gap: 8px;
}

input,
select,
textarea {
  min-width: 0;
  min-height: 42px;
  padding: 0 12px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  outline: 0;
  background: rgba(255, 255, 255, 0.04);
}

textarea {
  min-height: 92px;
  padding-top: 10px;
  line-height: 1.55;
  resize: vertical;
}

input::placeholder,
textarea::placeholder {
  color: var(--faint);
}

.wide-field {
  grid-column: 1 / -1;
}

.logistics-layout {
  display: grid;
  gap: 18px;
}

.logistics-section {
  display: grid;
  gap: 16px;
}

.fee-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.tier-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: fit-content;
  min-height: 38px;
  margin-top: 2px;
  padding: 0 10px 0 13px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.tier-toggle > span {
  color: inherit;
}

.tier-toggle :deep(.el-switch) {
  --el-switch-on-color: var(--gold-soft);
  --el-switch-off-color: rgba(255, 255, 255, 0.16);
}

.tier-toggle :deep(.el-switch__core) {
  min-width: 40px;
}

.tier-toggle.enabled {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.tier-editor {
  display: grid;
  gap: 0;
  max-width: 840px;
  margin-top: 0;
  padding: 0 16px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.018);
}

.tier-editor-head,
.tier-editor-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(140px, 1fr) minmax(160px, 1fr) minmax(76px, auto);
  align-items: center;
  gap: 12px;
}

.tier-editor-head {
  padding: 12px 0 8px;
  color: var(--muted);
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
  font-size: 12px;
}

.tier-editor-row {
  padding: 10px 0;
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
}

.tier-editor-row:last-of-type {
  border-bottom: 0;
}

.tier-editor .fee-field {
  min-width: 0;
}

.tier-editor .fee-field input {
  min-height: 40px;
}

.tier-example-price {
  display: inline-flex;
  align-items: center;
  align-self: center;
  gap: 4px;
  min-height: 40px;
  color: var(--text);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
  white-space: nowrap;
}

.tier-example-price small {
  color: var(--muted);
  font-size: 12px;
}

.tier-remove-button {
  width: 36px;
  height: 36px;
  min-height: 36px;
  padding: 0;
  color: var(--muted);
}

.tier-remove-button:hover:not(:disabled) {
  color: #ffb4aa;
  border-color: rgba(255, 132, 116, 0.35);
  background: rgba(255, 132, 116, 0.08);
}

.tier-add-button {
  justify-self: end;
  margin-top: 0;
  padding: 0 4px;
  white-space: nowrap;
}

.collection-config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  max-width: 1000px;
}

.collection-config-grid .wide-field {
  grid-column: 1 / -1;
}

.collection-method-field {
  display: grid;
  gap: 9px;
}

.collection-method-segmented {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  max-width: 620px;
  padding: 4px;
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
}

.collection-method-option {
  min-height: 42px;
  padding: 0 14px;
  color: #a9bbc2;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: color 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.collection-method-option:hover {
  color: var(--text);
  background: rgba(255, 255, 255, 0.04);
}

.collection-method-option.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.collection-upload {
  display: grid;
  gap: 9px;
}

.field-label {
  color: #c7d5db;
}

.field-label b,
.collection-config-grid label b {
  color: #e0b84a;
}

.collection-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

.upload-dropzone {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2px 12px;
  align-items: center;
  min-height: 94px;
  padding: 16px;
  color: #c7d5db;
  text-align: left;
  border: 1px dashed rgba(224, 184, 74, 0.42);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.upload-dropzone:hover {
  border-color: var(--gold-soft);
  background: rgba(224, 184, 74, 0.08);
}

.upload-icon {
  grid-row: 1 / span 2;
  display: inline-grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #10191d;
  border-radius: 50%;
  background: var(--gold-soft);
  font-size: 22px;
}

.upload-dropzone small,
.qr-preview-card small {
  color: var(--muted);
}

.qr-preview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 140px;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.qr-preview-card img {
  width: 116px;
  height: 116px;
  object-fit: contain;
  border-radius: 6px;
  background: #fff;
}

.qr-preview-card div {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.qr-preview-card strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qr-preview-card button {
  width: fit-content;
  padding: 0;
  color: var(--gold-soft);
  border: 0;
  background: transparent;
  cursor: pointer;
}

.collection-config-grid small {
  color: var(--muted);
}

.beer-coin-create-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  max-width: 1000px;
  margin-top: 16px;
  padding: 12px 14px;
  color: var(--muted);
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.beer-coin-create-hint > div {
  display: grid;
  gap: 5px;
}

.beer-coin-create-hint strong {
  color: #dce8eb;
  font-size: 13px;
  font-weight: 600;
}

.beer-coin-create-hint.insufficient {
  border-color: rgba(242, 153, 74, 0.35);
  background: rgba(242, 153, 74, 0.06);
}

.beer-coin-create-hint.insufficient strong {
  color: var(--orange);
}

@media (max-width: 720px) {
  .collection-config-grid {
    grid-template-columns: 1fr;
  }

  .collection-method-segmented {
    grid-template-columns: 1fr;
  }

  .beer-coin-create-hint {
    align-items: flex-start;
    flex-direction: column;
  }
}

.fee-field input {
  border: 0;
  background: transparent;
}

.fee-field span {
  padding: 0 12px;
  color: var(--muted);
  white-space: nowrap;
}

.built-in-strip {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  max-width: 1040px;
  padding: 0 0 18px;
  border-bottom: 1px solid var(--line);
}

.built-in-strip h3 {
  color: var(--text);
}

.built-in-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-start;
}

.built-in-fields span {
  padding: 7px 10px;
  color: #b7c8d0;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
  font-size: 13px;
}

.score-stack {
  display: grid;
  gap: 14px;
}

.score-stack {
  max-width: 1040px;
}

.review-layout {
  display: grid;
  gap: 18px;
  max-width: 1040px;
}

.entry-section {
  max-width: 1040px;
  padding: 20px 0;
  border-bottom: 1px solid var(--line);
}

.entry-section:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.config-card {
  align-self: start;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.018);
}

.card-title {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.score-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.score-title h3 {
  min-width: 0;
}

.score-info-hint {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  border: 0;
  background: transparent;
  font-size: 0;
}

.score-info-hint svg {
  width: 12px;
  height: 12px;
}

.score-info-hint::after {
  top: calc(100% + 8px);
  left: 0;
  width: min(280px, calc(100vw - 64px));
  font-size: 12px;
}

.title-with-count {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.title-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.style-sync-switch {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 38px;
  padding: 0 11px 0 13px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.style-sync-switch :deep(.el-switch) {
  --el-switch-on-color: #d8a935;
  --el-switch-off-color: rgba(255, 255, 255, 0.16);
}

.style-sync-switch:has(.el-switch.is-checked) {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.count-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  color: #9fb4bf;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.025);
  font-size: 12px;
  font-weight: 700;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
}

.category-list,
.dimension-list,
.field-list {
  display: grid;
  gap: 10px;
}

.category-list {
  grid-template-columns: repeat(auto-fill, minmax(240px, 300px));
  justify-content: start;
  max-height: 260px;
  overflow-y: auto;
  padding-right: 6px;
}

.category-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.category-row input {
  min-height: 38px;
}

.category-row input:disabled {
  color: #aebfc6;
  cursor: not-allowed;
}

.icon-button:disabled,
.tool-button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.field-card {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.012);
}

.field-main-row {
  display: grid;
  grid-template-columns: minmax(320px, 640px) 180px 40px;
  gap: 10px;
  align-items: end;
  justify-content: start;
}

.field-extra-row {
  display: grid;
  grid-template-columns: minmax(320px, 560px) auto auto;
  gap: 16px;
  align-items: end;
  justify-content: start;
}

.field-options-editor {
  display: grid;
  gap: 8px;
  padding: 10px 12px;
  border-left: 2px solid #d5a24f;
  background: rgba(213, 162, 79, 0.06);
}

.option-editor-head,
.option-editor-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.option-editor-head {
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}

.option-editor-row input {
  flex: 1;
  min-height: 34px;
}

.option-editor-row button,
.option-editor-head button {
  border: 0;
  background: transparent;
  color: #d5a24f;
  cursor: pointer;
  font-size: 12px;
}

.option-editor-row button:disabled {
  opacity: .35;
  cursor: default;
}

.option-editor-row .danger-text {
  color: #e68577;
}

.dimension-row {
  display: grid;
  grid-template-columns: minmax(180px, 220px) 80px minmax(260px, 1fr) 36px;
  gap: 10px;
}

.dimension-head {
  color: var(--muted);
  font-size: 13px;
}

.switch-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  cursor: pointer;
  line-height: 1.2;
}

.switch-line input[type='checkbox'] {
  display: grid;
  place-content: center;
  flex: 0 0 auto;
  width: 16px;
  height: 16px;
  min-width: 16px;
  min-height: 16px;
  margin: 0;
  appearance: none;
  border: 1px solid rgba(219, 232, 237, 0.2);
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.035);
  transition: border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.switch-line input[type='checkbox']::before {
  width: 9px;
  height: 9px;
  background: var(--gold-soft);
  clip-path: polygon(14% 44%, 0 58%, 40% 100%, 100% 16%, 84% 0, 38% 65%);
  content: '';
  transform: scale(0);
  transition: transform 0.16s ease;
}

.switch-line input[type='checkbox']:checked {
  border-color: rgba(224, 184, 74, 0.5);
  background: rgba(224, 184, 74, 0.1);
  box-shadow: 0 0 0 3px rgba(224, 184, 74, 0.08);
}

.switch-line input[type='checkbox']:checked::before {
  transform: scale(1);
}

.switch-line:hover input[type='checkbox'] {
  border-color: rgba(224, 184, 74, 0.42);
}

.visibility-switch.warning {
  color: var(--gold-soft);
}

.visibility-switch {
  position: relative;
  cursor: help;
}

.visibility-switch::after {
  position: absolute;
  right: 0;
  bottom: calc(100% + 10px);
  z-index: 20;
  width: 260px;
  padding: 10px 12px;
  color: var(--text);
  border: 1px solid rgba(216, 169, 53, 0.26);
  border-radius: 8px;
  background: rgba(18, 27, 31, 0.98);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.26);
  content: attr(data-tooltip);
  font-size: 12px;
  line-height: 1.55;
  opacity: 0;
  pointer-events: none;
  transform: translateY(4px);
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.visibility-switch:hover::after,
.visibility-switch:focus-within::after {
  opacity: 1;
  transform: translateY(0);
}

.score-total {
  color: var(--orange);
}

.score-total.ok {
  color: var(--green);
}

.score-foot {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto;
  gap: 10px;
  align-items: end;
  padding-top: 4px;
}

.library-row {
  display: grid;
  grid-template-columns: 120px minmax(320px, 420px) auto;
  gap: 18px;
  align-items: center;
  justify-content: start;
}

.library-row h3 {
  align-self: center;
}

.library-select,
.form-select {
  width: 100%;
}

.library-select :deep(.el-select__wrapper),
.form-select :deep(.el-select__wrapper) {
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  box-shadow: none;
}

.library-select :deep(.el-select__wrapper:hover),
.library-select :deep(.el-select__wrapper.is-focused),
.form-select :deep(.el-select__wrapper:hover),
.form-select :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(216, 169, 53, 0.36);
  box-shadow: 0 0 0 2px rgba(216, 169, 53, 0.08);
}

.library-select :deep(.el-select__selected-item),
.library-select :deep(.el-select__placeholder),
.form-select :deep(.el-select__selected-item),
.form-select :deep(.el-select__placeholder) {
  color: var(--text);
}

.library-select :deep(.el-select__caret),
.form-select :deep(.el-select__caret) {
  color: var(--muted);
}

:global(.competition-create-library-popper.el-popper),
:global(.competition-create-select-popper.el-popper) {
  border: 1px solid rgba(219, 232, 237, 0.12);
  background: #10191d;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.36);
}

:global(.competition-create-library-popper .el-select-dropdown),
:global(.competition-create-library-popper .el-select-dropdown__list),
:global(.competition-create-select-popper .el-select-dropdown),
:global(.competition-create-select-popper .el-select-dropdown__list) {
  background: #10191d;
}

:global(.competition-create-library-popper .el-popper__arrow::before),
:global(.competition-create-select-popper .el-popper__arrow::before) {
  border-color: rgba(219, 232, 237, 0.12);
  background: #10191d;
}

:global(.competition-create-library-popper .el-select-dropdown__item),
:global(.competition-create-select-popper .el-select-dropdown__item) {
  height: 36px;
  padding: 0 14px;
  color: #e6edf0;
  line-height: 36px;
}

:global(.competition-create-library-popper .el-select-dropdown__item.hover),
:global(.competition-create-library-popper .el-select-dropdown__item.is-hovering),
:global(.competition-create-library-popper .el-select-dropdown__item:hover),
:global(.competition-create-select-popper .el-select-dropdown__item.hover),
:global(.competition-create-select-popper .el-select-dropdown__item.is-hovering),
:global(.competition-create-select-popper .el-select-dropdown__item:hover) {
  background: rgba(216, 169, 53, 0.08);
}

:global(.competition-create-library-popper .el-select-dropdown__item.selected),
:global(.competition-create-library-popper .el-select-dropdown__item.is-selected),
:global(.competition-create-select-popper .el-select-dropdown__item.selected),
:global(.competition-create-select-popper .el-select-dropdown__item.is-selected) {
  color: #e0b84a;
  background: rgba(216, 169, 53, 0.11);
}

:global(.competition-create-library-popper .el-select-dropdown__item.is-disabled),
:global(.competition-create-select-popper .el-select-dropdown__item.is-disabled) {
  color: rgba(230, 237, 240, 0.3);
}

.library-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-self: center;
  justify-content: flex-start;
}

.library-tags span {
  padding: 7px 10px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.22);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.07);
  font-size: 12px;
  font-weight: 700;
}

.form-grid.compact {
  gap: 10px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.25);
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.08);
  font-size: 12px;
  font-weight: 700;
}

.status-pill.done {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.25);
  background: rgba(111, 207, 122, 0.08);
}

.review-banner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  max-width: 1040px;
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid rgba(111, 207, 122, 0.2);
  border-radius: 8px;
  background: rgba(111, 207, 122, 0.055);
}

.review-banner.blocked {
  border-color: rgba(242, 153, 74, 0.24);
  background: rgba(242, 153, 74, 0.06);
}

.review-banner-copy,
.review-check-item {
  display: flex;
  align-items: center;
}

.review-banner-copy {
  gap: 12px;
  min-width: 0;
}

.review-banner-copy h3 {
  margin-bottom: 4px;
}

.review-banner-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  color: var(--green);
  border: 1px solid rgba(111, 207, 122, 0.28);
  border-radius: 50%;
  background: rgba(111, 207, 122, 0.08);
}

.blocked .review-banner-icon {
  color: var(--orange);
  border-color: rgba(242, 153, 74, 0.32);
  background: rgba(242, 153, 74, 0.1);
}

.review-block {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.014);
}

.review-check-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.review-check-item {
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.012);
}

.review-check-item.done {
  color: var(--green);
}

.review-check-item.pending {
  color: var(--orange);
}

.review-check-item div {
  flex: 1;
  display: grid;
  gap: 4px;
  min-width: 0;
}

.review-check-item span {
  color: var(--muted);
  line-height: 1.45;
}

.text-button {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 0 8px;
  color: var(--gold-soft);
  border: 0;
  background: transparent;
  font-weight: 700;
}

.summary-list {
  display: grid;
  gap: 0;
  margin: 14px 0 0;
}

.summary-list div {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.summary-list div:last-child {
  border-bottom: 0;
}

.summary-list dt {
  color: var(--muted);
}

.summary-list dd {
  min-width: 0;
  margin: 0;
  color: var(--text);
  line-height: 1.5;
}

.section-actions {
  justify-content: flex-end;
  padding: 18px 24px;
  border-top: 1px solid var(--line);
}

.inline-actions {
  flex: 0 0 auto;
  padding: 0;
  border-top: 0;
}

@media (max-width: 720px) {
  .competition-create > .admin-page-header { align-items: stretch; }
}

@media (max-width: 820px) {
  .competition-create {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .form-grid.two,
  .competition-type-switch,
  .score-stack,
  .review-layout,
  .built-in-strip,
  .library-row {
    grid-template-columns: 1fr;
  }

  .dimension-row,
  .field-main-row,
  .field-extra-row,
  .score-foot,
  .review-banner {
    grid-template-columns: 1fr;
  }

  .summary-list div {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .review-check-item {
    align-items: flex-start;
  }

  .built-in-fields {
    justify-content: flex-start;
  }

  .competition-type-switch {
    grid-template-columns: 1fr;
    max-width: 100%;
  }

  .library-tags {
    justify-content: flex-start;
  }

  .form-section {
    padding: 20px;
  }

  .section-actions {
    padding: 14px 20px;
  }

  .review-head {
    align-items: stretch;
  }

  .tier-editor {
    padding: 0 12px 10px;
  }

  .tier-editor-head {
    grid-template-columns: 1fr;
  }

  .tier-editor-head > span {
    display: none;
  }

  .tier-editor-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .tier-remove-button {
    justify-self: end;
  }

  .inline-actions {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

}
</style>
