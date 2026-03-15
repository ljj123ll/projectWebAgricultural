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
            action="#"
            :auto-upload="false"
            :show-file-list="false"
          >
            <img v-if="shopForm.logo" :src="shopForm.logo" class="logo-preview" />
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
            <el-tag type="success">已认证</el-tag>
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
            <span class="label">营业执照：</span>
            <el-button link type="primary">查看</el-button>
          </div>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const shopForm = reactive({
  name: '绿色农场旗舰店',
  logo: '',
  description: '专注绿色有机农产品，为您提供最新鲜的蔬菜水果',
  contactName: '张老板',
  contactPhone: '138****8888',
  address: '北京市朝阳区某某农产品批发市场A区101号',
  categories: ['vegetables', 'fruits'],
  openTime: new Date(2024, 0, 1, 8, 0),
  closeTime: new Date(2024, 0, 1, 18, 0),
  ownerName: '张三',
  idCard: '110101********1234'
})

const saveShop = () => {
  ElMessage.success('保存成功')
}
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
