<template>
  <div class="after-sales-page">
    <div class="page-header">
      <h2>售后处理</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待处理" name="pending" />
      <el-tab-pane label="处理中" name="processing" />
      <el-tab-pane label="已完成" name="completed" />
    </el-tabs>

    <div class="after-sales-list">
      <div v-for="item in afterSalesList" :key="item.id" class="after-sale-card">
        <div class="card-header">
          <span class="after-sale-no">售后单号：{{ item.afterSaleNo }}</span>
          <el-tag :type="getStatusType(item.status)">{{ getStatusText(item.status) }}</el-tag>
        </div>
        
        <div class="order-info">
          <span>关联订单：{{ item.orderNo }}</span>
          <span class="time">{{ item.createTime }}</span>
        </div>

        <div class="product-info">
          <img :src="item.productImage" class="product-image" />
          <div class="product-detail">
            <h4>{{ item.productName }}</h4>
            <p class="spec">{{ item.spec }}</p>
          </div>
        </div>

        <div class="after-sale-info">
          <div class="info-row">
            <span class="label">售后类型：</span>
            <span>{{ getTypeText(item.type) }}</span>
          </div>
          <div class="info-row">
            <span class="label">退款金额：</span>
            <span class="amount">¥{{ item.amount.toFixed(2) }}</span>
          </div>
          <div class="info-row">
            <span class="label">申请原因：</span>
            <span>{{ item.reason }}</span>
          </div>
          <div class="info-row">
            <span class="label">问题描述：</span>
            <span>{{ item.description }}</span>
          </div>
        </div>

        <div class="card-actions">
          <el-button v-if="item.status === 1" type="primary" @click="openHandleDialog(item)">
            立即处理
          </el-button>
          <el-button link type="primary" @click="viewDetail(item)">
            查看详情
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="afterSalesList.length === 0" description="暂无售后申请" />

    <!-- 处理售后弹窗 -->
    <el-dialog v-model="showHandleDialog" title="处理售后申请" width="90%">
      <div class="handle-content">
        <div class="info-section">
          <h4>售后信息</h4>
          <p><strong>类型：</strong>{{ getTypeText(currentItem.type) }}</p>
          <p><strong>金额：</strong>¥{{ currentItem.amount?.toFixed(2) }}</p>
          <p><strong>原因：</strong>{{ currentItem.reason }}</p>
          <p><strong>描述：</strong>{{ currentItem.description }}</p>
        </div>
        
        <el-divider />
        
        <el-form :model="handleForm" label-position="top">
          <el-form-item label="处理结果">
            <el-radio-group v-model="handleForm.result">
              <el-radio label="agree">同意退款</el-radio>
              <el-radio label="reject">拒绝退款</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理说明">
            <el-input
              v-model="handleForm.remark"
              type="textarea"
              rows="3"
              placeholder="请输入处理说明"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listAfterSale, handleAfterSale } from '@/apis/merchant'

const activeTab = ref('all')
const showHandleDialog = ref(false)
const currentItem = ref<any>({})

const handleForm = reactive({
  result: 'agree',
  remark: ''
})

const afterSalesList = ref<any[]>([])

const getStatusType = (status: number) => {
  if (status === 1) return 'warning' // 待商家处理
  if (status === 2) return 'info' // 协商中
  if (status === 3 || status === 4) return 'success' // 已解决/管理员介入
  if (status === 5) return 'danger' // 已驳回
  return 'info'
}

const getStatusText = (status: number) => {
  if (status === 1) return '待商家处理'
  if (status === 2) return '协商中'
  if (status === 3) return '已解决'
  if (status === 4) return '管理员介入'
  if (status === 5) return '已驳回'
  return '未知状态'
}

const getTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '仅退款',
    2: '退货退款',
    3: '换货'
  }
  return map[type] || '未知类型'
}

const loadAfterSales = async () => {
  try {
    let statusParam = undefined;
    if (activeTab.value === 'pending') statusParam = 1; // 待商家处理
    else if (activeTab.value === 'processing') statusParam = 2; // 协商中
    else if (activeTab.value === 'completed') statusParam = 3; // 已解决
    
    const res = await listAfterSale({ pageNum: 1, pageSize: 20, afterSaleStatus: statusParam });
    if (res && res.list) {
      afterSalesList.value = res.list.map((item: any) => ({
        id: item.id,
        afterSaleNo: item.afterSaleNo,
        orderNo: item.orderNo,
        createTime: item.createTime,
        productName: '订单商品', // Need API update to get product details, using placeholder
        productImage: '', // Need API update
        spec: '', // Need API update
        type: item.afterSaleType,
        amount: 0, // Need API update to get refund amount
        reason: item.applyReason,
        description: item.applyReason,
        status: item.afterSaleStatus
      }));
    }
  } catch (error) {
    console.error('Failed to load after sales', error);
  }
}

const handleTabChange = () => {
  loadAfterSales()
}

const openHandleDialog = (item: any) => {
  currentItem.value = item
  showHandleDialog.value = true
}

const viewDetail = (item: any) => {
  void item
}

const confirmHandle = async () => {
  try {
    const agree = handleForm.result === 'agree'
    await handleAfterSale(currentItem.value.id, {
      agree,
      handleResult: handleForm.remark || (agree ? '同意退款' : '拒绝退款')
    })
    ElMessage.success('处理成功')
    showHandleDialog.value = false
    loadAfterSales()
  } catch (error) {
    console.error('Handle failed', error)
  }
}

onMounted(() => {
  loadAfterSales()
})
</script>

<style scoped lang="scss">
.after-sales-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
  }
}

.after-sales-list {
  margin-top: 20px;
}

.after-sale-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .after-sale-no {
      color: #666;
      font-size: 14px;
    }
  }

  .order-info {
    display: flex;
    justify-content: space-between;
    color: #999;
    font-size: 12px;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;
  }

  .product-info {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;

    .product-image {
      width: 60px;
      height: 60px;
      border-radius: 4px;
      object-fit: cover;
    }

    .product-detail {
      h4 {
        margin: 0 0 4px 0;
        font-size: 14px;
      }

      .spec {
        color: #999;
        font-size: 12px;
        margin: 0;
      }
    }
  }

  .after-sale-info {
    background: #f5f5f5;
    border-radius: 4px;
    padding: 12px;
    margin-bottom: 12px;

    .info-row {
      display: flex;
      margin-bottom: 8px;
      font-size: 14px;

      &:last-child {
        margin-bottom: 0;
      }

      .label {
        color: #666;
        width: 80px;
        flex-shrink: 0;
      }

      .amount {
        color: #f56c6c;
        font-weight: 600;
      }
    }
  }

  .card-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

.handle-content {
  .info-section {
    h4 {
      margin: 0 0 12px 0;
    }

    p {
      margin: 8px 0;
      color: #666;
    }
  }
}
</style>
