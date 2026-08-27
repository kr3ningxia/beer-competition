<template>
  <div :class="['admin-shell', { 'dashboard-shell': isDashboard }]">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">
          <img src="/brand-icon.png" alt="" aria-hidden="true">
        </span>
        <div>
          <h2>啤酒事务局赛事后台</h2>
        </div>
      </div>

      <nav class="nav-list" aria-label="组委会后台导航">
        <component
          :is="item.path ? RouterLink : 'span'"
          v-for="item in visibleNavItems"
          :key="item.label"
          :to="item.path"
          :class="['nav-item', { active: item.path === $route.path, disabled: !item.path }]"
          :title="item.path ? item.label : item.disabledReason"
        >
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </component>
      </nav>

      <div class="sidebar-actions">
        <el-button class="sidebar-logout" text :icon="SwitchButton" @click="logout">退出登录</el-button>
      </div>
    </aside>

    <section class="content">
      <header v-if="!isDashboard" class="header">
        <strong>{{ displayName || '管理员' }}</strong>
        <el-button text @click="logout">退出</el-button>
      </header>
      <main :class="['page', { 'dashboard-page': isDashboard }]">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  DataBoard,
  DocumentChecked,
  Document,
  Files,
  Download,
  Medal,
  OfficeBuilding,
  SwitchButton,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import { getAdminMe } from '@/api/auth'
import { clearSession, getAdminType, getDisplayName, isAdminCredentialSetupRequired, setSession } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const displayName = getDisplayName('admin')
const adminType = ref(getAdminType())
const credentialSetupRequired = ref(isAdminCredentialSetupRequired())
const isDashboard = computed(() => ['/admin/dashboard', '/admin/judges', '/admin/admin-users', '/admin/operation-logs', '/admin/entries', '/admin/bank-transfers', '/admin/style-libraries', '/admin/exports', '/admin/organizer-applications'].includes(route.path) || route.path.startsWith('/admin/competitions'))

const navItems = [
  { path: '/admin/competitions', label: '比赛管理', icon: Medal },
  { path: '/admin/live-board', label: '现场看板', icon: DataBoard },
  { path: '/admin/judges', label: '评审列表', icon: User },
  { path: '/admin/entries', label: '酒款管理', icon: Document },
  { path: '/admin/bank-transfers', label: '转账确认', icon: Document },
  { path: '/admin/style-libraries', label: '风格库管理', icon: Files },
  { path: '/admin/organizer-applications', label: '主办方入驻', icon: OfficeBuilding, platformOnly: true },
  { path: '/admin/admin-users', label: '管理员账号', icon: UserFilled },
  { path: '/admin/operation-logs', label: '操作日志', icon: DocumentChecked },
  { path: '/admin/exports', label: '数据导出', icon: Download },
]

const visibleNavItems = computed(() => {
  if (credentialSetupRequired.value) {
    return navItems.filter((item) => item.path === '/admin/admin-users')
  }
  return navItems.filter((item) => !item.platformOnly || adminType.value === 'PLATFORM_SUPER_ADMIN')
})

onMounted(async () => {
  try {
    const currentUser = await getAdminMe()
    if (currentUser?.adminType) {
      adminType.value = currentUser.adminType
      setSession('admin', currentUser)
    }
    credentialSetupRequired.value = Boolean(currentUser?.mustChangePassword || currentUser?.mustChangeUsername)
  } catch {
    // The request interceptor handles expired sessions and redirects.
  }
})

function logout() {
  clearSession('admin')
  router.replace('/admin/login')
}
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 250px 1fr;
  min-height: 100vh;
  background: #f5f7fb;
}

.dashboard-shell {
  grid-template-columns: 250px minmax(0, 1fr);
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: #0b1115;
}

.sidebar {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 24px 16px 18px;
  overflow-y: auto;
  overscroll-behavior: contain;
  color: #dce9ed;
  background:
    radial-gradient(circle at 86% 4%, rgba(216, 169, 53, 0.09), transparent 5.5rem),
    linear-gradient(180deg, #0d151a 0%, #0a1014 100%);
  border-right: 1px solid rgba(218, 231, 236, 0.08);
}

.sidebar::-webkit-scrollbar {
  width: 8px;
}

.sidebar::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.sidebar::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(216, 169, 53, 0.22);
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
}

.brand-mark {
  display: grid;
  place-items: center;
  overflow: hidden;
  width: 48px;
  height: 48px;
  color: #dfb94c;
  border-radius: 12px;
}

.brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.brand h2 {
  margin: 0;
}

.brand h2 {
  font-size: 16px;
  line-height: 1.25;
}

.nav-list {
  display: grid;
  gap: 8px;
  margin-top: 28px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 46px;
  padding: 0 12px;
  color: #a8bac2;
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 10px;
  transition: color 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.nav-item svg {
  width: 20px;
  height: 20px;
}

.nav-item:hover {
  color: #eef6f8;
  background: rgba(255, 255, 255, 0.035);
}

.nav-item.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(216, 169, 53, 0.18);
}

.nav-item.active svg {
  color: #d8a935;
}

.nav-item.disabled {
  cursor: default;
  opacity: 0.82;
}

.sidebar-actions {
  margin-top: auto;
  padding-top: 16px;
}

.sidebar-logout {
  justify-content: flex-start;
  width: 100%;
  height: 40px;
  margin: 0;
  padding: 0 12px;
  color: #657982;
  font-size: 12px;
  font-weight: 600;
}

.sidebar-logout:hover,
.sidebar-logout:focus {
  color: #a8bac2;
  background: transparent;
}

.sidebar-logout :deep(.el-icon) {
  margin-right: 4px;
}

.content {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.dashboard-shell .content {
  height: 100vh;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.page {
  flex: 1;
  padding: 24px;
}

.dashboard-page {
  min-height: 0;
  padding: 0;
  overflow: hidden;
  background: #0e1418;
}

@media (max-width: 980px) {
  .admin-shell,
  .dashboard-shell {
    grid-template-columns: 1fr;
  }

  .dashboard-shell {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .sidebar {
    height: auto;
    min-height: auto;
  }

  .dashboard-shell .content {
    height: auto;
    overflow: visible;
  }

  .dashboard-page {
    overflow: visible;
  }

}
</style>
