<template>
  <div class="logs-page">
    <div class="page-header">
      <h2>系统日志</h2>
      <div class="header-actions">
        <template v-if="activeTab === 'operation'">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
          />
          <el-button type="primary" size="small" :loading="exporting" @click="handleExport">导出</el-button>
        </template>
      </div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="操作日志" name="operation">
          <el-table :data="logList" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="operatorName" label="操作人" width="140" />
            <el-table-column prop="operationType" label="操作类型" width="120" />
            <el-table-column prop="businessType" label="业务模块" width="120" />
            <el-table-column prop="operationContent" label="日志内容" min-width="320" />
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
        </el-tab-pane>

        <el-tab-pane label="审核记录" name="audit">
          <div class="audit-filters">
            <el-select
              v-model="auditQuery.businessType"
              clearable
              placeholder="业务类型"
              style="width: 180px"
            >
              <el-option label="商家审核" value="merchant" />
              <el-option label="商品审核" value="product" />
              <el-option label="评论审核" value="comment" />
              <el-option label="售后裁决" value="after_sale" />
              <el-option label="账户审核" value="merchant_account" />
              <el-option label="提现审核" value="withdraw" />
            </el-select>
            <el-select
              v-model="auditQuery.auditStatus"
              clearable
              placeholder="审核结果"
              style="width: 160px"
            >
              <el-option label="审核通过" :value="1" />
              <el-option label="审核不通过" :value="2" />
            </el-select>
            <el-button type="primary" @click="searchAuditRecords">查询</el-button>
            <el-button @click="resetAuditFilters">重置</el-button>
          </div>

          <el-table :data="auditList" style="width: 100%" v-loading="auditLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="businessType" label="业务类型" width="160" />
            <el-table-column prop="businessId" label="业务ID" width="120" />
            <el-table-column prop="auditUserId" label="审核人ID" width="120" />
            <el-table-column prop="auditStatus" label="结果" width="120">
              <template #default="{ row }">
                <el-tag :type="Number(row.auditStatus) === 1 ? 'success' : 'danger'">
                  {{ Number(row.auditStatus) === 1 ? '通过' : '不通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="auditReason" label="审核说明" min-width="300">
              <template #default="{ row }">{{ row.auditReason || '-' }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="180" />
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="auditPage"
              v-model:page-size="auditPageSize"
              :total="auditTotal"
              layout="total, prev, pager, next"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listAuditRecords, listOperationLogs } from '@/apis/admin'
import axios from 'axios'
import { useUserStore } from '@/stores/modules/user'

const activeTab = ref<'operation' | 'audit'>('operation')
const dateRange = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const logList = ref<any[]>([])

const auditLoading = ref(false)
const auditList = ref<any[]>([])
const auditTotal = ref(0)
const auditPage = ref(1)
const auditPageSize = ref(10)
const auditQuery = ref<{
  businessType?: string
  auditStatus?: number
}>({})

const userStore = useUserStore()

const toDateText = (value: any) => {
  if (!value) return undefined
  if (value instanceof Date) return value.toISOString().slice(0, 10)
  return String(value).slice(0, 10)
}

const loadLogs = async () => {
  if (activeTab.value !== 'operation') return
  loading.value = true
  try {
    const params: any = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = toDateText((dateRange.value as any[])[0])
      params.endTime = toDateText((dateRange.value as any[])[1])
    }
    const res: any = await listOperationLogs(params)
    if (res) {
      logList.value = res.list || []
      total.value = res.total || 0
    }
  } catch (error) {
    console.error('Failed to load logs', error)
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

const loadAuditRecords = async () => {
  if (activeTab.value !== 'audit') return
  auditLoading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: auditPage.value,
      pageSize: auditPageSize.value
    }
    if (auditQuery.value.businessType) params.businessType = auditQuery.value.businessType
    if (auditQuery.value.auditStatus !== undefined) params.auditStatus = auditQuery.value.auditStatus

    const res: any = await listAuditRecords(params)
    auditList.value = res?.list || []
    auditTotal.value = Number(res?.total || 0)
  } catch (error) {
    console.error('Failed to load audit records', error)
    ElMessage.error('加载审核记录失败')
  } finally {
    auditLoading.value = false
  }
}

const handleExport = async () => {
  if (activeTab.value !== 'operation') return
  exporting.value = true
  try {
    const params: any = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = toDateText((dateRange.value as any[])[0])
      params.endTime = toDateText((dateRange.value as any[])[1])
    }
    const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
    const response = await axios.get(`${apiBase}/admin/operation-logs/export`, {
      params,
      responseType: 'blob',
      headers: userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}
    })
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = window.URL.createObjectURL(blob)
    link.href = url
    link.download = `operation_logs_${Date.now()}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出日志失败', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const searchAuditRecords = () => {
  auditPage.value = 1
  loadAuditRecords()
}

const resetAuditFilters = () => {
  auditQuery.value = {}
  auditPage.value = 1
  auditPageSize.value = 10
  loadAuditRecords()
}

watch([currentPage, pageSize, dateRange], () => {
  loadLogs()
}, { deep: true })

watch([auditPage, auditPageSize], () => {
  loadAuditRecords()
})

watch(activeTab, (tab) => {
  if (tab === 'operation') {
    loadLogs()
    return
  }
  loadAuditRecords()
})

onMounted(() => {
  loadLogs()
})
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

.audit-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
