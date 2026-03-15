<template>
  <div class="reviews-page">
    <h1 class="page-title">我的评价</h1>

    <div class="review-list">
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-header">
          <img :src="review.productImg" :alt="review.productName" class="product-img" />
          <div class="product-info">
            <h4>{{ review.productName }}</h4>
            <el-rate v-model="review.score" disabled />
          </div>
          <span class="review-time">{{ formatDate(review.createTime) }}</span>
        </div>
        <div class="review-content">
          <p>{{ review.content }}</p>
        </div>
      </div>
    </div>

    <el-empty v-if="reviews.length === 0" description="暂无评价" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const reviews = ref([
  {
    id: 1,
    productName: '四川红心猕猴桃',
    productImg: 'https://via.placeholder.com/100x100/67c23a/fff?text=猕猴桃',
    score: 5,
    content: '非常好吃，很新鲜，包装也很好！',
    createTime: '2024-03-01'
  },
  {
    id: 2,
    productName: '农家土鸡蛋',
    productImg: 'https://via.placeholder.com/100x100/e6a23c/fff?text=土鸡蛋',
    score: 4,
    content: '鸡蛋很新鲜，蛋黄很黄，就是有个别破了。',
    createTime: '2024-02-28'
  }
]);

const formatDate = (date?: string) => {
  if (!date) return '';
  return date.substring(0, 10);
};
</script>

<style scoped lang="scss">
.reviews-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.page-title {
  font-size: 24px;
  margin: 0 0 24px;
}

.review-list {
  .review-card {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 16px;

    .review-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      .product-img {
        width: 60px;
        height: 60px;
        border-radius: 8px;
        object-fit: cover;
      }

      .product-info {
        flex: 1;

        h4 {
          margin: 0 0 8px;
          font-size: 15px;
        }
      }

      .review-time {
        font-size: 13px;
        color: #909399;
      }
    }

    .review-content {
      p {
        margin: 0;
        color: #606266;
        line-height: 1.6;
      }
    }
  }
}

@media (max-width: 768px) {
  .reviews-page {
    padding: 16px;
  }
}
</style>
