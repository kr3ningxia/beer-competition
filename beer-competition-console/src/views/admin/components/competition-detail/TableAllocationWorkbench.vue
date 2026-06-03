<template>
  <section class="allocation-workbench">
    <section v-if="allocationMode === 'judges'" class="allocation-grid">
      <aside class="resource-panel">
        <div class="panel-head">
          <strong>评审池</strong>
          <span>{{ filteredJudgePool.length }} 人</span>
        </div>
        <label class="search-box">
          <Search />
          <input v-model="judgeKeywordModel" placeholder="搜索姓名、手机号、资质" />
        </label>
        <div class="filter-row primary-filter-row">
          <button
            v-for="filter in roleFilters"
            :key="filter.value"
            :class="{ active: judgeRoleFilter === filter.value }"
            type="button"
            @click="$emit('update:judgeRoleFilter', filter.value)"
          >
            <span>{{ filter.label }}</span>
            <b>{{ judgeFilterCounts[filter.value] || 0 }}</b>
          </button>
        </div>
        <div class="resource-list">
          <article
            v-for="judge in filteredJudgePool"
            :key="judge.publicId"
            :class="['resource-card', { assigned: isAssigned(judge.publicId), disabled: !isJudgeActive(judge) }]"
            :draggable="editableJudges && isJudgeActive(judge)"
            @dragstart="$emit('startJudgeDrag', judge)"
            @dragend="$emit('clearDrag')"
          >
            <span class="avatar">{{ getJudgeInitial(judge.name) }}</span>
            <div class="resource-body">
              <strong>{{ judge.name || '未填写姓名' }}</strong>
              <small>{{ judge.qualification || '未填写资质' }}</small>
              <em v-if="getJudgeAssignmentSummary(judge.publicId)">{{ getJudgeAssignmentSummary(judge.publicId) }}</em>
            </div>
            <button type="button" :disabled="!editableJudges || !isJudgeActive(judge)" @click="$emit('addJudgeToTarget', judge)">
              {{ isAssigned(judge.publicId) ? '调整' : '加入' }}
            </button>
          </article>
        </div>
      </aside>

      <main class="table-board judge-board">
        <article
          v-for="(table, index) in judgeTableForm"
          :key="table.localId"
          :class="['desk-card', { active: selectedTableLocalId === table.localId, danger: tableValidationIssues(table).length }]"
        >
          <header class="desk-summary">
            <div class="desk-summary-main">
              <label v-if="editableJudges" class="desk-name-field">
                <span>桌号：</span>
                <input :value="table.tableName" placeholder="例如 A桌" @input="table.tableName = $event.target.value" />
              </label>
              <h3 v-else>{{ table.tableName }}</h3>
            </div>
            <div class="desk-summary-side">
              <em :class="['desk-status', tableValidationIssues(table).length ? 'warning' : 'ok']">
                {{ tableValidationIssues(table)[0] || '人员完整' }}
              </em>
              <button v-if="editableJudges" class="icon-action" type="button" @click="$emit('removeJudgeTable', index)">
                <Delete />
              </button>
            </div>
          </header>
          <section class="role-grid">
            <div
              v-for="role in roleOptions"
              :key="role.value"
              :class="['role-lane', { active: selectedTableLocalId === table.localId && selectedRole === role.value }]"
              @click="$emit('selectAssignmentTarget', table, role.value)"
              @dragover.prevent
              @drop.prevent="$emit('dropOnRole', table, role.value)"
            >
              <header>
                <strong>{{ role.label }}</strong>
                <span>{{ assignmentsForTable(table, role.value).length }} 人</span>
              </header>
              <article
                v-for="assignment in assignmentsForTable(table, role.value)"
                :key="assignment.localId"
                class="mini-card"
                :draggable="editableJudges"
                @dragstart="$emit('startAssignmentDrag', assignment)"
                @dragend="$emit('clearDrag')"
              >
                <span class="avatar small">{{ getJudgeInitial(getJudge(assignment.judgePublicId)?.name) }}</span>
                <div>
                  <strong>{{ getJudge(assignment.judgePublicId)?.name || '未知评审' }}</strong>
                  <small>{{ getJudge(assignment.judgePublicId)?.qualification || '未填写资质' }}</small>
                </div>
                <button v-if="editableJudges" class="icon-action" type="button" @click.stop="$emit('removeAssignment', assignment)">
                  <Delete />
                </button>
              </article>
              <p v-if="assignmentsForTable(table, role.value).length === 0" class="role-empty">
                点击或拖拽到这里分配{{ role.label }}
              </p>
            </div>
          </section>
        </article>
      </main>

      <aside class="control-panel check-panel">
        <section class="control-section">
          <strong>当前轮次</strong>
          <div class="round-switch" aria-label="当前轮次">
            <button
              v-for="round in rounds"
              :key="round.id"
              :class="{ active: round.id === activeRoundId }"
              type="button"
              @click="$emit('selectRound', round.id)"
            >
              {{ round.name }}
            </button>
          </div>
        </section>
        <section class="control-section">
          <strong>分配对象</strong>
          <div class="mode-switch" aria-label="编辑对象">
            <button :class="{ active: allocationMode === 'judges' }" type="button" @click="$emit('update:allocationMode', 'judges')">评审</button>
            <button :class="{ active: allocationMode === 'entries' }" type="button" @click="$emit('update:allocationMode', 'entries')">酒款</button>
            <button :class="{ active: allocationMode === 'overview' }" type="button" @click="$emit('update:allocationMode', 'overview')">总览</button>
          </div>
        </section>
        <section class="control-section">
          <strong>操作</strong>
          <button
            v-if="!firstRoundExists"
            class="main-action"
            type="button"
            :disabled="validationIssues.length > 0"
            @click="$emit('generateFirstRound')"
          >
            生成第一轮编排
          </button>
          <p v-else class="ok">
            <CircleCheck />
            <span>第一轮编排已生成。</span>
          </p>
          <button v-if="editableJudges" class="add-desk" type="button" @click="$emit('addJudgeTable')">新增基础桌</button>
          <button v-if="editableJudges" type="button" @click="$emit('saveJudgeDraft')">保存评审人员</button>
        </section>
        <section class="control-section">
        <strong>人员检查</strong>
        <p v-for="issue in validationIssues" :key="issue" class="warning">
          <Warning />
          <span>{{ issue }}</span>
        </p>
        <p v-if="validationIssues.length === 0 && !firstRoundExists" class="ok">
          <CircleCheck />
          <span>可以生成第一轮编排。</span>
        </p>
        <p v-if="validationIssues.length === 0 && firstRoundExists" class="ok">
          <CircleCheck />
          <span>可以继续调整第一轮草稿。</span>
        </p>
        </section>
      </aside>
    </section>

    <section v-else-if="allocationMode === 'entries'" class="allocation-grid">
      <aside class="resource-panel">
        <div class="panel-head">
          <strong>{{ currentRound?.type === 'SCORE' ? '已入库酒款' : '晋级酒款' }}</strong>
          <span>{{ filteredRoundPool.length }} 款</span>
        </div>
        <label class="search-box">
          <Search />
          <input v-model="roundKeywordModel" placeholder="搜索编号、短编号、风格" />
        </label>
        <div class="filter-row">
          <button
            v-for="category in roundCategoryFilters"
            :key="category"
            :class="{ active: roundCategoryFilter === category }"
            type="button"
            @click="$emit('update:roundCategoryFilter', category)"
          >
            {{ category }}
          </button>
        </div>
        <div v-if="currentRound?.type === 'SCORE'" class="bulk-row">
          <button type="button" :disabled="!selectedEntryUuids.length" @click="$emit('addSelectedToTable')">加入选中桌</button>
          <button type="button" @click="$emit('assignEvenly')">平均分配</button>
          <button type="button" @click="$emit('assignByCategory')">按组别分配</button>
          <button type="button" @click="$emit('clearRoundAssignments')">清空</button>
        </div>
        <div class="resource-list">
          <article
            v-for="entry in filteredRoundPool"
            :key="entry.uuid"
            :class="['resource-card entry', { assigned: getRoundEntryAssignment(entry.uuid), selected: selectedEntryUuids.includes(entry.uuid) }]"
            draggable="true"
            @dragstart="$emit('startEntryDrag', entry.uuid)"
            @dragend="$emit('clearDrag')"
          >
            <input type="checkbox" :checked="selectedEntryUuids.includes(entry.uuid)" @change="$emit('toggleEntrySelection', entry.uuid)" />
            <div>
              <strong>{{ entry.uuid }}</strong>
              <small>{{ entry.shortCode }} · {{ entry.categoryName }} · {{ entry.style }}</small>
              <em>{{ getRoundEntryAssignment(entry.uuid) || '未分配' }}</em>
            </div>
            <button type="button" :disabled="!selectedRoundTableId" @click="$emit('addEntryToSelectedTable', entry.uuid)">加入</button>
          </article>
        </div>
      </aside>

      <main class="table-board">
        <article
          v-for="table in currentRoundTables"
          :key="table.id"
          :class="['desk-card', { active: selectedRoundTableId === table.id, danger: getRoundTableIssues(table).length }]"
          @click="$emit('selectRoundTable', table.id)"
          @dragover.prevent
          @drop.prevent="$emit('dropEntryOnRoundTable', table.id)"
        >
          <header>
            <div>
              <span>{{ currentRound?.type === 'SCORE' ? '评分桌' : '排序桌' }}</span>
              <h3>{{ table.name }}</h3>
            </div>
            <em>{{ table.entryUuids.length }} 款</em>
          </header>
          <div class="desk-meta">
            <span>桌长 <strong>{{ getJudge(table.captainPublicId)?.name || '未指定' }}</strong></span>
            <label class="inline-target" @click.stop>
              <span>{{ currentRound?.type === 'SCORE' ? '晋级' : '排序目标' }}</span>
              <input
                :value="table.targetCount"
                min="1"
                type="number"
                @input="$emit('updateTableTarget', table.id, Number($event.target.value || 1))"
              />
            </label>
          </div>
          <div class="entry-list">
            <article v-for="uuid in table.entryUuids" :key="uuid">
              <span>{{ uuid }}</span>
              <button type="button" @click.stop="$emit('removeEntryFromRoundTable', table.id, uuid)">移除</button>
            </article>
            <p v-if="!table.entryUuids.length">从左侧加入酒款。</p>
          </div>
        </article>
      </main>

      <aside class="control-panel check-panel">
        <section class="control-section">
          <strong>当前轮次</strong>
          <div class="round-switch" aria-label="当前轮次">
            <button
              v-for="round in rounds"
              :key="round.id"
              :class="{ active: round.id === activeRoundId }"
              type="button"
              @click="$emit('selectRound', round.id)"
            >
              {{ round.name }}
            </button>
          </div>
        </section>
        <section class="control-section">
          <strong>分配对象</strong>
          <div class="mode-switch" aria-label="编辑对象">
            <button :class="{ active: allocationMode === 'judges' }" type="button" @click="$emit('update:allocationMode', 'judges')">评审</button>
            <button :class="{ active: allocationMode === 'entries' }" type="button" @click="$emit('update:allocationMode', 'entries')">酒款</button>
            <button :class="{ active: allocationMode === 'overview' }" type="button" @click="$emit('update:allocationMode', 'overview')">总览</button>
          </div>
        </section>
        <section class="control-section">
          <strong>操作</strong>
          <button
            v-if="currentRound"
            class="main-action"
            type="button"
            :disabled="!canPublish"
            @click="$emit('publishCurrentRound')"
          >
            {{ currentRound?.type === 'SCORE' ? '发布给评委' : '发布给桌长' }}
          </button>
        </section>
        <section class="control-section">
          <strong>{{ currentRound?.name }}检查</strong>
          <p v-for="issue in roundValidationIssues" :key="issue" class="warning">
            <Warning />
            <span>{{ issue }}</span>
          </p>
          <p v-if="roundValidationIssues.length === 0" class="ok">
            <CircleCheck />
            <span>当前轮次可以发布。</span>
          </p>
        </section>
      </aside>
    </section>

    <section v-else class="allocation-grid overview-shell">
      <main class="overview-board">
        <section class="overview-summary">
          <article>
            <span>桌数</span>
            <strong>{{ currentRoundTables.length }}</strong>
          </article>
          <article>
            <span>评审</span>
            <strong>{{ overviewMetrics.assignedJudges }}</strong>
          </article>
          <article>
            <span>酒款</span>
            <strong>{{ overviewMetrics.assignedEntries }} / {{ currentPoolEntries.length }}</strong>
          </article>
          <article :class="{ warning: overviewMetrics.unassignedEntries > 0 }">
            <span>未分配</span>
            <strong>{{ overviewMetrics.unassignedEntries }}</strong>
          </article>
          <article :class="overviewMetrics.issueCount ? 'warning' : 'ok'">
            <span>问题</span>
            <strong>{{ overviewMetrics.issueCount }}</strong>
          </article>
          <article :class="canPublish ? 'ok' : 'warning'">
            <span>状态</span>
            <strong>{{ canPublish ? '可发布' : '需处理' }}</strong>
          </article>
        </section>

        <section class="overview-grid">
          <article
            v-for="table in currentRoundTables"
            :key="table.id"
            :class="['overview-card', { danger: getOverviewTableIssues(table).length }]"
          >
            <header>
              <div>
                <h3>{{ table.name }}</h3>
                <small>{{ table.entryUuids.length }} 款 · {{ currentRound?.type === 'SCORE' ? '晋级' : '排序目标' }} {{ table.targetCount }}</small>
              </div>
              <em :class="getOverviewTableIssues(table).length ? 'warning' : 'ok'">
                {{ getOverviewTableIssues(table).length ? '需处理' : '已就绪' }}
              </em>
            </header>

            <div class="overview-line">
              <span>桌长</span>
              <strong>{{ getJudge(table.captainPublicId)?.name || '未指定' }}</strong>
            </div>

            <div class="overview-block">
              <span>评审</span>
              <p>{{ formatJudgeNames(table) }}</p>
              <small>{{ formatRoleSummary(table) }}</small>
            </div>

            <div class="overview-block">
              <span>酒款</span>
              <p>{{ formatEntryCodes(table) }}</p>
              <small>{{ formatCategorySummary(table) }}</small>
            </div>

            <p v-if="getOverviewTableIssues(table).length" class="overview-issue">
              <Warning />
              <span>{{ getOverviewTableIssues(table)[0] }}</span>
            </p>
          </article>
        </section>
      </main>
      <aside class="control-panel check-panel">
        <section class="control-section">
          <strong>当前轮次</strong>
          <div class="round-switch" aria-label="当前轮次">
            <button
              v-for="round in rounds"
              :key="round.id"
              :class="{ active: round.id === activeRoundId }"
              type="button"
              @click="$emit('selectRound', round.id)"
            >
              {{ round.name }}
            </button>
          </div>
        </section>
        <section class="control-section">
          <strong>分配对象</strong>
          <div class="mode-switch" aria-label="编辑对象">
            <button :class="{ active: allocationMode === 'judges' }" type="button" @click="$emit('update:allocationMode', 'judges')">评审</button>
            <button :class="{ active: allocationMode === 'entries' }" type="button" @click="$emit('update:allocationMode', 'entries')">酒款</button>
            <button :class="{ active: allocationMode === 'overview' }" type="button" @click="$emit('update:allocationMode', 'overview')">总览</button>
          </div>
        </section>
      </aside>
    </section>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheck, Delete, Search, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  allocationMode: { type: String, default: 'judges' },
  rounds: { type: Array, required: true },
  activeRoundId: { type: String, default: '' },
  currentRound: { type: Object, default: null },
  currentRoundTables: { type: Array, required: true },
  selectedRoundTable: { type: Object, default: null },
  selectedRoundTableId: { type: String, default: '' },
  editableJudges: Boolean,
  judgeTableForm: { type: Array, required: true },
  roleOptions: { type: Array, required: true },
  roleFilters: { type: Array, required: true },
  judgeFilterCounts: { type: Object, required: true },
  judgeKeyword: { type: String, default: '' },
  judgeRoleFilter: { type: String, default: 'ALL' },
  selectedTableLocalId: { type: [String, Number], default: null },
  selectedRole: { type: String, default: 'CAPTAIN' },
  validationIssues: { type: Array, required: true },
  filteredJudgePool: { type: Array, required: true },
  assignmentsForTable: { type: Function, required: true },
  tableValidationIssues: { type: Function, required: true },
  filteredRoundPool: { type: Array, required: true },
  currentPoolEntries: { type: Array, required: true },
  roundCategoryFilters: { type: Array, required: true },
  roundCategoryFilter: { type: String, default: '全部' },
  roundKeyword: { type: String, default: '' },
  selectedEntryUuids: { type: Array, default: () => [] },
  captainCandidates: { type: Array, required: true },
  roundValidationIssues: { type: Array, required: true },
  canPublish: Boolean,
  getJudge: { type: Function, required: true },
  getJudgeInitial: { type: Function, required: true },
  getJudgeAssignmentSummary: { type: Function, required: true },
  isAssigned: { type: Function, required: true },
  isJudgeActive: { type: Function, required: true },
  getRoundEntryAssignment: { type: Function, required: true },
  getRoundTableIssues: { type: Function, required: true },
})

const emit = defineEmits([
  'update:allocationMode',
  'selectRound',
  'update:judgeKeyword',
  'update:judgeRoleFilter',
  'update:roundKeyword',
  'update:roundCategoryFilter',
  'selectAssignmentTarget',
  'addJudgeToTarget',
  'addJudgeTable',
  'removeJudgeTable',
  'removeAssignment',
  'saveJudgeDraft',
  'startJudgeDrag',
  'startAssignmentDrag',
  'dropOnRole',
  'clearDrag',
  'generateFirstRound',
  'toggleEntrySelection',
  'addSelectedToTable',
  'addEntryToSelectedTable',
  'startEntryDrag',
  'assignEvenly',
  'assignByCategory',
  'clearRoundAssignments',
  'selectRoundTable',
  'dropEntryOnRoundTable',
  'removeEntryFromRoundTable',
  'updateTableCaptain',
  'updateTableTarget',
  'publishCurrentRound',
])

const judgeKeywordModel = computed({
  get: () => props.judgeKeyword,
  set: (value) => emit('update:judgeKeyword', value),
})

const roundKeywordModel = computed({
  get: () => props.roundKeyword,
  set: (value) => emit('update:roundKeyword', value),
})

const firstRoundExists = computed(() => props.rounds.some((round) => round.roundNo === 1))

const overviewMetrics = computed(() => {
  const assignedEntries = new Set(props.currentRoundTables.flatMap((table) => table.entryUuids)).size
  const assignedJudges = props.currentRound?.type === 'SCORE'
    ? props.judgeTableForm.reduce((sum, table) => sum + props.assignmentsForTable(table).length, 0)
    : props.currentRoundTables.filter((table) => table.captainPublicId).length
  const issueCount = props.roundValidationIssues.length + props.validationIssues.length
  return {
    assignedEntries,
    assignedJudges,
    unassignedEntries: Math.max(props.currentPoolEntries.length - assignedEntries, 0),
    issueCount,
  }
})

function getEntry(uuid) {
  return props.currentPoolEntries.find((entry) => entry.uuid === uuid)
}

function getJudgeTable(roundTable) {
  return props.judgeTableForm.find((table) => table.tableName === roundTable.name)
}

function getTableJudgeAssignments(roundTable) {
  const judgeTable = getJudgeTable(roundTable)
  if (judgeTable) return props.assignmentsForTable(judgeTable)
  return roundTable.captainPublicId ? [{ judgePublicId: roundTable.captainPublicId, role: 'CAPTAIN' }] : []
}

function formatJudgeNames(roundTable) {
  const names = getTableJudgeAssignments(roundTable)
    .map((assignment) => props.getJudge(assignment.judgePublicId)?.name)
    .filter(Boolean)
  if (!names.length) return '未安排评审'
  return names.length > 4 ? `${names.slice(0, 4).join(' · ')} +${names.length - 4}` : names.join(' · ')
}

function formatRoleSummary(roundTable) {
  const assignments = getTableJudgeAssignments(roundTable)
  if (!assignments.length) return '桌长0 / 专业0 / 跨界0'
  const counts = assignments.reduce((map, assignment) => {
    map[assignment.role] = (map[assignment.role] || 0) + 1
    return map
  }, {})
  return `桌长${counts.CAPTAIN || 0} / 专业${counts.PROFESSIONAL || 0} / 跨界${counts.CROSS || 0}`
}

function formatEntryCodes(roundTable) {
  const codes = roundTable.entryUuids
    .map((uuid) => getEntry(uuid)?.shortCode || uuid)
    .filter(Boolean)
  if (!codes.length) return '未分配酒款'
  return codes.length > 6 ? `${codes.slice(0, 6).join(' · ')} +${codes.length - 6}` : codes.join(' · ')
}

function formatCategorySummary(roundTable) {
  const map = new Map()
  roundTable.entryUuids.forEach((uuid) => {
    const entry = getEntry(uuid)
    if (!entry?.categoryName) return
    map.set(entry.categoryName, (map.get(entry.categoryName) || 0) + 1)
  })
  if (!map.size) return '暂无组别分布'
  return [...map.entries()].map(([category, count]) => `${category} ${count}`).join(' · ')
}

function getOverviewTableIssues(roundTable) {
  const issues = [...props.getRoundTableIssues(roundTable)]
  if (props.currentRound?.type === 'SCORE') {
    const judgeTable = getJudgeTable(roundTable)
    if (judgeTable) issues.push(...props.tableValidationIssues(judgeTable))
    else issues.push(`${roundTable.name}缺少对应评审桌`)
  }
  return [...new Set(issues)]
}
</script>

<style scoped>
.allocation-workbench,
.allocation-grid,
.resource-panel,
.resource-list,
.table-board,
.check-panel,
.control-section,
.entry-list,
.overview-board,
.overview-grid {
  display: grid;
  gap: 10px;
}

.resource-list,
.table-board,
.check-panel {
  scrollbar-width: thin;
  scrollbar-color: rgba(216, 169, 53, 0.46) rgba(255, 255, 255, 0.04);
}

.resource-list::-webkit-scrollbar,
.table-board::-webkit-scrollbar,
.check-panel::-webkit-scrollbar {
  width: 8px;
}

.resource-list::-webkit-scrollbar-track,
.table-board::-webkit-scrollbar-track,
.check-panel::-webkit-scrollbar-track {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.035);
}

.resource-list::-webkit-scrollbar-thumb,
.table-board::-webkit-scrollbar-thumb,
.check-panel::-webkit-scrollbar-thumb {
  border: 1px solid rgba(9, 16, 20, 0.9);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(216, 169, 53, 0.64), rgba(216, 169, 53, 0.3));
}

.resource-list::-webkit-scrollbar-thumb:hover,
.table-board::-webkit-scrollbar-thumb:hover,
.check-panel::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, rgba(224, 184, 74, 0.78), rgba(216, 169, 53, 0.42));
}

.round-switch,
.mode-switch,
.filter-row,
.bulk-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

button,
input,
select {
  font: inherit;
}

input[type="checkbox"] {
  position: relative;
  width: 16px;
  min-width: 16px;
  height: 16px;
  min-height: 16px;
  margin: 0;
  border: 1px solid rgba(219, 232, 237, 0.2);
  border-radius: 5px;
  appearance: none;
  outline: 0;
  background: rgba(7, 14, 17, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
  cursor: pointer;
}

input[type="checkbox"]:hover {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(16, 25, 29, 0.9);
}

input[type="checkbox"]:focus-visible {
  border-color: rgba(224, 184, 74, 0.56);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.12);
}

input[type="checkbox"]:checked {
  border-color: rgba(216, 169, 53, 0.64);
  background: linear-gradient(180deg, rgba(216, 169, 53, 0.24), rgba(216, 169, 53, 0.48));
  box-shadow: inset 0 0 0 1px rgba(255, 248, 220, 0.08), 0 0 0 1px rgba(216, 169, 53, 0.14);
}

input[type="checkbox"]:checked::after {
  content: '';
  position: absolute;
  top: 1px;
  left: 4px;
  width: 4px;
  height: 8px;
  border: solid #fff5d7;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

button {
  min-height: 34px;
  padding: 0 10px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.round-switch button.active,
.mode-switch button.active,
.filter-row button.active,
.main-action {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
  font-weight: 800;
}

.allocation-grid {
  grid-template-columns: 320px minmax(0, 1fr) 300px;
  align-items: start;
}

.overview-shell {
  grid-template-columns: minmax(0, 1fr) 300px;
}

.resource-panel,
.desk-card,
.check-panel,
.overview-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.9);
}

.panel-head,
.desk-card header,
.role-lane header,
.resource-card,
.mini-card,
.check-panel p {
  display: flex;
  align-items: center;
}

.panel-head,
.desk-card header,
.role-lane header {
  justify-content: space-between;
  gap: 10px;
}

.panel-head span,
small,
em,
.desk-card header span,
dt,
.stack-field span {
  color: #8da1aa;
}

.search-box {
  position: relative;
}

.search-box :deep(svg) {
  position: absolute;
  top: 50%;
  left: 10px;
  width: 16px;
  height: 16px;
  color: #8da1aa;
  transform: translateY(-50%);
}

.search-box input,
.desk-card input,
.stack-field input,
.stack-field select {
  width: 100%;
  box-sizing: border-box;
  min-height: 38px;
  padding: 0 10px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: 0;
  background: rgba(7, 14, 17, 0.72);
}

.search-box input {
  padding-left: 32px;
}

.filter-row button,
.bulk-row button {
  min-height: 30px;
  color: #8da1aa;
  font-size: 12px;
}

.primary-filter-row {
  margin-bottom: 2px;
}

.filter-row button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.filter-row button b {
  min-width: 18px;
  color: inherit;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.resource-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 9px;
  padding: 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.resource-card.entry {
  grid-template-columns: 18px minmax(0, 1fr) auto;
  align-items: center;
}

.resource-card.assigned {
  border-color: rgba(219, 232, 237, 0.1);
  background: rgba(255, 255, 255, 0.02);
}

.resource-card.assigned .avatar,
.resource-card.assigned strong {
  opacity: 0.88;
}

.resource-body {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.resource-body em {
  font-size: 12px;
  color: #6fcf7a;
}

.resource-card.selected {
  border-color: rgba(216, 169, 53, 0.34);
}

.resource-card.disabled {
  opacity: 0.52;
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: #e0b84a;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
  font-weight: 900;
}

.avatar.small {
  width: 28px;
  height: 28px;
}

.resource-card div,
.mini-card div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.resource-card strong,
.resource-card small,
.resource-card em,
.mini-card strong,
.mini-card small,
.entry-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-board {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.judge-board {
  grid-template-columns: 1fr;
}

.desk-card {
  display: grid;
  gap: 8px;
}

.desk-card.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.045);
}

.desk-card.danger {
  border-color: rgba(242, 153, 74, 0.26);
}

.desk-card h3,
.overview-card h3,
dl,
dd,
p {
  margin: 0;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.role-lane {
  display: grid;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.role-lane.active {
  border-color: rgba(216, 169, 53, 0.34);
}

.mini-card {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 6px;
  padding: 6px 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.desk-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: start;
}

.desk-summary-main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.desk-summary-side {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 8px;
}

.desk-name-field {
  display: grid;
  grid-template-columns: 52px minmax(0, 120px);
  gap: 8px;
  align-items: center;
}

.desk-name-field span {
  color: #8da1aa;
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.desk-status {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.026);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.desk-status.ok {
  color: #6fcf7a;
  border-color: rgba(111, 207, 122, 0.2);
  background: rgba(111, 207, 122, 0.07);
}

.desk-status.warning {
  color: #f1bd79;
  border-color: rgba(242, 153, 74, 0.22);
  background: rgba(242, 153, 74, 0.08);
}

.role-empty {
  padding: 6px 2px 2px;
  color: #6f848d;
  font-size: 12px;
  line-height: 1.5;
}

.icon-action {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 0;
}

.icon-action :deep(svg),
.check-panel :deep(svg) {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

.add-desk {
  color: #e0b84a;
  border-style: dashed;
}

.control-panel {
  align-content: start;
}

.control-section {
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
}

.control-section:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.control-section > strong {
  font-size: 14px;
}

.control-section > button,
.control-section .main-action {
  width: 100%;
}

.control-section .round-switch,
.control-section .mode-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.control-section .round-switch {
  grid-template-columns: repeat(auto-fit, minmax(76px, 1fr));
}

.desk-meta,
.overview-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.desk-meta span,
.inline-target,
.entry-list article,
.overview-card dl div {
  padding: 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.inline-target {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 58px;
  gap: 8px;
  align-items: center;
}

.inline-target span {
  color: #8da1aa;
}

.inline-target input {
  width: 100%;
  box-sizing: border-box;
  min-height: 28px;
  padding: 0 6px;
  color: #e6edf0;
  text-align: center;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  outline: 0;
  background: rgba(7, 14, 17, 0.72);
  font-weight: 800;
}

.entry-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.entry-list button {
  min-height: 28px;
  color: #e0b84a;
}

.stack-field {
  display: grid;
  gap: 6px;
}

.check-panel {
  position: sticky;
  top: 0;
}

.check-panel p {
  gap: 8px;
  padding: 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.warning {
  color: #f1bd79;
}

.ok {
  color: #6fcf7a;
}

.overview-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.overview-card {
  display: grid;
  gap: 10px;
}

.overview-card.danger {
  border-color: rgba(242, 153, 74, 0.3);
}

.overview-card header,
.overview-line,
.overview-issue {
  display: flex;
  align-items: center;
}

.overview-card header,
.overview-line {
  justify-content: space-between;
  gap: 10px;
}

.overview-card header em {
  flex: 0 0 auto;
  padding: 5px 8px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  font-style: normal;
  font-weight: 800;
}

.overview-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}

.overview-summary article,
.overview-line,
.overview-block {
  min-width: 0;
  padding: 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.overview-summary article {
  display: grid;
  gap: 4px;
}

.overview-summary span,
.overview-line span,
.overview-block span,
.overview-card small {
  color: #8da1aa;
}

.overview-summary strong {
  color: #e6edf0;
  font-size: 18px;
}

.overview-block {
  display: grid;
  gap: 5px;
}

.overview-block p,
.overview-block small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-issue {
  gap: 7px;
  color: #f1bd79;
}

.overview-issue :deep(svg) {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

@media (max-width: 1180px) {
  .allocation-grid,
  .overview-shell,
  .table-board,
  .overview-grid,
  .overview-summary,
  .role-grid {
    grid-template-columns: 1fr;
  }

  .check-panel {
    position: static;
  }
}

@media (max-width: 1440px) {
  .desk-summary {
    grid-template-columns: 1fr;
  }

  .desk-summary-side,
  .desk-role-counts {
    justify-content: flex-start;
  }

  .desk-name-field {
    grid-template-columns: 52px minmax(0, 120px);
  }
}
</style>
