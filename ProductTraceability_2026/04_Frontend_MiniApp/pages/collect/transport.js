const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')
const submitHelper = require('../../utils/submit-helper.js')
const validator = require('../../utils/validator.js')

Page({
  data: {
    inputMode: 'scan',
    batchNo: '',
    batchInfo: null,
    formData: {
      logisticsNo: '',
      plateNumber: '',
      driverName: '',
      driverPhone: '',
      receiverName: '',
      transportDate: '',
      destination: '',
      location: '',
      latitude: '',
      longitude: '',
      temperature: '',
      humidity: '',
      transportCondition: '',
      transportQuantity: '',
      transportUnit: ''
    },
    locationText: '点击获取发货位置',
    transportImages: [],
    processingEnterprises: [],
    processingEnterpriseNames: [],
    selectedProcessingIndex: -1,
    selectedProcessingId: null,
    isProcessingEnterprise: false,
    availableTransportHint: '',
    availableTransportMax: null
  },

  onLoad(options) {
    // Default transport date to today
    const today = util.formatDate(new Date())
    this.setData({ 'formData.transportDate': today })

    // Check if current user is a processing enterprise
    const userInfo = app.globalData.userInfo || {}
    const enterpriseType = userInfo.enterpriseType || 0
    if (enterpriseType === 2) {
      this.setData({ isProcessingEnterprise: true })
    } else {
      this.loadProcessingEnterprises()
    }

    if (options.batchNo) {
      this.setData({ batchNo: options.batchNo })
      this.loadBatchInfo(options.batchNo)
    }
  },

  switchInputMode(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({
      inputMode: mode,
      batchNo: '',
      batchInfo: null
    })
  },

  onBatchNoInput(e) {
    this.setData({ batchNo: e.detail.value })
  },

  confirmBatchNo() {
    if (!this.data.batchNo) {
      wx.showToast({ title: '请输入批次号', icon: 'none' })
      return
    }
    this.loadBatchInfo(this.data.batchNo)
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
      fail: () => {
        wx.showToast({ title: '扫码失败', icon: 'none' })
      }
    })
  },

  loadBatchInfo(batchNo) {
    wx.showLoading({ title: '加载中...' })
    
    request.get('/api/batch/detail', { batchCode: batchNo }).then(res => {
      wx.hideLoading()
      const data = res.data || {}
      const batchId = data.batchId || data.id

      // 加载可运输数量，有出库记录（availableTransport > 0）即允许
      const userInfo = app.globalData.userInfo || {}
      const enterpriseId = userInfo.enterpriseId || null
      const params = { batchId }
      if (enterpriseId) params.enterpriseId = enterpriseId

      request.get('/api/transport/available-quantity', params).then(availRes => {
        const info = availRes.data || {}
        const avail = Number(info.availableTransport) || 0

        if (avail <= 0) {
          wx.showModal({
            title: '无法操作',
            content: '该批次暂无可运输数量，请先完成出库操作。',
            showCancel: false
          })
          this.setData({ batchNo: '', batchInfo: null })
          return
        }

        const unit = data.unit || ''
        this.setData({
          batchInfo: {
            batchId: batchId,
            batchNo: data.batchCode,
            productName: data.productName,
            quantity: avail,
            unit: unit
          },
          'formData.transportQuantity': String(avail),
          'formData.transportUnit': unit,
          availableTransportHint: '最多可运输 ' + avail + ' ' + unit + '（已出库 ' + (info.totalOutbound || 0) + '，已运输 ' + (info.totalTransported || 0) + '）',
          availableTransportMax: avail
        })
      }).catch(() => {
        wx.showToast({ title: '加载可运输数量失败', icon: 'none' })
        this.setData({ batchInfo: null })
      })
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载批次信息失败', icon: 'none' })
      this.setData({ batchInfo: null })
    })
  },

  loadProcessingEnterprises() {
    request.get('/api/enterprise/list-by-type', { type: 2 }).then(res => {
      const list = res.data || []
      this.setData({
        processingEnterprises: list,
        processingEnterpriseNames: list.map(e => e.enterpriseName)
      })
    }).catch(() => {})
  },

  onProcessingChange(e) {
    const index = Number(e.detail.value)
    const enterprise = this.data.processingEnterprises[index]
    if (!enterprise) return

    // Auto-fill receiver name and destination from selected enterprise
    const fullAddress = [enterprise.province, enterprise.city, enterprise.district, enterprise.address]
      .filter(Boolean)
      .join('')

    this.setData({
      selectedProcessingIndex: index,
      selectedProcessingId: enterprise.id,
      'formData.receiverName': enterprise.enterpriseName || '',
      'formData.destination': fullAddress || ''
    })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`formData.${field}`]: e.detail.value })
  },

  onTemperatureInput(e) {
    let value = e.detail.value || ''
    // 只允许输入数字、小数点和开头的负号
    value = value.replace(/[^0-9.\-]/g, '')
    // 负号只能在开头
    if (value.indexOf('-') > 0) {
      value = value.replace(/-/g, '')
    }
    // 只保留第一个小数点
    const parts = value.split('.')
    if (parts.length > 2) {
      value = parts[0] + '.' + parts.slice(1).join('')
    }
    this.setData({ 'formData.temperature': value })
    return value
  },

  onDateChange(e) {
    this.setData({ 'formData.transportDate': e.detail.value })
  },

  getLocation() {
    wx.chooseLocation({
      success: (res) => {
        const { latitude, longitude, address, name } = res
        const readableLocation = address || name || ''
        this.setData({
          'formData.location': readableLocation,
          'formData.latitude': latitude,
          'formData.longitude': longitude,
          locationText: readableLocation || '已获取定位坐标'
        })
        wx.showToast({ title: '位置获取成功', icon: 'success' })
      },
      fail: () => {
        wx.showToast({ title: '获取位置失败', icon: 'none' })
      }
    })
  },

  takePhoto() {
    const remaining = 5 - this.data.transportImages.length
    if (remaining <= 0) {
      wx.showToast({ title: '最多上传5张', icon: 'none' })
      return
    }
    wx.chooseImage({
      count: remaining,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => {
        const tempPaths = res.tempFilePaths
        wx.showLoading({ title: '上传中...', mask: true })
        submitHelper.uploadImages(tempPaths, 'transport').then(urls => {
          wx.hideLoading()
          if (!this._serverTransportImages) this._serverTransportImages = []
          this._serverTransportImages = [...this._serverTransportImages, ...urls]
          const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
          const fullUrls = urls.map(u => u.startsWith('http') ? u : apiBase + u)
          const newList = this.data.transportImages.concat(fullUrls)
          this.setData({ transportImages: newList })
          util.proxyImages(newList).then(localUrls => {
            this.setData({ transportImages: localUrls })
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
    const url = e.currentTarget.dataset.url
    wx.previewImage({
      current: url,
      urls: this.data.transportImages
    })
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.transportImages
    images.splice(index, 1)
    this.setData({ transportImages: images })
    if (this._serverTransportImages && this._serverTransportImages.length > index) {
      this._serverTransportImages.splice(index, 1)
    }
  },

  validateForm() {
    const { batchNo, batchInfo, formData } = this.data
    
    if (!batchNo || !batchInfo) {
      wx.showToast({ title: '请先关联批次', icon: 'none' })
      return false
    }
    if (!this.data.isProcessingEnterprise && !this.data.selectedProcessingId) {
      wx.showToast({ title: '请选择目标加工企业', icon: 'none' })
      return false
    }
    if (!formData.logisticsNo) {
      wx.showToast({ title: '请输入物流单号', icon: 'none' })
      return false
    }
    if (!formData.plateNumber) {
      wx.showToast({ title: '请输入车牌号', icon: 'none' })
      return false
    }
    if (!formData.driverName) {
      wx.showToast({ title: '请输入司机姓名', icon: 'none' })
      return false
    }
    if (!formData.driverPhone) {
      wx.showToast({ title: '请输入联系电话', icon: 'none' })
      return false
    }
    if (!validator.isValidPhone(formData.driverPhone)) {
      validator.showValidationError('请输入正确的11位手机号')
      return false
    }
    if (!formData.receiverName) {
      wx.showToast({ title: '请输入收货单位', icon: 'none' })
      return false
    }
    if (formData.transportQuantity && !validator.isPositiveNumber(formData.transportQuantity)) {
      validator.showValidationError('运输数量必须为正数')
      return false
    }
    if (formData.temperature && !validator.isValidTemperature(formData.temperature)) {
      validator.showValidationError('车厢温度必须为有效数字（-50到100℃）')
      return false
    }
    if (!formData.latitude || !formData.longitude) {
      wx.showToast({ title: '请获取发货位置', icon: 'none' })
      return false
    }
    return true
  },

  submitForm() {
    if (!this.validateForm()) return
    this._confirmAndSubmit()
  },

  _confirmAndSubmit() {
    wx.showModal({
      title: '确认提交',
      content: '确认提交运输记录？',
      success: (res) => {
        if (res.confirm) {
          this.createTransportRecord()
        }
      }
    })
  },

  createTransportRecord() {
    wx.showLoading({ title: '正在提交...', mask: true })

    const { formData, batchNo, transportImages } = this.data

    let submitData = {
      batchCode: batchNo,
      receiveEnterpriseId: this.data.selectedProcessingId,
      logisticsNo: formData.logisticsNo,
      transportDate: formData.transportDate || submitHelper.getCurrentDate(),
      plateNumber: formData.plateNumber,
      driverName: formData.driverName,
      driverPhone: formData.driverPhone,
      receiverName: formData.receiverName,
      departureLocation: formData.location || null,
      destination: formData.destination || '',
      temperature: formData.temperature ? Number(formData.temperature) : null,
      humidity: formData.humidity ? Number(formData.humidity) : null,
      transportCondition: formData.transportCondition || null,
      transportQuantity: formData.transportQuantity ? Number(formData.transportQuantity) : null,
      images: (this._serverTransportImages && this._serverTransportImages.length > 0) ? JSON.stringify(this._serverTransportImages) : null,
      gpsTrack: formData.latitude ? JSON.stringify({lat: formData.latitude, lng: formData.longitude}) : null
    }

    submitData = submitHelper.injectCommonFields(submitData, {
      dateField: 'transportDate'
    })

    request.post('/api/transport/create', submitData).then(res => {
      wx.hideLoading()
      wx.showModal({
        title: '提交成功',
        content: '运输记录提交成功',
        showCancel: false,
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
    const today = util.formatDate(new Date())
    this.setData({
      inputMode: 'scan',
      batchNo: '',
      batchInfo: null,
      availableTransportHint: '',
      availableTransportMax: null,
      formData: {
        logisticsNo: '',
        plateNumber: '',
        driverName: '',
        driverPhone: '',
        receiverName: '',
        transportDate: today,
        destination: '',
        location: '',
        latitude: '',
        longitude: '',
        temperature: '',
        humidity: '',
        transportCondition: '',
        transportQuantity: '',
        transportUnit: ''
      },
      locationText: '点击获取发货位置',
      transportImages: [],
      selectedProcessingIndex: -1,
      selectedProcessingId: null
    })
  }
})
