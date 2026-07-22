<!--
  极策k 支付配置页面
  作者: 极策k  日期: 2026-07-21

  功能：彩虹易支付网关地址/PID/商户密钥配置，启用通道勾选
  接口：GET /api/dev/pay/config/{tenantId}, POST /api/dev/pay/config
  安全：商户密钥为密码型输入，AES 加密存储
-->
<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">{{ t('payConfig.title') }}</span>
        <el-button type="primary" style="float: right" :loading="saving" @click="handleSave">
          {{ t('payConfig.save') }}
        </el-button>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" style="max-width: 750px">
        <el-form-item :label="t('payConfig.gatewayUrl')" prop="gatewayUrl">
          <el-input v-model="form.gatewayUrl" :placeholder="t('payConfig.gatewayUrlPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('payConfig.pid')" prop="pid">
          <el-input-number v-model="form.pid" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item :label="t('payConfig.merchantKey')" prop="merchantKey">
          <el-input
            v-model="form.merchantKey"
            type="password"
            show-password
            :placeholder="t('payConfig.merchantKeyPlaceholder')"
          />
          <div class="form-tip">{{ t('payConfig.merchantKeyTip') }}</div>
        </el-form-item>

        <el-form-item :label="t('payConfig.channels')">
          <el-checkbox-group v-model="selectedChannels">
            <el-checkbox value="alipay">{{ t('payConfig.channelAlipay') }}</el-checkbox>
            <el-checkbox value="wxpay">{{ t('payConfig.channelWxpay') }}</el-checkbox>
            <el-checkbox value="qqpay">{{ t('payConfig.channelQqpay') }}</el-checkbox>
            <el-checkbox value="unionpay">{{ t('payConfig.channelUnionpay') }}</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">{{ t('payConfig.channelsTip') }}</div>
        </el-form-item>

        <el-form-item :label="t('payConfig.notifyUrl')">
          <el-input v-model="form.notifyUrl" :placeholder="t('payConfig.notifyUrlPlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('payConfig.returnUrl')">
          <el-input v-model="form.returnUrl" :placeholder="t('payConfig.returnUrlPlaceholder')" />
        </el-form-item>

        <el-divider>{{ t('payConfig.cryptoTitle') }}</el-divider>

        <el-form-item :label="t('payConfig.cardCrypto')">
          <el-radio-group v-model="cryptoScheme">
            <el-radio value="rsa-aes">{{ t('payConfig.cryptoRsaAes') }}</el-radio>
            <el-radio value="sm">{{ t('payConfig.cryptoSm') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="t('payConfig.signStrength')">
          <el-radio-group v-model="signStrength">
            <el-radio value="md5">{{ t('payConfig.signMd5') }}</el-radio>
            <el-radio value="md5-replay">{{ t('payConfig.signMd5Replay') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button @click="handleTest">{{ t('payConfig.test') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { payApi } from '@/api'

const { t } = useI18n()

const formRef = ref<FormInstance>()
const saving = ref(false)
const cryptoScheme = ref('rsa-aes')
const signStrength = ref('md5-replay')

const form = reactive({
  id: undefined as number | undefined,
  tenantId: 1,
  gatewayUrl: '',
  pid: undefined as number | undefined,
  merchantKey: '',
  notifyUrl: '',
  returnUrl: 'https://www.jicek.com/pay/return',
  enabledChannels: '',
  enabled: 1
})

const selectedChannels = ref<string[]>([])

watch(selectedChannels, (val) => {
  form.enabledChannels = val.join(',')
})

const rules: FormRules = {
  gatewayUrl: [{ required: true, message: t('payConfig.gatewayUrlRequired'), trigger: 'blur' }],
  pid: [{ required: true, message: t('payConfig.pidRequired'), trigger: 'blur' }],
  merchantKey: [{ required: true, message: t('payConfig.merchantKeyRequired'), trigger: 'blur' }]
}

const loadConfig = async () => {
  try {
    const data: any = await payApi.getConfig(form.tenantId)
    if (data) {
      Object.assign(form, data)
      // merchantKey 已脱敏，清空避免覆盖
      form.merchantKey = ''
      selectedChannels.value = data.enabledChannels ? data.enabledChannels.split(',') : []
    }
  } catch (e) {
    // 未配置时静默
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (selectedChannels.value.length === 0) {
      ElMessage.warning(t('payConfig.channelsRequired'))
      return
    }
    saving.value = true
    try {
      await payApi.saveConfig(form)
      ElMessage.success(t('payConfig.saveSuccess'))
      loadConfig()
    } finally {
      saving.value = false
    }
  })
}

const handleTest = () => {
  ElMessage.info(t('payConfig.testTodo'))
}

loadConfig()
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: var(--jicek-text-secondary);
  margin-top: 4px;
}
</style>
