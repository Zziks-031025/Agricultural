const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

function resolveAuditStatus(userInfo) {
  if (!userInfo) return null
  if (userInfo.auditStatus !== undefined && userInfo.auditStatus !== null) {
    return userInfo.auditStatus
  }
  if (userInfo.enterpriseAuditStatus !== undefined && userInfo.enterpriseAuditStatus !== null) {
    return userInfo.enterpriseAuditStatus
  }
  return null
}

Page({
  data: {
    isLogin: false,
    hasWorkbenchAccess: false,
    isEnterpriseReview: false,
    showApplyGuide: false,

    userInfo: {},
    enterpriseType: null,
    enterpriseTypeName: '',
    isInspector: false,

    reviewStatus: null,
    reviewRemark: '',
    reviewEnterpriseId: null,

    recordCount: 0,
    batchCount: 0,
    chainCount: 0,

    pendingCount: 0,
    todayChecked: 0
  },

  onLoad() {
    this.initUserInfo()
  },

  onShow() {
    if (app.globalData.token) {
      app.refreshUserInfo(() => {
        this.initUserInfo()
      })
    } else {
      this.initUserInfo()
    }
  },

  initUserInfo() {
    const token = app.globalData.token || wx.getStorageSync('token')
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const isLogin = !!(token && userInfo && userInfo.userType)

    if (!isLogin) {
      this.setData({
        isLogin: false,
        hasWorkbenchAccess: false,
        isEnterpriseReview: false,
        showApplyGuide: false,
        userInfo: {},
        enterpriseType: null,
        enterpriseTypeName: '',
        isInspector: false,
        reviewStatus: null,
        reviewRemark: '',
        reviewEnterpriseId: null
      })
      return
    }

    const userType = userInfo.userType || 3
    const auditStatus = resolveAuditStatus(userInfo)
    const hasEnterpriseApplication = userType === 2 && userInfo.enterpriseId && auditStatus !== null && auditStatus !== undefined

    let enterpriseType = null
    let hasWorkbenchAccess = false
    let isEnterpriseReview = false
    let showApplyGuide = false

    if (userType === 1) {
      enterpriseType = 0
      hasWorkbenchAccess = true
    } else if (userType === 2 && auditStatus === 1) {
      enterpriseType = app.globalData.enterpriseType || userInfo.enterpriseType || null
      hasWorkbenchAccess = enterpriseType !== null
    } else if (hasEnterpriseApplication) {
      enterpriseType = userInfo.enterpriseType || null
      isEnterpriseReview = auditStatus !== 1
    } else if (userType === 3) {
      showApplyGuide = true
    }

    const isInspector = hasWorkbenchAccess && userType === 2 && enterpriseType === 3

    let avatarHttpUrl = userInfo.avatar || ''
    if (avatarHttpUrl && !avatarHttpUrl.startsWith('http')) {
      const baseUrl = (app.globalData && app.globalData.apiBaseUrl) || ''
      avatarHttpUrl = baseUrl + avatarHttpUrl
    }
    const resolvedUserInfo = Object.assign({}, userInfo, { avatar: avatarHttpUrl })

    this.setData({
      isLogin: true,
      hasWorkbenchAccess,
      isEnterpriseReview,
      showApplyGuide,
      userInfo: resolvedUserInfo,
      enterpriseType,
      enterpriseTypeName: enterpriseType !== null ? this.getEnterpriseTypeName(enterpriseType) : '',
      isInspector,
      reviewStatus: isEnterpriseReview ? auditStatus : null,
      reviewRemark: isEnterpriseReview ? (userInfo.auditRemark || '') : '',
      reviewEnterpriseId: isEnterpriseReview ? (userInfo.enterpriseId || null) : null
    })

    if (avatarHttpUrl) {
      util.proxyImage(avatarHttpUrl).then(lp => {
        if (lp && lp !== avatarHttpUrl) {
          this.setData({ 'userInfo.avatar': lp })
        }
      })
    }

    if (!hasWorkbenchAccess) {
      return
    }

    if (isInspector) {
      this.loadInspectorStats()
    }
    this.loadWorkbenchStats(enterpriseType, userInfo.enterpriseId)
  },

  getEnterpriseTypeName(type) {
    const typeMap = {
      0: '系统管理员',
      1: '种植养殖',
      2: '加工宰杀',
      3: '检疫质检'
    }
    return typeMap[type] || '未知类型'
  },

  loadWorkbenchStats(enterpriseType, enterpriseId) {
    let apiPath = '/api/dashboard/farmer-stats'
    if (enterpriseType === 2) {
      apiPath = '/api/dashboard/processor-stats'
    }
    const params = enterpriseId ? { enterpriseId } : {}
    request.get(apiPath, params).then(res => {
      if (res.code === 200 && res.data) {
        this.setData({
          recordCount: res.data.recordCount || 0,
          batchCount: res.data.batchCount || 0,
          chainCount: res.data.totalOnChain || 0
        })
      }
    }).catch(() => {})
  },

  loadInspectorStats() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const inspectionEnterpriseId = userInfo.enterpriseId

    if (!inspectionEnterpriseId) {
      return
    }

    request.get('/api/quarantine/apply/list-for-inspector', {
      inspectionEnterpriseId,
      status: 'pending'
    }).then(res => {
      const records = res.data || []
      this.setData({ pendingCount: records.length })
    }).catch(() => {})

    request.get('/api/quarantine/apply/list-for-inspector', {
      inspectionEnterpriseId,
      status: 'completed'
    }).then(res => {
      const records = res.data || []
      this.setData({ todayChecked: records.length })
    }).catch(() => {})
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  },

  goToUserCenter() {
    wx.switchTab({
      url: '/pages/user/user'
    })
  },

  goToMessages() {
    wx.navigateTo({
      url: '/pages/user/messages'
    })
  },

  handleReapply() {
    if (this.data.reviewStatus !== 2 || !this.data.reviewEnterpriseId) {
      wx.showToast({
        title: '当前状态无需重新提交',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url: `/pages/login/register?mode=reapply&enterpriseId=${this.data.reviewEnterpriseId}`
    })
  },

  goToEnterpriseApply() {
    if (!app.requireLogin()) {
      return
    }

    wx.navigateTo({
      url: '/pages/login/register'
    })
  },

  ensureWorkbenchAccess() {
    if (!app.requireLogin()) {
      return false
    }
    if (this.data.hasWorkbenchAccess) {
      return true
    }
    wx.showToast({
      title: '当前账号无企业工作台权限',
      icon: 'none'
    })
    return false
  },

  navigateTo(e) {
    if (!this.ensureWorkbenchAccess()) return

    const url = e.currentTarget.dataset.url
    if (!url) {
      wx.showToast({
        title: '功能开发中',
        icon: 'none'
      })
      return
    }

    wx.navigateTo({
      url,
      fail: () => {
        wx.showToast({
          title: '页面跳转失败',
          icon: 'none'
        })
      }
    })
  },

  uploadReport() {
    if (!this.ensureWorkbenchAccess()) return

    wx.navigateTo({
      url: '/pages/quarantine/report-upload',
      fail: () => {
        wx.showToast({ title: '功能开发中', icon: 'none' })
      }
    })
  },

  goToBlockchain() {
    if (!this.ensureWorkbenchAccess()) return

    wx.navigateTo({
      url: '/pages/blockchain/record-list',
      fail: () => {
        wx.showToast({
          title: '功能开发中',
          icon: 'none'
        })
      }
    })
  }
})
