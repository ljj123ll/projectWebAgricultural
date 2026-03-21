<template>
  <div class="home-page">
    <!-- 顶部通栏 -->
    <div class="top-nav">
      <div class="container">
        <div class="left">
          <span>欢迎来到助农电商平台！</span>
          <template v-if="!userInfo">
            <el-button link type="primary" @click="$router.push('/login')">请登录</el-button>
            <el-button link @click="$router.push('/register')">免费注册</el-button>
          </template>
          <template v-else>
            <span class="username">你好，{{ userInfo.nickname || userInfo.phone }}</span>
            <el-button link @click="logout">退出</el-button>
          </template>
        </div>
        <div class="right">
          <el-button link @click="$router.push('/profile')">个人中心</el-button>
          <el-button link @click="$router.push('/cart')">购物车</el-button>
          <el-button link @click="$router.push('/orders')">我的订单</el-button>
          <el-button link @click="$router.push('/merchant/login')">商家入口</el-button>
          <el-button link @click="$router.push('/admin/login')">管理员入口</el-button>
        </div>
      </div>
    </div>

    <!-- 头部搜索区 -->
    <div class="header-search">
      <div class="container">
        <div class="logo" @click="$router.push('/')">
          <img src="https://via.placeholder.com/200x60/e6a23c/fff?text=助农电商" alt="Logo" />
        </div>
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索助农产品、商家..."
            class="search-input"
            size="large"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button type="primary" class="search-btn" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
          <div class="hot-keywords">
            <span v-for="kw in ['猕猴桃', '土鸡蛋', '腊肉', '蜂蜜']" :key="kw" @click="quickSearch(kw)">{{ kw }}</span>
          </div>
        </div>
        <div class="cart-entry" @click="$router.push('/cart')">
          <el-badge :value="cartCount" class="item" :hidden="cartCount === 0">
            <el-button icon="ShoppingCart" size="large" plain>我的购物车</el-button>
          </el-badge>
        </div>
      </div>
    </div>

    <!-- 主导航 -->
    <div class="main-nav">
      <div class="container">
        <div class="all-categories">
          <el-icon><Menu /></el-icon> 全部商品分类
        </div>
        <ul class="nav-list">
          <li class="active">首页</li>
          <li @click="$router.push('/products')">全部商品</li>
          <li @click="$router.push('/unsold')">滞销专区</li>
          <li @click="$router.push('/news')">助农资讯</li>
        </ul>
      </div>
    </div>

    <!-- 轮播图与分类 -->
    <div class="banner-section">
      <div class="container">
        <div class="category-sidebar">
          <ul>
            <li v-for="cat in categoryOptions" :key="cat.id" @click="filterByCategory(cat.id)">
              <span>{{ cat.categoryName }}</span>
              <el-icon><ArrowRight /></el-icon>
            </li>
          </ul>
        </div>
        <div class="banner-content">
          <el-carousel height="400px" :interval="5000">
            <el-carousel-item v-for="item in banners" :key="item.id">
              <img :src="item.imgUrl" class="banner-img" />
            </el-carousel-item>
          </el-carousel>
        </div>
      </div>
    </div>

    <div class="main-container">
      <!-- 核心板块 -->
      <div class="feature-section">
        <div class="feature-card hot" @click="$router.push('/products?sort=sales')">
          <div class="text">
            <h3>助农热销</h3>
            <p>大家都在买</p>
          </div>
          <el-icon size="40"><TrendCharts /></el-icon>
        </div>
        <div class="feature-card unsold" @click="$router.push('/unsold')">
          <div class="text">
            <h3>滞销帮扶</h3>
            <p>爱心助农</p>
          </div>
          <el-icon size="40"><WarningFilled /></el-icon>
        </div>
        <div class="feature-card news" @click="$router.push('/news')">
          <div class="text">
            <h3>产地资讯</h3>
            <p>了解源头</p>
          </div>
          <el-icon size="40"><Document /></el-icon>
        </div>
        <div class="feature-card recommend" @click="$router.push('/products')">
          <div class="text">
            <h3>为你推荐</h3>
            <p>猜你喜欢</p>
          </div>
          <el-icon size="40"><StarFilled /></el-icon>
        </div>
      </div>

      <!-- 推荐商品 -->
      <section class="section-block">
        <div class="section-header-large">
          <h2><span class="highlight">精选</span> 助农好物</h2>
          <div class="more" @click="$router.push('/products')">查看更多 <el-icon><ArrowRight /></el-icon></div>
        </div>
        
        <el-skeleton :loading="loading" animated :count="4" v-if="loading">
          <template #template>
            <el-col :span="6">
              <el-skeleton-item variant="image" style="width: 100%; height: 260px" />
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
                  <img :src="getFullImageUrl(product.productImg)" class="product-image" />
                  <div v-if="product.stock <= 10" class="stock-badge">仅剩 {{ product.stock }} 件</div>
                </div>
                <div class="card-content">
                  <h3 class="product-name text-ellipsis" :title="product.productName">{{ product.productName }}</h3>
                  <div class="product-tags">
                    <el-tag size="small" effect="plain" type="success">{{ getCategoryText(product) }}</el-tag>
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
      </section>

      <!-- 楼层：按分类 -->
      <section class="section-block" v-for="cat in categoryOptions.slice(0, 3)" :key="cat.id">
         <div class="section-header-large">
          <h2>{{ cat.categoryName }}</h2>
          <div class="more" @click="filterByCategory(cat.id)">更多 <el-icon><ArrowRight /></el-icon></div>
        </div>
        <!-- 这里简单复用推荐商品列表，实际应该按分类加载 -->
        <!-- 为演示效果，这里仅展示占位，实际开发需根据分类ID请求 -->
        <div class="product-grid">
           <!-- 仅展示部分商品作为演示 -->
           <el-card
                v-for="product in products.filter(p => p.categoryId === cat.id || !p.categoryId).slice(0, 4)"
                :key="product.id"
                class="product-card"
                :body-style="{ padding: '0px' }"
                shadow="hover"
                @click="goToProduct(product.id)"
              >
                <div class="image-wrapper">
                  <img :src="getFullImageUrl(product.productImg)" class="product-image" />
                </div>
                <div class="card-content">
                  <h3 class="product-name text-ellipsis">{{ product.productName }}</h3>
                   <div class="price-row">
                    <span class="price">¥{{ product.price }}</span>
                  </div>
                </div>
            </el-card>
        </div>
      </section>

    </div>
    
    <!-- 底部 Footer -->
    <footer class="site-footer">
      <div class="container">
        <div class="footer-links">
          <div class="link-group">
            <h4>关于我们</h4>
            <a href="#">平台简介</a>
            <a href="#">联系我们</a>
          </div>
          <div class="link-group">
            <h4>帮助中心</h4>
            <a href="#">购物指南</a>
            <a href="#">支付方式</a>
          </div>
          <div class="link-group">
            <h4>商家服务</h4>
            <a href="#">商家入驻</a>
            <a href="#">商家后台</a>
          </div>
        </div>
        <div class="copyright">
          © 2026 助农电商平台 版权所有 | 川ICP备xxxxxx号
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Menu, ArrowRight, TrendCharts, WarningFilled, Document, StarFilled, Location, Shop } from '@element-plus/icons-vue';
import { searchProducts, hotProducts } from '@/apis/product';
import { listNews, getUserInfo, logout as userLogout } from '@/apis/user';
import type { Product, News, ProductCategory } from '@/types';
import { getFullImageUrl } from '@/utils/image';
import { ElMessage } from 'element-plus';
import { PRODUCT_CATEGORY_OPTIONS, getProductCategoryName } from '@/constants/category';

const router = useRouter();
const loading = ref(false);
const userInfo = ref<any>(null);
const cartCount = ref(0); // 暂未实现购物车数量

// Banner 数据
const banners = [
  { id: 1, title: '绿色助农', imgUrl: 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854?ixlib=rb-1.2.1&auto=format&fit=crop&w=1350&q=80' },
  { id: 2, title: '春季特惠', imgUrl: 'https://images.unsplash.com/photo-1610832958506-aa56368176cf?ixlib=rb-1.2.1&auto=format&fit=crop&w=1350&q=80' },
];

const searchKeyword = ref('');
const categoryOptions = ref<ProductCategory[]>([]);
const products = ref<Product[]>([]);

const fixedCategoryOptions: ProductCategory[] = PRODUCT_CATEGORY_OPTIONS.map((item) => ({
  id: item.value,
  categoryName: item.label,
  parentId: 0,
  categoryLevel: 1
}));
const hotProductList = ref<Product[]>([]);
const newsList = ref<News[]>([]);

onMounted(async () => {
  // 只在有token时检查登录状态
  const token = localStorage.getItem('token');
  if (token) {
    checkLogin();
  }
  await Promise.all([
    loadCategories(),
    loadProducts(),
    loadHotProducts(),
    loadNews()
  ]);
});

const checkLogin = async () => {
  try {
    const res = await getUserInfo();
    if (res) userInfo.value = res;
  } catch (e) {
    // 未登录或token过期，清除本地token
    localStorage.removeItem('token');
  }
};

const logout = async () => {
  try {
    await userLogout();
    userInfo.value = null;
    localStorage.removeItem('token');
    ElMessage.success('已退出登录');
  } catch (e) {
    console.error(e);
  }
};

const loadCategories = async () => {
  categoryOptions.value = fixedCategoryOptions;
};

const loadProducts = async () => {
  loading.value = true;
  try {
    const res = await searchProducts({ pageNum: 1, pageSize: 8 });
    if (res && res.list) {
      products.value = res.list;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const loadHotProducts = async () => {
  try {
    const res = await hotProducts({ limit: 5 });
    if (res) hotProductList.value = res;
  } catch (error) {}
};

const loadNews = async () => {
  try {
    const res = await listNews({ pageNum: 1, pageSize: 5 });
    if (res && res.list) newsList.value = res.list;
  } catch (error) {}
};

const getCategoryText = (product: Product) => {
  return getProductCategoryName(product.categoryId, product.categoryName) || '生鲜果蔬';
};

const handleSearch = () => {
  router.push(`/products?keyword=${searchKeyword.value}`);
};

const quickSearch = (kw: string) => {
  router.push(`/products?keyword=${kw}`);
};

const filterByCategory = (id: number) => {
  router.push(`/products?category=${id}`);
};

const goToProduct = (id: number) => {
  router.push(`/product/${id}`);
};
</script>

<style scoped lang="scss">
.home-page {
  background-color: #f5f5f5;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 顶部通栏 */
.top-nav {
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
  height: 36px;
  line-height: 36px;
  font-size: 12px;
  color: #666;
  
  .container {
    display: flex;
    justify-content: space-between;
    
    .left {
      display: flex;
      gap: 10px;
      .username { color: #f56c6c; }
    }
    
    .right {
      display: flex;
      gap: 15px;
      .el-button { font-size: 12px; color: #666; &:hover { color: #f56c6c; } }
    }
  }
}

/* 头部搜索区 */
.header-search {
  background: #fff;
  padding: 25px 0;
  
  .container {
    display: flex;
    align-items: center;
    gap: 40px;
    
    .logo {
      cursor: pointer;
      img { height: 60px; }
    }
    
    .search-box {
      flex: 1;
      
      .search-input {
        :deep(.el-input-group__append) {
          background-color: #f56c6c;
          border-color: #f56c6c;
          color: #fff;
          font-weight: bold;
          
          &:hover { opacity: 0.9; }
        }
      }
      
      .hot-keywords {
        margin-top: 6px;
        font-size: 12px;
        color: #999;
        span { margin-right: 12px; cursor: pointer; &:hover { color: #f56c6c; } }
      }
    }
    
    .cart-entry {
      width: 120px;
    }
  }
}

/* 主导航 */
.main-nav {
  background: #fff;
  border-bottom: 2px solid #f56c6c;
  
  .container {
    display: flex;
    height: 40px;
    
    .all-categories {
      width: 200px;
      background: #f56c6c;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      font-weight: bold;
      gap: 8px;
    }
    
    .nav-list {
      display: flex;
      list-style: none;
      margin: 0;
      padding: 0 20px;
      
      li {
        padding: 0 20px;
        line-height: 40px;
        font-size: 16px;
        cursor: pointer;
        color: #333;
        font-weight: 500;
        
        &:hover { color: #f56c6c; }
        &.active { color: #f56c6c; font-weight: bold; }
      }
    }
  }
}

/* Banner 区 */
.banner-section {
  background: #f5f5f5;
  margin-bottom: 20px;
  
  .container {
    display: flex;
    position: relative;
    
    .category-sidebar {
      width: 200px;
      background: #333;
      color: #fff;
      position: absolute;
      top: 0;
      left: 20px;
      z-index: 10;
      height: 400px;
      background: rgba(0,0,0,0.7);
      
      ul {
        list-style: none;
        padding: 10px 0;
        margin: 0;
        
        li {
          padding: 0 20px;
          height: 42px;
          line-height: 42px;
          display: flex;
          justify-content: space-between;
          align-items: center;
          cursor: pointer;
          font-size: 14px;
          
          &:hover { background: #f56c6c; }
        }
      }
    }
    
    .banner-content {
      flex: 1;
      margin-left: 200px;
      
      .banner-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
  }
}

/* 核心板块 */
.feature-section {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  
  .feature-card {
    flex: 1;
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    cursor: pointer;
    transition: transform 0.3s;
    box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
    
    &:hover { transform: translateY(-5px); }
    
    .text {
      h3 { margin: 0; font-size: 18px; color: #333; }
      p { margin: 5px 0 0; color: #999; font-size: 14px; }
    }
    
    &.hot { color: #f56c6c; }
    &.unsold { color: #e6a23c; }
    &.news { color: #409eff; }
    &.recommend { color: #67c23a; }
  }
}

/* 商品卡片 */
.section-header-large {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
  
  h2 {
    font-size: 24px;
    margin: 0;
    color: #333;
    .highlight { color: #f56c6c; }
  }
  
  .more {
    color: #666;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    &:hover { color: #f56c6c; }
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 40px;
}

.product-card {
  border: none;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 20px rgba(0,0,0,0.1);
  }
  
  .image-wrapper {
    height: 220px;
    position: relative;
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
      background: #f56c6c;
      color: #fff;
      font-size: 12px;
      padding: 2px 8px;
      border-top-left-radius: 8px;
    }
  }
  
  .card-content {
    padding: 15px;
    
    .product-name {
      font-size: 16px;
      margin-bottom: 8px;
      color: #333;
    }
    
    .product-tags {
      margin-bottom: 10px;
      display: flex;
      justify-content: space-between;
      .origin { font-size: 12px; color: #999; display: flex; align-items: center; }
    }
    
    .price-row {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      margin-bottom: 5px;
      
      .price { color: #f56c6c; font-size: 20px; font-weight: bold; }
      .sales { color: #999; font-size: 12px; }
    }
    
    .merchant-name {
      font-size: 12px;
      color: #999;
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.site-footer {
  background: #333;
  color: #999;
  padding: 40px 0 20px;
  margin-top: 40px;
  
  .footer-links {
    display: flex;
    justify-content: space-around;
    border-bottom: 1px solid #444;
    padding-bottom: 30px;
    margin-bottom: 20px;
    
    .link-group {
      display: flex;
      flex-direction: column;
      gap: 10px;
      
      h4 { color: #fff; font-size: 16px; margin-bottom: 10px; }
      a { color: #999; text-decoration: none; font-size: 14px; &:hover { color: #fff; } }
    }
  }
  
  .copyright {
    text-align: center;
    font-size: 12px;
  }
}

.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .header-search .container {
    flex-direction: column;
    gap: 10px;
  }
  
  .banner-section .category-sidebar {
    display: none;
  }
  
  .banner-section .banner-content {
    margin-left: 0;
  }
}
</style>

