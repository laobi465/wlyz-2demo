<!--
  极策k 状态标签组件
  作者: 极策k  日期: 2026-07-21

  用途：订单状态 / 卡密状态的统一标签展示
  Props: status (number), type ('order' | 'card')
  样式：jicek-tag-pending / success / danger / warning
-->
<template>
  <span class="jicek-tag" :class="tagClass">{{ text }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  status: number
  type?: 'order' | 'card'
}

const props = withDefaults(defineProps<Props>(), {
  type: 'order'
})

const orderMap: Record<number, { text: string; cls: string }> = {
  0: { text: '待支付', cls: 'jicek-tag-pending' },
  1: { text: '已支付', cls: 'jicek-tag-success' },
  2: { text: '失败', cls: 'jicek-tag-danger' },
  3: { text: '已退款', cls: 'jicek-tag-warning' },
  4: { text: '已关闭', cls: 'jicek-tag-pending' }
}

const cardMap: Record<number, { text: string; cls: string }> = {
  0: { text: '未使用', cls: 'jicek-tag-pending' },
  1: { text: '已使用', cls: 'jicek-tag-success' },
  2: { text: '已封禁', cls: 'jicek-tag-danger' },
  3: { text: '已退款', cls: 'jicek-tag-warning' },
  4: { text: '已过期', cls: 'jicek-tag-pending' }
}

const map = computed(() => (props.type === 'order' ? orderMap : cardMap))
const info = computed(() => map.value[props.status] || { text: '未知', cls: 'jicek-tag-pending' })
const text = computed(() => info.value.text)
const tagClass = computed(() => info.value.cls)
</script>
