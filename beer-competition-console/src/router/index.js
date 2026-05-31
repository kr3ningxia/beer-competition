import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

const routes = [
  { path: '/', redirect: '/portal/home' },
  {
    path: '/portal/login',
    component: () => import('@/views/portal/Login.vue'),
    meta: { public: true, scope: 'portal' },
  },
  {
    path: '/portal',
    component: () => import('@/layouts/PortalLayout.vue'),
    meta: { requiresAuth: true, scope: 'portal' },
    children: [
      { path: 'home', component: () => import('@/views/portal/Home.vue') },
      { path: 'events', component: () => import('@/views/portal/Events.vue') },
      { path: 'events/:id', component: () => import('@/views/portal/EventDetail.vue') },
      { path: 'entries', component: () => import('@/views/portal/Entries.vue') },
      { path: 'submit', component: () => import('@/views/portal/SubmitEntry.vue') },
      { path: 'payment', component: () => import('@/views/portal/PaymentQr.vue') },
      { path: 'results', component: () => import('@/views/portal/Results.vue') },
      { path: 'profile', component: () => import('@/views/portal/Profile.vue') },
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
      { path: 'assignments', component: () => import('@/views/admin/Assignments.vue') },
      { path: 'score-config', component: () => import('@/views/admin/ScoreConfig.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    if (to.meta.scope && isLoggedIn(to.meta.scope)) {
      next(to.meta.scope === 'admin' ? '/admin/dashboard' : '/portal/home')
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
