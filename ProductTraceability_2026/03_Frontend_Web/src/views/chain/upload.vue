<template>
  <div class="page-container">
    <!-- Summary -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8" v-for="(card, idx) in summaryCards" :key="idx">
        <el-card shadow="hover" class="summary-card">
          <div class="card-body">
            <div class="card-icon" :style="{ background: card.bg }">
              <i :class="card.icon"></i>
            </div>
            <div class="card-info">
              <p class="label">{{ card.label }}</p>
              <h3 class="value">{{ card.value }}</h3>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <div slot="header" class="card-header">
        <span>上链日志</span>
        <div class="header-right">
          <el-select v-model="queryParams.bizType" placeholder="业务类型" clearable size="small" style="width: 140px" @change="handleSearch">
            <el-option v-for="t in bizTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-select v-model="queryParams.status" placeholder="状态" clearable size="small" style="width: 100px" @change="handleSearch">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
          <el-button type="primary" size="small" icon="el-icon-refresh" @click="fetchList">刷新</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column label="交易哈希 (TxHash)" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span class="tx-hash" @click="copyHash(row.txHash)">
              {{ row.txHash || '--' }}
              <i class="el-icon-copy-document copy-icon"></i>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="blockHeight" label="区块高度" width="120" align="center" />
        <el-table-column label="业务类型" width="130" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="bizTagType(row.bizType)" size="mini">{{ bizLabel(row.bizType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chainTime" label="上链时间" width="170" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini" effect="dark">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="{ row }">
            <el-button v-if="row.status === 1" type="text" size="mini" icon="el-icon-document" @click="openCertificate(row)">查看凭证</el-button>
            <el-button v-if="row.status === 0" type="text" size="mini" icon="el-icon-refresh-right" style="color: #e6a23c" :loading="row._retrying" @click="handleRetry(row)">重新上链</el-button>
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

    <!-- Certificate Dialog -->
    <el-dialog :visible.sync="certVisible" width="560px" :show-close="false" custom-class="cert-dialog" top="5vh">
      <div class="certificate-card" v-if="certData">
        <div class="cert-border">
          <div class="cert-watermark">BLOCKCHAIN</div>

          <div class="cert-header">
            <h2 class="cert-title">{{ certData.certTitle || '农产品溯源存证数字证书' }}</h2>
            <p class="cert-subtitle">AGRICULTURAL TRACEABILITY BLOCKCHAIN CERTIFICATE</p>
            <div class="cert-divider"></div>
          </div>

          <div class="cert-body">
            <div class="info-section">
              <div class="section-label">存证信息</div>
              <div class="info-grid">
                <div class="info-item" v-if="certData.bizTypeLabel">
                  <span class="info-key">业务类型</span>
                  <span class="info-val">{{ certData.bizTypeLabel }}</span>
                </div>
                <div class="info-item">
                  <span class="info-key">批次编号</span>
                  <span class="info-val">{{ certData.batchCode || '--' }}</span>
                </div>
                <div class="info-item full">
                  <span class="info-key">所属企业</span>
                  <span class="info-val">{{ certData.enterpriseName || '--' }}</span>
                </div>
              </div>
            </div>

            <div class="blockchain-section">
              <div class="section-label highlight">区块链核心参数</div>

              <div class="param-row">
                <div class="param-label"><span class="label-badge">TX</span>交易哈希</div>
                <div class="param-value hash" @click="copyHash(certData.txHash)">
                  {{ certData.txHash }}
                  <i class="el-icon-copy-document"></i>
                </div>
              </div>
              <div class="param-row">
                <div class="param-label"><span class="label-badge">BH</span>区块高度</div>
                <div class="param-value">{{ certData.blockHeight }}</div>
              </div>
              <div class="param-row">
                <div class="param-label"><span class="label-badge">CA</span>合约地址</div>
                <div class="param-value hash" @click="copyHash(certData.contractAddress)">
                  {{ certData.contractAddress }}
                  <i class="el-icon-copy-document"></i>
                </div>
              </div>
              <div class="param-row">
                <div class="param-label"><span class="label-badge">TS</span>存证时间</div>
                <div class="param-value">{{ certData.chainTime }}</div>
              </div>
            </div>

            <div class="statement">
              <div class="statement-title">法律与技术声明</div>
              <p>本数据已通过 Web3j 协议写入以太坊公共账本，由分布式共识机制保障，任何个人或组织无法私自篡改。该存证记录具备不可逆性、时间戳证明和密码学验证特性，可作为产品溯源、质量追责和法律诉讼的有效电子证据。</p>
            </div>
          </div>

          <div class="cert-footer">
            <div class="footer-line"></div>
            <p>本证书由农产品溯源系统基于区块链技术自动生成</p>
            <p class="footer-date">签发时间: {{ certData.chainTime }}</p>
          </div>

          <div class="seal">
            <div class="seal-circle">
              <span class="seal-top">已上链</span>
              <span class="seal-center">存证</span>
              <span class="seal-bottom">不可篡改</span>
            </div>
          </div>
        </div>
      </div>
      <div slot="footer" style="text-align: center">
        <el-button size="small" @click="certVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getBlockchainLogs, retryChain } from '@/api/trace'

const BIZ_TYPES = [
  { value: 'batch_init', label: '批次初始化', tag: '' },
  { value: 'growth_record', label: '生长记录', tag: 'success' },
  { value: 'quarantine', label: '检疫申报', tag: 'warning' },
  { value: 'processing', label: '加工记录', tag: '' },
  { value: 'storage', label: '仓储入库', tag: 'info' },
  { value: 'transport', label: '物流运输', tag: 'info' },
  { value: 'sale', label: '销售记录', tag: 'danger' }
]

export default {
  name: 'ChainUpload',
  data() {
    return {
      loading: false,
      tableData: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, bizType: null, status: null },
      bizTypes: BIZ_TYPES,
      summaryCards: [
        { label: '累计上链条数', value: '--', icon: 'el-icon-connection', bg: 'rgba(45,138,86,0.1)' },
        { label: '今日新增', value: '--', icon: 'el-icon-time', bg: 'rgba(26,107,58,0.1)' },
        { label: 'Gas 消耗估算', value: '--', icon: 'el-icon-coin', bg: 'rgba(230,162,60,0.1)' }
      ],
      // Certificate dialog
      certVisible: false,
      certData: null
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    fetchList() {
      this.loading = true
      const userInfo = this.$store.getters.userInfo
      const params = { ...this.queryParams }
      if (userInfo && userInfo.enterpriseId) {
        params.enterpriseId = userInfo.enterpriseId
      }
      getBlockchainLogs(params).then(res => {
        const page = res.data
        const list = page.records || page.list || []
        list.forEach(r => { this.$set(r, '_retrying', false) })
        this.tableData = list
        this.total = page.total || 0
        // Update summary from response meta if available
        if (page.totalChained != null) this.summaryCards[0].value = page.totalChained
        if (page.todayChained != null) this.summaryCards[1].value = page.todayChained
        if (page.gasEstimate != null) this.summaryCards[2].value = page.gasEstimate
      }).catch(() => {
        this.tableData = []
        this.total = 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.queryParams.pageNum = 1
      this.fetchList()
    },
    // Hash display
    shortHash(hash) {
      if (!hash || hash.length < 22) return hash || '--'
      return hash.slice(0, 10) + '...' + hash.slice(-10)
    },
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
      this.$message.success('已复制到剪贴板')
    },
    // Biz type helpers
    bizLabel(type) {
      const found = BIZ_TYPES.find(t => t.value === type)
      return found ? found.label : type || '--'
    },
    bizTagType(type) {
      const found = BIZ_TYPES.find(t => t.value === type)
      return found ? found.tag : ''
    },
    // Certificate
    openCertificate(row) {
      const userInfo = this.$store.getters.userInfo
      this.certData = {
        ...row,
        bizTypeLabel: this.bizLabel(row.bizType),
        enterpriseName: row.enterpriseName || (userInfo && userInfo.enterpriseName) || '--',
        contractAddress: row.contractAddress || '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb',
        certTitle: this.bizLabel(row.bizType) + '存证数字证书'
      }
      this.certVisible = true
    },
    // Retry
    handleRetry(row) {
      this.$confirm('确定重新上链该记录？', '提示', { type: 'warning' }).then(() => {
        this.$set(row, '_retrying', true)
        retryChain(row.id).then(() => {
          this.$message.success('重新上链成功')
          this.fetchList()
        }).catch(() => {
          this.$message.error('重新上链失败，请稍后重试')
        }).finally(() => {
          this.$set(row, '_retrying', false)
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;
$primary-dark: #1B5E20;

.page-container { padding: 4px; }
.stat-row { margin-bottom: 16px; }

.summary-card {
  border-radius: 8px;
  .card-body {
    display: flex; align-items: center; gap: 16px;
    .card-icon {
      width: 52px; height: 52px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      i { font-size: 24px; color: $primary; }
    }
    .card-info {
      .label { font-size: 13px; color: #909399; margin: 0 0 4px; }
      .value { font-size: 24px; font-weight: 600; color: #303133; margin: 0; }
    }
  }
}

.table-card {
  border-radius: 8px;
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    font-weight: 600; color: #1a3a2a;
    .header-right { display: flex; gap: 8px; align-items: center; }
  }
}

.pagination-wrapper {
  display: flex; justify-content: flex-end; margin-top: 16px;
}

.tx-hash {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #1976D2;
  cursor: pointer;
  &:hover { text-decoration: underline; }
  .copy-icon { margin-left: 4px; font-size: 12px; color: #909399; }
}

/* ========== Certificate Dialog ========== */
::v-deep .cert-dialog {
  border-radius: 12px;
  .el-dialog__header { display: none; }
  .el-dialog__body { padding: 20px; }
}

.certificate-card {
  .cert-border {
    position: relative;
    border: 3px double $primary-dark;
    border-radius: 10px;
    padding: 36px 28px;
    background: #fff;
    overflow: hidden;
  }
}

.cert-watermark {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%) rotate(-45deg);
  font-size: 60px; font-weight: bold;
  color: rgba(46, 125, 50, 0.03);
  letter-spacing: 10px;
  pointer-events: none; z-index: 0;
}

.cert-header {
  position: relative; z-index: 1;
  text-align: center; margin-bottom: 28px;
  .cert-title {
    font-size: 22px; font-weight: bold; color: $primary-dark;
    letter-spacing: 3px; margin: 0 0 8px;
  }
  .cert-subtitle {
    font-size: 11px; color: #888; letter-spacing: 2px; margin: 0;
  }
  .cert-divider {
    width: 100px; height: 2px; margin: 14px auto 0;
    background: linear-gradient(90deg, transparent, $primary-dark, transparent);
  }
}

.cert-body { position: relative; z-index: 1; }

.info-section { margin-bottom: 20px; }

.section-label {
  font-size: 15px; font-weight: 600; color: $primary-dark;
  margin-bottom: 12px; padding-left: 10px;
  border-left: 3px solid #4CAF50;
  &.highlight { color: $primary-dark; border-left-color: $primary-dark; }
}

.info-grid {
  display: flex; flex-wrap: wrap; gap: 10px;
  .info-item {
    flex: 1 1 45%; background: #f5f5f5; border-radius: 6px; padding: 10px 12px;
    &.full { flex: 1 1 100%; }
    .info-key { display: block; font-size: 12px; color: #999; margin-bottom: 4px; }
    .info-val { display: block; font-size: 14px; color: #333; font-weight: 500; }
  }
}

.blockchain-section {
  background: linear-gradient(135deg, #E8F5E9, #F1F8E9);
  border-radius: 8px; padding: 18px; margin-bottom: 20px;
  border: 1px solid #C8E6C9;
}

.param-row {
  margin-bottom: 14px;
  &:last-child { margin-bottom: 0; }
  .param-label {
    font-size: 13px; color: #666; margin-bottom: 6px;
    display: flex; align-items: center; gap: 6px;
  }
  .param-value {
    background: #fff; border-radius: 6px; padding: 10px 12px;
    border: 1px solid #e0e0e0; font-size: 13px; color: #333;
    font-family: 'Courier New', monospace; word-break: break-all;
    &.hash {
      color: #1976D2; cursor: pointer;
      &:hover { background: #f5f5f5; }
      i { margin-left: 6px; font-size: 12px; color: #999; }
    }
  }
}

.label-badge {
  display: inline-block;
  width: 24px; height: 24px; line-height: 24px;
  background: $primary-dark; color: #fff;
  font-size: 10px; font-weight: bold;
  border-radius: 4px; text-align: center; flex-shrink: 0;
}

.statement {
  background: linear-gradient(135deg, #FFF9C4, #FFF59D);
  border-left: 3px solid #FBC02D;
  border-radius: 6px; padding: 14px; margin-bottom: 20px;
  .statement-title { font-size: 13px; font-weight: 600; color: #F57F17; margin-bottom: 8px; }
  p { font-size: 12px; color: #5D4037; line-height: 1.8; margin: 0; text-align: justify; }
}

.cert-footer {
  position: relative; z-index: 1;
  text-align: center; padding-top: 16px;
  .footer-line {
    width: 100%; height: 1px; margin-bottom: 12px;
    background: linear-gradient(90deg, transparent, $primary-dark, transparent);
  }
  p { font-size: 12px; color: #888; margin: 0 0 6px; }
  .footer-date { font-family: 'Courier New', monospace; font-size: 11px; color: #aaa; }
}

.seal {
  position: absolute; bottom: 50px; right: 36px; z-index: 2;
  transform: rotate(15deg);
  .seal-circle {
    width: 80px; height: 80px;
    border: 3px dashed rgba(211, 47, 47, 0.6);
    border-radius: 50%;
    display: flex; flex-direction: column;
    align-items: center; justify-content: center;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 2px 6px rgba(211, 47, 47, 0.2);
    span { color: rgba(211, 47, 47, 0.8); font-weight: bold; line-height: 1.2; }
    .seal-top { font-size: 11px; }
    .seal-center { font-size: 18px; margin: 1px 0; }
    .seal-bottom { font-size: 9px; }
  }
}
</style>
