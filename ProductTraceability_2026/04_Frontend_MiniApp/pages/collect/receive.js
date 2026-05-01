// pages/collect/receive.js
const config = require('../../config.js')
const util = require('../../utils/util.js')
const app = getApp()

Page({

  /**
   * 页面的初始数据
   */
  data: {
    step: 'scan',
    batchCode: '',
    batchInfo: null,
    useMock: false,
    formData: {
      receiveQuantity: '',
      receiver: ''
    },
    remark: ''
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.setData({
      useMock: config.USE_MOCK || false
    })
    if (options && options.batchCode) {
      this.setData({ batchCode: options.batchCode })
      this.queryBatch(options.batchCode)
    }
  },

  /**
   * 从扫码原始内容中解析批次编号
   * 兼容纯批次号和 URL 格式（如 https://trace.example.com/batch/BATCH202602010001）
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

  /**
   * 扫码
   */
  startScan() {
    var that = this
    util.scanCode().then(function(result) {
      if (result) {
        var batchCode = that.parseBatchCode(result)
        if (!batchCode) {
          util.showError('无法识别的二维码格式')
          return
        }
        that.setData({ batchCode: batchCode })
        that.queryBatch(batchCode)
      }
    }).catch(function() {
      util.showError('扫码取消或失败')
    })
  },

  /**
   * 手动输入批次编号
   */
  onBatchCodeInput(e) {
    this.setData({ batchCode: e.detail.value })
  },

  /**
   * 手动输入后点击查询
   */
  confirmManualInput() {
    var raw = this.data.batchCode.trim()
    if (!raw) {
      util.showError('请输入批次编号')
      return
    }
    var code = this.parseBatchCode(raw) || raw
    this.setData({ batchCode: code })
    this.queryBatch(code)
  },

  /**
   * 查询批次并核验检疫状态
   */
  queryBatch(batchCode) {
    this.setData({ step: 'verify' })
    var that = this
    var token = app.globalData.token

    wx.request({
      url: config.API_BASE_URL + '/api/batch/quarantine-check/' + batchCode,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + token
      },
      success: function(res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          var data = res.data.data
          var passed = data.quarantinePassed || data.passed || false
          if (passed) {
            that.setData({
              step: 'form',
              batchInfo: {
                batchCode: data.batchCode || batchCode,
                productName: data.productName || '--',
                currentQuantity: data.currentQuantity || data.quantity || '',
                initQuantity: data.initQuantity || '',
                unit: data.unit || '只',
                enterpriseName: data.enterpriseName || '--',
                inspectionDate: data.inspectionDate || data.quarantineDate || '--',
                inspector: data.inspector || data.quarantineInspector || '--',
                certificateNo: data.certificateNo || data.quarantineCertNo || '--',
                batchId: data.batchId || data.id
              },
              formData: {
                receiveQuantity: String(data.currentQuantity || data.quantity || data.initQuantity || ''),
                receiver: (app.globalData.userInfo && app.globalData.userInfo.realName) || ''
              },
              remark: ''
            })
          } else {
            wx.showModal({
              title: '无法接收',
              content: '该批次尚未通过检疫，不能接收。请确认批次编号是否正确。',
              showCancel: false,
              success: function() {
                that.setData({ step: 'scan' })
              }
            })
          }
        } else {
          var msg = (res.data && res.data.message) || '查询失败'
          wx.showModal({
            title: '查询失败',
            content: msg,
            showCancel: false,
            success: function() {
              that.setData({ step: 'scan' })
            }
          })
        }
      },
      fail: function() {
        wx.showModal({
          title: '网络错误',
          content: '无法连接服务器，请检查网络后重试',
          showCancel: false,
          success: function() {
            that.setData({ step: 'scan' })
          }
        })
      }
    })
  },

  /**
   * 接收数量输入
   */
  onQuantityInput(e) {
    this.setData({ 'formData.receiveQuantity': e.detail.value })
  },

  /**
   * 接收人输入
   */
  onReceiverInput(e) {
    this.setData({ 'formData.receiver': e.detail.value })
  },

  /**
   * 备注输入
   */
  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  /**
   * 重新扫码
   */
  reScan() {
    this.setData({
      step: 'scan',
      batchCode: '',
      batchInfo: null,
      formData: { receiveQuantity: '', receiver: '' },
      remark: ''
    })
  },

  /**
   * 确认接收
   */
  submitReceive() {
    var formData = this.data.formData
    var batchCode = this.data.batchCode
    var batchInfo = this.data.batchInfo
    var remark = this.data.remark

    if (!formData.receiveQuantity || Number(formData.receiveQuantity) <= 0) {
      util.showError('请输入有效的接收数量')
      return
    }
    if (!formData.receiver || !formData.receiver.trim()) {
      util.showError('请输入接收人姓名')
      return
    }

    var userInfo = app.globalData.userInfo || {}
    var token = app.globalData.token
    var that = this
    var enterpriseId = userInfo.enterpriseId
    var batchId = batchInfo && batchInfo.batchId

    if (!batchId) {
      that._confirmAndSubmit()
      return
    }

    // Check if this batch already has a storage record for current enterprise (receive = storage)
    util.showLoading('检查中...')
    var checkUrl = config.API_BASE_URL + '/api/storage/list?batchId=' + batchId + (enterpriseId ? '&enterpriseId=' + enterpriseId : '')
    wx.request({
      url: checkUrl,
      method: 'GET',
      header: { 'Authorization': 'Bearer ' + token },
      success: function(res) {
        util.hideLoading()
        var records = (res.data && res.data.data && res.data.data.records) || []
        if (records.length > 0) {
          wx.showModal({
            title: '记录已存在',
            content: '该批次已接收过，是否更新数据？',
            confirmText: '确认',
            cancelText: '取消',
            success: function(modalRes) {
              if (modalRes.confirm) {
                that._doReceive()
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
      title: '确认接收',
      content: '确认接收该批次？',
      success: function(res) {
        if (res.confirm) {
          that._doReceive()
        }
      }
    })
  },

  _doReceive() {
    var formData = this.data.formData
    var batchCode = this.data.batchCode
    var batchInfo = this.data.batchInfo
    var remark = this.data.remark
    var userInfo = app.globalData.userInfo || {}
    var token = app.globalData.token
    var that = this

    util.showLoading('提交中...')

    wx.request({
      url: config.API_BASE_URL + '/api/batch/receive',
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      data: {
        batchCode: batchInfo.batchCode || batchCode,
        receiveQuantity: Number(formData.receiveQuantity),
        receiver: formData.receiver.trim(),
        enterpriseId: userInfo.enterpriseId || null,
        receiveDate: new Date().toISOString().substring(0, 10),
        remark: remark || ''
      },
      success: function(res) {
        util.hideLoading()
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          wx.showModal({
            title: '接收成功',
            content: '批次 ' + (batchInfo.batchCode || batchCode) + ' 已成功接收',
            showCancel: false,
            success: function() {
              wx.navigateBack()
            }
          })
        } else {
          var msg = (res.data && res.data.message) || '接收失败'
          util.showError(msg)
        }
      },
      fail: function() {
        util.hideLoading()
        util.showError('网络错误，请重试')
      }
    })
  },

  /**
   * Mock: 检疫合格批次
   */
  mockScanPassed() {
    this.setData({
      step: 'form',
      batchCode: 'MOCK_BATCH_001',
      batchInfo: {
        batchCode: 'MOCK_BATCH_001',
        productName: '三黄鸡（Mock）',
        currentQuantity: 500,
        initQuantity: 500,
        unit: '只',
        enterpriseName: 'Mock养殖场',
        inspectionDate: '2026-03-01',
        inspector: '张检疫员',
        certificateNo: 'QC-MOCK-20260301'
      },
      formData: { receiveQuantity: '', receiver: '' },
      remark: ''
    })
  },

  /**
   * Mock: 未检疫批次
   */
  mockScanFailed() {
    wx.showModal({
      title: '无法接收',
      content: '该批次尚未通过检疫，不能接收。（Mock模拟）',
      showCancel: false
    })
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    wx.stopPullDownRefresh()
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {
    return {
      title: '扫码接收',
      path: '/pages/collect/receive'
    }
  }
})
