<template>
  <div class="news-page">
    <div class="page-header">
      <div>
        <h2>资讯管理</h2>
        <p>统一管理资讯分类、封面和正文内容，发布前可以先预览最终展示效果。</p>
      </div>
      <div class="header-actions">
        <el-select
          v-model="categoryId"
          clearable
          placeholder="全部分类"
          style="width: 160px"
          @change="handleFilterChange"
        >
          <el-option
            v-for="cat in categoryList"
            :key="cat.id"
            :label="cat.categoryName"
            :value="cat.id"
          />
        </el-select>
        <el-select
          v-model="auditStatus"
          clearable
          placeholder="发布状态"
          style="width: 160px"
          @change="handleFilterChange"
        >
          <el-option label="待审核" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
        <el-button @click="openCategoryDialog">分类管理</el-button>
        <el-button type="primary" @click="handleAdd">发布资讯</el-button>
      </div>
    </div>

    <el-card class="news-card" shadow="never">
      <el-table :data="newsList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="封面" width="108">
          <template #default="{ row }">
            <el-image
              v-if="row.coverImg"
              class="table-cover"
              :src="getFullImageUrl(row.coverImg)"
              fit="cover"
              :preview-src-list="[getFullImageUrl(row.coverImg)]"
              preview-teleported
            />
            <span v-else class="table-empty">未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="140">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getCategoryName(row.categoryId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.auditStatus)">
              {{ getStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
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
          :page-sizes="[10, 20, 30]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadNews"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑资讯' : '发布资讯'"
      width="1180px"
      top="4vh"
      destroy-on-close
      @closed="handleDialogClosed"
    >
      <div class="news-dialog" v-loading="dialogLoading">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="88px"
        >
          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="标题" prop="title">
                <el-input
                  v-model="formData.title"
                  maxlength="60"
                  show-word-limit
                  placeholder="请输入资讯标题，建议 18-32 个字"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="发布状态" prop="auditStatus">
                <el-radio-group v-model="formData.auditStatus">
                  <el-radio-button :value="1">直接发布</el-radio-button>
                  <el-radio-button :value="0">存为待审核</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <div class="editor-layout">
            <div class="editor-main">
              <el-tabs v-model="editorTab" class="editor-tabs">
                <el-tab-pane label="编辑内容" name="edit">
                  <el-form-item label="正文" prop="content" class="content-form-item">
                    <MdEditor
                      v-model="formData.content"
                      class="news-editor"
                      :preview="true"
                      :toolbars-exclude="[]"
                      :onUploadImg="handleEditorUpload"
                      placeholder="支持 Markdown，正文图片可直接拖拽或粘贴上传。"
                      height="560px"
                    />
                  </el-form-item>
                </el-tab-pane>
                <el-tab-pane label="预览效果" name="preview">
                  <div class="preview-panel">
                    <div class="preview-meta">
                      <h3>{{ formData.title || '请输入标题后预览' }}</h3>
                      <div class="preview-tags">
                        <el-tag effect="plain">{{ getCategoryName(formData.categoryId) }}</el-tag>
                        <el-tag :type="getStatusType(formData.auditStatus)">
                          {{ getStatusText(formData.auditStatus) }}
                        </el-tag>
                      </div>
                    </div>
                    <div v-if="formData.coverImg" class="preview-cover">
                      <img :src="getFullImageUrl(formData.coverImg)" alt="cover" />
                    </div>
                    <MdPreview
                      v-if="formData.content.trim()"
                      editorId="admin-news-preview"
                      :modelValue="formData.content"
                    />
                    <el-empty v-else description="先填写正文内容，再查看预览效果" />
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>

            <div class="editor-side">
              <el-card shadow="never" class="meta-card">
                <template #header>
                  <div class="meta-title">发布信息</div>
                </template>
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

                <el-form-item label="封面" prop="coverImg">
                  <el-upload
                    class="cover-uploader"
                    :show-file-list="false"
                    :http-request="uploadCover"
                    :before-upload="beforeUpload"
                    accept=".jpg,.jpeg,.png,.gif,.webp"
                  >
                    <img
                      v-if="formData.coverImg"
                      :src="getFullImageUrl(formData.coverImg)"
                      class="cover-img"
                    />
                    <div v-else class="cover-placeholder">
                      <el-icon class="cover-uploader-icon"><Plus /></el-icon>
                      <span>上传封面图</span>
                    </div>
                  </el-upload>
                  <div class="cover-actions">
                    <span class="form-tip">建议尺寸 800x450，支持 jpg/png/gif/webp，单张不超过 5MB</span>
                    <el-button
                      v-if="formData.coverImg"
                      link
                      type="danger"
                      @click.stop="handleRemoveCover"
                    >
                      移除封面
                    </el-button>
                  </div>
                </el-form-item>

                <div class="publish-stat">
                  <div class="stat-item">
                    <span>正文长度</span>
                    <strong>{{ contentLength }}</strong>
                  </div>
                  <div class="stat-item">
                    <span>封面状态</span>
                    <strong>{{ formData.coverImg ? '已上传' : '待上传' }}</strong>
                  </div>
                </div>

                <el-alert
                  v-if="!categoryList.length"
                  type="warning"
                  show-icon
                  :closable="false"
                  title="当前还没有资讯分类，请先创建分类再发布资讯"
                />
              </el-card>
            </div>
          </div>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '发布资讯' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="categoryDialogVisible"
      title="资讯分类管理"
      width="760px"
      destroy-on-close
    >
      <div class="category-dialog">
        <div class="category-toolbar">
          <div>
            <h3>分类设置</h3>
            <p>统一维护资讯分类，新增后可直接用于新闻发布与筛选。</p>
          </div>
          <el-tag effect="plain" type="success">当前 {{ categoryList.length }} 个分类</el-tag>
        </div>

        <el-form
          ref="categoryFormRef"
          :model="categoryForm"
          :rules="categoryRules"
          label-position="top"
          class="category-form"
        >
          <div class="category-form-grid">
            <el-form-item label="分类名称" prop="categoryName" class="category-form-item">
              <el-input v-model="categoryForm.categoryName" maxlength="20" placeholder="如：助农政策" />
            </el-form-item>
            <el-form-item label="分类编码" prop="categoryCode" class="category-form-item">
              <el-input v-model="categoryForm.categoryCode" maxlength="20" placeholder="可选，如：policy" />
            </el-form-item>
            <div class="category-form-actions">
              <el-button @click="resetCategoryForm">重置</el-button>
              <el-button type="primary" :loading="categorySaving" @click="handleSaveCategory">
                {{ categoryForm.id ? '保存' : '新增' }}
              </el-button>
            </div>
          </div>
        </el-form>

        <div class="category-table-wrap">
          <div class="category-table-head">
            <span>分类列表</span>
            <span class="table-tip">点击“编辑”后，上方表单会自动回填当前分类。</span>
          </div>

          <el-table :data="categoryList" border>
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="categoryName" label="分类名称" min-width="180" />
            <el-table-column prop="categoryCode" label="分类编码" min-width="160">
              <template #default="{ row }">
                {{ row.categoryCode || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="editCategory(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDeleteCategory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules, UploadProps, UploadRequestOptions } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { MdEditor, MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import 'md-editor-v3/lib/preview.css';
import request from '@/utils/request';
import {
  createNews,
  createNewsCategory,
  deleteNews,
  deleteNewsCategory,
  getNews,
  listNews,
  listNewsCategories,
  updateNews,
  updateNewsCategory
} from '@/apis/admin';
import { getFullImageUrl } from '@/utils/image';

type NewsForm = {
  id: number | undefined;
  title: string;
  categoryId: number | undefined;
  coverImg: string;
  content: string;
  auditStatus: number;
};

type NewsCategoryForm = {
  id: number | undefined;
  categoryName: string;
  categoryCode: string;
};

const categoryId = ref<number | undefined>(undefined);
const auditStatus = ref<number | undefined>(undefined);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const dialogLoading = ref(false);
const newsList = ref<any[]>([]);
const categoryList = ref<any[]>([]);

const dialogVisible = ref(false);
const categoryDialogVisible = ref(false);
const isEdit = ref(false);
const submitLoading = ref(false);
const categorySaving = ref(false);
const formRef = ref<FormInstance>();
const categoryFormRef = ref<FormInstance>();
const editorTab = ref('edit');

const createDefaultForm = (): NewsForm => ({
  id: undefined,
  title: '',
  categoryId: undefined,
  coverImg: '',
  content: '',
  auditStatus: 1
});

const createDefaultCategoryForm = (): NewsCategoryForm => ({
  id: undefined,
  categoryName: '',
  categoryCode: ''
});

const formData = reactive<NewsForm>(createDefaultForm());
const categoryForm = reactive<NewsCategoryForm>(createDefaultCategoryForm());

const contentLength = computed(() => formData.content.trim().length);

const rules: FormRules<NewsForm> = {
  title: [
    {
      validator: (_rule, value, callback) => {
        if (!String(value || '').trim()) {
          callback(new Error('请输入标题'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  coverImg: [
    {
      validator: (_rule, value, callback) => {
        if (!String(value || '').trim()) {
          callback(new Error('请上传封面图'));
          return;
        }
        callback();
      },
      trigger: 'change'
    }
  ],
  content: [
    {
      validator: (_rule, value, callback) => {
        if (!String(value || '').trim()) {
          callback(new Error('请输入正文内容'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ]
};

const categoryRules: FormRules<NewsCategoryForm> = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
};

const resetForm = () => {
  Object.assign(formData, createDefaultForm());
  editorTab.value = 'edit';
};

const resetCategoryForm = () => {
  Object.assign(categoryForm, createDefaultCategoryForm());
  categoryFormRef.value?.clearValidate();
};

const formatDate = (date?: string) => {
  if (!date) return '-';
  return date.replace('T', ' ').slice(0, 16);
};

const getCategoryName = (id?: number) => {
  if (!id) return '未分类';
  const cat = categoryList.value.find(item => Number(item.id) === Number(id));
  return cat?.categoryName || '未知分类';
};

const getStatusType = (status?: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  };
  return map[Number(status)] || 'info';
};

const getStatusText = (status?: number) => {
  const map: Record<number, string> = {
    0: '待审核',
    1: '已发布',
    2: '已拒绝'
  };
  return map[Number(status)] || '未知';
};

const loadCategories = async () => {
  const res = await listNewsCategories();
  categoryList.value = res || [];
};

const loadNews = async () => {
  loading.value = true;
  try {
    const res = await listNews({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      categoryId: categoryId.value,
      auditStatus: auditStatus.value
    });
    newsList.value = res?.list || [];
    total.value = Number(res?.total || 0);
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = async () => {
  currentPage.value = 1;
  await loadNews();
};

const handleSizeChange = async () => {
  currentPage.value = 1;
  await loadNews();
};

const uploadNewsFile = async (file: File) => {
  const form = new FormData();
  form.append('file', file);
  const res = await request.post<any, { url: string }>('/common/upload/news', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  if (!res?.url) {
    throw new Error('上传结果异常');
  }
  return res.url;
};

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.error('图片必须是 JPG/PNG/GIF/WEBP 格式');
    return false;
  }
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB');
    return false;
  }
  return true;
};

const uploadCover = async (options: UploadRequestOptions) => {
  try {
    const url = await uploadNewsFile(options.file as File);
    formData.coverImg = url;
    options.onSuccess?.({ url });
    ElMessage.success('封面上传成功');
    formRef.value?.validateField('coverImg');
  } catch (error) {
    options.onError?.(error as any);
  }
};

const handleEditorUpload = async (
  files: File[],
  callback: (urls: string[]) => void
) => {
  try {
    const urls = await Promise.all(files.map(uploadNewsFile));
    callback(urls.map(url => getFullImageUrl(url)));
    ElMessage.success('正文图片上传成功');
  } catch (error) {
    console.error('正文图片上传失败', error);
    ElMessage.error('正文图片上传失败');
  }
};

const handleRemoveCover = () => {
  formData.coverImg = '';
  formRef.value?.validateField('coverImg');
};

const handleAdd = async () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
  await nextTick();
  formRef.value?.clearValidate();
};

const fillForm = (data: Partial<NewsForm>) => {
  Object.assign(formData, createDefaultForm(), {
    id: data.id,
    title: data.title || '',
    categoryId: data.categoryId,
    coverImg: data.coverImg || '',
    content: data.content || '',
    auditStatus: Number(data.auditStatus ?? 1)
  });
};

const handleEdit = async (row: any) => {
  isEdit.value = true;
  dialogVisible.value = true;
  dialogLoading.value = true;
  resetForm();
  try {
    const detail = await getNews(row.id);
    fillForm(detail || row);
    await nextTick();
    formRef.value?.clearValidate();
  } finally {
    dialogLoading.value = false;
  }
};

const buildPayload = () => ({
  title: formData.title.trim(),
  categoryId: formData.categoryId,
  coverImg: formData.coverImg.trim(),
  content: formData.content.trim(),
  auditStatus: formData.auditStatus
});

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    const payload = buildPayload();
    if (isEdit.value && formData.id) {
      await updateNews(formData.id, payload);
      ElMessage.success('资讯修改成功');
    } else {
      await createNews(payload);
      ElMessage.success('资讯发布成功');
    }
    dialogVisible.value = false;
    await loadNews();
  } finally {
    submitLoading.value = false;
  }
};

const handleDialogClosed = () => {
  dialogLoading.value = false;
  resetForm();
  formRef.value?.clearValidate();
};

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定删除资讯《${row.title}》吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteNews(row.id);
    ElMessage.success('删除成功');
    await loadNews();
  }).catch(() => {});
};

const handleAudit = async (row: any, status: number) => {
  await updateNews(row.id, { auditStatus: status });
  ElMessage.success(status === 1 ? '已通过并发布' : '已拒绝');
  await loadNews();
};

const openCategoryDialog = async () => {
  categoryDialogVisible.value = true;
  resetCategoryForm();
  await loadCategories();
};

const editCategory = (row: any) => {
  Object.assign(categoryForm, {
    id: row.id,
    categoryName: row.categoryName || '',
    categoryCode: row.categoryCode || ''
  });
};

const handleSaveCategory = async () => {
  if (!categoryFormRef.value) return;
  await categoryFormRef.value.validate();
  categorySaving.value = true;
  try {
    const payload = {
      categoryName: categoryForm.categoryName.trim(),
      categoryCode: categoryForm.categoryCode.trim() || undefined
    };
    if (categoryForm.id) {
      await updateNewsCategory(categoryForm.id, payload);
      ElMessage.success('分类修改成功');
    } else {
      await createNewsCategory(payload);
      ElMessage.success('分类新增成功');
    }
    resetCategoryForm();
    await loadCategories();
  } finally {
    categorySaving.value = false;
  }
};

const handleDeleteCategory = (row: any) => {
  ElMessageBox.confirm(`确定删除分类“${row.categoryName}”吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteNewsCategory(row.id);
    if (Number(formData.categoryId) === Number(row.id)) {
      formData.categoryId = undefined;
    }
    if (Number(categoryId.value) === Number(row.id)) {
      categoryId.value = undefined;
    }
    ElMessage.success('分类删除成功');
    resetCategoryForm();
    await Promise.all([loadCategories(), loadNews()]);
  }).catch(() => {});
};

onMounted(async () => {
  await Promise.all([loadCategories(), loadNews()]);
});
</script>

<style scoped lang="scss">
.news-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;

  h2 {
    margin: 0 0 6px;
    font-size: 26px;
    color: #1f2937;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.news-card {
  border-radius: 16px;
}

.table-cover {
  width: 68px;
  height: 42px;
  border-radius: 6px;
}

.table-empty {
  color: #9ca3af;
  font-size: 13px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.news-dialog {
  min-height: 300px;
}

.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 20px;
}

.editor-main,
.editor-side {
  min-width: 0;
}

.editor-tabs {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 0 16px 16px;
}

.content-form-item {
  margin-bottom: 0;
}

.news-editor {
  border-radius: 12px;
  overflow: hidden;
}

.preview-panel {
  min-height: 560px;
  padding: 8px 4px 0;
}

.preview-meta {
  margin-bottom: 16px;

  h3 {
    margin: 0 0 12px;
    font-size: 28px;
    line-height: 1.4;
    color: #111827;
  }
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preview-cover {
  margin-bottom: 18px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #f8fafc;

  img {
    width: 100%;
    max-height: 320px;
    object-fit: cover;
    display: block;
  }
}

.meta-card {
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.meta-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.cover-uploader {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
    display: block;
    border: 1px dashed #cbd5e1;
    border-radius: 14px;
    overflow: hidden;
    background: #f8fafc;
    transition: border-color 0.2s ease, transform 0.2s ease;

    &:hover {
      border-color: var(--el-color-primary);
      transform: translateY(-1px);
    }
  }
}

.cover-placeholder {
  width: 100%;
  height: 170px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #64748b;
}

.cover-uploader-icon {
  font-size: 28px;
}

.cover-img {
  width: 100%;
  height: 170px;
  display: block;
  object-fit: cover;
}

.cover-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.form-tip {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.publish-stat {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid #e5e7eb;
  color: #475569;

  strong {
    color: #0f172a;
    font-size: 15px;
  }
}

.category-dialog {
  display: grid;
  gap: 18px;
}

.category-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  h3 {
    margin: 0 0 6px;
    font-size: 18px;
    color: #111827;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 13px;
  }
}

.category-form {
  padding: 18px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.category-form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.category-form-item {
  margin-bottom: 0;
}

.category-form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-bottom: 2px;
}

.category-table-wrap {
  display: grid;
  gap: 12px;
}

.category-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #111827;
  font-weight: 600;
}

.table-tip {
  font-size: 12px;
  font-weight: 400;
  color: #6b7280;
}

@media (max-width: 1200px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .news-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.el-select),
  .header-actions :deep(.el-button) {
    width: 100%;
  }

  .editor-tabs {
    padding: 0 10px 12px;
  }

  .preview-meta h3 {
    font-size: 22px;
  }

  .cover-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .category-toolbar,
  .category-table-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .category-form-grid {
    grid-template-columns: 1fr;
  }

  .category-form-actions {
    justify-content: flex-start;
    padding-bottom: 0;
  }
}
</style>
