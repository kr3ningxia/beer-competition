import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeStyleLibrary } from './styleLibraries.js'

test('legacy style library data defaults to public read-only metadata', () => {
  const library = normalizeStyleLibrary({ code: 'BJCP', name: 'BJCP' })
  assert.equal(library.visibility, 'PUBLIC')
  assert.equal(library.visibilityLabel, '公共')
  assert.equal(library.canEdit, false)
})

test('scoped private library metadata is preserved', () => {
  const library = normalizeStyleLibrary({
    code: 'ORG_64_CUSTOM',
    name: '我的风格库',
    visibility: 'PRIVATE',
    canEdit: true,
    canPublish: false,
  })
  assert.equal(library.visibility, 'PRIVATE')
  assert.equal(library.visibilityLabel, '内部')
  assert.equal(library.canEdit, true)
  assert.equal(library.canPublish, false)
})
