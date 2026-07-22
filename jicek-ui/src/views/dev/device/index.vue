<!--
  极策k 设备管理页面
  作者: 极策k  日期: 2026-07-22

  功能：设备分页查询（按软件/状态/在线状态筛选）+ 封禁/解封 + 详情查看
  接口：
    GET  /api/dev/device/page              分页查询
    GET  /api/dev/device/{tenantId}/{deviceId}  详情
    POST /api/dev/device/ban               封禁
    POST /api/dev/device/unban             解封
  安全：封禁需二次确认；设备指纹仅显示前 16 位（脱敏）；deviceInfo 为加密 JSON 不直接展示
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">设备管理</span>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item label="软件ID">
          <el-input-number v-model="filter.softwareId" :min="1" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="封禁" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="在线状态">
          <el-select v-model="filter.onlineStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="在线" :value="1" />
            <el-option label="离线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="softwareId" label="软件ID" width="90" />
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="osType" label="系统" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="osTagType(row.osType)">{{ osText(row.osType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clientVersion" label="客户端版本" width="120" />
        <el-table-column label="虚拟机" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isVm === 1" type="warning" size="small">VM</el-tag>
            <span v-else style="color: var(--jicek-text-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column label="指纹" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="fingerprint-text">{{ maskFingerprint(row.deviceFingerprint) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bindIp" label="绑定IP" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :status="deviceTagStatus(row)" type="device" />
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="160" />
        <el-table-column prop="bindTime" label="绑定时间" min-width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              link
              type="danger"
              size="small"
              @click="handleBan(row)"
            >
              封禁
            </el-button>
            <el-button
              v-else
              link
              type="success"
              size="small"
              @click="handleUnban(row)"
            >
              解封
            </el-button>
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
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="设备详情" width="600px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="设备 ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="软件 ID">{{ detailData.softwareId }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ osText(detailData.osType) }} {{ detailData.osVersion || '' }}</el-descriptions-item>
        <el-descriptions-item label="客户端版本">{{ detailData.clientVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="虚拟机">{{ detailData.isVm === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="绑定用户 ID">{{ detailData.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="绑定 IP">{{ detailData.bindIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备指纹" :span="2">
          <code class="fingerprint-full">{{ detailData.deviceFingerprint }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="换机码">{{ detailData.bindCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :status="deviceTagStatus(detailData)" type="device" />
        </el-descriptions-item>
        <el-descriptions-item label="绑定时间">{{ detailData.bindTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后心跳">{{ detailData.lastHeartbeat || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 封禁确认 -->
    <ConfirmDialog
      v-model="banVisible"
      title="封禁设备确认"
      type="danger"
      :message="`确认封禁设备 ${banRow?.deviceName || banRow?.id}?`"
      sub-message="封禁后该设备所有会话立即下线，无法再绑定卡密"
      confirm-text="确认封禁"
      @confirm="doBan"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { deviceApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailData = ref<any>(null)
const banVisible = ref(false)
const banRow = ref<any>(null)

const filter = reactive({
  current: 1,
  size: 20,
  tenantId: 1,
  softwareId: undefined as number | undefined,
  status: undefined as number | undefined,
  onlineStatus: undefined as number | undefined
})

/**
 * 组合设备状态为 StatusTag 的 device 类型值
 * 0=在线 1=离线 2=封禁
 */
const deviceTagStatus = (row: any): number => {
  if (row.status === 1) return 2
  return row.onlineStatus === 1 ? 0 : 1
}

const osText = (os: string) => {
  return ({
    windows: 'Windows',
    linux: 'Linux',
    macos: 'macOS',
    android: 'Android',
    ios: 'iOS'
  } as Record<string, string>)[os] || os || '-'
}

const osTagType = (os: string) => {
  return ({
    windows: '',
    linux: 'success',
    macos: 'info',
    android: 'warning',
    ios: 'info'
  } as Record<string, string>)[os] || 'info'
}

const maskFingerprint = (fp: string) => {
  if (!fp) return '-'
  if (fp.length <= 16) return fp
  return fp.substring(0, 16) + '****'
}

const loadData = async () => {
  loading.value = true
  try {
    const resp: any = await deviceApi.page(filter)
    tableData.value = resp.records || []
    total.value = resp.total || 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filter.softwareId = undefined
  filter.status = undefined
  filter.onlineStatus = undefined
  filter.current = 1
  loadData()
}

const handleDetail = async (row: any) => {
  try {
    detailData.value = await deviceApi.get(filter.tenantId, row.id)
    detailVisible.value = true
  } catch {
    // 错误已在拦截器处理
  }
}

const handleBan = (row: any) => {
  banRow.value = row
  banVisible.value = true
}

const doBan = async () => {
  if (!banRow.value) return
  try {
    await deviceApi.ban(filter.tenantId, banRow.value.id)
    ElMessage.success('设备已封禁')
    loadData()
  } catch {
    // 错误已在拦截器处理
  }
}

const handleUnban = async (row: any) => {
  try {
    await deviceApi.unban(filter.tenantId, row.id)
    ElMessage.success('设备已解封')
    loadData()
  } catch {
    // 静默
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.fingerprint-text {
  font-family: var(--jicek-font-mono);
  font-size: 12px;
  color: var(--jicek-text-secondary);
}

.fingerprint-full {
  font-family: var(--jicek-font-mono);
  font-size: 12px;
  word-break: break-all;
  color: var(--jicek-text-primary);
}
</style>
