<template>
  <div class="password-field">
    <span class="field-label">{{ label }}</span>
    <span :class="['field-control', { invalid: Boolean(error) }]">
      <input
        :value="modelValue"
        :type="visible ? 'text' : 'password'"
        :autocomplete="autocomplete"
        :placeholder="placeholder"
        @input="$emit('update:modelValue', $event.target.value)"
      />
      <button
        class="visibility-toggle"
        type="button"
        :aria-label="visible ? '隐藏密码' : '显示密码'"
        :title="visible ? '隐藏密码' : '显示密码'"
        @click="visible = !visible"
      >
        <Hide v-if="visible" />
        <View v-else />
      </button>
    </span>
    <small v-if="error" class="field-error">{{ error }}</small>
    <small v-else-if="hint" class="field-hint">{{ hint }}</small>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Hide, View } from '@element-plus/icons-vue'

defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: 'off' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
})

defineEmits(['update:modelValue'])

const visible = ref(false)
</script>

<style scoped>
.password-field {
  display: grid;
  gap: 7px;
}

.field-label {
  color: var(--muted, #8da1aa);
  font-size: 12px;
  font-weight: 800;
}

.field-control {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 42px;
  padding: 0 6px 0 11px;
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.68);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.field-control:focus-within {
  border-color: rgba(224, 184, 74, 0.5);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08);
}

.field-control.invalid {
  border-color: rgba(255, 180, 168, 0.5);
}

.field-control input {
  width: 100%;
  min-width: 0;
  color: var(--text, #e6edf0);
  font: inherit;
  border: 0;
  outline: 0;
  background: transparent;
}

.field-control input::placeholder {
  color: var(--faint, #5f737d);
}

.visibility-toggle {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: var(--muted, #8da1aa);
  border: 0;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
}

.visibility-toggle:hover {
  color: var(--text, #e6edf0);
  background: rgba(255, 255, 255, 0.05);
}

.visibility-toggle svg {
  width: 16px;
  height: 16px;
}

.field-hint {
  color: var(--faint, #5f737d);
  font-size: 12px;
}

.field-error {
  color: var(--danger, #ffb4a8);
  font-size: 12px;
}
</style>
