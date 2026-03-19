<template>
  <div class="landing-page">
    <!-- Header -->
    <header class="header">
      <div class="header-content">
        <div class="logo">
          <el-icon size="40" color="#d32f2f"><Shop /></el-icon>
          <div class="logo-text">
            <h1 class="main-title">脱贫地区农副产品网络销售平台</h1>
            <span class="sub-title">消费帮扶 · 政采同行</span>
          </div>
        </div>

      </div>
    </header>

    <!-- Main Banner Content -->
    <main class="main-content">
      <div class="slogan-container">
        <h2 class="slogan">致敬为乡村振兴贡献力量的您！</h2>
        <p class="sub-slogan">消费帮扶，感谢有您！让脱贫地区特色农副产品穿越四季，走出山野，恋过高原，进入万户千家。</p>
      </div>

      <div class="entrance-container">
        <!-- User Entrance -->
        <div class="entrance-card user-card" @click="goLogin('user')">
          <div class="icon-wrapper">
            <el-icon><User /></el-icon>
          </div>
          <div class="card-content">
            <h3>用户服务</h3>
            <p>面向个人消费者、企业采购</p>
            <el-button type="danger" round size="large">
              用户登录入口 >
            </el-button>
          </div>
        </div>

        <!-- Merchant Entrance -->
        <div class="entrance-card merchant-card" @click="goLogin('merchant')">
          <div class="icon-wrapper">
            <el-icon><Shop /></el-icon>
          </div>
          <div class="card-content">
            <h3>商家服务</h3>
            <p>面向农副产品供应商、合作社</p>
            <el-button type="primary" round size="large">
              商家登录入口 >
            </el-button>
          </div>
        </div>

        <!-- Visitor Entrance -->
        <div class="entrance-card visitor-card">
          <div class="icon-wrapper">
            <el-icon><View /></el-icon>
          </div>
          <div class="card-content">
            <h3>游客访问</h3>
            <p>无需登录，浏览商品</p>
            <el-button type="success" round size="large" @click="goHome">
              立即浏览 >
            </el-button>
          </div>
        </div>
      </div>
    </main>

    <!-- Footer Features -->
    <footer class="footer-features">
      <div class="feature-item">
        <el-icon color="#d32f2f" size="32"><Aim /></el-icon>
        <div class="text">
          <h4>精准帮扶</h4>
          <p>帮扶无止境</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon color="#d32f2f" size="32"><Service /></el-icon>
        <div class="text">
          <h4>售后无忧</h4>
          <p>平台售后服务</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon color="#d32f2f" size="32"><Box /></el-icon>
        <div class="text">
          <h4>产地保证</h4>
          <p>甄选优质产地</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon color="#d32f2f" size="32"><CreditCard /></el-icon>
        <div class="text">
          <h4>双向信用</h4>
          <p>平台无忧服务</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Shop, User, Aim, Service, Box, CreditCard, View } from '@element-plus/icons-vue';

const router = useRouter();

const goLogin = (role: 'user' | 'merchant') => {
  if (role === 'user') {
    router.push('/login');
  } else {
    router.push('/merchant/login');
  }
};

const goHome = () => {
  router.push('/home');
};
</script>

<style scoped lang="scss">
.landing-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-image: url('https://images.unsplash.com/photo-1500382017468-9049fed747ef?ixlib=rb-1.2.1&auto=format&fit=crop&w=1920&q=80');
  background-size: cover;
  background-position: center;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0, 0, 0, 0.2); 
    z-index: 0;
  }
}

.header {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  
  .header-content {
    max-width: 1200px;
    margin: 0 auto;
    padding: 15px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 12px;

    .logo-text {
      .main-title {
        margin: 0;
        font-size: 22px;
        color: #333;
        font-weight: bold;
      }
      .sub-title {
        font-size: 12px;
        color: #999;
        letter-spacing: 2px;
      }
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 14px;
    
    a {
      text-decoration: none;
      color: #666;
      &:hover { color: #d32f2f; }
    }
  }
}

.main-content {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #fff;
}

.slogan-container {
  text-align: center;
  margin-bottom: 60px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);

  .slogan {
    font-size: 48px;
    margin-bottom: 20px;
    font-weight: bold;
  }

  .sub-slogan {
    font-size: 20px;
    max-width: 800px;
    margin: 0 auto;
    line-height: 1.6;
    opacity: 0.9;
  }
}

.entrance-container {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
  justify-content: center;

  .entrance-card {
    width: 400px;
    height: 220px;
    border-radius: 12px;
    padding: 30px;
    display: flex;
    align-items: center;
    gap: 20px;
    transition: transform 0.3s, box-shadow 0.3s;
    cursor: pointer;
    backdrop-filter: blur(10px);

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 30px rgba(0,0,0,0.2);
    }

    .icon-wrapper {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: rgba(255,255,255,0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40px;
      color: #fff;
    }

    .card-content {
      flex: 1;
      
      h3 {
        font-size: 28px;
        margin: 0 0 10px;
        font-weight: bold;
      }
      
      p {
        font-size: 14px;
        opacity: 0.9;
        margin-bottom: 20px;
        min-height: 40px;
      }
    }

    &.user-card {
      background: linear-gradient(135deg, rgba(239, 83, 80, 0.9) 0%, rgba(211, 47, 47, 0.9) 100%);
    }

    &.merchant-card {
      background: linear-gradient(135deg, rgba(66, 165, 245, 0.9) 0%, rgba(25, 118, 210, 0.9) 100%);
    }

    &.visitor-card {
      background: linear-gradient(135deg, rgba(102, 187, 106, 0.9) 0%, rgba(56, 142, 60, 0.9) 100%);
    }
  }
}

.footer-features {
  position: relative;
  z-index: 1;
  background: #fff;
  padding: 30px 0;
  display: flex;
  justify-content: center;
  gap: 80px;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.05);

  .feature-item {
    display: flex;
    align-items: center;
    gap: 15px;

    .text {
      h4 {
        margin: 0 0 5px;
        font-size: 16px;
        color: #333;
      }
      p {
        margin: 0;
        font-size: 12px;
        color: #999;
      }
    }
  }
}

@media (max-width: 768px) {
  .slogan-container {
    .slogan { font-size: 32px; }
    .sub-slogan { font-size: 16px; }
  }
  
  .entrance-container {
    flex-direction: column;
    align-items: center;
    
    .entrance-card {
      width: 100%;
      max-width: 340px;
    }
  }
  
  .footer-features {
    flex-wrap: wrap;
    gap: 30px;
    padding: 30px 20px;
  }
}
</style>