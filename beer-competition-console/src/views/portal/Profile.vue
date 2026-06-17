<template>
  <div class="profile-page">
    <section class="profile-hero brewer-card">
      <div class="hero-copy">
        <span class="label-chip tone-green">厂牌资料</span>
        <h2>{{ profileForm.companyName || '完善厂牌资料' }}</h2>
        <p>报名资料会用于参赛核对、送样沟通和结果通知</p>
      </div>
      <div class="hero-avatar">
        <div class="avatar-frame">
          <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" alt="厂牌头像">
          <span v-else>{{ profileInitial }}</span>
        </div>
      </div>
    </section>

    <section class="profile-grid">
      <article class="brewer-card profile-card form-card">
        <div class="card-heading">
          <h3 class="portal-section-title">基础信息</h3>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        </div>

        <el-form :model="profileForm" label-position="top" class="profile-form">
          <div class="form-row">
            <el-form-item label="账号名称">
              <el-input v-model="profileForm.displayName" maxlength="64" />
            </el-form-item>
            <el-form-item>
              <template #label>
                <span class="brand-name-label">
                  <span>品牌名</span>
                  <el-checkbox v-model="sameAsAccountName" @change="applySameAsAccountName">同账号名称</el-checkbox>
                </span>
              </template>
              <el-input v-model="profileForm.companyName" :disabled="sameAsAccountName" maxlength="128" />
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="联系人">
              <el-input v-model="profileForm.contactName" maxlength="64" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" disabled />
            </el-form-item>
          </div>

          <el-form-item label="微信号">
            <el-input v-model="profileForm.wechat" maxlength="64" />
          </el-form-item>
        </el-form>
      </article>

      <aside class="brewer-card profile-card avatar-card">
        <div class="avatar-card-head">
          <h3 class="portal-section-title">厂牌头像</h3>
          <span>JPG / PNG / WebP，5MB 内</span>
        </div>

        <div class="avatar-upload-panel">
          <div class="avatar-large">
            <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" alt="厂牌头像预览">
            <span v-else>{{ profileInitial }}</span>
          </div>
          <div class="avatar-actions">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp"
              :on-change="handleAvatarChange"
            >
              <el-button :loading="uploadingAvatar">上传头像</el-button>
            </el-upload>
          </div>
        </div>

        <dl class="profile-summary">
          <div>
            <dt>展示名称</dt>
            <dd>{{ profileForm.displayName || '-' }}</dd>
          </div>
          <div>
            <dt>报名品牌</dt>
            <dd>{{ profileForm.companyName || '-' }}</dd>
          </div>
          <div>
            <dt>联系人</dt>
            <dd>{{ profileForm.contactName || '-' }}</dd>
          </div>
        </dl>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { BASE_URL } from '@/config'
import { fetchPortalProfile, updatePortalProfile, uploadPortalAvatar } from '@/api/portal'
import { setDisplayName } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const profileForm = reactive({
  displayName: '',
  companyName: '',
  contactName: '',
  phone: '',
  wechat: '',
  avatarUrl: '',
})
const sameAsAccountName = ref(false)
const saving = ref(false)
const uploadingAvatar = ref(false)

const avatarPreviewUrl = computed(() => resolveAvatarUrl(profileForm.avatarUrl))
const profileInitial = computed(() => {
  const value = (profileForm.companyName || profileForm.displayName || '厂').trim()
  const ascii = value.match(/[A-Za-z0-9]/)
  return (ascii ? ascii[0] : Array.from(value)[0] || '厂').toUpperCase()
})

onMounted(async () => {
  Object.assign(profileForm, await fetchPortalProfile())
  sameAsAccountName.value = Boolean(profileForm.displayName && profileForm.companyName === profileForm.displayName)
})

watch(
  () => profileForm.displayName,
  () => {
    if (sameAsAccountName.value) {
      profileForm.companyName = profileForm.displayName
    }
  }
)

async function saveProfile() {
  saving.value = true
  try {
    const data = await updatePortalProfile({
      displayName: profileForm.displayName,
      companyName: profileForm.companyName,
      contactName: profileForm.contactName,
      wechat: profileForm.wechat,
    })
    Object.assign(profileForm, data)
    setDisplayName('portal', data.displayName)
    ElMessage.success('已保存')
    const nextPath = normalizePortalPath(route.query.next)
    if (nextPath) {
      router.push(nextPath)
    }
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('头像仅支持 JPG、PNG、WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    return
  }

  uploadingAvatar.value = true
  try {
    const data = await uploadPortalAvatar(file)
    Object.assign(profileForm, data)
    setDisplayName('portal', data.displayName)
    ElMessage.success('头像已更新')
  } finally {
    uploadingAvatar.value = false
  }
}

function applySameAsAccountName() {
  if (sameAsAccountName.value) {
    profileForm.companyName = profileForm.displayName
  }
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

function normalizePortalPath(value) {
  const rawValue = Array.isArray(value) ? value[0] : value
  if (!rawValue || typeof rawValue !== 'string') {
    return ''
  }
  if (!rawValue.startsWith('/portal') || rawValue.startsWith('/portal/login') || rawValue.startsWith('/portal/profile')) {
    return ''
  }
  return rawValue
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 22px;
}

.profile-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  min-height: 178px;
  overflow: hidden;
  padding: 28px 32px;
  color: #fff6df;
  background:
    radial-gradient(circle at 86% 22%, rgba(226, 162, 61, 0.32), transparent 32%),
    linear-gradient(135deg, #24180e 0%, #3b2614 56%, #5d3918 100%);
}

.profile-hero::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(90deg, rgba(255, 250, 240, 0.06) 0 1px, transparent 1px 18px);
}

.hero-copy,
.hero-avatar {
  position: relative;
  z-index: 1;
}

.profile-hero h2 {
  max-width: 760px;
  margin: 16px 0 10px;
  font-size: 40px;
  line-height: 1.12;
}

.profile-hero p {
  margin: 0;
  color: #d8c8a8;
  line-height: 1.7;
}

.hero-avatar {
  display: grid;
  place-items: center;
  width: 132px;
  height: 132px;
  border: 1px solid rgba(255, 246, 223, 0.22);
  border-radius: 8px;
  background: rgba(255, 246, 223, 0.1);
}

.avatar-frame,
.avatar-large {
  display: grid;
  place-items: center;
  overflow: hidden;
  color: #fff6df;
  background: #3d7d50;
  font-weight: 900;
}

.avatar-frame {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 6px solid rgba(255, 246, 223, 0.28);
  font-size: 34px;
}

.avatar-frame img,
.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 20px;
  align-items: start;
}

.profile-card {
  padding: 24px;
}

.card-heading,
.avatar-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.card-heading :deep(.el-button) {
  flex: 0 0 auto;
}

.avatar-card-head {
  display: grid;
  gap: 4px;
}

.avatar-card-head span {
  color: #746a5f;
  font-size: 13px;
}

.profile-form {
  display: grid;
  gap: 2px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.brand-name-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.brand-name-label :deep(.el-checkbox) {
  height: auto;
  margin-right: 0;
  font-weight: 500;
}

.profile-card :deep(.el-form-item__label) {
  color: #5c5045;
  font-weight: 700;
}

.profile-card :deep(.el-input__wrapper),
.profile-card :deep(.el-textarea__inner) {
  min-height: 38px;
  background: #fffdf7;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(87, 58, 26, 0.14) inset;
}

.profile-card :deep(.el-input.is-disabled .el-input__wrapper) {
  background: rgba(245, 236, 216, 0.72);
}

.avatar-upload-panel {
  display: grid;
  justify-items: center;
  gap: 18px;
  padding: 22px;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.12);
  border-radius: 8px;
}

.avatar-large {
  width: 144px;
  height: 144px;
  border-radius: 50%;
  border: 8px solid #fffdf7;
  box-shadow: 0 16px 32px rgba(67, 43, 17, 0.14);
  font-size: 46px;
}

.avatar-actions {
  display: flex;
  justify-content: center;
}

.profile-summary {
  display: grid;
  gap: 0;
  margin: 18px 0 0;
  border-top: 1px solid rgba(87, 58, 26, 0.12);
}

.profile-summary div {
  display: grid;
  grid-template-columns: 78px minmax(0, 1fr);
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px solid rgba(87, 58, 26, 0.1);
}

.profile-summary dt {
  color: #746a5f;
}

.profile-summary dd {
  min-width: 0;
  margin: 0;
  color: #2b1d10;
  font-weight: 800;
  overflow-wrap: anywhere;
}

@media (max-width: 980px) {
  .profile-hero,
  .profile-grid,
  .form-row {
    grid-template-columns: 1fr;
  }

  .hero-avatar {
    justify-self: start;
  }
}

@media (max-width: 640px) {
  .profile-hero {
    padding: 24px;
  }

  .profile-hero h2 {
    font-size: 30px;
  }

  .card-heading {
    display: grid;
  }

  .card-heading :deep(.el-button) {
    width: 100%;
  }
}
</style>
