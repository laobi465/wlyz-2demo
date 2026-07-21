<!--
  极策k 二次确认弹窗组件
  作者: 极策k  日期: 2026-07-21

  用途：资金操作（退款）/ 卡密操作（封禁/退款）前的二次确认
  Props: title, message, subMessage, type (warning/danger/success), confirmText
  安全：禁止点击遮罩关闭、禁止 ESC 关闭，强制用户主动选择
-->
<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
  >
    <div class="confirm-content">
      <el-icon class="confirm-icon" :color="iconColor">
        <component :is="iconName" />
      </el-icon>
      <div class="confirm-text">
        <p class="confirm-message">{{ message }}</p>
        <p v-if="subMessage" class="confirm-sub">{{ subMessage }}</p>
      </div>
    </div>
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button :type="btnType" @click="handleConfirm">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  modelValue: boolean
  title?: string
  message: string
  subMessage?: string
  type?: 'warning' | 'danger' | 'success'
  confirmText?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '操作确认',
  type: 'warning',
  confirmText: '确认'
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const iconName = computed(() => {
  return {
    warning: 'WarningFilled',
    danger: 'CircleCloseFilled',
    success: 'CircleCheckFilled'
  }[props.type]
})

const iconColor = computed(() => {
  return {
    warning: 'var(--jicek-warning)',
    danger: 'var(--jicek-danger)',
    success: 'var(--jicek-success)'
  }[props.type]
})

const btnType = computed(() => {
  return props.type === 'danger' ? 'danger' : 'primary'
})

const handleConfirm = () => {
  emit('confirm')
  visible.value = false
}

const handleCancel = () => {
  emit('cancel')
  visible.value = false
}
</script>

<style scoped lang="scss">
.confirm-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
}

.confirm-icon {
  font-size: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}

.confirm-text {
  flex: 1;
}

.confirm-message {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--jicek-text-primary);
  font-weight: 500;
}

.confirm-sub {
  margin: 0;
  font-size: 13px;
  color: var(--jicek-text-secondary);
}
</style>
