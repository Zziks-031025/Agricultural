/**
 * 用户管理页面
 * 统一入口：工作台/我的页面 → 本页面
 * 通过 source 参数区分入口，展示不同的默认视图
 * - source=workbench: 默认展示待处理用户（如新注册用户）
 * - source=profile: 默认展示全部用户，支持角色筛选
 */
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    // 入口来源
    source: 'workbench',
    
    // 统计数据
    stats: {
      total: 0,
      active: 0,
      frozen: 0
    },
    
    // 筛选条件
    searchKeyword: '',
    currentStatus: 'all',     // all, active, frozen
    currentRole: 'all',       // all, 1, 2, 3
    showRoleFilter: false,
    
    // 列表数据
    userList: [],
    page: 1,
    pageSize: 10,
    loading: false,
    noMore: false,
    
    // 弹窗控制
    showResetPwdModal: false,
    showRoleModal: false,
    currentUser: null,
    
    // 修改权限相关
    selectedRole: null,
    selectedEnterprise: null,
    enterpriseOptions: []
  },

  onLoad(options) {
    // 获取入口来源
    const source = options.source || 'workbench'
    
    // 根据来源设置默认筛选状态
    let showRoleFilter = false
    
    if (source === 'profile') {
      // 从"我的"进入：显示角色筛选
      showRoleFilter = true
    }
    
    this.setData({
      source,
      showRoleFilter
    })
    
    // 设置页面标题
    wx.setNavigationBarTitle({
      title: source === 'profile' ? '用户档案管理' : '用户管理'
    })
    
    // 加载企业列表（用于修改权限时选择）
    this.loadEnterpriseOptions()
  },

  onShow() {
    this.loadStats()
    this.refreshList()
  },

  onPullDownRefresh() {
    this.refreshList().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  /**
   * 加载统计数据
   */
  loadStats() {
    const fetchCount = (status) => {
      const params = { current: 1, size: 1 }
      if (status !== undefined) params.status = status
      return request.get('/api/system/user/list', params).then(res => {
        return (res.data && res.data.total) || 0
      }).catch(() => 0)
    }
    Promise.all([
      fetchCount(),
      fetchCount(1),
      fetchCount(0)
    ]).then(([total, active, frozen]) => {
      this.setData({ stats: { total, active, frozen } })
    })
  },

  /**
   * 加载企业选项
   */
  loadEnterpriseOptions() {
    request.get('/api/enterprise/list', { current: 1, size: 100, auditStatus: 1 }).then(res => {
      const records = (res.data && res.data.records) || []
      this.setData({
        enterpriseOptions: records.map(item => ({
          id: item.id,
          name: item.enterpriseName || '',
          type: item.enterpriseType
        }))
      })
    }).catch(() => {})
  },

  /**
   * 刷新列表
   */
  refreshList() {
    this.setData({
      page: 1,
      userList: [],
      noMore: false
    })
    return this.loadUserList()
  },

  /**
   * 加载用户列表
   */
  loadUserList() {
    if (this.data.loading || this.data.noMore) {
      return Promise.resolve()
    }

    this.setData({ loading: true })

    const { currentStatus, currentRole, searchKeyword, page, pageSize } = this.data
    const params = { current: page, size: pageSize }
    if (searchKeyword) params.keyword = searchKeyword
    if (currentRole !== 'all') params.userType = parseInt(currentRole)
    if (currentStatus === 'active') params.status = 1
    else if (currentStatus === 'frozen') params.status = 0

    const enterpriseTypeNames = { 1: '种植养殖企业', 2: '加工屠杀企业', 3: '检疫质检企业' }

    return request.get('/api/system/user/list', params).then(res => {
      const records = (res.data && res.data.records) || []
      const total = (res.data && res.data.total) || 0
      const list = records.map(item => ({
        id: item.id,
        username: item.username || '',
        realName: item.realName || '',
        nickname: item.nickname || '',
        userType: item.userType,
        phone: item.phone || '',
        email: item.email || '',
        enterpriseId: item.enterpriseId || null,
        enterpriseName: item.enterpriseName || null,
        enterpriseTypeName: item.enterpriseType ? (enterpriseTypeNames[item.enterpriseType] || null) : null,
        status: item.status,
        createTime: item.createTime || '',
        lastLoginTime: item.lastLoginTime || null,
        loginCount: item.loginCount || 0,
        avatar: item.avatar || null
      }))
      const newList = page === 1 ? list : [...this.data.userList, ...list]
      this.setData({
        userList: newList,
        loading: false,
        noMore: newList.length >= total
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /**
   * 搜索输入
   */
  onSearchInput(e) {
    this.setData({ searchKeyword: e.detail.value })
    
    clearTimeout(this.searchTimer)
    this.searchTimer = setTimeout(() => {
      this.refreshList()
    }, 500)
  },

  /**
   * 清除搜索
   */
  clearSearch() {
    this.setData({ searchKeyword: '' })
    this.refreshList()
  },

  /**
   * 切换状态筛选
   */
  switchStatus(e) {
    const status = e.currentTarget.dataset.status
    if (status === this.data.currentStatus) return
    
    this.setData({ currentStatus: status })
    this.refreshList()
  },

  /**
   * 切换角色筛选
   */
  switchRole(e) {
    const role = e.currentTarget.dataset.role
    if (role === this.data.currentRole) return
    
    this.setData({ currentRole: role })
    this.refreshList()
  },

  /**
   * 加载更多
   */
  loadMore() {
    if (!this.data.loading && !this.data.noMore) {
      this.setData({ page: this.data.page + 1 })
      this.loadUserList()
    }
  },

  /**
   * 重置密码
   */
  resetPassword(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showResetPwdModal: true,
      currentUser: item
    })
  },

  /**
   * 关闭重置密码弹窗
   */
  closeResetPwdModal() {
    this.setData({ showResetPwdModal: false })
  },

  /**
   * 确认重置密码
   */
  confirmResetPassword() {
    wx.showLoading({ title: '处理中...' })
    
    request.put('/api/system/user/reset-password/' + this.data.currentUser.id).then(res => {
      wx.hideLoading()
      this.closeResetPwdModal()
      if (res.code === 200) {
        wx.showToast({ title: '密码已重置', icon: 'success' })
      } else {
        wx.showToast({ title: res.message || '重置失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '重置失败', icon: 'none' })
    })
  },

  /**
   * 修改权限
   */
  changeRole(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showRoleModal: true,
      currentUser: item,
      selectedRole: item.userType,
      selectedEnterprise: null
    })
  },

  /**
   * 关闭权限弹窗
   */
  closeRoleModal() {
    this.setData({
      showRoleModal: false,
      selectedRole: null,
      selectedEnterprise: null
    })
  },

  /**
   * 选择角色
   */
  selectRole(e) {
    const role = parseInt(e.currentTarget.dataset.role)
    this.setData({
      selectedRole: role,
      selectedEnterprise: role !== 2 ? null : this.data.selectedEnterprise
    })
  },

  /**
   * 选择企业
   */
  onEnterpriseChange(e) {
    const index = e.detail.value
    this.setData({
      selectedEnterprise: this.data.enterpriseOptions[index]
    })
  },

  /**
   * 确认修改权限
   */
  confirmChangeRole() {
    const { selectedRole, selectedEnterprise, currentUser } = this.data
    
    if (selectedRole === 2 && !selectedEnterprise) {
      wx.showToast({ title: '请选择关联企业', icon: 'none' })
      return
    }
    
    if (selectedRole === currentUser.userType) {
      wx.showToast({ title: '角色未变更', icon: 'none' })
      return
    }
    
    wx.showLoading({ title: '处理中...' })
    
    const updateData = {
      id: currentUser.id,
      userType: selectedRole,
      enterpriseId: selectedEnterprise ? selectedEnterprise.id : null
    }
    request.put('/api/system/user/update', updateData).then(res => {
      wx.hideLoading()
      this.closeRoleModal()
      if (res.code === 200) {
        wx.showToast({ title: '权限已修改', icon: 'success' })
        this.refreshList()
      } else {
        wx.showToast({ title: res.message || '修改失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '修改失败', icon: 'none' })
    })
  },

  /**
   * 切换用户状态（冻结/解冻）
   */
  toggleUserStatus(e) {
    const item = e.currentTarget.dataset.item
    const action = item.status === 1 ? '冻结' : '解除冻结'
    
    wx.showModal({
      title: '确认操作',
      content: `确定要${action}用户「${item.realName || item.username}」吗？`,
      confirmColor: item.status === 1 ? '#F44336' : '#4CAF50',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' })
          
          const newStatus = item.status === 1 ? 0 : 1
          request.put('/api/system/user/toggle-status/' + item.id + '?status=' + newStatus).then(r => {
            wx.hideLoading()
            if (r.code === 200) {
              wx.showToast({ title: `已${action}`, icon: 'success' })
              this.refreshList()
              this.loadStats()
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

  /**
   * 跳转到用户详情页
   */
  goToUserDetail(e) {
    const item = e.currentTarget.dataset.item
    // Store user data in globalData for detail page
    app.globalData = app.globalData || {}
    app.globalData.currentUserForDetail = item
    wx.navigateTo({
      url: `/pages/admin/user-detail?id=${item.id}`
    })
  },

  /**
   * 拨打用户电话
   */
  callUser(e) {
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

  /**
   * 阻止事件冒泡
   */
  preventClose() {},

  /**
   * 阻止触摸移动事件穿透（背景区域）
   */
  preventTouchMove() {
    return false;
  },

  /**
   * 阻止事件冒泡但允许滚动
   */
  stopPropagation() {
    // 空方法，仅用于阻止事件冒泡到 overlay
  }
})
