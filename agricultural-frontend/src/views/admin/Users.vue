<template>
  <div class="users-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户手机号/姓名"
          style="width: 250px"
          clearable
          @clear="loadUsers"
          @keyup.enter="loadUsers"
        >
          <template #append>
            <el-button :icon="Search" @click="loadUsers" />
          </template>
        </el-input>
        <el-button type="primary" @click="openCreateDialog">新增用户</el-button>
      </div>
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              link 
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="removeUser(row)">删除</el-button>
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

    <el-dialog v-model="showCreateDialog" title="新增用户" width="460px" destroy-on-close>
      <el-form label-width="92px">
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" maxlength="11" placeholder="请输入11位手机号" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" maxlength="20" placeholder="可选，不填自动生成" />
        </el-form-item>
        <el-form-item label="登录密码">
          <el-input
            v-model="createForm.password"
            type="password"
            show-password
            maxlength="30"
            placeholder="至少6位，可用于密码登录"
          />
        </el-form-item>
        <el-form-item>
          <el-alert title="短信登录仍可用；设置密码后可使用“密码登录”。" type="info" :closable="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreateUser">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, deleteUser, listUsers, updateUserStatus } from '@/apis/admin'

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const showCreateDialog = ref(false)
const createSubmitting = ref(false)
const createForm = reactive({
  phone: '',
  nickname: '',
  password: ''
})

const userList = ref<any[]>([])

const loadUsers = async () => {
  loading.value = true;
  try {
    const res = await listUsers({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
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

const openCreateDialog = () => {
  createForm.phone = ''
  createForm.nickname = ''
  createForm.password = ''
  showCreateDialog.value = true
}

const submitCreateUser = async () => {
  const phone = createForm.phone.trim()
  const nickname = createForm.nickname.trim()
  const password = createForm.password.trim()
  if (!/^1\d{10}$/.test(phone)) {
    ElMessage.warning('请输入有效的11位手机号')
    return
  }
  if (password && password.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }
  createSubmitting.value = true
  try {
    await createUser({
      phone,
      nickname: nickname || undefined,
      password: password || undefined,
      status: 1
    })
    ElMessage.success('用户创建成功')
    showCreateDialog.value = false
    currentPage.value = 1
    await loadUsers()
  } finally {
    createSubmitting.value = false
  }
}

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

const removeUser = (row: any) => {
  ElMessageBox.confirm(`确认删除用户「${row.nickname || row.phone || row.id}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      if (userList.value.length === 1 && currentPage.value > 1) {
        currentPage.value -= 1
      }
      await loadUsers()
    })
    .catch(() => {})
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
