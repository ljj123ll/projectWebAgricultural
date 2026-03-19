<template>
  <div class="products-page">
    <div class="page-header">
      <h2>商品审核</h2>
      <el-radio-group v-model="filterStatus" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="approved">已通过</el-radio-button>
        <el-radio-button label="rejected">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <el-card>
      <el-table :data="productList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image :src="getFullImageUrl(row.productImg)" style="width: 50px; height: 50px; border-radius: 4px;" fit="cover" />
              <span>{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="所属店铺" min-width="160" />
        <el-table-column prop="merchantName" label="所属商家" min-width="160" />
        <el-table-column prop="categoryId" label="分类ID" width="100" />
        <el-table-column prop="price" label="价格" width="120">
          <template #default="{ row }">
            ¥{{ row.price.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <template v-if="row.status === 0">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
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

    <el-dialog v-model="showDetailDialog" title="商品详情" width="90%">
      <div class="product-detail">
        <p><strong>商品名称：</strong>{{ currentProduct.productName }}</p>
        <p><strong>所属店铺：</strong>{{ currentProduct.shopName }}</p>
        <p><strong>所属商家：</strong>{{ currentProduct.merchantName }}</p>
        <p><strong>价格：</strong>¥{{ (currentProduct.price || 0).toFixed(2) }}</p>
        <p><strong>库存：</strong>{{ currentProduct.stock }}</p>
        <p><strong>状态：</strong>{{ getStatusText(currentProduct.status) }}</p>
        <p v-if="currentProduct.status === 3 && currentProduct.rejectReason"><strong>驳回原因：</strong>{{ currentProduct.rejectReason }}</p>
        <p><strong>商品图片：</strong></p>
        <el-image
          v-if="currentProduct.productImg"
          :src="getFullImageUrl(currentProduct.productImg)"
          :preview-src-list="[getFullImageUrl(currentProduct.productImg)]"
          style="width: 180px; height: 180px;"
          fit="cover"
        />
        <span v-else style="color:#999;">未上传</span>
        <p style="margin-top: 12px;"><strong>商品描述：</strong></p>
        <div style="white-space: pre-wrap; color: #666;">{{ currentProduct.productDesc || '未填写' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProductAudit, auditProduct } from '@/apis/admin'

const filterStatus = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const showDetailDialog = ref(false)
const currentProduct = ref<any>({})

const productList = ref<any[]>([])

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    3: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已通过',
    2: '已下架',
    3: '已驳回'
  }
  return map[status] || '未知'
}

const viewDetail = (row: any) => {
  currentProduct.value = row
  showDetailDialog.value = true
}

const getFullImageUrl = (url: string) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/api')) return url
  return `/api${url}`
}

const statusParam = () => {
  if (filterStatus.value === 'pending') return 0
  if (filterStatus.value === 'approved') return 1
  if (filterStatus.value === 'rejected') return 3
  return undefined
}

const loadList = async () => {
  try {
    loading.value = true
    const res = await listProductAudit({
      status: statusParam(),
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    productList.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const approve = async (row: any) => {
  await ElMessageBox.confirm('确认通过该商品审核吗？', '审核确认', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await auditProduct(row.id, { pass: true })
  ElMessage.success('已通过')
  await loadList()
}

const reject = async (row: any) => {
  const result = (await ElMessageBox.prompt('请输入驳回原因', '驳回审核', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValidator: (val: string) => !!val?.trim(),
    inputErrorMessage: '驳回原因不能为空'
  })) as { value: string }
  await auditProduct(row.id, { pass: false, rejectReason: result.value })
  ElMessage.success('已驳回')
  await loadList()
}

watch([filterStatus, currentPage, pageSize], () => {
  loadList()
})

onMounted(() => {
  loadList()
})
</script>

<style scoped lang="scss">
.products-page {
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

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .product-thumb {
    width: 50px;
    height: 50px;
    border-radius: 4px;
    object-fit: cover;
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
