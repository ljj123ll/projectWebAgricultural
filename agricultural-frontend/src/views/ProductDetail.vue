<template>
  <div class="product-detail-container">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>商品详情</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card class="detail-card">
      <div class="product-content">
        <div class="product-image">
          <el-image :src="product.productImg" fit="cover" />
        </div>
        <div class="product-info">
          <h1>{{ product.productName }}</h1>
          <div class="price">¥ {{ product.price }}</div>
          <div class="meta">
            <span>销量: {{ product.salesVolume }}</span>
            <span>库存: {{ product.stock }}</span>
          </div>
          <div class="description">
            <h3>商品简介</h3>
            <p>{{ product.productDesc }}</p>
          </div>
          <div class="traceability">
             <h3>溯源信息</h3>
             <p>产地: {{ product.originPlace }}</p>
             <el-tag type="success">绿色食品</el-tag>
          </div>
          <div class="actions">
            <el-input-number v-model="quantity" :min="1" :max="product.stock" />
            <el-button type="primary" size="large" @click="handleAddToCart">加入购物车</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getProductDetail } from '@/apis/product';
import { addToCart } from '@/apis/order';
import { ElMessage } from 'element-plus';
import type { Product } from '@/types';

const route = useRoute();
const router = useRouter();
const productId = Number(route.params.id);
const quantity = ref(1);
const product = ref<Product>({
    id: 0,
    productName: '',
    merchantId: 0,
    categoryId: 0,
    price: 0,
    stock: 0,
    productImg: '',
    productDesc: '',
    originPlace: '',
    salesVolume: 0
});

onMounted(async () => {
  try {
    const res = await getProductDetail(productId);
    if (res) {
      product.value = res;
    }
  } catch (e) {
    product.value = {
      id: productId,
      productName: '示例红富士苹果',
      merchantId: 1,
      categoryId: 1,
      price: 29.9,
      stock: 100,
      productImg: 'https://shadow.elemecdn.com/app/element/hamburger.9cf7b091-55e9-11e9-a976-7f4d0b07eef6.png',
      productDesc: '产自山东烟台，口感脆甜，汁多味美。',
      originPlace: '山东烟台',
      salesVolume: 500
    };
  }
});

const handleAddToCart = async () => {
  await addToCart({ productId: product.value.id, productNum: quantity.value });
  ElMessage.success('加入购物车成功');
  router.push('/user/cart');
};
</script>

<style scoped>
.product-detail-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.detail-card {
  margin-top: 20px;
}
.product-content {
  display: flex;
  gap: 40px;
}
.product-image {
  width: 400px;
  height: 400px;
}
.product-image .el-image {
  width: 100%;
  height: 100%;
}
.product-info {
  flex: 1;
}
.price {
  font-size: 28px;
  color: #f56c6c;
  margin: 10px 0;
}
.meta {
  color: #909399;
  margin-bottom: 20px;
  display: flex;
  gap: 20px;
}
.description {
  margin-bottom: 20px;
  line-height: 1.6;
}
.actions {
  margin-top: 30px;
  display: flex;
  gap: 20px;
  align-items: center;
}
</style>
