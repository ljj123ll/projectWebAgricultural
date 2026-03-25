<template>
  <el-card class="order-chat-card" shadow="never">
    <template #header>
      <div class="chat-header">
        <span>{{ title }}</span>
        <el-tag :type="connected ? 'success' : 'info'" size="small">
          {{ connected ? '实时连接中' : '连接中...' }}
        </el-tag>
      </div>
    </template>

    <div ref="listRef" class="chat-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="chat-item"
        :class="{ self: isSelf(msg.senderType) }"
      >
        <div class="sender">{{ senderLabel(msg.senderType) }}</div>
        <div class="bubble">
          <template v-if="resolveMessageType(msg) === 2">
            <el-image
              class="media-image"
              :src="mediaFullUrl(msg.mediaUrl)"
              :preview-src-list="[mediaFullUrl(msg.mediaUrl)]"
              preview-teleported
              fit="cover"
            />
          </template>
          <template v-else-if="resolveMessageType(msg) === 3">
            <video
              class="media-video"
              :src="mediaFullUrl(msg.mediaUrl)"
              controls
              preload="metadata"
            />
          </template>
          <p v-if="msg.content" class="text-content">{{ msg.content }}</p>
        </div>
        <div class="time">{{ formatTime(msg.createTime) }}</div>
      </div>
      <el-empty v-if="messages.length === 0" description="暂无沟通消息" :image-size="72" />
    </div>

    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        maxlength="500"
        show-word-limit
        resize="none"
        placeholder="请输入消息，可发送文本、图片、视频"
        @keyup.enter.exact.prevent="sendTextMessage"
      />
      <div class="actions">
        <el-upload
          :action="uploadAction"
          :show-file-list="false"
          :headers="uploadHeaders"
          accept="image/*"
          :before-upload="beforeImageUpload"
          :on-success="onImageUploadSuccess"
          :on-error="onUploadError"
        >
          <el-button :disabled="!connected" plain>上传图片</el-button>
        </el-upload>
        <el-upload
          :action="uploadAction"
          :show-file-list="false"
          :headers="uploadHeaders"
          accept="video/*"
          :before-upload="beforeVideoUpload"
          :on-success="onVideoUploadSuccess"
          :on-error="onUploadError"
        >
          <el-button :disabled="!connected" plain>上传视频</el-button>
        </el-upload>
        <el-button type="primary" :disabled="!connected" @click="sendTextMessage">发送文字</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import type { UploadProps } from 'element-plus';
import { useUserStore } from '@/stores/modules/user';
import type { OrderChatMessage } from '@/types';
import { listAdminOrderMessages, listMerchantOrderMessages, listUserOrderMessages } from '@/apis/order-chat';
import { getFullImageUrl } from '@/utils/image';

const props = defineProps<{
  orderNo: string;
  role?: 'user' | 'merchant' | 'admin';
  title?: string;
}>();

const userStore = useUserStore();

const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/common/upload`;
const uploadHeaders = computed(() => {
  const headers: Record<string, string> = {};
  if (userStore.token) {
    headers.Authorization = `Bearer ${userStore.token}`;
  }
  return headers;
});

const inputText = ref('');
const messages = ref<OrderChatMessage[]>([]);
const connected = ref(false);
const listRef = ref<HTMLElement | null>(null);

let socket: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let manualClosed = false;

const role = computed(() => props.role || (userStore.role as 'user' | 'merchant' | 'admin'));
const title = computed(() => props.title || '订单实时沟通');

const senderLabel = (senderType: number) => {
  if (senderType === 1) return '用户';
  if (senderType === 2) return '商家';
  if (senderType === 3) return '管理员';
  return '未知';
};

const selfSenderType = computed(() => {
  if (role.value === 'user') return 1;
  if (role.value === 'merchant') return 2;
  if (role.value === 'admin') return 3;
  return 0;
});

const isSelf = (senderType: number) => senderType === selfSenderType.value;

const formatTime = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
};

const resolveMessageType = (msg: OrderChatMessage) => {
  const mt = Number(msg.messageType || 0);
  if (mt === 2 || mt === 3) return mt;
  const url = String(msg.mediaUrl || '');
  if (!url) return 1;
  if (/\.(mp4|mov|avi|wmv|mkv|webm|mpeg|mpg)$/i.test(url)) return 3;
  return 2;
};

const mediaFullUrl = (path?: string) => getFullImageUrl(path || '');

const dedupeAndSort = (list: OrderChatMessage[]) => {
  const map = new Map<number, OrderChatMessage>();
  list.forEach((item) => {
    if (item && typeof item.id === 'number') {
      map.set(item.id, item);
    }
  });
  return [...map.values()].sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
};

const scrollToBottom = () => {
  const el = listRef.value;
  if (!el) return;
  el.scrollTop = el.scrollHeight;
};

const loadHistory = async () => {
  if (!props.orderNo) return;
  const params = { pageNum: 1, pageSize: 200 };
  let res: any = null;
  if (role.value === 'user') {
    res = await listUserOrderMessages(props.orderNo, params);
  } else if (role.value === 'merchant') {
    res = await listMerchantOrderMessages(props.orderNo, params);
  } else if (role.value === 'admin') {
    res = await listAdminOrderMessages(props.orderNo, params);
  }
  messages.value = dedupeAndSort((res?.list || []) as OrderChatMessage[]);
  await nextTick();
  scrollToBottom();
};

const buildWsUrl = () => {
  const token = encodeURIComponent(userStore.token || '');
  const orderNo = encodeURIComponent(props.orderNo || '');
  const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');

  if (apiBase.startsWith('http://') || apiBase.startsWith('https://')) {
    const wsBase = apiBase.replace(/^http/, 'ws');
    return `${wsBase}/ws/order-chat?token=${token}&orderNo=${orderNo}`;
  }
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${wsProtocol}//${window.location.host}${apiBase}/ws/order-chat?token=${token}&orderNo=${orderNo}`;
};

const clearReconnectTimer = () => {
  if (!reconnectTimer) return;
  clearTimeout(reconnectTimer);
  reconnectTimer = null;
};

const closeSocket = () => {
  clearReconnectTimer();
  connected.value = false;
  if (socket) {
    socket.close();
    socket = null;
  }
};

const scheduleReconnect = () => {
  clearReconnectTimer();
  reconnectTimer = setTimeout(() => {
    if (manualClosed) return;
    initSocket();
  }, 2000);
};

const initSocket = () => {
  closeSocket();
  if (!userStore.token || !props.orderNo) return;
  const ws = new WebSocket(buildWsUrl());
  socket = ws;

  ws.onopen = () => {
    connected.value = true;
  };

  ws.onmessage = async (event) => {
    try {
      const payload = JSON.parse(event.data || '{}');
      if (payload?.type === 'connected') {
        connected.value = true;
        return;
      }
      if (payload?.type === 'error') {
        ElMessage.warning(payload?.message || '消息发送失败');
        return;
      }
      if (payload?.type === 'message' && payload?.data) {
        messages.value = dedupeAndSort([...messages.value, payload.data as OrderChatMessage]);
        await nextTick();
        scrollToBottom();
      }
    } catch (error) {
      console.warn('order chat message parse error', error);
    }
  };

  ws.onclose = () => {
    connected.value = false;
    if (!manualClosed) {
      scheduleReconnect();
    }
  };

  ws.onerror = () => {
    connected.value = false;
  };
};

const sendPayload = (payload: Record<string, unknown>) => {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    ElMessage.warning('实时通道正在连接，请稍后重试');
    return false;
  }
  socket.send(JSON.stringify(payload));
  return true;
};

const sendTextMessage = () => {
  const content = inputText.value.trim();
  if (!content) {
    ElMessage.warning('请输入文字消息');
    return;
  }
  const ok = sendPayload({
    messageType: 1,
    content
  });
  if (ok) {
    inputText.value = '';
  }
};

const sendMediaMessage = (messageType: 2 | 3, mediaUrl: string, mediaName?: string) => {
  const payload: Record<string, unknown> = {
    messageType,
    mediaUrl,
    mediaName: mediaName || ''
  };
  const caption = inputText.value.trim();
  if (caption) {
    payload.content = caption;
  }
  const ok = sendPayload(payload);
  if (ok) {
    inputText.value = '';
  }
};

const isImageFile = (file: File) => {
  if (file.type.startsWith('image/')) return true;
  return /\.(png|jpg|jpeg|gif|webp|bmp)$/i.test(file.name || '');
};

const isVideoFile = (file: File) => {
  if (file.type.startsWith('video/')) return true;
  return /\.(mp4|mov|avi|wmv|mkv|webm|mpeg|mpg)$/i.test(file.name || '');
};

const beforeImageUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const file = rawFile as File;
  if (!connected.value) {
    ElMessage.warning('实时连接未就绪，请稍后上传');
    return false;
  }
  if (!isImageFile(file)) {
    ElMessage.warning('请选择图片文件');
    return false;
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB');
    return false;
  }
  return true;
};

const beforeVideoUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const file = rawFile as File;
  if (!connected.value) {
    ElMessage.warning('实时连接未就绪，请稍后上传');
    return false;
  }
  if (!isVideoFile(file)) {
    ElMessage.warning('请选择视频文件');
    return false;
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('视频大小不能超过50MB');
    return false;
  }
  return true;
};

const extractUploadUrl = (response: any) => {
  if (response && response.code === 200 && response.data) {
    return String(response.data);
  }
  return '';
};

const onImageUploadSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
  const url = extractUploadUrl(response);
  if (!url) {
    ElMessage.error(response?.msg || '图片上传失败');
    return;
  }
  sendMediaMessage(2, url, uploadFile?.name || '');
};

const onVideoUploadSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
  const url = extractUploadUrl(response);
  if (!url) {
    ElMessage.error(response?.msg || '视频上传失败');
    return;
  }
  sendMediaMessage(3, url, uploadFile?.name || '');
};

const onUploadError: UploadProps['onError'] = () => {
  ElMessage.error('文件上传失败，请稍后再试');
};

onMounted(async () => {
  manualClosed = false;
  await loadHistory();
  initSocket();
});

onUnmounted(() => {
  manualClosed = true;
  closeSocket();
});
</script>

<style scoped lang="scss">
.order-chat-card {
  border-radius: 12px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #344054;
}

.chat-list {
  max-height: 320px;
  overflow-y: auto;
  background: #f7f9fc;
  border: 1px solid #e9eef5;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 12px;
}

.chat-item {
  margin-bottom: 10px;
  max-width: 88%;

  .sender {
    font-size: 12px;
    color: #6b7280;
    margin-bottom: 4px;
  }

  .bubble {
    background: #ffffff;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 8px 10px;
    color: #1f2937;
    line-height: 1.5;
    word-break: break-word;
  }

  .time {
    margin-top: 3px;
    font-size: 12px;
    color: #9ca3af;
  }
}

.chat-item.self {
  margin-left: auto;
  text-align: right;

  .bubble {
    background: #ecfdf3;
    border-color: #ccefdc;
  }
}

.text-content {
  margin: 8px 0 0;
}

.media-image {
  width: 180px;
  max-width: 100%;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dbe5f3;
}

.media-video {
  width: 280px;
  max-width: 100%;
  border-radius: 8px;
  background: #000;
}

.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;

  :deep(.el-textarea) {
    flex: 1;
  }
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
</style>

