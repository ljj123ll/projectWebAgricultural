<template>
  <div class="reviews-page">
    <h1 class="page-title">我的评价</h1>
    <p v-if="filterText" class="filter-tip">{{ filterText }}</p>

    <div class="review-list" v-loading="loading">
      <div v-for="review in filteredReviews" :key="review.id" class="review-card">
        <div class="review-header">
          <img :src="getCoverImage(review.productImg)" :alt="review.productName" class="product-img" />
          <div class="product-info">
            <h4>{{ review.productName }}</h4>
            <el-rate :model-value="review.score" disabled />
            <p class="order-no">订单号：{{ review.orderNo }}</p>
          </div>
          <span class="review-time">{{ formatDate(review.createTime) }}</span>
        </div>
        <div class="review-content">
          <p>{{ review.content }}</p>
        </div>
        <div v-if="parseImageList(review.imgUrls).length" class="review-images">
          <el-image
            v-for="(img, idx) in parseImageList(review.imgUrls)"
            :key="`${review.id}-${idx}`"
            :src="img"
            :preview-src-list="parseImageList(review.imgUrls)"
            fit="cover"
            class="review-image"
            preview-teleported
          />
        </div>
        <div class="review-actions">
          <el-button link type="primary" @click="goToProduct(review.productId)">查看商品</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && filteredReviews.length === 0" description="暂无评价" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { listComments } from '@/apis/user';
import { getProductDetail } from '@/apis/product';
import type { Comment } from '@/types';
import { getFullImageUrl } from '@/utils/image';

type ReviewCard = Comment & {
  productName: string;
  productImg: string;
};

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const reviews = ref<ReviewCard[]>([]);

const orderNoQuery = computed(() => String(route.query.orderNo || '').trim());
const productIdQuery = computed(() => Number(route.query.productId || 0));

const filterText = computed(() => {
  const byOrder = orderNoQuery.value ? `订单 ${orderNoQuery.value}` : '';
  const byProduct = productIdQuery.value > 0 ? `商品ID ${productIdQuery.value}` : '';
  if (!byOrder && !byProduct) return '';
  return `当前筛选：${[byOrder, byProduct].filter(Boolean).join('，')}`;
});

const filteredReviews = computed(() => {
  return reviews.value.filter(review => {
    if (orderNoQuery.value && review.orderNo !== orderNoQuery.value) return false;
    if (productIdQuery.value > 0 && Number(review.productId) !== productIdQuery.value) return false;
    return true;
  });
});

const loadReviews = async () => {
  loading.value = true;
  try {
    const res = await listComments({ pageNum: 1, pageSize: 1000 });
    const list = (res?.list || []) as Comment[];
    const productIds = Array.from(new Set(list.map(item => item.productId).filter(Boolean)));
    const productMap = new Map<number, { productName: string; productImg: string }>();

    await Promise.all(productIds.map(async (id) => {
      try {
        const product = await getProductDetail(Number(id));
        if (!product) return;
        productMap.set(Number(id), {
          productName: product.productName || `商品#${id}`,
          productImg: product.productImg || ''
        });
      } catch (error) {
        console.warn(`加载商品信息失败 productId=${id}`, error);
      }
    }));

    reviews.value = list.map(item => {
      const product = productMap.get(Number(item.productId));
      return {
        ...item,
        productName: product?.productName || `商品#${item.productId}`,
        productImg: product?.productImg || ''
      } as ReviewCard;
    });
  } finally {
    loading.value = false;
  }
};

const formatDate = (date?: string) => {
  if (!date) return '';
  return date.substring(0, 16).replace('T', ' ');
};

const parseImageList = (raw?: string) => {
  if (!raw) return [];
  return raw
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .map(url => getFullImageUrl(url));
};

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return getFullImageUrl(first);
};

const goToProduct = (productId: number) => {
  router.push(`/product/${productId}`);
};

onMounted(() => {
  loadReviews();
});
</script>

<style scoped lang="scss">
.reviews-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.page-title {
  font-size: 24px;
  margin: 0 0 10px;
}

.filter-tip {
  margin: 0 0 18px;
  color: #5f6f86;
  font-size: 13px;
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

        .order-no {
          margin: 8px 0 0;
          font-size: 12px;
          color: #909399;
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

    .review-images {
      margin-top: 12px;
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      .review-image {
        width: 84px;
        height: 84px;
        border-radius: 8px;
        overflow: hidden;
      }
    }

    .review-actions {
      margin-top: 10px;
      text-align: right;
    }
  }
}

@media (max-width: 768px) {
  .reviews-page {
    padding: 16px;
  }
}
</style>
