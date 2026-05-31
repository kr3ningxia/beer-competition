<template>
  <div>
    <div class="header-row">
      <h2>比赛管理</h2>
      <el-button type="primary" @click="create">新增比赛</el-button>
    </div>
    <el-form inline>
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="日期"><el-date-picker v-model="form.competitionDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="报名截止"><el-date-picker v-model="form.registrationDeadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 180px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="报名中" value="REGISTRATION_OPEN" />
          <el-option label="报名截止" value="REGISTRATION_CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="报名费"><el-input-number v-model="form.entryFee" :min="0" /></el-form-item>
    </el-form>
    <el-table :data="list">
      <el-table-column prop="name" label="比赛名称" />
      <el-table-column prop="competitionDate" label="比赛日期" />
      <el-table-column prop="registrationDeadline" label="报名截止" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="entryFee" label="报名费" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createCompetition, fetchCompetitions } from '@/api/admin'

const list = ref([])
const form = reactive({
  name: '2026新建比赛',
  competitionDate: '2026-08-20',
  registrationDeadline: '2026-07-30T18:00:00',
  status: 'DRAFT',
  entryFee: 199,
})

async function load() {
  list.value = await fetchCompetitions()
}

async function create() {
  await createCompetition(form)
  ElMessage.success('比赛已创建')
  load()
}

onMounted(load)
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
