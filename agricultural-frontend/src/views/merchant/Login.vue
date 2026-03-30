<template>
  <div class="merchant-login-page">
    <!-- 返回首页按钮 -->
    <div class="back-btn-wrapper">
      <el-button link class="back-btn" @click="router.push('/')">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回首页</span>
      </el-button>
    </div>
    <div class="login-box">
      <div class="login-left">
        <div class="brand">
          <el-icon size="40"><Shop /></el-icon>
          <span>商家工作台</span>
        </div>
        <div class="welcome-text">
          <h2>数据驱动农业</h2>
          <p>连接消费者，优化供应链，实现精准助农</p>
        </div>
      </div>
      
      <div class="login-right">
        <div class="login-header">
          <h3>欢迎登录</h3>
        </div>

        <!-- 登录方式切换 -->
        <div class="login-tabs">
          <div 
            class="tab-item" 
            :class="{ active: loginType === 'code' }"
            @click="loginType = 'code'"
          >
            验证码登录
          </div>
          <div 
            class="tab-item" 
            :class="{ active: loginType === 'password' }"
            @click="loginType = 'password'"
          >
            密码登录
          </div>
        </div>

        <el-form 
          ref="formRef"
          :model="formData"
          :rules="rules"
          class="login-form"
          size="large"
          label-position="top"
        >
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model="formData.phone"
              placeholder="请输入手机号"
              maxlength="11"
            />
          </el-form-item>

          <!-- 验证码登录 -->
          <template v-if="loginType === 'code'">
            <el-form-item label="验证码" prop="code">
              <div class="code-input">
                <el-input
                  v-model="formData.code"
                  placeholder="请输入验证码"
                  maxlength="6"
                />
                <el-button 
                  type="primary" 
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
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="formData.password"
                type="password"
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>
          </template>

          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span>还没有店铺？</span>
          <el-button link type="primary" @click="router.push('/merchant/register')">
            立即入驻
          </el-button>
        </div>
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
import { merchantLogin, merchantSendSms } from '@/apis/merchant';
import { Shop, ArrowLeft } from '@element-plus/icons-vue';

const router = useRouter();
const userStore = useUserStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const loginType = ref<'code' | 'password'>('code');
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
    await merchantSendSms(formData.phone);
    ElMessage.success('验证码发送成功');
    if (timer) clearInterval(timer);
    codeCountdown.value = 60;
    timer = setInterval(() => {
      codeCountdown.value--;
      if (codeCountdown.value <= 0) {
        if (timer) {
          clearInterval(timer);
          timer = null;
        }
      }
    }, 1000);
  } catch (error) {
    console.error(error);
  }
};

const handleLogin = async () => {
  if (!formRef.value) {
    console.error('Form ref is null');
    return;
  }

  // 验证表单
  try {
    await formRef.value.validate();
  } catch (validationError) {
    return;
  }

  loading.value = true;
  try {
    const loginData = loginType.value === 'code' 
      ? { phone: formData.phone, code: formData.code }
      : { phone: formData.phone, password: formData.password };
    const res: any = await merchantLogin(loginData);

    const loginResult = {
      token: res.token,
      userInfo: {
        id: res.merchantId,
        nickname: res.merchantName,
        phone: formData.phone,
        role: 'merchant' as const
      }
    };
    userStore.setLoginState(loginResult);
    ElMessage.success('登录成功');
    router.push('/merchant/dashboard');
  } catch (error: any) {
    console.error('Login error:', error);
    if (error?.response?.data) {
      const responseData = error.response.data;
      if (responseData.code === 403) {
        const msg = responseData.msg || '';
        if (msg.includes('审核中') || msg.includes('未审核')) {
          ElMessage.warning('您的账号正在审核中，请耐心等待管理员审核');
        } else if (msg.includes('审核未通过') || msg.includes('驳回')) {
          ElMessage.error(msg);
        } else {
          ElMessage.error(msg || '登录失败');
        }
      } else if (responseData.code === 400) {
        ElMessage.error(responseData.msg || '请求参数错误');
      } else {
        ElMessage.error(responseData.msg || '登录失败，请检查账号密码');
      }
    } else if (error?.code === 403) {
      // 直接处理拦截器抛出的错误
      const msg = error?.message || '';
      if (msg.includes('审核中') || msg.includes('未审核')) {
        ElMessage.warning('您的账号正在审核中，请耐心等待管理员审核');
      } else if (msg.includes('审核未通过') || msg.includes('驳回')) {
        ElMessage.error(msg);
      } else {
        ElMessage.error(msg || '登录失败');
      }
    } else if (error?.message) {
      ElMessage.error(error.message);
    } else {
      ElMessage.error('登录失败，请稍后重试');
    }
  } finally {
    loading.value = false;
  }
};

// 清理定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<style scoped lang="scss">
.merchant-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at top right, rgba(255, 214, 112, 0.18), transparent 26%),
    radial-gradient(circle at left center, rgba(94, 155, 94, 0.22), transparent 28%),
    linear-gradient(135deg, #16302a 0%, #28493e 42%, #eef5eb 100%);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(180deg, rgba(9, 27, 22, 0.2) 0%, rgba(255, 255, 255, 0.05) 100%),
      repeating-linear-gradient(
        120deg,
        rgba(255, 255, 255, 0.035) 0,
        rgba(255, 255, 255, 0.035) 14px,
        transparent 14px,
        transparent 28px
      );
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
      color: #409eff;
    }

    .el-icon {
      margin-right: 4px;
    }
  }
}

.login-box {
  display: flex;
  width: 900px;
  height: 560px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(0,0,0,0.1);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1f2d3d 0%, #324157 100%);
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  
  .brand {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 20px;
    font-weight: 600;
  }
  
  .welcome-text {
    h2 {
      font-size: 32px;
      margin-bottom: 16px;
      font-weight: 600;
    }
    p {
      font-size: 16px;
      opacity: 0.8;
      line-height: 1.6;
    }
  }
}

.login-right {
  width: 420px;
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  
  .login-header {
    margin-bottom: 20px;
    h3 {
      font-size: 24px;
      color: #303133;
      font-weight: 600;
    }
  }

  // 登录方式切换
  .login-tabs {
    display: flex;
    margin-bottom: 20px;
    border-bottom: 1px solid #e4e7ed;

    .tab-item {
      flex: 1;
      text-align: center;
      padding: 12px 0;
      cursor: pointer;
      color: #606266;
      font-size: 14px;
      transition: all 0.3s;
      position: relative;

      &:hover {
        color: #409eff;
      }

      &.active {
        color: #409eff;
        font-weight: 500;

        &::after {
          content: '';
          position: absolute;
          bottom: -1px;
          left: 20%;
          right: 20%;
          height: 2px;
          background: #409eff;
          border-radius: 2px;
        }
      }
    }
  }
  
  .login-form {
    :deep(.el-input__wrapper) {
      box-shadow: 0 0 0 1px #dcdfe6 inset;
      &:hover {
        box-shadow: 0 0 0 1px #409eff inset;
      }
      &.is-focus {
        box-shadow: 0 0 0 1px #409eff inset;
      }
    }

    .code-input {
      display: flex;
      gap: 12px;

      .el-input {
        flex: 1;
      }

      .code-btn {
        width: 120px;
        font-size: 14px;
      }
    }
    
    .login-btn {
      width: 100%;
      margin-top: 10px;
      background: #324157;
      border-color: #324157;
      
      &:hover {
        background: #1f2d3d;
        border-color: #1f2d3d;
      }
    }
  }
  
  .login-footer {
    margin-top: 20px;
    text-align: center;
    font-size: 14px;
    color: #606266;
  }
}

@media (max-width: 900px) {
  .back-btn-wrapper {
    top: 10px;
    left: 10px;
  }

  .login-box {
    width: 100%;
    max-width: 420px;
    height: auto;
    margin: 60px 20px 20px;
    flex-direction: column;
  }
  
  .login-left {
    display: none;
  }
  
  .login-right {
    width: 100%;
    padding: 30px;
  }
}
</style>
