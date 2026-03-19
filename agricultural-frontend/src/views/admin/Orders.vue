<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>订单管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索订单号/手机号"
          style="width: 250px"
          clearable
          @clear="loadOrders"
        >
          <template #append>
            <el-button :icon="Search" @click="loadOrders" />
          </template>
        </el-input>
      </div>
    </div>

    <div class="filter-tabs">
      <el-tabs v-model="activeTab" @tab-change="loadOrders">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待付款" name="1" />
        <el-tab-pane label="待发货" name="2" />
        <el-tab-pane label="待收货" name="3" />
        <el-tab-pane label="已完成" name="4" />
        <el-tab-pane label="已取消" name="5" />
      </el-tabs>
    </div>

    <el-card>
      <el-table :data="orderList" style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column label="商品信息" min-width="250">
          <template #default="{ row }">
            <div class="product-info" v-if="row.items && row.items.length > 0">
              <img :src="row.items[0].productImg" class="product-img" />
              <div class="info-content">
                <div class="name">{{ row.items[0].productName }}</div>
                <div class="desc" v-if="row.items.length > 1">等 {{ row.items.length }} 件商品</div>
              </div>
            </div>
            <div v-else>无商品信息</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="实付款" width="120">
          <template #default="{ row }">
            ¥{{ row.totalAmount.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="receiver" label="买家信息" width="150">
          <template #default="{ row }">
            <div>{{ row.receiver }}</div>
            <div>{{ row.receiverPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button 
              v-if="row.orderStatus === 1"
              link 
              type="danger" 
              @click="handleCancel(row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadOrders"
        />
      </div>
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="800px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentOrder.orderStatus)">
              {{ getStatusText(currentOrder.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="付款时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.receiver }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ currentOrder.receiverAddress }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">商品明细</h4>
        <el-table :data="currentOrder.items || []" border>
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div class="product-info">
                <img :src="row.productImg" class="product-img" />
                <span class="name">{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="productPrice" label="单价" width="100">
            <template #default="{ row }">¥{{ row.productPrice.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="productNum" label="数量" width="80" />
          <el-table-column label="小计" width="100">
            <template #default="{ row }">¥{{ (row.productPrice * row.productNum).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrders, getOrder, cancelOrder } from '@/apis/admin'

const searchKeyword = ref('')
const activeTab = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const orderList = ref<any[]>([])
const detailVisible = ref(false)
const currentOrder = ref<any>(null)

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    1: 'warning',
    2: 'primary',
    3: 'primary',
    4: 'success',
    5: 'info',
    6: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    1: '待付款',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消',
    6: '支付异常'
  }
  return map[status] || '未知状态'
}

const loadOrders = async () => {
  loading.value = true;
  try {
    const params: any = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined
    }
    if (activeTab.value !== 'all') {
      params.status = Number(activeTab.value);
    }
    const res = await listOrders(params);
    if (res) {
      orderList.value = res.list || [];
      total.value = res.total || 0;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadOrders();
})

const viewDetail = async (row: any) => {
  try {
    const res = await getOrder(row.id);
    if (res) {
      currentOrder.value = res;
      detailVisible.value = true;
    }
  } catch (error) {
    console.error(error);
  }
}

const handleCancel = (row: any) => {
  ElMessageBox.prompt('请输入取消原因', '取消订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '原因不能为空'
  }).then(async (result) => {
    const value = (result as { value?: string }).value || '';
    try {
      await cancelOrder(row.id, value);
      ElMessage.success('订单已取消');
      loadOrders();
    } catch (error) {
      console.error(error);
    }
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.orders-page {
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

.filter-tabs {
  margin-bottom: 20px;
  background: #fff;
  padding: 0 20px;
  border-radius: 4px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  object-fit: cover;
}

.info-content {
  flex: 1;

  .name {
    font-size: 14px;
    color: #333;
    margin-bottom: 4px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .desc {
    font-size: 12px;
    color: #999;
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.section-title {
  margin: 24px 0 16px;
  padding-left: 10px;
  border-left: 4px solid var(--el-color-primary);
  font-size: 16px;
}
</style>
