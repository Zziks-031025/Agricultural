const util = require('../../utils/util.js')
const request = require('../../utils/request.js')
const validator = require('../../utils/validator.js')

Page({
  data: {
    submitting: false,
    agreed: false,
    formData: {
      username: '',
      realName: '',
      phone: '',
      password: '',
      confirmPassword: ''
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`formData.${field}`]: e.detail.value
    })
  },

  toggleAgreement() {
    this.setData({
      agreed: !this.data.agreed
    })
  },

  viewAgreement() {
    wx.showModal({
      title: '用户注册说明',
      content: '普通用户注册后可使用首页浏览、扫码溯源、消息查看等公开能力；企业工作台功能需要后续单独提交企业入驻申请并审核通过。',
      showCancel: false
    })
  },

  validateForm() {
    const { formData, agreed } = this.data

    if (!formData.username || formData.username.trim().length < 3) {
      util.showError('用户名至少3位')
      return false
    }
    if (!formData.realName || !formData.realName.trim()) {
      util.showError('请输入真实姓名')
      return false
    }
    if (!validator.isValidPhone(formData.phone)) {
      validator.showValidationError('请输入正确的11位手机号')
      return false
    }
    if (!formData.password || formData.password.length < 6) {
      util.showError('密码至少6位')
      return false
    }
    if (formData.password !== formData.confirmPassword) {
      util.showError('两次输入的密码不一致')
      return false
    }
    if (!agreed) {
      util.showError('请先阅读并同意注册说明')
      return false
    }

    return true
  },

  handleSubmit() {
    if (!this.validateForm()) {
      return
    }
    this.submitRegister()
  },

  submitRegister() {
    if (this.data.submitting) {
      return
    }

    const { formData } = this.data
    this.setData({ submitting: true })
    util.showLoading('提交中...')

    request.post('/auth/register', {
      username: formData.username.trim(),
      realName: formData.realName.trim(),
      phone: formData.phone.trim(),
      password: formData.password,
      confirmPassword: formData.confirmPassword
    }).then(() => {
      util.hideLoading()
      wx.showModal({
        title: '注册成功',
        content: '普通用户账号已创建，请返回登录页使用新账号登录。',
        showCancel: false,
        success: () => {
          wx.navigateBack()
        }
      })
    }).catch(() => {
      util.hideLoading()
    }).finally(() => {
      this.setData({ submitting: false })
    })
  }
})
