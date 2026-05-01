const app = getApp()
const util = require('../../utils/util.js')
const request = require('../../utils/request.js')

Page({
  data: {
    isCameraOpen: false,
    showModal: false,
    showSuccessToast: false,
    inputBatchCode: '',
    hasScanned: false,

    // -- Processor (Type 2) specific --
    isProcessor: false,
    enterpriseType: 0,
    showResultPanel: false,   // whether result panel is visible
    isVerifying: false,       // loading state during verification
    scanResultType: '',       // 'batch' or 'trace'
    batchInfo: null,          // batch detail info
    quarantinePassed: false,  // quarantine check result
    scannedCode: '',          // raw scanned code

  },

  onLoad(options) {
    this.checkUserType()
  },

  onShow() {
    this.setData({ hasScanned: false })
  },

  onHide() {
    this.setData({ isCameraOpen: false })
  },

  onUnload() {
    this.setData({ isCameraOpen: false })
  },

  /**
   * Check current user type
   */
  checkUserType() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const userType = userInfo.userType || 3
    let enterpriseType = 0

    if (userType === 2) {
      enterpriseType = userInfo.enterpriseType || app.globalData.enterpriseType || 0
    }

    this.setData({
      enterpriseType: enterpriseType,
      isProcessor: false
    })
  },

  // ==================== Camera controls ====================

  openCamera() {
    wx.authorize({
      scope: 'scope.camera',
      success: () => {
        this.setData({ isCameraOpen: true })
      },
      fail: () => {
        wx.showModal({
          title: '提示',
          content: '请授权相机权限以使用扫码功能',
          confirmText: '去设置',
          success: (res) => {
            if (res.confirm) {
              wx.openSetting()
            }
          }
        })
      }
    })
  },

  closeCamera() {
    this.setData({ isCameraOpen: false })
  },

  // ==================== Scan callback ====================

  handleScanSuccess(e) {
    if (this.data.hasScanned) return

    const result = e.detail.result
    if (!result) return

    this.setData({ hasScanned: true })

    // -- If processor, use special classification logic --
    if (this.data.isProcessor) {
      this.handleScanResult(result)
      return
    }

    // -- Default consumer / farmer logic: parse and jump --
    const batchId = this.parseBatchId(result)

    if (batchId) {
      this.setData({ showSuccessToast: true })

      setTimeout(() => {
        this.setData({
          showSuccessToast: false,
          isCameraOpen: false
        })
        wx.navigateTo({
          url: `/pages/trace/trace?batchId=${batchId}`
        })
      }, 800)
    } else {
      this.setData({ hasScanned: false })
      wx.showToast({
        title: '无法识别的二维码',
        icon: 'none',
        duration: 2000
      })
    }
  },

  // ==================== Processor scan result classification ====================

  /**
   * Classify scanned code and handle accordingly
   * Logic A: batch code -> quarantine check
   * Logic B: trace/product code -> jump to trace detail
   */
  handleScanResult(rawResult) {
    const scannedCode = rawResult.trim()

    this.setData({
      scannedCode: scannedCode,
      isCameraOpen: false
    })

    // Classify the code
    const codeType = this.classifyCode(scannedCode)

    if (codeType === 'batch') {
      // Logic A: batch code -> quarantine check
      this.handleBatchCode(scannedCode)
    } else if (codeType === 'trace') {
      // Logic B: trace/product code -> jump to trace page
      this.handleTraceCode(scannedCode)
    } else {
      // Unknown code type
      this.setData({ hasScanned: false })
      wx.showToast({
        title: '无法识别的二维码格式',
        icon: 'none',
        duration: 2000
      })
    }
  },

  /**
   * Classify code type: 'batch' (养殖批次码) or 'trace' (成品溯源码)
   */
  classifyCode(content) {
    if (!content) return 'unknown'

    // Trace/product code: starts with TRACE or URL contains /trace/
    if (/^TRACE/i.test(content)) return 'trace'
    if (/\/(trace|product)\//i.test(content)) return 'trace'
    if (/[?&]traceCode=/i.test(content)) return 'trace'

    // Batch code: starts with BATCH or URL contains /batch/
    if (/^BATCH/i.test(content)) return 'batch'
    if (/\/batch\//i.test(content)) return 'batch'
    if (/[?&](?:batchId|batchCode)=/i.test(content)) return 'batch'

    // Pure alphanumeric 8+ chars: default to batch
    if (/^[A-Za-z0-9]{8,}$/.test(content)) return 'batch'

    return 'unknown'
  },

  /**
   * Logic A: Handle batch code -> quarantine status check
   */
  handleBatchCode(code) {
    // Parse actual batch code from various formats
    const batchCode = this.parseBatchCode(code)
    if (!batchCode) {
      this.setData({ hasScanned: false })
      wx.showToast({ title: '无法解析批次编号', icon: 'none' })
      return
    }

    // Show verifying loading panel
    this.setData({
      showResultPanel: true,
      isVerifying: true,
      scanResultType: 'batch',
      batchInfo: null,
      quarantinePassed: false,
      scannedCode: batchCode
    })

    request.get('/api/batch/quarantine-check/' + batchCode).then(res => {
      const data = res.data
      this.setData({
        isVerifying: false,
        batchInfo: data,
        quarantinePassed: data.quarantinePassed === true
      })
    }).catch(err => {
      this.setData({
        isVerifying: false,
        showResultPanel: false,
        hasScanned: false
      })
      wx.showModal({
        title: '查询失败',
        content: err.message || '未找到该批次信息，请确认码是否正确',
        showCancel: false
      })
    })
  },

  /**
   * Logic B: Handle trace/product code -> jump to trace detail
   */
  handleTraceCode(code) {
    // Parse the trace code
    let traceCode = code
    const traceMatch = code.match(/[?&]traceCode=([^&]+)/)
    if (traceMatch) {
      traceCode = traceMatch[1]
    } else {
      const pathMatch = code.match(/\/(trace|product)\/([A-Za-z0-9]+)/)
      if (pathMatch) {
        traceCode = pathMatch[2]
      } else {
        const tracePrefix = code.match(/(TRACE[A-Za-z0-9]+)/i)
        if (tracePrefix) {
          traceCode = tracePrefix[1]
        }
      }
    }

    // Show brief loading then jump
    this.setData({
      showResultPanel: true,
      isVerifying: true,
      scanResultType: 'trace',
      scannedCode: traceCode
    })

    setTimeout(() => {
      this.setData({
        showResultPanel: false,
        isVerifying: false
      })
      wx.navigateTo({
        url: `/pages/trace/trace?batchId=${encodeURIComponent(traceCode)}`
      })
    }, 1000)
  },

  /**
   * Parse batch code from scan content
   */
  parseBatchCode(content) {
    if (!content) return null

    const urlParamMatch = content.match(/[?&](?:batchId|batchCode)=([^&]+)/)
    if (urlParamMatch) return urlParamMatch[1]

    const urlPathMatch = content.match(/\/batch\/([A-Za-z0-9]+)/)
    if (urlPathMatch) return urlPathMatch[1]

    const batchMatch = content.match(/(BATCH[A-Za-z0-9]+)/i)
    if (batchMatch) return batchMatch[1].toUpperCase()

    const codeMatch = content.match(/^[A-Za-z0-9]{8,}$/)
    if (codeMatch) return content

    return null
  },

  // ==================== Processor navigation ====================

  /**
   * Navigate to receive page (from quarantine-passed result)
   */
  goToReceive() {
    const { batchInfo, scannedCode } = this.data
    const batchCode = (batchInfo && batchInfo.batchCode) || scannedCode
    this.setData({ showResultPanel: false })
    wx.navigateTo({
      url: `/pages/collect/receive?batchCode=${encodeURIComponent(batchCode)}`
    })
  },

  /**
   * Close result panel and go back to scan
   */
  closeResultPanel() {
    this.setData({
      showResultPanel: false,
      isVerifying: false,
      batchInfo: null,
      quarantinePassed: false,
      scanResultType: '',
      hasScanned: false
    })
  },

  // ==================== Default (consumer/farmer) parsing ====================

  parseBatchId(content) {
    if (!content) return null

    const urlParamMatch = content.match(/[?&]batchId=([^&]+)/)
    if (urlParamMatch) return urlParamMatch[1]

    const urlPathMatch = content.match(/\/(trace|batch)\/([A-Za-z0-9]+)/)
    if (urlPathMatch) return urlPathMatch[2]

    const batchMatch = content.match(/(BATCH\d+)/i)
    if (batchMatch) return batchMatch[1].toUpperCase()

    const codeMatch = content.match(/^[A-Za-z0-9]{8,}$/)
    if (codeMatch) return content

    return null
  },

  // ==================== Common (shared across all roles) ====================

  handleCameraError(e) {
    console.error('Camera error:', e.detail)
    this.setData({ isCameraOpen: false })
    wx.showToast({
      title: '相机启动失败',
      icon: 'none',
      duration: 2000
    })
  },

  showInputModal() {
    this.setData({
      showModal: true,
      inputBatchCode: ''
    })
  },

  hideInputModal() {
    this.setData({ showModal: false })
  },

  onInputChange(e) {
    this.setData({ inputBatchCode: e.detail.value })
  },

  clearInput() {
    this.setData({ inputBatchCode: '' })
  },

  confirmInput() {
    const code = this.data.inputBatchCode.trim()

    if (!code) {
      wx.showToast({ title: '请输入批次号', icon: 'none' })
      return
    }

    this.setData({
      showModal: false,
      isCameraOpen: false,
      hasScanned: true
    })

    // Processor: use classification logic
    if (this.data.isProcessor) {
      this.handleScanResult(code)
      return
    }

    // Default: jump to trace page
    wx.navigateTo({
      url: `/pages/trace/trace?batchId=${code}`
    })
  },

  stopPropagation() {},
  preventTouchMove() {}
})
