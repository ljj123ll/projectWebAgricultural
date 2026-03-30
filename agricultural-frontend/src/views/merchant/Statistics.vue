<template>
  <div class="statistics-page">
    <div class="page-header">
      <h2>数据统计</h2>
      <el-radio-group v-model="timeRange" size="small" @change="loadStatistics">
        <el-radio-button label="today">今日</el-radio-button>
        <el-radio-button label="week">本周</el-radio-button>
        <el-radio-button label="month">本月</el-radio-button>
        <el-radio-button label="year">本年</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 数据概览 -->
    <div class="stats-overview" v-loading="loading">
      <el-row :gutter="16">
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">¥{{ formatMoney(stats.sales) }}</div>
            <div class="stat-label">销售额</div>
            <div class="stat-trend" :class="toNumber(stats.salesTrend) >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="toNumber(stats.salesTrend) >= 0" /><ArrowDown v-else /></el-icon>
              {{ formatPercent(stats.salesTrend) }}%
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">{{ formatInteger(stats.orders) }}</div>
            <div class="stat-label">订单数</div>
            <div class="stat-trend" :class="toNumber(stats.ordersTrend) >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="toNumber(stats.ordersTrend) >= 0" /><ArrowDown v-else /></el-icon>
              {{ formatPercent(stats.ordersTrend) }}%
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">{{ formatInteger(stats.visitors) }}</div>
            <div class="stat-label">访客数</div>
            <div class="stat-trend" :class="toNumber(stats.visitorsTrend) >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="toNumber(stats.visitorsTrend) >= 0" /><ArrowDown v-else /></el-icon>
              {{ formatPercent(stats.visitorsTrend) }}%
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">{{ formatPercent(stats.conversionRate) }}%</div>
            <div class="stat-label">转化率</div>
            <div class="stat-trend" :class="toNumber(stats.conversionTrend) >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="toNumber(stats.conversionTrend) >= 0" /><ArrowDown v-else /></el-icon>
              {{ formatPercent(stats.conversionTrend) }}%
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 销售趋势 -->
    <el-card class="chart-card">
      <template #header>
        <span>销售趋势</span>
      </template>
      <div class="chart-placeholder">
        <div v-if="salesTrend.length === 0" class="empty-chart">
          <el-empty description="暂无数据" :image-size="80" />
        </div>
        <div v-else class="mock-chart">
          <div v-for="(bar, index) in salesTrend" :key="index" class="bar-item">
            <div class="bar" :style="{ height: calculateBarHeight(bar.sales) + '%' }"></div>
            <span class="bar-label">{{ bar.date }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 商品排行 -->
    <el-card class="rank-card">
      <template #header>
        <span>热销商品TOP5</span>
      </template>
      <div v-if="topProducts.length === 0" class="empty-list">
        <el-empty description="暂无数据" :image-size="80" />
      </div>
      <div v-else class="product-rank">
        <div v-for="(product, index) in topProducts" :key="index" class="rank-item">
          <div class="rank-number" :class="{ top3: index < 3 }">{{ index + 1 }}</div>
          <div class="product-image-placeholder">
            <el-icon><Goods /></el-icon>
          </div>
          <div class="product-info">
            <h4>{{ product.productName }}</h4>
            <p class="sales">销量：{{ formatInteger(product.salesVolume) }}</p>
          </div>
          <div class="product-amount">¥{{ formatMoney(product.salesAmount ?? product.amount ?? Number(product.salesVolume || 0) * 15) }}</div>
        </div>
      </div>
    </el-card>

    <!-- 订单状态分布 -->
    <el-card class="chart-card">
      <template #header>
        <span>订单状态分布</span>
      </template>
      <div v-if="orderStatus.length === 0" class="empty-list">
        <el-empty description="暂无数据" :image-size="80" />
      </div>
      <div v-else class="status-distribution">
        <div v-for="(item, index) in orderStatus" :key="index" class="status-item">
          <div class="status-info">
            <span class="status-name">{{ item.name }}</span>
            <span class="status-count">{{ item.count }}单</span>
          </div>
          <el-progress :percentage="item.percentage" :color="item.color" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ArrowUp, ArrowDown, Goods } from '@element-plus/icons-vue'
import { getMerchantStatistics } from '@/apis/merchant'
import { ElMessage } from 'element-plus'

const timeRange = ref('week')
const loading = ref(false)

const stats = reactive({
  sales: 0,
  salesTrend: 0,
  orders: 0,
  ordersTrend: 0,
  visitors: 0,
  visitorsTrend: 0,
  conversionRate: 0,
  conversionTrend: 0
})

const salesTrend = ref<any[]>([])
const topProducts = ref<any[]>([])
const orderStatus = ref<any[]>([])

const toNumber = (value: unknown) => {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num : 0
}

const formatMoney = (value: unknown) => toNumber(value).toFixed(2)
const formatPercent = (value: unknown) => toNumber(value).toFixed(1)
const formatInteger = (value: unknown) => Math.round(toNumber(value))

const resetStatistics = () => {
  stats.sales = 0
  stats.salesTrend = 0
  stats.orders = 0
  stats.ordersTrend = 0
  stats.visitors = 0
  stats.visitorsTrend = 0
  stats.conversionRate = 0
  stats.conversionTrend = 0
  salesTrend.value = []
  topProducts.value = []
  orderStatus.value = []
}

// 计算柱状图高度
const calculateBarHeight = (sales: number) => {
  const maxSales = Math.max(...salesTrend.value.map((d: any) => toNumber(d.sales)), 1)
  return Math.max((toNumber(sales) / maxSales) * 100, 5)
}

const loadStatistics = async () => {
  loading.value = true
  try {
    const res = await getMerchantStatistics(timeRange.value)
    stats.sales = toNumber(res?.sales)
    stats.salesTrend = toNumber(res?.salesTrend)
    stats.orders = formatInteger(res?.orders)
    stats.ordersTrend = toNumber(res?.ordersTrend)
    stats.visitors = formatInteger(res?.visitors)
    stats.visitorsTrend = toNumber(res?.visitorsTrend)
    stats.conversionRate = toNumber(res?.conversionRate)
    stats.conversionTrend = toNumber(res?.conversionTrend)

    salesTrend.value = Array.isArray(res?.salesTrendData)
      ? res.salesTrendData.map((item: any) => ({
          date: item?.date || '-',
          sales: toNumber(item?.sales)
        }))
      : []

    topProducts.value = Array.isArray(res?.topProducts)
      ? res.topProducts.map((item: any) => ({
          ...item,
          productName: item?.productName || '未命名商品',
          salesVolume: formatInteger(item?.salesVolume),
          salesAmount: toNumber(item?.salesAmount ?? item?.amount)
        }))
      : []

    orderStatus.value = Array.isArray(res?.orderStatusDistribution)
      ? res.orderStatusDistribution.map((item: any) => ({
          ...item,
          name: item?.name || '未知状态',
          count: formatInteger(item?.count),
          percentage: Math.min(Math.max(toNumber(item?.percentage), 0), 100)
        }))
      : []
  } catch (error) {
    console.error('获取统计数据失败', error)
    resetStatistics()
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped lang="scss">
.statistics-page {
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
}

.stats-overview {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  margin-bottom: 16px;

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }

  .stat-label {
    color: #666;
    font-size: 14px;
    margin-bottom: 8px;
  }

  .stat-trend {
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;

    &.up {
      color: #67c23a;
    }

    &.down {
      color: #f56c6c;
    }
  }
}

.chart-card,
.rank-card {
  margin-bottom: 20px;
}

.empty-chart,
.empty-list {
  padding: 40px 0;
}

.chart-placeholder {
  height: 200px;
  display: flex;
  align-items: flex-end;
  padding: 20px 0;

  .mock-chart {
    display: flex;
    justify-content: space-around;
    align-items: flex-end;
    width: 100%;
    height: 100%;

    .bar-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      flex: 1;

      .bar {
        width: 30px;
        background: linear-gradient(to top, #67c23a, #95d475);
        border-radius: 4px 4px 0 0;
        transition: height 0.3s;
        min-height: 5px;
      }

      .bar-label {
        font-size: 12px;
        color: #999;
        margin-top: 8px;
      }
    }
  }
}

.product-rank {
  .rank-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
    }

    .rank-number {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: #e0e0e0;
      color: #666;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
      margin-right: 12px;

      &.top3 {
        background: #f56c6c;
        color: #fff;
      }
    }

    .product-image-placeholder {
      width: 50px;
      height: 50px;
      border-radius: 4px;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12px;
      color: #909399;
    }

    .product-info {
      flex: 1;

      h4 {
        margin: 0 0 4px 0;
        font-size: 14px;
      }

      .sales {
        color: #999;
        font-size: 12px;
        margin: 0;
      }
    }

    .product-amount {
      color: #f56c6c;
      font-weight: 600;
    }
  }
}

.status-distribution {
  .status-item {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }

    .status-info {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;

      .status-name {
        color: #666;
      }

      .status-count {
        color: #333;
        font-weight: 600;
      }
    }
  }
}
</style>
