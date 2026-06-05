<template>
  <section class="first-round-workbench">
    <header class="round-hero">
      <div>
        <small>第一轮</small>
        <h2>把已入库酒款分到评审桌</h2>
        <p>第一轮会发给评审评分，桌长最后汇总并选择晋级酒款。</p>
      </div>
      <button class="primary-action" type="button" :disabled="!canPublish" @click="$emit('publishCurrentRound')">
        {{ publishText }}
      </button>
    </header>

    <div class="round-summary-strip">
      <span>桌数 <strong>{{ currentRoundTables.length }}</strong></span>
      <span>已分配 <strong>{{ assignedCount }}</strong></span>
      <span>未分配 <strong>{{ unassignedCount }}</strong></span>
      <span>晋级目标 <strong>{{ targetCount }}</strong></span>
      <span>状态 <strong>{{ statusText }}</strong></span>
    </div>

    <section class="first-round-grid">
      <EntryPoolPanel
        title="已入库酒款池"
        is-score-round
        :filtered-entries="filteredRoundPool"
        :category-stats="categoryStats"
        :round-category-filters="roundCategoryFilters"
        :round-category-filter="roundCategoryFilter"
        :round-keyword="roundKeyword"
        :selected-entry-uuids="selectedEntryUuids"
        :selected-round-table-id="selectedRoundTableId"
        :get-round-entry-assignment="getRoundEntryAssignment"
        @update:round-keyword="$emit('update:roundKeyword', $event)"
        @update:round-category-filter="$emit('update:roundCategoryFilter', $event)"
        @toggle-entry-selection="$emit('toggleEntrySelection', $event)"
        @clear-selection="$emit('clearSelection')"
        @add-selected-to-table="$emit('addSelectedToTable')"
        @add-entry-to-selected-table="$emit('addEntryToSelectedTable', $event)"
        @start-entry-drag="$emit('startEntryDrag', $event)"
        @clear-drag="$emit('clearDrag')"
        @assign-evenly="$emit('assignEvenly')"
        @assign-by-category="$emit('assignByCategory')"
        @clear-round-assignments="$emit('clearRoundAssignments')"
      />

      <main class="table-board">
        <article
          v-for="table in currentRoundTables"
          :key="table.id"
          :class="['round-table-card', { active: selectedRoundTableId === table.id, danger: getRoundTableIssues(table).length }]"
          @click="$emit('selectRoundTable', table.id)"
          @dragover.prevent
          @drop.prevent="$emit('dropEntryOnRoundTable', table.id)"
        >
          <header>
            <div>
              <small>评分制</small>
              <h3>{{ table.name }}</h3>
            </div>
            <em>{{ tableStatusLabel(table.status) }}</em>
          </header>
          <div class="table-meta">
            <span>桌长 <strong>{{ getJudge(table.captainPublicId)?.name || '未指定' }}</strong></span>
            <span>酒款 <strong>{{ table.entryUuids.length }}</strong></span>
            <span>晋级 <strong>{{ table.targetCount }}</strong></span>
          </div>
          <div class="member-line">
            <span>评审</span>
            <strong>{{ table.professionalCount || 0 }} 专业 · {{ table.crossCount || 0 }} 跨界</strong>
          </div>
          <div class="entry-list">
            <article v-for="uuid in table.entryUuids" :key="uuid">
              <span>{{ uuid }}</span>
              <button type="button" @click.stop="$emit('removeEntryFromRoundTable', table.id, uuid)">移除</button>
            </article>
            <p v-if="!table.entryUuids.length">从左侧酒款池加入本桌。</p>
          </div>
          <p v-for="issue in getRoundTableIssues(table)" :key="issue" class="table-warning">
            <Warning />
            {{ issue }}
          </p>
        </article>
      </main>

      <RoundCheckPanel
        title="第一轮发布检查"
        target-label="晋级数量"
        success-text="第一轮可以发布给评审。"
        :selected-round-table="selectedRoundTable"
        :captain-candidates="captainCandidates"
        :round-validation-issues="roundValidationIssues"
        :can-remove-table="currentRound?.status === 'DRAFT'"
        @update-table-captain="(tableId, value) => $emit('updateTableCaptain', tableId, value)"
        @update-table-target="(tableId, value) => $emit('updateTableTarget', tableId, value)"
        @remove-round-table="$emit('removeRoundTable', $event)"
      >
        <button v-if="currentRound?.status === 'PUBLISHED'" class="secondary-action" type="button" @click="$emit('completeFirstRound')">
          确认第一轮完成
        </button>
      </RoundCheckPanel>
    </section>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import EntryPoolPanel from './EntryPoolPanel.vue'
import RoundCheckPanel from './RoundCheckPanel.vue'

const props = defineProps({
  currentRound: { type: Object, default: null },
  currentRoundTables: { type: Array, required: true },
  selectedRoundTable: { type: Object, default: null },
  selectedRoundTableId: { type: String, default: '' },
  filteredRoundPool: { type: Array, required: true },
  categoryStats: { type: Array, required: true },
  roundCategoryFilters: { type: Array, required: true },
  roundCategoryFilter: { type: String, default: '全部' },
  roundKeyword: { type: String, default: '' },
  selectedEntryUuids: { type: Array, default: () => [] },
  captainCandidates: { type: Array, required: true },
  roundValidationIssues: { type: Array, required: true },
  canPublish: Boolean,
  unassignedCount: { type: Number, default: 0 },
  roundStatusLabels: { type: Object, required: true },
  getJudge: { type: Function, required: true },
  getRoundTableIssues: { type: Function, required: true },
  getRoundEntryAssignment: { type: Function, required: true },
})

defineEmits([
  'update:roundKeyword',
  'update:roundCategoryFilter',
  'toggleEntrySelection',
  'clearSelection',
  'addSelectedToTable',
  'addEntryToSelectedTable',
  'startEntryDrag',
  'clearDrag',
  'assignEvenly',
  'assignByCategory',
  'clearRoundAssignments',
  'selectRoundTable',
  'dropEntryOnRoundTable',
  'removeEntryFromRoundTable',
  'updateTableCaptain',
  'updateTableTarget',
  'removeRoundTable',
  'publishCurrentRound',
  'completeFirstRound',
])

const assignedCount = computed(() => new Set(props.currentRoundTables.flatMap((table) => table.entryUuids)).size)
const targetCount = computed(() => props.currentRoundTables.reduce((sum, table) => sum + Number(table.targetCount || 0), 0))
const statusText = computed(() => props.roundStatusLabels[props.currentRound?.status] || props.currentRound?.status || '-')
const publishText = computed(() => {
  if (props.currentRound?.status === 'PUBLISHED') return '已发布'
  if (props.currentRound?.status === 'LOCKED') return '已锁定'
  return '发布第一轮'
})

function tableStatusLabel(status) {
  return props.roundStatusLabels[status] || status
}
</script>

<style scoped>
.first-round-workbench,
.table-board,
.entry-list {
  display: grid;
  gap: 12px;
}

.round-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.82);
}

.round-hero h2,
.round-hero p,
h3 {
  margin: 0;
}

.round-hero small {
  color: #e0b84a;
  font-weight: 800;
}

.round-hero p,
small {
  color: #8da1aa;
}

.primary-action,
.secondary-action {
  min-height: 40px;
  padding: 0 14px;
  color: #e0b84a;
  border: 1px solid rgba(216, 169, 53, 0.34);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.09);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.secondary-action {
  width: 100%;
  color: #e6edf0;
  border-color: rgba(219, 232, 237, 0.12);
  background: rgba(255, 255, 255, 0.035);
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.round-summary-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.9);
}

.round-summary-strip span {
  padding: 7px 10px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.first-round-grid {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr) 300px;
  gap: 10px;
  align-items: start;
}

.table-board {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.round-table-card {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.9);
}

.round-table-card.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.045);
}

.round-table-card.danger:not(.active) {
  border-color: rgba(242, 153, 74, 0.25);
}

.round-table-card header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.round-table-card h3 {
  margin-top: 3px;
  font-size: 22px;
}

.round-table-card em {
  align-self: start;
  padding: 5px 8px;
  color: #e0b84a;
  border: 1px solid rgba(216, 169, 53, 0.24);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.08);
  font-style: normal;
  font-weight: 800;
}

.table-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.table-meta span,
.member-line,
.entry-list article {
  padding: 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.table-meta span,
.member-line {
  display: grid;
  gap: 3px;
}

.member-line span {
  color: #8da1aa;
}

.entry-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.entry-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-list button {
  color: #e0b84a;
  border: 0;
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.entry-list p,
.table-warning {
  color: #f1bd79;
}

.table-warning {
  display: flex;
  gap: 7px;
  align-items: center;
  margin: 0;
  font-size: 12px;
}

.table-warning :deep(svg) {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
}

@media (max-width: 1180px) {
  .first-round-grid,
  .table-board {
    grid-template-columns: 1fr;
  }
}
</style>
