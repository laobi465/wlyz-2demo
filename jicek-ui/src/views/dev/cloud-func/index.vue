<!--
  极策k 云函数管理页面
  作者: 极策k  日期: 2026-07-22

  功能：云函数 CRUD + 测试运行 + 执行日志查询（单页 Tab 切换）
  接口：
    POST   /api/dev/cloud-func                          新建/更新
    GET    /api/dev/cloud-func/page                     分页
    GET    /api/dev/cloud-func/{tenantId}/{functionId}  详情
    DELETE /api/dev/cloud-func/{tenantId}/{functionId}  删除
    POST   /api/dev/cloud-func/toggle-enabled           启用/禁用
    POST   /api/dev/cloud-func/invoke                   测试执行
    GET    /api/dev/cloud-func/log/page                 执行日志分页
  安全：
    - 代码编辑器：textarea + 等宽字体，编辑时显示行数
    - 测试执行显示状态码（0成功/1-6各类失败）+ 耗时 + 输入输出大小
    - 删除需二次确认
-->
<template>
  <div class="jicek-page">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 函数列表 -->
      <el-tab-pane :label="t('cloudFunc.listTab')" name="list">
        <el-card>
          <template #header>
            <div class="tab-header">
              <span class="jicek-card-title">{{ t('cloudFunc.listTitle') }}</span>
              <el-button type="primary" :icon="Plus" @click="handleCreate">{{ t('cloudFunc.create') }}</el-button>
            </div>
          </template>

          <!-- 筛选 -->
          <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
            <el-form-item :label="t('cloudFunc.softwareId')">
              <el-input-number v-model="filter.softwareId" :min="1" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item :label="t('cloudFunc.functionName')">
              <el-input v-model="filter.name" :placeholder="t('cloudFunc.functionNamePlaceholder')" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item :label="t('cloudFunc.status')">
              <el-select v-model="filter.enabled" :placeholder="t('common.all')" clearable style="width: 120px">
                <el-option :label="t('cloudFunc.enabled')" :value="1" />
                <el-option :label="t('cloudFunc.disabled')" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">{{ t('common.search') }}</el-button>
              <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
            </el-form-item>
          </el-form>

          <!-- 表格 -->
          <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="softwareId" :label="t('cloudFunc.softwareId')" width="90" />
            <el-table-column prop="name" :label="t('cloudFunc.functionName')" min-width="140" show-overflow-tooltip />
            <el-table-column prop="description" :label="t('cloudFunc.description')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="t('cloudFunc.runtime')" width="90">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.runtime || 'lua' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="timeoutMs" :label="t('cloudFunc.timeoutMs')" width="100" />
            <el-table-column :label="t('cloudFunc.status')" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.enabled === 1" type="success" size="small">{{ t('cloudFunc.enabled') }}</el-tag>
                <el-tag v-else type="info" size="small">{{ t('cloudFunc.disabled') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="version" :label="t('cloudFunc.version')" width="70" />
            <el-table-column prop="invokeCount" :label="t('cloudFunc.invokeCount')" width="100" />
            <el-table-column prop="lastInvokeTime" :label="t('cloudFunc.lastInvokeTime')" min-width="160" />
            <el-table-column :label="t('common.operation')" width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEdit(row)">{{ t('cloudFunc.edit') }}</el-button>
                <el-button link type="success" size="small" @click="handleTest(row)">{{ t('cloudFunc.test') }}</el-button>
                <el-button
                  v-if="row.enabled === 1"
                  link
                  type="warning"
                  size="small"
                  @click="handleToggle(row, 0)"
                >
                  {{ t('cloudFunc.disable') }}
                </el-button>
                <el-button
                  v-else
                  link
                  type="success"
                  size="small"
                  @click="handleToggle(row, 1)"
                >
                  {{ t('cloudFunc.enable') }}
                </el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row)">{{ t('cloudFunc.delete') }}</el-button>
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
            @size-change="loadList"
            @current-change="loadList"
          />
        </el-card>
      </el-tab-pane>

      <!-- 执行日志 -->
      <el-tab-pane :label="t('cloudFunc.logTab')" name="log">
        <el-card>
          <template #header>
            <span class="jicek-card-title">{{ t('cloudFunc.logTitle') }}</span>
          </template>

          <el-form :inline="true" :model="logFilter" style="margin-bottom: 16px">
            <el-form-item :label="t('cloudFunc.functionId')">
              <el-input-number v-model="logFilter.functionId" :min="1" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item :label="t('cloudFunc.softwareId')">
              <el-input-number v-model="logFilter.softwareId" :min="1" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item :label="t('cloudFunc.status')">
              <el-select v-model="logFilter.status" :placeholder="t('common.all')" clearable style="width: 150px">
                <el-option :label="t('cloudFunc.logStatusSuccess')" :value="0" />
                <el-option :label="t('cloudFunc.logStatusCompileFail')" :value="1" />
                <el-option :label="t('cloudFunc.logStatusRuntimeError')" :value="2" />
                <el-option :label="t('cloudFunc.logStatusTimeout')" :value="3" />
                <el-option :label="t('cloudFunc.logStatusMemoryLimit')" :value="4" />
                <el-option :label="t('cloudFunc.logStatusInputLimit')" :value="5" />
                <el-option :label="t('cloudFunc.logStatusOutputLimit')" :value="6" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('cloudFunc.invokeSource')">
              <el-select v-model="logFilter.invokeSource" :placeholder="t('common.all')" clearable style="width: 120px">
                <el-option :label="t('cloudFunc.sourceDev')" value="dev" />
                <el-option :label="t('cloudFunc.sourceSdk')" value="sdk" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadLogs">{{ t('common.search') }}</el-button>
              <el-button @click="handleLogReset">{{ t('common.reset') }}</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="logLoading" :data="logData" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="functionId" :label="t('cloudFunc.functionId')" width="90" />
            <el-table-column prop="functionName" :label="t('cloudFunc.functionName')" min-width="140" show-overflow-tooltip />
            <el-table-column prop="softwareId" :label="t('cloudFunc.softwareId')" width="90" />
            <el-table-column :label="t('cloudFunc.invokeSource')" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.invokeSource === 'dev'" size="small" type="info">{{ t('cloudFunc.sourceDevTag') }}</el-tag>
                <el-tag v-else size="small">{{ t('cloudFunc.sourceSdkTag') }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('cloudFunc.status')" width="110">
              <template #default="{ row }">
                <el-tag :type="logStatusTag(row.status)" size="small">{{ logStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="durationMs" :label="t('cloudFunc.durationMs')" width="100" />
            <el-table-column prop="inputSize" :label="t('cloudFunc.inputB')" width="100" />
            <el-table-column prop="outputSize" :label="t('cloudFunc.outputB')" width="100" />
            <el-table-column prop="callerIp" :label="t('cloudFunc.callerIp')" width="130" />
            <el-table-column prop="errorMessage" :label="t('cloudFunc.errorMessage')" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" :label="t('cloudFunc.executeTime')" min-width="160" />
          </el-table>

          <el-pagination
            v-model:current-page="logFilter.current"
            v-model:page-size="logFilter.size"
            :total="logTotal"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="loadLogs"
            @current-change="loadLogs"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 编辑/新建弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editForm.id ? t('cloudFunc.editTitleEdit') : t('cloudFunc.editTitleCreate')"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item :label="t('cloudFunc.softwareIdLabel')" prop="softwareId">
          <el-input-number v-model="editForm.softwareId" :min="1" :disabled="!!editForm.id" style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('cloudFunc.functionNameLabel')" prop="name">
          <el-input
            v-model="editForm.name"
            :disabled="!!editForm.id"
            :placeholder="t('cloudFunc.functionNameInputPlaceholder')"
            style="max-width: 320px"
          />
        </el-form-item>
        <el-form-item :label="t('cloudFunc.descriptionLabel')">
          <el-input v-model="editForm.description" maxlength="255" show-word-count style="max-width: 600px" />
        </el-form-item>
        <el-form-item :label="t('cloudFunc.codeLabel')" prop="code">
          <div class="code-wrap">
            <el-input
              v-model="editForm.code"
              type="textarea"
              :rows="14"
              :placeholder="t('cloudFunc.codePlaceholder')"
              resize="vertical"
              class="code-textarea"
            />
            <div class="code-meta">{{ codeBytesText }}</div>
          </div>
        </el-form-item>
        <el-form-item :label="t('cloudFunc.timeoutMsLabel')" prop="timeoutMs">
          <el-input-number v-model="editForm.timeoutMs" :min="100" :max="30000" :step="500" style="width: 200px" />
          <span class="form-tip">{{ t('cloudFunc.timeoutMsHint') }}</span>
        </el-form-item>
        <el-form-item :label="t('cloudFunc.memoryLimitKb')" prop="memoryLimitKb">
          <el-input-number v-model="editForm.memoryLimitKb" :min="1" :max="262144" :step="1024" style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('cloudFunc.maxInputKb')" prop="maxInputKb">
          <el-input-number v-model="editForm.maxInputKb" :min="1" :max="256" style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('cloudFunc.maxOutputKb')" prop="maxOutputKb">
          <el-input-number v-model="editForm.maxOutputKb" :min="1" :max="256" style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('cloudFunc.enabledLabel')">
          <el-switch v-model="editForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('cloudFunc.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ t('cloudFunc.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 测试执行弹窗 -->
    <el-dialog
      v-model="testVisible"
      :title="t('cloudFunc.testTitle', { name: testRow?.name || '' })"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item :label="t('cloudFunc.functionIdLabel')">
          <span>{{ testRow?.id }}</span>
        </el-form-item>
        <el-form-item :label="t('cloudFunc.inputLabel')">
          <el-input
            v-model="testInput"
            type="textarea"
            :rows="6"
            :placeholder="t('cloudFunc.inputPlaceholder')"
            class="code-textarea"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="invoking" @click="doInvoke">{{ t('cloudFunc.execute') }}</el-button>
          <el-button @click="testInput = ''; testResult = null">{{ t('cloudFunc.clear') }}</el-button>
        </el-form-item>
        <el-form-item v-if="testResult" :label="t('cloudFunc.resultLabel')">
          <div class="result-block">
            <div class="result-meta">
              <el-tag :type="logStatusTag(testResult.status)" size="small">
                {{ logStatusText(testResult.status) }}
              </el-tag>
              <span class="meta-item">{{ t('cloudFunc.durationLabel', { ms: testResult.durationMs }) }}</span>
              <span class="meta-item">{{ t('cloudFunc.inputSizeLabel', { size: testResult.inputSize }) }}</span>
              <span class="meta-item">{{ t('cloudFunc.outputSizeLabel', { size: testResult.outputSize }) }}</span>
            </div>
            <pre v-if="testResult.result" class="result-output">{{ formatResult(testResult.result) }}</pre>
            <div v-if="testResult.errorMessage" class="result-error">{{ testResult.errorMessage }}</div>
          </div>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 删除确认 -->
    <ConfirmDialog
      v-model="deleteVisible"
      :title="t('cloudFunc.deleteTitle')"
      type="danger"
      :message="t('cloudFunc.deleteMessage', { name: deleteRow?.name })"
      :sub-message="t('cloudFunc.deleteSubMessage')"
      :confirm-text="t('cloudFunc.deleteConfirm')"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { cloudFuncApi } from '@/api'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'

const { t } = useI18n()

const activeTab = ref<'list' | 'log'>('list')

/* ============ 函数列表 ============ */
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const filter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  softwareId: undefined as number | undefined,
  name: '',
  enabled: undefined as number | undefined
})

const loadList = async () => {
  loading.value = true
  try {
    const resp: any = await cloudFuncApi.page(filter)
    tableData.value = resp.records || []
    total.value = resp.total || 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filter.softwareId = undefined
  filter.name = ''
  filter.enabled = undefined
  filter.current = 1
  loadList()
}

/* ============ 编辑/新建 ============ */
const editVisible = ref(false)
const editFormRef = ref<FormInstance>()
const saving = ref(false)
const editForm = reactive({
  id: undefined as number | undefined,
  tenantId: 1,
  softwareId: undefined as number | undefined,
  name: '',
  description: '',
  code: '',
  timeoutMs: 3000,
  memoryLimitKb: 8192,
  maxInputKb: 32,
  maxOutputKb: 32,
  enabled: 1
})

const editRules: FormRules = {
  softwareId: [{ required: true, message: t('cloudFunc.softwareIdRequired'), trigger: 'blur' }],
  name: [
    { required: true, message: t('cloudFunc.functionNameRequired'), trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{0,63}$/,
      message: t('cloudFunc.functionNamePattern'),
      trigger: 'blur'
    }
  ],
  code: [{ required: true, message: t('cloudFunc.codeRequired'), trigger: 'blur' }],
  timeoutMs: [{ required: true, message: t('cloudFunc.timeoutMsRequired'), trigger: 'blur' }]
}

const codeBytesText = computed(() => {
  const bytes = new Blob([editForm.code || '']).size
  return t('cloudFunc.codeBytes', { bytes })
})

const handleCreate = () => {
  Object.assign(editForm, {
    id: undefined,
    softwareId: undefined,
    name: '',
    description: '',
    code: '',
    timeoutMs: 3000,
    memoryLimitKb: 8192,
    maxInputKb: 32,
    maxOutputKb: 32,
    enabled: 1
  })
  editVisible.value = true
}

const handleEdit = async (row: any) => {
  try {
    const data: any = await cloudFuncApi.get(filter.tenantId, row.id)
    Object.assign(editForm, {
      id: data.id,
      tenantId: data.tenantId,
      softwareId: data.softwareId,
      name: data.name,
      description: data.description || '',
      code: data.code || '',
      timeoutMs: data.timeoutMs || 3000,
      memoryLimitKb: data.memoryLimitKb || 8192,
      maxInputKb: data.maxInputKb || 32,
      maxOutputKb: data.maxOutputKb || 32,
      enabled: data.enabled ?? 1
    })
    editVisible.value = true
  } catch {
    // 错误已拦截器处理
  }
}

const handleSave = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await cloudFuncApi.save({ ...editForm })
      ElMessage.success(editForm.id ? t('cloudFunc.updateSuccess') : t('cloudFunc.createSuccess'))
      editVisible.value = false
      loadList()
    } finally {
      saving.value = false
    }
  })
}

const handleToggle = async (row: any, enabled: number) => {
  try {
    await cloudFuncApi.toggleEnabled(filter.tenantId, row.id, enabled)
    ElMessage.success(enabled ? t('cloudFunc.enabledSuccess') : t('cloudFunc.disabledSuccess'))
    loadList()
  } catch {
    // 静默
  }
}

const handleDelete = (row: any) => {
  deleteRow.value = row
  deleteVisible.value = true
}

const doDelete = async () => {
  if (!deleteRow.value) return
  try {
    await cloudFuncApi.delete(filter.tenantId, deleteRow.value.id)
    ElMessage.success(t('cloudFunc.deleted'))
    loadList()
  } catch {
    // 静默
  }
}

/* ============ 测试执行 ============ */
const testVisible = ref(false)
const testRow = ref<any>(null)
const testInput = ref('')
const testResult = ref<any>(null)
const invoking = ref(false)

const handleTest = (row: any) => {
  testRow.value = row
  testInput.value = ''
  testResult.value = null
  testVisible.value = true
}

const doInvoke = async () => {
  if (!testRow.value) return
  invoking.value = true
  try {
    const result: any = await cloudFuncApi.invoke({
      tenantId: filter.tenantId,
      softwareId: testRow.value.softwareId,
      functionId: testRow.value.id,
      input: testInput.value
    })
    testResult.value = result
    if (result.status === 0) {
      ElMessage.success(t('cloudFunc.executeSuccess', { ms: result.durationMs }))
    } else {
      ElMessage.warning(t('cloudFunc.executeFailed', { reason: logStatusText(result.status) }))
    }
  } finally {
    invoking.value = false
  }
}

const formatResult = (s: string) => {
  // 尝试格式化 JSON
  try {
    return JSON.stringify(JSON.parse(s), null, 2)
  } catch {
    return s
  }
}

/* ============ 删除确认 ============ */
const deleteVisible = ref(false)
const deleteRow = ref<any>(null)

/* ============ 执行日志 ============ */
const logLoading = ref(false)
const logData = ref<any[]>([])
const logTotal = ref(0)
const logFilter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  functionId: undefined as number | undefined,
  softwareId: undefined as number | undefined,
  status: undefined as number | undefined,
  invokeSource: ''
})

const loadLogs = async () => {
  logLoading.value = true
  try {
    const resp: any = await cloudFuncApi.logPage(logFilter)
    logData.value = resp.records || []
    logTotal.value = resp.total || 0
  } finally {
    logLoading.value = false
  }
}

const handleLogReset = () => {
  logFilter.functionId = undefined
  logFilter.softwareId = undefined
  logFilter.status = undefined
  logFilter.invokeSource = ''
  logFilter.current = 1
  loadLogs()
}

const logStatusText = (status: number) => {
  return ({
    0: t('cloudFunc.logStatusSuccess'),
    1: t('cloudFunc.logStatusCompileFail'),
    2: t('cloudFunc.logStatusRuntimeError'),
    3: t('cloudFunc.logStatusTimeout'),
    4: t('cloudFunc.logStatusMemoryLimit'),
    5: t('cloudFunc.logStatusInputLimit'),
    6: t('cloudFunc.logStatusOutputLimit')
  } as Record<number, string>)[status] || t('cloudFunc.logStatusUnknown')
}

const logStatusTag = (status: number) => {
  return ({
    0: 'success',
    1: 'warning',
    2: 'danger',
    3: 'danger',
    4: 'danger',
    5: 'warning',
    6: 'warning'
  } as Record<number, string>)[status] || 'info'
}

/* ============ 初始化 ============ */
onMounted(() => {
  loadList()
})
</script>

<style scoped>
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: var(--jicek-text-secondary);
  margin-left: 12px;
}

.code-wrap {
  width: 100%;
}

.code-textarea :deep(textarea) {
  font-family: var(--jicek-font-mono);
  font-size: 13px;
  line-height: 1.6;
}

.code-meta {
  font-size: 12px;
  color: var(--jicek-text-secondary);
  margin-top: 4px;
  text-align: right;
}

.result-block {
  width: 100%;
  border: 1px solid var(--jicek-border);
  border-radius: 6px;
  padding: 12px;
  background: var(--jicek-bg-secondary);
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--jicek-text-secondary);
}

.meta-item {
  font-size: 12px;
}

.result-output {
  font-family: var(--jicek-font-mono);
  font-size: 13px;
  background: #fff;
  border: 1px solid var(--jicek-border);
  border-radius: 4px;
  padding: 8px;
  margin: 0;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--jicek-text-primary);
}

.result-error {
  margin-top: 8px;
  padding: 8px;
  background: rgba(178, 58, 58, 0.06);
  border: 1px solid rgba(178, 58, 58, 0.2);
  border-radius: 4px;
  color: var(--jicek-danger);
  font-size: 13px;
  word-break: break-all;
}
</style>
