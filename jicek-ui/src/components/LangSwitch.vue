<!--
  极策k网络验证 - 语言切换组件
  作者: 极策k  日期: 2026-07-22
  用途：顶栏右侧下拉切换中英文，选中态持久化到 localStorage
-->
<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <span class="lang-switch-trigger">
      <el-icon><Promotion /></el-icon>
      <span class="lang-label">{{ currentLabel }}</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :disabled="current === 'zh-CN'">
          简体中文
        </el-dropdown-item>
        <el-dropdown-item command="en-US" :disabled="current === 'en-US'">
          English
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/i18n'

const { locale } = useI18n()
const current = computed(() => locale.value)
const currentLabel = computed(() => (current.value === 'zh-CN' ? '简体中文' : 'English'))

function handleCommand(cmd: string) {
  if (cmd === 'zh-CN' || cmd === 'en-US') {
    setLocale(cmd)
    location.reload() // 刷新以同步 Element Plus 语言包
  }
}
</script>

<style scoped lang="scss">
.lang-switch-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: var(--jicek-text-secondary);
  font-size: 13px;
  padding: 0 8px;
  height: 32px;
  border-radius: 6px;
  transition: all 0.2s;
  &:hover {
    color: var(--jicek-primary);
    background: var(--jicek-bg-secondary);
  }
}
.lang-label {
  user-select: none;
}
</style>
