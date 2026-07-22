<!--
  极策k 部署管理页面（GitHub 自动更新）
  作者: 极策k  日期: 2026-07-22

  对应 docs/UI-DESIGN.md 6.2 节「系统设置 > 部署管理」+ PROJECT.md 第 7 节自动更新系统：
    - 当前部署状态卡片（是否部署中 + 最近一次部署结果）
    - 手动触发部署按钮（二次确认）
    - 部署审计日志分页表格

  接口：
    GET  /api/dev/deploy/status     当前状态
    POST /api/dev/deploy/manual     手动触发
    GET  /api/dev/deploy/log/page   日志分页

  安全：手动触发需二次确认；Webhook 自动触发由 GitHub 调用 /webhook 接口
-->
<template>
  <div class="jicek-page">
    <!-- 状态卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="jicek-card-title">部署功能</span>
          </template>
          <div class="status-block">
            <el-tag v-if="status.enabled" type="success" size="large">已启用</el-tag>
            <el-tag v-else type="info" size="large">未启用</el-tag>
            <div class="status-hint">
              {{ status.enabled ? 'Webhook 自动更新已开启' : '请在 application.yml 配置 jicek.deploy.enabled=true' }}
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="jicek-card-title">当前状态</span>
          </template>
          <div class="status-block">
            <el-tag v-if="status.deploying" type="warning" size="large">部署中...</el-tag>
            <el-tag v-else type="success" size="large">空闲</el-tag>
            <div class="status-hint">
              {{ status.deploying ? '部署任务进行中，请等待完成' : '当前无部署任务' }}
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="jicek-card-title">最近部署</span>
          </template>
          <div class="status-block">
            <el-tag
              v-if="status.lastDeploy"
              :type="deployTagType(status.lastDeploy.status)"
              size="large"
            >
              {{ deployStatusText(status.lastDeploy.status) }}
            </el-tag>
            <span v-else class="status-hint">暂无部署记录</span>
            <div v-if="status.lastDeploy" class="status-hint">
              {{ formatTime(status.lastDeploy.createTime) }} · {{ durationText(status.lastDeploy.durationMs) }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>
        <div class="card-header-with-action">
          <span class="jicek-card-title">部署日志</span>
          <div>
            <el-button
              type="primary"
              :loading="status.deploying"
              :disabled="!status.enabled || status.deploying"
              @click="handleManualDeploy"
            >
              手动触发部署
            </el-button>
            <el-button @click="reloadAll">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="进行中" :value="0" />
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="2" />
            <el-option label="已回滚" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="filter.triggerSource" placeholder="全部" clearable style="width: 120px">
            <el-option label="Webhook 自动" value="webhook" />
            <el-option label="手动触发" value="manual" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.triggerSource === 'webhook'" type="success" size="small">Webhook</el-tag>
            <el-tag v-else type="warning" size="small">手动</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="deployTagType(row.status)" size="small">
              {{ deployStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="branch" label="分支" width="100" />
        <el-table-column prop="commitHash" label="Commit" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="commit-hash">{{ row.commitHash ? row.commitHash.substring(0, 7) : '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ durationText(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorIp" label="操作 IP" width="140" />
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMessage" class="error-text">{{ row.errorMessage }}</span>
            <span v-else style="color: var(--jicek-text-secondary)">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="filter.current"
        v-model:page-size="filter.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadLog"
        @current-change="loadLog"
      />
    </el-card>

    <!-- 手动触发确认 -->
    <ConfirmDialog
      v-model="manualVisible"
      title="手动触发部署确认"
      type="warning"
      message="确认立即触发部署？"
      sub-message="将执行 git pull → 构建后端 → 构建前端 → 重启服务，过程约 1-3 分钟，期间服务可能短暂不可用"
      confirm-text="确认部署"
      @confirm="doManualDeploy"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { deployApi } from '@/api'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const manualVisible = ref(false)
const status = ref<any>({ enabled: false, deploying: false, lastDeploy: null })
let pollTimer: ReturnType<typeof setInterval> | null = null

const filter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  status: undefined as number | undefined,
  triggerSource: undefined as string | undefined
})

/**
 * 部署状态码 → el-tag type
 * 0进行中 1成功 2失败 3已回滚
 */
const deployTagType = (statusCode: number): string => {
  if (statusCode === 1) return 'success'
  if (statusCode === 0) return 'warning'
  if (statusCode === 2) return 'danger'
  if (statusCode === 3) return 'info'
  return 'info'
}

const deployStatusText = (statusCode: number): string => {
  if (statusCode === 0) return '进行中'
  if (statusCode === 1) return '成功'
  if (statusCode === 2) return '失败'
  if (statusCode === 3) return '已回滚'
  return '未知'
}

const durationText = (ms: number) => {
  if (ms === null || ms === undefined) return '-'
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const loadStatus = async () => {
  try {
    status.value = await deployApi.status()
  } catch {
    // 静默
  }
}

const loadLog = async () => {
  loading.value = true
  try {
    const resp: any = await deployApi.logPage(filter)
    tableData.value = resp.records || []
    total.value = resp.total || 0
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

const reloadAll = async () => {
  await Promise.all([loadStatus(), loadLog()])
}

const handleSearch = () => {
  filter.current = 1
  loadLog()
}

const handleReset = () => {
  filter.status = undefined
  filter.triggerSource = undefined
  filter.current = 1
  loadLog()
}

const handleManualDeploy = () => {
  manualVisible.value = true
}

const doManualDeploy = async () => {
  try {
    const resp: any = await deployApi.manual({
      tenantId: filter.tenantId,
      branch: 'main'
    })
    ElMessage.success(resp.message || '部署已触发')
    await reloadAll()
  } catch {
    // 错误已在拦截器处理
  }
}

/**
 * 轮询状态（部署进行中时每 5s 刷新，完成后停止）
 */
const startPolling = () => {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    await loadStatus()
    if (status.value.deploying) {
      // 部署中，继续轮询
    } else {
      // 部署完成，刷新日志并停止轮询
      await loadLog()
      stopPolling()
    }
  }, 5000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(async () => {
  await reloadAll()
  // 如果当前正在部署，启动轮询
  if (status.value.deploying) {
    startPolling()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.stat-row {
  margin-bottom: 0;
}

.status-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.status-hint {
  font-size: 13px;
  color: var(--jicek-text-secondary);
  text-align: center;
}

.card-header-with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.commit-hash {
  font-family: var(--jicek-font-mono);
  font-size: 12px;
  color: var(--jicek-primary);
}

.error-text {
  color: var(--jicek-danger);
  font-size: 12px;
}
</style>
