<template>
  <div class="image-upload">
    <!-- 图片列表 -->
    <div class="image-list">
      <div
        v-for="(file, index) in fileList"
        :key="index"
        class="image-item"
        :style="{ width: size + 'px', height: size + 'px' }"
      >
        <img :src="file.url" :alt="file.name" />
        <div class="image-actions">
          <el-icon @click="previewImage(file.url)"><ZoomIn /></el-icon>
          <el-icon @click="removeImage(index)"><Delete /></el-icon>
        </div>
      </div>
      
      <!-- 上传按钮 -->
      <el-upload
        v-if="fileList.length < maxCount"
        class="upload-trigger"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :show-file-list="false"
        :before-upload="beforeUpload"
        :on-success="handleSuccess"
        :on-error="handleError"
        :on-progress="handleProgress"
        :multiple="multiple"
        accept="image/*"
      >
        <div
          class="upload-placeholder"
          :style="{ width: size + 'px', height: size + 'px' }"
          v-loading="uploading"
        >
          <el-icon><Plus /></el-icon>
          <span v-if="!uploading" class="upload-text">上传图片</span>
          <span v-else class="upload-text">{{ uploadProgress }}%</span>
        </div>
      </el-upload>
    </div>
    
    <!-- 提示信息 -->
    <div class="upload-tips">
      <el-text type="info" size="small">
        支持 {{ allowedTypes.join('/') }} 格式，单张不超过 {{ maxSize }}MB
        <template v-if="maxCount > 1">
          ，最多上传 {{ maxCount }} 张
        </template>
      </el-text>
    </div>
    
    <!-- 图片预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { Plus, ZoomIn, Delete } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/modules/user';

interface UploadFile {
  url: string;
  name: string;
  size?: number;
}

interface Props {
  modelValue: string | string[];
  maxCount?: number;
  maxSize?: number;
  size?: number;
  multiple?: boolean;
  uploadType?: 'general' | 'product' | 'avatar' | 'qualification' | 'news';
}

const props = withDefaults(defineProps<Props>(), {
  maxCount: 1,
  maxSize: 10,
  size: 120,
  multiple: false,
  uploadType: 'general'
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | string[]): void;
  (e: 'change', value: string | string[]): void;
}>();

const userStore = useUserStore();

// 上传配置
const uploadUrl = import.meta.env.VITE_API_BASE_URL + '/common/upload/image';
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}));

const allowedTypes = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

// 状态
const fileList = ref<UploadFile[]>([]);
const uploading = ref(false);
const uploadProgress = ref(0);
const previewVisible = ref(false);
const previewIndex = ref(0);

// 预览列表
const previewList = computed(() => fileList.value.map(f => f.url));

// 初始化文件列表
const initFileList = () => {
  if (!props.modelValue) {
    fileList.value = [];
    return;
  }
  
  if (Array.isArray(props.modelValue)) {
    fileList.value = props.modelValue.map((url, index) => ({
      url,
      name: `图片${index + 1}`
    }));
  } else {
    fileList.value = [{
      url: props.modelValue,
      name: '图片1'
    }];
  }
};

// 监听modelValue变化
watch(() => props.modelValue, initFileList, { immediate: true });

// 上传前检查
const beforeUpload = (file: File) => {
  // 检查文件类型
  const ext = file.name.split('.').pop()?.toLowerCase();
  if (!ext || !allowedTypes.includes(ext)) {
    ElMessage.error(`仅支持 ${allowedTypes.join('/')} 格式图片`);
    return false;
  }
  
  // 检查文件大小
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`);
    return false;
  }
  
  // 检查数量限制
  if (fileList.value.length >= props.maxCount) {
    ElMessage.warning(`最多只能上传 ${props.maxCount} 张图片`);
    return false;
  }
  
  uploading.value = true;
  uploadProgress.value = 0;
  return true;
};

// 上传进度
const handleProgress = (event: any) => {
  uploadProgress.value = Math.round((event.loaded / event.total) * 100);
};

// 上传成功
const handleSuccess = (response: any) => {
  uploading.value = false;
  uploadProgress.value = 0;
  
  if (response.code === 200) {
    const url = response.data.url;
    fileList.value.push({
      url,
      name: response.data.originalName || '图片'
    });
    
    updateValue();
    ElMessage.success('上传成功');
  } else {
    ElMessage.error(response.msg || '上传失败');
  }
};

// 上传失败
const handleError = () => {
  uploading.value = false;
  uploadProgress.value = 0;
  ElMessage.error('上传失败，请重试');
};

// 删除图片
const removeImage = (index: number) => {
  fileList.value.splice(index, 1);
  updateValue();
};

// 预览图片
const previewImage = (url: string) => {
  previewIndex.value = fileList.value.findIndex(f => f.url === url);
  previewVisible.value = true;
};

// 更新值
const updateValue = () => {
  const urls = fileList.value.map(f => f.url);
  const value = props.maxCount === 1 ? (urls[0] || '') : urls;
  emit('update:modelValue', value);
  emit('change', value);
};
</script>

<style scoped lang="scss">
.image-upload {
  .image-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .image-item {
    position: relative;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #e4e7ed;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .image-actions {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      opacity: 0;
      transition: opacity 0.3s;
      
      .el-icon {
        color: #fff;
        font-size: 20px;
        cursor: pointer;
        
        &:hover {
          color: #409eff;
        }
      }
    }
    
    &:hover .image-actions {
      opacity: 1;
    }
  }
  
  .upload-trigger {
    :deep(.el-upload) {
      display: block;
    }
  }
  
  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 2px dashed #d9d9d9;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.3s;
    background: #fafafa;
    
    &:hover {
      border-color: #409eff;
    }
    
    .el-icon {
      font-size: 28px;
      color: #8c939d;
      margin-bottom: 8px;
    }
    
    .upload-text {
      font-size: 12px;
      color: #8c939d;
    }
  }
  
  .upload-tips {
    margin-top: 8px;
  }
}
</style>
