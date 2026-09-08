<template>
  <div :class="['organizer-application-page', { 'workspace-open': !showLanding }]">
    <OrganizerApplicationLanding v-if="showLanding" @apply="openApply" />

    <template v-else>
    <section class="application-intro" aria-labelledby="application-section-title">
      <div>
        <span class="section-eyebrow">主办方入驻</span>
        <h2 id="application-section-title">
          {{ pageMode === 'status' ? '找到您的申请记录' : resubmitContext ? '补充资料，继续审核' : '申请成为平台赛事主办方' }}
        </h2>
      </div>
      <p>
        {{ pageMode === 'status'
          ? portalLoggedIn ? '已登录的账号，会自动带出您的申请。' : '用申请编号和联系人手机号，找回您的申请。'
          : '用几分钟时间留下基本资料，后续由平台团队与您联系。' }}
      </p>
      <button
        v-if="pageMode === 'apply' && !submittedApplication && !resubmitContext"
        class="experience-back"
        type="button"
        @click="closeApplication"
      >
        <ArrowLeft />
        返回了解平台
      </button>
    </section>

    <Transition name="stage" mode="out-in">
      <section v-if="pageMode === 'status'" key="status" class="status-layout" aria-labelledby="status-title">
        <aside class="status-side">
          <span class="side-number">01</span>
          <h3 id="status-title">申请进度</h3>
          <p>{{ portalLoggedIn ? '已登录的账号，会自动带出您的申请。' : '申请提交后，请用相同手机号进行查询。' }}</p>
          <RouterLink class="side-link" to="/portal/organizer-application">
            <ArrowLeft />
            返回入驻申请
          </RouterLink>
        </aside>

        <div class="status-main">
          <div v-if="portalLoggedIn" class="mine-status-toolbar">
            <div class="mine-status-heading">
              <span class="result-eyebrow">SIGNED-IN VIEW</span>
              <strong>我的入驻申请</strong>
            </div>
            <div v-if="myApplications.length > 1" class="application-switcher" role="tablist" aria-label="选择申请记录">
              <button
                v-for="application in myApplications"
                :key="application.applicationNo"
                type="button"
                :class="{ active: application.applicationNo === selectedApplicationNo }"
                role="tab"
                :aria-selected="application.applicationNo === selectedApplicationNo"
                @click="selectMyApplication(application.applicationNo)"
              >
                <span>{{ application.organizationName || '未命名机构' }}</span>
                <small>{{ application.statusLabel || application.status }}</small>
              </button>
            </div>
          </div>

          <div v-if="portalLoggedIn && myApplicationsLoading" class="status-loading" aria-live="polite">
            <Loading class="spin-icon" />
            正在读取申请进度
          </div>

          <div v-if="portalLoggedIn && !myApplicationsLoading && !myApplications.length && !myApplicationsError && !statusResult" class="mine-status-empty">
            <OfficeBuilding />
            <strong>还没有入驻申请</strong>
            <button class="primary-action" type="button" @click="openApply">
              <DocumentAdd />
              开始申请
            </button>
          </div>

          <div v-if="portalLoggedIn && myApplicationsError" class="submit-error" role="alert">
            {{ myApplicationsError }}
          </div>

          <button
            v-if="portalLoggedIn && !showFallbackLookup"
            class="fallback-lookup-trigger"
            type="button"
            @click="showFallbackLookup = true"
          >
            <Search />
            用申请编号和手机号查询
          </button>
          <button
            v-if="portalLoggedIn && showFallbackLookup"
            class="fallback-lookup-trigger"
            type="button"
            @click="showFallbackLookup = false; loadMyApplications()"
          >
            <ArrowLeft />
            返回我的申请
          </button>

          <form v-if="!portalLoggedIn || showFallbackLookup || myApplicationsError" class="status-lookup-form" novalidate @submit.prevent="queryStatus">
            <div class="form-section-heading compact-heading">
              <span class="form-section-index">A</span>
              <div>
                <h3>验证申请信息</h3>
                <p>靠申请编号 + 手机号，找回您的申请。</p>
              </div>
            </div>
            <div class="field-grid field-grid-status">
              <label class="field">
                <span class="field-label">申请编号 <b>*</b></span>
                <span class="field-control">
                  <Document />
                  <input
                    v-model.trim="statusForm.applicationNo"
                    type="text"
                    autocomplete="off"
                    placeholder="例如 OA7A1B..."
                    :aria-invalid="Boolean(statusErrors.applicationNo)"
                    @blur="validateStatusField('applicationNo')"
                    @input="clearStatusError('applicationNo')"
                  >
                </span>
                <small v-if="statusErrors.applicationNo" class="field-error">{{ statusErrors.applicationNo }}</small>
              </label>
              <label class="field">
                <span class="field-label">联系人手机号 <b>*</b></span>
                <span class="field-control">
                  <Phone />
                  <input
                    v-model.trim="statusForm.contactPhone"
                    type="tel"
                    inputmode="numeric"
                    autocomplete="tel"
                    maxlength="11"
                    placeholder="提交申请时填写的手机号"
                    :aria-invalid="Boolean(statusErrors.contactPhone)"
                    @blur="validateStatusField('contactPhone')"
                    @input="clearStatusError('contactPhone')"
                  >
                </span>
                <small v-if="statusErrors.contactPhone" class="field-error">{{ statusErrors.contactPhone }}</small>
              </label>
            </div>
            <p v-if="statusQueryError" class="submit-error" role="alert">{{ statusQueryError }}</p>
            <div class="form-actions lookup-actions">
              <button class="primary-action" type="submit" :disabled="statusLoading">
                <Loading v-if="statusLoading" class="spin-icon" />
                <Search v-else />
                {{ statusLoading ? '查询中...' : '查询申请进度' }}
              </button>
            </div>
          </form>

          <Transition name="result-reveal">
             <section v-if="statusResult" class="status-result" aria-live="polite">
              <div class="status-result-topline">
                <div>
                  <span class="result-eyebrow">APPLICATION STATUS</span>
                  <h3>{{ statusResult.organizationName }}</h3>
                  <p>申请编号 {{ statusResult.applicationNo }}</p>
                </div>
                <span :class="['status-badge', statusTone(statusResult.status)]">
                  <CircleCheck v-if="statusResult.status === 'ACCOUNT_ISSUED' || statusResult.status === 'APPROVED'" />
                  <Warning v-else-if="statusResult.status === 'REJECTED'" />
                  <Clock v-else />
                  {{ statusResult.statusLabel || statusResult.status }}
                </span>
              </div>

              <ol class="status-timeline">
                <li
                  v-for="(item, index) in statusTimeline"
                  :key="item.key"
                  :class="{ complete: statusProgress > index, current: statusProgress === index && statusResult.status !== 'REJECTED' }"
                >
                  <span class="timeline-marker">
                    <Check v-if="statusProgress > index" />
                    <span v-else>{{ String(index + 1).padStart(2, '0') }}</span>
                  </span>
                  <span class="timeline-copy">
                    <strong>{{ item.label }}</strong>
                    <small>{{ item.description }}</small>
                  </span>
                </li>
              </ol>

               <div v-if="statusResult.reviewRemark" class="review-note">
                <span>平台备注</span>
                <p>{{ statusResult.reviewRemark }}</p>
               </div>

               <div v-if="statusResult.status === 'ACCOUNT_ISSUED' && statusResult.adminUsername" class="issued-account-panel">
                 <div class="issued-account-heading">
                   <div>
                     <span>主办方后台账号</span>
                     <strong>{{ statusResult.adminUsername }}</strong>
                   </div>
                   <span class="issued-account-state">已开通</span>
                 </div>
                 <div v-if="statusResult.initialPassword" class="issued-credentials">
                   <div class="issued-credential-row">
                     <span>初始密码</span>
                     <code>{{ statusResult.initialPassword }}</code>
                     <button type="button" :aria-label="credentialCopied === 'password' ? '初始密码已复制' : '复制初始密码'" @click="copyCredential(statusResult.initialPassword, 'password')">
                       <Check v-if="credentialCopied === 'password'" />
                       <CopyDocument v-else />
                     </button>
                   </div>
                   <p>请使用这组凭据登录后台，并在首次登录时完成账号设置。</p>
                 </div>
                 <p v-else class="issued-credential-note">初始密码已交付或已完成首次设置，如需重新进入后台，请联系平台管理员。</p>
                 <RouterLink class="primary-action" :to="{ path: '/admin/login', query: { username: statusResult.adminUsername } }">
                   <Right />
                   使用账号登录
                 </RouterLink>
               </div>

               <div class="status-result-footer">
                <span v-if="statusResult.maskedContactPhone">联系人 {{ statusResult.maskedContactPhone }}</span>
                <span v-if="statusResult.submittedTime">提交于 {{ formatDateTime(statusResult.submittedTime) }}</span>
                <button v-if="statusResult.status === 'NEED_MORE_INFO'" class="primary-action" type="button" @click="startResubmit">
                  <DocumentAdd />
                  补充资料
                </button>
              </div>
            </section>
          </Transition>
        </div>
      </section>

      <section v-else-if="submittedApplication" key="success" class="success-layout" aria-labelledby="success-title">
        <div class="success-mark" aria-hidden="true"><Check /></div>
        <div class="success-copy">
          <span class="section-eyebrow">APPLICATION RECEIVED</span>
          <h2 id="success-title">申请已提交，感谢您的信任</h2>
          <p>审核通过后，我们会主动联系这位联系人。请保存申请编号，随时可查进度。</p>
        </div>
        <div class="application-number-block">
          <span>您的申请编号</span>
          <div class="application-number-row">
            <code>{{ submittedApplication.applicationNo }}</code>
            <button type="button" :aria-label="isCopied ? '申请编号已复制' : '复制申请编号'" @click="copyApplicationNo">
              <Check v-if="isCopied" />
              <CopyDocument v-else />
            </button>
          </div>
          <small>{{ isCopied ? '已复制到剪贴板' : '请妥善保存此编号' }}</small>
        </div>
        <div class="success-actions">
          <RouterLink class="primary-action" :to="{ path: '/portal/organizer-application/status', query: { applicationNo: submittedApplication.applicationNo } }">
            <Search />
            查询申请进度
          </RouterLink>
          <button class="secondary-action" type="button" @click="startNewApplication">
            <Plus />
            提交另一份申请
          </button>
        </div>
      </section>

      <section v-else key="form" id="application-form" class="application-layout" aria-labelledby="form-title">
        <aside class="steps-side">
          <div class="steps-intro">
            <span class="side-number">03</span>
            <h3>资料采集</h3>
            <p>三步完成基础申请，填写内容越完整，平台越容易了解您的办赛计划。</p>
          </div>
          <ol class="step-list" aria-label="申请步骤">
            <li v-for="step in steps" :key="step.number" :class="{ active: currentStep === step.number, reached: currentStep >= step.number }">
              <button type="button" :disabled="step.number > currentStep" @click="goToStep(step.number)">
                <span class="step-marker">
                  <Check v-if="currentStep > step.number" />
                  <span v-else>{{ String(step.number).padStart(2, '0') }}</span>
                </span>
                <span class="step-copy">
                  <strong>{{ step.title }}</strong>
                  <small>{{ step.description }}</small>
                </span>
              </button>
            </li>
          </ol>
          <div class="privacy-note">
            <Lock />
            <span>资料仅用于入驻审核，敏感联系方式会按规则保护。</span>
          </div>
        </aside>

        <form class="form-panel" novalidate @submit.prevent="submitApplication">
          <div class="form-panel-topline">
            <span>STEP {{ String(currentStep).padStart(2, '0') }} / 03</span>
            <span>{{ resubmitContext ? '补充申请资料' : '公开入驻申请' }}</span>
          </div>

          <Transition name="step-content" mode="out-in">
            <section v-if="currentStep === 1" key="step-one" class="form-step" aria-labelledby="step-one-title">
              <div class="form-section-heading">
                <span class="form-section-index">01</span>
                <div>
                  <h3 id="step-one-title">先说说，这场赛由谁来办</h3>
                  <p>填办赛挂靠的公司或机构名称。</p>
                </div>
              </div>
              <div class="field-grid">
                <label class="field field-span-two">
                  <span class="field-label">公司 / 机构名称 <b>*</b></span>
                  <span class="field-control field-control-large">
                    <OfficeBuilding />
                    <input
                      v-model="form.organizationName"
                      type="text"
                      maxlength="128"
                      autocomplete="organization"
                      placeholder="例如：某某精酿文化有限公司"
                      :aria-invalid="Boolean(fieldError('organizationName'))"
                      @blur="validateField('organizationName')"
                      @input="clearError('organizationName')"
                    >
                  </span>
                  <small v-if="fieldError('organizationName')" class="field-error">{{ fieldError('organizationName') }}</small>
                </label>
              </div>

              <div class="upload-block">
                <div class="upload-heading">
                  <div>
                    <span class="field-label">机构证明文件 <i>选填</i></span>
                    <p>营业执照、机构证明或办赛相关资料都可以。</p>
                  </div>
                  <span class="upload-limit">PDF / JPG / PNG · 10MB 内</span>
                </div>
                <div
                  class="upload-zone"
                  :class="{ 'is-dragging': isDragging, 'has-file': material }"
                  tabindex="0"
                  role="button"
                  aria-label="上传机构证明文件"
                  @click="openFilePicker"
                  @keydown.enter.prevent="openFilePicker"
                  @keydown.space.prevent="openFilePicker"
                  @dragover.prevent="isDragging = true"
                  @dragleave.prevent="isDragging = false"
                  @drop.prevent="handleFileDrop"
                >
                  <input ref="materialInput" type="file" accept="application/pdf,image/jpeg,image/png" hidden @change="handleFileChange">
                  <template v-if="material">
                    <span class="file-icon"><Document /></span>
                    <span class="file-info">
                      <strong>{{ material.name }}</strong>
                      <small>{{ formatFileSize(material.size) }} · 已准备上传</small>
                    </span>
                    <button class="file-remove" type="button" aria-label="移除材料" @click.stop="removeMaterial">
                      <Close />
                    </button>
                  </template>
                  <template v-else>
                    <span class="upload-icon"><UploadFilled /></span>
                    <span class="upload-copy">
                      <strong>拖入文件，或点击选择</strong>
                      <small>一份材料即可，审核时会由平台团队查看</small>
                    </span>
                    <ArrowRight class="upload-arrow" />
                  </template>
                </div>
                <small v-if="errors.material" class="field-error">{{ errors.material }}</small>
              </div>
            </section>

            <section v-else-if="currentStep === 2" key="step-two" class="form-step" aria-labelledby="step-two-title">
              <div class="form-section-heading">
                <span class="form-section-index">02</span>
                <div>
                  <h3 id="step-two-title">留下可以联系到您的方式</h3>
                  <p>审核进度和开号通知，都会发到这位联系人。</p>
                </div>
              </div>
              <div class="field-grid">
                <label class="field">
                  <span class="field-label">联系人姓名 <b>*</b></span>
                  <span class="field-control">
                    <User />
                    <input
                      v-model="form.contactName"
                      type="text"
                      maxlength="64"
                      autocomplete="name"
                      placeholder="填写主要联系人"
                      :aria-invalid="Boolean(fieldError('contactName'))"
                      @blur="validateField('contactName')"
                      @input="clearError('contactName')"
                    >
                  </span>
                  <small v-if="fieldError('contactName')" class="field-error">{{ fieldError('contactName') }}</small>
                </label>
                <label class="field">
                  <span class="field-label">联系人手机号 <b>*</b></span>
                  <span class="field-control">
                    <Phone />
                    <input
                      v-model="form.contactPhone"
                      type="tel"
                      inputmode="numeric"
                      maxlength="11"
                      autocomplete="tel"
                      placeholder="用于查询申请进度"
                      :readonly="Boolean(resubmitContext) || portalLoggedIn"
                      :aria-invalid="Boolean(fieldError('contactPhone'))"
                      @blur="validateField('contactPhone')"
                      @input="clearError('contactPhone')"
                    >
                  </span>
                  <small v-if="fieldError('contactPhone')" class="field-error">{{ fieldError('contactPhone') }}</small>
                </label>
                <label class="field">
                  <span class="field-label">联系邮箱 <i>选填</i></span>
                  <span class="field-control">
                    <Message />
                    <input
                      v-model="form.contactEmail"
                      type="email"
                      maxlength="128"
                      autocomplete="email"
                      placeholder="用于接收重要通知"
                      :aria-invalid="Boolean(fieldError('contactEmail'))"
                      @blur="validateField('contactEmail')"
                      @input="clearError('contactEmail')"
                    >
                  </span>
                  <small v-if="fieldError('contactEmail')" class="field-error">{{ fieldError('contactEmail') }}</small>
                </label>
                <label class="field">
                  <span class="field-label">微信号 <i>选填</i></span>
                  <span class="field-control">
                    <ChatDotRound />
                    <input v-model="form.wechat" type="text" maxlength="64" autocomplete="off" placeholder="方便后续沟通即可">
                  </span>
                </label>
              </div>
            </section>

            <section v-else key="step-three" class="form-step" aria-labelledby="step-three-title">
              <div class="form-section-heading">
                <span class="form-section-index">03</span>
                <div>
                  <h3 id="step-three-title">说说您想办一场怎样的赛事</h3>
                  <p>不需要写得很正式，真实的想法足够让我们开始了解。</p>
                </div>
              </div>
              <div class="field-grid single-column">
                <label class="field">
                  <span class="field-label">办赛介绍 <i>选填</i></span>
                  <textarea
                    v-model="form.businessDescription"
                    maxlength="500"
                    rows="5"
                    placeholder="可以介绍过往办赛经验、赛事方向，或这次计划中的亮点。"
                  ></textarea>
                  <span class="character-count">{{ form.businessDescription.length }} / 500</span>
                </label>
                <fieldset class="field scale-field">
                  <span class="field-label">预计赛事规模 <i>选填</i></span>
                  <div class="scale-options">
                    <label v-for="option in scaleOptions" :key="option.value" :class="{ selected: form.expectedScale === option.value }">
                      <input v-model="form.expectedScale" type="radio" name="expectedScale" :value="option.value">
                      <span>{{ option.label }}</span>
                    </label>
                  </div>
                </fieldset>
              </div>
            </section>
          </Transition>

          <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
          <div class="form-actions">
            <button v-if="currentStep > 1" class="secondary-action" type="button" @click="previousStep">
              <ArrowLeft />
              上一步
            </button>
            <span v-else class="action-spacer"></span>
            <button v-if="currentStep < 3" class="primary-action" type="button" @click="nextStep">
              继续填写
              <ArrowRight />
            </button>
            <button v-else class="primary-action" type="submit" :disabled="isSubmitting">
              <Loading v-if="isSubmitting" class="spin-icon" />
              <Check v-else />
              {{ isSubmitting ? '提交中...' : resubmitContext ? '提交补充资料' : '提交入驻申请' }}
            </button>
          </div>
        </form>
      </section>
    </Transition>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import OrganizerApplicationLanding from './components/OrganizerApplicationLanding.vue'
import {
  ArrowLeft,
  ArrowRight,
  ChatDotRound,
  Check,
  CircleCheck,
  Clock,
  Close,
  CopyDocument,
  Document,
  DocumentAdd,
  Loading,
  Lock,
  Message,
  OfficeBuilding,
  Phone,
  Plus,
  Right,
  Search,
  UploadFilled,
  User,
  Warning,
} from '@element-plus/icons-vue'
import {
  fetchMyOrganizerApplications,
  fetchOrganizerApplicationStatus,
  resubmitMyOrganizerApplication,
  resubmitOrganizerApplication,
  submitMyOrganizerApplication,
  submitOrganizerApplication,
} from '@/api/organizerApplication'
import { fetchPortalProfile } from '@/api/portal'
import { isLoggedIn } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const steps = [
  { number: 1, title: '机构资料', description: '先认识您的机构' },
  { number: 2, title: '联系方式', description: '保持顺畅沟通' },
  { number: 3, title: '办赛计划', description: '分享您的想法' },
]

const scaleOptions = [
  { value: '50款以内', label: '50 款以内' },
  { value: '50-100款', label: '50 至 100 款' },
  { value: '100款以上', label: '100 款以上' },
]

const statusTimeline = [
  { key: 'SUBMITTED', label: '申请已提交', description: '资料已收到，等待平台处理' },
  { key: 'UNDER_REVIEW', label: '平台审核中', description: '我们正在审阅您的资料' },
  { key: 'NEED_MORE_INFO', label: '资料补充', description: '需要补充的，我们会在下面说明' },
  { key: 'APPROVED', label: '审核通过', description: '申请已通过，正在准备账号' },
  { key: 'ACCOUNT_ISSUED', label: '账号已开通', description: '主办方后台已经能进' },
]

const pageMode = ref(route.path.endsWith('/status') ? 'status' : 'apply')
const applicationOpen = ref(false)
const portalLoggedIn = computed(() => isLoggedIn('portal'))
const currentStep = ref(1)
const materialInput = ref(null)
const material = ref(null)
const isDragging = ref(false)
const isSubmitting = ref(false)
const isCopied = ref(false)
const credentialCopied = ref('')
const submittedApplication = ref(null)
const statusResult = ref(null)
const statusLoading = ref(false)
const statusQueryError = ref('')
const submitError = ref('')
const statusQueryPhone = ref('')
const resubmitContext = ref(null)
const myApplications = ref([])
const selectedApplicationNo = ref('')
const myApplicationsLoading = ref(false)
const myApplicationsError = ref('')
const showFallbackLookup = ref(false)
const portalProfile = ref(null)
const statusSource = ref('none')
const applyIntent = ref(false)
const preserveFallbackStatus = ref(false)
const showLanding = computed(() => (
  pageMode.value === 'apply'
  && !applicationOpen.value
  && !submittedApplication.value
  && !resubmitContext.value
))

const form = reactive(createEmptyForm())
const errors = reactive({})
const touched = reactive({})
const statusForm = reactive({ applicationNo: '', contactPhone: '' })
const statusErrors = reactive({})

const statusProgress = computed(() => {
  if (!statusResult.value) return -1
  const index = statusTimeline.findIndex((item) => item.key === statusResult.value.status)
  return index >= 0 ? index : 0
})

watch(
  () => route.path,
  (path) => {
    pageMode.value = path.endsWith('/status') ? 'status' : 'apply'
    if (pageMode.value === 'status') {
      applicationOpen.value = false
      const applicationNo = typeof route.query.applicationNo === 'string' ? route.query.applicationNo : ''
      if (applicationNo) statusForm.applicationNo = applicationNo
      if (portalLoggedIn.value) {
        if (preserveFallbackStatus.value) {
          preserveFallbackStatus.value = false
        } else {
          loadMyApplications()
        }
      }
    } else if (portalLoggedIn.value && !resubmitContext.value) {
      if (applyIntent.value) {
        applyIntent.value = false
        applicationOpen.value = true
        showFallbackLookup.value = false
        loadPortalProfile()
      } else {
        loadPortalProfile()
      }
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (portalLoggedIn.value) {
    loadPortalProfile()
  }
})

watch(portalLoggedIn, (loggedIn) => {
  if (!loggedIn) {
    myApplications.value = []
    selectedApplicationNo.value = ''
    portalProfile.value = null
    return
  }
  loadPortalProfile()
  if (pageMode.value === 'status') {
    loadMyApplications()
  }
})

function createEmptyForm() {
  return {
    organizationName: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    wechat: '',
    businessDescription: '',
    expectedScale: '',
    supplementalNote: '',
  }
}

function openApply() {
  applyIntent.value = true
  applicationOpen.value = true
  pageMode.value = 'apply'
  statusResult.value = null
  statusSource.value = 'none'
  if (route.path === '/portal/organizer-application') {
    applyIntent.value = false
    loadPortalProfile()
    return
  }
  router.push('/portal/organizer-application')
}

function closeApplication() {
  if (resubmitContext.value || isSubmitting.value) return
  applicationOpen.value = false
  currentStep.value = 1
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function fieldError(key) {
  return touched[key] ? errors[key] || '' : ''
}

function clearError(key) {
  delete errors[key]
}

function validateField(key) {
  touched[key] = true
  const value = String(form[key] || '').trim()
  let message = ''
  if (key === 'organizationName' && !value) message = '请填写公司或机构名称'
  if (key === 'contactName' && !value) message = '请填写联系人姓名'
  if (key === 'contactPhone' && !/^1\d{10}$/.test(value)) message = '请输入正确的 11 位手机号'
  if (key === 'contactEmail' && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) message = '请输入正确的邮箱地址'
  if (message) errors[key] = message
  else delete errors[key]
}

function validateStep(step) {
  const fields = step === 1
    ? ['organizationName']
    : step === 2
      ? ['contactName', 'contactPhone', 'contactEmail']
      : []
  fields.forEach(validateField)
  return fields.every((key) => !errors[key]) && !errors.material
}

function nextStep() {
  submitError.value = ''
  if (!validateStep(currentStep.value)) return
  currentStep.value = Math.min(3, currentStep.value + 1)
  focusStepHeading()
}

function previousStep() {
  submitError.value = ''
  currentStep.value = Math.max(1, currentStep.value - 1)
  focusStepHeading()
}

function goToStep(step) {
  if (step <= currentStep.value) {
    currentStep.value = step
    focusStepHeading()
  }
}

async function focusStepHeading() {
  await nextTick()
  document.querySelector('.form-step h3')?.focus?.()
}

function openFilePicker() {
  materialInput.value?.click()
}

function handleFileChange(event) {
  chooseMaterial(event.target.files?.[0])
  event.target.value = ''
}

function handleFileDrop(event) {
  isDragging.value = false
  chooseMaterial(event.dataTransfer.files?.[0])
}

function chooseMaterial(file) {
  if (!file) return
  const extension = file.name.split('.').pop()?.toLowerCase()
  const acceptedTypes = ['application/pdf', 'image/jpeg', 'image/png']
  const acceptedExtensions = ['pdf', 'jpg', 'jpeg', 'png']
  if (file.size > 10 * 1024 * 1024) {
    errors.material = '文件不能超过 10MB，请重新选择。'
    material.value = null
    return
  }
  if (!acceptedTypes.includes(file.type) && !acceptedExtensions.includes(extension)) {
    errors.material = '仅支持 PDF、JPG、PNG 文件。'
    material.value = null
    return
  }
  material.value = file
  delete errors.material
}

function removeMaterial() {
  material.value = null
  delete errors.material
}

function formatFileSize(bytes) {
  if (!bytes) return '0 KB'
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function buildPayload() {
  return Object.fromEntries(
    Object.entries(form).map(([key, value]) => [key, String(value || '').trim()]),
  )
}

async function submitApplication() {
  submitError.value = ''
  if (!validateStep(1) || !validateStep(2) || !validateStep(3)) {
    currentStep.value = errors.organizationName ? 1 : errors.contactName || errors.contactPhone || errors.contactEmail ? 2 : 3
    focusStepHeading()
    return
  }
  isSubmitting.value = true
  try {
    const payload = buildPayload()
    const authenticatedResubmit = Boolean(resubmitContext.value?.authenticated && portalLoggedIn.value)
    const result = resubmitContext.value
      ? authenticatedResubmit
        ? await resubmitMyOrganizerApplication(resubmitContext.value.applicationNo, payload, material.value)
        : await resubmitOrganizerApplication(resubmitContext.value.applicationNo, resubmitContext.value.contactPhone, payload, material.value)
      : portalLoggedIn.value
        ? await submitMyOrganizerApplication(payload, material.value)
        : await submitOrganizerApplication(payload, material.value)
    if (resubmitContext.value) {
      statusResult.value = result
      statusSource.value = authenticatedResubmit ? 'mine' : 'fallback'
      statusForm.applicationNo = resubmitContext.value.applicationNo
      statusQueryPhone.value = resubmitContext.value.contactPhone
      resubmitContext.value = null
      if (!authenticatedResubmit && portalLoggedIn.value) {
        preserveFallbackStatus.value = true
      }
      await router.replace({ path: '/portal/organizer-application/status', query: { applicationNo: statusForm.applicationNo } })
      if (authenticatedResubmit) {
        await loadMyApplications()
      }
      return
    }
    if (portalLoggedIn.value) {
      await router.replace({ path: '/portal/organizer-application/status', query: { applicationNo: result.applicationNo } })
      await loadMyApplications()
      return
    }
    submittedApplication.value = result
    isCopied.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    submitError.value = error?.userMessage || error?.message || '提交失败，请稍后重试。'
  } finally {
    isSubmitting.value = false
  }
}

function validateStatusField(key) {
  const value = String(statusForm[key] || '').trim()
  let message = ''
  if (key === 'applicationNo' && !value) message = '请输入申请编号'
  if (key === 'contactPhone' && !/^1\d{10}$/.test(value)) message = '请输入正确的 11 位手机号'
  if (message) statusErrors[key] = message
  else delete statusErrors[key]
}

function clearStatusError(key) {
  delete statusErrors[key]
  statusQueryError.value = ''
}

async function queryStatus() {
  validateStatusField('applicationNo')
  validateStatusField('contactPhone')
  if (Object.keys(statusErrors).length) return
  statusLoading.value = true
  statusQueryError.value = ''
  try {
    statusResult.value = await fetchOrganizerApplicationStatus(statusForm.applicationNo, statusForm.contactPhone)
    statusSource.value = 'fallback'
    statusQueryPhone.value = statusForm.contactPhone
  } catch (error) {
    statusResult.value = null
    statusQueryError.value = error?.userMessage || error?.message || '没有找到匹配的申请，请核对申请编号和手机号。'
  } finally {
    statusLoading.value = false
  }
}

function startResubmit() {
  if (!statusResult.value) return
  const currentPhone = portalLoggedIn.value
    ? statusSource.value === 'mine'
      ? portalProfile.value?.phone || statusQueryPhone.value || statusForm.contactPhone || ''
      : statusQueryPhone.value || statusForm.contactPhone || ''
    : statusQueryPhone.value || ''
  Object.assign(form, createEmptyForm(), {
    organizationName: statusResult.value.organizationName || '',
    contactName: statusResult.value.contactName || '',
    contactPhone: currentPhone,
  })
  Object.keys(errors).forEach((key) => delete errors[key])
  Object.keys(touched).forEach((key) => delete touched[key])
  material.value = null
  resubmitContext.value = {
    applicationNo: statusResult.value.applicationNo,
    contactPhone: currentPhone,
    authenticated: portalLoggedIn.value && statusSource.value === 'mine',
  }
  applyIntent.value = true
  applicationOpen.value = true
  currentStep.value = 1
  pageMode.value = 'apply'
  router.replace('/portal/organizer-application')
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function startNewApplication() {
  Object.assign(form, createEmptyForm())
  Object.keys(errors).forEach((key) => delete errors[key])
  Object.keys(touched).forEach((key) => delete touched[key])
  material.value = null
  currentStep.value = 1
  submittedApplication.value = null
  resubmitContext.value = null
  statusSource.value = 'none'
  applyIntent.value = true
  applicationOpen.value = true
  isCopied.value = false
  if (portalLoggedIn.value) {
    loadPortalProfile()
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function copyApplicationNo() {
  const applicationNo = submittedApplication.value?.applicationNo
  if (!applicationNo) return
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(applicationNo).catch(() => {})
  } else {
    const input = document.createElement('textarea')
    input.value = applicationNo
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    input.remove()
  }
  isCopied.value = true
}

function copyCredential(value, field) {
  if (!value) return
  const markCopied = () => {
    credentialCopied.value = field
    window.setTimeout(() => {
      if (credentialCopied.value === field) credentialCopied.value = ''
    }, 1800)
  }
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(value).then(markCopied).catch(() => {})
    return
  }
  const input = document.createElement('textarea')
  input.value = value
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  input.remove()
  markCopied()
}

function statusTone(status) {
  if (status === 'ACCOUNT_ISSUED' || status === 'APPROVED') return 'tone-success'
  if (status === 'REJECTED') return 'tone-danger'
  if (status === 'NEED_MORE_INFO') return 'tone-warning'
  return 'tone-neutral'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

async function loadPortalProfile() {
  if (!portalLoggedIn.value) return
  try {
    portalProfile.value = await fetchPortalProfile()
    if (pageMode.value === 'apply' && !resubmitContext.value) {
      form.organizationName = portalProfile.value?.companyName || portalProfile.value?.displayName || ''
      form.contactName = portalProfile.value?.contactName || ''
      form.contactPhone = portalProfile.value?.phone || ''
      form.wechat = portalProfile.value?.wechat || ''
    }
    if (portalProfile.value?.phone && !statusForm.contactPhone) {
      statusForm.contactPhone = portalProfile.value.phone
    }
  } catch {
    portalProfile.value = null
  }
}

async function loadMyApplications(redirectToStatus = false) {
  if (!portalLoggedIn.value) return
  myApplicationsLoading.value = true
  myApplicationsError.value = ''
  try {
    const applications = await fetchMyOrganizerApplications()
    myApplications.value = Array.isArray(applications) ? applications : []
    const queryApplicationNo = typeof route.query.applicationNo === 'string' ? route.query.applicationNo : ''
    const preferred = myApplications.value.find((item) => item.applicationNo === queryApplicationNo)
      || myApplications.value[0]
    selectedApplicationNo.value = preferred?.applicationNo || ''
    statusResult.value = preferred || null
    statusSource.value = preferred ? 'mine' : 'none'
    if (preferred?.applicationNo) {
      statusForm.applicationNo = preferred.applicationNo
      if (redirectToStatus && pageMode.value === 'apply') {
        await router.replace({ path: '/portal/organizer-application/status', query: { applicationNo: preferred.applicationNo } })
      }
    }
  } catch (error) {
    myApplications.value = []
    statusResult.value = null
    statusSource.value = 'none'
    myApplicationsError.value = error?.userMessage || error?.message || '暂时无法读取申请进度，请使用下方查询方式。'
  } finally {
    myApplicationsLoading.value = false
  }
}

function selectMyApplication(applicationNo) {
  const selected = myApplications.value.find((item) => item.applicationNo === applicationNo)
  if (!selected) return
  selectedApplicationNo.value = applicationNo
  statusResult.value = selected
  statusSource.value = 'mine'
  statusForm.applicationNo = applicationNo
  router.replace({ path: '/portal/organizer-application/status', query: { applicationNo } })
}
</script>

<style scoped>
.organizer-application-page {
  --ink: #211912;
  --muted: #746a5f;
  --paper: #fffaf0;
  --foam: #fff6df;
  --amber: #d89021;
  --amber-deep: #8b5c19;
  --green: #3d7d50;
  --line: rgba(87, 58, 26, 0.14);
  color: var(--ink);
}

.workspace-open {
  min-height: calc(100svh - 72px);
  padding: 0 max(3vw, 24px) 72px;
  color: var(--ink);
  background:
    linear-gradient(90deg, rgba(97, 69, 34, 0.04) 1px, transparent 1px),
    #f3eee4;
  background-size: 30px 30px;
}

.workspace-open .application-hero,
.workspace-open .application-intro,
.workspace-open .application-layout,
.workspace-open .status-layout,
.workspace-open .success-layout {
  width: min(1560px, 100%);
  margin-inline: auto;
}

.experience-back {
  order: -1;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 42px;
  padding: 0 15px;
  color: #2d281f;
  background: transparent;
  border: 1px solid rgba(45, 40, 31, 0.24);
  border-radius: 4px;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.experience-back svg {
  width: 16px;
  height: 16px;
}

.experience-back:hover {
  background: #fffaf0;
  border-color: rgba(45, 40, 31, 0.42);
}

.application-hero {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  min-height: 430px;
  overflow: hidden;
  padding: 54px 58px;
  color: #fff8e8;
  background: #1b130e;
  border-radius: 8px;
  box-shadow: 0 28px 70px rgba(67, 43, 17, 0.18);
}

.hero-media,
.hero-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-media {
  z-index: -2;
  background: url('/hero-beer.png') center 58% / cover no-repeat;
  animation: hero-drift 18s ease-in-out infinite alternate;
}

.hero-overlay {
  z-index: -1;
  background: rgba(22, 14, 9, 0.7);
}

.hero-content {
  max-width: 720px;
  animation: rise-in 680ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-kicker {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: #efca7a;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.kicker-dot {
  width: 4px;
  height: 4px;
  background: #efca7a;
  border-radius: 50%;
}

.hero-content h1 {
  max-width: 720px;
  margin: 20px 0 16px;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 58px;
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: 0;
}

.hero-content h1 em {
  color: #f3d978;
  font-style: normal;
}

.hero-content p {
  max-width: 590px;
  margin: 0;
  color: rgba(255, 248, 232, 0.82);
  font-size: 16px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
}

.hero-primary-action,
.hero-status-link,
.primary-action,
.secondary-action,
.side-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 17px;
  border-radius: 8px;
  font: inherit;
  font-size: 14px;
  font-weight: 900;
  line-height: 1.2;
  text-decoration: none;
  cursor: pointer;
  transition: transform 180ms ease, background 180ms ease, border-color 180ms ease, color 180ms ease;
}

.hero-primary-action {
  color: #2b1d10;
  background: #f3d978;
  border: 1px solid rgba(255, 244, 194, 0.42);
}

.hero-primary-action:hover,
.hero-primary-action:focus-visible {
  background: #ffe9a5;
  transform: translateY(-2px);
}

.hero-status-link {
  min-height: 42px;
  padding-inline: 0;
  color: #fff8e8;
  border: 1px solid transparent;
}

.hero-status-link:hover,
.hero-status-link:focus-visible {
  color: #f3d978;
  border-bottom-color: #f3d978;
  border-radius: 0;
}

.hero-primary-action svg,
.hero-status-link svg,
.primary-action svg,
.secondary-action svg,
.side-link svg {
  width: 18px;
  height: 18px;
}

.hero-aside {
  width: min(260px, 28%);
  padding: 2px 0 2px 28px;
  border-left: 1px solid rgba(255, 248, 232, 0.36);
  animation: rise-in 680ms 160ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-aside-label,
.result-eyebrow,
.section-eyebrow {
  color: var(--amber-deep);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.hero-aside-label,
.result-eyebrow {
  color: #efca7a;
}

.hero-aside strong {
  display: block;
  margin-top: 14px;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 25px;
  line-height: 1.25;
}

.hero-aside-rule {
  display: block;
  width: 54px;
  height: 1px;
  margin: 20px 0 16px;
  background: #efca7a;
}

.hero-aside p {
  margin: 0;
  color: rgba(255, 248, 232, 0.72);
  font-size: 13px;
  line-height: 1.7;
}

.application-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  padding: 44px 4px 28px;
}

.workspace-open .application-intro {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) minmax(300px, auto) auto;
  align-items: end;
}

.application-intro h2 {
  margin: 9px 0 0;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30px;
  line-height: 1.25;
}

.application-intro > p {
  max-width: 390px;
  margin: 0 0 2px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
  text-align: right;
}

.application-layout,
.status-layout {
  display: grid;
  grid-template-columns: 224px minmax(0, 1fr);
  gap: 38px;
  align-items: start;
}

.steps-side,
.status-side {
  position: sticky;
  top: 98px;
}

.steps-intro,
.status-side {
  padding: 4px 0 24px;
}

.side-number {
  display: block;
  color: var(--amber-deep);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.steps-intro h3,
.status-side h3 {
  margin: 10px 0 10px;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24px;
  line-height: 1.25;
}

.steps-intro p,
.status-side p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.75;
}

.step-list {
  display: grid;
  gap: 5px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.step-list li {
  position: relative;
}

.step-list li::before {
  position: absolute;
  top: 44px;
  bottom: -6px;
  left: 16px;
  width: 1px;
  content: '';
  background: var(--line);
}

.step-list li:last-child::before {
  display: none;
}

.step-list button {
  display: flex;
  align-items: flex-start;
  width: 100%;
  gap: 12px;
  padding: 9px 4px;
  color: var(--muted);
  background: transparent;
  border: 0;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.step-list button:disabled {
  cursor: default;
}

.step-marker,
.timeline-marker {
  position: relative;
  z-index: 1;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 33px;
  height: 33px;
  color: var(--muted);
  background: #f5ead3;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 50%;
  font-size: 10px;
  font-weight: 900;
}

.step-list li.active .step-marker,
.step-list li.reached:not(:first-child) .step-marker {
  color: #2b1d10;
  background: #f3d978;
  border-color: rgba(184, 117, 23, 0.32);
}

.step-list li.active .step-copy strong {
  color: var(--ink);
}

.step-copy {
  display: grid;
  gap: 3px;
  padding-top: 2px;
}

.step-copy strong {
  color: #4f4335;
  font-size: 14px;
  line-height: 1.3;
}

.step-copy small {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.4;
}

.privacy-note {
  display: flex;
  gap: 8px;
  margin-top: 27px;
  padding-top: 16px;
  color: #8c7d6c;
  border-top: 1px dashed rgba(87, 58, 26, 0.18);
  font-size: 11px;
  line-height: 1.6;
}

.privacy-note svg {
  flex: 0 0 auto;
  width: 15px;
  height: 15px;
  margin-top: 1px;
  color: var(--green);
}

.form-panel,
.status-lookup-form,
.status-result {
  padding: 30px 34px;
  background: rgba(255, 250, 240, 0.92);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 18px 46px rgba(67, 43, 17, 0.08);
}

.form-panel-topline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  color: #9a805e;
  border-bottom: 1px solid var(--line);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.form-step {
  min-height: 410px;
  padding-top: 28px;
}

.form-section-heading {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  margin-bottom: 28px;
}

.compact-heading {
  margin-bottom: 26px;
}

.form-section-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  color: #6b4710;
  background: #f3d978;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 900;
}

.form-section-heading h3 {
  margin: 1px 0 7px;
  color: var(--ink);
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 24px;
  line-height: 1.3;
}

.form-section-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.65;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px 18px;
}

.field-grid.single-column {
  grid-template-columns: minmax(0, 1fr);
}

.field-grid-status {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-span-two {
  grid-column: 1 / -1;
}

.field {
  position: relative;
  display: grid;
  align-content: start;
  gap: 9px;
  min-width: 0;
}

fieldset.field {
  margin: 0;
  padding: 0;
  border: 0;
}

.scale-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.scale-options label {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 52px;
  padding: 8px 12px;
  color: #6c5e4e;
  background: rgba(255, 252, 245, 0.72);
  border: 1px solid rgba(87, 58, 26, 0.17);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: color 180ms ease, background 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.scale-options label:hover,
.scale-options label.selected {
  color: #2b2117;
  background: #fff0c2;
  border-color: rgba(184, 117, 23, 0.58);
}

.scale-options label:hover {
  transform: translateY(-2px);
}

.scale-options input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.scale-options label:has(input:focus-visible) {
  outline: 3px solid rgba(216, 144, 33, 0.22);
  outline-offset: 2px;
}

.field-label {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
  color: #45382b;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.3;
}

.field-label b {
  color: #b66332;
  font-size: 13px;
}

.field-label i {
  color: #9b8972;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}

.field-control {
  display: flex;
  align-items: center;
  min-height: 48px;
  padding: 0 13px;
  background: rgba(255, 252, 245, 0.82);
  border: 1px solid rgba(87, 58, 26, 0.17);
  border-radius: 6px;
  transition: border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.field-control:focus-within {
  background: #fffdf8;
  border-color: rgba(184, 117, 23, 0.72);
  box-shadow: 0 0 0 3px rgba(216, 144, 33, 0.13);
}

.field-control svg {
  flex: 0 0 auto;
  width: 17px;
  height: 17px;
  margin-right: 9px;
  color: #a67a42;
}

.field-control input,
.field textarea {
  width: 100%;
  min-width: 0;
  color: var(--ink);
  background: transparent;
  border: 0;
  outline: 0;
  font: inherit;
  font-size: 14px;
}

.field-control input {
  height: 46px;
}

.field-control input::placeholder,
.field textarea::placeholder {
  color: #b8a895;
}

.field-control input[readonly] {
  color: #7d6a55;
  cursor: not-allowed;
}

.field textarea {
  min-height: 122px;
  padding: 13px 14px;
  resize: vertical;
  background: rgba(255, 252, 245, 0.82);
  border: 1px solid rgba(87, 58, 26, 0.17);
  border-radius: 6px;
  line-height: 1.65;
  transition: border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.field textarea:focus {
  background: #fffdf8;
  border-color: rgba(184, 117, 23, 0.72);
  box-shadow: 0 0 0 3px rgba(216, 144, 33, 0.13);
  outline: 0;
}

.character-count {
  align-self: end;
  margin-top: -4px;
  color: #a99680;
  font-size: 11px;
  text-align: right;
}

.field-error,
.submit-error {
  color: #ae4e34;
  font-size: 12px;
  line-height: 1.5;
}

.field-error {
  margin-top: -3px;
}

.submit-error {
  margin: 20px 0 0;
  padding: 11px 13px;
  background: #fff0e8;
  border: 1px solid rgba(174, 78, 52, 0.2);
  border-radius: 6px;
}

.upload-block {
  margin-top: 30px;
  padding-top: 24px;
  border-top: 1px dashed rgba(87, 58, 26, 0.2);
}

.upload-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 12px;
}

.upload-heading p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.upload-limit {
  flex: 0 0 auto;
  color: #9b8972;
  font-size: 11px;
  font-weight: 800;
}

.upload-zone {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 92px;
  padding: 16px 17px;
  color: #78624a;
  background: rgba(255, 247, 230, 0.68);
  border: 1px dashed rgba(184, 117, 23, 0.42);
  border-radius: 7px;
  cursor: pointer;
  transition: background 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.upload-zone:hover,
.upload-zone:focus-visible,
.upload-zone.is-dragging {
  background: #fff0c2;
  border-color: var(--amber);
  outline: 0;
}

.upload-zone.is-dragging {
  transform: translateY(-2px);
}

.upload-zone.has-file {
  cursor: default;
}

.upload-icon,
.file-icon {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 42px;
  height: 42px;
  color: #8b5c19;
  background: #f3d978;
  border-radius: 50%;
}

.upload-icon svg,
.file-icon svg {
  width: 21px;
  height: 21px;
}

.upload-copy,
.file-info {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.upload-copy strong,
.file-info strong {
  overflow: hidden;
  color: #523c26;
  font-size: 13px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-copy small,
.file-info small {
  color: #967d62;
  font-size: 11px;
  line-height: 1.4;
}

.upload-arrow {
  width: 18px;
  height: 18px;
  margin-left: auto;
  color: #9d773b;
}

.file-remove {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 34px;
  height: 34px;
  margin-left: auto;
  color: #7d5b37;
  background: transparent;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 50%;
  cursor: pointer;
}

.file-remove:hover,
.file-remove:focus-visible {
  color: #ae4e34;
  border-color: rgba(174, 78, 52, 0.32);
  outline: 0;
}

.file-remove svg {
  width: 16px;
  height: 16px;
}

.form-actions,
.success-actions,
.status-result-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.form-actions {
  min-height: 44px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.action-spacer {
  width: 1px;
}

.primary-action {
  color: #2b1d10;
  background: #e1a23d;
  border: 1px solid rgba(184, 117, 23, 0.26);
}

.primary-action:hover,
.primary-action:focus-visible {
  background: #f0bc62;
  transform: translateY(-1px);
  outline: 0;
}

.primary-action:disabled {
  cursor: wait;
  opacity: 0.62;
  transform: none;
}

.secondary-action {
  color: #725632;
  background: transparent;
  border: 1px solid rgba(87, 58, 26, 0.22);
}

.secondary-action:hover,
.secondary-action:focus-visible {
  color: var(--ink);
  background: #fff4d7;
  border-color: rgba(184, 117, 23, 0.38);
  outline: 0;
}

.spin-icon {
  animation: spin 900ms linear infinite;
}

.status-main {
  display: grid;
  gap: 20px;
}

.mine-status-toolbar {
  display: grid;
  gap: 14px;
  padding-bottom: 17px;
  border-bottom: 1px solid var(--line);
}

.mine-status-heading {
  display: grid;
  gap: 6px;
}

.mine-status-heading strong {
  color: #493522;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 20px;
}

.application-switcher {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.application-switcher button {
  display: grid;
  flex: 0 0 min(220px, 70vw);
  gap: 5px;
  min-height: 58px;
  padding: 10px 12px;
  color: #725632;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 7px;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.application-switcher button:hover,
.application-switcher button.active {
  color: #2b1d10;
  background: #fff0c2;
  border-color: rgba(184, 117, 23, 0.38);
}

.application-switcher button span,
.application-switcher button small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-switcher button span {
  font-size: 13px;
  font-weight: 900;
}

.application-switcher button small {
  color: #967d62;
  font-size: 11px;
}

.status-loading,
.mine-status-empty {
  display: grid;
  justify-items: start;
  gap: 11px;
  padding: 28px 0;
  color: var(--muted);
}

.status-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.mine-status-empty > svg {
  width: 30px;
  height: 30px;
  color: var(--amber-deep);
}

.mine-status-empty strong {
  color: #493522;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 20px;
}

.fallback-lookup-trigger {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  gap: 7px;
  min-height: 34px;
  padding: 0 4px;
  color: #8b5c19;
  background: transparent;
  border: 0;
  border-bottom: 1px solid transparent;
  font: inherit;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.fallback-lookup-trigger:hover,
.fallback-lookup-trigger:focus-visible {
  color: #5c3c14;
  border-bottom-color: #8b5c19;
  outline: 0;
}

.fallback-lookup-trigger svg {
  width: 16px;
  height: 16px;
}

.status-lookup-form {
  padding-bottom: 25px;
}

.lookup-actions {
  justify-content: flex-end;
  margin-top: 18px;
  padding-top: 17px;
}

.status-result {
  padding-bottom: 25px;
}

.issued-account-panel {
  display: grid;
  gap: 16px;
  margin-top: 18px;
  padding: 15px 16px;
  background: #edf5e6;
  border: 1px solid rgba(61, 125, 80, 0.2);
  border-radius: 7px;
}

.issued-account-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.issued-account-heading > div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.issued-account-panel span {
  color: #58725a;
  font-size: 11px;
  font-weight: 800;
}

.issued-account-state {
  flex: 0 0 auto;
  padding: 5px 8px;
  color: #2f6b3f !important;
  background: rgba(61, 125, 80, 0.1);
  border-radius: 999px;
  font-size: 10px !important;
  letter-spacing: 0.08em;
}

.issued-account-panel strong {
  overflow: hidden;
  color: #245635;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issued-credentials {
  display: grid;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px dashed rgba(61, 125, 80, 0.24);
}

.issued-credential-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.issued-credential-row > span {
  flex: 0 0 58px;
}

.issued-credential-row code {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #245635;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.issued-credential-row button {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 30px;
  height: 30px;
  color: #58725a;
  background: transparent;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
}

.issued-credential-row button:hover,
.issued-credential-row button:focus-visible {
  color: #245635;
  background: rgba(61, 125, 80, 0.1);
  outline: 0;
}

.issued-credential-row button svg {
  width: 15px;
  height: 15px;
}

.issued-credentials p,
.issued-credential-note {
  margin: 0;
  color: #58725a;
  font-size: 11px;
  line-height: 1.6;
}

.issued-account-panel .primary-action {
  justify-self: start;
  color: #fff8e8;
  background: var(--green);
  border-color: var(--green);
}

.issued-account-panel .primary-action:hover,
.issued-account-panel .primary-action:focus-visible {
  background: #306740;
}

@media (max-width: 620px) {
  .issued-account-heading {
    align-items: flex-start;
  }

  .issued-account-panel .primary-action {
    width: 100%;
  }
}

.status-result-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 21px;
  border-bottom: 1px solid var(--line);
}

.status-result-topline h3 {
  margin: 8px 0 5px;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 23px;
  line-height: 1.3;
}

.status-result-topline p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.status-badge svg {
  width: 15px;
  height: 15px;
}

.tone-success {
  color: #1f5a34;
  background: #cfead2;
}

.tone-danger {
  color: #9a432e;
  background: #f8d9ca;
}

.tone-warning {
  color: #80530c;
  background: #f8e4ad;
}

.tone-neutral {
  color: #5e5a4f;
  background: #e9e1d5;
}

.status-timeline {
  display: grid;
  gap: 0;
  margin: 24px 0 0;
  padding: 0;
  list-style: none;
}

.status-timeline li {
  position: relative;
  display: flex;
  gap: 13px;
  min-height: 61px;
}

.status-timeline li::before {
  position: absolute;
  top: 34px;
  bottom: 0;
  left: 16px;
  width: 1px;
  content: '';
  background: var(--line);
}

.status-timeline li:last-child::before {
  display: none;
}

.status-timeline li.complete::before {
  background: rgba(61, 125, 80, 0.44);
}

.timeline-marker {
  width: 33px;
  height: 33px;
  color: #9c8c78;
  background: #f3eadc;
}

.timeline-marker svg {
  width: 15px;
  height: 15px;
}

.status-timeline li.complete .timeline-marker {
  color: #fff8e8;
  background: var(--green);
  border-color: var(--green);
}

.status-timeline li.current .timeline-marker {
  color: #2b1d10;
  background: #f3d978;
  border-color: rgba(184, 117, 23, 0.4);
  box-shadow: 0 0 0 4px rgba(243, 217, 120, 0.28);
}

.timeline-copy {
  display: grid;
  align-content: start;
  gap: 4px;
  padding-top: 2px;
}

.timeline-copy strong {
  color: #5d4c38;
  font-size: 13px;
  line-height: 1.3;
}

.timeline-copy small {
  color: #9b8972;
  font-size: 11px;
  line-height: 1.5;
}

.review-note {
  margin-top: 10px;
  padding: 14px 15px;
  background: #fff4d7;
  border-left: 3px solid #e1a23d;
}

.review-note span {
  color: #8a631f;
  font-size: 11px;
  font-weight: 900;
}

.review-note p {
  margin: 7px 0 0;
  color: #5f4c33;
  font-size: 13px;
  line-height: 1.65;
}

.status-result-footer {
  flex-wrap: wrap;
  margin-top: 22px;
  padding-top: 17px;
  color: #9b8972;
  border-top: 1px dashed rgba(87, 58, 26, 0.18);
  font-size: 11px;
}

.status-result-footer .primary-action {
  margin-left: auto;
}

.side-link {
  justify-content: flex-start;
  margin-top: 22px;
  padding: 0;
  color: #8b5c19;
  border: 0;
  border-bottom: 1px solid transparent;
  border-radius: 0;
}

.side-link:hover,
.side-link:focus-visible {
  color: #5c3c14;
  border-bottom-color: #8b5c19;
  transform: none;
  outline: 0;
}

.success-layout {
  display: grid;
  justify-items: center;
  max-width: 760px;
  margin: 12px auto 34px;
  padding: 52px 48px 48px;
  text-align: center;
  background: rgba(255, 250, 240, 0.92);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 18px 46px rgba(67, 43, 17, 0.08);
}

.success-mark {
  display: grid;
  place-items: center;
  width: 68px;
  height: 68px;
  color: #fff8e8;
  background: var(--green);
  border: 8px solid #dcebd7;
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgba(61, 125, 80, 0.18);
  animation: success-pop 620ms cubic-bezier(0.22, 1, 0.36, 1) both;
}

.success-mark svg {
  width: 28px;
  height: 28px;
}

.success-copy {
  margin-top: 25px;
}

.success-copy h2 {
  margin: 10px 0 10px;
  font-family: 'Noto Serif SC', 'Songti SC', Georgia, serif;
  font-size: 30px;
  line-height: 1.3;
}

.success-copy p {
  max-width: 520px;
  margin: 0 auto;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.75;
}

.application-number-block {
  width: min(430px, 100%);
  margin-top: 30px;
  padding: 17px 19px;
  text-align: left;
  background: #fff4d7;
  border: 1px solid rgba(184, 117, 23, 0.2);
  border-radius: 7px;
}

.application-number-block > span {
  color: #96723a;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.application-number-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.application-number-row code {
  overflow: hidden;
  color: #4f3516;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 16px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-number-row button {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #7b551f;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.18);
  border-radius: 50%;
  cursor: pointer;
}

.application-number-row button:hover,
.application-number-row button:focus-visible {
  color: #2b1d10;
  border-color: rgba(184, 117, 23, 0.5);
  outline: 0;
}

.application-number-row svg {
  width: 16px;
  height: 16px;
}

.application-number-block small {
  display: block;
  margin-top: 7px;
  color: #9b8972;
  font-size: 11px;
}

.success-actions {
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 27px;
}

.stage-enter-active,
.stage-leave-active,
.step-content-enter-active,
.step-content-leave-active,
.result-reveal-enter-active,
.result-reveal-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}

.stage-enter-from,
.stage-leave-to,
.step-content-enter-from,
.step-content-leave-to,
.result-reveal-enter-from,
.result-reveal-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@keyframes hero-drift {
  from { transform: scale(1); }
  to { transform: scale(1.045); }
}

@keyframes rise-in {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes success-pop {
  0% { opacity: 0; transform: scale(0.7) rotate(-8deg); }
  70% { opacity: 1; transform: scale(1.06) rotate(2deg); }
  100% { opacity: 1; transform: scale(1) rotate(0); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1040px) {
  .application-hero {
    padding: 46px 42px;
  }

  .hero-content h1 {
    font-size: 50px;
  }

  .hero-aside {
    width: 25%;
    padding-left: 22px;
  }

  .application-layout,
  .status-layout {
    gap: 28px;
  }
}

@media (max-width: 820px) {
  .workspace-open {
    padding-inline: 20px;
  }

  .workspace-open .application-intro {
    grid-template-columns: 1fr auto;
  }

  .workspace-open .application-intro > p {
    grid-column: 1 / -1;
    text-align: left;
  }

  .application-hero {
    display: block;
    min-height: 470px;
    padding: 42px 32px;
  }

  .hero-content h1 {
    font-size: 48px;
  }

  .hero-aside {
    width: min(310px, 100%);
    margin-top: 44px;
  }

  .application-layout,
  .status-layout {
    grid-template-columns: 1fr;
  }

  .steps-side,
  .status-side {
    position: static;
  }

  .steps-intro,
  .status-side {
    display: flex;
    align-items: baseline;
    gap: 14px;
    padding-bottom: 15px;
  }

  .steps-intro p,
  .status-side p {
    max-width: 540px;
  }

  .steps-intro h3,
  .status-side h3 {
    margin: 0;
  }

  .step-list {
    display: flex;
    gap: 6px;
    padding-bottom: 15px;
    overflow-x: auto;
    border-bottom: 1px solid var(--line);
  }

  .step-list li {
    flex: 1 0 150px;
  }

  .step-list li::before {
    top: 25px;
    right: -5px;
    bottom: auto;
    left: auto;
    width: 10px;
    height: 1px;
  }

  .step-list button {
    align-items: center;
    gap: 8px;
    padding: 4px;
  }

  .step-copy small {
    display: none;
  }

  .privacy-note {
    display: none;
  }

  .side-link {
    margin: 0 0 0 auto;
  }
}

@media (max-width: 620px) {
  .workspace-open {
    min-height: calc(100svh - 60px);
    padding: 0 14px 44px;
  }

  .workspace-open .application-intro {
    grid-template-columns: 1fr;
  }

  .experience-back {
    width: fit-content;
  }

  .scale-options {
    grid-template-columns: 1fr;
  }

  .application-hero {
    min-height: 520px;
    padding: 34px 23px;
    border-radius: 7px;
  }

  .hero-content h1 {
    margin-top: 18px;
    font-size: 40px;
  }

  .hero-content p {
    font-size: 14px;
  }

  .hero-aside {
    margin-top: 36px;
    padding-left: 18px;
  }

  .application-intro {
    display: grid;
    gap: 12px;
    padding: 28px 2px 18px;
  }

  .application-intro h2 {
    font-size: 26px;
  }

  .application-intro > p {
    margin: 0;
    text-align: left;
  }

  .form-panel,
  .status-lookup-form,
  .status-result {
    padding: 23px 18px;
  }

  .form-panel-topline {
    font-size: 10px;
  }

  .form-step {
    min-height: 0;
    padding-top: 23px;
  }

  .form-section-heading {
    gap: 11px;
    margin-bottom: 23px;
  }

  .form-section-heading h3 {
    font-size: 21px;
  }

  .field-grid,
  .field-grid-status {
    grid-template-columns: 1fr;
    gap: 17px;
  }

  .upload-heading {
    display: grid;
    gap: 6px;
  }

  .upload-limit {
    font-size: 10px;
  }

  .upload-zone {
    min-height: 86px;
    padding: 13px;
  }

  .upload-copy strong,
  .file-info strong {
    white-space: normal;
  }

  .form-actions,
  .success-actions {
    flex-wrap: wrap;
  }

  .form-actions .primary-action,
  .form-actions .secondary-action {
    flex: 1 1 auto;
  }

  .status-result-topline {
    display: grid;
    gap: 14px;
  }

  .status-badge {
    justify-self: start;
  }

  .status-result-footer {
    display: grid;
    justify-content: stretch;
  }

  .status-result-footer .primary-action {
    margin-left: 0;
  }

  .success-layout {
    padding: 39px 20px 34px;
  }

  .success-copy h2 {
    font-size: 26px;
  }

  .success-actions .primary-action,
  .success-actions .secondary-action {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-media,
  .hero-content,
  .hero-aside,
  .success-mark,
  .spin-icon {
    animation: none;
  }

  .stage-enter-active,
  .stage-leave-active,
  .step-content-enter-active,
  .step-content-leave-active,
  .result-reveal-enter-active,
  .result-reveal-leave-active,
  .hero-primary-action,
  .primary-action,
  .secondary-action,
  .upload-zone {
    transition: none;
  }
}
</style>
