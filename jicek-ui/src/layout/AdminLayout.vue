<!--
  极策k网络验证 - 管理员后台布局
  作者: 极策k  日期: 2026-07-22

  布局结构：220px 左侧导航 + 60px 顶栏 + 主内容区
  遵循 docs/UI-DESIGN.md 现代简约风格规范

  v0.15.0 管理员后台：
   - 左侧菜单仅 2 项：工单管理 / 开发者管理
   - 顶栏：标题「极策k 管理后台」+ LangSwitch + 用户下拉（退出）
   - 管理员 token 独立存储（jicek_admin_token），与开发者隔离
-->
<template>
  <div class="jicek-layout">
    <!-- 左侧导航 -->
    <aside class="jicek-sidebar">
      <div class="logo">
        <span class="logo-text">极策k</span>
        <span class="logo-sub">{{ t('admin.title') }}</span>
      </div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/admin/ticket">
          <el-icon><Service /></el-icon>
          <span>{{ t('admin.menu.ticket') }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/dev-user">
          <el-icon><User /></el-icon>
          <span>{{ t('admin.menu.devUser') }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <main class="jicek-main">
      <header class="jicek-header">
        <div class="header-left">
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
                <el-dropdown-item command="logout" divided>{{ t('admin.topbar.logout') }}</el-dropdown-item>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ADMIN_TOKEN_KEY, ADMIN_USER_KEY, adminAuthApi } from '@/api/admin'
import LangSwitch from '@/components/LangSwitch.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string) || t('admin.title'))

interface StoredAdminUser {
  userId: number
  role: number
  username: string
  nickname?: string
}

const currentUser = ref<StoredAdminUser | null>(null)

const displayName = computed(() => {
  const u = currentUser.value
  if (!u) return t('admin.title')
  return u.nickname || u.username || 'Admin'
})

const avatarText = computed(() => {
  const u = currentUser.value
  if (!u) return '极'
  const name = u.nickname || u.username || ''
  return name ? name.charAt(0).toUpperCase() : '极'
})

onMounted(() => {
  loadUserFromStorage()
  refreshUserInfo()
})

function loadUserFromStorage() {
  const raw = localStorage.getItem(ADMIN_USER_KEY)
  if (!raw) return
  try {
    currentUser.value = JSON.parse(raw) as StoredAdminUser
  } catch {
    localStorage.removeItem(ADMIN_USER_KEY)
  }
}

async function refreshUserInfo() {
  if (!localStorage.getItem(ADMIN_TOKEN_KEY)) return
  try {
    const info: any = await adminAuthApi.me()
    currentUser.value = {
      userId: info.userId,
      role: info.role,
      username: info.username,
      nickname: info.nickname
    }
    localStorage.setItem(ADMIN_USER_KEY, JSON.stringify(currentUser.value))
  } catch {
    // 拦截器会处理 token 失效跳转
  }
}

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await handleLogout()
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm(t('admin.topbar.logoutConfirm'), t('admin.topbar.logout'), {
      confirmButtonText: t('admin.topbar.logout'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_USER_KEY)
  ElMessage.success(t('admin.topbar.logout'))
  router.push('/admin/login')
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
