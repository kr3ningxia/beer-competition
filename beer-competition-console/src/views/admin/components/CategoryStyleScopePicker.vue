<template>
  <div class="scope-picker">
    <div class="scope-toolbar">
      <label class="scope-restrict-switch">
        <span>限定可选风格</span>
        <el-switch :model-value="restricted" aria-label="限定该组别的可选风格" @change="toggleRestriction" />
      </label>
      <input
        v-if="restricted"
        v-model.trim="search"
        class="scope-search"
        type="search"
        placeholder="搜索分类、风格名或编号"
        @input="handleSearchInput"
      />
      <span v-if="restricted" class="scope-count">已选 {{ selectedStyleCount }} 个风格</span>
      <button v-if="restricted" class="text-action" type="button" @click="setAll">改为不限风格</button>
      <button v-if="restricted" class="text-action" type="button" @click="clearAll">清空已选</button>
    </div>

    <div v-if="unmatchedNames.length" class="scope-unmatched">
      <span>{{ unmatchedNames.length }} 个已选风格不在当前风格库</span>
      <button class="text-action" type="button" @click="removeUnmatched">移除</button>
    </div>

    <div v-if="!restricted" class="scope-unrestricted">
      <span>不限风格：厂商选择该组别后，可从全部 {{ allStyleNames.length }} 个风格中任选一项</span>
      <button class="text-action" type="button" @click="toggleRestriction(true)">限定可选风格</button>
    </div>

    <template v-else>
      <p class="scope-restrict-hint">厂商选择该组别后，只能从下方勾选的风格中选择</p>
      <div class="scope-picker-grid">
        <div class="scope-category-rail">
          <div
            v-if="search"
            :class="['scope-category-item', 'scope-category-result', { active: activeCategory === SEARCH_KEY }]"
            @click="activeCategory = SEARCH_KEY"
          >
            <span class="scope-category-name">搜索结果</span>
            <small>{{ searchResults.length }}</small>
          </div>
          <div
            v-for="group in visibleCategories"
            :key="group.name"
            :class="['scope-category-item', { active: activeCategory === group.name }]"
            @click="activeCategory = group.name"
          >
            <el-checkbox
              :model-value="categoryState(group).checked"
              :indeterminate="categoryState(group).indeterminate"
              @click.stop
              @change="toggleCategoryStyles(group, $event)"
            />
            <span class="scope-category-name" :title="group.name">{{ group.name }}</span>
            <small>{{ categoryState(group).count }}/{{ group.styles.length }}</small>
          </div>
          <p v-if="!visibleCategories.length" class="empty-line">没有匹配的分类</p>
        </div>

        <div class="scope-style-pane">
          <div class="scope-style-head">
            <strong>{{ paneTitle }}</strong>
            <span>{{ activeStyles.length }} 个</span>
            <button
              class="text-action"
              type="button"
              :disabled="!activeStyles.length"
              @click="toggleAllVisible"
            >
              {{ allVisibleSelected ? '取消全选' : '全选' }}
            </button>
          </div>
          <label
            v-for="style in activeStyles"
            :key="style.id || style.name"
            class="scope-style-row"
          >
            <el-checkbox
              :model-value="isStyleSelected(style.name)"
              @change="toggleStyle(style, $event)"
            />
            <span v-if="style.styleCode" class="scope-style-code">{{ style.styleCode }}</span>
            <span class="scope-style-name" :title="style.name">{{ style.name }}</span>
            <small v-if="search && style.categoryName" class="scope-style-category">{{ style.categoryName }}</small>
          </label>
          <p v-if="!activeStyles.length" class="empty-line">
            {{ search ? '没有匹配的风格' : '请选择左侧分类' }}
          </p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const SEARCH_KEY = '__search__'

const props = defineProps({
  modelValue: {
    type: Array,
    default: null,
  },
  styleItems: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const search = ref('')
const activeCategory = ref('')

const restricted = computed(() => Array.isArray(props.modelValue))
const allStyleNames = computed(() => props.styleItems.map((style) => style.name).filter(Boolean))
const allStyleNameSet = computed(() => new Set(allStyleNames.value))
const selectedNameSet = computed(() => new Set(Array.isArray(props.modelValue) ? props.modelValue : []))
const styleCategoryGroups = computed(() => {
  const groups = []
  const byName = new Map()
  props.styleItems.forEach((style) => {
    const categoryName = style.categoryName || '未归类'
    let group = byName.get(categoryName)
    if (!group) {
      group = { name: categoryName, styles: [] }
      byName.set(categoryName, group)
      groups.push(group)
    }
    group.styles.push(style)
  })
  return groups
})
const styleCategoryByName = computed(() => new Map(styleCategoryGroups.value.map((group) => [group.name, group])))
const selectedStyleCount = computed(() => {
  if (!restricted.value) return allStyleNames.value.length
  return props.modelValue.filter((name) => allStyleNameSet.value.has(name)).length
})
const unmatchedNames = computed(() => (
  restricted.value ? props.modelValue.filter((name) => !allStyleNameSet.value.has(name)) : []
))
const searchResults = computed(() => {
  const query = search.value.toLowerCase()
  if (!query) return []
  return props.styleItems.filter((style) => matchesQuery(style, query))
})
const visibleCategories = computed(() => {
  const query = search.value.toLowerCase()
  if (!query) {
    const selected = []
    const rest = []
    styleCategoryGroups.value.forEach((group) => {
      ;(group.styles.some((style) => selectedNameSet.value.has(style.name)) ? selected : rest).push(group)
    })
    return [...selected, ...rest]
  }
  return styleCategoryGroups.value.filter((group) => (
    group.name.toLowerCase().includes(query) || group.styles.some((style) => matchesQuery(style, query))
  ))
})
const activeStyles = computed(() => {
  const query = search.value.toLowerCase()
  if (activeCategory.value === SEARCH_KEY) return searchResults.value
  const group = styleCategoryByName.value.get(activeCategory.value) || visibleCategories.value[0]
  if (!group) return []
  return query ? group.styles.filter((style) => matchesQuery(style, query)) : group.styles
})
const paneTitle = computed(() => (
  activeCategory.value === SEARCH_KEY ? '搜索结果' : (activeCategory.value || '风格')
))
const allVisibleSelected = computed(() => (
  activeStyles.value.length > 0 && activeStyles.value.every((style) => selectedNameSet.value.has(style.name))
))

function matchesQuery(style, query) {
  return [style.name, style.styleCode, style.categoryName]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(query))
}

function ensureActiveCategory() {
  if (activeCategory.value === SEARCH_KEY || styleCategoryByName.value.has(activeCategory.value)) return
  const selectedGroup = styleCategoryGroups.value
    .find((group) => group.styles.some((style) => selectedNameSet.value.has(style.name)))
  activeCategory.value = selectedGroup?.name || styleCategoryGroups.value[0]?.name || ''
}

function updateSelection(names) {
  emit('update:modelValue', names)
}

function toggleRestriction(value) {
  if (value) {
    updateSelection([])
    ensureActiveCategory()
    return
  }
  updateSelection(null)
}

function setAll() {
  updateSelection(null)
}

function clearAll() {
  updateSelection([])
}

function removeUnmatched() {
  updateSelection(props.modelValue.filter((name) => allStyleNameSet.value.has(name)))
}

function handleSearchInput() {
  if (search.value) {
    activeCategory.value = SEARCH_KEY
    return
  }
  ensureActiveCategory()
}

function categoryState(group) {
  const count = group.styles.filter((style) => selectedNameSet.value.has(style.name)).length
  return {
    count,
    checked: count > 0 && count === group.styles.length,
    indeterminate: count > 0 && count < group.styles.length,
  }
}

function toggleCategoryStyles(group, checked) {
  const selected = new Set(props.modelValue || [])
  group.styles.forEach((style) => {
    if (checked) selected.add(style.name)
    else selected.delete(style.name)
  })
  updateSelection([...selected])
}

function isStyleSelected(name) {
  return selectedNameSet.value.has(name)
}

function toggleStyle(style, checked) {
  const selected = new Set(props.modelValue || [])
  if (checked) selected.add(style.name)
  else selected.delete(style.name)
  updateSelection([...selected])
}

function toggleAllVisible() {
  if (!activeStyles.value.length) return
  const selectAll = !allVisibleSelected.value
  const selected = new Set(props.modelValue || [])
  activeStyles.value.forEach((style) => {
    if (selectAll) selected.add(style.name)
    else selected.delete(style.name)
  })
  updateSelection([...selected])
}

watch(styleCategoryGroups, ensureActiveCategory)
watch(restricted, (value) => {
  if (value) ensureActiveCategory()
})

onMounted(ensureActiveCategory)
</script>

<style scoped>
.scope-picker {
  display: grid;
  gap: 10px;
  --el-checkbox-checked-bg-color: var(--gold-soft);
  --el-checkbox-checked-input-border-color: var(--gold-soft);
  --el-checkbox-input-border-color-hover: var(--gold-soft);
  --el-checkbox-checked-icon-color: #0b1417;
  --el-checkbox-input-border: 1px solid rgba(219, 232, 237, 0.3);
}

.scope-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.scope-restrict-switch {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}

.scope-restrict-switch :deep(.el-switch) {
  --el-switch-on-color: var(--gold-soft);
  --el-switch-off-color: rgba(255, 255, 255, 0.16);
}

.scope-search {
  flex: 1;
  min-width: 220px;
  min-height: 36px;
  box-sizing: border-box;
  padding: 0 12px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  outline: 0;
  background: rgba(255, 255, 255, 0.04);
}

.scope-search::-webkit-search-cancel-button {
  width: 13px;
  height: 13px;
  margin-right: 2px;
  background: var(--gold-soft);
  -webkit-appearance: none;
  appearance: none;
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23000' stroke-width='2.6' stroke-linecap='round'%3E%3Cpath d='M5 5l14 14M19 5 5 19'/%3E%3C/svg%3E") center / 100% 100% no-repeat;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23000' stroke-width='2.6' stroke-linecap='round'%3E%3Cpath d='M5 5l14 14M19 5 5 19'/%3E%3C/svg%3E") center / 100% 100% no-repeat;
  cursor: pointer;
  opacity: 0.75;
}

.scope-search::-webkit-search-cancel-button:hover {
  opacity: 1;
}

.scope-search::placeholder {
  color: var(--faint, var(--muted));
}

.scope-search:focus {
  border-color: rgba(224, 184, 74, 0.5);
}

.scope-count {
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.text-action {
  padding: 0;
  color: var(--gold-soft);
  background: none;
  border: 0;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.text-action:hover {
  color: var(--gold, var(--gold-soft));
}

.text-action:disabled {
  color: var(--muted);
  opacity: 0.6;
}

.scope-unmatched {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  color: #ffb8a3;
  border: 1px solid rgba(255, 132, 96, 0.32);
  border-radius: 8px;
  background: rgba(160, 46, 22, 0.18);
  font-size: 12px;
}

.scope-unrestricted {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  color: var(--muted);
  border: 1px dashed var(--line);
  border-radius: 8px;
  font-size: 13px;
}

.scope-restrict-hint {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

.scope-picker-grid {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 10px;
  height: 360px;
}

.scope-category-rail,
.scope-style-pane {
  min-height: 0;
  overflow-y: auto;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(7, 14, 17, 0.5);
}

.scope-category-rail {
  padding: 6px;
}

.scope-category-item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  gap: 6px;
  align-items: center;
  min-height: 40px;
  padding: 5px 7px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
}

.scope-category-result {
  grid-template-columns: minmax(0, 1fr) auto;
}

.scope-category-item:hover {
  background: rgba(255, 255, 255, 0.035);
}

.scope-category-item.active {
  border-color: rgba(216, 169, 53, 0.42);
  background: rgba(216, 169, 53, 0.1);
}

.scope-category-item :deep(.el-checkbox) {
  height: auto;
  margin: 0;
}

.scope-category-item :deep(.el-checkbox__label) {
  display: none;
}

.scope-category-name {
  min-width: 0;
  color: var(--text);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.scope-category-item small {
  color: var(--muted);
  font-size: 11px;
  white-space: nowrap;
}

.scope-style-pane {
  display: flex;
  flex-direction: column;
}

.scope-style-head {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid var(--line);
  background: #0d171b;
}

.scope-style-head strong {
  overflow: hidden;
  flex: 1;
  color: var(--text);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scope-style-head span {
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.scope-style-row {
  display: grid;
  grid-template-columns: 22px 72px minmax(0, 1fr) auto;
  gap: 6px;
  align-items: center;
  min-height: 36px;
  padding: 4px 10px;
  cursor: pointer;
}

.scope-style-row:hover {
  background: rgba(255, 255, 255, 0.035);
}

.scope-style-row :deep(.el-checkbox) {
  height: auto;
  margin: 0;
}

.scope-style-row :deep(.el-checkbox__label) {
  display: none;
}

.scope-style-code {
  overflow: hidden;
  color: var(--gold-soft);
  font-size: 11px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scope-style-name {
  overflow: hidden;
  color: var(--text);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scope-style-category {
  overflow: hidden;
  max-width: 140px;
  color: var(--muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-line {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.scope-style-pane .empty-line {
  padding: 16px 10px;
}

@media (max-width: 960px) {
  .scope-picker-grid {
    grid-template-columns: minmax(0, 1fr);
    height: auto;
  }

  .scope-category-rail {
    max-height: 200px;
  }

  .scope-style-pane {
    max-height: 320px;
  }
}
</style>
