<template>
  <div class="notification-page">
    <div class="workspace-top">
      <AdminPageHeader title="邮件通知">
        <template #actions><button class="button icon" title="刷新" aria-label="刷新" :disabled="busy" @click="refresh"><Refresh /></button></template>
      </AdminPageHeader>
      <div class="competition-bar">
        <label>赛事 <select :value="competitionId" :disabled="busy" @change="switchCompetition($event)"><option v-for="item in competitions" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
        <div v-if="config" class="summary-strip">
          <span>通知厂商 <strong>{{ config.stats.recipientCount }}</strong></span>
          <span>已填写邮箱 <strong class="green">{{ config.stats.emailRecipientCount }}</strong></span>
          <span>待发送 <strong>{{ config.stats.pendingCount }}</strong></span>
          <span>已发送 <strong>{{ config.stats.sentCount }}</strong></span>
          <span>失败 <strong :class="{ red: config.stats.failedCount }">{{ config.stats.failedCount }}</strong></span>
        </div>
      </div>
      <nav class="view-tabs" aria-label="邮件通知视图">
        <button v-for="item in views" :key="item.key" :class="{ active: view === item.key }" :aria-current="view === item.key ? 'page' : undefined" @click="view = item.key"><component :is="item.icon" />{{ item.label }}</button>
      </nav>
    </div>

    <div v-if="loading" class="empty-state" role="status">正在加载邮件通知…</div>
    <div v-else-if="error" class="empty-state" role="alert">{{ error }}<button class="button" @click="loadConfiguration">重新加载</button></div>
    <div v-else-if="!config" class="empty-state">暂无可管理的赛事</div>
    <template v-else>
      <section v-show="view === 'schedule'" class="scroll-area schedule-view">
        <div class="section-tools"><span class="muted">收样：{{ formatTime(config.sampleArrivalStart) }} 至 {{ formatTime(config.sampleArrivalDeadline) }}</span><span v-if="rulesDirty" class="gold">有未保存的安排</span></div>
        <article v-for="rule in rules" :key="rule.eventCode" class="schedule-item" :class="{ inactive: !rule.enabled }">
          <div class="event-name"><span class="event-icon"><component :is="rule.eventCode === 'RESULT_PUBLISHED' ? Trophy : Message" /></span><div><strong>{{ rule.eventLabel }}</strong><span class="muted">{{ triggerLabel(rule) }}</span></div></div>
          <div class="schedule-controls">
            <label>发送方式<select v-model="rule.scheduleMode" :disabled="busy"><option v-if="rule.eventCode !== 'RESULT_PUBLISHED'" value="RELATIVE">按赛事时间</option><option v-else value="AFTER_EVENT">结果发布后</option><option value="FIXED">指定日期</option></select></label>
            <template v-if="rule.scheduleMode === 'RELATIVE'"><label>{{ rule.eventCode === 'SAMPLE_START' ? '收样开始前（天）' : '收样截止前（天）' }}<input v-model.number="rule.offsetDays" type="number" min="0" max="365" :disabled="busy"></label><label>发送时刻<input v-model="rule.sendTime" type="time" :disabled="busy"></label></template>
            <label v-else-if="rule.scheduleMode === 'FIXED'" class="date-field">发送日期与时间<input v-model="rule.scheduledAt" type="datetime-local" :disabled="busy"></label>
            <span v-else class="event-rule muted">结果公开后发送</span>
          </div>
          <div class="event-actions"><el-switch v-model="rule.enabled" :disabled="busy" :aria-label="`启用${rule.eventLabel}`" /><button class="text-button" :disabled="busy" @click="editContent(rule.eventCode)">编辑邮件<ArrowRight /></button></div>
        </article>
        <p class="business-note">修改时间仅影响尚未生成的通知；已发送的通知不会重复发送。</p>
      </section>

      <section v-if="view === 'content'" class="content-workspace">
        <nav class="event-nav" aria-label="通知类型">
          <button v-for="item in templates" :key="item.eventCode" :class="{ selected: eventCode === item.eventCode }" :disabled="busy" @click="selectTemplate(item.eventCode)"><Message /><span>{{ item.eventLabel }}<small>{{ ruleEnabled(item.eventCode) ? '已启用' : '已停用' }}</small></span><ArrowRight /></button>
        </nav>
        <div class="mail-workspace">
          <div class="mail-actions"><strong>{{ selectedTemplate?.eventLabel }}</strong><span class="save-state" :class="{ gold: templateDirty }">{{ templateDirty ? '未保存' : '已保存' }}</span><button class="button" :disabled="busy" @click="previewOpen = true"><View />预览</button><button class="button" :disabled="busy" @click="testOpen = true"><Promotion />测试发送</button></div>
          <div class="scroll-area compose-scroll">
            <div class="subject-field"><label for="mail-subject">邮件主题</label><div class="subject-input"><input id="mail-subject" ref="subjectInput" v-model="draft.subject" maxlength="400" :disabled="busy" @select="rememberSubject" @keyup="rememberSubject" @click="rememberSubject"><el-dropdown trigger="click" @command="insertSubjectField"><button class="button icon" title="插入主题信息" aria-label="插入主题信息"><Plus /></button><template #dropdown><el-dropdown-menu><el-dropdown-item v-for="key in subjectFields" :key="key" :command="key">{{ fieldLabels[key] || key }}</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div></div>
            <MailBodyEditor v-if="selectedTemplate" :key="`${competitionId}-${eventCode}`" v-model="draft.htmlBody" :variables="selectedTemplate.availableVariables" />
          </div>
        </div>
      </section>

      <section v-show="view === 'records'" class="scroll-area records-view">
        <div class="section-tools"><label>发送状态 <select v-model="status" :disabled="recordsLoading" @change="loadRecords"><option value="">全部状态</option><option v-for="(label, key) in statuses" :key="key" :value="key">{{ label }}</option></select></label><span class="muted">最近 {{ deliveries.length }} 条记录</span></div>
        <div class="table-scroll"><table><thead><tr><th>通知 / 邮件主题</th><th>厂商 / 邮箱</th><th>计划时间</th><th>发送状态</th><th>尝试次数</th><th>操作</th></tr></thead><tbody><tr v-for="item in pagedRecords" :key="item.id"><td><strong>{{ item.eventLabel }}</strong><small>{{ item.subject }}</small></td><td>{{ item.recipientName }}<small>{{ item.recipientEmail }}</small></td><td>{{ formatTime(item.scheduledTime) }}</td><td><span class="delivery-status" :class="item.status.toLowerCase()">{{ item.statusLabel }}</span><small v-if="item.sentTime">{{ formatTime(item.sentTime) }}</small></td><td>{{ item.attemptCount || 0 }}</td><td><button class="text-button" @click="recordDetail = item">详情</button><button v-if="['FAILED', 'SKIPPED'].includes(item.status)" class="text-button" :disabled="busy" @click="retry(item)">重发</button></td></tr></tbody></table></div>
        <div v-if="!deliveries.length" class="empty-state"><Message /><span>{{ recordsLoading ? '正在加载发送记录…' : '暂无发送记录' }}</span></div>
        <el-pagination v-if="deliveries.length > 20" v-model:current-page="page" :total="deliveries.length" :page-size="20" layout="prev, pager, next" />
      </section>

      <footer v-if="view !== 'records'" class="workspace-footer"><button class="button" :disabled="busy || !(view === 'content' ? templateDirty : rulesDirty)" @click="resetCurrent">撤销修改</button><button class="button primary" :disabled="busy || !(view === 'content' ? templateDirty : rulesDirty)" @click="view === 'content' ? saveTemplate() : saveRules()"><Check />{{ saving ? '保存中…' : view === 'content' ? '保存邮件' : '保存安排' }}</button></footer>
    </template>

    <el-dialog v-model="previewOpen" title="邮件预览" width="min(820px, 94vw)" top="5vh" append-to-body destroy-on-close><div class="preview-subject">{{ previewSubject }}</div><iframe class="preview-frame" :srcdoc="previewHtml" title="邮件内容预览" sandbox referrerpolicy="no-referrer"></iframe></el-dialog>
    <el-dialog v-model="testOpen" title="测试发送" width="min(480px, 94vw)" append-to-body :close-on-click-modal="!testing" :show-close="!testing"><p>{{ selectedTemplate?.eventLabel }}</p><p v-if="templateDirty" class="dialog-note">当前邮件有未保存的修改。请先保存，再发送测试邮件。</p><el-form label-position="top" @submit.prevent="sendTest"><el-form-item label="收件邮箱"><el-input v-model.trim="testEmail" type="email" placeholder="填写测试收件邮箱" :disabled="testing" /></el-form-item><p class="dialog-note">测试使用已保存的邮件内容和示例厂商数据，仅发送至此邮箱。</p></el-form><template #footer><el-button :disabled="testing" @click="testOpen = false">取消</el-button><el-button type="primary" :disabled="templateDirty || !testEmail" :loading="testing" @click="sendTest">发送测试邮件</el-button></template></el-dialog>
    <el-dialog :model-value="Boolean(recordDetail)" title="发送详情" width="min(560px, 94vw)" append-to-body @close="recordDetail = null"><dl v-if="recordDetail" class="record-detail"><dt>邮件主题</dt><dd>{{ recordDetail.subject }}</dd><dt>收件厂商</dt><dd>{{ recordDetail.recipientName }} · {{ recordDetail.recipientEmail }}</dd><dt>状态</dt><dd>{{ recordDetail.statusLabel }} · 已尝试 {{ recordDetail.attemptCount || 0 }} 次</dd><dt>计划 / 发送时间</dt><dd>{{ formatTime(recordDetail.scheduledTime) }} / {{ formatTime(recordDetail.sentTime) }}</dd><dt v-if="recordDetail.lastError">失败原因</dt><dd v-if="recordDetail.lastError">{{ recordDetail.lastError }}</dd></dl></el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Calendar, Check, Clock, Message, Plus, Promotion, Refresh, Trophy, View } from '@element-plus/icons-vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import MailBodyEditor from '@/components/admin/MailBodyEditor.vue'
import { fieldLabels, readableFields, encodedFields, renderPreview } from './emailTemplateFields'
import { fetchCompetitionNotificationDeliveries, fetchCompetitionNotifications, fetchCompetitions, retryCompetitionNotification, sendCompetitionNotificationTest, updateCompetitionNotificationRule, updateCompetitionNotificationTemplate } from '@/api/admin'

const views = [{ key: 'schedule', label: '发送安排', icon: Calendar }, { key: 'content', label: '邮件内容', icon: Message }, { key: 'records', label: '发送记录', icon: Clock }]
const statuses = { PENDING: '待发送', SENDING: '发送中', SENT: '已发送', RETRY_WAITING: '等待重试', FAILED: '发送失败', SKIPPED: '已跳过', CANCELLED: '已取消' }
const view = ref('schedule'), competitions = ref([]), competitionId = ref(''), config = ref(null)
const loading = ref(false), saving = ref(false), testing = ref(false), error = ref('')
const rules = ref([]), savedRules = ref('[]'), templates = ref([]), eventCode = ref('SAMPLE_START')
const draft = ref({ subject: '', htmlBody: '' }), baseline = ref(''), subjectInput = ref(null)
const previewOpen = ref(false), testOpen = ref(false), testEmail = ref('')
const deliveries = ref([]), status = ref(''), page = ref(1), recordsLoading = ref(false), recordDetail = ref(null)
let recordRequest = 0, configRequest = 0, subjectSelection = { start: 0, end: 0 }
const busy = computed(() => loading.value || saving.value || testing.value)
const rulesDirty = computed(() => JSON.stringify(rules.value) !== savedRules.value)
const templateDirty = computed(() => JSON.stringify(draft.value) !== baseline.value && Boolean(baseline.value))
const selectedTemplate = computed(() => templates.value.find(item => item.eventCode === eventCode.value))
const subjectFields = computed(() => (selectedTemplate.value?.availableVariables || []).filter(key => key !== 'resultTable'))
const pagedRecords = computed(() => deliveries.value.slice((page.value - 1) * 20, page.value * 20))
const previewValues = computed(() => ({ ...config.value?.previewVariables, 'competition.name': config.value?.competitionName || '示例赛事', 'competition.code': config.value?.competitionCode || '示例编号', resultTable: '示例酒款：金奖', 'delivery.recipient': '示例厂商联系人' }))
const previewSubject = computed(() => readableFields(encodedFields(draft.value.subject).replace(/\{\{\s*([\w.]+)\s*}}/g, (_, key) => previewValues.value[key] || `〔${fieldLabels[key]}〕`)))
const previewHtml = computed(() => `<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'"><meta name="viewport" content="width=device-width, initial-scale=1"><style>body{margin:0;padding:20px;font-family:Arial,sans-serif;color:#26352e;overflow-wrap:anywhere}table{max-width:100%;table-layout:fixed}*{box-sizing:border-box}</style>${renderPreview(draft.value.htmlBody, previewValues.value)}`)

function syncDraft() {
  draft.value = { subject: readableFields(selectedTemplate.value?.subject || ''), htmlBody: selectedTemplate.value?.htmlBody || '' }
  baseline.value = JSON.stringify(draft.value)
}
function applyConfig(data) {
  config.value = data
  rules.value = (data.rules || []).map(rule => ({ ...rule, offsetDays: Math.abs(Math.round((rule.offsetMinutes || 0) / 1440)), scheduledAt: rule.scheduledAt?.slice(0, 16) || '' }))
  savedRules.value = JSON.stringify(rules.value)
  templates.value = data.templates || []
  if (!templates.value.some(item => item.eventCode === eventCode.value)) eventCode.value = templates.value[0]?.eventCode || 'SAMPLE_START'
  syncDraft()
}
async function loadConfiguration() {
  if (!competitionId.value) return
  const request = ++configRequest
  loading.value = true
  error.value = ''
  try { const data = await fetchCompetitionNotifications(competitionId.value); if (request === configRequest) { applyConfig(data); await loadRecords() } }
  catch { if (request === configRequest) error.value = '邮件通知加载失败，请重试' }
  finally { if (request === configRequest) loading.value = false }
}
async function loadRecords() {
  const request = ++recordRequest, id = competitionId.value
  recordsLoading.value = true
  try { const data = await fetchCompetitionNotificationDeliveries(id, { status: status.value || undefined }); if (request === recordRequest && id === competitionId.value) { deliveries.value = data; page.value = 1 } }
  finally { if (request === recordRequest) recordsLoading.value = false }
}
async function discardAllowed(dirty) {
  if (!dirty) return true
  try { await ElMessageBox.confirm('有尚未保存的修改，离开后将丢弃。', '放弃修改？', { confirmButtonText: '放弃修改', cancelButtonText: '继续编辑', type: 'warning' }); return true } catch { return false }
}
async function switchCompetition(event) {
  const value = event.target.value
  event.target.value = competitionId.value
  if (!await discardAllowed(rulesDirty.value || templateDirty.value)) return
  competitionId.value = value
  deliveries.value = []
  await loadConfiguration()
}
async function refresh() { if (await discardAllowed(rulesDirty.value || templateDirty.value)) await loadConfiguration() }
async function selectTemplate(code) {
  if (code === eventCode.value) return true
  if (!await discardAllowed(templateDirty.value)) return false
  eventCode.value = code
  syncDraft()
  return true
}
async function editContent(code) { if (await selectTemplate(code)) view.value = 'content' }
function ruleEnabled(code) { return rules.value.find(rule => rule.eventCode === code)?.enabled }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '未设置' }
function triggerLabel(rule) {
  if (!rule.enabled) return '已停用'
  if (rule.scheduleMode === 'AFTER_EVENT') return config.value.status === 'PUBLISHED' ? '结果已发布' : '等待结果发布'
  if (rule.scheduleMode === 'FIXED') return `计划 ${formatTime(rule.scheduledAt)}`
  return rule.nextTriggerDescription || '等待赛事时间配置'
}
async function saveRules() {
  const original = JSON.parse(savedRules.value)
  const changed = rules.value.filter(rule => JSON.stringify(rule) !== JSON.stringify(original.find(item => item.eventCode === rule.eventCode)))
  if (changed.some(rule => rule.scheduleMode === 'FIXED' && !rule.scheduledAt)) return ElMessage.warning('请选择发送日期和时间')
  if (changed.some(rule => rule.scheduleMode === 'RELATIVE' && (!Number.isInteger(rule.offsetDays) || rule.offsetDays < 0 || rule.offsetDays > 365 || !rule.sendTime))) return ElMessage.warning('提前天数须为 0 至 365 的整数，并填写发送时刻')
  saving.value = true
  try {
    for (const rule of changed) {
      const data = await updateCompetitionNotificationRule(competitionId.value, { eventCode: rule.eventCode, enabled: rule.enabled, scheduleMode: rule.scheduleMode, scheduledAt: rule.scheduleMode === 'FIXED' ? `${rule.scheduledAt}:00` : null, offsetMinutes: rule.scheduleMode === 'RELATIVE' ? -rule.offsetDays * 1440 : 0, sendTime: rule.sendTime || '10:00', templateId: rule.templateId })
      const saved = data.rules.find(item => item.eventCode === rule.eventCode)
      if (saved) rule.nextTriggerDescription = saved.nextTriggerDescription
      const index = original.findIndex(item => item.eventCode === rule.eventCode)
      original[index] = { ...rule }
      savedRules.value = JSON.stringify(original)
    }
    ElMessage.success('发送安排已保存')
  } finally { saving.value = false }
}
async function saveTemplate() {
  const subject = encodedFields(draft.value.subject).trim()
  if (!subject || subject.length > 200) return ElMessage.warning('邮件主题不能为空，且不能超过 200 个字符')
  if (!draft.value.htmlBody.trim() || draft.value.htmlBody.length > 50000) return ElMessage.warning('请填写邮件正文，内容不能超过 50000 个字符')
  saving.value = true
  try {
    const data = await updateCompetitionNotificationTemplate(competitionId.value, eventCode.value, { subject, htmlBody: draft.value.htmlBody })
    templates.value = data.templates || []
    // Refresh template references without overwriting unsaved schedule changes.
    const original = JSON.parse(savedRules.value)
    for (const item of data.rules) {
      const rule = rules.value.find(rule => rule.eventCode === item.eventCode)
      const saved = original.find(rule => rule.eventCode === item.eventCode)
      if (rule) rule.templateId = item.templateId
      if (saved) saved.templateId = item.templateId
    }
    savedRules.value = JSON.stringify(original)
    syncDraft()
    ElMessage.success('邮件内容已保存')
  } finally { saving.value = false }
}
async function resetCurrent() {
  if (!await discardAllowed(true)) return
  if (view.value === 'content') syncDraft()
  else rules.value = JSON.parse(savedRules.value)
}
function rememberSubject(event) { subjectSelection = { start: event.target.selectionStart || 0, end: event.target.selectionEnd || 0 } }
async function insertSubjectField(key) {
  const token = `〔${fieldLabels[key] || key}〕`, source = draft.value.subject
  draft.value.subject = source.slice(0, subjectSelection.start) + token + source.slice(subjectSelection.end)
  const caret = subjectSelection.start + token.length
  await nextTick()
  subjectInput.value.focus()
  subjectInput.value.setSelectionRange(caret, caret)
  subjectSelection = { start: caret, end: caret }
}
async function sendTest() {
  if (templateDirty.value || testing.value) return
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(testEmail.value)) return ElMessage.warning('请填写有效的收件邮箱')
  testing.value = true
  try { await sendCompetitionNotificationTest(competitionId.value, { eventCode: eventCode.value, email: testEmail.value }); testOpen.value = false; ElMessage.success('测试邮件已提交发送') } finally { testing.value = false }
}
async function retry(item) {
  saving.value = true
  try { await retryCompetitionNotification(competitionId.value, item.id); await loadRecords(); ElMessage.success('已安排重新发送') } finally { saving.value = false }
}
function beforeUnload(event) { if (rulesDirty.value || templateDirty.value) { event.preventDefault(); event.returnValue = '' } }
onBeforeRouteLeave(() => busy.value ? false : discardAllowed(rulesDirty.value || templateDirty.value))
onMounted(async () => {
  window.addEventListener('beforeunload', beforeUnload)
  loading.value = true
  try { competitions.value = await fetchCompetitions({ includeArchived: false }); competitionId.value = competitions.value[0]?.id || ''; await loadConfiguration() }
  catch { error.value = '赛事列表加载失败，请刷新重试' }
  finally { loading.value = false }
})
onBeforeUnmount(() => { configRequest++; recordRequest++; window.removeEventListener('beforeunload', beforeUnload) })
</script>

<style scoped>
.notification-page{height:100%;min-height:0;display:flex;flex-direction:column;padding:0 28px;color:#dce9ed;background:#0b1115;--line:#263237;--muted:#91a3aa;--gold:#dfbd66;letter-spacing:0}
.workspace-top{flex:none}.notification-page :deep(.admin-page-header){border-bottom-color:var(--line)}
.competition-bar{display:flex;align-items:center;gap:20px;padding:18px 0 14px;min-width:0}.competition-bar label,.section-tools label{display:flex;align-items:center;gap:12px;font-size:13px}.competition-bar>label{flex:none}.competition-bar select{width:310px;max-width:45vw}
.notification-page select,.notification-page input{height:38px;padding:0 11px;border:1px solid #354248;border-radius:5px;color:#e5edef;background:#192226;min-width:0;font:inherit;color-scheme:dark}.notification-page select{max-width:100%}.notification-page input:focus,.notification-page select:focus{outline:2px solid #c8a955;outline-offset:1px}.notification-page button:focus-visible{outline:2px solid #e2c577;outline-offset:2px}.notification-page svg{width:17px;height:17px;flex:none}
.summary-strip{display:flex;align-items:center;flex-wrap:wrap;gap:8px 28px;min-width:0;flex:1;font-size:12px;color:var(--muted)}.summary-strip span{display:inline-flex;align-items:center;gap:8px;white-space:nowrap}.summary-strip strong{font-size:20px;color:#f0f5f6;font-variant-numeric:tabular-nums}.summary-strip .green{color:#93c9a3}.summary-strip .red{color:#ef9c91}.gold{color:var(--gold)!important}.muted{color:var(--muted);font-size:12px}
.view-tabs{display:flex;gap:26px;border-bottom:1px solid var(--line)}.view-tabs button{display:flex;align-items:center;gap:8px;padding:17px 2px;background:none;border:0;border-bottom:2px solid transparent;color:var(--muted);font-size:14px;cursor:pointer}.view-tabs button.active{border-bottom-color:var(--gold);color:#f3d58d}
.scroll-area{min-height:0;overflow-y:auto;scrollbar-gutter:stable;scrollbar-width:thin;scrollbar-color:#435057 transparent}.schedule-view,.records-view{flex:1;padding:22px 0}.section-tools{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:16px;flex-wrap:wrap}
.schedule-item{display:grid;grid-template-columns:minmax(205px,1fr) minmax(420px,1.8fr) 145px;align-items:center;gap:24px;border-bottom:1px solid var(--line);padding:24px 8px}.event-name{display:flex;align-items:center;gap:14px;min-width:0}.event-name>div{display:grid;gap:8px;min-width:0}.event-name strong{font-size:14px;overflow-wrap:anywhere}.event-icon{display:grid;place-items:center;width:38px;height:38px;flex:none;border-radius:6px;background:#242b25;color:#cab66d}.inactive .event-icon{color:#859398;background:#1a2327}.schedule-controls{display:grid;grid-template-columns:1.15fr 1fr 1fr;gap:14px;align-items:end}.schedule-controls label{display:grid;gap:8px;font-size:12px;color:var(--muted)}.schedule-controls input,.schedule-controls select{width:100%;font-size:13px}.date-field{grid-column:span 2}.event-rule{grid-column:span 2;align-self:center}.event-actions{display:flex;align-items:center;justify-content:flex-end;gap:12px}.event-actions :deep(.el-switch){--el-switch-on-color:#b79843;--el-switch-off-color:#435057}.text-button{display:inline-flex;align-items:center;gap:3px;padding:6px 0;color:#dfbd66;background:none;border:0;cursor:pointer;font-size:12px;white-space:nowrap}.text-button+.text-button{margin-left:14px}.business-note{color:var(--muted);font-size:12px;margin:20px 8px}
.content-workspace{display:grid;grid-template-columns:218px minmax(0,1fr);flex:1;min-height:0}.event-nav{padding:20px 18px 20px 0;border-right:1px solid var(--line);overflow-y:auto}.event-nav button{display:flex;align-items:center;gap:10px;width:100%;padding:16px 12px;border:1px solid transparent;border-radius:6px;background:none;text-align:left;color:#aabac0;cursor:pointer;margin-bottom:6px}.event-nav button.selected{border-color:#5f5436;background:#26251c;color:#f0d18b}.event-nav button>span{flex:1;font-size:13px;line-height:1.5}.event-nav small{display:block;color:var(--muted);font-size:11px;margin-top:5px}.event-nav button>svg:last-child{width:12px}.mail-workspace{display:flex;flex-direction:column;min-height:0;min-width:0;padding-left:24px}.mail-actions{display:flex;align-items:center;gap:10px;padding:18px 0;flex:none}.mail-actions>strong{font-size:15px}.save-state{font-size:12px;color:#93c9a3;margin-right:auto}.compose-scroll{flex:1;padding:0 8px 24px 0}.subject-field{display:grid;gap:10px;margin-bottom:18px;font-size:12px;color:var(--muted)}.subject-input{display:flex;gap:8px}.subject-input>input{flex:1;width:100%;font-size:14px}.workspace-footer{display:flex;align-items:center;justify-content:flex-end;gap:10px;min-height:72px;padding:14px 0;border-top:1px solid var(--line);flex:none}
.button{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:36px;padding:0 13px;border:1px solid #364249;border-radius:5px;background:#192226;color:#dce9ed;cursor:pointer;font-size:13px;white-space:nowrap}.button:hover:not(:disabled){border-color:#ac9150;background:#272d29}.button.primary{background:#c5a54e;border-color:#c5a54e;color:#171c19;font-weight:700}.button.icon{width:36px;padding:0}.notification-page button:disabled{opacity:.45;cursor:not-allowed}
.table-scroll{overflow-x:auto}table{width:100%;border-collapse:collapse;text-align:left;font-size:13px;table-layout:fixed;min-width:850px}th{color:var(--muted);font-size:12px;font-weight:500;background:#151e22}th,td{padding:16px 12px;border-bottom:1px solid var(--line);overflow-wrap:anywhere}th:first-child{width:26%}th:nth-child(2){width:23%}th:nth-child(3){width:17%}th:nth-child(5){width:8%}td small{display:block;color:var(--muted);font-size:11px;margin-top:6px}.delivery-status{display:inline-flex;align-items:center;gap:6px}.delivery-status:before{content:'';width:6px;height:6px;border-radius:50%;background:#9baeb4}.delivery-status.sent{color:#93c9a3}.delivery-status.failed{color:#ef9c91}.delivery-status.retry_waiting{color:#dfbd66}.delivery-status:before{background:currentColor}.records-view :deep(.el-pagination){justify-content:flex-end;margin-top:18px}.empty-state{display:flex;flex:1;flex-direction:column;align-items:center;justify-content:center;gap:18px;min-height:180px;color:var(--muted);font-size:14px}.empty-state>svg{width:32px;height:32px}.preview-subject{padding:0 0 16px;overflow-wrap:anywhere;font-weight:600}.preview-frame{display:block;width:100%;height:65vh;border:1px solid #d9e0dc;background:#fff;border-radius:4px}.dialog-note{font-size:13px;color:#707a7f;line-height:1.7}.record-detail dt{font-size:12px;color:#7b8589;margin-top:18px}.record-detail dd{margin:6px 0 0;overflow-wrap:anywhere;line-height:1.6}
@media(max-width:1450px){.schedule-item{grid-template-columns:minmax(180px,1fr) minmax(330px,1.7fr);gap:16px}.event-actions{grid-column:2;justify-content:flex-end}.content-workspace{grid-template-columns:190px minmax(0,1fr)}.schedule-controls{gap:10px}}
@media(max-width:980px){.notification-page{height:calc(100dvh - 16px);min-height:620px;padding:0 16px}.summary-strip{gap:10px 18px;flex-basis:100%}.content-workspace{grid-template-columns:1fr;grid-template-rows:auto minmax(0,1fr)}.event-nav{display:flex;gap:8px;padding:10px 0;overflow-x:auto;border-right:0}.event-nav button{min-width:160px;margin:0;padding:10px}.event-nav small{display:none}.mail-workspace{padding-left:0}.schedule-item{grid-template-columns:1fr}.event-actions{grid-column:auto}.competition-bar{flex-wrap:wrap}.mail-actions{flex-wrap:wrap}}
</style>
