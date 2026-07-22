<!--
  极策k网络验证 - 管理员登录页
  作者: 极策k  日期: 2026-07-22

  v0.15.0 管理员后台：
   - 表单字段：用户名 + 密码（无租户ID，管理员为平台全局账号）
   - 登录调 /api/auth/admin/login，成功后存 jicek_admin_token（独立于开发者 jicek_token）
   - 跳转 /admin/ticket
   - 遵循 docs/UI-DESIGN.md 现代简约风格规范（无渐变、无 emoji）
-->
<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-text">{{ t('admin.login.title') }}</div>
        <div class="logo-sub">{{ t('admin.login.subtitle') }}</div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item :label="t('admin.login.username')" prop="username">
          <el-input
            v-model="form.username"
            :placeholder="t('admin.login.usernamePlaceholder')"
            clearable
          />
        </el-form-item>

        <el-form-item :label="t('admin.login.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('admin.login.passwordPlaceholder')"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? t('admin.login.logging') : t('admin.login.login') }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">
        {{ t('admin.login.tip') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { adminAuthApi, ADMIN_TOKEN_KEY, ADMIN_USER_KEY } from '@/api/admin'

const { t } = useI18n()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: t('admin.login.usernameRequired'), trigger: 'blur' },
    { min: 3, max: 64, message: t('admin.login.usernameLength'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('admin.login.passwordRequired'), trigger: 'blur' },
    { min: 8, max: 64, message: t('admin.login.passwordLength'), trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const result: any = await adminAuthApi.login({
        username: form.username,
        password: form.password
      })
      // 持久化管理员 token 与用户信息（独立于开发者）
      localStorage.setItem(ADMIN_TOKEN_KEY, result.token)
      localStorage.setItem(
        ADMIN_USER_KEY,
        JSON.stringify({
          userId: result.userId,
          role: result.role,
          username: result.username,
          nickname: result.nickname
        })
      )
      ElMessage.success(t('admin.login.loginSuccess'))
      router.push('/admin/ticket')
    } catch {
      // 响应拦截器已统一提示
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--jicek-bg-secondary);
}

.login-card {
  width: 400px;
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  box-shadow: var(--jicek-shadow-md);
  padding: 40px 32px;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  .logo-text {
    font-size: 26px;
    font-weight: 700;
    color: var(--jicek-primary);
    letter-spacing: 3px;
  }

  .logo-sub {
    font-size: 13px;
    color: var(--jicek-text-secondary);
    margin-top: 6px;
  }
}

.login-btn {
  width: 100%;
  height: 40px;
  font-size: 15px;
  letter-spacing: 4px;
}

.login-tip {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--jicek-text-placeholder);
}
</style>
