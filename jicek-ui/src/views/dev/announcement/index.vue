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
        <span class="jicek-card-title">{{ t('announcement.title') }}</span>
        <el-button type="primary" style="float: right" @click="handleCreate">{{ t('announcement.create') }}</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('announcement.software')">
          <el-select
            v-model="filter.softwareId"
            :placeholder="t('announcement.allSoftware')"
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
        <el-form-item :label="t('announcement.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 120px" @change="loadData">
            <el-option :label="t('announcement.statusDraft')" :value="0" />
            <el-option :label="t('announcement.statusPublished')" :value="1" />
            <el-option :label="t('announcement.statusOffline')" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('announcement.type')">
          <el-select v-model="filter.type" :placeholder="t('common.all')" clearable style="width: 120px" @change="loadData">
            <el-option :label="t('announcement.typeNotice')" :value="1" />
            <el-option :label="t('announcement.typeDialog')" :value="2" />
            <el-option :label="t('announcement.typeBanner')" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('announcement.titleLabel')">
          <el-input
            v-model="filter.title"
            :placeholder="t('announcement.titlePlaceholder')"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" :label="t('announcement.titleCol')" min-width="180">
          <template #default="{ row }">
            <el-tag v-if="row.pinned === 1" type="danger" size="small" style="margin-right: 4px">{{ t('announcement.pinned') }}</el-tag>
            <span>{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="softwareId" :label="t('announcement.softwareId')" width="90" />
        <el-table-column prop="type" :label="t('announcement.type')" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('announcement.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('announcement.versionRange')" width="160">
          <template #default="{ row }">
            <span v-if="!row.minVersion && !row.maxVersion" style="color: var(--jicek-text-secondary)">{{ t('announcement.versionRangeUnlimited') }}</span>
            <span v-else>{{ row.minVersion || '*' }} ~ {{ row.maxVersion || '*' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" :label="t('announcement.viewCount')" width="100" />
        <el-table-column prop="publishTime" :label="t('announcement.publishTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" size="small" @click="handleEdit(row)">{{ t('announcement.editBtn') }}</el-button>
            <el-button v-if="row.status === 0" link type="success" size="small" @click="handlePublish(row)">{{ t('announcement.publishBtn') }}</el-button>
            <el-button v-if="row.status === 1" link type="warning" size="small" @click="handleOffline(row)">{{ t('announcement.offlineBtn') }}</el-button>
            <el-button link type="info" size="small" @click="handleView(row)">{{ t('announcement.viewBtn') }}</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">{{ t('announcement.deleteBtn') }}</el-button>
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
      :title="formMode === 'create' ? t('announcement.create') : t('announcement.edit')"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('announcement.ownerSoftware')" prop="softwareId">
          <el-select v-model="form.softwareId" :placeholder="t('announcement.selectSoftware')" style="width: 100%">
            <el-option
              v-for="sw in softwareList"
              :key="sw.id"
              :label="sw.name"
              :value="sw.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('announcement.announcementTitle')" prop="title">
          <el-input v-model="form.title" :placeholder="t('announcement.announcementTitlePlaceholder')" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('announcement.announcementType')" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">{{ t('announcement.typeNotice') }}</el-radio>
            <el-radio :value="2">{{ t('announcement.typeDialog') }}</el-radio>
            <el-radio :value="3">{{ t('announcement.typeBanner') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('announcement.announcementContent')" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            :placeholder="t('announcement.contentPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('announcement.versionRangeLabel')">
          <el-input v-model="form.minVersion" :placeholder="t('announcement.minVersionPlaceholder')" style="width: 200px" />
          <span style="margin: 0 8px; color: var(--jicek-text-secondary)">~</span>
          <el-input v-model="form.maxVersion" :placeholder="t('announcement.maxVersionPlaceholder')" style="width: 200px" />
        </el-form-item>
        <el-form-item :label="t('announcement.sortOrder')">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">{{ t('announcement.sortOrderHint') }}</span>
        </el-form-item>
        <el-form-item :label="t('announcement.pinnedLabel')">
          <el-switch v-model="form.pinned" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ t('announcement.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ t('announcement.saveDraft') }}</el-button>
      </template>
    </el-dialog>

    <!-- 查看弹窗（只读） -->
    <el-dialog v-model="viewDialogVisible" :title="t('announcement.viewTitle')" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="t('announcement.titleLabel')">{{ viewData.title }}</el-descriptions-item>
        <el-descriptions-item :label="t('announcement.typeLabel')">{{ typeText(viewData.type) }}</el-descriptions-item>
        <el-descriptions-item :label="t('announcement.statusLabel')">
          <el-tag :type="statusTagType(viewData.status)" size="small">{{ statusText(viewData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('announcement.versionRangeLabel2')">
          {{ viewData.minVersion || '*' }} ~ {{ viewData.maxVersion || '*' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('announcement.publishTimeLabel')">{{ viewData.publishTime || t('announcement.unpublished') }}</el-descriptions-item>
        <el-descriptions-item :label="t('announcement.offlineTime')">{{ viewData.offlineTime || t('announcement.notOffline') }}</el-descriptions-item>
        <el-descriptions-item :label="t('announcement.viewCountLabel')">{{ viewData.viewCount }}</el-descriptions-item>
        <el-descriptions-item :label="t('announcement.contentLabel')">
          <div style="white-space: pre-wrap; max-height: 300px; overflow-y: auto">{{ viewData.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { announcementApi, softwareApi } from '@/api'

const { t } = useI18n()

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
function typeText(type: number): string {
  return { 1: t('announcement.typeNotice'), 2: t('announcement.typeDialog'), 3: t('announcement.typeBanner') }[type] || t('announcement.typeUnknown')
}
function typeTagType(type: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  return ({ 1: 'info', 2: 'warning', 3: 'danger' } as const)[type] || ''
}
function statusText(s: number): string {
  return { 0: t('announcement.statusDraft'), 1: t('announcement.statusPublished'), 2: t('announcement.statusOffline') }[s] || t('announcement.statusUnknown')
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
  softwareId: [{ required: true, message: t('announcement.softwareRequired'), trigger: 'change' }],
  title: [
    { required: true, message: t('announcement.titleRequired'), trigger: 'blur' },
    { max: 128, message: t('announcement.titleMax'), trigger: 'blur' }
  ],
  content: [{ required: true, message: t('announcement.contentRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('announcement.typeRequired'), trigger: 'change' }]
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
        ElMessage.success(t('announcement.createDraftSuccess'))
      } else {
        await announcementApi.update({ id: form.id!, ...payload })
        ElMessage.success(t('announcement.editSuccess'))
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
      t('announcement.publishMessage', { title: row.title }),
      t('announcement.publishTitle'),
      { confirmButtonText: t('announcement.publishConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await announcementApi.publish(row.id)
    ElMessage.success(t('announcement.publishSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

async function handleOffline(row: AnnouncementRow) {
  try {
    await ElMessageBox.confirm(
      t('announcement.offlineMessage', { title: row.title }),
      t('announcement.offlineTitle'),
      { confirmButtonText: t('announcement.offlineConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await announcementApi.offline(row.id)
    ElMessage.success(t('announcement.offlineSuccess'))
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
      t('announcement.deleteMessage', { title: row.title }),
      t('announcement.deleteTitle'),
      { confirmButtonText: t('announcement.deleteConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await announcementApi.delete(row.id)
    ElMessage.success(t('announcement.deleteSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}
</script>
