<template>
  <div class="withdrawals-page">
    <div class="page-header">
      <h2>提现审核</h2>
      <div class="header-actions">
        <el-input
          v-model="query.keyword"
          placeholder="提现单号"
          clearable
          style="width: 180px"
        />
        <el-input-number
          v-model="query.merchantId"
          :min="1"
          :step="1"
          controls-position="right"
          placeholder="商家ID"
          style="width: 140px"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 170px">
          <el-option label="待审核" :value="0" />
          <el-option label="待打款" :value="1" />
          <el-option label="已驳回" :value="2" />
          <el-option label="打款成功" :value="3" />
          <el-option label="失败待重试" :value="4" />
          <el-option label="失败人工处理" :value="5" />
          <el-option label="已取消" :value="6" />
        </el-select>
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          clearable
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="withdrawNo" label="提现单号" min-width="170" />
        <el-table-column prop="merchantId" label="商家ID" width="100" />
        <el-table-column prop="accountType" label="账户类型" width="110">
          <template #default="{ row }">{{ accountTypeText(row.accountType) }}</template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户名" width="130" />
        <el-table-column prop="accountNo" label="账号" min-width="180" />
        <el-table-column prop="applyAmount" label="申请金额" width="120">
          <template #default="{ row }">¥{{ toMoney(row.applyAmount) }}</template>
        </el-table-column>
        <el-table-column prop="actualAmount" label="打款金额" width="120">
          <template #default="{ row }">¥{{ toMoney(row.actualAmount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试" width="70" />
        <el-table-column prop="failReason" label="失败原因" min-width="160">
          <template #default="{ row }">{{ row.failReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核备注" min-width="140">
          <template #default="{ row }">{{ row.auditRemark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column prop="transferTime" label="打款时间" width="170">
          <template #default="{ row }">{{ row.transferTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <template v-else-if="[1, 4, 5].includes(row.status)">
              <el-button link type="primary" @click="manualTransfer(row)">人工打款</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onSizeChange"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listWithdrawals, auditWithdrawal, manualTransferWithdrawal } from '@/apis/admin'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  merchantId: undefined as number | undefined,
  status: undefined as number | undefined,
  dateRange: [] as string[],
  pageNum: 1,
  pageSize: 10
})

const toMoney = (value: unknown) => Number(value || 0).toFixed(2)

const accountTypeText = (type?: number) => {
  if (type === 1) return '银行卡'
  if (type === 2) return '支付宝'
  if (type === 3) return '微信收款'
  return '未知'
}

const statusText = (status?: number) => {
  if (status === 0) return '待审核'
  if (status === 1) return '待打款'
  if (status === 2) return '已驳回'
  if (status === 3) return '打款成功'
  if (status === 4) return '失败待重试'
  if (status === 5) return '失败人工处理'
  if (status === 6) return '已取消'
  return '未知'
}

const statusTag = (status?: number) => {
  if (status === 0) return 'warning'
  if (status === 1) return 'info'
  if (status === 2) return 'danger'
  if (status === 3) return 'success'
  if (status === 4) return 'danger'
  if (status === 5) return 'warning'
  return ''
}

const load = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (query.merchantId) params.merchantId = query.merchantId
    if (query.status !== undefined) params.status = query.status
    if (query.dateRange?.length === 2) {
      params.startDate = query.dateRange[0]
      params.endDate = query.dateRange[1]
    }
    const res = await listWithdrawals(params)
    list.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.pageNum = 1
  load()
}

const reset = () => {
  query.keyword = ''
  query.merchantId = undefined
  query.status = undefined
  query.dateRange = []
  query.pageNum = 1
  query.pageSize = 10
  load()
}

const onSizeChange = () => {
  query.pageNum = 1
  load()
}

const approve = (row: any) => {
  ElMessageBox.confirm('确认审核通过该提现申请吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await auditWithdrawal(row.id, { approve: true, remark: '审核通过，进入打款队列' })
    ElMessage.success('审核通过')
    load()
  }).catch(() => {})
}

const reject = (row: any) => {
  ElMessageBox.prompt('请输入驳回原因', '驳回提现申请', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea'
  }).then(async ({ value }) => {
    await auditWithdrawal(row.id, {
      approve: false,
      remark: (value || '').trim() || '提现申请审核未通过'
    })
    ElMessage.success('已驳回')
    load()
  }).catch(() => {})
}

const manualTransfer = (row: any) => {
  ElMessageBox.confirm('确认执行人工打款？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await manualTransferWithdrawal(row.id)
    ElMessage.success('人工打款请求已执行')
    load()
  }).catch(() => {})
}

onMounted(() => {
  load()
})
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

