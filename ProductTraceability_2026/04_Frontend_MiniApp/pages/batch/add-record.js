/**
 * 添加生长记录页面
 * 根据产品类型（肉鸡/西红柿）动态展示不同的表单模板
 */
const app = getApp()
const request = require('../../utils/request.js')
const submitHelper = require('../../utils/submit-helper.js')
const util = require('../../utils/util.js')
const validator = require('../../utils/validator.js')

// 产品类型配置
const PRODUCT_CONFIG = {
  // 肉鸡养殖 (productType = 1)
  '1': {
    name: 'chicken',
    label: '肉鸡养殖',
    pageTitle: '添加养殖记录',
    sectionTitle: '养殖信息',
    recordTypes: [
      { value: 'feeding', label: '喂养' },
      { value: 'vaccine', label: '防疫' },
      { value: 'inspect', label: '环境巡查' }
    ],
    // 字段标签配置
    fieldLabels: {
      feeding: { materialLabel: '饲料名称', materialPlaceholder: '请输入饲料名称', unit: 'kg' },
      vaccine: { materialLabel: '疫苗名称', materialPlaceholder: '请输入疫苗名称', unit: 'ml' },
      inspect: { materialLabel: '巡检作业', materialPlaceholder: '如: 温湿度检测、通风检查、卫生巡查', unit: '' }
    }
  },
  // 西红柿种植 (productType = 2)
  '2': {
    name: 'tomato',
    label: '西红柿种植',
    pageTitle: '添加种植记录',
    sectionTitle: '种植信息',
    recordTypes: [
      { value: 'fertilize', label: '施肥' },
      { value: 'irrigate', label: '灌溉' },
      { value: 'pesticide', label: '除虫' }
    ],
    // 字段标签配置
    fieldLabels: {
      fertilize: { materialLabel: '肥料名称', materialPlaceholder: '请输入肥料名称', unit: 'kg' },
      irrigate: { materialLabel: '灌溉量', materialPlaceholder: '请输入灌溉量', unit: 'L' },
      pesticide: { materialLabel: '农药名称', materialPlaceholder: '请输入农药名称', unit: 'ml' }
    }
  }
}

Page({
  data: {
    today: '',
    // 批次信息
    batchId: '',
    productType: '1', // 产品类型：1-肉鸡，2-西红柿
    batchInfo: {
      batchCode: '',
      productName: '',
      productType: 'chicken', // chicken 或 tomato
      enterpriseName: ''
    },
    
    // 产品配置（根据产品类型动态设置）
    productConfig: null,
    
    // 记录类型列表（根据产品类型动态设置）
    recordTypes: [],
    
    // 当前记录类型的字段配置
    currentFieldConfig: null,
    
    // 表单数据
    formData: {
      recordType: '',
      materialName: '',
      dosage: '',
      description: '',
      recordDate: '',
      operator: '',
      images: [],
      location: null,
      locationText: ''
    },
    locationDisplayText: '点击获取位置',
    
    // 提交状态
    submitting: false,
    submitStatus: '',
    
    // 加载弹窗
    showLoadingModal: false,
    loadingText: '',
    loadingStep: '',
    
    // 成功弹窗
    showSuccessModal: false,
    txHash: '',
    blockNumber: '',
    gasUsed: ''
  },

  onLoad(options) {
    this._serverImages = []
    const { batchId, productType } = options
    
    if (!batchId) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    
    // 获取产品类型，默认为1（肉鸡）
    const type = productType || '1'
    
    this.setData({ 
      batchId,
      productType: type
    })
    
    // 初始化产品配置
    this.initProductConfig(type)
    
    // 加载批次详细信息
    this.loadBatchInfo(batchId, type)
    
    // 设置默认日期为今天
    const today = this.formatDate(new Date())
    this.setData({ today, 'formData.recordDate': today })
    
    // 获取当前用户信息作为默认操作人
    const userInfo = app.globalData.userInfo
    if (userInfo && userInfo.realName) {
      this.setData({ 'formData.operator': userInfo.realName })
    }
  },

  /**
   * 初始化产品配置
   */
  initProductConfig(productType) {
    const config = PRODUCT_CONFIG[productType] || PRODUCT_CONFIG['1']
    const recordTypes = config.recordTypes
    const defaultRecordType = recordTypes[0].value
    const currentFieldConfig = config.fieldLabels[defaultRecordType]
    
    // 设置页面标题
    wx.setNavigationBarTitle({
      title: config.pageTitle
    })
    
    this.setData({
      productConfig: config,
      recordTypes: recordTypes,
      currentFieldConfig: currentFieldConfig,
      'formData.recordType': defaultRecordType
    })
  },

  /**
   * 加载批次信息
   */
  loadBatchInfo(batchId, productType) {
    const config = PRODUCT_CONFIG[productType] || PRODUCT_CONFIG['1']
    
    request.get('/api/batch/detail', { id: batchId }).then(res => {
      const data = res.data || {}
      const batchInfo = {
        batchCode: data.batchCode || '',
        productName: data.productName || '',
        productType: config.name,
        enterpriseName: data.enterpriseName || ''
      }
      this.setData({ batchInfo })
    }).catch(() => {
      this.setData({
        batchInfo: {
          batchCode: '',
          productName: '',
          productType: config.name,
          enterpriseName: ''
        }
      })
    })
  },

  /**
   * 选择记录类型
   */
  selectRecordType(e) {
    const { value } = e.currentTarget.dataset
    const { productConfig } = this.data
    
    // 获取当前记录类型的字段配置
    const currentFieldConfig = productConfig.fieldLabels[value] || {}
    
    this.setData({
      'formData.recordType': value,
      'formData.materialName': '',
      'formData.dosage': '',
      currentFieldConfig: currentFieldConfig
    })
  },

  /**
   * 输入框变化
   */
  onInputChange(e) {
    const { field } = e.currentTarget.dataset
    const { value } = e.detail
    this.setData({
      [`formData.${field}`]: value
    })
    
    // 实时验证用量字段
    if (field === 'dosage' && value) {
      if (!validator.isPositiveNumber(value)) {
        validator.showValidationError('用量必须为正数')
      }
    }
  },

  /**
   * 日期选择
   */
  onDateChange(e) {
    this.setData({
      'formData.recordDate': e.detail.value
    })
  },

  /**
   * 选择图片
   */
  chooseImage() {
    const { images } = this.data.formData
    const remaining = 9 - images.length
    
    wx.chooseImage({
      count: remaining,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        wx.showLoading({ title: '上传中...', mask: true })
        submitHelper.uploadImages(res.tempFilePaths, 'record').then(urls => {
          wx.hideLoading()
          // urls是服务器相对路径如 /uploads/record/xxx.jpg
          // _serverImages保存原始相对路径用于提交
          if (!this._serverImages) this._serverImages = []
          this._serverImages = [...this._serverImages, ...urls]
          // 拼接完整URL用于显示
          const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
          const fullUrls = urls.map(u => u.startsWith('http') ? u : apiBase + u)
          const newImages = [...images, ...fullUrls]
          this.setData({ 'formData.images': newImages })
          // 真机代理下载http图片
          util.proxyImages(newImages).then(localUrls => {
            this.setData({ 'formData.images': localUrls })
          })
          wx.showToast({ title: '上传成功', icon: 'success' })
        }).catch(() => {
          wx.hideLoading()
          wx.showToast({ title: '上传失败', icon: 'none' })
        })
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
      urls: this.data.formData.images
    })
  },

  /**
   * 删除图片
   */
  deleteImage(e) {
    const { index } = e.currentTarget.dataset
    const images = [...this.data.formData.images]
    images.splice(index, 1)
    this.setData({
      'formData.images': images
    })
    // 同步删除_serverImages中对应的路径
    if (this._serverImages && this._serverImages.length > index) {
      this._serverImages.splice(index, 1)
    }
  },

  /**
   * 选择位置（复用批次初始化页的地图选点模式）
   */
  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        const { name, address, latitude, longitude } = res
        const locationStr = address || name || `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`

        const location = {
          latitude: latitude.toFixed(6),
          longitude: longitude.toFixed(6),
          address: locationStr
        }

        const updateData = {
          'formData.location': location,
          locationDisplayText: locationStr
        }
        // 定位成功后自动填入位置描述输入框（若为空）
        if (!this.data.formData.locationText) {
          updateData['formData.locationText'] = locationStr
        }
        this.setData(updateData)

        wx.showToast({ title: '位置获取成功', icon: 'success' })
      },
      fail: (err) => {
        console.error('选择位置失败:', err)
        if (err.errMsg && err.errMsg.includes('auth deny')) {
          wx.showModal({
            title: '定位权限',
            content: '需要获取您的位置信息来记录作业地点，请在设置中开启定位权限',
            confirmText: '去设置',
            success: (res) => {
              if (res.confirm) wx.openSetting()
            }
          })
        } else if (!err.errMsg.includes('cancel')) {
          wx.showToast({ title: '位置获取失败', icon: 'none' })
        }
      }
    })
  },

  /**
   * 表单验证
   */
  validateForm() {
    const { formData, currentFieldConfig } = this.data
    
    if (!formData.recordType) {
      wx.showToast({ title: '请选择记录类型', icon: 'none' })
      return false
    }
    
    // 根据当前字段配置验证必填字段
    if (currentFieldConfig && currentFieldConfig.materialLabel && !formData.materialName) {
      wx.showToast({ title: `请输入${currentFieldConfig.materialLabel}`, icon: 'none' })
      return false
    }
    
    // 验证用量字段（如果填写了）
    if (formData.dosage && !validator.isPositiveNumber(formData.dosage)) {
      validator.showValidationError('用量必须为正数')
      return false
    }
    
    if (!formData.description) {
      wx.showToast({ title: '请输入操作说明', icon: 'none' })
      return false
    }
    
    if (!formData.recordDate) {
      wx.showToast({ title: '请选择记录日期', icon: 'none' })
      return false
    }
    
    if (!formData.operator) {
      wx.showToast({ title: '请输入操作人', icon: 'none' })
      return false
    }
    
    return true
  },

  /**
   * 提交表单
   */
  async submitForm() {
    if (!this.validateForm()) return
    
    this.setData({
      submitting: true,
      showLoadingModal: true,
      loadingText: '正在提交数据...',
      loadingStep: '保存生长记录中'
    })
    
    try {
      // Build submit data mapping to database trace_record fields
      const { formData, batchId, batchInfo } = this.data
      let submitData = {
        batchCode: batchInfo.batchCode || '',
        recordType: formData.recordType,
        recordDate: formData.recordDate,
        itemName: formData.materialName || '',
        amount: formData.dosage ? Number(formData.dosage) : null,
        description: formData.description,
        operator: formData.operator,
        images: JSON.stringify(this._serverImages || []),
        location: formData.locationText || (formData.location ? formData.location.address : ''),
        latitude: formData.location ? formData.location.latitude : null,
        longitude: formData.location ? formData.location.longitude : null
      }

      // Inject common fields (enterpriseId, default date)
      submitData = submitHelper.injectCommonFields(submitData, {
        dateField: 'recordDate'
      })

      const res = await request.post('/api/record/create', submitData)

      this.setData({
        showLoadingModal: false,
        showSuccessModal: true,
        submitting: false
      })
      
    } catch (error) {
      this.setData({
        showLoadingModal: false,
        submitting: false
      })
      wx.showToast({ title: error.message || '提交失败，请重试', icon: 'none' })
    }
  },

  /**
   * 复制交易哈希
   */
  copyTxHash() {
    wx.setClipboardData({
      data: this.data.txHash,
      success: () => {
        wx.showToast({ title: '已复制', icon: 'success' })
      }
    })
  },

  /**
   * 关闭成功弹窗
   */
  closeSuccessModal() {
    this.setData({ showSuccessModal: false })
    wx.navigateBack()
  },

  /**
   * 生成随机哈希
   */
  generateRandomHash(length) {
    const chars = '0123456789abcdef'
    let hash = ''
    for (let i = 0; i < length; i++) {
      hash += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return hash
  },

  /**
   * 延迟函数
   */
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  },

  /**
   * 格式化日期
   */
  formatDate(date) {
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    return `${year}-${month}-${day}`
  }
})
