<template>
  <div class="portal-shell">
    <header class="portal-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/portal/home" aria-label="赛事首页">
          <span class="brand-mark">
            <CoffeeCup />
          </span>
          <span>
            <strong>BREWER ENTRY</strong>
            <small>精酿啤酒大赛</small>
          </span>
        </RouterLink>

        <nav class="nav-list" aria-label="厂商导航">
          <RouterLink
            v-for="item in visibleNavItems"
            :key="item.path"
            :to="item.path"
            :class="['nav-item', { active: isNavActive(item) }]"
          >
            <component :is="item.icon" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </nav>

        <div v-if="loggedIn" class="account-pill">
          <span class="avatar">SM</span>
          <span>{{ accountName }}</span>
          <el-button text @click="logout">退出</el-button>
        </div>
        <RouterLink v-else class="login-link" to="/portal/login">登录报名</RouterLink>
      </div>
    </header>

    <main class="page-frame">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  CircleCheck,
  CoffeeCup,
  Document,
  Money,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import { clearSession, getDisplayName, isLoggedIn } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const displayName = getDisplayName('portal')
const loggedIn = computed(() => isLoggedIn('portal'))
const accountName = computed(() => displayName || '完善厂牌资料')

const navItems = [
  { path: '/portal/home', label: '赛事首页', icon: Tickets, public: true },
  { path: '/portal/events', label: '全部赛事', icon: Document, public: true, match: ['/portal/events'] },
  { path: '/portal/my', label: '我的参赛', icon: CircleCheck, auth: true },
  { path: '/portal/entries', label: '我的酒款', icon: Document, auth: true },
  { path: '/portal/payment', label: '付款与标签', icon: Money, auth: true },
  { path: '/portal/results', label: '结果反馈', icon: CircleCheck, auth: true },
  { path: '/portal/profile', label: '厂牌资料', icon: User, auth: true },
]

const visibleNavItems = computed(() => navItems.filter((item) => item.public || loggedIn.value))

function isNavActive(item) {
  if (item.match) {
    return item.match.some((path) => route.path === path || route.path.startsWith(`${path}/`))
  }
  return route.path === item.path
}

function logout() {
  clearSession('portal')
  router.push('/portal/home')
}
</script>

<style scoped>
.portal-shell {
  --ink: #211912;
  --muted: #746a5f;
  --paper: #fffaf0;
  --foam: #fff6df;
  --amber: #d89021;
  --green: #3d7d50;
  --charcoal: #181512;
  --line: rgba(87, 58, 26, 0.12);
  --el-color-primary: #b87517;
  --el-color-primary-light-3: #d99d3d;
  --el-color-primary-light-5: #e8bc6b;
  --el-color-primary-light-7: #f1d394;
  --el-color-primary-light-9: #fff0c2;
  --el-color-primary-dark-2: #8b5c19;
  min-height: 100vh;
  color: var(--ink);
  background:
    linear-gradient(90deg, rgba(80, 51, 22, 0.052) 1px, transparent 1px),
    linear-gradient(180deg, #fff8e7 0%, #f8edd4 48%, #f3e1bd 100%);
  background-size: 24px 24px, auto;
}

.portal-header {
  position: sticky;
  top: 0;
  z-index: 8;
  background: rgba(255, 250, 240, 0.92);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(18px);
}

.header-inner {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 22px;
  align-items: center;
  max-width: 1280px;
  min-height: 72px;
  margin: 0 auto;
  padding: 10px 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  color: #2b1d10;
  background: #e1a23d;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 8px;
  box-shadow: inset 0 -7px 0 rgba(74, 45, 16, 0.16);
}

.brand-mark svg,
.nav-item svg {
  width: 20px;
  height: 20px;
}

.brand strong {
  display: block;
  letter-spacing: 0.06em;
  font-size: 13px;
}

.brand small {
  display: block;
  margin-top: 3px;
  color: #746a5f;
  font-size: 12px;
}

.nav-list {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.nav-item {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 0 12px;
  color: #5f5549;
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 8px;
  white-space: nowrap;
  transition: background 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}

.nav-item:hover {
  color: #2b1d10;
  background: rgba(255, 247, 230, 0.86);
  border-color: rgba(87, 58, 26, 0.1);
}

.nav-item.active {
  color: #2b1d10;
  background: #fff4d7;
  border-color: rgba(184, 117, 23, 0.28);
}

.account-pill {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 42px;
  padding: 5px 6px 5px 5px;
  color: #33251a;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(83, 51, 17, 0.08);
  white-space: nowrap;
}

.login-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  color: #2b1d10;
  background: #e1a23d;
  border: 1px solid rgba(87, 58, 26, 0.16);
  border-radius: 999px;
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: #fff6df;
  background: #3d7d50;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
}

.page-frame {
  max-width: 1280px;
  min-width: 0;
  margin: 0 auto;
  padding: 28px 28px 48px;
}

:deep(.portal-section-title) {
  margin: 0 0 16px;
  font-size: 21px;
  line-height: 1.2;
}

:deep(.brewer-card) {
  background: rgba(255, 250, 240, 0.9);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(67, 43, 17, 0.08);
}

:deep(.label-chip) {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

:deep(.tone-amber) {
  color: #8f5100;
  background: #ffe3a6;
}

:deep(.tone-green) {
  color: #1f5a34;
  background: #cfead2;
}

:deep(.tone-blue) {
  color: #1f506e;
  background: #d4e8f0;
}

:deep(.tone-gold) {
  color: #6b4710;
  background: #f3d978;
}

:deep(.tone-muted) {
  color: #71695d;
  background: #e5ded1;
}

@media (max-width: 1120px) {
  .header-inner {
    grid-template-columns: auto auto;
  }

  .nav-list {
    grid-column: 1 / -1;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .account-pill {
    justify-self: end;
  }
}

@media (max-width: 720px) {
  .header-inner {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    padding: 10px 16px;
  }

  .brand {
    flex: 1 1 auto;
  }

  .account-pill {
    max-width: 100%;
  }

  .account-pill > span:nth-child(2) {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .nav-list {
    order: 3;
    width: 100%;
  }

  .page-frame {
    padding: 18px 16px 34px;
  }
}
</style>
