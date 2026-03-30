<template>
  <div class="product-add-page">
    <div class="page-header">
      <div class="header-left">
        <el-button link :disabled="submitting || pageLoading || navigationPending" @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div>
          <h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
          <p>完善商品基础信息、批次档案与分类特色溯源字段，系统将生成独立溯源码。</p>
        </div>
      </div>
      <div class="header-right">
        <el-button :disabled="submitting || pageLoading || navigationPending" @click="handleBack">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveProduct">
          {{ isEdit ? '保存并刷新溯源码' : '发布商品' }}
        </el-button>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="productForm"
      :rules="rules"
      label-position="top"
      class="product-form"
    >
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag size="small" type="info">必填</el-tag>
          </div>
        </template>
        <el-row :gutter="24">
          <el-col :span="16">
            <el-form-item label="商品名称" prop="productName">
              <el-input
                v-model="productForm.productName"
                placeholder="请输入商品名称，如：四川红心猕猴桃礼盒"
                maxlength="60"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品品类" prop="categoryId">
              <el-select
                v-model="productForm.categoryId"
                placeholder="请选择品类"
                style="width: 100%"
                @change="handleCategoryChange"
              >
                <el-option
                  v-for="item in PRODUCT_CATEGORY_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品主图（可上传多张）" prop="productImg">
          <el-upload
            :action="uploadAction"
            name="file"
            list-type="picture-card"
            :file-list="productImgList"
            :on-success="handleProductImgSuccess"
            :on-remove="handleProductImgRemove"
            :before-upload="beforeUpload"
            class="upload-demo"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>价格库存</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="销售价格" prop="price">
              <el-input-number
                v-model="productForm.price"
                :min="0"
                :precision="2"
                :step="0.1"
                style="width: 100%"
                controls-position="right"
              >
                <template #prefix>¥</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存数量" prop="stock">
              <el-input-number
                v-model="productForm.stock"
                :min="0"
                :step="1"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存预警值">
              <el-input-number
                v-model="productForm.stockWarning"
                :min="0"
                style="width: 100%"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="form-card trace-card">
        <template #header>
          <div class="card-header">
            <span><el-icon class="icon-success"><CircleCheck /></el-icon> 基础溯源档案</span>
            <el-tooltip content="系统将根据当前批次信息生成一物一码溯源码" placement="top">
              <el-icon class="help-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </template>

        <el-alert
          title="二维码将跳转到独立的溯源档案页，建议按真实批次填写，便于论文演示“全流程溯源、一物一码”"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="产地（省/市/区）" prop="originPlace">
              <el-cascader
                v-model="selectedRegion"
                :options="regionOptions"
                placeholder="请选择省/市/区"
                style="width: 100%"
                @change="handleRegionChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细产地" prop="originPlaceDetail">
              <el-input v-model="productForm.originPlaceDetail" placeholder="填写乡镇、村落、基地或养殖场地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="批次编号" prop="batchNo">
              <el-input v-model="productForm.batchNo" placeholder="如：20260330-KT-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="cycleLabel" prop="plantingCycle">
              <el-input
                v-model="productForm.plantingCycle"
                :placeholder="`填写${cycleLabel}，如：育苗 35 天 / 散养 180 天 / 加工熟化 15 天`"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item :label="productionDateLabel" prop="productionDate">
              <el-date-picker
                v-model="productForm.productionDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="harvestDateLabel" prop="harvestDate">
              <el-date-picker
                v-model="productForm.harvestDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="包装入库日期" prop="packagingDate">
              <el-date-picker
                v-model="productForm.packagingDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="生产方式" prop="fertilizerType">
              <el-select v-model="productForm.fertilizerType" placeholder="请选择生产方式" style="width: 100%">
                <el-option label="有机生产" value="有机生产" />
                <el-option label="绿色生产" value="绿色生产" />
                <el-option label="无公害" value="无公害" />
                <el-option label="传统农耕" value="传统农耕" />
                <el-option label="现代化养殖" value="现代化养殖" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保存方式" prop="storageMethod">
              <el-select v-model="productForm.storageMethod" placeholder="请选择保存方式" style="width: 100%">
                <el-option label="常温保存" value="常温保存" />
                <el-option label="冷藏保存" value="冷藏保存" />
                <el-option label="通风干燥" value="通风干燥" />
                <el-option label="冷冻保存" value="冷冻保存" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运输方式" prop="transportMethod">
              <el-select v-model="productForm.transportMethod" placeholder="请选择运输方式" style="width: 100%">
                <el-option label="普通物流" value="普通物流" />
                <el-option label="冷链运输" value="冷链运输" />
                <el-option label="加急空运" value="加急空运" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="检测/检疫说明" prop="inspectionReport">
          <el-input
            v-model="productForm.inspectionReport"
            type="textarea"
            :rows="3"
            placeholder="填写检测报告、检疫结论、追溯凭证编号等公开信息"
          />
        </el-form-item>

        <div class="trace-section-title">
          <div>
            <h3>分类特色溯源字段</h3>
            <p>{{ categoryHint }}</p>
          </div>
          <el-tag type="success" effect="plain">{{ currentCategoryName }}</el-tag>
        </div>

        <el-row v-if="traceFieldDefinitions.length" :gutter="24">
          <el-col
            v-for="field in traceFieldDefinitions"
            :key="field.key"
            :span="field.type === 'textarea' ? 24 : 12"
          >
            <el-form-item :label="field.label">
              <el-input
                v-if="field.type === 'input'"
                v-model="productForm.traceExtra[field.key]"
                :placeholder="field.placeholder"
              />
              <el-input
                v-else-if="field.type === 'textarea'"
                v-model="productForm.traceExtra[field.key]"
                type="textarea"
                :rows="3"
                :placeholder="field.placeholder"
              />
              <el-select
                v-else-if="field.type === 'select'"
                v-model="productForm.traceExtra[field.key]"
                :placeholder="field.placeholder"
                style="width: 100%"
              >
                <el-option
                  v-for="option in field.options || []"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <el-date-picker
                v-else
                v-model="productForm.traceExtra[field.key]"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="qr-preview-section">
          <div class="qr-box">
            <img :src="qrCodeUrl" v-if="qrCodeUrl" />
            <div class="placeholder" v-else>
              <el-icon size="30"><Grid /></el-icon>
              <span>保存后生成</span>
            </div>
          </div>
          <div class="qr-info">
            <h4>溯源二维码预览</h4>
            <p>扫码后将进入独立溯源档案页，显示基础档案与{{ currentCategoryName }}专属信息。</p>
            <p class="trace-code" v-if="traceCode">当前溯源码：{{ traceCode }}</p>
            <el-button type="primary" link :disabled="!qrCodeUrl" @click="downloadQrCode">下载二维码图片</el-button>
          </div>
        </div>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>详细介绍</span>
        </template>
        <el-form-item prop="productDesc" label="文字描述">
          <el-input
            v-model="productForm.productDesc"
            type="textarea"
            :rows="6"
            placeholder="请详细描述商品特色、口感、营养价值、包装规格等"
          />
        </el-form-item>
        <el-form-item label="详情图片（可上传多张）">
          <el-upload
            :action="uploadAction"
            name="file"
            list-type="picture-card"
            :file-list="productDetailImgList"
            :on-success="handleProductDetailImgSuccess"
            :on-remove="handleProductDetailImgRemove"
            :before-upload="beforeUpload"
            class="upload-demo"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CircleCheck, Grid, Plus, QuestionFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadProps } from 'element-plus'
import { regionData, codeToText } from 'element-china-area-data'
import { createProduct, generateProductQrcode, getProductDetail, updateProduct } from '@/apis/merchant'
import { PRODUCT_CATEGORY_OPTIONS, getProductCategoryName } from '@/constants/category'
import {
  getHarvestDateLabel,
  getProductionDateLabel,
  getTraceCycleLabel,
  getTraceFieldDefinitions
} from '@/constants/trace'
import { getFullImageUrl } from '@/utils/image'

/**
 * 商家商品新增/编辑页。
 * 这是答辩里展示“商品发布 + 分类溯源字段 + 二维码生成”的核心页面。
 */

const uploadAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/common/upload/product`
const route = useRoute()
const router = useRouter()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const pageLoading = ref(false)
const navigationPending = ref(false)
const isEdit = computed(() => !!route.params.id)
let loadRequestNo = 0
let navigationTimer: ReturnType<typeof setTimeout> | null = null
let productPageAlive = true

const selectedRegion = ref<string[]>([])
const regionOptions = regionData
const productImgList = ref<any[]>([])
const productDetailImgList = ref<any[]>([])
const qrCodeUrl = ref('')
const traceCode = ref('')

const createDefaultForm = () => ({
  productName: '',
  categoryId: '' as number | '',
  price: 0,
  stock: 0,
  stockWarning: 10,
  productImg: '',
  productDetailImg: '',
  productDesc: '',
  originPlace: '',
  batchNo: '',
  productionDate: '',
  harvestDate: '',
  packagingDate: '',
  inspectionReport: '',
  plantingCycle: '',
  originPlaceDetail: '',
  fertilizerType: '',
  storageMethod: '',
  transportMethod: '普通物流',
  traceExtra: {} as Record<string, string>
})
const productForm = reactive(createDefaultForm())

const traceFieldDefinitions = computed(() => getTraceFieldDefinitions(productForm.categoryId))
const cycleLabel = computed(() => getTraceCycleLabel(productForm.categoryId))
const productionDateLabel = computed(() => getProductionDateLabel(productForm.categoryId))
const harvestDateLabel = computed(() => getHarvestDateLabel(productForm.categoryId))
const currentCategoryName = computed(() => getProductCategoryName(productForm.categoryId) || '当前品类')
const categoryHint = computed(() => {
  if (!traceFieldDefinitions.value.length) {
    return '请选择商品品类后，系统会自动展示该品类的特色溯源字段。'
  }
  return `已切换为${currentCategoryName.value}专属字段，支持按品类维护差异化溯源数据。`
})

const rules: FormRules = {
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择品类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  productImg: [{ required: true, message: '请至少上传一张主图', trigger: 'change' }],
  originPlace: [{ required: true, message: '请选择产地', trigger: 'change' }],
  originPlaceDetail: [{ required: true, message: '请输入详细产地', trigger: 'blur' }],
  batchNo: [{ required: true, message: '请输入批次编号', trigger: 'blur' }],
  plantingCycle: [{ required: true, message: `请输入${cycleLabel.value}`, trigger: 'blur' }]
}

const normalizeTraceExtra = (categoryId?: number | string | null, source?: Record<string, string> | null) => {
  const result: Record<string, string> = {}
  getTraceFieldDefinitions(categoryId).forEach(field => {
    result[field.key] = source?.[field.key] ?? ''
  })
  return result
}

const isLatestLoadRequest = (requestNo: number, productId?: number) => {
  const currentId = Number(route.params.id || 0)
  return productPageAlive && requestNo === loadRequestNo && (productId === undefined || currentId === Number(productId))
}

// 在新增态和编辑态切换时，先把页面状态彻底重置，避免旧商品数据残留。
const resetPageState = () => {
  Object.assign(productForm, createDefaultForm())
  productForm.traceExtra = normalizeTraceExtra(productForm.categoryId)
  selectedRegion.value = []
  productImgList.value = []
  productDetailImgList.value = []
  qrCodeUrl.value = ''
  traceCode.value = ''
  formRef.value?.clearValidate()
}

const findRegionPath = (options: any[], labels: string[], depth = 0): string[] => {
  if (!labels[depth]) return []
  for (const option of options || []) {
    if (option.label !== labels[depth]) continue
    if (depth === labels.length - 1) {
      return [option.value]
    }
    const childPath = findRegionPath(option.children || [], labels, depth + 1)
    if (childPath.length) {
      return [option.value, ...childPath]
    }
  }
  return []
}

const syncSelectedRegionByOriginPlace = (originPlace?: string) => {
  if (!originPlace) {
    selectedRegion.value = []
    return
  }
  const labels = originPlace
    .split('/')
    .map(item => item.trim())
    .filter(Boolean)
    .slice(0, 3)
  selectedRegion.value = labels.length ? findRegionPath(regionOptions, labels) : []
}

const handleCategoryChange = (value: number | string) => {
  productForm.traceExtra = normalizeTraceExtra(value, productForm.traceExtra)
}

const handleRegionChange = (value: string[]) => {
  if (!value?.length) {
    productForm.originPlace = ''
    return
  }
  productForm.originPlace = value.map(code => codeToText[code]).join('/')
}

const releaseNavigationLock = () => {
  if (navigationTimer) {
    clearTimeout(navigationTimer)
  }
  navigationTimer = setTimeout(() => {
    navigationPending.value = false
  }, 300)
}

const handleBack = () => {
  if (submitting.value || pageLoading.value || navigationPending.value) return
  navigationPending.value = true
  try {
    router.back()
  } finally {
    releaseNavigationLock()
  }
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('图片必须是 JPG/PNG 格式')
    return false
  }
  if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

const updateProductImgStr = () => {
  productForm.productImg = productImgList.value
    .map(file => (file.response ? file.response.data : String(file.url || '').replace(getFullImageUrl(''), '')))
    .join(',')
}

const updateProductDetailImgStr = () => {
  productForm.productDetailImg = productDetailImgList.value
    .map(file => (file.response ? file.response.data : String(file.url || '').replace(getFullImageUrl(''), '')))
    .join(',')
}

const handleProductImgSuccess = (response: any, file: any, fileList: any[]) => {
  if (response.code === 200) {
    productImgList.value = fileList
    updateProductImgStr()
    ElMessage.success('主图上传成功')
    return
  }
  ElMessage.error(response.msg || '主图上传失败')
  productImgList.value = fileList.filter(item => item.uid !== file.uid)
}

const handleProductImgRemove = (_file: any, fileList: any[]) => {
  productImgList.value = fileList
  updateProductImgStr()
}

const handleProductDetailImgSuccess = (response: any, file: any, fileList: any[]) => {
  if (response.code === 200) {
    productDetailImgList.value = fileList
    updateProductDetailImgStr()
    ElMessage.success('详情图上传成功')
    return
  }
  ElMessage.error(response.msg || '详情图上传失败')
  productDetailImgList.value = fileList.filter(item => item.uid !== file.uid)
}

const handleProductDetailImgRemove = (_file: any, fileList: any[]) => {
  productDetailImgList.value = fileList
  updateProductDetailImgStr()
}

// 生成或刷新当前商品的溯源二维码，对应商家端“二维码预览”和下载。
const refreshQrCode = async (id: number, requestNo?: number) => {
  try {
    const nextQrCode = await generateProductQrcode(id)
    if (requestNo && !isLatestLoadRequest(requestNo, id)) {
      return
    }
    qrCodeUrl.value = nextQrCode
    if (!qrCodeUrl.value) {
      ElMessage.warning('二维码暂未生成，请稍后重试')
    }
  } catch (error) {
    console.error('生成溯源码失败', error)
    if (requestNo && !isLatestLoadRequest(requestNo, id)) {
      return
    }
    qrCodeUrl.value = ''
    ElMessage.warning('商品已保存，但二维码生成失败，请稍后重试')
  }
}

const downloadQrCode = () => {
  if (!qrCodeUrl.value) return
  const link = document.createElement('a')
  link.href = qrCodeUrl.value
  link.download = 'trace_qrcode.png'
  link.click()
  ElMessage.success('二维码已下载')
}

const toUploadList = (raw: string, prefix: string) => {
  if (!raw) return []
  return raw
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
    .map((url, index) => ({
      name: `${prefix}_${index}`,
      url: getFullImageUrl(url),
      uid: `${prefix}_${index}`
    }))
}

// 把后端商品详情回填到表单，便于编辑商品时展示已有的图文和溯源档案。
const applyFormData = (data: any) => {
  Object.assign(productForm, {
    productName: data.productName ?? '',
    categoryId: data.categoryId ?? '',
    price: Number(data.price ?? 0),
    stock: Number(data.stock ?? 0),
    stockWarning: Number(data.stockWarning ?? 10),
    productImg: data.productImg ?? '',
    productDetailImg: data.productDetailImg ?? '',
    productDesc: data.productDesc ?? '',
    originPlace: data.originPlace ?? '',
    batchNo: data.batchNo ?? '',
    productionDate: data.productionDate ?? '',
    harvestDate: data.harvestDate ?? '',
    packagingDate: data.packagingDate ?? '',
    inspectionReport: data.inspectionReport ?? '',
    plantingCycle: data.plantingCycle ?? '',
    originPlaceDetail: data.originPlaceDetail ?? '',
    fertilizerType: data.fertilizerType ?? '',
    storageMethod: data.storageMethod ?? '',
    transportMethod: data.transportMethod || '普通物流',
    traceExtra: normalizeTraceExtra(data.categoryId, data.traceExtra)
  })
  syncSelectedRegionByOriginPlace(data.originPlace)
  traceCode.value = data.traceCode ?? ''
  qrCodeUrl.value = data.qrCodeUrl ?? ''
  productImgList.value = toUploadList(productForm.productImg, 'img')
  productDetailImgList.value = toUploadList(productForm.productDetailImg, 'detail')
}

const loadEditProduct = async (id: number, requestNo?: number) => {
  const detail = await getProductDetail(id)
  if (detail && (!requestNo || isLatestLoadRequest(requestNo, id))) {
    applyFormData(detail)
  }
}

const buildPayload = () => ({
  ...productForm,
  categoryId: Number(productForm.categoryId),
  traceExtra: normalizeTraceExtra(productForm.categoryId, productForm.traceExtra)
})

const validateDateOrder = () => {
  if (productForm.productionDate && productForm.harvestDate && productForm.harvestDate < productForm.productionDate) {
    ElMessage.warning(`${harvestDateLabel.value}不能早于${productionDateLabel.value}`)
    return false
  }
  if (productForm.harvestDate && productForm.packagingDate && productForm.packagingDate < productForm.harvestDate) {
    ElMessage.warning(`包装入库日期不能早于${harvestDateLabel.value}`)
    return false
  }
  if (productForm.productionDate && productForm.packagingDate && productForm.packagingDate < productForm.productionDate) {
    ElMessage.warning(`包装入库日期不能早于${productionDateLabel.value}`)
    return false
  }
  return true
}

// 商品保存主入口：新增与编辑共用，并在成功后刷新二维码和档案数据。
const saveProduct = async () => {
  if (!formRef.value || submitting.value || pageLoading.value) return

  try {
    await formRef.value.validate()
    if (!validateDateOrder()) {
      return
    }
    submitting.value = true
    const payload = buildPayload()
    if (isEdit.value) {
      const id = Number(route.params.id)
      await updateProduct(id, payload)
      await refreshQrCode(id)
      await loadEditProduct(id)
      ElMessage.success('商品更新成功')
      return
    }

    const created = await createProduct(payload)
    const id = Number(created?.id || 0)
    if (id > 0) {
      await refreshQrCode(id)
      await loadEditProduct(id)
    }
    ElMessage.success('商品发布成功')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close' && error) {
      console.error('保存商品失败', error)
    }
  } finally {
    submitting.value = false
  }
}

// 页面初始化入口：根据当前路由判断是新增商品还是编辑商品。
const initializePage = async () => {
  const requestNo = ++loadRequestNo
  resetPageState()

  if (!isEdit.value) {
    pageLoading.value = false
    return
  }

  const id = Number(route.params.id)
  if (!id) {
    pageLoading.value = false
    return
  }

  pageLoading.value = true
  try {
    await loadEditProduct(id, requestNo)
    if (!isLatestLoadRequest(requestNo, id)) {
      return
    }
    await refreshQrCode(id, requestNo)
  } catch (error) {
    if (!isLatestLoadRequest(requestNo, id)) {
      return
    }
    console.error('加载商品详情失败', error)
    ElMessage.error('加载商品详情失败，请返回列表重试')
  } finally {
    if (productPageAlive && requestNo === loadRequestNo) {
      pageLoading.value = false
    }
  }
}

watch(
  () => `${String(route.name || '')}-${String(route.params.id || '')}`,
  () => {
    void initializePage()
  },
  { immediate: true }
)

onUnmounted(() => {
  productPageAlive = false
  if (navigationTimer) {
    clearTimeout(navigationTimer)
    navigationTimer = null
  }
})
</script>

<style scoped lang="scss">
.product-add-page {
  padding-bottom: 60px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 18px 22px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f5faef 100%);
  box-shadow: 0 10px 30px rgba(54, 88, 41, 0.08);

  .header-left {
    display: flex;
    align-items: flex-start;
    gap: 10px;

    h2 {
      margin: 0;
      font-size: 22px;
      color: #223548;
    }

    p {
      margin: 6px 0 0;
      color: #697b8e;
      font-size: 13px;
      line-height: 1.6;
    }
  }

  .header-right {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}

.product-form {
  max-width: 1080px;
  margin: 0 auto;
}

.form-card {
  margin-bottom: 20px;
  border-radius: 22px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 10px;

    .icon-success {
      color: #67c23a;
    }

    .help-icon {
      color: #909399;
      cursor: pointer;
    }
  }
}

.upload-demo {
  :deep(.el-upload--picture-card) {
    width: 110px;
    height: 110px;
    line-height: 110px;
    border-radius: 18px;
  }
}

.trace-card {
  border: 1px solid #e0efd4;

  :deep(.el-card__header) {
    background: linear-gradient(135deg, #f4fbe9 0%, #f8fcf2 100%);
  }
}

.trace-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 8px 0 18px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #f7faf4;

  h3 {
    margin: 0;
    color: #21364b;
    font-size: 18px;
  }

  p {
    margin: 6px 0 0;
    color: #738596;
    font-size: 13px;
  }
}

.qr-preview-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 20px;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f6f9f2 0%, #f7fbff 100%);

  .qr-box {
    width: 132px;
    height: 132px;
    border-radius: 18px;
    background: #fff;
    border: 1px solid #d9e5d0;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
    }

    .placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      color: #8b96a1;
      font-size: 12px;
    }
  }

  .qr-info {
    h4 {
      margin: 0 0 8px;
      color: #223548;
    }

    p {
      margin: 0 0 8px;
      color: #647688;
      line-height: 1.7;
      font-size: 13px;
    }

    .trace-code {
      color: #2f6f35;
      font-weight: 600;
    }
  }
}

@media (max-width: 768px) {
  .page-header,
  .trace-section-title,
  .qr-preview-section {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
