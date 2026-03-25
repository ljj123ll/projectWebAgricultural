<template>
  <div class="messages-page">
    <div class="page-header">
      <h2>消息中心</h2>
      <el-button v-if="unreadCount > 0" link type="primary" @click="markAllRead">全部已读</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="全部消息" name="all" />
      <el-tab-pane label="订单消息" name="order" />
      <el-tab-pane label="售后消息" name="aftersale" />
    </el-tabs>

    <div class="message-list" v-if="filteredMessageList.length > 0">
      <div
        v-for="msg in filteredMessageList"
        :key="msg.id"
        class="message-item"
        :class="{ unread: !msg.isRead }"
        @click="readMessage(msg)"
      >
        <div class="message-icon">
          <el-icon :size="24" :color="getIconColor(msg.type)">
            <component :is="getIcon(msg.type)" />
          </el-icon>
        </div>
        <div class="message-content">
          <div class="message-header">
            <h4>{{ msg.title }}</h4>
            <span class="message-time">{{ formatDate(msg.time) }}</span>
          </div>
          <p class="message-text">{{ msg.content }}</p>
        </div>
        <div v-if="!msg.isRead" class="unread-dot"></div>
      </div>
    </div>

    <el-empty v-else description="暂无消息" />

    <el-dialog v-model="showDetailDialog" title="消息详情" width="560px">
      <div class="message-detail">
        <h3>{{ currentMessage.title }}</h3>
        <p class="detail-time">{{ formatDate(currentMessage.time) }}</p>
        <div class="detail-content">{{ currentMessage.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { Bell, Box, Warning } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { listAfterSale, listOrders } from '@/apis/merchant';
import { useUserStore } from '@/stores/modules/user';
import { AFTER_SALE_STATUS, getAfterSaleStatusText, getAfterSaleResponsibleLabel } from '@/utils/afterSale';

type MessageType = 'order' | 'aftersale';

interface MerchantMessageItem {
  id: string;
  title: string;
  content: string;
  time: string;
  type: MessageType;
  isRead: boolean;
  link: string;
}

const READ_STORAGE_PREFIX = 'merchant_message_read_map';

const userStore = useUserStore();
const activeTab = ref<'all' | MessageType>('all');
const showDetailDialog = ref(false);
const currentMessage = reactive<Partial<MerchantMessageItem>>({});
const messageList = ref<MerchantMessageItem[]>([]);
const readMap = ref<Record<string, boolean>>({});

const unreadCount = computed(() => messageList.value.filter(msg => !msg.isRead).length);
const filteredMessageList = computed(() => {
  if (activeTab.value === 'all') return messageList.value;
  return messageList.value.filter(msg => msg.type === activeTab.value);
});

const getReadMapKey = () => {
  const merchantId = userStore.userInfo?.id || 'anonymous';
  return `${READ_STORAGE_PREFIX}_${merchantId}`;
};

const loadReadMap = () => {
  try {
    const raw = localStorage.getItem(getReadMapKey());
    readMap.value = raw ? JSON.parse(raw) : {};
  } catch (error) {
    console.warn('load read map failed', error);
    readMap.value = {};
  }
};

const saveReadMap = () => {
  localStorage.setItem(getReadMapKey(), JSON.stringify(readMap.value));
};

const getIcon = (type: MessageType) => {
  const map: Record<MessageType, any> = {
    order: Box,
    aftersale: Warning
  };
  return map[type] || Bell;
};

const getIconColor = (type: MessageType) => {
  const map: Record<MessageType, string> = {
    order: '#67c23a',
    aftersale: '#e6a23c'
  };
  return map[type] || '#909399';
};

const formatDate = (date?: string) => {
  if (!date) return '-';
  return String(date).replace('T', ' ').slice(0, 19);
};

const syncReadStatus = (messages: MerchantMessageItem[]) => {
  messages.forEach(msg => {
    msg.isRead = !!readMap.value[msg.id];
  });
  return messages;
};

const buildAfterSaleMessageMeta = (status?: number) => {
  if (status === AFTER_SALE_STATUS.PENDING_MERCHANT) {
    return {
      title: '售后待处理',
      prefix: '该售后等待商家处理，请尽快审核。'
    };
  }
  if (status === AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND) {
    return {
      title: '退货待签收退款',
      prefix: '用户已回传退货物流，请确认签收并退款。'
    };
  }
  if (status === AFTER_SALE_STATUS.WAIT_USER_RETURN) {
    return {
      title: '等待用户退货',
      prefix: '商家已同意退货退款，当前等待用户回寄商品。'
    };
  }
  if (status === AFTER_SALE_STATUS.ADMIN) {
    return {
      title: '管理员介入处理中',
      prefix: '该售后已进入平台裁决阶段。'
    };
  }
  return {
    title: `售后状态：${getAfterSaleStatusText(status)}`,
    prefix: getAfterSaleResponsibleLabel(status)
  };
};

const loadMessages = async () => {
  try {
    const messages: MerchantMessageItem[] = [];
    const [orderRes, pendingAfterSaleRes, processingAfterSaleRes, waitUserReturnRes, adminRes] = await Promise.all([
      listOrders({ pageNum: 1, pageSize: 50, orderStatus: 2 }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.PENDING_MERCHANT }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_USER_RETURN }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.ADMIN })
    ]);

    (orderRes?.list || []).forEach((order: any) => {
      messages.push({
        id: `order-${order.id}`,
        title: '新订单待发货',
        content: `订单号 ${order.orderNo} 待发货，请及时处理`,
        time: order.createTime,
        type: 'order',
        isRead: false,
        link: '/merchant/orders'
      });
    });

    [
      ...(pendingAfterSaleRes?.list || []),
      ...(processingAfterSaleRes?.list || []),
      ...(waitUserReturnRes?.list || []),
      ...(adminRes?.list || [])
    ].forEach((item: any) => {
      const meta = buildAfterSaleMessageMeta(Number(item?.afterSaleStatus));
      messages.push({
        id: `aftersale-${item.id}`,
        title: meta.title,
        content: `${meta.prefix} 售后单号 ${item.afterSaleNo}，申请原因：${item.applyReason || '无'}`,
        time: item.createTime,
        type: 'aftersale',
        isRead: false,
        link: '/merchant/after-sales'
      });
    });

    const sorted = messages.sort((a, b) => new Date(b.time || 0).getTime() - new Date(a.time || 0).getTime());
    messageList.value = syncReadStatus(sorted);
  } catch (error) {
    console.error('Failed to load messages', error);
  }
};

const readMessage = (msg: MerchantMessageItem) => {
  Object.assign(currentMessage, msg);
  if (!msg.isRead) {
    msg.isRead = true;
    readMap.value[msg.id] = true;
    saveReadMap();
    window.dispatchEvent(new Event('merchant-message-read-refresh'));
  }
  showDetailDialog.value = true;
};

const markAllRead = () => {
  messageList.value.forEach(msg => {
    msg.isRead = true;
    readMap.value[msg.id] = true;
  });
  saveReadMap();
  window.dispatchEvent(new Event('merchant-message-read-refresh'));
  ElMessage.success('已全部标记为已读');
};

const handleRefresh = () => {
  void loadMessages();
};

onMounted(() => {
  loadReadMap();
  void loadMessages();
  window.addEventListener('merchant-pending-refresh', handleRefresh);
});

onUnmounted(() => {
  window.removeEventListener('merchant-pending-refresh', handleRefresh);
});
</script>

<style scoped lang="scss">
.messages-page {
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
}

.message-list {
  margin-top: 20px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background 0.3s;
  position: relative;

  &:hover {
    background: #f5f5f5;
  }

  &.unread {
    background: #f0f9ff;

    &:hover {
      background: #e6f2ff;
    }
  }

  .message-icon {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    flex-shrink: 0;
  }

  .message-content {
    flex: 1;
    min-width: 0;

    .message-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      h4 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
      }

      .message-time {
        color: #999;
        font-size: 12px;
      }
    }

    .message-text {
      color: #666;
      font-size: 14px;
      margin: 0;
      line-height: 1.5;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
  }

  .unread-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f56c6c;
    margin-left: 8px;
    flex-shrink: 0;
    margin-top: 8px;
  }
}

.message-detail {
  h3 {
    margin: 0 0 12px 0;
    font-size: 18px;
  }

  .detail-time {
    color: #999;
    font-size: 14px;
    margin-bottom: 20px;
  }

  .detail-content {
    color: #333;
    font-size: 15px;
    line-height: 1.8;
  }
}
</style>
