<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索订单号"
          style="width: 200px"
          clearable
        />
        <el-button type="primary">导出</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="orderList" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="buyer" label="买家" />
        <el-table-column prop="shopName" label="店铺" />
        <el-table-column prop="productCount" label="商品数" width="80" />
        <el-table-column prop="totalAmount" label="订单金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
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

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(500)

const orderList = ref([
  {
    orderNo: 'ORD202403080001',
    buyer: '张三',
    shopName: '绿色农场旗舰店',
    productCount: 3,
    totalAmount: 158.5,
    status: 'completed',
    createTime: '2024-03-08 10:30:00'
  },
  {
    orderNo: 'ORD202403080002',
    buyer: '李四',
    shopName: '山东苹果直销',
    productCount: 2,
    totalAmount: 89.9,
    status: 'pending_shipment',
    createTime: '2024-03-08 09:15:00'
  }
])

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending_payment: 'warning',
    pending_shipment: 'primary',
    pending_receipt: 'success',
    completed: 'success',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending_payment: '待付款',
    pending_shipment: '待发货',
    pending_receipt: '待收货',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

const viewDetail = (row: any) => {
  void row
}
</script>

<style scoped lang="scss">
.orders-page {
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

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
