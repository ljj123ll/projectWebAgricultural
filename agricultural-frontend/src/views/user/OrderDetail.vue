<template>
  <div class="order-detail-page">
    <el-button link @click="router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <div class="order-status">
      <el-icon size="48" :color="statusColor"><CircleCheckFilled /></el-icon>
      <h2>{{ statusText }}</h2>
    </div>

    <div class="order-info">
      <p>订单编号：{{ order.orderNo }}</p>
      <p>下单时间：{{ formatDate(order.createTime) }}</p>
    </div>

    <div class="order-items">
      <h3>商品信息</h3>
      <div v-for="item in order.orderItems" :key="item.id" class="item">
        <img :src="item.productImg" :alt="item.productName" />
        <div class="item-info">
          <h4>{{ item.productName }}</h4>
          <p>¥{{ item.productPrice }} × {{ item.productNum }}</p>
        </div>
        <span class="subtotal">¥{{ (item.productPrice * item.productNum).toFixed(2) }}</span>
      </div>
    </div>

    <div class="order-amount">
      <div class="amount-row">
        <span>商品总额</span>
        <span>¥{{ order.totalAmount.toFixed(2) }}</span>
      </div>
      <div class="amount-row">
        <span>运费</span>
        <span>¥0.00</span>
      </div>
      <div class="amount-row total">
        <span>实付金额</span>
        <span>¥{{ order.totalAmount.toFixed(2) }}</span>
      </div>
    </div>

    <div v-if="order.orderStatus === 4" class="review-action">
      <el-button type="primary" size="large" @click="openReviewDialog">评价商品</el-button>
    </div>

    <el-dialog v-model="reviewDialogVisible" title="评价商品" width="90%">
      <el-form :model="reviewForm" label-position="top">
        <el-form-item label="选择商品">
          <el-select v-model="reviewForm.productId" placeholder="请选择商品" style="width: 100%">
            <el-option
              v-for="item in order.orderItems"
              :key="item.productId"
              :label="item.productName"
              :value="item.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.score" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入评价内容"
          />
        </el-form-item>
        <el-form-item label="上传图片/视频">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :file-list="reviewForm.files"
            :on-remove="handleFileRemove"
            :on-change="handleFileChange"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useOrderStore } from '@/stores/modules/order';
import { ElMessage } from 'element-plus';
import { Plus, ArrowLeft, CircleCheckFilled } from '@element-plus/icons-vue';
import { getOrderDetail } from '@/apis/order';
import { submitComment } from '@/apis/user';
import type { Order } from '@/types';

const router = useRouter();
const route = useRoute();
const orderStore = useOrderStore();

const orderId = Number(route.params.orderId);
const order = ref<Order | null>(null);

const reviewDialogVisible = ref(false);
const reviewForm = reactive({
  productId: 0,
  score: 5,
  content: '',
  imgUrls: ''
});
const fileList = ref<any[]>([]);

const statusText = computed(() => {
  if (!order.value) return '';
  // 简单映射，或者使用 store 方法
  const map: Record<number, string> = {
    1: '待付款',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消',
    6: '支付异常'
  };
  return map[order.value.orderStatus] || '未知状态';
});

const statusColor = computed(() => {
  if (!order.value) return '#909399';
  const map: Record<number, string> = {
    1: '#e6a23c', // Warning
    2: '#409eff', // Primary
    3: '#409eff',
    4: '#67c23a', // Success
    5: '#909399', // Info
    6: '#f56c6c'  // Danger
  };
  return map[order.value.orderStatus] || '#909399';
});

const formatDate = (date: string) => {
  if (!date) return '';
  return date.substring(0, 16).replace('T', ' ');
};

const openReviewDialog = () => {
  if (order.value && order.value.orderItems.length > 0) {
    reviewForm.productId = order.value.orderItems[0].productId;
  }
  reviewDialogVisible.value = true;
};

const handleFileChange = (_file: any, files: any[]) => {
  fileList.value = files;
};

const handleFileRemove = (_file: any, files: any[]) => {
  fileList.value = files;
};

const submitReview = async () => {
  if (!reviewForm.productId) {
    ElMessage.warning('请选择商品');
    return;
  }
  if (!reviewForm.content.trim()) {
    ElMessage.warning('请输入评价内容');
    return;
  }
  
  try {
    // 模拟图片上传，实际应先上传图片获取URL
    // reviewForm.imgUrls = fileList.value.map(f => f.url).join(',');
    
    await submitComment({
      orderNo: order.value?.orderNo,
      productId: reviewForm.productId,
      score: reviewForm.score,
      content: reviewForm.content,
      imgUrls: reviewForm.imgUrls
    });
    
    ElMessage.success('评价提交成功');
    reviewDialogVisible.value = false;
    reviewForm.content = '';
    fileList.value = [];
  } catch (error) {
    console.error('评价失败', error);
    ElMessage.error('评价提交失败');
  }
};

onMounted(async () => {
  if (!orderId) {
    ElMessage.error('订单不存在');
    router.back();
    return;
  }
  
  try {
    const res = await getOrderDetail(orderId);
    if (res) {
      order.value = res;
    }
  } catch (error) {
    console.error('获取订单详情失败', error);
    ElMessage.error('获取订单详情失败');
  }
  
  if (route.query.review === '1') {
    // 等待数据加载
    setTimeout(() => openReviewDialog(), 500);
  }
});
</script>

<style scoped lang="scss">
.order-detail-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;

  .back-btn {
    margin-bottom: 16px;
    font-size: 16px;
  }
}

.order-status {
  text-align: center;
  padding: 32px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 24px;

  h2 {
    margin: 16px 0 0;
    font-size: 20px;
  }
}

.order-info {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 24px;

  p {
    margin: 0 0 8px;
    color: #606266;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.order-items {
  margin-bottom: 24px;

  h3 {
    font-size: 16px;
    margin: 0 0 16px;
  }

  .item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 0;
    border-bottom: 1px solid #e4e7ed;

    &:last-child {
      border-bottom: none;
    }

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

    .subtotal {
      font-size: 16px;
      font-weight: 500;
      color: #f56c6c;
    }
  }
}

.order-amount {
  border-top: 1px solid #e4e7ed;
  padding-top: 16px;

  .amount-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 15px;

    &.total {
      font-size: 18px;
      font-weight: 500;
      color: #f56c6c;
    }
  }
}

.review-action {
  margin-top: 20px;
  text-align: right;
}

@media (max-width: 768px) {
  .order-detail-page {
    padding: 16px;
  }
}
</style>
