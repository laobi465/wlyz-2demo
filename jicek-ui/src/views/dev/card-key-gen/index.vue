<template>
  <div class="jicek-page">
    <el-card>
      <template #header>
        <span class="jicek-card-title">卡密生成</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px">
        <el-form-item label="所属软件" prop="softwareId">
          <el-select v-model="form.softwareId" placeholder="请选择软件" style="width: 100%">
            <el-option label="软件A" :value="1" />
          </el-select>
        </el-form-item>

        <el-form-item label="卡类" prop="cardTypeId">
          <el-select v-model="form.cardTypeId" placeholder="请选择卡类" style="width: 100%">
            <el-option label="月卡" :value="1" />
            <el-option label="年卡" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" :max="1000" style="width: 100%" />
        </el-form-item>

        <el-form-item label="前缀">
          <el-input v-model="form.prefix" placeholder="如 JC-，留空则无前缀" />
        </el-form-item>

        <el-form-item label="字符集">
          <el-radio-group v-model="form.charset">
            <el-radio :value="0">大小写字母+数字</el-radio>
            <el-radio :value="1">纯数字</el-radio>
            <el-radio :value="2">自定义</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.charset === 2" label="自定义字符集">
          <el-input v-model="form.customCharset" placeholder="如 ABCDEFG12345" />
        </el-form-item>

        <el-form-item label="长度" prop="length">
          <el-input-number v-model="form.length" :min="8" :max="64" style="width: 100%" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-alert
          type="warning"
          :closable="false"
          title="安全提示：生成后卡密将加密存储，明文仅在本次展示一次"
          style="margin-bottom: 16px"
        />

        <el-form-item>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" :loading="generating" @click="handleGenerate">
            确认生成
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 生成结果弹窗 -->
    <el-dialog v-model="resultVisible" title="生成结果" width="800px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        title="明文卡密仅展示本次，请立即复制保存到本地！"
        style="margin-bottom: 16px"
      />
      <el-input
        v-model="plainCardsText"
        type="textarea"
        :rows="15"
        readonly
      />
      <template #footer>
        <el-button type="primary" @click="handleCopy">复制全部</el-button>
        <el-button @click="resultVisible = false">已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { cardKeyApi } from '@/api'

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
  softwareId: [{ required: true, message: '请选择软件', trigger: 'change' }],
  cardTypeId: [{ required: true, message: '请选择卡类', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  length: [{ required: true, message: '请输入长度', trigger: 'blur' }]
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
      ElMessage.success(`成功生成 ${resp.count} 张卡密`)
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
    ElMessage.success('已复制到剪贴板')
  })
}
</script>
