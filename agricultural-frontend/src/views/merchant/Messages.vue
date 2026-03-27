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
import { AFTER_SALE_STATUS } from '@/utils/afterSale';
import {
  buildAfterSaleMessageMeta,
  markAllMerchantMessagesRead,
  markMerchantMessageRead,
  mergeMerchantMessages,
  type MerchantMessageItem,
  type MerchantMessageType
} from '@/utils/merchantMessageCenter';

const userStore = useUserStore();
const activeTab = ref<'all' | MerchantMessageType>('all');
const showDetailDialog = ref(false);
const currentMessage = reactive<Partial<MerchantMessageItem>>({});
const messageList = ref<MerchantMessageItem[]>([]);
const merchantId = computed(() => userStore.userInfo?.id || 'anonymous');

const unreadCount = computed(() => messageList.value.filter(msg => !msg.isRead).length);
const filteredMessageList = computed(() => {
  if (activeTab.value === 'all') return messageList.value;
  return messageList.value.filter(msg => msg.type === activeTab.value);
});

const getIcon = (type: MerchantMessageType) => {
  const map: Record<MerchantMessageType, any> = {
    order: Box,
    aftersale: Warning
  };
  return map[type] || Bell;
};

const getIconColor = (type: MerchantMessageType) => {
  const map: Record<MerchantMessageType, string> = {
    order: '#67c23a',
    aftersale: '#e6a23c'
  };
  return map[type] || '#909399';
};

const formatDate = (date?: string) => {
  if (!date) return '-';
  return String(date).replace('T', ' ').slice(0, 19);
};

const loadMessages = async () => {
  try {
    const liveMessages: MerchantMessageItem[] = [];
    const [orderRes, pendingAfterSaleRes, processingAfterSaleRes, waitUserReturnRes, adminRes] = await Promise.all([
      listOrders({ pageNum: 1, pageSize: 50, orderStatus: 2 }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.PENDING_MERCHANT }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_USER_RETURN }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.ADMIN })
    ]);

    (orderRes?.list || []).forEach((order: any) => {
      liveMessages.push({
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
      liveMessages.push({
        id: `aftersale-${item.id}`,
        title: meta.title,
        content: `${meta.prefix} 售后单号 ${item.afterSaleNo}，申请原因：${item.applyReason || '无'}`,
        time: item.createTime,
        type: 'aftersale',
        isRead: false,
        link: '/merchant/after-sales'
      });
    });

    messageList.value = mergeMerchantMessages(merchantId.value, liveMessages);
  } catch (error) {
    console.error('Failed to load messages', error);
  }
};

const readMessage = (msg: MerchantMessageItem) => {
  Object.assign(currentMessage, msg);
  if (!msg.isRead) {
    messageList.value = markMerchantMessageRead(merchantId.value, msg.id, messageList.value);
    window.dispatchEvent(new Event('merchant-message-read-refresh'));
  }
  showDetailDialog.value = true;
};

const markAllRead = () => {
  messageList.value = markAllMerchantMessagesRead(merchantId.value, messageList.value);
  window.dispatchEvent(new Event('merchant-message-read-refresh'));
  ElMessage.success('已全部标记为已读');
};

const handleRefresh = () => {
  void loadMessages();
};

onMounted(() => {
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
