<template>
  <div class="products-page">
    <div class="page-header">
      <h2>商品审核</h2>
      <el-radio-group v-model="filterStatus" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="approved">已通过</el-radio-button>
        <el-radio-button label="rejected">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <el-card>
      <el-table :data="productList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.image" class="product-thumb" />
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="所属店铺" />
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <template v-if="row.status === 'pending'">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">拒绝</el-button>
            </template>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const filterStatus = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

const productList = ref([
  {
    id: 1,
    name: '新鲜有机西红柿',
    image: 'https://via.placeholder.com/60',
    shopName: '绿色农场旗舰店',
    category: '新鲜蔬菜/瓜果类',
    price: 5.99,
    submitTime: '2024-03-08 10:30:00',
    status: 'pending'
  },
  {
    id: 2,
    name: '山东红富士苹果',
    image: 'https://via.placeholder.com/60',
    shopName: '山东苹果直销',
    category: '水果/苹果梨桃',
    price: 8.99,
    submitTime: '2024-03-07 14:20:00',
    status: 'approved'
  }
])

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return map[status] || status
}

const viewDetail = (row: any) => {
  void row
}

const approve = (row: any) => {
  row.status = 'approved'
  ElMessage.success('审核通过')
}

const reject = (row: any) => {
  row.status = 'rejected'
  ElMessage.success('已拒绝')
}
</script>

<style scoped lang="scss">
.products-page {
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
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
