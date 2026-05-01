// 消息详情页面
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    messageId: '',
    messageType: '',
    messageDetail: {}
  },

  onLoad(options) {
    const { id, type } = options
    const numId = Number(id)
    this.setData({
      messageId: numId,
      messageType: type
    })
    this.loadMessageDetail(numId)
    this.markAsRead(numId)
  },

  // 加载消息详情
  loadMessageDetail(id) {
    request.get('/api/message/detail/' + id).then(res => {
      if (res.code === 200 && res.data) {
        const detail = res.data
        this.setData({ messageDetail: detail })
        wx.setNavigationBarTitle({
          title: detail.type === 'system' ? '系统通知' : '业务提醒'
        })
      } else {
        wx.showToast({ title: '消息不存在', icon: 'none' })
      }
    }).catch(() => {
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  // 标记为已读
  markAsRead(id) {
    request.post('/api/message/mark-read', { ids: [id] }).catch(() => {})
  },

  // 前往相关页面
  goToAction() {
    const { actionUrl } = this.data.messageDetail
    if (actionUrl) {
      // 处理 tabBar 页面
      if (actionUrl.includes('/pages/workbench/workbench')) {
        wx.switchTab({
          url: '/pages/workbench/workbench'
        })
      } else {
        wx.navigateTo({
          url: actionUrl
        })
      }
    }
  },

  // 删除消息
  deleteMessage() {
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条消息吗？',
      confirmColor: '#F44336',
      success: (res) => {
        if (res.confirm) {
          request.post('/api/message/delete', { ids: [this.data.messageId] }).then(r => {
            if (r.code === 200) {
              wx.showToast({ title: '删除成功', icon: 'success' })
              setTimeout(() => { wx.navigateBack() }, 1500)
            } else {
              wx.showToast({ title: r.message || '删除失败', icon: 'none' })
            }
          }).catch(() => {
            wx.showToast({ title: '删除失败', icon: 'none' })
          })
        }
      }
    })
  },

  // 返回列表
  goBack() {
    wx.navigateBack()
  },

  // 分享
  onShareAppMessage() {
    return {
      title: this.data.messageDetail.title || '消息详情',
      path: '/pages/index/index'
    }
  }
})
