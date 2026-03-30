<template>
  <div class="home-page">
    <!-- 顶部通栏 -->
    <div class="top-nav">
      <div class="container">
        <div class="left">
          <span>欢迎来到助农电商平台！精选产地好物，天天有新鲜。</span>
          <template v-if="!userInfo">
            <el-button link type="primary" @click="safePush('/login')">请登录</el-button>
            <el-button link @click="safePush('/register')">免费注册</el-button>
          </template>
          <template v-else>
            <span class="username">你好，{{ userInfo.nickname || userInfo.phone }}</span>
            <el-button link @click="logout">退出</el-button>
          </template>
        </div>
        <div class="right">
          <el-button link @click="safePush('/merchant/login')">商家入口</el-button>
        </div>
      </div>
    </div>

    <!-- 头部搜索区 -->
    <div class="header-search">
      <div class="container">
        <div class="logo" @click="safePush('/')">
          <div class="logo-icon">
            <el-icon><Shop /></el-icon>
          </div>
          <div class="logo-text">
            <h1>助农电商</h1>
            <p>田间好货 直达餐桌</p>
          </div>
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
        </div>
        <div class="cart-entry" @click="safePush('/cart')">
          <el-badge :value="cartCount" class="item" :hidden="cartCount === 0">
            <el-button size="large" plain>
              <el-icon><ShoppingCart /></el-icon>
              我的购物车
            </el-button>
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
          <li @click="safePush('/products')">全部商品</li>
          <li @click="safePush('/unsold')">滞销专区</li>
          <li @click="safePush('/news')">助农资讯</li>
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
        <div class="feature-card hot" @click="safePush('/products?sort=sales')">
          <div class="text">
            <h3>助农热销</h3>
            <p>大家都在买</p>
          </div>
          <el-icon size="40"><TrendCharts /></el-icon>
        </div>
        <div class="feature-card unsold" @click="safePush('/unsold')">
          <div class="text">
            <h3>滞销帮扶</h3>
            <p>爱心助农</p>
          </div>
          <el-icon size="40"><WarningFilled /></el-icon>
        </div>
        <div class="feature-card news" @click="safePush('/news')">
          <div class="text">
            <h3>产地资讯</h3>
            <p>了解源头</p>
          </div>
          <el-icon size="40"><Document /></el-icon>
        </div>
        <div class="feature-card recommend" @click="safePush('/products')">
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
          <div class="more" @click="safePush('/products')">查看更多 <el-icon><ArrowRight /></el-icon></div>
        </div>
        
        <el-skeleton :loading="loading" animated :count="4">
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
                  <img :src="getCoverImage(product.productImg)" class="product-image" />
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
      <section class="section-block" v-for="cat in categoryOptions" :key="cat.id">
         <div class="section-header-large">
          <h2>{{ cat.categoryName }} · 销量榜</h2>
          <div class="more" @click="filterByCategory(cat.id)">更多 <el-icon><ArrowRight /></el-icon></div>
        </div>
        <div class="product-grid">
           <el-card
                v-for="(product, index) in categoryTopProductsMap[cat.id] || []"
                :key="product.id"
                class="product-card"
                :body-style="{ padding: '0px' }"
                shadow="hover"
                @click="goToProduct(product.id)"
              >
                <div class="rank-badge">TOP {{ index + 1 }}</div>
                <div class="image-wrapper">
                  <img :src="getCoverImage(product.productImg)" class="product-image" />
                </div>
                <div class="card-content">
                  <h3 class="product-name text-ellipsis">{{ product.productName }}</h3>
                   <div class="price-row">
                    <span class="price">¥{{ product.price }}</span>
                    <span class="sales">已售 {{ product.salesVolume || 0 }}</span>
                  </div>
                </div>
            </el-card>
            <el-empty
              v-if="(categoryTopProductsMap[cat.id] || []).length === 0"
              description="该分类暂无商品"
            />
        </div>
      </section>

      <section class="section-block" v-if="newsList.length > 0">
        <div class="section-header-large">
          <h2>助农资讯精选</h2>
          <div class="more" @click="safePush('/news')">更多 <el-icon><ArrowRight /></el-icon></div>
        </div>
        <div class="news-grid">
          <div
            v-for="item in newsList.slice(0, 4)"
            :key="item.id"
            class="news-card"
            @click="goToNews(item.id)"
          >
            <img :src="getNewsCover(item.coverImg)" class="news-cover" />
            <div class="news-content">
              <h4 class="text-ellipsis">{{ item.title }}</h4>
              <p>{{ formatDate(item.createTime) }}</p>
            </div>
          </div>
        </div>
      </section>

    </div>
    
    <!-- 底部 Footer -->
    <footer class="site-footer">
      <div class="container">
        <div class="footer-links">
          <div class="link-group">
            <h4>关于我们</h4>
            <router-link to="/home">产地直供平台</router-link>
            <router-link to="/unsold">滞销帮扶专区</router-link>
            <router-link to="/news">平台动态</router-link>
          </div>
          <div class="link-group">
            <h4>帮助中心</h4>
            <router-link to="/products">选购农产品</router-link>
            <router-link to="/orders">订单与售后</router-link>
            <router-link to="/messages">站内消息通知</router-link>
          </div>
          <div class="link-group">
            <h4>商家服务</h4>
            <router-link to="/merchant/register">申请入驻平台</router-link>
            <router-link to="/merchant/login">商家后台登录</router-link>
            <router-link to="/merchant/login">订单与售后处理</router-link>
          </div>
        </div>
        <div class="copyright">
          © 2026 助农电商平台 | 聚焦农产品助销、滞销帮扶、订单履约与售后协同
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { Menu, ArrowRight, TrendCharts, WarningFilled, Document, StarFilled, Location, Shop, ShoppingCart } from '@element-plus/icons-vue';
import { searchProducts } from '@/apis/product';
import { listNews, getUserInfo, logout as userLogout } from '@/apis/user';
import { getCart } from '@/apis/order';
import type { Product, News, ProductCategory } from '@/types';
import { getFullImageUrl } from '@/utils/image';
import { ElMessage } from 'element-plus';
import { PRODUCT_CATEGORY_OPTIONS, getProductCategoryName } from '@/constants/category';

const router = useRouter();
const loading = ref(false);
const userInfo = ref<any>(null);
const cartCount = ref(0);
const navigationPending = ref(false);
let navigationTimer: ReturnType<typeof setTimeout> | null = null;
let startupRequestNo = 0;
let homeAlive = true;

const createSvgPlaceholder = (config: {
  title: string;
  subtitle: string;
  start: string;
  end: string;
  accent: string;
}) => {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 420">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${config.start}" />
          <stop offset="100%" stop-color="${config.end}" />
        </linearGradient>
      </defs>
      <rect width="1200" height="420" rx="32" fill="url(#bg)" />
      <circle cx="980" cy="98" r="118" fill="rgba(255,255,255,0.14)" />
      <circle cx="1092" cy="308" r="146" fill="rgba(255,255,255,0.1)" />
      <circle cx="168" cy="332" r="98" fill="rgba(255,255,255,0.08)" />
      <rect x="88" y="84" width="132" height="12" rx="6" fill="${config.accent}" opacity="0.9" />
      <text x="88" y="178" font-size="56" font-family="'Microsoft YaHei', sans-serif" font-weight="700" fill="#ffffff">
        ${config.title}
      </text>
      <text x="88" y="236" font-size="28" font-family="'Microsoft YaHei', sans-serif" fill="rgba(255,255,255,0.88)">
        ${config.subtitle}
      </text>
      <rect x="88" y="286" width="224" height="52" rx="26" fill="rgba(255,255,255,0.18)" />
      <text x="120" y="320" font-size="24" font-family="'Microsoft YaHei', sans-serif" fill="#ffffff">
        产地直供 · 助农增收
      </text>
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
};

const createCardPlaceholder = (title: string, accent: string) => {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 450">
      <defs>
        <linearGradient id="cardBg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#f4f8ef" />
          <stop offset="100%" stop-color="#e6efe0" />
        </linearGradient>
      </defs>
      <rect width="800" height="450" rx="28" fill="url(#cardBg)" />
      <circle cx="644" cy="112" r="92" fill="${accent}" opacity="0.16" />
      <circle cx="162" cy="346" r="74" fill="${accent}" opacity="0.1" />
      <rect x="92" y="112" width="132" height="14" rx="7" fill="${accent}" opacity="0.82" />
      <text x="92" y="206" font-size="52" font-family="'Microsoft YaHei', sans-serif" font-weight="700" fill="#27412e">
        ${title}
      </text>
      <text x="92" y="258" font-size="26" font-family="'Microsoft YaHei', sans-serif" fill="#617265">
        平台本地占位图，离线演示也能稳定显示
      </text>
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
};

// Banner 数据
const banners = [
  {
    id: 1,
    title: '绿色助农',
    imgUrl: createSvgPlaceholder({
      title: '从田间到餐桌',
      subtitle: '精选产地好物，离线演示也能稳定打开',
      start: '#3d8f46',
      end: '#8bc76d',
      accent: '#ffd76a'
    })
  },
  {
    id: 2,
    title: '春季特惠',
    imgUrl: createSvgPlaceholder({
      title: '助农专区精选',
      subtitle: '热销商品、滞销帮扶、溯源档案一页直达',
      start: '#f08a24',
      end: '#f4c95d',
      accent: '#2d5d34'
    })
  }
];

const searchKeyword = ref('');
const categoryOptions = ref<ProductCategory[]>([]);
const products = ref<Product[]>([]);
const categoryTopProductsMap = reactive<Record<number, Product[]>>({});
const defaultNewsCover = createCardPlaceholder('助农资讯', '#5d9d52');
const defaultProductCover = createCardPlaceholder('助农好物', '#3f9f43');

const fixedCategoryOptions: ProductCategory[] = PRODUCT_CATEGORY_OPTIONS.map((item) => ({
  id: item.value,
  categoryName: item.label,
  parentId: 0,
  categoryLevel: 1
}));
const newsList = ref<News[]>([]);

onMounted(async () => {
  const requestNo = ++startupRequestNo;
  const token = localStorage.getItem('token');
  const startupTasks = [
    loadCategories(requestNo),
    loadProducts(requestNo),
    loadNews(requestNo),
    loadCategoryTopProducts(requestNo)
  ];
  if (token) {
    startupTasks.unshift(checkLogin(requestNo), syncCartCount(requestNo));
  }
  await Promise.all(startupTasks);
});

onUnmounted(() => {
  homeAlive = false;
  if (navigationTimer) {
    clearTimeout(navigationTimer);
    navigationTimer = null;
  }
});

const isLatestStartupRequest = (requestNo: number) => {
  return homeAlive && requestNo === startupRequestNo;
};

const releaseNavigationLock = () => {
  if (navigationTimer) {
    clearTimeout(navigationTimer);
  }
  navigationTimer = setTimeout(() => {
    navigationPending.value = false;
  }, 300);
};

const safePush = async (target: string | { path: string; query?: Record<string, any> }) => {
  if (navigationPending.value) return;
  navigationPending.value = true;
  try {
    await router.push(target as any);
  } catch (error) {
    console.warn('页面跳转失败', error);
  } finally {
    releaseNavigationLock();
  }
};

const checkLogin = async (requestNo: number) => {
  try {
    const res = await getUserInfo();
    if (res && isLatestStartupRequest(requestNo)) {
      userInfo.value = res;
    }
  } catch (e) {
    if (isLatestStartupRequest(requestNo)) {
      localStorage.removeItem('token');
      userInfo.value = null;
    }
  }
};

const syncCartCount = async (requestNo: number) => {
  try {
    const cartItems = await getCart();
    if (!isLatestStartupRequest(requestNo)) return;
    cartCount.value = (cartItems || []).reduce((sum, item) => sum + Number(item.productNum || 0), 0);
  } catch (error) {
    if (isLatestStartupRequest(requestNo)) {
      cartCount.value = 0;
    }
  }
};

const logout = async () => {
  try {
    await userLogout();
    userInfo.value = null;
    cartCount.value = 0;
    localStorage.removeItem('token');
    ElMessage.success('已退出登录');
  } catch (e) {
    console.error(e);
  }
};

const loadCategories = async (requestNo: number) => {
  if (!isLatestStartupRequest(requestNo)) return;
  categoryOptions.value = fixedCategoryOptions;
};

const loadProducts = async (requestNo: number) => {
  loading.value = true;
  try {
    const res = await searchProducts({ pageNum: 1, pageSize: 8, sortBy: 'sales' });
    if (res && res.list && isLatestStartupRequest(requestNo)) {
      products.value = res.list;
    }
  } catch (error) {
    console.error(error);
  } finally {
    if (isLatestStartupRequest(requestNo)) {
      loading.value = false;
    }
  }
};

const loadNews = async (requestNo: number) => {
  try {
    const res = await listNews({ pageNum: 1, pageSize: 6 });
    if (res && res.list && isLatestStartupRequest(requestNo)) {
      newsList.value = res.list;
    }
  } catch (error) {}
};

const loadCategoryTopProducts = async (requestNo: number) => {
  try {
    const tasks = fixedCategoryOptions.map((cat) =>
      searchProducts({
        pageNum: 1,
        pageSize: 4,
        categoryId: cat.id,
        sortBy: 'sales'
      })
    );
    const responses = await Promise.all(tasks);
    if (!isLatestStartupRequest(requestNo)) return;
    fixedCategoryOptions.forEach((cat, index) => {
      categoryTopProductsMap[cat.id] = responses[index]?.list || [];
    });
  } catch (error) {
    console.error('加载分类热销商品失败', error);
    if (!isLatestStartupRequest(requestNo)) return;
    fixedCategoryOptions.forEach((cat) => {
      categoryTopProductsMap[cat.id] = [];
    });
  }
};

const getCategoryText = (product: Product) => {
  return getProductCategoryName(product.categoryId, product.categoryName) || '生鲜果蔬';
};

const getCoverImage = (raw?: string) => {
  if (!raw) return defaultProductCover;
  const first = raw.split(',').map((item) => item.trim()).find(Boolean) || '';
  return first ? getFullImageUrl(first) : defaultProductCover;
};

const getNewsCover = (raw?: string) => {
  return raw ? getFullImageUrl(raw) : defaultNewsCover;
};

const formatDate = (value?: string) => {
  if (!value) return '刚刚更新';
  return String(value).slice(0, 10);
};

const handleSearch = () => {
  void safePush({
    path: '/products',
    query: { keyword: searchKeyword.value.trim() }
  });
};

const filterByCategory = (id: number) => {
  void safePush(`/products?category=${id}`);
};

const goToProduct = (id: number) => {
  void safePush(`/product/${id}`);
};

const goToNews = (id: number) => {
  void safePush(`/news/${id}`);
};
</script>

<style scoped lang="scss">
.home-page {
  background: linear-gradient(180deg, #f1f8ef 0%, #f7f8fb 220px, #f7f8fb 100%);
  min-height: 100vh;
}

.main-container {
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 22px clamp(12px, 2.2vw, 20px) 0;
  box-sizing: border-box;
}

.section-block {
  background: #fff;
  border: 1px solid #e3ebe1;
  border-radius: 14px;
  padding: 22px 22px 8px;
  margin-bottom: 18px;
  box-shadow: 0 10px 24px rgba(32, 64, 37, 0.05);
}

.container {
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 0 clamp(12px, 2.2vw, 20px);
  box-sizing: border-box;
}

/* 顶部通栏 */
.top-nav {
  background: #edf5ea;
  border-bottom: 1px solid #d8e7d2;
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
      .username { color: #3f9f43; font-weight: 600; }
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
  padding: 22px 0;
  border-bottom: 1px solid #e4ece2;
  
  .container {
    display: flex;
    align-items: center;
    gap: 40px;
    
    .logo {
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 10px;

      .logo-icon {
        width: 48px;
        height: 48px;
        border-radius: 14px;
        background: linear-gradient(135deg, #6dcf63, #3f9f43);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        box-shadow: 0 8px 18px rgba(63, 159, 67, 0.28);
      }

      .logo-text {
        h1 {
          margin: 0;
          font-size: 24px;
          color: #21452a;
          letter-spacing: 1px;
        }

        p {
          margin: 2px 0 0;
          color: #8a9687;
          font-size: 12px;
        }
      }
    }
    
    .search-box {
      flex: 1;
      
      .search-input {
        :deep(.el-input-group__append) {
          background-color: #3f9f43;
          border-color: #3f9f43;
          color: #fff;
          font-weight: bold;
          
          &:hover { opacity: 0.9; }
        }
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
  border-bottom: 1px solid #e4ece2;
  
  .container {
    display: flex;
    height: 40px;
    
    .all-categories {
      width: 200px;
      background: linear-gradient(135deg, #4fb84f, #3f9f43);
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
        &.active { color: #3f9f43; font-weight: bold; }
      }
    }
  }
}

/* Banner 区 */
.banner-section {
  background: transparent;
  margin-bottom: 24px;
  
  .container {
    display: grid;
    grid-template-columns: 200px minmax(0, 1fr);
    position: relative;
    
    .category-sidebar {
      width: 200px;
      background: #2e4433;
      color: #fff;
      position: relative;
      z-index: 1;
      height: 400px;
      border-radius: 10px 0 0 10px;
      
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
      width: 100%;
      min-width: 0;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 12px 28px rgba(25, 50, 28, 0.15);
      
      .banner-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 10px;
      }
    }
  }
}

/* 核心板块 */
.feature-section {
  display: flex;
  gap: 20px;
  margin-bottom: 18px;
  
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
    border: 1px solid #e3ebe1;
    
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
  margin-bottom: 16px;
  
  h2 {
    font-size: 24px;
    margin: 0;
    color: #213229;
    .highlight { color: #3f9f43; }
  }
  
  .more {
    color: #666;
    cursor: pointer;
    font-size: 14px;
    display: flex;
    align-items: center;
    &:hover { color: #3f9f43; }
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 14px;
}

.product-card {
  border: 1px solid #e3ebe1;
  border-radius: 10px;
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 20px rgba(0,0,0,0.1);
  }

  .rank-badge {
    position: absolute;
    top: 10px;
    left: 10px;
    z-index: 2;
    font-size: 12px;
    color: #fff;
    background: linear-gradient(135deg, #ff7a45, #f56c6c);
    border-radius: 999px;
    padding: 2px 8px;
    box-shadow: 0 4px 10px rgba(245, 108, 108, 0.32);
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
  background: #223228;
  color: #aeb8ad;
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
      a { color: #aeb8ad; text-decoration: none; font-size: 14px; &:hover { color: #fff; } }
    }
  }
  
  .copyright {
    text-align: center;
    font-size: 12px;
    color: #9ca89a;
  }
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.news-card {
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e3ebe1;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 10px 20px rgba(18, 40, 20, 0.12);
  }
}

.news-cover {
  width: 100%;
  height: 155px;
  object-fit: cover;
}

.news-content {
  padding: 12px 14px;

  h4 {
    margin: 0;
    font-size: 15px;
    color: #1f2c24;
  }

  p {
    margin: 8px 0 0;
    font-size: 12px;
    color: #8a9788;
  }
}

.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1024px) {
  .feature-section {
    flex-wrap: wrap;
    gap: 14px;
  }

  .feature-section .feature-card {
    flex: 0 0 calc(50% - 7px);
    min-width: 0;
  }

  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .main-nav .container {
    overflow-x: auto;
    scrollbar-width: thin;
  }

  .main-nav .nav-list {
    min-width: max-content;
  }
}

@media (max-width: 768px) {
  .main-container {
    padding: 12px 12px 0;
  }

  .section-block {
    padding: 14px 12px 6px;
    border-radius: 10px;
  }

  .feature-section {
    flex-wrap: wrap;
    gap: 12px;
  }

  .feature-section .feature-card {
    flex: 0 0 calc(50% - 6px);
    min-width: 0;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .news-grid {
    grid-template-columns: repeat(1, 1fr);
  }
  
  .header-search .container {
    flex-direction: column;
    gap: 10px;
  }
  
  .banner-section .category-sidebar {
    display: none;
  }

  .banner-section .container {
    display: block;
  }
  
  .banner-section .banner-content {
    margin-left: 0;
  }
}
</style>

