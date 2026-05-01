<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.keyword" placeholder="搜索批次号/加工方式/操作员" prefix-icon="el-icon-search"
            size="small" clearable style="width: 260px" @keyup.enter.native="handleSearch" />
          <el-select v-model="queryParams.methodFilter" placeholder="加工方式" size="small" clearable style="width: 130px" @change="handleSearch">
            <el-option label="屠宰" value="屠宰" />
            <el-option label="分割" value="分割" />
            <el-option label="冷冻" value="冷冻" />
            <el-option label="熟制" value="熟制" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreateDialog">添加加工信息</el-button>
      </div>
    </el-card>

    <!-- Processing Table -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border size="small" style="width: 100%">
        <el-table-column prop="sourceBatchCode" label="原料批次号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品名称" min-width="90" />
        <el-table-column prop="processMethod" label="加工方式" min-width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="methodTagType(row.processMethod)">{{ row.processMethod || '--' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投入数量" min-width="100">
          <template slot-scope="{ row }">{{ row.inputQuantity || '--' }} {{ row.inputUnit || row.unit || '' }}</template>
        </el-table-column>
        <el-table-column label="产出数量" min-width="100">
          <template slot-scope="{ row }">{{ row.outputQuantity || '--' }} {{ row.outputUnit || row.unit || '' }}</template>
        </el-table-column>
        <el-table-column prop="specs" label="包装规格" min-width="100">
          <template slot-scope="{ row }">{{ row.specs || '--' }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="操作员" min-width="80" />
        <el-table-column label="加工时间" min-width="140">
          <template slot-scope="{ row }">{{ row.processingDate || '--' }}</template>
        </el-table-column>
        <el-table-column label="上链状态" min-width="100" align="center">
          <template slot-scope="{ row }">
            <el-tooltip v-if="row.txHash" :content="'点击复制 TxHash'" placement="top">
              <el-tag size="mini" type="success" class="chain-tag" @click.native="copyHash(row.txHash)">已上链</el-tag>
            </el-tooltip>
            <el-tag v-else size="mini" type="info">未上链</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-view" @click="viewDetail(row)">详情</el-button>
            <el-popconfirm title="确定删除该加工记录？" @confirm="handleDelete(row)">
              <el-button slot="reference" type="text" size="mini" style="color: #f56c6c">删除</el-button>
            </el-popconfirm>
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
    <el-dialog title="添加加工信息" :visible.sync="createDialogVisible" width="600px" :close-on-click-modal="false">
      <el-form ref="processingForm" :model="form" :rules="formRules" label-width="100px" size="small">
        <el-form-item v-if="isAdmin" label="操作企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" filterable placeholder="请选择加工企业" style="width: 100%" @change="onEnterpriseChange">
            <el-option v-for="e in processingEnterprises" :key="e.id" :label="e.enterpriseName" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原料批次" prop="sourceBatchCode">
          <el-select v-model="form.sourceBatchCode" filterable placeholder="请选择加工中的批次" style="width: 100%" @change="onBatchSelect">
            <el-option v-for="b in processingBatches" :key="b.batchCode" :label="b.batchCode + ' - ' + b.productName" :value="b.batchCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品信息" v-if="selectedBatch">
          <span>{{ selectedBatch.productName }} / {{ selectedBatch.breed || '--' }} / {{ selectedBatch.quantity || selectedBatch.initQuantity }} {{ selectedBatch.unit }}</span>
        </el-form-item>
        <el-form-item label="加工方式" prop="processMethod">
          <el-select v-model="form.processMethod" placeholder="请选择" style="width: 100%">
            <el-option label="屠宰" value="屠宰" />
            <el-option label="分割" value="分割" />
            <el-option label="冷冻" value="冷冻" />
            <el-option label="熟制" value="熟制" />
          </el-select>
        </el-form-item>
        <el-form-item label="加工时间" prop="processingDate">
          <el-date-picker v-model="form.processingDate" type="date" value-format="yyyy-MM-dd" placeholder="选择加工日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="投入数量" prop="inputQuantity">
          <div style="display: flex; align-items: center; gap: 10px">
            <el-input-number v-model="form.inputQuantity" :min="1" :precision="0" style="width: 220px" />
            <el-select v-model="form.inputUnit" placeholder="单位" style="width: 100px">
              <el-option label="只" value="只" />
              <el-option label="kg" value="kg" />
              <el-option label="箱" value="箱" />
              <el-option label="袋" value="袋" />
              <el-option label="头" value="头" />
              <el-option label="株" value="株" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="产出数量" prop="outputQuantity">
          <div style="display: flex; align-items: center; gap: 10px">
            <el-input-number v-model="form.outputQuantity" :min="1" :precision="0" style="width: 220px" />
            <el-select v-model="form.outputUnit" placeholder="单位" style="width: 100px">
              <el-option label="只" value="只" />
              <el-option label="kg" value="kg" />
              <el-option label="箱" value="箱" />
              <el-option label="袋" value="袋" />
              <el-option label="头" value="头" />
              <el-option label="株" value="株" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="包装规格" prop="specs">
          <el-input v-model="form.specs" placeholder="如: 1kg/包、500g/袋" />
        </el-form-item>
        <el-form-item label="操作员">
          <el-input v-model="form.operator" placeholder="操作员姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="现场照片">
          <el-upload
            action="/api/file/upload"
            :data="{ type: 'processing' }"
            :headers="uploadHeaders"
            list-type="picture-card"
            :file-list="form.imageList"
            :on-success="onUploadSuccess"
            :on-remove="onUploadRemove"
            :before-upload="beforeUpload"
            :limit="6"
            accept="image/*"
          >
            <i class="el-icon-plus"></i>
          </el-upload>
          <div class="upload-tip">最多上传6张，支持 jpg/png，单张不超过10MB</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">提交记录</el-button>
      </div>
    </el-dialog>

    <!-- ========== Detail Dialog ========== -->
    <el-dialog title="加工记录详情" :visible.sync="detailDialogVisible" width="680px">
      <div v-if="detailData" class="detail-wrapper">
        <div class="detail-section-title">加工信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="原料批次号">{{ detailData.sourceBatchCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ detailData.productName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="加工方式">
            <el-tag size="mini" :type="methodTagType(detailData.processMethod)">{{ detailData.processMethod || '--' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="加工时间">{{ detailData.processingDate || '--' }}</el-descriptions-item>
          <el-descriptions-item label="投入数量">{{ detailData.inputQuantity || '--' }} {{ detailData.inputUnit || detailData.unit || '' }}</el-descriptions-item>
          <el-descriptions-item label="产出数量">{{ detailData.outputQuantity || '--' }} {{ detailData.outputUnit || detailData.unit || '' }}</el-descriptions-item>
          <el-descriptions-item label="包装规格">{{ detailData.specs || '--' }}</el-descriptions-item>
          <el-descriptions-item label="操作员">{{ detailData.operator || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">区块链存证</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="TxHash">
            <span v-if="detailData.txHash" class="tx-hash" @click="copyHash(detailData.txHash)">{{ detailData.txHash }}</span>
            <span v-else class="no-chain">未上链</span>
          </el-descriptions-item>
          <el-descriptions-item label="DataHash">{{ detailData.dataHash || '--' }}</el-descriptions-item>
          <el-descriptions-item label="区块高度">{{ detailData.blockNumber || '--' }}</el-descriptions-item>
          <el-descriptions-item label="上链时间">{{ detailData.chainTime || '--' }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="detailData.remark">
          <div class="detail-section-title">备注</div>
          <p style="padding: 0 8px; color: #606266; font-size: 13px;">{{ detailData.remark }}</p>
        </template>

        <template v-if="detailImages.length > 0">
          <div class="detail-section-title">现场照片</div>
          <div class="detail-photos">
            <el-image
              v-for="(url, idx) in detailImages" :key="idx"
              :src="url"
              :preview-src-list="detailImages"
              fit="cover"
              class="detail-photo-item"
            />
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getBatchList } from '@/api/batch'
import { getEnterpriseListByType } from '@/api/enterprise'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

const METHOD_TAG = {
  '屠宰': '',
  '分割': 'success',
  '冷冻': 'warning',
  '熟制': 'danger'
}


export default {
  name: 'ProcessingProduct',
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
        methodFilter: null
      },
      // Create dialog
      createDialogVisible: false,
      processingBatches: [],
      processingEnterprises: [],
      selectedBatch: null,
      unitOptions: ['只', 'kg', '箱', '袋', '头', '株'],
      form: {
        enterpriseId: null,
        sourceBatchCode: '',
        processMethod: '',
        processingDate: '',
        inputQuantity: null,
        inputUnit: '只',
        outputQuantity: null,
        outputUnit: 'kg',
        specs: '',
        operator: '',
        remark: '',
        imageList: []
      },
      formRules: {
        enterpriseId: [{ required: true, message: '请选择操作企业', trigger: 'change' }],
        sourceBatchCode: [{ required: true, message: '请选择原料批次', trigger: 'change' }],
        processMethod: [{ required: true, message: '请选择加工方式', trigger: 'change' }],
        processingDate: [{ required: true, message: '请选择加工时间', trigger: 'change' }],
        inputQuantity: [{ required: true, message: '请输入投入数量', trigger: 'blur' }],
        outputQuantity: [{ required: true, message: '请输入产出数量', trigger: 'blur' }],
        specs: [{ required: true, message: '请输入包装规格', trigger: 'blur' }]
      },
      // Detail dialog
      detailDialogVisible: false,
      detailData: null
    }
  },
  created() {
    this.fetchList()
    if (this.isAdmin) {
      this.fetchProcessingEnterprises()
    }
  },
  computed: {
    isAdmin() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.userType === 1
    },
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    },
    detailImages() {
      if (!this.detailData || !this.detailData.images) return []
      try {
        const arr = typeof this.detailData.images === 'string'
          ? JSON.parse(this.detailData.images)
          : this.detailData.images
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
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
      const params = {
        pageNum: this.queryParams.current,
        pageSize: this.queryParams.size,
        keyword: this.queryParams.keyword || undefined
      }
      const userInfo = this.$store.getters.userInfo
      if (userInfo && userInfo.enterpriseId) {
        params.enterpriseId = userInfo.enterpriseId
      }
      request({ url: '/api/processing/list', method: 'get', params }).then(res => {
        const page = res.data
        let records = page.records || []
        if (this.queryParams.methodFilter) {
          records = records.filter(r => r.processMethod === this.queryParams.methodFilter)
        }
        this.tableData = records
        this.total = page.total || 0
      }).catch(() => {}).finally(() => { this.loading = false })
    },
    handleSearch() {
      this.queryParams.current = 1
      this.fetchList()
    },
    methodTagType(method) {
      return METHOD_TAG[method] || 'info'
    },
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
    // --- Create dialog ---
    openCreateDialog() {
      this.selectedBatch = null
      this.form = {
        enterpriseId: null,
        sourceBatchCode: '',
        processMethod: '',
        processingDate: '',
        inputQuantity: null,
        inputUnit: '只',
        outputQuantity: null,
        outputUnit: 'kg',
        specs: '',
        operator: '',
        remark: '',
        imageList: []
      }
      // Auto-fill operator
      const userInfo = this.$store.getters.userInfo
      if (userInfo && userInfo.realName) {
        this.form.operator = userInfo.realName
      }
      this.createDialogVisible = true
      this.loadProcessingBatches()
      this.$nextTick(() => {
        if (this.$refs.processingForm) this.$refs.processingForm.clearValidate()
      })
    },
    loadProcessingBatches() {
      const userInfo = this.$store.getters.userInfo
      const params = { 
        current: 1, 
        size: 100, 
        statuses: '4,5,6,9',
        excludeProcessedCreated: true
      }
      if (this.isAdmin && this.form.enterpriseId) {
        params.enterpriseId = this.form.enterpriseId
        params.enterpriseType = 2
      } else {
        params.enterpriseId = userInfo?.enterpriseId
        params.enterpriseType = userInfo?.enterpriseType
      }
      getBatchList(params).then(res => {
        this.processingBatches = (res.data.records || [])
      }).catch(() => {})
    },
    onEnterpriseChange() {
      this.form.sourceBatchCode = ''
      this.selectedBatch = null
      this.processingBatches = []
      if (this.form.enterpriseId) {
        this.loadProcessingBatches()
      }
    },
    onBatchSelect(batchCode) {
      this.selectedBatch = this.processingBatches.find(b => b.batchCode === batchCode) || null
      if (this.selectedBatch) {
        this.form.inputQuantity = this.selectedBatch.quantity || null
        this.form.inputUnit = this.selectedBatch.unit || '只'
        this.form.outputUnit = 'kg'
      }
    },
    handleSubmit() {
      if (!this.isAdmin) {
        this.$refs.processingForm.clearValidate('enterpriseId')
      }
      this.$refs.processingForm.validate(valid => {
        if (!valid) return
        this.submitting = true

        const data = { ...this.form }
        data.images = JSON.stringify(this.form.imageList.map(f => f.response && f.response.data ? f.response.data.url : '').filter(Boolean))
        delete data.imageList
        const userInfo = this.$store.getters.userInfo
        if (this.isAdmin) {
          data.enterpriseId = this.form.enterpriseId
        } else if (userInfo && userInfo.enterpriseId) {
          data.enterpriseId = userInfo.enterpriseId
        }
        request({ url: '/api/processing/create', method: 'post', data }).then(res => {
          this.$message.success('加工记录提交成功')
          this.createDialogVisible = false
          this.fetchList()
        }).catch(() => {}).finally(() => { this.submitting = false })
      })
    },
    // --- Detail dialog ---
    viewDetail(row) {
      this.detailData = row
      this.detailDialogVisible = true
    },
    handleDelete(row) {
      request({
        url: `/api/processing/delete/${row.id}`,
        method: 'delete'
      }).then(res => {
        if (res.code === 200) {
          this.$message.success('删除成功')
          this.fetchList()
        } else {
          this.$message.error(res.message || '删除失败')
        }
      }).catch(() => {
        this.$message.error('删除失败')
      })
    },
    // --- Image upload ---
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isImage) {
        this.$message.error('只能上传图片文件')
        return false
      }
      if (!isLt10M) {
        this.$message.error('图片大小不能超过 10MB')
        return false
      }
      return true
    },
    onUploadSuccess(res, file, fileList) {
      if (res.code === 200 && res.data && res.data.url) {
        this.form.imageList = fileList
      } else {
        this.$message.error('上传失败')
        fileList.pop()
      }
    },
    onUploadRemove(file, fileList) {
      this.form.imageList = fileList
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

.table-card { border-radius: 8px; }

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

.chain-tag { cursor: pointer; }

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
  .detail-photos {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 0 8px;
    .detail-photo-item {
      width: 100px;
      height: 100px;
      border-radius: 4px;
      border: 1px solid #ebeef5;
    }
  }
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
