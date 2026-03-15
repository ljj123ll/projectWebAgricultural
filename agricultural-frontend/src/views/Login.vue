<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>系统登录</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" class="login-tabs" @tab-click="handleTabClick">
        <el-tab-pane label="用户登录" name="user"></el-tab-pane>
        <el-tab-pane label="商家登录" name="merchant"></el-tab-pane>
        <el-tab-pane label="管理员登录" name="admin"></el-tab-pane>
      </el-tabs>

      <el-form :model="loginForm" label-width="80px" :rules="rules" ref="loginFormRef">
        <el-form-item :label="activeTab === 'admin' ? '账号' : '手机号'" prop="account">
          <el-input 
            v-model="loginForm.account" 
            :placeholder="activeTab === 'admin' ? '请输入管理员账号' : '请输入手机号'"
            @keyup.enter="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%">登录</el-button>
        </el-form-item>
        <div class="login-options" v-if="activeTab !== 'admin'">
          <el-link type="primary" @click="$router.push(activeTab === 'merchant' ? '/merchant/register' : '/register')">
            注册{{ activeTab === 'merchant' ? '商家' : '用户' }}账号
          </el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage } from 'element-plus';
import { loginPassword } from '@/apis/user';
import { merchantLogin } from '@/apis/merchant';
import { adminLogin } from '@/apis/admin';
import type { FormInstance, FormRules } from 'element-plus';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);
const loginFormRef = ref<FormInstance>();
const activeTab = ref('user');

onMounted(() => {
  const role = route.query.role as string;
  if (role && ['user', 'merchant', 'admin'].includes(role)) {
    activeTab.value = role;
  }
});

const loginForm = reactive({
  account: '',
  password: ''
});

const rules = reactive<FormRules>({
  account: [{ required: true, message: '请输入账号/手机号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
});

const handleTabClick = () => {
  loginFormRef.value?.resetFields();
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        let res: any;
        let redirectPath = '';
        let role = '';

        if (activeTab.value === 'user') {
          // 用户登录
          res = await loginPassword({
            phone: loginForm.account,
            password: loginForm.password
          });
          role = 'user';
          redirectPath = '/home';
        } else if (activeTab.value === 'merchant') {
          // 商家登录
          res = await merchantLogin({
            phone: loginForm.account,
            password: loginForm.password
          });
          role = 'merchant';
          redirectPath = '/merchant/dashboard';
        } else if (activeTab.value === 'admin') {
          // 管理员登录
          res = await adminLogin({
            username: loginForm.account,
            password: loginForm.password
          });
          role = 'admin';
          redirectPath = '/admin/dashboard';
        }

        if (res) {
          // 确保 role 存在于 userInfo 中，如果后端没返回 role，手动补上
          const userInfo = { ...res.userInfo, role: res.userInfo?.role || role };
          
          userStore.setLoginState({
            token: res.token,
            userInfo: userInfo
          }, { remember: true });
          
          ElMessage.success('登录成功');
          router.push(redirectPath);
        }
      } catch (error: any) {
        console.error('Login error:', error);
        // Error handling is done in request interceptor usually, but here just in case
      } finally {
        loading.value = false;
      }
    }
  });
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
  background-image: url('https://images.unsplash.com/photo-1500382017468-9049fed747ef?ixlib=rb-1.2.1&auto=format&fit=crop&w=1920&q=80');
  background-size: cover;
  background-position: center;
}
.login-card {
  width: 450px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background-color: rgba(255, 255, 255, 0.95);
}
.card-header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  color: #333;
}
.login-tabs {
  margin-bottom: 20px;
}
.login-options {
  display: flex;
  justify-content: flex-end;
  margin-top: -10px;
}
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
:deep(.el-tabs__item) {
  font-size: 16px;
}
</style>
