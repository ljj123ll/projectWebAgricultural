<template>
  <div class="risk-monitor-page">
    <div class="page-header">
      <h2>风控监控</h2>
      <el-button type="primary" plain @click="refreshCurrent">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="物流超时监控" name="logistics" />
      <el-tab-pane label="异常订单风控" name="risk" />
    </el-tabs>

    <el-card v-if="activeTab === 'logistics'">
      <el-table :data="logisticsList" v-loading="logisticsLoading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column prop="merchantId" label="商家ID" width="100" />
        <el-table-column prop="receiver" label="收货人" width="120" />
        <el-table-column prop="receiverPhone" label="联系电话" width="150" />
        <el-table-column prop="receiverAddress" label="收货地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="订单金额" width="120">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="logisticsCompany" label="物流公司" width="130">
          <template #default="{ row }">{{ row.logisticsCompany || '-' }}</template>
        </el-table-column>
        <el-table-column prop="logisticsNo" label="物流单号" min-width="160">
          <template #default="{ row }">{{ row.logisticsNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="abnormalReason" label="异常原因" min-width="180">
          <template #default="{ row }">{{ row.abnormalReason || '物流超时超过5天，请核实并处理' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="logisticsQuery.pageNum"
          v-model:page-size="logisticsQuery.pageSize"
          :total="logisticsTotal"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onLogisticsSizeChange"
          @current-change="loadLogisticsAbnormal"
        />
      </div>
    </el-card>

    <el-card v-else>
      <el-table :data="riskList" v-loading="riskLoading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="merchantId" label="商家ID" width="100" />
        <el-table-column label="订单金额" width="120">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="riskScore" label="风险分" width="100">
          <template #default="{ row }">
            <el-tag :type="riskScoreTag(row.riskScore)">{{ row.riskScore || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskReason" label="风控规则命中" min-width="280" show-overflow-tooltip />
        <el-table-column prop="receiver" label="收货人" width="110" />
        <el-table-column prop="receiverPhone" label="联系电话" width="150" />
        <el-table-column prop="receiverAddress" label="收货地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="下单时间" width="170" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="riskQuery.pageNum"
          v-model:page-size="riskQuery.pageSize"
          :total="riskTotal"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onRiskSizeChange"
          @current-change="loadRiskOrders"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { listLogisticsAbnormal, listRiskOrders } from '@/apis/admin';
import { getLogisticsByOrderNo } from '@/apis/order';

const activeTab = ref<'logistics' | 'risk'>('logistics');

const logisticsLoading = ref(false);
const logisticsList = ref<any[]>([]);
const logisticsTotal = ref(0);
const logisticsQuery = reactive({
  pageNum: 1,
  pageSize: 10
});

const riskLoading = ref(false);
const riskList = ref<any[]>([]);
const riskTotal = ref(0);
const riskQuery = reactive({
  pageNum: 1,
  pageSize: 10
});

const money = (value: unknown) => Number(value || 0).toFixed(2);

const riskScoreTag = (score?: number) => {
  const value = Number(score || 0);
  if (value >= 80) return 'danger';
  if (value >= 60) return 'warning';
  return 'info';
};

const enrichLogisticsFields = async (rows: any[]) => {
  const tasks = rows.map(async (row) => {
    if (!row?.orderNo) return row;
    try {
      const logistics = await getLogisticsByOrderNo(String(row.orderNo));
      return {
        ...row,
        logisticsCompany: logistics?.logisticsCompany || '',
        logisticsNo: logistics?.logisticsNo || '',
        abnormalReason: logistics?.abnormalReason || row?.abnormalReason || ''
      };
    } catch {
      return row;
    }
  });
  return Promise.all(tasks);
};

const loadLogisticsAbnormal = async () => {
  logisticsLoading.value = true;
  try {
    const res: any = await listLogisticsAbnormal({
      pageNum: logisticsQuery.pageNum,
      pageSize: logisticsQuery.pageSize
    });
    const source = res?.list || [];
    logisticsTotal.value = Number(res?.total || 0);
    logisticsList.value = await enrichLogisticsFields(source);
  } finally {
    logisticsLoading.value = false;
  }
};

const loadRiskOrders = async () => {
  riskLoading.value = true;
  try {
    const res: any = await listRiskOrders({
      pageNum: riskQuery.pageNum,
      pageSize: riskQuery.pageSize
    });
    riskList.value = res?.list || [];
    riskTotal.value = Number(res?.total || 0);
  } finally {
    riskLoading.value = false;
  }
};

const onLogisticsSizeChange = () => {
  logisticsQuery.pageNum = 1;
  void loadLogisticsAbnormal();
};

const onRiskSizeChange = () => {
  riskQuery.pageNum = 1;
  void loadRiskOrders();
};

const onTabChange = () => {
  refreshCurrent();
};

const refreshCurrent = () => {
  if (activeTab.value === 'logistics') {
    void loadLogisticsAbnormal();
    return;
  }
  void loadRiskOrders();
};

onMounted(() => {
  void loadLogisticsAbnormal();
});
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
