<template>
  <div class="unsold-page">
    <div class="page-header">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h2>滞销农产品专区</h2>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索滞销商品或商家" clearable @keyup.enter="loadUnsoldProducts" />
      <el-select v-model="sortType" placeholder="排序方式" style="width: 160px">
        <el-option label="销量优先" value="sales" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
        <el-option label="库存优先" value="stock" />
      </el-select>
      <el-button type="primary" @click="loadUnsoldProducts">筛选</el-button>
    </div>

    <div v-loading="loading" class="unsold-list">
      <div v-for="item in productList" :key="item.id" class="unsold-card" @click="goDetail(item.id)">
        <img :src="getFullImageUrl(item.productImg)" class="product-img" />
        <div class="card-info">
          <h3>{{ item.productName }}</h3>
          <p class="farmer">商家：{{ item.merchantName || '助农商家' }}</p>
          <p class="reason">产地：{{ item.originPlace || '待补充' }}</p>
          <div class="price-row">
            <span class="sale-price">助农价 ¥{{ Number(item.price).toFixed(2) }}</span>
          </div>
          <div class="stock-row">
            <span>库存 {{ item.stock }} 件</span>
            <span>已售 {{ item.salesVolume || 0 }} 件</span>
          </div>
        </div>
        <el-button type="warning" size="small">助农购买</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && productList.length === 0" description="暂无滞销商品" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { searchProducts } from '@/apis/product';
import { getFullImageUrl } from '@/utils/image';
import type { Product } from '@/types';

const router = useRouter();
const keyword = ref('');
const sortType = ref('sales');
const loading = ref(false);
const productList = ref<Product[]>([]);

const loadUnsoldProducts = async () => {
  loading.value = true;
  try {
    const res = await searchProducts({
      keyword: keyword.value,
      sortBy: sortType.value,
      isUnsalable: 1,
      pageNum: 1,
      pageSize: 50
    });
    productList.value = res?.list || [];
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadUnsoldProducts();
});

const goDetail = (id: number) => {
  router.push(`/product/${id}`);
};
</script>

<style scoped lang="scss">
.unsold-page {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 18px;
  }
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.unsold-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.unsold-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  }

  .product-img {
    width: 100px;
    height: 100px;
    border-radius: 8px;
    object-fit: cover;
  }

  .card-info {
    flex: 1;

    h3 {
      margin: 0 0 8px;
      font-size: 16px;
    }

    .farmer {
      margin: 0 0 6px;
      color: #606266;
      font-size: 13px;
    }

    .reason {
      margin: 0 0 8px;
      color: #909399;
      font-size: 13px;
    }

    .price-row {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 6px;

      .sale-price {
        color: #e6a23c;
        font-weight: 600;
      }

      .original-price {
        color: #909399;
        text-decoration: line-through;
      }
    }

    .stock-row {
      display: flex;
      gap: 12px;
      font-size: 13px;
      color: #909399;
    }
  }
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
  }

  .unsold-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
