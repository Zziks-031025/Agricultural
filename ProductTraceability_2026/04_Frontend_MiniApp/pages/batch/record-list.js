/**
 * 生长记录列表页
 * 以时间轴形式展示批次下的所有生长记录
 */
const app = getApp()
const request = require('../../utils/request.js')
const { proxyImages } = require('../../utils/util.js')

// 记录类型配置
const recordTypeConfig = {
  // 肉鸡养殖
  feeding: { name: '喂养', label: '饲料', unit: 'kg', class: 'feeding' },
  vaccine: { name: '防疫', label: '疫苗', unit: 'ml', class: 'vaccine' },
  inspect: { name: '环境巡查', label: '', unit: '', class: 'inspect' },
  medication: { name: '用药', label: '药品', unit: 'ml', class: 'medication' },
  // 西红柿种植
  fertilize: { name: '施肥', label: '肥料', unit: 'kg', class: 'fertilize' },
  irrigate: { name: '灌溉', label: '', unit: 'L', class: 'irrigate' },
  pesticide: { name: '除虫', label: '农药', unit: 'ml', class: 'pesticide' }
}

Page({
  data: {
    batchId: '',
    batchInfo: {},
    enterpriseInfo: {},
    recordList: [],
    loading: true
  },

  onLoad(options) {
    const { batchId } = options
    
    if (!batchId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    
    // 获取当前登录用户的企业信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseInfo = {
      id: userInfo.enterpriseId,
      name: userInfo.enterpriseName,
      type: userInfo.enterpriseType
    }
    
    this.setData({ 
      batchId,
      enterpriseInfo
    })
    this.loadBatchInfo()
    this.loadRecordList()
  },

  onShow() {
    // 从添加记录页返回时刷新列表
    if (this.data.batchId && !this.data.loading) {
      this.loadRecordList()
    }
  },

  /**
   * 加载批次信息
   */
  loadBatchInfo() {
    const { batchId } = this.data
    request.get('/api/batch/detail', { id: batchId }).then(res => {
      if (res.code === 200 && res.data) {
        const d = res.data
        this.setData({
          batchInfo: {
            batchCode: d.batchCode || '--',
            productName: d.productName || '--',
            productType: d.productType || 1,
            enterpriseName: d.enterpriseName || '--',
            enterpriseType: d.enterpriseType || 1
          }
        })
      }
    }).catch(() => {})
  },

  /**
   * 加载记录列表
   */
  loadRecordList() {
    this.setData({ loading: true })
    request.get('/api/record/list', {
      batchId: this.data.batchId,
      pageNum: 1,
      pageSize: 100
    }).then(res => {
      if (res.code === 200 && res.data) {
        const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
        const records = (res.data.records || []).map(item => {
          const config = recordTypeConfig[item.recordType] || { name: item.recordType, label: '', unit: '', class: '' }
          const txHash = item.txHash || ''
          let rawImages = []
          try { rawImages = item.images ? (typeof item.images === 'string' ? JSON.parse(item.images) : item.images) : [] } catch (e) { rawImages = [] }
          const fullImages = rawImages.map(img => {
            if (!img) return ''
            if (img.startsWith('http')) return img
            return apiBase + img
          })
          return {
            id: item.id,
            recordType: item.recordType || '',
            recordTypeName: config.name,
            recordTypeClass: config.class,
            materialLabel: config.label,
            materialName: item.materialName || '',
            dosage: item.amount ? String(item.amount) : '',
            dosageUnit: config.unit,
            description: item.description || item.remark || '',
            operator: item.operator || '--',
            recordTime: item.recordDate || item.createTime || '--',
            images: fullImages,
            location: item.latitude ? { latitude: item.latitude, longitude: item.longitude } : null,
            hasChain: !!txHash,
            txHash: txHash,
            txHashShort: txHash ? `${txHash.substr(0, 10)}...${txHash.substr(-6)}` : '',
            blockNumber: item.blockNumber || null
          }
        })
        // 代理下载图片后再setData（与Banner页面一致，确保开发者工具和真机都能显示）
        this._proxyAndSetRecords(records)
      } else {
        this.setData({ loading: false })
      }
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /**
   * 跳转添加记录
   */
  goAddRecord() {
    const { batchId, batchInfo } = this.data
    const productType = batchInfo.productType || 1
    
    wx.navigateTo({
      url: `/pages/batch/add-record?batchId=${batchId}&productType=${productType}`
    })
  },

  /**
   * 查看记录详情
   */
  viewRecordDetail(e) {
    const { item } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/batch/record-detail?recordId=${item.id}&batchId=${this.data.batchId}`
    })
  },

  /**
   * 查看存证证书
   */
  viewCertificate(e) {
    const { item } = e.currentTarget.dataset
    if (item.txHash) {
      wx.navigateTo({
        url: `/pages/blockchain/certificate?txHash=${item.txHash}&batchId=${this.data.batchId}&recordId=${item.id}`
      })
    }
  },

  /**
   * 预览图片
   */
  previewImage(e) {
    const { urls, current } = e.currentTarget.dataset
    wx.previewImage({
      current,
      urls
    })
  },


  /**
   * 代理下载所有记录的图片后再统一setData
   */
  _proxyAndSetRecords(records) {
    const proxyTasks = records.map(record => {
      if (!record.images || record.images.length === 0) {
        return Promise.resolve([])
      }
      return proxyImages(record.images)
    })

    Promise.all(proxyTasks).then(allLocalImages => {
      const finalRecords = records.map((record, idx) => ({
        ...record,
        images: allLocalImages[idx].length > 0 ? allLocalImages[idx] : record.images
      }))
      this.setData({ recordList: finalRecords, loading: false })
    }).catch(() => {
      this.setData({ recordList: records, loading: false })
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadRecordList()
    setTimeout(() => {
      wx.stopPullDownRefresh()
    }, 1000)
  }
})
