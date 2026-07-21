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
        <span class="jicek-card-title">支付配置</span>
        <el-button type="primary" style="float: right" :loading="saving" @click="handleSave">
          保存配置
        </el-button>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" style="max-width: 750px">
        <el-form-item label="支付网关地址" prop="gatewayUrl">
          <el-input v-model="form.gatewayUrl" placeholder="https://pay.example.com" />
        </el-form-item>

        <el-form-item label="商户 ID" prop="pid">
          <el-input-number v-model="form.pid" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="商户密钥" prop="merchantKey">
          <el-input
            v-model="form.merchantKey"
            type="password"
            show-password
            placeholder="彩虹易支付商户密钥"
          />
          <div class="form-tip">密钥采用 AES-256-GCM 加密存储，禁明文</div>
        </el-form-item>

        <el-form-item label="支付通道">
          <el-checkbox-group v-model="selectedChannels">
            <el-checkbox value="alipay">支付宝 (alipay)</el-checkbox>
            <el-checkbox value="wxpay">微信支付 (wxpay)</el-checkbox>
            <el-checkbox value="qqpay">QQ 钱包 (qqpay)</el-checkbox>
            <el-checkbox value="unionpay">银联云闪付 (unionpay)</el-checkbox>
          </el-checkbox-group>
          <div class="form-tip">用户不可选支付通道，由你勾选的通道展示给用户</div>
        </el-form-item>

        <el-form-item label="异步通知地址">
          <el-input v-model="form.notifyUrl" placeholder="系统自动生成" />
        </el-form-item>

        <el-form-item label="同步跳转地址">
          <el-input v-model="form.returnUrl" placeholder="https://www.example.com/pay/return" />
        </el-form-item>

        <el-divider>加密选项</el-divider>

        <el-form-item label="卡密传输加密">
          <el-radio-group v-model="cryptoScheme">
            <el-radio value="rsa-aes">RSA-2048 + AES-256-GCM（推荐）</el-radio>
            <el-radio value="sm">国密 SM2 + SM4</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="回调验签强度">
          <el-radio-group v-model="signStrength">
            <el-radio value="md5">MD5（V1 兼容）</el-radio>
            <el-radio value="md5-replay">MD5 + 时间戳防重放（推荐）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button @click="handleTest">测试连接</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { payApi } from '@/api'

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
  gatewayUrl: [{ required: true, message: '请输入支付网关地址', trigger: 'blur' }],
  pid: [{ required: true, message: '请输入商户 ID', trigger: 'blur' }],
  merchantKey: [{ required: true, message: '请输入商户密钥', trigger: 'blur' }]
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
      ElMessage.warning('至少启用一个支付通道')
      return
    }
    saving.value = true
    try {
      await payApi.saveConfig(form)
      ElMessage.success('保存成功')
      loadConfig()
    } finally {
      saving.value = false
    }
  })
}

const handleTest = () => {
  ElMessage.info('测试连接功能待实现（v0.3.0）')
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
