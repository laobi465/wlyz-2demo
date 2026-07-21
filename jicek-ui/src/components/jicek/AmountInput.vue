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
