<template>
  <div class="product-list-page">
    <div class="main-container">
      <!-- 搜索与筛选区域 -->
      <div class="filter-bar">
        <el-row :gutter="20" align="middle">
          <el-col :xs="24" :sm="8" :md="6">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称、产地、店铺名、商家名"
              size="large"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="12" :sm="5" :md="6">
             <el-cascader
              v-model="selectedRegion"
              :options="originOptions"
              placeholder="产地筛选 (省/市/区)"
              size="large"
              clearable
              style="width: 100%"
              @change="handleFilter"
            />
          </el-col>
          <el-col :xs="12" :sm="5" :md="4">
            <el-select v-model="filterCategory" placeholder="商品品类" size="large" clearable @change="handleFilter">
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="6" :md="8">
            <el-radio-group v-model="sortType" size="large" @change="handleSort">
              <el-radio-button label="sales">销量</el-radio-button>
              <el-radio-button label="score">评分</el-radio-button>
              <el-radio-button label="price_asc">价格↑</el-radio-button>
              <el-radio-button label="price_desc">价格↓</el-radio-button>
            </el-radio-group>
          </el-col>
        </el-row>
      </div>

      <!-- 商品列表 -->
      <section class="section-block">
        <el-skeleton :loading="loading" animated :count="4">
          <template #template>
            <el-col :span="6">
              <el-skeleton-item variant="image" style="width: 100%; height: 200px" />
              <div style="padding: 14px">
                <el-skeleton-item variant="h3" style="width: 50%" />
                <el-skeleton-item variant="text" style="margin-right: 16px" />
              </div>
            </el-col>
          </template>
          <template #default>
            <div class="product-grid" v-if="products.length > 0">
              <el-card
                v-for="product in products"
                :key="product.id"
                class="product-card"
                :body-style="{ padding: '0px' }"
                shadow="hover"
                @click="goToProduct(product.id)"
              >
                <div class="image-wrapper">
                  <img :src="getCoverImage(product.productImg)" class="product-image" />
                  <div v-if="product.stock <= 10" class="stock-badge">仅剩 {{ product.stock }} 件</div>
                </div>
                <div class="card-content">
                  <h3 class="product-name text-ellipsis">{{ product.productName }}</h3>
                  <div class="product-tags">
                    <el-tag size="small" effect="plain">{{ getCategoryText(product) }}</el-tag>
                    <span class="origin"><el-icon><Location /></el-icon> {{ product.originPlace ? product.originPlace.split('/').slice(0, 2).join('/') : '四川' }}</span>
                  </div>
                  <div class="price-row">
                    <span class="price">¥{{ product.price }}</span>
                    <span class="sales">已售 {{ product.salesVolume || 0 }}</span>
                  </div>
                  <div class="merchant-name text-ellipsis">
                    <el-icon><Shop /></el-icon> {{ product.merchantName }}
                  </div>
                </div>
              </el-card>
            </div>
            <el-empty v-else description="暂无相关商品" />
          </template>
        </el-skeleton>
        
        <div class="pagination-wrapper" v-if="total > pageSize">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            v-model:current-page="currentPage"
            @current-change="loadProducts"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Search, Location, Shop } from '@element-plus/icons-vue';
import { searchProducts, listCategories } from '@/apis/product';
import type { Product, ProductCategory } from '@/types';
import { regionData, codeToText } from 'element-china-area-data';
import { getFullImageUrl } from '@/utils/image';
import { PRODUCT_CATEGORY_OPTIONS, getProductCategoryName } from '@/constants/category';

const router = useRouter();
const route = useRoute();
const loading = ref(false);

// 筛选状态
const searchKeyword = ref('');
const selectedRegion = ref<string[]>([]);
const filterCategory = ref<number | ''>('');
const sortType = ref('sales');
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);

// 选项数据
const originOptions = regionData; // 使用省市区数据
const categoryOptions = ref<ProductCategory[]>([]);
const fixedCategoryOptions: ProductCategory[] = PRODUCT_CATEGORY_OPTIONS.map((item) => ({
  id: item.value,
  categoryName: item.label,
  parentId: 0,
  categoryLevel: 1
}));

// 列表数据
const products = ref<Product[]>([]);

// 初始化
onMounted(async () => {
  applyQueryFilters();
  await Promise.all([
    loadOptions(),
    loadProducts()
  ]);
});

watch(
  () => route.query,
  () => {
    applyQueryFilters();
    currentPage.value = 1;
    loadProducts();
  }
);

// 加载选项
const loadOptions = async () => {
  try {
    await listCategories();
    categoryOptions.value = fixedCategoryOptions;
  } catch (error) {
    console.error('加载选项失败', error);
    categoryOptions.value = fixedCategoryOptions;
  }
};

const getCategoryText = (product: Product) => {
  return getProductCategoryName(product.categoryId, product.categoryName) || '生鲜果蔬';
};

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return getFullImageUrl(first);
};

const applyQueryFilters = () => {
  searchKeyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : '';
  const categoryQuery = Number(route.query.category);
  filterCategory.value = !Number.isNaN(categoryQuery) && categoryQuery > 0 ? categoryQuery : '';
  sortType.value = typeof route.query.sort === 'string' ? normalizeSort(route.query.sort) : 'sales';
};

const normalizeSort = (sort: string) => {
  if (sort === 'default') return 'sales';
  if (sort === 'price') return 'price_asc';
  if (sort === 'price_asc' || sort === 'price_desc' || sort === 'sales' || sort === 'score' || sort === 'stock') return sort;
  return 'sales';
};

// 加载商品列表
const loadProducts = async () => {
  loading.value = true;
  try {
    // 处理产地筛选
    let originStr = '';
    if (selectedRegion.value && selectedRegion.value.length > 0) {
      originStr = selectedRegion.value.map(code => codeToText[code]).join('/');
    }

    const params = {
      keyword: searchKeyword.value,
      originPlace: originStr,
      categoryId: filterCategory.value,
      sortBy: sortType.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };
    const res = await searchProducts(params);
    if (res && res.list) {
      products.value = res.list;
      total.value = res.total;
    } else {
       products.value = [];
       total.value = 0;
    }
  } catch (error) {
    console.error('加载商品失败', error);
    products.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadProducts();
};

const handleFilter = () => {
  currentPage.value = 1;
  loadProducts();
};

const handleSort = () => {
  currentPage.value = 1;
  loadProducts();
};

const goToProduct = (id: number) => {
  router.push(`/product/${id}`);
};

</script>

<style scoped lang="scss">
.product-list-page {
  background-color: #f5f7fa;
  min-height: 100vh;
}

.main-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.filter-bar {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
  margin-bottom: 30px;
}

.section-block {
  margin-bottom: 40px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.3s;
  
  &:hover {
    transform: translateY(-5px);
  }
  
  .image-wrapper {
    position: relative;
    height: 200px;
    overflow: hidden;
    
    .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .stock-badge {
      position: absolute;
      bottom: 0;
      right: 0;
      background: rgba(245, 108, 108, 0.9);
      color: #fff;
      padding: 4px 8px;
      font-size: 12px;
      border-top-left-radius: 8px;
    }
  }
  
  .card-content {
    padding: 14px;
    
    .product-name {
      font-size: 16px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 8px;
    }
    
    .product-tags {
      display: flex;
      justify-content: space-between;
      margin-bottom: 10px;
      
      .origin {
        font-size: 12px;
        color: #909399;
        display: flex;
        align-items: center;
      }
    }
    
    .price-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      
      .price {
        color: #f56c6c;
        font-size: 18px;
        font-weight: bold;
      }
      
      .sales {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .merchant-name {
      font-size: 12px;
      color: #606266;
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination-wrapper {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .filter-bar {
    padding: 10px;
  }
}
</style>

