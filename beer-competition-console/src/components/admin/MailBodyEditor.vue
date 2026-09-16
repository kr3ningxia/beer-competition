<template>
  <div class="mail-composer">
    <div v-if="editor" class="format-bar" role="toolbar" aria-label="正文格式">
      <button type="button" title="撤销" aria-label="撤销" :disabled="!editor.can().undo()" @click="editor.chain().focus().undo().run()"><RefreshLeft /></button>
      <button type="button" title="重做" aria-label="重做" :disabled="!editor.can().redo()" @click="editor.chain().focus().redo().run()"><RefreshRight /></button>
      <span class="divider"></span>
      <select aria-label="段落格式" :value="editor.isActive('heading') ? 'heading' : 'paragraph'" @change="setParagraph($event.target.value)"><option value="paragraph">正文</option><option value="heading">标题</option></select>
      <button type="button" title="加粗" aria-label="加粗" :aria-pressed="editor.isActive('bold')" @click="editor.chain().focus().toggleBold().run()"><b>B</b></button>
      <button type="button" title="斜体" aria-label="斜体" :aria-pressed="editor.isActive('italic')" @click="editor.chain().focus().toggleItalic().run()"><i>I</i></button>
      <button type="button" title="项目列表" aria-label="项目列表" :aria-pressed="editor.isActive('bulletList')" @click="editor.chain().focus().toggleBulletList().run()"><List /></button>
      <button type="button" title="插入链接" aria-label="插入链接" :aria-pressed="editor.isActive('link')" @click="linkOpen = true"><Link /></button>
      <button type="button" title="清除格式" aria-label="清除格式" @click="editor.chain().focus().unsetAllMarks().clearNodes().run()"><Brush /></button>
      <el-dropdown trigger="click" @command="insertField">
        <button type="button" class="field-button"><Plus />插入赛事信息<ArrowDown /></button>
        <template #dropdown><el-dropdown-menu><el-dropdown-item v-for="key in variables" :key="key" :command="key">{{ fieldLabels[key] || key }}</el-dropdown-item></el-dropdown-menu></template>
      </el-dropdown>
    </div>
    <EditorContent :editor="editor" class="body-content" />
    <el-dialog v-model="linkOpen" title="插入链接" width="440px" append-to-body>
      <el-form label-position="top"><el-form-item label="链接地址"><el-input v-model="linkUrl" placeholder="https://" /></el-form-item></el-form>
      <div class="link-options"><el-button @click="linkUrl = '{{portalUrl}}'">赛事平台</el-button><el-button @click="linkUrl = '{{resultUrl}}'">比赛结果</el-button></div>
      <template #footer><el-button @click="linkOpen = false">取消</el-button><el-button type="primary" @click="setLink">插入</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import { Node, Extension, mergeAttributes } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import { TableKit } from '@tiptap/extension-table'
import { RefreshLeft, RefreshRight, List, Link, Brush, Plus, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fieldLabels, readableFields, encodedFields } from '@/views/admin/emailTemplateFields'

const props = defineProps({ modelValue: { type: String, default: '' }, variables: { type: Array, default: () => [] } })
const emit = defineEmits(['update:modelValue'])
const linkOpen = ref(false)
const linkUrl = ref('')
// Preserve existing email containers and inline styles when opening a saved template.
const MailContainer = Node.create({ name: 'mailContainer', group: 'block', content: 'block+', parseHTML: () => [{ tag: 'div' }], renderHTML: ({ HTMLAttributes }) => ['div', HTMLAttributes, 0] })
const MailStyles = Extension.create({ name: 'mailStyles', addGlobalAttributes() { return [{ types: ['mailContainer', 'paragraph', 'heading', 'table', 'tableCell', 'tableHeader', 'link'], attributes: { style: { default: null, parseHTML: el => el.getAttribute('style'), renderHTML: attrs => attrs.style ? { style: attrs.style } : {} } } }] } })
const Field = Node.create({
  name: 'mailField', group: 'inline', inline: true, atom: true,
  addAttributes: () => ({ key: { default: '', parseHTML: el => el.getAttribute('data-mail-field') } }),
  parseHTML: () => [{ tag: 'span[data-mail-field]' }],
  renderHTML: ({ node, HTMLAttributes }) => ['span', mergeAttributes(HTMLAttributes, { 'data-mail-field': node.attrs.key, class: 'mail-field', contenteditable: 'false' }), fieldLabels[node.attrs.key] || node.attrs.key],
})

function toEditorHtml(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT)
  const nodes = []
  while (walker.nextNode()) nodes.push(walker.currentNode)
  for (const node of nodes) {
    const parts = node.textContent.split(/(\{\{\s*[\w.]+\s*}})/g)
    if (parts.length === 1) continue
    const fragment = doc.createDocumentFragment()
    for (const part of parts) {
      const match = part.match(/^\{\{\s*([\w.]+)\s*}}$/)
      if (match && fieldLabels[match[1]]) {
        const span = doc.createElement('span')
        span.setAttribute('data-mail-field', match[1])
        span.textContent = fieldLabels[match[1]]
        fragment.append(span)
      } else fragment.append(doc.createTextNode(part))
    }
    node.replaceWith(fragment)
  }
  return doc.body.innerHTML
}
function toMailHtml(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  doc.querySelectorAll('[data-mail-field]').forEach(el => el.replaceWith(doc.createTextNode(`{{${el.getAttribute('data-mail-field')}}}`)))
  return doc.body.innerHTML
}
const editor = new Editor({
  extensions: [StarterKit.configure({ codeBlock: false, code: false, horizontalRule: false, strike: false, link: { openOnClick: false, isAllowedUri: (url, ctx) => ['{{portalUrl}}', '{{resultUrl}}'].includes(url) || ctx.defaultValidate(url) } }), TableKit, MailContainer, MailStyles, Field],
  content: toEditorHtml(props.modelValue),
  editorProps: { attributes: { 'aria-label': '邮件正文', role: 'textbox', 'aria-multiline': 'true' } },
  onUpdate: ({ editor: current }) => emit('update:modelValue', toMailHtml(current.getHTML())),
})
watch(() => props.modelValue, value => { if (value !== toMailHtml(editor.getHTML())) editor.commands.setContent(toEditorHtml(value), { emitUpdate: false }) })
function setParagraph(value) { value === 'heading' ? editor.chain().focus().toggleHeading({ level: 2 }).run() : editor.chain().focus().setParagraph().run() }
function insertField(key) { editor.chain().focus().insertContent({ type: 'mailField', attrs: { key } }).run() }
function setLink() {
  const url = encodedFields(linkUrl.value.trim())
  if (!/^https?:\/\//i.test(url) && !['{{portalUrl}}', '{{resultUrl}}'].includes(url)) return ElMessage.warning('请填写完整的网址，或选择赛事页面')
  if (editor.state.selection.empty) editor.chain().focus().insertContent({ type: 'text', text: readableFields(url), marks: [{ type: 'link', attrs: { href: url } }] }).run()
  else editor.chain().focus().setLink({ href: url }).run()
  linkOpen.value = false
  linkUrl.value = ''
}
onBeforeUnmount(() => editor.destroy())
</script>

<style scoped>
.mail-composer{border:1px solid #344047;border-radius:6px;overflow:clip;background:#fff;color:#26352e}
.format-bar{display:flex;align-items:center;flex-wrap:wrap;gap:4px;padding:10px 12px;background:#182125;border-bottom:1px solid #344047;position:sticky;top:0;z-index:2;color:#dce9ed}
.format-bar button,.format-bar select{display:inline-flex;align-items:center;justify-content:center;gap:6px;min-width:32px;height:32px;border:0;border-radius:4px;background:transparent;color:inherit;cursor:pointer;font-size:14px}
.format-bar select{background:#202b30;padding:0 8px}.format-bar button:hover,.format-bar button[aria-pressed=true]{background:#364039;color:#edd08a}.format-bar button:disabled{opacity:.3;cursor:not-allowed}.format-bar svg{width:16px;height:16px}.divider{height:20px;border-left:1px solid #405057;margin:0 6px}.format-bar .field-button{padding:0 10px;color:#edd08a}.body-content{padding:28px;min-height:420px}.body-content :deep(.tiptap){outline:none;min-height:380px;overflow-wrap:anywhere;line-height:1.8}.body-content :deep(table){width:100%;border-collapse:collapse;table-layout:fixed}.body-content :deep(td),.body-content :deep(th){border:1px solid #dce3df;padding:10px;vertical-align:top;position:relative}.body-content :deep(p){margin:12px 0}.body-content :deep(.mail-field){display:inline;padding:2px 6px;border-radius:3px;background:#e7f1eb;color:#285d42;border:1px solid #b8d3c2;font-size:13px;white-space:normal}.body-content :deep(.ProseMirror-selectednode){outline:2px solid #8baf97}.body-content :deep(a){color:#2f7651;text-decoration:underline}.link-options{display:flex;gap:8px}
</style>
