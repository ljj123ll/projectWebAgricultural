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
              <el-image :src="getCoverImage(row.productImg)" class="list-cover" fit="cover" />
              <div class="product-meta">
                <span class="product-name">{{ row.productName }}</span>
                <span v-if="getImageList(row.productImg).length > 1" class="image-count">
                  主图{{ getImageList(row.productImg).length }}张
                </span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="所属店铺" min-width="160" />
        <el-table-column prop="merchantName" label="所属商家" min-width="160" />
        <el-table-column label="商品品类" min-width="120">
          <template #default="{ row }">
            {{ getCategoryText(row) }}
          </template>
        </el-table-column>
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

    <el-dialog v-model="showDetailDialog" title="商品详情" width="980px" class="product-detail-dialog">
      <div class="product-detail">
        <div class="detail-top">
          <div class="detail-title">{{ currentProduct.productName }}</div>
          <el-tag :type="getStatusType(currentProduct.status)">
            {{ getStatusText(currentProduct.status) }}
          </el-tag>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">所属店铺</span>
            <span class="value">{{ currentProduct.shopName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">所属商家</span>
            <span class="value">{{ currentProduct.merchantName || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">价格</span>
            <span class="value price">¥{{ (currentProduct.price || 0).toFixed(2) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">库存</span>
            <span class="value">{{ currentProduct.stock ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">商品品类</span>
            <span class="value">{{ getCategoryText(currentProduct) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">提交时间</span>
            <span class="value">{{ currentProduct.createTime || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">产地（省/市/区）</span>
            <span class="value">{{ currentProduct.originPlace || '请选择省/市/区' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">详细地址</span>
            <span class="value">{{ currentProduct.originPlaceDetail || '请输入详细产地' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">种植周期</span>
            <span class="value">{{ currentProduct.plantingCycle || '如：3月播种，9月采摘；或 孵化期30天，出栏180天' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">施肥类型</span>
            <span class="value">{{ currentProduct.fertilizerType || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">保存方式</span>
            <span class="value">{{ currentProduct.storageMethod || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="label">运输方式</span>
            <span class="value">{{ currentProduct.transportMethod || '-' }}</span>
          </div>
        </div>

        <div v-if="currentProduct.status === 3 && currentProduct.rejectReason" class="reject-box">
          <span class="label">驳回原因</span>
          <span class="value">{{ currentProduct.rejectReason }}</span>
        </div>

        <div class="section">
          <div class="section-title">商品主图（{{ mainImageList.length }}）</div>
          <div v-if="mainImageList.length" class="image-grid">
            <el-image
              v-for="(img, idx) in mainImageList"
              :key="`${img}-${idx}`"
              :src="img"
              :preview-src-list="mainImageList"
              :initial-index="idx"
              class="detail-image"
              fit="cover"
              preview-teleported
            />
          </div>
          <div v-else class="empty-text">未上传主图</div>
        </div>

        <div class="section">
          <div class="section-title">详情图片（{{ detailImageList.length }}）</div>
          <div v-if="detailImageList.length" class="image-grid">
            <el-image
              v-for="(img, idx) in detailImageList"
              :key="`${img}-${idx}`"
              :src="img"
              :preview-src-list="detailImageList"
              :initial-index="idx"
              class="detail-image"
              fit="cover"
              preview-teleported
            />
          </div>
          <div v-else class="empty-text">未上传详情图</div>
        </div>

        <div class="section">
          <div class="section-title">商品描述</div>
          <div class="desc-box">{{ currentProduct.productDesc || '未填写' }}</div>
        </div>

        <div class="section">
          <div class="section-title">溯源二维码预览</div>
          <div class="trace-qrcode">
            <el-image
              v-if="traceQrcodeUrl"
              :src="traceQrcodeUrl"
              :preview-src-list="[traceQrcodeUrl]"
              class="qrcode-image"
              fit="cover"
              preview-teleported
            />
            <div v-else class="qrcode-empty">填写信息后自动生成</div>
            <div class="qrcode-tip">
              包含产地、生产周期、生产方式等全链路信息，消费者扫码可见。
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
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

const getCategoryText = (product: any) => {
  const map: Record<number, string> = {
    1: '生鲜果蔬',
    2: '粮油副食',
    3: '干货特产',
    4: '畜禽肉蛋'
  }
  if (product?.categoryId && map[product.categoryId]) return map[product.categoryId]
  if (product?.categoryName) return product.categoryName
  if (product?.categoryId) return `分类${product.categoryId}`
  return '-'
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

const getImageList = (value?: string) => {
  if (!value) return []
  return value
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .map(getFullImageUrl)
}

const getCoverImage = (value?: string) => getImageList(value)[0] || ''

const mainImageList = computed(() => getImageList(currentProduct.value?.productImg))
const detailImageList = computed(() => getImageList(currentProduct.value?.productDetailImg))
const traceQrcodeUrl = computed(() => {
  const source = currentProduct.value?.qrCodeUrl
  return source ? getFullImageUrl(source) : ''
})

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
}

.list-cover {
  width: 50px;
  height: 50px;
  border-radius: 6px;
}

.product-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  color: #1f2937;
}

.image-count {
  color: #6b7280;
  font-size: 12px;
}

.product-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 14px 16px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  background: #ffffff;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: #64748b;
}

.value {
  color: #1f2937;
  font-size: 14px;
}

.value.price {
  color: #16a34a;
  font-weight: 600;
}

.reject-box {
  border: 1px solid #fecaca;
  background: #fff1f2;
  border-radius: 10px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.section {
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 14px;
  background: #ffffff;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.detail-image {
  width: 100%;
  height: 120px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.empty-text {
  color: #9ca3af;
  font-size: 13px;
}

.desc-box {
  white-space: pre-wrap;
  color: #4b5563;
  line-height: 1.7;
  min-height: 72px;
}

.trace-qrcode {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.qrcode-image {
  width: 160px;
  height: 160px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.qrcode-empty {
  width: 160px;
  height: 160px;
  border-radius: 10px;
  border: 1px dashed #cbd5e1;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  font-size: 13px;
}

.qrcode-tip {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
