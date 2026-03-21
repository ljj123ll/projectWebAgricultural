<template>
  <div class="merchant-shop-page">
    <div class="shop-header">
      <div class="left">
        <el-avatar :size="72" :src="getFullImageUrl(shop?.qualificationImg)">
          {{ (shop?.shopName || '店铺').slice(0, 1) }}
        </el-avatar>
        <div class="meta">
          <h1>{{ shop?.shopName || '店铺详情' }}</h1>
          <p>{{ shop?.shopIntro || '该店铺暂未填写简介。' }}</p>
          <div class="tags">
            <el-tag v-for="tag in shopTags" :key="tag" type="success" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
      </div>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <el-card class="products-card">
      <template #header>
        <div class="card-header">
          <span>店铺商品</span>
          <span class="count">共 {{ total }} 件</span>
        </div>
      </template>

      <div v-loading="loading" class="product-grid">
        <div v-for="item in products" :key="item.id" class="product-item" @click="goToProduct(item.id)">
          <img :src="getCoverImage(item.productImg)" :alt="item.productName" />
          <h4>{{ item.productName }}</h4>
          <p class="origin">{{ item.originPlace || '产地待补充' }}</p>
          <div class="bottom">
            <span class="price">¥{{ Number(item.price || 0).toFixed(2) }}</span>
            <span class="sales">销量 {{ item.salesVolume || 0 }}</span>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && products.length === 0" description="暂无在售商品" />

      <div class="pager" v-if="total > pageSize">
        <el-pagination
          background
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="total"
          :current-page="pageNum"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getMerchantProductsPublic, getMerchantShop } from '@/apis/product';
import type { Product, ShopInfo } from '@/types';
import { getFullImageUrl } from '@/utils/image';

const route = useRoute();
const router = useRouter();

const merchantId = Number(route.params.merchantId);
const loading = ref(false);
const shop = ref<ShopInfo | null>(null);
const products = ref<Product[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = 12;

const shopTags = computed(() => {
  const tags = (shop.value?.categories || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
  if (shop.value?.shopType) tags.unshift(shop.value.shopType);
  return Array.from(new Set(tags)).slice(0, 6);
});

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return getFullImageUrl(first);
};

const loadShop = async () => {
  const res = await getMerchantShop(merchantId);
  shop.value = res || null;
};

const loadProducts = async () => {
  loading.value = true;
  try {
    const res = await getMerchantProductsPublic(merchantId, { pageNum: pageNum.value, pageSize });
    products.value = res?.list || [];
    total.value = Number(res?.total || 0);
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page: number) => {
  pageNum.value = page;
  loadProducts();
};

const goToProduct = (id: number) => {
  router.push(`/product/${id}`);
};

onMounted(async () => {
  if (!merchantId) {
    router.replace('/products');
    return;
  }
  await Promise.all([loadShop(), loadProducts()]);
});
</script>

<style scoped lang="scss">
.merchant-shop-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shop-header {
  border: 1px solid #e7edf6;
  border-radius: 14px;
  background: linear-gradient(145deg, #ffffff 0%, #f7fbf4 100%);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  gap: 14px;

  .left {
    display: flex;
    gap: 14px;
  }

  .meta {
    h1 {
      margin: 2px 0 8px;
      font-size: 24px;
      color: #223045;
    }

    p {
      margin: 0;
      color: #5f6f86;
      line-height: 1.7;
    }
  }

  .tags {
    margin-top: 10px;
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.products-card {
  border-radius: 14px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #2f3a4f;
    font-weight: 600;
  }

  .count {
    font-size: 13px;
    color: #7a8699;
    font-weight: 500;
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.product-item {
  border: 1px solid #ebeff6;
  border-radius: 10px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cde6bd;
    box-shadow: 0 8px 18px rgba(55, 94, 148, 0.08);
    transform: translateY(-1px);
  }

  img {
    width: 100%;
    height: 148px;
    object-fit: cover;
    border-radius: 8px;
    background: #f6f8fb;
  }

  h4 {
    margin: 10px 0 6px;
    font-size: 14px;
    color: #263345;
    line-height: 1.4;
  }

  .origin {
    margin: 0;
    font-size: 12px;
    color: #8d97a7;
  }

  .bottom {
    margin-top: 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .price {
    color: #f56c6c;
    font-size: 18px;
    font-weight: 700;
  }

  .sales {
    font-size: 12px;
    color: #7d8697;
  }
}

.pager {
  margin-top: 18px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .shop-header {
    flex-direction: column;
    padding: 14px;
  }

  .shop-header .meta h1 {
    font-size: 20px;
  }
}
</style>

