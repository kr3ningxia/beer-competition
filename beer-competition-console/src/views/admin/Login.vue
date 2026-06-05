<template>
  <div class="login-page">
    <section class="login-shell">
      <div class="brand-panel">
        <div class="brand-lockup">
          <span class="brand-mark">
            <CoffeeCup />
          </span>
          <div>
            <p>Beer Competition Console</p>
            <h1>主办方控制台</h1>
          </div>
        </div>

        <div class="brew-visual" aria-hidden="true">
          <span class="glass">
            <i class="foam foam-a" />
            <i class="foam foam-b" />
            <i class="beer" />
            <i class="shine" />
          </span>
          <span class="grain grain-a" />
          <span class="grain grain-b" />
          <span class="grain grain-c" />
        </div>

        <div class="operation-strip">
          <span>
            <b>赛事配置</b>
            报名、入库与评审桌
          </span>
          <span>
            <b>现场执行</b>
            进度、分组与结果确认
          </span>
          <span>
            <b>数据归档</b>
            酒款、评审与导出记录
          </span>
        </div>
      </div>

      <section class="login-card">
        <div class="card-head">
          <span class="card-kicker">Organizer Access</span>
          <h2>主办方后台登录</h2>
          <p>进入比赛管理、评审配置和现场看板。</p>
        </div>

        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" autocomplete="username">
              <template #prefix>
                <User />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password autocomplete="current-password">
              <template #prefix>
                <Lock />
              </template>
            </el-input>
          </el-form-item>
          <el-button native-type="submit" class="full" :loading="loading">
            登录
            <Right v-if="!loading" />
          </el-button>
        </el-form>

        <div class="security-note">
          <span />
          <p>仅限主办方授权账号访问</p>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { CoffeeCup, Lock, Right, User } from '@element-plus/icons-vue'
import { adminLogin } from '@/api/auth'
import { setSession } from '@/utils/auth'

const router = useRouter()
const form = reactive({ username: 'admin', password: '123456' })
const loading = ref(false)

async function submit() {
  if (loading.value) return
  loading.value = true
  try {
    const data = await adminLogin(form)
    setSession('admin', data.token, data.displayName)
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  --panel: rgba(18, 28, 32, 0.86);
  --panel-strong: rgba(20, 31, 35, 0.94);
  --line: rgba(219, 232, 237, 0.1);
  --text: #e8f0f2;
  --muted: #91a5ad;
  --faint: #61757e;
  --gold: #d8a935;
  --gold-soft: #e0b84a;
  --green: #70cf7c;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: var(--text);
  overflow: hidden;
  background:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    radial-gradient(circle at 22% 16%, rgba(216, 169, 53, 0.18), transparent 20rem),
    radial-gradient(circle at 82% 12%, rgba(111, 180, 207, 0.1), transparent 21rem),
    linear-gradient(135deg, #0b1115 0%, #111c20 50%, #0b1418 100%);
  background-size: 48px 48px, 48px 48px, auto, auto, auto;
}

.login-page::before,
.login-page::after {
  position: fixed;
  pointer-events: none;
  content: "";
}

.login-page::before {
  inset: 0;
  background: repeating-linear-gradient(112deg, transparent 0 20px, rgba(216, 169, 53, 0.02) 20px 21px, transparent 21px 48px);
}

.login-page::after {
  inset: auto 8% 0 auto;
  width: 34rem;
  height: 34rem;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(216, 169, 53, 0.08), transparent 66%);
  transform: translateY(44%);
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  width: min(980px, 100%);
  min-height: 540px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(12, 18, 21, 0.72);
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.34);
  overflow: hidden;
  backdrop-filter: blur(18px);
}

.brand-panel,
.login-card {
  position: relative;
  min-width: 0;
}

.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 32px;
  background:
    radial-gradient(circle at 22% 20%, rgba(216, 169, 53, 0.14), transparent 12rem),
    linear-gradient(145deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.012));
  border-right: 1px solid var(--line);
}

.brand-lockup,
.operation-strip span,
.security-note,
.full {
  display: flex;
  align-items: center;
}

.brand-lockup {
  gap: 14px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 50px;
  height: 50px;
  color: var(--gold-soft);
  border: 1px solid rgba(216, 169, 53, 0.34);
  border-radius: 12px;
  background: rgba(216, 169, 53, 0.1);
  box-shadow: inset 0 0 18px rgba(216, 169, 53, 0.08);
}

.brand-mark svg {
  width: 26px;
  height: 26px;
}

p,
h1,
h2 {
  margin: 0;
}

.brand-lockup p,
.card-kicker {
  color: var(--gold-soft);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.brand-lockup h1 {
  margin-top: 6px;
  font-size: 30px;
  line-height: 1.1;
}

.brew-visual {
  position: relative;
  align-self: center;
  width: min(320px, 86%);
  aspect-ratio: 1.08;
}

.brew-visual::before {
  position: absolute;
  inset: 13%;
  border: 1px solid rgba(216, 169, 53, 0.13);
  border-radius: 50%;
  background:
    linear-gradient(90deg, transparent 48%, rgba(216, 169, 53, 0.08) 48% 50%, transparent 50%),
    linear-gradient(transparent 48%, rgba(216, 169, 53, 0.08) 48% 50%, transparent 50%);
  content: "";
  transform: rotate(-8deg);
}

.glass {
  position: absolute;
  inset: 18% 25% 14% 24%;
  border: 2px solid rgba(232, 240, 242, 0.3);
  border-top: 0;
  border-radius: 0 0 22px 22px;
  background: rgba(255, 255, 255, 0.03);
  box-shadow: inset 0 -60px 0 rgba(216, 169, 53, 0.82), 0 28px 58px rgba(0, 0, 0, 0.22);
}

.glass::after {
  position: absolute;
  top: 20%;
  right: -42%;
  width: 42%;
  height: 36%;
  border: 12px solid rgba(232, 240, 242, 0.18);
  border-left: 0;
  border-radius: 0 28px 28px 0;
  content: "";
}

.foam {
  position: absolute;
  top: -24px;
  border-radius: 999px;
  background: #f5efe2;
}

.foam-a {
  left: -10px;
  width: 58px;
  height: 34px;
}

.foam-b {
  right: -8px;
  width: 76px;
  height: 38px;
}

.beer {
  position: absolute;
  inset: 36% 10px 10px;
  border-radius: 0 0 16px 16px;
  background:
    radial-gradient(circle at 28% 42%, rgba(255, 244, 206, 0.42) 0 3px, transparent 4px),
    radial-gradient(circle at 66% 30%, rgba(255, 244, 206, 0.35) 0 4px, transparent 5px),
    linear-gradient(180deg, #e8bc3d 0%, #c88423 100%);
}

.shine {
  position: absolute;
  top: 16%;
  left: 16px;
  width: 9px;
  height: 52%;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.32);
}

.grain {
  position: absolute;
  width: 9px;
  height: 70px;
  border-radius: 999px;
  background: linear-gradient(180deg, transparent, rgba(224, 184, 74, 0.8), transparent);
  transform-origin: bottom center;
}

.grain::before,
.grain::after {
  position: absolute;
  width: 22px;
  height: 14px;
  border: 1px solid rgba(224, 184, 74, 0.45);
  border-radius: 999px 999px 999px 0;
  content: "";
}

.grain::before {
  top: 18px;
  right: 5px;
  transform: rotate(-28deg);
}

.grain::after {
  top: 36px;
  left: 5px;
  transform: scaleX(-1) rotate(-28deg);
}

.grain-a {
  left: 13%;
  bottom: 18%;
  transform: rotate(-18deg);
}

.grain-b {
  right: 17%;
  bottom: 13%;
  transform: rotate(16deg);
}

.grain-c {
  right: 28%;
  top: 9%;
  opacity: 0.58;
  transform: rotate(38deg) scale(0.8);
}

.operation-strip {
  display: grid;
  gap: 10px;
}

.operation-strip span {
  gap: 12px;
  min-height: 54px;
  padding: 12px 14px;
  color: var(--muted);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.operation-strip span::before {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--gold-soft);
  box-shadow: 0 0 0 6px rgba(216, 169, 53, 0.08);
  content: "";
}

.operation-strip b {
  display: block;
  color: var(--text);
  margin-right: 4px;
}

.login-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 44px 36px;
  background: var(--panel);
}

.login-card::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.045), transparent 32%);
  content: "";
}

.login-card > * {
  position: relative;
}

.card-head {
  margin-bottom: 28px;
}

.card-head h2 {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.15;
}

.card-head p {
  margin-top: 10px;
  color: var(--muted);
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #c5d3d8;
  font-weight: 700;
}

:deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid var(--line);
  box-shadow: none;
  transition: border-color 0.18s ease, background 0.18s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(224, 184, 74, 0.42);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 0 0 3px rgba(216, 169, 53, 0.08);
}

:deep(.el-input__inner) {
  color: var(--text);
}

:deep(.el-input__prefix),
:deep(.el-input__suffix) {
  color: var(--muted);
}

.full {
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 46px;
  margin-top: 4px;
  color: #111719;
  font-weight: 800;
  border: 1px solid rgba(224, 184, 74, 0.56);
  border-radius: 8px;
  background: linear-gradient(180deg, #efc85b 0%, #d7a42d 100%);
  box-shadow: 0 14px 28px rgba(216, 169, 53, 0.18);
}

.full:hover,
.full:focus {
  color: #111719;
  border-color: rgba(244, 215, 120, 0.74);
  background: linear-gradient(180deg, #f5d66f 0%, #deb03a 100%);
}

.full svg {
  width: 16px;
  height: 16px;
}

.security-note {
  gap: 8px;
  margin-top: 18px;
  color: var(--faint);
  font-size: 13px;
}

.security-note span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--green);
}

@media (max-width: 900px) {
  .login-page {
    padding: 18px;
    overflow: auto;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    gap: 24px;
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }

  .brew-visual {
    display: none;
  }
}

@media (max-width: 560px) {
  .login-page {
    align-items: stretch;
    padding: 0;
  }

  .login-shell {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
  }

  .brand-panel,
  .login-card {
    padding: 26px 20px;
  }

  .brand-lockup h1,
  .card-head h2 {
    font-size: 24px;
  }

  .operation-strip span {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>
