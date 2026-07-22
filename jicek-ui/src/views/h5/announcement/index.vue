<!--
  极策k网络验证 - H5 系统公告
  作者: 极策k  日期: 2026-07-22

  v0.13.0 功能1：H5 终端用户公告列表
   - 调 h5Api.announcement() 加载公告（X-H5-Token 鉴权）
   - 按发布时间倒序，置顶（pinned=1）排最前 + 红色置顶徽章
   - type 1 通知条 / 2 弹窗 / 3 置顶横幅（仅展示标签）
   - 标题 + 内容（保留换行）+ 发布时间
   - 空状态：「暂无公告」
   - 无 token 跳 /h5/login
   - 遵循 docs/UI-DESIGN.md（无渐变、无 emoji）
-->
<template>
  <div class="h5-announcement">
    <div v-if="loading" class="h5-skeleton-list">
      <el-skeleton :rows="4" animated />
      <el-skeleton :rows="4" animated style="margin-top: 16px" />
    </div>

    <template v-else>
      <div v-if="!list.length" class="h5-empty-wrap">
        <el-empty description="暂无公告" />
      </div>

      <div v-else class="h5-announcement-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="h5-announcement-item"
        >
          <div class="h5-announcement-head">
            <div class="h5-announcement-tags">
              <span v-if="item.pinned === 1" class="jicek-tag jicek-tag-danger">置顶</span>
              <span class="jicek-tag" :class="typeTagClass(item.type)">{{ typeText(item.type) }}</span>
            </div>
            <div class="h5-announcement-time">{{ item.publishTime || '未发布' }}</div>
          </div>
          <div class="h5-announcement-title">{{ item.title }}</div>
          <div class="h5-announcement-content">{{ item.content }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { h5Api, H5_TOKEN_KEY } from '@/api'

interface AnnouncementItem {
  id: number
  title: string
  content: string
  type: number
  pinned: number
  publishTime?: string
  minVersion?: string
  maxVersion?: string
}

const router = useRouter()
const loading = ref(true)
const list = ref<AnnouncementItem[]>([])

function typeText(t: number): string {
  const map: Record<number, string> = { 1: '通知条', 2: '弹窗', 3: '置顶横幅' }
  return map[t] || '未知'
}

function typeTagClass(t: number): string {
  const map: Record<number, string> = {
    1: 'jicek-tag-info',
    2: 'jicek-tag-warning',
    3: 'jicek-tag-danger'
  }
  return map[t] || 'jicek-tag-pending'
}

async function loadData() {
  if (!localStorage.getItem(H5_TOKEN_KEY)) {
    router.replace({ path: '/h5/login', query: { redirect: '/h5/announcement' } })
    return
  }
  loading.value = true
  try {
    const result: any = await h5Api.announcement()
    const arr: AnnouncementItem[] = Array.isArray(result) ? result : []
    // 置顶排前 + 发布时间倒序
    arr.sort((a, b) => {
      if ((b.pinned || 0) !== (a.pinned || 0)) {
        return (b.pinned || 0) - (a.pinned || 0)
      }
      const ta = a.publishTime ? new Date(a.publishTime).getTime() : 0
      const tb = b.publishTime ? new Date(b.publishTime).getTime() : 0
      return tb - ta
    })
    list.value = arr
  } catch (e: any) {
    const msg = e?.message || ''
    if (msg.includes('登录') || msg.includes('token') || msg.includes('过期')) {
      localStorage.removeItem(H5_TOKEN_KEY)
      router.replace({ path: '/h5/login', query: { redirect: '/h5/announcement' } })
      return
    }
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.h5-announcement {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.h5-skeleton-list {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 16px;
}

.h5-empty-wrap {
  padding: 40px 0;
}

.h5-announcement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.h5-announcement-item {
  background: var(--jicek-bg-primary);
  border: 1px solid var(--jicek-border);
  border-radius: var(--jicek-radius-lg);
  padding: 14px 16px;
  box-shadow: var(--jicek-shadow-sm);

  .h5-announcement-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }

  .h5-announcement-tags {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  .h5-announcement-time {
    font-size: 12px;
    color: var(--jicek-text-placeholder);
    line-height: 18px;
  }

  .h5-announcement-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--jicek-text-primary);
    line-height: 24px;
    margin-bottom: 6px;
  }

  .h5-announcement-content {
    font-size: 14px;
    color: var(--jicek-text-primary);
    line-height: 22px;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
</style>
