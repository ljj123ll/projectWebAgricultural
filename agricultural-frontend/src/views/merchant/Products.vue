<template>
  <div class="merchant-products">
    <div class="toolbar">
      <el-button type="primary" @click="router.push('/merchant/product-add')">发布商品</el-button>
    </div>
    <el-table :data="productList" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="商品图片" width="100">
        <template #default="scope">
          <el-image :src="getFullImageUrl(scope.row.productImg?.split(',')[0])" style="width: 50px; height: 50px" />
        </template>
      </el-table-column>
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="salesVolume" label="销量" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button 
            size="small" 
            :type="scope.row.status === 1 ? 'warning' : 'success'"
            @click="handleToggleStatus(scope.row)"
          >
            {{ scope.row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { Product } from '@/types';
import { listProducts, updateProductStatus, deleteProduct } from '@/apis/merchant';
import { getFullImageUrl } from '@/utils/image';

const router = useRouter();
const productList = ref<Product[]>([]);

const getStatusText = (status?: number) => {
  if (status === 0) return '待审核';
  if (status === 1) return '已上架';
  if (status === 2) return '已下架';
  if (status === 3) return '已驳回';
  return '未知';
};

const getStatusType = (status?: number) => {
  if (status === 1) return 'success';
  if (status === 2) return 'info';
  return 'warning';
};

const handleEdit = (row: Product) => {
  // 由于后端暂无详情接口，将当前行数据通过 state 传递给编辑页
  router.push({
    path: `/merchant/product-edit/${row.id}`,
    state: { productData: JSON.stringify(row) }
  });
};

const handleToggleStatus = async (row: Product) => {
  const status = row.status === 1 ? 2 : 1;
  await updateProductStatus(row.id, status);
  row.status = status;
  ElMessage.success(row.status === 1 ? '上架成功' : '下架成功');
};

const handleDelete = async (row: Product) => {
  await deleteProduct(row.id);
  ElMessage.success('删除成功');
  loadList();
};

const loadList = async () => {
  const res = await listProducts({ pageNum: 1, pageSize: 20 });
  if (res?.list) productList.value = res.list;
};



onMounted(() => {
  loadList();
});
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}
</style>
