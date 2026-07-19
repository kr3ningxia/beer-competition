import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

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
    meta: { public: true, scope: 'admin' },
  },
  {
    path: '/admin/live-board',
    component: () => import('@/views/admin/LiveBoard.vue'),
    meta: { requiresAuth: true, scope: 'admin' },
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, scope: 'admin' },
    children: [
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'competitions', component: () => import('@/views/admin/Competitions.vue') },
      { path: 'competitions/new', component: () => import('@/views/admin/CompetitionCreate.vue') },
      { path: 'competitions/:id', component: () => import('@/views/admin/CompetitionDetail.vue') },
      { path: 'entries', component: () => import('@/views/admin/AdminEntries.vue') },
      { path: 'bank-transfers', component: () => import('@/views/admin/AdminBankTransfers.vue') },
      { path: 'judges', component: () => import('@/views/admin/Judges.vue') },
      { path: 'admin-users', component: () => import('@/views/admin/AdminUsers.vue') },
      { path: 'operation-logs', component: () => import('@/views/admin/AdminOperationLogs.vue') },
      { path: 'style-libraries', component: () => import('@/views/admin/StyleLibraries.vue') },
      { path: 'exports', component: () => import('@/views/admin/AdminExports.vue') },
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
      next(to.meta.scope === 'admin' ? '/admin/dashboard' : '/portal/my')
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
  }
  next()
})

router.afterEach((to) => {
  document.title = to.path.startsWith('/admin')
    ? '赛事后台｜啤酒事务局'
    : '参赛平台｜啤酒事务局'
})

export default router
