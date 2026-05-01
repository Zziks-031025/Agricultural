const request = require('../../utils/request.js')

Page({
  data: {
    username: '',
    password: ''
  },

  onUsernameInput(e) {
    this.setData({
      username: e.detail.value
    })
  },

  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
    })
  },

  handleSubmit() {
    const { username, password } = this.data

    if (!username || !password) {
      wx.showToast({
        title: '请输入用户名和密码',
        icon: 'none'
      })
      return
    }

    this.login()
  },

  login() {
    wx.showLoading({ title: '登录中...' })

    request.post('/auth/login', {
      username: this.data.username,
      password: this.data.password
    }).then(res => {
      wx.hideLoading()

      if (res.data && res.data.token) {
        const app = getApp()
        const userInfo = res.data.userInfo || {}

        if (app && app.setLoginInfo) {
          app.setLoginInfo({
            token: res.data.token,
            userRole: userInfo.userType === 2 ? 'enterprise' : 'consumer',
            enterpriseType: userInfo.enterpriseType || null,
            userInfo: userInfo
          })
        }

        wx.showToast({
          title: '登录成功',
          icon: 'success'
        })

        setTimeout(() => {
          wx.switchTab({
            url: '/pages/index/index'
          })
        }, 1500)
      } else {
        wx.showToast({
          title: '登录数据异常',
          icon: 'none'
        })
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({
        title: err.message || '登录失败',
        icon: 'none'
      })
    })
  },

  register() {
    wx.navigateTo({
      url: '/pages/user/register'
    })
  },

  switchMode() {
    this.register()
  },

  goEnterpriseRegister() {
    wx.navigateTo({
      url: '/pages/login/register'
    })
  }
})
