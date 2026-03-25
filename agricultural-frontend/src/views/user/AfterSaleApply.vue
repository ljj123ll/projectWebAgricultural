<template>
  <div class="after-sale-apply-page">
    <h1 class="page-title">申请售后</h1>

    <el-form :model="form" label-position="top" class="apply-form">
      <el-form-item label="售后类型">
        <el-radio-group v-model="form.type">
          <el-radio label="refund">退款</el-radio>
          <el-radio label="return">退货退款</el-radio>
          <el-radio label="exchange">换货</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="申请原因">
        <el-select v-model="form.reason" placeholder="请选择原因">
          <el-option label="商品变质" value="变质" />
          <el-option label="少发/漏发" value="少发" />
          <el-option label="规格不符" value="规格不符" />
          <el-option label="质量问题" value="质量问题" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>

      <el-form-item label="问题描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请详细描述问题"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="上传凭证">
        <el-upload
          :action="uploadAction"
          list-type="picture-card"
          :file-list="fileList"
          multiple
          :limit="6"
          :on-success="handleUploadSuccess"
          :on-remove="handleRemove"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" size="large" @click="submit">提交申请</el-button>
        <el-button size="large" @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { applyAfterSale } from '@/apis/user';

const router = useRouter();
const route = useRoute();
const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/common/upload`;
const fileList = ref<any[]>([]);

const form = reactive({
  orderNo: route.params.orderNo,
  type: 'refund',
  reason: '',
  description: '',
  proofImgUrls: ''
});

const syncProofImages = () => {
  form.proofImgUrls = fileList.value
    .map(file => {
      if (file?.response?.code === 200) return file.response.data;
      return file?.url || '';
    })
    .filter(Boolean)
    .join(',');
};

const handleUploadSuccess = (response: any, _file: any, files: any[]) => {
  if (response?.code !== 200) {
    ElMessage.error(response?.msg || '凭证上传失败');
    return;
  }
  fileList.value = files;
  syncProofImages();
};

const handleRemove = (_file: any, files: any[]) => {
  fileList.value = files;
  syncProofImages();
};

const submit = async () => {
  if (!form.reason) {
    ElMessage.warning('请选择申请原因');
    return;
  }
  const afterSaleType = form.type === 'refund' ? 1 : (form.type === 'return' ? 2 : 3);
  try {
    const reasonText = form.description?.trim() ? `${form.reason}-${form.description.trim()}` : form.reason;
    const res = await applyAfterSale({
      orderNo: String(form.orderNo),
      afterSaleType,
      applyReason: reasonText.slice(0, 50),
      proofImgUrls: form.proofImgUrls
    });
    ElMessage.success('售后申请已提交');
    // 后端返回 afterSaleNo，用于进入售后详情页
    router.push(`/after-sale-detail/${res.afterSaleNo}`);
  } catch (e) {
    console.error(e);
  }
};
</script>

<style scoped lang="scss">
.after-sale-apply-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  margin: 0 0 24px;
}

.apply-form {
  .el-form-item {
    margin-bottom: 24px;
  }
}

@media (max-width: 768px) {
  .after-sale-apply-page {
    padding: 16px;
  }
}
</style>
