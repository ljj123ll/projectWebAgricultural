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
          action="#"
          list-type="picture-card"
          :auto-upload="false"
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
import { reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { applyAfterSale } from '@/apis/user';

const router = useRouter();
const route = useRoute();

const form = reactive({
  orderNo: route.params.orderNo,
  type: 'refund',
  reason: '',
  description: ''
});

const submit = async () => {
  if (!form.reason) {
    ElMessage.warning('请选择申请原因');
    return;
  }
  const afterSaleType = form.type === 'refund' ? 1 : (form.type === 'return' ? 2 : 3);
  try {
    const res = await applyAfterSale({
      orderNo: String(form.orderNo),
      afterSaleType,
      applyReason: form.reason,
      proofImgUrls: '' // 当前页面未接入图片上传；可按需要后续完善
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
