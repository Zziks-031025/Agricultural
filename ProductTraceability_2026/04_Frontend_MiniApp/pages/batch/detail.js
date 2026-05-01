const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

// 数据库状态码配置
const DB_STATUS_CONFIG = {
  1: { text: '初始化', color: '#4CAF50' },
  2: { text: '生长中', color: '#8BC34A' },
  3: { text: '已收获', color: '#FF9800' },
  4: { text: '加工中', color: '#2196F3' },
  5: { text: '已检疫', color: '#9C27B0' },
  6: { text: '已入库', color: '#00BCD4' },
  7: { text: '运输中', color: '#3F51B5' },
  8: { text: '已销售', color: '#757575' },
  9: { text: '加工完成', color: '#00897B' }
}

// 记录类型配置
const RECORD_TYPE_CONFIG = {
  feeding: { name: '喂养', icon: '/images/agricultural.png' },
  vaccine: { name: '防疫', icon: '/images/quarantine.png' },
  medication: { name: '用药', icon: '/images/sales-report.png' },
  inspect: { name: '巡查', icon: '/images/batch-list.png' }
}

Page({
  data: {
    batchId: '',
    batchInfo: {},
    recordList: [],
    showQrcodeModal: false,
    qrcodeUrl: '',

    // Identity isolation
    enterpriseType: 0,  // 0-admin/unknown 1-养殖户 2-加工厂 3-检疫机构
    isInspector: false
  },

  onLoad(options) {
    // Load enterprise type from global/storage
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const userType = userInfo.userType || 3
    let enterpriseType = 0
    if (userType === 2) {
      enterpriseType = userInfo.enterpriseType || app.globalData.enterpriseType || 1
    } else if (userType === 1) {
      enterpriseType = 0  // 管理员
    }
    const isInspector = (userType === 2 && enterpriseType === 3)
    this.setData({ enterpriseType, isInspector })

    // 支持 id 和 batchId 两种参数名（兼容不同来源跳转）
    const batchId = options.id || options.batchId || ''
    if (batchId) {
      this.setData({ batchId })
      this.loadBatchDetail()
      this.loadRecordList()
    } else {
      wx.showToast({ title: '批次参数缺失', icon: 'none' })
    }
  },

  onShow() {
    // 从添加记录页返回时刷新列表
    if (this.data.batchId) {
      this.loadRecordList()
    }
  },

  /**
   * 加载批次详情
   */
  loadBatchDetail() {
    wx.showLoading({ title: '加载中...' })
    
    const batchId = this.data.batchId
    // Determine query param: if it looks like a batch code use batchCode, otherwise use id
    const isCode = isNaN(parseInt(batchId)) || batchId.toString().length > 10
    const params = isCode ? { batchCode: batchId } : { id: batchId }

    request.get('/api/batch/detail', params).then(res => {
      wx.hideLoading()
      const data = res.data || {}
      
      // Map backend fields to display model
      const batchInfo = {
        id: data.id || data.batchId,
        batchCode: data.batchCode || '',
        productName: data.productName || '',
        breed: data.breed || '',
        seedSource: data.seedSource || '',
        productType: data.productType || 1,
        enterpriseName: data.enterpriseName || '',
        operator: data.manager || '',
        manager: data.manager || '',
        productionDate: data.productionDate || '',
        initQuantity: data.initQuantity || 0,
        currentQuantity: data.currentQuantity || data.initQuantity || 0,
        unit: data.unit || '',
        status: data.batchStatus || 1,
        txHash: data.txHash || '',
        blockNumber: data.blockNumber || '',
        chainTime: data.chainTime || '',
        originLocation: data.originLocation || '',
        plantArea: data.plantArea || '',
        greenhouseNo: data.greenhouseNo || ''
      }

      // Computed display fields
      batchInfo.quantityDiff = batchInfo.currentQuantity - batchInfo.initQuantity
      const statusConfig = DB_STATUS_CONFIG[batchInfo.status] || { text: '未知', color: '#999' }
      batchInfo.statusText = statusConfig.text
      batchInfo.statusColor = statusConfig.color
      batchInfo.txHashShort = batchInfo.txHash ?
        `${batchInfo.txHash.substring(0, 10)}...${batchInfo.txHash.substring(batchInfo.txHash.length - 8)}` : ''

      this.setData({ batchInfo })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: err.message || '加载失败', icon: 'none' })
    })
  },

  /**
   * 加载生长记录列表
   */
  loadRecordList() {
    const batchId = this.data.batchId
    
    request.get('/api/trace/detail', { batchId: batchId }).then(res => {
      if (!res.data || !res.data.timeline) {
        this.setData({ recordList: [] })
        return
      }
      
      const timeline = res.data.timeline || []
      const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
      
      const formattedRecords = timeline.map(node => {
        let rawImages = node.images || []
        const fullImages = rawImages.map(img => {
          if (!img) return ''
          if (img.startsWith('http')) return img
          return apiBase + img
        })
        
        return {
          id: node.id || '',
          typeName: node.title || '',
          typeIcon: this.getStageIcon(node.stage),
          recordType: node.stage || '',
          recordTime: node.time || '',
          description: this.formatNodeDetails(node.details),
          operator: node.operator || '',
          images: fullImages,
          txHash: node.txHash || ''
        }
      })
      
      this._proxyAndSetRecords(formattedRecords)
    }).catch(() => {
      this.setData({ recordList: [] })
    })
  },

  getStageIcon(stage) {
    const iconMap = {
      'batch_init': '/images/batch-list.png',
      'growth': '/images/batch-list.png',
      'inspection': '/images/batch-list.png',
      'processing': '/images/batch-list.png',
      'storage': '/images/batch-list.png',
      'transport': '/images/batch-list.png',
      'sale': '/images/batch-list.png'
    }
    return iconMap[stage] || '/images/batch-list.png'
  },

  formatNodeDetails(details) {
    if (!details) return ''
    const items = []
    for (const key in details) {
      if (details[key] !== null && details[key] !== '') {
        items.push(`${key}: ${details[key]}`)
      }
    }
    return items.join(', ')
  },

  /**
   * 代理下载所有记录的图片后再统一setData（与Banner页面一致）
   * 确保真机上http局域网图片能正常显示
   */
  _proxyAndSetRecords(records) {
    // 收集所有需要代理的图片
    const proxyTasks = records.map(record => {
      if (!record.images || record.images.length === 0) {
        return Promise.resolve([])
      }
      return util.proxyImages(record.images)
    })

    Promise.all(proxyTasks).then(allLocalImages => {
      const finalRecords = records.map((record, idx) => ({
        ...record,
        images: allLocalImages[idx].length > 0 ? allLocalImages[idx] : record.images
      }))
      this.setData({ recordList: finalRecords })
    }).catch(() => {
      // 代理失败时仍使用原始URL渲染
      this.setData({ recordList: records })
    })
  },

  /**
   * 复制哈希值
   */
  copyHash(e) {
    const hash = e.currentTarget.dataset.hash
    if (!hash) return
    
    wx.setClipboardData({
      data: hash,
      success: () => {
        wx.showToast({ title: '哈希已复制', icon: 'success' })
      }
    })
  },

  /**
   * 预览图片
   */
  previewImage(e) {
    const { urls, current } = e.currentTarget.dataset
    wx.previewImage({
      current: current,
      urls: urls
    })
  },

  /**
   * 显示二维码弹窗
   */
  showQrcode() {
    const qrcodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${encodeURIComponent('https://trace.example.com/batch/' + this.data.batchInfo.batchCode)}`
    
    this.setData({
      showQrcodeModal: true,
      qrcodeUrl: qrcodeUrl
    })
  },

  /**
   * 隐藏二维码弹窗
   */
  hideQrcode() {
    this.setData({ showQrcodeModal: false })
  },

  /**
   * 保存二维码到相册
   */
  saveQrcode() {
    wx.showLoading({ title: '保存中...' })
    
    wx.downloadFile({
      url: this.data.qrcodeUrl,
      success: (res) => {
        if (res.statusCode === 200) {
          wx.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: () => {
              wx.hideLoading()
              wx.showToast({ title: '已保存到相册', icon: 'success' })
            },
            fail: (err) => {
              wx.hideLoading()
              if (err.errMsg.includes('auth deny')) {
                wx.showModal({
                  title: '提示',
                  content: '请授权保存图片到相册',
                  confirmText: '去设置',
                  success: (res) => {
                    if (res.confirm) wx.openSetting()
                  }
                })
              } else {
                wx.showToast({ title: '保存失败', icon: 'none' })
              }
            }
          })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      }
    })
  },

  /**
   * 阻止冒泡
   */
  stopPropagation() {},

  /**
   * 添加生长记录
   */
  addRecord() {
    const { batchId, batchInfo } = this.data
    const productType = batchInfo.productType || 1
    
    wx.navigateTo({
      url: `/pages/batch/add-record?batchId=${batchId}&productType=${productType}`
    })
  },

  /**
   * 发起检疫申报 (Type 1)
   */
  applyInspection() {
    const batchCode = this.data.batchInfo.batchCode || ''
    wx.navigateTo({
      url: `/pages/collect/inspection?batchNo=${batchCode}&batchId=${this.data.batchId}`
    })
  },

  /**
   * 加工录入 (Type 2)
   */
  goProcessing() {
    const batchCode = this.data.batchInfo.batchCode || ''
    wx.navigateTo({
      url: `/pages/collect/processing?batchCode=${batchCode}`
    })
  },

  /**
   * 销售出库 (Type 2)
   */
  goSale() {
    const batchCode = this.data.batchInfo.batchCode || ''
    wx.navigateTo({
      url: `/pages/collect/sale?batchNo=${batchCode}`
    })
  },

  /**
   * 查看最终溯源报告
   */
  viewTraceReport() {
    wx.navigateTo({
      url: '/pages/trace/trace?batchId=' + this.data.batchId
    })
  },

  /**
   * 检疫录入 (Type 3)
   */
  goQuarantineForm() {
    const batchCode = this.data.batchInfo.batchCode || ''
    const batchId = this.data.batchId || ''
    wx.navigateTo({
      url: `/pages/quarantine/result-form?batchCode=${batchCode}&batchId=${batchId}`
    })
  },

  /**
   * 报告上传 (Type 3)
   */
  goReportUpload() {
    const batchCode = this.data.batchInfo.batchCode || ''
    wx.navigateTo({
      url: `/pages/quarantine/report-upload?batchCode=${batchCode}`
    })
  }
})
