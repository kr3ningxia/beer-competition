<template>
  <section class="round-stepper">
    <button
      v-for="item in stepItems"
      :key="item.key"
      :class="['step-item', item.state, { active: item.roundId === activeRoundId }]"
      type="button"
      :disabled="item.disabled"
      @click="$emit('selectRound', item.roundId)"
    >
      <small>{{ item.eyebrow }}</small>
      <strong>{{ item.label }}</strong>
      <span>{{ item.detail }}</span>
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
  const first = props.rounds.find((round) => round.id === 'round-1')
  const second = props.rounds.find((round) => round.id === 'round-2')
  const final = props.rounds.find((round) => round.id === 'round-final')
  return [
    {
      key: 'judge',
      roundId: 'round-1',
      eyebrow: '评分制',
      label: '第一轮',
      detail: first ? props.roundStatusLabels[first.status] || first.status : '待创建',
      state: first?.status === 'LOCKED' ? 'done' : 'pending',
      disabled: !first,
    },
    {
      key: 'round2',
      roundId: 'round-2',
      eyebrow: '选择排序',
      label: '第二轮',
      detail: second ? props.roundStatusLabels[second.status] || second.status : '从晋级池创建',
      state: second?.status === 'LOCKED' ? 'done' : second ? 'pending' : 'empty',
      disabled: !second,
    },
    {
      key: 'final',
      roundId: 'round-final',
      eyebrow: '选择排序',
      label: '决赛轮',
      detail: final ? props.roundStatusLabels[final.status] || final.status : '待创建',
      state: final?.status === 'LOCKED' ? 'done' : final ? 'pending' : 'empty',
      disabled: !final,
    },
    {
      key: 'result',
      roundId: 'round-final',
      eyebrow: '发布前',
      label: '结果确认',
      detail: final?.status === 'LOCKED' ? '可确认奖项' : '等待轮次完成',
      state: final?.status === 'LOCKED' ? 'done' : 'empty',
      disabled: true,
    },
  ]
})
</script>

<style scoped>
.round-stepper {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.step-item {
  display: grid;
  gap: 4px;
  min-height: 82px;
  padding: 12px;
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

.step-item.empty {
  color: #8da1aa;
  border-style: dashed;
}

.step-item:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.step-item small,
.step-item span {
  color: #8da1aa;
}

@media (max-width: 980px) {
  .round-stepper {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
