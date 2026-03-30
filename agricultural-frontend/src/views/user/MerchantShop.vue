<template>
  <div class="merchant-shop-page">
    <el-result
      v-if="loadError && !shop && !loading && !commentLoading"
      icon="warning"
      title="店铺信息暂不可用"
      :sub-title="loadError"
      class="load-error"
    >
      <template #extra>
        <el-button type="primary" @click="reloadAll">重新加载</el-button>
        <el-button plain @click="router.push('/products')">返回商品列表</el-button>
      </template>
    </el-result>

    <template v-else>
    <section class="shop-hero">
      <div class="hero-main">
        <div class="hero-left">
          <div class="hero-avatar">
            <el-avatar :size="96" :src="getFullImageUrl(shop?.qualificationImg)">
              {{ (shop?.shopName || '店').slice(0, 1) }}
            </el-avatar>
          </div>
          <div class="hero-copy">
            <div class="hero-title-row">
              <h1>{{ shop?.shopName || '店铺详情' }}</h1>
              <el-tag v-if="shopTypeText" round effect="dark" type="success">{{ shopTypeText }}</el-tag>
            </div>
            <p class="hero-intro">{{ shop?.shopIntro || '该店铺暂未填写简介，欢迎先看看店内商品与买家评价。' }}</p>
            <div class="shop-tags">
              <el-tag v-for="tag in shopTags" :key="tag" effect="plain" class="shop-tag">{{ tag }}</el-tag>
            </div>
            <div class="shop-address">
              <span class="label">店铺地址</span>
              <span>{{ shop?.shopAddress || '暂未填写详细地址' }}</span>
            </div>
          </div>
        </div>
        <div class="hero-actions">
          <el-button plain @click="router.back()">返回</el-button>
        </div>
      </div>

      <div class="hero-metrics">
        <div class="metric-card metric-rating">
          <span class="metric-label">商家评分</span>
          <div class="metric-rating-row">
            <strong>{{ ratingText }}</strong>
            <el-rate :model-value="shopAverageScore" disabled allow-half />
          </div>
          <p>{{ shop?.reviewCount || 0 }} 条已审核评价汇总</p>
        </div>
        <div class="metric-card">
          <span class="metric-label">在售商品</span>
          <strong>{{ shop?.productCount || total }}</strong>
          <p>当前店铺可购买商品数量</p>
        </div>
        <div class="metric-card">
          <span class="metric-label">累计销量</span>
          <strong>{{ shop?.totalSalesVolume || 0 }}</strong>
          <p>店内全部商品累计售出件数</p>
        </div>
        <div class="metric-card">
          <span class="metric-label">评论总数</span>
          <strong>{{ shop?.commentCount || 0 }}</strong>
          <p>来自该商家全部商品的公开评价</p>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <el-card class="products-card">
        <template #header>
          <div class="card-header">
            <div>
              <h2>店铺商品</h2>
              <p>挑选该商家当前在售的农产品</p>
            </div>
            <span class="count">共 {{ total }} 件</span>
          </div>
        </template>

        <div v-loading="loading" class="product-grid">
          <div v-for="item in products" :key="item.id" class="product-item" @click="goToProduct(item.id)">
            <img :src="getCoverImage(item.productImg)" :alt="item.productName" />
            <div class="product-body">
              <h4>{{ item.productName }}</h4>
              <p class="origin">{{ item.originPlace || '产地待补充' }}</p>
              <div class="product-rating">
                <el-rate :model-value="Number(item.score || 0)" disabled allow-half />
                <span>{{ Number(item.score || 0).toFixed(1) }}</span>
              </div>
              <div class="bottom">
                <span class="price">¥{{ Number(item.price || 0).toFixed(2) }}</span>
                <span class="sales">销量 {{ item.salesVolume || 0 }}</span>
              </div>
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

      <el-card class="reviews-card">
        <template #header>
          <div class="card-header">
            <div>
              <h2>买家评论汇总</h2>
              <p>来自该商家全部商品的公开评论，点击即可跳转到对应商品</p>
            </div>
            <span class="count">共 {{ commentTotal }} 条</span>
          </div>
        </template>

        <div v-loading="commentLoading" class="review-list">
          <template v-if="comments.length">
            <article
              v-for="comment in comments"
              :key="comment.id"
              class="review-item"
              @click="goToProductReview(comment.productId)"
            >
              <div class="review-top">
                <div class="review-user">
                  <el-avatar :src="getFullImageUrl(comment.avatarUrl)" :size="42">
                    {{ (comment.nickname || '买家').slice(0, 1) }}
                  </el-avatar>
                  <div class="review-user-meta">
                    <strong>{{ comment.nickname || '匿名买家' }}</strong>
                    <span>{{ formatDate(comment.createTime) }}</span>
                  </div>
                </div>
                <el-rate :model-value="comment.score" disabled />
              </div>

              <div class="review-product">
                <img :src="getCoverImage(comment.productImg)" :alt="comment.productName || '商品'" />
                <div class="review-product-info">
                  <span class="review-product-label">评价商品</span>
                  <h4>{{ comment.productName || `商品 #${comment.productId}` }}</h4>
                  <el-button link type="primary" @click.stop="goToProductReview(comment.productId)">
                    查看该商品详情
                  </el-button>
                </div>
              </div>

              <p class="review-text">{{ comment.content || '该买家未填写文字评价。' }}</p>

              <div v-if="parseReviewImages(comment).length" class="review-image-list">
                <el-image
                  v-for="(img, idx) in parseReviewImages(comment)"
                  :key="`${comment.id}-${idx}`"
                  :src="img"
                  :preview-src-list="parseReviewImages(comment)"
                  class="review-image-item"
                  fit="cover"
                  preview-teleported
                  @click.stop
                />
              </div>
            </article>
          </template>
          <el-empty v-else description="该商家暂时还没有公开评价" />
        </div>

        <div class="pager" v-if="commentTotal > commentPageSize">
          <el-pagination
            background
            layout="prev, pager, next"
            :page-size="commentPageSize"
            :total="commentTotal"
            :current-page="commentPageNum"
            @current-change="handleCommentPageChange"
          />
        </div>
      </el-card>
    </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getMerchantComments, getMerchantProductsPublic, getMerchantShop } from '@/apis/product';
import type { Product, ProductComment, ShopInfo } from '@/types';
import { getFullImageUrl } from '@/utils/image';

/**
 * 用户端商家主页。
 * 这里集中展示店铺信息、商家评分、在售商品和该商家全部商品的评论汇总。
 */

const SHOP_LABEL_MAP: Record<string, string> = {
  individual: '个体工商户',
  enterprise: '企业店铺',
  cooperative: '农业合作社',
  vegetables: '新鲜蔬菜',
  fruits: '时令水果',
  grains: '粮油米面',
  poultry: '禽畜肉蛋',
  specialty: '地方特产'
};

const route = useRoute();
const router = useRouter();

const merchantId = computed(() => Number(route.params.merchantId || 0));
const loading = ref(false);
const commentLoading = ref(false);
const shop = ref<ShopInfo | null>(null);
const products = ref<Product[]>([]);
const comments = ref<ProductComment[]>([]);
const total = ref(0);
const commentTotal = ref(0);
const pageNum = ref(1);
const commentPageNum = ref(1);
const pageSize = 12;
const commentPageSize = 6;
const loadError = ref('');

const productPlaceholder = (() => {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 720 720">
      <defs>
        <linearGradient id="shopCard" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#f4f8ef" />
          <stop offset="100%" stop-color="#e8efe3" />
        </linearGradient>
      </defs>
      <rect width="720" height="720" rx="42" fill="url(#shopCard)" />
      <circle cx="530" cy="190" r="110" fill="#80bb6f" opacity="0.16" />
      <circle cx="192" cy="558" r="88" fill="#f1c35c" opacity="0.14" />
      <text x="360" y="332" text-anchor="middle" font-size="72" font-family="'Microsoft YaHei', sans-serif" font-weight="700" fill="#2b4730">
        助农商品
      </text>
      <text x="360" y="408" text-anchor="middle" font-size="28" font-family="'Microsoft YaHei', sans-serif" fill="#6f7f72">
        当前图片缺失，已切换为本地占位图
      </text>
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
})();

const localizeTag = (value?: string) => {
  const normalized = String(value || '').trim();
  if (!normalized) return '';
  return SHOP_LABEL_MAP[normalized] || normalized;
};

const shopTypeText = computed(() => localizeTag(shop.value?.shopType));

const shopTags = computed(() => {
  const tags = (shop.value?.categories || '')
    .split(',')
    .map(item => localizeTag(item))
    .filter(Boolean);
  return Array.from(new Set(tags)).slice(0, 6);
});

const shopAverageScore = computed(() => Number(shop.value?.averageScore || 0));
const ratingText = computed(() => {
  if (!shop.value?.reviewCount) return '暂无评分';
  return shopAverageScore.value.toFixed(1);
});

const getCoverImage = (raw?: string) => {
  if (!raw) return productPlaceholder;
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return first ? getFullImageUrl(first) : productPlaceholder;
};

const parseReviewImages = (comment: ProductComment) => {
  const source = comment.mediaUrls || comment.imgUrls || '';
  return source
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .filter(item => {
      const lower = item.toLowerCase();
      return !lower.endsWith('.mp4')
        && !lower.endsWith('.webm')
        && !lower.endsWith('.mov')
        && !lower.endsWith('.avi')
        && !lower.endsWith('.mkv');
    })
    .map(url => getFullImageUrl(url));
};

const formatDate = (date?: string) => {
  if (!date) return '';
  return date.substring(0, 16).replace('T', ' ');
};

// 加载店铺基础资料与评分统计，答辩时可用于说明商家页顶部概览数据来源。
const loadShop = async () => {
  try {
    const res = await getMerchantShop(merchantId.value);
    shop.value = res || null;
    if (!shop.value) {
      loadError.value = '未查询到该店铺，可能已下架或链接无效。';
    }
  } catch (error) {
    console.error('加载店铺信息失败', error);
    shop.value = null;
    loadError.value = '店铺信息加载失败，请稍后重试。';
  }
};

// 加载店铺在售商品列表，左侧商品区和分页都从这里更新。
const loadProducts = async () => {
  loading.value = true;
  try {
    const res = await getMerchantProductsPublic(merchantId.value, { pageNum: pageNum.value, pageSize });
    products.value = res?.list || [];
    total.value = Number(res?.total || 0);
  } catch (error) {
    console.error('加载店铺商品失败', error);
    products.value = [];
    total.value = 0;
    if (!loadError.value) {
      loadError.value = '店铺商品加载失败，请稍后重试。';
    }
  } finally {
    loading.value = false;
  }
};

// 加载该商家全部商品的评论汇总，右侧评论区支持分页并可跳到具体商品详情。
const loadComments = async () => {
  commentLoading.value = true;
  try {
    const res = await getMerchantComments(merchantId.value, { pageNum: commentPageNum.value, pageSize: commentPageSize });
    comments.value = res?.list || [];
    commentTotal.value = Number(res?.total || 0);
  } catch (error) {
    console.error('加载商家评论失败', error);
    comments.value = [];
    commentTotal.value = 0;
    if (!loadError.value) {
      loadError.value = '商家评论加载失败，请稍后重试。';
    }
  } finally {
    commentLoading.value = false;
  }
};

// 商家页统一刷新入口：任意一块加载失败时给出友好提示，便于现场演示时重试。
const reloadAll = async () => {
  loadError.value = '';
  await Promise.allSettled([loadShop(), loadProducts(), loadComments()]);
  if (loadError.value) {
    ElMessage.warning(loadError.value);
  }
};

const handlePageChange = (page: number) => {
  pageNum.value = page;
  loadProducts();
};

const handleCommentPageChange = (page: number) => {
  commentPageNum.value = page;
  loadComments();
};

const goToProduct = (id: number) => {
  router.push(`/product/${id}`);
};

// 从商家评论直接跳转到对应商品评价区域，便于演示“评论汇总 -> 原商品”链路。
const goToProductReview = (id?: number) => {
  if (!id) return;
  router.push(`/product/${id}?tab=reviews`);
};

onMounted(async () => {
  if (!merchantId.value) {
    router.replace('/products');
    return;
  }
  await reloadAll();
});

watch(
  () => route.params.merchantId,
  async (newId, oldId) => {
    if (!newId || newId === oldId) return;
    pageNum.value = 1;
    commentPageNum.value = 1;
    await reloadAll();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
);
</script>

<style scoped lang="scss">
.merchant-shop-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 10px;
}

.load-error {
  padding: 24px 0 8px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
}

.shop-hero {
  position: relative;
  overflow: hidden;
  border: 1px solid #e6ecf1;
  border-radius: 24px;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(137, 186, 95, 0.18), transparent 28%),
    radial-gradient(circle at right center, rgba(244, 179, 80, 0.14), transparent 24%),
    linear-gradient(135deg, #fffef8 0%, #f6fbf4 46%, #f5f8ff 100%);
  box-shadow: 0 18px 42px rgba(48, 78, 48, 0.08);
}

.hero-main {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.hero-left {
  display: flex;
  gap: 18px;
  min-width: 0;
}

.hero-avatar :deep(.el-avatar) {
  border: 4px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(80, 104, 63, 0.16);
}

.hero-copy {
  min-width: 0;
}

.hero-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;

  h1 {
    margin: 0;
    font-size: 34px;
    line-height: 1.2;
    color: #1d3550;
  }
}

.hero-intro {
  margin: 12px 0 0;
  max-width: 780px;
  font-size: 17px;
  line-height: 1.9;
  color: #506175;
}

.shop-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.shop-tag {
  border-color: #b8d694;
  color: #507427;
  background: rgba(255, 255, 255, 0.7);
}

.shop-address {
  margin-top: 18px;
  display: inline-flex;
  gap: 10px;
  align-items: center;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #5b6f85;
  font-size: 14px;

  .label {
    color: #25384d;
    font-weight: 600;
  }
}

.hero-actions {
  display: flex;
  align-items: flex-start;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.metric-card {
  padding: 18px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(223, 233, 240, 0.9);
  backdrop-filter: blur(6px);

  strong {
    display: block;
    margin-top: 10px;
    font-size: 30px;
    color: #1d3550;
    line-height: 1.1;
  }

  p {
    margin: 10px 0 0;
    font-size: 13px;
    color: #7b8797;
    line-height: 1.6;
  }
}

.metric-label {
  font-size: 13px;
  letter-spacing: 0.08em;
  color: #709055;
}

.metric-rating-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  flex-wrap: wrap;

  strong {
    margin-top: 0;
  }
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 20px;
  align-items: start;
}

.products-card,
.reviews-card {
  border-radius: 24px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;

  h2 {
    margin: 0;
    font-size: 22px;
    color: #1f3347;
  }

  p {
    margin: 6px 0 0;
    color: #7b8898;
    font-size: 13px;
  }
}

.count {
  flex-shrink: 0;
  padding: 8px 14px;
  border-radius: 999px;
  background: #f3f7fb;
  color: #567089;
  font-size: 13px;
  font-weight: 600;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.product-item {
  overflow: hidden;
  border: 1px solid #eaf0f5;
  border-radius: 18px;
  cursor: pointer;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: #cadeb9;
    box-shadow: 0 16px 32px rgba(74, 103, 74, 0.12);
  }

  img {
    width: 100%;
    height: 186px;
    object-fit: cover;
    background: #f4f6f8;
  }
}

.product-body {
  padding: 14px;

  h4 {
    margin: 0;
    font-size: 15px;
    line-height: 1.6;
    color: #243447;
    min-height: 48px;
  }
}

.origin {
  margin: 8px 0 0;
  color: #8a95a7;
  font-size: 13px;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: #f08a24;
  font-size: 13px;
  font-weight: 600;
}

.bottom {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.price {
  color: #f56c6c;
  font-size: 22px;
  font-weight: 700;
}

.sales {
  color: #7a8698;
  font-size: 13px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 200px;
  max-height: 1060px;
  overflow-y: auto;
  padding-right: 4px;
}

.review-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid #ebf0f5;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: #d1e3c0;
    box-shadow: 0 12px 26px rgba(77, 96, 77, 0.1);
  }
}

.review-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    color: #243447;
    font-size: 15px;
  }

  span {
    color: #909caf;
    font-size: 12px;
  }
}

.review-product {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 14px;
  padding: 12px;
  border-radius: 14px;
  background: #f7faf6;

  img {
    width: 74px;
    height: 74px;
    border-radius: 12px;
    object-fit: cover;
    background: #eef2f5;
    flex-shrink: 0;
  }
}

.review-product-info {
  min-width: 0;

  h4 {
    margin: 4px 0 0;
    color: #21364b;
    font-size: 15px;
    line-height: 1.5;
  }
}

.review-product-label {
  color: #7c8b9c;
  font-size: 12px;
}

.review-text {
  margin: 14px 0 0;
  color: #536273;
  line-height: 1.9;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.review-image-list {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-image-item {
  width: 84px;
  height: 84px;
  border-radius: 12px;
  overflow: hidden;
}

.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-list {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
}

@media (max-width: 768px) {
  .shop-hero {
    padding: 20px;
    border-radius: 20px;
  }

  .hero-main,
  .hero-left {
    flex-direction: column;
  }

  .hero-title-row h1 {
    font-size: 28px;
  }

  .hero-intro {
    font-size: 15px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-grid {
    grid-template-columns: 1fr 1fr;
  }

  .review-top,
  .review-product {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 560px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
