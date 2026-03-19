<template>
  <div class="users-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户手机号/姓名"
        style="width: 250px"
        clearable
        @clear="loadUsers"
      >
        <template #append>
          <el-button :icon="Search" @click="loadUsers" />
        </template>
      </el-input>
    </div>

    <el-card>
      <el-table :data="userList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="createTime" label="注册时间" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              link 
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadUsers"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listUsers, updateUserStatus } from '@/apis/admin'

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const userList = ref<any[]>([])

const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await listUsers({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      // 假设后端支持搜索参数，如果不支持则由后端决定
      keyword: searchKeyword.value || undefined
    });
    if (res) {
      userList.value = res.list || [];
      total.value = res.total || 0;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadUsers();
})

const toggleStatus = async (row: any) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1;
    await updateUserStatus(row.id, newStatus);
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用');
    loadUsers();
  } catch (error) {
    console.error(error);
  }
}
</script>

<style scoped lang="scss">
.users-page {
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
</style>
