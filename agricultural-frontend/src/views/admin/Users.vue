<template>
  <div class="users-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户手机号/姓名"
        style="width: 250px"
        clearable
      >
        <template #append>
          <el-button :icon="Search" />
        </template>
      </el-input>
    </div>

    <el-card>
      <el-table :data="userList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="registerTime" label="注册时间" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="totalAmount" label="消费金额" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : 'danger'">
              {{ row.status === 'normal' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button 
              link 
              :type="row.status === 'normal' ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'normal' ? '禁用' : '启用' }}
            </el-button>
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
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

const userList = ref([
  {
    id: 1,
    nickname: '张三',
    phone: '138****8888',
    registerTime: '2024-01-15 10:30:00',
    orderCount: 15,
    totalAmount: 2580.5,
    status: 'normal'
  },
  {
    id: 2,
    nickname: '李四',
    phone: '139****9999',
    registerTime: '2024-02-20 14:20:00',
    orderCount: 8,
    totalAmount: 1280.0,
    status: 'normal'
  },
  {
    id: 3,
    nickname: '王五',
    phone: '137****7777',
    registerTime: '2024-03-01 09:15:00',
    orderCount: 3,
    totalAmount: 450.0,
    status: 'disabled'
  }
])

const viewDetail = (row: any) => {
  void row
}

const toggleStatus = (row: any) => {
  row.status = row.status === 'normal' ? 'disabled' : 'normal'
  ElMessage.success(row.status === 'normal' ? '已启用' : '已禁用')
}
</script>

<style scoped lang="scss">
.users-page {
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

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
