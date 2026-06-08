<template>
  <div :class="['admin-shell', { 'dashboard-shell': isDashboard }]">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">
          <CoffeeCup />
        </span>
        <div>
          <h2>Beer Competition Console</h2>
        </div>
      </div>

      <nav class="nav-list" aria-label="主办方后台导航">
        <component
          :is="item.path ? RouterLink : 'span'"
          v-for="item in navItems"
          :key="item.label"
          :to="item.path"
          :class="['nav-item', { active: item.path === $route.path, disabled: !item.path }]"
          :title="item.path ? item.label : item.disabledReason"
        >
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </component>
      </nav>

      <div class="system-card">
        <div class="system-status">
          <span />
          <strong>系统运行正常</strong>
        </div>
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
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  CoffeeCup,
  DataBoard,
  Document,
  Files,
  Download,
  House,
  Medal,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import { clearSession, getDisplayName } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const displayName = getDisplayName('admin')
const isDashboard = computed(() => ['/admin/dashboard', '/admin/judges', '/admin/entries', '/admin/style-libraries'].includes(route.path) || route.path.startsWith('/admin/competitions'))

const navItems = [
  { path: '/admin/dashboard', label: '首页', icon: House },
  { path: '/admin/live-board', label: '现场看板', icon: DataBoard },
  { path: '/admin/competitions', label: '比赛管理', icon: Medal },
  { path: '/admin/judges', label: '评审列表', icon: User },
  { path: '/admin/entries', label: '酒款管理', icon: Document },
  { path: '/admin/style-libraries', label: '风格库管理', icon: Files },
  { path: null, label: '结果发布', icon: Medal, disabledReason: '请进入比赛详情发布结果' },
  { path: null, label: '数据导出', icon: Download, disabledReason: '请进入比赛详情导出数据' },
]

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
  width: 48px;
  height: 48px;
  color: #dfb94c;
  border: 1px solid rgba(216, 169, 53, 0.28);
  border-radius: 12px;
  background: rgba(216, 169, 53, 0.1);
}

.brand-mark svg {
  width: 25px;
  height: 25px;
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

.system-card {
  display: grid;
  gap: 10px;
  margin-top: auto;
  padding: 12px;
  border: 1px solid rgba(218, 231, 236, 0.06);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
}

.system-status {
  display: flex;
  gap: 10px;
  align-items: center;
}

.system-status > span {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #53c979;
}

.system-card strong {
  display: block;
  color: #b9cad0;
  font-size: 13px;
  font-weight: 600;
}

.sidebar-logout {
  justify-content: flex-start;
  width: fit-content;
  height: 26px;
  margin-left: 18px;
  padding: 0 4px;
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

  .system-card {
    display: none;
  }
}
</style>
