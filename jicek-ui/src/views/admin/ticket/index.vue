<!--
  极策k 管理员工单管理页面
  作者: 极策k  日期: 2026-07-22

  v0.15.0 管理员后台：
   - 管理员可查看全部租户工单，支持 tenantId/category/status 筛选
   - 详情弹窗含回复列表 + 管理员回复输入框 + 关闭工单按钮
   - 管理员回复 replierType=3（后端固定），状态→已回复
   - 关闭工单二次确认

  接口：
    GET  /api/admin/ticket/page          分页查询
    GET  /api/admin/ticket/{id}          详情（含回复列表）
    POST /api/admin/ticket/{id}/reply    管理员回复
    POST /api/admin/ticket/{id}/close    关闭工单
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('admin.ticket.title') }}</span>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('admin.ticket.tenantId')">
          <el-input
            v-model.number="filter.tenantId"
            :placeholder="t('common.optional')"
            type="number"
            clearable
            style="width: 140px"
            @keyup.enter="loadList"
          />
        </el-form-item>
        <el-form-item :label="t('admin.ticket.category')">
          <el-select v-model="filter.category" :placeholder="t('common.all')" clearable style="width: 150px" @change="loadList">
            <el-option :label="t('admin.ticket.categoryChangeDevice')" :value="1" />
            <el-option :label="t('admin.ticket.categoryRecharge')" :value="2" />
            <el-option :label="t('admin.ticket.categoryCard')" :value="3" />
            <el-option :label="t('admin.ticket.categoryOther')" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('admin.ticket.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 130px" @change="loadList">
            <el-option :label="t('admin.ticket.statusPending')" :value="0" />
            <el-option :label="t('admin.ticket.statusProcessing')" :value="1" />
            <el-option :label="t('admin.ticket.statusReplied')" :value="2" />
            <el-option :label="t('admin.ticket.statusClosed')" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="ticketNo" :label="t('admin.ticket.ticketNo')" width="180" />
        <el-table-column prop="title" :label="t('admin.ticket.titleCol')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('admin.ticket.category')" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ categoryText(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.ticket.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="ticketTagType(row.status)" size="small">
              {{ ticketStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" :label="t('admin.ticket.creator')" width="120" show-overflow-tooltip />
        <el-table-column prop="tenantId" :label="t('admin.ticket.tenantId')" width="90" />
        <el-table-column prop="createTime" :label="t('admin.ticket.createTime')" width="170" />
        <el-table-column prop="updateTime" :label="t('admin.ticket.lastUpdate')" width="170" />
        <el-table-column :label="t('admin.ticket.operation')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">{{ t('admin.ticket.detail') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="filter.current"
        v-model:page-size="filter.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="loadList"
        @current-change="loadList"
      />
    </el-card>

    <!-- ============ 工单详情对话框 ============ -->
    <el-dialog v-model="detailVisible" :title="t('admin.ticket.detail')" width="720px" top="5vh">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border v-if="detail.ticket">
          <el-descriptions-item :label="t('admin.ticket.ticketNo')">{{ detail.ticket.ticketNo }}</el-descriptions-item>
          <el-descriptions-item :label="t('admin.ticket.status')">
            <el-tag :type="ticketTagType(detail.ticket.status)" size="small">
              {{ ticketStatusText(detail.ticket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('admin.ticket.titleCol')" :span="2">{{ detail.ticket.title }}</el-descriptions-item>
          <el-descriptions-item :label="t('admin.ticket.category')">{{ categoryText(detail.ticket.category) }}</el-descriptions-item>
          <el-descriptions-item :label="t('admin.ticket.tenantId')">{{ detail.ticket.tenantId }}</el-descriptions-item>
          <el-descriptions-item :label="t('admin.ticket.creator')">{{ detail.ticket.creatorName }}</el-descriptions-item>
          <el-descriptions-item :label="t('admin.ticket.createTime')">{{ detail.ticket.createTime }}</el-descriptions-item>
          <el-descriptions-item :label="t('admin.ticket.content')" :span="2">
            <div class="ticket-content">{{ detail.ticket.content }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 回复列表 -->
        <div class="reply-section">
          <div class="reply-section-title">{{ t('admin.ticket.replyRecord') }}</div>
          <div v-if="detail.replies && detail.replies.length">
            <div
              v-for="reply in detail.replies"
              :key="reply.id"
              :class="['reply-item', reply.replierType === 3 ? 'reply-admin' : 'reply-dev']"
            >
              <div class="reply-header">
                <el-tag size="small" :type="reply.replierType === 3 ? 'warning' : 'success'">
                  {{ replierTypeText(reply.replierType) }}
                </el-tag>
                <span class="reply-name">{{ reply.replierName }}</span>
                <span class="reply-time">{{ reply.createTime }}</span>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
            </div>
          </div>
          <el-empty v-else :description="t('admin.ticket.noReply')" :image-size="60" />
        </div>

        <!-- 管理员回复（已关闭工单禁回复） -->
        <div v-if="detail.ticket && detail.ticket.status !== 3" class="reply-input">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            :placeholder="t('admin.ticket.replyPlaceholder')"
            maxlength="4096"
            show-word-limit
          />
          <div class="reply-actions">
            <el-button type="danger" plain @click="handleClose(detail.ticket.id)">
              {{ t('admin.ticket.close') }}
            </el-button>
            <el-button type="primary" :loading="replySubmitting" @click="submitReply">
              {{ t('admin.ticket.reply') }}
            </el-button>
          </div>
        </div>
        <div v-else class="closed-tip">
          {{ t('admin.ticket.closedTip') }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const { t } = useI18n()

/* ============ 列表 ============ */
const filter = reactive({
  tenantId: undefined as number | undefined,
  category: undefined as number | undefined,
  status: undefined as number | undefined,
  current: 1,
  size: 20
})
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res: any = await adminApi.ticketPage({
      tenantId: filter.tenantId || undefined,
      category: filter.category,
      status: filter.status,
      current: filter.current,
      size: filter.size
    })
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filter.tenantId = undefined
  filter.category = undefined
  filter.status = undefined
  filter.current = 1
  loadList()
}

/* ============ 详情 + 回复 + 关闭 ============ */
const detailVisible = ref(false)
const detail = ref<any>({})
const detailLoading = ref(false)
const replyContent = ref('')
const replySubmitting = ref(false)

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  replyContent.value = ''
  detail.value = {}
  try {
    detail.value = await adminApi.ticketGet(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function reloadDetail(id: number) {
  detail.value = await adminApi.ticketGet(id)
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning(t('admin.ticket.replyEmpty'))
    return
  }
  const ticketId = detail.value.ticket.id
  replySubmitting.value = true
  try {
    await adminApi.ticketReply(ticketId, replyContent.value)
    ElMessage.success(t('admin.ticket.replySuccess'))
    replyContent.value = ''
    await reloadDetail(ticketId)
    loadList()
  } finally {
    replySubmitting.value = false
  }
}

async function handleClose(id: number) {
  try {
    await ElMessageBox.confirm(t('admin.ticket.closeConfirm'), t('admin.ticket.close'), {
      confirmButtonText: t('admin.ticket.close'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await adminApi.ticketClose(id)
    ElMessage.success(t('admin.ticket.closeSuccess'))
    await reloadDetail(id)
    loadList()
  } catch {
    // 拦截器已提示
  }
}

/* ============ 工具函数 ============ */
function ticketStatusText(status: number): string {
  const map: Record<number, string> = {
    0: t('admin.ticket.statusPending'),
    1: t('admin.ticket.statusProcessing'),
    2: t('admin.ticket.statusReplied'),
    3: t('admin.ticket.statusClosed')
  }
  return map[status] || ''
}

function ticketTagType(status: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'warning',
    1: '',
    2: 'success',
    3: 'info'
  }
  return map[status] || ''
}

function categoryText(category: number): string {
  const map: Record<number, string> = {
    1: t('admin.ticket.categoryChangeDevice'),
    2: t('admin.ticket.categoryRecharge'),
    3: t('admin.ticket.categoryCard'),
    4: t('admin.ticket.categoryOther')
  }
  return map[category] || ''
}

function replierTypeText(type: number): string {
  const map: Record<number, string> = {
    2: t('admin.ticket.replierDev'),
    3: t('admin.ticket.replierAdmin')
  }
  return map[type] || ''
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.reply-section {
  margin-top: 24px;
  border-top: 1px solid var(--el-border-color-light);
  padding-top: 16px;
}
.reply-section-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
}
.reply-item {
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-lighter);
}
.reply-dev {
  background: var(--el-color-success-light-9);
}
.reply-admin {
  background: var(--el-color-warning-light-9);
}
.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}
.reply-name {
  font-weight: 600;
}
.reply-time {
  color: var(--el-text-color-secondary);
  margin-left: auto;
}
.reply-content {
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}
.reply-input {
  margin-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
  padding-top: 16px;
}
.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.closed-tip {
  margin-top: 16px;
  padding: 12px;
  text-align: center;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border-radius: 6px;
}
.ticket-content {
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}
</style>
