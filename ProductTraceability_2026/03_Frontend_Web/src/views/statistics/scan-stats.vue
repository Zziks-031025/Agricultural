<template>
  <div class="page-container">
    <!-- KPI cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="(card, idx) in kpiCards" :key="idx">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-body">
            <div class="kpi-icon" :style="{ background: card.bg }">
              <i :class="card.icon"></i>
            </div>
            <div class="kpi-info">
              <p class="kpi-label">{{ card.label }}</p>
              <h3 class="kpi-value">{{ card.value }}</h3>
              <p class="kpi-sub" v-if="card.sub">{{ card.sub }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts row -->
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" class="chart-card">
          <div slot="header" class="card-header">
            <span>查询趋势</span>
            <el-radio-group v-model="trendDays" size="mini" @change="fetchTrend">
              <el-radio-button :label="7">近7天</el-radio-button>
              <el-radio-button :label="30">近30天</el-radio-button>
              <el-radio-button :label="90">近90天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="chart-card">
          <div slot="header"><span>设备类型分布</span></div>
          <div ref="deviceChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Hot products -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="10">
        <el-card shadow="never" class="chart-card">
          <div slot="header"><span>热门查询产品 TOP10</span></div>
          <div ref="hotChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <div slot="header" class="card-header">
            <span>查询日志</span>
            <div>
              <el-input
                v-model="logQuery.batchCode"
                placeholder="批次编号"
                size="mini"
                clearable
                style="width: 160px; margin-right: 8px"
                @keyup.enter.native="fetchLogs"
              />
              <el-select v-model="logQuery.deviceType" placeholder="设备类型" size="mini"
                         clearable style="width: 110px; margin-right: 8px" @change="fetchLogs">
                <el-option label="手机" value="mobile" />
                <el-option label="PC" value="pc" />
                <el-option label="平板" value="tablet" />
              </el-select>
              <el-button type="primary" size="mini" icon="el-icon-search" @click="fetchLogs">查询</el-button>
            </div>
          </div>
          <el-table :data="logList" size="mini" border v-loading="logLoading" max-height="320">
            <el-table-column prop="productName" label="产品名称" min-width="120" show-overflow-tooltip />
            <el-table-column prop="batchCode" label="批次编号" min-width="160" show-overflow-tooltip />
            <el-table-column prop="deviceType" label="设备" width="70" align="center">
              <template slot-scope="{ row }">
                <el-tag size="mini" :type="row.deviceType === 'mobile' ? 'success' : 'info'">
                  {{ deviceLabel(row.deviceType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ipAddress" label="IP" width="120" show-overflow-tooltip />
            <el-table-column prop="scanTime" label="查询时间" width="155" />
          </el-table>
          <el-pagination
            class="log-pagination"
            small
            background
            layout="total, prev, pager, next"
            :total="logTotal"
            :page-size="logQuery.size"
            :current-page.sync="logQuery.current"
            @current-change="fetchLogs"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getScanStatsOverview, getScanStatsTrend, getScanStatsLogs } from '@/api/scanStats'

export default {
  name: 'ScanStats',
  data() {
    return {
      kpiCards: [
        { label: '总查询次数', value: 0, icon: 'el-icon-search', bg: 'rgba(45,138,86,0.12)' },
        { label: '今日查询', value: 0, icon: 'el-icon-time', bg: 'rgba(64,158,255,0.12)', sub: '' },
        { label: '本周查询', value: 0, icon: 'el-icon-date', bg: 'rgba(230,162,60,0.12)' },
        { label: '被查询批次数', value: 0, icon: 'el-icon-document', bg: 'rgba(103,194,58,0.12)' }
      ],
      trendDays: 30,
      logQuery: { current: 1, size: 10, batchCode: '', deviceType: '' },
      logList: [],
      logTotal: 0,
      logLoading: false,
      overview: null
    }
  },
  mounted() {
    this.fetchOverview()
    this.fetchTrend()
    this.fetchLogs()
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
    this.disposeCharts()
  },
  methods: {
    fetchOverview() {
      getScanStatsOverview().then(res => {
        const d = res.data || {}
        this.overview = d
        this.kpiCards[0].value = d.totalCount || 0
        this.kpiCards[1].value = d.todayCount || 0
        const yc = d.yesterdayCount || 0
        const tc = d.todayCount || 0
        if (yc > 0) {
          const pct = Math.round(((tc - yc) / yc) * 100)
          this.kpiCards[1].sub = (pct >= 0 ? '+' : '') + pct + '% vs yesterday'
        }
        this.kpiCards[2].value = d.weekCount || 0
        this.kpiCards[3].value = d.queriedBatchCount || 0
        this.$nextTick(() => {
          this.renderDeviceChart(d.deviceDistribution || [])
          this.renderHotChart(d.hotProducts || [])
        })
      }).catch(() => {})
    },
    fetchTrend() {
      getScanStatsTrend({ days: this.trendDays }).then(res => {
        const trend = (res.data && res.data.trend) || []
        this.$nextTick(() => this.renderTrendChart(trend))
      }).catch(() => {})
    },
    fetchLogs() {
      this.logLoading = true
      getScanStatsLogs(this.logQuery).then(res => {
        const d = res.data || {}
        this.logList = d.records || []
        this.logTotal = d.total || 0
      }).catch(() => {}).finally(() => { this.logLoading = false })
    },
    renderTrendChart(data) {
      if (!this._trendChart) {
        this._trendChart = echarts.init(this.$refs.trendChart)
      }
      const dates = data.map(d => {
        const s = String(d.date || '')
        return s.length >= 10 ? s.substring(5) : s
      })
      const counts = data.map(d => d.count || 0)
      this._trendChart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 20, top: 20, bottom: 30 },
        xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
        yAxis: { type: 'value', minInterval: 1 },
        series: [{
          type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
          lineStyle: { color: '#2d8a56', width: 2 },
          itemStyle: { color: '#2d8a56' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(45,138,86,0.25)' },
              { offset: 1, color: 'rgba(45,138,86,0.02)' }
            ])
          },
          data: counts
        }]
      })
    },
    renderDeviceChart(data) {
      if (!this._deviceChart) {
        this._deviceChart = echarts.init(this.$refs.deviceChart)
      }
      const labelMap = { mobile: '手机', pc: 'PC', tablet: '平板', unknown: '未知' }
      const mapped = data.map(d => ({
        name: labelMap[d.name] || d.name,
        value: d.value || 0
      }))
      this._deviceChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        color: ['#2d8a56', '#409eff', '#e6a23c', '#909399'],
        series: [{
          type: 'pie', radius: ['40%', '68%'], center: ['50%', '52%'],
          label: { formatter: '{b}\n{d}%', fontSize: 12 },
          data: mapped
        }]
      })
    },
    renderHotChart(data) {
      if (!this._hotChart) {
        this._hotChart = echarts.init(this.$refs.hotChart)
      }
      const names = data.map(d => d.productName || d.batchCode).reverse()
      const values = data.map(d => d.queryCount || 0).reverse()
      this._hotChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 100, right: 30, top: 10, bottom: 20 },
        xAxis: { type: 'value', minInterval: 1 },
        yAxis: { type: 'category', data: names, axisLabel: { fontSize: 11 } },
        series: [{
          type: 'bar', barWidth: 16,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#2d8a56' },
              { offset: 1, color: '#67c23a' }
            ])
          },
          data: values
        }]
      })
    },
    deviceLabel(type) {
      const map = { mobile: '手机', pc: 'PC', tablet: '平板', unknown: '未知' }
      return map[type] || type || '未知'
    },
    resizeCharts() {
      [this._trendChart, this._deviceChart, this._hotChart].forEach(c => c && c.resize())
    },
    disposeCharts() {
      [this._trendChart, this._deviceChart, this._hotChart].forEach(c => c && c.dispose())
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container { padding: 4px; }
.stat-row { margin-bottom: 16px; }
.kpi-card {
  border-radius: 8px;
  .kpi-body {
    display: flex; align-items: center; gap: 14px;
    .kpi-icon {
      width: 50px; height: 50px; border-radius: 12px;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      i { font-size: 22px; color: #2d8a56; }
    }
    .kpi-info {
      .kpi-label { font-size: 13px; color: #909399; margin: 0 0 2px; }
      .kpi-value { font-size: 24px; font-weight: 600; color: #303133; margin: 0; }
      .kpi-sub { font-size: 11px; color: #909399; margin: 2px 0 0; }
    }
  }
}
.chart-card {
  border-radius: 8px;
  .card-header {
    display: flex; justify-content: space-between; align-items: center;
    font-weight: 600; color: #1a3a2a;
  }
}
.chart-box { height: 300px; }
.log-pagination { margin-top: 12px; text-align: right; }
</style>
