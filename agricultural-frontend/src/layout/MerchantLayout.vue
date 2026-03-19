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
          <el-badge v-if="pendingOrderCount > 0" :value="pendingOrderCount > 99 ? '99+' : pendingOrderCount" class="menu-badge" />
        </el-menu-item>

        <el-menu-item index="/merchant/after-sales">
          <el-icon><Service /></el-icon>
          <template #title>售后处理</template>
          <el-badge v-if="pendingAfterSaleCount > 0" :value="pendingAfterSaleCount > 99 ? '99+' : pendingAfterSaleCount" class="menu-badge" />
        </el-menu-item>

        <el-menu-item index="/merchant/statistics">
          <el-icon><TrendCharts /></el-icon>
          <template #title>数据统计</template>
        </el-menu-item>

        <el-menu-item index="/merchant/messages">
          <el-icon><Bell /></el-icon>
          <template #title>消息中心</template>
          <el-badge v-if="totalUnreadCount > 0" :value="totalUnreadCount > 99 ? '99+' : totalUnreadCount" class="menu-badge" />
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
          <el-badge :value="totalUnreadCount" :hidden="totalUnreadCount === 0">
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
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listOrders, listAfterSale } from '@/apis/merchant';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isCollapsed = ref(false);
const pendingOrderCount = ref(0);
const pendingAfterSaleCount = ref(0);

// 总未读数
const totalUnreadCount = computed(() => {
  return pendingOrderCount.value + pendingAfterSaleCount.value;
});

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
};

// 加载待处理数量
const loadPendingCounts = async () => {
  try {
    // 获取待发货订单数
    const orderRes = await listOrders({ pageNum: 1, pageSize: 1, orderStatus: 2 });
    pendingOrderCount.value = orderRes?.total || 0;
    
    // 获取待处理售后数
    const afterSaleRes = await listAfterSale({ pageNum: 1, pageSize: 1, afterSaleStatus: 0 });
    pendingAfterSaleCount.value = afterSaleRes?.total || 0;
  } catch (error) {
    console.error('Failed to load pending counts', error);
  }
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
  loadPendingCounts();
  // 每30秒刷新一次待处理数量
  setInterval(loadPendingCounts, 30000);
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

    .menu-badge {
      position: absolute;
      right: 20px;
      top: 50%;
      transform: translateY(-50%);
    }
  }
}

.main-container {
  flex: 1;
  margin-left: 210px;
  transition: margin-left 0.3s;
  display: flex;
  flex-direction: column;

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
