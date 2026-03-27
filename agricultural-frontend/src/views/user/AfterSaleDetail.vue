<template>
  <div class="after-sale-detail-page">
    <el-button link @click="router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <h1 class="page-title">售后详情</h1>

    <div class="status-section">
      <el-icon size="48" :color="statusColor"><CircleCheckFilled /></el-icon>
      <h2>{{ statusTitle }}</h2>
      <p>{{ statusDesc }}</p>
      <el-tag class="status-tag" :type="statusTagType">{{ currentStatusText }}</el-tag>
      <p class="responsible-tip">{{ responsibleLabel }}</p>
    </div>

    <div class="info-section">
      <h3>售后信息</h3>
      <p>售后单号：{{ afterSaleNo }}</p>
      <p>售后类型：{{ afterSaleTypeText }}</p>
      <p>申请原因：{{ afterSale?.applyReason || '-' }}</p>
      <p>申请时间：{{ formatDate(afterSale?.createTime) }}</p>
      <p v-if="afterSale?.handleResult">处理说明：{{ afterSale?.handleResult }}</p>
    </div>

    <div class="progress-section">
      <h3>处理进度</h3>
      <el-timeline>
        <el-timeline-item type="primary">
          <h4>提交申请</h4>
          <p>{{ formatDate(afterSale?.createTime) }}</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === AFTER_SALE_STATUS.PENDING_MERCHANT" type="warning">
          <h4>等待商家处理</h4>
          <p>商家将在审核后给出处理方案</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === AFTER_SALE_STATUS.WAIT_USER_RETURN" type="warning">
          <h4>商家已同意，请寄回商品</h4>
          <p>请填写退货快递单号，商家签收后完成退款</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND" type="warning">
          <h4>商家待签收退款</h4>
          <p>您已提交退货物流，等待商家确认收货并退款</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === AFTER_SALE_STATUS.ADMIN" type="warning">
          <h4>管理员介入中</h4>
          <p>平台正在协调处理，请耐心等待</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === AFTER_SALE_STATUS.RESOLVED" type="success">
          <h4>售后已完成</h4>
          <p>退款处理完成</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === AFTER_SALE_STATUS.REJECTED" type="danger">
          <h4>售后未通过</h4>
          <p>申请被驳回，可在沟通区与商家继续协商</p>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div v-if="showReturnShippingForm" class="return-section">
      <h3>上传退货快递单号</h3>
      <el-form :model="returnForm" label-position="top">
        <el-form-item label="物流公司">
          <el-select v-model="returnForm.logisticsCompany" placeholder="请选择退货快递公司" style="width: 100%">
            <el-option
              v-for="item in logisticsCompanies"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="returnForm.logisticsNo" placeholder="请输入退货快递单号" />
        </el-form-item>
      </el-form>
      <el-button type="primary" :loading="submittingReturn" @click="submitReturnLogistics">
        提交退货信息
      </el-button>
    </div>

    <div v-else-if="afterSale?.returnLogisticsNo" class="return-section readonly">
      <h3>退货物流信息</h3>
      <p>物流公司：{{ afterSale.returnLogisticsCompany || '-' }}</p>
      <p>物流单号：{{ afterSale.returnLogisticsNo }}</p>
      <p>退货时间：{{ formatDate(afterSale.returnShipTime) }}</p>
    </div>

    <div class="message-section">
      <div class="message-header">
        <h3>售后实时沟通</h3>
        <div class="header-actions">
          <el-tag v-if="afterSaleUnreadCount > 0" type="danger">未读 {{ afterSaleUnreadCount }}</el-tag>
          <el-button
            v-if="canEscalate"
            type="danger"
            plain
            size="small"
            @click="applyAdminIntervention"
          >
            申请管理员介入
          </el-button>
        </div>
      </div>
      <el-tabs v-model="messageTab" @tab-change="handleMessageTabChange">
        <el-tab-pane label="订单实时沟通" name="order">
          <OrderChatPanel
            v-if="afterSale?.orderNo"
            :order-no="afterSale.orderNo"
            role="user"
            title="售后实时沟通（用户/商家/管理员）"
          />
          <el-empty v-else description="暂无可沟通订单信息" :image-size="72" />
        </el-tab-pane>
        <el-tab-pane :label="afterSaleTabLabel" name="afterSale">
          <div class="after-sale-msg-panel">
            <el-scrollbar height="340px" v-loading="afterSaleMsgLoading">
              <div v-if="!afterSaleMessages.length" class="empty-msg">
                <el-empty description="暂无售后消息" :image-size="64" />
              </div>
              <div v-for="msg in afterSaleMessages" :key="msg.id" class="msg-item">
                <div class="msg-meta">
                  <span class="sender">{{ senderText(msg.senderType) }}</span>
                  <span class="time">{{ formatDate(msg.createTime) }}</span>
                </div>
                <div class="msg-content">{{ msg.content || '-' }}</div>
              </div>
            </el-scrollbar>
            <div class="msg-editor">
              <el-input
                v-model="afterSaleMsgForm.content"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="请输入售后沟通内容"
              />
              <div class="msg-actions">
                <el-button @click="loadAfterSaleMessages">刷新</el-button>
                <el-button
                  type="primary"
                  :loading="afterSaleSendPending"
                  :disabled="!afterSaleNo"
                  @click="submitAfterSaleMessage"
                >
                  发送消息
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  escalateAfterSale,
  getAfterSaleDetail,
  listAfterSaleMessages,
  sendAfterSaleMessage,
  submitAfterSaleReturnLogistics
} from '@/apis/user';
import { getAfterSaleUnreadCount, markAfterSaleMessagesRead } from '@/apis/after-sale-message';
import type { AfterSale, AfterSaleMessage } from '@/types';
import {
  AFTER_SALE_STATUS,
  canUserEscalate,
  getAfterSaleResponsibleLabel,
  getAfterSaleStatusTagType,
  getAfterSaleStatusText
} from '@/utils/afterSale';
import OrderChatPanel from '@/components/OrderChatPanel.vue';
import { USER_REALTIME_EVENT, parseRealtimePayload } from '@/utils/realtime';

const router = useRouter();
const route = useRoute();

const afterSaleNo = ref(route.params.afterSaleNo as string);
const afterSale = ref<AfterSale | null>(null);
const submittingReturn = ref(false);
const messageTab = ref<'order' | 'afterSale'>('afterSale');
const afterSaleMessages = ref<AfterSaleMessage[]>([]);
const afterSaleMsgLoading = ref(false);
const afterSaleSendPending = ref(false);
const afterSaleUnreadCount = ref(0);
const afterSaleMsgForm = reactive({
  content: ''
});
const returnForm = reactive({
  logisticsCompany: '顺丰速运',
  logisticsNo: ''
});
const logisticsCompanies = [
  '顺丰速运',
  '京东物流',
  '中通快递',
  '圆通速递',
  '申通快递',
  '韵达快递',
  '极兔速递',
  '中国邮政',
  '德邦快递',
  '其他快递'
];

const formatDate = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
};

const loadDetail = async () => {
  if (!afterSaleNo.value) return;
  const res = await getAfterSaleDetail(afterSaleNo.value);
  afterSale.value = res || null;
};

const senderText = (senderType?: number) => {
  if (senderType === 1) return '用户';
  if (senderType === 2) return '商家';
  if (senderType === 3) return '管理员';
  return '未知';
};

const afterSaleTabLabel = computed(() => {
  return afterSaleUnreadCount.value > 0
    ? `售后接口消息 (${afterSaleUnreadCount.value})`
    : '售后接口消息';
});

const loadAfterSaleMessages = async () => {
  if (!afterSaleNo.value) return;
  afterSaleMsgLoading.value = true;
  try {
    const res = await listAfterSaleMessages(afterSaleNo.value, { pageNum: 1, pageSize: 100 });
    const source = res?.list || [];
    afterSaleMessages.value = [...source].sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
  } finally {
    afterSaleMsgLoading.value = false;
  }
};

const loadAfterSaleUnreadCount = async () => {
  if (!afterSaleNo.value) return;
  try {
    const count = await getAfterSaleUnreadCount(afterSaleNo.value, 1);
    afterSaleUnreadCount.value = Number(count || 0);
  } catch (error) {
    console.warn('读取售后消息未读数失败', error);
    afterSaleUnreadCount.value = 0;
  }
};

const markAfterSaleRead = async () => {
  if (!afterSaleNo.value) return;
  try {
    await markAfterSaleMessagesRead(afterSaleNo.value, 1);
    afterSaleUnreadCount.value = 0;
  } catch (error) {
    console.warn('标记售后消息已读失败', error);
  }
};

const enterAfterSaleMessageTab = async () => {
  await Promise.all([loadAfterSaleMessages(), markAfterSaleRead()]);
};

const submitAfterSaleMessage = async () => {
  if (!afterSaleNo.value) return;
  const content = afterSaleMsgForm.content.trim();
  if (!content) {
    ElMessage.warning('请输入消息内容');
    return;
  }
  afterSaleSendPending.value = true;
  try {
    await sendAfterSaleMessage(afterSaleNo.value, { content });
    afterSaleMsgForm.content = '';
    await Promise.all([loadAfterSaleMessages(), loadAfterSaleUnreadCount()]);
    ElMessage.success('消息已发送');
  } finally {
    afterSaleSendPending.value = false;
  }
};

const handleMessageTabChange = (tab: string | number) => {
  if (String(tab) === 'afterSale') {
    void enterAfterSaleMessageTab();
  }
};

const canEscalate = computed(() => {
  return canUserEscalate(afterSale.value?.afterSaleStatus);
});

const showReturnShippingForm = computed(() => {
  return afterSale.value?.afterSaleType === 2
    && afterSale.value?.afterSaleStatus === AFTER_SALE_STATUS.WAIT_USER_RETURN;
});

const submitReturnLogistics = async () => {
  if (!afterSale.value?.id) return;
  if (!returnForm.logisticsNo.trim()) {
    ElMessage.warning('请填写退货物流单号');
    return;
  }
  submittingReturn.value = true;
  try {
    await submitAfterSaleReturnLogistics(afterSale.value.id, {
      logisticsNo: returnForm.logisticsNo.trim(),
      logisticsCompany: returnForm.logisticsCompany.trim() || undefined
    });
    ElMessage.success('退货物流已提交，等待商家签收退款');
    await loadDetail();
  } finally {
    submittingReturn.value = false;
  }
};

const applyAdminIntervention = async () => {
  if (!afterSale.value?.id) return;
  await escalateAfterSale(afterSale.value.id);
  ElMessage.success('已申请管理员介入，请耐心等待处理');
  await loadDetail();
};

const afterSaleTypeText = computed(() => {
  const t = afterSale.value?.afterSaleType;
  if (t === 1) return '仅退款';
  if (t === 2) return '退货退款';
  if (t === 3) return '换货';
  return '-';
});

const statusTitle = computed(() => {
  const s = afterSale.value?.afterSaleStatus;
  if (s === AFTER_SALE_STATUS.PENDING_MERCHANT) return '商家处理中';
  if (s === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND) return '待商家签收退款';
  if (s === AFTER_SALE_STATUS.RESOLVED) return '售后已完成';
  if (s === AFTER_SALE_STATUS.ADMIN) return '管理员介入中';
  if (s === AFTER_SALE_STATUS.REJECTED) return '售后未通过';
  if (s === AFTER_SALE_STATUS.WAIT_USER_RETURN) return '待您退货';
  return '加载中...';
});

const statusDesc = computed(() => {
  const s = afterSale.value?.afterSaleStatus;
  if (s === AFTER_SALE_STATUS.PENDING_MERCHANT) return '商家正在处理您的申请';
  if (s === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND) return '已提交退货信息，等待商家确认收货并退款';
  if (s === AFTER_SALE_STATUS.RESOLVED) return '售后流程已完成';
  if (s === AFTER_SALE_STATUS.ADMIN) return '平台正在介入协调';
  if (s === AFTER_SALE_STATUS.REJECTED) return '申请未通过，可查看处理说明';
  if (s === AFTER_SALE_STATUS.WAIT_USER_RETURN) return '商家已同意，请上传退货快递单号';
  return '请稍后';
});

const statusColor = computed(() => {
  const s = afterSale.value?.afterSaleStatus;
  if (s === AFTER_SALE_STATUS.REJECTED) return '#f56c6c';
  if (s === AFTER_SALE_STATUS.RESOLVED) return '#67c23a';
  return '#e6a23c';
});

const currentStatusText = computed(() => getAfterSaleStatusText(afterSale.value?.afterSaleStatus));
const statusTagType = computed(() => getAfterSaleStatusTagType(afterSale.value?.afterSaleStatus));
const responsibleLabel = computed(() => getAfterSaleResponsibleLabel(afterSale.value?.afterSaleStatus));

const handleRealtimeRefresh = (event: Event) => {
  const payload = parseRealtimePayload(event);
  const isCurrentAfterSale = payload.refNo && payload.refNo === afterSaleNo.value;
  const isAfterSaleEvent = String(payload.reason || '').startsWith('AFTER_SALE');
  if (!isCurrentAfterSale && !isAfterSaleEvent) return;
  void loadDetail();
  void loadAfterSaleUnreadCount();
  if (messageTab.value === 'afterSale') {
    void enterAfterSaleMessageTab();
  }
};

onMounted(async () => {
  try {
    await loadDetail();
    await loadAfterSaleUnreadCount();
    if (messageTab.value === 'afterSale') {
      await enterAfterSaleMessageTab();
    }
  } catch (e) {
    console.error(e);
  }
  window.addEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
});

onUnmounted(() => {
  window.removeEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
});
</script>

<style scoped lang="scss">
.after-sale-detail-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;

  .back-btn {
    margin-bottom: 16px;
    font-size: 16px;
  }
}

.page-title {
  font-size: 24px;
  margin: 0 0 24px;
}

.status-section {
  text-align: center;
  padding: 32px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 24px;

  h2 {
    margin: 16px 0 8px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #909399;
  }

  .status-tag {
    margin-top: 14px;
  }

  .responsible-tip {
    margin-top: 10px;
    color: #5f6b7a;
    font-size: 14px;
  }
}

.info-section {
  margin-bottom: 24px;

  h3 {
    font-size: 16px;
    margin: 0 0 16px;
  }

  p {
    margin: 0 0 8px;
    color: #606266;
  }
}

.progress-section {
  h3 {
    font-size: 16px;
    margin: 0 0 16px;
  }
}

.return-section {
  margin-top: 20px;
  background: #f8fbff;
  border: 1px solid #e4efff;
  border-radius: 10px;
  padding: 16px;

  h3 {
    margin: 0 0 12px;
    font-size: 16px;
  }

  &.readonly p {
    margin: 0 0 6px;
    color: #5f6b7a;
  }
}

.message-section {
  margin-top: 24px;

  .message-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    h3 {
      margin: 0;
      font-size: 16px;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .after-sale-msg-panel {
    .empty-msg {
      padding-top: 70px;
    }

    .msg-item {
      padding: 10px 12px;
      border: 1px solid #eef2f8;
      border-radius: 8px;
      margin-bottom: 10px;

      .msg-meta {
        display: flex;
        justify-content: space-between;
        margin-bottom: 6px;
        font-size: 12px;
        color: #94a3b8;

        .sender {
          color: #2563eb;
          font-weight: 600;
        }
      }

      .msg-content {
        color: #334155;
        line-height: 1.6;
        white-space: pre-wrap;
      }
    }

    .msg-editor {
      margin-top: 12px;

      .msg-actions {
        margin-top: 10px;
        display: flex;
        justify-content: flex-end;
        gap: 8px;
      }
    }
  }
}

@media (max-width: 768px) {
  .after-sale-detail-page {
    padding: 16px;
  }
}
</style>
