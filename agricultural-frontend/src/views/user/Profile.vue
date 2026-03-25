<template>
  <div class="profile-container">
    <el-row :gutter="16" class="profile-row">
      <el-col :xs="24" :lg="12">
        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <el-button link @click="$router.back()" class="back-btn">
                <el-icon><ArrowLeft /></el-icon>
              </el-button>
              <span>个人信息</span>
            </div>
          </template>

          <el-form label-width="86px" class="profile-form">
            <el-form-item label="头像">
              <div class="avatar-box">
                <el-avatar :size="72" :src="avatarPreview" />
                <el-upload
                  :action="uploadAvatarAction"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :before-upload="beforeAvatarUpload"
                  :on-success="handleAvatarUploadSuccess"
                >
                  <el-button type="primary" plain>上传头像</el-button>
                </el-upload>
              </div>
            </el-form-item>

            <el-form-item label="用户名">
              <el-input
                v-model="profileForm.nickname"
                maxlength="20"
                show-word-limit
                placeholder="请输入用户名"
              />
            </el-form-item>

            <el-form-item label="手机号">
              <el-input :model-value="userInfo?.phone || ''" disabled />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">
                保存个人信息
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="profile-card">
          <template #header>
            <span>修改密码</span>
          </template>

          <el-form label-width="86px" class="profile-form">
            <el-form-item label="新密码">
              <el-input
                v-model="passwordForm.password"
                type="password"
                show-password
                placeholder="请输入新密码（至少6位）"
              />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :loading="passwordSaving" @click="savePassword">
                更新密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="address-card">
      <template #header>
        <div class="address-header">
          <span>收货地址</span>
          <el-button size="small" type="primary" @click="openDialog">新增地址</el-button>
        </div>
      </template>

      <el-table :data="addressList" style="width: 100%">
        <el-table-column prop="receiver" label="收货人" min-width="90" />
        <el-table-column prop="phone" label="电话" min-width="120" />
        <el-table-column label="地址" min-width="260">
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

    <el-dialog v-model="dialogVisible" title="新增地址" width="520px">
      <el-form :model="addressForm" label-width="88px">
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
            :props="cascaderProps"
            clearable
            filterable
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
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { UploadProps } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { regionData } from 'element-china-area-data';
import type { UserInfo, UserAddress, RegionNode } from '@/types';
import {
  getUserInfo,
  updateUserInfo,
  getAddresses,
  setDefaultAddress,
  deleteAddress,
  addAddress,
  getRegionTree
} from '@/apis/user';
import { useUserStore } from '@/stores/modules/user';
import { getFullImageUrl } from '@/utils/image';

const userStore = useUserStore();

const userInfo = ref<UserInfo | null>(null);
const profileSaving = ref(false);
const passwordSaving = ref(false);
const addressList = ref<UserAddress[]>([]);
const regionOptions = ref<RegionNode[]>([]);
const dialogVisible = ref(false);

const profileForm = reactive({
  nickname: '',
  avatarUrl: ''
});

const passwordForm = reactive({
  password: '',
  confirmPassword: ''
});

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

const uploadAvatarAction = `${import.meta.env.VITE_API_BASE_URL || '/api'}/common/upload/avatar`;
const uploadHeaders = computed(() => {
  const headers: Record<string, string> = {};
  if (userStore.token) {
    headers.Authorization = `Bearer ${userStore.token}`;
  }
  return headers;
});

const avatarPreview = computed(() => getFullImageUrl(profileForm.avatarUrl || userInfo.value?.avatarUrl || ''));
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: true,
  checkStrictly: true
};

const normalizeRegionNodes = (nodes: any[]): RegionNode[] => {
  return (nodes || []).map((node) => ({
    value: String(node?.value ?? node?.label ?? ''),
    label: String(node?.label ?? node?.value ?? ''),
    children: normalizeRegionNodes(node?.children || [])
  }));
};

const updateStoreUserInfo = (nextInfo: UserInfo) => {
  userStore.userInfo = nextInfo;
  if (sessionStorage.getItem('token')) {
    sessionStorage.setItem('userInfo', JSON.stringify(nextInfo));
  }
  if (localStorage.getItem('token')) {
    localStorage.setItem('userInfo', JSON.stringify(nextInfo));
  }
};

const loadUser = async () => {
  const res = await getUserInfo();
  if (res) {
    userInfo.value = res;
    profileForm.nickname = res.nickname || '';
    profileForm.avatarUrl = res.avatarUrl || '';
    updateStoreUserInfo(res);
  }
};

const loadRegionOptions = async () => {
  try {
    const list = await getRegionTree();
    const normalized = normalizeRegionNodes((list || []) as any[]);
    if (!normalized.length) {
      throw new Error('地区数据为空');
    }
    regionOptions.value = normalized;
  } catch (error) {
    // 兜底：接口失败时使用本地地区数据，避免新增地址无法选择地区
    regionOptions.value = normalizeRegionNodes(regionData as any[]);
  }
};

const loadAddresses = async () => {
  const res = await getAddresses();
  if (res) {
    addressList.value = res;
  }
};

const saveProfile = async () => {
  const nickname = profileForm.nickname.trim();
  if (!nickname) {
    ElMessage.warning('用户名不能为空');
    return;
  }

  profileSaving.value = true;
  try {
    await updateUserInfo({
      nickname,
      avatarUrl: profileForm.avatarUrl
    });
    const nextInfo: UserInfo = {
      ...(userInfo.value as UserInfo),
      nickname,
      avatarUrl: profileForm.avatarUrl
    };
    userInfo.value = nextInfo;
    updateStoreUserInfo(nextInfo);
    ElMessage.success('个人信息已更新');
  } finally {
    profileSaving.value = false;
  }
};

const savePassword = async () => {
  const password = passwordForm.password.trim();
  const confirmPassword = passwordForm.confirmPassword.trim();

  if (password.length < 6) {
    ElMessage.warning('密码至少需要6位');
    return;
  }
  if (password !== confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return;
  }

  passwordSaving.value = true;
  try {
    await updateUserInfo({ password });
    passwordForm.password = '';
    passwordForm.confirmPassword = '';
    ElMessage.success('密码修改成功');
  } finally {
    passwordSaving.value = false;
  }
};

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(rawFile.type);
  if (!isImage) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WEBP 格式');
    return false;
  }
  const isLt5M = rawFile.size / 1024 / 1024 < 5;
  if (!isLt5M) {
    ElMessage.error('头像大小不能超过 5MB');
    return false;
  }
  return true;
};

const handleAvatarUploadSuccess: UploadProps['onSuccess'] = async (response: any) => {
  const url = response?.data?.url;
  if (!url) {
    ElMessage.error(response?.msg || '头像上传失败');
    return;
  }

  profileForm.avatarUrl = url;
  try {
    await updateUserInfo({ avatarUrl: url });
    if (userInfo.value) {
      const nextInfo = { ...userInfo.value, avatarUrl: url };
      userInfo.value = nextInfo;
      updateStoreUserInfo(nextInfo);
    }
    ElMessage.success('头像已更新');
  } catch (error) {
    ElMessage.error('头像保存失败，请重试');
  }
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

const resetAddressForm = () => {
  addressForm.receiver = '';
  addressForm.phone = '';
  addressForm.province = '';
  addressForm.city = '';
  addressForm.county = '';
  addressForm.town = '';
  addressForm.detailAddress = '';
  addressForm.region = [];
};

const openDialog = () => {
  resetAddressForm();
  dialogVisible.value = true;
};

const saveAddress = async () => {
  if (!addressForm.receiver || !addressForm.phone || addressForm.region.length < 3 || !addressForm.detailAddress) {
    ElMessage.warning('请完整选择到区/县并填写详细地址');
    return;
  }

  addressForm.province = addressForm.region[0] || '';
  addressForm.city = addressForm.region[1] || '';
  addressForm.county = addressForm.region[2] || '';
  addressForm.town = addressForm.region[3] || '';

  await addAddress({
    receiver: addressForm.receiver,
    phone: addressForm.phone,
    province: addressForm.province,
    city: addressForm.city,
    county: addressForm.county,
    town: addressForm.town,
    detailAddress: addressForm.detailAddress
  });

  dialogVisible.value = false;
  ElMessage.success('新增成功');
  loadAddresses();
};

onMounted(() => {
  loadUser();
  loadAddresses();
  loadRegionOptions();
});
</script>

<style scoped lang="scss">
.profile-container {
  padding: 16px;
}

.profile-row {
  margin-bottom: 16px;
}

.profile-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn {
  font-size: 18px;
}

.profile-form {
  padding-right: 6px;
}

.avatar-box {
  display: flex;
  align-items: center;
  gap: 14px;
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 10px;
  }
}
</style>
