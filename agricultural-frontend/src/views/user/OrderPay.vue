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
      </div>
      <el-countdown 
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
          @click="payMethod = 'alipay'"
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
          @click="payMethod = 'wechat'"
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
        :loading="paying"
        @click="handlePay"
      >
        确认支付 ¥{{ orderAmount }}
      </el-button>
      <el-button 
        link 
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
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Wallet, ChatDotRound, CircleCheckFilled } from '@element-plus/icons-vue';
import { payOrder, getOrderDetail } from '@/apis/order';

const router = useRouter();
const route = useRoute();

const orderId = Number(route.params.orderId);
const orderNo = ref('');
const orderAmount = ref('0.00');
const payMethod = ref<'alipay' | 'wechat'>('alipay');
const paying = ref(false);
const payError = ref(false);
const countdownTime = ref(Date.now() + 15 * 60 * 1000); // 15分钟倒计时

// 获取订单金额
onMounted(async () => {
  if (!orderId) {
    ElMessage.error('订单不存在');
    router.replace('/orders');
    return;
  }
  
  try {
    const res = await getOrderDetail(orderId);
    if (res) {
      orderNo.value = res.orderNo;
      orderAmount.value = res.totalAmount.toFixed(2);
      // 检查订单状态，如果已支付则跳转
      if (res.orderStatus > 1 && res.orderStatus !== 5 && res.orderStatus !== 6) {
        ElMessage.success('订单已支付');
        router.replace(`/order-detail/${orderId}`);
      } else if (res.orderStatus === 5) {
        ElMessage.warning('订单已取消');
        router.replace('/orders');
      }
    }
  } catch (error) {
    console.error('加载订单详情失败', error);
    ElMessage.error('无法获取订单信息');
  }
});

// 支付超时
const handleTimeout = () => {
  ElMessageBox.alert('订单支付超时，已自动取消', '提示', {
    confirmButtonText: '确定',
    callback: () => {
      router.push('/orders');
    }
  });
};

// 处理支付
const handlePay = async () => {
  paying.value = true;
  payError.value = false;

  try {
    // 调用支付接口
    // 文档：POST /user/orders/{id}/pay
    await payOrder(orderId);
    
    ElMessage.success('支付成功');
    // 跳转到订单详情
    router.replace(`/order-detail/${orderId}`);
    
  } catch (error) {
    console.error('支付失败', error);
    payError.value = true;
    ElMessage.error('支付失败，请重试');
  } finally {
    paying.value = false;
  }
};

// 重新支付
const retryPay = () => {
  payError.value = false;
  handlePay();
};

// 取消支付
const handleCancel = () => {
  ElMessageBox.confirm('确定要取消支付吗？订单将在15分钟后自动取消', '提示', {
    confirmButtonText: '确定取消',
    cancelButtonText: '继续支付',
    type: 'warning'
  }).then(() => {
    ElMessage.info('订单已取消');
    router.push('/orders');
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
