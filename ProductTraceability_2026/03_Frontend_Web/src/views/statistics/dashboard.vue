<template>
  <div class="page-container">
    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :span="4" v-for="(card, idx) in kpiCards" :key="idx">
        <el-card shadow="hover" class="kpi-card" :class="card.cls">
          <div class="kpi-icon"><i :class="card.icon"></i></div>
          <div class="kpi-info">
            <p class="kpi-label">{{ card.label }}</p>
            <h3 class="kpi-value">{{ card.value }}</h3>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="card-header"><span>企业类型分布</span></div>
          <div ref="pieChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="card-header"><span>批次状态分布</span></div>
          <div ref="batchPieChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="card-header"><span>近12个月批次创建趋势</span></div>
          <div ref="lineChart" class="chart-box-lg"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="card-header"><span>上链数据统计</span></div>
          <div ref="chainBarChart" class="chart-box-lg"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/dashboard'
import { getBlockchainStats } from '@/api/trace'

export default {
  name: 'StatisticsDashboard',
  data() {
    return {
      kpiCards: [
        { label: '企业总数', value: 0, icon: 'el-icon-office-building', cls: 'kpi-green' },
        { label: '种植养殖', value: 0, icon: 'el-icon-s-cooperation', cls: 'kpi-lime' },
        { label: '加工宰杀', value: 0, icon: 'el-icon-s-tools', cls: 'kpi-orange' },
        { label: '检疫质检', value: 0, icon: 'el-icon-s-check', cls: 'kpi-blue' },
        { label: '批次总数', value: 0, icon: 'el-icon-document', cls: 'kpi-purple' },
        { label: '上链记录', value: 0, icon: 'el-icon-link', cls: 'kpi-cyan' }
      ],
      stats: null,
      chainStats: null
    }
  },
  mounted() {
    this.fetchData()
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
    this.disposeCharts()
  },
  methods: {
    fetchData() {
      getDashboardStats('admin').then(res => {
        this.stats = res.data || {}
        this.updateKPI()
        this.$nextTick(() => {
          this.renderPieChart()
          this.renderBatchPieChart()
          this.renderLineChart()
        })
      }).catch(() => {})

      getBlockchainStats().then(res => {
        this.chainStats = res.data || {}
        this.$nextTick(() => { this.renderChainBarChart() })
      }).catch(() => {})
    },
    updateKPI() {
      const s = this.stats
      this.kpiCards[0].value = (s.type1Count || 0) + (s.type2Count || 0) + (s.type3Count || 0)
      this.kpiCards[1].value = s.type1Count || 0
      this.kpiCards[2].value = s.type2Count || 0
      this.kpiCards[3].value = s.type3Count || 0
      this.kpiCards[4].value = s.totalBatches || 0
      this.kpiCards[5].value = s.totalOnChain || 0
    },
    renderPieChart() {
      const chart = echarts.init(this.$refs.pieChart)
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        color: ['#67c23a', '#e6a23c', '#409eff'],
        series: [{
          type: 'pie', radius: ['40%', '70%'], center: ['50%', '55%'],
          label: { formatter: '{b}\n{c}家' },
          data: [
            { name: '种植养殖', value: this.stats.type1Count || 0 },
            { name: '加工宰杀', value: this.stats.type2Count || 0 },
            { name: '检疫质检', value: this.stats.type3Count || 0 }
          ]
        }]
      })
      this._pieChart = chart
    },
    renderBatchPieChart() {
      const chart = echarts.init(this.$refs.batchPieChart)
      const statusData = this.stats.batchStatusCounts || []
      const statusMap = { 1: '初始化', 2: '生长中', 3: '已收获', 4: '加工中', 5: '已检疫', 6: '已入库', 7: '运输中', 8: '已销售', 9: '加工完成' }
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        color: ['#909399', '#e6a23c', '#67c23a', '#409eff', '#f56c6c', '#2d8a56', '#9b59b6', '#1abc9c'],
        series: [{
          type: 'pie', radius: ['40%', '70%'], center: ['50%', '55%'],
          label: { formatter: '{b}\n{c}批' },
          data: statusData.map(item => ({ name: statusMap[item.status] || '状态' + item.status, value: item.count || 0 }))
        }]
      })
      this._batchPieChart = chart
    },
    renderLineChart() {
      const chart = echarts.init(this.$refs.lineChart)
      const monthData = this.stats.monthlyBatches || []
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: monthData.map(m => m.month || m.label), axisLabel: { fontSize: 11 } },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
          lineStyle: { color: '#2d8a56', width: 2 },
          itemStyle: { color: '#2d8a56' },
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(45,138,86,0.3)' }, { offset: 1, color: 'rgba(45,138,86,0.02)' }]) },
          data: monthData.map(m => m.count || m.value || 0)
        }]
      })
      this._lineChart = chart
    },
    renderChainBarChart() {
      const chart = echarts.init(this.$refs.chainBarChart)
      const cs = this.chainStats || {}
      const stages = cs.stageDistribution || []
      
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 80, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { 
          type: 'category', 
          data: stages.map(s => s.name),
          axisLabel: { fontSize: 11 }
        },
        series: [{
          type: 'bar', barWidth: 18,
          itemStyle: { 
            borderRadius: [0, 4, 4, 0],
            color: (p) => {
              const colors = ['#2d8a56', '#3ca66b', '#52bf84', '#7dd4a0', '#a8e6bf', '#c8f0d4', '#e6a23c']
              return colors[p.dataIndex] || '#2d8a56'
            }
          },
          data: stages.map(s => s.value || 0)
        }]
      })
      this._chainBarChart = chart
    },
    resizeCharts() {
      [this._pieChart, this._batchPieChart, this._lineChart, this._chainBarChart].forEach(c => c && c.resize())
    },
    disposeCharts() {
      [this._pieChart, this._batchPieChart, this._lineChart, this._chainBarChart].forEach(c => c && c.dispose())
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }

.kpi-row { margin-bottom: 16px; }
.kpi-card {
  border-radius: 8px; border: none; padding: 0;
  ::v-deep .el-card__body { display: flex; align-items: center; gap: 12px; padding: 16px; }
  .kpi-icon {
    width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
    i { font-size: 22px; color: #fff; }
  }
  .kpi-label { font-size: 12px; color: #909399; margin: 0 0 2px; }
  .kpi-value { font-size: 22px; font-weight: 700; color: #303133; margin: 0; }

  &.kpi-green .kpi-icon { background: #2d8a56; }
  &.kpi-lime .kpi-icon { background: #67c23a; }
  &.kpi-orange .kpi-icon { background: #e6a23c; }
  &.kpi-blue .kpi-icon { background: #409eff; }
  &.kpi-purple .kpi-icon { background: #9b59b6; }
  &.kpi-cyan .kpi-icon { background: #1abc9c; }
}

.chart-card {
  border-radius: 8px;
  .card-header { font-weight: 600; color: #1a3a2a; }
}

.chart-box { height: 280px; }
.chart-box-lg { height: 320px; }
</style>
