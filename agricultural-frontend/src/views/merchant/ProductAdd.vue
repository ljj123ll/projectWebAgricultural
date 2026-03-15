<template>
  <div class="product-add-page">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
      </div>
      <div class="header-right">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveProduct">发布商品</el-button>
      </div>
    </div>

    <el-form 
      ref="formRef" 
      :model="productForm" 
      :rules="rules"
      label-position="top" 
      class="product-form"
    >
      <!-- 基本信息 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag size="small" type="info">必填</el-tag>
          </div>
        </template>
        <el-row :gutter="24">
          <el-col :span="16">
            <el-form-item label="商品名称" prop="productName">
              <el-input v-model="productForm.productName" placeholder="请输入商品名称（如：四川红心猕猴桃）" maxlength="60" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品品类" prop="categoryId">
              <el-select v-model="productForm.categoryId" placeholder="请选择品类" style="width: 100%">
                <el-option label="生鲜果蔬" :value="1" />
                <el-option label="粮油副食" :value="2" />
                <el-option label="干货特产" :value="3" />
                <el-option label="畜禽肉蛋" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="商品图片" prop="productImg">
          <div class="upload-container">
            <el-input v-model="productForm.productImg" placeholder="输入图片URL或使用上传组件" />
            <div class="image-preview" v-if="productForm.productImg">
              <img :src="productForm.productImg" />
            </div>
            <!-- 模拟上传组件 -->
            <el-upload
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :limit="5"
              class="upload-demo"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>
        </el-form-item>
      </el-card>

      <!-- 价格库存 -->
      <el-card class="form-card">
        <template #header>
          <span>价格库存</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="销售价格" prop="price">
              <el-input-number 
                v-model="productForm.price" 
                :min="0" :precision="2" :step="0.1" 
                style="width: 100%"
                controls-position="right"
              >
                <template #prefix>¥</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存数量" prop="stock">
              <el-input-number 
                v-model="productForm.stock" 
                :min="0" :step="1" 
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
             <el-form-item label="库存预警值">
              <el-input-number 
                v-model="productForm.stockWarning" 
                :min="0" 
                placeholder="低于此值提醒"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 溯源信息 (核心特色) -->
      <el-card class="form-card trace-card">
        <template #header>
          <div class="card-header">
            <span><el-icon class="icon-success"><CircleCheck /></el-icon> 溯源信息</span>
            <el-tooltip content="填写完善的溯源信息有助于提升销量" placement="top">
              <el-icon class="help-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </template>
        
        <el-alert
          title="系统将根据以下信息自动生成防伪溯源二维码，请真实填写"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="产地详情" prop="originPlaceDetail">
              <el-input v-model="productForm.originPlaceDetail" placeholder="省/市/县/乡镇/村" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="种植周期" prop="plantingCycle">
              <el-input v-model="productForm.plantingCycle" placeholder="如：3月播种，9月采摘" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="施肥类型">
              <el-select v-model="productForm.fertilizerType" placeholder="请选择" style="width: 100%">
                <el-option label="有机肥" value="有机肥" />
                <el-option label="农家肥" value="农家肥" />
                <el-option label="复合肥" value="复合肥" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保存方式">
              <el-select v-model="productForm.storageMethod" placeholder="请选择" style="width: 100%">
                <el-option label="常温保存" value="常温保存" />
                <el-option label="冷藏保存" value="冷藏保存" />
                <el-option label="通风干燥" value="通风干燥" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="运输方式">
           <el-radio-group v-model="productForm.transportMethod">
             <el-radio label="普通物流" border />
             <el-radio label="冷链运输" border />
             <el-radio label="加急空运" border />
           </el-radio-group>
        </el-form-item>

        <!-- 溯源二维码预览 -->
        <div class="qr-preview-section">
          <div class="qr-box">
             <img :src="qrCodeUrl" v-if="qrCodeUrl" />
             <div class="placeholder" v-else>
               <el-icon size="30"><QrCode /></el-icon>
               <span>填写信息后自动生成</span>
             </div>
          </div>
          <div class="qr-info">
            <h4>溯源二维码预览</h4>
            <p>包含产地、种植、施肥等全链路信息，消费者扫码可见。</p>
            <el-button type="primary" link :disabled="!qrCodeUrl">下载二维码图片</el-button>
          </div>
        </div>
      </el-card>

      <!-- 商品详情 -->
      <el-card class="form-card">
        <template #header>
          <span>详细介绍</span>
        </template>
        <el-form-item prop="productDesc">
          <el-input
            v-model="productForm.productDesc"
            type="textarea"
            :rows="6"
            placeholder="请详细描述商品的特色、口感、营养价值等..."
          />
        </el-form-item>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Plus, CircleCheck, QuestionFilled, QrCode } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';

const router = useRouter();
const route = useRoute();
const formRef = ref<FormInstance>();
const submitting = ref(false);

const isEdit = computed(() => !!route.params.id);

// 表单数据
const productForm = reactive({
  productName: '',
  categoryId: '',
  price: 0,
  stock: 0,
  stockWarning: 10,
  productImg: '',
  productDesc: '',
  originPlaceDetail: '',
  plantingCycle: '',
  fertilizerType: '',
  storageMethod: '',
  transportMethod: '普通物流'
});

// 验证规则
const rules: FormRules = {
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择品类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  originPlaceDetail: [{ required: true, message: '请输入详细产地', trigger: 'blur' }],
  plantingCycle: [{ required: true, message: '请输入种植周期', trigger: 'blur' }]
};

// 模拟生成二维码
const qrCodeUrl = ref('');

// 监听关键字段变化生成二维码
watch(
  () => [productForm.productName, productForm.originPlaceDetail, productForm.plantingCycle],
  ([name, origin, cycle]) => {
    if (name && origin && cycle) {
      // 模拟生成
      qrCodeUrl.value = 'https://via.placeholder.com/150x150/000000/FFFFFF?text=TraceabilityQR';
    } else {
      qrCodeUrl.value = '';
    }
  }
);

const saveProduct = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate((valid, fields) => {
    if (valid) {
      submitting.value = true;
      // 模拟提交
      setTimeout(() => {
        ElMessage.success(isEdit.value ? '商品更新成功' : '商品发布成功');
        submitting.value = false;
        router.back();
      }, 1000);
    } else {
      ElMessage.warning('请检查必填项');
    }
  });
};
</script>

<style scoped lang="scss">
.product-add-page {
  padding-bottom: 60px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
    h2 { margin: 0; font-size: 18px; color: #303133; }
  }
}

.product-form {
  max-width: 1000px;
  margin: 0 auto;
}

.form-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 10px;
    
    .icon-success { color: #67c23a; }
    .help-icon { color: #909399; cursor: pointer; }
  }
}

.upload-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  
  .image-preview {
    width: 100px;
    height: 100px;
    border-radius: 4px;
    overflow: hidden;
    img { width: 100%; height: 100%; object-fit: cover; }
  }
  
  .upload-demo {
    :deep(.el-upload--picture-card) {
      width: 100px;
      height: 100px;
      line-height: 100px;
    }
  }
}

.trace-card {
  border: 1px solid #e1f3d8;
  :deep(.el-card__header) {
    background-color: #f0f9eb;
  }
}

.qr-preview-section {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #f8f9fa;
  padding: 20px;
  border-radius: 4px;
  margin-top: 10px;
  
  .qr-box {
    width: 120px;
    height: 120px;
    background: #fff;
    border: 1px solid #dcdfe6;
    display: flex;
    align-items: center;
    justify-content: center;
    
    img { width: 100%; height: 100%; }
    
    .placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      color: #909399;
      font-size: 12px;
      gap: 5px;
    }
  }
  
  .qr-info {
    h4 { margin: 0 0 5px 0; color: #303133; }
    p { margin: 0 0 10px 0; color: #606266; font-size: 13px; }
  }
}
</style>
