<!--
  极策k 更新包管理页面
  作者: 极策k  日期: 2026-07-22

  v0.11.0 多格式更新包（exe/sh/win/lua/zip/7z）：
   - 开发者上传文件 → 创建草稿 → 发布 → SDK 检查更新
   - 状态机：草稿(0) → 已发布(1) → 已下线(2)，不可逆
   - 通道：1稳定版 2内测版（SDK 可按通道拉取）
   - 仅草稿可编辑（仅改 releaseNotes/版本范围/强制更新，文件不可改）
   - 强制更新：旧版拒绝运行
   - SHA-256 客户端下载后校验完整性

  接口：
    POST   /api/dev/update-package/upload      上传文件（multipart）
    GET    /api/dev/update-package/page        分页查询
    GET    /api/dev/update-package/{id}        详情
    POST   /api/dev/update-package             创建（草稿）
    PUT    /api/dev/update-package             编辑（仅草稿）
    DELETE /api/dev/update-package/{id}        删除（含物理文件）
    POST   /api/dev/update-package/{id}/publish  发布
    POST   /api/dev/update-package/{id}/offline  下线
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">更新包管理</span>
        <el-button type="primary" style="float: right" @click="handleCreate">新增更新包</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="软件">
          <el-select v-model="filter.softwareId" placeholder="全部软件" clearable style="width: 180px" @change="loadData">
            <el-option v-for="sw in softwareList" :key="sw.id" :label="sw.name" :value="sw.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="通道">
          <el-select v-model="filter.channel" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="稳定版" :value="1" />
            <el-option label="内测版" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="filter.version" placeholder="如 1.2.0" clearable style="width: 140px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="softwareId" label="软件ID" width="90" />
        <el-table-column prop="version" label="版本" width="100">
          <template #default="{ row }">
            <span style="font-weight: 600">v{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="channel" label="通道" width="90">
          <template #default="{ row }">
            <el-tag :type="row.channel === 1 ? 'success' : 'warning'" size="small">
              {{ row.channel === 1 ? '稳定版' : '内测版' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="格式" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="forceUpdate" label="强制" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.forceUpdate === 1" type="danger" size="small">强制</el-tag>
            <span v-else style="color: var(--jicek-text-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载次数" width="90" />
        <el-table-column prop="publishTime" label="发布时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" link type="success" size="small" @click="handlePublish(row)">发布</el-button>
            <el-button v-if="row.status === 1" link type="warning" size="small" @click="handleOffline(row)">下线</el-button>
            <el-button link type="info" size="small" @click="handleView(row)">查看</el-button>
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
      v-model="formDialogVisible"
      :title="formMode === 'create' ? '新增更新包' : '编辑更新包'"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="所属软件" prop="softwareId">
          <el-select v-model="form.softwareId" placeholder="选择软件" style="width: 100%" :disabled="formMode === 'edit'">
            <el-option v-for="sw in softwareList" :key="sw.id" :label="sw.name" :value="sw.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="如 1.2.0" :disabled="formMode === 'edit'" />
        </el-form-item>
        <el-form-item label="通道" prop="channel">
          <el-radio-group v-model="form.channel" :disabled="formMode === 'edit'">
            <el-radio :value="1">稳定版</el-radio>
            <el-radio :value="2">内测版</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formMode === 'create'" label="更新包文件" prop="filePath">
          <el-upload
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            :http-request="handleUpload"
            :accept="'.exe,.sh,.win,.lua,.zip,.7z'"
          >
            <el-button type="primary" :loading="uploading">
              {{ uploadInfo ? '重新选择文件' : '选择文件上传' }}
            </el-button>
            <template #tip>
              <div style="color: var(--jicek-text-secondary); font-size: 12px; line-height: 1.6">
                支持 exe/sh/win/lua/zip/7z，最大 500MB<br />
                文件上传后自动计算 SHA-256 用于客户端完整性校验
              </div>
            </template>
          </el-upload>
          <!-- 上传进度 -->
          <el-progress v-if="uploading" :percentage="uploadPercent" style="margin-top: 8px" />
          <!-- 上传结果 -->
          <div v-if="uploadInfo" style="margin-top: 8px">
            <el-tag type="success" size="small">{{ uploadInfo.fileName }}</el-tag>
            <span style="margin-left: 8px; color: var(--jicek-text-secondary)">
              {{ formatSize(uploadInfo.fileSize) }} · SHA-256: {{ uploadInfo.fileSha256.substring(0, 16) }}...
            </span>
          </div>
        </el-form-item>
        <el-form-item v-else label="文件信息">
          <div>
            <el-tag size="small">{{ form.fileType }}</el-tag>
            <span style="margin-left: 8px">{{ form.fileName }} · {{ formatSize(form.fileSize) }}</span>
            <div style="color: var(--jicek-text-secondary); font-size: 12px; margin-top: 4px">
              SHA-256: {{ form.fileSha256 }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="更新说明">
          <el-input v-model="form.releaseNotes" type="textarea" :rows="5" placeholder="支持 Markdown，客户端展示" />
        </el-form-item>
        <el-form-item label="客户端版本范围">
          <el-input v-model="form.minClientVersion" placeholder="最低版本（含），如 1.0.0" style="width: 200px" />
          <span style="margin: 0 8px; color: var(--jicek-text-secondary)">~</span>
          <el-input v-model="form.maxClientVersion" placeholder="最高版本（含），留空不限" style="width: 200px" />
        </el-form-item>
        <el-form-item label="强制更新">
          <el-switch v-model="form.forceUpdate" :active-value="1" :inactive-value="0" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">开启后旧版客户端拒绝运行</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" :disabled="formMode === 'create' && !uploadInfo" @click="submitForm">
          保存为草稿
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="更新包详情" width="680px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="版本">v{{ viewData.version }}</el-descriptions-item>
        <el-descriptions-item label="通道">{{ viewData.channel === 1 ? '稳定版' : '内测版' }}</el-descriptions-item>
        <el-descriptions-item label="格式">{{ viewData.fileType }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ viewData.fileName }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ formatSize(viewData.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="SHA-256">
          <span style="word-break: break-all; font-family: monospace; font-size: 12px">{{ viewData.fileSha256 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(viewData.status)" size="small">{{ statusText(viewData.status) }}</el-tag>
          <el-tag v-if="viewData.forceUpdate === 1" type="danger" size="small" style="margin-left: 8px">强制更新</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户端版本范围">
          {{ viewData.minClientVersion || '*' }} ~ {{ viewData.maxClientVersion || '*' }}
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ viewData.publishTime || '未发布' }}</el-descriptions-item>
        <el-descriptions-item label="下线时间">{{ viewData.offlineTime || '未下线' }}</el-descriptions-item>
        <el-descriptions-item label="下载次数">{{ viewData.downloadCount }}</el-descriptions-item>
        <el-descriptions-item label="更新说明">
          <div style="white-space: pre-wrap; max-height: 300px; overflow-y: auto">{{ viewData.releaseNotes || '无' }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { updatePackageApi, softwareApi } from '@/api'

interface UpdatePackageRow {
  id: number
  tenantId: number
  softwareId: number
  version: string
  channel: number
  fileType: string
  fileName: string
  filePath: string
  fileSize: number
  fileSha256: string
  releaseNotes: string
  minClientVersion: string
  maxClientVersion: string
  status: number
  forceUpdate: number
  downloadCount: number
  publishTime: string
  offlineTime: string
  creatorId: number
  createTime: string
  updateTime: string
}

interface SoftwareOption {
  id: number
  name: string
}

interface UploadInfo {
  filePath: string
  fileName: string
  fileSize: number
  fileSha256: string
  fileType: string
}

const loading = ref(false)
const tableData = ref<UpdatePackageRow[]>([])
const total = ref(0)
const softwareList = ref<SoftwareOption[]>([])

const filter = reactive({
  current: 1,
  size: 20,
  softwareId: undefined as number | undefined,
  status: undefined as number | undefined,
  channel: undefined as number | undefined,
  version: ''
})

async function loadSoftwareList() {
  try {
    const res: any = await softwareApi.page({ current: 1, size: 100 })
    softwareList.value = (res.records || []).map((s: any) => ({ id: s.id, name: s.name }))
  } catch {
    // 拦截器已提示
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await updatePackageApi.page({
      current: filter.current,
      size: filter.size,
      softwareId: filter.softwareId,
      status: filter.status,
      channel: filter.channel,
      version: filter.version || undefined
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filter.softwareId = undefined
  filter.status = undefined
  filter.channel = undefined
  filter.version = ''
  filter.current = 1
  loadData()
}

onMounted(() => {
  loadSoftwareList()
  loadData()
})

/* ============ 文本/样式辅助 ============ */
function statusText(s: number): string {
  return { 0: '草稿', 1: '已发布', 2: '已下线' }[s] || '未知'
}
function statusTagType(s: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  return ({ 0: 'info', 1: 'success', 2: 'danger' } as const)[s] || ''
}
function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i === 0 ? 0 : 2) + ' ' + units[i]
}

/* ============ 新建/编辑 ============ */
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const uploading = ref(false)
const uploadPercent = ref(0)
const uploadInfo = ref<UploadInfo | null>(null)

const form = reactive({
  id: undefined as number | undefined,
  softwareId: undefined as number | undefined,
  version: '',
  channel: 1,
  filePath: '',
  fileName: '',
  fileSize: 0,
  fileSha256: '',
  fileType: '',
  releaseNotes: '',
  minClientVersion: '',
  maxClientVersion: '',
  forceUpdate: 0
})

const rules: FormRules = {
  softwareId: [{ required: true, message: '请选择软件', trigger: 'change' }],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { pattern: /^\d+\.\d+\.\d+/, message: '版本号格式 X.Y.Z', trigger: 'blur' }
  ],
  channel: [{ required: true, message: '请选择通道', trigger: 'change' }]
}

function handleCreate() {
  formMode.value = 'create'
  form.id = undefined
  form.softwareId = undefined
  form.version = ''
  form.channel = 1
  form.filePath = ''
  form.fileName = ''
  form.fileSize = 0
  form.fileSha256 = ''
  form.fileType = ''
  form.releaseNotes = ''
  form.minClientVersion = ''
  form.maxClientVersion = ''
  form.forceUpdate = 0
  uploadInfo.value = null
  formDialogVisible.value = true
}

function handleEdit(row: UpdatePackageRow) {
  formMode.value = 'edit'
  form.id = row.id
  form.softwareId = row.softwareId
  form.version = row.version
  form.channel = row.channel
  form.filePath = row.filePath
  form.fileName = row.fileName
  form.fileSize = row.fileSize
  form.fileSha256 = row.fileSha256
  form.fileType = row.fileType
  form.releaseNotes = row.releaseNotes || ''
  form.minClientVersion = row.minClientVersion || ''
  form.maxClientVersion = row.maxClientVersion || ''
  form.forceUpdate = row.forceUpdate
  uploadInfo.value = null
  formDialogVisible.value = true
}

/* ============ 文件上传 ============ */
function handleBeforeUpload(file: File): boolean {
  const allowedTypes = ['exe', 'sh', 'win', 'lua', 'zip', '7z']
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowedTypes.includes(ext)) {
    ElMessage.error('仅支持 exe/sh/win/lua/zip/7z 格式')
    return false
  }
  if (file.size > 500 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 500MB')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions): Promise<void> {
  uploading.value = true
  uploadPercent.value = 0
  try {
    const res: any = await updatePackageApi.upload(options.file as File, (percent) => {
      uploadPercent.value = percent
    })
    uploadInfo.value = res
    form.filePath = res.filePath
    form.fileName = res.fileName
    form.fileSize = res.fileSize
    form.fileSha256 = res.fileSha256
    form.fileType = res.fileType
    ElMessage.success('文件上传成功')
  } catch {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (formMode.value === 'create' && !uploadInfo.value) {
      ElMessage.warning('请先上传文件')
      return
    }
    submitLoading.value = true
    try {
      const payload = {
        softwareId: form.softwareId!,
        version: form.version,
        channel: form.channel,
        filePath: form.filePath,
        fileName: form.fileName,
        fileSize: form.fileSize,
        fileSha256: form.fileSha256,
        fileType: form.fileType,
        releaseNotes: form.releaseNotes || undefined,
        minClientVersion: form.minClientVersion || undefined,
        maxClientVersion: form.maxClientVersion || undefined,
        forceUpdate: form.forceUpdate
      }
      if (formMode.value === 'create') {
        await updatePackageApi.create(payload)
        ElMessage.success('创建成功（草稿状态）')
      } else {
        await updatePackageApi.update({ id: form.id!, ...payload })
        ElMessage.success('编辑成功')
      }
      formDialogVisible.value = false
      loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

/* ============ 发布/下线 ============ */
async function handlePublish(row: UpdatePackageRow) {
  try {
    await ElMessageBox.confirm(
      `确定要发布更新包 v${row.version} 吗？发布后终端用户客户端将能检查到此更新，且不可再编辑。`,
      '发布确认',
      { confirmButtonText: '发布', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updatePackageApi.publish(row.id)
    ElMessage.success('发布成功')
    loadData()
  } catch {
    // 拦截器已提示
  }
}

async function handleOffline(row: UpdatePackageRow) {
  try {
    await ElMessageBox.confirm(
      `确定要下线更新包 v${row.version} 吗？下线后终端用户客户端将不再检查到此更新，且不可重新发布。`,
      '下线确认',
      { confirmButtonText: '下线', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updatePackageApi.offline(row.id)
    ElMessage.success('下线成功')
    loadData()
  } catch {
    // 拦截器已提示
  }
}

/* ============ 查看 ============ */
const viewDialogVisible = ref(false)
const viewData = reactive<UpdatePackageRow>({} as UpdatePackageRow)

function handleView(row: UpdatePackageRow) {
  Object.assign(viewData, row)
  viewDialogVisible.value = true
}

/* ============ 删除 ============ */
async function handleDelete(row: UpdatePackageRow) {
  try {
    await ElMessageBox.confirm(
      `确定要删除更新包 v${row.version} 吗？删除后数据库记录和物理文件都将被清除，不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updatePackageApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 拦截器已提示
  }
}
</script>
