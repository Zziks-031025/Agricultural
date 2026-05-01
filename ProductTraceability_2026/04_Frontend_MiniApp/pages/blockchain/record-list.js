/**
 * 区块链上链记录流水页
 * 展示本企业所有已上链的操作记录
 */
const app = getApp()
const request = require('../../utils/request.js')

// 记录类型配置
const RECORD_TYPE_CONFIG = {
  // 肉鸡养殖
  feeding: { name: '喂养记录' },
  vaccine: { name: '防疫记录' },
  inspect: { name: '环境巡查' },
  medication: { name: '用药记录' },
  // 西红柿种植
  fertilize: { name: '施肥记录' },
  irrigate: { name: '灌溉记录' },
  pesticide: { name: '除虫记录' },
  // 通用
  init: { name: '批次初始化' },
  quarantine: { name: '检疫申报' },
  storage: { name: '仓储入库' },
  transport: { name: '物流运输' }
}

Page({
  data: {
    // 企业信息
    enterpriseInfo: {},
    
    // 统计数据
    statistics: {
      totalCount: 0,
      todayCount: 0,
      weekCount: 0
    },
    
    // 记录列表
    recordList: [],
    
    // 分页
    page: 1,
    pageSize: 20,
    loading: false,
    noMore: false
  },

  onLoad(options) {
    // 获取当前用户企业信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseInfo = {
      id: userInfo.enterpriseId || 1,
      name: userInfo.enterpriseName || '阳光肉鸡养殖场',
      type: userInfo.enterpriseType || 1
    }
    
    this.setData({ enterpriseInfo })
    this.loadStatistics()
    this.loadRecordList()
  },

  onShow() {
    // 返回时刷新列表
    if (this.data.recordList.length > 0) {
      this.loadStatistics()
    }
  },

  /**
   * 加载统计数据
   */
  loadStatistics() {
    const params = {}
    if (this.data.enterpriseInfo.id) {
      params.enterpriseId = this.data.enterpriseInfo.id
    }
    request.get('/api/blockchain/stats', params).then(res => {
      if (res.code === 200 && res.data) {
        this.setData({
          statistics: {
            totalCount: res.data.totalCount || 0,
            todayCount: res.data.todayCount || 0,
            weekCount: res.data.weekCount || 0
          }
        })
      }
    }).catch(() => {})
  },

  /**
   * 加载记录列表
   */
  loadRecordList(refresh = false) {
    if (this.data.loading) return
    if (!refresh && this.data.noMore) return
    
    this.setData({ loading: true })
    
    if (refresh) {
      this.setData({ page: 1, noMore: false, recordList: [] })
    }
    
    const params = {
      pageNum: this.data.page,
      pageSize: this.data.pageSize
    }
    if (this.data.enterpriseInfo.id) {
      params.enterpriseId = this.data.enterpriseInfo.id
    }

    request.get('/api/blockchain/logs', params).then(res => {
      if (res.code === 200 && res.data) {
        const records = (res.data.records || []).map(item => {
          const config = RECORD_TYPE_CONFIG[item.recordType] || { name: item.recordType || '操作记录' }
          const txHash = item.txHash || ''
          return {
            id: item.id,
            recordType: item.recordType || '',
            recordTypeName: config.name,
            batchCode: item.batchCode || '',
            batchId: item.batchId,
            operator: item.operator || '--',
            recordTime: item.chainTime || item.createTime || '--',
            recordDate: (item.chainTime || item.createTime || '').substring(5, 10),
            txHash: txHash,
            txHashShort: txHash ? `${txHash.substring(0, 10)}...${txHash.substring(txHash.length - 6)}` : '',
            blockNumber: item.blockNumber || null,
            gasUsed: item.gasUsed || null,
            status: item.status === 'SUCCESS' ? 'confirmed' : (item.status || 'confirmed'),
            confirmations: item.confirmations || null
          }
        })

        const { recordList, page } = this.data
        const newList = refresh ? records : [...recordList, ...records]
        this.setData({
          recordList: newList,
          page: page + 1,
          loading: false,
          noMore: records.length < this.data.pageSize
        })
      } else {
        this.setData({ loading: false })
      }
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /**
   * 查看证书详情
   */
  viewCertificate(e) {
    const { item } = e.currentTarget.dataset
    
    wx.navigateTo({
      url: `/pages/blockchain/certificate?txHash=${item.txHash}&batchId=${item.batchId}&recordId=${item.id}&recordType=${item.recordType}`
    })
  },

  /**
   * 复制哈希
   */
  copyHash(e) {
    const { hash } = e.currentTarget.dataset
    
    wx.setClipboardData({
      data: hash,
      success: () => {
        wx.showToast({ title: '哈希已复制', icon: 'success' })
      }
    })
  },

  /**
   * 查看区块浏览器
   */
  viewInExplorer(e) {
    const { hash } = e.currentTarget.dataset
    
    wx.setClipboardData({
      data: `https://etherscan.io/tx/${hash}`,
      success: () => {
        wx.showToast({ title: '链接已复制', icon: 'success' })
      }
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadStatistics()
    this.loadRecordList(true)
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  },

  /**
   * 触底加载更多
   */
  onReachBottom() {
    this.loadRecordList()
  }
})
