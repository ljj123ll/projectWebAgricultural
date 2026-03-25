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
import { useRoute } from 'vue-router';
import { View, ArrowLeft } from '@element-plus/icons-vue';
import { getNewsDetail } from '@/apis/user';
import { getFullImageUrl } from '@/utils/image';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/preview.css';

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
  background: linear-gradient(180deg, #f3f8f1 0%, #f6f8fc 28%, #f6f8fc 100%);
  min-height: 100vh;
  padding: 20px 0 28px;
}

.main-container {
  width: min(1120px, 100%);
  margin: 0 auto;
  padding: 0 clamp(12px, 2.5vw, 24px);
  box-sizing: border-box;
}

.back-btn {
  margin-bottom: 14px;
  font-size: 15px;
  color: #5a6472;
  
  &:hover {
    color: #4caa45;
  }
}

.article-container {
  background: rgba(255, 255, 255, 0.96);
  border-radius: 18px;
  padding: clamp(20px, 3.2vw, 40px);
  min-height: 500px;
  border: 1px solid #e4ece1;
  box-shadow: 0 14px 38px rgba(31, 55, 37, 0.08);
}

.article-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf2ea;
  
  .news-title {
    font-size: clamp(28px, 3.8vw, 40px);
    font-weight: 600;
    color: #1f2f28;
    line-height: 1.35;
    margin: 0 0 16px 0;
  }
  
  .meta-info {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 14px 20px;
    color: #7a8493;
    font-size: 14px;
    
    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    
    .author {
      color: #445064;
      font-weight: 500;
      
      .author-avatar {
        background: #dbe7d8;
        color: #fff;
      }
    }

    .category-tag {
      border-color: #d7ead2;
      color: #469c3f;
      background: #f2fbef;
      border-radius: 999px;
      padding: 0 10px;
    }
  }
}

.article-cover {
  margin: 0 0 24px;
  max-height: 520px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  background: linear-gradient(180deg, #f2f6f4 0%, #f7fbf9 100%);
  border-radius: 14px;
  border: 1px solid #e8efe7;
  
  img {
    width: 100%;
    max-height: 520px;
    object-fit: cover;
  }
}

.article-content {
  color: #253244;

  :deep(.md-editor-preview-wrapper) {
    padding: 0;
  }

  :deep(.md-editor-preview) {
    line-height: 1.85;
    color: #253244;
    font-size: 16px;
  }

  :deep(h1, h2, h3, h4) {
    color: #1f2f28;
    margin-top: 1.4em;
  }

  :deep(p) {
    margin: 1em 0;
    word-break: break-word;
  }

  :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 10px;
  }

  :deep(table) {
    display: block;
    width: 100%;
    overflow-x: auto;
  }

  :deep(pre) {
    overflow-x: auto;
    border-radius: 10px;
  }
}

@media (max-width: 992px) {
  .article-container {
    border-radius: 14px;
  }

  .article-header .meta-info {
    gap: 10px 14px;
    font-size: 13px;
  }
}

@media (max-width: 768px) {
  .news-detail-page {
    padding: 10px 0 14px;
  }

  .main-container {
    padding: 0 10px;
  }
  
  .article-container {
    padding: 16px 14px;
    border-radius: 12px;
    min-height: 320px;
  }
  
  .article-header {
    margin-bottom: 14px;
    padding-bottom: 12px;

    .news-title {
      font-size: 24px;
      margin-bottom: 10px;
    }
    
    .meta-info {
      gap: 12px;
      font-size: 13px;
    }
  }
  
  .article-cover {
    margin: 0 0 14px;
    border-radius: 10px;

    img {
      max-height: 260px;
    }
  }

  .article-content {
    :deep(.md-editor-preview) {
      font-size: 15px;
      line-height: 1.75;
    }
  }
}
</style>
