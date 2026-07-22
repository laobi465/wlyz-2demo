/**
 * 极策k网络验证 - 国际化配置
 * 作者: 极策k  日期: 2026-07-22
 *
 * 职责：vue-i18n 9.x 实例创建 + 语言切换 + 持久化
 */
import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'

const LOCALE_KEY = 'jicek_locale'

/** 获取初始语言（localStorage 优先，其次浏览器语言，默认中文） */
function getInitialLocale(): string {
  const saved = localStorage.getItem(LOCALE_KEY)
  if (saved && (saved === 'zh-CN' || saved === 'en-US')) {
    return saved
  }
  const browserLang = navigator.language
  return browserLang.startsWith('en') ? 'en-US' : 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

/** 切换语言并持久化 */
export function setLocale(locale: 'zh-CN' | 'en-US') {
  i18n.global.locale.value = locale
  localStorage.setItem(LOCALE_KEY, locale)
  // 同步切换 Element Plus 语言包
  document.documentElement.lang = locale
}

export default i18n
