<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.keyword" placeholder="搜索批次号/车牌号/司机" prefix-icon="el-icon-search"
            size="small" clearable style="width: 240px" @keyup.enter.native="handleSearch" />
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddDialog">新增运输</el-button>
      </div>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="batchCode" label="批次号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="transportDate" label="运输日期" width="110" />
        <el-table-column prop="plateNumber" label="车牌号" width="110" />
        <el-table-column prop="driverName" label="司机" width="90" />
        <el-table-column prop="departureLocation" label="出发地" min-width="130" show-overflow-tooltip />
        <el-table-column prop="destination" label="目的地" min-width="130" show-overflow-tooltip />
        <el-table-column label="运输量" width="110">
          <template slot-scope="{ row }">{{ row.transportQuantity || '--' }} {{ row.transportUnit || '' }}</template>
        </el-table-column>
        <el-table-column label="温度" width="80" align="center">
          <template slot-scope="{ row }">
            <span v-if="row.temperature != null" :class="tempClass(row.temperature)">{{ row.temperature }}&#8451;</span>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="照片" width="100" align="center">
          <template slot-scope="{ row }">
            <template v-if="parseImages(row.images).length">
              <el-image
                v-for="(url, idx) in parseImages(row.images).slice(0, 2)"
                :key="idx"
                :src="resolveUrl(url)"
                :preview-src-list="parseImages(row.images).map(resolveUrl)"
                fit="cover"
                class="thumb-img"
              />
              <span v-if="parseImages(row.images).length > 2" class="more-img">+{{ parseImages(row.images).length - 2 }}</span>
            </template>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="上链" width="60" align="center">
          <template slot-scope="{ row }">
            <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
              <i class="el-icon-link" style="color: #2d8a56; cursor: pointer" @click="copyHash(row.txHash)"></i>
            </el-tooltip>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="openDetail(row)">详情</el-button>
            <el-button type="text" size="mini" style="color: #f56c6c" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog title="新增运输记录" :visible.sync="addVisible" width="720px" :close-on-click-modal="false" @closed="onAddClosed">
      <el-form ref="addForm" :model="form" :rules="formRules" label-width="110px" size="small">
        <el-form-item v-if="isAdmin" label="操作企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" filterable placeholder="请选择操作企业" style="width: 100%" @change="onEnterpriseChange">
            <el-option v-for="e in allEnterprises" :key="e.id" :label="e.enterpriseName + (e.enterpriseType === 1 ? ' (养殖)' : ' (加工)')" :value="e.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联批次" prop="batchId">
          <el-select v-model="form.batchId" placeholder="搜索并选择批次" filterable style="width: 100%" @change="onBatchPick">
            <el-option v-for="b in batchOptions" :key="b.id" :label="b.batchCode + '  (' + b.productName + ')'" :value="b.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="showProcessingEnterpriseSelect" label="目标加工企业" prop="receiveEnterpriseId">
          <el-select v-model="form.receiveEnterpriseId" filterable placeholder="请选择目标加工企业" style="width: 100%" @change="onProcessingEnterprisePick">
            <el-option v-for="e in processingEnterprises" :key="e.id" :label="e.enterpriseName" :value="e.id" />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="运输日期" prop="transportDate">
              <el-date-picker v-model="form.transportDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物流单号">
              <el-input v-model="form.logisticsNo" placeholder="如: SF2026021000001" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          <i class="el-icon-truck"></i> 运输信息
        </el-divider>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="车牌号" prop="plateNumber">
              <el-input v-model="form.plateNumber" maxlength="20" placeholder="如: 鲁A12345" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运输条件">
              <el-input v-model="form.transportCondition" placeholder="如: 冷链运输、常温运输" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="司机姓名">
              <el-input v-model="form.driverName" placeholder="司机姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="司机电话" prop="driverPhone">
              <el-input v-model="form.driverPhone" placeholder="11位手机号" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出发地" prop="departureLocation">
              <el-input v-model="form.departureLocation" placeholder="如: 山东省寿光市XX镇" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的地" prop="destination">
              <el-input v-model="form.destination" placeholder="如: 北京市朝阳区XX路" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="收件人">
              <el-input v-model="form.receiverName" placeholder="收件人姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          <i class="el-icon-cloudy"></i> 运输详情
        </el-divider>

        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="运输数量" prop="transportQuantity">
              <el-input-number v-model="form.transportQuantity" :min="0" :max="selectedBatchMaxQuantity || Infinity" :precision="1" style="width: 200px" />
              <span v-if="availableTransportInfo" class="transport-quantity-hint">
                最多可运输 {{ availableTransportInfo.availableTransport }} {{ availableTransportInfo.unit }}（已出库 {{ availableTransportInfo.totalOutbound }}，已运输 {{ availableTransportInfo.totalTransported }}）
              </span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="车厢温度" prop="temperature">
              <el-input v-model.number="form.temperature" placeholder="摄氏度" @input="validateNumber('temperature')" style="width: 100%">
                <template slot="append">&#8451;</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="车厢湿度" prop="humidity">
              <el-input v-model.number="form.humidity" placeholder="百分比" @input="validateNumber('humidity')" style="width: 100%">
                <template slot="append">%</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="运输照片">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :file-list="form.fileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-preview="handlePreview"
            accept="image/*"
            :limit="6"
          >
            <div class="upload-trigger">
              <i class="el-icon-camera"></i>
              <span>上传</span>
            </div>
            <div slot="tip" class="upload-tip">最多6张，可上传运输车辆照/封条照</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="其他备注信息" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button size="small" @click="addVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交记录' }}
        </el-button>
      </div>
    </el-dialog>

    <!-- ========== Detail Dialog ========== -->
    <el-dialog title="运输记录详情" :visible.sync="detailVisible" width="720px">
      <div v-if="detailRow" class="detail-wrapper">
        <div class="detail-section-title">基础信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="批次号">{{ detailRow.batchCode }}</el-descriptions-item>
          <el-descriptions-item label="物流单号">{{ detailRow.logisticsNo || '--' }}</el-descriptions-item>
          <el-descriptions-item label="运输日期">{{ detailRow.transportDate }}</el-descriptions-item>
          <el-descriptions-item label="运输条件">{{ detailRow.transportCondition || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">运输信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="车牌号">{{ detailRow.plateNumber || '--' }}</el-descriptions-item>
          <el-descriptions-item label="司机">{{ detailRow.driverName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="司机电话">{{ detailRow.driverPhone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收件人">{{ detailRow.receiverName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="出发地">{{ detailRow.departureLocation || '--' }}</el-descriptions-item>
          <el-descriptions-item label="目的地">{{ detailRow.destination || '--' }}</el-descriptions-item>
          <el-descriptions-item label="运输数量">{{ detailRow.transportQuantity || '--' }} {{ detailRow.transportUnit || '' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">环境数据</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="温度">
            <span v-if="detailRow.temperature != null" :class="tempClass(detailRow.temperature)">{{ detailRow.temperature }}&#8451;</span>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="湿度">
            <span v-if="detailRow.humidity != null">{{ detailRow.humidity }}%</span>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="出发时间">{{ detailRow.departureTime || '--' }}</el-descriptions-item>
          <el-descriptions-item label="到达时间">{{ detailRow.arrivalTime || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">区块链存证</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="TxHash">
            <span v-if="detailRow.txHash" class="tx-hash" @click="copyHash(detailRow.txHash)">{{ detailRow.txHash }}</span>
            <span v-else style="color: #c0c4cc">未上链</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ detailRow.remark || '--' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Photos -->
        <div v-if="parseImages(detailRow.images).length" class="detail-photos">
          <p class="photo-title">运输照片</p>
          <el-image
            v-for="(url, idx) in parseImages(detailRow.images)"
            :key="idx"
            :src="resolveUrl(url)"
            :preview-src-list="parseImages(detailRow.images).map(resolveUrl)"
            fit="cover"
            class="detail-img"
          />
        </div>
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
import { getTransportList, createTransport, deleteTransport } from '@/api/transport'
import { uploadFile, getEnterpriseListByType, getEnterpriseList } from '@/api/enterprise'
import request from '@/utils/request'

export default {
  name: 'Transport',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      batchOptions: [],
      processingEnterprises: [],
      allEnterprises: [],
      selectedBatchMaxQuantity: null,
      availableTransportInfo: null,
      queryParams: { pageNum: 1, pageSize: 10, keyword: '' },
      // Add dialog
      addVisible: false,
      form: {
        batchId: null,
        batchCode: '',
        receiveEnterpriseId: null,
        logisticsNo: '',
        transportDate: '',
        plateNumber: '',
        driverName: '',
        driverPhone: '',
        receiverName: '',
        departureLocation: '',
        destination: '',
        transportCondition: '',
        transportQuantity: null,
        temperature: null,
        humidity: null,
        remark: '',
        fileList: [],
        enterpriseId: null
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
    isBreedingEnterprise() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.enterpriseType === 1
    },
    isAdmin() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.userType === 1
    },
    showProcessingEnterpriseSelect() {
      if (this.isAdmin) {
        // Admin selected a processing enterprise: no need to pick target
        if (this.form.enterpriseId) {
          const selected = this.allEnterprises.find(e => e.id === this.form.enterpriseId)
          if (selected && selected.enterpriseType === 2) return false
        }
        return true
      }
      return this.isBreedingEnterprise
    },
    formRules() {
      return {
        enterpriseId: this.isAdmin ? [{ required: true, message: '请选择操作企业', trigger: 'change' }] : [],
        batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
        receiveEnterpriseId: this.showProcessingEnterpriseSelect ? [{ required: true, message: '请选择目标加工企业', trigger: 'change' }] : [],
        transportDate: [{ required: true, message: '请选择运输日期', trigger: 'change' }],
        plateNumber: [
          { required: true, message: '请输入车牌号', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value && value.trim().length > 20) {
                callback(new Error('车牌号不能超过20个字符'))
                return
              }
              callback()
            },
            trigger: 'blur'
          }
        ],
        departureLocation: [{ required: true, message: '请输入出发地', trigger: 'blur' }],
        destination: [{ required: true, message: '请输入目的地', trigger: 'blur' }],
        driverPhone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
        ],
        transportQuantity: [
          { validator: (rule, value, callback) => {
            if (value !== null && value !== '' && isNaN(Number(value))) {
              callback(new Error('运输数量必须为数字'))
            } else if (this.selectedBatchMaxQuantity !== null && value > this.selectedBatchMaxQuantity) {
              callback(new Error('运输数量不能超过批次当前数量 ' + this.selectedBatchMaxQuantity))
            } else {
              callback()
            }
          }, trigger: 'blur' }
        ],
        temperature: [
          { validator: (rule, value, callback) => {
            if (value !== null && value !== '' && isNaN(Number(value))) {
              callback(new Error('温度必须为数字'))
            } else {
              callback()
            }
          }, trigger: 'blur' }
        ],
        humidity: [
          { validator: (rule, value, callback) => {
            if (value !== null && value !== '' && isNaN(Number(value))) {
              callback(new Error('湿度必须为数字'))
            } else {
              callback()
            }
          }, trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    const userInfo = this.$store.getters.userInfo
    if (userInfo && userInfo.enterpriseId) {
      this.queryParams.enterpriseId = userInfo.enterpriseId
    }
    this.fetchBatches()
    if (this.isBreedingEnterprise || this.isAdmin) {
      this.fetchProcessingEnterprises()
    }
    if (this.isAdmin) {
      this.fetchAllEnterprises()
    }
    this.fetchList()
  },
  methods: {
    validateNumber(field) {
      this.$nextTick(() => {
        if (this.form[field] !== null && this.form[field] !== '' && isNaN(Number(this.form[field]))) {
          this.$refs.addForm.validateField(field)
        }
      })
    },
    fetchProcessingEnterprises() {
      getEnterpriseListByType(2).then(res => {
        this.processingEnterprises = res.data || []
      }).catch(() => {})
    },
    fetchAllEnterprises() {
      getEnterpriseList({ size: 200, status: 1 }).then(res => {
        const records = res.data.records || res.data || []
        this.allEnterprises = records.filter(e => e.enterpriseType === 1 || e.enterpriseType === 2)
      }).catch(() => {})
    },
    fetchBatches(enterpriseIdOverride) {
      const userInfo = this.$store.getters.userInfo
      const enterpriseId = enterpriseIdOverride || (userInfo && userInfo.enterpriseId)

      // Query storage outbound records to get candidate batches
      const storageParams = { current: 1, size: 500, storageType: 2 }
      if (enterpriseId) storageParams.enterpriseId = enterpriseId

      request({ url: '/api/storage/list', method: 'get', params: storageParams }).then(storageRes => {
        const storageRecords = storageRes.data.records || []

        // Collect unique batchIds that have outbound records
        const batchMap = new Map()
        storageRecords.forEach(s => {
          if (s.batchId && s.quarantinePassed !== false && !batchMap.has(s.batchId)) {
            batchMap.set(s.batchId, {
              id: s.batchId,
              batchCode: s.batchCode,
              productName: s.productName || '',
              unit: s.storageUnit || s.unit || ''
            })
          }
        })

        if (batchMap.size === 0) {
          this.batchOptions = []
          return
        }

        // Filter by availableTransport > 0
        const candidates = Array.from(batchMap.values())
        const checks = candidates.map(b => {
          const params = { batchId: b.id }
          if (enterpriseId) params.enterpriseId = enterpriseId
          return request({ url: '/api/transport/available-quantity', method: 'get', params })
            .then(res => {
              const avail = (res.data && res.data.availableTransport) || 0
              return Number(avail) > 0 ? b : null
            })
            .catch(() => null)
        })

        Promise.all(checks).then(results => {
          this.batchOptions = results.filter(Boolean)
        })
      }).catch(() => {
        this.batchOptions = []
      })
    },
    fetchList() {
      this.loading = true
      getTransportList(this.queryParams).then(res => {
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
    // Temperature helper
    tempClass(v) {
      if (v == null) return ''
      if (v < 0) return 'env-cold'
      if (v <= 8) return 'env-cool'
      if (v <= 25) return 'env-normal'
      return 'env-hot'
    },
    // Images
    parseImages(val) {
      if (!val) return []
      try { return JSON.parse(val) } catch (e) { return [val] }
    },
    resolveUrl(path) {
      if (!path) return ''
      if (path.startsWith('http://tmp')) return ''
      if (path.startsWith('http')) return path
      return process.env.VUE_APP_BASE_API + path.replace(/^\/api/, '')
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
      this.selectedBatchMaxQuantity = null
      this.availableTransportInfo = null
      this.form = {
        batchId: null, batchCode: '', receiveEnterpriseId: null, logisticsNo: '', transportDate: '',
        plateNumber: '', driverName: '', driverPhone: '', receiverName: '',
        departureLocation: '', destination: '', transportCondition: '',
        transportQuantity: null, temperature: null, humidity: null,
        remark: '', fileList: [], enterpriseId: null
      }
      this.addVisible = true
    },
    onAddClosed() {
      if (this.$refs.addForm) this.$refs.addForm.resetFields()
    },
    onBatchPick(batchId) {
      const b = this.batchOptions.find(item => item.id === batchId)
      if (b) {
        this.form.batchCode = b.batchCode
      } else {
        this.form.batchCode = ''
        this.form.transportQuantity = null
        this.selectedBatchMaxQuantity = null
        this.availableTransportInfo = null
        return
      }

      const userInfo = this.$store.getters.userInfo
      const enterpriseId = this.isAdmin ? this.form.enterpriseId : (userInfo && userInfo.enterpriseId)
      const params = { batchId }
      if (enterpriseId) params.enterpriseId = enterpriseId

      request({ url: '/api/transport/available-quantity', method: 'get', params }).then(res => {
        const info = res.data || null
        this.availableTransportInfo = info
        if (info) {
          const avail = Number(info.availableTransport) || 0
          this.form.transportQuantity = avail
          this.selectedBatchMaxQuantity = avail
        }
      }).catch(() => {
        this.availableTransportInfo = null
        this.selectedBatchMaxQuantity = null
      })
    },
    onEnterpriseChange(enterpriseId) {
      this.form.batchId = null
      this.form.batchCode = ''
      this.form.transportQuantity = null
      this.selectedBatchMaxQuantity = null
      this.availableTransportInfo = null
      this.batchOptions = []
      if (enterpriseId) {
        this.fetchBatches(enterpriseId)
      }
    },
    onProcessingEnterprisePick(enterpriseId) {
      const ent = this.processingEnterprises.find(e => e.id === enterpriseId)
      if (ent) {
        const fullAddress = [ent.province, ent.city, ent.district, ent.address].filter(Boolean).join('')
        this.form.receiverName = ent.enterpriseName || ''
        this.form.destination = fullAddress || ''
      }
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
            const res = await uploadFile(f.raw, 'transport')
            imageUrls.push(res.data.url)
          } else if (f.url) {
            imageUrls.push(f.url)
          }
        }

        // 2. Build submit data
        const submitData = {
          batchId: this.form.batchId,
          batchCode: this.form.batchCode,
          receiveEnterpriseId: this.form.receiveEnterpriseId,
          logisticsNo: this.form.logisticsNo || null,
          transportDate: this.form.transportDate,
          plateNumber: this.form.plateNumber ? this.form.plateNumber.trim() : '',
          driverName: this.form.driverName || null,
          driverPhone: this.form.driverPhone || null,
          receiverName: this.form.receiverName || null,
          departureLocation: this.form.departureLocation,
          destination: this.form.destination,
          transportCondition: this.form.transportCondition || null,
          transportQuantity: this.form.transportQuantity,
          temperature: this.form.temperature,
          humidity: this.form.humidity,
          remark: this.form.remark || null,
          images: imageUrls.length > 0 ? JSON.stringify(imageUrls) : null
        }

        // Inject enterpriseId
        const userInfo = this.$store.getters.userInfo
        if (this.isAdmin) {
          submitData.enterpriseId = this.form.enterpriseId
        } else if (userInfo && userInfo.enterpriseId) {
          submitData.enterpriseId = userInfo.enterpriseId
        }

        await createTransport(submitData)
        this.$message.success('运输记录创建成功')
        this.addVisible = false
        this.fetchList()
        this.fetchBatches()
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
    handleDelete(row) {
      this.$confirm('确认删除该运输记录？', '提示', { type: 'warning' }).then(() => {
        deleteTransport(row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchList()
          this.fetchBatches()
        })
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

// Environment colors
.env-cold { color: #409eff; font-weight: 600; }
.env-cool { color: #67c23a; font-weight: 500; }
.env-normal { color: #303133; }
.env-hot { color: #f56c6c; font-weight: 600; }
.no-data { color: #c0c4cc; font-size: 12px; }

// Thumbnails
.thumb-img {
  width: 36px; height: 36px; border-radius: 4px; margin-right: 4px;
  cursor: pointer; vertical-align: middle;
}
.more-img { font-size: 11px; color: #909399; vertical-align: middle; }

// Upload
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

// TxHash
.tx-hash {
  font-family: monospace; font-size: 12px; color: $primary;
  cursor: pointer; word-break: break-all;
  &:hover { text-decoration: underline; }
}

.transport-quantity-hint {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

// Detail
.detail-wrapper {
  .detail-section-title {
    font-size: 14px; font-weight: 600; color: #303133;
    margin: 16px 0 8px; padding-left: 8px; border-left: 3px solid $primary;
    &:first-child { margin-top: 0; }
  }
}

// Detail photos
.detail-photos {
  margin-top: 16px;
  .photo-title { font-size: 14px; font-weight: 500; color: #303133; margin: 0 0 10px; }
  .detail-img { width: 100px; height: 100px; border-radius: 4px; margin-right: 8px; margin-bottom: 8px; }
}
</style>
