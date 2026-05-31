<template>
  <div>
    <h2>参赛酒款</h2>
    <el-table :data="entries" @row-click="showDetail">
      <el-table-column prop="uuid" label="匿名编码" />
      <el-table-column prop="name" label="酒名" />
      <el-table-column prop="competitionName" label="比赛" />
      <el-table-column prop="categoryName" label="组别" />
      <el-table-column prop="status" label="状态" />
    </el-table>

    <el-drawer v-model="visible" size="40%" :title="detail?.name || '酒款详情'">
      <pre>{{ detail }}</pre>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchPortalEntries, fetchPortalEntryDetail } from '@/api/portal'

const entries = ref([])
const detail = ref(null)
const visible = ref(false)

async function load() {
  entries.value = await fetchPortalEntries()
}

async function showDetail(row) {
  detail.value = await fetchPortalEntryDetail(row.id)
  visible.value = true
}

onMounted(load)
</script>
