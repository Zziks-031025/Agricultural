<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.batchCode" placeholder="搜索批次号" prefix-icon="el-icon-search"
            size="small" clearable style="width: 220px" @keyup.enter.native="handleSearch" />
          <el-select v-model="queryParams.status" placeholder="申报状态" clearable size="small" style="width: 140px" @change="handleSearch">
            <el-option label="待受理" value="pending" />
            <el-option label="已完成" value="completed" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openApplyDialog">新增检疫申报</el-button>
      </div>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.checkResult)" size="mini">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchCode" label="批次号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品名称" min-width="120" show-overflow-tooltip />
        <el-table-column label="数量" width="120">
          <template slot-scope="{ row }">{{ row.quantity || '--' }} {{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column prop="inspectionDate" label="期望检疫日期" width="130" />
        <el-table-column prop="inspector" label="检疫员" width="100">
          <template slot-scope="{ row }">{{ row.inspector || '--' }}</template>
        </el-table-column>
        <el-table-column prop="certNo" label="证书编号" width="140" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.certNo || '--' }}</template>
        </el-table-column>
        <el-table-column label="上链" width="60" align="center">
          <template slot-scope="{ row }">
            <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
              <i class="el-icon-link" style="color: #2d8a56; cursor: pointer" @click="copyHash(row.txHash)"></i>
            </el-tooltip>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.remark || '--' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申报时间" width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" style="color: #F56C6C"
              :disabled="row.checkResult !== null && row.checkResult !== undefined"
              @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="tableData.length === 0 && !loading" class="empty-text">暂无检疫申报记录</div>
    </el-card>

    <!-- Apply Dialog -->
    <el-dialog title="新增检疫申报" :visible.sync="dialogVisible" width="600px" :close-on-click-modal="false">
      <el-form ref="applyForm" :model="form" :rules="rules" label-width="120px" size="small">
        <el-form-item v-if="isAdmin" label="操作企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" filterable placeholder="请选择操作企业" style="width: 100%" @change="onEnterpriseChange">
            <el-option v-for="e in allEnterprises" :key="e.id" :label="e.enterpriseName" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联批次" prop="batchCode">
          <el-select
            v-model="form.batchCode"
            filterable
            placeholder="请选择或输入批次号"
            style="width: 100%"
            @change="onBatchSelect"
          >
            <el-option
              v-for="b in batchOptions"
              :key="b.batchCode || b.id"
              :label="b.batchCode + ' - ' + (b.productName || '')"
              :value="b.batchCode"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="selectedBatch" label="批次信息">
          <el-descriptions :column="2" size="mini" border>
            <el-descriptions-item label="产品名称">{{ selectedBatch.productName || '--' }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ selectedBatch.quantity || selectedBatch.initQuantity || '--' }} {{ selectedBatch.unit || '' }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>

        <el-form-item label="申报数量" prop="applyQuantity">
          <el-input-number v-model="form.applyQuantity" :min="1" :precision="0" style="width: 200px" placeholder="请输入申报数量" />
          <span v-if="form.unit" style="margin-left: 10px; color: #606266">{{ form.unit }}</span>
        </el-form-item>

        <el-form-item label="检疫企业" prop="inspectionEnterpriseId">
          <el-select
            v-model="form.inspectionEnterpriseId"
            filterable
            placeholder="请选择检疫企业"
            style="width: 100%"
          >
            <el-option
              v-for="e in inspectionEnterprises"
              :key="e.id"
              :label="e.enterpriseName"
              :value="e.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="期望检疫日期" prop="expectedDate">
          <el-date-picker
            v-model="form.expectedDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可填写备注信息" />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleApply">提交申报</el-button>
      </div>
    </el-dialog>
    <!-- Detail Dialog -->
    <el-dialog title="检疫申报详情" :visible.sync="detailVisible" width="650px">
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detailRow.checkResult)" size="mini">{{ detailRow.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detailRow.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detailRow.productName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detailRow.quantity || '--' }} {{ detailRow.unit || '' }}</el-descriptions-item>
        <el-descriptions-item label="期望检疫日期">{{ detailRow.inspectionDate || '--' }}</el-descriptions-item>
        <el-descriptions-item label="申报时间">{{ detailRow.createTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="检疫员">{{ detailRow.inspector || '--' }}</el-descriptions-item>
        <el-descriptions-item label="证书编号">{{ detailRow.certNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="区块链哈希" :span="2">
          <span v-if="detailRow.txHash" style="word-break: break-all; cursor: pointer; color: #2d8a56" @click="copyHash(detailRow.txHash)">{{ detailRow.txHash }}</span>
          <span v-else class="no-data">--</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailRow.remark || '--' }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button size="small" @click="detailVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { applyQuarantine, getQuarantineApplyList, deleteQuarantineApply } from '@/api/quarantine'
import { getBatchList } from '@/api/batch'
import { getEnterpriseListByType, getEnterpriseList } from '@/api/enterprise'

export default {
  name: 'QuarantineApply',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      queryParams: {
        batchCode: '',
        status: ''
      },
      dialogVisible: false,
      form: {
        batchCode: '',
        applyQuantity: null,
        expectedDate: '',
        inspectionEnterpriseId: null,
        remark: ''
      },
      rules: {
        enterpriseId: [{ required: true, message: '请选择操作企业', trigger: 'change' }],
        batchCode: [{ required: true, message: '请选择批次', trigger: 'change' }],
        applyQuantity: [{ required: true, message: '请输入申报数量', trigger: 'blur' }],
        inspectionEnterpriseId: [{ required: true, message: '请选择检疫企业', trigger: 'change' }],
        expectedDate: [{ required: true, message: '请选择期望检疫日期', trigger: 'change' }]
      },
      batchOptions: [],
      inspectionEnterprises: [],
      allEnterprises: [],
      selectedBatch: null,
      detailVisible: false,
      detailRow: null
    }
  },
  computed: {
    isAdmin() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.userType === 1
    }
  },
  created() {
    this.loadData()
    this.fetchBatches()
    this.fetchInspectionEnterprises()
    if (this.isAdmin) {
      this.fetchAllEnterprises()
    }
  },
  methods: {
    getEnterpriseId() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.enterpriseId ? userInfo.enterpriseId : null
    },

    loadData() {
      const enterpriseId = this.getEnterpriseId()
      this.loading = true
      const params = {}
      if (enterpriseId) params.enterpriseId = enterpriseId
      if (this.queryParams.batchCode) params.batchCode = this.queryParams.batchCode
      if (this.queryParams.status) params.status = this.queryParams.status

      getQuarantineApplyList(params).then(res => {
        this.tableData = res.data || []
      }).catch(() => {
        this.tableData = []
      }).finally(() => {
        this.loading = false
      })
    },

    handleSearch() {
      this.loadData()
    },

    fetchBatches(enterpriseIdOverride) {
      const enterpriseId = enterpriseIdOverride || this.getEnterpriseId()
      const params = { size: 200, statuses: '1,2,3' }
      if (enterpriseId) params.enterpriseId = enterpriseId
      getBatchList(params).then(res => {
        this.batchOptions = res.data.records || res.data.list || []
      }).catch(() => {})
    },

    onEnterpriseChange(enterpriseId) {
      this.form.batchCode = ''
      this.selectedBatch = null
      this.form.applyQuantity = null
      this.form.unit = ''
      this.batchOptions = []
      if (enterpriseId) {
        this.fetchBatches(enterpriseId)
      }
    },

    fetchInspectionEnterprises() {
      getEnterpriseListByType(3).then(res => {
        this.inspectionEnterprises = res.data || []
      }).catch(() => {})
    },

    fetchAllEnterprises() {
      getEnterpriseList({ size: 200, status: 1 }).then(res => {
        const records = res.data.records || res.data || []
        this.allEnterprises = records.filter(e => e.enterpriseType === 1)
      }).catch(() => {})
    },

    onBatchSelect(batchCode) {
      this.selectedBatch = this.batchOptions.find(b => b.batchCode === batchCode) || null
      if (this.selectedBatch) {
        const qty = this.selectedBatch.quantity || this.selectedBatch.initQuantity || this.selectedBatch.currentQuantity
        if (qty) this.form.applyQuantity = Number(qty)
        this.form.unit = this.selectedBatch.unit || ''
      } else {
        this.form.unit = ''
      }
    },

    openApplyDialog() {
      this.form = {
        batchCode: '',
        applyQuantity: null,
        expectedDate: '',
        inspectionEnterpriseId: null,
        remark: '',
        unit: '',
        enterpriseId: null
      }
      this.selectedBatch = null
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.applyForm && this.$refs.applyForm.clearValidate()
      })
    },

    handleApply() {
      if (!this.isAdmin) {
        this.$refs.applyForm.clearValidate('enterpriseId')
      }
      this.$refs.applyForm.validate(valid => {
        if (!valid) return

        this.$confirm('确认提交检疫申请？提交后请等待检疫机构受理。', '确认提交', {
          type: 'info'
        }).then(() => {
          this.submitting = true
          const submitData = {
            batchCode: this.form.batchCode,
            applyQuantity: this.form.applyQuantity,
            expectedDate: this.form.expectedDate,
            inspectionEnterpriseId: this.form.inspectionEnterpriseId,
            remark: this.form.remark
          }
          if (this.isAdmin) {
            submitData.enterpriseId = this.form.enterpriseId
          } else {
            const enterpriseId = this.getEnterpriseId()
            if (enterpriseId) submitData.enterpriseId = enterpriseId
          }
          const userInfo = this.$store.getters.userInfo
          if (userInfo && userInfo.id) submitData.userId = userInfo.id

          applyQuarantine(submitData).then(() => {
            this.$message.success('检疫申报已提交，请等待检疫机构受理')
            this.dialogVisible = false
            this.loadData()
          }).catch(() => {}).finally(() => {
            this.submitting = false
          })
        }).catch(() => {})
      })
    },

    statusTagType(checkResult) {
      if (checkResult === null || checkResult === undefined) return 'warning'
      if (checkResult === 1) return 'success'
      return 'danger'
    },

    handleDetail(row) {
      this.detailRow = row
      this.detailVisible = true
    },

    handleDelete(row) {
      if (row.checkResult !== null && row.checkResult !== undefined) {
        this.$message.warning('已完成检疫的记录不允许删除')
        return
      }
      this.$confirm('确认删除该检疫申报记录？删除后批次状态将恢复。', '确认删除', {
        type: 'warning'
      }).then(() => {
        deleteQuarantineApply(row.id).then(() => {
          this.$message.success('删除成功')
          this.loadData()
        }).catch(() => {})
      }).catch(() => {})
    },

    copyHash(hash) {
      if (!hash) return
      navigator.clipboard.writeText(hash).then(() => {
        this.$message.success('已复制交易哈希')
      }).catch(() => {
        const input = document.createElement('input')
        input.value = hash
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('已复制交易哈希')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.toolbar-card { margin-bottom: 12px; border-radius: 8px; }
.toolbar {
  display: flex; align-items: center; justify-content: space-between;
}
.toolbar-left {
  display: flex; align-items: center; gap: 8px;
}
.table-card { border-radius: 8px; }
.empty-text {
  text-align: center; padding: 40px 0; color: #909399; font-size: 14px;
}
.no-data { color: #c0c4cc; }
</style>
