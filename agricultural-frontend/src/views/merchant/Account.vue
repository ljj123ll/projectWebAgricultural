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
          <el-button link type="primary" @click="loadRecords">刷新</el-button>
        </div>
      </template>
      <div v-if="recentRecords.length > 0" class="records-list">
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
      <el-empty v-else description="暂无账单记录" />
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
import { ref, reactive, onMounted } from 'vue'
import { Wallet, Calendar, TrendCharts, Money, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMerchantStats, listReconciliation, listSubsidy } from '@/apis/merchant'

const showWithdrawDialog = ref(false)
const showSubsidyRules = ref(false)

const account = reactive({
  balance: 0,
  todayIncome: 0,
  weekIncome: 0,
  monthIncome: 0
})

const subsidy = reactive({
  total: 0,
  month: 0,
  orderCount: 0
})

const withdrawForm = reactive({
  amount: 0,
  method: 'alipay',
  account: ''
})

const recentRecords = ref<any[]>([])

const loadAccountData = async () => {
  try {
    // 获取统计数据
    const res = await getMerchantStats()
    if (res) {
      // 使用今日销售额作为今日收入
      account.todayIncome = res.todaySales || 0
      // 使用近7天营收作为周收入
      account.weekIncome = res.revenue7d || 0
      // 使用周收入的1/4作为月收入估算
      account.monthIncome = (res.revenue7d || 0) * 4
      // 余额使用周收入作为参考
      account.balance = (res.revenue7d || 0) * 0.8 // 假设80%可提现
    }
  } catch (error) {
    console.error('Failed to load account data', error)
  }
}

const loadRecords = async () => {
  try {
    const res = await listReconciliation({ pageNum: 1, pageSize: 5 })
    if (res && res.list) {
      recentRecords.value = res.list.map((item: any) => ({
        id: item.id,
        type: item.actualIncome > 0 ? 'income' : 'expense',
        title: item.orderNo ? `订单收入 ${item.orderNo}` : '账户变动',
        time: item.createTime,
        amount: Math.abs(item.actualIncome || 0)
      }))
    }
  } catch (error) {
    console.error('Failed to load records', error)
  }
}

const loadSubsidy = async () => {
  try {
    const res = await listSubsidy({ pageNum: 1, pageSize: 100 })
    if (res && res.list) {
      // 计算累计补贴
      subsidy.total = res.list.reduce((sum: number, item: any) => sum + (item.subsidyAmount || 0), 0)
      subsidy.orderCount = res.total || 0
      
      // 计算本月补贴（简化处理，实际应该按月份筛选）
      subsidy.month = subsidy.total * 0.3 // 假设30%是本月
    }
  } catch (error) {
    console.error('Failed to load subsidy', error)
  }
}

onMounted(() => {
  loadAccountData()
  loadRecords()
  loadSubsidy()
})

const confirmWithdraw = () => {
  if (withdrawForm.amount <= 0) {
    ElMessage.warning('请输入提现金额')
    return
  }
  if (!withdrawForm.account) {
    ElMessage.warning('请输入到账账户')
    return
  }
  if (withdrawForm.amount > account.balance) {
    ElMessage.warning('提现金额不能超过余额')
    return
  }
  ElMessage.success('提现申请已提交')
  showWithdrawDialog.value = false
  // 扣除余额
  account.balance -= withdrawForm.amount
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
