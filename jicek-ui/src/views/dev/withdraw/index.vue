<!--
  极策k 提现审核页面
  作者: 极策k  日期: 2026-07-22

  功能：提现申请分页查询 + 审核（通过/拒绝/打款/失败）
  接口：
    GET  /api/dev/withdraw/page         提现申请分页
    POST /api/dev/withdraw/audit        审核（action: approve/reject/payout/fail）
  安全：审核操作需二次确认；资金操作使用 ElNotification 持久提示
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('withdraw.title') }}</span>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('withdraw.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option :label="t('withdraw.statusPending')" :value="0" />
            <el-option :label="t('withdraw.statusApproved')" :value="1" />
            <el-option :label="t('withdraw.statusRejected')" :value="2" />
            <el-option :label="t('withdraw.statusPaid')" :value="3" />
            <el-option :label="t('withdraw.statusFailed')" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('withdraw.agentId')">
          <el-input-number v-model="filter.agentId" :min="1" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 汇总 -->
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          {{ t('withdraw.summary', { pending: summary.pending, pendingAmount: formatAmount(summary.pendingAmount), paid: summary.paid, paidAmount: formatAmount(summary.paidAmount) }) }}
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="agentId" :label="t('withdraw.agentId')" width="100" />
        <el-table-column prop="amount" :label="t('withdraw.applyAmount')" width="120">
          <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="fee" :label="t('withdraw.fee')" width="100">
          <template #default="{ row }">¥{{ formatAmount(row.fee) }}</template>
        </el-table-column>
        <el-table-column prop="actualAmount" :label="t('withdraw.actualAmount')" width="120">
          <template #default="{ row }">¥{{ formatAmount(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column prop="payType" :label="t('withdraw.payType')" width="100">
          <template #default="{ row }">{{ payTypeText(row.payType) }}</template>
        </el-table-column>
        <el-table-column prop="payAccount" :label="t('withdraw.payAccount')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="payName" :label="t('withdraw.payName')" width="100" />
        <el-table-column prop="status" :label="t('withdraw.status')" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" type="withdraw" />
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" :label="t('withdraw.applyTime')" min-width="160" />
        <el-table-column prop="auditTime" :label="t('withdraw.auditTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="280" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" size="small" @click="handleAudit(row, 'approve')">{{ t('withdraw.approve') }}</el-button>
              <el-button link type="danger" size="small" @click="handleAudit(row, 'reject')">{{ t('withdraw.reject') }}</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button link type="success" size="small" @click="handleAudit(row, 'payout')">{{ t('withdraw.payout') }}</el-button>
              <el-button link type="danger" size="small" @click="handleAudit(row, 'fail')">{{ t('withdraw.fail') }}</el-button>
            </template>
            <span v-else style="color: var(--jicek-text-secondary)">-</span>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" :title="auditTitle" width="480px">
      <el-alert
        v-if="auditForm.action === 'reject' || auditForm.action === 'fail'"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <template #title>
          {{ auditForm.action === 'reject' ? t('withdraw.rejectAlert') : t('withdraw.failAlert') }}
        </template>
      </el-alert>
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="t('withdraw.agentIdLabel')">{{ auditRow?.agentId }}</el-descriptions-item>
        <el-descriptions-item :label="t('withdraw.applyAmount')">¥{{ formatAmount(auditRow?.amount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('withdraw.actualAmountLabel')">¥{{ formatAmount(auditRow?.actualAmount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('withdraw.payTypeLabel')">{{ payTypeText(auditRow?.payType) }}</el-descriptions-item>
        <el-descriptions-item :label="t('withdraw.payAccountLabel')">{{ auditRow?.payAccount }}</el-descriptions-item>
        <el-descriptions-item :label="t('withdraw.payNameLabel')">{{ auditRow?.payName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="auditForm" label-width="100px" style="margin-top: 16px">
        <el-form-item v-if="auditForm.action === 'payout'" :label="t('withdraw.tradeNo')">
          <el-input v-model="auditForm.tradeNo" :placeholder="t('withdraw.tradeNoPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="auditForm.action === 'fail'" :label="t('withdraw.failReason')">
          <el-input v-model="auditForm.failReason" type="textarea" :rows="2" :placeholder="t('withdraw.failReasonPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="auditForm.action === 'approve' || auditForm.action === 'reject'" :label="t('withdraw.auditRemark')">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">{{ t('withdraw.cancel') }}</el-button>
        <el-button
          :type="auditForm.action === 'reject' || auditForm.action === 'fail' ? 'danger' : 'primary'"
          :loading="submitting"
          @click="doAudit"
        >
          {{ t('withdraw.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElNotification } from 'element-plus'
import { withdrawApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import Decimal from 'decimal.js'

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const auditVisible = ref(false)
const auditRow = ref<any>(null)
const summary = ref({ pending: 0, pendingAmount: 0, paid: 0, paidAmount: 0 })

const filter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  agentId: undefined as number | undefined,
  status: 0 as number | undefined
})

const auditForm = reactive({
  withdrawId: 0,
  action: 'approve' as 'approve' | 'reject' | 'payout' | 'fail',
  auditRemark: '',
  tradeNo: '',
  failReason: ''
})

const auditTitle = computed(() => {
  const map = {
    approve: t('withdraw.auditTitleApprove'),
    reject: t('withdraw.auditTitleReject'),
    payout: t('withdraw.auditTitlePayout'),
    fail: t('withdraw.auditTitleFail')
  }
  return map[auditForm.action]
})

const formatAmount = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  try {
    return new Decimal(val).toFixed(2)
  } catch {
    return '0.00'
  }
}

const payTypeText = (type: string) => {
  return { alipay: t('withdraw.payTypeAlipay'), wxpay: t('withdraw.payTypeWxpay'), bank: t('withdraw.payTypeBank') }[type as string] || type || '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const resp: any = await withdrawApi.page(filter)
    tableData.value = resp.records || []
    total.value = resp.total || 0
    // 计算汇总
    const pending = tableData.value.filter(r => r.status === 0)
    const paid = tableData.value.filter(r => r.status === 3)
    summary.value = {
      pending: pending.length,
      pendingAmount: pending.reduce((s, r) => s + Number(r.amount), 0),
      paid: paid.length,
      paidAmount: paid.reduce((s, r) => s + Number(r.actualAmount), 0)
    }
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filter.status = undefined
  filter.agentId = undefined
  filter.current = 1
  loadData()
}

const handleAudit = (row: any, action: 'approve' | 'reject' | 'payout' | 'fail') => {
  auditRow.value = row
  auditForm.withdrawId = row.id
  auditForm.action = action
  auditForm.auditRemark = ''
  auditForm.tradeNo = ''
  auditForm.failReason = ''
  auditVisible.value = true
}

const doAudit = async () => {
  submitting.value = true
  try {
    await withdrawApi.audit({
      tenantId: filter.tenantId,
      withdrawId: auditForm.withdrawId,
      action: auditForm.action,
      auditRemark: auditForm.auditRemark,
      tradeNo: auditForm.tradeNo,
      failReason: auditForm.failReason
    })
    ElNotification({
      title: t('withdraw.auditSuccess'),
      message: t('withdraw.auditSuccessMsg', { title: auditTitle.value, id: auditForm.withdrawId }),
      type: 'success',
      duration: 0
    })
    auditVisible.value = false
    loadData()
  } catch {
    // 静默
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
