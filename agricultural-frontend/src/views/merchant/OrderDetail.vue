<template>
  <div class="merchant-order-detail" v-loading="loading">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>订单详情</h2>
      </div>
      <el-button
        v-if="order && Number(order.orderStatus) === 2"
        type="primary"
        @click="openShipDialog"
      >
        立即发货
      </el-button>
    </div>

    <el-empty v-if="!loading && !order" description="订单不存在或无权限查看" />

    <template v-if="order">
      <el-card class="card">
        <div class="status-header">
          <div>
            <div class="order-no">订单号：{{ order.orderNo }}</div>
            <div class="time-text">下单时间：{{ formatDate(order.createTime) }}</div>
          </div>
          <el-tag :type="getStatusType(order.orderStatus)" size="large">
            {{ getStatusText(order.orderStatus) }}
          </el-tag>
        </div>
        <el-steps :active="getActiveStep(order.orderStatus)" simple>
          <el-step title="待付款" />
          <el-step title="待发货" />
          <el-step title="待收货" />
          <el-step title="已完成" />
        </el-steps>
      </el-card>

      <el-card class="card">
        <template #header>商品信息</template>
        <div class="item-list">
          <div v-for="item in getOrderItems(order)" :key="`${item.productId}-${item.productNum}`" class="item-row">
            <img :src="getCoverImage(item.productImg)" :alt="item.productName" class="item-image" />
            <div class="item-main">
              <div class="item-name">{{ item.productName }}</div>
              <div class="item-meta">单价：¥{{ Number(item.productPrice || 0).toFixed(2) }}</div>
            </div>
            <div class="item-count">x{{ item.productNum }}</div>
            <div class="item-amount">¥{{ Number(item.productAmount || Number(item.productPrice || 0) * Number(item.productNum || 0)).toFixed(2) }}</div>
          </div>
        </div>
        <div class="amount-summary">
          <div class="summary-row">
            <span>商品总额</span>
            <span>¥{{ Number(order.totalAmount || 0).toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>运费</span>
            <span>¥0.00</span>
          </div>
          <div class="summary-row total">
            <span>实付金额</span>
            <span>¥{{ Number(order.totalAmount || 0).toFixed(2) }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="card">
        <template #header>收货信息</template>
        <div class="info-row"><span class="label">收货人：</span>{{ order.receiver || '-' }}</div>
        <div class="info-row"><span class="label">手机号：</span>{{ order.receiverPhone || '-' }}</div>
        <div class="info-row"><span class="label">收货地址：</span>{{ order.receiverAddress || '-' }}</div>
        <div class="info-row"><span class="label">买家备注：</span>{{ order.remark || '无' }}</div>
      </el-card>

      <el-card class="card">
        <template #header>物流信息</template>
        <div class="info-row"><span class="label">物流公司：</span>{{ order.logisticsCompany || '暂无' }}</div>
        <div class="info-row"><span class="label">物流单号：</span>{{ order.logisticsNo || '暂无' }}</div>
      </el-card>

      <OrderChatPanel v-if="order.orderNo" :order-no="order.orderNo" role="merchant" />
    </template>

    <el-dialog v-model="shipDialogVisible" title="订单发货" width="420px">
      <el-form :model="shipForm" label-position="top">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中国邮政" value="中国邮政" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.logisticsNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import type { Order, OrderItem } from '@/types';
import { getOrderByNo, shipOrder } from '@/apis/merchant';
import { getFullImageUrl } from '@/utils/image';
import OrderChatPanel from '@/components/OrderChatPanel.vue';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const order = ref<Order | null>(null);
const shipDialogVisible = ref(false);
const shipForm = reactive({
  logisticsCompany: '',
  logisticsNo: ''
});

const orderNo = computed(() => String(route.params.orderNo || '').trim());

const getOrderItems = (value: Order): OrderItem[] => {
  return value.items || value.orderItems || [];
};

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || '';
  return getFullImageUrl(first);
};

const getStatusText = (status?: number) => {
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
  return map[Number(status)] || '未知状态';
};

const getStatusType = (status?: number) => {
  const map: Record<number, string> = {
    1: 'warning',
    2: 'primary',
    3: 'success',
    4: 'success',
    5: 'info',
    6: 'danger',
    7: 'warning',
    8: 'info'
  };
  return map[Number(status)] || 'info';
};

const getActiveStep = (status?: number) => {
  const map: Record<number, number> = {
    1: 1,
    2: 2,
    3: 3,
    4: 4,
    7: 3,
    8: 4
  };
  return map[Number(status)] || 1;
};

const formatDate = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
};

const loadOrderDetail = async () => {
  if (!orderNo.value) return;
  loading.value = true;
  try {
    const res = await getOrderByNo(orderNo.value);
    order.value = res || null;
  } catch (error) {
    console.error('加载订单详情失败', error);
    order.value = null;
  } finally {
    loading.value = false;
  }
};

const openShipDialog = () => {
  shipForm.logisticsCompany = '';
  shipForm.logisticsNo = '';
  shipDialogVisible.value = true;
};

const confirmShip = async () => {
  if (!order.value?.id) return;
  if (!shipForm.logisticsCompany || !shipForm.logisticsNo) {
    ElMessage.warning('请填写完整物流信息');
    return;
  }
  await shipOrder(order.value.id, shipForm);
  ElMessage.success('发货成功');
  shipDialogVisible.value = false;
  window.dispatchEvent(new Event('merchant-pending-refresh'));
  await loadOrderDetail();
};

onMounted(() => {
  void loadOrderDetail();
});
</script>

<style scoped lang="scss">
.merchant-order-detail {
  padding: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;

    h2 {
      margin: 0;
      font-size: 20px;
      color: #2f3b4d;
    }
  }
}

.card {
  margin-bottom: 12px;
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  .order-no {
    font-size: 15px;
    color: #2f3b4d;
    font-weight: 600;
  }

  .time-text {
    font-size: 13px;
    color: #8a97a8;
    margin-top: 4px;
  }
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-row {
  display: grid;
  grid-template-columns: 72px 1fr auto auto;
  gap: 12px;
  align-items: center;
  border: 1px solid #ecf1f7;
  border-radius: 10px;
  padding: 10px;
}

.item-image {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  background: #f6f8fb;
}

.item-name {
  font-size: 14px;
  color: #2f3b4d;
  font-weight: 600;
  margin-bottom: 6px;
}

.item-meta {
  font-size: 12px;
  color: #8a97a8;
}

.item-count {
  color: #6a7687;
  font-size: 13px;
}

.item-amount {
  color: #f56c6c;
  font-size: 15px;
  font-weight: 700;
}

.amount-summary {
  margin-top: 12px;
  border-top: 1px solid #edf2f8;
  padding-top: 10px;

  .summary-row {
    display: flex;
    justify-content: space-between;
    color: #627085;
    margin-bottom: 8px;
  }

  .summary-row.total {
    color: #f56c6c;
    font-size: 16px;
    font-weight: 700;
    margin-top: 4px;
  }
}

.info-row {
  margin-bottom: 10px;
  color: #4d5a6b;
  line-height: 1.7;

  .label {
    color: #8a97a8;
  }
}

@media (max-width: 768px) {
  .item-row {
    grid-template-columns: 64px 1fr;
  }

  .item-count,
  .item-amount {
    justify-self: end;
  }
}
</style>
