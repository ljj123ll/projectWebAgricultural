<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <el-icon size="32" color="#fff"><Setting /></el-icon>
        <span v-if="!isCollapsed" class="logo-text">管理后台</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        class="admin-menu"
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#fff"
      >
        <el-menu-item v-if="canAccess('dashboard:view')" index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>

        <el-sub-menu v-if="showUserManage" index="/admin/users">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item v-if="canAccess('user:manage')" index="/admin/users">用户列表</el-menu-item>
          <el-menu-item v-if="canAccess('merchant:manage')" index="/admin/merchants">商家审核</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="canAccess('audit:manage')" index="/admin/products">
          <el-icon><Goods /></el-icon>
          <template #title>商品审核</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('order:manage')" index="/admin/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('after_sale:manage')" index="/admin/after-sales">
          <el-icon><Service /></el-icon>
          <template #title>售后裁决</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('payment:manage')" index="/admin/payments">
          <el-icon><Wallet /></el-icon>
          <template #title>资金管控</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('order:manage')" index="/admin/risk-monitor">
          <el-icon><Warning /></el-icon>
          <template #title>风控监控</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('transfer:manage')" index="/admin/transfers">
          <el-icon><Wallet /></el-icon>
          <template #title>打款台账</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('merchant_account:audit')" index="/admin/merchant-accounts">
          <el-icon><Wallet /></el-icon>
          <template #title>收款账户审核</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('withdraw:audit')" index="/admin/withdrawals">
          <el-icon><Wallet /></el-icon>
          <template #title>提现审核</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('comment:manage')" index="/admin/comments">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('news:manage')" index="/admin/news">
          <el-icon><Document /></el-icon>
          <template #title>新闻管理</template>
        </el-menu-item>

        <el-menu-item v-if="canAccess('unsalable:manage')" index="/admin/unsold">
          <el-icon><Warning /></el-icon>
          <template #title>滞销专区</template>
        </el-menu-item>

        <el-sub-menu v-if="showSystemOps" index="/admin/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统运维</span>
          </template>
          <el-menu-item v-if="canAccess('role:manage')" index="/admin/roles">角色权限</el-menu-item>
          <el-menu-item v-if="canAccess('log:view')" index="/admin/logs">系统日志</el-menu-item>
          <el-menu-item v-if="canAccess('backup:manage')" index="/admin/backup">数据备份</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <div class="main-container" :class="{ 'sidebar-collapsed': isCollapsed }">
      <!-- 顶部导航 -->
      <header class="header">
        <div class="header-left">
          <el-button link @click="toggleSidebar">
            <el-icon size="20"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
          </el-button>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || '管理员' }}</span>
              <el-tag size="small" type="danger">{{ userRole }}</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getCurrentAdminPermissions } from '@/apis/admin';
import {
  ADMIN_REALTIME_EVENT,
  buildRealtimeWsUrl,
  dispatchRealtimeRefresh,
  parseRealtimePayload
} from '@/utils/realtime';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isCollapsed = ref(false);
const permissionCodes = ref<string[]>([]);
let socket: WebSocket | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
let manualClosed = false;

const canAccess = (permission?: string) => {
  if (!permission) return true;
  const roleCode = String(userStore.userInfo?.roleCode || '').toLowerCase();
  if (roleCode === 'super_admin' || roleCode === 'admin' || roleCode === 'root') {
    return true;
  }
  const codes = new Set(permissionCodes.value || []);
  if (codes.has('*') || codes.has(permission) || codes.has(permission.replace(':', '.'))) {
    return true;
  }
  const idx = permission.indexOf(':');
  if (idx > 0 && codes.has(`${permission.slice(0, idx)}:*`)) {
    return true;
  }
  if (['content_admin', 'content-manager', 'editor_admin'].includes(roleCode)) {
    return ['dashboard:view', 'audit:manage', 'comment:manage', 'news:manage'].includes(permission);
  }
  return false;
};

const showUserManage = computed(() => canAccess('user:manage') || canAccess('merchant:manage'));
const showSystemOps = computed(() => canAccess('role:manage') || canAccess('log:view') || canAccess('backup:manage'));

const userRole = computed(() => {
  return userStore.userInfo?.role === 'admin' ? '超级管理员' : '内容管理员';
});

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
};

const syncPermissions = async () => {
  if (!userStore.token || userStore.role !== 'admin') return;
  try {
    permissionCodes.value = await getCurrentAdminPermissions();
  } catch (error) {
    console.error('Failed to sync admin permissions', error);
    permissionCodes.value = userStore.userInfo?.permissionCodes || [];
  }
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
  reconnectTimer = setTimeout(() => {
    if (manualClosed) return;
    initRealtime();
  }, 2000);
};

const initRealtime = () => {
  closeRealtime();
  if (!userStore.token || userStore.role !== 'admin') return;

  socket = new WebSocket(buildRealtimeWsUrl(userStore.token));
  socket.onmessage = (event) => {
    dispatchRealtimeRefresh(ADMIN_REALTIME_EVENT, parseRealtimePayload(event as MessageEvent));
  };
  socket.onopen = () => {
    dispatchRealtimeRefresh(ADMIN_REALTIME_EVENT, { reason: 'CONNECTED', timestamp: Date.now() });
  };
  socket.onclose = () => {
    if (!manualClosed) {
      scheduleReconnect();
    }
  };
  socket.onerror = () => {
    if (socket?.readyState !== WebSocket.OPEN) {
      scheduleReconnect();
    }
  };
};

const handleCommand = (command: string) => {
  switch (command) {
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout();
        ElMessage.success('已退出登录');
        router.push('/admin/login');
      });
      break;
  }
};

onMounted(() => {
  manualClosed = false;
  void syncPermissions();
  initRealtime();
});

onUnmounted(() => {
  manualClosed = true;
  closeRealtime();
});
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 210px;
  background: #001529;
  transition: width 0.3s;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;

  &.collapsed {
    width: 64px;
  }

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    background: #002140;
    overflow: hidden; 

    .logo-text {
      color: #fff;
      font-size: 18px;
      font-weight: bold;
      white-space: nowrap; 
    }
  }

  .admin-menu {
    border-right: none;
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.22);
      border-radius: 999px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }
  }
}

.main-container {
  flex: 1;
  margin-left: 210px;
  transition: margin-left 0.3s;
  display: flex;
  flex-direction: column;
  width: calc(100% - 210px); 

  &.sidebar-collapsed {
    margin-left: 64px;
    width: calc(100% - 64px); 
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

  .header-right {
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
    width: 100%;
  }
}
</style>
