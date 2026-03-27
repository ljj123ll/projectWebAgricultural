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

            <!-- 售后中 -->
            <template v-if="order.orderStatus === 7">
              <el-button type="warning" plain @click="viewAfterSale(order)">查看售后</el-button>
              <el-button @click="contactMerchant(order)">联系商家</el-button>
            </template>

            <!-- 已完成 -->
            <template v-if="order.orderStatus === 4">
              <el-button
                v-if="getOrderReviewState(order) === 'approved'"
                type="success"
                plain
                @click="goToReviewed(order)"
              >
                已评价
              </el-button>
              <template v-else-if="getOrderReviewState(order) === 'rejected'">
                <el-button type="danger" plain @click="goToRejected(order)">
                  未通过
                </el-button>
                <el-button type="primary" @click="goToEditRejectedReview(order)">
                  修改评论
                </el-button>
              </template>
              <el-button
                v-else-if="getOrderReviewState(order) === 'pending'"
                type="warning"
                plain
                @click="goToReviewed(order)"
              >
                审核中
              </el-button>
              <el-button v-else type="primary" @click="goToReview(order.id)">
                评价商品
              </el-button>
              <el-button @click="applyAfterSale(order)">申请售后</el-button>
            </template>

            <!-- 已完成售后 -->
            <template v-if="order.orderStatus === 8">
              <el-button type="success" plain @click="viewAfterSale(order)">售后详情</el-button>
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
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useOrderStore } from '@/stores/modules/order';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Comment, Order } from '@/types';
import { getOrders, cancelOrder, receiveOrder, getLogistics, getLogisticsByOrderNo } from '@/apis/order';
import { listComments, listAfterSale as listUserAfterSale } from '@/apis/user';
import { getFullImageUrl } from '@/utils/image';
import { USER_REALTIME_EVENT, parseRealtimePayload } from '@/utils/realtime';

const router = useRouter();
const orderStore = useOrderStore();

const currentTab = ref(0);
const loading = ref(false);
const hiddenOrderIds = ref<number[]>([]);
const commentStateMap = ref<Record<string, { id: number; auditStatus: number }>>({});
const afterSaleNoMap = ref<Record<string, string>>({});
let refreshTimer: ReturnType<typeof setInterval> | null = null;

const tabs = ref([
  { label: '全部', value: 0 },
  { label: '待付款', value: 1 },
  { label: '待发货', value: 2 },
  { label: '待收货', value: 3 },
  { label: '售后中', value: 7 },
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

const getStatusPriority = (status?: number) => {
  const map: Record<number, number> = {
    8: 70, // 已完成售后
    4: 60, // 已完成
    7: 55, // 售后中
    3: 50, // 待收货
    2: 40, // 待发货
    1: 30, // 待付款
    5: 20, // 已取消
    6: 10  // 支付异常
  };
  return map[Number(status)] || 0;
};

const getTimeValue = (value?: string) => {
  if (!value) return 0;
  const parsed = new Date(value).getTime();
  return Number.isFinite(parsed) ? parsed : 0;
};

const mergeDuplicateOrders = (list: Order[]) => {
  const map = new Map<string, Order>();
  list.forEach((order) => {
    const key = order.orderNo || String(order.id);
    const existing = map.get(key);
    if (!existing) {
      map.set(key, order);
      return;
    }

    const currentPriority = getStatusPriority(order.orderStatus);
    const existingPriority = getStatusPriority(existing.orderStatus);
    const currentTime = Math.max(getTimeValue(order.updateTime), getTimeValue(order.createTime));
    const existingTime = Math.max(getTimeValue(existing.updateTime), getTimeValue(existing.createTime));

    const shouldReplace =
      currentPriority > existingPriority ||
      (currentPriority === existingPriority && (currentTime > existingTime || order.id > existing.id));

    if (shouldReplace) {
      map.set(key, order);
      return;
    }

    if ((!existing.orderItems || existing.orderItems.length === 0) && order.orderItems?.length) {
      existing.orderItems = order.orderItems;
    }
  });

  return Array.from(map.values()).sort((a, b) => {
    return getTimeValue(b.createTime) - getTimeValue(a.createTime);
  });
};

const loadOrders = async () => {
  loading.value = true;
  try {
    const res = await getOrders({ pageNum: 1, pageSize: 100 });
    const normalized = (res?.list || []).map(normalizeOrder);
    orders.value = mergeDuplicateOrders(normalized);
    await loadReviewedComments();
    await loadAfterSaleRecords();
  } finally {
    loading.value = false;
  }
};

const handleRealtimeRefresh = (event: Event) => {
  const payload = parseRealtimePayload(event);
  if (payload.reason === 'ORDER_SHIPPED') {
    currentTab.value = 3;
  }
  void loadOrders();
};

onMounted(() => {
  void loadOrders();
  window.addEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
  // SSE 异常断开时的兜底同步
  refreshTimer = setInterval(() => {
    void loadOrders();
  }, 30000);
});

onUnmounted(() => {
  window.removeEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
});

const visibleOrders = computed(() => orders.value.filter(order => !hiddenOrderIds.value.includes(order.id)));

const makeReviewedKey = (orderNo: string, productId: number) => `${orderNo}::${productId}`;

const loadReviewedComments = async () => {
  try {
    const res = await listComments({ pageNum: 1, pageSize: 1000 });
    const next: Record<string, { id: number; auditStatus: number }> = {};
    (res?.list || []).forEach((comment: Comment) => {
      if (!comment.orderNo || !comment.productId) return;
      const key = makeReviewedKey(comment.orderNo, Number(comment.productId));
      const current = next[key];
      const nextId = Number(comment.id || 0);
      if (!current || nextId > current.id) {
        next[key] = {
          id: nextId,
          auditStatus: Number(comment.auditStatus ?? 0)
        };
      }
    });
    commentStateMap.value = next;
  } catch (error) {
    console.warn('加载评价记录失败', error);
    commentStateMap.value = {};
  }
};

const loadAfterSaleRecords = async () => {
  try {
    const res = await listUserAfterSale({ pageNum: 1, pageSize: 1000 });
    const next: Record<string, { id: number; afterSaleNo: string }> = {};
    (res?.list || []).forEach((item: any) => {
      if (!item?.orderNo || !item?.afterSaleNo) return;
      const current = next[item.orderNo];
      const id = Number(item.id || 0);
      if (!current || id > current.id) {
        next[item.orderNo] = { id, afterSaleNo: String(item.afterSaleNo) };
      }
    });
    const map: Record<string, string> = {};
    Object.keys(next).forEach((orderNo) => {
      const record = next[orderNo];
      if (record) {
        map[orderNo] = record.afterSaleNo;
      }
    });
    afterSaleNoMap.value = map;
  } catch (error) {
    console.warn('加载售后记录失败', error);
    afterSaleNoMap.value = {};
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
  if (currentTab.value === 4) {
    return visibleOrders.value.filter(order => order.orderStatus === 4 || order.orderStatus === 8);
  }
  return visibleOrders.value.filter(order => order.orderStatus === currentTab.value);
});

const getTabCount = (tabValue: number) => {
  if (tabValue === 0) return visibleOrders.value.length;
  if (tabValue === 4) return visibleOrders.value.filter(order => order.orderStatus === 4 || order.orderStatus === 8).length;
  return visibleOrders.value.filter(order => order.orderStatus === tabValue).length;
};

const getOrderReviewState = (order: Order): 'approved' | 'rejected' | 'pending' | 'unreviewed' => {
  const items = order.orderItems || [];
  if (items.length === 0) return 'unreviewed';

  let hasRejected = false;
  let hasPending = false;
  let hasMissing = false;
  let allApproved = true;

  items.forEach((item) => {
    const key = makeReviewedKey(order.orderNo, item.productId);
    const meta = commentStateMap.value[key];
    if (!meta) {
      hasMissing = true;
      allApproved = false;
      return;
    }
    if (meta.auditStatus === 2) {
      hasRejected = true;
      allApproved = false;
      return;
    }
    if (meta.auditStatus === 0) {
      hasPending = true;
      allApproved = false;
      return;
    }
    if (meta.auditStatus !== 1) {
      allApproved = false;
    }
  });

  if (hasRejected) return 'rejected';
  if (allApproved) return 'approved';
  if (!hasMissing && hasPending) return 'pending';
  return 'unreviewed';
};

const getFirstRejectedMeta = (order: Order): { commentId: number; productId: number } | null => {
  const items = order.orderItems || [];
  for (const item of items) {
    const key = makeReviewedKey(order.orderNo, item.productId);
    const meta = commentStateMap.value[key];
    if (meta && meta.auditStatus === 2) {
      return {
        commentId: meta.id,
        productId: item.productId
      };
    }
  }
  return null;
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
  let logistics: any = null;
  try {
    logistics = await getLogistics(order.id);
  } catch (error) {
    console.warn('按订单ID查询物流失败，尝试按订单号查询', error);
  }
  if ((!logistics?.logisticsNo || !logistics?.logisticsCompany) && order.orderNo) {
    try {
      const fallback = await getLogisticsByOrderNo(order.orderNo);
      if (fallback) logistics = fallback;
    } catch (error) {
      console.warn('按订单号查询物流失败', error);
    }
  }
  const company = logistics?.logisticsCompany || '暂无';
  const no = logistics?.logisticsNo || '暂无';
  const abnormal = logistics?.abnormalReason ? `\n异常说明：${logistics.abnormalReason}` : '';
  ElMessageBox.alert(
    `物流公司：${company}\n物流单号：${no}${abnormal}`,
    '物流信息',
    {
      confirmButtonText: '确定'
    }
  );
};

const applyAfterSale = (order: Order) => {
  router.push(`/after-sale/${order.orderNo}`);
};

const viewAfterSale = (order: Order) => {
  const no = afterSaleNoMap.value[order.orderNo];
  if (!no) {
    ElMessage.warning('未找到对应售后单，请稍后重试');
    return;
  }
  router.push(`/after-sale-detail/${no}`);
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

const goToRejected = (order: Order) => {
  const rejected = getFirstRejectedMeta(order);
  router.push({
    path: '/reviews',
    query: {
      orderNo: order.orderNo,
      ...(rejected ? { productId: String(rejected.productId) } : {})
    }
  });
};

const goToEditRejectedReview = (order: Order) => {
  const rejected = getFirstRejectedMeta(order);
  router.push({
    path: `/order-detail/${order.id}`,
    query: {
      review: '1',
      editRejected: '1',
      ...(rejected ? { editCommentId: String(rejected.commentId), productId: String(rejected.productId) } : {})
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
