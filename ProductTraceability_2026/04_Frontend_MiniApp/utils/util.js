/**
 * 格式化时间
 */
const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return `${[year, month, day].map(formatNumber).join('-')} ${[hour, minute, second].map(formatNumber).join(':')}`
}

/**
 * 格式化日期
 */
const formatDate = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()

  return `${[year, month, day].map(formatNumber).join('-')}`
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

/**
 * 防抖函数
 */
const debounce = (fn, delay = 500) => {
  let timer = null
  return function(...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 */
const throttle = (fn, delay = 500) => {
  let lastTime = 0
  return function(...args) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      fn.apply(this, args)
      lastTime = now
    }
  }
}

/**
 * 获取当前位置
 */
const getLocation = () => {
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02',
      success: res => {
        resolve({
          latitude: res.latitude,
          longitude: res.longitude
        })
      },
      fail: err => {
        console.error('获取位置失败:', err)
        reject(err)
      }
    })
  })
}

/**
 * 选择图片
 */
const chooseImage = (count = 1) => {
  return new Promise((resolve, reject) => {
    wx.chooseImage({
      count: count,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: res => {
        resolve(res.tempFilePaths)
      },
      fail: err => {
        reject(err)
      }
    })
  })
}

/**
 * 预览图片
 */
const previewImage = (current, urls) => {
  wx.previewImage({
    current: current,
    urls: urls
  })
}

/**
 * 扫码
 */
const scanCode = () => {
  return new Promise((resolve, reject) => {
    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode', 'barCode'],
      success: res => {
        resolve(res.result)
      },
      fail: err => {
        reject(err)
      }
    })
  })
}

/**
 * 显示加载提示
 */
const showLoading = (title = '加载中...') => {
  wx.showLoading({
    title: title,
    mask: true
  })
}

/**
 * 隐藏加载提示
 */
const hideLoading = () => {
  wx.hideLoading()
}

/**
 * 显示成功提示
 */
const showSuccess = (title = '操作成功') => {
  wx.showToast({
    title: title,
    icon: 'success',
    duration: 2000
  })
}

/**
 * 显示错误提示
 */
const showError = (title = '操作失败') => {
  wx.showToast({
    title: title,
    icon: 'none',
    duration: 2000
  })
}

/**
 * 确认对话框
 */
const showConfirm = (content, title = '提示') => {
  return new Promise((resolve, reject) => {
    wx.showModal({
      title: title,
      content: content,
      success: res => {
        if (res.confirm) {
          resolve(true)
        } else {
          resolve(false)
        }
      },
      fail: err => {
        reject(err)
      }
    })
  })
}

/**
 * 图片代理：将http局域网图片下载为本地临时文件
 * 解决微信真机<image>组件无法加载http图片的问题
 * 
 * 策略：
 * 1. 仅代理 http:// 局域网地址（192.168.x / 10.x / 172.16-31.x / localhost）
 * 2. https:// 和外网http图片直接返回，不消耗存储
 * 3. LRU缓存，上限MAX_CACHE_SIZE，超出时淘汰最早的缓存
 * 4. 微信临时文件由系统自动回收，不会无限占用存储
 * 5. 同一URL并发请求合并，避免重复下载
 */
const _imageCache = {}
const _imageCacheKeys = []
const _pendingRequests = {}
const MAX_CACHE_SIZE = 50

const _isLocalNetwork = (url) => {
  return /^http:\/\/(192\.168\.|10\.|172\.(1[6-9]|2\d|3[01])\.|localhost|127\.0\.0\.1)/.test(url)
}

const _evictCache = () => {
  while (_imageCacheKeys.length > MAX_CACHE_SIZE) {
    const oldKey = _imageCacheKeys.shift()
    delete _imageCache[oldKey]
  }
}

const _downloadWithRetry = (url, retries) => {
  return new Promise((resolve) => {
    wx.downloadFile({
      url: url,
      success: (res) => {
        if (res.statusCode === 200 && res.tempFilePath) {
          resolve(res.tempFilePath)
        } else if (retries > 0) {
          console.warn('[proxyImage] 下载失败(将重试):', url, 'status:', res.statusCode)
          setTimeout(() => _downloadWithRetry(url, retries - 1).then(resolve), 500)
        } else {
          console.warn('[proxyImage] 下载最终失败:', url, 'status:', res.statusCode)
          resolve('')
        }
      },
      fail: (err) => {
        if (retries > 0) {
          console.warn('[proxyImage] 下载失败(将重试):', url, err)
          setTimeout(() => _downloadWithRetry(url, retries - 1).then(resolve), 500)
        } else {
          console.warn('[proxyImage] 下载最终失败:', url, err)
          resolve('')
        }
      }
    })
  })
}

const proxyImage = (url) => {
  if (!url) return Promise.resolve('')
  // 非http直接返回（https、本地路径、wxfile等）
  if (!url.startsWith('http://')) return Promise.resolve(url)
  // 非局域网http地址直接返回，不做代理
  if (!_isLocalNetwork(url)) return Promise.resolve(url)
  // 缓存命中
  if (_imageCache[url]) return Promise.resolve(_imageCache[url])
  // 合并同一URL的并发请求
  if (_pendingRequests[url]) return _pendingRequests[url]

  _pendingRequests[url] = _downloadWithRetry(url, 2).then(localPath => {
    if (localPath) {
      _imageCache[url] = localPath
      _imageCacheKeys.push(url)
      _evictCache()
    }
    return localPath || url
  }).finally(() => {
    delete _pendingRequests[url]
  })

  return _pendingRequests[url]
}

/**
 * 批量代理图片
 * @param {string[]} urls 图片URL数组
 * @returns {Promise<string[]>} 本地临时文件路径数组
 */
const proxyImages = (urls) => {
  return Promise.all((urls || []).map(u => proxyImage(u)))
}

module.exports = {
  formatTime,
  formatDate,
  debounce,
  throttle,
  getLocation,
  chooseImage,
  previewImage,
  scanCode,
  showLoading,
  hideLoading,
  showSuccess,
  showError,
  showConfirm,
  proxyImage,
  proxyImages
}
