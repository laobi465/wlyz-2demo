<!--
  极策k SDK 代码生成弹窗（v0.12.0）
  作者: 极策k  日期: 2026-07-22

  功能：
   - 开发者在软件管理页点「接入代码」打开此弹窗
   - 自动拉取软件的 appKey + rsaPublicKey（明文）
   - 一键生成 9 种语言的快速接入代码
   - 支持 serverUrl 编辑、代码复制
   - signSecret 为脱敏字段，模板中留占位符由开发者手动填入

  注意：
   - appKey / rsaPublicKey 从后端 detail 接口获取（明文返回）
   - signSecret 后端脱敏存储，无法自动填入，代码模板中以占位符提示
-->
<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="一键接入代码"
    width="900px"
    top="5vh"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-loading="loading" class="sdk-codegen">
      <!-- 顶部配置区 -->
      <el-alert type="warning" :closable="false" style="margin-bottom: 12px">
        <template #title>
          以下代码已自动填入 <b>AppKey</b> 与 <b>RSA 公钥</b>。<b>签名密钥 signSecret</b>
          为脱敏存储无法自动填入，请从创建/轮换密钥时保存的明文中获取并替换代码中的占位符。
        </template>
      </el-alert>

      <el-form :inline="true" style="margin-bottom: 12px">
        <el-form-item label="软件">
          <el-text type="primary" tag="b">{{ softwareName || '-' }}</el-text>
        </el-form-item>
        <el-form-item label="服务端地址">
          <el-input v-model="serverUrl" style="width: 280px" placeholder="https://api.jicek.com" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="regenerate">重新生成</el-button>
        </el-form-item>
      </el-form>

      <!-- 语言 Tab -->
      <el-tabs v-model="activeLang" type="card">
        <el-tab-pane
          v-for="lang in SDK_LANGUAGES"
          :key="lang.key"
          :label="lang.label"
          :name="lang.key"
        >
          <div class="lang-desc">
            <el-text size="small" type="info">{{ lang.desc }}</el-text>
            <el-button link type="primary" size="small" style="float: right" @click="copyCode(lang.key)">
              <el-icon><CopyDocument /></el-icon> 复制代码
            </el-button>
          </div>
          <pre class="code-block"><code>{{ codeCache[lang.key] }}</code></pre>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { softwareApi } from '@/api'
import { SDK_LANGUAGES, generateSdkCode, type SdkLanguage } from '@/utils/sdk-code-templates'

const props = defineProps<{
  modelValue: boolean
  softwareId: number
  softwareName?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const activeLang = ref<SdkLanguage>('python')
const serverUrl = ref('')
const codeCache = reactive<Record<string, string>>({})

// 软件凭证
const appKey = ref('')
const rsaPublicKey = ref('')

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible && props.softwareId) {
      await loadSoftwareDetail()
    }
  }
)

async function loadSoftwareDetail() {
  loading.value = true
  try {
    const res: any = await softwareApi.get(props.softwareId)
    appKey.value = res.appKey || ''
    rsaPublicKey.value = res.rsaPublicKey || ''
    // 默认服务端地址：用当前后台地址的 origin（开发者可自行修改）
    serverUrl.value = window.location.origin
    regenerate()
  } catch (e) {
    ElMessage.error('获取软件信息失败')
  } finally {
    loading.value = false
  }
}

function regenerate() {
  for (const lang of SDK_LANGUAGES) {
    codeCache[lang.key] = generateSdkCode(lang.key, {
      appKey: appKey.value,
      rsaPublicKey: rsaPublicKey.value,
      serverUrl: serverUrl.value,
      softwareName: props.softwareName || '我的软件'
    })
  }
}

async function copyCode(lang: SdkLanguage) {
  const code = codeCache[lang]
  if (!code) {
    ElMessage.warning('代码尚未生成')
    return
  }
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success(`${lang} 代码已复制到剪贴板`)
  } catch {
    ElMessage.warning('复制失败，请手动选择复制')
  }
}
</script>

<style scoped>
.sdk-codegen {
  min-height: 400px;
}
.lang-desc {
  margin-bottom: 8px;
  overflow: hidden;
}
.code-block {
  max-height: 480px;
  overflow: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 6px;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  white-space: pre;
  tab-size: 4;
}
.code-block code {
  color: inherit;
  font-family: inherit;
}
</style>
