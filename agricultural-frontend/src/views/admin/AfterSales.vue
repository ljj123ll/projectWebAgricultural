<template>
  <div class="admin-after-sales-page">
    <div class="page-header">
      <h2>售后裁决</h2>
      <el-tag type="info">用户申请介入 / 商家拒绝售后均可在此裁决</el-tag>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadList">
      <el-tab-pane label="待裁决" name="pending-judge" />
      <el-tab-pane label="处理中" name="processing" />
      <el-tab-pane label="已完成" name="resolved" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <div class="list-wrap" v-loading="loading">
      <el-empty v-if="!loading && list.length === 0" description="暂无售后记录" />
      <div v-for="item in list" :key="item.id" class="card">
        <div class="card-head">
          <div>
            <div class="title">售后单号：{{ item.afterSaleNo }}</div>
            <div class="sub">关联订单：{{ item.orderNo }}</div>
          </div>
          <el-tag :type="statusTagType(item.afterSaleStatus)">{{ statusText(item.afterSaleStatus) }}</el-tag>
        </div>

        <div class="grid">
          <div><span class="label">类型：</span>{{ typeText(item.afterSaleType) }}</div>
          <div><span class="label">申请时间：</span>{{ formatTime(item.createTime) }}</div>
          <div><span class="label">用户ID：</span>{{ item.userId || '-' }}</div>
          <div><span class="label">商家ID：</span>{{ item.merchantId || '-' }}</div>
          <div class="full"><span class="label">当前阶段：</span>{{ getResponsibleLabel(item.afterSaleStatus) }}</div>
          <div class="full"><span class="label">申请原因：</span>{{ item.applyReason || '-' }}</div>
          <div class="full" v-if="item.handleResult"><span class="label">处理说明：</span>{{ item.handleResult }}</div>
          <div class="full" v-if="item.returnLogisticsNo">
            <span class="label">退货物流：</span>{{ item.returnLogisticsCompany || '-' }} / {{ item.returnLogisticsNo }}
          </div>
          <div class="full" v-if="parseImages(item.proofImgUrls).length">
            <span class="label">凭证：</span>
            <el-image
              v-for="img in parseImages(item.proofImgUrls)"
              :key="img"
              :src="toImage(img)"
              fit="cover"
              class="proof-img"
              :preview-src-list="parseImages(item.proofImgUrls).map(toImage)"
              preview-teleported
            />
          </div>
        </div>

        <div class="actions">
          <el-button
            v-if="item.orderNo"
            plain
            @click="openChatDialog(item)"
          >
            实时沟通
          </el-button>
          <el-tag v-if="getUnreadCount(item.afterSaleNo) > 0" type="danger">
            未读 {{ getUnreadCount(item.afterSaleNo) }}
          </el-tag>
          <el-button
            v-if="canJudge(item.afterSaleStatus)"
            type="primary"
            @click="openJudgeDialog(item)"
          >
            发起裁决
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="judgeDialogVisible" title="管理员售后裁决" width="620px">
      <div v-if="current">
        <el-alert type="warning" :closable="false" show-icon>
          当前售后状态：{{ statusText(current.afterSaleStatus) }}，请根据事实证据进行裁决。
        </el-alert>

        <el-form :model="judgeForm" label-position="top" class="judge-form">
          <el-form-item label="裁决结果">
            <el-radio-group v-model="judgeForm.decision">
              <el-radio label="approve_refund">支持退款（直接完成）</el-radio>
              <el-radio
                v-if="current.afterSaleType === 2"
                label="approve_return_refund"
              >
                支持退货退款（等待用户回传退货单号）
              </el-radio>
              <el-radio label="reject">不支持本次售后申请</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="裁决说明">
            <el-input
              v-model="judgeForm.remark"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              placeholder="请输入裁决说明，将同步给用户和商家"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="judgeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitJudge">确认裁决</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="chatDialogVisible"
      title="售后实时沟通（用户/商家/管理员）"
      width="920px"
      destroy-on-close
    >
      <el-tabs v-model="chatTab" @tab-change="handleChatTabChange">
        <el-tab-pane label="订单实时沟通" name="order">
          <OrderChatPanel
            v-if="chatOrderNo"
            :order-no="chatOrderNo"
            role="admin"
            title="售后实时沟通（管理员）"
          />
          <el-empty v-else description="缺少订单号，无法查看订单沟通" :image-size="72" />
        </el-tab-pane>
        <el-tab-pane :label="afterSaleMsgTabLabel" name="afterSale">
          <div class="after-sale-msg-panel">
            <el-scrollbar height="360px" v-loading="afterSaleMsgLoading">
              <div v-if="!afterSaleMessages.length" class="empty-msg">
                <el-empty description="暂无售后消息" :image-size="64" />
              </div>
              <div v-for="msg in afterSaleMessages" :key="msg.id" class="msg-item">
                <div class="msg-meta">
                  <span class="sender">{{ senderText(msg.senderType) }}</span>
                  <span class="time">{{ formatTime(msg.createTime) }}</span>
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
                placeholder="请输入要发送给用户/商家的售后沟通内容"
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { handleAfterSale, listAdminAfterSaleMessages, listAfterSale, sendAdminAfterSaleMessage } from '@/apis/admin';
import { getAfterSaleUnreadCount, markAfterSaleMessagesRead } from '@/apis/after-sale-message';
import type { AfterSale } from '@/types';
import { useUserStore } from '@/stores/modules/user';
import { getFullImageUrl } from '@/utils/image';
import OrderChatPanel from '@/components/OrderChatPanel.vue';
import {
  AFTER_SALE_STATUS,
  getAfterSaleResponsibleLabel,
  getAfterSaleStatusTagType,
  getAfterSaleStatusText
} from '@/utils/afterSale';
import { ADMIN_REALTIME_EVENT, parseRealtimePayload } from '@/utils/realtime';

type TabType = 'pending-judge' | 'processing' | 'resolved' | 'all';
type DecisionType = 'approve_refund' | 'approve_return_refund' | 'reject';
type AfterSaleRecord = AfterSale & { userId?: number; merchantId?: number; adminId?: number };

const userStore = useUserStore();

const activeTab = ref<TabType>('pending-judge');
const list = ref<AfterSaleRecord[]>([]);
const loading = ref(false);
const judgeDialogVisible = ref(false);
const chatDialogVisible = ref(false);
const chatOrderNo = ref('');
const chatAfterSaleNo = ref('');
const chatTab = ref<'order' | 'afterSale'>('order');
const afterSaleMsgLoading = ref(false);
const afterSaleSendPending = ref(false);
const afterSaleMessages = ref<any[]>([]);
const unreadCountMap = ref<Record<string, number>>({});
const afterSaleMsgForm = reactive({
  content: ''
});
const current = ref<AfterSaleRecord | null>(null);
const judgeForm = reactive({
  decision: 'approve_refund' as DecisionType,
  remark: ''
});

const typeText = (type?: number) => {
  if (type === 1) return '仅退款';
  if (type === 2) return '退货退款';
  if (type === 3) return '换货';
  return '未知类型';
};

const statusText = (status?: number) => {
  if (status === AFTER_SALE_STATUS.REJECTED) return '商家拒绝/未通过';
  return getAfterSaleStatusText(status);
};

const statusTagType = (status?: number) => {
  return getAfterSaleStatusTagType(status);
};

const getResponsibleLabel = (status?: number) => getAfterSaleResponsibleLabel(status);

const formatTime = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
};

const canJudge = (status?: number) => status === AFTER_SALE_STATUS.ADMIN || status === AFTER_SALE_STATUS.REJECTED;

const parseImages = (raw?: string) => {
  if (!raw) return [];
  return raw.split(',').map(item => item.trim()).filter(Boolean);
};

const toImage = (path: string) => getFullImageUrl(path);

const getUnreadCount = (afterSaleNo?: string) => {
  if (!afterSaleNo) return 0;
  return Number(unreadCountMap.value[afterSaleNo] || 0);
};

const afterSaleMsgTabLabel = computed(() => {
  const unread = getUnreadCount(chatAfterSaleNo.value);
  return unread > 0 ? `售后接口消息 (${unread})` : '售后接口消息';
});

const senderText = (senderType?: number) => {
  if (senderType === 1) return '用户';
  if (senderType === 2) return '商家';
  if (senderType === 3) return '管理员';
  return '未知';
};

const uniqueSort = (records: AfterSaleRecord[]) => {
  const map = new Map<number, AfterSaleRecord>();
  records.forEach(item => {
    if (item?.id) map.set(item.id, item);
  });
  return [...map.values()].sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
};

const loadUnreadCounts = async () => {
  const source = list.value.slice(0, 30);
  if (!source.length) {
    unreadCountMap.value = {};
    return;
  }
  const entries = await Promise.all(source.map(async (item) => {
    const no = String(item?.afterSaleNo || '');
    if (!no) return ['', 0] as const;
    try {
      const count = await getAfterSaleUnreadCount(no, 3);
      return [no, Number(count || 0)] as const;
    } catch (error) {
      console.warn('读取售后未读数失败', no, error);
      return [no, 0] as const;
    }
  }));
  const map: Record<string, number> = {};
  entries.forEach(([no, count]) => {
    if (no) map[no] = count;
  });
  unreadCountMap.value = map;
};

const loadByStatus = async (status: number) => {
  const res = await listAfterSale({ pageNum: 1, pageSize: 100, afterSaleStatus: status });
  return (res?.list || []) as AfterSaleRecord[];
};

const loadList = async () => {
  loading.value = true;
  try {
    if (activeTab.value === 'pending-judge') {
      const [intervene, rejected] = await Promise.all([
        loadByStatus(AFTER_SALE_STATUS.ADMIN),
        loadByStatus(AFTER_SALE_STATUS.REJECTED)
      ]);
      list.value = uniqueSort([...intervene, ...rejected]);
      await loadUnreadCounts();
      return;
    }
    if (activeTab.value === 'processing') {
      const [s1, s2, s6] = await Promise.all([
        loadByStatus(AFTER_SALE_STATUS.PENDING_MERCHANT),
        loadByStatus(AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND),
        loadByStatus(AFTER_SALE_STATUS.WAIT_USER_RETURN)
      ]);
      list.value = uniqueSort([...s1, ...s2, ...s6]);
      await loadUnreadCounts();
      return;
    }
    if (activeTab.value === 'resolved') {
      list.value = uniqueSort(await loadByStatus(AFTER_SALE_STATUS.RESOLVED));
      await loadUnreadCounts();
      return;
    }
    const [s1, s2, s3, s4, s5, s6] = await Promise.all([
      loadByStatus(AFTER_SALE_STATUS.PENDING_MERCHANT),
      loadByStatus(AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND),
      loadByStatus(AFTER_SALE_STATUS.RESOLVED),
      loadByStatus(AFTER_SALE_STATUS.ADMIN),
      loadByStatus(AFTER_SALE_STATUS.REJECTED),
      loadByStatus(AFTER_SALE_STATUS.WAIT_USER_RETURN)
    ]);
    list.value = uniqueSort([...s1, ...s2, ...s3, ...s4, ...s5, ...s6]);
    await loadUnreadCounts();
  } finally {
    loading.value = false;
  }
};

const openJudgeDialog = (item: AfterSaleRecord) => {
  current.value = item;
  judgeForm.decision = item.afterSaleType === 2 ? 'approve_return_refund' : 'approve_refund';
  judgeForm.remark = '';
  judgeDialogVisible.value = true;
};

const openChatDialog = (item: AfterSaleRecord) => {
  chatOrderNo.value = String(item?.orderNo || '');
  chatAfterSaleNo.value = String(item?.afterSaleNo || '');
  chatTab.value = chatAfterSaleNo.value ? 'afterSale' : 'order';
  if (!chatOrderNo.value && !chatAfterSaleNo.value) {
    ElMessage.warning('未找到关联订单号，无法进入沟通');
    return;
  }
  if (chatAfterSaleNo.value) {
    void enterAfterSaleMessageTab();
  }
  chatDialogVisible.value = true;
};

const loadAfterSaleMessages = async () => {
  if (!chatAfterSaleNo.value) return;
  afterSaleMsgLoading.value = true;
  try {
    const res = await listAdminAfterSaleMessages(chatAfterSaleNo.value, { pageNum: 1, pageSize: 100 });
    const source = res?.list || [];
    afterSaleMessages.value = [...source].sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
  } finally {
    afterSaleMsgLoading.value = false;
  }
};

const markCurrentAfterSaleRead = async () => {
  if (!chatAfterSaleNo.value) return;
  try {
    await markAfterSaleMessagesRead(chatAfterSaleNo.value, 3);
    unreadCountMap.value[chatAfterSaleNo.value] = 0;
  } catch (error) {
    console.warn('标记管理员售后消息已读失败', error);
  }
};

const enterAfterSaleMessageTab = async () => {
  await Promise.all([loadAfterSaleMessages(), markCurrentAfterSaleRead()]);
};

const handleChatTabChange = (name: string | number) => {
  if (String(name) === 'afterSale') {
    void enterAfterSaleMessageTab();
  }
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
    await sendAdminAfterSaleMessage(chatAfterSaleNo.value, { content });
    afterSaleMsgForm.content = '';
    await enterAfterSaleMessageTab();
    ElMessage.success('消息已发送');
  } finally {
    afterSaleSendPending.value = false;
  }
};

const submitJudge = async () => {
  if (!current.value) return;
  const decision = judgeForm.decision;
  let afterSaleStatus: number = AFTER_SALE_STATUS.REJECTED;
  if (decision === 'approve_refund') afterSaleStatus = AFTER_SALE_STATUS.RESOLVED;
  if (decision === 'approve_return_refund') afterSaleStatus = AFTER_SALE_STATUS.WAIT_USER_RETURN;

  const defaultRemark =
    decision === 'approve_refund'
      ? '管理员裁决：支持用户退款，售后已完成。'
      : decision === 'approve_return_refund'
        ? '管理员裁决：支持退货退款，请用户上传退货快递单号。'
        : '管理员裁决：不支持本次售后申请。';

  await handleAfterSale(current.value.id, {
    afterSaleStatus,
    handleResult: judgeForm.remark.trim() || defaultRemark,
    adminId: userStore.userInfo?.id
  });
  ElMessage.success('裁决提交成功');
  judgeDialogVisible.value = false;
  await loadList();
};

const handleRealtimeRefresh = (event: Event) => {
  const payload = parseRealtimePayload(event);
  const isAfterSaleEvent = String(payload.reason || '').startsWith('AFTER_SALE');
  if (!isAfterSaleEvent) return;
  void loadList();
  if (chatDialogVisible.value && chatAfterSaleNo.value && (!payload.refNo || payload.refNo === chatAfterSaleNo.value)) {
    void enterAfterSaleMessageTab();
  }
};

onMounted(() => {
  void loadList();
  window.addEventListener(ADMIN_REALTIME_EVENT, handleRealtimeRefresh);
});

onUnmounted(() => {
  window.removeEventListener(ADMIN_REALTIME_EVENT, handleRealtimeRefresh);
});
</script>

<style scoped lang="scss">
.admin-after-sales-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  h2 {
    margin: 0;
    font-size: 22px;
    color: #111827;
  }
}

.list-wrap {
  margin-top: 12px;
}

.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;

  .title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }

  .sub {
    margin-top: 4px;
    color: #6b7280;
    font-size: 13px;
  }
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
  color: #334155;
  font-size: 14px;

  .label {
    color: #94a3b8;
  }

  .full {
    grid-column: 1 / -1;
  }
}

.proof-img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  margin-right: 8px;
  border: 1px solid #e5e7eb;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.judge-form {
  margin-top: 12px;
}

.after-sale-msg-panel {
  .empty-msg {
    padding-top: 80px;
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

@media (max-width: 768px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
