<template>
  <nav class="bottom-nav" :style="{ gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }">
    <router-link
      v-for="item in items"
      :key="item.to"
      class="nav-item"
      :class="{ 'is-active': isActive(item) }"
      :to="item.to"
      :aria-label="item.label"
    >
      <svg v-if="item.icon === 'scan'" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 4h6v6H4V4Z" />
        <path d="M14 4h6v6h-6V4Z" />
        <path d="M4 14h6v6H4v-6Z" />
        <path d="M14 14h2v2h-2v-2Z" />
        <path d="M18 14h2v2h-2v-2Z" />
        <path d="M14 18h2v2h-2v-2Z" />
        <path d="M18 18h2v2h-2v-2Z" />
      </svg>
      <svg v-else-if="item.icon === 'table'" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M5 5h5v5H5V5Z" />
        <path d="M14 5h5v5h-5V5Z" />
        <path d="M5 14h5v5H5v-5Z" />
        <path d="M14 14h5v5h-5v-5Z" />
      </svg>
      <svg v-else viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
        <path d="M4 21a8 8 0 0 1 16 0" />
      </svg>
      <span>{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const props = defineProps({
  role: {
    type: String,
    default: '',
  },
  active: {
    type: String,
    default: '',
  },
  hideTable: {
    type: Boolean,
    default: false,
  },
})

const route = useRoute()

const items = computed(() => {
  const nav = [
    { label: '扫码', to: '/competitions', icon: 'scan', key: 'scan' },
  ]
  if (props.role === 'CAPTAIN' && !props.hideTable) {
    nav.push({ label: '本桌', to: '/captain', icon: 'table', key: 'table' })
  }
  nav.push({ label: '我的', to: '/profile', icon: 'profile', key: 'profile' })
  return nav
})

function isActive(item) {
  if (props.active) return item.key === props.active
  return route.path.startsWith(item.to)
}
</script>
