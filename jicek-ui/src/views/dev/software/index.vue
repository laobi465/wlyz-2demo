<!--
  极策k 软件管理页面
  作者: 极策k  日期: 2026-07-22

  v0.8.0 软件管理：
   - 软件 CRUD（卡密/设备/云函数的父实体）
   - 创建后自动生成 appKey + signSecret + RSA 密钥对，明文仅返回一次
   - 支持轮换签名密钥 / RSA 密钥对（二次确认 + 明文仅此一次）
   - 删除前校验关联卡类/设备/云函数
   - tenantId 由后端从 AuthContext 获取，前端禁传（防越权）

  接口：
    GET    /api/dev/software/page                       分页查询
    GET    /api/dev/software/{id}                       详情
    POST   /api/dev/software                            创建（返回明文密钥）
    PUT    /api/dev/software                            更新（仅非敏感字段）
    DELETE /api/dev/software/{id}                       删除（关联校验）
    POST   /api/dev/software/{id}/regenerate-sign-secret 轮换签名密钥
    POST   /api/dev/software/{id}/regenerate-rsa-key     轮换 RSA 密钥对
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('software.title') }}</span>
        <el-button type="primary" style="float: right" @click="handleCreate">{{ t('software.create') }}</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('software.name')">
          <el-input
            v-model="filter.name"
            :placeholder="t('software.namePlaceholder')"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="filter.enabled" :placeholder="t('common.all')" clearable style="width: 120px">
            <el-option :label="t('common.enabled')" :value="1" />
            <el-option :label="t('common.disabled')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" :label="t('software.name')" min-width="140" />
        <el-table-column prop="appKey" :label="t('software.appKey')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-text type="primary" class="mono-text">{{ row.appKey }}</el-text>
          </template>
        </el-table-column>
        <el-table-column prop="signSecretMasked" :label="t('software.signSecret')" width="140">
          <template #default="{ row }">
            <el-text class="mono-text">{{ row.signSecretMasked }}</el-text>
          </template>
        </el-table-column>
        <el-table-column prop="version" :label="t('software.version')" width="100" />
        <el-table-column prop="heartbeatInterval" :label="t('software.heartbeatSeconds')" width="100" />
        <el-table-column prop="maxConcurrent" :label="t('software.concurrentShort')" width="80" />
        <el-table-column prop="enabled" :label="t('common.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? t('common.enabled') : t('common.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('common.createTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="350" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="success" size="small" @click="handleCodeGen(row)">{{ t('software.accessCode') }}</el-button>
            <el-dropdown trigger="click" style="margin: 0 8px" @command="(cmd: string) => handleKeyCommand(cmd, row)">
              <el-button link type="warning" size="small">
                {{ t('software.keyMenu') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="sign">{{ t('software.rotateSignSecret') }}</el-dropdown-item>
                  <el-dropdown-item command="rsa">{{ t('software.rotateRsaKey') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button link type="danger" size="small" @click="handleDelete(row)">{{ t('common.delete') }}</el-button>
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
      :title="formMode === 'create' ? t('software.create') : t('software.edit')"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('software.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('software.nameInputPlaceholder')" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('software.currentVersion')" prop="version">
          <el-input v-model="form.version" :placeholder="t('software.versionExample')" maxlength="20" />
        </el-form-item>
        <el-form-item :label="t('software.minVersion')" prop="minVersion">
          <el-input v-model="form.minVersion" :placeholder="t('software.minVersionExample')" maxlength="20" />
        </el-form-item>
        <el-form-item :label="t('software.heartbeatInterval')" prop="heartbeatInterval">
          <el-input-number v-model="form.heartbeatInterval" :min="5" :max="300" :step="5" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">{{ t('software.heartbeatUnit') }}</span>
        </el-form-item>
        <el-form-item :label="t('software.maxConcurrent')" prop="maxConcurrent">
          <el-input-number v-model="form.maxConcurrent" :min="1" :max="100" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">{{ t('software.concurrentUnit') }}</span>
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" :active-text="t('common.enabled')" :inactive-text="t('common.disabled')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 密钥展示弹窗（创建/轮换后，明文仅此一次） -->
    <el-dialog
      v-model="secretDialogVisible"
      :title="t('software.secretDialogTitle')"
      width="640px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        :title="t('software.secretAlert')"
        style="margin-bottom: 16px"
      />
      <el-form label-position="top">
        <el-form-item :label="t('software.appKeyLabel')">
          <el-input :model-value="secretData.appKey" readonly>
            <template #append>
              <el-button @click="copyText(secretData.appKey)">{{ t('software.copy') }}</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('software.signSecretLabel')">
          <el-input :model-value="secretData.signSecret" type="textarea" :rows="2" readonly />
          <el-button size="small" style="margin-top: 4px" @click="copyText(secretData.signSecret)">{{ t('software.copySignSecret') }}</el-button>
        </el-form-item>
        <el-form-item :label="t('software.rsaPublicKeyLabel')">
          <el-input :model-value="secretData.rsaPublicKey" type="textarea" :rows="4" readonly />
          <el-button size="small" style="margin-top: 4px" @click="copyText(secretData.rsaPublicKey)">{{ t('software.copyPublicKey') }}</el-button>
        </el-form-item>
        <el-form-item :label="t('software.rsaPrivateKeyLabel')">
          <el-input :model-value="secretData.rsaPrivateKey" type="textarea" :rows="6" readonly />
          <el-button size="small" style="margin-top: 4px" @click="copyText(secretData.rsaPrivateKey)">{{ t('software.copyPrivateKey') }}</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="confirmSecretSaved">{{ t('software.confirmSaved') }}</el-button>
      </template>
    </el-dialog>

    <!-- SDK 代码生成弹窗（v0.12.0 一键接入） -->
    <SdkCodeGenDialog
      v-model="codeGenDialogVisible"
      :software-id="codeGenSoftwareId"
      :software-name="codeGenSoftwareName"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { softwareApi } from '@/api'
import SdkCodeGenDialog from './SdkCodeGenDialog.vue'

const { t } = useI18n()

interface SoftwareRow {
  id: number
  tenantId: number
  name: string
  appKey: string
  signSecretMasked: string
  rsaPublicKey: string
  version: string
  minVersion: string
  heartbeatInterval: number
  maxConcurrent: number
  enabled: number
  createTime: string
  updateTime: string
}

interface SecretData {
  appKey: string
  signSecret: string
  rsaPublicKey: string
  rsaPrivateKey: string
}

const loading = ref(false)
const tableData = ref<SoftwareRow[]>([])
const total = ref(0)

const filter = reactive({
  current: 1,
  size: 20,
  name: '',
  enabled: undefined as number | undefined
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await softwareApi.page({
      current: filter.current,
      size: filter.size,
      name: filter.name || undefined,
      enabled: filter.enabled
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filter.name = ''
  filter.enabled = undefined
  filter.current = 1
  loadData()
}

onMounted(loadData)

/* ============ 新建/编辑 ============ */
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  version: '',
  minVersion: '',
  heartbeatInterval: 60,
  maxConcurrent: 1,
  enabled: 1
})

const rules: FormRules = {
  name: [
    { required: true, message: t('software.nameRequired'), trigger: 'blur' },
    { max: 64, message: t('software.nameMax'), trigger: 'blur' }
  ],
  heartbeatInterval: [
    { required: true, message: t('software.heartbeatRequired'), trigger: 'blur' },
    { type: 'number', min: 5, max: 300, message: t('software.heartbeatRange'), trigger: 'blur' }
  ],
  maxConcurrent: [
    { required: true, message: t('software.maxConcurrentRequired'), trigger: 'blur' },
    { type: 'number', min: 1, message: t('software.maxConcurrentMin'), trigger: 'blur' }
  ]
}

function handleCreate() {
  formMode.value = 'create'
  form.id = undefined
  form.name = ''
  form.version = ''
  form.minVersion = ''
  form.heartbeatInterval = 60
  form.maxConcurrent = 1
  form.enabled = 1
  formDialogVisible.value = true
}

function handleEdit(row: SoftwareRow) {
  formMode.value = 'edit'
  form.id = row.id
  form.name = row.name
  form.version = row.version || ''
  form.minVersion = row.minVersion || ''
  form.heartbeatInterval = row.heartbeatInterval
  form.maxConcurrent = row.maxConcurrent
  form.enabled = row.enabled
  formDialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (formMode.value === 'create') {
        const result: any = await softwareApi.create({
          name: form.name,
          version: form.version || undefined,
          minVersion: form.minVersion || undefined,
          heartbeatInterval: form.heartbeatInterval,
          maxConcurrent: form.maxConcurrent,
          enabled: form.enabled
        })
        formDialogVisible.value = false
        // 创建成功，展示明文密钥（仅此一次）
        showSecretDialog({
          appKey: result.appKey,
          signSecret: result.signSecret,
          rsaPublicKey: result.rsaPublicKey,
          rsaPrivateKey: result.rsaPrivateKey
        })
        ElMessage.success(t('software.createSuccess'))
        loadData()
      } else {
        await softwareApi.update({
          id: form.id!,
          name: form.name,
          version: form.version || undefined,
          minVersion: form.minVersion || undefined,
          heartbeatInterval: form.heartbeatInterval,
          maxConcurrent: form.maxConcurrent,
          enabled: form.enabled
        })
        formDialogVisible.value = false
        ElMessage.success(t('software.updateSuccess'))
        loadData()
      }
    } finally {
      submitLoading.value = false
    }
  })
}

/* ============ 密钥展示弹窗 ============ */
const secretDialogVisible = ref(false)
const secretData = reactive<SecretData>({
  appKey: '',
  signSecret: '',
  rsaPublicKey: '',
  rsaPrivateKey: ''
})

function showSecretDialog(data: SecretData) {
  secretData.appKey = data.appKey
  secretData.signSecret = data.signSecret
  secretData.rsaPublicKey = data.rsaPublicKey
  secretData.rsaPrivateKey = data.rsaPrivateKey
  secretDialogVisible.value = true
}

function confirmSecretSaved() {
  secretDialogVisible.value = false
  ElMessage.success(t('software.secretConfirmed'))
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('software.copied'))
  } catch {
    ElMessage.warning(t('software.copyFailed'))
  }
}

/* ============ 密钥轮换 ============ */
async function handleKeyCommand(cmd: string, row: SoftwareRow) {
  const actionText = cmd === 'sign' ? t('software.rotateSignSecret') : t('software.rotateRsaKey')
  const warning = cmd === 'sign'
    ? t('software.rotateSignWarning')
    : t('software.rotateRsaWarning')
  const title = cmd === 'sign' ? t('software.rotateSignTitle') : t('software.rotateRsaTitle')

  try {
    await ElMessageBox.confirm(
      `${actionText}？${warning}`,
      title,
      {
        confirmButtonText: t('software.rotateConfirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    const result: any = cmd === 'sign'
      ? await softwareApi.regenerateSignSecret(row.id)
      : await softwareApi.regenerateRsaKey(row.id)
    showSecretDialog({
      appKey: result.appKey,
      signSecret: result.signSecret,
      rsaPublicKey: result.rsaPublicKey,
      rsaPrivateKey: result.rsaPrivateKey
    })
    ElMessage.success(t('software.rotateSuccess', { action: actionText }))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

/* ============ 删除 ============ */
async function handleDelete(row: SoftwareRow) {
  try {
    await ElMessageBox.confirm(
      t('software.deleteConfirm', { name: row.name }),
      t('software.deleteTitle'),
      {
        confirmButtonText: t('common.delete'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }
  try {
    await softwareApi.delete(row.id)
    ElMessage.success(t('software.deleteSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

/* ============ SDK 代码生成（v0.12.0 一键接入） ============ */
const codeGenDialogVisible = ref(false)
const codeGenSoftwareId = ref(0)
const codeGenSoftwareName = ref('')

function handleCodeGen(row: SoftwareRow) {
  codeGenSoftwareId.value = row.id
  codeGenSoftwareName.value = row.name
  codeGenDialogVisible.value = true
}
</script>

<style scoped lang="scss">
.mono-text {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
}
</style>
