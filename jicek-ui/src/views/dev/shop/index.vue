<!--
  极策k网络验证 - 开发者后台 内嵌卡网管理
  作者: 极策k  日期: 2026-07-22

  v0.13.0 功能3 后台部分：
   - 店铺 CRUD + 开启/关闭切换
   - 商品管理（添加 / 编辑 / 删除商品，绑定卡类）
   - JWT 鉴权（router 守卫已有）
   - 删除前 ElMessageBox.confirm 二次确认
   - 遵循 docs/UI-DESIGN.md（无渐变、无 emoji）

  接口：
    GET    /api/dev/shop/page                       分页查询
    GET    /api/dev/shop/{id}                       详情
    POST   /api/dev/shop                            创建
    PUT    /api/dev/shop                            更新
    DELETE /api/dev/shop/{id}                       删除
    POST   /api/dev/shop/{id}/open                  开启
    POST   /api/dev/shop/{id}/close                 关闭
    GET    /api/dev/shop/{shopId}/products          商品列表
    POST   /api/dev/shop/product                    添加商品
    PUT    /api/dev/shop/product                    编辑商品
    DELETE /api/dev/shop/product/{shopId}/{productId} 删除商品
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('shop.title') }}</span>
        <el-button type="primary" style="float: right" @click="handleCreate">{{ t('shop.create') }}</el-button>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="filter" style="margin-bottom: 16px">
        <el-form-item :label="t('shop.software')">
          <el-select
            v-model="filter.softwareId"
            :placeholder="t('shop.allSoftware')"
            clearable
            style="width: 180px"
            @change="loadData"
          >
            <el-option
              v-for="sw in softwareList"
              :key="sw.id"
              :label="sw.name"
              :value="sw.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('shop.name')">
          <el-input
            v-model="filter.name"
            :placeholder="t('shop.namePlaceholder')"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item :label="t('shop.status')">
          <el-select
            v-model="filter.status"
            :placeholder="t('common.all')"
            clearable
            style="width: 120px"
            @change="loadData"
          >
            <el-option :label="t('shop.statusOpen')" :value="1" />
            <el-option :label="t('shop.statusClosed')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">{{ t('common.search') }}</el-button>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" :label="t('shop.shopName')" min-width="140" />
        <el-table-column prop="path" :label="t('shop.path')" min-width="140">
          <template #default="{ row }">
            <el-text type="primary" class="mono-text">{{ row.path }}</el-text>
          </template>
        </el-table-column>
        <el-table-column prop="softwareName" :label="t('shop.softwareName')" min-width="120" />
        <el-table-column prop="contact" :label="t('shop.contact')" min-width="140">
          <template #default="{ row }">
            <span v-if="row.contact">{{ row.contact }}</span>
            <span v-else style="color: var(--jicek-text-secondary)">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('shop.status')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? t('shop.statusOpen') : t('shop.statusClosed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="t('shop.createTime')" min-width="160" />
        <el-table-column :label="t('common.operation')" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">{{ t('shop.editBtn') }}</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="warning"
              size="small"
              @click="handleToggle(row, 'close')"
            >
              {{ t('shop.closeBtn') }}
            </el-button>
            <el-button
              v-else
              link
              type="success"
              size="small"
              @click="handleToggle(row, 'open')"
            >
              {{ t('shop.openBtn') }}
            </el-button>
            <el-button link type="info" size="small" @click="handleManageProducts(row)">
              {{ t('shop.manageProducts') }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">{{ t('shop.deleteBtn') }}</el-button>
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

    <!-- 店铺新建/编辑弹窗 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="formMode === 'create' ? t('shop.create') : t('shop.edit')"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('shop.shopNameLabel')" prop="name">
          <el-input v-model="form.name" :placeholder="t('shop.shopNamePlaceholder')" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('shop.ownerSoftware')" prop="softwareId">
          <el-select v-model="form.softwareId" :placeholder="t('shop.selectSoftware')" style="width: 100%">
            <el-option
              v-for="sw in softwareList"
              :key="sw.id"
              :label="sw.name"
              :value="sw.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('shop.shopPath')" prop="path">
          <el-input
            v-model="form.path"
            :placeholder="t('shop.shopPathPlaceholder')"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="t('shop.shopDesc')" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            :placeholder="t('shop.shopDescPlaceholder')"
            maxlength="256"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="t('shop.contactLabel')" prop="contact">
          <el-input v-model="form.contact" :placeholder="t('shop.contactPlaceholder')" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('shop.statusLabel')" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            :active-text="t('shop.statusOpen')"
            :inactive-text="t('shop.statusClosed')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">{{ t('shop.cancel') }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">{{ t('shop.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 商品管理弹窗 -->
    <el-dialog
      v-model="productDialogVisible"
      :title="t('shop.productDialogTitle', { name: currentShop?.name || '' })"
      width="820px"
      :close-on-click-modal="false"
    >
      <div style="margin-bottom: 12px; text-align: right">
        <el-button type="primary" size="small" @click="handleAddProduct">{{ t('shop.addProduct') }}</el-button>
      </div>
      <el-table v-loading="productLoading" :data="productList" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="cardTypeName" :label="t('shop.cardType')" min-width="140" />
        <el-table-column prop="cardType" :label="t('shop.cardTypeType')" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="cardTypeTagType(row.cardType)">
              {{ cardTypeText(row.cardType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" :label="t('shop.price')" width="110">
          <template #default="{ row }">¥{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" :label="t('shop.sortOrder')" width="80" />
        <el-table-column prop="status" :label="t('shop.statusProductLabel')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? t('shop.onShelf') : t('shop.offShelf') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEditProduct(row)">{{ t('shop.editProduct') }}</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteProduct(row)">{{ t('shop.deleteProduct') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 商品新建/编辑弹窗 -->
    <el-dialog
      v-model="productFormDialogVisible"
      :title="productFormMode === 'create' ? t('shop.productCreateTitle') : t('shop.productEditTitle')"
      width="520px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="100px">
        <el-form-item :label="t('shop.cardTypeLabel')" prop="cardTypeId">
          <el-select v-model="productForm.cardTypeId" :placeholder="t('shop.selectCardType')" style="width: 100%" filterable>
            <el-option
              v-for="ct in cardTypeList"
              :key="ct.id"
              :label="ct.name"
              :value="ct.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('shop.priceLabel')" prop="price">
          <el-input-number v-model="productForm.price" :min="0" :precision="2" :step="1" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">{{ t('shop.priceUnit') }}</span>
        </el-form-item>
        <el-form-item :label="t('shop.sortOrderLabel')" prop="sortOrder">
          <el-input-number v-model="productForm.sortOrder" :min="0" :max="9999" />
          <span style="margin-left: 8px; color: var(--jicek-text-secondary)">{{ t('shop.sortOrderHint') }}</span>
        </el-form-item>
        <el-form-item :label="t('shop.statusProductLabel')" prop="status">
          <el-switch
            v-model="productForm.status"
            :active-value="1"
            :inactive-value="0"
            :active-text="t('shop.onShelf')"
            :inactive-text="t('shop.offShelf')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productFormDialogVisible = false">{{ t('shop.cancel') }}</el-button>
        <el-button type="primary" :loading="productSubmitLoading" @click="submitProductForm">
          {{ t('shop.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { shopApi, softwareApi, cardTypeApi } from '@/api'

const { t } = useI18n()

interface ShopRow {
  id: number
  tenantId: number
  softwareId: number
  name: string
  path: string
  description?: string
  contact?: string
  status: number
  createTime: string
  softwareName: string
}

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

interface SoftwareOption {
  id: number
  name: string
}

interface CardTypeOption {
  id: number
  name: string
}

const loading = ref(false)
const tableData = ref<ShopRow[]>([])
const total = ref(0)
const softwareList = ref<SoftwareOption[]>([])
const cardTypeList = ref<CardTypeOption[]>([])

const filter = reactive({
  current: 1,
  size: 20,
  softwareId: undefined as number | undefined,
  name: '',
  status: undefined as number | undefined
})

async function loadSoftwareList() {
  try {
    const res: any = await softwareApi.page({ current: 1, size: 100 })
    softwareList.value = (res.records || []).map((s: any) => ({ id: s.id, name: s.name }))
  } catch {
    // 拦截器已提示
  }
}

async function loadCardTypeList() {
  try {
    const res: any = await cardTypeApi.page({ current: 1, size: 200 })
    cardTypeList.value = (res.records || []).map((c: any) => ({ id: c.id, name: c.name }))
  } catch {
    // 拦截器已提示
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await shopApi.page({
      current: filter.current,
      size: filter.size,
      softwareId: filter.softwareId,
      name: filter.name || undefined,
      status: filter.status
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filter.softwareId = undefined
  filter.name = ''
  filter.status = undefined
  filter.current = 1
  loadData()
}

onMounted(() => {
  loadSoftwareList()
  loadCardTypeList()
  loadData()
})

/* ============ 辅助 ============ */
function formatPrice(price: number): string {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

function cardTypeText(type: number): string {
  const map: Record<number, string> = { 1: t('shop.typeDuration'), 2: t('shop.typeCount'), 3: t('shop.typeFeature'), 4: t('shop.typePermanent') }
  return map[type] || t('shop.typeUnknown')
}

function cardTypeTagType(t: number): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'info',
    2: 'success',
    3: 'warning',
    4: 'info'
  }
  return map[t] || ''
}

/* ============ 店铺新建/编辑 ============ */
const formDialogVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  softwareId: undefined as number | undefined,
  path: '',
  description: '',
  contact: '',
  status: 1
})

const rules: FormRules = {
  name: [
    { required: true, message: t('shop.shopNameRequired'), trigger: 'blur' },
    { max: 64, message: t('shop.shopNameMax'), trigger: 'blur' }
  ],
  softwareId: [{ required: true, message: t('shop.softwareRequired'), trigger: 'change' }],
  path: [
    { required: true, message: t('shop.shopPathRequired'), trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_-]{1,64}$/,
      message: t('shop.shopPathPattern'),
      trigger: 'blur'
    }
  ],
  contact: [{ max: 128, message: t('shop.contactMax'), trigger: 'blur' }]
}

function handleCreate() {
  formMode.value = 'create'
  form.id = undefined
  form.name = ''
  form.softwareId = undefined
  form.path = ''
  form.description = ''
  form.contact = ''
  form.status = 1
  formDialogVisible.value = true
}

function handleEdit(row: ShopRow) {
  formMode.value = 'edit'
  form.id = row.id
  form.name = row.name
  form.softwareId = row.softwareId
  form.path = row.path
  form.description = row.description || ''
  form.contact = row.contact || ''
  form.status = row.status
  formDialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload = {
        name: form.name,
        softwareId: form.softwareId!,
        path: form.path,
        description: form.description || undefined,
        contact: form.contact || undefined,
        status: form.status
      }
      if (formMode.value === 'create') {
        await shopApi.create(payload)
        ElMessage.success(t('shop.shopCreateSuccess'))
      } else {
        await shopApi.update({ id: form.id!, ...payload })
        ElMessage.success(t('shop.shopUpdateSuccess'))
      }
      formDialogVisible.value = false
      loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

/* ============ 开启/关闭切换 ============ */
async function handleToggle(row: ShopRow, action: 'open' | 'close') {
  const actionText = action === 'open' ? t('shop.actionOpen') : t('shop.actionClose')
  try {
    await ElMessageBox.confirm(
      t('shop.toggleMessage', { action: actionText, name: row.name }),
      t('shop.toggleTitle', { action: actionText }),
      { confirmButtonText: actionText, cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    if (action === 'open') {
      await shopApi.open(row.id)
    } else {
      await shopApi.close(row.id)
    }
    ElMessage.success(t('shop.toggleSuccess', { action: actionText }))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

/* ============ 删除店铺 ============ */
async function handleDelete(row: ShopRow) {
  try {
    await ElMessageBox.confirm(
      t('shop.deleteMessage', { name: row.name }),
      t('shop.deleteTitle'),
      { confirmButtonText: t('shop.deleteConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await shopApi.delete(row.id)
    ElMessage.success(t('shop.deleteSuccess'))
    loadData()
  } catch {
    // 拦截器已提示
  }
}

/* ============ 商品管理 ============ */
const productDialogVisible = ref(false)
const productLoading = ref(false)
const productList = ref<ShopProduct[]>([])
const currentShop = ref<ShopRow | null>(null)

async function handleManageProducts(row: ShopRow) {
  currentShop.value = row
  productDialogVisible.value = true
  await loadProducts(row.id)
}

async function loadProducts(shopId: number) {
  productLoading.value = true
  try {
    const res: any = await shopApi.listProducts(shopId)
    productList.value = Array.isArray(res) ? res : []
  } finally {
    productLoading.value = false
  }
}

/* ============ 商品新建/编辑 ============ */
const productFormDialogVisible = ref(false)
const productFormMode = ref<'create' | 'edit'>('create')
const productSubmitLoading = ref(false)
const productFormRef = ref<FormInstance>()

const productForm = reactive({
  id: undefined as number | undefined,
  shopId: undefined as number | undefined,
  cardTypeId: undefined as number | undefined,
  price: 0,
  sortOrder: 0,
  status: 1
})

const productRules: FormRules = {
  cardTypeId: [{ required: true, message: t('shop.cardTypeRequired'), trigger: 'change' }],
  price: [
    { required: true, message: t('shop.priceRequired'), trigger: 'blur' },
    { type: 'number', min: 0, message: t('shop.priceNegative'), trigger: 'blur' }
  ]
}

function handleAddProduct() {
  if (!currentShop.value) return
  productFormMode.value = 'create'
  productForm.id = undefined
  productForm.shopId = currentShop.value.id
  productForm.cardTypeId = undefined
  productForm.price = 0
  productForm.sortOrder = 0
  productForm.status = 1
  productFormDialogVisible.value = true
}

function handleEditProduct(row: ShopProduct) {
  productFormMode.value = 'edit'
  productForm.id = row.id
  productForm.shopId = row.shopId
  productForm.cardTypeId = row.cardTypeId
  productForm.price = row.price
  productForm.sortOrder = row.sortOrder
  productForm.status = row.status
  productFormDialogVisible.value = true
}

async function submitProductForm() {
  if (!productFormRef.value) return
  await productFormRef.value.validate(async (valid) => {
    if (!valid) return
    productSubmitLoading.value = true
    try {
      const payload = {
        shopId: productForm.shopId!,
        cardTypeId: productForm.cardTypeId!,
        price: productForm.price,
        sortOrder: productForm.sortOrder,
        status: productForm.status
      }
      if (productFormMode.value === 'create') {
        await shopApi.addProduct(payload)
        ElMessage.success(t('shop.productAddSuccess'))
      } else {
        await shopApi.updateProduct({ id: productForm.id!, ...payload })
        ElMessage.success(t('shop.productUpdateSuccess'))
      }
      productFormDialogVisible.value = false
      if (currentShop.value) {
        loadProducts(currentShop.value.id)
      }
    } finally {
      productSubmitLoading.value = false
    }
  })
}

async function handleDeleteProduct(row: ShopProduct) {
  try {
    await ElMessageBox.confirm(
      t('shop.productDeleteMessage', { name: row.cardTypeName }),
      t('shop.productDeleteTitle'),
      { confirmButtonText: t('shop.productDeleteConfirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await shopApi.removeProduct(row.shopId, row.id)
    ElMessage.success(t('shop.productDeleteSuccess'))
    if (currentShop.value) {
      loadProducts(currentShop.value.id)
    }
  } catch {
    // 拦截器已提示
  }
}
</script>

<style scoped lang="scss">
.mono-text {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
}
</style>
