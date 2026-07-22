<!--
  极策k网络验证 - 开发者后台布局
  作者: 极策k  日期: 2026-07-21

  布局结构：220px 左侧导航 + 60px 顶栏 + 主内容区
  遵循 docs/UI-DESIGN.md 现代简约风格规范

  v0.7.0 鉴权：
   - 顶栏右侧显示当前登录用户昵称
   - 下拉菜单接入「修改密码」「退出登录」
-->
<template>
  <div class="jicek-layout">
    <!-- 左侧导航 -->
    <aside class="jicek-sidebar">
      <div class="logo">
        <span class="logo-text">极策k</span>
        <span class="logo-sub">网络验证</span>
      </div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>{{ t('menu.dashboard') }}</span>
        </el-menu-item>
        <el-menu-item index="/software">
          <el-icon><Cpu /></el-icon>
          <span>{{ t('menu.software') }}</span>
        </el-menu-item>
        <el-sub-menu index="card">
          <template #title>
            <el-icon><Key /></el-icon>
            <span>{{ t('menu.cardKey') }}</span>
          </template>
          <el-menu-item index="/card-type">{{ t('menu.cardType') }}</el-menu-item>
          <el-menu-item index="/card-key-gen">{{ t('menu.cardKeyGen') }}</el-menu-item>
          <el-menu-item index="/card-key-list">{{ t('menu.cardKeyList') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="user">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>{{ t('menu.userManage') }}</span>
          </template>
          <el-menu-item index="/device">{{ t('menu.device') }}</el-menu-item>
          <el-menu-item index="/end-user">{{ t('menu.endUser') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="pay">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>{{ t('menu.pay') }}</span>
          </template>
          <el-menu-item index="/pay-config">{{ t('menu.payConfig') }}</el-menu-item>
          <el-menu-item index="/pay-order">{{ t('menu.payOrder') }}</el-menu-item>
          <el-menu-item index="/shop">{{ t('menu.shop') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="agent">
          <template #title>
            <el-icon><User /></el-icon>
            <span>{{ t('menu.agent') }}</span>
          </template>
          <el-menu-item index="/agent">{{ t('menu.agentList') }}</el-menu-item>
          <el-menu-item index="/withdraw">{{ t('menu.withdraw') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="cloud">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>{{ t('menu.cloudData') }}</span>
          </template>
          <el-menu-item index="/cloud-func">{{ t('menu.cloudFunc') }}</el-menu-item>
          <el-menu-item index="/announcement">{{ t('menu.announcement') }}</el-menu-item>
          <el-menu-item index="/update-package">{{ t('menu.updatePackage') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="stats">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span>{{ t('menu.stats') }}</span>
          </template>
          <el-menu-item index="/stats">{{ t('menu.stats') }}</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>{{ t('menu.system') }}</span>
          </template>
          <el-menu-item index="/deploy">{{ t('menu.deploy') }}</el-menu-item>
          <el-menu-item index="/ticket">{{ t('menu.ticket') }}</el-menu-item>
          <el-menu-item index="/integration-doc">{{ t('menu.integrationDoc') }}</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <main class="jicek-main">
      <header class="jicek-header">
        <div class="header-left">
          <el-icon class="collapse-icon"><Fold /></el-icon>
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <LangSwitch class="header-lang" />
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32">{{ avatarText }}</el-avatar>
              <span class="username">{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePassword">{{ t('topbar.changePassword') }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>{{ t('topbar.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="jicek-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="pwdDialogVisible"
      :title="t('topbar.changePassword')"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="90px"
      >
        <el-form-item :label="t('topbar.oldPassword')" prop="oldPassword">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            show-password
            :placeholder="t('topbar.oldPassword')"
          />
        </el-form-item>
        <el-form-item :label="t('topbar.newPassword')" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            :placeholder="t('topbar.passwordTooShort')"
          />
        </el-form-item>
        <el-form-item :label="t('topbar.confirmPassword')" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            :placeholder="t('topbar.confirmPassword')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitChangePassword">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api'
import { TOKEN_KEY, USER_KEY } from '@/api/request'
import LangSwitch from '@/components/LangSwitch.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string) || '极策k')

interface StoredUser {
  userId: number
  role: number
  tenantId: number
  username: string
  nickname?: string
}

const currentUser = ref<StoredUser | null>(null)

const displayName = computed(() => {
  const u = currentUser.value
  if (!u) return '未登录'
  return u.nickname || u.username || '开发者'
})

const avatarText = computed(() => {
  const u = currentUser.value
  if (!u) return '客'
  const name = u.nickname || u.username || ''
  return name ? name.charAt(0).toUpperCase() : '极'
})

onMounted(() => {
  loadUserFromStorage()
  // 后端校验 token 有效性并补全信息
  refreshUserInfo()
})

function loadUserFromStorage() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return
  try {
    currentUser.value = JSON.parse(raw) as StoredUser
  } catch {
    localStorage.removeItem(USER_KEY)
  }
}

async function refreshUserInfo() {
  if (!localStorage.getItem(TOKEN_KEY)) return
  try {
    const info: any = await authApi.me()
    currentUser.value = {
      userId: info.userId,
      role: info.role,
      tenantId: info.tenantId,
      username: info.username,
      nickname: info.nickname
    }
    localStorage.setItem(USER_KEY, JSON.stringify(currentUser.value))
  } catch {
    // 拦截器会处理 token 失效跳转
  }
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await handleLogout()
  } else if (cmd === 'changePassword') {
    openPwdDialog()
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm(t('topbar.logoutConfirm'), t('topbar.logout'), {
      confirmButtonText: t('topbar.logout'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  ElMessage.success(t('topbar.logout'))
  router.push('/login')
}

/* ============ 修改密码 ============ */
const pwdDialogVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: t('topbar.oldPassword'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('topbar.newPassword'), trigger: 'blur' },
    { min: 8, max: 64, message: t('topbar.passwordTooShort'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('topbar.confirmPassword'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error(t('topbar.passwordMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function openPwdDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

async function submitChangePassword() {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdLoading.value = true
    try {
      await authApi.changePassword({
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      ElMessage.success(t('topbar.changePassword'))
      pwdDialogVisible.value = false
      // 修改密码后强制重新登录
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      setTimeout(() => router.push('/login'), 1000)
    } catch {
      // 拦截器已提示
    } finally {
      pwdLoading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.logo {
  height: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-bottom: 1px solid var(--jicek-border);

  .logo-text {
    font-size: 20px;
    font-weight: 700;
    color: var(--jicek-primary);
    letter-spacing: 2px;
  }

  .logo-sub {
    font-size: 11px;
    color: var(--jicek-text-secondary);
    margin-top: 2px;
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;

  .collapse-icon {
    font-size: 20px;
    cursor: pointer;
    color: var(--jicek-text-secondary);
  }

  .page-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--jicek-text-primary);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  .username {
    font-size: 14px;
    color: var(--jicek-text-primary);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
