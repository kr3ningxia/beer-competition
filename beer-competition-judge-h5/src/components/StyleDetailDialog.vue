<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="style-detail-backdrop"
      role="presentation"
      @click.self="$emit('close')"
    >
      <section
        class="style-detail-sheet"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
      >
        <header class="style-detail-head">
          <div>
            <span>{{ label }}</span>
            <h2 :id="titleId">{{ styleCategoryText }}</h2>
            <p>{{ baseStyleText }}</p>
          </div>
          <button class="style-detail-close" type="button" :aria-label="closeLabel" @click="$emit('close')">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
        </header>

        <div class="style-detail-body">
          <p>{{ detailText.main }}</p>
          <section v-if="detailText.metrics" class="style-metrics-panel">
            <span>指标范围</span>
            <p>{{ detailText.metrics }}</p>
          </section>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  entry: {
    type: Object,
    default: null,
  },
  label: {
    type: String,
    default: '风格分类',
  },
  titleId: {
    type: String,
    default: 'style-detail-title',
  },
  closeLabel: {
    type: String,
    default: '关闭风格介绍',
  },
  emptyText: {
    type: String,
    default: '暂无风格介绍。',
  },
})

defineEmits(['close'])

const styleCategoryText = computed(() => (
  props.entry?.styleCategoryName || props.entry?.categoryName || '-'
))
const baseStyleText = computed(() => (
  [props.entry?.styleCode, props.entry?.style].filter(Boolean).join(' ') || '基础风格'
))
const detailText = computed(() => splitStyleDescription(props.entry?.styleDescription))

function splitStyleDescription(description) {
  const text = String(description || '').trim()
  if (!text) return { main: props.emptyText, metrics: '' }
  const marker = '指标范围：'
  const markerIndex = text.indexOf(marker)
  if (markerIndex < 0) return { main: text, metrics: '' }
  return {
    main: text.slice(0, markerIndex).trim(),
    metrics: text.slice(markerIndex + marker.length).trim(),
  }
}
</script>

<style scoped>
.style-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  align-items: end;
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
  background: rgba(8, 12, 10, 0.42);
}

.style-detail-sheet {
  width: min(100%, 536px);
  max-height: min(78vh, 720px);
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid rgba(29, 32, 26, 0.12);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 -18px 42px rgba(15, 19, 13, 0.24);
}

.style-detail-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  gap: 12px;
  align-items: start;
  padding: 22px 22px 16px;
  border-bottom: 1px solid #ece7df;
}

.style-detail-head span {
  display: block;
  margin-bottom: 7px;
  color: #d17932;
  font-size: 14px;
  font-weight: 900;
  line-height: 1;
}

.style-detail-head h2 {
  margin: 0;
  color: #07111f;
  font-size: 21px;
  line-height: 1.22;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.style-detail-head p {
  margin: 7px 0 0;
  color: #687280;
  font-size: 14px;
  line-height: 1.45;
  font-weight: 650;
  overflow-wrap: anywhere;
}

.style-detail-close {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid #e4ded5;
  border-radius: 999px;
  color: #4e574c;
  background: #f8f7f3;
  appearance: none;
}

.style-detail-close svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  stroke-width: 2.2;
  fill: none;
  stroke-linecap: round;
}

.style-detail-body {
  max-height: calc(min(78vh, 720px) - 126px);
  overflow-y: auto;
  padding: 18px 22px 24px;
  color: #263241;
  overscroll-behavior: contain;
}

.style-detail-body > p {
  margin: 0;
  color: #263241;
  font-size: 16px;
  line-height: 1.72;
  font-weight: 560;
  white-space: pre-wrap;
}

.style-metrics-panel {
  margin-top: 18px;
  border: 1px solid #eadfd3;
  border-radius: 18px;
  padding: 15px 16px;
  background: #fbf7f2;
}

.style-metrics-panel span {
  display: block;
  margin-bottom: 7px;
  color: #b76427;
  font-size: 14px;
  font-weight: 900;
  line-height: 1;
}

.style-metrics-panel p {
  margin: 0;
  color: #2d3541;
  font-size: 15px;
  line-height: 1.68;
  font-weight: 680;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 420px) {
  .style-detail-backdrop {
    padding: 14px 10px calc(14px + env(safe-area-inset-bottom));
  }

  .style-detail-sheet {
    max-height: min(82vh, 720px);
    border-radius: 22px 22px 18px 18px;
  }

  .style-detail-head {
    padding: 20px 18px 14px;
  }

  .style-detail-head h2 {
    font-size: 19px;
  }

  .style-detail-body {
    max-height: calc(min(82vh, 720px) - 118px);
    padding: 16px 18px 22px;
  }

  .style-detail-body > p {
    font-size: 15px;
    line-height: 1.68;
  }
}
</style>
