<template>
  <div class="roles-page">
    <div class="page-header">
      <h2>角色与权限</h2>
      <div class="header-actions">
        <el-button @click="loadData">刷新</el-button>
        <el-button type="primary" :disabled="!activeRoleId" :loading="saving" @click="saveRolePermissions">
          保存权限
        </el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header>角色列表</template>
          <el-table
            :data="roles"
            v-loading="loadingRoles"
            highlight-current-row
            style="width: 100%"
            @current-change="handleRoleChange"
            row-key="id"
          >
            <el-table-column prop="roleName" label="角色名" min-width="120" />
            <el-table-column prop="roleCode" label="角色编码" min-width="120" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card v-loading="loadingPerms">
          <template #header>
            <div class="perm-header">
              <span>权限配置</span>
              <el-tag v-if="activeRoleName" type="info">当前角色：{{ activeRoleName }}</el-tag>
            </div>
          </template>
          <el-empty v-if="!activeRoleId" description="请选择左侧角色后配置权限" :image-size="80" />
          <div v-else>
            <el-checkbox-group v-model="checkedPermIds" class="perm-groups">
              <div v-for="group in groupedPermissions" :key="group.title" class="perm-group">
                <div class="group-title">{{ group.title }}</div>
                <div class="perm-items">
                  <el-checkbox
                    v-for="perm in group.items"
                    :key="perm.id"
                    :label="perm.id"
                    border
                  >
                    {{ perm.permName }}（{{ perm.permCode }}）
                  </el-checkbox>
                </div>
              </div>
            </el-checkbox-group>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getRolePermissionIds, listPermissions, listRoles, updateRolePermissionIds } from '@/apis/admin';
import type { SysPermission, SysRole } from '@/types';

const loadingRoles = ref(false);
const loadingPerms = ref(false);
const saving = ref(false);

const roles = ref<SysRole[]>([]);
const permissions = ref<SysPermission[]>([]);
const activeRoleId = ref<number | null>(null);
const checkedPermIds = ref<number[]>([]);

const activeRoleName = computed(() => {
  const role = roles.value.find(item => item.id === activeRoleId.value);
  return role?.roleName || '';
});

const groupedPermissions = computed(() => {
  const perms = permissions.value || [];
  const byId = new Map<number, SysPermission>();
  perms.forEach(item => byId.set(item.id, item));

  const map = new Map<string, SysPermission[]>();
  perms.forEach(item => {
    const parent = item.parentId && item.parentId > 0 ? byId.get(item.parentId) : undefined;
    const groupName = parent?.permName || '默认分组';
    const bucket = map.get(groupName) || [];
    bucket.push(item);
    map.set(groupName, bucket);
  });

  return [...map.entries()].map(([title, items]) => ({
    title,
    items: items.sort((a, b) => a.id - b.id)
  }));
});

const loadRolePermissions = async (roleId: number) => {
  loadingPerms.value = true;
  try {
    const res = await getRolePermissionIds(roleId);
    checkedPermIds.value = (res || []).map(item => Number(item));
  } finally {
    loadingPerms.value = false;
  }
};

const loadData = async () => {
  loadingRoles.value = true;
  try {
    const [roleRes, permRes] = await Promise.all([
      listRoles(),
      listPermissions()
    ]);
    roles.value = roleRes || [];
    permissions.value = permRes || [];

    if (!activeRoleId.value && roles.value.length && roles.value[0]) {
      activeRoleId.value = Number(roles.value[0].id);
    }
    if (activeRoleId.value) {
      await loadRolePermissions(activeRoleId.value);
    }
  } finally {
    loadingRoles.value = false;
  }
};

const handleRoleChange = async (row?: SysRole) => {
  if (!row?.id) return;
  activeRoleId.value = Number(row.id);
  await loadRolePermissions(activeRoleId.value);
};

const saveRolePermissions = async () => {
  if (!activeRoleId.value) return;
  saving.value = true;
  try {
    await updateRolePermissionIds(activeRoleId.value, checkedPermIds.value.map(id => Number(id)));
    ElMessage.success('角色权限已更新');
    await loadRolePermissions(activeRoleId.value);
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  void loadData();
});
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.perm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.perm-groups {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.perm-group {
  border: 1px solid #eef2f8;
  border-radius: 10px;
  padding: 12px;

  .group-title {
    font-size: 14px;
    color: #1f2937;
    font-weight: 600;
    margin-bottom: 10px;
  }
}

.perm-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
