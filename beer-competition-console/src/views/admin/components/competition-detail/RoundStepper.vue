<template>
  <section class="round-stepper">
    <button
      v-for="item in stepItems"
      :key="item.key"
      :class="['step-item', item.state, { active: item.roundId === activeRoundId }]"
      type="button"
      @click="$emit('selectRound', item.roundId)"
    >
      <strong>{{ item.label }}</strong>
      <span>{{ item.eyebrow }} · {{ item.detail }}</span>
    </button>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  rounds: { type: Array, required: true },
  activeRoundId: { type: String, default: '' },
  roundStatusLabels: { type: Object, required: true },
})

defineEmits(['selectRound'])

const stepItems = computed(() => {
  return props.rounds.map((round) => ({
    key: round.id,
    roundId: round.id,
    eyebrow: round.type === 'SCORE' ? '评分制' : '选择排序',
    label: round.name,
    detail: props.roundStatusLabels[round.status] || round.status,
    state: round.status === 'LOCKED' ? 'done' : 'pending',
  }))
})
</script>

<style scoped>
.round-stepper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.step-item {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  color: #e6edf0;
  text-align: left;
  border: 1px solid rgba(219, 232, 237, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  font: inherit;
  cursor: pointer;
}

.step-item.active {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.36);
  background: rgba(216, 169, 53, 0.08);
}

.step-item.done {
  border-color: rgba(111, 207, 122, 0.24);
}

.step-item:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.step-item small,
.step-item span {
  color: #8da1aa;
  font-size: 12px;
}

@media (max-width: 980px) {
  .round-stepper {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
