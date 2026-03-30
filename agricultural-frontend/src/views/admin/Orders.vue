<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索订单号/手机号/收货人"
          style="width: 280px"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>
    </div>

    <div class="filter-tabs">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待付款" name="1" />
        <el-tab-pane label="待发货" name="2" />
        <el-tab-pane label="待收货" name="3" />
        <el-tab-pane label="已完成" name="4" />
        <el-tab-pane label="已取消" name="5" />
      </el-tabs>
    </div>

    <el-card>
      <el-table :data="orderList" style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column label="商品信息" min-width="280">
          <template #default="{ row }">
            <div class="product-info" v-if="getOrderItems(row).length > 0">
              <img :src="getOrderCover(row)" class="product-img" />
              <div class="info-content">
                <div class="name">{{ getPrimaryItem(row)?.productName || '-' }}</div>
                <div class="desc">
                  共 {{ getOrderItems(row).length }} 件商品
                </div>
              </div>
            </div>
            <div v-else>无商品信息</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="实付款" width="120">
          <template #default="{ row }">
            ¥{{ formatAmount(row.totalAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="receiver" label="买家信息" width="170">
          <template #default="{ row }">
            <div>{{ row.receiver || '-' }}</div>
            <div>{{ row.receiverPhone || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.orderStatus === 1"
              link
              type="danger"
              @click="handleCancel(row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadOrders"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="980px"
      class="order-detail-dialog"
      destroy-on-close
    >
      <div v-if="currentOrder" class="order-detail">
        <div class="summary-panel">
          <div class="summary-main">
            <div class="order-no">订单号：{{ currentOrder.orderNo }}</div>
            <div class="order-time">下单时间：{{ formatDateTime(currentOrder.createTime) }}</div>
          </div>
          <div class="summary-side">
            <el-tag :type="getStatusType(currentOrder.orderStatus)" class="status-tag">
              {{ getStatusText(currentOrder.orderStatus) }}
            </el-tag>
            <div class="amount">¥{{ formatAmount(currentOrder.totalAmount) }}</div>
            <div class="amount-desc">共 {{ getOrderItems(currentOrder).length }} 件商品</div>
          </div>
        </div>

        <div class="detail-grid">
          <el-card shadow="never" class="detail-card">
            <template #header>
              <span>订单信息</span>
            </template>
            <div class="detail-row"><span class="label">订单编号</span><span class="value">{{ currentOrder.orderNo }}</span></div>
            <div class="detail-row"><span class="label">订单状态</span><span class="value">{{ getStatusText(currentOrder.orderStatus) }}</span></div>
            <div class="detail-row"><span class="label">下单时间</span><span class="value">{{ formatDateTime(currentOrder.createTime) }}</span></div>
            <div class="detail-row"><span class="label">支付时间</span><span class="value">{{ formatDateTime(currentOrder.payTime) }}</span></div>
            <div class="detail-row"><span class="label">支付截止</span><span class="value">{{ formatDateTime(currentOrder.payDeadline) }}</span></div>
            <div class="detail-row"><span class="label">订单总额</span><span class="value amount-text">¥{{ formatAmount(currentOrder.totalAmount) }}</span></div>
          </el-card>

          <el-card shadow="never" class="detail-card">
            <template #header>
              <span>收货信息</span>
            </template>
            <div class="detail-row"><span class="label">收货人</span><span class="value">{{ currentOrder.receiver || '-' }}</span></div>
            <div class="detail-row"><span class="label">手机号</span><span class="value">{{ currentOrder.receiverPhone || '-' }}</span></div>
            <div class="detail-row detail-column">
              <span class="label">收货地址</span>
              <span class="value">{{ currentOrder.receiverAddress || '-' }}</span>
            </div>
          </el-card>

          <el-card shadow="never" class="detail-card">
            <template #header>
              <span>物流信息</span>
            </template>
            <div class="detail-row"><span class="label">物流公司</span><span class="value">{{ currentOrder.logisticsCompany || '-' }}</span></div>
            <div class="detail-row"><span class="label">物流单号</span><span class="value">{{ currentOrder.logisticsNo || '-' }}</span></div>
            <div class="detail-row"><span class="label">物流状态</span><span class="value">{{ getLogisticsStatusText(currentOrder.logisticsStatus) }}</span></div>
            <div class="detail-row detail-column">
              <span class="label">异常说明</span>
              <span class="value">{{ currentOrder.abnormalReason || '-' }}</span>
            </div>
          </el-card>
        </div>

        <el-alert
          v-if="currentOrder.cancelReason"
          type="warning"
          show-icon
          :closable="false"
          class="cancel-alert"
          :title="`取消原因：${currentOrder.cancelReason}`"
        />

        <h4 class="section-title">商品明细</h4>
        <el-table :data="getOrderItems(currentOrder)" border class="items-table">
          <el-table-column label="商品" min-width="280">
            <template #default="{ row }">
              <div class="product-info">
                <img :src="getCoverImage(row.productImg)" class="product-img" />
                <span class="name">{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.productPrice) }}</template>
          </el-table-column>
          <el-table-column prop="productNum" label="数量" width="90" align="center" />
          <el-table-column label="小计" width="140" align="right">
            <template #default="{ row }">¥{{ formatAmount(getItemAmount(row)) }}</template>
          </el-table-column>
        </el-table>

        <div class="total-bar">
          实付款：
          <span class="money">¥{{ formatAmount(currentOrder.totalAmount) }}</span>
        </div>

        <OrderChatPanel
          v-if="currentOrder.orderNo"
          :order-no="currentOrder.orderNo"
          role="admin"
          title="订单实时沟通（管理员介入）"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listOrders, getOrder, cancelOrder } from '@/apis/admin';
import type { Order, OrderItem } from '@/types';
import { getFullImageUrl } from '@/utils/image';
import OrderChatPanel from '@/components/OrderChatPanel.vue';
import { ADMIN_REALTIME_EVENT, parseRealtimePayload } from '@/utils/realtime';

const searchKeyword = ref('');
const activeTab = ref('all');
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);

const orderList = ref<Order[]>([]);
const detailVisible = ref(false);
const currentOrder = ref<Order | null>(null);
const defaultOrderCover = (() => {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 120 120">
      <rect width="120" height="120" rx="20" fill="#f5f7fa" />
      <circle cx="60" cy="44" r="18" fill="#d9e8cf" />
      <rect x="24" y="72" width="72" height="12" rx="6" fill="#b8d6ae" />
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
})();

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    1: 'warning',
    2: 'primary',
    3: 'primary',
    4: 'success',
    5: 'info',
    6: 'danger',
    7: 'warning',
    8: 'success'
  };
  return map[status] || 'info';
};

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    1: '待付款',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消',
    6: '支付异常',
    7: '售后中',
    8: '已完成售后'
  };
  return map[status] || '未知状态';
};

const getLogisticsStatusText = (status?: number) => {
  const map: Record<number, string> = {
    1: '运输中',
    2: '已签收',
    3: '异常'
  };
  if (!status) return '-';
  return map[status] || `状态${status}`;
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
};

const formatAmount = (value?: number | string) => {
  const num = Number(value ?? 0);
  if (Number.isNaN(num)) return '0.00';
  return num.toFixed(2);
};

const getOrderItems = (order?: Partial<Order> | null): OrderItem[] => {
  if (!order) return [];
  const fromItems = (order as any).items;
  if (Array.isArray(fromItems)) return fromItems;
  const fromOrderItems = (order as any).orderItems;
  if (Array.isArray(fromOrderItems)) return fromOrderItems;
  return [];
};

const getCoverImage = (raw?: string) => {
  if (!raw) return defaultOrderCover;
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return first ? getFullImageUrl(first) : defaultOrderCover;
};

const getOrderCover = (order?: Partial<Order> | null) => {
  const first = getOrderItems(order)[0];
  return getCoverImage(first?.productImg || '');
};

const getPrimaryItem = (order?: Partial<Order> | null) => {
  return getOrderItems(order)[0] || null;
};

const getItemAmount = (item: OrderItem & { productAmount?: number }) => {
  if (item.productAmount !== undefined && item.productAmount !== null) {
    return Number(item.productAmount);
  }
  return Number(item.productPrice || 0) * Number(item.productNum || 0);
};

const loadOrders = async () => {
  loading.value = true;
  try {
    const params: any = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined
    };
    if (activeTab.value !== 'all') {
      params.status = Number(activeTab.value);
    }
    const res = await listOrders(params);
    if (res) {
      orderList.value = (res.list || []).map((item: Order) => ({
        ...item,
        items: getOrderItems(item)
      }));
      total.value = res.total || 0;
    }
  } catch (error) {
    console.error(error);
    orderList.value = [];
    total.value = 0;
    ElMessage.error('订单列表加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  loadOrders();
};

const handleTabChange = () => {
  currentPage.value = 1;
  loadOrders();
};

onMounted(() => {
  loadOrders();
  window.addEventListener(ADMIN_REALTIME_EVENT, handleRealtimeRefresh);
});

onUnmounted(() => {
  window.removeEventListener(ADMIN_REALTIME_EVENT, handleRealtimeRefresh);
});

const viewDetail = async (row: Order) => {
  try {
    const res = await getOrder(row.id);
    if (res) {
      currentOrder.value = {
        ...res,
        items: getOrderItems(res)
      };
      detailVisible.value = true;
    }
  } catch (error) {
    console.error(error);
    ElMessage.error('订单详情加载失败，请稍后重试');
  }
};

const handleRealtimeRefresh = (event: Event) => {
  const payload = parseRealtimePayload(event);
  const isOrderEvent = String(payload.reason || '').startsWith('ORDER');
  if (!isOrderEvent) return;
  void loadOrders();
  if (detailVisible.value && currentOrder.value?.id) {
    void viewDetail(currentOrder.value);
  }
};

const handleCancel = (row: Order) => {
  ElMessageBox.prompt('请输入取消原因', '取消订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '原因不能为空'
  })
    .then(async (result) => {
      const value = (result as { value?: string }).value || '';
      try {
        await cancelOrder(row.id, value);
        ElMessage.success('订单已取消');
        await loadOrders();
      } catch (error) {
        console.error(error);
        ElMessage.error('取消订单失败，请稍后重试');
      }
    })
    .catch(() => {});
};
</script>

<style scoped lang="scss">
.orders-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.filter-tabs {
  margin-bottom: 20px;
  background: #fff;
  padding: 0 20px;
  border-radius: 8px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eef2f7;
}

.info-content {
  flex: 1;

  .name {
    font-size: 14px;
    color: #1f2937;
    margin-bottom: 6px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .desc {
    font-size: 12px;
    color: #6b7280;
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.order-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-panel {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 18px;
  background: linear-gradient(120deg, #f8fafc, #ffffff);
}

.summary-main {
  display: flex;
  flex-direction: column;
  gap: 8px;

  .order-no {
    font-size: 16px;
    color: #111827;
    font-weight: 600;
  }

  .order-time {
    color: #6b7280;
    font-size: 13px;
  }
}

.summary-side {
  text-align: right;

  .status-tag {
    margin-bottom: 8px;
  }

  .amount {
    color: #ef4444;
    font-size: 24px;
    font-weight: 700;
    line-height: 1.2;
  }

  .amount-desc {
    color: #6b7280;
    font-size: 12px;
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-card {
  border-radius: 10px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;

  &:last-child {
    margin-bottom: 0;
  }

  .label {
    color: #6b7280;
    font-size: 13px;
    flex-shrink: 0;
  }

  .value {
    color: #111827;
    font-size: 13px;
    text-align: right;
    word-break: break-all;
  }

  .amount-text {
    color: #ef4444;
    font-weight: 600;
  }
}

.detail-column {
  align-items: flex-start;
  flex-direction: column;

  .value {
    text-align: left;
  }
}

.cancel-alert {
  margin-top: 2px;
}

.section-title {
  margin: 2px 0 -4px;
  padding-left: 10px;
  border-left: 4px solid var(--el-color-primary);
  font-size: 16px;
}

.items-table :deep(.el-table__cell) {
  padding-top: 10px;
  padding-bottom: 10px;
}

.total-bar {
  align-self: flex-end;
  color: #6b7280;
  font-size: 14px;

  .money {
    color: #ef4444;
    font-size: 24px;
    font-weight: 700;
    margin-left: 4px;
  }
}

@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .summary-panel {
    flex-direction: column;
  }

  .summary-side {
    text-align: left;
  }

  .detail-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }

  .product-img {
    width: 52px;
    height: 52px;
  }
}
</style>
