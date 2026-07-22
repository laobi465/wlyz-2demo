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
        <span class="jicek-card-title">提现审核</span>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
            <el-option label="已打款" :value="3" />
            <el-option label="打款失败" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理ID">
          <el-input-number v-model="filter.agentId" :min="1" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 汇总 -->
      <el-alert type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          汇总：待审核 {{ summary.pending }} 笔 / ¥{{ formatAmount(summary.pendingAmount) }} |
          已打款 {{ summary.paid }} 笔 / ¥{{ formatAmount(summary.paidAmount) }}
        </template>
      </el-alert>

      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="agentId" label="代理ID" width="100" />
        <el-table-column prop="amount" label="申请金额" width="120">
          <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="fee" label="手续费" width="100">
          <template #default="{ row }">¥{{ formatAmount(row.fee) }}</template>
        </el-table-column>
        <el-table-column prop="actualAmount" label="实际到账" width="120">
          <template #default="{ row }">¥{{ formatAmount(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column prop="payType" label="收款方式" width="100">
          <template #default="{ row }">{{ payTypeText(row.payType) }}</template>
        </el-table-column>
        <el-table-column prop="payAccount" label="收款账号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="payName" label="收款人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" type="withdraw" />
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" min-width="160" />
        <el-table-column prop="auditTime" label="审核时间" min-width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" size="small" @click="handleAudit(row, 'approve')">通过</el-button>
              <el-button link type="danger" size="small" @click="handleAudit(row, 'reject')">拒绝</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button link type="success" size="small" @click="handleAudit(row, 'payout')">已打款</el-button>
              <el-button link type="danger" size="small" @click="handleAudit(row, 'fail')">打款失败</el-button>
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
          {{ auditForm.action === 'reject' ? '拒绝后冻结金额将退回代理可用余额' : '标记失败后冻结金额将退回代理可用余额' }}
        </template>
      </el-alert>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="代理 ID">{{ auditRow?.agentId }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">¥{{ formatAmount(auditRow?.amount) }}</el-descriptions-item>
        <el-descriptions-item label="实际到账">¥{{ formatAmount(auditRow?.actualAmount) }}</el-descriptions-item>
        <el-descriptions-item label="收款方式">{{ payTypeText(auditRow?.payType) }}</el-descriptions-item>
        <el-descriptions-item label="收款账号">{{ auditRow?.payAccount }}</el-descriptions-item>
        <el-descriptions-item label="收款人">{{ auditRow?.payName || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="auditForm" label-width="100px" style="margin-top: 16px">
        <el-form-item v-if="auditForm.action === 'payout'" label="打款流水号">
          <el-input v-model="auditForm.tradeNo" placeholder="打款流水号（可选）" />
        </el-form-item>
        <el-form-item v-if="auditForm.action === 'fail'" label="失败原因">
          <el-input v-model="auditForm.failReason" type="textarea" :rows="2" placeholder="打款失败原因" />
        </el-form-item>
        <el-form-item v-if="auditForm.action === 'approve' || auditForm.action === 'reject'" label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button
          :type="auditForm.action === 'reject' || auditForm.action === 'fail' ? 'danger' : 'primary'"
          :loading="submitting"
          @click="doAudit"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { withdrawApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import Decimal from 'decimal.js'

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
    approve: '审核通过',
    reject: '审核拒绝',
    payout: '确认已打款',
    fail: '标记打款失败'
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

const payTypeText = (t: string) => {
  return { alipay: '支付宝', wxpay: '微信', bank: '银行卡' }[t as string] || t || '-'
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
      title: '操作成功',
      message: `${auditTitle.value} - 申请ID ${auditForm.withdrawId}`,
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
