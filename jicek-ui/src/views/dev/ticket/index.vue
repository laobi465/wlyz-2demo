<!--
  极策k 工单管理页面（双向工单双角色）
  作者: 极策k  日期: 2026-07-22

  对应 docs/UI-DESIGN.md 6.2 节「工单管理」+ 6.4 节「工单（联系开发者）」：
    开发者在工单系统中承担双重角色：
      1. [处理者] 处理终端用户提交的工单（target=1开发者）
      2. [提交者] 向管理员提交工单（target=2管理员）

  双 Tab 设计：
    - 收件箱（receive）：终端用户提交的工单列表 + 详情/回复/关闭
    - 已提交（submit）：开发者向管理员提交的工单列表 + 新建工单

  接口：
    GET  /api/dev/ticket/receive/page   收件箱分页
    GET  /api/dev/ticket/receive/{id}   收件箱详情
    POST /api/dev/ticket/receive/reply  回复
    POST /api/dev/ticket/receive/close  关闭
    POST /api/dev/ticket/submit         新建提交工单
    GET  /api/dev/ticket/submit/page    提交工单分页
    GET  /api/dev/ticket/submit/{id}    提交工单详情
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">工单管理</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- ============ Tab 1: 收件箱（处理终端用户工单） ============ -->
        <el-tab-pane label="收件箱（终端用户工单）" name="receive">
          <el-form :inline="true" :model="receiveFilter" style="margin-bottom: 16px">
            <el-form-item label="状态">
              <el-select v-model="receiveFilter.status" placeholder="全部" clearable style="width: 140px">
                <el-option label="待处理" :value="0" />
                <el-option label="处理中" :value="1" />
                <el-option label="已回复" :value="2" />
                <el-option label="已关闭" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="receiveFilter.category" placeholder="全部" clearable style="width: 160px">
                <el-option label="换机申请" :value="1" />
                <el-option label="充值问题" :value="2" />
                <el-option label="卡密问题" :value="3" />
                <el-option label="其他" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadReceive">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="receiveList" v-loading="receiveLoading" border stripe>
            <el-table-column prop="ticketNo" label="工单号" width="180" />
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column label="分类" width="110">
              <template #default="{ row }">
                <el-tag size="small">{{ categoryText(row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="ticketTagType(row.status)" size="small">
                  {{ ticketStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="creatorName" label="提交人" width="120" show-overflow-tooltip />
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openReceiveDetail(row)">详情</el-button>
                <el-button
                  v-if="row.status !== 3"
                  size="small"
                  type="danger"
                  @click="closeTicket(row)"
                >关闭</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="receiveFilter.current"
            v-model:page-size="receiveFilter.size"
            :total="receiveTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="loadReceive"
            @current-change="loadReceive"
          />
        </el-tab-pane>

        <!-- ============ Tab 2: 已提交（向管理员提单） ============ -->
        <el-tab-pane label="已提交（向管理员工单）" name="submit">
          <div style="margin-bottom: 16px">
            <el-button type="primary" @click="openSubmitDialog">新建工单</el-button>
            <el-button @click="loadSubmit">刷新</el-button>
          </div>

          <el-table :data="submitList" v-loading="submitLoading" border stripe>
            <el-table-column prop="ticketNo" label="工单号" width="180" />
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column label="分类" width="110">
              <template #default="{ row }">
                <el-tag size="small">{{ categoryText(row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="ticketTagType(row.status)" size="small">
                  {{ ticketStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openSubmitDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="submitFilter.current"
            v-model:page-size="submitFilter.size"
            :total="submitTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @size-change="loadSubmit"
            @current-change="loadSubmit"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ============ 工单详情对话框（收件箱） ============ -->
    <el-dialog v-model="receiveDetailVisible" title="工单详情" width="720px" top="5vh">
      <div v-loading="detailLoading">
        <el-descriptions :column="2" border v-if="receiveDetail.ticket">
          <el-descriptions-item label="工单号">{{ receiveDetail.ticket.ticketNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="ticketTagType(receiveDetail.ticket.status)" size="small">
              {{ ticketStatusText(receiveDetail.ticket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ receiveDetail.ticket.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ categoryText(receiveDetail.ticket.category) }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ receiveDetail.ticket.creatorName }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            <div class="ticket-content">{{ receiveDetail.ticket.content }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 回复列表 -->
        <div class="reply-section">
          <div class="reply-section-title">对话记录</div>
          <div v-if="receiveDetail.replies && receiveDetail.replies.length">
            <div
              v-for="reply in receiveDetail.replies"
              :key="reply.id"
              :class="['reply-item', reply.replierType === 1 ? 'reply-user' : 'reply-dev']"
            >
              <div class="reply-header">
                <el-tag size="small" :type="reply.replierType === 1 ? 'info' : 'success'">
                  {{ replierTypeText(reply.replierType) }}
                </el-tag>
                <span class="reply-name">{{ reply.replierName }}</span>
                <span class="reply-time">{{ reply.createTime }}</span>
              </div>
              <div class="reply-content">{{ reply.content }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无回复" :image-size="60" />
        </div>

        <!-- 回复输入（已关闭工单禁回复） -->
        <div v-if="receiveDetail.ticket && receiveDetail.ticket.status !== 3" class="reply-input">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            placeholder="输入回复内容..."
            maxlength="4096"
            show-word-limit
          />
          <el-button type="primary" @click="submitReply" :loading="replySubmitting" style="margin-top: 8px">
            发送回复
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- ============ 新建工单对话框（向管理员提交） ============ -->
    <el-dialog v-model="submitDialogVisible" title="向管理员提交工单" width="600px">
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="submitForm.title" maxlength="128" show-word-limit placeholder="简要描述问题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="submitForm.category" placeholder="选择分类" style="width: 100%">
            <el-option label="换机申请" :value="1" />
            <el-option label="充值问题" :value="2" />
            <el-option label="卡密问题" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" prop="content">
          <el-input
            v-model="submitForm.content"
            type="textarea"
            :rows="6"
            maxlength="4096"
            show-word-limit
            placeholder="详细描述问题，便于管理员处理"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTicket" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- ============ 提交工单详情对话框 ============ -->
    <el-dialog v-model="submitDetailVisible" title="工单详情" width="720px" top="5vh">
      <div v-loading="submitDetailLoading">
        <el-descriptions :column="2" border v-if="submitDetail.ticket">
          <el-descriptions-item label="工单号">{{ submitDetail.ticket.ticketNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="ticketTagType(submitDetail.ticket.status)" size="small">
              {{ ticketStatusText(submitDetail.ticket.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ submitDetail.ticket.title }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ categoryText(submitDetail.ticket.category) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ submitDetail.ticket.createTime }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">
            <div class="ticket-content">{{ submitDetail.ticket.content }}</div>
          </el-descriptions-item>
        </el-descriptions>

        <div class="reply-section">
          <div class="reply-section-title">对话记录</div>
          <div v-if="submitDetail.replies && submitDetail.replies.length">
            <div
              v-for="reply in submitDetail.replies"
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
          <el-empty v-else description="暂无回复" :image-size="60" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { ticketApi } from '@/api'

// TODO: 鉴权接入后从 token 解析，当前暂用固定值（开发者用户ID=1, 租户ID=1）
const DEV_USER_ID = 1
const TENANT_ID = 1

const activeTab = ref('receive')

/* ============ 收件箱（处理终端用户工单） ============ */
const receiveFilter = reactive({
  status: undefined as number | undefined,
  category: undefined as number | undefined,
  current: 1,
  size: 20
})
const receiveList = ref<any[]>([])
const receiveTotal = ref(0)
const receiveLoading = ref(false)

async function loadReceive() {
  receiveLoading.value = true
  try {
    const res = await ticketApi.receivePage({
      tenantId: TENANT_ID,
      status: receiveFilter.status,
      category: receiveFilter.category,
      current: receiveFilter.current,
      size: receiveFilter.size
    })
    receiveList.value = res.records || []
    receiveTotal.value = res.total || 0
  } finally {
    receiveLoading.value = false
  }
}

// 收件箱详情
const receiveDetailVisible = ref(false)
const receiveDetail = ref<any>({})
const detailLoading = ref(false)
const replyContent = ref('')
const replySubmitting = ref(false)

async function openReceiveDetail(row: any) {
  receiveDetailVisible.value = true
  detailLoading.value = true
  replyContent.value = ''
  try {
    receiveDetail.value = await ticketApi.receiveDetail(TENANT_ID, row.id)
  } finally {
    detailLoading.value = false
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  replySubmitting.value = true
  try {
    await ticketApi.receiveReply(
      { ticketId: receiveDetail.value.ticket.id, content: replyContent.value, replierName: '开发者' },
      TENANT_ID,
      DEV_USER_ID
    )
    ElMessage.success('回复成功')
    replyContent.value = ''
    // 重新加载详情
    receiveDetail.value = await ticketApi.receiveDetail(TENANT_ID, receiveDetail.value.ticket.id)
    // 刷新列表状态
    loadReceive()
  } finally {
    replySubmitting.value = false
  }
}

async function closeTicket(row: any) {
  await ElMessageBox.confirm('确认关闭该工单？关闭后用户将无法继续回复。', '关闭工单', {
    type: 'warning'
  })
  await ticketApi.receiveClose(TENANT_ID, row.id, DEV_USER_ID)
  ElMessage.success('工单已关闭')
  loadReceive()
}

/* ============ 已提交（向管理员提单） ============ */
const submitFilter = reactive({
  current: 1,
  size: 20,
  status: undefined as number | undefined,
  category: undefined as number | undefined
})
const submitList = ref<any[]>([])
const submitTotal = ref(0)
const submitLoading = ref(false)

async function loadSubmit() {
  submitLoading.value = true
  try {
    const res = await ticketApi.submitPage({
      tenantId: TENANT_ID,
      devUserId: DEV_USER_ID,
      status: submitFilter.status,
      category: submitFilter.category,
      current: submitFilter.current,
      size: submitFilter.size
    })
    submitList.value = res.records || []
    submitTotal.value = res.total || 0
  } finally {
    submitLoading.value = false
  }
}

// 新建工单
const submitDialogVisible = ref(false)
const submitFormRef = ref<FormInstance>()
const submitting = ref(false)
const submitForm = reactive({
  title: '',
  content: '',
  category: undefined as number | undefined
})
const submitRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入问题描述', trigger: 'blur' }]
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
        creatorName: '开发者'
      },
      DEV_USER_ID
    )
    ElMessage.success('工单已提交')
    submitDialogVisible.value = false
    loadSubmit()
  } finally {
    submitting.value = false
  }
}

// 提交工单详情
const submitDetailVisible = ref(false)
const submitDetail = ref<any>({})
const submitDetailLoading = ref(false)

async function openSubmitDetail(row: any) {
  submitDetailVisible.value = true
  submitDetailLoading.value = true
  try {
    submitDetail.value = await ticketApi.submitDetail(TENANT_ID, row.id)
  } finally {
    submitDetailLoading.value = false
  }
}

/* ============ 工具函数 ============ */
function ticketStatusText(status: number): string {
  const map: Record<number, string> = { 0: '待处理', 1: '处理中', 2: '已回复', 3: '已关闭' }
  return map[status] || '未知'
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
  const map: Record<number, string> = { 1: '换机申请', 2: '充值问题', 3: '卡密问题', 4: '其他' }
  return map[category] || '未知'
}

function replierTypeText(type: number): string {
  const map: Record<number, string> = { 1: '用户', 2: '开发者', 3: '管理员' }
  return map[type] || '未知'
}

onMounted(() => {
  loadReceive()
  loadSubmit()
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
.reply-user {
  background: var(--el-fill-color-light);
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
