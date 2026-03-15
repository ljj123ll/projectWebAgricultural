<template>
  <div class="product-detail-page">
    <div class="container">
      <div class="header-nav" style="display: flex; align-items: center; margin-bottom: 20px;">
        <el-button link @click="$router.back()" class="back-btn" style="margin-right: 15px; font-size: 18px;">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/" class="breadcrumb" style="margin-bottom: 0;">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ product.categoryName || '商品详情' }}</el-breadcrumb-item>
          <el-breadcrumb-item>{{ product.productName }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="product-main" v-loading="loading">
        <!-- 左侧图片展示 -->
        <div class="gallery">
          <div class="main-image-wrapper">
            <el-image 
              :src="currentImage" 
              fit="contain" 
              class="main-image"
              :preview-src-list="imageList" 
              :initial-index="currentImageIndex"
            />
          </div>
          <div class="thumbnail-list" v-if="imageList.length > 1">
            <div 
              v-for="(img, idx) in imageList" 
              :key="idx" 
              class="thumbnail-item"
              :class="{ active: currentImage === img }"
              @mouseenter="setCurrentImage(img, idx)"
            >
              <img :src="img" />
            </div>
          </div>
        </div>

        <!-- 右侧商品信息 -->
        <div class="info-section">
          <h1 class="product-title">{{ product.productName }}</h1>
          <p class="product-desc" v-if="product.productDesc">{{ product.productDesc }}</p>
          
          <div class="price-panel">
            <div class="price-row">
              <span class="label">价格</span>
              <span class="currency">¥</span>
              <span class="price">{{ product.price }}</span>
            </div>
            <div class="stats-row">
              <div class="stat-item">
                <span>月销量</span>
                <span class="value">{{ product.salesVolume || 0 }}</span>
              </div>
              <el-divider direction="vertical" />
              <div class="stat-item">
                <span>累计评价</span>
                <span class="value">{{ product.score ? '99+' : '0' }}</span>
              </div>
              <el-divider direction="vertical" />
              <div class="stat-item">
                <span>库存</span>
                <span class="value">{{ product.stock }}</span>
              </div>
            </div>
          </div>

          <div class="meta-info">
            <dl class="meta-row">
              <dt>产地</dt>
              <dd>{{ product.originPlace }}</dd>
            </dl>
            <dl class="meta-row">
              <dt>溯源</dt>
              <dd>
                <el-button type="success" link @click="showTrace = true">
                  <el-icon><CircleCheck /></el-icon> 查看溯源档案
                </el-button>
              </dd>
            </dl>
            <dl class="meta-row" v-if="merchant">
              <dt>商家</dt>
              <dd class="merchant-link" @click="goToMerchant">
                {{ merchant.merchantName }}
                <el-tag size="small" type="warning" effect="plain">金牌农户</el-tag>
              </dd>
            </dl>
          </div>

          <el-divider border-style="dashed" />

          <div class="purchase-area">
            <div class="quantity-row">
              <span class="label">数量</span>
              <el-input-number 
                v-model="quantity" 
                :min="1" 
                :max="product.stock" 
                size="large"
              />
              <span class="stock-warning" v-if="product.stock < 10 && product.stock > 0">
                仅剩 {{ product.stock }} 件
              </span>
              <span class="stock-warning" v-if="product.stock === 0">缺货</span>
            </div>

            <div class="action-btns">
              <el-button 
                type="warning" 
                size="large" 
                class="btn-cart" 
                :disabled="product.stock === 0"
                @click="handleAddToCart"
              >
                加入购物车
              </el-button>
              <el-button 
                type="danger" 
                size="large" 
                class="btn-buy"
                :disabled="product.stock === 0"
                @click="handleBuyNow"
              >
                立即购买
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 下方详情与评价 -->
      <el-row :gutter="20" class="detail-container">
        <!-- 左侧商家推荐 (可选) -->
        <el-col :span="5" class="hidden-xs-only">
          <el-card class="merchant-card">
            <template #header>
              <div class="card-header">商家推荐</div>
            </template>
            <div class="recommend-list">
              <!-- 这里可以放推荐商品，暂时留空 -->
              <el-empty description="暂无推荐" :image-size="60" />
            </div>
          </el-card>
        </el-col>
        
        <!-- 右侧详情内容 -->
        <el-col :xs="24" :sm="19">
          <el-tabs v-model="activeTab" class="product-tabs" type="border-card">
            <el-tab-pane label="商品详情" name="detail">
              <div class="detail-content">
                <div class="attr-list">
                  <div class="attr-item">
                    <span class="label">品类</span>
                    <span class="value">{{ product.categoryName }}</span>
                  </div>
                  <div class="attr-item">
                    <span class="label">产地</span>
                    <span class="value">{{ product.originPlaceDetail || product.originPlace }}</span>
                  </div>
                  <div class="attr-item">
                    <span class="label">种植周期</span>
                    <span class="value">{{ product.plantingCycle || '自然生长' }}</span>
                  </div>
                  <div class="attr-item">
                    <span class="label">施肥方式</span>
                    <span class="value">{{ product.fertilizerType || '无公害' }}</span>
                  </div>
                </div>
                
                <div class="rich-text">
                   <!-- 模拟详情图 -->
                   <p>此处展示商品详情图文介绍...</p>
                   <img v-if="product.productImg" :src="product.productImg" style="max-width: 100%; margin-top: 20px;" />
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="累计评价" name="reviews">
              <el-empty description="暂无评价" />
            </el-tab-pane>
            <el-tab-pane label="售后保障" name="guarantee">
              <div class="guarantee-content">
                <h4>服务承诺</h4>
                <p>卖家承诺48小时内发货，坏果包赔。</p>
                <h4>退换货说明</h4>
                <p>生鲜产品不支持7天无理由退货，如有质量问题请在签收后24小时内联系客服。</p>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-col>
      </el-row>

    </div>

    <!-- 溯源弹窗 -->
    <el-dialog
      v-model="showTrace"
      title="商品溯源档案"
      width="400px"
      center
      align-center
    >
      <div class="trace-dialog-body">
        <div class="qr-wrapper">
          <img :src="product.qrCodeUrl || 'https://via.placeholder.com/200?text=QRCode'" class="qr-code" />
          <p class="scan-tip">扫描二维码查看完整信息</p>
        </div>
        <div class="trace-timeline">
          <el-timeline>
            <el-timeline-item type="primary" :hollow="true" timestamp="种植">
              {{ product.plantingCycle || '周期未知' }}
            </el-timeline-item>
            <el-timeline-item type="success" :hollow="true" timestamp="施肥">
              {{ product.fertilizerType || '有机肥' }}
            </el-timeline-item>
            <el-timeline-item type="warning" :hollow="true" timestamp="采摘">
              {{ product.storageMethod || '常温保存' }}
            </el-timeline-item>
             <el-timeline-item type="info" :hollow="true" timestamp="运输">
              {{ product.transportMethod || '普通物流' }}
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { CircleCheck, ArrowLeft } from '@element-plus/icons-vue';
import { getProductDetail, getMerchantShop } from '@/apis/product';
import { addToCart } from '@/apis/order'; // Import API
import { useUserStore } from '@/stores/modules/user';
import type { Product, ShopInfo } from '@/types';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const productId = Number(route.params.id);
const product = ref<Product>({} as Product);
const merchant = ref<ShopInfo | null>(null);
const quantity = ref(1);
const activeTab = ref('detail');
const showTrace = ref(false);

const currentImage = ref('');
const currentImageIndex = ref(0);

// 图片列表处理（由于后端目前只返回单图，这里做个兼容，如果是多图可以扩展）
const imageList = computed(() => {
  if (product.value.productImg) {
    return [product.value.productImg];
  }
  return [];
});

const setCurrentImage = (img: string, idx: number) => {
  currentImage.value = img;
  currentImageIndex.value = idx;
};

onMounted(async () => {
  if (productId) {
    await loadProductData();
  }
});

const loadProductData = async () => {
  loading.value = true;
  try {
    const res = await getProductDetail(productId);
    if (res) {
      product.value = res;
      currentImage.value = res.productImg;
      
      // 加载商家信息
      if (res.merchantId) {
        loadMerchantInfo(res.merchantId);
      }
    }
  } catch (error) {
    console.error('加载商品详情失败', error);
    ElMessage.error('商品不存在或已下架');
  } finally {
    loading.value = false;
  }
};

const loadMerchantInfo = async (merchantId: number) => {
  try {
    const res = await getMerchantShop(merchantId);
    if (res) {
      merchant.value = res;
    }
  } catch (error) {
    console.warn('加载商家信息失败', error);
  }
};

const handleAddToCart = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录');
    router.push(`/login?redirect=${route.fullPath}`);
    return;
  }

  if (!product.value.id) return;

  try {
    await addToCart({
      productId: product.value.id,
      productNum: quantity.value
    });
    ElMessage.success('已加入购物车');
  } catch (error) {
    console.error(error);
  }
};

const handleBuyNow = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录');
    router.push(`/login?redirect=${route.fullPath}`);
    return;
  }
  
  // 立即购买通常是直接跳转到确认订单页，携带商品信息
  // 或者先加入购物车再跳转
  // 这里简化为先加入购物车，然后跳转到购物车页面（或者直接跳转确认订单页，如果支持直接购买）
  // 接口测试顺序中提到：24 创建订单-直接购买 POST /user/orders { productItems: [...] }
  // 所以可以直接跳转到确认订单页，并传递参数
  
  // 方式1：加入购物车 -> 购物车页
  // await handleAddToCart();
  // router.push('/cart');

  // 方式2：直接跳转确认订单页 (需确认订单页支持 query 参数或 store 传递)
  // 我们使用 query 传递 product info (简单方式)
  router.push({
    path: '/order-confirm',
    query: {
      productId: product.value.id,
      productNum: quantity.value
    }
  });
};

const goToMerchant = () => {
  if (merchant.value?.id) {
    // 假设有商家详情页
    // router.push(`/merchant/${merchant.value.id}`);
  }
};
</script>

<style scoped lang="scss">
.product-detail-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding-bottom: 40px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.breadcrumb {
  margin-bottom: 20px;
}

.product-main {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
  
  @media (max-width: 768px) {
    flex-direction: column;
    padding: 15px;
    gap: 20px;
  }
}

.gallery {
  width: 400px;
  flex-shrink: 0;
  
  @media (max-width: 768px) {
    width: 100%;
  }
  
  .main-image-wrapper {
    width: 100%;
    height: 400px;
    border: 1px solid #eee;
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .main-image {
      width: 100%;
      height: 100%;
    }
  }
  
  .thumbnail-list {
    display: flex;
    gap: 10px;
    overflow-x: auto;
    
    .thumbnail-item {
      width: 60px;
      height: 60px;
      border: 2px solid transparent;
      cursor: pointer;
      border-radius: 4px;
      
      &.active {
        border-color: #409eff;
      }
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
  }
}

.info-section {
  flex: 1;
  
  .product-title {
    font-size: 24px;
    color: #303133;
    margin-bottom: 10px;
    font-weight: bold;
  }
  
  .product-desc {
    color: #909399;
    font-size: 14px;
    margin-bottom: 20px;
  }
  
  .price-panel {
    background: #fff5f5;
    padding: 15px 20px;
    border-radius: 4px;
    margin-bottom: 20px;
    
    .price-row {
      margin-bottom: 10px;
      .label { color: #999; font-size: 14px; margin-right: 10px; }
      .currency { color: #f56c6c; font-size: 18px; font-weight: bold; }
      .price { color: #f56c6c; font-size: 28px; font-weight: bold; }
    }
    
    .stats-row {
      display: flex;
      align-items: center;
      color: #909399;
      font-size: 13px;
      
      .stat-item {
        padding: 0 10px;
        &:first-child { padding-left: 0; }
        
        .value { color: #409eff; margin-left: 4px; }
      }
    }
  }
  
  .meta-info {
    margin-bottom: 20px;
    
    .meta-row {
      display: flex;
      margin-bottom: 10px;
      font-size: 14px;
      
      dt {
        width: 60px;
        color: #999;
      }
      
      dd {
        flex: 1;
        color: #606266;
        margin: 0;
        
        &.merchant-link {
          color: #409eff;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }
  }
  
  .purchase-area {
    margin-top: 30px;
    
    .quantity-row {
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      
      .label { margin-right: 15px; color: #606266; }
      .stock-warning { margin-left: 15px; color: #f56c6c; font-size: 13px; }
    }
    
    .action-btns {
      display: flex;
      gap: 20px;
      
      .btn-cart { width: 140px; }
      .btn-buy { width: 140px; }
    }
  }
}

.detail-container {
  margin-top: 20px;
}

.detail-content {
  padding: 20px;
  
  .attr-list {
    display: flex;
    flex-wrap: wrap;
    border: 1px solid #ebeef5;
    border-right: none;
    border-bottom: none;
    margin-bottom: 20px;
    
    .attr-item {
      width: 33.33%;
      display: flex;
      border-right: 1px solid #ebeef5;
      border-bottom: 1px solid #ebeef5;
      
      .label {
        width: 90px;
        background: #f9fafc;
        padding: 10px;
        color: #909399;
        font-size: 13px;
        display: flex;
        align-items: center;
      }
      
      .value {
        flex: 1;
        padding: 10px;
        color: #606266;
        font-size: 13px;
      }
    }
  }
}

.trace-dialog-body {
  text-align: center;
  
  .qr-wrapper {
    margin-bottom: 20px;
    .qr-code { width: 150px; height: 150px; }
    .scan-tip { color: #909399; font-size: 12px; margin-top: 5px; }
  }
  
  .trace-timeline {
    text-align: left;
    padding: 0 20px;
  }
}

@media (max-width: 768px) {
  .attr-list .attr-item {
    width: 100% !important;
  }
}
</style>
