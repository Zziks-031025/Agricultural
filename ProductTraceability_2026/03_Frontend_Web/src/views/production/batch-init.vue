<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.keyword" placeholder="搜索批次号/产品名称" prefix-icon="el-icon-search"
            size="small" clearable style="width: 240px" @keyup.enter.native="handleSearch" />
          <el-select v-model="queryParams.productType" placeholder="产品类型" size="small" clearable style="width: 130px" @change="handleSearch">
            <el-option label="肉鸡" :value="1" />
            <el-option label="西红柿" :value="2" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreateDialog">新建批次</el-button>
      </div>
    </el-card>

    <!-- Batch Table -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border size="small" style="width: 100%">
        <el-table-column prop="batchCode" label="批次号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品名称" min-width="100" />
        <el-table-column label="品种" min-width="90">
          <template slot-scope="{ row }">{{ row.breed || '--' }}</template>
        </el-table-column>
        <el-table-column label="数量" min-width="90">
          <template slot-scope="{ row }">{{ row.quantity || row.initQuantity || 0 }} {{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="140">
          <template slot-scope="{ row }">{{ row.createTime || row.createDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.status)" size="mini">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="区块链 TxHash" min-width="200">
          <template slot-scope="{ row }">
            <template v-if="row.txHash">
              <el-tooltip content="点击复制" placement="top">
                <span class="tx-hash" @click="copyHash(row.txHash)">{{ shortHash(row.txHash) }}</span>
              </el-tooltip>
            </template>
            <span v-else class="no-chain">未上链</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-view" @click="viewDetail(row)">详情</el-button>
            <el-button type="text" size="mini" icon="el-icon-printer" @click="openQrDialog(row)">二维码</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" style="color: #F56C6C" @click="handleDelete(row)">删除</el-button>
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

    <!-- ========== Create Dialog ========== -->
    <el-dialog title="新建批次" :visible.sync="createDialogVisible" width="640px" :close-on-click-modal="false">
      <el-form ref="batchForm" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item v-if="isAdmin" label="养殖企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" filterable placeholder="请选择养殖企业" style="width: 100%">
            <el-option v-for="e in breedingEnterprises" :key="e.id" :label="e.enterpriseName" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品类型" prop="productType">
          <el-select v-model="form.productType" placeholder="请选择" style="width: 100%" @change="onTypeChange">
            <el-option label="肉鸡" :value="1" />
            <el-option label="西红柿" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种" prop="breed">
          <el-input v-model="form.breed" :placeholder="form.productType === 2 ? '如: 粉果番茄 / 大红' : '如: 三黄鸡 / 白羽肉鸡'" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="预计数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" style="width: 100%">
                <el-option label="只" value="只" />
                <el-option label="斤" value="斤" />
                <el-option label="公斤" value="公斤" />
                <el-option label="吨" value="吨" />
                <el-option label="棵" value="棵" />
                <el-option label="株" value="株" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="产地描述" prop="originLocation">
          <el-input v-model="form.originLocation" type="textarea" :rows="2" placeholder="请描述产地位置，如: 山东省寿光市XX镇XX村" />
        </el-form-item>
        <el-form-item label="负责人" prop="manager">
          <el-input v-model="form.manager" :placeholder="form.productType === 2 ? '种植负责人姓名' : '养殖负责人姓名'" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="form.productType === 2 ? '种植日期' : '入栏日期'">
              <el-date-picker v-model="form.productionDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="form.productType === 2 ? '预计收获' : '预计出栏'">
              <el-date-picker v-model="form.expectedHarvestDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="苗源/种子来源">
          <el-input v-model="form.seedSource" :placeholder="form.productType === 2 ? '如: XX种苗公司' : '如: XX种禽场'" />
        </el-form-item>
        <!-- Tomato-specific -->
        <template v-if="form.productType === 2">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="种植面积">
                <el-input v-model="form.plantArea" placeholder="如: 5 (亩)">
                  <template slot="append">亩</template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="大棚编号">
                <el-input v-model="form.greenhouseNo" placeholder="如: A-03" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">提交创建</el-button>
      </div>
    </el-dialog>

    <!-- ========== Detail Dialog ========== -->
    <el-dialog title="批次详情" :visible.sync="detailDialogVisible" width="720px">
      <div v-if="detailData" class="detail-wrapper">
        <!-- Section 1: Basic Info -->
        <div class="detail-section-title">基础信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="批次号">{{ detailData.batchCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.batchStatus)" size="mini">{{ statusText(detailData.batchStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ detailData.productName }}</el-descriptions-item>
          <el-descriptions-item label="产品类型">{{ detailData.productType === 1 ? '肉鸡' : (detailData.productType === 2 ? '西红柿' : '--') }}</el-descriptions-item>
          <el-descriptions-item label="品种">{{ detailData.breed || '--' }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detailData.manager || '--' }}</el-descriptions-item>
          <el-descriptions-item label="初始数量">{{ detailData.initQuantity || '--' }} {{ detailData.unit || '' }}</el-descriptions-item>
          <el-descriptions-item label="当前数量">{{ detailData.currentQuantity || '--' }} {{ detailData.unit || '' }}</el-descriptions-item>
          <el-descriptions-item label="苗源/种子来源">{{ detailData.seedSource || '--' }}</el-descriptions-item>
          <el-descriptions-item label="所属企业">{{ detailData.enterpriseName || '--' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Section 2: Location & Planting -->
        <div class="detail-section-title">产地信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="产地位置" :span="2">{{ detailData.originLocation || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.productType === 2" label="种植面积">{{ detailData.plantArea ? detailData.plantArea + ' 亩' : '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.productType === 2" label="大棚编号">{{ detailData.greenhouseNo || '--' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Section 3: Dates -->
        <div class="detail-section-title">时间节点</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="detailData.productType === 2 ? '种植日期' : '入栏日期'">{{ detailData.productionDate || '--' }}</el-descriptions-item>
          <el-descriptions-item :label="detailData.productType === 2 ? '预计收获' : '预计出栏'">{{ detailData.expectedHarvestDate || '--' }}</el-descriptions-item>
          <el-descriptions-item :label="detailData.productType === 2 ? '实际收获' : '实际出栏'">{{ detailData.actualHarvestDate || '--' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createTime || '--' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Section 4: Blockchain -->
        <div class="detail-section-title">区块链存证</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="TxHash">
            <span v-if="detailData.txHash" class="tx-hash" @click="copyHash(detailData.txHash)">{{ detailData.txHash }}</span>
            <span v-else class="no-chain">未上链</span>
          </el-descriptions-item>
          <el-descriptions-item label="DataHash">
            <span v-if="detailData.dataHash" class="tx-hash" style="color: #606266">{{ detailData.dataHash }}</span>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ detailData.blockNumber || '--' }}</el-descriptions-item>
          <el-descriptions-item label="上链时间">{{ detailData.chainTime || '--' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Section 5: Inspection (if any) -->
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
      </div>
    </el-dialog>

    <!-- ========== QR Code Dialog ========== -->
    <el-dialog title="溯源二维码" :visible.sync="qrDialogVisible" width="400px" @opened="generateQr">
      <div class="qr-wrapper">
        <div ref="qrContainer" class="qr-canvas"></div>
        <p class="qr-batch-code">{{ qrBatchCode }}</p>
        <p class="qr-tip">扫描二维码可查询该批次溯源信息</p>
      </div>
      <div slot="footer">
        <el-button size="small" icon="el-icon-printer" @click="printQr">打印二维码</el-button>
        <el-button size="small" @click="qrDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getBatchList, getBatchDetail, addBatch, deleteBatch } from '@/api/batch'
import { getEnterpriseListByType } from '@/api/enterprise'
import QRCode from 'qrcodejs2'

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
  name: 'BatchInit',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      queryParams: {
        current: 1,
        size: 10,
        keyword: '',
        productType: null
      },
      // Create dialog
      createDialogVisible: false,
      form: {
        productType: null,
        productName: '',
        breed: '',
        quantity: 1,
        unit: '只',
        originLocation: '',
        manager: '',
        seedSource: '',
        plantArea: '',
        greenhouseNo: '',
        productionDate: '',
        expectedHarvestDate: '',
        enterpriseId: null
      },
      rules: {
        enterpriseId: [{ required: true, message: '请选择养殖企业', trigger: 'change' }],
        productType: [{ required: true, message: '请选择产品类型', trigger: 'change' }],
        breed: [{ required: true, message: '请输入品种', trigger: 'blur' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
        unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
        originLocation: [{ required: true, message: '请输入产地描述', trigger: 'blur' }],
        manager: [{ required: true, message: '请输入负责人', trigger: 'blur' }]
      },
      // Detail dialog
      detailDialogVisible: false,
      detailData: null,
      // QR dialog
      qrDialogVisible: false,
      qrBatchCode: '',
      qrInstance: null,
      // Admin enterprise selection
      breedingEnterprises: []
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
      this.fetchBreedingEnterprises()
    }
  },
  methods: {
    fetchBreedingEnterprises() {
      getEnterpriseListByType(1).then(res => {
        this.breedingEnterprises = res.data || []
      }).catch(() => {})
    },
    fetchList() {
      this.loading = true
      const params = { ...this.queryParams }
      // pass enterpriseId from current user
      const userInfo = this.$store.getters.userInfo
      if (userInfo && userInfo.enterpriseId) {
        params.enterpriseId = userInfo.enterpriseId
      }
      getBatchList(params).then(res => {
        const page = res.data
        this.tableData = page.records || page.list || []
        this.total = page.total || 0
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchList()
    },
    // Status helpers
    statusText(status) {
      return (STATUS_MAP[status] || {}).text || '未知'
    },
    statusTagType(status) {
      return (STATUS_MAP[status] || {}).type || 'info'
    },
    // TxHash
    shortHash(hash) {
      if (!hash) return ''
      if (hash.length <= 16) return hash
      return hash.substring(0, 10) + '...' + hash.substring(hash.length - 6)
    },
    copyHash(hash) {
      if (!hash) return
      const textarea = document.createElement('textarea')
      textarea.value = hash
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.$message.success('TxHash 已复制到剪贴板')
    },
    // Create dialog
    openCreateDialog() {
      this.createDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.batchForm) this.$refs.batchForm.resetFields()
        this.form = {
          productType: null, productName: '', breed: '', quantity: 1, unit: '只',
          originLocation: '', manager: '', seedSource: '', plantArea: '', greenhouseNo: '',
          productionDate: '', expectedHarvestDate: '', enterpriseId: null
        }
      })
    },
    onTypeChange(val) {
      if (val === 1) {
        this.form.unit = '只'
        this.form.productName = '肉鸡'
        this.form.plantArea = ''
        this.form.greenhouseNo = ''
      } else if (val === 2) {
        this.form.unit = '株'
        this.form.productName = '西红柿'
      }
    },
    handleSubmit() {
      if (!this.isAdmin) {
        this.$refs.batchForm.clearValidate('enterpriseId')
      }
      this.$refs.batchForm.validate(valid => {
        if (!valid) return
        this.submitting = true
        const data = { ...this.form }
        const userInfo = this.$store.getters.userInfo
        if (this.isAdmin) {
          data.enterpriseId = this.form.enterpriseId
        } else if (userInfo && userInfo.enterpriseId) {
          data.enterpriseId = userInfo.enterpriseId
        }
        addBatch(data).then(() => {
          this.$message.success('批次创建成功')
          this.createDialogVisible = false
          this.fetchList()
        }).catch(() => {}).finally(() => {
          this.submitting = false
        })
      })
    },
    // Detail dialog
    viewDetail(row) {
      this.detailData = null
      this.detailDialogVisible = true
      getBatchDetail({ batchCode: row.batchCode }).then(res => {
        this.detailData = res.data
      }).catch(() => {
        this.detailDialogVisible = false
      })
    },
    // QR Code
    openQrDialog(row) {
      this.qrBatchCode = row.batchCode
      this.qrDialogVisible = true
    },
    generateQr() {
      // Clear previous QR
      const container = this.$refs.qrContainer
      if (!container) return
      container.innerHTML = ''
      if (this.qrInstance) {
        this.qrInstance = null
      }
      // QR content: trace URL with batch code
      const traceUrl = window.location.origin + '/#/trace-query/index?batchCode=' + this.qrBatchCode
      this.qrInstance = new QRCode(container, {
        text: traceUrl,
        width: 200,
        height: 200,
        colorDark: '#1a3a2a',
        colorLight: '#ffffff',
        correctLevel: QRCode.CorrectLevel.H
      })
    },
    handleDelete(row) {
      const statusText = this.statusText(row.status)
      this.$confirm(`确认删除批次 ${row.batchCode}（${statusText}）？删除后不可恢复。`, '删除确认', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteBatch(row.id).then(() => {
          this.$message.success('批次已删除')
          this.fetchList()
        }).catch(() => {})
      }).catch(() => {})
    },
    printQr() {
      const container = this.$refs.qrContainer
      if (!container) return
      const canvas = container.querySelector('canvas')
      if (!canvas) return
      const dataUrl = canvas.toDataURL('image/png')
      const printWindow = window.open('', '_blank')
      printWindow.document.write(
        '<html><head><title>' + this.qrBatchCode + '</title>' +
        '<style>body{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;font-family:sans-serif;}' +
        'p{margin:8px 0;font-size:14px;color:#333;}</style></head><body>' +
        '<img src="' + dataUrl + '" width="300" height="300" />' +
        '<p style="font-size:16px;font-weight:bold;">' + this.qrBatchCode + '</p>' +
        '<p>扫描二维码查询溯源信息</p>' +
        '</body></html>'
      )
      printWindow.document.close()
      printWindow.onload = function() {
        printWindow.print()
      }
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

    .toolbar-left {
      display: flex;
      gap: 10px;
      align-items: center;
    }
  }
}

.table-card {
  border-radius: 8px;
  overflow-x: auto;
}

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

.no-chain {
  color: #c0c4cc;
  font-size: 12px;
}

// Detail dialog sections
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

// QR dialog
.qr-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;

  .qr-canvas {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .qr-batch-code {
    margin: 16px 0 4px;
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    font-family: monospace;
  }

  .qr-tip {
    margin: 0;
    font-size: 13px;
    color: #909399;
  }
}
</style>
