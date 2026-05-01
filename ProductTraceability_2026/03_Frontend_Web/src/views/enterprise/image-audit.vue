<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane name="business">
        <span slot="label">业务图片</span>
      </el-tab-pane>
      <el-tab-pane name="qualification">
        <span slot="label" class="tab-label">
          资质图片审核
          <el-badge v-if="qualificationPendingCount > 0" is-dot class="tab-dot" />
        </span>
      </el-tab-pane>
    </el-tabs>

    <template v-if="activeTab === 'business'">
      <el-card shadow="never" class="table-card">
        <el-table
          v-loading="bizLoading"
          :data="bizTableData"
          border
          size="mini"
          style="width: 100%"
          :row-style="{ height: '42px' }"
          :cell-style="{ padding: '2px 0' }"
        >
          <el-table-column prop="sourceLabel" label="来源类型" width="80" />
          <el-table-column prop="enterpriseName" label="企业名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="batchCode" label="批次编号" min-width="140" show-overflow-tooltip />
          <el-table-column label="图片" width="60" align="center">
            <template slot-scope="{ row }">
              <el-image :src="resolveUrl(row.imageUrl)" :preview-src-list="[resolveUrl(row.imageUrl)]" fit="cover" class="thumb-img" />
            </template>
          </el-table-column>
          <el-table-column label="上传时间" width="145" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template slot-scope="{ row }">
              <el-button type="text" size="mini" style="color: #e6a23c" @click="handleBizViolation(row)">违规</el-button>
              <el-button type="text" size="mini" style="color: #f56c6c" @click="handleBizDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="bizTotal"
            :page-size="bizQuery.size"
            :current-page="bizQuery.current"
            :page-sizes="[20, 50, 100]"
            @current-change="val => { bizQuery.current = val; fetchBizImages() }"
            @size-change="val => { bizQuery.size = val; bizQuery.current = 1; fetchBizImages() }"
          />
        </div>
      </el-card>
    </template>

    <template v-if="activeTab === 'qualification'">
      <el-card shadow="never" class="toolbar-card">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-select
              v-model="queryParams.auditStatus"
              placeholder="审核状态"
              clearable
              size="small"
              style="width: 140px"
              @change="handleSearch"
            >
              <el-option :value="0" label="待审核" />
              <el-option :value="1" label="已通过" />
              <el-option :value="2" label="已拒绝" />
            </el-select>
            <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="table-card">
        <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
          <el-table-column prop="enterpriseName" label="企业名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="fieldLabel" label="变更类型" width="120" />
          <el-table-column label="申请人" width="120">
            <template slot-scope="{ row }">{{ row.realName || row.username || '--' }}</template>
          </el-table-column>
          <el-table-column label="旧图片" width="100" align="center">
            <template slot-scope="{ row }">
              <el-image v-if="row.oldValue" :src="resolveUrl(row.oldValue)" :preview-src-list="[resolveUrl(row.oldValue)]" fit="cover" class="thumb-img" />
              <span v-else class="no-data">无</span>
            </template>
          </el-table-column>
          <el-table-column label="新图片" width="100" align="center">
            <template slot-scope="{ row }">
              <el-image v-if="row.newValue" :src="resolveUrl(row.newValue)" :preview-src-list="[resolveUrl(row.newValue)]" fit="cover" class="thumb-img" />
              <span v-else class="no-data">无</span>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="statusType(row.auditStatus)" size="mini">{{ statusText(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="160" show-overflow-tooltip />
          <el-table-column prop="auditRemark" label="审核备注" min-width="120" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ row.auditRemark || '--' }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="220" align="center" fixed="right">
            <template slot-scope="{ row }">
              <template v-if="row.auditStatus === 0">
                <el-button type="text" size="mini" style="color: #67c23a" @click="handleApprove(row, true)">通过</el-button>
                <el-button type="text" size="mini" style="color: #f56c6c" @click="handleApprove(row, false)">拒绝</el-button>
              </template>
              <el-button type="text" size="mini" style="color: #e6a23c" @click="handleQualViolation(row)">违规</el-button>
              <el-button type="text" size="mini" style="color: #f56c6c" @click="handleQualDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :page-size="queryParams.size"
            :current-page="queryParams.current"
            :page-sizes="[10, 20, 50]"
            @current-change="val => { queryParams.current = val; fetchList() }"
            @size-change="val => { queryParams.size = val; queryParams.current = 1; fetchList() }"
          />
        </div>
      </el-card>
    </template>

    <el-dialog title="拒绝原因" :visible.sync="rejectVisible" width="450px" append-to-body>
      <el-form label-width="80px" size="small">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectRemark" type="textarea" :rows="3" placeholder="请输入拒绝原因，将通知给企业" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="rejectVisible = false">取消</el-button>
        <el-button size="small" type="danger" :loading="approving" @click="confirmReject">确认拒绝</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getImageAuditList, approveImageAudit, getQualificationPendingCount } from '@/api/enterprise'
import request from '@/utils/request'

export default {
  name: 'ImageAudit',
  data() {
    return {
      activeTab: 'business',
      bizLoading: false,
      bizTableData: [],
      bizTotal: 0,
      bizQuery: { current: 1, size: 20 },
      loading: false,
      approving: false,
      tableData: [],
      total: 0,
      qualificationPendingCount: 0,
      queryParams: { current: 1, size: 10, auditStatus: null },
      rejectVisible: false,
      rejectRemark: '',
      pendingRejectRow: null
    }
  },
  created() {
    this.fetchBizImages()
    this.fetchQualificationPendingCount()
  },
  methods: {
    handleTabChange(tab) {
      if (tab.name === 'business' && this.bizTableData.length === 0) this.fetchBizImages()
      if (tab.name === 'qualification' && this.tableData.length === 0) this.fetchList()
      this.fetchQualificationPendingCount()
    },
    fetchBizImages() {
      this.bizLoading = true
      request({ url: '/api/image-audit/business-images', method: 'get', params: this.bizQuery })
        .then(res => {
          this.bizTableData = res.data.records || []
          this.bizTotal = res.data.total || 0
        })
        .catch(() => { this.bizTableData = [] })
        .finally(() => { this.bizLoading = false })
    },
    fetchList() {
      this.loading = true
      getImageAuditList(this.queryParams)
        .then(res => {
          this.tableData = res.data.records || []
          this.total = res.data.total || 0
        })
        .catch(() => { this.tableData = [] })
        .finally(() => { this.loading = false })
    },
    fetchQualificationPendingCount() {
      getQualificationPendingCount()
        .then(res => {
          this.qualificationPendingCount = res.data || 0
        })
        .catch(() => {
          this.qualificationPendingCount = 0
        })
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchList()
    },
    formatTime(val) {
      if (!val) return '--'
      if (typeof val === 'string') return val.replace('T', ' ').substring(0, 19)
      return val
    },
    statusType(s) {
      if (s === 0) return 'warning'
      if (s === 1) return 'success'
      if (s === 3) return 'info'
      return 'danger'
    },
    statusText(s) {
      if (s === 0) return '待审核'
      if (s === 1) return '已通过'
      if (s === 3) return '违规'
      return '已拒绝'
    },
    resolveUrl(path) {
      if (!path) return ''
      if (path.startsWith('http')) return path
      return process.env.VUE_APP_BASE_API + path.replace(/^\/api/, '')
    },
    handleApprove(row, approved) {
      if (approved) {
        this.$confirm('确认通过该图片变更申请？', '确认通过', { type: 'success' })
          .then(() => { this.doApprove(row.id, true, '') })
          .catch(() => {})
      } else {
        this.pendingRejectRow = row
        this.rejectRemark = ''
        this.rejectVisible = true
      }
    },
    confirmReject() {
      if (!this.rejectRemark.trim()) {
        this.$message.warning('请输入拒绝原因')
        return
      }
      this.doApprove(this.pendingRejectRow.id, false, this.rejectRemark)
    },
    doApprove(auditId, approved, remark) {
      this.approving = true
      approveImageAudit({ auditId, approved, remark })
        .then(() => {
          this.$message.success(approved ? '已通过' : '已拒绝')
          this.rejectVisible = false
          this.fetchList()
          this.fetchQualificationPendingCount()
        })
        .catch(() => {})
        .finally(() => { this.approving = false })
    },
    handleBizViolation(row) {
      this.$confirm('确认标记该图片为违规？将从业务记录中移除并通知企业。', '违规确认', { type: 'warning' })
        .then(() => {
          request({
            url: '/api/image-audit/business-violation',
            method: 'post',
            data: { sourceType: row.sourceType, sourceId: row.sourceId, imageUrl: row.imageUrl }
          }).then(() => {
            this.$message.success('已标记违规')
            this.fetchBizImages()
          })
        })
        .catch(() => {})
    },
    handleBizDelete(row) {
      this.$confirm('确认删除该图片？', '删除确认', { type: 'warning' })
        .then(() => {
          request({
            url: '/api/image-audit/business-delete',
            method: 'post',
            data: { sourceType: row.sourceType, sourceId: row.sourceId, imageUrl: row.imageUrl }
          }).then(() => {
            this.$message.success('已删除')
            this.fetchBizImages()
          })
        })
        .catch(() => {})
    },
    handleQualViolation(row) {
      this.$confirm('确认标记该图片为违规？将通知企业重新上传。', '违规确认', { type: 'warning' })
        .then(() => {
          request({
            url: '/api/image-audit/qualification-violation',
            method: 'post',
            data: { auditId: row.id }
          }).then(() => {
            this.$message.success('已标记违规')
            this.fetchList()
            this.fetchQualificationPendingCount()
          })
        })
        .catch(() => {})
    },
    handleQualDelete(row) {
      this.$confirm('确认删除该审核记录？', '删除确认', { type: 'warning' })
        .then(() => {
          request({
            url: '/api/image-audit/qualification-delete',
            method: 'post',
            data: { auditId: row.id }
          }).then(() => {
            this.$message.success('已删除')
            this.fetchList()
            this.fetchQualificationPendingCount()
          })
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.tab-label { display: inline-flex; align-items: center; gap: 6px; }
.tab-dot { line-height: 1; }
.toolbar-card { margin-bottom: 12px; border-radius: 8px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.toolbar-left { display: flex; align-items: center; gap: 8px; }
.table-card { border-radius: 8px; margin-top: 8px; overflow-x: auto; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.thumb-img { width: 36px; height: 36px; border-radius: 4px; cursor: pointer; }
.no-data { color: #c0c4cc; font-size: 12px; }
</style>
