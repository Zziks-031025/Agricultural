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
      storageType: '',
      warehouseName: '',
      operator: '',
      storageDate: '',
      storageQuantity: '',
      storageUnit: '',
      temperature: '',
      humidity: '',
      warehouseLocation: '',
      latitude: '',
      longitude: '',
      storageCondition: '',
      remark: ''
    },
    storageTypes: ['入库', '出库', '盘点'],
    warehousePhotos: [],
    locationText: '点击获取仓库位置',
    availableHint: '',
    availableMax: null
  },

  onLoad(options) {
    // Default date and operator
    const today = util.formatDate(new Date())
    const operator = submitHelper.getOperatorName()
    this.setData({
      'formData.storageDate': today,
      'formData.operator': operator || ''
    })

    if (options.batchNo) {
      this.setData({
        batchNo: options.batchNo,
        inputMode: 'input'
      })
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

  loadBatchInfo(batchNo) {
    wx.showLoading({ title: '加载中...' })

    request.get('/api/batch/detail', { batchCode: batchNo }).then(res => {
      wx.hideLoading()
      const data = res.data || {}
      const status = data.batchStatus || data.status || 0

      const userInfo = (require('../../utils/submit-helper.js')).getUserInfo()
      const enterpriseType = userInfo.enterpriseType || 0

      if (enterpriseType === 1) {
        if (status !== 5) {
          wx.showModal({
            title: '无法操作',
            content: '该批次尚未通过检疫，不能进行仓储记录。',
            showCancel: false
          })
          this.setData({ batchNo: '', batchInfo: null })
          return
        }
      } else if (enterpriseType === 2) {
        if (status !== 4 && status !== 5 && status !== 6 && status !== 9) {
          wx.showModal({
            title: '无法操作',
            content: '该批次当前状态不允许仓储录入，请确认批次是否已接收并完成加工。',
            showCancel: false
          })
          this.setData({ batchNo: '', batchInfo: null })
          return
        }
      }

      const batchId = data.batchId || data.id
      const unit = data.unit || ''
      this.setData({
        batchInfo: {
          batchId: batchId,
          batchNo: data.batchCode,
          productName: data.productName,
          unit: unit
        },
        'formData.storageUnit': unit
      })

      // 加载可操作数量
      this._loadAvailableQuantity(batchId, enterpriseType === 1 ? (userInfo.enterpriseId || null) : (userInfo.enterpriseId || null))
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载批次信息失败', icon: 'none' })
      this.setData({ batchInfo: null })
    })
  },

  _loadAvailableQuantity(batchId, enterpriseId) {
    const params = { batchId }
    if (enterpriseId) params.enterpriseId = enterpriseId
    request.get('/api/storage/available-quantity', params).then(res => {
      const info = res.data || {}
      this._availableInfo = info
      this._applyAvailableQuantity()
    }).catch(() => {
      this._availableInfo = null
    })
  },

  _applyAvailableQuantity() {
    const info = this._availableInfo
    if (!info) return
    const unit = info.unit || ''
    // 始终更新批次卡片上的库存概览
    this.setData({
      'batchInfo.availableInbound': info.availableInbound != null ? info.availableInbound : 0,
      'batchInfo.availableOutbound': info.availableOutbound != null ? info.availableOutbound : 0,
      'batchInfo.currentStock': info.currentStock != null ? info.currentStock : 0,
      'batchInfo.unit': unit
    })
    // 如果已选类型，同步填入数量和提示
    const storageType = this.data.formData.storageType
    if (!storageType) return
    let qty = null
    let hint = ''
    if (storageType === '入库') {
      qty = info.availableInbound != null ? info.availableInbound : null
      if (qty !== null && Number(qty) <= 0) {
        wx.showToast({ title: '该批次可入库数量为 0，请切换其他操作类型', icon: 'none', duration: 2500 })
        this.setData({ availableHint: '可入库数量为 0', availableMax: 0, 'formData.storageQuantity': '' })
        return
      }
      hint = qty != null ? ('最多可入库 ' + qty + ' ' + unit) : ''
    } else if (storageType === '出库') {
      qty = info.availableOutbound != null ? info.availableOutbound : null
      if (qty !== null && Number(qty) <= 0) {
        wx.showToast({ title: '该批次可出库数量为 0，请先完成入库', icon: 'none', duration: 2500 })
        this.setData({ availableHint: '可出库数量为 0', availableMax: 0, 'formData.storageQuantity': '' })
        return
      }
      hint = qty != null ? ('最多可出库 ' + qty + ' ' + unit) : ''
    } else if (storageType === '盘点') {
      qty = info.currentStock != null ? info.currentStock : null
      hint = qty != null ? ('当前库存 ' + qty + ' ' + unit) : ''
    }
    const update = {
      availableHint: hint,
      availableMax: qty != null ? Number(qty) : null
    }
    if (qty != null) {
      update['formData.storageQuantity'] = String(qty)
    }
    if (unit) {
      update['formData.storageUnit'] = unit
    }
    this.setData(update)
  },

  onStorageTypeChange(e) {
    this.setData({
      'formData.storageType': this.data.storageTypes[e.detail.value]
    })
    // 切换类型后重新计算数量上限
    this._applyAvailableQuantity()
  },

  onDateChange(e) {
    this.setData({ 'formData.storageDate': e.detail.value })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({ [`formData.${field}`]: value })
    
    // 实时验证数量字段
    if (field === 'storageQuantity' && value) {
      if (!validator.isPositiveNumber(value)) {
        validator.showValidationError('入库数量必须为正数')
      }
    }
    
    // 实时验证温度字段
    if (field === 'temperature' && value) {
      if (!validator.isValidTemperature(value)) {
        validator.showValidationError('仓库温度必须为有效数字（-50到100℃）')
      }
    }
    
    // 实时验证湿度字段
    if (field === 'humidity' && value) {
      if (!validator.isValidHumidity(value)) {
        validator.showValidationError('仓库湿度必须为0-100之间的数字')
      }
    }
  },

  getLocation() {
    wx.chooseLocation({
      success: (res) => {
        const { latitude, longitude, address, name } = res
        const readableLocation = address || name || ''
        this.setData({
          'formData.latitude': latitude,
          'formData.longitude': longitude,
          'formData.warehouseLocation': readableLocation,
          locationText: readableLocation || '已获取定位坐标'
        })
        wx.showToast({ title: '定位成功', icon: 'success' })
      },
      fail: () => {
        wx.showToast({ title: '获取位置失败', icon: 'none' })
      }
    })
  },

  takePhoto() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => {
        const tempPath = res.tempFilePaths[0]
        wx.showLoading({ title: '上传中...', mask: true })
        submitHelper.uploadImage(tempPath, 'storage').then(url => {
          wx.hideLoading()
          if (!this._serverWarehousePhotos) this._serverWarehousePhotos = []
          this._serverWarehousePhotos.push(url)
          const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
          const fullUrl = url.startsWith('http') ? url : apiBase + url
          const photos = this.data.warehousePhotos.concat([fullUrl])
          this.setData({ warehousePhotos: photos })
          util.proxyImages(photos).then(localUrls => {
            this.setData({ warehousePhotos: localUrls })
          })
          wx.showToast({ title: '上传成功', icon: 'success' })
        }).catch(() => {
          wx.hideLoading()
          wx.showToast({ title: '上传失败', icon: 'none' })
        })
      },
      fail: () => {
        wx.showToast({ title: '选择失败', icon: 'none' })
      }
    })
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url
    wx.previewImage({
      current: url,
      urls: this.data.warehousePhotos
    })
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index
    const photos = this.data.warehousePhotos
    photos.splice(index, 1)
    this.setData({ warehousePhotos: photos })
    if (this._serverWarehousePhotos && this._serverWarehousePhotos.length > index) {
      this._serverWarehousePhotos.splice(index, 1)
    }
  },

  validateForm() {
    const { batchNo, batchInfo, formData, warehousePhotos } = this.data
    
    if (!batchNo || !batchInfo) {
      wx.showToast({ title: '请先关联批次', icon: 'none' })
      return false
    }
    if (!formData.storageType) {
      wx.showToast({ title: '请选择入库类型', icon: 'none' })
      return false
    }
    if (!formData.warehouseName) {
      wx.showToast({ title: '请输入仓库名称', icon: 'none' })
      return false
    }
    if (!formData.operator) {
      wx.showToast({ title: '请输入负责人', icon: 'none' })
      return false
    }
    if (formData.storageQuantity && !validator.isPositiveNumber(formData.storageQuantity)) {
      validator.showValidationError('入库数量必须为正数')
      return false
    }
    if (!formData.temperature) {
      wx.showToast({ title: '请输入仓库温度', icon: 'none' })
      return false
    }
    if (!validator.isValidTemperature(formData.temperature)) {
      validator.showValidationError('仓库温度必须为有效数字（-50到100℃）')
      return false
    }
    if (!formData.humidity) {
      wx.showToast({ title: '请输入仓库湿度', icon: 'none' })
      return false
    }
    if (!validator.isValidHumidity(formData.humidity)) {
      validator.showValidationError('仓库湿度必须为0-100之间的数字')
      return false
    }
    if (warehousePhotos.length === 0) {
      wx.showToast({ title: '请拍摄库房实景照片', icon: 'none' })
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
      content: '确认提交仓储记录并上链存证？',
      success: (res) => {
        if (res.confirm) {
          this.createStorageRecord()
        }
      }
    })
  },

  createStorageRecord() {
    wx.showLoading({ title: '正在上链存证...', mask: true })

    const { formData, batchNo, warehousePhotos } = this.data

    let submitData = {
      batchCode: batchNo,
      storageType: submitHelper.mapStorageType(formData.storageType),
      storageDate: formData.storageDate || submitHelper.getCurrentDate(),
      warehouseName: formData.warehouseName,
      warehouseLocation: formData.warehouseLocation || '',
      storageQuantity: formData.storageQuantity ? Number(formData.storageQuantity) : null,
      temperature: formData.temperature ? Number(formData.temperature) : null,
      humidity: formData.humidity ? Number(formData.humidity) : null,
      operator: formData.operator,
      storageCondition: formData.storageCondition || null,
      remark: formData.remark || null,
      images: JSON.stringify(this._serverWarehousePhotos || [])
    }

    submitData = submitHelper.injectCommonFields(submitData, {
      dateField: 'storageDate'
    })

    request.post('/api/storage/create', submitData).then(res => {
      wx.hideLoading()
      const result = res.data || {}
      const txHash = result.txHash || ''
      wx.showModal({
        title: '仓储数据已存证',
        content: '仓储记录提交成功' + (txHash ? '\n\n交易哈希:\n' + txHash.substr(0, 20) + '...' : ''),
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
    const operator = submitHelper.getOperatorName()
    this._availableInfo = null
    this.setData({
      batchNo: '',
      batchInfo: null,
      availableHint: '',
      availableMax: null,
      formData: {
        storageType: '',
        warehouseName: '',
        operator: operator || '',
        storageDate: today,
        storageQuantity: '',
        storageUnit: '',
        temperature: '',
        humidity: '',
        warehouseLocation: '',
        latitude: '',
        longitude: '',
        storageCondition: '',
        remark: ''
      },
      warehousePhotos: [],
      locationText: '点击获取仓库位置'
    })
  }
})
