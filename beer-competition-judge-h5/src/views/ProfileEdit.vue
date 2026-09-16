<template>
  <main class="app-shell">
    <section class="top-panel">
      <h1 class="page-title">{{ profile?.profileRequired ? '完善个人信息' : '修改个人信息' }}</h1>
    </section>

    <section class="card">
      <div class="avatar-edit-row">
        <button class="avatar-picker" type="button" aria-label="修改头像" @click="chooseAvatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" @error="avatarUrl = ''" />
          <span v-else>{{ avatarInitial }}</span>
          <span class="avatar-picker-mark" aria-hidden="true">+</span>
        </button>
        <div class="avatar-edit-copy">
          <strong>个人头像</strong>
          <span>支持 JPG、PNG、WebP，单张不超过 5MB</span>
          <button class="text-action" type="button" @click="chooseAvatar">选择图片</button>
        </div>
      </div>
      <input
        ref="avatarInput"
        class="visually-hidden"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        @change="handleAvatarSelected"
      />
      <label class="field">
        手机号
        <input v-model.trim="form.phone" class="input readonly" inputmode="tel" readonly />
      </label>
      <label class="field">
        姓名或称呼
        <input v-model.trim="form.name" class="input" placeholder="请输入真实姓名或行业熟知的称呼" />
      </label>
      <label class="field">
        微信号
        <input v-model.trim="form.wechat" class="input" placeholder="便于工作人员联系" />
      </label>
      <label class="field">
        资质信息
        <textarea v-model.trim="form.qualification" class="textarea" placeholder="例如 BJCP 等级、评审经验、从业背景"></textarea>
      </label>
      <label class="field">
        BJCP 编号（选填）
        <input v-model.trim="form.bjcpNumber" class="input" maxlength="64" />
      </label>
      <label class="consent-row">
        <input v-model="form.publicProfileConsent" type="checkbox" />
        <span>同意在已发布赛事结果页公开我的姓名、头像、评委角色和资质信息</span>
      </label>
      <section class="field conflict-field">
        <span>是否与酒厂有利益关联</span>
        <div class="choice-row" role="radiogroup" aria-label="是否与酒厂有利益关联">
          <button
            :class="{ active: !form.breweryConflictFlag }"
            type="button"
            @click="setBreweryConflict(false)"
          >
            无
          </button>
          <button
            :class="{ active: form.breweryConflictFlag }"
            type="button"
            @click="setBreweryConflict(true)"
          >
            有
          </button>
        </div>
        <p class="field-help">用于现场分桌和酒款回避，仅主办方后台可见。</p>
      </section>
      <label v-if="form.breweryConflictFlag" class="field">
        相关酒厂或品牌名称及关系说明
        <textarea
          v-model.trim="form.breweryConflictText"
          class="textarea"
          maxlength="500"
          placeholder="例如：某某酒厂，本人任职；某某品牌，近期有商业合作。"
        ></textarea>
      </label>
      <p v-if="error" class="form-error">{{ error }}</p>
    </section>

    <section class="card stack">
      <button class="button primary full" type="button" :disabled="saving" @click="save">保存资料</button>
      <button class="button secondary full" type="button" @click="goBack">返回</button>
    </section>

    <Teleport to="body">
      <div v-if="cropOpen" class="crop-backdrop" @click.self="cancelCrop">
        <section class="crop-sheet" role="dialog" aria-modal="true" aria-labelledby="crop-title">
          <header class="crop-header">
            <h2 id="crop-title">调整头像</h2>
            <button class="crop-close" type="button" aria-label="关闭裁剪层" @click="cancelCrop">×</button>
          </header>
          <div
            ref="cropViewport"
            class="crop-viewport"
            @pointerdown="startDrag"
            @pointermove="moveDrag"
            @pointerup="endDrag"
            @pointercancel="endDrag"
          >
            <img
              v-if="sourceUrl"
              class="crop-image"
              :src="sourceUrl"
              :style="cropImageStyle"
              alt="头像裁剪预览"
              draggable="false"
            />
            <span class="crop-guide" aria-hidden="true"></span>
          </div>
          <label class="zoom-control">
            <span>缩放</span>
            <input v-model.number="cropState.zoom" type="range" min="1" max="3" step="0.01" @input="constrainCrop" />
          </label>
          <div class="crop-actions">
            <button class="button secondary" type="button" :disabled="uploading" @click="cancelCrop">取消</button>
            <button class="button secondary" type="button" :disabled="uploading" @click="resetCrop">重置</button>
            <button class="button primary" type="button" :disabled="uploading" @click="uploadCroppedAvatar">
              {{ uploading ? '上传中' : '上传头像' }}
            </button>
          </div>
        </section>
      </div>
    </Teleport>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchJudgeAvatar, fetchProfile, updateProfile, uploadJudgeAvatar } from '@/api/judge'

const router = useRouter()
const profile = ref(null)
const saving = ref(false)
const error = ref('')
const avatarInput = ref(null)
const avatarUrl = ref('')
const cropOpen = ref(false)
const sourceUrl = ref('')
const sourceImage = ref(null)
const uploading = ref(false)
const dragState = ref(null)
const cropViewport = ref(null)
const cropState = reactive({ baseScale: 1, zoom: 1, x: 0, y: 0 })
const form = reactive({
  phone: '',
  name: '',
  wechat: '',
  qualification: '',
  bjcpNumber: '',
  publicProfileConsent: false,
  breweryConflictFlag: false,
  breweryConflictText: '',
})

const avatarInitial = computed(() => String(form.name || '评').trim().slice(0, 1) || '评')
const cropImageStyle = computed(() => {
  if (!sourceImage.value) return {}
  const scale = cropState.baseScale * cropState.zoom
  return {
    width: `${sourceImage.value.naturalWidth * scale}px`,
    height: `${sourceImage.value.naturalHeight * scale}px`,
    transform: `translate3d(${cropState.x}px, ${cropState.y}px, 0)`,
  }
})

onMounted(async () => {
  profile.value = await fetchProfile()
  form.phone = profile.value.phone || ''
  form.name = profile.value.name || ''
  form.wechat = profile.value.wechat || ''
  form.qualification = profile.value.qualification || ''
  form.bjcpNumber = profile.value.bjcpNumber || ''
  form.publicProfileConsent = Boolean(profile.value.publicProfileConsent)
  form.breweryConflictFlag = Boolean(profile.value.breweryConflictFlag)
  form.breweryConflictText = profile.value.breweryConflictText || ''
  await refreshAvatar(profile.value.avatarAssetId)
})

async function save() {
  error.value = ''
  if (!form.name || !form.qualification) {
    error.value = '请填写姓名或称呼，并补充资质信息。'
    return
  }
  if (form.breweryConflictFlag && !form.breweryConflictText) {
    error.value = '请填写相关酒厂或品牌名称及关系说明。'
    return
  }
  saving.value = true
  try {
    const next = await updateProfile({
      name: form.name,
      wechat: form.wechat,
      qualification: form.qualification,
      bjcpNumber: form.bjcpNumber,
      publicProfileConsent: Boolean(form.publicProfileConsent),
      breweryConflictFlag: form.breweryConflictFlag,
      breweryConflictText: form.breweryConflictFlag ? form.breweryConflictText : '',
    })
    if (Number(next.status) === 2) {
      router.push('/review-status')
      return
    }
    router.push('/profile')
  } finally {
    saving.value = false
  }
}

function goBack() {
  if (profile.value?.profileRequired) {
    router.push('/review-status')
    return
  }
  router.push('/profile')
}

function setBreweryConflict(value) {
  form.breweryConflictFlag = value
  if (!value) form.breweryConflictText = ''
}

function chooseAvatar() {
  avatarInput.value?.click()
}

function handleAvatarSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    error.value = '头像仅支持 JPG、PNG、WebP 图片。'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    error.value = '头像图片不能超过 5MB。'
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    const image = new Image()
    image.onload = () => {
      sourceImage.value = image
      sourceUrl.value = String(reader.result || '')
      cropOpen.value = true
      nextTick(resetCrop)
    }
    image.onerror = () => { error.value = '图片读取失败，请重新选择。' }
    image.src = String(reader.result || '')
  }
  reader.onerror = () => { error.value = '图片读取失败，请重新选择。' }
  reader.readAsDataURL(file)
}

function resetCrop() {
  if (!sourceImage.value) return
  const viewportSize = getCropViewportSize()
  cropState.baseScale = Math.max(viewportSize / sourceImage.value.naturalWidth, viewportSize / sourceImage.value.naturalHeight)
  cropState.zoom = 1
  cropState.x = (viewportSize - sourceImage.value.naturalWidth * cropState.baseScale) / 2
  cropState.y = (viewportSize - sourceImage.value.naturalHeight * cropState.baseScale) / 2
}

function constrainCrop() {
  if (!sourceImage.value) return
  const viewportSize = getCropViewportSize()
  const scale = cropState.baseScale * cropState.zoom
  const width = sourceImage.value.naturalWidth * scale
  const height = sourceImage.value.naturalHeight * scale
  cropState.x = Math.min(0, Math.max(viewportSize - width, cropState.x))
  cropState.y = Math.min(0, Math.max(viewportSize - height, cropState.y))
}

function getCropViewportSize() {
  const size = cropViewport.value?.getBoundingClientRect().width
  return size > 0 ? size : 240
}

function startDrag(event) {
  if (!sourceImage.value || uploading.value) return
  dragState.value = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, originX: cropState.x, originY: cropState.y }
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function moveDrag(event) {
  if (!dragState.value || dragState.value.pointerId !== event.pointerId) return
  cropState.x = dragState.value.originX + event.clientX - dragState.value.startX
  cropState.y = dragState.value.originY + event.clientY - dragState.value.startY
  constrainCrop()
}

function endDrag() {
  dragState.value = null
}

function cancelCrop() {
  if (uploading.value) return
  closeCrop()
}

function closeCrop() {
  cropOpen.value = false
  sourceUrl.value = ''
  sourceImage.value = null
  dragState.value = null
}

async function uploadCroppedAvatar() {
  if (!sourceImage.value || uploading.value) return
  uploading.value = true
  error.value = ''
  try {
    await uploadJudgeAvatar(await createCroppedFile())
    profile.value = await fetchProfile()
    await refreshAvatar(profile.value.avatarAssetId)
    closeCrop()
  } catch (uploadError) {
    error.value = uploadError.message || '头像上传失败，请稍后重试。'
  } finally {
    uploading.value = false
  }
}

function createCroppedFile() {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas')
    canvas.width = 512
    canvas.height = 512
    const context = canvas.getContext('2d')
    const scale = 512 / getCropViewportSize()
    const imageScale = cropState.baseScale * cropState.zoom
    context.drawImage(sourceImage.value, cropState.x * scale, cropState.y * scale,
      sourceImage.value.naturalWidth * imageScale * scale,
      sourceImage.value.naturalHeight * imageScale * scale)
    canvas.toBlob((blob) => {
      if (!blob) return reject(new Error('头像裁剪失败，请重试'))
      resolve(new File([blob], 'avatar.png', { type: 'image/png' }))
    }, 'image/png')
  })
}

async function refreshAvatar(assetId) {
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value)
  avatarUrl.value = ''
  if (!assetId) return
  try {
    avatarUrl.value = URL.createObjectURL(await fetchJudgeAvatar())
  } catch {
    avatarUrl.value = ''
  }
}

onBeforeUnmount(() => {
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value)
})
</script>

<style scoped>
.form-error {
  margin: 12px 0 0;
  color: #b42318;
  font-size: 14px;
  font-weight: 700;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
}

.avatar-edit-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eaecf0;
}

.avatar-picker {
  position: relative;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 72px;
  height: 72px;
  overflow: hidden;
  color: #fff4dc;
  background: #a75517;
  border: 0;
  border-radius: 50%;
  font-size: 30px;
  font-weight: 850;
}

.avatar-picker img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-picker-mark {
  position: absolute;
  right: 0;
  bottom: 0;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  color: #fff;
  background: #1f2a37;
  border: 2px solid #fff;
  border-radius: 50%;
  font-size: 18px;
  line-height: 1;
}

.avatar-edit-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.avatar-edit-copy strong {
  color: #18222f;
  font-size: 16px;
}

.avatar-edit-copy span {
  color: #667085;
  font-size: 13px;
  line-height: 1.4;
}

.text-action {
  justify-self: start;
  padding: 0;
  color: #a75517;
  background: transparent;
  border: 0;
  font-size: 14px;
  font-weight: 800;
}

.consent-row {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-top: 16px;
  color: #344054;
  font-size: 14px;
  line-height: 1.5;
}

.consent-row input {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin: 1px 0 0;
  accent-color: #a75517;
}

.crop-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 12px;
  background: rgba(15, 23, 29, 0.72);
}

.crop-sheet {
  width: min(100%, 420px);
  max-height: calc(100dvh - 24px);
  overflow: auto;
  padding: 18px 16px max(18px, env(safe-area-inset-bottom));
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.3);
}

.crop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.crop-header h2 {
  margin: 0;
  color: #18222f;
  font-size: 18px;
}

.crop-close {
  width: 36px;
  height: 36px;
  color: #667085;
  background: #f3f5f2;
  border: 0;
  border-radius: 50%;
  font-size: 24px;
  line-height: 1;
}

.crop-viewport {
  position: relative;
  width: min(100%, 320px);
  aspect-ratio: 1;
  height: auto;
  margin: 18px auto;
  overflow: hidden;
  touch-action: none;
  background: #18222f;
  border-radius: 8px;
  cursor: grab;
  user-select: none;
}

.crop-viewport:active {
  cursor: grabbing;
}

.crop-image {
  position: absolute;
  top: 0;
  left: 0;
  max-width: none;
  user-select: none;
  pointer-events: none;
}

.crop-guide {
  position: absolute;
  inset: 0;
  pointer-events: none;
  border: 2px solid rgba(255, 255, 255, 0.96);
  border-radius: 50%;
  box-shadow:
    0 0 0 999px rgba(12, 20, 24, 0.68),
    0 0 0 1px rgba(167, 85, 23, 0.9),
    inset 0 0 0 1px rgba(255, 255, 255, 0.34);
}

.crop-guide::before,
.crop-guide::after {
  position: absolute;
  content: '';
  pointer-events: none;
  opacity: 0.42;
}

.crop-guide::before {
  inset: 33.333% 0;
  border-top: 1px solid rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid rgba(255, 255, 255, 0.86);
}

.crop-guide::after {
  inset: 0 33.333%;
  border-right: 1px solid rgba(255, 255, 255, 0.86);
  border-left: 1px solid rgba(255, 255, 255, 0.86);
}

.zoom-control {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: #344054;
  font-size: 14px;
  font-weight: 750;
}

.zoom-control input {
  width: 100%;
  accent-color: #a75517;
}

.crop-actions {
  display: grid;
  grid-template-columns: 0.8fr 0.8fr 1.4fr;
  gap: 8px;
  margin-top: 20px;
}

.crop-actions .button {
  min-width: 0;
  padding-inline: 8px;
  white-space: nowrap;
}

@media (max-width: 380px) {
  .crop-sheet {
    padding-inline: 14px;
  }

  .crop-actions {
    grid-template-columns: 0.75fr 0.75fr 1.5fr;
  }
}

.readonly {
  color: #667085;
  background: #f3f5f2;
}

.conflict-field {
  display: grid;
  gap: 8px;
}

.conflict-field > span {
  color: #344054;
}

.choice-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.choice-row button {
  min-height: 44px;
  border: 1px solid #cbd5d1;
  border-radius: 8px;
  color: #344054;
  background: #fff;
  font-weight: 800;
}

.choice-row button.active {
  color: #a75517;
  border-color: rgba(167, 85, 23, 0.34);
  background: rgba(167, 85, 23, 0.08);
}

.field-help {
  margin: 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
}
</style>
