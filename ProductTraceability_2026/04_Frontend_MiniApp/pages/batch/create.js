const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')
const submitHelper = require('../../utils/submit-helper.js')
const validator = require('../../utils/validator.js')

Page({
  data: {
    templateType: 1,
    templates: ['肉鸡养殖', '西红柿种植'],
    formData: {
      batchNo: '',
      productName: '肉鸡',
      breed: '',
      quantity: '',
      unit: '只',
      origin: '',
      seedSource: '',
      manager: '',
      plantArea: '',
      greenhouseNo: '',
      productionDate: '',
      expectedHarvestDate: '',
      location: '',
      latitude: '',
      longitude: ''
    },
    locationText: '点击获取位置'
  },

  onLoad() {
    this.generateBatchNo()
    // Default production date to today
    const today = util.formatDate(new Date())
    // Default manager to current user
    const operator = submitHelper.getOperatorName()
    this.setData({
      'formData.productionDate': today,
      'formData.manager': operator || ''
    })
  },

  generateBatchNo() {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hour = String(now.getHours()).padStart(2, '0')
    const min = String(now.getMinutes()).padStart(2, '0')
    const sec = String(now.getSeconds()).padStart(2, '0')
    const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
    const batchNo = `BATCH${year}${month}${day}${hour}${min}${sec}${random}`
    
    this.setData({
      'formData.batchNo': batchNo
    })
  },

  switchTemplate(e) {
    const type = parseInt(e.currentTarget.dataset.type)
    const productName = submitHelper.getDefaultProductName(type)
    const unit = submitHelper.getDefaultUnit(type)
    this.setData({
      templateType: type,
      'formData.productName': productName,
      'formData.unit': unit
    })
    this.generateBatchNo()
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
    
    // 实时验证数量字段
    if (field === 'quantity' && value) {
      if (!validator.isPositiveNumber(value)) {
        validator.showValidationError('数量必须为正数')
      }
    }
    
    // 实时验证种植面积字段
    if (field === 'plantArea' && value) {
      if (!validator.isPositiveNumber(value)) {
        validator.showValidationError('种植面积必须为正数')
      }
    }
  },

  onProductionDateChange(e) {
    this.setData({
      'formData.productionDate': e.detail.value
    })
  },

  onExpectedDateChange(e) {
    this.setData({
      'formData.expectedHarvestDate': e.detail.value
    })
  },

  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        const { name, address, latitude, longitude } = res
        const readableLocation = address || name || ''
        const locationLabel = readableLocation || '已获取定位坐标'
        
        const updateData = {
          'formData.location': readableLocation,
          'formData.latitude': latitude,
          'formData.longitude': longitude,
          locationText: locationLabel
        }
        // 定位成功后自动填入产地输入框（若产地为空）
        if (readableLocation && !this.data.formData.origin) {
          updateData['formData.origin'] = readableLocation
        }
        this.setData(updateData)
        
        wx.showToast({
          title: '位置获取成功',
          icon: 'success'
        })
      },
      fail: (err) => {
        console.error('选择位置失败:', err)
        wx.showToast({
          title: '位置获取失败',
          icon: 'none'
        })
      }
    })
  },

  validateForm() {
    const { formData, templateType } = this.data
    
    if (!formData.batchNo) {
      wx.showToast({ title: '批次号不能为空', icon: 'none' })
      return false
    }
    
    if (!formData.breed) {
      wx.showToast({ title: '请输入品种', icon: 'none' })
      return false
    }
    
    if (templateType === 1) {
      if (!formData.quantity) {
        wx.showToast({ title: '请输入入栏数量', icon: 'none' })
        return false
      }
      if (!validator.isPositiveNumber(formData.quantity)) {
        validator.showValidationError('入栏数量必须为正数')
        return false
      }
      if (!formData.origin) {
        wx.showToast({ title: '请输入产地', icon: 'none' })
        return false
      }
    } else {
      if (!formData.quantity) {
        wx.showToast({ title: '请输入种植数量', icon: 'none' })
        return false
      }
      if (!validator.isPositiveNumber(formData.quantity)) {
        validator.showValidationError('种植数量必须为正数')
        return false
      }
      if (!formData.plantArea) {
        wx.showToast({ title: '请输入种植面积', icon: 'none' })
        return false
      }
      if (!validator.isPositiveNumber(formData.plantArea)) {
        validator.showValidationError('种植面积必须为正数')
        return false
      }
      if (!formData.greenhouseNo) {
        wx.showToast({ title: '请输入大棚号', icon: 'none' })
        return false
      }
    }
    
    if (!formData.manager) {
      wx.showToast({ title: '请输入负责人', icon: 'none' })
      return false
    }
    
    return true
  },

  submitForm() {
    if (!this.validateForm()) {
      return
    }
    
    wx.showModal({
      title: '确认提交',
      content: '确认创建该养殖批次？',
      success: (res) => {
        if (res.confirm) {
          this.createBatch()
        }
      }
    })
  },

  createBatch() {
    wx.showLoading({ title: '正在创建批次...', mask: true })
    
    const { formData, templateType } = this.data

    // Build submit data mapping to database fields
    let submitData = {
      batchCode: formData.batchNo,
      productName: formData.productName || submitHelper.getDefaultProductName(templateType),
      productType: templateType,
      breed: formData.breed,
      initQuantity: formData.quantity ? Number(formData.quantity) : null,
      unit: formData.unit || submitHelper.getDefaultUnit(templateType),
      originLocation: formData.origin || formData.location || '',
      seedSource: formData.seedSource || '',
      manager: formData.manager,
      latitude: formData.latitude || null,
      longitude: formData.longitude || null,
      productionDate: formData.productionDate || submitHelper.getCurrentDate(),
      expectedHarvestDate: formData.expectedHarvestDate || null
    }

    // Tomato-specific fields
    if (templateType === 2) {
      submitData.plantArea = formData.plantArea ? Number(formData.plantArea) : null
      submitData.greenhouseNo = formData.greenhouseNo || null
      // For tomato, origin might be from location
      if (!submitData.originLocation && formData.location) {
        submitData.originLocation = formData.location
      }
    }

    // Inject enterprise_id and operator from user info
    submitData = submitHelper.injectCommonFields(submitData, {
      dateField: 'productionDate'
    })

    request.post('/api/batch/create', submitData).then(res => {
      wx.hideLoading()
      const result = res.data || {}
      const batchCode = result.batchCode || submitData.batchCode

      wx.showModal({
        title: '批次创建成功',
        content: `批次 ${batchCode} 创建成功，数据将在入库时统一上链存证`,
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: err.message || '创建失败', icon: 'none' })
    })
  },

  resetForm() {
    const today = util.formatDate(new Date())
    const operator = submitHelper.getOperatorName()
    this.setData({
      formData: {
        batchNo: '',
        productName: this.data.templateType === 1 ? '肉鸡' : '西红柿',
        breed: '',
        quantity: '',
        unit: this.data.templateType === 1 ? '只' : '株',
        origin: '',
        seedSource: '',
        manager: operator || '',
        plantArea: '',
        greenhouseNo: '',
        productionDate: today,
        expectedHarvestDate: '',
        location: '',
        latitude: '',
        longitude: ''
      },
      locationText: '点击获取位置'
    })
    this.generateBatchNo()
  }
})
