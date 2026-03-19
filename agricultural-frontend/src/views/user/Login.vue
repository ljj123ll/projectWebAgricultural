<template>
  <div class="login-page">
    <div class="bg-overlay"></div>
    <!-- 返回按钮 -->
    <div class="back-btn-wrapper">
      <el-button link class="back-btn" @click="router.push('/')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回首页</span>
      </el-button>
    </div>
    <div class="login-container">
      <div class="login-header">
        <div class="logo-wrapper">
          <img src="@/assets/vue.svg" alt="logo" />
        </div>
        <h1>田园优选</h1>
        <p class="subtitle">连接城乡，传递自然味道</p>
      </div>

      <!-- 登录方式切换 -->
      <div class="login-tabs">
        <div 
          class="tab-item" 
          :class="{ active: loginType === 'code' }"
          @click="loginType = 'code'"
        >
          <span>验证码登录</span>
        </div>
        <div 
          class="tab-item" 
          :class="{ active: loginType === 'password' }"
          @click="loginType = 'password'"
        >
          <span>密码登录</span>
        </div>
      </div>

      <el-form 
        ref="formRef"
        :model="formData"
        :rules="rules"
        class="login-form"
        size="large"
      >
        <!-- 手机号 -->
        <el-form-item prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入手机号"
            maxlength="11"
            class="custom-input"
          >
            <template #prefix>
              <el-icon><Iphone /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 验证码登录 -->
        <template v-if="loginType === 'code'">
          <el-form-item prop="code">
            <div class="code-input">
              <el-input
                v-model="formData.code"
                placeholder="请输入验证码"
                maxlength="6"
                class="custom-input"
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
              <el-button 
                type="primary" 
                plain
                :disabled="codeCountdown > 0"
                class="code-btn"
                @click="sendCode"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 密码登录 -->
        <template v-else>
          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              show-password
              class="custom-input"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </template>

        <!-- 记住登录态 -->
        <el-form-item class="remember-row">
          <el-checkbox v-model="rememberMe">
            <span class="remember-text">7天自动登录</span>
          </el-checkbox>
          <el-button 
            link 
            type="primary" 
            class="forgot-link"
            @click="router.push('/forgot-password')"
          >
            忘记密码？
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
            round
          >
            <el-icon v-if="!loading" style="margin-right: 5px"><User /></el-icon>
            <span>立即登录</span>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <div class="divider">
          <span>还没有账号？</span>
        </div>
        <el-button 
          type="success" 
          text
          bg
          size="large" 
          class="register-btn"
          @click="router.push('/register')"
          round
        >
          注册新用户
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { loginPassword, loginSms, sendSms } from '@/apis/user';
import { Shop, Message, Lock, Iphone, User, ArrowLeft } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const loginType = ref<'code' | 'password'>('code');
const rememberMe = ref(true);
const codeCountdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const formData = reactive({
  phone: '',
  code: '',
  password: ''
});

const validatePhone = (rule: any, value: string, callback: any) => {
  void rule;
  const phoneReg = /^1[3-9]\d{9}$/;
  if (!value) {
    callback(new Error('请输入手机号'));
  } else if (!phoneReg.test(value)) {
    callback(new Error('手机号格式不正确'));
  } else {
    callback();
  }
};

const rules: FormRules = {
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

// 发送验证码
const sendCode = async () => {
  const phoneReg = /^1[3-9]\d{9}$/;
  if (!phoneReg.test(formData.phone)) {
    ElMessage.warning('请输入正确的手机号');
    return;
  }

  try {
    await sendSms(formData.phone);
    ElMessage.success('验证码发送成功');
    codeCountdown.value = 60;
    timer = setInterval(() => {
      codeCountdown.value--;
      if (codeCountdown.value <= 0) {
        if (timer) clearInterval(timer);
      }
    }, 1000);
  } catch (error) {
    console.error(error);
  }
};

// 登录
const handleLogin = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        let res;
        if (loginType.value === 'code') {
           res = await loginSms({
             phone: formData.phone,
             code: formData.code
           });
        } else {
           res = await loginPassword({
             phone: formData.phone,
             password: formData.password
           });
        }
        
        // 如果后端没有返回 role，手动设置为 user
        if (!res.userInfo.role) {
          res.userInfo.role = 'user';
        }

        userStore.setLoginState(res, { remember: rememberMe.value });
        ElMessage.success('登录成功');
        // 登录成功后跳转到首页 /home
        await router.push('/home');
      } catch (error) {
        console.error(error);
      } finally {
        loading.value = false;
      }
    }
  });
};

// 清理定时器
onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: url('https://images.unsplash.com/photo-1500382017468-9049fed747ef?q=80&w=2832&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  position: relative;
  
  .bg-overlay {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.3);
    backdrop-filter: blur(2px);
  }
}

// 返回按钮
.back-btn-wrapper {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 10;

  .back-btn {
    color: #fff;
    font-size: 14px;
    
    &:hover {
      color: #67c23a;
    }

    .el-icon {
      margin-right: 4px;
    }
  }
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  padding: 40px 32px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  .logo-wrapper {
    width: 64px;
    height: 64px;
    margin: 0 auto 16px;
    background: #fff;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    
    img {
      width: 40px;
      height: 40px;
    }
  }

  h1 {
    font-size: 24px;
    font-weight: 600;
    color: #2c3e50;
    margin-bottom: 8px;
    letter-spacing: 1px;
  }

  .subtitle {
    font-size: 14px;
    color: #7f8c8d;
    font-weight: 400;
  }
}

.login-tabs {
  display: flex;
  margin-bottom: 28px;
  background: #f5f7fa;
  padding: 4px;
  border-radius: 12px;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 10px 0;
    cursor: pointer;
    color: #909399;
    font-size: 14px;
    transition: all 0.3s ease;
    border-radius: 8px;

    &.active {
      background: #fff;
      color: #409eff; // 使用更清新的蓝色或保持绿色
      color: #67c23a;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    }
  }
}

.login-form {
  .custom-input {
    :deep(.el-input__wrapper) {
      box-shadow: none;
      background: #f5f7fa;
      border: 1px solid transparent;
      border-radius: 12px;
      padding: 4px 12px;
      
      &.is-focus {
        background: #fff;
        border-color: #67c23a;
        box-shadow: 0 0 0 4px rgba(103, 194, 58, 0.1);
      }
    }
  }

  .code-input {
    display: flex;
    gap: 12px;

    .code-btn {
      width: 110px;
      border-radius: 12px;
      font-weight: 500;
    }
  }

  .remember-row {
    margin-bottom: 24px;
    
    .remember-text {
      color: #606266;
    }
    
    .forgot-link {
      color: #909399;
      &:hover {
        color: #67c23a;
      }
    }
  }

  .login-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 600;
    background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
    border: none;
    box-shadow: 0 8px 16px rgba(103, 194, 58, 0.3);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 20px rgba(103, 194, 58, 0.4);
    }
    
    &:active {
      transform: translateY(1px);
    }
  }
}

.login-footer {
  margin-top: 20px;
  text-align: center;

  .divider {
    display: flex;
    align-items: center;
    margin: 20px 0 16px;
    color: #c0c4cc;
    font-size: 12px;
    
    &::before, &::after {
      content: '';
      flex: 1;
      height: 1px;
      background: #ebeef5;
    }
    
    span {
      padding: 0 12px;
    }
  }

  .register-btn {
    width: 100%;
    font-weight: 600;
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 32px 24px;
    margin: 16px;
  }
}
</style>
