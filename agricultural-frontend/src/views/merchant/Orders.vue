<template>
  <div class="merchant-orders">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="待发货" name="2" />
      <el-tab-pane label="已发货" name="3" />
      <el-tab-pane label="已完成" name="4" />
      <el-tab-pane label="售后中" name="aftersale" />
    </el-tabs>

    <el-table v-if="!isAfterSaleTab" :data="orderList" style="width: 100%" v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column label="商品" min-width="260">
        <template #default="scope">
          <div v-for="(item, idx) in getOrderItems(scope.row)" :key="`${scope.row.id}-${idx}`">
            {{ item.productName }} x {{ item.productNum }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120">
        <template #default="scope">¥{{ Number(scope.row.totalAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="收货信息" min-width="220">
        <template #default="scope">
          <div>{{ scope.row.receiver }} {{ scope.row.receiverPhone }}</div>
          <div class="address-ellipsis">{{ scope.row.receiverAddress }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag>{{ getStatusText(scope.row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button
            v-if="scope.row.orderStatus === 2"
            type="primary"
            size="small"
            :loading="isShipActionLocked(scope.row.id)"
            :disabled="shipSubmitting || shipDialogVisible"
            @click="handleShip(scope.row)"
          >发货</el-button>
          <el-button type="text" size="small" @click="goDetail(scope.row.orderNo)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-table v-else :data="afterSaleList" style="width: 100%" v-loading="loading">
      <el-table-column prop="afterSaleNo" label="售后单号" width="180" />
      <el-table-column prop="orderNo" label="关联订单号" width="180" />
      <el-table-column label="售后类型" width="120">
        <template #default="scope">
          {{ getAfterSaleTypeText(scope.row.afterSaleType) }}
        </template>
      </el-table-column>
      <el-table-column prop="applyReason" label="申请原因" min-width="220" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag type="warning">{{ getAfterSaleStatusText(scope.row.afterSaleStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default>
          <el-button type="text" size="small" @click="goAfterSales">去处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="shipDialogVisible"
      title="订单发货"
      width="400px"
      :close-on-click-modal="!shipSubmitting"
      :show-close="!shipSubmitting"
      @closed="handleShipDialogClosed"
    >
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中国邮政" value="中国邮政" />
            <el-option label="中通快递" value="中通快递" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.logisticsNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button :disabled="shipSubmitting" @click="closeShipDialog">取消</el-button>
          <el-button type="primary" :loading="shipSubmitting" @click="confirmShip">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { Order, OrderItem } from '@/types';
import { listAfterSale, listOrders, shipOrder } from '@/apis/merchant';
import { AFTER_SALE_STATUS, getAfterSaleStatusText as getAfterSaleStatusLabel } from '@/utils/afterSale';

/**
 * 商家订单页。
 * 核心用于展示待发货订单、执行发货、查看售后待办，是商家履约流程的主要页面。
 */

const route = useRoute();
const router = useRouter();
const activeTab = ref<'2' | '3' | '4' | 'aftersale'>('2');
const orderList = ref<Order[]>([]);
const afterSaleList = ref<any[]>([]);
const loading = ref(false);
const latestRequestNo = ref(0);
let refreshTimer: ReturnType<typeof setInterval> | null = null;

const shipDialogVisible = ref(false);
const currentOrderId = ref<number>(0);
const shipSubmitting = ref(false);
const shipActionOrderId = ref<number>(0);
const shipForm = reactive({
  logisticsCompany: '',
  logisticsNo: ''
});

const isAfterSaleTab = computed(() => activeTab.value === 'aftersale');

const getOrderItems = (order: any): OrderItem[] => {
  return order?.items || order?.orderItems || [];
};

const getStatusText = (status: number) => {
  if (status === 2) return '待发货';
  if (status === 3) return '已发货';
  if (status === 4) return '已完成';
  if (status === 7) return '售后中';
  if (status === 8) return '已完成售后';
  if (status === 5) return '已取消';
  return '未知状态';
};

const getAfterSaleTypeText = (type: number) => {
  if (type === 1) return '仅退款';
  if (type === 2) return '退货退款';
  if (type === 3) return '换货';
  return '未知类型';
};

const getAfterSaleStatusText = (status: number) => {
  return getAfterSaleStatusLabel(status);
};

const formatDate = (date?: string) => {
  if (!date) return '-';
  return String(date).replace('T', ' ').slice(0, 19);
};

const resetShipDialogState = () => {
  currentOrderId.value = 0;
  shipActionOrderId.value = 0;
  shipForm.logisticsCompany = '';
  shipForm.logisticsNo = '';
};

const isShipActionLocked = (orderId?: number) => {
  const id = Number(orderId || 0);
  return id > 0 && shipActionOrderId.value === id;
};

// 打开发货弹窗，准备填写物流公司和物流单号。
const handleShip = (row: Order) => {
  const orderId = Number(row.id || 0);
  if (!orderId || shipSubmitting.value || shipDialogVisible.value) return;
  shipActionOrderId.value = orderId;
  currentOrderId.value = orderId;
  shipForm.logisticsCompany = '';
  shipForm.logisticsNo = '';
  shipDialogVisible.value = true;
};

const closeShipDialog = () => {
  if (shipSubmitting.value) return;
  shipDialogVisible.value = false;
};

const handleShipDialogClosed = () => {
  if (shipSubmitting.value) return;
  resetShipDialogState();
};

// 发货提交入口：对应商家端最常演示的“填写物流并发货”功能。
const confirmShip = async () => {
  if (shipSubmitting.value) return;
  if (!shipForm.logisticsCompany || !shipForm.logisticsNo) {
    ElMessage.warning('请填写完整物流信息');
    return;
  }
  shipSubmitting.value = true;
  try {
    await shipOrder(currentOrderId.value, shipForm);
    shipDialogVisible.value = false;
    ElMessage.success('发货成功');
    window.dispatchEvent(new Event('merchant-pending-refresh'));
    await loadData();
  } catch (error) {
    console.error(error);
    ElMessage.error('发货失败，请稍后重试');
  } finally {
    shipSubmitting.value = false;
    if (!shipDialogVisible.value) {
      resetShipDialogState();
    }
  }
};

const goDetail = (orderNo?: string) => {
  if (!orderNo) return;
  router.push(`/merchant/order-detail/${orderNo}`);
};

const goAfterSales = () => {
  router.push('/merchant/after-sales');
};

const loadOrderData = async (status: number, requestNo: number) => {
  const res = await listOrders({ pageNum: 1, pageSize: 50, orderStatus: status });
  if (requestNo !== latestRequestNo.value) return;
  const list = (res?.list || []) as Order[];
  if (status === 4) {
    // 已完成页签包含普通完成与售后完成
    orderList.value = list.filter(item => [4, 8].includes(Number(item?.orderStatus)));
    return;
  }
  // 前端兜底按状态再过滤一次，避免接口偶发返回与页签状态不一致的数据
  orderList.value = list.filter(item => Number(item?.orderStatus) === status);
};

const loadAfterSaleData = async (requestNo: number) => {
  const results = await Promise.allSettled([
    listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.PENDING_MERCHANT }),
    listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND }),
    listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_USER_RETURN }),
    listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.ADMIN })
  ]);
  if (requestNo !== latestRequestNo.value) return;
  const successList = results
    .filter((item): item is PromiseFulfilledResult<any> => item.status === 'fulfilled')
    .map(item => item.value);
  const merged = [
    ...successList.flatMap(item => item?.list || [])
  ];
  const uniqueMap = new Map<number, any>();
  merged.forEach((item: any) => {
    if (item?.id) uniqueMap.set(item.id, item);
  });
  afterSaleList.value = [...uniqueMap.values()].sort((a, b) => {
    return new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime();
  });
};

// 根据当前页签加载订单或售后数据。
const loadData = async () => {
  const requestNo = ++latestRequestNo.value;
  loading.value = true;
  try {
    if (isAfterSaleTab.value) {
      await loadAfterSaleData(requestNo);
    } else {
      const status = Number(activeTab.value);
      if (![2, 3, 4].includes(status)) {
        orderList.value = [];
        return;
      }
      await loadOrderData(status, requestNo);
    }
  } catch (error) {
    console.error('商家订单页加载失败', error);
    if (requestNo === latestRequestNo.value) {
      orderList.value = [];
      afterSaleList.value = [];
    }
    ElMessage.error('订单数据加载失败，请稍后刷新重试');
  } finally {
    if (requestNo === latestRequestNo.value) {
      loading.value = false;
    }
  }
};

const applyRouteStatus = () => {
  const routeStatus = String(route.query.status || '').trim();
  if (routeStatus === '2' || routeStatus === '3' || routeStatus === '4' || routeStatus === 'aftersale') {
    activeTab.value = routeStatus;
  }
};

const handlePendingRefresh = () => {
  void loadData();
};

onMounted(() => {
  applyRouteStatus();
  window.addEventListener('merchant-pending-refresh', handlePendingRefresh);
  refreshTimer = setInterval(() => {
    void loadData();
  }, 15000);
});

onUnmounted(() => {
  window.removeEventListener('merchant-pending-refresh', handlePendingRefresh);
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
});

watch(activeTab, () => {
  void loadData();
}, { immediate: true });

watch(() => route.query.status, () => {
  applyRouteStatus();
});
</script>

<style scoped>
.merchant-orders {
  padding: 12px;
}

.address-ellipsis {
  color: #666;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
