<template>
  <div>
    <h2>欢迎回来，{{ me?.displayName || '厂商用户' }}</h2>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <div>当前手机号</div>
          <strong>{{ me?.phone || '-' }}</strong>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div>已接入模块</div>
          <strong>酒款列表 / 结果查询骨架</strong>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPortalMe } from '@/api/auth'

const me = ref(null)

onMounted(async () => {
  me.value = await getPortalMe()
})
</script>
