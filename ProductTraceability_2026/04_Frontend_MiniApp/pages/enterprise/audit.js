const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

Page({
  data: {
    enterpriseId: '',
    enterpriseInfo: {},
    showRejectModal: false,
    rejectReason: '',
    showEditModal: false,
    editForm: {
      avatar: '',
      introduction: '',
      contactPhone: '',
      address: ''
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        enterpriseId: options.id
      })
      
      // Try to get data from globalData first (passed from list page)
      if (app.globalData && app.globalData.currentEnterpriseForAudit) {
        const enterprise = app.globalData.currentEnterpriseForAudit
        this.processEnterpriseData(enterprise)
        // Clear after use
        app.globalData.currentEnterpriseForAudit = null
      } else {
        this.loadEnterpriseInfo()
      }
    }
    
    this.checkAdminPermission()
  },

  onShow() {
    // Only reload if no data exists (e.g., returning from other page)
  },

  checkAdminPermission() {
    const userInfo = app.globalData.userInfo || {}
    const userType = userInfo.userType || 3
    
    if (userType !== 1) {
      wx.showModal({
        title: '提示',
        content: '此页面仅供管理员使用',
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
      return false
    }
    
    return true
  },

  processEnterpriseData(enterprise) {
    // Map auditStatus to auditStatusText
    const statusTextMap = {
      0: '待审核',
      1: '已通过',
      2: '已拒绝'
    }
    
    const processedData = {
      ...enterprise,
      auditStatusText: statusTextMap[enterprise.auditStatus] || '未知',
      // Convert license strings to arrays if needed
      businessLicense: Array.isArray(enterprise.businessLicense) 
        ? enterprise.businessLicense.map(u => this.resolveImageUrl(u))
        : enterprise.businessLicense ? [this.resolveImageUrl(enterprise.businessLicense)] : [],
      productionLicense: Array.isArray(enterprise.productionLicense)
        ? enterprise.productionLicense.map(u => this.resolveImageUrl(u))
        : enterprise.productionLicense ? [this.resolveImageUrl(enterprise.productionLicense)] : [],
      otherCertificates: enterprise.otherCertificates || [],
      // Default values for stats
      chainCount: enterprise.chainCount || 0,
      batchCount: enterprise.batchCount || Math.floor((enterprise.chainCount || 0) * 0.3),
      productCount: enterprise.productCount || 3,
      // Default reject reason
      rejectReason: enterprise.rejectReason || '资料审核未通过'
    }
    
    this.setData({
      enterpriseInfo: processedData
    })
    // 代理http图片为本地临时文件，解决真机<image>无法加载http图片
    this.proxyLicenseImages(processedData)
  },

  proxyLicenseImages(data) {
    const proxyImg = util.proxyImage
    ;['businessLicense', 'productionLicense'].forEach(field => {
      const arr = data[field]
      if (Array.isArray(arr)) {
        arr.forEach((url, idx) => {
          if (url) {
            proxyImg(url).then(lp => {
              if (lp !== url) this.setData({ ['enterpriseInfo.' + field + '[' + idx + ']']: lp })
            })
          }
        })
      }
    })
  },

  loadEnterpriseInfo() {
    wx.showLoading({ title: '加载中...' })
    const typeNames = { 1: '种植养殖', 2: '加工屠杀', 3: '检疫质检' }
    
    request.get('/api/enterprise/detail/' + this.data.enterpriseId).then(res => {
      wx.hideLoading()
      if (res.code === 200 && res.data) {
        const d = res.data
        this.processEnterpriseData({
          id: d.id,
          enterpriseName: d.enterpriseName || '',
          enterpriseType: d.enterpriseType,
          enterpriseTypeName: typeNames[d.enterpriseType] || '未知',
          creditCode: d.enterpriseCode || '',
          legalPerson: d.legalPerson || '',
          contactPhone: d.contactPhone || '',
          address: d.address || '',
          introduction: d.introduction || '',
          applyTime: d.createTime || '',
          auditStatus: d.auditStatus,
          avatar: d.logo || null,
          businessLicense: d.businessLicense || '',
          productionLicense: d.productionLicense || '',
          otherCertificates: d.otherCertificates || [],
          rejectReason: d.auditRemark || '',
          chainCount: d.chainCount || 0,
          batchCount: d.batchCount || 0,
          productCount: d.productCount || 0
        })
      } else {
        wx.showToast({ title: '企业不存在', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  resolveImageUrl(path) {
    if (!path) return ''
    if (path.startsWith('http')) return path
    const baseUrl = (app && app.globalData ? app.globalData.apiBaseUrl : '')
    return baseUrl + path
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url
    const urls = e.currentTarget.dataset.urls
    wx.previewImage({
      current: url,
      urls: urls
    })
  },

  callPhone(e) {
    const phone = e.currentTarget.dataset.phone
    if (!phone) {
      wx.showToast({ title: '暂无联系电话', icon: 'none' })
      return
    }
    wx.makePhoneCall({
      phoneNumber: phone,
      fail: () => {
        wx.showToast({ title: '拨号取消', icon: 'none' })
      }
    })
  },

  // Reject Dialog
  showRejectDialog() {
    this.setData({
      showRejectModal: true,
      rejectReason: ''
    })
  },

  hideRejectDialog() {
    this.setData({
      showRejectModal: false,
      rejectReason: ''
    })
  },

  onRejectReasonInput(e) {
    this.setData({
      rejectReason: e.detail.value
    })
  },

  confirmReject() {
    if (!this.data.rejectReason.trim()) {
      wx.showToast({ title: '请输入驳回原因', icon: 'none' })
      return
    }
    
    wx.showModal({
      title: '确认驳回',
      content: '确定要驳回该企业的入驻申请吗？',
      confirmColor: '#F44336',
      success: (res) => {
        if (res.confirm) {
          this.rejectAudit()
        }
      }
    })
  },

  rejectAudit() {
    wx.showLoading({ title: '处理中...' })
    
    request.post('/api/enterprise/audit/approve', {
      enterpriseId: Number(this.data.enterpriseId),
      auditStatus: 2,
      auditRemark: this.data.rejectReason.trim()
    }).then(res => {
      wx.hideLoading()
      this.hideRejectDialog()
      if (res.code === 200) {
        wx.showToast({ title: '已驳回', icon: 'success' })
        setTimeout(() => { wx.navigateBack() }, 1500)
      } else {
        wx.showToast({ title: res.message || '操作失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '操作失败', icon: 'none' })
    })
  },

  approveAudit() {
    wx.showModal({
      title: '确认通过',
      content: '确定要通过该企业的入驻申请吗？通过后企业账号将可以登录使用。',
      confirmColor: '#4CAF50',
      success: (res) => {
        if (res.confirm) {
          this.doApproveAudit()
        }
      }
    })
  },

  doApproveAudit() {
    wx.showLoading({ title: '处理中...' })
    
    request.post('/api/enterprise/audit/approve', {
      enterpriseId: Number(this.data.enterpriseId),
      auditStatus: 1
    }).then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        wx.showModal({
          title: '审核通过',
          content: '企业入驻申请已通过，该企业账号现在可以登录使用。',
          showCancel: false,
          success: () => { wx.navigateBack() }
        })
      } else {
        wx.showToast({ title: res.message || '审核失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '审核失败', icon: 'none' })
    })
  },

  // Edit Modal
  openEditModal() {
    const { enterpriseInfo } = this.data
    
    // Only allow editing for approved enterprises
    if (enterpriseInfo.auditStatus !== 1) {
      wx.showToast({ title: '当前状态下不可编辑', icon: 'none' })
      return
    }
    
    this.setData({
      showEditModal: true,
      editForm: {
        avatar: enterpriseInfo.avatar || '',
        introduction: enterpriseInfo.introduction || '',
        contactPhone: enterpriseInfo.contactPhone || '',
        address: enterpriseInfo.address || ''
      }
    })
  },

  closeEditModal() {
    this.setData({ showEditModal: false })
  },

  onEditInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`editForm.${field}`]: e.detail.value
    })
  },

  chooseAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0]
        // TODO: Upload image and get URL
        this.setData({
          'editForm.avatar': tempFilePath
        })
      }
    })
  },

  saveEdit() {
    const { editForm, enterpriseId } = this.data
    
    if (!editForm.contactPhone) {
      wx.showToast({ title: '请输入联系电话', icon: 'none' })
      return
    }
    
    if (!/^1\d{10}$/.test(editForm.contactPhone)) {
      wx.showToast({ title: '请输入正确的手机号码', icon: 'none' })
      return
    }
    
    wx.showLoading({ title: '保存中...' })
    
    request.put('/api/enterprise/update', {
      id: Number(enterpriseId),
      introduction: editForm.introduction,
      contactPhone: editForm.contactPhone,
      address: editForm.address
    }).then(res => {
      wx.hideLoading()
      this.closeEditModal()
      if (res.code === 200) {
        this.setData({
          'enterpriseInfo.avatar': editForm.avatar,
          'enterpriseInfo.introduction': editForm.introduction,
          'enterpriseInfo.contactPhone': editForm.contactPhone,
          'enterpriseInfo.address': editForm.address
        })
        wx.showToast({ title: '保存成功', icon: 'success' })
      } else {
        wx.showToast({ title: res.message || '保存失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '保存失败', icon: 'none' })
    })
  },

  preventTouchMove() {
    return false
  },

  stopPropagation() {}
})
