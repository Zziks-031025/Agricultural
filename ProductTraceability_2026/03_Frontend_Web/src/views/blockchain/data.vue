<template>
  <div class="page-container">
    <!-- Summary cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="(card, idx) in summaryCards" :key="idx">
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

    <!-- Charts -->
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" class="chart-card">
          <div slot="header"><span>上链数量趋势</span></div>
          <div ref="lineChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <div slot="header"><span>Gas 消耗统计</span></div>
          <div ref="barChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="never" class="chart-card">
          <div slot="header"><span>各环节上链分布</span></div>
          <div ref="pieChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getBlockchainStats } from '@/api/trace'

export default {
  name: 'BlockchainData',
  data() {
    return {
      stats: null,
      summaryCards: [
        { label: '上链总量', value: '--', icon: 'el-icon-connection', bg: 'rgba(45,138,86,0.1)' },
        { label: '今日上链', value: '--', icon: 'el-icon-time', bg: 'rgba(26,107,58,0.1)' },
        { label: '总Gas费用(ETH)', value: '--', icon: 'el-icon-coin', bg: 'rgba(230,162,60,0.1)' },
        { label: '今日Gas费用(ETH)', value: '--', icon: 'el-icon-money', bg: 'rgba(103,194,58,0.1)' }
      ],
      lineChartInstance: null,
      barChartInstance: null,
      pieChartInstance: null
    }
  },
  mounted() {
    this.initCharts()
    this.fetchStats()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.lineChartInstance) this.lineChartInstance.dispose()
    if (this.barChartInstance) this.barChartInstance.dispose()
    if (this.pieChartInstance) this.pieChartInstance.dispose()
  },
  methods: {
    fetchStats() {
      getBlockchainStats().then(res => {
        this.stats = res.data
        this.updateSummary(res.data)
        this.updateCharts(res.data)
      }).catch(() => {
        // Backend not yet implemented, use empty state
        this.renderEmptyCharts()
      })
    },
    updateSummary(data) {
      if (!data) return
      this.summaryCards[0].value = data.totalOnChain || 0
      this.summaryCards[1].value = data.todayOnChain || 0
      this.summaryCards[2].value = data.totalFeeEth || '0'
      this.summaryCards[3].value = data.todayFeeEth || '0'
    },
    initCharts() {
      this.lineChartInstance = echarts.init(this.$refs.lineChart)
      this.barChartInstance = echarts.init(this.$refs.barChart)
      this.pieChartInstance = echarts.init(this.$refs.pieChart)
      this.renderEmptyCharts()
    },
    renderEmptyCharts() {
      // Generate recent 7 days as placeholder dates
      const dates = []
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        dates.push((d.getMonth() + 1) + '/' + d.getDate())
      }

      // Line chart
      this.lineChartInstance.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: dates, boundaryGap: false },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          name: '上链数量',
          type: 'line',
          smooth: true,
          data: [0, 0, 0, 0, 0, 0, 0],
          areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(45,138,86,0.3)' },
            { offset: 1, color: 'rgba(45,138,86,0.02)' }
          ])},
          lineStyle: { color: '#2d8a56', width: 2 },
          itemStyle: { color: '#2d8a56' }
        }]
      })

      // Bar chart
      this.barChartInstance.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 30, bottom: 30 },
        xAxis: { type: 'category', data: dates },
        yAxis: { type: 'value', name: 'Gas (wei)' },
        series: [{
          name: 'Gas 消耗',
          type: 'bar',
          data: [0, 0, 0, 0, 0, 0, 0],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#1a6b3a' },
              { offset: 1, color: '#2d8a56' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '50%'
        }]
      })

      // Pie chart
      this.pieChartInstance.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 10 },
        series: [{
          type: 'pie',
          radius: ['40%', '65%'],
          center: ['50%', '45%'],
          label: { formatter: '{b}\n{d}%' },
          data: [
            { value: 0, name: '批次初始化', itemStyle: { color: '#2d8a56' } },
            { value: 0, name: '生长记录', itemStyle: { color: '#3ca66b' } },
            { value: 0, name: '检疫质检', itemStyle: { color: '#52bf84' } },
            { value: 0, name: '加工环节', itemStyle: { color: '#7dd4a0' } },
            { value: 0, name: '仓储运输', itemStyle: { color: '#a8e6bf' } },
            { value: 0, name: '销售环节', itemStyle: { color: '#e6a23c' } }
          ]
        }]
      })
    },
    updateCharts(data) {
      if (!data) return
      if (data.dailyTrend) {
        const dates = data.dailyTrend.map(d => d.date)
        const counts = data.dailyTrend.map(d => d.count)
        this.lineChartInstance.setOption({ xAxis: { data: dates }, series: [{ data: counts }] })
      }
      // Gas bar chart: prefer gasTrend (real data from blockchain_gas_fee)
      if (data.gasTrend) {
        const gasDates = data.gasTrend.map(d => d.date)
        const gasFees = data.gasTrend.map(d => parseFloat(d.feeEth) || 0)
        this.barChartInstance.setOption({
          xAxis: { data: gasDates },
          yAxis: { name: 'ETH' },
          series: [{ data: gasFees }]
        })
      } else if (data.dailyTrend) {
        const dates = data.dailyTrend.map(d => d.date)
        const gas = data.dailyTrend.map(d => d.gas || 0)
        this.barChartInstance.setOption({ xAxis: { data: dates }, series: [{ data: gas }] })
      }
      if (data.stageDistribution) {
        this.pieChartInstance.setOption({
          series: [{ data: data.stageDistribution }]
        })
      }
    },
    handleResize() {
      if (this.lineChartInstance) this.lineChartInstance.resize()
      if (this.barChartInstance) this.barChartInstance.resize()
      if (this.pieChartInstance) this.pieChartInstance.resize()
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.stat-row { margin-bottom: 16px; }
.summary-card {
  border-radius: 8px;
  .card-body {
    display: flex; align-items: center; gap: 16px;
    .card-icon {
      width: 52px; height: 52px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      i { font-size: 24px; color: #2d8a56; }
    }
    .card-info {
      .label { font-size: 13px; color: #909399; margin: 0 0 4px; }
      .value { font-size: 24px; font-weight: 600; color: #303133; margin: 0; }
    }
  }
}
.chart-card {
  border-radius: 8px;
  .chart-container { height: 320px; }
}
</style>
