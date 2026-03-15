<template>
  <div class="news-page">
    <div class="page-header">
      <h2>新闻管理</h2>
      <el-button type="primary" @click="showAddDialog = true">发布新闻</el-button>
    </div>

    <el-card>
      <el-table :data="newsList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="150" />
        <el-table-column prop="views" label="阅读量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editNews(row)">编辑</el-button>
            <el-button link type="danger" @click="deleteNews(row)">删除</el-button>
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

    <!-- 添加/编辑新闻弹窗 -->
    <el-dialog v-model="showAddDialog" :title="isEdit ? '编辑新闻' : '发布新闻'" width="90%">
      <el-form :model="newsForm" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="newsForm.title" placeholder="请输入新闻标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newsForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="农业政策" value="policy" />
            <el-option label="种植技术" value="technology" />
            <el-option label="市场行情" value="market" />
            <el-option label="助农故事" value="story" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            class="cover-uploader"
            action="#"
            :auto-upload="false"
            :show-file-list="false"
          >
            <img v-if="newsForm.cover" :src="newsForm.cover" class="cover-preview" />
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <span>点击上传封面</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="newsForm.content"
            type="textarea"
            rows="10"
            placeholder="请输入新闻内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" @click="publishNews">立即发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(50)
const showAddDialog = ref(false)
const isEdit = ref(false)

const newsForm = reactive({
  title: '',
  category: '',
  cover: '',
  content: ''
})

const newsList = ref([
  {
    id: 1,
    title: '2024年农业补贴政策解读',
    category: '农业政策',
    author: '管理员',
    publishTime: '2024-03-08 10:30:00',
    views: 1256,
    status: 'published'
  },
  {
    id: 2,
    title: '春季蔬菜种植技术指南',
    category: '种植技术',
    author: '农技专家',
    publishTime: '2024-03-07 14:20:00',
    views: 892,
    status: 'published'
  }
])

const editNews = (row: any) => {
  isEdit.value = true
  Object.assign(newsForm, row)
  showAddDialog.value = true
}

const deleteNews = (row: any) => {
  ElMessageBox.confirm('确定要删除这条新闻吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    newsList.value = newsList.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  })
}

const saveDraft = () => {
  ElMessage.success('已保存为草稿')
  showAddDialog.value = false
}

const publishNews = () => {
  if (!newsForm.title || !newsForm.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  ElMessage.success(isEdit.value ? '修改成功' : '发布成功')
  showAddDialog.value = false
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

.cover-uploader {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  width: 300px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  &:hover {
    border-color: #409eff;
  }

  .cover-preview {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    color: #8c939d;

    .el-icon {
      font-size: 28px;
      margin-bottom: 8px;
    }

    span {
      font-size: 14px;
    }
  }
}
</style>
