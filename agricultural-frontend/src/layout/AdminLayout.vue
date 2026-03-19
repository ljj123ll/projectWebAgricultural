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
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>

        <el-sub-menu index="/admin/users">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/admin/users">用户列表</el-menu-item>
          <el-menu-item index="/admin/merchants">商家审核</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon>
          <template #title>商品审核</template>
        </el-menu-item>

        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/payments">
          <el-icon><Wallet /></el-icon>
          <template #title>资金管控</template>
        </el-menu-item>

        <el-menu-item index="/admin/comments">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/news">
          <el-icon><Document /></el-icon>
          <template #title>新闻管理</template>
        </el-menu-item>

        <el-menu-item index="/admin/unsold">
          <el-icon><Warning /></el-icon>
          <template #title>滞销专区</template>
        </el-menu-item>

        <el-sub-menu index="/admin/system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统运维</span>
          </template>
          <el-menu-item index="/admin/logs">系统日志</el-menu-item>
          <el-menu-item index="/admin/backup">数据备份</el-menu-item>
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
import { ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const isCollapsed = ref(false);

const userRole = computed(() => {
  return userStore.userInfo?.role === 'admin' ? '超级管理员' : '内容管理员';
});

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value;
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
