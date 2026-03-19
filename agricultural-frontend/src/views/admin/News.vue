<template>
  <div class="news-page">
    <div class="page-header">
      <h2>资讯管理</h2>
      <div class="header-actions">
        <el-select v-model="categoryId" placeholder="全部分类" clearable style="width: 150px; margin-right: 10px" @change="loadNews">
          <el-option 
            v-for="cat in categoryList" 
            :key="cat.id" 
            :label="cat.categoryName" 
            :value="cat.id" 
          />
        </el-select>
        <el-select v-model="auditStatus" placeholder="审核状态" clearable style="width: 150px; margin-right: 10px" @change="loadNews">
          <el-option label="待审核" :value="0" />
          <el-option label="通过" :value="1" />
          <el-option label="拒绝" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleAdd">发布资讯</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="newsList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image 
              v-if="row.coverImg"
              style="width: 60px; height: 40px; border-radius: 4px"
              :src="getFullImageUrl(row.coverImg)" 
              fit="cover" 
              :preview-src-list="[getFullImageUrl(row.coverImg)]"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column prop="categoryId" label="分类" width="120">
          <template #default="{ row }">
            {{ getCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="100" />
        <el-table-column prop="auditStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.auditStatus)">
              {{ getStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            <template v-if="row.auditStatus === 0">
              <el-button link type="success" @click="handleAudit(row, 1)">通过</el-button>
              <el-button link type="warning" @click="handleAudit(row, 2)">拒绝</el-button>
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
          @current-change="loadNews"
        />
      </div>
    </el-card>

    <!-- 发布/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑资讯' : '发布资讯'"
      width="1000px"
      top="5vh"
    >
      <el-form 
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="80px"
      >
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option 
                  v-for="cat in categoryList" 
                  :key="cat.id" 
                  :label="cat.categoryName" 
                  :value="cat.id" 
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="封面图" prop="coverImg">
          <el-upload
            class="cover-uploader"
            :action="uploadAction"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
          >
            <img v-if="formData.coverImg" :src="getFullImageUrl(formData.coverImg)" class="cover-img" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="form-tip">建议尺寸：800x450，支持 jpg/png，不超过 2MB</div>
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <MdEditor v-model="formData.content" height="500px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listNews, createNews, updateNews, deleteNews, listNewsCategories } from '@/apis/admin'
import { getFullImageUrl } from '@/utils/image'
import { MdEditor } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';

const uploadAction = (import.meta.env.VITE_API_BASE_URL || '/api') + '/common/upload'

const categoryId = ref<number | undefined>(undefined)
const auditStatus = ref<number | undefined>(undefined)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const newsList = ref<any[]>([])
const categoryList = ref<any[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  id: undefined as number | undefined,
  title: '',
  categoryId: undefined as number | undefined,
  coverImg: '',
  content: '',
  auditStatus: 1
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }]
}

const getCategoryName = (id: number) => {
  const cat = categoryList.value.find(c => c.id === id);
  return cat ? cat.categoryName : '未知分类';
}

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
    1: '已发布',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    formData.coverImg = response.data;
    ElMessage.success('上传成功');
  } else {
    ElMessage.error(response.msg || '上传失败');
  }
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('图片必须是 JPG/PNG 格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

const loadCategories = async () => {
  try {
    const res = await listNewsCategories();
    if (res) {
      categoryList.value = res;
    }
  }
  catch (error) {
    console.error(error);
  }
}

const loadNews = async () => {
  loading.value = true;
  try {
    const res = await listNews({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      categoryId: categoryId.value,
      auditStatus: auditStatus.value
    });
    if (res) {
      newsList.value = res.list || [];
      total.value = res.total || 0;
    }
  }
  catch (error) {
    console.error(error);
  }
  finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadCategories();
  loadNews();
})

const handleAdd = () => {
  isEdit.value = false;
  formData.id = undefined;
  formData.title = '';
  formData.categoryId = undefined;
  formData.coverImg = '';
  formData.content = '';
  formData.auditStatus = 1; // 默认直接发布
  dialogVisible.value = true;
  if (formRef.value) formRef.value.clearValidate();
}

const handleEdit = (row: any) => {
  isEdit.value = true;
  formData.id = row.id;
  formData.title = row.title;
  formData.categoryId = row.categoryId;
  formData.coverImg = row.coverImg;
  formData.content = row.content;
  formData.auditStatus = row.auditStatus;
  dialogVisible.value = true;
}

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        if (isEdit.value && formData.id) {
          await updateNews(formData.id, formData);
          ElMessage.success('修改成功');
        }
        else {
          await createNews(formData);
          ElMessage.success('发布成功');
        }
        dialogVisible.value = false;
        loadNews();
      }
      catch (error) {
        console.error(error);
      }
      finally {
        submitLoading.value = false;
      }
    }
  });
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除这条资讯吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteNews(row.id);
      ElMessage.success('删除成功');
      loadNews();
    }
    catch (error) {
      console.error(error);
    }
  }).catch(() => {})
}

const handleAudit = async (row: any, status: number) => {
  try {
    await updateNews(row.id, { ...row, auditStatus: status });
    ElMessage.success(status === 1 ? '已通过' : '已拒绝');
    loadNews();
  }
  catch (error) {
    console.error(error);
  }
}
</script>

<style scoped lang="scss">
.news-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.cover-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
    
    &:hover {
      border-color: var(--el-color-primary);
    }
  }
}

.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.cover-img {
  width: 178px;
  height: 100px;
  display: block;
  object-fit: cover;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
