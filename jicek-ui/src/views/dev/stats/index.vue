<!--
  极策k 数据统计与可视化页面
  作者: 极策k  日期: 2026-07-22

  对应 docs/UI-DESIGN.md 6.2 节「数据统计」菜单（4 子项）：
    1. 验证量趋势（折线图，按小时/天/月）
    2. 设备在线热力图
    3. 收入统计（按通道/卡类/代理分维度）
    4. 防破解事件（封禁 IP/设备、签名失败次数）

  接口：
    GET /api/dev/stats/verify-trend   验证量趋势
    GET /api/dev/stats/device-heatmap 设备热力图
    GET /api/dev/stats/income         收入统计
    GET /api/dev/stats/anti-crack     防破解事件

  图表：ECharts 5（折线/热力/柱状），使用极策k 色系
  金额展示使用 decimal.js 保证精度
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">数据统计</span>
      </template>

      <!-- 全局筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="软件ID">
          <el-input-number v-model="filter.softwareId" :min="1" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reloadAll">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 1. 验证量趋势 -->
        <el-tab-pane label="验证量趋势" name="verify">
          <div class="tab-toolbar">
            <el-form :inline="true" :model="verifyFilter">
              <el-form-item label="粒度">
                <el-radio-group v-model="verifyFilter.granularity" @change="loadVerify">
                  <el-radio-button label="hour">按小时</el-radio-button>
                  <el-radio-button label="day">按天</el-radio-button>
                  <el-radio-button label="month">按月</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="verifyFilter.granularity !== 'hour'" label="天数">
                <el-input-number v-model="verifyFilter.days" :min="1" :max="90" style="width: 100px" @change="loadVerify" />
              </el-form-item>
            </el-form>
          </div>

          <el-row :gutter="16" class="stat-row">
            <el-col :span="12">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">总激活次数</div>
                <div class="jicek-stat-value success">{{ verifyData.totalActivate || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">总新增设备</div>
                <div class="jicek-stat-value">{{ verifyData.totalNewDevice || 0 }}</div>
              </div>
            </el-col>
          </el-row>

          <div ref="verifyChartRef" class="chart-container-lg"></div>
        </el-tab-pane>

        <!-- 2. 设备在线热力图 -->
        <el-tab-pane label="设备在线热力图" name="heatmap">
          <el-row :gutter="16" class="stat-row">
            <el-col :span="12">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">当前在线设备</div>
                <div class="jicek-stat-value success">{{ heatmapData.currentOnline || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">总设备数</div>
                <div class="jicek-stat-value">{{ heatmapData.totalDevice || 0 }}</div>
              </div>
            </el-col>
          </el-row>

          <div ref="heatmapChartRef" class="chart-container-lg"></div>
        </el-tab-pane>

        <!-- 3. 收入统计 -->
        <el-tab-pane label="收入统计" name="income">
          <div class="tab-toolbar">
            <el-form :inline="true" :model="incomeFilter">
              <el-form-item label="维度">
                <el-radio-group v-model="incomeFilter.dimension" @change="loadIncome">
                  <el-radio-button label="channel">支付通道</el-radio-button>
                  <el-radio-button label="cardType">卡类</el-radio-button>
                  <el-radio-button label="agent">代理</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="天数">
                <el-input-number v-model="incomeFilter.days" :min="1" :max="90" style="width: 100px" @change="loadIncome" />
              </el-form-item>
            </el-form>
          </div>

          <el-row :gutter="16" class="stat-row">
            <el-col :span="12">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">总收入金额</div>
                <div class="jicek-stat-value success">¥{{ formatAmount(incomeData.totalAmount) }}</div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">总订单数</div>
                <div class="jicek-stat-value">{{ incomeData.totalCount || 0 }}</div>
              </div>
            </el-col>
          </el-row>

          <el-alert
            v-if="incomeFilter.dimension === 'agent'"
            type="info"
            :closable="false"
            title="代理维度统计待 PayOrder 扩展 agent_id 字段后开放"
            style="margin-bottom: 16px"
          />

          <div ref="incomeChartRef" class="chart-container-lg"></div>

          <el-table v-if="(incomeData.items || []).length > 0" :data="incomeData.items" border stripe style="margin-top: 16px">
            <el-table-column type="index" label="#" width="60" />
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="count" label="订单数" width="120" />
            <el-table-column label="金额" min-width="140">
              <template #default="{ row }">
                <span class="amount-text">¥{{ formatAmount(row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="占比" min-width="200">
              <template #default="{ row }">
                <el-progress :percentage="amountPercent(row.amount)" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 4. 防破解事件 -->
        <el-tab-pane label="防破解事件" name="antiCrack">
          <div class="tab-toolbar">
            <el-form :inline="true" :model="antiFilter">
              <el-form-item label="天数">
                <el-input-number v-model="antiFilter.days" :min="1" :max="90" style="width: 100px" @change="loadAntiCrack" />
              </el-form-item>
            </el-form>
          </div>

          <el-row :gutter="16" class="stat-row">
            <el-col :span="8">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">封禁设备数</div>
                <div class="jicek-stat-value danger">{{ antiCrackData.bannedDeviceCount || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">封禁卡密数</div>
                <div class="jicek-stat-value danger">{{ antiCrackData.bannedCardCount || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="jicek-stat-card">
                <div class="jicek-stat-label">封禁 IP 数</div>
                <div class="jicek-stat-value warning">{{ antiCrackData.bannedIpCount || 0 }}</div>
              </div>
            </el-col>
          </el-row>

          <div ref="antiChartRef" class="chart-container-lg"></div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { statsApi } from '@/api'
import Decimal from 'decimal.js'

// 全局筛选
const filter = reactive({
  tenantId: 1,
  softwareId: undefined as number | undefined
})

const activeTab = ref('verify')

/* ============ 1. 验证量趋势 ============ */
const verifyFilter = reactive({
  granularity: 'day',
  days: 7
})
const verifyData = ref<any>({})
const verifyChartRef = ref<HTMLElement>()
let verifyChart: echarts.ECharts | null = null

const loadVerify = async () => {
  try {
    verifyData.value = await statsApi.verifyTrend({
      tenantId: filter.tenantId,
      softwareId: filter.softwareId,
      granularity: verifyFilter.granularity,
      days: verifyFilter.days
    })
    await nextTick()
    renderVerifyChart()
  } catch {
    // 错误已在拦截器处理
  }
}

const renderVerifyChart = () => {
  if (!verifyChartRef.value) return
  if (!verifyChart) {
    verifyChart = echarts.init(verifyChartRef.value)
  }
  const labels = verifyData.value.labels || []
  const activate = verifyData.value.activateCounts || []
  const devices = verifyData.value.newDeviceCounts || []
  verifyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['卡密激活', '新增设备'], bottom: 0, textStyle: { color: '#6B7280' } },
    grid: { left: 60, right: 30, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#6B7280', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: '#6B7280', fontSize: 12 },
      splitLine: { lineStyle: { color: '#F0F4FA' } }
    },
    series: [
      {
        name: '卡密激活',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: activate,
        itemStyle: { color: '#2E7D5B' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(46, 125, 91, 0.25)' },
            { offset: 1, color: 'rgba(46, 125, 91, 0.02)' }
          ])
        }
      },
      {
        name: '新增设备',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: devices,
        itemStyle: { color: '#1A4D8F' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(26, 77, 143, 0.25)' },
            { offset: 1, color: 'rgba(26, 77, 143, 0.02)' }
          ])
        }
      }
    ]
  })
}

/* ============ 2. 设备在线热力图 ============ */
const heatmapData = ref<any>({})
const heatmapChartRef = ref<HTMLElement>()
let heatmapChart: echarts.ECharts | null = null

const loadHeatmap = async () => {
  try {
    heatmapData.value = await statsApi.deviceHeatmap({
      tenantId: filter.tenantId,
      softwareId: filter.softwareId,
      days: 7
    })
    await nextTick()
    renderHeatmapChart()
  } catch {
    // 静默
  }
}

const renderHeatmapChart = () => {
  if (!heatmapChartRef.value) return
  if (!heatmapChart) {
    heatmapChart = echarts.init(heatmapChartRef.value)
  }
  const days = heatmapData.value.days || []
  const hours = heatmapData.value.hours || []
  const points = heatmapData.value.points || []
  const maxVal = points.reduce((m: number, p: any) => Math.max(m, Number(p[2]) || 0), 0)
  heatmapChart.setOption({
    tooltip: {
      position: 'top',
      formatter: (p: any) => {
        const dayIdx = p.value[0]
        const hour = p.value[1]
        const val = p.value[2]
        const dayLabel = days[dayIdx] || ''
        return `${dayLabel} ${String(hour).padStart(2, '0')}:00<br/>在线设备: <b>${val}</b>`
      }
    },
    grid: { left: 100, right: 30, top: 30, bottom: 60 },
    xAxis: {
      type: 'category',
      data: hours.map((h: number) => `${String(h).padStart(2, '0')}时`),
      splitArea: { show: true },
      axisLabel: { color: '#6B7280', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: { show: true },
      axisLabel: { color: '#6B7280', fontSize: 11 }
    },
    visualMap: {
      min: 0,
      max: Math.max(maxVal, 1),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 5,
      textStyle: { color: '#6B7280' },
      inRange: { color: ['#F0F4FA', '#A8C8B5', '#2E7D5B'] }
    },
    series: [
      {
        type: 'heatmap',
        data: points,
        label: { show: false },
        emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0, 0, 0, 0.3)' } }
      }
    ]
  })
}

/* ============ 3. 收入统计 ============ */
const incomeFilter = reactive({
  dimension: 'channel',
  days: 30
})
const incomeData = ref<any>({})
const incomeChartRef = ref<HTMLElement>()
let incomeChart: echarts.ECharts | null = null

const loadIncome = async () => {
  try {
    incomeData.value = await statsApi.income({
      tenantId: filter.tenantId,
      softwareId: filter.softwareId,
      dimension: incomeFilter.dimension,
      days: incomeFilter.days
    })
    await nextTick()
    renderIncomeChart()
  } catch {
    // 静默
  }
}

const renderIncomeChart = () => {
  if (!incomeChartRef.value) return
  if (!incomeChart) {
    incomeChart = echarts.init(incomeChartRef.value)
  }
  const items = incomeData.value.items || []
  const names = items.map((i: any) => i.name)
  const amounts = items.map((i: any) => Number(i.amount) || 0)
  const counts = items.map((i: any) => Number(i.count) || 0)
  incomeChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        return params
          .map((p: any) => {
            if (p.seriesName === '金额') {
              return `${p.name}<br/>${p.seriesName}: ¥${new Decimal(p.value).toFixed(2)}`
            }
            return `${p.seriesName}: ${p.value}`
          })
          .join('<br/>')
      }
    },
    legend: { data: ['金额', '订单数'], bottom: 0, textStyle: { color: '#6B7280' } },
    grid: { left: 70, right: 60, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      data: names,
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#6B7280', fontSize: 11, interval: 0, rotate: names.length > 6 ? 30 : 0 }
    },
    yAxis: [
      {
        type: 'value',
        name: '金额',
        position: 'left',
        axisLine: { show: false },
        axisLabel: { color: '#6B7280', fontSize: 12, formatter: '¥{value}' },
        splitLine: { lineStyle: { color: '#F0F4FA' } }
      },
      {
        type: 'value',
        name: '订单数',
        position: 'right',
        minInterval: 1,
        axisLine: { show: false },
        axisLabel: { color: '#6B7280', fontSize: 12 }
      }
    ],
    series: [
      {
        name: '金额',
        type: 'bar',
        barWidth: '40%',
        data: amounts.map((v: number) => ({ value: v, itemStyle: { color: '#2E7D5B' } })),
        itemStyle: { borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: counts,
        itemStyle: { color: '#1A4D8F' },
        yAxisIndex: 1
      }
    ]
  })
}

const amountPercent = (amount: any) => {
  const total = Number(incomeData.value.totalAmount) || 0
  if (total <= 0) return 0
  try {
    return Number(new Decimal(amount).div(total).mul(100).toFixed(1))
  } catch {
    return 0
  }
}

/* ============ 4. 防破解事件 ============ */
const antiFilter = reactive({ days: 30 })
const antiCrackData = ref<any>({})
const antiChartRef = ref<HTMLElement>()
let antiChart: echarts.ECharts | null = null

const loadAntiCrack = async () => {
  try {
    antiCrackData.value = await statsApi.antiCrack({
      tenantId: filter.tenantId,
      softwareId: filter.softwareId,
      days: antiFilter.days
    })
    await nextTick()
    renderAntiChart()
  } catch {
    // 静默
  }
}

const renderAntiChart = () => {
  if (!antiChartRef.value) return
  if (!antiChart) {
    antiChart = echarts.init(antiChartRef.value)
  }
  const labels = antiCrackData.value.labels || []
  const deviceSeries = antiCrackData.value.bannedDeviceTrend || []
  const cardSeries = antiCrackData.value.bannedCardTrend || []
  antiChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['封禁设备', '封禁卡密'], bottom: 0, textStyle: { color: '#6B7280' } },
    grid: { left: 60, right: 30, top: 30, bottom: 50 },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#E5E7EB' } },
      axisLabel: { color: '#6B7280', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisLabel: { color: '#6B7280', fontSize: 12 },
      splitLine: { lineStyle: { color: '#F0F4FA' } }
    },
    series: [
      {
        name: '封禁设备',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: deviceSeries,
        itemStyle: { color: '#B23A3A' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(178, 58, 58, 0.25)' },
            { offset: 1, color: 'rgba(178, 58, 58, 0.02)' }
          ])
        }
      },
      {
        name: '封禁卡密',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: cardSeries,
        itemStyle: { color: '#D97706' }
      }
    ]
  })
}

/* ============ 工具方法 ============ */
const formatAmount = (val: any) => {
  if (val === null || val === undefined) return '0.00'
  try {
    return new Decimal(val).toFixed(2)
  } catch {
    return '0.00'
  }
}

const handleTabChange = async (name: string) => {
  await nextTick()
  if (name === 'verify') {
    if (!verifyData.value.labels) await loadVerify()
    else renderVerifyChart()
  } else if (name === 'heatmap') {
    if (!heatmapData.value.days) await loadHeatmap()
    else renderHeatmapChart()
  } else if (name === 'income') {
    if (!incomeData.value.dimension) await loadIncome()
    else renderIncomeChart()
  } else if (name === 'antiCrack') {
    if (!antiCrackData.value.labels) await loadAntiCrack()
    else renderAntiChart()
  }
  // 切换 Tab 后所有图表 resize 一次，避免尺寸未初始化
  setTimeout(() => resizeAll(), 50)
}

const reloadAll = async () => {
  await Promise.all([loadVerify(), loadHeatmap(), loadIncome(), loadAntiCrack()])
}

const resizeAll = () => {
  verifyChart?.resize()
  heatmapChart?.resize()
  incomeChart?.resize()
  antiChart?.resize()
}

watch(() => filter.softwareId, () => {
  reloadAll()
})

onMounted(async () => {
  await loadVerify()
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  verifyChart?.dispose()
  heatmapChart?.dispose()
  incomeChart?.dispose()
  antiChart?.dispose()
})
</script>

<style scoped>
.tab-toolbar {
  margin-bottom: 16px;
}

.stat-row {
  margin-bottom: 16px;
}

.chart-container-lg {
  width: 100%;
  height: 420px;
}

.amount-text {
  font-family: var(--jicek-font-mono);
  color: var(--jicek-primary);
  font-weight: 600;
}
</style>
