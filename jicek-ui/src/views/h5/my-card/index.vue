<!--
  极策k网络验证 - H5 我的卡密
  作者: 极策k  日期: 2026-07-22

  v0.13.0 功能1：H5 终端用户卡密详情
   - 调 h5Api.myCard() 加载卡密信息（X-H5-Token 鉴权）
   - 卡类信息按 cardType 渲染：1 时长卡 / 2 次数卡 / 3 功能卡 / 4 永久卡
   - 卡密状态 0-4 映射色彩（未使用/已使用/已封禁/已退款/已过期）
   - 无 token / token 失效跳 /h5/login
   - 遵循 docs/UI-DESIGN.md（无渐变、无 emoji）
-->
<template>
  <div class="h5-my-card">
    <!-- 骨架屏 -->
    <div v-if="loading" class="h5-skeleton-list">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="cardInfo">
      <!-- 卡密主信息卡片 -->
      <div class="h5-card-box">
        <div class="h5-card-top">
          <div class="h5-card-type">{{ cardInfo.cardTypeName }}</div>
          <span class="jicek-tag" :class="cardStatusClass">{{ cardStatusText }}</span>
        </div>
        <div class="h5-card-software">{{ cardInfo.softwareName }}</div>
        <div class="h5-card-no">
          <span class="h5-card-no-label">卡号：</span>
          <span class="h5-card-no-value">{{ cardInfo.cardNoMasked }}</span>
        </div>
      </div>

      <!-- 卡类信息 -->
      <div class="h5-info-box">
        <div class="h5-info-title">卡类信息</div>

        <!-- 时长卡：到期时间 + 剩余天数 -->
        <template v-if="cardInfo.cardType === 1">
          <div class="h5-info-row">
            <span class="h5-info-label">到期时间</span>
            <span class="h5-info-value">{{ cardInfo.expireTime || '-' }}</span>
          </div>
          <div class="h5-info-row">
            <span class="h5-info-label">剩余天数</span>
            <span class="h5-info-value" :class="{ expired: isExpired }">
              {{ remainingDaysText }}
            </span>
          </div>
        </template>

        <!-- 次数卡：剩余次数 -->
        <template v-else-if="cardInfo.cardType === 2">
          <div class="h5-info-row">
            <span class="h5-info-label">剩余次数</span>
            <span class="h5-info-value h5-info-value-strong">
              {{ cardInfo.remainingCount }}
            </span>
          </div>
        </template>

        <!-- 功能卡：功能列表 -->
        <template v-else-if="cardInfo.cardType === 3">
          <div class="h5-info-row h5-info-row-block">
            <span class="h5-info-label">功能列表</span>
            <div class="h5-feature-tags">
              <el-tag
                v-for="(feat, idx) in featureList"
                :key="idx"
                size="small"
                type="info"
                effect="plain"
              >
                {{ feat }}
              </el-tag>
              <span v-if="!featureList.length" class="h5-info-empty">无</span>
            </div>
          </div>
        </template>

        <!-- 永久卡 -->
        <template v-else-if="cardInfo.cardType === 4">
          <div class="h5-info-row">
            <span class="h5-info-label">有效期</span>
            <span class="h5-info-value h5-info-value-strong h5-info-value-success">
              永久有效
            </span>
          </div>
        </template>

        <div v-if="cardInfo.firstUseTime" class="h5-info-row">
          <span class="h5-info-label">首次使用</span>
          <span class="h5-info-value">{{ cardInfo.firstUseTime }}</span>
        </div>
      </div>

      <!-- 卡密操作提示 -->
      <div class="h5-tip">
        如卡密异常或无法使用，请联系开发者或所属代理处理。
      </div>
    </template>

    <el-empty v-else description="暂无卡密信息" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { h5Api, H5_TOKEN_KEY } from '@/api'

interface MyCardInfo {
  cardKeyId: number
  cardNoMasked: string
  cardStatus: number
  cardTypeName: string
  cardType: number
  expireTime?: string
  firstUseTime?: string
  remainingCount?: number
  features?: string
  softwareName: string
}

const router = useRouter()
const loading = ref(true)
const cardInfo = ref<MyCardInfo | null>(null)

const cardStatusText = computed(() => {
  const map: Record<number, string> = {
    0: '未使用',
    1: '已使用',
    2: '已封禁',
    3: '已退款',
    4: '已过期'
  }
  return map[cardInfo.value?.cardStatus ?? -1] || '未知'
})

const cardStatusClass = computed(() => {
  const map: Record<number, string> = {
    0: 'jicek-tag-pending',
    1: 'jicek-tag-success',
    2: 'jicek-tag-danger',
    3: 'jicek-tag-warning',
    4: 'jicek-tag-pending'
  }
  return map[cardInfo.value?.cardStatus ?? -1] || 'jicek-tag-pending'
})

const featureList = computed(() => {
  const feats = cardInfo.value?.features || ''
  if (!feats) return []
  return feats
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
})

const isExpired = computed(() => {
  if (!cardInfo.value?.expireTime) return false
  return new Date(cardInfo.value.expireTime).getTime() < Date.now()
})

const remainingDaysText = computed(() => {
  if (!cardInfo.value?.expireTime) return '-'
  const diff = new Date(cardInfo.value.expireTime).getTime() - Date.now()
  if (diff <= 0) return '已过期'
  const days = Math.ceil(diff / (24 * 60 * 60 * 1000))
  return `${days} 天`
})

async function loadCard() {
  if (!localStorage.getItem(H5_TOKEN_KEY)) {
    router.replace({ path: '/h5/login', query: { redirect: '/h5/my-card' } })
    return
  }
  loading.value = true
  try {
    const result: any = await h5Api.myCard()
    cardInfo.value = result as MyCardInfo
  } catch (e: any) {
    // 鉴权失败跳登录
    const msg = e?.message || ''
    if (msg.includes('登录') || msg.includes('token') || msg.includes('过期')) {
      localStorage.removeItem(H5_TOKEN_KEY)
      router.replace({ path: '/h5/login', query: { redirect: '/h5/my-card' } })
      return
    }
    ElMessage.error('加载卡密信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadCard)
</script>

<style scoped lang="scss">
.h5-my-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.h5-skeleton-list {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 20px 16px;
}

.h5-card-box {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 20px 16px;
  box-shadow: var(--jicek-shadow-sm);

  .h5-card-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .h5-card-type {
    font-size: 18px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 28px;
  }

  .h5-card-software {
    font-size: 14px;
    color: var(--jicek-text-secondary);
    line-height: 22px;
    margin-bottom: 12px;
  }

  .h5-card-no {
    font-size: 13px;
    color: var(--jicek-text-secondary);
    line-height: 20px;

    .h5-card-no-label {
      margin-right: 4px;
    }

    .h5-card-no-value {
      color: var(--jicek-text-primary);
      font-family: var(--jicek-font-mono);
    }
  }
}

.h5-info-box {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 16px;
  box-shadow: var(--jicek-shadow-sm);

  .h5-info-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 22px;
    padding-bottom: 8px;
    margin-bottom: 8px;
    border-bottom: 1px solid var(--jicek-border);
  }

  .h5-info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
    font-size: 14px;

    &.h5-info-row-block {
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
    }
  }

  .h5-info-label {
    color: var(--jicek-text-secondary);
    line-height: 22px;
  }

  .h5-info-value {
    color: var(--jicek-text-primary);
    line-height: 22px;

    &.h5-info-value-strong {
      font-weight: 600;
    }

    &.h5-info-value-success {
      color: var(--jicek-success);
    }

    &.expired {
      color: var(--jicek-danger);
    }
  }

  .h5-info-empty {
    color: var(--jicek-text-placeholder);
    font-size: 13px;
  }
}

.h5-feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.h5-tip {
  font-size: 12px;
  color: var(--jicek-text-placeholder);
  line-height: 18px;
  text-align: center;
  padding: 4px 8px;
}
</style>
