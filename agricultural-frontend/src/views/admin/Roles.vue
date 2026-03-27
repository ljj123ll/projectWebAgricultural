<template>
  <div class="roles-page">
    <div class="page-header">
      <div>
        <h2>角色与权限</h2>
        <p>这里决定管理员能看到哪些后台模块、能执行哪些敏感操作。建议先选角色，再按分组逐项配置权限。</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadData">刷新</el-button>
        <el-button type="primary" :disabled="!activeRoleId" :loading="saving" @click="saveRolePermissions">
          保存权限
        </el-button>
      </div>
    </div>

    <div class="overview-grid">
      <el-card shadow="never" class="overview-card">
        <span>角色数量</span>
        <strong>{{ roles.length }}</strong>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <span>权限节点</span>
        <strong>{{ permissions.length }}</strong>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <span>当前已选权限</span>
        <strong>{{ checkedPermIds.length }}</strong>
      </el-card>
    </div>

    <el-row :gutter="16">
      <el-col :xl="7" :lg="8" :md="9" :sm="24" :xs="24">
        <el-card class="role-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span>角色列表</span>
              <el-tag type="info" effect="plain">{{ roles.length }} 个角色</el-tag>
            </div>
          </template>

          <el-table
            ref="roleTableRef"
            :data="roles"
            v-loading="loadingRoles"
            highlight-current-row
            style="width: 100%"
            row-key="id"
            @current-change="handleRoleChange"
          >
            <el-table-column label="角色" min-width="180">
              <template #default="{ row }">
                <div class="role-cell">
                  <strong>{{ row.roleName }}</strong>
                  <span>{{ row.roleCode }}</span>
                  <p>{{ row.description || '暂无角色说明' }}</p>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xl="17" :lg="16" :md="15" :sm="24" :xs="24">
        <el-card v-loading="loadingPerms" class="permission-card" shadow="never">
          <template #header>
            <div class="perm-header">
              <div>
                <span>权限配置</span>
                <p v-if="activeRoleName">{{ activeRoleName }}：{{ activeRoleDescription }}</p>
              </div>
              <el-tag v-if="activeRoleName" type="success" effect="plain">
                当前角色：{{ activeRoleName }}
              </el-tag>
            </div>
          </template>

          <el-empty v-if="!activeRoleId" description="请选择左侧角色后配置权限" :image-size="90" />

          <div v-else class="permission-body">
            <div class="toolbar">
              <el-input
                v-model="permissionKeyword"
                clearable
                placeholder="搜索权限名称或编码"
                class="toolbar-search"
              />
              <el-checkbox v-model="onlyChecked">仅看已勾选</el-checkbox>
              <el-button @click="selectAllVisible">全选当前结果</el-button>
              <el-button @click="clearVisible">清空当前结果</el-button>
            </div>

            <el-empty
              v-if="!filteredGroups.length"
              description="没有匹配的权限项，请调整搜索条件"
              :image-size="70"
            />

            <el-checkbox-group v-else v-model="checkedPermIds" class="perm-groups">
              <div v-for="group in filteredGroups" :key="group.title" class="perm-group">
                <div class="group-head">
                  <div>
                    <div class="group-title">{{ group.title }}</div>
                    <div class="group-meta">
                      共 {{ group.items.length }} 项，已选 {{ getCheckedCount(group.items) }} 项
                    </div>
                  </div>
                  <div class="group-actions">
                    <el-button text type="primary" @click="checkGroup(group.items)">本组选中</el-button>
                    <el-button text @click="uncheckGroup(group.items)">本组清空</el-button>
                  </div>
                </div>
                <div class="perm-items">
                  <el-checkbox
                    v-for="perm in group.items"
                    :key="perm.id"
                    :label="perm.id"
                    border
                    class="perm-item"
                  >
                    <div class="perm-text">
                      <strong>{{ perm.permName }}</strong>
                      <span>{{ perm.permCode }}</span>
                    </div>
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
import { computed, nextTick, onMounted, ref } from 'vue';
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
const permissionKeyword = ref('');
const onlyChecked = ref(false);
const roleTableRef = ref();

const activeRole = computed(() => roles.value.find(item => Number(item.id) === Number(activeRoleId.value)) || null);
const activeRoleName = computed(() => activeRole.value?.roleName || '');
const activeRoleDescription = computed(() => activeRole.value?.description || '配置该角色可访问的后台模块与操作权限');

const groupedPermissions = computed(() => {
  const perms = permissions.value || [];
  const byId = new Map<number, SysPermission>();
  perms.forEach(item => byId.set(Number(item.id), item));

  const map = new Map<string, SysPermission[]>();
  perms.forEach(item => {
    const parent = item.parentId && item.parentId > 0 ? byId.get(Number(item.parentId)) : undefined;
    const groupName = parent?.permName || '默认分组';
    const bucket = map.get(groupName) || [];
    bucket.push(item);
    map.set(groupName, bucket);
  });

  return [...map.entries()].map(([title, items]) => ({
    title,
    items: items.sort((a, b) => Number(a.id) - Number(b.id))
  }));
});

const filteredGroups = computed(() => {
  const keyword = permissionKeyword.value.trim().toLowerCase();
  return groupedPermissions.value
    .map(group => {
      let items = group.items;
      if (keyword) {
        items = items.filter(item =>
          String(item.permName || '').toLowerCase().includes(keyword)
          || String(item.permCode || '').toLowerCase().includes(keyword)
        );
      }
      if (onlyChecked.value) {
        items = items.filter(item => checkedPermIds.value.includes(Number(item.id)));
      }
      return { ...group, items };
    })
    .filter(group => group.items.length > 0);
});

const getCheckedCount = (items: SysPermission[]) => {
  const ids = items.map(item => Number(item.id));
  return ids.filter(id => checkedPermIds.value.includes(id)).length;
};

const loadRolePermissions = async (roleId: number) => {
  loadingPerms.value = true;
  try {
    const res = await getRolePermissionIds(roleId);
    checkedPermIds.value = (res || []).map(item => Number(item));
  } finally {
    loadingPerms.value = false;
  }
};

const syncCurrentRole = async () => {
  await nextTick();
  const current = roles.value.find(item => Number(item.id) === Number(activeRoleId.value));
  if (current) {
    roleTableRef.value?.setCurrentRow?.(current);
  }
};

const loadData = async () => {
  loadingRoles.value = true;
  try {
    const [roleRes, permRes] = await Promise.all([listRoles(), listPermissions()]);
    roles.value = roleRes || [];
    permissions.value = permRes || [];

    const firstRole = roles.value[0];
    if (!activeRoleId.value && firstRole?.id) {
      activeRoleId.value = Number(firstRole.id);
    }
    if (activeRoleId.value) {
      await loadRolePermissions(activeRoleId.value);
    }
    await syncCurrentRole();
  } finally {
    loadingRoles.value = false;
  }
};

const handleRoleChange = async (row?: SysRole) => {
  if (!row?.id) return;
  activeRoleId.value = Number(row.id);
  await loadRolePermissions(activeRoleId.value);
};

const checkGroup = (items: SysPermission[]) => {
  const merged = new Set(checkedPermIds.value.map(id => Number(id)));
  items.forEach(item => merged.add(Number(item.id)));
  checkedPermIds.value = [...merged];
};

const uncheckGroup = (items: SysPermission[]) => {
  const removeSet = new Set(items.map(item => Number(item.id)));
  checkedPermIds.value = checkedPermIds.value.filter(id => !removeSet.has(Number(id)));
};

const selectAllVisible = () => {
  const merged = new Set(checkedPermIds.value.map(id => Number(id)));
  filteredGroups.value.forEach(group => group.items.forEach(item => merged.add(Number(item.id))));
  checkedPermIds.value = [...merged];
};

const clearVisible = () => {
  const removeSet = new Set<number>();
  filteredGroups.value.forEach(group => group.items.forEach(item => removeSet.add(Number(item.id))));
  checkedPermIds.value = checkedPermIds.value.filter(id => !removeSet.has(Number(id)));
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
.roles-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 0 0 6px;
    font-size: 24px;
    color: #111827;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 14px;
  }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  border-radius: 14px;

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  span {
    color: #6b7280;
    font-size: 13px;
  }

  strong {
    color: #0f172a;
    font-size: 28px;
    line-height: 1;
  }
}

.role-card,
.permission-card {
  border-radius: 16px;
}

.card-head,
.perm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.perm-header {
  align-items: flex-start;

  p {
    margin: 6px 0 0;
    color: #6b7280;
    font-size: 13px;
  }
}

.role-cell {
  display: grid;
  gap: 4px;
  padding: 2px 0;

  strong {
    color: #111827;
  }

  span {
    color: #3b82f6;
    font-size: 12px;
  }

  p {
    margin: 0;
    color: #6b7280;
    font-size: 12px;
    line-height: 1.5;
  }
}

.permission-body {
  display: grid;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-search {
  width: 280px;
}

.perm-groups {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.perm-group {
  border: 1px solid #e8edf5;
  border-radius: 14px;
  padding: 14px;
  background: #fbfcff;
}

.group-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.group-title {
  font-size: 15px;
  color: #1f2937;
  font-weight: 700;
}

.group-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.group-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.perm-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.perm-item {
  margin-right: 0;
  min-width: 220px;
}

.perm-text {
  display: grid;
  gap: 2px;

  strong {
    font-size: 13px;
    color: #111827;
    font-weight: 600;
  }

  span {
    font-size: 12px;
    color: #6b7280;
  }
}

@media (max-width: 992px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .page-header,
  .card-head,
  .perm-header,
  .group-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-search {
    width: 100%;
  }

  .perm-item {
    width: 100%;
    min-width: 0;
  }
}
</style>
