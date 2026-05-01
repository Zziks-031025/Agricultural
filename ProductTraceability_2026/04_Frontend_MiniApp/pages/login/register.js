const app = getApp()
const request = require('../../utils/request.js')
const submitHelper = require('../../utils/submit-helper.js')

Page({
  data: {
    currentStep: 1,
    submitting: false,
    isReapply: false,
    isLoggedInApply: false,
    loggedInUserId: null,
    reapplyEnterpriseId: null,
    enterpriseTypes: ['种植养殖企业', '加工宰杀企业', '检疫质检机构'],
    selectedTypeIndex: -1,
    form: {
      enterpriseName: '',
      enterpriseCode: '',
      enterpriseType: null,
      legalPerson: '',
      contactPerson: '',
      contactPhone: '',
      contactEmail: '',
      province: '',
      city: '',
      district: '',
      address: '',
      introduction: '',
      username: '',
      password: '',
      confirmPassword: ''
    },
    locationText: '',
    logoUrl: '',
    businessLicenseUrl: '',
    productionLicenseUrl: '',
    coverImageUrl: '',
    serverLogoUrl: '',
    serverBusinessLicenseUrl: '',
    serverProductionLicenseUrl: '',
    serverCoverImageUrl: ''
  },

  onLoad(options = {}) {
    const isReapply = options.mode === 'reapply' && !!options.enterpriseId
    if (isReapply) {
      const reapplyEnterpriseId = Number(options.enterpriseId)
      this.setData({
        isReapply: true,
        reapplyEnterpriseId
      })
      wx.setNavigationBarTitle({
        title: '修改后重新提交'
      })
      this.loadReapplyEnterprise(reapplyEnterpriseId)
      return
    }

    // 已登录普通用户申请入驻：自动填入当前账号信息
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const isLoggedIn = !!(app.globalData.token || wx.getStorageSync('token'))
    if (isLoggedIn && userInfo.username) {
      this.setData({
        isLoggedInApply: true,
        loggedInUserId: userInfo.id || null,
        'form.username': userInfo.username || '',
        'form.password': '******',
        'form.confirmPassword': '******'
      })
    }
  },

  onInputChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        const { name, address, latitude, longitude } = res
        // 优先使用微信返回的中文地址，name 通常是 POI 名称，address 是街道地址
        const displayText = address || name || ''
        const detailAddress = name || address || ''
        const locationStr = displayText || (latitude + ', ' + longitude)
        const region = this.parseRegionFromAddress(displayText)
        const updateData = {
          locationText: locationStr,
          'form.address': detailAddress
        }
        if (region.province) updateData['form.province'] = region.province
        if (region.city) updateData['form.city'] = region.city
        if (region.district) updateData['form.district'] = region.district

        this.setData(updateData)
        wx.showToast({ title: '定位成功', icon: 'success' })
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.indexOf('auth deny') !== -1) {
          wx.showModal({
            title: '定位权限',
            content: '需要开启定位权限以便自动识别地址，请在设置中允许使用位置信息。',
            confirmText: '去设置',
            success: (res) => {
              if (res.confirm) {
                wx.openSetting()
              }
            }
          })
          return
        }
        if (!err.errMsg || err.errMsg.indexOf('cancel') === -1) {
          wx.showToast({ title: '定位失败', icon: 'none' })
        }
      }
    })
  },

  parseRegionFromAddress(addressStr) {
    if (!addressStr) return { province: '', city: '', district: '' }

    let province = ''
    let city = ''
    let district = ''

    const provinceEnd = addressStr.indexOf('省')
    const cityEnd = addressStr.indexOf('市')
    let districtEnd = -1
    ;['区', '县'].forEach((suffix) => {
      if (districtEnd !== -1) return
      const idx = addressStr.indexOf(suffix)
      if (idx !== -1) {
        districtEnd = idx
      }
    })

    if (provinceEnd !== -1) {
      province = addressStr.slice(0, provinceEnd + 1)
    }
    if (cityEnd !== -1) {
      const start = provinceEnd !== -1 ? provinceEnd + 1 : 0
      city = addressStr.slice(start, cityEnd + 1)
    }
    if (districtEnd !== -1) {
      const start = cityEnd !== -1 ? cityEnd + 1 : (provinceEnd !== -1 ? provinceEnd + 1 : 0)
      district = addressStr.slice(start, districtEnd + 1)
    }

    return { province, city, district }
  },

  onTypeChange(e) {
    const index = Number(e.detail.value)
    this.setData({
      selectedTypeIndex: index,
      'form.enterpriseType': index + 1
    })
  },

  resolveImageUrl(url) {
    if (!url) {
      return ''
    }
    if (url.startsWith('http')) {
      return url
    }
    return `${app.globalData.apiBaseUrl || ''}${url}`
  },

  loadReapplyEnterprise(enterpriseId) {
    wx.showLoading({ title: '加载中...', mask: true })
    request.get(`/api/enterprise/detail/${enterpriseId}`).then((res) => {
      wx.hideLoading()
      if (res.code !== 200 || !res.data) {
        wx.showToast({ title: res.message || '加载申请信息失败', icon: 'none' })
        return
      }

      const enterprise = res.data
      const enterpriseType = Number(enterprise.enterpriseType) || null
      const selectedTypeIndex = enterpriseType ? enterpriseType - 1 : -1
      const locationText = [enterprise.province, enterprise.city, enterprise.district, enterprise.address]
        .filter(Boolean)
        .join('')

      this.setData({
        selectedTypeIndex,
        form: {
          ...this.data.form,
          enterpriseName: enterprise.enterpriseName || '',
          enterpriseCode: enterprise.enterpriseCode || '',
          enterpriseType,
          legalPerson: enterprise.legalPerson || '',
          contactPerson: enterprise.contactPerson || '',
          contactPhone: enterprise.contactPhone || '',
          contactEmail: enterprise.contactEmail || '',
          province: enterprise.province || '',
          city: enterprise.city || '',
          district: enterprise.district || '',
          address: enterprise.address || '',
          introduction: enterprise.introduction || ''
        },
        locationText,
        logoUrl: this.resolveImageUrl(enterprise.logo),
        businessLicenseUrl: this.resolveImageUrl(enterprise.businessLicense),
        productionLicenseUrl: this.resolveImageUrl(enterprise.productionLicense),
        coverImageUrl: this.resolveImageUrl(enterprise.coverImage),
        serverLogoUrl: enterprise.logo || '',
        serverBusinessLicenseUrl: enterprise.businessLicense || '',
        serverProductionLicenseUrl: enterprise.productionLicense || '',
        serverCoverImageUrl: enterprise.coverImage || ''
      })
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '加载申请信息失败', icon: 'none' })
    })
  },

  chooseImage(e) {
    const field = e.currentTarget.dataset.field
    const typeMap = {
      logo: 'logo',
      businessLicense: 'license',
      productionLicense: 'license',
      coverImage: 'cover'
    }

    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['camera', 'album'],
      success: (res) => {
        const tempPath = res.tempFilePaths[0]
        this.setData({ [`${field}Url`]: tempPath })
        wx.showLoading({ title: '上传中...', mask: true })
        submitHelper.uploadImage(tempPath, typeMap[field] || 'enterprise').then((serverUrl) => {
          wx.hideLoading()
          this.setData({
            [`server${field.charAt(0).toUpperCase() + field.slice(1)}Url`]: serverUrl
          })
          wx.showToast({ title: '上传成功', icon: 'success' })
        }).catch(() => {
          wx.hideLoading()
          this.setData({ [`${field}Url`]: '' })
          wx.showToast({ title: '上传失败', icon: 'none' })
        })
      }
    })
  },

  nextStep() {
    const { form, currentStep, selectedTypeIndex } = this.data
    if (currentStep === 1) {
      if (!form.enterpriseName) return wx.showToast({ title: '请输入企业名称', icon: 'none' })
      if (!form.enterpriseCode || form.enterpriseCode.length !== 18) return wx.showToast({ title: '请输入18位信用代码', icon: 'none' })
      if (selectedTypeIndex < 0) return wx.showToast({ title: '请选择企业类型', icon: 'none' })
      if (!form.legalPerson) return wx.showToast({ title: '请输入法人代表', icon: 'none' })
      if (!form.contactPerson) return wx.showToast({ title: '请输入联系人', icon: 'none' })
      if (!form.contactPhone || !/^1[3-9]\d{9}$/.test(form.contactPhone)) return wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      if (!form.address) return wx.showToast({ title: '请输入详细地址', icon: 'none' })
      this.setData({ currentStep: 2 })
      return
    }

    if (currentStep === 2) {
      if (!this.data.serverBusinessLicenseUrl) return wx.showToast({ title: '请上传营业执照', icon: 'none' })
      if (!this.data.serverProductionLicenseUrl) return wx.showToast({ title: '请上传行业许可证', icon: 'none' })
      this.setData({ currentStep: 3 })
    }
  },

  prevStep() {
    if (this.data.currentStep > 1) {
      this.setData({ currentStep: this.data.currentStep - 1 })
    }
  },

  buildSubmitData() {
    const { form } = this.data
    return {
      enterpriseName: form.enterpriseName,
      enterpriseCode: form.enterpriseCode,
      enterpriseType: form.enterpriseType,
      legalPerson: form.legalPerson,
      contactPerson: form.contactPerson,
      contactPhone: form.contactPhone,
      contactEmail: form.contactEmail || null,
      province: form.province || null,
      city: form.city || null,
      district: form.district || null,
      address: form.address,
      introduction: form.introduction || null,
      logo: this.data.serverLogoUrl || null,
      businessLicense: this.data.serverBusinessLicenseUrl || null,
      productionLicense: this.data.serverProductionLicenseUrl || null,
      coverImage: this.data.serverCoverImageUrl || null,
      username: form.username,
      password: form.password
    }
  },

  submitForm() {
    const { form, isReapply, isLoggedInApply, loggedInUserId, reapplyEnterpriseId } = this.data

    if (!isReapply && !isLoggedInApply) {
      if (!form.username || form.username.length < 3) {
        return wx.showToast({ title: '账号至少3位', icon: 'none' })
      }
      if (!form.password || form.password.length < 6) {
        return wx.showToast({ title: '密码至少6位', icon: 'none' })
      }
      if (form.password !== form.confirmPassword) {
        return wx.showToast({ title: '两次密码不一致', icon: 'none' })
      }
    }

    const submitData = this.buildSubmitData()

    // 已登录用户入驻：传 userId 而非 username/password，后端绑定已有用户
    if (isLoggedInApply && loggedInUserId) {
      submitData.userId = loggedInUserId
      delete submitData.username
      delete submitData.password
    }

    this.setData({ submitting: true })
    wx.showLoading({ title: '提交中...', mask: true })

    if (isReapply) {
      const updatePayload = {
        id: reapplyEnterpriseId,
        enterpriseName: submitData.enterpriseName,
        enterpriseCode: submitData.enterpriseCode,
        enterpriseType: submitData.enterpriseType,
        legalPerson: submitData.legalPerson,
        contactPerson: submitData.contactPerson,
        contactPhone: submitData.contactPhone,
        contactEmail: submitData.contactEmail,
        province: submitData.province,
        city: submitData.city,
        district: submitData.district,
        address: submitData.address,
        introduction: submitData.introduction,
        logo: submitData.logo,
        businessLicense: submitData.businessLicense,
        productionLicense: submitData.productionLicense,
        coverImage: submitData.coverImage
      }

      request.put('/api/enterprise/update', updatePayload).then(() => {
        return request.put(`/api/enterprise/reapply/${reapplyEnterpriseId}`)
      }).then(() => {
        const currentUserInfo = app.globalData.userInfo || {}
        const nextUserInfo = {
          ...currentUserInfo,
          enterpriseId: reapplyEnterpriseId,
          enterpriseType: submitData.enterpriseType,
          enterpriseName: submitData.enterpriseName,
          auditStatus: 0,
          enterpriseAuditStatus: 0,
          auditRemark: ''
        }

        if (app.setLoginInfo) {
          app.setLoginInfo({
            token: app.globalData.token || '',
            enterpriseType: submitData.enterpriseType,
            userInfo: nextUserInfo
          })
        }
        app.globalData = {
          ...app.globalData,
          enterpriseType: submitData.enterpriseType,
          userInfo: nextUserInfo
        }

        wx.hideLoading()
        this.setData({ submitting: false })
        wx.showModal({
          title: '重新提交成功',
          content: '企业申请资料已更新，并已重新提交审核。',
          showCancel: false,
          success: () => {
            wx.navigateBack()
          }
        })
      }).catch((err) => {
        wx.hideLoading()
        this.setData({ submitting: false })
        wx.showToast({ title: err.message || '重新提交失败', icon: 'none' })
      })
      return
    }

    request.post('/api/enterprise/register', submitData).then(() => {
      wx.hideLoading()
      this.setData({ submitting: false })
      wx.showModal({
        title: '注册成功',
        content: '企业入驻申请已提交，管理员将在1-3个工作日内完成审核。',
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
    }).catch((err) => {
      wx.hideLoading()
      this.setData({ submitting: false })
      wx.showToast({ title: err.message || '注册失败', icon: 'none' })
    })
  },

  goLogin() {
    wx.navigateBack()
  }
})
