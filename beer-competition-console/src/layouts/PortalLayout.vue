<template>
  <div class="portal-shell">
    <header class="portal-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/portal/home" aria-label="赛事首页">
          <span class="brand-mark">
            <img src="/brand-icon.png" alt="" aria-hidden="true">
          </span>
          <span>
            <strong>啤酒事务局</strong>
            <small>参赛平台</small>
          </span>
        </RouterLink>

        <nav class="nav-list" aria-label="厂牌导航">
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

        <div v-if="loggedIn" class="account-actions">
          <RouterLink
            :class="['account-pill', { active: route.path === '/portal/profile' }]"
            to="/portal/profile"
            aria-label="进入厂牌资料"
          >
            <span class="avatar">
              <img v-if="accountAvatarPreviewUrl" :src="accountAvatarPreviewUrl" alt="厂牌头像">
              <span v-else>{{ accountInitial }}</span>
            </span>
            <span>{{ accountName }}</span>
          </RouterLink>
          <el-button class="logout-button" text @click="logout">退出</el-button>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  CircleCheck,
  Document,
  Medal,
  Trophy,
  Tickets,
} from '@element-plus/icons-vue'
import { getPortalMe } from '@/api/auth'
import { fetchPortalProfile } from '@/api/portal'
import { BASE_URL } from '@/config'
import { clearSession, getDisplayName, isLoggedIn, setDisplayName } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const displayName = ref(getDisplayName('portal'))
const accountAvatarUrl = ref('')
const loggedIn = computed(() => isLoggedIn('portal'))
const accountName = computed(() => isQuestionPlaceholder(displayName.value) ? '完善厂牌资料' : displayName.value || '完善厂牌资料')
const accountInitial = computed(() => getAccountInitial(accountName.value))
const accountAvatarPreviewUrl = computed(() => resolveAvatarUrl(accountAvatarUrl.value))

const navItems = [
  { path: '/portal/home', label: '赛事首页', icon: Tickets, public: true },
  { path: '/portal/events', label: '全部赛事', icon: Document, public: true, match: ['/portal/events'] },
  { path: '/portal/competition-results', label: '赛事结果', icon: Trophy, public: true, match: ['/portal/competition-results'] },
  { path: '/portal/my', label: '我的参赛', icon: CircleCheck, auth: true },
  { path: '/portal/results', label: '我的结果', icon: Medal, auth: true },
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
  router.replace('/portal/home')
}

function syncDisplayNameFromStorage() {
  displayName.value = getDisplayName('portal')
}

async function refreshPortalProfile() {
  if (!loggedIn.value) {
    accountAvatarUrl.value = ''
    return
  }
  try {
    const profile = await fetchPortalProfile()
    accountAvatarUrl.value = profile?.avatarUrl || ''
    if (profile?.displayName && profile.displayName !== getDisplayName('portal')) {
      setDisplayName('portal', profile.displayName)
    }
  } catch {
    accountAvatarUrl.value = ''
  }
}

async function refreshCurrentUser() {
  if (!loggedIn.value) {
    accountAvatarUrl.value = ''
    return
  }
  try {
    const me = await getPortalMe()
    if (me?.displayName) {
      setDisplayName('portal', me.displayName)
    }
  } catch {
    // Request interceptor handles expired sessions.
  }
}

function handleSessionUpdate(event) {
  if (!event.detail?.scope || event.detail.scope === 'portal') {
    syncDisplayNameFromStorage()
    refreshPortalProfile()
  }
}

function handleStorageUpdate(event) {
  if (event.key === 'portal_display_name' || event.key === 'portal_token') {
    syncDisplayNameFromStorage()
    refreshPortalProfile()
  }
}

function isQuestionPlaceholder(value) {
  return /^\?{2,}$/.test((value || '').trim())
}

function getAccountInitial(value) {
  const normalized = (value || '').trim()
  if (!normalized || isQuestionPlaceholder(normalized) || normalized === '完善厂牌资料') {
    return '厂'
  }
  const ascii = normalized.match(/[A-Za-z0-9]/)
  return (ascii ? ascii[0] : Array.from(normalized)[0]).toUpperCase()
}

function resolveAvatarUrl(value) {
  if (!value) return ''
  if (/^https?:\/\//i.test(value) || value.startsWith('data:')) {
    return value
  }
  if (value.startsWith('/')) {
    return `${BASE_URL}${value}`
  }
  return value
}

onMounted(() => {
  window.addEventListener('beer-competition-session-updated', handleSessionUpdate)
  window.addEventListener('storage', handleStorageUpdate)
  refreshCurrentUser()
  refreshPortalProfile()
})

onUnmounted(() => {
  window.removeEventListener('beer-competition-session-updated', handleSessionUpdate)
  window.removeEventListener('storage', handleStorageUpdate)
})
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
  overflow: hidden;
  width: 44px;
  height: 44px;
  color: #2b1d10;
  border-radius: 8px;
}

.nav-item svg {
  width: 20px;
  height: 20px;
}

.brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.account-actions {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 42px;
  padding: 5px 6px 5px 5px;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(83, 51, 17, 0.08);
  white-space: nowrap;
}

.account-pill {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 32px;
  padding: 0 10px 0 0;
  color: #33251a;
  text-decoration: none;
  border-radius: 999px;
  transition: background 0.16s ease, color 0.16s ease;
}

.account-pill:hover,
.account-pill.active {
  color: #2b1d10;
  background: #fff4d7;
}

.logout-button {
  flex: 0 0 auto;
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
  overflow: hidden;
  width: 32px;
  height: 32px;
  color: #fff6df;
  background: #3d7d50;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

  .account-actions {
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

  .account-actions {
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
