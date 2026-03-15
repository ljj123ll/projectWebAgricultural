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
          v-if="tab.count > 0" 
          :value="tab.count" 
          class="tab-badge"
        />
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
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
          <div v-for="item in order.orderItems" :key="item.id" class="item">
            <img :src="item.productImg" :alt="item.productName" />
            <div class="item-info">
              <h4>{{ item.productName }}</h4>
              <p>¥{{ item.productPrice }} × {{ item.productNum }}</p>
            </div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-amount">
            共{{ getTotalCount(order) }}件商品 实付：
            <span class="amount">¥{{ order.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="order-actions">
            <!-- 待付款 -->
            <template v-if="order.orderStatus === 1">
              <el-button type="primary" @click="goToPay(order.orderNo)">
                立即支付
              </el-button>
              <el-button @click="cancelOrder(order)">取消订单</el-button>
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
              <el-button type="primary" @click="goToReview(order)">
                评价商品
              </el-button>
              <el-button @click="applyAfterSale(order)">申请售后</el-button>
            </template>

            <!-- 已取消 -->
            <template v-if="order.orderStatus === 5">
              <el-button @click="deleteOrder(order)">删除订单</el-button>
            </template>

            <el-button link @click="viewDetail(order.orderNo)">查看详情</el-button>
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
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useOrderStore } from '@/stores/modules/order';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Order } from '@/types';

const router = useRouter();
const orderStore = useOrderStore();

const currentTab = ref(0);

const tabs = ref([
  { label: '全部', value: 0, count: 0 },
  { label: '待付款', value: 1, count: 2 },
  { label: '待发货', value: 2, count: 1 },
  { label: '待收货', value: 3, count: 0 },
  { label: '已完成', value: 4, count: 3 },
  { label: '已取消', value: 5, count: 1 }
]);

// 模拟订单数据
const orders = ref<Order[]>([
  {
    id: 1,
    orderNo: 'ORD202403010001',
    totalAmount: 59.8,
    orderStatus: 1,
    createTime: '2024-03-01 10:30:00',
    orderItems: [
      {
        id: 1,
        productId: 1,
        productName: '四川红心猕猴桃',
        productImg: 'https://via.placeholder.com/100x100/67c23a/fff?text=猕猴桃',
        productPrice: 29.9,
        productNum: 2
      }
    ]
  },
  {
    id: 2,
    orderNo: 'ORD202402280002',
    totalAmount: 39.9,
    orderStatus: 2,
    createTime: '2024-02-28 15:20:00',
    orderItems: [
      {
        id: 2,
        productId: 2,
        productName: '农家土鸡蛋',
        productImg: 'https://via.placeholder.com/100x100/e6a23c/fff?text=土鸡蛋',
        productPrice: 39.9,
        productNum: 1
      }
    ]
  },
  {
    id: 3,
    orderNo: 'ORD202402250003',
    totalAmount: 128,
    orderStatus: 4,
    createTime: '2024-02-25 09:00:00',
    orderItems: [
      {
        id: 3,
        productId: 3,
        productName: '高山绿茶',
        productImg: 'https://via.placeholder.com/100x100/67c23a/fff?text=绿茶',
        productPrice: 128,
        productNum: 1
      }
    ]
  },
  {
    id: 4,
    orderNo: 'ORD202402200004',
    totalAmount: 88,
    orderStatus: 5,
    createTime: '2024-02-20 14:00:00',
    orderItems: [
      {
        id: 4,
        productId: 4,
        productName: '农家腊肉',
        productImg: 'https://via.placeholder.com/100x100/f56c6c/fff?text=腊肉',
        productPrice: 88,
        productNum: 1
      }
    ]
  }
]);

// 过滤订单
const filteredOrders = computed(() => {
  if (currentTab.value === 0) {
    return orders.value;
  }
  return orders.value.filter(order => order.orderStatus === currentTab.value);
});

// 获取状态文本
const getStatusText = (status: number) => {
  return orderStore.getStatusText(status);
};

// 获取状态类型
const getStatusType = (status: number) => {
  return orderStore.getStatusType(status);
};

// 获取商品总数
const getTotalCount = (order: Order) => {
  return order.orderItems.reduce((sum, item) => sum + item.productNum, 0);
};

// 格式化日期
const formatDate = (date: string) => {
  return date.substring(0, 16).replace('T', ' ');
};

const messageDialogVisible = ref(false);
const currentOrderNo = ref('');
const messageContent = ref('');

// 去支付
const goToPay = (orderNo: string) => {
  router.push(`/order-pay/${orderNo}`);
};

// 取消订单
const cancelOrder = (order: Order) => {
  ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    order.orderStatus = 5;
    ElMessage.success('订单已取消');
  });
};

// 确认收货
const confirmReceive = (order: Order) => {
  ElMessageBox.confirm('确认已收到商品？', '提示', {
    confirmButtonText: '确认收货',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    order.orderStatus = 4;
    ElMessage.success('确认收货成功');
  });
};

// 查看物流
const viewLogistics = (order: Order) => {
  ElMessageBox.alert(
    '物流公司：顺丰速运\n物流单号：SF1234567890',
    '物流信息',
    {
      confirmButtonText: '复制单号',
      callback: () => {
        navigator.clipboard.writeText('SF1234567890');
        ElMessage.success('物流单号已复制');
      }
    }
  );
};

// 申请售后
const applyAfterSale = (order: Order) => {
  router.push(`/after-sale/${order.orderNo}`);
};

// 去评价
const goToReview = (order: Order) => {
  router.push(`/order-detail/${order.orderNo}?review=1`);
};

// 联系商家
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

// 删除订单
const deleteOrder = (order: Order) => {
  ElMessageBox.confirm('确定要删除该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = orders.value.findIndex(o => o.id === order.id);
    if (index > -1) {
      orders.value.splice(index, 1);
    }
    ElMessage.success('订单已删除');
  });
};

// 查看详情
const viewDetail = (orderNo: string) => {
  router.push(`/order-detail/${orderNo}`);
};
</script>

<style scoped lang="scss">
.orders-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.page-title {
  font-size: 24px;
  margin: 0 0 24px;
}

.order-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 12px;
  overflow-x: auto;

  .tab-item {
    padding: 8px 20px;
    font-size: 15px;
    color: #606266;
    cursor: pointer;
    border-radius: 20px;
    transition: all 0.2s;
    white-space: nowrap;
    position: relative;

    &:hover {
      background: #f5f7fa;
    }

    &.active {
      background: #67c23a;
      color: #fff;
    }

    .tab-badge {
      position: absolute;
      top: -4px;
      right: -4px;
    }
  }
}

.order-list {
  .order-card {
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    margin-bottom: 16px;
    overflow: hidden;

    .order-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: #f5f7fa;

      .order-info {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .order-no {
          font-size: 14px;
          color: #606266;
        }

        .order-time {
          font-size: 13px;
          color: #909399;
        }
      }
    }

    .order-items {
      padding: 16px;

      .item {
        display: flex;
        gap: 12px;
        padding: 8px 0;

        img {
          width: 80px;
          height: 80px;
          border-radius: 8px;
          object-fit: cover;
        }

        .item-info {
          flex: 1;

          h4 {
            margin: 0 0 8px;
            font-size: 15px;
          }

          p {
            margin: 0;
            color: #909399;
            font-size: 14px;
          }
        }
      }
    }

    .order-footer {
      padding: 16px;
      border-top: 1px solid #e4e7ed;
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
      }
    }
  }
}

@media (max-width: 768px) {
  .orders-page {
    padding: 16px;
  }

  .page-title {
    font-size: 20px;
  }

  .order-tabs {
    .tab-item {
      padding: 6px 16px;
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
