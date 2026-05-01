<template>
  <div class="page-container">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryParams" size="small">
        <el-form-item label="审核状态">
          <el-select v-model="queryParams.auditStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业类型">
          <el-select v-model="queryParams.enterpriseType" placeholder="全部" clearable style="width: 140px">
            <el-option label="种植养殖" :value="1" />
            <el-option label="加工屠宰" :value="2" />
            <el-option label="检疫质检" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="企业名称/编号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="enterpriseCode" label="企业编号" width="180" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enterpriseType" label="企业类型" width="120" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="typeTagMap[row.enterpriseType]" size="small">
              {{ typeTextMap[row.enterpriseType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="legalPerson" label="法人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column prop="auditStatus" label="审核状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagMap[row.auditStatus]" size="small">
              {{ statusTextMap[row.auditStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="190" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleAudit(row)">
              {{ row.auditStatus === 0 ? '审核' : '查看' }}
            </el-button>
            <el-button
              v-if="row.auditStatus === 2"
              type="text"
              size="small"
              style="color: #f56c6c"
              @click="handleDeleteRejected(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-sizes="[10, 20, 50]"
        :page-size.sync="queryParams.size"
        :current-page.sync="queryParams.current"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <el-dialog
      :title="currentRow && currentRow.auditStatus === 0 ? '企业审核' : '企业详情'"
      :visible.sync="dialogVisible"
      width="720px"
      top="5vh"
      destroy-on-close
    >
      <div v-if="currentRow" class="audit-content">
        <el-descriptions :column="2" border size="small" class="info-section">
          <el-descriptions-item label="企业编号">{{ currentRow.enterpriseCode }}</el-descriptions-item>
          <el-descriptions-item label="企业名称">{{ currentRow.enterpriseName }}</el-descriptions-item>
          <el-descriptions-item label="企业类型">{{ typeTextMap[currentRow.enterpriseType] }}</el-descriptions-item>
          <el-descriptions-item label="法人代表">{{ currentRow.legalPerson }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentRow.contactPerson }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentRow.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱">{{ currentRow.contactEmail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所在地区">
            {{ [currentRow.province, currentRow.city, currentRow.district].filter(Boolean).join(' / ') || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="详细地址" :span="2">{{ currentRow.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="企业简介" :span="2">{{ currentRow.introduction || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="license-section">
          <h4>营业执照</h4>
          <div v-if="currentRow.businessLicense" class="license-preview">
            <el-image
              :src="currentRow.businessLicense"
              :preview-src-list="[currentRow.businessLicense]"
              fit="contain"
              style="max-width: 100%; max-height: 300px"
            >
              <div slot="error" class="image-error">
                <i class="el-icon-picture-outline"></i>
                <span>图片加载失败</span>
              </div>
            </el-image>
          </div>
          <div v-else class="no-license">暂未上传营业执照</div>
        </div>

        <div v-if="currentRow.otherCertificates" class="license-section">
          <h4>其他资质证明</h4>
          <div class="license-preview">
            <el-image
              v-for="(url, idx) in parseCertificates(currentRow.otherCertificates)"
              :key="idx"
              :src="url"
              :preview-src-list="parseCertificates(currentRow.otherCertificates)"
              fit="cover"
              style="width: 120px; height: 120px; margin-right: 8px; border-radius: 4px"
            />
          </div>
        </div>

        <div v-if="currentRow.auditStatus === 0" class="audit-form-section">
          <el-divider content-position="left">审核意见</el-divider>
          <el-form label-width="80px" size="small">
            <el-form-item label="审核备注">
              <el-input
                v-model="auditRemark"
                type="textarea"
                :rows="3"
                placeholder="请输入审核意见（驳回时必填）"
              />
            </el-form-item>
          </el-form>
        </div>

        <div v-else class="audit-result-section">
          <el-divider content-position="left">审核结果</el-divider>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="审核状态">
              <el-tag :type="statusTagMap[currentRow.auditStatus]" size="small">
                {{ statusTextMap[currentRow.auditStatus] }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ currentRow.auditTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核备注" :span="2">{{ currentRow.auditRemark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <span v-if="currentRow && currentRow.auditStatus === 0" slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleReject">驳回</el-button>
        <el-button type="primary" :loading="submitting" @click="handleApprove">通过</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getAuditList, approveEnterprise, deleteRejectedEnterprise } from '@/api/enterprise'

export default {
  name: 'EnterpriseAudit',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        auditStatus: null,
        enterpriseType: null,
        keyword: ''
      },
      dialogVisible: false,
      currentRow: null,
      auditRemark: '',
      typeTextMap: { 1: '种植养殖', 2: '加工屠宰', 3: '检疫质检' },
      typeTagMap: { 1: 'success', 2: 'warning', 3: '' },
      statusTextMap: { 0: '待审核', 1: '已通过', 2: '已驳回' },
      statusTagMap: { 0: 'warning', 1: 'success', 2: 'danger' }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (!params.keyword) delete params.keyword
      if (params.enterpriseType === null) delete params.enterpriseType
      if (params.auditStatus === null) delete params.auditStatus

      getAuditList(params).then(res => {
        this.tableData = res.data.records || res.data.list || []
        this.total = res.data.total || 0
      }).catch(() => {
        this.tableData = []
        this.total = 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.current = 1
      this.fetchList()
    },
    resetQuery() {
      this.queryParams = { current: 1, size: 10, auditStatus: null, enterpriseType: null, keyword: '' }
      this.fetchList()
    },
    handleAudit(row) {
      this.currentRow = { ...row }
      this.auditRemark = ''
      this.dialogVisible = true
    },
    handleApprove() {
      this.submitAudit(1)
    },
    handleReject() {
      if (!this.auditRemark.trim()) {
        this.$message.warning('驳回时请填写审核备注')
        return
      }
      this.submitAudit(2)
    },
    submitAudit(auditStatus) {
      this.submitting = true
      approveEnterprise({
        enterpriseId: this.currentRow.id,
        auditStatus,
        auditRemark: this.auditRemark
      }).then(() => {
        this.$message.success(auditStatus === 1 ? '已通过审核' : '已驳回')
        this.dialogVisible = false
        this.fetchList()
      }).catch(() => {}).finally(() => {
        this.submitting = false
      })
    },
    handleDeleteRejected(row) {
      this.$confirm(`确定删除驳回申请“${row.enterpriseName}”吗？删除后原注册账号可重新申请。`, '提示', {
        type: 'warning'
      }).then(() => {
        deleteRejectedEnterprise(row.id).then(() => {
          this.$message.success('驳回申请已删除')
          this.fetchList()
        })
      }).catch(() => {})
    },
    parseCertificates(val) {
      if (!val) return []
      try {
        return JSON.parse(val)
      } catch {
        return [val]
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.filter-card { margin-bottom: 16px; }

.table-card {
  .pagination { margin-top: 16px; text-align: right; }
  overflow-x: auto;
}

.audit-content {
  .info-section { margin-bottom: 20px; }

  .license-section {
    margin-bottom: 20px;

    h4 { font-size: 14px; color: #303133; margin-bottom: 12px; }

    .license-preview {
      background: #fafafa;
      padding: 16px;
      border-radius: 4px;
      text-align: center;
    }

    .no-license {
      color: #909399;
      font-size: 13px;
      padding: 20px 0;
      text-align: center;
      background: #fafafa;
      border-radius: 4px;
    }
  }

  .image-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 200px;
    color: #909399;

    i { font-size: 40px; margin-bottom: 8px; }
  }

  .audit-form-section { margin-top: 16px; }
  .audit-result-section { margin-top: 16px; }
}
</style>
