const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    userId: '',
    userInfo: {},
    showEditModal: false,
    editForm: {
      avatar: '',
      realName: '',
      nickname: '',
      phone: '',
      email: '',
      resetPassword: false
    }
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ userId: options.id })
      
      // Try to get data from globalData first (passed from list page)
      if (app.globalData && app.globalData.currentUserForDetail) {
        const user = app.globalData.currentUserForDetail
        this.setData({ userInfo: user })
        // Clear after use
        app.globalData.currentUserForDetail = null
      } else {
        this.loadUserInfo()
      }
    }
  },

  onShow() {
    // Only reload if no data exists
  },

  loadUserInfo() {
    wx.showLoading({ title: '加载中...' })
    const enterpriseTypeNames = { 1: '种植养殖企业', 2: '加工屠杀企业', 3: '检疫质检企业' }
    
    request.get('/api/system/user/list', {
      current: 1,
      size: 1,
      keyword: this.data.userId
    }).then(res => {
      wx.hideLoading()
      const records = (res.data && res.data.records) || []
      if (records.length > 0) {
        const item = records[0]
        this.setData({
          userInfo: {
            id: item.id,
            username: item.username || '',
            realName: item.realName || '',
            nickname: item.nickname || '',
            userType: item.userType,
            phone: item.phone || '',
            email: item.email || '',
            status: item.status,
            avatar: item.avatar || null,
            enterpriseId: item.enterpriseId || null,
            enterpriseName: item.enterpriseName || null,
            enterpriseTypeName: item.enterpriseType ? (enterpriseTypeNames[item.enterpriseType] || null) : null,
            createTime: item.createTime || '',
            lastLoginTime: item.lastLoginTime || null,
            loginCount: item.loginCount || 0
          }
        })
      } else {
        wx.showToast({ title: '用户不存在', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载失败', icon: 'none' })
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

  // Edit Modal
  openEditModal() {
    const { userInfo } = this.data
    
    // Check if user is frozen
    if (userInfo.status === 0) {
      wx.showToast({ 
        title: '账号已冻结，请先解冻', 
        icon: 'none',
        duration: 2000
      })
      return
    }
    
    this.setData({
      showEditModal: true,
      editForm: {
        avatar: userInfo.avatar || '',
        realName: userInfo.realName || '',
        nickname: userInfo.nickname || '',
        phone: userInfo.phone || '',
        email: userInfo.email || '',
        resetPassword: false
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

  onResetSwitch(e) {
    this.setData({
      'editForm.resetPassword': e.detail.value
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
    const { editForm, userId, userInfo } = this.data
    
    // Validate phone
    if (editForm.phone && !/^1\d{10}$/.test(editForm.phone)) {
      wx.showToast({ title: '请输入正确的手机号码', icon: 'none' })
      return
    }
    
    // Validate email
    if (editForm.email && !/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(editForm.email)) {
      wx.showToast({ title: '请输入正确的邮箱地址', icon: 'none' })
      return
    }
    
    wx.showLoading({ title: '保存中...' })
    
    const updateData = {
      id: Number(userId),
      realName: editForm.realName,
      phone: editForm.phone,
      email: editForm.email
    }
    
    request.put('/api/system/user/update', updateData).then(res => {
      if (res.code === 200) {
        // 如果需要重置密码
        const resetPromise = editForm.resetPassword 
          ? request.put('/api/system/user/reset-password/' + userId) 
          : Promise.resolve(null)
        
        resetPromise.then(() => {
          wx.hideLoading()
          this.closeEditModal()
          this.setData({
            'userInfo.avatar': editForm.avatar || userInfo.avatar,
            'userInfo.realName': editForm.realName,
            'userInfo.nickname': editForm.nickname,
            'userInfo.phone': editForm.phone,
            'userInfo.email': editForm.email
          })
          let msg = '保存成功'
          if (editForm.resetPassword) msg = '保存成功，密码已重置'
          wx.showToast({ title: msg, icon: 'success' })
        })
      } else {
        wx.hideLoading()
        wx.showToast({ title: res.message || '保存失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '保存失败', icon: 'none' })
    })
  },

  toggleStatus() {
    const { userInfo } = this.data
    const action = userInfo.status === 1 ? '冻结' : '解冻'
    
    wx.showModal({
      title: `确认${action}`,
      content: `确定要${action}用户「${userInfo.realName || userInfo.username}」吗？`,
      confirmColor: userInfo.status === 1 ? '#F44336' : '#4CAF50',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' })
          
          const newStatus = userInfo.status === 1 ? 0 : 1
          request.put('/api/system/user/toggle-status/' + userInfo.id + '?status=' + newStatus).then(r => {
            wx.hideLoading()
            if (r.code === 200) {
              this.setData({ 'userInfo.status': newStatus })
              wx.showToast({ title: `已${action}`, icon: 'success' })
            } else {
              wx.showToast({ title: r.message || '操作失败', icon: 'none' })
            }
          }).catch(() => {
            wx.hideLoading()
            wx.showToast({ title: '操作失败', icon: 'none' })
          })
        }
      }
    })
  },

  preventTouchMove() {
    return false
  },

  stopPropagation() {}
})
