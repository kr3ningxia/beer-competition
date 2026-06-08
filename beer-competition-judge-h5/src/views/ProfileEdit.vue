<template>
  <main class="app-shell">
    <section class="top-panel">
      <p class="eyebrow">评审资料</p>
      <h1 class="page-title">{{ profile?.profileRequired ? '完善个人信息' : '修改个人信息' }}</h1>
    </section>

    <section class="card">
      <label class="field">
        手机号
        <input v-model.trim="form.phone" class="input readonly" inputmode="tel" readonly />
      </label>
      <label class="field">
        姓名
        <input v-model.trim="form.name" class="input" placeholder="请输入真实姓名" />
      </label>
      <label class="field">
        微信号
        <input v-model.trim="form.wechat" class="input" placeholder="便于现场工作人员联系" />
      </label>
      <label class="field">
        资质信息
        <textarea v-model.trim="form.qualification" class="textarea" placeholder="例如 BJCP 等级、评审经验、从业背景"></textarea>
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
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchProfile, updateProfile } from '@/api/judge'

const router = useRouter()
const profile = ref(null)
const saving = ref(false)
const error = ref('')
const form = reactive({
  phone: '',
  name: '',
  wechat: '',
  qualification: '',
  breweryConflictFlag: false,
  breweryConflictText: '',
})

onMounted(async () => {
  profile.value = await fetchProfile()
  form.phone = profile.value.phone || ''
  form.name = profile.value.name || ''
  form.wechat = profile.value.wechat || ''
  form.qualification = profile.value.qualification || ''
  form.breweryConflictFlag = Boolean(profile.value.breweryConflictFlag)
  form.breweryConflictText = profile.value.breweryConflictText || ''
})

async function save() {
  error.value = ''
  if (!form.name || !form.qualification) {
    error.value = '请填写姓名和资质信息。'
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
</script>

<style scoped>
.form-error {
  margin: 12px 0 0;
  color: #b42318;
  font-size: 14px;
  font-weight: 700;
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
