<template>
  <div class="product-edit-page">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>编辑商品</h2>
      </div>
      <el-button type="primary" @click="saveProduct">保存修改</el-button>
    </div>

    <el-form :model="productForm" label-position="top" class="product-form">
      <el-card class="form-card">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-form-item label="商品名称">
          <el-input v-model="productForm.name" placeholder="请输入商品名称" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="商品分类">
          <el-cascader
            v-model="productForm.category"
            :options="categoryOptions"
            placeholder="请选择商品分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :limit="5"
            :file-list="fileList"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="upload-tip">最多上传5张图片，建议尺寸800x800px</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>价格库存</span>
        </template>
        <el-form-item label="售价">
          <el-input-number v-model="productForm.price" :min="0" :precision="2" :step="0.1" style="width: 200px">
            <template #append>元</template>
          </el-input-number>
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="productForm.originalPrice" :min="0" :precision="2" :step="0.1" style="width: 200px">
            <template #append>元</template>
          </el-input-number>
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="productForm.stock" :min="0" :step="1" style="width: 200px">
            <template #append>件</template>
          </el-input-number>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-radio-group v-model="productForm.unit">
            <el-radio-button label="斤">斤</el-radio-button>
            <el-radio-button label="公斤">公斤</el-radio-button>
            <el-radio-button label="件">件</el-radio-button>
            <el-radio-button label="箱">箱</el-radio-button>
            <el-radio-button label="袋">袋</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>商品详情</span>
        </template>
        <el-form-item label="商品描述">
          <el-input
            v-model="productForm.description"
            type="textarea"
            rows="4"
            placeholder="请描述商品的特点、产地、口感等信息"
          />
        </el-form-item>
        <el-form-item label="产地">
          <el-input v-model="productForm.origin" placeholder="请输入产地" />
        </el-form-item>
        <el-form-item label="保质期">
          <el-input v-model="productForm.shelfLife" placeholder="如：7天、30天" />
        </el-form-item>
        <el-form-item label="储存方式">
          <el-input v-model="productForm.storage" placeholder="如：冷藏、常温" />
        </el-form-item>
      </el-card>

      <el-card class="form-card">
        <template #header>
          <span>商品状态</span>
        </template>
        <el-form-item>
          <el-radio-group v-model="productForm.status">
            <el-radio label="on_sale">上架销售</el-radio>
            <el-radio label="off_sale">下架停售</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'

const route = useRoute()
void route

const fileList = ref([
  {
    name: 'product1.jpg',
    url: 'https://via.placeholder.com/150'
  }
])

const productForm = reactive({
  name: '新鲜有机西红柿',
  category: ['vegetables', 'melon'],
  price: 5.99,
  originalPrice: 8.99,
  stock: 100,
  unit: '斤',
  description: '自家种植，绿色有机，口感鲜美',
  origin: '山东寿光',
  shelfLife: '7天',
  storage: '冷藏',
  status: 'on_sale'
})

const categoryOptions = [
  {
    value: 'vegetables',
    label: '新鲜蔬菜',
    children: [
      { value: 'leafy', label: '叶菜类' },
      { value: 'root', label: '根茎类' },
      { value: 'melon', label: '瓜果类' }
    ]
  },
  {
    value: 'fruits',
    label: '水果',
    children: [
      { value: 'apple', label: '苹果梨桃' },
      { value: 'citrus', label: '柑橘橙柚' },
      { value: 'tropical', label: '热带水果' }
    ]
  }
]

const saveProduct = () => {
  ElMessage.success('商品修改成功')
}
</script>

<style scoped lang="scss">
.product-edit-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 20px;
    }
  }
}

.product-form {
  max-width: 800px;
}

.form-card {
  margin-bottom: 20px;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}
</style>
