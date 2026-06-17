<template>
  <div v-if="open" class="modal-backdrop" @click.self="$emit('close')">
    <section class="round-dialog" role="dialog" aria-modal="true" aria-labelledby="create-round-title">
      <header>
        <div>
          <h2 id="create-round-title">{{ dialogTitle }}</h2>
          <p>{{ dialogHint }}</p>
        </div>
        <button class="icon-action" type="button" @click="$emit('close')">×</button>
      </header>

      <section class="summary-panel">
        <div class="summary-main">
          <span>{{ candidateLabel }}</span>
          <strong>{{ advancedPool.length }}</strong>
          <small>{{ candidateHint }}</small>
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
        <label :class="{ 'fixed-count-field': isTargetCountFixed }">
          <span>{{ targetCountLabel }}</span>
          <input
            :value="targetCount"
            min="1"
            type="number"
            :disabled="isTargetCountFixed"
            @input="$emit('update:targetCount', Number($event.target.value || 1))"
          />
          <small>{{ targetCountHint }}</small>
        </label>
      </section>

      <footer>
        <button class="secondary-action" type="button" @click="$emit('close')">取消</button>
        <button class="primary-action" type="button" @click="$emit('finish')">
          {{ finishLabel }}
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
  earlyDraft: Boolean,
})

defineEmits(['close', 'finish', 'update:targetMode', 'update:targetCount', 'update:tableCount'])

const selectedTargetOption = computed(() => props.targetOptions.find((option) => option.value === props.targetMode))
const isChampion = computed(() => props.targetMode === 'CHAMPION')
const dialogTitle = computed(() => (isChampion.value ? '准备决赛轮' : `准备${props.nextRoundName}排序`))
const dialogHint = computed(() => {
  if (isChampion.value) return '安排决赛桌、桌长和参与评审。'
  if (props.earlyDraft) return '先安排桌次和人员，晋级酒款在本轮锁定后同步。'
  return '先安排桌长和参与评审，上一轮结果固定后再分配酒款。'
})
const candidateLabel = computed(() => (isChampion.value ? '各组别金奖' : (props.earlyDraft ? '当前晋级' : '当前候选')))
const candidateHint = computed(() => {
  if (isChampion.value) return '用于决出全场总冠军'
  if (props.earlyDraft) return `锁定后同步到${props.nextRoundName}`
  return `用于${props.nextRoundName}分桌`
})
const finishLabel = computed(() => (isChampion.value ? '创建决赛草稿并去分桌' : '创建草稿并去分桌'))
const isTargetCountFixed = computed(() => Boolean(selectedTargetOption.value?.fixedTargetCount))
const targetCountLabel = computed(() => {
  if (props.targetMode === 'MEDALS') return '奖项名额'
  if (props.targetMode === 'CHAMPION') return '总冠军'
  return '每桌晋级数量'
})
const targetCountHint = computed(() => {
  if (props.targetMode === 'MEDALS') return '金奖、银奖、铜奖为可用名额，可按评审结果留空。'
  if (props.targetMode === 'CHAMPION') return '总冠军 1 名。'
  return '每张桌由桌长提交并排序的晋级酒款数量。'
})
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
.summary-list span {
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

.fixed-count-field input {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.28);
  background: rgba(216, 169, 53, 0.055);
  font-weight: 850;
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

@media (max-width: 760px) {
  .summary-panel,
  .round-config-panel,
  .summary-list {
    grid-template-columns: 1fr;
  }
}
</style>
