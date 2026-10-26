<template>
  <main ref="pageEl" class="app-shell recruitment-page">
    <section class="top-panel hero-panel"><h1 class="page-title">报名参加评审</h1></section>
    <div class="tabs" role="tablist" aria-label="招募列表"><button :class="{ active: tab === 'open' }" role="tab" :aria-selected="tab === 'open'" @click="tab = 'open'">可报名 <span v-if="openCount" class="tab-count">{{ openCount }}</span></button><button :class="{ active: tab === 'mine' }" role="tab" :aria-selected="tab === 'mine'" @click="tab = 'mine'">我的申请 <span v-if="mineCount" class="tab-count">{{ mineCount }}</span></button></div>
    <section v-for="item in list" :key="item.id" class="recruit-card">
      <div v-if="tab === 'mine'" class="card-topline"><span class="status" :class="statusClass(item.myApplicationStatus)">{{ labels[item.myApplicationStatus] || item.myApplicationStatus }}</span></div>
      <div class="card-head"><h2>{{ item.competitionName }}</h2><button class="detail-link" type="button" :aria-label="`查看${item.competitionName}详情`" @click="openCompetition(item)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg></button></div>
      <div class="meta-grid"><span class="meta-item"><i class="meta-icon">◷</i>{{ formatRecruitmentSchedule(item) }}</span><span class="meta-item venue"><i class="meta-icon">⌖</i><span class="venue-text">{{ item.venue || '地点待定' }}</span></span></div>
      <div v-if="item.description" class="card-clamp"><p :data-desc-id="item.id" :class="['description', { clamped: !expandedDescriptions.has(item.id) }]">{{ item.description }}</p><button v-if="overflowDescriptions.has(item.id)" class="text-toggle" type="button" :aria-expanded="expandedDescriptions.has(item.id)" :aria-label="expandedDescriptions.has(item.id) ? '收起赛事简介' : '展开赛事简介'" @click="toggleDescription(item.id)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg></button></div>
      <p v-if="tab === 'mine' && item.myReviewRemark" class="review-note">审核意见：{{ item.myReviewRemark }}</p>
      <div v-if="tab === 'mine' && item.myNote" class="card-note"><span class="note-label">我的补充</span><p :data-note-id="item.id" :class="['note-text', { clamped: !expandedNotes.has(item.id) }]">{{ item.myNote }}</p><button v-if="overflowNotes.has(item.id)" class="text-toggle" type="button" :aria-expanded="expandedNotes.has(item.id)" :aria-label="expandedNotes.has(item.id) ? '收起我的补充' : '展开我的补充'" @click="toggleNote(item.id)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg></button></div>
      <div class="card-actions"><button v-if="tab === 'open' && !item.myApplicationStatus" class="button primary action-main" @click="openApply(item)">报名参加</button><span v-else-if="tab === 'open'" class="applied-hint">{{ labels[item.myApplicationStatus] || '已提交申请' }}</span><template v-if="tab === 'mine'"><button v-if="canReapply(item)" class="button primary action-main" @click="openEdit(item)">重新报名</button><button v-else-if="canEdit(item)" class="button primary action-main" @click="openEdit(item)">{{ item.myApplicationStatus === 'REJECTED' ? '补充资料后提交' : '编辑申请' }}</button><button v-else-if="canWithdraw(item)" class="quiet-danger" @click="withdraw(item)">撤回报名</button></template></div>
    </section>
    <div v-if="!list.length" class="empty-state"><span class="empty-icon">✦</span><p>{{ tab === 'mine' ? '还没有提交过申请' : '暂时没有开放中的招募' }}</p></div>
    <div v-if="selected" class="sheet-backdrop" @click.self="closeSheet"><section class="sheet" role="dialog" aria-modal="true" :aria-label="isReapply ? '重新报名' : editing ? '编辑申请' : '提交报名'"><button class="sheet-close" type="button" aria-label="关闭" @click="closeSheet">×</button><h2>{{ selected.competitionName }}</h2><p class="sheet-meta">{{ formatRecruitmentSchedule(selected) }} · {{ selected.venue || '地点待定' }}</p><div v-if="selected.requirements" class="sheet-requirement"><span>报名说明</span><p>{{ selected.requirements }}</p></div><label class="confirm-row"><input v-model="confirmed" type="checkbox"><span>我确认可以参加比赛日期</span></label><label class="field">补充说明 <span>选填</span><textarea v-model="note" class="textarea" maxlength="500" placeholder="可填写擅长风格、时间安排等信息" /></label><button class="button primary full submit-button" :disabled="!confirmed || submitting" @click="submitApplication">{{ submitting ? '提交中…' : isReapply ? '重新提交报名' : editing ? '保存并重新提交' : '提交报名' }}</button><button v-if="canWithdraw(selected)" class="quiet-danger sheet-withdraw" type="button" @click="withdraw(selected)">撤回报名</button></section></div>
    <JudgeBottomNav :active="'recruitments'" />
  </main>
</template>
<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import JudgeBottomNav from '@/components/JudgeBottomNav.vue'
import { formatRecruitmentSchedule } from '@/utils/recruitmentSchedule'
import { PORTAL_BASE_URL } from '@/config'
import { applyRecruitment, fetchMyRecruitmentApplications, fetchRecruitments, updateRecruitmentApplication, withdrawRecruitmentApplication } from '@/api/judge'
const tab = ref('open'), list = ref([]), selected = ref(null), editing = ref(false), confirmed = ref(false), note = ref(''), submitting = ref(false)
const pageEl = ref(null)
const expandedDescriptions = ref(new Set()), overflowDescriptions = ref(new Set())
const expandedNotes = ref(new Set()), overflowNotes = ref(new Set())
const isReapply = computed(() => selected.value?.myApplicationStatus === 'WITHDRAWN')
const labels = { APPLIED: '待审核', ACCEPTED: '已录用', REJECTED: '需补充', WITHDRAWN: '已撤回' }
const openCount = computed(() => tab.value === 'open' ? list.value.length : 0), mineCount = computed(() => tab.value === 'mine' ? list.value.length : 0)
async function load() { list.value = tab.value === 'open' ? await fetchRecruitments() : await fetchMyRecruitmentApplications() }
function openCompetition(item) { if (item.competitionId) window.location.href = `${PORTAL_BASE_URL}/portal/events/${item.competitionId}` }
function statusClass(status) { return { APPLIED: 'status-pending', ACCEPTED: 'status-accepted', REJECTED: 'status-rejected', WITHDRAWN: 'status-withdrawn' }[status] || '' }
function applicationWindowOpen(item) {
  if (typeof item?.applicationWindowOpen === 'boolean') return item.applicationWindowOpen
  if (item?.status !== 'OPEN') return false
  if (!item?.recruitmentDeadline) return true
  const deadline = new Date(item.recruitmentDeadline).getTime()
  return Number.isNaN(deadline) || deadline >= Date.now()
}
function canEdit(item) { return applicationWindowOpen(item) && ['APPLIED', 'REJECTED'].includes(item.myApplicationStatus) }
function canWithdraw(item) { return applicationWindowOpen(item) && ['APPLIED', 'ACCEPTED'].includes(item.myApplicationStatus) }
function canReapply(item) { return applicationWindowOpen(item) && item.myApplicationStatus === 'WITHDRAWN' }
function toggleExpanded(target, key) { const next = new Set(target.value); if (next.has(key)) next.delete(key); else next.add(key); target.value = next }
function toggleDescription(id) { toggleExpanded(expandedDescriptions, id) }
function toggleNote(id) { toggleExpanded(expandedNotes, id) }
function measureClamped(nodes, keyName, lines) {
  const next = new Set()
  nodes.forEach((el) => {
    const key = Number(el.dataset[keyName])
    if (!Number.isFinite(key)) return
    el.style.display = '-webkit-box'
    el.style.webkitBoxOrient = 'vertical'
    el.style.webkitLineClamp = String(lines)
    el.style.overflow = 'hidden'
    const clampedHeight = el.getBoundingClientRect().height
    el.style.display = 'block'
    el.style.webkitLineClamp = 'unset'
    el.style.overflow = 'visible'
    const fullHeight = el.getBoundingClientRect().height
    el.style.display = ''
    el.style.webkitBoxOrient = ''
    el.style.webkitLineClamp = ''
    el.style.overflow = ''
    if (fullHeight > clampedHeight + 1) next.add(key)
  })
  return next
}
function pruneExpanded(expanded, overflow) {
  const next = new Set([...expanded.value].filter((key) => overflow.value.has(key)))
  if (next.size !== expanded.value.size) expanded.value = next
}
function measureCardOverflow() {
  nextTick(() => {
    const root = pageEl.value
    if (!root) return
    overflowDescriptions.value = measureClamped(root.querySelectorAll('p[data-desc-id]'), 'descId', 2)
    overflowNotes.value = measureClamped(root.querySelectorAll('p[data-note-id]'), 'noteId', 1)
    pruneExpanded(expandedDescriptions, overflowDescriptions)
    pruneExpanded(expandedNotes, overflowNotes)
  })
}
function openApply(item) { selected.value = item; editing.value = false; confirmed.value = false; note.value = '' }
function openEdit(item) { selected.value = item; editing.value = item.myApplicationStatus !== 'WITHDRAWN'; confirmed.value = Boolean(item.myAvailabilityConfirmed); note.value = item.myNote || '' }
function closeSheet() { selected.value = null; editing.value = false; submitting.value = false }
async function submitApplication() { if (!selected.value || submitting.value) return; submitting.value = true; try { const payload = { availabilityConfirmed: confirmed.value, note: note.value.trim() }; if (editing.value) await updateRecruitmentApplication(selected.value.myApplicationId, payload); else await applyRecruitment(selected.value.id, payload); closeSheet(); await load() } catch (error) { window.alert(error?.response?.data?.message || error?.message || '提交失败，请稍后重试') } finally { submitting.value = false } }
async function withdraw(item) { if (!window.confirm('取消后将退出本场比赛的评审候选，确认取消报名吗？')) return; try { await withdrawRecruitmentApplication(item.myApplicationId); closeSheet(); await load() } catch (error) { window.alert(error?.response?.data?.message || error?.message || '操作失败，请稍后重试') } }
watch(list, measureCardOverflow, { flush: 'post' })
onMounted(() => { load(); window.addEventListener('resize', measureCardOverflow) })
onUnmounted(() => window.removeEventListener('resize', measureCardOverflow))
watch(tab, load)
</script>
<style scoped>
.recruitment-page{padding-top:14px}.hero-panel{position:relative;overflow:hidden;padding:19px 18px 17px;background:#17242b}.hero-panel:after{position:absolute;right:-30px;bottom:-58px;width:150px;height:150px;border:1px solid rgba(215,132,52,.35);border-radius:50%;content:''}.hero-mark{margin-bottom:10px;color:#d7873a;font-size:10px;font-weight:800;letter-spacing:1.8px}.hero-rule{width:38px;height:3px;margin-top:15px;background:#d7873a}.tabs{display:grid;grid-template-columns:1fr 1fr;gap:4px;padding:4px;border-radius:9px;background:#dde5e1}.tabs button{min-height:42px;border:0;border-radius:6px;color:#52605d;background:transparent;font-size:15px;font-weight:650}.tabs button.active{color:#17242b;background:#fff;box-shadow:0 2px 5px rgba(23,36,43,.09)}.tab-count{display:inline-grid;min-width:18px;min-height:18px;margin-left:3px;place-items:center;border-radius:9px;color:#fff;background:#d17932;font-size:11px}.recruit-card{display:grid;gap:10px;padding:16px 16px 14px;border:1px solid rgba(23,36,43,.1);border-radius:9px;background:#fff;box-shadow:0 8px 20px rgba(23,36,43,.065)}.card-topline{display:flex;align-items:center;justify-content:flex-end}.card-head{display:flex;align-items:flex-start;gap:8px}.recruit-card h2{flex:1;min-width:0;margin:0;color:#1c2c35;font-size:19px;line-height:1.3}.detail-link{display:grid;flex:none;place-items:center;width:36px;height:36px;margin:-8px -8px -4px 0;border:0;border-radius:50%;color:#8a9591;background:transparent;-webkit-tap-highlight-color:transparent}.detail-link svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round}.meta-grid{display:flex;align-items:center;gap:12px;min-width:0;color:#667774;font-size:13px}.meta-item{display:flex;align-items:center;flex:none;white-space:nowrap}.meta-item.venue{flex:1;min-width:0}.venue-text{min-width:0;overflow:hidden;text-overflow:ellipsis}.meta-icon{flex:none;margin-right:5px;color:#c16d2c;font-style:normal;font-size:15px}.card-clamp{display:flex;align-items:flex-start;gap:8px}.description,.note-text{flex:1;min-width:0;margin:0;color:#52605d;font-size:13px;line-height:1.55}.description.clamped,.note-text.clamped{display:-webkit-box;overflow:hidden;-webkit-box-orient:vertical}.description.clamped{-webkit-line-clamp:2}.note-text.clamped{-webkit-line-clamp:1}.text-toggle{display:grid;flex:none;place-items:center;margin:-7px -6px -7px 0;padding:7px 6px;border:0;color:#8a9591;background:transparent;-webkit-tap-highlight-color:transparent}.text-toggle svg{width:18px;height:18px;fill:none;stroke:currentColor;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round;transition:transform .18s ease}.text-toggle[aria-expanded="true"] svg{transform:rotate(90deg)}.review-note{margin:0;color:#a13d2d;font-size:13px;line-height:1.55}.card-note{display:flex;align-items:flex-start;gap:8px;padding-top:10px;border-top:1px solid #e5e9e6}.note-label{flex:none;padding-top:2px;color:#9a5b29;font-size:11px;font-weight:800;letter-spacing:.7px}.status{padding:4px 9px;border:1px solid;border-radius:999px;font-size:12px;font-weight:750}.status-pending{color:#965815;border-color:#f1d3aa;background:#fff8ed}.status-accepted{color:#13704c;border-color:#a8dec7;background:#eefaf4}.status-rejected{color:#a13d2d;border-color:#efc1b7;background:#fff4f1}.status-withdrawn{color:#667085;border-color:#d5dadd;background:#f5f6f6}.card-actions{display:flex;align-items:center;justify-content:flex-end;gap:10px;margin-top:2px}.action-main{min-height:42px;padding:10px 13px;font-size:14px}.applied-hint{color:#667774;font-size:13px}.quiet-danger{border:0;padding:9px 0;color:#a13d2d;background:transparent;font-size:12px;text-decoration:underline;text-underline-offset:3px}.empty-state{padding:56px 0 30px;text-align:center;color:#7a8581}.empty-state p{margin:12px 0;font-size:14px}.empty-icon{color:#d17932;font-size:25px}.sheet-backdrop{position:fixed;inset:0;z-index:30;display:flex;align-items:flex-end;justify-content:center;background:rgba(16,26,30,.38)}.sheet{position:relative;width:min(520px,100%);max-height:90vh;overflow:auto;padding:23px 18px calc(22px + env(safe-area-inset-bottom));border-radius:16px 16px 0 0;background:#fff;box-shadow:0 -14px 35px rgba(23,36,43,.2)}.sheet-close{position:absolute;top:12px;right:15px;width:34px;height:34px;border:0;border-radius:50%;color:#52605d;background:#f0f3f1;font-size:24px;line-height:1}.sheet-kicker{margin-bottom:8px;color:#b56b2d;font-size:10px;font-weight:800;letter-spacing:1.5px}.sheet h2{margin:0;color:#1c2c35;font-size:21px}.sheet-meta{margin:7px 0 17px;color:#667774;font-size:13px}.sheet-requirement{margin-bottom:16px;padding:10px 11px;border-radius:5px;background:#f6f8f5}.sheet-requirement p{margin:0;color:#52605d;font-size:13px;line-height:1.5}.confirm-row{display:flex;align-items:center;gap:9px;margin:14px 0 17px;color:#263b42;font-size:14px;font-weight:700}.confirm-row input{width:18px;height:18px;accent-color:#b76325}.field{display:block;color:#263b42;font-size:14px;font-weight:700}.field span{margin-left:5px;color:#89938f;font-size:12px;font-weight:500}.textarea{min-height:100px;margin-top:8px}.submit-button{margin-top:6px}.sheet-withdraw{display:block;margin:14px auto 0}
</style>
