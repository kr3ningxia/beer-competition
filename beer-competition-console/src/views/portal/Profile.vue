<template>
  <div class="profile-page">
    <section class="profile-hero brewer-card">
      <div>
        <span class="label-chip tone-green">BREWERY PROFILE</span>
        <h2>{{ breweryProfile.breweryName }}</h2>
        <p>{{ breweryProfile.bio }}</p>
      </div>
      <div class="seal">
        <span>2026</span>
        <strong>参赛厂牌</strong>
      </div>
    </section>

    <section class="profile-grid">
      <article class="brewer-card profile-card">
        <h3 class="portal-section-title">联系人信息</h3>
        <el-form :model="profileForm" label-position="top">
          <el-form-item label="厂牌名称"><el-input v-model="profileForm.breweryName" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model="profileForm.contactName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="profileForm.phone" /></el-form-item>
          <el-form-item label="微信号"><el-input v-model="profileForm.wechat" /></el-form-item>
          <el-form-item label="所在城市"><el-input v-model="profileForm.city" /></el-form-item>
          <el-form-item label="厂牌简介">
            <el-input v-model="profileForm.bio" type="textarea" :rows="4" />
          </el-form-item>
          <el-button type="primary" @click="saveMock">保存资料</el-button>
        </el-form>
      </article>

      <article class="brewer-card profile-card">
        <h3 class="portal-section-title">奖项收件地址</h3>
        <el-form :model="addressForm" label-position="top">
          <el-form-item label="收件人"><el-input v-model="addressForm.receiver" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="addressForm.phone" /></el-form-item>
          <el-form-item label="省市区"><el-input v-model="addressForm.region" /></el-form-item>
          <el-form-item label="详细地址">
            <el-input v-model="addressForm.detail" type="textarea" :rows="4" />
          </el-form-item>
          <div class="address-tip">
            <strong>用于获奖后寄送奖牌、奖状或主办方物料。</strong>
            <p>结果发布前也可以提前维护，减少赛后沟通成本。</p>
          </div>
          <el-button @click="saveMock">保存地址</el-button>
        </el-form>
      </article>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { breweryProfile, deliveryAddress } from './mockData'

const profileForm = reactive({ ...breweryProfile })
const addressForm = reactive({ ...deliveryAddress })

function saveMock() {
  ElMessage.success('Mock 保存成功')
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
  font-size: 30px;
  font-weight: 900;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
