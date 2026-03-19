<template>
  <div class="backup-page">
    <div class="page-header">
      <h2>数据备份</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Download" @click="handleBackup" :loading="backingUp">
          手动备份
        </el-button>
      </div>
    </div>

    <el-alert
      title="数据备份说明"
      type="info"
      description="系统每天凌晨 2:00 自动进行全量数据备份。手动备份将立即创建一个新的备份点。"
      show-icon
      class="mb-20"
    />

    <el-card>
      <el-table :data="backupList" style="width: 100%" v-loading="loading">
        <el-table-column prop="fileName" label="备份文件名" min-width="200" />
        <el-table-column prop="size" label="文件大小" width="120" />
        <el-table-column prop="type" label="备份类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.type === 'auto' ? 'success' : 'primary'">
              {{ row.type === 'auto' ? '自动备份' : '手动备份' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="备份时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button 
              link 
              type="warning" 
              :icon="RefreshLeft"
              @click="handleRestore(row)"
              :disabled="row.status !== 1"
            >
              恢复
            </el-button>
            <el-button 
              link 
              type="danger" 
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Download, RefreshLeft, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listBackups, createBackup, restoreBackup, deleteBackup } from '@/apis/admin'

const backingUp = ref(false)
const loading = ref(false)
const backupList = ref<any[]>([])

const loadBackups = async () => {
  loading.value = true;
  try {
    const res = await listBackups();
    if (res) {
      backupList.value = res;
    }
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadBackups();
})

const handleBackup = async () => {
  try {
    backingUp.value = true;
    await createBackup();
    ElMessage.success('备份成功');
    loadBackups();
  } catch (error) {
    console.error(error);
    ElMessage.error('备份失败');
  } finally {
    backingUp.value = false;
  }
}

const handleRestore = (row: any) => {
  ElMessageBox.confirm(
    '数据恢复将覆盖当前所有数据，且此操作不可逆！强烈建议在恢复前先进行一次手动备份。确定要继续吗？',
    '高风险操作警告',
    {
      confirmButtonText: '确认恢复',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await restoreBackup(row.id);
      ElMessage.success('数据恢复成功');
    } catch (error) {
      console.error(error);
    }
  }).catch(() => {})
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除这个备份文件吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteBackup(row.id);
      ElMessage.success('删除成功');
      loadBackups();
    } catch (error) {
      console.error(error);
    }
  }).catch(() => {})
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.mb-20 {
  margin-bottom: 20px;
}
</style>
