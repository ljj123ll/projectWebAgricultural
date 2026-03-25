<template>
  <div class="account-page">
    <div class="page-header">
      <h2>对账与补贴</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openWithdrawDialog">申请提现</el-button>
        <el-button plain @click="refreshAll">刷新全部</el-button>
      </div>
    </div>

    <el-card class="receiving-card">
      <template #header>
        <div class="card-header">
          <span>收款账户（1分钱验证 + 管理员审核）</span>
          <el-button link type="primary" @click="loadAccounts">刷新</el-button>
        </div>
      </template>

      <el-form :model="accountForm" inline class="account-form">
        <el-form-item label="账户类型">
          <el-select v-model="accountForm.accountType" style="width: 150px">
            <el-option label="银行卡" :value="1" />
            <el-option label="支付宝" :value="2" />
            <el-option label="微信收款" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户名称">
          <el-input v-model="accountForm.accountName" placeholder="请输入账户户名" style="width: 220px" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="accountForm.accountNo" placeholder="请输入收款账号" style="width: 260px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveReceivingAccount">保存账户</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        class="account-alert"
        :title="overview.hasPassedReceivingAccount ? '当前已有可用收款账户，自动打款将优先使用该账户。' : '当前没有通过审核的收款账户，自动打款可能失败，请尽快完成验证并提交审核。'"
        :type="overview.hasPassedReceivingAccount ? 'success' : 'warning'"
        :closable="false"
      >
        <template #default>
          <span v-if="overview.hasPassedReceivingAccount">
            已通过账户：{{ approvedAccountText }}
          </span>
          <span v-else>流程建议：保存账户 → 发起验证 → 确认到账金额 → 提交审核</span>
        </template>
      </el-alert>

      <el-table :data="accountList" v-loading="accountLoading">
        <el-table-column prop="accountType" label="类型" width="110">
          <template #default="{ row }">{{ accountTypeText(row.accountType) }}</template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户名称" width="160" />
        <el-table-column prop="accountNo" label="账号" min-width="220" />
        <el-table-column prop="verifyStatus" label="验证状态" width="120">
          <template #default="{ row }">
            <el-tag :type="verifyTagType(row.verifyStatus)">{{ verifyText(row.verifyStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="180">
          <template #default="{ row }">
            <span>{{ row.rejectReason || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="startVerify(row)">发起验证</el-button>
            <el-button link type="success" @click="openConfirmVerify(row)">确认金额</el-button>
            <el-button link type="warning" @click="submitAudit(row)">提交审核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="overview-card">
      <template #header>
        <div class="card-header">
          <span>资金总览</span>
          <div class="status-tags">
            <el-tag type="info">待打款 {{ overview.pendingTransferCount }}</el-tag>
            <el-tag type="danger">失败待重试 {{ overview.failedTransferCount }}</el-tag>
            <el-tag type="warning">人工兜底 {{ overview.manualFallbackCount }}</el-tag>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="metric-card primary">
          <p class="label">可见余额（打款+补贴）</p>
          <p class="amount">¥{{ toMoney(overview.balance) }}</p>
          <p class="sub">累计实收 ¥{{ toMoney(overview.totalIncome) }}，服务费 ¥{{ toMoney(overview.totalServiceFee) }}</p>
        </div>
        <div class="metric-card">
          <p class="label">今日入账</p>
          <p class="amount">¥{{ toMoney(overview.todayIncome) }}</p>
        </div>
        <div class="metric-card">
          <p class="label">近7天入账</p>
          <p class="amount">¥{{ toMoney(overview.weekIncome) }}</p>
        </div>
        <div class="metric-card">
          <p class="label">本月入账</p>
          <p class="amount">¥{{ toMoney(overview.monthIncome) }}</p>
        </div>
        <div class="metric-card">
          <p class="label">可提现金额</p>
          <p class="amount">¥{{ toMoney(overview.withdrawAvailableAmount) }}</p>
        </div>
        <div class="metric-card">
          <p class="label">提现冻结金额</p>
          <p class="amount">¥{{ toMoney(overview.withdrawFrozenAmount) }}</p>
        </div>
        <div class="metric-card">
          <p class="label">累计已提现</p>
          <p class="amount">¥{{ toMoney(overview.withdrawSuccessAmount) }}</p>
        </div>
      </div>
    </el-card>

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
            <el-icon :size="28" color="#67c23a"><Wallet /></el-icon>
          </div>
          <div class="subsidy-info">
            <span class="label">累计已发放</span>
            <span class="amount">¥{{ toMoney(overview.subsidyTotal) }}</span>
          </div>
        </div>
        <div class="subsidy-item">
          <div class="subsidy-icon">
            <el-icon :size="28" color="#409eff"><Calendar /></el-icon>
          </div>
          <div class="subsidy-info">
            <span class="label">本月已发放</span>
            <span class="amount">¥{{ toMoney(overview.subsidyMonth) }}</span>
          </div>
        </div>
        <div class="subsidy-item">
          <div class="subsidy-icon">
            <el-icon :size="28" color="#e6a23c"><TrendCharts /></el-icon>
          </div>
          <div class="subsidy-info">
            <span class="label">补贴记录</span>
            <span class="amount">{{ overview.subsidyOrderCount }} 笔</span>
            <span class="subtext">待处理 {{ overview.subsidyPendingCount }}，驳回 {{ overview.subsidyRejectedCount }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="records-card">
      <template #header>
        <div class="card-header">
          <span>近期资金动态</span>
          <el-button link type="primary" @click="loadRecentRecords">刷新</el-button>
        </div>
      </template>
      <div v-if="recentRecords.length > 0" class="records-list">
        <div v-for="record in recentRecords" :key="record.id" class="record-item">
          <div class="record-info">
            <div class="record-icon" :class="record.kind">
              <el-icon :size="20">
                <component :is="record.kind === 'pending' ? Clock : Money" />
              </el-icon>
            </div>
            <div class="record-detail">
              <h4>{{ record.title }}</h4>
              <p class="record-time">{{ record.timeText }} · {{ record.statusText }}</p>
            </div>
          </div>
          <div class="record-amount" :class="record.kind">
            {{ record.kind === 'withdraw' ? '-' : record.kind === 'pending' ? '' : '+' }}¥{{ toMoney(record.amount) }}
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无资金动态" />
    </el-card>

    <el-card class="detail-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="对账明细" name="reconciliation">
          <div class="filter-bar">
            <el-input
              v-model="reconciliationQuery.keyword"
              placeholder="按订单号搜索"
              style="width: 220px"
              clearable
            />
            <el-select
              v-model="reconciliationQuery.transferStatus"
              placeholder="打款状态"
              style="width: 160px"
              clearable
            >
              <el-option label="待打款" :value="0" />
              <el-option label="已打款" :value="1" />
              <el-option label="失败待重试" :value="2" />
              <el-option label="人工兜底" :value="3" />
            </el-select>
            <el-date-picker
              v-model="reconciliationQuery.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 280px"
              clearable
            />
            <el-button type="primary" @click="searchReconciliation">查询</el-button>
            <el-button @click="resetReconciliation">重置</el-button>
          </div>

          <el-table :data="reconciliationList" v-loading="reconciliationLoading">
            <el-table-column prop="orderNo" label="订单号" min-width="170" />
            <el-table-column prop="orderAmount" label="订单金额" width="120">
              <template #default="{ row }">¥{{ toMoney(row.orderAmount) }}</template>
            </el-table-column>
            <el-table-column prop="serviceFee" label="服务费" width="110">
              <template #default="{ row }">¥{{ toMoney(row.serviceFee) }}</template>
            </el-table-column>
            <el-table-column prop="actualIncome" label="实收金额" width="120">
              <template #default="{ row }">¥{{ toMoney(row.actualIncome) }}</template>
            </el-table-column>
            <el-table-column prop="transferStatus" label="打款状态" width="120">
              <template #default="{ row }">
                <el-tag :type="transferStatusTag(row.transferStatus)">{{ transferStatusText(row.transferStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="retryCount" label="重试次数" width="100" />
            <el-table-column prop="transferNo" label="打款流水号" min-width="170" />
            <el-table-column prop="transferTime" label="打款时间" width="170">
              <template #default="{ row }">{{ row.transferTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="记录时间" width="170" />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="reconciliationQuery.pageNum"
              v-model:page-size="reconciliationQuery.pageSize"
              :total="reconciliationTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="onReconciliationSizeChange"
              @current-change="loadReconciliation"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="补贴明细" name="subsidy">
          <div class="filter-bar">
            <el-input
              v-model="subsidyQuery.keyword"
              placeholder="按订单号搜索"
              style="width: 220px"
              clearable
            />
            <el-select
              v-model="subsidyQuery.auditStatus"
              placeholder="审核状态"
              style="width: 160px"
              clearable
            >
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="已驳回" :value="2" />
            </el-select>
            <el-select
              v-model="subsidyQuery.grantStatus"
              placeholder="发放状态"
              style="width: 160px"
              clearable
            >
              <el-option label="待发放" :value="0" />
              <el-option label="已发放" :value="1" />
            </el-select>
            <el-date-picker
              v-model="subsidyQuery.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 280px"
              clearable
            />
            <el-button type="primary" @click="searchSubsidy">查询</el-button>
            <el-button @click="resetSubsidy">重置</el-button>
          </div>

          <el-table :data="subsidyList" v-loading="subsidyLoading">
            <el-table-column prop="orderNo" label="订单号" min-width="170" />
            <el-table-column prop="subsidyType" label="补贴类型" width="130">
              <template #default="{ row }">{{ row.subsidyType || '-' }}</template>
            </el-table-column>
            <el-table-column prop="subsidyAmount" label="补贴金额" width="120">
              <template #default="{ row }">¥{{ toMoney(row.subsidyAmount) }}</template>
            </el-table-column>
            <el-table-column prop="auditStatus" label="审核状态" width="120">
              <template #default="{ row }">
                <el-tag :type="subsidyAuditTag(row.auditStatus)">{{ subsidyAuditText(row.auditStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="grantStatus" label="发放状态" width="120">
              <template #default="{ row }">
                <el-tag :type="subsidyGrantTag(row.grantStatus)">{{ subsidyGrantText(row.grantStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rejectReason" label="驳回原因" min-width="180">
              <template #default="{ row }">{{ row.rejectReason || '-' }}</template>
            </el-table-column>
            <el-table-column prop="grantTime" label="发放时间" width="170">
              <template #default="{ row }">{{ row.grantTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="记录时间" width="170" />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="subsidyQuery.pageNum"
              v-model:page-size="subsidyQuery.pageSize"
              :total="subsidyTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="onSubsidySizeChange"
              @current-change="loadSubsidyList"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="提现记录" name="withdraw">
          <div class="filter-bar">
            <el-input
              v-model="withdrawQuery.keyword"
              placeholder="按提现单号搜索"
              style="width: 220px"
              clearable
            />
            <el-select
              v-model="withdrawQuery.status"
              placeholder="提现状态"
              style="width: 180px"
              clearable
            >
              <el-option label="待审核" :value="0" />
              <el-option label="待打款" :value="1" />
              <el-option label="已驳回" :value="2" />
              <el-option label="打款成功" :value="3" />
              <el-option label="失败待重试" :value="4" />
              <el-option label="失败人工处理" :value="5" />
              <el-option label="已取消" :value="6" />
            </el-select>
            <el-date-picker
              v-model="withdrawQuery.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 280px"
              clearable
            />
            <el-button type="primary" @click="searchWithdraw">查询</el-button>
            <el-button @click="resetWithdraw">重置</el-button>
          </div>

          <el-table :data="withdrawList" v-loading="withdrawLoading">
            <el-table-column prop="withdrawNo" label="提现单号" min-width="180" />
            <el-table-column prop="accountType" label="账户类型" width="110">
              <template #default="{ row }">{{ accountTypeText(row.accountType) }}</template>
            </el-table-column>
            <el-table-column prop="accountName" label="账户名称" width="130" />
            <el-table-column prop="accountNo" label="收款账号" min-width="160" />
            <el-table-column prop="applyAmount" label="申请金额" width="120">
              <template #default="{ row }">¥{{ toMoney(row.applyAmount) }}</template>
            </el-table-column>
            <el-table-column prop="actualAmount" label="到账金额" width="120">
              <template #default="{ row }">¥{{ toMoney(row.actualAmount) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="withdrawStatusTag(row.status)">{{ withdrawStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="retryCount" label="重试次数" width="90" />
            <el-table-column prop="auditRemark" label="审核备注" min-width="170">
              <template #default="{ row }">{{ row.auditRemark || '-' }}</template>
            </el-table-column>
            <el-table-column prop="failReason" label="失败原因" min-width="180">
              <template #default="{ row }">{{ row.failReason || '-' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" width="170" />
            <el-table-column prop="transferTime" label="打款时间" width="170">
              <template #default="{ row }">{{ row.transferTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 0"
                  link
                  type="danger"
                  @click="cancelWithdrawRow(row)"
                >
                  取消
                </el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="withdrawQuery.pageNum"
              v-model:page-size="withdrawQuery.pageSize"
              :total="withdrawTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="onWithdrawSizeChange"
              @current-change="loadWithdrawals"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showSubsidyRules" title="助农补贴规则" width="620px">
      <div class="subsidy-rules">
        <h4>补贴对象</h4>
        <p>符合平台助农标准的农产品商家，可按订单规则享受补贴。</p>

        <h4>补贴发放流程</h4>
        <ul>
          <li>平台生成补贴记录后进入审核</li>
          <li>审核通过后进入待发放</li>
          <li>发放完成后显示在补贴明细中</li>
        </ul>

        <h4>不发放情形</h4>
        <ul>
          <li>订单退款或售后未结案</li>
          <li>补贴审核未通过</li>
          <li>商家账户异常</li>
        </ul>
      </div>
    </el-dialog>

    <el-dialog v-model="verifyAmountDialogVisible" title="确认1分钱验证金额" width="420px">
      <el-form :model="verifyAmountForm" label-position="top">
        <el-form-item label="到账金额（元）">
          <el-input-number
            v-model="verifyAmountForm.amount"
            :min="0"
            :step="0.01"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyAmountDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitConfirmVerify">确认验证</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="withdrawDialogVisible" title="申请提现" width="460px">
      <el-form :model="withdrawForm" label-position="top">
        <el-form-item label="收款账户">
          <el-select
            v-model="withdrawForm.accountId"
            style="width: 100%"
            placeholder="请选择已通过验证并审核的收款账户"
          >
            <el-option
              v-for="item in approvedAccounts"
              :key="item.id"
              :label="`${accountTypeText(item.accountType)} / ${item.accountName} / ${item.accountNo}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="提现金额（元）">
          <el-input-number
            v-model="withdrawForm.amount"
            :min="1"
            :max="withdrawMaxAmount"
            :step="10"
            :precision="2"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <div class="withdraw-tip">
        当前可提现金额：¥{{ toMoney(overview.withdrawAvailableAmount) }}
      </div>
      <template #footer>
        <el-button @click="withdrawDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="withdrawSubmitting" @click="submitWithdraw">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Wallet, Calendar, TrendCharts, Money, Clock } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listReconciliation,
  listSubsidy,
  listAccounts,
  saveAccount,
  initAccountVerify,
  confirmAccountVerify,
  submitAccountAudit,
  getMerchantAccountOverview,
  getWithdrawAvailable,
  applyWithdraw,
  listWithdrawals,
  cancelWithdraw
} from '@/apis/merchant'
import type {
  MerchantAccount,
  MerchantAccountOverview,
  ReconciliationRecord,
  SubsidyRecord,
  MerchantWithdrawRecord
} from '@/types'

interface RecentRecord {
  id: string
  kind: 'income' | 'subsidy' | 'pending' | 'withdraw'
  title: string
  statusText: string
  timeText: string
  amount: number
  ts: number
}

const accountLoading = ref(false)
const accountList = ref<MerchantAccount[]>([])
const verifyAmountDialogVisible = ref(false)
const verifyingAccountId = ref<number | null>(null)
const showSubsidyRules = ref(false)
const activeTab = ref('reconciliation')
const withdrawDialogVisible = ref(false)
const withdrawSubmitting = ref(false)

const overview = ref<MerchantAccountOverview>({
  balance: 0,
  totalIncome: 0,
  totalServiceFee: 0,
  todayIncome: 0,
  weekIncome: 0,
  monthIncome: 0,
  pendingTransferCount: 0,
  failedTransferCount: 0,
  manualFallbackCount: 0,
  subsidyTotal: 0,
  subsidyMonth: 0,
  subsidyOrderCount: 0,
  subsidyPendingCount: 0,
  subsidyRejectedCount: 0,
  hasPassedReceivingAccount: false,
  withdrawAvailableAmount: 0,
  withdrawFrozenAmount: 0,
  withdrawSuccessAmount: 0
})

const accountForm = reactive({
  accountType: 1,
  accountName: '',
  accountNo: ''
})

const verifyAmountForm = reactive({ amount: 0 })

const reconciliationLoading = ref(false)
const reconciliationList = ref<ReconciliationRecord[]>([])
const reconciliationTotal = ref(0)
const reconciliationQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  transferStatus: undefined as number | undefined,
  dateRange: [] as string[]
})

const subsidyLoading = ref(false)
const subsidyList = ref<SubsidyRecord[]>([])
const subsidyTotal = ref(0)
const subsidyQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  auditStatus: undefined as number | undefined,
  grantStatus: undefined as number | undefined,
  dateRange: [] as string[]
})

const withdrawLoading = ref(false)
const withdrawList = ref<MerchantWithdrawRecord[]>([])
const withdrawTotal = ref(0)
const withdrawQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
  dateRange: [] as string[]
})

const withdrawForm = reactive({
  amount: 0,
  accountId: undefined as number | undefined
})

const recentRecords = ref<RecentRecord[]>([])

const approvedAccountText = computed(() => {
  if (!overview.value.hasPassedReceivingAccount) return '未设置可用收款账户'
  return `${accountTypeText(overview.value.approvedAccountType)} / ${overview.value.approvedAccountName || '-'} / ${overview.value.approvedAccountNoMask || '-'}`
})

const approvedAccounts = computed(() =>
  accountList.value.filter(item => item.verifyStatus === 2 && item.auditStatus === 1)
)

const withdrawMaxAmount = computed(() => {
  const raw = Number(overview.value.withdrawAvailableAmount || 0)
  if (!Number.isFinite(raw) || raw <= 0) return 1
  return Number(raw.toFixed(2))
})

const toMoney = (value: unknown) => Number(value || 0).toFixed(2)

const accountTypeText = (type?: number) => {
  if (type === 1) return '银行卡'
  if (type === 2) return '支付宝'
  if (type === 3) return '微信收款'
  return '未知'
}

const verifyText = (status?: number) => {
  if (status === 2) return '已验证'
  if (status === 1) return '验证中'
  return '未验证'
}

const verifyTagType = (status?: number) => {
  if (status === 2) return 'success'
  if (status === 1) return 'warning'
  return 'info'
}

const auditText = (status?: number) => {
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '待审核'
}

const auditTagType = (status?: number) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

const transferStatusText = (status?: number) => {
  if (status === 1) return '已打款'
  if (status === 2) return '失败待重试'
  if (status === 3) return '人工兜底'
  return '待打款'
}

const transferStatusTag = (status?: number) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 3) return 'warning'
  return 'info'
}

const subsidyAuditText = (status?: number) => {
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return '待审核'
}

const subsidyAuditTag = (status?: number) => {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'warning'
}

const subsidyGrantText = (status?: number) => {
  if (status === 1) return '已发放'
  return '待发放'
}

const subsidyGrantTag = (status?: number) => {
  if (status === 1) return 'success'
  return 'info'
}

const withdrawStatusText = (status?: number) => {
  if (status === 0) return '待审核'
  if (status === 1) return '待打款'
  if (status === 2) return '已驳回'
  if (status === 3) return '打款成功'
  if (status === 4) return '失败待重试'
  if (status === 5) return '失败人工处理'
  if (status === 6) return '已取消'
  return '未知'
}

const withdrawStatusTag = (status?: number) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'info'
  if (status === 2) return 'danger'
  if (status === 3) return 'success'
  if (status === 4) return 'danger'
  if (status === 5) return 'warning'
  return ''
}

const mapDateRange = (range: string[]) => {
  const startDate = range?.[0]
  const endDate = range?.[1]
  return { startDate, endDate }
}

const loadOverview = async () => {
  const [data, withdrawData] = await Promise.all([
    getMerchantAccountOverview(),
    getWithdrawAvailable().catch(() => null)
  ])
  const fallbackAvailable = Number(withdrawData?.availableAmount || 0)
  const fallbackFrozen = Number(withdrawData?.frozenAmount || 0)
  const fallbackSuccess = Number(withdrawData?.withdrawSuccessAmount || 0)
  overview.value = {
    balance: Number(data?.balance || 0),
    totalIncome: Number(data?.totalIncome || 0),
    totalServiceFee: Number(data?.totalServiceFee || 0),
    todayIncome: Number(data?.todayIncome || 0),
    weekIncome: Number(data?.weekIncome || 0),
    monthIncome: Number(data?.monthIncome || 0),
    pendingTransferCount: Number(data?.pendingTransferCount || 0),
    failedTransferCount: Number(data?.failedTransferCount || 0),
    manualFallbackCount: Number(data?.manualFallbackCount || 0),
    subsidyTotal: Number(data?.subsidyTotal || 0),
    subsidyMonth: Number(data?.subsidyMonth || 0),
    subsidyOrderCount: Number(data?.subsidyOrderCount || 0),
    subsidyPendingCount: Number(data?.subsidyPendingCount || 0),
    subsidyRejectedCount: Number(data?.subsidyRejectedCount || 0),
    hasPassedReceivingAccount: !!data?.hasPassedReceivingAccount,
    approvedAccountType: data?.approvedAccountType,
    approvedAccountName: data?.approvedAccountName || '',
    approvedAccountNoMask: data?.approvedAccountNoMask || '',
    withdrawAvailableAmount: Number(data?.withdrawAvailableAmount ?? fallbackAvailable),
    withdrawFrozenAmount: Number(data?.withdrawFrozenAmount ?? fallbackFrozen),
    withdrawSuccessAmount: Number(data?.withdrawSuccessAmount ?? fallbackSuccess)
  }
}

const loadAccounts = async () => {
  accountLoading.value = true
  try {
    const list = await listAccounts()
    accountList.value = list || []
  } finally {
    accountLoading.value = false
  }
}

const loadReconciliation = async () => {
  reconciliationLoading.value = true
  try {
    const { startDate, endDate } = mapDateRange(reconciliationQuery.dateRange)
    const params: Record<string, any> = {
      pageNum: reconciliationQuery.pageNum,
      pageSize: reconciliationQuery.pageSize
    }
    if (reconciliationQuery.transferStatus !== undefined) params.transferStatus = reconciliationQuery.transferStatus
    if (reconciliationQuery.keyword.trim()) params.keyword = reconciliationQuery.keyword.trim()
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const res = await listReconciliation(params)
    reconciliationList.value = res?.list || []
    reconciliationTotal.value = Number(res?.total || 0)
  } finally {
    reconciliationLoading.value = false
  }
}

const loadSubsidyList = async () => {
  subsidyLoading.value = true
  try {
    const { startDate, endDate } = mapDateRange(subsidyQuery.dateRange)
    const params: Record<string, any> = {
      pageNum: subsidyQuery.pageNum,
      pageSize: subsidyQuery.pageSize
    }
    if (subsidyQuery.auditStatus !== undefined) params.auditStatus = subsidyQuery.auditStatus
    if (subsidyQuery.grantStatus !== undefined) params.grantStatus = subsidyQuery.grantStatus
    if (subsidyQuery.keyword.trim()) params.keyword = subsidyQuery.keyword.trim()
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const res = await listSubsidy(params)
    subsidyList.value = res?.list || []
    subsidyTotal.value = Number(res?.total || 0)
  } finally {
    subsidyLoading.value = false
  }
}

const loadWithdrawals = async () => {
  withdrawLoading.value = true
  try {
    const { startDate, endDate } = mapDateRange(withdrawQuery.dateRange)
    const params: Record<string, any> = {
      pageNum: withdrawQuery.pageNum,
      pageSize: withdrawQuery.pageSize
    }
    if (withdrawQuery.status !== undefined) params.status = withdrawQuery.status
    if (withdrawQuery.keyword.trim()) params.keyword = withdrawQuery.keyword.trim()
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const res = await listWithdrawals(params)
    withdrawList.value = res?.list || []
    withdrawTotal.value = Number(res?.total || 0)
  } finally {
    withdrawLoading.value = false
  }
}

const loadRecentRecords = async () => {
  const [reconciliationRes, subsidyRes, withdrawRes] = await Promise.all([
    listReconciliation({ pageNum: 1, pageSize: 8 }),
    listSubsidy({ pageNum: 1, pageSize: 8 }),
    listWithdrawals({ pageNum: 1, pageSize: 8 })
  ])
  const records: RecentRecord[] = []

  ;(reconciliationRes?.list || []).forEach((item) => {
    const timeText = item.transferTime || item.createTime || '-'
    records.push({
      id: `r-${item.id}`,
      kind: item.transferStatus === 1 ? 'income' : 'pending',
      title: item.orderNo ? `订单打款 ${item.orderNo}` : '系统打款记录',
      statusText: transferStatusText(item.transferStatus),
      timeText,
      amount: Number(item.actualIncome || 0),
      ts: timeText && timeText !== '-' ? new Date(timeText).getTime() : 0
    })
  })

  ;(subsidyRes?.list || []).forEach((item) => {
    const timeText = item.grantTime || item.createTime || '-'
    records.push({
      id: `s-${item.id}`,
      kind: item.grantStatus === 1 ? 'subsidy' : 'pending',
      title: item.orderNo ? `订单补贴 ${item.orderNo}` : '补贴记录',
      statusText: `${subsidyAuditText(item.auditStatus)} / ${subsidyGrantText(item.grantStatus)}`,
      timeText,
      amount: Number(item.subsidyAmount || 0),
      ts: timeText && timeText !== '-' ? new Date(timeText).getTime() : 0
    })
  })

  ;(withdrawRes?.list || []).forEach((item) => {
    const timeText = item.transferTime || item.auditTime || item.createTime || '-'
    records.push({
      id: `w-${item.id}`,
      kind: 'withdraw',
      title: `提现 ${item.withdrawNo || ''}`.trim(),
      statusText: withdrawStatusText(item.status),
      timeText,
      amount: Number(item.actualAmount || item.applyAmount || 0),
      ts: timeText && timeText !== '-' ? new Date(timeText).getTime() : 0
    })
  })

  records.sort((a, b) => b.ts - a.ts)
  recentRecords.value = records.slice(0, 8)
}

const searchReconciliation = () => {
  reconciliationQuery.pageNum = 1
  loadReconciliation()
}

const resetReconciliation = () => {
  reconciliationQuery.pageNum = 1
  reconciliationQuery.pageSize = 10
  reconciliationQuery.keyword = ''
  reconciliationQuery.transferStatus = undefined
  reconciliationQuery.dateRange = []
  loadReconciliation()
}

const onReconciliationSizeChange = () => {
  reconciliationQuery.pageNum = 1
  loadReconciliation()
}

const searchSubsidy = () => {
  subsidyQuery.pageNum = 1
  loadSubsidyList()
}

const resetSubsidy = () => {
  subsidyQuery.pageNum = 1
  subsidyQuery.pageSize = 10
  subsidyQuery.keyword = ''
  subsidyQuery.auditStatus = undefined
  subsidyQuery.grantStatus = undefined
  subsidyQuery.dateRange = []
  loadSubsidyList()
}

const onSubsidySizeChange = () => {
  subsidyQuery.pageNum = 1
  loadSubsidyList()
}

const searchWithdraw = () => {
  withdrawQuery.pageNum = 1
  loadWithdrawals()
}

const resetWithdraw = () => {
  withdrawQuery.pageNum = 1
  withdrawQuery.pageSize = 10
  withdrawQuery.keyword = ''
  withdrawQuery.status = undefined
  withdrawQuery.dateRange = []
  loadWithdrawals()
}

const onWithdrawSizeChange = () => {
  withdrawQuery.pageNum = 1
  loadWithdrawals()
}

const refreshAll = async () => {
  await Promise.all([
    loadOverview(),
    loadAccounts(),
    loadReconciliation(),
    loadSubsidyList(),
    loadWithdrawals(),
    loadRecentRecords()
  ])
}

const saveReceivingAccount = async () => {
  if (!accountForm.accountName.trim() || !accountForm.accountNo.trim()) {
    ElMessage.warning('请先填写完整账户信息')
    return
  }
  await saveAccount({
    accountType: accountForm.accountType,
    accountName: accountForm.accountName.trim(),
    accountNo: accountForm.accountNo.trim()
  })
  ElMessage.success('账户已保存，请发起1分钱验证')
  await Promise.all([loadAccounts(), loadOverview()])
}

const startVerify = async (row: MerchantAccount) => {
  if (row.verifyStatus === 2 && row.auditStatus === 1) {
    ElMessage.info('该账户已通过验证并审核，无需重复验证')
    return
  }
  const res = await initAccountVerify(row.id)
  const amount = Number(res?.verifyAmount || 0)
  if (amount > 0) {
    ElMessage.success(`验证已发起（模拟到账金额：¥${amount.toFixed(2)}）`)
  } else {
    ElMessage.success('验证已发起，请核对到账金额')
  }
  await Promise.all([loadAccounts(), loadOverview()])
}

const openConfirmVerify = (row: MerchantAccount) => {
  if (row.verifyStatus !== 1) {
    ElMessage.warning('请先发起验证后再确认金额')
    return
  }
  verifyingAccountId.value = row.id
  verifyAmountForm.amount = 0
  verifyAmountDialogVisible.value = true
}

const submitConfirmVerify = async () => {
  if (!verifyingAccountId.value) return
  await confirmAccountVerify(verifyingAccountId.value, Number(verifyAmountForm.amount))
  ElMessage.success('验证成功，请提交管理员审核')
  verifyAmountDialogVisible.value = false
  await Promise.all([loadAccounts(), loadOverview()])
}

const submitAudit = async (row: MerchantAccount) => {
  if (row.verifyStatus !== 2) {
    ElMessage.warning('请先完成1分钱验证后再提交审核')
    return
  }
  if (row.auditStatus === 1) {
    ElMessage.info('该账户已审核通过')
    return
  }
  await submitAccountAudit(row.id)
  ElMessage.success('已提交管理员审核')
  await Promise.all([loadAccounts(), loadOverview()])
}

const openWithdrawDialog = () => {
  const firstAccount = approvedAccounts.value[0]
  if (!firstAccount) {
    ElMessage.warning('请先完成收款账户验证并通过管理员审核')
    return
  }
  const available = Number(overview.value.withdrawAvailableAmount || 0)
  if (!Number.isFinite(available) || available < 1) {
    ElMessage.warning('当前可提现金额不足1元，暂不可申请提现')
    return
  }
  withdrawForm.accountId = firstAccount.id
  const maxAmount = available
  withdrawForm.amount = maxAmount >= 1 ? Number(Math.floor(maxAmount * 100) / 100) : 0
  withdrawDialogVisible.value = true
}

const submitWithdraw = async () => {
  if (withdrawSubmitting.value) return
  if (!withdrawForm.accountId) {
    ElMessage.warning('请选择收款账户')
    return
  }
  const amount = Number(withdrawForm.amount || 0)
  if (amount < 1) {
    ElMessage.warning('提现金额不能低于1元')
    return
  }
  const available = Number(overview.value.withdrawAvailableAmount || 0)
  if (amount > available) {
    ElMessage.warning('提现金额不能超过可提现余额')
    return
  }
  withdrawSubmitting.value = true
  try {
    const res = await applyWithdraw({
      amount: Number(amount.toFixed(2)),
      accountId: withdrawForm.accountId
    })
    withdrawDialogVisible.value = false
    ElMessage.success(`提现申请已提交（${res?.withdrawNo || '待审核'}）`)
    await Promise.all([loadOverview(), loadWithdrawals(), loadRecentRecords()])
  } finally {
    withdrawSubmitting.value = false
  }
}

const cancelWithdrawRow = (row: MerchantWithdrawRecord) => {
  ElMessageBox.confirm('确认取消这笔提现申请吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await cancelWithdraw(row.id)
      ElMessage.success('提现申请已取消')
      await Promise.all([loadOverview(), loadWithdrawals(), loadRecentRecords()])
    })
    .catch(() => {})
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped lang="scss">
.account-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.receiving-card,
.overview-card,
.subsidy-card,
.records-card,
.detail-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.account-form {
  margin-bottom: 8px;
}

.account-alert {
  margin: 12px 0 16px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px;
  background: #fff;

  &.primary {
    grid-column: span 1;
    border-color: #d9ecff;
    background: linear-gradient(135deg, #f0f8ff 0%, #ffffff 100%);
  }

  .label {
    margin: 0;
    font-size: 13px;
    color: #8a8f99;
  }

  .amount {
    margin: 8px 0 4px;
    font-size: 26px;
    font-weight: 700;
    color: #1f2d3d;
  }

  .sub {
    margin: 0;
    font-size: 12px;
    color: #909399;
  }
}

.subsidy-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.subsidy-item {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px;
  background: #fff;

  .subsidy-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .subsidy-info {
    display: flex;
    flex-direction: column;
    gap: 3px;

    .label {
      font-size: 12px;
      color: #909399;
    }

    .amount {
      font-size: 20px;
      font-weight: 700;
      color: #1f2d3d;
    }

    .subtext {
      font-size: 12px;
      color: #909399;
    }
  }
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;

  &.income {
    background: #ecf5ff;
    color: #409eff;
  }

  &.subsidy {
    background: #f0f9eb;
    color: #67c23a;
  }

  &.pending {
    background: #fdf6ec;
    color: #e6a23c;
  }

  &.withdraw {
    background: #fef0f0;
    color: #f56c6c;
  }
}

.record-detail {
  h4 {
    margin: 0 0 4px;
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }

  .record-time {
    margin: 0;
    font-size: 12px;
    color: #909399;
  }
}

.record-amount {
  font-size: 16px;
  font-weight: 700;

  &.income {
    color: #409eff;
  }

  &.subsidy {
    color: #67c23a;
  }

  &.pending {
    color: #e6a23c;
  }

  &.withdraw {
    color: #f56c6c;
  }
}

.filter-bar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.subsidy-rules {
  h4 {
    margin: 0 0 8px;
    color: #303133;
  }

  p {
    margin: 0 0 14px;
    color: #606266;
    line-height: 1.7;
  }

  ul {
    margin: 0 0 14px;
    padding-left: 20px;
    color: #606266;
    line-height: 1.8;
  }
}

.withdraw-tip {
  font-size: 13px;
  color: #606266;
  margin-top: -4px;
}

@media (max-width: 1200px) {
  .overview-grid,
  .subsidy-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .account-page {
    padding: 12px;
  }

  .overview-grid,
  .subsidy-overview {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }

  .filter-bar {
    :deep(.el-input),
    :deep(.el-select),
    :deep(.el-date-editor) {
      width: 100% !important;
    }
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
