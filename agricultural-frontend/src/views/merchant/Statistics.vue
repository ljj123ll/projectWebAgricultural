<template>
  <div class="statistics-page">
    <div class="page-header">
      <h2>数据统计</h2>
      <el-radio-group v-model="timeRange" size="small">
        <el-radio-button label="today">今日</el-radio-button>
        <el-radio-button label="week">本周</el-radio-button>
        <el-radio-button label="month">本月</el-radio-button>
        <el-radio-button label="year">本年</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 数据概览 -->
    <div class="stats-overview">
      <el-row :gutter="16">
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">¥{{ stats.sales.toFixed(2) }}</div>
            <div class="stat-label">销售额</div>
            <div class="stat-trend" :class="stats.salesTrend >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="stats.salesTrend >= 0" /><ArrowDown v-else /></el-icon>
              {{ Math.abs(stats.salesTrend) }}%
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">{{ stats.orders }}</div>
            <div class="stat-label">订单数</div>
            <div class="stat-trend" :class="stats.ordersTrend >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="stats.ordersTrend >= 0" /><ArrowDown v-else /></el-icon>
              {{ Math.abs(stats.ordersTrend) }}%
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">{{ stats.visitors }}</div>
            <div class="stat-label">访客数</div>
            <div class="stat-trend" :class="stats.visitorsTrend >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="stats.visitorsTrend >= 0" /><ArrowDown v-else /></el-icon>
              {{ Math.abs(stats.visitorsTrend) }}%
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-value">{{ stats.conversionRate }}%</div>
            <div class="stat-label">转化率</div>
            <div class="stat-trend" :class="stats.conversionTrend >= 0 ? 'up' : 'down'">
              <el-icon><ArrowUp v-if="stats.conversionTrend >= 0" /><ArrowDown v-else /></el-icon>
              {{ Math.abs(stats.conversionTrend) }}%
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
        <div class="mock-chart">
          <div v-for="(bar, index) in salesTrend" :key="index" class="bar-item">
            <div class="bar" :style="{ height: bar.value + '%' }"></div>
            <span class="bar-label">{{ bar.label }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 商品排行 -->
    <el-card class="rank-card">
      <template #header>
        <span>热销商品TOP5</span>
      </template>
      <div class="product-rank">
        <div v-for="(product, index) in topProducts" :key="index" class="rank-item">
          <div class="rank-number" :class="{ top3: index < 3 }">{{ index + 1 }}</div>
          <img :src="product.image" class="product-image" />
          <div class="product-info">
            <h4>{{ product.name }}</h4>
            <p class="sales">销量：{{ product.sales }}</p>
          </div>
          <div class="product-amount">¥{{ product.amount.toFixed(2) }}</div>
        </div>
      </div>
    </el-card>

    <!-- 订单状态分布 -->
    <el-card class="chart-card">
      <template #header>
        <span>订单状态分布</span>
      </template>
      <div class="status-distribution">
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
import { ref, reactive } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'

const timeRange = ref('today')

const stats = reactive({
  sales: 12580.5,
  salesTrend: 12.5,
  orders: 156,
  ordersTrend: 8.3,
  visitors: 2340,
  visitorsTrend: -5.2,
  conversionRate: 6.8,
  conversionTrend: 2.1
})

const salesTrend = ref([
  { label: '00:00', value: 30 },
  { label: '04:00', value: 20 },
  { label: '08:00', value: 50 },
  { label: '12:00', value: 80 },
  { label: '16:00', value: 65 },
  { label: '20:00', value: 90 },
  { label: '24:00', value: 40 }
])

const topProducts = ref([
  { name: '新鲜有机西红柿', image: 'https://via.placeholder.com/60', sales: 256, amount: 7644.8 },
  { name: '山东红富士苹果', image: 'https://via.placeholder.com/60', sales: 189, amount: 11334.9 },
  { name: '五常大米', image: 'https://via.placeholder.com/60', sales: 156, amount: 12480 },
  { name: '土鸡蛋', image: 'https://via.placeholder.com/60', sales: 134, amount: 5360 },
  { name: '新鲜黄瓜', image: 'https://via.placeholder.com/60', sales: 98, amount: 1960 }
])

const orderStatus = ref([
  { name: '已完成', count: 120, percentage: 60, color: '#67c23a' },
  { name: '待发货', count: 45, percentage: 22.5, color: '#409eff' },
  { name: '待收货', count: 25, percentage: 12.5, color: '#e6a23c' },
  { name: '售后中', count: 10, percentage: 5, color: '#f56c6c' }
])
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

    .product-image {
      width: 50px;
      height: 50px;
      border-radius: 4px;
      object-fit: cover;
      margin-right: 12px;
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
