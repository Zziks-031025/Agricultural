<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.keyword" placeholder="搜索批次号/产品名称" prefix-icon="el-icon-search"
            size="small" clearable style="width: 240px" @keyup.enter.native="handleSearch" />
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
      </div>
    </el-card>

    <!-- Batch Table -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border size="small" style="width: 100%">
        <el-table-column prop="batchCode" label="批次号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品名称" min-width="90" />
        <el-table-column label="数量" min-width="100">
          <template slot-scope="{ row }">{{ row.quantity || 0 }} {{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column label="来源企业" min-width="140">
          <template slot-scope="{ row }">{{ row.enterpriseName || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.status === 7" type="warning" size="mini">待接收</el-tag>
            <el-tag v-else-if="row.status === 4" type="" size="mini">已接收</el-tag>
            <el-tag v-else :type="statusTagType(row.status)" size="mini">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接收人" min-width="90">
          <template slot-scope="{ row }">{{ row.receiver || '--' }}</template>
        </el-table-column>
        <el-table-column label="接收日期" min-width="110">
          <template slot-scope="{ row }">{{ row.receiveDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="上链" min-width="80" align="center">
          <template slot-scope="{ row }">
            <span v-if="row.txHash" style="color:#2d8a56">已上链</span>
            <span v-else class="no-chain">未上链</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" align="center" fixed="right">
          <template slot-scope="{ row }">
            <template v-if="row.status === 7">
              <el-button type="text" size="mini" icon="el-icon-view" @click="viewDetail(row)">详情</el-button>
              <el-button type="text" size="mini" icon="el-icon-check" style="color:#67C23A" @click="handleConfirmReceive(row)">确认接收</el-button>
              <el-button type="text" size="mini" icon="el-icon-close" style="color:#F56C6C" @click="handleReject(row)">拒绝</el-button>
            </template>
            <template v-else>
              <el-button type="text" size="mini" icon="el-icon-view" @click="viewDetail(row)">详情</el-button>
              <el-button v-if="row.status === 4" type="text" size="mini" icon="el-icon-refresh-left" style="color:#F56C6C" @click="handleCancelReceive(row)">取消接收</el-button>
            </template>
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

    <!-- Detail Dialog -->
    <el-dialog title="批次详情" :visible.sync="detailDialogVisible" width="720px">
      <div v-if="detailData" class="detail-wrapper">
        <div class="detail-section-title">基础信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="批次号">{{ detailData.batchCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.batchStatus)" size="mini">{{ statusText(detailData.batchStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ detailData.productName }}</el-descriptions-item>
          <el-descriptions-item label="品种">{{ detailData.breed || '--' }}</el-descriptions-item>
          <el-descriptions-item label="初始数量">{{ detailData.initQuantity || '--' }} {{ detailData.unit || '' }}</el-descriptions-item>
          <el-descriptions-item label="当前数量">{{ detailData.currentQuantity || '--' }} {{ detailData.unit || '' }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detailData.manager || '--' }}</el-descriptions-item>
          <el-descriptions-item label="来源企业">{{ detailData.enterpriseName || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">产地信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="产地位置" :span="2">{{ detailData.originLocation || '--' }}</el-descriptions-item>
          <el-descriptions-item label="生产日期">{{ detailData.productionDate || '--' }}</el-descriptions-item>
          <el-descriptions-item label="预计出栏">{{ detailData.expectedHarvestDate || '--' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="detailData.inspectionResult != null">
          <div class="detail-section-title">检疫信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="检疫结果">
              <el-tag :type="detailData.inspectionResult === 1 ? 'success' : 'danger'" size="mini">
                {{ detailData.inspectionResult === 1 ? '合格' : '不合格' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="检疫日期">{{ detailData.inspectionDate || '--' }}</el-descriptions-item>
            <el-descriptions-item label="检疫员">{{ detailData.inspector || '--' }}</el-descriptions-item>
            <el-descriptions-item label="证书编号">{{ detailData.certificateNo || '--' }}</el-descriptions-item>
          </el-descriptions>
        </template>

        <div class="detail-section-title">区块链存证</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="TxHash">
            <span v-if="detailData.txHash" class="tx-hash" @click="copyHash(detailData.txHash)">{{ detailData.txHash }}</span>
            <span v-else class="no-chain">未上链</span>
          </el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ detailData.blockNumber || '--' }}</el-descriptions-item>
          <el-descriptions-item label="上链时间">{{ detailData.chainTime || '--' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- Receive Confirm Dialog -->
    <el-dialog title="确认接收入库" :visible.sync="receiveDialogVisible" width="480px" :close-on-click-modal="false">
      <el-form ref="receiveForm" :model="receiveForm" :rules="receiveRules" label-width="90px" size="medium">
        <el-form-item label="批次号">
          <el-input :value="receiveForm.batchCode" disabled />
        </el-form-item>
        <el-form-item label="产品信息">
          <span>{{ receiveForm.productName }}</span>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="接收企业">
          <el-input :value="receiveForm.enterpriseName || '自动识别中...'" disabled />
        </el-form-item>
        <el-form-item label="接收数量" prop="receiveQuantity">
          <el-input-number v-model="receiveForm.receiveQuantity" :min="0.01" :precision="2" :step="1" style="width: 200px" />
          <span style="margin-left:8px;color:#909399">{{ receiveForm.unit }}</span>
        </el-form-item>
        <el-form-item label="接收人" prop="receiver">
          <el-input v-model="receiveForm.receiver" placeholder="接收人姓名" style="width: 260px" />
        </el-form-item>
        <el-form-item label="接收日期" prop="receiveDate">
          <el-date-picker v-model="receiveForm.receiveDate" type="date" placeholder="选择接收日期" value-format="yyyy-MM-dd" style="width: 260px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="receiveForm.remark" type="textarea" :rows="3" placeholder="选填" style="width: 340px" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="receiveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="receiving" @click="submitReceive">提交接收</el-button>
      </div>
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog title="拒绝接收" :visible.sync="rejectDialogVisible" width="480px" :close-on-click-modal="false">
      <el-form ref="rejectForm" :model="rejectForm" :rules="rejectRules" label-width="90px" size="medium">
        <el-form-item label="批次号">
          <el-input :value="rejectForm.batchCode" disabled />
        </el-form-item>
        <el-form-item label="拒绝原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" placeholder="请填写拒绝原因（将通知养殖企业）" style="width: 340px" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="submitReject">确认拒绝</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getBatchList, getBatchDetail, receiveBatch, rejectBatch } from '@/api/batch'
import { getEnterpriseListByType } from '@/api/enterprise'
import request from '@/utils/request'

const STATUS_MAP = {
  1: { text: '初始化', type: '' },
  2: { text: '生长中', type: 'success' },
  3: { text: '已收获', type: 'warning' },
  4: { text: '加工中', type: '' },
  5: { text: '已检疫', type: 'success' },
  6: { text: '已入库', type: '' },
  7: { text: '运输中', type: 'warning' },
  8: { text: '已销售', type: 'info' },
  9: { text: '加工完成', type: 'success' }
}

export default {
  name: 'ProcessingMaterial',
  data() {
    return {
      loading: false,
      receiving: false,
      rejecting: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        keyword: ''
      },
      // Detail
      detailDialogVisible: false,
      detailData: null,
      // Receive
      receiveDialogVisible: false,
      receiveForm: {
        batchId: null,
        batchCode: '',
        productName: '',
        unit: '',
        receiveQuantity: null,
        receiver: '',
        receiveDate: '',
        remark: '',
        enterpriseId: null
      },
      receiveRules: {
        receiveQuantity: [{ required: true, message: '请输入接收数量', trigger: 'blur' }],
        receiver: [{ required: true, message: '请输入接收人', trigger: 'blur' }],
        receiveDate: [{ required: true, message: '请选择接收日期', trigger: 'change' }],
        enterpriseId: [{ required: true, message: '请选择接收企业', trigger: 'change' }]
      },
      // Reject
      rejectDialogVisible: false,
      rejectForm: {
        batchId: null,
        batchCode: '',
        reason: ''
      },
      rejectRules: {
        reason: [{ required: true, message: '请填写拒绝原因', trigger: 'blur' }]
      },
      processingEnterprises: []
    }
  },
  computed: {
    isAdmin() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.userType === 1
    }
  },
  created() {
    this.fetchList()
    if (this.isAdmin) {
      this.fetchProcessingEnterprises()
    }
  },
  methods: {
    fetchProcessingEnterprises() {
      getEnterpriseListByType(2).then(res => {
        this.processingEnterprises = res.data || []
      }).catch(() => {})
    },
    fetchList() {
      this.loading = true
      const userInfo = this.$store.getters.userInfo
      const enterpriseId = userInfo && userInfo.enterpriseId
      const params = {
        current: this.queryParams.current,
        size: this.queryParams.size,
        keyword: this.queryParams.keyword || undefined,
        statuses: '4,5,6,7,9'
      }
      if (enterpriseId) {
        params.enterpriseId = enterpriseId
        params.enterpriseType = userInfo.enterpriseType
      }
      getBatchList(params).then(res => {
        this.tableData = (res.data.records || []).map(item => ({
          batchId: item.id,
          batchCode: item.batchCode,
          productName: item.productName || '',
          quantity: item.quantity || item.currentQuantity,
          unit: item.unit,
          enterpriseName: item.enterpriseName || '',
          receiver: item.receiver || '',
          receiveDate: item.receiveDate || '',
          txHash: item.txHash,
          status: item.status
        }))
        this.total = res.data.total || 0
      }).catch(() => {}).finally(() => { this.loading = false })
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchList()
    },
    viewDetail(row) {
      this.detailData = null
      this.detailDialogVisible = true
      getBatchDetail({ batchCode: row.batchCode }).then(res => {
        this.detailData = res.data
      }).catch(() => { this.detailDialogVisible = false })
    },
    handleConfirmReceive(row) {
      const userInfo = this.$store.getters.userInfo
      const today = new Date().toISOString().substring(0, 10)
      this.receiveForm = {
        batchId: row.batchId,
        batchCode: row.batchCode,
        productName: row.productName,
        unit: row.unit || '',
        receiveQuantity: row.quantity,
        receiver: userInfo && userInfo.realName || '',
        receiveDate: today,
        remark: '',
        enterpriseId: null,
        enterpriseName: ''
      }
      this.receiveDialogVisible = true
      // Admin: auto-fetch receiveEnterpriseId from transport record
      if (this.isAdmin) {
        request({
          url: '/api/transport/list',
          method: 'get',
          params: { batchId: row.batchId, pageNum: 1, pageSize: 1 }
        }).then(res => {
          const records = res.data.records || res.data.list || []
          if (records.length > 0 && records[0].receiveEnterpriseId) {
            this.receiveForm.enterpriseId = records[0].receiveEnterpriseId
            // Find enterprise name for display
            const ent = this.processingEnterprises.find(e => e.id === records[0].receiveEnterpriseId)
            this.receiveForm.enterpriseName = ent ? ent.enterpriseName : ''
          }
        }).catch(() => {})
      }
    },
    submitReceive() {
      this.$refs.receiveForm.clearValidate('enterpriseId')
      this.$refs.receiveForm.validate(valid => {
        if (!valid) return
        this.receiving = true
        const userInfo = this.$store.getters.userInfo
        const enterpriseId = this.isAdmin ? this.receiveForm.enterpriseId : (userInfo && userInfo.enterpriseId)
        receiveBatch({
          batchCode: this.receiveForm.batchCode,
          receiveQuantity: this.receiveForm.receiveQuantity,
          receiver: this.receiveForm.receiver,
          enterpriseId: enterpriseId,
          receiveDate: this.receiveForm.receiveDate,
          remark: this.receiveForm.remark
        }).then(() => {
          this.$message.success('接收成功')
          this.receiveDialogVisible = false
          this.fetchList()
        }).catch(() => {}).finally(() => { this.receiving = false })
      })
    },
    handleReject(row) {
      this.rejectForm = {
        batchId: row.batchId,
        batchCode: row.batchCode,
        reason: ''
      }
      this.rejectDialogVisible = true
    },
    submitReject() {
      this.$refs.rejectForm.validate(valid => {
        if (!valid) return
        this.rejecting = true
        const userInfo = this.$store.getters.userInfo
        rejectBatch(this.rejectForm.batchId, {
          reason: this.rejectForm.reason,
          enterpriseId: userInfo && userInfo.enterpriseId
        }).then(() => {
          this.$message.success('已拒绝接收，已通知养殖企业')
          this.rejectDialogVisible = false
          this.fetchList()
        }).catch(() => {}).finally(() => { this.rejecting = false })
      })
    },
    handleCancelReceive(row) {
      this.$confirm('确认取消接收该批次？批次状态将恢复为待接收', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        request({
          url: '/api/batch/cancel-receive/' + row.batchId,
          method: 'post'
        }).then(() => {
          this.$message.success('已取消接收')
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    },
    statusText(status) {
      return STATUS_MAP[status]?.text || '未知'
    },
    statusTagType(status) {
      return STATUS_MAP[status]?.type || ''
    },
    copyHash(hash) {
      navigator.clipboard.writeText(hash).then(() => {
        this.$message.success('已复制到剪贴板')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;

.page-container { padding: 4px; }

.toolbar-card {
  border-radius: 8px;
  margin-bottom: 16px;
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .toolbar-left { display: flex; gap: 10px; align-items: center; }
  }
}

.table-card { border-radius: 8px; overflow-x: auto; }

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.tx-hash {
  color: $primary;
  cursor: pointer;
  font-family: monospace;
  font-size: 12px;
  &:hover { text-decoration: underline; }
}

.no-chain { color: #c0c4cc; font-size: 12px; }

.detail-wrapper {
  .detail-section-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin: 16px 0 8px;
    padding-left: 8px;
    border-left: 3px solid $primary;
    &:first-child { margin-top: 0; }
  }
}
</style>
