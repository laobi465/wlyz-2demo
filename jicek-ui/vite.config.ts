/**
 * 极策k网络验证 - Vite 构建配置
 * 作者: 极策k  日期: 2026-07-21
 *
 * 配置项：
 * 1. Vue + Element Plus 自动按需引入
 * 2. @ 路径别名指向 src
 * 3. 开发服务器代理 /api 和 /pay 到后端 8080
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/pay': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
