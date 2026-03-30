<template>
  <div class="trace-archive-page">
    <div class="trace-shell" v-loading="loading">
      <el-result
        v-if="loadError"
        icon="warning"
        title="溯源档案暂不可用"
        :sub-title="loadError"
        class="trace-error"
      >
        <template #extra>
          <el-button type="primary" @click="loadTraceArchive">重新加载</el-button>
          <el-button plain @click="router.push('/products')">返回商品列表</el-button>
        </template>
      </el-result>

      <section class="trace-hero" v-if="archive">
        <div class="hero-copy">
          <div class="hero-badges">
            <span class="badge">商品溯源档案</span>
            <span class="badge light">一物一码</span>
            <span class="badge light">扫码可验</span>
          </div>
          <h1>{{ archive.productName }}</h1>
          <p>{{ archive.productDesc || '该商品已建立完整溯源档案，信息由商家提交并随商品批次持续维护。' }}</p>
          <div class="hero-meta">
            <div>
              <span>溯源码</span>
              <strong>{{ archive.traceCode }}</strong>
              <el-button link type="primary" class="copy-btn" @click="copyTraceCode">复制</el-button>
            </div>
            <div>
              <span>批次编号</span>
              <strong>{{ archive.batchNo || '待补充' }}</strong>
            </div>
            <div>
              <span>所属店铺</span>
              <strong>{{ archive.shopName || '助农商家' }}</strong>
            </div>
          </div>
        </div>

        <div class="hero-preview">
          <img :src="coverImage" :alt="archive.productName" />
          <div class="preview-chip">
            <span>{{ archive.categoryName || '助农产品' }}</span>
            <strong>{{ archive.originPlace || '产地待完善' }}</strong>
          </div>
        </div>
      </section>

      <section class="trace-grid" v-if="archive">
        <el-card class="trace-card timeline-card">
          <template #header>
            <div class="card-header">
              <h2>关键时间轴</h2>
              <span>从建档到包装的关键节点</span>
            </div>
          </template>
          <el-timeline v-if="timelineItems.length">
            <el-timeline-item
              v-for="item in timelineItems"
              :key="item.label"
              :timestamp="item.value"
              type="success"
              hollow
            >
              {{ item.label }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂未录入时间轴信息" :image-size="70" />
        </el-card>

        <el-card class="trace-card base-card">
          <template #header>
            <div class="card-header">
              <h2>基础档案</h2>
              <span>批次、产地、流通与质检信息</span>
            </div>
          </template>
          <div class="field-grid">
            <div v-for="field in archive.baseFields || []" :key="field.key" class="field-item">
              <span class="label">{{ field.label }}</span>
              <strong class="value">{{ field.value }}</strong>
            </div>
          </div>
        </el-card>
      </section>

      <section class="trace-grid" v-if="archive">
        <el-card class="trace-card feature-card">
          <template #header>
            <div class="card-header">
              <h2>分类特色溯源</h2>
              <span>{{ archive.categoryName || '当前品类' }}专属档案字段</span>
            </div>
          </template>
          <div class="feature-list" v-if="archive.featureFields?.length">
            <div v-for="field in archive.featureFields" :key="field.key" class="feature-item">
              <span>{{ field.label }}</span>
              <p>{{ field.value }}</p>
            </div>
          </div>
          <el-empty v-else description="该批次暂未填写分类特色信息" :image-size="70" />
        </el-card>

        <el-card class="trace-card actions-card">
          <template #header>
            <div class="card-header">
              <h2>可信提示</h2>
              <span>扫码页展示的是当前批次公开信息</span>
            </div>
          </template>
          <ul class="trust-list">
            <li>本页信息由商家在商品发布或编辑时维护。</li>
            <li>二维码采用唯一溯源码，便于包装打印与回溯核验。</li>
            <li>若商品下架，已售出批次的档案仍可独立访问。</li>
            <li>建议在论文演示中结合手机扫码展示该页面。</li>
          </ul>
          <div class="action-row">
            <el-button type="primary" @click="goToProduct">查看商品详情</el-button>
            <el-button plain @click="router.push('/products')">返回商品列表</el-button>
          </div>
        </el-card>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTraceArchive } from '@/apis/product'
import type { TraceArchive } from '@/types'
import { getHarvestDateLabel, getProductionDateLabel } from '@/constants/trace'
import { getFullImageUrl } from '@/utils/image'

/**
 * 独立溯源档案页。
 * 二维码扫码后展示的就是这个页面，答辩时可直接用它说明“一物一码”和分类溯源。
 */

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const archive = ref<TraceArchive | null>(null)
const loadError = ref('')

const traceCode = computed(() => String(route.params.traceCode || '').trim())
const categoryId = computed(() => archive.value?.categoryId)

const coverImage = computed(() => {
  const raw = archive.value?.productImg || ''
  const first = raw.split(',').map(item => item.trim()).find(Boolean) || ''
  if (first) {
    return getFullImageUrl(first)
  }
  return "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="720" height="720" viewBox="0 0 720 720">
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#eef5e8"/>
          <stop offset="100%" stop-color="#f5f8fc"/>
        </linearGradient>
      </defs>
      <rect width="720" height="720" rx="36" fill="url(#g)"/>
      <circle cx="240" cy="245" r="72" fill="#cddfbf"/>
      <path d="M135 520c54-102 112-154 173-154 58 0 103 30 140 92 27-22 54-33 81-33 31 0 63 14 96 43v52H135z" fill="#b6cfa2"/>
      <text x="360" y="625" font-size="34" text-anchor="middle" fill="#53704d" font-family="Arial, sans-serif">助农商品溯源档案</text>
    </svg>
  `)
})

const timelineItems = computed(() => {
  if (!archive.value) return []
  return [
    { label: getProductionDateLabel(categoryId.value), value: archive.value.productionDate || '' },
    { label: getHarvestDateLabel(categoryId.value), value: archive.value.harvestDate || '' },
    { label: '包装入库', value: archive.value.packagingDate || '' }
  ].filter(item => item.value)
})

// 根据溯源码加载完整档案，包含基础档案和分类特色溯源字段。
const loadTraceArchive = async () => {
  if (!traceCode.value) {
    ElMessage.error('溯源码无效')
    router.replace('/products')
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getTraceArchive(traceCode.value)
    archive.value = res || null
    if (!archive.value) {
      loadError.value = '未查询到对应的溯源档案，请确认二维码是否已失效或该商品尚未补充档案。'
    }
  } catch (error) {
    console.error('加载溯源档案失败', error)
    archive.value = null
    loadError.value = '溯源档案不存在或暂不可用，请稍后重试。'
  } finally {
    loading.value = false
  }
}

// 从溯源页回跳到商品详情页，方便演示“扫码 -> 档案 -> 商品”的闭环。
const goToProduct = () => {
  if (!archive.value?.productId) return
  router.push(`/product/${archive.value.productId}`)
}

// 复制溯源码，便于答辩时展示“当前档案编码”。
const copyTraceCode = async () => {
  if (!archive.value?.traceCode) return
  try {
    await navigator.clipboard.writeText(archive.value.traceCode)
    ElMessage.success('溯源码已复制')
  } catch (error) {
    ElMessage.warning('当前浏览器暂不支持自动复制，请手动复制')
  }
}

onMounted(() => {
  loadTraceArchive()
})

watch(() => route.params.traceCode, () => {
  loadTraceArchive()
})
</script>

<style scoped lang="scss">
.trace-archive-page {
  min-height: 100vh;
  padding: 24px 16px 40px;
  background:
    radial-gradient(circle at top left, rgba(119, 171, 88, 0.16), transparent 26%),
    linear-gradient(180deg, #f5f8ef 0%, #f7fafc 48%, #f3f7fb 100%);
}

.trace-shell {
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.trace-error {
  padding: 40px 0 16px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
}

.trace-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 340px;
  gap: 20px;
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(223, 231, 238, 0.9);
  box-shadow: 0 24px 56px rgba(67, 84, 67, 0.1);
}

.hero-badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: #2f6f35;
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.badge.light {
  background: #eef5eb;
  color: #4a6b2f;
}

.hero-copy h1 {
  margin: 16px 0 0;
  font-size: 36px;
  color: #1f3348;
}

.hero-copy p {
  margin: 14px 0 0;
  font-size: 16px;
  line-height: 1.9;
  color: #5a6f82;
}

.hero-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.hero-meta div {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f7faf4;
}

.hero-meta span {
  display: block;
  color: #7a8d9f;
  font-size: 12px;
}

.hero-meta strong {
  display: block;
  margin-top: 8px;
  color: #203448;
  font-size: 16px;
  line-height: 1.5;
  word-break: break-word;
}

.copy-btn {
  margin-top: 8px;
  padding: 0;
}

.hero-preview {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(180deg, #f7f8fb 0%, #eef3f4 100%);
}

.hero-preview img {
  width: 100%;
  height: 100%;
  min-height: 300px;
  object-fit: cover;
}

.preview-chip {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
}

.preview-chip span {
  display: block;
  font-size: 12px;
  color: #7f8c97;
}

.preview-chip strong {
  display: block;
  margin-top: 6px;
  color: #21364b;
  font-size: 16px;
}

.trace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.trace-card {
  border-radius: 24px;
}

.card-header h2 {
  margin: 0;
  color: #21364b;
  font-size: 22px;
}

.card-header span {
  display: block;
  margin-top: 6px;
  color: #7d8998;
  font-size: 13px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field-item,
.feature-item {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid #ebf0f5;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
}

.field-item .label,
.feature-item span {
  display: block;
  font-size: 12px;
  color: #8594a4;
}

.field-item .value,
.feature-item p {
  display: block;
  margin-top: 8px;
  color: #203549;
  line-height: 1.8;
  font-size: 15px;
  font-weight: 600;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.feature-item p {
  margin: 8px 0 0;
  font-weight: 500;
}

.trust-list {
  margin: 0;
  padding-left: 18px;
  color: #546577;
  line-height: 1.9;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .trace-hero,
  .trace-grid,
  .hero-meta,
  .field-grid,
  .feature-list {
    grid-template-columns: 1fr;
  }
}
</style>
