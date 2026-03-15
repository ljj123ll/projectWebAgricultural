<template>
  <div class="backup-page">
    <div class="page-header">
      <h2>数据备份</h2>
      <el-button type="primary" @click="showBackupDialog = true">立即备份</el-button>
    </div>

    <!-- 备份统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="12" :md="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.totalBackups }}</div>
          <div class="stat-label">备份总数</div>
        </el-card>
      </el-col>
      <el-col :span="12" :md="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.lastBackupTime }}</div>
          <div class="stat-label">上次备份</div>
        </el-card>
      </el-col>
      <el-col :span="12" :md="8">
        <el-card class="stat-card">
          <div class="stat-value">{{ stats.storageSize }}</div>
          <div class="stat-label">存储占用</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 自动备份设置 -->
    <el-card class="settings-card">
      <template #header>
        <span>自动备份设置</span>
      </template>
      <el-form :model="backupSettings" label-width="120px">
        <el-form-item label="自动备份">
          <el-switch v-model="backupSettings.autoBackup" />
        </el-form-item>
        <el-form-item label="备份周期">
          <el-select v-model="backupSettings.frequency" style="width: 200px">
            <el-option label="每天" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
          </el-select>
        </el-form-item>
        <el-form-item label="备份时间">
          <el-time-picker v-model="backupSettings.time" format="HH:mm" />
        </el-form-item>
        <el-form-item label="保留份数">
          <el-input-number v-model="backupSettings.keepCount" :min="1" :max="30" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveSettings">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 备份记录 -->
    <el-card>
      <template #header>
        <span>备份记录</span>
      </template>
      <el-table :data="backupList" style="width: 100%">
        <el-table-column prop="id" label="备份ID" width="100" />
        <el-table-column prop="name" label="备份名称" />
        <el-table-column prop="size" label="大小" width="100" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'auto' ? 'info' : 'success'" size="small">
              {{ row.type === 'auto' ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="备份时间" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="downloadBackup(row)">下载</el-button>
            <el-button link type="success" @click="restoreBackup(row)">恢复</el-button>
            <el-button link type="danger" @click="deleteBackup(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
        />
      </div>
    </el-card>

    <!-- 备份弹窗 -->
    <el-dialog v-model="showBackupDialog" title="数据备份" width="90%">
      <el-form :model="backupForm" label-position="top">
        <el-form-item label="备份名称">
          <el-input v-model="backupForm.name" placeholder="请输入备份名称" />
        </el-form-item>
        <el-form-item label="备份内容">
          <el-checkbox-group v-model="backupForm.content">
            <el-checkbox label="database">数据库</el-checkbox>
            <el-checkbox label="files">文件</el-checkbox>
            <el-checkbox label="logs">日志</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="backupForm.remark"
            type="textarea"
            rows="3"
            placeholder="可选：输入备份备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBackupDialog = false">取消</el-button>
        <el-button type="primary" @click="startBackup">开始备份</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(50)
const showBackupDialog = ref(false)

const stats = reactive({
  totalBackups: 28,
  lastBackupTime: '2024-03-08 02:00:00',
  storageSize: '15.6GB'
})

const backupSettings = reactive({
  autoBackup: true,
  frequency: 'daily',
  time: new Date(2024, 0, 1, 2, 0),
  keepCount: 7
})

const backupForm = reactive({
  name: '',
  content: ['database', 'files'],
  remark: ''
})

const backupList = ref([
  {
    id: 'BK20240308020000',
    name: '自动备份-20240308',
    size: '520MB',
    type: 'auto',
    createTime: '2024-03-08 02:00:00',
    status: 'success'
  },
  {
    id: 'BK20240307020000',
    name: '自动备份-20240307',
    size: '518MB',
    type: 'auto',
    createTime: '2024-03-07 02:00:00',
    status: 'success'
  },
  {
    id: 'BK20240306100000',
    name: '手动备份-更新前',
    size: '515MB',
    type: 'manual',
    createTime: '2024-03-06 10:00:00',
    status: 'success'
  }
])

const saveSettings = () => {
  ElMessage.success('设置已保存')
}

const startBackup = () => {
  if (!backupForm.name) {
    ElMessage.warning('请输入备份名称')
    return
  }
  ElMessage.success('备份已开始，请稍后查看')
  showBackupDialog.value = false
}

const downloadBackup = (row: any) => {
  void row
  ElMessage.success('开始下载')
}

const restoreBackup = (row: any) => {
  void row
  ElMessageBox.confirm('确定要恢复到该备份点吗？此操作不可撤销！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('恢复成功')
  })
}

const deleteBackup = (row: any) => {
  ElMessageBox.confirm('确定要删除该备份吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    backupList.value = backupList.value.filter(item => item.id !== row.id)
    ElMessage.success('删除成功')
  })
}
</script>

<style scoped lang="scss">
.backup-page {
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  margin-bottom: 16px;

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
  }

  .stat-label {
    color: #666;
    font-size: 14px;
  }
}

.settings-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
