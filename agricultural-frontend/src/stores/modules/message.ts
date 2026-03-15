import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface Message {
  id: number;
  title: string;
  content: string;
  type: 'order' | 'after_sale' | 'system' | 'inventory';
  isRead: boolean;
  createTime: string;
}

export const useMessageStore = defineStore('message', () => {
  // 消息列表
  const messageList = ref<Message[]>([]);

  // 未读消息数量
  const unreadCount = computed(() => {
    return messageList.value.filter(msg => !msg.isRead).length;
  });

  // 未读订单消息数
  const unreadOrderCount = computed(() => {
    return messageList.value.filter(msg => !msg.isRead && msg.type === 'order').length;
  });

  // 未读售后消息数
  const unreadAfterSaleCount = computed(() => {
    return messageList.value.filter(msg => !msg.isRead && msg.type === 'after_sale').length;
  });

  // 添加消息
  function addMessage(message: Message) {
    messageList.value.unshift(message);
  }

  // 标记消息为已读
  function markAsRead(messageId: number) {
    const message = messageList.value.find(m => m.id === messageId);
    if (message) {
      message.isRead = true;
    }
  }

  // 标记所有消息为已读
  function markAllAsRead() {
    messageList.value.forEach(msg => {
      msg.isRead = true;
    });
  }

  // 删除消息
  function removeMessage(messageId: number) {
    const index = messageList.value.findIndex(m => m.id === messageId);
    if (index > -1) {
      messageList.value.splice(index, 1);
    }
  }

  // 清空消息
  function clearMessages() {
    messageList.value = [];
  }

  // 获取指定类型的消息
  function getMessagesByType(type: Message['type']) {
    return messageList.value.filter(msg => msg.type === type);
  }

  return {
    messageList,
    unreadCount,
    unreadOrderCount,
    unreadAfterSaleCount,
    addMessage,
    markAsRead,
    markAllAsRead,
    removeMessage,
    clearMessages,
    getMessagesByType
  };
}, {
  persist: true
});
