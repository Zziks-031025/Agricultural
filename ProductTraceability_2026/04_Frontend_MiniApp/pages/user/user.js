const app = getApp()
const util = require('../../utils/util.js')
const request = require('../../utils/request.js')

Page({
  data: {
    isLogin: false,
    userInfo: null,
    userRole: '',
    userType: null,
    roleName: '',
    auditStatus: null,
    auditRemark: '',
    unreadCount: 0,
    isAdmin: false,
    isEnterprise: false,
    // 企业专属数据
    enterpriseType: 0,
    isFarmer: false,
    isProcessor: false,
    isQuarantine: false,
    enterpriseInfo: {
      logo: '',
      name: '',
      enterpriseCode: '',
      contactPhone: '',
      address: '',
      businessLicense: '',
      productionLicense: ''
    },
    chainRecordCount: 0,
  },

  onLoad() {
    
  },

  onShow() {
    if (app.globalData.token) {
      app.refreshUserInfo(() => {
        this.loadUserInfo()
      })
    } else {
      this.loadUserInfo()
    }
  },

  loadUserInfo() {
    const token = app.globalData.token
    const userInfo = app.globalData.userInfo
    const userRole = app.globalData.userRole

    if (token && userInfo) {
      const userType = userInfo.userType || 3
      const isAdmin = userType === 1
      const isEnterprise = userType === 2
      const enterpriseType = isEnterprise ? (userInfo.enterpriseType || app.globalData.enterpriseType || 0) : 0
      const isFarmer = isEnterprise && enterpriseType === 1
      const isProcessor = isEnterprise && enterpriseType === 2
      const isQuarantine = isEnterprise && enterpriseType === 3
      
      this.setData({
        isLogin: true,
        userInfo: userInfo,
        userRole: userRole,
        userType: userType,
        roleName: this.getRoleName(userType),
        auditStatus: userInfo.auditStatus,
        auditRemark: userInfo.auditRemark || '',
        isAdmin: isAdmin,
        isEnterprise: isEnterprise,
        enterpriseType: enterpriseType,
        isFarmer: isFarmer,
        isProcessor: isProcessor,
        isQuarantine: isQuarantine
      })
      
      this.loadUnreadCount()
      
      // 如果是养殖户、加工厂或检疫机构，加载企业详情
      if ((isFarmer || isProcessor || isQuarantine) && userInfo.enterpriseId) {
        this.loadEnterpriseInfo(userInfo.enterpriseId)
      }
    } else {
      this.setData({
        isLogin: false,
        userInfo: null,
        userRole: '',
        userType: null,
        roleName: '',
        isAdmin: false,
        isEnterprise: false,
        enterpriseType: 0,
        isFarmer: false,
        isProcessor: false,
        isQuarantine: false
      })
    }
  },

  // 加载企业信息
  loadEnterpriseInfo(enterpriseId) {
    request.get(`/api/enterprise/detail/${enterpriseId}`).then(res => {
      if (res.code === 200 && res.data) {
        const data = res.data
        const fullAddress = [data.province, data.city, data.district, data.address].filter(Boolean).join('')
        const logoUrl = this.resolveImageUrl(data.logo)
        const blUrl = this.resolveImageUrl(data.businessLicense)
        const plUrl = this.resolveImageUrl(data.productionLicense)
        this.setData({
          enterpriseInfo: {
            logo: logoUrl || '',
            name: data.enterpriseName || '',
            enterpriseCode: data.enterpriseCode || '',
            contactPhone: data.contactPhone || '',
            address: fullAddress,
            businessLicense: blUrl,
            productionLicense: plUrl
          },
          chainRecordCount: 0
        })
        // 代理http图片为本地临时文件，解决真机<image>无法加载http图片
        if (logoUrl) util.proxyImage(logoUrl).then(lp => { if (lp !== logoUrl) this.setData({ 'enterpriseInfo.logo': lp }) })
        if (blUrl) util.proxyImage(blUrl).then(lp => { if (lp !== blUrl) this.setData({ 'enterpriseInfo.businessLicense': lp }) })
        if (plUrl) util.proxyImage(plUrl).then(lp => { if (lp !== plUrl) this.setData({ 'enterpriseInfo.productionLicense': lp }) })
        // 获取真实的上链记录数
        request.get('/api/blockchain/stats').then(r => {
          if (r.code === 200 && r.data) {
            this.setData({ chainRecordCount: r.data.totalCount || 0 })
          }
        }).catch(() => {})
      }
    }).catch(err => {
      console.error('获取企业信息失败:', err)
      wx.showToast({ title: '加载企业信息失败', icon: 'none' })
    })
  },

  getRoleName(userType) {
    if (userType === 1) {
      return '系统管理员'
    } else if (userType === 2) {
      return this.getEnterpriseTypeName()
    } else {
      return '普通用户'
    }
  },

  getEnterpriseTypeName() {
    const enterpriseType = app.globalData.enterpriseType
    const typeMap = {
      1: '种植养殖企业',
      2: '加工宰杀企业',
      3: '检疫质检企业'
    }
    return typeMap[enterpriseType] || '企业用户'
  },

  loadUnreadCount() {
    this.setData({
      unreadCount: 0
    })
  },

  navigateTo(e) {
    const url = e.currentTarget.dataset.url
    if (!url) {
      util.showError('功能开发中')
      return
    }

    wx.navigateTo({
      url: url
    })
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  },

  goToRegister() {
    wx.navigateTo({
      url: '/pages/login/register'
    })
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 清除所有登录信息（globalData + 本地缓存）
          app.clearLoginInfo()
          
          // 重置当前页面数据
          this.setData({
            isLogin: false,
            userInfo: null,
            userRole: '',
            userType: null,
            roleName: '',
            isAdmin: false,
            isEnterprise: false,
            enterpriseType: 0,
            isFarmer: false,
            isProcessor: false,
            isQuarantine: false
          })
          
          util.showSuccess('已退出登录')
          
          // 跳转首页，触发首页 onShow → initPage() 刷新为游客视图
          setTimeout(() => {
            wx.switchTab({
              url: '/pages/index/index'
            })
          }, 1000)
        }
      }
    })
  },

  // 更换Logo（走审核流程）
  changeLogo() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFiles[0].tempFilePath
        wx.showLoading({ title: '上传中...', mask: true })
        const submitHelper = require('../../utils/submit-helper.js')
        submitHelper.uploadImage(tempFilePath, 'logo').then(serverUrl => {
          const userInfo = app.globalData.userInfo || {}
          const enterpriseId = userInfo.enterpriseId
          if (!enterpriseId) {
            wx.hideLoading()
            util.showError('企业信息异常')
            return
          }
          // 提交图片审核而非直接更新
          request.post('/api/image-audit/submit', {
            enterpriseId: enterpriseId,
            userId: userInfo.id || null,
            fieldName: 'logo',
            oldValue: this.data.enterpriseInfo.logo || '',
            newValue: serverUrl
          }).then(auditRes => {
            wx.hideLoading()
            if (auditRes.code === 200) {
              wx.showModal({
                title: '提示',
                content: 'Logo变更已提交审核，管理员审核通过后将自动生效。',
                showCancel: false
              })
            } else {
              util.showError(auditRes.message || '提交失败')
            }
          }).catch(() => {
            wx.hideLoading()
            util.showError('提交审核失败')
          })
        }).catch(() => {
          wx.hideLoading()
          util.showError('Logo上传失败')
        })
      }
    })
  },

  // 将相对路径转为后端完整URL
  resolveImageUrl(path) {
    if (!path) return ''
    if (path.startsWith('http')) return path
    const baseUrl = (app && app.globalData ? app.globalData.apiBaseUrl : '')
    return baseUrl + path
  },

  // 预览资质图片
  previewLicense(e) {
    const type = e.currentTarget.dataset.type
    const { enterpriseInfo } = this.data
    let urls = []
    let current = ''
    
    if (type === 'business' && enterpriseInfo.businessLicense) {
      current = enterpriseInfo.businessLicense
      urls = [enterpriseInfo.businessLicense]
    } else if (type === 'production' && enterpriseInfo.productionLicense) {
      current = enterpriseInfo.productionLicense
      urls = [enterpriseInfo.productionLicense]
    }
    
    if (urls.length > 0) {
      wx.previewImage({ current, urls })
    } else {
      util.showError('暂无资质图片')
    }
  },

  // 复制企业识别码
  copyEnterpriseCode() {
    const code = this.data.enterpriseInfo.enterpriseCode
    if (code) {
      wx.setClipboardData({
        data: code,
        success: () => {
          util.showSuccess('已复制')
        }
      })
    }
  },

  // 跳转区块链存证记录列表
  goToChainRecords() {
    wx.navigateTo({
      url: '/pages/blockchain/record-list',
      fail: () => {
        util.showError('功能开发中')
      }
    })
  },

  // 跳转修改密码
  goToChangePassword() {
    wx.navigateTo({
      url: '/pages/user/profile?tab=password',
      fail: () => {
        util.showError('功能开发中')
      }
    })
  }
})
