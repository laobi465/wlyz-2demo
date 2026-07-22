<!--
  极策k 卡密查询页面
  作者: 极策k  日期: 2026-07-21

  功能：按卡号查询卡密详情，封禁/退款操作（带二次确认）
  接口：GET /api/dev/card/query, POST /api/dev/card/ban, POST /api/dev/card/refund
  安全：查询结果中卡密明文已脱敏
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('cardKey.listTitle') }}</span>
      </template>

      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('cardKey.cardNo')">
          <el-input v-model="filter.cardNo" :placeholder="t('cardKey.cardNoPlaceholder')" clearable style="width: 320px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleQuery">{{ t('cardKey.query') }}</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="cardInfo" :column="2" border>
        <el-descriptions-item :label="t('cardKey.cardNo')">{{ cardInfo.cardNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('cardKey.status')">
          <StatusTag :status="cardInfo.status" type="card" />
        </el-descriptions-item>
        <el-descriptions-item :label="t('cardKey.cardTypeId')">{{ cardInfo.cardTypeId }}</el-descriptions-item>
        <el-descriptions-item :label="t('cardKey.softwareIdLabel')">{{ cardInfo.softwareId }}</el-descriptions-item>
        <el-descriptions-item :label="t('cardKey.firstUseTime')">{{ cardInfo.firstUseTime || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('cardKey.expireTime')">{{ cardInfo.expireTime || t('cardKey.expirePermanent') }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.createTime')">{{ cardInfo.createTime }}</el-descriptions-item>
        <el-descriptions-item :label="t('common.updateTime')">{{ cardInfo.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <el-empty v-else-if="!loading" :description="t('cardKey.queryEmpty')" />

      <div v-if="cardInfo" style="margin-top: 16px">
        <el-button
          v-if="cardInfo.status === 0 || cardInfo.status === 1"
          type="danger"
          @click="handleBan"
        >
          {{ t('cardKey.banCard') }}
        </el-button>
        <el-button
          v-if="cardInfo.status === 0 || cardInfo.status === 1"
          type="warning"
          @click="handleRefund"
        >
          {{ t('cardKey.refundCard') }}
        </el-button>
      </div>
    </el-card>

    <ConfirmDialog
      v-model="banVisible"
      :title="t('cardKey.banTitle')"
      type="danger"
      :message="t('cardKey.banMessage', { cardNo: cardInfo?.cardNo })"
      :sub-message="t('cardKey.banSubMessage')"
      :confirm-text="t('cardKey.banConfirm')"
      @confirm="doBan"
    />

    <ConfirmDialog
      v-model="refundVisible"
      :title="t('cardKey.refundTitle')"
      type="warning"
      :message="t('cardKey.refundMessage', { cardNo: cardInfo?.cardNo })"
      :sub-message="t('cardKey.refundSubMessage')"
      :confirm-text="t('cardKey.refundConfirm')"
      @confirm="doRefund"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { cardKeyApi } from '@/api'
import StatusTag from '@/components/jicek/StatusTag.vue'
import ConfirmDialog from '@/components/jicek/ConfirmDialog.vue'

const { t } = useI18n()

const loading = ref(false)
const cardInfo = ref<any>(null)
const banVisible = ref(false)
const refundVisible = ref(false)

const filter = reactive({
  tenantId: 1,
  cardNo: ''
})

const handleQuery = async () => {
  if (!filter.cardNo) {
    ElMessage.warning(t('cardKey.cardNoRequired'))
    return
  }
  loading.value = true
  try {
    const resp: any = await cardKeyApi.query(filter.tenantId, filter.cardNo)
    cardInfo.value = resp.card
  } finally {
    loading.value = false
  }
}

const handleBan = () => {
  banVisible.value = true
}

const doBan = async () => {
  if (!cardInfo.value) return
  try {
    await cardKeyApi.ban(filter.tenantId, cardInfo.value.id, '管理员手动封禁')
    ElMessage.success(t('cardKey.banSuccess'))
    handleQuery()
  } catch {}
}

const handleRefund = () => {
  refundVisible.value = true
}

const doRefund = async () => {
  if (!cardInfo.value) return
  try {
    await cardKeyApi.refund(filter.tenantId, cardInfo.value.id)
    ElMessage.success(t('cardKey.refundSuccess'))
    handleQuery()
  } catch {}
}
</script>
