<template>
  <section class="judge-performance-panel">
    <header class="panel-toolbar">
      <div class="panel-heading">
        <h2>评审表现</h2>
        <span v-if="summary">{{ summary.confirmedCount || 0 }} / {{ summary.totalJudges || 0 }} 已确认</span>
      </div>
      <div class="panel-actions">
        <label class="search-control">
          <Search />
          <input v-model.trim="keyword" aria-label="搜索评审" placeholder="搜索评审" />
        </label>
        <select v-model="statusFilter" aria-label="评价状态">
          <option value="ALL">全部状态</option>
          <option value="UNRATED">待评价</option>
          <option value="DRAFT">草稿</option>
          <option value="CONFIRMED">已确认</option>
        </select>
        <select v-model="roleFilter" aria-label="评审角色">
          <option value="ALL">全部角色</option>
          <option value="PROFESSIONAL">专业评审</option>
          <option value="CROSS">跨界评审</option>
          <option value="CAPTAIN">桌长</option>
        </select>
        <button class="tool-button" type="button" :disabled="loading" @click="loadData">
          <Refresh />
          刷新
        </button>
      </div>
    </header>

    <div v-if="summary && !summary.draftAllowed" class="state-note">
      <Clock />
      评分轮锁定后开放评审表现评价，当前仅显示实时任务统计。
    </div>
    <div v-else-if="summary && !summary.confirmAllowed" class="state-note warning">
      <Warning />
      {{ summary.confirmDisabledReason }}。可以先保存草稿，比赛进入结果确认后再提交。
    </div>

    <div class="performance-table-wrap">
      <div class="performance-table-head">
        <span>排名</span>
        <span>评审</span>
        <span>完成度</span>
        <span>评语投入</span>
        <span>人工评价</span>
        <span>赛事表现</span>
        <span>操作</span>
      </div>
      <div v-if="loading" class="table-empty">正在读取评审表现</div>
      <div v-else-if="visibleRows.length === 0" class="table-empty">暂无符合条件的评审</div>
      <div v-for="row in visibleRows" v-else :key="row.judgePublicId" class="performance-row">
        <span class="rank-cell">{{ row.overallRank || '-' }}</span>
        <div class="judge-cell">
          <span class="avatar">{{ initial(row.judgeName) }}</span>
          <div>
            <strong>{{ row.judgeName || '未填写姓名' }}</strong>
            <small>{{ row.roleLabel }} · {{ row.tableName || '未分桌' }}</small>
          </div>
        </div>
        <div class="completion-cell">
          <strong>{{ row.taskCompletedCount || 0 }} / {{ row.taskTotalCount || 0 }}</strong>
          <small>{{ formatNumber(row.completionRate) }}%</small>
        </div>
        <div class="comment-cell">
          <strong>{{ row.commentTopPercent ? `前 ${row.commentTopPercent}%` : '-' }}</strong>
          <small>{{ row.commentAverageChars || 0 }} 字/份 · {{ formatNumber(row.commentScore) }} 分</small>
        </div>
        <div class="manual-cell">
          <strong>{{ row.manualScore == null ? '-' : `${formatNumber(row.manualScore)} / 70` }}</strong>
          <small>{{ statusLabel(row.evaluationStatus) }}</small>
        </div>
        <div class="total-cell">
          <strong v-if="row.totalScore != null">{{ formatNumber(row.totalScore) }}</strong>
          <strong v-else class="muted">待确认</strong>
          <span v-if="row.excellentCandidate" class="candidate-badge">优秀候选</span>
          <span v-else-if="row.sampleWarning" class="sample-badge">样本较少</span>
        </div>
        <div class="row-actions">
          <button class="row-action" type="button" :disabled="!summary?.draftAllowed && row.evaluationStatus === 'UNRATED'" @click="openDrawer(row)">
            {{ row.evaluationStatus === 'CONFIRMED' ? '查看' : '评价' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="drawerOpen" class="drawer-mask" @click.self="closeDrawer">
      <aside class="evaluation-drawer">
        <header class="drawer-header">
          <div>
            <span>评审表现评价</span>
            <h2>{{ selectedRow?.judgeName || '未填写姓名' }}</h2>
            <small>{{ selectedRow?.roleLabel }} · {{ selectedRow?.tableName || '未分桌' }}</small>
          </div>
          <button class="icon-close" type="button" aria-label="关闭" @click="closeDrawer"><Close /></button>
        </header>

        <div class="drawer-summary">
          <div><span>赛事表现</span><strong>{{ selectedRow?.totalScore == null ? '待确认' : formatNumber(selectedRow.totalScore) }}</strong></div>
          <div><span>评语投入</span><strong>{{ selectedRow?.commentTopPercent ? `前 ${selectedRow.commentTopPercent}%` : '-' }}</strong></div>
          <div><span>任务完成</span><strong>{{ selectedRow?.taskCompletedCount || 0 }} / {{ selectedRow?.taskTotalCount || 0 }}</strong></div>
        </div>

        <section class="score-section">
          <div class="section-title"><h3>人工评价 <small>70 分</small></h3><span>保存草稿时可暂不填完</span></div>
          <div v-for="item in manualItems" :key="item.key" class="manual-item">
            <div><strong>{{ item.label }}</strong><small>{{ item.weight }} 分</small></div>
            <div class="level-options">
              <button v-for="level in levels" :key="level.value" :class="{ active: form[item.key] === level.value }" type="button" :disabled="selectedRow?.evaluationStatus === 'CONFIRMED'" @click="form[item.key] = level.value">
                {{ level.label }}
              </button>
            </div>
          </div>
          <label class="evidence-field">
            <span>评价依据 <small>选择“严重不足”或“优秀”时必填</small></span>
            <textarea v-model.trim="form.evidence" maxlength="1000" :readonly="selectedRow?.evaluationStatus === 'CONFIRMED'" placeholder="记录具体观察，便于后续复盘"></textarea>
            <small>{{ form.evidence.length }} / 1000</small>
          </label>
        </section>

        <section class="score-section readonly-section">
          <div class="section-title"><h3>评语投入 <small>30 分</small></h3><span>系统按锁定评分轮统计</span></div>
          <dl>
            <div><dt>有效评语</dt><dd>{{ selectedRow?.commentRecordCount || 0 }} 份</dd></div>
            <div><dt>总字数</dt><dd>{{ selectedRow?.commentTotalChars || 0 }} 字</dd></div>
            <div><dt>平均字数</dt><dd>{{ selectedRow?.commentAverageChars || 0 }} 字/份</dd></div>
            <div><dt>比赛百分位</dt><dd>{{ selectedRow?.commentTopPercent ? `前 ${selectedRow.commentTopPercent}%` : '-' }}</dd></div>
          </dl>
        </section>

        <footer class="drawer-footer">
          <button class="tool-button" type="button" @click="closeDrawer">取消</button>
          <button v-if="selectedRow?.evaluationStatus !== 'CONFIRMED'" class="tool-button" type="button" :disabled="saving || !summary?.draftAllowed" @click="save('DRAFT')">保存草稿</button>
          <button v-if="selectedRow?.evaluationStatus !== 'CONFIRMED'" class="tool-button primary" type="button" :disabled="saving || !summary?.confirmAllowed" @click="save('CONFIRMED')">确认评价</button>
          <span v-else class="confirmed-note">已确认，评价内容仅供查看</span>
        </footer>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Close, Refresh, Search, Warning } from '@element-plus/icons-vue'
import { fetchCompetitionJudgePerformances, saveJudgePerformance } from '@/api/admin'

const props = defineProps({ competition: { type: Object, default: null } })
const summary = ref(null)
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('ALL')
const roleFilter = ref('ALL')
const drawerOpen = ref(false)
const selectedRow = ref(null)
const form = reactive({ judgmentLevel: null, feedbackQualityLevel: null, ruleExecutionLevel: null, professionalismLevel: null, evidence: '', version: 0 })
const levels = [{ value: 1, label: '严重不足' }, { value: 2, label: '待改进' }, { value: 3, label: '合格' }, { value: 4, label: '良好' }, { value: 5, label: '优秀' }]
const manualItems = [
  { key: 'judgmentLevel', label: '专业判断与评分依据', weight: 20 },
  { key: 'feedbackQualityLevel', label: '反馈内容质量', weight: 20 },
  { key: 'ruleExecutionLevel', label: '规则执行与任务完成', weight: 15 },
  { key: 'professionalismLevel', label: '协作、公正与职业表现', weight: 15 },
]

const visibleRows = computed(() => (summary.value?.records || []).filter((row) => {
  const matchesKeyword = !keyword.value || String(row.judgeName || '').includes(keyword.value)
  const matchesStatus = statusFilter.value === 'ALL' || row.evaluationStatus === statusFilter.value
  const matchesRole = roleFilter.value === 'ALL' || row.role === roleFilter.value
  return matchesKeyword && matchesStatus && matchesRole
}))

watch(() => props.competition?.id, () => loadData())
onMounted(loadData)

async function loadData() {
  if (!props.competition?.id) return
  loading.value = true
  try { summary.value = await fetchCompetitionJudgePerformances(props.competition.id) } finally { loading.value = false }
}

function openDrawer(row) {
  selectedRow.value = row
  form.judgmentLevel = row.judgmentLevel || null
  form.feedbackQualityLevel = row.feedbackQualityLevel || null
  form.ruleExecutionLevel = row.ruleExecutionLevel || null
  form.professionalismLevel = row.professionalismLevel || null
  form.evidence = row.evidence || ''
  form.version = row.version || 0
  drawerOpen.value = true
}

function closeDrawer() { drawerOpen.value = false; selectedRow.value = null }

async function save(status) {
  if (!selectedRow.value || !props.competition?.id) return
  saving.value = true
  try {
    const judgePublicId = selectedRow.value.judgePublicId
    await saveJudgePerformance(props.competition.id, judgePublicId, { ...form, status })
    await loadData()
    selectedRow.value = (summary.value?.records || []).find((row) => row.judgePublicId === judgePublicId) || null
    form.version = selectedRow.value?.version || 0
    ElMessage.success(status === 'CONFIRMED' ? '评审表现已确认' : '评审表现草稿已保存')
    if (status === 'CONFIRMED') closeDrawer()
  } finally { saving.value = false }
}

function initial(name) { return String(name || '评').trim().slice(0, 1) || '评' }
function formatNumber(value) { return value == null ? '-' : Number(value).toFixed(1) }
function statusLabel(value) { return ({ UNRATED: '待评价', DRAFT: '草稿', CONFIRMED: '已确认' })[value] || '待评价' }
</script>

<style scoped>
.judge-performance-panel { height: 100%; min-height: 0; display: flex; flex-direction: column; color: #e6edf0; }
.panel-toolbar { display: flex; justify-content: space-between; gap: 18px; align-items: center; padding: 0 0 16px; }
.panel-heading, .panel-actions, .search-control, .judge-cell, .completion-cell, .comment-cell, .manual-cell, .total-cell, .row-actions, .section-title, .drawer-footer { display: flex; align-items: center; }
.panel-heading { gap: 10px; }
.panel-heading h2 { margin: 0; font-size: 22px; }
.panel-heading span, .state-note, .performance-table-head, .judge-cell small, .completion-cell small, .comment-cell small, .manual-cell small, .section-title span, .readonly-section dt, .evidence-field > small { color: #8da1aa; }
.panel-actions { gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.search-control { gap: 8px; height: 40px; width: 220px; padding: 0 10px; border: 1px solid rgba(219,232,237,.14); border-radius: 8px; background: rgba(7,14,17,.68); color: #8da1aa; }
.search-control input, .panel-actions select { color: #e6edf0; background: #182327; border: 1px solid rgba(219,232,237,.14); border-radius: 8px; height: 40px; padding: 0 10px; outline: 0; }
.search-control input { width: 100%; border: 0; background: transparent; }
.tool-button, .row-action, .icon-close, .level-options button { color: #dbe6e9; border: 1px solid rgba(219,232,237,.14); border-radius: 8px; background: rgba(255,255,255,.035); }
.tool-button { min-height: 40px; padding: 0 12px; display: inline-flex; align-items: center; gap: 7px; }
.tool-button.primary { color: #e0b84a; border-color: rgba(216,169,53,.38); background: rgba(216,169,53,.09); }
.tool-button:disabled, .row-action:disabled { cursor: not-allowed; opacity: .45; }
.state-note { display: flex; align-items: center; gap: 8px; border: 1px solid rgba(219,232,237,.1); border-radius: 8px; padding: 10px 12px; margin-bottom: 12px; background: rgba(255,255,255,.03); }
.state-note.warning { color: #e0b84a; border-color: rgba(216,169,53,.28); }
.performance-table-wrap { min-height: 0; flex: 1; overflow: auto; border: 1px solid rgba(219,232,237,.1); border-radius: 8px; background: rgba(22,32,36,.88); }
.performance-table-head, .performance-row { display: grid; grid-template-columns: 58px minmax(190px,1.4fr) minmax(110px, .8fr) minmax(160px,1fr) minmax(120px,.8fr) minmax(130px,.9fr) 76px; align-items: center; column-gap: 14px; padding: 0 16px; }
.performance-table-head { min-height: 44px; position: sticky; top: 0; z-index: 1; background: #172125; font-size: 12px; }
.performance-row { min-height: 72px; border-top: 1px solid rgba(219,232,237,.08); }
.rank-cell { color: #e0b84a; font-size: 18px; font-weight: 700; text-align: center; }
.judge-cell { gap: 10px; min-width: 0; }
.avatar { width: 34px; height: 34px; flex: 0 0 auto; display: grid; place-items: center; color: #e0b84a; border: 1px solid rgba(216,169,53,.3); border-radius: 8px; background: rgba(216,169,53,.08); }
.judge-cell div, .completion-cell, .comment-cell, .manual-cell, .total-cell { min-width: 0; flex-direction: column; align-items: flex-start; gap: 4px; }
.judge-cell strong, .completion-cell strong, .comment-cell strong, .manual-cell strong, .total-cell strong { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
.judge-cell small, .completion-cell small, .comment-cell small, .manual-cell small { font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
.total-cell strong { font-size: 20px; color: #e0b84a; }
.total-cell .muted { color: #8da1aa; font-size: 14px; }
.candidate-badge, .sample-badge { font-size: 11px; padding: 3px 6px; border-radius: 5px; white-space: nowrap; }
.candidate-badge { color: #6fcf7a; background: rgba(111,207,122,.12); }
.sample-badge { color: #e0b84a; background: rgba(216,169,53,.1); }
.row-action { min-height: 36px; padding: 0 10px; color: #e0b84a; }
.table-empty { display: grid; place-items: center; min-height: 180px; color: #8da1aa; }
.drawer-mask { position: fixed; inset: 0; z-index: 30; display: flex; justify-content: flex-end; background: rgba(3,8,10,.58); }
.evaluation-drawer { width: min(520px, 100vw); height: 100%; overflow: auto; padding: 24px; background: #111c20; border-left: 1px solid rgba(219,232,237,.12); box-shadow: -24px 0 60px rgba(0,0,0,.3); }
.drawer-header { display: flex; justify-content: space-between; gap: 16px; padding-bottom: 18px; border-bottom: 1px solid rgba(219,232,237,.1); }
.drawer-header span, .drawer-header small, .drawer-summary span, .manual-item small, .section-title h3 small, .evidence-field span small { color: #8da1aa; }
.drawer-header h2 { margin: 6px 0; font-size: 24px; }
.icon-close { width: 36px; height: 36px; font-size: 22px; }
.drawer-summary { display: grid; grid-template-columns: repeat(3,1fr); gap: 10px; padding: 18px 0; }
.drawer-summary div { padding: 12px; border: 1px solid rgba(219,232,237,.1); border-radius: 8px; background: rgba(255,255,255,.03); }
.drawer-summary span, .drawer-summary strong { display: block; }
.drawer-summary strong { margin-top: 7px; color: #e0b84a; font-size: 18px; }
.score-section { padding: 18px 0; border-top: 1px solid rgba(219,232,237,.1); }
.section-title { justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.section-title h3 { margin: 0; font-size: 16px; }
.section-title h3 small { font-weight: 400; margin-left: 6px; }
.section-title span { font-size: 12px; }
.manual-item { padding: 12px 0; border-bottom: 1px solid rgba(219,232,237,.07); }
.manual-item > div:first-child { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 9px; }
.level-options { display: grid; grid-template-columns: repeat(5,1fr); gap: 6px; }
.level-options button { min-height: 34px; padding: 0 4px; font-size: 12px; color: #a9bbc2; }
.level-options button.active { color: #e0b84a; border-color: rgba(216,169,53,.44); background: rgba(216,169,53,.1); }
.level-options button:disabled { cursor: default; opacity: .78; }
.evidence-field { display: block; margin-top: 16px; }
.evidence-field span { display: flex; justify-content: space-between; gap: 10px; margin-bottom: 8px; }
.evidence-field textarea { width: 100%; min-height: 92px; resize: vertical; padding: 10px; color: #e6edf0; border: 1px solid rgba(219,232,237,.14); border-radius: 8px; background: rgba(7,14,17,.55); outline: 0; box-sizing: border-box; }
.evidence-field textarea:read-only { opacity: .78; cursor: default; }
.evidence-field > small { display: block; margin-top: 5px; text-align: right; }
.readonly-section dl { display: grid; grid-template-columns: repeat(2,1fr); gap: 10px 18px; margin: 0; }
.readonly-section dl div { display: flex; justify-content: space-between; gap: 10px; }
.readonly-section dd { margin: 0; color: #e6edf0; }
.drawer-footer { justify-content: flex-end; gap: 8px; padding-top: 18px; border-top: 1px solid rgba(219,232,237,.1); }
.confirmed-note { color: #8da1aa; font-size: 12px; }
@media (max-width: 1100px) { .panel-toolbar { align-items: flex-start; flex-direction: column; } .panel-actions { width: 100%; justify-content: flex-start; } }
@media (max-width: 760px) { .performance-table-wrap { overflow-x: auto; } .performance-table-head, .performance-row { min-width: 900px; } .evaluation-drawer { padding: 18px; } .drawer-summary { grid-template-columns: 1fr; } .drawer-footer { flex-wrap: wrap; } }
</style>
