<!--
  极策k 开发者控制台页面
  作者: 极策k  日期: 2026-07-21

  展示：今日收入/订单/退款/净收入/卡密状态分布 + 卡密状态饼图
  数据源：GET /api/dev/dashboard/summary
  金额展示使用 decimal.js 保证精度
  图表：ECharts 5 饼图，使用极策k 色系
-->
<template>
  <div class="jicek-page">
    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.todayIncome') }}</div>
          <div class="jicek-stat-value success">¥{{ formatAmount(summary.todayIncome) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.todayOrder') }}</div>
          <div class="jicek-stat-value">{{ summary.todayOrderCount || 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.todayRefund') }}</div>
          <div class="jicek-stat-value danger">¥{{ formatAmount(summary.todayRefund) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.todayNetIncome') }}</div>
          <div class="jicek-stat-value success">¥{{ formatAmount(summary.todayNetIncome) }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.todayCardCount') }}</div>
          <div class="jicek-stat-value">{{ summary.todayCardCount || 0 }} {{ t('dashboard.cardUnit') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.cardUnused') }}</div>
          <div class="jicek-stat-value warning">{{ summary.cardStatus?.unused || 0 }} {{ t('dashboard.cardUnit') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.cardUsed') }}</div>
          <div class="jicek-stat-value success">{{ summary.cardStatus?.used || 0 }} {{ t('dashboard.cardUnit') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="jicek-stat-card">
          <div class="jicek-stat-label">{{ t('dashboard.cardBanned') }}</div>
          <div class="jicek-stat-value danger">{{ summary.cardStatus?.banned || 0 }} {{ t('dashboard.cardUnit') }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 卡密状态分布饼图 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="jicek-card-title">{{ t('dashboard.cardStatusDist') }}</span>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span class="jicek-card-title">{{ t('dashboard.todayOverview') }}</span>
          </template>
          <div ref="barChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api'
import Decimal from 'decimal.js'

const { t } = useI18n()

const summary = ref<any>({})

const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

const formatAmount = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  try {
    return new Decimal(val).toFixed(2)
  } catch {
    return '0.00'
  }
}

const loadSummary = async () => {
  // TODO: tenantId 从登录态获取，此处临时用 1
  try {
    summary.value = await dashboardApi.summary(1)
  } catch {
    // 接口未启动时静默
  }
}

const renderPieChart = () => {
  if (!pieChartRef.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  const cs = summary.value.cardStatus || {}
  const data = [
    { name: t('dashboard.cardUnused'), value: cs.unused || 0, itemStyle: { color: '#6B7280' } },
    { name: t('dashboard.cardUsed'), value: cs.used || 0, itemStyle: { color: '#2E7D5B' } },
    { name: t('dashboard.cardBanned'), value: cs.banned || 0, itemStyle: { color: '#B23A3A' } }
  ]
  const total = data.reduce((s, d) => s + d.value, 0)
  const cardUnit = t('dashboard.cardUnit')
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: `{b}: {c} ${cardUnit} ({d}%)`
    },
    legend: {
      bottom: 10,
      left: 'center',
      textStyle: { color: '#6B7280', fontSize: 13 }
    },
    series: [
      {
        name: t('dashboard.cardStatusSeries'),
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: total > 0 ? `{b}\n{c} ${cardUnit}` : '',
          color: '#1F2937',
          fontSize: 13
        },
        labelLine: { show: total > 0 },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        data: total > 0 ? data : [{ name: t('dashboard.noData'), value: 1, itemStyle: { color: '#E5E7EB' } }]
      }
    ]
  })
}

const renderBarChart = () => {
  if (!barChartRef.value) return
  if (!barChart) {
    barChart = echarts.init(barChartRef.value)
  }
  const income = Number(summary.value.todayIncome) || 0
  const refund = Number(summary.value.todayRefund) || 0
  const net = Number(summary.value.todayNetIncome) || 0
  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        return params
          .map((p: any) => `${p.name}: ¥${new Decimal(p.value).toFixed(2)}`)
          .join('<br/>')
      }
    },
    grid: { left: 60, right: 30, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: [t('dashboard.todayIncome'), t('dashboard.refund'), t('dashboard.netIncome')],
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#6B7280', fontSize: 13 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: { color: '#6B7280', fontSize: 12, formatter: '¥{value}' },
      splitLine: { lineStyle: { color: '#F0F4FA' } }
    },
    series: [
      {
        type: 'bar',
        barWidth: '40%',
        data: [
          { value: income, itemStyle: { color: '#2E7D5B' } },
          { value: refund, itemStyle: { color: '#B23A3A' } },
          { value: net, itemStyle: { color: '#1A4D8F' } }
        ],
        itemStyle: { borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

const handleResize = () => {
  pieChart?.resize()
  barChart?.resize()
}

watch(summary, () => {
  nextTick(() => {
    renderPieChart()
    renderBarChart()
  })
})

onMounted(async () => {
  await loadSummary()
  await nextTick()
  renderPieChart()
  renderBarChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.stat-row {
  margin-bottom: 16px;
}

.chart-container {
  width: 100%;
  height: 320px;
}
</style>
