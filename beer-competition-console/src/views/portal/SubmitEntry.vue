<template>
  <div class="submit-page">
    <section class="form-card brewer-card">
      <div class="form-header">
        <span class="label-chip tone-gold">NEW ENTRY</span>
        <h2 class="portal-section-title">提交参赛酒款</h2>
        <p>填写完整信息后，系统将生成匿名编码并进入待付款状态。</p>
      </div>

      <el-form :model="form" label-position="top" class="entry-form">
        <div class="form-grid">
          <el-form-item label="目标比赛">
            <el-select v-model="form.competition">
              <el-option v-for="item in competitionOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="酒款名称">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="投递组别">
            <el-select v-model="form.category">
              <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="基础风格">
            <el-select v-model="form.style" filterable>
              <el-option v-for="item in styleOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="ABV">
            <el-input v-model="form.abv" />
          </el-form-item>
          <el-form-item label="IBU">
            <el-input-number v-model="form.ibu" :min="0" :max="120" />
          </el-form-item>
        </div>

        <el-form-item label="酒款简介">
          <el-input v-model="form.description" type="textarea" :rows="5" maxlength="300" show-word-limit />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="特殊工艺">
            <el-input v-model="form.process" />
          </el-form-item>
          <el-form-item label="增味原料">
            <el-input v-model="form.ingredients" />
          </el-form-item>
        </div>

        <div class="confirm-box">
          <el-checkbox v-model="form.confirmed">
            已确认评审匿名视图不会包含厂牌、联系人和酒款名称
          </el-checkbox>
        </div>

        <div class="form-actions">
          <el-button>保存草稿</el-button>
          <el-button type="primary" :disabled="!form.confirmed" @click="submitMock">提交并生成 UUID</el-button>
        </div>
      </el-form>
    </section>

    <aside class="preview-card">
      <div class="preview-label">
        <span>ANONYMOUS SCORE CARD</span>
        <strong>{{ mockUuid }}</strong>
        <p>{{ form.category }} · {{ form.style }} · {{ form.abv }}</p>
        <div class="foam-line" />
        <p>{{ form.description }}</p>
        <dl>
          <div>
            <dt>特殊工艺</dt>
            <dd>{{ form.process }}</dd>
          </div>
          <div>
            <dt>增味原料</dt>
            <dd>{{ form.ingredients }}</dd>
          </div>
        </dl>
      </div>

      <div class="hidden-note">
        <strong>匿名保护</strong>
        <p>此预览模拟评审扫码后看到的信息。酒名、厂牌和联系人仅供厂商与主办方后台查看。</p>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { activeCompetition, categoryOptions, competitionOptions, styleOptions } from './mockData'

const mockUuid = '提交后自动生成'

const form = reactive({
  competition: activeCompetition.name,
  name: '新批次实验 IPA',
  category: '美式 IPA',
  style: 'Hazy IPA',
  abv: '6.2%',
  ibu: 32,
  description: '热带水果香气明显，酒体柔和，使用 Mosaic、Citra 与 Nelson Sauvin 干投。',
  process: '双倍干投，冷端酒花 15g/L',
  ingredients: '无',
  confirmed: true,
})

function submitMock() {
  ElMessage.success('Mock 提交成功：已生成匿名编码 BC-2026-IPA-0042')
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
  font-size: 29px;
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
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
