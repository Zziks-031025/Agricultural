const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

Page({
  data: {
    userList: [],
    loading: false,
    isEmpty: false,
    showActions: false,
    currentUser: {}
  },

  onLoad() {
    this.checkAdminPermission()
  },

  onShow() {
    this.loadUserList()
  },

  checkAdminPermission() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
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

  loadUserList() {
    this.setData({ loading: true })
    
    const userTypeNames = { 1: '管理员', 2: '企业用户', 3: '消费者' }
    
    request.get('/api/system/user/list', { current: 1, size: 50 }).then(res => {
      const records = (res.data && res.data.records) || []
      const list = records.map(item => ({
        id: item.id,
        username: item.username || '',
        realName: item.realName || '',
        avatar: item.avatar || '',
        enterpriseName: item.enterpriseName || '',
        userType: item.userType,
        userTypeName: userTypeNames[item.userType] || '未知',
        status: item.status,
        statusText: item.status === 1 ? '正常' : '已禁用'
      }))
      this.setData({
        userList: list,
        loading: false,
        isEmpty: list.length === 0
      })
    }).catch(() => {
      this.setData({ loading: false, isEmpty: true })
    })
  },

  showUserActions(e) {
    const user = e.currentTarget.dataset.user
    this.setData({
      currentUser: user,
      showActions: true
    })
  },

  hideUserActions() {
    this.setData({
      showActions: false,
      currentUser: {}
    })
  },

  toggleUserStatus() {
    const user = this.data.currentUser
    const action = user.status === 1 ? '禁用' : '启用'
    
    wx.showModal({
      title: `确认${action}`,
      content: `确定要${action}用户"${user.realName || user.username}"吗？`,
      success: (res) => {
        if (res.confirm) {
          this.doToggleUserStatus(user)
        }
      }
    })
  },

  doToggleUserStatus(user) {
    wx.showLoading({ title: '处理中...' })
    
    const newStatus = user.status === 1 ? 0 : 1
    request.put('/api/system/user/toggle-status/' + user.id + '?status=' + newStatus).then(res => {
      wx.hideLoading()
      if (res.code === 200) {
        const userList = this.data.userList.map(item => {
          if (item.id === user.id) {
            return {
              ...item,
              status: newStatus,
              statusText: newStatus === 1 ? '正常' : '已禁用'
            }
          }
          return item
        })
        this.setData({ userList, showActions: false, currentUser: {} })
        wx.showToast({ title: newStatus === 1 ? '已启用' : '已禁用', icon: 'success' })
      } else {
        wx.showToast({ title: res.message || '操作失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '操作失败', icon: 'none' })
    })
  },

  resetPassword() {
    const user = this.data.currentUser
    
    wx.showModal({
      title: '确认重置密码',
      content: `确定要重置用户"${user.realName || user.username}"的密码吗？密码将重置为初始密码：123456`,
      success: (res) => {
        if (res.confirm) {
          this.doResetPassword(user)
        }
      }
    })
  },

  doResetPassword(user) {
    wx.showLoading({ title: '处理中...' })
    
    request.put('/api/system/user/reset-password/' + user.id).then(res => {
      wx.hideLoading()
      this.setData({ showActions: false, currentUser: {} })
      if (res.code === 200) {
        wx.showModal({
          title: '密码重置成功',
          content: '密码已重置为初始密码\n请通知用户及时修改密码',
          showCancel: false
        })
      } else {
        wx.showToast({ title: res.message || '重置失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '重置失败', icon: 'none' })
    })
  },

  onPullDownRefresh() {
    this.loadUserList()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
