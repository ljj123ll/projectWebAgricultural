<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <el-icon size="48" color="#67c23a"><Shop /></el-icon>
        <h1>用户注册</h1>
        <p>仅需手机号即可快速注册</p>
      </div>

      <el-form 
        ref="formRef"
        :model="formData"
        :rules="rules"
        class="register-form"
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

        <!-- 验证码 -->
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

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请设置密码"
            show-password
            maxlength="20"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 昵称（可选） -->
        <el-form-item prop="nickname">
          <el-input
            v-model="formData.nickname"
            placeholder="设置昵称（可选，默认显示手机号后四位）"
            maxlength="20"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <el-button link type="primary" @click="router.push('/login')">
          立即登录
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
import { register, sendSms } from '@/apis/user';

const router = useRouter();

const formRef = ref<FormInstance>();
const loading = ref(false);
const codeCountdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const formData = reactive({
  phone: '',
  code: '',
  nickname: '',
  password: '' // Added password field to reactive data, though not in form?
});

// The form in template doesn't seem to have password field, but registration usually requires one or defaults to something?
// Looking at the markdown:
// // 用户注册
// {
//   "phone": "13800138001",
//   "code": "123456",
//   "nickname": "测试用户",
//   "password": "123456"
// }
// The current template in Register.vue DOES NOT have a password field!
// I need to add a password field to the template as well.

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

// 注册
const handleRegister = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        await register(formData);
        ElMessage.success('注册成功，请登录');
        router.push('/login');
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
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}

.register-header {
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

.register-form {
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

  .register-btn {
    width: 100%;
    height: 48px;
    font-size: 18px;
    font-weight: 500;
  }

  :deep(.el-checkbox__label) {
    font-size: 14px;
  }
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  color: #606266;
}

@media (max-width: 768px) {
  .register-container {
    padding: 32px 24px;
  }

  .register-header {
    h1 {
      font-size: 24px;
    }
  }
}
</style>
