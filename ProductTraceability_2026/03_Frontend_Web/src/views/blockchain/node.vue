<template>
  <div class="page-container">
    <!-- 节点连接状态 -->
    <el-card shadow="never" class="status-card">
      <div slot="header" class="card-header">
        <span>节点连接状态</span>
        <el-button type="primary" size="small" icon="el-icon-refresh" :loading="loading" @click="fetchNodeStatus">刷新</el-button>
      </div>

      <el-descriptions v-if="nodeInfo" :column="2" border size="medium" label-class-name="desc-label">
        <el-descriptions-item label="RPC 连接">
          <el-tag :type="nodeInfo.rpcReachable ? 'success' : 'danger'" size="small">
            {{ nodeInfo.rpcReachable ? '已连接' : '未连接' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="RPC 地址">
          <span class="mono-text">{{ nodeInfo.rpcUrl || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="客户端版本">{{ nodeInfo.clientVersion || '--' }}</el-descriptions-item>
        <el-descriptions-item label="链 ID">{{ nodeInfo.chainId != null ? nodeInfo.chainId : '--' }}</el-descriptions-item>
        <el-descriptions-item label="最新区块高度">{{ nodeInfo.latestBlockNumber != null ? nodeInfo.latestBlockNumber : '--' }}</el-descriptions-item>
        <el-descriptions-item label="最新区块时间">{{ formatBlockTime(nodeInfo.latestBlockTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-empty v-else-if="!loading" description="无法获取节点信息，请检查区块链节点是否启动" />
    </el-card>

    <!-- 合约部署状态 -->
    <el-card shadow="never" class="status-card">
      <div slot="header" class="card-header"><span>合约部署状态</span></div>

      <el-descriptions v-if="nodeInfo" :column="2" border size="medium" label-class-name="desc-label">
        <el-descriptions-item label="合约地址配置">
          <el-tag :type="nodeInfo.contractConfigured ? 'success' : 'warning'" size="small">
            {{ nodeInfo.contractConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="合约部署状态">
          <el-tag :type="nodeInfo.contractDeployed ? 'success' : 'info'" size="small">
            {{ nodeInfo.contractDeployed ? '已部署' : '未部署' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="合约地址" :span="2">
          <span class="mono-text">{{ nodeInfo.contractAddress || '--' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script>
import { getBlockchainNodeStatus } from '@/api/trace'

export default {
  name: 'BlockchainNode',
  data() {
    return {
      loading: false,
      nodeInfo: null
    }
  },
  created() {
    this.fetchNodeStatus()
  },
  methods: {
    fetchNodeStatus() {
      this.loading = true
      getBlockchainNodeStatus().then(res => {
        this.nodeInfo = res.data
      }).catch(() => {
        this.nodeInfo = null
      }).finally(() => {
        this.loading = false
      })
    },
    formatBlockTime(time) {
      if (!time) return '--'
      return time.replace('T', ' ')
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.status-card { border-radius: 8px; margin-bottom: 16px; }
.card-header {
  display: flex; align-items: center; justify-content: space-between;
  font-weight: 600; color: #1a3a2a;
}
.mono-text { font-family: monospace; font-size: 12px; color: #606266; word-break: break-all; }
::v-deep .desc-label { width: 130px !important; font-weight: 600; }
</style>
