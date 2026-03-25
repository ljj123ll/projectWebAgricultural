<template>
  <div class="user-layout">
    <!-- 顶部导航 -->
    <header class="user-header">
      <div class="header-content">
        <div class="logo" @click="router.push('/home')">
          <el-icon size="28" color="#67c23a"><Shop /></el-icon>
          <span class="logo-text">助农电商</span>
        </div>
        
        <!-- 中间导航菜单 -->
        <div class="main-nav hidden-xs-only">
          <router-link to="/home" class="nav-link" active-class="active">首页</router-link>
          <router-link to="/products" class="nav-link" active-class="active">全部商品</router-link>
          <router-link to="/news" class="nav-link" active-class="active">助农新闻</router-link>
        </div>

        <div class="header-actions">
          <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0" class="cart-badge">
            <el-button circle @click="router.push('/cart')">
              <el-icon size="20"><ShoppingCart /></el-icon>
            </el-button>
          </el-badge>
          <template v-if="userStore.token">
            <el-dropdown @command="handleCommand">
              <el-avatar :size="36" :src="userStore.userInfo?.avatarUrl" class="user-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="reviews">我的评价</el-dropdown-item>
                  <el-dropdown-item command="messages">我的消息</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-tag type="info" effect="plain" style="margin-right: 12px; font-size: 14px; padding: 6px 10px;">
              未登录
            </el-tag>
            <el-button type="primary" size="default" @click="router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="user-main">
      <router-view />
    </main>

    <!-- 底部导航（移动端） -->
    <nav class="mobile-nav">
      <div 
        class="nav-item" 
        :class="{ active: route.path === '/home' }"
        @click="router.push('/home')"
      >
        <el-icon size="24"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: route.path === '/cart' }"
        @click="router.push('/cart')"
      >
        <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0">
          <el-icon size="24"><ShoppingCart /></el-icon>
        </el-badge>
        <span>购物车</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: route.path === '/orders' }"
        @click="router.push('/orders')"
      >
        <el-icon size="24"><Document /></el-icon>
        <span>订单</span>
      </div>
      <div 
        class="nav-item" 
        :class="{ active: route.path === '/profile' }"
        @click="router.push('/profile')"
      >
        <el-icon size="24"><User /></el-icon>
        <span>我的</span>
      </div>
    </nav>

    <!-- 底部信息 -->
    <footer class="user-footer">
      <p>助农电商平台 - 助力乡村振兴</p>
      <p class="footer-links">
        <span @click="router.push('/news')">助农新闻</span>
        <el-divider direction="vertical" />
        <span>关于我们</span>
        <el-divider direction="vertical" />
        <span>联系方式</span>
      </p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { useCartStore } from '@/stores/modules/cart';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const cartStore = useCartStore();

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'orders':
      router.push('/orders');
      break;
    case 'reviews':
      router.push('/reviews');
      break;
    case 'messages':
      router.push('/messages');
      break;
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        userStore.logout();
        ElMessage.success('已退出登录');
        router.push('/');
      });
      break;
  }
};
</script>

<style scoped lang="scss">
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.user-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;

  .header-content {
    max-width: 1200px;
    margin: 0 auto;
    padding: 12px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .logo-text {
      font-size: 20px;
      font-weight: bold;
      color: #67c23a;
    }
  }

  .main-nav {
    display: flex;
    gap: 30px;
    
    .nav-link {
      font-size: 16px;
      color: #303133;
      text-decoration: none;
      font-weight: 500;
      position: relative;
      padding-bottom: 4px;
      
      &:hover {
        color: #67c23a;
      }
      
      &.active {
        color: #67c23a;
        
        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          width: 100%;
          height: 2px;
          background-color: #67c23a;
          border-radius: 2px;
        }
      }
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 16px;

    .cart-badge {
      :deep(.el-badge__content) {
        font-size: 12px;
      }
    }

    .user-avatar {
      cursor: pointer;
      border: 2px solid #67c23a;
    }
  }
}

.user-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 16px;
}

.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  padding: 8px 0;
  z-index: 100;

  .nav-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    color: #909399;
    cursor: pointer;
    font-size: 12px;

    &.active {
      color: #67c23a;
    }
  }
}

.user-footer {
  background: #fff;
  padding: 24px 16px;
  text-align: center;
  color: #909399;
  font-size: 14px;

  .footer-links {
    margin-top: 12px;

    span {
      cursor: pointer;

      &:hover {
        color: #67c23a;
      }
    }
  }
}

@media (max-width: 768px) {
  .user-header {
    .header-content {
      padding: 10px 12px;
    }

    .logo-text {
      font-size: 18px !important;
    }
  }

  .user-main {
    padding: 12px;
    padding-bottom: 70px;
  }

  .mobile-nav {
    display: flex;
  }

  .user-footer {
    padding-bottom: 80px;
  }
}
</style>
