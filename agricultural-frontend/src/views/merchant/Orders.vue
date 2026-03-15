<template>
  <div class="merchant-orders">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="待发货" name="2"></el-tab-pane>
      <el-tab-pane label="已发货" name="3"></el-tab-pane>
      <el-tab-pane label="已完成" name="4"></el-tab-pane>
      <el-tab-pane label="售后中" name="aftersale"></el-tab-pane>
    </el-tabs>

    <el-table :data="orderList" style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column label="商品" width="250">
        <template #default="scope">
          <div v-for="item in scope.row.orderItems" :key="item.id">
            {{ item.productName }} x {{ item.productNum }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="100" />
      <el-table-column label="收货信息" width="200">
        <template #default="scope">
          <div>{{ scope.row.receiver }} {{ scope.row.receiverPhone }}</div>
          <div>{{ scope.row.receiverAddress }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
           <el-tag>{{ getStatusText(scope.row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button v-if="scope.row.orderStatus === 2" type="primary" size="small" @click="handleShip(scope.row)">发货</el-button>
          <el-button type="text" size="small">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialogVisible" title="订单发货" width="400px">
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择">
            <el-option label="顺丰速运" value="SF" />
            <el-option label="中国邮政" value="EMS" />
            <el-option label="中通快递" value="ZTO" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.logisticsNo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="shipDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmShip">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import type { Order } from '@/types';
import { listOrders, shipOrder } from '@/apis/merchant';

const activeTab = ref('2');
const orderList = ref<Order[]>([]);

const shipDialogVisible = ref(false);
const currentOrderId = ref<number>(0);
const shipForm = reactive({
  logisticsCompany: '',
  logisticsNo: ''
});

const getStatusText = (status: number) => {
    // 1-待付款 2-待发货 3-待收货 4-已完成
    if (status === 2) return '待发货';
    if (status === 3) return '已发货';
    if (status === 4) return '已完成';
    return status;
}

const handleTabClick = () => {
  loadOrders();
};

const handleShip = (row: Order) => {
  currentOrderId.value = row.id;
  shipDialogVisible.value = true;
};

const confirmShip = async () => {
  await shipOrder(currentOrderId.value, shipForm);
  const order = orderList.value.find((o: Order) => o.id === currentOrderId.value);
  if (order) order.orderStatus = 3;
  shipDialogVisible.value = false;
  ElMessage.success('发货成功');
};

const loadOrders = async () => {
  const res = await listOrders({ pageNum: 1, pageSize: 20, orderStatus: Number(activeTab.value) });
  if (res?.list) orderList.value = res.list;
};

onMounted(() => {
  loadOrders();
});
</script>
