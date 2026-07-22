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
        <span class="jicek-card-title">{{ t('updatePackage.title') }}</span>
        <el-button type="primary" style="float: right" @click="handleCreate">{{ t('updatePackage.create') }}</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('updatePackage.software')">
          <el-select v-model="filter.softwareId" :placeholder="t('updatePackage.allSoftware')" clearable style="width: 180px" @change="loadData">
            <el-option v-for="sw in softwareList" :key="sw.id" :label="sw.name" :value="sw.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('updatePackage.channel')">
          <el-select v-model="filter.channel" :placeholder="t('common.all')" clearable style="width: 120px" @change="loadData">
            <el-option :label="t('updatePackage.channelStable')" :value="1" />
            <el-option :label="t('updatePackage.channelBeta')" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('updatePackage.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 120px" @change="loadData">
            <el-option :label="t('updatePackage.statusDraft')" :value="0" />
            <el-option :label="t('updatePackage.statusPublished')" :value="1" />
            <el-option :label="t('updatePackage.statusOffline')" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('updatePackage.version')">
          <el-input v-model="filter.version" :placeholder="t('updatePackage.versionPlaceholder')" clearable style="width: 140px" @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="softwareId" :label="t('updatePackage.softwareId')" width="90" />
        <el-table-column prop="version" :label="t('updatePackage.version')" width="100">
          <template #default="{ row }">
            <span style="font-weight: 600">v{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="channel" :label="t('updatePackage.channel')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.channel === 1 ? 'success' : 'warning'" size="small">
              {{ row.channel === 1 ? t('updatePackage.channelStable') : t('updatePackage.channelBeta') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileType" :label="t('updatePackage.fileType')" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" :label="t('updatePackage.fileSize')" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="status" :label="t('updatePackage.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="forceUpdate" :label="t('updatePackage.forceUpdate')" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.forceUpdate === 1" type="danger" size="small">{{ t('updatePackage.forceUpdateTag') }}</el-tag>
            <span v-else style="color: var(--jicek-text-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" :label="t('updatePackage.downloadCount')" width="90" />
        <el-table-column prop="publishTime" :label="t('updatePackage.publishTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" size="small" @click="handleEdit(row)">{{ t('updatePackage.editBtn') }}</el-button>
            <el-button v-if="row.status === 0" link type="success" size="small" @click="handlePublish(row)">{{ t('updatePackage.publishBtn') }}</el-button>
            <el-button v-if="row.status === 1" link type="warning" size="small" @click="handleOffline(row)">{{ t('updatePackage.offlineBtn') }}</el-button>
            <el-button link type="info" size="small" @click="handleView(row)">{{ t('updatePackage.viewBtn') }}</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">{{ t('updatePackage.deleteBtn') }}</el-button>
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
      :title="formMode === 'create' ? t('updatePackage.create') : t('updatePackage.edit')"
      width="720px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="t('updatePackage.ownerSoftware')" prop="softwareId">
          <el-select v-model="form.softwareId" :placeholder="t('updatePackage.selectSoftware')" style="width: 100%" :disabled="formMode === 'edit'">
            <el-option v-for="sw in softwareList" :key="sw.id" :label="sw.name" :value="sw.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('updatePackage.versionLabel')" prop="version">
          <el-input v-model="form.version" :placeholder="t('updatePackage.versionPlaceholder')" :disabled="formMode === 'edit'" />
        </el-form-item>
        <el-form-item :label="t('updatePackage.channelLabel')" prop="channel">
          <el-radio-group v-model="form.channel" :disabled="formMode === 'edit'">
            <el-radio :value="1">{{ t('updatePackage.channelStable') }}</el-radio>
            <el-radio :value="2">{{ t('updatePackage.channelBeta') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formMode === 'create'" :label="t('updatePackage.fileLabel')" prop="filePath">
          <el-upload
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            :http-request="handleUpload"
            :accept="'.exe,.sh,.win,.lua,.zip,.7z'"
          >
            <el-button type="primary" :loading="uploading">
              {{ uploadInfo ? t('updatePackage.reselectFile') : t('updatePackage.selectFile') }}
            </el-button>
            <template #tip>
              <div style="color: var(--jicek-text-secondary); font-size: 12px; line-height: 1.6; white-space: pre-line">
                {{ t('updatePackage.fileTip') }}
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
        <el-form-item v-else :label="t('updatePackage.fileInfo')">
          <div>
            <el-tag size="small">{{ form.fileType }}</el-tag>
            <span style="margin-left: 8px">{{ form.fileName }} · {{ formatSize(form.fileSize) }}</span>
            <div style="color: var(--jicek-text-secondary); font-size: 12px; margin-top: 4px">
              SHA-256: {{ form.fileSha256 }}
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="t('updatePackage.releaseNotes')">
          <el-input v-model="form.releaseNotes" type="textarea" :rows="5" :placeholder="t('updatePackage.releaseNotesPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('updatePackage.clientVersionRange')">
          <el-input v-model="form.minClientVersion" :placeholder="t('updatePackage.minClientVersionPlaceholder')" style="width: 200px" />
          <span style="margin: 0 8px; color: var(--jicek-text-secondary)">~</span>
          <el-input v-model="form.maxClientVersion" :placeholder="t('updatePackage.maxClientVersionPlaceholder')" style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('updatePackage.forceUpdateLabel')">
          <el-switch v-model="form.forceUpdate" :active-value="1" :inactive-value="0" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">{{ t('updatePackage.forceUpdateHint') }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ t('updatePackage.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" :disabled="formMode === 'create' && !uploadInfo" @click="submitForm">
          {{ t('updatePackage.saveDraft') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗 -->
    <el-dialog v-model="viewDialogVisible" :title="t('updatePackage.viewTitle')" width="680px">
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="t('updatePackage.versionLabel2')">v{{ viewData.version }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.channelLabel2')">{{ viewData.channel === 1 ? t('updatePackage.channelStable') : t('updatePackage.channelBeta') }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.fileTypeLabel')">{{ viewData.fileType }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.fileName')">{{ viewData.fileName }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.sizeLabel')">{{ formatSize(viewData.fileSize) }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.sha256')">
          <span style="word-break: break-all; font-family: monospace; font-size: 12px">{{ viewData.fileSha256 }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.statusLabel')">
          <el-tag :type="statusTagType(viewData.status)" size="small">{{ statusText(viewData.status) }}</el-tag>
          <el-tag v-if="viewData.forceUpdate === 1" type="danger" size="small" style="margin-left: 8px">{{ t('updatePackage.forceUpdateLabel2') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.clientVersionRangeLabel')">
          {{ viewData.minClientVersion || '*' }} ~ {{ viewData.maxClientVersion || '*' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.publishTimeLabel')">{{ viewData.publishTime || t('updatePackage.unpublished') }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.offlineTime')">{{ viewData.offlineTime || t('updatePackage.notOffline') }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.downloadCountLabel')">{{ viewData.downloadCount }}</el-descriptions-item>
        <el-descriptions-item :label="t('updatePackage.releaseNotesLabel')">
          <div style="white-space: pre-wrap; max-height: 300px; overflow-y: auto">{{ viewData.releaseNotes || t('updatePackage.noReleaseNotes') }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { updatePackageApi, softwareApi } from '@/api'

const { t } = useI18n()

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
  return { 0: t('updatePackage.statusDraft'), 1: t('updatePackage.statusPublished'), 2: t('updatePackage.statusOffline') }[s] || t('updatePackage.statusUnknown')
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
  softwareId: [{ required: true, message: t('updatePackage.softwareRequired'), trigger: 'change' }],
  version: [
    { required: true, message: t('updatePackage.versionRequired'), trigger: 'blur' },
    { pattern: /^\d+\.\d+\.\d+/, message: t('updatePackage.versionPattern'), trigger: 'blur' }
  ],
  channel: [{ required: true, message: t('updatePackage.channelRequired'), trigger: 'change' }]
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
    ElMessage.error(t('updatePackage.invalidFileType'))
    return false
  }
  if (file.size > 500 * 1024 * 1024) {
    ElMessage.error(t('updatePackage.fileTooLarge'))
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
    ElMessage.success(t('updatePackage.uploadSuccess'))
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
      ElMessage.warning(t('updatePackage.uploadFirst'))
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
        ElMessage.success(t('updatePackage.createDraftSuccess'))
      } else {
        await updatePackageApi.update({ id: form.id!, ...payload })
        ElMessage.success(t('updatePackage.editSuccess'))
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
      t('updatePackage.publishMessage', { version: row.version }),
      t('updatePackage.publishTitle'),
      { confirmButtonText: t('updatePackage.publishConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updatePackageApi.publish(row.id)
    ElMessage.success(t('updatePackage.publishSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

async function handleOffline(row: UpdatePackageRow) {
  try {
    await ElMessageBox.confirm(
      t('updatePackage.offlineMessage', { version: row.version }),
      t('updatePackage.offlineTitle'),
      { confirmButtonText: t('updatePackage.offlineConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updatePackageApi.offline(row.id)
    ElMessage.success(t('updatePackage.offlineSuccess'))
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
      t('updatePackage.deleteMessage', { version: row.version }),
      t('updatePackage.deleteTitle'),
      { confirmButtonText: t('updatePackage.deleteConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await updatePackageApi.delete(row.id)
    ElMessage.success(t('updatePackage.deleteSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}
</script>
