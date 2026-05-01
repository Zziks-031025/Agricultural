const request = require('../../utils/request.js')

Page({
  data: {
    currentType: 'all',
    enterpriseList: [],
    filteredList: [],
    loading: false,
    noMore: false
  },

  onLoad() {
    this.loadEnterpriseList()
  },

  onPullDownRefresh() {
    this.loadEnterpriseList().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  loadEnterpriseList() {
    this.setData({ loading: true })

    const typeNames = { 1: '种植养殖企业', 2: '加工宰杀企业', 3: '检疫质检企业' }

    return request.get('/api/enterprise/list', { current: 1, size: 100, auditStatus: 1 }).then(res => {
      const records = (res.data && res.data.records) || []
      const list = records.map(item => this.normalizeEnterpriseRecord(item, typeNames))
      return this.hydrateMissingMetrics(list).then(finalList => {
        this.setData({
          enterpriseList: finalList,
          filteredList: finalList,
          loading: false,
          noMore: true
        })
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  normalizeEnterpriseRecord(item, typeNames) {
    return {
      id: item.id,
      enterpriseName: item.enterpriseName || '',
      enterpriseType: item.enterpriseType,
      enterpriseTypeName: item.enterpriseTypeName || typeNames[item.enterpriseType] || '未知类型',
      creditCode: item.creditCode || item.enterpriseCode || '',
      legalPerson: item.legalPerson || '',
      contactPhone: item.contactPhone || '',
      province: item.province || '',
      city: item.city || '',
      district: item.district || '',
      address: item.address || '',
      logo: item.logo || '',
      chainCount: this.toCount(item.chainCount),
      batchCount: this.toCount(item.batchCount),
      verifyCount: this.toCount(item.verifyCount),
      productCount: this.toCount(item.productCount),
      lastChainTime: item.lastChainTime || '',
      businessLicense: item.businessLicense || '',
      productionLicense: item.productionLicense || '',
      coverImage: item.coverImage || '',
      introduction: item.introduction || '',
      _needsMetrics: !this.hasOwnField(item, 'chainCount')
    }
  },

  hasOwnField(item, field) {
    return !!item && Object.prototype.hasOwnProperty.call(item, field)
  },

  toCount(value) {
    const count = Number(value)
    return Number.isFinite(count) ? count : 0
  },

  hydrateMissingMetrics(list) {
    const pendingItems = (list || []).filter(item => item._needsMetrics && item.id)
    if (pendingItems.length === 0) {
      return Promise.resolve(list)
    }

    return Promise.all(
      pendingItems.map(item =>
        request.get(`/api/enterprise/metrics/${item.id}`)
          .then(res => ({ id: item.id, data: res.data || {} }))
          .catch(() => ({ id: item.id, data: null }))
      )
    ).then(results => {
      const metricsMap = {}
      results.forEach(result => {
        if (result.data) {
          metricsMap[result.id] = result.data
        }
      })

      return list.map(item => {
        const metrics = metricsMap[item.id]
        if (!metrics) {
          return { ...item, _needsMetrics: false }
        }
        return {
          ...item,
          chainCount: this.toCount(metrics.chainCount),
          batchCount: this.toCount(metrics.batchCount),
          verifyCount: this.toCount(metrics.verifyCount),
          productCount: this.toCount(metrics.productCount),
          lastChainTime: metrics.lastChainTime || item.lastChainTime || '',
          _needsMetrics: false
        }
      })
    })
  },

  switchType(e) {
    const type = e.currentTarget.dataset.type
    if (type === this.data.currentType) return

    this.setData({ currentType: type })
    this.filterList()
  },

  filterList() {
    const { enterpriseList, currentType } = this.data
    
    if (currentType === 'all') {
      this.setData({ filteredList: enterpriseList })
    } else {
      const filtered = enterpriseList.filter(item => item.enterpriseType === parseInt(currentType))
      this.setData({ filteredList: filtered })
    }
  },

  goToDetail(e) {
    const item = e.currentTarget.dataset.item
    // Store enterprise data in app.globalData for detail page
    const app = getApp()
    app.globalData = app.globalData || {}
    app.globalData.currentEnterprise = item
    
    wx.navigateTo({
      url: `/pages/index/enterprise-detail?id=${item.id}`
    })
  },

  loadMore() {
  }
})
