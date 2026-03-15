<template>
  <div class="address-page">
    <div class="page-header">
      <el-button link @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h2>收货地址</h2>
      <el-button type="primary" size="small" @click="showAddDialog = true">
        新增地址
      </el-button>
    </div>

    <div class="address-list" v-if="addressList.length > 0">
      <div
        v-for="addr in addressList"
        :key="addr.id"
        class="address-card"
        :class="{ default: addr.isDefault === 1 }"
      >
        <div class="address-info">
          <div class="user-info">
            <span class="name">{{ addr.receiver }}</span>
            <span class="phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault === 1" type="danger" size="small">默认</el-tag>
          </div>
          <p class="address-text">
            {{ addr.province }}{{ addr.city }}{{ addr.county }}{{ addr.town }}{{ addr.detailAddress }}
          </p>
        </div>
        <div class="address-actions">
          <el-button link type="primary" @click="editAddress(addr)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDelete(addr.id)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无收货地址，请添加">
      <el-button type="primary" @click="showAddDialog = true">添加地址</el-button>
    </el-empty>

    <!-- 新增/编辑地址弹窗 -->
    <el-dialog
      v-model="showAddDialog"
      :title="isEdit ? '编辑地址' : '新增地址'"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form :model="addressForm" label-position="top">
        <el-form-item label="收货人姓名">
          <el-input v-model="addressForm.receiver" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号码">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号码" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区">
          <el-cascader
            v-model="addressForm.region"
            :options="regionOptions"
            placeholder="请选择省/市/区"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            rows="3"
            placeholder="请输入街道、门牌号等详细地址"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox 
            v-model="addressForm.isDefault" 
            :true-label="1" 
            :false-label="0"
          >设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ArrowLeft, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/apis/user'
import type { UserAddress } from '@/types'

const addressList = ref<UserAddress[]>([])
const showAddDialog = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const addressForm = reactive({
  receiver: '',
  phone: '',
  region: [] as string[],
  detailAddress: '',
  isDefault: 0 // 0-否 1-是
})

// 模拟地区数据
const regionOptions = [
  {
    value: '四川省',
    label: '四川省',
    children: [
      {
        value: '成都市',
        label: '成都市',
        children: [
          {
            value: '锦江区',
            label: '锦江区',
            children: [
              { value: '春熙路街道', label: '春熙路街道' },
              { value: '盐市口街道', label: '盐市口街道' }
            ]
          },
          {
            value: '蒲江县',
            label: '蒲江县',
            children: [
              { value: '寿安镇', label: '寿安镇' },
              { value: '鹤山街道', label: '鹤山街道' }
            ]
          }
        ]
      },
      {
        value: '绵阳市',
        label: '绵阳市',
        children: [
          {
            value: '三台县',
            label: '三台县',
            children: [
              { value: '芦溪镇', label: '芦溪镇' },
              { value: '塔山镇', label: '塔山镇' }
            ]
          }
        ]
      }
    ]
  }
]

onMounted(() => {
  loadAddresses()
})

const loadAddresses = async () => {
  try {
    const res = await getAddresses()
    if (res) {
      addressList.value = res
    }
  } catch (error) {
    console.error('加载地址失败', error)
    ElMessage.error('加载地址失败')
  }
}

const editAddress = (addr: UserAddress) => {
  isEdit.value = true
  editingId.value = addr.id
  addressForm.receiver = addr.receiver
  addressForm.phone = addr.phone
  addressForm.region = [addr.province, addr.city, addr.county, addr.town].filter(Boolean)
  addressForm.detailAddress = addr.detailAddress
  addressForm.isDefault = addr.isDefault
  showAddDialog.value = true
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除这个地址吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteAddress(id)
      ElMessage.success('删除成功')
      loadAddresses()
    } catch (error) {
      console.error(error)
      ElMessage.error('删除失败')
    }
  })
}

const saveAddress = async () => {
  if (!addressForm.receiver || !addressForm.phone || !addressForm.detailAddress || addressForm.region.length === 0) {
    ElMessage.warning('请填写完整信息')
    return
  }

  const data = {
    receiver: addressForm.receiver,
    phone: addressForm.phone,
    province: addressForm.region[0] || '',
    city: addressForm.region[1] || '',
    county: addressForm.region[2] || '',
    town: addressForm.region[3] || '',
    detailAddress: addressForm.detailAddress,
    isDefault: addressForm.isDefault
  }

  try {
    if (isEdit.value && editingId.value !== null) {
      await updateAddress(editingId.value, data)
      ElMessage.success('修改成功')
    } else {
      await addAddress(data)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    loadAddresses()
    resetForm()
  } catch (error) {
    console.error(error)
    ElMessage.error(isEdit.value ? '修改失败' : '添加失败')
  }
}

const resetForm = () => {
  addressForm.receiver = ''
  addressForm.phone = ''
  addressForm.region = []
  addressForm.detailAddress = ''
  addressForm.isDefault = 0
  isEdit.value = false
  editingId.value = null
}
</script>

<style scoped lang="scss">
.address-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 100;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.address-list {
  padding: 12px;
}

.address-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid #ebeef5;

  &.default {
    border-color: #67c23a;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;

  .name {
    font-size: 16px;
    font-weight: 600;
  }

  .phone {
    color: #666;
    font-size: 14px;
  }
}

.address-text {
  color: #333;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.address-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
</style>
