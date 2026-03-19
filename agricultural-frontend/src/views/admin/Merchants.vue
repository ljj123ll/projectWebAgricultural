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
      <el-table :data="merchantList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="merchantName" label="商家名称" min-width="180" />
        <el-table-column prop="contactPerson" label="联系人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="证件" width="180">
          <template #default="{ row }">
            <el-image
              v-if="row.idCardFront"
              :src="getFullImageUrl(row.idCardFront)"
              :preview-src-list="[getFullImageUrl(row.idCardFront)]"
              :preview-teleported="true"
              style="width: 44px; height: 32px; margin-right: 8px;"
              fit="cover"
            />
            <el-image
              v-if="row.licenseImg"
              :src="getFullImageUrl(row.licenseImg)"
              :preview-src-list="[getFullImageUrl(row.licenseImg)]"
              :preview-teleported="true"
              style="width: 44px; height: 32px;"
              fit="cover"
            />
            <span v-if="!row.idCardFront && !row.licenseImg" style="color: #999;">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column prop="auditStatus" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.auditStatus)">
              {{ getStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">查看</el-button>
            <template v-if="row.auditStatus === 0">
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

    <!-- 商家详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="商家详情" width="90%">
      <div class="merchant-detail">
        <h4>基本信息</h4>
        <p><strong>商家名称：</strong>{{ currentMerchant.merchantName }}</p>
        <p><strong>联系人：</strong>{{ currentMerchant.contactPerson }}</p>
        <p><strong>联系电话：</strong>{{ currentMerchant.phone }}</p>
        <p><strong>审核状态：</strong>{{ getStatusText(currentMerchant.auditStatus) }}</p>
        <p v-if="currentMerchant.auditStatus === 2 && currentMerchant.rejectReason"><strong>驳回原因：</strong>{{ currentMerchant.rejectReason }}</p>

        <h4>店铺信息</h4>
        <p><strong>店铺名称：</strong>{{ currentMerchant.shopName }}</p>
        <p><strong>店铺类型：</strong>{{ currentMerchant.shopType }}</p>
        <p><strong>主营类目：</strong>
          <template v-if="currentMerchant.categories">
            <el-tag v-for="c in currentMerchant.categories.split(',')" :key="c" size="small" style="margin-right: 6px;">{{ c }}</el-tag>
          </template>
          <span v-else style="color: #999;">未填写</span>
        </p>
        <p><strong>店铺简介：</strong>{{ currentMerchant.shopIntro || '未填写' }}</p>
        <p><strong>店铺Logo：</strong></p>
        <el-image
          v-if="currentMerchant.qualificationImg"
          :src="getFullImageUrl(currentMerchant.qualificationImg)"
          :preview-src-list="[getFullImageUrl(currentMerchant.qualificationImg)]"
          :preview-teleported="true"
          style="width: 120px; height: 120px;"
          fit="cover"
        />
        <span v-else style="color: #999;">未上传</span>
        
        <h4>资质信息</h4>
        <p><strong>身份证号：</strong>{{ currentMerchant.idCard }}</p>
        <div class="idcard-images">
          <div class="idcard-item">
            <span>身份证正面</span>
            <el-image
              v-if="currentMerchant.idCardFront"
              :src="getFullImageUrl(currentMerchant.idCardFront)"
              :preview-src-list="[getFullImageUrl(currentMerchant.idCardFront)]"
              :preview-teleported="true"
              style="width: 200px; height: 130px;"
              fit="cover"
            />
            <span v-else style="color: #999;">未上传</span>
          </div>
          <div class="idcard-item">
            <span>身份证反面</span>
            <el-image
              v-if="currentMerchant.idCardBack"
              :src="getFullImageUrl(currentMerchant.idCardBack)"
              :preview-src-list="[getFullImageUrl(currentMerchant.idCardBack)]"
              :preview-teleported="true"
              style="width: 200px; height: 130px;"
              fit="cover"
            />
            <span v-else style="color: #999;">未上传</span>
          </div>
        </div>
        <p><strong>营业执照：</strong></p>
        <el-image
          v-if="currentMerchant.licenseImg"
          :src="getFullImageUrl(currentMerchant.licenseImg)"
          :preview-src-list="[getFullImageUrl(currentMerchant.licenseImg)]"
          :preview-teleported="true"
          class="license-image"
          fit="cover"
        />
        <span v-else style="color: #999;">未上传</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMerchantAudit, auditMerchant, getMerchantDetail } from '@/apis/admin'
import { getFullImageUrl } from '@/utils/image'

const filterStatus = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showDetailDialog = ref(false)
const currentMerchant = reactive<any>({})
const loading = ref(false)

const merchantList = ref<any[]>([])

const getStatusType = (auditStatus: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[auditStatus] || 'info'
}

const getStatusText = (auditStatus: number) => {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已通过',
    2: '已驳回'
  }
  return map[auditStatus] || '未知'
}



const auditStatusParam = () => {
  if (filterStatus.value === 'pending') return 0
  if (filterStatus.value === 'approved') return 1
  if (filterStatus.value === 'rejected') return 2
  return undefined
}

const loadList = async () => {
  try {
    loading.value = true
    const res = await listMerchantAudit({
      auditStatus: auditStatusParam(),
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    merchantList.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const viewDetail = async (row: any) => {
  const res = await getMerchantDetail(row.id)
  Object.assign(currentMerchant, res || {})
  showDetailDialog.value = true
}

const approve = async (row: any) => {
  await ElMessageBox.confirm('确认通过该商家入驻审核吗？', '审核确认', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await auditMerchant(row.id, { pass: true })
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
  await auditMerchant(row.id, { pass: false, rejectReason: result.value })
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
