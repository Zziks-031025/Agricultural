const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

// 记录类型配置
const RECORD_TYPE_CONFIG = {
  feeding: { name: '喂养记录', certTitle: '喂养作业存证' },
  vaccine: { name: '防疫记录', certTitle: '防疫接种存证' },
  inspect: { name: '环境巡查', certTitle: '巡查记录存证' },
  medication: { name: '用药记录', certTitle: '用药作业存证' },
  fertilize: { name: '施肥记录', certTitle: '施肥作业存证' },
  irrigate: { name: '灌溉记录', certTitle: '灌溉作业存证' },
  pesticide: { name: '除虫记录', certTitle: '除虫作业存证' },
  init: { name: '批次初始化', certTitle: '批次创建存证' },
  quarantine: { name: '检疫申报', certTitle: '检疫申报存证' },
  storage: { name: '仓储入库', certTitle: '仓储入库存证' },
  transport: { name: '物流运输', certTitle: '物流运输存证' }
}

Page({
  data: {
    certificateData: {
      productName: '',
      batchNo: '',
      enterpriseName: '',
      recordType: '',
      recordTypeName: '',
      certTitle: '区块链存证证书',
      txHash: '',
      blockHeight: '',
      contractAddress: '',
      timestamp: '',
      certTime: ''
    },
    showCard: false
  },

  onLoad(options) {
    const { txHash, batchId, recordId, recordType, batchNo } = options
    
    // 保存传入的参数
    this.options = options
    
    if (txHash) {
      this.loadCertificateData(txHash, recordType)
    } else if (batchNo) {
      this.loadCertificateByBatch(batchNo)
    } else {
      wx.showToast({ title: '缺少存证参数', icon: 'none' })
    }
    
    setTimeout(() => {
      this.setData({ showCard: true })
    }, 100)
  },

  loadCertificateData(txHash, recordType) {
    wx.showLoading({ title: '加载存证数据...' })
    
    const typeConfig = RECORD_TYPE_CONFIG[recordType] || { name: '操作记录', certTitle: '区块链存证证书' }
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseName = userInfo.enterpriseName || ''

    request.get('/api/blockchain/logs', {
      txHash: txHash,
      pageNum: 1,
      pageSize: 1
    }).then(res => {
      wx.hideLoading()
      const records = (res.data && res.data.records) || []
      const log = records.length > 0 ? records[0] : null
      this.setData({
        certificateData: {
          productName: (log && log.productName) || '',
          batchNo: (log && log.batchCode) || (this.options.batchId ? `BATCH${String(this.options.batchId).padStart(8, '0')}` : ''),
          enterpriseName: (log && log.enterpriseName) || enterpriseName,
          recordType: recordType,
          recordTypeName: typeConfig.name,
          certTitle: typeConfig.certTitle,
          txHash: txHash,
          blockHeight: (log && log.blockNumber) ? String(log.blockNumber) : '--',
          contractAddress: (log && log.contractAddress) || '',
          timestamp: (log && log.chainTime) ? Math.floor(new Date(log.chainTime).getTime() / 1000) : Math.floor(Date.now() / 1000),
          certTime: (log && log.chainTime) || util.formatTime(new Date())
        }
      })
    }).catch(() => {
      wx.hideLoading()
      this.setData({
        certificateData: {
          productName: '',
          batchNo: this.options.batchId || '',
          enterpriseName: enterpriseName,
          recordType: recordType,
          recordTypeName: typeConfig.name,
          certTitle: typeConfig.certTitle,
          txHash: txHash,
          blockHeight: '--',
          contractAddress: '',
          timestamp: Math.floor(Date.now() / 1000),
          certTime: util.formatTime(new Date())
        }
      })
    })
  },

  loadCertificateByBatch(batchNo) {
    wx.showLoading({ title: '加载存证数据...' })
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    
    request.get('/api/blockchain/logs', {
      batchCode: batchNo,
      pageNum: 1,
      pageSize: 1
    }).then(res => {
      wx.hideLoading()
      const records = (res.data && res.data.records) || []
      const log = records.length > 0 ? records[0] : null
      this.setData({
        certificateData: {
          productName: (log && log.productName) || '',
          batchNo: batchNo,
          enterpriseName: (log && log.enterpriseName) || userInfo.enterpriseName || '',
          txHash: (log && log.txHash) || '',
          blockHeight: (log && log.blockNumber) ? String(log.blockNumber) : '--',
          contractAddress: (log && log.contractAddress) || '',
          timestamp: (log && log.chainTime) ? Math.floor(new Date(log.chainTime).getTime() / 1000) : Math.floor(Date.now() / 1000),
          certTime: (log && log.chainTime) || util.formatTime(new Date())
        }
      })
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  copyText(e) {
    const text = e.currentTarget.dataset.text
    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({
          title: '已复制到剪贴板',
          icon: 'success'
        })
      }
    })
  },

  saveCertificate() {
    wx.showModal({
      title: '保存证书',
      content: '请长按证书区域截图保存，或使用系统截图功能保存本证书',
      showCancel: false,
      confirmText: '知道了'
    })
  },

  onShareAppMessage() {
    return {
      title: '区块链存证数字证书',
      path: `/pages/blockchain/certificate?txHash=${this.data.certificateData.txHash}`
    }
  }
})
