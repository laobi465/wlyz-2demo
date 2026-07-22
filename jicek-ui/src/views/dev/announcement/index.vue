<!--
  极策k 公告管理页面
  作者: 极策k  日期: 2026-07-22

  v0.10.0 远程公告：
   - 开发者按软件/版本下发公告，终端用户客户端通过 SDK 拉取展示
   - 状态机：草稿(0) → 已发布(1) → 已下线(2)，不可逆
   - 仅草稿可编辑；已发布可下线；已下线只能删除
   - 支持按客户端版本范围匹配（minVersion/maxVersion）
   - 排序：pinned DESC + sortOrder DESC + publishTime DESC

  接口：
    GET    /api/dev/announcement/page           分页查询
    GET    /api/dev/announcement/{id}           详情
    POST   /api/dev/announcement                创建（草稿）
    PUT    /api/dev/announcement                编辑（仅草稿）
    DELETE /api/dev/announcement/{id}           删除
    POST   /api/dev/announcement/{id}/publish   发布
    POST   /api/dev/announcement/{id}/offline   下线
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">远程公告</span>
        <el-button type="primary" style="float: right" @click="handleCreate">新增公告</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="软件">
          <el-select
            v-model="filter.softwareId"
            placeholder="全部软件"
            clearable
            style="width: 180px"
            @change="loadData"
          >
            <el-option
              v-for="sw in softwareList"
              :key="sw.id"
              :label="sw.name"
              :value="sw.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filter.type" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="通知条" :value="1" />
            <el-option label="弹窗" :value="2" />
            <el-option label="置顶横幅" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input
            v-model="filter.title"
            placeholder="公告标题"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="{ row }">
            <el-tag v-if="row.pinned === 1" type="danger" size="small" style="margin-right: 4px">置顶</el-tag>
            <span>{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="softwareId" label="软件ID" width="90" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本范围" width="160">
          <template #default="{ row }">
            <span v-if="!row.minVersion && !row.maxVersion" style="color: var(--jicek-text-secondary)">不限</span>
            <span v-else>{{ row.minVersion || '*' }} ~ {{ row.maxVersion || '*' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="拉取次数" width="100" />
        <el-table-column prop="publishTime" label="发布时间" min-width="160" />
        <el-table-column label="操作" width="240" fixed="right">
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
      :title="formMode === 'create' ? '新增公告' : '编辑公告'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属软件" prop="softwareId">
          <el-select v-model="form.softwareId" placeholder="选择软件" style="width: 100%">
            <el-option
              v-for="sw in softwareList"
              :key="sw.id"
              :label="sw.name"
              :value="sw.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="如：v1.2.0 更新公告" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="公告类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">通知条</el-radio>
            <el-radio :value="2">弹窗</el-radio>
            <el-radio :value="3">置顶横幅</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="支持 Markdown 格式，客户端渲染展示"
          />
        </el-form-item>
        <el-form-item label="版本范围">
          <el-input v-model="form.minVersion" placeholder="最低版本（含），如 1.0.0" style="width: 200px" />
          <span style="margin: 0 8px; color: var(--jicek-text-secondary)">~</span>
          <el-input v-model="form.maxVersion" placeholder="最高版本（含），留空不限" style="width: 200px" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">越大越靠前</span>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存为草稿</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗（只读） -->
    <el-dialog v-model="viewDialogVisible" title="公告详情" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ viewData.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeText(viewData.type) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(viewData.status)" size="small">{{ statusText(viewData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="版本范围">
          {{ viewData.minVersion || '*' }} ~ {{ viewData.maxVersion || '*' }}
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ viewData.publishTime || '未发布' }}</el-descriptions-item>
        <el-descriptions-item label="下线时间">{{ viewData.offlineTime || '未下线' }}</el-descriptions-item>
        <el-descriptions-item label="拉取次数">{{ viewData.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <div style="white-space: pre-wrap; max-height: 300px; overflow-y: auto">{{ viewData.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { announcementApi, softwareApi } from '@/api'

interface AnnouncementRow {
  id: number
  tenantId: number
  softwareId: number
  title: string
  content: string
  type: number
  status: number
  minVersion: string
  maxVersion: string
  sortOrder: number
  pinned: number
  viewCount: number
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

const loading = ref(false)
const tableData = ref<AnnouncementRow[]>([])
const total = ref(0)
const softwareList = ref<SoftwareOption[]>([])

const filter = reactive({
  current: 1,
  size: 20,
  softwareId: undefined as number | undefined,
  status: undefined as number | undefined,
  type: undefined as number | undefined,
  title: ''
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
    const res: any = await announcementApi.page({
      current: filter.current,
      size: filter.size,
      softwareId: filter.softwareId,
      status: filter.status,
      type: filter.type,
      title: filter.title || undefined
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
  filter.type = undefined
  filter.title = ''
  filter.current = 1
  loadData()
}

onMounted(() => {
  loadSoftwareList()
  loadData()
})

/* ============ 文本/样式辅助 ============ */
function typeText(t: number): string {
  return { 1: '通知条', 2: '弹窗', 3: '置顶横幅' }[t] || '未知'
}
function typeTagType(t: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  return ({ 1: 'info', 2: 'warning', 3: 'danger' } as const)[t] || ''
}
function statusText(s: number): string {
  return { 0: '草稿', 1: '已发布', 2: '已下线' }[s] || '未知'
}
function statusTagType(s: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  return ({ 0: 'info', 1: 'success', 2: 'danger' } as const)[s] || ''
}

/* ============ 新建/编辑 ============ */
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  softwareId: undefined as number | undefined,
  title: '',
  content: '',
  type: 1,
  minVersion: '',
  maxVersion: '',
  sortOrder: 0,
  pinned: 0
})

const rules: FormRules = {
  softwareId: [{ required: true, message: '请选择软件', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 128, message: '标题最长 128 字符', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function handleCreate() {
  formMode.value = 'create'
  form.id = undefined
  form.softwareId = undefined
  form.title = ''
  form.content = ''
  form.type = 1
  form.minVersion = ''
  form.maxVersion = ''
  form.sortOrder = 0
  form.pinned = 0
  formDialogVisible.value = true
}

function handleEdit(row: AnnouncementRow) {
  formMode.value = 'edit'
  form.id = row.id
  form.softwareId = row.softwareId
  form.title = row.title
  form.content = row.content
  form.type = row.type
  form.minVersion = row.minVersion || ''
  form.maxVersion = row.maxVersion || ''
  form.sortOrder = row.sortOrder
  form.pinned = row.pinned
  formDialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = {
        softwareId: form.softwareId!,
        title: form.title,
        content: form.content,
        type: form.type,
        minVersion: form.minVersion || undefined,
        maxVersion: form.maxVersion || undefined,
        sortOrder: form.sortOrder,
        pinned: form.pinned
      }
      if (formMode.value === 'create') {
        await announcementApi.create(payload)
        ElMessage.success('创建成功（草稿状态）')
      } else {
        await announcementApi.update({ id: form.id!, ...payload })
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
async function handlePublish(row: AnnouncementRow) {
  try {
    await ElMessageBox.confirm(
      `确定要发布公告「${row.title}」吗？发布后对终端用户客户端可见，且不可再编辑。`,
      '发布确认',
      { confirmButtonText: '发布', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await announcementApi.publish(row.id)
    ElMessage.success('发布成功')
    loadData()
  } catch {
    // 拦截器已提示
  }
}

async function handleOffline(row: AnnouncementRow) {
  try {
    await ElMessageBox.confirm(
      `确定要下线公告「${row.title}」吗？下线后终端用户客户端将不再展示此公告，且不可重新发布。`,
      '下线确认',
      { confirmButtonText: '下线', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await announcementApi.offline(row.id)
    ElMessage.success('下线成功')
    loadData()
  } catch {
    // 拦截器已提示
  }
}

/* ============ 查看 ============ */
const viewDialogVisible = ref(false)
const viewData = reactive<AnnouncementRow>({} as AnnouncementRow)

async function handleView(row: AnnouncementRow) {
  Object.assign(viewData, row)
  viewDialogVisible.value = true
}

/* ============ 删除 ============ */
async function handleDelete(row: AnnouncementRow) {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告「${row.title}」吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await announcementApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 拦截器已提示
  }
}
</script>
