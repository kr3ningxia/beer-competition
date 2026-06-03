<template>
  <div class="profile-page">
    <section class="profile-hero brewer-card">
      <div>
        <span class="label-chip tone-green">厂牌资料</span>
        <h2>{{ profileForm.companyName || '完善厂牌资料' }}</h2>
        <p>用于报名核对和主办方联系。</p>
      </div>
      <div class="seal">
        <span>资料</span>
        <strong>报名核对使用</strong>
      </div>
    </section>

    <section class="profile-grid">
      <article class="brewer-card profile-card">
        <h3 class="portal-section-title">联系人信息</h3>
        <el-form :model="profileForm" label-position="top">
          <el-form-item label="账号名称"><el-input v-model="profileForm.displayName" /></el-form-item>
          <el-form-item label="厂牌名称"><el-input v-model="profileForm.companyName" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model="profileForm.contactName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="profileForm.phone" disabled /></el-form-item>
          <el-form-item label="微信号"><el-input v-model="profileForm.wechat" /></el-form-item>
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </el-form>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchPortalProfile, updatePortalProfile } from '@/api/portal'
import { setDisplayName } from '@/utils/auth'

const profileForm = reactive({
  displayName: '',
  companyName: '',
  contactName: '',
  phone: '',
  wechat: '',
})

onMounted(async () => {
  Object.assign(profileForm, await fetchPortalProfile())
})

async function saveProfile() {
  const data = await updatePortalProfile({
    displayName: profileForm.displayName,
    companyName: profileForm.companyName,
    contactName: profileForm.contactName,
    wechat: profileForm.wechat,
  })
  Object.assign(profileForm, data)
  setDisplayName('portal', data.displayName)
  ElMessage.success('已保存')
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 22px;
}

.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  color: #fff6df;
  background:
    linear-gradient(135deg, #2b1d10, #4f3216),
    repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 16px);
}

.profile-hero h2 {
  margin: 18px 0 10px;
  font-size: 42px;
}

.profile-hero p {
  max-width: 720px;
  margin: 0;
  color: #d8c8a8;
  line-height: 1.7;
}

.seal {
  display: grid;
  place-items: center;
  flex: 0 0 138px;
  height: 138px;
  color: #2b1d10;
  background: #e1a23d;
  border-radius: 50%;
  border: 8px solid rgba(255, 250, 240, 0.32);
}

.seal span,
.seal strong {
  display: block;
}

.seal span {
  font-size: 26px;
  font-weight: 900;
}

.seal strong {
  max-width: 86px;
  text-align: center;
  line-height: 1.25;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 760px);
  gap: 20px;
}

.profile-card {
  padding: 24px;
}

.profile-card :deep(.el-input__wrapper),
.profile-card :deep(.el-textarea__inner) {
  background: #fffdf7;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(87, 58, 26, 0.14) inset;
}

.address-tip {
  margin-bottom: 18px;
  padding: 14px;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.2);
  border-radius: 8px;
}

.address-tip p {
  margin: 6px 0 0;
  color: #746a5f;
}

@media (max-width: 980px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-hero {
    flex-direction: column;
  }
}
</style>
