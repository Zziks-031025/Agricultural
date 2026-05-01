/**
 * 资质证照展示页
 * type=business: 营业执照
 * type=production: 行业许可证
 */
const app = getApp()
const request = require('../../utils/request.js')
const util = require('../../utils/util.js')

// 证照类型配置
const LICENSE_TYPE_CONFIG = {
  business: {
    title: '营业执照',
    field: 'businessLicense',
    emptyText: '暂未上传营业执照',
    emptyTip: '请联系管理员上传企业营业执照'
  },
  production: {
    title: '行业许可证',
    field: 'productionLicense',
    emptyText: '暂未上传行业许可证',
    emptyTip: '请联系管理员上传行业许可证'
  },
  'food-production': {
    title: '行业许可证',
    field: 'productionLicense',
    emptyText: '暂未上传行业许可证',
    emptyTip: '请联系管理员上传行业许可证'
  },
  authority: {
    title: '机构执业资质',
    field: 'productionLicense',
    emptyText: '暂未上传执业资质',
    emptyTip: '请联系管理员上传机构执业资质'
  }
}

Page({
  data: {
    type: 'business',
    config: {},
    licenseUrl: '',
    enterpriseInfo: {},
    loading: true
  },

  onLoad(options) {
    const type = options.type || 'business'
    const config = LICENSE_TYPE_CONFIG[type] || LICENSE_TYPE_CONFIG.business
    
    // 设置页面标题
    wx.setNavigationBarTitle({
      title: config.title
    })
    
    this.setData({ type, config })
    this.loadLicenseInfo()
  },

  /**
   * 加载证照信息
   */
  loadLicenseInfo() {
    const userInfo = app.globalData.userInfo || {}
    const enterpriseId = userInfo.enterpriseId
    
    if (!enterpriseId) {
      this.setData({ loading: false })
      return
    }
    
    request.get(`/api/enterprise/detail/${enterpriseId}`).then(res => {
      if (res.code === 200 && res.data) {
        const data = res.data
        const { type } = this.data
        
        // 根据配置的字段名获取对应的证照URL
        const fieldName = this.data.config.field || (type === 'business' ? 'businessLicense' : 'productionLicense')
        let licenseUrl = data[fieldName] || ''
        
        // 拼接后端完整URL
        if (licenseUrl && !licenseUrl.startsWith('http')) {
          const baseUrl = (app && app.globalData ? app.globalData.apiBaseUrl : '')
          licenseUrl = baseUrl + licenseUrl
        }

        this.setData({
          enterpriseInfo: {
            name: data.enterpriseName || '',
            code: data.enterpriseCode || '',
            creditCode: data.creditCode || ''
          },
          licenseUrl: licenseUrl,
          loading: false
        })
        // 代理http图片为本地临时文件，解决真机<image>无法加载http图片
        if (licenseUrl) {
          util.proxyImage(licenseUrl).then(lp => {
            if (lp !== licenseUrl) this.setData({ licenseUrl: lp })
          })
        }
      } else {
        this.setData({ loading: false })
      }
    }).catch(err => {
      console.error('获取证照信息失败:', err)
      this.setData({ loading: false })
      wx.showToast({ title: '加载证照失败', icon: 'none' })
    })
  },

  /**
   * 预览证照大图
   */
  previewLicense() {
    const { licenseUrl } = this.data
    if (!licenseUrl) {
      util.showError('暂无证照图片')
      return
    }
    
    wx.previewImage({
      current: licenseUrl,
      urls: [licenseUrl]
    })
  },

  /**
   * 保存证照到相册
   */
  saveLicense() {
    const { licenseUrl } = this.data
    if (!licenseUrl) {
      util.showError('暂无证照图片')
      return
    }
    
    wx.showLoading({ title: '保存中...' })
    
    wx.downloadFile({
      url: licenseUrl,
      success: (res) => {
        if (res.statusCode === 200) {
          wx.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: () => {
              wx.hideLoading()
              util.showSuccess('已保存到相册')
            },
            fail: (err) => {
              wx.hideLoading()
              if (err.errMsg.includes('auth deny')) {
                wx.showModal({
                  title: '提示',
                  content: '请授权保存图片到相册',
                  confirmText: '去设置',
                  success: (res) => {
                    if (res.confirm) wx.openSetting()
                  }
                })
              } else {
                util.showError('保存失败')
              }
            }
          })
        }
      },
      fail: () => {
        wx.hideLoading()
        util.showError('下载失败')
      }
    })
  }
})
