import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

const routes = [
  { path: '/', redirect: '/competitions' },
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('@/views/Register.vue'), meta: { public: true } },
  { path: '/review-status', component: () => import('@/views/ReviewStatus.vue'), meta: { requiresAuth: true } },
  { path: '/profile/edit', component: () => import('@/views/ProfileEdit.vue'), meta: { requiresAuth: true } },
  { path: '/competitions', component: () => import('@/views/Competitions.vue'), meta: { requiresAuth: true } },
  { path: '/judged', component: () => import('@/views/Judged.vue'), meta: { requiresAuth: true } },
  { path: '/scan-result/:uuid', component: () => import('@/views/ScanResult.vue'), meta: { requiresAuth: true } },
  { path: '/score/:uuid', component: () => import('@/views/Score.vue'), meta: { requiresAuth: true } },
  { path: '/ranking/:roundTableId', component: () => import('@/views/Ranking.vue'), meta: { requiresAuth: true } },
  { path: '/captain', component: () => import('@/views/Captain.vue'), meta: { requiresAuth: true } },
  { path: '/captain/:uuid', component: () => import('@/views/Captain.vue'), meta: { requiresAuth: true } },
  { path: '/profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    if (isLoggedIn()) {
      next('/competitions')
      return
    }
    next()
    return
  }

  if (to.meta.requiresAuth && !isLoggedIn()) {
    next('/login')
    return
  }

  next()
})

export default router
