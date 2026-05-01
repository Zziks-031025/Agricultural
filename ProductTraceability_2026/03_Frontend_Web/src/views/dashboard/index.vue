<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="welcome-row">
      <el-col :span="24">
        <div class="welcome-card">
          <div class="welcome-content">
            <div class="welcome-text">
              <h2>{{ greeting }}，{{ name }}</h2>
              <p>{{ roleDescription }}</p>
            </div>
            <div class="welcome-icon">
              <i class="el-icon-s-home"></i>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <template v-if="isFarmer">
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6" v-for="(card, idx) in farmerCards" :key="idx">
          <el-card shadow="hover" class="stat-card" :class="{ 'warning-card': card.warning && card.value > 0 }">
            <div class="stat-content">
              <div class="stat-icon-wrapper" :style="{ background: card.bgColor }">
                <i :class="card.icon" class="stat-icon" :style="{ color: card.iconColor }"></i>
              </div>
              <div class="stat-info">
                <p class="stat-label">{{ card.label }}</p>
                <h3 class="stat-value">{{ card.value }}</h3>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="action-row">
        <el-col :span="24">
          <el-card shadow="never" class="action-card">
            <div slot="header" class="card-header">
              <span>快捷操作</span>
            </div>
            <div class="action-buttons">
              <el-button type="primary" icon="el-icon-plus" @click="$router.push('/production/batch-init')">新建批次</el-button>
              <el-button type="success" icon="el-icon-edit-outline" @click="$router.push('/production/process-record')">添加记录</el-button>
              <el-button plain icon="el-icon-search" @click="$router.push('/trace-query/index')">溯源查询</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-card shadow="never" class="chart-card">
            <div slot="header" class="card-header">
              <span>近 7 天生长记录录入趋势</span>
            </div>
            <div ref="trendChart" class="chart-area"></div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="isProcessor">
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6" v-for="(card, idx) in processorCards" :key="idx">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper" :style="{ background: card.bgColor }">
                <i :class="card.icon" class="stat-icon" :style="{ color: card.iconColor }"></i>
              </div>
              <div class="stat-info">
                <p class="stat-label">{{ card.label }}</p>
                <h3 class="stat-value">{{ card.value }}<span v-if="card.unit" class="stat-unit">{{ card.unit }}</span></h3>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="action-row">
        <el-col :span="24">
          <el-card shadow="never" class="action-card">
            <div slot="header" class="card-header"><span>快捷操作</span></div>
            <div class="action-buttons">
              <el-button type="primary" icon="el-icon-download" @click="$router.push('/processing/material')">原料接收</el-button>
              <el-button type="success" icon="el-icon-scissors" @click="$router.push('/processing/product')">加工录入</el-button>
              <el-button plain icon="el-icon-search" @click="$router.push('/trace-query/index')">溯源查询</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="14">
          <el-card shadow="never" class="chart-card">
            <div slot="header" class="card-header"><span>近 7 日加工量趋势</span></div>
            <div ref="processorLineChart" class="chart-area"></div>
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card shadow="never" class="chart-card">
            <div slot="header" class="card-header"><span>原料转化率</span></div>
            <div ref="processorPieChart" class="chart-area"></div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="isQuarantine">
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6" v-for="(card, idx) in quarantineCards" :key="idx">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper" :style="{ background: card.bgColor }">
                <i :class="card.icon" class="stat-icon" :style="{ color: card.iconColor }"></i>
              </div>
              <div class="stat-info">
                <p class="stat-label">{{ card.label }}</p>
                <h3 class="stat-value">{{ card.value }}<span v-if="card.unit" class="stat-unit">{{ card.unit }}</span></h3>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="action-row">
        <el-col :span="24">
          <el-card shadow="never" class="action-card">
            <div slot="header" class="card-header"><span>快捷操作</span></div>
            <div class="action-buttons">
              <el-button type="primary" icon="el-icon-document-checked" @click="$router.push('/circulation/inspection')">检疫录入</el-button>
              <el-button type="success" icon="el-icon-connection" @click="$router.push('/chain/upload')">上链记录</el-button>
              <el-button plain icon="el-icon-search" @click="$router.push('/trace-query/index')">溯源查询</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-card shadow="never" class="chart-card">
            <div slot="header" class="card-header">
              <span>近 7 天检疫记录趋势</span>
            </div>
            <div ref="quarantineChart" class="chart-area"></div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else-if="isEnterpriseReview">
      <el-row :gutter="20">
        <el-col :span="24">
          <el-card shadow="never" class="info-card review-card">
            <div slot="header" class="card-header"><span>企业审核状态</span></div>
            <el-alert
              :title="reviewAlertTitle"
              :description="reviewAlertDescription"
              :type="reviewAlertType"
              show-icon
              :closable="false"
            />
            <div class="review-actions">
              <el-button type="primary" @click="$router.push('/enterprise-self/info')">前往完善企业信息</el-button>
              <el-button plain @click="$router.push('/message')">查看消息通知</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else>
      <el-row :gutter="20" class="stat-row">
        <el-col :span="6" v-for="(stat, index) in defaultCards" :key="index">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon-wrapper" :style="{ background: stat.bgColor }">
                <i :class="stat.icon" class="stat-icon"></i>
              </div>
              <div class="stat-info">
                <p class="stat-label">{{ stat.label }}</p>
                <h3 class="stat-value">{{ stat.value }}</h3>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <statistics-dashboard />
        </el-col>
      </el-row>

    </template>
  </div>
</template>

<script>
import { getFarmerStats, getProcessorStats, getQuarantineStats, getDashboardStats } from '@/api/dashboard'
import StatisticsDashboard from '@/views/statistics/dashboard.vue'

const ROLE_DESC = {
  admin: '您是系统管理员，可以管理企业、用户、模板和区块链等所有模块。',
  type1: '您是种植养殖企业用户，可以管理批次、生长记录、仓储运输及销售信息。',
  type2: '您是加工屠宰企业用户，可以管理原料接收、产品加工、仓储运输及销售信息。',
  type3: '您是检疫质检企业用户，可以管理检疫记录、仓储运输及销售信息。',
  'enterprise-review': '当前企业仍处于审核流程中，请先根据审核状态完善资料，再继续使用完整业务功能。'
}

let echarts = null

export default {
  name: 'Dashboard',
  components: { StatisticsDashboard },
  data() {
    return {
      chartInstance: null,
      processorLineInstance: null,
      processorPieInstance: null,
      quarantineChartInstance: null,
      farmerCards: [
        { label: '在养批次', value: 0, icon: 'el-icon-document-copy', bgColor: 'rgba(45,138,86,0.1)', iconColor: '#2d8a56' },
        { label: '存栏总数', value: 0, icon: 'el-icon-s-data', bgColor: 'rgba(26,107,58,0.1)', iconColor: '#1a6b3a' },
        { label: '累计上链', value: 0, icon: 'el-icon-connection', bgColor: 'rgba(230,162,60,0.12)', iconColor: '#e6a23c' },
        { label: '异常预警', value: 0, icon: 'el-icon-warning-outline', bgColor: 'rgba(245,108,108,0.1)', iconColor: '#f56c6c', warning: true }
      ],
      processorCards: [
        { label: '记录数', value: 0, icon: 'el-icon-document-copy', bgColor: 'rgba(45,138,86,0.1)', iconColor: '#2d8a56' },
        { label: '批次数', value: 0, icon: 'el-icon-files', bgColor: 'rgba(26,107,58,0.1)', iconColor: '#1a6b3a' },
        { label: '上链数', value: 0, icon: 'el-icon-connection', bgColor: 'rgba(64,158,255,0.1)', iconColor: '#409eff' },
        { label: '今日接收', value: 0, unit: ' 批次', icon: 'el-icon-download', bgColor: 'rgba(230,162,60,0.12)', iconColor: '#e6a23c' }
      ],
      quarantineCards: [
        { label: '检疫批次', value: 0, icon: 'el-icon-document', bgColor: 'rgba(45,138,86,0.1)', iconColor: '#2d8a56' },
        { label: '检疫记录', value: 0, icon: 'el-icon-edit-outline', bgColor: 'rgba(26,107,58,0.1)', iconColor: '#1a6b3a' },
        { label: '累计上链', value: 0, icon: 'el-icon-connection', bgColor: 'rgba(230,162,60,0.12)', iconColor: '#e6a23c' },
        { label: '待处理', value: 0, icon: 'el-icon-bell', bgColor: 'rgba(245,108,108,0.1)', iconColor: '#f56c6c' }
      ],
      adminStats: { enterpriseTotal: '--', batchTotal: '--', chainTotal: '--', userTotal: '--' }
    }
  },
  computed: {
    name() {
      return this.$store.getters.name || '用户'
    },
    roles() {
      return this.$store.getters.roles || []
    },
    userInfo() {
      return this.$store.getters.userInfo || {}
    },
    isFarmer() {
      return this.roles.includes('type1')
    },
    isProcessor() {
      return this.roles.includes('type2')
    },
    isQuarantine() {
      return this.roles.includes('type3')
    },
    isEnterpriseReview() {
      return this.roles.includes('enterprise-review')
    },
    greeting() {
      const hour = new Date().getHours()
      if (hour < 12) return '上午好'
      if (hour < 18) return '下午好'
      return '晚上好'
    },
    roleDescription() {
      return ROLE_DESC[this.roles[0]] || '欢迎使用农产品溯源管理系统。'
    },
    reviewAlertType() {
      return this.userInfo.enterpriseAuditStatus === 2 ? 'error' : 'warning'
    },
    reviewAlertTitle() {
      return this.userInfo.enterpriseAuditStatus === 2 ? '企业审核未通过，请修改后重新提交' : '企业正在审核中'
    },
    reviewAlertDescription() {
      if (this.userInfo.enterpriseAuditStatus === 2) {
        return this.userInfo.auditRemark || '管理员尚未填写驳回原因，请前往企业信息页完善资料后重新提交。'
      }
      return '审核通过后，系统会自动开放企业工作台和业务菜单。'
    },
    defaultCards() {
      if (this.roles.includes('admin')) {
        return [
          { label: '企业总数', value: this.adminStats.enterpriseTotal, icon: 'el-icon-office-building', bgColor: 'rgba(45,138,86,0.1)' },
          { label: '批次总数', value: this.adminStats.batchTotal, icon: 'el-icon-document', bgColor: 'rgba(26,107,58,0.1)' },
          { label: '上链记录', value: this.adminStats.chainTotal, icon: 'el-icon-connection', bgColor: 'rgba(230,162,60,0.1)' },
          { label: '用户总数', value: this.adminStats.userTotal, icon: 'el-icon-user', bgColor: 'rgba(45,90,63,0.1)' }
        ]
      }
      return [
        { label: '我的批次', value: '--', icon: 'el-icon-document', bgColor: 'rgba(45,138,86,0.1)' },
        { label: '操作记录', value: '--', icon: 'el-icon-edit-outline', bgColor: 'rgba(26,107,58,0.1)' },
        { label: '上链数据', value: '--', icon: 'el-icon-connection', bgColor: 'rgba(230,162,60,0.1)' },
        { label: '待处理', value: '--', icon: 'el-icon-bell', bgColor: 'rgba(245,108,108,0.1)' }
      ]
    }
  },
  mounted() {
    if (this.isFarmer) {
      this.loadEcharts()
      this.fetchFarmerStats()
    } else if (this.isProcessor) {
      this.loadEcharts()
      this.fetchProcessorStats()
    } else if (this.isQuarantine) {
      this.loadEcharts()
      this.fetchQuarantineStats()
    } else if (this.roles.includes('admin')) {
      this.fetchAdminStats()
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    if (this.chartInstance) this.chartInstance.dispose()
    if (this.processorLineInstance) this.processorLineInstance.dispose()
    if (this.processorPieInstance) this.processorPieInstance.dispose()
    if (this.quarantineChartInstance) this.quarantineChartInstance.dispose()
  },
  methods: {
    fetchAdminStats() {
      getDashboardStats('admin').then(res => {
        const d = res.data || {}
        this.adminStats = {
          enterpriseTotal: (d.type1Count || 0) + (d.type2Count || 0) + (d.type3Count || 0),
          batchTotal: d.totalBatches || 0,
          chainTotal: d.totalOnChain || 0,
          userTotal: d.totalUsers || 0
        }
      }).catch(() => {})
    },
    loadEcharts() {
      import('echarts').then(mod => {
        echarts = mod.default || mod
        this.$nextTick(() => {
          if (this.isFarmer) {
            this.initChart()
          } else if (this.isProcessor) {
            this.initProcessorCharts()
          } else if (this.isQuarantine) {
            this.initQuarantineChart()
          }
        })
      })
    },
    fetchFarmerStats() {
      const enterpriseId = this.userInfo.enterpriseId || null
      getFarmerStats({ enterpriseId }).then(res => {
        const d = res.data || {}
        this.farmerCards[0].value = d.activeBatchCount || 0
        this.farmerCards[1].value = d.totalLivestock || 0
        this.farmerCards[2].value = d.totalOnChain || 0
        this.farmerCards[3].value = d.warningCount || 0
        if (d.dailyTrend && this.chartInstance) {
          this.updateChart(d.dailyTrend)
        }
      }).catch(() => {})
    },
    initChart() {
      if (!this.$refs.trendChart) return
      this.chartInstance = echarts.init(this.$refs.trendChart)
      window.addEventListener('resize', this.handleResize)
      const dates = []
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        dates.push((d.getMonth() + 1) + '/' + d.getDate())
      }
      this.chartInstance.setOption(this.buildChartOption(dates, new Array(7).fill(0)))
    },
    updateChart(dailyTrend) {
      if (!this.chartInstance) return
      const dates = dailyTrend.map(d => d.date)
      const counts = dailyTrend.map(d => d.count)
      this.chartInstance.setOption(this.buildChartOption(dates, counts))
    },
    buildChartOption(dates, counts) {
      return {
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>{a}: <b>{c}</b> 条'
        },
        grid: { left: 50, right: 30, top: 40, bottom: 30 },
        xAxis: {
          type: 'category',
          data: dates,
          boundaryGap: false,
          axisLine: { lineStyle: { color: '#dcdfe6' } },
          axisLabel: { color: '#606266' }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: '#f0f0f0' } },
          axisLabel: { color: '#909399' }
        },
        series: [{
          name: '记录数',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          data: counts,
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(45,138,86,0.35)' },
                { offset: 1, color: 'rgba(45,138,86,0.02)' }
              ]
            }
          },
          lineStyle: { color: '#2d8a56', width: 3 },
          itemStyle: { color: '#2d8a56', borderWidth: 2, borderColor: '#fff' }
        }]
      }
    },
    handleResize() {
      if (this.chartInstance) this.chartInstance.resize()
      if (this.processorLineInstance) this.processorLineInstance.resize()
      if (this.processorPieInstance) this.processorPieInstance.resize()
      if (this.quarantineChartInstance) this.quarantineChartInstance.resize()
    },
    fetchProcessorStats() {
      const enterpriseId = this.userInfo.enterpriseId || null
      getProcessorStats({ enterpriseId }).then(res => {
        const d = res.data || {}
        this.processorCards[0].value = d.recordCount || 0
        this.processorCards[1].value = d.batchCount || 0
        this.processorCards[2].value = d.totalOnChain || 0
        this.processorCards[3].value = d.todayReceived || 0
        if (d.processingTrend && this.processorLineInstance) {
          this.updateProcessorLineChart(d.processingTrend)
        }
        if (d.conversionRate && this.processorPieInstance) {
          this.updateProcessorPieChart(d.conversionRate)
        }
      }).catch(() => {})
    },
    initProcessorCharts() {
      if (this.$refs.processorLineChart) {
        this.processorLineInstance = echarts.init(this.$refs.processorLineChart)
        window.addEventListener('resize', this.handleResize)
        const dates = []
        for (let i = 6; i >= 0; i--) {
          const d = new Date()
          d.setDate(d.getDate() - i)
          dates.push((d.getMonth() + 1) + '/' + d.getDate())
        }
        this.processorLineInstance.setOption(this.buildProcessorLineOption(dates, new Array(7).fill(0)))
      }
      if (this.$refs.processorPieChart) {
        this.processorPieInstance = echarts.init(this.$refs.processorPieChart)
        this.processorPieInstance.setOption(this.buildProcessorPieOption([
          { name: '接收量', value: 0 },
          { name: '产出量', value: 0 }
        ]))
      }
    },
    updateProcessorLineChart(trend) {
      if (!this.processorLineInstance) return
      const dates = trend.map(d => d.date)
      const amounts = trend.map(d => d.amount)
      this.processorLineInstance.setOption(this.buildProcessorLineOption(dates, amounts))
    },
    updateProcessorPieChart(data) {
      if (!this.processorPieInstance) return
      this.processorPieInstance.setOption(this.buildProcessorPieOption(data))
    },
    buildProcessorLineOption(dates, amounts) {
      return {
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>{a}: <b>{c}</b> kg'
        },
        grid: { left: 50, right: 30, top: 40, bottom: 30 },
        xAxis: {
          type: 'category',
          data: dates,
          boundaryGap: false,
          axisLine: { lineStyle: { color: '#dcdfe6' } },
          axisLabel: { color: '#606266' }
        },
        yAxis: {
          type: 'value',
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: '#f0f0f0' } },
          axisLabel: { color: '#909399' }
        },
        series: [{
          name: '加工量',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          data: amounts,
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(26,107,58,0.35)' },
                { offset: 1, color: 'rgba(26,107,58,0.02)' }
              ]
            }
          },
          lineStyle: { color: '#1a6b3a', width: 3 },
          itemStyle: { color: '#1a6b3a', borderWidth: 2, borderColor: '#fff' }
        }]
      }
    },
    fetchQuarantineStats() {
      const enterpriseId = this.userInfo.enterpriseId || null
      getQuarantineStats({ enterpriseId }).then(res => {
        const d = res.data || {}
        this.quarantineCards[0].value = d.batchCount || 0
        this.quarantineCards[1].value = d.inspectionCount || 0
        this.quarantineCards[2].value = d.totalOnChain || 0
        this.quarantineCards[3].value = d.pendingCount || 0
        if (d.dailyTrend && this.quarantineChartInstance) {
          this.updateQuarantineChart(d.dailyTrend)
        }
      }).catch(() => {})
    },
    initQuarantineChart() {
      if (!this.$refs.quarantineChart) return
      this.quarantineChartInstance = echarts.init(this.$refs.quarantineChart)
      window.addEventListener('resize', this.handleResize)
      const dates = []
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        dates.push((d.getMonth() + 1) + '/' + d.getDate())
      }
      this.quarantineChartInstance.setOption(this.buildChartOption(dates, new Array(7).fill(0)))
    },
    updateQuarantineChart(dailyTrend) {
      if (!this.quarantineChartInstance) return
      const dates = dailyTrend.map(d => d.date)
      const counts = dailyTrend.map(d => d.count)
      this.quarantineChartInstance.setOption(this.buildChartOption(dates, counts))
    },
    buildProcessorPieOption(data) {
      return {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          bottom: 10,
          data: data.map(d => d.name)
        },
        series: [{
          type: 'pie',
          radius: ['40%', '65%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          label: { show: true, formatter: '{b}\n{d}%' },
          data,
          color: ['#2d8a56', '#e6a23c']
        }]
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #2d8a56;
$primary-dark: #1a6b3a;
$primary-darker: #1a3a2a;

.dashboard-container {
  padding: 4px;

  .welcome-row {
    margin-bottom: 20px;
  }

  .welcome-card {
    background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
    border-radius: 8px;
    padding: 28px 32px;
    color: #fff;

    .welcome-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .welcome-text {
        h2 { font-size: 22px; font-weight: 600; margin: 0 0 10px; color: #fff; }
        p { font-size: 14px; margin: 0; color: rgba(255,255,255,0.85); line-height: 1.6; }
      }

      .welcome-icon i {
        font-size: 64px;
        color: rgba(255,255,255,0.2);
      }
    }
  }

  .stat-row { margin-bottom: 20px; }

  .stat-card {
    border-radius: 8px;
    border: none;
    transition: transform 0.2s;

    &:hover { transform: translateY(-2px); }

    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon-wrapper {
        width: 52px;
        height: 52px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .stat-icon { font-size: 26px; }
      }

      .stat-info {
        .stat-label { font-size: 13px; color: #909399; margin: 0 0 6px; }
        .stat-value {
          font-size: 28px;
          font-weight: 600;
          color: #303133;
          margin: 0;

          .stat-unit { font-size: 14px; font-weight: 400; color: #909399; margin-left: 2px; }
        }
      }
    }

    &.warning-card .stat-info .stat-value {
      color: #f56c6c;
    }
  }

  .action-row { margin-bottom: 20px; }

  .action-card {
    border-radius: 8px;

    .action-buttons {
      display: flex;
      gap: 12px;
    }
  }

  .chart-card {
    border-radius: 8px;

    .chart-area {
      height: 340px;
    }
  }

  .info-card {
    border-radius: 8px;
  }

  .review-card {
    .review-actions {
      margin-top: 16px;
      display: flex;
      gap: 12px;
    }
  }

  .card-header {
    font-weight: 600;
    color: $primary-darker;
  }
}
</style>
