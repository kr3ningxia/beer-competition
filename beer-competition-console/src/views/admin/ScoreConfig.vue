<template>
  <div>
    <div class="header-row">
      <h2>评分配置</h2>
      <el-button @click="load">加载</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </div>
    <el-form inline>
      <el-form-item label="比赛 ID"><el-input-number v-model="competitionId" :min="1" /></el-form-item>
    </el-form>
    <el-input v-model="configText" type="textarea" :rows="18" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchScoreConfigs, updateScoreConfigs } from '@/api/admin'

const competitionId = ref(1)
const configText = ref('[]')

async function load() {
  const data = await fetchScoreConfigs(competitionId.value)
  configText.value = JSON.stringify(data, null, 2)
}

async function save() {
  const parsed = JSON.parse(configText.value)
  await updateScoreConfigs(competitionId.value, { configs: parsed.map((item) => ({ judgeRoleType: item.judgeRoleType, dimensions: item.dimensions })) })
  ElMessage.success('评分配置已更新')
  load()
}

load()
</script>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
