// 我的消息页面
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    currentTab: 'all', // all, system, business
    messageList: [],
    loading: false,
    noMore: false,
    page: 1,
    pageSize: 20,
    isAllSelected: false,
    unreadCount: {
      all: 0,
      system: 0,
      business: 0
    }
  },

  onLoad() {
    this.loadMessages()
    this.loadUnreadCount()
  },

  onShow() {
    // 每次显示时刷新未读数
    this.loadUnreadCount()
  },

  onPullDownRefresh() {
    this.setData({
      page: 1,
      messageList: [],
      noMore: false
    })
    this.loadMessages().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 切换标签
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab === this.data.currentTab) return

    this.setData({
      currentTab: tab,
      page: 1,
      messageList: [],
      noMore: false,
      isAllSelected: false
    })
    this.loadMessages()
  },

  // 加载消息列表
  loadMessages() {
    if (this.data.loading || this.data.noMore) return Promise.resolve()

    this.setData({ loading: true })

    const params = {
      current: this.data.page,
      size: this.data.pageSize
    }
    if (this.data.currentTab !== 'all') {
      params.type = this.data.currentTab
    }

    return request.get('/api/message/list', params).then(res => {
      const records = (res.data && res.data.records) || []
      const list = records.map(item => ({
        id: item.id,
        type: item.type || 'system',
        title: item.title || '',
        summary: item.summary || '',
        content: item.content || '',
        isRead: item.isRead || false,
        createTime: item.createTime || '',
        timeText: item.createTime ? item.createTime.substring(5, 16) : ''
      }))

      this.setData({
        messageList: this.data.page === 1 ? list : [...this.data.messageList, ...list],
        loading: false,
        noMore: list.length < this.data.pageSize,
        page: this.data.page + 1
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  // 加载未读数量
  loadUnreadCount() {
    request.get('/api/message/unread-count').then(res => {
      if (res.code === 200 && res.data) {
        this.setData({
          unreadCount: {
            all: res.data.all || 0,
            system: res.data.system || 0,
            business: res.data.business || 0
          }
        })
      }
    }).catch(() => {})
  },

  // 加载更多
  loadMore() {
    this.loadMessages()
  },

  // 查看详情
  viewDetail(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: `/pages/user/message-detail?id=${item.id}&type=${item.type}`
    })
  },

  // 切换单个选择
  toggleSelect(e) {
    const id = e.currentTarget.dataset.id
    const messageList = this.data.messageList.map(item => {
      if (item.id === id) {
        return { ...item, selected: !item.selected }
      }
      return item
    })

    const selectedCount = messageList.filter(item => item.selected).length
    const isAllSelected = selectedCount === messageList.length && messageList.length > 0

    this.setData({ messageList, isAllSelected })
  },

  // 切换全选
  toggleSelectAll() {
    const isAllSelected = !this.data.isAllSelected
    const messageList = this.data.messageList.map(item => ({
      ...item,
      selected: isAllSelected
    }))

    this.setData({ messageList, isAllSelected })
  },

  // 标记已读
  markSelectedAsRead() {
    const selectedMessages = this.data.messageList.filter(item => item.selected)
    if (selectedMessages.length === 0) {
      wx.showToast({
        title: '请先选择消息',
        icon: 'none'
      })
      return
    }

    const ids = selectedMessages.map(item => Number(item.id))
    request.post('/api/message/mark-read', { ids }).then(res => {
      if (res.code === 200) {
        const messageList = this.data.messageList.map(item => {
          if (item.selected) {
            return { ...item, isRead: true, selected: false }
          }
          return item
        })
        this.setData({ messageList, isAllSelected: false })
        this.loadUnreadCount()
        wx.showToast({ title: `已标记 ${selectedMessages.length} 条为已读`, icon: 'success' })
      } else {
        wx.showToast({ title: res.message || '操作失败', icon: 'none' })
      }
    }).catch(() => {
      wx.showToast({ title: '操作失败', icon: 'none' })
    })
  },

  // 删除选中
  deleteSelected() {
    const selectedMessages = this.data.messageList.filter(item => item.selected)
    if (selectedMessages.length === 0) {
      wx.showToast({
        title: '请先选择消息',
        icon: 'none'
      })
      return
    }

    wx.showModal({
      title: '确认删除',
      content: `确定要删除选中的 ${selectedMessages.length} 条消息吗？`,
      confirmColor: '#F44336',
      success: (res) => {
        if (res.confirm) {
          const ids = selectedMessages.map(item => Number(item.id))
          request.post('/api/message/delete', { ids }).then(res => {
            if (res.code === 200) {
              const messageList = this.data.messageList.filter(item => !item.selected)
              this.setData({ messageList, isAllSelected: false })
              this.loadUnreadCount()
              wx.showToast({ title: '删除成功', icon: 'success' })
            } else {
              wx.showToast({ title: res.message || '删除失败', icon: 'none' })
            }
          }).catch(() => {
            wx.showToast({ title: '删除失败', icon: 'none' })
          })
        }
      }
    })
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '农产品溯源系统',
      path: '/pages/index/index'
    }
  }
})
