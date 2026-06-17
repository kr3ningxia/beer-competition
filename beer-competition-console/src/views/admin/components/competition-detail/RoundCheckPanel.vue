<template>
  <aside class="round-check-panel">
    <section class="check-card">
      <div class="panel-heading">
        <div>
          <h3>{{ title }}</h3>
          <span>{{ roundValidationIssues.length }} 项</span>
        </div>
      </div>

      <template v-if="selectedRoundTable">
        <label class="stack-field">
          <span>当前桌</span>
          <strong>{{ selectedRoundTable.name }}</strong>
        </label>
        <label class="stack-field">
          <span>桌长</span>
          <select :value="selectedRoundTable.captainPublicId" @change="$emit('updateTableCaptain', selectedRoundTable.id, $event.target.value)">
            <option value="">未指定</option>
            <option v-for="judge in captainCandidates" :key="judge.publicId" :value="judge.publicId">
              {{ judge.name }} · {{ judge.qualification }}
            </option>
          </select>
        </label>
        <label class="stack-field">
          <span>{{ targetLabel }}</span>
          <input
            :value="selectedRoundTable.targetCount"
            min="1"
            type="number"
            :disabled="targetFixed"
            @input="$emit('updateTableTarget', selectedRoundTable.id, Number($event.target.value || 1))"
          />
          <small v-if="targetHint">{{ targetHint }}</small>
        </label>
        <button v-if="canRemoveTable" class="secondary-action" type="button" @click="$emit('removeRoundTable', selectedRoundTable.id)">
          删除草稿桌
        </button>
      </template>

      <div class="check-list">
        <p v-for="issue in roundValidationIssues" :key="issue">
          <Warning />
          <span>{{ issue }}</span>
        </p>
        <p v-if="roundValidationIssues.length === 0" class="ok">
          <CircleCheck />
          <span>{{ successText }}</span>
        </p>
      </div>

      <slot />
    </section>
  </aside>
</template>

<script setup>
import { CircleCheck, Warning } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, default: '发布检查' },
  targetLabel: { type: String, default: '目标设置' },
  targetHint: { type: String, default: '' },
  targetFixed: Boolean,
  successText: { type: String, default: '当前轮次可以发布' },
  selectedRoundTable: { type: Object, default: null },
  captainCandidates: { type: Array, required: true },
  roundValidationIssues: { type: Array, required: true },
  canRemoveTable: Boolean,
})

defineEmits(['updateTableCaptain', 'updateTableTarget', 'removeRoundTable'])
</script>

<style scoped>
.round-check-panel {
  position: sticky;
  top: 0;
}

.check-card {
  display: grid;
  gap: 12px;
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
.stack-field span,
.stack-field small {
  color: #8da1aa;
}

.stack-field small {
  line-height: 1.45;
}

.stack-field,
.check-list {
  display: grid;
  gap: 8px;
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

input:disabled {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.055);
  cursor: not-allowed;
  font-weight: 850;
}

.check-list p {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 0;
  padding: 10px;
  color: #f1bd79;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.check-list :deep(svg) {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
}

.check-list p.ok {
  color: #6fcf7a;
}

.secondary-action {
  min-height: 38px;
  padding: 0 12px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font: inherit;
  cursor: pointer;
}

@media (max-width: 1180px) {
  .round-check-panel {
    position: static;
  }
}
</style>
