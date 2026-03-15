<template>
  <div id="site-header" :class="{ 'scrolled': isScrolled }">
    <div class="navbar-container">
      <!-- Logo -->
      <div class="navbar-header">
        <router-link to="/" class="navbar-brand">
          <span class="navbar-brand-logo">助农</span>
        </router-link>
      </div>

      <!-- Navigation -->
      <div class="site-header-navbar">
        <ul class="navbar-category">
          <li class="category-item" v-for="(item, index) in navItems" :key="index" :class="{ 'active': activeIndex === index }">
            <router-link :to="getRoutePath(index)" class="nav-item-title" @click="setActive(index)">
              {{ item }}
            </router-link>
          </li>
        </ul>

        <ul class="navbar-right">
          <!-- Search -->
          <li class="nav-search">
             <a href="#" class="nav-link-search" @click.prevent>
                 <el-icon :size="20"><Search /></el-icon>
             </a>
             <div class="search-bar">
                 <input type="text" placeholder="搜索商品..." v-model="keyword" @keyup.enter="handleSearch">
                 <button class="search-submit" @click="handleSearch">搜索</button>
             </div>
          </li>

          <!-- User -->
          <li class="nav-user">
            <router-link :to="userStore.token ? '/user/profile' : '/login'" class="nav-link-user">
                <el-avatar v-if="userStore.token && userStore.userInfo?.avatarUrl" :size="32" :src="userStore.userInfo.avatarUrl" />
                <el-icon v-else :size="20"><User /></el-icon>
            </router-link>
          </li>

          <!-- Cart -->
          <li class="nav-cart">
             <router-link to="/user/cart" class="nav-link-cart">
                <el-icon :size="20"><ShoppingCart /></el-icon>
             </router-link>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { Search, User, ShoppingCart } from '@element-plus/icons-vue';

const navItems = ref(['首页', '商品浏览', '助农新闻', '个人中心']);
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const activeIndex = ref(0);
const isScrolled = ref(false);
const keyword = ref('');

const getRoutePath = (index: number) => {
  const paths = ['/', '/home', '/news', '/user/profile'];
  return paths[index] || '/';
};

const getRouteIndex = (path: string) => {
    if (path === '/' || path === '/home') return 0;
    if (path.startsWith('/product')) return 1;
    if (path.startsWith('/news')) return 2;
    if (path.startsWith('/user')) return 3;
    return 0;
};

const setActive = (index: number) => {
  activeIndex.value = index;
};

const handleScroll = () => {
  isScrolled.value = window.scrollY > 10;
};

const updateActiveIndex = () => {
    activeIndex.value = getRouteIndex(route.path);
};

const handleSearch = () => {
    if (keyword.value.trim()) {
        router.push({ path: '/home', query: { keyword: keyword.value } }); // Assuming home page has search
    }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll);
  updateActiveIndex();
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});

watch(() => route.path, () => {
    updateActiveIndex();
});
</script>

<style scoped lang="scss">
#site-header {
    background: #333; // Default dark background
    position: fixed;
    width: 100%;
    height: 64px;
    top: 0;
    left: 0;
    z-index: 900;
    transition: all 0.3s ease;
    color: white;

    &.scrolled {
        background: rgba(255, 255, 255, 0.95);
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        color: #333;

        .nav-item-title { color: #333 !important; }
        .nav-link-user, .nav-link-search, .nav-link-cart { color: #333 !important; }
    }

    .navbar-container {
        width: 100%;
        max-width: 1200px;
        margin: 0 auto;
        padding: 0 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 100%;
    }

    .navbar-brand-logo {
        font-size: 24px;
        font-weight: bold;
        color: inherit;
    }

    .site-header-navbar {
        display: flex;
        align-items: center;
        flex: 1;
        margin: 0 20px;

        .navbar-category {
            display: flex;
            gap: 20px;
            margin-left: 40px;

            .nav-item-title {
                color: white;
                font-size: 16px;
                padding: 0 10px;
                line-height: 64px;
                display: block;
                transition: color 0.3s;

                &:hover, &.router-link-active {
                    color: #409EFF;
                }
            }
        }

        .navbar-right {
            margin-left: auto;
            display: flex;
            align-items: center;
            gap: 20px;

            li {
                display: flex;
                align-items: center;
                cursor: pointer;
            }

            .nav-search {
                position: relative;
                
                .search-bar {
                    position: absolute;
                    right: 0;
                    top: 50%;
                    transform: translateY(-50%) translateX(100%);
                    opacity: 0;
                    visibility: hidden;
                    transition: all 0.3s ease;
                    background: white;
                    padding: 4px;
                    border-radius: 24px;
                    display: flex;
                    align-items: center;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.15);

                    input {
                        width: 200px;
                        border: none;
                        outline: none;
                        background: #f5f5f5;
                        padding: 5px 10px;
                        border-radius: 15px;
                    }

                    button {
                        border: none;
                        background: #409EFF;
                        color: white;
                        padding: 5px 15px;
                        border-radius: 15px;
                        margin-left: 5px;
                        cursor: pointer;
                    }
                }

                &:hover .search-bar {
                    transform: translateY(-50%) translateX(0);
                    opacity: 1;
                    visibility: visible;
                }
            }
        }
    }
}
</style>
