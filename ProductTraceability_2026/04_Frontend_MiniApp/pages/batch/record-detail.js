/**
 * 记录详情页
 * 展示单条生长记录的全部信息和区块链验证
 */
const app = getApp()
const request = require('../../utils/request.js')
const { proxyImages } = require('../../utils/util.js')

// 记录类型配置
const recordTypeConfig = {
  feeding: { name: '喂养', label: '饲料', unit: 'kg', class: 'feeding' },
  vaccine: { name: '防疫', label: '疫苗', unit: 'ml', class: 'vaccine' },
  inspect: { name: '环境巡查', label: '', unit: '', class: 'inspect' },
  fertilize: { name: '施肥', label: '肥料', unit: 'kg', class: 'fertilize' },
  irrigate: { name: '灌溉', label: '', unit: 'L', class: 'irrigate' },
  pesticide: { name: '除虫', label: '农药', unit: 'ml', class: 'pesticide' }
}

Page({
  data: {
    recordId: '',
    batchId: '',
    recordInfo: {},
    markers: [],
    
    // 验真状态
    verifying: false,
    verifyStatus: '', // success, fail
    verifyBtnText: '一键验真',
    verifyResult: null
  },

  onLoad(options) {
    const { recordId, batchId } = options
    
    if (!recordId || !batchId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    
    this.setData({ recordId, batchId })
    this.loadRecordDetail()
  },

  /**
   * 加载记录详情
   */
  loadRecordDetail() {
    wx.showLoading({ title: '加载中...' })
    
    const { recordId, batchId } = this.data
    
    request.get('/api/record/list', {
      batchId: batchId,
      pageNum: 1,
      pageSize: 100
    }).then(res => {
      wx.hideLoading()
      if (res.code === 200 && res.data) {
        const allRecords = res.data.records || []
        const item = allRecords.find(r => String(r.id) === String(recordId))
        if (!item) {
          wx.showToast({ title: '记录不存在', icon: 'none' })
          return
        }
        const config = recordTypeConfig[item.recordType] || { name: item.recordType || '--', label: '', unit: '', class: '' }
        const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
        let rawImages = []
        try { rawImages = item.images ? (typeof item.images === 'string' ? JSON.parse(item.images) : item.images) : [] } catch (e) { rawImages = [] }
        const fullImages = rawImages.map(img => {
          if (!img) return ''
          if (img.startsWith('http')) return img
          return apiBase + img
        })
        const recordInfo = {
          id: item.id,
          batchId: batchId,
          batchCode: item.batchCode || '--',
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
          location: item.latitude ? {
            latitude: item.latitude,
            longitude: item.longitude,
            address: item.address || ''
          } : null,
          txHash: item.txHash || '',
          blockNumber: item.blockNumber || null,
          dataHash: item.dataHash || '',
          chainTime: item.chainTime || '',
          gasUsed: item.gasUsed || ''
        }
        
        const markers = recordInfo.location ? [{
          id: 1,
          latitude: recordInfo.location.latitude,
          longitude: recordInfo.location.longitude,
          iconPath: '/images/position.png',
          width: 32,
          height: 32,
          callout: {
            content: '作业位置',
            display: 'ALWAYS',
            padding: 8,
            borderRadius: 4,
            fontSize: 12,
            color: '#333333',
            bgColor: '#FFFFFF'
          }
        }] : []
        
        // 代理下载图片后再setData（与Banner页面一致）
        if (fullImages.length > 0) {
          proxyImages(fullImages).then(localImages => {
            recordInfo.images = localImages
            this.setData({ recordInfo, markers })
          }).catch(() => {
            this.setData({ recordInfo, markers })
          })
        } else {
          this.setData({ recordInfo, markers })
        }
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  /**
   * 一键验真
   */
  verifyRecord() {
    if (this.data.verifying) return
    
    this.setData({
      verifying: true,
      verifyBtnText: '验证中...',
      verifyResult: null,
      verifyStatus: ''
    })
    
      const { recordInfo } = this.data
    if (!recordInfo.txHash) {
      this.setData({
        verifying: false,
        verifyStatus: 'fail',
        verifyBtnText: '验证失败',
        verifyResult: { title: '无法验证', desc: '该记录尚未上链，无法进行验真' }
      })
      return
    }

    request.post('/api/blockchain/verify', {
      batchId: String(this.data.batchId)
    }).then(res => {
      if (res.code === 200 && res.data) {
        const isValid = res.data.valid === true
        if (isValid) {
          this.setData({
            verifying: false,
            verifyStatus: 'success',
            verifyBtnText: '验证通过',
            verifyResult: {
              title: '数据验证通过',
              desc: '链上哈希与本地数据摘要完全一致，数据未被篡改'
            }
          })
        } else {
          this.setData({
            verifying: false,
            verifyStatus: 'fail',
            verifyBtnText: '验证失败',
            verifyResult: {
              title: '数据验证失败',
              desc: res.data.message || '链上哈希与本地数据摘要不一致，数据可能已被篡改'
            }
          })
        }
      }
    }).catch(() => {
      this.setData({
        verifying: false,
        verifyStatus: 'fail',
        verifyBtnText: '验证失败',
        verifyResult: { title: '网络错误', desc: '无法连接验证服务，请稍后重试' }
      })
    })
  },

  /**
   * 复制哈希值
   */
  copyHash(e) {
    const { value } = e.currentTarget.dataset
    wx.setClipboardData({
      data: value,
      success: () => {
        wx.showToast({ title: '已复制', icon: 'success' })
      }
    })
  },

  /**
   * 预览图片
   */
  previewImage(e) {
    const { url } = e.currentTarget.dataset
    wx.previewImage({
      current: url,
      urls: this.data.recordInfo.images
    })
  },

  /**
   * 查看存证证书
   */
  viewCertificate() {
    const { recordInfo, batchId } = this.data
    if (recordInfo.txHash) {
      wx.navigateTo({
        url: `/pages/blockchain/certificate?txHash=${recordInfo.txHash}&batchId=${batchId}&recordId=${recordInfo.id}`
      })
    }
  },

})
