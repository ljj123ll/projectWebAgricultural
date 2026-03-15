<template>
  <div class="news-page">
    <div class="page-header">
      <el-button link @click="$router.back()" class="back-btn" style="margin-right: 10px; font-size: 20px;">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">助农新闻</h1>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="tab-item"
        :class="{ active: currentCategory === cat.id }"
        @click="currentCategory = cat.id"
      >
        {{ cat.name }}
      </div>
    </div>

    <!-- 新闻列表 -->
    <div class="news-list">
      <div
        v-for="news in filteredNews"
        :key="news.id"
        class="news-item"
        @click="goToDetail(news.id)"
      >
        <img :src="news.coverImg" :alt="news.title" class="news-image" />
        <div class="news-content">
          <span class="news-category">{{ getCategoryName(news.categoryId) }}</span>
          <h3 class="news-title">{{ news.title }}</h3>
          <p class="news-time">{{ formatDate(news.createTime) }}</p>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="filteredNews.length === 0" description="暂无新闻" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';

const router = useRouter();
const currentCategory = ref(0);

const categories = ref([
  { id: 0, name: '全部' },
  { id: 1, name: '农业政策' },
  { id: 2, name: '扶贫案例' },
  { id: 3, name: '产地动态' }
]);

const newsList = ref([
  {
    id: 1,
    title: '2024年农业补贴政策解读',
    categoryId: 1,
    coverImg: 'https://via.placeholder.com/200x150/409eff/fff?text=政策',
    createTime: '2024-03-01'
  },
  {
    id: 2,
    title: '助农电商助力乡村振兴典型案例',
    categoryId: 2,
    coverImg: 'https://via.placeholder.com/200x150/67c23a/fff?text=案例',
    createTime: '2024-02-28'
  },
  {
    id: 3,
    title: '春季农产品产销对接会成功举办',
    categoryId: 3,
    coverImg: 'https://via.placeholder.com/200x150/e6a23c/fff?text=动态',
    createTime: '2024-02-25'
  }
]);

const filteredNews = computed(() => {
  if (currentCategory.value === 0) {
    return newsList.value;
  }
  return newsList.value.filter(news => news.categoryId === currentCategory.value);
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

const goToDetail = (id: number) => {
  router.push(`/news/${id}`);
};
</script>

<style scoped lang="scss">
.news-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  margin: 0;
}

.category-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;

  .tab-item {
    padding: 8px 20px;
    font-size: 15px;
    color: #606266;
    cursor: pointer;
    border-radius: 20px;
    transition: all 0.2s;
    background: #f5f7fa;

    &:hover {
      background: #e4e7ed;
    }

    &.active {
      background: #67c23a;
      color: #fff;
    }
  }
}

.news-list {
  .news-item {
    display: flex;
    gap: 16px;
    padding: 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    }

    .news-image {
      width: 120px;
      height: 90px;
      border-radius: 8px;
      object-fit: cover;
    }

    .news-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .news-category {
        display: inline-block;
        background: #67c23a;
        color: #fff;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 12px;
        width: fit-content;
      }

      .news-title {
        margin: 8px 0;
        font-size: 16px;
        font-weight: 500;
      }

      .news-time {
        margin: 0;
        font-size: 13px;
        color: #909399;
      }
    }
  }
}

@media (max-width: 768px) {
  .news-page {
    padding: 16px;
  }

  .news-item {
    .news-image {
      width: 100px !important;
      height: 75px !important;
    }
  }
}
</style>
