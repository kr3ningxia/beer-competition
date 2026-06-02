<template>
  <aside class="entry-pool-panel">
    <div class="panel-heading">
      <div>
        <h3>{{ title }}</h3>
        <span>{{ filteredEntries.length }} 款</span>
      </div>
      <button v-if="selectedEntryUuids.length" type="button" @click="$emit('clearSelection')">清空选择</button>
    </div>

    <label class="search-box">
      <Search />
      <input v-model="keywordModel" placeholder="搜索匿名编号、短编号、风格" />
    </label>

    <div class="category-summary">
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

    <div v-if="showStats" class="stat-list">
      <span v-for="stat in categoryStats" :key="stat.category">
        <strong>{{ stat.unassigned }}</strong>
        {{ stat.category }}未分配
      </span>
    </div>

    <div class="bulk-actions">
      <button type="button" :disabled="!selectedEntryUuids.length || !selectedRoundTableId" @click="$emit('addSelectedToTable')">
        加入选中桌
      </button>
      <button v-if="isScoreRound" type="button" @click="$emit('assignEvenly')">平均分配</button>
      <button v-if="isScoreRound" type="button" @click="$emit('assignByCategory')">按组别分配</button>
      <button v-if="isScoreRound" type="button" @click="$emit('clearRoundAssignments')">清空重排</button>
    </div>

    <div class="entry-list">
      <article
        v-for="entry in filteredEntries"
        :key="entry.uuid"
        :class="['entry-card', { selected: selectedEntryUuids.includes(entry.uuid), assigned: getRoundEntryAssignment(entry.uuid) }]"
        draggable="true"
        @dragstart="$emit('startEntryDrag', entry.uuid)"
        @dragend="$emit('clearDrag')"
      >
        <label>
          <input
            type="checkbox"
            :checked="selectedEntryUuids.includes(entry.uuid)"
            @change="$emit('toggleEntrySelection', entry.uuid)"
          />
          <span>
            <strong>{{ entry.uuid }}</strong>
            <small>{{ entry.shortCode }} · {{ entry.categoryName }} · {{ entry.style }}</small>
            <em v-if="entry.sourceTable">{{ entry.sourceTable }} · {{ entry.sourceResult }}</em>
          </span>
        </label>
        <button type="button" :disabled="!selectedRoundTableId" @click="$emit('addEntryToSelectedTable', entry.uuid)">
          {{ getRoundEntryAssignment(entry.uuid) ? '移动' : '加入' }}
        </button>
      </article>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, required: true },
  isScoreRound: Boolean,
  filteredEntries: { type: Array, required: true },
  categoryStats: { type: Array, default: () => [] },
  roundCategoryFilters: { type: Array, required: true },
  roundCategoryFilter: { type: String, default: '全部' },
  roundKeyword: { type: String, default: '' },
  selectedEntryUuids: { type: Array, default: () => [] },
  selectedRoundTableId: { type: String, default: '' },
  getRoundEntryAssignment: { type: Function, required: true },
})

const emit = defineEmits([
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
])

const keywordModel = computed({
  get: () => props.roundKeyword,
  set: (value) => emit('update:roundKeyword', value),
})

const showStats = computed(() => props.isScoreRound && props.categoryStats.length)
</script>

<style scoped>
.entry-pool-panel,
.entry-list {
  display: grid;
  gap: 10px;
}

.entry-pool-panel {
  position: sticky;
  top: 0;
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.9);
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

h3 {
  margin: 0;
}

.panel-heading span,
small,
em {
  color: #8da1aa;
}

button {
  min-height: 32px;
  padding: 0 10px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font: inherit;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.search-box {
  position: relative;
  display: block;
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

.search-box input {
  width: 100%;
  box-sizing: border-box;
  min-height: 40px;
  padding: 0 10px 0 32px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: 0;
  background: rgba(7, 14, 17, 0.72);
}

.category-summary,
.bulk-actions,
.stat-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-summary button {
  min-height: 30px;
  color: #8da1aa;
  font-size: 12px;
}

.category-summary button.active,
.bulk-actions button:first-child {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.08);
}

.stat-list span {
  padding: 7px 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  color: #8da1aa;
  font-size: 12px;
}

.stat-list strong {
  color: #e6edf0;
}

.entry-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.entry-card.assigned {
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.055);
}

.entry-card.selected {
  border-color: rgba(216, 169, 53, 0.34);
}

.entry-card label {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
}

.entry-card span {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.entry-card strong,
.entry-card small,
.entry-card em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

input[type="checkbox"] {
  width: 16px;
  height: 16px;
  margin-top: 2px;
  accent-color: #e0b84a;
}

@media (max-width: 1180px) {
  .entry-pool-panel {
    position: static;
  }
}
</style>
