<template>
  <div class="news-detail-page">
    <el-button link @click="router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <article class="news-article">
      <span class="news-category">{{ getCategoryName(news.categoryId) }}</span>
      <h1 class="news-title">{{ news.title }}</h1>
      <p class="news-time">发布时间：{{ formatDate(news.createTime) }}</p>
      
      <img v-if="news.coverImg" :src="news.coverImg" :alt="news.title" class="news-cover" />
      
      <div class="news-content" v-html="contentHtml"></div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

const news = ref({
  id: Number(route.params.id),
  title: '2024年农业补贴政策解读',
  categoryId: 1,
  coverImg: 'https://via.placeholder.com/800x400/409eff/fff?text=新闻详情',
  createTime: '2024-03-01',
  content: '# 补贴政策要点\n\n- **补贴对象**：种植大户、家庭农场、合作社\n- **补贴范围**：种植补贴、农机购置补贴、农业保险补贴\n- **申请流程**：线上提交材料，线下核验\n\n## 申报材料\n\n1. 身份证明\n2. 经营主体证明\n3. 土地流转或承包证明\n\n## 温馨提示\n\n政策解读以当地农业部门通知为准，建议提前准备材料。'
});

const getCategoryName = (categoryId: number) => {
  const map: Record<number, string> = {
    1: '农业政策',
    2: '扶贫案例',
    3: '产地动态'
  };
  return map[categoryId] || '其他';
};

const formatDate = (date?: string) => {
  if (!date) return '';
  return date.substring(0, 10);
};

const renderMarkdown = (markdown: string) => {
  const escaped = markdown
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  let html = escaped
    .replace(/^### (.*)$/gm, '<h3>$1</h3>')
    .replace(/^## (.*)$/gm, '<h2>$1</h2>')
    .replace(/^# (.*)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/^\d+\.\s+(.*)$/gm, '<li>$1</li>')
    .replace(/^- (.*)$/gm, '<li>$1</li>');
  html = html.replace(/(<li>.*<\/li>\n?)+/g, match => `<ul>${match}</ul>`);
  html = html.replace(/\n{2,}/g, '</p><p>').replace(/\n/g, '<br>');
  return `<p>${html}</p>`;
};

const contentHtml = computed(() => renderMarkdown(news.value.content || ''));

onMounted(() => {
  // 根据ID加载新闻详情
  console.log('加载新闻ID:', route.params.id);
});
</script>

<style scoped lang="scss">
.news-detail-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;

  .back-btn {
    margin-bottom: 16px;
    font-size: 16px;
  }
}

.news-article {
  max-width: 800px;
  margin: 0 auto;

  .news-category {
    display: inline-block;
    background: #67c23a;
    color: #fff;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 14px;
  }

  .news-title {
    font-size: 28px;
    margin: 16px 0;
    line-height: 1.4;
  }

  .news-time {
    color: #909399;
    font-size: 14px;
    margin-bottom: 24px;
  }

  .news-cover {
    width: 100%;
    max-height: 400px;
    object-fit: cover;
    border-radius: 8px;
    margin-bottom: 24px;
  }

  .news-content {
    font-size: 16px;
    line-height: 1.8;
    color: #303133;

    :deep(h1) {
      font-size: 22px;
      margin: 16px 0 12px;
    }

    :deep(h2) {
      font-size: 20px;
      margin: 16px 0 12px;
    }

    :deep(h3) {
      font-size: 18px;
      margin: 16px 0 12px;
    }

    :deep(ul) {
      padding-left: 20px;
      margin: 8px 0 16px;
    }

    :deep(code) {
      background: #f5f7fa;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: inherit;
    }
  }
}

@media (max-width: 768px) {
  .news-detail-page {
    padding: 16px;
  }

  .news-article {
    .news-title {
      font-size: 22px;
    }
  }
}
</style>
