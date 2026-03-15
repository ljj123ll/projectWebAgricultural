<template>
  <div class="merchant-products">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">发布商品</el-button>
    </div>
    <el-table :data="productList" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="商品图片" width="100">
        <template #default="scope">
          <el-image :src="scope.row.productImg" style="width: 50px; height: 50px" />
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

    <!-- 添加/编辑商品弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '发布商品'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="商品名称">
          <el-input v-model="form.productName" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="产地">
          <el-input v-model="form.originPlace" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.productDesc" type="textarea" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.productImg" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import type { Product } from '@/types';
import { listProducts, createProduct, updateProduct, updateProductStatus, deleteProduct } from '@/apis/merchant';

const productList = ref<Product[]>([]);

const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive<Partial<Product>>({});

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

const handleAdd = () => {
  isEdit.value = false;
  Object.assign(form, {
    productName: '',
    price: 0,
    stock: 0,
    productImg: '',
    productDesc: '',
    originPlace: ''
  });
  dialogVisible.value = true;
};

const handleEdit = (row: Product) => {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!isEdit.value) {
    await createProduct(form);
    ElMessage.success('发布成功');
  } else if (form.id) {
    await updateProduct(form.id as number, form);
    ElMessage.success('修改成功');
  }
  dialogVisible.value = false;
  loadList();
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
