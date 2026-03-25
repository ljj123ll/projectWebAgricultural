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

    <div v-if="order" class="order-info">
      <p>订单编号：{{ order.orderNo }}</p>
      <p>下单时间：{{ formatDate(order.createTime) }}</p>
    </div>

    <div v-if="order" class="order-items">
      <h3>商品信息</h3>
      <div v-for="item in order.items || order.orderItems" :key="item.productId || item.id" class="item">
        <img :src="getCoverImage(item.productImg)" :alt="item.productName" />
        <div class="item-info">
          <h4>{{ item.productName }}</h4>
          <p>¥{{ item.productPrice }} × {{ item.productNum }}</p>
        </div>
        <span class="subtotal">¥{{ (item.productPrice * item.productNum).toFixed(2) }}</span>
      </div>
    </div>

    <div v-if="order" class="order-amount">
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

    <OrderChatPanel v-if="order?.orderNo" :order-no="order.orderNo" role="user" class="chat-panel" />

    <div v-if="order && order.orderStatus === 4" class="review-action">
      <el-button type="primary" size="large" @click="openReviewDialog">评价商品</el-button>
    </div>

    <el-dialog v-model="reviewDialogVisible" title="评价商品" width="90%">
      <el-form :model="reviewForm" label-position="top">
        <el-form-item label="选择商品">
          <el-select v-model="reviewForm.productId" placeholder="请选择商品" style="width: 100%" @change="handleProductChange">
            <el-option
              v-for="item in order?.items || order?.orderItems"
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
            :action="uploadAction"
            list-type="picture-card"
            :file-list="fileList"
            accept="image/*,video/*"
            multiple
            :limit="6"
            :on-success="handleUploadSuccess"
            :on-remove="handleFileRemove"
            :on-exceed="handleExceed"
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
import { ElMessage } from 'element-plus';
import { Plus, ArrowLeft, CircleCheckFilled } from '@element-plus/icons-vue';
import { getOrderDetail } from '@/apis/order';
import { listComments, submitComment, updateComment } from '@/apis/user';
import type { Comment, Order } from '@/types';
import { getFullImageUrl } from '@/utils/image';
import OrderChatPanel from '@/components/OrderChatPanel.vue';

const router = useRouter();
const route = useRoute();

const orderId = Number(route.params.orderId);
const order = ref<Order | null>(null);
const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/common/upload`;

const getCoverImage = (raw?: string) => {
  if (!raw) return '';
  const first = raw
    .split(',')
    .map(item => item.trim())
    .find(Boolean) || '';
  return getFullImageUrl(first);
};

const reviewDialogVisible = ref(false);
const editingCommentId = ref<number | null>(null);
const commentMapByProduct = ref<Record<number, Comment>>({});
const reviewForm = reactive({
  productId: 0,
  score: 5,
  content: '',
  imgUrls: '',
  mediaUrls: ''
});
const fileList = ref<any[]>([]);

const parseMediaList = (raw?: string) => {
  if (!raw) return [];
  return raw
    .split(',')
    .map(item => item.trim())
    .filter(Boolean);
};

const normalizeImageValue = (raw?: string) => {
  if (!raw) return '';
  if (raw.startsWith('http://') || raw.startsWith('https://')) {
    try {
      const parsed = new URL(raw);
      let path = parsed.pathname || '';
      if (path.startsWith('/api/')) {
        path = path.slice(4);
      }
      return path || raw;
    } catch {
      return raw;
    }
  }
  if (raw.startsWith('/api/')) return raw.slice(4);
  return raw;
};

const buildUploadFileList = (raw?: string) => {
  return parseMediaList(raw).map((url, index) => ({
    uid: `${Date.now()}-${index}`,
    name: `media-${index + 1}`,
    status: 'success',
    url: getFullImageUrl(url)
  }));
};

const isVideoUrl = (url: string) => {
  const target = (url || '').toLowerCase();
  return target.endsWith('.mp4')
    || target.endsWith('.webm')
    || target.endsWith('.mov')
    || target.endsWith('.avi')
    || target.endsWith('.mkv')
    || target.includes('video');
};

const toServerUrl = (raw?: string) => normalizeImageValue(raw || '');

const loadOrderComments = async () => {
  if (!order.value?.orderNo) return;
  try {
    const res = await listComments({ pageNum: 1, pageSize: 1000 });
    const list = ((res?.list || []) as Comment[]).filter(item => item.orderNo === order.value?.orderNo);
    const map: Record<number, Comment> = {};
    list.forEach((item) => {
      const productId = Number(item.productId || 0);
      if (!productId) return;
      if (!map[productId] || Number(item.id || 0) > Number(map[productId].id || 0)) {
        map[productId] = item;
      }
    });
    commentMapByProduct.value = map;
  } catch (error) {
    console.warn('加载订单评价记录失败', error);
    commentMapByProduct.value = {};
  }
};

const statusText = computed(() => {
  if (!order.value) return '';
  // 简单映射，或者使用 store 方法
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
    6: '#f56c6c',  // Danger
    7: '#e6a23c',
    8: '#67c23a'
  };
  return map[order.value.orderStatus] || '#909399';
});

const formatDate = (date: string) => {
  if (!date) return '';
  return date.substring(0, 16).replace('T', ' ');
};

const openReviewDialog = () => {
  const items = order.value?.items || order.value?.orderItems;
  if (!items || items.length === 0) return;

  const queryProductId = Number(route.query.productId || 0);
  if (queryProductId > 0 && items.some(item => Number(item.productId) === queryProductId)) {
    reviewForm.productId = queryProductId;
  } else {
    const firstRejected = items.find(item => {
      const comment = commentMapByProduct.value[Number(item.productId)];
      return Number(comment?.auditStatus) === 2;
    });
    reviewForm.productId = Number(firstRejected?.productId || items[0]?.productId || 0);
  }
  applyCommentToFormByProduct(reviewForm.productId);
  reviewDialogVisible.value = true;
};

const applyCommentToFormByProduct = (productId: number) => {
  const comment = commentMapByProduct.value[Number(productId)];
  if (comment && Number(comment.auditStatus) === 2) {
    editingCommentId.value = Number(comment.id);
    reviewForm.score = Number(comment.score || 5);
    reviewForm.content = comment.content || '';
    reviewForm.imgUrls = comment.imgUrls || '';
    const mediaRaw = comment.mediaUrls || comment.imgUrls || '';
    reviewForm.mediaUrls = mediaRaw;
    fileList.value = buildUploadFileList(mediaRaw);
    return;
  }
  editingCommentId.value = null;
  reviewForm.score = 5;
  reviewForm.content = '';
  reviewForm.imgUrls = '';
  reviewForm.mediaUrls = '';
  fileList.value = [];
};

const handleProductChange = (productId: number) => {
  applyCommentToFormByProduct(productId);
};

const syncUploadedImages = () => {
  const mediaUrls = fileList.value
    .map(file => {
      if (file?.response?.code === 200) {
        const data = file.response.data;
        if (typeof data === 'string') return data;
        if (typeof data?.url === 'string') return data.url;
      }
      return toServerUrl(file?.url || '');
    })
    .filter(Boolean)
    .join(',');

  reviewForm.mediaUrls = mediaUrls;
  reviewForm.imgUrls = mediaUrls
    .split(',')
    .map(item => item.trim())
    .filter(item => item && !isVideoUrl(item))
    .join(',');
};

const handleUploadSuccess = (response: any, _file: any, files: any[]) => {
  if (response?.code !== 200) {
    ElMessage.error(response?.msg || '图片上传失败');
    return;
  }
  fileList.value = files;
  syncUploadedImages();
};

const handleFileRemove = (_file: any, files: any[]) => {
  fileList.value = files;
  syncUploadedImages();
};

const handleExceed = () => {
  ElMessage.warning('最多上传6个图片/视频文件');
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
    syncUploadedImages();
    if (editingCommentId.value) {
      await updateComment(editingCommentId.value, {
        score: reviewForm.score,
        content: reviewForm.content,
        imgUrls: reviewForm.imgUrls,
        mediaUrls: reviewForm.mediaUrls
      });
      ElMessage.success('评论已修改并重新提交审核');
    } else {
      const existing = commentMapByProduct.value[Number(reviewForm.productId)];
      if (existing) {
        const auditStatus = Number(existing.auditStatus ?? 0);
        if (auditStatus === 0) {
          ElMessage.warning('该商品评论正在审核中，请耐心等待');
          return;
        }
        if (auditStatus === 1) {
          ElMessage.warning('该商品评论已审核通过，无需重复评价');
          return;
        }
        if (auditStatus === 2) {
          ElMessage.warning('该商品评论审核未通过，请点击“修改评论”后重新提交');
          return;
        }
      }
      await submitComment({
        orderNo: order.value?.orderNo,
        productId: reviewForm.productId,
        score: reviewForm.score,
        content: reviewForm.content,
        imgUrls: reviewForm.imgUrls,
        mediaUrls: reviewForm.mediaUrls
      });
      ElMessage.success('评价已提交，等待管理员审核后即可查看评论');
    }
    reviewDialogVisible.value = false;
    reviewForm.content = '';
    reviewForm.imgUrls = '';
    reviewForm.mediaUrls = '';
    fileList.value = [];
    editingCommentId.value = null;
    await loadOrderComments();
    router.replace(`/product/${reviewForm.productId}?tab=reviews`);
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
      await loadOrderComments();
    }
  } catch (error) {
    console.error('获取订单详情失败', error);
    ElMessage.error('获取订单详情失败');
  }
  
  if (route.query.review === '1') {
    openReviewDialog();
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

.chat-panel {
  margin-top: 20px;
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
