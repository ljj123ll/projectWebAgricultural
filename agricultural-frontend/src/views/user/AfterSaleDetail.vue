<template>
  <div class="after-sale-detail-page">
    <el-button link @click="router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <h1 class="page-title">售后详情</h1>

    <div class="status-section">
      <el-icon size="48" color="#67c23a"><CircleCheckFilled /></el-icon>
      <h2>售后处理中</h2>
      <p>商家正在处理您的售后申请</p>
    </div>

    <div class="info-section">
      <h3>售后信息</h3>
      <p>售后单号：{{ afterSaleNo }}</p>
      <p>售后类型：退款</p>
      <p>申请原因：商品质量问题</p>
      <p>申请时间：2024-03-01 10:30:00</p>
    </div>

    <div class="progress-section">
      <h3>处理进度</h3>
      <el-timeline>
        <el-timeline-item type="primary">
          <h4>提交申请</h4>
          <p>2024-03-01 10:30:00</p>
        </el-timeline-item>
        <el-timeline-item type="warning">
          <h4>商家处理中</h4>
          <p>等待商家审核</p>
        </el-timeline-item>
        <el-timeline-item>
          <h4>处理完成</h4>
          <p>待处理</p>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="message-section">
      <div class="message-header">
        <h3>售后沟通</h3>
        <el-button type="danger" plain size="small" @click="applyAdminIntervention">申请管理员介入</el-button>
      </div>
      <div class="message-list">
        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ self: msg.sender === 'user' }">
          <div class="message-content">
            <p>{{ msg.content }}</p>
            <span class="time">{{ msg.time }}</span>
          </div>
        </div>
      </div>
      <div class="message-input">
        <el-input
          v-model="messageText"
          type="textarea"
          :rows="3"
          maxlength="200"
          show-word-limit
          placeholder="请输入要与商家沟通的内容"
        />
        <el-button type="primary" @click="sendMessage">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();

const afterSaleNo = ref(route.params.afterSaleNo as string);
const messageText = ref('');
const messages = ref([
  { id: 1, sender: 'user', content: '商品有异味，申请退款。', time: '2024-03-01 11:00' },
  { id: 2, sender: 'merchant', content: '非常抱歉，能否提供照片？', time: '2024-03-01 11:20' }
]);

const sendMessage = () => {
  if (!messageText.value.trim()) {
    ElMessage.warning('请输入沟通内容');
    return;
  }
  messages.value.push({
    id: Date.now(),
    sender: 'user',
    content: messageText.value.trim(),
    time: new Date().toLocaleString()
  });
  messageText.value = '';
};

const applyAdminIntervention = () => {
  ElMessage.success('已申请管理员介入，请耐心等待处理');
};

onMounted(() => {
  console.log('加载售后单:', route.params.afterSaleNo);
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
  }

  .message-list {
    background: #f5f7fa;
    border-radius: 8px;
    padding: 12px;
    margin-bottom: 12px;
    max-height: 240px;
    overflow-y: auto;

    .message-item {
      display: flex;
      margin-bottom: 8px;

      &.self {
        justify-content: flex-end;

        .message-content {
          background: #67c23a;
          color: #fff;
        }
      }

      .message-content {
        max-width: 70%;
        background: #fff;
        border-radius: 8px;
        padding: 8px 12px;
        box-shadow: 0 1px 4px rgba(0,0,0,0.08);

        p {
          margin: 0 0 6px;
        }

        .time {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  .message-input {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .after-sale-detail-page {
    padding: 16px;
  }
}
</style>
