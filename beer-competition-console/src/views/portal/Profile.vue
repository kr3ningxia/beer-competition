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
              ref="avatarUploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp"
              :on-change="handleAvatarChange"
            >
              <el-button :loading="uploadingAvatar">选择头像</el-button>
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

    <div v-if="avatarCrop.open" class="avatar-crop-mask" @click.self="cancelAvatarCrop">
      <section class="avatar-crop-dialog" role="dialog" aria-modal="true" aria-label="调整厂牌头像">
        <header>
          <div>
            <h3>调整厂牌头像</h3>
            <span>拖动图片调整位置，缩放到合适大小后再上传</span>
          </div>
          <button class="avatar-crop-close" type="button" aria-label="关闭调整头像" @click="cancelAvatarCrop">
            <Close />
          </button>
        </header>

        <div
          class="avatar-crop-stage"
          @pointerdown="startAvatarDrag"
          @pointermove="moveAvatarDrag"
          @pointerup="endAvatarDrag"
          @pointercancel="endAvatarDrag"
          @pointerleave="endAvatarDrag"
          @wheel.prevent="zoomAvatarCrop"
        >
          <img
            v-if="avatarCrop.previewUrl"
            :src="avatarCrop.previewUrl"
            alt="头像预览"
            draggable="false"
            :style="avatarCropImageStyle"
            @load="handleAvatarPreviewLoad"
          >
          <div class="avatar-crop-shade" aria-hidden="true"></div>
          <div class="avatar-crop-frame" aria-hidden="true"></div>
        </div>

        <div class="avatar-crop-tools">
          <label>
            <span>缩放</span>
            <input v-model.number="avatarCrop.scale" min="1" max="3" step="0.01" type="range">
          </label>
          <div class="avatar-crop-actions">
            <el-button :icon="RefreshLeft" :disabled="avatarCrop.uploading" @click="resetAvatarCrop">重置</el-button>
            <el-button type="primary" :loading="avatarCrop.uploading" @click="confirmAvatarCrop">确认上传</el-button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, RefreshLeft } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { BASE_URL } from '@/config'
import { fetchPortalProfile, updatePortalProfile, uploadPortalAvatar } from '@/api/portal'
import { setDisplayName } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const avatarCropOutputSize = 720
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
const avatarUploadRef = ref()
const avatarCrop = reactive({
  open: false,
  file: null,
  previewUrl: '',
  imageWidth: 0,
  imageHeight: 0,
  scale: 1,
  x: 0,
  y: 0,
  dragging: false,
  dragStartX: 0,
  dragStartY: 0,
  startX: 0,
  startY: 0,
  uploading: false,
})

const avatarPreviewUrl = computed(() => resolveAvatarUrl(profileForm.avatarUrl))
const profileInitial = computed(() => {
  const value = (profileForm.companyName || profileForm.displayName || '厂').trim()
  const ascii = value.match(/[A-Za-z0-9]/)
  return (ascii ? ascii[0] : Array.from(value)[0] || '厂').toUpperCase()
})
const avatarCropGeometry = computed(() => {
  const imageWidth = Number(avatarCrop.imageWidth) || 1
  const imageHeight = Number(avatarCrop.imageHeight) || 1
  const imageRatio = imageWidth / imageHeight
  const baseWidthPercent = imageRatio >= 1 ? imageRatio * 100 : 100
  const baseHeightPercent = imageRatio >= 1 ? 100 : (1 / imageRatio) * 100
  const widthPercent = baseWidthPercent * avatarCrop.scale
  const heightPercent = baseHeightPercent * avatarCrop.scale
  return {
    widthPercent,
    heightPercent,
    maxX: Math.max(0, (widthPercent - 100) / 2),
    maxY: Math.max(0, (heightPercent - 100) / 2),
  }
})
const avatarCropImageStyle = computed(() => ({
  width: `${avatarCropGeometry.value.widthPercent}%`,
  height: `${avatarCropGeometry.value.heightPercent}%`,
  left: `calc(50% + ${avatarCrop.x}%)`,
  top: `calc(50% + ${avatarCrop.y}%)`,
}))

onMounted(async () => {
  Object.assign(profileForm, await fetchPortalProfile())
  sameAsAccountName.value = Boolean(profileForm.displayName && profileForm.companyName === profileForm.displayName)
})

onUnmounted(() => {
  closeAvatarCrop()
})

watch(
  () => profileForm.displayName,
  () => {
    if (sameAsAccountName.value) {
      profileForm.companyName = profileForm.displayName
    }
  }
)

watch(() => avatarCrop.scale, clampAvatarCropOffset)

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

function handleAvatarChange(uploadFile) {
  const file = uploadFile.raw
  avatarUploadRef.value?.clearFiles()
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('头像仅支持 JPG、PNG、WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    return
  }

  openAvatarCrop(file)
}

function openAvatarCrop(file) {
  closeAvatarCrop()
  avatarCrop.open = true
  avatarCrop.file = file
  avatarCrop.previewUrl = URL.createObjectURL(file)
  avatarCrop.imageWidth = 0
  avatarCrop.imageHeight = 0
  avatarCrop.scale = 1
  avatarCrop.x = 0
  avatarCrop.y = 0
  avatarCrop.dragging = false
  avatarCrop.uploading = false
}

function closeAvatarCrop() {
  if (avatarCrop.previewUrl) URL.revokeObjectURL(avatarCrop.previewUrl)
  avatarCrop.open = false
  avatarCrop.file = null
  avatarCrop.previewUrl = ''
  avatarCrop.dragging = false
  avatarCrop.uploading = false
}

function cancelAvatarCrop() {
  if (avatarCrop.uploading) return
  closeAvatarCrop()
}

function handleAvatarPreviewLoad(event) {
  avatarCrop.imageWidth = event.target.naturalWidth || 1
  avatarCrop.imageHeight = event.target.naturalHeight || 1
  resetAvatarCrop()
}

function resetAvatarCrop() {
  avatarCrop.scale = 1
  avatarCrop.x = 0
  avatarCrop.y = 0
}

function startAvatarDrag(event) {
  if (!avatarCrop.previewUrl || avatarCrop.uploading) return
  avatarCrop.dragging = true
  avatarCrop.dragStartX = event.clientX
  avatarCrop.dragStartY = event.clientY
  avatarCrop.startX = avatarCrop.x
  avatarCrop.startY = avatarCrop.y
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function moveAvatarDrag(event) {
  if (!avatarCrop.dragging) return
  const bounds = event.currentTarget.getBoundingClientRect()
  const deltaXPercent = ((event.clientX - avatarCrop.dragStartX) / bounds.width) * 100
  const deltaYPercent = ((event.clientY - avatarCrop.dragStartY) / bounds.height) * 100
  avatarCrop.x = avatarCrop.startX + deltaXPercent
  avatarCrop.y = avatarCrop.startY + deltaYPercent
  clampAvatarCropOffset()
}

function endAvatarDrag(event) {
  if (!avatarCrop.dragging) return
  avatarCrop.dragging = false
  event.currentTarget.releasePointerCapture?.(event.pointerId)
}

function zoomAvatarCrop(event) {
  if (!avatarCrop.previewUrl || avatarCrop.uploading) return
  const nextScale = avatarCrop.scale + (event.deltaY > 0 ? -0.08 : 0.08)
  avatarCrop.scale = Math.min(3, Math.max(1, Number(nextScale.toFixed(2))))
  clampAvatarCropOffset()
}

function clampAvatarCropOffset() {
  const { maxX, maxY } = avatarCropGeometry.value
  avatarCrop.x = Math.min(maxX, Math.max(-maxX, Number(avatarCrop.x) || 0))
  avatarCrop.y = Math.min(maxY, Math.max(-maxY, Number(avatarCrop.y) || 0))
}

async function confirmAvatarCrop() {
  if (!avatarCrop.file || !avatarCrop.previewUrl) return
  avatarCrop.uploading = true
  uploadingAvatar.value = true
  try {
    const croppedFile = await buildAvatarCropFile()
    const data = await uploadPortalAvatar(croppedFile)
    Object.assign(profileForm, data)
    setDisplayName('portal', data.displayName)
    ElMessage.success('头像已更新')
    closeAvatarCrop()
  } finally {
    uploadingAvatar.value = false
    avatarCrop.uploading = false
  }
}

function buildAvatarCropFile() {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = avatarCropOutputSize
      canvas.height = avatarCropOutputSize
      const context = canvas.getContext('2d')
      if (!context) {
        reject(new Error('无法处理头像图片'))
        return
      }
      const { widthPercent, heightPercent } = avatarCropGeometry.value
      const drawWidth = canvas.width * (widthPercent / 100)
      const drawHeight = canvas.height * (heightPercent / 100)
      const drawX = (canvas.width - drawWidth) / 2 + canvas.width * (avatarCrop.x / 100)
      const drawY = (canvas.height - drawHeight) / 2 + canvas.height * (avatarCrop.y / 100)
      context.imageSmoothingEnabled = true
      context.imageSmoothingQuality = 'high'
      context.drawImage(image, drawX, drawY, drawWidth, drawHeight)
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('头像图片导出失败'))
          return
        }
        const sourceName = avatarCrop.file?.name || 'avatar.png'
        const filename = sourceName.replace(/\.[^.]+$/, '') || 'avatar'
        resolve(new File([blob], `${filename}-avatar.png`, { type: 'image/png' }))
      }, 'image/png')
    }
    image.onerror = () => reject(new Error('头像图片读取失败'))
    image.src = avatarCrop.previewUrl
  })
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

.avatar-crop-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 28px;
  background: rgba(43, 29, 16, 0.42);
  backdrop-filter: blur(12px);
}

.avatar-crop-dialog {
  display: grid;
  gap: 16px;
  width: min(560px, 100%);
  max-height: calc(100dvh - 56px);
  overflow: auto;
  padding: 20px;
  color: #2b1d10;
  background:
    linear-gradient(180deg, rgba(255, 253, 247, 0.98), rgba(255, 246, 223, 0.98));
  border: 1px solid rgba(87, 58, 26, 0.18);
  border-radius: 8px;
  box-shadow: 0 28px 80px rgba(43, 29, 16, 0.28);
}

.avatar-crop-dialog header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.avatar-crop-dialog h3 {
  margin: 0;
  font-size: 22px;
  line-height: 1.2;
}

.avatar-crop-dialog header span {
  display: block;
  margin-top: 6px;
  color: #746a5f;
  font-size: 13px;
  line-height: 1.5;
}

.avatar-crop-close {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  color: #5c5045;
  background: #fffaf0;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.avatar-crop-close:hover,
.avatar-crop-close:focus-visible {
  color: #2b1d10;
  background: #fff0c2;
  border-color: rgba(184, 117, 23, 0.32);
}

.avatar-crop-close svg {
  width: 18px;
  height: 18px;
}

.avatar-crop-stage {
  position: relative;
  width: min(420px, 100%);
  aspect-ratio: 1;
  justify-self: center;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(45deg, rgba(255, 255, 255, 0.92) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(255, 255, 255, 0.92) 25%, transparent 25%),
    linear-gradient(45deg, transparent 75%, rgba(255, 255, 255, 0.92) 75%),
    linear-gradient(-45deg, transparent 75%, rgba(255, 255, 255, 0.92) 75%),
    #efe5d0;
  background-position: 0 0, 0 10px, 10px -10px, -10px 0;
  background-size: 20px 20px;
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.avatar-crop-stage:active {
  cursor: grabbing;
}

.avatar-crop-stage img {
  position: absolute;
  transform: translate(-50%, -50%);
  object-fit: fill;
  user-select: none;
  pointer-events: none;
}

.avatar-crop-shade,
.avatar-crop-frame {
  position: absolute;
  inset: 24px;
  border-radius: 50%;
  pointer-events: none;
}

.avatar-crop-shade {
  box-shadow: 0 0 0 999px rgba(43, 29, 16, 0.42);
}

.avatar-crop-frame {
  border: 3px solid rgba(255, 250, 240, 0.94);
  outline: 2px solid rgba(184, 117, 23, 0.86);
  outline-offset: -5px;
  box-shadow: 0 12px 34px rgba(43, 29, 16, 0.18);
}

.avatar-crop-tools {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.avatar-crop-tools label {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  color: #5c5045;
  font-size: 13px;
  font-weight: 800;
}

.avatar-crop-tools input[type='range'] {
  width: 100%;
  accent-color: #b87517;
}

.avatar-crop-actions {
  display: flex;
  gap: 10px;
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

  .avatar-crop-mask {
    align-items: end;
    padding: 12px;
  }

  .avatar-crop-dialog {
    gap: 14px;
    max-height: calc(100dvh - 24px);
    padding: 16px;
  }

  .avatar-crop-dialog h3 {
    font-size: 20px;
  }

  .avatar-crop-stage {
    width: min(100%, calc(100dvh - 260px));
    min-width: 0;
  }

  .avatar-crop-shade,
  .avatar-crop-frame {
    inset: 18px;
  }

  .avatar-crop-tools,
  .avatar-crop-actions {
    grid-template-columns: 1fr;
  }

  .avatar-crop-actions {
    display: grid;
  }

  .avatar-crop-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
