<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <el-button link @click="$router.back()" class="back-btn" style="margin-right: 10px; font-size: 18px;">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <span>个人信息</span>
        </div>
      </template>
      <div class="profile-info">
        <el-avatar :size="64" :src="userInfo?.avatarUrl" />
        <div class="info">
          <div>昵称：{{ userInfo?.nickname }}</div>
          <div>手机号：{{ userInfo?.phone }}</div>
        </div>
      </div>
    </el-card>

    <el-card class="address-card">
      <template #header>
        <div class="address-header">
          <span>收货地址</span>
          <el-button size="small" type="primary" @click="openDialog">新增地址</el-button>
        </div>
      </template>
      <el-table :data="addressList" style="width: 100%">
        <el-table-column prop="receiver" label="收货人" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column label="地址">
          <template #default="scope">
            {{ scope.row.province }}{{ scope.row.city }}{{ scope.row.county }}{{ scope.row.town }}{{ scope.row.detailAddress }}
          </template>
        </el-table-column>
        <el-table-column label="默认" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.isDefault === 1" type="success">是</el-tag>
            <el-tag v-else>否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button size="small" @click="setDefault(scope.row)">设为默认</el-button>
            <el-button size="small" type="danger" @click="removeAddress(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" title="新增地址" width="500px">
      <el-form :model="addressForm" label-width="80px">
        <el-form-item label="收货人">
          <el-input v-model="addressForm.receiver" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="addressForm.phone" />
        </el-form-item>
        <el-form-item label="所在地区">
          <el-cascader
            v-model="addressForm.region"
            :options="regionOptions"
            placeholder="请选择省/市/县/乡镇"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="addressForm.detailAddress" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { UserInfo, UserAddress } from '@/types';
import { getUserInfo, getAddresses, setDefaultAddress, deleteAddress, addAddress } from '@/apis/user';

const userInfo = ref<UserInfo | null>(null);
const addressList = ref<UserAddress[]>([]);
const dialogVisible = ref(false);
const addressForm = reactive({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  county: '',
  town: '',
  detailAddress: '',
  region: [] as string[]
});

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
      },
      {
        value: '德阳市',
        label: '德阳市',
        children: [
          {
            value: '中江县',
            label: '中江县',
            children: [
              { value: '凯江镇', label: '凯江镇' },
              { value: '兴隆镇', label: '兴隆镇' }
            ]
          }
        ]
      },
      {
        value: '南充市',
        label: '南充市',
        children: [
          {
            value: '阆中市',
            label: '阆中市',
            children: [
              { value: '七里街道', label: '七里街道' },
              { value: '江南街道', label: '江南街道' }
            ]
          }
        ]
      }
    ]
  }
]

const loadUser = async () => {
  const res = await getUserInfo();
  if (res) userInfo.value = res;
};

const loadAddresses = async () => {
  const res = await getAddresses();
  if (res) addressList.value = res;
};

const setDefault = async (row: UserAddress) => {
  await setDefaultAddress(row.id);
  ElMessage.success('已设为默认');
  loadAddresses();
};

const removeAddress = (row: UserAddress) => {
  ElMessageBox.confirm('确认删除地址吗?', '提示').then(async () => {
    await deleteAddress(row.id);
    ElMessage.success('删除成功');
    loadAddresses();
  });
};

const openDialog = () => {
  dialogVisible.value = true;
};

const saveAddress = async () => {
  if (!addressForm.receiver || !addressForm.phone || addressForm.region.length === 0 || !addressForm.detailAddress) {
    ElMessage.warning('请填写完整信息');
    return;
  }
  addressForm.province = addressForm.region[0] || '';
  addressForm.city = addressForm.region[1] || '';
  addressForm.county = addressForm.region[2] || '';
  addressForm.town = addressForm.region[3] || '';
  await addAddress(addressForm);
  dialogVisible.value = false;
  ElMessage.success('新增成功');
  loadAddresses();
};

onMounted(() => {
  loadUser();
  loadAddresses();
});
</script>

<style scoped>
.profile-container {
  padding: 20px;
}
.profile-card {
  margin-bottom: 20px;
}
.profile-info {
  display: flex;
  align-items: center;
  gap: 20px;
}
.card-header {
  display: flex;
  align-items: center;
}
.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
