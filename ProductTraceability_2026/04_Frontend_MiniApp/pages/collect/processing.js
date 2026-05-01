// pages/collect/processing.js
const config = require('../../config.js')
const util = require('../../utils/util.js')
const app = getApp()

Page({
  data: {
    sourceBatchCode: '',
    sourceBatchInfo: null,
    methodList: ['屠宰', '分割', '冷冻', '熟制'],
    methodIndex: 0,
    unitList: ['只', 'kg', '箱', '袋', '头', '株'],
    inputUnitIndex: 0,
    outputUnitIndex: 1,
    formData: {
      processingDate: '',
      processingMethod: '',
      packagingSpec: '',
      operator: '',
      inputQuantity: '',
      inputUnit: '只',
      outputQuantity: '',
      outputUnit: 'kg'
    },
    remark: '',
    photoList: [],
    maxPhotos: 6,
    submitting: false
  },

  onLoad(options) {
    var userInfo = app.globalData.userInfo || {}
    this.setData({ 'formData.operator': userInfo.realName || '' })
    if (options && options.batchCode) {
      this.setData({ sourceBatchCode: options.batchCode })
      this.queryBatchInfo(options.batchCode)
    }
  },

  /**
   * 从扫码原始内容中解析批次编号
   */
  parseBatchCode(content) {
    if (!content) return null
    var urlParamMatch = content.match(/[?&](?:batchId|batchCode)=([^&]+)/)
    if (urlParamMatch) return urlParamMatch[1]
    var urlPathMatch = content.match(/\/batch\/([A-Za-z0-9]+)/)
    if (urlPathMatch) return urlPathMatch[1]
    var batchMatch = content.match(/(BATCH[A-Za-z0-9]+)/i)
    if (batchMatch) return batchMatch[1].toUpperCase()
    if (/^[A-Za-z0-9]{8,}$/.test(content)) return content
    return null
  },

  scanBatchCode() {
    var that = this
    util.scanCode().then(function(result) {
      if (result) {
        var batchCode = that.parseBatchCode(result)
        if (!batchCode) {
          util.showError('无法识别的二维码格式')
          return
        }
        that.setData({ sourceBatchCode: batchCode })
        that.queryBatchInfo(batchCode)
      }
    }).catch(function() {
      util.showError('扫码取消或失败')
    })
  },

  onBatchCodeInput(e) {
    this.setData({ sourceBatchCode: e.detail.value })
  },

  confirmBatchCode() {
    var raw = this.data.sourceBatchCode.trim()
    if (!raw) {
      util.showError('请输入批次编号')
      return
    }
    var code = this.parseBatchCode(raw) || raw
    this.setData({ sourceBatchCode: code })
    this.queryBatchInfo(code)
  },

  queryBatchInfo(batchCode) {
    var that = this
    var token = app.globalData.token
    util.showLoading('查询中...')
    wx.request({
      url: config.API_BASE_URL + '/api/batch/detail?batchCode=' + batchCode,
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
      success: function(res) {
        util.hideLoading()
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          var data = res.data.data
          var status = data.batchStatus || data.status
          if (status !== 4 && status !== 5) {
            wx.showModal({
              title: '无法加工',
              content: '该批次当前状态不允许加工录入，请确认批次是否已接收。',
              showCancel: false
            })
            return
          }
          that.setData({
            sourceBatchInfo: {
              batchId: data.batchId || data.id,
              batchCode: data.batchCode || batchCode,
              productName: data.productName || '--',
              currentQuantity: data.currentQuantity || data.quantity || data.initQuantity || '',
              unit: data.unit || '只',
              supplierName: data.enterpriseName || '--',
              productionDate: data.productionDate || '--'
            },
            'formData.inputQuantity': String(data.currentQuantity || data.quantity || data.initQuantity || ''),
            'formData.inputUnit': data.unit || '只',
            inputUnitIndex: that.data.unitList.indexOf(data.unit || '只') >= 0 ? that.data.unitList.indexOf(data.unit || '只') : 0
          })
        } else {
          var msg = (res.data && res.data.message) || '查询失败'
          wx.showModal({ title: '查询失败', content: msg, showCancel: false })
        }
      },
      fail: function() {
        util.hideLoading()
        util.showError('网络错误，请检查网络后重试')
      }
    })
  },

  changeBatch() {
    this.setData({ sourceBatchCode: '', sourceBatchInfo: null })
  },

  onDateChange(e) {
    this.setData({ 'formData.processingDate': e.detail.value })
  },

  onMethodChange(e) {
    var idx = e.detail.value
    this.setData({
      methodIndex: idx,
      'formData.processingMethod': this.data.methodList[idx]
    })
  },

  onInputUnitChange(e) {
    var idx = e.detail.value
    this.setData({
      inputUnitIndex: idx,
      'formData.inputUnit': this.data.unitList[idx]
    })
  },

  onOutputUnitChange(e) {
    var idx = e.detail.value
    this.setData({
      outputUnitIndex: idx,
      'formData.outputUnit': this.data.unitList[idx]
    })
  },

  onInputChange(e) {
    var field = e.currentTarget.dataset.field
    if (field) {
      this.setData({ ['formData.' + field]: e.detail.value })
    }
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  takePhoto() {
    var that = this
    var remaining = that.data.maxPhotos - that.data.photoList.length
    if (remaining <= 0) {
      util.showError('最多上传' + that.data.maxPhotos + '张照片')
      return
    }
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: function(res) {
        var newPhotos = res.tempFiles.map(function(f) { return f.tempFilePath })
        that.setData({ photoList: that.data.photoList.concat(newPhotos) })
      }
    })
  },

  previewPhoto(e) {
    wx.previewImage({
      current: e.currentTarget.dataset.url,
      urls: this.data.photoList
    })
  },

  deletePhoto(e) {
    var list = this.data.photoList
    list.splice(e.currentTarget.dataset.index, 1)
    this.setData({ photoList: list })
  },

  resetForm() {
    var userInfo = app.globalData.userInfo || {}
    this.setData({
      sourceBatchCode: '',
      sourceBatchInfo: null,
      methodIndex: 0,
      inputUnitIndex: 0,
      outputUnitIndex: 1,
      formData: {
        processingDate: '',
        processingMethod: '',
        packagingSpec: '',
        operator: userInfo.realName || '',
        inputQuantity: '',
        inputUnit: '只',
        outputQuantity: '',
        outputUnit: 'kg'
      },
      remark: '',
      photoList: []
    })
  },

  uploadPhotos(callback) {
    var photos = this.data.photoList
    if (!photos || photos.length === 0) { callback([]); return }
    var token = app.globalData.token
    var uploaded = []
    var total = photos.length
    var completed = 0
    for (var i = 0; i < total; i++) {
      (function(filePath) {
        wx.uploadFile({
          url: config.API_BASE_URL + '/api/file/upload',
          filePath: filePath,
          name: 'file',
          formData: { type: 'processing' },
          header: { 'Authorization': 'Bearer ' + token },
          success: function(res) {
            try {
              var data = JSON.parse(res.data)
              if (data.code === 200 && data.data) {
                uploaded.push(data.data.url || data.data)
              } else { uploaded.push(filePath) }
            } catch (e) { uploaded.push(filePath) }
          },
          fail: function() { uploaded.push(filePath) },
          complete: function() {
            completed++
            if (completed === total) { callback(uploaded) }
          }
        })
      })(photos[i])
    }
  },

  submitForm() {
    var formData = this.data.formData
    var batchInfo = this.data.sourceBatchInfo
    if (!batchInfo) { util.showError('请先查询关联批次'); return }
    if (!formData.processingDate) { util.showError('请选择加工日期'); return }
    if (!formData.processingMethod) { util.showError('请选择加工方式'); return }
    if (!formData.packagingSpec || !formData.packagingSpec.trim()) { util.showError('请输入包装规格'); return }
    if (!formData.operator || !formData.operator.trim()) { util.showError('请输入操作员姓名'); return }
    if (!formData.inputQuantity || Number(formData.inputQuantity) <= 0) { util.showError('请输入有效的投入数量'); return }
    if (!formData.outputQuantity || Number(formData.outputQuantity) <= 0) { util.showError('请输入有效的产出数量'); return }

    var that = this
    var batchId = batchInfo.batchId
    var userInfo = app.globalData.userInfo || {}
    var token = app.globalData.token
    var enterpriseId = userInfo.enterpriseId

    if (!batchId) {
      that._confirmAndSubmit()
      return
    }

    // Check if this batch already has a processing record
    util.showLoading('检查中...')
    wx.request({
      url: config.API_BASE_URL + '/api/processing/list?batchId=' + batchId + (enterpriseId ? '&enterpriseId=' + enterpriseId : ''),
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
      success: function(res) {
        util.hideLoading()
        var records = (res.data && res.data.data && res.data.data.records) || []
        if (records.length > 0) {
          that._existingRecordId = records[0].id || records[0].processingId
          wx.showModal({
            title: '记录已存在',
            content: '该批次已有加工记录，是否更新数据？',
            confirmText: '确认',
            cancelText: '取消',
            success: function(modalRes) {
              if (modalRes.confirm) {
                that._doSubmit(true)
              }
            }
          })
        } else {
          that._confirmAndSubmit()
        }
      },
      fail: function() {
        util.hideLoading()
        that._confirmAndSubmit()
      }
    })
  },

  _confirmAndSubmit() {
    var that = this
    wx.showModal({
      title: '确认提交',
      content: '确认提交加工记录？',
      success: function(res) {
        if (res.confirm) {
          that._doSubmit()
        }
      }
    })
  },

  _doSubmit(isUpdate) {
    this.setData({ submitting: true })
    var that = this
    var formData = this.data.formData
    var batchInfo = this.data.sourceBatchInfo
    var userInfo = app.globalData.userInfo || {}
    var token = app.globalData.token

    var apiUrl = config.API_BASE_URL + '/api/processing/create'
    var httpMethod = 'POST'
    if (isUpdate && that._existingRecordId) {
      apiUrl = config.API_BASE_URL + '/api/processing/update'
      httpMethod = 'PUT'
    }

    that.uploadPhotos(function(imageUrls) {
      var submitData = {
        batchId: batchInfo.batchId,
        sourceBatchCode: batchInfo.batchCode,
        enterpriseId: userInfo.enterpriseId || null,
        processingDate: formData.processingDate,
          processMethod: formData.processingMethod,
          specs: formData.packagingSpec.trim(),
          operator: formData.operator.trim(),
          inputQuantity: Number(formData.inputQuantity),
          inputUnit: formData.inputUnit || '只',
          outputQuantity: Number(formData.outputQuantity),
          outputUnit: formData.outputUnit || 'kg',
          images: imageUrls.length > 0 ? JSON.stringify(imageUrls) : null,
          remark: that.data.remark || ''
        }
        if (isUpdate && that._existingRecordId) {
          submitData.id = that._existingRecordId
        }
        wx.request({
          url: apiUrl,
          method: httpMethod,
          header: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
          },
          data: submitData,
        success: function(res) {
          that.setData({ submitting: false })
          if (res.statusCode === 200 && res.data && res.data.code === 200) {
            wx.showModal({
              title: '提交成功',
              content: '加工记录已提交',
              showCancel: false,
              success: function() { wx.navigateBack() }
            })
          } else {
            var msg = (res.data && res.data.message) || '提交失败'
            util.showError(msg)
          }
        },
        fail: function() {
          that.setData({ submitting: false })
          util.showError('网络错误，请重试')
        }
      })
    })
  },

  onPullDownRefresh() { wx.stopPullDownRefresh() },

  onShareAppMessage() {
    return { title: '加工录入', path: '/pages/collect/processing' }
  }
})
