import { createRouter, createWebHistory } from 'vue-router'
import { getAdminType, isAdminCredentialSetupRequired, isLoggedIn } from '@/utils/auth'
import { ADMIN_TYPES, ALL_ADMIN_TYPES, canAccessAdminTypes, getDefaultAdminPath } from '@/config/adminAccess'

const routes = [
  { path: '/', redirect: '/portal/home' },
  {
    path: '/portal/login',
    component: () => import('@/views/portal/Login.vue'),
    meta: { public: true, guestOnly: true, scope: 'portal' },
  },
  {
    path: '/portal',
    component: () => import('@/layouts/PortalLayout.vue'),
    redirect: '/portal/home',
    meta: { scope: 'portal' },
    children: [
      { path: 'home', component: () => import('@/views/portal/Home.vue'), meta: { public: true, scope: 'portal' } },
      { path: 'organizer-application', component: () => import('@/views/portal/OrganizerApplication.vue'), meta: { public: true, scope: 'portal' } },
      { path: 'organizer-application/status', component: () => import('@/views/portal/OrganizerApplication.vue'), meta: { public: true, scope: 'portal' } },
      { path: 'events', component: () => import('@/views/portal/Events.vue'), meta: { public: true, scope: 'portal' } },
      { path: 'events/:id', component: () => import('@/views/portal/EventDetail.vue'), meta: { public: true, scope: 'portal' } },
      { path: 'competition-results', component: () => import('@/views/portal/CompetitionResults.vue'), meta: { public: true, scope: 'portal' } },
      { path: 'competition-results/:id', component: () => import('@/views/portal/CompetitionResultDetail.vue'), meta: { public: true, scope: 'portal' } },
      { path: 'my', component: () => import('@/views/portal/MyParticipation.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'entries', component: () => import('@/views/portal/Entries.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'submit', component: () => import('@/views/portal/SubmitEntry.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'payment', component: () => import('@/views/portal/PaymentQr.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'fulfillment', component: () => import('@/views/portal/PaymentQr.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'batch-payment', component: () => import('@/views/portal/BatchPayment.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'results', component: () => import('@/views/portal/Results.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'profile', component: () => import('@/views/portal/Profile.vue'), meta: { requiresAuth: true, scope: 'portal' } },
    ],
  },
  {
    path: '/admin/login',
    component: () => import('@/views/admin/Login.vue'),
    meta: { public: true, guestOnly: true, scope: 'admin' },
  },
  {
    path: '/admin/live-board',
    component: () => import('@/views/admin/LiveBoard.vue'),
    meta: { requiresAuth: true, scope: 'admin', adminTypes: ALL_ADMIN_TYPES },
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, scope: 'admin' },
    children: [
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'account-setup', component: () => import('@/views/admin/AdminUsers.vue'), meta: { adminTypes: ALL_ADMIN_TYPES, credentialSetup: true } },
      { path: 'account', component: () => import('@/views/admin/AdminUsers.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'competitions', component: () => import('@/views/admin/Competitions.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'competitions/new', component: () => import('@/views/admin/CompetitionCreate.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'competitions/:id', component: () => import('@/views/admin/CompetitionDetail.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'entries', component: () => import('@/views/admin/AdminEntries.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'bank-transfers', component: () => import('@/views/admin/AdminBankTransfers.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'judges', component: () => import('@/views/admin/Judges.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'judge-recruitments', component: () => import('@/views/admin/JudgeRecruitments.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'judge-recruitments/:id', component: () => import('@/views/admin/JudgeRecruitmentDetail.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'organizer-applications', component: () => import('@/views/admin/OrganizerApplications.vue'), meta: { adminTypes: [ADMIN_TYPES.PLATFORM_SUPER_ADMIN] } },
      { path: 'admin-users', component: () => import('@/views/admin/AdminUsers.vue'), meta: { adminTypes: [ADMIN_TYPES.PLATFORM_SUPER_ADMIN, ADMIN_TYPES.ORGANIZER_ADMIN] } },
      { path: 'beer-coins', component: () => import('@/views/admin/BeerCoins.vue'), meta: { adminTypes: [ADMIN_TYPES.PLATFORM_SUPER_ADMIN, ADMIN_TYPES.ORGANIZER_ADMIN] } },
      { path: 'operation-logs', component: () => import('@/views/admin/AdminOperationLogs.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'style-libraries', component: () => import('@/views/admin/StyleLibraries.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'exports', component: () => import('@/views/admin/AdminExports.vue'), meta: { adminTypes: ALL_ADMIN_TYPES } },
      { path: 'assignments', redirect: '/admin/competitions' },
      { path: 'score-config', redirect: '/admin/competitions' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    if (to.meta.guestOnly && to.meta.scope && isLoggedIn(to.meta.scope)) {
      next(to.meta.scope === 'admin'
        ? (isAdminCredentialSetupRequired() ? '/admin/account-setup' : getDefaultAdminPath())
        : '/portal/my')
      return
    }
    next()
    return
  }

  if (to.meta.requiresAuth) {
    const scope = to.meta.scope
    if (!isLoggedIn(scope)) {
      next(scope === 'admin' ? '/admin/login' : { path: '/portal/login', query: { redirect: to.fullPath } })
      return
    }
    if (scope === 'admin' && isAdminCredentialSetupRequired() && !to.meta.credentialSetup) {
      next('/admin/account-setup')
      return
    }
    if (scope === 'admin' && !isAdminCredentialSetupRequired() && to.meta.credentialSetup) {
      next(getDefaultAdminPath())
      return
    }
    if (scope === 'admin' && getAdminType()
      && !canAccessAdminTypes(getAdminType(), to.meta.adminTypes)) {
      next(getDefaultAdminPath())
      return
    }
  }
  next()
})

router.afterEach((to) => {
  if (to.path.includes('organizer-application')) {
    document.title = '主办方入驻｜啤酒事务局'
    return
  }
  document.title = to.path.startsWith('/admin')
    ? '赛事后台｜啤酒事务局'
    : '赛事平台｜啤酒事务局'
})

export default router
