<template>
  <div class="merchant-register-page">
    <div class="register-header">
      <div class="header-content">
        <el-button link class="back-btn" @click="$router.push('/merchant/login')">
          <el-icon><ArrowLeft /></el-icon>
          返回登录
        </el-button>
        <div class="logo">
          <el-icon size="40"><Shop /></el-icon>
          <span>商家入驻</span>
        </div>
        <div class="placeholder"></div>
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
            <el-input v-model="form.basic.code" placeholder="请输入验证码" maxlength="6" :disabled="!codeSent" />
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
              :action="uploadAction"
              :headers="uploadHeaders"
              name="file"
              :show-file-list="false"
              :on-success="handleUploadSuccess('idCardFront')"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <template #default>
                <img v-if="form.qualification.idCardFront" :src="getFullImageUrl(form.qualification.idCardFront)" class="uploaded-img" @error="handleImageError" />
                <div v-else class="upload-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <div class="upload-text">点击上传身份证正面</div>
                </div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item label="身份证反面">
            <el-upload
              class="idcard-uploader"
              :action="uploadAction"
              :headers="uploadHeaders"
              name="file"
              :show-file-list="false"
              :on-success="handleUploadSuccess('idCardBack')"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <template #default>
                <img v-if="form.qualification.idCardBack" :src="getFullImageUrl(form.qualification.idCardBack)" class="uploaded-img" @error="handleImageError" />
                <div v-else class="upload-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <div class="upload-text">点击上传身份证反面</div>
                </div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item label="营业执照">
            <el-upload
              class="license-uploader"
              :action="uploadAction"
              :headers="uploadHeaders"
              name="file"
              :show-file-list="false"
              :on-success="handleUploadSuccess('license')"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <template #default>
                <img v-if="form.qualification.license" :src="getFullImageUrl(form.qualification.license)" class="uploaded-img" @error="handleImageError" />
                <div v-else class="upload-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <div class="upload-text">点击上传营业执照</div>
                </div>
              </template>
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
              :action="uploadAction"
              :headers="uploadHeaders"
              name="file"
              :show-file-list="false"
              :on-success="handleUploadSuccess('logo')"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <template #default>
                <img v-if="form.shop.logo" :src="getFullImageUrl(form.shop.logo)" class="uploaded-img" @error="handleImageError" />
                <div v-else class="upload-placeholder">
                  <el-icon class="upload-icon"><Plus /></el-icon>
                  <div class="upload-text">点击上传Logo</div>
                </div>
              </template>
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
import { Shop, Plus, CircleCheck, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { merchantRegister, merchantSendSms } from '@/apis/merchant'
import { getFullImageUrl } from '@/utils/image'
import type { UploadProps } from 'element-plus'

const activeStep = ref(0)
const codeSending = ref(false)
const codeSent = ref(false)
const countdown = ref(60)

const uploadAction = (import.meta.env.VITE_API_BASE_URL || '/api') + '/common/upload'
const uploadHeaders = {
  // Add headers if needed, e.g., Authorization
}



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

const handleUploadSuccess = (field: string) => (response: any) => {
  console.log('Upload success:', response)
  if (response.code === 200) {
    // Map the field string to the correct nested property
    const imageUrl = response.data
    if (field === 'idCardFront') form.qualification.idCardFront = imageUrl
    else if (field === 'idCardBack') form.qualification.idCardBack = imageUrl
    else if (field === 'license') form.qualification.license = imageUrl
    else if (field === 'logo') form.shop.logo = imageUrl
    ElMessage.success('上传成功')
    console.log('Image URL:', imageUrl, 'Full URL:', getFullImageUrl(imageUrl))
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('图片必须是 JPG/PNG 格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const handleUploadError = (error: any) => {
  console.error('Upload error:', error)
  ElMessage.error('上传失败，请检查网络连接')
}

const handleImageError = (e: any) => {
  console.error('Image load error:', e)
  ElMessage.error('图片加载失败')
}

const validateStep = () => {
  if (activeStep.value === 0) {
    if (!form.basic.phone) {
      ElMessage.warning('请输入手机号')
      return false
    }
    if (!/^1[3-9]\d{9}$/.test(form.basic.phone)) {
      ElMessage.warning('请输入正确的手机号')
      return false
    }
    if (!codeSent.value) {
      ElMessage.warning('请先获取验证码')
      return false
    }
    if (!form.basic.code) {
      ElMessage.warning('请输入验证码')
      return false
    }
    if (!form.basic.password) {
      ElMessage.warning('请设置登录密码')
      return false
    }
    if (form.basic.password.length < 6) {
      ElMessage.warning('密码长度至少6位')
      return false
    }
    if (form.basic.password !== form.basic.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致')
      return false
    }
  } else if (activeStep.value === 1) {
    if (!form.qualification.name) {
      ElMessage.warning('请输入经营者姓名')
      return false
    }
    if (!form.qualification.idCard) {
      ElMessage.warning('请输入身份证号')
      return false
    }
    if (form.qualification.idCard.length !== 18) {
      ElMessage.warning('请输入18位身份证号')
      return false
    }
    if (!form.qualification.idCardFront) {
      ElMessage.warning('请上传身份证正面')
      return false
    }
    if (!form.qualification.idCardBack) {
      ElMessage.warning('请上传身份证反面')
      return false
    }
    if (!form.qualification.license) {
      ElMessage.warning('请上传营业执照')
      return false
    }
  } else if (activeStep.value === 2) {
    if (!form.shop.name) {
      ElMessage.warning('请输入店铺名称')
      return false
    }
    if (form.shop.categories.length === 0) {
      ElMessage.warning('请选择主营类目')
      return false
    }
    if (!form.shop.description) {
      ElMessage.warning('请输入店铺简介')
      return false
    }
    if (!form.shop.logo) {
      ElMessage.warning('请上传店铺Logo')
      return false
    }
  }
  return true
}

const sendCode = async () => {
  if (!form.basic.phone || !/^1[3-9]\d{9}$/.test(form.basic.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  
  try {
    codeSending.value = true
    await merchantSendSms(form.basic.phone)
    ElMessage.success('验证码已发送')
    codeSent.value = true
    
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        codeSending.value = false
        countdown.value = 60
      }
    }, 1000)
  } catch (error: any) {
    codeSending.value = false
    ElMessage.error('发送失败，请稍后重试')
    console.error(error)
  }
}

const nextStep = async () => {
  if (!validateStep()) return

  if (activeStep.value === 2) {
    try {
      const payload = {
        phone: form.basic.phone,
        code: form.basic.code,
        password: form.basic.password,
        merchantName: form.shop.name,
        contactPerson: form.qualification.name,
        contactPhone: form.basic.phone,
        idCard: form.qualification.idCard,
        idCardFront: form.qualification.idCardFront,
        idCardBack: form.qualification.idCardBack,
        license: form.qualification.license,
        shopType: form.shop.type,
        categories: form.shop.categories,
        shopDescription: form.shop.description,
        logo: form.shop.logo
      }
      await merchantRegister(payload)
      activeStep.value++
    } catch (error) {
      console.error(error)
    }
  } else {
    activeStep.value++
  }
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

  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    max-width: 1000px;
    margin: 0 auto;
  }

  .back-btn {
    color: #fff;
    font-size: 16px;
    
    .el-icon {
      margin-right: 4px;
    }
    
    &:hover {
      opacity: 0.9;
    }
  }

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

  .placeholder {
    width: 100px; // Match back button width roughly for centering
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

  .uploaded-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    border-radius: 6px;
  }

  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    padding: 20px;
  }
}

.idcard-uploader {
  width: 100%;
  max-width: 300px;
  height: 180px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.license-uploader {
  width: 100%;
  max-width: 300px;
  height: 180px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-uploader {
  width: 120px;
  height: 120px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
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
