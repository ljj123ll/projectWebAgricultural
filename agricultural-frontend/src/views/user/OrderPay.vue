<template>
  <div class="order-pay-page">
    <h1 class="page-title">订单支付</h1>

    <!-- 支付信息 -->
    <div class="pay-info-section">
      <div class="order-info">
        <p class="order-no">订单编号：{{ orderNo }}</p>
        <p class="pay-amount">
          支付金额：<span class="amount">¥{{ orderAmount }}</span>
        </p>
        <p class="order-status" v-if="orderStatusText">当前状态：{{ orderStatusText }}</p>
      </div>
      <el-countdown 
        v-if="showCountdown"
        title="支付剩余时间" 
        :value="countdownTime"
        format="mm:ss"
        @finish="handleTimeout"
      />
    </div>

    <!-- 支付方式 -->
    <div class="pay-method-section">
      <h3>选择支付方式</h3>
      <div class="pay-methods">
        <div 
          class="pay-method" 
          :class="{ active: payMethod === 'alipay' }"
          @click="selectPayMethod('alipay')"
        >
          <div class="method-icon alipay">
            <el-icon size="32"><Wallet /></el-icon>
          </div>
          <div class="method-info">
            <span class="name">支付宝</span>
            <span class="desc">推荐使用</span>
          </div>
          <el-icon v-if="payMethod === 'alipay'" class="check-icon"><CircleCheckFilled /></el-icon>
        </div>

        <div 
          class="pay-method" 
          :class="{ active: payMethod === 'wechat' }"
          @click="selectPayMethod('wechat')"
        >
          <div class="method-icon wechat">
            <el-icon size="32"><ChatDotRound /></el-icon>
          </div>
          <div class="method-info">
            <span class="name">微信支付</span>
            <span class="desc">快捷支付</span>
          </div>
          <el-icon v-if="payMethod === 'wechat'" class="check-icon"><CircleCheckFilled /></el-icon>
        </div>
      </div>
    </div>

    <!-- 支付按钮 -->
    <div class="pay-action">
      <el-button 
        type="danger" 
        size="large"
        class="pay-btn"
        :loading="paying && payAction === 'success'"
        :disabled="!canPay"
        @click="handlePay(true)"
      >
        确认支付 ¥{{ orderAmount }}
      </el-button>
      <el-button 
        type="warning" 
        size="large"
        class="pay-btn"
        :loading="paying && payAction === 'failed'"
        :disabled="!canPay"
        @click="handlePay(false)"
      >
        模拟支付失败
      </el-button>
      <el-button 
        link 
        :disabled="paying || canceling"
        @click="handleCancel"
        class="cancel-btn"
      >
        取消支付
      </el-button>
    </div>

    <!-- 支付异常提示 -->
    <el-alert
      v-if="payError"
      title="支付异常"
      type="error"
      description="支付遇到问题，请重新支付"
      show-icon
      :closable="false"
      class="pay-error"
    >
      <template #default>
        <el-button type="primary" link @click="retryPay">重新支付</el-button>
      </template>
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Wallet, ChatDotRound, CircleCheckFilled } from '@element-plus/icons-vue';
import { cancelOrder, payOrder, getOrderDetail } from '@/apis/order';
import { useOrderStore } from '@/stores/modules/order';

/**
 * 用户端支付页。
 * 用于演示订单支付、支付失败重试、支付超时和取消支付的完整前端流程。
 */

const router = useRouter();
const route = useRoute();
const orderStore = useOrderStore();

const orderId = Number(route.params.orderId);
const orderNo = ref('');
const orderAmount = ref('0.00');
const payMethod = ref<'alipay' | 'wechat'>('alipay');
const paying = ref(false);
const payAction = ref<'success' | 'failed' | ''>('');
const payError = ref(false);
const loadingOrder = ref(false);
const canceling = ref(false);
const orderStatus = ref<number | null>(null);
const countdownTime = ref(Date.now() + 15 * 60 * 1000); // 15分钟倒计时

const canPay = computed(() => !loadingOrder.value && !paying.value && !canceling.value && (orderStatus.value === 1 || orderStatus.value === 6));
const showCountdown = computed(() => canPay.value && countdownTime.value > Date.now());
const orderStatusText = computed(() => {
  if (orderStatus.value == null) return '';
  return orderStore.getStatusText(orderStatus.value);
});

const selectPayMethod = (method: 'alipay' | 'wechat') => {
  if (paying.value || canceling.value) return;
  payMethod.value = method;
};

const redirectByStatus = (status?: number | null) => {
  if (status == null) return;
  if (status === 5) {
    ElMessage.warning('订单已取消');
    router.replace('/orders');
    return;
  }
  if (status !== 1 && status !== 6) {
    ElMessage.success('订单状态已更新');
    router.replace(`/order-detail/${orderId}`);
  }
};

// 支付页初始化入口：读取订单最新状态与支付截止时间，避免重复进入旧状态页面。
const loadOrder = async () => {
  if (!orderId) {
    ElMessage.error('订单不存在');
    router.replace('/orders');
    return;
  }

  loadingOrder.value = true;
  try {
    const res = await getOrderDetail(orderId);
    if (!res) {
      ElMessage.error('订单不存在');
      router.replace('/orders');
      return;
    }

    orderNo.value = res.orderNo || '';
    orderAmount.value = Number(res.totalAmount || 0).toFixed(2);
    orderStatus.value = Number(res.orderStatus ?? 0);
    if (res.payDeadline) {
      const deadline = new Date(res.payDeadline).getTime();
      countdownTime.value = Number.isFinite(deadline) ? deadline : Date.now();
    }

    if (countdownTime.value <= Date.now() && orderStatus.value === 1) {
      ElMessage.warning('该订单已超时，请刷新订单状态');
    }
    redirectByStatus(orderStatus.value);
  } catch (error) {
    console.error('加载订单详情失败', error);
    ElMessage.error('无法获取订单信息');
    router.replace('/orders');
  } finally {
    loadingOrder.value = false;
  }
};

onMounted(async () => {
  await loadOrder();
});

// 支付超时
const handleTimeout = async () => {
  await loadOrder();
  ElMessageBox.alert('订单支付时间已到，请在订单列表中确认最新状态。', '提示', {
    confirmButtonText: '确定',
    callback: () => {
      router.push('/orders');
    }
  });
};

// 支付主流程：按钮防重复点击，成功后跳转详情页，失败则回拉订单状态兜底。
const handlePay = async (success: boolean = true) => {
  if (!canPay.value || paying.value) return;
  paying.value = true;
  payAction.value = success ? 'success' : 'failed';
  payError.value = false;

  try {
    await payOrder(orderId, success);
    ElMessage.success('支付成功');
    router.replace(`/order-detail/${orderId}`);
  } catch (error) {
    console.error('支付失败', error);
    await loadOrder();
    payError.value = true;
    if (!canPay.value) {
      return;
    }
    ElMessage.error(success ? '支付异常，请重新支付' : '已模拟支付失败，请重新支付');
  } finally {
    paying.value = false;
    payAction.value = '';
  }
};

// 重新发起一次支付请求，用于模拟支付失败后的恢复演示。
const retryPay = () => {
  payError.value = false;
  void handlePay(true);
};

// 取消支付本质上是取消订单，答辩时可以从这里串到订单状态回退逻辑。
const handleCancel = () => {
  if (paying.value || canceling.value) return;
  ElMessageBox.confirm('确定要取消支付吗？订单将在15分钟后自动取消', '提示', {
    confirmButtonText: '确定取消',
    cancelButtonText: '继续支付',
    type: 'warning'
  }).then(() => {
    canceling.value = true;
    return cancelOrder(orderId);
  }).then(async () => {
    ElMessage.success('订单已取消');
    await loadOrder();
    router.push('/orders');
  }).catch((error) => {
    if (error === 'cancel') return;
    console.error('取消订单失败', error);
  }).finally(() => {
    canceling.value = false;
  });
};
</script>

<style scoped lang="scss">
.order-pay-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  margin: 0 0 24px;
  text-align: center;
}

.pay-info-section {
  background: #fef0f0;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  margin-bottom: 24px;

  .order-info {
    margin-bottom: 16px;

    .order-no {
      color: #606266;
      margin: 0 0 8px;
    }

    .order-status {
      color: #909399;
      margin: 0;
    }

    .pay-amount {
      font-size: 16px;
      margin: 0;

      .amount {
        font-size: 32px;
        font-weight: bold;
        color: #f56c6c;
      }
    }
  }

  :deep(.el-countdown) {
    .el-statistic__content {
      color: #f56c6c;
      font-size: 20px;
    }
  }
}

.pay-method-section {
  margin-bottom: 32px;

  h3 {
    font-size: 16px;
    margin: 0 0 16px;
  }

  .pay-methods {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .pay-method {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      border: 2px solid #e4e7ed;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #c0c4cc;
      }

      &.active {
        border-color: #67c23a;
        background: #f0f9ff;
      }

      .method-icon {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;

        &.alipay {
          background: #1677ff;
          color: #fff;
        }

        &.wechat {
          background: #07c160;
          color: #fff;
        }
      }

      .method-info {
        flex: 1;
        display: flex;
        flex-direction: column;

        .name {
          font-size: 16px;
          font-weight: 500;
        }

        .desc {
          font-size: 13px;
          color: #909399;
        }
      }

      .check-icon {
        color: #67c23a;
        font-size: 24px;
      }
    }
  }
}

.pay-action {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;

  .pay-btn {
    width: 100%;
    height: 48px;
    font-size: 18px;
  }

  .cancel-btn {
    color: #909399;
  }
}

.pay-error {
  margin-top: 24px;
}

@media (max-width: 768px) {
  .order-pay-page {
    padding: 16px;
  }

  .page-title {
    font-size: 20px;
  }

  .pay-info-section {
    padding: 16px;

    .pay-amount .amount {
      font-size: 28px;
    }
  }
}
</style>
