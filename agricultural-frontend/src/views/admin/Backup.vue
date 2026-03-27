<template>
  <div class="backup-page">
    <div class="page-header">
      <div>
        <h2>数据备份</h2>
        <p>当前页面管理的是本地 SQL 备份文件。手动备份会立即生成一个新的备份点，恢复会覆盖当前数据。</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadBackups">刷新列表</el-button>
        <el-button type="primary" :icon="Download" @click="handleBackup" :loading="backingUp">
          手动备份
        </el-button>
      </div>
    </div>

    <div class="overview-grid">
      <el-card shadow="never" class="overview-card">
        <span>备份文件数</span>
        <strong>{{ backupList.length }}</strong>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <span>最近备份时间</span>
        <strong class="small">{{ latestBackupTime }}</strong>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <span>累计备份体积</span>
        <strong class="small">{{ totalBackupSizeText }}</strong>
      </el-card>
    </div>

    <el-alert
      title="重要说明"
      type="warning"
      :closable="false"
      show-icon
      description="当前系统运维页已接入手动 SQL 备份、恢复和删除。页面里原先“每天凌晨 2 点自动备份”的描述并不准确，现已改为按实际功能展示。恢复前建议先额外创建一次手动备份。"
    />

    <div class="guide-grid">
      <el-card shadow="never" class="guide-card">
        <h3>手动备份</h3>
        <p>把当前数据库导出成一个新的 SQL 文件，适合做重大改动前的保护点。</p>
      </el-card>
      <el-card shadow="never" class="guide-card">
        <h3>恢复备份</h3>
        <p>会用选中的 SQL 文件覆盖当前数据，适合系统回滚或异常修复，属于高风险操作。</p>
      </el-card>
      <el-card shadow="never" class="guide-card">
        <h3>删除备份</h3>
        <p>只会移除本地备份文件，不影响当前线上数据，但删除后该恢复点也会彻底失效。</p>
      </el-card>
    </div>

    <el-card shadow="never" class="backup-table-card">
      <template #header>
        <div class="table-header">
          <span>备份文件列表</span>
          <span class="table-tip">建议至少保留最近一次稳定版本和最近一次重大操作前的备份。</span>
        </div>
      </template>

      <el-table :data="backupList" style="width: 100%" v-loading="loading">
        <el-table-column prop="fileName" label="备份文件名" min-width="260" show-overflow-tooltip />
        <el-table-column prop="size" label="文件大小" width="120" />
        <el-table-column label="备份类型" width="130">
          <template #default="{ row }">
            <el-tag :type="getBackupTypeTag(row.type)" effect="plain">
              {{ getBackupTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备份时间" width="190">
          <template #default="{ row }">
            {{ row.createTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
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
import { computed, onMounted, ref } from 'vue';
import { Delete, Download, RefreshLeft } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createBackup, deleteBackup, listBackups, restoreBackup } from '@/apis/admin';

const backingUp = ref(false);
const loading = ref(false);
const backupList = ref<any[]>([]);

const parseSizeToBytes = (value?: string) => {
  if (!value) return 0;
  const text = String(value).trim().toUpperCase();
  const num = Number(text.replace(/[A-Z]/g, ''));
  if (Number.isNaN(num)) return 0;
  if (text.endsWith('KB')) return num * 1024;
  if (text.endsWith('MB')) return num * 1024 * 1024;
  if (text.endsWith('GB')) return num * 1024 * 1024 * 1024;
  return num;
};

const formatBytes = (bytes: number) => {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)}KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(2)}MB`;
  return `${(bytes / 1024 / 1024 / 1024).toFixed(2)}GB`;
};

const totalBackupSizeText = computed(() => {
  const totalBytes = backupList.value.reduce((sum, item) => sum + parseSizeToBytes(item.size), 0);
  return formatBytes(totalBytes);
});

const latestBackupTime = computed(() => backupList.value[0]?.createTime || '暂无备份');

const getBackupTypeText = (type?: string) => {
  if (type === 'auto') return '自动备份';
  return '手动备份';
};

const getBackupTypeTag = (type?: string) => {
  if (type === 'auto') return 'success';
  return 'primary';
};

const loadBackups = async () => {
  loading.value = true;
  try {
    const res = await listBackups();
    backupList.value = res || [];
  } finally {
    loading.value = false;
  }
};

const handleBackup = async () => {
  backingUp.value = true;
  try {
    await createBackup();
    ElMessage.success('备份成功');
    await loadBackups();
  } catch (error) {
    console.error(error);
    ElMessage.error('备份失败');
  } finally {
    backingUp.value = false;
  }
};

const handleRestore = async (row: any) => {
  const keyword = row.fileName || 'RESTORE';
  try {
    await ElMessageBox.prompt(
      `恢复会覆盖当前数据库。请输入备份文件名“${keyword}”确认本次恢复。`,
      '高风险操作确认',
      {
        confirmButtonText: '确认恢复',
        cancelButtonText: '取消',
        inputPlaceholder: `请输入 ${keyword}`,
        inputValidator: (value) => value === keyword || '输入的确认内容不正确',
        type: 'warning'
      }
    );
    await restoreBackup(row.id);
    ElMessage.success('数据恢复成功');
    await loadBackups();
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return;
    console.error(error);
  }
};

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定删除备份文件“${row.fileName}”吗？删除后将无法再从该文件恢复。`,
      '删除备份',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    await deleteBackup(row.id);
    ElMessage.success('删除成功');
    await loadBackups();
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return;
    console.error(error);
  }
};

onMounted(() => {
  void loadBackups();
});
</script>

<style scoped lang="scss">
.backup-page {
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
  gap: 10px;
}

.overview-grid,
.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.overview-card,
.guide-card,
.backup-table-card {
  border-radius: 16px;
}

.overview-card {
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
    line-height: 1.2;

    &.small {
      font-size: 18px;
    }
  }
}

.guide-card {
  h3 {
    margin: 0 0 8px;
    font-size: 16px;
    color: #111827;
  }

  p {
    margin: 0;
    color: #6b7280;
    line-height: 1.7;
    font-size: 13px;
  }
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.table-tip {
  font-size: 12px;
  color: #6b7280;
}

@media (max-width: 992px) {
  .page-header,
  .table-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .overview-grid,
  .guide-grid {
    grid-template-columns: 1fr;
  }
}
</style>
