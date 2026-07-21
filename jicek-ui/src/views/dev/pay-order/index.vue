<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">资金流水</span>
        <div style="float: right">
          <el-button @click="handleExport">导出 Excel</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="失败" :value="2" />
            <el-option label="已退款" :value="3" />
            <el-option label="已关闭" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 汇总 -->
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #title>
          汇总：今日收入 ¥{{ formatAmount(summary.todayIncome) }} |
          已退款 ¥{{ formatAmount(summary.todayRefund) }} |
          净收入 ¥{{ formatAmount(summary.todayNetIncome) }}
        </template>
      </el-alert>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="outTradeNo" label="订单号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">
            ¥{{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="payType" label="通道" width="100">
          <template #default="{ row }">
            {{ channelText(row.payType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" type="order" />
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="payTime" label="支付时间" min-width="160" />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              size="small"
              @click="handleRefund(row)"
            >
              退款
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="filter.current"
        v-model:page-size="filter.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 退款确认弹窗 -->
    <ConfirmDialog
      v-model="refundVisible"
      title="退款确认"
      type="danger"
      :message="`确认退款订单 ${refundOrder?.outTradeNo}?`"
      sub-message="退款后关联的卡密将立即失效，不可恢复"
      confirm-text="确认退款"
      @confirm="doRefund"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { payApi, dashboardApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'
import Decimal from 'decimal.js'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const summary = ref<any>({})
const refundVisible = ref(false)
const refundOrder = ref<any>(null)

const filter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  status: undefined as number | undefined
})

const formatAmount = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  try {
    return new Decimal(val).toFixed(2)
  } catch {
    return '0.00'
  }
}

const channelText = (type: string) => {
  return {
    alipay: '支付宝',
    wxpay: '微信',
    qqpay: 'QQ',
    unionpay: '银联'
  }[type] || type || '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const resp: any = await payApi.pageOrder(filter)
    tableData.value = resp.records || []
    total.value = resp.total || 0
  } finally {
    loading.value = false
  }
}

const loadSummary = async () => {
  try {
    summary.value = await dashboardApi.summary(filter.tenantId)
  } catch {
    // 静默
  }
}

const handleReset = () => {
  filter.status = undefined
  filter.current = 1
  loadData()
}

const handleRefund = (row: any) => {
  refundOrder.value = row
  refundVisible.value = true
}

const doRefund = async () => {
  if (!refundOrder.value) return
  try {
    await payApi.refund(refundOrder.value.outTradeNo, '管理员手动退款')
    ElMessage.success('退款成功')
    loadData()
    loadSummary()
  } catch {
    // 错误已在拦截器处理
  }
}

const handleExport = () => {
  ElMessage.info('导出 Excel 功能待实现（v0.3.0）')
}

onMounted(() => {
  loadData()
  loadSummary()
})
</script>
