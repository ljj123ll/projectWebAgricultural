<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>总用户数</template>
          <div class="card-value">1,024</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>总商家数</template>
          <div class="card-value">120</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>今日交易额</template>
          <div class="card-value">¥ 45,678.00</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>待审核</template>
          <div class="card-value" style="color: #F56C6C">8</div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>商家销量排行</template>
          <el-table :data="rankList">
             <el-table-column type="index" label="排名" width="80" />
             <el-table-column prop="name" label="商家名称" />
             <el-table-column prop="sales" label="销量" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>系统公告</template>
          <p>系统运行正常...</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const rankList = ref([
    { name: '张三农场', sales: 1200 },
    { name: '李四果园', sales: 980 },
    { name: '王五粮油', sales: 850 }
]);
</script>

<style scoped>
.card-value {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
}
</style>
