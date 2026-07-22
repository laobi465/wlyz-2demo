<!--
  极策k 代理管理页面
  作者: 极策k  日期: 2026-07-22

  功能：代理树形展示 + 分页列表 + 创建/编辑/封禁/解封/充值
  接口：
    GET  /api/dev/agent/tree         代理树（多级）
    GET  /api/dev/agent/page         代理分页（扁平）
    POST /api/dev/agent              创建代理
    PUT  /api/dev/agent              更新代理
    POST /api/dev/agent/ban          封禁
    POST /api/dev/agent/unban        解封
    POST /api/dev/agent/recharge     充值
  安全：分润比例 0-100；密码创建必填、更新可空；封禁需二次确认
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('agent.title') }}</span>
        <div style="float: right">
          <el-button type="primary" @click="handleCreate">{{ t('agent.create') }}</el-button>
          <el-button @click="loadTree">{{ treeVisible ? t('agent.hideTree') : t('agent.showTree') }}</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('agent.parentId')">
          <el-input v-model.number="filter.parentId" :placeholder="t('agent.parentIdPlaceholder')" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item :label="t('agent.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 120px">
            <el-option :label="t('agent.statusNormal')" :value="1" />
            <el-option :label="t('agent.statusBanned')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 树形展示 -->
      <el-card v-if="treeVisible" shadow="never" style="margin-bottom: 16px">
        <template #header><span style="font-weight: 600">{{ t('agent.treeTitle') }}</span></template>
        <el-tree
          v-if="treeData.length"
          :data="treeData"
          node-key="id"
          default-expand-all
          :props="{ label: 'treeLabel', children: 'children' }"
        >
          <template #default="{ data }">
            <span>
              <span>{{ data.username }}</span>
              <el-tag v-if="data.status === 0" type="danger" size="small" style="margin-left: 8px">{{ t('agent.banned') }}</el-tag>
              <span style="margin-left: 12px; color: var(--jicek-text-secondary); font-size: 12px">
                {{ t('agent.treeStats', { level: data.level, balance: formatAmount(data.balance), rate: data.commissionRate, subCount: data.subCount }) }}
              </span>
            </span>
          </template>
        </el-tree>
        <el-empty v-else :description="t('agent.treeEmpty')" />
      </el-card>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="t('agent.username')" min-width="120" />
        <el-table-column prop="realName" :label="t('agent.realName')" width="100" />
        <el-table-column prop="contact" :label="t('agent.contact')" min-width="140" />
        <el-table-column prop="level" :label="t('agent.level')" width="80">
          <template #default="{ row }">L{{ row.level }}</template>
        </el-table-column>
        <el-table-column prop="balance" :label="t('agent.balance')" width="120">
          <template #default="{ row }">¥{{ formatAmount(row.balance) }}</template>
        </el-table-column>
        <el-table-column prop="frozenBalance" :label="t('agent.frozenBalance')" width="120">
          <template #default="{ row }">¥{{ formatAmount(row.frozenBalance) }}</template>
        </el-table-column>
        <el-table-column prop="totalEarnings" :label="t('agent.totalEarnings')" width="120">
          <template #default="{ row }">¥{{ formatAmount(row.totalEarnings) }}</template>
        </el-table-column>
        <el-table-column prop="commissionRate" :label="t('agent.commissionRate')" width="100">
          <template #default="{ row }">{{ row.commissionRate }}%</template>
        </el-table-column>
        <el-table-column prop="maxSubLevel" :label="t('agent.maxSubLevel')" width="100" />
        <el-table-column prop="status" :label="t('agent.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? t('agent.statusNormal') : t('agent.statusBanned') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('agent.createTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">{{ t('agent.editBtn') }}</el-button>
            <el-button link type="success" size="small" @click="handleRecharge(row)">{{ t('agent.recharge') }}</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              size="small"
              @click="handleBan(row)"
            >
              {{ t('agent.ban') }}
            </el-button>
            <el-button
              v-else
              link
              type="primary"
              size="small"
              @click="handleUnban(row)"
            >
              {{ t('agent.unban') }}
            </el-button>
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

    <!-- 创建/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? t('agent.create') : t('agent.edit')"
      width="560px"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item :label="t('agent.tenant')" v-if="formMode === 'create'">
          <el-input-number v-model="formData.tenantId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('agent.parentIdLabel')">
          <el-input-number v-model="formData.parentId" :min="0" style="width: 100%" />
          <span style="color: var(--jicek-text-secondary); font-size: 12px">{{ t('agent.parentIdHint') }}</span>
        </el-form-item>
        <el-form-item :label="t('agent.username')" prop="username">
          <el-input v-model="formData.username" :placeholder="t('agent.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('agent.password')" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            :placeholder="formMode === 'edit' ? t('agent.passwordEditPlaceholder') : t('agent.passwordCreatePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('agent.realNameLabel')">
          <el-input v-model="formData.realName" />
        </el-form-item>
        <el-form-item :label="t('agent.contact')">
          <el-input v-model="formData.contact" :placeholder="t('agent.contactPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('agent.commissionRate')" prop="commissionRate">
          <el-input-number
            v-model="formData.commissionRate"
            :min="0"
            :max="100"
            :precision="2"
            style="width: 100%"
          />
          <span style="color: var(--jicek-text-secondary); font-size: 12px">{{ t('agent.commissionRateHint') }}</span>
        </el-form-item>
        <el-form-item :label="t('agent.maxSubLevel')" prop="maxSubLevel">
          <el-input-number v-model="formData.maxSubLevel" :min="0" :max="10" style="width: 100%" />
          <span style="color: var(--jicek-text-secondary); font-size: 12px">{{ t('agent.maxSubLevelHint') }}</span>
        </el-form-item>
        <el-form-item :label="t('agent.remark')">
          <el-input v-model="formData.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ t('agent.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitForm">{{ t('agent.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 充值弹窗 -->
    <el-dialog v-model="rechargeVisible" :title="t('agent.rechargeTitle')" width="420px">
      <el-form :model="rechargeForm" label-width="100px">
        <el-form-item :label="t('agent.agentLabel')">
          <span>{{ rechargeForm.username }} (ID: {{ rechargeForm.agentId }})</span>
        </el-form-item>
        <el-form-item :label="t('agent.currentBalance')">
          <span>¥{{ formatAmount(rechargeForm.currentBalance) }}</span>
        </el-form-item>
        <el-form-item :label="t('agent.rechargeAmount')">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="0.01"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('agent.remark')">
          <el-input v-model="rechargeForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">{{ t('agent.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRechargeSubmit">{{ t('agent.rechargeConfirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 封禁确认 -->
    <ConfirmDialog
      v-model="banVisible"
      :title="t('agent.banTitle')"
      type="danger"
      :message="t('agent.banMessage', { username: banRow?.username })"
      :sub-message="t('agent.banSubMessage')"
      :confirm-text="t('agent.banConfirm')"
      @confirm="doBan"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { agentApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'
import Decimal from 'decimal.js'

const { t } = useI18n()

const loading = ref(false)
const submitting = ref(false)
const treeVisible = ref(false)
const tableData = ref<any[]>([])
const treeData = ref<any[]>([])
const total = ref(0)
const banVisible = ref(false)
const banRow = ref<any>(null)
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const rechargeVisible = ref(false)

const filter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  parentId: undefined as number | undefined,
  status: undefined as number | undefined
})

const formData = reactive({
  id: undefined as number | undefined,
  tenantId: 1,
  parentId: 0,
  username: '',
  password: '',
  realName: '',
  contact: '',
  commissionRate: 10,
  maxSubLevel: 0,
  remark: ''
})

const formRules: FormRules = {
  username: [
    { required: true, message: t('agent.usernameRequired'), trigger: 'blur' },
    { min: 4, max: 32, message: t('agent.usernameLength'), trigger: 'blur' }
  ],
  password: [
    {
      validator: (_: any, value: string, callback: any) => {
        if (formMode.value === 'create') {
          if (!value) return callback(new Error(t('agent.passwordRequired')))
          if (value.length < 6) return callback(new Error(t('agent.passwordMinLength')))
        } else if (value && value.length < 6) {
          return callback(new Error(t('agent.passwordMinLength')))
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  commissionRate: [
    { required: true, message: t('agent.commissionRateRequired'), trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: t('agent.commissionRateRange'), trigger: 'blur' }
  ]
}

const rechargeForm = reactive({
  agentId: 0,
  username: '',
  currentBalance: 0,
  amount: 100,
  remark: ''
})

const formatAmount = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  try {
    return new Decimal(val).toFixed(2)
  } catch {
    return '0.00'
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const resp: any = await agentApi.page(filter)
    tableData.value = resp.records || []
    total.value = resp.total || 0
  } finally {
    loading.value = false
  }
}

const loadTree = async () => {
  treeVisible.value = !treeVisible.value
  if (treeVisible.value && treeData.value.length === 0) {
    try {
      treeData.value = await agentApi.tree(filter.tenantId, 0)
    } catch {
      // 静默
    }
  }
}

const handleReset = () => {
  filter.parentId = undefined
  filter.status = undefined
  filter.current = 1
  loadData()
}

const handleCreate = () => {
  formMode.value = 'create'
  formData.id = undefined
  formData.tenantId = filter.tenantId
  formData.parentId = 0
  formData.username = ''
  formData.password = ''
  formData.realName = ''
  formData.contact = ''
  formData.commissionRate = 10
  formData.maxSubLevel = 0
  formData.remark = ''
  formVisible.value = true
}

const handleEdit = (row: any) => {
  formMode.value = 'edit'
  formData.id = row.id
  formData.tenantId = row.tenantId
  formData.parentId = row.parentId || 0
  formData.username = row.username
  formData.password = ''
  formData.realName = row.realName || ''
  formData.contact = row.contact || ''
  formData.commissionRate = Number(row.commissionRate) || 0
  formData.maxSubLevel = row.maxSubLevel || 0
  formData.remark = row.remark || ''
  formVisible.value = true
}

const handleSubmitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      await agentApi.create(formData)
      ElMessage.success(t('agent.createSuccess'))
    } else {
      await agentApi.update(formData)
      ElMessage.success(t('agent.updateSuccess'))
    }
    formVisible.value = false
    loadData()
    if (treeVisible.value) {
      treeData.value = await agentApi.tree(filter.tenantId, 0)
    }
  } finally {
    submitting.value = false
  }
}

const handleRecharge = (row: any) => {
  rechargeForm.agentId = row.id
  rechargeForm.username = row.username
  rechargeForm.currentBalance = row.balance
  rechargeForm.amount = 100
  rechargeForm.remark = ''
  rechargeVisible.value = true
}

const handleRechargeSubmit = async () => {
  submitting.value = true
  try {
    await agentApi.recharge(filter.tenantId, rechargeForm.agentId, rechargeForm.amount, rechargeForm.remark)
    ElMessage.success(t('agent.rechargeSuccess'))
    rechargeVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleBan = (row: any) => {
  banRow.value = row
  banVisible.value = true
}

const doBan = async () => {
  if (!banRow.value) return
  try {
    await agentApi.ban(filter.tenantId, banRow.value.id)
    ElMessage.success(t('agent.banSuccess'))
    loadData()
  } catch {
    // 错误已在拦截器处理
  }
}

const handleUnban = async (row: any) => {
  try {
    await agentApi.unban(filter.tenantId, row.id)
    ElMessage.success(t('agent.unbanSuccess'))
    loadData()
  } catch {
    // 静默
  }
}

onMounted(() => {
  loadData()
})
</script>
