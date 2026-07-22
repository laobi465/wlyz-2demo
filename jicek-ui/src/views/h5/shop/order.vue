<!--
  极策k网络验证 - H5 确认订单
  作者: 极策k  日期: 2026-07-22

  v0.13.0 功能3：H5 购卡-确认订单
   - 路由 query 读取 productId
   - 从 sessionStorage 读取商品信息（key: jicek_h5_pending_order）
   - 数量选择器（min 1 max 99）+ 支付方式 radio
   - 金额：单价 × 数量 = 总价（toFixed(2)）
   - 调 h5Api.createOrder({ shopProductId, quantity, payType })
   - 成功：跳 payUrl 或 ElNotification 提示订单号
   - 未登录跳 /h5/login
   - 遵循 docs/UI-DESIGN.md（无渐变、无 emoji）
-->
<template>
  <div class="h5-order">
    <template v-if="product">
      <!-- 商品信息 -->
      <div class="h5-order-section">
        <div class="h5-section-title">商品信息</div>
        <div class="h5-order-product">
          <div class="h5-order-product-top">
            <div class="h5-order-product-name">{{ product.cardTypeName }}</div>
            <span class="jicek-tag" :class="cardTypeTagClass(product.cardType)">
              {{ cardTypeText(product.cardType) }}
            </span>
          </div>
          <div v-if="product.duration" class="h5-order-meta">时长：{{ product.duration }}</div>
          <div v-if="product.count" class="h5-order-meta">次数：{{ product.count }}</div>
          <div v-if="product.features" class="h5-order-meta">功能：{{ product.features }}</div>
          <div class="h5-order-price-row">
            <span class="h5-order-price-label">单价</span>
            <span class="h5-order-price">¥{{ formatPrice(product.price) }}</span>
          </div>
        </div>
      </div>

      <!-- 数量选择 -->
      <div class="h5-order-section">
        <div class="h5-section-title">购买数量</div>
        <div class="h5-order-quantity">
          <el-input-number v-model="quantity" :min="1" :max="99" :step="1" />
        </div>
      </div>

      <!-- 支付方式 -->
      <div class="h5-order-section">
        <div class="h5-section-title">支付方式</div>
        <el-radio-group v-model="payType" class="h5-pay-group">
          <el-radio
            v-for="opt in payOptions"
            :key="opt.value"
            :value="opt.value"
            class="h5-pay-radio"
          >
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </div>

      <!-- 金额合计 -->
      <div class="h5-order-section">
        <div class="h5-section-title">金额合计</div>
        <div class="h5-order-total">
          <span class="h5-order-total-label">应付金额</span>
          <span class="h5-order-total-value">¥{{ totalAmount }}</span>
        </div>
      </div>

      <!-- 提交 -->
      <div class="h5-order-submit">
        <el-button
          type="primary"
          class="h5-submit-btn"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          确认订单
        </el-button>
      </div>
    </template>

    <div v-else class="h5-empty">
      <el-empty description="未找到商品信息，请返回店铺重新选择">
        <el-button type="primary" @click="goShop">返回店铺</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElNotification } from 'element-plus'
import { h5Api, H5_TOKEN_KEY } from '@/api'

interface ShopProduct {
  id: number
  shopId: number
  cardTypeId: number
  price: number
  sortOrder: number
  status: number
  cardTypeName: string
  cardType: number
  duration?: string
  count?: number
  features?: string
}

const route = useRoute()
const router = useRouter()

const PENDING_ORDER_KEY = 'jicek_h5_pending_order'

const product = ref<ShopProduct | null>(null)
const quantity = ref(1)
const payType = ref('alipay')
const submitLoading = ref(false)

const payOptions = [
  { value: 'alipay', label: '支付宝' },
  { value: 'wechat', label: '微信支付' },
  { value: 'qq', label: 'QQ 钱包' },
  { value: 'unionpay', label: '银联云闪付' }
]

function cardTypeText(t: number): string {
  const map: Record<number, string> = {
    1: '时长卡',
    2: '次数卡',
    3: '功能卡',
    4: '永久卡'
  }
  return map[t] || '未知'
}

function cardTypeTagClass(t: number): string {
  const map: Record<number, string> = {
    1: 'jicek-tag-info',
    2: 'jicek-tag-success',
    3: 'jicek-tag-warning',
    4: 'jicek-tag-info'
  }
  return map[t] || 'jicek-tag-pending'
}

function formatPrice(price: number): string {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const totalAmount = computed(() => {
  if (!product.value) return '0.00'
  return (Number(product.value.price) * quantity.value).toFixed(2)
})

function loadProduct() {
  // 校验登录态
  if (!localStorage.getItem(H5_TOKEN_KEY)) {
    router.replace({ path: '/h5/login', query: { redirect: '/h5/shop' } })
    return
  }
  // 从 sessionStorage 读取
  const raw = sessionStorage.getItem(PENDING_ORDER_KEY)
  if (!raw) {
    return
  }
  try {
    const obj = JSON.parse(raw) as ShopProduct
    // 校验 productId 一致
    const productId = Number(route.query.productId)
    if (productId && obj.id !== productId) {
      // 不一致，以 sessionStorage 为准（提示用户）
      ElMessage.warning('商品信息不匹配，请重新选择')
      sessionStorage.removeItem(PENDING_ORDER_KEY)
      return
    }
    product.value = obj
    // 还原 quantity
    const q = Number(route.query.quantity)
    if (q && q > 0) {
      quantity.value = Math.min(99, Math.max(1, q))
    }
  } catch {
    sessionStorage.removeItem(PENDING_ORDER_KEY)
  }
}

async function handleSubmit() {
  if (!product.value) return
  if (!payType.value) {
    ElMessage.warning('请选择支付方式')
    return
  }
  submitLoading.value = true
  try {
    const result: any = await h5Api.createOrder({
      shopProductId: product.value.id,
      quantity: quantity.value,
      payType: payType.value
    })
    // 清理 sessionStorage
    sessionStorage.removeItem(PENDING_ORDER_KEY)
    // 跳支付 URL 或通知
    if (result.payUrl) {
      ElNotification({
        title: '订单创建成功',
        message: `订单号 ${result.outTradeNo}，即将跳转支付`,
        type: 'success',
        duration: 3000
      })
      setTimeout(() => {
        window.location.href = result.payUrl
      }, 800)
    } else {
      ElNotification({
        title: '订单创建成功',
        message: `订单号 ${result.outTradeNo}，请前往支付`,
        type: 'success',
        duration: 0
      })
      setTimeout(() => {
        router.push('/h5/my-card')
      }, 1500)
    }
  } catch {
    // 拦截器已提示
  } finally {
    submitLoading.value = false
  }
}

function goShop() {
  router.push('/h5/shop')
}

onMounted(loadProduct)
</script>

<style scoped lang="scss">
.h5-order {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 80px;
}

.h5-order-section {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 14px 16px;
  box-shadow: var(--jicek-shadow-sm);

  .h5-section-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--jicek-text-secondary);
    line-height: 20px;
    margin-bottom: 10px;
  }
}

.h5-order-product {
  .h5-order-product-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }

  .h5-order-product-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 24px;
  }

  .h5-order-meta {
    font-size: 12px;
    color: var(--jicek-text-secondary);
    line-height: 18px;
    margin-top: 2px;
  }

  .h5-order-price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px dashed var(--jicek-border);
  }

  .h5-order-price-label {
    font-size: 13px;
    color: var(--jicek-text-secondary);
  }

  .h5-order-price {
    font-size: 16px;
    font-weight: 600;
    color: var(--jicek-danger);
    line-height: 24px;
  }
}

.h5-order-quantity {
  display: flex;
  align-items: center;
}

.h5-pay-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;

  .h5-pay-radio {
    width: 100%;
    margin-right: 0;
    height: 32px;
    line-height: 32px;
  }
}

.h5-order-total {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .h5-order-total-label {
    font-size: 14px;
    color: var(--jicek-text-primary);
  }

  .h5-order-total-value {
    font-size: 22px;
    font-weight: 700;
    color: var(--jicek-danger);
    line-height: 30px;
  }
}

.h5-order-submit {
  position: fixed;
  bottom: 56px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  padding: 12px 16px;
  background: var(--jicek-bg-primary);
  border-top: 1px solid var(--jicek-border);
  z-index: 9;
  box-sizing: border-box;
}

.h5-submit-btn {
  width: 100%;
  height: 40px;
  font-size: 15px;
  letter-spacing: 2px;
}

.h5-empty {
  padding: 40px 0;
}
</style>
