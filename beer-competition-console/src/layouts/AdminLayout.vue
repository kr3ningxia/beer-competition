<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark"><img src="/brand-icon.png" alt="" aria-hidden="true"></span>
        <div class="brand-copy">
          <strong>赛事运营后台</strong>
          <small>{{ organizationTitle }}</small>
        </div>
      </div>

      <nav v-if="!credentialSetupRequired" class="nav-list" aria-label="后台导航">
        <section v-for="group in visibleNavGroups" :key="group.key" class="nav-group">
          <p>{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            :class="['nav-item', { active: isNavActive(item) }]"
          >
            <component :is="iconComponents[item.icon]" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </section>
      </nav>

      <div v-else class="setup-state">
        <Key />
        <span>完成账号设置后开放工作台</span>
      </div>

      <div class="sidebar-actions">
        <button class="identity-button" type="button" title="账号设置" @click="openAccountSettings">
          <span class="identity-copy">
            <strong>{{ displayName || '管理员' }}</strong>
            <small>{{ adminTypeLabel }}</small>
          </span>
          <Setting />
        </button>
        <button class="logout-button" type="button" title="退出登录" aria-label="退出登录" @click="logout">
          <SwitchButton />
        </button>
      </div>
    </aside>

    <section class="content">
      <main class="page"><router-view /></main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  DataBoard, Document, DocumentChecked, Download, Files, Key, Medal, Money,
  OfficeBuilding, Setting, SwitchButton, User, UserFilled,
} from '@element-plus/icons-vue'
import { getAdminMe } from '@/api/auth'
import { ADMIN_NAV_GROUPS, canAccessAdminTypes, getAdminTypeLabel } from '@/config/adminAccess'
import {
  clearSession, getAdminOrganizerName, getAdminType, getDisplayName,
  isAdminCredentialSetupRequired, setSession,
} from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const iconComponents = {
  dashboard: DataBoard,
  competitions: Medal,
  entries: Document,
  payments: Money,
  judges: User,
  exports: Download,
  accounts: UserFilled,
  beerCoins: Money,
  logs: DocumentChecked,
  applications: OfficeBuilding,
  styles: Files,
}

const adminType = computed(() => getAdminType())
const displayName = computed(() => getDisplayName('admin'))
const organizerName = computed(() => getAdminOrganizerName())
const credentialSetupRequired = computed(() => isAdminCredentialSetupRequired())
const adminTypeLabel = computed(() => getAdminTypeLabel(adminType.value))
const organizationTitle = computed(() => organizerName.value || '啤酒事务局')
const visibleNavGroups = computed(() => ADMIN_NAV_GROUPS
  .map((group) => ({
    ...group,
    items: group.items.filter((item) => canAccessAdminTypes(adminType.value, item.adminTypes)),
  }))
  .filter((group) => group.items.length))

onMounted(async () => {
  try {
    setSession('admin', await getAdminMe())
  } catch {
    // 请求拦截器统一处理登录失效。
  }
})

function isNavActive(item) {
  return item.matchPrefix ? route.path.startsWith(item.path) : route.path === item.path
}

function openAccountSettings() {
  if (!credentialSetupRequired.value) router.push('/admin/account')
}

function logout() {
  clearSession('admin')
  router.replace('/admin/login')
}
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background: #0b1115;
}

.sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 22px 14px 16px;
  color: #dce9ed;
  border-right: 1px solid rgba(218, 231, 236, 0.08);
  background: linear-gradient(rgba(255, 255, 255, 0.025) 1px, transparent 1px), linear-gradient(180deg, #0d151a 0%, #091014 100%);
  background-size: 100% 48px, auto;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
  padding: 0 4px;
}

.brand-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  overflow: hidden;
  width: 46px;
  height: 46px;
  border-radius: 8px;
}

.brand-mark img { width: 100%; height: 100%; object-fit: cover; }
.brand-copy { display: grid; gap: 4px; min-width: 0; }
.brand-copy strong, .brand-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.brand-copy strong { color: #f0f5f6; font-size: 15px; }
.brand-copy small { color: #82969e; font-size: 12px; }

.nav-list {
  flex: 1;
  min-height: 0;
  margin-top: 24px;
  padding-right: 3px;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.nav-list::-webkit-scrollbar { width: 6px; }
.nav-list::-webkit-scrollbar-thumb { border-radius: 999px; background: rgba(216, 169, 53, 0.2); }
.nav-group + .nav-group { margin-top: 18px; }
.nav-group > p { margin: 0 0 6px; padding: 0 12px; color: #536972; font-size: 11px; font-weight: 800; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 42px;
  padding: 0 12px;
  color: #9bafb7;
  text-decoration: none;
  border: 1px solid transparent;
  border-radius: 8px;
  transition: color 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.nav-item svg { flex: 0 0 auto; width: 18px; height: 18px; }
.nav-item:hover { color: #eef5f7; background: rgba(255, 255, 255, 0.035); }
.nav-item.active { color: #fff; border-color: rgba(216, 169, 53, 0.2); background: rgba(216, 169, 53, 0.08); }
.nav-item.active svg { color: #d8a935; }

.setup-state {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 26px;
  padding: 13px 12px;
  color: #9dafb6;
  border: 1px solid rgba(216, 169, 53, 0.16);
  border-radius: 8px;
  background: rgba(216, 169, 53, 0.05);
  font-size: 12px;
  line-height: 1.45;
}

.setup-state svg { flex: 0 0 auto; width: 18px; color: #d8a935; }
.sidebar-actions { display: grid; grid-template-columns: minmax(0, 1fr) 40px; gap: 8px; margin-top: auto; padding-top: 14px; border-top: 1px solid rgba(218, 231, 236, 0.07); }
.identity-button, .logout-button { min-height: 44px; color: #a8bac2; border: 1px solid transparent; border-radius: 8px; background: transparent; }

.identity-button {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 16px;
  align-items: center;
  gap: 8px;
  padding: 5px 9px;
  text-align: left;
}

.identity-button:hover, .logout-button:hover { color: #eef5f7; border-color: rgba(218, 231, 236, 0.08); background: rgba(255, 255, 255, 0.035); }
.identity-copy { display: grid; gap: 2px; min-width: 0; }
.identity-copy strong, .identity-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.identity-copy strong { color: #dce9ed; font-size: 12px; }
.identity-copy small { color: #60757e; font-size: 10px; }
.identity-button > svg, .logout-button svg { width: 16px; height: 16px; }
.logout-button { display: grid; place-items: center; padding: 0; }
.content, .page { min-width: 0; min-height: 0; height: 100vh; overflow: hidden; }

@media (max-width: 980px) {
  .admin-shell { grid-template-columns: 1fr; height: auto; overflow: visible; }
  .sidebar { min-height: auto; }
  .nav-list { overflow: visible; }
  .content, .page { height: auto; overflow: visible; }
}
</style>
