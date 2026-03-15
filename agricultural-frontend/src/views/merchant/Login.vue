<template>
  <div class="merchant-login-page">
    <div class="login-container">
      <div class="login-header">
        <el-icon size="48" color="#67c23a"><Shop /></el-icon>
        <h1>商家登录</h1>
        <p>助农电商平台商家端</p>
      </div>

      <el-form 
        ref="formRef"
        :model="formData"
        :rules="rules"
        class="login-form"
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

      <div class="back-link">
        <el-button link @click="router.push('/login')">
          <el-icon><ArrowLeft /></el-icon>
          返回用户端
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
import { merchantLogin } from '@/apis/merchant';

const router = useRouter();
const userStore = useUserStore();

const formRef = ref<FormInstance>();
const loading = ref(false);

const formData = reactive({
  phone: '',
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
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

const handleLogin = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const res: any = await merchantLogin(formData);
        // Ensure role is set
        if (!res.userInfo.role) {
          res.userInfo.role = 'merchant';
        }
        userStore.setLoginState(res);
        ElMessage.success('登录成功');
        router.push('/merchant/dashboard');
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
.merchant-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #304156 0%, #67c23a 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  h1 {
    font-size: 28px;
    color: #304156;
    margin: 16px 0 8px;
  }

  p {
    font-size: 16px;
    color: #909399;
    margin: 0;
  }
}

.login-form {
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

.back-link {
  text-align: center;
  margin-top: 16px;
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
