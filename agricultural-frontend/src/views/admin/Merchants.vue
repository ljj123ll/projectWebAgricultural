<template>
  <div class="merchants-page">
    <div class="page-header">
      <h2>商家审核</h2>
      <el-radio-group v-model="filterStatus" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="approved">已通过</el-radio-button>
        <el-radio-button label="rejected">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <el-card>
      <el-table :data="merchantList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="shopName" label="店铺名称" />
        <el-table-column prop="ownerName" label="经营者" />
        <el-table-column prop="phone" label="联系电话" />
        <el-table-column prop="applyTime" label="申请时间" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <template v-if="row.status === 'pending'">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 商家详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="商家详情" width="90%">
      <div class="merchant-detail">
        <h4>基本信息</h4>
        <p><strong>店铺名称：</strong>{{ currentMerchant.shopName }}</p>
        <p><strong>经营者：</strong>{{ currentMerchant.ownerName }}</p>
        <p><strong>联系电话：</strong>{{ currentMerchant.phone }}</p>
        <p><strong>店铺类型：</strong>{{ currentMerchant.type }}</p>
        
        <h4>资质信息</h4>
        <p><strong>身份证号：</strong>{{ currentMerchant.idCard }}</p>
        <div class="idcard-images">
          <div class="idcard-item">
            <span>身份证正面</span>
            <img :src="currentMerchant.idCardFront" />
          </div>
          <div class="idcard-item">
            <span>身份证反面</span>
            <img :src="currentMerchant.idCardBack" />
          </div>
        </div>
        <p><strong>营业执照：</strong></p>
        <img :src="currentMerchant.license" class="license-image" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const filterStatus = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(50)
const showDetailDialog = ref(false)
const currentMerchant = reactive<any>({})

const merchantList = ref([
  {
    id: 1,
    shopName: '绿色农场旗舰店',
    ownerName: '张三',
    phone: '138****8888',
    applyTime: '2024-03-08 10:30:00',
    status: 'pending',
    type: '个体工商户',
    idCard: '110101********1234',
    idCardFront: 'https://via.placeholder.com/300x200',
    idCardBack: 'https://via.placeholder.com/300x200',
    license: 'https://via.placeholder.com/300x400'
  },
  {
    id: 2,
    shopName: '山东苹果直销',
    ownerName: '李四',
    phone: '139****9999',
    applyTime: '2024-03-07 14:20:00',
    status: 'approved',
    type: '农业合作社',
    idCard: '370101********5678',
    idCardFront: 'https://via.placeholder.com/300x200',
    idCardBack: 'https://via.placeholder.com/300x200',
    license: 'https://via.placeholder.com/300x400'
  }
])

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return map[status] || status
}

const viewDetail = (row: any) => {
  Object.assign(currentMerchant, row)
  showDetailDialog.value = true
}

const approve = (row: any) => {
  row.status = 'approved'
  ElMessage.success('审核通过')
}

const reject = (row: any) => {
  row.status = 'rejected'
  ElMessage.success('已拒绝')
}
</script>

<style scoped lang="scss">
.merchants-page {
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

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.merchant-detail {
  h4 {
    margin: 20px 0 12px 0;
    padding-bottom: 8px;
    border-bottom: 1px solid #ebeef5;

    &:first-child {
      margin-top: 0;
    }
  }

  p {
    margin: 8px 0;
    color: #666;
  }

  .idcard-images {
    display: flex;
    gap: 20px;
    margin: 12px 0;

    .idcard-item {
      text-align: center;

      span {
        display: block;
        margin-bottom: 8px;
        color: #666;
      }

      img {
        width: 200px;
        height: 130px;
        object-fit: cover;
        border-radius: 4px;
      }
    }
  }

  .license-image {
    width: 300px;
    height: 400px;
    object-fit: cover;
    border-radius: 4px;
  }
}
</style>
