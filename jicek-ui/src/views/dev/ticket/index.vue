<!--
  极策k 工单管理页面（单向工单：开发者→管理员）
  作者: 极策k  日期: 2026-07-22

  v0.6.1 简化：取消终端用户→开发者方向，仅保留开发者→管理员。
  开发者向管理员提交工单（如申请支付通道、解封账号），查看处理进度，补充信息。

  接口：
    POST /api/dev/ticket/submit         新建工单
    GET  /api/dev/ticket/submit/page    分页查询
    GET  /api/dev/ticket/submit/{id}    详情（含回复列表）
    POST /api/dev/ticket/submit/reply   开发者补充回复
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('ticket.title') }}</span>
      </template>

      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="openSubmitDialog">{{ t('ticket.create') }}</el-button>
        <el-button @click="loadList">{{ t('ticket.refresh') }}</el-button>
      </div>

      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('ticket.status')">
          <el-select v-model="filter.status" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option :label="t('ticket.statusPending')" :value="0" />
            <el-option :label="t('ticket.statusProcessing')" :value="1" />
            <el-option :label="t('ticket.statusReplied')" :value="2" />
            <el-option :label="t('ticket.statusClosed')" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ticket.category')">
          <el-select v-model="filter.category" :placeholder="t('common.all')" clearable style="width: 160px">
            <el-option :label="t('ticket.categoryChangeMachine')" :value="1" />
            <el-option :label="t('ticket.categoryRecharge')" :value="2" />
            <el-option :label="t('ticket.categoryCard')" :value="3" />
            <el-option :label="t('ticket.categoryOther')" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="ticketNo" :label="t('ticket.ticketNo')" width="180" />
        <el-table-column prop="title" :label="t('ticket.titleCol')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('ticket.category')" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ categoryText(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('ticket.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="ticketTagType(row.status)" size="small">
              {{ ticketStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('ticket.submitTime')" width="170" />
        <el-table-column :label="t('common.operation')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">{{ t('ticket.detail') }}</el-button>
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

    <!-- ============ 新建工单对话框 ============ -->
    <el-dialog v-model="submitDialogVisible" :title="t('ticket.submitDialogTitle')" width="600px">
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="80px">
        <el-form-item :label="t('ticket.titleLabel')" prop="title">
          <el-input v-model="submitForm.title" maxlength="128" show-word-limit :placeholder="t('ticket.titlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ticket.categoryLabel')" prop="category">
          <el-select v-model="submitForm.category" :placeholder="t('ticket.categoryPlaceholder')" style="width: 100%">
            <el-option :label="t('ticket.categoryChangeMachine')" :value="1" />
            <el-option :label="t('ticket.categoryRecharge')" :value="2" />
            <el-option :label="t('ticket.categoryCard')" :value="3" />
            <el-option :label="t('ticket.categoryOther')" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ticket.contentLabel')" prop="content">
          <el-input
            v-model="submitForm.content"
            type="textarea"
            :rows="6"
            maxlength="4096"
            show-word-limit
            :placeholder="t('ticket.contentPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">{{ t('ticket.cancel') }}</el-button>
        <el-button type="primary" @click="submitTicket" :loading="submitting">{{ t('ticket.submit') }}</el-button>
      </template>
    </el-dialog>

    <!-- ============ 工单详情对话框 ============ -->
    <el-dialog v-model="detailVisible" :title="t('ticket.detailDialogTitle')" width="720px" top="5vh">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border v-if="detail.ticket">
          <el-descriptions-item :label="t('ticket.ticketNo')">{{ detail.ticket.ticketNo }}</el-descriptions-item>
          <el-descriptions-item :label="t('ticket.statusLabel')">
            <el-tag :type="ticketTagType(detail.ticket.status)" size="small">
              {{ ticketStatusText(detail.ticket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('ticket.titleLabel')" :span="2">{{ detail.ticket.title }}</el-descriptions-item>
          <el-descriptions-item :label="t('ticket.categoryLabel2')">{{ categoryText(detail.ticket.category) }}</el-descriptions-item>
          <el-descriptions-item :label="t('ticket.submitTimeLabel')">{{ detail.ticket.createTime }}</el-descriptions-item>
          <el-descriptions-item :label="t('ticket.contentLabel2')" :span="2">
            <div class="ticket-content">{{ detail.ticket.content }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 回复列表 -->
        <div class="reply-section">
          <div class="reply-section-title">{{ t('ticket.replySectionTitle') }}</div>
          <div v-if="detail.replies && detail.replies.length">
            <div
              v-for="reply in detail.replies"
              :key="reply.id"
              :class="['reply-item', reply.replierType === 2 ? 'reply-dev' : 'reply-admin']"
            >
              <div class="reply-header">
                <el-tag size="small" :type="reply.replierType === 2 ? 'success' : 'warning'">
                  {{ replierTypeText(reply.replierType) }}
                </el-tag>
                <span class="reply-name">{{ reply.replierName }}</span>
                <span class="reply-time">{{ reply.createTime }}</span>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
            </div>
          </div>
          <el-empty v-else :description="t('ticket.replyEmpty')" :image-size="60" />
        </div>

        <!-- 补充回复（已关闭工单禁回复） -->
        <div v-if="detail.ticket && detail.ticket.status !== 3" class="reply-input">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            :placeholder="t('ticket.replyInputPlaceholder')"
            maxlength="4096"
            show-word-limit
          />
          <el-button type="primary" @click="submitReply" :loading="replySubmitting" style="margin-top: 8px">
            {{ t('ticket.sendReply') }}
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { ticketApi } from '@/api'

const { t } = useI18n()

// TODO: 鉴权接入后从 token 解析，当前暂用固定值（开发者用户ID=1, 租户ID=1）
const DEV_USER_ID = 1
const TENANT_ID = 1

/* ============ 列表 ============ */
const filter = reactive({
  status: undefined as number | undefined,
  category: undefined as number | undefined,
  current: 1,
  size: 20
})
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await ticketApi.submitPage({
      tenantId: TENANT_ID,
      devUserId: DEV_USER_ID,
      status: filter.status,
      category: filter.category,
      current: filter.current,
      size: filter.size
    })
    list.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

/* ============ 新建工单 ============ */
const submitDialogVisible = ref(false)
const submitFormRef = ref<FormInstance>()
const submitting = ref(false)
const submitForm = reactive({
  title: '',
  content: '',
  category: undefined as number | undefined
})
const submitRules = {
  title: [{ required: true, message: t('ticket.titleRequired'), trigger: 'blur' }],
  category: [{ required: true, message: t('ticket.categoryRequired'), trigger: 'change' }],
  content: [{ required: true, message: t('ticket.contentRequired'), trigger: 'blur' }]
}

function openSubmitDialog() {
  submitForm.title = ''
  submitForm.content = ''
  submitForm.category = undefined
  submitDialogVisible.value = true
}

async function submitTicket() {
  await submitFormRef.value?.validate()
  submitting.value = true
  try {
    await ticketApi.submit(
      {
        tenantId: TENANT_ID,
        title: submitForm.title,
        content: submitForm.content,
        category: submitForm.category,
        creatorName: t('ticket.creatorDev')
      },
      DEV_USER_ID
    )
    ElMessage.success(t('ticket.submitSuccess'))
    submitDialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

/* ============ 详情 + 补充回复 ============ */
const detailVisible = ref(false)
const detail = ref<any>({})
const detailLoading = ref(false)
const replyContent = ref('')
const replySubmitting = ref(false)

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  replyContent.value = ''
  try {
    detail.value = await ticketApi.submitDetail(TENANT_ID, row.id)
  } finally {
    detailLoading.value = false
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning(t('ticket.replyEmptyWarn'))
    return
  }
  replySubmitting.value = true
  try {
    await ticketApi.submitReply(
      { ticketId: detail.value.ticket.id, content: replyContent.value, replierName: t('ticket.creatorDev') },
      TENANT_ID,
      DEV_USER_ID
    )
    ElMessage.success(t('ticket.replySuccess'))
    replyContent.value = ''
    // 重新加载详情
    detail.value = await ticketApi.submitDetail(TENANT_ID, detail.value.ticket.id)
    loadList()
  } finally {
    replySubmitting.value = false
  }
}

/* ============ 工具函数 ============ */
function ticketStatusText(status: number): string {
  const map: Record<number, string> = {
    0: t('ticket.statusPending'),
    1: t('ticket.statusProcessing'),
    2: t('ticket.statusReplied'),
    3: t('ticket.statusClosed')
  }
  return map[status] || t('ticket.statusUnknown')
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
    1: t('ticket.categoryChangeMachine'),
    2: t('ticket.categoryRecharge'),
    3: t('ticket.categoryCard'),
    4: t('ticket.categoryOther')
  }
  return map[category] || t('ticket.statusUnknown')
}

function replierTypeText(type: number): string {
  const map: Record<number, string> = {
    2: t('ticket.replierDev'),
    3: t('ticket.replierAdmin')
  }
  return map[type] || t('ticket.replierUnknown')
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
.ticket-content {
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}
</style>
