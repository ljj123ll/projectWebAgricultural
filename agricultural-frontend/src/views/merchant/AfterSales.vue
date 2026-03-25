<template>
  <div class="after-sales-page">
    <div class="page-header">
      <h2>售后处理</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待商家处理" name="pending" />
      <el-tab-pane label="等待用户/平台" name="processing" />
      <el-tab-pane label="已完成" name="completed" />
      <el-tab-pane label="已驳回" name="rejected" />
    </el-tabs>

    <div class="after-sales-list" v-loading="loading">
      <div v-for="item in afterSalesList" :key="item.id" class="after-sale-card">
        <div class="card-header">
          <span class="after-sale-no">售后单号：{{ item.afterSaleNo }}</span>
          <el-tag :type="getStatusType(item.afterSaleStatus)">{{ getStatusText(item.afterSaleStatus) }}</el-tag>
        </div>

        <div class="order-info">
          <span>关联订单：{{ item.orderNo }}</span>
          <span class="time">{{ formatDate(item.createTime) }}</span>
        </div>

        <div class="after-sale-info">
          <div class="info-row">
            <span class="label">售后类型：</span>
            <span>{{ getTypeText(item.afterSaleType) }}</span>
          </div>
          <div class="info-row">
            <span class="label">申请原因：</span>
            <span>{{ item.applyReason || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">当前阶段：</span>
            <span>{{ getResponsibleLabel(item.afterSaleStatus) }}</span>
          </div>
          <div class="info-row" v-if="item.returnLogisticsNo">
            <span class="label">退货物流：</span>
            <span>{{ item.returnLogisticsCompany || '-' }} / {{ item.returnLogisticsNo }}</span>
          </div>
          <div class="info-row" v-if="item.handleResult">
            <span class="label">处理说明：</span>
            <span>{{ item.handleResult }}</span>
          </div>
        </div>

        <div class="card-actions">
          <el-button v-if="item.afterSaleStatus === 1" type="primary" @click="openHandleDialog(item)">
            处理申请
          </el-button>
          <el-button
            v-if="item.afterSaleType === 2 && item.afterSaleStatus === 2"
            type="success"
            @click="confirmReturnRefund(item)"
          >
            确认收货并退款
          </el-button>
          <el-button
            v-if="item.orderNo"
            type="primary"
            plain
            @click="openChatDialog(item)"
          >
            实时沟通
          </el-button>
          <el-tag v-if="getUnreadCount(item.afterSaleNo) > 0" type="danger">
            未读 {{ getUnreadCount(item.afterSaleNo) }}
          </el-tag>
          <el-tag v-if="item.afterSaleStatus === 6" type="info">等待用户提交退货物流</el-tag>
          <el-tag v-if="item.afterSaleStatus === 4" type="warning">等待管理员裁决</el-tag>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && afterSalesList.length === 0" description="暂无售后申请" />

    <el-dialog v-model="showHandleDialog" title="处理售后申请" width="560px">
      <div class="handle-content">
        <div class="info-section">
          <p><strong>售后单号：</strong>{{ currentItem.afterSaleNo }}</p>
          <p><strong>关联订单：</strong>{{ currentItem.orderNo }}</p>
          <p><strong>售后类型：</strong>{{ getTypeText(currentItem.afterSaleType) }}</p>
          <p><strong>申请原因：</strong>{{ currentItem.applyReason }}</p>
        </div>

        <el-divider />

        <el-form :model="handleForm" label-position="top">
          <el-form-item label="处理结果">
            <el-radio-group v-model="handleForm.result">
              <el-radio label="agree">{{ currentItem.afterSaleType === 2 ? '同意退货退款' : '同意退款' }}</el-radio>
              <el-radio label="reject">拒绝申请</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理说明">
            <el-input
              v-model="handleForm.remark"
              type="textarea"
              rows="3"
              placeholder="请输入处理说明"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showChatDialog"
      title="售后实时沟通（用户/商家/管理员）"
      width="900px"
      destroy-on-close
    >
      <el-tabs v-model="chatTab" @tab-change="handleChatTabChange">
        <el-tab-pane label="订单实时沟通" name="order">
          <OrderChatPanel
            v-if="chatOrderNo"
            :order-no="chatOrderNo"
            role="merchant"
            title="售后实时沟通"
          />
          <el-empty v-else description="暂无可沟通订单信息" :image-size="72" />
        </el-tab-pane>
        <el-tab-pane :label="afterSaleMsgTabLabel" name="afterSale">
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
                  :disabled="!chatAfterSaleNo"
                  @click="submitAfterSaleMessage"
                >
                  发送消息
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { AfterSaleMessage } from '@/types';
import {
  confirmAfterSaleReturn,
  handleAfterSale,
  listAfterSale,
  listAfterSaleMessages,
  sendAfterSaleMessage
} from '@/apis/merchant';
import { getAfterSaleUnreadCount, markAfterSaleMessagesRead } from '@/apis/after-sale-message';
import OrderChatPanel from '@/components/OrderChatPanel.vue';
import {
  AFTER_SALE_STATUS,
  getAfterSaleResponsibleLabel,
  getAfterSaleStatusTagType,
  getAfterSaleStatusText
} from '@/utils/afterSale';

type TabKey = 'all' | 'pending' | 'processing' | 'completed' | 'rejected';

const activeTab = ref<TabKey>('all');
const showHandleDialog = ref(false);
const showChatDialog = ref(false);
const chatOrderNo = ref('');
const chatAfterSaleNo = ref('');
const chatTab = ref<'order' | 'afterSale'>('afterSale');
const loading = ref(false);
const currentItem = ref<any>({});
const unreadCountMap = ref<Record<string, number>>({});
const afterSaleMessages = ref<AfterSaleMessage[]>([]);
const afterSaleMsgLoading = ref(false);
const afterSaleSendPending = ref(false);
const afterSaleMsgForm = reactive({
  content: ''
});

const handleForm = reactive({
  result: 'agree',
  remark: ''
});

const afterSalesList = ref<any[]>([]);

const getStatusType = (status: number) => {
  return getAfterSaleStatusTagType(status);
};

const getStatusText = (status: number) => {
  return getAfterSaleStatusText(status);
};

const getResponsibleLabel = (status: number) => {
  return getAfterSaleResponsibleLabel(status);
};

const getTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '仅退款',
    2: '退货退款',
    3: '换货'
  };
  return map[type] || '未知类型';
};

const senderText = (senderType?: number) => {
  if (senderType === 1) return '用户';
  if (senderType === 2) return '商家';
  if (senderType === 3) return '管理员';
  return '未知';
};

const getUnreadCount = (afterSaleNo?: string) => {
  if (!afterSaleNo) return 0;
  return Number(unreadCountMap.value[afterSaleNo] || 0);
};

const afterSaleMsgTabLabel = computed(() => {
  const unread = getUnreadCount(chatAfterSaleNo.value);
  return unread > 0 ? `售后接口消息 (${unread})` : '售后接口消息';
});

const formatDate = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
};

const mergeAfterSaleList = (list: any[]) => {
  const map = new Map<number, any>();
  list.forEach((item) => {
    if (item?.id) map.set(item.id, item);
  });
  return [...map.values()].sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
};

const loadUnreadCounts = async () => {
  const source = afterSalesList.value.slice(0, 30);
  if (!source.length) {
    unreadCountMap.value = {};
    return;
  }
  const entries = await Promise.all(source.map(async (item) => {
    const no = String(item?.afterSaleNo || '');
    if (!no) return ['', 0] as const;
    try {
      const count = await getAfterSaleUnreadCount(no, 2);
      return [no, Number(count || 0)] as const;
    } catch (error) {
      console.warn('读取售后消息未读数失败', no, error);
      return [no, 0] as const;
    }
  }));
  const map: Record<string, number> = {};
  entries.forEach(([no, count]) => {
    if (no) map[no] = count;
  });
  unreadCountMap.value = map;
};

const loadAfterSales = async () => {
  loading.value = true;
  try {
    if (activeTab.value === 'pending') {
      const [res1, res2] = await Promise.all([
        listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.PENDING_MERCHANT }),
        listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND })
      ]);
      afterSalesList.value = mergeAfterSaleList([...(res1?.list || []), ...(res2?.list || [])]);
      await loadUnreadCounts();
      return;
    }
    if (activeTab.value === 'completed') {
      const res = await listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.RESOLVED });
      afterSalesList.value = res?.list || [];
      await loadUnreadCounts();
      return;
    }
    if (activeTab.value === 'rejected') {
      const res = await listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.REJECTED });
      afterSalesList.value = res?.list || [];
      await loadUnreadCounts();
      return;
    }
    if (activeTab.value === 'processing') {
      const [res4, res6] = await Promise.all([
        listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.ADMIN }),
        listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_USER_RETURN })
      ]);
      afterSalesList.value = mergeAfterSaleList([...(res4?.list || []), ...(res6?.list || [])]);
      await loadUnreadCounts();
      return;
    }
    const [res1, res2, res3, res4, res5, res6] = await Promise.all([
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.PENDING_MERCHANT }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.RESOLVED }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.ADMIN }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.REJECTED }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_USER_RETURN })
    ]);
    afterSalesList.value = mergeAfterSaleList([
      ...(res1?.list || []),
      ...(res2?.list || []),
      ...(res3?.list || []),
      ...(res4?.list || []),
      ...(res5?.list || []),
      ...(res6?.list || [])
    ]);
    await loadUnreadCounts();
  } finally {
    loading.value = false;
  }
};

const handleTabChange = () => {
  void loadAfterSales();
};

const openHandleDialog = (item: any) => {
  currentItem.value = item;
  handleForm.result = 'agree';
  handleForm.remark = '';
  showHandleDialog.value = true;
};

const openChatDialog = (item: any) => {
  chatOrderNo.value = String(item?.orderNo || '');
  chatAfterSaleNo.value = String(item?.afterSaleNo || '');
  chatTab.value = chatAfterSaleNo.value ? 'afterSale' : 'order';
  if (!chatOrderNo.value && !chatAfterSaleNo.value) {
    ElMessage.warning('未找到关联订单号，无法进入沟通');
    return;
  }
  showChatDialog.value = true;
  if (chatAfterSaleNo.value) {
    void enterAfterSaleMessageTab();
  }
};

const loadAfterSaleMessages = async () => {
  if (!chatAfterSaleNo.value) return;
  afterSaleMsgLoading.value = true;
  try {
    const res = await listAfterSaleMessages(chatAfterSaleNo.value, { pageNum: 1, pageSize: 100 });
    const source = res?.list || [];
    afterSaleMessages.value = [...source].sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
  } finally {
    afterSaleMsgLoading.value = false;
  }
};

const markCurrentAfterSaleRead = async () => {
  if (!chatAfterSaleNo.value) return;
  try {
    await markAfterSaleMessagesRead(chatAfterSaleNo.value, 2);
    unreadCountMap.value[chatAfterSaleNo.value] = 0;
    window.dispatchEvent(new Event('merchant-message-read-refresh'));
  } catch (error) {
    console.warn('标记售后消息已读失败', error);
  }
};

const enterAfterSaleMessageTab = async () => {
  await Promise.all([loadAfterSaleMessages(), markCurrentAfterSaleRead()]);
};

const submitAfterSaleMessage = async () => {
  if (!chatAfterSaleNo.value) return;
  const content = afterSaleMsgForm.content.trim();
  if (!content) {
    ElMessage.warning('请输入消息内容');
    return;
  }
  afterSaleSendPending.value = true;
  try {
    await sendAfterSaleMessage(chatAfterSaleNo.value, { content });
    afterSaleMsgForm.content = '';
    await enterAfterSaleMessageTab();
    ElMessage.success('消息已发送');
  } finally {
    afterSaleSendPending.value = false;
  }
};

const handleChatTabChange = (name: string | number) => {
  if (String(name) === 'afterSale') {
    void enterAfterSaleMessageTab();
  }
};

const confirmHandle = async () => {
  try {
    const agree = handleForm.result === 'agree';
    await handleAfterSale(currentItem.value.id, {
      agree,
      handleResult: handleForm.remark || (agree
        ? (currentItem.value.afterSaleType === 2 ? '同意退货退款，请用户填写退货单号' : '同意退款，售后已完成')
        : '拒绝该售后申请')
    });
    ElMessage.success('处理成功');
    showHandleDialog.value = false;
    window.dispatchEvent(new Event('merchant-pending-refresh'));
    await loadAfterSales();
  } catch (error) {
    console.error('Handle failed', error);
  }
};

const confirmReturnRefund = async (item: any) => {
  ElMessageBox.confirm('确认已收到退货并执行退款？', '确认退款', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await confirmAfterSaleReturn(item.id, { handleResult: '商家确认收到退货，退款完成。' });
    ElMessage.success('已完成退款');
    window.dispatchEvent(new Event('merchant-pending-refresh'));
    await loadAfterSales();
  }).catch(() => {});
};

onMounted(() => {
  void loadAfterSales();
});
</script>

<style scoped lang="scss">
.after-sales-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
  }
}

.after-sales-list {
  margin-top: 20px;
}

.after-sale-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .after-sale-no {
      color: #666;
      font-size: 14px;
    }
  }

  .order-info {
    display: flex;
    justify-content: space-between;
    color: #999;
    font-size: 12px;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;
  }

  .after-sale-info {
    background: #f5f7fa;
    border-radius: 4px;
    padding: 12px;
    margin-bottom: 12px;

    .info-row {
      display: flex;
      margin-bottom: 8px;
      font-size: 14px;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        color: #666;
        width: 88px;
        flex-shrink: 0;
      }
    }
  }

  .card-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.handle-content {
  .info-section {
    p {
      margin: 8px 0;
      color: #666;
    }
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
</style>
