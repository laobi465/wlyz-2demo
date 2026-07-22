<!--
  极策k网络验证 - H5 卡密登录
  作者: 极策k  日期: 2026-07-22

  v0.13.0 功能1：H5 终端用户卡密登录
   - 表单：appKey（32 位）+ cardKey（≥ 8 位）
   - 登录成功存 h5Token 到 localStorage，跳 /h5/my-card
   - 已登录直接跳 /h5/my-card
   - 失焦即校验 + 提交前全表单校验 + 按钮 loading
   - 遵循 docs/UI-DESIGN.md（无渐变、无 emoji）
-->
<template>
  <div class="h5-login">
    <div class="h5-login-header">
      <div class="h5-logo">极策k</div>
      <div class="h5-logo-sub">网络验证 · 终端用户</div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="AppKey" prop="appKey">
        <el-input
          v-model="form.appKey"
          placeholder="请输入 32 位 AppKey"
          clearable
          maxlength="32"
        />
      </el-form-item>

      <el-form-item label="卡密" prop="cardKey">
        <el-input
          v-model="form.cardKey"
          type="password"
          placeholder="请输入卡密（至少 8 位）"
          show-password
          clearable
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="h5-login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form-item>
    </el-form>

    <div class="h5-login-tip">卡密由开发者通过短信/IM 等渠道分发，请妥善保管</div>

    <div class="h5-login-actions">
      <el-button link type="primary" @click="goAgentRegister">代理注册入口</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { h5Api, H5_TOKEN_KEY } from '@/api'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  appKey: '',
  cardKey: ''
})

const rules: FormRules = {
  appKey: [
    { required: true, message: '请输入 AppKey', trigger: 'blur' },
    { len: 32, message: 'AppKey 长度必须为 32 字符', trigger: 'blur' }
  ],
  cardKey: [
    { required: true, message: '请输入卡密', trigger: 'blur' },
    { min: 8, message: '卡密长度至少 8 字符', trigger: 'blur' }
  ]
}

onMounted(() => {
  // 已登录跳转
  if (localStorage.getItem(H5_TOKEN_KEY)) {
    const redirect = (route.query.redirect as string) || '/h5/my-card'
    router.replace(redirect)
  }
})

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const result: any = await h5Api.login({
        appKey: form.appKey.trim(),
        cardKey: form.cardKey.trim()
      })
      localStorage.setItem(H5_TOKEN_KEY, result.h5Token)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/h5/my-card'
      router.replace(redirect)
    } catch {
      // 响应拦截器已统一提示
    } finally {
      loading.value = false
    }
  })
}

function goAgentRegister() {
  router.push('/h5/agent/register')
}
</script>

<style scoped lang="scss">
.h5-login {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 44px - 56px - 32px);
  padding-top: 32px;
}

.h5-login-header {
  text-align: center;
  margin-bottom: 32px;

  .h5-logo {
    font-size: 24px;
    font-weight: 700;
    color: var(--jicek-primary);
    letter-spacing: 2px;
    line-height: 32px;
  }

  .h5-logo-sub {
    font-size: 13px;
    color: var(--jicek-text-secondary);
    margin-top: 4px;
    line-height: 22px;
  }
}

.h5-login-btn {
  width: 100%;
  height: 40px;
  font-size: 15px;
  letter-spacing: 4px;
}

.h5-login-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--jicek-text-placeholder);
  line-height: 18px;
  padding: 0 8px;
}

.h5-login-actions {
  margin-top: 24px;
  text-align: center;
}
</style>
