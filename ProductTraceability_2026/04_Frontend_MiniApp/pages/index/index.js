/**
 * 首页 - 游客/企业 双视图统一布局
 * 未登录(游客)：展示平台品牌宣传 + 公开数据
 * 已登录(企业)：展示企业品牌 + 企业专属数据
 * 扫码溯源、科普教育、企业信息 对所有用户可见
 */
const app = getApp()
const request = require('../../utils/request.js')
const { proxyImages } = require('../../utils/util.js')

Page({
  data: {
    // ---- 登录状态 ----
    isLogin: false,
    showEnterpriseHome: false,

    // ---- 用户/企业信息 ----
    userType: 3,           // 1-平台管理员 2-企业用户 3-普通用户
    enterpriseType: 0,     // 1-种植养殖 2-加工宰杀 3-检疫质检
    enterpriseInfo: {
      id: null,
      name: '',
      auditStatus: 1
    },

    // ---- 头部展示(已登录) ----
    badgeText: '',         // 身份标签文案
    sloganText: '',        // 品牌标语

    // ---- 轮播图 ----
    swiperItems: [],

    // ---- 数据统计 ----
    platformChainCount: 0,    // 全平台存证总数
    enterpriseCount: 128,     // 入驻企业总数（游客）
    contributeCount: 0,       // 本企业贡献数（已登录）
    contributeLabel: '',      // 贡献数标签

    // ---- 底部导航（按企业类型适配） ----
    navEducationTitle: '科普教育',
    navEducationDesc: '农产品知识科普',
    navEnterpriseTitle: '企业信息',
    navEnterpriseDesc: '入驻企业列表',

    // ---- 内部 ----
    countTimer: null,
    _bannersLoaded: false
  },

  onLoad() {
    this.initPage()
  },

  onShow() {
    this.initPage()
  },

  onUnload() {
  },

  /**
   * 初始化页面：检测登录状态并加载数据
   */
  initPage() {
    const token = app.globalData.token || wx.getStorageSync('token')
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const isLogin = !!(token && userInfo && userInfo.userType)

    if (isLogin) {
      this.setupLoggedInView(userInfo)
    } else {
      this.setupGuestView()
    }

    this.loadStats(isLogin, userInfo)
  },

  /**
   * 已登录视图设置
   */
  setupLoggedInView(userInfo) {
    const userType = userInfo.userType || 3
    let enterpriseType = 0
    let enterpriseId = null
    const showEnterpriseHome = userType === 2

    if (showEnterpriseHome) {
      enterpriseType = userInfo.enterpriseType || app.globalData.enterpriseType || 1
      enterpriseId = userInfo.enterpriseId || null
    }

    const enterpriseInfo = {
      id: enterpriseId,
      name: showEnterpriseHome ? (userInfo.enterpriseName || this.getDefaultName(enterpriseType)) : '',
      auditStatus: showEnterpriseHome && userInfo.auditStatus !== undefined ? userInfo.auditStatus : 1
    }

    const badgeText = showEnterpriseHome ? this.getBadgeText(enterpriseType) : ''
    const sloganText = showEnterpriseHome ? this.getSloganText(enterpriseType) : ''

    const contributeLabel = showEnterpriseHome ? this.getContributeLabel(enterpriseType) : ''
    const navConfig = showEnterpriseHome
      ? this.getNavConfig(enterpriseType)
      : {
          educationTitle: '科普教育',
          educationDesc: '农产品知识科普',
          enterpriseTitle: '企业信息',
          enterpriseDesc: '入驻企业列表'
        }

    this.setData({
      isLogin: true,
      showEnterpriseHome,
      userType,
      enterpriseType,
      enterpriseInfo,
      badgeText,
      sloganText,
      swiperItems: [],
      contributeLabel,
      navEducationTitle: navConfig.educationTitle,
      navEducationDesc: navConfig.educationDesc,
      navEnterpriseTitle: navConfig.enterpriseTitle,
      navEnterpriseDesc: navConfig.enterpriseDesc
    })

    this.loadBanners(showEnterpriseHome ? enterpriseId : null)

    console.log('已登录 - 企业首页:', showEnterpriseHome, '企业类型:', enterpriseType, '企业:', enterpriseInfo.name)
  },

  /**
   * 游客视图设置
   */
  setupGuestView() {
    this.setData({
      isLogin: false,
      showEnterpriseHome: false,
      userType: 3,
      enterpriseType: 0,
      enterpriseInfo: { id: null, name: '', auditStatus: 1 },
      badgeText: '',
      sloganText: '',
      swiperItems: [],
      contributeCount: 0,
      contributeLabel: '',
      navEducationTitle: '科普教育',
      navEducationDesc: '农产品知识科普',
      navEnterpriseTitle: '企业信息',
      navEnterpriseDesc: '入驻企业列表'
    })

    this.loadBanners(null)
  },

  /**
   * 加载统计数据（从后端API获取真实数据）
   */
  loadStats(isLogin, userInfo) {
    // 1. 全平台存证总数（不传enterpriseId查全部）
    request.get('/api/dashboard/farmer-stats', {}).then(res => {
      if (res.code === 200 && res.data) {
        this.setData({ platformChainCount: res.data.totalOnChain || 0 })
      }
    }).catch(() => {})

    // 2. 企业用户：获取本企业贡献数
    if (isLogin && userInfo.userType === 2) {
      const eType = userInfo.enterpriseType || app.globalData.enterpriseType || 1
      const enterpriseId = userInfo.enterpriseId || null
      let apiPath = '/api/dashboard/farmer-stats'
      if (eType === 2) {
        apiPath = '/api/dashboard/processor-stats'
      } else if (eType === 3) {
        apiPath = '/api/dashboard/quarantine-stats'
      }
      const params = enterpriseId ? { enterpriseId: enterpriseId } : {}
      request.get(apiPath, params).then(res => {
        if (res.code === 200 && res.data) {
          this.setData({ contributeCount: res.data.totalOnChain || 0 })
        }
      }).catch(() => {})
    } else {
      // 游客：获取平台统计数据（admin-stats 接口无权限校验，可公开访问）
      request.get('/api/dashboard/admin-stats', {}).then(res => {
        if (res.code === 200 && res.data) {
          const total = (res.data.type1Count || 0) + (res.data.type2Count || 0) + (res.data.type3Count || 0)
          this.setData({ enterpriseCount: total })
        }
      }).catch(() => {})
    }
  },

  // ========== 辅助方法：根据企业类型获取文案 ==========

  getDefaultName(enterpriseType) {
    const map = { 1: '阳光肉鸡养殖场', 2: '鑫源肉类加工厂', 3: '动物卫生监督所' }
    return map[enterpriseType] || '企业用户'
  },

  getBadgeText(enterpriseType) {
    const map = { 1: '认证养殖户', 2: '认证加工企业', 3: '认证检测机构' }
    return map[enterpriseType] || '认证企业'
  },

  getSloganText(enterpriseType) {
    const map = {
      1: '科学养殖 · 品质保证 · 区块链溯源',
      2: '智能加工 · 冷链保鲜 · 区块链溯源',
      3: '科学检测 · 权威认证 · 区块链存证'
    }
    return map[enterpriseType] || '区块链溯源认证'
  },

  getContributeLabel(enterpriseType) {
    const map = { 1: '本农场累计贡献', 2: '本厂累计加工笔数', 3: '本机构检测数' }
    return map[enterpriseType] || '本企业贡献数'
  },

  /**
   * 底部导航配置（按企业类型差异化）
   */
  getNavConfig(enterpriseType) {
    const map = {
      1: {
        educationTitle: '科普教育',
        educationDesc: '养殖常识百科',
        enterpriseTitle: '企业信息',
        enterpriseDesc: '查看农场资质'
      },
      2: {
        educationTitle: '科普教育',
        educationDesc: '企业知识科普',
        enterpriseTitle: '企业信息',
        enterpriseDesc: '资质公示'
      },
      3: {
        educationTitle: '科普教育',
        educationDesc: '检疫知识科普',
        enterpriseTitle: '机构信息公示',
        enterpriseDesc: '资质与认证公开'
      }
    }
    return map[enterpriseType] || {
      educationTitle: '科普教育',
      educationDesc: '行业知识百科',
      enterpriseTitle: '企业信息',
      enterpriseDesc: '查看企业资质'
    }
  },

  /**
   * 从后端加载Banner数据
   * 企业用户：传递enterpriseId，只返回该企业自己的Banner
   * 游客：不传enterpriseId，只返回平台通用(游客)Banner
   */
  loadBanners(enterpriseId) {
    const params = enterpriseId ? { enterpriseId: enterpriseId } : {}
    const fullUrl = request.getFullURL('/api/banner/active')
    const token = (app && app.globalData ? app.globalData.token : null) || wx.getStorageSync('token')
    const fallbackSwiperItems = []
    
    console.log('[Banner] 开始请求:', fullUrl, 'enterpriseId:', enterpriseId)
    
    wx.request({
      url: fullUrl,
      method: 'GET',
      data: params,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? ('Bearer ' + token) : ''
      },
      timeout: 15000,
      success: (res) => {
        console.log('[Banner] 响应状态:', res.statusCode, '数据条数:', res.data && res.data.data ? res.data.data.length : 0)
        const data = res.data
        if (res.statusCode === 200 && data && data.code === 200 && data.data && data.data.length > 0) {
          const items = data.data.map(item => {
            return {
              image: this.resolveBannerImageUrl(item.imageUrl),
              title: item.title || '',
              desc: item.description || '',
              link: item.linkUrl || ''
            }
          })
          const imgUrls = items.map(i => i.image)
          proxyImages(imgUrls).then(localPaths => {
            const swiperItems = items.map((item, idx) => ({
              ...item,
              image: localPaths[idx] || item.image
            }))
            console.log('[Banner] 加载成功, 图片路径示例:', swiperItems[0] ? swiperItems[0].image : 'none')
            this.setData({ swiperItems, _bannersLoaded: true })
          })
          this._lastBannerEnterpriseId = enterpriseId
        } else {
          this.setData({ swiperItems: fallbackSwiperItems, _bannersLoaded: true })
          this._lastBannerEnterpriseId = enterpriseId
          console.warn('[Banner] API返回异常:', JSON.stringify(data).substring(0, 200))
        }
      },
      fail: (err) => {
        this.setData({ swiperItems: fallbackSwiperItems, _bannersLoaded: true })
        this._lastBannerEnterpriseId = enterpriseId
        console.error('[Banner] 请求失败:', JSON.stringify(err))
      }
    })
  },

  resolveBannerImageUrl(imageUrl) {
    if (!imageUrl) return ''
    return request.getFullURL(imageUrl)
  },

  // ========== 页面交互事件 ==========

  /**
   * 扫码溯源（所有用户可用）
   */
  scanCode() {
    wx.navigateTo({
      url: '/pages/scan/scan',
      fail: () => {
        wx.scanCode({
          onlyFromCamera: false,
          scanType: ['qrCode', 'barCode'],
          success: (res) => {
            if (res.result) {
              wx.navigateTo({
                url: `/pages/trace/trace?code=${encodeURIComponent(res.result)}`
              })
            }
          },
          fail: () => {
            wx.showToast({ title: '扫码取消', icon: 'none' })
          }
        })
      }
    })
  },

  /**
   * 跳转科普/规范教育（按企业类型路由）
   * 养殖户：通用科普
   * 加工企业：加工标准科普
   * 检疫机构：检疫规范科普
   * 游客：通用科普
   */
  goToEducation() {
    const { isLogin, enterpriseType } = this.data
    let url = '/pages/education/education'

    // 企业用户不传category参数，只按enterpriseId过滤，显示该企业所有分类的文章
    // 普通用户不传参数，显示所有文章

    wx.navigateTo({
      url,
      fail: () => {
        wx.showToast({ title: '功能开发中', icon: 'none' })
      }
    })
  },

  /**
   * 跳转企业列表/企业详情
   * 游客：跳转企业列表
   * 已登录：跳转本企业详情
   */
  goToEnterprise() {
    if (!this.data.showEnterpriseHome) {
      wx.navigateTo({
        url: '/pages/index/enterprise-list',
        fail: () => {
          wx.showToast({ title: '功能开发中', icon: 'none' })
        }
      })
      return
    }

    const { enterpriseInfo, enterpriseType } = this.data
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    let enterpriseId = enterpriseInfo.id || userInfo.enterpriseId

    if (!enterpriseId && enterpriseType) {
      enterpriseId = enterpriseType
    }

    if (enterpriseId) {
      wx.navigateTo({
        url: `/pages/index/enterprise-detail?id=${enterpriseId}`,
        fail: (err) => {
          console.error('跳转失败:', err)
          wx.showToast({ title: '页面跳转失败', icon: 'none' })
        }
      })
    } else {
      wx.showToast({ title: '暂无企业信息', icon: 'none' })
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseId = userInfo.enterpriseId || null
    
    this.setData({ _bannersLoaded: false })
    this.loadBanners(enterpriseId)
    this.initPage()
    
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  },

  /**
   * 分享
   */
  onShareAppMessage() {
    const { showEnterpriseHome, enterpriseInfo, enterpriseType } = this.data

    if (showEnterpriseHome && enterpriseType) {
      const badgeText = this.getBadgeText(enterpriseType)
      return {
        title: `${enterpriseInfo.name} - ${badgeText}`,
        path: '/pages/index/index',
        imageUrl: '/images/agricultural.png'
      }
    }

    return {
      title: '农产品区块链溯源平台',
      path: '/pages/index/index',
      imageUrl: '/images/agricultural.png'
    }
  }
})
