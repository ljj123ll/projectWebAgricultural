<template>
  <div class="messages-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">我的消息</h1>
        <p class="page-subtitle">系统通知、商家消息和管理员消息都会在这里展示</p>
      </div>
      <el-button :disabled="unreadCount === 0" type="primary" plain @click="handleReadAll">
        全部已读
      </el-button>
    </div>

    <div class="tab-bar">
      <el-radio-group v-model="activeTab" @change="onTabChange">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="system">系统</el-radio-button>
        <el-radio-button label="merchant">商家</el-radio-button>
        <el-radio-button label="admin">管理员</el-radio-button>
        <el-radio-button label="unread">未读</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="message-list">
      <div
        v-for="msg in messageList"
        :key="msg.id"
        class="message-card"
        :class="{ unread: msg.isRead === 0 }"
      >
        <div class="card-header">
          <div class="left">
            <el-tag size="small" :type="getTypeTag(msg.senderType)">{{ getTypeText(msg.senderType) }}</el-tag>
            <strong class="title">{{ msg.title || '消息通知' }}</strong>
          </div>
          <span class="time">{{ formatDate(msg.createTime) }}</span>
        </div>

        <p class="content">{{ msg.content }}</p>

        <div class="card-actions">
          <el-button v-if="msg.isRead === 0" link type="primary" @click="markRead(msg.id)">标记已读</el-button>
          <el-button v-if="canJump(msg)" link @click="jumpByRef(msg)">查看详情</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && messageList.length === 0" description="暂无消息" />

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadMessages"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { UserMessage } from '@/types';
import {
  getUserMessageUnreadCount,
  listUserMessages,
  markAllUserMessagesRead,
  markUserMessageRead
} from '@/apis/user';
import { USER_REALTIME_EVENT } from '@/utils/realtime';

type TabType = 'all' | 'system' | 'merchant' | 'admin' | 'unread';

const router = useRouter();
const loading = ref(false);
const activeTab = ref<TabType>('all');
const messageList = ref<UserMessage[]>([]);
const unreadCount = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const queryParams = computed(() => {
  const params: Record<string, any> = {
    pageNum: pageNum.value,
    pageSize: pageSize.value
  };
  if (activeTab.value === 'system') params.senderType = 1;
  if (activeTab.value === 'merchant') params.senderType = 2;
  if (activeTab.value === 'admin') params.senderType = 3;
  if (activeTab.value === 'unread') params.isRead = 0;
  return params;
});

const loadUnreadCount = async () => {
  unreadCount.value = Number(await getUserMessageUnreadCount()) || 0;
};

const loadMessages = async () => {
  loading.value = true;
  try {
    const res = await listUserMessages(queryParams.value);
    messageList.value = res?.list || [];
    total.value = Number(res?.total || 0);
    await loadUnreadCount();
  } finally {
    loading.value = false;
  }
};

const onTabChange = () => {
  pageNum.value = 1;
  void loadMessages();
};

const getTypeTag = (type: number) => {
  if (type === 1) return 'info';
  if (type === 2) return 'success';
  if (type === 3) return 'warning';
  return 'info';
};

const getTypeText = (type: number) => {
  if (type === 1) return '系统';
  if (type === 2) return '商家';
  if (type === 3) return '管理员';
  return '通知';
};

const formatDate = (value?: string) => {
  if (!value) return '';
  return value.substring(0, 16).replace('T', ' ');
};

const markRead = async (id: number) => {
  await markUserMessageRead(id);
  ElMessage.success('已标记为已读');
  await loadMessages();
};

const handleReadAll = async () => {
  await markAllUserMessagesRead();
  ElMessage.success('全部消息已读');
  await loadMessages();
};

const canJump = (msg: UserMessage) => {
  return Boolean(msg.refType && msg.refNo);
};

const jumpByRef = async (msg: UserMessage) => {
  if (msg.isRead === 0) {
    await markUserMessageRead(msg.id);
  }
  const refType = String(msg.refType || '');
  const refNo = String(msg.refNo || '');
  if (refType === 'after_sale' && refNo) {
    router.push(`/after-sale-detail/${refNo}`);
    return;
  }
  if (refType === 'order') {
    router.push('/orders');
    return;
  }
  if (refType === 'product' && Number(refNo) > 0) {
    router.push(`/product/${refNo}?tab=reviews`);
    return;
  }
  if (refType === 'comment') {
    router.push('/reviews');
    return;
  }
  router.push('/orders');
};

const handleRealtimeRefresh = () => {
  void loadMessages();
};

onMounted(() => {
  void loadMessages();
  window.addEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
});

onUnmounted(() => {
  window.removeEventListener(USER_REALTIME_EVENT, handleRealtimeRefresh);
});
</script>

<style scoped lang="scss">
.messages-page {
  background: #fff;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  padding: 22px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #1f2a37;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #7b8794;
  font-size: 13px;
}

.tab-bar {
  margin: 18px 0;
}

.message-list {
  min-height: 180px;
}

.message-card {
  border: 1px solid #e8eef6;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fff;

  &.unread {
    border-color: #b7d8ff;
    background: linear-gradient(180deg, #f8fcff 0%, #f2f8ff 100%);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;

  .left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .title {
    color: #283548;
    font-size: 15px;
  }

  .time {
    color: #8a97a8;
    font-size: 12px;
    white-space: nowrap;
  }
}

.content {
  margin: 10px 0 8px;
  color: #495568;
  line-height: 1.7;
  white-space: pre-wrap;
}

.card-actions {
  display: flex;
  gap: 14px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .messages-page {
    padding: 14px;
  }

  .page-title {
    font-size: 22px;
  }
}
</style>
