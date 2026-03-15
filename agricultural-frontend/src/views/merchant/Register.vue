<template>
  <div class="merchant-register-page">
    <div class="register-header">
      <div class="logo">
        <el-icon size="40"><Shop /></el-icon>
        <span>商家入驻</span>
      </div>
    </div>

    <div class="register-container">
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="基本信息" />
        <el-step title="资质认证" />
        <el-step title="店铺信息" />
        <el-step title="提交审核" />
      </el-steps>

      <!-- 第一步：基本信息 -->
      <div v-if="activeStep === 0" class="step-content">
        <el-form :model="form.basic" label-position="top">
          <el-form-item label="手机号">
            <el-input v-model="form.basic.phone" placeholder="请输入手机号" maxlength="11">
              <template #append>
                <el-button :disabled="codeSending" @click="sendCode">
                  {{ codeSending ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="验证码">
            <el-input v-model="form.basic.code" placeholder="请输入验证码" maxlength="6" />
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input v-model="form.basic.password" type="password" placeholder="请设置登录密码" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="form.basic.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
          </el-form-item>
        </el-form>
      </div>

      <!-- 第二步：资质认证 -->
      <div v-if="activeStep === 1" class="step-content">
        <el-form :model="form.qualification" label-position="top">
          <el-form-item label="经营者姓名">
            <el-input v-model="form.qualification.name" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="form.qualification.idCard" placeholder="请输入身份证号码" maxlength="18" />
          </el-form-item>
          <el-form-item label="身份证正面">
            <el-upload
              class="idcard-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
            >
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">点击上传身份证正面</div>
            </el-upload>
          </el-form-item>
          <el-form-item label="身份证反面">
            <el-upload
              class="idcard-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
            >
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">点击上传身份证反面</div>
            </el-upload>
          </el-form-item>
          <el-form-item label="营业执照">
            <el-upload
              class="license-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
            >
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">点击上传营业执照</div>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>

      <!-- 第三步：店铺信息 -->
      <div v-if="activeStep === 2" class="step-content">
        <el-form :model="form.shop" label-position="top">
          <el-form-item label="店铺名称">
            <el-input v-model="form.shop.name" placeholder="请输入店铺名称" />
          </el-form-item>
          <el-form-item label="店铺类型">
            <el-radio-group v-model="form.shop.type">
              <el-radio label="individual">个体工商户</el-radio>
              <el-radio label="enterprise">企业</el-radio>
              <el-radio label="cooperative">农业合作社</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="主营类目">
            <el-checkbox-group v-model="form.shop.categories">
              <el-checkbox label="vegetables">新鲜蔬菜</el-checkbox>
              <el-checkbox label="fruits">水果</el-checkbox>
              <el-checkbox label="grains">粮油米面</el-checkbox>
              <el-checkbox label="poultry">禽畜肉蛋</el-checkbox>
              <el-checkbox label="specialty">土特产</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="店铺简介">
            <el-input
              v-model="form.shop.description"
              type="textarea"
              rows="4"
              placeholder="请简要介绍您的店铺"
            />
          </el-form-item>
          <el-form-item label="店铺Logo">
            <el-upload
              class="logo-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
            >
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">点击上传Logo</div>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>

      <!-- 第四步：提交审核 -->
      <div v-if="activeStep === 3" class="step-content">
        <div class="submit-success">
          <el-icon class="success-icon" color="#67c23a" :size="60"><CircleCheck /></el-icon>
          <h3>提交成功！</h3>
          <p>您的入驻申请已提交，我们将在1-3个工作日内完成审核</p>
          <p class="tip">审核结果将通过短信通知您，请保持手机畅通</p>
          <el-button type="primary" @click="$router.push('/merchant/login')">
            返回登录页
          </el-button>
        </div>
      </div>

      <div v-if="activeStep < 3" class="step-actions">
        <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
        <el-button type="primary" @click="nextStep">
          {{ activeStep === 2 ? '提交审核' : '下一步' }}
        </el-button>
      </div>

      <div class="login-link">
        已有账号？<el-button link type="primary" @click="$router.push('/merchant/login')">立即登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Shop, Plus, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const activeStep = ref(0)
const codeSending = ref(false)
const countdown = ref(60)

const form = reactive({
  basic: {
    phone: '',
    code: '',
    password: '',
    confirmPassword: ''
  },
  qualification: {
    name: '',
    idCard: '',
    idCardFront: '',
    idCardBack: '',
    license: ''
  },
  shop: {
    name: '',
    type: 'individual',
    categories: [],
    description: '',
    logo: ''
  }
})

const sendCode = () => {
  if (!form.basic.phone || form.basic.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  codeSending.value = true
  ElMessage.success('验证码已发送')
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      codeSending.value = false
      countdown.value = 60
    }
  }, 1000)
}

const nextStep = () => {
  if (activeStep.value === 2) {
    ElMessage.success('提交成功')
  }
  activeStep.value++
}
</script>

<style scoped lang="scss">
.merchant-register-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.register-header {
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  padding: 40px 20px;
  text-align: center;
  color: #fff;

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;

    span {
      font-size: 24px;
      font-weight: 600;
    }
  }
}

.register-container {
  max-width: 600px;
  margin: -30px auto 0;
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 30px 20px;
  min-height: calc(100vh - 150px);
}

.step-content {
  margin: 30px 0;
}

.idcard-uploader,
.license-uploader,
.logo-uploader {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
  text-align: center;
  padding: 40px 20px;

  &:hover {
    border-color: #67c23a;
  }

  .upload-icon {
    font-size: 28px;
    color: #8c939d;
  }

  .upload-text {
    color: #8c939d;
    font-size: 14px;
    margin-top: 8px;
  }
}

.idcard-uploader {
  width: 100%;
  max-width: 300px;
}

.license-uploader {
  width: 100%;
  max-width: 300px;
}

.logo-uploader {
  width: 120px;
  height: 120px;
  padding: 20px;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.submit-success {
  text-align: center;
  padding: 40px 20px;

  .success-icon {
    margin-bottom: 20px;
  }

  h3 {
    font-size: 20px;
    color: #333;
    margin-bottom: 16px;
  }

  p {
    color: #666;
    margin-bottom: 8px;
  }

  .tip {
    color: #999;
    font-size: 14px;
    margin-bottom: 30px;
  }
}

.login-link {
  text-align: center;
  margin-top: 30px;
  color: #666;
}

@media (max-width: 768px) {
  .register-container {
    margin: -20px 0 0;
    border-radius: 12px 12px 0 0;
    padding: 20px 16px;
  }
}
</style>
