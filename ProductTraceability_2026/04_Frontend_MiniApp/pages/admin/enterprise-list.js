/**
 * 企业管理页面
 * 统一入口：工作台/我的页面 → 本页面
 * 通过 source 参数区分入口，展示不同的默认视图
 * - source=workbench: 默认展示待审核企业（侧重即时审批）
 * - source=profile: 默认展示全部企业（侧重档案维护）
 */
const app = getApp()
const request = require('../../utils/request.js')
const { proxyImage } = require('../../utils/util.js')

Page({
  data: {
    // 入口来源
    source: 'workbench',
    
    // 统计数据
    stats: {
      total: 0,
      pending: 0,
      approved: 0,
      rejected: 0
    },
    
    // 筛选条件
    searchKeyword: '',
    currentStatus: 'pending',  // all, pending, approved, rejected
    currentType: 'all',        // all, 1, 2, 3
    showTypeFilter: false,
    
    // 列表数据
    enterpriseList: [],
    page: 1,
    pageSize: 10,
    loading: false,
    noMore: false,
    
    // 弹窗控制
    showLicenseModal: false,
    showRejectModal: false,
    currentEnterprise: null,
    rejectReason: ''
  },

  onLoad(options) {
    // 获取入口来源
    const source = options.source || 'workbench'
    
    // 根据来源设置默认筛选状态
    let defaultStatus = 'pending'
    let showTypeFilter = false
    
    if (source === 'profile') {
      // 从"我的"进入：展示全部，显示类型筛选
      defaultStatus = 'all'
      showTypeFilter = true
    }
    
    this.setData({
      source,
      currentStatus: defaultStatus,
      showTypeFilter
    })
    
    // 设置页面标题
    wx.setNavigationBarTitle({
      title: source === 'profile' ? '企业档案管理' : '企业审核'
    })
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
    // 查询各状态数量
    const fetchCount = (auditStatus) => {
      return request.get('/api/enterprise/list', { current: 1, size: 1, auditStatus }).then(res => {
        return (res.data && res.data.total) || 0
      }).catch(() => 0)
    }
    Promise.all([
      request.get('/api/enterprise/list', { current: 1, size: 1 }).then(r => (r.data && r.data.total) || 0).catch(() => 0),
      fetchCount(0),
      fetchCount(1),
      fetchCount(2)
    ]).then(([total, pending, approved, rejected]) => {
      this.setData({ stats: { total, pending, approved, rejected } })
    })
  },

  /**
   * 刷新列表
   */
  refreshList() {
    this.setData({
      page: 1,
      enterpriseList: [],
      noMore: false
    })
    return this.loadEnterpriseList()
  },

  /**
   * 加载企业列表
   */
  loadEnterpriseList() {
    if (this.data.loading || this.data.noMore) {
      return Promise.resolve()
    }

    this.setData({ loading: true })

    const { currentStatus, currentType, searchKeyword, page, pageSize } = this.data
    const params = { current: page, size: pageSize }
    if (searchKeyword) params.keyword = searchKeyword
    if (currentType !== 'all') params.enterpriseType = parseInt(currentType)
    if (currentStatus === 'pending') params.auditStatus = 0
    else if (currentStatus === 'approved') params.auditStatus = 1
    else if (currentStatus === 'rejected') params.auditStatus = 2

    return request.get('/api/enterprise/list', params).then(res => {
      const records = (res.data && res.data.records) || []
      const total = (res.data && res.data.total) || 0
      const typeNames = { 1: '种植养殖企业', 2: '加工屠杀企业', 3: '检疫质检企业' }
      const list = records.map(item => ({
        id: item.id,
        enterpriseName: item.enterpriseName || '',
        enterpriseCode: item.enterpriseCode || '',
        creditCode: item.enterpriseCode || '',
        enterpriseType: item.enterpriseType,
        enterpriseTypeName: typeNames[item.enterpriseType] || '未知类型',
        legalPerson: item.legalPerson || '',
        contactPerson: item.contactPerson || item.legalPerson || '',
        contactPhone: item.contactPhone || '',
        contactEmail: item.contactEmail || '',
        province: item.province || '',
        city: item.city || '',
        district: item.district || '',
        address: item.address || '',
        auditStatus: item.auditStatus,
        applyTime: item.createTime || '',
        chainCount: item.chainCount || 0,
        batchCount: item.batchCount || 0,
        productCount: item.productCount || 0,
        lastChainTime: item.lastChainTime || null,
        businessLicense: this.resolveImageUrl(item.businessLicense),
        productionLicense: this.resolveImageUrl(item.productionLicense),
        otherCertificates: item.otherCertificates || [],
        introduction: item.introduction || '',
        rejectReason: item.rejectReason || item.auditRemark || '',
        avatar: item.avatar || null,
        logo: item.logo || null
      }))
      const newList = page === 1 ? list : [...this.data.enterpriseList, ...list]
      this.setData({
        enterpriseList: newList,
        loading: false,
        noMore: newList.length >= total
      })
      // 代理http图片为本地临时文件，解决真机<image>无法加载http图片
      newList.forEach((item, idx) => {
        if (item.businessLicense) {
          proxyImage(item.businessLicense).then(lp => {
            if (lp !== item.businessLicense) this.setData({ ['enterpriseList[' + idx + '].businessLicense']: lp })
          })
        }
        if (item.productionLicense) {
          proxyImage(item.productionLicense).then(lp => {
            if (lp !== item.productionLicense) this.setData({ ['enterpriseList[' + idx + '].productionLicense']: lp })
          })
        }
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
   * 切换类型筛选
   */
  switchType(e) {
    const type = e.currentTarget.dataset.type
    if (type === this.data.currentType) return
    
    this.setData({ currentType: type })
    this.refreshList()
  },

  /**
   * 加载更多
   */
  loadMore() {
    if (!this.data.loading && !this.data.noMore) {
      this.setData({ page: this.data.page + 1 })
      this.loadEnterpriseList()
    }
  },

  /**
   * 查看营业执照
   */
  viewLicense(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showLicenseModal: true,
      currentEnterprise: item
    })
  },

  /**
   * 关闭资质弹窗
   */
  closeLicenseModal() {
    this.setData({ showLicenseModal: false })
  },

  /**
   * 预览资质图片
   */
  previewLicense(e) {
    const type = e.currentTarget.dataset.type
    const enterprise = this.data.currentEnterprise
    const urls = []
    
    if (enterprise.businessLicense) urls.push(enterprise.businessLicense)
    if (enterprise.productionLicense) urls.push(enterprise.productionLicense)
    
    const current = type === 'business' ? enterprise.businessLicense : enterprise.productionLicense
    
    wx.previewImage({
      current,
      urls
    })
  },

  /**
   * 一键拨号
   */
  callLegalPerson(e) {
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
   * 查看上链记录
   */
  viewBatches(e) {
    const item = e.currentTarget.dataset.item
    const enterpriseName = encodeURIComponent(item.enterpriseName || '')
    wx.navigateTo({
      url: `/pages/batch/batch?enterpriseId=${item.id}&enterpriseName=${enterpriseName}`
    })
  },

  /**
   * 跳转到企业详情/审核页面
   */
  goToDetail(e) {
    const item = e.currentTarget.dataset.item
    // Store enterprise data in globalData for detail page
    app.globalData = app.globalData || {}
    app.globalData.currentEnterpriseForAudit = item
    wx.navigateTo({
      url: `/pages/enterprise/audit?id=${item.id}`
    })
  },

  /**
   * 通过审核
   */
  approveEnterprise(e) {
    const item = e.currentTarget.dataset.item
    
    wx.showModal({
      title: '确认审核',
      content: `确定通过「${item.enterpriseName}」的入驻申请吗？`,
      confirmColor: '#4CAF50',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '处理中...' })
          
          request.post('/api/enterprise/audit/approve', {
            enterpriseId: item.id,
            auditStatus: 1
          }).then(res => {
            wx.hideLoading()
            if (res.code === 200) {
              wx.showToast({ title: '审核已通过', icon: 'success' })
              this.refreshList()
              this.loadStats()
            } else {
              wx.showToast({ title: res.message || '审核失败', icon: 'none' })
            }
          }).catch(() => {
            wx.hideLoading()
            wx.showToast({ title: '审核失败', icon: 'none' })
          })
        }
      }
    })
  },

  /**
   * 拒绝审核
   */
  rejectEnterprise(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showRejectModal: true,
      currentEnterprise: item,
      rejectReason: ''
    })
  },

  /**
   * 关闭拒绝弹窗
   */
  closeRejectModal() {
    this.setData({
      showRejectModal: false,
      rejectReason: ''
    })
  },

  /**
   * 拒绝原因输入
   */
  onRejectReasonInput(e) {
    this.setData({ rejectReason: e.detail.value })
  },

  /**
   * 快捷选择拒绝原因
   */
  selectQuickReason(e) {
    const reason = e.currentTarget.dataset.reason
    this.setData({ rejectReason: reason })
  },

  /**
   * 确认拒绝
   */
  confirmReject() {
    if (!this.data.rejectReason.trim()) {
      wx.showToast({ title: '请填写拒绝原因', icon: 'none' })
      return
    }
    
    wx.showLoading({ title: '处理中...' })
    
    request.post('/api/enterprise/audit/approve', {
      enterpriseId: this.data.currentEnterprise.id,
      auditStatus: 2,
      auditRemark: this.data.rejectReason.trim()
    }).then(res => {
      wx.hideLoading()
      this.closeRejectModal()
      if (res.code === 200) {
        wx.showToast({ title: '已拒绝申请', icon: 'success' })
        this.refreshList()
        this.loadStats()
      } else {
        wx.showToast({ title: res.message || '操作失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '操作失败', icon: 'none' })
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
  },

  /**
   * 阻止触摸移动穿透但允许 scroll-view 滚动
   */
  stopTouchMove() {
    // 空方法，scroll-view 会自行处理滚动
  },

  resolveImageUrl(path) {
    if (!path) return ''
    if (path.startsWith('http')) return path
    const baseUrl = (app && app.globalData ? app.globalData.apiBaseUrl : '')
    return baseUrl + path
  }
})
