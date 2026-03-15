<template>
  <div class="unsold-page">
    <div class="page-header">
      <h2>滞销专区管理</h2>
      <el-button type="primary" @click="showAddDialog = true">添加滞销商品</el-button>
    </div>

    <!-- 滞销统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="12" :md="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.totalProducts }}</div>
          <div class="stat-label">滞销商品数</div>
        </el-card>
      </el-col>
      <el-col :span="12" :md="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.helpedFarmers }}</div>
          <div class="stat-label">已帮扶农户</div>
        </el-card>
      </el-col>
      <el-col :span="12" :md="8">
        <el-card class="stat-card">
          <div class="stat-value">¥{{ stats.totalSales.toFixed(2) }}</div>
          <div class="stat-label">累计销售额</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="unsoldList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.image" class="product-thumb" />
              <div class="product-info">
                <span class="name">{{ row.name }}</span>
                <span class="farmer">农户：{{ row.farmerName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="originalPrice" label="原价">
          <template #default="{ row }">
            <span class="original-price">¥{{ row.originalPrice.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="助农价">
          <template #default="{ row }">
            <span class="sale-price">¥{{ row.salePrice.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" />
        <el-table-column prop="sold" label="已售" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'selling' ? 'success' : 'info'">
              {{ row.status === 'selling' ? '销售中' : '已售罄' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editProduct(row)">编辑</el-button>
            <el-button link type="danger" @click="removeProduct(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="showAddDialog" :title="isEdit ? '编辑滞销商品' : '添加滞销商品'" width="90%">
      <el-form :model="productForm" label-position="top">
        <el-form-item label="商品名称">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="所属农户">
          <el-input v-model="productForm.farmerName" placeholder="请输入农户姓名" />
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select v-model="productForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="蔬菜" value="vegetables" />
            <el-option label="水果" value="fruits" />
            <el-option label="粮油" value="grains" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="productForm.originalPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="助农价">
          <el-input-number v-model="productForm.salePrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="productForm.stock" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="滞销原因">
          <el-input
            v-model="productForm.reason"
            type="textarea"
            rows="3"
            placeholder="请描述滞销原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(30)
const showAddDialog = ref(false)
const isEdit = ref(false)

const stats = reactive({
  totalProducts: 28,
  helpedFarmers: 15,
  totalSales: 56800.5
})

const productForm = reactive({
  name: '',
  farmerName: '',
  category: '',
  originalPrice: 0,
  salePrice: 0,
  stock: 0,
  reason: ''
})

const unsoldList = ref([
  {
    id: 1,
    name: '滞销大白菜',
    image: 'https://via.placeholder.com/60',
    farmerName: '王大爷',
    category: '蔬菜',
    originalPrice: 2.5,
    salePrice: 1.5,
    stock: 500,
    sold: 156,
    status: 'selling'
  },
  {
    id: 2,
    name: '滞销苹果',
    image: 'https://via.placeholder.com/60',
    farmerName: '李大叔',
    category: '水果',
    originalPrice: 8.0,
    salePrice: 5.0,
    stock: 0,
    sold: 300,
    status: 'soldout'
  }
])

const editProduct = (row: any) => {
  isEdit.value = true
  Object.assign(productForm, row)
  showAddDialog.value = true
}

const removeProduct = (row: any) => {
  ElMessageBox.confirm('确定要下架这个商品吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    unsoldList.value = unsoldList.value.filter(item => item.id !== row.id)
    ElMessage.success('已下架')
  })
}

const saveProduct = () => {
  ElMessage.success(isEdit.value ? '修改成功' : '添加成功')
  showAddDialog.value = false
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
