<template>
  <div class="order-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>订单详情</h2>
      </div>
    </div>

    <div class="order-content">
      <!-- 订单状态 -->
      <el-card class="status-card">
        <div class="status-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="getStatusType(order.status)" size="large">
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>
        <div class="status-timeline">
          <el-steps :active="getActiveStep(order.status)" simple>
            <el-step title="待付款" />
            <el-step title="待发货" />
            <el-step title="待收货" />
            <el-step title="已完成" />
          </el-steps>
        </div>
      </el-card>

      <!-- 商品信息 -->
      <el-card class="info-card">
        <template #header>
          <span>商品信息</span>
        </template>
        <div class="product-list">
          <div v-for="item in order.products" :key="item.id" class="product-item">
            <img :src="item.image" class="product-image" />
            <div class="product-info">
              <h4>{{ item.name }}</h4>
              <p class="spec">{{ item.spec }}</p>
              <div class="price-row">
                <span class="price">¥{{ item.price.toFixed(2) }}</span>
                <span class="quantity">x{{ item.quantity }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="order-summary">
          <div class="summary-row">
            <span>商品总价</span>
            <span>¥{{ order.productAmount.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>运费</span>
            <span>¥{{ order.shippingFee.toFixed(2) }}</span>
          </div>
          <div class="summary-row total">
            <span>订单实付</span>
            <span class="total-price">¥{{ order.totalAmount.toFixed(2) }}</span>
          </div>
        </div>
      </el-card>

      <!-- 收货信息 -->
      <el-card class="info-card">
        <template #header>
          <span>收货信息</span>
        </template>
        <div class="info-list">
          <div class="info-item">
            <span class="label">收货人：</span>
            <span>{{ order.receiver.name }}</span>
          </div>
          <div class="info-item">
            <span class="label">手机号：</span>
            <span>{{ order.receiver.phone }}</span>
          </div>
          <div class="info-item">
            <span class="label">收货地址：</span>
            <span>{{ order.receiver.address }}</span>
          </div>
        </div>
      </el-card>

      <!-- 订单信息 -->
      <el-card class="info-card">
        <template #header>
          <span>订单信息</span>
        </template>
        <div class="info-list">
          <div class="info-item">
            <span class="label">下单时间：</span>
            <span>{{ order.createTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">支付方式：</span>
            <span>{{ order.payMethod }}</span>
          </div>
          <div class="info-item">
            <span class="label">支付时间：</span>
            <span>{{ order.payTime || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">买家备注：</span>
            <span>{{ order.remark || '无' }}</span>
          </div>
        </div>
      </el-card>

      <!-- 物流信息 -->
      <el-card v-if="order.logistics" class="info-card">
        <template #header>
          <span>物流信息</span>
        </template>
        <div class="info-list">
          <div class="info-item">
            <span class="label">物流公司：</span>
            <span>{{ order.logistics.company }}</span>
          </div>
          <div class="info-item">
            <span class="label">运单号：</span>
            <span>{{ order.logistics.trackingNo }}</span>
          </div>
          <div class="info-item">
            <span class="label">发货时间：</span>
            <span>{{ order.logistics.shipTime }}</span>
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button v-if="order.status === 'pending_shipment'" type="primary" size="large" @click="showShipDialog = true">
          立即发货
        </el-button>
        <el-button v-if="order.status === 'pending_payment'" type="danger" size="large" @click="cancelOrder">
          取消订单
        </el-button>
      </div>
    </div>

    <!-- 发货弹窗 -->
    <el-dialog v-model="showShipDialog" title="订单发货" width="90%">
      <el-form :model="shipForm" label-position="top">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.company" placeholder="请选择物流公司" style="width: 100%">
            <el-option label="顺丰速运" value="sf" />
            <el-option label="中通快递" value="zt" />
            <el-option label="圆通速递" value="yt" />
            <el-option label="韵达快递" value="yd" />
            <el-option label="申通快递" value="st" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShipDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'

const route = useRoute()

const showShipDialog = ref(false)
const shipForm = reactive({
  company: '',
  trackingNo: ''
})

const order = reactive<{
  orderNo: string
  status: string
  products: Array<{
    id: number
    name: string
    spec: string
    price: number
    quantity: number
    image: string
  }>
  productAmount: number
  shippingFee: number
  totalAmount: number
  receiver: {
    name: string
    phone: string
    address: string
  }
  createTime: string
  payMethod: string
  payTime: string
  remark: string
  logistics: {
    company: string
    trackingNo: string
    shipTime: string
  } | null
}>({
  orderNo: 'ORD202403080001',
  status: 'pending_shipment',
  products: [
    {
      id: 1,
      name: '新鲜有机西红柿',
      spec: '5斤装',
      price: 29.9,
      quantity: 2,
      image: 'https://via.placeholder.com/100'
    }
  ],
  productAmount: 59.8,
  shippingFee: 0,
  totalAmount: 59.8,
  receiver: {
    name: '张三',
    phone: '138****8888',
    address: '北京市朝阳区某某街道某某小区1号楼101室'
  },
  createTime: '2024-03-08 10:30:00',
  payMethod: '微信支付',
  payTime: '2024-03-08 10:32:15',
  remark: '请尽快发货',
  logistics: {
    company: '顺丰速运',
    trackingNo: 'SF1234567890',
    shipTime: '2024-03-08 12:00:00'
  }
})

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending_payment: 'warning',
    pending_shipment: 'primary',
    pending_receipt: 'success',
    completed: 'success',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending_payment: '待付款',
    pending_shipment: '待发货',
    pending_receipt: '待收货',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
}

const getActiveStep = (status: string) => {
  const map: Record<string, number> = {
    pending_payment: 1,
    pending_shipment: 2,
    pending_receipt: 3,
    completed: 4
  }
  return map[status] || 1
}

const confirmShip = () => {
  if (!shipForm.company || !shipForm.trackingNo) {
    ElMessage.warning('请填写完整物流信息')
    return
  }
  ElMessage.success('发货成功')
  showShipDialog.value = false
}

const cancelOrder = () => {
  ElMessage.success('订单已取消')
}
</script>

<style scoped lang="scss">
.order-detail-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 20px;
    }
  }
}

.status-card {
  margin-bottom: 16px;

  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .order-no {
      color: #666;
      font-size: 14px;
    }
  }
}

.info-card {
  margin-bottom: 16px;
}

.product-list {
  .product-item {
    display: flex;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
    }

    .product-image {
      width: 80px;
      height: 80px;
      border-radius: 8px;
      object-fit: cover;
    }

    .product-info {
      flex: 1;

      h4 {
        margin: 0 0 8px 0;
        font-size: 14px;
      }

      .spec {
        color: #999;
        font-size: 12px;
        margin: 0 0 8px 0;
      }

      .price-row {
        display: flex;
        justify-content: space-between;

        .price {
          color: #f56c6c;
          font-weight: 600;
        }

        .quantity {
          color: #999;
        }
      }
    }
  }
}

.order-summary {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;

  .summary-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    color: #666;

    &.total {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #ebeef5;
      font-weight: 600;
      color: #333;

      .total-price {
        color: #f56c6c;
        font-size: 18px;
      }
    }
  }
}

.info-list {
  .info-item {
    display: flex;
    padding: 8px 0;

    .label {
      color: #999;
      width: 100px;
      flex-shrink: 0;
    }
  }
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.order-content {
  padding-bottom: 80px;
}
</style>
