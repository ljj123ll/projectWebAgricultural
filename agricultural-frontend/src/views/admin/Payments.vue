<template>
  <div class="payments-page">
    <div class="page-header">
      <h2>资金管控</h2>
      <el-button type="primary">导出报表</el-button>
    </div>

    <!-- 资金概览 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-value">¥{{ stats.totalBalance.toFixed(2) }}</div>
          <div class="stat-label">平台总余额</div>
        </el-card>
      </el-col>
      <el-col :span="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-value">¥{{ stats.todayIncome.toFixed(2) }}</div>
          <div class="stat-label">今日收入</div>
        </el-card>
      </el-col>
      <el-col :span="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-value">¥{{ stats.pendingSettlement.toFixed(2) }}</div>
          <div class="stat-label">待结算</div>
        </el-card>
      </el-col>
      <el-col :span="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-value">¥{{ stats.subsidyAmount.toFixed(2) }}</div>
          <div class="stat-label">补贴支出</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 交易记录 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>交易记录</span>
          <el-radio-group v-model="filterType" size="small">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="income">收入</el-radio-button>
            <el-radio-button label="expense">支出</el-radio-button>
            <el-radio-button label="subsidy">补贴</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-table :data="transactionList" style="width: 100%">
        <el-table-column prop="id" label="流水号" width="180" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeType(row.type)">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.amount >= 0 ? 'income' : 'expense'">
              {{ row.amount >= 0 ? '+' : '' }}¥{{ Math.abs(row.amount).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" />
        <el-table-column prop="createTime" label="时间" />
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
import { ref, reactive } from 'vue'

const filterType = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(200)

const stats = reactive({
  totalBalance: 1588000.5,
  todayIncome: 25800.0,
  pendingSettlement: 56800.0,
  subsidyAmount: 12500.0
})

const transactionList = ref([
  {
    id: 'TRX202403080001',
    type: 'income',
    amount: 299.0,
    description: '订单收入-ORD202403080001',
    createTime: '2024-03-08 14:30:00'
  },
  {
    id: 'TRX202403080002',
    type: 'subsidy',
    amount: -58.5,
    description: '助农补贴发放',
    createTime: '2024-03-08 12:00:00'
  },
  {
    id: 'TRX202403080003',
    type: 'expense',
    amount: -5000.0,
    description: '商户提现',
    createTime: '2024-03-08 10:30:00'
  }
])

const getTypeType = (type: string) => {
  const map: Record<string, string> = {
    income: 'success',
    expense: 'danger',
    subsidy: 'warning'
  }
  return map[type] || 'info'
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    income: '收入',
    expense: '支出',
    subsidy: '补贴'
  }
  return map[type] || type
}
</script>

<style scoped lang="scss">
.payments-page {
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

.overview-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  margin-bottom: 16px;

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }

  .stat-label {
    color: #666;
    font-size: 14px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.income {
  color: #67c23a;
}

.expense {
  color: #f56c6c;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
