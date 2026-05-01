<template>
  <div class="stats-page">
    <template v-if="enterpriseType !== null">
    <!-- 顶部指标卡片 - 管理员全局 -->
    <el-row v-if="enterpriseType === 0" :gutter="16" class="kpi-row">
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-active">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-s-cooperation"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">在养批次</p>
              <p class="kpi-value">{{ stats.activeBatchCount || 0 }}</p>
              <p class="kpi-sub">全平台"生长中"批次</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-livestock">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-s-goods"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">存栏总数</p>
              <p class="kpi-value">{{ formatNum(stats.totalLivestock) }}</p>
              <p class="kpi-sub">全平台在养合计</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-pass">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-circle-check"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">检疫合格率</p>
              <p class="kpi-value">{{ stats.passRate || 100 }}<span class="kpi-unit">%</span></p>
              <p class="kpi-sub">{{ stats.passCount || 0 }}/{{ stats.totalInspection || 0 }} 合格</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 顶部指标卡片 - 检疫企业 -->
    <el-row v-if="enterpriseType === 3" :gutter="16" class="kpi-row">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-active">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-document-checked"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">检疫批次</p>
              <p class="kpi-value">{{ stats.batchCount || 0 }}</p>
              <p class="kpi-sub">累计处理批次数</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-livestock">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-s-check"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">检疫记录</p>
              <p class="kpi-value">{{ stats.inspectionCount || 0 }}</p>
              <p class="kpi-sub">累计检疫记录数</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-pass">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-circle-check"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">检疫合格率</p>
              <p class="kpi-value">{{ stats.passRate || 100 }}<span class="kpi-unit">%</span></p>
              <p class="kpi-sub">已出结果的检疫</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-chain">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-link"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">累计上链</p>
              <p class="kpi-value">{{ stats.totalOnChain || 0 }}</p>
              <p class="kpi-sub">区块链存证</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 顶部指标卡片 - 养殖企业 -->
    <el-row v-if="enterpriseType === 1" :gutter="16" class="kpi-row">
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-active">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-s-cooperation"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">在养批次</p>
              <p class="kpi-value">{{ stats.activeBatchCount || 0 }}</p>
              <p class="kpi-sub">当前处于"生长中"的批次</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-livestock">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-s-goods"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">存栏总数</p>
              <p class="kpi-value">{{ formatNum(stats.totalLivestock) }}</p>
              <p class="kpi-sub">所有在养批次合计</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="kpi-card kpi-pass">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-circle-check"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">检疫合格率</p>
              <p class="kpi-value">{{ stats.passRate || 100 }}<span class="kpi-unit">%</span></p>
              <p class="kpi-sub">{{ stats.passCount || 0 }}/{{ stats.totalInspection || 0 }} 合格</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 顶部指标卡片 - 加工企业 -->
    <el-row v-if="enterpriseType === 2" :gutter="16" class="kpi-row">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-received">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-download"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">今日接收</p>
              <p class="kpi-value">{{ stats.todayReceived || 0 }}</p>
              <p class="kpi-sub">入库批次数</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-processed">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-s-tools"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">今日加工</p>
              <p class="kpi-value">{{ formatNum(stats.todayProcessed) }}</p>
              <p class="kpi-sub">产出数量</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-inventory">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-box"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">待销库存</p>
              <p class="kpi-value">{{ formatNum(stats.pendingSaleInventory) }}</p>
              <p class="kpi-sub">已加工未销售</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card kpi-chain">
          <div class="kpi-body">
            <div class="kpi-icon"><i class="el-icon-link"></i></div>
            <div class="kpi-info">
              <p class="kpi-label">累计上链</p>
              <p class="kpi-value">{{ stats.totalOnChain || 0 }}</p>
              <p class="kpi-sub">区块链存证</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <!-- 养殖企业：投入品使用统计 / 加工企业：加工量趋势 / 检疫企业：检疫记录趋势 -->
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="chart-header">
            <span v-if="enterpriseType === 1">近30天投入品使用统计</span>
            <span v-if="enterpriseType === 2">近7日加工量趋势</span>
            <span v-if="enterpriseType === 3">近30天检疫记录趋势</span>
          </div>
          <div ref="lineChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <!-- 养殖企业：批次状态分布 / 加工企业：原料转化率 / 检疫企业：检疫结果分布 -->
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="chart-header">
            <span v-if="enterpriseType === 1">当前批次状态占比</span>
            <span v-if="enterpriseType === 2">原料转化率</span>
            <span v-if="enterpriseType === 3">检疫结果分布</span>
          </div>
          <div ref="pieChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 上链活跃度 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="chart-header">
            <span>每日区块链存证笔数</span>
          </div>
          <div ref="barChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
    </template>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getAnalysisStats, getProcessorStats, getQuarantineStats } from '@/api/dashboard'

export default {
  name: 'AnalysisStatistics',
  data() {
    return {
      stats: {},
      lineInstance: null,
      pieInstance: null,
      barInstance: null
    }
  },
  computed: {
    enterpriseId() {
      const info = this.$store.state.user.userInfo
      return info ? info.enterpriseId : null
    },
    enterpriseType() {
      const info = this.$store.state.user.userInfo
      if (!info) return null
      // Admin (userType=1) without enterpriseType: treat as global view (type 0)
      if (info.userType === 1 && !info.enterpriseType) return 0
      return info.enterpriseType
    }
  },
  mounted() {
    this.fetchData()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.lineInstance) this.lineInstance.dispose()
    if (this.pieInstance) this.pieInstance.dispose()
    if (this.barInstance) this.barInstance.dispose()
  },
  methods: {
    fetchData() {
      const params = {}
      if (this.enterpriseId) params.enterpriseId = this.enterpriseId
      
      let apiCall
      if (this.enterpriseType === 3) {
        apiCall = getQuarantineStats(params)
      } else if (this.enterpriseType === 2) {
        apiCall = getProcessorStats(params)
      } else {
        // type 1 (breeding) or type 0 (admin global)
        apiCall = getAnalysisStats(params)
      }
      
      apiCall.then(res => {
        this.stats = res.data || {}
        console.log('统计数据:', this.stats)
        this.$nextTick(() => {
          this.renderLineChart()
          this.renderPieChart()
          this.renderBarChart()
        })
      }).catch(() => {
        this.$message.error('获取统计数据失败')
      })
    },
    renderLineChart() {
      const el = this.$refs.lineChart
      if (!el) return
      this.lineInstance = echarts.init(el)
      
      if (this.enterpriseType === 3) {
        const trend = this.stats.inspectionTrend30 || []
        this.lineInstance.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 50, right: 20, top: 30, bottom: 30 },
          xAxis: {
            type: 'category',
            data: trend.map(i => i.date),
            axisLabel: { rotate: 45, fontSize: 11 }
          },
          yAxis: {
            type: 'value',
            name: '检疫数',
            minInterval: 1,
            axisLabel: { fontSize: 11 }
          },
          series: [{
            name: '检疫记录',
            type: 'line',
            data: trend.map(i => i.amount),
            smooth: true,
            areaStyle: { opacity: 0.15 },
            itemStyle: { color: '#409eff' },
            lineStyle: { width: 2 }
          }]
        })
      } else if (this.enterpriseType === 2) {
        const trend = this.stats.processingTrend || []
        this.lineInstance.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 50, right: 20, top: 30, bottom: 30 },
          xAxis: {
            type: 'category',
            data: trend.map(i => i.date),
            axisLabel: { rotate: 45, fontSize: 11 }
          },
          yAxis: {
            type: 'value',
            name: '产出量',
            axisLabel: { fontSize: 11 }
          },
          series: [{
            name: '加工产出',
            type: 'line',
            data: trend.map(i => i.amount),
            smooth: true,
            areaStyle: { opacity: 0.15 },
            itemStyle: { color: '#e6a23c' },
            lineStyle: { width: 2 }
          }]
        })
      } else {
        const trend = this.stats.inputTrend || []
        this.lineInstance.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: 50, right: 20, top: 30, bottom: 30 },
          xAxis: {
            type: 'category',
            data: trend.map(i => i.date),
            axisLabel: { rotate: 45, fontSize: 11 }
          },
          yAxis: {
            type: 'value',
            name: '用量(kg)',
            axisLabel: { fontSize: 11 }
          },
          series: [{
            name: '投入品用量',
            type: 'line',
            data: trend.map(i => i.amount),
            smooth: true,
            areaStyle: { opacity: 0.15 },
            itemStyle: { color: '#2d8a56' },
            lineStyle: { width: 2 }
          }]
        })
      }
    },
    renderPieChart() {
      const el = this.$refs.pieChart
      if (!el) return
      this.pieInstance = echarts.init(el)
      
      if (this.enterpriseType === 3) {
        const dist = this.stats.resultDistribution || []
        this.pieInstance.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, textStyle: { fontSize: 12 } },
          color: ['#67C23A', '#F56C6C', '#E6A23C'],
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['50%', '45%'],
            avoidLabelOverlap: true,
            label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
            data: dist
          }]
        })
      } else if (this.enterpriseType === 2) {
        const rate = this.stats.conversionRate || []
        this.pieInstance.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c}' },
          legend: { bottom: 0, textStyle: { fontSize: 12 } },
          color: ['#409EFF', '#67C23A'],
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['50%', '45%'],
            label: { show: true, formatter: '{b}\n{c}', fontSize: 11 },
            data: rate
          }]
        })
      } else {
        const dist = this.stats.statusDistribution || []
        this.pieInstance.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          legend: { bottom: 0, textStyle: { fontSize: 12 } },
          color: ['#2d8a56', '#E6A23C', '#409EFF', '#67C23A', '#F56C6C', '#909399'],
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['50%', '45%'],
            avoidLabelOverlap: true,
            label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
            data: dist
          }]
        })
      }
    },
    renderBarChart() {
      const el = this.$refs.barChart
      if (!el) return
      this.barInstance = echarts.init(el)
      const activity = this.stats.chainActivity || []
      console.log('chainActivity数据:', activity)
      this.barInstance.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 30, bottom: 30 },
        xAxis: {
          type: 'category',
          data: activity.map(i => i.date),
          axisLabel: { rotate: 45, fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          name: '存证笔数',
          minInterval: 1,
          axisLabel: { fontSize: 11 }
        },
        series: [{
          name: '上链笔数',
          type: 'bar',
          data: activity.map(i => i.count),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#409EFF' },
              { offset: 1, color: '#79bbff' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          barMaxWidth: 20
        }]
      })
    },
    handleResize() {
      if (this.lineInstance) this.lineInstance.resize()
      if (this.pieInstance) this.pieInstance.resize()
      if (this.barInstance) this.barInstance.resize()
    },
    formatNum(val) {
      if (val == null) return '0'
      return Number(val).toLocaleString()
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;

.stats-page { padding: 4px; }

.kpi-row { margin-bottom: 16px; }

.kpi-card {
  border-radius: 8px;
  .kpi-body {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .kpi-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    color: #fff;
    flex-shrink: 0;
  }
  .kpi-info {
    flex: 1;
    .kpi-label { margin: 0; font-size: 13px; color: #909399; }
    .kpi-value {
      margin: 4px 0 2px;
      font-size: 28px;
      font-weight: 700;
      color: #303133;
      line-height: 1.2;
    }
    .kpi-unit { font-size: 16px; font-weight: 400; margin-left: 2px; }
    .kpi-sub { margin: 0; font-size: 12px; color: #c0c4cc; }
  }
  &.kpi-active .kpi-icon { background: linear-gradient(135deg, #2d8a56, #4caf50); }
  &.kpi-livestock .kpi-icon { background: linear-gradient(135deg, #409EFF, #79bbff); }
  &.kpi-pass .kpi-icon { background: linear-gradient(135deg, #67C23A, #95d475); }
  &.kpi-received .kpi-icon { background: linear-gradient(135deg, #409EFF, #79bbff); }
  &.kpi-processed .kpi-icon { background: linear-gradient(135deg, #e6a23c, #f0b86e); }
  &.kpi-inventory .kpi-icon { background: linear-gradient(135deg, #9b59b6, #b98cc7); }
  &.kpi-chain .kpi-icon { background: linear-gradient(135deg, #1abc9c, #48c9b0); }
}

.chart-card {
  .chart-header {
    font-weight: 600;
    font-size: 15px;
    color: #303133;
  }
}

.chart-box {
  width: 100%;
  height: 340px;
}
</style>
