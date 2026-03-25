<template>
  <div class="shop-page">
    <div class="page-header">
      <h2>店铺信息</h2>
      <el-button type="primary" @click="saveShop">保存修改</el-button>
    </div>

    <el-form :model="shopForm" label-position="top" class="shop-form">
      <el-card class="form-card">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-form-item label="店铺名称">
          <el-input v-model="shopForm.name" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="店铺Logo">
          <el-upload
            class="logo-uploader"
            :action="uploadAction"
            name="file"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
          >
            <img v-if="shopForm.logo" :src="getFullImageUrl(shopForm.logo)" class="logo-preview" />
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <span>点击上传</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input
            v-model="shopForm.description"
            type="textarea"
            rows="4"
            placeholder="请简要介绍您的店铺"
          />
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>联系信息</span>
        </template>
        <el-form-item label="联系人">
          <el-input v-model="shopForm.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="shopForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="店铺地址">
          <el-input v-model="shopForm.address" placeholder="请输入详细地址" />
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>经营信息</span>
        </template>
        <el-form-item label="店铺类型">
          <el-radio-group v-model="shopForm.shopType">
            <el-radio label="individual">个体工商户</el-radio>
            <el-radio label="enterprise">企业</el-radio>
            <el-radio label="cooperative">农业合作社</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主营类目">
          <el-checkbox-group v-model="shopForm.categories">
            <el-checkbox label="vegetables">新鲜蔬菜</el-checkbox>
            <el-checkbox label="fruits">水果</el-checkbox>
            <el-checkbox label="grains">粮油米面</el-checkbox>
            <el-checkbox label="poultry">禽畜肉蛋</el-checkbox>
            <el-checkbox label="specialty">土特产</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="营业时间">
          <el-time-picker
            v-model="shopForm.openTime"
            placeholder="开始时间"
            format="HH:mm"
          />
          <span style="margin: 0 10px;">至</span>
          <el-time-picker
            v-model="shopForm.closeTime"
            placeholder="结束时间"
            format="HH:mm"
          />
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>资质信息</span>
        </template>
        <div class="qualification-info">
          <div class="info-item">
            <span class="label">认证状态：</span>
            <el-tag :type="auditTagType">{{ auditText }}</el-tag>
          </div>
          <div v-if="shopForm.auditStatus === 2 && shopForm.rejectReason" class="info-item">
            <span class="label">驳回原因：</span>
            <span style="color: #f56c6c;">{{ shopForm.rejectReason }}</span>
          </div>
          <div class="info-item">
            <span class="label">经营者：</span>
            <span>{{ shopForm.ownerName }}</span>
          </div>
          <div class="info-item">
            <span class="label">身份证号：</span>
            <span>{{ shopForm.idCard }}</span>
          </div>
          <div class="info-item">
            <span class="label">身份证正面：</span>
            <el-image
              v-if="shopForm.idCardFront"
              :src="getFullImageUrl(shopForm.idCardFront)"
              :preview-src-list="[getFullImageUrl(shopForm.idCardFront)]"
              style="width: 120px; height: 80px;"
              fit="cover"
            />
            <span v-else style="color: #999;">未上传</span>
          </div>
          <div class="info-item">
            <span class="label">身份证反面：</span>
            <el-image
              v-if="shopForm.idCardBack"
              :src="getFullImageUrl(shopForm.idCardBack)"
              :preview-src-list="[getFullImageUrl(shopForm.idCardBack)]"
              style="width: 120px; height: 80px;"
              fit="cover"
            />
            <span v-else style="color: #999;">未上传</span>
          </div>
          <div class="info-item">
            <span class="label">营业执照：</span>
            <el-image
              v-if="shopForm.licenseImg"
              :src="getFullImageUrl(shopForm.licenseImg)"
              :preview-src-list="[getFullImageUrl(shopForm.licenseImg)]"
              style="width: 120px; height: 80px;"
              fit="cover"
            />
            <span v-else style="color: #999;">未上传</span>
          </div>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getShopInfo, updateShopInfo } from '@/apis/merchant'
import type { UploadProps } from 'element-plus'
import { getFullImageUrl } from '@/utils/image'

const uploadAction = (import.meta.env.VITE_API_BASE_URL || '/api') + '/common/upload/qualification'

const shopForm = reactive({
  name: '',
  logo: '',
  description: '',
  contactName: '',
  contactPhone: '',
  address: '',
  shopType: 'individual',
  categories: [] as string[],
  openTime: new Date(2024, 0, 1, 8, 0),
  closeTime: new Date(2024, 0, 1, 18, 0),
  ownerName: '',
  idCard: '',
  idCardFront: '',
  idCardBack: '',
  licenseImg: '',
  auditStatus: 0,
  rejectReason: ''
})

const auditText = computed(() => {
  if (shopForm.auditStatus === 1) return '已通过'
  if (shopForm.auditStatus === 2) return '已驳回'
  return '待审核'
})

const auditTagType = computed(() => {
  if (shopForm.auditStatus === 1) return 'success'
  if (shopForm.auditStatus === 2) return 'danger'
  return 'warning'
})

const loadShopInfo = async () => {
  try {
    const res = await getShopInfo();
    if (res) {
      shopForm.name = res.shopName || '';
      shopForm.description = res.shopIntro || '';
      shopForm.logo = res.qualificationImg || ''; 
      shopForm.contactName = res.contactName || '';
      shopForm.contactPhone = res.contactPhone || '';
      shopForm.ownerName = res.ownerName || '';
      shopForm.idCard = res.idCard || '';
      shopForm.idCardFront = res.idCardFront || '';
      shopForm.idCardBack = res.idCardBack || '';
      shopForm.licenseImg = res.licenseImg || '';
      shopForm.auditStatus = res.auditStatus ?? 0;
      shopForm.rejectReason = res.rejectReason || '';
      shopForm.shopType = res.shopType || 'individual';
      if (res.categories) {
        shopForm.categories = res.categories.split(',');
      }
      shopForm.address = res.shopAddress || '';
    }
  } catch (error) {
    console.error('Failed to load shop info', error);
  }
}

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    shopForm.logo = response.data;
    ElMessage.success('上传成功');
  } else {
    ElMessage.error(response.msg || '上传失败');
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

const saveShop = async () => {
  try {
    await updateShopInfo({
      shopName: shopForm.name,
      shopIntro: shopForm.description,
      qualificationImg: shopForm.logo,
      shopType: shopForm.shopType,
      categories: shopForm.categories.join(','),
      shopAddress: shopForm.address
    });
    ElMessage.success('保存成功')
  } catch (error) {
    console.error('Failed to save shop info', error);
  }
}

onMounted(() => {
  loadShopInfo();
})
</script>

<style scoped lang="scss">
.shop-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
  }
}

.shop-form {
  max-width: 800px;
}

.form-card {
  margin-bottom: 20px;
}

.logo-uploader {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  &:hover {
    border-color: #67c23a;
  }

  .logo-preview {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #8c939d;

    .el-icon {
      font-size: 28px;
      margin-bottom: 8px;
    }

    span {
      font-size: 12px;
    }
  }
}

.qualification-info {
  .info-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
    }

    .label {
      color: #666;
      width: 100px;
    }
  }
}
</style>
