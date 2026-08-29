export const ADMIN_TYPES = Object.freeze({
  PLATFORM_SUPER_ADMIN: 'PLATFORM_SUPER_ADMIN',
  PLATFORM_EVENT_ADMIN: 'PLATFORM_EVENT_ADMIN',
  ORGANIZER_ADMIN: 'ORGANIZER_ADMIN',
})

export const ALL_ADMIN_TYPES = Object.freeze(Object.values(ADMIN_TYPES))

export const ADMIN_NAV_GROUPS = Object.freeze([
  {
    key: 'workspace',
    label: '概览',
    items: [
      { path: '/admin/dashboard', label: '工作台', icon: 'dashboard', adminTypes: ALL_ADMIN_TYPES },
    ],
  },
  {
    key: 'operations',
    label: '赛事运营',
    items: [
      { path: '/admin/competitions', label: '比赛管理', icon: 'competitions', adminTypes: ALL_ADMIN_TYPES, matchPrefix: true },
      { path: '/admin/entries', label: '酒款管理', icon: 'entries', adminTypes: ALL_ADMIN_TYPES },
      { path: '/admin/bank-transfers', label: '收款确认', icon: 'payments', adminTypes: ALL_ADMIN_TYPES },
      { path: '/admin/judges', label: '评审人员', icon: 'judges', adminTypes: ALL_ADMIN_TYPES },
      { path: '/admin/exports', label: '数据导出', icon: 'exports', adminTypes: ALL_ADMIN_TYPES },
    ],
  },
  {
    key: 'organization',
    label: '组织管理',
    items: [
      {
        path: '/admin/admin-users',
        label: '管理员账号',
        icon: 'accounts',
        adminTypes: [ADMIN_TYPES.PLATFORM_SUPER_ADMIN, ADMIN_TYPES.ORGANIZER_ADMIN],
      },
      {
        path: '/admin/beer-coins',
        label: '啤酒币',
        icon: 'beerCoins',
        adminTypes: [ADMIN_TYPES.PLATFORM_SUPER_ADMIN, ADMIN_TYPES.ORGANIZER_ADMIN],
      },
      { path: '/admin/operation-logs', label: '操作日志', icon: 'logs', adminTypes: ALL_ADMIN_TYPES },
    ],
  },
  {
    key: 'platform',
    label: '平台管理',
    items: [
      {
        path: '/admin/organizer-applications',
        label: '主办方入驻',
        icon: 'applications',
        adminTypes: [ADMIN_TYPES.PLATFORM_SUPER_ADMIN],
      },
      {
        path: '/admin/style-libraries',
        label: '风格库',
        icon: 'styles',
        adminTypes: ALL_ADMIN_TYPES,
      },
    ],
  },
])

export function canAccessAdminTypes(adminType, allowedTypes) {
  return !allowedTypes?.length || allowedTypes.includes(adminType)
}

export function getDefaultAdminPath() {
  return '/admin/dashboard'
}

export function getAdminTypeLabel(adminType) {
  const labels = {
    [ADMIN_TYPES.PLATFORM_SUPER_ADMIN]: '平台超级管理员',
    [ADMIN_TYPES.PLATFORM_EVENT_ADMIN]: '平台赛事管理员',
    [ADMIN_TYPES.ORGANIZER_ADMIN]: '主办方管理员',
  }
  return labels[adminType] || '后台管理员'
}
