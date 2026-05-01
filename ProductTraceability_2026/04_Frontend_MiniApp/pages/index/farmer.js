/**
 * 肉鸡养殖户专属首页
 * enterprise_type = 1 (种植养殖企业)
 */
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    // 企业信息
    enterpriseInfo: {
      name: '阳光肉鸡养殖场',
      logo: '/images/agricultural.png',
      auditStatus: 1 // 1-审核通过
    },
    
    // 区块链数据看板
    platformChainCount: 0,      // 全平台存证总数
    todayChainIncrement: 0,     // 今日新增
    todayCollectCount: 0,       // 本农场今日采集数
    totalCollectCount: 0,       // 累计采集数
    
    // 动画相关
    countAnimationTimer: null
  },

  onLoad() {
    this.initEnterpriseInfo()
    this.loadBlockchainStats()
    this.loadFarmStats()
  },

  onShow() {
    // 每次显示时刷新数据
    this.loadFarmStats()
  },

  onUnload() {
  },

  /**
   * 初始化企业信息
   */
  initEnterpriseInfo() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    
    // 从全局数据或存储中获取企业信息
    const enterpriseInfo = {
      name: userInfo.enterpriseName || '阳光肉鸡养殖场',
      logo: userInfo.enterpriseLogo || '/images/agricultural.png',
      auditStatus: userInfo.auditStatus !== undefined ? userInfo.auditStatus : 1
    }
    
    this.setData({ enterpriseInfo })
  },

  /**
   * 加载区块链统计数据（从后端API获取真实数据）
   */
  loadBlockchainStats() {
    // 全平台存证总数（不传enterpriseId查全部）
    request.get('/api/dashboard/farmer-stats', {}).then(res => {
      if (res.code === 200 && res.data) {
        this.setData({ platformChainCount: res.data.totalOnChain || 0 })
      }
    }).catch(() => {})

    // 今日新增（当日上链数）
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseId = userInfo.enterpriseId || null
    const params = enterpriseId ? { enterpriseId: enterpriseId } : {}
    request.get('/api/dashboard/farmer-stats', params).then(res => {
      if (res.code === 200 && res.data) {
        // dailyTrend最后一项即今日数据
        const trend = res.data.dailyTrend || []
        const todayData = trend.length > 0 ? trend[trend.length - 1] : {}
        this.setData({ todayChainIncrement: todayData.count || 0 })
      }
    }).catch(() => {})
  },

  /**
   * 加载本农场统计数据（从后端API获取真实数据）
   */
  loadFarmStats() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseId = userInfo.enterpriseId || null
    const params = enterpriseId ? { enterpriseId: enterpriseId } : {}

    request.get('/api/dashboard/farmer-stats', params).then(res => {
      if (res.code === 200 && res.data) {
        const trend = res.data.dailyTrend || []
        const todayData = trend.length > 0 ? trend[trend.length - 1] : {}
        // totalOnChain作为累计采集数，今日趋势数据作为今日采集数
        this.setData({
          todayCollectCount: todayData.count || 0,
          totalCollectCount: res.data.totalOnChain || 0
        })
      }
    }).catch(() => {})
  },

  /**
   * 扫码溯源
   */
  scanTrace() {
    wx.navigateTo({
      url: '/pages/scan/scan',
      fail: () => {
        // 如果自定义扫码页面不存在，使用系统扫码
        wx.scanCode({
          onlyFromCamera: false,
          scanType: ['qrCode', 'barCode'],
          success: (res) => {
            console.log('扫码结果:', res.result)
            this.handleScanResult(res.result)
          },
          fail: () => {
            wx.showToast({
              title: '扫码取消',
              icon: 'none'
            })
          }
        })
      }
    })
  },

  /**
   * 处理扫码结果
   */
  handleScanResult(result) {
    // 解析溯源码，跳转到溯源详情页
    if (result) {
      wx.navigateTo({
        url: `/pages/trace/trace?code=${encodeURIComponent(result)}`
      })
    }
  },

  /**
   * 页面导航
   */
  navigateTo(e) {
    const url = e.currentTarget.dataset.url
    if (!url) {
      wx.showToast({
        title: '功能开发中',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: url,
      fail: (err) => {
        console.error('导航失败:', err)
        wx.showToast({
          title: '页面跳转失败',
          icon: 'none'
        })
      }
    })
  },

  /**
   * 跳转到科普教育
   */
  goToEducation(e) {
    wx.navigateTo({
      url: `/pages/education/education`,
      fail: () => {
        wx.showToast({
          title: '功能开发中',
          icon: 'none'
        })
      }
    })
  },

  /**
   * 跳转到企业信息
   */
  goToEnterprise() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseId = userInfo.enterpriseId

    if (enterpriseId) {
      wx.navigateTo({
        url: `/pages/index/enterprise-detail?id=${enterpriseId}`,
        fail: () => {
          wx.showToast({ title: '页面跳转失败', icon: 'none' })
        }
      })
    } else {
      wx.showToast({ title: '暂无企业信息', icon: 'none' })
    }
  },

  /**
   * 跳转到区块链存证
   */
  goToBlockchain() {
    wx.navigateTo({
      url: '/pages/blockchain/certificate',
      fail: () => {
        wx.showToast({
          title: '功能开发中',
          icon: 'none'
        })
      }
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    Promise.all([
      this.loadBlockchainStats(),
      this.loadFarmStats()
    ]).finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  /**
   * 分享给好友
   */
  onShareAppMessage() {
    return {
      title: '农产品区块链溯源平台 - 肉鸡养殖',
      path: '/pages/index/index',
      imageUrl: '/images/agricultural.png'
    }
  }
})
