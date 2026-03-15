<template>
  <div class="logs-page">
    <div class="page-header">
      <h2>系统日志</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="small"
        />
        <el-button type="primary" size="small">导出</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="logList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="message" label="日志内容" min-width="300" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createTime" label="时间" width="160" />
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

const dateRange = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(1000)

const logList = ref([
  {
    id: 1,
    level: 'INFO',
    module: '用户管理',
    message: '管理员登录成功',
    operator: 'admin',
    ip: '192.168.1.100',
    createTime: '2024-03-08 15:30:00'
  },
  {
    id: 2,
    level: 'WARN',
    module: '订单管理',
    message: '订单ORD202403080001支付超时',
    operator: 'system',
    ip: '-',
    createTime: '2024-03-08 15:25:00'
  },
  {
    id: 3,
    level: 'ERROR',
    module: '支付系统',
    message: '微信支付接口调用失败',
    operator: 'system',
    ip: '-',
    createTime: '2024-03-08 15:20:00'
  }
])

const getLevelType = (level: string) => {
  const map: Record<string, string> = {
    INFO: 'info',
    WARN: 'warning',
    ERROR: 'danger'
  }
  return map[level] || 'info'
}
</script>

<style scoped lang="scss">
.logs-page {
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
