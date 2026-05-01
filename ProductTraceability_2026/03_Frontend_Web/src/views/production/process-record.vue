<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索批次号/产品名称" prefix-icon="el-icon-search"
            size="small" clearable style="width: 240px" @keyup.enter.native="handleSearch" />
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
      </div>
    </el-card>

    <!-- Batch Table with expandable record rows -->
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>批次与过程记录</span>
        <el-tag size="small" type="info">共 {{ batchTotal }} 个批次</el-tag>
      </div>

      <el-table
        ref="batchTable"
        v-loading="batchLoading"
        :data="batchList"
        border
        size="small"
        style="width: 100%"
        row-key="id"
        :expand-row-keys="expandedRows"
        @expand-change="onExpandChange"
      >
        <!-- Expand column: shows records for this batch -->
        <el-table-column type="expand">
          <template slot-scope="{ row }">
            <div class="expand-content">
              <div class="expand-header">
                <span class="expand-title">{{ row.batchCode }} - 过程记录</span>
                <el-select v-model="row._recordTypeFilter" placeholder="全部类型" clearable size="mini" style="width: 120px" @change="() => fetchRecords(row)">
                  <el-option v-for="t in getRecordTypesForBatch(row)" :key="t.value" :label="t.label" :value="t.value" />
                </el-select>
              </div>

              <el-table v-loading="row._recordLoading" :data="row._records" border size="mini" style="width: 100%">
                <el-table-column label="类型" width="80" align="center">
                  <template slot-scope="scope">
                    <el-tag :type="typeTagColor(scope.row.recordType)" size="mini">{{ typeLabel(scope.row.recordType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="recordDate" label="记录日期" width="110" />
                <el-table-column prop="itemName" label="项目名称" min-width="130" show-overflow-tooltip />
                <el-table-column prop="operator" label="操作人" width="90" />
                <el-table-column prop="description" label="内容描述" min-width="160" show-overflow-tooltip />
                <el-table-column label="照片" width="120" align="center">
                  <template slot-scope="scope">
                    <template v-if="parseImages(scope.row.images).length">
                      <el-image
                        v-for="(url, idx) in parseImages(scope.row.images).slice(0, 3)"
                        :key="idx"
                        :src="resolveUrl(url)"
                        :preview-src-list="parseImages(scope.row.images).map(resolveUrl)"
                        fit="cover"
                        class="thumb-img"
                      />
                    </template>
                    <span v-else class="no-img">--</span>
                  </template>
                </el-table-column>
                <el-table-column label="上链" width="60" align="center">
                  <template slot-scope="scope">
                    <el-tooltip v-if="scope.row.txHash" :content="scope.row.txHash" placement="top">
                      <i class="el-icon-link" style="color: #2d8a56; cursor: pointer" @click="copyHash(scope.row.txHash)"></i>
                    </el-tooltip>
                    <span v-else style="color: #c0c4cc">--</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" size="mini" @click="openDetail(scope.row)">详情</el-button>
                    <el-popconfirm title="确定删除？" @confirm="handleDelete(scope.row, row)">
                      <el-button slot="reference" type="text" size="mini" style="color: #f56c6c">删除</el-button>
                    </el-popconfirm>
                  </template>
                </el-table-column>
              </el-table>

              <div v-if="!row._recordLoading && (!row._records || row._records.length === 0)" class="expand-empty">
                暂无过程记录，请点击上方"新增记录"添加
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="batchCode" label="批次号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品名称" width="100" />
        <el-table-column label="品种" width="100">
          <template slot-scope="{ row }">{{ row.breed || '--' }}</template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template slot-scope="{ row }">{{ row.quantity || row.initQuantity || 0 }} {{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.batchStatus || row.status)" size="mini">{{ statusText(row.batchStatus || row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-view" @click="toggleExpand(row)">
              {{ isExpanded(row.id) ? '收起记录' : '查看记录' }}
            </el-button>
            <el-button type="primary" size="mini" icon="el-icon-plus" plain @click="openAddDialog(row)">新增记录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="batchTotal"
          :page-size="batchQuery.size"
          :current-page="batchQuery.current"
          :page-sizes="[10, 20, 50]"
          @current-change="val => { batchQuery.current = val; fetchBatches() }"
          @size-change="val => { batchQuery.size = val; batchQuery.current = 1; fetchBatches() }"
        />
      </div>
    </el-card>

    <!-- ========== Add Record Dialog ========== -->
    <el-dialog title="新增过程记录" :visible.sync="addDialogVisible" width="640px" :close-on-click-modal="false" @closed="onDialogClosed">
      <el-form ref="addForm" :model="form" :rules="formRules" label-width="100px" size="small">
        <el-form-item label="所属批次">
          <el-input :value="activeBatchLabel" disabled />
        </el-form-item>

        <el-form-item label="记录类型" prop="recordType">
          <el-radio-group v-model="form.recordType">
            <el-radio-button v-for="t in activeRecordTypes" :key="t.value" :label="t.value">{{ t.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="记录日期" prop="recordDate">
              <el-date-picker v-model="form.recordDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%" :picker-options="recordDatePickerOptions" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作人" prop="operator">
              <el-input v-model="form.operator" placeholder="操作人姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="项目名称" prop="itemName">
          <el-input v-model="form.itemName" :placeholder="itemNamePlaceholder" />
        </el-form-item>

        <el-form-item label="用量" v-if="form.recordType !== 'inspect'">
          <el-input v-model="form.amount" placeholder="如: 50 (仅填数字, 可不填)" />
        </el-form-item>

        <el-form-item label="作业位置">
          <el-input v-model="form.location" placeholder="如: 3号鸡舍、A区温室大棚" />
        </el-form-item>

        <el-form-item label="内容描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请详细描述操作内容、观察结果等..." maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="现场照片">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :file-list="form.fileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-preview="handlePreview"
            accept="image/*"
            :limit="9"
          >
            <div class="upload-trigger">
              <i class="el-icon-camera"></i>
              <span>上传</span>
            </div>
            <div slot="tip" class="upload-tip">最多9张, 单张不超过10MB</div>
          </el-upload>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button size="small" @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '正在提交...' : '提交记录' }}
        </el-button>
      </div>
    </el-dialog>

    <!-- ========== Detail Dialog ========== -->
    <el-dialog title="记录详情" :visible.sync="detailVisible" width="640px">
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item label="批次号">{{ detailRow.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="记录类型">
          <el-tag :type="typeTagColor(detailRow.recordType)" size="mini">{{ typeLabel(detailRow.recordType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="记录日期">{{ detailRow.recordDate }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailRow.operator || '--' }}</el-descriptions-item>
        <el-descriptions-item label="项目名称" :span="2">{{ detailRow.itemName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="用量">{{ detailRow.amount || '--' }}</el-descriptions-item>
        <el-descriptions-item label="作业位置">{{ detailRow.location || '--' }}</el-descriptions-item>
        <el-descriptions-item label="内容描述" :span="2">{{ detailRow.description || '--' }}</el-descriptions-item>
        <el-descriptions-item label="TxHash" :span="2">
          <span v-if="detailRow.txHash" class="tx-hash" @click="copyHash(detailRow.txHash)">{{ detailRow.txHash }}</span>
          <span v-else style="color: #c0c4cc">未上链</span>
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="detailRow && parseImages(detailRow.images).length" class="detail-photos">
        <p class="photo-title">现场照片</p>
        <el-image
          v-for="(url, idx) in parseImages(detailRow.images)"
          :key="idx"
          :src="resolveUrl(url)"
          :preview-src-list="parseImages(detailRow.images).map(resolveUrl)"
          fit="cover"
          class="detail-img"
        />
      </div>
    </el-dialog>

    <!-- Image Preview -->
    <el-dialog :visible.sync="previewVisible" width="auto" append-to-body>
      <img :src="previewUrl" style="max-width: 100%; max-height: 80vh" />
    </el-dialog>
  </div>
</template>

<script>
import { getBatchList } from '@/api/batch'
import { getRecordList, createRecord, deleteRecord } from '@/api/record'
import { uploadFile } from '@/api/enterprise'

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

// All record types (for display/mapping)
const ALL_RECORD_TYPES = [
  { value: 'feeding', label: '喂养', tag: '' },
  { value: 'vaccine', label: '防疫', tag: 'warning' },
  { value: 'inspect', label: '环境巡查', tag: 'info' },
  { value: 'fertilize', label: '施肥', tag: 'success' },
  { value: 'irrigate', label: '灌溉', tag: '' },
  { value: 'pesticide', label: '除虫', tag: 'danger' }
]

// Record types by product type (matching mini-program)
const RECORD_TYPES_BY_PRODUCT = {
  1: [ // chicken
    { value: 'feeding', label: '喂养', tag: '' },
    { value: 'vaccine', label: '防疫', tag: 'warning' },
    { value: 'inspect', label: '环境巡查', tag: 'info' }
  ],
  2: [ // tomato
    { value: 'fertilize', label: '施肥', tag: 'success' },
    { value: 'irrigate', label: '灌溉', tag: '' },
    { value: 'pesticide', label: '除虫', tag: 'danger' }
  ]
}

export default {
  name: 'ProcessRecord',
  data() {
    return {
      batchLoading: false,
      submitting: false,
      keyword: '',
      batchList: [],
      batchTotal: 0,
      batchQuery: { current: 1, size: 10 },
      expandedRows: [],
      // Active batch (for add dialog)
      activeBatch: null,
      // Add dialog
      addDialogVisible: false,
      form: {
        recordType: '',
        recordDate: '',
        itemName: '',
        amount: '',
        operator: '',
        description: '',
        location: '',
        fileList: []
      },
      autoOperator: '',
      formRules: {
        recordType: [{ required: true, message: '请选择记录类型', trigger: 'change' }],
        recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
        itemName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
        operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
        description: [{ required: true, message: '请输入内容描述', trigger: 'blur' }]
      },
      recordDatePickerOptions: {
        disabledDate(time) {
          const today = new Date()
          today.setHours(23, 59, 59, 999)
          return time.getTime() > today.getTime()
        }
      },
      // Detail
      detailVisible: false,
      detailRow: null,
      // Preview
      previewVisible: false,
      previewUrl: ''
    }
  },
  computed: {
    activeBatchLabel() {
      if (!this.activeBatch) return ''
      return this.activeBatch.batchCode + ' (' + this.activeBatch.productName + ')'
    },
    // Record types for active batch's product type
    activeRecordTypes() {
      if (!this.activeBatch) return ALL_RECORD_TYPES
      const pt = this.activeBatch.productType
      return RECORD_TYPES_BY_PRODUCT[pt] || ALL_RECORD_TYPES
    },
    itemNamePlaceholder() {
      const rt = this.form.recordType
      if (!this.activeBatch) return '请输入项目名称'
      const pt = this.activeBatch.productType
      const placeholders = {
        1: { feeding: '如: 玉米饲料、豆粕混合料', vaccine: '如: 新城疫疫苗、禽流感疫苗', inspect: '如: 温湿度检测、通风检查、卫生巡查' },
        2: { fertilize: '如: 复合肥、有机肥', irrigate: '如: 滴灌、喷灌', pesticide: '如: 吡虫啉、多菌灵' }
      }
      return (placeholders[pt] && placeholders[pt][rt]) || '请输入项目名称'
    }
  },
  created() {
    this.fetchBatches()
  },
  methods: {
    // === Batch List ===
    fetchBatches() {
      this.batchLoading = true
      const userInfo = this.$store.getters.userInfo
      const params = {
        current: this.batchQuery.current,
        size: this.batchQuery.size,
        keyword: this.keyword || undefined
      }
      if (userInfo && userInfo.enterpriseId) {
        params.enterpriseId = userInfo.enterpriseId
      }
      getBatchList(params).then(res => {
        const page = res.data
        const list = page.records || page.list || []
        // Attach reactive properties for expand state
        list.forEach(b => {
          this.$set(b, '_records', [])
          this.$set(b, '_recordLoading', false)
          this.$set(b, '_recordTypeFilter', null)
        })
        this.batchList = list
        this.batchTotal = page.total || 0
      }).catch(() => {
        this.batchList = []
        this.batchTotal = 0
      }).finally(() => {
        this.batchLoading = false
      })
    },
    handleSearch() {
      this.batchQuery.current = 1
      this.expandedRows = []
      this.fetchBatches()
    },
    // === Status Helpers ===
    statusText(status) {
      return (STATUS_MAP[status] || {}).text || '未知'
    },
    statusTagType(status) {
      return (STATUS_MAP[status] || {}).type || 'info'
    },
    // === Expand / Collapse ===
    isExpanded(id) {
      return this.expandedRows.includes(id)
    },
    toggleExpand(row) {
      if (this.isExpanded(row.id)) {
        this.expandedRows = this.expandedRows.filter(id => id !== row.id)
      } else {
        this.expandedRows = [...this.expandedRows, row.id]
        this.fetchRecords(row)
      }
    },
    onExpandChange(row, expandedRows) {
      this.expandedRows = expandedRows.map(r => r.id)
      // When newly expanded, fetch records
      if (this.isExpanded(row.id)) {
        this.fetchRecords(row)
      }
    },
    // === Fetch Records for a Batch ===
    fetchRecords(batch) {
      this.$set(batch, '_recordLoading', true)
      const params = {
        pageNum: 1,
        pageSize: 100,
        batchId: batch.id,
        recordType: batch._recordTypeFilter || undefined
      }
      getRecordList(params).then(res => {
        this.$set(batch, '_records', res.data.records || res.data.list || [])
      }).catch(() => {
        this.$set(batch, '_records', [])
      }).finally(() => {
        this.$set(batch, '_recordLoading', false)
      })
    },
    // === Record Type Helpers ===
    getRecordTypesForBatch(batch) {
      const pt = batch.productType
      return RECORD_TYPES_BY_PRODUCT[pt] || ALL_RECORD_TYPES
    },
    typeLabel(type) {
      const found = ALL_RECORD_TYPES.find(t => t.value === type)
      return found ? found.label : type || '--'
    },
    typeTagColor(type) {
      const found = ALL_RECORD_TYPES.find(t => t.value === type)
      return found ? found.tag : ''
    },
    // === Images ===
    parseImages(val) {
      if (!val) return []
      try { return JSON.parse(val) } catch (e) { return [val] }
    },
    resolveUrl(path) {
      if (!path) return ''
      // wx临时文件(http://tmp/xxx)无法在浏览器中访问，返回空
      if (path.startsWith('http://tmp')) return ''
      if (path.startsWith('http')) return path
      return process.env.VUE_APP_BASE_API + path.replace(/^\/api/, '')
    },
    // === Clipboard ===
    copyHash(hash) {
      if (!hash) return
      const el = document.createElement('textarea')
      el.value = hash
      el.style.position = 'fixed'
      el.style.opacity = '0'
      document.body.appendChild(el)
      el.select()
      document.execCommand('copy')
      document.body.removeChild(el)
      this.$message.success('TxHash 已复制')
    },
    // === Add Dialog ===
    openAddDialog(batch) {
      this.activeBatch = batch
      const userInfo = this.$store.getters.userInfo
      const operatorName = (userInfo && (userInfo.realName || userInfo.username)) || ''
      this.autoOperator = operatorName
      this.form = {
        recordType: '',
        recordDate: '',
        itemName: '',
        amount: '',
        operator: operatorName,
        description: '',
        location: '',
        fileList: []
      }
      this.addDialogVisible = true
    },
    onDialogClosed() {
      if (this.$refs.addForm) this.$refs.addForm.resetFields()
    },
    handleFileChange(file, fileList) {
      if (file.raw && file.raw.size > 10 * 1024 * 1024) {
        this.$message.error('单张图片不超过10MB')
        fileList.pop()
        return
      }
      this.form.fileList = fileList
    },
    handleFileRemove(file, fileList) {
      this.form.fileList = fileList
    },
    handlePreview(file) {
      this.previewUrl = file.url
      this.previewVisible = true
    },
    async handleSubmit() {
      const valid = await this.$refs.addForm.validate().catch(() => false)
      if (!valid) return

      this.submitting = true
      try {
        // 1. Upload images
        const imageUrls = []
        for (const f of this.form.fileList) {
          if (f.raw) {
            const res = await uploadFile(f.raw, 'record')
            imageUrls.push(res.data.url)
          } else if (f.url) {
            imageUrls.push(f.url)
          }
        }
        // 2. Create record
        const data = {
          batchId: this.activeBatch.id,
          batchCode: this.activeBatch.batchCode,
          recordType: this.form.recordType,
          recordDate: this.form.recordDate,
          itemName: this.form.itemName,
          amount: this.form.amount && !isNaN(this.form.amount) ? parseFloat(this.form.amount) : null,
          operator: this.form.operator,
          description: this.form.description,
          location: this.form.location || null,
          images: imageUrls.length > 0 ? JSON.stringify(imageUrls) : null
        }
        await createRecord(data)
        this.$message.success('记录提交成功')
        this.addDialogVisible = false
        // Refresh the expanded batch records
        if (this.isExpanded(this.activeBatch.id)) {
          this.fetchRecords(this.activeBatch)
        } else {
          // Auto-expand to show the new record
          this.expandedRows = [...this.expandedRows, this.activeBatch.id]
          this.fetchRecords(this.activeBatch)
        }
      } catch (e) {
        // handled by interceptor
      } finally {
        this.submitting = false
      }
    },
    // === Detail ===
    openDetail(row) {
      this.detailRow = row
      this.detailVisible = true
    },
    // === Delete ===
    handleDelete(record, batch) {
      deleteRecord(record.id).then(() => {
        this.$message.success('删除成功')
        this.fetchRecords(batch)
      }).catch(() => {})
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
    display: flex; justify-content: space-between; align-items: center;
    .toolbar-left { display: flex; gap: 10px; align-items: center; }
  }
}

.table-card {
  border-radius: 8px;
  overflow-x: auto;
  .card-header {
    display: flex; align-items: center; gap: 12px;
    font-weight: 600; color: #1a3a2a;
  }
}

.pagination-wrapper {
  display: flex; justify-content: flex-end; margin-top: 16px;
}

// === Fix: isolate expand row hover from parent table ===
::v-deep .el-table__expanded-cell {
  padding: 0 !important;
  background-color: #fafbfc !important;

  &:hover {
    background-color: #fafbfc !important;
  }
}

// Prevent parent row hover from bleeding into expand area
::v-deep .el-table__body tr.el-table__row--level-0:hover > td {
  background-color: #f5f7fa;
}

// === Expand Area ===
.expand-content {
  padding: 12px 16px 12px 20px;
  background: #fafbfc;

  .expand-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }

  .expand-title {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
  }

  .expand-empty {
    text-align: center;
    color: #909399;
    font-size: 13px;
    padding: 20px 0;
  }
}

// === Thumbnails ===
.thumb-img {
  width: 32px; height: 32px; border-radius: 4px;
  margin-right: 3px; cursor: pointer; vertical-align: middle;
}
.no-img { color: #c0c4cc; font-size: 12px; }

// === Upload ===
::v-deep .el-upload--picture-card {
  display: flex; align-items: center; justify-content: center; line-height: normal;
}
.upload-trigger {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 100%; height: 100%;
  i { font-size: 24px; color: $primary; }
  span { font-size: 11px; color: #909399; margin-top: 2px; }
}
.upload-tip { font-size: 12px; color: #909399; margin-top: 4px; }

// === TxHash ===
.tx-hash {
  font-family: monospace; font-size: 12px; color: $primary;
  cursor: pointer; word-break: break-all;
  &:hover { text-decoration: underline; }
}

// === Detail Photos ===
.detail-photos {
  margin-top: 16px;
  .photo-title { font-size: 14px; font-weight: 500; color: #303133; margin: 0 0 10px; }
  .detail-img { width: 100px; height: 100px; border-radius: 4px; margin-right: 8px; margin-bottom: 8px; }
}
</style>
