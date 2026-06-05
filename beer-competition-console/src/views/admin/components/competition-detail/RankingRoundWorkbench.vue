<template>
  <section class="ranking-round-workbench">
    <header class="round-hero">
      <div>
        <small>后续轮</small>
        <h2>由桌长选择并排序</h2>
        <p>本轮不会生成普通评委评分任务，只把候选酒款发给桌长处理。</p>
      </div>
      <button class="primary-action" type="button" :disabled="!mainActionEnabled" @click="$emit('rankingMainAction')">
        {{ mainActionText }}
      </button>
    </header>

    <div class="round-summary-strip">
      <span>候选 <strong>{{ candidateCount }}</strong></span>
      <span>桌数 <strong>{{ currentRoundTables.length }}</strong></span>
      <span>{{ targetSummaryLabel }} <strong>{{ targetCount }}</strong></span>
      <span>已选择 <strong>{{ filledCount }}</strong></span>
      <span>状态 <strong>{{ statusText }}</strong></span>
    </div>

    <section class="ranking-grid">
      <EntryPoolPanel
        title="晋级酒款池"
        :filtered-entries="filteredRoundPool"
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
      />

      <main class="ranking-board">
        <article
          v-for="table in currentRoundTables"
          :key="table.id"
          :class="['ranking-table-card', { active: selectedRoundTableId === table.id, danger: getRoundTableIssues(table).length }]"
          @click="$emit('selectRoundTable', table.id)"
          @dragover.prevent
          @drop.prevent="$emit('dropEntryOnRoundTable', table.id)"
        >
          <header>
            <div>
              <small>选择排序</small>
              <h3>{{ table.name }}</h3>
            </div>
            <em>{{ tableStatusLabel(table.status) }}</em>
          </header>
          <div class="table-meta">
            <span>桌长 <strong>{{ getJudge(table.captainPublicId)?.name || '未指定' }}</strong></span>
            <span>候选 <strong>{{ table.entryUuids.length }}</strong></span>
            <span>已选 <strong>{{ getFilledRankingCount(table) }} / {{ table.targetCount }}</strong></span>
          </div>

          <section class="ranking-slots">
            <h4>排序槽位</h4>
            <article v-for="slot in getRankingSlots(table)" :key="slot.rank" :class="{ filled: slot.uuid }">
              <span>
                <strong>{{ slot.label }}</strong>
                <small>{{ slot.uuid || '待选择' }}</small>
              </span>
              <button v-if="slot.uuid" type="button" @click.stop="$emit('clearRankingSlot', table.id, slot.rank)">移出</button>
            </article>
          </section>

          <section class="candidate-list">
            <h4>本桌候选</h4>
            <article v-for="uuid in table.entryUuids" :key="uuid">
              <span>{{ uuid }}</span>
              <div>
                <button
                  v-for="slot in getRankingSlots(table)"
                  :key="slot.rank"
                  type="button"
                  @click.stop="$emit('setRankingSlot', table.id, slot.rank, uuid)"
                >
                  {{ slot.label }}
                </button>
                <button type="button" @click.stop="$emit('removeEntryFromRoundTable', table.id, uuid)">移除</button>
              </div>
            </article>
            <p v-if="!table.entryUuids.length">从左侧晋级池加入本桌。</p>
          </section>

          <p v-for="issue in getRoundTableIssues(table)" :key="issue" class="table-warning">
            <Warning />
            {{ issue }}
          </p>
        </article>
      </main>

      <RoundCheckPanel
        title="后续轮发布检查"
        :target-label="selectedTableTargetLabel"
        :target-hint="selectedTableTargetHint"
        :target-fixed="selectedTableTargetFixed"
        success-text="本轮可以发布给桌长。"
        :selected-round-table="selectedRoundTable"
        :captain-candidates="captainCandidates"
        :round-validation-issues="roundValidationIssues"
        :can-remove-table="currentRound?.status === 'DRAFT'"
        @update-table-captain="(tableId, value) => $emit('updateTableCaptain', tableId, value)"
        @update-table-target="(tableId, value) => $emit('updateTableTarget', tableId, value)"
        @remove-round-table="$emit('removeRoundTable', $event)"
      >
        <button class="secondary-action" type="button" :disabled="!canSubmitRanking" @click="$emit('submitRankingRound')">
          桌长排序已完成
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
  roundCategoryFilters: { type: Array, required: true },
  roundCategoryFilter: { type: String, default: '全部' },
  roundKeyword: { type: String, default: '' },
  selectedEntryUuids: { type: Array, default: () => [] },
  captainCandidates: { type: Array, required: true },
  roundValidationIssues: { type: Array, required: true },
  canPublish: Boolean,
  canSubmitRanking: Boolean,
  roundStatusLabels: { type: Object, required: true },
  getJudge: { type: Function, required: true },
  getRoundTableIssues: { type: Function, required: true },
  getRoundEntryAssignment: { type: Function, required: true },
  getRankingSlots: { type: Function, required: true },
  getFilledRankingCount: { type: Function, required: true },
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
  'selectRoundTable',
  'dropEntryOnRoundTable',
  'removeEntryFromRoundTable',
  'updateTableCaptain',
  'updateTableTarget',
  'removeRoundTable',
  'setRankingSlot',
  'clearRankingSlot',
  'submitRankingRound',
  'rankingMainAction',
])

const candidateCount = computed(() => new Set(props.currentRoundTables.flatMap((table) => table.entryUuids)).size)
const targetCount = computed(() => props.currentRoundTables.reduce((sum, table) => sum + Number(table.targetCount || 0), 0))
const filledCount = computed(() => props.currentRoundTables.reduce((sum, table) => sum + props.getFilledRankingCount(table), 0))
const statusText = computed(() => props.roundStatusLabels[props.currentRound?.status] || props.currentRound?.status || '-')
const targetSummaryLabel = computed(() => {
  const modes = new Set(props.currentRoundTables.map((table) => table.targetMode).filter(Boolean))
  if (modes.size === 1 && modes.has('MEDALS')) return '奖项槽位'
  if (modes.size === 1 && modes.has('CHAMPION')) return '总冠军'
  return '晋级数量'
})
const selectedTableTargetLabel = computed(() => targetCountLabel(props.selectedRoundTable))
const selectedTableTargetHint = computed(() => targetCountHint(props.selectedRoundTable))
const selectedTableTargetFixed = computed(() => isTargetCountFixed(props.selectedRoundTable))
const mainActionText = computed(() => {
  if (props.currentRound?.status === 'SUBMITTED') return '确认排序并锁定'
  if (props.currentRound?.status === 'LOCKED') return '已锁定'
  return '发布给桌长'
})
const mainActionEnabled = computed(() => {
  if (props.currentRound?.status === 'SUBMITTED') return true
  return props.canPublish
})

function tableStatusLabel(status) {
  return props.roundStatusLabels[status] || status
}

function targetCountLabel(table) {
  if (table?.targetMode === 'MEDALS') return '奖项槽位'
  if (table?.targetMode === 'CHAMPION') return '总冠军'
  return '每桌晋级数量'
}

function targetCountHint(table) {
  if (table?.targetMode === 'MEDALS') return '固定为金奖、银奖、铜奖 3 个槽位。'
  if (table?.targetMode === 'CHAMPION') return '总冠军 1 名。'
  return '桌长需要提交并排序的晋级酒款数量。'
}

function isTargetCountFixed(table) {
  return table?.targetMode === 'MEDALS' || table?.targetMode === 'CHAMPION'
}
</script>

<style scoped>
.ranking-round-workbench,
.ranking-board,
.ranking-slots,
.candidate-list {
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
h3,
h4 {
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

.ranking-grid {
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr) 300px;
  gap: 10px;
  align-items: start;
}

.ranking-board {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.ranking-table-card {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.9);
}

.ranking-table-card.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.045);
}

.ranking-table-card header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.ranking-table-card h3 {
  margin-top: 3px;
  font-size: 22px;
}

.ranking-table-card em {
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
.ranking-slots article,
.candidate-list article {
  padding: 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.table-meta span {
  display: grid;
  gap: 3px;
}

.ranking-slots article,
.candidate-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.ranking-slots article.filled {
  color: #6fcf7a;
  border-color: rgba(111, 207, 122, 0.24);
  background: rgba(111, 207, 122, 0.07);
}

.ranking-slots span {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.candidate-list div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.ranking-slots button,
.candidate-list button {
  min-height: 28px;
  padding: 0 8px;
  color: #e0b84a;
  border: 1px solid rgba(216, 169, 53, 0.2);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.06);
  font: inherit;
  cursor: pointer;
}

.candidate-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.candidate-list p,
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
  .ranking-grid,
  .ranking-board {
    grid-template-columns: 1fr;
  }
}
</style>
