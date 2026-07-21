<!--
  极策k 金额输入组件
  作者: 极策k  日期: 2026-07-21

  用途：资金相关金额输入，使用 decimal.js 保证精度（禁用 number 直接运算）
  Props: modelValue, precision (默认 2), prefix (默认 ¥), placeholder
-->
<template>
  <el-input
    v-model="displayValue"
    :placeholder="placeholder"
    @blur="handleBlur"
  >
    <template v-if="prefix" #prefix>
      <span>{{ prefix }}</span>
    </template>
  </el-input>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import Decimal from 'decimal.js'

interface Props {
  modelValue: number | string
  placeholder?: string
  prefix?: string
  precision?: number
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入金额',
  prefix: '¥',
  precision: 2
})

const emit = defineEmits(['update:modelValue'])

const displayValue = ref('')

watch(
  () => props.modelValue,
  (val) => {
    if (val === null || val === undefined || val === '') {
      displayValue.value = ''
      return
    }
    try {
      displayValue.value = new Decimal(val).toFixed(props.precision)
    } catch {
      displayValue.value = String(val)
    }
  },
  { immediate: true }
)

const handleBlur = () => {
  if (displayValue.value === '') {
    emit('update:modelValue', '')
    return
  }
  try {
    const num = new Decimal(displayValue.value)
    emit('update:modelValue', num.toFixed(props.precision))
  } catch {
    emit('update:modelValue', '0.00')
  }
}
</script>
