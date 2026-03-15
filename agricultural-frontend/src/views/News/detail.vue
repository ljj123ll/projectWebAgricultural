<template>
  <div class="news-detail">
    <el-card>
      <h2>{{ news?.title }}</h2>
      <div class="meta">{{ news?.createTime }}</div>
      <div class="content" v-html="news?.content"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import type { News } from '@/types';
import { getNewsDetail } from '@/apis/user';

const route = useRoute();
const news = ref<News | null>(null);

const loadDetail = async () => {
  const id = Number(route.params.id);
  if (!id) return;
  const res = await getNewsDetail(id);
  if (res) news.value = res;
};

onMounted(() => {
  loadDetail();
});
</script>

<style scoped>
.news-detail {
  padding: 20px;
}
.meta {
  color: #999;
  margin-bottom: 10px;
}
.content {
  line-height: 1.8;
}
</style>
