<!--
  极策k网络验证 - H5 购卡店铺
  作者: 极策k  日期: 2026-07-22

  v0.13.0 功能3：H5 购卡-店铺列表
   - 路由支持 ?path=xxx 直接打开指定店铺
   - 输入框 + 查询按钮：输入店铺 path
   - 调 h5Api.shopInfo(path) 公开接口加载店铺信息 + 商品列表
   - 商品卡片：cardTypeName + 卡类徽章 + 价格 + 立即购买
   - 点击购买：存商品到 sessionStorage 跳 /h5/shop/order?productId=xxx&quantity=1
   - 未登录跳 /h5/login?redirect=/h5/shop
   - 遵循 docs/UI-DESIGN.md（无渐变、无 emoji）
-->
<template>
  <div class="h5-shop">
    <!-- 店铺查询输入 -->
    <div class="h5-shop-search">
      <el-input
        v-model="pathInput"
        placeholder="请输入店铺路径"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :loading="loading" @click="handleSearch">查询</el-button>
        </template>
      </el-input>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="h5-skeleton">
      <el-skeleton :rows="4" animated />
      <el-skeleton :rows="3" animated style="margin-top: 12px" />
    </div>

    <template v-else-if="shopInfo">
      <!-- 店铺信息 -->
      <div class="h5-shop-info">
        <div class="h5-shop-name">{{ shopInfo.name }}</div>
        <div class="h5-shop-software">所属软件：{{ shopInfo.softwareName }}</div>
        <div v-if="shopInfo.description" class="h5-shop-desc">
          {{ shopInfo.description }}
        </div>
        <div v-if="shopInfo.contact" class="h5-shop-contact">
          <span class="h5-shop-contact-label">联系方式：</span>
          <span>{{ shopInfo.contact }}</span>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="h5-product-section">
        <div class="h5-section-title">可购商品</div>
        <div v-if="!shopInfo.products || !shopInfo.products.length" class="h5-empty">
          <el-empty description="暂无可购商品" />
        </div>
        <div v-else class="h5-product-list">
          <div
            v-for="prod in shopInfo.products"
            :key="prod.id"
            class="h5-product-card"
          >
            <div class="h5-product-top">
              <div class="h5-product-name">{{ prod.cardTypeName }}</div>
              <span class="jicek-tag" :class="cardTypeTagClass(prod.cardType)">
                {{ cardTypeText(prod.cardType) }}
              </span>
            </div>
            <div class="h5-product-meta">
              <span v-if="prod.duration" class="h5-product-meta-item">
                时长：{{ prod.duration }}
              </span>
              <span v-if="prod.count" class="h5-product-meta-item">
                次数：{{ prod.count }}
              </span>
              <span v-if="prod.features" class="h5-product-meta-item h5-product-feat">
                功能：{{ prod.features }}
              </span>
            </div>
            <div class="h5-product-bottom">
              <div class="h5-product-price">¥{{ formatPrice(prod.price) }}</div>
              <el-button
                type="primary"
                size="small"
                :disabled="prod.status !== 1"
                @click="handleBuy(prod)"
              >
                立即购买
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else-if="searched" class="h5-empty">
      <el-empty description="未找到店铺或店铺已关闭" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

interface ShopInfo {
  id: number
  name: string
  description?: string
  contact?: string
  softwareName: string
  products: ShopProduct[]
}

const route = useRoute()
const router = useRouter()

const PENDING_ORDER_KEY = 'jicek_h5_pending_order'

const pathInput = ref('')
const loading = ref(false)
const searched = ref(false)
const shopInfo = ref<ShopInfo | null>(null)

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

async function loadShop(path: string) {
  loading.value = true
  searched.value = true
  try {
    const result: any = await h5Api.shopInfo(path)
    shopInfo.value = result as ShopInfo
  } catch {
    // 拦截器已提示
    shopInfo.value = null
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  const path = pathInput.value.trim()
  if (!path) {
    ElMessage.warning('请输入店铺路径')
    return
  }
  loadShop(path)
}

function handleBuy(product: ShopProduct) {
  if (!localStorage.getItem(H5_TOKEN_KEY)) {
    ElMessage.warning('请先登录后购买')
    router.push({ path: '/h5/login', query: { redirect: '/h5/shop' } })
    return
  }
  // 存当前选中商品到 sessionStorage，跳订单页
  sessionStorage.setItem(PENDING_ORDER_KEY, JSON.stringify(product))
  router.push({
    path: '/h5/shop/order',
    query: { productId: String(product.id), quantity: '1' }
  })
}

onMounted(() => {
  // 路由 query ?path=xxx 直接打开店铺
  const path = (route.query.path as string) || ''
  if (path) {
    pathInput.value = path
    loadShop(path)
  }
})
</script>

<style scoped lang="scss">
.h5-shop {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.h5-shop-search {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 12px;
  box-shadow: var(--jicek-shadow-sm);
}

.h5-skeleton {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 16px;
}

.h5-shop-info {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 16px;
  box-shadow: var(--jicek-shadow-sm);

  .h5-shop-name {
    font-size: 18px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 28px;
    margin-bottom: 4px;
  }

  .h5-shop-software {
    font-size: 13px;
    color: var(--jicek-text-secondary);
    line-height: 20px;
    margin-bottom: 8px;
  }

  .h5-shop-desc {
    font-size: 14px;
    color: var(--jicek-text-primary);
    line-height: 22px;
    margin-bottom: 6px;
    white-space: pre-wrap;
  }

  .h5-shop-contact {
    font-size: 13px;
    color: var(--jicek-text-secondary);
    line-height: 20px;

    .h5-shop-contact-label {
      margin-right: 4px;
    }
  }
}

.h5-product-section {
  .h5-section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 22px;
    margin-bottom: 8px;
    padding-left: 4px;
  }
}

.h5-empty {
  padding: 24px 0;
}

.h5-product-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.h5-product-card {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 12px 14px;
  box-shadow: var(--jicek-shadow-sm);

  .h5-product-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }

  .h5-product-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 24px;
  }

  .h5-product-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 12px;
    margin-bottom: 8px;

    .h5-product-meta-item {
      font-size: 12px;
      color: var(--jicek-text-secondary);
      line-height: 18px;
    }

    .h5-product-feat {
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .h5-product-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .h5-product-price {
    font-size: 18px;
    font-weight: 700;
    color: var(--jicek-danger);
    line-height: 24px;
  }
}
</style>
