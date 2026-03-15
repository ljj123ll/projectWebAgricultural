<template>
  <div class="admin-login-page">
    <div class="login-container">
      <div class="login-header">
        <el-icon size="48" color="#001529"><Setting /></el-icon>
        <h1>管理后台</h1>
        <p>助农电商平台管理员入口</p>
      </div>

      <el-alert
        title="安全提示"
        description="此页面仅限授权管理员访问"
        type="warning"
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
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入管理员账号"
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
            placeholder="请输入密码"
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
            placeholder="请输入手机号"
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
              placeholder="请输入短信验证码"
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

        <el-form-item>
          <el-button 
            type="danger" 
            size="large" 
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="back-link">
        <el-button link type="info" @click="router.push('/')">
          <el-icon><ArrowLeft /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { adminLogin, adminSendSms } from '@/apis/admin';

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
  code: [{ required: true, message: '请输入短信验证码', trigger: 'blur' }]
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
        // Ensure role is set
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
</script>

<style scoped lang="scss">
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #001529;
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;

  h1 {
    font-size: 28px;
    color: #001529;
    margin: 16px 0 8px;
  }

  p {
    font-size: 16px;
    color: #909399;
    margin: 0;
  }
}

.security-notice {
  margin-bottom: 24px;
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

  .login-btn {
    width: 100%;
    height: 48px;
    font-size: 18px;
    font-weight: 500;
  }
}

.back-link {
  text-align: center;
  margin-top: 24px;
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
}
</style>
