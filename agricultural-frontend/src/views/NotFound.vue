<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <el-icon class="error-icon" :size="120" color="#909399"><Warning /></el-icon>
      <h1>404</h1>
      <p class="subtitle">页面不存在</p>
      <p class="description">抱歉，您访问的页面不存在或已被移除</p>
      <div class="actions">
        <el-button type="primary" size="large" @click="goBack">返回上一页</el-button>
        <el-button size="large" @click="goHome">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Warning } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}

const goHome = () => {
  router.push('/')
}
</script>

<style scoped lang="scss">
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.not-found-content {
  text-align: center;
  padding: 40px;

  .error-icon {
    margin-bottom: 24px;
  }

  h1 {
    font-size: 80px;
    font-weight: 700;
    color: #333;
    margin: 0 0 8px 0;
    line-height: 1;
  }

  .subtitle {
    font-size: 24px;
    color: #666;
    margin: 0 0 16px 0;
  }

  .description {
    font-size: 16px;
    color: #999;
    margin: 0 0 32px 0;
  }

  .actions {
    display: flex;
    gap: 16px;
    justify-content: center;
  }
}
</style>
