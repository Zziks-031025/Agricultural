const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

Page({
  data: {
    today: '',
    formData: {
      batchCode: '',
      recordDate: '',
      recordType: '',
      operator: '',
      operationContent: '',
      temperature: '',
      humidity: '',
      materialName: '',
      materialAmount: '',
      images: [],
      location: null
    },
    batchInfo: {},
    recordTypes: [
      { value: 1, label: '播种/投苗' },
      { value: 2, label: '施肥' },
      { value: 3, label: '浇水' },
      { value: 4, label: '用药' },
      { value: 5, label: '环境监测' },
      { value: 6, label: '日常巡查' },
      { value: 7, label: '其他' }
    ],
    recordTypeIndex: 0,
    showMaterialInfo: false,
    submitting: false,
    showBlockchainModal: false,
    txHash: ''
  },

  onLoad(options) {
    this.initFormData()
    
    if (options.batchCode) {
      this.setData({
        'formData.batchCode': options.batchCode
      })
      this.loadBatchInfo(options.batchCode)
    }
  },

  /**
   * 初始化表单数据
   */
  initFormData() {
    const today = util.formatDate(new Date())
    const userInfo = app.globalData.userInfo || {}
    
    this.setData({
      today,
      'formData.recordDate': today,
      'formData.operator': userInfo.realName || ''
    })
  },

  /**
   * 扫描批次二维码
   */
  scanBatchCode() {
    util.scanCode().then(result => {
      this.setData({
        'formData.batchCode': result
      })
      this.loadBatchInfo(result)
    }).catch(err => {
      console.error('扫码失败:', err)
      util.showError('扫码失败，请重试')
    })
  },

  /**
   * 加载批次信息
   */
  loadBatchInfo(batchCode) {
    util.showLoading('加载中...')
    
    request.get('/api/batch/detail', { batchCode }).then(res => {
      util.hideLoading()
      if (res.code === 200) {
        this.setData({
          batchInfo: res.data
        })
      } else {
        util.showError('批次不存在')
      }
    }).catch(err => {
      util.hideLoading()
      console.error('加载批次信息失败:', err)
    })
  },

  /**
   * 输入框变化
   */
  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    const value = e.detail.value
    this.setData({
      [`formData.${field}`]: value
    })
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
   * 记录类型选择
   */
  onRecordTypeChange(e) {
    const index = e.detail.value
    const recordType = this.data.recordTypes[index]
    
    this.setData({
      recordTypeIndex: index,
      'formData.recordType': recordType.value,
      showMaterialInfo: recordType.value === 2 || recordType.value === 4
    })
  },

  /**
   * 选择图片
   */
  chooseImage() {
    const maxCount = 9 - this.data.formData.images.length
    
    util.chooseImage(maxCount).then(tempFilePaths => {
      this.uploadImages(tempFilePaths)
    }).catch(err => {
      console.error('选择图片失败:', err)
    })
  },

  /**
   * 上传图片
   */
  uploadImages(tempFilePaths) {
    util.showLoading('上传中...')
    
    const uploadPromises = tempFilePaths.map(filePath => {
      return request.upload('/api/file/upload', filePath)
    })

    Promise.all(uploadPromises).then(results => {
      util.hideLoading()
      
      const imageUrls = results.map(res => res.data.url)
      if (!this._serverImages) this._serverImages = []
      this._serverImages = [...this._serverImages, ...imageUrls]
      const apiBase = (app.globalData && app.globalData.apiBaseUrl) || ''
      const fullUrls = imageUrls.map(u => u.startsWith('http') ? u : apiBase + u)
      const currentImages = this.data.formData.images
      const newImages = [...currentImages, ...fullUrls]
      this.setData({ 'formData.images': newImages })
      util.proxyImages(newImages).then(localUrls => {
        this.setData({ 'formData.images': localUrls })
      })
      
      util.showSuccess('上传成功')
    }).catch(err => {
      util.hideLoading()
      console.error('上传图片失败:', err)
      util.showError('上传失败，请重试')
    })
  },

  /**
   * 删除图片
   */
  deleteImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.formData.images
    
    util.showConfirm('确定删除这张图片吗？').then(confirm => {
      if (confirm) {
        images.splice(index, 1)
        this.setData({ 'formData.images': images })
        if (this._serverImages && this._serverImages.length > index) {
          this._serverImages.splice(index, 1)
        }
      }
    })
  },

  /**
   * 预览图片
   */
  previewImage(e) {
    const url = e.currentTarget.dataset.url
    util.previewImage(url, this.data.formData.images)
  },

  /**
   * 获取位置
   */
  getLocation() {
    util.showLoading('获取位置中...')
    
    util.getLocation().then(location => {
      this.reverseGeocode(location)
    }).catch(err => {
      util.hideLoading()
      console.error('获取位置失败:', err)
      
      wx.showModal({
        title: '提示',
        content: '获取位置失败，请检查定位权限',
        confirmText: '去设置',
        success: res => {
          if (res.confirm) {
            wx.openSetting()
          }
        }
      })
    })
  },

  /**
   * 逆地理编码
   */
  reverseGeocode(location) {
    request.get('/api/common/geocode', {
      latitude: location.latitude,
      longitude: location.longitude
    }).then(res => {
      util.hideLoading()
      
      if (res.code === 200) {
        this.setData({
          'formData.location': {
            latitude: location.latitude,
            longitude: location.longitude,
            address: res.data.address
          }
        })
        util.showSuccess('位置获取成功')
      }
    }).catch(err => {
      util.hideLoading()
      console.error('逆地理编码失败:', err)
      
      this.setData({
        'formData.location': {
          latitude: location.latitude,
          longitude: location.longitude,
          address: '位置解析失败'
        }
      })
    })
  },

  /**
   * 表单验证
   */
  validateForm() {
    const { batchCode, recordDate, recordType, operator, operationContent } = this.data.formData

    if (!batchCode) {
      util.showError('请输入或扫描批次编号')
      return false
    }

    if (!recordDate) {
      util.showError('请选择记录日期')
      return false
    }

    if (!recordType) {
      util.showError('请选择记录类型')
      return false
    }

    if (!operator) {
      util.showError('请输入操作人')
      return false
    }

    if (!operationContent) {
      util.showError('请输入操作内容')
      return false
    }

    return true
  },

  /**
   * 提交表单
   */
  submitForm() {
    if (!this.validateForm()) {
      return
    }

    if (this.data.submitting) {
      return
    }

    util.showConfirm('确认提交数据？').then(confirm => {
      if (confirm) {
        this.doSubmit()
      }
    })
  },

  /**
   * 执行提交
   */
  doSubmit() {
    this.setData({ submitting: true })
    util.showLoading('提交中...')

    const submitData = {
      ...this.data.formData,
      images: JSON.stringify(this._serverImages || []),
      location: JSON.stringify(this.data.formData.location)
    }

    request.post('/api/record/create', submitData).then(res => {
      util.hideLoading()
      this.setData({ submitting: false })

      if (res.code === 200) {
        wx.showModal({
          title: '提交成功',
          content: '数据已保存成功',
          showCancel: true,
          confirmText: '继续录入',
          cancelText: '返回',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.resetForm()
            } else {
              wx.navigateBack()
            }
          }
        })
      } else {
        util.showError(res.message || '提交失败')
      }
    }).catch(err => {
      util.hideLoading()
      this.setData({ submitting: false })
      console.error('提交失败:', err)
      util.showError('提交失败，请重试')
    })
  },

  /**
   * 关闭区块链弹窗
   */
  closeBlockchainModal() {
    this.setData({
      showBlockchainModal: false
    })

    wx.showModal({
      title: '提示',
      content: '是否继续录入数据？',
      confirmText: '继续录入',
      cancelText: '返回',
      success: res => {
        if (res.confirm) {
          this.resetForm()
        } else {
          wx.navigateBack()
        }
      }
    })
  },

  /**
   * 复制交易哈希
   */
  copyTxHash() {
    wx.setClipboardData({
      data: this.data.txHash,
      success: () => {
        util.showSuccess('已复制到剪贴板')
      }
    })
  },

  /**
   * 重置表单
   */
  resetForm() {
    const today = util.formatDate(new Date())
    const userInfo = app.globalData.userInfo || {}
    
    this.setData({
      formData: {
        batchCode: this.data.formData.batchCode,
        recordDate: today,
        recordType: '',
        operator: userInfo.realName || '',
        operationContent: '',
        temperature: '',
        humidity: '',
        materialName: '',
        materialAmount: '',
        images: [],
        location: null
      },
      recordTypeIndex: 0,
      showMaterialInfo: false
    })
  }
})
