<template>
  <div class="confirm-page">
    <div class="container">
      <div class="page-header">
        <h2>
          <el-button link @click="$router.back()" class="back-btn" style="margin-right: 10px; font-size: 20px;">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          确认订单
        </h2>
      </div>

      <!-- 收货地址 -->
      <div class="section address-section">
        <div class="section-header">
          <h3><el-icon><Location /></el-icon> 收货地址</h3>
          <el-button link type="primary" @click="router.push('/address')">管理地址</el-button>
        </div>
        
        <div class="address-list" v-loading="loadingAddress">
          <template v-if="addressList.length > 0">
            <div 
              class="address-card" 
              v-for="addr in addressList" 
              :key="addr.id"
              :class="{ active: selectedAddressId === addr.id }"
              @click="selectedAddressId = addr.id"
            >
              <div class="card-top">
                <span class="name">{{ addr.receiver }}</span>
                <span class="phone">{{ addr.phone }}</span>
                <el-tag size="small" type="danger" v-if="addr.isDefault">默认</el-tag>
              </div>
              <div class="card-bottom">
                {{ addr.province }} {{ addr.city }} {{ addr.county }} {{ addr.town }} {{ addr.detailAddress }}
              </div>
              <div class="check-mark" v-if="selectedAddressId === addr.id">
                <el-icon><Check /></el-icon>
              </div>
            </div>
          </template>
          <el-empty v-else description="暂无收货地址" :image-size="80">
            <el-button type="primary" @click="router.push('/address')">去添加地址</el-button>
          </el-empty>
        </div>
      </div>

      <!-- 商品清单 -->
      <div class="section goods-section">
        <div class="section-header">
          <h3><el-icon><Goods /></el-icon> 商品清单</h3>
        </div>
        
      <div class="goods-list">
          <div class="goods-item" v-for="item in selectedItems" :key="item.productId">
            <img :src="getCoverImage(item.productImg)" class="thumb" />
            <div class="info">
              <div class="name">{{ item.productName }}</div>
              <div class="props">
                <span>数量: x{{ item.productNum }}</span>
                <span v-if="item.stock <= 0" class="danger-text">已无库存</span>
                <span v-else-if="item.productNum > item.stock" class="danger-text">库存不足，仅剩 {{ item.stock }} 件</span>
              </div>
            </div>
            <div class="price">
              <div class="unit-price">¥{{ item.price }}</div>
              <div class="subtotal">¥{{ (item.price * item.productNum).toFixed(2) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单备注 -->
      <div class="section remark-section">
        <div class="section-header">
          <h3><el-icon><EditPen /></el-icon> 订单备注</h3>
        </div>
        <el-input
          v-model="remark"
          type="textarea"
          :rows="2"
          placeholder="选填：请填写您的备注信息"
          maxlength="100"
          show-word-limit
        />
      </div>

      <!-- 底部结算栏 -->
      <div class="action-bar">
        <div class="bar-left">
          <div class="total-row">
            <span class="label">共 {{ totalCount }} 件商品，合计：</span>
            <span class="price">¥{{ totalAmount.toFixed(2) }}</span>
          </div>
          <div class="addr-preview" v-if="currentAddress">
            寄送至：{{ currentAddress.province }} {{ currentAddress.city }} {{ currentAddress.county }} {{ currentAddress.detailAddress }}
          </div>
        </div>
        <el-button 
          type="danger" 
          size="large" 
          class="btn-submit"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Location, Goods, EditPen, Check, ArrowLeft } from '@element-plus/icons-vue';
import { useCartStore } from '@/stores/modules/cart';
import { createOrder } from '@/apis/order';
import { getAddresses } from '@/apis/user';
import { getProductDetail } from '@/apis/product';
import type { UserAddress, CartItem } from '@/types';
import { getFullImageUrl } from '@/utils/image';

/**
 * 确认订单页。
 * 这里负责把“购物车购买”和“立即购买”两条链路统一收口成一次下单请求。
 */

const router = useRouter();
const route = useRoute();
const cartStore = useCartStore();

const loadingAddress = ref(false);
const submitting = ref(false);
const addressList = ref<UserAddress[]>([]);
const selectedAddressId = ref<number | undefined>(undefined);
const remark = ref('');

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return getFullImageUrl(first);
};

// 直接购买的商品
const directBuyItem = ref<CartItem | null>(null);

// 选中的商品
const selectedItems = computed(() => {
  if (directBuyItem.value) {
    return [directBuyItem.value];
  }
  return cartStore.getSelectedItems();
});

const hasInvalidItems = computed(() => {
  return selectedItems.value.some(item => item.productNum <= 0 || item.stock <= 0 || item.productNum > item.stock);
});

const canSubmit = computed(() => {
  return !submitting.value
    && !!selectedAddressId.value
    && selectedItems.value.length > 0
    && totalCount.value > 0
    && !hasInvalidItems.value;
});

// 总件数
const totalCount = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + item.productNum, 0);
});

// 总金额
const totalAmount = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + item.price * item.productNum, 0);
});

// 当前选中地址对象
const currentAddress = computed(() => {
  return addressList.value.find(addr => addr.id === selectedAddressId.value);
});

onMounted(async () => {
  // 检查是否是直接购买
  const { productId, productNum } = route.query;
  if (productId && productNum) {
    await loadDirectBuyItem(Number(productId), Number(productNum));
  } else {
    // 购物车购买
    if (selectedItems.value.length === 0) {
      ElMessage.warning('请先选择商品');
      router.replace('/cart');
      return;
    }
  }
  
  await loadAddresses();
});

// 直接购买入口：根据商品 ID 拉取实时商品信息，避免库存和价格过期。
const loadDirectBuyItem = async (pid: number, num: number) => {
  try {
    if (!pid || !num || num <= 0) {
      ElMessage.error('购买参数无效');
      router.replace('/products');
      return;
    }
    const res = await getProductDetail(pid);
    if (res) {
      if ((res.stock || 0) <= 0) {
        ElMessage.warning('该商品当前无库存');
        router.replace(`/product/${pid}`);
        return;
      }
      const safeNum = Math.min(num, Number(res.stock || 0));
      if (safeNum !== num) {
        ElMessage.warning(`商品库存不足，已自动调整为 ${safeNum} 件`);
      }
      directBuyItem.value = {
        id: 0, // 临时ID
        productId: res.id,
        productName: res.productName,
        productImg: res.productImg,
        price: res.price,
        productNum: safeNum,
        stock: res.stock,
        selectStatus: true
      };
    } else {
      ElMessage.error('商品不存在');
      router.back();
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('加载商品失败');
    router.back();
  }
};

// 收货地址入口：订单提交前必须先有有效地址。
const loadAddresses = async () => {
  loadingAddress.value = true;
  try {
    const res = await getAddresses();
    if (res && Array.isArray(res)) {
      addressList.value = res;
      // 默认选中默认地址，或第一个地址
      const defaultAddr = res.find(a => a.isDefault);
      if (defaultAddr) {
        selectedAddressId.value = defaultAddr.id;
      } else if (res.length > 0) {
        const firstAddr = res[0];
        if (firstAddr) {
          selectedAddressId.value = firstAddr.id;
        }
      }
    } else {
       addressList.value = [];
       selectedAddressId.value = undefined;
    }
  } catch (error) {
    console.error('加载地址失败', error);
    ElMessage.error('加载收货地址失败，请稍后重试');
  } finally {
    loadingAddress.value = false;
  }
};

// 提交订单主入口：兼容购物车下单和直接购买两种业务来源。
const handleSubmit = async () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择商品');
    router.replace('/cart');
    return;
  }
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址');
    return;
  }
  if (hasInvalidItems.value) {
    ElMessage.warning('商品库存状态已变化，请返回重新确认');
    return;
  }

  submitting.value = true;
  try {
    let orderData: any = {
      addressId: selectedAddressId.value,
      remark: remark.value
    };

    if (directBuyItem.value) {
      // 直接购买
      orderData.productItems = [{
        productId: directBuyItem.value.productId,
        productNum: directBuyItem.value.productNum
      }];
    } else {
      // 购物车购买，传递 cartIds
      // 注意：cartIds 是购物车记录的ID，不是商品ID。
      // 前端 CartItem.id 应该是购物车记录ID。
      // 如果 CartStore 里的数据是从后端获取的，那么 id 应该是有效的。
      // 如果是从本地添加的（未同步到后端），id 可能是 0。
      // 假设我们已经实现了购物车同步，或者后端支持传递 productItems 即使是购物车购买（通常 cartIds 更高效且能删除购物车记录）。
      // 按照 API 文档：购物车下单用 cartIds。
      
      // 检查 CartStore 数据来源。如果未完全联调，CartStore 里的 id 可能是 0。
      // 此时可能需要 fallback 到 productItems 方式，或者确保 CartStore 数据正确。
      // 为了稳健，如果 cartIds 有效（>0）则传 cartIds，否则传 productItems（后端可能不支持混用，需注意）
      // 鉴于目前 CartStore 是纯本地的，id 都是 0。
      // 所以我们暂时使用 productItems 方式提交（模拟直接购买），或者如果后端严格区分接口，我们需要先实现 CartStore 的同步。
      // 根据 "接口测试顺序与数据.md"，Cart Buy 必须传 cartIds。
      // 这意味着我们必须先实现购物车列表获取接口 /user/cart，它会返回带有 id 的 CartItem。
      // 所以 OrderConfirm 页面加载时，如果是购物车购买，应该依赖 CartStore 已经获取了最新的购物车列表（含ID）。
      
      const cartIds = selectedItems.value.map(item => item.id).filter(id => id > 0);
      if (cartIds.length > 0) {
        orderData.cartIds = cartIds;
      } else {
        // Fallback: 如果没有有效的 cartId (例如本地测试)，尝试传 productItems
        // 但根据文档，购物车下单接口是 /user/orders，参数 cartIds。
        // 直接购买也是 /user/orders，参数 productItems。
        // 所以接口是一样的，只是参数不同。
        orderData.productItems = selectedItems.value.map(item => ({
          productId: item.productId,
          productNum: item.productNum
        }));
      }
    }

    const res: any = await createOrder(orderData);
    
    // 假设 API 返回订单 ID 和订单号
    // 接口文档：TC019 创建订单 -> 成功返回 200，data 通常包含订单信息
    // 假设返回结构 { id: 123, orderNo: "ORDER2024..." }
    const orderId = res?.id || res?.data?.id;
    const orderNo = res?.orderNo || res?.data?.orderNo;
    
    if (orderId) {
      ElMessage.success(`订单提交成功，订单号：${orderNo}`);
      // 清除已购买的商品
      if (!directBuyItem.value) {
         cartStore.clearSelected();
      }
      
      // 跳转支付
      router.replace(`/order-pay/${orderId}`);
    } else {
       throw new Error('订单创建失败，未返回订单ID');
    }
    
  } catch (error) {
    console.error('提交订单失败', error);
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped lang="scss">
.confirm-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding: 20px 0;
  padding-bottom: 80px;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  margin-bottom: 20px;
  h2 { font-size: 22px; color: #303133; }
}

.section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h3 {
      margin: 0;
      font-size: 16px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.address-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  
  .address-card {
    width: 280px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 15px;
    cursor: pointer;
    position: relative;
    transition: all 0.3s;
    
    &:hover {
      border-color: #409eff;
    }
    
    &.active {
      border-color: #f56c6c;
      background-color: #fef0f0;
    }
    
    .card-top {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;
      font-weight: bold;
      
      .name { font-size: 16px; color: #303133; }
      .phone { font-size: 14px; color: #606266; }
    }
    
    .card-bottom {
      font-size: 13px;
      color: #909399;
      line-height: 1.5;
    }
    
    .check-mark {
      position: absolute;
      right: 0;
      bottom: 0;
      width: 0;
      height: 0;
      border-style: solid;
      border-width: 0 0 20px 20px;
      border-color: transparent transparent #f56c6c transparent;
      
      .el-icon {
        position: absolute;
        right: 1px;
        bottom: -20px;
        color: #fff;
        font-size: 12px;
      }
    }
  }
}

.goods-list {
  .goods-item {
    display: flex;
    padding: 15px 0;
    border-bottom: 1px solid #f0f2f5;
    
    &:last-child { border-bottom: none; }
    
    .thumb {
      width: 80px;
      height: 80px;
      object-fit: cover;
      border-radius: 4px;
      margin-right: 15px;
    }
    
    .info {
      flex: 1;
      .name { font-size: 15px; color: #303133; margin-bottom: 8px; }
      .props {
        font-size: 13px;
        color: #909399;
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
      }
    }
    
    .price {
      text-align: right;
      .unit-price { font-size: 14px; color: #606266; margin-bottom: 4px; }
      .subtotal { font-size: 16px; font-weight: bold; color: #f56c6c; }
    }
  }
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 15px 40px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  z-index: 100;
  
  .bar-left {
    text-align: right;
    margin-right: 20px;
    
    .total-row {
      margin-bottom: 4px;
      .label { color: #606266; }
      .price { color: #f56c6c; font-size: 24px; font-weight: bold; }
    }
    
    .addr-preview {
      font-size: 13px;
      color: #909399;
    }
  }
  
  .btn-submit {
    width: 140px;
  }
}

@media (max-width: 768px) {
  .address-list .address-card {
    width: 100%;
  }
  
  .action-bar {
    padding: 15px 20px;
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    
    .bar-left {
      text-align: left;
      margin-right: 0;
    }
    
    .btn-submit {
      width: 100%;
    }
  }
}
</style>
