<template>
  <div class="page-container">
    <!-- Toolbar -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="queryParams.keyword" placeholder="搜索批次号/买方名称" prefix-icon="el-icon-search"
            size="small" clearable style="width: 240px" @keyup.enter.native="handleSearch" />
          <el-select v-model="queryParams.saleChannel" placeholder="销售渠道" clearable size="small" style="width: 130px" @change="handleSearch">
            <el-option v-for="c in channelOptions" :key="c" :label="c" :value="c" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        </div>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openAddDialog">新增销售</el-button>
      </div>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="batchCode" label="批次号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="saleDate" label="销售日期" width="110" />
        <el-table-column prop="buyerName" label="销售对象" min-width="130" show-overflow-tooltip />
        <el-table-column label="渠道" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.saleChannel" :type="channelTagType(row.saleChannel)" size="mini">{{ row.saleChannel }}</el-tag>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="100">
          <template slot-scope="{ row }">{{ row.saleQuantity || '--' }} {{ row.saleUnit || '' }}</template>
        </el-table-column>
        <el-table-column label="单价" width="90" align="right">
          <template slot-scope="{ row }">
            <span v-if="row.salePrice != null">{{ row.salePrice }}</span>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column label="总额" width="100" align="right">
          <template slot-scope="{ row }">
            <span v-if="row.totalAmount != null" class="amount">{{ row.totalAmount }}</span>
            <span v-else class="no-data">--</span>
          </template>
        </el-table-column>
        <el-table-column prop="destination" label="目的地" min-width="130" show-overflow-tooltip />
        <el-table-column label="凭证" width="80" align="center">
          <template slot-scope="{ row }">
            <template v-if="parseImages(row.saleVoucher).length">
              <el-image
                :src="resolveUrl(parseImages(row.saleVoucher)[0])"
                :preview-src-list="parseImages(row.saleVoucher).map(resolveUrl)"
                fit="cover"
                class="thumb-img"
              />
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
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="openDetail(row)">详情</el-button>
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
    <el-dialog title="新增销售记录" :visible.sync="addVisible" width="680px" :close-on-click-modal="false" @closed="onAddClosed">
      <el-form ref="addForm" :model="form" :rules="formRules" label-width="100px" size="small">
        <!-- Admin enterprise select -->
        <el-form-item v-if="isAdmin" label="操作企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" filterable placeholder="请选择操作企业" style="width: 100%" @change="onEnterpriseChange">
            <el-option v-for="e in allEnterprises" :key="e.id" :label="e.enterpriseName + (e.enterpriseType === 1 ? ' (养殖)' : ' (加工)')" :value="e.id" />
          </el-select>
        </el-form-item>
        <!-- Batch -->
        <el-form-item label="关联批次" prop="batchId">
          <el-select v-model="form.batchId" placeholder="搜索并选择批次" filterable style="width: 100%" @change="onBatchPick">
            <el-option v-for="b in batchOptions" :key="b.id" :label="b.batchCode + '  (' + b.productName + ')'" :value="b.id" />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="销售日期" prop="saleDate">
              <el-date-picker v-model="form.saleDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售时间">
              <el-time-picker v-model="form.saleTime" value-format="HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="销售对象" prop="buyerName">
          <el-input v-model="form.buyerName" placeholder="如: XX超市 / XX批发市场" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="销售渠道">
              <el-select v-model="form.saleChannel" placeholder="请选择" style="width: 100%">
                <el-option v-for="c in channelOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目的地">
              <el-input v-model="form.destination" placeholder="如: 北京市朝阳区" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">
          <i class="el-icon-coin"></i> 交易信息
        </el-divider>

        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="数量" prop="saleQuantity">
              <el-input-number v-model="form.saleQuantity" :min="0" :max="selectedBatchMaxQuantity || Infinity" :precision="1" style="width: 200px" @change="calcTotal" />
              <div v-if="saleQuantityHint" class="quantity-hint-text">{{ saleQuantityHint }}</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="单位">
              <el-select v-model="form.saleUnit" style="width: 100%">
                <el-option label="只" value="只" />
                <el-option label="斤" value="斤" />
                <el-option label="公斤" value="公斤" />
                <el-option label="吨" value="吨" />
                <el-option label="箱" value="箱" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价(元)">
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" style="width: 100%" @change="calcTotal" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="销售总额">
          <el-input :value="totalAmountDisplay" disabled>
            <template slot="prepend">RMB</template>
            <template slot="append">元</template>
          </el-input>
        </el-form-item>

        <el-form-item label="销售凭证">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :file-list="form.fileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-preview="handlePreview"
            accept="image/*"
            :limit="3"
          >
            <div class="upload-trigger">
              <i class="el-icon-camera"></i>
              <span>上传</span>
            </div>
            <div slot="tip" class="upload-tip">最多3张，可上传发票/收据/合同</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="其他备注信息" maxlength="500" show-word-limit />
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
    <el-dialog title="销售记录详情" :visible.sync="detailVisible" width="680px">
      <div v-if="detailRow" class="detail-wrapper">
        <div class="detail-section-title">基础信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="批次号">{{ detailRow.batchCode }}</el-descriptions-item>
          <el-descriptions-item label="销售日期">{{ detailRow.saleDate }} {{ detailRow.saleTime || '' }}</el-descriptions-item>
          <el-descriptions-item label="销售对象">{{ detailRow.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="销售渠道">
            <el-tag v-if="detailRow.saleChannel" :type="channelTagType(detailRow.saleChannel)" size="mini">{{ detailRow.saleChannel }}</el-tag>
            <span v-else>--</span>
          </el-descriptions-item>
          <el-descriptions-item label="目的地">{{ detailRow.destination || '--' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">交易信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="销售数量">{{ detailRow.saleQuantity || '--' }} {{ detailRow.saleUnit || '' }}</el-descriptions-item>
          <el-descriptions-item label="单价">{{ detailRow.salePrice != null ? detailRow.salePrice + ' 元' : '--' }}</el-descriptions-item>
          <el-descriptions-item label="销售总额">
            <span v-if="detailRow.totalAmount != null" class="amount">{{ detailRow.totalAmount }} 元</span>
            <span v-else>--</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">区块链存证</div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="TxHash">
            <span v-if="detailRow.txHash" class="tx-hash" @click="copyHash(detailRow.txHash)">{{ detailRow.txHash }}</span>
            <span v-else style="color: #c0c4cc">未上链</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ detailRow.remark || '--' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Voucher -->
        <div v-if="parseImages(detailRow.saleVoucher).length" class="detail-photos">
          <p class="photo-title">销售凭证</p>
          <el-image
            v-for="(url, idx) in parseImages(detailRow.saleVoucher)"
            :key="idx"
            :src="resolveUrl(url)"
            :preview-src-list="parseImages(detailRow.saleVoucher).map(resolveUrl)"
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
import { getSaleList, createSale } from '@/api/sale'
import { uploadFile, getEnterpriseList } from '@/api/enterprise'
import request from '@/utils/request'

export default {
  name: 'Sale',
  data() {
    return {
      loading: false,
      submitting: false,
      tableData: [],
      total: 0,
      batchOptions: [],
      allEnterprises: [],
      selectedBatchMaxQuantity: null,
      saleQuantityHint: '',
      channelOptions: ['批发市场', '超市', '深加工', '电商平台', '直销'],
      queryParams: { pageNum: 1, pageSize: 10, keyword: '', saleChannel: '' },
      // Add dialog
      addVisible: false,
      form: {
        batchId: null,
        batchCode: '',
        saleDate: '',
        saleTime: '',
        buyerName: '',
        saleChannel: '',
        destination: '',
        saleQuantity: null,
        saleUnit: '公斤',
        salePrice: null,
        remark: '',
        fileList: [],
        enterpriseId: null
      },
      formRules: {
        enterpriseId: [{ required: true, message: '请选择操作企业', trigger: 'change' }],
        batchId: [{ required: true, message: '请选择批次', trigger: 'change' }],
        saleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
        buyerName: [{ required: true, message: '请输入销售对象', trigger: 'blur' }],
        saleQuantity: [
          { required: true, message: '请输入数量', trigger: 'blur' },
          { validator: (rule, value, callback) => {
            if (value !== null && value !== '' && isNaN(Number(value))) {
              callback(new Error('销售数量必须为数字'))
            } else if (this.selectedBatchMaxQuantity !== null && value > this.selectedBatchMaxQuantity) {
              callback(new Error('销售数量不能超过批次当前数量 ' + this.selectedBatchMaxQuantity))
            } else {
              callback()
            }
          }, trigger: 'blur' }
        ]
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
    isAdmin() {
      const userInfo = this.$store.getters.userInfo
      return userInfo && userInfo.userType === 1
    },
    totalAmountDisplay() {
      if (this.form.saleQuantity && this.form.salePrice) {
        return (this.form.saleQuantity * this.form.salePrice).toFixed(2)
      }
      return '0.00'
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
        this.allEnterprises = records.filter(e => e.enterpriseType === 2)
      }).catch(() => {})
    },
    fetchBatches(enterpriseIdOverride) {
      const userInfo = this.$store.getters.userInfo
      const enterpriseId = enterpriseIdOverride || (userInfo && userInfo.enterpriseId)
      this.fetchProcessingSaleBatches(enterpriseId)
    },
    fetchProcessingSaleBatches(enterpriseId) {
      // 通过运输记录的 receiveEnterpriseId 找到该加工企业接收的所有批次
      const transportParams = { pageNum: 1, pageSize: 500 }
      if (enterpriseId) transportParams.receiveEnterpriseId = enterpriseId
      request({
        url: '/api/transport/list',
        method: 'get',
        params: transportParams
      }).then(res => {
        const transportRecords = (res.data && (res.data.records || res.data.list)) || []
        if (!transportRecords.length) {
          this.batchOptions = []
          return
        }
        // 去重 batchId，并保留批次基本信息
        const batchMap = new Map()
        transportRecords.forEach(t => {
          if (t.batchId && !batchMap.has(t.batchId)) {
            batchMap.set(t.batchId, {
              id: t.batchId,
              batchCode: t.batchCode || '',
              productName: '未知产品',
              unit: t.transportUnit || ''
            })
          }
        })
        const batches = Array.from(batchMap.values())
        // 并发查询每个批次的可销售数量，只保留 availableSale > 0 的批次
        const requests = batches.map(b =>
          request({
            url: '/api/sale/available-quantity',
            method: 'get',
            params: { batchId: b.id, enterpriseId: enterpriseId || undefined }
          }).then(r => ({ batch: b, data: r.data || {} })).catch(() => null)
        )
        Promise.all(requests).then(results => {
          this.batchOptions = results
            .filter(r => r && Number(r.data.availableSale) > 0)
            .map(r => ({
              id: r.batch.id,
              batchCode: r.batch.batchCode,
              productName: r.data.productName || r.batch.productName || '未知产品',
              availableSale: Number(r.data.availableSale),
              unit: r.data.unit || r.batch.unit || ''
            }))
        })
      }).catch(() => {
        this.batchOptions = []
      })
    },
    buildAvailableBatchOptions(storageRecords, soldBatchIds, eligibleBatchIds = null) {
      const batchMap = new Map()

      storageRecords.forEach(storage => {
        if (!storage.batchId) return
        if (soldBatchIds.has(storage.batchId)) return
        if (eligibleBatchIds && !eligibleBatchIds.has(storage.batchId)) return

        if (!batchMap.has(storage.batchId)) {
          batchMap.set(storage.batchId, {
            id: storage.batchId,
            batchCode: storage.batchCode,
            productName: storage.productName || '未知产品',
            currentQuantity: storage.storageQuantity,
            unit: storage.storageUnit || storage.unit
          })
          return
        }

        const existing = batchMap.get(storage.batchId)
        existing.currentQuantity = (existing.currentQuantity || 0) + (storage.storageQuantity || 0)
      })

      return Array.from(batchMap.values())
    },
    fetchList() {
      this.loading = true
      getSaleList(this.queryParams).then(res => {
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
    calcTotal() {
      // auto-calculate, computed handles display
    },
    // Channel tag
    channelTagType(channel) {
      const map = { '批发市场': '', '超市': 'success', '深加工': 'warning', '电商平台': 'info', '直销': '' }
      return map[channel] || ''
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
      this.saleQuantityHint = ''
      this.form = {
        batchId: null, batchCode: '', saleDate: '', saleTime: '',
        buyerName: '', saleChannel: '', destination: '',
        saleQuantity: null, saleUnit: '公斤', salePrice: null,
        remark: '', fileList: [], enterpriseId: null
      }
      this.fetchBatches()
      this.addVisible = true
    },
    onAddClosed() {
      this.saleQuantityHint = ''
      if (this.$refs.addForm) this.$refs.addForm.resetFields()
    },
    onBatchPick(batchId) {
      const b = this.batchOptions.find(item => item.id === batchId)
      if (!b) {
        this.form.batchCode = ''
        this.selectedBatchMaxQuantity = null
        this.saleQuantityHint = ''
        return
      }
      this.form.batchCode = b.batchCode
      const userInfo = this.$store.getters.userInfo
      const enterpriseId = this.isAdmin ? this.form.enterpriseId : (userInfo && userInfo.enterpriseId)
      const params = { batchId }
      if (enterpriseId) params.enterpriseId = enterpriseId

      // 查可销售数量
      const availableReq = request({ url: '/api/sale/available-quantity', method: 'get', params })
      // 查该加工企业对该批次的最新运输记录
      const transportParams = { current: 1, size: 1, batchId }
      if (enterpriseId) transportParams.enterpriseId = enterpriseId
      const transportReq = request({ url: '/api/transport/list', method: 'get', params: transportParams })

      Promise.all([availableReq, transportReq]).then(([availRes, transRes]) => {
        const data = availRes.data || {}
        const available = data.availableSale != null ? Number(data.availableSale) : null
        const unit = data.unit || ''
        this.selectedBatchMaxQuantity = available
        if (available !== null) {
          this.form.saleQuantity = available
          this.saleQuantityHint = '最多可销售 ' + available + ' ' + unit +
            '（已运输 ' + (data.totalTransported || 0) + '，已销售 ' + (data.totalSold || 0) + '）'
        } else {
          this.saleQuantityHint = ''
        }
        if (unit) this.form.saleUnit = unit

        // 从最新运输记录带入目的地和单位
        const transportRecords = (transRes.data && (transRes.data.records || transRes.data.list)) || []
        if (transportRecords.length > 0) {
          const latest = transportRecords[0]
          if (latest.destination) this.form.destination = latest.destination
          if (latest.transportUnit) this.form.saleUnit = latest.transportUnit
        }
      }).catch(() => {
        this.selectedBatchMaxQuantity = null
        this.saleQuantityHint = ''
      })
    },
    onEnterpriseChange(enterpriseId) {
      this.form.batchId = null
      this.form.batchCode = ''
      this.form.saleQuantity = null
      this.selectedBatchMaxQuantity = null
      this.saleQuantityHint = ''
      this.batchOptions = []
      if (enterpriseId) {
        this.fetchBatches(enterpriseId)
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
      if (!this.isAdmin) {
        this.$refs.addForm.clearValidate('enterpriseId')
      }
      const valid = await this.$refs.addForm.validate().catch(() => false)
      if (!valid) return

      this.submitting = true
      try {
        // 1. Upload voucher images
        const imageUrls = []
        for (const f of this.form.fileList) {
          if (f.raw) {
            const res = await uploadFile(f.raw, 'sale')
            imageUrls.push(res.data.url)
          } else if (f.url) {
            imageUrls.push(f.url)
          }
        }

        // 2. Build submit data
        const totalAmount = (this.form.saleQuantity && this.form.salePrice)
          ? parseFloat((this.form.saleQuantity * this.form.salePrice).toFixed(2))
          : null

        const submitData = {
          batchId: this.form.batchId,
          batchCode: this.form.batchCode,
          saleDate: this.form.saleDate,
          saleTime: this.form.saleTime || null,
          buyerName: this.form.buyerName,
          saleChannel: this.form.saleChannel || null,
          destination: this.form.destination || null,
          saleQuantity: this.form.saleQuantity,
          saleUnit: this.form.saleUnit || null,
          salePrice: this.form.salePrice,
          totalAmount: totalAmount,
          remark: this.form.remark || null,
          saleVoucher: imageUrls.length > 0 ? JSON.stringify(imageUrls) : null
        }

        // Inject enterpriseId
        const userInfo = this.$store.getters.userInfo
        if (this.isAdmin) {
          submitData.enterpriseId = this.form.enterpriseId
        } else if (userInfo && userInfo.enterpriseId) {
          submitData.enterpriseId = userInfo.enterpriseId
        }

        await createSale(submitData)
        this.$message.success('销售记录创建成功，数据已上链')
        this.fetchBatches(this.isAdmin ? this.form.enterpriseId : undefined)
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

.no-data { color: #c0c4cc; font-size: 12px; }
.amount { color: #f56c6c; font-weight: 600; }
.quantity-hint-text { font-size: 12px; color: #909399; margin-top: 4px; }

// Thumbnails
.thumb-img {
  width: 36px; height: 36px; border-radius: 4px;
  cursor: pointer; vertical-align: middle;
}

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

// Detail sections
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
