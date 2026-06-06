<template>
  <div class="style-library-page">
    <header class="style-page-header">
      <h1>风格库管理</h1>
    </header>

    <main class="library-shell">
      <aside class="library-rail" aria-label="风格库列表">
        <div class="rail-head">
          <strong>风格库 <span>{{ styleLibraryOptions.length }}</span></strong>
          <button class="tool-button primary compact" type="button" @click="startCreate">
            <Plus />
            新建
          </button>
        </div>

        <label class="search-field">
          <Search />
          <input v-model.trim="libraryKeyword" placeholder="搜索风格库" />
          <button v-if="libraryKeyword" class="search-clear" type="button" aria-label="清除搜索" @click="libraryKeyword = ''">
            <Close />
          </button>
        </label>

        <div class="library-list">
          <button
            v-for="library in filteredLibraryOptions"
            :key="library.value"
            :class="['library-nav-item', { active: selectedLibrary.value === library.value }]"
            type="button"
            @click="selectedLibraryValue = library.value"
          >
            <strong>{{ library.label }}</strong>
            <small>
              <span><Files />{{ library.categoryCount }} 类</span>
              <span><DocumentCopy />{{ library.styleCount }} 风格</span>
            </small>
          </button>
          <p v-if="filteredLibraryOptions.length === 0" class="empty-line list-empty">没有匹配的风格库。</p>
        </div>
      </aside>

      <section class="library-detail">
        <section class="library-summary">
          <div class="summary-copy">
            <div class="title-line">
              <h2>{{ selectedLibrary.label }}</h2>
              <span :class="['status-pill', selectedLibrary.statusLabel === '启用' ? 'success' : 'muted']">{{ selectedLibrary.statusLabel }}</span>
            </div>
            <div class="summary-meta">
              <span>{{ selectedLibrary.source }} · {{ selectedLibrary.version }} · {{ selectedLibrary.language }}</span>
              <i />
              <span>分类 <strong>{{ selectedLibrary.categories.length }}</strong></span>
              <span>风格 <strong>{{ normalizedPreviewItems.length }}</strong></span>
              <i v-if="selectedLibrary.updatedAt" />
              <span v-if="selectedLibrary.updatedAt">更新于 {{ selectedLibrary.updatedAt }}</span>
            </div>
          </div>
          <button class="tool-button outline" type="button" @click="startEdit(selectedLibrary)">
            <EditPen />
            编辑
          </button>
        </section>

        <section class="style-browser">
          <aside class="category-panel" aria-label="风格分类">
            <div class="category-head">
              <strong>分类</strong>
              <span>{{ selectedLibrary.categories.length }}</span>
            </div>

            <label class="search-field small">
              <Search />
              <input v-model.trim="categoryKeyword" placeholder="按名称或编号筛选分类" />
              <button v-if="categoryKeyword" class="search-clear" type="button" aria-label="清除分类筛选" @click="categoryKeyword = ''">
                <Close />
              </button>
            </label>

            <div class="category-list">
              <button :class="{ active: previewCategory === '' }" type="button" @click="previewCategory = ''">
                <span class="category-index">全部</span>
                <span class="category-name">全部分类</span>
                <small>{{ normalizedPreviewItems.length }}</small>
              </button>
              <button
                v-for="category in filteredPreviewCategories"
                :key="category"
                :class="{ active: previewCategory === category }"
                type="button"
                @click="previewCategory = category"
              >
                <span class="category-index">{{ categoryNumber(category) }}</span>
                <span class="category-name" :title="categoryName(category)">{{ displayCategoryName(category) }}</span>
                <small>{{ countStylesInCategory(category) }}</small>
              </button>
              <p v-if="filteredPreviewCategories.length === 0" class="empty-line list-empty">无匹配分类。</p>
            </div>
          </aside>

          <section class="style-panel">
            <header class="style-toolbar">
              <label class="search-field">
                <Search />
                <input v-model.trim="previewKeyword" placeholder="搜索风格名称、编号或分类" />
                <button v-if="previewKeyword" class="search-clear" type="button" aria-label="清除风格筛选" @click="previewKeyword = ''">
                  <Close />
                </button>
              </label>
              <div class="style-count">
                <span>{{ selectedCategoryText }}</span>
                <i />
                <strong>{{ previewStyles.length }}</strong> 个风格
              </div>
            </header>

            <div v-if="previewCategory || previewKeyword" class="filter-chips">
              <button v-if="previewCategory" type="button" @click="previewCategory = ''">
                {{ selectedCategoryText }}
                <Close />
              </button>
              <button v-if="previewKeyword" type="button" @click="previewKeyword = ''">
                关键词“{{ previewKeyword }}”
                <Close />
              </button>
            </div>

            <div class="style-list">
              <article v-for="style in previewStyles" :key="styleKey(style)" class="style-row">
                <div class="style-title-line">
                  <span v-if="style.styleCode" class="style-code">{{ style.styleCode }}</span>
                  <strong>{{ style.name }}</strong>
                </div>
                <p v-if="style.description">{{ style.description }}</p>
                <footer>
                  <span>{{ styleCategoryText(style) }}</span>
                  <template v-if="style.styleCode">
                    <i />
                    <span>{{ style.styleCode }}</span>
                  </template>
                </footer>
              </article>
              <p v-if="previewStyles.length === 0" class="empty-line style-empty">未找到匹配的风格。</p>
            </div>
          </section>
        </section>
      </section>
    </main>

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

          <section class="style-editor-main">
            <section class="edit-style-panel">
              <div class="panel-title">
                <label class="search-field">
                  <Search />
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
                <div class="style-detail-grid">
                  <label class="detail-category">
                    <span>所属分类</span>
                    <select v-model="selectedStyle.categoryName">
                      <option value="">选择分类</option>
                      <option v-for="category in cleanEditorCategories" :key="category" :value="category">{{ category }}</option>
                    </select>
                  </label>
                  <label>
                    <span>编号</span>
                    <input v-model.trim="selectedStyle.styleCode" placeholder="例如 21A" />
                  </label>
                  <label>
                    <span>状态</span>
                    <select v-model.number="selectedStyle.status">
                      <option :value="1">启用</option>
                      <option :value="0">停用</option>
                    </select>
                  </label>
                  <label class="detail-name">
                    <span>风格名称</span>
                    <input v-model.trim="selectedStyle.name" placeholder="例如 American IPA" />
                  </label>
                  <label class="detail-description">
                    <span>评审可见说明</span>
                    <textarea v-model.trim="selectedStyle.description" rows="3" placeholder="用一两句话说明评审时关注的风格口径"></textarea>
                  </label>
                </div>
              </template>
              <p v-else class="empty-line">选择一个风格查看详情。</p>
            </aside>
          </section>
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
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, DocumentCopy, EditPen, Files, Plus, Search } from '@element-plus/icons-vue'
import { fetchStyleLibraries, saveStyleLibrary, updateStyleLibrary } from '@/api/admin'
import { defaultStyleLibraryValue, fallbackStyleLibraries, getStyleLibrary, normalizeStyleLibraries } from './styleLibraries'

const route = useRoute()
const selectedLibraryValue = ref(defaultStyleLibraryValue)
const styleLibraryOptions = ref(normalizeStyleLibraries(fallbackStyleLibraries))
const selectedLibrary = computed(() => getStyleLibrary(selectedLibraryValue.value, styleLibraryOptions.value))
const libraryKeyword = ref('')
const previewCategory = ref('')
const categoryKeyword = ref('')
const previewKeyword = ref('')
const editorOpen = ref(false)
const editingCode = ref('')
const editorCategory = ref('')
const editorKeyword = ref('')
const selectedStyleId = ref('')
const importText = ref('')
const editor = reactive(createEmptyEditor())

const normalizedPreviewItems = computed(() => getStyleItems(selectedLibrary.value))
const filteredLibraryOptions = computed(() => {
  const keyword = libraryKeyword.value.toLowerCase()
  if (!keyword) return styleLibraryOptions.value
  return styleLibraryOptions.value.filter((library) => {
    const text = [library.label, library.source, library.version, library.language].filter(Boolean).join(' ').toLowerCase()
    return text.includes(keyword)
  })
})
const filteredPreviewCategories = computed(() => {
  const keyword = categoryKeyword.value.toLowerCase()
  if (!keyword) return selectedLibrary.value.categories
  return selectedLibrary.value.categories.filter((category) => {
    const text = [category, categoryNumber(category), categoryName(category), displayCategoryName(category)].filter(Boolean).join(' ').toLowerCase()
    return text.includes(keyword)
  })
})
const previewStyles = computed(() => {
  const keyword = previewKeyword.value.toLowerCase()
  return normalizedPreviewItems.value.filter((style) => {
    const inCategory = !previewCategory.value || style.categoryName === previewCategory.value
    const text = [style.name, style.styleCode, style.categoryName].filter(Boolean).join(' ').toLowerCase()
    return inCategory && (!keyword || text.includes(keyword))
  })
})
const selectedCategoryText = computed(() => (previewCategory.value ? categoryLabel(previewCategory.value) : '全部分类'))
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

watch(() => route.query.library, applyRouteLibrarySelection)

watch(selectedLibrary, () => {
  previewCategory.value = ''
  categoryKeyword.value = ''
  previewKeyword.value = ''
}, { immediate: true })

async function loadStyleLibraries() {
  try {
    const data = await fetchStyleLibraries()
    styleLibraryOptions.value = normalizeStyleLibraries(data)
    const routeLibrary = normalizeRouteLibraryValue(route.query.library)
    if (routeLibrary && styleLibraryOptions.value.some((library) => library.value === routeLibrary)) {
      selectedLibraryValue.value = routeLibrary
    } else if (!styleLibraryOptions.value.some((library) => library.value === selectedLibraryValue.value)) {
      selectedLibraryValue.value = styleLibraryOptions.value[0]?.value || defaultStyleLibraryValue
    }
  } catch {
    styleLibraryOptions.value = normalizeStyleLibraries(fallbackStyleLibraries)
    applyRouteLibrarySelection()
  }
}

function applyRouteLibrarySelection() {
  const routeLibrary = normalizeRouteLibraryValue(route.query.library)
  if (!routeLibrary) return
  if (styleLibraryOptions.value.some((library) => library.value === routeLibrary)) {
    selectedLibraryValue.value = routeLibrary
  }
}

function normalizeRouteLibraryValue(value) {
  return Array.isArray(value) ? value[0] : value
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

function categoryNumber(category) {
  const match = String(category || '').match(/^(\d+)[.．、\s]+(.+)$/)
  if (match) return match[1]
  const index = selectedLibrary.value.categories.indexOf(category)
  return index >= 0 ? String(index + 1) : ''
}

function categoryName(category) {
  const match = String(category || '').match(/^(\d+)[.．、\s]+(.+)$/)
  return match ? match[2] : category
}

function displayCategoryName(category) {
  const name = String(categoryName(category) || '')
  if (!isUppercaseEnglishName(name)) return name
  return name.toLowerCase().replace(/\b[a-z]/g, (letter) => letter.toUpperCase())
}

function categoryLabel(category) {
  const number = categoryNumber(category)
  const name = displayCategoryName(category)
  return number ? `${number}. ${name}` : name
}

function styleCategoryText(style) {
  return style.categoryName ? categoryLabel(style.categoryName) : '未归类'
}

function isUppercaseEnglishName(value) {
  return /[A-Z]/.test(value) && !/[\u4e00-\u9fa5]/.test(value) && value === value.toUpperCase()
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
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 18px;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
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
  width: 100%;
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
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 14px;
  min-height: 0;
  align-items: stretch;
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
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow: auto;
  padding: 10px;
  overscroll-behavior: contain;
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

.style-workbench {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
  min-height: 0;
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
  min-height: 0;
}

.category-panel,
.style-panel {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  padding: 12px;
}

.category-panel {
  overflow: auto;
  overscroll-behavior: contain;
}

.style-panel {
  grid-template-rows: auto minmax(0, 1fr);
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
  min-height: 38px;
  padding: 7px 10px;
  color: var(--text);
  text-align: left;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  line-height: 1.25;
}

.category-panel button.active {
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.3);
  background: rgba(216, 169, 53, 0.07);
}

.category-panel button small {
  flex: 0 0 auto;
}

.search-field {
  flex: 1 1 auto;
}

.style-list,
.edit-style-list {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
  overscroll-behavior: contain;
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
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background: rgba(1, 7, 9, 0.72);
}

.editor-dialog {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto;
  gap: 12px;
  width: min(1220px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  min-height: min(800px, calc(100vh - 32px));
  overflow: hidden;
  padding: 16px;
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
  gap: 9px 10px;
}

.editor-grid label,
.stack-field,
.style-detail-panel label {
  display: grid;
  gap: 7px;
}

.structured-editor {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.style-editor-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 10px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.edit-category-panel,
.edit-style-panel,
.style-detail-panel {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: 12px;
  overscroll-behavior: contain;
}

.edit-style-panel {
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.edit-style-panel .edit-style-list {
  overflow-x: hidden;
  overflow-y: auto;
}

.edit-style-panel .panel-title {
  align-items: stretch;
}

.edit-style-panel .panel-title .search-field {
  flex: 1 1 auto;
}

.edit-style-panel .panel-title button {
  flex: 0 0 auto;
  min-height: 40px;
  white-space: nowrap;
}

.style-detail-panel {
  overflow: hidden;
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
  width: 100%;
  min-width: 0;
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
  flex: 0 0 auto;
  color: var(--gold-soft);
  font-style: normal;
  font-size: 12px;
  font-weight: 800;
}

.style-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(96px, 0.6fr) minmax(96px, 0.6fr);
  gap: 9px 10px;
  min-width: 0;
}

.style-detail-grid label {
  min-width: 0;
}

.style-detail-grid .detail-name,
.style-detail-grid .detail-description {
  grid-column: 1 / -1;
}

.style-detail-panel textarea {
  min-height: 82px;
  resize: none;
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

.library-rail::-webkit-scrollbar,
.category-panel::-webkit-scrollbar,
.style-list::-webkit-scrollbar,
.edit-category-panel::-webkit-scrollbar,
.edit-style-list::-webkit-scrollbar,
.style-detail-panel::-webkit-scrollbar {
  width: 8px;
}

.library-rail::-webkit-scrollbar-track,
.category-panel::-webkit-scrollbar-track,
.style-list::-webkit-scrollbar-track,
.edit-category-panel::-webkit-scrollbar-track,
.edit-style-list::-webkit-scrollbar-track,
.style-detail-panel::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.library-rail::-webkit-scrollbar-thumb,
.category-panel::-webkit-scrollbar-thumb,
.style-list::-webkit-scrollbar-thumb,
.edit-category-panel::-webkit-scrollbar-thumb,
.edit-style-list::-webkit-scrollbar-thumb,
.style-detail-panel::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.24);
}

.style-library-page {
  --panel: #141c20;
  --panel-soft: rgba(255, 255, 255, 0.026);
  --panel-strong: rgba(255, 255, 255, 0.045);
  --input-bg: #0b1114;
  --line: rgba(219, 232, 237, 0.1);
  --line-strong: rgba(219, 232, 237, 0.16);
  --text: #edf4f6;
  --muted: #8ea1aa;
  --gold-soft: #d9aa39;
  --green: #70d486;
  grid-template-rows: 62px minmax(0, 1fr);
  gap: 0;
  padding: 0;
  background: #0d1418;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.022) 1px, transparent 1px);
  background-size: 48px 48px;
}

.style-page-header {
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid var(--line);
}

.style-page-header h1 {
  font-size: 20px;
  line-height: 1;
  letter-spacing: 0;
}

.library-shell {
  display: grid;
  grid-template-columns: 258px minmax(0, 1fr);
  min-height: 0;
}

.library-rail {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  padding: 14px 10px;
  overflow: hidden;
  border: 0;
  border-right: 1px solid var(--line);
  border-radius: 0;
  background: rgba(11, 17, 20, 0.4);
}

.rail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 2px 12px;
}

.rail-head strong {
  font-size: 14px;
}

.rail-head span {
  margin-left: 3px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 500;
}

.tool-button {
  gap: 6px;
  min-height: 34px;
  border-radius: 8px;
}

.tool-button svg,
.search-field svg,
.library-nav-item svg,
.filter-chips svg {
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
}

.tool-button.compact {
  min-height: 32px;
  padding: 0 11px;
}

.tool-button.outline {
  color: var(--text);
  border-color: var(--line-strong);
  background: rgba(255, 255, 255, 0.025);
}

.search-field {
  position: relative;
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  width: 100%;
  min-width: 0;
}

.search-field > svg {
  position: absolute;
  left: 10px;
  z-index: 1;
  color: var(--muted);
}

.search-field input {
  min-height: 34px;
  padding: 0 32px 0 32px;
  border-radius: 8px;
  background: rgba(8, 13, 16, 0.8);
  font-size: 13px;
}

.search-field.small input {
  min-height: 32px;
}

.search-clear {
  position: absolute;
  right: 7px;
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: var(--muted);
  border: 0;
  background: transparent;
}

.search-clear:hover {
  color: var(--text);
}

.library-list {
  display: grid;
  align-content: start;
  gap: 6px;
  min-height: 0;
  margin-top: 12px;
  overflow: auto;
  padding-right: 2px;
  overscroll-behavior: contain;
}

.library-nav-item {
  position: relative;
  grid-template-columns: minmax(0, 1fr);
  gap: 7px;
  min-height: 78px;
  padding: 10px 9px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
  transition: background 0.16s ease, border-color 0.16s ease;
}

.library-nav-item:hover {
  border-color: rgba(219, 232, 237, 0.12);
  background: rgba(255, 255, 255, 0.04);
}

.library-nav-item.active {
  border-color: rgba(217, 170, 57, 0.5);
  background: rgba(217, 170, 57, 0.09);
}

.library-nav-item strong {
  grid-column: 1;
  white-space: normal;
  line-height: 1.3;
}

.library-nav-item small {
  grid-column: 1;
  display: flex;
  gap: 12px;
}

.library-nav-item small span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.library-detail {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
}

.library-summary {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  padding: 22px 24px 20px;
  border-bottom: 1px solid var(--line);
}

.summary-copy {
  min-width: 0;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.title-line h2 {
  min-width: 0;
  overflow: hidden;
  font-size: 23px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}

.summary-meta strong {
  color: var(--text);
}

.summary-meta i,
.style-count i,
.style-row footer i {
  display: block;
  width: 1px;
  height: 13px;
  background: var(--line);
}

.status-pill {
  min-height: 24px;
  border-radius: 7px;
}

.style-browser {
  grid-template-columns: 288px minmax(0, 1fr);
  gap: 0;
  min-height: 0;
}

.category-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 16px 14px 12px;
  overflow: hidden;
  border: 0;
  border-right: 1px solid var(--line);
  border-radius: 0;
  background: transparent;
}

.category-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.category-head span {
  min-width: 28px;
  padding: 2px 7px;
  color: var(--muted);
  text-align: center;
  border-radius: 7px;
  background: var(--panel-strong);
  font-size: 12px;
}

.category-list {
  display: grid;
  align-content: start;
  gap: 3px;
  min-height: 0;
  margin-top: 10px;
  overflow: auto;
  overscroll-behavior: contain;
}

.category-list button {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 7px;
  align-items: center;
  min-height: 36px;
  padding: 7px 8px;
  color: rgba(237, 244, 246, 0.82);
  text-align: left;
  border: 0;
  border-radius: 8px;
  background: transparent;
  line-height: 1.25;
}

.category-list button:hover {
  background: rgba(255, 255, 255, 0.045);
}

.category-list button.active {
  color: var(--gold-soft);
  background: rgba(217, 170, 57, 0.12);
  font-weight: 800;
}

.category-name {
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  line-height: 1.18;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.category-index {
  color: var(--muted);
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}

.category-list button.active .category-index {
  color: var(--gold-soft);
}

.category-list small {
  min-width: 22px;
  padding: 1px 6px;
  text-align: center;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.055);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.style-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.style-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 62px;
  padding: 13px 24px;
  border-bottom: 1px solid var(--line);
}

.style-toolbar .search-field {
  flex: 1 1 auto;
}

.style-count {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  max-width: 280px;
  color: var(--muted);
  font-size: 13px;
}

.style-count span {
  max-width: 170px;
  overflow: hidden;
  color: var(--text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style-count strong {
  color: var(--text);
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 24px 0;
}

.filter-chips button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 260px;
  min-height: 28px;
  padding: 0 8px 0 10px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.04);
  font-size: 12px;
}

.filter-chips button svg {
  width: 13px;
  height: 13px;
  color: var(--muted);
}

.style-list {
  display: grid;
  align-content: start;
  flex: 1 1 auto;
  gap: 12px;
  min-height: 0;
  overflow: auto;
  padding: 14px 24px 24px;
}

.style-row {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid rgba(219, 232, 237, 0.09);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.027);
  transition: background 0.16s ease, border-color 0.16s ease;
}

.style-row:hover {
  border-color: rgba(217, 170, 57, 0.26);
  background: rgba(255, 255, 255, 0.04);
}

.style-row .style-title-line {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  min-width: 0;
}

.style-title-line strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.style-code {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 21px;
  padding: 0 7px;
  color: var(--gold-soft);
  border-radius: 7px;
  background: rgba(217, 170, 57, 0.12);
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  font-weight: 800;
}

.style-row p {
  color: #a9bbc2;
  font-size: 13px;
  line-height: 1.55;
}

.style-row footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  padding-top: 8px;
  color: rgba(142, 161, 170, 0.88);
  border-top: 1px solid rgba(219, 232, 237, 0.07);
  font-size: 12px;
}

.list-empty,
.style-empty {
  padding: 28px 12px;
  text-align: center;
}

.library-list::-webkit-scrollbar,
.category-list::-webkit-scrollbar {
  width: 8px;
}

.library-list::-webkit-scrollbar-track,
.category-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.library-list::-webkit-scrollbar-thumb,
.category-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
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
