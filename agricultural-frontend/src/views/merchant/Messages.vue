<template>
  <div class="messages-page">
    <div class="page-header">
      <h2>消息中心</h2>
      <el-button link type="primary" @click="markAllRead">全部已读</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="全部消息" name="all">
        <el-badge :value="unreadCount" v-if="unreadCount > 0" />
      </el-tab-pane>
      <el-tab-pane label="系统通知" name="system" />
      <el-tab-pane label="订单消息" name="order" />
      <el-tab-pane label="售后消息" name="aftersale" />
    </el-tabs>

    <div class="message-list">
      <div
        v-for="msg in messageList"
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

    <el-empty v-if="messageList.length === 0" description="暂无消息" />

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
import { ref, reactive } from 'vue'
import { Bell, Box, Warning, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('all')
const showDetailDialog = ref(false)
const currentMessage = reactive<any>({})
const unreadCount = ref(3)

const messageList = ref([
  {
    id: 1,
    type: 'order',
    title: '新订单提醒',
    content: '您有一个新订单待处理，订单号：ORD202403080001，请及时发货',
    time: '10分钟前',
    isRead: false
  },
  {
    id: 2,
    type: 'aftersale',
    title: '售后申请提醒',
    content: '订单ORD202403070001有新的售后申请，请及时处理',
    time: '30分钟前',
    isRead: false
  },
  {
    id: 3,
    type: 'system',
    title: '系统通知',
    content: '您的店铺已通过实名认证审核，可以正常经营了',
    time: '2小时前',
    isRead: false
  },
  {
    id: 4,
    type: 'system',
    title: '平台公告',
    content: '关于春节期间物流安排的通知，请提前做好准备',
    time: '昨天',
    isRead: true
  },
  {
    id: 5,
    type: 'order',
    title: '订单发货提醒',
    content: '订单ORD202403060005已发货，请及时关注物流状态',
    time: '昨天',
    isRead: true
  }
])

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
  updateUnreadCount()
}

const markAllRead = () => {
  messageList.value.forEach(msg => msg.isRead = true)
  updateUnreadCount()
  ElMessage.success('已全部标记为已读')
}

const updateUnreadCount = () => {
  unreadCount.value = messageList.value.filter(msg => !msg.isRead).length
}
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
