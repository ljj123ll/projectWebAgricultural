<template>
  <div class="news-page">
    <div class="page-header">
      <el-button link @click="$router.back()" class="back-btn" style="margin-right: 10px; font-size: 20px;">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">助农新闻</h1>
    </div>

    <div class="category-tabs">
      <div
        v-for="cat in categories"
        :key="cat.id"
        class="tab-item"
        :class="{ active: currentCategory === cat.id }"
        @click="changeCategory(cat.id)"
      >
        {{ cat.categoryName }}
      </div>
    </div>

    <div v-loading="loading" class="news-list">
      <div
        v-for="news in newsList"
        :key="news.id"
        class="news-item"
        @click="goToDetail(news.id)"
      >
        <img :src="getFullImageUrl(news.coverImg)" :alt="news.title" class="news-image" />
        <div class="news-content">
          <span class="news-category">{{ getCategoryName(news.categoryId) }}</span>
          <h3 class="news-title">{{ news.title }}</h3>
          <p class="news-time">{{ formatDate(news.createTime) }}</p>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && newsList.length === 0" description="暂无新闻" />
    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="loadNews"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';
import { listNews, listNewsCategories } from '@/apis/user';
import { getFullImageUrl } from '@/utils/image';
import type { News, NewsCategory } from '@/types';

const router = useRouter();
const currentCategory = ref(0);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const categories = ref<Array<{ id: number; categoryName: string }>>([
  { id: 0, categoryName: '全部' }
]);
const newsList = ref<News[]>([]);

const loadCategories = async () => {
  const res = await listNewsCategories();
  categories.value = [{ id: 0, categoryName: '全部' }, ...(res || []).map((item: NewsCategory) => ({ id: item.id, categoryName: item.categoryName }))];
};

const loadNews = async () => {
  loading.value = true;
  try {
    const res = await listNews({
      categoryId: currentCategory.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    });
    newsList.value = res?.list || [];
    total.value = res?.total || 0;
  } finally {
    loading.value = false;
  }
};

const changeCategory = (id: number) => {
  currentCategory.value = id;
  currentPage.value = 1;
  loadNews();
};

const getCategoryName = (categoryId: number) => {
  return categories.value.find(item => item.id === categoryId)?.categoryName || '其他';
};

const formatDate = (date?: string) => {
  if (!date) return '';
  return date.substring(0, 10);
};

const goToDetail = (id: number) => {
  router.push(`/news/${id}`);
};

onMounted(async () => {
  await loadCategories();
  await loadNews();
});
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

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
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
