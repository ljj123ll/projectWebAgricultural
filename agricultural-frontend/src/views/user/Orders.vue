<template>
  <div class="orders-page">
    <h1 class="page-title">我的订单</h1>

    <!-- 订单状态标签 -->
    <div class="order-tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentTab === tab.value }"
        @click="currentTab = tab.value"
      >
        {{ tab.label }}
        <el-badge 
          v-if="getTabCount(tab.value) > 0" 
          :value="getTabCount(tab.value)" 
          class="tab-badge"
        />
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-list" v-loading="loading">
      <div v-for="order in filteredOrders" :key="order.id" class="order-card">
        <div class="order-header">
          <div class="order-info">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ formatDate(order.createTime) }}</span>
          </div>
          <el-tag :type="getStatusType(order.orderStatus)">
            {{ getStatusText(order.orderStatus) }}
          </el-tag>
        </div>

        <div class="order-items">
          <div v-for="item in order.orderItems" :key="item.productId" class="item">
            <img :src="getCoverImage(item.productImg)" :alt="item.productName" />
            <div class="item-info">
              <h4>{{ item.productName }}</h4>
              <p>¥{{ item.productPrice }} × {{ item.productNum }}</p>
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-amount">
            共{{ getTotalCount(order) }}件商品 实付：
            <span class="amount">¥{{ Number(order.totalAmount).toFixed(2) }}</span>
          </div>
          <div class="order-actions">
            <!-- 待付款 -->
            <template v-if="order.orderStatus === 1">
              <el-button type="primary" @click="goToPay(order.id)">
                立即支付
              </el-button>
              <el-button @click="cancelCurrentOrder(order)">取消订单</el-button>
            </template>

            <!-- 待发货 -->
            <template v-if="order.orderStatus === 2">
              <el-button @click="contactMerchant(order)">联系商家</el-button>
            </template>

            <!-- 待收货 -->
            <template v-if="order.orderStatus === 3">
              <el-button type="primary" @click="confirmReceive(order)">
                确认收货
              </el-button>
              <el-button @click="viewLogistics(order)">查看物流</el-button>
              <el-button @click="applyAfterSale(order)">申请售后</el-button>
            </template>

            <!-- 已完成 -->
            <template v-if="order.orderStatus === 4">
              <el-button v-if="isOrderReviewed(order)" type="success" plain @click="goToReviewed(order)">
                已评价
              </el-button>
              <el-button v-else type="primary" @click="goToReview(order.id)">
                评价商品
              </el-button>
              <el-button @click="applyAfterSale(order)">申请售后</el-button>
            </template>

            <!-- 已取消 -->
            <template v-if="order.orderStatus === 5">
              <el-button @click="deleteOrder(order)">删除订单</el-button>
            </template>

            <el-button link @click="viewDetail(order.id)">查看详情</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="filteredOrders.length === 0" description="暂无订单" />

    <el-dialog v-model="messageDialogVisible" title="联系商家" width="90%">
      <el-form label-position="top">
        <el-form-item label="订单号">
          <el-input v-model="currentOrderNo" disabled />
        </el-form-item>
        <el-form-item label="留言内容">
          <el-input
            v-model="messageContent"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请描述问题或需求"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="messageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="sendMessage">发送留言</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useOrderStore } from '@/stores/modules/order';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Comment, Order } from '@/types';
import { getOrders, cancelOrder, receiveOrder, getLogistics } from '@/apis/order';
import { listComments } from '@/apis/user';
import { getFullImageUrl } from '@/utils/image';

const router = useRouter();
const orderStore = useOrderStore();

const currentTab = ref(0);
const loading = ref(false);
const hiddenOrderIds = ref<number[]>([]);
const reviewedKeySet = ref<Set<string>>(new Set());

const tabs = ref([
  { label: '全部', value: 0 },
  { label: '待付款', value: 1 },
  { label: '待发货', value: 2 },
  { label: '待收货', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 }
]);

const orders = ref<Order[]>([]);

const normalizeOrder = (order: any): Order => {
  const orderItems = Array.isArray(order.orderItems) ? order.orderItems : order.items || [];
  return {
    ...order,
    totalAmount: Number(order.totalAmount || 0),
    orderItems
  };
};

const loadOrders = async () => {
  loading.value = true;
  try {
    const res = await getOrders({ pageNum: 1, pageSize: 100 });
    orders.value = (res?.list || []).map(normalizeOrder);
    await loadReviewedComments();
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadOrders();
});

const visibleOrders = computed(() => orders.value.filter(order => !hiddenOrderIds.value.includes(order.id)));

const makeReviewedKey = (orderNo: string, productId: number) => `${orderNo}::${productId}`;

const loadReviewedComments = async () => {
  try {
    const res = await listComments({ pageNum: 1, pageSize: 1000 });
    const next = new Set<string>();
    (res?.list || []).forEach((comment: Comment) => {
      if (!comment.orderNo || !comment.productId) return;
      next.add(makeReviewedKey(comment.orderNo, Number(comment.productId)));
    });
    reviewedKeySet.value = next;
  } catch (error) {
    console.warn('加载评价记录失败', error);
    reviewedKeySet.value = new Set();
  }
};

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw
    .split(',')
    .map(item => item.trim())
    .find(Boolean) || '';
  return getFullImageUrl(first);
};

const filteredOrders = computed(() => {
  if (currentTab.value === 0) {
    return visibleOrders.value;
  }
  return visibleOrders.value.filter(order => order.orderStatus === currentTab.value);
});

const getTabCount = (tabValue: number) => {
  if (tabValue === 0) return visibleOrders.value.length;
  return visibleOrders.value.filter(order => order.orderStatus === tabValue).length;
};

const isOrderReviewed = (order: Order) => {
  const items = order.orderItems || [];
  if (items.length === 0) return false;
  return items.every(item => reviewedKeySet.value.has(makeReviewedKey(order.orderNo, item.productId)));
};

const getStatusText = (status: number) => {
  return orderStore.getStatusText(status);
};

const getStatusType = (status: number) => {
  return orderStore.getStatusType(status);
};

const getTotalCount = (order: Order) => {
  return (order.orderItems || []).reduce((sum, item) => sum + item.productNum, 0);
};

const formatDate = (date: string) => {
  if (!date) return '';
  return date.substring(0, 16).replace('T', ' ');
};

const messageDialogVisible = ref(false);
const currentOrderNo = ref('');
const messageContent = ref('');

const goToPay = (orderId: number) => {
  router.push(`/order-pay/${orderId}`);
};

const cancelCurrentOrder = (order: Order) => {
  ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await cancelOrder(order.id);
    ElMessage.success('订单已取消');
    await loadOrders();
  });
};

const confirmReceive = (order: Order) => {
  ElMessageBox.confirm('确认已收到商品？', '提示', {
    confirmButtonText: '确认收货',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    await receiveOrder(order.id);
    ElMessage.success('确认收货成功');
    await loadOrders();
  });
};

const viewLogistics = async (order: Order) => {
  const logistics = await getLogistics(order.id);
  const company = logistics?.logisticsCompany || '暂无';
  const no = logistics?.logisticsNo || '暂无';
  ElMessageBox.alert(
    `物流公司：${company}\n物流单号：${no}`,
    '物流信息',
    {
      confirmButtonText: '确定'
    }
  );
};

const applyAfterSale = (order: Order) => {
  router.push(`/after-sale/${order.orderNo}`);
};

const goToReview = (orderId: number) => {
  router.push(`/order-detail/${orderId}?review=1`);
};

const goToReviewed = (order: Order) => {
  const first = order.orderItems?.[0];
  router.push({
    path: '/reviews',
    query: {
      orderNo: order.orderNo,
      ...(first ? { productId: String(first.productId) } : {})
    }
  });
};

const contactMerchant = (order: Order) => {
  currentOrderNo.value = order.orderNo;
  messageContent.value = '';
  messageDialogVisible.value = true;
};

const sendMessage = () => {
  if (!messageContent.value.trim()) {
    ElMessage.warning('请输入留言内容');
    return;
  }
  ElMessage.success('留言已发送');
  messageDialogVisible.value = false;
};

const deleteOrder = (order: Order) => {
  ElMessageBox.confirm('确定要删除该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    hiddenOrderIds.value.push(order.id);
    ElMessage.success('订单已删除');
  });
};

const viewDetail = (orderId: number) => {
  router.push(`/order-detail/${orderId}`);
};
</script>

<style scoped lang="scss">
.orders-page {
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  border: 1px solid #edf0f5;
  border-radius: 16px;
  box-shadow: 0 8px 28px rgba(28, 49, 76, 0.06);
  padding: 28px;
}

.page-title {
  font-size: 32px;
  line-height: 1.1;
  margin: 0 0 26px;
  color: #1f2a37;
  letter-spacing: 0.5px;
  font-weight: 700;
}

.order-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 26px;
  border-bottom: 1px solid #e4e7ed;
  padding: 2px 2px 14px;
  overflow-x: auto;

  .tab-item {
    padding: 9px 20px;
    font-size: 15px;
    font-weight: 600;
    color: #5e6b80;
    cursor: pointer;
    border-radius: 999px;
    border: 1px solid transparent;
    background: #f4f7fb;
    transition: all 0.2s ease;
    white-space: nowrap;
    position: relative;
    padding-right: 26px;

    &:hover {
      background: #edf3fb;
      color: #3d4a5d;
    }

    &.active {
      background: linear-gradient(135deg, #71cb45 0%, #5fb83a 100%);
      color: #fff;
      border-color: #61ba3d;
      box-shadow: 0 6px 14px rgba(103, 194, 58, 0.25);
    }

    .tab-badge {
      position: absolute;
      top: 2px;
      right: -8px;

      :deep(.el-badge__content) {
        min-width: 18px;
        height: 18px;
        line-height: 18px;
        padding: 0 5px;
        border-radius: 999px;
        font-size: 11px;
        font-weight: 700;
        border: 2px solid #fff;
        box-shadow: 0 2px 6px rgba(245, 108, 108, 0.35);
      }
    }
  }
}

.order-list {
  .order-card {
    border: 1px solid #e7edf5;
    border-radius: 12px;
    margin-bottom: 18px;
    overflow: hidden;
    background: #fff;
    box-shadow: 0 4px 14px rgba(27, 46, 89, 0.04);
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 8px 24px rgba(27, 46, 89, 0.08);
    }

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 18px;
      background: linear-gradient(180deg, #f7f9fc 0%, #f3f6fb 100%);

      .order-info {
        display: flex;
        flex-direction: column;
        gap: 5px;

        .order-no {
          font-size: 14px;
          color: #4c5668;
          font-weight: 600;
        }

        .order-time {
          font-size: 13px;
          color: #8a94a6;
        }
      }
    }

    .order-items {
      padding: 14px 18px;

      .item {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 12px 0;
        border-bottom: 1px solid #eff3f8;

        &:last-child {
          border-bottom: none;
        }

        img {
          width: 86px;
          height: 86px;
          border-radius: 10px;
          object-fit: cover;
          border: 1px solid #edf1f7;
          background: #f8fafc;
        }

        .item-info {
          flex: 1;

          h4 {
            margin: 0 0 8px;
            font-size: 16px;
            color: #2c3b50;
            font-weight: 600;
          }

          p {
            margin: 0;
            color: #8d97a7;
            font-size: 14px;
          }
        }
      }
    }

    .order-footer {
      padding: 15px 18px;
      border-top: 1px solid #e8edf4;
      background: #fbfdff;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .order-amount {
        color: #606266;

        .amount {
          font-size: 18px;
          font-weight: bold;
          color: #f56c6c;
        }
      }

      .order-actions {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
        align-items: center;
      }
    }
  }
}

@media (max-width: 768px) {
  .orders-page {
    padding: 16px;
    border-radius: 12px;
  }

  .page-title {
    font-size: 26px;
    margin-bottom: 18px;
  }

  .order-tabs {
    gap: 10px;

    .tab-item {
      padding: 7px 14px;
      padding-right: 22px;
      font-size: 14px;
    }
  }

  .order-footer {
    flex-direction: column;
    align-items: flex-start !important;

    .order-actions {
      width: 100%;
      justify-content: flex-end;
    }
  }
}
</style>
