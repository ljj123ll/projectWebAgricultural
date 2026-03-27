<template>
  <div class="unsold-page">
    <div class="page-header">
      <div>
        <h2>滞销帮扶管理</h2>
        <p>这里同时展示管理员手动帮扶池和算法实时命中的商品。</p>
      </div>
      <div class="header-actions">
        <el-tag type="warning">WebSocket 自动刷新</el-tag>
        <el-button type="primary" @click="showAddDialog = true">添加手动帮扶商品</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :md="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ summary.totalCount }}</div>
          <div class="stat-label">总命中数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card class="stat-card accent-red">
          <div class="stat-value">{{ summary.manualCount }}</div>
          <div class="stat-label">手动加入</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card class="stat-card accent-orange">
          <div class="stat-value">{{ summary.algorithmCount }}</div>
          <div class="stat-label">算法命中</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card class="stat-card accent-gold">
          <div class="stat-value">{{ summary.mixedCount }}</div>
          <div class="stat-label">双重命中</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索商品、商家、产地"
          clearable
          @keyup.enter="loadData"
        />
        <el-select v-model="sourceType">
          <el-option label="全部来源" value="all" />
          <el-option label="管理员手动" value="manual" />
          <el-option label="算法推荐" value="algorithm" />
          <el-option label="双重命中" value="mixed" />
        </el-select>
        <el-select v-model="sortType">
          <el-option label="帮扶优先级" value="support" />
          <el-option label="最新上架优先" value="latest" />
          <el-option label="库存高优先" value="stock" />
          <el-option label="销量最低优先" value="sales" />
          <el-option label="价格从低到高" value="price_asc" />
          <el-option label="价格从高到低" value="price_desc" />
        </el-select>
        <el-button type="primary" @click="loadData">筛选</el-button>
      </div>

      <el-table :data="unsoldList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="resolveImage(row.productImg)" class="product-thumb" />
              <div class="product-info">
                <span class="name">{{ row.productName }}</span>
                <span class="merchant">{{ row.merchantName || '未命名商家' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源" min-width="170">
          <template #default="{ row }">
            <div class="source-tags">
              <el-tag v-if="row.manualIncluded" type="danger">手动</el-tag>
              <el-tag v-if="row.algorithmIncluded" type="warning">算法</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="unsalableScore" label="紧急度" width="100" />
        <el-table-column label="原因" min-width="300">
          <template #default="{ row }">
            <div class="reason-cell">{{ row.unsalableReason || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span class="sale-price">¥{{ Number(row.price || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column prop="salesVolume" label="销量" width="90" />
        <el-table-column prop="ageDays" label="上架天数" width="110" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.manualIncluded"
              link
              type="primary"
              @click="saveProduct(Number(row.id))"
            >
              加入手动帮扶
            </el-button>
            <el-button
              v-if="row.manualIncluded"
              link
              type="danger"
              @click="removeProduct(row)"
            >
              移出手动帮扶
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="showAddDialog" title="添加手动帮扶商品" width="500px">
      <el-form label-position="top">
        <el-form-item label="商品ID">
          <el-input v-model="addProductId" placeholder="请输入商品ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveProduct()">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUnsalableProducts, getUnsalableSummary, setUnsalable } from '@/apis/admin';
import { getFullImageUrl } from '@/utils/image';
import { ADMIN_REALTIME_EVENT, parseRealtimePayload } from '@/utils/realtime';
import type { UnsalableProduct, UnsalableSummary } from '@/types';

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const showAddDialog = ref(false);
const loading = ref(false);
const addProductId = ref('');
const keyword = ref('');
const sourceType = ref('all');
const sortType = ref('support');
const unsoldList = ref<UnsalableProduct[]>([]);
const summary = reactive<UnsalableSummary>({
  totalCount: 0,
  manualCount: 0,
  algorithmCount: 0,
  mixedCount: 0
});

let refreshTimer: number | undefined;

const loadData = async () => {
  loading.value = true;
  try {
    const [listRes, summaryRes] = await Promise.all([
      getUnsalableProducts({
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        keyword: keyword.value,
        sourceType: sourceType.value,
        sortBy: sortType.value
      }),
      getUnsalableSummary({ keyword: keyword.value })
    ]);
    unsoldList.value = listRes?.list || [];
    total.value = Number(listRes?.total || 0);
    Object.assign(summary, summaryRes || {});
  } catch (error) {
    ElMessage.error('加载滞销帮扶数据失败');
  } finally {
    loading.value = false;
  }
};

const handleRealtimeRefresh = (event: Event) => {
  const payload = parseRealtimePayload(event);
  if (payload.reason !== 'UNSALABLE_UPDATED') return;
  void loadData();
};

onMounted(() => {
  void loadData();
  window.addEventListener(ADMIN_REALTIME_EVENT, handleRealtimeRefresh);
  refreshTimer = window.setInterval(() => {
    void loadData();
  }, 30000);
});

onUnmounted(() => {
  window.removeEventListener(ADMIN_REALTIME_EVENT, handleRealtimeRefresh);
  if (refreshTimer) window.clearInterval(refreshTimer);
});

const removeProduct = (row: UnsalableProduct) => {
  ElMessageBox.confirm('确定要移出手动帮扶池吗？算法命中的商品仍可能继续保留。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await setUnsalable(Number(row.id), 0);
      ElMessage.success('已移出手动帮扶池');
      await loadData();
    } catch (e) {
      ElMessage.error('操作失败');
    }
  });
};

const saveProduct = async (productId?: number) => {
  const targetId = Number(productId || addProductId.value);
  if (!targetId) {
    ElMessage.warning('请输入商品ID');
    return;
  }
  try {
    await setUnsalable(targetId, 1);
    ElMessage.success('已加入手动帮扶池');
    showAddDialog.value = false;
    addProductId.value = '';
    await loadData();
  } catch (e) {
    ElMessage.error('操作失败');
  }
};

const resolveImage = (source?: string) => {
  const first = String(source || '')
    .split(',')
    .map(item => item.trim())
    .find(Boolean);
  return getFullImageUrl(first || '');
};
</script>

<style scoped lang="scss">
.unsold-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    margin: 0 0 6px;
    font-size: 22px;
  }

  p {
    margin: 0;
    color: #7c8798;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  border-radius: 16px;

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 8px;
  }

  .stat-label {
    color: #606266;
    font-size: 14px;
  }

  &.accent-red .stat-value {
    color: #f56c6c;
  }

  &.accent-orange .stat-value {
    color: #e6a23c;
  }

  &.accent-gold .stat-value {
    color: #c17811;
  }
}

.table-card {
  border-radius: 18px;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1.6fr) 160px 170px 120px;
  gap: 12px;
  margin-bottom: 16px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .product-thumb {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    object-fit: cover;
    background: #f5f7fa;
  }

  .product-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .name {
    font-weight: 600;
    color: #303133;
  }

  .merchant {
    color: #909399;
    font-size: 12px;
  }
}

.source-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reason-cell {
  color: #606266;
  line-height: 1.6;
}

.sale-price {
  color: #d97706;
  font-weight: 700;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
