<template>
  <div class="home-page">
    <!-- 顶部 Banner -->
    <div class="banner-section">
      <el-carousel height="400px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="item in banners" :key="item.id">
          <div class="banner-item" :style="{ backgroundImage: `url(${item.imgUrl})` }">
            <div class="banner-content">
              <h2>{{ item.title }}</h2>
              <p>{{ item.subtitle }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="main-container">
      <!-- 搜索与筛选区域 -->
      <div class="filter-bar">
        <el-row :gutter="20" align="middle">
          <el-col :xs="24" :sm="8" :md="6">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称、商家..."
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
          <el-col :xs="12" :sm="5" :md="4">
            <el-select v-model="filterOrigin" placeholder="产地筛选" size="large" clearable @change="handleFilter">
              <el-option v-for="item in originOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-col>
          <el-col :xs="12" :sm="5" :md="4">
            <el-select v-model="filterCategory" placeholder="商品品类" size="large" clearable @change="handleFilter">
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="6" :md="4">
            <el-radio-group v-model="sortType" size="large" @change="handleSort">
              <el-radio-button label="default">综合</el-radio-button>
              <el-radio-button label="sales">销量</el-radio-button>
              <el-radio-button label="price">价格</el-radio-button>
            </el-radio-group>
          </el-col>
        </el-row>
      </div>

      <!-- 推荐商品 -->
      <section class="section-block">
        <div class="section-header">
          <h2 class="title">
            <el-icon class="icon-success"><StarFilled /></el-icon> 推荐商品
          </h2>
          <span class="subtitle">精选优质农产品，产地直供</span>
        </div>
        
        <el-skeleton :loading="loading" animated :count="4" v-if="loading">
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
                  <img :src="product.productImg" class="product-image" />
                  <div v-if="product.stock <= 10" class="stock-badge">仅剩 {{ product.stock }} 件</div>
                </div>
                <div class="card-content">
                  <h3 class="product-name text-ellipsis">{{ product.productName }}</h3>
                  <div class="product-tags">
                    <el-tag size="small" effect="plain">{{ product.categoryName || '生鲜' }}</el-tag>
                    <span class="origin"><el-icon><Location /></el-icon> {{ product.originPlace || '四川' }}</span>
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

      <!-- 助农热销榜 & 滞销专区 (双列布局) -->
      <el-row :gutter="24" class="section-block">
        <!-- 热销榜 -->
        <el-col :xs="24" :md="16">
          <el-card class="box-card hot-section" shadow="never">
            <template #header>
              <div class="card-header">
                <span>
                  <el-icon class="icon-danger"><TrendCharts /></el-icon> 助农热销榜
                </span>
                <el-button text type="primary">查看更多</el-button>
              </div>
            </template>
            <div class="hot-list">
              <div v-for="(item, index) in hotProductList" :key="item.id" class="hot-item" @click="goToProduct(item.id)">
                <div class="rank-badge" :class="{'top-3': index < 3}">{{ index + 1 }}</div>
                <img :src="item.productImg" class="hot-img" />
                <div class="hot-info">
                  <div class="name">{{ item.productName }}</div>
                  <div class="desc text-ellipsis">{{ item.productDesc || '暂无描述' }}</div>
                </div>
                <div class="hot-stats">
                  <div class="price">¥{{ item.price }}</div>
                  <div class="sales">月销 {{ item.salesVolume }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <!-- 滞销专区入口 -->
        <el-col :xs="24" :md="8">
          <el-card class="box-card unsold-card" shadow="hover" :body-style="{ padding: '0' }" @click="goToUnsold">
            <div class="unsold-banner">
              <div class="overlay">
                <h3><el-icon><WarningFilled /></el-icon> 滞销农产品专区</h3>
                <p>急需帮助，爱心助农</p>
                <el-button type="warning" round>立即查看</el-button>
              </div>
            </div>
          </el-card>
          
          <!-- 助农新闻简略 -->
          <el-card class="box-card news-card" shadow="never">
            <template #header>
              <div class="card-header">
                <span><el-icon class="icon-primary"><Document /></el-icon> 助农新闻</span>
                <el-button text @click="router.push('/news')">更多</el-button>
              </div>
            </template>
            <ul class="news-list-simple">
              <li v-for="news in newsList" :key="news.id" @click="goToNews(news.id)">
                <span class="news-tag">{{ news.categoryName }}</span>
                <span class="news-title text-ellipsis">{{ news.title }}</span>
                <span class="news-date">{{ formatDate(news.createTime) }}</span>
              </li>
            </ul>
          </el-card>
        </el-col>
      </el-row>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Search, StarFilled, TrendCharts, WarningFilled, Document, Location, Shop } from '@element-plus/icons-vue';
import { searchProducts, hotProducts, listOrigins, listCategories } from '@/apis/product';
import { listNews } from '@/apis/user';
import type { Product, News, ProductCategory } from '@/types';

const router = useRouter();
const loading = ref(false);

// Banner 数据 (模拟)
const banners = [
  { id: 1, title: '绿色助农，健康生活', subtitle: '连接城乡，传递新鲜', imgUrl: 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854?ixlib=rb-1.2.1&auto=format&fit=crop&w=1350&q=80' },
  { id: 2, title: '春季特惠，产地直发', subtitle: '时令鲜果，限时抢购', imgUrl: 'https://images.unsplash.com/photo-1610832958506-aa56368176cf?ixlib=rb-1.2.1&auto=format&fit=crop&w=1350&q=80' },
];

// 筛选状态
const searchKeyword = ref('');
const filterOrigin = ref('');
const filterCategory = ref<number | ''>('');
const sortType = ref('default');
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);

// 选项数据
const originOptions = ref<string[]>([]);
const categoryOptions = ref<ProductCategory[]>([]);

// 列表数据
const products = ref<Product[]>([]);
const hotProductList = ref<Product[]>([]);
const newsList = ref<News[]>([]);

// 初始化
onMounted(async () => {
  await Promise.all([
    loadOptions(),
    loadProducts(),
    loadHotProducts(),
    loadNews()
  ]);
});

// 加载选项
const loadOptions = async () => {
  try {
    const [originsRes, categoriesRes] = await Promise.all([
      listOrigins(),
      listCategories()
    ]);
    // 假设后端返回的是标准 ApiResponse 结构，这里可能需要根据实际情况调整
    // 由于 types 中定义 request 返回的是 T，我们假设 request 拦截器已经解包了 data
    // 但如果 request.ts 没有解包，则需要 .data
    // 假设 request.ts 已经处理了 response.data.data 的解包，或者直接返回 data
    // 根据 apis/product.ts 的定义：request.get<any, string[]>('/user/origins')
    // 这里我们先按直接返回数据处理，如果不行再调整
    if (originsRes) originOptions.value = originsRes;
    if (categoriesRes) categoryOptions.value = categoriesRes;
  } catch (error) {
    console.error('加载选项失败', error);
    // Mock data fallback
    originOptions.value = ['成都市', '绵阳市', '德阳市', '南充市'];
    categoryOptions.value = [
      { id: 1, categoryName: '生鲜果蔬', categoryCode: 'fresh' },
      { id: 2, categoryName: '粮油副食', categoryCode: 'grain' }
    ];
  }
};

// 加载商品列表
const loadProducts = async () => {
  loading.value = true;
  try {
    const params = {
      keyword: searchKeyword.value,
      origin: filterOrigin.value,
      categoryId: filterCategory.value,
      sortBy: sortType.value, // Changed from sort to sortBy to match API
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };
    const res = await searchProducts(params);
    if (res && res.list) {
      products.value = res.list;
      total.value = res.total;
    } else {
       // Mock fallback
       products.value = mockProducts;
       total.value = mockProducts.length;
    }
  } catch (error) {
    console.error('加载商品失败', error);
    // Use mock data if API fails to allow UI development without backend
    products.value = mockProducts;
    total.value = mockProducts.length;
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
  loadProducts();
};

// 加载热销
const loadHotProducts = async () => {
  try {
    const res = await hotProducts({ limit: 5 });
    if (res) {
      hotProductList.value = res;
    } else {
        hotProductList.value = mockProducts.slice(0, 5);
    }
  } catch (error) {
     hotProductList.value = mockProducts.slice(0, 5);
  }
};

// 加载新闻
const loadNews = async () => {
  try {
    const res = await listNews({ pageNum: 1, pageSize: 5 });
    if (res && res.list) {
      newsList.value = res.list;
    }
  } catch (error) {
      // mock
      newsList.value = [
          { id: 1, title: '四川启动助农增收计划', categoryName: '农业政策', createTime: '2023-10-01' } as any,
          { id: 2, title: '蒲江猕猴桃丰收在望', categoryName: '产地动态', createTime: '2023-09-28' } as any
      ];
  }
};

// 跳转逻辑
const goToProduct = (id: number) => {
  router.push(`/product/${id}`);
};

const goToUnsold = () => {
  router.push('/unsold');
};

const goToNews = (id: number) => {
  router.push(`/news/${id}`);
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString();
};

// Mock Data
const mockProducts: Product[] = [
  {
    id: 1,
    productName: '四川红心猕猴桃',
    productImg: 'https://via.placeholder.com/300x300/67c23a/fff?text=Kiwi',
    price: 29.9,
    stock: 8,
    salesVolume: 156,
    originPlace: '成都市蒲江',
    merchantName: '蒲江生态果园',
    categoryId: 1,
    categoryName: '生鲜果蔬'
  },
  {
    id: 2,
    productName: '农家土鸡蛋 30枚',
    productImg: 'https://via.placeholder.com/300x300/e6a23c/fff?text=Eggs',
    price: 45.0,
    stock: 120,
    salesVolume: 89,
    originPlace: '绵阳市三台',
    merchantName: '三台养殖合作社',
    categoryId: 4,
    categoryName: '畜禽肉蛋'
  },
  {
    id: 3,
    productName: '安岳柠檬 5斤装',
    productImg: 'https://via.placeholder.com/300x300/f56c6c/fff?text=Lemon',
    price: 19.9,
    stock: 500,
    salesVolume: 230,
    originPlace: '资阳市安岳',
    merchantName: '安岳柠檬基地',
    categoryId: 1,
    categoryName: '生鲜果蔬'
  },
  {
    id: 4,
    productName: '通江银耳 特级',
    productImg: 'https://via.placeholder.com/300x300/409eff/fff?text=Fungus',
    price: 88.0,
    stock: 45,
    salesVolume: 67,
    originPlace: '巴中市通江',
    merchantName: '巴山珍品',
    categoryId: 3,
    categoryName: '干货特产'
  }
];

</script>

<style scoped lang="scss">
.home-page {
  background-color: #f5f7fa;
  min-height: 100vh;
}

.banner-section {
  margin-bottom: 20px;
  .banner-item {
    height: 100%;
    background-size: cover;
    background-position: center;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    
    &::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.3);
    }
    
    .banner-content {
      position: relative;
      z-index: 1;
      text-align: center;
      color: #fff;
      h2 {
        font-size: 3em;
        margin-bottom: 10px;
        text-shadow: 0 2px 4px rgba(0,0,0,0.5);
      }
      p {
        font-size: 1.5em;
        text-shadow: 0 1px 2px rgba(0,0,0,0.5);
      }
    }
  }
}

.main-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px 40px;
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

.section-header {
  display: flex;
  align-items: baseline;
  margin-bottom: 20px;
  border-bottom: 2px solid #e4e7ed;
  padding-bottom: 10px;
  
  .title {
    font-size: 24px;
    color: #303133;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 10px;
  }
  
  .subtitle {
    margin-left: 15px;
    color: #909399;
    font-size: 14px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 18px;
    font-weight: bold;
  }
}

.icon-success { color: #67c23a; }
.icon-danger { color: #f56c6c; }
.icon-primary { color: #409eff; }

/* Product Grid */
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

/* Hot List */
.hot-list {
  .hot-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f0f2f5;
    cursor: pointer;
    
    &:last-child { border-bottom: none; }
    &:hover { background-color: #f9fafc; }
    
    .rank-badge {
      width: 24px;
      height: 24px;
      line-height: 24px;
      text-align: center;
      background: #dcdfe6;
      color: #fff;
      border-radius: 4px;
      margin-right: 12px;
      font-weight: bold;
      
      &.top-3 {
        background: #f56c6c;
      }
    }
    
    .hot-img {
      width: 60px;
      height: 60px;
      object-fit: cover;
      border-radius: 4px;
      margin-right: 12px;
    }
    
    .hot-info {
      flex: 1;
      min-width: 0;
      
      .name {
        font-size: 15px;
        color: #303133;
        margin-bottom: 4px;
      }
      .desc {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .hot-stats {
      text-align: right;
      .price { color: #f56c6c; font-weight: bold; }
      .sales { font-size: 12px; color: #909399; }
    }
  }
}

/* Unsold Section */
.unsold-card {
  margin-bottom: 20px;
  cursor: pointer;
  
  .unsold-banner {
    height: 160px;
    background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .overlay {
      text-align: center;
      color: #fff;
      
      h3 {
        font-size: 20px;
        margin-bottom: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
      }
      p { margin-bottom: 16px; }
    }
  }
}

/* News List Simple */
.news-list-simple {
  list-style: none;
  padding: 0;
  margin: 0;
  
  li {
    display: flex;
    align-items: center;
    padding: 10px 0;
    cursor: pointer;
    
    &:hover .news-title { color: #409eff; }
    
    .news-tag {
      background: #ecf5ff;
      color: #409eff;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 12px;
      margin-right: 8px;
      flex-shrink: 0;
    }
    
    .news-title {
      flex: 1;
      color: #606266;
      font-size: 14px;
      margin-right: 10px;
    }
    
    .news-date {
      font-size: 12px;
      color: #909399;
      flex-shrink: 0;
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
  .banner-section .el-carousel__container {
    height: 200px !important;
  }
  
  .filter-bar {
    padding: 10px;
  }
}
</style>
