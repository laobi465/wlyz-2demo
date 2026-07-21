/**
 * 极策k 路由配置
 * 作者: 极策k  日期: 2026-07-21
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const Layout = () => import('@/layout/DevLayout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dev/dashboard/index.vue'),
        meta: { title: '控制台', icon: 'Odometer' }
      },
      {
        path: 'card-key-gen',
        name: 'CardKeyGen',
        component: () => import('@/views/dev/card-key-gen/index.vue'),
        meta: { title: '卡密生成', icon: 'Key' }
      },
      {
        path: 'card-key-list',
        name: 'CardKeyList',
        component: () => import('@/views/dev/card-key-list/index.vue'),
        meta: { title: '卡密查询', icon: 'Search' }
      },
      {
        path: 'pay-config',
        name: 'PayConfig',
        component: () => import('@/views/dev/pay-config/index.vue'),
        meta: { title: '支付配置', icon: 'Setting' }
      },
      {
        path: 'pay-order',
        name: 'PayOrder',
        component: () => import('@/views/dev/pay-order/index.vue'),
        meta: { title: '资金流水', icon: 'Money' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
