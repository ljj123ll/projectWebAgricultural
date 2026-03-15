<template>
  <div class="account-page">
    <div class="page-header">
      <h2>对账与补贴</h2>
    </div>

    <!-- 账户概览 -->
    <el-card class="overview-card">
      <div class="account-overview">
        <div class="balance-section">
          <div class="balance-item">
            <span class="label">账户余额</span>
            <span class="amount">¥{{ account.balance.toFixed(2) }}</span>
          </div>
          <div class="balance-actions">
            <el-button type="primary" @click="showWithdrawDialog = true">提现</el-button>
            <el-button @click="$router.push('/merchant/account/records')">明细</el-button>
          </div>
        </div>
        <el-divider />
        <div class="income-stats">
          <div class="stat-item">
            <span class="label">今日收入</span>
            <span class="amount">¥{{ account.todayIncome.toFixed(2) }}</span>
          </div>
          <div class="stat-item">
            <span class="label">本周收入</span>
            <span class="amount">¥{{ account.weekIncome.toFixed(2) }}</span>
          </div>
          <div class="stat-item">
            <span class="label">本月收入</span>
            <span class="amount">¥{{ account.monthIncome.toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 助农补贴 -->
    <el-card class="subsidy-card">
      <template #header>
        <div class="card-header">
          <span>助农补贴</span>
          <el-button link type="primary" @click="showSubsidyRules = true">补贴规则</el-button>
        </div>
      </template>
      <div class="subsidy-overview">
        <div class="subsidy-item">
          <div class="subsidy-icon">
            <el-icon :size="32" color="#67c23a"><Wallet /></el-icon>
          </div>
          <div class="subsidy-info">
            <span class="label">累计获得补贴</span>
            <span class="amount">¥{{ subsidy.total.toFixed(2) }}</span>
          </div>
        </div>
        <div class="subsidy-item">
          <div class="subsidy-icon">
            <el-icon :size="32" color="#409eff"><Calendar /></el-icon>
          </div>
          <div class="subsidy-info">
            <span class="label">本月补贴</span>
            <span class="amount">¥{{ subsidy.month.toFixed(2) }}</span>
          </div>
        </div>
        <div class="subsidy-item">
          <div class="subsidy-icon">
            <el-icon :size="32" color="#e6a23c"><TrendCharts /></el-icon>
          </div>
          <div class="subsidy-info">
            <span class="label">补贴订单数</span>
            <span class="amount">{{ subsidy.orderCount }}单</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 近期账单 -->
    <el-card class="records-card">
      <template #header>
        <div class="card-header">
          <span>近期账单</span>
          <el-button link type="primary" @click="$router.push('/merchant/account/records')">查看全部</el-button>
        </div>
      </template>
      <div class="records-list">
        <div v-for="record in recentRecords" :key="record.id" class="record-item">
          <div class="record-info">
            <div class="record-icon" :class="record.type">
              <el-icon :size="20">
                <component :is="record.type === 'income' ? Money : ShoppingCart" />
              </el-icon>
            </div>
            <div class="record-detail">
              <h4>{{ record.title }}</h4>
              <p class="record-time">{{ record.time }}</p>
            </div>
          </div>
          <div class="record-amount" :class="record.type">
            {{ record.type === 'income' ? '+' : '-' }}¥{{ record.amount.toFixed(2) }}
          </div>
        </div>
      </div>
    </el-card>

    <!-- 提现弹窗 -->
    <el-dialog v-model="showWithdrawDialog" title="提现" width="90%">
      <el-form :model="withdrawForm" label-position="top">
        <el-form-item label="提现金额">
          <el-input-number v-model="withdrawForm.amount" :min="1" :max="account.balance" :precision="2" style="width: 100%">
            <template #append>元</template>
          </el-input-number>
          <div class="balance-hint">
            可提现余额：¥{{ account.balance.toFixed(2) }}
            <el-button link type="primary" @click="withdrawForm.amount = account.balance">全部提现</el-button>
          </div>
        </el-form-item>
        <el-form-item label="提现方式">
          <el-radio-group v-model="withdrawForm.method">
            <el-radio label="alipay">支付宝</el-radio>
            <el-radio label="bank">银行卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="到账账户">
          <el-input v-model="withdrawForm.account" placeholder="请输入支付宝账号或银行卡号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showWithdrawDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmWithdraw">确认提现</el-button>
      </template>
    </el-dialog>

    <!-- 补贴规则弹窗 -->
    <el-dialog v-model="showSubsidyRules" title="助农补贴规则" width="90%">
      <div class="subsidy-rules">
        <h4>补贴对象</h4>
        <p>符合条件的农业合作社、家庭农场、种植大户</p>
        
        <h4>补贴标准</h4>
        <ul>
          <li>订单金额满100元，补贴5%</li>
          <li>订单金额满500元，补贴8%</li>
          <li>订单金额满1000元，补贴10%</li>
        </ul>
        
        <h4>补贴发放</h4>
        <p>补贴将于订单完成后7个工作日内发放至账户余额</p>
        
        <h4>注意事项</h4>
        <ul>
          <li>仅限农产品类目订单</li>
          <li>退款订单不享受补贴</li>
          <li>每月补贴上限5000元</li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Wallet, Calendar, TrendCharts, Money, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const showWithdrawDialog = ref(false)
const showSubsidyRules = ref(false)

const account = reactive({
  balance: 15880.5,
  todayIncome: 2580.0,
  weekIncome: 15800.0,
  monthIncome: 56800.0
})

const subsidy = reactive({
  total: 5680.0,
  month: 1200.0,
  orderCount: 156
})

const withdrawForm = reactive({
  amount: 0,
  method: 'alipay',
  account: ''
})

const recentRecords = ref([
  {
    id: 1,
    type: 'income',
    title: '订单收入-ORD202403080001',
    time: '2024-03-08 14:30:00',
    amount: 299.0
  },
  {
    id: 2,
    type: 'income',
    title: '订单收入-ORD202403080002',
    time: '2024-03-08 12:15:00',
    amount: 158.0
  },
  {
    id: 3,
    type: 'subsidy',
    title: '助农补贴',
    time: '2024-03-08 10:00:00',
    amount: 58.5
  },
  {
    id: 4,
    type: 'expense',
    title: '提现到支付宝',
    time: '2024-03-07 16:20:00',
    amount: 5000.0
  },
  {
    id: 5,
    type: 'income',
    title: '订单收入-ORD202403070015',
    time: '2024-03-07 11:30:00',
    amount: 899.0
  }
])

const confirmWithdraw = () => {
  if (withdrawForm.amount <= 0) {
    ElMessage.warning('请输入提现金额')
    return
  }
  if (!withdrawForm.account) {
    ElMessage.warning('请输入到账账户')
    return
  }
  ElMessage.success('提现申请已提交')
  showWithdrawDialog.value = false
}
</script>

<style scoped lang="scss">
.account-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
  }
}

.overview-card,
.subsidy-card,
.records-card {
  margin-bottom: 20px;
}

.account-overview {
  .balance-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    .balance-item {
      display: flex;
      flex-direction: column;

      .label {
        color: #666;
        font-size: 14px;
        margin-bottom: 8px;
      }

      .amount {
        font-size: 32px;
        font-weight: 600;
        color: #333;
      }
    }
  }

  .income-stats {
    display: flex;
    justify-content: space-around;
    padding-top: 16px;

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .label {
        color: #999;
        font-size: 12px;
        margin-bottom: 8px;
      }

      .amount {
        font-size: 18px;
        font-weight: 600;
        color: #333;
      }
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subsidy-overview {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 16px;

  .subsidy-item {
    display: flex;
    align-items: center;
    gap: 12px;

    .subsidy-icon {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .subsidy-info {
      display: flex;
      flex-direction: column;

      .label {
        color: #999;
        font-size: 12px;
        margin-bottom: 4px;
      }

      .amount {
        font-size: 20px;
        font-weight: 600;
        color: #333;
      }
    }
  }
}

.records-list {
  .record-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 0;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
    }

    .record-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .record-icon {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        &.income {
          background: #f0f9ff;
          color: #409eff;
        }

        &.expense {
          background: #fef0f0;
          color: #f56c6c;
        }

        &.subsidy {
          background: #f0f9eb;
          color: #67c23a;
        }
      }

      .record-detail {
        h4 {
          margin: 0 0 4px 0;
          font-size: 14px;
          font-weight: normal;
        }

        .record-time {
          color: #999;
          font-size: 12px;
          margin: 0;
        }
      }
    }

    .record-amount {
      font-size: 16px;
      font-weight: 600;

      &.income {
        color: #f56c6c;
      }

      &.expense {
        color: #67c23a;
      }
    }
  }
}

.balance-hint {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
}

.subsidy-rules {
  h4 {
    margin: 20px 0 12px 0;
    color: #333;

    &:first-child {
      margin-top: 0;
    }
  }

  p {
    color: #666;
    line-height: 1.6;
    margin: 0;
  }

  ul {
    margin: 0;
    padding-left: 20px;
    color: #666;
    line-height: 1.8;
  }
}
</style>
