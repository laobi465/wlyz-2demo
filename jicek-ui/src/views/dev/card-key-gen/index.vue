<!--
  极策k 卡密生成页面
  作者: 极策k  日期: 2026-07-21

  功能：批量生成卡密（最多 1000 张），明文一次性展示弹窗 + 复制
  接口：POST /api/dev/card/generate
  安全：明文仅展示一次，关闭弹窗后不可再查看
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('cardKey.genTitle') }}</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px">
        <el-form-item :label="t('cardKey.software')" prop="softwareId">
          <el-select v-model="form.softwareId" :placeholder="t('cardKey.selectSoftware')" style="width: 100%">
            <el-option :label="t('cardKey.softwareOptionA')" :value="1" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('cardKey.cardType')" prop="cardTypeId">
          <el-select v-model="form.cardTypeId" :placeholder="t('cardKey.selectCardType')" style="width: 100%">
            <el-option :label="t('cardKey.monthCard')" :value="1" />
            <el-option :label="t('cardKey.yearCard')" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('cardKey.quantity')" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>

        <el-form-item :label="t('cardKey.prefix')">
          <el-input v-model="form.prefix" :placeholder="t('cardKey.prefixPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('cardKey.charset')">
          <el-radio-group v-model="form.charset">
            <el-radio :value="0">{{ t('cardKey.charsetAlphaNum') }}</el-radio>
            <el-radio :value="1">{{ t('cardKey.charsetNumber') }}</el-radio>
            <el-radio :value="2">{{ t('cardKey.charsetCustom') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.charset === 2" :label="t('cardKey.customCharset')">
          <el-input v-model="form.customCharset" :placeholder="t('cardKey.customCharsetPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('cardKey.length')" prop="length">
          <el-input-number v-model="form.length" :min="8" :max="64" style="width: 100%" />
        </el-form-item>

        <el-form-item :label="t('cardKey.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-alert
          type="warning"
          :closable="false"
          :title="t('cardKey.securityTip')"
          style="margin-bottom: 16px"
        />

        <el-form-item>
          <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
          <el-button type="primary" :loading="generating" @click="handleGenerate">
            {{ t('cardKey.confirmGenerate') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 生成结果弹窗 -->
    <el-dialog v-model="resultVisible" :title="t('cardKey.resultTitle')" width="800px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        :title="t('cardKey.resultAlert')"
        style="margin-bottom: 16px"
      />
      <el-input
        v-model="plainCardsText"
        type="textarea"
        :rows="15"
        readonly
      />
      <template #footer>
        <el-button type="primary" @click="handleCopy">{{ t('cardKey.copyAll') }}</el-button>
        <el-button @click="resultVisible = false">{{ t('cardKey.saved') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { cardKeyApi } from '@/api'

const { t } = useI18n()

const formRef = ref<FormInstance>()
const generating = ref(false)
const resultVisible = ref(false)
const plainCardsText = ref('')

const form = reactive({
  tenantId: 1,
  softwareId: undefined as number | undefined,
  cardTypeId: undefined as number | undefined,
  quantity: 100,
  prefix: 'JC-',
  charset: 0,
  customCharset: '',
  length: 24,
  remark: ''
})

const rules: FormRules = {
  softwareId: [{ required: true, message: t('cardKey.softwareRequired'), trigger: 'change' }],
  cardTypeId: [{ required: true, message: t('cardKey.cardTypeRequired'), trigger: 'change' }],
  quantity: [{ required: true, message: t('cardKey.quantityRequired'), trigger: 'blur' }],
  length: [{ required: true, message: t('cardKey.lengthRequired'), trigger: 'blur' }]
}

const handleGenerate = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    generating.value = true
    try {
      const resp: any = await cardKeyApi.generate(form)
      plainCardsText.value = (resp.plainCards || []).join('\n')
      resultVisible.value = true
      ElMessage.success(t('cardKey.generateSuccess', { count: resp.count }))
    } finally {
      generating.value = false
    }
  })
}

const handleReset = () => {
  formRef.value?.resetFields()
  form.prefix = 'JC-'
  form.charset = 0
  form.length = 24
}

const handleCopy = () => {
  navigator.clipboard.writeText(plainCardsText.value).then(() => {
    ElMessage.success(t('cardKey.copied'))
  })
}
</script>
