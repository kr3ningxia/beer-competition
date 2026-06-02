<template>
  <div class="submit-page">
    <section class="form-card brewer-card">
      <div class="form-header">
        <span class="label-chip tone-gold">提交酒款</span>
        <h2 class="portal-section-title">填写参赛酒款资料</h2>
        <p>请选择目标赛事并填写酒款信息。评审可见内容中请避免出现厂牌、联系人或可识别身份的信息。</p>
      </div>

      <el-form :model="form" label-position="top" class="entry-form">
        <div class="form-grid">
          <el-form-item label="目标赛事">
            <el-select v-model="form.competitionId" :disabled="lockedCompetition" @change="syncCompetitionDefaults">
              <el-option
                v-for="item in openCompetitions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="酒款名称">
            <el-input v-model="form.name" placeholder="仅主办方和厂商可见" />
          </el-form-item>
          <el-form-item label="投递组别">
            <el-select v-model="form.category">
              <el-option v-for="item in selectedCompetition.categories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="基础风格">
            <el-select v-model="form.style" filterable>
              <el-option v-for="item in selectedCompetition.styleOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="ABV">
            <el-input-number v-model="form.abv" :min="0" :max="20" :precision="1" :step="0.1" />
          </el-form-item>
          <el-form-item label="IBU">
            <el-input-number v-model="form.ibu" :min="0" :max="120" />
          </el-form-item>
        </div>

        <el-form-item label="酒款简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="评审可见。请描述风格、香气、口感或工艺特点，避免出现厂牌和联系人。"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item v-for="field in selectedCompetition.entryFields" :key="field" :label="field">
            <el-input v-model="form.extraFields[field]" placeholder="评审可见，请谨慎填写可识别信息" />
          </el-form-item>
        </div>

        <div class="confirm-box">
          <el-checkbox v-model="form.confirmed">
            我已确认评审可见内容不包含厂牌、联系人或其他可识别身份的信息
          </el-checkbox>
        </div>

        <div class="form-actions">
          <RouterLink class="secondary-link" :to="`/portal/events/${form.competitionId}`">返回赛事详情</RouterLink>
          <el-button type="primary" :disabled="submitDisabled" @click="submitEntry">提交报名</el-button>
        </div>
      </el-form>
    </section>

    <aside class="preview-card">
      <div class="preview-label">
        <span>评审可见信息预览</span>
        <strong>提交后生成参赛编号</strong>
        <p>{{ form.category || '投递组别' }} · {{ form.style || '基础风格' }} · {{ form.abv ? `${form.abv}%` : 'ABV' }}</p>
        <div class="foam-line" />
        <p>{{ form.description || '酒款简介会展示给评审，用于了解风格和工艺特点。' }}</p>
        <dl>
          <div v-for="field in selectedCompetition.entryFields" :key="field">
            <dt>{{ field }}</dt>
            <dd>{{ form.extraFields[field] || '待填写' }}</dd>
          </div>
        </dl>
      </div>

      <div class="hidden-note">
        <strong>不会展示给评审</strong>
        <p>酒款名称、厂牌资料、联系人和付款信息只用于厂商与主办方核对。</p>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { competitions } from './mockData'

const route = useRoute()
const router = useRouter()
const openCompetitions = competitions.filter((competition) => competition.status === 'REGISTRATION_OPEN')
const queryCompetitionId = route.query.competitionId
const lockedCompetition = computed(() => Boolean(queryCompetitionId && openCompetitions.some((item) => item.id === queryCompetitionId)))

const form = reactive({
  competitionId: lockedCompetition.value ? queryCompetitionId : openCompetitions[0]?.id,
  name: '',
  category: '',
  style: '',
  abv: null,
  ibu: null,
  description: '',
  extraFields: {},
  confirmed: false,
})

const selectedCompetition = computed(() => openCompetitions.find((item) => item.id === form.competitionId) || openCompetitions[0])
const submitDisabled = computed(() => !form.confirmed || !form.competitionId || !form.name || !form.category || !form.style || !form.abv || !form.description)

syncCompetitionDefaults()

function syncCompetitionDefaults() {
  form.category = ''
  form.style = ''
  form.extraFields = Object.fromEntries((selectedCompetition.value?.entryFields || []).map((field) => [field, '']))
}

function submitEntry() {
  ElMessage.success('已提交报名，等待付款确认')
  router.push('/portal/my')
}
</script>

<style scoped>
.submit-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 22px;
  align-items: start;
}

.form-card {
  padding: 24px;
}

.form-header p {
  margin: 0 0 20px;
  color: #746a5f;
  line-height: 1.65;
}

.entry-form :deep(.el-input__wrapper),
.entry-form :deep(.el-textarea__inner),
.entry-form :deep(.el-select__wrapper) {
  background: #fffdf7;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(87, 58, 26, 0.14) inset;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 18px;
}

.confirm-box {
  padding: 14px;
  background: #fff7e6;
  border: 1px dashed rgba(87, 58, 26, 0.2);
  border-radius: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.secondary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 14px;
  color: #6b4710;
  background: #fff7e6;
  border: 1px solid rgba(87, 58, 26, 0.14);
  border-radius: 8px;
  font-weight: 800;
  text-decoration: none;
}

.preview-card {
  position: sticky;
  top: 116px;
  display: grid;
  gap: 16px;
}

.preview-label {
  min-height: 520px;
  padding: 26px;
  color: #2b1d10;
  background:
    linear-gradient(180deg, #fffaf0, #eac06c),
    repeating-linear-gradient(135deg, rgba(45, 29, 14, 0.07) 0 1px, transparent 1px 10px);
  border: 1px solid rgba(87, 58, 26, 0.18);
  border-radius: 8px;
  box-shadow: 0 24px 48px rgba(83, 51, 17, 0.14);
}

.preview-label > span {
  display: block;
  color: #8b5c19;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.preview-label > strong {
  display: block;
  margin-top: 34px;
  font-size: 26px;
  line-height: 1.15;
}

.preview-label p {
  color: #675b4a;
  line-height: 1.7;
}

.foam-line {
  height: 9px;
  margin: 26px 0;
  background:
    radial-gradient(circle, #fffaf0 0 45%, transparent 48%) 0 0 / 16px 9px repeat-x;
}

.preview-label dl {
  display: grid;
  gap: 12px;
  margin: 26px 0 0;
}

.preview-label dt {
  color: #806f5b;
  font-size: 13px;
}

.preview-label dd {
  margin: 4px 0 0;
  font-weight: 800;
  line-height: 1.5;
}

.hidden-note {
  padding: 16px;
  color: #d8c8a8;
  background: #211710;
  border-radius: 8px;
}

.hidden-note p {
  margin: 8px 0 0;
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .submit-page {
    grid-template-columns: 1fr;
  }

  .preview-card {
    position: static;
  }
}

@media (max-width: 680px) {
  .form-grid,
  .form-actions {
    grid-template-columns: 1fr;
  }

  .form-actions {
    display: grid;
  }
}
</style>
