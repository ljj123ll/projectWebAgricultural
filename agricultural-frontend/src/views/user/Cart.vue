<template>
  <div class="cart-page">
    <div class="container">
      <div class="cart-header">
        <h2>
          <el-button link @click="$router.back()" class="back-btn" style="margin-right: 10px; font-size: 20px;">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <el-icon><ShoppingCart /></el-icon> 购物车 
          <span class="count" v-if="cartStore.totalCount > 0">({{ cartStore.totalCount }})</span>
        </h2>
        <div class="header-actions" v-if="cartStore.cartList.length > 0">
           <el-button link type="danger" @click="handleClearCart">
             <el-icon><Delete /></el-icon> 清空购物车
           </el-button>
        </div>
      </div>

      <div class="cart-body" v-if="cartStore.cartList.length > 0">
        <!-- 表头 (PC端显示) -->
        <div class="cart-thead hidden-xs-only">
          <div class="col col-check">
            <el-checkbox 
              :model-value="cartStore.isAllSelected" 
              :indeterminate="isIndeterminate"
              @change="toggleSelectAll"
            >全选</el-checkbox>
          </div>
          <div class="col col-img">商品</div>
          <div class="col col-name">&nbsp;</div>
          <div class="col col-price">单价</div>
          <div class="col col-num">数量</div>
          <div class="col col-total">小计</div>
          <div class="col col-action">操作</div>
        </div>

        <!-- 列表 -->
        <div class="cart-list">
          <div class="cart-item" v-for="item in cartStore.cartList" :key="item.productId">
            <div class="col col-check">
              <el-checkbox 
                :model-value="item.selectStatus" 
                @change="() => cartStore.toggleSelect(item.productId)"
              />
            </div>
            <div class="col col-img" @click="goToProduct(item.productId)">
              <img :src="getCoverImage(item.productImg)" />
            </div>
            <div class="col col-name" @click="goToProduct(item.productId)">
              <div class="name">{{ item.productName }}</div>
              <div class="stock-tag" v-if="item.stock < 10">库存紧张: {{ item.stock }}</div>
            </div>
            <div class="col col-price">
              <span class="label-xs">单价:</span> ¥{{ item.price }}
            </div>
            <div class="col col-num">
              <el-input-number 
                v-model="item.productNum" 
                :min="1" 
                :max="item.stock" 
                size="small"
                @change="(val) => handleNumChange(item, val)"
              />
            </div>
            <div class="col col-total">
              <span class="label-xs">小计:</span> 
              <span class="total-text">¥{{ (item.price * item.productNum).toFixed(2) }}</span>
            </div>
            <div class="col col-action">
              <el-button link type="danger" @click="handleDelete(item)">删除</el-button>
            </div>
          </div>
        </div>
        
        <!-- 结算栏 -->
        <div class="cart-footer">
          <div class="footer-left">
            <el-checkbox 
              :model-value="cartStore.isAllSelected" 
              :indeterminate="isIndeterminate"
              @change="toggleSelectAll"
            >全选</el-checkbox>
            <span class="selected-info">
              已选 <span class="highlight">{{ cartStore.selectedCount }}</span> 件
            </span>
          </div>
          <div class="footer-right">
            <div class="total-info">
              <span class="label">合计:</span>
              <span class="price">¥{{ cartStore.selectedAmount.toFixed(2) }}</span>
            </div>
            <el-button 
              type="primary" 
              size="large" 
              class="btn-checkout"
              :disabled="cartStore.selectedCount === 0"
              @click="handleCheckout"
            >
              去结算
            </el-button>
          </div>
        </div>
      </div>
      
      <el-empty v-else description="购物车空空如也">
        <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ShoppingCart, Delete, ArrowLeft } from '@element-plus/icons-vue';
import { useCartStore } from '@/stores/modules/cart';
import { getCart, updateCartItem, deleteCartItem, clearCart } from '@/apis/order';
import type { CartItem } from '@/types';
import { getFullImageUrl } from '@/utils/image';

const router = useRouter();
const cartStore = useCartStore();

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return getFullImageUrl(first);
};

onMounted(() => {
  loadCart();
});

const loadCart = async () => {
  try {
    const res = await getCart();
    if (res) {
      cartStore.setCartList(res);
    }
  } catch (error) {
    console.error('加载购物车失败', error);
  }
};

const isIndeterminate = computed(() => {
  // @ts-ignore
  return cartStore.selectedCount > 0 && !cartStore.isAllSelected;
});

// 全选/反选 (纯前端操作，不涉及后端?)
// 通常全选状态只影响结算，不保存到后端，除非后端有 select_status 字段
// 数据库 schema 有 select_status 字段。
// 所以全选应该也要同步到后端吗？
// 接口文档没有批量更新选中状态的接口，只有 updateCartItem。
// 如果要同步选中状态，可能需要循环调用 updateCartItem 或者后端提供批量接口。
// 鉴于效率，暂时只在前端维护选中状态，或者 assume 后端 updateCartItem 可以更新 selectStatus。
// 这里暂时只做前端全选。
const toggleSelectAll = (val: any) => {
  cartStore.toggleSelectAll(val);
};

const handleNumChange = async (item: CartItem, val: number | undefined) => {
  if (val === undefined) return;
  
  // 乐观更新或等待 API
  // 这里选择等待 API
  try {
    // 假设 item.id 是购物车记录 ID
    // 接口文档：PUT /user/cart/{id}
    // Body: { productNum: 3 }
    if (item.id) {
       await updateCartItem(item.id, { productNum: val });
       cartStore.updateQuantity(item.productId, val);
    } else {
       // 如果是没有 ID 的临时项 (不太可能，因为 loadCart 会获取 ID)
       cartStore.updateQuantity(item.productId, val);
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('更新数量失败');
    // 回滚? 暂时略过
    loadCart(); // 重新加载以恢复正确状态
  }
};

const handleDelete = (item: CartItem) => {
  ElMessageBox.confirm(`确定要删除 ${item.productName} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      if (item.id) {
        await deleteCartItem(item.id);
      }
      cartStore.removeFromCart(item.productId);
      ElMessage.success('已删除');
    } catch (error) {
      console.error(error);
      ElMessage.error('删除失败');
    }
  });
};

const handleClearCart = () => {
  ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await clearCart();
      cartStore.clearCart();
      ElMessage.success('购物车已清空');
    } catch (error) {
      console.error(error);
      ElMessage.error('清空失败');
    }
  });
};

const handleCheckout = () => {
  // @ts-ignore
  if (cartStore.selectedCount === 0) {
    ElMessage.warning('请选择要结算的商品');
    return;
  }
  router.push('/order-confirm');
};

const goToProduct = (id: number) => {
  router.push(`/product/${id}`);
};
</script>

<style scoped lang="scss">
.cart-page {
  background-color: #f5f7fa;
  min-height: 100vh;
  padding: 20px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  background: #fff;
  border-radius: 8px;
  min-height: 500px;
  padding-bottom: 20px;
}

.cart-header {
  padding: 20px 0;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  h2 {
    margin: 0;
    font-size: 20px;
    display: flex;
    align-items: center;
    gap: 8px;
    
    .count {
      font-size: 14px;
      color: #909399;
      font-weight: normal;
    }
  }
}

.cart-body {
  margin-top: 20px;
}

.cart-thead {
  display: flex;
  background: #f5f7fa;
  padding: 10px 0;
  font-size: 14px;
  color: #909399;
  border-radius: 4px;
  
  .col {
    padding: 0 10px;
    &.col-check { width: 50px; text-align: center; }
    &.col-img { width: 100px; }
    &.col-name { flex: 1; }
    &.col-price { width: 120px; text-align: center; }
    &.col-num { width: 150px; text-align: center; }
    &.col-total { width: 120px; text-align: center; }
    &.col-action { width: 100px; text-align: center; }
  }
}

.cart-list {
  margin-bottom: 80px; /* 留出底部结算栏的空间 */
  
  .cart-item {
    display: flex;
    align-items: center;
    padding: 20px 0;
    border-bottom: 1px solid #ebeef5;
    
    .col {
      padding: 0 10px;
    }
    
    .col-check {
      width: 50px;
      text-align: center;
    }
    
    .col-img {
      width: 100px;
      height: 100px;
      cursor: pointer;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 4px;
        border: 1px solid #eee;
      }
    }
    
    .col-name {
      flex: 1;
      cursor: pointer;
      
      .name {
        font-size: 16px;
        color: #303133;
        margin-bottom: 8px;
        font-weight: 500;
        
        &:hover { color: #409eff; }
      }
      
      .stock-tag {
        font-size: 12px;
        color: #f56c6c;
        background: #fef0f0;
        display: inline-block;
        padding: 2px 6px;
        border-radius: 4px;
      }
    }
    
    .col-price {
      width: 120px;
      text-align: center;
      font-weight: bold;
      color: #303133;
    }
    
    .col-num {
      width: 150px;
      text-align: center;
    }
    
    .col-total {
      width: 120px;
      text-align: center;
      font-weight: bold;
      color: #f56c6c;
    }
    
    .col-action {
      width: 100px;
      text-align: center;
    }
    
    .label-xs { display: none; }
  }
}

.cart-footer {
  position: sticky;
  bottom: 0;
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 15px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);
  z-index: 10;
  
  .footer-left {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .selected-info {
      font-size: 14px;
      color: #606266;
      .highlight { color: #f56c6c; font-weight: bold; margin: 0 4px; }
    }
  }
  
  .footer-right {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .total-info {
      font-size: 14px;
      color: #606266;
      
      .price {
        font-size: 24px;
        color: #f56c6c;
        font-weight: bold;
        margin-left: 8px;
      }
    }
    
    .btn-checkout {
      width: 120px;
    }
  }
}

@media (max-width: 768px) {
  .cart-thead { display: none; }
  
  .cart-item {
    flex-wrap: wrap;
    position: relative;
    
    .col-check {
      position: absolute;
      top: 20px;
      left: 0;
    }
    
    .col-img {
      margin-left: 30px;
      width: 80px;
      height: 80px;
    }
    
    .col-name {
      width: calc(100% - 130px);
      margin-left: 10px;
    }
    
    .col-price, .col-num, .col-total, .col-action {
      width: 50%;
      text-align: left;
      margin-top: 10px;
      padding-left: 30px;
      display: flex;
      align-items: center;
    }
    
    .label-xs {
      display: inline-block;
      margin-right: 10px;
      color: #909399;
      width: 40px;
    }
    
    .col-action {
      position: absolute;
      right: 0;
      bottom: 20px;
      width: auto;
    }
  }
  
  .cart-footer {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
    
    .footer-left, .footer-right {
      justify-content: space-between;
    }
  }
}
</style>
