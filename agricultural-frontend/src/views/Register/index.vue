<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h2>注册账号</h2>
        <p>加入助农电商平台</p>
      </div>
      
      <el-form :model="form" ref="formRef" :rules="rules" label-width="0">
        <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" :prefix-icon="Iphone" />
        </el-form-item>
        <el-form-item prop="code">
            <div class="sms-code-wrapper">
                <el-input v-model="form.code" placeholder="请输入验证码" :prefix-icon="Key" />
                <el-button type="primary" :disabled="isCounting" @click="handleSendSms">
                    {{ isCounting ? `${count}s后重发` : '获取验证码' }}
                </el-button>
            </div>
        </el-form-item>
        <el-form-item prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入昵称" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
            <el-button type="primary" class="register-btn" @click="handleRegister" :loading="loading">立即注册</el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/modules/user';
import { ElMessage, type FormInstance } from 'element-plus';
import { Iphone, Lock, Key, User } from '@element-plus/icons-vue';
import { register, sendSms } from '@/apis/user';

const router = useRouter();
const userStore = useUserStore();
const formRef = ref<FormInstance>();
const loading = ref(false);

const form = reactive({
    phone: '',
    code: '',
    nickname: '',
    password: '',
    confirmPassword: ''
});

const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
    if (value !== form.password) {
        callback(new Error('两次输入密码不一致'));
    } else {
        callback();
    }
};

const rules = {
    phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
    code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
    nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
    ]
};

const isCounting = ref(false);
const count = ref(60);

const handleSendSms = async () => {
    if (!form.phone) {
        ElMessage.warning('请输入手机号');
        return;
    }
    try {
        await sendSms(form.phone);
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

const handleRegister = async () => {
    if (!formRef.value) return;
    await formRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true;
            try {
                const res = await register({
                    phone: form.phone,
                    code: form.code,
                    nickname: form.nickname,
                    password: form.password
                });
                if (res) {
                    userStore.setLoginState(res);
                    ElMessage.success('注册成功');
                    router.push('/');
                }
            } catch (e) {
                // Error handled
            } finally {
                loading.value = false;
            }
        }
    });
};
</script>

<style scoped lang="scss">
.register-container {
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

.register-box {
    position: relative;
    width: 400px;
    background: white;
    padding: 40px;
    border-radius: 8px;
    box-shadow: 0 10px 30px rgba(0,0,0,0.2);

    .register-header {
        text-align: center;
        margin-bottom: 30px;
        h2 { font-size: 24px; color: #333; margin-bottom: 10px; }
        p { color: #999; font-size: 14px; }
    }

    .register-btn {
        width: 100%;
        height: 40px;
        font-size: 16px;
    }

    .sms-code-wrapper {
        display: flex;
        gap: 10px;
        .el-button { width: 120px; }
    }

    .register-footer {
        margin-top: 20px;
        text-align: center;
        font-size: 14px;
        color: #666;
        
        a { color: #409EFF; text-decoration: none; margin-left: 5px; }
    }
}
</style>
