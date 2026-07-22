<!--
  极策k网络验证 - H5 代理注册（公开页）
  作者: 极策k  日期: 2026-07-22

  v0.13.0 功能2：代理注册邀请码
   - 表单：appKey + inviteCode + username + password + realName? + contact?
   - 公开接口，无需鉴权
   - 提交前 ElMessageBox.confirm 二次确认（注册不可逆）
   - 成功 ElNotification 持久通知
   - 失败 ElMessage.error
   - 遵循 docs/UI-DESIGN.md（无渐变、无 emoji）
-->
<template>
  <div class="h5-agent-register">
    <div class="h5-register-header">
      <div class="h5-title">代理注册</div>
      <div class="h5-sub">填写邀请码与账号信息完成注册</div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="AppKey" prop="appKey">
        <el-input
          v-model="form.appKey"
          placeholder="请输入 32 位 AppKey"
          clearable
          maxlength="32"
        />
      </el-form-item>

      <el-form-item label="邀请码" prop="inviteCode">
        <el-input
          v-model="form.inviteCode"
          placeholder="请输入 8 位邀请码"
          clearable
          maxlength="8"
        />
      </el-form-item>

      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="3-64 字符"
          clearable
          maxlength="64"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="6-64 字符"
          show-password
          clearable
          maxlength="64"
        />
      </el-form-item>

      <el-form-item label="真实姓名（选填）" prop="realName">
        <el-input
          v-model="form.realName"
          placeholder="选填，最长 64 字符"
          clearable
          maxlength="64"
        />
      </el-form-item>

      <el-form-item label="联系方式（选填）" prop="contact">
        <el-input
          v-model="form.contact"
          placeholder="选填，最长 128 字符"
          clearable
          maxlength="128"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="h5-submit-btn"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          提交注册
        </el-button>
      </el-form-item>
    </el-form>

    <div class="h5-tip">注册成功后，请使用账号密码登录代理后台。</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  ElMessageBox,
  ElNotification,
  type FormInstance,
  type FormRules
} from 'element-plus'
import { h5Api } from '@/api'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const form = reactive({
  appKey: '',
  inviteCode: '',
  username: '',
  password: '',
  realName: '',
  contact: ''
})

const rules: FormRules = {
  appKey: [
    { required: true, message: '请输入 AppKey', trigger: 'blur' },
    { len: 32, message: 'AppKey 长度必须为 32 字符', trigger: 'blur' }
  ],
  inviteCode: [
    { required: true, message: '请输入邀请码', trigger: 'blur' },
    { len: 8, message: '邀请码长度必须为 8 字符', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名长度 3-64 字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 字符', trigger: 'blur' }
  ],
  realName: [{ max: 64, message: '真实姓名最长 64 字符', trigger: 'blur' }],
  contact: [{ max: 128, message: '联系方式最长 128 字符', trigger: 'blur' }]
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    // 注册是不可逆操作，二次确认
    try {
      await ElMessageBox.confirm(
        '注册操作不可撤销，请确认填写信息无误。是否继续？',
        '注册确认',
        {
          confirmButtonText: '确认注册',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
    submitLoading.value = true
    try {
      await h5Api.agentRegister({
        appKey: form.appKey.trim(),
        inviteCode: form.inviteCode.trim(),
        username: form.username.trim(),
        password: form.password,
        realName: form.realName.trim() || undefined,
        contact: form.contact.trim() || undefined
      })
      ElNotification({
        title: '注册成功',
        message: '请使用账号密码登录代理后台',
        type: 'success',
        duration: 0
      })
      // 跳回登录页
      setTimeout(() => {
        router.push('/h5/login')
      }, 800)
    } catch {
      // 响应拦截器已统一提示
    } finally {
      submitLoading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.h5-agent-register {
  display: flex;
  flex-direction: column;
}

.h5-register-header {
  text-align: center;
  margin-bottom: 24px;

  .h5-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 28px;
  }

  .h5-sub {
    font-size: 13px;
    color: var(--jicek-text-secondary);
    margin-top: 4px;
    line-height: 22px;
  }
}

.h5-submit-btn {
  width: 100%;
  height: 40px;
  font-size: 15px;
  letter-spacing: 2px;
}

.h5-tip {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: var(--jicek-text-placeholder);
  line-height: 18px;
  padding: 0 8px;
}
</style>
