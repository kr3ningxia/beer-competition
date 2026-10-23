<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="min(520px, 94vw)"
    class="image-crop-dialog-shell"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="(value) => emit('update:modelValue', value)"
  >
    <div class="image-crop-panel">
      <div
        class="image-crop-stage"
        :class="{ dragging }"
        @pointerdown="startDrag"
        @pointermove="moveDrag"
        @pointerup="endDrag"
        @pointercancel="endDrag"
        @pointerleave="endDrag"
        @wheel.prevent="zoomByWheel"
      >
        <img
          v-if="previewUrl"
          :src="previewUrl"
          alt="裁剪预览"
          draggable="false"
          :style="imageStyle"
          @load="handleImageLoad"
        >
      </div>
      <label class="image-crop-scale">
        <span>缩放</span>
        <input v-model.number="scale" min="1" max="4" step="0.01" type="range" :disabled="!previewUrl">
      </label>
    </div>
    <template #footer>
      <div class="image-crop-actions">
        <button class="image-crop-button" type="button" :disabled="!previewUrl" @click="resetCrop">重置</button>
        <button class="image-crop-button primary" type="button" :disabled="!previewUrl" @click="confirmCrop">使用此图片</button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  file: { type: File, default: null },
  title: { type: String, default: '调整图片' },
  outputSize: { type: Number, default: 1024 },
  outputSuffix: { type: String, default: 'crop' },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const previewUrl = ref('')
const scale = ref(1)
const offset = reactive({ x: 0, y: 0 })
const dragging = ref(false)
const drag = reactive({ startX: 0, startY: 0, originX: 0, originY: 0 })
const imageSize = reactive({ width: 0, height: 0 })

const geometry = computed(() => {
  const imageWidth = Number(imageSize.width) || 1
  const imageHeight = Number(imageSize.height) || 1
  const imageRatio = imageWidth / imageHeight
  const baseWidthPercent = imageRatio >= 1 ? imageRatio * 100 : 100
  const baseHeightPercent = imageRatio >= 1 ? 100 : (1 / imageRatio) * 100
  const widthPercent = baseWidthPercent * scale.value
  const heightPercent = baseHeightPercent * scale.value
  return {
    widthPercent,
    heightPercent,
    maxX: Math.max(0, (widthPercent - 100) / 2),
    maxY: Math.max(0, (heightPercent - 100) / 2),
  }
})

const imageStyle = computed(() => ({
  width: `${geometry.value.widthPercent}%`,
  height: `${geometry.value.heightPercent}%`,
  left: `calc(50% + ${offset.x}%)`,
  top: `calc(50% + ${offset.y}%)`,
}))

watch(() => props.modelValue, (open) => {
  if (!open) {
    revokePreview()
    return
  }
  if (props.file) loadFile(props.file)
})

watch(scale, clampOffset)

onBeforeUnmount(revokePreview)

function loadFile(file) {
  revokePreview()
  previewUrl.value = URL.createObjectURL(file)
  imageSize.width = 0
  imageSize.height = 0
  resetCrop()
}

function revokePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  dragging.value = false
}

function handleImageLoad(event) {
  imageSize.width = event.target.naturalWidth || 1
  imageSize.height = event.target.naturalHeight || 1
  resetCrop()
}

function resetCrop() {
  scale.value = 1
  offset.x = 0
  offset.y = 0
}

function startDrag(event) {
  if (!previewUrl.value) return
  dragging.value = true
  drag.startX = event.clientX
  drag.startY = event.clientY
  drag.originX = offset.x
  drag.originY = offset.y
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function moveDrag(event) {
  if (!dragging.value) return
  const bounds = event.currentTarget.getBoundingClientRect()
  offset.x = drag.originX + ((event.clientX - drag.startX) / bounds.width) * 100
  offset.y = drag.originY + ((event.clientY - drag.startY) / bounds.height) * 100
  clampOffset()
}

function endDrag(event) {
  if (!dragging.value) return
  dragging.value = false
  event.currentTarget.releasePointerCapture?.(event.pointerId)
}

function zoomByWheel(event) {
  if (!previewUrl.value) return
  const nextScale = scale.value + (event.deltaY > 0 ? -0.08 : 0.08)
  scale.value = Math.min(4, Math.max(1, Number(nextScale.toFixed(2))))
}

function clampOffset() {
  const { maxX, maxY } = geometry.value
  offset.x = Math.min(maxX, Math.max(-maxX, Number(offset.x) || 0))
  offset.y = Math.min(maxY, Math.max(-maxY, Number(offset.y) || 0))
}

function confirmCrop() {
  if (!props.file || !previewUrl.value) return
  buildCroppedFile().then((file) => emit('confirm', file)).catch(() => {})
}

function buildCroppedFile() {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => {
      const size = props.outputSize
      const canvas = document.createElement('canvas')
      canvas.width = size
      canvas.height = size
      const context = canvas.getContext('2d')
      if (!context) {
        reject(new Error('无法处理图片'))
        return
      }
      const { widthPercent, heightPercent } = geometry.value
      const drawWidth = size * (widthPercent / 100)
      const drawHeight = size * (heightPercent / 100)
      const drawX = (size - drawWidth) / 2 + size * (offset.x / 100)
      const drawY = (size - drawHeight) / 2 + size * (offset.y / 100)
      context.fillStyle = '#ffffff'
      context.fillRect(0, 0, size, size)
      context.imageSmoothingEnabled = true
      context.imageSmoothingQuality = 'high'
      context.drawImage(image, drawX, drawY, drawWidth, drawHeight)
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('图片导出失败'))
          return
        }
        const baseName = (props.file?.name || 'image').replace(/\.[^.]+$/, '') || 'image'
        resolve(new File([blob], `${baseName}-${props.outputSuffix}.png`, { type: 'image/png' }))
      }, 'image/png')
    }
    image.onerror = () => reject(new Error('图片读取失败'))
    image.src = previewUrl.value
  })
}
</script>

<style scoped>
:global(.image-crop-dialog-shell.el-dialog) {
  overflow: hidden;
  border: 1px solid rgba(219, 232, 237, 0.12);
  border-radius: 10px;
  background: #10191d;
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.46);
}

:global(.image-crop-dialog-shell .el-dialog__header) {
  margin: 0;
  padding: 16px 18px 10px;
  border-bottom: 1px solid rgba(219, 232, 237, 0.08);
}

:global(.image-crop-dialog-shell .el-dialog__title) {
  color: #edf4f6;
  font-size: 16px;
  font-weight: 900;
}

:global(.image-crop-dialog-shell .el-dialog__headerbtn) {
  top: 8px;
}

:global(.image-crop-dialog-shell .el-dialog__close) {
  color: #8ea1aa;
}

:global(.image-crop-dialog-shell .el-dialog__body) {
  padding: 16px 18px 4px;
}

:global(.image-crop-dialog-shell .el-dialog__footer) {
  padding: 12px 18px 16px;
}

.image-crop-panel {
  display: grid;
  gap: 14px;
}

.image-crop-stage {
  position: relative;
  width: min(340px, 70vw);
  aspect-ratio: 1;
  justify-self: center;
  overflow: hidden;
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  background: #ffffff;
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.image-crop-stage.dragging {
  cursor: grabbing;
}

.image-crop-stage img {
  position: absolute;
  transform: translate(-50%, -50%);
  object-fit: fill;
  pointer-events: none;
  user-select: none;
}

.image-crop-scale {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  color: #a9bbc2;
  font-size: 13px;
  font-weight: 800;
}

.image-crop-scale input[type='range'] {
  width: 100%;
  accent-color: #e0b84a;
}

.image-crop-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.image-crop-button {
  min-height: 40px;
  padding: 0 14px;
  color: #edf4f6;
  border: 1px solid rgba(219, 232, 237, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.image-crop-button:hover:not(:disabled) {
  border-color: rgba(224, 184, 74, 0.42);
  background: rgba(255, 255, 255, 0.06);
}

.image-crop-button.primary {
  color: #e0b84a;
  border-color: rgba(216, 169, 53, 0.32);
  background: rgba(216, 169, 53, 0.08);
}

.image-crop-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
