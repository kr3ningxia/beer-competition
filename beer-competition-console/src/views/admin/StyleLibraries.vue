<template>
  <div class="style-library-page">
    <section class="page-head">
      <div>
        <h1>风格库管理</h1>
        <p>维护报名时可选的啤酒风格库，比赛可从这里选择一套口径。</p>
      </div>
      <button class="tool-button primary" type="button" @click="startCreate">
        新建风格库
      </button>
    </section>

    <section class="library-layout">
      <section class="library-grid" aria-label="风格库列表">
        <article
          v-for="library in styleLibraryOptions"
          :key="library.value"
          :class="['library-card', { active: selectedLibrary.value === library.value }]"
        >
          <header>
            <div>
              <span :class="['status-pill', library.statusLabel === '启用' ? 'success' : 'muted']">{{ library.statusLabel }}</span>
              <h2>{{ library.label }}</h2>
            </div>
          </header>
          <dl class="library-meta">
            <div>
              <dt>版本</dt>
              <dd>{{ library.version }}</dd>
            </div>
            <div>
              <dt>语言</dt>
              <dd>{{ library.language }}</dd>
            </div>
            <div>
              <dt>分类</dt>
              <dd>{{ library.categoryCount }}</dd>
            </div>
            <div>
              <dt>风格</dt>
              <dd>{{ library.styleCount }}</dd>
            </div>
          </dl>
          <div class="tag-row">
            <span v-for="tag in library.tags" :key="tag">{{ tag }}</span>
          </div>
          <footer>
            <small>{{ library.source }} · 更新于 {{ library.updatedAt }}</small>
            <div>
              <button type="button" @click="selectedLibraryValue = library.value">查看风格</button>
              <button type="button" @click="startEdit(library)">编辑</button>
            </div>
          </footer>
        </article>
      </section>

      <aside class="preview-panel">
        <header>
          <span>风格预览</span>
          <h2>{{ selectedLibrary.label }}</h2>
        </header>
        <div class="preview-list">
          <span v-for="style in selectedLibrary.styles" :key="style">{{ style }}</span>
        </div>
      </aside>
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
            <input v-model.trim="editor.name" placeholder="例如 华南酸啤风格库" />
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
        <label class="stack-field">
          <span>分类</span>
          <textarea v-model="editor.categoriesText" rows="3" placeholder="每行一个分类"></textarea>
        </label>
        <label class="stack-field">
          <span>风格</span>
          <textarea v-model="editor.stylesText" rows="8" placeholder="每行一个风格，可写为 分类 | 风格名称 | 编号"></textarea>
        </label>

        <footer>
          <button type="button" @click="closeEditor">取消</button>
          <button class="primary" type="submit">保存</button>
        </footer>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchStyleLibraries, saveStyleLibrary, updateStyleLibrary } from '@/api/admin'
import { defaultStyleLibraryValue, fallbackStyleLibraries, getStyleLibrary, normalizeStyleLibraries } from './styleLibraries'

const selectedLibraryValue = ref(defaultStyleLibraryValue)
const styleLibraryOptions = ref(normalizeStyleLibraries(fallbackStyleLibraries))
const selectedLibrary = computed(() => getStyleLibrary(selectedLibraryValue.value, styleLibraryOptions.value))
const editorOpen = ref(false)
const editingCode = ref('')
const editor = reactive(createEmptyEditor())

onMounted(loadStyleLibraries)

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
  editorOpen.value = true
}

function startEdit(library) {
  selectedLibraryValue.value = library.value
  editingCode.value = library.value
  Object.assign(editor, {
    code: library.value,
    name: library.label,
    version: library.version,
    language: library.language,
    source: library.source,
    status: library.status === 0 ? 0 : 1,
    tagsText: (library.tags || []).join(', '),
    categoriesText: (library.categories || []).join('\n'),
    stylesText: buildStylesText(library),
  })
  editorOpen.value = true
}

function closeEditor() {
  editorOpen.value = false
}

async function submitEditor() {
  const payload = buildPayload()
  if (!payload.code || !payload.name || !payload.version || !payload.language || !payload.source) {
    ElMessage.warning('请补全风格库基础信息')
    return
  }
  if (!payload.styles.length) {
    ElMessage.warning('请至少配置 1 个风格')
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
    categoriesText: '',
    stylesText: '',
  }
}

function buildPayload() {
  const categories = editor.categoriesText
    .split('\n')
    .map((name) => name.trim())
    .filter(Boolean)
    .map((name, index) => ({ name, sortOrder: index }))
  return {
    code: editor.code,
    name: editor.name,
    version: editor.version,
    language: editor.language,
    source: editor.source,
    status: Number(editor.status),
    tags: editor.tagsText.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean),
    categories,
    styles: parseStylesText(editor.stylesText),
  }
}

function buildStylesText(library) {
  const items = library.styleItems?.length
    ? library.styleItems
    : (library.styles || []).map((name) => ({ name }))
  return items.map((item) => [item.categoryName, item.name, item.styleCode].filter(Boolean).join(' | ')).join('\n')
}

function parseStylesText(value) {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const parts = line.split('|').map((part) => part.trim()).filter(Boolean)
      if (parts.length >= 3) {
        return { categoryName: parts[0], name: parts[1], styleCode: parts[2], sortOrder: index, status: 1 }
      }
      if (parts.length === 2) {
        return { categoryName: parts[0], name: parts[1], sortOrder: index, status: 1 }
      }
      return { name: parts[0], sortOrder: index, status: 1 }
    })
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
  cursor: pointer;
  font: inherit;
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
}

svg {
  width: 1em;
  height: 1em;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  max-width: 1420px;
  margin: 0 auto 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.page-head h1 {
  font-size: 30px;
  line-height: 1.15;
}

.page-head p,
small,
dt {
  color: var(--muted);
}

.page-head p {
  margin-top: 8px;
}

.tool-button,
.library-card footer button,
.editor-dialog footer button,
.editor-dialog header button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 13px;
  color: var(--text);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font-weight: 800;
}

.tool-button.primary,
.library-card footer button,
.editor-dialog footer button.primary {
  flex: 0 0 auto;
  white-space: nowrap;
  color: var(--gold-soft);
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.library-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
  max-width: 1420px;
  margin: 0 auto;
  align-items: start;
}

.library-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.library-card {
  display: grid;
  gap: 16px;
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.library-card.active {
  border-color: rgba(216, 169, 53, 0.34);
  background: rgba(216, 169, 53, 0.045);
}

.library-card header,
.library-card footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.library-card footer div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.library-card h2 {
  margin-top: 10px;
  font-size: 22px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
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

.library-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.library-meta div,
.tag-row span {
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.026);
}

.library-meta dd {
  margin: 5px 0 0;
  color: var(--text);
  font-weight: 900;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span {
  padding: 7px 10px;
  color: var(--text);
}

.preview-panel {
  position: sticky;
  top: 18px;
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(22, 32, 36, 0.94);
}

.preview-panel header {
  display: grid;
  gap: 8px;
}

.preview-panel header span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.preview-panel h2 {
  font-size: 20px;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-list span {
  padding: 8px 10px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
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
  width: min(820px, 100%);
  max-height: min(780px, 92vh);
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
.editor-grid label span {
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
.stack-field {
  display: grid;
  gap: 7px;
}

.editor-dialog footer {
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .library-layout {
    grid-template-columns: 1fr;
  }

  .preview-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .style-library-page {
    padding: 18px;
  }

  .page-head,
  .library-card header,
  .library-card footer {
    display: grid;
  }

  .library-grid {
    grid-template-columns: 1fr;
  }
}
</style>
