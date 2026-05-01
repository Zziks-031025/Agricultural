const app = getApp()
const request = require('../../utils/request.js')
const { proxyImage } = require('../../utils/util.js')

Page({
  data: {
    enterpriseId: '',
    enterpriseInfo: {}
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ enterpriseId: options.id })

      const enterprise = this.getCachedEnterprise(options.id)
      if (enterprise) {
        const formatted = this.formatEnterpriseData(enterprise)
        this.setData({ enterpriseInfo: formatted })
        this.proxyEnterpriseImages(formatted)
      }

      this.loadEnterpriseDetail()
    }
  },

  getCachedEnterprise(id) {
    const cached = app.globalData && app.globalData.currentEnterprise
    if (!cached || !cached.id) {
      return null
    }
    return String(cached.id) === String(id) ? cached : null
  },

  loadEnterpriseDetail() {
    wx.showLoading({ title: '加载中...' })
    const id = this.data.enterpriseId

    // 调用后端 API 获取企业详情
    request.get(`/api/enterprise/detail/${id}`).then(res => {
      wx.hideLoading()
      if (res.code === 200 && res.data) {
        const data = {
          ...(this.getCachedEnterprise(id) || {}),
          ...res.data
        }
        const enterpriseInfo = this.formatEnterpriseData(data)
        this.setData({ enterpriseInfo })
        if (app.globalData) {
          app.globalData.currentEnterprise = { ...data }
        }
        this.proxyEnterpriseImages(enterpriseInfo)
        this.loadEnterpriseMetrics(id)
      } else {
        wx.showToast({ title: '企业不存在', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error('获取企业详情失败:', err)
      wx.showToast({ title: '加载失败', icon: 'none' })
    })
  },

  loadEnterpriseMetrics(id) {
    return request.get(`/api/enterprise/metrics/${id}`).then(res => {
      if (res.code !== 200 || !res.data) {
        return
      }
      this.setData({
        'enterpriseInfo.chainCount': this.toCount(res.data.chainCount),
        'enterpriseInfo.batchCount': this.toCount(res.data.batchCount),
        'enterpriseInfo.verifyCount': this.toCount(res.data.verifyCount),
        'enterpriseInfo.productCount': this.toCount(res.data.productCount),
        'enterpriseInfo.lastChainTime': res.data.lastChainTime || ''
      })
    }).catch(() => {})
  },

  /**
   * 将相对路径转为后端完整URL
   */
  resolveImageUrl(path) {
    if (!path) return ''
    if (path.startsWith('http')) return path
    const baseUrl = (getApp() && getApp().globalData ? getApp().globalData.apiBaseUrl : '')
    return baseUrl + path
  },

  /**
   * 格式化后端返回的企业数据
   */
  formatEnterpriseData(data) {
    const typeNameMap = {
      1: '种植养殖企业',
      2: '加工宰杀企业',
      3: '检疫质检机构'
    }
    
    // 组合完整地址
    const fullAddress = [data.province, data.city, data.district, data.address]
      .filter(Boolean)
      .join('')
    
    return {
      id: data.id,
      enterpriseName: data.enterpriseName,
      enterpriseType: data.enterpriseType,
      enterpriseTypeName: data.enterpriseTypeName || typeNameMap[data.enterpriseType] || '未知类型',
      creditCode: data.creditCode || data.enterpriseCode,
      legalPerson: data.legalPerson,
      contactPhone: data.contactPhone,
      province: data.province,
      city: data.city,
      district: data.district,
      address: fullAddress,
      logo: this.resolveImageUrl(data.logo),
      chainCount: this.toCount(data.chainCount),
      batchCount: this.toCount(data.batchCount),
      verifyCount: this.toCount(data.verifyCount),
      productCount: this.toCount(data.productCount),
      lastChainTime: data.lastChainTime || '',
      businessLicense: this.resolveImageUrl(data.businessLicense),
      productionLicense: this.resolveImageUrl(data.productionLicense),
      coverImage: this.resolveImageUrl(data.coverImage || data.logo),
      introduction: data.introduction || '暂无企业简介'
    }
  },

  toCount(value) {
    const count = Number(value)
    return Number.isFinite(count) ? count : 0
  },

  async proxyEnterpriseImages(info) {
    const fields = ['logo', 'coverImage', 'businessLicense', 'productionLicense']
    for (const field of fields) {
      if (info[field]) {
        try {
          const localPath = await proxyImage(info[field])
          if (localPath && localPath !== info[field]) {
            this.setData({ ['enterpriseInfo.' + field]: localPath })
          }
        } catch (e) {
          console.warn('[enterprise-detail] proxy失败:', field, e)
        }
      }
    }
  },

  previewImage(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return

    const { enterpriseInfo } = this.data
    const urls = [
      enterpriseInfo.businessLicense,
      enterpriseInfo.productionLicense
    ].filter(Boolean)

    wx.previewImage({
      current: url,
      urls: urls
    })
  },

  callPhone() {
    const phone = this.data.enterpriseInfo.contactPhone
    if (!phone) {
      wx.showToast({ title: '暂无联系电话', icon: 'none' })
      return
    }

    wx.makePhoneCall({
      phoneNumber: phone,
      fail: () => {
        wx.showToast({ title: '拨号取消', icon: 'none' })
      }
    })
  }
})
