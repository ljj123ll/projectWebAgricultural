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
          <el-button v-if="item.status === 'pending'" type="primary" @click="handleAfterSale(item)">
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('all')
const showHandleDialog = ref(false)
const currentItem = ref<any>({})

const handleForm = reactive({
  result: 'agree',
  remark: ''
})

const afterSalesList = ref([
  {
    id: 1,
    afterSaleNo: 'AS202403080001',
    orderNo: 'ORD202403080001',
    createTime: '2024-03-08 15:30:00',
    productName: '新鲜有机西红柿',
    productImage: 'https://via.placeholder.com/100',
    spec: '5斤装',
    type: 'refund',
    amount: 29.9,
    reason: '商品质量问题',
    description: '收到的西红柿有部分已经腐烂',
    status: 'pending'
  },
  {
    id: 2,
    afterSaleNo: 'AS202403070002',
    orderNo: 'ORD202403070002',
    createTime: '2024-03-07 10:20:00',
    productName: '山东红富士苹果',
    productImage: 'https://via.placeholder.com/100',
    spec: '10斤装',
    type: 'return_refund',
    amount: 59.9,
    reason: '商品与描述不符',
    description: '苹果大小与描述不符，偏小',
    status: 'processing'
  }
])

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    processing: 'primary',
    completed: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    rejected: '已拒绝'
  }
  return map[status] || status
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    refund: '仅退款',
    return_refund: '退货退款',
    exchange: '换货'
  }
  return map[type] || type
}

const handleTabChange = () => {
  // 根据标签筛选数据
}

const handleAfterSale = (item: any) => {
  currentItem.value = item
  showHandleDialog.value = true
}

const viewDetail = (item: any) => {
  void item
}

const confirmHandle = () => {
  ElMessage.success('处理成功')
  showHandleDialog.value = false
}
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
