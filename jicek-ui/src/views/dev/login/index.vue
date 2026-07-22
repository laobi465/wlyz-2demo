<!--
  极策k网络验证 - 开发者登录页
  作者: 极策k  日期: 2026-07-22

  v0.7.0 鉴权框架：
   - 表单字段：租户ID + 用户名 + 密码（密码 ≥ 8 位）
   - 登录成功后存 token + 用户信息到 localStorage，跳转 /dashboard
   - 遵循 docs/UI-DESIGN.md 现代简约风格规范（无渐变、无 emoji）
-->
<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-text">{{ t('login.title') }}</div>
        <div class="logo-sub">{{ t('login.subtitle') }}</div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item :label="t('login.tenantId')" prop="tenantId">
          <el-input
            v-model.number="form.tenantId"
            :placeholder="t('login.tenantIdPlaceholder')"
            type="number"
            clearable
          />
        </el-form-item>

        <el-form-item :label="t('login.username')" prop="username">
          <el-input
            v-model="form.username"
            :placeholder="t('login.usernamePlaceholder')"
            clearable
          />
        </el-form-item>

        <el-form-item :label="t('login.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('login.passwordPlaceholder')"
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
            {{ loading ? t('login.logging') : t('login.login') }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">
        默认账号：dev / dev@123（tenantId=1）
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api'
import { TOKEN_KEY, USER_KEY } from '@/api/request'

const { t } = useI18n()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  tenantId: 1,
  username: '',
  password: ''
})

const rules: FormRules = {
  tenantId: [
    { required: true, message: t('login.tenantIdRequired'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!Number.isInteger(Number(value)) || Number(value) <= 0) {
          callback(new Error(t('login.tenantIdInvalid')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  username: [
    { required: true, message: t('login.usernameRequired'), trigger: 'blur' },
    { min: 3, max: 64, message: t('login.usernameLength'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('login.passwordRequired'), trigger: 'blur' },
    { min: 8, max: 64, message: t('login.passwordLength'), trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const result: any = await authApi.devLogin({
        tenantId: Number(form.tenantId),
        username: form.username,
        password: form.password
      })
      // 持久化 token 与用户信息
      localStorage.setItem(TOKEN_KEY, result.token)
      localStorage.setItem(
        USER_KEY,
        JSON.stringify({
          userId: result.userId,
          role: result.role,
          tenantId: result.tenantId,
          username: result.username,
          nickname: result.nickname
        })
      )
      ElMessage.success(t('login.loginSuccess'))
      router.push('/dashboard')
    } catch (e) {
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
    font-size: 28px;
    font-weight: 700;
    color: var(--jicek-primary);
    letter-spacing: 4px;
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
