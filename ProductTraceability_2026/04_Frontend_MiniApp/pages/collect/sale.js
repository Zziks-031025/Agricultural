const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')
const submitHelper = require('../../utils/submit-helper.js')
const validator = require('../../utils/validator.js')

Page({
  data: {
    activeTab: 1,
    batchNo: '',
    batchInfo: null,
    formData: {
      buyerName: '',
      saleDate: '',
      saleTime: '',
      unitPrice: '',
      quantity: '',
      saleUnit: '',
      totalAmount: '',
      saleChannel: '',
      destination: ''
    },
    invoiceImages: [],
    maxImages: 3,
    saleChannels: ['批发市场', '超市', '深加工'],
    saleChannelIndex: -1
  },

  onLoad(options) {
    const today = util.formatTime(new Date()).split(' ')[0]
    const now = util.formatTime(new Date()).split(' ')[1].substring(0, 5)
    
    this.setData({
      'formData.saleDate': today,
      'formData.saleTime': now
    })

    if (options.batchNo) {
      this.setData({
        batchNo: options.batchNo
      })
      this.loadBatchInfo(options.batchNo)
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({
      activeTab: tab
    })
  },

  scanBatchCode() {
    wx.scanCode({
      success: (res) => {
        const raw = res.result || ''
        let batchNo = raw
        const pathMatch = raw.match(/\/batch\/([A-Za-z0-9]+)/)
        if (pathMatch) batchNo = pathMatch[1]
        else {
          const paramMatch = raw.match(/[?&](?:batchCode|batchId)=([^&#]+)/)
          if (paramMatch) batchNo = decodeURIComponent(paramMatch[1])
          else {
            const batchMatch = raw.match(/(BATCH[A-Za-z0-9]+)/i)
            if (batchMatch) batchNo = batchMatch[1].toUpperCase()
          }
        }
        this.setData({ batchNo: batchNo })
        this.loadBatchInfo(batchNo)
      },
      fail: (err) => {
        console.error('扫码失败:', err)
        wx.showToast({ title: '扫码失败', icon: 'none' })
      }
    })
  },

  onBatchNoInput(e) {
    this.setData({
      batchNo: e.detail.value
    })
  },

  confirmBatchNo() {
    const { batchNo } = this.data
    if (!batchNo) {
      wx.showToast({
        title: '请输入批次号',
        icon: 'none'
      })
      return
    }
    this.loadBatchInfo(batchNo)
  },

  loadBatchInfo(batchNo) {
    wx.showLoading({ title: '加载中...' })
    
    request.get('/api/batch/detail', { batchCode: batchNo }).then(res => {
      wx.hideLoading()
      const data = res.data || {}
      const status = data.batchStatus || data.status || 0

      // 销售：已检疫(5)、已入库(6)或运输中(7)均可销售，销售与运输不分先后
      if (status !== 5 && status !== 6 && status !== 7) {
        wx.showModal({
          title: '无法操作',
          content: '该批次当前状态不允许销售录入，需已检疫或已入库。',
          showCancel: false
        })
        this.setData({ batchNo: '', batchInfo: null })
        return
      }

      const qty = data.currentQuantity || data.initQuantity || ''
      const unit = data.unit || ''
      this.setData({
        batchInfo: {
          batchId: data.batchId || data.id,
          batchNo: data.batchCode,
          productName: data.productName,
          currentQuantity: qty,
          unit: unit,
          enterpriseName: data.enterpriseName
        },
        'formData.quantity': String(qty),
        'formData.saleUnit': unit
      })
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载批次信息失败', icon: 'none' })
      this.setData({ batchInfo: null })
    })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    
    this.setData({
      [`formData.${field}`]: value
    })

    if (field === 'unitPrice' || field === 'quantity') {
      this.calculateTotal()
    }
  },

  onDateChange(e) {
    this.setData({
      'formData.saleDate': e.detail.value
    })
  },

  onTimeChange(e) {
    this.setData({
      'formData.saleTime': e.detail.value
    })
  },

  onChannelChange(e) {
    const idx = e.detail.value
    this.setData({
      saleChannelIndex: idx,
      'formData.saleChannel': this.data.saleChannels[idx]
    })
  },

  calculateTotal() {
    const { unitPrice, quantity } = this.data.formData
    if (unitPrice && quantity) {
      const total = (parseFloat(unitPrice) * parseFloat(quantity)).toFixed(2)
      this.setData({
        'formData.totalAmount': total
      })
    }
  },

  chooseInvoiceImage() {
    const { invoiceImages, maxImages } = this.data
    const remaining = maxImages - invoiceImages.length

    if (remaining <= 0) {
      wx.showToast({
        title: `最多上传${maxImages}张照片`,
        icon: 'none'
      })
      return
    }

    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['camera', 'album'],
      success: (res) => {
        const tempFiles = res.tempFiles.map(file => file.tempFilePath)
        wx.showLoading({ title: '上传中...', mask: true })
        submitHelper.uploadImages(tempFiles, 'sale').then(urls => {
          wx.hideLoading()
          if (!this._serverInvoiceImages) this._serverInvoiceImages = []
          this._serverInvoiceImages = [...this._serverInvoiceImages, ...urls]
          const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
          const fullUrls = urls.map(u => u.startsWith('http') ? u : apiBase + u)
          const newList = [...invoiceImages, ...fullUrls]
          this.setData({ invoiceImages: newList })
          util.proxyImages(newList).then(localUrls => {
            this.setData({ invoiceImages: localUrls })
          })
          wx.showToast({ title: '上传成功', icon: 'success' })
        }).catch(() => {
          wx.hideLoading()
          wx.showToast({ title: '上传失败', icon: 'none' })
        })
      }
    })
  },

  previewImage(e) {
    const index = e.currentTarget.dataset.index
    wx.previewImage({
      current: this.data.invoiceImages[index],
      urls: this.data.invoiceImages
    })
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.invoiceImages
    images.splice(index, 1)
    this.setData({ invoiceImages: images })
    if (this._serverInvoiceImages && this._serverInvoiceImages.length > index) {
      this._serverInvoiceImages.splice(index, 1)
    }
  },

  validateForm() {
    const { batchNo, batchInfo, formData, invoiceImages } = this.data
    
    if (!batchNo || !batchInfo) {
      wx.showToast({ title: '请先关联批次', icon: 'none' })
      return false
    }
    
    if (!formData.buyerName) {
      wx.showToast({ title: '请输入销售对象', icon: 'none' })
      return false
    }
    
    if (!formData.saleDate) {
      wx.showToast({ title: '请选择销售日期', icon: 'none' })
      return false
    }
    
    if (!formData.saleTime) {
      wx.showToast({ title: '请选择销售时间', icon: 'none' })
      return false
    }
    
    if (!formData.unitPrice) {
      wx.showToast({ title: '请输入销售单价', icon: 'none' })
      return false
    }
    
    if (!validator.isPositiveNumber(formData.unitPrice)) {
      validator.showValidationError('销售单价必须为正数')
      return false
    }
    
    if (!formData.quantity) {
      wx.showToast({ title: '请输入销售数量', icon: 'none' })
      return false
    }
    
    if (!validator.isPositiveNumber(formData.quantity)) {
      validator.showValidationError('销售数量必须为正数')
      return false
    }
    
    if (invoiceImages.length === 0) {
      wx.showToast({ title: '请上传销售凭证', icon: 'none' })
      return false
    }
    
    return true
  },

  submitForm() {
    if (!this.validateForm()) {
      return
    }

    const batchId = this.data.batchInfo && this.data.batchInfo.batchId
    const enterpriseId = submitHelper.getEnterpriseId()

    if (!batchId) {
      this._confirmAndSubmit()
      return
    }

    // Check if this batch already has a sale record
    wx.showLoading({ title: '检查中...', mask: true })
    request.get('/api/sale/list', {
      batchId: batchId,
      enterpriseId: enterpriseId || undefined
    }).then(res => {
      wx.hideLoading()
      const records = (res.data && res.data.records) || []
      if (records.length > 0) {
        this._existingRecordId = records[0].id
        wx.showModal({
          title: '记录已存在',
          content: '该批次已有销售记录，是否更新数据？',
          confirmText: '确认',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.createSaleRecord(true)
            }
          }
        })
      } else {
        this._confirmAndSubmit()
      }
    }).catch(() => {
      wx.hideLoading()
      this._confirmAndSubmit()
    })
  },

  _confirmAndSubmit() {
    wx.showModal({
      title: '确认提交',
      content: '确认提交销售记录？提交后数据将上链存证。',
      success: (res) => {
        if (res.confirm) {
          this.createSaleRecord()
        }
      }
    })
  },

  createSaleRecord(isUpdate) {
    wx.showLoading({ title: '正在上链存证...', mask: true })

    const { formData, batchNo, invoiceImages } = this.data

    // Build submit data mapping to database trace_sale fields
    let submitData = {
      batchCode: batchNo,
      saleDate: formData.saleDate,
      saleTime: formData.saleTime || null,
      buyerName: formData.buyerName,
      saleQuantity: formData.quantity ? Number(formData.quantity) : null,
      saleUnit: this.data.batchInfo ? this.data.batchInfo.unit : '',
      salePrice: formData.unitPrice ? Number(formData.unitPrice) : null,
      totalAmount: formData.totalAmount ? Number(formData.totalAmount) : null,
      saleVoucher: JSON.stringify(this._serverInvoiceImages || []),
      saleChannel: formData.saleChannel || null,
      destination: formData.destination || null
    }

    // Inject common fields
    submitData = submitHelper.injectCommonFields(submitData, {
      dateField: 'saleDate'
    })

    let apiUrl = '/api/sale/create'
    let method = 'post'
    if (isUpdate && this._existingRecordId) {
      apiUrl = '/api/sale/update'
      method = 'put'
      submitData.id = this._existingRecordId
    }

    request[method](apiUrl, submitData).then(res => {
      wx.hideLoading()
      const result = res.data || {}
      const txHash = result.txHash || ''
      wx.showModal({
        title: '提交成功',
        content: '销售记录已提交并完成上链存证。',
        showCancel: false,
        confirmText: '完成',
        success: () => {
          wx.navigateBack()
        }
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: err.message || '提交失败', icon: 'none' })
    })
  },

  resetForm() {
    const today = util.formatTime(new Date()).split(' ')[0]
    const now = util.formatTime(new Date()).split(' ')[1].substring(0, 5)
    
    this.setData({
      batchNo: '',
      batchInfo: null,
      formData: {
        buyerName: '',
        saleDate: today,
        saleTime: now,
        unitPrice: '',
        quantity: '',
        saleUnit: '',
        totalAmount: '',
        saleChannel: '',
        destination: ''
      },
      invoiceImages: [],
      saleChannelIndex: -1
    })
  }
})
