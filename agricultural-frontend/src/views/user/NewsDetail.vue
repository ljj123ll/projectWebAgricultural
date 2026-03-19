<template>
  <div class="news-detail-page">
    <div class="main-container">
      <el-button link @click="$router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>

      <div class="article-container" v-loading="loading">
        <template v-if="news">
          <div class="article-header">
            <h1 class="news-title">{{ news.title }}</h1>
            <div class="meta-info">
              <span class="meta-item author" v-if="news.author">
                <el-avatar :size="24" icon="UserFilled" class="author-avatar" />
                {{ news.author }}
              </span>
              <span class="meta-item time">{{ formatDate(news.createTime) }}</span>
              <span class="meta-item views">
                <el-icon><View /></el-icon> {{ news.viewCount || 0 }} 阅读
              </span>
              <el-tag size="small" effect="plain" class="category-tag">
                {{ news.categoryName }}
              </el-tag>
            </div>
          </div>

          <div class="article-cover" v-if="news.coverImg">
            <img :src="getFullImageUrl(news.coverImg)" alt="cover" />
          </div>
          
          <div class="article-content">
             <MdPreview :editorId="id" :modelValue="news.content" />
          </div>
        </template>
        <el-empty v-else description="未找到相关资讯" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { View, ArrowLeft, UserFilled } from '@element-plus/icons-vue';
import { getNewsDetail } from '@/apis/user';
import { getFullImageUrl } from '@/utils/image';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';

const router = useRouter();
const route = useRoute();
const id = 'preview-only';
const loading = ref(false);
const news = ref<any>(null);

const formatDate = (date?: string) => {
  if (!date) return '';
  return date.replace('T', ' ').substring(0, 16);
};

onMounted(async () => {
  const newsId = Number(route.params.id);
  if (newsId) {
    loading.value = true;
    try {
      const res = await getNewsDetail(newsId);
      if (res) {
        news.value = res;
      }
    } catch (error) {
      console.error(error);
    } finally {
      loading.value = false;
    }
  }
});
</script>

<style scoped lang="scss">
.news-detail-page {
  background-color: #f4f5f5;
  min-height: 100vh;
  padding-top: 20px;
}

.main-container {
  max-width: 800px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 20px;
  font-size: 16px;
  color: #606266;
  
  &:hover {
    color: var(--el-color-primary);
  }
}

.article-container {
  background: #fff;
  border-radius: 4px;
  padding: 32px;
  min-height: 500px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.article-header {
  margin-bottom: 24px;
  
  .news-title {
    font-size: 32px;
    font-weight: 600;
    color: #252933;
    line-height: 1.31;
    margin: 0 0 20px 0;
  }
  
  .meta-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 20px;
    color: #8a919f;
    font-size: 14px;
    
    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    .author {
      color: #515767;
      font-weight: 500;
      
      .author-avatar {
        background: #e4e6eb;
        color: #fff;
      }
    }
  }
}

.article-cover {
  margin: 0 -32px 32px;
  max-height: 400px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  background: #f8f9fa;
  
  img {
    max-width: 100%;
    max-height: 400px;
    object-fit: contain;
  }
}

.article-content {
  :deep(.md-editor-preview-wrapper) {
    padding: 0;
  }
}

@media (max-width: 768px) {
  .news-detail-page {
    padding: 0;
  }
  
  .article-container {
    padding: 20px 16px;
    border-radius: 0;
  }
  
  .article-header {
    .news-title {
      font-size: 24px;
    }
    
    .meta-info {
      gap: 12px;
      font-size: 13px;
    }
  }
  
  .article-cover {
    margin: 0 -16px 20px;
  }
}
</style>
