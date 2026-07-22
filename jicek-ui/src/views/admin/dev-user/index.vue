<!--
  极策k 管理员 - 开发者（租户）管理页面
  作者: 极策k  日期: 2026-07-22

  v0.15.0 管理员后台：
   - 管理员可管理全部租户开发者账号，不限 tenantId
   - 筛选：租户ID + 用户名模糊 + 状态下拉
   - 操作：封禁/解封切换 + 重置密码（弹窗）
   - 密码 BCrypt 哈希存储，明文永不返回（铁律 09）
   - 重置密码长度 8-64（与开发者登录一致）

  接口：
    GET  /api/admin/dev-user/page                分页查询
    GET  /api/admin/dev-user/{id}                详情
    POST /api/admin/dev-user/{id}/ban            封禁
    POST /api/admin/dev-user/{id}/unban          解封
    POST /api/admin/dev-user/reset-password      重置密码
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('admin.devUser.title') }}</span>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('admin.devUser.tenantId')">
          <el-input
            v-model.number="filter.tenantId"
            :placeholder="t('common.optional')"
            type="number"
            clearable
            style="width: 140px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item :label="t('admin.devUser.username')">
          <el-input
            v-model="filter.username"
            :placeholder="t('admin.devUser.usernameFuzzy')"
            clearable
            style="width: 180px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 120px" @change="loadData">
            <el-option :label="t('admin.devUser.normal')" :value="1" />
            <el-option :label="t('admin.devUser.banned')" :value="0" />
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
        <el-table-column prop="username" :label="t('admin.devUser.username')" min-width="120" />
        <el-table-column prop="nickname" :label="t('admin.devUser.nickname')" min-width="100" />
        <el-table-column prop="tenantId" :label="t('admin.devUser.tenantId')" width="90" />
        <el-table-column prop="email" :label="t('admin.devUser.email')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" :label="t('common.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? t('admin.devUser.normal') : t('admin.devUser.banned') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" :label="t('admin.devUser.lastLoginTime')" min-width="160" />
        <el-table-column prop="createTime" :label="t('common.createTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" size="small" @click="handleResetPwd(row)">
              {{ t('admin.devUser.resetPassword') }}
            </el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              size="small"
              @click="handleBan(row)"
            >
              {{ t('admin.devUser.ban') }}
            </el-button>
            <el-button
              v-else
              link
              type="primary"
              size="small"
              @click="handleUnban(row)"
            >
              {{ t('admin.devUser.unban') }}
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

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="resetPwdDialogVisible"
      :title="t('admin.devUser.resetPassword')"
      width="440px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        :title="t('admin.devUser.resetAlert')"
        style="margin-bottom: 16px"
      />
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item :label="t('admin.devUser.username')">
          <el-input :model-value="resetPwdForm.username" readonly />
        </el-form-item>
        <el-form-item :label="t('admin.devUser.newPassword')" prop="newPassword">
          <el-input
            v-model="resetPwdForm.newPassword"
            type="password"
            show-password
            :placeholder="t('admin.devUser.newPasswordPlaceholder')"
            maxlength="64"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="resetPwdLoading" @click="submitResetPwd">
          {{ t('admin.devUser.confirmReset') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { adminApi } from '@/api/admin'

const { t } = useI18n()

interface DevUserRow {
  id: number
  tenantId: number
  username: string
  nickname: string
  email: string
  status: number
  lastLoginTime: string
  lastLoginIp: string
  remark: string
  createTime: string
  updateTime: string
}

const loading = ref(false)
const tableData = ref<DevUserRow[]>([])
const total = ref(0)

const filter = reactive({
  current: 1,
  size: 20,
  tenantId: undefined as number | undefined,
  username: '',
  status: undefined as number | undefined
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await adminApi.devUserPage({
      current: filter.current,
      size: filter.size,
      tenantId: filter.tenantId || undefined,
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
  filter.tenantId = undefined
  filter.username = ''
  filter.status = undefined
  filter.current = 1
  loadData()
}

onMounted(() => {
  loadData()
})

/* ============ 封禁/解封 ============ */
async function handleBan(row: DevUserRow) {
  try {
    await ElMessageBox.confirm(
      t('admin.devUser.banConfirm', { username: row.username }),
      t('admin.devUser.ban'),
      { confirmButtonText: t('admin.devUser.ban'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await adminApi.devUserBan(row.id)
    ElMessage.success(t('admin.devUser.banSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

async function handleUnban(row: DevUserRow) {
  try {
    await ElMessageBox.confirm(
      t('admin.devUser.unbanConfirm', { username: row.username }),
      t('admin.devUser.unban'),
      { confirmButtonText: t('admin.devUser.unban'), cancelButtonText: t('common.cancel'), type: 'info' }
    )
  } catch {
    return
  }
  try {
    await adminApi.devUserUnban(row.id)
    ElMessage.success(t('admin.devUser.unbanSuccess'))
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
    { required: true, message: t('admin.devUser.newPasswordPlaceholder'), trigger: 'blur' },
    { min: 8, max: 64, message: t('admin.devUser.passwordLength'), trigger: 'blur' }
  ]
}

function handleResetPwd(row: DevUserRow) {
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
      await adminApi.devUserResetPassword({
        id: resetPwdForm.id,
        newPassword: resetPwdForm.newPassword
      })
      ElMessage.success(t('admin.devUser.resetSuccess'))
      resetPwdDialogVisible.value = false
    } finally {
      resetPwdLoading.value = false
    }
  })
}
</script>
