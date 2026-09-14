<template>
  <section class="judge-performance-panel">
    <header class="panel-toolbar">
      <div class="panel-heading">
        <h2>评审表现</h2>
        <div class="status-chips">
          <span class="chip pending">待评价 {{ statusCounts.unrated }}</span>
          <span class="chip draft">草稿 {{ statusCounts.draft }}</span>
          <span class="chip confirmed">已确认 {{ statusCounts.confirmed }} / {{ statusCounts.total }}</span>
          <span v-if="summary?.excellentCandidateCount" class="chip candidate">优秀候选 {{ summary.excellentCandidateCount }}</span>
        </div>
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

    <div v-if="summary && (!summary.draftAllowed || !summary.confirmAllowed)" class="state-note">
      <Warning />
      <span>{{ summary.draftAllowed ? summary.confirmDisabledReason : '评分轮锁定后开放评审表现评价' }}</span>
    </div>

    <div class="performance-table-wrap">
      <div class="performance-table-head">
        <span>排名</span>
        <span>评审</span>
        <span>人工评价 · 70 分</span>
        <span>评语投入 · 30 分</span>
        <span>完成度</span>
        <span>赛事表现 · 100 分</span>
        <span>操作</span>
      </div>
      <div v-if="loading" class="table-empty">正在读取评审表现</div>
      <div v-else-if="visibleRows.length === 0" class="table-empty">暂无符合条件的评审</div>
      <div v-for="row in visibleRows" v-else :key="row.judgePublicId" class="performance-row">
        <span :class="['rank-cell', rankTone(row.overallRank)]">{{ row.overallRank || '-' }}</span>

        <div class="judge-cell">
          <span class="avatar">{{ initial(row.judgeName) }}</span>
          <div>
            <strong>{{ row.judgeName || '未填写姓名' }}</strong>
            <small>{{ row.roleLabel }} · {{ row.tableName || '未分桌' }}</small>
          </div>
        </div>

        <div class="manual-cell">
          <template v-if="row.manualScore != null">
            <div class="score-line">
              <strong>{{ formatNumber(row.manualScore) }}</strong>
              <small>/ 70</small>
            </div>
          </template>
          <template v-else>
            <strong class="pending-text">—</strong>
            <small>{{ statusLabel(row.evaluationStatus) }}</small>
          </template>
        </div>

        <div class="comment-cell">
          <div class="score-line">
            <strong>{{ row.commentScore == null ? '-' : formatNumber(row.commentScore) }}</strong>
            <small>/ 30</small>
          </div>
          <small>{{ row.commentTopPercent ? `前 ${row.commentTopPercent}%` : '-' }} · {{ row.commentAverageChars || 0 }} 字/份</small>
        </div>

        <div :class="['completion-cell', { incomplete: isUnderCompleted(row) }]">
          <span>{{ row.taskCompletedCount || 0 }}/{{ row.taskTotalCount || 0 }}</span>
          <small>{{ formatNumber(row.completionRate) }}%</small>
        </div>

        <div class="total-cell">
          <div class="score-line">
            <strong v-if="row.totalScore != null">{{ formatNumber(row.totalScore) }}</strong>
            <strong v-else class="muted">待确认</strong>
            <small v-if="row.totalScore != null">/ 100</small>
          </div>
          <span v-if="row.excellentCandidate" class="candidate-badge">优秀候选</span>
          <span v-else-if="row.sampleWarning" class="sample-badge">样本较少</span>
        </div>

        <div class="row-actions">
          <button class="row-action" type="button" :disabled="!summary?.draftAllowed && row.evaluationStatus === 'UNRATED'" @click="openModal(row)">
            {{ row.evaluationStatus === 'CONFIRMED' ? '查看' : '评价' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="modalOpen" class="modal-mask" @click.self="closeModal">
      <section class="performance-modal" role="dialog" aria-modal="true" :aria-label="`评审表现评价 ${selectedRow?.judgeName || ''}`">
        <header class="modal-head">
          <div class="modal-identity">
            <span class="avatar">{{ initial(selectedRow?.judgeName) }}</span>
            <div>
              <h2>{{ selectedRow?.judgeName || '未填写姓名' }}</h2>
              <small>{{ selectedRow?.roleLabel }} · {{ selectedRow?.tableName || '未分桌' }}</small>
            </div>
            <span :class="['status-chip', statusTone(selectedRow?.evaluationStatus)]">{{ statusLabel(selectedRow?.evaluationStatus) }}</span>
          </div>
          <div class="modal-metrics">
            <div>
              <span>人工评价</span>
              <strong>{{ selectedRow?.manualScore == null ? '—' : formatNumber(selectedRow.manualScore) }}</strong>
              <small>/ 70</small>
            </div>
            <div>
              <span>评语投入</span>
              <strong>{{ selectedRow?.commentScore == null ? '—' : formatNumber(selectedRow.commentScore) }}</strong>
              <small>/ 30</small>
            </div>
            <div>
              <span>赛事表现</span>
              <strong>{{ selectedRow?.totalScore == null ? '待确认' : formatNumber(selectedRow.totalScore) }}</strong>
              <small v-if="selectedRow?.totalScore != null">/ 100</small>
            </div>
            <div>
              <span>比赛排名</span>
              <strong>{{ selectedRow?.overallRank ? `${selectedRow.overallRank} / ${selectedRow.participantCount || '-'}` : '-' }}</strong>
            </div>
          </div>
          <button class="icon-close" type="button" aria-label="关闭" @click="closeModal"><Close /></button>
        </header>

        <div class="modal-body">
          <section class="eval-column">
            <div class="column-title">
              <h3>人工评价 <small>70 分</small></h3>
              <span>
                已评 {{ selectedItemCount }}/{{ manualItems.length }} 项
                <strong>当前 {{ formatNumber(liveManualScore) }} / 70</strong>
              </span>
            </div>
            <div v-for="item in manualItems" :key="item.key" class="manual-item">
              <div class="manual-item-label">
                <strong>{{ item.label }}</strong>
                <small>
                  {{ item.weight }} 分
                  <em v-if="itemContribution(item) != null">当前 {{ formatNumber(itemContribution(item)) }}</em>
                </small>
              </div>
              <div class="level-options">
                <button
                  v-for="level in levels"
                  :key="level.value"
                  :class="{ active: form[item.key] === level.value }"
                  type="button"
                  :disabled="readonly"
                  @click="selectLevel(item.key, level.value)"
                >
                  {{ level.label }}
                </button>
              </div>
            </div>
          </section>

          <section class="stat-column">
            <div class="column-title">
              <h3>评语投入 <small>30 分</small></h3>
              <span>按锁定评分轮统计</span>
            </div>
            <dl class="stat-list">
              <div><dt>有效评语</dt><dd>{{ selectedRow?.commentRecordCount || 0 }} 份</dd></div>
              <div><dt>总字数</dt><dd>{{ selectedRow?.commentTotalChars || 0 }} 字</dd></div>
              <div><dt>平均字数</dt><dd>{{ selectedRow?.commentAverageChars || 0 }} 字/份</dd></div>
              <div><dt>比赛百分位</dt><dd>{{ selectedRow?.commentTopPercent ? `前 ${selectedRow.commentTopPercent}%` : '-' }}</dd></div>
            </dl>
            <div class="task-block">
              <div class="task-line">
                <span>任务完成</span>
                <strong>{{ selectedRow?.taskCompletedCount || 0 }} / {{ selectedRow?.taskTotalCount || 0 }}</strong>
              </div>
              <span class="completion-bar"><i :style="{ width: barWidth(selectedRow?.completionRate) }"></i></span>
            </div>
            <div v-if="selectedRow?.excellentCandidate || selectedRow?.sampleWarning" class="remark-block">
              <span v-if="selectedRow?.excellentCandidate" class="candidate-badge">优秀候选</span>
              <span v-if="selectedRow?.sampleWarning" class="sample-badge">样本较少</span>
            </div>
          </section>

          <label class="evidence-field">
            <span>
              评价依据
              <em v-if="needsEvidence" :class="['rule-hint', { missing: !form.evidence }]">已选“严重不足/优秀”，必填</em>
              <em v-else>可选</em>
            </span>
            <textarea v-model.trim="form.evidence" maxlength="1000" :readonly="readonly" placeholder="记录具体观察，便于后续复盘"></textarea>
            <small>{{ form.evidence.length }} / 1000</small>
          </label>
        </div>

        <footer class="modal-foot">
          <div class="foot-meta">
            <span v-if="selectedRow?.evaluatedByName">评价人 {{ selectedRow.evaluatedByName }}</span>
            <span v-if="selectedRow?.confirmedTime">确认于 {{ formatTime(selectedRow.confirmedTime) }}</span>
          </div>
          <div class="foot-actions">
            <button class="tool-button" type="button" @click="closeModal">取消</button>
            <template v-if="readonly">
              <button class="tool-button primary" type="button" @click="editing = true">重新编辑</button>
            </template>
            <template v-else-if="editing">
              <button
                class="tool-button primary"
                type="button"
                :disabled="saving || !summary?.confirmAllowed || selectedItemCount < manualItems.length"
                :title="selectedItemCount < manualItems.length ? '保存前需完成四项人工评价' : ''"
                @click="save('CONFIRMED')"
              >
                保存修改
              </button>
            </template>
            <template v-else>
              <button class="tool-button" type="button" :disabled="saving || !summary?.draftAllowed" @click="save('DRAFT')">保存草稿</button>
              <button
                class="tool-button primary"
                type="button"
                :disabled="saving || !summary?.confirmAllowed || selectedItemCount < manualItems.length"
                :title="selectedItemCount < manualItems.length ? '确认前需完成四项人工评价' : ''"
                @click="save('CONFIRMED')"
              >
                确认评价
              </button>
            </template>
          </div>
        </footer>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Refresh, Search, Warning } from '@element-plus/icons-vue'
import { fetchCompetitionJudgePerformances, saveJudgePerformance } from '@/api/admin'

const props = defineProps({ competition: { type: Object, default: null } })
const summary = ref(null)
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('ALL')
const roleFilter = ref('ALL')
const modalOpen = ref(false)
const editing = ref(false)
const selectedRow = ref(null)
const form = reactive({ judgmentLevel: null, feedbackQualityLevel: null, ruleExecutionLevel: null, professionalismLevel: null, evidence: '', version: 0 })

const levels = [
  { value: 1, label: '严重不足', ratio: 0 },
  { value: 2, label: '待改进', ratio: 0.6 },
  { value: 3, label: '合格', ratio: 0.75 },
  { value: 4, label: '良好', ratio: 0.9 },
  { value: 5, label: '优秀', ratio: 1 },
]
const manualItems = [
  { key: 'judgmentLevel', label: '专业判断与评分依据', weight: 20 },
  { key: 'feedbackQualityLevel', label: '反馈内容质量', weight: 20 },
  { key: 'ruleExecutionLevel', label: '规则执行与任务完成', weight: 15 },
  { key: 'professionalismLevel', label: '协作、公正与职业表现', weight: 15 },
]

const statusCounts = computed(() => {
  const records = summary.value?.records || []
  return {
    total: records.length,
    unrated: records.filter((row) => row.evaluationStatus === 'UNRATED').length,
    draft: records.filter((row) => row.evaluationStatus === 'DRAFT').length,
    confirmed: records.filter((row) => row.evaluationStatus === 'CONFIRMED').length,
  }
})

const visibleRows = computed(() => (summary.value?.records || []).filter((row) => {
  const matchesKeyword = !keyword.value || String(row.judgeName || '').includes(keyword.value)
  const matchesStatus = statusFilter.value === 'ALL' || row.evaluationStatus === statusFilter.value
  const matchesRole = roleFilter.value === 'ALL' || row.role === roleFilter.value
  return matchesKeyword && matchesStatus && matchesRole
}))

const readonly = computed(() => selectedRow.value?.evaluationStatus === 'CONFIRMED' && !editing.value)
const selectedItemCount = computed(() => manualItems.filter((item) => form[item.key] != null).length)
const liveManualScore = computed(() => manualItems.reduce((sum, item) => sum + (itemContribution(item) || 0), 0))
const needsEvidence = computed(() => manualItems.some((item) => form[item.key] === 1 || form[item.key] === 5))

watch(() => props.competition?.id, () => loadData())
onMounted(loadData)

async function loadData() {
  if (!props.competition?.id) return
  loading.value = true
  try {
    summary.value = await fetchCompetitionJudgePerformances(props.competition.id)
    if (selectedRow.value) {
      selectedRow.value = (summary.value?.records || []).find((row) => row.judgePublicId === selectedRow.value.judgePublicId) || selectedRow.value
    }
  } finally {
    loading.value = false
  }
}

function openModal(row) {
  selectedRow.value = row
  editing.value = false
  form.judgmentLevel = row.judgmentLevel || null
  form.feedbackQualityLevel = row.feedbackQualityLevel || null
  form.ruleExecutionLevel = row.ruleExecutionLevel || null
  form.professionalismLevel = row.professionalismLevel || null
  form.evidence = row.evidence || ''
  form.version = row.version || 0
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editing.value = false
  selectedRow.value = null
}

function selectLevel(key, value) {
  if (readonly.value) return
  form[key] = form[key] === value ? null : value
}

function itemContribution(item) {
  const level = levels.find((entry) => entry.value === form[item.key])
  return level ? item.weight * level.ratio : null
}

async function save(status) {
  if (!selectedRow.value || !props.competition?.id) return
  if (status === 'CONFIRMED' && selectedItemCount.value < manualItems.length) {
    ElMessage.warning('确认评价前请完成四项人工评价')
    return
  }
  if (needsEvidence.value && !form.evidence) {
    ElMessage.warning('选择严重不足或优秀时请填写评价依据')
    return
  }
  saving.value = true
  try {
    const judgePublicId = selectedRow.value.judgePublicId
    await saveJudgePerformance(props.competition.id, judgePublicId, { ...form, status })
    await loadData()
    selectedRow.value = (summary.value?.records || []).find((row) => row.judgePublicId === judgePublicId) || null
    form.version = selectedRow.value?.version || 0
    ElMessage.success(status === 'CONFIRMED' ? '评审表现已确认' : '评审表现草稿已保存')
    if (status === 'CONFIRMED') closeModal()
  } finally {
    saving.value = false
  }
}

function initial(name) { return String(name || '评').trim().slice(0, 1) || '评' }
function formatNumber(value) { return value == null ? '-' : Number(value).toFixed(1) }
function barWidth(value) { return `${Math.min(Math.max(Number(value) || 0, 0), 100)}%` }
function isUnderCompleted(row) {
  const total = Number(row?.taskTotalCount) || 0
  return total > 0 && (Number(row?.taskCompletedCount) || 0) < total
}
function rankTone(rank) { return Number(rank) >= 1 && Number(rank) <= 3 ? `top-${rank}` : '' }
function statusLabel(value) { return ({ UNRATED: '待评价', DRAFT: '草稿', CONFIRMED: '已确认' })[value] || '待评价' }
function statusTone(value) { return ({ UNRATED: 'pending', DRAFT: 'draft', CONFIRMED: 'confirmed' })[value] || 'pending' }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
</script>

<style scoped>
.judge-performance-panel {
  --line: rgba(219, 232, 237, 0.1);
  --line-strong: rgba(219, 232, 237, 0.16);
  --text: #e6edf0;
  --muted: #8da1aa;
  --faint: #71868e;
  --gold: #e0b84a;
  --green: #6fcf7a;
  --row: rgba(255, 255, 255, 0.026);
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  color: var(--text);
}

h2,
h3 {
  margin: 0;
}

button,
input,
select,
textarea {
  font: inherit;
}

svg {
  width: 1em;
  height: 1em;
}

.panel-toolbar,
.panel-heading,
.status-chips,
.panel-actions,
.search-control,
.score-line,
.row-actions,
.column-title,
.manual-item-label,
.task-line,
.modal-identity,
.modal-metrics,
.remark-block,
.foot-actions,
.foot-meta {
  display: flex;
  align-items: center;
}

.panel-toolbar {
  flex: 0 0 auto;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 14px;
}

.panel-heading {
  gap: 14px;
  flex-wrap: wrap;
}

.panel-heading h2 {
  font-size: 22px;
}

.status-chips {
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  padding: 4px 10px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.035);
  color: var(--muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.chip.confirmed {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.22);
}

.chip.draft {
  color: var(--gold);
  border-color: rgba(216, 169, 53, 0.24);
}

.chip.candidate {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.3);
  background: rgba(111, 207, 122, 0.09);
}

.panel-actions {
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.search-control {
  gap: 8px;
  height: 40px;
  width: 220px;
  padding: 0 10px;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
  color: var(--muted);
}

.search-control input,
.panel-actions select {
  height: 40px;
  padding: 0 10px;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: #182327;
  outline: 0;
}

.search-control input {
  width: 100%;
  border: 0;
  padding: 0;
  background: transparent;
}

.tool-button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 12px;
  color: #dbe6e9;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.tool-button.primary {
  color: var(--gold);
  border-color: rgba(216, 169, 53, 0.38);
  background: rgba(216, 169, 53, 0.09);
}

.tool-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.state-note {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  color: var(--gold);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  font-size: 13px;
}

.performance-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.88);
}

.performance-table-head,
.performance-row {
  display: grid;
  grid-template-columns: 56px minmax(190px, 1.4fr) minmax(150px, 1fr) minmax(140px, 0.9fr) 84px minmax(130px, 0.8fr) 84px;
  align-items: center;
  column-gap: 16px;
  padding: 0 18px;
}

.performance-table-head {
  position: sticky;
  top: 0;
  z-index: 1;
  min-height: 44px;
  color: var(--muted);
  background: #172125;
  font-size: 12px;
}

.performance-row {
  min-height: 68px;
  border-top: 1px solid rgba(219, 232, 237, 0.07);
  transition: background 0.16s ease;
}

.performance-row:hover {
  background: var(--row);
}

.rank-cell {
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--muted);
}

.rank-cell.top-1 { color: var(--gold); }
.rank-cell.top-2 { color: #cdd8dc; }
.rank-cell.top-3 { color: #d09a63; }

.judge-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.avatar {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--gold);
  font-weight: 800;
  border: 1px solid rgba(216, 169, 53, 0.3);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
}

.judge-cell div,
.manual-cell,
.comment-cell,
.total-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
}

.judge-cell strong,
.judge-cell small,
.comment-cell > small {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.judge-cell small,
.manual-cell small,
.comment-cell > small {
  color: var(--muted);
  font-size: 12px;
}

.completion-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--faint);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.completion-cell small {
  color: var(--faint);
  font-size: 11px;
}

.completion-cell.incomplete span,
.completion-cell.incomplete small {
  color: #f1bd79;
  font-weight: 700;
}

.score-line {
  gap: 6px;
  align-items: baseline;
}

.score-line strong {
  font-size: 17px;
  font-variant-numeric: tabular-nums;
}

.score-line small {
  color: var(--muted);
  font-size: 12px;
}

.total-cell .score-line strong {
  color: var(--gold);
  font-size: 21px;
}

.total-cell .muted,
.pending-text {
  color: var(--muted);
  font-size: 14px;
  font-weight: 600;
}

.completion-bar {
  display: block;
  width: 100%;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
}

.completion-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(216, 169, 53, 0.55), var(--gold));
}

.candidate-badge,
.sample-badge {
  padding: 3px 7px;
  border-radius: 5px;
  font-size: 11px;
  white-space: nowrap;
}

.candidate-badge {
  color: var(--green);
  background: rgba(111, 207, 122, 0.12);
}

.sample-badge {
  color: var(--gold);
  background: rgba(216, 169, 53, 0.1);
}

.row-actions {
  justify-content: flex-end;
}

.row-action {
  min-height: 36px;
  padding: 0 12px;
  color: var(--gold);
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.06);
  cursor: pointer;
}

.row-action:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.table-empty {
  display: grid;
  place-items: center;
  min-height: 180px;
  color: var(--muted);
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 26px;
  background: rgba(3, 8, 10, 0.7);
  backdrop-filter: blur(6px);
}

.performance-modal {
  display: flex;
  flex-direction: column;
  width: min(1060px, 100%);
  max-height: min(86vh, 880px);
  overflow: hidden;
  border: 1px solid var(--line-strong);
  border-radius: 10px;
  background: #111c20;
  box-shadow: 0 30px 90px rgba(0, 0, 0, 0.5);
}

.modal-head {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: minmax(0, auto) minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.02);
}

.modal-identity {
  gap: 12px;
  min-width: 0;
}

.modal-identity h2 {
  font-size: 20px;
  white-space: nowrap;
}

.modal-identity small {
  color: var(--muted);
  font-size: 12px;
}

.status-chip {
  margin-left: 4px;
  padding: 4px 9px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.status-chip.pending { color: var(--gold); border: 1px solid rgba(216, 169, 53, 0.26); background: rgba(216, 169, 53, 0.09); }
.status-chip.draft { color: #f7d774; border: 1px solid rgba(247, 215, 116, 0.24); background: rgba(247, 215, 116, 0.08); }
.status-chip.confirmed { color: var(--green); border: 1px solid rgba(111, 207, 122, 0.3); background: rgba(111, 207, 122, 0.1); }

.modal-metrics {
  justify-content: flex-end;
  gap: 26px;
}

.modal-metrics div {
  display: flex;
  align-items: baseline;
  gap: 5px;
  white-space: nowrap;
}

.modal-metrics span {
  margin-right: 2px;
  color: var(--muted);
  font-size: 12px;
}

.modal-metrics strong {
  color: var(--gold);
  font-size: 19px;
  font-variant-numeric: tabular-nums;
}

.modal-metrics small {
  color: var(--muted);
  font-size: 12px;
}

.icon-close {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  font-size: 18px;
  cursor: pointer;
}

.modal-body {
  flex: 1 1 auto;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.85fr);
  gap: 16px;
  overflow: auto;
  padding: 18px 22px;
}

.eval-column,
.stat-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.column-title {
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
}

.column-title h3 {
  font-size: 15px;
}

.column-title h3 small {
  margin-left: 5px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 400;
}

.column-title span {
  color: var(--muted);
  font-size: 12px;
}

.column-title span strong {
  margin-left: 6px;
  color: var(--gold);
  font-size: 15px;
  font-variant-numeric: tabular-nums;
}

.manual-item {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.4);
}

.manual-item-label {
  justify-content: space-between;
  gap: 12px;
}

.manual-item-label strong {
  font-size: 14px;
}

.manual-item-label small {
  display: flex;
  gap: 10px;
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.manual-item-label em {
  color: var(--gold);
  font-style: normal;
  font-variant-numeric: tabular-nums;
}

.level-options {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
}

.level-options button {
  min-height: 38px;
  color: #a9bbc2;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.035);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.14s ease, border-color 0.14s ease, background 0.14s ease;
}

.level-options button:hover:not(:disabled) {
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.07);
}

.level-options button.active {
  color: var(--gold);
  border-color: rgba(216, 169, 53, 0.5);
  background: rgba(216, 169, 53, 0.12);
  font-weight: 700;
}

.level-options button:disabled {
  cursor: default;
  opacity: 0.78;
}

.stat-list {
  display: grid;
  gap: 9px;
  margin: 0;
}

.stat-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.stat-list dt {
  color: var(--muted);
}

.stat-list dd {
  margin: 0;
  font-variant-numeric: tabular-nums;
}

.task-block {
  display: grid;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.task-line {
  justify-content: space-between;
  gap: 12px;
  color: var(--muted);
  font-size: 12px;
}

.task-line strong {
  color: var(--text);
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}

.remark-block {
  gap: 8px;
  flex-wrap: wrap;
}

.evidence-field {
  grid-column: 1 / -1;
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.025);
}

.evidence-field > span {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  font-weight: 700;
}

.evidence-field em {
  color: var(--faint);
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.evidence-field em.rule-hint.missing {
  color: #ff9c8a;
}

.evidence-field textarea {
  width: 100%;
  min-height: 96px;
  box-sizing: border-box;
  padding: 11px 12px;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.55);
  outline: 0;
  resize: vertical;
}

.evidence-field textarea:read-only {
  opacity: 0.78;
  cursor: default;
}

.evidence-field > small {
  justify-self: end;
  color: var(--faint);
  font-size: 12px;
}

.modal-foot {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 22px;
  border-top: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.02);
}

.foot-meta {
  gap: 14px;
  color: var(--faint);
  font-size: 12px;
}

.foot-actions {
  gap: 8px;
}

.confirmed-note {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .panel-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .modal-head {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .modal-metrics {
    grid-column: 1 / -1;
    justify-content: flex-start;
    gap: 18px;
  }
}

@media (max-width: 900px) {
  .performance-table-wrap {
    overflow-x: auto;
  }

  .performance-table-head,
  .performance-row {
    min-width: 940px;
  }

  .search-control {
    width: 100%;
  }

  .modal-mask {
    padding: 14px;
  }

  .modal-body {
    grid-template-columns: minmax(0, 1fr);
  }

  .modal-foot {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
