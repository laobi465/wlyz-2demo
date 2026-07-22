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
      <el-tab-pane label="函数列表" name="list">
        <el-card>
          <template #header>
            <div class="tab-header">
              <span class="jicek-card-title">云函数列表</span>
              <el-button type="primary" :icon="Plus" @click="handleCreate">新建函数</el-button>
            </div>
          </template>

          <!-- 筛选 -->
          <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
            <el-form-item label="软件ID">
              <el-input-number v-model="filter.softwareId" :min="1" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="函数名">
              <el-input v-model="filter.name" placeholder="支持模糊查询" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="filter.enabled" placeholder="全部" clearable style="width: 120px">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadList">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 表格 -->
          <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="softwareId" label="软件ID" width="90" />
            <el-table-column prop="name" label="函数名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column label="运行时" width="90">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.runtime || 'lua' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="timeoutMs" label="超时(ms)" width="100" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.enabled === 1" type="success" size="small">启用</el-tag>
                <el-tag v-else type="info" size="small">禁用</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="version" label="版本" width="70" />
            <el-table-column prop="invokeCount" label="调用次数" width="100" />
            <el-table-column prop="lastInvokeTime" label="最后调用" min-width="160" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button link type="success" size="small" @click="handleTest(row)">测试</el-button>
                <el-button
                  v-if="row.enabled === 1"
                  link
                  type="warning"
                  size="small"
                  @click="handleToggle(row, 0)"
                >
                  禁用
                </el-button>
                <el-button
                  v-else
                  link
                  type="success"
                  size="small"
                  @click="handleToggle(row, 1)"
                >
                  启用
                </el-button>
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
            @size-change="loadList"
            @current-change="loadList"
          />
        </el-card>
      </el-tab-pane>

      <!-- 执行日志 -->
      <el-tab-pane label="执行日志" name="log">
        <el-card>
          <template #header>
            <span class="jicek-card-title">云函数执行日志</span>
          </template>

          <el-form :inline="true" :model="logFilter" style="margin-bottom: 16px">
            <el-form-item label="函数ID">
              <el-input-number v-model="logFilter.functionId" :min="1" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="软件ID">
              <el-input-number v-model="logFilter.softwareId" :min="1" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="logFilter.status" placeholder="全部" clearable style="width: 150px">
                <el-option label="成功" :value="0" />
                <el-option label="编译失败" :value="1" />
                <el-option label="运行时错误" :value="2" />
                <el-option label="超时" :value="3" />
                <el-option label="内存超限" :value="4" />
                <el-option label="输入超限" :value="5" />
                <el-option label="输出超限" :value="6" />
              </el-select>
            </el-form-item>
            <el-form-item label="来源">
              <el-select v-model="logFilter.invokeSource" placeholder="全部" clearable style="width: 120px">
                <el-option label="开发者测试" value="dev" />
                <el-option label="SDK 调用" value="sdk" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadLogs">查询</el-button>
              <el-button @click="handleLogReset">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="logLoading" :data="logData" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="functionId" label="函数ID" width="90" />
            <el-table-column prop="functionName" label="函数名" min-width="140" show-overflow-tooltip />
            <el-table-column prop="softwareId" label="软件ID" width="90" />
            <el-table-column label="来源" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.invokeSource === 'dev'" size="small" type="info">开发者</el-tag>
                <el-tag v-else size="small">SDK</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="logStatusTag(row.status)" size="small">{{ logStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
            <el-table-column prop="inputSize" label="输入(B)" width="100" />
            <el-table-column prop="outputSize" label="输出(B)" width="100" />
            <el-table-column prop="callerIp" label="调用IP" width="130" />
            <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="执行时间" min-width="160" />
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
      :title="editForm.id ? '编辑云函数' : '新建云函数'"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px">
        <el-form-item label="软件ID" prop="softwareId">
          <el-input-number v-model="editForm.softwareId" :min="1" :disabled="!!editForm.id" style="width: 200px" />
        </el-form-item>
        <el-form-item label="函数名" prop="name">
          <el-input
            v-model="editForm.name"
            :disabled="!!editForm.id"
            placeholder="字母开头，仅含字母数字下划线，最长 64 字符"
            style="max-width: 320px"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" maxlength="255" show-word-count style="max-width: 600px" />
        </el-form-item>
        <el-form-item label="Lua 代码" prop="code">
          <div class="code-wrap">
            <el-input
              v-model="editForm.code"
              type="textarea"
              :rows="14"
              placeholder="-- 输入 Lua 代码，通过 jicek.input 获取输入，return 返回结果&#10;-- 示例：&#10;-- local s = jicek.input&#10;-- return string.upper(s)"
              resize="vertical"
              class="code-textarea"
            />
            <div class="code-meta">{{ codeBytesText }}</div>
          </div>
        </el-form-item>
        <el-form-item label="超时(ms)" prop="timeoutMs">
          <el-input-number v-model="editForm.timeoutMs" :min="100" :max="30000" :step="500" style="width: 200px" />
          <span class="form-tip">范围 100-30000，默认 3000</span>
        </el-form-item>
        <el-form-item label="内存上限(KB)" prop="memoryLimitKb">
          <el-input-number v-model="editForm.memoryLimitKb" :min="1" :max="262144" :step="1024" style="width: 200px" />
        </el-form-item>
        <el-form-item label="输入上限(KB)" prop="maxInputKb">
          <el-input-number v-model="editForm.maxInputKb" :min="1" :max="256" style="width: 200px" />
        </el-form-item>
        <el-form-item label="输出上限(KB)" prop="maxOutputKb">
          <el-input-number v-model="editForm.maxOutputKb" :min="1" :max="256" style="width: 200px" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试执行弹窗 -->
    <el-dialog
      v-model="testVisible"
      :title="`测试执行：${testRow?.name || ''}`"
      width="820px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item label="函数ID">
          <span>{{ testRow?.id }}</span>
        </el-form-item>
        <el-form-item label="输入(JSON)">
          <el-input
            v-model="testInput"
            type="textarea"
            :rows="6"
            placeholder='输入任意文本，沙箱内通过 jicek.input 获取（字符串）'
            class="code-textarea"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="invoking" @click="doInvoke">执行</el-button>
          <el-button @click="testInput = ''; testResult = null">清空</el-button>
        </el-form-item>
        <el-form-item v-if="testResult" label="执行结果">
          <div class="result-block">
            <div class="result-meta">
              <el-tag :type="logStatusTag(testResult.status)" size="small">
                {{ logStatusText(testResult.status) }}
              </el-tag>
              <span class="meta-item">耗时 {{ testResult.durationMs }}ms</span>
              <span class="meta-item">输入 {{ testResult.inputSize }}B</span>
              <span class="meta-item">输出 {{ testResult.outputSize }}B</span>
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
      title="删除云函数确认"
      type="danger"
      :message="`确认删除云函数 ${deleteRow?.name}?`"
      sub-message="删除后不可恢复，执行日志保留用于审计"
      confirm-text="确认删除"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { cloudFuncApi } from '@/api'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'

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
  softwareId: [{ required: true, message: '请输入软件ID', trigger: 'blur' }],
  name: [
    { required: true, message: '请输入函数名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{0,63}$/,
      message: '字母开头，仅含字母数字下划线，最长 64 字符',
      trigger: 'blur'
    }
  ],
  code: [{ required: true, message: '请输入 Lua 代码', trigger: 'blur' }],
  timeoutMs: [{ required: true, message: '请输入超时', trigger: 'blur' }]
}

const codeBytesText = computed(() => {
  const bytes = new Blob([editForm.code || '']).size
  return `${bytes} / 65536 字节`
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
      ElMessage.success(editForm.id ? '更新成功' : '创建成功')
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
    ElMessage.success(enabled ? '已启用' : '已禁用')
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
    ElMessage.success('已删除')
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
      ElMessage.success(`执行成功，耗时 ${result.durationMs}ms`)
    } else {
      ElMessage.warning(`执行失败：${logStatusText(result.status)}`)
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
    0: '成功',
    1: '编译失败',
    2: '运行时错误',
    3: '超时',
    4: '内存超限',
    5: '输入超限',
    6: '输出超限'
  } as Record<number, string>)[status] || '未知'
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
