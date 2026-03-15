<template>
  <div class="news-container">
    <el-card>
      <template #header>
        <div class="header">
          <span>助农新闻</span>
          <el-select v-model="categoryId" placeholder="分类" clearable @change="loadNews">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </div>
      </template>
      <el-table :data="newsList" @row-click="goDetail">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="createTime" label="发布时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { News, NewsCategory } from '@/types';
import { listNews, listNewsCategories } from '@/apis/user';

const router = useRouter();
const newsList = ref<News[]>([]);
const categories = ref<NewsCategory[]>([]);
const categoryId = ref<number | undefined>();

const loadCategories = async () => {
  const res = await listNewsCategories();
  if (res) categories.value = res;
};

const loadNews = async () => {
  const res = await listNews({ pageNum: 1, pageSize: 20, categoryId: categoryId.value });
  if (res?.list) newsList.value = res.list;
};

const goDetail = (row: News) => {
  router.push(`/news/${row.id}`);
};

onMounted(() => {
  loadCategories();
  loadNews();
});
</script>

<style scoped>
.news-container {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
