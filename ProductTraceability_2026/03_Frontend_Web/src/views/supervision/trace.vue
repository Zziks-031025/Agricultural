<template>
  <div class="trace-page">
    <!-- 搜索区 -->
    <div class="search-section">
      <div class="search-box">
        <h2 class="search-title">
          <i class="el-icon-search"></i> 溯源查询
        </h2>
        <p class="search-desc">输入批次号或溯源码，查看产品全生命周期溯源信息</p>
        <div class="search-row">
          <el-input
            v-model="keyword"
            placeholder="请输入批次号 / 溯源码"
            clearable
            size="large"
            class="search-input"
            @keyup.enter.native="handleSearch"
          >
            <i slot="prefix" class="el-icon-search" style="line-height:40px;margin-left:4px;"></i>
          </el-input>
          <el-button type="primary" size="medium" :loading="loading" @click="handleSearch">
            查询溯源
          </el-button>
        </div>
      </div>
    </div>

    <!-- 批次概览 -->
    <el-card v-if="traceData" shadow="never" class="summary-card">
      <div class="batch-header">
        <div class="batch-left">
          <h3>{{ traceData.batchInfo.productName }}</h3>
          <span class="batch-code">{{ traceData.batchInfo.batchCode }}</span>
          <el-tag size="mini" :type="statusTagType(traceData.batchInfo.status)">
            {{ traceData.batchInfo.statusText }}
          </el-tag>
        </div>
        <div class="batch-actions">
          <el-button size="small" plain @click="handleVerify" :loading="verifying">
            <i class="el-icon-connection"></i> 区块链验证
          </el-button>
          <el-button size="small" type="primary" plain @click="handlePrint">
            <i class="el-icon-printer"></i> 打印档案
          </el-button>
        </div>
      </div>
      <el-descriptions :column="5" size="mini" border>
        <el-descriptions-item label="品种">{{ traceData.batchInfo.breed || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ traceData.batchInfo.quantity }} {{ traceData.batchInfo.unit }}</el-descriptions-item>
        <el-descriptions-item label="产地">{{ traceData.batchInfo.originLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ traceData.batchInfo.manager || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源企业">{{ traceData.batchInfo.enterpriseName || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 时间轴 -->
    <el-card v-if="traceData && traceData.timeline && traceData.timeline.length" shadow="never" class="timeline-card" id="print-area">
      <div slot="header" class="timeline-card-header">
        <span>全生命周期溯源时间轴</span>
        <span class="node-count">共 {{ traceData.timeline.length }} 个节点</span>
      </div>

      <el-timeline class="trace-timeline">
        <el-timeline-item
          v-for="(node, idx) in traceData.timeline"
          :key="idx"
          :timestamp="node.time"
          :type="stageColor(node.stage)"
          :icon="stageIcon(node.stage)"
          size="large"
          placement="top"
        >
          <el-card shadow="hover" class="node-card" :class="'stage-' + node.stage">
            <!-- 节点头部 -->
            <div class="node-header">
              <span class="node-title">{{ node.title }}</span>
              <span v-if="node.operator" class="node-operator">操作人: {{ node.operator }}</span>
              <div class="node-tags">
                <el-tag v-if="node.official" type="warning" size="mini" effect="plain">官方背书</el-tag>
                <el-tag v-if="node.txHash" type="success" size="mini" effect="plain">
                  已上链
                </el-tag>
                <el-tag v-else type="info" size="mini" effect="plain">未上链</el-tag>
              </div>
            </div>

            <!-- 节点详情 -->
            <div class="node-details">
              <el-descriptions :column="3" size="mini" :border="false">
                <el-descriptions-item
                  v-for="(val, key) in node.details"
                  :key="key"
                  :label="key"
                >
                  <template v-if="key === '检疫结果'">
                    <el-tag :type="val === '合格' ? 'success' : 'danger'" size="mini">{{ val }}</el-tag>
                  </template>
                  <template v-else-if="val !== null && val !== ''">{{ val }}</template>
                  <template v-else>-</template>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 图片展示(折叠) -->
            <div v-if="node.images && node.images.length" class="node-images">
              <el-collapse>
                <el-collapse-item :title="'现场照片 (' + node.images.length + '张)'">
                  <div class="image-gallery">
                    <el-image
                      v-for="(img, imgIdx) in node.images"
                      :key="imgIdx"
                      :src="resolveImg(img)"
                      :preview-src-list="node.images.map(resolveImg)"
                      fit="cover"
                      class="gallery-img"
                    />
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>

            <!-- TxHash -->
            <div v-if="node.txHash" class="node-chain">
              <span class="chain-label">TxHash:</span>
              <span class="chain-hash" :title="node.txHash">{{ node.txHash }}</span>
              <span v-if="node.blockNumber" class="chain-block">Block #{{ node.blockNumber }}</span>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 空状态 -->
    <el-card v-if="searched && !traceData" shadow="never" class="empty-card">
      <el-empty description="未找到该批次的溯源信息" />
    </el-card>

    <!-- 区块链验证弹窗 -->
    <el-dialog title="区块链验证结果" :visible.sync="verifyDialogVisible" custom-class="verify-dialog">
      <div v-if="verifyResult" class="verify-result">
        <div class="verify-icon" :class="verifyResult.valid ? 'valid' : 'invalid'">
          <i :class="verifyResult.valid ? 'el-icon-circle-check' : 'el-icon-circle-close'"></i>
        </div>
        <h3>{{ verifyResult.valid ? '数据验证通过' : '数据存在异常' }}</h3>
        <p>{{ verifyResult.message }}</p>
        <el-table v-if="verifyResult.details && verifyResult.details.length" :data="verifyResult.details" border size="mini" style="margin-top:16px">
          <el-table-column prop="nodeName" label="环节" width="120" />
          <el-table-column prop="status" label="验证状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="row.status === 'valid' ? 'success' : 'danger'" size="mini">
                {{ row.status === 'valid' ? '通过' : '异常' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="txHash" label="交易哈希">
            <template slot-scope="{ row }">
              <span style="word-break: break-all; font-family: monospace; font-size: 12px">{{ row.txHash || '--' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getTraceDetail, verifyBlockchain } from '@/api/trace'

export default {
  name: 'SupervisionTrace',
  data() {
    return {
      keyword: '',
      loading: false,
      verifying: false,
      searched: false,
      traceData: null,
      verifyDialogVisible: false,
      verifyResult: null
    }
  },
  methods: {
    handleSearch() {
      const q = this.keyword.trim()
      if (!q) {
        this.$message.warning('请输入批次号或溯源码')
        return
      }
      this.loading = true
      this.searched = true
      getTraceDetail({ batchId: q }).then(res => {
        this.traceData = res.data
      }).catch(() => {
        this.traceData = null
      }).finally(() => {
        this.loading = false
      })
    },
    handleVerify() {
      if (!this.traceData || !this.traceData.batchInfo) return
      this.verifying = true
      const info = this.traceData.batchInfo
      verifyBlockchain({
        batchId: String(info.id || info.batchCode),
        batchCode: info.batchCode
      }).then(res => {
        this.verifyResult = res.data
        this.verifyDialogVisible = true
      }).catch(() => {
        this.$message.error('区块链验证请求失败')
      }).finally(() => {
        this.verifying = false
      })
    },
    handlePrint() {
      window.print()
    },
    resolveImg(url) {
      if (!url) return ''
      if (url.startsWith('http://') || url.startsWith('https://')) return url
      const baseUrl = process.env.VUE_APP_BASE_API || ''
      if (url.startsWith('/')) {
        return baseUrl + url
      }
      return baseUrl + '/' + url
    },
    stageColor(stage) {
      const map = {
        batch_init: 'primary',
        growth: 'success',
        inspection: 'warning',
        processing: '',
        storage: 'info',
        transport: 'primary',
        sale: 'success'
      }
      return map[stage] || ''
    },
    stageIcon(stage) {
      const map = {
        batch_init: 'el-icon-document-add',
        growth: 'el-icon-sunny',
        inspection: 'el-icon-s-claim',
        processing: 'el-icon-s-tools',
        storage: 'el-icon-house',
        transport: 'el-icon-truck',
        sale: 'el-icon-sell'
      }
      return map[stage] || 'el-icon-more'
    },
    statusTagType(status) {
      if (status >= 8) return 'success'
      if (status >= 4) return 'warning'
      return 'primary'
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;

.trace-page {
  padding: 4px;
}

/* -- 搜索区 -- */
.search-section {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #f0f9f4 0%, #e8f5e9 100%);
  border-radius: 8px;
  padding: 32px 40px;
}
.search-box {
  max-width: 700px;
  margin: 0 auto;
  text-align: center;
}
.search-title {
  font-size: 22px;
  color: #303133;
  margin: 0 0 8px;
  i { color: $primary; }
}
.search-desc {
  color: #909399;
  font-size: 14px;
  margin: 0 0 20px;
}
.search-row {
  display: flex;
  gap: 12px;
  justify-content: center;
}
.search-input {
  width: 480px;
  ::v-deep .el-input__inner {
    height: 44px;
    line-height: 44px;
    font-size: 15px;
    border-radius: 6px;
  }
}

/* -- 批次概览 -- */
.summary-card {
  margin-bottom: 16px;
  .batch-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }
  .batch-left {
    display: flex;
    align-items: center;
    gap: 12px;
    h3 { margin: 0; font-size: 18px; color: #303133; }
    .batch-code { font-family: monospace; font-size: 13px; color: $primary; }
  }
}

/* -- 时间轴卡片 -- */
.timeline-card {
  .timeline-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    .node-count { font-size: 13px; color: #909399; font-weight: normal; }
  }
}

.trace-timeline {
  padding: 20px 0 0 0;
}

.node-card {
  .node-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
    flex-wrap: wrap;
    .node-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }
    .node-operator {
      font-size: 12px;
      color: #909399;
    }
    .node-tags {
      margin-left: auto;
      display: flex;
      gap: 6px;
    }
  }
  .node-details {
    margin-bottom: 8px;
  }
  .node-images {
    margin-bottom: 8px;
    ::v-deep .el-collapse-item__header {
      font-size: 13px;
      color: #606266;
      height: 36px;
      line-height: 36px;
    }
  }
  .image-gallery {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 8px 0;
  }
  .gallery-img {
    width: 100px;
    height: 100px;
    border-radius: 4px;
    border: 1px solid #ebeef5;
    cursor: pointer;
  }
  .node-chain {
    padding-top: 8px;
    border-top: 1px dashed #ebeef5;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
    .chain-label { color: #909399; }
    .chain-hash {
      font-family: monospace;
      color: $primary;
      word-break: break-all;
      flex: 1;
    }
    .chain-block {
      color: #909399;
      white-space: nowrap;
    }
  }
}

/* stage left border accent */
.node-card.stage-batch_init { border-left: 3px solid #409EFF; }
.node-card.stage-growth { border-left: 3px solid #67C23A; }
.node-card.stage-inspection { border-left: 3px solid #E6A23C; }
.node-card.stage-processing { border-left: 3px solid #909399; }
.node-card.stage-storage { border-left: 3px solid #909399; }
.node-card.stage-transport { border-left: 3px solid #409EFF; }
.node-card.stage-sale { border-left: 3px solid #67C23A; }

/* -- 验证弹窗 -- */
.verify-result {
  text-align: center;
  .verify-icon {
    font-size: 60px;
    margin-bottom: 12px;
    &.valid { color: $primary; }
    &.invalid { color: #F56C6C; }
  }
  h3 { margin: 0 0 8px; color: #303133; }
  p { color: #909399; font-size: 14px; }
}

.empty-card { text-align: center; }
</style>

<!-- 验证弹窗自适应宽度(全局) -->
<style>
.verify-dialog {
  width: fit-content;
  min-width: 500px;
  max-width: 90vw;
}
</style>

<!-- 打印样式(全局) -->
<style>
@media print {
  body * { visibility: hidden; }
  #print-area, #print-area * { visibility: visible; }
  #print-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }
  .search-section,
  .summary-card .batch-actions,
  .el-dialog__wrapper,
  .el-message,
  .el-collapse-item__arrow {
    display: none !important;
  }
  .node-card {
    break-inside: avoid;
    page-break-inside: avoid;
    box-shadow: none !important;
    border: 1px solid #ddd !important;
  }
  .el-collapse-item__wrap {
    display: block !important;
    height: auto !important;
  }
  .image-gallery .gallery-img {
    width: 80px;
    height: 80px;
  }
}
</style>
