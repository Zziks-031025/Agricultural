/**
 * 申报接收列表 - 检疫质检机构专属
 * 从后端 /api/batch/list?statuses=3 查询待检疫批次
 * 从后端 /api/batch/list?statuses=5 查询已检疫批次
 */
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    keyword: '',
    currentTab: 'pending',

    // 待检疫列表
    pendingList: [],
    pendingTotal: 0,
    pendingPage: 1,
    pendingLoading: false,
    pendingNoMore: false,

    // 已完成列表
    completedList: [],
    completedTotal: 0,
    completedPage: 1,
    completedLoading: false,
    completedNoMore: false,

    pageSize: 10
  },

  onLoad() {
    this.loadPendingList(true)
    this._loaded = true
  },

  onShow() {
    // 首次进入由 onLoad 处理，后续返回本页时尝试刷新接口
    if (!this._loaded) {
      this.loadPendingList(true)
    }
    this._loaded = false
  },

  /**
   * 加载待检疫列表 (调用检疫机构专用接口)
   */
  loadPendingList(refresh = false) {
    if (this.data.pendingLoading) return
    if (!refresh && this.data.pendingNoMore) return

    const page = refresh ? 1 : this.data.pendingPage
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const inspectionEnterpriseId = userInfo.enterpriseId

    console.log('[apply-list] loadPendingList')
    console.log('[apply-list] userInfo:', JSON.stringify(userInfo))
    console.log('[apply-list] inspectionEnterpriseId:', inspectionEnterpriseId)
    console.log('[apply-list] app.globalData:', JSON.stringify(app.globalData))

    this.setData({ pendingLoading: true })

    const params = {
      status: 'pending'
    }
    
    if (inspectionEnterpriseId) {
      params.inspectionEnterpriseId = inspectionEnterpriseId
    }

    if (this.data.keyword && this.data.keyword.trim()) {
      params.batchCode = this.data.keyword.trim()
    }

    console.log('[apply-list] request params:', JSON.stringify(params))

    request.get('/api/quarantine/apply/list-for-inspector', params).then(res => {
      console.log('[apply-list] pending response:', JSON.stringify(res))
      const records = res.data || []
      const list = records.map(item => this.formatBatchItem(item))

      this.setData({
        pendingList: refresh ? list : this.data.pendingList.concat(list),
        pendingTotal: records.length,
        pendingPage: page + 1,
        pendingLoading: false,
        pendingNoMore: true
      })
    }).catch(err => {
      console.error('[apply-list] pending error:', err)
      this.setData({ pendingLoading: false })
      wx.showToast({
        title: '加载失败: ' + (err.message || '未知错误'),
        icon: 'none',
        duration: 2000
      })
    })
  },

  /**
   * 加载已完成列表 (调用检疫机构专用接口)
   */
  loadCompletedList(refresh = false) {
    if (this.data.completedLoading) return
    if (!refresh && this.data.completedNoMore) return

    const page = refresh ? 1 : this.data.completedPage
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const inspectionEnterpriseId = userInfo.enterpriseId

    console.log('[apply-list] loadCompletedList')
    console.log('[apply-list] userInfo:', JSON.stringify(userInfo))
    console.log('[apply-list] inspectionEnterpriseId:', inspectionEnterpriseId)

    this.setData({ completedLoading: true })

    const params = {
      status: 'completed'
    }
    
    if (inspectionEnterpriseId) {
      params.inspectionEnterpriseId = inspectionEnterpriseId
    }

    if (this.data.keyword && this.data.keyword.trim()) {
      params.batchCode = this.data.keyword.trim()
    }

    console.log('[apply-list] request params:', JSON.stringify(params))

    request.get('/api/quarantine/apply/list-for-inspector', params).then(res => {
      console.log('[apply-list] completed response:', JSON.stringify(res))
      const records = res.data || []
      const list = records.map(item => this.formatBatchItem(item))

      this.setData({
        completedList: refresh ? list : this.data.completedList.concat(list),
        completedTotal: records.length,
        completedPage: page + 1,
        completedLoading: false,
        completedNoMore: true
      })
    }).catch(err => {
      console.error('[apply-list] completed error:', err)
      this.setData({ completedLoading: false })
      wx.showToast({
        title: '加载失败: ' + (err.message || '未知错误'),
        icon: 'none',
        duration: 2000
      })
    })
  },

  /**
   * 格式化批次数据
   */
  formatBatchItem(item) {
    const passed = item.checkResult === 1;
    const hasChain = item.txHash != null && item.txHash !== '';
    
    return {
      id: item.id,
      batchId: item.batchId,
      batchCode: item.batchCode || '',
      productName: item.productName || '',
      productType: item.productType,
      productTypeName: item.productType === 2 ? '种植' : '养殖',
      productTypeTag: item.productType === 2 ? '种植类' : '畜禽类',
      enterpriseName: item.enterpriseName || '—',
      enterpriseId: item.enterpriseId,
      quantity: item.quantity || 0,
      unit: item.unit || '',
      applyTime: item.createTime || '',
      applyDate: item.createDate || '',
      status: item.status,
      statusText: item.statusText || '待检疫',
      checkResult: item.checkResult,
      passed: passed,
      hasChain: hasChain
    }
  },

  /**
   * Tab 切换
   */
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ currentTab: tab })

    // 切换到已完成时首次加载
    if (tab === 'completed' && this.data.completedList.length === 0) {
      this.loadCompletedList(true)
    }
  },

  /**
   * 搜索
   */
  onSearch(e) {
    this.setData({ keyword: e.detail.value })
  },

  /**
   * 确认搜索（回车 / 失焦触发）
   */
  doSearch() {
    if (this.data.currentTab === 'pending') {
      this.loadPendingList(true)
    } else {
      this.loadCompletedList(true)
    }
  },

  /**
   * 点击卡片 → 进入检疫录入详情
   */
  goToDetail(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: `/pages/quarantine/result-form?batchCode=${item.batchCode}&productName=${encodeURIComponent(item.productName)}&quantity=${item.quantity}&unit=${item.unit}&enterpriseName=${encodeURIComponent(item.enterpriseName)}&batchId=${item.batchId}`
    })
  },

  /**
   * 快捷按钮「去处理」→ 直接跳转检疫录入
   */
  handleProcess(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: `/pages/quarantine/result-form?batchCode=${item.batchCode}&productName=${encodeURIComponent(item.productName)}&quantity=${item.quantity}&unit=${item.unit}&enterpriseName=${encodeURIComponent(item.enterpriseName)}&batchId=${item.batchId}`
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    if (this.data.currentTab === 'pending') {
      this.loadPendingList(true)
    } else {
      this.loadCompletedList(true)
    }
    wx.stopPullDownRefresh()
  },

  /**
   * 触底加载更多
   */
  onReachBottom() {
    if (this.data.currentTab === 'pending') {
      this.loadPendingList(false)
    } else {
      this.loadCompletedList(false)
    }
  }
})
