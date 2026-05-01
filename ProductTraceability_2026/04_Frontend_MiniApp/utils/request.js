const app = getApp()

/**
 * 封装 wx.request
 * 支持自动携带Token、统一错误处理、Promise调用
 */
class Request {
  constructor() {
    this.baseURL = ''
    this.timeout = 30000
  }

  /**
   * 设置基础URL
   */
  setBaseURL(url) {
    this.baseURL = url
  }

  /**
   * 获取完整URL
   */
  getFullURL(url) {
    if (url.startsWith('http')) {
      return url
    }
    const baseURL = this.baseURL || (getApp() && getApp().globalData ? getApp().globalData.apiBaseUrl : '')
    return baseURL + url
  }

  /**
   * 获取请求头
   */
  getHeaders() {
    const headers = {
      'Content-Type': 'application/json'
    }
    
    // 自动携带Token
    const token = (getApp() && getApp().globalData ? getApp().globalData.token : null) || wx.getStorageSync('token')
    if (token) {
      headers['Authorization'] = 'Bearer ' + token
    }
    
    return headers
  }

  /**
   * 核心请求方法
   */
  request(options) {
    return new Promise((resolve, reject) => {
      const method = options.method || 'GET'
      let url = this.getFullURL(options.url)
      let data = options.data || {}
      
      // GET请求需要将参数拼接到URL上
      if (method === 'GET' && Object.keys(data).length > 0) {
        const params = []
        for (const key in data) {
          if (data[key] !== undefined && data[key] !== null && data[key] !== '') {
            params.push(`${encodeURIComponent(key)}=${encodeURIComponent(data[key])}`)
          }
        }
        if (params.length > 0) {
          url += (url.indexOf('?') > -1 ? '&' : '?') + params.join('&')
        }
        data = {}
      }
      
      wx.request({
        url: url,
        method: method,
        data: data,
        header: {
          ...this.getHeaders(),
          ...options.header
        },
        timeout: options.timeout || this.timeout,
        success: res => {
          const { statusCode, data } = res
          
          // HTTP状态码处理
          if (statusCode === 200) {
            // 业务状态码处理
            if (data.code === 200) {
              resolve(data)
            } else if (data.code === 401) {
              // Token失效，跳转登录
              this.handleUnauthorized()
              reject(data)
            } else {
              // 其他业务错误
              this.showError(data.message || '请求失败')
              reject(data)
            }
          } else if (statusCode === 401) {
            // HTTP 401 未授权
            this.handleUnauthorized()
            reject(res)
          } else {
            // 其他HTTP错误
            this.showError(`请求失败 (${statusCode})`)
            reject(res)
          }
        },
        fail: err => {
          console.error('请求失败:', err)
          this.showError('网络请求失败，请检查网络连接')
          reject(err)
        }
      })
    })
  }

  /**
   * 处理未授权（401）
   */
  handleUnauthorized() {
    // 清除登录信息
    const app = getApp()
    if (app && app.clearLoginInfo) {
      app.clearLoginInfo()
    }
    
    // 提示用户
    wx.showModal({
      title: '登录过期',
      content: '您的登录状态已过期，请重新登录',
      showCancel: false,
      success: () => {
        // 跳转到登录页
        wx.reLaunch({
          url: '/pages/login/login'
        })
      }
    })
  }

  /**
   * 显示错误提示
   */
  showError(message) {
    wx.showToast({
      title: message,
      icon: 'none',
      duration: 2000
    })
  }

  /**
   * GET请求
   */
  get(url, data = {}, options = {}) {
    return this.request({
      url,
      method: 'GET',
      data,
      ...options
    })
  }

  /**
   * POST请求
   */
  post(url, data = {}, options = {}) {
    return this.request({
      url,
      method: 'POST',
      data,
      ...options
    })
  }

  /**
   * PUT请求
   */
  put(url, data = {}, options = {}) {
    return this.request({
      url,
      method: 'PUT',
      data,
      ...options
    })
  }

  /**
   * DELETE请求
   */
  delete(url, data = {}, options = {}) {
    return this.request({
      url,
      method: 'DELETE',
      data,
      ...options
    })
  }

  /**
   * 文件上传
   */
  upload(url, filePath, formData = {}) {
    return new Promise((resolve, reject) => {
      const token = (getApp() && getApp().globalData ? getApp().globalData.token : null) || wx.getStorageSync('token')
      wx.uploadFile({
        url: this.getFullURL(url),
        filePath: filePath,
        name: 'file',
        formData: formData,
        header: {
          'Authorization': 'Bearer ' + token
        },
        success: res => {
          const data = JSON.parse(res.data)
          if (data.code === 200) {
            resolve(data)
          } else {
            this.showError(data.message || '上传失败')
            reject(data)
          }
        },
        fail: err => {
          console.error('上传失败:', err)
          this.showError('文件上传失败')
          reject(err)
        }
      })
    })
  }

  /**
   * 文件下载
   */
  download(url) {
    return new Promise((resolve, reject) => {
      const token = (getApp() && getApp().globalData ? getApp().globalData.token : null) || wx.getStorageSync('token')
      wx.downloadFile({
        url: this.getFullURL(url),
        header: {
          'Authorization': 'Bearer ' + token
        },
        success: res => {
          if (res.statusCode === 200) {
            resolve(res.tempFilePath)
          } else {
            this.showError('下载失败')
            reject(res)
          }
        },
        fail: err => {
          console.error('下载失败:', err)
          this.showError('文件下载失败')
          reject(err)
        }
      })
    })
  }
}

// 创建实例
const request = new Request()

// 导出实例
module.exports = request
