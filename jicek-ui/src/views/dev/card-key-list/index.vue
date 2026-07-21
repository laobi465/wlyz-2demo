<!--
  极策k 卡密查询页面
  作者: 极策k  日期: 2026-07-21

  功能：按卡号查询卡密详情，封禁/退款操作（带二次确认）
  接口：GET /api/dev/card/query, POST /api/dev/card/ban, POST /api/dev/card/refund
  安全：查询结果中卡密明文已脱敏
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">卡密查询</span>
      </template>

      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="卡号">
          <el-input v-model="filter.cardNo" placeholder="请输入卡号" clearable style="width: 320px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="cardInfo" :column="2" border>
        <el-descriptions-item label="卡号">{{ cardInfo.cardNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="cardInfo.status" type="card" />
        </el-descriptions-item>
        <el-descriptions-item label="卡类ID">{{ cardInfo.cardTypeId }}</el-descriptions-item>
        <el-descriptions-item label="软件ID">{{ cardInfo.softwareId }}</el-descriptions-item>
        <el-descriptions-item label="首次使用">{{ cardInfo.firstUseTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到期时间">{{ cardInfo.expireTime || '永久' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ cardInfo.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ cardInfo.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <el-empty v-else-if="!loading" description="请输入卡号查询" />

      <div v-if="cardInfo" style="margin-top: 16px">
        <el-button
          v-if="cardInfo.status === 0 || cardInfo.status === 1"
          type="danger"
          @click="handleBan"
        >
          封禁卡密
        </el-button>
        <el-button
          v-if="cardInfo.status === 0 || cardInfo.status === 1"
          type="warning"
          @click="handleRefund"
        >
          退款卡密
        </el-button>
      </div>
    </el-card>

    <ConfirmDialog
      v-model="banVisible"
      title="封禁确认"
      type="danger"
      :message="`确认封禁卡密 ${cardInfo?.cardNo}?`"
      sub-message="封禁后所有绑定设备立即下线，不可恢复"
      confirm-text="确认封禁"
      @confirm="doBan"
    />

    <ConfirmDialog
      v-model="refundVisible"
      title="退款确认"
      type="warning"
      :message="`确认退款卡密 ${cardInfo?.cardNo}?`"
      sub-message="退款后卡密立即失效，不可恢复"
      confirm-text="确认退款"
      @confirm="doRefund"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { cardKeyApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'

const loading = ref(false)
const cardInfo = ref<any>(null)
const banVisible = ref(false)
const refundVisible = ref(false)

const filter = reactive({
  tenantId: 1,
  cardNo: ''
})

const handleQuery = async () => {
  if (!filter.cardNo) {
    ElMessage.warning('请输入卡号')
    return
  }
  loading.value = true
  try {
    const resp: any = await cardKeyApi.query(filter.tenantId, filter.cardNo)
    cardInfo.value = resp.card
  } finally {
    loading.value = false
  }
}

const handleBan = () => {
  banVisible.value = true
}

const doBan = async () => {
  if (!cardInfo.value) return
  try {
    await cardKeyApi.ban(filter.tenantId, cardInfo.value.id, '管理员手动封禁')
    ElMessage.success('封禁成功')
    handleQuery()
  } catch {}
}

const handleRefund = () => {
  refundVisible.value = true
}

const doRefund = async () => {
  if (!cardInfo.value) return
  try {
    await cardKeyApi.refund(filter.tenantId, cardInfo.value.id)
    ElMessage.success('退款成功')
    handleQuery()
  } catch {}
}
</script>
