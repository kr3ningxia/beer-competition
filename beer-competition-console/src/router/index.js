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
      { path: 'my', component: () => import('@/views/portal/MyParticipation.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'entries', component: () => import('@/views/portal/Entries.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'submit', component: () => import('@/views/portal/SubmitEntry.vue'), meta: { requiresAuth: true, scope: 'portal' } },
      { path: 'payment', component: () => import('@/views/portal/PaymentQr.vue'), meta: { requiresAuth: true, scope: 'portal' } },
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
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, scope: 'admin' },
    children: [
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'competitions', component: () => import('@/views/admin/Competitions.vue') },
      { path: 'competitions/new', component: () => import('@/views/admin/CompetitionCreate.vue') },
      { path: 'competitions/:id', component: () => import('@/views/admin/CompetitionDetail.vue') },
      { path: 'judges', component: () => import('@/views/admin/Judges.vue') },
      { path: 'style-libraries', component: () => import('@/views/admin/StyleLibraries.vue') },
      { path: 'assignments', redirect: '/admin/competitions' },
      { path: 'score-config', redirect: '/admin/competitions' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
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
      next(scope === 'admin' ? '/admin/login' : '/portal/login')
      return
    }
  }
  next()
})

export default router
