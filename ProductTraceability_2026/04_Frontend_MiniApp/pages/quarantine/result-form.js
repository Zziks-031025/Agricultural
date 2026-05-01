/**
 * 检疫录入 - 检疫质检机构专属
 * 对待检批次录入检疫结论，提交后调用后端接口上链存证
 * 支持两种入口：
 *   1. 从申报接收列表带入批次信息 → 直接展示
 *   2. 从工作台快捷入口进入（无参数）→ 需扫码关联批次
 */
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    // 批次关联状态
    batchLinked: false,
    scanning: false,
    manualCode: '',

    // 批次信息
    batchId: '',
    batchCode: '',
    productName: '',
    quantity: '',
    unit: '',
    enterpriseName: '',

    // 表单字段
    result: 'pass',
    inspectionItems: '',
    certificateNo: '',
    inspector: '',
    inspectorCode: '',
    inspectionDate: '',
    remark: '',

    // 提交状态
    submitting: false,         // 全屏 Loading 开关
    submitSuccess: false,      // 是否提交成功（展示证书页）

    // 上链结果
    txHash: '',
    txHashShort: '',
    dataHash: '',
    dataHashShort: '',
    blockNumber: '',
    chainTime: '',
    checkResultText: ''
  },

  onLoad(options) {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const today = new Date()
    const dateStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

    const inspectorName = userInfo.realName || userInfo.username || ''
    const inspectorCode = userInfo.certificateNo || userInfo.employeeCode || ('QY-' + (userInfo.id || userInfo.userId || '0001'))
    const hasBatchInfo = !!(options.batchCode || options.batchId)

    this.setData({
      batchLinked: hasBatchInfo,
      batchId: options.batchId || '',
      batchCode: options.batchCode || '',
      productName: decodeURIComponent(options.productName || ''),
      quantity: options.quantity || '',
      unit: options.unit || '',
      enterpriseName: decodeURIComponent(options.enterpriseName || ''),
      inspector: inspectorName,
      inspectorCode: inspectorCode,
      inspectionDate: dateStr
    })

    if (options.batchCode && !options.productName) {
      this.fetchBatchInfo(options.batchCode)
    }
  },

  // ========== 扫码关联 ==========

  startScan() {
    if (this.data.scanning) return
    this.setData({ scanning: true })

    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode', 'barCode'],
      success: (res) => {
        const scanResult = res.result || ''
        const batchCode = this.parseBatchCode(scanResult)
        if (batchCode) {
          this.fetchBatchInfo(batchCode)
        } else {
          wx.showToast({ title: '未识别到有效批次号', icon: 'none' })
        }
      },
      fail: () => {
        // 用户取消扫码
      },
      complete: () => {
        this.setData({ scanning: false })
      }
    })
  },

  parseBatchCode(result) {
    if (/^BATCH\d+$/i.test(result)) return result.toUpperCase()
    const urlMatch = result.match(/batch[\/=]?(BATCH\d+)/i)
    if (urlMatch) return urlMatch[1].toUpperCase()
    try {
      const obj = JSON.parse(result)
      if (obj.batchCode) return obj.batchCode
      if (obj.batchId) return String(obj.batchId)
    } catch (e) { /* not JSON */ }
    if (/^\d+$/.test(result)) return result
    return null
  },

  fetchBatchInfo(batchCode) {
    wx.showLoading({ title: '查询批次...' })
    request.get('/api/batch/detail', { batchCode }).then(res => {
      wx.hideLoading()
      if (res && res.code === 200 && res.data) {
        const d = res.data
        this.setData({
          batchLinked: true,
          batchId: String(d.id || d.batchId || ''),
          batchCode: d.batchCode || batchCode,
          productName: d.productName || '',
          quantity: String(d.currentQuantity || d.initQuantity || d.quantity || ''),
          unit: d.unit || '只',
          enterpriseName: d.enterpriseName || ''
        })
        wx.showToast({ title: '批次关联成功', icon: 'success' })
      } else {
        wx.showToast({ title: '未找到该批次信息', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '查询批次失败', icon: 'none' })
    })
  },

  onManualInput(e) {
    this.setData({ manualCode: e.detail.value })
  },

  confirmManualInput() {
    const code = this.data.manualCode.trim()
    if (!code) {
      wx.showToast({ title: '请输入批次编号', icon: 'none' })
      return
    }
    this.fetchBatchInfo(code)
  },

  unlinkBatch() {
    wx.showModal({
      title: '提示',
      content: '确认取消当前批次关联？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            batchLinked: false, batchId: '', batchCode: '',
            productName: '', quantity: '', unit: '',
            enterpriseName: '', manualCode: ''
          })
        }
      }
    })
  },

  // ========== 表单交互 ==========

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  setResult(e) {
    this.setData({ result: e.currentTarget.dataset.result })
  },

  onDateChange(e) {
    this.setData({ inspectionDate: e.detail.value })
  },

  // ========== 提交检疫结果 ==========

  submitForm() {
    const { batchLinked, batchId, batchCode, result, inspectionItems, inspectionDate, certificateNo } = this.data

    // 校验
    if (!batchLinked || !batchCode) {
      wx.showToast({ title: '请先扫码关联批次', icon: 'none' }); return
    }
    if (!inspectionItems.trim()) {
      wx.showToast({ title: '请填写具体检测项', icon: 'none' }); return
    }
    if (!inspectionDate) {
      wx.showToast({ title: '请选择检疫日期', icon: 'none' }); return
    }

    // 进入全屏 Loading 状态
    this.setData({ submitting: true })

    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}

    // 组装提交数据（对齐后端 QuarantineSubmitDTO）
    const postData = {
      batchId: batchId ? Number(batchId) : undefined,
      checkResult: result === 'pass' ? 1 : 0,
      inspectorId: userInfo.id || userInfo.userId || undefined,
      imagePath: '',
      enterpriseId: userInfo.enterpriseId || undefined,
      certificateNo: certificateNo || '',
      inspectionItems: inspectionItems.trim(),
      inspectionType: 1,
      unqualifiedReason: result === 'fail' ? (this.data.remark || '检疫不合格') : '',
      remark: this.data.remark || ''
    }

    // 调用后端接口
    request.post('/api/quarantine/submit', postData).then(res => {
      if (res && res.code === 200 && res.data) {
        this.handleSubmitSuccess(res.data)
      } else {
        this.setData({ submitting: false })
        wx.showToast({ title: res?.message || '提交失败', icon: 'none' })
      }
    }).catch(err => {
      this.setData({ submitting: false })
      wx.showToast({ title: err.message || '提交失败', icon: 'none' })
    })
  },

  /**
   * 处理提交成功 → 切换到证书展示态
   */
  handleSubmitSuccess(data) {
    const txHash = data.txHash || data.transactionHash || ''
    const dataHash = data.dataHash || ''
    const blockNumber = data.blockNumber || ''
    const chainTime = data.chainTime || ''

    this.setData({
      submitting: false,
      submitSuccess: true,
      checkResultText: this.data.result === 'pass' ? '合格' : '不合格',
      txHash: txHash,
      txHashShort: txHash ? (txHash.substring(0, 10) + '...' + txHash.substring(txHash.length - 8)) : '',
      dataHash: dataHash,
      dataHashShort: dataHash ? (dataHash.substring(0, 10) + '...' + dataHash.substring(dataHash.length - 8)) : '',
      blockNumber: blockNumber ? String(blockNumber) : '',
      chainTime: chainTime || ''
    })

    wx.setNavigationBarTitle({ title: '提交成功' })
  },

  getCurrentTime() {
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  },

  // ========== 成功页操作 ==========

  copyTxHash() {
    if (!this.data.txHash) return
    wx.setClipboardData({
      data: this.data.txHash,
      success: () => wx.showToast({ title: '交易哈希已复制', icon: 'success' })
    })
  },

  copyDataHash() {
    if (!this.data.dataHash) return
    wx.setClipboardData({
      data: this.data.dataHash,
      success: () => wx.showToast({ title: '数据哈希已复制', icon: 'success' })
    })
  },

  goBackToList() {
    wx.reLaunch({ url: '/pages/workbench/workbench' })
  },

  continueInspection() {
    // 跳转到报告上传页，带当前批次信息和证书编号
    const { batchCode, certificateNo } = this.data
    const params = []
    if (batchCode) params.push('batchCode=' + encodeURIComponent(batchCode))
    if (certificateNo) params.push('certNo=' + encodeURIComponent(certificateNo))
    wx.redirectTo({
      url: '/pages/quarantine/report-upload' + (params.length ? '?' + params.join('&') : '')
    })
  }
})
