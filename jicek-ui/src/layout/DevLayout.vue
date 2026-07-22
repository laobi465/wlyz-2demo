<!--
  极策k网络验证 - 开发者后台布局
  作者: 极策k  日期: 2026-07-21

  布局结构：220px 左侧导航 + 60px 顶栏 + 主内容区
  遵循 docs/UI-DESIGN.md 现代简约风格规范
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
        <el-sub-menu index="card">
          <template #title>
            <el-icon><Key /></el-icon>
            <span>卡密管理</span>
          </template>
          <el-menu-item index="/card-key-gen">卡密生成</el-menu-item>
          <el-menu-item index="/card-key-list">卡密查询</el-menu-item>
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
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32">极</el-avatar>
              <span class="username">开发者</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
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
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => (route.meta.title as string) || '极策k')
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
