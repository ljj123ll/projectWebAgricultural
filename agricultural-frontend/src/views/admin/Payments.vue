<template>
  <div class="payments-page">
    <div class="page-header">
      <h2>资金流水</h2>
      <div class="header-actions">
        <el-select v-model="payStatus" placeholder="支付状态" clearable style="width: 150px; margin-right: 10px" @change="handleFilterChange">
          <el-option label="待支付" :value="0" />
          <el-option label="支付成功" :value="1" />
          <el-option label="支付失败" :value="2" />
        </el-select>
        <el-select v-model="refundStatus" placeholder="退款状态" clearable style="width: 150px; margin-right: 10px" @change="handleFilterChange">
          <el-option label="未退款" :value="0" />
          <el-option label="退款中" :value="1" />
          <el-option label="退款成功" :value="2" />
          <el-option label="退款失败" :value="3" />
        </el-select>
      </div>
    </div>

    <el-card>
      <el-table :data="paymentList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="payAmount" label="支付金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ formatMoney(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payType" label="支付方式" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.payType === 1 ? '微信支付' : row.payType === 2 ? '支付宝' : '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payStatus" label="支付状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getPayStatusType(row.payStatus)">
              {{ getPayStatusText(row.payStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="180" />
        <el-table-column prop="refundStatus" label="退款状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getRefundStatusType(row.refundStatus)">
              {{ getRefundStatusText(row.refundStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额" width="120">
          <template #default="{ row }">
            <span v-if="Number(row.refundAmount || 0) > 0" class="amount refund">
              ¥{{ formatMoney(row.refundAmount) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.refundStatus === 1">
              <el-button
                link
                type="success"
                :loading="isRefundSubmitting(row.id, 2)"
                :disabled="isRefundRowBusy(row.id)"
                @click="handleRefund(row, 2)"
              >
                确认退款
              </el-button>
              <el-button
                link
                type="danger"
                :loading="isRefundSubmitting(row.id, 3)"
                :disabled="isRefundRowBusy(row.id)"
                @click="handleRefund(row, 3)"
              >
                拒绝退款
              </el-button>
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
          @current-change="loadPayments"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPayments, updateRefund } from '@/apis/admin'

/**
 * 管理员资金流水页。
 * 支付记录查看、退款状态处理都在这里完成，适合答辩讲“支付与退款闭环”。
 */

const payStatus = ref<number | undefined>(undefined)
const refundStatus = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const refundSubmittingMap = ref<Record<number, number>>({})

const paymentList = ref<any[]>([])

const isRefundRowBusy = (id?: number) => {
  if (!id) return false
  return Object.prototype.hasOwnProperty.call(refundSubmittingMap.value, id)
}

const isRefundSubmitting = (id?: number, status?: number) => {
  if (!id || !status) return false
  return refundSubmittingMap.value[id] === status
}

const formatMoney = (value: unknown) => {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? amount.toFixed(2) : '0.00'
}

const getPayStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] || 'info'
}

const getPayStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待支付',
    1: '支付成功',
    2: '支付失败'
  }
  return map[status] || '未知'
}

const getRefundStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return map[status] || 'info'
}

const getRefundStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '未退款',
    1: '退款中',
    2: '退款成功',
    3: '退款失败'
  }
  return map[status] || '未知'
}

const handleFilterChange = () => {
  currentPage.value = 1
  void loadPayments()
}

// 支付记录列表入口，支持按支付状态和退款状态筛选。
const loadPayments = async () => {
  loading.value = true;
  try {
    const res = await listPayments({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      payStatus: payStatus.value,
      refundStatus: refundStatus.value
    });
    if (res) {
      paymentList.value = res.list || [];
      total.value = res.total || 0;
    }
  } catch (error) {
    console.error(error);
    paymentList.value = [];
    total.value = 0;
    ElMessage.error('资金流水加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadPayments();
})

// 管理员退款处理入口：支持确认退款和拒绝退款两种结果。
const handleRefund = (row: any, status: number) => {
  const rowId = Number(row?.id || 0)
  if (!rowId || isRefundRowBusy(rowId)) return
  const actionText = status === 2 ? '确认退款' : '拒绝退款';
  ElMessageBox.confirm(`确定要${actionText}吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      refundSubmittingMap.value = {
        ...refundSubmittingMap.value,
        [rowId]: status
      }
      await updateRefund(rowId, status);
      ElMessage.success('操作成功');
      await loadPayments();
    } catch (error) {
      console.error(error);
      ElMessage.error('退款处理失败，请稍后重试');
    } finally {
      const next = { ...refundSubmittingMap.value }
      delete next[rowId]
      refundSubmittingMap.value = next
    }
  }).catch(() => {})
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.amount {
  font-family: monospace;
  font-size: 16px;
  color: #f56c6c;
}

.amount.refund {
  color: #67c23a;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
