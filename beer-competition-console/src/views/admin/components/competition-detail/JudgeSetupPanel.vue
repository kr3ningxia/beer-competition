<template>
  <section class="judge-setup-panel">
    <header class="task-hero">
      <div>
        <small>第一轮评审人员</small>
        <h2>先安排每张基础桌的评审</h2>
        <p>完成后生成第一轮编排，再去分配酒款。</p>
      </div>
      <button class="primary-action" type="button" :disabled="validationIssues.length > 0" @click="$emit('generateFirstRound')">
        生成第一轮编排
      </button>
    </header>

    <section class="judge-layout">
      <aside class="judge-pool">
        <div class="panel-heading">
          <div>
            <h3>本场评审池</h3>
            <span>{{ filteredJudgePool.length }} 人</span>
          </div>
        </div>
        <label class="search-box">
          <Search />
          <input v-model="judgeKeywordModel" placeholder="搜索姓名、手机号、资质" />
        </label>
        <div class="filter-row">
          <button
            v-for="filter in roleFilters"
            :key="filter.value"
            :class="{ active: judgeRoleFilter === filter.value }"
            type="button"
            @click="$emit('update:judgeRoleFilter', filter.value)"
          >
            {{ filter.label }}
          </button>
        </div>

        <div class="pool-list">
          <article
            v-for="judge in filteredJudgePool"
            :key="judge.publicId"
            :class="['judge-card', { assigned: isAssigned(judge.publicId), inactive: !isJudgeActive(judge) }]"
            :draggable="editable"
            @dragstart="$emit('startJudgeDrag', judge)"
            @dragend="$emit('clearDrag')"
          >
            <span class="avatar">{{ getJudgeInitial(judge.name) }}</span>
            <div>
              <strong>{{ judge.name || '未填写姓名' }}</strong>
              <small>{{ judge.qualification || '未填写资质' }}</small>
              <em>{{ isAssigned(judge.publicId) ? '已安排' : '未安排' }}</em>
            </div>
            <button type="button" :disabled="!editable || !isJudgeActive(judge)" @click="$emit('addJudgeToTarget', judge)">
              加入
            </button>
          </article>
        </div>
      </aside>

      <main class="base-table-board">
        <div class="board-toolbar">
          <div class="metric-strip">
            <span>已安排 <strong>{{ judgeMetrics.assigned }}</strong></span>
            <span>桌长 <strong>{{ judgeMetrics.captain }} / {{ judgeTableForm.length }}</strong></span>
            <span>专业 <strong>{{ judgeMetrics.professional }}</strong></span>
            <span>跨界 <strong>{{ judgeMetrics.cross }}</strong></span>
          </div>
          <button v-if="editable" class="secondary-action" type="button" @click="$emit('saveJudgeDraft')">保存评审人员</button>
        </div>

        <article
          v-for="(table, index) in judgeTableForm"
          :key="table.localId"
          :class="['base-table-card', { active: selectedTableLocalId === table.localId, danger: tableValidationIssues(table).length }]"
          @click="$emit('selectAssignmentTarget', table, selectedRole)"
        >
          <header>
            <label v-if="editable">
              <span>桌号</span>
              <input :value="table.tableName" placeholder="例如 A桌" @input="table.tableName = $event.target.value" />
            </label>
            <h3 v-else>{{ table.tableName }}</h3>
            <button v-if="editable" class="icon-action" type="button" @click.stop="$emit('removeJudgeTable', index)">
              <Delete />
            </button>
          </header>

          <p :class="tableValidationIssues(table).length ? 'warning' : 'ok'">
            {{ tableValidationIssues(table)[0] || '本桌可生成第一轮任务' }}
          </p>

          <section class="role-grid">
            <div
              v-for="role in roleOptions"
              :key="role.value"
              :class="['role-lane', { active: selectedTableLocalId === table.localId && selectedRole === role.value }]"
              @click.stop="$emit('selectAssignmentTarget', table, role.value)"
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
                class="assignment-card"
                :draggable="editable"
                @dragstart="$emit('startAssignmentDrag', assignment)"
                @dragend="$emit('clearDrag')"
              >
                <span class="avatar small">{{ getJudgeInitial(getJudge(assignment.judgePublicId)?.name) }}</span>
                <div>
                  <strong>{{ getJudge(assignment.judgePublicId)?.name || '未知评审' }}</strong>
                  <small>{{ getJudge(assignment.judgePublicId)?.qualification || '未填写资质' }}</small>
                </div>
                <button v-if="editable" class="icon-action" type="button" @click.stop="$emit('removeAssignment', assignment)">
                  <Delete />
                </button>
              </article>
              <button v-if="editable && assignmentsForTable(table, role.value).length === 0" class="empty-add" type="button">
                选择左侧评审加入
              </button>
            </div>
          </section>
        </article>

        <button v-if="editable" class="add-table" type="button" @click="$emit('addJudgeTable')">新增基础桌</button>
      </main>

      <aside class="setup-check">
        <h3>下一步检查</h3>
        <div class="check-list">
          <p v-for="issue in validationIssues" :key="issue">
            <Warning />
            <span>{{ issue }}</span>
          </p>
          <p v-if="validationIssues.length === 0" class="ok">
            <CircleCheck />
            <span>可以生成第一轮编排。</span>
          </p>
        </div>
      </aside>
    </section>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheck, Delete, Search, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  editable: Boolean,
  judgeTableForm: { type: Array, required: true },
  roleOptions: { type: Array, required: true },
  roleFilters: { type: Array, required: true },
  judgeKeyword: { type: String, default: '' },
  judgeRoleFilter: { type: String, default: 'ALL' },
  selectedTableLocalId: { type: [String, Number], default: null },
  selectedRole: { type: String, default: 'CAPTAIN' },
  judgeMetrics: { type: Object, required: true },
  validationIssues: { type: Array, required: true },
  filteredJudgePool: { type: Array, required: true },
  assignmentsForTable: { type: Function, required: true },
  tableValidationIssues: { type: Function, required: true },
  getJudge: { type: Function, required: true },
  getJudgeInitial: { type: Function, required: true },
  isAssigned: { type: Function, required: true },
  isJudgeActive: { type: Function, required: true },
})

const emit = defineEmits([
  'update:judgeKeyword',
  'update:judgeRoleFilter',
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
])

const judgeKeywordModel = computed({
  get: () => props.judgeKeyword,
  set: (value) => emit('update:judgeKeyword', value),
})
</script>

<style scoped>
.judge-setup-panel,
.judge-layout,
.base-table-board,
.pool-list,
.check-list {
  display: grid;
  gap: 12px;
}

.task-hero,
.board-toolbar,
.panel-heading,
.judge-card,
.assignment-card,
.setup-check p {
  display: flex;
  align-items: center;
}

.task-hero {
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.82);
}

.task-hero h2,
.task-hero p,
h3 {
  margin: 0;
}

.task-hero small {
  color: #e0b84a;
  font-weight: 800;
}

.task-hero p,
small,
em,
.setup-check span {
  color: #8da1aa;
}

.primary-action,
.secondary-action,
.judge-card button,
.add-table,
.empty-add,
.filter-row button {
  min-height: 38px;
  padding: 0 12px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font: inherit;
  cursor: pointer;
}

.primary-action {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.09);
  font-weight: 800;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.judge-layout {
  grid-template-columns: 320px minmax(0, 1fr) 300px;
  align-items: start;
}

.judge-pool,
.setup-check,
.base-table-card {
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.9);
}

.panel-heading {
  justify-content: space-between;
}

.search-box {
  position: relative;
  display: block;
  margin-top: 10px;
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
.base-table-card input {
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

.base-table-card input {
  padding-left: 10px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 10px 0;
}

.filter-row button {
  min-height: 30px;
  color: #8da1aa;
  font-size: 12px;
}

.filter-row button.active {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.08);
}

.judge-card,
.assignment-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 9px;
  padding: 9px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.judge-card.assigned {
  border-color: rgba(111, 207, 122, 0.24);
  background: rgba(111, 207, 122, 0.06);
}

.judge-card.inactive {
  opacity: 0.55;
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

.judge-card div,
.assignment-card div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.judge-card strong,
.judge-card small,
.judge-card em,
.assignment-card strong,
.assignment-card small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.board-toolbar {
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
}

.metric-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.metric-strip span {
  padding: 7px 10px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.base-table-card {
  display: grid;
  gap: 12px;
}

.base-table-card.active {
  border-color: rgba(216, 169, 53, 0.36);
  background: rgba(216, 169, 53, 0.045);
}

.base-table-card.danger {
  border-color: rgba(242, 153, 74, 0.3);
}

.base-table-card header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.base-table-card label {
  display: grid;
  gap: 6px;
}

.warning {
  color: #f1bd79;
}

.ok {
  color: #6fcf7a;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.role-lane {
  display: grid;
  gap: 8px;
  min-height: 136px;
  padding: 10px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.022);
}

.role-lane.active {
  border-color: rgba(216, 169, 53, 0.34);
}

.role-lane header {
  display: flex;
  justify-content: space-between;
}

.icon-action {
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  color: #a9bbc2;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
}

.icon-action :deep(svg),
.setup-check :deep(svg) {
  width: 16px;
  height: 16px;
  flex: 0 0 auto;
}

.empty-add,
.add-table {
  width: 100%;
  color: #e0b84a;
  border-style: dashed;
}

.setup-check {
  position: sticky;
  top: 0;
}

.setup-check p {
  gap: 8px;
  margin: 0;
  padding: 10px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

@media (max-width: 1180px) {
  .judge-layout,
  .role-grid {
    grid-template-columns: 1fr;
  }

  .setup-check {
    position: static;
  }
}
</style>
