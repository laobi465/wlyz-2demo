<!--
  极策k 终端用户管理页面
  作者: 极策k  日期: 2026-07-22

  v0.14.0 终端用户管理（独立账号体系）：
   - 独立于卡密登录体系，终端用户使用账号密码登录 H5
   - 同软件内用户名唯一（tenantId + softwareId + username 三元唯一）
   - 密码 BCrypt 哈希存储，明文永不返回
   - 支持封禁/解封、重置密码（开发者后台调用，无需原密码）
   - tenantId 由后端从 AuthContext 获取，前端禁传（防越权）

  接口：
    GET    /api/dev/end-user/page                分页查询
    GET    /api/dev/end-user/{id}                详情
    POST   /api/dev/end-user                     创建（password 必填）
    PUT    /api/dev/end-user                     更新（password 留空表示不修改）
    DELETE /api/dev/end-user/{id}                删除
    POST   /api/dev/end-user/{id}/ban            封禁
    POST   /api/dev/end-user/{id}/unban          解封
    POST   /api/dev/end-user/reset-password      重置密码
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('endUser.title') }}</span>
        <el-button type="primary" style="float: right" @click="handleCreate">{{ t('endUser.create') }}</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('endUser.software')">
          <el-select
            v-model="filter.softwareId"
            :placeholder="t('endUser.allSoftware')"
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
        <el-form-item :label="t('endUser.username')">
          <el-input
            v-model="filter.username"
            :placeholder="t('endUser.usernameFuzzy')"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 120px" @change="loadData">
            <el-option :label="t('endUser.normal')" :value="1" />
            <el-option :label="t('endUser.banned')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" :label="t('endUser.username')" min-width="120" />
        <el-table-column prop="nickname" :label="t('endUser.nickname')" min-width="100" />
        <el-table-column prop="softwareName" :label="t('endUser.software')" min-width="140" />
        <el-table-column prop="email" :label="t('endUser.email')" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" :label="t('endUser.phone')" width="130" />
        <el-table-column prop="status" :label="t('common.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? t('endUser.normal') : t('endUser.banned') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" :label="t('endUser.lastLoginTime')" min-width="160" />
        <el-table-column prop="lastLoginIp" :label="t('endUser.lastLoginIp')" width="130" />
        <el-table-column prop="createTime" :label="t('common.createTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="warning" size="small" @click="handleResetPwd(row)">{{ t('endUser.resetPassword') }}</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              size="small"
              @click="handleBan(row)"
            >
              {{ t('endUser.ban') }}
            </el-button>
            <el-button
              v-else
              link
              type="primary"
              size="small"
              @click="handleUnban(row)"
            >
              {{ t('endUser.unban') }}
            </el-button>
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
      :title="formMode === 'create' ? t('endUser.create') : t('endUser.edit')"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('endUser.software')" prop="softwareId">
          <el-select v-model="form.softwareId" :placeholder="t('endUser.selectSoftware')" style="width: 100%">
            <el-option
              v-for="sw in softwareList"
              :key="sw.id"
              :label="sw.name"
              :value="sw.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('endUser.username')" prop="username">
          <el-input v-model="form.username" :placeholder="t('endUser.usernamePlaceholder')" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('endUser.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="formMode === 'create' ? t('endUser.passwordCreatePlaceholder') : t('endUser.passwordEditPlaceholder')"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item :label="t('endUser.nickname')" prop="nickname">
          <el-input v-model="form.nickname" :placeholder="t('common.optional')" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('endUser.email')" prop="email">
          <el-input v-model="form.email" :placeholder="t('common.optional')" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('endUser.phone')" prop="phone">
          <el-input v-model="form.phone" :placeholder="t('common.optional')" maxlength="20" />
        </el-form-item>
        <el-form-item :label="t('common.status')" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" :active-text="t('endUser.normal')" :inactive-text="t('endUser.banned')" />
        </el-form-item>
        <el-form-item :label="t('endUser.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" :placeholder="t('common.optional')" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="resetPwdDialogVisible"
      :title="t('endUser.resetPassword')"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        :title="t('endUser.resetAlert')"
        style="margin-bottom: 16px"
      />
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item :label="t('endUser.username')">
          <el-input :model-value="resetPwdForm.username" readonly />
        </el-form-item>
        <el-form-item :label="t('endUser.newPassword')" prop="newPassword">
          <el-input
            v-model="resetPwdForm.newPassword"
            type="password"
            show-password
            :placeholder="t('endUser.newPasswordPlaceholder')"
            maxlength="64"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="submitResetPwd">{{ t('endUser.confirmReset') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { endUserApi, softwareApi } from '@/api'

const { t } = useI18n()

interface EndUserRow {
  id: number
  tenantId: number
  softwareId: number
  softwareName: string
  username: string
  nickname: string
  email: string
  phone: string
  status: number
  lastLoginTime: string
  lastLoginIp: string
  remark: string
  createTime: string
  updateTime: string
}

interface SoftwareOption {
  id: number
  name: string
}

const loading = ref(false)
const tableData = ref<EndUserRow[]>([])
const total = ref(0)
const softwareList = ref<SoftwareOption[]>([])

const filter = reactive({
  current: 1,
  size: 20,
  softwareId: undefined as number | undefined,
  username: '',
  status: undefined as number | undefined
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
    const res: any = await endUserApi.page({
      current: filter.current,
      size: filter.size,
      softwareId: filter.softwareId,
      username: filter.username || undefined,
      status: filter.status
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filter.softwareId = undefined
  filter.username = ''
  filter.status = undefined
  filter.current = 1
  loadData()
}

onMounted(() => {
  loadSoftwareList()
  loadData()
})

/* ============ 新建/编辑 ============ */
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  softwareId: undefined as number | undefined,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1,
  remark: ''
})

const rules: FormRules = {
  softwareId: [{ required: true, message: t('endUser.softwareRequired'), trigger: 'change' }],
  username: [
    { required: true, message: t('endUser.usernameRequired'), trigger: 'blur' },
    { min: 3, max: 64, message: t('endUser.usernameLength'), trigger: 'blur' }
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (formMode.value === 'create') {
          if (!value) {
            callback(new Error(t('endUser.passwordRequiredOnCreate')))
          } else if (value.length < 6 || value.length > 64) {
            callback(new Error(t('endUser.passwordLength')))
          } else {
            callback()
          }
        } else {
          // 编辑时密码留空表示不修改，填写则校验长度
          if (value && (value.length < 6 || value.length > 64)) {
            callback(new Error(t('endUser.passwordLength')))
          } else {
            callback()
          }
        }
      },
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: t('endUser.emailInvalid'), trigger: 'blur' }]
}

function handleCreate() {
  formMode.value = 'create'
  form.id = undefined
  form.softwareId = undefined
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.status = 1
  form.remark = ''
  formDialogVisible.value = true
}

function handleEdit(row: EndUserRow) {
  formMode.value = 'edit'
  form.id = row.id
  form.softwareId = row.softwareId
  form.username = row.username
  form.password = ''
  form.nickname = row.nickname || ''
  form.email = row.email || ''
  form.phone = row.phone || ''
  form.status = row.status
  form.remark = row.remark || ''
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
        username: form.username,
        password: form.password || undefined,
        nickname: form.nickname || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined,
        status: form.status,
        remark: form.remark || undefined
      }
      if (formMode.value === 'create') {
        await endUserApi.create({ ...payload, password: form.password })
        ElMessage.success(t('endUser.createSuccess'))
      } else {
        await endUserApi.update({ id: form.id!, ...payload })
        ElMessage.success(t('endUser.updateSuccess'))
      }
      formDialogVisible.value = false
      loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

/* ============ 封禁/解封 ============ */
async function handleBan(row: EndUserRow) {
  try {
    await ElMessageBox.confirm(
      t('endUser.banConfirm', { username: row.username }),
      t('endUser.ban'),
      { confirmButtonText: t('endUser.ban'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await endUserApi.ban(row.id)
    ElMessage.success(t('endUser.banSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

async function handleUnban(row: EndUserRow) {
  try {
    await ElMessageBox.confirm(
      t('endUser.unbanConfirm', { username: row.username }),
      t('endUser.unban'),
      { confirmButtonText: t('endUser.unban'), cancelButtonText: t('common.cancel'), type: 'info' }
    )
  } catch {
    return
  }
  try {
    await endUserApi.unban(row.id)
    ElMessage.success(t('endUser.unbanSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

/* ============ 重置密码 ============ */
const resetPwdDialogVisible = ref(false)
const resetPwdLoading = ref(false)
const resetPwdFormRef = ref<FormInstance>()
const resetPwdForm = reactive({
  id: 0,
  username: '',
  newPassword: ''
})

const resetPwdRules: FormRules = {
  newPassword: [
    { required: true, message: t('endUser.newPasswordPlaceholder'), trigger: 'blur' },
    { min: 6, max: 64, message: t('endUser.passwordLength'), trigger: 'blur' }
  ]
}

function handleResetPwd(row: EndUserRow) {
  resetPwdForm.id = row.id
  resetPwdForm.username = row.username
  resetPwdForm.newPassword = ''
  resetPwdDialogVisible.value = true
}

async function submitResetPwd() {
  if (!resetPwdFormRef.value) return
  await resetPwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    resetPwdLoading.value = true
    try {
      await endUserApi.resetPassword({
        id: resetPwdForm.id,
        newPassword: resetPwdForm.newPassword
      })
      ElMessage.success(t('endUser.resetSuccess'))
      resetPwdDialogVisible.value = false
    } finally {
      resetPwdLoading.value = false
    }
  })
}

/* ============ 删除 ============ */
async function handleDelete(row: EndUserRow) {
  try {
    await ElMessageBox.confirm(
      t('endUser.deleteConfirm', { username: row.username }),
      t('common.delete'),
      { confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await endUserApi.delete(row.id)
    ElMessage.success(t('endUser.deleteSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}
</script>
