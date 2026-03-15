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
              <span class="trend up"><el-icon><CaretTop /></el-icon> 12%</span>
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
              <span class="trend down"><el-icon><CaretBottom /></el-icon> 5%</span>
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
            <!-- 模拟图表展示 -->
            <div class="mock-chart">
              <div class="bar-group" v-for="(item, index) in salesData" :key="index">
                <div class="bar" :style="{ height: item.value / 2 + 'px' }">
                  <span class="bar-value">{{ item.value }}</span>
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
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="sales" label="销量" width="120" sortable />
        <el-table-column prop="revenue" label="销售额" width="150" sortable>
          <template #default="scope">¥ {{ scope.row.revenue.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="剩余库存" width="120">
          <template #default="scope">
            <span :class="{ 'text-danger': scope.row.stock < 10 }">{{ scope.row.stock }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { CaretTop, CaretBottom, Box, List, Service } from '@element-plus/icons-vue';

const router = useRouter();
const currentDate = new Date().toLocaleDateString();

// 模拟统计数据
const statistics = ref({
  todayOrders: 12,
  yesterdayOrders: 10,
  todaySales: 1203.00,
  yesterdaySales: 1260.50,
  lowStock: 5,
  pendingOrders: 8,
  pendingAfterSales: 2
});

// 待办事项
const todoList = ref([
  { title: '待发货订单', desc: '5个订单等待发货', icon: 'Box', type: 'warning', route: '/merchant/orders?status=2' },
  { title: '库存预警', desc: '3个商品库存不足10件', icon: 'List', type: 'danger', route: '/merchant/products' },
  { title: '售后申请', desc: '2个新的售后申请', icon: 'Service', type: 'primary', route: '/merchant/after-sales' }
]);

// 销售趋势数据 (模拟)
const salesData = ref([
  { date: '03-01', value: 800 },
  { date: '03-02', value: 1200 },
  { date: '03-03', value: 950 },
  { date: '03-04', value: 1500 },
  { date: '03-05', value: 1100 },
  { date: '03-06', value: 1300 },
  { date: '03-07', value: 1203 }
]);

// 热销商品数据
const hotProducts = ref([
  { name: '四川红心猕猴桃', sales: 156, revenue: 4664.4, stock: 8 },
  { name: '农家土鸡蛋', sales: 89, revenue: 4005.0, stock: 120 },
  { name: '安岳柠檬', sales: 230, revenue: 4577.0, stock: 500 },
  { name: '通江银耳', sales: 67, revenue: 5896.0, stock: 45 },
  { name: '蒲江丑柑', sales: 45, revenue: 1341.0, stock: 200 }
]);

const handleTodo = (item: any) => {
  if (item.route) {
    router.push(item.route);
  }
};
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
  
  .mock-chart {
    display: flex;
    align-items: flex-end;
    gap: 30px;
    height: 100%;
    
    .bar-group {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .bar {
        width: 30px;
        background: #409eff;
        border-radius: 4px 4px 0 0;
        position: relative;
        transition: height 0.5s ease;
        
        &:hover { opacity: 0.8; }
        
        .bar-value {
          position: absolute;
          top: -20px;
          left: 50%;
          transform: translateX(-50%);
          font-size: 12px;
          color: #606266;
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
</style>
