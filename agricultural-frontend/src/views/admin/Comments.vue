<template>
  <div class="comments-page">
    <div class="page-header">
      <h2>评论管理</h2>
      <el-radio-group v-model="filterStatus" size="small">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="normal">正常</el-radio-button>
        <el-radio-button label="reported">被举报</el-radio-button>
        <el-radio-button label="hidden">已隐藏</el-radio-button>
      </el-radio-group>
    </div>

    <el-card>
      <el-table :data="commentList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="productName" label="商品" min-width="150" />
        <el-table-column prop="content" label="评论内容" min-width="200">
          <template #default="{ row }">
            <div class="comment-content">
              <el-rate v-model="row.rating" disabled />
              <p>{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'hidden'" link type="danger" @click="hideComment(row)">
              隐藏
            </el-button>
            <el-button v-else link type="success" @click="showComment(row)">
              显示
            </el-button>
            <el-button link type="primary" @click="replyComment(row)">回复</el-button>
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

    <!-- 回复弹窗 -->
    <el-dialog v-model="showReplyDialog" title="回复评论" width="90%">
      <div class="reply-content">
        <div class="original-comment">
          <h4>原评论</h4>
          <p>{{ currentComment.content }}</p>
        </div>
        <el-input
          v-model="replyContent"
          type="textarea"
          rows="4"
          placeholder="请输入回复内容"
        />
      </div>
      <template #footer>
        <el-button @click="showReplyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const filterStatus = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)
const showReplyDialog = ref(false)
const replyContent = ref('')
const currentComment = reactive<any>({})

const commentList = ref([
  {
    id: 1,
    userName: '张三',
    productName: '新鲜有机西红柿',
    content: '西红柿很新鲜，口感很好，下次还会购买！',
    rating: 5,
    createTime: '2024-03-08 14:30:00',
    status: 'normal'
  },
  {
    id: 2,
    userName: '李四',
    productName: '山东红富士苹果',
    content: '苹果个头有点小，不太满意',
    rating: 3,
    createTime: '2024-03-07 10:20:00',
    status: 'reported'
  }
])

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    normal: 'success',
    reported: 'warning',
    hidden: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    normal: '正常',
    reported: '被举报',
    hidden: '已隐藏'
  }
  return map[status] || status
}

const hideComment = (row: any) => {
  row.status = 'hidden'
  ElMessage.success('已隐藏')
}

const showComment = (row: any) => {
  row.status = 'normal'
  ElMessage.success('已显示')
}

const replyComment = (row: any) => {
  Object.assign(currentComment, row)
  showReplyDialog.value = true
}

const submitReply = () => {
  if (!replyContent.value) {
    ElMessage.warning('请输入回复内容')
    return
  }
  ElMessage.success('回复成功')
  showReplyDialog.value = false
  replyContent.value = ''
}
</script>

<style scoped lang="scss">
.comments-page {
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

.comment-content {
  p {
    margin: 8px 0 0 0;
    color: #666;
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.reply-content {
  .original-comment {
    background: #f5f5f5;
    padding: 16px;
    border-radius: 8px;
    margin-bottom: 16px;

    h4 {
      margin: 0 0 8px 0;
    }

    p {
      margin: 0;
      color: #666;
    }
  }
}
</style>
