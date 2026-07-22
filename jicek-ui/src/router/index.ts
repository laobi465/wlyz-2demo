/**
 * 极策k 路由配置
 * 作者: 极策k  日期: 2026-07-21
 *
 * v0.7.0 鉴权：
 *  - 新增 /login 路由（无需鉴权，独立布局）
 *  - beforeEach 守卫：无 token 跳 /login；已登录访问 /login 跳 /dashboard
 *
 * v0.15.0 管理员后台：
 *  - 新增 /admin/login（公开）+ /admin（受保护，AdminLayout）路由
 *  - 管理员 token 独立存储（jicek_admin_token），与开发者 jicek_token 隔离
 *  - 守卫按路径前缀分流：/admin/* 校验管理员 token，其余校验开发者 token
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { TOKEN_KEY } from '@/api/request'
import { ADMIN_TOKEN_KEY } from '@/api/admin'

const Layout = () => import('@/layout/DevLayout.vue')
const AdminLayout = () => import('@/layout/AdminLayout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/dev/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/login/index.vue'),
    meta: { title: '管理员登录', public: true, admin: true }
  },
  {
    path: '/h5',
    name: 'H5Root',
    component: () => import('@/views/h5/H5Layout.vue'),
    meta: { title: 'H5 终端用户', public: true },
    children: [
      {
        path: 'login',
        name: 'H5Login',
        component: () => import('@/views/h5/login/index.vue'),
        meta: { title: '卡密登录', public: true }
      },
      {
        path: 'my-card',
        name: 'H5MyCard',
        component: () => import('@/views/h5/my-card/index.vue'),
        meta: { title: '我的卡密', public: true, h5Auth: true }
      },
      {
        path: 'announcement',
        name: 'H5Announcement',
        component: () => import('@/views/h5/announcement/index.vue'),
        meta: { title: '系统公告', public: true, h5Auth: true }
      },
      {
        path: 'agent/register',
        name: 'H5AgentRegister',
        component: () => import('@/views/h5/agent/register.vue'),
        meta: { title: '代理注册', public: true }
      },
      {
        path: 'shop',
        name: 'H5ShopHome',
        component: () => import('@/views/h5/shop/index.vue'),
        meta: { title: '购卡店铺', public: true }
      },
      {
        path: 'shop/order',
        name: 'H5ShopOrder',
        component: () => import('@/views/h5/shop/order.vue'),
        meta: { title: '确认订单', public: true, h5Auth: true }
      }
    ]
  },
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
        path: 'software',
        name: 'Software',
        component: () => import('@/views/dev/software/index.vue'),
        meta: { title: '软件管理', icon: 'Cpu' }
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
        path: 'end-user',
        name: 'EndUser',
        component: () => import('@/views/dev/end-user/index.vue'),
        meta: { title: '终端用户', icon: 'UserFilled' }
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
      },
      {
        path: 'ticket',
        name: 'Ticket',
        component: () => import('@/views/dev/ticket/index.vue'),
        meta: { title: '工单管理', icon: 'Service' }
      },
      {
        path: 'announcement',
        name: 'Announcement',
        component: () => import('@/views/dev/announcement/index.vue'),
        meta: { title: '远程公告', icon: 'Bell' }
      },
      {
        path: 'update-package',
        name: 'UpdatePackage',
        component: () => import('@/views/dev/update-package/index.vue'),
        meta: { title: '更新包', icon: 'Upload' }
      },
      {
        path: 'integration-doc',
        name: 'IntegrationDoc',
        component: () => import('@/views/dev/integration-doc/index.vue'),
        meta: { title: '对接文档', icon: 'Document' }
      },
      {
        path: 'shop',
        name: 'Shop',
        component: () => import('@/views/dev/shop/index.vue'),
        meta: { title: '内嵌卡网', icon: 'Shop' }
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/ticket',
    children: [
      {
        path: 'ticket',
        name: 'AdminTicket',
        component: () => import('@/views/admin/ticket/index.vue'),
        meta: { title: '工单管理', icon: 'Service', admin: true }
      },
      {
        path: 'dev-user',
        name: 'AdminDevUser',
        component: () => import('@/views/admin/dev-user/index.vue'),
        meta: { title: '开发者管理', icon: 'User', admin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局守卫：
//  - /admin/* 路由校验管理员 token（jicek_admin_token），缺失跳 /admin/login
//  - 其余受保护页面校验开发者 token（jicek_token），缺失跳 /login
router.beforeEach((to, _from, next) => {
  const isAdminRoute = to.path.startsWith('/admin')
  const isAdminLogin = to.path === '/admin/login'

  if (isAdminRoute) {
    const adminToken = localStorage.getItem(ADMIN_TOKEN_KEY)
    if (isAdminLogin) {
      // 管理员登录页：已登录跳工单管理
      if (adminToken) {
        next('/admin/ticket')
      } else {
        next()
      }
      return
    }
    // 受保护管理员页面：无 token 跳管理员登录
    if (!adminToken) {
      next({ path: '/admin/login', query: { redirect: to.fullPath } })
      return
    }
    next()
    return
  }

  // 开发者/H5 公开页
  if (to.meta.public) {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  // 受保护开发者页面：无 token 跳登录
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
