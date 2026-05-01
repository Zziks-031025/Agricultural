/**
 * 报告上传 - 检疫合格证原件拍照上链
 * 流程：扫码关联批次 → 上传证书照片(最多3张) → 填写证书编号 → 提交并上链固证
 * 仅 enterpriseType === 3 可用
 */
const app = getApp()
const request = require('../../utils/request.js')

Page({
  data: {
    // 权限
    authorized: false,

    // 批次关联
    batchLinked: false,
    batchId: '',
    batchCode: '',
    productName: '',
    quantity: '',
    unit: '',
    enterpriseName: '',
    manualCode: '',

    // 照片列表（最多3张）
    imageList: [],
    maxImageCount: 3,

    // 表单
    certificateNo: '',
    remark: '',

    // 提交状态
    submitting: false,
    submitSuccess: false,

    // 上链结果
    txHash: '',
    txHashShort: '',
    dataHash: '',
    dataHashShort: '',
    blockNumber: '',
    chainTime: '',
    inspector: ''
  },

  onLoad(options) {
    // 权限校验
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const enterpriseType = userInfo.enterpriseType || app.globalData.enterpriseType || 0
    if (enterpriseType !== 3) {
      this.setData({ authorized: false })
      wx.showModal({
        title: '无权限',
        content: '此功能仅对检疫质检机构开放',
        showCancel: false,
        success: () => wx.navigateBack()
      })
      return
    }

    this.setData({
      authorized: true,
      inspector: userInfo.realName || userInfo.username || ''
    })

    // 如果从工作台传入了图片（兼容旧逻辑）
    if (options.imagePath) {
      const path = decodeURIComponent(options.imagePath)
      this.setData({ imageList: [path] })
    }
    // 接收检疫录入页传来的证书编号，自动填充
    if (options.certNo) {
      this.setData({ certificateNo: decodeURIComponent(options.certNo) })
    }

    if (options.batchCode) {
      this.setData({ manualCode: decodeURIComponent(options.batchCode) })
      this.fetchBatchInfo(decodeURIComponent(options.batchCode))
    }
  },

  // ========== 扫码关联批次 ==========

  startScan() {
    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode', 'barCode'],
      success: (res) => {
        const code = this.parseBatchCode(res.result || '')
        if (code) {
          this.fetchBatchInfo(code)
        } else {
          wx.showToast({ title: '未识别到有效批次号', icon: 'none' })
        }
      },
      fail: () => {
        // 用户取消扫码
      }
    })
  },

  parseBatchCode(result) {
    if (/^BATCH\d+$/i.test(result)) return result.toUpperCase()
    const m = result.match(/batch[\/=]?(BATCH\d+)/i)
    if (m) return m[1].toUpperCase()
    try {
      const obj = JSON.parse(result)
      if (obj.batchCode) return obj.batchCode
    } catch (e) {}
    if (/^\d+$/.test(result)) return result
    return null
  },

  fetchBatchInfo(code) {
    wx.showLoading({ title: '查询批次...' })
    request.get('/api/batch/detail', { batchCode: code }).then(res => {
      wx.hideLoading()
      if (res && res.code === 200 && res.data) {
        const d = res.data
        this.setData({
          batchLinked: true,
          batchId: String(d.id || d.batchId || ''),
          batchCode: d.batchCode || code,
          productName: d.productName || '',
          quantity: String(d.currentQuantity || d.initQuantity || d.quantity || ''),
          unit: d.unit || '只',
          enterpriseName: d.enterpriseName || ''
        })
        
        this.checkInspectionStatus(d.batchCode || code)
      } else {
        wx.showToast({ title: '未找到该批次信息', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '查询批次失败', icon: 'none' })
    })
  },

  checkInspectionStatus(batchCode) {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    
    request.get('/api/quarantine/apply/list-for-inspector', {
      status: 'completed',
      inspectionEnterpriseId: userInfo.enterpriseId,
      batchCode: batchCode
    }).then(res => {
      const records = res.data || []
      const existingRecord = records.find(r => r.batchCode === batchCode)
      
      if (!existingRecord) {
        wx.showModal({
          title: '提示',
          content: '该批次尚未进行检疫录入，是否跳转到检疫录入页面？',
          confirmText: '去录入',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              wx.navigateTo({
                url: `/pages/quarantine/result-form?batchCode=${batchCode}`
              })
            } else {
              this.setData({
                batchLinked: false,
                batchId: '',
                batchCode: '',
                productName: '',
                quantity: '',
                unit: '',
                enterpriseName: ''
              })
            }
          }
        })
      } else {
        wx.showToast({ title: '批次关联成功', icon: 'success' })
      }
    }).catch(err => {
      wx.showToast({ title: '批次关联成功', icon: 'success' })
    })
  },

  onManualInput(e) {
    this.setData({ manualCode: e.detail.value })
  },

  confirmManualInput() {
    const code = this.data.manualCode.trim()
    if (!code) { wx.showToast({ title: '请输入批次编号', icon: 'none' }); return }
    this.fetchBatchInfo(code)
  },

  unlinkBatch() {
    wx.showModal({
      title: '提示', content: '确认取消当前批次关联？',
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

  // ========== 照片管理（最多3张） ==========

  chooseImage() {
    const remaining = this.data.maxImageCount - this.data.imageList.length
    if (remaining <= 0) {
      wx.showToast({ title: '最多上传3张', icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['camera', 'album'],
      camera: 'back',
      success: (res) => {
        const newPaths = res.tempFiles.map(f => f.tempFilePath)
        this.setData({
          imageList: [...this.data.imageList, ...newPaths].slice(0, this.data.maxImageCount)
        })
      }
    })
  },

  previewImage(e) {
    const idx = e.currentTarget.dataset.idx
    wx.previewImage({
      urls: this.data.imageList,
      current: this.data.imageList[idx]
    })
  },

  deleteImage(e) {
    const idx = e.currentTarget.dataset.idx
    const list = [...this.data.imageList]
    list.splice(idx, 1)
    this.setData({ imageList: list })
  },

  // ========== 表单 ==========

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  // ========== 提交并上链 ==========

  submitReport() {
    const { batchLinked, batchCode, imageList, certificateNo } = this.data

    if (!batchLinked || !batchCode) {
      wx.showToast({ title: '请先扫码关联批次', icon: 'none' }); return
    }
    if (imageList.length === 0) {
      wx.showToast({ title: '请上传至少一张证书照片', icon: 'none' }); return
    }
    // 证书编号非必填，可由检疫录入页自动传入
    if (this.data.submitting) return

    this.setData({ submitting: true })

    // 计算所有图片的组合哈希
    this.computeImagesHash(imageList).then(dataHash => {
      this.setData({ dataHash })
      this.doUpload(dataHash)
    })
  },

  /**
   * 计算多张图片的组合哈希
   */
  computeImagesHash(paths) {
    return new Promise((resolve) => {
      const fs = wx.getFileSystemManager()
      let combinedHash = 0
      let processed = 0

      paths.forEach((path, idx) => {
        fs.readFile({
          filePath: path,
          success: (res) => {
            const bytes = new Uint8Array(res.data)
            const sample = Math.min(bytes.length, 2048)
            let h = 0x6a09e667
            for (let i = 0; i < sample; i++) {
              h = ((h << 5) - h + bytes[i]) | 0
            }
            h = ((h << 5) - h + bytes.length + idx) | 0
            combinedHash = (combinedHash ^ h) | 0
          },
          complete: () => {
            processed++
            if (processed === paths.length) {
              // 生成64字符哈希
              const p1 = Math.abs(combinedHash).toString(16).padStart(8, '0')
              const p2 = Date.now().toString(16).padStart(12, '0')
              const p3 = Math.abs(combinedHash * 31).toString(16).padStart(8, '0')
              const p4 = paths.length.toString(16).padStart(4, '0')
              const p5 = Math.floor(Math.random() * 0xffffffff).toString(16).padStart(8, '0')
              const p6 = Math.abs(combinedHash ^ 0xdeadbeef).toString(16).padStart(8, '0')
              const p7 = Math.floor(Math.random() * 0xffffffff).toString(16).padStart(8, '0')
              const p8 = Math.abs(combinedHash + 0x12345678).toString(16).padStart(8, '0')
              resolve('0x' + (p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8).substring(0, 64))
            }
          }
        })
      })
    })
  },

  /**
   * 上传到后端并更新检疫记录
   */
  doUpload(dataHash) {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}

    // 先查询该批次是否已有检疫记录
    wx.showLoading({ title: '查询检疫记录...' })
    
    request.get('/api/quarantine/apply/list-for-inspector', {
      status: 'completed',
      inspectionEnterpriseId: userInfo.enterpriseId
    }).then(res => {
      wx.hideLoading()
      
      const records = res.data || []
      const existingRecord = records.find(r => r.batchCode === this.data.batchCode)
      
      if (!existingRecord) {
        this.setData({ submitting: false })
        wx.showModal({
          title: '提示',
          content: '该批次尚未进行检疫录入，是否跳转到检疫录入页面？',
          confirmText: '去录入',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              wx.navigateTo({
                url: `/pages/quarantine/result-form?batchCode=${this.data.batchCode}`
              })
            }
          }
        })
        return
      }
      
      // 更新已有记录
      this.updateInspectionRecord(existingRecord.id)
      
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })
      wx.showToast({ title: '查询检疫记录失败', icon: 'none' })
    })
  },

  updateInspectionRecord(inspectionId) {
    wx.showLoading({ title: '上传图片...' })
    
    const submitHelper = require('../../utils/submit-helper.js')
    
    submitHelper.uploadImages(this.data.imageList, 'certificate').then(serverUrls => {
      wx.showLoading({ title: '提交中...' })
      
      const postData = {
        id: inspectionId,
        certImage: serverUrls.join(','),
        certNo: this.data.certificateNo.trim(),
        remark: this.data.remark || ''
      }

      request.post('/api/quarantine/update-certificate', postData).then(res => {
        wx.hideLoading()
        if (res && res.code === 200) {
          this.handleSuccess(res.data || {})
        } else {
          this.setData({ submitting: false })
          wx.showToast({ title: res?.message || '提交失败', icon: 'none' })
        }
      }).catch(err => {
        wx.hideLoading()
        this.setData({ submitting: false })
        wx.showToast({ title: err.message || '提交失败', icon: 'none' })
      })
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })
      wx.showToast({ title: '图片上传失败', icon: 'none' })
    })
  },

  handleSuccess(data) {
    const txHash = data.txHash || ''
    const dataHash = data.dataHash || ''
    const blockNumber = data.blockNumber || ''
    const chainTime = data.chainTime || ''

    this.setData({
      submitting: false,
      submitSuccess: true,
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
    const p = n => String(n).padStart(2, '0')
    return `${now.getFullYear()}-${p(now.getMonth() + 1)}-${p(now.getDate())} ${p(now.getHours())}:${p(now.getMinutes())}:${p(now.getSeconds())}`
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
      success: () => wx.showToast({ title: '数据指纹已复制', icon: 'success' })
    })
  },

  previewSuccessImage(e) {
    const idx = e.currentTarget.dataset.idx
    wx.previewImage({
      urls: this.data.imageList,
      current: this.data.imageList[idx]
    })
  },

  resetPage() {
    // 跳转到检疫录入页，继续录入下一个批次
    wx.redirectTo({ url: '/pages/quarantine/result-form' })
  },

  goBack() {
    wx.reLaunch({ url: '/pages/workbench/workbench' })
  }
})
