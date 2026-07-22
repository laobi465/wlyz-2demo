<!--
  极策k 卡类管理页面
  作者: 极策k  日期: 2026-07-22

  功能：卡类 CRUD（时长卡/次数卡/功能卡/永久卡）+ 定价 + 绑定策略
  接口：
    GET  /api/dev/card-type/page    分页查询
    POST /api/dev/card-type         新建/更新
    GET  /api/dev/card-type/{id}    详情
    DELETE /api/dev/card-type/{id}  删除
  安全：价格用 decimal.js 精度；删除需二次确认；type 切换联动显示字段
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">卡类管理</span>
        <el-button type="primary" style="float: right" @click="handleCreate">新增卡类</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="软件ID">
          <el-input-number v-model="filter.softwareId" :min="1" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="卡类名称" min-width="140" />
        <el-table-column prop="softwareId" label="软件ID" width="90" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规格" width="140">
          <template #default="{ row }">{{ specText(row) }}</template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ formatAmount(row.price) }}</template>
        </el-table-column>
        <el-table-column label="绑定策略" width="120">
          <template #default="{ row }">{{ bindStrategyText(row.bindStrategy, row.maxDevices) }}</template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增卡类' : '编辑卡类'"
      width="600px"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="所属软件ID" prop="softwareId">
          <el-input-number v-model="formData.softwareId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="卡类名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：月卡 / 季卡 / 100次卡" />
        </el-form-item>
        <el-form-item label="卡类类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio :value="1">时长卡</el-radio>
            <el-radio :value="2">次数卡</el-radio>
            <el-radio :value="3">功能卡</el-radio>
            <el-radio :value="4">永久卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.type === 1" label="时长（秒）" prop="duration">
          <el-input-number v-model="formData.duration" :min="1" style="width: 100%" />
          <span class="form-tip">{{ durationHint }}</span>
        </el-form-item>
        <el-form-item v-if="formData.type === 2" label="次数" prop="count">
          <el-input-number v-model="formData.count" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="formData.type === 3" label="功能列表" prop="features">
          <el-input
            v-model="formData.features"
            type="textarea"
            :rows="3"
            placeholder='JSON 数组，如 ["feature_a","feature_b"]'
          />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="formData.price"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="绑定策略" prop="bindStrategy">
          <el-radio-group v-model="formData.bindStrategy">
            <el-radio :value="0">不绑定</el-radio>
            <el-radio :value="1">首次登录绑定</el-radio>
            <el-radio :value="2">指定 N 台</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.bindStrategy === 2" label="最大设备数" prop="maxDevices">
          <el-input-number v-model="formData.maxDevices" :min="1" :max="99" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="formData.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitForm">确认</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认 -->
    <ConfirmDialog
      v-model="deleteVisible"
      title="删除卡类确认"
      type="danger"
      :message="`确认删除卡类「${deleteRow?.name}」?`"
      sub-message="删除后已生成的卡密不受影响，但无法再以此卡类生成新卡密"
      confirm-text="确认删除"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { cardTypeApi } from '@/api'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'
import Decimal from 'decimal.js'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const deleteVisible = ref(false)
const deleteRow = ref<any>(null)

const filter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  softwareId: undefined as number | undefined
})

const formData = reactive({
  id: undefined as number | undefined,
  tenantId: 1,
  softwareId: undefined as number | undefined,
  name: '',
  type: 1,
  duration: 2592000,
  count: 100,
  features: '',
  price: 10,
  bindStrategy: 1,
  maxDevices: 1,
  enabled: 1
})

const formRules: FormRules = {
  softwareId: [{ required: true, message: '请输入软件 ID', trigger: 'blur' }],
  name: [
    { required: true, message: '请输入卡类名称', trigger: 'blur' },
    { min: 1, max: 64, message: '1-64 字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择卡类类型', trigger: 'change' }],
  duration: [{ required: true, message: '请输入时长', trigger: 'blur' }],
  count: [{ required: true, message: '请输入次数', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const durationHint = computed(() => {
  const d = formData.duration || 0
  const days = Math.floor(d / 86400)
  const hours = Math.floor((d % 86400) / 3600)
  if (days > 0 && hours > 0) return `${days} 天 ${hours} 小时`
  if (days > 0) return `${days} 天`
  if (hours > 0) return `${hours} 小时`
  return `${d} 秒`
})

const formatAmount = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  try {
    return new Decimal(val).toFixed(2)
  } catch {
    return '0.00'
  }
}

const typeText = (type: number) => {
  return ({ 1: '时长卡', 2: '次数卡', 3: '功能卡', 4: '永久卡' } as Record<number, string>)[type] || '未知'
}

const typeTagType = (type: number) => {
  return ({ 1: '', 2: 'success', 3: 'warning', 4: 'info' } as Record<number, string>)[type] || ''
}

const specText = (row: any) => {
  switch (row.type) {
    case 1: {
      const d = row.duration || 0
      const days = Math.floor(d / 86400)
      return days > 0 ? `${days} 天` : `${d} 秒`
    }
    case 2:
      return `${row.count} 次`
    case 3:
      return '功能卡'
    case 4:
      return '永久'
    default:
      return '-'
  }
}

const bindStrategyText = (strategy: number, maxDevices: number) => {
  if (strategy === 0) return '不绑定'
  if (strategy === 1) return '首次登录绑定'
  if (strategy === 2) return `指定 ${maxDevices} 台`
  return '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const resp: any = await cardTypeApi.page(filter)
    tableData.value = resp.records || []
    total.value = resp.total || 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filter.softwareId = undefined
  filter.current = 1
  loadData()
}

const handleCreate = () => {
  formMode.value = 'create'
  formData.id = undefined
  formData.tenantId = filter.tenantId
  formData.softwareId = undefined
  formData.name = ''
  formData.type = 1
  formData.duration = 2592000
  formData.count = 100
  formData.features = ''
  formData.price = 10
  formData.bindStrategy = 1
  formData.maxDevices = 1
  formData.enabled = 1
  formVisible.value = true
}

const handleEdit = (row: any) => {
  formMode.value = 'edit'
  formData.id = row.id
  formData.tenantId = row.tenantId
  formData.softwareId = row.softwareId
  formData.name = row.name
  formData.type = row.type
  formData.duration = row.duration || 2592000
  formData.count = row.count || 100
  formData.features = row.features || ''
  formData.price = Number(row.price) || 0
  formData.bindStrategy = row.bindStrategy ?? 1
  formData.maxDevices = row.maxDevices || 1
  formData.enabled = row.enabled ?? 1
  formVisible.value = true
}

const handleSubmitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await cardTypeApi.save(formData)
    ElMessage.success(formMode.value === 'create' ? '卡类创建成功' : '卡类更新成功')
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = (row: any) => {
  deleteRow.value = row
  deleteVisible.value = true
}

const doDelete = async () => {
  if (!deleteRow.value) return
  try {
    await cardTypeApi.delete(deleteRow.value.id)
    ElMessage.success('卡类已删除')
    loadData()
  } catch {
    // 错误已在拦截器处理
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.form-tip {
  color: var(--jicek-text-secondary);
  font-size: 12px;
  line-height: 1.4;
}
</style>
