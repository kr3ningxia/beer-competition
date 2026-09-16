import { Node, Extension, mergeAttributes } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import { TableKit } from '@tiptap/extension-table'
import { fieldLabels } from '../../views/admin/emailTemplateFields.js'

// Keep legacy email containers and styles through visual editing and serialization.
const MailContainer = Node.create({ name: 'mailContainer', group: 'block', content: 'block+', parseHTML: () => [{ tag: 'div' }], renderHTML: ({ HTMLAttributes }) => ['div', HTMLAttributes, 0] })
const MailStyles = Extension.create({ name: 'mailStyles', addGlobalAttributes() { return [{ types: ['mailContainer', 'paragraph', 'heading', 'table', 'tableCell', 'tableHeader', 'link'], attributes: { style: { default: null, parseHTML: el => el.getAttribute('style'), renderHTML: attrs => attrs.style ? { style: attrs.style } : {} } } }] } })
const Field = Node.create({
  name: 'mailField', group: 'inline', inline: true, atom: true,
  addAttributes: () => ({ key: { default: '', parseHTML: el => el.getAttribute('data-mail-field') } }),
  parseHTML: () => [{ tag: 'span[data-mail-field]' }],
  renderHTML: ({ node, HTMLAttributes }) => ['span', mergeAttributes(HTMLAttributes, { 'data-mail-field': node.attrs.key, class: 'mail-field', contenteditable: 'false' }), fieldLabels[node.attrs.key] || node.attrs.key],
})

export const mailExtensions = [StarterKit.configure({ codeBlock: false, code: false, horizontalRule: false, strike: false, link: { openOnClick: false, isAllowedUri: (url, ctx) => ['{{portalUrl}}', '{{resultUrl}}'].includes(url) || ctx.defaultValidate(url) } }), TableKit, MailContainer, MailStyles, Field]

export function toEditorHtml(html) {
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

export function toMailHtml(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  doc.querySelectorAll('[data-mail-field]').forEach(el => el.replaceWith(doc.createTextNode(`{{${el.getAttribute('data-mail-field')}}}`)))
  return doc.body.innerHTML
}
