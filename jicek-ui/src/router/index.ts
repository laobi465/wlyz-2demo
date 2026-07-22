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
        path: 'card-type',
        name: 'CardType',
        component: () => import('@/views/dev/card-type/index.vue'),
        meta: { title: '卡类管理', icon: 'Files' }
      },
      {
        path: 'device',
        name: 'Device',
        component: () => import('@/views/dev/device/index.vue'),
        meta: { title: '设备管理', icon: 'Monitor' }
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
      },
      {
        path: 'agent',
        name: 'Agent',
        component: () => import('@/views/dev/agent/index.vue'),
        meta: { title: '代理列表', icon: 'User' }
      },
      {
        path: 'withdraw',
        name: 'Withdraw',
        component: () => import('@/views/dev/withdraw/index.vue'),
        meta: { title: '提现审核', icon: 'Wallet' }
      },
      {
        path: 'cloud-func',
        name: 'CloudFunc',
        component: () => import('@/views/dev/cloud-func/index.vue'),
        meta: { title: '云函数', icon: 'Cpu' }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/dev/stats/index.vue'),
        meta: { title: '数据统计', icon: 'TrendCharts' }
      },
      {
        path: 'deploy',
        name: 'Deploy',
        component: () => import('@/views/dev/deploy/index.vue'),
        meta: { title: '部署管理', icon: 'Refresh' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
