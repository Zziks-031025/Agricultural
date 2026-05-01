<template>
  <div class="page-container">
    <!-- 合约概览 -->
    <el-card shadow="never" class="overview-card">
      <div slot="header" class="card-header">
        <span>智能合约管理</span>
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="fetchContractInfo">刷新状态</el-button>
      </div>

      <el-descriptions v-if="contractInfo" :column="2" border size="medium" label-class-name="desc-label">
        <el-descriptions-item label="合约名称">{{ contractInfo.contractName || 'Traceability' }}</el-descriptions-item>
        <el-descriptions-item label="部署状态">
          <el-tag :type="contractInfo.deployed ? 'success' : 'danger'" size="small">
            {{ contractInfo.deployed ? '已部署' : '未部署' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="合约地址" :span="2">
          <span class="mono-text">{{ contractInfo.contractAddress || '--' }}</span>
          <el-button v-if="contractInfo.contractAddress" type="text" size="mini" icon="el-icon-copy-document"
            @click="copyText(contractInfo.contractAddress)" style="margin-left: 8px">复制</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="部署交易哈希" :span="2">
          <span class="mono-text">{{ contractInfo.deployTxHash || '--' }}</span>
          <el-button v-if="contractInfo.deployTxHash" type="text" size="mini" icon="el-icon-copy-document"
            @click="copyText(contractInfo.deployTxHash)" style="margin-left: 8px">复制</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="网络">{{ contractInfo.network || 'Ethereum (Ganache)' }}</el-descriptions-item>
        <el-descriptions-item label="部署区块高度">{{ contractInfo.deployBlockNumber || '--' }}</el-descriptions-item>
        <el-descriptions-item label="部署时间">{{ contractInfo.deployTime || '--' }}</el-descriptions-item>
        <el-descriptions-item label="合约状态">
          <el-tag :type="contractInfo.status === 1 ? 'success' : 'info'" size="small">
            {{ contractInfo.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-empty v-else-if="!loading" description="暂无合约信息，请部署合约后刷新" />
    </el-card>

    <!-- 合约函数列表 -->
    <el-card shadow="never" class="func-card">
      <div slot="header" class="card-header"><span>合约函数</span></div>
      <el-table :data="functions" border size="small" style="width: 100%">
        <el-table-column prop="name" label="函数名" min-width="180">
          <template slot-scope="{ row }">
            <span class="func-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.type === 'write' ? 'warning' : 'success'" size="mini">{{ row.type === 'write' ? '写入' : '查询' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="功能描述" min-width="260" />
        <el-table-column prop="params" label="参数" min-width="200" show-overflow-tooltip>
          <template slot-scope="{ row }">
            <span class="mono-text-sm">{{ row.params }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 授权操作员管理 (暂不在本轮需求范围内，隐藏) -->
    <el-card v-if="false" shadow="never" class="operator-card">
      <div slot="header" class="card-header">
        <span>授权操作员</span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="showAddOperator">添加操作员</el-button>
      </div>

      <el-table v-loading="opLoading" :data="operators" border size="small" style="width: 100%" :resizable="false">
        <el-table-column prop="address" label="钱包地址" min-width="400" show-overflow-tooltip :resizable="false">
          <template slot-scope="{ row }">
            <span class="mono-text">{{ row.address }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="备注名" min-width="150" show-overflow-tooltip :resizable="false" />
        <el-table-column prop="addTime" label="授权时间" width="170" :resizable="false" />
        <el-table-column label="状态" width="100" align="center" :resizable="false">
          <template slot-scope="{ row }">
            <el-tag :type="row.authorized ? 'success' : 'info'" size="mini">{{ row.authorized ? '已授权' : '已撤销' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" :resizable="false">
          <template slot-scope="{ row }">
            <el-button v-if="row.authorized" type="text" size="mini" class="danger-btn" @click="handleRevokeOperator(row)">撤销</el-button>
            <el-button v-else type="text" size="mini" @click="handleReauthorize(row)">重新授权</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加操作员弹窗 (暂不在本轮需求范围内，隐藏) -->
    <el-dialog v-if="false" title="添加操作员" :visible.sync="addOpVisible" width="460px">
      <el-form ref="opForm" :model="opForm" :rules="opRules" label-width="90px" size="small">
        <el-form-item label="钱包地址" prop="address">
          <el-input v-model="opForm.address" placeholder="0x开头的以太坊地址" />
        </el-form-item>
        <el-form-item label="备注名">
          <el-input v-model="opForm.name" placeholder="操作员名称/企业名" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="addOpVisible = false">取消</el-button>
        <el-button type="primary" size="small" :loading="opSubmitting" @click="handleAddOperator">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'BlockchainContract',
  data() {
    return {
      loading: false,
      contractInfo: null,
      functions: [
        { name: 'uploadHash(string, string)', type: 'write', description: '上传溯源数据哈希到区块链', params: 'traceId, dataHash' },
        { name: 'queryHash(string)', type: 'read', description: '查询指定溯源ID的链上哈希', params: 'traceId' },
        { name: 'verifyData(string, string)', type: 'read', description: '验证数据哈希与链上记录是否一致', params: 'traceId, dataHash' },
        { name: 'getRecord(string)', type: 'read', description: '获取完整的溯源记录信息', params: 'traceId' },
        { name: 'addOperator(address)', type: 'write', description: '添加授权操作员地址', params: 'operatorAddress' },
        { name: 'removeOperator(address)', type: 'write', description: '移除授权操作员', params: 'operatorAddress' },
        { name: 'isOperator(address)', type: 'read', description: '检查地址是否为授权操作员', params: 'operatorAddress' },
        { name: 'transferOwnership(address)', type: 'write', description: '转移合约所有权', params: 'newOwner' }
      ],
      opLoading: false,
      operators: [],
      addOpVisible: false,
      opSubmitting: false,
      opForm: { address: '', name: '' },
      opRules: {
        address: [
          { required: true, message: '请输入钱包地址', trigger: 'blur' },
          { pattern: /^0x[a-fA-F0-9]{40}$/, message: '请输入有效的以太坊地址', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchContractInfo()
    this.fetchOperators()
  },
  methods: {
    fetchContractInfo() {
      this.loading = true
      request({ url: '/api/blockchain/contract/info', method: 'get' }).then(res => {
        this.contractInfo = res.data
      }).catch(() => {
        this.contractInfo = null
      }).finally(() => { this.loading = false })
    },
    fetchOperators() {
      this.opLoading = true
      request({ url: '/api/blockchain/contract/operators', method: 'get' }).then(res => {
        this.operators = res.data || []
      }).catch(() => { this.operators = [] }).finally(() => { this.opLoading = false })
    },
    showAddOperator() {
      this.opForm = { address: '', name: '' }
      this.addOpVisible = true
    },
    handleAddOperator() {
      this.$refs.opForm.validate(valid => {
        if (!valid) return
        this.opSubmitting = true
        request({ url: '/api/blockchain/contract/operator/add', method: 'post', data: this.opForm }).then(() => {
          this.$message.success('操作员添加成功')
          this.addOpVisible = false
          this.fetchOperators()
        }).finally(() => { this.opSubmitting = false })
      })
    },
    handleRevokeOperator(row) {
      this.$confirm(`确定撤销操作员「${row.name || row.address}」的授权吗？`, '提示', { type: 'warning' }).then(() => {
        request({ url: '/api/blockchain/contract/operator/revoke', method: 'post', data: { address: row.address } }).then(() => {
          this.$message.success('已撤销授权')
          this.fetchOperators()
        })
      }).catch(() => {})
    },
    handleReauthorize(row) {
      request({ url: '/api/blockchain/contract/operator/add', method: 'post', data: { address: row.address, name: row.name } }).then(() => {
        this.$message.success('已重新授权')
        this.fetchOperators()
      })
    },
    copyText(text) {
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('已复制到剪贴板')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }

.overview-card, .func-card, .operator-card {
  border-radius: 8px; margin-bottom: 16px;
}

.card-header {
  display: flex; align-items: center; justify-content: space-between;
  font-weight: 600; color: #1a3a2a;
}

.mono-text { font-family: monospace; font-size: 12px; color: #606266; word-break: break-all; }
.mono-text-sm { font-family: monospace; font-size: 11px; color: #909399; }
.func-name { font-family: monospace; font-size: 12px; color: #409eff; font-weight: 600; }
.danger-btn { color: #f56c6c !important; }

::v-deep .desc-label { width: 120px !important; font-weight: 600; }
</style>
