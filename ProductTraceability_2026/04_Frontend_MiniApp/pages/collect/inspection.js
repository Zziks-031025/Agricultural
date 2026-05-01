const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')
const submitHelper = require('../../utils/submit-helper.js')

Page({
  data: {
    inputMode: 'scan',
    batchNo: '',
    batchInfo: null,
    submitting: false,
    today: '',
    formData: {
      applyQuantity: '',
      applyUnit: '',
      expectedDate: '',
      remark: ''
    },
    inspectionEnterprises: [],
    inspectionEnterpriseNames: [],
    selectedInspectionIndex: -1,
    selectedInspectionId: null
  },

  onLoad(options) {
    const today = util.formatDate(new Date())
    this.setData({ today: today })

    this.loadInspectionEnterprises()

    if (options.batchNo || options.batchId) {
      this.setData({
        batchNo: options.batchNo || options.batchId,
        inputMode: 'input'
      })
      this.loadBatchInfo(options.batchNo || options.batchId)
    }
  },

  switchInputMode(e) {
    const mode = e.currentTarget.dataset.mode
    this.setData({
      inputMode: mode,
      batchNo: '',
      batchInfo: null,
      'formData.applyQuantity': '',
      'formData.applyUnit': ''
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
      fail: (err) => {
        console.error('scan failed:', err)
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

      // 检疫申请：批次状态必须为初始化(1)、生长中(2)或已收获(3)
      if (status !== 1 && status !== 2 && status !== 3) {
        wx.showModal({
          title: '无法申请',
          content: '该批次当前状态不允许提交检疫申请。已检疫合格的批次无需重复申请。',
          showCancel: false
        })
        this.setData({ batchNo: '', batchInfo: null })
        return
      }

      const qty = data.currentQuantity || data.initQuantity || ''
      const unit = data.unit || ''
      this.setData({
        batchInfo: {
          batchId: data.batchId || data.id,
          batchNo: data.batchCode,
          productName: data.productName,
          breed: data.breed,
          quantity: qty,
          unit: unit,
          origin: data.originLocation
        },
        'formData.applyQuantity': String(qty),
        'formData.applyUnit': unit
      })
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: err.message || '加载批次失败', icon: 'none' })
    })
  },

  changeBatch() {
    this.setData({
      batchNo: '',
      batchInfo: null,
      'formData.applyQuantity': '',
      'formData.applyUnit': ''
    })
  },

  loadInspectionEnterprises() {
    request.get('/api/enterprise/list-by-type', { type: 3 }).then(res => {
      const list = res.data || []
      this.setData({
        inspectionEnterprises: list,
        inspectionEnterpriseNames: list.map(e => e.enterpriseName)
      })
    }).catch(() => {})
  },

  onInspectionChange(e) {
    const index = Number(e.detail.value)
    const enterprise = this.data.inspectionEnterprises[index]
    this.setData({
      selectedInspectionIndex: index,
      selectedInspectionId: enterprise ? enterprise.id : null
    })
  },

  onDateChange(e) {
    this.setData({ 'formData.expectedDate': e.detail.value })
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`formData.${field}`]: e.detail.value })
  },

  validateForm() {
    const { batchNo, batchInfo, formData } = this.data

    if (!batchNo || !batchInfo) {
      wx.showToast({ title: '请先关联批次', icon: 'none' })
      return false
    }
    if (!formData.applyQuantity || Number(formData.applyQuantity) <= 0) {
      wx.showToast({ title: '请输入申报数量', icon: 'none' })
      return false
    }
    if (!this.data.selectedInspectionId) {
      wx.showToast({ title: '请选择检疫企业', icon: 'none' })
      return false
    }
    if (!formData.expectedDate) {
      wx.showToast({ title: '请选择期望检疫日期', icon: 'none' })
      return false
    }
    return true
  },

  submitForm() {
    if (!this.validateForm()) return

    const { batchNo } = this.data
    const userInfo = submitHelper.getUserInfo()
    const enterpriseId = userInfo.enterpriseId

    if (!enterpriseId) {
      this._confirmAndSubmit()
      return
    }

    // Check if this batch already has a quarantine apply record
    wx.showLoading({ title: '检查中...', mask: true })
    request.get('/api/quarantine/apply/list', {
      enterpriseId: enterpriseId,
      batchCode: batchNo
    }).then(res => {
      wx.hideLoading()
      const list = res.data || []
      // Find record matching this exact batchCode
      const existing = list.find(item => item.batchCode === batchNo || item.batch_code === batchNo)
      if (!existing) {
        this._confirmAndSubmit()
        return
      }

      // check_result: null=pending, 1=passed, 0=failed
      const checkResult = existing.checkResult !== undefined ? existing.checkResult : existing.check_result
      if (checkResult === null || checkResult === undefined) {
        // Pending review - block submission
        wx.showModal({
          title: '无法重复申报',
          content: '该批次已提交检疫申报，正在等待受理，请勿重复提交。',
          showCancel: false
        })
      } else if (checkResult === 1) {
        // Passed - block submission
        wx.showModal({
          title: '无法重复申报',
          content: '该批次检疫结果已合格，无需重复申报。',
          showCancel: false
        })
      } else if (checkResult === 0) {
        // Failed - allow re-apply with confirmation
        wx.showModal({
          title: '重新申报',
          content: '该批次上次检疫结果为不合格，是否重新申报？',
          confirmText: '确认',
          cancelText: '取消',
          success: (modalRes) => {
            if (modalRes.confirm) {
              this.createApply()
            }
          }
        })
      } else {
        this._confirmAndSubmit()
      }
    }).catch(() => {
      wx.hideLoading()
      // If check fails, proceed with normal flow
      this._confirmAndSubmit()
    })
  },

  _confirmAndSubmit() {
    wx.showModal({
      title: '确认提交',
      content: '确认提交检疫申请？提交后请等待检疫机构受理。',
      success: (res) => {
        if (res.confirm) {
          this.createApply()
        }
      }
    })
  },

  createApply() {
    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中...', mask: true })

    const { formData, batchNo } = this.data

    let submitData = {
      batchCode: batchNo,
      applyQuantity: Number(formData.applyQuantity),
      expectedDate: formData.expectedDate,
      inspectionEnterpriseId: this.data.selectedInspectionId,
      remark: formData.remark || ''
    }

    // Inject enterpriseId from current user
    submitData = submitHelper.injectCommonFields(submitData)

    request.post('/api/quarantine/apply', submitData).then(res => {
      wx.hideLoading()
      this.setData({ submitting: false })

      wx.showModal({
        title: '申请已提交',
        content: '检疫申请已提交，请等待检疫机构受理。',
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
    }).catch(err => {
      wx.hideLoading()
      this.setData({ submitting: false })
      wx.showToast({ title: err.message || '提交失败', icon: 'none' })
    })
  },

  resetForm() {
    this.setData({
      batchNo: '',
      batchInfo: null,
      formData: {
        applyQuantity: '',
        applyUnit: '',
        expectedDate: '',
        remark: ''
      },
      selectedInspectionIndex: -1,
      selectedInspectionId: null
    })
  }
})