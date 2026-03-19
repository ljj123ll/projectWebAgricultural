<template>
  <div class="messages-page">
    <div class="page-header">
      <h2>消息中心</h2>
      <el-button v-if="unreadCount > 0" link type="primary" @click="markAllRead">全部已读</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部消息" name="all">
        <el-badge :value="unreadCount" v-if="unreadCount > 0" />
      </el-tab-pane>
      <el-tab-pane label="系统通知" name="system" />
      <el-tab-pane label="订单消息" name="order" />
      <el-tab-pane label="售后消息" name="aftersale" />
    </el-tabs>

    <div class="message-list" v-if="messageList.length > 0">
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
            <span class="message-time">{{ msg.time }}</span>
          </div>
          <p class="message-text">{{ msg.content }}</p>
        </div>
        <div v-if="!msg.isRead" class="unread-dot"></div>
      </div>
    </div>

    <el-empty v-else description="暂无消息" />

    <!-- 消息详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="消息详情" width="90%">
      <div class="message-detail">
        <h3>{{ currentMessage.title }}</h3>
        <p class="detail-time">{{ currentMessage.time }}</p>
        <div class="detail-content">{{ currentMessage.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Bell, Box, Warning, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listOrders } from '@/apis/merchant'
import { listAfterSale } from '@/apis/merchant'

const activeTab = ref('all')
const showDetailDialog = ref(false)
const currentMessage = reactive<any>({})

const messageList = ref<any[]>([])

const unreadCount = computed(() => {
  return messageList.value.filter(msg => !msg.isRead).length;
})

const filteredMessageList = computed(() => {
  if (activeTab.value === 'all') return messageList.value;
  return messageList.value.filter(msg => msg.type === activeTab.value);
})

// 从订单和售后数据生成消息
const loadMessages = async () => {
  try {
    const messages: any[] = []
    
    // 获取待发货订单作为消息
    const orderRes = await listOrders({ pageNum: 1, pageSize: 10, orderStatus: 2 })
    if (orderRes?.list) {
      orderRes.list.forEach((order: any) => {
        messages.push({
          id: `order-${order.id}`,
          title: '新订单待发货',
          content: `订单号 ${order.orderNo} 待发货，请及时处理`,
          time: order.createTime,
          type: 'order',
          isRead: false,
          link: `/merchant/orders`
        })
      })
    }
    
    // 获取待处理售后作为消息
    const afterSaleRes = await listAfterSale({ pageNum: 1, pageSize: 10, afterSaleStatus: 0 })
    if (afterSaleRes?.list) {
      afterSaleRes.list.forEach((item: any) => {
        messages.push({
          id: `aftersale-${item.id}`,
          title: '售后申请待处理',
          content: `售后单号 ${item.afterSaleNo} 申请${item.afterSaleType === 1 ? '退款' : '退货退款'}，原因：${item.applyReason}`,
          time: item.createTime,
          type: 'aftersale',
          isRead: false,
          link: `/merchant/after-sales`
        })
      })
    }
    
    // 按时间排序
    messages.sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
    
    messageList.value = messages
  } catch (error) {
    console.error('Failed to load messages', error)
  }
}

const getIcon = (type: string) => {
  const map: Record<string, any> = {
    system: Bell,
    order: Box,
    aftersale: Warning,
    message: ChatDotRound
  }
  return map[type] || Bell
}

const getIconColor = (type: string) => {
  const map: Record<string, string> = {
    system: '#409eff',
    order: '#67c23a',
    aftersale: '#e6a23c',
    message: '#909399'
  }
  return map[type] || '#909399'
}

const readMessage = (msg: any) => {
  Object.assign(currentMessage, msg)
  msg.isRead = true
  showDetailDialog.value = true
}

const markAllRead = () => {
  messageList.value.forEach(msg => msg.isRead = true)
  ElMessage.success('已全部标记为已读')
}

const handleTabChange = () => {
  // 切换标签时可以做额外处理
}

onMounted(() => {
  loadMessages()
})
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
