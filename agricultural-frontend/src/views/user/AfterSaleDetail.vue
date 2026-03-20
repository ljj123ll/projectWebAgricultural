<template>
  <div class="after-sale-detail-page">
    <el-button link @click="router.back()" class="back-btn">
      <el-icon><ArrowLeft /></el-icon>
      返回
    </el-button>

    <h1 class="page-title">售后详情</h1>

    <div class="status-section">
      <el-icon size="48" color="#67c23a"><CircleCheckFilled /></el-icon>
      <h2>{{ statusTitle }}</h2>
      <p>{{ statusDesc }}</p>
    </div>

    <div class="info-section">
      <h3>售后信息</h3>
      <p>售后单号：{{ afterSaleNo }}</p>
      <p>售后类型：{{ afterSaleTypeText }}</p>
      <p>申请原因：{{ afterSale?.applyReason || '-' }}</p>
      <p>申请时间：{{ afterSale?.createTime || '-' }}</p>
    </div>

    <div class="progress-section">
      <h3>处理进度</h3>
      <el-timeline>
        <el-timeline-item type="primary">
          <h4>提交申请</h4>
          <p>{{ afterSale?.createTime || '-' }}</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === 1" type="warning">
          <h4>商家处理中</h4>
          <p>等待商家处理</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === 2" type="warning">
          <h4>协商中</h4>
          <p>等待双方协商</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === 3" type="success">
          <h4>处理完成</h4>
          <p>商家已处理完成</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === 4">
          <h4>管理员介入</h4>
          <p>平台正在处理</p>
        </el-timeline-item>
        <el-timeline-item v-if="afterSale?.afterSaleStatus === 5" type="danger">
          <h4>处理结果</h4>
          <p>售后已驳回</p>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="message-section">
      <div class="message-header">
        <h3>售后沟通</h3>
        <el-button
          v-if="afterSale?.afterSaleStatus === 1 || afterSale?.afterSaleStatus === 2"
          type="danger"
          plain
          size="small"
          @click="applyAdminIntervention"
        >
          申请管理员介入
        </el-button>
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
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { listAfterSaleMessages, sendAfterSaleMessage, escalateAfterSale, getAfterSaleDetail } from '@/apis/user';
import type { AfterSale, AfterSaleMessage } from '@/types';

const router = useRouter();
const route = useRoute();

const afterSaleNo = ref(route.params.afterSaleNo as string);
const afterSale = ref<AfterSale | null>(null);
const messageText = ref('');
const messages = ref<Array<{ id: number; sender: 'user' | 'merchant' | 'admin'; content: string; time: string }>>([]);

const senderToString = (senderType: number) => {
  if (senderType === 1) return 'user';
  if (senderType === 2) return 'merchant';
  return 'admin';
};

const loadDetail = async () => {
  if (!afterSaleNo.value) return;
  const res = await getAfterSaleDetail(afterSaleNo.value);
  afterSale.value = res || null;
};

const loadMessages = async () => {
  if (!afterSaleNo.value) return;
  const res = await listAfterSaleMessages(afterSaleNo.value, { pageNum: 1, pageSize: 50 });
  const list = (res && res.list) ? res.list : [];
  messages.value = list.map((msg: AfterSaleMessage) => ({
    id: msg.id,
    sender: senderToString(msg.senderType || 0) as 'user' | 'merchant' | 'admin',
    content: msg.content,
    time: (msg.createTime as any) || ''
  }));
};

const sendMessage = async () => {
  if (!messageText.value.trim()) {
    ElMessage.warning('请输入沟通内容');
    return;
  }
  await sendAfterSaleMessage(afterSaleNo.value, { content: messageText.value.trim() });
  messageText.value = '';
  await loadMessages();
};

const applyAdminIntervention = async () => {
  if (!afterSale.value?.id) return;
  await escalateAfterSale(afterSale.value.id);
  ElMessage.success('已申请管理员介入，请耐心等待处理');
  await loadDetail();
  await loadMessages();
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
  if (s === 1) return '商家处理中';
  if (s === 2) return '协商中';
  if (s === 3) return '处理完成';
  if (s === 4) return '管理员介入';
  if (s === 5) return '已驳回';
  return '加载中...';
});

const statusDesc = computed(() => {
  const s = afterSale.value?.afterSaleStatus;
  if (s === 1) return '等待商家处理';
  if (s === 2) return '等待双方协商';
  if (s === 3) return '商家已处理完成';
  if (s === 4) return '平台正在处理';
  if (s === 5) return '售后已驳回';
  return '请稍后';
});

onMounted(async () => {
  try {
    await loadDetail();
    await loadMessages();
  } catch (e) {
    console.error(e);
  }
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
