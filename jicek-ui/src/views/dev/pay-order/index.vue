<!--
  极策k 资金流水页面
  作者: 极策k  日期: 2026-07-21

  功能：订单分页查询（按状态筛选），退款操作（带二次确认），今日汇总
  接口：GET /api/dev/pay/order/page, POST /api/dev/pay/refund
  安全：退款前必须确认，金额展示使用 decimal.js
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('payOrder.title') }}</span>
        <div style="float: right">
          <el-button @click="handleExport">{{ t('payOrder.export') }}</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('payOrder.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option :label="t('payOrder.statusPending')" :value="0" />
            <el-option :label="t('payOrder.statusPaid')" :value="1" />
            <el-option :label="t('payOrder.statusFailed')" :value="2" />
            <el-option :label="t('payOrder.statusRefunded')" :value="3" />
            <el-option :label="t('payOrder.statusClosed')" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 汇总 -->
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #title>
          {{ t('payOrder.summary', { income: formatAmount(summary.todayIncome), refund: formatAmount(summary.todayRefund), netIncome: formatAmount(summary.todayNetIncome) }) }}
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
        <el-table-column prop="outTradeNo" :label="t('payOrder.outTradeNo')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="amount" :label="t('payOrder.amount')" width="100">
          <template #default="{ row }">
            ¥{{ formatAmount(row.amount) }}
          </template>
        </el-table-column>
        <el-table-column prop="payType" :label="t('payOrder.channel')" width="100">
          <template #default="{ row }">
            {{ channelText(row.payType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('payOrder.status')" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" type="order" />
          </template>
        </el-table-column>
        <el-table-column prop="quantity" :label="t('payOrder.quantity')" width="80" />
        <el-table-column prop="payTime" :label="t('payOrder.payTime')" min-width="160" />
        <el-table-column prop="createTime" :label="t('payOrder.createTime')" min-width="160" />
        <el-table-column :label="t('payOrder.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              size="small"
              @click="handleRefund(row)"
            >
              {{ t('payOrder.refund') }}
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
      :title="t('payOrder.refundTitle')"
      type="danger"
      :message="t('payOrder.refundMessage', { tradeNo: refundOrder?.outTradeNo })"
      :sub-message="t('payOrder.refundSubMessage')"
      :confirm-text="t('payOrder.refundConfirm')"
      @confirm="doRefund"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { payApi, dashboardApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'
import Decimal from 'decimal.js'

const { t } = useI18n()

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
    alipay: t('payOrder.channelAlipay'),
    wxpay: t('payOrder.channelWxpay'),
    qqpay: t('payOrder.channelQqpay'),
    unionpay: t('payOrder.channelUnionpay')
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
    await payApi.refund(refundOrder.value.outTradeNo, t('payOrder.refund'))
    ElMessage.success(t('payOrder.refundSuccess'))
    loadData()
    loadSummary()
  } catch {
    // 错误已在拦截器处理
  }
}

const handleExport = () => {
  ElMessage.info(t('payOrder.exportTodo'))
}

onMounted(() => {
  loadData()
  loadSummary()
})
</script>
