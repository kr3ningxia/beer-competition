<template>
  <div class="style-library-page">
    <section class="page-head">
      <div>
        <h1>风格库管理</h1>
        <p>维护报名和评审使用的基础风格口径。</p>
      </div>
      <button class="tool-button primary" type="button" @click="startCreate">
        新建风格库
      </button>
    </section>

    <section class="library-layout">
      <aside class="library-rail" aria-label="风格库列表">
        <button
          v-for="library in styleLibraryOptions"
          :key="library.value"
          :class="['library-nav-item', { active: selectedLibrary.value === library.value }]"
          type="button"
          @click="selectedLibraryValue = library.value"
        >
          <span :class="['status-dot', library.statusLabel === '启用' ? 'success' : 'muted']" />
          <strong>{{ library.label }}</strong>
          <small>{{ library.categoryCount }} 类 · {{ library.styleCount }} 个风格</small>
        </button>
      </aside>

      <main class="style-workbench">
        <section class="library-summary">
          <div>
            <span :class="['status-pill', selectedLibrary.statusLabel === '启用' ? 'success' : 'muted']">{{ selectedLibrary.statusLabel }}</span>
            <h2>{{ selectedLibrary.label }}</h2>
            <p>{{ selectedLibrary.source }} · {{ selectedLibrary.version }} · {{ selectedLibrary.language }}</p>
          </div>
          <button type="button" @click="startEdit(selectedLibrary)">编辑</button>
        </section>

        <section class="style-browser">
          <aside class="category-panel">
            <header>
              <strong>分类</strong>
              <span>{{ selectedLibrary.categories.length }}</span>
            </header>
            <button
              v-for="category in selectedLibrary.categories"
              :key="category"
              :class="{ active: previewCategory === category }"
              type="button"
              @click="previewCategory = category"
            >
              {{ category }}
              <small>{{ countStylesInCategory(category) }}</small>
            </button>
          </aside>

          <section class="style-panel">
            <header>
              <label class="search-field">
                <input v-model.trim="previewKeyword" placeholder="搜索风格、编号或分类" />
              </label>
              <span>{{ previewStyles.length }} 个</span>
            </header>
            <div class="style-list">
              <article v-for="style in previewStyles" :key="styleKey(style)" class="style-row">
                <div>
                  <strong>{{ style.name }}</strong>
                  <small>{{ style.categoryName || '未归类' }}<template v-if="style.styleCode"> · {{ style.styleCode }}</template></small>
                </div>
                <p v-if="style.description">{{ style.description }}</p>
              </article>
              <p v-if="previewStyles.length === 0" class="empty-line">没有匹配的风格。</p>
            </div>
          </section>
        </section>
      </main>
    </section>

    <section v-if="editorOpen" class="editor-backdrop">
      <form class="editor-dialog" @submit.prevent="submitEditor">
        <header>
          <div>
            <span>风格库</span>
            <h2>{{ editingCode ? '编辑风格库' : '新建风格库' }}</h2>
          </div>
          <button type="button" @click="closeEditor">关闭</button>
        </header>

        <div class="editor-grid">
          <label>
            <span>编码</span>
            <input v-model.trim="editor.code" :disabled="Boolean(editingCode)" placeholder="例如 CUSTOM_2026" />
          </label>
          <label>
            <span>名称</span>
            <input v-model.trim="editor.name" placeholder="例如 主办方标准风格库" />
          </label>
          <label>
            <span>版本</span>
            <input v-model.trim="editor.version" placeholder="例如 2026A" />
          </label>
          <label>
            <span>语言</span>
            <input v-model.trim="editor.language" placeholder="例如 中文" />
          </label>
          <label>
            <span>来源</span>
            <input v-model.trim="editor.source" placeholder="例如 主办方" />
          </label>
          <label>
            <span>状态</span>
            <select v-model.number="editor.status">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </label>
        </div>

        <label class="stack-field">
          <span>标签</span>
          <input v-model.trim="editor.tagsText" placeholder="用逗号分隔，例如 报名必填, 支持搜索, 评审可见" />
        </label>

        <section class="structured-editor">
          <aside class="edit-category-panel">
            <div class="panel-title">
              <strong>分类</strong>
              <button type="button" @click="addCategory">添加</button>
            </div>
            <label v-for="category in editor.categories" :key="category.localId" :class="{ active: editorCategory === category.name }">
              <input v-model.trim="category.name" placeholder="分类名称" @focus="editorCategory = category.name" @input="syncCategoryName(category)" />
              <button type="button" @click="removeCategory(category.localId)">删除</button>
            </label>
            <p v-if="editor.categories.length === 0" class="empty-line">先添加分类。</p>
          </aside>

          <section class="edit-style-panel">
            <div class="panel-title">
              <label class="search-field">
                <input v-model.trim="editorKeyword" placeholder="搜索风格" />
              </label>
              <button type="button" @click="addStyle">添加风格</button>
            </div>
            <div class="edit-style-list">
              <button
                v-for="style in editorStyles"
                :key="style.localId"
                :class="{ active: selectedStyleId === style.localId }"
                type="button"
                @click="selectedStyleId = style.localId"
              >
                <span>
                  <strong>{{ style.name || '未命名风格' }}</strong>
                  <small>{{ style.categoryName || '未选择分类' }}<template v-if="style.styleCode"> · {{ style.styleCode }}</template></small>
                </span>
                <em>{{ style.status === 1 ? '启用' : '停用' }}</em>
              </button>
              <p v-if="editorStyles.length === 0" class="empty-line">当前分类没有风格。</p>
            </div>
          </section>

          <aside class="style-detail-panel">
            <template v-if="selectedStyle">
              <div class="panel-title">
                <strong>风格详情</strong>
                <button type="button" @click="removeStyle(selectedStyle.localId)">删除</button>
              </div>
              <label>
                <span>所属分类</span>
                <select v-model="selectedStyle.categoryName">
                  <option value="">选择分类</option>
                  <option v-for="category in cleanEditorCategories" :key="category" :value="category">{{ category }}</option>
                </select>
              </label>
              <label>
                <span>风格名称</span>
                <input v-model.trim="selectedStyle.name" placeholder="例如 American IPA" />
              </label>
              <label>
                <span>编号</span>
                <input v-model.trim="selectedStyle.styleCode" placeholder="例如 21A" />
              </label>
              <label>
                <span>评审可见说明</span>
                <textarea v-model.trim="selectedStyle.description" rows="5" placeholder="用一两句话说明评审时关注的风格口径"></textarea>
              </label>
              <label>
                <span>状态</span>
                <select v-model.number="selectedStyle.status">
                  <option :value="1">启用</option>
                  <option :value="0">停用</option>
                </select>
              </label>
            </template>
            <p v-else class="empty-line">选择一个风格查看详情。</p>
          </aside>
        </section>

        <details class="import-box">
          <summary>批量导入</summary>
          <textarea v-model="importText" rows="5" placeholder="每行：分类 | 风格名称 | 编号 | 评审可见说明"></textarea>
          <button type="button" @click="applyImportText">导入到列表</button>
        </details>

        <footer>
          <button type="button" @click="closeEditor">取消</button>
          <button class="primary" type="submit">保存</button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchStyleLibraries, saveStyleLibrary, updateStyleLibrary } from '@/api/admin'
import { defaultStyleLibraryValue, fallbackStyleLibraries, getStyleLibrary, normalizeStyleLibraries } from './styleLibraries'

const selectedLibraryValue = ref(defaultStyleLibraryValue)
const styleLibraryOptions = ref(normalizeStyleLibraries(fallbackStyleLibraries))
const selectedLibrary = computed(() => getStyleLibrary(selectedLibraryValue.value, styleLibraryOptions.value))
const previewCategory = ref('')
const previewKeyword = ref('')
const editorOpen = ref(false)
const editingCode = ref('')
const editorCategory = ref('')
const editorKeyword = ref('')
const selectedStyleId = ref('')
const importText = ref('')
const editor = reactive(createEmptyEditor())

const normalizedPreviewItems = computed(() => getStyleItems(selectedLibrary.value))
const previewStyles = computed(() => {
  const keyword = previewKeyword.value.toLowerCase()
  return normalizedPreviewItems.value.filter((style) => {
    const inCategory = !previewCategory.value || style.categoryName === previewCategory.value
    const text = [style.name, style.styleCode, style.categoryName].filter(Boolean).join(' ').toLowerCase()
    return inCategory && (!keyword || text.includes(keyword))
  })
})
const cleanEditorCategories = computed(() => editor.categories.map((category) => category.name.trim()).filter(Boolean))
const editorStyles = computed(() => {
  const keyword = editorKeyword.value.toLowerCase()
  return editor.styles.filter((style) => {
    const inCategory = !editorCategory.value || style.categoryName === editorCategory.value
    const text = [style.name, style.styleCode, style.categoryName].filter(Boolean).join(' ').toLowerCase()
    return inCategory && (!keyword || text.includes(keyword))
  })
})
const selectedStyle = computed(() => editor.styles.find((style) => style.localId === selectedStyleId.value))

onMounted(loadStyleLibraries)

watch(selectedLibrary, (library) => {
  if (!library.categories.includes(previewCategory.value)) {
    previewCategory.value = library.categories[0] || ''
  }
}, { immediate: true })

async function loadStyleLibraries() {
  try {
    const data = await fetchStyleLibraries()
    styleLibraryOptions.value = normalizeStyleLibraries(data)
    if (!styleLibraryOptions.value.some((library) => library.value === selectedLibraryValue.value)) {
      selectedLibraryValue.value = styleLibraryOptions.value[0]?.value || defaultStyleLibraryValue
    }
  } catch {
    styleLibraryOptions.value = normalizeStyleLibraries(fallbackStyleLibraries)
  }
}

function startCreate() {
  Object.assign(editor, createEmptyEditor())
  editingCode.value = ''
  editorCategory.value = ''
  selectedStyleId.value = ''
  importText.value = ''
  editorOpen.value = true
}

function startEdit(library) {
  selectedLibraryValue.value = library.value
  editingCode.value = library.value
  const categories = (library.categories || []).map((name) => ({ localId: createLocalId('category'), name }))
  const styles = getStyleItems(library).map((item) => ({
    localId: createLocalId('style'),
    categoryName: item.categoryName || '',
    name: item.name || '',
    styleCode: item.styleCode || '',
    description: item.description || '',
    status: item.status === 0 ? 0 : 1,
  }))
  Object.assign(editor, {
    code: library.value,
    name: library.label,
    version: library.version,
    language: library.language,
    source: library.source,
    status: library.status === 0 ? 0 : 1,
    tagsText: (library.tags || []).join(', '),
    categories,
    styles,
  })
  editorCategory.value = categories[0]?.name || ''
  selectedStyleId.value = styles[0]?.localId || ''
  importText.value = ''
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
}

async function submitEditor() {
  const payload = buildPayload()
  const issue = validatePayload(payload)
  if (issue) {
    ElMessage.warning(issue)
    return
  }
  const saved = editingCode.value
    ? await updateStyleLibrary(editingCode.value, payload)
    : await saveStyleLibrary(payload)
  await loadStyleLibraries()
  selectedLibraryValue.value = saved.value || saved.code || payload.code
  closeEditor()
  ElMessage.success('风格库已保存')
}

function createEmptyEditor() {
  return {
    code: '',
    name: '',
    version: '',
    language: '中文',
    source: '主办方',
    status: 1,
    tagsText: '报名必填, 支持搜索, 评审可见',
    categories: [],
    styles: [],
  }
}

function buildPayload() {
  const categories = cleanEditorCategories.value.map((name, index) => ({ name, sortOrder: index }))
  return {
    code: editor.code,
    name: editor.name,
    version: editor.version,
    language: editor.language,
    source: editor.source,
    status: Number(editor.status),
    tags: editor.tagsText.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean),
    categories,
    styles: editor.styles.map((style, index) => ({
      categoryName: style.categoryName,
      name: style.name,
      styleCode: style.styleCode,
      description: style.description,
      status: Number(style.status),
      sortOrder: index,
    })),
  }
}

function validatePayload(payload) {
  if (!payload.code || !payload.name || !payload.version || !payload.language || !payload.source) return '请补全风格库基础信息'
  if (!payload.categories.length) return '请至少配置 1 个分类'
  if (!payload.styles.length) return '请至少配置 1 个风格'
  const categorySet = new Set(payload.categories.map((category) => category.name))
  const missingCategory = payload.styles.find((style) => !categorySet.has(style.categoryName))
  if (missingCategory) return `风格分类不存在：${missingCategory.categoryName || missingCategory.name}`
  const unnamedStyle = payload.styles.find((style) => !style.name)
  if (unnamedStyle) return '风格名称不能为空'
  const duplicatedName = findDuplicated(payload.styles.map((style) => style.name))
  if (duplicatedName) return `风格名称不能重复：${duplicatedName}`
  const duplicatedCode = findDuplicated(payload.styles.map((style) => style.styleCode).filter(Boolean))
  if (duplicatedCode) return `风格编号不能重复：${duplicatedCode}`
  return ''
}

function addCategory() {
  const nextName = createUniqueCategoryName()
  editor.categories.push({ localId: createLocalId('category'), name: nextName })
  editorCategory.value = nextName
}

function removeCategory(localId) {
  const category = editor.categories.find((item) => item.localId === localId)
  if (!category) return
  if (editor.styles.some((style) => style.categoryName === category.name)) {
    ElMessage.warning('该分类下还有风格，先移动或删除这些风格')
    return
  }
  editor.categories = editor.categories.filter((item) => item.localId !== localId)
  editorCategory.value = editor.categories[0]?.name || ''
}

function syncCategoryName(category) {
  const oldName = editorCategory.value
  if (oldName && category.localId && editor.styles.some((style) => style.categoryName === oldName)) {
    editor.styles.forEach((style) => {
      if (style.categoryName === oldName) style.categoryName = category.name
    })
  }
  editorCategory.value = category.name
}

function addStyle() {
  if (!cleanEditorCategories.value.length) {
    ElMessage.warning('先添加分类')
    return
  }
  const categoryName = editorCategory.value || cleanEditorCategories.value[0]
  const style = {
    localId: createLocalId('style'),
    categoryName,
    name: '',
    styleCode: '',
    description: '',
    status: 1,
  }
  editor.styles.push(style)
  selectedStyleId.value = style.localId
}

function removeStyle(localId) {
  editor.styles = editor.styles.filter((style) => style.localId !== localId)
  selectedStyleId.value = editorStyles.value[0]?.localId || editor.styles[0]?.localId || ''
}

function applyImportText() {
  const rows = importText.value.split('\n').map((line) => line.trim()).filter(Boolean)
  if (!rows.length) {
    ElMessage.warning('请先填写导入内容')
    return
  }
  rows.forEach((line) => {
    const [categoryName = '', name = '', styleCode = '', description = ''] = line.split('|').map((part) => part.trim())
    if (!categoryName || !name) return
    if (!cleanEditorCategories.value.includes(categoryName)) {
      editor.categories.push({ localId: createLocalId('category'), name: categoryName })
    }
    editor.styles.push({
      localId: createLocalId('style'),
      categoryName,
      name,
      styleCode,
      description,
      status: 1,
    })
  })
  editorCategory.value = cleanEditorCategories.value[0] || ''
  selectedStyleId.value = editor.styles[0]?.localId || ''
  importText.value = ''
  ElMessage.success('已导入到列表，请检查后保存')
}

function getStyleItems(library) {
  if (library.styleItems?.length) return library.styleItems
  return (library.styles || []).map((name) => ({ name, categoryName: '', status: 1 }))
}

function countStylesInCategory(category) {
  return normalizedPreviewItems.value.filter((style) => style.categoryName === category).length
}

function styleKey(style) {
  return [style.categoryName, style.name, style.styleCode].filter(Boolean).join('|')
}

function findDuplicated(values) {
  const set = new Set()
  return values.find((value) => {
    if (set.has(value)) return true
    set.add(value)
    return false
  })
}

function createUniqueCategoryName() {
  let index = editor.categories.length + 1
  let name = `新分类 ${index}`
  while (cleanEditorCategories.value.includes(name)) {
    index += 1
    name = `新分类 ${index}`
  }
  return name
}

function createLocalId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}
</script>

<style scoped>
.style-library-page {
  --panel: rgba(22, 32, 36, 0.9);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e6edf0;
  --muted: #8da1aa;
  --gold-soft: #e0b84a;
  --green: #6fcf7a;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 26px 28px;
  color: var(--text);
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(216, 169, 53, 0.12), transparent 18rem),
    linear-gradient(135deg, #0d1418 0%, #111c20 50%, #0c1519 100%);
  background-size: 48px 48px, 48px 48px, auto, auto;
}

h1,
h2,
p,
dl {
  margin: 0;
}

button,
input,
select,
textarea {
  font: inherit;
}

button {
  cursor: pointer;
}

input,
select,
textarea {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  color: var(--text);
  border: 1px solid rgba(219, 232, 237, 0.14);
  border-radius: 8px;
  outline: 0;
  background-color: rgba(7, 14, 17, 0.72);
}

input,
select {
  min-height: 40px;
  padding: 0 10px;
}

textarea {
  resize: vertical;
  padding: 10px;
  line-height: 1.55;
}

.page-head,
.library-layout {
  max-width: 1440px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.page-head h1 {
  font-size: 30px;
  line-height: 1.15;
}

.page-head p,
small,
.empty-line,
.library-summary p {
  color: var(--muted);
}

.page-head p {
  margin-top: 8px;
}

.tool-button,
.library-summary button,
.editor-dialog footer button,
.editor-dialog header button,
.panel-title button,
.edit-category-panel label button,
.import-box button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 13px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font-weight: 800;
}

.tool-button.primary,
.library-summary button,
.editor-dialog footer button.primary {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.library-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 14px;
  margin-top: 18px;
  align-items: start;
}

.library-rail,
.style-workbench,
.category-panel,
.style-panel,
.edit-category-panel,
.edit-style-panel,
.style-detail-panel {
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.library-rail {
  position: sticky;
  top: 18px;
  display: grid;
  gap: 8px;
  padding: 10px;
}

.library-nav-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 8px 10px;
  min-height: 70px;
  padding: 10px;
  color: var(--text);
  text-align: left;
  border: 1px solid transparent;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.024);
}

.library-nav-item.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.07);
}

.library-nav-item strong,
.library-nav-item small {
  grid-column: 2;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-dot {
  grid-row: 1 / span 2;
  align-self: start;
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: var(--muted);
}

.status-dot.success {
  background: var(--green);
  box-shadow: 0 0 0 4px rgba(111, 207, 122, 0.09);
}

.style-workbench {
  display: grid;
  gap: 14px;
  padding: 14px;
}

.library-summary {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  padding: 6px 4px 14px;
  border-bottom: 1px solid var(--line);
}

.library-summary h2 {
  margin-top: 8px;
  font-size: 24px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 9px;
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--muted);
  background: rgba(255, 255, 255, 0.026);
  font-size: 12px;
  font-weight: 800;
}

.status-pill.success {
  color: var(--green);
  border-color: rgba(111, 207, 122, 0.22);
  background: rgba(111, 207, 122, 0.06);
}

.style-browser {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 12px;
}

.category-panel,
.style-panel {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 12px;
}

.category-panel header,
.style-panel header,
.panel-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.category-panel button {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  min-height: 36px;
  padding: 0 10px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.category-panel button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.07);
}

.search-field {
  flex: 1 1 auto;
}

.style-list,
.edit-style-list {
  display: grid;
  gap: 8px;
  max-height: 560px;
  overflow: auto;
  padding-right: 4px;
}

.style-row {
  display: grid;
  gap: 6px;
  padding: 11px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.024);
}

.style-row div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.style-row p {
  color: #b6c8cf;
  line-height: 1.55;
}

.editor-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(1, 7, 9, 0.72);
}

.editor-dialog {
  display: grid;
  gap: 14px;
  width: min(1180px, 100%);
  max-height: min(840px, 92vh);
  overflow: auto;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(15, 24, 28, 0.98);
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.42);
}

.editor-dialog header,
.editor-dialog footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.editor-dialog header span,
.stack-field span,
.editor-grid label span,
.style-detail-panel label span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.editor-grid label,
.stack-field,
.style-detail-panel label {
  display: grid;
  gap: 7px;
}

.structured-editor {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr) 340px;
  gap: 10px;
  min-height: 430px;
}

.edit-category-panel,
.edit-style-panel,
.style-detail-panel {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
  padding: 12px;
}

.edit-category-panel label {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.edit-category-panel label.active input {
  border-color: rgba(216, 169, 53, 0.38);
}

.edit-category-panel label button,
.style-detail-panel .panel-title button {
  min-height: 40px;
  color: #ffb4ad;
}

.edit-style-list button {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  min-height: 58px;
  padding: 9px;
  color: var(--text);
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.024);
}

.edit-style-list button.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.07);
}

.edit-style-list span {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.edit-style-list strong,
.edit-style-list small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-style-list em {
  color: var(--gold-soft);
  font-style: normal;
  font-size: 12px;
  font-weight: 800;
}

.import-box {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.018);
}

.import-box summary {
  cursor: pointer;
  font-weight: 800;
}

.import-box textarea {
  margin-top: 10px;
}

.import-box button {
  margin-top: 10px;
}

.editor-dialog footer {
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .library-layout,
  .style-browser,
  .structured-editor {
    grid-template-columns: 1fr;
  }

  .library-rail {
    position: static;
  }
}

@media (max-width: 760px) {
  .style-library-page {
    padding: 18px;
  }

  .page-head,
  .library-summary,
  .editor-dialog header,
  .editor-dialog footer {
    display: grid;
  }

  .editor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
