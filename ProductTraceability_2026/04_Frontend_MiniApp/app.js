const config = require('./config.js')

function normalizeUserInfo(userInfo) {
  if (!userInfo) {
    return null
  }
  const normalized = { ...userInfo }
  if (normalized.auditStatus === undefined && normalized.enterpriseAuditStatus !== undefined) {
    normalized.auditStatus = normalized.enterpriseAuditStatus
  }
  return normalized
}

function resolveUserRole(userInfo) {
  if (!userInfo) return ''
  if (userInfo.userType === 1) return 'admin'
  if (userInfo.auditStatus !== undefined && userInfo.auditStatus !== null && userInfo.auditStatus !== 1) {
    return 'enterprise-review'
  }
  if (userInfo.userType === 2) return 'enterprise'
  return 'consumer'
}

App({
  globalData: {
    useMock: config.USE_MOCK,
    userRole: '',
    enterpriseType: null,
    userInfo: null,
    token: '',
    apiBaseUrl: config.API_BASE_URL
  },

  onLaunch() {
    this.checkLogin()
    this.checkUpdate()
  },

  checkLogin() {
    const token = wx.getStorageSync('token')
    const storedUserInfo = wx.getStorageSync('userInfo')
    const userInfo = normalizeUserInfo(storedUserInfo)
    const userRole = wx.getStorageSync('userRole')
    const enterpriseType = wx.getStorageSync('enterpriseType')

    if (token) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      this.globalData.enterpriseType = enterpriseType || (userInfo ? userInfo.enterpriseType || null : null)
      this.globalData.userRole = userRole || resolveUserRole(userInfo)

      if (userInfo) {
        wx.setStorageSync('userInfo', userInfo)
        wx.setStorageSync('userRole', this.globalData.userRole)
        wx.setStorageSync('enterpriseType', this.globalData.enterpriseType)
      }

      this.validateToken()
    }
  },

  validateToken() {
    const token = this.globalData.token
    if (!token) return

    const baseUrl = this.globalData.apiBaseUrl
    wx.request({
      url: baseUrl + '/auth/info',
      method: 'GET',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const info = normalizeUserInfo(res.data.data)
          this.globalData.userInfo = info
          if (info) {
            const role = resolveUserRole(info)
            this.globalData.userRole = role
            this.globalData.enterpriseType = info.enterpriseType || null
            wx.setStorageSync('userRole', role)
            wx.setStorageSync('enterpriseType', info.enterpriseType || null)
            wx.setStorageSync('userInfo', info)
          }
        } else {
          console.log('Token 已失效，已静默清除登录状态')
          this.clearLoginInfo()
        }
      },
      fail: () => {
        console.log('Token 验证请求失败（网络异常），保留本地缓存')
      }
    })
  },

  refreshUserInfo(callback) {
    const token = this.globalData.token
    if (!token) {
      if (callback) callback(null)
      return
    }
    const baseUrl = this.globalData.apiBaseUrl
    wx.request({
      url: baseUrl + '/auth/info',
      method: 'GET',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const info = normalizeUserInfo(res.data.data)
          this.globalData.userInfo = info
          if (info) {
            const role = resolveUserRole(info)
            this.globalData.userRole = role
            this.globalData.enterpriseType = info.enterpriseType || null
            wx.setStorageSync('userRole', role)
            wx.setStorageSync('enterpriseType', info.enterpriseType || null)
            wx.setStorageSync('userInfo', info)
          }
          if (callback) callback(info)
        } else {
          if (callback) callback(null)
        }
      },
      fail: () => {
        if (callback) callback(null)
      }
    })
  },

  setLoginInfo(data) {
    const normalizedUserInfo = normalizeUserInfo(data.userInfo)
    const role = resolveUserRole(normalizedUserInfo) || data.userRole || ''
    const enterpriseType = data.enterpriseType !== undefined
      ? data.enterpriseType
      : (normalizedUserInfo ? normalizedUserInfo.enterpriseType || null : null)

    this.globalData.token = data.token
    this.globalData.userRole = role
    this.globalData.enterpriseType = enterpriseType
    this.globalData.userInfo = normalizedUserInfo

    wx.setStorageSync('token', data.token)
    wx.setStorageSync('userRole', role)
    wx.setStorageSync('enterpriseType', enterpriseType)
    wx.setStorageSync('userInfo', normalizedUserInfo)
  },

  clearLoginInfo() {
    this.globalData.token = ''
    this.globalData.userRole = ''
    this.globalData.enterpriseType = null
    this.globalData.userInfo = null

    wx.removeStorageSync('token')
    wx.removeStorageSync('userRole')
    wx.removeStorageSync('enterpriseType')
    wx.removeStorageSync('userInfo')
  },

  isLoggedIn() {
    return !!(this.globalData.token && this.globalData.userInfo)
  },

  requireLogin() {
    if (this.isLoggedIn()) return true

    wx.showModal({
      title: '需要登录',
      content: '该功能需要登录后才能使用，是否前往登录？',
      confirmText: '去登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({
            url: '/pages/login/login'
          })
        }
      }
    })
    return false
  },

  isEnterpriseUser() {
    return this.globalData.userRole === 'enterprise' || this.globalData.userRole === 'enterprise-review'
  },

  isConsumer() {
    return this.globalData.userRole === 'consumer'
  },

  getEnterpriseTypeName() {
    const typeMap = {
      1: '种植养殖',
      2: '加工宰杀',
      3: '检疫质检'
    }
    return typeMap[this.globalData.enterpriseType] || '未知'
  },

  checkUpdate() {
    if (wx.canIUse('getUpdateManager')) {
      const updateManager = wx.getUpdateManager()

      updateManager.onCheckForUpdate(res => {
        if (res.hasUpdate) {
          updateManager.onUpdateReady(() => {
            wx.showModal({
              title: '更新提示',
              content: '新版本已准备好，是否重启应用？',
              success: res => {
                if (res.confirm) {
                  updateManager.applyUpdate()
                }
              }
            })
          })

          updateManager.onUpdateFailed(() => {
            wx.showModal({
              title: '更新失败',
              content: '新版本下载失败，请删除小程序后重新搜索打开',
              showCancel: false
            })
          })
        }
      })
    }
  }
})
