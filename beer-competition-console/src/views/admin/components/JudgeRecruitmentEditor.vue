<template>
  <el-dialog :model-value="modelValue" :title="recruitment ? '编辑裁判招募' : '发起裁判招募'"
    width="600px" class="recruitment-editor" :close-on-click-modal="false" :close-on-press-escape="!saving"
    :show-close="!saving" @update:model-value="close">
    <form class="recruitment-form" @submit.prevent="save">
      <label>比赛
        <input v-if="recruitment" :value="recruitment.competitionName" readonly>
        <el-select v-else v-model="form.competitionId" filterable placeholder="选择比赛" @change="selectCompetition">
          <el-option v-for="item in competitions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </label>
      <div class="schedule-grid">
        <label>比赛日期<input :value="competitionDate || '日期待定'" readonly></label>
        <label>评审开始时间<input v-model="form.judgingStartTime" type="time" required></label>
        <label>招募开始<input v-model="form.recruitmentStart" type="datetime-local" required></label>
        <label>招募截止<input v-model="form.recruitmentDeadline" type="datetime-local" required></label>
      </div>
      <label>评审地点<input v-model.trim="form.venue" maxlength="255" required></label>
      <label>详细地址<input v-model.trim="form.address" maxlength="500"></label>
      <label>赛事介绍<textarea v-model="form.description" rows="3"></textarea></label>
      <label>裁判要求<textarea v-model="form.requirements" rows="3"></textarea></label>
      <label>预计需求人数<input v-model.number="form.expectedCount" type="number" min="1" max="999" required></label>
      <footer><button class="tool-button" type="button" :disabled="saving" @click="close(false)">取消</button>
        <button class="tool-button primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : recruitment && recruitment.status !== 'DRAFT' ? '保存' : '保存草稿' }}</button>
      </footer>
    </form>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createJudgeRecruitment, fetchCompetitions, updateJudgeRecruitment } from '@/api/admin'

const props = defineProps({ modelValue: Boolean, recruitment: { type: Object, default: null } })
const emit = defineEmits(['update:modelValue', 'saved'])
const competitions = ref([]), saving = ref(false)
const form = reactive({ competitionId: null, judgingStartTime: '', recruitmentStart: '', recruitmentDeadline: '', venue: '', address: '', description: '', requirements: '', expectedCount: 1 })
const selectedCompetition = computed(() => competitions.value.find(item => String(item.id) === String(form.competitionId)))
const competitionDate = computed(() => props.recruitment?.competitionDate || selectedCompetition.value?.competitionDate || selectedCompetition.value?.date || '')
const inputDateTime = value => value ? String(value).replace(' ', 'T').slice(0, 16) : ''

function defaultDeadline(value) {
  if (!value) return ''
  const date = new Date(`${String(value).slice(0, 10)}T00:00:00`)
  date.setDate(date.getDate() - 1)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T23:59`
}

function selectCompetition() {
  const item = selectedCompetition.value
  form.recruitmentStart = inputDateTime(item?.registrationStart)
  form.recruitmentDeadline = defaultDeadline(item?.competitionDate || item?.date)
  form.venue = item?.venue || item?.location || item?.logistics?.venue || item?.logistics?.deliveryAddress || item?.deliveryAddress || ''
  form.address = item?.deliveryAddress || ''
  form.description = item?.description || ''
}

watch(() => props.modelValue, async visible => {
  if (!visible) return
  const item = props.recruitment
  Object.assign(form, {
    competitionId: item?.competitionId ?? null, judgingStartTime: item?.judgingStartTime?.slice(0, 5) || '',
    recruitmentStart: inputDateTime(item?.recruitmentStart), recruitmentDeadline: inputDateTime(item?.recruitmentDeadline),
    venue: item?.venue || '', address: item?.address || '', description: item?.description || '',
    requirements: item?.requirements || '', expectedCount: item?.expectedCount ?? 1,
  })
  if (!item) {
    try {
      competitions.value = await fetchCompetitions({})
      if (!props.modelValue || props.recruitment) return
      form.competitionId = competitions.value[0]?.id ?? null
      selectCompetition()
    } catch { /* 请求层已经展示加载失败信息。 */ }
  }
})

function close(visible) { if (!saving.value) emit('update:modelValue', visible) }
async function save() {
  if (saving.value) return
  if (!form.competitionId || !form.venue.trim() || !form.recruitmentStart || !form.recruitmentDeadline || !form.judgingStartTime) {
    ElMessage.warning('请填写比赛、评审时间、招募时间和评审地点')
    return
  }
  if (form.recruitmentDeadline < form.recruitmentStart) { ElMessage.warning('截止时间不能早于开始时间'); return }
  if (!Number.isInteger(form.expectedCount) || form.expectedCount < 1 || form.expectedCount > 999) {
    ElMessage.warning('预计需求人数应为 1 至 999 的整数'); return
  }
  saving.value = true
  try {
    const payload = { ...form, venue: form.venue.trim(), judgingStartTime: `${form.judgingStartTime.slice(0, 5)}:00` }
    const result = props.recruitment ? await updateJudgeRecruitment(props.recruitment.id, payload) : await createJudgeRecruitment(payload)
    emit('update:modelValue', false)
    emit('saved', result)
    ElMessage.success('已保存')
  } catch { /* 请求层已经展示保存失败信息，保留表单供修改。 */ }
  finally { saving.value = false }
}
</script>

<style>
.recruitment-editor.el-dialog{max-width:calc(100vw - 48px);max-height:90vh;margin-top:5vh;overflow:auto;border:1px solid rgba(219,232,237,.16);border-radius:10px;background:#162024;color:#e6edf0;--el-text-color-primary:#e6edf0;--el-text-color-regular:#a9bbc2;--el-fill-color-blank:#101a1e;--el-border-color:#35464e;--el-color-primary:#d8a935}
.recruitment-editor .el-dialog__title{font-size:20px;font-weight:650;color:#e6edf0}.recruitment-editor .el-dialog__body{padding-top:12px}.recruitment-editor .el-select{width:100%}.recruitment-editor .el-select__wrapper{min-height:42px;background:#101a1e}
</style>
<style scoped>
.recruitment-form{display:grid;gap:14px}.recruitment-form label{display:grid;gap:7px;min-width:0;color:#a9bbc2;font-size:13px}.schedule-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.recruitment-form input,.recruitment-form textarea{box-sizing:border-box;width:100%;min-width:0;min-height:42px;padding:9px 11px;border:1px solid rgba(219,232,237,.16);border-radius:7px;color:#e6edf0;background:#101a1e;font:inherit;color-scheme:dark}.recruitment-form textarea{resize:vertical;line-height:1.5}.recruitment-form input[readonly]{color:#8da1aa}.recruitment-form input:focus,.recruitment-form textarea:focus{outline:1px solid #d8a935;outline-offset:1px}.recruitment-form footer{display:flex;justify-content:flex-end;gap:8px;margin-top:6px}.tool-button{min-height:42px;padding:0 16px;border:1px solid rgba(219,232,237,.16);border-radius:8px;color:#e6edf0;background:rgba(255,255,255,.035);cursor:pointer}.tool-button.primary{color:#f7d774;border-color:rgba(216,169,53,.32);background:rgba(216,169,53,.08)}.tool-button:disabled{opacity:.55;cursor:wait}@media(max-width:600px){.schedule-grid{grid-template-columns:1fr}}
</style>
