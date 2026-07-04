<template>
  <main class="app-shell">
    <section class="top-panel">
      <button class="back-link" type="button" @click="$router.push('/competitions')">
        <span class="back-icon" aria-hidden="true"></span>
        返回任务
      </button>
      <p class="eyebrow">{{ table?.roundName || '排序轮次' }}</p>
      <h1 class="page-title">{{ table?.tableName || '排序任务' }}</h1>
      <p class="ranking-mode">{{ rankingModeText }}</p>
    </section>

    <section class="card">
      <div class="split">
        <h2 class="section-title compact">{{ canSubmitFinal ? '本桌最终排序' : '我的参考排序' }}</h2>
        <span :class="['pill', submitted || draftSaved ? 'status-ok' : 'status-warn']">{{ rankingStatusText }}</span>
      </div>

      <div :class="['slot-list', { dragging: isDraggingEntry }]">
        <article
          v-for="slot in slots"
          :key="slot.rank"
          :data-rank="slot.rank"
          :class="['slot-drop', { filled: slot.beerEntryId, active: activeDropRank === slot.rank, dragging: isDraggingEntry }]"
          @dragover.prevent="activeDropRank = slot.rank"
          @dragleave="activeDropRank = null"
          @drop.prevent="dropEntryOnSlot(slot, $event)"
        >
          <div class="slot-label">
            <span>{{ displaySlotLabel(slot) }}</span>
            <div class="slot-actions">
              <button
                v-if="canEdit && !isDraggingEntry && findEntryById(slot.beerEntryId)"
                class="slot-clear-button"
                type="button"
                @click.stop="clearSlot(slot)"
              >
                清空
              </button>
              <button
                v-if="canEdit"
                class="slot-scan-button"
                type="button"
                :aria-label="`扫码指定${displaySlotLabel(slot)}`"
                @click.stop="openSlotScanner(slot)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 4h6v6H4V4Z" />
                  <path d="M14 4h6v6h-6V4Z" />
                  <path d="M4 14h6v6H4v-6Z" />
                  <path d="M14 14h2v2h-2v-2Z" />
                  <path d="M18 14h2v2h-2v-2Z" />
                  <path d="M14 18h2v2h-2v-2Z" />
                  <path d="M18 18h2v2h-2v-2Z" />
                </svg>
              </button>
            </div>
          </div>
          <template v-if="isDraggingEntry">
            <div v-if="findEntryById(slot.beerEntryId)" class="slot-entry">
              <strong>{{ displayShortCode(findEntryById(slot.beerEntryId)) }}</strong>
              <small>{{ entryMeta(findEntryById(slot.beerEntryId)) }}</small>
              <button v-if="canEdit" type="button" @click.stop="clearSlot(slot)">移除</button>
            </div>
            <div v-else class="slot-empty">
              <span>+</span>
              <strong>拖入酒款</strong>
            </div>
          </template>
          <template v-else>
            <select
              class="slot-select"
              :value="slot.beerEntryId || ''"
              :disabled="!canEdit"
              :aria-label="`${displaySlotLabel(slot)}选择酒款`"
              @change="selectSlotEntry(slot, $event.target.value)"
            >
              <option value="">选择酒款</option>
              <option v-for="entry in entries" :key="entry.id" :value="entry.id">
                {{ displayEntryOption(entry) }}
              </option>
            </select>
          </template>
        </article>
      </div>

      <p v-if="submitBlockedMessage" class="submit-blocked-note">{{ submitBlockedMessage }}</p>
      <button
        v-if="canSubmitFinal"
        :class="['button', 'primary', 'full', 'ranking-submit', { disabled: !canSubmit }]"
        type="button"
        :aria-disabled="!canSubmit"
        @click="openSubmitConfirm"
      >
        {{ submitButtonText }}
      </button>
      <button v-else-if="canEdit" class="button primary full ranking-submit" type="button" :disabled="savingDraft" @click="saveDraft">
        {{ savingDraft ? '保存中' : '保存我的参考排序' }}
      </button>
      <p v-else class="readonly-note">{{ readonlyText }}</p>
      <p v-if="message" class="message">{{ message }}</p>
    </section>

    <section class="card">
      <div class="split">
        <h2 class="section-title compact">候选酒款</h2>
        <span class="pill">{{ entries.length }} 款</span>
      </div>
      <div class="entry-list">
        <article
          v-for="entry in entries"
          :key="entry.id"
          :class="['entry-row', { dragging: draggedEntryId === entry.id, used: isEntryUsed(entry.id) }]"
          draggable="true"
          @dragstart="dragEntry(entry, $event)"
          @dragend="endDrag"
          @pointerdown="startPointerDrag(entry, $event)"
          @pointermove="movePointerDrag($event)"
          @pointerup="endPointerDrag($event)"
          @pointercancel="cancelPointerDrag"
          @click="handleEntryClick"
        >
          <strong>{{ displayShortCode(entry) }}</strong>
          <small>{{ entryMeta(entry) }}</small>
        </article>
      </div>
    </section>

    <section v-if="confirmOpen" class="ranking-confirm-overlay" role="dialog" aria-modal="true">
      <div class="ranking-confirm">
        <h2>{{ confirmTitle }}</h2>
        <div class="confirm-result-list">
          <div v-for="item in selectedResults" :key="item.rank">
            <span>{{ item.label }}</span>
            <strong>{{ item.shortCode }}</strong>
          </div>
        </div>
        <p v-if="submitError" class="confirm-error">{{ submitError }}</p>
        <div class="confirm-actions">
          <button class="button secondary" type="button" :disabled="submitting" @click="confirmOpen = false">
            继续调整
          </button>
          <button class="button primary" type="button" :disabled="submitting" @click="confirmSubmit">
            {{ submitting ? '提交中' : '确认提交' }}
          </button>
        </div>
      </div>
    </section>

    <section v-if="scannerOpen" class="ranking-scanner-overlay" role="dialog" aria-modal="true">
      <div class="scanner-shell">
        <div class="scanner-head">
          <button class="scanner-close" type="button" @click="stopSlotScanner">取消</button>
          <div>
            <p class="eyebrow">扫码指定</p>
            <h2>{{ activeSlot ? displaySlotLabel(activeSlot) : '排序槽位' }}</h2>
          </div>
        </div>
        <div class="scanner-panel">
          <div id="ranking-qr-reader" class="scanner-reader"></div>
          <p class="scanner-message">{{ scannerMessage }}</p>
        </div>
        <div class="scanner-manual">
          <input
            v-model.trim="manualCode"
            class="input"
            aria-label="输入酒款编号"
            placeholder="输入编号"
            @keyup.enter="assignManualCode"
          />
          <button class="button dark" type="button" @click="assignManualCode">确认</button>
        </div>
      </div>
    </section>

    <div v-if="dragGhost" class="drag-ghost" :style="{ left: `${dragGhost.x}px`, top: `${dragGhost.y}px` }">
      <strong>{{ displayShortCode(dragGhost.entry) }}</strong>
      <small>{{ entryMeta(dragGhost.entry) }}</small>
    </div>
  </main>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Html5Qrcode } from 'html5-qrcode'
import { fetchRoundTable, saveRankingDraft, submitRanking } from '@/api/judge'

const route = useRoute()
const router = useRouter()
const table = ref(null)
const entries = ref([])
const slots = ref([])
const message = ref('')
const submitBlockedMessage = ref('')
const scannerOpen = ref(false)
const scannerMessage = ref('将二维码放入取景框内')
const activeSlot = ref(null)
const manualCode = ref('')
const confirmOpen = ref(false)
const submitting = ref(false)
const savingDraft = ref(false)
const submitted = ref(false)
const draftSaved = ref(false)
const hasLocalChanges = ref(false)
const submitError = ref('')
const draggedEntryId = ref(null)
const activeDropRank = ref(null)
const pointerEntryId = ref(null)
const pointerStart = ref(null)
const pointerDragging = ref(false)
const dragGhost = ref(null)
const suppressEntryClick = ref(false)
let qrReader = null
let scanLocked = false

const canSubmitFinal = computed(() => Boolean(table.value?.canSubmitRanking))
const canEdit = computed(() => !['SUBMITTED', 'LOCKED'].includes(table.value?.status) && !submitting.value && !savingDraft.value)
const isMedalMode = computed(() => table.value?.targetMode === 'MEDALS')
const isDraggingEntry = computed(() => Boolean(draggedEntryId.value || pointerDragging.value))
const rankingConfirmation = computed(() => table.value?.rankingConfirmation || null)
const readyForFinalSubmit = computed(() => Boolean(rankingConfirmation.value?.readyForFinalSubmit))
const filledCount = computed(() => slots.value.filter((slot) => slot.beerEntryId).length)
const canSubmit = computed(() => {
  if (!canSubmitFinal.value || !canEdit.value || !slots.value.length) return false
  if (waitingForConfirmations.value) return false
  if (submitted.value && !hasLocalChanges.value) return false
  return isMedalMode.value ? filledCount.value > 0 : filledCount.value === slots.value.length
})
const waitingForConfirmations = computed(() => (
  canSubmitFinal.value
  && submitted.value
  && !readyForFinalSubmit.value
  && !hasLocalChanges.value
  && Number(rankingConfirmation.value?.requiredCount || 0) > 0
))
const submitBlockedReason = computed(() => {
  if (waitingForConfirmations.value) {
    return `同桌确认未完成，当前 ${rankingConfirmation.value?.confirmedCount || 0}/${rankingConfirmation.value?.requiredCount || 0}。`
  }
  if (readyForFinalSubmit.value && submitted.value && !hasLocalChanges.value) return '同桌确认已完成，系统将自动提交本桌排序。'
  if (!canEdit.value) return readonlyText.value
  if (submitted.value && !hasLocalChanges.value) return '排序已提交确认，等待同桌评审确认。'
  if (!slots.value.length) return '当前没有可提交的排序槽位。'
  if (isMedalMode.value && filledCount.value <= 0) return '请先选择至少一款酒。'
  if (!isMedalMode.value && filledCount.value < slots.value.length) return '请先补齐本桌排序。'
  return ''
})
const rankingStatusText = computed(() => {
  if (table.value?.status === 'LOCKED') return '已锁定'
  if (table.value?.status === 'SUBMITTED') return '已提交'
  if (canSubmitFinal.value && readyForFinalSubmit.value) return '确认已齐'
  if (canSubmitFinal.value && submitted.value) return `待确认 ${rankingConfirmation.value?.confirmedCount || 0}/${rankingConfirmation.value?.requiredCount || 0}`
  if (!canSubmitFinal.value && draftSaved.value) return '已保存'
  return `${filledCount.value}/${slots.value.length}`
})
const readonlyText = computed(() => (
  table.value?.status === 'LOCKED'
    ? '本桌排序已锁定。'
    : '当前不能调整排序。'
))
const selectedResults = computed(() => slots.value.map((slot) => {
  const entry = findEntryById(slot.beerEntryId)
  return {
    rank: slot.rank,
    label: displaySlotLabel(slot),
    shortCode: entry ? [entry.shortCode, entry.categoryName, entry.style].filter(Boolean).join(' · ') : '待选择',
  }
}))
const submitResults = computed(() => slots.value
  .filter((slot) => slot.beerEntryId)
  .map((slot) => ({
    beerEntryId: slot.beerEntryId,
    rankNo: slot.rank,
    slotLabel: slot.label,
  })))
const rankingModeText = computed(() => {
  const count = table.value?.targetCount || slots.value.length || 0
  if (canSubmitFinal.value) {
    if (table.value?.targetMode === 'MEDALS') return '组别决战：确认奖项，未设置的奖项可留空'
    if (table.value?.targetMode === 'CHAMPION') return '总冠军轮：确认全场总冠军'
    return `继续筛选：选择并排序前 ${count} 款`
  }
  return '记录自己的排序，仅供本人参考'
})
const submitButtonText = computed(() => {
  if (table.value?.status === 'SUBMITTED' && !hasLocalChanges.value) return '本桌排序已提交'
  if (readyForFinalSubmit.value && submitted.value && !hasLocalChanges.value) return '等待自动提交'
  if (waitingForConfirmations.value) return '等待同桌确认'
  return submitted.value ? '重新提交同桌确认' : '提交同桌确认'
})
const confirmTitle = computed(() => '提交同桌确认')

function displayShortCode(entry) {
  return entry?.shortCode || '编号'
}

function displaySlotLabel(slot) {
  if (!isMedalMode.value) return slot?.label || `第 ${slot?.rank || ''} 名`
  return ['第一名', '第二名', '第三名'][Number(slot?.rank) - 1] || slot?.label || '名次'
}

function displayEntryOption(entry) {
  return [displayShortCode(entry), entry?.categoryName, entry?.style].filter(Boolean).join(' · ')
}

function entryMeta(entry) {
  return [entry?.categoryName, entry?.style].filter(Boolean).join(' · ') || '-'
}

function findEntryById(id) {
  return entries.value.find((entry) => String(entry.id) === String(id)) || null
}

function isEntryUsed(entryId) {
  return slots.value.some((slot) => String(slot.beerEntryId) === String(entryId))
}

function handleEntryClick(event) {
  if (!suppressEntryClick.value) return
  event.preventDefault()
  event.stopPropagation()
  suppressEntryClick.value = false
}

function dragEntry(entry, event) {
  if (!canEdit.value) return
  draggedEntryId.value = entry.id
  if (event?.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(entry.id))
  }
}

function endDrag() {
  draggedEntryId.value = null
  activeDropRank.value = null
}

function dropEntryOnSlot(slot, event) {
  const droppedEntryId = draggedEntryId.value || event?.dataTransfer?.getData('text/plain')
  if (!canEdit.value || !droppedEntryId) return
  assignEntryToSlot(droppedEntryId, slot)
  endDrag()
}

function startPointerDrag(entry, event) {
  if (!canEdit.value || event.pointerType === 'mouse') return
  pointerEntryId.value = entry.id
  pointerStart.value = { x: event.clientX, y: event.clientY }
  event.currentTarget?.setPointerCapture?.(event.pointerId)
}

function movePointerDrag(event) {
  if (!pointerEntryId.value || !pointerStart.value) return
  const moved = Math.abs(event.clientX - pointerStart.value.x) + Math.abs(event.clientY - pointerStart.value.y)
  if (moved < 10) return
  event.preventDefault()
  pointerDragging.value = true
  draggedEntryId.value = pointerEntryId.value
  suppressEntryClick.value = true
  const entry = findEntryById(pointerEntryId.value)
  dragGhost.value = entry ? { entry, x: event.clientX, y: event.clientY } : null
  activeDropRank.value = findSlotByPoint(event.clientX, event.clientY)?.rank || null
}

function endPointerDrag(event) {
  if (pointerDragging.value && draggedEntryId.value) {
    const slot = findSlotByPoint(event.clientX, event.clientY)
    if (slot) assignEntryToSlot(draggedEntryId.value, slot)
  }
  event.currentTarget?.releasePointerCapture?.(event.pointerId)
  cancelPointerDrag()
}

function cancelPointerDrag() {
  pointerEntryId.value = null
  pointerStart.value = null
  pointerDragging.value = false
  draggedEntryId.value = null
  activeDropRank.value = null
  dragGhost.value = null
}

function findSlotByPoint(x, y) {
  const target = document.elementFromPoint(x, y)?.closest?.('[data-rank]')
  return target ? slots.value.find((item) => item.rank === Number(target.dataset.rank)) : null
}

function selectSlotEntry(slot, value) {
  if (!canEdit.value) return
  if (!value) {
    clearSlot(slot)
    return
  }
  const entry = findEntryById(value)
  if (!entry) return
  assignEntryToSlot(entry.id, slot)
}

function assignEntryToSlot(entryId, targetSlot) {
  submitBlockedMessage.value = ''
  const sourceSlot = slots.value.find((slot) => String(slot.beerEntryId) === String(entryId))
  const replacedEntryId = targetSlot.beerEntryId
  if (sourceSlot && sourceSlot.rank !== targetSlot.rank) {
    sourceSlot.beerEntryId = replacedEntryId || null
  }
  targetSlot.beerEntryId = entryId
  draftSaved.value = false
  hasLocalChanges.value = true
  if (canSubmitFinal.value) {
    submitted.value = false
    table.value = { ...table.value, status: 'IN_PROGRESS', rankingConfirmation: { ...rankingConfirmation.value, readyForFinalSubmit: false } }
  }
}

function clearSlot(slot) {
  submitBlockedMessage.value = ''
  slot.beerEntryId = null
  draftSaved.value = false
  hasLocalChanges.value = true
  if (canSubmitFinal.value) {
    submitted.value = false
    table.value = { ...table.value, status: 'IN_PROGRESS', rankingConfirmation: { ...rankingConfirmation.value, readyForFinalSubmit: false } }
  }
}

async function openSlotScanner(slot) {
  if (!canEdit.value) return
  activeSlot.value = slot
  manualCode.value = ''
  scannerMessage.value = '将二维码放入取景框内'
  scannerOpen.value = true
  scanLocked = false
  await nextTick()
  await startSlotScanner()
}

async function startSlotScanner() {
  try {
    qrReader = new Html5Qrcode('ranking-qr-reader')
    await qrReader.start(
      { facingMode: 'environment' },
      { fps: 10, qrbox: { width: 220, height: 220 } },
      (decodedText) => {
        if (scanLocked) return
        scanLocked = true
        if (!assignCodeToActiveSlot(decodedText)) {
          scanLocked = false
        }
      },
    )
  } catch {
    scannerMessage.value = '无法打开摄像头，可手动输入编号。'
  }
}

async function stopSlotScanner() {
  scannerOpen.value = false
  activeSlot.value = null
  manualCode.value = ''
  if (!qrReader) return
  try {
    if (qrReader.isScanning) {
      await qrReader.stop()
    }
    await qrReader.clear()
  } catch {
    // 关闭相机失败不影响继续排序。
  } finally {
    qrReader = null
  }
}

function assignManualCode() {
  assignCodeToActiveSlot(manualCode.value)
}

function assignCodeToActiveSlot(value) {
  const slot = activeSlot.value
  if (!slot) return false
  const code = normalizeScanCode(value)
  if (!code) {
    scannerMessage.value = '请输入酒款编号。'
    return false
  }
  const entry = findEntryByCode(code)
  if (!entry) {
    scannerMessage.value = '这款酒不在本桌候选中。'
    return false
  }
  assignEntryToSlot(entry.id, slot)
  message.value = `${displaySlotLabel(slot)}已选 ${entry.shortCode || '该酒款'}`
  stopSlotScanner()
  return true
}

function findEntryByCode(code) {
  const normalized = String(code || '').trim().toUpperCase()
  return entries.value.find((entry) => {
    const candidates = [
      entry.shortCode,
      entry.uuid,
      entry.publicId,
      entry.id,
    ].map((item) => String(item || '').trim().toUpperCase()).filter(Boolean)
    return candidates.includes(normalized)
  })
}

function normalizeScanCode(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  try {
    const url = new URL(text)
    const parts = url.pathname.split('/').filter(Boolean)
    if (parts[0] === 'q' && parts[1]) {
      return decodeURIComponent(parts[1]).toUpperCase()
    }
  } catch {
    // 普通编号直接匹配候选酒款。
  }
  return text.toUpperCase()
}

function openSubmitConfirm() {
  if (!canSubmit.value) {
    submitBlockedMessage.value = submitBlockedReason.value
    return
  }
  submitBlockedMessage.value = ''
  submitError.value = ''
  confirmOpen.value = true
}

async function confirmSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await submitRanking(route.params.roundTableId, {
      results: submitResults.value,
    })
    message.value = '排序已提交，等待同桌评审确认'
    confirmOpen.value = false
    submitted.value = true
    await loadTable()
  } catch {
    submitError.value = '提交失败，请稍后重试或联系现场工作人员。'
  } finally {
    submitting.value = false
  }
}

async function saveDraft() {
  if (!canEdit.value || savingDraft.value) return
  savingDraft.value = true
  try {
    await saveRankingDraft(route.params.roundTableId, {
      results: submitResults.value,
    })
    draftSaved.value = true
    message.value = '我的参考排序已保存'
  } catch {
    message.value = '保存失败，请稍后重试。'
  } finally {
    savingDraft.value = false
  }
}

async function loadTable() {
  table.value = await fetchRoundTable(route.params.roundTableId)
  submitBlockedMessage.value = ''
  entries.value = table.value.entries || []
  const sourceSlots = table.value.canSubmitRanking || table.value?.status === 'LOCKED'
    ? table.value.rankings
    : table.value.myRankingDraft
  slots.value = (sourceSlots || table.value.rankings || []).map((slot) => ({
    ...slot,
    beerEntryId: slot.beerEntryId || null,
  }))
  hasLocalChanges.value = false
  submitted.value = table.value.canSubmitRanking
    && (['SUBMITTED', 'LOCKED'].includes(table.value?.status) || slots.value.some((slot) => slot.beerEntryId))
  draftSaved.value = !table.value.canSubmitRanking && slots.value.some((slot) => slot.beerEntryId)
}

onMounted(async () => {
  await loadTable()
})

onBeforeUnmount(() => {
  stopSlotScanner()
})
</script>

<style scoped>
.back-link {
  justify-self: start;
  display: inline-flex;
  gap: 7px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  margin: 0 0 14px;
  padding: 7px 11px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.08);
  font-size: 13px;
  font-weight: 750;
  line-height: 1;
}

.back-icon {
  width: 8px;
  height: 8px;
  border: solid currentColor;
  border-width: 0 0 2px 2px;
  transform: rotate(45deg);
}

.compact {
  margin-bottom: 0;
}

.slot-list,
.entry-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.slot-drop {
  display: grid;
  gap: 9px;
  min-height: 78px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  color: #344054;
  background: #fff;
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}

.slot-drop.dragging {
  min-height: 104px;
  border-style: dashed;
  background: #f8fafc;
}

.slot-drop.active {
  border-color: #f3c04f;
  background: #fffaf0;
  box-shadow: 0 0 0 3px rgba(243, 192, 79, 0.18);
}

.slot-drop.filled {
  border-style: solid;
  background: #fff;
}

.slot-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  color: #9a5b26;
  font-size: 14px;
  font-weight: 900;
}

.slot-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.slot-empty {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 4px;
  min-height: 48px;
  color: #98a2b3;
}

.slot-empty span {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  color: #9a5b26;
  background: #fff2cf;
  font-size: 22px;
  font-weight: 900;
}

.slot-entry {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 10px;
  align-items: center;
}

.slot-entry strong,
.slot-entry small {
  min-width: 0;
  overflow-wrap: anywhere;
}

.slot-entry small {
  color: #667085;
}

.slot-entry button {
  grid-row: span 2;
  border: 0;
  border-radius: 999px;
  padding: 6px 9px;
  color: #9a3412;
  background: #fff2e8;
  font-weight: 850;
}

.slot-select {
  width: 100%;
  min-height: 42px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  padding: 0 38px 0 11px;
  color: #18222f;
  background:
    linear-gradient(45deg, transparent 50%, #667085 50%) calc(100% - 19px) 18px / 6px 6px no-repeat,
    linear-gradient(135deg, #667085 50%, transparent 50%) calc(100% - 14px) 18px / 6px 6px no-repeat,
    #fff;
  font-size: 15px;
  font-weight: 750;
  line-height: 1.2;
  appearance: none;
}

.slot-select:focus {
  border-color: #d89a35;
  outline: 3px solid rgba(216, 154, 53, 0.18);
}

.slot-select:disabled {
  color: #98a2b3;
  background-color: #f9fafb;
}

.slot-clear-button {
  border: 0;
  border-radius: 999px;
  min-height: 28px;
  padding: 0 9px;
  color: #9a3412;
  background: #fff2e8;
  font-size: 12px;
  font-weight: 850;
  line-height: 1;
}

.slot-scan-button {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  color: #344054;
  background: #fff;
}

.slot-scan-button svg {
  width: 20px;
  height: 20px;
  fill: currentColor;
}

.ranking-submit {
  margin-top: 16px;
}

.ranking-submit.disabled {
  color: #98a2b3;
  background: #e4e7ec;
  box-shadow: none;
}

.submit-blocked-note {
  margin: 14px 0 -4px;
  border: 1px solid #fedf89;
  border-radius: 8px;
  padding: 10px 12px;
  color: #93370d;
  background: #fffaeb;
  text-align: center;
  font-size: 13px;
  font-weight: 750;
}

.ranking-mode,
.readonly-note {
  margin: 8px 0 0;
  color: rgba(248, 250, 252, 0.76);
  font-size: 13px;
  font-weight: 750;
}

.readonly-note {
  color: #667085;
}

.entry-row {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 12px;
  color: #18222f;
  background: #fff;
  touch-action: none;
  user-select: none;
  transition: border-color 0.18s ease, background-color 0.18s ease, transform 0.18s ease;
}

.entry-row strong,
.entry-row small {
  display: block;
}

.entry-row small {
  margin-top: 5px;
  color: #667085;
}

.entry-row.dragging {
  border-color: #f3c04f;
  background: #fffaf0;
  transform: scale(0.99);
}

.entry-row.used {
  color: #667085;
  background: #f9fafb;
}

.drag-ghost {
  position: fixed;
  z-index: 80;
  width: min(280px, calc(100vw - 40px));
  pointer-events: none;
  transform: translate(-50%, -50%);
  border: 1px solid #f3c04f;
  border-radius: 8px;
  padding: 10px 12px;
  color: #18222f;
  background: #fffaf0;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.18);
}

.drag-ghost strong,
.drag-ghost small {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
}

.drag-ghost small {
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
}

.message {
  margin: 12px 0 0;
  color: #067647;
  text-align: center;
  font-size: 14px;
  font-weight: 750;
}

.ranking-confirm-overlay,
.ranking-scanner-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
}

.ranking-confirm-overlay {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 18px;
  background: rgba(16, 24, 32, 0.54);
}

.ranking-confirm {
  display: grid;
  gap: 14px;
  width: min(100%, 430px);
  border-radius: 12px;
  padding: 18px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.22);
}

.ranking-confirm h2,
.ranking-confirm p {
  margin: 0;
}

.ranking-confirm h2 {
  color: #18222f;
  font-size: 20px;
  line-height: 1.25;
}

.ranking-confirm .confirm-error {
  border-radius: 8px;
  padding: 9px 10px;
  color: #b42318;
  background: #fef3f2;
  font-weight: 750;
}

.confirm-result-list {
  display: grid;
  gap: 8px;
}

.confirm-result-list div {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  padding: 9px 10px;
  background: #f8faf9;
}

.confirm-result-list span {
  color: #667085;
  font-size: 13px;
  font-weight: 800;
}

.confirm-result-list strong {
  min-width: 0;
  color: #18222f;
  font-size: 14px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.confirm-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.confirm-actions .button {
  min-height: 44px;
}

.ranking-scanner-overlay {
  display: flex;
  justify-content: center;
  align-items: stretch;
  overflow: hidden;
  color: #fff;
  background: #101820;
}

.scanner-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 16px;
  width: min(100%, 500px);
  height: 100dvh;
  max-height: 960px;
  padding: 18px;
  color: #fff;
  background: #101820;
  overflow: hidden;
}

.scanner-head {
  display: flex;
  gap: 14px;
  justify-content: space-between;
  align-items: flex-start;
}

.scanner-head h2,
.scanner-head p {
  margin: 0;
  text-align: right;
}

.scanner-head h2 {
  margin-top: 4px;
  font-size: 24px;
}

.scanner-close {
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  padding: 8px 12px;
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
  font-weight: 850;
}

.scanner-panel {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

.scanner-reader {
  position: relative;
  overflow: hidden;
  width: min(100%, 460px, max(240px, calc(100dvh - 220px)));
  aspect-ratio: 1 / 1;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 12px;
  background: #05070a;
}

.scanner-reader :deep(div) {
  border: 0 !important;
}

.scanner-reader :deep(#ranking-qr-reader__scan_region) {
  position: absolute;
  inset: 0;
  width: 100% !important;
  height: 100% !important;
}

.scanner-reader :deep(video) {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

.scanner-reader :deep(canvas),
.scanner-reader :deep(img) {
  max-width: 100%;
}

.scanner-message {
  min-height: 20px;
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  text-align: center;
  font-size: 14px;
  font-weight: 750;
}

.scanner-manual {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding-bottom: max(4px, env(safe-area-inset-bottom));
}

.scanner-manual .input,
.scanner-manual .button {
  min-height: 46px;
}

.scanner-manual .button {
  min-width: 74px;
}

@media (max-width: 380px) {
  .confirm-actions {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
