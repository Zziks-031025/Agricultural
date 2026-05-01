const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

Page({
  data: {
    enterpriseList: [],
    loading: false,
    isEmpty: false
  },

  onLoad() {
    this.checkAdminPermission()
  },

  onShow() {
    this.loadEnterpriseList()
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

  loadEnterpriseList() {
    this.setData({ loading: true })
    
    const typeNames = { 1: '种植养殖', 2: '加工宰杀', 3: '检疫质检' }
    
    request.get('/api/enterprise/list', { current: 1, size: 50, auditStatus: 0 }).then(res => {
      const records = (res.data && res.data.records) || []
      const list = records.map(item => ({
        id: item.id,
        enterpriseName: item.enterpriseName || '',
        enterpriseType: item.enterpriseType,
        enterpriseTypeName: typeNames[item.enterpriseType] || '未知',
        applyTime: item.createTime || '',
        auditStatus: item.auditStatus
      }))
      this.setData({
        enterpriseList: list,
        loading: false,
        isEmpty: list.length === 0
      })
    }).catch(() => {
      this.setData({ loading: false, isEmpty: true })
    })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/enterprise/audit?id=${id}`
    })
  },

  onPullDownRefresh() {
    this.loadEnterpriseList()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
