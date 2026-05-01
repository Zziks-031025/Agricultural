<template>
  <div class="page-container">
    <!-- 查询区 -->
    <el-card shadow="never" class="query-card">
      <div slot="header" class="card-header">
        <span>数据完整性校验</span>
      </div>
      <el-form :inline="true" size="small">
        <el-form-item label="批次号">
          <el-input v-model="batchCode" placeholder="请输入批次号" clearable style="width: 260px" @keyup.enter.native="handleVerify" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-check" :loading="verifying" @click="handleVerify">校验</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" style="margin-top: 12px" />
    </el-card>

    <!-- 校验结果 -->
    <el-card v-if="result" shadow="never" class="result-card">
      <div slot="header" class="card-header">
        <span>校验结果</span>
        <el-tag :type="result.verified ? 'success' : 'danger'" size="medium">
          <i :class="result.verified ? 'el-icon-circle-check' : 'el-icon-circle-close'" style="margin-right: 4px"></i>
          {{ result.verified ? '数据一致 — 未被篡改' : '数据不一致 — 可能被篡改' }}
        </el-tag>
      </div>

      <el-descriptions :column="1" border size="medium" label-class-name="desc-label">
        <el-descriptions-item label="批次号">{{ result.batchCode }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ result.productName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="所属企业">{{ result.enterpriseName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="链下数据哈希 (SHA-256)">
          <span class="hash-text">{{ result.localHash || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="链上数据哈希 (区块链)">
          <span class="hash-text">{{ result.chainHash || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="交易哈希">
          <span class="hash-text">{{ result.txHash || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="区块高度">{{ result.blockNumber || '--' }}</el-descriptions-item>
        <el-descriptions-item label="上链时间">{{ result.chainTime || '--' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 对比面板 -->
      <div class="compare-panel" v-if="result.localHash && result.chainHash">
        <div class="compare-item">
          <div class="compare-label">链下哈希</div>
          <div class="compare-hash" :class="{ match: result.verified }">{{ result.localHash }}</div>
        </div>
        <div class="compare-arrow">
          <i :class="result.verified ? 'el-icon-check' : 'el-icon-close'" :style="{ color: result.verified ? '#2d8a56' : '#f56c6c', fontSize: '24px' }"></i>
        </div>
        <div class="compare-item">
          <div class="compare-label">链上哈希</div>
          <div class="compare-hash" :class="{ match: result.verified }">{{ result.chainHash }}</div>
        </div>
      </div>

      <!-- 各环节校验详情表格 -->
      <div v-if="result.nodeResults && result.nodeResults.length > 0" style="margin-top: 20px;">
        <div style="font-weight: 600; margin-bottom: 12px; color: #1a3a2a;">各环节校验详情</div>
        <el-table :data="result.nodeResults" border size="small" style="width: 100%">
          <el-table-column prop="nodeName" label="环节名称" width="120" />
          <el-table-column prop="stage" label="环节类型" width="140" />
          <el-table-column label="校验状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="row.verified ? 'success' : 'danger'" size="mini">
                {{ row.verified ? '通过' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="交易哈希" min-width="200" show-overflow-tooltip>
            <template slot-scope="{ row }">
              <span class="hash-text-sm">{{ row.txHash || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="官方认证" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag v-if="row.official" type="warning" size="mini">官方</el-tag>
              <span v-else>--</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 批量校验表格 -->
    <el-card shadow="never" class="batch-card">
      <div slot="header" class="card-header">
        <span>批量校验记录</span>
        <el-button type="primary" size="small" icon="el-icon-refresh" @click="fetchBatchList">刷新</el-button>
      </div>

      <el-table v-loading="tableLoading" :data="tableData" border size="small" style="width: 100%">
        <el-table-column prop="batchCode" label="批次号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="productName" label="产品名称" min-width="100" />
        <el-table-column prop="enterpriseName" label="企业" min-width="130" show-overflow-tooltip />
        <el-table-column label="链下哈希" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }"><span class="hash-text-sm">{{ row.dataHash || '--' }}</span></template>
        </el-table-column>
        <el-table-column label="交易哈希" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }"><span class="hash-text-sm">{{ row.txHash || '--' }}</span></template>
        </el-table-column>
        <el-table-column label="上链状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.txHash" type="success" size="mini">已上链</el-tag>
            <el-tag v-else type="info" size="mini">未上链</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" icon="el-icon-check" @click="verifyRow(row)" :disabled="!row.txHash">校验</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :total="tableTotal"
        :page-size="tableParams.size"
        :current-page.sync="tableParams.current"
        @current-change="fetchBatchList"
      />
    </el-card>
  </div>
</template>

<script>
import { verifyBlockchain } from '@/api/trace'
import request from '@/utils/request'

export default {
  name: 'SupervisionIntegrity',
  data() {
    return {
      batchCode: '',
      verifying: false,
      result: null,
      errorMsg: '',
      tableLoading: false,
      tableData: [],
      tableTotal: 0,
      tableParams: { current: 1, size: 10 }
    }
  },
  created() {
    this.fetchBatchList()
  },
  methods: {
    handleVerify() {
      if (!this.batchCode.trim()) {
        this.$message.warning('请输入批次号')
        return
      }
      this.verifying = true
      this.result = null
      this.errorMsg = ''

      verifyBlockchain({ batchCode: this.batchCode.trim() }).then(res => {
        this.result = res.data
      }).catch(err => {
        this.errorMsg = err.message || '校验失败，请检查批次号是否正确'
      }).finally(() => { this.verifying = false })
    },
    verifyRow(row) {
      this.batchCode = row.batchCode
      this.handleVerify()
    },
    fetchBatchList() {
      this.tableLoading = true
      request({
        url: '/api/batch/list',
        method: 'get',
        params: { ...this.tableParams }
      }).then(res => {
        const data = res.data
        if (data.records) {
          this.tableData = data.records
          this.tableTotal = data.total || 0
        } else if (Array.isArray(data)) {
          this.tableData = data
          this.tableTotal = data.length
        }
      }).catch(() => {
        this.tableData = []
      }).finally(() => { this.tableLoading = false })
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;

.page-container { padding: 4px; }
.query-card, .result-card, .batch-card { border-radius: 8px; margin-bottom: 16px; }

.card-header {
  display: flex; align-items: center; justify-content: space-between;
  font-weight: 600; color: #1a3a2a;
}

.hash-text { font-family: monospace; font-size: 12px; color: #606266; word-break: break-all; }
.hash-text-sm { font-family: monospace; font-size: 11px; color: #909399; }

.compare-panel {
  display: flex; align-items: center; gap: 16px; margin-top: 20px;
  padding: 16px; background: #fafafa; border-radius: 8px;

  .compare-item { flex: 1; }
  .compare-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
  .compare-hash {
    font-family: monospace; font-size: 12px; padding: 8px 12px;
    background: #fff; border: 1px solid #e4e7ed; border-radius: 4px;
    word-break: break-all;
    &.match { border-color: $primary; background: #f0f9eb; }
  }
  .compare-arrow { flex-shrink: 0; text-align: center; padding: 0 8px; }
}

.pagination { margin-top: 16px; text-align: right; }

::v-deep .desc-label { width: 160px !important; font-weight: 600; }
</style>
