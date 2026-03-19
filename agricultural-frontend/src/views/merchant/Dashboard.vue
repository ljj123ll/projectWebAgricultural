<template>
  <div class="merchant-dashboard">
    <div class="header-section">
      <h2>工作台</h2>
      <div class="date-display">{{ currentDate }}</div>
    </div>

    <!-- 核心数据卡片 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>今日订单</span>
              <el-tag type="success" size="small">日</el-tag>
            </div>
          </template>
          <div class="card-content">
            <div class="card-value">{{ statistics.todayOrders }}</div>
            <div class="card-footer">
              昨日：{{ statistics.yesterdayOrders }}
              <span :class="['trend', todayOrderTrend >= 0 ? 'up' : 'down']">
                <el-icon><CaretTop v-if="todayOrderTrend >= 0" /><CaretBottom v-else /></el-icon>
                {{ Math.abs(todayOrderTrend) }}%
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card">
          <template #header>
            <div class="card-header">
              <span>今日销售额</span>
              <el-tag type="warning" size="small">日</el-tag>
            </div>
          </template>
          <div class="card-content">
            <div class="card-value">¥ {{ statistics.todaySales.toFixed(2) }}</div>
            <div class="card-footer">
              昨日：¥ {{ statistics.yesterdaySales.toFixed(2) }}
              <span :class="['trend', todaySalesTrend >= 0 ? 'up' : 'down']">
                <el-icon><CaretTop v-if="todaySalesTrend >= 0" /><CaretBottom v-else /></el-icon>
                {{ Math.abs(todaySalesTrend) }}%
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card" @click="router.push('/merchant/products')">
          <template #header>
            <div class="card-header">
              <span>库存预警</span>
              <el-tag type="danger" size="small">急</el-tag>
            </div>
          </template>
          <div class="card-content">
            <div class="card-value danger">{{ statistics.lowStock }}</div>
            <div class="card-footer">
              <span class="action-text">立即补货 ></span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card" @click="router.push('/merchant/orders')">
          <template #header>
            <div class="card-header">
              <span>待处理订单</span>
              <el-tag size="small">待</el-tag>
            </div>
          </template>
          <div class="card-content">
            <div class="card-value primary">{{ statistics.pendingOrders }}</div>
            <div class="card-footer">
              含售后：{{ statistics.pendingAfterSales }} 单
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <!-- 待办事项 -->
      <el-col :xs="24" :md="8">
        <el-card class="todo-card">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
            </div>
          </template>
          <div class="todo-list">
            <div class="todo-item" v-for="(item, index) in todoList" :key="index" @click="handleTodo(item)">
              <div class="todo-icon" :class="item.type">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="todo-info">
                <div class="todo-title">{{ item.title }}</div>
                <div class="todo-desc">{{ item.desc }}</div>
              </div>
              <el-button link type="primary">处理</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 销售趋势 -->
      <el-col :xs="24" :md="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>近7天销售趋势</span>
            </div>
          </template>
          <div class="chart-container" ref="chartRef">
            <div v-if="salesData.length === 0" class="empty-chart">
              <el-empty description="暂无数据" :image-size="80" />
            </div>
            <div v-else class="mock-chart">
              <div class="bar-group" v-for="(item, index) in salesData" :key="index">
                <div class="bar" :style="{ height: calculateBarHeight(item.sales) + 'px' }">
                  <span class="bar-value">{{ item.orderCount }}</span>
                </div>
                <div class="bar-label">{{ item.date }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 热销商品 -->
    <el-card class="mt-20">
      <template #header>
        <div class="card-header">
          <span>热销商品 TOP5</span>
        </div>
      </template>
      <el-table :data="hotProducts" style="width: 100%">
        <el-table-column type="index" label="排名" width="80" align="center">
          <template #default="scope">
            <div class="rank-badge" :class="{ top: scope.$index < 3 }">{{ scope.$index + 1 }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" />
        <el-table-column prop="salesVolume" label="销量" width="120" sortable />
        <el-table-column label="销售额" width="150">
          <template #default="scope">¥ {{ calculateProductRevenue(scope.row).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { CaretTop, CaretBottom, Box, List, Service } from '@element-plus/icons-vue';
import { getMerchantStats } from '@/apis/merchant';

const router = useRouter();
const currentDate = new Date().toLocaleDateString();

// 统计数据
const statistics = ref({
  todayOrders: 0,
  yesterdayOrders: 0,
  todaySales: 0.00,
  yesterdaySales: 0.00,
  lowStock: 0,
  pendingOrders: 0,
  pendingAfterSales: 0
});

// 销售趋势数据
const salesData = ref<any[]>([]);

// 热销商品数据
const hotProducts = ref<any[]>([]);

// 计算今日订单趋势
const todayOrderTrend = computed(() => {
  if (statistics.value.yesterdayOrders === 0) return 0;
  return Math.round(((statistics.value.todayOrders - statistics.value.yesterdayOrders) / statistics.value.yesterdayOrders) * 100);
});

// 计算今日销售额趋势
const todaySalesTrend = computed(() => {
  if (statistics.value.yesterdaySales === 0) return 0;
  return Math.round(((statistics.value.todaySales - statistics.value.yesterdaySales) / statistics.value.yesterdaySales) * 100);
});

// 计算柱状图高度
const calculateBarHeight = (sales: number) => {
  const maxSales = Math.max(...salesData.value.map(d => d.sales || 0), 1);
  return Math.max((sales / maxSales) * 150, 10);
};

// 计算商品销售额（根据平均单价估算）
const calculateProductRevenue = (product: any) => {
  // 由于没有具体的销售额数据，使用一个估算值
  return (product.salesVolume || 0) * 15; // 假设平均单价15元
};

// 待办事项
const todoList = ref([
  { title: '待发货订单', desc: '暂无数据', icon: Box, type: 'warning', route: '/merchant/orders?status=2' },
  { title: '库存预警', desc: '暂无数据', icon: List, type: 'danger', route: '/merchant/products' },
  { title: '售后申请', desc: '暂无数据', icon: Service, type: 'primary', route: '/merchant/after-sales' }
]);

const handleTodo = (item: any) => {
  if (item.route) {
    router.push(item.route);
  }
};

const loadStats = async () => {
  try {
    const res = await getMerchantStats();
    if (res) {
      // 更新统计数据
      statistics.value.todayOrders = res.todayOrders || 0;
      statistics.value.yesterdayOrders = res.yesterdayOrders || 0;
      statistics.value.todaySales = res.todaySales || 0;
      statistics.value.yesterdaySales = res.yesterdaySales || 0;
      statistics.value.lowStock = res.lowStock || 0;
      statistics.value.pendingOrders = res.pendingOrders || 0;
      statistics.value.pendingAfterSales = res.pendingAfterSales || 0;
      
      // 更新销售趋势数据
      salesData.value = res.salesTrend || [];
      
      // 更新热销商品
      hotProducts.value = res.hotProducts || [];
      
      // 更新待办事项描述
      if (todoList.value.length >= 3) {
      if (todoList.value[0]) todoList.value[0].desc = `${res.pendingOrders || 0} 个订单待发货`;
      if (todoList.value[1]) todoList.value[1].desc = `${res.lowStock || 0} 个商品库存不足`;
      if (todoList.value[2]) todoList.value[2].desc = `${res.pendingAfterSales || 0} 个售后待处理`;
      }
    }
  } catch (error) {
    console.error('获取统计数据失败', error);
  }
};

onMounted(() => {
  loadStats();
});
</script>

<style scoped lang="scss">
.merchant-dashboard {
  .header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 { margin: 0; color: #303133; }
    .date-display { color: #909399; font-size: 14px; }
  }
}

.data-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform 0.3s;
  
  &:hover { transform: translateY(-5px); }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .card-value {
    font-size: 28px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 10px;
    
    &.danger { color: #F56C6C; }
    &.primary { color: #409eff; }
  }
  
  .card-footer {
    font-size: 13px;
    color: #909399;
    display: flex;
    align-items: center;
    justify-content: space-between;
    
    .trend {
      display: flex;
      align-items: center;
      &.up { color: #67c23a; }
      &.down { color: #F56C6C; }
    }
    
    .action-text { color: #409eff; }
  }
}

.todo-list {
  .todo-item {
    display: flex;
    align-items: center;
    padding: 15px 0;
    border-bottom: 1px solid #f0f2f5;
    cursor: pointer;
    
    &:last-child { border-bottom: none; }
    
    .todo-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 15px;
      color: #fff;
      
      &.warning { background: #E6A23C; }
      &.danger { background: #F56C6C; }
      &.primary { background: #409eff; }
    }
    
    .todo-info {
      flex: 1;
      .todo-title { font-size: 14px; color: #303133; font-weight: 500; margin-bottom: 4px; }
      .todo-desc { font-size: 12px; color: #909399; }
    }
  }
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 20px;
  
  .empty-chart {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .mock-chart {
    display: flex;
    align-items: flex-end;
    gap: 30px;
    height: 100%;
    width: 100%;
    justify-content: space-around;
    
    .bar-group {
      display: flex;
      flex-direction: column;
      align-items: center;
      flex: 1;
      max-width: 60px;
      
      .bar {
        width: 30px;
        background: #409eff;
        border-radius: 4px 4px 0 0;
        position: relative;
        transition: height 0.5s ease;
        min-height: 10px;
        
        &:hover { opacity: 0.8; }
        
        .bar-value {
          position: absolute;
          top: -20px;
          left: 50%;
          transform: translateX(-50%);
          font-size: 12px;
          color: #606266;
          white-space: nowrap;
        }
      }
      
      .bar-label {
        margin-top: 10px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.rank-badge {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #dcdfe6;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  margin: 0 auto;
  
  &.top { background: #F56C6C; }
}

.text-danger { color: #F56C6C; font-weight: bold; }

.mt-20 { margin-top: 20px; }
</style>
