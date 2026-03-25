<template>
  <div class="admin-dashboard">
    <div style="margin-bottom: 20px; display: flex; justify-content: flex-end;">
      <el-radio-group v-model="timeRange" @change="loadDashboard">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="week">近一周</el-radio-button>
        <el-radio-button label="month">近一月</el-radio-button>
      </el-radio-group>
    </div>
    <!-- 核心数据卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>总用户数</template>
          <div class="card-value">{{ formatNumber(stats.totalUserCount) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>总商家数</template>
          <div class="card-value">{{ formatNumber(stats.totalMerchantCount) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>今日交易额</template>
          <div class="card-value">¥ {{ formatMoney(stats.todayRevenue) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" @click="$router.push('/admin/merchants')" style="cursor: pointer;">
          <template #header>待审核</template>
          <div class="card-value" style="color: #F56C6C">{{ stats.pendingAuditCount }}</div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 商家销量排行 -->
      <el-col :span="12">
        <el-card>
          <template #header>商家销量排行 TOP10</template>
          <el-table :data="rankList" v-loading="loading">
             <el-table-column type="index" label="排名" width="80" align="center">
               <template #default="scope">
                 <div class="rank-badge" :class="{ top: scope.$index < 3 }">{{ scope.$index + 1 }}</div>
               </template>
             </el-table-column>
             <el-table-column prop="merchantName" label="商家名称" show-overflow-tooltip />
             <el-table-column prop="salesVolume" label="订单数" width="100" align="center" />
             <el-table-column label="销售额" width="120" align="right">
               <template #default="scope">¥{{ formatMoney(scope.row.revenue) }}</template>
             </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      
      <!-- 最近订单 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>最近订单</span>
              <el-button link type="primary" @click="$router.push('/admin/orders')">查看更多</el-button>
            </div>
          </template>
          <el-table :data="recentOrders" v-loading="loading" size="small">
            <el-table-column prop="orderNo" label="订单号" width="140" show-overflow-tooltip />
            <el-table-column prop="merchantName" label="商家" show-overflow-tooltip />
            <el-table-column label="金额" width="100" align="right">
              <template #default="scope">¥{{ formatMoney(scope.row.orderAmount) }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160">
              <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 系统公告 -->
    <el-row style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>系统公告</template>
          <el-timeline>
            <el-timeline-item
              v-for="notice in notices"
              :key="notice.id"
              :timestamp="formatDate(notice.createTime)"
              type="primary"
            >
              <h4>{{ notice.title }}</h4>
              <p>{{ notice.content }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getDashboard, getMerchantRank } from '@/apis/admin';

const loading = ref(false);
const timeRange = ref('all');
const stats = ref({
  totalUserCount: 0,
  totalMerchantCount: 0,
  totalOrderCount: 0,
  totalRevenue: 0,
  todayRevenue: 0,
  pendingAuditCount: 0
});
const rankList = ref([]);
const recentOrders = ref([]);
const notices = ref<any[]>([]);

const formatNumber = (num: number) => {
  if (!num) return '0';
  return num.toLocaleString();
};

const formatMoney = (amount: number) => {
  if (!amount) return '0.00';
  return Number(amount).toFixed(2);
};

const formatDate = (date: string) => {
  if (!date) return '-';
  return new Date(date).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const loadDashboard = async () => {
  loading.value = true;
  try {
    const [dashboardRes, rankRes] = await Promise.all([
      getDashboard(timeRange.value),
      getMerchantRank(10)
    ]);
    const res = dashboardRes || {};
    stats.value = {
      totalUserCount: res.totalUserCount || 0,
      totalMerchantCount: res.totalMerchantCount || 0,
      totalOrderCount: res.totalOrderCount || 0,
      totalRevenue: res.totalRevenue || 0,
      todayRevenue: res.todayRevenue || 0,
      pendingAuditCount: res.pendingAuditCount || 0
    };
    rankList.value = (rankRes && rankRes.length ? rankRes : (res.merchantRank || []));
    recentOrders.value = res.recentOrders || [];
    notices.value = res.notices || [];
  } catch (error) {
    console.error('Failed to load dashboard', error);
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadDashboard();
});
</script>

<style scoped>
.card-value {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  color: #303133;
}

.rank-badge {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #e0e0e0;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  margin: 0 auto;
}

.rank-badge.top {
  background: #F56C6C;
}

.admin-dashboard {
  padding: 0;
}
</style>
