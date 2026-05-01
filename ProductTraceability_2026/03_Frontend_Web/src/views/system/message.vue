<template>
  <div class="message-container">
    <!-- 顶部标签页 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane name="all">
        <span slot="label">
          全部消息
          <el-badge v-if="unreadCount.all > 0" :value="unreadCount.all" :max="99" class="tab-badge" />
        </span>
      </el-tab-pane>
      <el-tab-pane name="system">
        <span slot="label">
          <i class="el-icon-message-solid"></i> 系统通知
          <el-badge v-if="unreadCount.system > 0" :value="unreadCount.system" :max="99" class="tab-badge" />
        </span>
      </el-tab-pane>
      <el-tab-pane name="business">
        <span slot="label">
          <i class="el-icon-bell"></i> 业务提醒
          <el-badge v-if="unreadCount.business > 0" :value="unreadCount.business" :max="99" class="tab-badge" />
        </span>
      </el-tab-pane>
    </el-tabs>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button size="small" type="primary" :disabled="selectedIds.length === 0" @click="handleBatchRead">
        <i class="el-icon-check"></i> 标记已读
      </el-button>
      <el-button size="small" type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
        <i class="el-icon-delete"></i> 删除
      </el-button>
      <el-button size="small" @click="handleReadAll">
        <i class="el-icon-finished"></i> 全部已读
      </el-button>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" v-loading="loading">
      <template v-if="messageList.length > 0">
        <div
          v-for="item in messageList"
          :key="item.id"
          class="message-item"
          :class="{ 'unread': item.isRead === 0 }"
          @click="handleDetail(item)"
        >
          <el-checkbox
            :value="selectedIds.includes(item.id)"
            @change="toggleSelect(item.id)"
            @click.native.stop
            class="message-checkbox"
          />
          <div class="message-icon">
            <i :class="item.type === 'system' ? 'el-icon-message-solid' : 'el-icon-bell'" />
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="message-title">
                <el-tag v-if="item.isRead === 0" size="mini" type="danger" effect="dark" class="unread-tag">未读</el-tag>
                {{ item.title }}
              </span>
              <span class="message-time">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="message-summary">{{ item.summary }}</div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无消息" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="currentPage"
        @current-change="loadMessages"
      />
    </div>

    <!-- 消息详情弹窗 -->
    <el-dialog
      :title="detailData.title || '消息详情'"
      :visible.sync="detailVisible"
      width="600px"
      append-to-body
    >
      <div class="detail-meta">
        <el-tag size="small" :type="detailData.type === 'system' ? '' : 'warning'">
          {{ detailData.type === 'system' ? '系统通知' : '业务提醒' }}
        </el-tag>
        <span class="detail-time">{{ formatTime(detailData.createTime) }}</span>
      </div>
      <div class="detail-content">
        <pre class="detail-text">{{ detailData.content }}</pre>
      </div>
      <span slot="footer">
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getMessageList, getUnreadCount, markAsRead, deleteMessages } from '@/api/message'

export default {
  name: 'MessageCenter',
  data() {
    return {
      activeTab: 'all',
      loading: false,
      messageList: [],
      total: 0,
      currentPage: 1,
      pageSize: 20,
      selectedIds: [],
      unreadCount: { all: 0, system: 0, business: 0 },
      detailVisible: false,
      detailData: {}
    }
  },
  created() {
    this.loadMessages()
    this.loadUnreadCount()
  },
  methods: {
    async loadMessages() {
      this.loading = true
      try {
        const params = {
          current: this.currentPage,
          size: this.pageSize
        }
        if (this.activeTab !== 'all') {
          params.type = this.activeTab
        }
        const res = await getMessageList(params)
        const page = res.data || {}
        this.messageList = page.records || []
        this.total = page.total || 0
      } catch (e) {
        console.error('加载消息失败', e)
      } finally {
        this.loading = false
      }
    },
    async loadUnreadCount() {
      try {
        const res = await getUnreadCount()
        this.unreadCount = res.data || { all: 0, system: 0, business: 0 }
      } catch (e) {
        console.error('加载未读数量失败', e)
      }
    },
    handleTabClick() {
      this.currentPage = 1
      this.selectedIds = []
      this.loadMessages()
    },
    toggleSelect(id) {
      const idx = this.selectedIds.indexOf(id)
      if (idx > -1) {
        this.selectedIds.splice(idx, 1)
      } else {
        this.selectedIds.push(id)
      }
    },
    async handleBatchRead() {
      if (this.selectedIds.length === 0) return
      try {
        await markAsRead(this.selectedIds)
        this.$message.success('已标记为已读')
        this.selectedIds = []
        this.loadMessages()
        this.loadUnreadCount()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async handleReadAll() {
      const unreadIds = this.messageList.filter(m => m.isRead === 0).map(m => m.id)
      if (unreadIds.length === 0) {
        this.$message.info('没有未读消息')
        return
      }
      try {
        await markAsRead(unreadIds)
        this.$message.success('已全部标记为已读')
        this.selectedIds = []
        this.loadMessages()
        this.loadUnreadCount()
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async handleBatchDelete() {
      if (this.selectedIds.length === 0) return
      try {
        await this.$confirm('确认删除所选消息？', '提示', { type: 'warning' })
        await deleteMessages(this.selectedIds)
        this.$message.success('删除成功')
        this.selectedIds = []
        this.loadMessages()
        this.loadUnreadCount()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('删除失败')
        }
      }
    },
    async handleDetail(item) {
      this.detailData = item
      this.detailVisible = true
      // 自动标记已读
      if (item.isRead === 0) {
        try {
          await markAsRead([item.id])
          item.isRead = 1
          this.loadUnreadCount()
        } catch (e) {
          // ignore
        }
      }
    },
    formatTime(time) {
      if (!time) return ''
      if (typeof time === 'string') return time.replace('T', ' ').substring(0, 19)
      return time
    }
  }
}
</script>

<style lang="scss" scoped>
.message-container {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  min-height: calc(100vh - 130px);
}

.tab-badge {
  margin-left: 4px;
  vertical-align: middle;
}

.action-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.message-list {
  min-height: 300px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 12px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f9fafb;
  }

  &.unread {
    background: #f0f9eb;

    &:hover {
      background: #e8f5e0;
    }

    .message-title {
      font-weight: 600;
    }
  }
}

.message-checkbox {
  margin-top: 4px;
  flex-shrink: 0;
}

.message-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #2d8a56, #1a6b3a);
  color: #fff;
  font-size: 16px;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.message-title {
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

.unread-tag {
  flex-shrink: 0;
}

.message-time {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 12px;
}

.message-summary {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-time {
  font-size: 13px;
  color: #909399;
}

.detail-content {
  padding: 8px 0;
}

.detail-text {
  font-family: inherit;
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
