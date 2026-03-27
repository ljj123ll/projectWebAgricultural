<template>
  <div class="merchant-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <el-icon size="32" color="#67c23a"><Shop /></el-icon>
        <span v-if="!isCollapsed" class="logo-text">商家中心</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        class="merchant-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#67c23a"
      >
        <el-menu-item index="/merchant/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>控制台</template>
        </el-menu-item>

        <el-menu-item index="/merchant/shop">
          <el-icon><Shop /></el-icon>
          <template #title>店铺信息</template>
        </el-menu-item>

        <el-menu-item index="/merchant/products">
          <el-icon><Goods /></el-icon>
          <template #title>商品管理</template>
        </el-menu-item>

        <el-menu-item index="/merchant/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
          <span v-if="pendingOrderCount > 0" class="menu-dot">{{ formatBadgeValue(pendingOrderCount) }}</span>
        </el-menu-item>

        <el-menu-item index="/merchant/after-sales">
          <el-icon><Service /></el-icon>
          <template #title>售后处理</template>
          <span v-if="pendingAfterSaleCount > 0" class="menu-dot">{{ formatBadgeValue(pendingAfterSaleCount) }}</span>
        </el-menu-item>

        <el-menu-item index="/merchant/statistics">
          <el-icon><TrendCharts /></el-icon>
          <template #title>数据统计</template>
        </el-menu-item>

        <el-menu-item index="/merchant/messages">
          <el-icon><Bell /></el-icon>
          <template #title>消息中心</template>
          <span v-if="messageUnreadCount > 0" class="menu-dot">{{ formatBadgeValue(messageUnreadCount) }}</span>
        </el-menu-item>

        <el-menu-item index="/merchant/account">
          <el-icon><Wallet /></el-icon>
          <template #title>对账与补贴</template>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <div class="main-container">
      <!-- 顶部导航 -->
      <header class="header">
        <div class="header-left">
          <el-button link @click="toggleSidebar">
            <el-icon size="20"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
          </el-button>
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-badge :value="messageUnreadCount" :hidden="messageUnreadCount === 0">
            <el-button circle @click="router.push('/merchant/messages')">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || '商家' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">店铺信息</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getMerchantAuditStatus, listOrders, listAfterSale } from '@/apis/merchant';
import { getAfterSaleUnreadCount } from '@/apis/after-sale-message';
import { AFTER_SALE_STATUS } from '@/utils/afterSale';
import {
  MERCHANT_REALTIME_EVENT,
  buildRealtimeWsUrl,
  dispatchRealtimeRefresh,
  parseRealtimePayload
} from '@/utils/realtime';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isCollapsed = ref(false);
const pendingOrderCount = ref(0);
const pendingAfterSaleCount = ref(0);
const messageUnreadCount = ref(0);
let refreshTimer: ReturnType<typeof setInterval> | null = null;
let socket: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let manualClosed = false;
const socketConnected = ref(false);
const READ_STORAGE_PREFIX = 'merchant_message_read_map';

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
};

const formatBadgeValue = (count: number) => {
  return count > 99 ? '99+' : String(count);
};

// 加载待处理数量
const loadPendingCounts = async () => {
  try {
    // 获取待发货订单数
    const orderRes = await listOrders({ pageNum: 1, pageSize: 50, orderStatus: 2 });
    pendingOrderCount.value = orderRes?.total || 0;
    
    // 获取售后待办数量（仅统计当前需要商家操作的状态）
    const [pendingAfterSaleRes, processingAfterSaleRes, waitUserReturnRes, adminRes] = await Promise.all([
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.PENDING_MERCHANT }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_MERCHANT_REFUND }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.WAIT_USER_RETURN }),
      listAfterSale({ pageNum: 1, pageSize: 50, afterSaleStatus: AFTER_SALE_STATUS.ADMIN })
    ]);
    pendingAfterSaleCount.value =
      (pendingAfterSaleRes?.total || 0) +
      (processingAfterSaleRes?.total || 0);

    // 消息中心未读：订单沟通按本地已读记录，售后沟通按后端未读接口
    const merchantId = userStore.userInfo?.id || 'anonymous';
    const readMapKey = `${READ_STORAGE_PREFIX}_${merchantId}`;
    const readMap = JSON.parse(localStorage.getItem(readMapKey) || '{}') as Record<string, boolean>;
    const orderIds = (orderRes?.list || []).map((order: any) => `order-${order.id}`);
    const unreadOrderCount = orderIds.filter(id => !readMap[id]).length;
    const afterSaleList = [
      ...(pendingAfterSaleRes?.list || []),
      ...(processingAfterSaleRes?.list || []),
      ...(waitUserReturnRes?.list || []),
      ...(adminRes?.list || [])
    ];
    const afterSaleNos = [...new Set(afterSaleList.map((item: any) => String(item?.afterSaleNo || '')).filter(Boolean))].slice(0, 30);
    const unreadAfterSaleFlags: number[] = await Promise.all(afterSaleNos.map(async (no) => {
      try {
        const count = await getAfterSaleUnreadCount(no, 2);
        return Number(count || 0) > 0 ? 1 : 0;
      } catch {
        return 0;
      }
    }));
    const unreadAfterSaleCount = unreadAfterSaleFlags.reduce<number>((sum, value) => sum + Number(value || 0), 0);
    messageUnreadCount.value = unreadOrderCount + unreadAfterSaleCount;
  } catch (error) {
    console.error('Failed to load pending counts', error);
  }
};

const syncAuditStatus = async () => {
  try {
    const data = await getMerchantAuditStatus();
    if (Number(data?.status) === 2) {
      ElMessage.error('店铺已被禁用，请联系管理员处理');
      userStore.logout();
      router.replace('/merchant/login');
      return;
    }
    if (Number(data?.auditStatus) === 2) {
      const reason = data?.rejectReason ? `，原因：${data.rejectReason}` : '';
      ElMessage.warning(`商家资质审核未通过${reason}`);
    }
  } catch (error) {
    console.error('Failed to sync merchant audit status', error);
  }
};

const handleRefreshBadge = () => {
  void loadPendingCounts();
};

const handleReadRefresh = () => {
  void loadPendingCounts();
};

const clearReconnectTimer = () => {
  if (!reconnectTimer) return;
  clearTimeout(reconnectTimer);
  reconnectTimer = null;
};

const closeRealtime = () => {
  clearReconnectTimer();
  if (socket) {
    socket.close();
    socket = null;
  }
};

const scheduleReconnect = () => {
  clearReconnectTimer();
  socketConnected.value = false;
  reconnectTimer = setTimeout(() => {
    if (manualClosed) return;
    initRealtime();
  }, 2000);
};

const initRealtime = () => {
  closeRealtime();
  if (!userStore.token || userStore.role !== 'merchant') return;
  socket = new WebSocket(buildRealtimeWsUrl(userStore.token));

  socket.onmessage = (event) => {
    const payload = parseRealtimePayload(event as MessageEvent);
    dispatchRealtimeRefresh(MERCHANT_REALTIME_EVENT, payload);
    void loadPendingCounts();
    window.dispatchEvent(new Event('merchant-pending-refresh'));
  };

  socket.onopen = () => {
    socketConnected.value = true;
    dispatchRealtimeRefresh(MERCHANT_REALTIME_EVENT, { reason: 'CONNECTED', timestamp: Date.now() });
    void loadPendingCounts();
  };

  socket.onclose = () => {
    socketConnected.value = false;
    if (!manualClosed) {
      scheduleReconnect();
    }
  };

  socket.onerror = () => {
    socketConnected.value = false;
    if (socket?.readyState !== WebSocket.OPEN) {
      scheduleReconnect();
    }
  };
};

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/merchant/shop');
      break;
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout();
        ElMessage.success('已退出登录');
        router.push('/merchant/login');
      });
      break;
  }
};

onMounted(() => {
  manualClosed = false;
  void syncAuditStatus();
  void loadPendingCounts();
  initRealtime();
  window.addEventListener('merchant-pending-refresh', handleRefreshBadge);
  window.addEventListener('merchant-message-read-refresh', handleReadRefresh);
  // 实时链路断开时才启用轮询兜底，避免正常在线时重复请求
  refreshTimer = setInterval(() => {
    if (!socketConnected.value) {
      void loadPendingCounts();
    }
  }, 30000);
});

onUnmounted(() => {
  manualClosed = true;
  closeRealtime();
  window.removeEventListener('merchant-pending-refresh', handleRefreshBadge);
  window.removeEventListener('merchant-message-read-refresh', handleReadRefresh);
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
});
</script>

<style scoped lang="scss">
.merchant-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 210px;
  background: #304156;
  transition: width 0.3s;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;

  &.collapsed {
    width: 64px;
  }

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    border-bottom: 1px solid #1f2d3d;

    .logo-text {
      color: #fff;
      font-size: 18px;
      font-weight: bold;
    }
  }

  .merchant-menu {
    border-right: none;

    :deep(.el-menu-item) {
      position: relative;
    }

    .menu-dot {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      min-width: 20px;
      height: 20px;
      padding: 0 6px;
      border-radius: 999px;
      background: #f56c6c;
      color: #fff;
      font-size: 12px;
      line-height: 20px;
      text-align: center;
      font-weight: 600;
      border: 2px solid rgba(255, 255, 255, 0.85);
      box-sizing: border-box;
      pointer-events: none;
    }
  }
}

.main-container {
  flex: 1;
  margin-left: 210px;
  transition: margin-left 0.3s;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止内容撑开 */

  .sidebar.collapsed + & {
    margin-left: 64px;
  }
}

.header {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 99;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: background 0.2s;

      &:hover {
        background: #f5f7fa;
      }

      .username {
        font-size: 14px;
        color: #606266;
      }
    }
  }
}

.main-content {
  flex: 1;
  padding: 20px;
  background: #f0f2f5;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);

    &.open {
      transform: translateX(0);
    }
  }

  .main-container {
    margin-left: 0;
  }
}
</style>
