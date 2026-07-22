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
          <span>控制台</span>
        </el-menu-item>
        <el-menu-item index="/software">
          <el-icon><Cpu /></el-icon>
          <span>软件管理</span>
        </el-menu-item>
        <el-sub-menu index="card">
          <template #title>
            <el-icon><Key /></el-icon>
            <span>卡密管理</span>
          </template>
          <el-menu-item index="/card-type">卡类管理</el-menu-item>
          <el-menu-item index="/card-key-gen">卡密生成</el-menu-item>
          <el-menu-item index="/card-key-list">卡密查询</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="user">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/device">设备管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="pay">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>支付管理</span>
          </template>
          <el-menu-item index="/pay-config">支付配置</el-menu-item>
          <el-menu-item index="/pay-order">资金流水</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="agent">
          <template #title>
            <el-icon><User /></el-icon>
            <span>代理管理</span>
          </template>
          <el-menu-item index="/agent">代理列表</el-menu-item>
          <el-menu-item index="/withdraw">提现审核</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="cloud">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>云端数据</span>
          </template>
          <el-menu-item index="/cloud-func">云函数</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="stats">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span>数据统计</span>
          </template>
          <el-menu-item index="/stats">数据统计</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/deploy">部署管理</el-menu-item>
          <el-menu-item index="/ticket">工单管理</el-menu-item>
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
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32">{{ avatarText }}</el-avatar>
              <span class="username">{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
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
      title="修改密码"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="90px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            placeholder="至少 8 位"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitChangePassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api'
import { TOKEN_KEY, USER_KEY } from '@/api/request'

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
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  ElMessage.success('已退出登录')
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
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度 8-64 字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
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
      ElMessage.success('密码修改成功，请重新登录')
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
