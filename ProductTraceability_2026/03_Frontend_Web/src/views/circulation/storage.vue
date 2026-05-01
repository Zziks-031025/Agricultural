<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.keyword" placeholder="搜索批次号/仓库/操作人" prefix-icon="el-icon-search"
            size="small" clearable style="width: 240px" @keyup.enter.native="handleSearch" />
          <el-select v-model="queryParams.storageType" placeholder="类型" clearable size="small" style="width: 120px" @change="handleSearch">
            <el-option label="入库" :value="1" />
            <el-option label="出库" :value="2" />
            <el-option label="库存盘点" :value="3" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddDialog">新增入库</el-button>
      </div>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column label="类型" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="typeTagColor(row.storageType)" size="mini">{{ typeLabel(row.storageType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="batchCode" label="批次号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="storageDate" label="日期" width="110" />
        <el-table-column prop="warehouseName" label="仓库名称" min-width="130" show-overflow-tooltip />
        <el-table-column label="数量" width="110">
          <template slot-scope="{ row }">{{ row.storageQuantity || '--' }} {{ row.storageUnit || '' }}</template>
        </el-table-column>
        <el-table-column label="温度" width="90" align="center">
          <template slot-scope="{ row }">
            <span v-if="row.temperature != null" :class="tempClass(row.temperature)">{{ row.temperature }}&#8451;</span>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="湿度" width="90" align="center">
          <template slot-scope="{ row }">
            <span v-if="row.humidity != null" :class="humidClass(row.humidity)">{{ row.humidity }}%</span>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column label="上链" width="60" align="center">
          <template slot-scope="{ row }">
            <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
              <i class="el-icon-link" style="color: #2d8a56; cursor: pointer" @click="copyHash(row.txHash)"></i>
            </el-tooltip>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="openDetail(row)">详情</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <el-button slot="reference" type="text" size="mini" style="color: #f56c6c">删除</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
          :page-size="queryParams.pageSize" :current-page="queryParams.pageNum" :page-sizes="[10, 20, 50]"
          @current-change="val => { queryParams.pageNum = val; fetchList() }"
          @size-change="val => { queryParams.pageSize = val; queryParams.pageNum = 1; fetchList() }"
        />
      </div>
    </el-card>

    <!-- ========== Add Dialog ========== -->
    <el-dialog title="新增仓储记录" :visible.sync="addVisible" width="680px" :close-on-click-modal="false" @closed="onAddClosed">
      <el-form ref="addForm" :model="form" :rules="formRules" label-width="110px" size="small">
        <!-- Batch -->
        <el-form-item v-if="isAdmin" label="操作企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" filterable placeholder="请选择操作企业" style="width: 100%" @change="onEnterpriseChange">
            <el-option v-for="e in allEnterprises" :key="e.id" :label="e.enterpriseName + (e.enterpriseType === 1 ? ' (养殖)' : ' (加工)')" :value="e.id" />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="操作类型" prop="storageType">
              <el-radio-group v-model="form.storageType" @change="onStorageTypeChange">
                <el-radio-button :label="1">入库</el-radio-button>
                <el-radio-button :label="2">出库</el-radio-button>
                <el-radio-button :label="3">盘点</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作日期" prop="storageDate">
              <el-date-picker v-model="form.storageDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="关联批次" prop="batchId">
          <el-select v-model="form.batchId" placeholder="搜索并选择批次" filterable style="width: 100%" @change="onBatchPick">
            <el-option v-for="b in batchOptions" :key="b.id" :label="b.batchCode + '  (' + b.productName + ')'" :value="b.id" />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="仓库名称" prop="warehouseName">
              <el-input v-model="form.warehouseName" placeholder="如: 1号冷库" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库位置">
              <el-input v-model="form.warehouseLocation" placeholder="如: A区3排" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数量" prop="storageQuantity">
              <el-input-number v-model="form.storageQuantity" :min="0" :max="storageQuantityMax" :precision="1" style="width: 100%" />
              <div v-if="storageQuantityHint" class="quantity-hint">{{ storageQuantityHint }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-select v-model="form.storageUnit" style="width: 100%">
                <el-option label="只" value="只" />
                <el-option label="斤" value="斤" />
                <el-option label="公斤" value="公斤" />
                <el-option label="吨" value="吨" />
                <el-option label="箱" value="箱" />
                <el-option label="袋" value="袋" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Environment Data - Key Feature -->
        <el-divider content-position="left">
          <i class="el-icon-cloudy"></i> 环境监控数据
        </el-divider>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="仓库温度" prop="temperature">
              <el-input v-model.number="form.temperature" placeholder="摄氏度">
                <template slot="append">&#8451;</template>
              </el-input>
              <div class="env-hint" v-if="form.temperature != null && form.temperature !== ''">
                <span :class="tempClass(form.temperature)">
                  {{ tempStatus(form.temperature) }}
                </span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库湿度" prop="humidity">
              <el-input v-model.number="form.humidity" placeholder="百分比">
                <template slot="append">%</template>
              </el-input>
              <div class="env-hint" v-if="form.humidity != null && form.humidity !== ''">
                <span :class="humidClass(form.humidity)">
                  {{ humidStatus(form.humidity) }}
                </span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="存储条件">
          <el-input v-model="form.storageCondition" placeholder="如: 冷藏保鲜、常温干燥、-18度急冻" />
        </el-form-item>

        <el-form-item label="操作人" prop="operator">
          <el-input v-model="form.operator" placeholder="操作人姓名" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="其他备注信息" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button size="small" @click="addVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交并上链' }}
        </el-button>
      </div>
    </el-dialog>

    <!-- ========== Detail Dialog ========== -->
    <el-dialog title="仓储记录详情" :visible.sync="detailVisible" width="640px">
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item label="操作类型">
          <el-tag :type="typeTagColor(detailRow.storageType)" size="mini">{{ typeLabel(detailRow.storageType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作日期">{{ detailRow.storageDate }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detailRow.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailRow.operator || '--' }}</el-descriptions-item>
        <el-descriptions-item label="仓库名称">{{ detailRow.warehouseName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="仓库位置">{{ detailRow.warehouseLocation || '--' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ detailRow.storageQuantity || '--' }} {{ detailRow.storageUnit || '' }}</el-descriptions-item>
        <el-descriptions-item label="存储条件">{{ detailRow.storageCondition || '--' }}</el-descriptions-item>
        <el-descriptions-item label="温度">
          <span v-if="detailRow.temperature != null" :class="tempClass(detailRow.temperature)">
            {{ detailRow.temperature }}&#8451; ({{ tempStatus(detailRow.temperature) }})
          </span>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="湿度">
          <span v-if="detailRow.humidity != null" :class="humidClass(detailRow.humidity)">
            {{ detailRow.humidity }}% ({{ humidStatus(detailRow.humidity) }})
          </span>
          <span v-else>--</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailRow.remark || '--' }}</el-descriptions-item>
        <el-descriptions-item label="TxHash" :span="2">
          <span v-if="detailRow.txHash" class="tx-hash" @click="copyHash(detailRow.txHash)">{{ detailRow.txHash }}</span>
          <span v-else style="color: #c0c4cc">未上链</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { getBatchList } from '@/api/batch'
import { getStorageList, createStorage, deleteStorage } from '@/api/storage'
import { getEnterpriseList } from '@/api/enterprise'
import request from '@/utils/request'

const TYPE_INBOUND = 1
const TYPE_OUTBOUND = 2
const TYPE_INVENTORY = 3

export default {
  name: 'Storage',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      batchOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, storageType: null, keyword: '' },
      // Add dialog
      addVisible: false,
      availableInfo: null,
      form: {
        batchId: null,
        batchCode: '',
        storageType: 1,
        storageDate: '',
        warehouseName: '',
        warehouseLocation: '',
        storageQuantity: null,
        storageUnit: '公斤',
        temperature: null,
        humidity: null,
        storageCondition: '',
        operator: '',
        remark: ''
      },
      formRules: {
        enterpriseId: [{ required: true, message: '请选择操作企业', trigger: 'change' }],
        batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
        storageType: [{ required: true, message: '请选择类型', trigger: 'change' }],
        storageDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
        warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
        storageQuantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
        operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }],
        temperature: [
          { required: true, message: '请输入温度', trigger: 'blur' },
          { validator: (rule, value, callback) => {
            if (value !== null && value !== '' && isNaN(Number(value))) {
              callback(new Error('温度必须为数字'))
            } else {
              callback()
            }
          }, trigger: 'blur' }
        ],
        humidity: [
          { required: true, message: '请输入湿度', trigger: 'blur' },
          { validator: (rule, value, callback) => {
            if (value !== null && value !== '' && isNaN(Number(value))) {
              callback(new Error('湿度必须为数字'))
            } else {
              callback()
            }
          }, trigger: 'blur' }
        ]
      },
      // Detail
      detailVisible: false,
      detailRow: null,
      // Admin enterprise selection
      allEnterprises: []
    }
  },
  computed: {
    isAdmin() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.userType === 1
    },
    storageQuantityMax() {
      if (!this.availableInfo) return Infinity
      const type = this.form.storageType
      if (type === TYPE_INBOUND) return Number(this.availableInfo.availableInbound) || Infinity
      if (type === TYPE_OUTBOUND) return Number(this.availableInfo.availableOutbound) || Infinity
      if (type === TYPE_INVENTORY) return Number(this.availableInfo.currentStock) || Infinity
      return Infinity
    },
    storageQuantityHint() {
      if (!this.availableInfo) return ''
      const info = this.availableInfo
      const unit = info.unit || ''
      const type = this.form.storageType
      if (type === TYPE_INBOUND) return '最多可入库 ' + info.availableInbound + ' ' + unit
      if (type === TYPE_OUTBOUND) return '最多可出库 ' + info.availableOutbound + ' ' + unit
      if (type === TYPE_INVENTORY) return '当前库存 ' + info.currentStock + ' ' + unit
      return ''
    }
  },
  created() {
    const userInfo = this.$store.getters.userInfo
    if (userInfo && userInfo.enterpriseId) {
      this.queryParams.enterpriseId = userInfo.enterpriseId
    }
    this.fetchBatches()
    this.fetchList()
    if (this.isAdmin) {
      this.fetchAllEnterprises()
    }
  },
  methods: {
    fetchAllEnterprises() {
      getEnterpriseList({ size: 200, status: 1 }).then(res => {
        const records = res.data.records || res.data || []
        this.allEnterprises = records.filter(e => e.enterpriseType === 1 || e.enterpriseType === 2)
      }).catch(() => {})
    },
    fetchBatches(enterpriseIdOverride) {
      const userInfo = this.$store.getters.userInfo
      const enterpriseId = enterpriseIdOverride || (userInfo && userInfo.enterpriseId)
      const selectedEnterprise = enterpriseIdOverride ? this.allEnterprises.find(e => e.id === enterpriseIdOverride) : null
      const enterpriseType = selectedEnterprise ? selectedEnterprise.enterpriseType : (userInfo && userInfo.enterpriseType)

      const filterByType = (batches) => {
        const storageType = this.form.storageType
        const checks = batches.map(b => {
          const params = { batchId: b.id }
          if (enterpriseId) params.enterpriseId = enterpriseId
          return request({ url: '/api/storage/available-quantity', method: 'get', params })
            .then(res => {
              const info = res.data || {}
              if (storageType === TYPE_INBOUND && Number(info.availableInbound) > 0) return b
              if (storageType === TYPE_OUTBOUND && Number(info.availableOutbound) > 0) return b
              if (storageType === TYPE_INVENTORY && Number(info.currentStock) > 0) return b
              return null
            })
            .catch(() => null)
        })
        Promise.all(checks).then(results => {
          this.batchOptions = results.filter(Boolean)
        })
      }

      if (!enterpriseId) {
        getBatchList({ size: 200 }).then(batchRes => {
          filterByType(batchRes.data.records || [])
        }).catch(() => {})
        return
      }

      if (enterpriseType === 1) {
        getBatchList({ size: 200, statuses: '5', enterpriseId }).then(batchRes => {
          const records = (batchRes.data.records || []).filter(b => b.quarantinePassed !== false)
          filterByType(records)
        }).catch(() => {})
      } else if (enterpriseType === 2) {
        request({ url: '/api/processing/list', method: 'get', params: { pageNum: 1, pageSize: 500, enterpriseId } })
          .then(res => {
            const processedBatchCodes = [...new Set((res.data.records || []).map(r => r.sourceBatchCode))]
            getBatchList({ size: 200, statuses: '4,5,6,9', enterpriseId, enterpriseType: 2 }).then(batchRes => {
              const records = (batchRes.data.records || []).filter(b =>
                processedBatchCodes.includes(b.batchCode) && b.quarantinePassed !== false
              )
              filterByType(records)
            }).catch(() => {})
          }).catch(() => {})
      }
    },
    onEnterpriseChange(enterpriseId) {
      this.form.batchId = null
      this.form.batchCode = ''
      this.form.storageQuantity = null
      this.availableInfo = null
      this.batchOptions = []
      if (enterpriseId) {
        this.fetchBatches(enterpriseId)
      }
    },
    fetchList() {
      this.loading = true
      getStorageList(this.queryParams).then(res => {
        this.tableData = res.data.records || res.data.list || []
        this.total = res.data.total || 0
      }).catch(() => {
        this.tableData = []
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.queryParams.pageNum = 1
      this.fetchList()
    },
    // Type helpers
    typeLabel(t) {
      return { 1: '入库', 2: '出库', 3: '盘点' }[t] || '未知'
    },
    typeTagColor(t) {
      return { 1: 'success', 2: 'warning', 3: 'info' }[t] || ''
    },
    // Temperature status
    tempClass(v) {
      if (v == null) return ''
      if (v < 0) return 'env-cold'
      if (v <= 8) return 'env-cool'
      if (v <= 25) return 'env-normal'
      return 'env-hot'
    },
    tempStatus(v) {
      if (v == null) return ''
      if (v < 0) return '急冻'
      if (v <= 4) return '冷藏'
      if (v <= 8) return '冷链'
      if (v <= 25) return '常温'
      return '高温预警'
    },
    // Humidity status
    humidClass(v) {
      if (v == null) return ''
      if (v < 30) return 'env-dry'
      if (v <= 70) return 'env-normal'
      return 'env-humid'
    },
    humidStatus(v) {
      if (v == null) return ''
      if (v < 30) return '干燥'
      if (v <= 70) return '适宜'
      return '潮湿预警'
    },
    // Clipboard
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
    // Add dialog
    openAddDialog() {
      const userInfo = this.$store.getters.userInfo
      this.availableInfo = null
      this.batchOptions = []
      this.form = {
        batchId: null, batchCode: '', storageType: 1, storageDate: '',
        warehouseName: '', warehouseLocation: '', storageQuantity: null,
        storageUnit: '公斤', temperature: null, humidity: null,
        storageCondition: '', operator: (userInfo && (userInfo.realName || userInfo.username)) || '', remark: '',
        enterpriseId: null
      }
      this.addVisible = true
      this.$nextTick(() => {
        this.fetchBatches()
      })
    },
    onAddClosed() {
      if (this.$refs.addForm) this.$refs.addForm.resetFields()
    },
    onStorageTypeChange() {
      this.form.batchId = null
      this.form.batchCode = ''
      this.form.storageQuantity = null
      this.availableInfo = null
      this.batchOptions = []
      const enterpriseId = this.isAdmin ? this.form.enterpriseId : null
      this.fetchBatches(enterpriseId || undefined)
    },
    onBatchPick(batchId) {
      const b = this.batchOptions.find(item => item.id === batchId)
      if (b) {
        this.form.batchCode = b.batchCode
        this.form.storageUnit = b.unit || '公斤'
      } else {
        this.form.batchCode = ''
        this.form.storageQuantity = null
        this.availableInfo = null
        return
      }

      const userInfo = this.$store.getters.userInfo
      const enterpriseId = this.isAdmin ? this.form.enterpriseId : (userInfo && userInfo.enterpriseId)
      const params = { batchId }
      if (enterpriseId) params.enterpriseId = enterpriseId

      request({ url: '/api/storage/available-quantity', method: 'get', params }).then(res => {
        this.availableInfo = res.data || null
        const info = this.availableInfo
        if (!info) return
        const type = this.form.storageType
        if (type === TYPE_INBOUND) {
          this.form.storageQuantity = info.availableInbound
        } else if (type === TYPE_OUTBOUND) {
          this.form.storageQuantity = info.availableOutbound
        } else if (type === TYPE_INVENTORY) {
          this.form.storageQuantity = info.currentStock
        }
        if (info.unit) this.form.storageUnit = info.unit
      }).catch(() => {
        this.availableInfo = null
      })
    },
    async handleSubmit() {
      if (!this.isAdmin) {
        this.$refs.addForm.clearValidate('enterpriseId')
      }
      const valid = await this.$refs.addForm.validate().catch(() => false)
      if (!valid) return
      this.submitting = true
      try {
        const submitData = { ...this.form }
        const userInfo = this.$store.getters.userInfo
        if (this.isAdmin) {
          submitData.enterpriseId = this.form.enterpriseId
        } else if (userInfo && userInfo.enterpriseId) {
          submitData.enterpriseId = userInfo.enterpriseId
        }
        await createStorage(submitData)
        this.$message.success('仓储记录创建成功，数据已上链')
        this.addVisible = false
        this.fetchList()
      } catch (e) {
        // handled by interceptor
      } finally {
        this.submitting = false
      }
    },
    // Detail
    openDetail(row) {
      this.detailRow = row
      this.detailVisible = true
    },
    // Delete
    handleDelete(row) {
      deleteStorage(row.id).then(() => {
        this.$message.success('删除成功')
        this.fetchList()
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

.table-card { border-radius: 8px; }

.pagination-wrapper {
  display: flex; justify-content: flex-end; margin-top: 16px;
}

// Environment data colors
.env-cold { color: #409eff; font-weight: 600; }
.env-cool { color: #67c23a; font-weight: 500; }
.env-normal { color: #303133; }
.env-hot { color: #f56c6c; font-weight: 600; }
.env-dry { color: #e6a23c; font-weight: 500; }
.env-humid { color: #f56c6c; font-weight: 600; }
.no-data { color: #c0c4cc; font-size: 12px; }

// Env hint under input
.env-hint {
  font-size: 12px;
  margin-top: 2px;
  line-height: 1.4;
}

.quantity-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

// TxHash
.tx-hash {
  font-family: monospace; font-size: 12px; color: $primary;
  cursor: pointer; word-break: break-all;
  &:hover { text-decoration: underline; }
}
</style>
