<template>
  <div class="transfers-page">
    <div class="page-header">
      <h2>打款台账</h2>
      <div class="header-actions">
        <el-button type="primary" plain @click="loadList">刷新</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="records" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="merchantId" label="商家ID" width="110" />
        <el-table-column label="订单金额" width="120">
          <template #default="{ row }">¥{{ money(row.orderAmount) }}</template>
        </el-table-column>
        <el-table-column label="实际打款" width="120">
          <template #default="{ row }">¥{{ money(row.actualIncome) }}</template>
        </el-table-column>
        <el-table-column label="平台服务费" width="120">
          <template #default="{ row }">¥{{ money(row.serviceFee) }}</template>
        </el-table-column>
        <el-table-column prop="transferStatus" label="打款状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.transferStatus)">{{ statusText(row.transferStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column prop="paymentTime" label="结算时间" width="170" />
        <el-table-column prop="transferTime" label="打款时间" width="170">
          <template #default="{ row }">{{ row.transferTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="transferNo" label="打款流水号" min-width="160">
          <template #default="{ row }">{{ row.transferNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canManualTransfer(row.transferStatus)"
              type="primary"
              link
              @click="handleManualTransfer(row)"
            >
              人工打款
            </el-button>
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
          @current-change="loadList"
          @size-change="onPageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listTransfers, manualTransfer } from '@/apis/admin';

const loading = ref(false);
const records = ref<any[]>([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10
});

const money = (value: unknown) => Number(value || 0).toFixed(2);

const statusText = (status?: number) => {
  if (status === 1) return '已打款';
  if (status === 2) return '失败待重试';
  if (status === 3) return '人工兜底';
  return '待打款';
};

const statusTag = (status?: number) => {
  if (status === 1) return 'success';
  if (status === 2) return 'danger';
  if (status === 3) return 'warning';
  return 'info';
};

const canManualTransfer = (status?: number) => status === 0 || status === 2 || status === 3;

const loadList = async () => {
  loading.value = true;
  try {
    const res = await listTransfers({
      pageNum: query.pageNum,
      pageSize: query.pageSize
    });
    records.value = res?.list || [];
    total.value = res?.total || 0;
  } catch (error) {
    console.error('加载打款台账失败', error);
    records.value = [];
    total.value = 0;
    ElMessage.error('打款台账加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const onPageSizeChange = () => {
  query.pageNum = 1;
  void loadList();
};

const handleManualTransfer = (row: any) => {
  ElMessageBox.confirm('确认执行人工打款吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await manualTransfer(row.id);
      ElMessage.success('人工打款成功');
      await loadList();
    } catch (error) {
      console.error('人工打款失败', error);
      ElMessage.error('人工打款失败，请稍后重试');
    }
  }).catch(() => {});
};

onMounted(() => {
  void loadList();
});
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
