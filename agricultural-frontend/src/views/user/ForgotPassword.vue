<template>
  <div class="forgot-page">
    <div class="forgot-container">
      <div class="forgot-header">
        <el-icon size="48" color="#67c23a"><Key /></el-icon>
        <h1>找回密码</h1>
        <p>通过手机号验证码重置密码</p>
      </div>

      <el-steps :active="currentStep" finish-status="success" simple class="steps">
        <el-step title="验证手机号" />
        <el-step title="重置密码" />
        <el-step title="完成" />
      </el-steps>

      <!-- 第一步：验证手机号 -->
      <div v-if="currentStep === 0" class="step-content">
        <el-form 
          ref="formRef1"
          :model="formData"
          :rules="rules1"
          size="large"
        >
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

          <el-button 
            type="primary" 
            size="large" 
            class="submit-btn"
            @click="verifyPhone"
          >
            下一步
          </el-button>
        </el-form>
      </div>

      <!-- 第二步：重置密码 -->
      <div v-if="currentStep === 1" class="step-content">
        <el-form 
          ref="formRef2"
          :model="formData"
          :rules="rules2"
          size="large"
        >
          <el-form-item prop="newPassword">
            <el-input
              v-model="formData.newPassword"
              type="password"
              placeholder="设置新密码"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="确认新密码"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button 
            type="primary" 
            size="large" 
            class="submit-btn"
            :loading="loading"
            @click="resetPassword"
          >
            重置密码
          </el-button>
        </el-form>
      </div>

      <!-- 第三步：完成 -->
      <div v-if="currentStep === 2" class="step-content success-step">
        <el-icon size="64" color="#67c23a" class="success-icon"><CircleCheckFilled /></el-icon>
        <h2>密码重置成功</h2>
        <p>请使用新密码登录</p>
        <el-button 
          type="primary" 
          size="large" 
          class="submit-btn"
          @click="router.push('/login')"
        >
          去登录
        </el-button>
      </div>

      <div class="forgot-footer">
        <el-button link type="primary" @click="router.push('/login')">
          返回登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';

const router = useRouter();

const currentStep = ref(0);
const loading = ref(false);
const codeCountdown = ref(0);
const formRef1 = ref<FormInstance>();
const formRef2 = ref<FormInstance>();

const formData = reactive({
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
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

const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  void rule;
  if (!value) {
    callback(new Error('请确认密码'));
  } else if (value !== formData.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const rules1: FormRules = {
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
};

const rules2: FormRules = {
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
};

// 发送验证码
const sendCode = async () => {
  const phoneReg = /^1[3-9]\d{9}$/;
  if (!phoneReg.test(formData.phone)) {
    ElMessage.warning('请输入正确的手机号');
    return;
  }

  // 模拟发送验证码
  ElMessage.success('验证码已发送：123456');
  codeCountdown.value = 60;
  const timer = setInterval(() => {
    codeCountdown.value--;
    if (codeCountdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
};

// 验证手机号
const verifyPhone = async () => {
  if (!formRef1.value) return;

  await formRef1.value.validate((valid) => {
    if (valid) {
      if (formData.code !== '123456') {
        ElMessage.error('验证码错误');
        return;
      }
      currentStep.value = 1;
    }
  });
};

// 重置密码
const resetPassword = async () => {
  if (!formRef2.value) return;

  await formRef2.value.validate((valid) => {
    if (valid) {
      loading.value = true;
      setTimeout(() => {
        ElMessage.success('密码重置成功');
        loading.value = false;
        currentStep.value = 2;
      }, 1000);
    }
  });
};
</script>

<style scoped lang="scss">
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  padding: 20px;
}

.forgot-container {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}

.forgot-header {
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

.steps {
  margin-bottom: 32px;
}

.step-content {
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

  .submit-btn {
    width: 100%;
    height: 48px;
    font-size: 18px;
    font-weight: 500;
    margin-top: 16px;
  }

  &.success-step {
    text-align: center;
    padding: 32px 0;

    .success-icon {
      margin-bottom: 16px;
    }

    h2 {
      font-size: 24px;
      color: #67c23a;
      margin: 0 0 8px;
    }

    p {
      color: #909399;
      margin: 0 0 24px;
    }
  }
}

.forgot-footer {
  text-align: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .forgot-container {
    padding: 32px 20px;
  }

  .forgot-header {
    h1 {
      font-size: 24px;
    }
  }
}
</style>
