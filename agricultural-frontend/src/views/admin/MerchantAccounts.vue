<template>
  <div class="merchant-accounts-page">
    <div class="page-header">
      <h2>收款账户审核</h2>
      <div class="header-actions">
        <el-select
          v-model="auditStatus"
          placeholder="审核状态"
          clearable
          style="width: 150px"
          @change="onFilterChange"
        >
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-button type="primary" plain @click="loadAccounts">刷新</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="accountList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="merchantId" label="商家ID" width="100" />
        <el-table-column prop="accountType" label="账户类型" width="110">
          <template #default="{ row }">{{ accountTypeText(row.accountType) }}</template>
        </el-table-column>
        <el-table-column prop="accountName" label="账户名称" width="150" />
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
        <el-table-column prop="verifiedTime" label="验证时间" width="170">
          <template #default="{ row }">{{ row.verifiedTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="auditSubmitTime" label="提交审核时间" width="170">
          <template #default="{ row }">{{ row.auditSubmitTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="驳回原因" min-width="180">
          <template #default="{ row }">{{ row.rejectReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="success"
              :disabled="row.auditStatus === 1"
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.auditStatus === 2 && !!row.rejectReason"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onPageSizeChange"
          @current-change="loadAccounts"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMerchantAccounts, auditMerchantAccount } from '@/apis/admin'

const loading = ref(false)
const accountList = ref<any[]>([])
const auditStatus = ref<number | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

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

const loadAccounts = async () => {
  loading.value = true
  try {
    const res = await listMerchantAccounts({
      auditStatus: auditStatus.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res) {
      accountList.value = res.list || []
      total.value = res.total || 0
    }
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  pageNum.value = 1
  loadAccounts()
}

const onPageSizeChange = () => {
  pageNum.value = 1
  loadAccounts()
}

const handleApprove = (row: any) => {
  if (row.verifyStatus !== 2) {
    ElMessage.warning('该账户尚未完成1分钱验证，不能审核通过')
    return
  }
  ElMessageBox.confirm('确认审核通过该收款账户吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    await auditMerchantAccount(row.id, { approve: true })
    ElMessage.success('审核通过')
    loadAccounts()
  }).catch(() => {})
}

const handleReject = (row: any) => {
  ElMessageBox.prompt('请输入驳回原因', '驳回账户审核', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '例如：账户信息不完整，请补充后重新提交'
  }).then(async ({ value }) => {
    await auditMerchantAccount(row.id, {
      approve: false,
      reason: (value || '').trim() || '资料信息不完整，请补充后重新提交'
    })
    ElMessage.success('已驳回')
    loadAccounts()
  }).catch(() => {})
}

onMounted(() => {
  loadAccounts()
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
  gap: 10px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

