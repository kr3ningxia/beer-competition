<template>
  <header class="admin-page-header">
    <div class="admin-page-header-title">
      <slot name="leading" />
      <div class="admin-page-header-title-content">
        <h1>{{ title }}</h1>
        <slot name="meta" />
      </div>
    </div>
    <div v-if="$slots.actions" class="admin-page-header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
})
</script>

<style scoped>
.admin-page-header {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 96px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.1);
}

.admin-page-header-title {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.admin-page-header-title-content {
  min-width: 0;
}

.admin-page-header h1 {
  margin: 0;
  color: #edf4f6;
  font-size: 30px;
  font-weight: 850;
  line-height: 1.12;
  letter-spacing: 0;
}

.admin-page-header-actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .admin-page-header {
    align-items: flex-start;
    flex-direction: column;
    justify-content: center;
    padding: 18px 0;
  }

  .admin-page-header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
