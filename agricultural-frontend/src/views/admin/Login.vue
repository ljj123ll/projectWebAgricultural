<template>
  <div class="admin-login-page">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <el-icon size="32" color="#fff"><Setting /></el-icon>
          <span>管理控制台</span>
        </div>
      </div>
      
      <div class="login-content">
        <h2 class="welcome-title">系统登录</h2>
        <p class="welcome-desc">助农电商平台 · 后台管理系统</p>

        <el-alert
          title="非管理员请勿尝试登录"
          type="info"
          show-icon
          :closable="false"
          class="security-notice"
        />

        <el-form 
          ref="formRef"
          :model="formData"
          :rules="rules"
          class="login-form"
          size="large"
          hide-required-asterisk
        >
          <el-form-item prop="username">
            <el-input
              v-model="formData.username"
              placeholder="管理员账号"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="登录密码"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
              v-model="formData.phone"
              placeholder="安全手机号"
              maxlength="11"
            >
              <template #prefix>
                <el-icon><Iphone /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="code">
            <div class="code-input">
              <el-input
                v-model="formData.code"
                placeholder="动态验证码"
                maxlength="6"
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
              <el-button 
                class="code-btn"
                :disabled="codeCountdown > 0"
                @click="sendCode"
              >
                {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              安全登录
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="login-footer">
        <p>&copy; 2024 助农电商平台 · 技术支持</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { adminLogin, adminSendSms } from '@/apis/admin';
import { Setting, User, Lock, Iphone, Message } from '@element-plus/icons-vue';

const router = useRouter();
const userStore = useUserStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const codeCountdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const formData = reactive({
  username: '',
  password: '',
  phone: '',
  code: ''
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
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
};

const sendCode = async () => {
  if (!formData.phone) {
    ElMessage.warning('请先输入手机号');
    return;
  }
  
  try {
    await adminSendSms(formData.phone);
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

const handleLogin = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const res: any = await adminLogin(formData);
        if (!res.userInfo.role) {
          res.userInfo.role = 'admin';
        }
        userStore.setLoginState(res);
        ElMessage.success('登录成功');
        router.push('/admin/dashboard');
      } catch (error) {
        console.error(error);
      } finally {
        loading.value = false;
      }
    }
  });
};

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped lang="scss">
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2d3a4b;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    background: radial-gradient(circle at center, #3a4b5f 0%, #2d3a4b 100%);
    opacity: 0.5;
  }
}

.login-box {
  position: relative;
  width: 420px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.login-header {
  background: rgba(0, 0, 0, 0.2);
  padding: 20px;
  
  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    letter-spacing: 1px;
  }
}

.login-content {
  padding: 40px;
  background: #fff;
}

.welcome-title {
  text-align: center;
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
  font-weight: 600;
}

.welcome-desc {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 24px;
}

.security-notice {
  margin-bottom: 24px;
  background: #f4f4f5;
  border: none;
}

.login-form {
  :deep(.el-input__wrapper) {
    background: #f5f7fa;
    box-shadow: none;
    border: 1px solid transparent;
    transition: all 0.3s;
    
    &:hover, &.is-focus {
      background: #fff;
      border-color: #409eff;
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }

  .code-input {
    display: flex;
    gap: 12px;

    .code-btn {
      width: 110px;
      background: #f5f7fa;
      border: none;
      color: #606266;
      
      &:hover {
        color: #409eff;
        background: #ecf5ff;
      }
    }
  }

  .login-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
    font-weight: 500;
    margin-top: 10px;
    background: #2d3a4b;
    border-color: #2d3a4b;
    
    &:hover {
      background: #3a4b5f;
      border-color: #3a4b5f;
    }
  }
}

.login-footer {
  text-align: center;
  padding: 15px;
  background: #f5f7fa;
  border-top: 1px solid #ebeef5;
  
  p {
    margin: 0;
    font-size: 12px;
    color: #909399;
  }
}

@media (max-width: 480px) {
  .login-box {
    width: 100%;
    margin: 20px;
  }
  
  .login-content {
    padding: 30px 20px;
  }
}
</style>
