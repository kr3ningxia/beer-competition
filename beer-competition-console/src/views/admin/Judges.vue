<template>
  <div>
    <h2>评审列表</h2>
    <el-table :data="list">
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="wechat" label="微信号" />
      <el-table-column prop="qualification" label="资质" />
      <el-table-column prop="status" label="状态" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchJudges } from '@/api/admin'

const list = ref([])

onMounted(async () => {
  list.value = await fetchJudges()
})
</script>
