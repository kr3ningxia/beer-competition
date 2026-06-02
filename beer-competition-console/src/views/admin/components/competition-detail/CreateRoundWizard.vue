<template>
  <div v-if="open" class="modal-backdrop" @click.self="$emit('close')">
    <section class="round-dialog" role="dialog" aria-modal="true" aria-labelledby="create-round-title">
      <header>
        <div>
          <h2 id="create-round-title">从晋级酒款创建第二轮</h2>
          <p>上一轮锁定后，才会把晋级酒款带到这里。</p>
        </div>
        <button class="icon-action" type="button" @click="$emit('close')">×</button>
      </header>

      <div class="wizard-steps">
        <button v-for="item in steps" :key="item.step" :class="{ active: createRoundStep === item.step }" type="button" @click="$emit('update:createRoundStep', item.step)">
          {{ item.label }}
        </button>
      </div>

      <section v-if="createRoundStep === 1" class="wizard-panel">
        <h3>候选来源</h3>
        <p>默认使用上一轮晋级酒款，共 {{ advancedPool.length }} 款。</p>
        <div class="wizard-stat-grid">
          <span v-for="item in advancedCategoryStats" :key="item.category">
            <strong>{{ item.count }}</strong>
            {{ item.category }}
          </span>
        </div>
      </section>

      <section v-if="createRoundStep === 2" class="wizard-panel">
        <h3>建桌方式</h3>
        <div class="strategy-grid">
          <button
            v-for="strategy in roundStrategies"
            :key="strategy.value"
            :class="{ active: createRoundForm.strategy === strategy.value }"
            type="button"
            @click="createRoundForm.strategy = strategy.value"
          >
            <strong>{{ strategy.label }}</strong>
            <small>{{ strategy.detail }}</small>
          </button>
        </div>
        <label v-if="createRoundForm.strategy === 'EVEN'" class="stack-field">
          <span>平均分成几桌</span>
          <input v-model.number="createRoundForm.tableCount" min="1" max="4" type="number" />
        </label>
      </section>

      <section v-if="createRoundStep === 3" class="wizard-panel">
        <h3>指定桌长</h3>
        <p>每张后续轮桌必须指定 1 名桌长。</p>
        <div class="wizard-table-list">
          <label v-for="table in previewNextRoundTables" :key="table.id">
            <span>{{ table.name }}</span>
            <select v-model="table.captainPublicId">
              <option value="">未指定</option>
              <option v-for="judge in captainCandidates" :key="judge.publicId" :value="judge.publicId">
                {{ judge.name }} · {{ judge.qualification }}
              </option>
            </select>
          </label>
        </div>
      </section>

      <section v-if="createRoundStep === 4" class="wizard-panel">
        <h3>分配酒款</h3>
        <p>系统按当前策略先分好，创建后仍可在编排页微调。</p>
        <div class="wizard-preview-tables">
          <article v-for="table in previewNextRoundTables" :key="`preview-${table.id}`">
            <strong>{{ table.name }}</strong>
            <span>{{ table.entryUuids.length }} 款</span>
            <small>{{ table.entryUuids.join('、') || '待手动加入' }}</small>
          </article>
        </div>
      </section>

      <section v-if="createRoundStep === 5" class="wizard-panel">
        <h3>排序目标</h3>
        <label class="stack-field">
          <span>目标名称</span>
          <select v-model="createRoundForm.targetMode">
            <option value="TOP3">选前 3</option>
            <option value="MEDALS">金银铜</option>
            <option value="CHAMPION">总冠军候选</option>
          </select>
        </label>
        <label class="stack-field">
          <span>每桌目标数量</span>
          <input v-model.number="createRoundForm.targetCount" min="1" type="number" />
        </label>
      </section>

      <section v-if="createRoundStep === 6" class="wizard-panel">
        <h3>确认创建</h3>
        <div class="check-list">
          <p v-for="issue in createRoundPreviewIssues" :key="issue">
            <Warning />
            <span>{{ issue }}</span>
          </p>
          <p v-if="createRoundPreviewIssues.length === 0" class="ok">
            <CircleCheck />
            <span>可以创建第二轮。发布后只给桌长生成排序任务。</span>
          </p>
        </div>
      </section>

      <footer>
        <button class="secondary-action" type="button" :disabled="createRoundStep === 1" @click="$emit('update:createRoundStep', createRoundStep - 1)">上一步</button>
        <button v-if="createRoundStep < 6" class="primary-action" type="button" @click="$emit('update:createRoundStep', createRoundStep + 1)">下一步</button>
        <button v-else class="primary-action" type="button" :disabled="createRoundPreviewIssues.length > 0" @click="$emit('finish')">
          创建第二轮
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { CircleCheck, Warning } from '@element-plus/icons-vue'

defineProps({
  open: Boolean,
  createRoundStep: { type: Number, default: 1 },
  createRoundForm: { type: Object, required: true },
  previewNextRoundTables: { type: Array, required: true },
  advancedPool: { type: Array, required: true },
  advancedCategoryStats: { type: Array, required: true },
  roundStrategies: { type: Array, required: true },
  captainCandidates: { type: Array, required: true },
  createRoundPreviewIssues: { type: Array, required: true },
})

defineEmits(['update:createRoundStep', 'close', 'finish'])

const steps = [
  { step: 1, label: '来源' },
  { step: 2, label: '建桌' },
  { step: 3, label: '桌长' },
  { step: 4, label: '酒款' },
  { step: 5, label: '目标' },
  { step: 6, label: '确认' },
]
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(1, 7, 9, 0.72);
}

.round-dialog {
  width: min(920px, 100%);
  max-height: min(760px, 92vh);
  overflow: auto;
  display: grid;
  gap: 16px;
  padding: 18px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(15, 24, 28, 0.98);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.42);
}

header,
footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

footer {
  justify-content: flex-end;
}

h2,
h3,
p {
  margin: 0;
}

p,
small,
.stack-field span {
  color: #8da1aa;
}

button,
select,
input {
  font: inherit;
}

.wizard-steps {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.wizard-steps button,
.secondary-action,
.primary-action,
.icon-action {
  min-height: 36px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.wizard-steps button.active,
.primary-action {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
  font-weight: 800;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.wizard-panel,
.stack-field,
.check-list {
  display: grid;
  gap: 12px;
}

.wizard-panel {
  min-height: 260px;
  align-content: start;
}

.wizard-stat-grid,
.strategy-grid,
.wizard-preview-tables {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.strategy-grid button {
  display: grid;
  gap: 6px;
  min-height: 88px;
  padding: 12px;
  color: #e6edf0;
  text-align: left;
}

.strategy-grid button.active {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.wizard-stat-grid span,
.wizard-preview-tables article,
.check-list p {
  padding: 10px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.wizard-table-list {
  display: grid;
  gap: 10px;
}

.wizard-table-list label {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

select,
input {
  width: 100%;
  box-sizing: border-box;
  min-height: 40px;
  padding: 0 10px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: 0;
  background: rgba(7, 14, 17, 0.72);
}

.check-list p {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #f1bd79;
}

.check-list :deep(svg) {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

.check-list p.ok {
  color: #6fcf7a;
}
</style>
