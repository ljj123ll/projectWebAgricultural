import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // 根据后端接口设计，Base URL本身包含 /api，所以可能不需要 rewrite，或者需要根据实际情况调整。
        // 根据 "Base URL: http://localhost:8080/api"，如果前端请求 /api/user/login，会被代理到 http://localhost:8080/api/user/login
        // 所以不需要 rewrite
      }
    }
  }
})
