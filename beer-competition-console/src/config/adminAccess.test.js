import test from 'node:test'
import assert from 'node:assert/strict'
import { ADMIN_NAV_GROUPS, ADMIN_TYPES, canAccessAdminTypes } from './adminAccess.js'

function visiblePaths(adminType) {
  return ADMIN_NAV_GROUPS.flatMap((group) => group.items)
    .filter((item) => canAccessAdminTypes(adminType, item.adminTypes))
    .map((item) => item.path)
}

test('organizer admin sees own operations and read-only style library', () => {
  const paths = visiblePaths(ADMIN_TYPES.ORGANIZER_ADMIN)
  assert.ok(paths.includes('/admin/competitions'))
  assert.ok(paths.includes('/admin/admin-users'))
  assert.ok(!paths.includes('/admin/organizer-applications'))
  assert.ok(paths.includes('/admin/style-libraries'))
})

test('platform event admin does not receive account or tenant administration', () => {
  const paths = visiblePaths(ADMIN_TYPES.PLATFORM_EVENT_ADMIN)
  assert.ok(paths.includes('/admin/competitions'))
  assert.ok(paths.includes('/admin/operation-logs'))
  assert.ok(!paths.includes('/admin/admin-users'))
  assert.ok(!paths.includes('/admin/organizer-applications'))
})

test('platform event admin can browse style library without tenant maintenance', () => {
  const paths = visiblePaths(ADMIN_TYPES.PLATFORM_EVENT_ADMIN)
  assert.ok(paths.includes('/admin/style-libraries'))
  assert.ok(!paths.includes('/admin/organizer-applications'))
})

test('only platform super admin receives platform maintenance menu', () => {
  const paths = visiblePaths(ADMIN_TYPES.PLATFORM_SUPER_ADMIN)
  assert.ok(paths.includes('/admin/style-libraries'))
  assert.ok(paths.includes('/admin/organizer-applications'))
})

test('sub admin has its own identity without account management', () => {
  const paths = visiblePaths(ADMIN_TYPES.ORGANIZER_SUB_ADMIN)
  assert.ok(paths.includes('/admin/dashboard'))
  assert.ok(paths.includes('/admin/competitions'))
  assert.ok(!paths.includes('/admin/admin-users'))
  assert.ok(!paths.includes('/admin/organizer-applications'))
})
