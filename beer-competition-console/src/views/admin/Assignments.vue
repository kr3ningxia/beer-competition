<template>
  <div>
    <h2>评审分组分配</h2>
    <el-form label-position="top" class="form-card">
      <el-form-item label="比赛 ID"><el-input-number v-model="form.competitionId" :min="1" /></el-form-item>
      <el-form-item label="评审 ID"><el-input-number v-model="form.judgeAccountId" :min="1" /></el-form-item>
      <el-form-item label="桌次 ID"><el-input-number v-model="form.tableId" :min="1" /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.role">
          <el-option label="跨界评审" value="CROSS" />
          <el-option label="专业评审" value="PROFESSIONAL" />
          <el-option label="桌长" value="CAPTAIN" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="submit">提交分配</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { createAssignment } from '@/api/admin'

const form = reactive({
  competitionId: 1,
  judgeAccountId: 1,
  tableId: 1,
  role: 'PROFESSIONAL',
})

async function submit() {
  await createAssignment(form)
  ElMessage.success('分配已保存')
}
</script>

<style scoped>
.form-card {
  max-width: 460px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
}
</style>
