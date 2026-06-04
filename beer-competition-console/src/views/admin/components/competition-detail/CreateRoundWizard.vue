<template>
  <div v-if="open" class="modal-backdrop" @click.self="$emit('close')">
    <section class="round-dialog" role="dialog" aria-modal="true" aria-labelledby="create-round-title">
      <header>
        <div>
          <h2 id="create-round-title">创建{{ nextRoundName }}</h2>
          <p>使用上一轮晋级酒款，创建后重新分配桌长和酒款。</p>
        </div>
        <button class="icon-action" type="button" @click="$emit('close')">×</button>
      </header>

      <section class="summary-panel">
        <div class="summary-main">
          <span>晋级酒款</span>
          <strong>{{ advancedPool.length }}</strong>
          <small>创建后作为{{ nextRoundName }}可分配酒款池</small>
        </div>
        <div class="summary-list">
          <span v-for="item in advancedCategoryStats" :key="item.category">
            <strong>{{ item.count }}</strong>
            {{ item.category }}
          </span>
        </div>
      </section>

      <section class="round-config-panel">
        <label>
          <span>轮次目标</span>
          <select :value="targetMode" @change="$emit('update:targetMode', $event.target.value)">
            <option v-for="option in targetOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <small>{{ selectedTargetOption?.description }}</small>
        </label>
        <label>
          <span>桌数</span>
          <input
            :value="tableCount"
            min="1"
            type="number"
            :disabled="Boolean(selectedTargetOption?.fixedTableCount)"
            @input="$emit('update:tableCount', Number($event.target.value || 1))"
          />
        </label>
        <label>
          <span>每桌目标</span>
          <input
            :value="targetCount"
            min="1"
            type="number"
            :disabled="Boolean(selectedTargetOption?.fixedTargetCount)"
            @input="$emit('update:targetCount', Number($event.target.value || 1))"
          />
        </label>
      </section>

      <section class="result-panel">
        <article>
          <strong>创建后</strong>
          <p>{{ nextRoundName }}会有一张空的草稿桌，桌长和酒款都由后台人员在分桌分配页重新安排。</p>
        </article>
        <article>
          <strong>不自动沿用</strong>
          <p>不复制上一轮桌长，不自动分配酒款，避免现场重组时还要先删掉系统预设。</p>
        </article>
      </section>

      <footer>
        <button class="secondary-action" type="button" @click="$emit('close')">取消</button>
        <button class="primary-action" type="button" :disabled="advancedPool.length === 0" @click="$emit('finish')">
          创建并去分桌
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  open: Boolean,
  nextRoundName: { type: String, required: true },
  advancedPool: { type: Array, required: true },
  advancedCategoryStats: { type: Array, required: true },
  targetMode: { type: String, required: true },
  targetCount: { type: Number, required: true },
  tableCount: { type: Number, required: true },
  targetOptions: { type: Array, required: true },
})

defineEmits(['close', 'finish', 'update:targetMode', 'update:targetCount', 'update:tableCount'])

const selectedTargetOption = computed(() => props.targetOptions.find((option) => option.value === props.targetMode))
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
  width: min(720px, 100%);
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
p {
  margin: 0;
}

p,
small,
.summary-main span {
  color: #8da1aa;
}

button {
  font: inherit;
}

.icon-action,
.secondary-action,
.primary-action {
  min-height: 36px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  cursor: pointer;
}

.icon-action {
  display: grid;
  place-items: center;
  width: 36px;
  padding: 0;
  font-size: 20px;
}

.secondary-action,
.primary-action {
  padding: 0 14px;
}

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

.summary-panel {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 10px;
}

.round-config-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(96px, 0.6fr) minmax(120px, 0.6fr);
  gap: 10px;
}

.round-config-panel label,
.summary-main,
.summary-list span,
.result-panel article {
  padding: 12px;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.round-config-panel label {
  display: grid;
  gap: 8px;
}

.round-config-panel span {
  color: #e6edf0;
  font-weight: 800;
}

.round-config-panel select,
.round-config-panel input {
  width: 100%;
  min-height: 36px;
  color: #e6edf0;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(1, 7, 9, 0.42);
}

.round-config-panel input {
  padding: 0 10px;
}

.round-config-panel select {
  padding: 0 8px;
}

.round-config-panel input:disabled {
  color: #8da1aa;
  cursor: not-allowed;
}

.summary-main {
  display: grid;
  gap: 6px;
}

.summary-main strong {
  color: #e0b84a;
  font-size: 34px;
  line-height: 1;
}

.summary-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.summary-list span {
  display: grid;
  gap: 4px;
  color: #8da1aa;
}

.summary-list strong {
  color: #e6edf0;
}

.result-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.result-panel article {
  display: grid;
  gap: 6px;
}

@media (max-width: 760px) {
  .summary-panel,
  .round-config-panel,
  .summary-list,
  .result-panel {
    grid-template-columns: 1fr;
  }
}
</style>
