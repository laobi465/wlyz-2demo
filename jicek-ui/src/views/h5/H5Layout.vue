<!--
  极策k网络验证 - H5 终端用户布局
  作者: 极策k  日期: 2026-07-22

  v0.13.0 H5 终端用户：
   - 375px 设计稿居中容器（max-width 480px 兼容 PC 浏览）
   - 44px 顶部导航 + 56px 底部 Tab 栏（4 Tab）
   - H5 鉴权独立 X-H5-Token（与开发者后台 Authorization Bearer 完全隔离）
   - 未登录访问非 /h5/login 页面自动跳 /h5/login
   - 遵循 docs/UI-DESIGN.md 现代简约风格规范（无渐变、无 emoji）
-->
<template>
  <div class="h5-root">
    <div class="h5-container">
      <!-- 顶部导航 44px -->
      <header class="h5-header">
        <div class="h5-header-left" @click="handleBack">
          <el-icon v-if="canBack"><ArrowLeft /></el-icon>
          <span v-else class="h5-header-placeholder"></span>
        </div>
        <div class="h5-header-title">{{ pageTitle }}</div>
        <div class="h5-header-right">
          <span class="h5-header-placeholder"></span>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="h5-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <!-- 底部 Tab 栏 56px -->
      <nav class="h5-tabbar">
        <div
          v-for="tab in tabs"
          :key="tab.path"
          class="h5-tab-item"
          :class="{ active: isActive(tab) }"
          @click="handleTabClick(tab)"
        >
          <el-icon class="h5-tab-icon"><component :is="tab.icon" /></el-icon>
          <span class="h5-tab-label">{{ tab.label }}</span>
        </div>
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { h5Api, H5_TOKEN_KEY } from '@/api'

interface TabItem {
  path: string
  label: string
  icon: string
  needAuth?: boolean
  action?: 'logout'
}

const route = useRoute()
const router = useRouter()

const tabs: TabItem[] = [
  { path: '/h5/my-card', label: '我的卡密', icon: 'Key', needAuth: true },
  { path: '/h5/announcement', label: '公告', icon: 'Bell', needAuth: true },
  { path: '/h5/shop', label: '购卡', icon: 'ShoppingCart' },
  { path: '/h5/login', label: '退出', icon: 'SwitchButton', action: 'logout' }
]

const pageTitle = computed(() => (route.meta.title as string) || '极策k')

const canBack = computed(() => {
  // 登录页不显示返回
  return route.path !== '/h5/login' && route.path !== '/h5/my-card'
})

function isActive(tab: TabItem): boolean {
  if (tab.path === '/h5/shop') {
    return route.path.startsWith('/h5/shop')
  }
  return route.path === tab.path
}

function handleBack() {
  if (!canBack.value) return
  router.back()
}

function hasH5Token(): boolean {
  return !!localStorage.getItem(H5_TOKEN_KEY)
}

function handleTabClick(tab: TabItem) {
  if (tab.action === 'logout') {
    handleLogout()
    return
  }
  if (tab.needAuth && !hasH5Token()) {
    ElMessage.warning('请先登录')
    router.push({ path: '/h5/login', query: { redirect: tab.path } })
    return
  }
  router.push(tab.path)
}

async function handleLogout() {
  if (!hasH5Token()) {
    router.push('/h5/login')
    return
  }
  try {
    await h5Api.logout()
  } catch {
    // 忽略退出失败，本地清理即可
  } finally {
    localStorage.removeItem(H5_TOKEN_KEY)
    ElMessage.success('已退出登录')
    router.push('/h5/login')
  }
}

onMounted(() => {
  // 未登录访问需鉴权页面，跳登录
  const needAuth = route.meta.h5Auth === true
  if (needAuth && !hasH5Token()) {
    router.replace({ path: '/h5/login', query: { redirect: route.fullPath } })
  }
})
</script>

<style scoped lang="scss">
.h5-root {
  min-height: 100vh;
  background: var(--jicek-bg-secondary);
  display: flex;
  justify-content: center;
}

.h5-container {
  width: 100%;
  max-width: 480px;
  min-height: 100vh;
  background: var(--jicek-bg-primary);
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 0 16px rgba(0, 0, 0, 0.04);
}

.h5-header {
  height: 44px;
  background: var(--jicek-bg-primary);
  border-bottom: 1px solid var(--jicek-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  position: sticky;
  top: 0;
  z-index: 10;

  .h5-header-left,
  .h5-header-right {
    width: 32px;
    display: flex;
    align-items: center;
    cursor: pointer;
    color: var(--jicek-text-primary);
    font-size: 18px;
  }

  .h5-header-right {
    cursor: default;
  }

  .h5-header-placeholder {
    width: 18px;
    display: inline-block;
  }

  .h5-header-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 24px;
  }
}

.h5-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: var(--jicek-bg-secondary);
}

.h5-tabbar {
  height: 56px;
  background: var(--jicek-bg-primary);
  border-top: 1px solid var(--jicek-border);
  display: flex;
  position: sticky;
  bottom: 0;
  z-index: 10;

  .h5-tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: var(--jicek-text-secondary);
    transition: var(--jicek-transition);

    .h5-tab-icon {
      font-size: 20px;
      line-height: 1;
    }

    .h5-tab-label {
      font-size: 12px;
      margin-top: 2px;
      line-height: 16px;
    }

    &.active {
      color: var(--jicek-primary);
      font-weight: 600;
    }
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
