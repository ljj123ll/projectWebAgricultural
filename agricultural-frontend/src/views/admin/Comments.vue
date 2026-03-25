<template>
  <div class="comments-page">
    <div class="page-header">
      <h2>评价管理</h2>
      <div class="header-actions">
        <el-select v-model="auditStatus" placeholder="审核状态" clearable style="width: 150px; margin-right: 10px" @change="loadComments">
          <el-option label="待审核" :value="0" />
          <el-option label="通过" :value="1" />
          <el-option label="拒绝" :value="2" />
        </el-select>
      </div>
    </div>

    <el-card>
      <el-table :data="commentList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品" min-width="150">
          <template #default="{ row }">
            商品ID {{ row.productId }}
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="评分" width="120">
          <template #default="{ row }">
            <el-rate :model-value="row.score" disabled show-score text-color="#ff9900" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="200">
          <template #default="{ row }">
            <div class="content-text">{{ row.content }}</div>
            <div v-if="parseImages(row).length" class="content-images">
              <el-image 
                v-for="(img, index) in parseImages(row)" 
                :key="index"
                :src="img"
                :preview-src-list="parseImages(row)"
                preview-teleported
                :z-index="6000"
                class="comment-img"
              />
            </div>
            <div v-if="parseVideos(row).length" class="content-videos">
              <video
                v-for="(video, index) in parseVideos(row)"
                :key="`video-${index}`"
                :src="video"
                class="comment-video"
                controls
                preload="metadata"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="160" />
        <el-table-column prop="auditStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.auditStatus)">
              {{ getStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="handleAudit(row, 1)">审核通过</el-button>
            <el-button link type="warning" @click="handleAudit(row, 2)">审核不通过</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadComments"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listComments, auditComment, deleteComment } from '@/apis/admin'
import { getFullImageUrl } from '@/utils/image';

const auditStatus = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const commentList = ref<any[]>([])

const parseMedia = (row: any) => {
  const mediaStr = row?.mediaUrls || row?.imgUrls || '';
  if (!mediaStr) return [];
  try {
    const parsed = JSON.parse(mediaStr);
    if (Array.isArray(parsed)) return parsed.map(item => getFullImageUrl(String(item || ''))).filter(Boolean);
  } catch (e) {
    // 非 JSON 数组时按逗号处理
  }
  return mediaStr.split(',').map((item: string) => getFullImageUrl(item.trim())).filter(Boolean);
}

const isVideoUrl = (url: string) => {
  const target = (url || '').toLowerCase();
  return target.endsWith('.mp4')
    || target.endsWith('.webm')
    || target.endsWith('.mov')
    || target.endsWith('.avi')
    || target.endsWith('.mkv')
    || target.includes('video');
}

const parseImages = (row: any) => parseMedia(row).filter((url: string) => !isVideoUrl(url))
const parseVideos = (row: any) => parseMedia(row).filter((url: string) => isVideoUrl(url))

const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

const loadComments = async () => {
  loading.value = true;
  try {
    const res = await listComments({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      auditStatus: auditStatus.value
    });
    if (res) {
      commentList.value = res.list || [];
      total.value = res.total || 0;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadComments();
})

const handleAudit = async (row: any, status: number) => {
  try {
    await auditComment(row.id, status);
    ElMessage.success(status === 1 ? '已通过' : '已拒绝');
    loadComments();
  } catch (error) {
    console.error(error);
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除这条评价吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteComment(row.id);
      ElMessage.success('删除成功');
      loadComments();
    } catch (error) {
      console.error(error);
    }
  }).catch(() => {})
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.content-text {
  margin-bottom: 8px;
  line-height: 1.5;
}

.content-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.content-videos {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.comment-img {
  width: 60px;
  height: 60px;
  border-radius: 4px;
}

.comment-video {
  width: 160px;
  height: 100px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  background: #000;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:global(.el-image-viewer__wrapper) {
  z-index: 6000 !important;
}
</style>
