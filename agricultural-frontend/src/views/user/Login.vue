<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <el-icon size="48" color="#67c23a"><Shop /></el-icon>
        <h1>助农电商</h1>
        <p>用户登录</p>
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
      >
        <!-- 手机号 -->
        <el-form-item prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入手机号"
            maxlength="11"
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
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
              <el-button 
                type="primary" 
                :disabled="codeCountdown > 0"
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
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </template>

        <!-- 记住登录态 -->
        <el-form-item>
          <el-checkbox v-model="rememberMe">7天自动登录</el-checkbox>
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
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <el-button link type="primary" @click="router.push('/register')">
          立即注册
        </el-button>
      </div>

      <!-- 其他登录方式 -->
      <div class="other-login">
        <el-divider>其他方式</el-divider>
        <div class="login-icons">
          <el-button circle size="large">
            <el-icon size="24"><ChatDotRound /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { loginPassword, loginSms, sendSms } from '@/apis/user';

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
        const redirect = route.query.redirect as string;
        router.push(redirect || '/');
      } catch (error) {
        console.error(error);
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  h1 {
    font-size: 28px;
    color: #67c23a;
    margin: 16px 0 8px;
  }

  p {
    font-size: 16px;
    color: #909399;
    margin: 0;
  }
}

.login-tabs {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 2px solid #e4e7ed;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 12px;
    font-size: 16px;
    color: #606266;
    cursor: pointer;
    transition: all 0.3s;
    position: relative;

    &.active {
      color: #67c23a;
      font-weight: 500;

      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 50%;
        transform: translateX(-50%);
        width: 40px;
        height: 3px;
        background: #67c23a;
        border-radius: 2px;
      }
    }
  }
}

.login-form {
  .code-input {
    display: flex;
    gap: 12px;

    .el-input {
      flex: 1;
    }

    .el-button {
      width: 120px;
    }
  }

  .forgot-link {
    float: right;
  }

  .login-btn {
    width: 100%;
    height: 48px;
    font-size: 18px;
    font-weight: 500;
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  color: #606266;
}

.other-login {
  margin-top: 32px;

  .login-icons {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-top: 16px;
  }
}

@media (max-width: 768px) {
  .login-container {
    padding: 32px 24px;
  }

  .login-header {
    h1 {
      font-size: 24px;
    }
  }

  .login-tabs {
    .tab-item {
      font-size: 15px;
    }
  }
}
</style>
