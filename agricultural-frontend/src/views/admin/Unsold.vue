<template>
  <div class="unsold-page">
    <div class="page-header">
      <h2>滞销专区管理</h2>
      <el-button type="primary" @click="showAddDialog = true">添加滞销商品 (ID)</el-button>
    </div>

    <!-- 滞销统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="12" :md="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ total }}</div>
          <div class="stat-label">滞销商品数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="unsoldList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="getFullImageUrl(row.productImg)" class="product-thumb" />
              <div class="product-info">
                <span class="name">{{ row.productName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格">
          <template #default="{ row }">
            <span class="sale-price">¥{{ row.price.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" />
        <el-table-column prop="salesVolume" label="销量" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeProduct(row)">移出滞销</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="showAddDialog" title="设置商品为滞销" width="500px">
      <el-form label-position="top">
        <el-form-item label="商品ID">
          <el-input v-model="addProductId" placeholder="请输入商品ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUnsalableProducts, setUnsalable } from '@/apis/admin'
import { getFullImageUrl } from '@/utils/image'

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showAddDialog = ref(false)
const loading = ref(false)
const addProductId = ref('')
const unsoldList = ref<any[]>([])

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getUnsalableProducts({ pageNum: currentPage.value, pageSize: pageSize.value });
    if (res) {
      unsoldList.value = res.list || [];
      total.value = res.total || 0;
    }
  } catch (error) {
    ElMessage.error('加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
})

const removeProduct = (row: any) => {
  ElMessageBox.confirm('确定要移出滞销专区吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await setUnsalable(row.id, 0);
      ElMessage.success('已移出');
      loadData();
    } catch (e) {
      ElMessage.error('操作失败');
    }
  })
}

const saveProduct = async () => {
  if (!addProductId.value) {
    ElMessage.warning('请输入商品ID');
    return;
  }
  try {
    await setUnsalable(Number(addProductId.value), 1);
    ElMessage.success('添加成功');
    showAddDialog.value = false;
    addProductId.value = '';
    loadData();
  } catch (e) {
    ElMessage.error('操作失败');
  }
}
</script>

<style scoped lang="scss">
.unsold-page {
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  margin-bottom: 16px;

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #f56c6c;
    margin-bottom: 8px;
  }

  .stat-label {
    color: #666;
    font-size: 14px;
  }
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .product-thumb {
    width: 50px;
    height: 50px;
    border-radius: 4px;
    object-fit: cover;
  }

  .product-info {
    display: flex;
    flex-direction: column;

    .name {
      font-weight: 500;
    }

    .farmer {
      font-size: 12px;
      color: #999;
    }
  }
}

.original-price {
  text-decoration: line-through;
  color: #999;
}

.sale-price {
  color: #f56c6c;
  font-weight: 600;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
