<template>
  <div class="logs-page">
    <div class="page-header">
      <div>
        <h2>系统日志</h2>
        <p>系统日志用于追踪后台操作和审核决策，便于排查问题、复盘敏感操作和导出留档。</p>
      </div>
      <div class="header-actions">
        <el-button @click="handleRefresh">刷新</el-button>
        <el-button
          v-if="activeTab === 'operation'"
          type="primary"
          :loading="exporting"
          @click="handleExport"
        >
          导出当前筛选
        </el-button>
      </div>
    </div>

    <div class="overview-grid">
      <el-card shadow="never" class="overview-card">
        <span>{{ activeTab === 'operation' ? '操作日志总数' : '审核记录总数' }}</span>
        <strong>{{ activeTab === 'operation' ? total : auditTotal }}</strong>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <span>{{ activeTab === 'operation' ? '当前页操作人' : '当前页通过数' }}</span>
        <strong>{{ activeTab === 'operation' ? currentOperatorCount : currentAuditPassCount }}</strong>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <span>{{ activeTab === 'operation' ? '当前页业务模块' : '当前页驳回数' }}</span>
        <strong>{{ activeTab === 'operation' ? currentBusinessTypeCount : currentAuditRejectCount }}</strong>
      </el-card>
    </div>

    <el-card shadow="never" class="logs-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="操作日志" name="operation">
          <div class="filter-bar">
            <el-select
              v-model="operationQuery.operationType"
              clearable
              placeholder="操作类型"
              style="width: 180px"
            >
              <el-option label="新增" value="新增" />
              <el-option label="修改" value="修改" />
              <el-option label="删除" value="删除" />
              <el-option label="审核" value="审核" />
              <el-option label="登录" value="登录" />
              <el-option label="导出" value="导出" />
            </el-select>
            <el-input
              v-model="operationQuery.businessType"
              clearable
              placeholder="业务模块，如订单/新闻/售后"
              style="width: 220px"
            />
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
            <el-button type="primary" @click="searchOperationLogs">查询</el-button>
            <el-button @click="resetOperationFilters">重置</el-button>
          </div>

          <el-table :data="logList" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="操作人" min-width="140">
              <template #default="{ row }">
                <div class="cell-stack">
                  <strong>{{ row.operatorName || '未知管理员' }}</strong>
                  <span>ID {{ row.operatorId || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作类型" width="120">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.operationType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="业务模块" width="140">
              <template #default="{ row }">
                {{ row.businessType || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="operationContent" label="日志内容" min-width="320" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP地址" width="140" />
            <el-table-column label="时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 30]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadLogs"
              @size-change="handleOperationPageSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="审核记录" name="audit">
          <div class="filter-bar">
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
            <el-table-column label="业务类型" width="160">
              <template #default="{ row }">
                {{ getAuditBusinessText(row.businessType) }}
              </template>
            </el-table-column>
            <el-table-column prop="businessId" label="业务ID" width="120" />
            <el-table-column prop="auditUserId" label="审核人ID" width="120" />
            <el-table-column label="结果" width="120">
              <template #default="{ row }">
                <el-tag :type="Number(row.auditStatus) === 1 ? 'success' : 'danger'">
                  {{ Number(row.auditStatus) === 1 ? '通过' : '不通过' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="auditReason" label="审核说明" min-width="300">
              <template #default="{ row }">{{ row.auditReason || '-' }}</template>
            </el-table-column>
            <el-table-column label="时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="auditPage"
              v-model:page-size="auditPageSize"
              :page-sizes="[10, 20, 30]"
              :total="auditTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadAuditRecords"
              @size-change="handleAuditPageSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { listAuditRecords, listOperationLogs } from '@/apis/admin';
import axios from 'axios';
import { useUserStore } from '@/stores/modules/user';

const activeTab = ref<'operation' | 'audit'>('operation');
const dateRange = ref<string[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const exporting = ref(false);
const logList = ref<any[]>([]);

const auditLoading = ref(false);
const auditList = ref<any[]>([]);
const auditTotal = ref(0);
const auditPage = ref(1);
const auditPageSize = ref(10);

const operationQuery = ref<{
  operationType?: string;
  businessType?: string;
}>({});

const auditQuery = ref<{
  businessType?: string;
  auditStatus?: number;
}>({});

const userStore = useUserStore();

const currentOperatorCount = computed(() => new Set(logList.value.map(item => item.operatorId).filter(Boolean)).size);
const currentBusinessTypeCount = computed(() => new Set(logList.value.map(item => item.businessType).filter(Boolean)).size);
const currentAuditPassCount = computed(() => auditList.value.filter(item => Number(item.auditStatus) === 1).length);
const currentAuditRejectCount = computed(() => auditList.value.filter(item => Number(item.auditStatus) === 2).length);

const formatDate = (value?: string) => {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 19);
};

const getAuditBusinessText = (value?: string) => {
  const map: Record<string, string> = {
    merchant: '商家审核',
    product: '商品审核',
    comment: '评论审核',
    after_sale: '售后裁决',
    merchant_account: '账户审核',
    withdraw: '提现审核'
  };
  return map[String(value || '')] || value || '-';
};

const loadLogs = async () => {
  if (activeTab.value !== 'operation') return;
  loading.value = true;
  try {
    const params: Record<string, any> = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };
    if (operationQuery.value.operationType) params.operationType = operationQuery.value.operationType;
    if (operationQuery.value.businessType) params.businessType = operationQuery.value.businessType;
    if (dateRange.value?.length === 2) {
      params.startTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const res: any = await listOperationLogs(params);
    logList.value = res?.list || [];
    total.value = Number(res?.total || 0);
  } catch (error) {
    console.error('Failed to load logs', error);
    ElMessage.error('加载日志失败');
  } finally {
    loading.value = false;
  }
};

const loadAuditRecords = async () => {
  if (activeTab.value !== 'audit') return;
  auditLoading.value = true;
  try {
    const params: Record<string, any> = {
      pageNum: auditPage.value,
      pageSize: auditPageSize.value
    };
    if (auditQuery.value.businessType) params.businessType = auditQuery.value.businessType;
    if (auditQuery.value.auditStatus !== undefined) params.auditStatus = auditQuery.value.auditStatus;
    const res: any = await listAuditRecords(params);
    auditList.value = res?.list || [];
    auditTotal.value = Number(res?.total || 0);
  } catch (error) {
    console.error('Failed to load audit records', error);
    ElMessage.error('加载审核记录失败');
  } finally {
    auditLoading.value = false;
  }
};

const handleRefresh = async () => {
  if (activeTab.value === 'operation') {
    await loadLogs();
    return;
  }
  await loadAuditRecords();
};

const searchOperationLogs = async () => {
  currentPage.value = 1;
  await loadLogs();
};

const resetOperationFilters = async () => {
  operationQuery.value = {};
  dateRange.value = [];
  currentPage.value = 1;
  pageSize.value = 10;
  await loadLogs();
};

const handleOperationPageSizeChange = async () => {
  currentPage.value = 1;
  await loadLogs();
};

const searchAuditRecords = async () => {
  auditPage.value = 1;
  await loadAuditRecords();
};

const resetAuditFilters = async () => {
  auditQuery.value = {};
  auditPage.value = 1;
  auditPageSize.value = 10;
  await loadAuditRecords();
};

const handleAuditPageSizeChange = async () => {
  auditPage.value = 1;
  await loadAuditRecords();
};

const handleExport = async () => {
  if (activeTab.value !== 'operation') return;
  exporting.value = true;
  try {
    const params: Record<string, any> = {};
    if (operationQuery.value.operationType) params.operationType = operationQuery.value.operationType;
    if (operationQuery.value.businessType) params.businessType = operationQuery.value.businessType;
    if (dateRange.value?.length === 2) {
      params.startTime = dateRange.value[0];
      params.endTime = dateRange.value[1];
    }
    const apiBase = import.meta.env.VITE_API_BASE_URL || '/api';
    const response = await axios.get(`${apiBase}/admin/operation-logs/export`, {
      params,
      responseType: 'blob',
      headers: userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}
    });
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = window.URL.createObjectURL(blob);
    link.href = url;
    link.download = `operation_logs_${Date.now()}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (error) {
    console.error('导出日志失败', error);
    ElMessage.error('导出失败');
  } finally {
    exporting.value = false;
  }
};

watch(activeTab, async (tab) => {
  if (tab === 'operation') {
    await loadLogs();
    return;
  }
  await loadAuditRecords();
});

onMounted(() => {
  void loadLogs();
});
</script>

<style scoped lang="scss">
.logs-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;

  h2 {
    margin: 0 0 6px;
    font-size: 24px;
    color: #111827;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
  }
}

.header-actions {
  display: flex;
  gap: 10px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  border-radius: 14px;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  span {
    color: #6b7280;
    font-size: 13px;
  }

  strong {
    color: #0f172a;
    font-size: 28px;
    line-height: 1;
  }
}

.logs-card {
  border-radius: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.cell-stack {
  display: grid;
  gap: 4px;

  strong {
    color: #111827;
  }

  span {
    color: #6b7280;
    font-size: 12px;
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 992px) {
  .page-header {
    flex-direction: column;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
