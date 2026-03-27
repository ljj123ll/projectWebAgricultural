<template>
  <div class="unsold-page">
    <section class="hero-card">
      <div class="hero-top">
        <el-button link class="back-btn" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-tag effect="dark" type="warning">实时帮扶池</el-tag>
      </div>
      <div class="hero-main">
        <div>
          <p class="eyebrow">滞销帮扶</p>
          <h2>管理员手动帮扶 + 算法实时推荐</h2>
          <p class="desc">
            页面会根据商品库存、累计销量、上架时长实时重算帮扶商品；管理员手动加入的商品也会优先展示。
          </p>
        </div>
        <div class="hero-side">
          <div class="side-metric">
            <span class="metric-label">当前帮扶商品</span>
            <strong>{{ total }}</strong>
          </div>
          <div class="side-metric">
            <span class="metric-label">最近刷新</span>
            <strong>{{ lastUpdatedText }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="filter-card">
      <el-input
        v-model="keyword"
        placeholder="搜索商品、商家、产地"
        clearable
        @keyup.enter="loadUnsoldProducts"
      />
      <el-select v-model="sourceType">
        <el-option label="全部来源" value="all" />
        <el-option label="管理员手动" value="manual" />
        <el-option label="算法推荐" value="algorithm" />
        <el-option label="双重命中" value="mixed" />
      </el-select>
      <el-select v-model="sortType">
        <el-option label="帮扶优先级" value="support" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
        <el-option label="销量最低优先" value="sales" />
        <el-option label="库存高优先" value="stock" />
        <el-option label="最新上架优先" value="latest" />
      </el-select>
      <el-button type="warning" @click="loadUnsoldProducts">立即更新</el-button>
    </section>

    <section v-loading="loading" class="unsold-list">
      <article
        v-for="item in productList"
        :key="item.id"
        class="unsold-card"
        @click="goDetail(Number(item.id))"
      >
        <img :src="resolveImage(item.productImg)" class="product-img" />
        <div class="card-info">
          <div class="card-header">
            <div>
              <h3>{{ item.productName }}</h3>
              <p class="merchant">商家：{{ item.merchantName || '助农商家' }}</p>
            </div>
            <div class="tag-group">
              <el-tag v-if="item.manualIncluded" type="danger">管理员帮扶</el-tag>
              <el-tag v-if="item.algorithmIncluded" type="warning">算法推荐</el-tag>
              <el-tag effect="plain">紧急度 {{ item.unsalableScore || 0 }}</el-tag>
            </div>
          </div>
          <p class="reason">{{ item.unsalableReason || '系统正在计算帮扶原因' }}</p>
          <div class="meta-row">
            <span>产地 {{ item.originPlace || '待补充' }}</span>
            <span>库存 {{ item.stock || 0 }} 件</span>
            <span>销量 {{ item.salesVolume || 0 }} 件</span>
            <span>上架 {{ item.ageDays || 0 }} 天</span>
          </div>
          <div class="footer-row">
            <span class="sale-price">助农价 ¥{{ Number(item.price || 0).toFixed(2) }}</span>
            <el-button type="warning" size="small">助农购买</el-button>
          </div>
        </div>
      </article>
    </section>

    <el-empty v-if="!loading && productList.length === 0" description="当前没有命中的帮扶商品" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { ArrowLeft } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { getUnsalableProductsPublic } from '@/apis/product';
import { getFullImageUrl } from '@/utils/image';
import { USER_REALTIME_EVENT, parseRealtimePayload } from '@/utils/realtime';
import type { UnsalableProduct } from '@/types';

const router = useRouter();
const keyword = ref('');
const sortType = ref('support');
const sourceType = ref('all');
const loading = ref(false);
const total = ref(0);
const productList = ref<UnsalableProduct[]>([]);
const lastUpdatedAt = ref<number>(0);

let refreshTimer: number | undefined;

const lastUpdatedText = computed(() => {
  if (!lastUpdatedAt.value) return '刚刚';
  return new Date(lastUpdatedAt.value).toLocaleTimeString();
});

const loadUnsoldProducts = async () => {
  loading.value = true;
  try {
    const res = await getUnsalableProductsPublic({
      keyword: keyword.value,
      sortBy: sortType.value,
      sourceType: sourceType.value,
      pageNum: 1,
      pageSize: 50
    });
    productList.value = res?.list || [];
    total.value = Number(res?.total || 0);
    lastUpdatedAt.value = Date.now();
  } finally {
    loading.value = false;
  }
};

const handleRealtimeRefresh = (event: Event) => {
  const payload = parseRealtimePayload(event);
  if (payload.reason !== 'UNSALABLE_UPDATED') return;
  void loadUnsoldProducts();
};

onMounted(() => {
  void loadUnsoldProducts();
  window.addEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
  refreshTimer = window.setInterval(() => {
    void loadUnsoldProducts();
  }, 30000);
});

onUnmounted(() => {
  window.removeEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
  if (refreshTimer) window.clearInterval(refreshTimer);
});

const goDetail = (id: number) => {
  router.push(`/product/${id}`);
};

const resolveImage = (source?: string) => {
  const first = String(source || '')
    .split(',')
    .map(item => item.trim())
    .find(Boolean);
  return getFullImageUrl(first || '');
};
</script>

<style scoped lang="scss">
.unsold-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card,
.filter-card,
.unsold-card {
  background: #fff;
  border-radius: 18px;
  border: 1px solid #ebe4d6;
}

.hero-card {
  padding: 22px 24px;
  background:
    radial-gradient(circle at top right, rgba(230, 162, 60, 0.22), transparent 30%),
    linear-gradient(135deg, #fff9ef 0%, #fff 55%, #fef6ea 100%);
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-main {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 8px 0 10px;
    font-size: 28px;
    color: #7a4b12;
  }
}

.eyebrow {
  margin: 0;
  color: #c17811;
  font-size: 13px;
  letter-spacing: 0.08em;
}

.desc {
  margin: 0;
  max-width: 680px;
  color: #7a6b56;
  line-height: 1.7;
}

.hero-side {
  min-width: 200px;
  display: grid;
  gap: 12px;
}

.side-metric {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(230, 162, 60, 0.2);

  strong {
    display: block;
    margin-top: 6px;
    font-size: 22px;
    color: #8f4d04;
  }
}

.metric-label {
  color: #9a7b52;
  font-size: 13px;
}

.filter-card {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) 160px 170px 120px;
  gap: 12px;
}

.unsold-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.unsold-card {
  padding: 18px;
  display: flex;
  gap: 18px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 30px rgba(185, 126, 36, 0.08);
  }
}

.product-img {
  width: 132px;
  height: 132px;
  border-radius: 14px;
  object-fit: cover;
  background: #f6efe1;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;

  h3 {
    margin: 0 0 6px;
    font-size: 20px;
    color: #3b2a16;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.merchant {
  margin: 0;
  color: #7a6b56;
  font-size: 14px;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.reason {
  margin: 0;
  color: #5f513f;
  line-height: 1.7;
  background: #fff8ee;
  border-radius: 12px;
  padding: 10px 12px;
}

.meta-row,
.footer-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
}

.meta-row {
  color: #8e7a5f;
  font-size: 13px;
}

.footer-row {
  justify-content: space-between;
}

.sale-price {
  color: #d97706;
  font-size: 22px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .hero-main,
  .unsold-card {
    flex-direction: column;
  }

  .filter-card {
    grid-template-columns: 1fr;
  }

  .product-img {
    width: 100%;
    height: 220px;
  }

  .card-header,
  .footer-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .tag-group {
    justify-content: flex-start;
  }
}
</style>
