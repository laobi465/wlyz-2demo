<!--
  极策k 开发者控制台页面
  作者: 极策k  日期: 2026-07-21

  展示：今日收入/订单/退款/净收入/卡密状态分布
  数据源：GET /api/dev/dashboard/summary
  金额展示使用 decimal.js 保证精度
-->
<template>
  <div class="jicek-page">
    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">今日收入</div>
          <div class="jicek-stat-value success">¥{{ formatAmount(summary.todayIncome) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">今日订单</div>
          <div class="jicek-stat-value">{{ summary.todayOrderCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">今日退款</div>
          <div class="jicek-stat-value danger">¥{{ formatAmount(summary.todayRefund) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">今日净收入</div>
          <div class="jicek-stat-value success">¥{{ formatAmount(summary.todayNetIncome) }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">今日生成卡密</div>
          <div class="jicek-stat-value">{{ summary.todayCardCount || 0 }} 张</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">未使用卡密</div>
          <div class="jicek-stat-value warning">{{ summary.cardStatus?.unused || 0 }} 张</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">已使用卡密</div>
          <div class="jicek-stat-value success">{{ summary.cardStatus?.used || 0 }} 张</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">已封禁卡密</div>
          <div class="jicek-stat-value danger">{{ summary.cardStatus?.banned || 0 }} 张</div>
        </div>
      </el-col>
    </el-row>

    <!-- 卡密状态分布 -->
    <el-card>
      <template #header>
        <span class="jicek-card-title">数据趋势</span>
      </template>
      <el-empty description="验证量趋势图待实现（v0.3.0）" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { dashboardApi } from '@/api'
import Decimal from 'decimal.js'

const summary = ref<any>({})

const formatAmount = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  try {
    return new Decimal(val).toFixed(2)
  } catch {
    return '0.00'
  }
}

const loadSummary = async () => {
  // TODO: tenantId 从登录态获取，此处临时用 1
  try {
    summary.value = await dashboardApi.summary(1)
  } catch (e) {
    // 接口未启动时静默
  }
}

onMounted(() => {
  loadSummary()
})
</script>

<style scoped>
.stat-row {
  margin-bottom: 16px;
}
</style>
