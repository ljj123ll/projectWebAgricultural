<template>
  <div class="admin-audit">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="商家审核" name="merchant"></el-tab-pane>
      <el-tab-pane label="商品审核" name="product"></el-tab-pane>
    </el-tabs>

    <div v-if="activeTab === 'merchant'">
        <el-table :data="merchantAuditList" style="width: 100%">
            <el-table-column prop="merchantName" label="商家名称" />
            <el-table-column prop="contactPerson" label="联系人" />
            <el-table-column prop="phone" label="电话" />
            <el-table-column label="资质图片">
                <template #default="scope">
                    <el-image :src="scope.row.qualificationImg" style="width: 50px; height: 50px" :preview-src-list="[scope.row.qualificationImg]" />
                </template>
            </el-table-column>
            <el-table-column label="操作">
                <template #default="scope">
                    <el-button type="success" size="small" @click="handleAudit(scope.row, true)">通过</el-button>
                    <el-button type="danger" size="small" @click="handleAudit(scope.row, false)">驳回</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>

    <div v-else>
         <el-table :data="productAuditList" style="width: 100%">
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column prop="merchantName" label="所属商家" />
            <el-table-column prop="price" label="价格" />
            <el-table-column label="商品图片">
                <template #default="scope">
                    <el-image :src="scope.row.productImg" style="width: 50px; height: 50px" :preview-src-list="[scope.row.productImg]" />
                </template>
            </el-table-column>
            <el-table-column label="操作">
                <template #default="scope">
                    <el-button type="success" size="small" @click="handleAudit(scope.row, true)">通过</el-button>
                    <el-button type="danger" size="small" @click="handleAudit(scope.row, false)">驳回</el-button>
                </template>
            </el-table-column>
        </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listMerchantAudit, auditMerchant, listProductAudit, auditProduct } from '@/apis/admin';

const activeTab = ref('merchant');

const merchantAuditList = ref<any[]>([]);
const productAuditList = ref<any[]>([]);

const handleAudit = (row: any, pass: boolean) => {
    ElMessageBox.prompt(pass ? '确认通过审核吗？' : '请输入驳回原因', '审核', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: pass ? 'none' : 'textarea'
    }).then((result) => {
        const reason = (result as { value?: string }).value || '';
        if (activeTab.value === 'merchant') {
            auditMerchant(row.id, { pass, reason }).then(() => {
                ElMessage.success('操作成功');
                loadMerchant();
            });
        } else {
            auditProduct(row.id, { pass, reason }).then(() => {
                ElMessage.success('操作成功');
                loadProduct();
            });
        }
    });
};

const loadMerchant = async () => {
  const res = await listMerchantAudit({ auditStatus: 0, pageNum: 1, pageSize: 20 });
  if (res?.list) merchantAuditList.value = res.list;
};

const loadProduct = async () => {
  const res = await listProductAudit({ status: 0, pageNum: 1, pageSize: 20 });
  if (res?.list) productAuditList.value = res.list;
};

onMounted(() => {
  loadMerchant();
  loadProduct();
});
</script>
