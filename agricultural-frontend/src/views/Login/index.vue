<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>欢迎登录</h2>
        <p>助农电商平台</p>
      </div>
      
      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="密码登录" name="password">
            <el-form :model="passwordForm" ref="passwordFormRef" :rules="passwordRules" label-width="0">
                <el-form-item prop="phone">
                    <el-input v-model="passwordForm.phone" placeholder="请输入手机号" :prefix-icon="Iphone" />
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="passwordForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" class="login-btn" @click="handlePasswordLogin" :loading="loading">登录</el-button>
                </el-form-item>
            </el-form>
        </el-tab-pane>
        <el-tab-pane label="验证码登录" name="sms">
            <el-form :model="smsForm" ref="smsFormRef" :rules="smsRules" label-width="0">
                <el-form-item prop="phone">
                    <el-input v-model="smsForm.phone" placeholder="请输入手机号" :prefix-icon="Iphone" />
                </el-form-item>
                <el-form-item prop="code">
                    <div class="sms-code-wrapper">
                        <el-input v-model="smsForm.code" placeholder="请输入验证码" :prefix-icon="Key" />
                        <el-button type="primary" :disabled="isCounting" @click="handleSendSms">
                            {{ isCounting ? `${count}s后重发` : '获取验证码' }}
                        </el-button>
                    </div>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" class="login-btn" @click="handleSmsLogin" :loading="loading">登录</el-button>
                </el-form-item>
            </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <router-link to="/register">注册账号</router-link>
        <span class="divider">|</span>
        <a href="#">忘记密码?</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage, type FormInstance } from 'element-plus';
import { Iphone, Lock, Key } from '@element-plus/icons-vue';
import { loginPassword, loginSms, sendSms } from '@/apis/user';

const router = useRouter();
const userStore = useUserStore();
const activeTab = ref('password');
const loading = ref(false);

// Password Login
const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive({ phone: '', password: '' });
const passwordRules = {
    phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

// SMS Login
const smsFormRef = ref<FormInstance>();
const smsForm = reactive({ phone: '', code: '' });
const smsRules = {
    phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
};

const isCounting = ref(false);
const count = ref(60);

const handlePasswordLogin = async () => {
    if (!passwordFormRef.value) return;
    await passwordFormRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true;
            try {
                const res = await loginPassword(passwordForm);
                if (res) {
                    userStore.setLoginState(res);
                    ElMessage.success('登录成功');
                    router.push('/');
                }
            } catch (e) {
                // Error handled in request.ts
            } finally {
                loading.value = false;
            }
        }
    });
};

const handleSmsLogin = async () => {
     if (!smsFormRef.value) return;
     await smsFormRef.value.validate(async (valid) => {
         if (valid) {
             loading.value = true;
             try {
                 const res = await loginSms(smsForm);
                 if (res) {
                    userStore.setLoginState(res);
                    ElMessage.success('登录成功');
                    router.push('/');
                 }
             } catch (e) {
                 
             } finally {
                 loading.value = false;
             }
         }
     });
};

const handleSendSms = async () => {
    if (!smsForm.phone) {
        ElMessage.warning('请输入手机号');
        return;
    }
    try {
        await sendSms(smsForm.phone);
        ElMessage.success('验证码已发送');
        isCounting.value = true;
        const timer = setInterval(() => {
            count.value--;
            if (count.value <= 0) {
                clearInterval(timer);
                isCounting.value = false;
                count.value = 60;
            }
        }, 1000);
    } catch (e) {
        // Error handled
    }
};
</script>

<style scoped lang="scss">
.login-container {
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background: url('https://img.zcool.cn/community/01f00e5c57b777a801203d22b62058.jpg@1280w_1l_2o_100sh.jpg') no-repeat center center;
    background-size: cover;
    
    &::before {
        content: '';
        position: absolute;
        top: 0; left: 0; right: 0; bottom: 0;
        background: rgba(0,0,0,0.4);
    }
}

.login-box {
    position: relative;
    width: 400px;
    background: white;
    padding: 40px;
    border-radius: 8px;
    box-shadow: 0 10px 30px rgba(0,0,0,0.2);

    .login-header {
        text-align: center;
        margin-bottom: 30px;
        h2 { font-size: 24px; color: #333; margin-bottom: 10px; }
        p { color: #999; font-size: 14px; }
    }

    .login-btn {
        width: 100%;
        height: 40px;
        font-size: 16px;
    }

    .sms-code-wrapper {
        display: flex;
        gap: 10px;
        .el-button { width: 120px; }
    }

    .login-footer {
        margin-top: 20px;
        text-align: center;
        font-size: 14px;
        
        a { color: #409EFF; text-decoration: none; }
        .divider { margin: 0 10px; color: #eee; }
    }
}
</style>
