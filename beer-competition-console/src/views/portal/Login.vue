<template>
  <div class="portal-login">
    <section class="login-art">
      <span>BC-2026</span>
      <h1>参赛厂牌报名入口</h1>
      <p>提交酒款、完成付款、下载现场标签，并在结果发布后查看评审反馈。</p>
      <div class="label-stack">
        <div>IPA / SOUR / STOUT / LAGER</div>
        <strong>Greater Bay Craft Beer Awards</strong>
      </div>
    </section>

    <section class="login-card">
      <span class="label-chip tone-gold">BREWER LOGIN</span>
      <h2>厂商端登录</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="sms-row">
            <el-input v-model="form.code" />
            <el-button @click="send">发送验证码</el-button>
          </div>
        </el-form-item>
        <div class="mock-note">当前为前端 mock 预览，验证码默认 123456。</div>
        <el-button type="primary" class="full" @click="submit">进入厂商端</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createLocalSessionToken, setSession } from '@/utils/auth'

const router = useRouter()
const form = reactive({ phone: '13800000001', code: '123456' })

function send() {
  ElMessage.success('Mock 验证码已发送：123456')
}

function submit() {
  const displayName = '山雾麦芽工坊'
  setSession('portal', createLocalSessionToken('portal', displayName), displayName)
  ElMessage.success('已进入厂商端')
  router.push('/portal/home')
}
</script>

<style scoped>
.portal-login {
  --el-color-primary: #b87517;
  --el-color-primary-light-3: #d99d3d;
  --el-color-primary-light-5: #e8bc6b;
  --el-color-primary-light-7: #f1d394;
  --el-color-primary-light-9: #fff0c2;
  --el-color-primary-dark-2: #8b5c19;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 460px;
  min-height: 100vh;
  padding: 28px;
  color: #2b1d10;
  background:
    linear-gradient(90deg, rgba(80, 51, 22, 0.055) 1px, transparent 1px),
    linear-gradient(180deg, #fbf2dc 0%, #f1e1bf 100%);
  background-size: 24px 24px, auto;
}

.login-art {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: calc(100vh - 56px);
  padding: 44px;
  color: #fff6df;
  background:
    linear-gradient(135deg, rgba(33, 25, 18, 0.94), rgba(84, 48, 17, 0.88)),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 16px);
  border-radius: 8px;
}

.login-art > span {
  width: max-content;
  padding: 8px 12px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 999px;
  font-weight: 900;
}

.login-art h1 {
  max-width: 720px;
  margin: 24px 0 12px;
  font-size: 64px;
  line-height: 1;
}

.login-art p {
  max-width: 560px;
  margin: 0;
  color: #d8c8a8;
  font-size: 18px;
}

.label-stack {
  width: min(430px, 100%);
  margin-top: 54px;
  padding: 20px;
  color: #2b1d10;
  background: #fffaf0;
  border-radius: 8px;
}

.label-stack div {
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.label-stack strong {
  display: block;
  margin-top: 12px;
  font-size: 22px;
}

.login-card {
  align-self: center;
  margin-left: 28px;
  padding: 30px;
  background: rgba(255, 250, 240, 0.92);
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
  box-shadow: 0 24px 56px rgba(83, 51, 17, 0.14);
}

.login-card h2 {
  margin: 18px 0 24px;
  font-size: 30px;
}

.label-chip {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  color: #6b4710;
  background: #f3d978;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.sms-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.mock-note {
  margin: 8px 0 18px;
  padding: 12px;
  color: #725018;
  background: #fff0c2;
  border-radius: 8px;
}

.full {
  width: 100%;
}

@media (max-width: 980px) {
  .portal-login {
    grid-template-columns: 1fr;
  }

  .login-card {
    margin: 20px 0 0;
  }
}

@media (max-width: 680px) {
  .portal-login {
    padding: 16px;
  }

  .login-art {
    min-height: auto;
    padding: 28px;
  }

  .login-art h1 {
    font-size: 42px;
  }
}
</style>
