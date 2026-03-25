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
                <el-option
                  v-for="item in PRODUCT_CATEGORY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="商品主图 (可上传多张)" prop="productImg">
          <div class="upload-container">
            <el-upload
              :action="uploadAction"
              name="file"
              list-type="picture-card"
              :file-list="productImgList"
              :on-success="handleProductImgSuccess"
              :on-remove="handleProductImgRemove"
              :before-upload="beforeUpload"
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
            <el-form-item label="产地 (省/市/区)" prop="originPlace">
               <el-cascader
                v-model="selectedRegion"
                :options="regionOptions"
                placeholder="请选择省/市/区"
                style="width: 100%"
                @change="handleRegionChange"
              />
            </el-form-item>
          </el-col>
           <el-col :span="12">
            <el-form-item label="详细地址" prop="originPlaceDetail">
              <el-input v-model="productForm.originPlaceDetail" placeholder="乡镇/村/街道/门牌号" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="种植周期" prop="plantingCycle">
              <el-input v-model="productForm.plantingCycle" placeholder="如：3月播种，9月采摘；或 孵化期30天，出栏180天" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="施肥类型">
              <el-select v-model="productForm.fertilizerType" placeholder="请选择" style="width: 100%">
                <el-option label="有机生产" value="有机生产" />
                <el-option label="绿色生产" value="绿色生产" />
                <el-option label="无公害" value="无公害" />
                <el-option label="传统农耕" value="传统农耕" />
                <el-option label="现代化养殖" value="现代化养殖" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
           <el-col :span="12">
            <el-form-item label="保存方式">
              <el-select v-model="productForm.storageMethod" placeholder="请选择" style="width: 100%">
                <el-option label="常温保存" value="常温保存" />
                <el-option label="冷藏保存" value="冷藏保存" />
                <el-option label="通风干燥" value="通风干燥" />
                <el-option label="冷冻保存" value="冷冻保存" />
              </el-select>
            </el-form-item>
          </el-col>
           <el-col :span="12">
             <el-form-item label="运输方式">
               <el-radio-group v-model="productForm.transportMethod">
                 <el-radio label="普通物流" border />
                 <el-radio label="冷链运输" border />
                 <el-radio label="加急空运" border />
               </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 溯源二维码预览 -->
        <div class="qr-preview-section">
          <div class="qr-box">
             <img :src="qrCodeUrl" v-if="qrCodeUrl" />
             <div class="placeholder" v-else>
               <el-icon size="30"><Grid /></el-icon>
               <span>填写信息后自动生成</span>
             </div>
          </div>
          <div class="qr-info">
            <h4>溯源二维码预览</h4>
            <p>包含产地、生产周期、生产方式等全链路信息，消费者扫码可见。</p>
            <el-button type="primary" link :disabled="!qrCodeUrl" @click="downloadQrCode">下载二维码图片</el-button>
          </div>
        </div>
      </el-card>

      <!-- 商品详情 -->
      <el-card class="form-card">
        <template #header>
          <span>详细介绍</span>
        </template>
        <el-form-item prop="productDesc" label="文字描述">
          <el-input
            v-model="productForm.productDesc"
            type="textarea"
            :rows="6"
            placeholder="请详细描述商品的特色、口感、营养价值等..."
          />
        </el-form-item>
        <el-form-item label="详情图片 (可上传多张)">
          <div class="upload-container">
            <el-upload
              :action="uploadAction"
              name="file"
              list-type="picture-card"
              :file-list="productDetailImgList"
              :on-success="handleProductDetailImgSuccess"
              :on-remove="handleProductDetailImgRemove"
              :before-upload="beforeUpload"
              class="upload-demo"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>
        </el-form-item>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Plus, CircleCheck, QuestionFilled, Grid } from '@element-plus/icons-vue';
import type { FormInstance, FormRules, UploadProps } from 'element-plus';
import { createProduct, getProductDetail, updateProduct, generateProductQrcode } from '@/apis/merchant';
import { getFullImageUrl } from '@/utils/image';
import { regionData, codeToText } from 'element-china-area-data';
import { PRODUCT_CATEGORY_OPTIONS } from '@/constants/category';

const uploadAction = (import.meta.env.VITE_API_BASE_URL || '/api') + '/common/upload/product'
const route = useRoute();
const formRef = ref<FormInstance>();
const submitting = ref(false);

const isEdit = computed(() => !!route.params.id);

const selectedRegion = ref<string[]>([]);
const regionOptions = regionData;

const handleRegionChange = (value: string[]) => {
  if (value && value.length > 0) {
    const loc = value.map(code => codeToText[code]).join('/');
    productForm.originPlace = loc;
  }
};

const productImgList = ref<any[]>([]);
const productDetailImgList = ref<any[]>([]);

const handleProductImgSuccess = (response: any, file: any, fileList: any[]) => {
  if (response.code === 200) {
    productImgList.value = fileList;
    updateProductImgStr();
    ElMessage.success('主图上传成功');
  } else {
    ElMessage.error(response.msg || '上传失败');
    productImgList.value = fileList.filter(f => f.uid !== file.uid);
  }
}

const handleProductImgRemove = (_file: any, fileList: any[]) => {
  productImgList.value = fileList;
  updateProductImgStr();
}

const updateProductImgStr = () => {
  productForm.productImg = productImgList.value
    .map(f => f.response ? f.response.data : f.url.replace(getFullImageUrl(''), ''))
    .join(',');
}

const handleProductDetailImgSuccess = (response: any, file: any, fileList: any[]) => {
  if (response.code === 200) {
    productDetailImgList.value = fileList;
    updateProductDetailImgStr();
    ElMessage.success('详情图上传成功');
  } else {
    ElMessage.error(response.msg || '上传失败');
    productDetailImgList.value = fileList.filter(f => f.uid !== file.uid);
  }
}

const handleProductDetailImgRemove = (_file: any, fileList: any[]) => {
  productDetailImgList.value = fileList;
  updateProductDetailImgStr();
}

const updateProductDetailImgStr = () => {
  productForm.productDetailImg = productDetailImgList.value
    .map(f => f.response ? f.response.data : f.url.replace(getFullImageUrl(''), ''))
    .join(',');
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

// 表单数据
const productForm = reactive({
  productName: '',
  categoryId: '' as number | '',
  price: 0,
  stock: 0,
  stockWarning: 10,
  productImg: '',
  productDetailImg: '',
  productDesc: '',
  originPlace: '', // 省/市/区
  originPlaceDetail: '', // 详细地址
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
  originPlace: [{ required: true, message: '请选择产地', trigger: 'change' }],
  originPlaceDetail: [{ required: true, message: '请输入详细产地', trigger: 'blur' }],
  plantingCycle: [{ required: true, message: '请输入种植周期', trigger: 'blur' }]
};

// 由后端生成二维码（data:image/png;base64...）
const qrCodeUrl = ref('');

const refreshQrCode = async (id: number) => {
  try {
    const code = await generateProductQrcode(id);
    qrCodeUrl.value = code || '';
    return qrCodeUrl.value;
  } catch (e) {
    console.error('生成溯源码失败', e);
    qrCodeUrl.value = '';
    return '';
  }
};

const downloadQrCode = () => {
  if (!qrCodeUrl.value) return;
  const link = document.createElement('a');
  link.href = qrCodeUrl.value;
  link.download = 'trace_qrcode.png';
  link.click();
  ElMessage.success('二维码已下载');
};

const toUploadList = (raw: string, prefix: string) => {
  if (!raw) return [];
  return raw.split(',').map((url, index) => ({
    name: `${prefix}_${index}`,
    url: getFullImageUrl(url.trim()),
    uid: `${prefix}_${index}`
  }));
};

const applyFormData = (data: any) => {
  if (!data) return;
  Object.assign(productForm, {
    productName: data.productName ?? '',
    categoryId: data.categoryId ?? '',
    price: Number(data.price ?? 0),
    stock: Number(data.stock ?? 0),
    stockWarning: Number(data.stockWarning ?? 10),
    productImg: data.productImg ?? '',
    productDetailImg: data.productDetailImg ?? '',
    productDesc: data.productDesc ?? '',
    originPlace: data.originPlace ?? '',
    originPlaceDetail: data.originPlaceDetail ?? '',
    plantingCycle: data.plantingCycle ?? '',
    fertilizerType: data.fertilizerType ?? '',
    storageMethod: data.storageMethod ?? '',
    transportMethod: data.transportMethod || '普通物流'
  });
  productImgList.value = toUploadList(productForm.productImg, 'img');
  productDetailImgList.value = toUploadList(productForm.productDetailImg, 'detail_img');
};

const loadEditProduct = async (id: number) => {
  const detail = await getProductDetail(id);
  if (detail) {
    applyFormData(detail);
  }
};

onMounted(async () => {
  if (!isEdit.value) return;
  const id = Number(route.params.id);
  if (Number.isNaN(id) || id <= 0) return;

  let hasLocalSnapshot = false;
  const state = history.state;
  if (state && state.productData) {
    try {
      const snapshot = JSON.parse(state.productData);
      applyFormData(snapshot);
      hasLocalSnapshot = true;
    } catch (e) {
      console.error('解析商品快照失败', e);
    }
  }

  try {
    await loadEditProduct(id);
  } catch (e) {
    console.error('加载商品详情失败', e);
    if (!hasLocalSnapshot) {
      ElMessage.error('加载商品详情失败，请返回列表重试');
    }
  }
  void refreshQrCode(id);
});

const saveProduct = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        if (isEdit.value) {
          const id = Number(route.params.id);
          await updateProduct(id, productForm);
          const code = await refreshQrCode(id);
          qrCodeUrl.value = code || '';
          ElMessage.success('商品更新成功');
        } else {
          const created = await createProduct(productForm);
          const id = created?.id;
          const code = id ? await refreshQrCode(id) : '';
          qrCodeUrl.value = code || '';
          ElMessage.success('商品发布成功');
        }
        // 生成溯源码后留在当前页，方便用户预览/下载打印
      } catch (error) {
        console.error(error);
      } finally {
        submitting.value = false;
      }
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
