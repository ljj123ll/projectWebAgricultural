<template>
  <div class="unsold-page">
    <div class="page-header">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h2>滞销农产品专区</h2>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索滞销商品或农户" clearable />
      <el-select v-model="sortType" placeholder="排序方式" style="width: 160px">
        <el-option label="综合排序" value="default" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
        <el-option label="库存优先" value="stock" />
      </el-select>
    </div>

    <div class="unsold-list">
      <div v-for="item in filteredList" :key="item.id" class="unsold-card" @click="goDetail(item.id)">
        <img :src="item.image" class="product-img" />
        <div class="card-info">
          <h3>{{ item.name }}</h3>
          <p class="farmer">农户：{{ item.farmerName }}</p>
          <p class="reason">原因：{{ item.reason }}</p>
          <div class="price-row">
            <span class="sale-price">助农价 ¥{{ item.salePrice.toFixed(2) }}</span>
            <span class="original-price">原价 ¥{{ item.originalPrice.toFixed(2) }}</span>
          </div>
          <div class="stock-row">
            <span>库存 {{ item.stock }} 件</span>
            <span>已售 {{ item.sold }} 件</span>
          </div>
        </div>
        <el-button type="warning" size="small">助农购买</el-button>
      </div>
    </div>

    <el-empty v-if="filteredList.length === 0" description="暂无滞销商品" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const keyword = ref('');
const sortType = ref('default');

const unsoldList = ref([
  {
    id: 1,
    name: '滞销大白菜',
    image: 'https://via.placeholder.com/120',
    farmerName: '王大爷',
    originalPrice: 2.5,
    salePrice: 1.5,
    stock: 500,
    sold: 156,
    reason: '集中上市，供大于求'
  },
  {
    id: 2,
    name: '滞销苹果',
    image: 'https://via.placeholder.com/120',
    farmerName: '李大叔',
    originalPrice: 8.0,
    salePrice: 5.0,
    stock: 320,
    sold: 300,
    reason: '物流成本高，销售渠道有限'
  },
  {
    id: 3,
    name: '滞销土豆',
    image: 'https://via.placeholder.com/120',
    farmerName: '张阿姨',
    originalPrice: 3.2,
    salePrice: 2.2,
    stock: 800,
    sold: 210,
    reason: '雨季影响市场需求'
  }
]);

const filteredList = computed(() => {
  let list = [...unsoldList.value];
  if (keyword.value.trim()) {
    const key = keyword.value.trim();
    list = list.filter(item => item.name.includes(key) || item.farmerName.includes(key));
  }
  if (sortType.value === 'price_asc') {
    list.sort((a, b) => a.salePrice - b.salePrice);
  } else if (sortType.value === 'price_desc') {
    list.sort((a, b) => b.salePrice - a.salePrice);
  } else if (sortType.value === 'stock') {
    list.sort((a, b) => b.stock - a.stock);
  }
  return list;
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
