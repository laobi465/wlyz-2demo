/**
 * 极策k网络验证 - 前端入口
 * 作者: 极策k  日期: 2026-07-21
 *
 * 职责：创建 Vue 实例，注册 vue-i18n + Element Plus（语言包随 locale 切换）+ 图标 + Pinia + 路由
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import enUS from 'element-plus/es/locale/lang/en'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import i18n from './i18n'
import './styles/jicek.scss'

const app = createApp(App)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(i18n)
// Element Plus 语言包随 i18n locale 切换（切换语言时 LangSwitch 会触发 location.reload 同步）
const epLocale = i18n.global.locale.value === 'en-US' ? enUS : zhCn
app.use(ElementPlus, { locale: epLocale })
app.mount('#app')
